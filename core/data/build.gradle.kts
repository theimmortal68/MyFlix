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
    // Exposed to consumers
    api(project(":core:network"))

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.2")

    // Encrypted storage for sensitive credentials
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${rootProject.extra["coroutinesVersion"]}")

}
