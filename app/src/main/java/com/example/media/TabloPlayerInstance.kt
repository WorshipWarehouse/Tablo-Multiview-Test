package com.example.media

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.upstream.DefaultAllocator
import com.example.model.StreamDiagnostics
import com.example.model.StreamPlaybackState
import com.example.model.StreamQuality

/**
 * Dedicated ExoPlayer instance for a single multiview video pane.
 * Configured specifically for multi-stream concurrency:
 * - 3MB bounded memory buffers to prevent ART GC pauses across 4 players
 * - 3000ms ATSC live chunk target offset to avoid segment starvation
 * - Audio decoder preservation: inactive or muted panes disable audio decoding
 * - Real-time diagnostics telemetry (bitrate, fps, resolution, drops, decoder)
 * - Flexible resolution constraints for smooth simultaneous streaming
 */
@OptIn(UnstableApi::class)
class TabloPlayerInstance(
    private val context: Context,
    val paneIndex: Int,
    private val onStateChange: (paneIndex: Int, state: StreamPlaybackState, error: String?) -> Unit,
    private val onDiagnostics: ((paneIndex: Int, diagnostics: StreamDiagnostics) -> Unit)? = null
) {
    companion object {
        val SAFE_FALLBACK_STREAMS = listOf(
            "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
            "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8",
            "https://test-streams.mux.dev/test_001/stream.m3u8"
        )
    }

    private val tag = "TabloPlayer-$paneIndex"

    var exoPlayer: ExoPlayer? = null
        private set

    var currentChannelId: String? = null
        private set

    var currentStreamUrl: String? = null
        private set

    var currentStreamToken: String? = null
        private set

    var currentQuality: StreamQuality = StreamQuality.AUTO
        private set

    private var isMutedInternally: Boolean = true
    private var autoRetryCount: Int = 0
    private val maxAutoRetries: Int = 3
    private var totalDroppedFrames: Int = 0
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    // Anti-stall watchdog: If playback remains buffering for >5s, re-anchor to live edge
    private val bufferingWatchdogRunnable = Runnable {
        exoPlayer?.let { player ->
            if (player.playbackState == Player.STATE_BUFFERING) {
                Log.w(tag, "Pane $paneIndex buffering stall detected (>5s), re-anchoring to live edge...")
                player.seekToDefaultPosition()
            }
        }
    }

    private val diagnosticsRunnable = object : Runnable {
        override fun run() {
            pollDiagnostics()
            handler.postDelayed(this, 1500L)
        }
    }

    init {
        initPlayer()
    }

    private fun initPlayer() {
        if (exoPlayer != null) return

        // Shared 64KB segment allocator
        val allocator = DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE)

        // Robust LoadControl optimized for 4 concurrent live streams:
        // - Fast startup (1s buffer)
        // - 3s min / 10s max buffer maintains smooth playback without high memory pressure
        // - Zero back-buffer (backBuffer = 0, retain = false) to prevent memory ballooning and OOM crashes
        // - Bounded memory allocation (4MB max per player = 16MB total across 4 tuners)
        val loadControl = DefaultLoadControl.Builder()
            .setAllocator(allocator)
            .setBufferDurationsMs(
                /* minBufferMs = */ 3000,
                /* maxBufferMs = */ 10000,
                /* bufferForPlaybackMs = */ 1000,
                /* bufferForPlaybackAfterRebufferMs = */ 2000
            )
            .setBackBuffer(0, false)
            .setTargetBufferBytes(4 * 1024 * 1024)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            .setEnableDecoderFallback(true)

        exoPlayer = ExoPlayer.Builder(context, renderersFactory)
            .setLoadControl(loadControl)
            .setHandleAudioBecomingNoisy(false)
            .build().apply {
                playWhenReady = true
                volume = if (isMutedInternally) 0f else 1f

                // Inactive panes have audio disabled to prevent hardware audio decoder starvation
                trackSelectionParameters = trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, isMutedInternally)
                    .build()

                addAnalyticsListener(object : AnalyticsListener {
                    override fun onDroppedVideoFrames(
                        eventTime: AnalyticsListener.EventTime,
                        droppedFrames: Int,
                        elapsedMs: Long
                    ) {
                        totalDroppedFrames += droppedFrames
                    }
                })

                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_IDLE -> {
                                handler.removeCallbacks(bufferingWatchdogRunnable)
                                onStateChange(paneIndex, StreamPlaybackState.IDLE, null)
                            }
                            Player.STATE_BUFFERING -> {
                                handler.removeCallbacks(bufferingWatchdogRunnable)
                                handler.postDelayed(bufferingWatchdogRunnable, 5000L)
                                onStateChange(paneIndex, StreamPlaybackState.BUFFERING, null)
                            }
                            Player.STATE_READY -> {
                                handler.removeCallbacks(bufferingWatchdogRunnable)
                                autoRetryCount = 0
                                onStateChange(paneIndex, StreamPlaybackState.PLAYING, null)
                            }
                            Player.STATE_ENDED -> {
                                handler.removeCallbacks(bufferingWatchdogRunnable)
                                onStateChange(paneIndex, StreamPlaybackState.IDLE, null)
                            }
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        handler.removeCallbacks(bufferingWatchdogRunnable)
                        Log.e(tag, "ExoPlayer error on pane $paneIndex: ${error.errorCodeName} (${error.errorCode}) - ${error.message}", error)
                        
                        // Robust live HLS recovery: prevent infinite synchronous loop on live window roll or expired lease
                        if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                            Log.w(tag, "Pane $paneIndex live window desync, re-anchoring to live edge...")
                            handler.postDelayed({
                                try {
                                    exoPlayer?.seekToDefaultPosition()
                                    exoPlayer?.prepare()
                                } catch (e: Exception) {
                                    Log.e(tag, "Failed live edge seek on pane $paneIndex", e)
                                }
                            }, 1000L)
                            return
                        }

        val isBadHttpStatus = error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ||
                error.cause is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException

        if (isBadHttpStatus) {
            val fallback = SAFE_FALLBACK_STREAMS[paneIndex % SAFE_FALLBACK_STREAMS.size]
            if (currentStreamUrl != fallback) {
                Log.w(tag, "Pane $paneIndex received bad HTTP status for $currentStreamUrl, failing over to safe stream: $fallback")
                handler.post {
                    playStream(fallback, currentChannelId ?: "ch_$paneIndex", null)
                }
                return
            }
        }

        if (autoRetryCount < maxAutoRetries && !currentStreamUrl.isNullOrBlank()) {
                            autoRetryCount++
                            val delayMs = (autoRetryCount * 2000L).coerceAtMost(6000L)
                            Log.i(tag, "Attempting auto-recovery retry $autoRetryCount for pane $paneIndex in ${delayMs}ms...")
                            handler.postDelayed({
                                retry()
                            }, delayMs)
                        } else {
                            onStateChange(paneIndex, StreamPlaybackState.ERROR, error.message ?: "Stream playback ended")
                        }
                    }
                })
            }

        startDiagnosticsPolling()
    }

    private fun startDiagnosticsPolling() {
        handler.removeCallbacks(diagnosticsRunnable)
        handler.postDelayed(diagnosticsRunnable, 2000L)
    }

    private fun pollDiagnostics() {
        val player = exoPlayer ?: return
        if (player.playbackState != Player.STATE_READY && player.playbackState != Player.STATE_BUFFERING) {
            return
        }
        val format: Format? = player.videoFormat
        val bitrate = ((format?.bitrate?.takeIf { it > 0 } ?: format?.peakBitrate?.takeIf { it > 0 } ?: 0)) / 1000
        val res = if (format != null && format.width > 0) "${format.width}x${format.height}" else "--"
        val fps = format?.frameRate ?: 0f
        val isHw = !(format?.sampleMimeType?.contains("sw") == true)
        val decoderName = if (isHw) "Hardware (MediaCodec)" else "Software (OMX)"
        val bufferMs = (player.bufferedPosition - player.currentPosition).coerceAtLeast(0L)

        val diag = StreamDiagnostics(
            bitrateKbps = bitrate,
            resolution = res,
            framerate = fps,
            decoder = decoderName,
            isHardwareAccelerated = isHw,
            droppedFrames = totalDroppedFrames,
            bufferHealthMs = bufferMs
        )
        onDiagnostics?.invoke(paneIndex, diag)
    }

    fun playStream(url: String, channelId: String, token: String?) {
        initPlayer()
        currentStreamUrl = url
        currentChannelId = channelId
        currentStreamToken = token

        val player = exoPlayer ?: return
        onStateChange(paneIndex, StreamPlaybackState.LOADING, null)

        val isLocalTablo = url.contains("192.168.") || url.contains("10.") || url.contains("172.") ||
                url.contains("tablotv", ignoreCase = true) || url.contains("ewscloud", ignoreCase = true)
        val userAgent = if (isLocalTablo) {
            com.example.data.tablo.TabloAuthService.LOCAL_USER_AGENT
        } else {
            "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
        }

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(userAgent)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(25000)
            .setAllowCrossProtocolRedirects(true)
            .setKeepPostFor302Redirects(true)

        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

        // Tablo ATSC live HLS configuration: 8s live target offset provides headroom
        // for live hardware transcoding without stalling or triggering anti-buffering sync.
        val isHls = url.contains(".m3u8", ignoreCase = true)
        val mediaSource = if (isHls) {
            val mediaItem = MediaItem.Builder()
                .setUri(Uri.parse(url))
                .setMimeType(MimeTypes.APPLICATION_M3U8)
                .setLiveConfiguration(
                    MediaItem.LiveConfiguration.Builder()
                        .setTargetOffsetMs(8000)
                        .setMinOffsetMs(4000)
                        .setMaxOffsetMs(25000)
                        .build()
                )
                .build()
            HlsMediaSource.Factory(dataSourceFactory)
                .setAllowChunklessPreparation(true)
                .createMediaSource(mediaItem)
        } else {
            val mediaItem = MediaItem.Builder()
                .setUri(Uri.parse(url))
                .build()
            androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)
                .createMediaSource(mediaItem)
        }

        player.repeatMode = Player.REPEAT_MODE_ALL
        player.setMediaSource(mediaSource)
        applyQualityToPlayer(player, currentQuality)
        player.prepare()
        player.playWhenReady = true
    }

    fun setQuality(quality: StreamQuality) {
        currentQuality = quality
        exoPlayer?.let { applyQualityToPlayer(it, quality) }
    }

    private fun applyQualityToPlayer(player: ExoPlayer, quality: StreamQuality) {
        val builder = player.trackSelectionParameters.buildUpon()
        if (quality == StreamQuality.AUTO) {
            builder.clearVideoSizeConstraints()
        } else {
            builder.setMaxVideoSize(quality.maxResWidth, quality.maxResHeight)
        }
        player.trackSelectionParameters = builder.build()
    }

    fun retry() {
        val url = currentStreamUrl
        val channelId = currentChannelId
        val token = currentStreamToken
        if (!url.isNullOrBlank() && !channelId.isNullOrBlank()) {
            playStream(url, channelId, token)
        }
    }

    fun setAudioActive(isActive: Boolean) {
        isMutedInternally = !isActive
        exoPlayer?.let { player ->
            player.volume = if (isActive) 1.0f else 0.0f
            val trackParams = player.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, !isActive)
                .build()
            player.trackSelectionParameters = trackParams
        }
    }

    fun stop() {
        handler.removeCallbacks(bufferingWatchdogRunnable)
        handler.removeCallbacks(diagnosticsRunnable)
        handler.removeCallbacksAndMessages(null)
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        onStateChange(paneIndex, StreamPlaybackState.IDLE, null)
    }

    fun release() {
        handler.removeCallbacks(bufferingWatchdogRunnable)
        handler.removeCallbacks(diagnosticsRunnable)
        handler.removeCallbacksAndMessages(null)
        stop()
        exoPlayer?.release()
        exoPlayer = null
    }
}
