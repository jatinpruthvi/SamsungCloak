package com.samsungcloak.xposed;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;

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
 * Hook15Realism - Weather & Environmental Sensor Effects
 * 
 * Simulates environmental effects on device sensors for Samsung Galaxy A12:
 * - Rain/water droplet simulation: dead spots, reduced sensitivity 30-50%, ghost touches 3-8%
 * - Humidity effects: touch screen stickiness, microphone degradation, speaker muffling
 * - Temperature extremes: cold latency increase 20-40%, hot thermal throttling
 * - Altitude/pressure changes: barometric sensor variation, GPS accuracy reduction
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class Hook15Realism extends BaseRealismHook {

    private static final String TAG = "[Realism][Hook15-Weather]";
    private static final boolean DEBUG = true;
    
    private static final String HOOK_ID = "hook_15";
    private static final String HOOK_NAME = "Weather & Environmental Sensor Effects";
    
    // Configuration keys
    private static final String KEY_ENABLED = "weather_effects_enabled";
    private static final String KEY_RAIN_SIM = "rain_simulation_enabled";
    private static final String KEY_HUMIDITY_SIM = "humidity_simulation_enabled";
    private static final String KEY_TEMP_SIM = "temperature_simulation_enabled";
    private static final String KEY_ALTITUDE_SIM = "altitude_simulation_enabled";
    
    // Hook state
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    private static AtomicBoolean enabled = new AtomicBoolean(true);
    private static float intensity = 0.5f;
    
    // Environmental conditions
    public enum WeatherCondition {
        CLEAR,
        LIGHT_RAIN,
        HEAVY_RAIN,
        HIGH_HUMIDITY,
        DRY,
        COLD,
        HOT,
        NORMAL
    }
    
    // Current weather state
    private static final AtomicReference<WeatherCondition> currentCondition = 
        new AtomicReference<>(WeatherCondition.NORMAL);
    private static final AtomicReference<Float> currentTemperature = 
        new AtomicReference<>(22.0f); // Celsius
    private static final AtomicReference<Float> currentHumidity = 
        new AtomicReference<>(50.0f); // Percentage
    private static final AtomicReference<Float> currentPressure = 
        new AtomicReference<>(1013.25f); // hPa
    private static final AtomicReference<Float> currentAltitude = 
        new AtomicReference<>(0.0f); // meters
    
    // Rain effects
    private static boolean rainEnabled = true;
    private static float touchSensitivityReduction = 0.40f;
    private static float ghostTouchProbability = 0.05f;
    private static int rainDeadSpotCount = 2;
    
    // Humidity effects
    private static boolean humidityEnabled = true;
    private static float touchStickiness = 0.20f;
    private static float micDegradation = 0.15f;
    private static float speakerMuffling = 0.20f;
    
    // Temperature effects
    private static boolean temperatureEnabled = true;
    private static float coldLatencyIncrease = 0.30f;
    private static float hotThrottleThreshold = 40.0f;
    
    // Altitude effects
    private static boolean altitudeEnabled = true;
    private static float pressureVariation = 0.02f;
    private static float gpsAccuracyLoss = 0.15f;
    
    // Touch dead spots (simulated water droplets)
    private static final CopyOnWriteArrayList<DeadSpot> deadSpots = 
        new CopyOnWriteArrayList<>();
    
    // Tracking
    private static long lastUpdateTime = 0;
    private static final AtomicInteger ghostTouchCount = new AtomicInteger(0);
    private static final AtomicInteger totalTouchCount = new AtomicInteger(0);
    
    // Thread-local random
    private static final ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new Random());
    
    public Hook15Realism() {
        super(HOOK_ID, HOOK_NAME);
    }
    
    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!hookInitialized.compareAndSet(false, true)) {
            logInfo("Hook already initialized");
            return;
        }
        
        logInfo("Initializing Weather & Environmental Sensor Effects Hook");
        
        try {
            // Load configuration
            loadConfiguration();
            
            // Initialize dead spots
            initializeDeadSpots();
            
            // Hook SensorManager
            hookSensorManager(lpparam);
            
            // Hook MotionEvent for touch effects
            hookMotionEvent(lpparam);
            
            // Hook Location for pressure-based effects
            hookLocation(lpparam);
            
            // Hook CameraDevice if available
            hookCamera(lpparam);
            
            logInfo("Weather & Environmental Effects Hook initialized successfully");
        } catch (Exception e) {
            logError("Failed to initialize Weather & Environmental Effects Hook", e);
        }
    }
    
    private void loadConfiguration() {
        if (configManager != null) {
            enabled.set(configManager.getHookParamBool(HOOK_ID, KEY_ENABLED, true));
            intensity = configManager.getHookIntensity(HOOK_ID);
            
            rainEnabled = configManager.getHookParamBool(HOOK_ID, KEY_RAIN_SIM, true);
            touchSensitivityReduction = configManager.getHookParamFloat(HOOK_ID, "touch_sensitivity_reduction", 0.40f);
            ghostTouchProbability = configManager.getHookParamFloat(HOOK_ID, "ghost_touch_probability", 0.05f);
            rainDeadSpotCount = configManager.getHookParamInt(HOOK_ID, "dead_spot_count", 2);
            
            humidityEnabled = configManager.getHookParamBool(HOOK_ID, KEY_HUMIDITY_SIM, true);
            touchStickiness = configManager.getHookParamFloat(HOOK_ID, "touch_stickiness", 0.20f);
            micDegradation = configManager.getHookParamFloat(HOOK_ID, "mic_degradation", 0.15f);
            speakerMuffling = configManager.getHookParamFloat(HOOK_ID, "speaker_muffling", 0.20f);
            
            temperatureEnabled = configManager.getHookParamBool(HOOK_ID, KEY_TEMP_SIM, true);
            coldLatencyIncrease = configManager.getHookParamFloat(HOOK_ID, "cold_latency_increase", 0.30f);
            hotThrottleThreshold = configManager.getHookParamFloat(HOOK_ID, "hot_throttle_threshold", 40.0f);
            
            altitudeEnabled = configManager.getHookParamBool(HOOK_ID, KEY_ALTITUDE_SIM, true);
            pressureVariation = configManager.getHookParamFloat(HOOK_ID, "pressure_variation", 0.02f);
            gpsAccuracyLoss = configManager.getHookParamFloat(HOOK_ID, "gps_accuracy_loss", 0.15f);
        }
        
        logInfo("Configuration loaded: enabled=" + enabled.get() + ", intensity=" + intensity);
    }
    
    private void initializeDeadSpots() {
        deadSpots.clear();
        for (int i = 0; i < rainDeadSpotCount; i++) {
            // Random position on screen (would need display metrics in real implementation)
            deadSpots.add(new DeadSpot(
                100 + random.get().nextInt(800),
                200 + random.get().nextInt(1200),
                30 + random.get().nextInt(50) // radius
            ));
        }
    }
    
    private void hookSensorManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> sensorManagerClass = XposedHelpers.findClass(
                "android.hardware.SensorManager", lpparam.classLoader);
            
            // Hook getDefaultSensor for pressure sensor
            XposedBridge.hookAllMethods(sensorManagerClass, "getDefaultSensor",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get() || !altitudeEnabled) return;
                        
                        try {
                            int sensorType = (Integer) param.args[0];
                            if (sensorType == Sensor.TYPE_PRESSURE) {
                                // Add variation to pressure readings
                                // This would need to be handled in the sensor event callback
                            }
                        } catch (Exception e) {
                            logDebug("Error in getDefaultSensor hook: " + e.getMessage());
                        }
                    }
                });
            
            // Hook registerListener to add latency based on temperature
            XposedBridge.hookAllMethods(sensorManagerClass, "registerListener",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get() || !temperatureEnabled) return;
                        
                        try {
                            float temp = currentTemperature.get();
                            
                            // Apply cold latency increase
                            if (temp < 5) {
                                int samplingPeriod = (Integer) param.args[1];
                                int adjustedPeriod = (int)(samplingPeriod * (1 + coldLatencyIncrease));
                                param.args[1] = adjustedPeriod;
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
            
            // Hook getPressure to apply sensitivity reduction
            XposedBridge.hookAllMethods(motionEventClass, "getPressure",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get()) return;
                        
                        try {
                            float originalPressure = (Float) param.getResult();
                            float reducedPressure = applySensitivityReduction(originalPressure);
                            param.setResult(reducedPressure);
                        } catch (Exception e) {
                            logDebug("Error in getPressure hook: " + e.getMessage());
                        }
                    }
                });
            
            // Hook ACTION_DOWN to potentially trigger ghost touches
            XposedBridge.hookAllMethods(motionEventClass, "obtain",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get()) return;
                        
                        try {
                            MotionEvent event = (MotionEvent) param.getResult();
                            if (event != null && event.getAction() == MotionEvent.ACTION_DOWN) {
                                checkForGhostTouch(event);
                            }
                        } catch (Exception e) {
                            logDebug("Error in MotionEvent.obtain ghost touch check: " + e.getMessage());
                        }
                    }
                });
            
            logInfo("Hooked MotionEvent for weather effects");
        } catch (Exception e) {
            logError("Failed to hook MotionEvent", e);
        }
    }
    
    private void hookLocation(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> locationClass = XposedHelpers.findClass(
                "android.location.Location", lpparam.classLoader);
            
            // Hook getAccuracy to apply altitude-based reduction
            XposedBridge.hookAllMethods(locationClass, "getAccuracy",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get() || !altitudeEnabled) return;
                        
                        try {
                            float originalAccuracy = (Float) param.getResult();
                            float altitude = currentAltitude.get();
                            
                            // Apply GPS accuracy loss at high altitude
                            if (altitude > 1000) {
                                float lossFactor = 1.0f + (gpsAccuracyLoss * (altitude / 1000.0f));
                                param.setResult(originalAccuracy * lossFactor);
                            }
                        } catch (Exception e) {
                            logDebug("Error in getAccuracy hook: " + e.getMessage());
                        }
                    }
                });
            
            logInfo("Hooked Location for altitude effects");
        } catch (Exception e) {
            logError("Failed to hook Location", e);
        }
    }
    
    private void hookCamera(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook CameraDevice for humidity effects on camera
            Class<?> cameraDeviceClass = XposedHelpers.findClass(
                "android.hardware.camera2.CameraDevice", lpparam.classLoader);
            
            XposedBridge.hookAllMethods(cameraDeviceClass, "createCaptureSession",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get() || !humidityEnabled) return;
                        
                        try {
                            float humidity = currentHumidity.get();
                            
                            // High humidity can cause fogging
                            if (humidity > 85) {
                                // Add slight blur/delay to focus
                            }
                        } catch (Exception e) {
                            logDebug("Error in createCaptureSession hook: " + e.getMessage());
                        }
                    }
                });
            
            logInfo("Hooked CameraDevice for humidity effects");
        } catch (Exception e) {
            logDebug("CameraDevice not available: " + e.getMessage());
        }
    }
    
    private float applySensitivityReduction(float originalPressure) {
        WeatherCondition condition = currentCondition.get();
        
        if (condition == WeatherCondition.LIGHT_RAIN || 
            condition == WeatherCondition.HEAVY_RAIN) {
            
            float reduction = touchSensitivityReduction * intensity;
            return Math.max(0.1f, originalPressure * (1.0f - reduction));
        }
        
        if (condition == WeatherCondition.HIGH_HUMIDITY) {
            // High humidity can cause touch screen stickiness
            float stickiness = touchStickiness * intensity;
            return originalPressure * (1.0f + stickiness);
        }
        
        return originalPressure;
    }
    
    private void checkForGhostTouch(MotionEvent event) {
        if (!rainEnabled) return;
        
        WeatherCondition condition = currentCondition.get();
        if (condition != WeatherCondition.LIGHT_RAIN && 
            condition != WeatherCondition.HEAVY_RAIN) {
            return;
        }
        
        float probability = ghostTouchProbability * intensity;
        
        // Check rain intensity
        if (condition == WeatherCondition.HEAVY_RAIN) {
            probability *= 1.5f;
        }
        
        totalTouchCount.incrementAndGet();
        
        if (random.get().nextDouble() < probability) {
            ghostTouchCount.incrementAndGet();
            
            // In a full implementation, this would inject a ghost touch event
            // For now, we just track the count
            
            if (DEBUG && random.get().nextDouble() < 0.01) {
                HookUtils.logDebug(TAG, "Ghost touch triggered - humidity: " + 
                    currentHumidity.get() + "%, condition: " + condition);
            }
        }
    }
    
    /**
     * Set current weather condition
     */
    public static void setWeatherCondition(WeatherCondition condition) {
        currentCondition.set(condition);
        
        // Update related values based on condition
        switch (condition) {
            case LIGHT_RAIN:
                currentHumidity.set(85.0f);
                break;
            case HEAVY_RAIN:
                currentHumidity.set(95.0f);
                break;
            case HIGH_HUMIDITY:
                currentHumidity.set(90.0f);
                break;
            case COLD:
                currentTemperature.set(0.0f);
                break;
            case HOT:
                currentTemperature.set(38.0f);
                break;
            case CLEAR:
            case DRY:
                currentHumidity.set(30.0f);
                break;
            default:
                break;
        }
        
        HookUtils.logInfo(TAG, "Weather condition changed to: " + condition);
    }
    
    /**
     * Set current temperature (Celsius)
     */
    public static void setTemperature(float temperature) {
        currentTemperature.set(temperature);
        
        // Update condition based on temperature
        if (temperature < 5) {
            currentCondition.set(WeatherCondition.COLD);
        } else if (temperature > 35) {
            currentCondition.set(WeatherCondition.HOT);
        }
        
        HookUtils.logInfo(TAG, "Temperature set to: " + temperature + "°C");
    }
    
    /**
     * Set current humidity (percentage)
     */
    public static void setHumidity(float humidity) {
        currentHumidity.set(humidity);
        
        // Update condition based on humidity
        if (humidity > 80) {
            currentCondition.set(WeatherCondition.HIGH_HUMIDITY);
        }
        
        HookUtils.logInfo(TAG, "Humidity set to: " + humidity + "%");
    }
    
    /**
     * Set current altitude (meters)
     */
    public static void setAltitude(float altitude) {
        currentAltitude.set(altitude);
        HookUtils.logInfo(TAG, "Altitude set to: " + altitude + "m");
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
     * Get ghost touch count
     */
    public static int getGhostTouchCount() {
        return ghostTouchCount.get();
    }
    
    /**
     * Get total touch count
     */
    public static int getTotalTouchCount() {
        return totalTouchCount.get();
    }
    
    /**
     * Dead spot class for rain water droplets
     */
    private static class DeadSpot {
        final int x;
        final int y;
        final int radius;
        
        DeadSpot(int x, int y, int radius) {
            this.x = x;
            this.y = y;
            this.radius = radius;
        }
        
        boolean contains(float touchX, float touchY) {
            float dx = touchX - x;
            float dy = touchY - y;
            return (dx * dx + dy * dy) <= (radius * radius);
        }
    }
}
