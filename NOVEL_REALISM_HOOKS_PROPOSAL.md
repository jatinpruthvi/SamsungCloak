# Novel Realism Hooks Proposal
## Samsung Galaxy A12 (SM-A125U) Xposed Framework Extension

**Version**: 1.0.0  
**Date**: March 2025  
**Target Device**: Samsung Galaxy A12 (SM-A125U)  
**Android Version**: 10/11 (API 29-30)

---

## Executive Summary

This document proposes **7 novel realism hooks** that extend the existing framework beyond the 12 reference hooks and 5 recently implemented hooks.

### Existing Hook Inventory

**Original 12 Hooks (Reference)**:
1. Mechanical micro-error simulation
2. Sensor-fusion coherence
3. Inter-app navigation context
4. Input pressure & surface area dynamics
5. Asymmetric latency
6. Ambient light adaptation
7. Battery thermal & performance throttling
8. Network quality variation
9. Typographical errors & keyboard realism (PARTIAL)
10. Multi-touch gesture imperfections (PARTIAL)
11. Proximity sensor & call-mode simulation (PARTIAL)
12. Background process & memory pressure

**Recently Implemented (5 hooks)**:
1. HapticFeedbackRealismHook
2. NotificationInterruptionHook
3. BiometricRealismHook
4. DeviceGripAndOrientationHook
5. PowerStateManagementHook

---

## Novel Hook Proposals

### HOOK-1: AudioEnvironmentRealismHook
**Status**: NEW - No existing coverage

#### Description
Simulates realistic audio environment interactions including:
- Background noise floor affecting microphone input SNR
- Automatic volume adjustment based on ambient noise (Adaptive Sound)
- Microphone occlusion detection (covered finger, case obstruction)
- Audio focus interruptions and ducking behavior

#### Overlap Analysis
- **No overlap** with existing hooks. While `AudioFingerprintHook` spoofs audio hardware fingerprinting, this hook models the *acoustic environment* and user-audio interactions.

#### Key Implementation Details

**Target Classes**:
- `android.media.AudioManager`
- `android.media.AudioRecord`
- `android.media.MediaRecorder`

**Methods Hooked**:
- `AudioManager.getStreamVolume()` - Adaptive volume
- `AudioManager.requestAudioFocus()` - Focus interruptions
- `AudioRecord.read()` - Noise injection
- `AudioRecord.startRecording()` - Occlusion simulation
- `MediaRecorder.getMaxAmplitude()` - SNR modulation

**Key Features**:
```java
public enum AcousticEnvironment {
    QUIET_ROOM(30, 5),      // 30dB ±5dB
    OFFICE(55, 8),          // 55dB ±8dB
    CAFE(70, 10),           // 70dB ±10dB
    STREET(75, 12),         // 75dB ±12dB
    CONSTRUCTION(90, 15);   // 90dB ±15dB
}
```

**Cross-Hook Coherence**:
- Integrates with `NotificationInterruptionHook` for audio focus loss during notifications
- Links with `DeviceGripAndOrientationHook` for microphone occlusion when grip changes

---

### HOOK-2: AppUsagePatternRealismHook
**Status**: NEW - No existing coverage

#### Description
Simulates realistic application usage patterns including:
- Task sequence authenticity (typical user workflows)
- Session duration distributions (Pareto/Weibull models)
- Task switching frequency with context-aware timing
- App category usage time-of-day patterns
- Long-term habit formation and drift

#### Overlap Analysis
- **No overlap** with existing hooks. While `AppHistorySpoofer` provides fake usage history data, this hook models the *temporal patterns* and *behavioral sequences* of usage.

#### Key Implementation Details

**Target Classes**:
- `android.app.usage.UsageStatsManager`
- `android.app.ActivityManager`
- `com.android.server.am.ActivityManagerService`

**Methods Hooked**:
- `UsageStatsManager.queryUsageStats()` - Session duration modification
- `UsageStatsManager.queryEvents()` - Event sequence realism
- `ActivityManager.getRunningAppProcesses()` - Task switching tracking
- `ActivityManagerService.activityPaused()` - Lifecycle analysis

**Key Features**:
```java
// Weibull distribution for realistic session lengths
private static final double SESSION_SHAPE = 1.5;
private static final double SESSION_SCALE = 300;  // seconds

// Time-of-day usage probability
private static final float[] USAGE_PROBABILITY_BY_HOUR = {
    0.02f, 0.01f, 0.01f, 0.01f, 0.02f, 0.05f,   // 0-5: Night
    0.15f, 0.25f, 0.20f, 0.15f, 0.15f, 0.18f,   // 6-11: Morning
    0.25f, 0.20f, 0.18f, 0.20f, 0.22f, 0.25f,   // 12-17: Afternoon
    0.35f, 0.45f, 0.50f, 0.45f, 0.35f, 0.20f,   // 18-23: Evening
};
```

**Cross-Hook Coherence**:
- Integrates with `PowerStateManagementHook` for app standby bucket assignment
- Links with `NotificationInterruptionHook` for usage context

---

### HOOK-3: HardwareAgingDegradationHook
**Status**: NEW - Partial overlap with HardwareDegradationHook

#### Description
Simulates progressive hardware degradation over device lifetime:
- Touch screen latency degradation (non-uniform across screen areas)
- Battery capacity fade with cycle count
- Speaker/microphone frequency response degradation
- Camera focus mechanism drift and delay
- Storage NAND wear and slowdown

#### Overlap Analysis
- **Partial overlap** with `HardwareDegradationHook` (cognitive-testing-framework). This hook specifically targets **measurable hardware aging patterns** with SM-A125U-specific parameters.

#### Key Implementation Details

**Target Classes**:
- `android.view.MotionEvent`
- `android.view.InputDevice`
- `android.os.BatteryManager`
- `android.hardware.camera2.CameraDevice`
- `android.os.storage.StorageManager`

**Methods Hooked**:
- `MotionEvent.obtain()` - Latency injection
- `MotionEvent.getEventTime()` - Timestamp modification
- `BatteryManager.getIntProperty()` - Capacity fade
- `CameraDevice.capture()` - Focus delay
- `StorageManager.getAllocatableBytes()` - Wear simulation

**Key Features**:
```java
// SM-A125U specific degradation parameters
private static final float MAX_AGE_YEARS = 3.5f;
private static final long CYCLES_PER_YEAR = 400;
private static final float BASE_TOUCH_LATENCY_MS = 12f;
private static final float MAX_TOUCH_LATENCY_MS = 45f;
private static final float BATTERY_CAPACITY_NEW = 5000f;  // mAh
private static final float BATTERY_CAPACITY_EOL = 3500f;  // 70% retention

// Non-uniform touch grid (6x10 for 720x1600)
private static final int GRID_COLS = 6;
private static final int GRID_ROWS = 10;
private static final float[][] sTouchLatencyGrid = new float[GRID_COLS][GRID_ROWS];
```

**Cross-Hook Coherence**:
- Integrates with `BatterySimulator` for capacity fade coordination
- Links with `TouchSimulator` for latency overlay

---

### HOOK-4: TypographicalErrorRealismHook
**Status**: IMPROVEMENT - Extends reference hook #9

#### Description
Expands on basic typographical error simulation with:
- Keyboard-specific error patterns (QWERTY adjacency, Samsung keyboard layout)
- Touch-typing vs hunt-and-peck error profiles
- Auto-correction interaction modeling
- Backspace behavior and correction patterns
- Context-aware typo likelihood (password fields vs text)

#### Overlap Analysis
- **Partial overlap** with reference hook #9. The existing implementation provides basic error injection, but this adds **keyboard-specific physics** and **correction behavior modeling**.

#### Validation Plan for Existing Implementation

**1. Fidelity Assessment**:
- Verify QWERTY adjacency errors are 70% of all typos
- Check typing skill levels produce appropriate error rates:
  - Hunt-and-peck: 12% error rate
  - Average: 6% error rate
  - Touch-typist: 3% error rate
  - Expert: 1% error rate
- Validate backspace correction patterns follow error clustering

**2. Specific Improvements**:

```java
// QWERTY adjacency map for realistic errors
private static final Map<Character, char[]> QWERTY_ADJACENCY = new HashMap<>();

static {
    QWERTY_ADJACENCY.put('q', new char[] {'w', 'a', 's'});
    QWERTY_ADJACENCY.put('w', new char[] {'q', 'e', 's', 'a', 'd'});
    // ... full QWERTY mapping
}

// Typing skill levels with error rates
public enum TypingSkill {
    HUNT_AND_PECK(0.12f, 0.05f),    // 12% error, 5% correction
    AVERAGE(0.06f, 0.08f),          // 6% error, 8% correction
    TOUCH_TYPER(0.03f, 0.10f),      // 3% error, 10% correction
    EXPERT(0.01f, 0.15f);           // 1% error, 15% correction
}
```

**Target Classes**:
- `android.inputmethodservice.InputMethodService`
- `android.view.inputmethod.InputConnection`
- `com.samsung.android.honeyboard` (Samsung-specific)

**Cross-Hook Coherence**:
- Integrates with `InputPressureDynamicsHook` for pressure-correlated errors
- Links with `DeviceGripAndOrientationHook` for grip-affected typing

---

### HOOK-5: ProximityCallModeRealismHook
**Status**: IMPROVEMENT - Extends reference hook #11

#### Description
Enhances proximity sensor and call-mode simulation with:
- In-call sensor behavior (ear proximity detection)
- Speakerphone transition patterns
- Call UI interaction timing
- Pocket detection false positives
- Face-down table detection

#### Overlap Analysis
- **Partial overlap** with reference hook #11. Existing implementation provides basic sensor spoofing, but this adds **call-scenario realism** and **transition behavior**.

#### Validation Plan for Existing Implementation

**1. Fidelity Assessment**:
- Verify proximity trigger timing matches natural ear-to-phone movement (~200-400ms)
- Check that speakerphone transitions have realistic user confirmation delays (~800ms)
- Validate pocket detection false positive rate (~2-5%)
- Ensure ear proximity hold time minimum (~5s before FAR transition)

**2. Specific Improvements**:

```java
// Proximity timing parameters (ms)
private static final long EAR_PROXIMITY_MIN_TIME = 200;
private static final long EAR_PROXIMITY_MAX_TIME = 400;
private static final long EAR_PROXIMITY_HOLD_MIN = 5000;
private static final long SPEAKERPHONE_TRANSITION_DELAY = 800;

// Pocket detection false positive
private static final float POCKET_FP_PROBABILITY = 0.03f;
private static final long POCKET_DETECTION_DELAY = 1500;
```

**Target Classes**:
- `android.hardware.SensorManager`
- `android.telephony.TelephonyManager`
- `android.os.PowerManager`
- `android.media.AudioManager`

**Cross-Hook Coherence**:
- Integrates with `AudioEnvironmentRealismHook` for call audio quality
- Links with `NotificationInterruptionHook` for call interruption handling

---

### HOOK-6: GPSLocationTrajectoryHook
**Status**: NEW - No existing coverage

#### Description
Simulates realistic GPS trajectories and location behavior:
- Continuous trajectory generation with realistic speed/acceleration profiles
- GPS signal acquisition time (TTFF - Time To First Fix)
- Urban canyon multipath effects
- Indoor/outdoor transition handling
- Location update interval variation

#### Overlap Analysis
- **No overlap** with existing hooks. While `LocationHook` provides static coordinate spoofing, this hook models **dynamic trajectory realism** and **GPS physics**.

#### Key Implementation Details

**Target Classes**:
- `android.location.LocationManager`
- `android.location.Location`
- `android.location.LocationRequest`

**Methods Hooked**:
- `LocationManager.getLastKnownLocation()` - Trajectory-based location
- `LocationManager.requestLocationUpdates()` - Update interval jitter
- `LocationManager.getCurrentLocation()` - TTFF simulation
- `Location.getAccuracy()` - Environment-based accuracy
- `Location.getSpeed()` - Physics-based speed

**Key Features**:
```java
// GPS characteristics (SM-A125U with MediaTek MT6765)
private static final float GPS_ACCURACY_OPEN_SKY = 3.0f;   // meters
private static final float GPS_ACCURACY_URBAN = 15.0f;     // meters
private static final float GPS_ACCURACY_INDOOR = 50.0f;    // meters
private static final long TTFF_COLD_START_MS = 35000;      // 35 seconds
private static final long TTFF_WARM_START_MS = 5000;       // 5 seconds

// Trajectory physics
public enum MotionState {
    STATIONARY, WALKING, RUNNING, DRIVING, TRANSIT
}

private static final float MAX_WALKING_SPEED = 1.5f;       // m/s
private static final float MAX_RUNNING_SPEED = 4.0f;       // m/s
private static final float MAX_DRIVING_SPEED = 25.0f;      // m/s
private static final float ACCELERATION_MAX = 2.0f;        // m/s²
```

**Cross-Hook Coherence**:
- Integrates with `SensorFusionCoherenceHook` for IMU-correlated location
- Links with `DeviceGripAndOrientationHook` for motion state detection

---

### HOOK-7: MultiTouchGestureRealismHook
**Status**: IMPROVEMENT - Extends reference hook #10

#### Description
Enhances multi-touch gesture simulation with:
- Pinch-to-zoom physics (finger distance dynamics)
- Rotation gesture imperfections
- Two-finger scroll jitter and decoupling
- Three-finger gesture recognition variability
- Gesture conflict resolution timing

#### Overlap Analysis
- **Partial overlap** with reference hook #10. Existing `TouchSimulator` provides basic multi-touch, but this adds **gesture physics** and **recognition variability**.

#### Validation Plan for Existing Implementation

**1. Fidelity Assessment**:
- Verify pinch zoom velocity follows power law (Fitts's Law)
- Check rotation gesture has realistic angular momentum (friction ~0.95)
- Validate two-finger scroll shows finger decoupling (~15px max)
- Ensure gesture recognition timing varies by complexity

**2. Specific Improvements**:

```java
// Pinch gesture physics
private static final float PINCH_BASE_VELOCITY = 0.5f;    // scale factor/s
private static final float PINCH_ACCELERATION = 2.0f;
private static final float PINCH_DECELERATION = 3.0f;
private static final float PINCH_JITTER_FACTOR = 0.02f;

// Rotation gesture physics
private static final float ROTATION_FRICTION = 0.95f;
private static final float ROTATION_JITTER_DEGREES = 2.0f;
private static final float MIN_ROTATION_DETECTION = 5.0f;

// Two-finger scroll decoupling
private static final float FINGER_DECOUPLING_MAX = 15.0f;  // pixels
private static final float SCROLL_CORRELATION = 0.85f;

// Gesture recognition timing
private long calculateRecognitionDelay(MotionEvent event) {
    int pointerCount = event.getPointerCount();
    if (pointerCount == 1) return 10 + random(30);   // 10-40ms
    if (pointerCount == 2) return 20 + random(50);   // 20-70ms
    return 50 + random(100);                          // 50-150ms
}
```

**Target Classes**:
- `android.view.ScaleGestureDetector`
- `android.view.GestureDetector`
- `android.view.MotionEvent`

**Cross-Hook Coherence**:
- Integrates with `TouchSimulator` for pressure/area correlation
- Links with `DeviceGripAndOrientationHook` for grip-affected gestures

---

## Summary Table

| Hook | Status | Overlap | Key Classes Hooked |
|------|--------|---------|-------------------|
| AudioEnvironmentRealismHook | NEW | None | AudioManager, AudioRecord, MediaRecorder |
| AppUsagePatternRealismHook | NEW | None | UsageStatsManager, ActivityManager |
| HardwareAgingDegradationHook | NEW | Partial | MotionEvent, BatteryManager, Camera2 |
| TypographicalErrorRealismHook | IMPROVEMENT | Reference #9 | InputMethodService, InputConnection |
| ProximityCallModeRealismHook | IMPROVEMENT | Reference #11 | SensorManager, TelephonyManager, PowerManager |
| GPSLocationTrajectoryHook | NEW | None | LocationManager, Location |
| MultiTouchGestureRealismHook | IMPROVEMENT | Reference #10 | ScaleGestureDetector, GestureDetector |

---

## Implementation Priority

1. **High Priority**: AudioEnvironmentRealismHook, GPSLocationTrajectoryHook
   - Fills major gaps in environmental realism
   - High impact on detection vectors

2. **Medium Priority**: HardwareAgingDegradationHook, AppUsagePatternRealismHook
   - Adds temporal depth to simulation
   - Long-term behavioral fingerprinting defense

3. **Low Priority**: TypographicalErrorRealismHook, ProximityCallModeRealismHook, MultiTouchGestureRealismHook
   - Enhance existing functionality
   - Incremental realism improvements

---

## SM-A125U Specific Considerations

1. **Hardware Constraints**:
   - MediaTek MT6765 sensor hub behavior
   - 720x1600 display touch digitizer characteristics
   - 5000mAh battery degradation curves

2. **Software Version**:
   - Android 10/11 (API 29-30) API availability
   - Samsung One UI 2.x specific classes
   - T-Mobile US carrier configurations

3. **Testing Recommendations**:
   - Validate on physical SM-A125U hardware
   - Compare against stock Samsung A12 behavior
   - Monitor hook overhead on low-end processor

---

*Document End*
