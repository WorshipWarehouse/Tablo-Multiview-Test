package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TabloConnectionState
import com.example.model.TabloDevice
import com.example.ui.theme.TvBackground
import com.example.ui.theme.TvCyanPrimary
import com.example.ui.theme.TvError
import com.example.ui.theme.TvSuccess
import com.example.ui.theme.TvSurfaceVariant
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary
import com.example.ui.theme.TvWarning

@Composable
fun TvTopBar(
    title: String,
    device: TabloDevice?,
    connectionState: TabloConnectionState,
    modifier: Modifier = Modifier,
    onNavigateHome: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(TvBackground)
            .padding(horizontal = 32.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "TABLO MULTIVIEW",
                color = TvCyanPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            if (title.isNotBlank()) {
                Text(
                    text = "  /  $title",
                    color = TvTextSecondary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Connection Status Pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(TvSurfaceVariant.copy(alpha = 0.7f))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            val (statusColor, statusText) = when (connectionState) {
                TabloConnectionState.CONNECTED -> Pair(TvSuccess, device?.name ?: "Connected")
                TabloConnectionState.CONNECTING -> Pair(TvWarning, "Connecting...")
                TabloConnectionState.DISCOVERING -> Pair(TvCyanPrimary, "Discovering...")
                TabloConnectionState.ERROR -> Pair(TvError, "Disconnected")
                TabloConnectionState.DISCONNECTED -> Pair(TvTextSecondary, "Not Connected")
            }

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )

            Text(
                text = "  $statusText",
                color = TvTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            if (device != null && connectionState == TabloConnectionState.CONNECTED) {
                Text(
                    text = " (${device.host})",
                    color = TvTextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}
