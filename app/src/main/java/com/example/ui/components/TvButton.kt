package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TvAmberAccent
import com.example.ui.theme.TvBackground
import com.example.ui.theme.TvCyanPrimary
import com.example.ui.theme.TvSurfaceElevated
import com.example.ui.theme.TvTextPrimary

enum class TvButtonStyle {
    PRIMARY,
    SECONDARY,
    AMBER,
    OUTLINE
}

@Composable
fun TvButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: TvButtonStyle = TvButtonStyle.PRIMARY,
    enabled: Boolean = true,
    minWidth: Dp = 64.dp,
    minHeight: Dp = 44.dp,
    testTag: String = "tv_button",
    leadingIcon: (@Composable () -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isFocused) 1.04f else 1.0f, label = "btn_scale")

    val (bgColor, textColor, borderColor) = when (style) {
        TvButtonStyle.PRIMARY -> {
            if (isFocused) Triple(TvCyanPrimary, TvBackground, TvCyanPrimary)
            else Triple(TvCyanPrimary.copy(alpha = 0.88f), TvBackground, Color.Transparent)
        }
        TvButtonStyle.AMBER -> {
            if (isFocused) Triple(TvAmberAccent, TvBackground, TvAmberAccent)
            else Triple(TvAmberAccent.copy(alpha = 0.88f), TvBackground, Color.Transparent)
        }
        TvButtonStyle.SECONDARY -> {
            if (isFocused) Triple(TvSurfaceElevated, TvCyanPrimary, TvCyanPrimary)
            else Triple(TvSurfaceElevated.copy(alpha = 0.7f), TvTextPrimary, Color.Transparent)
        }
        TvButtonStyle.OUTLINE -> {
            if (isFocused) Triple(TvCyanPrimary.copy(alpha = 0.25f), TvCyanPrimary, TvCyanPrimary)
            else Triple(Color.Transparent, TvTextPrimary, TvTextPrimary.copy(alpha = 0.35f))
        }
    }

    val animatedBg by animateColorAsState(targetValue = bgColor, label = "btn_bg")
    val animatedText by animateColorAsState(targetValue = textColor, label = "btn_text")
    val animatedBorder by animateColorAsState(targetValue = borderColor, label = "btn_border")

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = animatedBg,
        border = BorderStroke(if (isFocused) 2.dp else 1.dp, animatedBorder),
        modifier = modifier
            .testTag(testTag)
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable(enabled = enabled)
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyUp &&
                    (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter)
                ) {
                    onClick()
                    true
                } else {
                    false
                }
            }
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .defaultMinSize(minWidth = minWidth, minHeight = minHeight)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                Box(modifier = Modifier.padding(end = 6.dp)) {
                    leadingIcon()
                }
            }
            Text(
                text = text,
                color = animatedText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
