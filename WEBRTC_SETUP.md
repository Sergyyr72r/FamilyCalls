# WebRTC Setup Guide

## Current Status

The WebRTC dependency has been **temporarily commented out** so the app can build. Video calls will show a "Setup Required" message.

## Why WebRTC is Commented Out

The WebRTC library `org.webrtc:google-webrtc:1.0.32006` is **not available** in standard Maven repositories (Google, Maven Central). You need to either:

1. Build WebRTC from source
2. Use a pre-built AAR file
3. Add a custom repository

## Option 1: Use Pre-built WebRTC AAR (Easiest)

1. Download WebRTC AAR from: https://github.com/webrtc-mirror/webrtc/releases
2. Place the AAR file in `app/libs/` folder
3. In `app/build.gradle`, add:

```gradle
dependencies {
    // ... other dependencies
    
    // WebRTC from local AAR
    implementation files('libs/libwebrtc.aar')
}
```

## Option 2: Build WebRTC from Source

1. Follow instructions at: https://webrtc.github.io/webrtc-org/native-code/android/
2. Build the AAR file
3. Add it to `app/libs/` as in Option 1

## Option 3: Use Alternative WebRTC Library

Some alternatives available in Maven:
- Use a different WebRTC wrapper library
- Use a third-party WebRTC service (Twilio, Agora, etc.)

## For Now

The app will build and work for:
- ✅ Messaging (text, images, videos)
- ✅ Contact management
- ❌ Video calls (shows "WebRTC setup required" message)

## Re-enabling Video Calls

Once you have WebRTC set up:

1. Uncomment in `app/build.gradle`:
```gradle
implementation 'org.webrtc:google-webrtc:1.0.32006'
// OR
implementation files('libs/libwebrtc.aar')
```

2. Uncomment in `VideoCallActivity.kt`:
```kotlin
import org.webrtc.*
```

3. Rebuild the project

---

**Note:** For a family app, you might want to use a simpler video calling solution like integrating with a service provider, or building WebRTC from source for full control.

