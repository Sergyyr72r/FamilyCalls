# Fix Java 21 Compatibility Issue

## Problem

You're using **Java 21.0.8**, but **Gradle 8.0** only supports up to **Java 19**.

The error `Unsupported class file major version 65` means:
- Class file version 65 = Java 21
- Gradle 8.0 can't read Java 21 compiled classes

## Solution Applied

✅ **Upgraded Gradle from 8.0 → 8.5**
- Gradle 8.5 supports Java 21
- This is the recommended fix from the error message

## Next Steps

### Step 1: Clear Gradle Cache

The cache contains files compiled with Java 21 that Gradle 8.0 couldn't read. Clear it:

**In Terminal:**
```bash
cd /Users/sergeyromanov/Desktop/FamilyCalls
rm -rf ~/.gradle/caches/8.0/
rm -rf .gradle/
rm -rf build/
rm -rf app/build/
```

**Or in Android Studio:**
- **File** → **Invalidate Caches** → **Invalidate and Restart**

### Step 2: Sync Gradle

After clearing cache:
1. **File** → **Sync Project with Gradle Files**
2. Gradle 8.5 will be downloaded automatically
3. Wait for sync to complete

### Step 3: Build

The project should now build successfully!

## What Changed

**gradle-wrapper.properties:**
- Gradle version: `8.0` → `8.5`
- Now supports Java 21 ✅

## Compatibility

- ✅ Gradle 8.5 supports Java 21
- ✅ AGP 8.0.2 works with Gradle 8.5
- ✅ All dependencies compatible

## Alternative: Use Java 17/19

If you prefer to keep Gradle 8.0, you can configure Android Studio to use Java 17:

1. **File** → **Project Structure** → **SDK Location**
2. Set **JDK location** to Java 17 or 19
3. Or install Java 17 and set it as project JDK

But upgrading Gradle to 8.5 is the recommended solution!

---

**After clearing cache and syncing, the build should work!** 🎉

