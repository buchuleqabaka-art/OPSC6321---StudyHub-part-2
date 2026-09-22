package com.studyhub.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = LilacPrimary,
    onPrimary = White,
    primaryContainer = LilacSurface,
    onPrimaryContainer = Ink,
    secondary = LilacPrimaryDark,
    background = LilacBackground,
    onBackground = Ink,
    surface = LilacSurface,
    onSurface = Ink,
    surfaceVariant = CardGrey,
    onSurfaceVariant = Ink,
    error = PriorityHigh,
)

private val DarkColors = darkColorScheme(
    primary = LilacPrimary,
    onPrimary = Ink,
    primaryContainer = DarkSurface,
    onPrimaryContainer = White,
    secondary = LilacPrimaryDark,
    background = DarkBackground,
    onBackground = White,
    surface = DarkSurface,
    onSurface = White,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = White
)

@Composable
fun StudyHubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = StudyHubTypography,
        content = content
    )
}