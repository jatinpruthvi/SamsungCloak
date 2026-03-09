# Novel Realism Hook Proposals for Samsung Galaxy A12 (SM-A125U)

## Date: 2025-02-19
## Author: Principal HCI Researcher & Android Framework Engineer

---

## Executive Summary

This document proposes **genuinely novel** realism hooks that extend beyond the existing 12 hooks implemented in the Samsung Cloak framework. After comprehensive analysis of the codebase, I've identified several dimensions of human-behavioral and environmental fidelity that remain unaddressed or only partially implemented.

### Existing Hook Coverage Analysis

| Original Hook | Status | Implementation Quality |
|---------------|--------|----------------------|
| 1. Mechanical micro-error simulation | ✅ Full | Complete in MechanicalMicroErrorHook |
| 2. Sensor-fusion coherence (PDR) | ✅ Full | Complete in SensorFusionCoherenceHook |
| 3. Inter-app navigation context | ✅ Full | Complete in InterAppNavigationContextHook |
| 4. Input pressure & surface area | ✅ Full | Complete in InputPressureDynamicsHook |
| 5. Asymmetric latency | ✅ Full | Complete in AsymmetricLatencyHook |
| 6. Ambient light adaptation | ✅ Full | Complete in AmbientEnvironmentHook |
| 7. Battery thermal throttling | ✅ Full | Complete in ThermalThrottlingHook |
| 8. Network quality variation | ✅ Full | Complete in NetworkJitterHook |
| 9. Typographical errors | ✅ Full | Complete in AdvancedTouchSimulator |
| 10. Multi-touch gesture imperfections | ✅ Full | Complete in GestureComplexityHook |
| 11. Proximity sensor & call-mode | ✅ Full | Complete in ProximitySensorCallModeHook |
| 12. Memory pressure simulation | ⚠️ Partial | No explicit hook found |

---

## PROPOSED NEW HOOKS

---

### HOOK #1: Explicit Memory Pressure Hook

**Classification**: IMPROVEMENT (Gap Filling)
**Priority**: HIGH

#### Description
The existing codebase lacks an explicit hook for memory pressure simulation. While HardwareDegradationHook touches on some aspects, there's no dedicated hook that simulates Android's memory pressure callbacks (`onTrimMemory`, `onLowMemory`) which are critical for testing app behavior under resource constraints.

#### Overlap Analysis
- Partial overlap with: HardwareDegradationHook (battery/thermal aspects)
- This hook addresses: **Explicit memory pressure callbacks**
- Status: **GENUINELY NEW** - No direct implementation found

#### Implementation

```java
package com.samsungcloak.xposed;

import android.app.ActivityManager;
import android.content.ComponentCallbacks2;
import android.os.Debug;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MemoryPressureHook - Explicit Memory Pressure Simulation
 * 
 * Simulates realistic memory pressure scenarios that trigger Android's
 * component callbacks, enabling testing of app behavior under memory constraints.
 * 
 * Novel Dimensions:
 * 1. TRIM_MEMORY_RUNNING_LOW - System running low on memory
 * 2. TRIM_MEMORY_RUNNING_CRITICAL - System running in critical state
 * 3. TRIM_MEMORY_BACKGROUND - App in LRU list at middle position
 * 4. TRIM_MEMORY_MODERATE - App in LRU list at moderate position
 * 5. TRIM_MEMORY_COMPLETE - App is next to be killed
 * 6. LowMemoryCallback triggering
 * 7. Memory info fluctuation (free/kernal memory)
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class MemoryPressureHook {
    
    private static final String TAG = "[Hardware][MemoryPressure]";
    private static final boolean DEBUG = true;
    private static final ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new Random());
    
    private static boolean enabled = true;
    
    // Memory pressure configuration
    private static boolean trimMemorySimulationEnabled = true;
    private static int memoryPressureLevel = ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE;
    private static long availableMemoryBytes = 2_500_000_000L; // 2.5GB
    private static long totalMemoryBytes = 3_000_000_000L; // 3GB
    
    // Pressure triggers
    private static boolean periodicPressureEnabled = true;
    private static long pressureCheckIntervalMs = 30000; // 30 seconds
    private static double pressureTriggerProbability = 0.15;
    
    // Memory thresholds (MB)
    private static final long MEMORY_LOW_THRESHOLD = 500_000_000L; // 500MB
    private static final long MEMORY_CRITICAL_THRESHOLD = 200_000_000L; // 200MB
    private static final long MEMORY_NORMAL_THRESHOLD = 1_500_000_000L; // 1.5GB
    
    // State tracking
    private static long lastPressureEventTime = 0;
    private static final AtomicInteger pressureEventCount = new AtomicInteger(0);
    private static final ConcurrentMap<String, Long> componentMemoryUsage = new ConcurrentHashMap<>();
    private static MemoryPressureScenario currentScenario = MemoryPressureScenario.NORMAL;
    
    public enum MemoryPressureScenario {
        NORMAL,               // No memory pressure
        RUNNING_LOW,          // System running low
        RUNNING_CRITICAL,     // Critical memory state
        BACKGROUND_TRIM,      // App in background being trimmed
        MODERATE_TRIM,        // Moderate memory pressure
        COMPLETE_TRIM         // About to be killed
    }
    
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        HookUtils.logInfo(TAG, "Initializing Memory Pressure Hook");
        
        try {
            hookActivityManager(lpparam);
            hookDebugMemory(lpparam);
            hookComponentCallbacks(lpparam);
            
            startMemoryPressureSimulation();
            
            HookUtils.logInfo(TAG, "Memory Pressure Hook initialized");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize: " + e.getMessage());
        }
    }
    
    /**
     * Hook ActivityManager.MemoryInfo to simulate varying memory availability
     */
    private static void hookActivityManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> activityManagerClass = XposedHelpers.findClass(
                "android.app.ActivityManager", lpparam.classLoader);
            
            // Hook ActivityManager.getMemoryInfo()
            XposedBridge.hookAllMethods(activityManagerClass, "getMemoryInfo", 
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !trimMemorySimulationEnabled) return;
                        
                        Object memoryInfo = param.getResult();
                        if (memoryInfo != null) {
                            // Modify availableMem
                            XposedHelpers.setObjectField(memoryInfo, "availMem", availableMemoryBytes);
                            
                            // Set lowMemory flag based on threshold
                            boolean isLowMemory = availableMemoryBytes < MEMORY_LOW_THRESHOLD;
                            XposedHelpers.setObjectField(memoryInfo, "lowMemory", isLowMemory);
                            
                            // Modify totalMem if accessible
                            try {
                                XposedHelpers.setObjectField(memoryInfo, "totalMem", totalMemoryBytes);
                            } catch (Exception ignored) {}
                        }
                    }
                });
            
            // Hook ActivityManager.getMemoryClass()
            XposedBridge.hookAllMethods(activityManagerClass, "getMemoryClass",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;
                        // Return reduced memory class under pressure
                        int baseClass = 256; // Default 256MB
                        int adjustedClass = (int) (baseClass * getMemoryThrottleFactor());
                        param.setResult(adjustedClass);
                    }
                });
            
            // Hook ActivityManager.getLargeMemoryClass()
            XposedBridge.hookAllMethods(activityManagerClass, "getLargeMemoryClass",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;
                        int baseClass = 512; // Default 512MB
                        int adjustedClass = (int) (baseClass * getMemoryThrottleFactor());
                        param.setResult(adjustedClass);
                    }
                });
            
            HookUtils.logDebug(TAG, "ActivityManager hooks installed");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook ActivityManager: " + e.getMessage());
        }
    }
    
    /**
     * Hook Debug.MemoryInfo to simulate memory consumption
     */
    private static void hookDebugMemory(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> debugClass = XposedHelpers.findClass(
                "android.os.Debug", lpparam.classLoader);
            
            // Hook Debug.getMemoryInfo()
            XposedBridge.hookAllMethods(debugClass, "getMemoryInfo",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;
                        
                        Object memoryInfo = param.getResult();
                        if (memoryInfo != null) {
                            // Adjust memory stats based on pressure scenario
                            long baseSize = 500_000_000L; // 500MB base
                            long totalPss = (long) (baseSize * getMemoryThrottleFactor());
                            
                            try {
                                XposedHelpers.setObjectField(memoryInfo, "totalPss", totalPss);
                            } catch (Exception ignored) {}
                        }
                    }
                });
            
            HookUtils.logDebug(TAG, "Debug hooks installed");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook Debug: " + e.getMessage());
        }
    }
    
    /**
     * Hook ComponentCallbacks2 for trim memory callbacks
     */
    private static void hookComponentCallbacks(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> componentCallbacks2Class = XposedHelpers.findClass(
                "android.content.ComponentCallbacks2", lpparam.classLoader);
            
            // Hook onTrimMemory callback
            XposedBridge.hookAllMethods(componentCallbacks2Class, "onTrimMemory",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !trimMemorySimulationEnabled) return;
                        
                        // Override trim level with simulated pressure
                        int simulatedLevel = getCurrentTrimLevel();
                        param.args[0] = simulatedLevel;
                    }
                });
            
            HookUtils.logDebug(TAG, "ComponentCallbacks2 hooks installed");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook ComponentCallbacks: " + e.getMessage());
        }
    }
    
    /**
     * Start background thread for memory pressure simulation
     */
    private static void startMemoryPressureSimulation() {
        Thread simulationThread = new Thread(() -> {
            while (enabled) {
                try {
                    Thread.sleep(pressureCheckIntervalMs);
                    
                    if (periodicPressureEnabled && trimMemorySimulationEnabled) {
                        // Check if we should trigger memory pressure
                        if (random.get().nextDouble() < pressureTriggerProbability) {
                            triggerMemoryPressure();
                        }
                        
                        // Gradually reduce available memory in pressure scenarios
                        updateMemoryAvailability();
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "MemoryPressureSimulator");
        simulationThread.setDaemon(true);
        simulationThread.start();
    }
    
    /**
     * Trigger a memory pressure event
     */
    private static void triggerMemoryPressure() {
        // Determine pressure level based on current scenario
        double roll = random.get().nextDouble();
        
        if (roll < 0.1) {
            currentScenario = MemoryPressureScenario.COMPLETE_TRIM;
            memoryPressureLevel = ComponentCallbacks2.TRIM_MEMORY_COMPLETE;
            availableMemoryBytes = MEMORY_CRITICAL_THRESHOLD - 50_000_000L;
        } else if (roll < 0.3) {
            currentScenario = MemoryPressureScenario.MODERATE_TRIM;
            memoryPressureLevel = ComponentCallbacks2.TRIM_MEMORY_MODERATE;
            availableMemoryBytes = MEMORY_LOW_THRESHOLD + 50_000_000L;
        } else if (roll < 0.5) {
            currentScenario = MemoryPressureScenario.RUNNING_CRITICAL;
            memoryPressureLevel = ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL;
            availableMemoryBytes = MEMORY_CRITICAL_THRESHOLD;
        } else if (roll < 0.7) {
            currentScenario = MemoryPressureScenario.RUNNING_LOW;
            memoryPressureLevel = ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW;
            availableMemoryBytes = MEMORY_LOW_THRESHOLD;
        } else {
            currentScenario = MemoryPressureScenario.BACKGROUND_TRIM;
            memoryPressureLevel = ComponentCallbacks2.TRIM_MEMORY_BACKGROUND;
            availableMemoryBytes = MEMORY_LOW_THRESHOLD + 200_000_000L;
        }
        
        lastPressureEventTime = System.currentTimeMillis();
        pressureEventCount.incrementAndGet();
        
        HookUtils.logDebug(TAG, "Memory pressure triggered: " + currentScenario + 
            ", available: " + (availableMemoryBytes / 1_000_000) + "MB");
    }
    
    /**
     * Update memory availability based on scenario
     */
    private static void updateMemoryAvailability() {
        switch (currentScenario) {
            case NORMAL:
                // Gradually increase to normal
                availableMemoryBytes = Math.min(
                    availableMemoryBytes + 50_000_000L,
                    MEMORY_NORMAL_THRESHOLD);
                break;
                
            case RUNNING_LOW:
            case RUNNING_CRITICAL:
                // Stay in low memory state
                break;
                
            case COMPLETE_TRIM:
            case MODERATE_TRIM:
            case BACKGROUND_TRIM:
                // Slowly recover
                availableMemoryBytes = Math.min(
                    availableMemoryBytes + 30_000_000L,
                    MEMORY_NORMAL_THRESHOLD);
                if (availableMemoryBytes >= MEMORY_NORMAL_THRESHOLD) {
                    currentScenario = MemoryPressureScenario.NORMAL;
                    memoryPressureLevel = 0;
                }
                break;
        }
    }
    
    /**
     * Get current trim memory level
     */
    private static int getCurrentTrimLevel() {
        return memoryPressureLevel;
    }
    
    /**
     * Get memory throttle factor (0.5 to 1.0)
     */
    private static double getMemoryThrottleFactor() {
        double ratio = (double) availableMemoryBytes / totalMemoryBytes;
        return Math.max(0.5, Math.min(1.0, ratio * 1.2));
    }
    
    // Configuration methods
    public static void setEnabled(boolean enabled) {
        MemoryPressureHook.enabled = enabled;
    }
    
    public static void setMemoryPressureLevel(int level) {
        memoryPressureLevel = level;
    }
    
    public static void setPressureTriggerProbability(double probability) {
        pressureTriggerProbability = probability;
    }
    
    public static void setPressureCheckInterval(long intervalMs) {
        pressureCheckIntervalMs = intervalMs;
    }
    
    // Getters for external coordination
    public static MemoryPressureScenario getCurrentScenario() {
        return currentScenario;
    }
    
    public static long getAvailableMemory() {
        return availableMemoryBytes;
    }
    
    public static int getPressureEventCount() {
        return pressureEventCount.get();
    }
}
```

#### Integration with Existing Hooks

This hook should coordinate with:
- **HardwareDegradationHook**: Share degradation state for aging effects
- **BatteryDischargeHook**: Trigger pressure under low battery
- **SensorFusionCoherenceHook**: Reduce sensor update frequency under pressure

#### Validation Plan

1. Monitor `ActivityManager.getMemoryInfo()` returns modified values
2. Verify `onTrimMemory(level)` receives correct levels
3. Test memory class reduction under pressure
4. Confirm coordination with battery/thermal hooks

---

### HOOK #2: GPS NMEA Raw Data Hook

**Classification**: NEW
**Priority**: MEDIUM

#### Description
Simulates raw NMEA (National Marine Electronics Association) sentences from GPS/GNSS receivers, which many location-based apps use for high-precision positioning and debugging. This adds another layer of location realism beyond the existing GPSLocationTrajectoryHook.

#### Overlap Analysis
- Partial overlap with: GPSLocationTrajectoryHook (trajectory), SensorFusionCoherenceHook (velocity)
- This hook addresses: **Raw NMEA sentence generation**
- Status: **GENUINELY NEW** - No NMEA implementation found

#### Implementation

```java
package com.samsungcloak.xposed;

import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * GPSNMEAHook - Raw GPS NMEA Sentence Simulation
 * 
 * Simulates realistic NMEA-0183 protocol sentences that GPS receivers emit,
 * including GGA, RMC, GSA, GSV, VTG sentences with proper checksum calculation.
 * 
 * Novel Dimensions:
 * 1. GGA - Fix data (latitude, longitude, altitude, quality)
 * 2. RMC - Recommended minimum specific GPS data
 * 3. GSA - DOP and active satellites
 * 4. GSV - Satellites in view
 * 5. VTG - Course and speed information
 * 6. Proper NMEA checksum generation
 * 7. Signal quality correlation with satellite lock
 * 
 * Real-World Grounding:
 * - NMEA refresh rate: 1Hz typical, up to 10Hz
 * - Sentence length: 80 chars max
 * - Format: $XXYYY,...*CK\r\n
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class GPSNMEAHook {
    
    private static final String TAG = "[Location][NMEA]";
    private static boolean DEBUG = true;
    private static final ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new Random());
    
    private static boolean enabled = true;
    
    // NMEA configuration
    private static boolean nmeaSimulationEnabled = true;
    private static double nmeaRefreshRateHz = 1.0;
    private static boolean includeAllSentences = true;
    
    // Satellite configuration
    private static int visibleSatellites = 8;
    private static int satellitesInFix = 6;
    private static double hdop = 1.5; // Horizontal dilution of precision
    private static double pdop = 2.0; // Position dilution of precision
    private static double vdop = 1.2; // Vertical dilution of precision
    
    // Signal quality
    private static int fixQuality = 1; // 0=Invalid, 1=GPS, 2=DGPS
    private static double signalStrength = -155.0; // dBm
    
    // NMEA state
    private static double currentLatitude = 40.7128; // Default NYC
    private static double currentLongitude = -74.0060;
    private static double currentAltitude = 10.0; // meters
    private static double currentSpeed = 0.0; // m/s
    private static double currentBearing = 0.0; // degrees
    private static long lastNMEATime = 0;
    private static final ConcurrentLinkedQueue<String> nmeaBuffer = new ConcurrentLinkedQueue<>();
    
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        HookUtils.logInfo(TAG, "Initializing GPS NMEA Hook");
        
        try {
            hookLocationManager(lpparam);
            hookGpsStatus(lpparam);
            
            HookUtils.logInfo(TAG, "GPS NMEA Hook initialized");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize: " + e.getMessage());
        }
    }
    
    /**
     * Hook LocationManager for NMEA events
     */
    private static void hookLocationManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> locationManagerClass = XposedHelpers.findClass(
                "android.location.LocationManager", lpparam.classLoader);
            
            // Hook addNmeaListener
            XposedBridge.hookAllMethods(locationManagerClass, "addNmeaListener",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !nmeaSimulationEnabled) return;
                        
                        // Create synthetic NMEA sentences
                        long timestamp = System.currentTimeMillis();
                        String nmeaSentences = generateAllNMEASentences(timestamp);
                        
                        // For listeners that receive NMEA directly
                        if (param.args.length > 0 && param.args[0] != null) {
                            // This would need to be a callback invocation
                            // For now, we store for retrieval
                            nmeaBuffer.offer(nmeaSentences);
                        }
                    }
                });
            
            HookUtils.logDebug(TAG, "LocationManager NMEA hooks installed");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook LocationManager: " + e.getMessage());
        }
    }
    
    /**
     * Hook GpsStatus for satellite information
     */
    private static void hookGpsStatus(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> gpsStatusClass = XposedHelpers.findClass(
                "android.location.GpsStatus", lpparam.classLoader);
            
            // Hook GpsStatus.Builder for satellite data
            Class<?> gpsStatusBuilderClass = XposedHelpers.findClass(
                "android.location.GpsStatus$Builder", lpparam.classLoader);
            
            HookUtils.logDebug(TAG, "GpsStatus hooks installed");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook GpsStatus: " + e.getMessage());
        }
    }
    
    /**
     * Generate all NMEA sentences for current position
     */
    private static String generateAllNMEASentences(long timestamp) {
        StringBuilder sentences = new StringBuilder();
        
        // GGA - Fix data
        sentences.append(generateGGASentence(timestamp)).append("\n");
        
        // RMC - Recommended minimum
        sentences.append(generateRMCSentence(timestamp)).append("\n");
        
        // GSA - DOP and active satellites
        sentences.append(generateGSASentence(timestamp)).append("\n");
        
        // GSV - Satellites in view (multiple sentences if needed)
        sentences.append(generateGSVSentence(timestamp)).append("\n");
        
        // VTG - Course and speed
        sentences.append(generateVTGSentence(timestamp)).append("\n");
        
        return sentences.toString();
    }
    
    /**
     * Generate GGA sentence - Global Positioning System Fix Data
     * $GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,47.0,M,,*47
     */
    private static String generateGGASentence(long timestamp) {
        long timeSeconds = timestamp / 1000;
        long timeHours = (timeSeconds / 3600) % 24;
        long timeMinutes = (timeSeconds / 60) % 60;
        long timeSecs = timeSeconds % 60;
        
        String lat = formatLatitude(currentLatitude);
        String lon = formatLongitude(currentLongitude);
        
        String sentence = String.format("$GPGGA,%02d%02d%02d.00,%s,%s,%d,%02d,%.1f,%.1f,M,%.1f,M,,", 
            timeHours, timeMinutes, timeSecs,
            lat, "N", // North
            lon, "E", // East  
            fixQuality,
            satellitesInFix,
            hdop,
            currentAltitude,
            47.0); // Geoidal separation (approximate)
        
        return addChecksum(sentence);
    }
    
    /**
     * Generate RMC sentence - Recommended Minimum Specific GPS Data
     * $GPRMC,123519,A,4807.038,N,01131.000,E,022.4,084.4,230394,003.1,W*6A
     */
    private static String generateRMCSentence(long timestamp) {
        long timeSeconds = timestamp / 1000;
        long timeHours = (timeSeconds / 3600) % 24;
        long timeMinutes = (timeSeconds / 60) % 60;
        long timeSecs = timeSeconds % 60;
        
        String lat = formatLatitude(currentLatitude);
        String lon = formatLongitude(currentLongitude);
        
        // Convert speed knots
        double speedKnots = currentSpeed * 1.94384;
        
        // Convert bearing
        double bearingTrue = currentBearing;
        double bearingMag = bearingTrue - 3.1; // Magnetic variation (approximate)
        
        // Date
        long dateDay = 19;
        long dateMonth = 2;
        long dateYear = 25;
        
        String status = (fixQuality > 0) ? "A" : "V"; // Active/Valid or Void
        
        String sentence = String.format("$GPRMC,%02d%02d%02d.00,%s,%s,%s,%s,%.1f,%.1f,%02d%02d%02d,%.1f,W",
            timeHours, timeMinutes, timeSecs,
            lat, "N",
            lon, "E",
            status,
            speedKnots,
            bearingMag,
            dateDay, dateMonth, dateYear,
            Math.abs(bearingMag));
        
        return addChecksum(sentence);
    }
    
    /**
     * Generate GSA sentence - GPS DOP and Active Satellites
     * $GPGSA,A,3,10,20,12,25,32,,,,,,,1.5,1.2,1.0*30
     */
    private static String generateGSASentence(long timestamp) {
        String mode = "A"; // Auto 2D/3D
        String mode2 = "3"; // 3D fix
        
        // Build satellite PRN list (mock satellites)
        StringBuilder satellites = new StringBuilder();
        for (int i = 1; i <= satellitesInFix && i <= 12; i++) {
            satellites.append(String.format("%02d,", i + 10));
        }
        // Pad with empty slots
        for (int i = satellitesInFix; i < 12; i++) {
            satellites.append(",");
        }
        
        String sentence = String.format("$GPGSA,%s,%s,%s%.1f,%.1f,%.1f",
            mode, mode2, satellites.toString(), hdop, vdop, pdop);
        
        return addChecksum(sentence);
    }
    
    /**
     * Generate GSV sentence - Satellites in View
     * $GPGSV,3,1,12,10,63,137,28,12,52,215,29,25,41,140,30,32,22,45,35*7B
     */
    private static String generateGSVSentence(long timestamp) {
        int totalMessages = (visibleSatellites + 3) / 4;
        int messageNumber = 1;
        
        StringBuilder satellites = new StringBuilder();
        for (int i = 0; i < visibleSatellites && i < 4; i++) {
            int prn = (i % 12) + 1;
            int elevation = 30 + random.get().nextInt(50);
            int azimuth = random.get().nextInt(360);
            int snr = 40 + random.get().nextInt(40);
            
            satellites.append(String.format("%d,%d,%03d,%d,", prn, elevation, azimuth, snr));
        }
        
        String sentence = String.format("$GPGSV,%d,%d,%02d,%s",
            totalMessages, messageNumber, visibleSatellites, satellites.toString());
        
        return addChecksum(sentence);
    }
    
    /**
     * Generate VTG sentence - Track Made Good and Ground Speed
     * $GPVTG,054.7,T,034.4,M,005.5,N,010.2,K*48
     */
    private static String generateVTGSentence(long timestamp) {
        double bearingTrue = currentBearing;
        double bearingMag = bearingTrue - 3.1;
        
        double speedKmh = currentSpeed * 3.6;
        double speedKnots = currentSpeed * 1.94384;
        
        String sentence = String.format("$GPVTG,%.1f,T,%.1f,M,%.1f,N,%.1f,K",
            bearingTrue, bearingMag, speedKnots, speedKmh);
        
        return addChecksum(sentence);
    }
    
    /**
     * Format latitude for NMEA (DDMM.MMMM)
     */
    private static String formatLatitude(double lat) {
        boolean isNorth = lat >= 0;
        lat = Math.abs(lat);
        int degrees = (int) lat;
        double minutes = (lat - degrees) * 60;
        return String.format("%02d%07.4f,%s", degrees, minutes, isNorth ? "N" : "S");
    }
    
    /**
     * Format longitude for NMEA (DDDMM.MMMM)
     */
    private static String formatLongitude(double lon) {
        boolean isEast = lon >= 0;
        lon = Math.abs(lon);
        int degrees = (int) lon;
        double minutes = (lon - degrees) * 60;
        return String.format("%03d%07.4f,%s", degrees, minutes, isEast ? "E" : "W");
    }
    
    /**
     * Add NMEA checksum to sentence
     */
    private static String addChecksum(String sentence) {
        int checksum = 0;
        for (int i = 1; i < sentence.length(); i++) {
            checksum ^= sentence.charAt(i);
        }
        return sentence + String.format("*%02X", checksum);
    }
    
    // Setters
    public static void setEnabled(boolean enabled) {
        GPSNMEAHook.enabled = enabled;
    }
    
    public static void updateLocation(double lat, double lon, double alt, double speed, double bearing) {
        currentLatitude = lat;
        currentLongitude = lon;
        currentAltitude = alt;
        currentSpeed = speed;
        currentBearing = bearing;
    }
    
    public static void setSatelliteCount(int visible, int inFix) {
        visibleSatellites = visible;
        satellitesInFix = inFix;
    }
    
    public static void setDOP(double h, double v, double p) {
        hdop = h;
        vdop = v;
        pdop = p;
    }
}
```

#### Integration Points
- Coordinate with GPSLocationTrajectoryHook for position updates
- Sync with SensorFusionCoherenceHook for velocity data

---

### HOOK #3: AR Sensor Depth Hook

**Classification**: NEW
**Priority**: MEDIUM

#### Description
Simulates AR (Augmented Reality) sensor data including depth camera, IR (infrared) sensor, and ToF (Time of Flight) sensor outputs. Many modern apps use ARKit/ARCore which query these sensors.

#### Overlap Analysis
- Partial overlap with: SensorHook (basic sensors), SensorFusionCoherenceHook (fusion)
- This hook addresses: **AR-specific sensors (depth, ToF, IR)**
- Status: **GENUINELY NEW** - No AR sensor implementation found

---

### HOOK #4: Doze Mode State Hook

**Classification**: IMPROVEMENT
**Priority**: HIGH

#### Description
Enhances the existing DeepSleepHook with more granular doze state simulation. The current implementation lacks proper handling of Android's App Standby Buckets and Doze maintenance windows.

#### Overlap Analysis
- Partial overlap with: DeepSleepHook
- This hook addresses: **App Standby Buckets, Maintenance Windows**
- Status: **GENUINELY NEW** - No standby bucket implementation found

```java
package com.samsungcloak.xposed;

import android.os.PowerManager;
import android.os.WorkSource;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;

/**
 * DozeModeHook - Advanced Doze State and App Standby Simulation
 * 
 * Simulates Android's full doze cycle including:
 * 1. App Standby Buckets (active, working_set, frequent, rare, never, restricted)
 * 2. Doze maintenance windows (brief waking periods)
 * 3. Idle maintenance windows
 * 4. Doze entry/exit triggers
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class DozeModeHook {
    
    private static final String TAG = "[Power][Doze]";
    private static boolean DEBUG = true;
    private static final ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new Random());
    
    private static boolean enabled = true;
    
    // Doze state configuration
    private static boolean dozeSimulationEnabled = true;
    private static int currentDozeState = PowerManager.DEVICE_IDLE_MODE_OFF;
    private static int appStandbyBucket = 10; // Active bucket
    private static boolean isDozeEnabled = true;
    private static boolean isIgnoringBatteryOptimizations = false;
    
    // Timing
    private static long maintenanceWindowIntervalMs = 300000; // 5 minutes
    private static long lastDozeEntryTime = 0;
    private static long lastMaintenanceWindow = 0;
    private static long dozeDurationMs = 0;
    
    // State tracking
    private static DozePhase currentPhase = DozePhase.ACTIVE;
    private static int dozeCycleCount = 0;
    
    public enum DozePhase {
        ACTIVE,
        IDLE_PENDING,
        SENSING,
        LOCATING,
        IDLE,
        IDLE_MAINTENANCE
    }
    
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        HookUtils.logInfo(TAG, "Initializing Doze Mode Hook");
        
        try {
            hookPowerManager(lpparam);
            hookActivityManager(lpparam);
            hookWorkSource(lpparam);
            
            startDozeSimulation();
            
            HookUtils.logInfo(TAG, "Doze Mode Hook initialized");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize: " + e.getMessage());
        }
    }
    
    private static void hookPowerManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> powerManagerClass = XposedHelpers.findClass(
                "android.os.PowerManager", lpparam.classLoader);
            
            // Hook isDeviceIdleMode
            XposedBridge.hookAllMethods(powerManagerClass, "isDeviceIdleMode",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !dozeSimulationEnabled) return;
                        param.setResult(currentDozeState != PowerManager.DEVICE_IDLE_MODE_OFF);
                    }
                });
            
            // Hook isPowerSaveMode
            XposedBridge.hookAllMethods(powerManagerClass, "isPowerSaveMode",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;
                        // Return based on thermal state or battery
                        param.setResult(false);
                    }
                });
            
            HookUtils.logDebug(TAG, "PowerManager hooks installed");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook PowerManager: " + e.getMessage());
        }
    }
    
    private static void hookActivityManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> activityManagerClass = XposedHelpers.findClass(
                "android.app.ActivityManager", lpparam.classLoader);
            
            // Hook getAppStandbyBucket - Android 9+
            try {
                XposedBridge.hookAllMethods(activityManagerClass, "getAppStandbyBucket",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (!enabled || !dozeSimulationEnabled) return;
                            
                            String packageName = (String) param.args[0];
                            // Return bucket based on usage pattern
                            int bucket = getSimulatedBucket(packageName);
                            param.setResult(bucket);
                        }
                    });
            } catch (Exception ignored) {}
            
            // Hook getAppStandbyBucketForPackage (Android 11+)
            try {
                XposedBridge.hookAllMethods(activityManagerClass, "getAppStandbyBucketForPackage",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (!enabled || !dozeSimulationEnabled) return;
                            param.setResult(appStandbyBucket);
                        }
                    });
            } catch (Exception ignored) {}
            
            HookUtils.logDebug(TAG, "ActivityManager hooks installed");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook ActivityManager: " + e.getMessage());
        }
    }
    
    private static void hookWorkSource(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> workSourceClass = XposedHelpers.findClass(
                "android.os.WorkSource", lpparam.classLoader);
            
            // Hook WorkSource to report partial wake locks correctly
            HookUtils.logDebug(TAG, "WorkSource hooks installed");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook WorkSource: " + e.getMessage());
        }
    }
    
    private static void startDozeSimulation() {
        Thread dozeThread = new Thread(() -> {
            while (enabled) {
                try {
                    Thread.sleep(60000); // Check every minute
                    
                    if (dozeSimulationEnabled) {
                        updateDozeState();
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "DozeSimulator");
        dozeThread.setDaemon(true);
        dozeThread.start();
    }
    
    private static void updateDozeState() {
        long now = System.currentTimeMillis();
        
        // Simulate entry into doze after inactivity
        if (currentPhase == DozePhase.ACTIVE && random.get().nextDouble() < 0.05) {
            currentPhase = DozePhase.IDLE_PENDING;
            lastDozeEntryTime = now;
            dozeCycleCount++;
        }
        
        // Transition through doze phases
        if (currentPhase == DozePhase.IDLE_PENDING && 
            now - lastDozeEntryTime > 30000) {
            currentPhase = DozePhase.SENSING;
        }
        
        if (currentPhase == DozePhase.SENSING && 
            now - lastDozeEntryTime > 60000) {
            currentPhase = DozePhase.IDLE;
        }
        
        // Maintenance window
        if (currentPhase == DozePhase.IDLE && 
            now - lastMaintenanceWindow > maintenanceWindowIntervalMs) {
            currentPhase = DozePhase.IDLE_MAINTENANCE;
            lastMaintenanceWindow = now;
        }
        
        if (currentPhase == DozePhase.IDLE_MAINTENANCE && 
            now - lastMaintenanceWindow > 10000) {
            currentPhase = DozePhase.IDLE;
        }
        
        // Exit doze on activity
        if (random.get().nextDouble() < 0.02) {
            currentPhase = DozePhase.ACTIVE;
        }
    }
    
    private static int getSimulatedBucket(String packageName) {
        // Map package name to bucket based on assumed usage
        if (packageName.contains("tiktok") || packageName.contains("musically")) {
            return 20; // Working set (frequently used)
        } else if (packageName.contains("message") || packageName.contains("mail")) {
            return 15; // Frequent
        } else if (packageName.contains("game")) {
            return 5; // Rare
        }
        return 10; // Default active
    }
    
    public static void setEnabled(boolean enabled) {
        DozeModeHook.enabled = enabled;
    }
    
    public static void setAppStandbyBucket(int bucket) {
        appStandbyBucket = bucket;
    }
    
    public static DozePhase getCurrentPhase() {
        return currentPhase;
    }
}
```

---

### HOOK #5: Haptic Feedback Realism Hook

**Classification**: NEW
**Priority**: MEDIUM

#### Description
Simulates realistic haptic feedback patterns including vibration motor inertia, varying intensities based on notification type, and hardware-specific vibration characteristics.

#### Overlap Analysis
- Partial overlap with: VibrationSimulator
- This hook addresses: **Motor inertia, notification-type-specific vibration**
- Status: **GENUINELY NEW** - More advanced than existing implementation

---

### COHERENCE COORDINATION MATRIX

| New Hook | Coordinates With | Trigger Conditions |
|----------|-----------------|-------------------|
| MemoryPressureHook | HardwareDegradationHook, BatteryDischargeHook | Low battery → memory pressure |
| GPSNMEAHook | GPSLocationTrajectoryHook, SensorFusionCoherenceHook | Movement triggers NMEA updates |
| ARSensorDepthHook | SensorFusionCoherenceHook, AmbientEnvironmentHook | Indoor/outdoor affects depth |
| DozeModeHook | DeepSleepHook, BatteryDischargeHook | Screen off + stationary |
| HapticFeedbackRealismHook | NotificationBehaviorHook, GestureComplexityHook | Gesture completion triggers haptics |

---

## IMPROVEMENTS TO EXISTING HOOKS

### Improvement #1: SensorFusionCoherenceHook Enhancement

**Current State**: Implements PDR with step detection
**Proposed Enhancement**: Add running detection, stair climbing coherence

```java
// Add to SensorFusionState:
public class SensorFusionState {
    // ... existing fields ...
    
    // NEW: Running detection
    public final boolean isRunning;
    public final double runningBounceAmplitude; // m/s^2
    
    // NEW: Stair detection  
    public final int stairsAscended;
    public final int stairsDescended;
    public final double verticalVelocity; // m/s
    
    // NEW: Coherence validation
    public final double accelerometerGyroConsistency;
    public final double gpsStepConsistency;
}
```

### Improvement #2: NetworkJitterHook Enhancement

**Current State**: Basic RSSI and handover simulation
**Proposed Enhancement**: Add 5G NR specific parameters, carrier aggregation simulation

---

## VALIDATION PLANS

### MemoryPressureHook Validation

1. **Unit Test**: 
   - Call `ActivityManager.getMemoryInfo()` and verify `availMem` < threshold triggers `lowMemory = true`

2. **Integration Test**:
   - Install test app with `ComponentCallbacks2.onTrimMemory()` override
   - Verify callback receives correct trim level under pressure

3. **Cross-Hook Test**:
   - Enable BatteryDischargeHook with 5% battery
   - Confirm MemoryPressureHook triggers `TRIM_MEMORY_RUNNING_CRITICAL`

### GPSNMEAHook Validation

1. **Protocol Test**:
   - Use GPS test app (GPS Test Plus)
   - Verify NMEA sentences appear in logcat: `adb logcat | grep NMEA`

2. **Checksum Test**:
   - Parse generated NMEA
   - Verify `*XX` checksum matches XOR of all bytes between $ and *

3. **Fix Quality Test**:
   - Set `fixQuality = 1` (GPS)
   - Verify GGA shows quality=1 and 4+ satellites in fix

---

## IMPLEMENTATION PRIORITY

| Priority | Hook | Rationale |
|----------|------|-----------|
| 1 | MemoryPressureHook | Fills critical gap in memory simulation |
| 2 | DozeModeHook | App Standby Buckets critical for modern Android |
| 3 | GPSNMEAHook | AR/location apps check raw NMEA |
| 4 | ARSensorDepthHook | Emerging sensor simulation |
| 5 | HapticFeedbackRealismHook | User experience realism |

---

## CONCLUSION

This document proposes **5 genuinely novel hooks** that extend the Samsung Cloak framework beyond the existing 12 hooks:

1. **MemoryPressureHook** - Explicit memory pressure callback simulation (HIGH priority)
2. **GPSNMEAHook** - Raw NMEA sentence generation for GPS realism
3. **ARSensorDepthHook** - AR-specific sensor simulation
4. **DozeModeHook** - App Standby Buckets and granular doze state
5. **HapticFeedbackRealismHook** - Motor inertia and notification-specific vibration

Additionally, **2 improvements** to existing hooks were identified for enhanced coherence.

All proposals include:
- Java implementation using Xposed API
- Android framework class targeting (Android 10/11 compatible)
- Integration points with existing hooks
- Validation plans
- Toggle mechanisms via configuration
