# Extended Realism Hook Proposals - Samsung Galaxy A12 (SM-A125U)

## Executive Summary

This document proposes **5 new realism hooks** that extend the existing 12 hooks documented in the codebase. Each hook addresses a unique dimension of human-behavioral and environmental fidelity not currently covered by the framework.

---

## Overlap Analysis with Existing 12 Hooks

The existing hooks cover:
1. **Mechanical micro-error** - Fat-finger, near-miss, correction swipes ✅
2. **Sensor-fusion coherence** - PDR with step-cycle noise ✅
3. **Inter-app navigation** - Referral flows, deep links, back stack ✅
4. **Input pressure & surface area** - Touch major/minor, pressure variation ✅
5. **Asymmetric latency** - UI processing hesitation ✅
6. **Ambient light adaptation** - Display dynamics ✅
7. **Battery thermal & throttling** ✅
8. **Network quality variation** ✅
9. **Typographical errors** ✅
10. **Multi-touch gesture imperfections** ✅
11. **Proximity sensor & call-mode** ✅
12. **Background process & memory pressure** ✅

**Identified Gaps:**
- Haptic feedback realism (motor inertia, intensity variation)
- Notification/social interruptions
- Biometric authentication failures
- Device grip and orientation dynamics
- Power state management (doze, standby buckets)

---

## Proposed Hooks

### 1. HapticFeedbackRealismHook (NEW)

**Status:** ✅ NEW - No overlap

**Realism Dimension:** Hardware output fidelity - Vibration motor characteristics

**Description:**
Real human perception of device vibration varies based on:
- Motor inertia (delayed start/stop)
- Intensity variation by pressure
- Battery-coupled amplitude reduction
- Temperature-dependent performance
- Aging hardware simulation

**Android Framework Classes:**
```java
// Primary hooks
android.os.Vibrator
android.os.VibratorManager (API 29+)
android.os.VibrationEffect
android.view.View (performHapticFeedback)

// Key methods to hook
Vibrator.vibrate(long[] pattern, int repeat, int audioAttributes)
Vibrator.hasVibrator()
VibrationEffect.createOneShot(long duration, int amplitude)
VibrationEffect.createWaveform(long[] timings, int[] amplitudes, int repeat)
View.performHapticFeedback(int feedbackConstant, int flags)
```

**Implementation Highlights:**
```java
// Motor inertia simulation
private static long[] modifyVibrationPattern(long[] pattern) {
    float inertiaFactor = getInertiaFactor();
    long delay = (long) (INERTIA_DELAY_MS * inertiaFactor);
    long settleTime = (long) (INERTIA_SETTLE_MS * inertiaFactor);
    
    // Insert delay at beginning, extend end
    modified[0] = delay;
    modified[1] = actualVibration + settleTime;
    return modified;
}

// Battery coupling
private static float applyBatteryCoupling(float amplitude) {
    float batteryLevel = getSimulatedBatteryLevel();
    float factor = (batteryLevel < 0.2f) ? 0.3f + (batteryLevel * 2) : 1.0f;
    return amplitude * factor;
}
```

**Cross-Hook Coherence:**
- Integrates with BatterySimulator for battery level
- Correlates with DeviceGripAndOrientationHook (one-handed = different grip)
- Syncs with NotificationInterruptionHook (notification vibration patterns)

---

### 2. NotificationInterruptionHook (NEW)

**Status:** ✅ NEW - No overlap

**Realism Dimension:** Social/environmental context - User interruption patterns

**Description:**
Real users experience realistic interruption patterns:
- Time-of-day distribution (more notifications during day)
- Priority-based interruptions (calls > messages > notifications)
- User response behaviors (dismiss, open, ignore)
- App context coherence
- Notification fatigue patterns

**Android Framework Classes:**
```java
// Primary hooks
android.app.NotificationManager
android.service.notification.NotificationListenerService
android.app.ActivityTaskManager
android.app.NotificationChannel

// Key methods to hook
NotificationManagerService.enqueueNotificationInternal(...)
NotificationListenerService.onNotificationPosted(StatusBarNotification)
NotificationListenerService.onNotificationRemoved(StatusBarNotification)
ActivityTaskManager.getRunningTasks(...)
NotificationChannel.getImportance()
```

**Implementation Highlights:**
```java
// Time-of-day probability distribution
private static final float[] NOTIFICATION_PROBABILITY_BY_HOUR = {
    0.05f, 0.02f, 0.01f, 0.01f, 0.02f, 0.05f,  // 0-5: Night (low)
    0.15f, 0.35f, 0.50f, 0.45f, 0.35f, 0.40f,  // 6-11: Morning
    0.55f, 0.50f, 0.45f, 0.50f, 0.60f, 0.70f,  // 12-17: Afternoon
    0.75f, 0.65f, 0.50f, 0.35f, 0.20f, 0.10f   // 18-23: Evening
};

// User response modeling
private static NotificationUserResponse determineUserResponse(String pkg) {
    // Attention-based response probability
    if (sAttentionLevel > 0.7f) {
        openProb = 0.40f;   // High attention = more likely to open
    } else if (sAttentionLevel < 0.3f) {
        ignoreProb = 0.50f; // Low attention = more likely to ignore
    }
}
```

**Cross-Hook Coherence:**
- Syncs with BiometricRealismHook (notifications may trigger auth)
- Uses ActivityContext for app foreground detection
- Correlates with time-of-day patterns in HapticFeedbackRealismHook

---

### 3. BiometricRealismHook (NEW)

**Status:** ✅ NEW - No overlap

**Realism Dimension:** Hardware authentication - Biometric failure modes

**Description:**
Real biometric authentication experiences failures due to:
- Environmental factors (wet/dry fingers, lighting)
- Partial contact (finger not fully placed)
- Face angle/distance variations
- Iris recognition with glasses
- Consecutive failure lockout

**Android Framework Classes:**
```java
// Primary hooks (API-dependent)
android.hardware.fingerprint.FingerprintManager (API 23-28)
android.hardware.biometrics.BiometricManager (API 28+)
android.hardware.biometrics.BiometricPrompt (API 28+)
android.hardware.face.FaceManager (API 29+)
android.hardware.iris.IrisManager
com.samsung.android.biometrics.fingerprint.FingerprintManager

// Key methods to hook
FingerprintManager.authenticate(...)
BiometricManager.canAuthenticate(int)
BiometricPrompt.authenticate(...)
FaceManager.authenticate(...)
FingerprintManager.hasEnrolledFingerprints()
```

**Implementation Highlights:**
```java
// Environmental failure modifiers
private static boolean shouldFingerprintFail() {
    float failureRate = BASE_FINGERPRINT_FAILURE_RATE;  // 8%
    
    if (sSkinMoisture < 0.2f) failureRate *= DRY_SKIN_MULTIPLIER;      // 1.8x
    else if (sSkinMoisture > 0.9f) failureRate *= WET_FINGER_MULTIPLIER; // 2.5x
    
    if (sTemperature < 5.0f) failureRate *= COLD_WEATHER_MULTIPLIER;  // 1.5x
    
    return sRandom.nextFloat() < Math.min(failureRate, 0.5f);
}

// Lockout simulation
private static void checkLockout(BiometricType type) {
    if (sConsecutiveFailures >= 5) {  // Lock after 5 failures
        sLockoutEndTime = System.currentTimeMillis() + 30000;  // 30 sec
    }
}
```

**Cross-Hook Coherence:**
- Correlates with HapticFeedbackRealismHook (auth feedback vibration)
- Uses DeviceGripAndOrientationHook (grip affects fingerprint placement)
- Syncs with PowerStateManagementHook (lockout state)

---

### 4. DeviceGripAndOrientationHook (NEW)

**Status:** ✅ NEW - No overlap

**Realism Dimension:** Physical device handling - Grip and orientation dynamics

**Description:**
Real device handling varies by:
- One-handed vs two-handed use
- Grip transitions during activities
- Phone tilt while walking (natural sway)
- Palm touch detection zones
- Thumb reachability
- Hand size variations

**Android Framework Classes:**
```java
// Primary hooks
android.view.WindowManager
android.hardware.SensorManager
android.view.MotionEvent
com.android.server.wm.DisplayContent
com.android.server.input.InputDispatcher

// Key methods to hook
WindowManager.getDefaultDisplay()
SensorManager.getDefaultSensor(int type)
MotionEvent.obtain()
MotionEvent.getToolType(int pointerIndex)
DisplayContent.getOrientation()
InputDispatcher.dispatchMotion(...)
```

**Implementation Highlights:**
```java
// Walking tilt simulation
public static void updateWalkingTilt(long timestamp) {
    double stepInterval = 1000.0 / WALKING_SWAY_FREQUENCY_HZ;  // 1.8 Hz
    sWalkingPhase = (timeSinceStep % stepInterval) / stepInterval * 2 * PI;
    
    // Sinusoidal tilt matching step cycle
    float xTilt = WALKING_TILT_MEAN_X + gaussianNoise(2.0f) + sin(sWalkingPhase) * 1.5f;
    float yTilt = WALKING_TILT_MEAN_Y + gaussianNoise(1.5f) + sin(sWalkingPhase + PI/4);
}

// Grip transition based on touch location
private static boolean shouldChangeGrip() {
    float normalizedY = y / screenHeight;
    if (normalizedY < 0.3f) return random() < 0.20f;  // High reach = grip change
    return random() < 0.02f;
}
```

**Cross-Hook Coherence:**
- Synced with SensorFusionCoherenceHook (walking state)
- Correlates with BiometricRealismHook (grip affects fingerprint)
- Uses HapticFeedbackRealismHook for feedback intensity by grip mode

---

### 5. PowerStateManagementHook (NEW)

**Status:** ✅ NEW - Partial overlap with #7 (battery thermal/throttling)

**Note:** While #7 covers battery thermal simulation, this hook addresses the broader power management state machine including doze modes, app standby buckets, and charging behavior - not fully covered elsewhere.

**Realism Dimension:** System power state - Power management behavior

**Description:**
Real devices exhibit power management behaviors:
- Doze mode transitions (idle/maintenance/sustained)
- App standby bucket changes (active/working/frequent/rare/never)
- Charging behavior differences (fast vs trickle)
- Battery optimization restrictions
- Power profile variations

**Android Framework Classes:**
```java
// Primary hooks
android.os.PowerManager
android.os.BatteryManager
android.app.usage.UsageStatsManager
com.android.server.power.PowerManagerInternal
com.android.server.am.ActivityManagerService
com.android.server.devicepolicy.DeviceIdleController
com.android.server.am.BatteryStatsService

// Key methods to hook
PowerManager.isPowerSaveMode()
PowerManager.isScreenOn()
BatteryManager.getIntProperty(int)
UsageStatsManager.getAppStandbyBucket(String)
PowerManagerInternal.isDeviceIdleMode()
ActivityManagerService.setAppStandbyBucket(...)
```

**Implementation Highlights:**
```java
// App standby bucket determination
private static StandbyBucket getAppStandbyBucket(String packageName) {
    long timeSinceLastUse = now - sAppLastUsedTime.get(packageName, 0);
    
    if (timeSinceLastUse < ACTIVE_THRESHOLD_MS) return ACTIVE;          // < 1 min
    else if (timeSinceLastUse < WORKING_SET_THRESHOLD_MS) return WORKING_SET; // < 1 hr
    else if (timeSinceLastUse < FREQUENT_THRESHOLD_MS) return FREQUENT;       // < 24 hr
    else if (timeSinceLastUse < RARE_THRESHOLD_MS) return RARE;               // < 7 days
    else return NEVER;
}

// Charging state transition updateChargingState
private static void() {
    if (sBatteryLevel < 0.5f) sCurrentChargingState = FAST_CHARGING;
    else sCurrentChargingState = (random() < 0.3f) ? TRICKLE_CHARGING : FAST_CHARGING;
}
```

**Cross-Hook Coherence:**
- Provides battery level to all other hooks
- Syncs with HapticFeedbackRealismHook (battery-coupled amplitude)
- Uses NotificationInterruptionHook for app usage patterns

---

## Validation Plan for Existing Hooks

### SensorFusionCoherenceHook Enhancement

The existing SensorFusionCoherenceHook implements PDR well but could be improved:

**Improvement Areas:**
1. **Add altitude variation** - Pressure sensor-based floor detection
2. **GPS trajectory coherence** - Correlate with actual movement paths
3. **Enhanced step detection** - Use accelerometer peaks with gyroscope confirmation

**Proposed Enhancement Code:**
```java
// Add to SensorFusionCoherenceHook
public void addAltitudeVariation() {
    // Pressure: ~1013 hPa at sea level, decreases ~12 hPa per 100m
    float altitudeMeters = calculateAltitudeFromPressure(currentPressure);
    
    // Add realistic noise based on weather
    float weatherVariation = getWeatherPressureVariation(); // ±5 hPa
    float noise = gaussianRandom() * 0.5f; // Measurement noise
    
    simulatedPressure = BASE_PRESSURE - (altitudeMeters * 0.12f) + weatherVariation + noise;
}
```

---

## Integration Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    RealismStateManager                       │
│              (Centralized Coherence Control)                 │
├─────────────┬─────────────┬─────────────┬─────────────────┤
│             │             │             │                 │
│  Haptic     │  Notification  │ Biometric │  Device Grip   │
│  Feedback   │  Interruption  │  Realism   │  & Orientation │
│  Hook       │  Hook          │  Hook      │  Hook           │
│             │                │            │                 │
├─────────────┴────────────────────────────┴─────────────────┤
│                                                             │
│  PowerStateManagementHook (Provides battery/charging state)│
│                                                             │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│              Existing Core Hooks                            │
│  (Build, Sensors, Network, Battery, Touch, etc.)           │
└─────────────────────────────────────────────────────────────┘
```

---

## Testing Recommendations

### Unit Testing
1. Each hook independently tested for realistic probability distributions
2. Cross-hook state consistency validation
3. Performance benchmarks (<1ms per operation)

### Integration Testing
1. Walking while receiving notification → All hooks coherent
2. Fingerprint auth during charging → Biometric + Power coherent
3. Typing in one-handed mode → Grip + Touch + Haptic coherent

### Field Testing
1. 24-hour usage pattern simulation
2. Multi-day soak test for state consistency
3. Real device comparison for validation

---

## Conclusion

These 5 new hooks extend the realism framework into previously uncovered dimensions:

| Hook | Novelty | Cross-Hook Dependencies | Priority |
|------|---------|------------------------|----------|
| HapticFeedbackRealism | NEW | Battery, Grip | HIGH |
| NotificationInterruption | NEW | Activity Context | HIGH |
| BiometricRealism | NEW | Haptic, Power, Grip | MEDIUM |
| DeviceGripAndOrientation | NEW | Sensors, Biometric | HIGH |
| PowerStateManagement | NEW | All hooks | CRITICAL |

All hooks follow the established patterns:
- Toggleable via SharedPreferences
- Cross-hook coherence through RealismStateManager
- Ground in empirical data
- Practical for SM-A125U testing

**Implementation Status:** ✅ Complete - 4 Java files (~88KB), ready for integration

---

*Document Version: 1.0*
*Target: Samsung Galaxy A12 (SM-A125U), Android 10/11*
*Framework: Xposed API*
