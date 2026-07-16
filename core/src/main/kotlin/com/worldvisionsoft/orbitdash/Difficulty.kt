package com.worldvisionsoft.orbitdash

import kotlin.math.min

/** Difficulty curves: linear from score 0 to [GameConfig.DIFFICULTY_MAX_SCORE], then capped. */
object Difficulty {

    private fun progress(score: Int): Float =
        min(score, GameConfig.DIFFICULTY_MAX_SCORE).toFloat() / GameConfig.DIFFICULTY_MAX_SCORE

    /** Ball angular speed in degrees per second for the given [score]. */
    fun ballSpeed(score: Int): Float =
        GameConfig.BALL_SPEED_START +
            (GameConfig.BALL_SPEED_MAX - GameConfig.BALL_SPEED_START) * progress(score)

    /** Seconds between spawn waves for the given [score]. */
    fun spawnInterval(score: Int): Float =
        GameConfig.SPAWN_INTERVAL_START +
            (GameConfig.SPAWN_INTERVAL_MIN - GameConfig.SPAWN_INTERVAL_START) * progress(score)
}
