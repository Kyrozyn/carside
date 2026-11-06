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
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
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
import androidx.compose.material3.OutlinedButton
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
import kotlinx.coroutines.flow.MutableStateFlow
import me.kyrozyn.carside.data.navigation.AutoNavState
import me.kyrozyn.carside.data.navigation.AutoNavigationManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.RoundCap
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import me.kyrozyn.carside.core.AutoPlaceResult
import me.kyrozyn.carside.core.AutoRouteResult
import me.kyrozyn.carside.core.AutoRouteStep
import me.kyrozyn.carside.core.GoogleMapsHelper
import me.kyrozyn.carside.domain.model.GeoLocation

/**
 * Tampilan Peta Google Maps bergaya Android Auto (Coolwalk UI).
 * Mendukung pencarian alamat/tempat, rute navigasi dengan directions, petunjuk manuver,
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
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }
    var isTrafficEnabled by remember { mutableStateOf(true) }
    var mapType by remember { mutableStateOf(GoogleMap.MAP_TYPE_NORMAL) }

    // Search state
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<AutoPlaceResult>>(emptyList()) }
    var isCalculatingRoute by remember { mutableStateOf(false) }

    // Markers & Polylines references
    var destinationMarker by remember { mutableStateOf<Marker?>(null) }
    var userNavMarker by remember { mutableStateOf<Marker?>(null) }
    var routePolylines by remember { mutableStateOf<List<Polyline>>(emptyList()) }

    // Terapkan API Key ke metadata
    LaunchedEffect(apiKey) {
        GoogleMapsHelper.applyApiKey(context, apiKey)
    }

    // Inisialisasi MapView
    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
        }
    }

    // Kelola siklus hidup MapView
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            runCatching { mapView.onDestroy() }
        }
    }

    // Kamera mengikuti lokasi & rute (posisi kendaraan selalu berada di bagian bawah layar)
    LaunchedEffect(googleMap, userLocation, isNavigating, activeRoute, currentStepIndex, bearing) {
        val map = googleMap ?: return@LaunchedEffect

        if (!isNavigating && activeRoute == null) {
            val loc = userLocation
            if (loc != null) {
                val latLng = LatLng(loc.latitude, loc.longitude)
                val cameraPosition = CameraPosition.Builder()
                    .target(latLng)
                    .zoom(16.5f)
                    .bearing(bearing)
                    .tilt(if (bearing > 0f) 25f else 0f)
                    .build()
                map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 800, null)
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

            // Arah navigasi rute (peta berputar menghadap arah jalan raya ke depan)
            val routeHeading = if (currentStepIndex < steps.size - 1 && steps.isNotEmpty()) {
                val from = steps[currentStepIndex].latLng
                val to = steps[currentStepIndex + 1].latLng
                calculateBearing(from, to)
            } else {
                calculateInitialRouteBearing(route.points)
            }

            val effectiveBearing = if (bearing > 0f) bearing else routeHeading

            // Close-up Zoom & 3D Tilt khas Google Maps Driving Mode
            val cameraPosition = CameraPosition.Builder()
                .target(navTarget)
                .zoom(18.5f) // Zoom dekat agar jalan raya terlihat jelas dan nyaman
                .bearing(effectiveBearing) // Menghadap ke arah navigasi/jalan raya
                .tilt(55f) // Sudut 3D perspektif mengemudi
                .build()
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 1000, null)

            // Tampilkan icon kendaraan berupa lingkaran biru dengan panah putih khas Android Auto
            userNavMarker?.remove()
            val navIcon = createNavArrowBitmap(context)
            val markerOptions = MarkerOptions()
                .position(navTarget)
                .icon(navIcon)
                .rotation(effectiveBearing)
                .flat(true)
                .anchor(0.5f, 0.5f)
            userNavMarker = map.addMarker(markerOptions)
        } else if (!isNavigating) {
            userNavMarker?.remove()
            userNavMarker = null
        }
    }

    // Gambar rute di peta saat availableRoutes, selectedRouteIndex, isNavigating, atau selectedPlace berubah
    LaunchedEffect(googleMap, availableRoutes, selectedRouteIndex, isNavigating, selectedPlace) {
        val map = googleMap ?: return@LaunchedEffect
        routePolylines.forEach { it.remove() }
        routePolylines = emptyList()
        destinationMarker?.remove()
        destinationMarker = null

        val place = selectedPlace
        if (place != null) {
            val markerOptions = MarkerOptions()
                .position(place.latLng)
                .title(place.name)
            destinationMarker = map.addMarker(markerOptions)
        }

        if (availableRoutes.isEmpty()) return@LaunchedEffect

        val newPolylines = mutableListOf<Polyline>()

        if (isNavigating) {
            // Mode Navigasi: Tampilkan hanya rute yang sedang aktif
            val route = activeRoute ?: availableRoutes.getOrNull(selectedRouteIndex) ?: return@LaunchedEffect
            val polyOptions = PolylineOptions()
                .addAll(route.points)
                .width(16f)
                .color(AndroidColor.parseColor("#1A73E8")) // Google Maps Navigation Blue
                .startCap(RoundCap())
                .endCap(RoundCap())
                .jointType(JointType.ROUND)
                .geodesic(true)
                .zIndex(2f)

            newPolylines.add(map.addPolyline(polyOptions))
        } else {
            // Mode Pemilihan Rute: Gambar seluruh kemungkinan rute
            // Rute alternatif digambar abu-abu (zIndex 1f), rute aktif digambar biru (zIndex 2f)
            availableRoutes.forEachIndexed { index, route ->
                val isSelected = index == selectedRouteIndex
                val colorHex = if (isSelected) "#1A73E8" else "#80868B"
                val width = if (isSelected) 16f else 12f
                val zIndex = if (isSelected) 2f else 1f

                val polyOptions = PolylineOptions()
                    .addAll(route.points)
                    .width(width)
                    .color(AndroidColor.parseColor(colorHex))
                    .startCap(RoundCap())
                    .endCap(RoundCap())
                    .jointType(JointType.ROUND)
                    .geodesic(true)
                    .zIndex(zIndex)
                    .clickable(true)

                newPolylines.add(map.addPolyline(polyOptions))
            }

            // Zoom kamera mencakup seluruh rute alternatif
            try {
                val boundsBuilder = LatLngBounds.builder()
                var count = 0
                availableRoutes.forEach { r ->
                    r.points.forEach { pt ->
                        boundsBuilder.include(pt)
                        count++
                    }
                }
                if (count > 0) {
                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120))
                }
            } catch (e: Exception) {}
        }
        routePolylines = newPolylines
    }

    Box(modifier = modifier.fillMaxSize()) {
        // MapView Google Maps
        AndroidView(
            factory = {
                mapViewInstance = mapView
                mapView.addOnLayoutChangeListener { _, _, top, _, bottom, _, _, _, _ ->
                    val h = bottom - top
                    if (h > 0) {
                        // Top padding ~54% memposisikan mobil di ~77% dari atas (bagian bawah layar seperti Google Maps)
                        val topPadding = (h * 0.54f).toInt()
                        googleMap?.setPadding(0, topPadding, 0, 0)
                    }
                }
                mapView.getMapAsync { map ->
                    googleMap = map
                    map.uiSettings.isCompassEnabled = true
                    map.uiSettings.isMapToolbarEnabled = false
                    map.uiSettings.isMyLocationButtonEnabled = false
                    map.isTrafficEnabled = isTrafficEnabled
                    map.mapType = mapType

                    val h = mapView.height
                    if (h > 0) {
                        val topPadding = (h * 0.54f).toInt()
                        map.setPadding(0, topPadding, 0, 0)
                    }

                    if (hasLocationPermission) {
                        try {
                            map.isMyLocationEnabled = true
                        } catch (e: SecurityException) {}
                    }

                    map.setOnPolylineClickListener { clickedPolyline ->
                        val clickedIdx = routePolylines.indexOf(clickedPolyline)
                        if (clickedIdx >= 0) {
                            autoNavigationManager?.selectRoute(clickedIdx)
                        }
                    }

                    // Terapkan styling malam jika dark mode
                    if (isDark) {
                        applyGoogleMapDarkStyle(context, map)
                    }

                    // Lokasi awal (default Monas/Jakarta jika GPS belum siap)
                    val startPos = if (userLocation != null) {
                        LatLng(userLocation.latitude, userLocation.longitude)
                    } else {
                        LatLng(-6.1754, 106.8272)
                    }
                    val initialCamera = CameraPosition.Builder()
                        .target(startPos)
                        .zoom(16.5f)
                        .bearing(bearing)
                        .tilt(if (bearing > 0f) 25f else 0f)
                        .build()
                    map.moveCamera(CameraUpdateFactory.newCameraPosition(initialCamera))
                }
                mapView
            },
            modifier = Modifier.fillMaxSize(),
        )

        // ── TOP BAR: Pencarian Android Auto & Kategori Cepat ───────────
        if (!isNavigating) {
            // Bar Pencarian Floating (start = 124.dp agar sejajar di kanan tombol Apps & Settings tanpa tabrakan)
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

            // Warning jika API Key belum diisi (diletakkan di bawah tombol atas)
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
                            text = "Mode Auto aktif. Masukkan Google Maps API Key di Pengaturan.",
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
                // Chips Kategori Cepat (SPBU, Restoran, Parkir, Kafe) di bawah baris tombol atas
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
                                            // Minta seluruh rute ke tempat ini (fallback ke posisi peta jika GPS tidak tersedia)
                                            val startLatLng = userLocation?.let { LatLng(it.latitude, it.longitude) }
                                                ?: googleMap?.cameraPosition?.target
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
                if (userLocation != null && googleMap != null) {
                    val cameraPosition = CameraPosition.Builder()
                        .target(LatLng(userLocation.latitude, userLocation.longitude))
                        .zoom(if (isNavigating) 17.8f else 16.5f)
                        .bearing(bearing)
                        .tilt(if (isNavigating) 48f else 25f)
                        .build()
                    googleMap?.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                }
            }

            // Traffic Layer Toggle
            FloatingControlIconButton(
                icon = Icons.Default.Traffic,
                contentDescription = "Lalu Lintas",
                tint = if (isTrafficEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                isTrafficEnabled = !isTrafficEnabled
                googleMap?.isTrafficEnabled = isTrafficEnabled
            }

            // Satellite / Map Type Toggle
            FloatingControlIconButton(
                icon = Icons.Default.Layers,
                contentDescription = "Tipe Peta",
            ) {
                mapType = if (mapType == GoogleMap.MAP_TYPE_NORMAL) {
                    GoogleMap.MAP_TYPE_HYBRID
                } else {
                    GoogleMap.MAP_TYPE_NORMAL
                }
                googleMap?.mapType = mapType
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
                            Text("Mencari seluruh kemungkinan rute...", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else if (availableRoutes.isNotEmpty()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Jika ada lebih dari 1 alternatif rute, tampilkan pilihan rute
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
                        // Jika rute belum dimuat (misal belum ada API key atau GPS)
                        Button(
                            onClick = {
                                val startLatLng = userLocation?.let { LatLng(it.latitude, it.longitude) }
                                    ?: googleMap?.cameraPosition?.target
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
                // Header Manuver Utama
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
                        color = Color(0xFF093D30), // Hijau lebih gelap untuk sub-maneuver
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
                color = Color(0xFF1E2124).copy(alpha = 0.94f), // Hitam transparan Android Auto
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
                    // Tombol Keluar Navigasi (✕)
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

                    // Durasi waktu sisa (Hijau cerah khas Android Auto ETA)
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

                    // Jarak sisa
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

                        // Jam perkiraan tiba
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
 * Styling malam (Dark Mode) untuk Google Maps SDK yang serasi dengan Android Auto Coolwalk.
 */
private fun applyGoogleMapDarkStyle(context: Context, map: GoogleMap) {
    val darkJson = """
    [
      { "elementType": "geometry", "stylers": [{ "color": "#18222d" }] },
      { "elementType": "labels.text.stroke", "stylers": [{ "color": "#18222d" }] },
      { "elementType": "labels.text.fill", "stylers": [{ "color": "#8c9ba5" }] },
      { "featureType": "administrative.locality", "elementType": "labels.text.fill", "stylers": [{ "color": "#c4d1db" }] },
      { "featureType": "poi", "elementType": "labels.text.fill", "stylers": [{ "color": "#8c9ba5" }] },
      { "featureType": "poi.park", "elementType": "geometry", "stylers": [{ "color": "#1a2c30" }] },
      { "featureType": "poi.park", "elementType": "labels.text.fill", "stylers": [{ "color": "#5b8c74" }] },
      { "featureType": "road", "elementType": "geometry", "stylers": [{ "color": "#283747" }] },
      { "featureType": "road", "elementType": "geometry.stroke", "stylers": [{ "color": "#1e2a36" }] },
      { "featureType": "road", "elementType": "labels.text.fill", "stylers": [{ "color": "#94a3b8" }] },
      { "featureType": "road.highway", "elementType": "geometry", "stylers": [{ "color": "#33475b" }] },
      { "featureType": "road.highway", "elementType": "geometry.stroke", "stylers": [{ "color": "#202e3b" }] },
      { "featureType": "road.highway", "elementType": "labels.text.fill", "stylers": [{ "color": "#cbd5e1" }] },
      { "featureType": "transit", "elementType": "geometry", "stylers": [{ "color": "#223243" }] },
      { "featureType": "transit.station", "elementType": "labels.text.fill", "stylers": [{ "color": "#94a3b8" }] },
      { "featureType": "water", "elementType": "geometry", "stylers": [{ "color": "#101923" }] },
      { "featureType": "water", "elementType": "labels.text.fill", "stylers": [{ "color": "#516375" }] },
      { "featureType": "water", "elementType": "labels.text.stroke", "stylers": [{ "color": "#101923" }] }
    ]
    """.trimIndent()
    try {
        map.setMapStyle(MapStyleOptions(darkJson))
    } catch (e: Exception) {}
}

/**
 * Membuat icon navigasi resmi Android Auto: Lingkaran biru dengan panah segitiga putih.
 */
private fun createNavArrowBitmap(context: Context): BitmapDescriptor {
    val density = context.resources.displayMetrics.density
    val sizePx = (42 * density).toInt()
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val center = sizePx / 2f
    val strokeWidth = 2.5f * density
    val radius = center - strokeWidth

    // Lingkaran luar biru Google Android Auto
    val paintCircle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#1A73E8")
        style = Paint.Style.FILL
    }
    // Border putih
    val paintBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.STROKE
        this.strokeWidth = strokeWidth
    }
    canvas.drawCircle(center, center, radius, paintCircle)
    canvas.drawCircle(center, center, radius, paintBorder)

    // Panah segitiga putih tajam di tengah
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

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

/**
 * Menghitung sudut arah (bearing/heading) dari satu koordinat ke koordinat berikutnya.
 */
private fun calculateBearing(from: LatLng, to: LatLng): Float {
    val lat1 = Math.toRadians(from.latitude)
    val lon1 = Math.toRadians(from.longitude)
    val lat2 = Math.toRadians(to.latitude)
    val lon2 = Math.toRadians(to.longitude)
    val dLon = lon2 - lon1
    val y = Math.sin(dLon) * Math.cos(lat2)
    val x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon)
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
            return calculateBearing(start, next)
        }
    }
    return calculateBearing(points[0], points[1])
}
