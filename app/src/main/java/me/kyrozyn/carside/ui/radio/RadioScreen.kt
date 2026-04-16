package me.kyrozyn.carside.ui.radio

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.kyrozyn.carside.di.AppContainer
import me.kyrozyn.carside.domain.model.RadioBand
import me.kyrozyn.carside.domain.model.RadioPreset
import me.kyrozyn.carside.domain.model.RadioState

@Composable
fun RadioScreen(
    container: AppContainer,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: RadioViewModel = viewModel(factory = RadioViewModel.factory(container))
    val state by vm.uiState.collectAsStateWithLifecycle()

    BackHandler(onBack = onBack)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(Modifier.fillMaxSize()) {
            RadioHeader(state = state, onBack = onBack)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            if (!state.available) {
                RadioUnavailableBody(state = state)
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        RadioNowPlaying(state = state)
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        RadioControls(
                            state = state,
                            onSetBand = vm::setBand,
                            onStepUp = vm::stepUp,
                            onStepDown = vm::stepDown,
                            onScanUp = vm::scanUp,
                            onScanDown = vm::scanDown,
                            onCancelScan = vm::cancelScan,
                            onToggleMute = vm::toggleMute,
                            onLoadPreset = vm::loadPreset,
                            onSavePreset = vm::savePreset,
                            onClearPreset = vm::clearPreset,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RadioHeader(state: RadioState, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Kembali",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Icon(
            Icons.Default.Radio,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(26.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Radio",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.weight(1f))
        if (state.available) {
            Text(
                text = state.band.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
    }
}

@Composable
private fun RadioUnavailableBody(state: RadioState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Default.Radio,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(72.dp),
        )
        Text(
            text = "Radio tidak tersedia",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = state.errorMessage
                ?: "Pastikan perangkat memiliki tuner FM/AM dan aplikasi diinstal sebagai system/privileged app dengan permission ACCESS_BROADCAST_RADIO.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun RadioNowPlaying(state: RadioState) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = formatFrequency(state.frequencyKhz, state.band),
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = state.band.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val station = state.program?.stationName
            if (!station.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = station,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            val radioText = state.program?.radioText
            if (!radioText.isNullOrBlank()) {
                Text(
                    text = radioText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            val pty = state.program?.programType
            if (!pty.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                ) {
                    Text(
                        text = "PTY $pty",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            SignalBar(strength = state.program?.signalStrength)

            val status = when {
                state.isScanning -> "Scanning…"
                state.isTuning -> "Tuning…"
                state.errorMessage != null -> state.errorMessage
                else -> null
            }
            if (status != null) {
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SignalBar(strength: Int?) {
    if (strength == null) return
    val clamped = strength.coerceIn(0, 100)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Sinyal $clamped",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(clamped / 100f)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Composable
private fun RadioControls(
    state: RadioState,
    onSetBand: (RadioBand) -> Unit,
    onStepUp: () -> Unit,
    onStepDown: () -> Unit,
    onScanUp: () -> Unit,
    onScanDown: () -> Unit,
    onCancelScan: () -> Unit,
    onToggleMute: () -> Unit,
    onLoadPreset: (Int) -> Unit,
    onSavePreset: (Int) -> Unit,
    onClearPreset: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.band == RadioBand.FM,
                onClick = { onSetBand(RadioBand.FM) },
                label = { Text("FM") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
            FilterChip(
                selected = state.band == RadioBand.AM,
                onClick = { onSetBand(RadioBand.AM) },
                label = { Text("AM") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }

        Text(
            text = "Preset — tap untuk load, tekan lama untuk simpan",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items((0 until RadioState.PRESET_SLOT_COUNT).toList(), key = { it }) { slot ->
                val preset = state.presets.firstOrNull { it.slot == slot }
                PresetChip(
                    slot = slot,
                    preset = preset,
                    onLoad = { onLoadPreset(slot) },
                    onSave = { onSavePreset(slot) },
                    onClear = { onClearPreset(slot) },
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ControlButton(
                icon = Icons.Default.SkipPrevious,
                description = "Scan down",
                onClick = { if (state.isScanning) onCancelScan() else onScanDown() },
                highlight = state.isScanning,
            )
            ControlButton(
                icon = Icons.Default.FastRewind,
                description = "Step down",
                onClick = onStepDown,
            )
            ControlButton(
                icon = if (state.isMuted) {
                    Icons.AutoMirrored.Filled.VolumeOff
                } else {
                    Icons.AutoMirrored.Filled.VolumeUp
                },
                description = if (state.isMuted) "Unmute" else "Mute",
                onClick = onToggleMute,
                highlight = state.isMuted,
                big = true,
            )
            ControlButton(
                icon = Icons.Default.FastForward,
                description = "Step up",
                onClick = onStepUp,
            )
            ControlButton(
                icon = Icons.Default.SkipNext,
                description = "Scan up",
                onClick = { if (state.isScanning) onCancelScan() else onScanUp() },
                highlight = state.isScanning,
            )
        }
    }
}

@Composable
private fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    highlight: Boolean = false,
    big: Boolean = false,
) {
    val size = if (big) 64.dp else 52.dp
    val iconSize = if (big) 34.dp else 26.dp
    val bg = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (highlight) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = fg,
            modifier = Modifier.size(iconSize),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PresetChip(
    slot: Int,
    preset: RadioPreset?,
    onLoad: () -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (preset != null) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = { if (preset != null) onLoad() else onSave() },
                onLongClick = onSave,
            ),
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "P${slot + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (preset != null) {
                    Text(
                        text = formatFrequency(preset.frequencyKhz, preset.band),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!preset.label.isNullOrBlank()) {
                        Text(
                            text = preset.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    } else {
                        Text(
                            text = preset.band.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Text(
                        text = "Kosong",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Tap untuk simpan",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }
            if (preset != null) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(28.dp),
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Hapus preset",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

private fun formatFrequency(frequencyKhz: Int, band: RadioBand): String = when (band) {
    RadioBand.FM -> {
        val mhz = frequencyKhz / 1000.0
        "%.2f FM".format(mhz)
    }
    RadioBand.AM -> {
        if (frequencyKhz >= 1000) {
            "%d kHz".format(frequencyKhz)
        } else {
            "$frequencyKhz kHz"
        }
    }
}
