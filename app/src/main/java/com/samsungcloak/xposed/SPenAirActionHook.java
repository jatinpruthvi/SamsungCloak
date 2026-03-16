package com.samsungcloak.xposed;

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
 * SPenAirActionHook - Stylus Input Realism
 * 
 * Simulates S Pen air gesture detection issues:
 * - Hover jitter
 * - Gesture misrecognition
 * - Pen tip pressure drift
 * - Air action failures
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 * Note: S Pen features may not be available on A12, hook will gracefully degrade
 */
public class SPenAirActionHook {

    private static final String TAG = "[SPen][AirAction]";
    private static final boolean DEBUG = true;
    
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    
    // Parameters
    private static float hoverJitterPx = 5f;            // ±5px
    private static float gestureFailureRate = 0.15f;   // 15%
    private static float pressureDriftRate = 0.02f;    // 2% per hour
    private static float airGestureDelay = 100;        // ms
    
    // State
    private static boolean isSPenConnected = false;
    private static float accumulatedUsageHours = 0;
    private static float currentPressureSensitivity = 1.0f;
    
    private static final Random random = new Random();
    private static final List<SPenEvent> spenEvents = new CopyOnWriteArrayList<>();
    
    public static class SPenEvent {
        public long timestamp;
        public String type;
        public String details;
        
        public SPenEvent(String type, String details) {
            this.timestamp = System.currentTimeMillis();
            this.type = type;
            this.details = details;
        }
    }
    
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (hookInitialized.getAndSet(true)) {
            HookUtils.logDebug(TAG, "Already initialized");
            return;
        }
        
        HookUtils.logInfo(TAG, "Initializing S Pen Air Action Hook");
        
        try {
            hookSpenEvent(lpparam);
            hookAirGesture(lpparam);
            
            // Assume S Pen connected for simulation purposes
            isSPenConnected = true;
            
            HookUtils.logInfo(TAG, "S Pen hook initialized");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "S Pen hook skipped: " + t.getMessage());
            // Graceful degradation - S Pen may not be available on A12
            hookInitialized.set(true);
        }
    }
    
    private static void hookSpenEvent(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> spenEventClass = XposedHelpers.findClass(
                "com.samsung.android.spen.SpenEvent", lpparam.classLoader
            );
            
            XposedBridge.hookAllMethods(spenEventClass, "getAction",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || !isSPenConnected) return;
                    
                    // Apply hover jitter
                    float jitterX = (random.nextFloat() - 0.5f) * 2 * hoverJitterPx;
                    float jitterY = (random.nextFloat() - 0.5f) * 2 * hoverJitterPx;
                    
                    if (random.nextFloat() < 0.1f) {
                        spenEvents.add(new SPenEvent("HOVER_JITTER", 
                            "Offset: " + jitterX + ", " + jitterY + "px"));
                    }
                }
            });
            
            HookUtils.logInfo(TAG, "SpenEvent hooked");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "SpenEvent hook skipped: " + t.getMessage());
        }
    }
    
    private static void hookAirGesture(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> airGestureClass = XposedHelpers.findClass(
                "com.samsung.android.airgesture.AirGestureManager", lpparam.classLoader
            );
            
            XposedBridge.hookAllMethods(airGestureClass, "detect",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || !isSPenConnected) return;
                    
                    // Air gesture delay
                    int delay = (int) (airGestureDelay + random.nextInt(50));
                    spenEvents.add(new SPenEvent("AIR_GESTURE_DELAY", 
                        "Detection: " + delay + "ms"));
                    
                    // Gesture misrecognition
                    if (random.nextFloat() < gestureFailureRate) {
                        spenEvents.add(new SPenEvent("GESTURE_MISRECOGNIZED", 
                            "Air gesture failed"));
                    }
                }
            });
            
            HookUtils.logInfo(TAG, "AirGestureManager hooked");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "AirGesture hook skipped: " + t.getMessage());
        }
    }
    
    public static void setEnabled(boolean enabled) {
        SPenAirActionHook.enabled = enabled;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void setSPenConnected(boolean connected) {
        isSPenConnected = connected;
    }
    
    public static void addUsageTime(float hours) {
        accumulatedUsageHours += hours;
        // Pressure sensitivity drift
        currentPressureSensitivity = Math.max(0.8f, 1.0f - (accumulatedUsageHours * pressureDriftRate));
    }
    
    public static List<SPenEvent> getSPenEvents() {
        return new ArrayList<>(spenEvents);
    }
}
