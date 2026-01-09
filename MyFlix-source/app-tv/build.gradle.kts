plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "dev.jausc.myflix.tv"
    compileSdk = rootProject.extra["compileSdk"] as Int

    defaultConfig {
        applicationId = "dev.jausc.myflix.tv"
        minSdk = rootProject.extra["minSdk"] as Int
        targetSdk = rootProject.extra["targetSdk"] as Int
        versionCode = 1
        versionName = "1.0.0"
        
        // Only include ARM ABIs for TV devices
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }
    
    // Split APKs by ABI - creates smaller per-device APKs
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a")
            isUniversalApk = true  // Also build a universal APK
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true  // Enable R8/ProGuard for release
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

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
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=androidx.tv.material3.ExperimentalTvMaterial3Api"
        )
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:network"))
    implementation(project(":core:player"))

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:${rootProject.extra["composeBomVersion"]}")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.foundation:foundation")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Compose TV
    val tvVersion = rootProject.extra["tvComposeVersion"] as String
    implementation("androidx.tv:tv-foundation:1.0.0-alpha12")
    implementation("androidx.tv:tv-material:$tvVersion")

    // Coil 3 for image loading
    implementation("io.coil-kt.coil3:coil-compose:${rootProject.extra["coilVersion"]}")
    implementation("io.coil-kt.coil3:coil-network-okhttp:${rootProject.extra["coilVersion"]}")
    
    // Material Icons Extended for outlined icons
    implementation("androidx.compose.material:material-icons-extended")
    
    // ZXing for QR code generation
    implementation("com.google.zxing:core:3.5.2")
    
    // Palette for extracting colors from images
    implementation("androidx.palette:palette-ktx:1.0.0")

    // Media3 ExoPlayer for video
    val media3Version = rootProject.extra["media3Version"] as String
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
}
