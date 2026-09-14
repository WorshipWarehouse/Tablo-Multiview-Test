package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MultiviewLayoutMode
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

@Composable
fun HomeScreen(
    viewModel: TabloAppViewModel,
    modifier: Modifier = Modifier
) {
    val device by viewModel.deviceRepository.currentDevice.collectAsState()
    val connectionState by viewModel.deviceRepository.connectionState.collectAsState()
    val channels by viewModel.channelRepository.channels.collectAsState()

    val antennaChannels = remember(channels) {
        channels.filter { it.major < 100 }
    }
    val streamingChannels = remember(channels) {
        channels.filter { it.major >= 100 }
    }
    val suggestedChannels = remember(channels) {
        channels.take(6)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CompetitorAppBg)
    ) {
        // macOS / iPad Style Top Navigation Bar
        TvTopBar(
            title = "Home",
            device = device,
            connectionState = connectionState,
            activeScreen = AppScreen.HOME,
            onNavigate = { screen -> viewModel.navigateTo(screen) },
            onOpenSettings = { viewModel.navigateTo(AppScreen.SETTINGS) }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Page Header
            Text(
                text = "Home",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TvTextPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 1. "Watch Now" Hero Card (Screenshots 1 & 4)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CompetitorCardBg)
                    .border(1.dp, CompetitorCardBorder, RoundedCornerShape(14.dp))
                    .padding(vertical = 28.dp, horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Watch Now",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Choose a channel or start a multiview layout",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = CompetitorTabInactive
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = { viewModel.navigateTo(AppScreen.CHANNEL_GUIDE) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CompetitorPurple,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(22.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
                        modifier = Modifier.testTag("btn_browse_all_channels")
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Browse All Channels",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 2. "Continue Watching" Section (Screenshot 1)
            Text(
                text = "Continue Watching",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CompetitorCardBg)
                    .border(1.dp, CompetitorCardBorder, RoundedCornerShape(12.dp))
                    .clickable {
                        // Quick resume quad or dual view with available channels
                        viewModel.startMultiviewWithMode(MultiviewLayoutMode.QUAD_GRID)
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .testTag("card_continue_watching")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Multiview dual-rect icon
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF16181E))
                                .border(1.dp, CompetitorCardBorder, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 11.dp, height = 18.dp)
                                        .border(1.5.dp, CompetitorLayoutBlue, RoundedCornerShape(2.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(width = 11.dp, height = 18.dp)
                                        .border(1.5.dp, CompetitorLayoutBlue, RoundedCornerShape(2.dp))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "CBS • FOX",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Grid • 18h ago",
                                fontSize = 12.sp,
                                color = CompetitorTabInactive
                            )
                        }
                    }

                    // Green Replay Icon Button (Screenshot 1)
                    IconButton(
                        onClick = {
                            viewModel.startMultiviewWithMode(MultiviewLayoutMode.QUAD_GRID)
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF14241B))
                            .border(1.dp, Color(0xFF1D472D), CircleShape)
                            .testTag("btn_resume_multiview")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = "Resume Watching",
                            tint = CompetitorGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 3. "Suggested Channels" Section (Screenshot 1)
            ChannelSectionRow(
                title = "Suggested Channels",
                channels = suggestedChannels,
                onChannelClick = { channel ->
                    viewModel.playChannelInMultiview(0, channel)
                }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 4. "Antenna" Section (Screenshot 1)
            ChannelSectionRow(
                title = "Antenna",
                channels = antennaChannels,
                onChannelClick = { channel ->
                    viewModel.playChannelInMultiview(0, channel)
                }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 5. "Streaming TV" Section (Screenshot 1)
            ChannelSectionRow(
                title = "Streaming TV",
                channels = streamingChannels,
                onChannelClick = { channel ->
                    viewModel.playChannelInMultiview(0, channel)
                }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 6. "Start New Layout" Section (Screenshot 4)
            Text(
                text = "Start New Layout",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LayoutOptionItem(
                    title = "Single View",
                    subtitle = "1 channel",
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(width = 24.dp, height = 18.dp)
                                .border(1.5.dp, CompetitorLayoutBlue, RoundedCornerShape(2.dp))
                        )
                    },
                    testTag = "layout_single",
                    onClick = {
                        viewModel.startMultiviewWithMode(MultiviewLayoutMode.SINGLE)
                    }
                )

                LayoutOptionItem(
                    title = "Split View (2)",
                    subtitle = "2 channels",
                    icon = {
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(width = 11.dp, height = 18.dp)
                                    .border(1.5.dp, CompetitorLayoutBlue, RoundedCornerShape(2.dp))
                            )
                            Box(
                                modifier = Modifier
                                    .size(width = 11.dp, height = 18.dp)
                                    .border(1.5.dp, CompetitorLayoutBlue, RoundedCornerShape(2.dp))
                            )
                        }
                    },
                    testTag = "layout_split_2",
                    onClick = {
                        viewModel.startMultiviewWithMode(MultiviewLayoutMode.SIDE_BY_SIDE)
                    }
                )

                LayoutOptionItem(
                    title = "Triple View (3)",
                    subtitle = "3 channels",
                    icon = {
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(width = 13.dp, height = 18.dp)
                                    .border(1.5.dp, CompetitorLayoutBlue, RoundedCornerShape(2.dp))
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 10.dp, height = 8.dp)
                                        .border(1.5.dp, CompetitorLayoutBlue, RoundedCornerShape(2.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(width = 10.dp, height = 8.dp)
                                        .border(1.5.dp, CompetitorLayoutBlue, RoundedCornerShape(2.dp))
                                )
                            }
                        }
                    },
                    testTag = "layout_triple_3",
                    onClick = {
                        viewModel.startMultiviewWithMode(MultiviewLayoutMode.FOCUS_PRIMARY_BOTTOM_STRIP)
                    }
                )

                LayoutOptionItem(
                    title = "Quad View (4)",
                    subtitle = "4 channels",
                    icon = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 11.dp, height = 8.dp)
                                        .border(1.5.dp, CompetitorLayoutBlue, RoundedCornerShape(2.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(width = 11.dp, height = 8.dp)
                                        .border(1.5.dp, CompetitorLayoutBlue, RoundedCornerShape(2.dp))
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 11.dp, height = 8.dp)
                                        .border(1.5.dp, CompetitorLayoutBlue, RoundedCornerShape(2.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(width = 11.dp, height = 8.dp)
                                        .border(1.5.dp, CompetitorLayoutBlue, RoundedCornerShape(2.dp))
                                )
                            }
                        }
                    },
                    testTag = "layout_quad_4",
                    onClick = {
                        viewModel.startMultiviewWithMode(MultiviewLayoutMode.QUAD_GRID)
                    }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun ChannelSectionRow(
    title: String,
    channels: List<TabloChannel>,
    onChannelClick: (TabloChannel) -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(channels, key = { it.id }) { channel ->
                CompetitorChannelCard(
                    channel = channel,
                    onClick = { onChannelClick(channel) }
                )
            }
        }
    }
}

@Composable
private fun CompetitorChannelCard(
    channel: TabloChannel,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Column(
        modifier = Modifier
            .width(136.dp)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .testTag("ch_card_${channel.major}_${channel.minor}")
    ) {
        // Thumbnail / Callout Card Box (Screenshot 1)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(CompetitorCardBg)
                .border(
                    width = if (isFocused) 2.dp else 1.dp,
                    color = if (isFocused) CompetitorPurple else CompetitorCardBorder,
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = channel.network.ifBlank { channel.callSign },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Channel Number & CallSign Label
        Text(
            text = "${channel.major}.${channel.minor} ${channel.network}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = CompetitorTabInactive,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LayoutOptionItem(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    testTag: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CompetitorCardBg)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) CompetitorPurple else CompetitorCardBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Icon Container
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF16181E))
                        .border(1.dp, CompetitorCardBorder, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = CompetitorTabInactive
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = CompetitorTabInactive,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
