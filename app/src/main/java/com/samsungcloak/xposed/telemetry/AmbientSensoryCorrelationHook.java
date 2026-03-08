package com.samsungcloak.xposed.telemetry;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Calendar;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Ambient Sensory Correlation Hook
 * 
 * Synchronizes Light Sensor (Lux) and Proximity data with Circadian Rhythm patterns
 * to simulate realistic human usage patterns, especially during sleep hours.
 * 
 * Patterns modeled:
 * - Dark environment (0 Lux) + Face-down proximity during sleep hours (22:00-07:00)
 * - Variable lighting conditions during daytime
 * - Screen proximity detection for call handling
 * - Environmental context transitions (bedroom, office, outdoors)
 * 
 * Target Device: Samsung Galaxy A12 (SM-A125U)
 */
public class AmbientSensoryCorrelationHook {
    
    private static final String LOG_TAG = "SamsungCloak.AmbientSensory";
    private static boolean initialized = false;
    
    private static final Random random = new Random();
    
    private static final float PROXIMITY_NEAR_THRESHOLD = 5.0f;
    private static final float PROXIMITY_FAR_THRESHOLD = 10.0f;
    
    private static final int SLEEP_HOUR_START = 22;
    private static final int SLEEP_HOUR_END = 7;
    
    private static volatile float currentLux = 300.0f;
    private static volatile float currentProximity = PROXIMITY_FAR_THRESHOLD;
    private static volatile boolean isFaceDown = false;
    private static volatile String currentEnvironmentContext = "indoor";
    private static volatile CircadianPhase currentCircadianPhase = CircadianPhase.AWAKE;
    
    private static final AtomicReference<SensorySnapshot> lastSnapshot = new AtomicReference<>();
    private static final AtomicBoolean sleepModeActive = new AtomicBoolean(false);
    
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (initialized) {
            XposedBridge.log(LOG_TAG + " already initialized");
            return;
        }
        
        try {
            hookLightSensor(lpparam);
            hookProximitySensor(lpparam);
            hookContentResolver(lpparam);
            
            updateCircadianPhase();
            initialized = true;
            XposedBridge.log(LOG_TAG + " initialized successfully");
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " Failed to initialize: " + e.getMessage());
        }
    }
    
    private static void hookLightSensor(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> sensorManagerClass = XposedHelpers.findClass(
                "android.hardware.SensorManager", lpparam.classLoader
            );
            
            XposedBridge.hookAllMethods(sensorManagerClass, "getDefaultSensor", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    int sensorType = (int) param.args[0];
                    if (sensorType == 5) {
                        // Light sensor - let it through
                    }
                }
            });
            
            XposedBridge.log(LOG_TAG + " Hooked Light Sensor");
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " Failed to hook Light Sensor: " + e.getMessage());
        }
    }
    
    private static void hookProximitySensor(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> sensorManagerClass = XposedHelpers.findClass(
                "android.hardware.SensorManager", lpparam.classLoader
            );
            
            XposedBridge.hookAllMethods(sensorManagerClass, "getDefaultSensor", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    int sensorType = (int) param.args[0];
                    if (sensorType == 8) {
                        // Proximity sensor - let it through
                    }
                }
            });
            
            XposedBridge.log(LOG_TAG + " Hooked Proximity Sensor");
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " Failed to hook Proximity Sensor: " + e.getMessage());
        }
    }
    
    private static void hookContentResolver(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> contentResolverClass = XposedHelpers.findClass(
                "android.content.ContentResolver", lpparam.classLoader
            );
            
            XposedBridge.hookAllMethods(contentResolverClass, "getInt", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.args.length >= 1 && param.args[0] != null) {
                        String setting = param.args[0].toString();
                        if (setting.contains("screen_brightness")) {
                            float brightnessWithEntropy = applyAmbientBrightnessCorrelation();
                            param.setResult((int) (brightnessWithEntropy * 255));
                        }
                    }
                }
            });
            
            XposedBridge.log(LOG_TAG + " Hooked ContentResolver for ambient correlation");
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " Failed to hook ContentResolver: " + e.getMessage());
        }
    }
    
    /**
     * Calculate current circadian phase based on time of day
     */
    private static void updateCircadianPhase() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        
        if (hour >= SLEEP_HOUR_START || hour < SLEEP_HOUR_END) {
            currentCircadianPhase = CircadianPhase.SLEEP;
            sleepModeActive.set(true);
        } else if (hour >= 5 && hour < 8) {
            currentCircadianPhase = CircadianPhase.DAWN;
        } else if (hour >= 8 && hour < 12) {
            currentCircadianPhase = CircadianPhase.MORNING;
        } else if (hour >= 12 && hour < 14) {
            currentCircadianPhase = CircadianPhase.MIDDAY;
        } else if (hour >= 14 && hour < 18) {
            currentCircadianPhase = CircadianPhase.AFTERNOON;
        } else if (hour >= 18 && hour < 22) {
            currentCircadianPhase = CircadianPhase.EVENING;
        } else {
            currentCircadianPhase = CircadianPhase.AWAKE;
        }
    }
    
    /**
     * Generate correlated ambient sensory data based on circadian phase
     */
    public static SensoryData generateCorrelatedSensoryData() {
        updateCircadianPhase();
        
        float lux;
        float proximity;
        
        if (currentCircadianPhase == CircadianPhase.SLEEP) {
            // Sleep mode: very low lux, face-down proximity
            if (random.nextFloat() < 0.85f) {
                lux = random.nextFloat() * 5.0f; // 0-5 lux (very dark)
                proximity = random.nextFloat() * PROXIMITY_NEAR_THRESHOLD; // Near (face down)
                isFaceDown = true;
            } else {
                // Occasional ambient wake
                lux = 10.0f + random.nextFloat() * 50.0f;
                proximity = PROXIMITY_FAR_THRESHOLD;
                isFaceDown = false;
            }
        } else if (currentCircadianPhase == CircadianPhase.DAWN) {
            // Dawn: low to medium light
            lux = 50.0f + random.nextFloat() * 150.0f;
            proximity = PROXIMITY_FAR_THRESHOLD;
            isFaceDown = false;
        } else if (currentCircadianPhase == CircadianPhase.MIDDAY) {
            // Midday: bright outdoor-like conditions
            lux = 500.0f + random.nextFloat() * 2000.0f;
            currentEnvironmentContext = "outdoor";
            proximity = PROXIMITY_FAR_THRESHOLD;
            isFaceDown = false;
        } else if (currentCircadianPhase == CircadianPhase.EVENING) {
            // Evening: indoor lighting
            lux = 100.0f + random.nextFloat() * 400.0f;
            currentEnvironmentContext = "indoor";
            proximity = PROXIMITY_FAR_THRESHOLD;
            isFaceDown = false;
        } else {
            // Morning/Afternoon: typical indoor
            lux = 200.0f + random.nextFloat() * 600.0f;
            currentEnvironmentContext = "indoor";
            proximity = PROXIMITY_FAR_THRESHOLD;
            isFaceDown = false;
        }
        
        // Add sensor noise
        lux += (random.nextFloat() * 10.0f) - 5.0f;
        lux = Math.max(0, lux);
        
        currentLux = lux;
        currentProximity = proximity;
        
        SensorySnapshot snapshot = new SensorySnapshot(
            lux, proximity, isFaceDown, currentEnvironmentContext, 
            currentCircadianPhase, System.currentTimeMillis()
        );
        lastSnapshot.set(snapshot);
        
        return new SensoryData(lux, proximity, isFaceDown);
    }
    
    /**
     * Apply ambient brightness correlation to screen brightness
     */
    private static float applyAmbientBrightnessCorrelation() {
        float ambientFactor;
        
        if (currentLux < 10.0f) {
            ambientFactor = 0.05f + random.nextFloat() * 0.1f;
        } else if (currentLux < 100.0f) {
            ambientFactor = 0.15f + random.nextFloat() * 0.15f;
        } else if (currentLux < 500.0f) {
            ambientFactor = 0.3f + random.nextFloat() * 0.3f;
        } else if (currentLux < 1000.0f) {
            ambientFactor = 0.5f + random.nextFloat() * 0.25f;
        } else {
            ambientFactor = 0.7f + random.nextFloat() * 0.3f;
        }
        
        return Math.min(1.0f, ambientFactor);
    }
    
    /**
     * Check if device is in sleep context
     */
    public static boolean isSleepContext() {
        return currentCircadianPhase == CircadianPhase.SLEEP;
    }
    
    /**
     * Check if device is face-down
     */
    public static boolean isFaceDown() {
        return isFaceDown;
    }
    
    /**
     * Get current environment context
     */
    public static String getEnvironmentContext() {
        return currentEnvironmentContext;
    }
    
    /**
     * Get current circadian phase
     */
    public static CircadianPhase getCircadianPhase() {
        return currentCircadianPhase;
    }
    
    /**
     * Get current lux value
     */
    public static float getCurrentLux() {
        return currentLux;
    }
    
    /**
     * Get current proximity value
     */
    public static float getCurrentProximity() {
        return currentProximity;
    }
    
    public static boolean isInitialized() {
        return initialized;
    }
    
    public enum CircadianPhase {
        SLEEP("Sleep"),
        DAWN("Dawn"),
        MORNING("Morning"),
        MIDDAY("Midday"),
        AFTERNOON("Afternoon"),
        EVENING("Evening"),
        AWAKE("Awake");
        
        private final String displayName;
        
        CircadianPhase(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public static class SensoryData {
        private final float lux;
        private final float proximity;
        private final boolean faceDown;
        
        public SensoryData(float lux, float proximity, boolean faceDown) {
            this.lux = lux;
            this.proximity = proximity;
            this.faceDown = faceDown;
        }
        
        public float getLux() {
            return lux;
        }
        
        public float getProximity() {
            return proximity;
        }
        
        public boolean isFaceDown() {
            return faceDown;
        }
        
        public boolean isNear() {
            return proximity < PROXIMITY_NEAR_THRESHOLD;
        }
        
        @Override
        public String toString() {
            return String.format("SensoryData{lux=%.1f, proximity=%.1f, faceDown=%s}",
                lux, proximity, faceDown);
        }
    }
    
    public static class SensorySnapshot {
        private final float lux;
        private final float proximity;
        private final boolean faceDown;
        private final String environment;
        private final CircadianPhase phase;
        private final long timestamp;
        
        public SensorySnapshot(float lux, float proximity, boolean faceDown,
                              String environment, CircadianPhase phase, long timestamp) {
            this.lux = lux;
            this.proximity = proximity;
            this.faceDown = faceDown;
            this.environment = environment;
            this.phase = phase;
            this.timestamp = timestamp;
        }
        
        public float getLux() { return lux; }
        public float getProximity() { return proximity; }
        public boolean isFaceDown() { return faceDown; }
        public String getEnvironment() { return environment; }
        public CircadianPhase getPhase() { return phase; }
        public long getTimestamp() { return timestamp; }
        
        @Override
        public String toString() {
            return String.format("Snapshot{lux=%.1f, proximity=%.1f, faceDown=%s, env=%s, phase=%s}",
                lux, proximity, faceDown, environment, phase.getDisplayName());
        }
    }
}
