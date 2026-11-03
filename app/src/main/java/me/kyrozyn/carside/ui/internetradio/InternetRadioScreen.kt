package me.kyrozyn.carside.ui.internetradio

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import me.kyrozyn.carside.di.AppContainer
import me.kyrozyn.carside.domain.model.InternetRadioState
import me.kyrozyn.carside.domain.model.InternetStation
import me.kyrozyn.carside.domain.model.SortOrder

import androidx.compose.material.icons.outlined.Home

private enum class ListTab { ALL, FAVORITES }

@Composable
fun InternetRadioScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onHome: () -> Unit = onBack,
    modifier: Modifier = Modifier,
) {
    val vm: InternetRadioViewModel = viewModel(factory = InternetRadioViewModel.factory(container))
    val state by vm.uiState.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(ListTab.ALL) }

    BackHandler(onBack = onBack)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(Modifier.fillMaxSize()) {
            Header(state = state, onBack = onBack, onHome = onHome, onRefresh = vm::refresh)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight(),
                ) {
                    StationListWithFilters(
                        state = state,
                        tab = tab,
                        onTabChange = { tab = it },
                        onQuery = vm::setQuery,
                        onSort = vm::setSort,
                        onTag = vm::setTag,
                        onPlay = vm::play,
                        onToggleFavorite = vm::toggleFavorite,
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight(),
                ) {
                    NowPlayingPanel(
                        state = state,
                        onVolume = vm::setVolume,
                        onToggleMute = vm::toggleMute,
                        onStop = vm::stop,
                        onToggleFavorite = vm::toggleFavorite,
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(
    state: InternetRadioState,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onRefresh: () -> Unit,
) {
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
            Icons.Default.Podcasts,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(26.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Radio Internet (Indonesia)",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.weight(1f))
        if (state.loading) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
        }
        IconButton(onClick = onRefresh) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = "Segarkan",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        IconButton(onClick = onHome) {
            Icon(
                Icons.Outlined.Home,
                contentDescription = "Layar Utama / Home",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
    if (state.errorMessage != null) {
        Text(
            text = state.errorMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
}

private fun filterStations(
    stations: List<InternetStation>,
    query: String,
    tag: String?,
): List<InternetStation> {
    val q = query.trim().lowercase()
    return stations.asSequence()
        .filter { s ->
            if (q.isBlank()) true
            else s.name.lowercase().contains(q) ||
                s.tags.any { it.lowercase().contains(q) }
        }
        .filter { s ->
            if (tag == null) true
            else s.tags.any { it.equals(tag, ignoreCase = true) }
        }
        .toList()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StationListWithFilters(
    state: InternetRadioState,
    tab: ListTab,
    onTabChange: (ListTab) -> Unit,
    onQuery: (String) -> Unit,
    onSort: (SortOrder) -> Unit,
    onTag: (String?) -> Unit,
    onPlay: (InternetStation) -> Unit,
    onToggleFavorite: (InternetStation) -> Unit,
) {
    val source = when (tab) {
        ListTab.ALL -> state.stations
        ListTab.FAVORITES -> state.favorites
    }
    val filtered = filterStations(source, state.query, state.tagFilter)
    val favoriteIds = remember(state.favorites) { state.favorites.map { it.uuid }.toSet() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "__search") {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQuery,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                placeholder = { Text("Cari stasiun…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item(key = "__sort") {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val sorts = listOf(
                    SortOrder.POPULAR to "Populer",
                    SortOrder.MOST_VOTED to "Top Vote",
                    SortOrder.RECENT to "Baru",
                )
                sorts.forEachIndexed { index, (order, label) ->
                    SegmentedButton(
                        selected = state.sort == order,
                        onClick = { onSort(order) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = sorts.size),
                    ) {
                        Text(label)
                    }
                }
            }
        }
        item(key = "__tab") {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val items = listOf(ListTab.ALL to "Semua", ListTab.FAVORITES to "Favorit")
                items.forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        selected = tab == value,
                        onClick = { onTabChange(value) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = items.size),
                    ) {
                        Text(label)
                    }
                }
            }
        }
        if (state.availableTags.isNotEmpty()) {
            item(key = "__tags") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = state.tagFilter == null,
                            onClick = { onTag(null) },
                            label = { Text("Semua tag") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        )
                    }
                    items(state.availableTags, key = { it }) { tag ->
                        FilterChip(
                            selected = state.tagFilter == tag,
                            onClick = { onTag(if (state.tagFilter == tag) null else tag) },
                            label = { Text(tag) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        )
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item(key = "__empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = when {
                            state.loading -> "Memuat stasiun…"
                            tab == ListTab.FAVORITES -> "Belum ada favorit. Long-press stasiun untuk menambah."
                            else -> "Tidak ada stasiun cocok."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            items(filtered, key = { it.uuid }) { station ->
                StationRow(
                    station = station,
                    isPlaying = state.playing?.uuid == station.uuid && state.isPlaying,
                    isFavorite = favoriteIds.contains(station.uuid),
                    onPlay = { onPlay(station) },
                    onLongClick = { onToggleFavorite(station) },
                    onFavoriteClick = { onToggleFavorite(station) },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StationRow(
    station: InternetStation,
    isPlaying: Boolean,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onLongClick: () -> Unit,
    onFavoriteClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = if (isPlaying) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        },
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .combinedClickable(onClick = onPlay, onLongClick = onLongClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StationFavicon(url = station.faviconUrl, size = 40.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val meta = buildString {
                    if (station.codec != null) append(station.codec.uppercase())
                    if (station.bitrate > 0) {
                        if (isNotEmpty()) append(" • ")
                        append("${station.bitrate}kbps")
                    }
                    val tagHint = station.tags.take(2).joinToString(", ")
                    if (tagHint.isNotBlank()) {
                        if (isNotEmpty()) append(" • ")
                        append(tagHint)
                    }
                }
                if (meta.isNotBlank()) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "Hapus favorit" else "Tambah favorit",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StationFavicon(url: String?, size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                Icons.Default.Podcasts,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(size * 0.55f),
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
private fun NowPlayingPanel(
    state: InternetRadioState,
    onVolume: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onStop: () -> Unit,
    onToggleFavorite: (InternetStation) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val playing = state.playing
            val now = state.nowPlaying
            if (playing == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Podcasts,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(72.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Pilih stasiun untuk mulai",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                return@Column
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StationFavicon(url = playing.faviconUrl, size = 72.dp)
                Column(Modifier.weight(1f)) {
                    Text(
                        text = playing.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val meta = buildString {
                        if (playing.codec != null) append(playing.codec.uppercase())
                        if (playing.bitrate > 0) {
                            if (isNotEmpty()) append(" • ")
                            append("${playing.bitrate}kbps")
                        }
                    }
                    if (meta.isNotBlank()) {
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                val isFav = state.favorites.any { it.uuid == playing.uuid }
                IconButton(onClick = { onToggleFavorite(playing) }) {
                    Icon(
                        imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFav) "Hapus favorit" else "Tambah favorit",
                        tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            val title = now?.title
            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee(iterations = Int.MAX_VALUE),
                )
            } else {
                Text(
                    text = if (state.isBuffering) "Menyangga…" else if (state.isPlaying) "On Air" else "Diam",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (state.isBuffering) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Buffering",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Volume",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = state.volume,
                    onValueChange = onVolume,
                    valueRange = 0f..1f,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleButton(
                    onClick = onToggleMute,
                    highlight = state.isMuted,
                    size = 56.dp,
                ) {
                    Icon(
                        imageVector = if (state.isMuted) {
                            Icons.AutoMirrored.Filled.VolumeOff
                        } else {
                            Icons.AutoMirrored.Filled.VolumeUp
                        },
                        contentDescription = if (state.isMuted) "Unmute" else "Mute",
                    )
                }
                CircleButton(
                    onClick = onStop,
                    highlight = false,
                    size = 64.dp,
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Stop",
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CircleButton(
    onClick: () -> Unit,
    highlight: Boolean,
    size: androidx.compose.ui.unit.Dp,
    content: @Composable () -> Unit,
) {
    val bg = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val fg = if (highlight) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides fg,
        ) {
            content()
        }
    }
}
