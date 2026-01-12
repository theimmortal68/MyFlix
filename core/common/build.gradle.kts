plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "dev.jausc.myflix.core.common"
    compileSdk = rootProject.extra["compileSdk"] as Int
    defaultConfig { minSdk = rootProject.extra["minSdk"] as Int }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))

    // Core modules
    api(project(":core:seerr"))

    // Coroutines - exposed to consumers for StateFlow in preferences
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")

    // Serialization - exposed to consumers
    api("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.0")

    // DataStore - for preference key definitions
    api("androidx.datastore:datastore-preferences:1.1.6")

    // Compose UI - exposed to consumers
    api("androidx.compose.runtime:runtime")
    api("androidx.compose.ui:ui-graphics")
    api("androidx.compose.ui:ui-text")
    api("androidx.compose.ui:ui-unit")

    // Compose UI - internal use (for MyFlixLogo)
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.foundation:foundation-layout")
    implementation("androidx.compose.ui:ui")

    // Material Icons - for ActionMenuBuilder
    api("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test:${rootProject.extra["kotlinVersion"]}")
    testImplementation("io.mockk:mockk:1.13.10")
}
