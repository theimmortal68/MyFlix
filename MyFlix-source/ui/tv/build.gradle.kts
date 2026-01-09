plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "dev.jausc.myflix.ui.tv"
    compileSdk = rootProject.extra["compileSdk"] as Int
    defaultConfig { minSdk = rootProject.extra["minSdk"] as Int }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures { compose = true }
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
    implementation(project(":ui:common"))
    val composeBom = platform("androidx.compose:compose-bom:${rootProject.extra["composeBomVersion"]}")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.runtime:runtime")
    val tvVersion = rootProject.extra["tvComposeVersion"] as String
    implementation("androidx.tv:tv-foundation:1.0.0-alpha12")
    implementation("androidx.tv:tv-material:$tvVersion")
    implementation("io.coil-kt.coil3:coil-compose:${rootProject.extra["coilVersion"]}")
    implementation("io.coil-kt.coil3:coil-network-okhttp:${rootProject.extra["coilVersion"]}")
}
