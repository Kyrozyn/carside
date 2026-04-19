package me.kyrozyn.carside.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// Palette enum
// ---------------------------------------------------------------------------

enum class AppThemePalette(
    val displayName: String,
    val isDark: Boolean,
    /** Representative color shown as preview swatch in UI */
    val previewColor: Color,
) {
    // ── Dark palettes ───────────────────────────────────────────────────────
    DARK_BLUE(
        displayName = "Biru Malam",
        isDark = true,
        previewColor = Color(0xFF4FC3F7),
    ),
    DARK_RED(
        displayName = "Merah Api",
        isDark = true,
        previewColor = Color(0xFFEF5350),
    ),
    DARK_GREEN(
        displayName = "Hijau Neon",
        isDark = true,
        previewColor = Color(0xFF66BB6A),
    ),
    DARK_PURPLE(
        displayName = "Ungu Cyber",
        isDark = true,
        previewColor = Color(0xFFCE93D8),
    ),

    // ── Light palettes ──────────────────────────────────────────────────────
    LIGHT_WHITE(
        displayName = "Putih Bersih",
        isDark = false,
        previewColor = Color(0xFF1565C0),
    ),
    LIGHT_AMBER(
        displayName = "Amber Siang",
        isDark = false,
        previewColor = Color(0xFFE65100),
    ),
    LIGHT_GREEN(
        displayName = "Hijau Alam",
        isDark = false,
        previewColor = Color(0xFF2E7D32),
    ),
}

// ---------------------------------------------------------------------------
// Color schemes
// ---------------------------------------------------------------------------

// ── Dark: Biru Malam (original head-unit blue) ──────────────────────────────
private val DarkBlueScheme = darkColorScheme(
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

// ── Dark: Merah Api ──────────────────────────────────────────────────────────
private val DarkRedScheme = darkColorScheme(
    primary = Color(0xFFEF5350),
    onPrimary = Color(0xFF1A0000),
    primaryContainer = Color(0xFF3B0000),
    onPrimaryContainer = Color(0xFFFFDAD6),

    secondary = Color(0xFFFFCC80),
    onSecondary = Color(0xFF2A1200),
    secondaryContainer = Color(0xFF3D1C00),
    onSecondaryContainer = Color(0xFFFFDDB5),

    tertiary = Color(0xFF8D6E63),
    onTertiary = Color(0xFF2A1209),

    background = Color(0xFF130707),
    onBackground = Color(0xFFF5E9E9),

    surface = Color(0xFF1E0A0A),
    onSurface = Color(0xFFF5E9E9),
    surfaceVariant = Color(0xFF2E1010),
    onSurfaceVariant = Color(0xFFD4B8B8),

    outline = Color(0xFF4A2020),
    outlineVariant = Color(0xFF3A1515),

    error = Color(0xFFFF8A80),
    onError = Color(0xFF690005),
)

// ── Dark: Hijau Neon ─────────────────────────────────────────────────────────
private val DarkGreenScheme = darkColorScheme(
    primary = Color(0xFF66BB6A),
    onPrimary = Color(0xFF00200A),
    primaryContainer = Color(0xFF003912),
    onPrimaryContainer = Color(0xFFB4F0B7),

    secondary = Color(0xFFA5D6A7),
    onSecondary = Color(0xFF002108),
    secondaryContainer = Color(0xFF003011),
    onSecondaryContainer = Color(0xFFC8F0CA),

    tertiary = Color(0xFF4DB6AC),
    onTertiary = Color(0xFF001F1E),

    background = Color(0xFF060E07),
    onBackground = Color(0xFFE6F4E7),

    surface = Color(0xFF091409),
    onSurface = Color(0xFFE6F4E7),
    surfaceVariant = Color(0xFF0F2010),
    onSurfaceVariant = Color(0xFFB4CCB5),

    outline = Color(0xFF1E3D1E),
    outlineVariant = Color(0xFF152D15),

    error = Color(0xFFFF5252),
    onError = Color(0xFF690005),
)

// ── Dark: Ungu Cyber ─────────────────────────────────────────────────────────
private val DarkPurpleScheme = darkColorScheme(
    primary = Color(0xFFCE93D8),
    onPrimary = Color(0xFF1A0028),
    primaryContainer = Color(0xFF2E004D),
    onPrimaryContainer = Color(0xFFF4D0FA),

    secondary = Color(0xFFB39DDB),
    onSecondary = Color(0xFF14003A),
    secondaryContainer = Color(0xFF260060),
    onSecondaryContainer = Color(0xFFE9DCFF),

    tertiary = Color(0xFF80DEEA),
    onTertiary = Color(0xFF00202A),

    background = Color(0xFF0D0714),
    onBackground = Color(0xFFF0EAF5),

    surface = Color(0xFF130A1E),
    onSurface = Color(0xFFF0EAF5),
    surfaceVariant = Color(0xFF1E0D30),
    onSurfaceVariant = Color(0xFFCCB8D4),

    outline = Color(0xFF3A1A50),
    outlineVariant = Color(0xFF2A1040),

    error = Color(0xFFFF7DBD),
    onError = Color(0xFF690045),
)

// ── Light: Putih Bersih ──────────────────────────────────────────────────────
private val LightWhiteScheme = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E4FF),
    onPrimaryContainer = Color(0xFF001944),

    secondary = Color(0xFF1976D2),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDBEAFF),
    onSecondaryContainer = Color(0xFF001C3D),

    tertiary = Color(0xFF546E7A),
    onTertiary = Color(0xFFFFFFFF),

    background = Color(0xFFF4F7FA),
    onBackground = Color(0xFF0D1B2A),

    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0D1B2A),
    surfaceVariant = Color(0xFFE8EDF3),
    onSurfaceVariant = Color(0xFF354050),

    outline = Color(0xFFBDC7D2),
    outlineVariant = Color(0xFFD5DDE6),

    error = Color(0xFFB00020),
    onError = Color(0xFFFFFFFF),
)

// ── Light: Amber Siang ───────────────────────────────────────────────────────
private val LightAmberScheme = lightColorScheme(
    primary = Color(0xFFE65100),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD9CB),
    onPrimaryContainer = Color(0xFF3A0E00),

    secondary = Color(0xFFF57C00),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFE5C4),
    onSecondaryContainer = Color(0xFF2C1A00),

    tertiary = Color(0xFF6D7C3A),
    onTertiary = Color(0xFFFFFFFF),

    background = Color(0xFFFFF8F4),
    onBackground = Color(0xFF1E1108),

    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1E1108),
    surfaceVariant = Color(0xFFF5EDE6),
    onSurfaceVariant = Color(0xFF403126),

    outline = Color(0xFFD4B8A8),
    outlineVariant = Color(0xFFE5D3C8),

    error = Color(0xFFB00020),
    onError = Color(0xFFFFFFFF),
)

// ── Light: Hijau Alam ────────────────────────────────────────────────────────
private val LightGreenScheme = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC8EFC9),
    onPrimaryContainer = Color(0xFF002107),

    secondary = Color(0xFF388E3C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD4F2D4),
    onSecondaryContainer = Color(0xFF00200A),

    tertiary = Color(0xFF558B2F),
    onTertiary = Color(0xFFFFFFFF),

    background = Color(0xFFF4FAF4),
    onBackground = Color(0xFF081408),

    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF081408),
    surfaceVariant = Color(0xFFE6F0E6),
    onSurfaceVariant = Color(0xFF2A3C2A),

    outline = Color(0xFFB2CEB2),
    outlineVariant = Color(0xFFCCDECC),

    error = Color(0xFFB00020),
    onError = Color(0xFFFFFFFF),
)

// ---------------------------------------------------------------------------
// Mapping
// ---------------------------------------------------------------------------

fun AppThemePalette.toColorScheme(): ColorScheme = when (this) {
    AppThemePalette.DARK_BLUE   -> DarkBlueScheme
    AppThemePalette.DARK_RED    -> DarkRedScheme
    AppThemePalette.DARK_GREEN  -> DarkGreenScheme
    AppThemePalette.DARK_PURPLE -> DarkPurpleScheme
    AppThemePalette.LIGHT_WHITE -> LightWhiteScheme
    AppThemePalette.LIGHT_AMBER -> LightAmberScheme
    AppThemePalette.LIGHT_GREEN -> LightGreenScheme
}
