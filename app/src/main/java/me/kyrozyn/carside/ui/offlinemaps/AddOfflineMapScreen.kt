package me.kyrozyn.carside.ui.offlinemaps

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.kyrozyn.carside.di.AppContainer
import me.kyrozyn.carside.domain.model.CityPreset

private fun formatSizeMb(bytes: Long): String =
    "%.1f MB".format(bytes / 1_000_000.0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOfflineMapScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onHome: () -> Unit = onBack,
    modifier: Modifier = Modifier,
) {
    val vm: OfflineMapsViewModel = viewModel(factory = OfflineMapsViewModel.factory(container))
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        vm.clearLastError()
    }

    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(searchQuery, uiState.cityPresets) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) uiState.cityPresets
        else uiState.cityPresets.filter { preset ->
            preset.displayName.lowercase().contains(q) ||
                preset.province.lowercase().contains(q)
        }
    }

    BackHandler(onBack = onBack)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Tambah peta offline") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = onHome) {
                        Icon(
                            Icons.Outlined.Home,
                            contentDescription = "Layar Utama / Home",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    ),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            text = "Hanya style OSM (OpenFreeMap) yang bisa diunduh. Google Maps tidak bisa dipaketkan offline.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Cari kota atau provinsi") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    singleLine = true,
                )
            }

            item {
                Text(
                    text = "Pilih wilayah (${filtered.size} dari ${uiState.cityPresets.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (filtered.isEmpty()) {
                item {
                    Text(
                        text = "Belum ada hasil. Ubah kata kunci pencarian.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            } else {
                items(filtered, key = { it.id }) { preset ->
                    val selected = preset.id == uiState.selectedCityId
                    CityRow(
                        preset = preset,
                        selected = selected,
                        onClick = { vm.setSelectedCityId(preset.id) },
                    )
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }

            item {
                Text(
                    text = "Rentang zoom",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            item {
                RangeSlider(
                    value = uiState.zoomMin..uiState.zoomMax,
                    onValueChange = { range ->
                        vm.setZoomRange(range.start, range.endInclusive)
                    },
                    valueRange = 0f..17f,
                    steps = 16,
                )
                Text(
                    text = "Min ${uiState.zoomMin.toInt()} — Max ${uiState.zoomMax.toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    ),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            text = "Estimasi unduhan",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "~${uiState.estimatedTiles} tile (~${formatSizeMb(uiState.estimatedSizeBytes)})",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }

            if (uiState.estimatedSizeBytes > 500L * 1_000_000L) {
                item {
                    Text(
                        text = "Peringatan: estimasi di atas ~500 MB. Kurangi zoom max atau pilih wilayah lebih kecil.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            if (uiState.lastError != null) {
                item {
                    Text(
                        text = uiState.lastError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                val canSubmit = uiState.cityPresets.any { it.id == uiState.selectedCityId }
                Button(
                    onClick = {
                        if (vm.startDownload()) {
                            onBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canSubmit,
                ) {
                    Text("Mulai unduh")
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun CityRow(
    preset: CityPreset,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = preset.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
            Text(
                text = preset.province,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
