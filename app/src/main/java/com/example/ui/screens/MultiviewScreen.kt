package com.example.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.example.R
import com.example.model.MultiviewLayoutMode
import com.example.model.PaneState
import com.example.model.StreamDiagnostics
import com.example.model.StreamPlaybackState
import com.example.model.StreamQuality
import com.example.model.TabloChannel
import com.example.ui.components.TvButton
import com.example.ui.components.TvButtonStyle
import com.example.ui.components.TvFocusableCard
import com.example.ui.theme.TvAmberAccent
import com.example.ui.theme.TvBorderGrey
import com.example.ui.theme.TvCyanPrimary
import com.example.ui.theme.TvError
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary
import com.example.ui.theme.TvTextTertiary
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

    var showHud by remember { mutableStateOf(true) }
    var hudVersion by remember { mutableLongStateOf(System.currentTimeMillis()) }

    fun pingHud() {
        showHud = true
        hudVersion = System.currentTimeMillis()
    }

    LaunchedEffect(hudVersion) {
        if (showHud) {
            delay(4000)
            showHud = false
        }
    }

    // Android TV Remote Back Button Handling (YouTube TV & Sunday Ticket style)
    BackHandler(enabled = true) {
        when {
            state.isActionMenuOpen -> viewModel.toggleActionMenu(false)
            state.isChannelPickerOpen -> viewModel.closeChannelPicker()
            state.isSaveLayoutDialogOpen -> viewModel.closeSaveLayoutDialog()
            state.showQualityMenuForPane != null -> viewModel.openQualityMenu(null)
            state.isReorderMode -> viewModel.cancelReorderMode()
            showHud -> showHud = false
            state.isFullScreenSingle -> viewModel.toggleFullScreenActivePane()
            else -> viewModel.navigateTo(AppScreen.HOME)
        }
    }

    // PiP Mode: Render only active stream full bleed with no overlays
    if (state.isInPipMode) {
        val activePane = state.panes.getOrNull(state.activePaneIndex) ?: state.panes.first()
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            PipVideoView(paneIndex = activePane.paneIndex, viewModel = viewModel)
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (!showHud) pingHud() else showHud = false
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
                                viewModel.toggleFullScreenActivePane()
                            } else {
                                pingHud()
                            }
                            true
                        }
                        Key.DirectionDown -> {
                            viewModel.togglePlaybackOverlay(true)
                            pingHud()
                            true
                        }
                        Key.DirectionUp -> {
                            if (showHud || state.isPlaybackOverlayVisible) {
                                showHud = false
                                viewModel.togglePlaybackOverlay(false)
                                true
                            } else false
                        }
                        Key.S -> {
                            viewModel.openStatsOverlay()
                            true
                        }
                        Key.B -> {
                            viewModel.openMultiviewBuilder()
                            true
                        }
                        Key.F -> {
                            viewModel.toggleFullScreenActivePane()
                            true
                        }
                        Key.A -> {
                            viewModel.togglePaneAudio(state.activePaneIndex)
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

        // Top Command Bar Header Overlay (Animated with HUD)
        AnimatedVisibility(
            visible = showHud && !state.isActionMenuOpen && !state.isChannelPickerOpen && !state.isSaveLayoutDialogOpen && state.showQualityMenuForPane == null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 16.dp)
        ) {
            MultiviewTopHeaderBar(
                state = state,
                viewModel = viewModel,
                onPingHud = { pingHud() }
            )
        }

        // Leanback Media Controls Overlay: Bottom-third anchored carousel & actions
        AnimatedVisibility(
            visible = showHud && !state.isActionMenuOpen && !state.isChannelPickerOpen && !state.isSaveLayoutDialogOpen && state.showQualityMenuForPane == null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
        ) {
            LeanbackBottomControlBar(
                state = state,
                channels = allChannels,
                viewModel = viewModel,
                onPingHud = { pingHud() }
            )
        }

        // Reorder Mode Active Banner
        if (state.isReorderMode) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = TvAmberAccent.copy(alpha = 0.95f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Icon(
                        Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "SWAP MODE: Select target pane to switch positions with Pane ${state.activePaneIndex + 1}",
                        color = Color.Black,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    TvButton(
                        text = "Cancel",
                        onClick = { viewModel.cancelReorderMode() },
                        style = TvButtonStyle.SECONDARY,
                        modifier = Modifier.height(32.dp)
                    )
                }
            }
        }

        // Action Menu Dialog Modal
        if (state.isActionMenuOpen) {
            MultiviewActionMenu(
                state = state,
                viewModel = viewModel,
                onDismiss = { viewModel.toggleActionMenu(false) }
            )
        }

        // Quick Channel Picker Modal
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

        // Quality Selection Dropdown Modal
        state.showQualityMenuForPane?.let { paneIndex ->
            val pane = state.panes.getOrNull(paneIndex)
            QualitySelectorModal(
                currentQuality = pane?.quality ?: StreamQuality.AUTO,
                paneIndex = paneIndex,
                onSelectQuality = { quality ->
                    viewModel.setPaneQuality(paneIndex, quality)
                },
                onDismiss = { viewModel.openQualityMenu(null) }
            )
        }

        // Save Layout Dialog Modal
        if (state.isSaveLayoutDialogOpen) {
            SaveLayoutDialog(
                onSave = { name -> viewModel.saveCurrentLayout(name) },
                onDismiss = { viewModel.closeSaveLayoutDialog() }
            )
        }

        // YouTube TV Playback Overlay (Triggered by remote DOWN key or screen click)
        AnimatedVisibility(
            visible = showHud || state.isPlaybackOverlayVisible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            YouTubeTvPlaybackOverlay(
                state = state,
                viewModel = viewModel,
                activePane = state.panes.getOrElse(state.activePaneIndex) { state.panes[0] },
                onDismiss = {
                    showHud = false
                    viewModel.togglePlaybackOverlay(false)
                }
            )
        }

        // YouTube TV Multiview Builder (Custom Selection modal)
        if (state.isMultiviewBuilderOpen) {
            YouTubeTvMultiviewBuilderModal(
                allChannels = allChannels,
                onDismiss = { viewModel.closeMultiviewBuilder() },
                onLaunchMultiview = { selectedList ->
                    viewModel.launchCustomMultiview(selectedList)
                }
            )
        }

        // YouTube TV Fantasy & Stats Overlay Modal
        if (state.isStatsOverlayOpen) {
            YouTubeTvStatsOverlayModal(
                activePane = state.panes.getOrElse(state.activePaneIndex) { state.panes[0] },
                onDismiss = { viewModel.closeStatsOverlay() }
            )
        }
    }
}

// --- Multiview Top Header Bar ---

@Composable
fun MultiviewTopHeaderBar(
    state: com.example.model.MultiviewUiState,
    viewModel: TabloAppViewModel,
    onPingHud: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xF210131A),
        border = BorderStroke(1.dp, Color(0xFF252A36)),
        shadowElevation = 10.dp,
        modifier = Modifier.fillMaxWidth().widthIn(max = 1100.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Brand Logo & Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, TvCyanPrimary.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_app_brand_logo),
                        contentDescription = "Tablo Multiview Logo",
                        modifier = Modifier.size(30.dp).clip(RoundedCornerShape(5.dp))
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "TABLO",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = " MULTIVIEW",
                    color = TvCyanPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Mode Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1E2330))
                        .border(1.dp, Color(0xFF32394A), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    val modeLabel = when (state.layoutMode) {
                        MultiviewLayoutMode.ONE_PANE -> "1 SCREEN (SOLO)"
                        MultiviewLayoutMode.TWO_PANE -> "2 SCREENS (SPLIT)"
                        MultiviewLayoutMode.THREE_PANE -> "3 SCREENS (GAMEDAY FOCUS)"
                        MultiviewLayoutMode.FOUR_PANE -> "4 SCREENS (QUAD VIEW)"
                    }
                    Text(
                        text = modeLabel,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Quick Nav Shortcuts
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TvButton(
                    text = "Guide",
                    onClick = { viewModel.navigateTo(AppScreen.CHANNEL_GUIDE) },
                    style = TvButtonStyle.SECONDARY,
                    modifier = Modifier.height(32.dp),
                    leadingIcon = { Icon(Icons.Default.List, contentDescription = "Guide", tint = Color.White, modifier = Modifier.size(16.dp)) },
                    testTag = "btn_header_guide"
                )
                TvButton(
                    text = "Presets",
                    onClick = { viewModel.navigateTo(AppScreen.SAVED_LAYOUTS) },
                    style = TvButtonStyle.SECONDARY,
                    modifier = Modifier.height(32.dp),
                    leadingIcon = { Icon(Icons.Default.Bookmark, contentDescription = "Presets", tint = Color.White, modifier = Modifier.size(16.dp)) },
                    testTag = "btn_header_presets"
                )
                TvButton(
                    text = "Save",
                    onClick = { viewModel.openSaveLayoutDialog() },
                    style = TvButtonStyle.SECONDARY,
                    modifier = Modifier.height(32.dp),
                    testTag = "btn_header_save"
                )
                TvButton(
                    text = "Home",
                    onClick = { viewModel.navigateTo(AppScreen.HOME) },
                    style = TvButtonStyle.SECONDARY,
                    modifier = Modifier.height(32.dp),
                    leadingIcon = { Icon(Icons.Default.Home, contentDescription = "Home", tint = Color.White, modifier = Modifier.size(16.dp)) },
                    testTag = "btn_header_home"
                )
            }
        }
    }
}

// --- Leanback Bottom Controls Bar ---

@Composable
fun LeanbackBottomControlBar(
    state: com.example.model.MultiviewUiState,
    channels: List<TabloChannel>,
    viewModel: TabloAppViewModel,
    onPingHud: () -> Unit
) {
    val activePane = state.panes.getOrNull(state.activePaneIndex)
    val activeChannel = activePane?.channel

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xF210131A),
        border = BorderStroke(1.dp, Color(0xFF252A36)),
        shadowElevation = 14.dp,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 1100.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            // Top Row: Active stream identity & primary actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Active pane indicator with cyan accent
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(TvCyanPrimary)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "PANE ${state.activePaneIndex + 1}",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = activeChannel?.displayTitle ?: "Select Game or Channel",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val airingTitle = activePane?.airing?.title ?: activeChannel?.displaySubtitle
                        if (!airingTitle.isNullOrBlank()) {
                            Text(
                                text = airingTitle,
                                color = TvTextSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Grid layout quick toggles & clean action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isSinglePane = state.layoutMode == MultiviewLayoutMode.ONE_PANE

                    // Audio Toggle
                    val isAudioActive = viewModel.playerManager.isPaneAudioActive(state.activePaneIndex)
                    TvButton(
                        text = if (isAudioActive) "Audio On" else "Muted",
                        onClick = {
                            viewModel.togglePaneAudio(state.activePaneIndex)
                            onPingHud()
                        },
                        style = if (isAudioActive) TvButtonStyle.PRIMARY else TvButtonStyle.SECONDARY,
                        minHeight = 36.dp,
                        leadingIcon = {
                            Icon(
                                if (isAudioActive) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                                contentDescription = null,
                                tint = if (isAudioActive) Color.Black else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        testTag = "btn_hud_audio"
                    )

                    // Fullscreen / Multiview Toggle
                    TvButton(
                        text = if (isSinglePane) "Multiview" else "Full Screen",
                        onClick = {
                            viewModel.toggleFullScreenActivePane()
                            onPingHud()
                        },
                        style = TvButtonStyle.SECONDARY,
                        minHeight = 36.dp,
                        leadingIcon = {
                            Icon(
                                if (isSinglePane) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        testTag = "btn_hud_fullscreen"
                    )

                    // Layout Mode Selector Pills (1, 2, 3, 4)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1A1E28))
                            .border(1.dp, Color(0xFF2C3240), RoundedCornerShape(8.dp))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        listOf(
                            MultiviewLayoutMode.ONE_PANE to "1",
                            MultiviewLayoutMode.TWO_PANE to "2",
                            MultiviewLayoutMode.THREE_PANE to "3",
                            MultiviewLayoutMode.FOUR_PANE to "4"
                        ).forEach { (mode, label) ->
                            val isSelected = state.layoutMode == mode
                            TvFocusableCard(
                                onClick = {
                                    viewModel.changeLayoutMode(mode)
                                    onPingHud()
                                },
                                modifier = Modifier.size(width = 34.dp, height = 32.dp),
                                focusedContainerColor = TvCyanPrimary,
                                unfocusedContainerColor = if (isSelected) Color(0xFF2E3547) else Color.Transparent,
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = if (isSelected) TvCyanPrimary else Color.Transparent,
                                testTag = "btn_layout_pill_$label"
                            ) { isFocused ->
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        color = if (isFocused) Color.Black else if (isSelected) Color.White else Color(0xFF9E9EA7),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Change Channel Modal
                    TvButton(
                        text = "Channels",
                        onClick = {
                            viewModel.openChannelPicker(state.activePaneIndex)
                            onPingHud()
                        },
                        style = TvButtonStyle.SECONDARY,
                        minHeight = 36.dp,
                        leadingIcon = {
                            Icon(Icons.Default.List, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        },
                        testTag = "btn_hud_channels"
                    )

                    // Menu button
                    TvButton(
                        text = "Menu",
                        onClick = { viewModel.toggleActionMenu(true) },
                        style = TvButtonStyle.SECONDARY,
                        minHeight = 36.dp,
                        leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp)) },
                        testTag = "btn_hud_menu"
                    )
                }
            }

            // Bottom Row: Quick-channel flipping carousel
            if (channels.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(channels) { ch ->
                        val isCurrent = ch.id == activeChannel?.id
                        TvFocusableCard(
                            onClick = {
                                viewModel.switchChannelForActivePane(ch)
                                onPingHud()
                            },
                            modifier = Modifier
                                .width(130.dp)
                                .height(46.dp),
                            focusedContainerColor = Color(0xFF2C2C34),
                            unfocusedContainerColor = if (isCurrent) Color(0xFF1E2533) else Color(0xFF16181F),
                            focusedBorderColor = TvCyanPrimary,
                            unfocusedBorderColor = if (isCurrent) TvCyanPrimary.copy(alpha = 0.8f) else Color(0xFF282C38),
                            testTag = "carousel_ch_${ch.id}"
                        ) { isFocused ->
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = ch.channelNumberFormatted,
                                    color = if (isFocused) Color.White else Color(0xFFD1D1D6),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = ch.network.ifBlank { ch.callSign },
                                    color = if (isCurrent) TvCyanPrimary else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
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
        // Primary Viewport (Pane 0): Left 65%
        if (panes.isNotEmpty()) {
            val primary = panes[0]
            MultiviewVideoPane(
                paneState = primary,
                isActive = primary.paneIndex == activePaneIndex,
                showOverlay = showOverlay,
                onFocus = { onFocus(primary.paneIndex) },
                onClick = onClick,
                onRetry = { onRetry(primary.paneIndex) },
                viewModel = viewModel,
                modifier = Modifier
                    .weight(1.8f)
                    .fillMaxHeight()
            )
        }

        // Secondary Viewports (Panes 1 & 2): Right 35% stacked vertically
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            for (i in 1..2) {
                if (i < panes.size) {
                    val pane = panes[i]
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
                            .fillMaxWidth()
                    )
                }
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

// --- Individual Video Pane with Spatial Focus & HUD Controls ---

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

    // Spatial Focus Border: YouTube TV white highlight box on active sub-window
    val borderModifier = if (isSinglePane) {
        Modifier
    } else if (isActive) {
        Modifier.border(3.5.dp, Color.White)
    } else {
        Modifier.border(1.dp, Color(0xFF252A36))
    }

    var lastTapTime by remember { mutableLongStateOf(0L) }

    if (paneState.channel == null) {
        Box(
            modifier = modifier
                .testTag("empty_pane_${paneState.paneIndex}")
                .background(Color(0xFF0D0F14))
                .then(borderModifier)
                .onFocusChanged { if (it.isFocused) onFocus() }
                .focusable()
                .clickable { viewModel.openChannelPicker(paneState.paneIndex) },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E2330)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add View",
                        tint = TvCyanPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Pane ${paneState.paneIndex + 1}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Press SELECT to Choose Game",
                    color = TvTextSecondary,
                    fontSize = 11.sp
                )
            }
        }
        return
    }

    Box(
        modifier = modifier
            .testTag("pane_${paneState.paneIndex}")
            .background(Color.Black)
            .then(borderModifier)
            .onFocusChanged { if (it.isFocused) onFocus() }
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyUp) {
                    when (keyEvent.key) {
                        Key.DirectionCenter, Key.Enter, Key.Spacebar -> {
                            val mode = viewModel.multiviewState.value.layoutMode
                            if (mode != MultiviewLayoutMode.ONE_PANE) {
                                viewModel.toggleFullScreenActivePane()
                            } else {
                                if (!showOverlay) onClick() else viewModel.toggleFullScreenActivePane()
                            }
                            true
                        }
                        Key.DirectionDown -> {
                            onClick()
                            true
                        }
                        Key.F -> {
                            viewModel.toggleFullScreenActivePane()
                            true
                        }
                        Key.A -> {
                            viewModel.togglePaneAudio(paneState.paneIndex)
                            true
                        }
                        Key.D -> {
                            viewModel.togglePaneDiagnostics(paneState.paneIndex)
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                val now = System.currentTimeMillis()
                if (now - lastTapTime < 350) {
                    viewModel.toggleFullScreenActivePane()
                } else {
                    onClick()
                }
                lastTapTime = now
            }
    ) {
        // Video View via ExoPlayer
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

        // Real-Time Technical Stream Diagnostics Overlay (Top Right)
        if (paneState.showDiagnostics) {
            StreamDiagnosticsHud(
                diagnostics = paneState.diagnostics,
                quality = paneState.quality,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            )
        }

        // YouTube TV Signature Active Audio Badge on focused stream
        if (isActive && !isSinglePane) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.85f))
                    .border(1.dp, Color.White, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Active Audio",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "(Active Audio)",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Top Channel Info Pill (Clean YouTube TV pill style)
        AnimatedVisibility(
            visible = showOverlay,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xE610131A))
                    .border(1.dp, Color(0xFF252A36), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // LIVE broadcast indicator dot
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E676))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = paneState.channel?.channelNumberFormatted ?: "PANE ${paneState.paneIndex + 1}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = paneState.channel?.network?.ifBlank { paneState.channel?.callSign } ?: "Tablo Feed",
                        color = Color(0xFFE5E5EA),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    // Audio Routing Indicator
                    val isAudioActive = viewModel.playerManager.isPaneAudioActive(paneState.paneIndex)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isAudioActive) TvCyanPrimary else Color(0xFF1E2330))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isAudioActive) "AUDIO ON" else "MUTED",
                            color = if (isAudioActive) Color.Black else TvTextSecondary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
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
                        modifier = Modifier.size(28.dp),
                        color = TvCyanPrimary,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (paneState.playbackState == StreamPlaybackState.LOADING) "Tuning live channel..." else "Buffering stream...",
                        color = TvTextPrimary,
                        fontSize = 12.sp,
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
                    .background(Color.Black.copy(alpha = 0.82f))
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
                        text = paneState.errorMessage ?: "Check tuner availability on Tablo",
                        color = TvTextSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TvButton(
                            text = "Retry",
                            onClick = onRetry,
                            style = TvButtonStyle.AMBER,
                            modifier = Modifier.height(36.dp)
                        )
                        TvButton(
                            text = "Pick Channel",
                            onClick = { viewModel.openChannelPicker(paneState.paneIndex) },
                            style = TvButtonStyle.SECONDARY,
                            modifier = Modifier.height(36.dp)
                        )
                    }
                }
            }
        }
    }
}

// --- Real-Time Stream Diagnostics HUD ---

@Composable
fun StreamDiagnosticsHud(
    diagnostics: StreamDiagnostics,
    quality: StreamQuality,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xD90A0A0E),
        border = BorderStroke(1.dp, Color(0xFF2C2C34)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.widthIn(min = 160.dp)
            ) {
                Text(
                    text = "LIVE DIAGNOSTICS",
                    color = TvCyanPrimary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = quality.name,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "Bitrate: ${if (diagnostics.bitrateKbps > 0) "${diagnostics.bitrateKbps} kbps" else "Adaptive"}",
                color = Color.White,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Res: ${diagnostics.resolution} @ ${diagnostics.framerate.toInt()}fps",
                color = Color(0xFFD1D1D6),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Decoder: ${diagnostics.decoder}",
                color = if (diagnostics.isHardwareAccelerated) Color(0xFF34C759) else Color(0xFFFF9500),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Drops: ${diagnostics.droppedFrames} | Buf: ${diagnostics.bufferHealthMs}ms",
                color = Color(0xFFA1A1AA),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun IconButtonWithTooltip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF1E1E24),
        border = BorderStroke(1.dp, Color(0xFF323238)),
        modifier = Modifier
            .testTag(testTag)
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = label, color = tint, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// --- Quality Selector Modal ---

@Composable
fun QualitySelectorModal(
    currentQuality: StreamQuality,
    paneIndex: Int,
    onSelectQuality: (StreamQuality) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF141418),
            border = BorderStroke(1.dp, Color(0xFF38383E)),
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth(0.88f)
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
                        text = "PANE ${paneIndex + 1} QUALITY",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    TvButton(
                        text = "Close",
                        onClick = onDismiss,
                        style = TvButtonStyle.OUTLINE
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                StreamQuality.values().forEach { quality ->
                    val isSelected = quality == currentQuality
                    TvFocusableCard(
                        onClick = { onSelectQuality(quality) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .padding(vertical = 3.dp),
                        focusedContainerColor = Color(0xFF26262C),
                        unfocusedContainerColor = if (isSelected) Color(0xFF202026) else Color(0xFF16161A),
                        focusedBorderColor = TvCyanPrimary,
                        unfocusedBorderColor = if (isSelected) TvCyanPrimary.copy(alpha = 0.7f) else Color(0xFF2A2A30),
                        testTag = "quality_${quality.name}"
                    ) { isFocused ->
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = quality.label,
                                color = if (isFocused || isSelected) Color.White else Color(0xFFD1D1D6),
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = TvCyanPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
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
                        style = TvButtonStyle.OUTLINE
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Primary Quick Actions Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TvButton(
                        text = "Change Channel",
                        onClick = { viewModel.openChannelPicker() },
                        style = TvButtonStyle.PRIMARY,
                        modifier = Modifier.weight(1f),
                        leadingIcon = { Icon(Icons.Default.List, contentDescription = null, tint = Color.Black) }
                    )

                    if (state.layoutMode != MultiviewLayoutMode.FOUR_PANE) {
                        TvButton(
                            text = "+ Add Another View",
                            onClick = {
                                viewModel.addNextView()
                                onDismiss()
                            },
                            style = TvButtonStyle.AMBER,
                            modifier = Modifier.weight(1f),
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black) }
                        )
                    } else {
                        TvButton(
                            text = if (state.isFullScreenSingle) "Restore Grid" else "Full Screen",
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
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.layoutMode != MultiviewLayoutMode.ONE_PANE) {
                        TvButton(
                            text = "Close View (Free Tuner)",
                            onClick = {
                                viewModel.clearPane(state.activePaneIndex)
                                onDismiss()
                            },
                            style = TvButtonStyle.OUTLINE,
                            modifier = Modifier.weight(1f),
                            leadingIcon = { Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFFF453A)) }
                        )
                    }

                    TvButton(
                        text = "Swap Positions",
                        onClick = { viewModel.startReorderMode() },
                        style = TvButtonStyle.SECONDARY,
                        modifier = Modifier.weight(1f),
                        leadingIcon = { Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = Color.White) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Layout Switcher
                Text(
                    text = "MULTIVIEW LAYOUT",
                    color = TvTextTertiary,
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
                            MultiviewLayoutMode.ONE_PANE -> "1 (Solo)"
                            MultiviewLayoutMode.TWO_PANE -> "2 (Split)"
                            MultiviewLayoutMode.THREE_PANE -> "3 (Focus)"
                            MultiviewLayoutMode.FOUR_PANE -> "4 (Quad)"
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
                                            if (ch.callSign.isNotBlank() && ch.network.isNotBlank()) {
                                                Text(
                                                    text = ch.callSign,
                                                    color = TvTextTertiary,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }

                                    if (!ch.resolution.isNullOrBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF2C2C2E))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = ch.resolution,
                                                color = Color(0xFFA1A1AA),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
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
}

// --- Save Layout Dialog Modal ---

@Composable
fun SaveLayoutDialog(
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var presetName by remember { mutableStateOf("Game Day Quad") }
    val suggestions = listOf("Game Day Quad", "Prime News", "Morning Shows", "Favorite 4")

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

// --- Picture-in-Picture Video View ---

@OptIn(UnstableApi::class)
@Composable
fun PipVideoView(
    paneIndex: Int,
    viewModel: TabloAppViewModel
) {
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
                player = viewModel.playerManager.getPlayer(paneIndex)
            }
        },
        update = { playerView ->
            val player = viewModel.playerManager.getPlayer(paneIndex)
            if (playerView.player != player) {
                playerView.player = player
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

// --- YouTube TV Playback Overlay ---

@Composable
fun YouTubeTvPlaybackOverlay(
    state: com.example.model.MultiviewUiState,
    viewModel: TabloAppViewModel,
    activePane: PaneState,
    onDismiss: () -> Unit
) {
    val isSinglePane = state.layoutMode == MultiviewLayoutMode.ONE_PANE || state.isFullScreenSingle
    var libraryAddedToast by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = Color(0xF70C0E14),
        border = BorderStroke(1.dp, Color(0xFF282F3E)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("youtube_tv_playback_overlay")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 18.dp)
        ) {
            // Live broadcast info & scrub bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // LIVE Red Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF2B1010))
                            .border(1.dp, Color(0xFFFF0000).copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF0000))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "LIVE",
                                color = Color(0xFFFF4444),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = activePane.channel?.channelNumberFormatted ?: "CH 4.1",
                        color = TvAmberAccent,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = activePane.channel?.network?.ifBlank { activePane.channel?.callSign } ?: "Live Stream",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "• Live Programming",
                        color = TvTextSecondary,
                        fontSize = 13.sp
                    )
                }

                // Quick Close or Back hint
                Text(
                    text = "Press DOWN or BACK to close",
                    color = TvTextTertiary,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Live progress/buffer line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF242834))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(3.dp)
                        .background(Color(0xFFFF0000))
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // YouTube TV Action Control Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Multiview Builder Button
                TvButton(
                    text = "Build Multiview",
                    onClick = {
                        onDismiss()
                        viewModel.openMultiviewBuilder()
                    },
                    style = TvButtonStyle.PRIMARY,
                    leadingIcon = {
                        Icon(Icons.Default.GridView, contentDescription = null, tint = Color.Black)
                    },
                    testTag = "btn_overlay_multiview"
                )

                // Stats & Fantasy Button
                TvButton(
                    text = "Stats & Fantasy",
                    onClick = {
                        onDismiss()
                        viewModel.openStatsOverlay()
                    },
                    style = TvButtonStyle.OUTLINE,
                    leadingIcon = {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color.White)
                    },
                    testTag = "btn_overlay_stats"
                )

                // Add to Library (Cloud DVR)
                TvButton(
                    text = if (libraryAddedToast) "Added to Library" else "+ Add to Library",
                    onClick = {
                        libraryAddedToast = true
                        activePane.channel?.let { ch ->
                            viewModel.addToLibrary(
                                title = ch.network.ifBlank { ch.callSign } + " Broadcast",
                                channel = ch,
                                category = com.example.model.DvrCategory.SPORTS,
                                subtitle = "Live Cloud Recording"
                            )
                        }
                    },
                    style = if (libraryAddedToast) TvButtonStyle.SECONDARY else TvButtonStyle.OUTLINE,
                    leadingIcon = {
                        Icon(
                            if (libraryAddedToast) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White
                        )
                    },
                    testTag = "btn_overlay_add_library"
                )

                // Quality Selector
                TvButton(
                    text = "Quality: ${activePane.quality.label}",
                    onClick = {
                        viewModel.openQualityMenu(activePane.paneIndex)
                    },
                    style = TvButtonStyle.OUTLINE,
                    leadingIcon = {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = Color.White)
                    },
                    testTag = "btn_overlay_quality"
                )

                // Audio focus toggle
                if (!isSinglePane) {
                    TvButton(
                        text = "Audio: Pane ${activePane.paneIndex + 1}",
                        onClick = {
                            viewModel.togglePaneAudio(activePane.paneIndex)
                        },
                        style = TvButtonStyle.OUTLINE,
                        leadingIcon = {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.White)
                        },
                        testTag = "btn_overlay_audio"
                    )
                }

                // Fullscreen toggle
                TvButton(
                    text = if (isSinglePane && state.layoutMode != MultiviewLayoutMode.ONE_PANE) "Back to Grid" else "Fullscreen",
                    onClick = {
                        viewModel.toggleFullScreenActivePane()
                    },
                    style = TvButtonStyle.OUTLINE,
                    leadingIcon = {
                        Icon(
                            if (isSinglePane) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = null,
                            tint = Color.White
                        )
                    },
                    testTag = "btn_overlay_fullscreen"
                )
            }
        }
    }
}

// --- YouTube TV Multiview Builder Modal ---

@Composable
fun YouTubeTvMultiviewBuilderModal(
    allChannels: List<com.example.model.TabloChannel>,
    onDismiss: () -> Unit,
    onLaunchMultiview: (List<com.example.model.TabloChannel>) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("Sports & NFL") }
    var selectedChannels by remember {
        mutableStateOf(allChannels.take(4).toSet())
    }

    val categories = listOf("Sports & NFL", "News", "Shows & Movies", "Local Affiliates")

    val displayedChannels = remember(allChannels, selectedCategory) {
        when (selectedCategory) {
            "Sports & NFL" -> allChannels.filter { ch ->
                val text = (ch.network + " " + ch.callSign).lowercase()
                text.contains("cbs") || text.contains("fox") || text.contains("nbc") || text.contains("abc") || text.contains("sport")
            }.ifEmpty { allChannels }
            "News" -> allChannels.filter { ch ->
                val text = (ch.network + " " + ch.callSign).lowercase()
                text.contains("news") || text.contains("pbs") || text.contains("weather")
            }.ifEmpty { allChannels }
            "Shows & Movies" -> allChannels.filter { ch ->
                val text = (ch.network + " " + ch.callSign).lowercase()
                text.contains("movie") || text.contains("paramount") || text.contains("cw")
            }.ifEmpty { allChannels }
            "Local Affiliates" -> allChannels.filter { ch ->
                ch.callSign.isNotBlank()
            }.ifEmpty { allChannels }
            else -> allChannels
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        color = Color.Black.copy(alpha = 0.88f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF141720),
                border = BorderStroke(1.dp, Color(0xFF2C3244)),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 680.dp)
                    .testTag("multiview_builder_modal")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(28.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.GridView,
                                    contentDescription = null,
                                    tint = TvCyanPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "BUILD A MULTIVIEW",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Select up to 4 streams. Combine out-of-market NFL games with local CBS & FOX channels.",
                                color = TvTextSecondary,
                                fontSize = 13.sp
                            )
                        }

                        // Close button
                        TvButton(
                            text = "Close",
                            onClick = onDismiss,
                            style = TvButtonStyle.OUTLINE,
                            leadingIcon = {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Category Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(categories) { category ->
                            val isSelected = selectedCategory == category
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) Color.White else Color(0xFF1F2432))
                                    .border(1.dp, if (isSelected) Color.White else Color(0xFF2D3344), RoundedCornerShape(16.dp))
                                    .clickable { selectedCategory = category }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = category,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Channel List Grid
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(displayedChannels) { channel ->
                            val isSelected = selectedChannels.contains(channel)
                            val canSelect = isSelected || selectedChannels.size < 4

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) Color(0xFF252E42) else Color(0xFF171B24))
                                    .border(
                                        1.dp,
                                        if (isSelected) TvCyanPrimary else Color(0xFF252A38),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable(enabled = canSelect) {
                                        selectedChannels = if (isSelected) {
                                            selectedChannels - channel
                                        } else {
                                            if (selectedChannels.size < 4) selectedChannels + channel else selectedChannels
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) TvCyanPrimary else Color(0xFF2B3140)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = Color.Black,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = channel.channelNumberFormatted,
                                                color = TvAmberAccent,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = channel.network.ifBlank { channel.callSign },
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            text = getChannelAiringDesc(channel),
                                            color = TvTextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF1B2332))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "Pane ${selectedChannels.indexOf(channel) + 1}",
                                            color = TvCyanPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Footer with Selection Counter & Launch Multiview
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Selected: ${selectedChannels.size} / 4 Streams",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when (selectedChannels.size) {
                                    1 -> "Layout: Fullscreen Single Stream"
                                    2 -> "Layout: 2-Game Split Screen"
                                    3 -> "Layout: 3-Game Triple Layout"
                                    4 -> "Layout: 4-Game Quad Multiview"
                                    else -> "Select at least 1 stream to begin"
                                },
                                color = TvTextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        TvButton(
                            text = "Start Multiview",
                            onClick = {
                                if (selectedChannels.isNotEmpty()) {
                                    onLaunchMultiview(selectedChannels.toList())
                                }
                            },
                            style = TvButtonStyle.PRIMARY,
                            enabled = selectedChannels.isNotEmpty(),
                            leadingIcon = {
                                Icon(Icons.Default.GridView, contentDescription = null, tint = Color.Black)
                            },
                            testTag = "btn_start_multiview"
                        )
                    }
                }
            }
        }
    }
}

// --- YouTube TV Stats & Fantasy Overlay Modal ---

@Composable
fun YouTubeTvStatsOverlayModal(
    activePane: PaneState,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                onDismiss()
            },
        color = Color.Black.copy(alpha = 0.88f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF12151D),
                border = BorderStroke(1.dp, Color(0xFF282F40)),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 620.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .testTag("sports_stats_overlay_modal")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFF0000))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "NFL & SPORTS STATS OVERLAY",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "• Live Contextual Data",
                                    color = TvCyanPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Real-time stats, league scoreboard, and NFL Fantasy score tracking",
                                color = TvTextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        TvButton(
                            text = "Back to Game",
                            onClick = onDismiss,
                            style = TvButtonStyle.OUTLINE,
                            leadingIcon = {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3 Columns: Live Scores | Team Stats | NFL Fantasy Tracker
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Column 1: Live Scores
                        StatsCardColumn(
                            title = "LEAGUE SCOREBOARD",
                            modifier = Modifier.weight(1f)
                        ) {
                            ScoreItem("BAL", 24, "KC", 27, "4th Qtr • 2:15", isLive = true)
                            ScoreItem("SF", 21, "LAR", 14, "3rd Qtr • 8:40", isLive = true)
                            ScoreItem("PHI", 31, "DAL", 28, "Final", isLive = false)
                            ScoreItem("DET", 24, "GB", 20, "4th Qtr • 1:02", isLive = true)
                            ScoreItem("CIN", 17, "PIT", 21, "Final", isLive = false)
                        }

                        // Column 2: Team & Player Stats
                        StatsCardColumn(
                            title = "GAME STATS & LEADERS",
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text(
                                text = "Passing Leaders",
                                color = TvAmberAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            StatRow("P. Mahomes (KC)", "23/32, 291 YDS, 2 TD")
                            StatRow("L. Jackson (BAL)", "19/27, 248 YDS, 1 TD")

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Rushing Leaders",
                                color = TvAmberAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            StatRow("I. Pacheco (KC)", "16 CAR, 84 YDS, 1 TD")
                            StatRow("D. Henry (BAL)", "14 CAR, 78 YDS, 1 TD")

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Receiving Leaders",
                                color = TvAmberAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            StatRow("T. Kelce (KC)", "8 REC, 102 YDS, 1 TD")
                            StatRow("Z. Flowers (BAL)", "6 REC, 81 YDS")
                        }

                        // Column 3: NFL Fantasy Tracker
                        StatsCardColumn(
                            title = "NFL FANTASY TRACKER",
                            modifier = Modifier.weight(1.1f)
                        ) {
                            FantasyPlayerRow("Patrick Mahomes", "QB • KC", "21.6 PTS", "Proj: 20.4", true)
                            FantasyPlayerRow("Travis Kelce", "TE • KC", "18.2 PTS", "Proj: 14.8", true)
                            FantasyPlayerRow("Lamar Jackson", "QB • BAL", "19.3 PTS", "Proj: 21.0", true)
                            FantasyPlayerRow("Derrick Henry", "RB • BAL", "16.4 PTS", "Proj: 15.2", true)
                            FantasyPlayerRow("Harrison Butker", "K • KC", "9.0 PTS", "Proj: 7.8", false)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Tip: Press DOWN on your remote while watching any game to access this overlay.",
                        color = TvTextTertiary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsCardColumn(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF181C26))
            .border(1.dp, Color(0xFF262C3D), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun ScoreItem(
    team1: String, score1: Int,
    team2: String, score2: Int,
    status: String,
    isLive: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF1F2432))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "$team1 $score1  -  $team2 $score2",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = status,
                color = if (isLive) Color(0xFFFF4444) else TvTextSecondary,
                fontSize = 10.sp,
                fontWeight = if (isLive) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun StatRow(player: String, stats: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(player, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Text(stats, color = TvTextSecondary, fontSize = 11.sp)
    }
}

@Composable
private fun FantasyPlayerRow(name: String, pos: String, pts: String, proj: String, isBeatingProj: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF1F2432))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(pos, color = TvTextSecondary, fontSize = 9.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(pts, color = TvCyanPrimary, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Text(proj, color = if (isBeatingProj) Color(0xFF00E676) else TvTextTertiary, fontSize = 9.sp)
        }
    }
}

private fun getChannelAiringDesc(channel: com.example.model.TabloChannel): String {
    val net = channel.network.lowercase()
    return when {
        net.contains("cbs") -> "NFL Sunday Ticket: Live Broadcast"
        net.contains("fox") -> "NFL on FOX: Local Affiliate Game"
        net.contains("nbc") -> "Sunday Night Football Live"
        net.contains("abc") -> "College Football Live"
        net.contains("news") -> "Live 24/7 News Desk"
        else -> "Live Over-The-Air Broadcast"
    }
}
