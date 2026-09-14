package com.example.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.ui.components.TvTopBar
import com.example.ui.theme.CompetitorAppBg
import com.example.ui.theme.CompetitorCardBg
import com.example.ui.theme.CompetitorCardBorder
import com.example.ui.theme.CompetitorGreen
import com.example.ui.theme.CompetitorLayoutBlue
import com.example.ui.theme.CompetitorPurple
import com.example.ui.theme.CompetitorTabInactive
import com.example.ui.theme.TvTextPrimary
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.TabloAppViewModel
import kotlinx.coroutines.delay

@Composable
fun MultiviewScreen(
    viewModel: TabloAppViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.multiviewState.collectAsState()
    val allChannels by viewModel.channelRepository.channels.collectAsState()
    val currentDevice by viewModel.deviceRepository.currentDevice.collectAsState()
    val connectionState by viewModel.deviceRepository.connectionState.collectAsState()

    var showControls by remember { mutableStateOf(true) }
    var userActivityCounter by remember { mutableLongStateOf(System.currentTimeMillis()) }

    fun keepControlsVisible() {
        showControls = true
        userActivityCounter = System.currentTimeMillis()
    }

    // Auto-hide controls after 8 seconds of inactivity if in full screen
    LaunchedEffect(userActivityCounter, state.isFullScreenSingle) {
        if (state.isFullScreenSingle) {
            delay(5000)
            showControls = false
        } else {
            showControls = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CompetitorAppBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                keepControlsVisible()
            }
    ) {
        // macOS / iPad Style Top Navigation Bar (Screenshot 3)
        // Hidden only when user is in 100% immersive full-screen mode
        if (!state.isFullScreenSingle || showControls) {
            TvTopBar(
                title = "Multiview",
                device = currentDevice,
                connectionState = connectionState,
                activeScreen = AppScreen.MULTIVIEW,
                onNavigate = { screen -> viewModel.navigateTo(screen) },
                onOpenSettings = { viewModel.navigateTo(AppScreen.SETTINGS) }
            )

            // Sleek Multiview Quick Command HUD Bar (Screenshot 3 & 4)
            MultiviewCommandHud(
                state = state,
                viewModel = viewModel,
                onInteract = { keepControlsVisible() }
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Main Multiview Video Grid (Supports 4, 3, 2, or 1 active streams)
            MultiviewGridLayout(
                state = state,
                viewModel = viewModel,
                onUserInteract = { keepControlsVisible() },
                modifier = Modifier.fillMaxSize()
            )

            // Bottom Channel Switcher Drawer
            androidx.compose.animation.AnimatedVisibility(
                visible = state.isChannelPickerOpen,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                CompetitorChannelPickerDrawer(
                    activePaneIndex = state.activePaneIndex,
                    channels = allChannels,
                    onSelectChannel = { channel ->
                        viewModel.switchChannelForActivePane(channel)
                    },
                    onClose = { viewModel.closeChannelPicker() }
                )
            }
        }
    }
}

/**
 * Top command HUD matching the competitor's layout pills, audio router, and channel actions.
 */
@Composable
private fun MultiviewCommandHud(
    state: com.example.model.MultiviewUiState,
    viewModel: TabloAppViewModel,
    onInteract: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CompetitorAppBg)
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Layout Mode Switcher Pills (Single, Split 2, Triple 3, Quad 4)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            LayoutModeHudPill(
                label = "Single",
                icon = {
                    Box(
                        modifier = Modifier
                            .size(width = 12.dp, height = 9.dp)
                            .border(1.dp, if (state.isFullScreenSingle || state.layoutMode == MultiviewLayoutMode.ONE_PANE) Color.White else CompetitorTabInactive, RoundedCornerShape(1.dp))
                    )
                },
                isSelected = state.isFullScreenSingle || state.layoutMode == MultiviewLayoutMode.ONE_PANE,
                onClick = {
                    onInteract()
                    if (!state.isFullScreenSingle) {
                        viewModel.toggleFullScreenActivePane()
                    }
                }
            )

            LayoutModeHudPill(
                label = "Split (2)",
                icon = {
                    Row(horizontalArrangement = Arrangement.spacedBy(1.5.dp)) {
                        Box(
                            modifier = Modifier
                                .size(width = 6.dp, height = 9.dp)
                                .border(1.dp, if (state.layoutMode == MultiviewLayoutMode.TWO_PANE && !state.isFullScreenSingle) Color.White else CompetitorTabInactive, RoundedCornerShape(1.dp))
                        )
                        Box(
                            modifier = Modifier
                                .size(width = 6.dp, height = 9.dp)
                                .border(1.dp, if (state.layoutMode == MultiviewLayoutMode.TWO_PANE && !state.isFullScreenSingle) Color.White else CompetitorTabInactive, RoundedCornerShape(1.dp))
                        )
                    }
                },
                isSelected = state.layoutMode == MultiviewLayoutMode.TWO_PANE && !state.isFullScreenSingle,
                onClick = {
                    onInteract()
                    viewModel.changeLayoutMode(MultiviewLayoutMode.TWO_PANE)
                }
            )

            LayoutModeHudPill(
                label = "Triple (3)",
                icon = {
                    Row(horizontalArrangement = Arrangement.spacedBy(1.5.dp)) {
                        Box(
                            modifier = Modifier
                                .size(width = 7.dp, height = 9.dp)
                                .border(1.dp, if (state.layoutMode == MultiviewLayoutMode.THREE_PANE && !state.isFullScreenSingle) Color.White else CompetitorTabInactive, RoundedCornerShape(1.dp))
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(width = 5.dp, height = 4.dp)
                                    .border(1.dp, if (state.layoutMode == MultiviewLayoutMode.THREE_PANE && !state.isFullScreenSingle) Color.White else CompetitorTabInactive, RoundedCornerShape(1.dp))
                            )
                            Box(
                                modifier = Modifier
                                    .size(width = 5.dp, height = 4.dp)
                                    .border(1.dp, if (state.layoutMode == MultiviewLayoutMode.THREE_PANE && !state.isFullScreenSingle) Color.White else CompetitorTabInactive, RoundedCornerShape(1.dp))
                            )
                        }
                    }
                },
                isSelected = state.layoutMode == MultiviewLayoutMode.THREE_PANE && !state.isFullScreenSingle,
                onClick = {
                    onInteract()
                    viewModel.changeLayoutMode(MultiviewLayoutMode.THREE_PANE)
                }
            )

            LayoutModeHudPill(
                label = "Quad (4)",
                icon = {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(1.5.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(width = 6.dp, height = 4.dp)
                                    .border(1.dp, if (state.layoutMode == MultiviewLayoutMode.FOUR_PANE && !state.isFullScreenSingle) Color.White else CompetitorTabInactive, RoundedCornerShape(1.dp))
                            )
                            Box(
                                modifier = Modifier
                                    .size(width = 6.dp, height = 4.dp)
                                    .border(1.dp, if (state.layoutMode == MultiviewLayoutMode.FOUR_PANE && !state.isFullScreenSingle) Color.White else CompetitorTabInactive, RoundedCornerShape(1.dp))
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(1.5.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(width = 6.dp, height = 4.dp)
                                    .border(1.dp, if (state.layoutMode == MultiviewLayoutMode.FOUR_PANE && !state.isFullScreenSingle) Color.White else CompetitorTabInactive, RoundedCornerShape(1.dp))
                            )
                            Box(
                                modifier = Modifier
                                    .size(width = 6.dp, height = 4.dp)
                                    .border(1.dp, if (state.layoutMode == MultiviewLayoutMode.FOUR_PANE && !state.isFullScreenSingle) Color.White else CompetitorTabInactive, RoundedCornerShape(1.dp))
                            )
                        }
                    }
                },
                isSelected = state.layoutMode == MultiviewLayoutMode.FOUR_PANE && !state.isFullScreenSingle,
                onClick = {
                    onInteract()
                    viewModel.changeLayoutMode(MultiviewLayoutMode.FOUR_PANE)
                }
            )
        }

        // Active Audio Indicator + Quick Actions
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Audio Routing Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1E212A))
                    .border(1.dp, CompetitorCardBorder, RoundedCornerShape(14.dp))
                    .clickable {
                        onInteract()
                        viewModel.togglePaneAudio(state.activePaneIndex)
                    }
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedEqualizer(barColor = CompetitorPurple, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Audio: Pane ${state.activePaneIndex + 1}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            // Change Channel Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(CompetitorPurple)
                    .clickable {
                        onInteract()
                        viewModel.openChannelPicker(state.activePaneIndex)
                    }
                    .padding(horizontal = 12.dp, vertical = 5.dp)
                    .testTag("btn_change_channel_hud")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Swap Channel",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            // Fullscreen Zoom Toggle
            IconButton(
                onClick = {
                    onInteract()
                    viewModel.toggleFullScreenActivePane()
                },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = if (state.isFullScreenSingle) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = "Toggle Fullscreen",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun LayoutModeHudPill(
    label: String,
    icon: @Composable () -> Unit,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) CompetitorPurple else CompetitorCardBg)
            .border(1.dp, if (isSelected) CompetitorPurple else CompetitorCardBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else CompetitorTabInactive
            )
        }
    }
}

/**
 * Main Multiview Grid Layout supporting 4-pane, 2-pane, 3-pane, and 1-pane.
 */
@Composable
fun MultiviewGridLayout(
    state: com.example.model.MultiviewUiState,
    viewModel: TabloAppViewModel,
    onUserInteract: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.isFullScreenSingle || state.layoutMode == MultiviewLayoutMode.ONE_PANE) {
        val activePane = state.panes.getOrNull(state.activePaneIndex) ?: state.panes.first()
        MultiviewVideoPane(
            paneState = activePane,
            isActive = true,
            isSinglePane = true,
            onFocus = { viewModel.setActivePane(activePane.paneIndex) },
            onClick = onUserInteract,
            onDoubleClick = { viewModel.toggleFullScreenActivePane() },
            viewModel = viewModel,
            modifier = modifier
        )
        return
    }

    when (state.layoutMode) {
        MultiviewLayoutMode.FOUR_PANE -> {
            // 4 Games Quad Grid (2x2) - Exactly matching Screenshot 3
            Column(
                modifier = modifier.padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (i in 0..1) {
                        val pane = state.panes.getOrNull(i) ?: PaneState(paneIndex = i)
                        MultiviewVideoPane(
                            paneState = pane,
                            isActive = pane.paneIndex == state.activePaneIndex,
                            isSinglePane = false,
                            onFocus = {
                                viewModel.setActivePane(pane.paneIndex)
                                onUserInteract()
                            },
                            onClick = {
                                viewModel.setActivePane(pane.paneIndex)
                                onUserInteract()
                            },
                            onDoubleClick = { viewModel.toggleFullScreenActivePane() },
                            viewModel = viewModel,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (i in 2..3) {
                        val pane = state.panes.getOrNull(i) ?: PaneState(paneIndex = i)
                        MultiviewVideoPane(
                            paneState = pane,
                            isActive = pane.paneIndex == state.activePaneIndex,
                            isSinglePane = false,
                            onFocus = {
                                viewModel.setActivePane(pane.paneIndex)
                                onUserInteract()
                            },
                            onClick = {
                                viewModel.setActivePane(pane.paneIndex)
                                onUserInteract()
                            },
                            onDoubleClick = { viewModel.toggleFullScreenActivePane() },
                            viewModel = viewModel,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }
        }
        MultiviewLayoutMode.TWO_PANE -> {
            // 2 Games Split View
            Row(
                modifier = modifier.padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (i in 0..1) {
                    val pane = state.panes.getOrNull(i) ?: PaneState(paneIndex = i)
                    MultiviewVideoPane(
                        paneState = pane,
                        isActive = pane.paneIndex == state.activePaneIndex,
                        isSinglePane = false,
                        onFocus = {
                            viewModel.setActivePane(pane.paneIndex)
                            onUserInteract()
                        },
                        onClick = {
                            viewModel.setActivePane(pane.paneIndex)
                            onUserInteract()
                        },
                        onDoubleClick = { viewModel.toggleFullScreenActivePane() },
                        viewModel = viewModel,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }
        MultiviewLayoutMode.THREE_PANE -> {
            // 3 Games Focus View
            Row(
                modifier = modifier.padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val pane0 = state.panes.getOrNull(0) ?: PaneState(paneIndex = 0)
                MultiviewVideoPane(
                    paneState = pane0,
                    isActive = pane0.paneIndex == state.activePaneIndex,
                    isSinglePane = false,
                    onFocus = {
                        viewModel.setActivePane(0)
                        onUserInteract()
                    },
                    onClick = {
                        viewModel.setActivePane(0)
                        onUserInteract()
                    },
                    onDoubleClick = { viewModel.toggleFullScreenActivePane() },
                    viewModel = viewModel,
                    modifier = Modifier.weight(0.6f).fillMaxHeight()
                )
                Column(
                    modifier = Modifier.weight(0.4f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (i in 1..2) {
                        val pane = state.panes.getOrNull(i) ?: PaneState(paneIndex = i)
                        MultiviewVideoPane(
                            paneState = pane,
                            isActive = pane.paneIndex == state.activePaneIndex,
                            isSinglePane = false,
                            onFocus = {
                                viewModel.setActivePane(pane.paneIndex)
                                onUserInteract()
                            },
                            onClick = {
                                viewModel.setActivePane(pane.paneIndex)
                                onUserInteract()
                            },
                            onDoubleClick = { viewModel.toggleFullScreenActivePane() },
                            viewModel = viewModel,
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        )
                    }
                }
            }
        }
        MultiviewLayoutMode.ONE_PANE -> {}
    }
}

/**
 * Individual live video pane with hardware-accelerated ExoPlayer,
 * Royal Purple active focus border matching Screenshot 3, scorebug, and audio indicator.
 */
@OptIn(UnstableApi::class)
@Composable
fun MultiviewVideoPane(
    paneState: PaneState,
    isActive: Boolean,
    isSinglePane: Boolean,
    onFocus: () -> Unit,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    viewModel: TabloAppViewModel,
    modifier: Modifier = Modifier
) {
    var lastTapTime by remember { mutableLongStateOf(0L) }

    // Royal Purple focus border for active pane (Screenshot 3)
    val borderModifier = if (isSinglePane) {
        Modifier
    } else if (isActive) {
        Modifier.border(2.5.dp, CompetitorPurple, RoundedCornerShape(8.dp))
    } else {
        Modifier.border(1.dp, CompetitorCardBorder, RoundedCornerShape(8.dp))
    }

    Box(
        modifier = modifier
            .testTag("multiview_pane_${paneState.paneIndex}")
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black)
            .then(borderModifier)
            .onFocusChanged { if (it.isFocused) onFocus() }
            .focusable()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                val now = System.currentTimeMillis()
                if (now - lastTapTime < 350) {
                    onDoubleClick()
                } else {
                    onClick()
                }
                lastTapTime = now
            }
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyUp) {
                    when (keyEvent.key) {
                        Key.DirectionCenter, Key.Enter, Key.Spacebar -> {
                            onDoubleClick()
                            true
                        }
                        Key.DirectionDown -> {
                            viewModel.openChannelPicker(paneState.paneIndex)
                            true
                        }
                        Key.F -> {
                            onDoubleClick()
                            true
                        }
                        Key.A -> {
                            viewModel.togglePaneAudio(paneState.paneIndex)
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        // ExoPlayer Hardware-Accelerated Video Surface
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

        // Top-Left Broadcast Network Badge
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xCC13151D))
                .border(1.dp, CompetitorCardBorder, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(CompetitorGreen)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = paneState.channel?.network?.ifBlank { "Pane ${paneState.paneIndex + 1}" } ?: "Pane ${paneState.paneIndex + 1}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                if (!paneState.channel?.channelNumberFormatted.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = paneState.channel?.channelNumberFormatted ?: "",
                        color = CompetitorTabInactive,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Live Airing or Matchup Info (Bottom Overlay)
        val scoreInfo = paneState.channel?.scoreBug ?: paneState.airing?.title
        if (!scoreInfo.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xDD13151D))
                    .border(1.dp, CompetitorCardBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = scoreInfo,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Top-Right Active Audio Badge
        if (isActive && !isSinglePane) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(CompetitorPurple)
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedEqualizer(
                        barColor = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "AUDIO",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Loading / Buffering Indicator
        if (paneState.playbackState == StreamPlaybackState.LOADING ||
            paneState.playbackState == StreamPlaybackState.BUFFERING
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = CompetitorPurple,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tuning stream...",
                        color = TvTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Error Retry Overlay
        if (paneState.playbackState == StreamPlaybackState.ERROR) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Signal lost or tuner busy",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(CompetitorPurple)
                            .clickable {
                                paneState.channel?.let {
                                    viewModel.startStreamForPane(paneState.paneIndex, it)
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Retry",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Bottom Channel Switcher Drawer with Competitor styling.
 */
@Composable
private fun CompetitorChannelPickerDrawer(
    activePaneIndex: Int,
    channels: List<TabloChannel>,
    onSelectChannel: (TabloChannel) -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(Color(0xF2141720))
            .border(1.dp, CompetitorCardBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Switch Channel for Pane ${activePaneIndex + 1}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = CompetitorTabInactive,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(channels, key = { it.id }) { channel ->
                    Box(
                        modifier = Modifier
                            .width(140.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CompetitorCardBg)
                            .border(1.dp, CompetitorCardBorder, RoundedCornerShape(10.dp))
                            .clickable { onSelectChannel(channel) }
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = channel.network.ifBlank { channel.callSign },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${channel.major}.${channel.minor}",
                                fontSize = 11.sp,
                                color = CompetitorPurple,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Animated 3-bar audio equalizer indicating active sound stream.
 */
@Composable
fun AnimatedEqualizer(
    modifier: Modifier = Modifier,
    barColor: Color = CompetitorPurple
) {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer")

    val h1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h1"
    )

    val h2 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(320),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h2"
    )

    val h3 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(480),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h3"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .fillMaxHeight(h1)
                .clip(RoundedCornerShape(1.dp))
                .background(barColor)
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .fillMaxHeight(h2)
                .clip(RoundedCornerShape(1.dp))
                .background(barColor)
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .fillMaxHeight(h3)
                .clip(RoundedCornerShape(1.dp))
                .background(barColor)
        )
    }
}
