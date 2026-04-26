package me.kyrozyn.carside.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.kyrozyn.carside.data.settings.AppSettingsRepository
import me.kyrozyn.carside.ui.theme.AppThemePalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onKelolaDownloadMaps: () -> Unit,
    onOpenLauncherSettings: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    speedWarningEnabled: Boolean,
    onSpeedWarningEnabledChange: (Boolean) -> Unit,
    onTestSpeedWarningSound: () -> Unit,
    selectedPalette: AppThemePalette,
    onPaletteSelected: (AppThemePalette) -> Unit,
    settingsRepository: AppSettingsRepository,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Pengaturan",
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Di sini Anda mengatur fitur yang sebelumnya ada di peta atau tempat lain. " +
                    "Pilihan lain bisa ditambahkan nanti.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))

            // ── Tema Aplikasi ─────────────────────────────────────────────
            ThemePaletteCard(
                selectedPalette = selectedPalette,
                onPaletteSelected = onPaletteSelected,
            )

            // ── Launcher / layar utama ────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Launcher / layar utama",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Carside bisa dipakai sebagai aplikasi layar utama (launcher) di mobil. " +
                            "Setelah itu tombol Home sistem akan membuka Carside. " +
                            "Pilih Carside di pengaturan Android sebagai aplikasi layar utama.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    OutlinedButton(
                        onClick = onOpenLauncherSettings,
                        modifier = Modifier.padding(top = 10.dp),
                    ) {
                        Text("Buka pengaturan launcher")
                    }
                }
            }

            // ── Peringatan kecepatan ──────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Peringatan kecepatan (>90 km/h)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Satu nada lembut saat kecepatan baru melewati 90 km/h. " +
                                "Paling banyak sekali tiap 10 menit.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        OutlinedButton(
                            onClick = onTestSpeedWarningSound,
                            modifier = Modifier.padding(top = 10.dp),
                        ) {
                            Text("Tes suara")
                        }
                    }
                    Switch(
                        checked = speedWarningEnabled,
                        onCheckedChange = onSpeedWarningEnabledChange,
                    )
                }
            }

            // ── Ukuran titik lokasi ───────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Ukuran Titik Lokasi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val dotSize by settingsRepository.locationDotSize.collectAsState()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${dotSize.toInt()}px",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.width(60.dp),
                        )
                        Slider(
                            value = dotSize,
                            onValueChange = { settingsRepository.setLocationDotSize(it) },
                            valueRange = 40f..100f,
                            steps = 11,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Text(
                        text = "Atur ukuran indikator lokasi Anda saat ini di peta",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ── Kelola download maps ──────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onKelolaDownloadMaps),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Kelola download maps",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Unduh peta OSM untuk dipakai offline, hapus wilayah, atau tambah wilayah baru.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ── Privacy policy ──────────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenPrivacyPolicy),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Privacy Policy",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Lihat kebijakan privasi resmi Carside.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Theme palette card
// ---------------------------------------------------------------------------

@Composable
private fun ThemePaletteCard(
    selectedPalette: AppThemePalette,
    onPaletteSelected: (AppThemePalette) -> Unit,
    modifier: Modifier = Modifier,
) {
    val darkPalettes = AppThemePalette.entries.filter { it.isDark }
    val lightPalettes = AppThemePalette.entries.filter { !it.isDark }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Tema Aplikasi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Pilih palet warna untuk tampilan aplikasi. Palet tidak bisa dikustomisasi.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )

            // Tema Gelap
            Text(
                text = "TEMA GELAP",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                darkPalettes.forEach { palette ->
                    PaletteRow(
                        palette = palette,
                        isSelected = palette == selectedPalette,
                        onClick = { onPaletteSelected(palette) },
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            // Tema Terang
            Text(
                text = "TEMA TERANG",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                lightPalettes.forEach { palette ->
                    PaletteRow(
                        palette = palette,
                        isSelected = palette == selectedPalette,
                        onClick = { onPaletteSelected(palette) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PaletteRow(
    palette: AppThemePalette,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedBorderColor = MaterialTheme.colorScheme.primary
    val unselectedBorderColor = MaterialTheme.colorScheme.outlineVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 1.5.dp,
                        color = selectedBorderColor,
                        shape = MaterialTheme.shapes.small,
                    )
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Color swatch
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(palette.previewColor),
        )

        // Name
        Text(
            text = palette.displayName,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )

        // Checkmark when selected
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Terpilih",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
