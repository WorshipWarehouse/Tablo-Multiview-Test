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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VideoLibrary
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DvrCategory
import com.example.model.DvrRecording
import com.example.ui.components.TvFocusableCard
import com.example.ui.components.TvTopBar
import com.example.ui.theme.CompetitorAppBg
import com.example.ui.theme.CompetitorCardBg
import com.example.ui.theme.CompetitorCardBorder
import com.example.ui.theme.CompetitorPurple
import com.example.ui.theme.CompetitorTabInactive
import com.example.ui.theme.TvTextPrimary
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.TabloAppViewModel

@Composable
fun LibraryScreen(
    viewModel: TabloAppViewModel,
    modifier: Modifier = Modifier
) {
    val device by viewModel.deviceRepository.currentDevice.collectAsState()
    val connectionState by viewModel.deviceRepository.connectionState.collectAsState()
    val recordings by viewModel.dvrRecordings.collectAsState()
    val allChannels by viewModel.channelRepository.channels.collectAsState()
    var selectedCategory by remember { mutableStateOf(DvrCategory.ALL) }

    val filteredRecordings = remember(recordings, selectedCategory) {
        if (selectedCategory == DvrCategory.ALL) recordings
        else recordings.filter { it.category == selectedCategory }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CompetitorAppBg)
    ) {
        // macOS Top Navigation Bar
        TvTopBar(
            title = "Library",
            device = device,
            connectionState = connectionState,
            activeScreen = AppScreen.LIBRARY,
            onNavigate = { screen -> viewModel.navigateTo(screen) },
            onOpenSettings = { viewModel.navigateTo(AppScreen.SETTINGS) }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header Info: Cloud DVR status & storage
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Library",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = TvTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Your recordings and saved programs appear here",
                            fontSize = 13.sp,
                            color = CompetitorTabInactive
                        )
                    }

                    // DVR Storage Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(CompetitorCardBg)
                            .border(1.dp, CompetitorCardBorder, RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = null,
                            tint = CompetitorPurple,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Tablo Local DVR • 128 GB Available",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Category Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(DvrCategory.values()) { category ->
                        val isSelected = selectedCategory == category
                        val interactionSource = remember { MutableInteractionSource() }
                        val isFocused by interactionSource.collectIsFocusedAsState()

                        Box(
                            modifier = Modifier
                                .testTag("dvr_chip_${category.name}")
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    when {
                                        isSelected -> CompetitorPurple
                                        isFocused -> Color(0xFF222532)
                                        else -> CompetitorCardBg
                                    }
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) CompetitorPurple else CompetitorCardBorder,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable(interactionSource = interactionSource, indication = null) {
                                    selectedCategory = category
                                }
                                .focusable(interactionSource = interactionSource)
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = category.displayName,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else CompetitorTabInactive
                            )
                        }
                    }
                }
            }

            // Recordings Grid / Rows
            if (filteredRecordings.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(CompetitorCardBg, RoundedCornerShape(12.dp))
                            .border(1.dp, CompetitorCardBorder, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.VideoLibrary,
                                contentDescription = null,
                                tint = CompetitorTabInactive,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No recordings in ${selectedCategory.displayName}",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Select any program in the Guide to record it directly to your Tablo",
                                color = CompetitorTabInactive,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        filteredRecordings.forEach { recording ->
                            CompetitorRecordingCardItem(
                                recording = recording,
                                onPlay = {
                                    val ch = allChannels.find { it.id == recording.channelId }
                                    if (ch != null) {
                                        viewModel.playChannelInMultiview(0, ch)
                                    } else {
                                        viewModel.startMultiviewWithMode(com.example.model.MultiviewLayoutMode.SINGLE)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompetitorRecordingCardItem(
    recording: DvrRecording,
    onPlay: () -> Unit
) {
    TvFocusableCard(
        onClick = onPlay,
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp),
        focusedContainerColor = Color(0xFF222532),
        unfocusedContainerColor = CompetitorCardBg,
        focusedBorderColor = CompetitorPurple,
        unfocusedBorderColor = CompetitorCardBorder,
        testTag = "dvr_recording_${recording.id}"
    ) { isFocused ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                // Channel / Episode badge
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF161820))
                        .border(1.dp, CompetitorCardBorder, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (isFocused) CompetitorPurple else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = recording.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        if (recording.isWatched) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Watched",
                                tint = CompetitorTabInactive,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${recording.network} • ${recording.subtitle.ifBlank { "Live Broadcast" }} • ${recording.durationMinutes}m",
                        fontSize = 12.sp,
                        color = CompetitorTabInactive,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isFocused) CompetitorPurple else Color(0xFF1E212A))
                    .border(1.dp, if (isFocused) CompetitorPurple else CompetitorCardBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Play",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
