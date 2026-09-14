package com.example.model

enum class MultiviewLayoutMode(val paneCount: Int, val label: String) {
    ONE_PANE(1, "1 Pane (Single)"),
    TWO_PANE(2, "2 Panes (Split)"),
    THREE_PANE(3, "3 Panes (Primary + 2)"),
    FOUR_PANE(4, "4 Panes (Quad Grid)");

    companion object {
        val SINGLE get() = ONE_PANE
        val SIDE_BY_SIDE get() = TWO_PANE
        val FOCUS_PRIMARY_BOTTOM_STRIP get() = THREE_PANE
        val QUAD_GRID get() = FOUR_PANE
    }
}

enum class StreamPlaybackState {
    IDLE,
    LOADING,
    PLAYING,
    BUFFERING,
    ERROR
}

enum class StreamQuality(val label: String, val maxResWidth: Int, val maxResHeight: Int) {
    AUTO("Auto (Adaptive)", 1920, 1080),
    HD_1080P("1080p Full HD", 1920, 1080),
    HD_720P("720p HD (Low Buffer)", 1280, 720),
    SD_480P("480p SD (High Stability)", 854, 480)
}

data class StreamDiagnostics(
    val bitrateKbps: Int = 0,
    val resolution: String = "--",
    val framerate: Float = 0f,
    val decoder: String = "Hardware (MediaCodec)",
    val isHardwareAccelerated: Boolean = true,
    val droppedFrames: Int = 0,
    val bufferHealthMs: Long = 0L
)

data class PaneState(
    val paneIndex: Int,
    val channel: TabloChannel? = null,
    val airing: TabloAiring? = null,
    val playbackState: StreamPlaybackState = StreamPlaybackState.IDLE,
    val errorMessage: String? = null,
    val isMuted: Boolean = false,
    val streamUrl: String? = null,
    val streamToken: String? = null,
    val quality: StreamQuality = StreamQuality.AUTO,
    val showDiagnostics: Boolean = false,
    val diagnostics: StreamDiagnostics = StreamDiagnostics(),
    val isPrimaryFocus: Boolean = false
)

data class MultiviewUiState(
    val layoutMode: MultiviewLayoutMode = MultiviewLayoutMode.FOUR_PANE,
    val activePaneIndex: Int = 0,
    val panes: List<PaneState> = (0 until 4).map { PaneState(paneIndex = it) },
    val isActionMenuOpen: Boolean = false,
    val isChannelPickerOpen: Boolean = false,
    val isReorderMode: Boolean = false,
    val isSaveLayoutDialogOpen: Boolean = false,
    val isFullScreenSingle: Boolean = false,
    val previousModeBeforeFullScreen: MultiviewLayoutMode = MultiviewLayoutMode.FOUR_PANE,
    val isInPipMode: Boolean = false,
    val showQualityMenuForPane: Int? = null,
    val activeSourceCategory: String = "Tablo Live TV",
    val isPlaybackOverlayVisible: Boolean = false,
    val isMultiviewBuilderOpen: Boolean = false,
    val isStatsOverlayOpen: Boolean = false,
    val isSettingsModalOpen: Boolean = false
)

