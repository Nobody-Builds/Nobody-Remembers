package com.nobodybuilds.remembers.geo

import org.junit.Assert.assertEquals
import org.junit.Test

class ClimbTest {
    @Test fun `noise below the threshold is ignored`() {
        val c = climb(listOf(100.0, 101.5, 99.0, 100.8, 99.5, 100.0), threshold = 3.0)
        assertEquals(0.0, c.ascent, 1e-9)
        assertEquals(0.0, c.descent, 1e-9)
    }

    @Test fun `a slow steady climb still counts`() {
        val c = climb((0..50).map { 100.0 + it }, threshold = 3.0)
        assertEquals(48.0, c.ascent, 1e-9) // last 2 m stay under the threshold
        assertEquals(0.0, c.descent, 1e-9)
    }

    @Test fun `up then down`() {
        val c = climb(listOf(100.0, 120.0, 140.0, 110.0, 90.0), threshold = 3.0)
        assertEquals(40.0, c.ascent, 1e-9)
        assertEquals(50.0, c.descent, 1e-9)
    }

    @Test fun `counter carries on from saved totals`() {
        val counter = ClimbCounter(threshold = 2.0, ascent = 10.0, descent = 5.0, reference = 200.0)
        counter.add(205.0)
        counter.add(199.0)
        assertEquals(15.0, counter.ascent, 1e-9)
        assertEquals(11.0, counter.descent, 1e-9)
    }
}
