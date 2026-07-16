package com.worldvisionsoft.orbitdash

/** Angle utilities on a 360-degree circle, careful about wraparound. Pure math — headless testable. */
object AngleMath {

    /** Normalizes [deg] into [0, 360). */
    fun normalize(deg: Float): Float {
        var d = deg % 360f
        if (d < 0f) d += 360f
        return d
    }

    /**
     * Whether arc A ([aStart], length [aLen]) overlaps arc B ([bStart], length [bLen]) on a circle.
     * Starts may be any angle (normalized internally); lengths must be in (0, 360].
     * Arcs that merely touch at an endpoint do NOT count as overlapping.
     */
    fun arcsOverlap(aStart: Float, aLen: Float, bStart: Float, bLen: Float): Boolean {
        if (aLen >= 360f || bLen >= 360f) return true
        // Offset of B's start relative to A's start, in [0, 360).
        // Overlap iff B starts inside A, or B extends across 360 back into A.
        val delta = normalize(bStart - aStart)
        return delta < aLen || delta > 360f - bLen
    }
}
