package com.worldvisionsoft.orbitdash.android

import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.worldvisionsoft.orbitdash.OrbitDashGame

/** Launches the Android application. */
class AndroidLauncher : AndroidApplication() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val configuration = AndroidApplicationConfiguration().apply {
            useImmersiveMode = true // Recommended, but not required.
        }
        initialize(OrbitDashGame(), configuration)
    }
}
