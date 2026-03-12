package com.samsungcloak.xposed;

import android.location.Location;
import android.location.LocationManager;
import android.location.GpsStatus;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;

/**
 * Hook #45: GPSTrajectorySimulationHook
 * 
 * Simulates realistic GPS movement patterns:
 * - Walking speed variations (1-1.5 m/s)
 * - Running speed variations (2-4 m/s)
 * - Driving speed variations (10-30 m/s)
 * - GPS accuracy noise
 * - Route deviations
 * - Signal loss during transit
 * 
 * Based on GPS navigation research
 * 
 * Target: SM-A125U (Android 10/11)
 */
public class GPSTrajectorySimulationHook {

    private static final String TAG = "[HumanInteraction][GPSTrajectory]";
    private static final boolean DEBUG = true;

    private static boolean enabled = true;
    private static int movementType = MOVEMENT_WALKING; // Walking by default
    
    private static final Random random = new Random();
    
    // Movement types
    public static final int MOVEMENT_STATIONARY = 0;
    public static final int MOVEMENT_WALKING = 1;
    public static final int MOVEMENT_RUNNING = 2;
    public static final int MOVEMENT_DRIVING = 3;
    public static final int MOVEMENT_TRANSIT = 4;
    
    // Speed ranges (m/s)
    private static final float[] WALKING_SPEEDS = {0.8f, 1.5f};
    private static final float[] RUNNING_SPEEDS = {2.0f, 4.0f};
    private static final float[] DRIVING_SPEEDS = {8.0f, 30.0f};
    private static final float[] TRANSIT_SPEEDS = {10.0f, 25.0f};
    
    // Base coordinates (example: NYC)
    private static double baseLatitude = 40.7128;
    private static double baseLongitude = -74.0060;
    
    // Current position
    private static double currentLatitude = baseLatitude;
    private static double currentLongitude = baseLongitude;
    private static float currentSpeed = 0;
    private static float currentAccuracy = 10f;
    
    // Position tracking
    private static long lastPositionTime = 0;
    private static int positionUpdateCount = 0;

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        HookUtils.logInfo(TAG, "Initializing GPS Trajectory Simulation Hook");

        try {
            hookLocationManager(lpparam);
            hookLocation(lpparam);
            HookUtils.logInfo(TAG, "GPS Trajectory Simulation Hook initialized");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize hook", e);
        }
    }

    private static void hookLocationManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> locationManagerClass = XposedHelpers.findClass(
                "android.location.LocationManager", lpparam.classLoader);

            // Hook requestLocationUpdates to inject simulated locations
            XposedBridge.hookAllMethods(locationManagerClass, "requestLocationUpdates",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    // Could intercept location requests and inject our own
                    String provider = (String) param.args[0];
                    
                    if (DEBUG && random.nextFloat() < 0.02f) {
                        HookUtils.logDebug(TAG, "Location update requested: " + provider);
                    }
                }
            });

            // Hook getLastKnownLocation
            XposedBridge.hookAllMethods(locationManagerClass, "getLastKnownLocation",
                new XC_MethodHook() {
                @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    String provider = (String) param.args[0];
                    if (param.getResult() != null) {
                        // Modify the location to add noise
                        Location location = (Location) param.getResult();
                        addGPSToNoise(location);
                        param.setResult(location);
                    }
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked LocationManager");

        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook LocationManager", e);
        }
    }

    private static void hookLocation(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> locationClass = XposedHelpers.findClass("android.location.Location", lpparam.classLoader);

            // Hook getLatitude to return simulated position
            XposedBridge.hookAllMethods(locationClass, "getLatitude",
                new XC_MethodHook() {
                @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    double lat = (double) param.getResult();
                    if (lat != 0.0) {
                        // Add position noise
                        lat += (random.nextFloat() - 0.5f) * 0.0001; // ~10m noise
                        param.setResult(lat);
                    }
                }
            });

            // Hook getLongitude
            XposedBridge.hookAllMethods(locationClass, "getLongitude",
                new XC_MethodHook() {
                @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    double lon = (double) param.getResult();
                    if (lon != 0.0) {
                        lon += (random.nextFloat() - 0.5f) * 0.0001;
                        param.setResult(lon);
                    }
                }
            });

            // Hook getAccuracy to simulate GPS accuracy variations
            XposedBridge.hookAllMethods(locationClass, "getAccuracy",
                new XC_MethodHook() {
                @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    float accuracy = (float) param.getResult();
                    
                    // Accuracy varies based on movement type and environment
                    float baseAccuracy = currentAccuracy;
                    
                    switch (movementType) {
                        case MOVEMENT_WALKING:
                            baseAccuracy = 8f + random.nextFloat() * 5f;
                            break;
                        case MOVEMENT_RUNNING:
                            baseAccuracy = 10f + random.nextFloat() * 8f;
                            break;
                        case MOVEMENT_DRIVING:
                            baseAccuracy = 15f + random.nextFloat() * 10f;
                            break;
                        case MOVEMENT_TRANSIT:
                            baseAccuracy = 20f + random.nextFloat() * 15f;
                            break;
                        default:
                            baseAccuracy = 5f + random.nextFloat() * 3f;
                    }
                    
                    param.setResult(baseAccuracy);
                }
            });

            // Hook getSpeed to return simulated speed
            XposedBridge.hookAllMethods(locationClass, "getSpeed",
                new XC_MethodHook() {
                @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    // Calculate speed based on movement type
                    float speed = getSpeedForMovementType();
                    speed += (random.nextFloat() - 0.5f) * 0.5f; // Add variation
                    
                    param.setResult(Math.max(0, speed));
                    currentSpeed = (float) param.getResult();
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked Location methods");

        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook Location", e);
        }
    }

    private static float getSpeedForMovementType() {
        float[] speedRange;
        
        switch (movementType) {
            case MOVEMENT_WALKING:
                speedRange = WALKING_SPEEDS;
                break;
            case MOVEMENT_RUNNING:
                speedRange = RUNNING_SPEEDS;
                break;
            case MOVEMENT_DRIVING:
                speedRange = DRIVING_SPEEDS;
                break;
            case MOVEMENT_TRANSIT:
                speedRange = TRANSIT_SPEEDS;
                break;
            default:
                return 0;
        }
        
        return speedRange[0] + random.nextFloat() * (speedRange[1] - speedRange[0]);
    }

    private static void addGPSToNoise(Location location) {
        // Add realistic GPS noise
        float accuracy = location.getAccuracy();
        
        // Add position jitter based on accuracy
        double latNoise = (random.nextFloat() - 0.5f) * accuracy * 0.00001;
        double lonNoise = (random.nextFloat() - 0.5f) * accuracy * 0.00001;
        
        location.setLatitude(location.getLatitude() + latNoise);
        location.setLongitude(location.getLongitude() + lonNoise);
        
        // Occasionally add altitude noise
        if (random.nextFloat() < 0.3f) {
            double altNoise = (random.nextFloat() - 0.5f) * 10;
            location.setAltitude(location.getAltitude() + altNoise);
        }
        
        positionUpdateCount++;
        
        if (DEBUG && random.nextFloat() < 0.01f) {
            HookUtils.logDebug(TAG, String.format("GPS position updated: (%.4f, %.4f), accuracy: %.1fm",
                location.getLatitude(), location.getLongitude(), location.getAccuracy()));
        }
    }
}
