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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
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
    val errorMessage by viewModel.deviceRepository.errorMessage.collectAsState()
    val isAuthenticating by viewModel.isAuthenticating.collectAsState()
    val isDiscovering = connectionState == TabloConnectionState.DISCOVERING

    var email by remember { mutableStateOf(viewModel.getSavedAuthEmail() ?: "") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(TvBackground)
            .padding(32.dp)
    ) {
        // Left Column: Tablo Account Sign In (Required for Gen 4)
        Column(
            modifier = Modifier
                .weight(1.15f)
                .padding(end = 28.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(TvSurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Tv,
                        contentDescription = "Tablo",
                        tint = TvCyanPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "TABLO 4TH GEN SETUP",
                        color = TvCyanPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Connect Your Tablo",
                        color = TvTextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Tablo 4th Gen DVRs require cloud authentication to link with your device, retrieve your channel guide, and authorize live streams over your local network.",
                color = TvTextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Email Input
            var emailFocused by remember { mutableStateOf(false) }
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    viewModel.deviceRepository.clearError()
                },
                label = { Text("Tablo Account Email", fontSize = 13.sp) },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = null, tint = if (emailFocused) TvCyanPrimary else TvTextTertiary)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TvTextPrimary,
                    unfocusedTextColor = TvTextPrimary,
                    focusedBorderColor = TvCyanPrimary,
                    unfocusedBorderColor = TvSurfaceVariant,
                    focusedContainerColor = TvSurfaceElevated,
                    unfocusedContainerColor = TvSurface,
                    focusedLabelColor = TvCyanPrimary,
                    unfocusedLabelColor = TvTextSecondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { emailFocused = it.isFocused }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Password Input
            var passFocused by remember { mutableStateOf(false) }
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    viewModel.deviceRepository.clearError()
                },
                label = { Text("Tablo Account Password", fontSize = 13.sp) },
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = if (passFocused) TvCyanPrimary else TvTextTertiary)
                },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle password visibility",
                            tint = TvTextTertiary
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (email.isNotBlank() && password.isNotBlank() && !isAuthenticating) {
                            viewModel.loginWithTabloAccount(email, password)
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TvTextPrimary,
                    unfocusedTextColor = TvTextPrimary,
                    focusedBorderColor = TvCyanPrimary,
                    unfocusedBorderColor = TvSurfaceVariant,
                    focusedContainerColor = TvSurfaceElevated,
                    unfocusedContainerColor = TvSurface,
                    focusedLabelColor = TvCyanPrimary,
                    unfocusedLabelColor = TvTextSecondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { passFocused = it.isFocused }
            )

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

            Spacer(modifier = Modifier.height(18.dp))

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TvButton(
                    text = if (isAuthenticating) "Signing In..." else "Sign In & Connect Tablo",
                    onClick = {
                        viewModel.loginWithTabloAccount(email, password)
                    },
                    style = TvButtonStyle.PRIMARY,
                    enabled = !isAuthenticating && email.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        if (isAuthenticating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = TvBackground
                            )
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null, tint = TvBackground)
                        }
                    },
                    testTag = "btn_sign_in"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TvButton(
                        text = "Manual IP",
                        onClick = { viewModel.navigateTo(AppScreen.MANUAL_IP) },
                        style = TvButtonStyle.OUTLINE,
                        modifier = Modifier.weight(1f),
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = TvCyanPrimary)
                        },
                        testTag = "btn_manual_ip"
                    )

                    TvButton(
                        text = if (isDiscovering) "Scanning..." else "Scan LAN",
                        onClick = { viewModel.startDiscovery() },
                        style = TvButtonStyle.SECONDARY,
                        enabled = !isDiscovering,
                        modifier = Modifier.weight(1f),
                        leadingIcon = {
                            if (isDiscovering) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = TvTextPrimary
                                )
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = TvTextPrimary)
                            }
                        },
                        testTag = "btn_scan_network"
                    )
                }
            }
        }

        // Right Column: Discovered Devices / Status
        Column(
            modifier = Modifier.weight(1.0f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AVAILABLE TABLO DEVICES",
                    color = TvCyanPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                if (isDiscovering || isAuthenticating) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = TvCyanPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAuthenticating) "Linking..." else "Scanning...",
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
                        .clip(RoundedCornerShape(16.dp))
                        .background(TvSurfaceElevated)
                        .border(1.dp, TvSurfaceVariant, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        if (isAuthenticating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = TvCyanPrimary
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Connecting to Tablo account...",
                                color = TvTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Retrieving linked Tablo Gen 4 tuners & keys",
                                color = TvTextSecondary,
                                fontSize = 12.sp
                            )
                        } else {
                            Icon(
                                Icons.Default.CastConnected,
                                contentDescription = null,
                                tint = TvTextTertiary,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "No Tablo Connected Yet",
                                color = TvTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Sign in with your Tablo account to automatically link your Gen 4 DVR, or use 'Manual IP' if you know your device's LAN address.",
                                color = TvTextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(discoveredDevices) { device ->
                        TvFocusableCard(
                            onClick = { viewModel.registerDiscoveredDevice(device) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(82.dp),
                            testTag = "discovered_tablo_${device.host}"
                        ) { isFocused ->
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f).padding(end = 12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(if (isFocused) TvCyanPrimary else TvSurfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Tv,
                                            contentDescription = null,
                                            tint = if (isFocused) TvBackground else TvCyanPrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = device.name,
                                            color = TvTextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${device.host} • ${device.modelName} (${device.tunerCount} tuners)",
                                            color = TvTextSecondary,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                TvButton(
                                    text = "Connect",
                                    onClick = { viewModel.registerDiscoveredDevice(device) },
                                    style = if (isFocused) TvButtonStyle.PRIMARY else TvButtonStyle.SECONDARY,
                                    modifier = Modifier.height(38.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
