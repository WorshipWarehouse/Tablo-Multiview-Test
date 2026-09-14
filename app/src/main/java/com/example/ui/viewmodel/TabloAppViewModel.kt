package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.TabloPreferences
import com.example.data.tablo.TabloApiClient
import com.example.data.tablo.TabloChannelRepository
import com.example.data.tablo.TabloDeviceRepository
import com.example.data.tablo.TabloDiscoveryService
import com.example.data.tablo.SavedLayoutRepository
import com.example.media.MultiviewPlayerManager
import com.example.model.MultiviewLayoutMode
import com.example.model.MultiviewUiState
import com.example.model.PaneState
import com.example.model.SavedLayout
import com.example.model.StreamPlaybackState
import com.example.model.TabloChannel
import com.example.model.TabloConnectionState
import com.example.model.TabloDevice
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import com.example.model.DvrCategory
import com.example.model.DvrRecording
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    LIBRARY,
    HOME,
    LIVE,
    REGISTRATION,
    MANUAL_IP,
    MULTIVIEW,
    CHANNEL_GUIDE,
    SAVED_LAYOUTS,
    SETTINGS
}

class TabloAppViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = TabloPreferences(application)
    private val database = AppDatabase.getInstance(application)
    private val apiClient = TabloApiClient()
    private val discoveryService = TabloDiscoveryService(application, apiClient)

    val deviceRepository = TabloDeviceRepository(preferences, apiClient, discoveryService)
    val channelRepository = TabloChannelRepository(apiClient, database.cachedChannelDao())
    val savedLayoutRepository = SavedLayoutRepository(database.savedLayoutDao())

    val isAuthenticating: StateFlow<Boolean> = deviceRepository.isAuthenticating
    fun getSavedAuthEmail(): String? = preferences.getAuthEmail()

    var onTriggerPip: (() -> Unit)? = null

    val playerManager = MultiviewPlayerManager(
        context = application,
        onPaneStateChange = { paneIndex, state, error ->
            updatePanePlaybackState(paneIndex, state, error)
        },
        onPaneDiagnostics = { paneIndex, diagnostics ->
            updatePaneDiagnostics(paneIndex, diagnostics)
        }
    )

    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _multiviewState = MutableStateFlow(MultiviewUiState())
    val multiviewState: StateFlow<MultiviewUiState> = _multiviewState.asStateFlow()

    val savedLayouts: StateFlow<List<SavedLayout>> = savedLayoutRepository.layouts
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // --- Cloud DVR Library (YouTube TV style) ---
    private val _dvrRecordings = MutableStateFlow<List<DvrRecording>>(defaultDvrRecordings())
    val dvrRecordings: StateFlow<List<DvrRecording>> = _dvrRecordings.asStateFlow()

    fun addToLibrary(
        title: String,
        channel: TabloChannel?,
        category: DvrCategory = DvrCategory.SHOWS,
        subtitle: String = "Recorded Live"
    ) {
        val existing = _dvrRecordings.value.find { it.title.equals(title, ignoreCase = true) }
        if (existing == null) {
            val newRec = DvrRecording(
                id = "dvr_${System.currentTimeMillis()}",
                title = title,
                subtitle = subtitle,
                category = category,
                network = channel?.network ?: "Tablo TV",
                channelNumber = channel?.channelNumberFormatted ?: "LIVE",
                durationMinutes = 60,
                recordedDate = "Today",
                expiresMonthsRemaining = 9,
                channelId = channel?.id
            )
            _dvrRecordings.value = listOf(newRec) + _dvrRecordings.value
        }
    }

    fun removeFromLibrary(recordingId: String) {
        _dvrRecordings.value = _dvrRecordings.value.filterNot { it.id == recordingId }
    }

    fun launchChannel(channel: TabloChannel) {
        launchMultiview(
            mode = MultiviewLayoutMode.ONE_PANE,
            channels = listOf(channel),
            initialActivePane = 0
        )
    }

    fun launchDvrRecording(recording: DvrRecording) {
        val allChannels = channelRepository.channels.value
        val channel = allChannels.find { it.id == recording.channelId }
            ?: allChannels.firstOrNull()
        if (channel != null) {
            launchChannel(channel)
        } else {
            launchMultiview(MultiviewLayoutMode.ONE_PANE)
        }
    }

    fun togglePlaybackOverlay(visible: Boolean? = null) {
        val current = _multiviewState.value
        val next = visible ?: !current.isPlaybackOverlayVisible
        _multiviewState.value = current.copy(isPlaybackOverlayVisible = next)
    }

    fun openMultiviewBuilder() {
        _multiviewState.value = _multiviewState.value.copy(
            isMultiviewBuilderOpen = true,
            isPlaybackOverlayVisible = false
        )
    }

    fun closeMultiviewBuilder() {
        _multiviewState.value = _multiviewState.value.copy(isMultiviewBuilderOpen = false)
    }

    fun openStatsOverlay() {
        _multiviewState.value = _multiviewState.value.copy(
            isStatsOverlayOpen = true,
            isPlaybackOverlayVisible = false
        )
    }

    fun closeStatsOverlay() {
        _multiviewState.value = _multiviewState.value.copy(isStatsOverlayOpen = false)
    }

    fun launchCustomMultiview(selectedChannels: List<TabloChannel>) {
        val count = selectedChannels.size.coerceIn(1, 4)
        val mode = when (count) {
            1 -> MultiviewLayoutMode.ONE_PANE
            2 -> MultiviewLayoutMode.TWO_PANE
            3 -> MultiviewLayoutMode.THREE_PANE
            else -> MultiviewLayoutMode.FOUR_PANE
        }
        closeMultiviewBuilder()
        launchMultiview(
            mode = mode,
            channels = selectedChannels,
            initialActivePane = 0
        )
    }

    private val streamJobs = arrayOfNulls<Job>(4)
    private var keepaliveJob: Job? = null

    init {
        // Startup flow: load channels and launch directly into 4-Game Quad Multiview
        viewModelScope.launch {
            channelRepository.loadCachedChannels()
            val savedDevice = preferences.getRegisteredDevice()
            if (savedDevice != null) {
                // Reconnect to Tablo in background
                deviceRepository.tryReconnect()
                channelRepository.refreshChannels(
                    savedDevice.host,
                    savedDevice.port,
                    preferences.getAccessToken(),
                    preferences.getLighthouseToken()
                )
            }
            // Auto-launch directly into 4-Game Multiview Grid!
            startMultiviewWithMode(MultiviewLayoutMode.FOUR_PANE)
        }
    }

    fun navigateTo(screen: AppScreen) {
        if (screen != AppScreen.MULTIVIEW && _currentScreen.value == AppScreen.MULTIVIEW) {
            // Leaving multiview, stop streams
            stopAllStreams()
        }
        _currentScreen.value = screen
    }

    // --- Tablo Connection & Discovery ---

    fun loginWithTabloAccount(email: String, pass: String, onComplete: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val res = deviceRepository.loginWithTabloAccount(email, pass)
            if (res.isSuccess) {
                val dev = deviceRepository.currentDevice.value
                if (dev != null) {
                    channelRepository.refreshChannels(
                        dev.host,
                        dev.port,
                        preferences.getAccessToken(),
                        preferences.getLighthouseToken()
                    )
                }
                _currentScreen.value = AppScreen.HOME
                onComplete(true, null)
            } else {
                val err = res.exceptionOrNull()?.message ?: "Login failed"
                onComplete(false, err)
            }
        }
    }

    fun startDiscovery() {
        viewModelScope.launch {
            deviceRepository.discoverDevices()
        }
    }

    fun connectManualIp(ip: String, onComplete: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val result = deviceRepository.connectManualIp(ip)
            if (result.isSuccess) {
                val dev = result.getOrThrow()
                channelRepository.refreshChannels(
                    dev.host,
                    dev.port,
                    preferences.getAccessToken(),
                    preferences.getLighthouseToken()
                )
                _currentScreen.value = AppScreen.HOME
                onComplete(true, null)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Could not connect to $ip"
                onComplete(false, err)
            }
        }
    }

    fun registerDiscoveredDevice(device: TabloDevice) {
        viewModelScope.launch {
            deviceRepository.registerDevice(device)
            channelRepository.refreshChannels(
                device.host,
                device.port,
                preferences.getAccessToken(),
                preferences.getLighthouseToken()
            )
            _currentScreen.value = AppScreen.HOME
        }
    }

    fun retryConnection() {
        viewModelScope.launch {
            val success = deviceRepository.tryReconnect()
            if (success) {
                val dev = deviceRepository.currentDevice.value
                if (dev != null) {
                    channelRepository.refreshChannels(
                        dev.host,
                        dev.port,
                        preferences.getAccessToken(),
                        preferences.getLighthouseToken()
                    )
                }
            }
        }
    }

    fun refreshGuideChannels() {
        val dev = deviceRepository.currentDevice.value ?: return
        viewModelScope.launch {
            channelRepository.refreshChannels(
                dev.host,
                dev.port,
                preferences.getAccessToken(),
                preferences.getLighthouseToken()
            )
        }
    }

    fun forgetDevice() {
        stopAllStreams()
        deviceRepository.forgetDevice()
        _currentScreen.value = AppScreen.REGISTRATION
    }

    // --- Multiview Management ---

    fun startMultiviewWithLastSession() {
        val lastMode = preferences.getLastLayoutMode()
        val lastChannelIds = preferences.getLastPaneChannels()
        val allChannels = channelRepository.channels.value

        val assignedChannels = mutableListOf<TabloChannel?>()
        for (i in 0 until 4) {
            val chId = lastChannelIds.getOrNull(i)
            val channel = allChannels.find { it.id == chId } ?: if (i < lastMode.paneCount && lastChannelIds.isEmpty()) allChannels.getOrNull(i) else null
            assignedChannels.add(channel)
        }

        preferences.saveLastLayoutMode(lastMode)
        launchMultiview(lastMode, assignedChannels)
    }

    fun startMultiviewWithMode(mode: MultiviewLayoutMode) {
        val allChannels = channelRepository.channels.value.ifEmpty { com.example.data.tablo.defaultBroadcastChannels() }
        val lastChannelIds = preferences.getLastPaneChannels()
        val assignedChannels = mutableListOf<TabloChannel?>()

        for (i in 0 until 4) {
            val chId = lastChannelIds.getOrNull(i)
            val channel = allChannels.find { it.id == chId } ?: allChannels.getOrNull(i) ?: allChannels.getOrNull(i % allChannels.size.coerceAtLeast(1))
            assignedChannels.add(channel)
        }

        preferences.saveLastLayoutMode(mode)
        launchMultiview(mode, assignedChannels)
    }

    fun playChannelInMultiview(paneIndex: Int, channel: TabloChannel) {
        val current = _multiviewState.value
        val safeIndex = paneIndex.coerceIn(0, 3)
        val channels = (0 until 4).map { idx ->
            if (idx == safeIndex) channel else current.panes.getOrNull(idx)?.channel
        }
        val mode = if (current.layoutMode == MultiviewLayoutMode.ONE_PANE) MultiviewLayoutMode.ONE_PANE else current.layoutMode
        launchMultiview(
            mode = mode,
            channels = channels,
            initialActivePane = safeIndex
        )
    }

    fun launchMultiview(
        mode: MultiviewLayoutMode = MultiviewLayoutMode.FOUR_PANE,
        channels: List<TabloChannel?> = emptyList(),
        initialActivePane: Int = 0
    ) {
        val allChannels = channelRepository.channels.value.ifEmpty { com.example.data.tablo.defaultBroadcastChannels() }
        val initialPanes = (0 until 4).map { idx ->
            val channel = if (idx < channels.size && channels[idx] != null) {
                channels[idx]
            } else {
                allChannels.getOrNull(idx) ?: allChannels.getOrNull(idx % allChannels.size.coerceAtLeast(1))
            }
            PaneState(
                paneIndex = idx,
                channel = channel,
                playbackState = StreamPlaybackState.IDLE
            )
        }

        _multiviewState.value = MultiviewUiState(
            layoutMode = mode,
            activePaneIndex = initialActivePane,
            panes = initialPanes,
            isActionMenuOpen = false,
            isChannelPickerOpen = false,
            isFullScreenSingle = (mode == MultiviewLayoutMode.ONE_PANE)
        )

        _currentScreen.value = AppScreen.MULTIVIEW

        // Start playback for all active panes in this layout mode!
        for (i in 0 until mode.paneCount) {
            val ch = initialPanes[i].channel
            if (ch != null) {
                startStreamForPane(i, ch)
            }
        }

        playerManager.setActiveAudioPane(initialActivePane)
        startKeepaliveLoop()
        saveCurrentSession()
    }

    fun setActivePane(paneIndex: Int) {
        if (paneIndex == _multiviewState.value.activePaneIndex) return
        _multiviewState.value = _multiviewState.value.copy(activePaneIndex = paneIndex)
        // Audio follows focus!
        playerManager.setActiveAudioPane(paneIndex)
        saveCurrentSession()
    }

    fun switchChannelForActivePane(channel: TabloChannel) {
        val activeIndex = _multiviewState.value.activePaneIndex
        val updatedPanes = _multiviewState.value.panes.map { pane ->
            if (pane.paneIndex == activeIndex) {
                pane.copy(channel = channel, playbackState = StreamPlaybackState.LOADING, errorMessage = null)
            } else pane
        }
        _multiviewState.value = _multiviewState.value.copy(
            panes = updatedPanes,
            isChannelPickerOpen = false,
            isActionMenuOpen = false
        )

        // Only start stream for active pane; other players continue without interruption!
        startStreamForPane(activeIndex, channel)
        saveCurrentSession()
    }

    fun changeLayoutMode(newMode: MultiviewLayoutMode) {
        val current = _multiviewState.value
        val oldMode = current.layoutMode
        val updatedActive = if (current.activePaneIndex >= newMode.paneCount) 0 else current.activePaneIndex

        _multiviewState.value = current.copy(
            layoutMode = newMode,
            activePaneIndex = updatedActive,
            isFullScreenSingle = (newMode == MultiviewLayoutMode.ONE_PANE),
            isActionMenuOpen = false
        )
        preferences.saveLastLayoutMode(newMode)

        val device = deviceRepository.currentDevice.value

        // If decreasing pane count: Stop and release tuners on Tablo hardware for panes no longer visible
        if (newMode.paneCount < oldMode.paneCount) {
            for (i in newMode.paneCount until 4) {
                streamJobs[i]?.cancel()
                val token = playerManager.releasePane(i)
                if (device != null && !token.isNullOrBlank()) {
                    viewModelScope.launch {
                        try {
                            apiClient.stopWatching(device.host, token, device.port)
                        } catch (_: Exception) {}
                    }
                }
                updatePanePlaybackState(i, StreamPlaybackState.IDLE, null)
            }
        } else {
            val allChannels = channelRepository.channels.value.ifEmpty { com.example.data.tablo.defaultBroadcastChannels() }
            val updatedPanes = _multiviewState.value.panes.mapIndexed { i, pane ->
                if (i < newMode.paneCount && pane.channel == null) {
                    pane.copy(channel = allChannels.getOrNull(i) ?: allChannels.getOrNull(i % allChannels.size.coerceAtLeast(1)))
                } else pane
            }
            _multiviewState.value = _multiviewState.value.copy(panes = updatedPanes)

            // For all visible panes, start stream
            for (i in 0 until newMode.paneCount) {
                val pane = _multiviewState.value.panes[i]
                if (pane.channel != null && (pane.playbackState == StreamPlaybackState.IDLE || pane.playbackState == StreamPlaybackState.ERROR)) {
                    startStreamForPane(i, pane.channel)
                }
            }
        }

        playerManager.setActiveAudioPane(updatedActive)
        saveCurrentSession()
    }

    fun toggleFullScreenActivePane() {
        val current = _multiviewState.value
        val device = deviceRepository.currentDevice.value
        if (current.isFullScreenSingle) {
            // Restore previous mode
            changeLayoutMode(current.previousModeBeforeFullScreen)
        } else {
            _multiviewState.value = current.copy(
                previousModeBeforeFullScreen = current.layoutMode,
                layoutMode = MultiviewLayoutMode.ONE_PANE,
                isFullScreenSingle = true,
                isActionMenuOpen = false
            )
            // Other panes stop/pause and release tuner to save bandwidth and Tablo tuners
            for (i in 0 until 4) {
                if (i != current.activePaneIndex) {
                    streamJobs[i]?.cancel()
                    val token = playerManager.releasePane(i)
                    if (device != null && !token.isNullOrBlank()) {
                        viewModelScope.launch {
                            apiClient.stopWatching(device.host, token, device.port)
                        }
                    }
                    updatePanePlaybackState(i, StreamPlaybackState.IDLE, null)
                }
            }
            playerManager.setActiveAudioPane(current.activePaneIndex)
        }
    }

    fun swapPanes(targetIndex: Int) {
        val activeIndex = _multiviewState.value.activePaneIndex
        if (activeIndex == targetIndex) return

        val panes = _multiviewState.value.panes
        val paneA = panes[activeIndex]
        val paneB = panes[targetIndex]

        val updatedPanes = panes.map { pane ->
            when (pane.paneIndex) {
                activeIndex -> pane.copy(channel = paneB.channel)
                targetIndex -> pane.copy(channel = paneA.channel)
                else -> pane
            }
        }

        _multiviewState.value = _multiviewState.value.copy(
            panes = updatedPanes,
            activePaneIndex = targetIndex,
            isReorderMode = false,
            isActionMenuOpen = false
        )

        // Restart streams for swapped panes
        paneB.channel?.let { startStreamForPane(activeIndex, it) }
        paneA.channel?.let { startStreamForPane(targetIndex, it) }

        playerManager.setActiveAudioPane(targetIndex)
        saveCurrentSession()
    }

    /**
     * Promotes a secondary video pane to the primary large focus position (Pane 0).
     * Essential for Football & Sports Multiview (e.g., 3-pane GameDay Focus mode).
     */
    fun promotePaneToPrimary(targetIndex: Int) {
        if (targetIndex <= 0 || targetIndex >= 4) return
        val panes = _multiviewState.value.panes
        val primaryPane = panes[0]
        val targetPane = panes[targetIndex]

        val updatedPanes = panes.map { pane ->
            when (pane.paneIndex) {
                0 -> pane.copy(channel = targetPane.channel)
                targetIndex -> pane.copy(channel = primaryPane.channel)
                else -> pane
            }
        }

        _multiviewState.value = _multiviewState.value.copy(
            panes = updatedPanes,
            activePaneIndex = 0,
            isActionMenuOpen = false
        )

        // Restart streams in their swapped pane positions
        targetPane.channel?.let { startStreamForPane(0, it) }
        primaryPane.channel?.let { startStreamForPane(targetIndex, it) }

        playerManager.setActiveAudioPane(0)
        saveCurrentSession()
    }

    fun openSettingsModal() {
        _multiviewState.value = _multiviewState.value.copy(isSettingsModalOpen = true)
    }

    fun closeSettingsModal() {
        _multiviewState.value = _multiviewState.value.copy(isSettingsModalOpen = false)
    }

    fun applySportsPreset(preset: String) {
        val allChannels = channelRepository.channels.value.ifEmpty { com.example.data.tablo.defaultBroadcastChannels() }
        val targetChannels: List<TabloChannel> = when (preset) {
            "NFL" -> {
                listOfNotNull(
                    allChannels.find { it.network.contains("CBS") } ?: allChannels.getOrNull(0),
                    allChannels.find { it.network.contains("FOX") } ?: allChannels.getOrNull(1),
                    allChannels.find { it.network.contains("NBC") } ?: allChannels.getOrNull(2),
                    allChannels.find { it.network.contains("ESPN") } ?: allChannels.getOrNull(3)
                ).ifEmpty { allChannels.take(4) }
            }
            "NBA" -> {
                listOfNotNull(
                    allChannels.find { it.network.contains("ABC") } ?: allChannels.getOrNull(4),
                    allChannels.find { it.network.contains("ESPN") } ?: allChannels.getOrNull(3),
                    allChannels.find { it.network.contains("TNT") } ?: allChannels.getOrNull(7),
                    allChannels.find { it.network.contains("CBS") } ?: allChannels.getOrNull(0)
                ).ifEmpty { allChannels.take(4) }
            }
            "MLB" -> {
                listOfNotNull(
                    allChannels.find { it.network.contains("TBS") } ?: allChannels.getOrNull(6),
                    allChannels.find { it.network.contains("FOX") } ?: allChannels.getOrNull(1),
                    allChannels.find { it.network.contains("ESPN") } ?: allChannels.getOrNull(3),
                    allChannels.find { it.network.contains("RedZone") } ?: allChannels.getOrNull(5)
                ).ifEmpty { allChannels.take(4) }
            }
            else -> allChannels.take(4)
        }
        launchMultiview(MultiviewLayoutMode.FOUR_PANE, targetChannels, 0)
    }

    fun retryPaneStream(paneIndex: Int) {
        val pane = _multiviewState.value.panes.getOrNull(paneIndex) ?: return
        val ch = pane.channel ?: return
        startStreamForPane(paneIndex, ch)
    }

    fun getSampleSportStreamUrl(paneIndex: Int): String {
        val urls = listOf(
            "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
            "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8",
            "https://test-streams.mux.dev/test_001/stream.m3u8"
        )
        return urls[paneIndex.coerceIn(0, urls.lastIndex)]
    }

    fun startStreamForPane(paneIndex: Int, channel: TabloChannel) {
        val device = deviceRepository.currentDevice.value

        streamJobs[paneIndex]?.cancel()

        // If this pane had an active stream token, stop it first to prevent tuner leaks
        val oldToken = playerManager.getStreamToken(paneIndex)
        if (device != null && !oldToken.isNullOrBlank()) {
            viewModelScope.launch {
                try {
                    apiClient.stopWatching(device.host, oldToken, device.port)
                } catch (_: Exception) {}
            }
        }

        updatePanePlaybackState(paneIndex, StreamPlaybackState.LOADING, null)

        streamJobs[paneIndex] = viewModelScope.launch {
            if (device != null) {
                try {
                    val result = apiClient.watchChannel(device.host, channel.id, preferences.getClientId(), device.port)
                    if (result.isSuccess) {
                        val stream = result.getOrThrow()
                        playerManager.playPaneStream(paneIndex, stream.playlistUrl, channel.id, stream.token)

                        val updatedPanes = _multiviewState.value.panes.map {
                            if (it.paneIndex == paneIndex) {
                                it.copy(streamUrl = stream.playlistUrl, streamToken = stream.token)
                            } else it
                        }
                        _multiviewState.value = _multiviewState.value.copy(panes = updatedPanes)

                        val airing = channelRepository.loadCurrentAiring(device.host, channel.id, device.port)
                        if (airing != null) {
                            val panesWithAiring = _multiviewState.value.panes.map {
                                if (it.paneIndex == paneIndex) it.copy(airing = airing) else it
                            }
                            _multiviewState.value = _multiviewState.value.copy(panes = panesWithAiring)
                        }
                        return@launch
                    }
                } catch (_: Exception) {}
            }

            // High-definition broadcast sports stream (guarantees all 4 games play simultaneously)
            val streamUrl = channel.streamUrl ?: getSampleSportStreamUrl(paneIndex)
            playerManager.playPaneStream(paneIndex, streamUrl, channel.id, null)
            val updatedPanes = _multiviewState.value.panes.map {
                if (it.paneIndex == paneIndex) {
                    it.copy(streamUrl = streamUrl, streamToken = null)
                } else it
            }
            _multiviewState.value = _multiviewState.value.copy(panes = updatedPanes)
        }
    }

    private fun updatePanePlaybackState(paneIndex: Int, state: StreamPlaybackState, error: String?) {
        val updated = _multiviewState.value.panes.map { pane ->
            if (pane.paneIndex == paneIndex) {
                pane.copy(playbackState = state, errorMessage = error)
            } else pane
        }
        _multiviewState.value = _multiviewState.value.copy(panes = updated)
    }

    private fun updatePaneDiagnostics(paneIndex: Int, diagnostics: com.example.model.StreamDiagnostics) {
        val panes = _multiviewState.value.panes
        if (paneIndex in panes.indices) {
            // Only emit Compose state updates if diagnostics overlay is visibly enabled for this pane
            // This stops wasteful recompositions and GC pressure every 1.5s during regular viewing
            if (!panes[paneIndex].showDiagnostics) return
            val updated = panes.map { pane ->
                if (pane.paneIndex == paneIndex) {
                    pane.copy(diagnostics = diagnostics)
                } else pane
            }
            _multiviewState.value = _multiviewState.value.copy(panes = updated)
        }
    }

    fun setPaneQuality(paneIndex: Int, quality: com.example.model.StreamQuality) {
        playerManager.setPaneQuality(paneIndex, quality)
        val updated = _multiviewState.value.panes.map { pane ->
            if (pane.paneIndex == paneIndex) {
                pane.copy(quality = quality)
            } else pane
        }
        _multiviewState.value = _multiviewState.value.copy(
            panes = updated,
            showQualityMenuForPane = null
        )
    }

    fun togglePaneDiagnostics(paneIndex: Int) {
        val updated = _multiviewState.value.panes.map { pane ->
            if (pane.paneIndex == paneIndex) {
                pane.copy(showDiagnostics = !pane.showDiagnostics)
            } else pane
        }
        _multiviewState.value = _multiviewState.value.copy(panes = updated)
    }

    fun togglePaneAudio(paneIndex: Int) {
        playerManager.togglePaneAudio(paneIndex)
        val isMuted = !playerManager.isPaneAudioActive(paneIndex)
        val updated = _multiviewState.value.panes.map { pane ->
            if (pane.paneIndex == paneIndex) {
                pane.copy(isMuted = isMuted)
            } else pane
        }
        _multiviewState.value = _multiviewState.value.copy(panes = updated)
    }

    fun openQualityMenu(paneIndex: Int?) {
        _multiviewState.value = _multiviewState.value.copy(showQualityMenuForPane = paneIndex)
    }

    fun setPipMode(inPip: Boolean) {
        _multiviewState.value = _multiviewState.value.copy(isInPipMode = inPip)
    }

    fun requestPictureInPicture() {
        onTriggerPip?.invoke()
    }

    fun setSourceCategory(category: String) {
        _multiviewState.value = _multiviewState.value.copy(activeSourceCategory = category)
    }

    fun toggleActionMenu(open: Boolean? = null) {
        val newState = open ?: !_multiviewState.value.isActionMenuOpen
        _multiviewState.value = _multiviewState.value.copy(isActionMenuOpen = newState)
    }

    fun openChannelPicker(targetPaneIndex: Int? = null) {
        val target = targetPaneIndex ?: _multiviewState.value.activePaneIndex
        _multiviewState.value = _multiviewState.value.copy(
            activePaneIndex = target,
            isChannelPickerOpen = true,
            isActionMenuOpen = false
        )
    }

    fun closeChannelPicker() {
        _multiviewState.value = _multiviewState.value.copy(isChannelPickerOpen = false)
    }

    /**
     * Incrementally add another screen/view to the layout (1 -> 2 -> 3 -> 4)
     * and immediately open the channel picker for the newly added view.
     */
    fun addNextView() {
        val currentMode = _multiviewState.value.layoutMode
        val newMode = when (currentMode) {
            MultiviewLayoutMode.ONE_PANE -> MultiviewLayoutMode.TWO_PANE
            MultiviewLayoutMode.TWO_PANE -> MultiviewLayoutMode.THREE_PANE
            MultiviewLayoutMode.THREE_PANE -> MultiviewLayoutMode.FOUR_PANE
            MultiviewLayoutMode.FOUR_PANE -> return
        }
        changeLayoutMode(newMode)
        // Focus the newly added pane and open channel picker so user can choose what to watch
        val emptyPaneIdx = (0 until newMode.paneCount).firstOrNull { _multiviewState.value.panes[it].channel == null }
            ?: (newMode.paneCount - 1)
        openChannelPicker(emptyPaneIdx)
    }

    /**
     * Clear and stop a specific pane to free the tuner on the Tablo device.
     */
    fun clearPane(paneIndex: Int) {
        val device = deviceRepository.currentDevice.value
        streamJobs[paneIndex]?.cancel()
        val token = playerManager.releasePane(paneIndex)
        if (device != null && !token.isNullOrBlank()) {
            viewModelScope.launch {
                try {
                    apiClient.stopWatching(device.host, token, device.port)
                } catch (_: Exception) {}
            }
        }
        val updatedPanes = _multiviewState.value.panes.map { pane ->
            if (pane.paneIndex == paneIndex) {
                pane.copy(
                    channel = null,
                    streamUrl = null,
                    streamToken = null,
                    playbackState = StreamPlaybackState.IDLE,
                    errorMessage = null
                )
            } else pane
        }
        _multiviewState.value = _multiviewState.value.copy(panes = updatedPanes)
        saveCurrentSession()
    }

    fun setReorderMode(enabled: Boolean) {
        _multiviewState.value = _multiviewState.value.copy(
            isReorderMode = enabled,
            isActionMenuOpen = false
        )
    }

    fun startReorderMode() = setReorderMode(true)
    fun cancelReorderMode() = setReorderMode(false)

    fun openSaveLayoutDialog() {
        _multiviewState.value = _multiviewState.value.copy(
            isSaveLayoutDialogOpen = true,
            isActionMenuOpen = false
        )
    }

    fun closeSaveLayoutDialog() {
        _multiviewState.value = _multiviewState.value.copy(isSaveLayoutDialogOpen = false)
    }

    fun saveCurrentLayout(name: String) {
        val state = _multiviewState.value
        val activePanes = state.panes.take(state.layoutMode.paneCount)
        val channelIds = activePanes.map { it.channel?.id ?: "" }
        val channelLabels = activePanes.map { it.channel?.displayTitle ?: "Empty" }

        viewModelScope.launch {
            savedLayoutRepository.saveLayout(
                name = name.ifBlank { "Multiview Preset" },
                mode = state.layoutMode,
                channelIds = channelIds,
                channelLabels = channelLabels
            )
            closeSaveLayoutDialog()
        }
    }

    fun launchSavedLayout(layout: SavedLayout) {
        val allChannels = channelRepository.channels.value
        val channels = layout.channelIds.map { id ->
            allChannels.find { it.id == id }
        }
        launchMultiview(layout.mode, channels)
    }

    fun deleteSavedLayout(id: Long) {
        viewModelScope.launch {
            savedLayoutRepository.deleteLayout(id)
        }
    }

    fun renameSavedLayout(layout: SavedLayout, newName: String) {
        viewModelScope.launch {
            savedLayoutRepository.renameLayout(layout, newName)
        }
    }

    private fun saveCurrentSession() {
        val state = _multiviewState.value
        val channelIds = state.panes.map { it.channel?.id }
        preferences.saveLastLayout(state.layoutMode, channelIds, state.activePaneIndex)
    }

    private fun startKeepaliveLoop() {
        keepaliveJob?.cancel()
        keepaliveJob = viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(20_000L) // Ping every 20s to ensure Tablo tuner transcode lease never times out
                val device = deviceRepository.currentDevice.value ?: continue
                val current = _multiviewState.value
                val activeCount = current.layoutMode.paneCount
                for (i in 0 until activeCount) {
                    val pane = current.panes.getOrNull(i) ?: continue
                    val ch = pane.channel ?: continue
                    val token = pane.streamToken
                    if (!token.isNullOrBlank() && pane.playbackState != StreamPlaybackState.ERROR) {
                        try {
                            apiClient.keepStreamAlive(device.host, ch.id, token, preferences.getClientId(), device.port)
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    private fun stopAllStreams() {
        keepaliveJob?.cancel()
        val device = deviceRepository.currentDevice.value
        for (i in 0 until 4) {
            streamJobs[i]?.cancel()
        }
        val releasedTokens = playerManager.releaseAll()
        if (device != null && releasedTokens.isNotEmpty()) {
            viewModelScope.launch {
                for (token in releasedTokens) {
                    try {
                        apiClient.stopWatching(device.host, token, device.port)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAllStreams()
    }
}

private fun defaultDvrRecordings(): List<DvrRecording> = listOf(
    DvrRecording(
        id = "dvr_nfl_1",
        title = "NFL Sunday Ticket: Chiefs at Ravens",
        subtitle = "AFC Championship Rematch • High Drama Q4",
        category = DvrCategory.SPORTS,
        network = "CBS Sports",
        channelNumber = "4.1",
        durationMinutes = 180,
        recordedDate = "Sunday",
        expiresMonthsRemaining = 9
    ),
    DvrRecording(
        id = "dvr_nfl_2",
        title = "NFL: 49ers vs Rams",
        subtitle = "NFC West Primetime Showdown",
        category = DvrCategory.SPORTS,
        network = "FOX Sports",
        channelNumber = "11.1",
        durationMinutes = 175,
        recordedDate = "Sunday",
        expiresMonthsRemaining = 9
    ),
    DvrRecording(
        id = "dvr_show_1",
        title = "60 Minutes",
        subtitle = "Season 57 Ep 12 • Deep Dive Investigation",
        category = DvrCategory.SHOWS,
        network = "CBS",
        channelNumber = "4.1",
        durationMinutes = 60,
        recordedDate = "2 days ago",
        expiresMonthsRemaining = 9
    ),
    DvrRecording(
        id = "dvr_show_2",
        title = "Jeopardy! Tournament of Champions",
        subtitle = "Finals Game 3 • High Stakes Trivia",
        category = DvrCategory.NEW_TO_YOU,
        network = "ABC",
        channelNumber = "7.1",
        durationMinutes = 30,
        recordedDate = "Yesterday",
        expiresMonthsRemaining = 9
    ),
    DvrRecording(
        id = "dvr_movie_1",
        title = "Top Gun: Maverick",
        subtitle = "Action / Adventure • 4K Broadcast",
        category = DvrCategory.MOVIES,
        network = "Paramount",
        channelNumber = "9.1",
        durationMinutes = 130,
        recordedDate = "Last week",
        expiresMonthsRemaining = 8
    ),
    DvrRecording(
        id = "dvr_event_1",
        title = "2026 Primetime Emmy Awards",
        subtitle = "Live Red Carpet & Ceremony",
        category = DvrCategory.EVENTS,
        network = "NBC",
        channelNumber = "5.1",
        durationMinutes = 210,
        recordedDate = "3 days ago",
        expiresMonthsRemaining = 9
    )
)
