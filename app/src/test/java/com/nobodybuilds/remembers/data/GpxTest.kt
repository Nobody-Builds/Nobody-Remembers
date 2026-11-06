package com.nobodybuilds.remembers.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

class GpxTest {
    private val trail = Trail(id = 1, name = "Ridge & <back>", startedAt = 0L)
    private val points = listOf(
        TrackPoint(trailId = 1, time = 1_000L, lat = 10.1234567, lon = 76.7654321, alt = 812.34, accuracy = 4f),
        TrackPoint(trailId = 1, time = 2_000L, lat = 10.2, lon = 76.8, alt = null, accuracy = 4f),
    )
    private val marks = listOf(Mark(trailId = 1, time = 1_500L, lat = 10.15, lon = 76.78, alt = 820.0, type = MarkType.BIRD, note = "Hornbill \"pair\""))

    private fun parse(xml: String) =
        DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }.newDocumentBuilder()
            .parse(ByteArrayInputStream(xml.toByteArray()))

    @Test fun `writes valid xml with escaped text`() {
        val doc = parse(Gpx.write(trail, points, marks))
        assertEquals("Ridge & <back>", doc.getElementsByTagName("name").item(0).textContent)
        assertEquals(2, doc.getElementsByTagName("trkpt").length)
        assertEquals(1, doc.getElementsByTagName("wpt").length)
        assertEquals("Hornbill \"pair\"", (doc.getElementsByTagName("wpt").item(0) as org.w3c.dom.Element).getElementsByTagName("name").item(0).textContent)
    }

    @Test fun `coordinates use dots and elevation is optional`() {
        val xml = Gpx.write(trail, points, emptyList())
        assertTrue(xml.contains("""lat="10.1234567" lon="76.7654321""""))
        assertTrue(xml.contains("<ele>812.3</ele>"))
        assertEquals(1, parse(xml).getElementsByTagName("ele").length)
        assertTrue(xml.contains("<time>1970-01-01T00:00:01Z</time>"))
    }
}
