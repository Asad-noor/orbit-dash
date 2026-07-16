package com.worldvisionsoft.orbitdash

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AngleMathTest {

    @Test
    fun `normalize maps into 0 until 360`() {
        assertEquals(0f, AngleMath.normalize(0f), 1e-4f)
        assertEquals(0f, AngleMath.normalize(360f), 1e-4f)
        assertEquals(0f, AngleMath.normalize(720f), 1e-4f)
        assertEquals(330f, AngleMath.normalize(-30f), 1e-4f)
        assertEquals(10f, AngleMath.normalize(370f), 1e-4f)
        assertEquals(350f, AngleMath.normalize(-370f), 1e-4f)
    }

    @Test
    fun `simple overlap`() {
        // [10, 50] vs [40, 70]
        assertTrue(AngleMath.arcsOverlap(10f, 40f, 40f, 30f))
        // symmetric
        assertTrue(AngleMath.arcsOverlap(40f, 30f, 10f, 40f))
    }

    @Test
    fun `simple disjoint`() {
        // [10, 40] vs [100, 140]
        assertFalse(AngleMath.arcsOverlap(10f, 30f, 100f, 40f))
        assertFalse(AngleMath.arcsOverlap(100f, 40f, 10f, 30f))
    }

    @Test
    fun `containment counts as overlap`() {
        // [0, 90] contains [30, 40]
        assertTrue(AngleMath.arcsOverlap(0f, 90f, 30f, 10f))
        assertTrue(AngleMath.arcsOverlap(30f, 10f, 0f, 90f))
    }

    @Test
    fun `touching endpoints is not overlap`() {
        // [0, 30] and [30, 40] merely touch at 30
        assertFalse(AngleMath.arcsOverlap(0f, 30f, 30f, 10f))
        assertFalse(AngleMath.arcsOverlap(30f, 10f, 0f, 30f))
        // touching across the wraparound: [350, 360] and [0, 20]
        assertFalse(AngleMath.arcsOverlap(350f, 10f, 0f, 20f))
    }

    @Test
    fun `overlap across the 360 wraparound`() {
        // A = [350, 370] i.e. wraps to 10; B = [5, 15] overlaps the wrapped part
        assertTrue(AngleMath.arcsOverlap(350f, 20f, 5f, 10f))
        assertTrue(AngleMath.arcsOverlap(5f, 10f, 350f, 20f))
        // B = [15, 25] does not
        assertFalse(AngleMath.arcsOverlap(350f, 20f, 15f, 10f))
        assertFalse(AngleMath.arcsOverlap(15f, 10f, 350f, 20f))
    }

    @Test
    fun `both arcs wrap`() {
        // A = [340, 380]->20, B = [355, 415]->55: both cross 0 and overlap
        assertTrue(AngleMath.arcsOverlap(340f, 40f, 355f, 60f))
    }

    @Test
    fun `negative start angles are normalized`() {
        // A starting at -10 is the same as 350
        assertTrue(AngleMath.arcsOverlap(-10f, 20f, 5f, 10f))
        assertFalse(AngleMath.arcsOverlap(-10f, 20f, 15f, 10f))
    }

    @Test
    fun `full circle arc overlaps everything`() {
        assertTrue(AngleMath.arcsOverlap(0f, 360f, 123f, 1f))
        assertTrue(AngleMath.arcsOverlap(123f, 1f, 0f, 360f))
    }
}
