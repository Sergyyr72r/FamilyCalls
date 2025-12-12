# Final Solution: Screen Wake for Incoming Calls

## The Problem

When the app is completely closed and the device screen is off, incoming call notifications:
- ✅ Show as regular notifications (vibration works)
- ❌ Don't wake the screen
- ❌ Don't show full-screen UI

## Root Cause

**Android DOZE mode** (especially aggressive on Samsung devices) blocks:
- Background activity starts from services
- Wake locks from background apps  
- Full-screen intents when app is closed

This is **by design** to prevent battery drain and security issues.

## The Reality

**Full-screen call notifications when app is closed are unreliable on Android**, especially:
- Samsung devices (aggressive DOZE)
- Android 11+ (stricter background restrictions)
- Devices in deep sleep (DOZE_SUSPEND mode)

## What We've Tried

1. ✅ Full-screen intent in notification → Blocked by DOZE
2. ✅ Direct activity start from FCM service → Blocked by DOZE  
3. ✅ Wake locks → Blocked by DOZE
4. ✅ Notification with MAX priority → Shows but doesn't wake screen

## Solutions (User Must Configure)

### Solution 1: Disable Battery Optimization (REQUIRED)

**Samsung/OneUI:**
1. Settings → Apps → Family Calls
2. Battery → **Unrestricted**
3. OR: Settings → Battery → Background restrictions → Allow Family Calls

**Stock Android:**
1. Settings → Apps → Family Calls → Battery → **Unrestricted**

**This is CRITICAL** - without this, DOZE will block everything.

### Solution 2: Grant "Display Over Other Apps" Permission

1. Settings → Apps → Special access → Display over other apps
2. Find **Family Calls** → Enable

### Solution 3: Disable DOZE for App

**Samsung:**
1. Settings → Battery → Optimize battery usage
2. Find **Family Calls** → Don't optimize

**Stock Android:**
1. Settings → Apps → Special access → Optimize battery usage
2. Find **Family Calls** → Don't optimize

### Solution 4: Add to "Never Sleeping Apps"

**Samsung:**
1. Settings → Battery → Background restrictions
2. Find **Family Calls** → Allow

## Testing After Configuration

After making the above changes:

1. **Restart the device** (important - DOZE settings may require reboot)
2. **Close app completely** (swipe away)
3. **Turn off screen**
4. **Wait 30 seconds** (let DOZE activate)
5. **Have someone call you**
6. **Check if screen wakes**

## Expected Behavior After Configuration

✅ Screen wakes up  
✅ Full-screen call UI appears  
✅ Accept/Reject buttons visible  
✅ Sound and vibration work

## If Still Not Working

### Check Logcat:
```bash
adb logcat | grep -E "MyFirebaseMsgService|CallService|VideoCallActivity"
```

Look for:
- `VideoCallActivity started directly from FCM` → Activity start attempted
- `VideoCallActivity: === onCreate called ===` → Activity actually started
- Any error messages about blocked activity starts

### Alternative: Keep Notification Prominent

If screen wake still doesn't work, at least ensure:
- ✅ Heads-up notification appears
- ✅ Sound and vibration work
- ✅ User can tap notification to open call UI

This is better than nothing, and is how most third-party calling apps work.

## Why This Is Difficult

Android restricts background activity starts for good reasons:
- Prevents battery drain
- Prevents security issues (apps waking device)
- Prevents spam/abuse

System dialer apps (like Phone app) have special privileges that third-party apps don't.

## Bottom Line

**Full-screen wake notifications require user configuration** on most devices. The code is correct, but Android's DOZE mode blocks it unless the user:
1. Disables battery optimization
2. Grants special permissions
3. Configures device settings

**This is normal Android behavior** - even WhatsApp and other apps require similar configuration on some devices.
