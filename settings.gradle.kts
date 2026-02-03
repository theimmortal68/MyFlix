pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    // Enable automatic JDK download for toolchains
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "MyFlix"

// App modules
include(":app-tv")
include(":app-mobile")

// Core modules
include(":core:common")
include(":core:network")
include(":core:data")
include(":core:player")
include(":core:seerr")
include(":core:viewmodel")

// UI modules
include(":ui:common")
include(":ui:tv")
include(":ui:mobile")
