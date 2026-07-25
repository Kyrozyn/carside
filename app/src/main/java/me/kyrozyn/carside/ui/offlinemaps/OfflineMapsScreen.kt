package me.kyrozyn.carside.ui.offlinemaps

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.kyrozyn.carside.di.AppContainer
import me.kyrozyn.carside.domain.model.ActiveOfflineDownload
import me.kyrozyn.carside.domain.model.OfflineRegionInfo
import me.kyrozyn.carside.domain.model.OfflineRegionState

private fun formatSizeMb(bytes: Long): String =
    "%.1f MB".format(bytes / 1_000_000.0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineMapsScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onHome: () -> Unit = onBack,
    onNavigateToAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: OfflineMapsViewModel = viewModel(factory = OfflineMapsViewModel.factory(container))
    val regions by vm.regions.collectAsStateWithLifecycle()
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val activeDownload by vm.activeDownload.collectAsStateWithLifecycle()

    BackHandler(onBack = onBack)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Peta offline (OSM)") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                ),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        text = "Peta offline hanya tersedia untuk mode OSM (OpenFreeMap). Mode Google Maps tetap membutuhkan internet dan tidak dapat diunduh.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            activeDownload?.let { active ->
                ActiveDownloadBanner(active = active, modifier = Modifier.padding(vertical = 6.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Area tersimpan (${regions.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                TextButton(
                    onClick = onNavigateToAdd,
                    enabled = activeDownload == null,
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Tambah")
                }
            }

            if (uiState.lastError != null) {
                Text(
                    text = uiState.lastError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(regions, key = { it.id }) { region ->
                    OfflineRegionRow(
                        region = region,
                        onPause = { vm.pauseRegion(region.id) },
                        onResume = { vm.resumeRegion(region.id) },
                        onDelete = { vm.deleteRegion(region.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveDownloadBanner(
    active: ActiveOfflineDownload,
    modifier: Modifier = Modifier,
) {
    val prog = active.progress
    val p = if (prog.requiredResourceCount > 0) {
        (prog.completedResourceCount.toFloat() / prog.requiredResourceCount.toFloat()).coerceIn(0f, 1f)
    } else {
        null
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
        ),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = "Mengunduh di background: ${active.label}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Anda bisa kembali ke layar utama; unduhan tetap berjalan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.height(8.dp))
            if (p != null) {
                LinearProgressIndicator(
                    progress = { p },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "${prog.completedResourceCount} / ${prog.requiredResourceCount} sumber",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            prog.errorMessage?.let { err ->
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun OfflineRegionRow(
    region: OfflineRegionInfo,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        ),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(region.name, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Zoom ${region.minZoom}–${region.maxZoom} • ${formatSizeMb(region.sizeBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        when (region.state) {
                            OfflineRegionState.COMPLETE -> "Selesai"
                            OfflineRegionState.DOWNLOADING -> "Mengunduh…"
                            OfflineRegionState.IDLE -> "Jeda / tidak aktif"
                            OfflineRegionState.ERROR -> "Error"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Row {
                    when (region.state) {
                        OfflineRegionState.DOWNLOADING -> {
                            IconButton(onClick = onPause) {
                                Icon(Icons.Default.Pause, contentDescription = "Jeda")
                            }
                        }
                        OfflineRegionState.IDLE,
                        OfflineRegionState.ERROR,
                        -> {
                            IconButton(onClick = onResume) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Lanjutkan")
                            }
                        }
                        OfflineRegionState.COMPLETE -> Unit
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            if (region.state == OfflineRegionState.DOWNLOADING && region.tilesRequired > 0) {
                Spacer(Modifier.height(8.dp))
                val p = (region.tilesCompleted.toFloat() / region.tilesRequired.toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
