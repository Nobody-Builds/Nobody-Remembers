package com.nobodybuilds.remembers.geo

import org.junit.Assert.assertEquals
import org.junit.Test

class GeoTest {
    @Test fun `one degree of latitude is about 111 km`() {
        assertEquals(111_195.0, Geo.distance(10.0, 76.0, 11.0, 76.0), 50.0)
    }

    @Test fun `distance is zero to itself`() {
        assertEquals(0.0, Geo.distance(9.93, 76.26, 9.93, 76.26), 1e-9)
    }

    @Test fun `bearings point the right way`() {
        assertEquals(0.0, Geo.bearing(0.0, 0.0, 1.0, 0.0), 0.01)
        assertEquals(90.0, Geo.bearing(0.0, 0.0, 0.0, 1.0), 0.01)
        assertEquals(180.0, Geo.bearing(1.0, 0.0, 0.0, 0.0), 0.01)
        assertEquals(270.0, Geo.bearing(0.0, 1.0, 0.0, 0.0), 0.01)
    }

    @Test fun `angles normalise`() {
        assertEquals(10.0, Geo.normalize360(370.0), 1e-9)
        assertEquals(350.0, Geo.normalize360(-10.0), 1e-9)
        assertEquals(-170.0, Geo.normalize180(190.0), 1e-9)
        assertEquals(180.0, Geo.normalize180(-180.0), 1e-9)
    }

    @Test fun `cardinal names`() {
        assertEquals("N", Geo.cardinal(359.0))
        assertEquals("NE", Geo.cardinal(44.0))
        assertEquals("S", Geo.cardinal(181.0))
        assertEquals("NW", Geo.cardinal(320.0))
    }

    @Test fun `projection round-trips`() {
        val p = LocalProjection(10.0, 76.0)
        assertEquals(10.01, p.lat(p.y(10.01)), 1e-9)
        assertEquals(76.02, p.lon(p.x(76.02)), 1e-9)
        assertEquals(Geo.distance(10.0, 76.0, 10.0, 76.01), p.x(76.01), 0.5)
    }
}
