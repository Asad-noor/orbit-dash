package com.worldvisionsoft.orbitdash

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.RandomXS128
import com.badlogic.gdx.utils.Pool
import com.badlogic.gdx.utils.Array as GdxArray

/**
 * Pure game logic — no rendering, no Gdx.app/graphics/input statics, headlessly testable.
 * Angles are degrees; the ball travels in the +angle (counter-clockwise) direction.
 * Positions are polar around the track center; the renderer applies the screen-space center.
 */
class GameWorld(private val rng: RandomXS128 = RandomXS128()) {

    enum class State { RUNNING, GAME_OVER }

    companion object {
        const val LANE_INNER = 0
        const val LANE_OUTER = 1
    }

    // --- Geometry, derived from the shorter viewport dimension via setGeometry() ---
    var innerRadius = GameConfig.MIN_WORLD_WIDTH * GameConfig.INNER_LANE_RADIUS_FRAC; private set
    var outerRadius = GameConfig.MIN_WORLD_WIDTH * GameConfig.OUTER_LANE_RADIUS_FRAC; private set
    var ballRadius = GameConfig.MIN_WORLD_WIDTH * GameConfig.BALL_RADIUS_FRAC; private set
    var coinRadius = GameConfig.MIN_WORLD_WIDTH * GameConfig.COIN_RADIUS_FRAC; private set

    // --- State ---
    var state = State.RUNNING; private set
    var score = 0; private set
    var ballAngle = 0f; private set
    var ballLane = LANE_INNER; private set

    val obstacles = GdxArray<Obstacle>(false, GameConfig.MAX_OBSTACLES)
    val coins = GdxArray<Coin>(false, GameConfig.MAX_COINS)

    private val obstaclePool = object : Pool<Obstacle>(GameConfig.MAX_OBSTACLES) {
        override fun newObject() = Obstacle()
    }
    private val coinPool = object : Pool<Coin>(GameConfig.MAX_COINS) {
        override fun newObject() = Coin()
    }

    private var spawnTimer = GameConfig.SPAWN_INTERVAL_START
    private var revolutionDeg = 0f

    /** Recomputes track geometry from the shorter viewport dimension (world units). */
    fun setGeometry(shortDimension: Float) {
        innerRadius = shortDimension * GameConfig.INNER_LANE_RADIUS_FRAC
        outerRadius = shortDimension * GameConfig.OUTER_LANE_RADIUS_FRAC
        ballRadius = shortDimension * GameConfig.BALL_RADIUS_FRAC
        coinRadius = shortDimension * GameConfig.COIN_RADIUS_FRAC
    }

    fun laneRadius(lane: Int): Float = if (lane == LANE_INNER) innerRadius else outerRadius

    /** The ball's angular half-width (degrees) on its current lane, derived from its radius. */
    fun ballHalfWidthDeg(): Float = (ballRadius / laneRadius(ballLane)) * MathUtils.radiansToDegrees

    // --- Current solvability parameters, scaled with ball speed so the guarantee is a
    // --- constant reaction TIME (35 deg within 90 deg at 120 deg/s == 0.29s of gap).
    fun currentSolvabilityLookahead(): Float =
        kotlin.math.min(GameConfig.SOLVABILITY_LOOKAHEAD * Difficulty.solvabilityScale(score), 180f)

    fun currentSolvabilityMinGap(): Float =
        GameConfig.SOLVABILITY_MIN_GAP * Difficulty.solvabilityScale(score)

    /** Arc padding for double-block checks: widest ball half-width plus safety margin. */
    fun currentSolvabilityPad(): Float =
        (ballRadius / innerRadius) * MathUtils.radiansToDegrees + GameConfig.SOLVABILITY_PAD

    /** Tap input: instantly switch lanes. No-op unless running. */
    fun toggleLane() {
        if (state != State.RUNNING) return
        ballLane = if (ballLane == LANE_INNER) LANE_OUTER else LANE_INNER
    }

    /** Advances the world by one fixed step of [dt] seconds. Zero allocation. */
    fun step(dt: Float) {
        if (state != State.RUNNING) return

        // Ball motion + revolution scoring.
        val speed = Difficulty.ballSpeed(score)
        ballAngle = AngleMath.normalize(ballAngle + speed * dt)
        revolutionDeg += speed * dt
        while (revolutionDeg >= 360f) {
            revolutionDeg -= 360f
            score++
        }

        // Obstacles: rotate + expire.
        var i = 0
        while (i < obstacles.size) {
            val o = obstacles[i]
            o.angle = AngleMath.normalize(o.angle + o.rotationSpeed * dt)
            o.life -= dt
            if (o.life <= 0f) {
                obstacles.removeIndex(i)
                obstaclePool.free(o)
            } else {
                i++
            }
        }

        // Coins: expire + collect (circle-vs-circle in world space).
        val ballDist = laneRadius(ballLane)
        val bx = MathUtils.cosDeg(ballAngle) * ballDist
        val by = MathUtils.sinDeg(ballAngle) * ballDist
        i = 0
        while (i < coins.size) {
            val c = coins[i]
            c.life -= dt
            val cr = laneRadius(c.lane)
            val cx = MathUtils.cosDeg(c.angle) * cr
            val cy = MathUtils.sinDeg(c.angle) * cr
            val dx = cx - bx
            val dy = cy - by
            val reach = ballRadius + coinRadius
            when {
                dx * dx + dy * dy <= reach * reach -> { // collected
                    score++
                    coins.removeIndex(i)
                    coinPool.free(c)
                }
                c.life <= 0f -> {
                    coins.removeIndex(i)
                    coinPool.free(c)
                }
                else -> i++
            }
        }

        // Spawning.
        spawnTimer -= dt
        if (spawnTimer <= 0f) {
            spawnWave()
            spawnTimer = Difficulty.spawnInterval(score)
        }

        // Solvability guarantee is an invariant over every state, not just spawn time:
        // the ball's lookahead window sweeps onto track regions the spawn check never saw,
        // and rotating obstacles can close once-legal gaps. Enforce it every step.
        enforceSolvability()

        // Death check: angular overlap on the same lane.
        if (ballHitsObstacle()) state = State.GAME_OVER
    }

    /** Freezes are handled by state; call to start a fresh run. */
    fun reset() {
        state = State.RUNNING
        score = 0
        ballAngle = 0f
        ballLane = LANE_INNER
        revolutionDeg = 0f
        spawnTimer = GameConfig.SPAWN_INTERVAL_START
        obstaclePool.freeAll(obstacles)
        obstacles.clear()
        coinPool.freeAll(coins)
        coins.clear()
    }

    /**
     * Restores the solvability guarantee if ball travel / obstacle rotation violated it:
     * removes obstacles inside the lookahead window (least remaining life first — they'd
     * despawn soonest anyway) until at least one lane has the required gap again.
     */
    private fun enforceSolvability() {
        val lookahead = currentSolvabilityLookahead()
        val minGap = currentSolvabilityMinGap()
        val pad = currentSolvabilityPad()
        var guard = obstacles.size
        while (guard-- > 0 && !Solvability.isSolvable(ballAngle, obstacles, lookahead, minGap, pad)) {
            var victim = -1
            var victimLife = Float.MAX_VALUE
            for (i in 0 until obstacles.size) {
                val o = obstacles[i]
                if (o.life < victimLife &&
                    AngleMath.arcsOverlap(ballAngle, lookahead, o.angle - pad, o.arcLength + 2f * pad)
                ) {
                    victimLife = o.life
                    victim = i
                }
            }
            if (victim < 0) return // nothing in the window; cannot be the blocker
            obstaclePool.free(obstacles.removeIndex(victim))
        }
    }

    private fun ballHitsObstacle(): Boolean {
        val halfW = ballHalfWidthDeg()
        for (i in 0 until obstacles.size) {
            val o = obstacles[i]
            if (o.lane == ballLane &&
                AngleMath.arcsOverlap(o.angle, o.arcLength, ballAngle - halfW, halfW * 2f)
            ) return true
        }
        return false
    }

    private fun rand(min: Float, max: Float): Float = min + rng.nextFloat() * (max - min)

    private fun spawnWave() {
        trySpawnObstacle()
        if (coins.size < GameConfig.MAX_COINS && rng.nextFloat() < GameConfig.COIN_SPAWN_CHANCE) {
            trySpawnCoin()
        }
    }

    private fun trySpawnObstacle() {
        if (obstacles.size >= GameConfig.MAX_OBSTACLES) return
        for (attempt in 0 until GameConfig.SPAWN_ATTEMPTS) {
            val lane = rng.nextInt(2)
            val arcLen = rand(GameConfig.OBSTACLE_ARC_MIN, GameConfig.OBSTACLE_ARC_MAX)
            val angle = rand(0f, 360f)

            // Fairness: never materialize on top of (or right in front of) the ball —
            // on EITHER lane, since the player may toggle into the other lane at any instant.
            if (AngleMath.arcsOverlap(
                    angle, arcLen,
                    ballAngle - GameConfig.SAFE_SPAWN_BEHIND,
                    GameConfig.SAFE_SPAWN_BEHIND + GameConfig.SAFE_SPAWN_AHEAD
                )
            ) continue

            // Tentatively place, then enforce the solvability guarantee.
            val direction = if (rng.nextBoolean()) 1f else -1f
            val o = obstaclePool.obtain()
            o.set(
                lane, angle, arcLen,
                direction * rand(GameConfig.OBSTACLE_ROT_SPEED_MIN, GameConfig.OBSTACLE_ROT_SPEED_MAX),
                GameConfig.OBSTACLE_LIFETIME
            )
            obstacles.add(o)
            if (Solvability.isSolvable(
                    ballAngle, obstacles,
                    currentSolvabilityLookahead(), currentSolvabilityMinGap(), currentSolvabilityPad()
                )
            ) return // committed
            obstacles.removeValue(o, true)
            obstaclePool.free(o)
        }
        // All attempts violated the guarantee — skip this wave.
    }

    private fun trySpawnCoin() {
        for (attempt in 0 until GameConfig.SPAWN_ATTEMPTS) {
            val lane = rng.nextInt(2)
            val angle = rand(0f, 360f)
            val halfW = (coinRadius / laneRadius(lane)) * MathUtils.radiansToDegrees

            // Don't bury the coin inside an obstacle on the same lane (at spawn time).
            var blocked = false
            for (i in 0 until obstacles.size) {
                val o = obstacles[i]
                if (o.lane == lane && AngleMath.arcsOverlap(o.angle, o.arcLength, angle - halfW, halfW * 2f)) {
                    blocked = true
                    break
                }
            }
            if (blocked) continue

            val c = coinPool.obtain()
            c.set(lane, angle, GameConfig.COIN_LIFETIME)
            coins.add(c)
            return
        }
    }
}
