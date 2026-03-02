# Samsung Cloak - Validation Checklist

Use this checklist to verify that Samsung Cloak is working correctly on your device.

## 📋 Pre-Installation Validation

### Environment Checks
- [ ] Device is rooted (Magisk/KernelSU)
- [ ] LSPosed Framework is installed and active
- [ ] LSPosed version is 1.8.3 or newer
- [ ] Android version is 11-14 (API 30-34)
- [ ] Target app (TikTok) is installed

### Build Verification
- [ ] APK built successfully without errors
- [ ] APK size is reasonable (50-150 KB)
- [ ] APK signature is valid
- [ ] All required files are present in APK

## 🔧 Installation Validation

### Module Installation
- [ ] APK installed successfully
- [ ] Module appears in LSPosed Manager
- [ ] Module can be enabled (toggle works)
- [ ] Module metadata displays correctly

### Scope Configuration
- [ ] TikTok appears in application list
- [ ] Module scope includes TikTok
- [ ] Scope can be saved
- [ ] TikTok shows as "hooked" after reboot

### Post-Installation
- [ ] No installation errors in logs
- [ ] Device still boots normally
- [ ] LSPosed Manager still functional
- [ ] Other apps still work normally

## ✅ Functional Validation

### 1. Build Field Spoofing

Check these values in a device info app or programmatically:

```java
// Expected values:
Build.MANUFACTURER = "samsung"
Build.BRAND = "samsung"
Build.MODEL = "SM-A125U"
Build.DEVICE = "a12"
Build.PRODUCT = "a12qltesq"
Build.HARDWARE = "mt6765"
Build.VERSION.RELEASE = "11"
Build.VERSION.SDK_INT = 30
Build.FINGERPRINT = "samsung/a12qltesq/a12:11/RP1A.200720.012/A125USQU3CVI1:user/release-keys"
```

**Validation Steps**:
- [ ] Install "Device Info HW" app with module scope
- [ ] Verify Model shows "SM-A125U"
- [ ] Verify Manufacturer shows "samsung"
- [ ] Verify Android version shows "11"
- [ ] Verify Build fingerprint matches

### 2. System Properties Spoofing

Check via ADB or programmatically:

```bash
# Run TikTok, then via ADB:
adb shell "getprop ro.product.model"
# Should return: SM-A125U (if TikTok is in foreground)
```

**Validation Steps**:
- [ ] ro.product.model = "SM-A125U"
- [ ] ro.product.manufacturer = "samsung"
- [ ] ro.product.brand = "samsung"
- [ ] ro.hardware = "mt6765"
- [ ] ro.build.fingerprint matches expected value

### 3. Sensor Simulation

Monitor sensor data while TikTok is active:

**Validation Steps**:
- [ ] Accelerometer values are NOT constant (vary over time)
- [ ] Values show organic patterns (not pure random jumps)
- [ ] Y-axis accelerometer ≈ 9.81 m/s² (gravity)
- [ ] Gyroscope shows micro-movements
- [ ] Light sensor reports realistic lux values (not 0 or max)
- [ ] Magnetic field values within -60 to 60 µT range
- [ ] Sensor values correlate over time (drift patterns visible)

**Test App**: Use "Sensor Kinetics" or "Sensors Multitool" in module scope

### 4. Battery Behavior

Monitor battery reporting over time:

**Validation Steps** (15 minute test):
- [ ] Battery level starts at 72% or similar realistic value
- [ ] Battery level DECREASES over time (not static)
- [ ] Drain rate is approximately 1% per 3 minutes
- [ ] Battery status shows "Discharging" (not charging)
- [ ] Temperature reports 28-31°C range
- [ ] Voltage reports 3850-4050 mV range
- [ ] Technology shows "Li-ion"

**Test Method**: 
```bash
# Check battery level repeatedly
adb shell dumpsys battery | grep level
# Wait 3 minutes, check again - should decrease by ~1%
```

### 5. Memory Configuration

Check memory reporting:

**Validation Steps**:
- [ ] Total RAM reported as 3 GB (3,221,225,472 bytes)
- [ ] Available RAM varies (30-55% of total)
- [ ] Max heap size is 256 MB (268,435,456 bytes)
- [ ] Low memory flag is false

**Test Method**:
```bash
adb shell dumpsys meminfo | grep "Total RAM"
```

### 6. Display Metrics

Verify screen specifications:

**Validation Steps**:
- [ ] Width = 720 pixels
- [ ] Height = 1600 pixels
- [ ] Density = 2.0 (320 dpi)
- [ ] DPI = 320
- [ ] xdpi = 270.0
- [ ] ydpi = 270.0

**Test App**: Check in TikTok's debug info or device info app

### 7. Input Device

Verify touchscreen identity:

**Validation Steps**:
- [ ] Input device name = "sec_touchscreen"
- [ ] Sources = 0x00001002 (touchscreen)
- [ ] Vendor ID = 0
- [ ] Product ID = 0
- [ ] Descriptor = "sec_touchscreen_0"

### 8. Network/Carrier Info

Check carrier information:

**Validation Steps**:
- [ ] Network operator name = "T-Mobile"
- [ ] SIM operator name = "T-Mobile"
- [ ] Network operator = "310260"
- [ ] Country ISO = "us"
- [ ] Phone type = 1 (GSM)
- [ ] Network type = 13 (LTE)

### 9. Anti-Detection Measures

Verify framework hiding:

**Validation Steps**:
- [ ] Stack traces don't contain "xposed" or "lsposed"
- [ ] File.exists() returns false for Xposed framework files
- [ ] PackageManager doesn't list LSPosed or Magisk
- [ ] Thread.getStackTrace() is clean

**Test Method**:
```java
// In a test app within module scope:
new File("/system/framework/edxposed.jar").exists(); // Should return false
new Exception().printStackTrace(); // Should not show Xposed in stack
```

## 📊 Log Validation

### LSPosed Log Checks

Launch TikTok and check LSPosed logs for these messages:

**Expected Log Sequence**:
```
[SamsungCloak][Main] ========================================
[SamsungCloak][Main] Samsung Cloak activated for: com.zhiliaoapp.musically
[SamsungCloak][Main] Target: Samsung Galaxy A12 (SM-A125U)
[SamsungCloak][Main] ========================================
[SamsungCloak][Main] Build fields spoofed successfully
[SamsungCloak][Property] Property hooks initialized with 48 entries
[SamsungCloak][Property] SystemProperties hooks installed successfully
[SamsungCloak][Sensor] Sensor hooks initialized
[SamsungCloak][Sensor] SensorEventQueue.dispatchSensorEvent hooked successfully
[SamsungCloak][Environment] Battery hooks installed
[SamsungCloak][Environment] InputDevice hooks installed
[SamsungCloak][Environment] Memory hooks installed
[SamsungCloak][Environment] DisplayMetrics hooks installed
[SamsungCloak][Environment] TelephonyManager hooks installed
[SamsungCloak][Environment] Environment hooks initialized
[SamsungCloak][AntiDetection] Stack trace hooks installed
[SamsungCloak][AntiDetection] File operation hooks installed
[SamsungCloak][AntiDetection] PackageManager hooks installed
[SamsungCloak][AntiDetection] Anti-detection hooks initialized
[SamsungCloak][Main] Build method hooks installed
[SamsungCloak][Main] All hooks initialized successfully
```

**Validation Checklist**:
- [ ] Activation banner appears
- [ ] No ERROR messages in logs
- [ ] All hook categories initialize successfully
- [ ] Property count is 48 or more
- [ ] No crash reports

### Debug Logging (if enabled)

With DEBUG = true, you should see detailed operation logs:

```
[SamsungCloak][Property] Spoofed property: ro.product.model = SM-A125U
[SamsungCloak][Sensor] Accelerometer: [0.052, 9.823, -0.015]
[SamsungCloak][Environment] Battery extra: level = 72
[SamsungCloak][AntiDetection] Blocked file.exists(): /system/framework/edxposed.jar
```

**Validation Checklist**:
- [ ] Property spoofing logs appear when TikTok queries properties
- [ ] Sensor modification logs show organic values
- [ ] Battery values decrease over time in logs
- [ ] File hiding logs show blocked paths

## 🧪 Integration Testing

### Test Scenario 1: Fresh Install
1. [ ] Install TikTok (clean install)
2. [ ] Enable module for TikTok
3. [ ] Reboot device
4. [ ] Launch TikTok
5. [ ] Complete onboarding
6. [ ] Check device info in settings
7. [ ] Verify all hooks active in logs

### Test Scenario 2: Existing Account
1. [ ] Enable module for existing TikTok install
2. [ ] Clear TikTok app data
3. [ ] Force stop TikTok
4. [ ] Reboot device
5. [ ] Launch TikTok
6. [ ] Log in to account
7. [ ] Verify device appears as Samsung A12

### Test Scenario 3: Cold Start
1. [ ] Reboot device
2. [ ] Wait for full boot
3. [ ] Launch TikTok immediately
4. [ ] Verify hooks initialize before app UI loads
5. [ ] Check logs for proper initialization order

### Test Scenario 4: Hot Start
1. [ ] Launch TikTok
2. [ ] Press home button (don't force stop)
3. [ ] Wait 1 minute
4. [ ] Launch TikTok again (resume)
5. [ ] Verify hooks still active
6. [ ] Check battery level continued to decrease

### Test Scenario 5: Multi-App
1. [ ] Launch TikTok
2. [ ] Switch to TikTok Lite (if installed)
3. [ ] Switch to Douyin (if installed)
4. [ ] Verify module works for all target apps
5. [ ] Verify module DOESN'T affect other apps

## 🔍 Edge Case Testing

### Battery Edge Cases
- [ ] Battery level correctly stops decreasing at 15%
- [ ] Battery never goes negative or above 100%
- [ ] Temperature stays in realistic range
- [ ] Voltage stays in realistic range

### Sensor Edge Cases
- [ ] Accelerometer Y-axis never goes to 0
- [ ] Light sensor never stuck at exactly 0.0
- [ ] Magnetic field values clamped to -60/+60 range
- [ ] Sensor arrays handle null gracefully

### Memory Edge Cases
- [ ] Available memory never exceeds total memory
- [ ] Memory values stay consistent across queries
- [ ] No integer overflows in calculations

### Property Edge Cases
- [ ] Unknown properties return original value (not crash)
- [ ] Null key passed to property getter doesn't crash
- [ ] Integer properties parse correctly
- [ ] Boolean properties parse correctly

## 🚨 Failure Scenarios

### If Module Doesn't Activate
- [ ] Check LSPosed is active (green indicator)
- [ ] Verify target app is in module scope
- [ ] Confirm module is enabled (toggle is on)
- [ ] Try rebooting device
- [ ] Check LSPosed logs for errors

### If Build Values Wrong
- [ ] Verify reflection succeeded (check logs)
- [ ] Ensure app restarted after module enabled
- [ ] Check for conflicting modules
- [ ] Try clearing app data

### If Sensors Not Modified
- [ ] Verify sensor hook installed (check logs)
- [ ] Confirm sensor handle resolution works
- [ ] Check if app uses different sensor APIs
- [ ] Monitor logs during sensor usage

### If Anti-Detection Fails
- [ ] App may use alternative detection methods
- [ ] Check for WebView-based detection (not covered)
- [ ] Verify file paths are correctly hidden
- [ ] Check stack trace filtering is working

## 📝 Performance Validation

### CPU Usage
- [ ] Module adds <1% CPU usage average
- [ ] No sustained high CPU usage
- [ ] No CPU spikes when idle
- [ ] TikTok remains responsive

### Memory Usage
- [ ] Module adds <5 MB RAM usage
- [ ] No memory leaks over time
- [ ] No excessive GC activity
- [ ] App doesn't run out of memory

### Battery Impact
- [ ] Module adds <0.1% battery drain per hour
- [ ] No unusual wake locks
- [ ] No background activity
- [ ] Battery life similar to unhooked app

### App Responsiveness
- [ ] TikTok launches in normal time
- [ ] No UI lag or stuttering
- [ ] Videos play smoothly
- [ ] Scrolling is responsive

## ✅ Final Validation

### Comprehensive Check
- [ ] All core features working
- [ ] No crashes in 30 minutes of use
- [ ] No error logs (warnings acceptable)
- [ ] TikTok perceives device as Samsung A12
- [ ] Organic sensor patterns observed
- [ ] Battery drains realistically
- [ ] Anti-detection measures functional
- [ ] Performance impact minimal

### User Experience
- [ ] Module is invisible to user (no UI impact)
- [ ] TikTok functions normally
- [ ] No unexpected behaviors
- [ ] Module can be disabled cleanly

### Documentation
- [ ] Installation guide followed successfully
- [ ] All features behave as documented
- [ ] No undocumented side effects
- [ ] Troubleshooting guide is accurate

## 🎯 Success Criteria

### Minimum Requirements (Must Pass)
- ✅ Module activates without errors
- ✅ Build.MODEL = "SM-A125U"
- ✅ System properties spoofed
- ✅ No crashes or force closes
- ✅ TikTok runs normally

### Full Compliance (Ideal)
- ✅ All validation checks pass
- ✅ Organic sensor patterns
- ✅ Gradual battery drain
- ✅ Anti-detection working
- ✅ Zero performance impact
- ✅ No detection by TikTok

## 📞 If Validation Fails

1. **Review logs** - Check LSPosed logs for errors
2. **Check environment** - Verify prerequisites met
3. **Try clean install** - Fresh TikTok installation
4. **Report issue** - Create GitHub issue with:
   - Device model and Android version
   - LSPosed version
   - Complete validation results
   - LSPosed logs
   - Steps to reproduce

---

**Validation Version**: 1.0  
**Last Updated**: February 11, 2024  
**Module Version**: 1.0.0
