# Fix: Background Message Notifications

## Problem
Message notifications don't work when the app is closed/backgrounded.

## Solution
I've updated the code to use **data-only FCM messages** so notifications work reliably even when the app is closed.

## Critical: Deploy Cloud Functions

**The Cloud Function MUST be deployed for this to work!**

1. **Open terminal in the project root:**
   ```bash
   cd /Users/sergeyromanov/Desktop/FamilyCalls
   ```

2. **Navigate to functions directory:**
   ```bash
   cd functions
   ```

3. **Deploy the functions:**
   ```bash
   firebase deploy --only functions
   ```

4. **Wait for deployment to complete** - You should see:
   ```
   ✔  functions[sendMessageNotification] Successful create operation.
   ```

## Verify Deployment

After deploying, check Firebase Console:
1. Go to Firebase Console → Functions
2. You should see `sendMessageNotification` function listed
3. Check logs to see if it's being triggered when messages are sent

## Testing Steps

### 1. Verify FCM Tokens are Registered

**On both devices:**
1. Open the app
2. Check Logcat for: `"FCM Token registered successfully for user: ..."`
3. If you don't see this, notifications won't work!

### 2. Verify FCM Token in Firestore

1. Go to Firebase Console → Firestore
2. Open the `users` collection
3. Check that each user document has an `fcmToken` field with a value
4. If missing, the app needs to register the token

### 3. Test Notification Flow

**Device A (Sender):**
1. Open app and send a message to Device B

**Device B (Receiver):**
1. **Completely close the app** (swipe away from recent apps)
2. Wait for the message
3. You should receive a notification

### 4. Check Logcat (if app was recently closed)

Even when closed, Android may keep the service alive briefly. Check Logcat for:
```
D/MyFirebaseMsgService: From: ...
D/MyFirebaseMsgService: Message data payload: {...}
D/MyFirebaseMsgService: Received message FCM from: ... - ...
D/MyFirebaseMsgService: Message notification shown from FCM
```

### 5. Check Cloud Function Logs

1. Firebase Console → Functions → `sendMessageNotification` → Logs
2. Look for:
   - `Successfully sent message notification for ...`
   - Any error messages

## Troubleshooting

### No notifications appear

**Check 1: Is the Cloud Function deployed?**
- Go to Firebase Console → Functions
- Is `sendMessageNotification` listed? If not, deploy it!

**Check 2: Are FCM tokens registered?**
- Check Firestore `users` collection
- Each user document should have `fcmToken` field
- If missing, reinstall the app and check Logcat for token registration

**Check 3: Check notification permissions**
- Android 13+: Settings → Apps → Family Calls → Notifications → Enable
- Check that "Messages" channel is enabled

**Check 4: Check Cloud Function logs**
- Firebase Console → Functions → Logs
- Look for errors when a message is sent

**Check 5: Battery optimization**
- Some devices kill apps aggressively
- Settings → Apps → Family Calls → Battery → Unrestricted (if available)

### Notifications work in foreground but not when closed

This means:
- ✅ FCM tokens are registered
- ✅ Cloud Function is deployed
- ❌ But notifications aren't being delivered when app is closed

**Possible causes:**
1. Device is in Doze mode (Android battery saver)
2. Battery optimization is killing the app
3. FCM message is being throttled

**Solutions:**
- Disable battery optimization for the app
- Check if Doze mode is active
- Test on a different device

### "FCM Token registered successfully" but still no notifications

1. **Check Firestore** - Is the token actually saved?
   - Firebase Console → Firestore → users → [your user] → Check `fcmToken` field

2. **Check Cloud Function logs** - Is it being triggered?
   - When a message is sent, check Firebase Console → Functions → Logs

3. **Check for errors in Cloud Function:**
   - `Receiver ... has no FCM token` - Token not saved properly
   - `Receiver ... not found` - User document doesn't exist

## What Changed

### Cloud Function (`functions/index.js`)
- Changed from `notification + data` payload to **data-only** payload
- This ensures `onMessageReceived` is **always** called, even when app is closed

### Android Service (`MyFirebaseMessagingService.kt`)
- Enhanced logging to help debug
- Handles data-only messages properly
- Shows notification using `MessageNotificationManager`

## Important Notes

1. **Data-only messages are required** - Using `notification` payload prevents `onMessageReceived` from being called when app is closed
2. **Cloud Function must be deployed** - The function won't work until deployed
3. **FCM tokens must be registered** - Check Firestore to verify
4. **Notification permissions** - Must be granted on Android 13+

## Next Steps

1. **Deploy the Cloud Function** (critical!)
2. **Test on a device** with the app completely closed
3. **Check Logcat** for FCM message receipt
4. **Check Firebase Console** for function logs and errors

If notifications still don't work after deployment, check the troubleshooting section above.

