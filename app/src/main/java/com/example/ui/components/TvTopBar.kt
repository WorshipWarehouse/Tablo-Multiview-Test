package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.model.TabloConnectionState
import com.example.model.TabloDevice
import com.example.ui.theme.CompetitorAppBg
import com.example.ui.theme.CompetitorCardBorder
import com.example.ui.theme.CompetitorSurface
import com.example.ui.theme.CompetitorTabActive
import com.example.ui.theme.CompetitorTabInactive
import com.example.ui.theme.TvSuccess
import com.example.ui.theme.TvTextPrimary
import com.example.ui.viewmodel.AppScreen

@Composable
fun TvTopBar(
    title: String,
    device: TabloDevice?,
    connectionState: TabloConnectionState,
    modifier: Modifier = Modifier,
    activeScreen: AppScreen = AppScreen.HOME,
    onNavigate: ((AppScreen) -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    onFilterClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CompetitorAppBg)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: macOS Style Window Dots + App Brand (Screenshot 1)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onNavigate?.invoke(AppScreen.HOME) }
        ) {
            // Three window control dots
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFF5F56)))
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFFBD2E)))
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF27C93F)))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = "Multiview",
                color = TvTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp
            )
        }

        // Center: Pill Navigation Capsule: Search | Home | Guide | Library | Layouts (Screenshots 1 & 2)
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(CompetitorSurface)
                .border(1.dp, CompetitorCardBorder, RoundedCornerShape(22.dp))
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompetitorNavTab(
                label = "Search",
                isSelected = false,
                testTag = "tab_search",
                onClick = { onNavigate?.invoke(AppScreen.CHANNEL_GUIDE) }
            )
            CompetitorNavTab(
                label = "Home",
                isSelected = activeScreen == AppScreen.HOME,
                testTag = "tab_home",
                onClick = { onNavigate?.invoke(AppScreen.HOME) }
            )
            CompetitorNavTab(
                label = "Guide",
                isSelected = activeScreen == AppScreen.CHANNEL_GUIDE || activeScreen == AppScreen.LIVE,
                testTag = "tab_guide",
                onClick = { onNavigate?.invoke(AppScreen.CHANNEL_GUIDE) }
            )
            CompetitorNavTab(
                label = "Library",
                isSelected = activeScreen == AppScreen.LIBRARY,
                testTag = "tab_library",
                onClick = { onNavigate?.invoke(AppScreen.LIBRARY) }
            )
            CompetitorNavTab(
                label = "Layouts",
                isSelected = activeScreen == AppScreen.SAVED_LAYOUTS,
                testTag = "tab_layouts",
                onClick = { onNavigate?.invoke(AppScreen.SAVED_LAYOUTS) }
            )
        }

        // Right: Tuner Status Pill + Settings Gear (+ Filter in Guide)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tuner indicator badge
            val tunerCount = device?.tunerCount ?: 4
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF16201B))
                    .border(1.dp, Color(0xFF203828), RoundedCornerShape(12.dp))
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(TvSuccess)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$tunerCount Tuners",
                    color = Color(0xFF4ADE80),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (activeScreen == AppScreen.CHANNEL_GUIDE || activeScreen == AppScreen.LIVE) {
                IconButton(
                    onClick = { onFilterClick?.invoke() },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .testTag("btn_filter_channels")
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter Channels",
                        tint = CompetitorTabInactive,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (onOpenSettings != null) {
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .testTag("btn_top_settings")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = CompetitorTabInactive,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CompetitorNavTab(
    label: String,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = Modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected || isFocused) CompetitorTabActive else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isSelected || isFocused) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected || isFocused) Color.White else CompetitorTabInactive
        )
    }
}
