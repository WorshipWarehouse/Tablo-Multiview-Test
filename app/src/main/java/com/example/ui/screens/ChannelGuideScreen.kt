package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MultiviewLayoutMode
import com.example.model.TabloChannel
import com.example.ui.components.TvButton
import com.example.ui.components.TvButtonStyle
import com.example.ui.components.TvFocusableCard
import com.example.ui.theme.TvAmberAccent
import com.example.ui.theme.TvBackground
import com.example.ui.theme.TvCyanPrimary
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvSurfaceElevated
import com.example.ui.theme.TvSurfaceVariant
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary
import com.example.ui.theme.TvTextTertiary
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.TabloAppViewModel

@Composable
fun ChannelGuideScreen(
    viewModel: TabloAppViewModel,
    modifier: Modifier = Modifier
) {
    val channels by viewModel.channelRepository.channels.collectAsState()
    val isLoading by viewModel.channelRepository.isLoading.collectAsState()
    val device by viewModel.deviceRepository.currentDevice.collectAsState()
    val airingsMap by viewModel.channelRepository.airingsMap.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TvBackground)
            .padding(28.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TvButton(
                    text = "Back",
                    onClick = { viewModel.navigateTo(AppScreen.HOME) },
                    style = TvButtonStyle.OUTLINE,
                    leadingIcon = {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    },
                    testTag = "btn_guide_back"
                )
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    Text(
                        text = "LIVE CHANNEL GUIDE",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "${channels.size} broadcast channels from ${device?.name ?: "Tablo"}",
                        color = TvTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Row {
                TvButton(
                    text = if (isLoading) "Refreshing..." else "Refresh Guide",
                    onClick = { viewModel.refreshGuideChannels() },
                    style = TvButtonStyle.OUTLINE,
                    enabled = !isLoading,
                    leadingIcon = {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                        }
                    },
                    testTag = "btn_refresh_guide"
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (channels.isEmpty() && isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = TvCyanPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading channels from Tablo...", color = TvTextSecondary)
                }
            }
        } else if (channels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(TvSurface, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Tv, contentDescription = null, tint = TvTextTertiary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No channels found on this Tablo", color = TvTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Run a channel scan in your Tablo setup or tap Refresh Guide.", color = TvTextSecondary, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(channels) { channel ->
                    ChannelGuideRowItem(
                        channel = channel,
                        currentAiringTitle = airingsMap[channel.id]?.title,
                        onWatchFullscreen = {
                            viewModel.launchMultiview(
                                mode = MultiviewLayoutMode.ONE_PANE,
                                channels = listOf(channel),
                                initialActivePane = 0
                            )
                        },
                        onWatchMultiview = {
                            viewModel.launchMultiview(
                                mode = MultiviewLayoutMode.TWO_PANE,
                                channels = listOf(channel),
                                initialActivePane = 0
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ChannelGuideRowItem(
    channel: TabloChannel,
    currentAiringTitle: String?,
    onWatchFullscreen: () -> Unit,
    onWatchMultiview: () -> Unit
) {
    TvFocusableCard(
        onClick = onWatchFullscreen,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        focusedContainerColor = Color(0xFF222226),
        unfocusedContainerColor = Color(0xFF141416),
        focusedBorderColor = Color.White,
        unfocusedBorderColor = Color(0xFF2C2C2E),
        testTag = "guide_item_${channel.id}"
    ) { isFocused ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Number & Network & Show
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (isFocused) Color.White else Color(0xFF222226)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = channel.channelNumberFormatted,
                        color = if (isFocused) Color.Black else Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = channel.network.ifBlank { channel.callSign },
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (channel.callSign.isNotBlank() && channel.callSign != channel.network) {
                            Text(
                                text = "  •  ${channel.callSign}",
                                color = TvTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        if (!channel.resolution.isNullOrBlank()) {
                            Text(
                                text = "  •  ${channel.resolution}",
                                color = Color(0xFF8E8E93),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }

                    Text(
                        text = currentAiringTitle ?: "Live Broadcast",
                        color = TvTextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }

            // Right: Actions
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TvButton(
                    text = "Watch",
                    onClick = onWatchFullscreen,
                    style = if (isFocused) TvButtonStyle.PRIMARY else TvButtonStyle.SECONDARY,
                    modifier = Modifier.height(40.dp),
                    leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = if (isFocused) Color.Black else Color.White) }
                )

                TvButton(
                    text = "+ Multiview",
                    onClick = onWatchMultiview,
                    style = TvButtonStyle.OUTLINE,
                    modifier = Modifier.height(40.dp)
                )
            }
        }
    }
}
