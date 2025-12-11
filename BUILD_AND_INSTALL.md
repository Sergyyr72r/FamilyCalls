# Build and Install Guide

This guide will walk you through building and installing the Family Calls app on your Android device.

## Prerequisites

1. ✅ **Android Studio** (Arctic Fox or later) - [Download here](https://developer.android.com/studio)
2. ✅ **Android Device** with USB debugging enabled OR **Android Emulator**
3. ✅ **Firebase Project** (see Step 1 below)

## Step 1: Firebase Setup (REQUIRED - Do this first!)

The app **requires** Firebase to work. You must complete this step before building.

### 1.1 Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click **"Add project"** or **"Create a project"**
3. Enter project name: `FamilyCalls` (or any name you prefer)
4. Click **Continue**
5. Disable Google Analytics (optional) or enable it - your choice
6. Click **Create project**
7. Wait for project creation to complete

### 1.2 Add Android App to Firebase

1. In Firebase Console, click the **Android icon** (or "Add app" → Android)
2. Enter package name: `com.familycalls.app` (exactly this)
3. Enter app nickname: `Family Calls` (optional)
4. Click **Register app**
5. **Download `google-services.json`** - This is critical!
6. Click **Next** → **Next** → **Continue to console**

### 1.3 Enable Firebase Services

#### Enable Firestore Database:
1. In Firebase Console, go to **Firestore Database** (left sidebar)
2. Click **Create database**
3. Choose **Start in test mode** (for now)
4. Select a location (choose closest to you)
5. Click **Enable**

#### Enable Firebase Storage:
1. In Firebase Console, go to **Storage** (left sidebar)
2. Click **Get started**
3. Choose **Start in test mode**
4. Select a location (same as Firestore)
5. Click **Done**

### 1.4 Add google-services.json to Project

1. **Copy** the downloaded `google-services.json` file
2. **Paste** it into: `/Users/sergeyromanov/Desktop/FamilyCalls/app/`
   - The file should be at: `app/google-services.json`
   - NOT in `app/src/main/` - it goes directly in `app/` folder

### 1.5 Set Security Rules (Important!)

#### Firestore Rules:
1. Go to **Firestore Database** → **Rules** tab
2. Replace the rules with:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, write: if true;
    }
    match /messages/{messageId} {
      allow read, write: if true;
    }
    match /calls/{callId} {
      allow read, write: if true;
    }
  }
}
```

3. Click **Publish**

#### Storage Rules:
1. Go to **Storage** → **Rules** tab
2. Replace the rules with:

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /images/{userId}/{allPaths=**} {
      allow read, write: if true;
    }
    match /videos/{userId}/{allPaths=**} {
      allow read, write: if true;
    }
  }
}
```

3. Click **Publish**

---

## Step 2: Open Project in Android Studio

1. **Launch Android Studio**
2. Click **File** → **Open** (or **Open an Existing Project**)
3. Navigate to: `/Users/sergeyromanov/Desktop/FamilyCalls`
4. Select the folder and click **OK**
5. Wait for Gradle sync to complete (bottom status bar will show "Gradle sync finished")

**If you see errors about `google-services.json`:**
- Make sure the file is in `app/google-services.json`
- Click **File** → **Sync Project with Gradle Files**

---

## Step 3: Prepare Your Device

### Option A: Physical Android Device

1. **Enable Developer Options:**
   - Go to **Settings** → **About phone**
   - Tap **Build number** 7 times
   - You'll see "You are now a developer!"

2. **Enable USB Debugging:**
   - Go to **Settings** → **Developer options**
   - Enable **USB debugging**
   - Enable **Install via USB** (if available)

3. **Connect Device:**
   - Connect your phone to computer via USB
   - On your phone, when prompted, tap **Allow USB debugging**
   - Check "Always allow from this computer" (optional)

4. **Verify Connection:**
   - In Android Studio, open **Terminal** (bottom panel)
   - Run: `adb devices`
   - You should see your device listed

### Option B: Android Emulator

1. In Android Studio, click **Tools** → **Device Manager**
2. Click **Create Device**
3. Choose a device (e.g., Pixel 5)
4. Download a system image (API 30 or higher recommended)
5. Click **Finish**
6. Click **Play** button next to the emulator to start it

---

## Step 4: Build and Install the App

### Method 1: Build and Install via Android Studio (Recommended)

1. **Select your device:**
   - At the top toolbar, click the device dropdown
   - Select your connected device or emulator

2. **Build and Run:**
   - Click the green **Run** button (▶️) or press `Shift + F10` (Mac: `Control + R`)
   - OR go to **Run** → **Run 'app'**

3. **Wait for build:**
   - Android Studio will compile the app
   - First build may take 2-5 minutes
   - You'll see progress in the bottom status bar

4. **Install on device:**
   - The app will automatically install on your device
   - It will launch automatically

### Method 2: Build APK and Install Manually

#### Build Debug APK:

1. In Android Studio, go to **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**
2. Wait for build to complete
3. When done, click **locate** in the notification
4. The APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

#### Install APK on Device:

**Via USB:**
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

**Via File Transfer:**
1. Copy `app-debug.apk` to your phone
2. On your phone, open the APK file
3. Allow installation from unknown sources if prompted
4. Tap **Install**

#### Build Release APK (for distribution):

1. Go to **Build** → **Generate Signed Bundle / APK**
2. Select **APK** → **Next**
3. Create a keystore (first time only):
   - Click **Create new...**
   - Fill in the form (remember the password!)
   - Save the keystore file securely
4. Select your keystore → Enter passwords
5. Click **Next**
6. Select **release** build variant
7. Click **Finish**
8. APK will be at: `app/build/outputs/apk/release/app-release.apk`

---

## Step 5: Test the App

1. **First Launch:**
   - The app should open to the registration screen
   - Enter your name and phone number
   - Tap **Register**

2. **Verify Registration:**
   - You should see the contacts screen
   - If you're the first user, you'll see "No contacts yet"

3. **Share with Family:**
   - Tap the share button (top right)
   - Share the download link with family members
   - They can install and register

4. **Test Messaging:**
   - Once you have 2+ users, tap a contact → **Message**
   - Send a text message
   - Try sending an image

5. **Test Video Call:**
   - Tap a contact → **Call**
   - Grant camera and microphone permissions
   - Test mute and hang up

---

## Troubleshooting

### Build Errors

**"google-services.json not found"**
- ✅ Make sure the file is at `app/google-services.json`
- ✅ File name must be exactly `google-services.json`
- ✅ Sync Gradle: **File** → **Sync Project with Gradle Files**

**"Gradle sync failed"**
- ✅ Check internet connection
- ✅ In Android Studio: **File** → **Invalidate Caches** → **Invalidate and Restart**
- ✅ Try: **Build** → **Clean Project**, then **Build** → **Rebuild Project**

**"SDK not found"**
- ✅ Go to **Tools** → **SDK Manager**
- ✅ Install Android SDK Platform 34
- ✅ Install Android SDK Build-Tools

### Runtime Errors

**"App crashes on launch"**
- ✅ Check that Firebase is properly configured
- ✅ Verify `google-services.json` is correct
- ✅ Check Logcat in Android Studio for error messages

**"Can't register user"**
- ✅ Check internet connection
- ✅ Verify Firestore is enabled in Firebase Console
- ✅ Check Firestore security rules

**"Messages not syncing"**
- ✅ Check internet connection
- ✅ Verify Firestore rules allow read/write
- ✅ Check Logcat for errors

**"Video call not working"**
- ✅ Grant camera and microphone permissions
- ✅ Check that WebRTC dependencies are included
- ✅ Note: Full WebRTC requires a signaling server for production

### Device Connection Issues

**Device not detected:**
```bash
# Check if device is connected
adb devices

# If empty, try:
adb kill-server
adb start-server
adb devices
```

**"Installation failed"**
- ✅ Enable "Install via USB" in Developer options
- ✅ Uninstall any existing version first
- ✅ Check available storage on device

---

## Sharing with Family Members

### Option 1: Direct APK Sharing

1. Build release APK (see Method 2 above)
2. Share `app-release.apk` via:
   - Email
   - Cloud storage (Google Drive, Dropbox)
   - Messaging apps
3. Family members install the APK on their devices

### Option 2: Firebase App Distribution (You've already configured this!)

1. Build release APK
2. In terminal, run:
   ```bash
   ./gradlew assembleRelease appDistributionUploadRelease
   ```
3. Family members will receive an email with download link
4. They install from the link

### Option 3: Google Play Store (Future)

- Create a Google Play Developer account
- Upload the app as a private/internal app
- Share with family members

---

## Next Steps After Installation

1. ✅ **Register all 5 family members**
2. ✅ **Test messaging between devices**
3. ✅ **Test video calls**
4. ✅ **Customize app icon** (optional)
5. ✅ **Set up push notifications** (optional, for better call notifications)

---

## Quick Command Reference

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install on connected device
adb install app/build/outputs/apk/debug/app-debug.apk

# Check connected devices
adb devices

# View logs
adb logcat
```

---

## Need Help?

- Check `README.md` for full documentation
- Check `FIREBASE_SETUP.md` for detailed Firebase setup
- Check Android Studio's **Logcat** for error messages
- Firebase Console → **Project Settings** → **Your apps** to verify configuration

Good luck! 🚀

