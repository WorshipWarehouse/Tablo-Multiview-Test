package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TvDarkColorScheme = darkColorScheme(
    primary = TvCyanPrimary,
    onPrimary = TvBackground,
    primaryContainer = TvSurfaceElevated,
    onPrimaryContainer = TvCyanLight,
    secondary = TvAmberAccent,
    onSecondary = TvBackground,
    secondaryContainer = TvSurfaceElevated,
    onSecondaryContainer = TvAmberLight,
    background = TvBackground,
    onBackground = TvTextPrimary,
    surface = TvSurface,
    onSurface = TvTextPrimary,
    surfaceVariant = TvSurfaceVariant,
    onSurfaceVariant = TvTextSecondary,
    error = TvError,
    onError = TvBackground
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TvDarkColorScheme,
        typography = Typography,
        content = content
    )
}
