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
 * Real-World Grounding (Android Memory Management):
 * - ActivityManager.MemoryInfo.availableMem: Actual available RAM
 * - ActivityManager.MemoryInfo.lowMemory: Threshold set by system
 * - ComponentCallbacks2.onTrimMemory(level): Called during memory pressure
 * - Typical thresholds: LOW=500MB, CRITICAL=200MB on 3GB device
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
    
    // SharedPreferences key for toggle
    private static final String PREFS_NAME = "SamsungCloak_MemoryPressure";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_TRIGGER_PROBABILITY = "trigger_probability";
    private static final String KEY_CHECK_INTERVAL = "check_interval";
    
    public enum MemoryPressureScenario {
        NORMAL,               // No memory pressure
        RUNNING_LOW,          // System running low
        RUNNING_CRITICAL,     // Critical memory state
        BACKGROUND_TRIM,      // App in background being trimmed
        MODERATE_TRIM,        // Moderate memory pressure
        COMPLETE_TRIM         // About to be killed
    }
    
    /**
     * Initialize the Memory Pressure Hook
     */
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        HookUtils.logInfo(TAG, "Initializing Memory Pressure Hook");
        
        try {
            loadConfiguration(lpparam);
            
            hookActivityManager(lpparam);
            hookDebugMemory(lpparam);
            hookComponentCallbacks(lpparam);
            
            startMemoryPressureSimulation();
            
            HookUtils.logInfo(TAG, "Memory Pressure Hook initialized successfully");
            HookUtils.logDebug(TAG, "Initial scenario: " + currentScenario + 
                ", available memory: " + (availableMemoryBytes / 1_000_000) + "MB");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize: " + e.getMessage());
        }
    }
    
    /**
     * Load configuration from SharedPreferences
     */
    private static void loadConfiguration(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Object prefs = XposedHelpers.callMethod(
                XposedHelpers.findClass("android.preference.PreferenceManager", lpparam.classLoader)
                    .getMethod("getDefaultSharedPreferences", android.content.Context.class)
                    .invoke(null, XposedHelpers.getObjectField(lpparam.appContext, "getApplicationContext")),
                "getSharedPreferences", PREFS_NAME, 0);
            
            if (prefs != null) {
                enabled = XposedHelpers.callMethod(prefs, "getBoolean", KEY_ENABLED, true);
                pressureTriggerProbability = XposedHelpers.callMethod(prefs, "getFloat", 
                    KEY_TRIGGER_PROBABILITY, 0.15f);
                pressureCheckIntervalMs = XposedHelpers.callMethod(prefs, "getLong", 
                    KEY_CHECK_INTERVAL, 30000L);
            }
        } catch (Exception e) {
            HookUtils.logDebug(TAG, "Using default configuration: " + e.getMessage());
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
                            // Modify availableMem - this is the critical field apps check
                            XposedHelpers.setObjectField(memoryInfo, "availMem", availableMemoryBytes);
                            
                            // Set lowMemory flag based on threshold
                            boolean isLowMemory = availableMemoryBytes < MEMORY_LOW_THRESHOLD;
                            XposedHelpers.setObjectField(memoryInfo, "lowMemory", isLowMemory);
                            
                            // Set totalMem if accessible
                            try {
                                XposedHelpers.setObjectField(memoryInfo, "totalMem", totalMemoryBytes);
                            } catch (Exception ignored) {}
                            
                            if (DEBUG && random.get().nextDouble() < 0.01) {
                                HookUtils.logDebug(TAG, "MemoryInfo: avail=" + 
                                    (availableMemoryBytes / 1_000_000) + "MB, lowMemory=" + isLowMemory);
                            }
                        }
                    }
                });
            
            // Hook ActivityManager.getMemoryClass() - returns heap size in MB
            XposedBridge.hookAllMethods(activityManagerClass, "getMemoryClass",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;
                        
                        // Return reduced memory class under pressure
                        int baseClass = 256; // Default 256MB for A12
                        int adjustedClass = (int) (baseClass * getMemoryThrottleFactor());
                        param.setResult(adjustedClass);
                        
                        if (DEBUG) {
                            HookUtils.logDebug(TAG, "MemoryClass: " + adjustedClass + "MB (throttled from " + baseClass + ")");
                        }
                    }
                });
            
            // Hook ActivityManager.getLargeMemoryClass() - for large heap apps
            XposedBridge.hookAllMethods(activityManagerClass, "getLargeMemoryClass",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;
                        
                        int baseClass = 512; // Default 512MB for large heap
                        int adjustedClass = (int) (baseClass * getMemoryThrottleFactor());
                        param.setResult(adjustedClass);
                    }
                });
            
            // Hook getSystemMemoryInfo() if available
            try {
                XposedBridge.hookAllMethods(activityManagerClass, "getSystemMemoryInfo",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            if (!enabled || !trimMemorySimulationEnabled) return;
                            
                            Object memInfo = param.getResult();
                            if (memInfo != null) {
                                // Modify system memory info
                                try {
                                    XposedHelpers.setObjectField(memInfo, "availMem", availableMemoryBytes);
                                    XposedHelpers.setObjectField(memInfo, "totalMem", totalMemoryBytes);
                                    XposedHelpers.setObjectField(memInfo, "threshold", MEMORY_LOW_THRESHOLD);
                                    XposedHelpers.setObjectField(memInfo, "lowMemory", 
                                        availableMemoryBytes < MEMORY_LOW_THRESHOLD);
                                } catch (Exception ignored) {}
                            }
                        }
                    });
            } catch (Exception ignored) {}
            
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
            
            // Hook Debug.getNativeHeapSize() and related methods
            try {
                XposedBridge.hookAllMethods(debugClass, "getNativeHeapSize",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (!enabled) return;
                            // Return throttled heap size
                            long baseSize = 128_000_000L;
                            param.setResult((long)(baseSize * getMemoryThrottleFactor()));
                        }
                    });
            } catch (Exception ignored) {}
            
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
            
            // Hook onTrimMemory callback - this is where the magic happens
            XposedBridge.hookAllMethods(componentCallbacks2Class, "onTrimMemory",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !trimMemorySimulationEnabled) return;
                        
                        // Override trim level with simulated pressure
                        int originalLevel = (int) param.args[0];
                        int simulatedLevel = getCurrentTrimLevel();
                        
                        // Only override if we have active pressure
                        if (currentScenario != MemoryPressureScenario.NORMAL) {
                            param.args[0] = simulatedLevel;
                            
                            if (DEBUG) {
                                HookUtils.logDebug(TAG, "onTrimMemory: original=" + 
                                    trimLevelToString(originalLevel) + " -> simulated=" + 
                                    trimLevelToString(simulatedLevel));
                            }
                        }
                    }
                });
            
            // Hook onLowMemory for legacy support
            try {
                XposedBridge.hookAllMethods(componentCallbacks2Class, "onLowMemory",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (!enabled || !trimMemorySimulationEnabled) return;
                            
                            // Trigger low memory only in critical scenarios
                            if (currentScenario == MemoryPressureScenario.RUNNING_CRITICAL ||
                                currentScenario == MemoryPressureScenario.COMPLETE_TRIM) {
                                // Allow the callback to proceed
                            } else {
                                // Simulate low memory by reducing available
                                availableMemoryBytes = MEMORY_CRITICAL_THRESHOLD;
                                currentScenario = MemoryPressureScenario.RUNNING_CRITICAL;
                            }
                        }
                    });
            } catch (Exception ignored) {}
            
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
            HookUtils.logDebug(TAG, "Memory pressure simulation thread started");
            
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
                    HookUtils.logDebug(TAG, "Memory pressure simulation interrupted");
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
        // Determine pressure level based on random selection
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
        
        HookUtils.logInfo(TAG, "Memory pressure triggered: " + currentScenario + 
            ", available: " + (availableMemoryBytes / 1_000_000) + "MB, level: " + 
            trimLevelToString(memoryPressureLevel));
    }
    
    /**
     * Update memory availability based on scenario - gradual recovery
     */
    private static void updateMemoryAvailability() {
        switch (currentScenario) {
            case NORMAL:
                // Gradually increase to normal (already at normal)
                break;
                
            case RUNNING_LOW:
            case RUNNING_CRITICAL:
                // Stay in low memory state, occasionally recover
                if (random.get().nextDouble() < 0.3) {
                    currentScenario = MemoryPressureScenario.NORMAL;
                    memoryPressureLevel = 0;
                    availableMemoryBytes = MEMORY_NORMAL_THRESHOLD;
                }
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
     * Get current trim memory level based on scenario
     */
    private static int getCurrentTrimLevel() {
        switch (currentScenario) {
            case COMPLETE_TRIM:
                return ComponentCallbacks2.TRIM_MEMORY_COMPLETE;
            case MODERATE_TRIM:
                return ComponentCallbacks2.TRIM_MEMORY_MODERATE;
            case BACKGROUND_TRIM:
                return ComponentCallbacks2.TRIM_MEMORY_BACKGROUND;
            case RUNNING_CRITICAL:
                return ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL;
            case RUNNING_LOW:
                return ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW;
            default:
                return 0;
        }
    }
    
    /**
     * Get memory throttle factor (0.5 to 1.0)
     */
    private static double getMemoryThrottleFactor() {
        double ratio = (double) availableMemoryBytes / totalMemoryBytes;
        return Math.max(0.5, Math.min(1.0, ratio * 1.2));
    }
    
    /**
     * Convert trim level to readable string
     */
    private static String trimLevelToString(int level) {
        switch (level) {
            case ComponentCallbacks2.TRIM_MEMORY_COMPLETE:
                return "TRIM_MEMORY_COMPLETE";
            case ComponentCallbacks2.TRIM_MEMORY_MODERATE:
                return "TRIM_MEMORY_MODERATE";
            case ComponentCallbacks2.TRIM_MEMORY_BACKGROUND:
                return "TRIM_MEMORY_BACKGROUND";
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL:
                return "TRIM_MEMORY_RUNNING_CRITICAL";
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW:
                return "TRIM_MEMORY_RUNNING_LOW";
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE:
                return "TRIM_MEMORY_RUNNING_MODERATE";
            default:
                return "UNKNOWN(" + level + ")";
        }
    }
    
    // Configuration methods (can be called from external controls)
    
    public static void setEnabled(boolean enabled) {
        MemoryPressureHook.enabled = enabled;
        HookUtils.logInfo(TAG, "MemoryPressureHook " + (enabled ? "enabled" : "disabled"));
    }
    
    public static void setMemoryPressureLevel(int level) {
        memoryPressureLevel = level;
    }
    
    public static void setPressureTriggerProbability(double probability) {
        pressureTriggerProbability = Math.max(0.0, Math.min(1.0, probability));
        HookUtils.logDebug(TAG, "Pressure trigger probability set to: " + pressureTriggerProbability);
    }
    
    public static void setPressureCheckInterval(long intervalMs) {
        pressureCheckIntervalMs = Math.max(5000L, intervalMs);
        HookUtils.logDebug(TAG, "Pressure check interval set to: " + pressureCheckIntervalMs + "ms");
    }
    
    // Getters for external coordination and testing
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static MemoryPressureScenario getCurrentScenario() {
        return currentScenario;
    }
    
    public static long getAvailableMemory() {
        return availableMemoryBytes;
    }
    
    public static int getPressureEventCount() {
        return pressureEventCount.get();
    }
    
    public static int getCurrentTrimLevelValue() {
        return memoryPressureLevel;
    }
    
    public static double getMemoryThrottleFactorValue() {
        return getMemoryThrottleFactor();
    }
}
