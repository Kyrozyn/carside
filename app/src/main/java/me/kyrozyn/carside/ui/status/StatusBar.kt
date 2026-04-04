package me.kyrozyn.carside.ui.status

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.kyrozyn.carside.domain.model.CompassHeading
import me.kyrozyn.carside.ui.dashboard.WeatherUi

// Warna ikon kompas — amber/oranye seperti jarum kompas asli
private val CompassColor = Color(0xFFFFB300)

@Composable
fun LeftStatusBar(
    clockText: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Surface(
            modifier = Modifier.wrapContentWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
            ),
            tonalElevation = 2.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
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
                        style = MaterialTheme.typography.headlineLarge,
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
}

@Composable
fun RightStatusBar(
    compass: CompassHeading,
    weather: WeatherUi,
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
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompassSection(compass = compass)
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(36.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            )
            WeatherSection(weather = weather)
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
            modifier = Modifier.size(26.dp),
        )
        Spacer(Modifier.width(8.dp))
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
