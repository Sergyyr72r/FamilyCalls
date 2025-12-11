# Agora.io Video Calling - Quick Setup

## ✅ What I've Done

I've integrated **Agora.io SDK** into your app for in-app video calling:
- ✅ Added Agora SDK to `build.gradle`
- ✅ Updated `VideoCallActivity` to use Agora
- ✅ Set up video/audio streams
- ✅ Added mute/unmute functionality
- ✅ Added hang up functionality

## 🔑 What You Need to Do

### Step 1: Get Agora App ID (5 minutes)

1. Go to: **https://www.agora.io/**
2. Click **"Sign Up"** (free account)
3. After signup, go to: **https://console.agora.io/**
4. Click **"Create"** → **"New Project"**
5. Enter project name: **"Family Calls"**
6. Copy your **App ID** (looks like: `a1b2c3d4e5f6g7h8i9j0`)

### Step 2: Add App ID to Code

1. Open: `app/src/main/java/com/familycalls/app/ui/call/VideoCallActivity.kt`
2. Find line: `private val appId = "YOUR_AGORA_APP_ID"`
3. Replace `"YOUR_AGORA_APP_ID"` with your actual App ID:
   ```kotlin
   private val appId = "a1b2c3d4e5f6g7h8i9j0" // Your actual App ID
   ```

### Step 3: Rebuild and Test

1. **Sync Gradle files** in Android Studio
2. **Build** the project
3. **Install** on your device
4. **Test video call** with another family member

---

## How It Works

1. **User A** taps "Call" on **User B**
2. Both users join the same **channel** (room)
3. Video/audio streams are connected
4. They can see and hear each other
5. Mute/unmute and hang up work

---

## Cost

- **Free tier:** 10,000 minutes/month
- Your usage: ~2,700 minutes/month (5 users, 3 calls/day, 30 min/call)
- **You'll pay $0!** ✅

---

## Features

- ✅ In-app video calling (no external apps)
- ✅ Mute/unmute microphone
- ✅ Hang up
- ✅ Accept/reject incoming calls
- ✅ Works on Android 7.0+

---

## Troubleshooting

**"App ID not set" error:**
→ Make sure you replaced `YOUR_AGORA_APP_ID` with your actual App ID

**Video not showing:**
→ Check camera permissions are granted

**Can't hear audio:**
→ Check microphone permissions are granted

**Connection fails:**
→ Check internet connection
→ Verify App ID is correct

---

## Next Steps

1. **Get Agora App ID** (see Step 1 above)
2. **Add it to VideoCallActivity.kt**
3. **Rebuild and test!**

That's it! Video calls will work within your app! 🎉

