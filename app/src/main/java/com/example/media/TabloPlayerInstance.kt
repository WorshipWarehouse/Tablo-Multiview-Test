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

    init {
        initPlayer()
    }

    private fun initPlayer() {
        if (exoPlayer != null) return

        // Optimized for real-time broadcast HLS chunk buffering over local Wi-Fi/Ethernet.
        // Tablo ATSC tuners produce rolling windows of ~6-8 seconds total.
        // A low initial buffer (500ms) allows immediate playback start on the first available chunk,
        // avoiding infinite buffering traps caused by oversized buffer thresholds.
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 2500,
                /* maxBufferMs = */ 12000,
                /* bufferForPlaybackMs = */ 500,
                /* bufferForPlaybackAfterRebufferMs = */ 1200
            )
            .setBackBuffer(
                /* backBufferDurationMs = */ 5000,
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
                                onStateChange(paneIndex, StreamPlaybackState.PLAYING, null)
                            }
                            Player.STATE_ENDED -> {
                                onStateChange(paneIndex, StreamPlaybackState.IDLE, null)
                            }
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Log.e(tag, "ExoPlayer error on pane $paneIndex: ${error.errorCodeName} - ${error.message}", error)
                        onStateChange(paneIndex, StreamPlaybackState.ERROR, error.message ?: "Stream playback failed")
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

        // For live broadcast HLS from Tablo tuners, allow ExoPlayer to align with
        // the HLS manifest's live edge rather than forcing fixed 6s offsets that
        // exceed initial window duration.
        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(url))
            .setMimeType(MimeTypes.APPLICATION_M3U8)
            .build()

        val hlsMediaSource = HlsMediaSource.Factory(dataSourceFactory)
            .setAllowChunklessPreparation(true)
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
        exoPlayer?.volume = if (isActive) 1.0f else 0.0f
    }

    fun setExplicitMute(isMuted: Boolean) {
        isMutedInternally = isMuted
        exoPlayer?.volume = if (isMuted) 0.0f else 1.0f
    }

    fun stop() {
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        onStateChange(paneIndex, StreamPlaybackState.IDLE, null)
    }

    fun release() {
        stop()
        exoPlayer?.release()
        exoPlayer = null
    }
}
