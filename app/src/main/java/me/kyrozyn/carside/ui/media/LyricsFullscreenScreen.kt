package me.kyrozyn.carside.ui.media

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import me.kyrozyn.carside.domain.model.LyricsState
import me.kyrozyn.carside.domain.repository.MediaPlaybackState

@Composable
fun LyricsFullscreenScreen(
    mediaState: MediaPlaybackState,
    lyricsState: LyricsState,
    onClose: () -> Unit,
) {
    val displayPositionMs = rememberFullscreenPositionMs(mediaState)
    val isDarkTheme = isSystemInDarkTheme()
    val primaryTextColor = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = if (isDarkTheme) {
        Color.White.copy(alpha = 0.70f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = if (isDarkTheme) Color(0xFF0D1B2A) else MaterialTheme.colorScheme.surface,
    ) {
        Box(Modifier.fillMaxSize()) {
            val albumArtUri = mediaState.track.albumArtUri
            if (!albumArtUri.isNullOrBlank()) {
                val context = LocalContext.current
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(albumArtUri)
                        .crossfade(true)
                        .allowHardware(false)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(radius = 40.dp),
                    contentScale = ContentScale.Crop,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to if (isDarkTheme) Color.Black.copy(alpha = 0.30f)
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.30f),
                            0.25f to if (isDarkTheme) Color.Black.copy(alpha = 0.68f)
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                            1f to if (isDarkTheme) Color.Black.copy(alpha = 0.94f)
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                        ),
                    ),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (isDarkTheme) Color.White.copy(alpha = 0.10f)
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Tutup lirik",
                                tint = primaryTextColor,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }

                    Spacer(Modifier.width(14.dp))

                    AlbumArtThumbnail(
                        albumArtUri = albumArtUri,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    )

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mediaState.track.title.ifBlank { "Tidak ada lagu" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (mediaState.track.artist.isNotBlank()) {
                            Text(
                                text = mediaState.track.artist,
                                style = MaterialTheme.typography.bodyMedium,
                                color = secondaryTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                LyricsDisplay(
                    lyricsState = lyricsState,
                    positionMs = displayPositionMs,
                    primaryTextColor = primaryTextColor,
                    secondaryTextColor = secondaryTextColor,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun rememberFullscreenPositionMs(state: MediaPlaybackState): Long {
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(state.isPlaying, state.positionMs, state.positionAnchorElapsedRealtimeMs) {
        if (!state.isPlaying) {
            tick = 0L
            return@LaunchedEffect
        }
        while (isActive) {
            delay(500)
            tick = SystemClock.elapsedRealtime()
        }
    }
    return remember(state.positionMs, state.positionAnchorElapsedRealtimeMs, state.isPlaying, state.durationMs, tick) {
        if (!state.isPlaying) {
            state.positionMs.coerceAtLeast(0L)
        } else {
            val drift = SystemClock.elapsedRealtime() - state.positionAnchorElapsedRealtimeMs
            val extrapolated = state.positionMs + drift.coerceAtLeast(0L)
            if (state.durationMs > 0L) extrapolated.coerceIn(0L, state.durationMs)
            else extrapolated.coerceAtLeast(0L)
        }
    }
}

@Composable
private fun AlbumArtThumbnail(albumArtUri: String?, modifier: Modifier) {
    if (!albumArtUri.isNullOrBlank()) {
        val context = LocalContext.current
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(albumArtUri)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop,
            error = { AlbumArtPlaceholder(modifier) },
            loading = { AlbumArtPlaceholder(modifier) },
        )
    } else {
        AlbumArtPlaceholder(modifier)
    }
}

@Composable
private fun AlbumArtPlaceholder(modifier: Modifier) {
    Box(
        modifier = modifier.background(
            MaterialTheme.colorScheme.surfaceVariant,
            RoundedCornerShape(8.dp),
        ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp),
        )
    }
}

