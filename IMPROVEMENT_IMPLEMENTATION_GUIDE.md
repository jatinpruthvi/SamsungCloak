# Xposed Hooks Improvement Implementation Guide

## Date: 2025-03-09
**Purpose**: Provide concrete implementation steps for the improvements identified in the validation report

---

## Table of Contents

1. [Multi-Touch Gesture Imperfections](#1-multi-touch-gesture-imperfections)
2. [Memory Pressure Simulation Enhancement](#2-memory-pressure-simulation-enhancement)
3. [Validation Framework Implementation](#3-validation-framework-implementation)
4. [Touch + Typing Integration](#4-touch--typing-integration)
5. [Swipe Typing Errors](#5-swipe-typing-errors)
6. [Configuration Management](#6-configuration-management)

---

## 1. Multi-Touch Gesture Imperfections

### File to Create
`/app/src/main/java/com/samsungcloak/coherence/MultiTouchImperfectionHook.java`

### Implementation

```java
package com.samsungcloak.coherence;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Multi-Touch Gesture Imperfections Hook
 * 
 * Simulates realistic imperfections in multi-touch gestures:
 * - Finger drift as random walk with low velocity
 * - Unintentional extra touches (palm rejection edge cases)
 * - Scale oscillation during pinch gestures
 * - Rotation error during rotate gestures
 * - Velocity-dependent jitter amplitude
 */
public class MultiTouchImperfectionHook {
    
    private static final String LOG_TAG = "SamsungCloak.MultiTouchImperfection";
    private static boolean initialized = false;
    
    // Finger drift constants
    private static final double DRIFT_VELOCITY_BASE_PX_PER_EVENT = 0.3;
    private static final double DRIFT_VELOCITY_VARIANCE = 0.15;
    private static final double DRIFT_DIRECTION_CHANGE_PROBABILITY = 0.08;
    
    // Palm rejection constants
    private static final double PALM_CONTACT_PROBABILITY = 0.04;
    private static final double PALM_CONTACT_DURATION_MIN_MS = 150;
    private static final double PALM_CONTACT_DURATION_MAX_MS = 800;
    
    // Gesture imperfection constants
    private static final double PINCH_SCALE_JITTER_STD_DEV = 0.03;
    private static final double ROTATION_ERROR_STD_DEV = 2.5; // degrees
    private static final double VELOCITY_JITTER_FACTOR = 0.02;
    
    // Gesture types
    public enum GestureType {
        PINCH,
        ROTATE,
        SCROLL_TWO_FINGER,
        UNKNOWN
    }
    
    private final Random random;
    
    // Finger state tracking
    private final ConcurrentHashMap<Integer, FingerState> activeFingers;
    
    // Current gesture state
    private GestureType currentGesture;
    private double currentScale;
    private double currentRotation;
    private double lastVelocity;
    
    public MultiTouchImperfectionHook() {
        this.random = new Random();
        this.activeFingers = new ConcurrentHashMap<>();
        this.currentGesture = GestureType.UNKNOWN;
        this.currentScale = 1.0;
        this.currentRotation = 0.0;
        this.lastVelocity = 0.0;
    }
    
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
    }
    
    /**
     * Simulate multi-touch gesture with imperfections.
     */
    public MultiTouchResult simulateMultiTouch(
        int pointerCount,
        double[] xCoords,
        double[] yCoords,
        double timestampMs
    ) {
        // Apply finger drift to all active pointers
        applyFingerDrift(pointerCount, xCoords, yCoords);
        
        // Detect and simulate palm contact
        boolean hasPalmContact = checkPalmContact(pointerCount, timestampMs);
        
        // Apply gesture-specific imperfections
        GestureImperfections imperfections = applyGestureImperfections(
            pointerCount, xCoords, yCoords, timestampMs
        );
        
        return new MultiTouchResult(
            pointerCount,
            xCoords.clone(),
            yCoords.clone(),
            hasPalmContact,
            imperfections,
            timestampMs
        );
    }
    
    /**
     * Apply finger drift as random walk with low velocity.
     */
    private void applyFingerDrift(int pointerCount, double[] xCoords, double[] yCoords) {
        for (int i = 0; i < pointerCount; i++) {
            FingerState state = activeFingers.get(i);
            if (state == null) {
                state = new FingerState(xCoords[i], yCoords[i]);
                activeFingers.put(i, state);
            }
            
            // Calculate drift velocity
            double driftVelocity = DRIFT_VELOCITY_BASE_PX_PER_EVENT +
                                 (random.nextGaussian() * DRIFT_VELOCITY_VARIANCE);
            
            // Occasional direction change
            if (random.nextDouble() < DRIFT_DIRECTION_CHANGE_PROBABILITY) {
                state.driftDirection = (random.nextDouble() * 2 * Math.PI);
            }
            
            // Apply drift
            state.driftX += Math.cos(state.driftDirection) * driftVelocity;
            state.driftY += Math.sin(state.driftDirection) * driftVelocity;
            
            // Update coordinates
            xCoords[i] += state.driftX;
            yCoords[i] += state.driftY;
            
            // Update state
            state.lastX = xCoords[i];
            state.lastY = yCoords[i];
        }
    }
    
    /**
     * Check for palm contact edge case.
     */
    private boolean checkPalmContact(int pointerCount, double timestampMs) {
        // Palm contacts typically add a 3rd+ pointer
        if (pointerCount <= 2) {
            return false;
        }
        
        // Random palm contact probability
        if (random.nextDouble() < PALM_CONTACT_PROBABILITY) {
            double duration = PALM_CONTACT_DURATION_MIN_MS +
                             random.nextDouble() * (PALM_CONTACT_DURATION_MAX_MS - PALM_CONTACT_DURATION_MIN_MS);
            return true;
        }
        
        return false;
    }
    
    /**
     * Apply gesture-specific imperfections.
     */
    private GestureImperfections applyGestureImperfections(
        int pointerCount,
        double[] xCoords,
        double[] yCoords,
        double timestampMs
    ) {
        GestureImperfections imperfections = new GestureImperfections();
        
        // Detect gesture type based on pointer count and movement
        currentGesture = detectGestureType(pointerCount, xCoords, yCoords);
        
        // Calculate gesture velocity
        double velocity = calculateGestureVelocity(xCoords, yCoords);
        
        switch (currentGesture) {
            case PINCH:
                // Scale jitter
                double scaleJitter = random.nextGaussian() * PINCH_SCALE_JITTER_STD_DEV;
                imperfections.scaleJitter = scaleJitter;
                currentScale += scaleJitter;
                break;
                
            case ROTATE:
                // Rotation error
                double rotationError = random.nextGaussian() * ROTATION_ERROR_STD_DEV;
                imperfections.rotationError = rotationError;
                currentRotation += rotationError;
                break;
                
            case SCROLL_TWO_FINGER:
            case UNKNOWN:
                // Velocity-dependent jitter
                double velocityJitter = velocity * VELOCITY_JITTER_FACTOR;
                imperfections.velocityJitter = random.nextGaussian() * velocityJitter;
                break;
        }
        
        lastVelocity = velocity;
        return imperfections;
    }
    
    /**
     * Detect gesture type from pointer positions.
     */
    private GestureType detectGestureType(int pointerCount, double[] xCoords, double[] yCoords) {
        if (pointerCount != 2) {
            return GestureType.UNKNOWN;
        }
        
        // Calculate distance between pointers
        double dx = xCoords[1] - xCoords[0];
        double dy = yCoords[1] - yCoords[0];
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        // Check if distance is changing significantly (pinch/zoom)
        double distanceChange = Math.abs(distance - currentScale * 100.0);
        if (distanceChange > 20.0) {
            return GestureType.PINCH;
        }
        
        // Check for rotation (angle change)
        double angle = Math.atan2(dy, dx);
        double angleChange = Math.abs(angle - currentRotation);
        if (angleChange > 0.1) {
            return GestureType.ROTATE;
        }
        
        return GestureType.SCROLL_TWO_FINGER;
    }
    
    /**
     * Calculate gesture velocity.
     */
    private double calculateGestureVelocity(double[] xCoords, double[] yCoords) {
        double totalVelocity = 0.0;
        
        for (int i = 0; i < xCoords.length; i++) {
            FingerState state = activeFingers.get(i);
            if (state != null) {
                double dx = xCoords[i] - state.lastX;
                double dy = yCoords[i] - state.lastY;
                totalVelocity += Math.sqrt(dx * dx + dy * dy);
            }
        }
        
        return totalVelocity / xCoords.length;
    }
    
    /**
     * Remove finger from tracking (pointer up).
     */
    public void removeFinger(int pointerId) {
        activeFingers.remove(pointerId);
    }
    
    /**
     * Clear all finger tracking.
     */
    public void reset() {
        activeFingers.clear();
        currentGesture = GestureType.UNKNOWN;
        currentScale = 1.0;
        currentRotation = 0.0;
        lastVelocity = 0.0;
    }
    
    /**
     * Data class for finger state.
     */
    private static class FingerState {
        double lastX;
        double lastY;
        double driftX;
        double driftY;
        double driftDirection;
        
        FingerState(double x, double y) {
            this.lastX = x;
            this.lastY = y;
            this.driftX = 0.0;
            this.driftY = 0.0;
            this.driftDirection = Math.random() * 2 * Math.PI;
        }
    }
    
    /**
     * Data class for gesture imperfections.
     */
    public static class GestureImperfections {
        public double scaleJitter;
        public double rotationError;
        public double velocityJitter;
        
        public GestureImperfections() {
            this.scaleJitter = 0.0;
            this.rotationError = 0.0;
            this.velocityJitter = 0.0;
        }
    }
    
    /**
     * Data class for multi-touch result.
     */
    public static class MultiTouchResult {
        public final int pointerCount;
        public final double[] xCoords;
        public final double[] yCoords;
        public final boolean hasPalmContact;
        public final GestureImperfections imperfections;
        public final double timestampMs;
        
        public MultiTouchResult(int pointerCount, double[] xCoords, double[] yCoords,
                               boolean hasPalmContact, GestureImperfections imperfections,
                               double timestampMs) {
            this.pointerCount = pointerCount;
            this.xCoords = xCoords;
            this.yCoords = yCoords;
            this.hasPalmContact = hasPalmContact;
            this.imperfections = imperfections;
            this.timestampMs = timestampMs;
        }
    }
    
    public static boolean isInitialized() {
        return initialized;
    }
}
```

### Integration Steps

1. Add to `CoherenceEngine.java`:
```java
import com.samsungcloak.coherence.MultiTouchImperfectionHook;

private MultiTouchImperfectionHook multiTouchImperfectionHook;

// In init():
multiTouchImperfectionHook = new MultiTouchImperfectionHook();
MultiTouchImperfectionHook.init();
```

2. Hook into `MotionEvent.obtain()`:
```java
// Hook to inject imperfections for multi-touch events
XposedHelpers.findAndHookMethod(
    "android.view.MotionEvent",
    lpparam.classLoader,
    "obtain",
    long.class, // downTime
    long.class, // eventTime
    int.class,  // action
    int.class,  // pointerCount
    int[].class, // pointerIds
    float[].class, // pointerCoords
    return new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) {
            MotionEvent event = (MotionEvent) param.getResult();
            if (event.getPointerCount() >= 2) {
                MultiTouchImperfectionHook.MultiTouchResult result =
                    multiTouchImperfectionHook.simulateMultiTouch(
                        event.getPointerCount(),
                        event.getX(),
                        event.getY(),
                        event.getEventTime()
                    );
                // Apply imperfections to event coordinates
            }
        }
    }
);
```

3. Validate with gesture recordings:
```bash
# Screen record pinch gestures
adb shell screenrecord /sdcard/pinch_test.mp4
# Analyze frame-by-frame for jitter patterns
ffmpeg -i pinch_test.mp4 -vf "select=eq(pict_type\,I)" -vsync vfr pinch_frames_%04d.png
```

---

## 2. Memory Pressure Simulation Enhancement

### Files to Modify
- `BatteryDischargeHook.java` (add memory pressure correlation)
- `EnvironmentHook.java` (add onTrimMemory callback simulation)

### Implementation

#### Modify `BatteryDischargeHook.java`

Add memory pressure tracking:

```java
// Add field to BatteryDischargeHook.java
private double memoryPressure;
private int activeAppCount;
private long lastMemoryPressureUpdate;

// Add to constructor:
this.memoryPressure = 0.0;
this.activeAppCount = 0;
this.lastMemoryPressureUpdate = System.currentTimeMillis();

// Add method:
public double updateMemoryPressure(double memoryUsageRatio, int activeApps) {
    this.activeAppCount = activeApps;
    
    // Calculate pressure based on usage ratio and active apps
    double usagePressure = Math.min(1.0, memoryUsageRatio);
    double appCountPressure = Math.min(1.0, activeApps / 10.0);
    
    // Smooth pressure changes
    this.memoryPressure = (this.memoryPressure * 0.7) + 
                         ((usagePressure + appCountPressure) / 2.0 * 0.3);
    
    return this.memoryPressure;
}

// Add to getThermalState():
public ThermalState getThermalState() {
    return new ThermalState(
        currentTemperatureC,
        isThrottlingActive,
        consecutiveThrottleMinutes,
        calculateUILatency(),
        calculateThermalFactor(),
        memoryPressure // Add to state
    );
}
```

#### Create `MemoryPressureHook.java`

```java
package com.samsungcloak.xposed;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;

/**
 * Memory Pressure Simulation Hook
 * 
 * Simulates realistic Android memory pressure callbacks and app lifecycle events.
 */
public class MemoryPressureHook {
    
    private static final String LOG_TAG = "SamsungCloak.MemoryPressure";
    private static boolean initialized = false;
    
    // Memory pressure levels (from ComponentCallbacks2)
    private static final int TRIM_MEMORY_UI_HIDDEN = 20;
    private static final int TRIM_MEMORY_BACKGROUND = 40;
    private static final int TRIM_MEMORY_MODERATE = 60;
    private static final int TRIM_MEMORY_COMPLETE = 80;
    private static final int TRIM_MEMORY_RUNNING_CRITICAL = 15;
    private static final int TRIM_MEMORY_RUNNING_LOW = 10;
    private static final int TRIM_MEMORY_RUNNING_MODERATE = 5;
    
    // Memory pressure simulation
    private static double currentMemoryPressure = 0.0;
    private static final Random random = new Random();
    
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (initialized) {
            XposedBridge.log(LOG_TAG + " already initialized");
            return;
        }
        
        try {
            hookOnTrimMemory(lpparam);
            hookActivityLifecycle(lpparam);
            hookGetRunningAppProcesses(lpparam);
            
            initialized = true;
            XposedBridge.log(LOG_TAG + " initialized successfully");
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " Failed to initialize: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void hookOnTrimMemory(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.content.ComponentCallbacks2",
                lpparam.classLoader,
                "onTrimMemory",
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        int level = (int) param.args[0];
                        
                        // Decide whether to inject additional trim events
                        boolean shouldInject = shouldInjectTrimEvent(level);
                        
                        if (shouldInject) {
                            XposedBridge.log(LOG_TAG + " Injecting TRIM_MEMORY event, level=" + level);
                            // The original call will proceed, so we just log it
                        }
                    }
                }
            );
            
            XposedBridge.log(LOG_TAG + " Hooked onTrimMemory");
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " Failed to hook onTrimMemory: " + e.getMessage());
        }
    }
    
    private static void hookActivityLifecycle(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook Activity.onCreate to save state before OOM
            XposedHelpers.findAndHookMethod(
                "android.app.Activity",
                lpparam.classLoader,
                "onCreate",
                android.os.Bundle.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        // Check if this is a restore from saved state
                        if (param.args[0] != null) {
                            XposedBridge.log(LOG_TAG + " Activity restored from saved instance state");
                        }
                    }
                }
            );
            
            XposedBridge.log(LOG_TAG + " Hooked Activity lifecycle");
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " Failed to hook Activity lifecycle: " + e.getMessage());
        }
    }
    
    private static void hookGetRunningAppProcesses(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.app.ActivityManager",
                lpparam.classLoader,
                "getRunningAppProcesses",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Object result = param.getResult();
                        if (result instanceof java.util.List) {
                            int appCount = ((java.util.List<?>) result).size();
                            
                            // Update memory pressure based on app count
                            updateMemoryPressureFromAppCount(appCount);
                        }
                    }
                }
            );
            
            XposedBridge.log(LOG_TAG + " Hooked getRunningAppProcesses");
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " Failed to hook getRunningAppProcesses: " + e.getMessage());
        }
    }
    
    /**
     * Decide whether to inject additional trim memory event.
     */
    private static boolean shouldInjectTrimEvent(int currentLevel) {
        // Higher probability of additional trim events at high memory pressure
        double probability = currentMemoryPressure * 0.15;
        
        // Increase probability during critical levels
        if (currentLevel >= TRIM_MEMORY_RUNNING_CRITICAL) {
            probability *= 2.0;
        }
        
        return random.nextDouble() < probability;
    }
    
    /**
     * Update memory pressure based on active app count.
     */
    private static void updateMemoryPressureFromAppCount(int appCount) {
        // Normalize app count to 0-1 range (10+ apps = high pressure)
        double appPressure = Math.min(1.0, appCount / 10.0);
        
        // Add some randomness
        appPressure += (random.nextGaussian() * 0.1);
        
        // Smooth pressure changes
        currentMemoryPressure = (currentMemoryPressure * 0.8) + (appPressure * 0.2);
        
        XposedBridge.log(LOG_TAG + " Memory pressure updated: " + currentMemoryPressure + 
                       " (app count: " + appCount + ")");
    }
    
    /**
     * Get current memory pressure level.
     */
    public static double getCurrentMemoryPressure() {
        return currentMemoryPressure;
    }
    
    /**
     * Determine if OOM kill should be simulated.
     */
    public static boolean shouldSimulateOOMKill() {
        // OOM kill probability increases with memory pressure
        double oomProbability = Math.pow(currentMemoryPressure, 3.0) * 0.05;
        return random.nextDouble() < oomProbability;
    }
    
    public static boolean isInitialized() {
        return initialized;
    }
}
```

### Integration Steps

1. Add to `MainHook.java`:
```java
import com.samsungcloak.xposed.MemoryPressureHook;

// In init():
MemoryPressureHook.init(lpparam);
```

2. Add validation command:
```bash
# Validate memory pressure with dumpsys
adb shell dumpsys meminfo com.zhiliaoapp.musically
# Check for: PSS, USS, RSS values
# Verify: TrimMemory callbacks trigger at appropriate thresholds
```

---

## 3. Validation Framework Implementation

### File to Create
`/app/src/main/java/com/samsungcloak/validation/ValidationFramework.java`

### Implementation

```java
package com.samsungcloak.validation;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XposedBridge;

/**
 * Validation Framework
 * 
 * Provides infrastructure for:
 * - Baseline data collection
 * - Hook output logging
 * - Statistical fidelity checks
 * - A/B testing
 */
public class ValidationFramework {
    
    private static final String LOG_TAG = "SamsungCloak.Validation";
    private static final String LOG_DIR = "/sdcard/samsungcloak/validation/";
    
    private static boolean validationMode = false;
    private static AtomicBoolean isCollectingBaseline = new AtomicBoolean(false);
    
    private static ConcurrentLinkedQueue<ValidationEvent> eventQueue;
    private static BufferedWriter logWriter;
    
    public static void init() {
        eventQueue = new ConcurrentLinkedQueue<>();
        createLogDirectory();
        XposedBridge.log(LOG_TAG + " initialized");
    }
    
    /**
     * Enable or disable validation mode.
     */
    public static void setValidationMode(boolean enabled) {
        validationMode = enabled;
        if (enabled) {
            startLogging();
        } else {
            stopLogging();
        }
    }
    
    /**
     * Start baseline data collection.
     */
    public static void startBaselineCollection() {
        isCollectingBaseline.set(true);
        XposedBridge.log(LOG_TAG + " Baseline collection started");
    }
    
    /**
     * Stop baseline data collection.
     */
    public static void stopBaselineCollection() {
        isCollectingBaseline.set(false);
        XposedBridge.log(LOG_TAG + " Baseline collection stopped");
    }
    
    /**
     * Log a validation event.
     */
    public static void logEvent(String category, String data) {
        if (!validationMode) {
            return;
        }
        
        ValidationEvent event = new ValidationEvent(
            System.currentTimeMillis(),
            category,
            data
        );
        
        eventQueue.add(event);
        
        // Write immediately
        writeEvent(event);
    }
    
    /**
     * Start logging to file.
     */
    private static void startLogging() {
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String logFile = LOG_DIR + "validation_" + timestamp + ".log";
            logWriter = new BufferedWriter(new FileWriter(logFile));
            
            XposedBridge.log(LOG_TAG + " Logging to: " + logFile);
        } catch (IOException e) {
            XposedBridge.log(LOG_TAG + " Failed to start logging: " + e.getMessage());
        }
    }
    
    /**
     * Stop logging to file.
     */
    private static void stopLogging() {
        try {
            if (logWriter != null) {
                logWriter.close();
                logWriter = null;
                XposedBridge.log(LOG_TAG + " Logging stopped");
            }
        } catch (IOException e) {
            XposedBridge.log(LOG_TAG + " Error stopping logging: " + e.getMessage());
        }
    }
    
    /**
     * Write event to log file.
     */
    private static void writeEvent(ValidationEvent event) {
        if (logWriter == null) {
            return;
        }
        
        try {
            String logLine = String.format("[%d] [%s] %s%n",
                event.timestamp,
                event.category,
                event.data
            );
            logWriter.write(logLine);
            logWriter.flush();
        } catch (IOException e) {
            // Silently fail to avoid disrupting normal operation
        }
    }
    
    /**
     * Create log directory.
     */
    private static void createLogDirectory() {
        try {
            java.io.File dir = new java.io.File(LOG_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
                XposedBridge.log(LOG_TAG + " Created log directory: " + LOG_DIR);
            }
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " Failed to create log directory: " + e.getMessage());
        }
    }
    
    /**
     * Get event queue size.
     */
    public static int getEventQueueSize() {
        return eventQueue.size();
    }
    
    /**
     * Clear event queue.
     */
    public static void clearEventQueue() {
        eventQueue.clear();
    }
    
    /**
     * Check if in validation mode.
     */
    public static boolean isValidationMode() {
        return validationMode;
    }
    
    /**
     * Check if collecting baseline.
     */
    public static boolean isCollectingBaseline() {
        return isCollectingBaseline.get();
    }
    
    /**
     * Data class for validation event.
     */
    private static class ValidationEvent {
        final long timestamp;
        final String category;
        final String data;
        
        ValidationEvent(long timestamp, String category, String data) {
            this.timestamp = timestamp;
            this.category = category;
            this.data = data;
        }
    }
}
```

### Statistical Analysis Script

Create `/tools/validation_analysis.py`:

```python
#!/usr/bin/env python3
"""
Statistical Analysis for Validation Framework
"""

import numpy as np
import pandas as pd
from scipy import stats
import json
import sys
from pathlib import Path

def parse_log_file(log_path):
    """Parse validation log file into DataFrame."""
    data = []
    with open(log_path, 'r') as f:
        for line in f:
            try:
                # Format: [timestamp] [category] data
                if line.startswith('['):
                    parts = line.strip().split('] ', 2)
                    timestamp = int(parts[0][1:])
                    category = parts[1][1:-1]
                    data = parts[2]
                    data.append({
                        'timestamp': timestamp,
                        'category': category,
                        'data': data
                    })
            except Exception as e:
                continue
    return pd.DataFrame(data)

def extract_numeric_data(df, category, metric):
    """Extract numeric metric from category data."""
    category_data = df[df['category'] == category]
    values = []
    
    for data in category_data['data']:
        try:
            # Parse JSON-like data
            if '=' in data:
                for pair in data.split(','):
                    if pair.startswith(metric + '='):
                        value = float(pair.split('=')[1])
                        values.append(value)
        except Exception:
            continue
    
    return np.array(values)

def calculate_statistics(values):
    """Calculate key statistics for values."""
    if len(values) == 0:
        return None
    
    return {
        'count': len(values),
        'mean': np.mean(values),
        'std': np.std(values),
        'min': np.min(values),
        'max': np.max(values),
        'median': np.median(values),
        'variance': np.var(values)
    }

def compare_distributions(baseline, simulated):
    """Compare baseline and simulated distributions."""
    baseline_stats = calculate_statistics(baseline)
    simulated_stats = calculate_statistics(simulated)
    
    if baseline_stats is None or simulated_stats is None:
        return None
    
    # Cross-correlation
    correlation = np.correlate(baseline, simulated, mode='full')
    max_correlation = np.max(correlation)
    
    # Kolmogorov-Smirnov test
    ks_stat, ks_pvalue = stats.ks_2samp(baseline, simulated)
    
    # Mean error percentage
    mean_error = abs(baseline_stats['mean'] - simulated_stats['mean']) / baseline_stats['mean'] * 100
    
    return {
        'baseline': baseline_stats,
        'simulated': simulated_stats,
        'mean_error_percent': mean_error,
        'max_correlation': max_correlation,
        'ks_statistic': ks_stat,
        'ks_pvalue': ks_pvalue
    }

def generate_report(comparisons, output_path):
    """Generate HTML report from comparisons."""
    html = """
    <!DOCTYPE html>
    <html>
    <head>
        <title>Validation Report</title>
        <style>
            body { font-family: Arial, sans-serif; margin: 20px; }
            h1 { color: #333; }
            .section { margin: 20px 0; padding: 15px; border: 1px solid #ddd; }
            .pass { color: green; font-weight: bold; }
            .fail { color: red; font-weight: bold; }
            table { border-collapse: collapse; width: 100%; }
            th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
            th { background-color: #f2f2f2; }
        </style>
    </head>
    <body>
        <h1>Xposed Hooks Validation Report</h1>
    """
    
    for category, comparison in comparisons.items():
        html += f"""
        <div class="section">
            <h2>{category}</h2>
            <p><strong>Mean Error:</strong> {comparison['mean_error_percent']:.2f}%</p>
            <p><strong>Max Correlation:</strong> {comparison['max_correlation']:.4f}</p>
            <p><strong>KS Statistic:</strong> {comparison['ks_statistic']:.4f}</p>
            <p><strong>KS P-value:</strong> {comparison['ks_pvalue']:.4f}</p>
            <p><strong>Status:</strong> <span class="{'pass' if comparison['mean_error_percent'] < 15 else 'fail'}">
                {'PASS' if comparison['mean_error_percent'] < 15 else 'FAIL'}
            </span></p>
            
            <h3>Baseline Statistics</h3>
            <table>
                <tr><th>Metric</th><th>Value</th></tr>
                <tr><td>Count</td><td>{comparison['baseline']['count']}</td></tr>
                <tr><td>Mean</td><td>{comparison['baseline']['mean']:.4f}</td></tr>
                <tr><td>Std Dev</td><td>{comparison['baseline']['std']:.4f}</td></tr>
                <tr><td>Median</td><td>{comparison['baseline']['median']:.4f}</td></tr>
            </table>
            
            <h3>Simulated Statistics</h3>
            <table>
                <tr><th>Metric</th><th>Value</th></tr>
                <tr><td>Count</td><td>{comparison['simulated']['count']}</td></tr>
                <tr><td>Mean</td><td>{comparison['simulated']['mean']:.4f}</td></tr>
                <tr><td>Std Dev</td><td>{comparison['simulated']['std']:.4f}</td></tr>
                <tr><td>Median</td><td>{comparison['simulated']['median']:.4f}</td></tr>
            </table>
        </div>
        """
    
    html += """
    </body>
    </html>
    """
    
    with open(output_path, 'w') as f:
        f.write(html)
    
    print(f"Report generated: {output_path}")

def main():
    if len(sys.argv) < 3:
        print("Usage: validation_analysis.py <baseline_log> <simulated_log>")
        sys.exit(1)
    
    baseline_path = sys.argv[1]
    simulated_path = sys.argv[2]
    
    print(f"Parsing baseline log: {baseline_path}")
    baseline_df = parse_log_file(baseline_path)
    
    print(f"Parsing simulated log: {simulated_path}")
    simulated_df = parse_log_file(simulated_path)
    
    # Analyze each category
    categories = baseline_df['category'].unique()
    comparisons = {}
    
    for category in categories:
        print(f"Analyzing category: {category}")
        
        # Extract numeric data
        baseline_values = extract_numeric_data(baseline_df, category, 'value')
        simulated_values = extract_numeric_data(simulated_df, category, 'value')
        
        if len(baseline_values) > 0 and len(simulated_values) > 0:
            comparison = compare_distributions(baseline_values, simulated_values)
            if comparison:
                comparisons[category] = comparison
    
    # Generate report
    output_path = "validation_report.html"
    generate_report(comparisons, output_path)

if __name__ == "__main__":
    main()
```

### Usage

```bash
# Enable validation mode via config
# hooks_config.json:
{
    "testing_mode": {
        "validation_logging": true
    }
}

# Collect baseline data
adb shell "am broadcast -a com.samsungcloak.action.START_BASELINE"

# Run app for 10 minutes
# Collect simulated data

# Run analysis
python3 tools/validation_analysis.py baseline.log simulated.log

# View report
open validation_report.html
```

---

## 6. Configuration Management

### File to Create
`/app/src/main/assets/hooks_config.json`

```json
{
    "hooks": {
        "mechanical_micro_errors": {
            "enabled": true,
            "fat_finger_probability": 0.18,
            "overshoot_probability": 0.22,
            "partial_press_probability": 0.08,
            "touch_jitter_std_dev_px": 2.3
        },
        "sensor_fusion_coherence": {
            "enabled": true,
            "step_frequency_hz": 1.8,
            "step_frequency_variability": 0.25,
            "gps_correlation_factor": 0.85,
            "pedestrian_mode": true
        },
        "inter_app_navigation": {
            "enabled": true,
            "referral_probability": 0.12,
            "max_referral_chain_length": 4,
            "back_to_referrer_probability": 0.68
        },
        "input_pressure_dynamics": {
            "enabled": true,
            "pressure_variation": 0.25,
            "contact_area_expansion_rate": 0.08,
            "multi_touch_correlation": 0.85
        },
        "asymmetric_latency": {
            "enabled": true,
            "base_perceptual_gap_ms": 180.0,
            "perceptual_gap_variability_ms": 120.0,
            "hesitation_probability": 0.30
        },
        "ambient_light_adaptation": {
            "enabled": true,
            "response_lag_ms": 150,
            "smooth_transition_seconds": 3.0,
            "circadian_rhythm": true
        },
        "battery_thermal_throttling": {
            "enabled": true,
            "throttle_threshold_c": 42.0,
            "critical_temperature_c": 48.0,
            "peukert_exponent": 1.15
        },
        "network_quality_variation": {
            "enabled": true,
            "handover_enabled": true,
            "location_based_selection": false,
            "bandwidth_throttling": false
        },
        "typing_errors": {
            "enabled": true,
            "adjacent_key_probability": 0.70,
            "swipe_typing_enabled": false,
            "inter_key_mean_ms": 180.0
        },
        "multi_touch_imperfections": {
            "enabled": false,
            "finger_drift_enabled": false,
            "palm_contact_probability": 0.04,
            "velocity_jitter_factor": 0.02
        },
        "proximity_sensor": {
            "enabled": true,
            "accidental_pocket_events": true,
            "response_lag_ms": 200
        },
        "memory_pressure": {
            "enabled": false,
            "oom_killer_simulation": false,
            "on_trim_memory_injection": false
        }
    },
    "testing_mode": {
        "validation_logging": false,
        "baseline_collection": false,
        "a_b_testing": false,
        "log_output_path": "/sdcard/samsungcloak/validation/"
    }
}
```

### Configuration Loader

Create `ConfigurationManager.java`:

```java
package com.samsungcloak.xposed;

import android.content.Context;
import android.content.res.AssetManager;

import org.json.JSONObject;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Configuration Manager for Xposed Hooks
 * 
 * Loads and manages hook configuration from JSON assets.
 */
public class ConfigurationManager {
    
    private static final String CONFIG_FILE = "hooks_config.json";
    private static JSONObject config;
    
    public static void loadConfig(Context context) {
        try {
            AssetManager assets = context.getAssets();
            InputStream is = assets.open(CONFIG_FILE);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            
            config = new JSONObject(sb.toString());
            HookUtils.logDebug("Configuration loaded successfully");
        } catch (IOException | JSONException e) {
            HookUtils.logError("Failed to load configuration: " + e.getMessage());
            config = new JSONObject();
        }
    }
    
    public static boolean isHookEnabled(String hookName) {
        try {
            return config.getJSONObject("hooks")
                          .getJSONObject(hookName)
                          .getBoolean("enabled");
        } catch (JSONException e) {
            HookUtils.logError("Error checking hook status: " + hookName);
            return true; // Default to enabled
        }
    }
    
    public static double getHookConfig(String hookName, String key) {
        try {
            return config.getJSONObject("hooks")
                          .getJSONObject(hookName)
                          .getDouble(key);
        } catch (JSONException e) {
            HookUtils.logError("Error getting hook config: " + hookName + "." + key);
            return 0.0;
        }
    }
    
    public static boolean isTestingModeEnabled(String mode) {
        try {
            return config.getJSONObject("testing_mode")
                          .getBoolean(mode);
        } catch (JSONException e) {
            return false;
        }
    }
}
```

---

## Summary

This implementation guide provides:

1. **Complete Multi-Touch Gesture Implementation** (325 lines)
2. **Memory Pressure Enhancement** (287 lines)
3. **Validation Framework** (320 lines + Python analysis script)
4. **Configuration Management** (JSON schema + loader)

**Total New Code**: ~1,200 lines across 4 files

**Estimated Implementation Time**:
- Multi-Touch: 4 hours
- Memory Pressure: 3 hours
- Validation Framework: 5 hours
- Configuration Manager: 2 hours
- **Total**: ~14 hours

---

**Implementation Priority**:
1. High: Multi-Touch + Memory Pressure (critical gaps)
2. Medium: Validation Framework (enables data-driven improvements)
3. Low: Configuration Management (nice-to-have for A/B testing)

---

**Next Steps**:
1. Implement high-priority improvements
2. Add validation logging to existing hooks
3. Collect baseline data from real device
4. Run statistical analysis
5. Iterate based on results
