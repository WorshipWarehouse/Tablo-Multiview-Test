package com.example.ui.components

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.TabloConnectionState
import com.example.model.TabloDevice
import com.example.ui.theme.TvBackground
import com.example.ui.theme.TvBorderNormal
import com.example.ui.theme.TvCyanPrimary
import com.example.ui.theme.TvError
import com.example.ui.theme.TvSuccess
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary
import com.example.ui.theme.TvWarning
import com.example.ui.viewmodel.AppScreen

@Composable
fun TvTopBar(
    title: String,
    device: TabloDevice?,
    connectionState: TabloConnectionState,
    modifier: Modifier = Modifier,
    activeScreen: AppScreen = AppScreen.HOME,
    onNavigate: ((AppScreen) -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(TvBackground)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: YouTube TV Styled Logo / Brand
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onNavigate?.invoke(AppScreen.HOME) }
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFF0000)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_app_brand_logo),
                    contentDescription = "YouTube TV Style Logo",
                    modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp))
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "TABLO",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Text(
                text = " TV",
                color = Color(0xFFFF0000),
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }

        // Center: YouTube TV 3 Primary Tabs (LIBRARY, HOME, LIVE)
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TopNavTabItem(
                label = "LIBRARY",
                isSelected = activeScreen == AppScreen.LIBRARY,
                testTag = "tab_library",
                onClick = { onNavigate?.invoke(AppScreen.LIBRARY) }
            )
            TopNavTabItem(
                label = "HOME",
                isSelected = activeScreen == AppScreen.HOME,
                testTag = "tab_home",
                onClick = { onNavigate?.invoke(AppScreen.HOME) }
            )
            TopNavTabItem(
                label = "LIVE",
                isSelected = activeScreen == AppScreen.LIVE || activeScreen == AppScreen.CHANNEL_GUIDE,
                testTag = "tab_live",
                onClick = { onNavigate?.invoke(AppScreen.LIVE) }
            )
        }

        // Right: Connection Pill & Settings
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Connection Status Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF141720))
                    .border(1.dp, TvBorderNormal, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                val (statusColor, statusText) = when (connectionState) {
                    TabloConnectionState.CONNECTED -> Pair(TvSuccess, device?.name ?: "Tablo Connected")
                    TabloConnectionState.CONNECTING -> Pair(TvWarning, "Connecting...")
                    TabloConnectionState.DISCOVERING -> Pair(TvCyanPrimary, "Discovering...")
                    TabloConnectionState.ERROR -> Pair(TvError, "Disconnected")
                    TabloConnectionState.DISCONNECTED -> Pair(TvTextSecondary, "Offline")
                }

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )

                Text(
                    text = "  $statusText",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Settings Gear Icon
            if (onOpenSettings != null) {
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1C1F28))
                        .testTag("btn_top_settings")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TopNavTabItem(
    label: String,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(6.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = if (isSelected || isFocused) FontWeight.ExtraBold else FontWeight.Medium,
            letterSpacing = 1.sp,
            color = when {
                isFocused -> Color.White
                isSelected -> Color.White
                else -> TvTextSecondary
            }
        )
        Spacer(modifier = Modifier.height(4.dp))
        // YouTube TV active indicator bar
        Box(
            modifier = Modifier
                .width(if (isSelected || isFocused) 28.dp else 0.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    when {
                        isFocused -> Color.White
                        isSelected -> Color(0xFFFF0000)
                        else -> Color.Transparent
                    }
                )
        )
    }
}
