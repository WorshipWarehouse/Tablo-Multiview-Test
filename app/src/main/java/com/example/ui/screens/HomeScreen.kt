package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsFootball
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.DvrCategory
import com.example.model.MultiviewLayoutMode
import com.example.model.TabloChannel
import com.example.ui.components.TvButton
import com.example.ui.components.TvButtonStyle
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
fun HomeScreen(
    viewModel: TabloAppViewModel,
    modifier: Modifier = Modifier
) {
    val device by viewModel.deviceRepository.currentDevice.collectAsState()
    val connectionState by viewModel.deviceRepository.connectionState.collectAsState()
    val channels by viewModel.channelRepository.channels.collectAsState()
    val airingsMap by viewModel.channelRepository.airingsMap.collectAsState()

    val featuredChannel = channels.firstOrNull()
    var isAddedToLibrary by remember { mutableStateOf(false) }

    val sportsChannels = remember(channels) {
        channels.filter { ch ->
            val text = (ch.network + " " + ch.callSign).lowercase()
            text.contains("cbs") || text.contains("fox") || text.contains("nbc") || text.contains("abc") || text.contains("sport")
        }.ifEmpty { channels.take(4) }
    }

    val newsChannels = remember(channels) {
        channels.filter { ch ->
            val text = (ch.network + " " + ch.callSign).lowercase()
            text.contains("news") || text.contains("pbs") || text.contains("weather")
        }.ifEmpty { channels.drop(2).take(4) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TvBackground)
    ) {
        // YouTube TV 3-Tab Top Navigation Bar (LIBRARY, HOME, LIVE)
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
                .padding(horizontal = 32.dp, vertical = 12.dp)
        ) {
            // YouTube TV Hero Discovery Showcase (Featured Live Stream Preview)
            if (featuredChannel != null) {
                val featuredAiring = airingsMap[featuredChannel.id]?.title ?: "NFL Sunday Ticket: Live AFC Showdown"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF1E2433),
                                    Color(0xFF10131B)
                                )
                            )
                        )
                        .border(1.dp, Color(0xFF2E3547), RoundedCornerShape(16.dp))
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFF0000))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "FEATURED LIVE",
                                    color = Color(0xFFFF0000),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = " • ${featuredChannel.channelNumberFormatted} ${featuredChannel.network}",
                                    color = TvTextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = featuredAiring,
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Live broadcast streaming directly from ${device?.name ?: "Tablo TV"}. Crystal clear 1080p OTA signal.",
                                color = TvTextSecondary,
                                fontSize = 13.sp,
                                maxLines = 2
                            )
                        }

                        // Hero Action Buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TvButton(
                                text = "Watch Live",
                                onClick = {
                                    // 1-click immediate fullscreen playback!
                                    viewModel.launchChannel(featuredChannel)
                                },
                                style = TvButtonStyle.PRIMARY,
                                minHeight = 44.dp,
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.Black
                                    )
                                },
                                testTag = "btn_hero_watch"
                            )

                            TvButton(
                                text = if (isAddedToLibrary) "Added to Library" else "+ Add to Library",
                                onClick = {
                                    isAddedToLibrary = !isAddedToLibrary
                                    if (isAddedToLibrary) {
                                        viewModel.addToLibrary(
                                            title = featuredAiring,
                                            channel = featuredChannel,
                                            category = DvrCategory.SPORTS,
                                            subtitle = "Recorded from Home Discovery"
                                        )
                                    }
                                },
                                style = if (isAddedToLibrary) TvButtonStyle.SECONDARY else TvButtonStyle.OUTLINE,
                                minHeight = 44.dp,
                                leadingIcon = {
                                    Icon(
                                        if (isAddedToLibrary) Icons.Default.Check else Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                },
                                testTag = "btn_hero_add_library"
                            )

                            TvButton(
                                text = "4-Stream Multiview",
                                onClick = {
                                    viewModel.startMultiviewWithMode(MultiviewLayoutMode.FOUR_PANE)
                                },
                                style = TvButtonStyle.OUTLINE,
                                minHeight = 44.dp,
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.GridView,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                },
                                testTag = "btn_hero_multiview"
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Shelf 1: Top Picks for You (Live broadcast channels)
            HomeSectionHeader(
                title = "TOP PICKS FOR YOU",
                subtitle = "Recommended live broadcasts based on your viewing",
                onSeeAll = { viewModel.navigateTo(AppScreen.LIVE) }
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(channels.take(8)) { channel ->
                    val airing = airingsMap[channel.id]?.title ?: "Live Programming"
                    HomeChannelCard(
                        channel = channel,
                        airingTitle = airing,
                        badge = "LIVE",
                        onClick = {
                            // Instant 1-click fullscreen playback - ZERO CLUTTER!
                            viewModel.launchChannel(channel)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Shelf 2: Live Sports (NFL Sunday Ticket & Local Affiliates)
            HomeSectionHeader(
                title = "LIVE SPORTS",
                subtitle = "NFL Sunday Ticket, college football, and local game broadcasts",
                onSeeAll = { viewModel.navigateTo(AppScreen.LIVE) }
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(sportsChannels) { channel ->
                    val sportTitle = getSportsTitle(channel)
                    HomeChannelCard(
                        channel = channel,
                        airingTitle = sportTitle,
                        badge = "4th Qtr • 2:15",
                        isSports = true,
                        onClick = {
                            viewModel.launchChannel(channel)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Shelf 3: Live News & Info
            HomeSectionHeader(
                title = "LIVE NEWS",
                subtitle = "National news desks and continuous local coverage",
                onSeeAll = { viewModel.navigateTo(AppScreen.LIVE) }
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(newsChannels) { channel ->
                    HomeChannelCard(
                        channel = channel,
                        airingTitle = "${channel.network} Live Broadcast",
                        badge = "LIVE NEWS",
                        onClick = {
                            viewModel.launchChannel(channel)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Shelf 4: YouTube TV Multiview Showcase Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF141822))
                    .border(1.dp, Color(0xFF282F42), RoundedCornerShape(14.dp))
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.GridView,
                                contentDescription = null,
                                tint = TvCyanPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CUSTOM MULTIVIEW (SPLIT-SCREEN)",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Watch up to 4 games or shows simultaneously. Use your remote D-pad to move the focus box for active audio. Press DOWN while watching any channel to build a Multiview.",
                            color = TvTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    TvButton(
                        text = "Build Multiview",
                        onClick = {
                            viewModel.openMultiviewBuilder()
                            viewModel.startMultiviewWithMode(MultiviewLayoutMode.FOUR_PANE)
                        },
                        style = TvButtonStyle.PRIMARY,
                        minHeight = 40.dp,
                        testTag = "btn_home_build_multiview"
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun HomeSectionHeader(
    title: String,
    subtitle: String,
    onSeeAll: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TvTextSecondary,
                fontSize = 12.sp
            )
        }

        Text(
            text = "See Guide >",
            color = TvCyanPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable { onSeeAll() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun HomeChannelCard(
    channel: TabloChannel,
    airingTitle: String,
    badge: String,
    isSports: Boolean = false,
    onClick: () -> Unit
) {
    TvFocusableCard(
        onClick = onClick,
        modifier = Modifier
            .width(230.dp)
            .height(130.dp),
        focusedContainerColor = Color(0xFF222634),
        unfocusedContainerColor = Color(0xFF141720),
        focusedBorderColor = Color.White,
        unfocusedBorderColor = Color(0xFF232734),
        testTag = "home_card_${channel.id}"
    ) { isFocused ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Card Header: Badge + Channel Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSports) Color(0xFF2B1C10) else Color(0xFF2B1010))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (isSports) TvAmberAccent else Color(0xFFFF0000))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = badge,
                        color = if (isSports) TvAmberAccent else Color(0xFFFF6666),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Channel Number
                Text(
                    text = channel.channelNumberFormatted,
                    color = TvTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Card Body: Show Title & Network
            Column {
                Text(
                    text = airingTitle,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = channel.network.ifBlank { channel.callSign },
                    color = TvTextSecondary,
                    fontSize = 11.sp
                )
            }

            // Bottom 1-Click prompt when focused
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (isFocused) Color.White else Color.Transparent,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isFocused) "Press OK to Watch" else "",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun getSportsTitle(channel: TabloChannel): String {
    val net = channel.network.lowercase()
    return when {
        net.contains("cbs") -> "NFL: Chiefs at Ravens"
        net.contains("fox") -> "NFL: 49ers vs Rams"
        net.contains("nbc") -> "Sunday Night Football: Eagles at Cowboys"
        net.contains("abc") -> "College Football: Ohio State vs Michigan"
        else -> "Live Sports Broadcast"
    }
}
