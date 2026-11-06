package com.nobodybuilds.remembers.geo

import kotlin.math.abs
import kotlin.math.hypot

data class RoutePoint(val lat: Double, val lon: Double, val alt: Double?)

/** Where a position sits relative to the route. */
data class Match(
    val segment: Int,
    val progress: Double,
    val distance: Double,
    val lat: Double,
    val lon: Double,
)

/** A recorded path in the order it should be walked. Needs at least two points. */
class Route(val points: List<RoutePoint>) {
    init {
        require(points.size >= 2) { "A route needs at least two points" }
    }

    private val projection = LocalProjection(points[0].lat, points[0].lon)
    private val xs = DoubleArray(points.size) { projection.x(points[it].lon) }
    private val ys = DoubleArray(points.size) { projection.y(points[it].lat) }

    /** Metres from the first point to each point. */
    val cumulative: DoubleArray = DoubleArray(points.size).also {
        for (i in 1 until points.size) it[i] = it[i - 1] + hypot(xs[i] - xs[i - 1], ys[i] - ys[i - 1])
    }
    val length: Double get() = cumulative.last()
    val hasAltitude: Boolean = points.count { it.alt != null } >= 2

    /**
     * Closest point on the route. When [from]..[to] is given only that stretch is searched, which keeps
     * the match on the right pass where a path doubles back on itself. When two passes are about as close
     * (an out-and-back spur), the one nearest [near] along the route wins, forward on a tie; with no [near]
     * the earlier one wins.
     */
    fun locate(lat: Double, lon: Double, from: Double = 0.0, to: Double = length, near: Double? = null): Match? {
        val px = projection.x(lon)
        val py = projection.y(lat)
        val candidates = ArrayList<Match>()
        for (i in 0 until points.size - 1) {
            if (cumulative[i + 1] < from || cumulative[i] > to) continue
            val dx = xs[i + 1] - xs[i]
            val dy = ys[i + 1] - ys[i]
            val len2 = dx * dx + dy * dy
            val t = if (len2 == 0.0) 0.0 else (((px - xs[i]) * dx + (py - ys[i]) * dy) / len2).coerceIn(0.0, 1.0)
            val cx = xs[i] + t * dx
            val cy = ys[i] + t * dy
            candidates += Match(
                segment = i,
                progress = cumulative[i] + t * (cumulative[i + 1] - cumulative[i]),
                distance = hypot(px - cx, py - cy),
                lat = projection.lat(cy),
                lon = projection.lon(cx),
            )
        }
        val best = candidates.minOfOrNull { it.distance } ?: return null
        // Each separate pass along the route shows up as a local minimum of distance. Only those compete
        // in a near-tie; neighbouring segments of the same pass don't.
        val passes = candidates.filterIndexed { i, c ->
            c.distance <= (candidates.getOrNull(i - 1)?.distance ?: Double.MAX_VALUE) &&
                c.distance <= (candidates.getOrNull(i + 1)?.distance ?: Double.MAX_VALUE)
        }
        val tied = passes.filter { it.distance <= best + TIE_METRES }
        return if (near == null) tied.minBy { it.progress }
        else tied.minBy { abs(it.progress - near) - if (it.progress >= near) FORWARD_BIAS else 0.0 }
    }

    /** The point [progress] metres along the route. */
    fun pointAt(progress: Double): RoutePoint {
        val p = progress.coerceIn(0.0, length)
        var i = cumulative.binarySearch(p).let { if (it >= 0) it else -it - 2 }.coerceIn(0, points.size - 2)
        while (i < points.size - 2 && cumulative[i + 1] < p) i++
        val span = cumulative[i + 1] - cumulative[i]
        val t = if (span == 0.0) 0.0 else (p - cumulative[i]) / span
        val a = points[i]
        val b = points[i + 1]
        val alt = if (a.alt != null && b.alt != null) a.alt + t * (b.alt - a.alt) else a.alt ?: b.alt
        return RoutePoint(
            lat = projection.lat(ys[i] + t * (ys[i + 1] - ys[i])),
            lon = projection.lon(xs[i] + t * (xs[i + 1] - xs[i])),
            alt = alt,
        )
    }

    /** Climbing and descending still ahead after [progress] metres. */
    fun climbAfter(progress: Double, threshold: Double): Climb {
        val alts = ArrayList<Double>()
        pointAt(progress).alt?.let(alts::add)
        for (i in points.indices) if (cumulative[i] > progress) points[i].alt?.let(alts::add)
        return climb(alts, threshold)
    }

    fun reversed(): Route = Route(points.asReversed().toList())

    private companion object {
        const val TIE_METRES = 5.0
        const val FORWARD_BIAS = 0.01
    }
}
