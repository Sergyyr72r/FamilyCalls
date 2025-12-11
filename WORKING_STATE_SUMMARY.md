# Working State Summary - Family Calls App
Date: December 11, 2025
Status: **Functional**

## Key Features Verified
1.  **User Authentication**: Login/Register flow works without redirect loops.
2.  **Incoming Call Detection**: Firestore listener correctly triggers on "ringing" status.
3.  **Notifications**:
    -   Appears correctly on Android 14+ (Samsung).
    -   Works when app is in background (but active).
    -   Handles "Simulate Call" and real calls.
4.  **Permissions**:
    -   Notifications permission requested correctly.
    -   Foreground service permissions handled.

## Critical Configurations (Do Not Change Without Backup)

### 1. `AndroidManifest.xml`
-   **Service Type**: `CallService` is set to `foregroundServiceType="shortService"` to avoid Android 14 background start crashes (`SecurityException`).
-   **Permissions**: Includes `POST_NOTIFICATIONS`, `USE_FULL_SCREEN_INTENT`, `FOREGROUND_SERVICE`.

### 2. `MainActivity.kt`
-   **Listener Logic**:
    -   Checks for empty `userId` and falls back to Firebase Auth.
    -   Validates `callerId` before querying Firestore to avoid `IllegalArgumentException`.
    -   Uses `CallNotificationManager(applicationContext)` to ensure notifications work even if the Activity context is unstable.
    -   Catches `IllegalArgumentException` in the listener loop.
-   **Lifecycle**: Defer notification settings check with `binding.root.post` to prevent `onCreate` crashes.

### 3. `AuthActivity.kt`
-   **Redirect Fix**: Saves `userId` to `SharedPreferences` *before* redirecting to `MainActivity` to prevent an infinite login loop.

### 4. `CallNotificationManager.kt`
-   **Channel ID**: `family_calls_channel_v3` (High Importance).
-   **Context**: Uses `applicationContext` where possible.

## Known Constraints
-   **App Kill State**: If the app is force-killed by the system (swipe away or battery optimizer), the Firestore listener *will stop*. Use "Lock App" in Recent Apps on Samsung devices to keep it alive. Reliable "killed state" notifications require Firebase Cloud Messaging (FCM).

## How to Rollback
If future changes break the app, revert files to the state matching this timestamp. Ensure `callerId` validation and `shortService` type remain in place.

