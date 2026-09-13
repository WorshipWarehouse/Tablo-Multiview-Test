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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TabloConnectionState
import com.example.ui.components.TvButton
import com.example.ui.components.TvButtonStyle
import com.example.ui.components.TvFocusableCard
import com.example.ui.theme.TvBackground
import com.example.ui.theme.TvCyanPrimary
import com.example.ui.theme.TvError
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvSurfaceElevated
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.TabloAppViewModel

@Composable
fun ManualIpScreen(
    viewModel: TabloAppViewModel,
    modifier: Modifier = Modifier
) {
    val connectionState by viewModel.deviceRepository.connectionState.collectAsState()
    val errorMessage by viewModel.deviceRepository.errorMessage.collectAsState()
    val isConnecting = connectionState == TabloConnectionState.CONNECTING

    var ipAddress by remember { mutableStateOf("192.168.1.") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TvBackground)
            .padding(36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TvButton(
                text = "Back",
                onClick = { viewModel.navigateTo(AppScreen.REGISTRATION) },
                style = TvButtonStyle.OUTLINE,
                leadingIcon = {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TvCyanPrimary
                    )
                },
                testTag = "btn_manual_back"
            )
            Spacer(modifier = Modifier.width(24.dp))
            Column {
                Text(
                    text = "MANUAL TABLO IP ADDRESS",
                    color = TvTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Enter your Tablo's local IP address (port 8885 will be used automatically)",
                    color = TvTextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 16:9 Landscape Two-Column Layout
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column: IP Entry Box & Action Controls
            Column(
                modifier = Modifier.weight(1.1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "TARGET IP ADDRESS",
                    color = TvCyanPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // IP Display Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(TvSurfaceElevated),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = ipAddress.ifBlank { "192.168.1.xxx" },
                        color = if (ipAddress.isNotBlank()) TvCyanPrimary else TvTextSecondary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = TvError,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick IP Helpers & Clear
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TvButton(
                        text = "192.168.1.",
                        onClick = { ipAddress = "192.168.1." },
                        style = TvButtonStyle.OUTLINE,
                        modifier = Modifier.weight(1f)
                    )
                    TvButton(
                        text = "10.0.0.",
                        onClick = { ipAddress = "10.0.0." },
                        style = TvButtonStyle.OUTLINE,
                        modifier = Modifier.weight(1f)
                    )
                    TvButton(
                        text = "Clear",
                        onClick = { ipAddress = "" },
                        style = TvButtonStyle.SECONDARY,
                        modifier = Modifier.weight(0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Connect Action Button
                TvButton(
                    text = if (isConnecting) "Connecting to Tablo..." else "Connect to Tablo",
                    onClick = {
                        viewModel.connectManualIp(ipAddress)
                    },
                    style = TvButtonStyle.PRIMARY,
                    enabled = !isConnecting && ipAddress.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        if (isConnecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = TvBackground,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null, tint = TvBackground)
                        }
                    },
                    testTag = "btn_manual_connect"
                )
            }

            // Right Column: Remote-Friendly TV Keypad Grid
            Column(
                modifier = Modifier.weight(0.9f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "D-PAD NUMBER PAD",
                    color = TvTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                val keypadRows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf(".", "0", "DEL")
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    keypadRows.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { key ->
                                TvFocusableCard(
                                    onClick = {
                                        viewModel.deviceRepository.clearError()
                                        when (key) {
                                            "DEL" -> {
                                                if (ipAddress.isNotEmpty()) {
                                                    ipAddress = ipAddress.dropLast(1)
                                                }
                                            }
                                            else -> {
                                                if (ipAddress.length < 15) {
                                                    ipAddress += key
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .width(84.dp)
                                        .height(48.dp),
                                    testTag = "keypad_$key"
                                ) { isFocused ->
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (key == "DEL") {
                                            Icon(
                                                Icons.Default.Backspace,
                                                contentDescription = "Delete",
                                                tint = if (isFocused) TvCyanPrimary else TvTextSecondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        } else {
                                            Text(
                                                text = key,
                                                color = if (isFocused) TvCyanPrimary else TvTextPrimary,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
