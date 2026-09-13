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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    HOME,
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

    val playerManager = MultiviewPlayerManager(application) { paneIndex, state, error ->
        updatePanePlaybackState(paneIndex, state, error)
    }

    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _multiviewState = MutableStateFlow(MultiviewUiState())
    val multiviewState: StateFlow<MultiviewUiState> = _multiviewState.asStateFlow()

    val savedLayouts: StateFlow<List<SavedLayout>> = savedLayoutRepository.layouts
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val streamJobs = arrayOfNulls<Job>(4)

    init {
        // Startup flow
        viewModelScope.launch {
            channelRepository.loadCachedChannels()
            val savedDevice = preferences.getRegisteredDevice()
            if (savedDevice == null) {
                _currentScreen.value = AppScreen.REGISTRATION
            } else {
                _currentScreen.value = AppScreen.HOME
                // Reconnect in background
                deviceRepository.tryReconnect()
                channelRepository.refreshChannels(savedDevice.host, savedDevice.port)
            }
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

    fun startDiscovery() {
        viewModelScope.launch {
            deviceRepository.discoverDevices()
        }
    }

    fun connectManualIp(ip: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = deviceRepository.connectManualIp(ip)
            if (result.isSuccess) {
                val dev = result.getOrThrow()
                channelRepository.refreshChannels(dev.host, dev.port)
                _currentScreen.value = AppScreen.HOME
                onComplete(true)
            } else {
                onComplete(false)
            }
        }
    }

    fun registerDiscoveredDevice(device: TabloDevice) {
        viewModelScope.launch {
            deviceRepository.registerDevice(device)
            channelRepository.refreshChannels(device.host, device.port)
            _currentScreen.value = AppScreen.HOME
        }
    }

    fun retryConnection() {
        viewModelScope.launch {
            val success = deviceRepository.tryReconnect()
            if (success) {
                val dev = deviceRepository.currentDevice.value
                if (dev != null) {
                    channelRepository.refreshChannels(dev.host, dev.port)
                }
            }
        }
    }

    fun refreshGuideChannels() {
        val dev = deviceRepository.currentDevice.value ?: return
        viewModelScope.launch {
            channelRepository.refreshChannels(dev.host, dev.port)
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
            val channel = allChannels.find { it.id == chId } ?: allChannels.getOrNull(i)
            assignedChannels.add(channel)
        }

        launchMultiview(lastMode, assignedChannels)
    }

    fun launchMultiview(
        mode: MultiviewLayoutMode = MultiviewLayoutMode.FOUR_PANE,
        channels: List<TabloChannel?> = emptyList(),
        initialActivePane: Int = 0
    ) {
        val allChannels = channelRepository.channels.value
        val initialPanes = (0 until 4).map { idx ->
            val channel = channels.getOrNull(idx) ?: allChannels.getOrNull(idx)
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

        // Start playback for all panes active in this mode
        for (i in 0 until mode.paneCount) {
            val ch = initialPanes[i].channel
            if (ch != null) {
                startStreamForPane(i, ch)
            }
        }

        playerManager.setActiveAudioPane(initialActivePane)
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

        // Only start stream for active pane; other 3 players continue without interruption!
        startStreamForPane(activeIndex, channel)
        saveCurrentSession()
    }

    fun changeLayoutMode(newMode: MultiviewLayoutMode) {
        val current = _multiviewState.value
        val updatedActive = if (current.activePaneIndex >= newMode.paneCount) 0 else current.activePaneIndex

        _multiviewState.value = current.copy(
            layoutMode = newMode,
            activePaneIndex = updatedActive,
            isFullScreenSingle = (newMode == MultiviewLayoutMode.ONE_PANE),
            isActionMenuOpen = false
        )

        // Ensure newly visible panes have streams running
        val allChannels = channelRepository.channels.value
        for (i in 0 until newMode.paneCount) {
            val pane = _multiviewState.value.panes[i]
            if (pane.channel == null && allChannels.isNotEmpty()) {
                val fallbackChannel = allChannels.getOrNull(i)
                if (fallbackChannel != null) {
                    val newPanes = _multiviewState.value.panes.map {
                        if (it.paneIndex == i) it.copy(channel = fallbackChannel) else it
                    }
                    _multiviewState.value = _multiviewState.value.copy(panes = newPanes)
                    startStreamForPane(i, fallbackChannel)
                }
            } else if (pane.channel != null && pane.playbackState == StreamPlaybackState.IDLE) {
                startStreamForPane(i, pane.channel)
            }
        }

        // Stop streams for panes no longer visible
        for (i in newMode.paneCount until 4) {
            streamJobs[i]?.cancel()
            playerManager.stopPane(i)
        }

        playerManager.setActiveAudioPane(updatedActive)
        saveCurrentSession()
    }

    fun toggleFullScreenActivePane() {
        val current = _multiviewState.value
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
            // Other panes stop/pause
            for (i in 0 until 4) {
                if (i != current.activePaneIndex) {
                    streamJobs[i]?.cancel()
                    playerManager.stopPane(i)
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

    fun retryPaneStream(paneIndex: Int) {
        val pane = _multiviewState.value.panes.getOrNull(paneIndex) ?: return
        val ch = pane.channel ?: return
        startStreamForPane(paneIndex, ch)
    }

    private fun startStreamForPane(paneIndex: Int, channel: TabloChannel) {
        val device = deviceRepository.currentDevice.value ?: return

        streamJobs[paneIndex]?.cancel()
        updatePanePlaybackState(paneIndex, StreamPlaybackState.LOADING, null)

        streamJobs[paneIndex] = viewModelScope.launch {
            // Fetch live stream URL from Tablo API
            val result = apiClient.watchChannel(device.host, channel.id, device.port)
            if (result.isSuccess) {
                val stream = result.getOrThrow()
                playerManager.playPaneStream(paneIndex, stream.playlistUrl, channel.id, stream.token)
                // Load current airing in background
                val airing = channelRepository.loadCurrentAiring(device.host, channel.id, device.port)
                if (airing != null) {
                    val updatedPanes = _multiviewState.value.panes.map {
                        if (it.paneIndex == paneIndex) it.copy(airing = airing) else it
                    }
                    _multiviewState.value = _multiviewState.value.copy(panes = updatedPanes)
                }
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Stream unavailable"
                updatePanePlaybackState(paneIndex, StreamPlaybackState.ERROR, errorMsg)
            }
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

    fun toggleActionMenu(open: Boolean? = null) {
        val newState = open ?: !_multiviewState.value.isActionMenuOpen
        _multiviewState.value = _multiviewState.value.copy(isActionMenuOpen = newState)
    }

    fun openChannelPicker() {
        _multiviewState.value = _multiviewState.value.copy(
            isChannelPickerOpen = true,
            isActionMenuOpen = false
        )
    }

    fun closeChannelPicker() {
        _multiviewState.value = _multiviewState.value.copy(isChannelPickerOpen = false)
    }

    fun setReorderMode(enabled: Boolean) {
        _multiviewState.value = _multiviewState.value.copy(
            isReorderMode = enabled,
            isActionMenuOpen = false
        )
    }

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

    private fun stopAllStreams() {
        for (i in 0 until 4) {
            streamJobs[i]?.cancel()
        }
        playerManager.releaseAll()
    }

    override fun onCleared() {
        super.onCleared()
        stopAllStreams()
    }
}
