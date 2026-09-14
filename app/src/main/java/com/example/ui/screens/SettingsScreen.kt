package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TabloConnectionState
import com.example.ui.components.TvFocusableCard
import com.example.ui.components.TvTopBar
import com.example.ui.theme.CompetitorAppBg
import com.example.ui.theme.CompetitorCardBg
import com.example.ui.theme.CompetitorCardBorder
import com.example.ui.theme.CompetitorGreen
import com.example.ui.theme.CompetitorPurple
import com.example.ui.theme.CompetitorTabInactive
import com.example.ui.theme.TvError
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvWarning
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.TabloAppViewModel

@Composable
fun SettingsScreen(
    viewModel: TabloAppViewModel,
    modifier: Modifier = Modifier
) {
    val device by viewModel.deviceRepository.currentDevice.collectAsState()
    val connectionState by viewModel.deviceRepository.connectionState.collectAsState()
    val isConnecting = connectionState == TabloConnectionState.CONNECTING

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CompetitorAppBg)
    ) {
        // macOS Top Navigation Bar
        TvTopBar(
            title = "Settings",
            device = device,
            connectionState = connectionState,
            activeScreen = AppScreen.SETTINGS,
            onNavigate = { screen -> viewModel.navigateTo(screen) },
            onOpenSettings = { }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Settings",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TvTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Device specs, 4-tuner buffer configuration, and network diagnostics",
                fontSize = 13.sp,
                color = CompetitorTabInactive
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 2-Column TV Layout: Left is Actionable Menu; Right is specs
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Left Column: Device & Actions
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Device Summary Header Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CompetitorCardBg)
                            .border(1.dp, CompetitorCardBorder, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF16181E))
                                        .border(1.dp, CompetitorCardBorder, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Tv, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = device?.name ?: "Tablo DUAL LITE (4-Tuner)",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = device?.host ?: "192.168.1.189",
                                        color = CompetitorTabInactive,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            // Connection Indicator
                            val (statusColor, statusText) = when (connectionState) {
                                TabloConnectionState.CONNECTED -> Pair(CompetitorGreen, "Connected")
                                TabloConnectionState.CONNECTING -> Pair(TvWarning, "Connecting...")
                                TabloConnectionState.DISCOVERING -> Pair(Color.White, "Scanning...")
                                TabloConnectionState.ERROR -> Pair(TvError, "Offline")
                                TabloConnectionState.DISCONNECTED -> Pair(CompetitorTabInactive, "Ready")
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF16181E))
                                    .border(1.dp, CompetitorCardBorder, RoundedCornerShape(16.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(statusColor))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = statusText, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "ACTIONS",
                        color = CompetitorTabInactive,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    // Action 1: Test Connection Ping
                    TvFocusableCard(
                        onClick = { viewModel.retryConnection() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        focusedContainerColor = Color(0xFF222530),
                        unfocusedContainerColor = CompetitorCardBg,
                        focusedBorderColor = CompetitorPurple,
                        unfocusedBorderColor = CompetitorCardBorder,
                        enabled = !isConnecting,
                        testTag = "btn_test_ping"
                    ) { isFocused ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isConnecting) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = CompetitorPurple, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = if (isFocused) Color.White else CompetitorTabInactive, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = if (isConnecting) "Testing Connection..." else "Test Connection Ping",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Verify port 8885 response and 4-tuner availability",
                                    color = CompetitorTabInactive,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Action 2: Change IP Address
                    TvFocusableCard(
                        onClick = { viewModel.navigateTo(AppScreen.MANUAL_IP) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        focusedContainerColor = Color(0xFF222530),
                        unfocusedContainerColor = CompetitorCardBg,
                        focusedBorderColor = CompetitorPurple,
                        unfocusedBorderColor = CompetitorCardBorder,
                        testTag = "btn_change_ip"
                    ) { isFocused ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = if (isFocused) Color.White else CompetitorTabInactive, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Change IP Address",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Direct connection to Tablo on a specific subnet",
                                    color = CompetitorTabInactive,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Action 3: Scan LAN for Tablos
                    TvFocusableCard(
                        onClick = { viewModel.navigateTo(AppScreen.REGISTRATION) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        focusedContainerColor = Color(0xFF222530),
                        unfocusedContainerColor = CompetitorCardBg,
                        focusedBorderColor = CompetitorPurple,
                        unfocusedBorderColor = CompetitorCardBorder,
                        testTag = "btn_scan_lan"
                    ) { isFocused ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = if (isFocused) Color.White else CompetitorTabInactive, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Scan Local Network",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Find Tablo Gen 4 tuners on Wi-Fi / Ethernet",
                                    color = CompetitorTabInactive,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Action 4: Forget Device
                    TvFocusableCard(
                        onClick = { viewModel.forgetDevice() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        focusedContainerColor = Color(0xFF222530),
                        unfocusedContainerColor = CompetitorCardBg,
                        focusedBorderColor = TvError,
                        unfocusedBorderColor = CompetitorCardBorder,
                        testTag = "btn_forget_device"
                    ) { isFocused ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = if (isFocused) TvError else CompetitorTabInactive, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Disconnect & Forget Device",
                                    color = if (isFocused) TvError else Color(0xFFD1D1D6),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Clear stored IP and reset connection credentials",
                                    color = CompetitorTabInactive,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // Right Column: Hardware & Stream Specifications
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Hardware & Device Specs Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CompetitorCardBg)
                            .border(1.dp, CompetitorCardBorder, RoundedCornerShape(12.dp))
                            .padding(18.dp)
                    ) {
                        Column {
                            Text(
                                text = "HARDWARE SPECIFICATIONS",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            CompetitorDiagRow(label = "IP Address", value = device?.host ?: "192.168.1.189")
                            CompetitorDiagRow(label = "Streaming Port", value = "${device?.port ?: 8885}")
                            CompetitorDiagRow(label = "Hardware Tuners", value = "4 Active Tuners")
                            CompetitorDiagRow(label = "Firmware Build", value = device?.version?.ifBlank { "2.2.40" } ?: "2.2.40 Lighthouse")
                            CompetitorDiagRow(label = "Server ID", value = device?.serverId?.ifBlank { "tablo_004" } ?: "tablo_004")
                        }
                    }

                    // Streaming Engine Specs Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CompetitorCardBg)
                            .border(1.dp, CompetitorCardBorder, RoundedCornerShape(12.dp))
                            .padding(18.dp)
                    ) {
                        Column {
                            Text(
                                text = "STREAMING ENGINE CONFIGURATION",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            CompetitorDiagRow(label = "Per-Player Memory Limit", value = "4MB Capped (16MB Total)")
                            CompetitorDiagRow(label = "Tuner Keepalive Interval", value = "20s Automatic Ping")
                            CompetitorDiagRow(label = "Simultaneous Streams", value = "4 Active Decoders")
                            CompetitorDiagRow(label = "Hardware Acceleration", value = "Active (Zero OOM)")
                            CompetitorDiagRow(label = "Network Topology", value = "Direct LAN (Zero Cloud Latency)")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompetitorDiagRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = CompetitorTabInactive, fontSize = 12.sp)
        Text(text = value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
