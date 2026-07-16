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

    /**
     * Whether any angle relevant to the lookahead window is blocked on BOTH lanes at once.
     * Such a "double wall" is lethal no matter how large the gaps elsewhere are: the ball
     * cannot occupy either lane at that angle. Arcs are inflated by [pad] per side so that
     * every forced lane-hop between opposing obstacles keeps a physically usable window.
     * Slightly conservative: a pair is flagged when both arcs touch the window and overlap
     * each other, even if the overlap itself sits just outside the window.
     */
    fun hasDoubleBlock(ballAngle: Float, lookahead: Float, obstacles: GdxArray<Obstacle>, pad: Float): Boolean {
        for (i in 0 until obstacles.size) {
            val a = obstacles[i]
            if (a.lane != GameWorld.LANE_INNER) continue
            val aStart = a.angle - pad
            val aLen = a.arcLength + 2f * pad
            if (!AngleMath.arcsOverlap(ballAngle, lookahead, aStart, aLen)) continue
            for (j in 0 until obstacles.size) {
                val b = obstacles[j]
                if (b.lane != GameWorld.LANE_OUTER) continue
                val bStart = b.angle - pad
                val bLen = b.arcLength + 2f * pad
                if (!AngleMath.arcsOverlap(ballAngle, lookahead, bStart, bLen)) continue
                if (AngleMath.arcsOverlap(aStart, aLen, bStart, bLen)) return true
            }
        }
        return false
    }

    /**
     * Whether the state satisfies the solvability guarantee for a ball at [ballAngle]:
     * (1) at least one lane keeps a free gap of [minGap] within [lookahead] ahead, AND
     * (2) no angle in the window is blocked on both lanes at once ([pad]-inflated).
     * Defaults are the base-speed spec values; GameWorld passes speed-scaled ones.
     */
    fun isSolvable(
        ballAngle: Float,
        obstacles: GdxArray<Obstacle>,
        lookahead: Float = GameConfig.SOLVABILITY_LOOKAHEAD,
        minGap: Float = GameConfig.SOLVABILITY_MIN_GAP,
        pad: Float = 0f
    ): Boolean {
        if (hasDoubleBlock(ballAngle, lookahead, obstacles, pad)) return false
        return maxFreeGap(ballAngle, lookahead, obstacles, GameWorld.LANE_INNER) >= minGap ||
            maxFreeGap(ballAngle, lookahead, obstacles, GameWorld.LANE_OUTER) >= minGap
    }
}
