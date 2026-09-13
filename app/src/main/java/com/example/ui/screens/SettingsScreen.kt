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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TabloConnectionState
import com.example.ui.components.TvButton
import com.example.ui.components.TvButtonStyle
import com.example.ui.components.TvFocusableCard
import com.example.ui.theme.TvBackground
import com.example.ui.theme.TvError
import com.example.ui.theme.TvSuccess
import com.example.ui.theme.TvTextSecondary
import com.example.ui.theme.TvWarning
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.TabloAppViewModel

/**
 * Modern Minimalist TV Settings Screen.
 * Engineered for smooth, intuitive D-pad remote navigation with clear vertical focus progression.
 */
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
            .background(TvBackground)
            .padding(horizontal = 36.dp, vertical = 24.dp)
    ) {
        // Top Navigation Bar
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
                testTag = "btn_settings_back"
            )

            Spacer(modifier = Modifier.width(20.dp))

            Column {
                Text(
                    text = "SETTINGS",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Device specs, playback buffer configuration, and network diagnostics",
                    color = TvTextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2-Column TV Layout: Left is D-pad actionable menu; Right is diagnostic specs
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // Left Column: Interactive D-pad Action List
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
                        .background(Color(0xFF141416))
                        .border(1.dp, Color(0xFF2C2C2E), RoundedCornerShape(12.dp))
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
                                    .background(Color(0xFF222226)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Tv, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = device?.name ?: "No Tablo Connected",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = device?.host ?: "Not configured",
                                    color = TvTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Connection Indicator
                        val (statusColor, statusText) = when (connectionState) {
                            TabloConnectionState.CONNECTED -> Pair(TvSuccess, "Connected")
                            TabloConnectionState.CONNECTING -> Pair(TvWarning, "Connecting...")
                            TabloConnectionState.DISCOVERING -> Pair(Color.White, "Scanning...")
                            TabloConnectionState.ERROR -> Pair(TvError, "Offline")
                            TabloConnectionState.DISCONNECTED -> Pair(TvTextSecondary, "Disconnected")
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF1E1E22))
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
                    color = TvTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )

                // Focusable Action 1: Test Connection Ping
                TvFocusableCard(
                    onClick = { viewModel.retryConnection() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    focusedContainerColor = Color(0xFF26262A),
                    unfocusedContainerColor = Color(0xFF141416),
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color(0xFF2C2C2E),
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
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = if (isFocused) Color.White else Color(0xFFD1D1D6), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = if (isConnecting) "Testing Connection..." else "Test Connection Ping",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Verify port 8885 response and tuner availability",
                                color = TvTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Focusable Action 2: Change IP Address
                TvFocusableCard(
                    onClick = { viewModel.navigateTo(AppScreen.MANUAL_IP) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    focusedContainerColor = Color(0xFF26262A),
                    unfocusedContainerColor = Color(0xFF141416),
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color(0xFF2C2C2E),
                    testTag = "btn_change_ip"
                ) { isFocused ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = if (isFocused) Color.White else Color(0xFFD1D1D6), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Change IP Address",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Direct connection to Tablo on a specific subnet",
                                color = TvTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Focusable Action 3: Scan LAN for Tablos
                TvFocusableCard(
                    onClick = { viewModel.navigateTo(AppScreen.REGISTRATION) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    focusedContainerColor = Color(0xFF26262A),
                    unfocusedContainerColor = Color(0xFF141416),
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color(0xFF2C2C2E),
                    testTag = "btn_scan_lan"
                ) { isFocused ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = if (isFocused) Color.White else Color(0xFFD1D1D6), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Scan Local Network",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Find other Tablo Gen 4 tuners on Wi-Fi / Ethernet",
                                color = TvTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Focusable Action 4: Forget Device
                TvFocusableCard(
                    onClick = { viewModel.forgetDevice() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    focusedContainerColor = Color(0xFF26262A),
                    unfocusedContainerColor = Color(0xFF141416),
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color(0xFF2C2C2E),
                    testTag = "btn_forget_device"
                ) { isFocused ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = if (isFocused) TvError else Color(0xFF8E8E93), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Disconnect & Forget Device",
                                color = if (isFocused) TvError else Color(0xFFD1D1D6),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Clear stored IP and reset connection credentials",
                                color = TvTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Right Column: Technical Diagnostics Cards (Clean Minimalist Display)
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
                        .background(Color(0xFF141416))
                        .border(1.dp, Color(0xFF2C2C2E), RoundedCornerShape(12.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = "HARDWARE SPECIFICATIONS",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        DiagnosticRow(label = "IP Address", value = device?.host ?: "Not configured")
                        DiagnosticRow(label = "Streaming Port", value = "${device?.port ?: 8885}")
                        DiagnosticRow(label = "Physical Tuners", value = "${device?.tunerCount ?: 2} Tuners")
                        DiagnosticRow(label = "Firmware Build", value = device?.version?.ifBlank { "2.2.x" } ?: "Gen 4 Lighthouse")
                        DiagnosticRow(label = "Server ID", value = device?.serverId?.ifBlank { "N/A" } ?: "N/A")
                    }
                }

                // Streaming Engine Specs Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF141416))
                        .border(1.dp, Color(0xFF2C2C2E), RoundedCornerShape(12.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = "STREAMING ENGINE CONFIGURATION",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        DiagnosticRow(label = "HLS Stream Buffer", value = "15s - 30s Low-Jitter")
                        DiagnosticRow(label = "Initial Playback Pre-buffer", value = "2.5s Chunk Guard")
                        DiagnosticRow(label = "Live Target Offset", value = "6.0s Underrun Protected")
                        DiagnosticRow(label = "Hardware Video Codec", value = "Media3 Decoder Fallback Active")
                        DiagnosticRow(label = "Network Topology", value = "Direct LAN (Zero Cloud Latency)")
                    }
                }

                // Architecture & Privacy Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF141416))
                        .border(1.dp, Color(0xFF2C2C2E), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "100% self-contained local network streaming. No advertising tracking, no external relays, and zero telemetry.",
                        color = TvTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TvTextSecondary, fontSize = 12.sp)
        Text(text = value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
