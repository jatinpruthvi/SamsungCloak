package com.samsungcloak.xposed;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.Settings;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.CellInfoWcdma;
import android.telephony.TelephonyManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.List;
import java.util.Random;

/**
 * AmbientEnvironmentHook - Environmental Context Simulation
 *
 * Simulates realistic environmental factors that affect device behavior and sensor readings,
 * including humidity effects on touch, altitude effects on sensors, ambient noise levels,
 * and location-based environmental context.
 *
 * Novel Dimensions:
 * 1. Humidity Effects - High humidity affects touch capacitance detection
 * 2. Altitude Effects - Barometric pressure affects weather/travel apps
 * 3. Ambient Noise - Microphone background noise simulation
 * 4. Location Context - Indoor vs outdoor, urban vs rural simulation
 * 5. Weather Correlation - Weather conditions affecting sensor readings
 * 6. Electromagnetic Interference - EMI from environment affecting sensors
 *
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class AmbientEnvironmentHook {

    private static final String TAG = "[Environment][Ambient]";
    private static final boolean DEBUG = true;
    private static final ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new Random());

    private static boolean enabled = true;

    // Humidity configuration
    private static boolean humidityEffectsEnabled = true;
    private static double currentHumidityPercent = 55.0; // 55% relative humidity
    private static double highHumidityThreshold = 75.0;

    // Altitude configuration
    private static boolean altitudeEffectsEnabled = true;
    private static double currentAltitudeMeters = 50.0; // Sea level + 50m
    private static double pressureBaseline = 1013.25; // hPa at sea level

    // Ambient noise configuration
    private static boolean ambientNoiseEnabled = true;
    private static double ambientNoiseLeveldB = 45.0; // Quiet office
    private static EnvironmentType currentEnvironmentType = EnvironmentType.INDOOR_OFFICE;

    // Location context
    private static boolean locationContextEnabled = true;
    private static LocationContext currentLocationContext = LocationContext.INDOOR_URBAN;

    // Weather conditions
    private static boolean weatherCorrelationEnabled = true;
    private static WeatherCondition currentWeather = WeatherCondition.CLOUDY;

    // EMI configuration
    private static boolean emiEffectsEnabled = true;
    private static double emiLevel = 0.1; // 0.0 - 1.0

    // Environmental transition
    private static long lastEnvironmentChangeTime = 0;
    private static long environmentChangeIntervalMs = 60000; // Check every minute

    // Sensor adjustments
    private static boolean sensorAdjustmentsEnabled = true;
    private static final double[] touchNoiseFactors = {1.0, 1.0}; // X, Y

    public enum EnvironmentType {
        INDOOR_OFFICE,
        INDOOR_HOME,
        INDOOR_RETAIL,
        OUTDOOR_URBAN,
        OUTDOOR_SUBURBAN,
        OUTDOOR_RURAL,
        PUBLIC_TRANSIT,
        VEHICLE,
        UNDERGROUND
    }

    public enum LocationContext {
        INDOOR_URBAN,
        INDOOR_SUBURBAN,
        OUTDOOR_URBAN,
        OUTDOOR_SUBURBAN,
        INDOOR_RURAL,
        OUTDOOR_RURAL,
        UNDERGROUND,
        IN_VEHICLE
    }

    public enum WeatherCondition {
        CLEAR,
        PARTLY_CLOUDY,
        CLOUDY,
        RAIN,
        HEAVY_RAIN,
        SNOW,
        FOG,
        STORM
    }

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        HookUtils.logInfo(TAG, "Initializing Ambient Environment Hook");

        try {
            determineInitialEnvironment();
            
            hookSensorManager(lpparam);
            hookTelephonyManager(lpparam);
            hookLocationManager(lpparam);
            hookConnectivityManager(lpparam);
            
            startEnvironmentSimulationThread();

            HookUtils.logInfo(TAG, "Ambient Environment Hook initialized successfully");
            HookUtils.logInfo(TAG, String.format("Environment: %s, Location: %s, Weather: %s, Humidity: %.0f%%", 
                currentEnvironmentType.name(), currentLocationContext.name(), 
                currentWeather.name(), currentHumidityPercent));
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize hook", e);
        }
    }

    private static void determineInitialEnvironment() {
        // Determine environment based on time of day and day of week
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        
        // Morning: likely indoor office or commute
        if (hour >= 8 && hour <= 10) {
            currentEnvironmentType = EnvironmentType.PUBLIC_TRANSIT;
            currentLocationContext = LocationContext.IN_VEHICLE;
            ambientNoiseLeveldB = 65.0;
        }
        // Work hours: indoor office
        else if (hour >= 10 && hour <= 17) {
            currentEnvironmentType = EnvironmentType.INDOOR_OFFICE;
            currentLocationContext = LocationContext.INDOOR_URBAN;
            ambientNoiseLeveldB = 45.0;
        }
        // Evening: home or outdoor
        else if (hour >= 17 && hour <= 21) {
            currentEnvironmentType = EnvironmentType.INDOOR_HOME;
            currentLocationContext = LocationContext.INDOOR_SUBURBAN;
            ambientNoiseLeveldB = 35.0;
        }
        // Night: home
        else {
            currentEnvironmentType = EnvironmentType.INDOOR_HOME;
            currentLocationContext = LocationContext.INDOOR_SUBURBAN;
            ambientNoiseLeveldB = 25.0;
        }

        // Determine weather
        double weatherRand = random.get().nextDouble();
        if (weatherRand < 0.4) {
            currentWeather = WeatherCondition.CLEAR;
        } else if (weatherRand < 0.7) {
            currentWeather = WeatherCondition.PARTLY_CLOUDY;
        } else if (weatherRand < 0.85) {
            currentWeather = WeatherCondition.CLOUDY;
        } else if (weatherRand < 0.95) {
            currentWeather = WeatherCondition.RAIN;
        } else {
            currentWeather = WeatherCondition.STORM;
        }

        // Determine altitude based on location context
        switch (currentLocationContext) {
            case OUTDOOR_URBAN:
                currentAltitudeMeters = 20 + random.get().nextDouble() * 50;
                break;
            case OUTDOOR_RURAL:
                currentAltitudeMeters = 100 + random.get().nextDouble() * 400;
                break;
            default:
                currentAltitudeMeters = 10 + random.get().nextDouble() * 100;
                break;
        }

        // Calculate baseline pressure
        updateBarometricPressure();
    }

    private static void hookSensorManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> sensorEventQueueClass = XposedHelpers.findClass(
                "android.hardware.SystemSensorManager$SensorEventQueue",
                lpparam.classLoader
            );

            XposedBridge.hookAllMethods(sensorEventQueueClass, "dispatchSensorEvent", 
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !sensorAdjustmentsEnabled) return;

                        try {
                            int handle = (int) param.args[0];
                            float[] values = (float[]) param.args[1];
                            
                            if (values == null || values.length == 0) return;

                            int sensorType = mapHandleToSensorType(handle);
                            
                            switch (sensorType) {
                                case Sensor.TYPE_LIGHT:
                                    applyAmbientLightAdjustment(values);
                                    break;
                                case Sensor.TYPE_PRESSURE:
                                    applyBarometricPressureAdjustment(values);
                                    break;
                                case Sensor.TYPE_PROXIMITY:
                                    applyProximityNoise(values);
                                    break;
                                case Sensor.TYPE_ACCELEROMETER:
                                    applyEMIeffects(values);
                                    break;
                            }
                        } catch (Exception e) {
                            if (DEBUG) HookUtils.logDebug(TAG, "Error in sensor hook: " + e.getMessage());
                        }
                    }
                });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked SensorEventQueue");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook SensorEventQueue", e);
        }
    }

    private static void hookTelephonyManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> telephonyManagerClass = XposedHelpers.findClass(
                "android.telephony.TelephonyManager",
                lpparam.classLoader
            );

            XposedBridge.hookAllMethods(telephonyManagerClass, "getCellLocation", 
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !locationContextEnabled) return;

                        try {
                            // Add location noise based on environment
                            Object cellLocation = param.getResult();
                            if (cellLocation != null) {
                                applyCellLocationNoise(cellLocation);
                            }
                        } catch (Exception e) {
                            if (DEBUG) HookUtils.logDebug(TAG, "Error in cell location hook: " + e.getMessage());
                        }
                    }
                });

            XposedBridge.hookAllMethods(telephonyManagerClass, "getNetworkOperatorName", 
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        // Could modify operator name based on virtual location
                    }
                });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked TelephonyManager");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook TelephonyManager", e);
        }
    }

    private static void hookLocationManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> locationManagerClass = XposedHelpers.findClass(
                "android.location.LocationManager",
                lpparam.classLoader
            );

            XposedBridge.hookAllMethods(locationManagerClass, "getLastKnownLocation", 
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !locationContextEnabled) return;

                        try {
                            Location location = (Location) param.getResult();
                            if (location != null) {
                                applyLocationNoise(location);
                            }
                        } catch (Exception e) {
                            if (DEBUG) HookUtils.logDebug(TAG, "Error in location hook: " + e.getMessage());
                        }
                    }
                });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked LocationManager");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook LocationManager", e);
        }
    }

    private static void hookConnectivityManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> connectivityManagerClass = XposedHelpers.findClass(
                "android.net.ConnectivityManager",
                lpparam.classLoader
            );

            XposedBridge.hookAllMethods(connectivityManagerClass, "getActiveNetworkInfo", 
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        try {
                            NetworkInfo networkInfo = (NetworkInfo) param.getResult();
                            if (networkInfo != null) {
                                // Could add network type noise based on environment
                            }
                        } catch (Exception e) {
                            if (DEBUG) HookUtils.logDebug(TAG, "Error in connectivity hook: " + e.getMessage());
                        }
                    }
                });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked ConnectivityManager");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook ConnectivityManager", e);
        }
    }

    private static int mapHandleToSensorType(int handle) {
        switch (handle) {
            case 0: return Sensor.TYPE_ACCELEROMETER;
            case 1: return Sensor.TYPE_GYROSCOPE;
            case 2: return Sensor.TYPE_MAGNETIC_FIELD;
            case 3: return Sensor.TYPE_LIGHT;
            case 4: return Sensor.TYPE_PRESSURE;
            case 5: return Sensor.TYPE_PROXIMITY;
            default: return -1;
        }
    }

    /**
     * 1. HUMIDITY EFFECTS ON TOUCH
     *
     * High humidity can affect touch screen sensitivity and cause
     * spurious touch events
     */
    private static void applyHumidityEffects(float[] touchCoords) {
        if (!humidityEffectsEnabled) return;

        if (currentHumidityPercent > highHumidityThreshold) {
            double excessHumidity = (currentHumidityPercent - highHumidityThreshold) / 25.0;
            
            // Add noise to touch coordinates
            double noiseX = random.get().nextGaussian() * excessHumidity * 2.0;
            double noiseY = random.get().nextGaussian() * excessHumidity * 2.0;
            
            touchCoords[0] = (float) (touchCoords[0] + noiseX);
            touchCoords[1] = (float) (touchCoords[1] + noiseY);

            // Occasional phantom touches in high humidity
            if (random.get().nextDouble() < excessHumidity * 0.02) {
                if (DEBUG) HookUtils.logDebug(TAG, String.format(
                    "High humidity phantom touch: humidity=%.0f%%", currentHumidityPercent
                ));
            }
        }
    }

    /**
     * 2. AMBIENT LIGHT ADJUSTMENT
     */
    private static void applyAmbientLightAdjustment(float[] values) {
        if (values.length < 1) return;

        double baseLux = values[0];
        
        // Adjust based on environment type
        double environmentLux = 0;
        switch (currentEnvironmentType) {
            case INDOOR_OFFICE:
                environmentLux = 300 + random.get().nextGaussian() * 50;
                break;
            case INDOOR_HOME:
                environmentLux = 150 + random.get().nextGaussian() * 30;
                break;
            case OUTDOOR_URBAN:
                environmentLux = 5000 + random.get().nextGaussian() * 1000;
                break;
            case OUTDOOR_RURAL:
                environmentLux = 10000 + random.get().nextGaussian() * 2000;
                break;
            case UNDERGROUND:
                environmentLux = 50 + random.get().nextGaussian() * 10;
                break;
        }

        // Weather adjustment
        switch (currentWeather) {
            case CLOUDY:
                environmentLux *= 0.7;
                break;
            case RAIN:
                environmentLux *= 0.6;
                break;
            case STORM:
                environmentLux *= 0.4;
                break;
        }

        // Blend with sensor reading
        values[0] = (float) ((baseLux + environmentLux) / 2.0);
        
        // Clamp to reasonable range
        values[0] = HookUtils.clamp(values[0], 0.0f, 100000.0f);
    }

    /**
     * 3. BAROMETRIC PRESSURE ADJUSTMENT
     */
    private static void applyBarometricPressureAdjustment(float[] values) {
        if (values.length < 1) return;

        // Calculate pressure based on altitude
        double temperatureCelsius = 20.0; // Assume 20°C
        double pressure = pressureBaseline * Math.pow(1 - (currentAltitudeMeters / 44330.0), 5.255);
        
        // Add weather variation
        switch (currentWeather) {
            case HIGH_PRESSURE:
                pressure += 15;
                break;
            case LOW_PRESSURE:
                pressure -= 15;
                break;
            case STORM:
                pressure -= 25;
                break;
        }

        // Add sensor noise
        pressure += random.get().nextGaussian() * 0.5;
        
        values[0] = (float) pressure;
    }

    /**
     * 4. PROXIMITY SENSOR NOISE
     */
    private static void applyProximitySensorNoise(float[] values) {
        if (values.length < 1) return;

        // Some environments have more false proximity triggers
        switch (currentEnvironmentType) {
            case OUTDOOR_URBAN:
            case PUBLIC_TRANSIT:
                // More ambient IR can cause false proximity triggers
                if (random.get().nextDouble() < 0.05) {
                    values[0] = 0.0f; // Far
                }
                break;
        }
    }

    /**
     * 5. EMI EFFECTS ON SENSORS
     */
    private static void applyEMIeffects(float[] values) {
        if (!emiEffectsEnabled || values.length < 3) return;

        // EMI affects accelerometer more in certain environments
        double emiNoise = emiLevel * 0.1;
        
        switch (currentEnvironmentType) {
            case VEHICLE:
            case PUBLIC_TRANSIT:
                emiNoise *= 2.0;
                break;
            case INDOOR_HOME:
                emiNoise *= 0.5;
                break;
        }

        for (int i = 0; i < values.length; i++) {
            values[i] += HookUtils.generateGaussianNoise((float) emiNoise);
        }
    }

    private static void applyCellLocationNoise(Object cellLocation) {
        // Add noise to cell location based on environment
        // Urban areas have more cells, so less precision
        switch (currentLocationContext) {
            case INDOOR_URBAN:
            case OUTDOOR_URBAN:
                // High cell density - more location noise
                break;
            case OUTDOOR_RURAL:
            case INDOOR_RURAL:
                // Low cell density - less noise
                break;
        }
    }

    private static void applyLocationNoise(Location location) {
        // Add noise based on environment type
        float accuracy = location.getAccuracy();
        
        switch (currentEnvironmentType) {
            case INDOOR_URBAN:
                // Buildings cause GPS accuracy degradation
                accuracy *= 1.5;
                break;
            case OUTDOOR_RURAL:
                // Open areas have better GPS
                accuracy *= 0.8;
                break;
            case UNDERGROUND:
                // No GPS signal
                accuracy = 1000; // Very poor
                break;
        }

        // Can't directly set accuracy, but could add noise to coordinates
        if (random.get().nextDouble() < 0.1) {
            double latNoise = random.get().nextGaussian() * accuracy * 0.001;
            double lonNoise = random.get().nextGaussian() * accuracy * 0.001;
            
            location.setLatitude(location.getLatitude() + latNoise);
            location.setLongitude(location.getLongitude() + lonNoise);
        }
    }

    private static void updateBarometricPressure() {
        // Calculate expected pressure at current altitude
        pressureBaseline = 1013.25 * Math.pow(1 - (currentAltitudeMeters / 44330.0), 5.255);
    }

    private static void startEnvironmentSimulationThread() {
        Thread simulationThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(environmentChangeIntervalMs);
                    
                    if (!enabled) continue;
                    
                    simulateEnvironmentChange();
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    if (DEBUG) HookUtils.logDebug(TAG, "Simulation error: " + e.getMessage());
                }
            }
        });
        simulationThread.setName("EnvironmentSimulator");
        simulationThread.setDaemon(true);
        simulationThread.start();
    }

    private static void simulateEnvironmentChange() {
        long currentTime = System.currentTimeMillis();
        
        // Gradual humidity changes
        if (random.get().nextDouble() < 0.2) {
            double humidityChange = (random.get().nextDouble() - 0.5) * 2.0;
            currentHumidityPercent = HookUtils.clamp(currentHumidityPercent + humidityChange, 20.0, 100.0);
        }

        // Occasional environment type changes
        if (random.get().nextDouble() < 0.05) {
            EnvironmentType[] types = EnvironmentType.values();
            currentEnvironmentType = types[random.get().nextInt(types.length)];
            
            // Update related parameters
            switch (currentEnvironmentType) {
                case INDOOR_OFFICE:
                    currentLocationContext = LocationContext.INDOOR_URBAN;
                    ambientNoiseLeveldB = 45.0;
                    break;
                case INDOOR_HOME:
                    currentLocationContext = LocationContext.INDOOR_SUBURBAN;
                    ambientNoiseLeveldB = 35.0;
                    break;
                case OUTDOOR_URBAN:
                    currentLocationContext = LocationContext.OUTDOOR_URBAN;
                    ambientNoiseLeveldB = 70.0;
                    break;
                case PUBLIC_TRANSIT:
                    currentLocationContext = LocationContext.IN_VEHICLE;
                    ambientNoiseLeveldB = 65.0;
                    break;
            }
            
            lastEnvironmentChangeTime = currentTime;
            
            if (DEBUG) {
                HookUtils.logDebug(TAG, String.format(
                    "Environment changed: %s, Noise: %.0fdB", 
                    currentEnvironmentType.name(), ambientNoiseLeveldB
                ));
            }
        }

        // Weather changes
        if (random.get().nextDouble() < 0.02) {
            WeatherCondition[] conditions = WeatherCondition.values();
            currentWeather = conditions[random.get().nextInt(conditions.length)];
        }
    }

    // Configuration methods

    public static void setEnabled(boolean enabled) {
        AmbientEnvironmentHook.enabled = enabled;
        HookUtils.logInfo(TAG, "Hook " + (enabled ? "enabled" : "disabled"));
    }

    public static void setHumidity(double percent) {
        currentHumidityPercent = HookUtils.clamp(percent, 0.0, 100.0);
        HookUtils.logInfo(TAG, "Humidity set to: " + currentHumidityPercent + "%");
    }

    public static void setAltitude(double meters) {
        currentAltitudeMeters = HookUtils.clamp(meters, -500.0, 9000.0);
        updateBarometricPressure();
        HookUtils.logInfo(TAG, "Altitude set to: " + currentAltitudeMeters + "m");
    }

    public static void setAmbientNoiseLevel(double dB) {
        ambientNoiseLeveldB = HookUtils.clamp(dB, 0.0, 120.0);
        HookUtils.logInfo(TAG, "Ambient noise set to: " + ambientNoiseLeveldB + "dB");
    }

    public static void setEnvironmentType(EnvironmentType type) {
        currentEnvironmentType = type;
        
        // Update related context
        switch (type) {
            case INDOOR_OFFICE:
            case INDOOR_RETAIL:
                currentLocationContext = LocationContext.INDOOR_URBAN;
                break;
            case OUTDOOR_URBAN:
                currentLocationContext = LocationContext.OUTDOOR_URBAN;
                break;
            case VEHICLE:
            case PUBLIC_TRANSIT:
                currentLocationContext = LocationContext.IN_VEHICLE;
                break;
            case UNDERGROUND:
                currentLocationContext = LocationContext.UNDERGROUND;
                break;
        }
        
        HookUtils.logInfo(TAG, "Environment type set to: " + type.name());
    }

    public static void setLocationContext(LocationContext context) {
        currentLocationContext = context;
        HookUtils.logInfo(TAG, "Location context set to: " + context.name());
    }

    public static void setWeatherCondition(WeatherCondition condition) {
        currentWeather = condition;
        HookUtils.logInfo(TAG, "Weather condition set to: " + condition.name());
    }

    public static void setEMILevel(double level) {
        emiLevel = HookUtils.clamp(level, 0.0, 1.0);
        HookUtils.logInfo(TAG, "EMI level set to: " + emiLevel);
    }

    // Getters

    public static double getHumidity() {
        return currentHumidityPercent;
    }

    public static double getAltitude() {
        return currentAltitudeMeters;
    }

    public static double getAmbientNoiseLevel() {
        return ambientNoiseLeveldB;
    }

    public static EnvironmentType getEnvironmentType() {
        return currentEnvironmentType;
    }

    public static LocationContext getLocationContext() {
        return currentLocationContext;
    }

    public static WeatherCondition getWeatherCondition() {
        return currentWeather;
    }

    public static double getPressure() {
        return pressureBaseline;
    }

    public static double getEMILevel() {
        return emiLevel;
    }
}
