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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TabloConnectionState
import com.example.ui.components.TvButton
import com.example.ui.components.TvButtonStyle
import com.example.ui.theme.TvAmberAccent
import com.example.ui.theme.TvBackground
import com.example.ui.theme.TvCyanPrimary
import com.example.ui.theme.TvError
import com.example.ui.theme.TvSuccess
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvSurfaceElevated
import com.example.ui.theme.TvSurfaceVariant
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary
import com.example.ui.theme.TvTextTertiary
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
                        tint = TvCyanPrimary
                    )
                },
                testTag = "btn_settings_back"
            )
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(
                    text = "SETTINGS & DIAGNOSTICS",
                    color = TvTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Device specs, local network diagnostics, and configuration",
                    color = TvTextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Left Column: Tablo Device Specs Card
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(TvSurface)
                    .padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(TvSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Tv, contentDescription = null, tint = TvCyanPrimary, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = device?.name ?: "No Tablo Connected",
                                color = TvTextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = device?.modelName ?: "Tablo Gen 4",
                                color = TvCyanPrimary,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Status Pill
                    val (statusColor, statusText) = when (connectionState) {
                        TabloConnectionState.CONNECTED -> Pair(TvSuccess, "Connected")
                        TabloConnectionState.CONNECTING -> Pair(TvWarning, "Connecting...")
                        TabloConnectionState.DISCOVERING -> Pair(TvCyanPrimary, "Discovering...")
                        TabloConnectionState.ERROR -> Pair(TvError, "Disconnected")
                        TabloConnectionState.DISCONNECTED -> Pair(TvTextSecondary, "Not Connected")
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(TvSurfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = statusText, color = TvTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Detail Specs Rows
                DiagnosticRow(label = "Local IP Address", value = device?.host ?: "Not assigned")
                DiagnosticRow(label = "HTTP Port", value = "${device?.port ?: 8885}")
                DiagnosticRow(label = "Hardware Tuners", value = "${device?.tunerCount ?: 2} Tuners")
                DiagnosticRow(label = "Firmware Version", value = device?.version?.ifBlank { "2.2.x" } ?: "Unknown")
                DiagnosticRow(label = "Timezone", value = device?.timezone?.ifBlank { "Local" } ?: "Local")
                DiagnosticRow(label = "Server ID", value = device?.serverId?.ifBlank { "N/A" } ?: "N/A")

                Spacer(modifier = Modifier.weight(1f))

                // Action Buttons Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TvButton(
                        text = if (isConnecting) "Testing..." else "Test Ping",
                        onClick = { viewModel.retryConnection() },
                        style = TvButtonStyle.PRIMARY,
                        enabled = !isConnecting,
                        modifier = Modifier.weight(1f),
                        leadingIcon = {
                            if (isConnecting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = TvBackground, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = TvBackground)
                            }
                        }
                    )

                    TvButton(
                        text = "Change IP",
                        onClick = { viewModel.navigateTo(AppScreen.MANUAL_IP) },
                        style = TvButtonStyle.SECONDARY,
                        modifier = Modifier.weight(1f),
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = TvCyanPrimary) }
                    )
                }
            }

            // Right Column: Network & App Architecture
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(TvSurface)
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "NETWORK & ARCHITECTURE",
                        color = TvCyanPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    DiagnosticRow(label = "Communication", value = "Direct LAN Stream (Port 8885)")
                    DiagnosticRow(label = "Tablo Account", value = viewModel.getSavedAuthEmail() ?: "Direct LAN Mode")
                    DiagnosticRow(label = "Custom Backend", value = "None (Self-Contained)")
                    DiagnosticRow(label = "Video Engine", value = "Media3 ExoPlayer 1.5.1")
                    DiagnosticRow(label = "Hardware Decoder", value = "Active (Fire TV)")
                    DiagnosticRow(label = "Local Storage", value = "Room SQLite Database")

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Video streams flow directly from your Tablo Gen 4 over your local Wi-Fi / Ethernet connection. No video telemetry or advertising trackers are used.",
                        color = TvTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TvButton(
                        text = "Scan for Other Tablos",
                        onClick = { viewModel.navigateTo(AppScreen.REGISTRATION) },
                        style = TvButtonStyle.OUTLINE,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TvCyanPrimary) }
                    )

                    TvButton(
                        text = "Forget This Tablo",
                        onClick = { viewModel.forgetDevice() },
                        style = TvButtonStyle.OUTLINE,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = TvError) }
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
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TvTextSecondary, fontSize = 13.sp)
        Text(text = value, color = TvTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
