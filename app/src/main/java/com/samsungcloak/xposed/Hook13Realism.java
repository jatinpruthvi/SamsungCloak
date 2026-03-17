package com.samsungcloak.xposed;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.GnssStatus;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Hook13Realism - GPS Trajectory & Location Context Realism
 * 
 * Implements realistic GPS behavior for Samsung Galaxy A12 (SM-A125U):
 * - Signal quality simulation: urban canyon (20-50m), outdoor (5-15m), indoor (50-200m)
 * - Trajectory smoothing: GPS lag 1-3s, speed overestimation on turns 10-30%
 * - Multi-path interference: false location jumps 2-5%, satellite count fluctuation
 * - Commute simulation: regularized movement patterns, location-based context detection
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class Hook13Realism extends BaseRealismHook {

    private static final String TAG = "[Realism][Hook13-GPS]";
    private static final boolean DEBUG = true;
    
    private static final String HOOK_ID = "hook_13";
    private static final String HOOK_NAME = "GPS Trajectory & Location Context";
    
    // Configuration keys
    private static final String KEY_ENABLED = "gps_trajectory_enabled";
    private static final String KEY_INTENSITY = "gps_trajectory_intensity";
    private static final String KEY_URBAN_ACCURACY = "urban_accuracy_enabled";
    private static final String KEY_TRAJECTORY_SMOOTHING = "trajectory_smoothing_enabled";
    private static final String KEY_MULTIPATH = "multipath_interference_enabled";
    private static final String KEY_COMMUTE_SIM = "commute_simulation_enabled";
    
    // Hook state
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    private static AtomicBoolean enabled = new AtomicBoolean(true);
    private static float intensity = 0.5f;
    
    // Location context types
    public enum LocationContext {
        URBAN_CANYON,
        OUTDOOR,
        INDOOR,
        TRANSIT,
        VEHICLE,
        PEDESTRIAN
    }
    
    // Current location state
    private static final AtomicReference<LocationContext> currentContext = 
        new AtomicReference<>(LocationContext.OUTDOOR);
    private static final AtomicReference<String> currentProvider = 
        new AtomicReference<>(LocationManager.GPS_PROVIDER);
    
    // Location smoothing buffers
    private static final List<Location> locationBuffer = new CopyOnWriteArrayList<>();
    private static final int BUFFER_SIZE = 10;
    
    // GPS accuracy by context
    private static float urbanMinAccuracy = 20.0f;
    private static float urbanMaxAccuracy = 50.0f;
    private static float outdoorMinAccuracy = 5.0f;
    private static float outdoorMaxAccuracy = 15.0f;
    private static float indoorMinAccuracy = 50.0f;
    private static float indoorMaxAccuracy = 200.0f;
    
    // Trajectory parameters
    private static int gpsLagMs = 1500;
    private static float turnSpeedOverestimation = 0.20f;
    private static float altitudeJitterMeters = 15.0f;
    
    // Multi-path interference
    private static float falseJumpProbability = 0.03f;
    private static float satelliteCountVariance = 0.25f;
    
    // Commute simulation
    private static boolean commuteSimulationEnabled = true;
    private static final Map<String, long[]> commuteSchedule = new ConcurrentHashMap<>();
    private static final List<String> routineLocations = new CopyOnWriteArrayList<>();
    
    // Last location
    private static double lastLatitude = 37.7749;
    private static double lastLongitude = -122.4194;
    private static long lastLocationTime = 0;
    
    // Thread-local random
    private static final ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new Random());
    
    public Hook13Realism() {
        super(HOOK_ID, HOOK_NAME);
    }
    
    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!hookInitialized.compareAndSet(false, true)) {
            logInfo("Hook already initialized");
            return;
        }
        
        logInfo("Initializing GPS Trajectory & Location Context Realism Hook");
        
        try {
            // Load configuration
            loadConfiguration();
            
            // Hook LocationManager methods
            hookLocationManager(lpparam);
            
            // Hook Location class
            hookLocationClass(lpparam);
            
            // Hook Samsung-specific location services
            hookSamsungLocation(lpparam);
            
            // Hook GnssStatus callback
            hookGnssStatus(lpparam);
            
            logInfo("GPS Trajectory Hook initialized successfully");
        } catch (Exception e) {
            logError("Failed to initialize GPS Trajectory Hook", e);
        }
    }
    
    private void loadConfiguration() {
        if (configManager != null) {
            enabled.set(configManager.getHookParamBool(HOOK_ID, KEY_ENABLED, true));
            intensity = configManager.getHookIntensity(HOOK_ID);
            
            urbanMinAccuracy = configManager.getHookParamFloat(HOOK_ID, "urban_min_accuracy", 20.0f);
            urbanMaxAccuracy = configManager.getHookParamFloat(HOOK_ID, "urban_max_accuracy", 50.0f);
            outdoorMinAccuracy = configManager.getHookParamFloat(HOOK_ID, "outdoor_min_accuracy", 5.0f);
            outdoorMaxAccuracy = configManager.getHookParamFloat(HOOK_ID, "outdoor_max_accuracy", 15.0f);
            indoorMinAccuracy = configManager.getHookParamFloat(HOOK_ID, "indoor_min_accuracy", 50.0f);
            indoorMaxAccuracy = configManager.getHookParamFloat(HOOK_ID, "indoor_max_accuracy", 200.0f);
            
            gpsLagMs = configManager.getHookParamInt(HOOK_ID, "gps_lag_ms", 1500);
            turnSpeedOverestimation = configManager.getHookParamFloat(HOOK_ID, "turn_speed_overestimation", 0.20f);
            altitudeJitterMeters = configManager.getHookParamFloat(HOOK_ID, "altitude_jitter", 15.0f);
            
            falseJumpProbability = configManager.getHookParamFloat(HOOK_ID, "false_jump_probability", 0.03f);
            satelliteCountVariance = configManager.getHookParamFloat(HOOK_ID, "satellite_variance", 0.25f);
            
            commuteSimulationEnabled = configManager.getHookParamBool(HOOK_ID, KEY_COMMUTE_SIM, true);
        }
        
        logInfo("Configuration loaded: enabled=" + enabled.get() + ", intensity=" + intensity);
    }
    
    private void hookLocationManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> locationManagerClass = XposedHelpers.findClass(
                "android.location.LocationManager", lpparam.classLoader);
            
            // Hook requestLocationUpdates
            XposedBridge.hookAllMethods(locationManagerClass, "requestLocationUpdates",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get()) return;
                        
                        try {
                            String provider = (String) param.args[0];
                            long minTime = (Long) param.args[1];
                            float minDistance = (Float) param.args[2];
                            
                            currentProvider.set(provider);
                            
                            // Add GPS lag simulation
                            long adjustedMinTime = minTime + (long)(gpsLagMs * intensity);
                            param.args[1] = adjustedMinTime;
                            
                            if (DEBUG && random.get().nextDouble() < 0.01) {
                                logDebug("requestLocationUpdates: provider=" + provider + 
                                    ", minTime=" + adjustedMinTime + ", minDistance=" + minDistance);
                            }
                        } catch (Exception e) {
                            logDebug("Error in requestLocationUpdates hook: " + e.getMessage());
                        }
                    }
                });
            
            // Hook getLastKnownLocation
            XposedBridge.hookAllMethods(locationManagerClass, "getLastKnownLocation",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get()) return;
                        if (param.getResult() == null) return;
                        
                        try {
                            Location original = (Location) param.getResult();
                            Location modified = simulateLocationAccuracy(original);
                            param.setResult(modified);
                        } catch (Exception e) {
                            logDebug("Error in getLastKnownLocation hook: " + e.getMessage());
                        }
                    }
                });
            
            logInfo("Hooked LocationManager methods");
        } catch (Exception e) {
            logError("Failed to hook LocationManager", e);
        }
    }
    
    private void hookLocationClass(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> locationClass = XposedHelpers.findClass(
                "android.location.Location", lpparam.classLoader);
            
            // Hook distanceBetween
            XposedBridge.hookAllMethods(locationClass, "distanceBetween",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get()) return;
                        
                        try {
                            // Add slight distance calculation variance
                            if (random.get().nextDouble() < 0.1 * intensity) {
                                float variance = 1.0f + (random.get().nextFloat() - 0.5f) * 0.05f;
                                float[] results = (float[]) param.args[4];
                                if (results != null && results.length >= 3) {
                                    results[0] *= variance;
                                    results[1] *= variance;
                                    results[2] *= variance;
                                }
                            }
                        } catch (Exception e) {
                            logDebug("Error in distanceBetween hook: " + e.getMessage());
                        }
                    }
                });
            
            logInfo("Hooked Location class methods");
        } catch (Exception e) {
            logError("Failed to hook Location class", e);
        }
    }
    
    private void hookSamsungLocation(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook Samsung's SLocationManager if available
            Class<?> sLocationManagerClass = XposedHelpers.findClass(
                "com.samsung.android.location.S LocationManager", lpparam.classLoader);
            
            XposedBridge.hookAllMethods(sLocationManagerClass, "getCurrentLocation",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get()) return;
                        if (param.getResult() == null) return;
                        
                        try {
                            Location location = (Location) param.getResult();
                            param.setResult(simulateLocationAccuracy(location));
                        } catch (Exception e) {
                            logDebug("Error in Samsung location hook: " + e.getMessage());
                        }
                    }
                });
            
            logInfo("Hooked Samsung location services");
        } catch (ClassNotFoundException e) {
            logDebug("Samsung SLocationManager not found (non-Samsung device or different version)");
        } catch (Exception e) {
            logError("Failed to hook Samsung location", e);
        }
    }
    
    private void hookGnssStatus(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> gnssStatusClass = XposedHelpers.findClass(
                "android.location.GnssStatus", lpparam.classLoader);
            
            // Hook getSatelliteCount
            XposedBridge.hookAllMethods(gnssStatusClass, "getSatelliteCount",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get()) return;
                        
                        try {
                            int original = (Integer) param.getResult();
                            // Add satellite count variance
                            int variance = (int)(original * satelliteCountVariation * intensity);
                            int adjusted = Math.max(0, original + random.get().nextInt(variance * 2 + 1) - variance);
                            param.setResult(adjusted);
                        } catch (Exception e) {
                            logDebug("Error in GnssStatus hook: " + e.getMessage());
                        }
                    }
                });
            
            logInfo("Hooked GnssStatus");
        } catch (Exception e) {
            logDebug("Failed to hook GnssStatus: " + e.getMessage());
        }
    }
    
    private Location simulateLocationAccuracy(Location location) {
        if (location == null || !enabled.get()) return location;
        
        try {
            // Determine context-based accuracy
            float[] accuracyRange = getAccuracyRange(currentContext.get());
            float baseAccuracy = accuracyRange[0] + random.get().nextFloat() * 
                (accuracyRange[1] - accuracyRange[0]);
            
            // Apply intensity
            float adjustedAccuracy = baseAccuracy * (0.5f + intensity * 0.5f);
            
            // Create modified location with adjusted accuracy
            Location modified = new Location(location);
            modified.setAccuracy(adjustedAccuracy);
            
            // Add multi-path interference (false location jumps)
            if (random.get().nextDouble() < falseJumpProbability * intensity) {
                double jumpMeters = 10 + random.get().nextDouble() * 50;
                double angle = random.get().nextDouble() * 2 * Math.PI;
                modified.setLatitude(modified.getLatitude() + (jumpMeters / 111000) * Math.cos(angle));
                modified.setLongitude(modified.getLongitude() + (jumpMeters / 111000) * Math.sin(angle));
            }
            
            // Add altitude jitter
            if (location.hasAltitude()) {
                double altitudeJitter = (random.get().nextDouble() - 0.5) * 2 * altitudeJitterMeters;
                modified.setAltitude(location.getAltitude() + altitudeJitter);
            }
            
            // Add speed overestimation on turns (simplified)
            if (location.hasSpeed() && random.get().nextDouble() < turnSpeedOverestimation * intensity) {
                float speedVariance = 1.0f + (random.get().nextFloat() * 0.3f);
                modified.setSpeed(location.getSpeed() * speedVariance);
            }
            
            // Update buffer
            locationBuffer.add(modified);
            while (locationBuffer.size() > BUFFER_SIZE) {
                locationBuffer.remove(0);
            }
            
            lastLatitude = modified.getLatitude();
            lastLongitude = modified.getLongitude();
            lastLocationTime = System.currentTimeMillis();
            
            return modified;
        } catch (Exception e) {
            logDebug("Error simulating location accuracy: " + e.getMessage());
            return location;
        }
    }
    
    private float[] getAccuracyRange(LocationContext context) {
        switch (context) {
            case URBAN_CANYON:
                return new float[]{urbanMinAccuracy, urbanMaxAccuracy};
            case INDOOR:
                return new float[]{indoorMinAccuracy, indoorMaxAccuracy};
            case OUTDOOR:
            default:
                return new float[]{outdoorMinAccuracy, outdoorMaxAccuracy};
        }
    }
    
    /**
     * Set location context based on sensor data or app state
     */
    public static void setLocationContext(LocationContext context) {
        currentContext.set(context);
        if (DEBUG) {
            HookUtils.logInfo(TAG, "Location context changed to: " + context);
        }
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
     * Get current location context
     */
    public static LocationContext getLocationContext() {
        return currentContext.get();
    }
}
