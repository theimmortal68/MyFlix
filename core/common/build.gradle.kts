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
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // Core library desugaring
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:${rootProject.extra["desugarVersion"]}")

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:${rootProject.extra["composeBomVersion"]}"))

    // Core modules
    api(project(":core:seerr"))

    // Coroutines - exposed to consumers for StateFlow in preferences
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:${rootProject.extra["coroutinesVersion"]}")

    // Serialization - exposed to consumers
    api("org.jetbrains.kotlinx:kotlinx-serialization-core:${rootProject.extra["serializationVersion"]}")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:${rootProject.extra["serializationVersion"]}")

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

    // Coil - for SubsetTransformation
    api("io.coil-kt.coil3:coil-core:${rootProject.extra["coilVersion"]}")

    // Material Icons - for ActionMenuBuilder
    api("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")

    // HTTP client for trailer stream service
    implementation("com.squareup.okhttp3:okhttp:${rootProject.extra["okHttpVersion"]}")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${rootProject.extra["coroutinesVersion"]}")
    testImplementation("org.jetbrains.kotlin:kotlin-test:${rootProject.extra["kotlinVersion"]}")
    testImplementation("io.mockk:mockk:1.13.10")
}
