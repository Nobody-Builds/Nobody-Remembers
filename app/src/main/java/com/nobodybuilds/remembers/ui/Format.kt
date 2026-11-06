package com.nobodybuilds.remembers.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

fun formatDistance(metres: Double): String = when {
    metres < 1000 -> "${(metres / 5).roundToInt() * 5} m"
    metres < 10_000 -> String.format(Locale.getDefault(), "%.2f km", metres / 1000)
    else -> String.format(Locale.getDefault(), "%.1f km", metres / 1000)
}

fun formatMetres(metres: Double): String = "${metres.roundToInt()} m"

fun formatDuration(millis: Long): String {
    val totalMinutes = millis / 60_000
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return when {
        h > 0 -> "$h h ${m.toString().padStart(2, '0')} min"
        totalMinutes > 0 -> "$m min"
        else -> "${(millis / 1000).coerceAtLeast(0)} s"
    }
}

/** Clock-style elapsed time for a running recording. */
fun formatClock(millis: Long): String {
    val s = (millis / 1000).coerceAtLeast(0)
    return String.format(Locale.getDefault(), "%d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60)
}

private val dateFormat = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm")

fun formatDate(millis: Long): String = dateFormat.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

fun defaultTrailName(millis: Long): String =
    "Trail · " + DateTimeFormatter.ofPattern("d MMM, HH:mm").format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

/**
 * Walking time by Naismith's rule: 5 km/h plus a minute for every 10 m of climb.
 */
fun naismithMillis(distance: Double, ascent: Double): Long =
    ((distance / 5000.0 * 60 + ascent / 10.0) * 60_000).toLong()

/** Plain-language turn instruction from the angle between where you face and where to go. */
fun turnHint(relative: Double): String {
    val a = kotlin.math.abs(relative)
    val side = if (relative < 0) "left" else "right"
    return when {
        a <= 20 -> "Straight on"
        a <= 60 -> "Bear $side"
        a <= 135 -> "Turn $side"
        else -> "Turn round"
    }
}
