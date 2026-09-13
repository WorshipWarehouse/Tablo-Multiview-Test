package com.example.data.tablo

import com.example.data.local.TabloPreferences
import com.example.model.TabloConnectionState
import com.example.model.TabloDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages Tablo physical device registration, connection state, authentication, and persistence.
 */
class TabloDeviceRepository(
    private val preferences: TabloPreferences,
    val apiClient: TabloApiClient = TabloApiClient(),
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

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    init {
        val saved = preferences.getRegisteredDevice()
        if (saved != null) {
            _currentDevice.value = saved
        }
    }

    /**
     * Authenticate with Tablo Cloud (LighthouseTV) using user's Tablo account.
     * Tablo Gen 4 requires this to discover registered DVRs, receive the device-scoped token,
     * and unlock local live TV streaming.
     */
    suspend fun loginWithTabloAccount(email: String, pass: String): Result<List<TabloDevice>> {
        val cleanEmail = email.trim()
        val cleanPass = pass.trim()
        if (cleanEmail.isBlank() || cleanPass.isBlank()) {
            val err = "Please enter both your Tablo account email and password."
            _errorMessage.value = err
            return Result.failure(IllegalArgumentException(err))
        }

        _isAuthenticating.value = true
        _connectionState.value = TabloConnectionState.CONNECTING
        _errorMessage.value = null

        try {
            // 1. Cloud login
            val loginResult = apiClient.authService.login(cleanEmail, cleanPass)
            if (loginResult.isFailure) {
                val err = loginResult.exceptionOrNull()?.message ?: "Login failed"
                _errorMessage.value = err
                _connectionState.value = TabloConnectionState.ERROR
                return Result.failure(Exception(err))
            }

            val (accessToken, _) = loginResult.getOrThrow()

            // 2. Fetch account profile and devices
            val accountResult = apiClient.authService.getAccountDevices(accessToken)
            if (accountResult.isFailure) {
                val err = accountResult.exceptionOrNull()?.message ?: "Could not retrieve account devices"
                _errorMessage.value = err
                _connectionState.value = TabloConnectionState.ERROR
                return Result.failure(Exception(err))
            }

            val accountData = accountResult.getOrThrow()
            val devices = accountData.devices

            if (devices.isEmpty()) {
                val err = "No Tablo devices found under this account. Please verify your Tablo is set up in the Tablo mobile app."
                _errorMessage.value = err
                _connectionState.value = TabloConnectionState.ERROR
                return Result.failure(Exception(err))
            }

            _discoveredDevices.value = devices

            // Save credentials and session tokens
            preferences.saveAuthSession(
                email = cleanEmail,
                password = cleanPass,
                accessToken = accessToken,
                profileId = accountData.profileId
            )

            // 3. If there is at least one device, select it
            val targetDevice = devices.first()
            val selectRes = selectAndRegisterDevice(targetDevice, accessToken, accountData.profileId)
            if (selectRes.isFailure) {
                return selectRes
            }

            return Result.success(devices)
        } catch (e: Exception) {
            val err = e.localizedMessage ?: "Authentication error"
            _errorMessage.value = err
            _connectionState.value = TabloConnectionState.ERROR
            return Result.failure(e)
        } finally {
            _isAuthenticating.value = false
        }
    }

    /**
     * Select a specific Tablo device, acquire its lighthouse token, and verify connectivity.
     */
    suspend fun selectAndRegisterDevice(
        device: TabloDevice,
        accessToken: String? = preferences.getAccessToken(),
        profileId: String? = preferences.getProfileId()
    ): Result<List<TabloDevice>> {
        val token = accessToken ?: preferences.getAccessToken()
        val pid = profileId ?: preferences.getProfileId() ?: ""

        if (!token.isNullOrBlank() && device.serverId.isNotBlank()) {
            val selectRes = apiClient.authService.selectDevice(token, pid, device.serverId)
            if (selectRes.isSuccess) {
                val lighthouseToken = selectRes.getOrThrow()
                preferences.saveLighthouseToken(lighthouseToken)
            }
        }

        // Test local reachability: ping device on LAN
        val pingRes = apiClient.ping(device.host, device.port)
        val verifiedDevice = if (pingRes.isSuccess) {
            val sid = pingRes.getOrThrow()
            device.copy(
                serverId = if (sid.isNotBlank()) sid else device.serverId,
                lastConnected = System.currentTimeMillis()
            )
        } else {
            device
        }

        registerDevice(verifiedDevice)
        return Result.success(listOf(verifiedDevice))
    }

    /**
     * Reconnect to the registered Tablo over LAN, refreshing credentials if needed.
     */
    suspend fun tryReconnect(): Boolean {
        val saved = preferences.getRegisteredDevice() ?: return false
        _connectionState.value = TabloConnectionState.CONNECTING
        _errorMessage.value = null

        // First try silent re-auth if credentials exist
        val email = preferences.getAuthEmail()
        val pass = preferences.getAuthPassword()
        if (!email.isNullOrBlank() && !pass.isNullOrBlank()) {
            try {
                val loginRes = apiClient.authService.login(email, pass)
                if (loginRes.isSuccess) {
                    val (accessToken, _) = loginRes.getOrThrow()
                    val pid = preferences.getProfileId() ?: ""
                    if (saved.serverId.isNotBlank()) {
                        val selRes = apiClient.authService.selectDevice(accessToken, pid, saved.serverId)
                        if (selRes.isSuccess) {
                            preferences.saveAuthSession(email, pass, accessToken, pid, selRes.getOrThrow())
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // Check local device
        val pingRes = apiClient.ping(saved.host, saved.port)
        if (pingRes.isSuccess) {
            _currentDevice.value = saved
            _connectionState.value = TabloConnectionState.CONNECTED
            return true
        }

        val result = apiClient.getServerInfo(saved.host, saved.port)
        return if (result.isSuccess) {
            val updated = result.getOrThrow()
            preferences.saveRegisteredDevice(updated)
            _currentDevice.value = updated
            _connectionState.value = TabloConnectionState.CONNECTED
            true
        } else {
            _connectionState.value = TabloConnectionState.ERROR
            _errorMessage.value = "Can't reach your Tablo at ${saved.host}. Ensure your Tablo is on the same Wi-Fi / LAN."
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

        // 1. Unauthenticated ping check
        val pingRes = apiClient.ping(cleanHost, port)
        var serverId = ""
        if (pingRes.isSuccess) {
            serverId = pingRes.getOrThrow()
        }

        // 2. Query /server/info with HMAC
        val result = apiClient.getServerInfo(cleanHost, port)
        if (result.isSuccess) {
            val device = result.getOrThrow()
            registerDevice(device)
            return Result.success(device)
        }

        // If ping succeeded, we know the hardware is definitely at this IP!
        if (pingRes.isSuccess) {
            val existing = preferences.getRegisteredDevice()
            val device = TabloDevice(
                serverId = serverId.ifBlank { existing?.serverId ?: "SID_GEN4" },
                name = existing?.name ?: "Tablo Gen 4",
                host = cleanHost,
                port = port,
                modelName = existing?.modelName ?: "Tablo Gen 4",
                tunerCount = 2,
                lastConnected = System.currentTimeMillis()
            )
            registerDevice(device)
            return Result.success(device)
        }

        _connectionState.value = TabloConnectionState.ERROR
        val msg = "Could not connect to Tablo at $cleanHost. Check IP address and verify device is powered on."
        _errorMessage.value = msg
        return Result.failure(Exception(msg))
    }

    /**
     * Start network discovery.
     */
    suspend fun discoverDevices(): List<TabloDevice> {
        _connectionState.value = TabloConnectionState.DISCOVERING
        _errorMessage.value = null
        try {
            // First check if user is logged in to cloud:
            val email = preferences.getAuthEmail()
            val pass = preferences.getAuthPassword()
            if (!email.isNullOrBlank() && !pass.isNullOrBlank()) {
                val cloudRes = loginWithTabloAccount(email, pass)
                if (cloudRes.isSuccess) {
                    return cloudRes.getOrThrow()
                }
            }

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
        preferences.clearAuthSession()
        _currentDevice.value = null
        _connectionState.value = TabloConnectionState.DISCONNECTED
        _errorMessage.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
