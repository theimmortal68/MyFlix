pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    // Enable automatic JDK download for toolchains (needed for NewPipeExtractor which requires JDK 11)
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

// Temporarily disabled - v0.25.0 requires PoTokenProvider for YouTube extraction
// Use v0.24.8 from Maven instead (matching Wholphin's working implementation)
// includeBuild("NewPipeExtractor") {
//     dependencySubstitution {
//         substitute(module("com.github.TeamNewPipe:NewPipeExtractor"))
//             .using(project(":extractor"))
//     }
// }

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
