package com.samsungcloak.xposed;

import android.content.Context;
import android.hardware.input.InputManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;

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
 * WeatherTouchscreenEffectHook - Weather Effects on Touchscreen
 * 
 * Simulates weather-related touchscreen issues:
 * - Rain droplets causing phantom touches
 * - High humidity increasing response time
 * - Extreme temperatures affecting sensitivity
 * - Water film effect with offset coordinates
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class WeatherTouchscreenEffectHook {

    private static final String TAG = "[Weather][Touch]";
    private static final boolean DEBUG = true;
    
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    
    // Weather modes
    public enum WeatherMode {
        NORMAL,
        RAIN,
        HIGH_HUMIDITY,
        EXTREME_COLD,
        EXTREME_HEAT
    }
    
    private static WeatherMode currentMode = WeatherMode.NORMAL;
    
    // Effect parameters
    private static float phantomTouchRate = 0.03f;      // 3% during rain
    private static float humidityDelay = 30;            // ms
    private static float temperatureSensitivityLoss = 0.15f; // 15% in extreme temps
    private static float waterFilmOffset = 5.0f;        // pixels
    
    // State
    private static boolean isRaining = false;
    private static int humidityLevel = 50;             // 0-100
    private static int temperature = 25;                // Celsius
    private static long lastPhantomTouch = 0;
    
    private static final Random random = new Random();
    private static final List<TouchEvent> touchEvents = new CopyOnWriteArrayList<>();
    
    public static class TouchEvent {
        public long timestamp;
        public float x, y;
        public boolean isPhantom;
        public String reason;
        
        public TouchEvent(float x, float y, boolean isPhantom, String reason) {
            this.timestamp = System.currentTimeMillis();
            this.x = x;
            this.y = y;
            this.isPhantom = isPhantom;
            this.reason = reason;
        }
    }
    
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (hookInitialized.getAndSet(true)) {
            HookUtils.logDebug(TAG, "Already initialized");
            return;
        }
        
        HookUtils.logInfo(TAG, "Initializing Weather Touchscreen Effect Hook");
        
        try {
            hookMotionEvent(lpparam);
            hookInputManager(lpparam);
            
            HookUtils.logInfo(TAG, "Weather touchscreen hook initialized");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Init failed: " + t.getMessage());
            hookInitialized.set(false);
        }
    }
    
    private static void hookMotionEvent(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> motionEventClass = XposedHelpers.findClass(
                "android.view.MotionEvent", lpparam.classLoader
            );
            
            // Hook obtain to inject delays
            XposedBridge.hookAllMethods(motionEventClass, "obtain",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    // Apply humidity delay
                    if (currentMode == WeatherMode.HIGH_HUMIDITY || humidityLevel > 70) {
                        Thread.sleep(humidityDelay + random.nextInt(20));
                    }
                    
                    // Apply temperature sensitivity loss
                    if (currentMode == WeatherMode.EXTREME_COLD || currentMode == WeatherMode.EXTREME_HEAT) {
                        // Could modify pressure values
                    }
                }
            });
            
            HookUtils.logInfo(TAG, "MotionEvent hooked for weather effects");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "MotionEvent hook failed: " + t.getMessage());
        }
    }
    
    private static void hookInputManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> inputManagerClass = XposedHelpers.findClass(
                "android.hardware.input.InputManager", lpparam.classLoader
            );
            
            // Hook injectInputEvent for phantom touches
            XposedBridge.hookAllMethods(inputManagerClass, "injectInputEvent",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    // Check for rain phantom touches
                    if (currentMode == WeatherMode.RAIN || isRaining) {
                        if (random.nextFloat() < phantomTouchRate) {
                            long now = System.currentTimeMillis();
                            if (now - lastPhantomTouch > 500) { // Debounce
                                injectPhantomTouch();
                                lastPhantomTouch = now;
                            }
                        }
                    }
                }
            });
            
            HookUtils.logInfo(TAG, "InputManager hooked");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "InputManager hook skipped: " + t.getMessage());
        }
    }
    
    private static void injectPhantomTouch() {
        // Generate random phantom touch position
        float x = random.nextInt(1080);
        float y = random.nextInt(2400);
        
        touchEvents.add(new TouchEvent(x, y, true, "RAIN_PHANTOM"));
        
        HookUtils.logDebug(TAG, "Phantom touch injected at (" + x + ", " + y + ")");
    }
    
    /**
     * Apply water film offset to coordinates
     */
    public static float applyWaterFilmOffset(float original, boolean isX) {
        if (currentMode != WeatherMode.RAIN && humidityLevel < 80) {
            return original;
        }
        
        // Water creates a refracting effect
        float offset = (random.nextFloat() - 0.5f) * waterFilmOffset * 2;
        return original + offset;
    }
    
    // ========== Configuration ==========
    
    public static void setEnabled(boolean enabled) {
        WeatherTouchscreenEffectHook.enabled = enabled;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void setWeatherMode(WeatherMode mode) {
        currentMode = mode;
        HookUtils.logInfo(TAG, "Weather mode set to: " + mode);
    }
    
    public static void setRaining(boolean raining) {
        isRaining = raining;
    }
    
    public static void setHumidityLevel(int level) {
        humidityLevel = Math.max(0, Math.min(100, level));
    }
    
    public static void setTemperature(int celsius) {
        temperature = Math.max(-20, Math.min(50, celsius));
    }
    
    public static WeatherMode getWeatherMode() {
        return currentMode;
    }
    
    public static List<TouchEvent> getTouchEvents() {
        return new ArrayList<>(touchEvents);
    }
}
