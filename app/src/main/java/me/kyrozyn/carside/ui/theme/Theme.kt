package me.kyrozyn.carside.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun CarsideTheme(
    palette: AppThemePalette = AppThemePalette.DARK_BLUE,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = palette.toColorScheme(),
        typography = Typography,
        content = content,
    )
}
