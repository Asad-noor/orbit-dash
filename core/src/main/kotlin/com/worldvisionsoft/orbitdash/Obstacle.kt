package com.worldvisionsoft.orbitdash

import com.badlogic.gdx.utils.Pool

/** An arc segment occupying exactly one lane, rotating around the center. Pooled — plain data. */
class Obstacle : Pool.Poolable {
    /** Lane index: [GameWorld.LANE_INNER] or [GameWorld.LANE_OUTER]. */
    var lane = GameWorld.LANE_INNER
    /** Arc start angle in degrees, [0, 360). The arc spans [angle, angle + arcLength]. */
    var angle = 0f
    /** Arc length in degrees. */
    var arcLength = 60f
    /** Rotation speed in degrees per second; sign is the direction. */
    var rotationSpeed = 0f
    /** Remaining lifetime in seconds; despawns at 0. */
    var life = 0f

    fun set(lane: Int, angle: Float, arcLength: Float, rotationSpeed: Float, life: Float) {
        this.lane = lane
        this.angle = AngleMath.normalize(angle)
        this.arcLength = arcLength
        this.rotationSpeed = rotationSpeed
        this.life = life
    }

    override fun reset() {
        lane = GameWorld.LANE_INNER
        angle = 0f
        arcLength = 60f
        rotationSpeed = 0f
        life = 0f
    }
}
