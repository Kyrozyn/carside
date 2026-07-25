package me.kyrozyn.carside.ui.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.kyrozyn.carside.data.location.FAB_STREET_MAX_CHARS
import me.kyrozyn.carside.data.location.reverseGeocodeResult
import me.kyrozyn.carside.data.map.OPEN_FREE_MAP_DARK_OFFLINE_STYLE_URL
import me.kyrozyn.carside.data.map.OPEN_FREE_MAP_DARK_STYLE_URL
import me.kyrozyn.carside.data.map.OPEN_FREE_MAP_LIBERTY_OFFLINE_STYLE_URL
import me.kyrozyn.carside.data.map.OPEN_FREE_MAP_LIBERTY_STYLE_URL
import me.kyrozyn.carside.domain.model.GeoLocation
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleOpacity
import org.maplibre.android.style.layers.PropertyFactory.circlePitchAlignment
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.iconKeepUpright
import org.maplibre.android.style.layers.PropertyFactory.iconOptional
import org.maplibre.android.style.layers.PropertyFactory.iconPitchAlignment
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

/** Jeda antar pembaruan nama jalan di FAB (gerak GPS tidak memicu geocode berulang). */
private const val STREET_GEOCODE_INTERVAL_MS = 60_000L

private const val FOLLOW_ZOOM = 17.0
private const val CAMERA_TILT = 35.0
private const val TOP_PADDING_RATIO = 0.42f
private const val LOCATION_SOURCE_ID = "location-source"
private const val LOCATION_LAYER_ID = "location-layer"
private const val LOCATION_CIRCLE_LAYER_ID = "location-circle-layer"
private const val LOCATION_ICON_ID = "location-dot"
private const val OSM_TRAFFIC_SOURCE_ID = "osm-google-traffic-source"
private const val OSM_TRAFFIC_LAYER_ID = "osm-google-traffic-layer"
private const val TABLET_MIN_WIDTH_DP = 600

private val GOOGLE_TRAFFIC_TILE_URLS = arrayOf(
    "https://mt0.google.com/vt/lyrs=m@221097413,traffic&x={x}&y={y}&z={z}",
    "https://mt1.google.com/vt/lyrs=m@221097413,traffic&x={x}&y={y}&z={z}",
    "https://mt2.google.com/vt/lyrs=m@221097413,traffic&x={x}&y={y}&z={z}",
    "https://mt3.google.com/vt/lyrs=m@221097413,traffic&x={x}&y={y}&z={z}",
)

private enum class MapStyle {
    /** Google raster + traffic */
    GOOGLE,
    /** Google raster + traffic, tampilan lebih gelap (paint raster) */
    GOOGLE_DARK,
    /** OpenFreeMap Liberty + overlay traffic Google */
    OSM_GOOGLE_TRAFFIC,
    /** OpenFreeMap dark + overlay traffic Google */
    OSM_GOOGLE_TRAFFIC_DARK,
    /** OpenFreeMap Liberty */
    OSM,
    /** OpenFreeMap dark */
    OSM_DARK,
}

// Style JSON untuk Google Maps raster + traffic ΓÇö teks tidak dirotasi karena raster
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

// Google + traffic dengan raster dipergelap (night-style tanpa API Google resmi)
private val GOOGLE_DARK_STYLE_JSON = """
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
    "source": "google-traffic",
    "paint": {
      "raster-brightness-min": 0.0,
      "raster-brightness-max": 0.35,
      "raster-contrast": 0.25,
      "raster-saturation": -0.45
    }
  }]
}
""".trimIndent()

// Vector tiles OpenFreeMap ΓÇö teks tetap tegak saat dirotasi

private class MapState {
    var map: MapLibreMap? = null
    var styleLoaded: Boolean = false
    var pendingLocation: GeoLocation? = null
    var pendingBearing: Float = 0f
    var didInitialZoom: Boolean = false
    /**
     * Setelah reload style Google (raster), hanya perbarui titik lokasi di peta;
     * kamera sudah diatur dari onClick tombol lokasi.
     */
    var skipCameraApplyLocation: Boolean = false
    var lastCameraBearing: Double? = null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPane(
    hasLocationPermission: Boolean,
    userLocation: GeoLocation?,
    bearing: Float = 0f,
    onNavigationClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    useOfflineStyle: Boolean = false,
    locationDotSize: Float = 60f,
    locationRefreshIntervalMs: Long = 60_000L,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var mapStyle by remember { mutableStateOf(MapStyle.OSM) }
    var showStreetSheet by remember { mutableStateOf(false) }
    var streetLoading by remember { mutableStateOf(false) }
    var streetResult by remember { mutableStateOf<String?>(null) }
    var streetError by remember { mutableStateOf<String?>(null) }
    var fabStreetLabel by remember { mutableStateOf("ΓÇö") }
    val streetSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val mapState = remember { MapState() }
    val currentLocation by rememberUpdatedState(userLocation)
    val currentBearing by rememberUpdatedState(bearing)
    val currentStyle by rememberUpdatedState(mapStyle)
    val latestLocationForFab by rememberUpdatedState(userLocation)
    val latestPermissionForFab by rememberUpdatedState(hasLocationPermission)

    val mapView = remember(context) {
        MapView(context).apply {
            addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
                val h = v.height
                if (h > 0) {
                    val topPadding = (h * TOP_PADDING_RATIO).toInt()
                    mapState.map?.setPadding(0, topPadding, 0, 0)
                }
            }
        }
    }

    val locationDotBitmap = remember(locationDotSize) {
        locationDotBitmap(locationDotSize.toInt())
    }

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
            val h = mapView.height
            if (h > 0) {
                val topPadding = (h * TOP_PADDING_RATIO).toInt()
                map.setPadding(0, topPadding, 0, 0)
            }
            loadMapStyle(map, mapStyle, mapState, context, useOfflineStyle, locationDotBitmap)
        }
    }

    // Ganti style saat mode berubah
    LaunchedEffect(mapStyle, locationDotBitmap) {
        val map = mapState.map ?: return@LaunchedEffect
        mapState.styleLoaded = false
        loadMapStyle(map, mapStyle, mapState, context, useOfflineStyle, locationDotBitmap)
    }

    // Update lokasi + bearing
    LaunchedEffect(userLocation, bearing, hasLocationPermission, mapState.styleLoaded) {
        val map = mapState.map ?: return@LaunchedEffect
        if (!hasLocationPermission || userLocation == null) return@LaunchedEffect

        mapState.pendingLocation = userLocation
        mapState.pendingBearing = bearing

        if (!mapState.styleLoaded) return@LaunchedEffect

        applyLocationAndBearing(map, mapState, currentStyle)
    }

    LaunchedEffect(hasLocationPermission) {
        if (!latestPermissionForFab) {
            fabStreetLabel = "Izin lokasi".truncateStreetLabel()
            return@LaunchedEffect
        }
        while (true) {
            if (!latestPermissionForFab) {
                fabStreetLabel = "Izin lokasi".truncateStreetLabel()
                return@LaunchedEffect
            }
            val loc = latestLocationForFab
            if (loc == null) {
                fabStreetLabel = "Tunggu GPSΓÇª".truncateStreetLabel()
                delay(1_000L)
                continue
            }
            val r = reverseGeocodeResult(
                context,
                loc.latitude,
                loc.longitude,
            )
            fabStreetLabel = when {
                r == null -> "Alamat tidak diketahui".truncateStreetLabel()
                else -> r.fabParts.fabLabel()
            }
            delay(locationRefreshIntervalMs)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
        )

        ExtendedFloatingActionButton(
            onClick = {
                showStreetSheet = true
                streetResult = null
                streetError = null
                when {
                    !hasLocationPermission -> {
                        streetLoading = false
                        streetError =
                            "Izin lokasi belum diberikan. Berikan izin di pengaturan aplikasi."
                    }
                    userLocation == null -> {
                        streetLoading = false
                        streetError = "Menunggu sinyal GPSΓÇª Coba lagi setelah titik lokasi muncul."
                    }
                    else -> {
                        streetLoading = true
                        val lat = userLocation.latitude
                        val lon = userLocation.longitude
                        scope.launch {
                            val r = reverseGeocodeResult(context, lat, lon)
                            streetLoading = false
                            if (r == null) {
                                streetError =
                                    "Tidak menemukan nama jalan di lokasi ini. Pastikan layanan lokasi aktif dan ada koneksi jaringan."
                            } else {
                                streetResult = r.fullAddressLine
                            }
                        }
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp, end = 64.dp)
                .widthIn(max = 320.dp)
                .heightIn(min = 40.dp),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            icon = {
                Icon(
                    Icons.Default.Place,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
            text = {
                Text(
                    text = fabStreetLabel,
                    maxLines = 2,
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 11.sp,
                )
            },
        )

        if (showStreetSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showStreetSheet = false
                    streetLoading = false
                    streetResult = null
                    streetError = null
                },
                sheetState = streetSheetState,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp),
                ) {
                    Text(
                        text = "Informasi nama jalan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(12.dp))
                    when {
                        streetLoading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Mencari alamat dari lokasi saat iniΓÇª",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        streetError != null -> {
                            Text(
                                text = streetError!!,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        streetResult != null -> {
                            Text(
                                text = streetResult!!,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Start,
                            )
                        }
                    }
                }
            }
        }

        // Kontrol peta di kanan bawah
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End,
        ) {
            if (onNavigationClick != null) {
                MapControlButton(
                    icon = Icons.Default.Navigation,
                    contentDescription = "Kembali ke navigasi",
                    tintOverride = MaterialTheme.colorScheme.primary,
                    onClick = onNavigationClick,
                )
            }
            MapLayerSwitchButton(
                currentStyle = mapStyle,
                onClick = {
                    mapStyle = when (mapStyle) {
                        MapStyle.GOOGLE -> MapStyle.GOOGLE_DARK
                        MapStyle.GOOGLE_DARK -> MapStyle.OSM_GOOGLE_TRAFFIC
                        MapStyle.OSM_GOOGLE_TRAFFIC -> MapStyle.OSM_GOOGLE_TRAFFIC_DARK
                        MapStyle.OSM_GOOGLE_TRAFFIC_DARK -> MapStyle.OSM
                        MapStyle.OSM -> MapStyle.OSM_DARK
                        MapStyle.OSM_DARK -> MapStyle.GOOGLE
                    }
                },
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
                        mapState.map?.let { map ->
                            if (mapStyle == MapStyle.GOOGLE || mapStyle == MapStyle.GOOGLE_DARK) {
                                mapState.styleLoaded = false
                                mapState.skipCameraApplyLocation = true
                                loadMapStyle(map, mapStyle, mapState, context, useOfflineStyle, locationDotBitmap)
                            }
                            map.animateCamera(
                                CameraUpdateFactory.newCameraPosition(
                                    CameraPosition.Builder()
                                        .target(LatLng(loc.latitude, loc.longitude))
                                        .zoom(FOLLOW_ZOOM)
                                        .tilt(CAMERA_TILT)
                                        .build(),
                                ),
                            )
                        }
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
    useOfflineStyle: Boolean = false,
    dotBitmap: android.graphics.Bitmap,
) {
    val locationDotScale = locationDotScale(context)
    val styleUri = when (style) {
        MapStyle.GOOGLE -> Style.Builder().fromJson(GOOGLE_STYLE_JSON)
        MapStyle.GOOGLE_DARK -> Style.Builder().fromJson(GOOGLE_DARK_STYLE_JSON)
        MapStyle.OSM_GOOGLE_TRAFFIC,
        MapStyle.OSM,
        -> Style.Builder().fromUri(
            if (useOfflineStyle) OPEN_FREE_MAP_LIBERTY_OFFLINE_STYLE_URL
            else OPEN_FREE_MAP_LIBERTY_STYLE_URL
        )
        MapStyle.OSM_GOOGLE_TRAFFIC_DARK,
        MapStyle.OSM_DARK,
        -> Style.Builder().fromUri(
            if (useOfflineStyle) OPEN_FREE_MAP_DARK_OFFLINE_STYLE_URL
            else OPEN_FREE_MAP_DARK_STYLE_URL
        )
    }

    map.setStyle(styleUri) { loadedStyle ->
        loadedStyle.addImage(LOCATION_ICON_ID, dotBitmap)

        // OSM + traffic: overlay raster Google 20% di atas vector OpenFreeMap (terang atau dark)
        if (style == MapStyle.OSM_GOOGLE_TRAFFIC || style == MapStyle.OSM_GOOGLE_TRAFFIC_DARK) {
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

        // Remove old layer and source if they exist
        loadedStyle.getLayer(LOCATION_LAYER_ID)?.let { loadedStyle.removeLayer(it) }
        loadedStyle.getLayer(LOCATION_CIRCLE_LAYER_ID)?.let { loadedStyle.removeLayer(it) }
        loadedStyle.getSource(LOCATION_SOURCE_ID)?.let { loadedStyle.removeSource(it) }

        // Always recreate location source and layers
        val locationSource = GeoJsonSource(LOCATION_SOURCE_ID)
        loadedStyle.addSource(locationSource)

        val useMapRotation = when (style) {
            MapStyle.GOOGLE,
            MapStyle.GOOGLE_DARK,
            -> false
            MapStyle.OSM,
            MapStyle.OSM_DARK,
            MapStyle.OSM_GOOGLE_TRAFFIC,
            MapStyle.OSM_GOOGLE_TRAFFIC_DARK,
            -> true
        }

        // Layer 1: Vector CircleLayer (GPU native fallback, anti-hilang)
        val circleLayer = CircleLayer(LOCATION_CIRCLE_LAYER_ID, LOCATION_SOURCE_ID).withProperties(
            circleColor("#4285F4"),
            circleRadius(13f),
            circleStrokeColor("#FFFFFF"),
            circleStrokeWidth(3f),
            circleOpacity(0.40f),
            circlePitchAlignment("map"),
        )
        loadedStyle.addLayer(circleLayer)

        // Layer 2: SymbolLayer (ikonic blue dot + halo)
        val symbolLayer = SymbolLayer(LOCATION_LAYER_ID, LOCATION_SOURCE_ID).withProperties(
            iconImage(LOCATION_ICON_ID),
            iconSize(locationDotScale),
            iconAllowOverlap(true),
            iconIgnorePlacement(true),
            iconOptional(false),
            iconKeepUpright(false),
            iconRotationAlignment(if (useMapRotation) "map" else "viewport"),
            iconPitchAlignment(if (useMapRotation) "map" else "viewport"),
        )
        loadedStyle.addLayer(symbolLayer)

        mapState.styleLoaded = true
        applyLocationAndBearing(map, mapState, style)
    }
}

private fun locationDotScale(context: android.content.Context): Float {
    val smallestWidthDp = context.resources.configuration.smallestScreenWidthDp
    return if (smallestWidthDp >= TABLET_MIN_WIDTH_DP) 0.72f else 1.0f
}

private fun applyLocationAndBearing(
    map: MapLibreMap,
    mapState: MapState,
    mode: MapStyle,
) {
    val loc = mapState.pendingLocation ?: return
    val bearing = mapState.pendingBearing

    val mapStyleObj = map.style ?: return
    val source = mapStyleObj.getSource(LOCATION_SOURCE_ID) as? GeoJsonSource
    if (source == null) {
        android.util.Log.w("MapPane", "Location source not found, skipping location update")
        return
    }
    source.setGeoJson(Feature.fromGeometry(Point.fromLngLat(loc.longitude, loc.latitude)))

    if (mapState.skipCameraApplyLocation) {
        mapState.skipCameraApplyLocation = false
        return
    }

    // Kamera: mode vector OSM ikut kompas; Google north-up
    val rawCameraBearing = when (mode) {
        MapStyle.GOOGLE,
        MapStyle.GOOGLE_DARK,
        -> 0.0
        MapStyle.OSM_GOOGLE_TRAFFIC,
        MapStyle.OSM_GOOGLE_TRAFFIC_DARK,
        MapStyle.OSM,
        MapStyle.OSM_DARK,
        -> bearing.toDouble()
    }

    val cameraBearing = if (mode == MapStyle.GOOGLE || mode == MapStyle.GOOGLE_DARK) {
        0.0
    } else {
        val last = mapState.lastCameraBearing
        if (last != null) {
            var diff = (rawCameraBearing - last) % 360.0
            if (diff > 180.0) diff -= 360.0
            if (diff < -180.0) diff += 360.0
            // Haluskan rotasi: abaikan jitter kompas < 1.5 derajat saat kendaraan pelan agar peta tidak goyang
            if (kotlin.math.abs(diff) < 1.5 && (loc.speedMps ?: 0f) < 2.5f) {
                last
            } else {
                mapState.lastCameraBearing = rawCameraBearing
                rawCameraBearing
            }
        } else {
            mapState.lastCameraBearing = rawCameraBearing
            rawCameraBearing
        }
    }

    val target = LatLng(loc.latitude, loc.longitude)
    val cameraBuilder = CameraPosition.Builder()
        .target(target)
        .bearing(cameraBearing)
        .tilt(CAMERA_TILT)

    if (!mapState.didInitialZoom) {
        cameraBuilder.zoom(FOLLOW_ZOOM)
        mapState.didInitialZoom = true
        map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraBuilder.build()))
    } else {
        val currentZoom = map.cameraPosition.zoom.coerceAtLeast(10.0)
        map.easeCamera(
            CameraUpdateFactory.newCameraPosition(cameraBuilder.zoom(currentZoom).build()),
            800,
        )
    }
}

@Composable
private fun MapLayerSwitchButton(currentStyle: MapStyle, onClick: () -> Unit) {
    val isGoogle = currentStyle == MapStyle.GOOGLE || currentStyle == MapStyle.GOOGLE_DARK
    val (label, labelSp) = when (currentStyle) {
        MapStyle.GOOGLE -> "G" to 10.sp
        MapStyle.GOOGLE_DARK -> "GD" to 9.sp
        MapStyle.OSM_GOOGLE_TRAFFIC -> "OSMG" to 6.sp
        MapStyle.OSM_GOOGLE_TRAFFIC_DARK -> "OSMGD" to 5.5.sp
        MapStyle.OSM -> "OSM" to 8.sp
        MapStyle.OSM_DARK -> "OSMD" to 7.sp
    }
    val desc = when (currentStyle) {
        MapStyle.GOOGLE -> "Google Maps + traffic"
        MapStyle.GOOGLE_DARK -> "Google Maps + traffic (gelap)"
        MapStyle.OSM_GOOGLE_TRAFFIC -> "OpenStreetMap + traffic"
        MapStyle.OSM_GOOGLE_TRAFFIC_DARK -> "OpenStreetMap dark + traffic"
        MapStyle.OSM -> "OpenStreetMap"
        MapStyle.OSM_DARK -> "OpenStreetMap dark"
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

/** Maksimal [FAB_STREET_MAX_CHARS] karakter; jika lebih, dipotong dan ditambah ellipsis. */
private fun String.truncateStreetLabel(maxChars: Int = FAB_STREET_MAX_CHARS): String {
    val t = trim()
    if (t.isEmpty()) return "ΓÇö".take(maxChars.coerceAtLeast(1))
    if (t.length <= maxChars) return t
    return t.take(maxChars - 1) + "ΓÇª"
}
