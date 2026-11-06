package com.nobodybuilds.remembers.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class FormatTest {
    init { Locale.setDefault(Locale.UK) }

    @Test fun distances() {
        assertEquals("0 m", formatDistance(1.0))
        assertEquals("640 m", formatDistance(641.0))
        assertEquals("1.24 km", formatDistance(1_240.0))
        assertEquals("12.5 km", formatDistance(12_480.0))
    }

    @Test fun durations() {
        assertEquals("45 s", formatDuration(45_000))
        assertEquals("12 min", formatDuration(12 * 60_000L))
        assertEquals("1 h 05 min", formatDuration(65 * 60_000L))
        assertEquals("1:01:01", formatClock(3_661_000))
    }

    @Test fun `naismith adds a minute per 10 m of climb`() {
        assertEquals(60 * 60_000L, naismithMillis(5_000.0, 0.0))
        assertEquals(70 * 60_000L, naismithMillis(5_000.0, 100.0))
    }

    @Test fun `turn hints`() {
        assertEquals("Straight on", turnHint(10.0))
        assertEquals("Bear left", turnHint(-40.0))
        assertEquals("Turn right", turnHint(100.0))
        assertEquals("Turn round", turnHint(-170.0))
    }
}
