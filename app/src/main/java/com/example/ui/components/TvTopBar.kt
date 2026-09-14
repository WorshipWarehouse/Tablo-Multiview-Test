package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.TabloConnectionState
import com.example.model.TabloDevice
import com.example.ui.theme.TvBackground
import com.example.ui.theme.TvBorderNormal
import com.example.ui.theme.TvCyanGlow
import com.example.ui.theme.TvCyanPrimary
import com.example.ui.theme.TvError
import com.example.ui.theme.TvSuccess
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
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = if (onNavigateHome != null) Modifier.clickable { onNavigateHome() } else Modifier
        ) {
            // Brand Retro-Modern TV Icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, TvCyanPrimary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_app_brand_logo),
                    contentDescription = "Tablo Multiview Logo",
                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(7.dp))
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "TABLO",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = " MULTIVIEW",
                color = TvCyanPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            if (title.isNotBlank()) {
                Text(
                    text = "  /  $title",
                    color = TvTextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }

        // Connection Status Pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF141720))
                .border(1.dp, TvBorderNormal, RoundedCornerShape(20.dp))
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
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
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
