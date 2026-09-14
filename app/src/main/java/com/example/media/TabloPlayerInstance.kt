package com.example.media

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import com.example.model.StreamPlaybackState

/**
 * Dedicated ExoPlayer instance for a single multiview video pane.
 * Configured specifically for Amazon Fire TV / Android TV hardware acceleration,
 * smooth live HLS chunk playback, and buffer underrun prevention.
 */
@OptIn(UnstableApi::class)
class TabloPlayerInstance(
    private val context: Context,
    val paneIndex: Int,
    private val onStateChange: (paneIndex: Int, state: StreamPlaybackState, error: String?) -> Unit
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

    private var isMutedInternally: Boolean = true
    private var autoRetryCount: Int = 0
    private val maxAutoRetries: Int = 2
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    init {
        initPlayer()
    }

    private fun initPlayer() {
        if (exoPlayer != null) return

        // Buffer configuration tailored for Tablo ATSC live HLS rolling window (~6-8s).
        // A minimal bufferForPlaybackMs (500ms) with a 1500ms minBufferMs ensures playback starts
        // immediately as soon as the first segment is parsed, preventing infinite buffering stalls.
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 1500,
                /* maxBufferMs = */ 8000,
                /* bufferForPlaybackMs = */ 500,
                /* bufferForPlaybackAfterRebufferMs = */ 1000
            )
            .setBackBuffer(
                /* backBufferDurationMs = */ 3000,
                /* retainBackBufferFromKeyframe = */ true
            )
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
                            Log.i(tag, "Attempting auto-recovery retry $autoRetryCount for pane $paneIndex in 1000ms...")
                            handler.postDelayed({
                                retry()
                            }, 1000L)
                        } else {
                            onStateChange(paneIndex, StreamPlaybackState.ERROR, error.message ?: "Stream playback failed")
                        }
                    }
                })
            }
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

        // Tablo ATSC live HLS configuration
        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(url))
            .setMimeType(MimeTypes.APPLICATION_M3U8)
            .setLiveConfiguration(
                MediaItem.LiveConfiguration.Builder()
                    .setTargetOffsetMs(1500)
                    .setMinOffsetMs(500)
                    .setMaxOffsetMs(6000)
                    .build()
            )
            .build()

        // CRITICAL: setAllowChunklessPreparation(false)
        // Tablo live ATSC manifests do not specify track codecs or EXT-X-MAP initialization tags.
        // Chunkless preparation causes ExoPlayer to stall indefinitely in STATE_BUFFERING.
        // Disabling chunkless preparation forces ExoPlayer to parse media chunks, discovering the
        // H.264 video and audio tracks immediately.
        val hlsMediaSource = HlsMediaSource.Factory(dataSourceFactory)
            .setAllowChunklessPreparation(false)
            .createMediaSource(mediaItem)

        player.setMediaSource(hlsMediaSource)
        player.prepare()
        player.playWhenReady = true
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
            // Conserve audio decoders: only the active pane decodes audio
            val trackParams = player.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, !isActive)
                .build()
            player.trackSelectionParameters = trackParams
        }
    }

    fun setExplicitMute(isMuted: Boolean) {
        setAudioActive(!isMuted)
    }

    fun stop() {
        handler.removeCallbacksAndMessages(null)
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        onStateChange(paneIndex, StreamPlaybackState.IDLE, null)
    }

    fun release() {
        handler.removeCallbacksAndMessages(null)
        stop()
        exoPlayer?.release()
        exoPlayer = null
    }
}
