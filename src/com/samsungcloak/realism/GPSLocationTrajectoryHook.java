package com.samsungcloak.realism;

import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.SystemClock;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;

/**
 * GPSLocationTrajectoryHook - Realistic GPS trajectory and location behavior
 *
 * Simulates:
 * - Continuous trajectory with physics-based motion
 * - GPS signal acquisition time (TTFF - Time To First Fix)
 * - Accuracy degradation in urban/indoor environments
 * - Multipath effects in urban canyons
 * - Location update interval variation
 *
 * Device: Samsung Galaxy A12 (SM-A125U) Android 10/11
 */
public class GPSLocationTrajectoryHook {
    private static final String TAG = "GPSLocationTrajectory";

    // Configuration keys
    private static final String KEY_ENABLED = "gps_trajectory_enabled";
    private static final String KEY_TRAJECTORY_PHYSICS = "gps_physics";
    private static final String KEY_TTFF_SIMULATION = "gps_ttff";
    private static final String KEY_ACCURACY_DEGRADATION = "gps_accuracy";
    private static final String KEY_URBAN_MULTIPATH = "gps_multipath";

    // GPS characteristics (SM-A125U with MediaTek MT6765)
    private static final float GPS_ACCURACY_OPEN_SKY = 3.0f;
    private static final float GPS_ACCURACY_URBAN = 15.0f;
    private static final float GPS_ACCURACY_INDOOR = 50.0f;
    private static final long TTFF_COLD_START_MS = 35000;
    private static final long TTFF_WARM_START_MS = 5000;
    private static final long TTFF_HOT_START_MS = 1000;

    // Trajectory parameters
    private static final float MAX_WALKING_SPEED = 1.5f;
    private static final float MAX_RUNNING_SPEED = 4.0f;
    private static final float MAX_DRIVING_SPEED = 25.0f;
    private static final float ACCELERATION_MAX = 2.0f;

    // State
    private static SharedPreferences sPrefs;
    private static boolean sEnabled = true;
    private static boolean sTrajectoryPhysics = true;
    private static boolean sTtffSimulation = true;
    private static boolean sAccuracyDegradation = true;
    private static boolean sUrbanMultipath = true;

    // Runtime state
    private static final Random sRandom = new Random();
    private static double sCurrentLat = 40.7128;
    private static double sCurrentLon = -74.0060;
    private static double sCurrentAltitude = 10.0;
    private static float sCurrentSpeed = 0.0f;
    private static float sCurrentBearing = 0.0f;
    private static float sCurrentAccuracy = GPS_ACCURACY_OPEN_SKY;
    private static long sLastLocationUpdate = 0;
    private static long sFixAcquisitionTime = 0;
    private static boolean sHasFix = false;

    // Trajectory state
    public enum MotionState {
        STATIONARY, WALKING, RUNNING, DRIVING, TRANSIT
    }
    private static MotionState sMotionState = MotionState.STATIONARY;
    private static long sMotionStateStartTime = 0;

    // Environment state
    public enum Environment {
        OPEN_SKY, URBAN, INDOOR, UNDERGROUND
    }
    private static Environment sCurrentEnvironment = Environment.OPEN_SKY;

    public static void init(SharedPreferences prefs) {
        sPrefs = prefs;
        reloadSettings();
        sFixAcquisitionTime = System.currentTimeMillis() + TTFF_WARM_START_MS;
    }

    public static void reloadSettings() {
        if (sPrefs == null) return;
        sEnabled = sPrefs.getBoolean(KEY_ENABLED, true);
        sTrajectoryPhysics = sPrefs.getBoolean(KEY_TRAJECTORY_PHYSICS, true);
        sTtffSimulation = sPrefs.getBoolean(KEY_TTFF_SIMULATION, true);
        sAccuracyDegradation = sPrefs.getBoolean(KEY_ACCURACY_DEGRADATION, true);
        sUrbanMultipath = sPrefs.getBoolean(KEY_URBAN_MULTIPATH, true);
    }

    /**
     * Hook LocationManager for GPS provider
     */
    public static void hookLocationManager(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!sEnabled) return;

        try {
            Class<?> locationManagerClass = XposedHelpers.findClass(
                "android.location.LocationManager", lpparam.classLoader);

            // Hook getLastKnownLocation()
            XposedBridge.hookAllMethods(locationManagerClass, "getLastKnownLocation",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!sEnabled) return;

                        String provider = (String) param.args[0];

                        if (LocationManager.GPS_PROVIDER.equals(provider)) {
                            Location location = generateRealisticLocation();
                            if (location != null) {
                                param.setResult(location);
                            }
                        }
                    }
                });

            // Hook requestLocationUpdates()
            XposedBridge.hookAllMethods(locationManagerClass, "requestLocationUpdates",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!sEnabled) return;

                        String provider = (String) param.args[0];
                        if (LocationManager.GPS_PROVIDER.equals(provider)) {
                            XposedBridge.log(TAG + ": GPS location updates requested");
                        }
                    }
                });

            // Hook getCurrentLocation() (API 30+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                XposedBridge.hookAllMethods(locationManagerClass, "getCurrentLocation",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            if (!sEnabled) return;

                            String provider = (String) param.args[0];
                            if (LocationManager.GPS_PROVIDER.equals(provider)) {
                                if (sTtffSimulation && !sHasFix) {
                                    simulateTTFF();
                                }
                            }
                        }
                    });
            }

            XposedBridge.log(TAG + ": LocationManager hooks installed");

        } catch (Exception e) {
            XposedBridge.log(TAG + ": Error hooking LocationManager: " + e.getMessage());
        }
    }

    /**
     * Hook Location class for property modification
     */
    public static void hookLocation(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!sEnabled || (!sAccuracyDegradation && !sUrbanMultipath)) return;

        try {
            Class<?> locationClass = XposedHelpers.findClass(
                "android.location.Location", lpparam.classLoader);

            // Hook getAccuracy()
            XposedBridge.hookAllMethods(locationClass, "getAccuracy",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!sEnabled || !sAccuracyDegradation) return;

                        Location location = (Location) param.thisObject;

                        if (LocationManager.GPS_PROVIDER.equals(location.getProvider())) {
                            float accuracy = calculateAccuracy();
                            param.setResult(accuracy);
                        }
                    }
                });

            // Hook getSpeed() for physics-based speed
            XposedBridge.hookAllMethods(locationClass, "getSpeed",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!sEnabled || !sTrajectoryPhysics) return;

                        Location location = (Location) param.thisObject;

                        if (LocationManager.GPS_PROVIDER.equals(location.getProvider())) {
                            param.setResult(sCurrentSpeed);
                        }
                    }
                });

            // Hook getBearing()
            XposedBridge.hookAllMethods(locationClass, "getBearing",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!sEnabled || !sTrajectoryPhysics) return;

                        Location location = (Location) param.thisObject;

                        if (LocationManager.GPS_PROVIDER.equals(location.getProvider())) {
                            param.setResult(sCurrentBearing);
                        }
                    }
                });

            XposedBridge.log(TAG + ": Location hooks installed");

        } catch (Exception e) {
            XposedBridge.log(TAG + ": Error hooking Location: " + e.getMessage());
        }
    }

    private static Location generateRealisticLocation() {
        if (sTtffSimulation && !sHasFix) {
            long now = System.currentTimeMillis();
            if (now < sFixAcquisitionTime) {
                return null;
            }
            sHasFix = true;
        }

        if (sTrajectoryPhysics) {
            updateTrajectory();
        }

        Location location = new Location(LocationManager.GPS_PROVIDER);
        location.setLatitude(sCurrentLat);
        location.setLongitude(sCurrentLon);
        location.setAltitude(sCurrentAltitude);
        location.setSpeed(sCurrentSpeed);
        location.setBearing(sCurrentBearing);
        location.setAccuracy(calculateAccuracy());
        location.setTime(System.currentTimeMillis());
        location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());

        sLastLocationUpdate = System.currentTimeMillis();

        return location;
    }

    private static void updateTrajectory() {
        long now = System.currentTimeMillis();
        float deltaTime = (now - sLastLocationUpdate) / 1000.0f;

        if (deltaTime <= 0) return;

        updateSpeed(deltaTime);
        updateBearing(deltaTime);

        float distance = sCurrentSpeed * deltaTime;

        double latDelta = (distance * Math.cos(Math.toRadians(sCurrentBearing))) / 111320.0;
        double lonDelta = (distance * Math.sin(Math.toRadians(sCurrentBearing))) /
            (111320.0 * Math.cos(Math.toRadians(sCurrentLat)));

        sCurrentLat += latDelta;
        sCurrentLon += lonDelta;

        if (sUrbanMultipath && sCurrentEnvironment == Environment.URBAN) {
            sCurrentLat += (sRandom.nextGaussian() * 0.00001);
            sCurrentLon += (sRandom.nextGaussian() * 0.00001);
        }
    }

    private static void updateSpeed(float deltaTime) {
        float targetSpeed = 0;

        switch (sMotionState) {
            case STATIONARY:
                targetSpeed = 0;
                break;
            case WALKING:
                targetSpeed = 0.5f + sRandom.nextFloat() * (MAX_WALKING_SPEED - 0.5f);
                break;
            case RUNNING:
                targetSpeed = 2.0f + sRandom.nextFloat() * (MAX_RUNNING_SPEED - 2.0f);
                break;
            case DRIVING:
                targetSpeed = 5.0f + sRandom.nextFloat() * (MAX_DRIVING_SPEED - 5.0f);
                break;
            case TRANSIT:
                targetSpeed = 10.0f + sRandom.nextFloat() * 5.0f;
                break;
        }

        float speedDiff = targetSpeed - sCurrentSpeed;
        float maxDelta = ACCELERATION_MAX * deltaTime;

        if (Math.abs(speedDiff) > maxDelta) {
            speedDiff = Math.signum(speedDiff) * maxDelta;
        }

        sCurrentSpeed += speedDiff;
        sCurrentSpeed = Math.max(0, sCurrentSpeed);
    }

    private static void updateBearing(float deltaTime) {
        float bearingChange = (float) (sRandom.nextGaussian() * 5.0 * deltaTime);
        sCurrentBearing += bearingChange;
        sCurrentBearing = (sCurrentBearing + 360) % 360;
    }

    private static float calculateAccuracy() {
        if (!sAccuracyDegradation) return GPS_ACCURACY_OPEN_SKY;

        float baseAccuracy;

        switch (sCurrentEnvironment) {
            case OPEN_SKY:
                baseAccuracy = GPS_ACCURACY_OPEN_SKY;
                break;
            case URBAN:
                baseAccuracy = GPS_ACCURACY_URBAN;
                break;
            case INDOOR:
                baseAccuracy = GPS_ACCURACY_INDOOR;
                break;
            case UNDERGROUND:
                baseAccuracy = 200.0f;
                break;
            default:
                baseAccuracy = GPS_ACCURACY_OPEN_SKY;
        }

        return baseAccuracy * (0.8f + sRandom.nextFloat() * 0.4f);
    }

    private static void simulateTTFF() {
        try {
            Thread.sleep(Math.max(0, sFixAcquisitionTime - System.currentTimeMillis()));
        } catch (InterruptedException ignored) {}
        sHasFix = true;
    }

    public static void setMotionState(MotionState state) {
        sMotionState = state;
        sMotionStateStartTime = System.currentTimeMillis();
    }

    public static void setEnvironment(Environment env) {
        sCurrentEnvironment = env;

        if (env == Environment.UNDERGROUND || env == Environment.INDOOR) {
            sHasFix = false;
            sFixAcquisitionTime = System.currentTimeMillis() + TTFF_COLD_START_MS;
        }
    }

    public static void setLocation(double lat, double lon) {
        sCurrentLat = lat;
        sCurrentLon = lon;
    }

    public static TrajectoryState getState() {
        TrajectoryState state = new TrajectoryState();
        state.enabled = sEnabled;
        state.latitude = sCurrentLat;
        state.longitude = sCurrentLon;
        state.altitude = sCurrentAltitude;
        state.speed = sCurrentSpeed;
        state.bearing = sCurrentBearing;
        state.accuracy = sCurrentAccuracy;
        state.hasFix = sHasFix;
        state.motionState = sMotionState;
        state.environment = sCurrentEnvironment;
        return state;
    }

    public static class TrajectoryState {
        public boolean enabled;
        public double latitude;
        public double longitude;
        public double altitude;
        public float speed;
        public float bearing;
        public float accuracy;
        public boolean hasFix;
        public MotionState motionState;
        public Environment environment;
    }
}
