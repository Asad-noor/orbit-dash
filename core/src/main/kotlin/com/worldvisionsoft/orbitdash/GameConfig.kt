package com.worldvisionsoft.orbitdash

/**
 * Every tunable gameplay constant in one place, so the game can be tuned by feel on-device.
 * Fractions are relative to the shorter viewport dimension unless noted otherwise.
 */
object GameConfig {

    // --- Viewport (world units; ExtendViewport extends the longer axis) ---
    const val MIN_WORLD_WIDTH = 480f
    const val MIN_WORLD_HEIGHT = 800f

    // --- Track geometry ---
    /** Inner lane radius as a fraction of the shorter viewport dimension. */
    const val INNER_LANE_RADIUS_FRAC = 0.40f
    /** Outer lane radius as a fraction of the shorter viewport dimension. */
    const val OUTER_LANE_RADIUS_FRAC = 0.46f
    /** Ball radius as a fraction of the shorter viewport dimension. */
    const val BALL_RADIUS_FRAC = 0.022f
    /** Coin radius as a fraction of the shorter viewport dimension. */
    const val COIN_RADIUS_FRAC = 0.015f

    // --- Ball speed difficulty curve (degrees per second) ---
    const val BALL_SPEED_START = 120f
    const val BALL_SPEED_MAX = 240f
    /** Score at which ball speed and spawn interval reach their extremes. */
    const val DIFFICULTY_MAX_SCORE = 100

    // --- Obstacle spawning ---
    /** Seconds between spawn waves at score 0. */
    const val SPAWN_INTERVAL_START = 2.0f
    /** Seconds between spawn waves at DIFFICULTY_MAX_SCORE and beyond. */
    const val SPAWN_INTERVAL_MIN = 0.9f
    /** Obstacle arc length range, degrees. */
    const val OBSTACLE_ARC_MIN = 40f
    const val OBSTACLE_ARC_MAX = 80f
    /** Obstacle rotation speed range, degrees per second (direction randomized). */
    const val OBSTACLE_ROT_SPEED_MIN = 10f
    const val OBSTACLE_ROT_SPEED_MAX = 25f
    /** Seconds an obstacle stays alive before despawning. */
    const val OBSTACLE_LIFETIME = 12f
    /** Hard caps so pools stay bounded and the field never clogs. */
    const val MAX_OBSTACLES = 12
    const val MAX_COINS = 5
    /** Retries when a spawn placement fails fairness/solvability checks. */
    const val SPAWN_ATTEMPTS = 10
    /** New obstacles on the ball's lane must not overlap this window around the ball (degrees). */
    const val SAFE_SPAWN_BEHIND = 15f
    const val SAFE_SPAWN_AHEAD = 45f

    // --- Solvability guarantee ---
    // LOOKAHEAD and MIN_GAP are the values at BALL_SPEED_START; GameWorld scales both
    // linearly with the current ball speed so the guarantee represents constant reaction
    // TIME (35 deg @ 120 deg/s = 0.29s) instead of shrinking to nothing at high speed.
    /** Window ahead of the ball that must stay passable, degrees (at base speed). */
    const val SOLVABILITY_LOOKAHEAD = 90f
    /** Minimum free gap required on at least one lane within the lookahead, degrees (at base speed). */
    const val SOLVABILITY_MIN_GAP = 35f
    /**
     * Safety padding (degrees, per side, on top of the ball's angular half-width) used when
     * checking that opposing-lane obstacles never block the same angle: guarantees every
     * forced lane-hop has a usable window even at 60Hz step quantization.
     */
    const val SOLVABILITY_PAD = 6f

    // --- Coins ---
    /** Chance that a spawn wave also places a coin. */
    const val COIN_SPAWN_CHANCE = 0.5f
    /** Seconds a coin stays alive before despawning. */
    const val COIN_LIFETIME = 10f

    // --- Fixed timestep ---
    const val FIXED_DT = 1f / 60f
    /** Render deltas are clamped to this to avoid a spiral of death after resume/hitches. */
    const val MAX_FRAME_DELTA = 0.25f

    // --- HUD ---
    /** Top margin for the score text in world units; clears typical portrait camera cutouts. */
    const val SCORE_TOP_MARGIN = 80f
}
