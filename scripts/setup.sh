#!/bin/bash
# Setup script for MyFlix development environment
# Run this after cloning the repository

set -e

echo "=== MyFlix Development Setup ==="

# Initialize and update submodules
echo "Initializing submodules..."
git submodule update --init --recursive

# Patch NewPipeExtractor to enable automatic JDK downloads
SETTINGS_FILE="NewPipeExtractor/settings.gradle.kts"
if ! grep -q "foojay-resolver" "$SETTINGS_FILE" 2>/dev/null; then
    echo "Patching NewPipeExtractor for JDK toolchain downloads..."

    # Create the patched content
    cat > "$SETTINGS_FILE" << 'EOF'
/*
 * SPDX-FileCopyrightText: 2025 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
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
        maven(url = "https://jitpack.io")
    }
}
include("extractor", "timeago-generator")
rootProject.name = "NewPipeExtractor"
EOF

    echo "Patch applied successfully"
else
    echo "NewPipeExtractor already patched"
fi

echo ""
echo "=== Setup Complete ==="
echo "You can now build with:"
echo "  ./gradlew :app-tv:assembleDebug"
echo "  ./gradlew :app-mobile:assembleDebug"
