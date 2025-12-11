# Quick Start Guide

## Prerequisites Checklist

- [ ] Android Studio installed
- [ ] Firebase project created
- [ ] `google-services.json` downloaded and placed in `app/` folder
- [ ] Firestore Database enabled
- [ ] Firebase Storage enabled

## Setup Steps

1. **Clone/Download this project**

2. **Add Firebase Configuration**
   - Follow instructions in `FIREBASE_SETUP.md`
   - Place `google-services.json` in `app/` directory

3. **Open in Android Studio**
   - Open the project folder
   - Wait for Gradle sync to complete

4. **Build and Run**
   - Connect an Android device or start an emulator
   - Click Run (▶️) or press `Shift+F10`

## First Use

1. **Register First User**
   - Enter your name and phone number
   - Tap "Register"
   - You'll be taken to the contacts screen

2. **Share with Family**
   - Tap the share button (top right)
   - Share the download link with family members
   - They install, register, and automatically appear in your contacts

3. **Start Communicating**
   - Tap a contact to see options: "Call" or "Message"
   - Send messages, images, videos, or make video calls

## Features

✅ **Messaging**: Real-time text messages
✅ **Media Sharing**: Send pictures and videos
✅ **Video Calls**: Make video calls with mute/hang up
✅ **Call Management**: Accept/reject incoming calls
✅ **Auto Sync**: Family members automatically appear in contact list

## Troubleshooting

**App won't build?**
- Check that `google-services.json` is in `app/` folder
- Sync Gradle files: File → Sync Project with Gradle Files

**Can't register?**
- Check internet connection
- Verify Firestore is enabled in Firebase Console
- Check Firestore security rules

**Messages not syncing?**
- Check internet connection
- Verify Firestore rules allow read/write

**Video calls not working?**
- Grant camera and microphone permissions
- Check that WebRTC is properly configured

## Next Steps

- Customize the app icon
- Set up your own download link distribution
- Configure push notifications (optional)
- Set up a proper WebRTC signaling server for production

