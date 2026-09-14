package com.example.model

/**
 * Represents a broadcast or virtual channel provided by the Tablo DVR.
 */
data class TabloChannel(
    val id: String,
    val major: Int,
    val minor: Int,
    val network: String,
    val callSign: String,
    val resolution: String? = null,
    val audio: String? = null,
    val logoUrl: String? = null,
    val channelPath: String = "/guide/channels/$id",
    val streamUrl: String? = null,
    val liveEventTitle: String? = null,
    val scoreBug: String? = null
) {
    val channelNumberFormatted: String
        get() = if (minor > 0) "$major.$minor" else "$major"

    val displayTitle: String
        get() = if (network.isNotBlank() && network != callSign) {
            "$channelNumberFormatted  $network"
        } else if (callSign.isNotBlank()) {
            "$channelNumberFormatted  $callSign"
        } else {
            "Channel $channelNumberFormatted"
        }

    val displaySubtitle: String
        get() = buildString {
            if (callSign.isNotBlank() && callSign != network) append(callSign)
            if (!resolution.isNullOrBlank()) {
                if (isNotEmpty()) append(" • ")
                append(resolution)
            }
        }
}
