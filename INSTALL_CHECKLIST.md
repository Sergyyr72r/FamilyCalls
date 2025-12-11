# Installation Checklist

Use this checklist to ensure everything is set up correctly before building.

## Pre-Build Checklist

### Firebase Setup
- [ ] Firebase project created
- [ ] Android app added to Firebase project
- [ ] Package name set to: `com.familycalls.app`
- [ ] `google-services.json` downloaded
- [ ] `google-services.json` placed in `app/` folder (not `app/src/main/`)
- [ ] Firestore Database enabled
- [ ] Firebase Storage enabled
- [ ] Firestore security rules set (see FIREBASE_SETUP.md)
- [ ] Storage security rules set (see FIREBASE_SETUP.md)

### Android Studio Setup
- [ ] Android Studio installed and updated
- [ ] Project opened in Android Studio
- [ ] Gradle sync completed successfully (no errors)
- [ ] Android SDK Platform 34 installed
- [ ] Android SDK Build-Tools installed

### Device Setup
- [ ] Android device connected via USB OR
- [ ] Android emulator created and running
- [ ] USB debugging enabled (for physical device)
- [ ] Device visible in `adb devices`

### Build Verification
- [ ] No red errors in Android Studio
- [ ] All dependencies downloaded
- [ ] `google-services.json` file exists and is valid

## Quick Verification Commands

Run these in terminal to verify setup:

```bash
# Check if device is connected
adb devices

# Verify google-services.json exists
ls -la app/google-services.json

# Check Gradle can build
./gradlew tasks
```

## Ready to Build?

If all items above are checked ✅, you're ready to build!

**Next step:** Follow `BUILD_AND_INSTALL.md` for detailed build instructions.

