# How to Create APK File for Family Members

## Quick Method (Easiest)

### Step 1: Build Release APK in Android Studio

1. **Open Android Studio**
2. Go to **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**
3. Wait for build to complete (2-5 minutes)
4. When done, click **locate** in the notification at bottom right
5. The APK will be at: `app/build/outputs/apk/release/app-release.apk`

### Step 2: Share the APK

**Option A: Email**
- Attach `app-release.apk` to an email
- Send to family members
- They download and install

**Option B: Cloud Storage**
- Upload to Google Drive, Dropbox, or iCloud
- Share the link with family
- They download and install

**Option C: Messaging Apps**
- Send via WhatsApp, Telegram, etc.
- They download and install

### Step 3: Family Members Install

1. On their Android phone, open the APK file
2. If prompted, allow "Install from unknown sources"
3. Tap **Install**
4. Done! App is installed

---

## Command Line Method

### Build Release APK

```bash
cd /Users/sergeyromanov/Desktop/FamilyCalls
./gradlew assembleRelease
```

The APK will be at: `app/build/outputs/apk/release/app-release.apk`

---

## Important Notes

### Before Building Release APK

You may want to create a **signing key** for security (optional but recommended):

1. **Generate a keystore:**
   ```bash
   keytool -genkey -v -keystore familycalls-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias familycalls
   ```
   - Remember the password!
   - Store the keystore file securely

2. **Add to `app/build.gradle`:**
   ```gradle
   android {
       signingConfigs {
           release {
               storeFile file('familycalls-key.jks')
               storePassword 'your-password'
               keyAlias 'familycalls'
               keyPassword 'your-password'
           }
       }
       buildTypes {
           release {
               signingConfig signingConfigs.release
               // ... other settings
           }
       }
   }
   ```

**Note:** For a family app, unsigned APK works fine, but signed is more secure.

---

## APK File Size

- Typically 15-30 MB
- Depends on included libraries
- Can be compressed for sharing

---

## Troubleshooting

### "Install blocked" on family member's phone

**Solution:**
1. Go to **Settings** → **Security**
2. Enable **"Install from unknown sources"** or **"Install unknown apps"**
3. Try installing again

### "App not installed" error

**Possible causes:**
- APK is corrupted (re-download)
- Not enough storage space
- Incompatible Android version (needs Android 7.0+)

### APK file is very large

**Solution:**
- Use **Build Bundle** instead (smaller file)
- Or enable ProGuard to reduce size (advanced)

---

## Alternative: Build App Bundle (Smaller)

Instead of APK, you can build an **AAB (Android App Bundle)**:

1. **Build** → **Generate Signed Bundle / APK**
2. Select **Android App Bundle**
3. Smaller file size
4. Family members can install via Google Play (if you upload it) or convert AAB to APK

---

## Quick Summary

✅ **Easiest way:**
1. Build → Build Bundle(s) / APK(s) → Build APK(s)
2. Find APK in `app/build/outputs/apk/release/`
3. Share via email/cloud/messaging
4. Family installs on their phones

✅ **That's it!** Your family can install the app directly without Play Store.

---

## Security Note

- Only share APK with trusted family members
- Don't post APK publicly
- Consider signing the APK for extra security

