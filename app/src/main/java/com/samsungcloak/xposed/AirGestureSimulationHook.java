package com.samsungcloak.xposed;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AirGestureSimulationHook - Air Gesture Recognition Failures
 * 
 * Simulates air gesture recognition:
 * - Swipe not detected
 * - Wrong gesture recognized
 * - Proximity sensor interference
 * - Recognition delays
 * - Palm rejection issues
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class AirGestureSimulationHook {

    private static final String TAG = "[Air][Gesture]";
    private static final boolean DEBUG = true;
    
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    
    // Failure rates
    private static float gestureFailRate = 0.20f;        // 20%
    private static float wrongGestureRate = 0.10f;       // 10%
    private static float proximityInterferenceRate = 0.15f; // 15%
    private static float palmRejectionRate = 0.12f;      // 12%
    
    // Timing
    private static int minRecognitionDelay = 3000;  // ms
    private static int maxRecognitionDelay = 5000;  // ms
    
    // State
    private static boolean isAirGestureEnabled = false;
    private static boolean isGestureActive = false;
    private static String lastRecognizedGesture = null;
    private static int gestureCount = 0;
    private static int failureCount = 0;
    
    private static final Random random = new Random();
    private static final List<GestureEvent> gestureHistory = new CopyOnWriteArrayList<>();
    
    public static class GestureEvent {
        public long timestamp;
        public String type;
        public String gesture;
        public String details;
        
        public GestureEvent(String type, String gesture, String details) {
            this.timestamp = System.currentTimeMillis();
            this.type = type;
            this.gesture = gesture;
            this.details = details;
        }
    }
    
    // Gesture types
    private static final String[] GESTURES = {
        "SWIPE_UP", "SWIPE_DOWN", "SWIPE_LEFT", "SWIPE_RIGHT",
        "CIRCLE", "WAVE", "PICK_UP", "OVERVIEW"
    };
    
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (hookInitialized.getAndSet(true)) {
            HookUtils.logDebug(TAG, "Already initialized");
            return;
        }
        
        HookUtils.logInfo(TAG, "Initializing Air Gesture Simulation Hook");
        
        try {
            hookGestureDetector(lpparam);
            hookProximitySensor(lpparam);
            
            HookUtils.logInfo(TAG, "Air gesture hook initialized");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Init failed: " + t.getMessage());
            hookInitialized.set(false);
        }
    }
    
    private static void hookGestureDetector(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> gestureClass = XposedHelpers.findClass(
                "com.samsung.android.gesture.AirGestureDetector", lpparam.classLoader
            );
            
            // Hook detectAirGesture
            XposedBridge.hookAllMethods(gestureClass, "detectAirGesture",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    isGestureActive = true;
                    gestureCount++;
                    
                    // Check for gesture failure
                    if (random.nextFloat() < gestureFailRate) {
                        gestureHistory.add(new GestureEvent("GESTURE_FAIL", 
                            lastRecognizedGesture, "Gesture not recognized"));
                        
                        failureCount++;
                        
                        HookUtils.logDebug(TAG, "Gesture not recognized");
                        
                        return;
                    }
                    
                    // Check for wrong gesture
                    if (random.nextFloat() < wrongGestureRate) {
                        String wrongGesture = GESTURES[random.nextInt(GESTURES.length)];
                        
                        gestureHistory.add(new GestureEvent("WRONG_GESTURE", 
                            lastRecognizedGesture, "Recognized as " + wrongGesture));
                        
                        lastRecognizedGesture = wrongGesture;
                        
                        HookUtils.logDebug(TAG, "Wrong gesture recognized: " + wrongGesture);
                        
                        return;
                    }
                    
                    // Add recognition delay
                    int delay = minRecognitionDelay + 
                        random.nextInt(maxRecognitionDelay - minRecognitionDelay);
                    
                    gestureHistory.add(new GestureEvent("RECOGNITION_DELAY", 
                        lastRecognizedGesture, "Delay: " + delay + "ms"));
                    
                    HookUtils.logDebug(TAG, "Gesture recognition delay: " + delay + "ms");
                }
            });
            
            // Hook onGestureDetected
            XposedBridge.hookAllMethods(gestureClass, "onGestureDetected",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    gestureHistory.add(new GestureEvent("GESTURE_DETECTED", 
                        lastRecognizedGesture, "Gesture detected"));
                }
            });
            
            HookUtils.logInfo(TAG, "Air gesture detector hooked");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "Air gesture hook skipped: " + t.getMessage());
        }
    }
    
    private static void hookProximitySensor(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> sensorManagerClass = XposedHelpers.findClass(
                "android.hardware.SensorManager", lpparam.classLoader
            );
            
            // Hook getDefaultSensor for proximity
            XposedBridge.hookAllMethods(sensorManagerClass, "getDefaultSensor",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    if (param.args.length > 0 && param.args[0] instanceof Integer) {
                        int sensorType = (int) param.args[0];
                        
                        // TYPE_PROXIMITY = 5
                        if (sensorType == Sensor.TYPE_PROXIMITY) {
                            // Could inject interference
                        }
                    }
                }
            });
            
            HookUtils.logInfo(TAG, "Proximity sensor hooked");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "Proximity sensor hook skipped: " + t.getMessage());
        }
    }
    
    // ========== Configuration ==========
    
    public static void setEnabled(boolean enabled) {
        AirGestureSimulationHook.enabled = enabled;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void setGestureFailRate(float rate) {
        gestureFailRate = Math.max(0, Math.min(1, rate));
    }
    
    public static void setRecognitionDelayRange(int minMs, int maxMs) {
        minRecognitionDelay = Math.max(1000, minMs);
        maxRecognitionDelay = Math.max(minRecognitionDelay, maxMs);
    }
    
    public static void setAirGestureEnabled(boolean enabled) {
        isAirGestureEnabled = enabled;
        
        if (enabled) {
            gestureHistory.add(new GestureEvent("AIR_GESTURE_ENABLED", 
                null, "Air gesture enabled"));
        }
    }
    
    public static void setLastRecognizedGesture(String gesture) {
        lastRecognizedGesture = gesture;
    }
    
    public static int getGestureCount() {
        return gestureCount;
    }
    
    public static int getFailureCount() {
        return failureCount;
    }
    
    public static float getFailureRate() {
        return gestureCount > 0 ? (float) failureCount / gestureCount : 0;
    }
    
    public static List<GestureEvent> getGestureHistory() {
        return new ArrayList<>(gestureHistory);
    }
}
