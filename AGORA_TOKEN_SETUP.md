# Setting Up Agora Token Authentication

## Why Tokens Are Required

Since App Certificate is **mandatory** in your Agora Console (cannot be disabled), you **must** use token-based authentication. This is actually more secure and is the recommended approach.

## Step 1: Generate a Temporary Token from Agora Console

### For Testing (Temporary Token):

1. **Go to Agora Console**: https://console.agora.io/
2. **Select your project**: "Family Calls"
3. **Navigate to**: Your Project → **Security** section
4. **Find "Generate Temp Token" button** (should be near the Primary Certificate field)
5. **Click "Generate Temp Token"**
6. **Copy the generated token** - it will look something like:
   ```
   006970CA35DE0EEC8250572E0C30835940IAA...
   ```

### Important Notes:
- **Temp tokens expire** after 24 hours
- For production, you'll need to implement server-side token generation
- Each user should have their own token (or use the same token for testing)

## Step 2: Set the Token in Your App

### Option A: Using ADB (Recommended for Testing)

1. **Connect your device** via USB with ADB enabled
2. **Open Terminal** on your Mac
3. **Run this command** (replace `YOUR_TOKEN_HERE` with the actual token):

```bash
adb shell "run-as com.familycalls.app sh -c 'echo \"<string name=\\\"agoraToken\\\">YOUR_TOKEN_HERE</string>\" >> /data/data/com.familycalls.app/shared_prefs/FamilyCalls.xml'"
```

**Or use a simpler method:**

```bash
# First, get the token from Agora Console
# Then run:
adb shell
run-as com.familycalls.app
echo 'YOUR_TOKEN_HERE' > /data/data/com.familycalls.app/files/agora_token.txt
exit
exit
```

Then update the code to read from the file instead.

### Option B: Add Token Input in App Settings (Better UX)

We can add a settings screen where you can paste the token. Would you like me to implement this?

### Option C: Hardcode for Testing (Quick but Not Secure)

For quick testing only, you can temporarily hardcode the token in `VideoCallActivity.kt`:

```kotlin
private val testToken = "YOUR_TOKEN_HERE" // Remove after testing!
```

Then in `joinChannel()`, use:
```kotlin
val token = testToken // For testing only
```

**⚠️ Warning**: Never commit hardcoded tokens to version control!

## Step 3: Verify Token is Set

1. **Run your app**
2. **Try to make a video call**
3. **Check Logcat** - you should see:
   ```
   Agora: Joining channel: [channel], UID: [uid], Token: provided (006970CA35...)
   ```
4. **If token is missing**, you'll see:
   ```
   Agora: Token is required but not set
   ```

## Step 4: Test the Video Call

1. **Generate a temp token** from Agora Console
2. **Set it in your app** (using one of the methods above)
3. **Run the app** on two devices (or emulator + device)
4. **Make a video call** between them
5. **Both devices should join the same channel**

## Token Expiration Handling

Temp tokens expire after 24 hours. When a token expires:

1. **Generate a new temp token** from Agora Console
2. **Update it in your app** (using the same method you used before)
3. **Restart the app** or the token will be reloaded automatically

For production, you'll need to:
- Implement server-side token generation
- Handle token refresh automatically using `onTokenPrivilegeWillExpire` callback

## Current Code Status

The code has been updated to:
- ✅ Load token from SharedPreferences (`agoraToken` key)
- ✅ Check if token exists before joining channel
- ✅ Show helpful error message if token is missing
- ✅ Use token when joining channel

## Quick Test Command

After generating a token from Agora Console, use this one-liner to set it:

```bash
TOKEN="YOUR_TOKEN_FROM_CONSOLE"
adb shell "run-as com.familycalls.app sh -c 'mkdir -p /data/data/com.familycalls.app/shared_prefs && echo \"<?xml version=\\\"1.0\\\" encoding=\\\"utf-8\\\"?><map><string name=\\\"agoraToken\\\">$TOKEN</string></map>\" > /data/data/com.familycalls.app/shared_prefs/FamilyCalls.xml'"
```

## Next Steps

1. **Generate temp token** from Agora Console (Security section)
2. **Set it in your app** using one of the methods above
3. **Test video calling** - error 110 should be resolved!

If you'd like, I can add a settings screen in the app where you can easily paste and save the token. Would that be helpful?

