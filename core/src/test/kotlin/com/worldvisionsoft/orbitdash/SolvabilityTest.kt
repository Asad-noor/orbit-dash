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
    fun `exactly the minimum gap on one lane is solvable`() {
        // Inner: [0,30] and [65,90] leave exactly a 35-degree gap at [30,65].
        // Outer: [30,65] blocked -> best outer gap is 30. Lanes only touch, never overlap.
        val obs = world(
            obstacle(GameWorld.LANE_INNER, 0f, 30f),
            obstacle(GameWorld.LANE_INNER, 65f, 25f),
            obstacle(GameWorld.LANE_OUTER, 30f, 35f)
        )
        assertTrue(Solvability.isSolvable(0f, obs))
    }

    @Test
    fun `gap just under the minimum on both lanes is unsolvable`() {
        // Inner: [0,30] and [64,90] leave a 34-degree gap. Outer: [30,64] -> gaps 30 and 26.
        val obs = world(
            obstacle(GameWorld.LANE_INNER, 0f, 30f),
            obstacle(GameWorld.LANE_INNER, 64f, 26f),
            obstacle(GameWorld.LANE_OUTER, 30f, 34f)
        )
        assertFalse(Solvability.isSolvable(0f, obs))
    }

    // --- double-block detection: a large gap does not help if both lanes are blocked
    // --- at the SAME angle somewhere ahead (the ball can occupy neither lane there).

    @Test
    fun `overlapping opposing-lane arcs are a double block and unsolvable despite a large gap`() {
        // Inner blocked [0,50], outer blocked [40,90]: inner still has a 40-degree gap
        // at [50,90], but angles [40,50] are walled on BOTH lanes -> lethal, unsolvable.
        val obs = world(
            obstacle(GameWorld.LANE_INNER, 0f, 50f),
            obstacle(GameWorld.LANE_OUTER, 40f, 50f)
        )
        assertTrue(Solvability.hasDoubleBlock(0f, 90f, obs, pad = 0f))
        assertFalse(Solvability.isSolvable(0f, obs))
    }

    @Test
    fun `double block across the 360 wraparound is detected`() {
        // Ball at 350. Inner [340,30] covers 340..10; outer [0,20] overlaps it at [0,10].
        val obs = world(
            obstacle(GameWorld.LANE_INNER, 340f, 30f),
            obstacle(GameWorld.LANE_OUTER, 0f, 20f)
        )
        assertTrue(Solvability.hasDoubleBlock(350f, 90f, obs, pad = 0f))
    }

    @Test
    fun `padding turns near-misses between opposing lanes into double blocks`() {
        // Inner [0,30], outer [40,20]: a 10-degree hop window between them.
        val obs = world(
            obstacle(GameWorld.LANE_INNER, 0f, 30f),
            obstacle(GameWorld.LANE_OUTER, 40f, 20f)
        )
        assertFalse(Solvability.hasDoubleBlock(0f, 90f, obs, pad = 0f))   // raw arcs don't touch
        assertTrue(Solvability.hasDoubleBlock(0f, 90f, obs, pad = 6f))    // padded arcs do
    }

    @Test
    fun `opposing arcs far from the window are not flagged`() {
        // Same overlapping pair, but 180+ degrees behind the ball and outside the window.
        val obs = world(
            obstacle(GameWorld.LANE_INNER, 180f, 50f),
            obstacle(GameWorld.LANE_OUTER, 200f, 50f)
        )
        assertFalse(Solvability.hasDoubleBlock(0f, 90f, obs, pad = 0f))
        assertTrue(Solvability.isSolvable(0f, obs))
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
