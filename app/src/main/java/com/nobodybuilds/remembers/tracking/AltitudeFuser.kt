package com.nobodybuilds.remembers.tracking

import android.hardware.SensorManager
import android.location.Location
import android.os.Build

/**
 * GPS altitude wanders by ±10 m; a barometer tracks height changes to a metre or so but drifts with the
 * weather. Until there's a good GPS altitude we use GPS (smoothed). At the first good one we anchor the
 * barometer to it and follow the barometer from then on. Anchoring to the current GPS reading means the
 * switch doesn't make the height jump.
 */
class AltitudeFuser {
    private var pressure: Float? = null
    private var offset: Double? = null
    private var smoothedGps: Double? = null

    val barometric: Boolean get() = pressure != null

    fun onPressure(hPa: Float) {
        pressure = pressure?.let { it + (hPa - it) * 0.2f } ?: hPa
    }

    fun altitudeFor(location: Location): Double? {
        val gps = gpsAltitude(location)?.let { g ->
            (smoothedGps?.let { it + (g - it) * 0.3 } ?: g).also { smoothedGps = it }
        }
        val p = pressure ?: return gps
        val baro = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, p).toDouble()
        offset?.let { return baro + it }
        if (gps != null && (!location.hasVerticalAccuracy() || location.verticalAccuracyMeters <= 20f)) {
            offset = gps - baro
            return gps
        }
        return gps
    }

    private fun gpsAltitude(location: Location): Double? = when {
        Build.VERSION.SDK_INT >= 34 && location.hasMslAltitude() -> location.mslAltitudeMeters
        location.hasAltitude() -> location.altitude
        else -> null
    }
}
