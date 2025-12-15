# Debugging Screen Wake for Incoming Calls

## Critical: Check Logcat with Proper Filter

Your logs show a notification was posted but NO app logs. This means either:
1. The logs are filtered out
2. FCM service isn't being called
3. Activity start is failing silently

### Check Logcat with This Filter:

In Android Studio Logcat, use this filter:
```
package:com.familycalls.app
```

Or use adb:
```bash
adb logcat | grep -E "MyFirebaseMsgService|CallService|VideoCallActivity|CallNotificationManager"
```

### Expected Logs When Call Arrives:

**If FCM is working, you should see:**
```
D/MyFirebaseMsgService: Received call FCM from: [name] ([id])
D/MyFirebaseMsgService: VideoCallActivity started directly from FCM
D/VideoCallActivity: === onCreate called ===
D/VideoCallActivity: Setting screen wake flags (API 27+)
D/CallService: Wake lock acquired
```

**If you DON'T see these logs:**
- FCM message isn't being received
- OR logs are being filtered

## Test: Is FCM Working?

1. **Close the app completely** (swipe away)
2. **Turn off screen**
3. **Open Logcat** with filter: `package:com.familycalls.app`
4. **Have someone call you**
5. **Check logs immediately**

### What to Look For:

**✅ If you see `MyFirebaseMsgService` logs:**
- FCM is working
- The issue is with activity start/screen wake

**❌ If you DON'T see any app logs:**
- FCM might not be triggering
- OR check if Cloud Function is deployed
- OR check if FCM token is registered

## Alternative: Test Activity Start Directly

To verify if activity start works when app is closed:

1. Keep app closed
2. Screen off
3. In Logcat, run:
   ```bash
   adb shell am start -n com.familycalls.app/.ui.call.VideoCallActivity --es contactId "test" --es contactName "Test" --ez isIncoming true
   ```
4. Check if screen wakes up

If this works, the issue is with FCM → activity start.
If this doesn't work, the issue is with activity wake capability.

## Samsung-Specific: DOZE Mode Restrictions

Samsung devices have aggressive DOZE mode that blocks:
- Background activity starts
- Wake locks from background services
- Full-screen intents

### Solutions:

1. **Disable battery optimization:**
   - Settings → Apps → Family Calls → Battery → Unrestricted

2. **Add to "Never sleeping" apps:**
   - Settings → Battery → Background restrictions → Allow Family Calls

3. **Disable DOZE for app:**
   - Settings → Apps → Special access → Optimize battery usage → Family Calls → Don't optimize

4. **Grant "Display over other apps" permission:**
   - Settings → Apps → Special access → Display over other apps → Family Calls → Allow

## If Activity Start Is Blocked

When the app is closed and device is in DOZE, Android blocks background activity starts. 

### Workaround Options:

1. **Keep notification as fallback** - At least user gets notification
2. **Request special permissions** - "Display over other apps" 
3. **Use system call notification** - Requires app to be default dialer (not practical)

## Current Status

Based on your logs:
- ✅ Notification is being posted (`onNotificationPosted com.familycalls.app`)
- ❓ Unknown if FCM service is being called (no logs)
- ❓ Unknown if activity start is attempted (no logs)

**Next Step:** Check Logcat with proper filter to see what's actually happening.


