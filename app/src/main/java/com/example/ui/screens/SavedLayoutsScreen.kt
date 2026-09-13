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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PlayArrow
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
import com.example.model.SavedLayout
import com.example.ui.components.TvButton
import com.example.ui.components.TvButtonStyle
import com.example.ui.components.TvFocusableCard
import com.example.ui.theme.TvAmberAccent
import com.example.ui.theme.TvBackground
import com.example.ui.theme.TvCyanPrimary
import com.example.ui.theme.TvError
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvSurfaceVariant
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary
import com.example.ui.theme.TvTextTertiary
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.TabloAppViewModel

@Composable
fun SavedLayoutsScreen(
    viewModel: TabloAppViewModel,
    modifier: Modifier = Modifier
) {
    val layouts by viewModel.savedLayouts.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TvBackground)
            .padding(28.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                testTag = "btn_layouts_back"
            )
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(
                    text = "SAVED MULTIVIEW PRESETS",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Quickly launch your favorite channel grid combinations",
                    color = TvTextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (layouts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF141416))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.GridView,
                        contentDescription = null,
                        tint = TvTextSecondary,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No saved presets yet",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "While watching Multiview, press SELECT and choose 'Save Preset'.",
                        color = TvTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TvButton(
                        text = "Start Multiview Now",
                        onClick = { viewModel.startMultiviewWithLastSession() },
                        style = TvButtonStyle.PRIMARY
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(layouts) { layout ->
                    SavedLayoutCardItem(
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
fun SavedLayoutCardItem(
    layout: SavedLayout,
    onLaunch: () -> Unit,
    onDelete: () -> Unit
) {
    TvFocusableCard(
        onClick = onLaunch,
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp),
        focusedContainerColor = Color(0xFF222226),
        unfocusedContainerColor = Color(0xFF141416),
        focusedBorderColor = Color.White,
        unfocusedBorderColor = Color(0xFF2C2C2E),
        testTag = "preset_card_${layout.id}"
    ) { isFocused ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                // Mode Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isFocused) Color.White else Color(0xFF222226))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${layout.mode.paneCount}-PANE",
                        color = if (isFocused) Color.Black else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = layout.name,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    val channelsText = layout.channelLabels.filter { it.isNotBlank() }.joinToString("  •  ")
                    Text(
                        text = channelsText.ifBlank { "${layout.channelIds.size} channels assigned" },
                        color = TvTextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TvButton(
                    text = "Launch Multiview",
                    onClick = onLaunch,
                    style = if (isFocused) TvButtonStyle.PRIMARY else TvButtonStyle.SECONDARY,
                    modifier = Modifier.height(40.dp),
                    leadingIcon = {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = if (isFocused) Color.Black else Color.White
                        )
                    }
                )

                TvButton(
                    text = "Delete",
                    onClick = onDelete,
                    style = TvButtonStyle.OUTLINE,
                    modifier = Modifier.height(40.dp),
                    leadingIcon = {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = TvError
                        )
                    }
                )
            }
        }
    }
}
