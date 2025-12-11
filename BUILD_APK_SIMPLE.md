# Build APK - Simple Instructions

## ✅ Easiest Method: Use Android Studio

You don't need `gradlew` - just use Android Studio's menu!

### Step 1: Build the APK

1. **Open Android Studio**
2. Make sure your project is open
3. Click **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**
4. Wait for build to complete (2-5 minutes)
5. When done, you'll see a notification at the bottom right

### Step 2: Find Your APK

1. Click **"locate"** in the notification, OR
2. Navigate to: `app/build/outputs/apk/release/app-release.apk`
   - (If release build) or `app/build/outputs/apk/debug/app-debug.apk` (if debug build)

### Step 3: Share with Family

- **Email:** Attach the APK file
- **Cloud Storage:** Upload to Google Drive/Dropbox, share link
- **Messaging:** Send via WhatsApp, Telegram, etc.

### Step 4: Family Members Install

1. Download APK on their Android phone
2. Open the APK file
3. Allow "Install from unknown sources" if prompted
4. Tap **Install**
5. Done! ✅

---

## Alternative: Create Gradle Wrapper (If You Want Command Line)

If you really want to use `./gradlew`, you need to create the wrapper first:

### Option 1: Use Android Studio

1. **File** → **Settings** (or **Preferences** on Mac)
2. **Build, Execution, Deployment** → **Build Tools** → **Gradle**
3. Check **"Use Gradle from: 'gradle-wrapper.properties' file"**
4. Click **Apply**
5. Android Studio will generate the wrapper automatically

### Option 2: Install Gradle and Create Wrapper

```bash
# Install Gradle (if not installed)
brew install gradle

# Create wrapper
cd /Users/sergeyromanov/Desktop/FamilyCalls
gradle wrapper

# Now you can use
./gradlew assembleRelease
```

---

## Quick Summary

**Just use Android Studio:**
- Build → Build Bundle(s) / APK(s) → Build APK(s)
- Find APK in `app/build/outputs/apk/release/`
- Share with family!

**No need for command line!** 🎉

