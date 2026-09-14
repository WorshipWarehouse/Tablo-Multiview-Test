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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TabloChannel
import com.example.ui.components.TvTopBar
import com.example.ui.theme.CompetitorAppBg
import com.example.ui.theme.CompetitorCardBg
import com.example.ui.theme.CompetitorCardBorder
import com.example.ui.theme.CompetitorPurple
import com.example.ui.theme.CompetitorTabInactive
import com.example.ui.theme.TvAmberAccent
import com.example.ui.theme.TvTextPrimary
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
    val connectionState by viewModel.deviceRepository.connectionState.collectAsState()
    val airingsMap by viewModel.channelRepository.airingsMap.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var favoriteChannelIds by remember { mutableStateOf(setOf<String>()) }

    val filterPills = listOf("All", "Favorites", "Antenna", "Streaming TV")

    val filteredChannels = remember(channels, selectedFilter, searchQuery, favoriteChannelIds) {
        channels.filter { channel ->
            val matchesFilter = when (selectedFilter) {
                "Favorites" -> favoriteChannelIds.contains(channel.id)
                "Antenna" -> channel.major < 100
                "Streaming TV" -> channel.major >= 100
                else -> true
            }

            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                val q = searchQuery.lowercase().trim()
                channel.network.lowercase().contains(q) ||
                channel.callSign.lowercase().contains(q) ||
                "${channel.major}.${channel.minor}".contains(q) ||
                (airingsMap[channel.id]?.title?.lowercase()?.contains(q) == true)
            }

            matchesFilter && matchesSearch
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CompetitorAppBg)
    ) {
        // Top Navigation Bar
        TvTopBar(
            title = "Guide",
            device = device,
            connectionState = connectionState,
            activeScreen = AppScreen.CHANNEL_GUIDE,
            onNavigate = { screen -> viewModel.navigateTo(screen) },
            onOpenSettings = { viewModel.navigateTo(AppScreen.SETTINGS) }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 14.dp)
        ) {
            // Screen Title: "All Channels" (Screenshot 2)
            Text(
                text = "All Channels",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TvTextPrimary,
                modifier = Modifier.padding(bottom = 14.dp)
            )

            // Search Bar: "Search channels and programs..." (Screenshot 2)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CompetitorCardBg)
                    .border(1.dp, CompetitorCardBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = CompetitorTabInactive,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search channels and programs...",
                            color = CompetitorTabInactive,
                            fontSize = 13.sp
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                        cursorBrush = SolidColor(CompetitorPurple),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_search_channels")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Filter Pills: All | Favorites | Antenna | Streaming TV (Screenshot 2)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filterPills) { pill ->
                    val isSelected = selectedFilter == pill
                    val pillInteraction = remember { MutableInteractionSource() }
                    val isFocused by pillInteraction.collectIsFocusedAsState()

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) CompetitorPurple
                                else if (isFocused) Color(0xFF2A2D37)
                                else CompetitorCardBg
                            )
                            .border(
                                1.dp,
                                if (isSelected) CompetitorPurple else CompetitorCardBorder,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable(interactionSource = pillInteraction, indication = null) {
                                selectedFilter = pill
                            }
                            .focusable(interactionSource = pillInteraction)
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                            .testTag("filter_pill_$pill"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = pill,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else CompetitorTabInactive
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Channels Guide List
            if (isLoading && channels.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = CompetitorPurple)
                }
            } else if (filteredChannels.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No channels match the selected filter.",
                        color = CompetitorTabInactive,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredChannels, key = { it.id }) { channel ->
                        val isFavorite = favoriteChannelIds.contains(channel.id)
                        val airing = airingsMap[channel.id]
                        val programTitle = airing?.title ?: channel.liveEventTitle ?: "Live Broadcast"
                        val programTime = channel.scoreBug ?: "Live Now"

                        CompetitorGuideRow(
                            channel = channel,
                            programTitle = programTitle,
                            programTime = programTime,
                            isFavorite = isFavorite,
                            onToggleFavorite = {
                                favoriteChannelIds = if (isFavorite) {
                                    favoriteChannelIds - channel.id
                                } else {
                                    favoriteChannelIds + channel.id
                                }
                            },
                            onWatch = {
                                viewModel.playChannelInMultiview(0, channel)
                            },
                            onAddToMultiview = {
                                // Find first idle pane or open multiview
                                viewModel.playChannelInMultiview(1, channel)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompetitorGuideRow(
    channel: TabloChannel,
    programTitle: String,
    programTime: String,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onWatch: () -> Unit,
    onAddToMultiview: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CompetitorCardBg)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) CompetitorPurple else CompetitorCardBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onWatch)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("guide_channel_${channel.major}_${channel.minor}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Favorite Star + Channel Badge & Identity (Screenshot 2)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.width(220.dp)
            ) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(28.dp).testTag("fav_btn_${channel.id}")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) TvAmberAccent else CompetitorTabInactive,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = "${channel.major}.${channel.minor} ${channel.network}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = channel.callSign.ifBlank { if (channel.major < 100) "OTA Antenna" else "FAST Streaming" },
                        fontSize = 11.sp,
                        color = CompetitorTabInactive,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Center: Airing Program Title and Time (Screenshot 2)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = programTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = programTime,
                    fontSize = 12.sp,
                    color = CompetitorTabInactive,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Right: Watch action pills
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(CompetitorPurple)
                        .clickable(onClick = onWatch)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("btn_watch_${channel.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Watch",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF272A35))
                        .border(1.dp, CompetitorCardBorder, RoundedCornerShape(14.dp))
                        .clickable(onClick = onAddToMultiview)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("btn_add_multiview_${channel.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+ Multi",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
