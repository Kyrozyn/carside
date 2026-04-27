package me.kyrozyn.carside.ui.media

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import me.kyrozyn.carside.domain.model.LyricsState
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
    lyricsState: LyricsState = LyricsState.Idle,
    modifier: Modifier = Modifier,
) {
    val displayPosMs = rememberDisplayedPositionMs(state)
    val isDarkTheme = isSystemInDarkTheme()
    val primaryTextColor = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = if (isDarkTheme) {
        Color.White.copy(alpha = 0.75f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val iconColor = if (isDarkTheme) Color.White.copy(alpha = 0.92f) else MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Transparent,
        tonalElevation = 0.dp,
    ) {
        Box(Modifier.fillMaxSize()) {
            // Blurred background album art
            MediaBackgroundArt(albumArtUri = state.track.albumArtUri)

            // Rich glassmorphism gradient overlay — denser at bottom
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to if (isDarkTheme) Color.Black.copy(alpha = 0.05f)
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.05f),
                            0.28f to if (isDarkTheme) Color.Black.copy(alpha = 0.30f)
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.22f),
                            0.6f to if (isDarkTheme) Color.Black.copy(alpha = 0.62f)
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.70f),
                            1f to if (isDarkTheme) Color.Black.copy(alpha = 0.90f)
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        ),
                    ),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            ) {
                // Notification access banner
                AnimatedVisibility(
                    visible = !notificationListenerEnabled,
                    enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.95f),
                    exit = fadeOut(tween(200)) + scaleOut(tween(200)),
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .clickable { onOpenNotificationAccessSettings() },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.93f),
                        shadowElevation = 4.dp,
                        tonalElevation = 2.dp,
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                text = "Aktifkan akses notifikasi",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Text(
                                text = "Diperlukan agar Spotify & pemutar lain terbaca. Ketuk di sini.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            Button(
                                onClick = onOpenNotificationAccessSettings,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                shape = RoundedCornerShape(12.dp),
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

                // Settings button — top right, 48dp touch target
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (isDarkTheme) Color.White.copy(alpha = 0.08f)
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        IconButton(
                            onClick = onBrowseClick,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Pengaturan",
                                tint = iconColor,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                LyricsDisplay(
                    lyricsState = lyricsState,
                    positionMs = displayPosMs,
                    primaryTextColor = primaryTextColor,
                    secondaryTextColor = secondaryTextColor,
                    modifier = Modifier.weight(1f),
                )

                // Main content area
                Column(Modifier.fillMaxWidth()) {
                    if (state.hasActiveSession) {
                        // Source app indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 12.dp),
                        ) {
                            AppSourceIcon(
                                packageName = state.sourcePackageName,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = state.sourceLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = secondaryTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        // Track title
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
                                fontWeight = FontWeight.Normal,
                                color = secondaryTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        Spacer(Modifier.height(18.dp))

                        // Progress bar / seek slider
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
                                        .height(32.dp),
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = if (isDarkTheme)
                                            Color.White.copy(alpha = 0.22f)
                                        else
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
                                        activeTickColor = Color.Transparent,
                                        inactiveTickColor = Color.Transparent,
                                    ),
                                )
                            } else {
                                val animatedProgress by animateFloatAsState(
                                    targetValue = progress,
                                    animationSpec = tween(500, easing = FastOutSlowInEasing),
                                    label = "progressAnim",
                                )
                                LinearProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(50)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = if (isDarkTheme)
                                        Color.White.copy(alpha = 0.18f)
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                    strokeCap = StrokeCap.Round,
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
                                    style = MaterialTheme.typography.labelSmall,
                                    color = secondaryTextColor,
                                )
                                Text(
                                    text = formatTrackTime(state.durationMs),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = secondaryTextColor,
                                )
                            }
                        } else if (state.isPlaying) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = if (isDarkTheme)
                                    Color.White.copy(alpha = 0.18f)
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                strokeCap = StrokeCap.Round,
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

                    Spacer(Modifier.height(18.dp))

                    val canToggle = state.hasActiveSession && (state.canPlay || state.canPause)

                    // Playback controls row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Skip previous
                        PlayerIconButton(
                            icon = Icons.Default.SkipPrevious,
                            contentDescription = "Sebelumnya",
                            enabled = state.canSkipPrevious,
                            iconColor = iconColor,
                            size = 56,
                            iconSize = 30,
                            onClick = onPrevious,
                        )

                        // Play/Pause — larger pill button
                        val playPressSource = remember { MutableInteractionSource() }
                        val playIsPressed by playPressSource.collectIsPressedAsState()
                        val playScale by animateFloatAsState(
                            targetValue = if (playIsPressed) 0.90f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "playScaleAnim",
                        )
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .scale(playScale)
                                .shadow(
                                    elevation = if (canToggle) 12.dp else 0.dp,
                                    shape = CircleShape,
                                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                                )
                                .clip(CircleShape)
                                .background(
                                    if (canToggle) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            IconButton(
                                onClick = onPlayPause,
                                enabled = canToggle,
                                modifier = Modifier.fillMaxSize(),
                                interactionSource = playPressSource,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = state.isPlaying,
                                        enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                                        exit = scaleOut(tween(150)),
                                    ) {
                                        Icon(
                                            Icons.Default.Pause,
                                            contentDescription = "Jeda",
                                            modifier = Modifier.size(38.dp),
                                            tint = if (canToggle) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                        )
                                    }
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = !state.isPlaying,
                                        enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                                        exit = scaleOut(tween(150)),
                                    ) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = "Putar",
                                            modifier = Modifier.size(38.dp),
                                            tint = if (canToggle) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                        )
                                    }
                                }
                            }
                        }

                        // Skip next
                        PlayerIconButton(
                            icon = Icons.Default.SkipNext,
                            contentDescription = "Berikutnya",
                            enabled = state.canSkipNext,
                            iconColor = iconColor,
                            size = 56,
                            iconSize = 30,
                            onClick = onNext,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean,
    iconColor: Color,
    size: Int,
    iconSize: Int,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "btnScaleAnim",
    )
    Box(
        modifier = Modifier
            .size(size.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                if (enabled) Color.White.copy(alpha = 0.10f)
                else Color.Transparent
            ),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxSize(),
            interactionSource = interactionSource,
        ) {
            Icon(
                icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(iconSize.dp),
                tint = iconColor.copy(alpha = if (enabled) 0.95f else 0.30f),
            )
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
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
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
                modifier = Modifier
                    .fillMaxSize()
                    .blur(radius = 28.dp),
                contentScale = ContentScale.Crop,
                loading = {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                if (isDarkTheme) Color(0xFF0D1B2A)
                                else MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
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
