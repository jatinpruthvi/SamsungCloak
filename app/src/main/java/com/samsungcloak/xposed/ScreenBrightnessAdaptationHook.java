package com.samsungcloak.xposed;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Display;
import android.view.WindowManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicFloat;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ScreenBrightnessAdaptationHook - Auto-Brightness Hysteresis & Delayed Adaptation
 * 
 * Simulates realistic auto-brightness behavior:
 * - Slow ramp (2-5 seconds transition)
 * - Overshoot/undershoot
 * - User override persistence
 * - Ambient light sensor noise
 * - "Stuck at low brightness" after sudden changes
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class ScreenBrightnessAdaptationHook {

    private static final String TAG = "[Brightness][Display]";
    private static final boolean DEBUG = true;
    
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    
    // Timing (milliseconds)
    private static int minTransitionTime = 2000;
    private static int maxTransitionTime = 5000;
    private static int sensorPollingInterval = 500;
    
    // Brightness levels (0.0 - 1.0)
    private static float currentBrightness = 0.5f;
    private static float targetBrightness = 0.5f;
    private static float lastSensorBrightness = 0.5f;
    private static float userOverrideBrightness = -1; // -1 means no override
    
    // Sensor noise
    private static float sensorNoiseLevel = 0.05f;
    private static boolean sensorNoiseEnabled = true;
    
    // Hysteresis
    private static float overshootAmount = 0.1f;
    private static float undershootAmount = 0.08f;
    private static boolean isTransitioning = false;
    
    // State
    private static int screenState = Display.STATE_ON;
    private static boolean autoBrightnessEnabled = true;
    private static long lastTransitionTime = 0;
    
    private static final Random random = new Random();
    private static final List<BrightnessEvent> brightnessHistory = new CopyOnWriteArrayList<>();
    private static Handler transitionHandler = null;
    
    public static class BrightnessEvent {
        public float from;
        public float to;
        public long timestamp;
        public String reason;
        
        public BrightnessEvent(float from, float to, String reason) {
            this.from = from;
            this.to = to;
            this.timestamp = System.currentTimeMillis();
            this.reason = reason;
        }
    }
    
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (hookInitialized.getAndSet(true)) {
            HookUtils.logDebug(TAG, "Already initialized");
            return;
        }
        
        HookUtils.logInfo(TAG, "Initializing Screen Brightness Adaptation Hook");
        
        try {
            hookSettings(lpparam);
            hookDisplay(lpparam);
            hookSensorManager(lpparam);
            
            transitionHandler = new Handler(Looper.getMainLooper());
            
            HookUtils.logInfo(TAG, "Brightness adaptation hook initialized");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Init failed: " + t.getMessage());
            hookInitialized.set(false);
        }
    }
    
    private static void hookSettings(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> settingsClass = XposedHelpers.findClass(
                "android.provider.Settings$System", lpparam.classLoader
            );
            
            // Hook getInt for screen brightness
            XposedBridge.hookAllMethods(settingsClass, "getInt",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    String name = null;
                    if (param.args.length > 1 && param.args[1] instanceof String) {
                        name = (String) param.args[1];
                    }
                    
                    if (name != null && name.equals(Settings.System.SCREEN_BRIGHTNESS)) {
                        // Check for user override
                        if (userOverrideBrightness >= 0) {
                            param.setResult((int)(userOverrideBrightness * 255));
                        } else {
                            param.setResult((int)(currentBrightness * 255));
                        }
                    }
                }
            });
            
            // Hook putInt for brightness changes
            XposedBridge.hookAllMethods(settingsClass, "putInt",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    String name = null;
                    int value = 0;
                    
                    if (param.args.length > 0 && param.args[0] instanceof String) {
                        name = (String) param.args[0];
                    }
                    if (param.args.length > 1 && param.args[1] instanceof Integer) {
                        value = (int) param.args[1];
                    }
                    
                    if (name != null && name.equals(Settings.System.SCREEN_BRIGHTNESS)) {
                        float newBrightness = value / 255.0f;
                        
                        // Detect user manual override vs auto
                        if (!isTransitioning) {
                            userOverrideBrightness = newBrightness;
                            HookUtils.logDebug(TAG, "User override: " + userOverrideBrightness);
                        }
                    }
                }
            });
            
            HookUtils.logInfo(TAG, "Settings hooked for brightness");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Settings hook failed: " + t.getMessage());
        }
    }
    
    private static void hookDisplay(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> displayClass = XposedHelpers.findClass(
                "android.view.Display", lpparam.classLoader
            );
            
            // Hook getBrightness
            XposedBridge.hookAllMethods(displayClass, "getBrightness",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    float brightness = userOverrideBrightness >= 0 ? 
                        userOverrideBrightness : currentBrightness;
                    
                    param.setResult(brightness);
                }
            });
            
            HookUtils.logInfo(TAG, "Display hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Display hook failed: " + t.getMessage());
        }
    }
    
    private static void hookSensorManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> sensorManagerClass = XposedHelpers.findClass(
                "android.hardware.SensorManager", lpparam.classLoader
            );
            
            // Hook getDefaultSensor for light sensor
            XposedBridge.hookAllMethods(sensorManagerClass, "getDefaultSensor",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    // Let it pass through
                }
            });
            
            HookUtils.logInfo(TAG, "SensorManager hooked for light sensor");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "SensorManager hook failed: " + t.getMessage());
        }
    }
    
    /**
     * Process light sensor value and calculate target brightness
     */
    public static void onLightSensorChanged(float lux) {
        if (!enabled || !autoBrightnessEnabled || userOverrideBrightness >= 0) {
            return;
        }
        
        // Add sensor noise
        float noisyLux = lux;
        if (sensorNoiseEnabled) {
            float noise = (random.nextFloat() - 0.5f) * sensorNoiseLevel * lux;
            noisyLux = Math.max(0, lux + noise);
        }
        
        // Convert lux to brightness (simplified curve)
        float newTarget = calculateBrightnessFromLux(noisyLux);
        
        // Apply overshoot/undershoot
        if (newTarget > lastSensorBrightness) {
            newTarget += overshootAmount * (newTarget - lastSensorBrightness);
        } else {
            newTarget -= undershootAmount * (lastSensorBrightness - newTarget);
        }
        
        // Clamp
        newTarget = Math.max(0.01f, Math.min(1.0f, newTarget));
        
        // Start transition
        if (Math.abs(newTarget - targetBrightness) > 0.05f) {
            targetBrightness = newTarget;
            startBrightnessTransition();
        }
        
        lastSensorBrightness = newTarget;
    }
    
    /**
     * Convert lux value to screen brightness (0-1)
     * Based on typical ambient light curves
     */
    private static float calculateBrightnessFromLux(float lux) {
        if (lux < 10) return 0.05f;       // Dark
        if (lux < 50) return 0.15f;       // Dim
        if (lux < 200) return 0.30f;       // Indoor
        if (lux < 500) return 0.50f;       // Cloudy
        if (lux < 1000) return 0.65f;      // Window
        if (lux < 10000) return 0.80f;    // Overcast
        if (lux < 30000) return 0.90f;     // Bright
        return 1.0f;                       // Direct sunlight
    }
    
    /**
     * Start smooth brightness transition
     */
    private static void startBrightnessTransition() {
        if (isTransitioning || transitionHandler == null) return;
        
        isTransitioning = true;
        float startBrightness = currentBrightness;
        int duration = minTransitionTime + random.nextInt(maxTransitionTime - minTransitionTime);
        long startTime = System.currentTimeMillis();
        
        transitionHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!enabled) {
                    isTransitioning = false;
                    return;
                }
                
                long elapsed = System.currentTimeMillis() - startTime;
                float progress = Math.min(1.0f, (float) elapsed / duration);
                
                // Ease-in-out curve
                float easedProgress = progress < 0.5f ? 
                    2 * progress * progress : 
                    1 - (float) Math.pow(-2 * progress + 2, 2) / 2;
                
                currentBrightness = startBrightness + 
                    (targetBrightness - startBrightness) * easedProgress;
                
                if (progress >= 1.0f) {
                    currentBrightness = targetBrightness;
                    isTransitioning = false;
                    
                    brightnessHistory.add(new BrightnessEvent(
                        startBrightness, currentBrightness, "AUTO"));
                } else {
                    transitionHandler.postDelayed(this, 50);
                }
            }
        });
    }
    
    /**
     * Simulate "stuck at low brightness" after sudden change
     */
    public static void simulateStuckLowBrightness() {
        if (!enabled || random.nextFloat() > 0.1f) return;
        
        // Temporarily cap brightness
        float stuckBrightness = currentBrightness;
        
        transitionHandler.postDelayed(() -> {
            if (currentBrightness < targetBrightness) {
                HookUtils.logDebug(TAG, "Simulating stuck-low brightness");
                // Let it recover slowly
                currentBrightness += 0.05f;
            }
        }, 3000);
    }
    
    // ========== Configuration ==========
    
    public static void setEnabled(boolean enabled) {
        ScreenBrightnessAdaptationHook.enabled = enabled;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void setAutoBrightnessEnabled(boolean enabled) {
        autoBrightnessEnabled = enabled;
        if (!enabled) {
            userOverrideBrightness = currentBrightness;
        }
    }
    
    public static void clearUserOverride() {
        userOverrideBrightness = -1;
    }
    
    public static void setSensorNoiseLevel(float level) {
        sensorNoiseLevel = Math.max(0, Math.min(0.2f, level));
    }
    
    public static float getCurrentBrightness() {
        return userOverrideBrightness >= 0 ? userOverrideBrightness : currentBrightness;
    }
    
    public static float getTargetBrightness() {
        return targetBrightness;
    }
    
    public static boolean isTransitioning() {
        return isTransitioning;
    }
    
    public static List<BrightnessEvent> getBrightnessHistory() {
        return new ArrayList<>(brightnessHistory);
    }
}
