# Complete Xposed Hook Implementation Reference

## Target Device Specifications
- **Model**: Samsung Galaxy A12 (SM-A125U)
- **Android Version**: 10/11 (API 29-30)
- **Screen**: 720×1600 pixels, 6.5" PLS TFT
- **Processor**: MediaTek Helio P35
- **Xposed API**: 82+ (LSPosed)

## Hook 1: Mechanical Micro-Error Hook

### Classes & Methods Hooked
1. `android.view.View.onTouchEvent(MotionEvent event)`
2. `android.view.ViewRootImpl.dispatchInputEvent(InputEvent event)`

### Implementation Logic

```java
// Fat-finger error detection
if (distanceFromCenter > viewDimension * 0.35) {
    if (random < nearMissProbability) {
        offset.x = (random - 0.5) * viewWidth * 0.15;
        offset.y = (random - 0.5) * viewHeight * 0.15;
        event.offsetLocation(offset.x, offset.y);
    }
}

// Correction swipe during move events
if (velocity > 50 pixels/frame && random < correctionProbability) {
    jitter.x = (random - 0.5) * 8 pixels;
    jitter.y = (random - 0.5) * 8 pixels;
    event.offsetLocation(jitter.x, jitter.y);
}
```

### Key Parameters
- Error Rate: 8% (default)
- Near-Miss Probability: 15% (when touching edges)
- Correction Swipe Probability: 12%
- Max Offset: 15% of view dimension
- Min Offset: 3 pixels

### Telemetry Output
```
[HumanInteraction][MechanicalError] Fat-finger error applied: offset=(5.23,-3.45), viewSize=(120,48)
[HumanInteraction][MechanicalError] Correction swipe applied: jitter=(2.34,-1.87), velocity=67.23
```

---

## Hook 2: Sensor-Fusion Coherence Hook

### Classes & Methods Hooked
1. `android.hardware.SystemSensorManager.SensorEventQueue.dispatchSensorEvent(int handle, float[] values, ...)`
2. `android.location.Location.getSpeed()`
3. `android.location.Location.setSpeed(float speed)`

### Implementation Logic

```java
// Walking detection
if (0.5 < gpsSpeed < 3.0) {
    isWalking = true;
    currentSpeed = gpsSpeed;
}

// Step-cycle oscillation calculation
double phase = 2 * PI * 1.8 * (currentTime - startTime) / 1000;
double speedFactor = currentSpeed / 1.5;

// Accelerometer modification
accelX += 2.5 * speedFactor * sin(phase);
accelY += 2.5 * speedFactor * sin(phase + PI/4);
accelZ += 2.5 * speedFactor * cos(phase * 2);

// Gyroscope modification
gyroX += 0.3 * speedFactor * sin(phase + PI/6);
gyroY += 0.3 * speedFactor * cos(phase + PI/3);
gyroZ += 0.3 * speedFactor * sin(phase * 1.5);
```

### Key Parameters
- Walking Speed Threshold: 0.5-3.0 m/s
- Step Frequency: 1.8 Hz (typical walking cadence)
- Accelerometer Amplitude: ±2.5 m/s² (speed-scaled)
- Gyroscope Amplitude: ±0.3 rad/s (speed-scaled)
- Noise: Gaussian (σ=0.03-0.2)

### Sensor Type Mapping
- Handle 0: Accelerometer (TYPE_ACCELEROMETER)
- Handle 1: Gyroscope (TYPE_GYROSCOPE)
- Handle 3: Linear Acceleration (TYPE_LINEAR_ACCELERATION)
- Handle 4: Gravity (TYPE_GRAVITY)

### Telemetry Output
```
[HumanInteraction][SensorFusion] Walking detected from GPS: speed=1.45 m/s
[HumanInteraction][SensorFusion] Walking accel applied: speed=1.45, phase=4.73, accel=(2.34,-1.87,9.82)
[HumanInteraction][SensorFusion] Walking gyro applied: speed=1.45, phase=4.73, gyro=(0.234,-0.187,0.156)
```

---

## Hook 3: Inter-App Navigation Hook

### Classes & Methods Hooked
1. `android.app.Instrumentation.execStartActivity(...)`
2. `android.app.Activity.onResume()`
3. `android.app.TaskStackBuilder.startActivities(...)`

### Implementation Logic

```java
// Deep-link context injection
if (intent.action == ACTION_VIEW && random < deepLinkProbability) {
    extras.putString("android.intent.extra.REFERRER", "android-app://" + referrerPackage);
    extras.putString("android.intent.extra.REFERRER_NAME", referrerPackage);
    extras.putLong("android.intent.extra.REFERRER_TIME", System.currentTimeMillis());
    extras.putString("utm_source", generateUTM(referrerPackage));
    extras.putString("utm_medium", random < 0.6 ? "social" : "referral");
}

// Referral flow simulation
if (random < referralFlowProbability) {
    extras.putString("referrer_package", selectReferrer());
    extras.putLong("referral_timestamp", System.currentTimeMillis());
    extras.putLong("time_since_referral_ms", timeSinceLastSwitch);
}

// Task stack modification
if (!backStack.isEmpty() && random < stackModificationProbability) {
    extras.putString("stack_context", "simulated_back_navigation");
    extras.putString("previous_package", backStack.peek().packageName);
    extras.putLong("previous_intent_time", backStack.peek().timestamp);
}
```

### Key Parameters
- Referral Flow Probability: 18%
- Deep Link Probability: 12%
- Back Stack Modification Probability: 15%
- Stack Size Limit: 20 entries

### Referrer Distribution
- com.android.browser: 35%
- com.chrome.browser: 30%
- com.instagram.android: 15%
- com.facebook.katana: 12%
- com.twitter.android: 8%

### Telemetry Output
```
[HumanInteraction][InterAppNav] Injected referral context: referrer=com.android.browser, utm_source=android_browser
[HumanInteraction][InterAppNav] Simulated referral flow: referrer=com.instagram.android, intent=android.intent.action.VIEW
[HumanInteraction][InterAppNav] Modified task stack: previous=com.ss.android.ugc.trill, current=ComponentInfo{com.zhiliaoapp.musically/...}
```

---

## Hook 4: Input Pressure & Surface Area Dynamics Hook

### Classes & Methods Hooked
1. `android.view.MotionEvent.obtain(...)`
2. `android.view.View.onTouchEvent(MotionEvent event)`
3. `android.view.GestureDetector.onScroll(...)`

### Implementation Logic

```java
// Pressure calculation by action
switch (actionMasked) {
    case ACTION_DOWN:
        pressure = 0.85 + random * 0.12;  // 0.85-0.97
        break;
    case ACTION_MOVE:
        pressure = Math.max(0.3, 0.7 - (velocity * 0.01));
        pressure += (random - 0.5) * 0.1;
        break;
    case ACTION_UP:
        pressure = 0.15 + random * 0.2;   // 0.15-0.35
        break;
}

// Surface area (touch size)
size = baseSize + random * variation;
touchMajor = size * 0.9 + random * 0.1;
touchMinor = size * (0.7 + random * 0.2);

// Scrolling-specific dynamics
if (isScrolling && random < 0.6) {
    pressure = 0.4 + random * 0.25;
    touchMajor = 0.8 + random * 0.6;
    touchMinor = 0.6 + random * 0.4;
}
```

### Key Parameters
- Pressure Variation Probability: 25%
- Surface Area Variation Probability: 22%
- Scrolling Pressure: 0.4-0.65
- Tap Pressure: 0.85-0.97
- Touch Major Range: 0.1-1.0
- Touch Minor Range: 0.1-1.0

### Interaction Classification
- Tap: distance < 15 pixels
- Scroll: distance > 50 pixels
- Long Press: duration > 500ms

### Telemetry Output
```
[HumanInteraction][InputPressure] Touch dynamics applied: pressure=0.923, touchMajor=0.87, touchMinor=0.72, action=0
[HumanInteraction][InputPressure] Interaction classified: type=tap, distance=8.34, duration=234ms
[HumanInteraction][InputPressure] Scrolling dynamics applied: pressure=0.512, touchMajor=1.12, touchMinor=0.83
```

---

## Hook 5: Asymmetric Latency Hook

### Classes & Methods Hooked
1. `android.app.Activity.onResume()`
2. `android.app.Fragment.onResume()`
3. `android.view.Choreographer.postCallback(int callbackType, ...)`
4. `android.view.ViewRootImpl.dispatchInputEvent(InputEvent event)`

### Implementation Logic

```java
// Load latency calculation
baseLatency = 250ms;
complexityMultiplier = isActivity ? (hasList ? 1.4 : 1.0) : 0.8;
randomFactor = 0.8 + random * 0.4;
loadLatency = baseLatency * complexityMultiplier * randomFactor;

// Post-load hesitation
if (random < hesitationProbability) {
    hesitationDelay = loadLatency * (0.2 + random * 0.3);
    blockInput(true);
    handler.postDelayed(() -> blockInput(false), hesitationDelay);
}

// Processing hesitation on input
if (timeSinceLastInteraction > 500ms && random < hesitationProbability) {
    cognitiveLoadFactor = getCognitiveLoadFactor(recentViewLoadTime);
    hesitationDelay = 200ms * cognitiveLoadFactor * (0.7 + random * 0.6);
    blockInput(true);
    handler.postDelayed(() -> blockInput(false), hesitationDelay);
}

// Input blocking
if (inputBlocked) {
    return null;  // Discard input
}
```

### Key Parameters
- Base Latency: 250ms
- Hesitation Probability: 30%
- Complexity Multipliers: 0.8-1.4
- Cognitive Load Factors: 1.0-1.6
- Hesitation Ratio: 20-50% of load latency

### Cognitive Load Factor
- View load < 1s ago: ×1.6
- View load 1-3s ago: ×1.3
- View load 3-10s ago: ×1.1
- View load > 10s ago: ×1.0

### Telemetry Output
```
[HumanInteraction][AsymmetricLatency] Activity resumed: com.example.MainActivity, latency=312ms
[HumanInteraction][AsymmetricLatency] Processing hesitation triggered: delay=145ms
[HumanInteraction][AsymmetricLatency] Post-load hesitation applied for com.example.DetailActivity: delay=87ms (load=312ms)
[HumanInteraction][AsymmetricLatency] Input blocked during processing hesitation
```

---

## Configuration API

### Complete API Reference

```java
// Global control
HumanInteractionModule.ConfigurationAPI.enableAllHooks();
HumanInteractionModule.ConfigurationAPI.disableAllHooks();

// Preset modes
HumanInteractionModule.ConfigurationAPI.setHighFidelityMode(true);    // Maximum realism
HumanInteractionModule.ConfigurationAPI.setHighFidelityMode(false);   // Standard mode
HumanInteractionModule.ConfigurationAPI.setMinimalInterferenceMode(); // Minimal impact

// Individual hook configuration
HumanInteractionModule.ConfigurationAPI.setMechanicalErrorRate(0.08);
HumanInteractionModule.ConfigurationAPI.setSensorFusionWalkingState(true, 1.5);
HumanInteractionModule.ConfigurationAPI.setReferralFlowProbability(0.18);
HumanInteractionModule.ConfigurationAPI.setTouchPressureVariation(0.25);
HumanInteractionModule.ConfigurationAPI.setHesitationProbability(0.30);
```

### Preset Mode Comparison

| Parameter | High-Fidelity | Standard | Minimal |
|-----------|---------------|----------|---------|
| Error Rate | 10% | 5% | 2% |
| Hesitation Probability | 35% | 20% | 8% |
| Referral Flow Probability | 20% | 10% | 3% |
| Pressure Variation | 30% | 15% | 5% |
| Surface Area Variation | 28% | 12% | 5% |
| Walking Indication | 30% | 15% | 5% |

---

## Telemetry Synchronization

### Timestamp Strategy
```java
long systemTime = System.currentTimeMillis();
long eventTime = event.getEventTime();  // For MotionEvent
long sensorTime = sensorEvent.timestamp; // Hardware timestamp
```

### State Sharing
```java
// Walking state (shared across sensors)
private static boolean isWalking = false;
private static double currentWalkingSpeed = 0.0;

// Touch state (shared across events)
private static long lastDownTime = 0;
private static float lastX = 0;
private static float lastY = 0;

// Navigation history (global stack)
private static Stack<IntentEntry> intentBackStack;

// View load timing (for latency)
private static ConcurrentHashMap<String, ViewLoadInfo> viewLoadTimes;
```

---

## Thread Safety

### Thread-Safe Random Generation
```java
private static final ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new Random());
```

### Atomic Operations
```java
private static final AtomicBoolean inputBlocked = new AtomicBoolean(false);
private static final AtomicLong lastInteractionTime = new AtomicLong();
```

### Concurrent Collections
```java
private static final ConcurrentHashMap<String, ViewLoadInfo> viewLoadTimes;
```

### Main Thread Handler
```java
private static Handler latencyHandler = new Handler(Looper.getMainLooper());
```

---

## Performance Characteristics

### Per-Event Overhead
- Mechanical Micro-Error: < 1ms
- Sensor-Fusion: < 0.5ms
- Navigation: < 2ms
- Pressure Dynamics: < 1ms
- Latency: Intentional delay (feature)

### Memory Footprint
- Back stack: 20 entries × ~100 bytes = ~2KB
- View load history: ~50 entries × ~200 bytes = ~10KB
- Total additional memory: < 5MB

### CPU Usage
- Idle: < 0.1%
- Active: < 0.5% (excluding intentional latency delays)

---

## Verification Checklist

### Pre-Installation
- [ ] Device rooted with Magisk
- [ ] LSPosed Framework installed and activated
- [ ] Android version 10/11 confirmed
- [ ] Target apps identified (package names)

### Post-Installation
- [ ] APK installed successfully
- [ ] Module enabled in LSPosed
- [ ] Scope configured with target apps
- [ ] Target apps force-stopped
- [ ] Device rebooted

### Functional Verification
- [ ] LSPosed logs show initialization
- [ ] Mechanical errors observable in logs
- [ ] Sensor oscillations visible (with GPS speed)
- [ ] Referral context present in Intent extras
- [ ] Touch pressure varies by action type
- [ ] Latency delays occur after UI loads

### Telemetry Verification
- [ ] Timestamps synchronized across hooks
- [ ] Sensor data coherent with GPS speed
- [ ] Touch parameters realistic ranges
- [ ] Latency delays correlate with complexity

---

## Troubleshooting

### Common Issues

**Module not activating**
- Check LSPosed logs for errors
- Verify scope includes target apps
- Force-stop and reboot

**No sensor oscillations**
- Verify GPS speed > 0.5 m/s
- Check walking state in logs
- Ensure SensorFusionCoherenceHook enabled

**App crashes**
- Check LSPosed logs for stack traces
- Try disabling specific hooks
- Verify Android version compatibility

**No latency effects**
- Verify AsymmetricLatencyHook enabled
- Check hesitation probability setting
- Monitor logs for hesitation messages

---

## Build & Deploy

### Quick Build
```bash
./gradlew assembleRelease
```

### Install
```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

### Verify
```bash
adb logcat | grep "HumanInteraction"
```

---

**End of Complete Implementation Reference**
