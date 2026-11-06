package com.nobodybuilds.remembers.geo

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object Geo {
    const val EARTH_RADIUS_M = 6_371_008.8

    /** Great-circle distance in metres. */
    fun distance(aLat: Double, aLon: Double, bLat: Double, bLon: Double): Double {
        val dLat = Math.toRadians(bLat - aLat)
        val dLon = Math.toRadians(bLon - aLon)
        val h = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(aLat)) * cos(Math.toRadians(bLat)) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * EARTH_RADIUS_M * atan2(sqrt(h), sqrt(1 - h))
    }

    /** Initial bearing from a to b, degrees clockwise from true north in [0, 360). */
    fun bearing(aLat: Double, aLon: Double, bLat: Double, bLon: Double): Double {
        val p1 = Math.toRadians(aLat)
        val p2 = Math.toRadians(bLat)
        val dLon = Math.toRadians(bLon - aLon)
        val y = sin(dLon) * cos(p2)
        val x = cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dLon)
        return normalize360(Math.toDegrees(atan2(y, x)))
    }

    fun normalize360(deg: Double): Double = ((deg % 360) + 360) % 360

    /** Signed angle in (-180, 180]. */
    fun normalize180(deg: Double): Double {
        val d = normalize360(deg)
        return if (d > 180) d - 360 else d
    }

    fun cardinal(bearing: Double): String {
        val names = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        return names[((normalize360(bearing) + 22.5) / 45).toInt() % 8]
    }
}

/** Flat east/north projection around an origin. Error is well under a metre across a day's walk. */
class LocalProjection(private val originLat: Double, private val originLon: Double) {
    private val cosLat = cos(Math.toRadians(originLat))

    fun x(lon: Double): Double = Math.toRadians(lon - originLon) * Geo.EARTH_RADIUS_M * cosLat
    fun y(lat: Double): Double = Math.toRadians(lat - originLat) * Geo.EARTH_RADIUS_M
    fun lon(x: Double): Double = originLon + Math.toDegrees(x / (Geo.EARTH_RADIUS_M * cosLat))
    fun lat(y: Double): Double = originLat + Math.toDegrees(y / Geo.EARTH_RADIUS_M)
}
