package com.example.media

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import com.example.model.StreamPlaybackState

/**
 * Manages up to 4 simultaneous hardware-accelerated ExoPlayer instances.
 * Guarantees that only the user-focused active pane outputs audio.
 * Allows independent channel switching and failure isolation per pane.
 */
class MultiviewPlayerManager(
    private val context: Context,
    private val onPaneStateChange: (paneIndex: Int, state: StreamPlaybackState, error: String?) -> Unit
) {
    private val players = arrayOfNulls<TabloPlayerInstance>(4)
    private var activeAudioPaneIndex = 0
    private var isGloballyMuted = false

    fun getPlayer(paneIndex: Int): ExoPlayer? {
        ensurePlayerExists(paneIndex)
        return players[paneIndex]?.exoPlayer
    }

    private fun ensurePlayerExists(paneIndex: Int): TabloPlayerInstance {
        val existing = players[paneIndex]
        if (existing != null && existing.exoPlayer != null) {
            return existing
        }
        val instance = TabloPlayerInstance(context, paneIndex, onPaneStateChange)
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

    /**
     * Switch active audio to the specified pane.
     * Audio follows focus: only the active pane produces sound.
     */
    fun setActiveAudioPane(paneIndex: Int) {
        activeAudioPaneIndex = paneIndex.coerceIn(0, 3)
        updateAudioRouting()
    }

    fun setGlobalMute(muted: Boolean) {
        isGloballyMuted = muted
        updateAudioRouting()
    }

    private fun updateAudioRouting() {
        for (i in 0 until 4) {
            val player = players[i] ?: continue
            val shouldHaveAudio = !isGloballyMuted && (i == activeAudioPaneIndex)
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
