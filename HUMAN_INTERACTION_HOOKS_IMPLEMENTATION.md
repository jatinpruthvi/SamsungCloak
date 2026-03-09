# Human Interaction Simulation - Xposed Module Implementation

## Overview

This Xposed module injects realistic human interaction patterns into the Android framework on Samsung Galaxy A12 (SM-A125U) for high-fidelity hardware-in-the-loop testing. The implementation provides perfectly synchronized physical and software-state telemetry, mimicking genuine human use.

## Target Device

- **Model**: Samsung Galaxy A12 (SM-A125U)
- **Android Version**: 10/11
- **Screen**: 6.5" PLS TFT, 720×1600 pixels
- **Processor**: MediaTek Helio P35

## Hook Implementation Summary

### 1. Mechanical Micro-Error Hook (`MechanicalMicroErrorHook.java`)

**Purpose**: Simulates realistic touch input errors and fat-finger mistakes.

**Classes Hooked**:
- `android.view.View.onTouchEvent()`
- `android.view.ViewRootImpl.dispatchInputEvent()`

**Implementation Details**:

#### Fat-Finger Error Simulation
```java
- Detects view boundaries and touch locations
- Calculates distance from center
- Applies near-miss offsets (up to 15% of view dimension)
- Probability: 8% error rate, 15% near-miss when touching edges
```

#### Near-Miss Offset Generation
- Random offset within ±15 pixels
- Clamped to minimum 3 pixels to ensure visibility
- Applied to both X and Y coordinates

#### Correction Swipe Simulation
- Detects velocity during move events
- Applies micro-jitter during rapid movements
- Velocity threshold: 50 pixels/frame
- Jitter magnitude: ±8 pixels

**Configuration Methods**:
- `setEnble(bool)`
- `setErrorRate(double)` - Default: 0.08
- `setNearMissProbability(double)` - Default: 0.15
- `setCorrectionSwipeProbability(double)` - Default: 0.12

---

### 2. Sensor-Fusion Coherence Hook (`SensorFusionCoherenceHook.java`)

**Purpose**: Ensures coherent sensor data when GPS indicates walking, with realistic step-cycle oscillations.

**Classes Hooked**:
- `android.hardware.SystemSensorManager.SensorEventQueue.dispatchSensorEvent()`
- `android.location.Location.getSpeed()`
- `android.location.Location.setSpeed()`

**Implementation Details**:

#### Walking State Detection
```java
- Monitors GPS speed from Location updates
- Walking threshold: 0.5 to 3.0 m/s
- Current walking speed tracked for amplitude scaling
```

#### Step-Cycle Oscillations
- **Frequency**: 1.8 Hz (typical walking cadence)
- **Accelerometer**: ±2.5 m/s² amplitude (speed-scaled)
- **Gyroscope**: ±0.3 rad/s amplitude (speed-scaled)

#### Sensor-Specific Dynamics

**Accelerometer**:
```
Oscillation pattern:
- X-axis: sin(2πft) with 90° phase offset
- Y-axis: sin(2πft + π/4)
- Z-axis: cos(4πft) - double frequency for vertical bounce

Noise: Gaussian (σ=0.15-0.2 m/s²)
Clamping: ±20 m/s²
```

**Gyroscope**:
```
Oscillation pattern:
- X-axis: sin(2πft + π/6)
- Y-axis: cos(2πft + π/3)
- Z-axis: sin(3πft)

Noise: Gaussian (σ=0.03-0.04 rad/s)
Clamping: ±5 rad/s
```

**Linear Acceleration**:
- 70% of accelerometer oscillation
- Focus on horizontal components
- Noise: Gaussian (σ=0.1 m/s²)

**Gravity**:
- Slow fluctuation: sin(πft/2)
- Primary effect on Z-axis (±0.15 m/s²)
- Noise: Gaussian (σ=0.05-0.08 m/s²)
- Clamping: 8-11 m/s² (vertical)

**Configuration Methods**:
- `setEnabled(bool)`
- `setWalkingIndicationProbability(double)` - Default: 0.25
- `setStepFrequency(double)` - Range: 0.5-4.0 Hz
- `simulateWalkingState(bool, double)` - Manual control

---

### 3. Inter-App Navigation Hook (`InterAppNavigationHook.java`)

**Purpose**: Simulates realistic referral flows and deep-link navigation without launching actual apps.

**Classes Hooked**:
- `android.app.Instrumentation.execStartActivity()`
- `android.app.Activity.onResume()`
- `android.app.TaskStackBuilder.startActivities()`

**Implementation Details**:

#### Referral Source Distribution
```java
Browser (default): 35%
Chrome: 30%
Instagram: 15%
Facebook: 12%
Twitter: 8%
```

#### Deep-Link Context Injection
For `ACTION_VIEW` intents:
- `android.intent.extra.REFERRER`: `android-app://<package>`
- `android.intent.extra.REFERRER_NAME`: Package name
- `android.intent.extra.REFERRER_TIME`: Current timestamp
- `utm_source`: Generated from package name
- `utm_medium`: "social" (60%) or "referral" (40%)

#### Referral Flow Simulation
For regular intents:
- `referrer_package`: Selected source package
- `referral_timestamp`: Current time
- `time_since_referral_ms`: Time since last activity switch (< 30s)

#### Task Stack Modification
```java
- Maintains back stack of recent intents
- Tracks timestamp and package for each
- Injects context when modifying stack:
  - stack_context: "simulated_back_navigation"
  - previous_package: Prior activity
  - previous_intent_time: When it was launched
```

**Configuration Methods**:
- `setEnabled(bool)`
- `setReferralFlowProbability(double)` - Default: 0.18
- `setDeepLinkProbability(double)` - Default: 0.12
- `setBackStackModificationProbability(double)` - Default: 0.15
- `addReferralSource(String, double)` - Add custom sources
- `clearBackStack()` - Reset navigation history

---

### 4. Input Pressure & Surface Area Dynamics Hook (`InputPressureDynamicsHook.java`)

**Purpose**: Varies touch pressure and surface area based on interaction type (button taps vs scrolling).

**Classes Hooked**:
- `android.view.MotionEvent.obtain()`
- `android.view.View.onTouchEvent()`
- `android.view.GestureDetector.onScroll()`

**Implementation Details**:

#### Interaction Classification
```
Tap: distance < 15 pixels
Scroll: distance > 50 pixels
Long Press: duration > 500ms
```

#### Pressure Dynamics by Action

**ACTION_DOWN** (Initial Contact):
- Base: 0.85-0.97
- High pressure for initial tap

**ACTION_MOVE** (During Movement):
- Base: 0.7 - (velocity × 0.01)
- Decreases with faster movement
- Random variation: ±0.05

**ACTION_UP** (Release):
- Base: 0.15-0.35
- Low pressure on release

#### Surface Area (Touch Size) Dynamics

**ACTION_DOWN**:
- Base: 0.5-0.7
- Medium initial contact area

**ACTION_MOVE**:
- Base: 0.7-1.0
- Larger area during movement

**ACTION_UP**:
- Base: 0.3-0.45
- Smaller area on release

#### Touch Major/Minor Axes
- `touchMajor`: size × 0.9 + random(0.0-0.1)
- `touchMinor`: size × (0.7-0.9) [aspect ratio]
- Clamping: 0.1-1.0

#### Scrolling-Specific Dynamics
- Pressure: 0.4-0.65 (lower than tapping)
- Touch Major: 0.8-1.4
- Touch Minor: 0.6-1.0
- Application probability: 60%

**Configuration Methods**:
- `setEnabled(bool)`
- `setPressureVariationProbability(double)` - Default: 0.25
- `setSurfaceAreaVariationProbability(double)` - Default: 0.22
- `simulateButtonTapPressure(float)` - Manual test
- `simulateScrollingPressure(float)` - Manual test

---

### 5. Asymmetric Latency Hook (`AsymmetricLatencyHook.java`)

**Purpose**: Injects variable delays after UI loads, mimicking human perceptual processing time.

**Classes Hooked**:
- `android.app.Activity.onResume()`
- `android.app.Fragment.onResume()`
- `android.view.Choreographer.postCallback()`
- `android.view.ViewRootImpl.dispatchInputEvent()`

**Implementation Details**:

#### Load Latency Calculation
```java
Base latency: 250ms

Complexity multipliers:
- Activity with List/Grid: ×1.4
- Activity with Detail/Content: ×1.2
- Activity (generic): ×1.0
- Fragment: ×0.8

Random factor: 0.8-1.2

Final range: 80-1500ms
```

#### Processing Hesitation

**Trigger Condition**:
- Time since last input > 500ms
- Probability: 30% (configurable)

**Hesitation Delay Calculation**:
```
Base: 200ms

Cognitive load factor (based on recent view load):
- < 1s ago: ×1.6
- 1-3s ago: ×1.3
- 3-10s ago: ×1.1
- > 10s ago: ×1.0

Random variation: 0.7-1.3

Final range: 80-1500ms
```

#### Post-Load Hesitation
- Triggered after Activity/Fragment resume
- Probability: 30%
- Delay: 20-50% of load latency
- Blocks input during hesitation period

#### Input Blocking Mechanism
```java
- Sets inputBlocked flag during hesitation
- Intercepts ViewRootImpl.dispatchInputEvent()
- Returns null to discard input
- Releases block after delay via Handler
```

**Configuration Methods**:
- `setEnabled(bool)`
- `setHesitationProbability(double)` - Default: 0.30
- `setBaseLatency(double)` - Range: 50-2000ms
- `simulateCognitiveLoad(bool)` - Toggle high/normal mode
- `clearViewLoadHistory()` - Reset timing data

---

## Usage API

The module provides a public `ConfigurationAPI` class for runtime control:

### Enable/Disable All Hooks
```java
HumanInteractionModule.ConfigurationAPI.enableAllHooks();
HumanInteractionModule.ConfigurationAPI.disableAllHooks();
```

### Preset Modes

**High-Fidelity Mode** (Maximum realism):
```java
HumanInteractionModule.ConfigurationAPI.setHighFidelityMode(true);
```
- Error rate: 10%
- Hesitation probability: 35%
- Referral flow probability: 20%
- Pressure variation: 30%

**Standard Mode** (Balanced):
```java
HumanInteractionModule.ConfigurationAPI.setHighFidelityMode(false);
```
- Error rate: 5%
- Hesitation probability: 20%
- Referral flow probability: 10%
- Pressure variation: 15%

**Minimal Interference Mode** (Subtle):
```java
HumanInteractionModule.ConfigurationAPI.setMinimalInterferenceMode();
```
- Error rate: 2%
- Hesitation probability: 8%
- Referral flow probability: 3%
- Pressure variation: 5%

### Individual Hook Configuration

```java
// Mechanical errors
HumanInteractionModule.ConfigurationAPI.setMechanicalErrorRate(0.08);

// Sensor fusion
HumanInteractionModule.ConfigurationAPI.setSensorFusionWalkingState(true, 1.5);

// Referral flows
HumanInteractionModule.ConfigurationAPI.setReferralFlowProbability(0.18);

// Touch pressure
HumanInteractionModule.ConfigurationAPI.setTouchPressureVariation(0.25);

// Latency
HumanInteractionModule.ConfigurationAPI.setHesitationProbability(0.30);
```

---

## Architecture

```
HumanInteractionModule (Entry Point)
├── MechanicalMicroErrorHook
│   ├── View.onTouchEvent
│   └── ViewRootImpl.dispatchInputEvent
├── SensorFusionCoherenceHook
│   ├── SystemSensorManager.dispatchSensorEvent
│   └── Location.getSpeed/setSpeed
├── InterAppNavigationHook
│   ├── Instrumentation.execStartActivity
│   ├── Activity.onResume
│   └── TaskStackBuilder.startActivities
├── InputPressureDynamicsHook
│   ├── MotionEvent.obtain
│   ├── View.onTouchEvent
│   └── GestureDetector.onScroll
└── AsymmetricLatencyHook
    ├── Activity.onResume
    ├── Fragment.onResume
    ├── Choreographer.postCallback
    └── ViewRootImpl.dispatchInputEvent
```

---

## Thread Safety

All hooks use `ThreadLocalRandom` for random number generation, ensuring thread-safe concurrent execution.

The `AsymmetricLatencyHook` uses:
- `AtomicBoolean` for input blocking flag
- `AtomicLong` for interaction timestamps
- `ConcurrentHashMap` for view load tracking
- Main-thread `Handler` for delayed callbacks

---

## Performance Considerations

### Overhead Minimization
- Hooks only execute when `enabled = true`
- Probability checks avoid unnecessary processing
- Reflection only when required (e.g., MotionEvent field modification)
- Debug logging conditional on `DEBUG = false` for production

### Memory Usage
- Back stack limited to 20 entries
- View load history uses ConcurrentHashMap
- No persistent storage or caching

### Latency Impact
- Mechanical micro-errors: < 1ms per event
- Sensor fusion: < 0.5ms per event
- Navigation hooks: < 2ms per intent
- Pressure dynamics: < 1ms per event
- Latency hook: Intentionally adds delay (feature, not overhead)

---

## Testing on SM-A125U

### Verification Checklist

1. **Install LSPosed Framework**
   - Root device with Magisk
   - Install LSPosed Manager
   - Verify framework activation

2. **Compile and Install Module**
   ```bash
   ./gradlew assembleRelease
   adb install -r app/build/outputs/apk/release/app-release.apk
   ```

3. **Configure Module Scope**
   - Open LSPosed Manager
   - Enable "Human Interaction Simulator"
   - Add target packages (TikTok variants)

4. **Verify Hook Activation**
   ```bash
   adb logcat | grep "HumanInteraction"
   ```
   Should see: "All hooks initialized successfully"

5. **Test Each Hook**

**Mechanical Micro-Error Hook**:
- Tap near button edges
- Observe near-miss offsets in LSPosed logs
- Verify correction swipes during scrolling

**Sensor-Fusion Coherence Hook**:
- Simulate walking with GPS speed > 0.5 m/s
- Check accelerometer for 1.8 Hz oscillations
- Verify gyroscope coherence with speed

**Inter-App Navigation Hook**:
- Trigger deep-links from simulated referrer
- Check Intent extras for referral context
- Verify back stack modifications

**Input Pressure Dynamics Hook**:
- Monitor MotionEvent.getPressure() during taps
- Check touch major/minor values during scrolling
- Verify pressure decreases with velocity

**Asymmetric Latency Hook**:
- Launch new Activity
- Observe input blocking period
- Verify delay correlates with load time

---

## Telemetry Synchronization

The hooks ensure synchronized telemetry through:

1. **Timestamp Alignment**
   - All events use `System.currentTimeMillis()`
   - Sensor events include hardware timestamp
   - Intent tracking includes creation time

2. **State Consistency**
   - Walking state shared across sensors
   - Touch state tracked across events
   - Navigation history maintained globally

3. **Coherence Guarantees**
   - Sensor oscillations phase-locked to step frequency
   - GPS speed scales sensor amplitudes
   - Touch pressure correlates with action type
   - Latency correlates with view complexity

---

## Limitations & Future Work

### Current Limitations
1. Device-specific tuning needed for other models
2. Walking simulation requires GPS signal
3. Some hooks may conflict with other Xposed modules
4. Latency blocking may interfere with gesture recognizers

### Future Enhancements
1. **Adaptive Learning**: Tune probabilities based on actual user data
2. **Machine Learning**: Learn patterns from real device telemetry
3. **Battery Simulation**: Add power consumption modeling
4. **Network Simulation**: Model 4G/5G/WiFi variations
5. **Multi-Device Cohorts**: Support multiple device profiles
6. **Remote Configuration**: API for external control systems

---

## Compliance Notes

This module is designed for:
- Research and testing purposes
- Hardware-in-the-loop validation
- Accessibility algorithm testing
- App robustness evaluation

Ensure compliance with:
- Target app terms of service
- Local privacy regulations
- Institutional review board requirements (if applicable)

---

## References

### HCI Research
- Fitts' Law for pointing accuracy
- Human walking gait analysis (1.5-2.0 Hz typical)
- Touch pressure patterns in mobile interaction
- Cognitive processing time for UI elements

### Android Framework
- Xposed API Documentation
- MotionEvent reference
- SensorManager integration
- Activity lifecycle

### Implementation
- Probabilistic modeling techniques
- Gaussian noise generation
- Thread-safe concurrent patterns
- Hook optimization strategies

---

## Contact & Support

For issues or questions regarding implementation:
- Review LSPosed logs for errors
- Verify device compatibility (Android 10/11, SM-A125U)
- Check module scope configuration
- Ensure target app is force-stopped after installation

---

**End of Implementation Documentation**
