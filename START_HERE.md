# 🚀 START HERE - Build & Install Guide

## Quick Start (5 Steps)

### Step 1: Set Up Firebase (5 minutes) ⚠️ REQUIRED

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project
3. Add Android app with package: `com.familycalls.app`
4. **Download `google-services.json`**
5. Place it in: `app/google-services.json` (this folder)
6. Enable Firestore Database and Storage
7. Set security rules (see `FIREBASE_SETUP.md`)

### Step 2: Open in Android Studio

1. Open Android Studio
2. **File** → **Open** → Select this folder
3. Wait for Gradle sync (bottom right)

### Step 3: Connect Your Device

**Physical Device:**
- Enable Developer Options (tap Build number 7 times)
- Enable USB Debugging
- Connect via USB

**OR Emulator:**
- Tools → Device Manager → Create Device

### Step 4: Build & Install

1. Click the green **Run** button (▶️) in Android Studio
2. Select your device
3. Wait for build and install
4. App will launch automatically!

### Step 5: Test

1. Register with your name and phone
2. Share download link with family (top right button)
3. Start messaging and calling!

---

## 📚 Detailed Guides

- **`BUILD_AND_INSTALL.md`** - Complete step-by-step build guide
- **`FIREBASE_SETUP.md`** - Detailed Firebase configuration
- **`INSTALL_CHECKLIST.md`** - Pre-build verification checklist
- **`README.md`** - Full app documentation

---

## ⚡ Quick Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Install on device
adb install app/build/outputs/apk/debug/app-debug.apk

# Check connected devices
adb devices
```

Or use the helper script:
```bash
./build_commands.sh
```

---

## ❗ Common Issues

**"google-services.json not found"**
→ Make sure file is in `app/` folder (not `app/src/main/`)

**"Gradle sync failed"**
→ Check internet, then: File → Invalidate Caches → Restart

**"Device not detected"**
→ Run `adb devices` to check connection

---

## ✅ Success Checklist

- [ ] Firebase project created
- [ ] `google-services.json` in `app/` folder
- [ ] Firestore and Storage enabled
- [ ] Project opens in Android Studio
- [ ] Device connected
- [ ] App builds successfully
- [ ] App installs and runs
- [ ] Can register user
- [ ] Can see contacts screen

---

**Need help?** Check the detailed guides above or see troubleshooting in `BUILD_AND_INSTALL.md`

Good luck! 🎉

