# Build APK - Alternative Methods

## Method 1: Build Menu (Different Location)

The menu might be in a different location depending on your Android Studio version:

### Try These Locations:

1. **Build** → **Build APK(s)**
2. **Build** → **Make Project** (then find APK in build folder)
3. **Build** → **Rebuild Project**
4. Right-click on **app** folder → **Build** → **Build APK(s)**

## Method 2: Terminal in Android Studio

1. Open **Terminal** at the bottom of Android Studio
2. Run:
   ```bash
   ./gradlew assembleRelease
   ```
   OR if that doesn't work:
   ```bash
   ./gradlew assembleDebug
   ```

3. Find APK at: `app/build/outputs/apk/release/app-release.apk` or `app/build/outputs/apk/debug/app-debug.apk`

## Method 3: Gradle Panel

1. Open **Gradle** panel (usually on the right side)
2. Expand: **FamilyCalls** → **app** → **Tasks** → **build**
3. Double-click **assembleRelease** or **assembleDebug**
4. Wait for build to complete
5. Find APK in: `app/build/outputs/apk/`

## Method 4: Create Gradle Wrapper First

If `./gradlew` doesn't work, create the wrapper:

1. In Android Studio Terminal, run:
   ```bash
   gradle wrapper
   ```
   (This requires Gradle to be installed)

2. Then run:
   ```bash
   ./gradlew assembleRelease
   ```

## Method 5: Use Android Studio's Run Button

1. Make sure your device/emulator is connected
2. Click the green **Run** button (▶️)
3. This creates a debug APK automatically
4. Find it at: `app/build/outputs/apk/debug/app-debug.apk`

## Quick Check: Where is Your APK?

After building (any method), check:
- `app/build/outputs/apk/debug/app-debug.apk` (debug build)
- `app/build/outputs/apk/release/app-release.apk` (release build)

## Still Can't Find It?

1. **File** → **Project Structure** → Check your build configuration
2. Look in Android Studio's **Build** tab at the bottom for any errors
3. Try **Build** → **Clean Project**, then **Build** → **Rebuild Project**

---

**Which Android Studio version are you using?** The menu structure varies by version.

