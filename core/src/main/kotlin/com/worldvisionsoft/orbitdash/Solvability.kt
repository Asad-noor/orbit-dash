package com.worldvisionsoft.orbitdash

import com.badlogic.gdx.utils.Array as GdxArray
import kotlin.math.max
import kotlin.math.min

/**
 * The solvability guarantee: within the next [GameConfig.SOLVABILITY_LOOKAHEAD] degrees ahead
 * of the ball, at least one lane must have a contiguous free gap of at least
 * [GameConfig.SOLVABILITY_MIN_GAP] degrees. The spawner checks this before committing a placement.
 *
 * Uses preallocated scratch buffers — zero allocation, single-threaded use only (the update loop).
 */
object Solvability {

    // 2 intervals max per obstacle (an arc can enter the window directly and by wrapping past 360).
    private const val MAX_INTERVALS = 2 * (GameConfig.MAX_OBSTACLES + 1)
    private val starts = FloatArray(MAX_INTERVALS)
    private val ends = FloatArray(MAX_INTERVALS)

    /**
     * Largest contiguous free angular gap (degrees) on [lane] within [lookahead] degrees
     * ahead of [ballAngle], given the current [obstacles]. Ball travel direction is +angle.
     */
    fun maxFreeGap(ballAngle: Float, lookahead: Float, obstacles: GdxArray<Obstacle>, lane: Int): Float {
        // Collect blocked intervals in window-local coordinates [0, lookahead].
        var n = 0
        for (i in 0 until obstacles.size) {
            val o = obstacles[i]
            if (o.lane != lane) continue
            val d0 = AngleMath.normalize(o.angle - ballAngle) // arc start, degrees ahead of ball
            val d1 = d0 + o.arcLength                          // arc end, may exceed 360
            if (d0 < lookahead) {
                starts[n] = d0
                ends[n] = min(d1, lookahead)
                n++
            }
            if (d1 > 360f) { // arc wraps past the ball position back into the window start
                val e = min(d1 - 360f, lookahead)
                starts[n] = 0f
                ends[n] = e
                n++
            }
        }

        // Insertion sort by interval start (n is tiny).
        for (i in 1 until n) {
            val s = starts[i]
            val e = ends[i]
            var j = i - 1
            while (j >= 0 && starts[j] > s) {
                starts[j + 1] = starts[j]
                ends[j + 1] = ends[j]
                j--
            }
            starts[j + 1] = s
            ends[j + 1] = e
        }

        // Sweep for the largest gap between merged blocked intervals.
        var maxGap = 0f
        var cursor = 0f
        for (i in 0 until n) {
            if (starts[i] > cursor) maxGap = max(maxGap, starts[i] - cursor)
            cursor = max(cursor, ends[i])
        }
        maxGap = max(maxGap, lookahead - cursor)
        return maxGap
    }

    /** Whether the state satisfies the solvability guarantee for a ball at [ballAngle]. */
    fun isSolvable(ballAngle: Float, obstacles: GdxArray<Obstacle>): Boolean {
        val lookahead = GameConfig.SOLVABILITY_LOOKAHEAD
        val minGap = GameConfig.SOLVABILITY_MIN_GAP
        return maxFreeGap(ballAngle, lookahead, obstacles, GameWorld.LANE_INNER) >= minGap ||
            maxFreeGap(ballAngle, lookahead, obstacles, GameWorld.LANE_OUTER) >= minGap
    }
}
