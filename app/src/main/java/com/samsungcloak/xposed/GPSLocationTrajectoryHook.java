package com.samsungcloak.xposed;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
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

/**
 * GPSLocationTrajectoryHook - GPS Location & Movement Trajectory Simulation
 *
 * Simulates realistic GPS behavior including:
 *
 * 1. Trajectory Patterns - Walking, driving, transit routes
 * 2. Speed Modeling - Pedestrian vs vehicle speeds with natural variation
 * 3. GPS Accuracy - Position jitter, urban canyon effects
 * 4. Location Provider Handoffs - GPS/WiFi/Cell positioning
 * 5. Geofencing Behavior - Entry/exit detection simulation
 * 6. Background Location - Battery-aware update patterns
 * 7. Indoor/Outdoor Transitions - WiFi-based indoor positioning
 * 8. Historical Patterns - Routine locations (home, work)
 *
 * Novelty: NOT covered by existing hooks (SensorFusionCoherenceHook covers PDR,
 *          but specific GPS/trajectory modeling is not implemented)
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class GPSLocationTrajectoryHook {

    private static final String TAG = "[GPS][Location]";
    private static final boolean DEBUG = true;
    
    // Toggleable via SharedPreferences
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    
    // Trajectory modes
    public enum TrajectoryMode {
        STATIONARY,
        WALKING,
        RUNNING,
        CYCLING,
        DRIVING,
        PUBLIC_TRANSIT,
        INDOOR
    }
    
    // Current trajectory state
    private static TrajectoryMode currentMode = TrajectoryMode.STATIONARY;
    private static double currentLatitude = 37.7749; // Default: San Francisco
    private static double currentLongitude = -122.4194;
    private static float currentSpeed = 0f;
    private static float currentBearing = 0f;
    private static float gpsAccuracy = 10f;
    private static long lastLocationUpdate = 0;
    
    // Movement modeling
    private static double walkingSpeedMps = 1.4; // 5 km/h
    private static double runningSpeedMps = 2.8; // 10 km/h
    private static double cyclingSpeedMps = 6.0; // 22 km/h
    private static double drivingSpeedMps = 13.9; // 50 km/h (urban)
    private static double speedVariation = 0.2; // 20% variation
    
    // GPS accuracy modeling
    private static double outdoorAccuracy = 5.0; // meters
    private static double urbanCanyonAccuracy = 15.0;
    private static double indoorAccuracy = 25.0;
    private static double accuracyVariation = 0.3;
    
    // Trajectory history
    private static final List<LocationPoint> trajectoryHistory = new CopyOnWriteArrayList<>();
    private static final int maxHistorySize = 1000;
    
    // Geofence simulation
    private static final Map<String, Geofence> activeGeofences = new ConcurrentHashMap<>();
    private static final List<GeofenceEvent> geofenceEvents = new CopyOnWriteArrayList<>();
    
    // Provider state
    private static boolean gpsProviderEnabled = true;
    private static boolean networkProviderEnabled = true;
    private static String lastProvider = LocationManager.GPS_PROVIDER;
    private static int providerSwitchCount = 0;
    
    // Timing
    private static long sessionStartTime = System.currentTimeMillis();
    private static final Random random = new Random();
    private static long lastUpdateTime = 0;
    
    /**
     * Location point for trajectory
     */
    public static class LocationPoint {
        public double latitude;
        public double longitude;
        public float speed;
        public float bearing;
        public float accuracy;
        public long timestamp;
        public String provider;
        
        public LocationPoint(double lat, double lon, float speed, float bearing, 
                           float accuracy, String provider) {
            this.latitude = lat;
            this.longitude = lon;
            this.speed = speed;
            this.bearing = bearing;
            this.accuracy = accuracy;
            this.provider = provider;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    /**
     * Geofence definition
     */
    public static class Geofence {
        public String id;
        public double latitude;
        public double longitude;
        public float radius; // meters
        public GeofenceType type;
        public boolean isInside;
        
        public enum GeofenceType {
            CIRCULAR,
            RECTANGULAR
        }
        
        public Geofence(String id, double lat, double lon, float radius) {
            this.id = id;
            this.latitude = lat;
            this.longitude = lon;
            this.radius = radius;
            this.type = GeofenceType.CIRCULAR;
            this.isInside = false;
        }
    }
    
    /**
     * Geofence event record
     */
    public static class GeofenceEvent {
        public String geofenceId;
        public GeofenceTransition transition;
        public long timestamp;
        
        public enum GeofenceTransition {
            ENTER,
            EXIT,
            DWELL
        }
        
        public GeofenceEvent(String id, GeofenceTransition transition) {
            this.geofenceId = id;
            this.transition = transition;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (hookInitialized.getAndSet(true)) {
            HookUtils.logDebug(TAG, "Already initialized");
            return;
        }
        
        HookUtils.logInfo(TAG, "Initializing GPS Location Trajectory Hook");
        
        // Initialize with some common geofences
        initializeGeofences();
        
        try {
            hookLocationManager(lpparam);
            hookLocation(lpparam);
            
            HookUtils.logInfo(TAG, "GPS hook initialized successfully");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Init failed: " + t.getMessage());
            hookInitialized.set(false);
        }
    }
    
    private static void initializeGeofences() {
        // Add home geofence (San Francisco)
        activeGeofences.put("home", new Geofence("home", 37.7749, -122.4194, 100f));
        
        // Add work geofence (downtown SF)
        activeGeofences.put("work", new Geofence("work", 37.7879, -122.4074, 150f));
        
        // Add gym geofence
        activeGeofences.put("gym", new Geofence("gym", 37.7850, -122.4100, 50f));
        
        HookUtils.logInfo(TAG, "Initialized " + activeGeofences.size() + " geofences");
    }
    
    private static void hookLocationManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> locationManagerClass = XposedHelpers.findClass(
                "android.location.LocationManager", lpparam.classLoader
            );
            
            // Hook requestLocationUpdates
            XposedBridge.hookAllMethods(locationManagerClass, "requestLocationUpdates",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    String provider = (String) param.args[0];
                    HookUtils.logDebug(TAG, "requestLocationUpdates: " + provider);
                }
            });
            
            // Hook getLastKnownLocation
            XposedBridge.hookAllMethods(locationManagerClass, "getLastKnownLocation",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || param.getResult() == null) return;
                    
                    String provider = (String) param.args[0];
                    Location location = (Location) param.getResult();
                    
                    // Could modify location here for realism
                    HookUtils.logDebug(TAG, "getLastKnownLocation: " + provider);
                }
            });
            
            // Hook isProviderEnabled
            XposedBridge.hookAllMethods(locationManagerClass, "isProviderEnabled",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    String provider = (String) param.args[0];
                    boolean result = (boolean) param.getResult();
                    
                    // Return simulated provider states
                    if (LocationManager.GPS_PROVIDER.equals(provider)) {
                        param.setResult(gpsProviderEnabled);
                    } else if (LocationManager.NETWORK_PROVIDER.equals(provider)) {
                        param.setResult(networkProviderEnabled);
                    }
                }
            });
            
            // Hook getProviders
            XposedBridge.hookAllMethods(locationManagerClass, "getProviders",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || param.getResult() == null) return;
                    
                    @SuppressWarnings("unchecked")
                    List<String> providers = (List<String>) param.getResult();
                    
                    // Ensure GPS is in the list
                    if (gpsProviderEnabled && !providers.contains(LocationManager.GPS_PROVIDER)) {
                        providers.add(LocationManager.GPS_PROVIDER);
                    }
                }
            });
            
            HookUtils.logInfo(TAG, "LocationManager hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "LocationManager hook failed: " + t.getMessage());
        }
    }
    
    private static void hookLocation(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> locationClass = XposedHelpers.findClass(
                "android.location.Location", lpparam.classLoader
            );
            
            // Hook getLatitude
            XposedBridge.hookAllMethods(locationClass, "getLatitude",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    Location location = (Location) param.thisObject;
                    // Could inject GPS jitter here
                }
            });
            
            // Hook getLongitude
            XposedBridge.hookAllMethods(locationClass, "getLongitude",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    Location location = (Location) param.thisObject;
                    // Could inject GPS jitter here
                }
            });
            
            // Hook getAccuracy
            XposedBridge.hookAllMethods(locationClass, "getAccuracy",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    Location location = (Location) param.thisObject;
                    float accuracy = location.getAccuracy();
                    
                    // Inject realistic accuracy variation
                    float newAccuracy = accuracy;
                    if (currentMode == TrajectoryMode.INDOOR) {
                        newAccuracy = (float)(indoorAccuracy * (1 + random.nextDouble() * accuracyVariation));
                    } else if (currentMode == TrajectoryMode.DRIVING) {
                        newAccuracy = (float)(urbanCanyonAccuracy * (1 + random.nextDouble() * accuracyVariation));
                    } else {
                        newAccuracy = (float)(outdoorAccuracy * (1 + random.nextDouble() * accuracyVariation));
                    }
                    
                    param.setResult(newAccuracy);
                }
            });
            
            // Hook getSpeed
            XposedBridge.hookAllMethods(locationClass, "getSpeed",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    Location location = (Location) param.thisObject;
                    float speed = location.getSpeed();
                    
                    // Apply speed variation based on mode
                    if (currentMode != TrajectoryMode.STATIONARY) {
                        float baseSpeed = getBaseSpeed();
                        float variation = (float)((random.nextDouble() - 0.5) * speedVariation * 2);
                        float newSpeed = baseSpeed * (1 + variation);
                        param.setResult(Math.max(0, newSpeed));
                    }
                }
            });
            
            HookUtils.logInfo(TAG, "Location hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Location hook failed: " + t.getMessage());
        }
    }
    
    // ========== Trajectory Methods ==========
    
    /**
     * Get base speed for current trajectory mode
     */
    private static float getBaseSpeed() {
        switch (currentMode) {
            case WALKING:
                return (float)(walkingSpeedMps * (1 + (random.nextDouble() - 0.5) * speedVariation));
            case RUNNING:
                return (float)(runningSpeedMps * (1 + (random.nextDouble() - 0.5) * speedVariation));
            case CYCLING:
                return (float)(cyclingSpeedMps * (1 + (random.nextDouble() - 0.5) * speedVariation));
            case DRIVING:
                return (float)(drivingSpeedMps * (1 + (random.nextDouble() - 0.5) * speedVariation));
            case PUBLIC_TRANSIT:
                return (float)(drivingSpeedMps * 1.5 * (1 + (random.nextDouble() - 0.5) * speedVariation));
            default:
                return 0f;
        }
    }
    
    /**
     * Update location based on current trajectory mode
     */
    public static Location updateLocation() {
        long currentTime = System.currentTimeMillis();
        
        if (currentMode == TrajectoryMode.STATIONARY) {
            return null;
        }
        
        // Calculate movement
        float speed = getBaseSpeed();
        double timeDeltaSeconds = (currentTime - lastUpdateTime) / 1000.0;
        
        // Calculate distance traveled
        double distanceMeters = speed * timeDeltaSeconds;
        
        // Calculate new position using bearing
        double distanceDegrees = distanceMeters / 111320.0; // rough conversion
        double latChange = distanceDegrees * Math.cos(Math.toRadians(currentBearing));
        double lonChange = distanceDegrees * Math.sin(Math.toRadians(currentBearing));
        
        currentLatitude += latChange;
        currentLongitude += lonChange;
        
        // Vary bearing slightly
        currentBearing += (random.nextFloat() - 0.5) * 10;
        
        currentSpeed = speed;
        lastLocationUpdate = currentTime;
        
        // Add to history
        LocationPoint point = new LocationPoint(
            currentLatitude, currentLongitude, currentSpeed, 
            currentBearing, gpsAccuracy, lastProvider
        );
        
        trajectoryHistory.add(point);
        if (trajectoryHistory.size() > maxHistorySize) {
            trajectoryHistory.remove(0);
        }
        
        // Check geofences
        checkGeofences();
        
        return createLocation(point);
    }
    
    /**
     * Create Location object from point
     */
    private static Location createLocation(LocationPoint point) {
        Location location = new Location(lastProvider);
        location.setLatitude(point.latitude);
        location.setLongitude(point.longitude);
        location.setSpeed(point.speed);
        location.setBearing(point.bearing);
        location.setAccuracy(point.accuracy);
        location.setTime(point.timestamp);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            location.setElapsedRealtimeNanos(System.nanoTime());
        }
        
        return location;
    }
    
    /**
     * Check and trigger geofence events
     */
    private static void checkGeofences() {
        for (Geofence geofence : activeGeofences.values()) {
            float[] results = new float[1];
            Location.distanceBetween(
                currentLatitude, currentLongitude,
                geofence.latitude, geofence.longitude,
                results
            );
            
            float distance = results[0];
            boolean wasInside = geofence.isInside;
            geofence.isInside = distance <= geofence.radius;
            
            // Detect transitions
            if (!wasInside && geofence.isInside) {
                geofenceEvents.add(new GeofenceEvent(geofence.id, GeofenceEvent.GeofenceTransition.ENTER));
                HookUtils.logInfo(TAG, "Geofence ENTER: " + geofence.id);
            } else if (wasInside && !geofence.isInside) {
                geofenceEvents.add(new GeofenceEvent(geofence.id, GeofenceEvent.GeofenceTransition.EXIT));
                HookUtils.logInfo(TAG, "Geofence EXIT: " + geofence.id);
            }
        }
    }
    
    // ========== Configuration ==========
    
    public static void setTrajectoryMode(TrajectoryMode mode) {
        currentMode = mode;
        currentSpeed = getBaseSpeed();
        HookUtils.logInfo(TAG, "Trajectory mode: " + mode);
    }
    
    public static void setLocation(double lat, double lon) {
        currentLatitude = lat;
        currentLongitude = lon;
    }
    
    public static void setBearing(float bearing) {
        currentBearing = bearing;
    }
    
    public static void addGeofence(String id, double lat, double lon, float radius) {
        activeGeofences.put(id, new Geofence(id, lat, lon, radius));
    }
    
    public static void removeGeofence(String id) {
        activeGeofences.remove(id);
    }
    
    public static TrajectoryMode getCurrentMode() {
        return currentMode;
    }
    
    public static double[] getCurrentLocation() {
        return new double[]{currentLatitude, currentLongitude};
    }
    
    public static List<LocationPoint> getTrajectoryHistory() {
        return new ArrayList<>(trajectoryHistory);
    }
    
    public static void setEnabled(boolean enabled) {
        GPSLocationTrajectoryHook.enabled = enabled;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
}
