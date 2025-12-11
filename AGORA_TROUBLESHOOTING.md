# Agora Initialization Error - Troubleshooting

## Error: "Can not initialize agora rtc engine"

This error can occur for several reasons. Here's how to fix it:

## Step 1: Verify SDK is Downloaded

1. **Sync Gradle:**
   - File → Sync Project with Gradle Files
   - Wait for sync to complete
   - Check for any errors in the Build tab

2. **Check SDK Download:**
   - The Agora SDK should download automatically
   - If it fails, check your internet connection
   - Try: Build → Clean Project, then Sync again

## Step 2: Verify App ID

1. **Check App ID in code:**
   - Open: `app/src/main/java/com/familycalls/app/ui/call/VideoCallActivity.kt`
   - Verify: `private val appId = "00d17693a38645dea2ce319c23b1476b"`
   - Make sure there are no extra spaces or quotes

2. **Verify in Agora Console:**
   - Go to: https://console.agora.io/
   - Check your project's App ID matches

## Step 3: Check Permissions

Make sure these permissions are in `AndroidManifest.xml`:
- ✅ `INTERNET`
- ✅ `CAMERA`
- ✅ `RECORD_AUDIO`
- ✅ `READ_PHONE_STATE` (added)

## Step 4: Check Logcat for Detailed Error

1. In Android Studio, open **Logcat** (bottom panel)
2. Filter by: `Agora` or `VideoCallActivity`
3. Look for the actual error message
4. Common errors:
   - **Error -2**: Invalid App ID
   - **Error -7**: SDK not initialized
   - **Native library error**: SDK not downloaded properly

## Step 5: Clean and Rebuild

1. **Build** → **Clean Project**
2. **File** → **Invalidate Caches** → **Invalidate and Restart**
3. **File** → **Sync Project with Gradle Files**
4. **Build** → **Rebuild Project**

## Step 6: Check SDK Version

If issues persist, try a different Agora SDK version:

In `app/build.gradle`, change:
```gradle
implementation 'io.agora.rtc:full-sdk:4.3.0'
```

To:
```gradle
implementation 'io.agora.rtc:full-sdk:4.2.0'
```

Then sync and rebuild.

## Step 7: Verify Native Libraries

The Agora SDK includes native libraries. Make sure:
1. Gradle sync completed successfully
2. No errors about missing `.so` files
3. Check `app/build/intermediates/merged_native_libs/` for Agora libraries

## Common Solutions

### Solution 1: App ID Format
- App ID should be 32 characters
- No spaces or special characters
- Your App ID: `00d17693a38645dea2ce319c23b1476b` ✅ (32 chars, looks correct)

### Solution 2: SDK Not Downloaded
- Check internet connection
- Try: `./gradlew clean` then `./gradlew build`
- Check if Agora SDK appears in External Libraries in Android Studio

### Solution 3: Native Library Issue
- Clean project
- Delete `.gradle` folder
- Re-sync Gradle

## Get Detailed Error

The code now shows detailed error messages. When you run the app and try to call:
1. Check the Toast message (it will show the actual error)
2. Check Logcat for full stack trace
3. Share the error message for more specific help

## Quick Test

1. **Run the app**
2. **Try to make a call**
3. **Check the Toast message** - it will tell you the exact error
4. **Check Logcat** for detailed logs

---

**Next step:** Run the app, try a call, and check what error message appears. That will help identify the exact issue!

