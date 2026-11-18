package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = IndigoPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = IndigoPrimaryContainerLight,
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = CoralSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = CoralSecondaryContainerLight,
    onSecondaryContainer = Color(0xFF4C0519),
    tertiary = CyanTertiaryLight,
    onTertiary = Color.White,
    tertiaryContainer = CyanTertiaryContainerLight,
    onTertiaryContainer = Color(0xFF083344),
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceContainerLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceContainerHighLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    error = RoseError,
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = IndigoPrimaryDark,
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = IndigoPrimaryContainerDark,
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = CoralSecondaryDark,
    onSecondary = Color(0xFF4C0519),
    secondaryContainer = CoralSecondaryContainerDark,
    onSecondaryContainer = Color(0xFFFFE4E6),
    tertiary = CyanTertiaryDark,
    onTertiary = Color(0xFF083344),
    tertiaryContainer = CyanTertiaryContainerDark,
    onTertiaryContainer = Color(0xFFCFFAFE),
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceContainerDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceContainerHighDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    error = RoseError,
    onError = Color.White
)

val PulseStreamShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun PulseStreamTheme(
    themeMode: String = "system",
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = PulseStreamTypography,
        shapes = PulseStreamShapes,
        content = content
    )
}
