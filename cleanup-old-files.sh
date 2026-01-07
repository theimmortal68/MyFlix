#!/bin/bash
# cleanup-old-files.sh
# Run this script in your MyFlix project root BEFORE unzipping the new version
# to remove old files that are no longer needed.

echo "Cleaning up old MyFlix files..."

# TV app old files
rm -f app-tv/src/main/java/dev/jausc/myflix/tv/MyFlixApp.kt
rm -f app-tv/src/main/java/dev/jausc/myflix/tv/Navigation.kt
rm -f app-tv/src/main/java/dev/jausc/myflix/tv/ui/components/MediaRow.kt

# Mobile app old files
rm -f app-mobile/src/main/java/dev/jausc/myflix/mobile/Navigation.kt

echo "Cleanup complete! You can now unzip the new version."
