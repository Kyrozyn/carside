package me.kyrozyn.carside.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.kyrozyn.carside.ui.map.MapPane
import me.kyrozyn.carside.ui.media.MediaPlayerPanel
import me.kyrozyn.carside.ui.status.LeftStatusBar
import me.kyrozyn.carside.ui.status.RightStatusBar

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
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(0.62f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                MapPane(
                    hasLocationPermission = uiState.locationPermissionGranted,
                    userLocation = uiState.userLocation,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
                LeftStatusBar(clockText = uiState.clockText)
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            )

            Column(
                modifier = Modifier
                    .weight(0.38f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                MediaPlayerPanel(
                    state = uiState.media,
                    notificationListenerEnabled = notificationListenerEnabled,
                    onBrowseClick = onBrowseClick,
                    onOpenNotificationAccessSettings = onOpenNotificationAccessSettings,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    onSeekTo = onSeekTo,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
                RightStatusBar(
                    compass = uiState.compass,
                    weather = uiState.weather,
                )
            }
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
}
