package com.example.media

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import com.example.model.StreamDiagnostics
import com.example.model.StreamPlaybackState
import com.example.model.StreamQuality

/**
 * Manages up to 4 simultaneous hardware-accelerated ExoPlayer instances.
 * Guarantees that only the user-focused active pane outputs audio,
 * while allowing individual audio unmuting and per-pane quality controls.
 */
class MultiviewPlayerManager(
    private val context: Context,
    private val onPaneStateChange: (paneIndex: Int, state: StreamPlaybackState, error: String?) -> Unit,
    private val onPaneDiagnostics: ((paneIndex: Int, diagnostics: StreamDiagnostics) -> Unit)? = null
) {
    private val players = arrayOfNulls<TabloPlayerInstance>(4)
    private var activeAudioPaneIndex = 0
    private var isGloballyMuted = false
    private val individualMuteOverrides = BooleanArray(4) { false }

    fun getPlayer(paneIndex: Int): ExoPlayer? {
        return players.getOrNull(paneIndex)?.exoPlayer
    }

    private fun ensurePlayerExists(paneIndex: Int): TabloPlayerInstance {
        val existing = players[paneIndex]
        if (existing != null && existing.exoPlayer != null) {
            return existing
        }
        val instance = TabloPlayerInstance(
            context = context,
            paneIndex = paneIndex,
            onStateChange = onPaneStateChange,
            onDiagnostics = onPaneDiagnostics
        )
        players[paneIndex] = instance
        updateAudioRouting()
        return instance
    }

    fun playPaneStream(paneIndex: Int, url: String, channelId: String, token: String?) {
        val player = ensurePlayerExists(paneIndex)
        player.playStream(url, channelId, token)
        updateAudioRouting()
    }

    fun retryPane(paneIndex: Int) {
        players[paneIndex]?.retry()
    }

    fun setPaneQuality(paneIndex: Int, quality: StreamQuality) {
        players.getOrNull(paneIndex)?.setQuality(quality)
    }

    /**
     * Switch active audio to the specified pane.
     * Audio follows focus by default: only the active pane produces sound.
     */
    fun setActiveAudioPane(paneIndex: Int) {
        activeAudioPaneIndex = paneIndex.coerceIn(0, 3)
        updateAudioRouting()
    }

    fun togglePaneAudio(paneIndex: Int) {
        if (paneIndex in 0..3) {
            individualMuteOverrides[paneIndex] = !individualMuteOverrides[paneIndex]
            updateAudioRouting()
        }
    }

    fun isPaneAudioActive(paneIndex: Int): Boolean {
        if (isGloballyMuted) return false
        if (individualMuteOverrides[paneIndex]) return false
        return (paneIndex == activeAudioPaneIndex)
    }

    fun setGlobalMute(muted: Boolean) {
        isGloballyMuted = muted
        updateAudioRouting()
    }

    private fun updateAudioRouting() {
        for (i in 0 until 4) {
            val player = players[i] ?: continue
            val shouldHaveAudio = !isGloballyMuted && (i == activeAudioPaneIndex) && !individualMuteOverrides[i]
            player.setAudioActive(shouldHaveAudio)
        }
    }

    fun getStreamToken(paneIndex: Int): String? = players.getOrNull(paneIndex)?.currentStreamToken

    fun stopPane(paneIndex: Int): String? {
        val token = players.getOrNull(paneIndex)?.currentStreamToken
        players.getOrNull(paneIndex)?.stop()
        return token
    }

    fun releasePane(paneIndex: Int): String? {
        val token = players.getOrNull(paneIndex)?.currentStreamToken
        players.getOrNull(paneIndex)?.release()
        players[paneIndex] = null
        return token
    }

    fun pauseAll() {
        for (i in 0 until 4) {
            players[i]?.exoPlayer?.playWhenReady = false
        }
    }

    fun resumeAll() {
        for (i in 0 until 4) {
            players[i]?.exoPlayer?.playWhenReady = true
        }
        updateAudioRouting()
    }

    fun releaseAll(): List<String> {
        val releasedTokens = mutableListOf<String>()
        for (i in 0 until 4) {
            val token = players[i]?.currentStreamToken
            if (!token.isNullOrBlank()) {
                releasedTokens.add(token)
            }
            players[i]?.release()
            players[i] = null
        }
        return releasedTokens
    }
}
