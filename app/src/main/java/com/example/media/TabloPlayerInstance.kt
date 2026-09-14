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

        // Bounded LoadControl prevents multi-player buffer allocation spikes
        val loadControl = DefaultLoadControl.Builder()
            .setAllocator(allocator)
            .setBufferDurationsMs(
                /* minBufferMs = */ 2000,
                /* maxBufferMs = */ 6500,
                /* bufferForPlaybackMs = */ 500,
                /* bufferForPlaybackAfterRebufferMs = */ 1200
            )
            .setTargetBufferBytes(3 * 1024 * 1024) // 3MB target prevents RAM exhaustion
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
                                onStateChange(paneIndex, StreamPlaybackState.IDLE, null)
                            }
                            Player.STATE_BUFFERING -> {
                                onStateChange(paneIndex, StreamPlaybackState.BUFFERING, null)
                            }
                            Player.STATE_READY -> {
                                autoRetryCount = 0
                                onStateChange(paneIndex, StreamPlaybackState.PLAYING, null)
                            }
                            Player.STATE_ENDED -> {
                                onStateChange(paneIndex, StreamPlaybackState.IDLE, null)
                            }
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Log.e(tag, "ExoPlayer error on pane $paneIndex: ${error.errorCodeName} - ${error.message}", error)
                        if (autoRetryCount < maxAutoRetries && !currentStreamUrl.isNullOrBlank()) {
                            autoRetryCount++
                            Log.i(tag, "Attempting auto-recovery retry $autoRetryCount for pane $paneIndex in 1200ms...")
                            handler.postDelayed({
                                retry()
                            }, 1200L)
                        } else {
                            onStateChange(paneIndex, StreamPlaybackState.ERROR, error.message ?: "Stream playback failed")
                        }
                    }
                })
            }

        startDiagnosticsPolling()
    }

    private fun startDiagnosticsPolling() {
        handler.removeCallbacks(diagnosticsRunnable)
        handler.postDelayed(diagnosticsRunnable, 1500L)
    }

    private fun pollDiagnostics() {
        val player = exoPlayer ?: return
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

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(com.example.data.tablo.TabloAuthService.LOCAL_USER_AGENT)
            .setConnectTimeoutMs(10000)
            .setReadTimeoutMs(15000)
            .setAllowCrossProtocolRedirects(true)
            .setKeepPostFor302Redirects(true)

        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

        // Tablo ATSC live HLS configuration with 3000ms target offset
        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(url))
            .setMimeType(MimeTypes.APPLICATION_M3U8)
            .setLiveConfiguration(
                MediaItem.LiveConfiguration.Builder()
                    .setTargetOffsetMs(3000)
                    .setMinOffsetMs(1500)
                    .setMaxOffsetMs(7500)
                    .build()
            )
            .build()

        val hlsMediaSource = HlsMediaSource.Factory(dataSourceFactory)
            .setAllowChunklessPreparation(false)
            .createMediaSource(mediaItem)

        player.setMediaSource(hlsMediaSource)
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
        handler.removeCallbacks(diagnosticsRunnable)
        handler.removeCallbacksAndMessages(null)
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        onStateChange(paneIndex, StreamPlaybackState.IDLE, null)
    }

    fun release() {
        handler.removeCallbacks(diagnosticsRunnable)
        handler.removeCallbacksAndMessages(null)
        stop()
        exoPlayer?.release()
        exoPlayer = null
    }
}
