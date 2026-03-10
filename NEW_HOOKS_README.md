# New Realism Hooks Implementation Guide

## Overview

This document describes the additional realism hooks proposed to expand the Samsung Galaxy A12 (SM-A125U) Xposed-based realism injection framework.

---

## Hook Summary & Overlap Analysis

| # | Proposed Hook | Overlap Status | Action |
|---|---------------|----------------|--------|
| 1 | User Profile & Multi-Profile Switching | **NEW** | Implement |
| 2 | Adaptive Display & Auto-Rotation | **Partial** | Enhance ScreenOrientationUsageHook |
| 3 | Camera Behavior Simulation | **NEW** | Implement |
| 4 | Audio Output & Headphone Simulation | **NEW** | Implement |
| 5 | Physical Button Presses (Power, Volume, Bixby) | **NEW** | Implement |
| 6 | Gesture Navigation Imperfections | **NEW** | Implement |
| 7 | Runtime Permission Granting/Revocation | **NEW** | Implement |

---

## Implementation Details

### 1. UserProfileSwitchingHook

**Description:** Simulates multiple user profiles (work/personal) and switching between them.

**Target:** Samsung Galaxy A12 (SM-A125U) - Android 10/11

**Implementation Location:** `/home/engine/project/app/src/main/java/com/samsungcloak/xposed/UserProfileSwitchingHook.java`

**Key Hooks:**
- `android.os.UserManager.getUserInfo(int userId)`
- `android.os.UserManager.getUserProfiles()`
- `android.app.ActivityManager.getCurrentUser()`
- `android.app.KeyguardManager.isKeyguardLocked()`

---

### 2. DisplayRotationBehaviorHook (Enhancement)

**Description:** Enhances existing ScreenOrientationUsageHook with auto-rotate toggle simulation and synthetic sensor event injection.

**Enhancement:** Adds hooks to:
- `android.provider.Settings.System.getInt(ContentResolver, "accelerometer_rotation")`
- Synthetic orientation sensor events via `SensorManager`

---

### 3. CameraBehaviorSimulationHook

**Description:** Emulates real-world camera quirks: autofocus breathing, exposure adjustments, capture latency.

**Target:** Samsung Galaxy A12 (SM-A125U) - Android 10/11

**Implementation Location:** `/home/engine/project/app/src/main/java/com/samsungcloak/xposed/CameraBehaviorSimulationHook.java`

**Key Hooks:**
- `android.hardware.Camera.Parameters` - modify focus distances, exposure
- `android.hardware.Camera.autoFocus(AutoFocusCallback)`
- `android.hardware.Camera.takePicture shutterCallback, rawCallback, jpegCallback)`

---

### 4. AudioOutputHeadphoneHook

**Description:** Simulates headphone plug/unplug events, Bluetooth device connections, volume changes.

**Target:** Samsung Galaxy A12 (SM-A125U) - Android 10/11

**Implementation Location:** `/home/engine/project/app/src/main/java/com/samsungcloak/xposed/AudioOutputHeadphoneHook.java`

**Key Hooks:**
- `android.media.AudioManager.isWiredHeadsetOn()`
- `android.media.AudioManager.isBluetoothA2dpOn()`
- `android.media.AudioService.setStreamVolume(int streamType, int index, int flags)`
- `Intent.ACTION_HEADSET_PLUG` broadcast injection

---

### 5. PhysicalButtonPressHook

**Description:** Simulates random or pattern-based physical button presses: short presses, long presses, double clicks.

**Target:** Samsung Galaxy A12 (SM-A125U) - Android 10/11

**Implementation Location:** `/home/engine/project/app/src/main/java/com/samsungcloak/xposed/PhysicalButtonPressHook.java`

**Key Hooks:**
- `com.android.server.policy.PhoneWindowManager.interceptKeyBeforeQueueing(KeyEvent, int)`
- `com.android.server.policy.PhoneWindowManager.interceptKeyBeforeDispatching(KeyEvent, int)`
- Inject synthetic `KeyEvent` for KEYCODE_POWER, KEYCODE_VOLUME_UP/DOWN

---

### 6. GestureNavigationImperfectionsHook

**Description:** Simulates incomplete or accidental gestures in gesture navigation (back, home, recent apps).

**Target:** Samsung Galaxy A12 (SM-A125U) - Android 10/11

**Implementation Location:** `/home/engine/project/app/src/main/java/com/samsungcloak/xposed/GestureNavigationImperfectionsHook.java`

**Key Hooks:**
- `android.view.GestureDetector` for edge swipe processing
- Touch event coordinate injection with noise
- Gesture threshold modification

---

### 7. RuntimePermissionChangeHook

**Description:** Simulates the user granting or revoking permissions while an app is in the foreground.

**Target:** Samsung Galaxy A12 (SM-A125U) - Android 10/11

**Implementation Location:** `/home/engine/project/app/src/main/java/com/samsungcloak/xposed/RuntimePermissionChangeHook.java`

**Key Hooks:**
- `android.app.AppOpsManager.noteOp(int op, int uid, String packageName)`
- `android.content.pm.PackageManager.checkPermission(String permName, String pkgName)`
- Permission mode modification via `AppOpsManager.setMode`

---

## Configuration

All hooks are toggleable via SharedPreferences:

```java
private static final String PREFS_NAME = "SamsungCloak_NewHooks";
private static final String KEY_ENABLED = "enabled";
```

---

## Cross-Hook Coherence

The new hooks integrate with existing hooks as follows:

1. **Gesture Navigation + Mechanical Micro-Error**: When walking, gesture swipes become more erratic
2. **Physical Buttons + Proximity Sensor**: Power button presses trigger proximity sensor state changes
3. **Camera + Battery/Thermal**: Camera capture latency increases under thermal throttling
4. **Audio + Network**: Bluetooth audio handover simulates network quality changes

---

## References

- Android 10 (API 29) UserManager: https://developer.android.com/reference/android/os/UserManager
- Android 10 (API 29) Camera: https://developer.android.com/reference/android/hardware/Camera
- Android 10 (API 29) AudioManager: https://developer.android.com/reference/android/media/AudioManager
- Samsung Galaxy A12 (SM-A125U) Specifications: Android 10/11, MediaTek Helio P35
