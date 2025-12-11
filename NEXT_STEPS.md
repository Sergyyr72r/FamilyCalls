# ✅ Next Steps - Simple Instructions

Follow these steps in order:

## Step 1: Set Up Firebase (5 minutes)

1. Go to: https://console.firebase.google.com/
2. Click **"Add project"** or **"Create a project"**
3. Enter project name: `FamilyCalls`
4. Click **Continue** → **Continue** → **Create project**
5. Wait for project to be created

## Step 2: Add Android App to Firebase

1. In Firebase Console, click the **Android icon** (🟢)
2. Enter package name: `com.familycalls.app`
3. Click **Register app**
4. **Download `google-services.json`** (important!)
5. Click **Next** → **Next** → **Continue to console**

## Step 3: Enable Firebase Services

### Enable Firestore:
1. Click **Firestore Database** (left sidebar)
2. Click **Create database**
3. Choose **Start in test mode**
4. Select location → **Enable**

### Enable Storage:
1. Click **Storage** (left sidebar)
2. Click **Get started**
3. Choose **Start in test mode**
4. Select location → **Done**

## Step 4: Add google-services.json to Project

1. Find the downloaded `google-services.json` file
2. Copy it
3. Paste it into: `/Users/sergeyromanov/Desktop/FamilyCalls/app/`
   - The file should be directly in the `app` folder
   - NOT in `app/src/main/`

## Step 5: Set Security Rules

### Firestore Rules:
1. Go to **Firestore Database** → **Rules** tab
2. Copy and paste this:

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

### Storage Rules:
1. Go to **Storage** → **Rules** tab
2. Copy and paste this:

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

## Step 6: Open Project in Android Studio

1. Open **Android Studio**
2. Click **File** → **Open**
3. Navigate to: `/Users/sergeyromanov/Desktop/FamilyCalls`
4. Select the folder and click **OK**
5. Wait for Gradle sync (bottom right corner)

## Step 7: Sync Gradle Files

1. Click **File** → **Sync Project with Gradle Files**
2. Wait for sync to complete (no errors should appear)

## Step 8: Connect Your Device

### Option A: Physical Android Phone
1. On your phone: **Settings** → **About phone**
2. Tap **Build number** 7 times (enables Developer options)
3. Go back: **Settings** → **Developer options**
4. Enable **USB debugging**
5. Connect phone to computer via USB
6. On phone, tap **Allow USB debugging**

### Option B: Android Emulator
1. In Android Studio: **Tools** → **Device Manager**
2. Click **Create Device**
3. Choose device (e.g., Pixel 5) → **Next**
4. Download system image → **Next** → **Finish**
5. Click **Play** button to start emulator

## Step 9: Build and Install

1. At the top of Android Studio, select your device from the dropdown
2. Click the green **Run** button (▶️) or press `Shift + F10`
3. Wait for build (2-5 minutes first time)
4. App will install and launch automatically!

## Step 10: Test the App

1. Enter your name and phone number
2. Tap **Register**
3. You should see the contacts screen
4. Tap the share button (top right) to share with family

---

## ✅ Checklist

Before building, make sure:
- [ ] Firebase project created
- [ ] `google-services.json` is in `app/` folder
- [ ] Firestore Database enabled
- [ ] Firebase Storage enabled
- [ ] Security rules set
- [ ] Project opened in Android Studio
- [ ] Gradle sync completed (no errors)
- [ ] Device connected or emulator running

---

## ❗ If You See Errors

**"google-services.json not found"**
→ Make sure file is in `app/` folder (not `app/src/main/`)

**"Gradle sync failed"**
→ Check internet connection, then: **File** → **Invalidate Caches** → **Restart**

**"Device not found"**
→ Run in terminal: `adb devices` to check connection

---

**That's it!** Once you complete these steps, your app will be ready to use. 🚀

