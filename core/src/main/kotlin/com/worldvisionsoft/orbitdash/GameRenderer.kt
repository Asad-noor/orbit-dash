package com.worldvisionsoft.orbitdash

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.viewport.Viewport
import kotlin.math.max

/**
 * Draws the world with ShapeRenderer only (placeholder art) plus BitmapFont text for the HUD.
 * Owns all GL resources; GameWorld stays render-free.
 */
class GameRenderer : Disposable {

    private val shapes = ShapeRenderer()
    private val batch = SpriteBatch()
    private val font = BitmapFont().apply {
        data.setScale(2f)
        setUseIntegerPositions(false)
    }

    // Text layouts are cached; the score layout is only rebuilt when the score changes,
    // so the steady-state render path allocates nothing.
    private val scoreLayout = GlyphLayout()
    private var lastScore = -1
    private val gameOverLayout = GlyphLayout(font, "GAME OVER - tap to restart")

    private val laneColor = Color(0.28f, 0.30f, 0.40f, 1f)
    private val ballColor = Color(0.40f, 0.85f, 1f, 1f)
    private val obstacleColor = Color(1f, 0.42f, 0.30f, 1f)
    private val coinColor = Color(1f, 0.84f, 0.25f, 1f)
    private val textColor = Color(0.92f, 0.94f, 1f, 1f)

    fun render(world: GameWorld, viewport: Viewport) {
        val cx = viewport.worldWidth / 2f
        val cy = viewport.worldHeight / 2f
        val laneThickness = world.outerRadius - world.innerRadius

        shapes.projectionMatrix = viewport.camera.combined

        // Lane guides.
        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.color = laneColor
        shapes.circle(cx, cy, world.innerRadius, 72)
        shapes.circle(cx, cy, world.outerRadius, 72)
        shapes.end()

        shapes.begin(ShapeRenderer.ShapeType.Filled)

        // Obstacles: thick arc segments on their lane.
        shapes.color = obstacleColor
        for (i in 0 until world.obstacles.size) {
            val o = world.obstacles[i]
            drawArc(cx, cy, world.laneRadius(o.lane), o.angle, o.arcLength, laneThickness)
        }

        // Coins.
        shapes.color = coinColor
        for (i in 0 until world.coins.size) {
            val c = world.coins[i]
            val r = world.laneRadius(c.lane)
            shapes.circle(
                cx + MathUtils.cosDeg(c.angle) * r,
                cy + MathUtils.sinDeg(c.angle) * r,
                world.coinRadius, 16
            )
        }

        // Ball.
        shapes.color = ballColor
        val br = world.laneRadius(world.ballLane)
        shapes.circle(
            cx + MathUtils.cosDeg(world.ballAngle) * br,
            cy + MathUtils.sinDeg(world.ballAngle) * br,
            world.ballRadius, 24
        )

        shapes.end()

        // HUD text.
        if (world.score != lastScore) {
            lastScore = world.score
            scoreLayout.setText(font, lastScore.toString())
        }
        batch.projectionMatrix = viewport.camera.combined
        batch.begin()
        font.color = textColor
        font.draw(
            batch, scoreLayout,
            cx - scoreLayout.width / 2f,
            viewport.worldHeight - GameConfig.SCORE_TOP_MARGIN
        )
        if (world.state == GameWorld.State.GAME_OVER) {
            font.draw(
                batch, gameOverLayout,
                cx - gameOverLayout.width / 2f,
                cy + gameOverLayout.height / 2f
            )
        }
        batch.end()
    }

    /** Draws a ring arc as short thick segments (ShapeRenderer has no ring-arc primitive). */
    private fun drawArc(cx: Float, cy: Float, radius: Float, startDeg: Float, lengthDeg: Float, thickness: Float) {
        val segments = max(2, (lengthDeg / 5f).toInt())
        val step = lengthDeg / segments
        var px = cx + MathUtils.cosDeg(startDeg) * radius
        var py = cy + MathUtils.sinDeg(startDeg) * radius
        for (s in 1..segments) {
            val a = startDeg + step * s
            val nx = cx + MathUtils.cosDeg(a) * radius
            val ny = cy + MathUtils.sinDeg(a) * radius
            shapes.rectLine(px, py, nx, ny, thickness)
            px = nx
            py = ny
        }
    }

    override fun dispose() {
        shapes.dispose()
        batch.dispose()
        font.dispose()
    }
}
