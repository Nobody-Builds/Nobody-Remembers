package com.nobodybuilds.remembers.geo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PathFollowerTest {
    private val proj = LocalProjection(10.0, 76.0)

    /** A point [east] and [north] metres from the origin. */
    private fun at(east: Double, north: Double, alt: Double? = null) = RoutePoint(proj.lat(north), proj.lon(east), alt)

    /** 0,0 → 400 m north, climbing 40 m on the way. */
    private val northward = Route((0..40).map { at(0.0, it * 10.0, 100.0 + it) })

    @Test fun `route length and point lookup`() {
        assertEquals(400.0, northward.length, 0.5)
        val mid = northward.pointAt(205.0)
        assertEquals(205.0, proj.y(mid.lat), 0.5)
        assertEquals(120.5, mid.alt!!, 0.01)
    }

    @Test fun `on the path heading the right way`() {
        val f = PathFollower(northward)
        val g = f.update(at(3.0, 100.0).lat, at(3.0, 100.0).lon)
        assertTrue(g.onPath)
        assertFalse(g.arrived)
        assertEquals(100.0, g.progress, 1.0)
        assertEquals(300.0, g.remaining, 1.0)
        assertEquals(0.0, Geo.normalize180(g.bearingToTarget), 10.0) // look-ahead is north
        assertEquals(30.0, g.ascentLeft!!, 3.0)
        assertEquals(0.0, g.descentLeft!!, 1e-9)
    }

    @Test fun `off the path points back to the path`() {
        val f = PathFollower(northward, offPathMetres = 30.0)
        val p = at(80.0, 200.0)
        val g = f.update(p.lat, p.lon)
        assertFalse(g.onPath)
        assertEquals(80.0, g.offPath, 1.0)
        assertEquals(270.0, g.bearingToTarget, 2.0) // due west, back to the path
        assertEquals(280.0, g.remaining, 2.0)
    }

    @Test fun `arrives at the end`() {
        val f = PathFollower(northward)
        val end = at(2.0, 395.0)
        assertTrue(f.update(end.lat, end.lon).arrived)
    }

    @Test fun `reversed route leads back to the start`() {
        val back = northward.reversed()
        val f = PathFollower(back)
        val p = at(0.0, 300.0)
        val g = f.update(p.lat, p.lon)
        assertEquals(100.0, g.progress, 1.0)
        assertEquals(180.0, g.bearingToTarget, 10.0)
        assertEquals(0.0, g.ascentLeft!!, 1e-9)
        assertEquals(30.0, g.descentLeft!!, 3.0)
    }

    @Test fun `a path that doubles back keeps to the right stretch`() {
        // Out 200 m north along x=0, step east 10 m, back south along x=10: the two legs are 10 m apart.
        val out = (0..20).map { at(0.0, it * 10.0) }
        val back = (0..20).map { at(10.0, 200.0 - it * 10.0) }
        val route = Route(out + back)
        val f = PathFollower(route, offPathMetres = 30.0)
        // Walk the outward leg up to the turn, then down the return leg.
        for (n in 0..200 step 10) f.update(at(0.0, n.toDouble()).lat, at(0.0, n.toDouble()).lon)
        var g = f.update(at(10.0, 190.0).lat, at(10.0, 190.0).lon)
        for (n in 180 downTo 100 step 10) g = f.update(at(10.0, n.toDouble()).lat, at(10.0, n.toDouble()).lon)
        // At 100 m north on the return leg we're 310 m along, not 100 m (which is the outward leg).
        assertEquals(310.0, g.progress, 2.0)
        assertEquals(100.0, g.remaining, 2.0)
    }

    @Test fun `first fix prefers the earlier stretch on a tie`() {
        val route = Route((0..10).map { at(0.0, it * 10.0) } + (0..10).map { at(0.0, 100.0 - it * 10.0) })
        val m = route.locate(at(1.0, 50.0).lat, at(1.0, 50.0).lon)!!
        assertEquals(50.0, m.progress, 1.0)
    }

    @Test fun `turning round at the end of a spur keeps going forward`() {
        // Out 200 m to a viewpoint and straight back along the same line: the legs overlap exactly.
        val route = Route((0..20).map { at(0.0, it * 10.0) } + (1..20).map { at(0.0, 200.0 - it * 10.0) })
        val f = PathFollower(route)
        for (n in 0..200 step 10) f.update(at(0.0, n.toDouble()).lat, at(0.0, n.toDouble()).lon)
        var g = f.update(at(0.0, 195.0).lat, at(0.0, 195.0).lon)
        for (n in 190 downTo 120 step 10) g = f.update(at(0.0, n.toDouble()).lat, at(0.0, n.toDouble()).lon)
        assertEquals(280.0, g.progress, 2.0)
        assertEquals(120.0, g.remaining, 2.0)
        assertEquals(180.0, g.bearingToTarget, 10.0) // still heading south, back to the trailhead
    }

    @Test fun `a loop doesn't arrive at the trailhead before you've walked it`() {
        // A 100 m square loop that ends where it starts.
        val side = (0..10).map { it * 10.0 }
        val loop = Route(side.map { at(it, 0.0) } + side.drop(1).map { at(100.0, it) } +
            side.drop(1).map { at(100.0 - it, 100.0) } + side.drop(1).map { at(0.0, 100.0 - it) })
        val f = PathFollower(loop)
        val start = f.update(at(0.0, 0.0).lat, at(0.0, 0.0).lon)
        assertFalse(start.arrived)
        assertEquals(400.0, start.remaining, 2.0)
        var g = start
        for (n in 10..100 step 10) g = f.update(at(n.toDouble(), 0.0).lat, at(n.toDouble(), 0.0).lon)
        for (n in 10..100 step 10) g = f.update(at(100.0, n.toDouble()).lat, at(100.0, n.toDouble()).lon)
        for (n in 10..100 step 10) g = f.update(at(100.0 - n, 100.0).lat, at(100.0 - n, 100.0).lon)
        for (n in 10..95 step 10) g = f.update(at(0.0, 100.0 - n).lat, at(0.0, 100.0 - n).lon)
        assertTrue(g.arrived)
    }

    @Test fun `on-path state doesn't flicker at the edge`() {
        val f = PathFollower(northward, offPathMetres = 30.0, backOnPathMetres = 20.0)
        assertTrue(f.update(at(25.0, 100.0).lat, at(25.0, 100.0).lon).onPath)
        assertFalse(f.update(at(35.0, 110.0).lat, at(35.0, 110.0).lon).onPath)
        assertFalse(f.update(at(25.0, 120.0).lat, at(25.0, 120.0).lon).onPath) // not back until inside 20 m
        assertTrue(f.update(at(15.0, 130.0).lat, at(15.0, 130.0).lon).onPath)
    }
}
