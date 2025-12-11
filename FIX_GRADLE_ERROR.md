# Fix Gradle Dependency Error

## Error Explanation

The error `'org.gradle.api.artifacts.Dependency org.gradle.api.artifacts.dsl.DependencyHandler.module(java.lang.Object)'` typically occurs due to:

1. **Plugin conflicts** - Mixing buildscript and plugins DSL incorrectly
2. **Corrupted Gradle cache** - Old cached files causing conflicts
3. **Version incompatibilities** - Plugin versions not compatible with Gradle version

## What We Fixed

✅ Removed Firebase App Distribution plugin (was causing conflicts)
✅ Cleaned up buildscript configuration
✅ Ensured proper plugin application

## Step-by-Step Fix

### Step 1: Clean Gradle Cache

In Android Studio Terminal or your system terminal, run:

```bash
cd /Users/sergeyromanov/Desktop/FamilyCalls

# Stop any running Gradle daemons
./gradlew --stop

# Clean the project
./gradlew clean

# If the above doesn't work, manually clear cache:
rm -rf ~/.gradle/caches/
rm -rf .gradle/
rm -rf build/
rm -rf app/build/
```

**Or in Android Studio:**
- **File** → **Invalidate Caches** → **Invalidate and Restart**

### Step 2: Verify Build Files

Make sure your files match these:

**build.gradle** (root):
```gradle
// Top-level build file
buildscript {
    ext.kotlin_version = "1.9.0"
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath "com.android.tools.build:gradle:8.1.0"
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"
        classpath 'com.google.gms:google-services:4.4.4'
    }
}

task clean(type: Delete) {
    delete rootProject.buildDir
}
```

**app/build.gradle** (should NOT have Firebase App Distribution):
```gradle
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
    id 'com.google.gms.google-services'
}
```

### Step 3: Sync Gradle

1. In Android Studio: **File** → **Sync Project with Gradle Files**
2. Wait for sync to complete
3. Check for any errors in the **Build** tab

### Step 4: If Error Persists

Try these additional steps:

**Option A: Update Gradle Wrapper**
```bash
./gradlew wrapper --gradle-version=8.0
```

**Option B: Check Gradle Version**
- In Android Studio: **File** → **Project Structure** → **Project**
- Ensure Gradle version is compatible (7.5+ for AGP 8.1.0)

**Option C: Re-import Project**
1. Close Android Studio
2. Delete `.idea` folder (backup first if needed)
3. Delete `.gradle` folder
4. Reopen project in Android Studio
5. Let it re-index

## Verification

After fixing, you should be able to:
- ✅ Sync Gradle without errors
- ✅ Build the project successfully
- ✅ Run the app on device/emulator

## If Still Having Issues

1. Check the full error in **Build** tab → **Toggle View** → **Build Output**
2. Look for the specific line causing the error
3. Share the full stacktrace for more targeted help

## Summary

The main fix was:
- ✅ Removed Firebase App Distribution plugin (not essential)
- ✅ Cleaned buildscript configuration
- ✅ Need to clear Gradle cache

Try Step 1 (cleaning cache) first - this resolves the issue in most cases!

