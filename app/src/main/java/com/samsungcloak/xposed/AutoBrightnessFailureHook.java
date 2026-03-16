package com.samsungcloak.xposed;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AutoBrightnessFailureHook - Display & Ambient Light
 * 
 * Simulates auto-brightness sensor failures:
 * - Delayed response to light changes
 * - Stuck brightness levels
 * - Incorrect lux readings
 * - Sensor blocking detection
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class AutoBrightnessFailureHook {

    private static final String TAG = "[AutoBrightness][Failure]";
    private static final boolean DEBUG = true;
    
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    
    // Parameters
    private static float sensorNoisePercent = 0.20f;    // ±20%
    private static int responseDelayMin = 500;         // ms
    private static int responseDelayMax = 2000;        // ms
    private static float sensorBlockedRate = 0.05f;    // 5%
    
    // State
    private static boolean isSensorBlocked = false;
    private static float currentLux = 100;
    private static int currentBrightness = 128; // 0-255
    
    private static final Random random = new Random();
    private static final List<BrightnessEvent> brightnessEvents = new CopyOnWriteArrayList<>();
    private static final AtomicInteger simulatedDelay = new AtomicInteger(0);
    
    public static class BrightnessEvent {
        public long timestamp;
        public String type;
        public String details;
        
        public BrightnessEvent(String type, String details) {
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
        
        HookUtils.logInfo(TAG, "Initializing Auto Brightness Failure Hook");
        
        try {
            hookSensorManager(lpparam);
            hookSettingsSystem(lpparam);
            
            HookUtils.logInfo(TAG, "Auto brightness hook initialized");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Init failed: " + t.getMessage());
            hookInitialized.set(false);
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
                    
                    // Light sensor noise
                    if (sensorType == Sensor.TYPE_LIGHT) {
                        // Sensor blocked simulation
                        if (random.nextFloat() < sensorBlockedRate) {
                            isSensorBlocked = true;
                            brightnessEvents.add(new BrightnessEvent("SENSOR_BLOCKED", 
                                "Light sensor blocked"));
                        }
                        
                        // Reading noise
                        float noise = (random.nextFloat() - 0.5f) * 2 * sensorNoisePercent;
                        float noisyLux = currentLux * (1 + noise);
                        
                        brightnessEvents.add(new BrightnessEvent("LUX_READING", 
                            "Lux: " + noisyLux + " (actual: " + currentLux + ")"));
                    }
                }
            });
            
            HookUtils.logInfo(TAG, "SensorManager hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "SensorManager hook failed: " + t.getMessage());
        }
    }
    
    private static void hookSettingsSystem(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> settingsSystemClass = XposedHelpers.findClass(
                "android.provider.Settings.System", lpparam.classLoader
            );
            
            XposedBridge.hookAllMethods(settingsSystemClass, "putInt",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    String key = (String) param.args[1];
                    
                    // Brightness changes
                    if ("screen_brightness".equals(key)) {
                        int brightness = (int) param.args[2];
                        
                        // Response delay
                        int delay = responseDelayMin + 
                            random.nextInt(responseDelayMax - responseDelayMin);
                        
                        simulatedDelay.set(delay);
                        
                        brightnessEvents.add(new BrightnessEvent("BRIGHTNESS_CHANGE", 
                            "Setting brightness: " + brightness + " (delay: " + delay + "ms)"));
                        
                        currentBrightness = brightness;
                        
                        HookUtils.logDebug(TAG, "Brightness change delay: " + delay + "ms");
                    }
                }
            });
            
            XposedBridge.hookAllMethods(settingsSystemClass, "getInt",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    String key = (String) param.args[1];
                    
                    // Return stuck brightness when sensor blocked
                    if ("screen_brightness".equals(key) && isSensorBlocked) {
                        int stuckBrightness = currentBrightness;
                        param.setResult(stuckBrightness);
                        
                        brightnessEvents.add(new BrightnessEvent("SENSOR_BLOCKED", 
                            "Brightness stuck at: " + stuckBrightness));
                    }
                }
            });
            
            HookUtils.logInfo(TAG, "Settings.System hooked");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "Settings hook skipped: " + t.getMessage());
        }
    }
    
    public static void setEnabled(boolean enabled) {
        AutoBrightnessFailureHook.enabled = enabled;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void setAmbientLux(float lux) {
        currentLux = lux;
    }
    
    public static void setSensorBlocked(boolean blocked) {
        isSensorBlocked = blocked;
    }
    
    public static int getCurrentBrightness() {
        return currentBrightness;
    }
    
    public static List<BrightnessEvent> getBrightnessEvents() {
        return new ArrayList<>(brightnessEvents);
    }
}
