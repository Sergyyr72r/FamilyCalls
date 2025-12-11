# Firebase Setup Guide

## Step 1: Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project"
3. Enter project name (e.g., "FamilyCalls")
4. Follow the setup wizard

## Step 2: Add Android App

1. In Firebase Console, click the Android icon
2. Enter package name: `com.familycalls.app`
3. Download `google-services.json`
4. Place the file in: `app/google-services.json`

## Step 3: Enable Firebase Services

### Firestore Database
1. Go to Firestore Database in Firebase Console
2. Click "Create database"
3. Start in test mode (or production with rules below)
4. Choose a location

### Firebase Storage
1. Go to Storage in Firebase Console
2. Click "Get started"
3. Use default security rules (or use rules below)
4. Choose a location

## Step 4: Set Security Rules

### Firestore Rules
Go to Firestore Database → Rules tab:

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

**Note:** These rules allow anyone to read/write. For a private family app with only 5 members, this is acceptable, but you can add additional security if needed.

### Storage Rules
Go to Storage → Rules tab:

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

## Step 5: Build and Run

1. Sync Gradle files in Android Studio
2. Build the project
3. Run on device or emulator

## Troubleshooting

### "google-services.json not found"
- Ensure the file is in `app/google-services.json` (not `app/src/main/`)
- Re-sync Gradle files

### "Firestore permission denied"
- Check that Firestore is enabled
- Verify security rules are set correctly

### "Storage permission denied"
- Check that Storage is enabled
- Verify storage rules are set correctly

