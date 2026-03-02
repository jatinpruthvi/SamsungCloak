# Samsung Cloak - Build & Installation Guide

Complete step-by-step instructions for building and installing the Samsung Cloak Xposed module.

## 📋 Prerequisites

### Required Software

1. **Android Studio** (or Android SDK Command-line Tools)
   - Download: https://developer.android.com/studio
   - Minimum version: Arctic Fox (2020.3.1) or newer

2. **Java Development Kit (JDK) 8 or 11**
   - Download: https://adoptium.net/
   - Verify: `java -version`

3. **Android SDK** with the following components:
   - Android SDK Platform 34 (Android 14)
   - Android SDK Build-Tools 34.0.0+
   - Android SDK Platform-Tools

4. **Git** (for cloning repository)
   - Download: https://git-scm.com/

### Device Requirements

- Android device with **Android 11-14** (API 30-34)
- **Root access** (Magisk recommended)
- **LSPosed Framework** installed and activated
  - Download: https://github.com/LSPosed/LSPosed/releases
  - Or **EdXposed** (legacy): https://github.com/ElderDrivers/EdXposed

## 🔨 Building from Source

### Step 1: Clone Repository

```bash
git clone https://github.com/yourusername/SamsungCloak.git
cd SamsungCloak
```

### Step 1.5: Review Project Structure (Optional)

To understand the complete project file structure and organization:

```bash
./setup_project.sh
```

This script documents all 83 files in the project, their locations, and the complete package organization. Useful for:
- Understanding the codebase layout
- Setting up a new project from scratch
- Verifying all files are in their correct locations

### Step 2: Build with Gradle

#### Option A: Using Gradle Wrapper (Recommended)

**Linux/macOS:**
```bash
./gradlew assembleRelease
```

**Windows:**
```cmd
gradlew.bat assembleRelease
```

#### Option B: Using Android Studio

1. Open Android Studio
2. Select **File → Open** and choose the `SamsungCloak` directory
3. Wait for Gradle sync to complete
4. Select **Build → Build Bundle(s) / APK(s) → Build APK(s)**
5. Find APK in `app/build/outputs/apk/release/`

### Step 3: Locate Output APK

The compiled APK will be at:
```
app/build/outputs/apk/release/app-release.apk
```

### Build Verification

Check the APK was built successfully:
```bash
ls -lh app/build/outputs/apk/release/app-release.apk
```

Expected size: ~50-100 KB (small module with no resources)

## 📱 Installation

### Method 1: ADB Installation (Recommended)

1. **Enable USB Debugging** on your device:
   - Settings → About Phone → Tap "Build Number" 7 times
   - Settings → Developer Options → Enable "USB Debugging"

2. **Connect device** via USB

3. **Verify connection**:
   ```bash
   adb devices
   ```
   Should show: `XXXXXXXXX device`

4. **Install APK**:
   ```bash
   adb install -r app/build/outputs/apk/release/app-release.apk
   ```

### Method 2: Manual Installation

1. Copy APK to device:
   ```bash
   adb push app/build/outputs/apk/release/app-release.apk /sdcard/Download/
   ```

2. On device, open **File Manager**
3. Navigate to **Downloads** folder
4. Tap **app-release.apk**
5. Tap **Install** (may need to allow "Install from Unknown Sources")

### Method 3: Direct USB Transfer

1. Connect device to PC
2. Copy `app-release.apk` to device storage
3. Use file manager to install

## ⚙️ Configuration

### Step 1: Enable Module in LSPosed

1. Open **LSPosed Manager** app
2. Navigate to **Modules** tab
3. Find **Samsung Cloak** in the list
4. Toggle the switch to **enable** the module
5. Green checkmark should appear

### Step 2: Configure Module Scope

1. Tap on **Samsung Cloak** module name
2. Select **Application List** or **Scope**
3. Search for and enable:
   - ✅ **TikTok** (`com.zhiliaoapp.musically`)
   - ✅ **TikTok Lite** (`com.ss.android.ugc.trill`) - if installed
   - ✅ **Douyin** (`com.ss.android.ugc.aweme`) - if installed
4. Tap **Save** or back button

### Step 3: Force Stop Target Apps

**Option A: Via LSPosed Manager**
- Long-press each target app in scope list
- Select "Force Stop"

**Option B: Via ADB**
```bash
adb shell am force-stop com.zhiliaoapp.musically
adb shell am force-stop com.ss.android.ugc.trill
adb shell am force-stop com.ss.android.ugc.aweme
```

**Option C: Via Settings**
- Settings → Apps → TikTok → Force Stop

### Step 4: Reboot Device

**Highly recommended** for full activation:
```bash
adb reboot
```

Or use device power menu.

## ✅ Verification

### Check 1: LSPosed Logs

1. Open **LSPosed Manager**
2. Go to **Logs** tab
3. Filter by "SamsungCloak"
4. Launch TikTok
5. Look for initialization messages:

```
[SamsungCloak][Main] Samsung Cloak activated for: com.zhiliaoapp.musically
[SamsungCloak][Main] Target: Samsung Galaxy A12 (SM-A125U)
[SamsungCloak][Property] Property hooks initialized with 48 entries
[SamsungCloak][Sensor] Sensor hooks initialized
[SamsungCloak][Environment] Environment hooks initialized
[SamsungCloak][AntiDetection] Anti-detection hooks initialized
[SamsungCloak][Main] All hooks initialized successfully
```

### Check 2: Logcat (Advanced)

Monitor real-time logs via ADB:

```bash
adb logcat | grep -E "SamsungCloak|Build|SystemProperties"
```

Launch TikTok and verify spoofed values appear.

### Check 3: Device Info Apps

Install a device info app (e.g., "Device Info HW") and verify:
- **Model**: SM-A125U
- **Manufacturer**: samsung
- **Device**: a12
- **Android Version**: 11
- **Build ID**: RP1A.200720.012

**Note**: Info apps may show different values than what TikTok sees, depending on whether they're in the module scope.

## 🐛 Troubleshooting

### Issue: Module not appearing in LSPosed

**Solutions:**
1. Reinstall APK
2. Clear LSPosed Manager cache:
   ```bash
   adb shell pm clear org.lsposed.manager
   ```
3. Reboot device
4. Check module has `xposed_init` in assets folder

### Issue: Module enabled but not working

**Solutions:**
1. Verify target app is in scope list
2. Force stop target app completely
3. Clear target app data:
   ```bash
   adb shell pm clear com.zhiliaoapp.musically
   ```
4. Reboot device
5. Check LSPosed logs for errors

### Issue: Build fails with "SDK not found"

**Solution:**
Create `local.properties` file in project root:
```properties
sdk.dir=/path/to/Android/Sdk
```

**Find SDK path:**
- **Linux/Mac**: `~/Android/Sdk` or `~/Library/Android/sdk`
- **Windows**: `C:\Users\YourName\AppData\Local\Android\Sdk`

### Issue: "API not found" during build

**Solution:**
Install Android SDK Platform 34:
```bash
sdkmanager "platforms;android-34"
sdkmanager "build-tools;34.0.0"
```

### Issue: Gradle sync fails

**Solutions:**
1. Update Gradle wrapper:
   ```bash
   ./gradlew wrapper --gradle-version=8.0
   ```
2. Clean project:
   ```bash
   ./gradlew clean
   ```
3. Invalidate caches (Android Studio):
   - File → Invalidate Caches → Invalidate and Restart

### Issue: "Unsupported class file major version"

**Cause**: Java version mismatch

**Solution:**
Check Java version used by Gradle:
```bash
./gradlew --version
```

Set JDK in `gradle.properties`:
```properties
org.gradle.java.home=/path/to/jdk11
```

### Issue: App crashes on launch

**Solutions:**
1. Check LSPosed logs for stack traces
2. Verify Android version compatibility (11-14)
3. Try disabling module, test if app works normally
4. Check for conflicting Xposed modules
5. Clear app data and cache

## 📊 Performance Optimization

### Reducing Log Spam

Edit `HookUtils.java` to disable debug logging:
```java
private static final boolean DEBUG = false; // Change to false
```

Rebuild module for production use.

### Selective Hook Disabling

If certain hooks cause issues, comment out initialization in `MainHook.java`:
```java
// PropertyHook.init(lpparam);    // Disabled
SensorHook.init(lpparam);         // Enabled
// EnvironmentHook.init(lpparam); // Disabled
```

## 🔄 Updating the Module

### Method 1: Reinstall APK

1. Build new version
2. Install over existing:
   ```bash
   adb install -r app/build/outputs/apk/release/app-release.apk
   ```
3. No need to disable/re-enable in LSPosed
4. Force stop target apps
5. Reboot (optional but recommended)

### Method 2: Clean Install

1. Disable module in LSPosed
2. Uninstall old version:
   ```bash
   adb uninstall com.samsungcloak.xposed
   ```
3. Install new version
4. Enable module and configure scope
5. Reboot

## 🧪 Development Build

For development with verbose logging:

```bash
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

Debug builds include:
- Additional logging
- Non-obfuscated code
- Easier debugging

## 📦 Release Build Optimization

For production release builds:

1. **Enable ProGuard** in `app/build.gradle`:
   ```gradle
   buildTypes {
       release {
           minifyEnabled true
           shrinkResources true
           proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 
                         'proguard-rules.pro'
       }
   }
   ```

2. **Sign APK** (optional):
   ```bash
   jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
             -keystore my-release-key.jks \
             app/build/outputs/apk/release/app-release-unsigned.apk \
             alias_name
   ```

3. **Zipalign**:
   ```bash
   zipalign -v 4 app-release-unsigned.apk app-release.apk
   ```

## 🔐 Security Notes

### Module Signing

For distribution, sign with your own keystore:

1. Generate keystore:
   ```bash
   keytool -genkey -v -keystore my-release-key.jks \
           -keyalg RSA -keysize 2048 -validity 10000 \
           -alias my-key-alias
   ```

2. Configure in `app/build.gradle`:
   ```gradle
   android {
       signingConfigs {
           release {
               storeFile file("my-release-key.jks")
               storePassword "password"
               keyAlias "my-key-alias"
               keyPassword "password"
           }
       }
   }
   ```

### Verifying APK Integrity

Check APK signature:
```bash
apksigner verify --verbose app-release.apk
```

## 📖 Additional Resources

- **LSPosed Documentation**: https://github.com/LSPosed/LSPosed/wiki
- **Xposed Module Development**: https://github.com/rovo89/XposedBridge/wiki
- **Android Developer Guide**: https://developer.android.com/guide

## 🆘 Getting Help

If you encounter issues not covered here:

1. **Check LSPosed logs** for error messages
2. **Search GitHub Issues** for similar problems
3. **Create new issue** with:
   - Device model and Android version
   - LSPosed version
   - Complete error logs
   - Steps to reproduce

---

**Success!** You should now have Samsung Cloak installed and working. Verify by checking LSPosed logs when launching TikTok.
