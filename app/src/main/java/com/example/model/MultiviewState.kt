package com.example.model

enum class MultiviewLayoutMode(val paneCount: Int, val label: String) {
    ONE_PANE(1, "1 Pane (Single)"),
    TWO_PANE(2, "2 Panes (Split)"),
    THREE_PANE(3, "3 Panes (Primary + 2)"),
    FOUR_PANE(4, "4 Panes (Quad Grid)")
}

enum class StreamPlaybackState {
    IDLE,
    LOADING,
    PLAYING,
    BUFFERING,
    ERROR
}

data class PaneState(
    val paneIndex: Int,
    val channel: TabloChannel? = null,
    val airing: TabloAiring? = null,
    val playbackState: StreamPlaybackState = StreamPlaybackState.IDLE,
    val errorMessage: String? = null,
    val isMuted: Boolean = false,
    val streamUrl: String? = null,
    val streamToken: String? = null
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
    val previousModeBeforeFullScreen: MultiviewLayoutMode = MultiviewLayoutMode.FOUR_PANE
)
