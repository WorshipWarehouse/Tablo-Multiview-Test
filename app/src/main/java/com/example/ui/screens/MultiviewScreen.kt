package com.example.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import kotlinx.coroutines.delay
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.example.model.MultiviewLayoutMode
import com.example.model.PaneState
import com.example.model.StreamPlaybackState
import com.example.model.TabloChannel
import com.example.ui.components.TvButton
import com.example.ui.components.TvButtonStyle
import com.example.ui.components.TvFocusableCard
import com.example.ui.theme.TvAmberAccent
import com.example.ui.theme.TvBackground
import com.example.ui.theme.TvBorderFocused
import com.example.ui.theme.TvBorderGrey
import com.example.ui.theme.TvBorderNormal
import com.example.ui.theme.TvCyanPrimary
import com.example.ui.theme.TvError
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvSurfaceElevated
import com.example.ui.theme.TvSurfaceVariant
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary
import com.example.ui.theme.TvTextTertiary
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.TabloAppViewModel

@Composable
fun MultiviewScreen(
    viewModel: TabloAppViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.multiviewState.collectAsState()
    val allChannels by viewModel.channelRepository.channels.collectAsState()

    var showHud by remember { mutableStateOf(true) }
    var hudVersion by remember { mutableLongStateOf(System.currentTimeMillis()) }

    fun pingHud() {
        showHud = true
        hudVersion = System.currentTimeMillis()
    }

    LaunchedEffect(hudVersion) {
        if (showHud) {
            delay(3500)
            showHud = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (!showHud) pingHud() else viewModel.toggleActionMenu()
            }
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyUp) {
                    when (keyEvent.key) {
                        Key.One, Key.NumPad1 -> {
                            viewModel.changeLayoutMode(MultiviewLayoutMode.ONE_PANE)
                            pingHud()
                            true
                        }
                        Key.Two, Key.NumPad2 -> {
                            viewModel.changeLayoutMode(MultiviewLayoutMode.TWO_PANE)
                            pingHud()
                            true
                        }
                        Key.Three, Key.NumPad3 -> {
                            viewModel.changeLayoutMode(MultiviewLayoutMode.THREE_PANE)
                            pingHud()
                            true
                        }
                        Key.Four, Key.NumPad4 -> {
                            viewModel.changeLayoutMode(MultiviewLayoutMode.FOUR_PANE)
                            pingHud()
                            true
                        }
                        Key.M, Key.Menu -> {
                            viewModel.toggleActionMenu()
                            true
                        }
                        Key.DirectionCenter, Key.Enter, Key.Spacebar -> {
                            if (!showHud) {
                                pingHud()
                            } else {
                                viewModel.toggleActionMenu()
                            }
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        // Layout Container based on mode
        when (state.layoutMode) {
            MultiviewLayoutMode.ONE_PANE -> {
                SinglePaneLayout(
                    paneState = state.panes[state.activePaneIndex],
                    isActive = true,
                    showOverlay = showHud,
                    onFocus = {},
                    onClick = {
                        if (!showHud) pingHud() else viewModel.toggleActionMenu()
                    },
                    onRetry = { viewModel.retryPaneStream(state.activePaneIndex) },
                    viewModel = viewModel
                )
            }
            MultiviewLayoutMode.TWO_PANE -> {
                TwoPaneLayout(
                    panes = state.panes.take(2),
                    activePaneIndex = state.activePaneIndex,
                    showOverlay = showHud,
                    onFocus = {
                        viewModel.setActivePane(it)
                        pingHud()
                    },
                    onClick = {
                        if (!showHud) pingHud() else viewModel.toggleActionMenu()
                    },
                    onRetry = { viewModel.retryPaneStream(it) },
                    viewModel = viewModel
                )
            }
            MultiviewLayoutMode.THREE_PANE -> {
                ThreePaneLayout(
                    panes = state.panes.take(3),
                    activePaneIndex = state.activePaneIndex,
                    showOverlay = showHud,
                    onFocus = {
                        viewModel.setActivePane(it)
                        pingHud()
                    },
                    onClick = {
                        if (!showHud) pingHud() else viewModel.toggleActionMenu()
                    },
                    onRetry = { viewModel.retryPaneStream(it) },
                    viewModel = viewModel
                )
            }
            MultiviewLayoutMode.FOUR_PANE -> {
                FourPaneGrid(
                    panes = state.panes,
                    activePaneIndex = state.activePaneIndex,
                    showOverlay = showHud,
                    onFocus = {
                        viewModel.setActivePane(it)
                        pingHud()
                    },
                    onClick = {
                        if (!showHud) pingHud() else viewModel.toggleActionMenu()
                    },
                    onRetry = { viewModel.retryPaneStream(it) },
                    viewModel = viewModel
                )
            }
        }

        // Minimal Auto-Hiding Bottom HUD: Only visible when active, never gets in the way of video
        AnimatedVisibility(
            visible = showHud && !state.isActionMenuOpen && !state.isChannelPickerOpen && !state.isSaveLayoutDialogOpen,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xF0121214),
                border = BorderStroke(1.dp, Color(0xFF2C2C2E)),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Minimal 1, 2, 3, 4 Screen Selector Pills
                    MultiviewLayoutMode.values().forEach { mode ->
                        val isSelected = state.layoutMode == mode
                        val label = when (mode) {
                            MultiviewLayoutMode.ONE_PANE -> "1"
                            MultiviewLayoutMode.TWO_PANE -> "2"
                            MultiviewLayoutMode.THREE_PANE -> "3"
                            MultiviewLayoutMode.FOUR_PANE -> "4"
                        }

                        TvButton(
                            text = label,
                            onClick = {
                                viewModel.changeLayoutMode(mode)
                                pingHud()
                            },
                            style = if (isSelected) TvButtonStyle.PRIMARY else TvButtonStyle.OUTLINE,
                            modifier = Modifier.size(width = 38.dp, height = 30.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(18.dp)
                            .width(1.dp)
                            .background(Color(0xFF38383A))
                    )

                    TvButton(
                        text = "Channels",
                        onClick = {
                            viewModel.openChannelPicker()
                            pingHud()
                        },
                        style = TvButtonStyle.SECONDARY,
                        modifier = Modifier.height(30.dp),
                        leadingIcon = {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    )

                    TvButton(
                        text = "Home",
                        onClick = { viewModel.navigateTo(AppScreen.HOME) },
                        style = TvButtonStyle.OUTLINE,
                        modifier = Modifier.height(30.dp),
                        leadingIcon = {
                            Icon(
                                Icons.Default.Home,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    )
                }
            }
        }

        // Action Menu Dialog Overlay
        if (state.isActionMenuOpen) {
            MultiviewActionMenu(
                state = state,
                viewModel = viewModel,
                onDismiss = { viewModel.toggleActionMenu(false) }
            )
        }

        // Quick Channel Switcher Overlay
        if (state.isChannelPickerOpen) {
            QuickChannelPickerModal(
                channels = allChannels,
                activeChannel = state.panes[state.activePaneIndex].channel,
                onSelectChannel = { channel ->
                    viewModel.switchChannelForActivePane(channel)
                },
                onDismiss = { viewModel.closeChannelPicker() }
            )
        }

        // Save Layout Preset Dialog Overlay
        if (state.isSaveLayoutDialogOpen) {
            SaveLayoutDialog(
                onSave = { name -> viewModel.saveCurrentLayout(name) },
                onDismiss = { viewModel.closeSaveLayoutDialog() }
            )
        }
    }
}

// --- Layout Arrangements ---

@Composable
fun SinglePaneLayout(
    paneState: PaneState,
    isActive: Boolean,
    showOverlay: Boolean,
    onFocus: () -> Unit,
    onClick: () -> Unit,
    onRetry: () -> Unit,
    viewModel: TabloAppViewModel
) {
    Box(modifier = Modifier.fillMaxSize()) {
        MultiviewVideoPane(
            paneState = paneState,
            isActive = isActive,
            showOverlay = showOverlay,
            onFocus = onFocus,
            onClick = onClick,
            onRetry = onRetry,
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun TwoPaneLayout(
    panes: List<PaneState>,
    activePaneIndex: Int,
    showOverlay: Boolean,
    onFocus: (Int) -> Unit,
    onClick: () -> Unit,
    onRetry: (Int) -> Unit,
    viewModel: TabloAppViewModel
) {
    Row(modifier = Modifier.fillMaxSize()) {
        panes.forEach { pane ->
            MultiviewVideoPane(
                paneState = pane,
                isActive = pane.paneIndex == activePaneIndex,
                showOverlay = showOverlay,
                onFocus = { onFocus(pane.paneIndex) },
                onClick = onClick,
                onRetry = { onRetry(pane.paneIndex) },
                viewModel = viewModel,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
fun ThreePaneLayout(
    panes: List<PaneState>,
    activePaneIndex: Int,
    showOverlay: Boolean,
    onFocus: (Int) -> Unit,
    onClick: () -> Unit,
    onRetry: (Int) -> Unit,
    viewModel: TabloAppViewModel
) {
    Row(modifier = Modifier.fillMaxSize()) {
        // Large Primary Pane on Left (60% width)
        val primaryPane = panes.getOrNull(0) ?: PaneState(0)
        MultiviewVideoPane(
            paneState = primaryPane,
            isActive = primaryPane.paneIndex == activePaneIndex,
            showOverlay = showOverlay,
            onFocus = { onFocus(primaryPane.paneIndex) },
            onClick = onClick,
            onRetry = { onRetry(primaryPane.paneIndex) },
            viewModel = viewModel,
            modifier = Modifier
                .weight(1.5f)
                .fillMaxHeight()
        )

        // Stacked Secondary Panes on Right (40% width)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            for (i in 1..2) {
                val secPane = panes.getOrNull(i) ?: PaneState(i)
                MultiviewVideoPane(
                    paneState = secPane,
                    isActive = secPane.paneIndex == activePaneIndex,
                    showOverlay = showOverlay,
                    onFocus = { onFocus(secPane.paneIndex) },
                    onClick = onClick,
                    onRetry = { onRetry(secPane.paneIndex) },
                    viewModel = viewModel,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun FourPaneGrid(
    panes: List<PaneState>,
    activePaneIndex: Int,
    showOverlay: Boolean,
    onFocus: (Int) -> Unit,
    onClick: () -> Unit,
    onRetry: (Int) -> Unit,
    viewModel: TabloAppViewModel
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Top Row: Panes 0 and 1
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            for (i in 0..1) {
                val pane = panes[i]
                MultiviewVideoPane(
                    paneState = pane,
                    isActive = pane.paneIndex == activePaneIndex,
                    showOverlay = showOverlay,
                    onFocus = { onFocus(pane.paneIndex) },
                    onClick = onClick,
                    onRetry = { onRetry(pane.paneIndex) },
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }

        // Bottom Row: Panes 2 and 3
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            for (i in 2..3) {
                val pane = panes[i]
                MultiviewVideoPane(
                    paneState = pane,
                    isActive = pane.paneIndex == activePaneIndex,
                    showOverlay = showOverlay,
                    onFocus = { onFocus(pane.paneIndex) },
                    onClick = onClick,
                    onRetry = { onRetry(pane.paneIndex) },
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }
    }
}

// --- Individual Video Pane Composable ---

@OptIn(UnstableApi::class)
@Composable
fun MultiviewVideoPane(
    paneState: PaneState,
    isActive: Boolean,
    showOverlay: Boolean,
    onFocus: () -> Unit,
    onClick: () -> Unit,
    onRetry: () -> Unit,
    viewModel: TabloAppViewModel,
    modifier: Modifier = Modifier
) {
    val isSinglePane = viewModel.multiviewState.value.layoutMode == MultiviewLayoutMode.ONE_PANE
    val borderModifier = if (isSinglePane) {
        Modifier
    } else if (isActive) {
        Modifier.border(2.dp, TvBorderGrey)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .testTag("pane_${paneState.paneIndex}")
            .background(Color.Black)
            .then(borderModifier)
            .onFocusChanged { if (it.isFocused) onFocus() }
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyUp &&
                    (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter)
                ) {
                    onClick()
                    true
                } else false
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        // Video View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = false
                    keepScreenOn = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                    player = viewModel.playerManager.getPlayer(paneState.paneIndex)
                }
            },
            update = { playerView ->
                val player = viewModel.playerManager.getPlayer(paneState.paneIndex)
                if (playerView.player != player) {
                    playerView.player = player
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Minimalist Channel Header Overlay: Only visible when requested / user interaction
        AnimatedVisibility(
            visible = showOverlay,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.65f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = paneState.channel?.channelNumberFormatted ?: "PANE ${paneState.paneIndex + 1}",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = paneState.channel?.network?.ifBlank { paneState.channel?.callSign } ?: "Select Channel",
                            color = Color(0xFFE5E5EA),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    val showTitle = paneState.airing?.title ?: paneState.channel?.displaySubtitle
                    if (!showTitle.isNullOrBlank()) {
                        Text(
                            text = showTitle,
                            color = TvTextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Loading Overlay
        if (paneState.playbackState == StreamPlaybackState.LOADING ||
            paneState.playbackState == StreamPlaybackState.BUFFERING
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(26.dp),
                        color = TvCyanPrimary,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (paneState.playbackState == StreamPlaybackState.LOADING) "Tuning live channel..." else "Buffering...",
                        color = TvTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Error Overlay
        if (paneState.playbackState == StreamPlaybackState.ERROR) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = TvError,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Stream Unavailable",
                        color = TvTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = paneState.errorMessage ?: "Check tuner availability",
                        color = TvTextSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    TvButton(
                        text = "Retry Stream",
                        onClick = onRetry,
                        style = TvButtonStyle.AMBER,
                        modifier = Modifier.height(36.dp)
                    )
                }
            }
        }
    }
}

// --- Action Menu Modal ---

@Composable
fun MultiviewActionMenu(
    state: com.example.model.MultiviewUiState,
    viewModel: TabloAppViewModel,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF141416),
            border = BorderStroke(1.dp, Color(0xFF38383A)),
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth(0.92f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PANE ${state.activePaneIndex + 1} CONTROLS",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        val channel = state.panes[state.activePaneIndex].channel
                        Text(
                            text = channel?.displayTitle ?: "No channel selected",
                            color = TvTextSecondary,
                            fontSize = 13.sp
                        )
                    }

                    TvButton(
                        text = "Close",
                        onClick = onDismiss,
                        style = TvButtonStyle.OUTLINE,
                        leadingIcon = { Icon(Icons.Default.Close, contentDescription = null, tint = TvTextSecondary) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons Grid
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TvButton(
                        text = "Change Channel",
                        onClick = { viewModel.openChannelPicker() },
                        style = TvButtonStyle.PRIMARY,
                        modifier = Modifier.weight(1f),
                        leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, tint = Color.Black) }
                    )

                    TvButton(
                        text = if (state.isFullScreenSingle) "Multiview Grid" else "Fullscreen Pane",
                        onClick = { viewModel.toggleFullScreenActivePane() },
                        style = TvButtonStyle.SECONDARY,
                        modifier = Modifier.weight(1f),
                        leadingIcon = {
                            Icon(
                                if (state.isFullScreenSingle) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Layout Mode Selector
                Text(
                    text = "CHOOSE SCREENS (1, 2, 3, or 4)",
                    color = TvTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MultiviewLayoutMode.values().forEach { mode ->
                        val isSelected = state.layoutMode == mode
                        val label = when (mode) {
                            MultiviewLayoutMode.ONE_PANE -> "1 Screen"
                            MultiviewLayoutMode.TWO_PANE -> "2 Screens"
                            MultiviewLayoutMode.THREE_PANE -> "3 Screens"
                            MultiviewLayoutMode.FOUR_PANE -> "4 Screens"
                        }
                        TvButton(
                            text = label,
                            onClick = { viewModel.changeLayoutMode(mode) },
                            style = if (isSelected) TvButtonStyle.PRIMARY else TvButtonStyle.OUTLINE,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TvButton(
                        text = "Save Preset",
                        onClick = { viewModel.openSaveLayoutDialog() },
                        style = TvButtonStyle.SECONDARY,
                        modifier = Modifier.weight(1f),
                        leadingIcon = { Icon(Icons.Default.Bookmark, contentDescription = null, tint = Color.White) }
                    )

                    TvButton(
                        text = "Channel Guide",
                        onClick = { viewModel.navigateTo(AppScreen.CHANNEL_GUIDE) },
                        style = TvButtonStyle.SECONDARY,
                        modifier = Modifier.weight(1f),
                        leadingIcon = { Icon(Icons.Default.List, contentDescription = null, tint = Color.White) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                TvButton(
                    text = "Exit to Home Screen",
                    onClick = { viewModel.navigateTo(AppScreen.HOME) },
                    style = TvButtonStyle.OUTLINE,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Close, contentDescription = null, tint = TvTextSecondary) }
                )
            }
        }
    }
}

// --- Quick Channel Picker Modal ---

@Composable
fun QuickChannelPickerModal(
    channels: List<TabloChannel>,
    activeChannel: TabloChannel?,
    onSelectChannel: (TabloChannel) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF141416),
            border = BorderStroke(1.dp, Color(0xFF38383A)),
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth(0.90f)
                .heightIn(max = 440.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SELECT CHANNEL",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    TvButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        style = TvButtonStyle.OUTLINE
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (channels.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No channels available", color = TvTextSecondary)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(channels) { ch ->
                            val isSelected = ch.id == activeChannel?.id
                            TvFocusableCard(
                                onClick = { onSelectChannel(ch) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp),
                                focusedContainerColor = Color(0xFF26262A),
                                unfocusedContainerColor = if (isSelected) Color(0xFF202024) else Color(0xFF161618),
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = if (isSelected) Color(0xFF8E8E93) else Color(0xFF262628),
                                testTag = "quick_ch_${ch.id}"
                            ) { isFocused ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = ch.channelNumberFormatted,
                                            color = if (isFocused) Color.White else Color(0xFFD1D1D6),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                text = ch.network.ifBlank { ch.callSign },
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            if (ch.callSign.isNotBlank() && ch.callSign != ch.network) {
                                                Text(
                                                    text = ch.callSign,
                                                    color = TvTextSecondary,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }

                                    if (!ch.resolution.isNullOrBlank()) {
                                        Text(
                                            text = ch.resolution,
                                            color = TvTextTertiary,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Save Layout Preset Dialog ---

@Composable
fun SaveLayoutDialog(
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var presetName by remember { mutableStateOf("Sports Quad") }
    val suggestions = listOf("Sports Quad", "Morning News", "Prime Time", "Weekend EPG", "Football 4-Way")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF141416),
            border = BorderStroke(1.dp, Color(0xFF38383A)),
            modifier = Modifier
                .width(460.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "SAVE MULTIVIEW PRESET",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Save the current channels and grid arrangement to your presets library.",
                    color = TvTextSecondary,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF222226))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = presetName,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "QUICK SUGGESTIONS",
                    color = TvTextTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    suggestions.take(3).forEach { suggestion ->
                        TvButton(
                            text = suggestion,
                            onClick = { presetName = suggestion },
                            style = if (presetName == suggestion) TvButtonStyle.PRIMARY else TvButtonStyle.OUTLINE,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TvButton(
                        text = "Save Preset",
                        onClick = { onSave(presetName) },
                        style = TvButtonStyle.PRIMARY,
                        modifier = Modifier.weight(1f)
                    )
                    TvButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        style = TvButtonStyle.OUTLINE,
                        modifier = Modifier.weight(0.8f)
                    )
                }
            }
        }
    }
}
