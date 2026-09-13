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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TabloConnectionState
import com.example.ui.components.TvButton
import com.example.ui.components.TvButtonStyle
import com.example.ui.components.TvFocusableCard
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
fun RegistrationScreen(
    viewModel: TabloAppViewModel,
    modifier: Modifier = Modifier
) {
    val connectionState by viewModel.deviceRepository.connectionState.collectAsState()
    val discoveredDevices by viewModel.deviceRepository.discoveredDevices.collectAsState()
    val isDiscovering = connectionState == TabloConnectionState.DISCOVERING

    LaunchedEffect(Unit) {
        if (discoveredDevices.isEmpty()) {
            viewModel.startDiscovery()
        }
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(TvBackground)
            .padding(40.dp)
    ) {
        // Left Column: Information & Actions
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 32.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(TvSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Tv,
                    contentDescription = "Tablo",
                    tint = TvCyanPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "CONNECT TO TABLO GEN 4",
                color = TvTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "This app connects directly to your Tablo Gen 4 over your local Wi-Fi or Ethernet network. No login, cloud account, or subscription is required.",
                color = TvTextSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TvButton(
                    text = if (isDiscovering) "Scanning..." else "Scan Network",
                    onClick = { viewModel.startDiscovery() },
                    style = TvButtonStyle.PRIMARY,
                    enabled = !isDiscovering,
                    leadingIcon = {
                        if (isDiscovering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = TvBackground
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = TvBackground)
                        }
                    },
                    testTag = "btn_scan_network"
                )

                TvButton(
                    text = "Manual IP Address",
                    onClick = { viewModel.navigateTo(AppScreen.MANUAL_IP) },
                    style = TvButtonStyle.OUTLINE,
                    leadingIcon = {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = TvCyanPrimary)
                    },
                    testTag = "btn_manual_ip"
                )
            }
        }

        // Right Column: Discovered Devices List
        Column(
            modifier = Modifier.weight(1.2f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DISCOVERED DEVICES",
                    color = TvCyanPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                if (isDiscovering) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = TvCyanPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Searching LAN...",
                            color = TvTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (discoveredDevices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(TvSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        if (isDiscovering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = TvCyanPrimary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Probing UDP port 8881 & local subnet...",
                                color = TvTextSecondary,
                                fontSize = 14.sp
                            )
                        } else {
                            Icon(
                                Icons.Default.CastConnected,
                                contentDescription = null,
                                tint = TvTextTertiary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No Tablo detected automatically",
                                color = TvTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Check that your Tablo is on and connected to the same Wi-Fi, or use 'Manual IP Address' to connect directly.",
                                color = TvTextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(discoveredDevices) { device ->
                        TvFocusableCard(
                            onClick = { viewModel.registerDiscoveredDevice(device) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp),
                            testTag = "discovered_tablo_${device.host}"
                        ) { isFocused ->
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(if (isFocused) TvCyanPrimary else TvSurfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Tv,
                                            contentDescription = null,
                                            tint = if (isFocused) TvBackground else TvCyanPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = device.name,
                                            color = TvTextPrimary,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${device.host} • ${device.modelName} (${device.tunerCount} tuners)",
                                            color = TvTextSecondary,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                TvButton(
                                    text = "Connect",
                                    onClick = { viewModel.registerDiscoveredDevice(device) },
                                    style = if (isFocused) TvButtonStyle.PRIMARY else TvButtonStyle.SECONDARY,
                                    modifier = Modifier.height(40.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
