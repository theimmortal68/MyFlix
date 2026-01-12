plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "dev.jausc.myflix.core.player"
    compileSdk = rootProject.extra["compileSdk"] as Int

    defaultConfig {
        minSdk = rootProject.extra["minSdk"] as Int

        ndk {
            // TV devices are arm64, some older ones are armv7
            // Include x86/x86_64 for emulator testing
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
        }
    }

    // No CMake needed - using prebuilt libplayer.so from mpv-android

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // AndroidX annotations for @RequiresApi
    implementation("androidx.annotation:annotation:1.9.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    runtimeOnly("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")

    // ExoPlayer (Media3) - exposed to consumers
    val media3Version = rootProject.extra["media3Version"] as String
    api("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-common:$media3Version")

    // Jellyfin's FFmpeg extension for DTS/DTS-HD/DTS:X/TrueHD audio decoding
    // Version must match Media3 version
    implementation("org.jellyfin.media3:media3-ffmpeg-decoder:1.9.0+1")
}
