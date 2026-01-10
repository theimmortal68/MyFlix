plugins {
    id("com.android.application") version "8.13.2" apply false
    id("com.android.library") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.3.0" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.0" apply false
}

// Shared versions
buildscript {
    extra.apply {
        set("minSdk", 25)
        set("targetSdk", 36)
        set("compileSdk", 36)
        set("kotlinVersion", "2.3.0")
        set("composeBomVersion", "2025.12.01")
        set("tvComposeVersion", "1.1.0-alpha01")
        set("ktorVersion", "3.3.0")
        set("coilVersion", "3.3.0")
        set("media3Version", "1.9.0")
    }
}
