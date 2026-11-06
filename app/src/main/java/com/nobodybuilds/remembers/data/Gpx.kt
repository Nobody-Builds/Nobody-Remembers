package com.nobodybuilds.remembers.data

import java.time.Instant
import java.util.Locale

/** GPX 1.1, readable by every mapping app. Marks become waypoints. */
object Gpx {
    fun write(trail: Trail, points: List<TrackPoint>, marks: List<Mark>): String = buildString {
        appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        appendLine("""<gpx version="1.1" creator="Nobody Remembers" xmlns="http://www.topografix.com/GPX/1/1">""")
        appendLine("  <metadata><name>${esc(trail.name)}</name><time>${time(trail.startedAt)}</time></metadata>")
        for (m in marks) {
            appendLine("  <wpt ${latLon(m.lat, m.lon)}>")
            m.alt?.let { appendLine("    <ele>${num(it)}</ele>") }
            appendLine("    <time>${time(m.time)}</time>")
            appendLine("    <name>${esc(m.note.ifBlank { m.type.label })}</name>")
            appendLine("    <type>${esc(m.type.label)}</type>")
            appendLine("  </wpt>")
        }
        appendLine("  <trk><name>${esc(trail.name)}</name><trkseg>")
        for (p in points) {
            append("    <trkpt ${latLon(p.lat, p.lon)}>")
            p.alt?.let { append("<ele>${num(it)}</ele>") }
            appendLine("<time>${time(p.time)}</time></trkpt>")
        }
        appendLine("  </trkseg></trk>")
        appendLine("</gpx>")
    }

    private fun latLon(lat: Double, lon: Double) = """lat="${String.format(Locale.US, "%.7f", lat)}" lon="${String.format(Locale.US, "%.7f", lon)}""""
    private fun num(v: Double) = String.format(Locale.US, "%.1f", v)
    private fun time(ms: Long) = Instant.ofEpochMilli(ms).toString()
    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;").replace("'", "&apos;")
}

val MarkType.label: String
    get() = when (this) {
        MarkType.BIRD -> "Bird"
        MarkType.JUNCTION -> "Junction"
        MarkType.WATER -> "Water"
        MarkType.VIEW -> "View"
        MarkType.HAZARD -> "Hazard"
        MarkType.NOTE -> "Note"
    }
