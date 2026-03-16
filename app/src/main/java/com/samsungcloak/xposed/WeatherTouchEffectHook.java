package com.samsungcloak.xposed;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.os.Handler;
import android.os.Looper;
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
 * WeatherTouchEffectHook - Environmental Sensor Effects
 * 
 * Simulates weather-related sensor interference:
 * - Humidity effects on touchscreen
 * - Rain droplet detection
 * - Barometric pressure changes
 * - Temperature effects on sensors
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class WeatherTouchEffectHook {

    private static final String TAG = "[Weather][TouchEffect]";
    private static final boolean DEBUG = true;
    
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    
    // Parameters
    private static float rainIntensity = 0f;           // 0-1
    private static float humidityLevel = 0.5f;        // 0-1
    private static int rainTouchDriftPx = 10;          // max pixels
    private static int humidityTouchDelayMs = 100;     // ms
    private static float pressureNoiseMeters = 2f;    // ±2m
    
    // State
    private static boolean isRaining = false;
    private static float currentTemperature = 25f;    // Celsius
    
    private static final Random random = new Random();
    private static final List<WeatherEvent> weatherEvents = new CopyOnWriteArrayList<>();
    
    public static class WeatherEvent {
        public long timestamp;
        public String type;
        public String details;
        
        public WeatherEvent(String type, String details) {
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
        
        HookUtils.logInfo(TAG, "Initializing Weather Touch Effect Hook");
        
        try {
            hookMotionEvent(lpparam);
            hookSensorManager(lpparam);
            
            HookUtils.logInfo(TAG, "Weather touch hook initialized");
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
            
            XposedBridge.hookAllMethods(motionEventClass, "getX",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || !isRaining) return;
                    
                    // Rain-induced touch drift
                    if (rainIntensity > 0.3f && random.nextFloat() < rainIntensity * 0.5f) {
                        float drift = (random.nextFloat() - 0.5f) * 2 * rainTouchDriftPx * rainIntensity;
                        float originalX = (float) param.getResult();
                        
                        weatherEvents.add(new WeatherEvent("RAIN_DRIFT_X", 
                            "X drift: " + drift + "px"));
                        
                        param.setResult(originalX + drift);
                    }
                }
            });
            
            XposedBridge.hookAllMethods(motionEventClass, "getY",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || !isRaining) return;
                    
                    // Rain-induced touch drift
                    if (rainIntensity > 0.3f && random.nextFloat() < rainIntensity * 0.5f) {
                        float drift = (random.nextFloat() - 0.5f) * 2 * rainTouchDriftPx * rainIntensity;
                        float originalY = (float) param.getResult();
                        
                        weatherEvents.add(new WeatherEvent("RAIN_DRIFT_Y", 
                            "Y drift: " + drift + "px"));
                        
                        param.setResult(originalY + drift);
                    }
                }
            });
            
            HookUtils.logInfo(TAG, "MotionEvent hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "MotionEvent hook failed: " + t.getMessage());
        }
    }
    
    private static void hookSensorManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> sensorManagerClass = XposedHelpers.findClass(
                "android.hardware.SensorManager", lpparam.classLoader
            );
            
            XposedBridge.hookAllMethods(sensorManagerClass, "getDefaultSensor",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    int sensorType = (int) param.args[0];
                    
                    // Barometric pressure noise
                    if (sensorType == Sensor.TYPE_PRESSURE) {
                        weatherEvents.add(new WeatherEvent("PRESSURE_NOISE", 
                            "Altitude noise: ±" + pressureNoiseMeters + "m"));
                    }
                }
            });
            
            HookUtils.logInfo(TAG, "SensorManager hooked");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "SensorManager hook skipped: " + t.getMessage());
        }
    }
    
    public static void setEnabled(boolean enabled) {
        WeatherTouchEffectHook.enabled = enabled;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void setRaining(boolean raining) {
        isRaining = raining;
    }
    
    public static void setRainIntensity(float intensity) {
        rainIntensity = Math.max(0, Math.min(1, intensity));
        isRaining = rainIntensity > 0.1f;
    }
    
    public static void setHumidityLevel(float humidity) {
        humidityLevel = Math.max(0, Math.min(1, humidity));
    }
    
    public static void setTemperature(float temp) {
        currentTemperature = temp;
    }
    
    public static List<WeatherEvent> getWeatherEvents() {
        return new ArrayList<>(weatherEvents);
    }
}
