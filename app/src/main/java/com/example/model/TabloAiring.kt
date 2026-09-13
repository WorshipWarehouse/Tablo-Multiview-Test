package com.example.model

/**
 * Information regarding a currently playing or scheduled program airing.
 */
data class TabloAiring(
    val airingId: String,
    val channelId: String,
    val title: String,
    val episodeTitle: String? = null,
    val description: String? = null,
    val startTime: Long = 0L,
    val durationSeconds: Int = 0,
    val releaseYear: Int? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null
) {
    val endTime: Long
        get() = startTime + (durationSeconds * 1000L)

    val isLiveNow: Boolean
        get() {
            val now = System.currentTimeMillis()
            return startTime in 1..now && now <= endTime
        }
}

/**
 * Result of requesting to watch a live channel via Tablo API.
 */
data class TabloStream(
    val channelId: String,
    val playlistUrl: String,
    val token: String? = null,
    val expires: String? = null
)
