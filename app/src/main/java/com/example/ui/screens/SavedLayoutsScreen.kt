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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MultiviewLayoutMode
import com.example.model.SavedLayout
import com.example.ui.components.TvTopBar
import com.example.ui.theme.CompetitorAppBg
import com.example.ui.theme.CompetitorCardBg
import com.example.ui.theme.CompetitorCardBorder
import com.example.ui.theme.CompetitorLayoutBlue
import com.example.ui.theme.CompetitorPurple
import com.example.ui.theme.CompetitorTabInactive
import com.example.ui.theme.TvTextPrimary
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.TabloAppViewModel

@Composable
fun SavedLayoutsScreen(
    viewModel: TabloAppViewModel,
    modifier: Modifier = Modifier
) {
    val layouts by viewModel.savedLayouts.collectAsState()
    val device by viewModel.deviceRepository.currentDevice.collectAsState()
    val connectionState by viewModel.deviceRepository.connectionState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CompetitorAppBg)
    ) {
        // macOS Top Navigation Bar
        TvTopBar(
            title = "Layouts",
            device = device,
            connectionState = connectionState,
            activeScreen = AppScreen.SAVED_LAYOUTS,
            onNavigate = { screen -> viewModel.navigateTo(screen) },
            onOpenSettings = { viewModel.navigateTo(AppScreen.SETTINGS) }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Layouts",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = TvTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Choose a layout mode or resume your saved multiview configurations",
                    fontSize = 13.sp,
                    color = CompetitorTabInactive
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Quick Layout Options (Screenshot 4)
            item {
                Text(
                    text = "Start New Layout",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickLayoutRow(
                        title = "Single View",
                        subtitle = "1 channel",
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(width = 24.dp, height = 18.dp)
                                    .border(1.5.dp, CompetitorLayoutBlue, RoundedCornerShape(2.dp))
                            )
                        },
                        onClick = { viewModel.startMultiviewWithMode(MultiviewLayoutMode.SINGLE) }
                    )

                    QuickLayoutRow(
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
                        onClick = { viewModel.startMultiviewWithMode(MultiviewLayoutMode.SIDE_BY_SIDE) }
                    )

                    QuickLayoutRow(
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
                        onClick = { viewModel.startMultiviewWithMode(MultiviewLayoutMode.FOCUS_PRIMARY_BOTTOM_STRIP) }
                    )

                    QuickLayoutRow(
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
                        onClick = { viewModel.startMultiviewWithMode(MultiviewLayoutMode.QUAD_GRID) }
                    )
                }
            }

            // Saved Custom Presets
            if (layouts.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Saved Presets",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(layouts, key = { it.id }) { layout ->
                    CompetitorSavedPresetItem(
                        layout = layout,
                        onLaunch = { viewModel.launchSavedLayout(layout) },
                        onDelete = { viewModel.deleteSavedLayout(layout.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickLayoutRow(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
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
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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

@Composable
private fun CompetitorSavedPresetItem(
    layout: SavedLayout,
    onLaunch: () -> Unit,
    onDelete: () -> Unit
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
            .clickable(interactionSource = interactionSource, indication = null, onClick = onLaunch)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("preset_card_${layout.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CompetitorPurple)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "${layout.mode.paneCount}P",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = layout.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = layout.channelLabels.joinToString(" • "),
                        fontSize = 12.sp,
                        color = CompetitorTabInactive,
                        maxLines = 1
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(CompetitorPurple)
                        .clickable(onClick = onLaunch)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
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
                            text = "Launch",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = CompetitorTabInactive,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
