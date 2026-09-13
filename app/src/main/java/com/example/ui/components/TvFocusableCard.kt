package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.TvBorderFocused
import com.example.ui.theme.TvBorderNormal
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvSurfaceElevated

/**
 * A remote-friendly focusable card for Fire TV / Android TV.
 * Scales up slightly and highlights border when selected by D-pad.
 */
@Composable
fun TvFocusableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp),
    focusedBorderColor: Color = TvBorderFocused,
    unfocusedBorderColor: Color = TvBorderNormal,
    focusedContainerColor: Color = TvSurfaceElevated,
    unfocusedContainerColor: Color = TvSurface,
    borderWidth: Dp = 2.dp,
    testTag: String = "tv_card",
    content: @Composable BoxScope.(isFocused: Boolean) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isFocused) 1.03f else 1.0f, label = "card_scale")
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) focusedBorderColor else unfocusedBorderColor,
        label = "card_border"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isFocused) focusedContainerColor else unfocusedContainerColor,
        label = "card_container"
    )

    Surface(
        shape = shape,
        color = containerColor,
        border = BorderStroke(if (isFocused) borderWidth else 1.dp, borderColor),
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
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
    ) {
        Box {
            content(isFocused)
        }
    }
}
