package com.example.data.tablo

import com.example.data.local.TabloPreferences
import com.example.model.TabloConnectionState
import com.example.model.TabloDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages Tablo physical device registration, connection state, and persistence.
 */
class TabloDeviceRepository(
    private val preferences: TabloPreferences,
    private val apiClient: TabloApiClient = TabloApiClient(),
    private val discoveryService: TabloDiscoveryService
) {
    private val _connectionState = MutableStateFlow(TabloConnectionState.DISCONNECTED)
    val connectionState: StateFlow<TabloConnectionState> = _connectionState.asStateFlow()

    private val _currentDevice = MutableStateFlow<TabloDevice?>(null)
    val currentDevice: StateFlow<TabloDevice?> = _currentDevice.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<TabloDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<TabloDevice>> = _discoveredDevices.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        // Load existing registered Tablo if present
        val saved = preferences.getRegisteredDevice()
        if (saved != null) {
            _currentDevice.value = saved
        }
    }

    /**
     * Initial startup reconnect: attempts to ping the registered Tablo over LAN.
     */
    suspend fun tryReconnect(): Boolean {
        val saved = preferences.getRegisteredDevice() ?: return false
        _connectionState.value = TabloConnectionState.CONNECTING
        _errorMessage.value = null

        val result = apiClient.getServerInfo(saved.host, saved.port)
        return if (result.isSuccess) {
            val updated = result.getOrThrow()
            preferences.saveRegisteredDevice(updated)
            _currentDevice.value = updated
            _connectionState.value = TabloConnectionState.CONNECTED
            true
        } else {
            _connectionState.value = TabloConnectionState.ERROR
            _errorMessage.value = "Can't reach your Tablo at ${saved.host}. Make sure your Tablo is powered on and connected to the same network."
            false
        }
    }

    /**
     * Test and register a device via manual IP address.
     */
    suspend fun connectManualIp(host: String, port: Int = 8885): Result<TabloDevice> {
        val cleanHost = host.trim()
        if (cleanHost.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter a valid IP address"))
        }

        _connectionState.value = TabloConnectionState.CONNECTING
        _errorMessage.value = null

        val result = apiClient.getServerInfo(cleanHost, port)
        if (result.isSuccess) {
            val device = result.getOrThrow()
            registerDevice(device)
            return Result.success(device)
        } else {
            _connectionState.value = TabloConnectionState.ERROR
            val msg = "Could not connect to Tablo at $cleanHost. Check IP address and verify device is on LAN."
            _errorMessage.value = msg
            return Result.failure(Exception(msg))
        }
    }

    /**
     * Start network discovery.
     */
    suspend fun discoverDevices(): List<TabloDevice> {
        _connectionState.value = TabloConnectionState.DISCOVERING
        _errorMessage.value = null
        try {
            val list = discoveryService.discoverTablos()
            _discoveredDevices.value = list
            _connectionState.value = if (_currentDevice.value != null) TabloConnectionState.CONNECTED else TabloConnectionState.DISCONNECTED
            return list
        } catch (e: Exception) {
            _connectionState.value = TabloConnectionState.ERROR
            _errorMessage.value = "Discovery error: ${e.localizedMessage}"
            return emptyList()
        }
    }

    fun registerDevice(device: TabloDevice) {
        preferences.saveRegisteredDevice(device)
        _currentDevice.value = device
        _connectionState.value = TabloConnectionState.CONNECTED
        _errorMessage.value = null
    }

    fun forgetDevice() {
        preferences.clearRegisteredDevice()
        _currentDevice.value = null
        _connectionState.value = TabloConnectionState.DISCONNECTED
        _errorMessage.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
