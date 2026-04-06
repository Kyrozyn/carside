package me.kyrozyn.carside.ui.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.roundToInt
import me.kyrozyn.carside.domain.model.GeoLocation
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay

private const val FOLLOW_ZOOM = 17.0

private enum class MapStyle { GOOGLE, OSM }

private val TileSourceGoogle = object : OnlineTileSourceBase(
    "GoogleMapsTraffic",
    0, 20, 256, "",
    arrayOf(
        "https://mt0.google.com/vt/lyrs=m@221097413,traffic&",
        "https://mt1.google.com/vt/lyrs=m@221097413,traffic&",
        "https://mt2.google.com/vt/lyrs=m@221097413,traffic&",
        "https://mt3.google.com/vt/lyrs=m@221097413,traffic&",
    ),
) {
    override fun getTileURLString(pMapTileIndex: Long): String =
        "${baseUrl}x=${MapTileIndex.getX(pMapTileIndex)}" +
        "&y=${MapTileIndex.getY(pMapTileIndex)}" +
        "&z=${MapTileIndex.getZoom(pMapTileIndex)}"
}

// OSM default (Mapnik) — tile bawaan osmdroid, ringan dan proper untuk rotasi navigasi
private val TileSourceOsmDefault = TileSourceFactory.MAPNIK

private class MapFollowState {
    var marker: Marker? = null
    var didInitialZoom: Boolean = false
}

@Composable
fun MapPane(
    hasLocationPermission: Boolean,
    userLocation: GeoLocation?,
    bearing: Float = 0f,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    val arrowPx = (36 * density.density).roundToInt().coerceIn(24, 72)

    val mapState = remember { MapFollowState() }
    var mapStyle by remember { mutableStateOf(MapStyle.GOOGLE) }

    val mapView = remember(context) {
        MapView(context).apply {
            setTileSource(TileSourceGoogle)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            minZoomLevel = 4.0
            maxZoomLevel = 20.0
            controller.setZoom(10.0)
            controller.setCenter(GeoPoint(-6.2, 106.8))
            overlays.add(RotationGestureOverlay(this).apply { isEnabled = true })
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE  -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapState.marker?.let { mapView.overlays.remove(it) }
            mapState.marker = null
            mapState.didInitialZoom = false
            mapView.onDetach()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { mv ->
                // Terapkan tile source sesuai style aktif
                val expectedSource = when (mapStyle) {
                    MapStyle.GOOGLE -> TileSourceGoogle
                    MapStyle.OSM    -> TileSourceOsmDefault
                }
                if (mv.tileProvider.tileSource.name() != expectedSource.name()) {
                    mv.setTileSource(expectedSource)
                    mv.invalidate()
                }

                // Rotasi ikut kompas di OSM; Google Maps tetap North-Up
                mv.mapOrientation = if (mapStyle == MapStyle.OSM) -bearing else 0f

                if (!hasLocationPermission) {
                    mapState.marker?.let {
                        mv.overlays.remove(it)
                        mapState.marker = null
                    }
                    mapState.didInitialZoom = false
                    mv.invalidate()
                    return@AndroidView
                }

                val loc = userLocation ?: run {
                    mv.invalidate()
                    return@AndroidView
                }

                val point = GeoPoint(loc.latitude, loc.longitude)

                if (mapState.marker == null) {
                    val icon = locationArrowDrawable(mv.context.resources, arrowPx)
                    val marker = Marker(mv).apply {
                        position = point
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = "Posisi Anda"
                        setIcon(icon)
                        rotation = bearing
                    }
                    mv.overlays.add(marker)
                    mapState.marker = marker
                    if (!mapState.didInitialZoom) {
                        mv.controller.setZoom(FOLLOW_ZOOM)
                        mapState.didInitialZoom = true
                    }
                    mv.controller.animateTo(point)
                } else {
                    mapState.marker?.apply {
                        position = point
                        rotation = bearing
                    }
                    mv.controller.setCenter(point)
                }
                mv.invalidate()
            },
        )

        // Kontrol peta di kanan bawah
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End,
        ) {
            // Tombol switch layer
            MapLayerSwitchButton(
                currentStyle = mapStyle,
                onClick = {
                    mapStyle = if (mapStyle == MapStyle.GOOGLE) MapStyle.OSM else MapStyle.GOOGLE
                },
            )

            MapControlButton(
                icon = Icons.Default.Add,
                contentDescription = "Perbesar",
                onClick = { mapView.controller.zoomIn() },
            )
            MapControlButton(
                icon = Icons.Default.Remove,
                contentDescription = "Perkecil",
                onClick = { mapView.controller.zoomOut() },
            )
            MapControlButton(
                icon = Icons.Default.MyLocation,
                contentDescription = "Lokasi saya",
                tintOverride = MaterialTheme.colorScheme.primary,
                onClick = {
                    userLocation?.let {
                        mapView.controller.animateTo(GeoPoint(it.latitude, it.longitude))
                        mapView.controller.setZoom(FOLLOW_ZOOM)
                    }
                },
            )
        }
    }
}

@Composable
private fun MapLayerSwitchButton(
    currentStyle: MapStyle,
    onClick: () -> Unit,
) {
    val isGoogle = currentStyle == MapStyle.GOOGLE
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isGoogle)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.92f)
                else
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            ),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(44.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = "Ganti layer peta",
                    tint = if (isGoogle)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = if (isGoogle) "G" else "OSM",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isGoogle)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun MapControlButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tintOverride: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tintOverride,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

private fun locationArrowDrawable(
    res: android.content.res.Resources,
    sizePx: Int,
): BitmapDrawable {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#2196F3")
    }
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = sizePx * 0.12f
        strokeJoin = Paint.Join.ROUND
        color = Color.WHITE
    }

    val cx = sizePx / 2f
    val cy = sizePx / 2f
    val tip = cy * 0.10f
    val baseY = cy * 1.85f
    val halfBase = cx * 0.60f
    val notchDepth = cy * 1.45f

    val arrow = Path().apply {
        moveTo(cx, tip)
        lineTo(cx + halfBase, baseY)
        lineTo(cx, notchDepth)
        lineTo(cx - halfBase, baseY)
        close()
    }

    canvas.drawPath(arrow, fill)
    canvas.drawPath(arrow, stroke)

    return BitmapDrawable(res, bmp)
}
