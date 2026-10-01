package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TerminalDarkScheme = darkColorScheme(
    primary = SmaGolden,
    onPrimary = Color.Black,
    primaryContainer = SmaGoldenDark,
    onPrimaryContainer = Color.White,
    secondary = BullGreen,
    onSecondary = Color.Black,
    secondaryContainer = BullGreenBg,
    onSecondaryContainer = BullGreenLight,
    tertiary = IndicatorCyan,
    onTertiary = Color.Black,
    background = TerminalBackground,
    onBackground = TextPrimary,
    surface = TerminalSurface,
    onSurface = TextPrimary,
    surfaceVariant = TerminalSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = TerminalBorder,
    outlineVariant = Color(0xFF1E293B),
    error = BearRed,
    onError = Color.White,
    errorContainer = BearRedBg,
    onErrorContainer = BearRedLight
)

@Composable
fun Sma44ScannerTheme(
    darkTheme: Boolean = true, // Force dark mode for authentic trading terminal feel
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TerminalDarkScheme,
        typography = Typography,
        content = content
    )
}
