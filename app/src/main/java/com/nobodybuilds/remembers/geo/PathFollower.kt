package com.nobodybuilds.remembers.geo

data class Guidance(
    /** Metres walked along the route. */
    val progress: Double,
    /** Metres left to the end, including the way back onto the path. */
    val remaining: Double,
    /** Metres from the nearest point on the route. */
    val offPath: Double,
    val onPath: Boolean,
    val arrived: Boolean,
    val targetLat: Double,
    val targetLon: Double,
    val bearingToTarget: Double,
    val distanceToTarget: Double,
    val ascentLeft: Double?,
    val descentLeft: Double?,
)

/**
 * Turns position fixes into directions along a [Route]: which way to walk, how far is left,
 * and whether you've strayed. It remembers how far along you are so a path that crosses
 * itself doesn't make it jump to the wrong stretch.
 */
class PathFollower(
    val route: Route,
    private val offPathMetres: Double = 30.0,
    private val backOnPathMetres: Double = 20.0,
    private val lookAheadMetres: Double = 25.0,
    private val arrivalMetres: Double = 20.0,
    private val climbThreshold: Double = 3.0,
) {
    private var lastProgress: Double? = null
    private var onPath = true

    fun update(lat: Double, lon: Double): Guidance {
        val last = lastProgress
        val near = last?.let { route.locate(lat, lon, it - BACK_WINDOW, it + AHEAD_WINDOW, near = it) }
        val match = if (near != null && near.distance <= offPathMetres) near else route.locate(lat, lon)!!
        lastProgress = match.progress

        // Leave the path past offPathMetres, rejoin only inside backOnPathMetres, so GPS wobble near
        // the line doesn't flip it every fix.
        onPath = if (onPath) match.distance <= offPathMetres else match.distance <= backOnPathMetres
        val end = route.points.last()
        val toEnd = Geo.distance(lat, lon, end.lat, end.lon)
        val nearlyDone = route.length - match.progress <= FINAL_STRETCH
        // A loop passes its own end at the start; only count the end once you're on the last stretch.
        val arrived = (nearlyDone && toEnd <= arrivalMetres) || (onPath && route.length - match.progress <= arrivalMetres)

        val target = if (onPath) route.pointAt(match.progress + lookAheadMetres) else RoutePoint(match.lat, match.lon, null)
        val climb = if (route.hasAltitude) route.climbAfter(match.progress, climbThreshold) else null
        return Guidance(
            progress = match.progress,
            remaining = (route.length - match.progress) + if (onPath) 0.0 else match.distance,
            offPath = match.distance,
            onPath = onPath,
            arrived = arrived,
            targetLat = target.lat,
            targetLon = target.lon,
            bearingToTarget = Geo.bearing(lat, lon, target.lat, target.lon),
            distanceToTarget = Geo.distance(lat, lon, target.lat, target.lon),
            ascentLeft = climb?.ascent,
            descentLeft = climb?.descent,
        )
    }

    private companion object {
        const val BACK_WINDOW = 60.0
        const val AHEAD_WINDOW = 400.0
        const val FINAL_STRETCH = 100.0
    }
}
