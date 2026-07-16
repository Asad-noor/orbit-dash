package com.worldvisionsoft.orbitdash

import com.badlogic.gdx.math.RandomXS128
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The solvability guarantee is an invariant over EVERY world state — not just at spawn
 * time. Ball travel sweeps the lookahead window onto unchecked track regions, and
 * rotating obstacles close once-legal gaps; GameWorld.step must repair such states.
 */
class GameWorldSolvabilityTest {

    private fun obstacle(lane: Int, angle: Float, arcLength: Float, life: Float = 10f): Obstacle =
        Obstacle().also { it.set(lane, angle, arcLength, rotationSpeed = 0f, life = life) }

    @Test
    fun `step repairs an unsolvable state created by obstacle drift`() {
        val world = GameWorld(RandomXS128(1))
        // Force a state the spawn check would never have created: both lanes walled
        // off directly ahead of the ball (ball starts at angle 0).
        world.obstacles.add(obstacle(GameWorld.LANE_INNER, 5f, 90f))
        world.obstacles.add(obstacle(GameWorld.LANE_OUTER, 5f, 90f))
        assertTrue(!Solvability.isSolvable(0f, world.obstacles))

        world.step(GameConfig.FIXED_DT)

        assertTrue(Solvability.isSolvable(world.ballAngle, world.obstacles))
    }

    @Test
    fun `repair removes the obstacle with the least remaining life first`() {
        val world = GameWorld(RandomXS128(1))
        val longLived = obstacle(GameWorld.LANE_INNER, 5f, 90f, life = 10f)
        val shortLived = obstacle(GameWorld.LANE_OUTER, 5f, 90f, life = 1f)
        world.obstacles.add(longLived)
        world.obstacles.add(shortLived)

        world.step(GameConfig.FIXED_DT)

        // Removing the short-lived outer wall alone restores a >=35-degree gap.
        assertTrue(world.obstacles.contains(longLived, true))
        assertTrue(!world.obstacles.contains(shortLived, true))
    }

    @Test
    fun `solvable states are left untouched`() {
        val world = GameWorld(RandomXS128(1))
        // One lane blocked, the other fully free: solvable, nothing may be removed.
        world.obstacles.add(obstacle(GameWorld.LANE_INNER, 5f, 90f))

        world.step(GameConfig.FIXED_DT)

        assertEquals(1, world.obstacles.size)
    }

    @Test
    fun `invariant holds after every step across long seeded runs`() {
        // End-to-end regression: several minutes of simulated play across multiple seeds,
        // with periodic lane toggles, asserting the guarantee after every single step.
        for (seed in 1L..5L) {
            val world = GameWorld(RandomXS128(seed))
            var steps = 0
            repeat(3 * 60 * 60) { // 3 simulated minutes per seed at 60 Hz
                world.step(GameConfig.FIXED_DT)
                steps++
                if (steps % 47 == 0) world.toggleLane() // arbitrary off-beat cadence
                if (world.state == GameWorld.State.GAME_OVER) world.reset()
                assertTrue(
                    "seed=$seed step=$steps ball=${world.ballAngle} obstacles=${world.obstacles.size}",
                    Solvability.isSolvable(
                        world.ballAngle, world.obstacles,
                        world.currentSolvabilityLookahead(),
                        world.currentSolvabilityMinGap(),
                        world.currentSolvabilityPad()
                    )
                )
            }
        }
    }
}
