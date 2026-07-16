package com.worldvisionsoft.orbitdash

import com.badlogic.gdx.utils.Array as GdxArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SolvabilityTest {

    private fun obstacle(lane: Int, angle: Float, arcLength: Float): Obstacle =
        Obstacle().also { it.set(lane, angle, arcLength, rotationSpeed = 0f, life = 10f) }

    private fun world(vararg obstacles: Obstacle): GdxArray<Obstacle> {
        val arr = GdxArray<Obstacle>()
        obstacles.forEach { arr.add(it) }
        return arr
    }

    // --- maxFreeGap ---

    @Test
    fun `empty lane has the whole lookahead free`() {
        val gap = Solvability.maxFreeGap(0f, 90f, world(), GameWorld.LANE_INNER)
        assertEquals(90f, gap, 1e-3f)
    }

    @Test
    fun `obstacle on the other lane does not reduce the gap`() {
        val obs = world(obstacle(GameWorld.LANE_OUTER, 10f, 60f))
        assertEquals(90f, Solvability.maxFreeGap(0f, 90f, obs, GameWorld.LANE_INNER), 1e-3f)
    }

    @Test
    fun `single obstacle in the middle of the window splits it`() {
        // Ball at 0, obstacle [30, 70]: gaps are [0,30] and [70,90] -> max 30
        val obs = world(obstacle(GameWorld.LANE_INNER, 30f, 40f))
        assertEquals(30f, Solvability.maxFreeGap(0f, 90f, obs, GameWorld.LANE_INNER), 1e-3f)
    }

    @Test
    fun `obstacle extending past the window end is clipped`() {
        // Ball at 0, obstacle [50, 130]: blocked [50,90] -> max gap [0,50] = 50
        val obs = world(obstacle(GameWorld.LANE_INNER, 50f, 80f))
        assertEquals(50f, Solvability.maxFreeGap(0f, 90f, obs, GameWorld.LANE_INNER), 1e-3f)
    }

    @Test
    fun `window that crosses 360 is handled`() {
        // Ball at 340, window [340, 70]. Obstacle [350, 30] -> window-local [10, 40].
        // Gaps: [0,10] and [40,90] -> max 50.
        val obs = world(obstacle(GameWorld.LANE_INNER, 350f, 30f))
        assertEquals(50f, Solvability.maxFreeGap(340f, 90f, obs, GameWorld.LANE_INNER), 1e-3f)
    }

    @Test
    fun `obstacle arc that wraps past 360 into the window is handled`() {
        // Ball at 350. Obstacle [300, 80] covers 300..380, i.e. wraps to 20 past the ball.
        // Window-local: blocked [0, 30] -> max gap 60.
        val obs = world(obstacle(GameWorld.LANE_INNER, 300f, 80f))
        assertEquals(60f, Solvability.maxFreeGap(350f, 90f, obs, GameWorld.LANE_INNER), 1e-3f)
    }

    @Test
    fun `multiple unsorted obstacles merge correctly`() {
        // Ball at 0. Obstacles [60,80] and [10,35] -> gaps [0,10], [35,60], [80,90] -> max 25
        val obs = world(
            obstacle(GameWorld.LANE_INNER, 60f, 20f),
            obstacle(GameWorld.LANE_INNER, 10f, 25f)
        )
        assertEquals(25f, Solvability.maxFreeGap(0f, 90f, obs, GameWorld.LANE_INNER), 1e-3f)
    }

    // --- isSolvable (35-degree gap within 90 degrees ahead, on at least one lane) ---

    @Test
    fun `no obstacles is solvable`() {
        assertTrue(Solvability.isSolvable(0f, world()))
    }

    @Test
    fun `both lanes fully blocked ahead is unsolvable`() {
        val obs = world(
            obstacle(GameWorld.LANE_INNER, 0f, 90f),
            obstacle(GameWorld.LANE_OUTER, 0f, 90f)
        )
        assertFalse(Solvability.isSolvable(0f, obs))
    }

    @Test
    fun `one lane blocked but the other has exactly the minimum gap is solvable`() {
        // Inner fully blocked. Outer: [0,30] and [65,90] leave exactly a 35-degree gap.
        val obs = world(
            obstacle(GameWorld.LANE_INNER, 0f, 90f),
            obstacle(GameWorld.LANE_OUTER, 0f, 30f),
            obstacle(GameWorld.LANE_OUTER, 65f, 25f)
        )
        assertTrue(Solvability.isSolvable(0f, obs))
    }

    @Test
    fun `one lane blocked and the other gap just under the minimum is unsolvable`() {
        // Inner fully blocked. Outer: [0,30] and [64,90] leave a 34-degree gap.
        val obs = world(
            obstacle(GameWorld.LANE_INNER, 0f, 90f),
            obstacle(GameWorld.LANE_OUTER, 0f, 30f),
            obstacle(GameWorld.LANE_OUTER, 64f, 26f)
        )
        assertFalse(Solvability.isSolvable(0f, obs))
    }

    @Test
    fun `blockage far ahead of the window does not matter`() {
        // Obstacles beyond the 90-degree lookahead on both lanes.
        val obs = world(
            obstacle(GameWorld.LANE_INNER, 120f, 80f),
            obstacle(GameWorld.LANE_OUTER, 200f, 80f)
        )
        assertTrue(Solvability.isSolvable(0f, obs))
    }

    @Test
    fun `solvability window respects ball position with wraparound`() {
        // Ball at 300, window [300, 30]. Both lanes blocked across the wrap.
        val obs = world(
            obstacle(GameWorld.LANE_INNER, 290f, 110f), // covers 290..400 -> whole window
            obstacle(GameWorld.LANE_OUTER, 290f, 110f)
        )
        assertFalse(Solvability.isSolvable(300f, obs))
        // Free the outer lane -> solvable again.
        val obs2 = world(obstacle(GameWorld.LANE_INNER, 290f, 110f))
        assertTrue(Solvability.isSolvable(300f, obs2))
    }
}
