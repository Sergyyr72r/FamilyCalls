# Debugging Full-Screen Call Notifications

## Issue
When the app is closed and the screen is off, incoming call notifications only vibrate but don't wake the screen or show full-screen UI.

## What to Check

### 1. Check App Logs
When a call comes in, look for these log tags in Logcat:

**CallService:**
```
D/CallService: Service created
D/CallService: Showing incoming call notification: callerId=..., callerName=...
D/CallService: Wake lock acquired (flags: ...)
D/CallService: Foreground service started successfully with incoming call notification
```

**CallNotificationManager:**
```
D/CallNotificationManager: Creating notification with full-screen intent for: ...
D/CallNotificationManager: Notification built with full-screen intent
D/CallNotificationManager: Priority: MAX, Category: CALL, FullScreenIntent: true
```

**VideoCallActivity (if full-screen intent triggers):**
```
D/VideoCallActivity: === onCreate called ===
D/VideoCallActivity: Setting screen wake flags (API 27+)
D/VideoCallActivity: Screen wake flags set: showWhenLocked=true, turnScreenOn=true
```

**MyFirebaseMsgService (if FCM is working):**
```
D/MyFirebaseMsgService: Received call FCM from: ... (...)
D/MyFirebaseMsgService: CallService started from FCM
```

### 2. If VideoCallActivity onCreate is NOT Called

This means the full-screen intent is **not triggering**. Possible causes:

#### A. USE_FULL_SCREEN_INTENT Permission
- **Check**: Settings → Apps → Family Calls → Special app access → Display over other apps / Full-screen intent
- **Fix**: Grant permission if not granted

#### B. Notification Channel Settings
- The channel might be blocked or have wrong importance
- **Check**: Settings → Apps → Family Calls → Notifications → Family Calls channel
- **Should be**: High importance, All notifications enabled

#### C. Battery Optimization
- Battery optimization might be blocking full-screen intents
- **Check**: Settings → Apps → Family Calls → Battery → Unrestricted (if available)
- **Fix**: Disable battery optimization for the app

#### D. DOZE Mode Restrictions
- When screen is off, Android enters DOZE mode which can block full-screen intents
- **Note**: Even with proper setup, DOZE mode on some devices (especially Samsung) can be aggressive
- **Fix**: Add app to "Never sleeping" apps list if available

### 3. If VideoCallActivity onCreate IS Called But Screen Doesn't Wake

This means the activity is launching but screen wake isn't working:

#### A. Window Flags
- Check logs for "Screen wake flags set"
- The activity uses `setTurnScreenOn(true)` which should work

#### B. Wake Lock
- Check logs for "Wake lock acquired"
- The service acquires a wake lock but it might not be working

### 4. Samsung-Specific Issues

Samsung devices often have additional restrictions:

#### A. Do Not Disturb
- Settings → Notifications → Do Not Disturb → Allow exceptions → Family Calls

#### B. App Power Saving
- Settings → Battery → Background restrictions → Family Calls → Allow

#### C. Edge Lighting / Always On Display
- These features might interfere with full-screen notifications

### 5. Testing Steps

1. **Close the app completely** (swipe away from recent apps)
2. **Turn off the screen**
3. **Wait 30 seconds** (let device enter DOZE mode)
4. **Have someone call you**
5. **Check Logcat** for the log messages above
6. **Check if screen turns on**
7. **Check if VideoCallActivity opens**

### 6. Expected Behavior

When working correctly:
1. ✅ FCM receives call notification
2. ✅ CallService starts and acquires wake lock
3. ✅ Notification is posted with full-screen intent
4. ✅ Screen wakes up (from wake lock or full-screen intent)
5. ✅ VideoCallActivity onCreate is called
6. ✅ Activity's setTurnScreenOn keeps screen on
7. ✅ Full-screen incoming call UI is displayed

### 7. Common Issues

**Issue**: Only vibration, no screen wake
- **Likely cause**: Full-screen intent not triggering or wake lock not working
- **Check**: Logs to see if VideoCallActivity onCreate is called

**Issue**: Notification appears but not full-screen
- **Likely cause**: USE_FULL_SCREEN_INTENT permission not granted or notification channel issue
- **Fix**: Grant permission and check channel settings

**Issue**: Works when app is in background but not when closed
- **Likely cause**: DOZE mode blocking full-screen intents
- **Fix**: Disable battery optimization and add to "never sleep" list

## Next Steps

If full-screen notifications still don't work after checking all above:

1. Share Logcat output showing:
   - MyFirebaseMsgService logs
   - CallService logs
   - CallNotificationManager logs
   - VideoCallActivity logs (if any)

2. Share device information:
   - Android version
   - Device manufacturer/model
   - Whether permission is granted

3. Check if notification appears at all (even if not full-screen):
   - This helps narrow down the issue



