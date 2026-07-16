# Orbit Dash

## Project Overview

Orbit Dash is a mobile-first 2D arcade game built with libGDX 1.14.2 and Kotlin, targeting Android (Google Play) with a desktop LWJGL3 launcher used for fast development iteration. The project is a Gradle multi-module build in the layout produced by gdx-liftoff: shared game code lives in `core`, and thin platform launchers live in `android` and `lwjgl3`. Package root is `com.worldvisionsoft.orbitdash`. The gdx-freetype extension is included for runtime font rasterization; Box2D, Ashley, and AI extensions are intentionally excluded.

## Module Responsibilities

| Module   | Responsibility |
|----------|----------------|
| `core`   | All game logic and rendering. Pure JVM/libGDX code — **no Android imports**. |
| `android`| Android launcher only: activity lifecycle, ads, platform services. minSdk 24, targetSdk 36, arm64-v8a + armeabi-v7a, App Bundle-ready. |
| `lwjgl3` | Desktop dev launcher (LWJGL3). Used for quick local testing; not a shipping target. |

Shared assets live in the root `assets/` directory, wired into both launchers.

## Build / Run Commands

```bash
./gradlew lwjgl3:run              # run the game on desktop (dev loop)
./gradlew android:assembleDebug   # build debug APK
./gradlew android:bundleRelease   # build release App Bundle (AAB) for Play
./gradlew build                   # compile + test everything
```

Desktop run on macOS is handled automatically (`-XstartOnFirstThread` via StartupHelper + run task jvmArgs).

## Standing Rules

- core module must never import android.* classes
- all platform services go through interfaces in core
- fixed timestep 1/60s for game logic
- commit at the end of each phase

## Versions

- libGDX 1.14.2, Kotlin 2.2.21, Gradle 8.14.3 (wrapper), AGP 8.13.0
- Java 11 bytecode target across modules; JDK 17+ required to build
