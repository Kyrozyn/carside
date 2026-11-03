package me.kyrozyn.carside.ui.status

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import me.kyrozyn.carside.domain.model.CompassHeading
import me.kyrozyn.carside.ui.dashboard.WeatherUi

private val CompassColor = Color(0xFFFFB300)
private val SpeedColor = Color(0xFF26A69A)

private const val SPEED_KMH_HIGHLIGHT = 90f

/** Satu bar penuh: jam lokal | shortcut aplikasi | cuaca (di bawah peta + pemutar media). */
@Composable
fun UnifiedDashboardStatusBar(
    clockText: String,
    /** Kecepatan dari GPS (m/s); null jika belum ada atau tidak tersedia. */
    gpsSpeedMps: Float?,
    compass: CompassHeading,
    shortcuts: List<String?>,
    weather: WeatherUi,
    onShortcutClick: (Int) -> Unit,
    onShortcutLongPress: (Int) -> Unit,
    onWeatherClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    ClockStatusPill(clockText = clockText)
                    Spacer(Modifier.width(10.dp))
                    StatusBarVerticalDivider()
                    Spacer(Modifier.width(10.dp))
                    GpsSpeedSection(speedMps = gpsSpeedMps)
                }
            }
            StatusBarVerticalDivider()
            Box(
                modifier = Modifier
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                ShortcutsSection(
                    shortcuts = shortcuts,
                    onShortcutClick = onShortcutClick,
                    onShortcutLongPress = onShortcutLongPress,
                )
            }
            StatusBarVerticalDivider()
            Box(
                modifier = Modifier
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CompassSection(compass = compass)
                    Spacer(Modifier.width(10.dp))
                    StatusBarVerticalDivider()
                    Spacer(Modifier.width(10.dp))
                    Box(modifier = Modifier.clickable(onClick = onWeatherClick)) {
                        WeatherSection(weather = weather)
                    }
                }
            }
        }
    }
}
@Composable
private fun GpsSpeedSection(speedMps: Float?) {
    val kmh = speedMps?.let { it * 3.6f }
    val overLimit = kmh != null && kmh > SPEED_KMH_HIGHLIGHT
    val iconTint = if (overLimit) MaterialTheme.colorScheme.error else SpeedColor
    val valueColor = if (overLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val labelColor = if (overLimit) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.75f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Outlined.Speed,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(6.dp))
        Column {
            Text(
                text = if (kmh != null) "${kmh.toInt()} km/h" else "—",
                style = MaterialTheme.typography.titleMedium,
                color = valueColor,
                maxLines = 1,
            )
            Text(
                text = "Kecepatan GPS",
                style = MaterialTheme.typography.labelMedium,
                color = labelColor,
            )
        }
    }
}

@Composable
private fun CompassSection(compass: CompassHeading) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Outlined.Explore,
            contentDescription = null,
            tint = CompassColor,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(6.dp))
        Column {
            Text(
                text = compass.cardinal,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Text(
                text = "${compass.degrees.toInt()}°",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            )
        }
    }
}


@Composable
private fun StatusBarVerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(36.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
    )
}

@Composable
private fun ClockStatusPill(clockText: String) {
    Surface(
        modifier = Modifier.wrapContentWidth(),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
        ),
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Outlined.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Column {
                Text(
                    text = clockText,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Waktu lokal",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                )
            }
        }
    }
}

@Composable
private fun ShortcutsSection(
    shortcuts: List<String?>,
    onShortcutClick: (Int) -> Unit,
    onShortcutLongPress: (Int) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(4) { index ->
            ShortcutItem(
                packageName = shortcuts.getOrNull(index),
                onClick = { onShortcutClick(index) },
                onLongClick = { onShortcutLongPress(index) },
            )
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun ShortcutItem(
    packageName: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val context = LocalContext.current
    val appIcon = remember(packageName) {
        if (packageName.isNullOrBlank()) return@remember null
        runCatching {
            context.packageManager.getApplicationIcon(packageName).toBitmap(96, 96).asImageBitmap()
        }.getOrNull()
    }

    Surface(
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .padding(6.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (appIcon != null) {
                Image(bitmap = appIcon, contentDescription = null, modifier = Modifier.size(30.dp))
            } else {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = "Pilih aplikasi shortcut",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun WeatherSection(weather: WeatherUi) {
    when (weather) {
        WeatherUi.Idle -> WeatherItem(emoji = "🌐", title = "—", subtitle = "Menunggu…")
        WeatherUi.Loading -> WeatherItem(emoji = "⏳", title = "—", subtitle = "Memuat…")
        is WeatherUi.Ready -> WeatherItem(
            emoji = weatherEmoji(weather.summary.weatherCode),
            title = "${weather.summary.temperatureCelsius.toInt()}°C",
            subtitle = weather.summary.description,
        )
        is WeatherUi.Error -> WeatherItem(emoji = "❓", title = "—", subtitle = weather.message)
    }
}

@Composable
private fun WeatherItem(emoji: String, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = emoji,
            fontSize = 26.sp,
        )
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            )
        }
    }
}

private fun weatherEmoji(code: Int): String = when (code) {
    0            -> "☀️"   // Cerah
    1            -> "🌤️"  // Sebagian cerah
    2            -> "⛅"   // Berawan sebagian
    3            -> "☁️"   // Mendung penuh
    45, 48       -> "🌫️"  // Kabut
    51, 53, 55   -> "🌦️"  // Gerimis ringan
    56, 57       -> "🌧️"  // Gerimis beku
    61, 63, 65   -> "🌧️"  // Hujan
    66, 67       -> "🌨️"  // Hujan beku
    71, 73, 75   -> "❄️"  // Salju
    77           -> "🌨️"  // Butiran salju
    80, 81, 82   -> "🌦️"  // Hujan deras sesaat
    85, 86       -> "🌨️"  // Hujan salju
    95           -> "⛈️"  // Badai petir
    96, 99       -> "⛈️"  // Badai petir + hujan es
    else         -> "🌡️"
}
