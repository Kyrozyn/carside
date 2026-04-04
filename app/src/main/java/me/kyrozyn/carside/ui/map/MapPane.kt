package me.kyrozyn.carside.ui.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.roundToInt
import me.kyrozyn.carside.domain.model.GeoLocation
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

private const val FOLLOW_ZOOM = 17.0

private class MapFollowState {
    var marker: Marker? = null
    var didInitialZoom: Boolean = false
}

/**
 * Peta OpenStreetMap (osmdroid) yang **mengikuti posisi Anda** dari Fused Location:
 * titik biru di lokasi + peta memusat ke koordinat terbaru (seperti mode ikut di Android Auto).
 */
@Composable
fun MapPane(
    hasLocationPermission: Boolean,
    userLocation: GeoLocation?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    val dotPx = (18 * density.density).roundToInt().coerceIn(12, 48)

    val mapState = remember { MapFollowState() }

    val mapView = remember(context) {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            minZoomLevel = 4.0
            maxZoomLevel = 19.0
            controller.setZoom(10.0)
            controller.setCenter(GeoPoint(-6.2, 106.8))
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
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

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { mv ->
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
                val icon = locationDotDrawable(mv.context.resources, dotPx)
                val marker = Marker(mv).apply {
                    position = point
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    title = "Posisi Anda"
                    setIcon(icon)
                }
                mv.overlays.add(marker)
                mapState.marker = marker
                if (!mapState.didInitialZoom) {
                    mv.controller.setZoom(FOLLOW_ZOOM)
                    mapState.didInitialZoom = true
                }
                mv.controller.animateTo(point)
            } else {
                mapState.marker?.position = point
                mv.controller.setCenter(point)
            }
            mv.invalidate()
        },
    )
}

private fun locationDotDrawable(res: android.content.res.Resources, sizePx: Int): BitmapDrawable {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#2196F3")
    }
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = sizePx * 0.15f
        color = Color.WHITE
    }
    val cx = sizePx / 2f
    val cy = sizePx / 2f
    val r = sizePx / 2f - stroke.strokeWidth
    canvas.drawCircle(cx, cy, r, fill)
    canvas.drawCircle(cx, cy, r - stroke.strokeWidth / 2f, stroke)
    return BitmapDrawable(res, bmp)
}
