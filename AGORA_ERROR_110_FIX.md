# Fixing Agora Error Code 110 (Invalid Token)

## What is Error Code 110?

Error code **110** means `ERR_INVALID_TOKEN`. This error occurs when:
- **App Certificate is ENABLED** in your Agora Console project
- Your app is trying to join a channel **without a token** (using `null`)
- The SDK requires token authentication but no valid token is provided

## Why This Happens

For the **free tier** of Agora, you can use your App ID directly without tokens **ONLY if App Certificate is DISABLED**. 

If App Certificate is enabled, Agora requires token-based authentication for security, which means you need to:
1. Generate tokens on a server
2. Pass tokens to the app when joining channels

## Solution: Disable App Certificate (Recommended for Free Tier)

Since you're using the free tier and want simple setup, **disable App Certificate** in your Agora Console:

### Step-by-Step Instructions

1. **Go to Agora Console**: https://console.agora.io/
2. **Login** to your account
3. **Select your project** (the one with App ID: `00d17693a38645dea2ce319c23b1476b`)
4. **Navigate to**: Project Settings → **App Certificate**
5. **Check the status**:
   - If it shows **"Enabled"** → Click **"Disable"**
   - If it shows **"Disabled"** → The error might be something else
6. **Confirm** the disable action
7. **Wait a few minutes** for changes to propagate
8. **Rebuild and test** your app

### Visual Guide

```
Agora Console
  └── Your Project
      └── Project Settings
          └── App Certificate
              └── Status: [Enabled] → Click "Disable"
```

## Alternative Solution: Use Token Authentication

If you **must** keep App Certificate enabled (for production security), you need to:

1. **Generate tokens on a server** using Agora's token generator
2. **Pass tokens to your app** when joining channels
3. **Handle token expiration** and refresh tokens

This is more complex and requires server-side code. For a family app using the free tier, **disabling App Certificate is the simpler solution**.

## Verify the Fix

After disabling App Certificate:

1. **Wait 2-3 minutes** for changes to take effect
2. **Rebuild your app**:
   - `Build` → `Clean Project`
   - `Build` → `Rebuild Project`
3. **Run the app** and try making a video call
4. **Check Logcat** - you should see:
   ```
   Agora: Joining channel: [channel_name], UID: [uid], Token: null (App Certificate disabled)
   Agora: joinChannel result: 0 (0 = success)
   Agora: onJoinChannelSuccess: channel=[channel_name], uid=[uid]
   ```

## Updated Error Handling

The code now includes:
- ✅ Error code 110 detection with clear message
- ✅ Detailed logging in `joinChannel()` method
- ✅ Error messages explaining the issue

## Common Questions

### Q: Is it safe to disable App Certificate?
**A:** For a private family app using the free tier, yes. App Certificate adds security but requires token management. For production apps with many users, you should use tokens.

### Q: Will disabling App Certificate affect my existing setup?
**A:** No, it only affects authentication. Your App ID and all other settings remain the same.

### Q: How long does it take for changes to take effect?
**A:** Usually 2-5 minutes, but can take up to 10 minutes in some cases.

### Q: What if I still get error 110 after disabling?
**A:** 
1. Wait a few more minutes
2. Check that you disabled it for the correct project
3. Verify your App ID matches: `00d17693a38645dea2ce319c23b1476b`
4. Check Logcat for the exact error message

## Next Steps

1. **Disable App Certificate** in Agora Console (see steps above)
2. **Wait 2-3 minutes**
3. **Rebuild your app**
4. **Test video calling** - error 110 should be resolved

If you continue to have issues, check the Logcat output for more details.

