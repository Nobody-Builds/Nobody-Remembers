package com.nobodybuilds.remembers.geo

/**
 * Totals climbing and descending while ignoring wobbles smaller than [threshold] metres,
 * so GPS and barometer noise doesn't turn a flat path into a hill.
 */
class ClimbCounter(
    private val threshold: Double,
    ascent: Double = 0.0,
    descent: Double = 0.0,
    reference: Double? = null,
) {
    var ascent = ascent
        private set
    var descent = descent
        private set
    private var reference = reference

    fun add(altitude: Double) {
        val ref = reference ?: run { reference = altitude; return }
        val delta = altitude - ref
        if (delta >= threshold) {
            ascent += delta
            reference = altitude
        } else if (delta <= -threshold) {
            descent -= delta
            reference = altitude
        }
    }
}

data class Climb(val ascent: Double, val descent: Double)

fun climb(altitudes: Iterable<Double>, threshold: Double = 3.0): Climb {
    val counter = ClimbCounter(threshold)
    altitudes.forEach(counter::add)
    return Climb(counter.ascent, counter.descent)
}
