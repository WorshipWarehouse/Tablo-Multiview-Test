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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VolumeMute
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TabloChannel
import com.example.ui.components.TvFocusableCard
import com.example.ui.components.TvTopBar
import com.example.ui.theme.TvAmberAccent
import com.example.ui.theme.TvBackground
import com.example.ui.theme.TvBorderNormal
import com.example.ui.theme.TvCyanPrimary
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvSurfaceElevated
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
    val connectionState by viewModel.deviceRepository.connectionState.collectAsState()
    val airingsMap by viewModel.channelRepository.airingsMap.collectAsState()

    var selectedCategory by remember { mutableStateOf("All") }
    var focusedChannel by remember { mutableStateOf<TabloChannel?>(null) }
    var favoriteChannelIds by remember { mutableStateOf(setOf<String>()) }

    val categories = listOf("All", "Sports", "News", "Movies", "Kids")

    val filteredChannels = remember(channels, selectedCategory, favoriteChannelIds) {
        val baseList = when (selectedCategory) {
            "Sports" -> channels.filter { ch ->
                val text = (ch.network + " " + ch.callSign).lowercase()
                text.contains("cbs") || text.contains("nbc") || text.contains("fox") ||
                text.contains("abc") || text.contains("sport") || text.contains("espn")
            }.ifEmpty { channels }
            "News" -> channels.filter { ch ->
                val text = (ch.network + " " + ch.callSign).lowercase()
                text.contains("news") || text.contains("pbs") || text.contains("weather")
            }.ifEmpty { channels }
            "Movies" -> channels.filter { ch ->
                val text = (ch.network + " " + ch.callSign).lowercase()
                text.contains("movie") || text.contains("cinema") || text.contains("paramount")
            }.ifEmpty { channels }
            "Kids" -> channels.filter { ch ->
                val text = (ch.network + " " + ch.callSign).lowercase()
                text.contains("kid") || text.contains("cartoon") || text.contains("pbs")
            }.ifEmpty { channels }
            else -> channels
        }

        // Put favorites at the top like YouTube TV Customization
        baseList.sortedByDescending { favoriteChannelIds.contains(it.id) }
    }

    // Set first channel as initially focused preview if null
    val previewChannel = focusedChannel ?: filteredChannels.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TvBackground)
    ) {
        // YouTube TV Top Bar
        TvTopBar(
            title = "Live",
            device = device,
            connectionState = connectionState,
            activeScreen = AppScreen.LIVE,
            onNavigate = { screen -> viewModel.navigateTo(screen) },
            onOpenSettings = { viewModel.navigateTo(AppScreen.SETTINGS) }
        )

        // Live EPG Sub-Header with Category Chips & Silent Live Preview in corner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(categories) { category ->
                    val isSelected = selectedCategory == category
                    val interactionSource = remember { MutableInteractionSource() }
                    val isFocused by interactionSource.collectIsFocusedAsState()

                    Box(
                        modifier = Modifier
                            .testTag("epg_chip_$category")
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                when {
                                    isSelected -> Color.White
                                    isFocused -> Color(0xFF323642)
                                    else -> Color(0xFF1B1E26)
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = if (isFocused) Color.White else Color(0xFF282C38),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable(interactionSource = interactionSource, indication = null) {
                                selectedCategory = category
                            }
                            .focusable(interactionSource = interactionSource)
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = category,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.Black else Color.White
                        )
                    }
                }
            }

            // Right: Silent Live Video Preview in Corner (YouTube TV hallmark feature)
            if (previewChannel != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF141720))
                        .border(1.dp, Color(0xFF2A2F3D), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp, 24.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF222634)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeMute,
                            contentDescription = "Silent Live Preview",
                            tint = TvCyanPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF0000))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "LIVE PREVIEW",
                                color = Color(0xFFFF0000),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = " • ${previewChannel.channelNumberFormatted} ${previewChannel.network}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = airingsMap[previewChannel.id]?.title ?: "Live Program Feed",
                            color = TvTextSecondary,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Timeline Bar Header (Now | +30 min | +60 min | +90 min)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF12141A))
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CHANNELS",
                color = TvTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.width(170.dp)
            )
            Text(
                text = "NOW (LIVE)",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.weight(1.6f)
            )
            Text(
                text = "+30 MIN",
                color = TvTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "+60 MIN",
                color = TvTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Main EPG Grid
        if (channels.isEmpty() && isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = TvCyanPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading Live Guide from Tablo...", color = TvTextSecondary)
                }
            }
        } else if (channels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp)
                    .background(TvSurface, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Tv, contentDescription = null, tint = TvTextTertiary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No channels found", color = TvTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Check your antenna connection or refresh the guide.", color = TvTextSecondary, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredChannels, key = { it.id }) { channel ->
                    val isFavorite = favoriteChannelIds.contains(channel.id)
                    val currentTitle = airingsMap[channel.id]?.title ?: "Live Programming"
                    val nextTitle = getUpcomingTitle(channel)

                    EpgChannelRow(
                        channel = channel,
                        currentTitle = currentTitle,
                        nextTitle = nextTitle,
                        isFavorite = isFavorite,
                        onFocus = { focusedChannel = channel },
                        onSelectChannel = {
                            // Instant 1-click fullscreen playback - ZERO CLUTTER!
                            viewModel.launchChannel(channel)
                        },
                        onToggleFavorite = {
                            favoriteChannelIds = if (isFavorite) {
                                favoriteChannelIds - channel.id
                            } else {
                                favoriteChannelIds + channel.id
                            }
                        }
                    )
                }

                item { Spacer(modifier = Modifier.height(30.dp)) }
            }
        }
    }
}

@Composable
private fun EpgChannelRow(
    channel: TabloChannel,
    currentTitle: String,
    nextTitle: String,
    isFavorite: Boolean,
    onFocus: () -> Unit,
    onSelectChannel: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    TvFocusableCard(
        onClick = onSelectChannel,
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp),
        focusedContainerColor = Color(0xFF222634),
        unfocusedContainerColor = Color(0xFF141720),
        focusedBorderColor = Color.White,
        unfocusedBorderColor = Color(0xFF232734),
        testTag = "guide_row_${channel.id}"
    ) { isFocused ->
        if (isFocused) {
            onFocus()
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Channel Info Column (Channel #, Network, Star)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.width(170.dp)
            ) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite Channel",
                        tint = if (isFavorite) TvAmberAccent else Color(0xFF555966),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isFocused) Color.White else Color(0xFF1E222D)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = channel.channelNumberFormatted,
                        color = if (isFocused) Color.Black else Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = channel.network.ifBlank { channel.callSign },
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = channel.callSign,
                        color = TvTextSecondary,
                        fontSize = 10.sp
                    )
                }
            }

            // Program Block 1: Now / Live (with live progress indicator)
            Box(
                modifier = Modifier
                    .weight(1.6f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isFocused) Color(0xFF2B3245) else Color(0xFF181C26))
                    .border(1.dp, if (isFocused) Color.White.copy(alpha = 0.5f) else Color(0xFF252A38), RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF0000))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = currentTitle,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    // Subtle live broadcast progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(Color(0xFFFF0000).copy(alpha = 0.8f))
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Program Block 2: +30 min
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF161922))
                    .border(1.dp, Color(0xFF222634), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = nextTitle,
                    color = TvTextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Program Block 3: +60 min
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF161922))
                    .border(1.dp, Color(0xFF222634), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "Primetime Feature",
                    color = TvTextTertiary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun getUpcomingTitle(channel: TabloChannel): String {
    val net = channel.network.lowercase()
    return when {
        net.contains("cbs") -> "CBS Evening News"
        net.contains("nbc") -> "NBC Nightly News"
        net.contains("fox") -> "FOX NFL Kickoff"
        net.contains("abc") -> "World News Tonight"
        net.contains("pbs") -> "PBS NewsHour"
        else -> "Scheduled Program"
    }
}
