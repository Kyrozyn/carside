package me.kyrozyn.carside.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HeadUnitDark = darkColorScheme(
    primary = Color(0xFF4FC3F7),
    onPrimary = Color(0xFF001F2E),
    primaryContainer = Color(0xFF003547),
    onPrimaryContainer = Color(0xFFB8EAFF),

    secondary = Color(0xFF90CAF9),
    onSecondary = Color(0xFF001E30),
    secondaryContainer = Color(0xFF002945),
    onSecondaryContainer = Color(0xFFCBE6FF),

    tertiary = Color(0xFF78909C),
    onTertiary = Color(0xFF1A2C35),

    background = Color(0xFF080C14),
    onBackground = Color(0xFFE8EEF4),

    surface = Color(0xFF0F1624),
    onSurface = Color(0xFFE8EEF4),
    surfaceVariant = Color(0xFF162030),
    onSurfaceVariant = Color(0xFFB0C4D4),

    outline = Color(0xFF2A3A4A),
    outlineVariant = Color(0xFF1E2D3D),

    error = Color(0xFFFF5252),
    onError = Color(0xFF690005),
)

@Composable
fun CarsideTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HeadUnitDark,
        typography = Typography,
        content = content,
    )
}
