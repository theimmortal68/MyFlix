# Picture-in-Picture (PiP) Implementation Plan

This plan outlines the steps to implement Picture-in-Picture (PiP) support for the mobile application. This allows users to continue watching video while interacting with other apps or navigating the home screen.

## Phase 1: Picture-in-Picture (PiP) Support

### 1. Enable PiP in Manifest
Update `app-mobile/src/main/AndroidManifest.xml` to declare PiP support for `MainActivity`.

- Add `android:supportsPictureInPicture="true"` to the `<activity>` tag.
- Ensure `android:configChanges` includes `smallestScreenSize`, `screenLayout`, `screenSize`, and `orientation` to prevent activity restart on PiP entry.

### 2. Handle PiP Entry in MainActivity
Update `app-mobile/src/main/java/dev/jausc/myflix/mobile/MainActivity.kt` to handle the transition.

- Add a state property `var isPlayerActive` to track when the video player is on screen.
- Override `onUserLeaveHint()`:
  ```kotlin
  override fun onUserLeaveHint() {
      super.onUserLeaveHint()
      if (isPlayerActive) {
          // Calculate aspect ratio from current video if possible, or use default
          val aspectRatio = Rational(16, 9)
          val params = PictureInPictureParams.Builder()
              .setAspectRatio(aspectRatio)
              .build()
          enterPictureInPictureMode(params)
      }
  }
  ```
- Pass a callback `onPlayerActiveChange: (Boolean) -> Unit` down to `MyFlixMobileContent` and eventually to `PlayerScreen`.

### 3. Update PlayerScreen for PiP
Modify `app-mobile/src/main/java/dev/jausc/myflix/mobile/ui/screens/PlayerScreen.kt`.

- **State Management**:
  - Receive `onPlayerActiveChange` callback.
  - Call `onPlayerActiveChange(true)` in `LaunchedEffect` (on enter).
  - Call `onPlayerActiveChange(false)` in `DisposableEffect` (on exit).
  
- **UI Adaptation**:
  - Detect PiP mode using `LocalContext.current.findActivity().isInPictureInPictureMode` (needs a helper extension).
  - Observe `onPictureInPictureModeChanged` (via `DisposableEffect` with `addOnPictureInPictureModeChangedListener` if API 26+, or checking state changes).
  - **Hide Controls**: When in PiP mode, force `showControls = false` and disable gesture detection to prevent controls from appearing.
  
- **Lifecycle Handling**:
  - Important: The `DisposableEffect` that stops the player must **NOT** stop playback if the activity is changing configurations (entering PiP) or pausing but remaining visible.
  - Since PiP keeps the Activity in `onPause` but the View attached, the standard Compose `DisposableEffect` might be safe *unless* the NavHost destroys the screen. Standard PiP does not destroy the screen, so `onDispose` shouldn't trigger.
  - *Verify*: Ensure `playerController.release()` is not called when entering PiP.

### 4. Aspect Ratio Handling
- Update `MainActivity`'s `enterPictureInPictureMode` call to use the actual video aspect ratio.
- `PlayerScreen` can report the current aspect ratio up to `MainActivity` via the `onPlayerActiveChange` (e.g., passing a `Rational?` instead of `Boolean`).

## Phase 2: Background Audio Playback (Future)
*Note: This requires significant architectural changes and is outside the scope of Phase 1.*

To support background audio (screen off) or "background play", we need to migrate the player logic to a `MediaLibraryService` (Android Media3).
- **Service**: Create `PlaybackService` extending `MediaLibraryService`.
- **Session**: Manage a `MediaSession`.
- **Controller**: The UI (`PlayerScreen`) uses a `MediaController` to communicate with the Service instead of holding a `PlayerController` directly.
- **Notification**: The Service handles the persistent media notification.

For now, Phase 1 (PiP) provides the most immediate value with lower complexity.

## Implementation Steps (Phase 1)

1.  **Modify Manifest**: Add PiP support.
2.  **Refactor MainActivity**: Add `activePlayerRatio` state and `onUserLeaveHint` override.
3.  **Update Navigation**: Pass ratio callback to `PlayerScreen`.
4.  **Update PlayerScreen**: 
    - Report aspect ratio when video loads.
    - Handle PiP mode UI (hide controls).
    - Handle lifecycle to ensure playback continues.
