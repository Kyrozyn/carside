package me.kyrozyn.carside.ui.media

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.kyrozyn.carside.domain.model.LyricsState
import me.kyrozyn.carside.domain.model.SyncedLine

@Composable
fun LyricsDisplay(
    lyricsState: LyricsState,
    positionMs: Long,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    modifier: Modifier = Modifier,
) {
    when (lyricsState) {
        is LyricsState.Idle, is LyricsState.NotFound -> Spacer(modifier)
        is LyricsState.Loading -> LoadingLyrics(modifier)
        is LyricsState.Found -> {
            val synced = lyricsState.syncedLines
            if (!synced.isNullOrEmpty()) {
                // Hanya tampilkan 1 baris lirik time-synced yang aktif
                SingleLineSyncedLyricsView(
                    lines = synced,
                    positionMs = positionMs,
                    primaryTextColor = primaryTextColor,
                    modifier = modifier,
                )
            } else {
                // Jika lirik tidak time-synced (plain), jangan tampilkan di sini
                // User bisa akses melalui halaman lirik full
                Spacer(modifier)
            }
        }
    }
}

@Composable
private fun LoadingLyrics(modifier: Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier.padding(8.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun PlainLyricsView(
    lyrics: String,
    secondaryTextColor: Color,
    modifier: Modifier,
) {
    val scrollState = rememberScrollState()
    Box(
        modifier = modifier
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()
                val fadeHeight = size.height * 0.15f
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent,
                        fadeHeight / size.height to Color.Black,
                        1f - fadeHeight / size.height to Color.Black,
                        1f to Color.Transparent,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            },
    ) {
        Text(
            text = lyrics,
            style = MaterialTheme.typography.bodyMedium,
            color = secondaryTextColor,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(vertical = 8.dp),
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
        )
    }
}

@Composable
private fun SingleLineSyncedLyricsView(
    lines: List<SyncedLine>,
    positionMs: Long,
    primaryTextColor: Color,
    modifier: Modifier,
) {
    val activeIndex by remember(positionMs, lines) {
        derivedStateOf {
            var idx = 0
            for (i in lines.indices) {
                if (lines[i].timestampMs <= positionMs) idx = i else break
            }
            idx
        }
    }

    val activeLine = lines.getOrNull(activeIndex)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        if (activeLine != null) {
            Text(
                text = activeLine.text.ifBlank { "\u200B" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = primaryTextColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SyncedLyricsView(
    lines: List<SyncedLine>,
    positionMs: Long,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    modifier: Modifier,
) {
    val listState = rememberLazyListState()

    val activeIndex by remember(positionMs, lines) {
        derivedStateOf {
            var idx = 0
            for (i in lines.indices) {
                if (lines[i].timestampMs <= positionMs) idx = i else break
            }
            idx
        }
    }

    LaunchedEffect(activeIndex) {
        if (lines.isEmpty()) return@LaunchedEffect
        listState.animateScrollToItem(
            index = activeIndex,
            scrollOffset = -200,
        )
    }

    Box(
        modifier = modifier
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()
                val fadeHeight = size.height * 0.18f
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent,
                        fadeHeight / size.height to Color.Black,
                        1f - fadeHeight / size.height to Color.Black,
                        1f to Color.Transparent,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            },
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.Start,
        ) {
            itemsIndexed(lines, key = { i, _ -> i }) { index, line ->
                val distance = index - activeIndex
                val targetAlpha = when {
                    distance == 0 -> 1.0f
                    distance in -1..1 -> 0.55f
                    else -> 0.28f
                }
                val alpha by animateFloatAsState(
                    targetValue = targetAlpha,
                    animationSpec = tween(300),
                    label = "lyricAlpha$index",
                )
                val style = when {
                    distance == 0 -> MaterialTheme.typography.titleMedium
                    distance in -1..1 -> MaterialTheme.typography.bodyLarge
                    else -> MaterialTheme.typography.bodyMedium
                }
                val weight = if (distance == 0) FontWeight.Bold else FontWeight.Normal

                Text(
                    text = line.text.ifBlank { "\u200B" },
                    style = style,
                    fontWeight = weight,
                    color = primaryTextColor.copy(alpha = alpha),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                )
            }
        }
    }
}
