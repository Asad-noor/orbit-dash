package com.worldvisionsoft.orbitdash

import com.badlogic.gdx.utils.Pool

/** A collectible placed at a fixed angle on one lane. Pooled — plain data. */
class Coin : Pool.Poolable {
    /** Lane index: [GameWorld.LANE_INNER] or [GameWorld.LANE_OUTER]. */
    var lane = GameWorld.LANE_INNER
    /** Position angle in degrees, [0, 360). */
    var angle = 0f
    /** Remaining lifetime in seconds; despawns at 0. */
    var life = 0f

    fun set(lane: Int, angle: Float, life: Float) {
        this.lane = lane
        this.angle = AngleMath.normalize(angle)
        this.life = life
    }

    override fun reset() {
        lane = GameWorld.LANE_INNER
        angle = 0f
        life = 0f
    }
}
