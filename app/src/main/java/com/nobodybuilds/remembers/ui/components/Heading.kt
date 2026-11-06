package com.nobodybuilds.remembers.ui.components

import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.nobodybuilds.remembers.geo.Geo
import com.nobodybuilds.remembers.tracking.Fix
import kotlin.math.cos
import kotlin.math.sin

@Immutable
data class Heading(val degrees: Double, val needsCalibration: Boolean)

/**
 * Which way the top of the phone points, in degrees from true north. Null when the phone has no compass.
 */
@Composable
fun rememberHeading(fix: Fix?): State<Heading?> {
    val context = LocalContext.current
    val view = LocalView.current
    val state = remember { mutableStateOf<Heading?>(null) }
    val declination = remember(fix?.lat?.toInt(), fix?.lon?.toInt()) {
        fix?.let { GeomagneticField(it.lat.toFloat(), it.lon.toFloat(), (it.alt ?: 0.0).toFloat(), System.currentTimeMillis()).declination } ?: 0f
    }
    DisposableEffect(declination) {
        val sm = context.getSystemService(SensorManager::class.java)
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        var sx = 0.0
        var sy = 0.0
        var calibration = SensorManager.SENSOR_STATUS_ACCURACY_HIGH
        val listener = object : SensorEventListener {
            private val rotation = FloatArray(9)
            private val remapped = FloatArray(9)
            private val orientation = FloatArray(3)
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotation, event.values)
                val (ax, ay) = when (view.display?.rotation) {
                    Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
                    Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
                    Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
                    else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
                }
                SensorManager.remapCoordinateSystem(rotation, ax, ay, remapped)
                SensorManager.getOrientation(remapped, orientation)
                val azimuth = Math.toRadians(Geo.normalize360(Math.toDegrees(orientation[0].toDouble()) + declination))
                // Smooth on the unit circle so 359° → 1° doesn't swing the long way round.
                sx += (cos(azimuth) - sx) * 0.15
                sy += (sin(azimuth) - sy) * 0.15
                state.value = Heading(
                    degrees = Geo.normalize360(Math.toDegrees(kotlin.math.atan2(sy, sx))),
                    needsCalibration = calibration <= SensorManager.SENSOR_STATUS_ACCURACY_LOW,
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                calibration = accuracy
            }
        }
        sensor?.let { sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        onDispose { sm.unregisterListener(listener) }
    }
    return state
}
