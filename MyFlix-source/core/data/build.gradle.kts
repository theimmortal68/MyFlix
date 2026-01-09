plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "dev.jausc.myflix.core.data"
    compileSdk = rootProject.extra["compileSdk"] as Int
    defaultConfig { minSdk = rootProject.extra["minSdk"] as Int }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:network"))
    implementation("androidx.datastore:datastore-preferences:1.1.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
}
