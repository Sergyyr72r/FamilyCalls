# Notification Debug Guide

## Current Issue: No Notifications Showing

The notification system is set up, but notifications aren't appearing when someone calls you.

## How to Debug

### Step 1: Check Logcat

When someone calls you, filter Logcat by these tags:
- `MainActivity`
- `VideoCallActivity`
- `CallService`
- `CallNotificationManager`

**Expected logs when someone calls:**

**On Caller's Device (VideoCallActivity):**
```
VideoCallActivity: onCreate - starting call activity
VideoCallActivity: Creating call document:
VideoCallActivity:   callerId: [caller_user_id]
VideoCallActivity:   receiverId: [your_user_id]
VideoCallActivity:   status: ringing
VideoCallActivity: ✓ Call document created successfully!
VideoCallActivity:   Document ID: [document_id]
```

**On Your Device (MainActivity):**
```
MainActivity: MainActivity onCreate - currentUserId: [your_user_id]
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

### Step 2: Verify Call Document in Firestore

1. Open Firebase Console: https://console.firebase.google.com/
2. Go to **Firestore Database**
3. Check the **"calls"** collection
4. When someone calls, you should see a document with:
   - `callerId`: The caller's user ID
   - `receiverId`: Your user ID
   - `status`: "ringing"
   - `timestamp`: Current timestamp

**If the document doesn't exist:**
- The call document isn't being created
- Check VideoCallActivity logs for errors

**If the document exists but no notification:**
- The listener isn't detecting it
- Check if MainActivity is active (in foreground)
- Check MainActivity logs for listener activity

### Step 3: Check Notification Settings

**Android 13+ (API 33+):**
1. Settings → Apps → Family Calls
2. Tap **Notifications**
3. Ensure notifications are **enabled**
4. Check that **"Family Calls"** channel is enabled and set to **High importance**

**All Android Versions:**
1. Settings → Apps → Family Calls
2. Tap **Notifications**
3. Ensure notifications are **enabled**

### Step 4: Test with App in Foreground

**Important:** The Firestore listener only works when MainActivity is active!

1. **Open the app** (keep MainActivity visible)
2. **Have someone call you** from another device
3. **Check Logcat** - you should see the listener trigger
4. **Check if notification appears**

If notification appears when app is in foreground:
- The system works, but only when app is active
- You need background notifications (see Solution below)

If notification doesn't appear even when app is in foreground:
- Check notification settings
- Check Logcat for errors
- Verify call document is created correctly

## Root Cause

The Firestore listener in `MainActivity.listenForIncomingCalls()` only works when:
- ✅ MainActivity is in the **foreground**
- ✅ The app is **not killed** by the system

**It does NOT work when:**
- ❌ App is in the **background**
- ❌ App is **killed** by the system
- ❌ Device is **locked** (sometimes)

## Solution: Background Notifications

To receive notifications when the app is in the background, you need **Firebase Cloud Messaging (FCM)**:

1. **FCM sends push notifications** to devices
2. **Works even when app is killed**
3. **Standard approach** for background notifications

### Current Implementation (Foreground Only)
```
Caller → Creates Firestore document → MainActivity listener → Notification
```
❌ Only works when MainActivity is active

### With FCM (Background Support)
```
Caller → Creates Firestore document → Cloud Function → FCM → Notification
```
✅ Works even when app is killed

## Quick Test

To test if notifications work at all:

1. **Keep MainActivity open** (app in foreground)
2. **Have someone call you**
3. **Check Logcat** for listener activity
4. **Check if notification appears**

If it works in foreground but not in background:
- ✅ Notification system is working
- ❌ Need FCM for background support

If it doesn't work even in foreground:
- ❌ Check notification settings
- ❌ Check Logcat for errors
- ❌ Verify call document is created

## Next Steps

1. **Test with app in foreground** first
2. **Check Logcat** to see what's happening
3. **Verify call document** is created in Firestore
4. **If it works in foreground**: Implement FCM for background support
5. **If it doesn't work**: Check notification settings and logs

