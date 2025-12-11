# Call Notifications Setup

## Overview

The app now includes system-style call notifications with Accept/Reject actions. When someone calls you, you'll receive a notification that looks like a system phone call notification.

## Features

✅ **System-style call notification** with full-screen intent  
✅ **Accept/Reject action buttons** in the notification  
✅ **Automatic notification** when incoming call is detected  
✅ **Foreground service** to keep notification active  
✅ **Firebase integration** to listen for incoming calls  

## How It Works

### 1. Outgoing Call Flow

1. User A taps "Call" on a contact
2. `MainActivity.startCall()` creates a call document in Firestore with status "ringing"
3. `VideoCallActivity` opens for User A (outgoing call UI)

### 2. Incoming Call Flow

1. **Firestore Listener** in `MainActivity.listenForIncomingCalls()` detects new call with:
   - `receiverId` = current user
   - `status` = "ringing"

2. **CallService** is started as a foreground service:
   ```kotlin
   startForegroundService(serviceIntent)
   ```

3. **CallNotificationManager** creates and shows the notification:
   - Full-screen intent (opens VideoCallActivity when tapped)
   - Accept button → Opens VideoCallActivity and updates status to "accepted"
   - Reject button → Updates status to "rejected" and dismisses notification

4. **User can:**
   - Tap notification → Opens full-screen call UI
   - Tap "Accept" → Accepts call and opens VideoCallActivity
   - Tap "Reject" → Rejects call and dismisses notification

### 3. Call End Flow

When call ends:
- Notification is cancelled
- CallService is stopped
- Agora engine is cleaned up

## Components

### CallNotificationManager
- Creates notification channel
- Shows incoming call notification
- Handles notification actions
- Cancels notification when call ends

### CallService
- Foreground service to keep notification active
- Receives incoming call intents
- Manages notification lifecycle

### CallActionReceiver
- BroadcastReceiver for Accept/Reject actions
- Updates Firestore call status
- Opens VideoCallActivity on accept

### MainActivity
- Listens for incoming calls via Firestore
- Starts CallService when incoming call detected
- Gets caller information from Firestore

## Permissions

The following permissions are required:

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

**Note:** For Android 13+ (API 33+), you need to request `POST_NOTIFICATIONS` permission at runtime.

## Android 13+ Notification Permission

If targeting Android 13+, add this to `MainActivity.onCreate()`:

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
        != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            NOTIFICATION_PERMISSION_REQUEST_CODE
        )
    }
}
```

## Testing

### Test Incoming Call Notification:

1. **User A** (Device 1):
   - Open app
   - Tap "Call" on a contact
   - Call document is created in Firestore

2. **User B** (Device 2):
   - Should receive notification immediately
   - Notification shows: "Incoming Video Call - [Caller Name] is calling..."
   - Has Accept and Reject buttons

3. **Test Accept:**
   - Tap "Accept" button
   - VideoCallActivity opens
   - Call status updates to "accepted"

4. **Test Reject:**
   - Tap "Reject" button
   - Notification dismisses
   - Call status updates to "rejected"

5. **Test Full-Screen:**
   - Tap the notification itself
   - VideoCallActivity opens in full-screen mode

## Notification Appearance

The notification includes:
- **Title:** "Incoming Video Call"
- **Text:** "[Caller Name] is calling..."
- **Icon:** System call icon
- **Actions:**
  - Accept (green/system call icon)
  - Reject (red/close icon)
- **Vibration:** Pattern (0, 250, 250, 250ms)
- **Priority:** High
- **Category:** Call
- **Full-screen intent:** Opens VideoCallActivity

## Troubleshooting

### Notification Not Showing

1. **Check permissions:**
   - Android 13+: Request POST_NOTIFICATIONS permission
   - Check notification channel is created

2. **Check Firestore:**
   - Verify call document is created with correct `receiverId`
   - Check `status` is set to "ringing"

3. **Check Logcat:**
   - Filter by "CallService" or "CallNotificationManager"
   - Look for errors

### Notification Shows But Actions Don't Work

1. **Check BroadcastReceiver:**
   - Verify `CallActionReceiver` is registered in manifest
   - Check intent actions match

2. **Check PendingIntent flags:**
   - Ensure `FLAG_IMMUTABLE` is set (Android 12+)

### Full-Screen Intent Not Working

1. **Check activity flags:**
   - Verify `FLAG_ACTIVITY_NEW_TASK` is set
   - Check activity is exported correctly

## Future Enhancements

- [ ] Add caller photo/avatar to notification
- [ ] Add ringtone customization
- [ ] Add call history
- [ ] Add missed call notifications
- [ ] Add notification sound customization

