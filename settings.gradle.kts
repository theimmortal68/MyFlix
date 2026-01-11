pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Uncomment to enable MPV library from JitPack:
        // maven { url = uri("https://jitpack.io") }
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

// UI modules
include(":ui:common")
include(":ui:tv")
include(":ui:mobile")
