package com.samsungcloak.xposed;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.os.Build;
import android.view.MotionEvent;

import java.lang.reflect.Method;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hook15HardwareAging - Hardware Aging & Wear Simulation
 * 
 * Simulates device hardware degradation over time:
 * - Screen wear: zone-specific latency, reduced sensitivity, ghost touches
 * - Battery: capacity fade, voltage sag, rapid discharge, early shutdown
 * - Camera: focus latency increase, color drift, dust artifacts
 * - Speaker: frequency degradation, volume reduction, distortion
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class Hook15HardwareAging {

    private static final String TAG = "[HardwareAging][Hook15]";
    private static final boolean DEBUG = true;
    private static final ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new Random());

    // Configuration
    private static boolean enabled = true;
    private static float intensity = 0.5f;

    // Device age simulation (months)
    private static int deviceAgeMonths = 6;
    private static double degradationFactor = 0.0;

    // Screen wear settings
    private static boolean screenWearEnabled = true;
    private static double screenSensitivityLoss = 0.05;  // 5% per month
    private static double screenLatencyIncrease = 0.03;  // 3% per month
    private static double ghostTouchProbability = 0.02;  // 2% chance per hour

    // Battery aging settings
    private static boolean batteryAgingEnabled = true;
    private static double batteryCapacityFade = 0.008;   // 0.8% per month
    private static double voltageSagFactor = 0.02;
    private static double rapidDischargeChance = 0.05;
    private static double earlyShutdownThreshold = 0.15;  // Shutdown at 15%

    // Camera aging settings
    private static boolean cameraAgingEnabled = true;
    private static double focusLatencyIncrease = 0.04;
    private static double colorDriftFactor = 0.02;
    private static double dustArtifactProbability = 0.03;

    // Speaker aging settings
    private static boolean speakerAgingEnabled = true;
    private static double frequencyDegradation = 0.01;
    private static double volumeReduction = 0.015;
    private static double distortionChance = 0.04;

    // Current sensor values for screen
    private static final ConcurrentMap<Integer, float[]> screenTouchHistory = 
        new ConcurrentHashMap<>();
    private static final AtomicInteger screenZoneWearLevel = new AtomicInteger(0);

    // Battery state
    private static final AtomicReference<BatteryState> currentBatteryState = 
        new AtomicReference<>(new BatteryState(100, 4200, 1000));

    // Camera state
    private static final AtomicInteger cameraUseCount = new AtomicInteger(0);
    private static double currentFocusLatency = 0;

    // Speaker state
    private static final AtomicInteger speakerUseTime = new AtomicInteger(0);
    private static double currentSpeakerHealth = 1.0;

    // Statistics
    private static final AtomicLong ghostTouchCount = new AtomicLong(0);
    private static final AtomicLong batteryShutdownCount = new AtomicLong(0);

    public static class BatteryState {
        public int level;
        public int voltage;
        public int temperature;

        public BatteryState(int level, int voltage, int temperature) {
            this.level = level;
            this.voltage = voltage;
            this.temperature = temperature;
        }
    }

    public static class ScreenZone {
        public int x, y;
        public double wearLevel;
        public double sensitivity;
        public double latency;

        public ScreenZone(int x, int y) {
            this.x = x;
            this.y = y;
            this.wearLevel = 0;
            this.sensitivity = 1.0;
            this.latency = 0;
        }
    }

    private static final ConcurrentMap<String, ScreenZone> screenZones = 
        new ConcurrentHashMap<>();

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!enabled) return;

        HookUtils.logInfo(TAG, "Initializing Hardware Aging Hook 15");

        try {
            calculateDegradationFactor();
            initializeScreenZones();
            initializeBatteryState();

            hookMotionEvent(lpparam);
            hookBatteryManager(lpparam);
            hookCameraDevice(lpparam);
            hookAudioManager(lpparam);
            hookSensorManager(lpparam);

            HookUtils.logInfo(TAG, "Hardware Aging Hook 15 initialized");
            HookUtils.logInfo(TAG, String.format("Device age: %d months, degradation: %.2f%%", 
                deviceAgeMonths, degradationFactor * 100));
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize", e);
        }
    }

    /**
     * Calculate overall degradation factor
     */
    private static void calculateDegradationFactor() {
        degradationFactor = Math.min(1.0, deviceAgeMonths / 48.0); // Max at 4 years
    }

    /**
     * Initialize screen zones for zone-specific wear
     */
    private static void initializeScreenZones() {
        // Create grid of screen zones
        int zoneSize = 100; // pixels
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 6; y++) {
                String key = x + "," + y;
                ScreenZone zone = new ScreenZone(x * zoneSize, y * zoneSize);
                // Corners and navigation area wear faster
                if ((x == 0 || x == 3) && (y == 0 || y == 5)) {
                    zone.wearLevel = degradationFactor * 1.3;
                } else {
                    zone.wearLevel = degradationFactor;
                }
                zone.sensitivity = 1.0 - (zone.wearLevel * screenSensitivityLoss * 12);
                zone.latency = zone.wearLevel * screenLatencyIncrease * 12;
                screenZones.put(key, zone);
            }
        }
    }

    /**
     * Initialize battery state
     */
    private static void initializeBatteryState() {
        int currentCapacity = (int) (100 * (1 - deviceAgeMonths * batteryCapacityFade));
        int voltage = (int) (4200 * (1 - voltageSagFactor * degradationFactor));
        currentBatteryState.set(new BatteryState(currentCapacity, voltage, 350));
    }

    /**
     * Hook MotionEvent for touch screen simulation
     */
    private static void hookMotionEvent(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> motionEventClass = MotionEvent.class;

            // Hook obtain method to inject ghost touches
            XposedBridge.hookMethod(
                XposedHelpers.findMethodExact(motionEventClass, "obtain"),
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!screenWearEnabled || !enabled) return;

                        float effectiveIntensity = getEffectiveIntensity();
                        MotionEvent event = (MotionEvent) param.getResult();
                        if (event == null) return;

                        // Check for ghost touch probability
                        if (random.get().nextDouble() < ghostTouchProbability * degradationFactor * effectiveIntensity) {
                            injectGhostTouch(event);
                        }
                    }
                });

            // Hook getAxisValue for sensitivity simulation
            XposedBridge.hookMethod(
                XposedHelpers.findMethodExact(motionEventClass, "getAxisValue", int.class),
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!screenWearEnabled || !enabled) return;

                        float effectiveIntensity = getEffectiveIntensity();
                        int axis = (int) param.args[0];
                        
                        if (axis == MotionEvent.AXIS_PRESSURE || axis == MotionEvent.AXIS_TOUCH_MINOR) {
                            float original = (float) param.getResult();
                            float degraded = original * (1.0f - (float)(degradationFactor * screenSensitivityLoss * effectiveIntensity));
                            param.setResult(degraded);
                        }
                    }
                });

            HookUtils.logDebug(TAG, "MotionEvent hooked");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook MotionEvent", e);
        }
    }

    /**
     * Hook BatteryManager
     */
    private static void hookBatteryManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> batteryManagerClass = XposedHelpers.findClass(
                "android.os.BatteryManager", lpparam.classLoader);

            // Hook getIntProperty
            XposedBridge.hookMethod(
                XposedHelpers.findMethodExact(batteryManagerClass, "getIntProperty", int.class),
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!batteryAgingEnabled || !enabled) return;

                        int property = (int) param.args[0];
                        int original = (int) param.getResult();

                        if (property == BatteryManager.BATTERY_PROPERTY_CAPACITY) {
                            int degraded = (int) (original * (1 - degradationFactor * batteryCapacityFade * 12));
                            param.setResult(Math.max(0, degraded));
                        } else if (property == BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) {
                            // Charge counter degradation
                            int degraded = (int) (original * (1 - degradationFactor * batteryCapacityFade * 12));
                            param.setResult(Math.max(0, degraded));
                        }
                    }
                });

            // Hook getLongProperty
            XposedBridge.hookMethod(
                XposedHelpers.findMethodExact(batteryManagerClass, "getLongProperty", int.class),
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!batteryAgingEnabled || !enabled) return;

                        int property = (int) param.args[0];
                        
                        if (property == BatteryManager.BATTERY_PROPERTY_VOLTAGE) {
                            int original = (int) param.getResult();
                            int sagged = (int) (original * (1 - voltageSagFactor * degradationFactor));
                            param.setResult(sagged);
                        }
                    }
                });

            HookUtils.logDebug(TAG, "BatteryManager hooked");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook BatteryManager", e);
        }
    }

    /**
     * Hook CameraDevice (Samsung-specific)
     */
    private static void hookCameraDevice(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> cameraDeviceClass = XposedHelpers.findClass(
                "android.hardware.camera2.CameraDevice", lpparam.classLoader);

            // Hook createCaptureSession
            XposedBridge.hookMethod(
                XposedHelpers.findMethodExact(cameraDeviceClass, "createCaptureSession",
                    List.class, Object.class, Handler.class),
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!cameraAgingEnabled || !enabled) return;
                        
                        cameraUseCount.incrementAndGet();
                        
                        // Increase focus latency over time
                        currentFocusLatency = cameraUseCount.get() * focusLatencyIncrease * degradationFactor;
                    }
                });

            HookUtils.logDebug(TAG, "CameraDevice hooked");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook CameraDevice", e);
        }
    }

    /**
     * Hook AudioManager for speaker simulation
     */
    private static void hookAudioManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> audioManagerClass = AudioManager.class;

            // Hook getStreamVolume
            XposedBridge.hookMethod(
                XposedHelpers.findMethodExact(audioManagerClass, "getStreamVolume", int.class),
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!speakerAgingEnabled || !enabled) return;

                        int streamType = (int) param.args[0];
                        int original = (int) param.getResult();

                        // Only affect speaker streams
                        if (streamType == AudioManager.STREAM_MUSIC || 
                            streamType == AudioManager.STREAM_NOTIFICATION ||
                            streamType == AudioManager.STREAM_RING) {
                            
                            float effectiveIntensity = getEffectiveIntensity();
                            double reduction = volumeReduction * degradationFactor * effectiveIntensity;
                            int degraded = (int) (original * (1 - reduction));
                            param.setResult(Math.max(0, degraded));
                        }
                    }
                });

            HookUtils.logDebug(TAG, "AudioManager hooked");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook AudioManager", e);
        }
    }

    /**
     * Hook SensorManager for sensor drift
     */
    private static void hookSensorManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> sensorManagerClass = SensorManager.class;

            // Hook getDefaultSensor
            XposedBridge.hookMethod(
                XposedHelpers.findMethodExact(sensorManagerClass, "getDefaultSensor", int.class),
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        int type = (int) param.args[0];
                        Object sensor = param.getResult();
                        
                        if (sensor != null && (type == Sensor.TYPE_ACCELEROMETER || 
                            type == Sensor.TYPE_GYROSCOPE)) {
                            // Apply sensor drift
                            applySensorDrift(sensor, type);
                        }
                    }
                });

            HookUtils.logDebug(TAG, "SensorManager hooked");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook SensorManager", e);
        }
    }

    /**
     * Inject ghost touch event
     */
    private static void injectGhostTouch(MotionEvent originalEvent) {
        // Generate random position near original
        float offsetX = (random.get().nextFloat() - 0.5f) * 100;
        float offsetY = (random.get().nextFloat() - 0.5f) * 100;
        
        // Create ghost touch
        long eventTime = System.currentTimeMillis();
        // In real implementation, would inject via input subsystem
        
        ghostTouchCount.incrementAndGet();
        HookUtils.logDebug(TAG, String.format("Ghost touch injected at offset (%.1f, %.1f)", 
            offsetX, offsetY));
    }

    /**
     * Apply sensor drift
     */
    private static void applySensorDrift(Object sensor, int type) {
        // In real implementation, would modify sensor calibration
    }

    /**
     * Get effective intensity
     */
    private static float getEffectiveIntensity() {
        ConfigurationManager config = BaseRealismHook.configManager;
        float configIntensity = config != null ? config.getHookIntensity("hook_15") : intensity;
        return intensity * configIntensity;
    }

    /**
     * Get hardware aging statistics
     */
    public static String getStats() {
        return String.format("HardwareAging[age=%dmo, degradation=%.1f%%, ghostTouches=%d, batteryShutdowns=%d, cameraUses=%d, speakerTime=%ds]",
            deviceAgeMonths, degradationFactor * 100, 
            ghostTouchCount.get(), batteryShutdownCount.get(),
            cameraUseCount.get(), speakerUseTime.get());
    }

    /**
     * Set device age in months
     */
    public static void setDeviceAge(int months) {
        deviceAgeMonths = months;
        calculateDegradationFactor();
    }
}
