plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "androidx.media3.decoder.av1"
    compileSdk = rootProject.extra["compileSdk"] as Int

    defaultConfig {
        minSdk = rootProject.extra["minSdk"] as Int

        ndk {
            // Build for ARM architectures (Shield TV, most Android TV devices)
            // Include x86_64 for emulator testing
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86_64"))
        }

        externalNativeBuild {
            cmake {
                // Release build for better performance
                arguments("-DCMAKE_BUILD_TYPE=Release")
                arguments("-DBUILD_TESTING=OFF")
                // libgav1: Use std::mutex instead of abseil (abseil not bundled)
                arguments("-DLIBGAV1_THREADPOOL_USE_STD_MUTEX=1")
                // libgav1: Disable examples and tests (not needed, avoids abseil dep)
                arguments("-DLIBGAV1_ENABLE_EXAMPLES=0")
                arguments("-DLIBGAV1_ENABLE_TESTS=0")
                targets("gav1JNI")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    // Configure native build with CMake
    externalNativeBuild {
        cmake {
            path = file("src/main/jni/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

dependencies {
    // Media3 decoder base
    val media3Version = rootProject.extra["media3Version"] as String
    api("androidx.media3:media3-decoder:$media3Version")
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.annotation:annotation:1.9.1")
}
