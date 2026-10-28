package com.example.myapplication.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LoopiColorScheme = lightColorScheme(
    primary = LoopiColors.Blue,
    onPrimary = Color.White,
    primaryContainer = LoopiColors.BlueSoft,
    onPrimaryContainer = LoopiColors.Navy,
    secondary = LoopiColors.Navy,
    onSecondary = Color.White,
    secondaryContainer = LoopiColors.BlueSoft,
    onSecondaryContainer = LoopiColors.Navy,
    tertiary = LoopiColors.Teal,
    onTertiary = Color.White,
    tertiaryContainer = LoopiColors.TealSoft,
    error = LoopiColors.Red,
    errorContainer = LoopiColors.RedSoft,
    background = LoopiColors.Background,
    onBackground = LoopiColors.Ink,
    surface = LoopiColors.Surface,
    onSurface = LoopiColors.Ink,
    surfaceVariant = LoopiColors.Track,
    onSurfaceVariant = LoopiColors.Muted,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = LoopiColors.Background,
    surfaceContainerHighest = LoopiColors.Track,
    outline = LoopiColors.Border,
    outlineVariant = LoopiColors.Border
)

private val LoopiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// Fixed brand theme: dynamic colour is off so wallpaper colours never replace Loopi's palette.
@Composable
fun LoopiTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LoopiColorScheme,
        typography = Typography,
        shapes = LoopiShapes,
        content = content
    )
}
