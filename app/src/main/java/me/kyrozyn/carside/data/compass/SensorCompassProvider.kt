package me.kyrozyn.carside.data.compass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import me.kyrozyn.carside.domain.model.CompassHeading
import me.kyrozyn.carside.domain.repository.CompassProvider
import kotlin.math.abs
import kotlin.math.roundToInt

class SensorCompassProvider(context: Context) : CompassProvider {

    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    override val heading: Flow<CompassHeading> = callbackFlow {
        if (rotation == null) {
            trySend(CompassHeading(0f, "—"))
            awaitClose { }
            return@callbackFlow
        }

        val r = FloatArray(9)
        val orientation = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
                SensorManager.getRotationMatrixFromVector(r, event.values)
                SensorManager.getOrientation(r, orientation)
                val azimuthRad = orientation[0]
                var deg = Math.toDegrees(azimuthRad.toDouble()).toFloat()
                if (deg < 0f) deg += 360f
                val rounded = (deg.roundToInt() % 360).toFloat()
                trySend(CompassHeading(degrees = rounded, cardinal = toCardinal(rounded)))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(
            listener,
            rotation,
            SensorManager.SENSOR_DELAY_UI,
        )

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }.distinctUntilChanged { a, b ->
        abs(a.degrees - b.degrees) < 2f && a.cardinal == b.cardinal
    }

    private fun toCardinal(deg: Float): String {
        val d = ((deg % 360f) + 360f) % 360f
        return when {
            d < 22.5f || d >= 337.5f -> "Utara"
            d < 67.5f  -> "Timur Laut"
            d < 112.5f -> "Timur"
            d < 157.5f -> "Tenggara"
            d < 202.5f -> "Selatan"
            d < 247.5f -> "Barat Daya"
            d < 292.5f -> "Barat"
            else       -> "Barat Laut"
        }
    }
}
