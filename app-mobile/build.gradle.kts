plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "dev.jausc.myflix.mobile"
    compileSdk = rootProject.extra["compileSdk"] as Int

    defaultConfig {
        applicationId = "dev.jausc.myflix.mobile"
        minSdk = rootProject.extra["minSdk"] as Int
        targetSdk = rootProject.extra["targetSdk"] as Int
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:${rootProject.extra["desugarVersion"]}")

    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:network"))
    implementation(project(":core:player"))
    implementation(project(":core:seerr"))

    val composeBom = platform("androidx.compose:compose-bom:${rootProject.extra["composeBomVersion"]}")
    implementation(composeBom)

    // Core Android
    implementation("androidx.activity:activity:1.10.1")
    implementation("androidx.activity:activity-compose:1.10.1")

    // Compose UI
    implementation("androidx.compose.animation:animation-core")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.foundation:foundation-layout")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-geometry")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-text")
    implementation("androidx.compose.ui:ui-unit")

    // Navigation
    implementation("androidx.navigation:navigation-common:${rootProject.extra["navigationVersion"]}")
    implementation("androidx.navigation:navigation-runtime:${rootProject.extra["navigationVersion"]}")
    implementation("androidx.navigation:navigation-compose:${rootProject.extra["navigationVersion"]}")

    // Coil 3 for image loading
    implementation("io.coil-kt.coil3:coil-compose:${rootProject.extra["coilVersion"]}")
    implementation("io.coil-kt.coil3:coil-network-okhttp:${rootProject.extra["coilVersion"]}")

    // Media3 ExoPlayer
    val media3Version = rootProject.extra["media3Version"] as String
    implementation("androidx.media3:media3-common:$media3Version")
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-session:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${rootProject.extra["coroutinesVersion"]}")

    // Guava (used by Media3)
    implementation("com.google.guava:guava:33.3.1-android")

    debugImplementation("androidx.compose.ui:ui-tooling")

    // Lifecycle for ViewModel and collectAsStateWithLifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.1")
}
