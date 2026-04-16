package me.kyrozyn.carside.ui.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import me.kyrozyn.carside.data.map.OPEN_FREE_MAP_LIBERTY_STYLE_URL
import me.kyrozyn.carside.domain.model.GeoLocation
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.iconRotationAlignment
import org.maplibre.android.style.layers.PropertyFactory.iconSize
import org.maplibre.android.style.layers.PropertyFactory.rasterOpacity
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.TileSet
import org.maplibre.geojson.Feature
import org.maplibre.geojson.Point

private const val FOLLOW_ZOOM = 17.0
private const val LOCATION_SOURCE_ID = "location-source"
private const val LOCATION_LAYER_ID = "location-layer"
private const val LOCATION_ICON_ID = "location-dot"
private const val OSM_TRAFFIC_SOURCE_ID = "osm-google-traffic-source"
private const val OSM_TRAFFIC_LAYER_ID = "osm-google-traffic-layer"

private val GOOGLE_TRAFFIC_TILE_URLS = arrayOf(
    "https://mt0.google.com/vt/lyrs=m@221097413,traffic&x={x}&y={y}&z={z}",
    "https://mt1.google.com/vt/lyrs=m@221097413,traffic&x={x}&y={y}&z={z}",
    "https://mt2.google.com/vt/lyrs=m@221097413,traffic&x={x}&y={y}&z={z}",
    "https://mt3.google.com/vt/lyrs=m@221097413,traffic&x={x}&y={y}&z={z}",
)

private enum class MapStyle {
    /** Google raster + traffic */
    GOOGLE,
    /** OpenFreeMap vector + overlay traffic Google (opacity ringan) */
    OSM_GOOGLE_TRAFFIC,
    /** OpenFreeMap vector saja */
    OSM,
}

// Style JSON untuk Google Maps raster + traffic — teks tidak dirotasi karena raster
private val GOOGLE_STYLE_JSON = """
{
  "version": 8,
  "sources": {
    "google-traffic": {
      "type": "raster",
      "tiles": [
        "https://mt0.google.com/vt/lyrs=m@221097413,traffic&x={x}&y={y}&z={z}",
        "https://mt1.google.com/vt/lyrs=m@221097413,traffic&x={x}&y={y}&z={z}",
        "https://mt2.google.com/vt/lyrs=m@221097413,traffic&x={x}&y={y}&z={z}",
        "https://mt3.google.com/vt/lyrs=m@221097413,traffic&x={x}&y={y}&z={z}"
      ],
      "tileSize": 256
    }
  },
  "layers": [{
    "id": "google-traffic-layer",
    "type": "raster",
    "source": "google-traffic"
  }]
}
""".trimIndent()

// Vector tiles OpenFreeMap — teks tetap tegak saat dirotasi

private class MapState {
    var map: MapLibreMap? = null
    var styleLoaded: Boolean = false
    var pendingLocation: GeoLocation? = null
    var pendingBearing: Float = 0f
    var didInitialZoom: Boolean = false
}

@Composable
fun MapPane(
    hasLocationPermission: Boolean,
    userLocation: GeoLocation?,
    bearing: Float = 0f,
    onOpenOfflineMaps: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var mapStyle by remember { mutableStateOf(MapStyle.GOOGLE) }
    val mapState = remember { MapState() }
    val currentLocation by rememberUpdatedState(userLocation)
    val currentBearing by rememberUpdatedState(bearing)
    val currentStyle by rememberUpdatedState(mapStyle)

    val mapView = remember(context) { MapView(context) }

    // Lifecycle MapLibre
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START   -> mapView.onStart()
                Lifecycle.Event.ON_RESUME  -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE   -> mapView.onPause()
                Lifecycle.Event.ON_STOP    -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        mapView.onCreate(null)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    // Inisialisasi map + load style awal
    LaunchedEffect(mapView) {
        mapView.getMapAsync { map ->
            mapState.map = map
            map.uiSettings.apply {
                isCompassEnabled = false
                isLogoEnabled = false
                isAttributionEnabled = false
            }
            loadMapStyle(map, mapStyle, mapState, context)
        }
    }

    // Ganti style saat mode berubah
    LaunchedEffect(mapStyle) {
        val map = mapState.map ?: return@LaunchedEffect
        mapState.styleLoaded = false
        loadMapStyle(map, mapStyle, mapState, context)
    }

    // Update lokasi + bearing
    LaunchedEffect(userLocation, bearing, hasLocationPermission) {
        val map = mapState.map ?: return@LaunchedEffect
        if (!hasLocationPermission || userLocation == null) return@LaunchedEffect

        mapState.pendingLocation = userLocation
        mapState.pendingBearing = bearing

        if (!mapState.styleLoaded) return@LaunchedEffect

        applyLocationAndBearing(map, mapState, currentStyle)
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
        )

        // Kontrol peta di kanan bawah
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End,
        ) {
            MapLayerSwitchButton(
                currentStyle = mapStyle,
                onClick = {
                    mapStyle = when (mapStyle) {
                        MapStyle.GOOGLE -> MapStyle.OSM_GOOGLE_TRAFFIC
                        MapStyle.OSM_GOOGLE_TRAFFIC -> MapStyle.OSM
                        MapStyle.OSM -> MapStyle.GOOGLE
                    }
                },
            )
            MapControlButton(
                icon = Icons.Default.Download,
                contentDescription = "Peta offline",
                onClick = onOpenOfflineMaps,
            )
            MapControlButton(
                icon = Icons.Default.Add,
                contentDescription = "Perbesar",
                onClick = { mapState.map?.animateCamera(CameraUpdateFactory.zoomIn()) },
            )
            MapControlButton(
                icon = Icons.Default.Remove,
                contentDescription = "Perkecil",
                onClick = { mapState.map?.animateCamera(CameraUpdateFactory.zoomOut()) },
            )
            MapControlButton(
                icon = Icons.Default.MyLocation,
                contentDescription = "Lokasi saya",
                tintOverride = MaterialTheme.colorScheme.primary,
                onClick = {
                    userLocation?.let { loc ->
                        mapState.map?.animateCamera(
                            CameraUpdateFactory.newCameraPosition(
                                CameraPosition.Builder()
                                    .target(LatLng(loc.latitude, loc.longitude))
                                    .zoom(FOLLOW_ZOOM)
                                    .build(),
                            ),
                        )
                    }
                },
            )
        }
    }
}

private fun loadMapStyle(
    map: MapLibreMap,
    style: MapStyle,
    mapState: MapState,
    context: android.content.Context,
) {
    val dotBitmap = locationDotBitmap(72)
    val styleUri = when (style) {
        MapStyle.GOOGLE -> Style.Builder().fromJson(GOOGLE_STYLE_JSON)
        MapStyle.OSM_GOOGLE_TRAFFIC,
        MapStyle.OSM,
        -> Style.Builder().fromUri(OPEN_FREE_MAP_LIBERTY_STYLE_URL)
    }

    map.setStyle(styleUri) { loadedStyle ->
        loadedStyle.addImage(LOCATION_ICON_ID, dotBitmap)

        // OSM + traffic: overlay raster Google 20% di atas vector OpenFreeMap
        if (style == MapStyle.OSM_GOOGLE_TRAFFIC) {
            if (loadedStyle.getSource(OSM_TRAFFIC_SOURCE_ID) == null) {
                val tileSet = TileSet("2.1.0", *GOOGLE_TRAFFIC_TILE_URLS)
                loadedStyle.addSource(RasterSource(OSM_TRAFFIC_SOURCE_ID, tileSet, 256))
            }
            if (loadedStyle.getLayer(OSM_TRAFFIC_LAYER_ID) == null) {
                loadedStyle.addLayer(
                    RasterLayer(OSM_TRAFFIC_LAYER_ID, OSM_TRAFFIC_SOURCE_ID).withProperties(
                        rasterOpacity(0.2f),
                    ),
                )
            }
        }

        if (loadedStyle.getSource(LOCATION_SOURCE_ID) == null) {
            loadedStyle.addSource(GeoJsonSource(LOCATION_SOURCE_ID))
        }
        if (loadedStyle.getLayer(LOCATION_LAYER_ID) == null) {
            val useMapRotation = style == MapStyle.OSM || style == MapStyle.OSM_GOOGLE_TRAFFIC
            loadedStyle.addLayer(
                SymbolLayer(LOCATION_LAYER_ID, LOCATION_SOURCE_ID).withProperties(
                    iconImage(LOCATION_ICON_ID),
                    iconSize(1.0f),
                    iconAllowOverlap(true),
                    iconIgnorePlacement(true),
                    iconRotationAlignment(
                        if (useMapRotation) "map" else "viewport",
                    ),
                ),
            )
        }

        mapState.styleLoaded = true
        applyLocationAndBearing(map, mapState, style)
    }
}

private fun applyLocationAndBearing(
    map: MapLibreMap,
    mapState: MapState,
    mode: MapStyle,
) {
    val loc = mapState.pendingLocation ?: return
    val bearing = mapState.pendingBearing

    val mapStyleObj = map.style ?: return
    (mapStyleObj.getSource(LOCATION_SOURCE_ID) as? GeoJsonSource)
        ?.setGeoJson(Feature.fromGeometry(Point.fromLngLat(loc.longitude, loc.latitude)))

    // Kamera: mode vector OSM ikut kompas; Google north-up
    val cameraBearing = when (mode) {
        MapStyle.GOOGLE -> 0.0
        MapStyle.OSM_GOOGLE_TRAFFIC,
        MapStyle.OSM,
        -> bearing.toDouble()
    }

    val target = LatLng(loc.latitude, loc.longitude)
    val cameraBuilder = CameraPosition.Builder().target(target).bearing(cameraBearing)

    if (!mapState.didInitialZoom) {
        cameraBuilder.zoom(FOLLOW_ZOOM)
        mapState.didInitialZoom = true
        map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraBuilder.build()))
    } else {
        map.animateCamera(
            CameraUpdateFactory.newCameraPosition(cameraBuilder.zoom(map.cameraPosition.zoom).build()),
            300,
        )
    }
}

@Composable
private fun MapLayerSwitchButton(currentStyle: MapStyle, onClick: () -> Unit) {
    val isGoogle = currentStyle == MapStyle.GOOGLE
    val (label, labelSp) = when (currentStyle) {
        MapStyle.GOOGLE -> "G" to 10.sp
        MapStyle.OSM_GOOGLE_TRAFFIC -> "OSMG" to 7.sp
        MapStyle.OSM -> "OSM" to 8.sp
    }
    val desc = when (currentStyle) {
        MapStyle.GOOGLE -> "Google Maps + traffic"
        MapStyle.OSM_GOOGLE_TRAFFIC -> "OpenStreetMap + traffic"
        MapStyle.OSM -> "OpenStreetMap"
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isGoogle) MaterialTheme.colorScheme.primary.copy(alpha = 0.92f)
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            ),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = desc,
                    tint = if (isGoogle) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = label,
                    fontSize = labelSp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    color = if (isGoogle) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface,
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
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Icon(icon, contentDescription, tint = tintOverride, modifier = Modifier.size(22.dp))
        }
    }
}

/** Titik lokasi seperti Google Maps: lingkaran biru dengan halo putih. */
private fun locationDotBitmap(sizePx: Int): Bitmap {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val cx = sizePx / 2f
    val cy = sizePx / 2f
    val outer = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }
    val inner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#4285F4")
    }
    val outerR = sizePx * 0.42f
    val innerR = sizePx * 0.30f
    canvas.drawCircle(cx, cy, outerR, outer)
    canvas.drawCircle(cx, cy, innerR, inner)
    return bmp
}
