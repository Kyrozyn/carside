package me.kyrozyn.carside.ui.media

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import android.os.SystemClock
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import me.kyrozyn.carside.domain.repository.MediaPlaybackState

@Composable
fun MediaPlayerPanel(
    state: MediaPlaybackState,
    notificationListenerEnabled: Boolean,
    onBrowseClick: () -> Unit,
    onOpenNotificationAccessSettings: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayPosMs = rememberDisplayedPositionMs(state)
    val isDarkTheme = isSystemInDarkTheme()
    val primaryTextColor = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = if (isDarkTheme) {
        Color.White.copy(alpha = 0.82f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val iconColor = if (isDarkTheme) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Transparent,
        tonalElevation = 0.dp,
    ) {
        Box(Modifier.fillMaxSize()) {
            MediaBackgroundArt(albumArtUri = state.track.albumArtUri)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to if (isDarkTheme) Color.Black.copy(alpha = 0.1f)
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.08f),
                            0.35f to if (isDarkTheme) Color.Black.copy(alpha = 0.25f)
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                            1f to if (isDarkTheme) Color.Black.copy(alpha = 0.82f)
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        ),
                    ),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                if (!notificationListenerEnabled) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clickable { onOpenNotificationAccessSettings() },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.92f),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                text = "Aktifkan akses notifikasi",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Text(
                                text = "Diperlukan agar Spotify & pemutar lain terbaca. Ketuk di sini.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f),
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            Button(
                                onClick = onOpenNotificationAccessSettings,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError,
                                ),
                            ) {
                                Text("Buka pengaturan akses notifikasi")
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    IconButton(
                        onClick = onBrowseClick,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Pengaturan",
                            tint = iconColor,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                Column(Modifier.fillMaxWidth()) {
                    if (state.hasActiveSession) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 10.dp),
                        ) {
                            AppSourceIcon(
                                packageName = state.sourcePackageName,
                                modifier = Modifier.size(22.dp),
                            )
                            Text(
                                text = state.sourceLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = secondaryTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        Text(
                            text = state.track.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = primaryTextColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (state.track.artist.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = state.track.artist,
                                style = MaterialTheme.typography.titleMedium,
                                color = secondaryTextColor,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        if (state.durationMs > 0L) {
                            val progress = (displayPosMs.toFloat() / state.durationMs).coerceIn(0f, 1f)
                            if (state.canSeek) {
                                var sliderValue by remember(state.track.title, state.durationMs) {
                                    mutableFloatStateOf(progress)
                                }
                                var dragging by remember { mutableStateOf(false) }
                                LaunchedEffect(progress, state.isPlaying) {
                                    if (!dragging) sliderValue = progress
                                }
                                Slider(
                                    value = sliderValue,
                                    onValueChange = {
                                        dragging = true
                                        sliderValue = it
                                    },
                                    onValueChangeFinished = {
                                        dragging = false
                                        onSeekTo((sliderValue * state.durationMs).toLong())
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(28.dp),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF42A5F5),
                                        activeTrackColor = Color(0xFF42A5F5),
                                        inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f),
                                    ),
                                )
                            } else {
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = Color(0xFF42A5F5),
                                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f),
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = formatTrackTime(displayPosMs),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = secondaryTextColor,
                                )
                                Text(
                                    text = formatTrackTime(state.durationMs),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = secondaryTextColor,
                                )
                            }
                        } else if (state.isPlaying) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = Color(0xFF42A5F5),
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f),
                            )
                        }
                    } else {
                        Text(
                            text = state.track.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = primaryTextColor,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = state.track.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = secondaryTextColor,
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    val canToggle = state.hasActiveSession && (state.canPlay || state.canPause)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = onPrevious,
                            enabled = state.canSkipPrevious,
                            modifier = Modifier.size(52.dp),
                        ) {
                            Icon(
                                Icons.Default.SkipPrevious,
                                contentDescription = "Sebelumnya",
                                modifier = Modifier.size(30.dp),
                                tint = iconColor.copy(alpha = if (state.canSkipPrevious) 0.95f else 0.35f),
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(
                                    if (canToggle) MaterialTheme.colorScheme.primary.copy(alpha = 0.95f)
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            IconButton(
                                onClick = onPlayPause,
                                enabled = canToggle,
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                Icon(
                                    imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (state.isPlaying) "Jeda" else "Putar",
                                    modifier = Modifier.size(34.dp),
                                    tint = if (canToggle) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                )
                            }
                        }

                        IconButton(
                            onClick = onNext,
                            enabled = state.canSkipNext,
                            modifier = Modifier.size(52.dp),
                        ) {
                            Icon(
                                Icons.Default.SkipNext,
                                contentDescription = "Berikutnya",
                                modifier = Modifier.size(30.dp),
                                tint = iconColor.copy(alpha = if (state.canSkipNext) 0.95f else 0.35f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberDisplayedPositionMs(state: MediaPlaybackState): Long {
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(state.isPlaying, state.positionMs, state.positionAnchorElapsedRealtimeMs, state.durationMs) {
        if (!state.isPlaying) {
            tick = 0L
            return@LaunchedEffect
        }
        while (isActive) {
            delay(500)
            tick = SystemClock.elapsedRealtime()
        }
    }
    return remember(
        state.positionMs,
        state.positionAnchorElapsedRealtimeMs,
        state.isPlaying,
        state.durationMs,
        tick,
    ) {
        if (!state.isPlaying) {
            state.positionMs.coerceAtLeast(0L)
        } else {
            val drift = SystemClock.elapsedRealtime() - state.positionAnchorElapsedRealtimeMs
            val extrapolated = state.positionMs + drift.coerceAtLeast(0L)
            if (state.durationMs > 0L) {
                extrapolated.coerceIn(0L, state.durationMs)
            } else {
                extrapolated.coerceAtLeast(0L)
            }
        }
    }
}

private fun formatTrackTime(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val m = totalSec / 60L
    val s = totalSec % 60L
    return "%02d:%02d".format(m, s)
}

@Composable
private fun AppSourceIcon(packageName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val imageBitmap = remember(packageName) {
        if (packageName.isBlank()) return@remember null
        runCatching {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            drawable.toBitmap(width = 96, height = 96).asImageBitmap()
        }.getOrNull()
    }
    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = null,
            modifier = modifier.clip(RoundedCornerShape(5.dp)),
            contentScale = ContentScale.Crop,
        )
    } else {
        Icon(
            Icons.Default.MusicNote,
            contentDescription = null,
            modifier = modifier,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
        )
    }
}

@Composable
private fun MediaBackgroundArt(albumArtUri: String?) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    Box(Modifier.fillMaxSize()) {
        if (albumArtUri.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                if (isDarkTheme) Color(0xFF1A2744) else MaterialTheme.colorScheme.surfaceVariant,
                                if (isDarkTheme) Color(0xFF0D1B2A) else MaterialTheme.colorScheme.surface,
                            ),
                        ),
                    ),
            )
        } else {
            val request = ImageRequest.Builder(context)
                .data(albumArtUri)
                .crossfade(true)
                .allowHardware(false)
                .build()
            SubcomposeAsyncImage(
                model = request,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            strokeWidth = 3.dp,
                            color = Color(0xFF42A5F5),
                        )
                    }
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (isDarkTheme) Color(0xFF0D1B2A) else MaterialTheme.colorScheme.surface,
                            ),
                    )
                },
            )
        }
    }
}
