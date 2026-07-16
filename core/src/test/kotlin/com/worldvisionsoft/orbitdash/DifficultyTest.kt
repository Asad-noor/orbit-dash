package com.worldvisionsoft.orbitdash

import org.junit.Assert.assertEquals
import org.junit.Test

class DifficultyTest {

    @Test
    fun `ball speed at score 0 is the starting speed`() {
        assertEquals(120f, Difficulty.ballSpeed(0), 1e-3f)
    }

    @Test
    fun `ball speed at score 50 is halfway`() {
        assertEquals(180f, Difficulty.ballSpeed(50), 1e-3f)
    }

    @Test
    fun `ball speed at score 100 is the maximum`() {
        assertEquals(240f, Difficulty.ballSpeed(100), 1e-3f)
    }

    @Test
    fun `ball speed is capped beyond score 100`() {
        assertEquals(240f, Difficulty.ballSpeed(150), 1e-3f)
        assertEquals(240f, Difficulty.ballSpeed(100000), 1e-3f)
    }

    @Test
    fun `spawn interval at score 0 is the starting interval`() {
        assertEquals(2.0f, Difficulty.spawnInterval(0), 1e-3f)
    }

    @Test
    fun `spawn interval at score 50 is halfway`() {
        assertEquals(1.45f, Difficulty.spawnInterval(50), 1e-3f)
    }

    @Test
    fun `spawn interval at score 100 is the minimum`() {
        assertEquals(0.9f, Difficulty.spawnInterval(100), 1e-3f)
    }

    @Test
    fun `spawn interval is capped beyond score 100`() {
        assertEquals(0.9f, Difficulty.spawnInterval(9999), 1e-3f)
    }
}
