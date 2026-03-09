# Human Interaction Xposed Module - Build & Usage Guide

## Overview

This guide provides step-by-step instructions for building, installing, and using the Human Interaction Simulation Xposed module on Samsung Galaxy A12 (SM-A125U).

---

## Prerequisites

### Required Software

1. **Android Studio** or **Android SDK Command-line Tools**
   - Download: https://developer.android.com/studio
   - Minimum version: Arctic Fox (2020.3.1) or newer

2. **Java Development Kit (JDK) 8 or 11**
   - Download: https://adoptium.net/
   - Verify: `java -version`

3. **Android SDK** with components:
   - Android SDK Platform 34 (Android 14)
   - Android SDK Build-Tools 34.0.0+
   - Android SDK Platform-Tools

4. **Git**
   - Download: https://git-scm.com/

### Device Requirements

- Android device with **Android 10/11** (API 29-30)
- **Root access** (Magisk recommended)
- **LSPosed Framework** installed and activated
  - Download: https://github.com/LSPosed/LSPosed/releases
  - Or EdXposed (legacy): https://github.com/ElderDrivers/EdXposed

---

## Building the Module

### Step 1: Clone Repository

```bash
git clone <repository-url>
cd HumanInteractionXposed
```

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
2. Select **File → Open** and choose the `HumanInteractionXposed` directory
3. Wait for Gradle sync to complete
4. Select **Build → Build Bundle(s) / APK(s) → Build APK(s)**
5. Find APK in `app/build/outputs/apk/release/`

### Step 3: Locate Output APK

The compiled APK will be at:
```
app/build/outputs/apk/release/app-release.apk
```

Expected size: ~30-50 KB (small module with no resources)

### Build Verification

Check the APK was built successfully:
```bash
ls -lh app/build/outputs/apk/release/app-release.apk
```

---

## Installation

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

---

## Configuration

### Step 1: Enable Module in LSPosed

1. Open **LSPosed Manager** app
2. Navigate to **Modules** tab
3. Find **Human Interaction Simulator** in the list
4. Toggle the switch to **enable** the module
5. Green checkmark should appear

### Step 2: Configure Module Scope

1. Tap on **Human Interaction Simulator** module name
2. Select **Application List** or **Scope**
3. Search for and enable:
   - ✅ **TikTok** (`com.zhiliaoapp.musically`)
   - ✅ **TikTok Lite** (`com.ss.android.ugc.trill`)
   - ✅ **Douyin** (`com.ss.android.ugc.aweme`)
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

---

## Verification

### Check 1: LSPosed Logs

1. Open **LSPosed Manager**
2. Go to **Logs** tab
3. Filter by "HumanInteraction"
4. Launch target app (e.g., TikTok)
5. Look for initialization messages:

```
[HumanInteraction][Main] Human Interaction Module activated for: com.zhiliaoapp.musically
[HumanInteraction][Main] All hooks initialized successfully
[HumanInteraction][Main] === Human Interaction Module Configuration ===
[HumanInteraction][Main]   1. Mechanical Micro-Error Hook - Simulates fat-finger errors
[HumanInteraction][Main]   2. Sensor-Fusion Coherence Hook - Walking dynamics injection
[HumanInteraction][Main]   3. Inter-App Navigation Hook - Referral flow simulation
[HumanInteraction][Main]   4. Input Pressure Dynamics Hook - Touch pressure/area variation
[HumanInteraction][Main]   5. Asymmetric Latency Hook - Processing hesitation simulation
[HumanInteraction][Main] ============================================
```

### Check 2: Logcat (Advanced)

Monitor real-time logs via ADB:

```bash
adb logcat | grep "HumanInteraction"
```

Launch target app and verify hook activation messages.

### Check 3: Functional Verification

#### Mechanical Micro-Error Hook
- Tap buttons near edges
- Observe near-miss offsets in logs
- Verify correction swipes during scrolling

#### Sensor-Fusion Coherence Hook
- Use an app that reads accelerometer data
- Install a GPS spoofing app to simulate walking speed (1.0-2.0 m/s)
- Verify 1.8 Hz oscillations in accelerometer data
- Check gyroscope coherence

#### Inter-App Navigation Hook
- Monitor logs when deep-links are triggered
- Verify referral context in Intent extras
- Check back stack modifications

#### Input Pressure Dynamics Hook
- Use a MotionEvent debugging app
- Monitor pressure values during taps vs scrolling
- Verify touch major/minor variations

#### Asymmetric Latency Hook
- Launch new activities
- Observe input blocking periods in logs
- Verify delays correlate with load times

---

## Configuration & Tuning

### Preset Modes

The module provides three preset modes:

#### High-Fidelity Mode (Maximum Realism)
```java
// Via ADB shell (requires root)
adb shell su -c "am broadcast -a com.samsungcloak.xposed.SET_MODE --es mode high_fidelity"
```
Configuration:
- Error rate: 10%
- Hesitation probability: 35%
- Referral flow probability: 20%
- Pressure variation: 30%

#### Standard Mode (Balanced)
```java
adb shell su -c "am broadcast -a com.samsungcloak.xposed.SET_MODE --es mode standard"
```
Configuration:
- Error rate: 5%
- Hesitation probability: 20%
- Referral flow probability: 10%
- Pressure variation: 15%

#### Minimal Interference Mode (Subtle)
```java
adb shell su -c "am broadcast -a com.samsungcloak.xposed.SET_MODE --es mode minimal"
```
Configuration:
- Error rate: 2%
- Hesitation probability: 8%
- Referral flow probability: 3%
- Pressure variation: 5%

### Manual Configuration

Modify individual hook parameters via a small test app or ADB:

```java
// Example configuration code
HumanInteractionModule.ConfigurationAPI.setMechanicalErrorRate(0.08);
HumanInteractionModule.ConfigurationAPI.setSensorFusionWalkingState(true, 1.5);
HumanInteractionModule.ConfigurationAPI.setReferralFlowProbability(0.18);
HumanInteractionModule.ConfigurationAPI.setTouchPressureVariation(0.25);
HumanInteractionModule.ConfigurationAPI.setHesitationProbability(0.30);
```

---

## Troubleshooting

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
- Linux/Mac: `~/Android/Sdk` or `~/Library/Android/sdk`
- Windows: `C:\Users\YourName\AppData\Local\Android\Sdk`

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

### Issue: App crashes on launch

**Solutions:**
1. Check LSPosed logs for stack traces
2. Verify Android version compatibility (10/11)
3. Try disabling module, test if app works normally
4. Check for conflicting Xposed modules
5. Clear app data and cache

### Issue: Sensor oscillations not appearing

**Solutions:**
1. Verify GPS speed > 0.5 m/s (use GPS spoofing if needed)
2. Check that SensorFusionCoherenceHook is enabled
3. Verify hook initialization in logs
4. Test with a dedicated sensor monitoring app

---

## Development Build

For development with verbose logging:

```bash
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

Debug builds include:
- Additional logging
- Non-obfuscated code
- Easier debugging

To reduce log spam, edit each hook file and set:
```java
private static final boolean DEBUG = false;
```

---

## Performance Optimization

### Reducing Log Spam

Edit hook files to disable debug logging:
```java
private static final boolean DEBUG = false; // Change to false
```

Rebuild module for production use.

### Selective Hook Disabling

If certain hooks cause issues, modify `HumanInteractionModule.java`:
```java
// MechanicalMicroErrorHook.init(lpparam);    // Disabled
SensorFusionCoherenceHook.init(lpparam);     // Enabled
// InterAppNavigationHook.init(lpparam);     // Disabled
InputPressureDynamicsHook.init(lpparam);     // Enabled
AsymmetricLatencyHook.init(lpparam);         // Enabled
```

---

## Updating the Module

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

---

## Testing Guidelines

### Unit Testing Scenarios

1. **Mechanical Errors**
   - Tap 100 times near button edges
   - Count near-miss occurrences (should be ~8% with default config)
   - Verify correction swipes during scrolling

2. **Sensor Fusion**
   - Simulate walking at 1.5 m/s
   - Collect 10 seconds of accelerometer data
   - Verify 1.8 Hz frequency with spectral analysis
   - Check amplitude correlation with speed

3. **Navigation Context**
   - Trigger 50 deep-links
   - Verify referral extras in Intent
   - Check back stack modifications
   - Validate probability distributions

4. **Touch Dynamics**
   - Record 100 button taps
   - Verify pressure > 0.8 on DOWN
   - Verify pressure < 0.35 on UP
   - Compare touch size for taps vs scrolling

5. **Latency**
   - Launch 20 different activities
   - Measure post-load hesitation periods
   - Verify blocking delays correlate with complexity
   - Check input blocking is released

### Integration Testing

1. **End-to-End Flow**
   - Simulate full user session
   - Verify all hooks work together
   - Check telemetry synchronization
   - Validate performance impact

2. **Device Compatibility**
   - Test on Samsung Galaxy A12 (SM-A125U)
   - Verify Android 10/11 compatibility
   - Check for device-specific issues

3. **Target App Testing**
   - Test with TikTok, TikTok Lite, Douyin
   - Verify no app crashes
   - Check for detection evasion

---

## Security Notes

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

---

## Compliance & Ethics

This module is designed for:
- ✅ Research and testing purposes
- ✅ Hardware-in-the-loop validation
- ✅ Accessibility algorithm testing
- ✅ App robustness evaluation

**Not for:**
- ❌ Fraud or deception
- ❌ Violating terms of service
- ❌ Evading legitimate security measures

Ensure compliance with:
- Target app terms of service
- Local privacy regulations
- Institutional review board requirements (if applicable)

---

## Additional Resources

- **LSPosed Documentation**: https://github.com/LSPosed/LSPosed/wiki
- **Xposed Module Development**: https://github.com/rovo89/XposedBridge/wiki
- **Android Developer Guide**: https://developer.android.com/guide
- **Implementation Details**: See `HUMAN_INTERACTION_HOOKS_IMPLEMENTATION.md`

---

## Getting Help

If you encounter issues not covered here:

1. **Check LSPosed logs** for error messages
2. **Review implementation documentation**
3. **Verify device compatibility** (SM-A125U, Android 10/11)
4. **Check for conflicting modules**

---

**Success!** You should now have the Human Interaction Xposed module installed and working on your Samsung Galaxy A12.
