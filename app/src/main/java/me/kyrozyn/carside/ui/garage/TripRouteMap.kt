package me.kyrozyn.carside.ui.garage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import me.kyrozyn.carside.data.map.OPEN_FREE_MAP_LIBERTY_STYLE_URL
import me.kyrozyn.carside.domain.model.TripPoint
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

private const val ROUTE_SOURCE_ID = "trip-route-source"
private const val ROUTE_LAYER_ID = "trip-route-layer"

@Composable
fun TripRouteMap(
    points: List<TripPoint>,
    lineColorHex: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember(context) { MapView(context) }
    var mapRef by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleLoaded by remember { mutableStateOf(false) }

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
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    LaunchedEffect(mapView) {
        mapView.getMapAsync { map ->
            mapRef = map
            map.uiSettings.apply {
                isCompassEnabled = false
                isLogoEnabled = false
                isAttributionEnabled = false
            }
            map.setStyle(Style.Builder().fromUri(OPEN_FREE_MAP_LIBERTY_STYLE_URL)) { style ->
                styleLoaded = true
                applyRouteToStyle(style, points, lineColorHex, map)
            }
        }
    }

    LaunchedEffect(points, lineColorHex, styleLoaded, mapRef) {
        val map = mapRef ?: return@LaunchedEffect
        if (!styleLoaded) return@LaunchedEffect
        map.getStyle { style ->
            applyRouteToStyle(style, points, lineColorHex, map)
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
    )
}

private fun applyRouteToStyle(
    style: Style,
    points: List<TripPoint>,
    lineColorHex: String,
    map: MapLibreMap,
) {
    val coords = points.map { Point.fromLngLat(it.longitude, it.latitude) }
    if (coords.size < 2) {
        if (style.getSource(ROUTE_SOURCE_ID) != null) {
            style.removeLayer(ROUTE_LAYER_ID)
            style.removeSource(ROUTE_SOURCE_ID)
        }
        if (coords.size == 1) {
            val p = coords.first()
            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(p.latitude(), p.longitude()), 15.0),
            )
        }
        return
    }

    val lineString = LineString.fromLngLats(coords)
    val feature = Feature.fromGeometry(lineString)
    val source = style.getSource(ROUTE_SOURCE_ID) as? GeoJsonSource
    if (source != null) {
        source.setGeoJson(feature)
    } else {
        val geo = GeoJsonSource(ROUTE_SOURCE_ID).apply { setGeoJson(feature) }
        style.addSource(geo)
        val color = android.graphics.Color.parseColor(lineColorHex)
        style.addLayer(
            LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
                PropertyFactory.lineColor(color),
                PropertyFactory.lineWidth(5f),
            ),
        )
    }

    val boundsBuilder = LatLngBounds.Builder()
    coords.forEach { boundsBuilder.include(LatLng(it.latitude(), it.longitude())) }
    val bounds = boundsBuilder.build()
    map.easeCamera(CameraUpdateFactory.newLatLngBounds(bounds, 80))
}
