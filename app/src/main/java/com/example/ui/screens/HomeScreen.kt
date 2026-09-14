package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MultiviewLayoutMode
import com.example.model.TabloChannel
import com.example.model.TabloConnectionState
import com.example.ui.components.TvButton
import com.example.ui.components.TvButtonStyle
import com.example.ui.components.TvFocusableCard
import com.example.ui.components.TvTopBar
import com.example.ui.theme.TvAmberAccent
import com.example.ui.theme.TvBackground
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
fun HomeScreen(
    viewModel: TabloAppViewModel,
    modifier: Modifier = Modifier
) {
    val device by viewModel.deviceRepository.currentDevice.collectAsState()
    val connectionState by viewModel.deviceRepository.connectionState.collectAsState()
    val errorMessage by viewModel.deviceRepository.errorMessage.collectAsState()
    val channels by viewModel.channelRepository.channels.collectAsState()
    val savedLayouts by viewModel.savedLayouts.collectAsState()
    var selectedCategory by remember { mutableStateOf("All Channels") }

    val filteredChannels = remember(channels, selectedCategory) {
        when (selectedCategory) {
            "Sports & Primetime" -> channels.filter { ch ->
                val text = (ch.network + " " + ch.callSign).lowercase()
                text.contains("cbs") || text.contains("nbc") || text.contains("fox") ||
                text.contains("abc") || text.contains("sport") || text.contains("espn")
            }.ifEmpty { channels }
            "News & Info" -> channels.filter { ch ->
                val text = (ch.network + " " + ch.callSign).lowercase()
                text.contains("news") || text.contains("pbs") || text.contains("weather")
            }.ifEmpty { channels }
            else -> channels
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TvBackground)
    ) {
        TvTopBar(
            title = "Home",
            device = device,
            connectionState = connectionState
        )

        // Error Banner if disconnected
        if (connectionState == TabloConnectionState.ERROR || errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(TvError.copy(alpha = 0.2f))
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = TvError,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = errorMessage ?: "Cannot connect to Tablo. Verify your device is on the same local network.",
                            color = TvTextPrimary,
                            fontSize = 14.sp
                        )
                    }
                    Row {
                        TvButton(
                            text = "Retry",
                            onClick = { viewModel.retryConnection() },
                            style = TvButtonStyle.AMBER,
                            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = TvBackground) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TvButton(
                            text = "Find Tablos",
                            onClick = { viewModel.navigateTo(AppScreen.REGISTRATION) },
                            style = TvButtonStyle.OUTLINE
                        )
                    }
                }
            }
        }

        // Main TV Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Primary Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Primary Hero Card: Watch Multiview
                TvFocusableCard(
                    onClick = { viewModel.startMultiviewWithLastSession() },
                    modifier = Modifier
                        .weight(1.8f)
                        .height(180.dp),
                    focusedContainerColor = Color(0xFF222226),
                    unfocusedContainerColor = Color(0xFF141416),
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color(0xFF2C2C2E),
                    testTag = "btn_watch_multiview"
                ) { isFocused ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = "Watch",
                                            tint = Color.Black,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = "WATCH MULTIVIEW",
                                            color = Color.White,
                                            fontSize = 19.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                        Text(
                                            text = "Simultaneous live broadcast video panes",
                                            color = TvTextSecondary,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                // 4-Pane Mini Grid Graphic
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Box(modifier = Modifier.size(16.dp).background(if (isFocused) Color.White else Color(0xFF636366), RoundedCornerShape(2.dp)))
                                        Box(modifier = Modifier.size(16.dp).background(Color(0xFF2C2C2E), RoundedCornerShape(2.dp)))
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Box(modifier = Modifier.size(16.dp).background(Color(0xFF2C2C2E), RoundedCornerShape(2.dp)))
                                        Box(modifier = Modifier.size(16.dp).background(Color(0xFF2C2C2E), RoundedCornerShape(2.dp)))
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (channels.isNotEmpty()) "${channels.size} live channels ready on Tablo" else "Ready to stream",
                                    color = TvTextSecondary,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Press SELECT to Launch  ▶",
                                    color = if (isFocused) Color.White else Color(0xFF8E8E93),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Channel Guide Card
                TvFocusableCard(
                    onClick = { viewModel.navigateTo(AppScreen.CHANNEL_GUIDE) },
                    modifier = Modifier
                        .weight(1f)
                        .height(180.dp),
                    focusedContainerColor = Color(0xFF222226),
                    unfocusedContainerColor = Color(0xFF141416),
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color(0xFF2C2C2E),
                    testTag = "btn_channel_guide"
                ) { isFocused ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF222226)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.List,
                                contentDescription = "Guide",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "CHANNEL GUIDE",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${channels.size} broadcast channels",
                                color = TvTextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Text(
                            text = "Browse EPG  ›",
                            color = if (isFocused) Color.White else Color(0xFF8E8E93),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Saved Layouts Card
                TvFocusableCard(
                    onClick = { viewModel.navigateTo(AppScreen.SAVED_LAYOUTS) },
                    modifier = Modifier
                        .weight(1f)
                        .height(180.dp),
                    focusedContainerColor = Color(0xFF222226),
                    unfocusedContainerColor = Color(0xFF141416),
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color(0xFF2C2C2E),
                    testTag = "btn_saved_layouts"
                ) { isFocused ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF222226)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.GridView,
                                contentDescription = "Layouts",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "SAVED PRESETS",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Sports, News, Quad grids",
                                color = TvTextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Text(
                            text = "Manage Presets  ›",
                            color = if (isFocused) Color.White else Color(0xFF8E8E93),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Settings Card
                TvFocusableCard(
                    onClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
                    modifier = Modifier
                        .weight(0.9f)
                        .height(180.dp),
                    focusedContainerColor = Color(0xFF222226),
                    unfocusedContainerColor = Color(0xFF141416),
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color(0xFF2C2C2E),
                    testTag = "btn_settings"
                ) { isFocused ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF222226)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "SETTINGS",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Network & Tablo",
                                color = TvTextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Text(
                            text = "Diagnostics  ›",
                            color = if (isFocused) Color.White else Color(0xFF8E8E93),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Choose Multiview Screens Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Dashboard,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CHOOSE MULTIVIEW SCREENS",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
                Text(
                    text = "Select 1, 2, 3, or 4 simultaneous live screens",
                    color = TvTextSecondary,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 1, 2, 3, 4 Screens Cards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1 Screen Card
                MultiviewLayoutSelectorCard(
                    title = "1 Screen",
                    subtitle = "Fullscreen Live TV",
                    badge = "1 Tuner",
                    mode = MultiviewLayoutMode.ONE_PANE,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.startMultiviewWithMode(MultiviewLayoutMode.ONE_PANE) }
                )

                // 2 Screens Card
                MultiviewLayoutSelectorCard(
                    title = "2 Screens",
                    subtitle = "Dual Split View",
                    badge = "2 Tuners",
                    mode = MultiviewLayoutMode.TWO_PANE,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.startMultiviewWithMode(MultiviewLayoutMode.TWO_PANE) }
                )

                // 3 Screens Card
                MultiviewLayoutSelectorCard(
                    title = "3 Screens",
                    subtitle = "Primary + 2 Stacked",
                    badge = "3 Tuners",
                    mode = MultiviewLayoutMode.THREE_PANE,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.startMultiviewWithMode(MultiviewLayoutMode.THREE_PANE) }
                )

                // 4 Screens Card
                MultiviewLayoutSelectorCard(
                    title = "4 Screens",
                    subtitle = "Quad 2x2 Grid",
                    badge = "4 Tuners",
                    mode = MultiviewLayoutMode.FOUR_PANE,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.startMultiviewWithMode(MultiviewLayoutMode.FOUR_PANE) }
                )
            }

            // Saved Presets Rail (if any exist)
            if (savedLayouts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SAVED MULTIVIEW PRESETS",
                        color = TvTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "${savedLayouts.size} Presets",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(savedLayouts) { layout ->
                        TvFocusableCard(
                            onClick = { viewModel.launchSavedLayout(layout) },
                            modifier = Modifier
                                .width(200.dp)
                                .height(94.dp),
                            focusedContainerColor = Color(0xFF222228),
                            unfocusedContainerColor = Color(0xFF141418),
                            focusedBorderColor = TvCyanPrimary,
                            unfocusedBorderColor = Color(0xFF2C2C32),
                            testTag = "preset_card_${layout.id}"
                        ) { isFocused ->
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = layout.name,
                                        color = if (isFocused) TvCyanPrimary else Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF2C2C32))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${layout.mode.paneCount} Screens",
                                            color = Color(0xFFA1A1AA),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Text(
                                    text = layout.channelLabels.joinToString(" • ").ifBlank { "Multi-stream Grid" },
                                    color = TvTextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = "Launch Preset  ▶",
                                    color = if (isFocused) Color.White else Color(0xFF8E8E93),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Channels Strip Header with Category Filters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AVAILABLE CHANNELS",
                    color = TvTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                // Category Filter Pills
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (cat in listOf("All Channels", "Sports & Primetime", "News & Info")) {
                        val isCatSelected = selectedCategory == cat
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCatSelected) TvCyanPrimary else Color(0xFF222226),
                            modifier = Modifier
                                .clickable { selectedCategory = cat }
                        ) {
                            Text(
                                text = cat,
                                color = if (isCatSelected) Color.Black else Color(0xFFD1D1D6),
                                fontSize = 11.sp,
                                fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Channels Horizontal Rail
            if (filteredChannels.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF141416))
                        .border(1.dp, Color(0xFF2C2C2E), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No channels in this category.",
                        color = TvTextSecondary,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(filteredChannels) { channel ->
                        TvFocusableCard(
                            onClick = {
                                viewModel.launchMultiview(
                                    mode = MultiviewLayoutMode.ONE_PANE,
                                    channels = listOf(channel),
                                    initialActivePane = 0
                                )
                            },
                            modifier = Modifier
                                .width(160.dp)
                                .height(90.dp),
                            focusedContainerColor = Color(0xFF222226),
                            unfocusedContainerColor = Color(0xFF141416),
                            focusedBorderColor = TvCyanPrimary,
                            unfocusedBorderColor = Color(0xFF2C2C2E),
                            testTag = "channel_card_${channel.id}"
                        ) { isFocused ->
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = channel.channelNumberFormatted,
                                        color = if (isFocused) Color.White else Color(0xFFD1D1D6),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (!channel.resolution.isNullOrBlank()) {
                                        Text(
                                            text = channel.resolution,
                                            color = TvTextTertiary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Text(
                                    text = channel.network.ifBlank { channel.callSign },
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
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

@Composable
private fun MultiviewLayoutSelectorCard(
    title: String,
    subtitle: String,
    badge: String,
    mode: MultiviewLayoutMode,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    TvFocusableCard(
        onClick = onClick,
        modifier = modifier.height(130.dp),
        focusedContainerColor = Color(0xFF222226),
        unfocusedContainerColor = Color(0xFF141416),
        focusedBorderColor = Color.White,
        unfocusedBorderColor = Color(0xFF2C2C2E),
        testTag = "btn_layout_${mode.name.lowercase()}"
    ) { isFocused ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Layout mini diagram preview
                LayoutPreviewThumbnail(mode = mode, isFocused = isFocused)

                // Tuner count badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isFocused) Color.White.copy(alpha = 0.2f) else Color(0xFF2C2C2E))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        color = if (isFocused) Color.White else Color(0xFFA1A1AA),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = TvTextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun LayoutPreviewThumbnail(
    mode: MultiviewLayoutMode,
    isFocused: Boolean,
    modifier: Modifier = Modifier
) {
    val activeColor = if (isFocused) Color.White else Color(0xFF8E8E93)

    Box(
        modifier = modifier
            .size(width = 44.dp, height = 28.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF0A0A0C))
            .border(1.dp, if (isFocused) Color.White else Color(0xFF38383A), RoundedCornerShape(4.dp))
            .padding(3.dp)
    ) {
        when (mode) {
            MultiviewLayoutMode.ONE_PANE -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(activeColor, RoundedCornerShape(2.dp))
                )
            }
            MultiviewLayoutMode.TWO_PANE -> {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(activeColor, RoundedCornerShape(2.dp)))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(activeColor, RoundedCornerShape(2.dp)))
                }
            }
            MultiviewLayoutMode.THREE_PANE -> {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(modifier = Modifier.weight(1.5f).fillMaxHeight().background(activeColor, RoundedCornerShape(2.dp)))
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(activeColor, RoundedCornerShape(2.dp)))
                        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(activeColor, RoundedCornerShape(2.dp)))
                    }
                }
            }
            MultiviewLayoutMode.FOUR_PANE -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(activeColor, RoundedCornerShape(2.dp)))
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(activeColor, RoundedCornerShape(2.dp)))
                    }
                    Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(activeColor, RoundedCornerShape(2.dp)))
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(activeColor, RoundedCornerShape(2.dp)))
                    }
                }
            }
        }
    }
}
