package me.kyrozyn.carside.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.kyrozyn.carside.data.navigation.AutoNavigationManager
import me.kyrozyn.carside.domain.model.ActiveOfflineDownload
import me.kyrozyn.carside.domain.model.InternetRadioState
import me.kyrozyn.carside.ui.apps.InstalledAppsLoader
import me.kyrozyn.carside.ui.apps.LaunchableApp
import me.kyrozyn.carside.ui.internetradio.InternetRadioMiniPanel
import me.kyrozyn.carside.ui.map.AutoMapPane
import me.kyrozyn.carside.ui.map.MapPane
import me.kyrozyn.carside.ui.media.LyricsFullscreenScreen
import me.kyrozyn.carside.ui.media.MediaPlayerPanel
import me.kyrozyn.carside.ui.navigation.NavigationPanel
import me.kyrozyn.carside.ui.status.UnifiedDashboardStatusBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    notificationListenerEnabled: Boolean,
    onBrowseClick: () -> Unit,
    onDismissBrowse: () -> Unit,
    onOpenNotificationAccessSettings: () -> Unit,
    onRefreshSystemMedia: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onOpenBluetoothMusic: () -> Unit = {},
    onOpenAppDrawer: () -> Unit,
    onOpenInternetRadio: () -> Unit,
    onOpenSettings: () -> Unit,
    internetRadioState: InternetRadioState,
    onStopInternetRadio: () -> Unit,
    onSetUseGpsForwardMapBearing: (Boolean) -> Unit,
    onLaunchShortcutApp: (String) -> Unit,
    onOpenShortcutPicker: (Int) -> Unit,
    onDismissShortcutPicker: () -> Unit,
    onSelectShortcutApp: (slot: Int, packageName: String?) -> Unit,
    onOpenWeatherDetails: () -> Unit,
    onCloseWeatherDetails: () -> Unit,
    /** Unduhan peta offline aktif (background); banner tipis di atas peta. */
    offlineDownloadActive: ActiveOfflineDownload? = null,
    onOpenGarage: () -> Unit = {},
    onOpenLyricsFullscreen: () -> Unit = {},
    onCloseLyricsFullscreen: () -> Unit = {},
    hasOfflineMaps: Boolean = false,
    locationDotSize: Float = 60f,
    locationRefreshIntervalMs: Long = 60_000L,
    autoNavigationManager: AutoNavigationManager? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Scaffold memberi slot konten tinggi pasti; Column+weight pada Row sering kolaps (peta/media tinggi 0).
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                UnifiedDashboardStatusBar(
                    clockText = uiState.clockText,
                    gpsSpeedMps = uiState.userLocation?.speedMps,
                    compass = uiState.compass,
                    shortcuts = uiState.shortcutPackages,
                    weather = uiState.weather,
                    onShortcutClick = { slot ->
                        val pkg = uiState.shortcutPackages.getOrNull(slot)
                        if (pkg.isNullOrBlank()) onOpenShortcutPicker(slot) else onLaunchShortcutApp(pkg)
                    },
                    onShortcutLongPress = onOpenShortcutPicker,
                    onWeatherClick = onOpenWeatherDetails,
                )
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
            ) {
                Box(
                    modifier = Modifier
                        .weight(0.62f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface),
                ) {
                    var showMapOverride by remember { mutableStateOf(false) }
                    LaunchedEffect(uiState.navigation.active) {
                        if (!uiState.navigation.active) showMapOverride = false
                    }

                    val showNavPanel = uiState.navigation.active && !showMapOverride
                    if (showNavPanel) {
                        NavigationPanel(
                            state = uiState.navigation,
                            onOpenSourceApp = {
                                val pkg = uiState.navigation.sourceApp.packageName
                                if (pkg.isNotBlank()) onLaunchShortcutApp(pkg)
                            },
                            onSwitchToMap = { showMapOverride = true },
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else if (uiState.autoModeEnabled) {
                        AutoMapPane(
                            hasLocationPermission = uiState.locationPermissionGranted,
                            userLocation = uiState.userLocation,
                            bearing = uiState.mapBearingDegrees,
                            apiKey = uiState.googleMapsApiKey,
                            autoNavigationManager = autoNavigationManager,
                            onOpenSettings = onOpenSettings,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        MapPane(
                            hasLocationPermission = uiState.locationPermissionGranted,
                            userLocation = uiState.userLocation,
                            bearing = uiState.mapBearingDegrees,
                            onNavigationClick = if (uiState.navigation.active) {
                                { showMapOverride = false }
                            } else null,
                            modifier = Modifier.fillMaxSize(),
                            useOfflineStyle = hasOfflineMaps,
                            locationDotSize = locationDotSize,
                            locationRefreshIntervalMs = locationRefreshIntervalMs,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                )

                Box(
                    modifier = Modifier
                        .weight(0.38f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface),
                ) {
                    if (internetRadioState.playing != null) {
                        InternetRadioMiniPanel(
                            state = internetRadioState,
                            onOpenInternetRadio = onOpenInternetRadio,
                            onStop = onStopInternetRadio,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        MediaPlayerPanel(
                            state = uiState.media,
                            notificationListenerEnabled = notificationListenerEnabled,
                            onBrowseClick = onBrowseClick,
                            onOpenNotificationAccessSettings = onOpenNotificationAccessSettings,
                            onPlayPause = onPlayPause,
                            onNext = onNext,
                            onPrevious = onPrevious,
                            onSeekTo = onSeekTo,
                            onOpenBluetoothMusic = onOpenBluetoothMusic,
                            lyricsState = uiState.lyricsState,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }

        offlineDownloadActive?.let { active ->
            val prog = active.progress
            val p = if (prog.requiredResourceCount > 0) {
                (prog.completedResourceCount.toFloat() / prog.requiredResourceCount.toFloat()).coerceIn(0f, 1f)
            } else {
                null
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(0.88f)
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(32.dp),
                tonalElevation = 4.dp,
                shadowElevation = 2.dp,
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        text = "Unduh peta offline: ${active.label}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                    )
                    Spacer(Modifier.height(6.dp))
                    if (p != null) {
                        LinearProgressIndicator(
                            progress = { p },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }

        // App drawer + pengaturan (pojok kiri atas)
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onOpenAppDrawer,
                modifier = Modifier
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
            ) {
                Icon(
                    Icons.Default.Apps,
                    contentDescription = "Semua aplikasi",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp),
                )
            }
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Pengaturan",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp),
                )
            }
        }

        // Tombol Garage + Radio Internet (pojok kanan atas)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val hasLyrics = uiState.lyricsState is me.kyrozyn.carside.domain.model.LyricsState.Found
                            || uiState.lyricsState is me.kyrozyn.carside.domain.model.LyricsState.Loading
                    if (hasLyrics) {
                        IconButton(
                            onClick = onOpenLyricsFullscreen,
                            modifier = Modifier
                                .clip(RoundedCornerShape(32.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        ) {
                            Icon(
                                Icons.Default.MusicNote,
                                contentDescription = "Lirik lagu",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }
                    IconButton(
                        onClick = onOpenGarage,
                        modifier = Modifier
                            .clip(RoundedCornerShape(32.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
                    ) {
                        Icon(
                            Icons.Default.LocalGasStation,
                            contentDescription = "Garage — Trip & BBM",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                    IconButton(
                        onClick = onOpenInternetRadio,
                        modifier = Modifier
                            .clip(RoundedCornerShape(32.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
                    ) {
                        Icon(
                            Icons.Default.Podcasts,
                            contentDescription = "Radio Internet",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
        }

        // Fullscreen lyrics overlay
        if (uiState.lyricsFullscreenOpen) {
            LyricsFullscreenScreen(
                mediaState = uiState.media,
                lyricsState = uiState.lyricsState,
                onClose = onCloseLyricsFullscreen,
            )
        }
    }

    if (uiState.browseOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onDismissBrowse,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Text(
                text = "Media dari sistem",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Carside tidak memutar file sendiri. Informasi lagu diambil dari aplikasi yang sedang aktif (Spotify, YouTube Music, dsb.).\n\nUntuk membaca sesi media, Android meminta Anda mengaktifkan akses notifikasi untuk Carside.",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
            )
            TextButton(
                onClick = onOpenNotificationAccessSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Text("Buka pengaturan akses notifikasi", modifier = Modifier.fillMaxWidth())
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Orientasi peta: arah maju (GPS)",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = uiState.mapRotationMode == MapRotationMode.FORWARD_GPS,
                    onCheckedChange = onSetUseGpsForwardMapBearing,
                )
            }
            Text(
                text = "Saat aktif, peta mengikuti arah gerak GPS. Saat kendaraan pelan/diam, otomatis fallback ke kompas agar stabil.",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
            )
            TextButton(
                onClick = {
                    onRefreshSystemMedia()
                    onDismissBrowse()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Text("Segarkan daftar media", modifier = Modifier.fillMaxWidth())
            }
            TextButton(
                onClick = onDismissBrowse,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Text("Tutup", modifier = Modifier.fillMaxWidth())
            }
        }
    }

    uiState.shortcutPickerSlot?.let { slot ->
        ShortcutPickerSheet(
            slot = slot,
            onDismiss = onDismissShortcutPicker,
            onSelect = { packageName -> onSelectShortcutApp(slot, packageName) },
        )
    }

    if (uiState.weatherDetailsOpen) {
        WeatherDetailsFullScreen(
            uiState = uiState,
            onClose = onCloseWeatherDetails,
        )
    }
}

@Composable
private fun WeatherDetailsFullScreen(
    uiState: DashboardUiState,
    onClose: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = "Detail Cuaca",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when (val weather = uiState.weather) {
                    WeatherUi.Idle -> {
                        InfoCard(
                            title = "🌤️ Status",
                            value = "Menunggu data cuaca",
                        )
                    }
                    WeatherUi.Loading -> {
                        InfoCard(
                            title = "⏳ Status",
                            value = "Sedang memuat cuaca...",
                        )
                    }
                    is WeatherUi.Error -> {
                        InfoCard(
                            title = "⚠️ Status",
                            value = "Gagal memuat cuaca",
                        )
                        InfoCard(
                            title = "📝 Pesan",
                            value = weather.message,
                        )
                    }
                    is WeatherUi.Ready -> {
                        ForecastChartCard(summary = weather.summary)
                        InfoRow(
                            leftTitle = "🌡️ Suhu",
                            leftValue = "${"%.1f".format(weather.summary.temperatureCelsius)} °C",
                            rightTitle = "🔢 Kode WMO",
                            rightValue = weather.summary.weatherCode.toString(),
                        )
                        InfoCard(
                            title = "☁️ Kondisi",
                            value = weather.summary.description,
                        )
                        InfoCard(
                            title = "🕒 Update",
                            value = weather.summary.fetchedAt.atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss")),
                        )
                        InfoRow(
                            leftTitle = "💨 Angin",
                            leftValue = weather.summary.windSpeedKmh?.let { "${"%.1f".format(it)} km/h" } ?: "N/A",
                            rightTitle = "💧 Kelembapan",
                            rightValue = weather.summary.humidityPercent?.let { "$it%" } ?: "N/A",
                        )
                        InfoRow(
                            leftTitle = "🌍 Timezone",
                            leftValue = weather.summary.timezone ?: "N/A",
                            rightTitle = "⛰️ Elevasi",
                            rightValue = weather.summary.elevationMeters?.let { "${"%.0f".format(it)} m" } ?: "N/A",
                        )
                    }
                }

                val loc = uiState.userLocation
                if (loc != null) {
                    InfoRow(
                        leftTitle = "📍 Latitude",
                        leftValue = "%.6f".format(loc.latitude),
                        rightTitle = "📍 Longitude",
                        rightValue = "%.6f".format(loc.longitude),
                    )
                    InfoRow(
                        leftTitle = "🧭 Bearing GPS",
                        leftValue = loc.bearingDegrees?.let { "${it.toInt()}°" } ?: "N/A",
                        rightTitle = "🚗 Kecepatan",
                        rightValue = loc.speedMps?.let { "${"%.1f".format(it)} m/s" } ?: "N/A",
                        rightValueColor = loc.speedMps?.let { mps ->
                            if (mps * 3.6f > 90f) MaterialTheme.colorScheme.error else null
                        },
                    )
                } else {
                    InfoCard(
                        title = "📍 Lokasi",
                        value = "Belum tersedia",
                    )
                }

                InfoRow(
                    leftTitle = "🧭 Kompas",
                    leftValue = "${uiState.compass.cardinal} (${uiState.compass.degrees.toInt()}°)",
                    rightTitle = "🗺️ Orientasi peta",
                    rightValue = if (uiState.mapRotationMode == MapRotationMode.FORWARD_GPS) "Arah maju GPS" else "Kompas",
                )
            }
        }
    }
}

@Composable
private fun ForecastChartCard(summary: me.kyrozyn.carside.domain.model.WeatherSummary) {
    val points = summary.forecastHourly
    InfoCard(
        title = "📈 Forecast 24 Jam",
        value = if (points.isNotEmpty()) {
            "Dari ${points.first().timeIso.takeLast(5)} sampai ${points.last().timeIso.takeLast(5)}"
        } else {
            "Forecast belum tersedia"
        },
    )
    if (points.isEmpty()) return
    InfoRow(
        leftTitle = "⬇️ Min/Max Suhu",
        leftValue = "${"%.1f".format(points.minOf { it.temperatureCelsius })}°C / ${"%.1f".format(points.maxOf { it.temperatureCelsius })}°C",
        rightTitle = "💨 Max Angin",
        rightValue = "${"%.1f".format(points.maxOf { it.windSpeedKmh })} km/h",
    )
    MetricSparkline(
        title = "🌡️ Suhu (°C)",
        values = points.map { it.temperatureCelsius.toFloat() },
        yAxisLabel = "°C",
        xStartLabel = points.first().timeIso.takeLast(5),
        xEndLabel = points.last().timeIso.takeLast(5),
    )
    MetricSparkline(
        title = "💧 Kelembapan (%)",
        values = points.map { it.humidityPercent.toFloat() },
        yAxisLabel = "%",
        xStartLabel = points.first().timeIso.takeLast(5),
        xEndLabel = points.last().timeIso.takeLast(5),
    )
    MetricSparkline(
        title = "💨 Angin (km/h)",
        values = points.map { it.windSpeedKmh.toFloat() },
        yAxisLabel = "km/h",
        xStartLabel = points.first().timeIso.takeLast(5),
        xEndLabel = points.last().timeIso.takeLast(5),
    )
}

@Composable
private fun MetricSparkline(
    title: String,
    values: List<Float>,
    yAxisLabel: String,
    xStartLabel: String,
    xEndLabel: String,
) {
    val min = values.minOrNull() ?: 0f
    val max = values.maxOrNull() ?: 0f
    val range = (max - min).takeIf { it > 0f } ?: 1f
    val chartColor = MaterialTheme.colorScheme.primary
    val baselineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Y: $yAxisLabel   •   X: Jam lokal",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
            )
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp),
            ) {
                if (values.size < 2) return@Canvas
                val barGap = 2f
                val barWidth = ((size.width - ((values.size - 1) * barGap)) / values.size).coerceAtLeast(1f)
                values.forEachIndexed { i, v ->
                    val x = i * (barWidth + barGap)
                    val normalized = (v - min) / range
                    val top = size.height - (normalized * size.height)
                    drawLine(
                        color = chartColor,
                        start = Offset(x + (barWidth / 2f), size.height),
                        end = Offset(x + (barWidth / 2f), top),
                        strokeWidth = barWidth,
                    )
                }
                drawLine(
                    color = baselineColor,
                    start = Offset(0f, size.height - 1f),
                    end = Offset(size.width, size.height - 1f),
                    strokeWidth = 1f,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "X mulai: $xStartLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "X akhir: $xEndLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "Y min ${"%.1f".format(min)} $yAxisLabel  •  Y max ${"%.1f".format(max)} $yAxisLabel",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InfoRow(
    leftTitle: String,
    leftValue: String,
    rightTitle: String,
    rightValue: String,
    rightValueColor: Color? = null,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(modifier = Modifier.weight(1f)) {
            InfoCard(title = leftTitle, value = leftValue)
        }
        Box(modifier = Modifier.weight(1f)) {
            InfoCard(title = rightTitle, value = rightValue, valueColor = rightValueColor)
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    value: String,
    valueColor: Color? = null,
) {
    val resolvedValueColor = valueColor ?: MaterialTheme.colorScheme.onSurface
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = resolvedValueColor,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShortcutPickerSheet(
    slot: Int,
    onDismiss: () -> Unit,
    onSelect: (String?) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val pm = remember { context.packageManager }
    var apps by remember { mutableStateOf<List<LaunchableApp>>(emptyList()) }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.Default) {
            InstalledAppsLoader.loadLaunchableApps(pm, context)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = "Pilih aplikasi shortcut #${slot + 1}",
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        TextButton(
            onClick = { onSelect(null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            Text("Kosongkan slot", modifier = Modifier.fillMaxWidth())
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
        ) {
            items(apps, key = { it.targetId }) { app ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .clickable { onSelect(app.targetId) },
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                ) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = app.targetId,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        TextButton(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            Text("Batal", modifier = Modifier.fillMaxWidth())
        }
    }
}
