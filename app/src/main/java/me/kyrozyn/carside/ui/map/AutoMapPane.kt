package me.kyrozyn.carside.ui.map

import android.content.Context
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
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
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()

    // Map instances & state
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }
    var isTrafficEnabled by remember { mutableStateOf(true) }
    var mapType by remember { mutableStateOf(GoogleMap.MAP_TYPE_NORMAL) }

    // Search & Navigation state
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<AutoPlaceResult>>(emptyList()) }
    var selectedPlace by remember { mutableStateOf<AutoPlaceResult?>(null) }
    var activeRoute by remember { mutableStateOf<AutoRouteResult?>(null) }
    var isCalculatingRoute by remember { mutableStateOf(false) }
    var isNavigating by remember { mutableStateOf(false) }

    // Markers & Polylines references
    var destinationMarker by remember { mutableStateOf<Marker?>(null) }
    var routePolyline by remember { mutableStateOf<Polyline?>(null) }

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

    // Kamera mengikuti lokasi GPS
    LaunchedEffect(googleMap, userLocation) {
        val map = googleMap ?: return@LaunchedEffect
        val loc = userLocation ?: return@LaunchedEffect
        val latLng = LatLng(loc.latitude, loc.longitude)

        if (!isNavigating && activeRoute == null) {
            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 16f)
            map.animateCamera(cameraUpdate)
        } else if (isNavigating) {
            // Dalam mode navigasi: kamera menghadap arah kendaraan
            val cameraPosition = CameraPosition.Builder()
                .target(latLng)
                .zoom(17.5f)
                .bearing(bearing)
                .tilt(45f)
                .build()
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 1000, null)
        }
    }

    // Gambar rute di peta saat activeRoute berubah
    LaunchedEffect(googleMap, activeRoute) {
        val map = googleMap ?: return@LaunchedEffect
        routePolyline?.remove()
        routePolyline = null
        destinationMarker?.remove()
        destinationMarker = null

        val route = activeRoute
        val place = selectedPlace
        if (route != null && route.points.isNotEmpty()) {
            val polyOptions = PolylineOptions()
                .addAll(route.points)
                .width(16f)
                .color(AndroidColor.parseColor("#1A73E8")) // Google Maps Navigation Blue
                .startCap(RoundCap())
                .endCap(RoundCap())
                .jointType(JointType.ROUND)
                .geodesic(true)

            routePolyline = map.addPolyline(polyOptions)

            if (place != null) {
                val markerOptions = MarkerOptions()
                    .position(place.latLng)
                    .title(place.name)
                destinationMarker = map.addMarker(markerOptions)
            }

            // Zoom kamera mencakup seluruh rute
            if (!isNavigating) {
                try {
                    val boundsBuilder = LatLngBounds.builder()
                    route.points.forEach { boundsBuilder.include(it) }
                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120))
                } catch (e: Exception) {}
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // MapView Google Maps
        AndroidView(
            factory = {
                mapViewInstance = mapView
                mapView.getMapAsync { map ->
                    googleMap = map
                    map.uiSettings.isCompassEnabled = true
                    map.uiSettings.isMapToolbarEnabled = false
                    map.uiSettings.isMyLocationButtonEnabled = false
                    map.isTrafficEnabled = isTrafficEnabled
                    map.mapType = mapType

                    if (hasLocationPermission) {
                        try {
                            map.isMyLocationEnabled = true
                        } catch (e: SecurityException) {}
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
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(startPos, 15f))
                }
                mapView
            },
            modifier = Modifier.fillMaxSize(),
        )

        // ── TOP BAR: Pencarian Android Auto & Kategori Cepat ───────────
        if (!isNavigating) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(12.dp),
            ) {
                // Warning jika API Key belum diisi
                if (apiKey.isNullOrBlank()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clickable { onOpenSettings() },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.95f),
                        shadowElevation = 3.dp,
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
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
                }

                // Bar Pencarian Floating
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    shadowElevation = 6.dp,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Cari",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
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
                            placeholder = { Text("Cari tempat atau alamat...") },
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
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Hapus",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                // Chips Kategori Cepat (SPBU, Restoran, Parkir, Kafe)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 8.dp),
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

                // Hasil Pencarian Floating
                AnimatedVisibility(
                    visible = searchResults.isNotEmpty() || isSearching,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically(),
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .heightIn(max = 240.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                        shadowElevation = 8.dp,
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
                                                selectedPlace = place
                                                searchResults = emptyList()
                                                searchQuery = place.name
                                                // Minta rute ke tempat ini
                                                if (userLocation != null) {
                                                    isCalculatingRoute = true
                                                    coroutineScope.launch {
                                                        activeRoute = GoogleMapsHelper.getDirections(
                                                            LatLng(userLocation.latitude, userLocation.longitude),
                                                            place.latLng,
                                                            apiKey,
                                                        )
                                                        isCalculatingRoute = false
                                                    }
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
                    googleMap?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(userLocation.latitude, userLocation.longitude),
                            16f,
                        ),
                    )
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
            val place = selectedPlace!!
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
                                selectedPlace = null
                                activeRoute = null
                                routePolyline?.remove()
                                routePolyline = null
                                destinationMarker?.remove()
                                destinationMarker = null
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
                            Text("Menghitung rute terbaik...", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else if (route != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = route.durationText,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "(${route.distanceText})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            Button(
                                onClick = { isNavigating = true },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                ),
                            ) {
                                Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Mulai Navigasi", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    } else {
                        // Jika rute belum dimuat (misal belum ada API key)
                        Button(
                            onClick = {
                                if (userLocation != null) {
                                    isCalculatingRoute = true
                                    coroutineScope.launch {
                                        activeRoute = GoogleMapsHelper.getDirections(
                                            LatLng(userLocation.latitude, userLocation.longitude),
                                            place.latLng,
                                            apiKey,
                                        )
                                        isCalculatingRoute = false
                                    }
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

        // ── MODE NAVIGASI AKTIF: Turn-by-Turn Banner & Bottom Bar ───────
        if (isNavigating && activeRoute != null) {
            val route = activeRoute!!
            val firstStep = route.steps.firstOrNull()

            // Banner Navigasi Hijau/Gelap (Android Auto Turn Banner) di Kiri Atas
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp)
                    .fillMaxWidth(0.85f),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0F5132), // Emerald Navigation Green
                shadowElevation = 8.dp,
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        ManeuverIcon(
                            maneuver = firstStep?.maneuver,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = firstStep?.distanceText?.ifBlank { "Dalam 200 m" } ?: "Dalam 200 m",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Text(
                            text = firstStep?.instruction ?: "Ikuti rute di peta",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            // Bottom Bar Navigasi (ETA, Jarak, Tombol Keluar)
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shadowElevation = 8.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = route.durationText,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "•  ${route.distanceText}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = "Menuju: ${selectedPlace?.name ?: route.endAddress}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            isNavigating = false
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Text("Keluar")
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
 * Styling malam (Dark Mode) untuk Google Maps SDK.
 */
private fun applyGoogleMapDarkStyle(context: Context, map: GoogleMap) {
    val darkJson = """
    [
      { "elementType": "geometry", "stylers": [{ "color": "#242f3e" }] },
      { "elementType": "labels.text.stroke", "stylers": [{ "color": "#242f3e" }] },
      { "elementType": "labels.text.fill", "stylers": [{ "color": "#746855" }] },
      { "featureType": "administrative.locality", "elementType": "labels.text.fill", "stylers": [{ "color": "#d59563" }] },
      { "featureType": "poi", "elementType": "labels.text.fill", "stylers": [{ "color": "#d59563" }] },
      { "featureType": "poi.park", "elementType": "geometry", "stylers": [{ "color": "#263c3f" }] },
      { "featureType": "poi.park", "elementType": "labels.text.fill", "stylers": [{ "color": "#6b9a76" }] },
      { "featureType": "road", "elementType": "geometry", "stylers": [{ "color": "#38414e" }] },
      { "featureType": "road", "elementType": "geometry.stroke", "stylers": [{ "color": "#212a37" }] },
      { "featureType": "road", "elementType": "labels.text.fill", "stylers": [{ "color": "#9ca5b3" }] },
      { "featureType": "road.highway", "elementType": "geometry", "stylers": [{ "color": "#746855" }] },
      { "featureType": "road.highway", "elementType": "geometry.stroke", "stylers": [{ "color": "#1f2835" }] },
      { "featureType": "road.highway", "elementType": "labels.text.fill", "stylers": [{ "color": "#f3d19c" }] },
      { "featureType": "transit", "elementType": "geometry", "stylers": [{ "color": "#2f3948" }] },
      { "featureType": "transit.station", "elementType": "labels.text.fill", "stylers": [{ "color": "#d59563" }] },
      { "featureType": "water", "elementType": "geometry", "stylers": [{ "color": "#17263c" }] },
      { "featureType": "water", "elementType": "labels.text.fill", "stylers": [{ "color": "#515c6d" }] },
      { "featureType": "water", "elementType": "labels.text.stroke", "stylers": [{ "color": "#17263c" }] }
    ]
    """.trimIndent()
    try {
        map.setMapStyle(MapStyleOptions(darkJson))
    } catch (e: Exception) {}
}
