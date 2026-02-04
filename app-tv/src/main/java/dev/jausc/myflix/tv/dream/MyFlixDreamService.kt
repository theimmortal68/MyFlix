package dev.jausc.myflix.tv.dream

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelProvider
import dev.jausc.myflix.core.common.ui.theme.ThemePreset
import dev.jausc.myflix.tv.TvPreferences
import dev.jausc.myflix.tv.ui.theme.MyFlixTvTheme

/**
 * MyFlix Dream Service (screensaver) for Android TV.
 *
 * Displays rotating media artwork from the user's Jellyfin library
 * with smooth crossfade transitions and Ken Burns zoom effects.
 *
 * To test:
 * 1. Install the app on an Android TV device
 * 2. Go to Settings → Screen saver → Select "MyFlix Screensaver"
 * 3. Wait for the screensaver to activate, or run:
 *    adb shell am start -n "com.android.systemui/.Somnambulator"
 */
class MyFlixDreamService : DreamServiceCompat() {
    private lateinit var viewModel: DreamViewModel

    override fun onDreamingStarted() {
        super.onDreamingStarted()

        // Configure dream service behavior
        isInteractive = false
        isFullscreen = true
        isScreenBright = false

        // Create ViewModel
        viewModel = ViewModelProvider(
            this,
            DreamViewModel.Factory(applicationContext),
        )[DreamViewModel::class.java]

        // Set Compose content
        setContent {
            // Collect theme preference
            val tvPreferences = remember { TvPreferences.getInstance(applicationContext) }
            val themePresetName by tvPreferences.themePreset.collectAsState()
            val themePreset = remember(themePresetName) {
                ThemePreset.fromName(themePresetName)
            }

            MyFlixTvTheme(themePreset = themePreset) {
                DreamScreen(viewModel = viewModel)
            }
        }
    }
}
