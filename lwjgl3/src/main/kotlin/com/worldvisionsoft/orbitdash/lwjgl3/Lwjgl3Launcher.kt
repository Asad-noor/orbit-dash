package com.worldvisionsoft.orbitdash.lwjgl3

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.worldvisionsoft.orbitdash.OrbitDashGame

/** Launches the desktop (LWJGL3) application. */
fun main() {
    // This handles macOS support and helps on Windows.
    if (StartupHelper.startNewJvmIfRequired()) return
    Lwjgl3Application(OrbitDashGame(), Lwjgl3ApplicationConfiguration().apply {
        setTitle("Orbit Dash")
        // Vsync limits FPS to the refresh rate of the monitor, saving resources.
        useVsync(true)
        // Cap FPS slightly above the display refresh rate in case vsync is disabled.
        setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1)
        setWindowedMode(720, 1280)
    })
}
