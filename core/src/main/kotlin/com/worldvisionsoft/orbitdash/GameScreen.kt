package com.worldvisionsoft.orbitdash

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ExtendViewport
import kotlin.math.min

/**
 * Owns the world (pure logic) and the renderer (drawing), the fixed-timestep loop,
 * input, and lifecycle. Logic advances in fixed 1/60s steps; render deltas are
 * accumulated and clamped so hitches and resume never cause a time jump.
 */
class GameScreen : ScreenAdapter() {

    private val world = GameWorld()
    private val renderer = GameRenderer()
    private val viewport = ExtendViewport(GameConfig.MIN_WORLD_WIDTH, GameConfig.MIN_WORLD_HEIGHT)

    private var accumulator = 0f
    private var paused = false

    private val input = object : InputAdapter() {
        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            onTap()
            return true
        }

        // Desktop convenience: spacebar acts like a tap (mouse click already is touchDown).
        override fun keyDown(keycode: Int): Boolean {
            if (keycode == Input.Keys.SPACE) {
                onTap()
                return true
            }
            return false
        }
    }

    private fun onTap() {
        if (world.state == GameWorld.State.GAME_OVER) {
            world.reset()
            accumulator = 0f
        } else {
            world.toggleLane()
        }
    }

    override fun show() {
        Gdx.input.inputProcessor = input
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
        world.setGeometry(min(viewport.worldWidth, viewport.worldHeight))
    }

    override fun render(delta: Float) {
        if (!paused) {
            // Fixed timestep with clamped accumulation (no spiral of death, no resume jump).
            accumulator += min(delta, GameConfig.MAX_FRAME_DELTA)
            while (accumulator >= GameConfig.FIXED_DT) {
                world.step(GameConfig.FIXED_DT)
                accumulator -= GameConfig.FIXED_DT
            }
        }

        ScreenUtils.clear(0.06f, 0.07f, 0.11f, 1f)
        viewport.apply(true)
        renderer.render(world, viewport)
    }

    override fun pause() {
        // Android onPause: stop accumulating logic time entirely.
        paused = true
    }

    override fun resume() {
        // Discard any partial step so the first post-resume frame starts clean.
        accumulator = 0f
        paused = false
    }

    override fun hide() {
        if (Gdx.input.inputProcessor === input) Gdx.input.inputProcessor = null
    }

    override fun dispose() {
        renderer.dispose()
    }
}
