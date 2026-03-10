# Existing Hook Validation and Improvement Plans

## Analysis of 12 Core Hooks Against Proposed Extensions

---

## Hook 1: MechanicalMicroErrorHook

### Current Implementation Status
**File:** `MechanicalMicroErrorHook.java`  
**Coverage:** Fat-finger taps, near-miss corrections, swipe micro-adjustments

### Validation Plan

#### 1. Fidelity Assessment
```java
// Test metrics to validate
public class MechanicalErrorValidation {
    
    // Collect 1000 touch events and verify:
    
    // A. Near-miss rate should be 8-15% (empirical from HCI studies)
    double observedNearMissRate = calculateNearMissRate(events);
    assert observedNearMissRate >= 0.08 && observedNearMissRate <= 0.15 
        : "Near-miss rate outside empirical range";
    
    // B. Correction swipe angle should follow normal distribution
    // around intended target with σ = 15-25 degrees
    double swipeAngleStdDev = calculateSwipeAngleStdDev(events);
    assert swipeAngleStdDev >= 15 && swipeAngleStdDev <= 25
        : "Correction swipe variance unrealistic";
    
    // C. Time-to-correct should be 120-400ms
    double avgCorrectionTime = calculateCorrectionTime(events);
    assert avgCorrectionTime >= 120 && avgCorrectionTime <= 400
        : "Correction timing unrealistic";
}
```

#### 2. Improvements Recommended

**A. Add Grip-Dependent Error Rates**
```java
// In MechanicalMicroErrorHook.java
private static final Map<GripType, Double> GRIP_ERROR_MULTIPLIERS = new HashMap<>();
static {
    GRIP_ERROR_MULTIPLIERS.put(GripType.ONE_HANDED_BASE, 1.0);
    GRIP_ERROR_MULTIPLIERS.put(GripType.ONE_HANDED_HIGH, 1.3); // Less stable
    GRIP_ERROR_MULTIPLIERS.put(GripType.POCKET_RETRIEVAL, 2.2); // Very unstable
    GRIP_TYPE_MULTIPLIERS.put(GripType.TWO_HANDED_TYPE, 0.7); // Most stable
}

// Apply multiplier in error calculation
double baseErrorRate = 0.12;
double adjustedRate = baseErrorRate * GRIP_ERROR_MULTIPLIERS.get(currentGrip);
```

**B. Add Touch Area Fatigue**
```java
// Track repeated taps in same area causing increasing error
private static final Map<String, Integer> areaTapCounts = new ConcurrentHashMap<>();

public static void recordTapAt(float x, float y) {
    String zoneKey = getZoneKey(x, y);
    int count = areaTapCounts.getOrDefault(zoneKey, 0) + 1;
    areaTapCounts.put(zoneKey, count);
    
    // Fatigue increases error rate after 20+ rapid taps
    if (count > 20) {
        currentFatigueErrorMultiplier = 1.0 + ((count - 20) * 0.02);
    }
}
```

**C. Add Screen Edge Avoidance**
```java
// Real users avoid extreme edges due to bezels/cases
private static final float EDGE_MARGIN_PX = 12.0f;

private static boolean isNearScreenEdge(MotionEvent event) {
    float x = event.getX();
    float y = event.getY();
    // A12: 720x1600
    return x < EDGE_MARGIN_PX || x > (720 - EDGE_MARGIN_PX) ||
           y < EDGE_MARGIN_PX || y > (1600 - EDGE_MARGIN_PX);
}
```

---

## Hook 2: SensorFusionCoherenceHook

### Current Implementation Status
**File:** `SensorFusionCoherenceHook.java` (coherence package)  
**Coverage:** Pedestrian dead reckoning, step-cycle noise

### Validation Plan

#### 1. Physics Consistency Check
```java
public class SensorFusionValidation {
    
    // Validate accelerometer-gravity relationship
    public static boolean validateGravityConsistency(float[] accel, float[] gravity) {
        // |accel - gravity| should equal linear acceleration
        float[] linear = new float[3];
        for (int i = 0; i < 3; i++) {
            linear[i] = accel[i] - gravity[i];
        }
        float linearMag = magnitude(linear);
        
        // Should match reported linear acceleration within 5%
        return Math.abs(linearMag - reportedLinearAccel) < 0.5f;
    }
    
    // Validate step detection timing
    // Average human: 1.4-2.0 steps/second while walking
    public static boolean validateStepFrequency(List<Long> stepTimestamps) {
        for (int i = 1; i < stepTimestamps.size(); i++) {
            long interval = stepTimestamps.get(i) - stepTimestamps.get(i-1);
            double freq = 1000.0 / interval; // Hz
            
            if (freq < 1.4 || freq > 2.0) {
                return false; // Outside human range
            }
        }
        return true;
    }
}
```

#### 2. Improvements Recommended

**A. Add Gyroscope Drift Simulation**
```java
// Real MEMS gyroscopes exhibit bias drift over time
private static final float GYRO_DRIFT_RATE_PER_HOUR = 0.5f; // degrees/hour
private static long gyroCalibrationTime = System.currentTimeMillis();
private static float[] gyroBias = new float[3];

public static float[] applyGyroDrift(float[] gyroValues) {
    long elapsedHours = (System.currentTimeMillis() - gyroCalibrationTime) / 3600000;
    
    for (int i = 0; i < 3; i++) {
        // Bias accumulates over time
        gyroBias[i] += (random.nextFloat() - 0.5f) * GYRO_DRIFT_RATE_PER_HOUR * elapsedHours;
        gyroValues[i] += gyroBias[i];
    }
    return gyroValues;
}
```

**B. Add Magnetic Interference Zones**
```java
// Simulate passing through areas with magnetic interference
private static final float[][] INTERFERENCE_ZONES = {
    {37.7858f, -122.4064f, 50.0f}, // Example: downtown SF
};

public static float[] applyMagneticInterference(float[] magnetic, Location loc) {
    for (float[] zone : INTERFERENCE_ZONES) {
        float distance = calculateDistance(loc, zone[0], zone[1]);
        if (distance < zone[2]) { // Within interference radius
            float interferenceStrength = 1.0f - (distance / zone[2]);
            magnetic[0] += interferenceStrength * random.nextGaussian() * 10;
            magnetic[1] += interferenceStrength * random.nextGaussian() * 10;
            magnetic[2] += interferenceStrength * random.nextGaussian() * 5;
        }
    }
    return magnetic;
}
```

**C. Add Barometer Altitude Consistency**
```java
// Barometric altitude must be consistent with GPS altitude
public static float calculateExpectedPressure(float gpsAltitude) {
    // International Standard Atmosphere model
    // Pressure decreases ~12hPa per 100m
    return 1013.25f - (gpsAltitude * 0.12f);
}

public static boolean isBarometerConsistent(float pressure, float gpsAltitude) {
    float expectedPressure = calculateExpectedPressure(gpsAltitude);
    return Math.abs(pressure - expectedPressure) < 20; // Allow 20hPa variance
}
```

---

## Hook 3: InterAppNavigationContextHook

### Current Implementation Status
**File:** `InterAppNavigationHook.java` and `InterAppNavigationContextHook.java`  
**Coverage:** Referral flows, deep links, back stack

### Validation Plan

#### 1. Navigation Pattern Analysis
```java
public class NavigationValidation {
    
    // Realistic app switch patterns based on App Annie data:
    // - Social apps: 15-25 switches per session
    // - Productivity: 5-10 switches per session
    // - Gaming: 2-5 switches per session
    
    public static boolean validateSwitchFrequency(String appCategory, int switchCount) {
        switch (appCategory) {
            case "social":
                return switchCount >= 15 && switchCount <= 25;
            case "productivity":
                return switchCount >= 5 && switchCount <= 10;
            case "game":
                return switchCount >= 2 && switchCount <= 5;
            default:
                return switchCount >= 5 && switchCount <= 15;
        }
    }
    
    // Back button usage should be 30-40% of navigation events
    public static boolean validateBackButtonRatio(int backEvents, int totalNavEvents) {
        double ratio = (double) backEvents / totalNavEvents;
        return ratio >= 0.30 && ratio <= 0.40;
    }
}
```

#### 2. Improvements Recommended

**A. Add Task Switcher Timing**
```java
// Real users pause at task switcher to identify app
private static final int TASK_SWITCHER_PAUSE_MS = 400;

public static void simulateTaskSwitcherPause() {
    if (random.nextDouble() < 0.7) { // 70% pause
        int pauseTime = TASK_SWITCHER_PAUSE_MS + random.nextInt(300);
        SystemClock.sleep(pauseTime);
    }
}
```

**B. Add Deep Link Parameter Validation**
```java
// Ensure deep links have realistic referrer data
public static boolean validateDeepLinkRealism(Intent intent) {
    String referrer = intent.getStringExtra("android.intent.extra.REFERRER");
    
    if (referrer != null) {
        // Check for realistic referrer patterns
        boolean validReferrer = 
            referrer.contains("google.com") ||
            referrer.contains("facebook.com") ||
            referrer.contains("instagram.com") ||
            referrer.startsWith("app://");
        
        return validReferrer;
    }
    return true; // No referrer is valid
}
```

**C. Add Recent Apps LRU Behavior**
```java
// Real recent apps list follows LRU pattern
private static final LinkedHashMap<String, Long> recentAppsLRU = 
    new LinkedHashMap<String, Long>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
            return size() > 7; // Android keeps ~7 recent apps in memory
        }
    };

public static void recordAppUsage(String packageName) {
    recentAppsLRU.put(packageName, System.currentTimeMillis());
}
```

---

## Hook 4: InputPressureDynamicsHook

### Current Implementation Status
**File:** `InputPressureDynamicsHook.java` (xposed package)  
**Coverage:** Touch major/minor, pressure variation

### Validation Plan

#### 1. Biomechanical Accuracy
```java
public class PressureValidation {
    
    // Finger touch characteristics (empirical from touchscreen studies):
    // - Typical contact area: 50-120 mm²
    // - Pressure range: 0.3 - 0.8 normalized
    // - Major axis: 8-15mm
    // - Minor axis: 6-12mm
    // - Eccentricity (major/minor): 1.1 - 1.4
    
    public static boolean validateTouchSize(float major, float minor, float pressure) {
        // Convert pixels to mm (A12: ~270 DPI = ~106 DPcm)
        float majorMm = major / 106.0f * 10;
        float minorMm = minor / 106.0f * 10;
        
        boolean validMajor = majorMm >= 8.0f && majorMm <= 15.0f;
        boolean validMinor = minorMm >= 6.0f && minorMm <= 12.0f;
        boolean validPressure = pressure >= 0.3f && pressure <= 0.8f;
        boolean validEccentricity = (majorMm / minorMm) >= 1.1f && (majorMm / minorMm) <= 1.4f;
        
        return validMajor && validMinor && validPressure && validEccentricity;
    }
}
```

#### 2. Improvements Recommended

**A. Add Pressure-Velocity Correlation**
```java
// Faster movements have lighter touch (less dwell time)
public static float calculatePressureFromVelocity(float velocityPixelsPerSec) {
    // Empirical: fast swipes (1000+ px/s) have ~20% less pressure
    float basePressure = 0.55f;
    float velocityFactor = Math.min(velocityPixelsPerSec / 1000.0f, 1.0f);
    return basePressure * (1.0f - (velocityFactor * 0.2f));
}
```

**B. Add Finger Angle Simulation**
```java
// Different fingers held at different angles
public enum FingerType {
    THUMB(0.8f, 0.6f),      // Thumb: high pressure, more elliptical
    INDEX(0.6f, 0.8f),      // Index: medium pressure, more circular
    MIDDLE(0.55f, 0.85f);   // Middle: lighter, most circular
    
    public final float avgPressure;
    public final float circularity; // 0=elliptical, 1=circular
    
    FingerType(float pressure, float circularity) {
        this.avgPressure = pressure;
        this.circularity = circularity;
    }
}
```

**C. Add Temperature-Dependent Sensitivity**
```java
// Cold fingers have different touch characteristics
private static float ambientTemperatureCelsius = 22.0f;

public static float[] applyTemperatureEffect(float[] touchParams) {
    if (ambientTemperatureCelsius < 10.0f) {
        // Cold fingers: lighter touch, smaller contact area
        touchParams[0] *= 0.9f; // pressure
        touchParams[1] *= 0.95f; // major axis
        touchParams[2] *= 0.95f; // minor axis
    }
    return touchParams;
}
```

---

## Hook 5: AsymmetricLatencyHook

### Current Implementation Status
**File:** `AsymmetricLatencyHook.java`  
**Coverage:** UI processing hesitation after screen load

### Validation Plan

#### 1. Latency Distribution Analysis
```java
public class LatencyValidation {
    
    // Collect 1000 latency measurements
    
    // A. Post-load hesitation should follow exponential decay
    // P(delay > t) = e^(-λt) where λ = 1/300ms for first interaction
    
    public static boolean validateHesitationDistribution(List<Long> latencies) {
        // Fit exponential distribution
        double lambda = calculateLambda(latencies);
        
        // Should be approximately 1/300ms = 0.0033
        return Math.abs(lambda - 0.0033) < 0.001;
    }
    
    // B. Cold start vs warm start difference should be 2-5x
    public static boolean validateColdWarmRatio(long coldStart, long warmStart) {
        double ratio = (double) coldStart / warmStart;
        return ratio >= 2.0 && ratio <= 5.0;
    }
}
```

#### 2. Improvements Recommended

**A. Add Cognitive Load-Dependent Latency**
```java
// Complex UI elements increase processing time
private static final Map<String, Float> UI_COMPLEXITY_WEIGHTS = new HashMap<>();
static {
    UI_COMPLEXITY_WEIGHTS.put("RecyclerView", 1.3f);
    UI_COMPLEXITY_WEIGHTS.put("ViewPager", 1.2f);
    UI_COMPLEXITY_WEIGHTS.put("SurfaceView", 1.5f);
    UI_COMPLEXITY_WEIGHTS.put("WebView", 1.8f);
    UI_COMPLEXITY_WEIGHTS.put("TextureView", 1.4f);
}

public static long calculateUILatency(View view, long baseLatency) {
    String viewClass = view.getClass().getSimpleName();
    float weight = UI_COMPLEXITY_WEIGHTS.getOrDefault(viewClass, 1.0f);
    return (long) (baseLatency * weight);
}
```

**B. Add Memory Pressure Effects**
```java
// Low memory increases GC pauses and lag
private static float memoryPressureLevel = 0.0f; // 0-1

public static long applyMemoryLatency(long baseLatency) {
    if (memoryPressureLevel > 0.7f) {
        // High memory pressure: 50-200% latency increase
        double multiplier = 1.5 + (memoryPressureLevel * 0.5);
        return (long) (baseLatency * multiplier);
    }
    return baseLatency;
}
```

---

## Hook 6: AmbientLightAdaptationHook

### Current Implementation Status
**File:** `AmbientEnvironmentHook.java` covers light  
**Coverage:** Light sensor adjustments

### Validation Plan

#### 1. Physiological Response Timing
```java
public class LightValidation {
    
    // Human pupil response: 200-500ms for constriction, 1-5s for dilation
    
    public static boolean validatePupilResponseTiming(
            float oldLux, float newLux, long responseTimeMs) {
        
        float ratio = newLux / oldLux;
        
        if (ratio > 2.0f) {
            // Brightening - pupil constriction (fast)
            return responseTimeMs >= 200 && responseTimeMs <= 500;
        } else if (ratio < 0.5f) {
            // Darkening - pupil dilation (slow)
            return responseTimeMs >= 1000 && responseTimeMs <= 5000;
        }
        return true; // Small change, no significant response
    }
}
```

---

## Hook 7: BatteryThermalThrottlingHook

### Current Implementation Status
**File:** `ThermalThrottlingHook.java`  
**Coverage:** Battery temperature and throttling

### Validation Plan

#### 1. Thermal Model Accuracy
```java
public class ThermalValidation {
    
    // Battery thermal mass and heat transfer equations
    // dT/dt = (P_loss - P_dissipated) / (mass * specific_heat)
    
    // For A12 5000mAh battery:
    // - Mass: ~75g
    // - Specific heat: ~1100 J/kg·K
    // - Surface area: ~0.012 m²
    // - Heat transfer coefficient: ~10 W/m²·K (convection)
    
    public static float calculateExpectedTemp(float currentTemp, float powerDissipationW, 
                                               long timeDeltaMs) {
        float thermalMass = 0.075f * 1100; // 82.5 J/K
        float heatLoss = 10.0f * 0.012f * (currentTemp - 25.0f); // Convection loss
        float netHeat = powerDissipationW * (timeDeltaMs / 1000.0f) - heatLoss;
        
        return currentTemp + (netHeat / thermalMass);
    }
}
```

---

## Hook 8: NetworkQualityVariationHook

### Current Implementation Status
**File:** `NetworkJitterHook.java` and `DataConnectivityBehaviorHook.java`  
**Coverage:** Network jitter, handover simulation

### Validation Plan

#### 1. Handover Timing
```java
public class HandoverValidation {
    
    // LTE handover timing (3GPP specs):
    // - Measurement report delay: 200ms
    // - Handover execution: 50-100ms
    // - Total interruption: 50-300ms
    
    public static boolean validateHandoverTiming(long handoverDurationMs) {
        return handoverDurationMs >= 50 && handoverDurationMs <= 300;
    }
    
    // During handover, RTT should spike to 500-2000ms
    public static boolean validateHandoverRTT(long rttDuringHandover) {
        return rttDuringHandover >= 500 && rttDuringHandover <= 2000;
    }
}
```

---

## Hook 9: TypographicalErrorsHook

### Current Implementation Status
**File:** `TypingCadenceEngine.java` in motor package  
**Coverage:** Typing cadence and timing

### Validation Plan

#### 1. Error Pattern Analysis
```java
public class TypingValidation {
    
    // Empirical error patterns:
    // - Adjacent key errors: 60% of all errors
    // - Double character errors: 15%
    // - Missed characters: 15%
    // - Other: 10%
    
    public static boolean validateErrorDistribution(List<TypingError> errors) {
        Map<ErrorType, Integer> counts = new HashMap<>();
        for (TypingError e : errors) {
            counts.merge(e.type, 1, Integer::sum);
        }
        
        int total = errors.size();
        double adjacentPct = counts.getOrDefault(ErrorType.ADJACENT, 0) / (double) total;
        
        return adjacentPct >= 0.50 && adjacentPct <= 0.70; // 60% +/- 10%
    }
}
```

---

## Hook 10: MultiTouchGestureImperfectionsHook

### Current Implementation Status
**File:** `GestureComplexityHook.java`  
**Coverage:** Multi-touch errors, asymmetric movement

### Validation Plan

#### 1. Finger Coordination
```java
public class MultiTouchValidation {
    
    // In real two-finger gestures, fingers move at slightly different speeds
    // Variance should be 5-15% between fingers
    
    public static boolean validateFingerCoordination(
            float velocityFinger1, float velocityFinger2) {
        
        double ratio = Math.max(velocityFinger1, velocityFinger2) / 
                      Math.min(velocityFinger1, velocityFinger2);
        
        return ratio >= 1.05 && ratio <= 1.15;
    }
}
```

---

## Hook 11: ProximitySensorCallModeHook

### Current Implementation Status
**File:** `ProximitySensorCallModeHook.java`  
**Coverage:** Proximity sensor during calls

### Validation Plan

#### 1. Call State Consistency
```java
public class ProximityValidation {
    
    // Proximity sensor should be:
    // - NEAR during active call with phone at ear
    // - FAR during speakerphone
    // - Variable during call setup (user positioning phone)
    
    public static boolean validateCallProximityState(
            Call.State callState, boolean speakerOn, float proximityValue) {
        
        if (callState == Call.State.ACTIVE && !speakerOn) {
            // Should be NEAR (0cm) when at ear
            return proximityValue < 1.0f;
        } else if (speakerOn) {
            // Should be FAR when on speaker
            return proximityValue > 5.0f;
        }
        return true;
    }
}
```

---

## Hook 12: MemoryPressureHook

### Current Implementation Status
**File:** `MemoryPressureHook.java`  
**Coverage:** Background process behavior

### Validation Plan

#### 1. LMKD Behavior
```java
public class MemoryValidation {
    
    // Android LMKD kills processes in order:
    // 1. Empty processes (adj 900)
    // 2. Cached processes (adj 800)
    // 3. Services (adj 500)
    // 4. Perceptible (adj 200)
    // 5. Visible (adj 100)
    // 6. Foreground (adj 0)
    
    // Should never kill foreground or visible during normal operation
    
    public static boolean validateKillOrder(List<ProcessKill> kills) {
        int lastAdj = Integer.MAX_VALUE;
        for (ProcessKill kill : kills) {
            if (kill.adj > lastAdj) {
                return false; // Killed lower priority before higher priority
            }
            lastAdj = kill.adj;
        }
        return true;
    }
}
```

---

## Cross-Hook Validation Scenarios

### Scenario: Walking While Using App
```java
// Validates coherence between:
// 1. SensorFusionCoherenceHook (step detection)
// 2. NetworkQualityVariationHook (handover)
// 3. GripHandDominanceHook (grip changes)
// 4. MechanicalMicroErrorHook (walking-induced errors)

public void validateWalkingScenario() {
    // When walking:
    // - Step count should increase
    // - Grip should be ONE_HANDED_BASE or TWO_HANDED_TYPE
    // - Touch errors should increase 20-40%
    // - Network handovers may occur (if urban)
    
    assert stepCount > 0 : "No steps detected during walking scenario";
    assert gripType != GripType.TABLE_PICKUP : "Invalid grip while walking";
    assert touchErrorRate > BASE_ERROR_RATE * 1.2 : "Walking errors not elevated";
}
```

### Scenario: Low Battery Usage
```java
// Validates coherence between:
// 1. BatteryThermalThrottlingHook (low power mode)
// 2. AsymmetricLatencyHook (performance throttling)
// 3. MemoryPressureHook (aggressive cleanup)

public void validateLowBatteryScenario() {
    // When battery < 20%:
    // - CPU should throttle
    // - Background services should be reduced
    // - UI latency should increase
    
    assert cpuFreq < MAX_CPU_FREQ * 0.7 : "CPU not throttled in low battery";
    assert bgServicesCount < NORMAL_BG_COUNT * 0.6 : "Background cleanup not aggressive";
    assert uiLatency > NORMAL_LATENCY * 1.3 : "UI latency not increased";
}
```

---

## Summary of Recommended Improvements

| Hook | Priority | Improvement | Effort |
|------|----------|-------------|--------|
| MechanicalMicroErrorHook | HIGH | Grip-dependent errors | Medium |
| SensorFusionCoherenceHook | HIGH | Gyro drift simulation | Medium |
| InputPressureDynamicsHook | MEDIUM | Temperature effects | Low |
| AsymmetricLatencyHook | MEDIUM | Memory pressure integration | Medium |
| InterAppNavigationContextHook | LOW | Task switcher timing | Low |
| GestureComplexityHook | MEDIUM | Finger angle simulation | High |

---

**Document Version:** 1.0  
**Date:** March 10, 2025
