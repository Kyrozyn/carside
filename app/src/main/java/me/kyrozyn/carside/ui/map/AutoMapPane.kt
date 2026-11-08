package me.kyrozyn.carside.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Traffic
import androidx.compose.material.icons.filled.TurnLeft
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.model.LatLng
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import me.kyrozyn.carside.core.AutoPlaceResult
import me.kyrozyn.carside.core.AutoRouteResult
import me.kyrozyn.carside.core.GoogleMapsHelper
import me.kyrozyn.carside.data.navigation.AutoNavState
import me.kyrozyn.carside.data.navigation.AutoNavigationManager
import me.kyrozyn.carside.domain.model.GeoLocation
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng as MapLibreLatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

private const val SOURCE_ACTIVE_ROUTE = "auto-active-route-source"
private const val LAYER_ACTIVE_ROUTE = "auto-active-route-layer"
private const val SOURCE_ALT_ROUTE_PREFIX = "auto-alt-route-source-"
private const val LAYER_ALT_ROUTE_PREFIX = "auto-alt-route-layer-"
private const val SOURCE_VEHICLE = "auto-vehicle-source"
private const val LAYER_VEHICLE = "auto-vehicle-layer"
private const val SOURCE_DEST = "auto-dest-source"
private const val LAYER_DEST = "auto-dest-layer"
private const val ICON_NAV_ARROW = "icon-nav-arrow"
private const val ICON_LOCATION_DOT = "icon-location-dot"
private const val ICON_DEST_PIN = "icon-dest-pin"

// Style Google Maps dengan lalu lintas (Traffic)
private val GOOGLE_TRAFFIC_STYLE_JSON = """
{
  "version": 8,
  "sources": {
    "google-tiles": {
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
    "id": "google-tiles-layer",
    "type": "raster",
    "source": "google-tiles"
  }]
}
""".trimIndent()

// Style Google Maps tanpa layer traffic
private val GOOGLE_NO_TRAFFIC_STYLE_JSON = """
{
  "version": 8,
  "sources": {
    "google-tiles": {
      "type": "raster",
      "tiles": [
        "https://mt0.google.com/vt/lyrs=m&x={x}&y={y}&z={z}",
        "https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={z}",
        "https://mt2.google.com/vt/lyrs=m&x={x}&y={y}&z={z}",
        "https://mt3.google.com/vt/lyrs=m&x={x}&y={y}&z={z}"
      ],
      "tileSize": 256
    }
  },
  "layers": [{
    "id": "google-tiles-layer",
    "type": "raster",
    "source": "google-tiles"
  }]
}
""".trimIndent()

// Style Google Maps Mode Gelap dengan traffic
private val GOOGLE_DARK_TRAFFIC_STYLE_JSON = """
{
  "version": 8,
  "sources": {
    "google-tiles": {
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
    "id": "google-tiles-layer",
    "type": "raster",
    "source": "google-tiles",
    "paint": {
      "raster-brightness-min": 0.0,
      "raster-brightness-max": 0.35,
      "raster-contrast": 0.25,
      "raster-saturation": -0.45
    }
  }]
}
""".trimIndent()

// Style Google Maps Mode Gelap tanpa traffic
private val GOOGLE_DARK_NO_TRAFFIC_STYLE_JSON = """
{
  "version": 8,
  "sources": {
    "google-tiles": {
      "type": "raster",
      "tiles": [
        "https://mt0.google.com/vt/lyrs=m&x={x}&y={y}&z={z}",
        "https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={z}",
        "https://mt2.google.com/vt/lyrs=m&x={x}&y={y}&z={z}",
        "https://mt3.google.com/vt/lyrs=m&x={x}&y={y}&z={z}"
      ],
      "tileSize": 256
    }
  },
  "layers": [{
    "id": "google-tiles-layer",
    "type": "raster",
    "source": "google-tiles",
    "paint": {
      "raster-brightness-min": 0.0,
      "raster-brightness-max": 0.35,
      "raster-contrast": 0.25,
      "raster-saturation": -0.45
    }
  }]
}
""".trimIndent()

// Style Google Maps Satelit / Hybrid dengan traffic
private val GOOGLE_HYBRID_TRAFFIC_STYLE_JSON = """
{
  "version": 8,
  "sources": {
    "google-tiles": {
      "type": "raster",
      "tiles": [
        "https://mt0.google.com/vt/lyrs=y,traffic&x={x}&y={y}&z={z}",
        "https://mt1.google.com/vt/lyrs=y,traffic&x={x}&y={y}&z={z}",
        "https://mt2.google.com/vt/lyrs=y,traffic&x={x}&y={y}&z={z}",
        "https://mt3.google.com/vt/lyrs=y,traffic&x={x}&y={y}&z={z}"
      ],
      "tileSize": 256
    }
  },
  "layers": [{
    "id": "google-tiles-layer",
    "type": "raster",
    "source": "google-tiles"
  }]
}
""".trimIndent()

/**
 * Tampilan Peta bergaya Android Auto (Coolwalk UI).
 * Berjalan tanpa crash dan tanpa layar blank pada seluruh Head Unit Android (dengan atau tanpa Google Play Services),
 * mendukung pencarian alamat/tempat, rute navigasi dengan directions, petunjuk manuver belokan,
 * serta kontrol peta interaktif langsung di head unit.
 */
@Composable
fun AutoMapPane(
    hasLocationPermission: Boolean,
    userLocation: GeoLocation?,
    bearing: Float,
    apiKey: String?,
    onOpenSettings: () -> Unit,
    autoNavigationManager: AutoNavigationManager? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()

    // Sesi navigasi terpusat (persisten saat navigasi antar halaman)
    val fallbackNavState = remember { MutableStateFlow(AutoNavState()) }
    val navState by (autoNavigationManager?.navState ?: fallbackNavState).collectAsStateWithLifecycle()
    val isNavigating = navState.isNavigating
    val selectedPlace = navState.selectedPlace
    val availableRoutes = navState.availableRoutes
    val selectedRouteIndex = navState.selectedRouteIndex
    val activeRoute = navState.activeRoute
    val currentStepIndex = navState.currentStepIndex

    // Map instances & state
    var mapRef by remember { mutableStateOf<MapLibreMap?>(null) }
    var isStyleLoaded by remember { mutableStateOf(false) }
    var isTrafficEnabled by remember { mutableStateOf(true) }
    var isSatellite by remember { mutableStateOf(false) }

    // Search state
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<AutoPlaceResult>>(emptyList()) }
    var isCalculatingRoute by remember { mutableStateOf(false) }

    // Hitung arah hadap kendaraan (heading) yang efektif
    val effectiveBearing = remember(isNavigating, activeRoute, currentStepIndex, bearing) {
        if (isNavigating && activeRoute != null) {
            val steps = activeRoute.steps
            val routeHeading = if (currentStepIndex < steps.size - 1 && steps.isNotEmpty()) {
                val from = steps[currentStepIndex].latLng
                val to = steps[currentStepIndex + 1].latLng
                calculateBearing(from.latitude, from.longitude, to.latitude, to.longitude)
            } else {
                calculateInitialRouteBearing(activeRoute.points)
            }
            if (bearing > 0f) bearing else routeHeading
        } else {
            bearing
        }
    }

    // Inisialisasi MapView MapLibre
    val mapView = remember(context) {
        MapView(context).apply {
            addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
                val h = v.height
                if (h > 0) {
                    val topPadding = (h * 0.52f).toInt()
                    mapRef?.setPadding(0, topPadding, 0, 0)
                }
            }
        }
    }

    // Kelola siklus hidup MapView secara aman di Jetpack Compose
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        mapView.onCreate(null)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            mapView.onStart()
        }
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            mapView.onResume()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    // Muat style peta saat pertama kali siap atau saat mode (Gelap / Satelit / Trafik) berubah
    LaunchedEffect(mapRef, isDark, isSatellite, isTrafficEnabled) {
        val map = mapRef ?: return@LaunchedEffect
        isStyleLoaded = false
        val styleJson = getStyleJson(isDark, isSatellite, isTrafficEnabled)
        map.setStyle(Style.Builder().fromJson(styleJson)) { loadedStyle ->
            setupStyleLayers(loadedStyle, context)
            isStyleLoaded = true
        }
    }

    // Kamera mengikuti lokasi & rute
    LaunchedEffect(mapRef, isStyleLoaded, userLocation, isNavigating, activeRoute, currentStepIndex, bearing, effectiveBearing) {
        val map = mapRef ?: return@LaunchedEffect
        if (!isStyleLoaded) return@LaunchedEffect

        if (!isNavigating && activeRoute == null) {
            val loc = userLocation
            if (loc != null) {
                val cameraPosition = CameraPosition.Builder()
                    .target(MapLibreLatLng(loc.latitude, loc.longitude))
                    .zoom(16.5)
                    .bearing(bearing.toDouble())
                    .tilt(if (bearing > 0f) 25.0 else 0.0)
                    .build()
                map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 800)
            }
        } else if (isNavigating && activeRoute != null) {
            val route = activeRoute
            val steps = route.steps
            val currentStep = steps.getOrNull(currentStepIndex)

            // Target koordinat navigasi (jika GPS tidak ada di headunit, gunakan titik rute)
            val navTarget = if (userLocation != null) {
                LatLng(userLocation.latitude, userLocation.longitude)
            } else if (currentStep != null && currentStepIndex > 0) {
                currentStep.latLng
            } else {
                route.points.firstOrNull() ?: LatLng(-6.1754, 106.8272)
            }

            // Close-up Zoom & 3D Tilt khas Google Maps Driving Mode
            val cameraPosition = CameraPosition.Builder()
                .target(MapLibreLatLng(navTarget.latitude, navTarget.longitude))
                .zoom(18.0)
                .bearing(effectiveBearing.toDouble())
                .tilt(50.0)
                .build()
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 1000)
        }
    }

    // Perbarui penanda kendaraan (Vehicle Marker) dan penanda tujuan (Destination Marker)
    LaunchedEffect(mapRef, isStyleLoaded, userLocation, isNavigating, activeRoute, currentStepIndex, effectiveBearing, selectedPlace) {
        val map = mapRef ?: return@LaunchedEffect
        if (!isStyleLoaded) return@LaunchedEffect
        val style = map.style ?: return@LaunchedEffect

        // Destination Marker
        val destSource = style.getSource(SOURCE_DEST) as? GeoJsonSource
        val place = selectedPlace
        if (place != null) {
            destSource?.setGeoJson(Point.fromLngLat(place.latLng.longitude, place.latLng.latitude))
        } else {
            destSource?.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
        }

        // Vehicle Marker
        val navTarget = if (isNavigating && activeRoute != null) {
            val steps = activeRoute.steps
            val currentStep = steps.getOrNull(currentStepIndex)
            if (userLocation != null) {
                LatLng(userLocation.latitude, userLocation.longitude)
            } else if (currentStep != null && currentStepIndex > 0) {
                currentStep.latLng
            } else {
                activeRoute.points.firstOrNull() ?: LatLng(-6.1754, 106.8272)
            }
        } else if (userLocation != null) {
            LatLng(userLocation.latitude, userLocation.longitude)
        } else null

        val vehicleSource = style.getSource(SOURCE_VEHICLE) as? GeoJsonSource
        val vehicleLayer = style.getLayer(LAYER_VEHICLE) as? SymbolLayer
        if (navTarget != null) {
            vehicleSource?.setGeoJson(Point.fromLngLat(navTarget.longitude, navTarget.latitude))
            if (isNavigating) {
                vehicleLayer?.setProperties(
                    PropertyFactory.iconImage(ICON_NAV_ARROW),
                    PropertyFactory.iconRotate(effectiveBearing),
                    PropertyFactory.iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP),
                )
            } else {
                vehicleLayer?.setProperties(
                    PropertyFactory.iconImage(ICON_LOCATION_DOT),
                    PropertyFactory.iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_VIEWPORT),
                )
            }
        } else {
            vehicleSource?.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
        }
    }

    // Perbarui rute polyline di peta saat availableRoutes, selectedRouteIndex, atau isNavigating berubah
    LaunchedEffect(mapRef, isStyleLoaded, availableRoutes, selectedRouteIndex, isNavigating) {
        val map = mapRef ?: return@LaunchedEffect
        if (!isStyleLoaded) return@LaunchedEffect
        val style = map.style ?: return@LaunchedEffect

        val activeSource = style.getSource(SOURCE_ACTIVE_ROUTE) as? GeoJsonSource
        val currentRoute = if (isNavigating) {
            activeRoute ?: availableRoutes.getOrNull(selectedRouteIndex)
        } else {
            availableRoutes.getOrNull(selectedRouteIndex)
        }

        if (currentRoute != null && currentRoute.points.isNotEmpty()) {
            val coords = currentRoute.points.map { Point.fromLngLat(it.longitude, it.latitude) }
            activeSource?.setGeoJson(Feature.fromGeometry(LineString.fromLngLats(coords)))
        } else {
            activeSource?.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
        }

        // Rute alternatif (hanya aktif saat dalam mode pratinjau rute)
        for (i in 0 until 5) {
            val srcId = "$SOURCE_ALT_ROUTE_PREFIX$i"
            val altSource = style.getSource(srcId) as? GeoJsonSource
            if (!isNavigating && i < availableRoutes.size && i != selectedRouteIndex) {
                val r = availableRoutes[i]
                val coords = r.points.map { Point.fromLngLat(it.longitude, it.latitude) }
                altSource?.setGeoJson(Feature.fromGeometry(LineString.fromLngLats(coords)))
            } else {
                altSource?.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
            }
        }

        // Zoom kamera mencakup seluruh rute alternatif yang tersedia saat pemilihan rute
        if (!isNavigating && availableRoutes.isNotEmpty()) {
            try {
                val boundsBuilder = LatLngBounds.Builder()
                var count = 0
                availableRoutes.forEach { r ->
                    r.points.forEach { pt ->
                        boundsBuilder.include(MapLibreLatLng(pt.latitude, pt.longitude))
                        count++
                    }
                }
                if (count > 0) {
                    map.easeCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120))
                }
            } catch (_: Exception) {}
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Tampilan Peta MapView
        AndroidView(
            factory = {
                mapView.getMapAsync { map ->
                    mapRef = map
                    map.uiSettings.isCompassEnabled = true
                    map.uiSettings.isLogoEnabled = false
                    map.uiSettings.isAttributionEnabled = false

                    val h = mapView.height
                    if (h > 0) {
                        val topPadding = (h * 0.52f).toInt()
                        map.setPadding(0, topPadding, 0, 0)
                    }

                    // Posisi awal kamera (Monas/Jakarta jika GPS belum aktif)
                    val startPos = if (userLocation != null) {
                        MapLibreLatLng(userLocation.latitude, userLocation.longitude)
                    } else {
                        MapLibreLatLng(-6.1754, 106.8272)
                    }
                    val initialCamera = CameraPosition.Builder()
                        .target(startPos)
                        .zoom(16.5)
                        .bearing(bearing.toDouble())
                        .tilt(if (bearing > 0f) 25.0 else 0.0)
                        .build()
                    map.moveCamera(CameraUpdateFactory.newCameraPosition(initialCamera))
                }
                mapView
            },
            modifier = Modifier.fillMaxSize(),
        )

        // ── TOP BAR: Pencarian Android Auto & Kategori Cepat ───────────
        if (!isNavigating) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(start = 124.dp, top = 12.dp, end = 12.dp),
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    shadowElevation = 6.dp,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Cari",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { query ->
                                searchQuery = query
                                if (query.isNotBlank()) {
                                    isSearching = true
                                    coroutineScope.launch {
                                        searchResults = GoogleMapsHelper.searchPlaces(
                                            context,
                                            query,
                                            userLocation,
                                            apiKey,
                                        )
                                        isSearching = false
                                    }
                                } else {
                                    searchResults = emptyList()
                                }
                            },
                            placeholder = {
                                Text(
                                    "Cari tempat atau alamat...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                            ),
                        )
                        if (searchQuery.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    searchQuery = ""
                                    searchResults = emptyList()
                                },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Hapus",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }

            // Banner pemberitahuan jika API Key belum diisi
            if (apiKey.isNullOrBlank()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .padding(start = 12.dp, top = 68.dp, end = 12.dp)
                        .clickable { onOpenSettings() },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.95f),
                    shadowElevation = 4.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Mode Auto aktif. Masukkan Google Maps API Key di Pengaturan untuk rute & navigasi akurat.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.weight(1f),
                        )
                        Button(
                            onClick = onOpenSettings,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                            ),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text("Pengaturan", fontSize = 12.sp)
                        }
                    }
                }
            } else {
                // Chips Kategori Cepat (SPBU, Restoran, Parkir, Kafe)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .padding(start = 12.dp, top = 68.dp, end = 12.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    QuickCategoryChip("⛽ SPBU") {
                        searchQuery = "SPBU terdekat"
                        coroutineScope.launch {
                            isSearching = true
                            searchResults = GoogleMapsHelper.searchPlaces(context, "SPBU", userLocation, apiKey)
                            isSearching = false
                        }
                    }
                    QuickCategoryChip("🍽️ Restoran") {
                        searchQuery = "Restoran terdekat"
                        coroutineScope.launch {
                            isSearching = true
                            searchResults = GoogleMapsHelper.searchPlaces(context, "Restoran", userLocation, apiKey)
                            isSearching = false
                        }
                    }
                    QuickCategoryChip("🅿️ Parkir") {
                        searchQuery = "Tempat parkir"
                        coroutineScope.launch {
                            isSearching = true
                            searchResults = GoogleMapsHelper.searchPlaces(context, "Parkir", userLocation, apiKey)
                            isSearching = false
                        }
                    }
                    QuickCategoryChip("☕ Kafe") {
                        searchQuery = "Kafe terdekat"
                        coroutineScope.launch {
                            isSearching = true
                            searchResults = GoogleMapsHelper.searchPlaces(context, "Kafe", userLocation, apiKey)
                            isSearching = false
                        }
                    }
                }
            }

            // Hasil Pencarian Floating
            AnimatedVisibility(
                visible = searchResults.isNotEmpty() || isSearching,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically(),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(start = 12.dp, top = 114.dp, end = 12.dp),
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                    shadowElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    ),
                ) {
                    if (isSearching) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        }
                    } else {
                        LazyColumn {
                            items(searchResults) { place ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            searchResults = emptyList()
                                            searchQuery = place.name
                                            val startLatLng = userLocation?.let { LatLng(it.latitude, it.longitude) }
                                                ?: mapRef?.cameraPosition?.target?.let { LatLng(it.latitude, it.longitude) }
                                                ?: LatLng(-6.1754, 106.8272)
                                            isCalculatingRoute = true
                                            coroutineScope.launch {
                                                val routes = GoogleMapsHelper.getRoutes(
                                                    startLatLng,
                                                    place.latLng,
                                                    apiKey,
                                                )
                                                autoNavigationManager?.setPreviewRoutes(place, routes)
                                                isCalculatingRoute = false
                                            }
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp),
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = place.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = place.address,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            }
                        }
                    }
                }
            }
        }

        // ── FLOATING CONTROLS (Pojok Kanan Peta) ───────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // My Location
            FloatingControlIconButton(
                icon = Icons.Default.MyLocation,
                contentDescription = "Pusatkan Lokasi",
            ) {
                if (userLocation != null && mapRef != null) {
                    val cameraPosition = CameraPosition.Builder()
                        .target(MapLibreLatLng(userLocation.latitude, userLocation.longitude))
                        .zoom(if (isNavigating) 18.0 else 16.5)
                        .bearing(if (isNavigating) effectiveBearing.toDouble() else bearing.toDouble())
                        .tilt(if (isNavigating) 50.0 else 25.0)
                        .build()
                    mapRef?.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                }
            }

            // Traffic Layer Toggle
            FloatingControlIconButton(
                icon = Icons.Default.Traffic,
                contentDescription = "Lalu Lintas",
                tint = if (isTrafficEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                isTrafficEnabled = !isTrafficEnabled
            }

            // Satellite / Map Type Toggle
            FloatingControlIconButton(
                icon = Icons.Default.Layers,
                contentDescription = "Tipe Peta",
                tint = if (isSatellite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                isSatellite = !isSatellite
            }
        }

        // ── CARD DETAIL TUJUAN & RUTE (Android Auto Style Bottom Sheet) ──
        if (selectedPlace != null && !isNavigating) {
            val place = selectedPlace
            val route = activeRoute

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                shadowElevation = 10.dp,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                ),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = place.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = place.address,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(
                            onClick = {
                                autoNavigationManager?.stopNavigation()
                            },
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup")
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    if (isCalculatingRoute) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text("Mencari seluruh rute perjalanan...", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else if (availableRoutes.isNotEmpty()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (availableRoutes.size > 1) {
                                Text(
                                    text = "Pilih Rute (${availableRoutes.size} rute tersedia):",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 8.dp),
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    availableRoutes.forEachIndexed { idx, r ->
                                        val isSelected = idx == selectedRouteIndex
                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { autoNavigationManager?.selectRoute(idx) },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            },
                                            border = androidx.compose.foundation.BorderStroke(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                                },
                                            ),
                                        ) {
                                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = r.durationText,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) {
                                                            MaterialTheme.colorScheme.primary
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurface
                                                        },
                                                    )
                                                    if (idx == 0) {
                                                        Spacer(Modifier.width(6.dp))
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                        ) {
                                                            Text(
                                                                text = "Tercepat",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.primary,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                            )
                                                        }
                                                    }
                                                }
                                                val viaText = if (r.summary.isNotBlank()) "via ${r.summary}" else "Rute ${idx + 1}"
                                                Text(
                                                    text = "${r.distanceText} • $viaText",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                            }

                            // Detail rute yang dipilih + Tombol Mulai Navigasi
                            val selRoute = activeRoute ?: availableRoutes.first()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = selRoute.durationText,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "(${selRoute.distanceText})",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    if (selRoute.summary.isNotBlank()) {
                                        Text(
                                            text = "via ${selRoute.summary}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        autoNavigationManager?.startNavigation()
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                    ),
                                ) {
                                    Icon(
                                        Icons.Default.DirectionsCar,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Mulai Navigasi", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                val startLatLng = userLocation?.let { LatLng(it.latitude, it.longitude) }
                                    ?: mapRef?.cameraPosition?.target?.let { LatLng(it.latitude, it.longitude) }
                                    ?: LatLng(-6.1754, 106.8272)
                                isCalculatingRoute = true
                                coroutineScope.launch {
                                    val routes = GoogleMapsHelper.getRoutes(
                                        startLatLng,
                                        place.latLng,
                                        apiKey,
                                    )
                                    autoNavigationManager?.setPreviewRoutes(place, routes)
                                    isCalculatingRoute = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Default.Directions, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Dapatkan Petunjuk Arah")
                        }
                    }
                }
            }
        }

        // ── MODE NAVIGASI AKTIF: Tampilan Resmi Android Auto Coolwalk ────────
        if (isNavigating && activeRoute != null) {
            val route = activeRoute!!
            val steps = route.steps
            val currentStep = steps.getOrNull(currentStepIndex) ?: steps.firstOrNull()
            val nextStep = steps.getOrNull(currentStepIndex + 1)

            // 1. KARTU MANUVER HIJAU (Pojok Kiri Atas ala Android Auto)
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp, top = 66.dp)
                    .widthIn(max = 380.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (nextStep != null) 0.dp else 16.dp,
                        bottomEnd = 16.dp,
                    ),
                    color = Color(0xFF0F5A47), // Hijau tua Android Auto
                    shadowElevation = 8.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ManeuverIcon(
                            maneuver = currentStep?.maneuver,
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentStep?.instruction?.ifBlank { "Ikuti jalan terus" } ?: "Ikuti rute ke ${selectedPlace?.name ?: route.endAddress}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (!currentStep?.distanceText.isNullOrBlank()) {
                                Text(
                                    text = currentStep?.distanceText.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f),
                                )
                            }
                        }

                        // Tombol panah langkah rute (berguna terutama jika headunit tanpa GPS)
                        if (steps.size > 1) {
                            Spacer(Modifier.width(6.dp))
                            IconButton(
                                onClick = { autoNavigationManager?.previousStep() },
                                enabled = currentStepIndex > 0,
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = if (currentStepIndex > 0) 0.2f else 0.05f)),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Sebelumnya",
                                    tint = if (currentStepIndex > 0) Color.White else Color.White.copy(alpha = 0.3f),
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                            IconButton(
                                onClick = { autoNavigationManager?.nextStep() },
                                enabled = currentStepIndex < steps.size - 1,
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = if (currentStepIndex < steps.size - 1) 0.2f else 0.05f)),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Berikutnya",
                                    tint = if (currentStepIndex < steps.size - 1) Color.White else Color.White.copy(alpha = 0.3f),
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }

                // Sub-Box "Then" jika ada langkah rute selanjutnya
                if (nextStep != null) {
                    Surface(
                        shape = RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp),
                        color = Color(0xFF093D30),
                        shadowElevation = 4.dp,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Then",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.9f),
                            )
                            Spacer(Modifier.width(6.dp))
                            ManeuverIcon(
                                maneuver = nextStep.maneuver,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }

            // 2. KARTU STATUS PERJALANAN / ETA PILL (Pojok Kiri Bawah ala Android Auto)
            val etaTimeText = remember(route.durationSeconds) {
                try {
                    LocalTime.now()
                        .plusSeconds(route.durationSeconds)
                        .format(DateTimeFormatter.ofPattern("HH:mm"))
                } catch (e: Exception) {
                    ""
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 14.dp, bottom = 14.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF1E2124).copy(alpha = 0.94f),
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color.White.copy(alpha = 0.12f),
                ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IconButton(
                        onClick = { autoNavigationManager?.stopNavigation() },
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Keluar Navigasi",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    Text(
                        text = route.durationText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2DD36F),
                    )

                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.5f),
                    )

                    Text(
                        text = route.distanceText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )

                    if (etaTimeText.isNotBlank()) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.5f),
                        )

                        Text(
                            text = etaTimeText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f),
                        )
                    }
                }
            }
        }
    }
}

private fun getStyleJson(isDark: Boolean, isSatellite: Boolean, isTraffic: Boolean): String {
    return when {
        isSatellite -> GOOGLE_HYBRID_TRAFFIC_STYLE_JSON
        isDark -> if (isTraffic) GOOGLE_DARK_TRAFFIC_STYLE_JSON else GOOGLE_DARK_NO_TRAFFIC_STYLE_JSON
        isTraffic -> GOOGLE_TRAFFIC_STYLE_JSON
        else -> GOOGLE_NO_TRAFFIC_STYLE_JSON
    }
}

private fun setupStyleLayers(style: Style, context: Context) {
    if (style.getImage(ICON_NAV_ARROW) == null) {
        style.addImage(ICON_NAV_ARROW, createNavArrowBitmap(context))
    }
    if (style.getImage(ICON_LOCATION_DOT) == null) {
        style.addImage(ICON_LOCATION_DOT, createLocationDotBitmap(context))
    }
    if (style.getImage(ICON_DEST_PIN) == null) {
        style.addImage(ICON_DEST_PIN, createDestinationPinBitmap(context))
    }

    // Alt routes sources & layers
    for (i in 0 until 5) {
        val srcId = "$SOURCE_ALT_ROUTE_PREFIX$i"
        val layerId = "$LAYER_ALT_ROUTE_PREFIX$i"
        if (style.getSource(srcId) == null) {
            style.addSource(GeoJsonSource(srcId))
        }
        if (style.getLayer(layerId) == null) {
            val layer = LineLayer(layerId, srcId).withProperties(
                PropertyFactory.lineColor(AndroidColor.parseColor("#80868B")),
                PropertyFactory.lineWidth(5.5f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
            )
            style.addLayer(layer)
        }
    }

    // Active route source & layer
    if (style.getSource(SOURCE_ACTIVE_ROUTE) == null) {
        style.addSource(GeoJsonSource(SOURCE_ACTIVE_ROUTE))
    }
    if (style.getLayer(LAYER_ACTIVE_ROUTE) == null) {
        val layer = LineLayer(LAYER_ACTIVE_ROUTE, SOURCE_ACTIVE_ROUTE).withProperties(
            PropertyFactory.lineColor(AndroidColor.parseColor("#1A73E8")),
            PropertyFactory.lineWidth(8.5f),
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
        )
        style.addLayer(layer)
    }

    // Destination pin source & layer
    if (style.getSource(SOURCE_DEST) == null) {
        style.addSource(GeoJsonSource(SOURCE_DEST))
    }
    if (style.getLayer(LAYER_DEST) == null) {
        val layer = SymbolLayer(LAYER_DEST, SOURCE_DEST).withProperties(
            PropertyFactory.iconImage(ICON_DEST_PIN),
            PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM),
            PropertyFactory.iconAllowOverlap(true),
            PropertyFactory.iconIgnorePlacement(true),
        )
        style.addLayer(layer)
    }

    // Vehicle marker source & layer
    if (style.getSource(SOURCE_VEHICLE) == null) {
        style.addSource(GeoJsonSource(SOURCE_VEHICLE))
    }
    if (style.getLayer(LAYER_VEHICLE) == null) {
        val layer = SymbolLayer(LAYER_VEHICLE, SOURCE_VEHICLE).withProperties(
            PropertyFactory.iconImage(ICON_LOCATION_DOT),
            PropertyFactory.iconAllowOverlap(true),
            PropertyFactory.iconIgnorePlacement(true),
            PropertyFactory.iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_VIEWPORT),
        )
        style.addLayer(layer)
    }
}

@Composable
private fun QuickCategoryChip(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shadowElevation = 3.dp,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun FloatingControlIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.size(46.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shadowElevation = 4.dp,
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun ManeuverIcon(maneuver: String?, modifier: Modifier = Modifier) {
    val m = maneuver?.lowercase() ?: ""
    val icon = when {
        m.contains("left") -> Icons.Default.TurnLeft
        m.contains("right") -> Icons.Default.TurnRight
        m.contains("u-turn") -> Icons.Default.TurnLeft
        else -> Icons.AutoMirrored.Filled.ArrowForward
    }
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = Color.White,
        modifier = modifier,
    )
}

/**
 * Membuat icon navigasi Android Auto: Lingkaran biru dengan border putih dan panah segitiga tajam.
 */
private fun createNavArrowBitmap(context: Context): Bitmap {
    val density = context.resources.displayMetrics.density
    val sizePx = (44 * density).toInt()
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val center = sizePx / 2f
    val strokeWidth = 2.5f * density
    val radius = center - strokeWidth

    val paintCircle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#1A73E8")
        style = Paint.Style.FILL
    }
    val paintBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.STROKE
        this.strokeWidth = strokeWidth
    }
    canvas.drawCircle(center, center, radius, paintCircle)
    canvas.drawCircle(center, center, radius, paintBorder)

    val paintArrow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.FILL
    }
    val path = Path().apply {
        val top = center - radius * 0.62f
        val bottom = center + radius * 0.50f
        val left = center - radius * 0.50f
        val right = center + radius * 0.50f
        val indent = center + radius * 0.22f

        moveTo(center, top)
        lineTo(right, bottom)
        lineTo(center, indent)
        lineTo(left, bottom)
        close()
    }
    canvas.drawPath(path, paintArrow)

    return bitmap
}

/**
 * Membuat icon titik lokasi (Location Dot): Titik biru dengan halo transparan dan border putih.
 */
private fun createLocationDotBitmap(context: Context): Bitmap {
    val density = context.resources.displayMetrics.density
    val sizePx = (32 * density).toInt()
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = sizePx / 2f
    val radius = center * 0.72f
    val haloRadius = center * 0.95f

    val paintHalo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#334285F4")
        style = Paint.Style.FILL
    }
    val paintWhite = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.FILL
    }
    val paintBlue = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#1A73E8")
        style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, haloRadius, paintHalo)
    canvas.drawCircle(center, center, radius, paintWhite)
    canvas.drawCircle(center, center, radius * 0.70f, paintBlue)
    return bitmap
}

/**
 * Membuat icon pin tujuan (Destination Pin Marker) berwarna merah khas Google Maps.
 */
private fun createDestinationPinBitmap(context: Context): Bitmap {
    val density = context.resources.displayMetrics.density
    val widthPx = (30 * density).toInt()
    val heightPx = (40 * density).toInt()
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paintRed = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#EA4335")
        style = Paint.Style.FILL
    }
    val paintWhite = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.FILL
    }

    val r = widthPx / 2f
    canvas.drawCircle(r, r, r - 2f, paintRed)
    val path = Path().apply {
        moveTo(2f, r)
        lineTo(widthPx - 2f, r)
        lineTo(r, heightPx.toFloat())
        close()
    }
    canvas.drawPath(path, paintRed)
    canvas.drawCircle(r, r, r * 0.45f, paintWhite)

    return bitmap
}

/**
 * Menghitung sudut arah (bearing/heading) dari satu koordinat ke koordinat berikutnya.
 */
private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val rLat1 = Math.toRadians(lat1)
    val rLon1 = Math.toRadians(lon1)
    val rLat2 = Math.toRadians(lat2)
    val rLon2 = Math.toRadians(lon2)
    val dLon = rLon2 - rLon1
    val y = Math.sin(dLon) * Math.cos(rLat2)
    val x = Math.cos(rLat1) * Math.sin(rLat2) - Math.sin(rLat1) * Math.cos(rLat2) * Math.cos(dLon)
    val brng = Math.toDegrees(Math.atan2(y, x)).toFloat()
    return (brng + 360f) % 360f
}

/**
 * Menghitung sudut arah awal perjalanan rute agar peta berputar menghadap ke arah jalan navigasi.
 */
private fun calculateInitialRouteBearing(points: List<LatLng>): Float {
    if (points.size < 2) return 0f
    val start = points[0]
    for (i in 1 until points.size) {
        val next = points[i]
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            start.latitude, start.longitude,
            next.latitude, next.longitude,
            results,
        )
        if (results[0] >= 15f || i == points.lastIndex) {
            return calculateBearing(start.latitude, start.longitude, next.latitude, next.longitude)
        }
    }
    return calculateBearing(points[0].latitude, points[0].longitude, points[1].latitude, points[1].longitude)
}
