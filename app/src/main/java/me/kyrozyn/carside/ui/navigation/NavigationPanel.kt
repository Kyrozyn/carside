package me.kyrozyn.carside.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import me.kyrozyn.carside.domain.model.NavSource
import me.kyrozyn.carside.domain.model.NavigationState

/**
 * Panel navigasi yang menampilkan instruksi manuver dari notifikasi Google Maps / Waze.
 * Menggantikan [me.kyrozyn.carside.ui.map.MapPane] di Dashboard saat navigasi aktif.
 */
@Composable
fun NavigationPanel(
    state: NavigationState,
    onOpenSourceApp: () -> Unit,
    onSwitchToMap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxSize(),
        color = colors.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 72.dp, bottom = 20.dp),
        ) {
            SourceBadgeRow(
                source = state.sourceApp,
                onOpenSourceApp = onOpenSourceApp,
                onSwitchToMap = onSwitchToMap,
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ManeuverIconTile(
                    iconUri = state.iconUri,
                    distanceText = state.distanceText,
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.45f),
                )

                Spacer(Modifier.width(20.dp))

                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.55f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = state.maneuverText ?: "—",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    state.roadText?.let { road ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = road,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            state.etaText?.let { eta ->
                Spacer(Modifier.height(12.dp))
                EtaFooter(etaText = eta)
            }
        }
    }
}

@Composable
private fun SourceBadgeRow(
    source: NavSource,
    onOpenSourceApp: () -> Unit,
    onSwitchToMap: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            color = colors.primaryContainer,
            contentColor = colors.onPrimaryContainer,
            shape = RoundedCornerShape(50),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Navigation,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Navigasi • ${source.displayName}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                color = colors.surfaceVariant,
                contentColor = colors.onSurfaceVariant,
                shape = RoundedCornerShape(50),
                modifier = Modifier.clickable(onClick = onSwitchToMap),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Map,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Peta",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            if (source != NavSource.Unknown) {
                Surface(
                    color = colors.secondaryContainer,
                    contentColor = colors.onSecondaryContainer,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.clickable(onClick = onOpenSourceApp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Buka ${source.displayName}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ManeuverIconTile(
    iconUri: String?,
    distanceText: String?,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        color = colors.surfaceVariant,
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surface),
                contentAlignment = Alignment.Center,
            ) {
                if (iconUri != null) {
                    val ctx = LocalContext.current
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(ctx)
                            .data(iconUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Ikon manuver",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Navigation,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(80.dp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = distanceText ?: "…",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(min = 1.dp),
            )
        }
    }
}

@Composable
private fun EtaFooter(etaText: String) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.tertiaryContainer.copy(alpha = 0.6f),
        contentColor = colors.onTertiaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "ETA",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = etaText,
                style = MaterialTheme.typography.titleMedium,
                color = Color.Unspecified,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
