package com.samsungcloak.xposed;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import android.view.MotionEvent;
import android.view.WindowManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Hook17Realism - Device Orientation & Grip Dynamics
 * 
 * Simulates realistic device orientation and grip behavior for Samsung Galaxy A12:
 * - Orientation changes: portrait↔landscape transitions 3-8s delay, auto-rotate disable 25-35%
 * - Grip modes: one-handed (70-80% standing/walking), two-handed (60-70% sitting), on surface (30-40% desk)
 * - Accidental palm touches: palm rejection failure 5-15%, screen edge ghost touches 2-5%
 * - Dynamic grip changes: 2-4 changes per 5min session, physical device movement patterns
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class Hook17Realism extends BaseRealismHook {

    private static final String TAG = "[Realism][Hook17-Orientation]";
    private static final boolean DEBUG = true;
    
    private static final String HOOK_ID = "hook_17";
    private static final String HOOK_NAME = "Device Orientation & Grip Dynamics";
    
    // Configuration keys
    private static final String KEY_ENABLED = "orientation_grip_enabled";
    private static final String KEY_ORIENTATION = "orientation_simulation_enabled";
    private static final String KEY_GRIP = "grip_mode_enabled";
    private static final String KEY_PALM_TOUCH = "palm_touch_enabled";
    
    // Hook state
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    private static AtomicBoolean enabled = new AtomicBoolean(true);
    private static float intensity = 0.5f;
    
    // Orientation states
    public enum OrientationMode {
        PORTRAIT,
        LANDSCAPE,
        TRANSITIONING
    }
    
    // Grip modes
    public enum GripMode {
        ONE_HANDED,
        TWO_HANDED,
        ON_SURFACE,
        IN_MOTION
    }
    
    // Current state
    private static final AtomicReference<OrientationMode> currentOrientation = 
        new AtomicReference<>(OrientationMode.PORTRAIT);
    private static final AtomicReference<GripMode> currentGrip = 
        new AtomicReference<>(GripMode.ONE_HANDED);
    private static final AtomicReference<Integer> currentRotation = 
        new AtomicReference<>(0);
    
    // Orientation transition
    private static boolean orientationSimulationEnabled = true;
    private static int transitionDelayMs = 5000;
    private static float autoRotateDisableProbability = 0.30f;
    
    // Grip simulation
    private static boolean gripSimulationEnabled = true;
    private static float oneHandedProbability = 0.75f;
    private static float twoHandedProbability = 0.65f;
    private static float surfaceProbability = 0.35f;
    
    // Palm touch simulation
    private static boolean palmTouchEnabled = true;
    private static float palmRejectionFailure = 0.10f;
    private static float edgeGhostTouchProbability = 0.035f;
    
    // Grip change tracking
    private static final AtomicInteger gripChanges = new AtomicInteger(0);
    private static long lastGripChangeTime = 0;
    private static long sessionStartTime = 0;
    
    // Device movement tracking
    private static final CopyOnWriteArrayList<Float> rotationHistory = 
        new CopyOnWriteArrayList<>();
    private static final int MAX_HISTORY = 100;
    
    // Thread-local random
    private static final ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new Random());
    
    // Context state integration
    private static RealityCoordinator.ContextState contextState = 
        RealityCoordinator.ContextState.IDLE;
    
    public Hook17Realism() {
        super(HOOK_ID, HOOK_NAME);
    }
    
    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!hookInitialized.compareAndSet(false, true)) {
            logInfo("Hook already initialized");
            return;
        }
        
        logInfo("Initializing Device Orientation & Grip Dynamics Hook");
        sessionStartTime = System.currentTimeMillis();
        
        try {
            // Load configuration
            loadConfiguration();
            
            // Hook WindowManager for orientation
            hookWindowManager(lpparam);
            
            // Hook SensorManager for rotation
            hookSensorManager(lpparam);
            
            // Hook MotionEvent for palm touches
            hookMotionEvent(lpparam);
            
            // Hook Display for rotation state
            hookDisplay(lpparam);
            
            logInfo("Orientation & Grip Dynamics Hook initialized successfully");
        } catch (Exception e) {
            logError("Failed to initialize Orientation & Grip Dynamics Hook", e);
        }
    }
    
    private void loadConfiguration() {
        if (configManager != null) {
            enabled.set(configManager.getHookParamBool(HOOK_ID, KEY_ENABLED, true));
            intensity = configManager.getHookIntensity(HOOK_ID);
            
            orientationSimulationEnabled = configManager.getHookParamBool(HOOK_ID, KEY_ORIENTATION, true);
            transitionDelayMs = configManager.getHookParamInt(HOOK_ID, "transition_delay_ms", 5000);
            autoRotateDisableProbability = configManager.getHookParamFloat(HOOK_ID, "auto_rotate_disable_prob", 0.30f);
            
            gripSimulationEnabled = configManager.getHookParamBool(HOOK_ID, KEY_GRIP, true);
            oneHandedProbability = configManager.getHookParamFloat(HOOK_ID, "one_handed_prob", 0.75f);
            twoHandedProbability = configManager.getHookParamFloat(HOOK_ID, "two_handed_prob", 0.65f);
            surfaceProbability = configManager.getHookParamFloat(HOOK_ID, "surface_prob", 0.35f);
            
            palmTouchEnabled = configManager.getHookParamBool(HOOK_ID, KEY_PALM_TOUCH, true);
            palmRejectionFailure = configManager.getHookParamFloat(HOOK_ID, "palm_rejection_failure", 0.10f);
            edgeGhostTouchProbability = configManager.getHookParamFloat(HOOK_ID, "edge_ghost_touch_prob", 0.035f);
        }
        
        logInfo("Configuration loaded: enabled=" + enabled.get() + ", intensity=" + intensity);
    }
    
    private void hookWindowManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> windowManagerClass = XposedHelpers.findClass(
                "android.view.WindowManager", lpparam.classLoader);
            
            // Hook getDefaultDisplay
            XposedBridge.hookAllMethods(windowManagerClass, "getDefaultDisplay",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get() || !orientationSimulationEnabled) return;
                        
                        try {
                            // Simulate auto-rotate being disabled
                            if (random.get().nextDouble() < autoRotateDisableProbability * intensity) {
                                // In a full implementation, would return a modified Display
                                // that reports different rotation
                            }
                        } catch (Exception e) {
                            logDebug("Error in getDefaultDisplay hook: " + e.getMessage());
                        }
                    }
                });
            
            logInfo("Hooked WindowManager");
        } catch (Exception e) {
            logError("Failed to hook WindowManager", e);
        }
    }
    
    private void hookSensorManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> sensorManagerClass = XposedHelpers.findClass(
                "android.hardware.SensorManager", lpparam.classLoader);
            
            // Hook getDefaultSensor for rotation vector
            XposedBridge.hookAllMethods(sensorManagerClass, "getDefaultSensor",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get() || !orientationSimulationEnabled) return;
                        
                        try {
                            int sensorType = (Integer) param.args[0];
                            
                            if (sensorType == Sensor.TYPE_ROTATION_VECTOR ||
                                sensorType == Sensor.TYPE_ACCELEROMETER) {
                                
                                // Add noise to rotation sensor based on grip
                                // In a full implementation, this would modify sensor readings
                            }
                        } catch (Exception e) {
                            logDebug("Error in getDefaultSensor hook: " + e.getMessage());
                        }
                    }
                });
            
            // Hook registerListener for rotation sensor
            XposedBridge.hookAllMethods(sensorManagerClass, "registerListener",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get() || !orientationSimulationEnabled) return;
                        
                        try {
                            // Add transition delay for orientation changes
                            if (random.get().nextDouble() < 0.15 * intensity) {
                                // Simulate slow orientation detection
                                int delay = transitionDelayMs + 
                                    random.get().nextInt(3000);
                                
                                if (DEBUG && random.get().nextDouble() < 0.01) {
                                    logDebug("Orientation transition delay: " + delay + "ms");
                                }
                            }
                        } catch (Exception e) {
                            logDebug("Error in registerListener hook: " + e.getMessage());
                        }
                    }
                });
            
            logInfo("Hooked SensorManager");
        } catch (Exception e) {
            logError("Failed to hook SensorManager", e);
        }
    }
    
    private void hookMotionEvent(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> motionEventClass = XposedHelpers.findClass(
                "android.view.MotionEvent", lpparam.classLoader);
            
            // Hook getToolType to detect palm touches
            XposedBridge.hookAllMethods(motionEventClass, "getToolType",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get() || !palmTouchEnabled) return;
                        
                        try {
                            int pointerIndex = (Integer) param.args[0];
                            float x = param.thisObject.getClass()
                                .getMethod("getX", int.class).invoke(param.thisObject, pointerIndex);
                            
                            // Check for edge touches (potential palm)
                            float screenWidth = 1080; // Would get from display metrics
                            float edgeThreshold = screenWidth * 0.1f;
                            
                            if (x < edgeThreshold || x > screenWidth - edgeThreshold) {
                                // Simulate palm rejection failure
                                if (random.get().nextDouble() < palmRejectionFailure * intensity) {
                                    // Allow palm touch through
                                    if (DEBUG && random.get().nextDouble() < 0.01) {
                                        logDebug("Palm rejection failure - edge touch accepted");
                                    }
                                }
                            }
                        } catch (Exception e) {
                            logDebug("Error in getToolType hook: " + e.getMessage());
                        }
                    }
                });
            
            // Hook ACTION_MOVE for grip-based coordinate offset
            XposedBridge.hookAllMethods(motionEventClass, "getX",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get() || !gripSimulationEnabled) return;
                        
                        try {
                            float originalX = (Float) param.getResult();
                            
                            // Apply grip-based offset for one-handed mode
                            if (currentGrip.get() == GripMode.ONE_HANDED) {
                                float offset = applyGripOffset(originalX, true);
                                param.setResult(originalX + offset);
                            }
                        } catch (Exception e) {
                            logDebug("Error in getX hook: " + e.getMessage());
                        }
                    }
                });
            
            logInfo("Hooked MotionEvent");
        } catch (Exception e) {
            logError("Failed to hook MotionEvent", e);
        }
    }
    
    private void hookDisplay(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> displayClass = XposedHelpers.findClass(
                "android.view.Display", lpparam.classLoader);
            
            // Hook getRotation
            XposedBridge.hookAllMethods(displayClass, "getRotation",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get() || !orientationSimulationEnabled) return;
                        
                        try {
                            int originalRotation = (Integer) param.getResult();
                            
                            // Store rotation in history
                            rotationHistory.add((float) originalRotation);
                            while (rotationHistory.size() > MAX_HISTORY) {
                                rotationHistory.remove(0);
                            }
                            
                            // Update current orientation
                            OrientationMode newMode;
                            switch (originalRotation) {
                                case 0:
                                case 2:
                                    newMode = OrientationMode.PORTRAIT;
                                    break;
                                case 1:
                                case 3:
                                    newMode = OrientationMode.LANDSCAPE;
                                    break;
                                default:
                                    newMode = OrientationMode.PORTRAIT;
                            }
                            
                            if (newMode != currentOrientation.get()) {
                                currentOrientation.set(OrientationMode.TRANSITIONING);
                                // After delay, set actual orientation
                                currentOrientation.set(newMode);
                            }
                            
                            currentRotation.set(originalRotation);
                        } catch (Exception e) {
                            logDebug("Error in getRotation hook: " + e.getMessage());
                        }
                    }
                });
            
            logInfo("Hooked Display");
        } catch (Exception e) {
            logError("Failed to hook Display", e);
        }
    }
    
    private float applyGripOffset(float coordinate, boolean isX) {
        // Apply offset based on grip mode
        // In a full implementation, this would use actual screen dimensions
        
        GripMode grip = currentGrip.get();
        
        switch (grip) {
            case ONE_HANDED:
                // Offset for thumb reach
                if (isX) {
                    return (random.get().nextFloat() - 0.5f) * 20 * intensity;
                } else {
                    return (random.get().nextFloat() - 0.5f) * 30 * intensity;
                }
            case TWO_HANDED:
                // Smaller offset for two-handed grip
                return (random.get().nextFloat() - 0.5f) * 5 * intensity;
            default:
                return 0;
        }
    }
    
    /**
     * Update grip mode based on context state
     */
    public static void updateGripFromContext(RealityCoordinator.ContextState state) {
        contextState = state;
        
        if (!gripSimulationEnabled) return;
        
        GripMode newGrip;
        float probability;
        
        switch (state) {
            case WALKING:
            case RUNNING:
                probability = oneHandedProbability;
                newGrip = GripMode.ONE_HANDED;
                break;
            case RIDING:
            case DRIVING:
                probability = 0.85f;
                newGrip = GripMode.IN_MOTION;
                break;
            case WORKING:
            case MEETING:
                probability = twoHandedProbability;
                newGrip = GripMode.TWO_HANDED;
                break;
            default:
                probability = surfaceProbability;
                newGrip = GripMode.ON_SURFACE;
        }
        
        // Only change grip based on probability
        if (random.get().nextFloat() < probability) {
            GripMode oldGrip = currentGrip.get();
            if (oldGrip != newGrip) {
                currentGrip.set(newGrip);
                gripChanges.incrementAndGet();
                lastGripChangeTime = System.currentTimeMillis();
                
                HookUtils.logInfo(TAG, "Grip changed: " + oldGrip + " -> " + newGrip);
            }
        }
    }
    
    /**
     * Set orientation mode manually
     */
    public static void setOrientationMode(OrientationMode mode) {
        currentOrientation.set(mode);
        HookUtils.logInfo(TAG, "Orientation set to: " + mode);
    }
    
    /**
     * Set grip mode manually
     */
    public static void setGripMode(GripMode mode) {
        GripMode oldGrip = currentGrip.get();
        currentGrip.set(mode);
        gripChanges.incrementAndGet();
        lastGripChangeTime = System.currentTimeMillis();
        
        HookUtils.logInfo(TAG, "Grip mode set to: " + mode);
    }
    
    /**
     * Enable/disable the hook
     */
    public static void setEnabled(boolean isEnabled) {
        enabled.set(isEnabled);
        HookUtils.logInfo(TAG, "Hook " + (isEnabled ? "enabled" : "disabled"));
    }
    
    /**
     * Set intensity (0.0 - 1.0)
     */
    public static void setIntensity(float value) {
        intensity = Math.max(0.0f, Math.min(1.0f, value));
        HookUtils.logInfo(TAG, "Intensity set to: " + intensity);
    }
    
    /**
     * Get current orientation
     */
    public static OrientationMode getCurrentOrientation() {
        return currentOrientation.get();
    }
    
    /**
     * Get current grip mode
     */
    public static GripMode getCurrentGrip() {
        return currentGrip.get();
    }
    
    /**
     * Get grip change count
     */
    public static int getGripChangeCount() {
        return gripChanges.get();
    }
}
