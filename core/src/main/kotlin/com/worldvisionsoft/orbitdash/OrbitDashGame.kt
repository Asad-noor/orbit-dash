package com.worldvisionsoft.orbitdash

import com.badlogic.gdx.Game

/**
 * Shared game entry point across all platforms.
 * Platform launchers (lwjgl3, android) instantiate this class.
 */
class OrbitDashGame : Game() {

    override fun create() {
        setScreen(GameScreen())
    }

    override fun dispose() {
        super.dispose() // hides the current screen
        screen?.dispose()
    }
}
