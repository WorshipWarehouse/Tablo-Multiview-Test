package com.example.model

/**
 * Represents a physical Tablo DVR device discovered or registered on the local network.
 */
data class TabloDevice(
    val serverId: String,
    val name: String,
    val host: String,
    val port: Int = 8885,
    val modelName: String = "Tablo Gen 4",
    val modelType: String? = null,
    val version: String = "",
    val timezone: String = "",
    val tunerCount: Int = 2,
    val isWifi: Boolean = true,
    val lastConnected: Long = System.currentTimeMillis()
) {
    val baseUrl: String
        get() = "http://$host:$port"

    val displaySubtitle: String
        get() = "$host • $modelName${if (tunerCount > 0) " ($tunerCount tuners)" else ""}"
}

enum class TabloConnectionState {
    DISCONNECTED,
    DISCOVERING,
    CONNECTING,
    CONNECTED,
    ERROR
}
