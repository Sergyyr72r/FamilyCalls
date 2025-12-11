# How to Change the App Icon in Android Studio

## Method 1: Using Image Asset Studio (Recommended)

### Step 1: Open Image Asset Studio
1. In Android Studio, open your project
2. In the **Project** panel (left side), right-click on the `app` folder
3. Navigate to: **New → Image Asset**
   - Alternatively: **File → New → Image Asset**

### Step 2: Configure Icon Settings
1. The **Asset Studio** window will open
2. At the top, ensure **Icon Type** is set to **"Launcher Icons (Adaptive and Legacy)"**
3. Under **Foreground Layer**:
   - **Source Asset**: Choose your icon image
     - Click the **Path** folder icon to browse for your image file
     - Select your icon image (PNG, JPG, or SVG recommended)
     - The image should be square (e.g., 1024x1024px for best quality)

### Step 3: Adjust Icon Appearance
1. **Foreground Layer** settings:
   - **Scaling**: Adjust if needed (usually leave at default)
   - **Shape**: Choose "None" if you want your exact icon shape
   - **Background Color**: Set if your icon has transparency
   - **Foreground Color**: Adjust icon color if needed

2. **Background Layer** settings:
   - **Color**: Choose background color (or use an image)
   - This is the background shown behind your icon on some Android versions

### Step 4: Preview and Generate
1. Preview your icon in the right panel
   - You'll see how it looks on different Android versions
   - Check both **Adaptive** (round/square) and **Legacy** (older versions) previews

2. Click **Next**

### Step 5: Review Generated Files
1. Android Studio will show a summary of files to be created/modified
2. Review the paths:
   - `res/mipmap-*/ic_launcher.png` (various sizes)
   - `res/mipmap-anydpi-v26/ic_launcher.xml` (adaptive icon)
   - `res/mipmap-anydpi-v26/ic_launcher_round.xml` (round variant)

3. Click **Finish**

### Step 6: Verify Changes
1. Check that files were created in:
   - `app/src/main/res/mipmap-*dpi/` (different density folders)
   - `app/src/main/res/mipmap-anydpi-v26/`

2. Rebuild your project: **Build → Rebuild Project**

3. Install the app to see the new icon on your device

---

## Method 2: Manual Replacement (Advanced)

If you prefer to manually replace icon files:

### Step 1: Prepare Icon Images
You need multiple sizes for different screen densities:
- **mdpi**: 48x48px
- **hdpi**: 72x72px
- **xhdpi**: 96x96px
- **xxhdpi**: 144x144px
- **xxxhdpi**: 192x192px

### Step 2: Create Adaptive Icon XML
1. Create/edit: `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
2. Define background and foreground drawables

### Step 3: Place Icon Files
1. Create/update folders in `app/src/main/res/`:
   - `mipmap-mdpi/ic_launcher.png`
   - `mipmap-hdpi/ic_launcher.png`
   - `mipmap-xhdpi/ic_launcher.png`
   - `mipmap-xxhdpi/ic_launcher.png`
   - `mipmap-xxxhdpi/ic_launcher.png`

2. Also create `ic_launcher_round.png` in each folder for round icons

### Step 4: Update AndroidManifest.xml
Ensure your `AndroidManifest.xml` references the icon:
```xml
<application
    android:icon="@mipmap/ic_launcher"
    android:roundIcon="@mipmap/ic_launcher_round"
    ...>
```

---

## Tips for Best Results

1. **Image Requirements**:
   - Square format (1:1 aspect ratio)
   - Minimum 512x512px (1024x1024px recommended)
   - PNG format with transparency supported
   - High quality, not pixelated

2. **Design Guidelines**:
   - Keep important content in the center (safe zone)
   - Outer 25% of the icon may be cropped on some devices
   - Test on different Android versions if possible

3. **After Changing Icon**:
   - Clean and rebuild: **Build → Clean Project**, then **Build → Rebuild Project**
   - Uninstall the old app from your device before installing the new version
   - Some launchers cache icons - you may need to clear app cache or restart device

4. **Testing**:
   - Install on a device/emulator
   - Check how the icon looks in the app drawer
   - Test on different Android versions if possible (adaptive icons behave differently)

---

## Troubleshooting

**Icon doesn't change after installation:**
- Uninstall the app completely and reinstall
- Clear launcher cache (device settings → Apps → Launcher → Clear cache)
- Restart device

**Icon looks blurry:**
- Ensure you're providing high-resolution images
- Use Image Asset Studio to generate all sizes automatically

**Build errors:**
- Check that XML files are valid
- Ensure all referenced drawables exist
- Clean and rebuild project
