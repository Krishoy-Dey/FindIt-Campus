package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val FindItDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = OnElectricCyan,
    primaryContainer = CyanContainer,
    onPrimaryContainer = OnCyanContainer,
    secondary = EmeraldMint,
    onSecondary = OnEmeraldMint,
    secondaryContainer = EmeraldContainer,
    onSecondaryContainer = OnEmeraldContainer,
    tertiary = WarmAmber,
    onTertiary = OnWarmAmber,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = OnAmberContainer,
    error = CoralRose,
    onError = OnCoralRose,
    errorContainer = RoseContainer,
    onErrorContainer = OnRoseContainer,
    background = ObsidianBg,
    onBackground = CrispWhite,
    surface = SlateSurface,
    onSurface = CrispWhite,
    surfaceVariant = SlateSurfaceElevated,
    onSurfaceVariant = HighContrastSlate,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = SlateSurface,
    surfaceContainer = SlateSurfaceElevated,
    surfaceContainerHigh = SlateSurfaceHigh,
    surfaceContainerHighest = SlateSurfaceHigh,
    outline = SlateOutline,
    outlineVariant = SlateOutlineSubtle
)

private val FindItShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun FindItCampusTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FindItDarkColorScheme,
        typography = Typography,
        shapes = FindItShapes,
        content = content
    )
}
