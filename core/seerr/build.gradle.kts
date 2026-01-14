plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "dev.jausc.myflix.core.seerr"
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
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:${rootProject.extra["coroutinesVersion"]}")
    api("org.jetbrains.kotlinx:kotlinx-serialization-core:${rootProject.extra["serializationVersion"]}")

    // Ktor
    val ktorVersion = rootProject.extra["ktorVersion"] as String
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-http:$ktorVersion")
    implementation("io.ktor:ktor-serialization:$ktorVersion")
    implementation("io.ktor:ktor-utils:$ktorVersion")

    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:${rootProject.extra["okHttpVersion"]}")

    // Serialization JSON
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${rootProject.extra["serializationVersion"]}")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testImplementation("io.ktor:ktor-http:$ktorVersion")
    testImplementation("io.ktor:ktor-serialization:$ktorVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${rootProject.extra["coroutinesVersion"]}")
    testImplementation("org.jetbrains.kotlin:kotlin-test:${rootProject.extra["kotlinVersion"]}")
    testImplementation("io.mockk:mockk:1.13.10")
}
