# Family Calls - Private Family Messenger & Video Calls App

A private Android app for family communication with messaging, media sharing, and video calling capabilities. Designed exclusively for your family (maximum 5 members).

## Features

- ✅ **Private Family Network**: Only 5 family members can register
- ✅ **Real-time Messaging**: Send text messages to family members
- ✅ **Media Sharing**: Send pictures and videos
- ✅ **Video Calls**: Make video calls with mute and hang up functionality
- ✅ **Call Management**: Accept/reject incoming calls
- ✅ **Auto Contact Sync**: Family members automatically appear in contact list
- ✅ **Download Link Sharing**: Share the app download link with family members

## Prerequisites

1. **Android Studio** (Arctic Fox or later)
2. **Firebase Project**:
   - Create a Firebase project at [Firebase Console](https://console.firebase.google.com/)
   - Enable Firestore Database
   - Enable Firebase Storage
   - Download `google-services.json` and place it in `app/` directory

## Setup Instructions

### 1. Firebase Setup

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project or use an existing one
3. Add an Android app with package name: `com.familycalls.app`
4. Download `google-services.json`
5. Place the file in `app/google-services.json`

### 2. Firestore Rules

Set up Firestore security rules in Firebase Console:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, write: if true; // For private family app
    }
    match /messages/{messageId} {
      allow read, write: if true; // For private family app
    }
    match /calls/{callId} {
      allow read, write: if true; // For private family app
    }
  }
}
```

### 3. Storage Rules

Set up Firebase Storage rules:

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /images/{userId}/{allPaths=**} {
      allow read, write: if true; // For private family app
    }
    match /videos/{userId}/{allPaths=**} {
      allow read, write: if true; // For private family app
    }
  }
}
```

### 4. Build and Run

1. Open the project in Android Studio
2. Sync Gradle files
3. Build the project
4. Run on a device or emulator

## Usage

### First Time Setup

1. **Register**: Enter your name and phone number
2. The app will automatically register your device
3. You'll see other family members in your contact list

### Sharing with Family

1. Tap the share button (top right) in the contacts screen
2. Share the download link with family members
3. When they install and register, they'll automatically appear in everyone's contact list

### Messaging

1. Tap a contact → Select "Message"
2. Type a message and send
3. Tap the attach button to send images or videos
4. Messages are synced in real-time

### Video Calls

1. Tap a contact → Select "Call"
2. For incoming calls: Tap "Accept" or "Reject"
3. During calls:
   - Tap "Mute" to mute/unmute microphone
   - Tap "Hang Up" to end the call

## Architecture

- **Backend**: Firebase (Firestore, Storage)
- **Video Calling**: WebRTC
- **UI**: Material Design Components
- **Language**: Kotlin
- **Architecture**: MVVM pattern with repositories

## Project Structure

```
app/
├── src/main/
│   ├── java/com/familycalls/app/
│   │   ├── data/
│   │   │   ├── model/          # Data models (User, Message)
│   │   │   └── repository/     # Data repositories
│   │   ├── ui/
│   │   │   ├── auth/           # Authentication screen
│   │   │   ├── main/           # Main contacts screen
│   │   │   ├── chat/           # Chat/messaging screen
│   │   │   └── call/           # Video call screen
│   │   ├── service/            # Background services
│   │   └── utils/              # Utility functions
│   └── res/                     # Resources (layouts, strings, etc.)
```

## Important Notes

1. **Maximum 5 Users**: The app enforces a limit of 5 registered family members
2. **Device-Based Auth**: Authentication is based on device ID, so each device needs to register once
3. **WebRTC Setup**: For production, you'll need to set up a signaling server for WebRTC. The current implementation uses a basic Firebase-based approach
4. **Download Link**: Update the download link in `ShareUtils.kt` with your actual app distribution method (Play Store, direct APK, etc.)

## Troubleshooting

### Firebase Connection Issues
- Ensure `google-services.json` is in the correct location
- Check that Firebase services are enabled in Firebase Console

### Video Call Issues
- Ensure camera and microphone permissions are granted
- For production, set up a proper WebRTC signaling server

### Registration Issues
- Check Firestore rules allow read/write
- Verify device has internet connection

## Future Enhancements

- [ ] Push notifications for messages and calls
- [ ] Group messaging
- [ ] Call history
- [ ] Message search
- [ ] Better WebRTC signaling server integration

## License

Private family app - for personal use only.

