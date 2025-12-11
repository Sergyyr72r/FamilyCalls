#!/bin/bash

# Script to clear Gradle cache and fix build issues

echo "Clearing Gradle cache and build files..."

cd /Users/sergeyromanov/Desktop/FamilyCalls

# Stop Gradle daemon
./gradlew --stop 2>/dev/null || echo "Gradle daemon stopped or not running"

# Remove local build files
rm -rf .gradle/
rm -rf build/
rm -rf app/build/
rm -rf .idea/

# Remove global Gradle cache (optional - uncomment if needed)
# rm -rf ~/.gradle/caches/

echo "✅ Cache cleared!"
echo ""
echo "Next steps:"
echo "1. Open Android Studio"
echo "2. File → Invalidate Caches → Invalidate and Restart"
echo "3. After restart, File → Sync Project with Gradle Files"
echo "4. Try building again"

