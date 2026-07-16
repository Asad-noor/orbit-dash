package com.worldvisionsoft.orbitdash

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.utils.ScreenUtils

/**
 * Shared game entry point across all platforms.
 * Platform launchers (lwjgl3, android) instantiate this class.
 */
class OrbitDashGame : ApplicationAdapter() {

    override fun render() {
        ScreenUtils.clear(0.08f, 0.09f, 0.13f, 1f)
    }
}
