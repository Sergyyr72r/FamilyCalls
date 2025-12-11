#!/bin/bash

# Family Calls - Build Commands Script
# This script provides quick commands to build and install the app

echo "Family Calls - Build Commands"
echo "=============================="
echo ""
echo "Choose an option:"
echo "1. Build Debug APK"
echo "2. Build Release APK"
echo "3. Install Debug APK on connected device"
echo "4. Build and Install Debug (one command)"
echo "5. Check connected devices"
echo "6. View app logs"
echo "7. Clean build"
echo ""
read -p "Enter option (1-7): " option

case $option in
    1)
        echo "Building Debug APK..."
        ./gradlew assembleDebug
        echo "APK location: app/build/outputs/apk/debug/app-debug.apk"
        ;;
    2)
        echo "Building Release APK..."
        ./gradlew assembleRelease
        echo "APK location: app/build/outputs/apk/release/app-release.apk"
        ;;
    3)
        echo "Installing Debug APK on device..."
        adb install -r app/build/outputs/apk/debug/app-debug.apk
        ;;
    4)
        echo "Building and Installing Debug APK..."
        ./gradlew assembleDebug
        adb install -r app/build/outputs/apk/debug/app-debug.apk
        echo "Done! App should be installed on your device."
        ;;
    5)
        echo "Connected devices:"
        adb devices
        ;;
    6)
        echo "Viewing app logs (Press Ctrl+C to stop)..."
        adb logcat | grep -i "familycalls\|firebase"
        ;;
    7)
        echo "Cleaning build..."
        ./gradlew clean
        echo "Build cleaned!"
        ;;
    *)
        echo "Invalid option"
        ;;
esac

