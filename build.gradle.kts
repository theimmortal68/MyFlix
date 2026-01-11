plugins {
    id("com.android.application") version "8.13.2" apply false
    id("com.android.library") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.3.0" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.0" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.7"
}

// Configure Detekt
detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$rootDir/detekt.yml")
    source.setFrom(
        "app-tv/src/main/java",
        "app-mobile/src/main/java",
        "core/common/src/main/java",
        "core/network/src/main/java",
        "core/data/src/main/java",
        "core/player/src/main/java",
        "core/seerr/src/main/java",
    )
}

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.7")
    detektPlugins("io.nlopez.compose.rules:detekt:0.4.22")
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
