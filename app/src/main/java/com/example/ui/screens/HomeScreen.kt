package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                    focusedContainerColor = TvSurfaceElevated,
                    unfocusedContainerColor = TvSurface,
                    testTag = "btn_watch_multiview"
                ) { isFocused ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        TvSurfaceElevated,
                                        if (isFocused) TvCyanPrimary.copy(alpha = 0.2f) else TvSurface
                                    )
                                )
                            )
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
                                            .background(TvCyanPrimary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = "Watch",
                                            tint = TvBackground,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = "WATCH MULTIVIEW",
                                            color = TvTextPrimary,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            text = "Four simultaneous live TV video panes",
                                            color = TvCyanPrimary,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                // 4-Pane Mini Grid Graphic
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Box(modifier = Modifier.size(16.dp).background(TvCyanPrimary, RoundedCornerShape(2.dp)))
                                        Box(modifier = Modifier.size(16.dp).background(TvSurfaceVariant, RoundedCornerShape(2.dp)))
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Box(modifier = Modifier.size(16.dp).background(TvSurfaceVariant, RoundedCornerShape(2.dp)))
                                        Box(modifier = Modifier.size(16.dp).background(TvSurfaceVariant, RoundedCornerShape(2.dp)))
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
                                    color = if (isFocused) TvCyanPrimary else TvTextTertiary,
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
                                .background(TvSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.List,
                                contentDescription = "Guide",
                                tint = TvCyanPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "CHANNEL GUIDE",
                                color = TvTextPrimary,
                                fontSize = 16.sp,
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
                            color = if (isFocused) TvCyanPrimary else TvTextTertiary,
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
                                .background(TvSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.GridView,
                                contentDescription = "Layouts",
                                tint = TvAmberAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "SAVED PRESETS",
                                color = TvTextPrimary,
                                fontSize = 16.sp,
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
                            color = if (isFocused) TvAmberAccent else TvTextTertiary,
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
                                .background(TvSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = TvTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "SETTINGS",
                                color = TvTextPrimary,
                                fontSize = 16.sp,
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
                            color = if (isFocused) TvCyanPrimary else TvTextTertiary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Channels Strip Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AVAILABLE CHANNELS",
                    color = TvTextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${channels.size} Detected",
                    color = TvCyanPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Channels Horizontal Rail
            if (channels.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(TvSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No channels loaded yet. Press Channel Guide or Settings to refresh.",
                        color = TvTextTertiary,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(channels) { channel ->
                        TvFocusableCard(
                            onClick = {
                                viewModel.launchMultiview(
                                    channels = listOf(channel),
                                    initialActivePane = 0
                                )
                            },
                            modifier = Modifier
                                .width(160.dp)
                                .height(90.dp),
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
                                        color = if (isFocused) TvCyanPrimary else TvAmberAccent,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black
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
                                    color = TvTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
