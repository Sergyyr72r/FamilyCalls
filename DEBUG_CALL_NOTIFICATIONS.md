# Debugging Call Notifications

## Issue: No Notification Showing

If notifications aren't showing when someone calls you, follow these steps:

## Step 1: Check Notification Permissions

### Android 13+ (API 33+):
1. Go to **Settings** → **Apps** → **Family Calls**
2. Tap **Notifications**
3. Ensure notifications are **enabled**
4. Check that **"Family Calls"** channel is enabled

### All Android Versions:
1. Go to **Settings** → **Apps** → **Family Calls**
2. Tap **Notifications**
3. Ensure notifications are **enabled**

## Step 2: Check Logcat for Listener Activity

When someone calls you, check Logcat (filter by "MainActivity" or "CallService"):

**Expected logs:**
```
MainActivity: Setting up incoming call listener for userId: [your_user_id]
MainActivity: Call listener triggered. Documents: 1
MainActivity: Found call: id=[call_id], callerId=[caller_id], status=ringing
MainActivity: Processing incoming call from: [caller_id]
MainActivity: Caller info: name=[name], phone=[phone]
MainActivity: CallService started successfully
CallService: Service created
CallService: Showing incoming call notification: callerId=[id], callerName=[name]
CallNotificationManager: showIncomingCallNotification called: callerId=[id], callerName=[name]
CallNotificationManager: Posting notification with ID: 1001
CallNotificationManager: Notification posted successfully
```

**If you don't see these logs:**
- The listener isn't being triggered
- The call document might not be created correctly
- The app might be in the background (listener only works when MainActivity is active)

## Step 3: Verify Call Document is Created

1. **Open Firebase Console**: https://console.firebase.google.com/
2. Go to **Firestore Database**
3. Check the **"calls"** collection
4. When someone calls you, you should see a document with:
   - `callerId`: The caller's user ID
   - `receiverId`: Your user ID
   - `status`: "ringing"
   - `timestamp`: Current timestamp

**If the document doesn't exist:**
- The call isn't being created
- Check `VideoCallActivity.createCallDocument()` logs

## Step 4: Test the Listener Manually

Add this test button temporarily in `MainActivity`:

```kotlin
// In onCreate(), add:
binding.btnTestCall.setOnClickListener {
    // Simulate incoming call
    val serviceIntent = Intent(this, CallService::class.java).apply {
        action = CallService.ACTION_SHOW_INCOMING_CALL
        putExtra("callerId", "test_caller_id")
        putExtra("callerName", "Test Caller")
        putExtra("callerPhone", "1234567890")
    }
    startForegroundService(serviceIntent)
}
```

If this shows a notification, the issue is with the Firestore listener.

## Step 5: Check if App is in Background

**Important:** The Firestore listener in `MainActivity` only works when:
- MainActivity is in the foreground
- The app is not killed by the system

**If the app is in the background:**
- The listener won't work
- You won't receive notifications
- You need to use Firebase Cloud Messaging (FCM) for background notifications

## Step 6: Verify Notification Channel

Check if the notification channel exists:

```kotlin
// Add this to MainActivity.onCreate() temporarily
val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    val channel = notificationManager.getNotificationChannel("family_calls_channel")
    Log.d("MainActivity", "Notification channel: ${channel?.name}, importance: ${channel?.importance}")
}
```

## Common Issues

### Issue 1: "Suppressing notification by user request"
**Solution:** Enable notifications in app settings (see Step 1)

### Issue 2: Listener not triggering
**Possible causes:**
- App is in background
- Firestore listener not set up
- `currentUserId` is empty
- Network connection issues

**Solution:**
- Keep app in foreground to test
- Check Logcat for listener setup logs
- Verify `currentUserId` is set correctly

### Issue 3: Notification shows but actions don't work
**Solution:**
- Check `CallActionReceiver` is registered in manifest
- Verify intent actions match

## Next Steps

1. **Check Logcat** when someone calls you
2. **Verify call document** is created in Firestore
3. **Test notification** manually using test button
4. **Check notification settings** in Android Settings

If notifications still don't work, we may need to implement Firebase Cloud Messaging (FCM) for background notifications.

