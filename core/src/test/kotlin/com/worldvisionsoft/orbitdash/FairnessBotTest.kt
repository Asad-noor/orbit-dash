package com.worldvisionsoft.orbitdash

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.RandomXS128
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * "Plays the game" headlessly for a long time: a greedy bot with perfect reactions
 * (but a simple strategy — hop when the current lane's nearest threat is closer than
 * the other lane's and the other lane is free right here). If the solvability
 * guarantee truly holds at every step, this bot must NEVER die, including at the
 * capped top speed of 240 deg/s. A single death means an unfair state existed.
 */
class FairnessBotTest {

    private fun laneHalfWidthDeg(world: GameWorld, lane: Int): Float =
        (world.ballRadius / world.laneRadius(lane)) * MathUtils.radiansToDegrees

    /** Degrees ahead of the ball until the nearest blocked angle on [lane]; 360 if free. */
    private fun nearestThreat(world: GameWorld, lane: Int): Float {
        val pad = laneHalfWidthDeg(world, lane)
        var nearest = 360f
        for (i in 0 until world.obstacles.size) {
            val o = world.obstacles[i]
            if (o.lane != lane) continue
            val start = o.angle - pad
            val len = o.arcLength + 2f * pad
            val d = if (AngleMath.arcsOverlap(start, len, world.ballAngle, 0.001f)) 0f
            else AngleMath.normalize(start - world.ballAngle)
            if (d < nearest) nearest = d
        }
        return nearest
    }

    /** Whether [lane] is free at the ball's current angle (with clearance). */
    private fun laneFreeHere(world: GameWorld, lane: Int): Boolean {
        val pad = laneHalfWidthDeg(world, lane) + 1f
        for (i in 0 until world.obstacles.size) {
            val o = world.obstacles[i]
            if (o.lane == lane &&
                AngleMath.arcsOverlap(o.angle - pad, o.arcLength + 2f * pad, world.ballAngle, 0.001f)
            ) return false
        }
        return true
    }

    private fun botAct(world: GameWorld) {
        val current = world.ballLane
        val other = 1 - current
        val threatCurrent = nearestThreat(world, current)
        val threatOther = nearestThreat(world, other)
        // React when a wall is within ~4 steps of travel.
        val reactWindow = Difficulty.ballSpeed(world.score) * GameConfig.FIXED_DT * 4f
        if (threatCurrent < reactWindow && threatOther > threatCurrent && laneFreeHere(world, other)) {
            world.toggleLane()
        }
    }

    @Test
    fun `a perfect-reaction bot never dies across 20 simulated minutes per seed`() {
        for (seed in 1L..3L) {
            val world = GameWorld(RandomXS128(seed))
            var deaths = 0
            var maxScore = 0
            repeat(20 * 60 * 60) { // 20 minutes at 60 Hz
                botAct(world)
                world.step(GameConfig.FIXED_DT)
                if (world.score > maxScore) maxScore = world.score
                if (world.state == GameWorld.State.GAME_OVER) {
                    deaths++
                    world.reset()
                }
            }
            assertEquals("unfair state existed (seed=$seed, reached score $maxScore)", 0, deaths)
            // Sanity: the run must have reached the difficulty cap, or we never tested top speed.
            assertTrue(
                "run too short to reach max difficulty (seed=$seed, score $maxScore)",
                maxScore > GameConfig.DIFFICULTY_MAX_SCORE
            )
        }
    }
}
