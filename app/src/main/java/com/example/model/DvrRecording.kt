package com.example.model

/**
 * Cloud DVR recording model matching YouTube TV's Library structure:
 * Categorized by New to You, Shows, Movies, Sports, and Events.
 * Unlimited storage, kept for 9 months.
 */
enum class DvrCategory(val displayName: String) {
    ALL("All"),
    NEW_TO_YOU("New to You"),
    SHOWS("Shows"),
    MOVIES("Movies"),
    SPORTS("Sports"),
    EVENTS("Events")
}

data class DvrRecording(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: DvrCategory,
    val network: String,
    val channelNumber: String,
    val durationMinutes: Int,
    val recordedDate: String,
    val expiresMonthsRemaining: Int = 9,
    val channelId: String? = null,
    val isWatched: Boolean = false
)
