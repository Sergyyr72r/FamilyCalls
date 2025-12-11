WebRTC Library Setup
===================

To enable video calls:

1. Download libwebrtc.aar from:
   https://github.com/webrtc-mirror/webrtc/releases

2. Place the downloaded libwebrtc.aar file in this folder (app/libs/)

3. In app/build.gradle, uncomment:
   implementation files('libs/libwebrtc.aar')

4. In VideoCallActivity.kt, uncomment:
   import org.webrtc.*
   (and all WebRTC code)

5. Rebuild the project

Then video calls will work!

