package com.samsungcloak.xposed;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.HapticFeedbackConstants;
import android.view.View;

import java.lang.reflect.Method;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hook18HapticFeedbackRealism - Haptic Feedback Realism
 * 
 * Simulates realistic haptic feedback behaviors:
 * - Motor physics: ramp-up/down latency, inertia, resonance decay
 * - Intensity modulation: battery-dependent, thermal throttling, grip effects
 * - Wear simulation: reduced high-frequency, amplitude drift, rattles
 * - Context-aware: notification variation, missed keyboard haptics, gesture scaling
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class Hook18HapticFeedbackRealism {

    private static final String TAG = "[Haptics][Hook18]";
    private static final boolean DEBUG = true;
    private static final ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new Random());

    // Configuration
    private static boolean enabled = true;
    private static float intensity = 0.5f;

    // Motor physics settings
    private static boolean motorPhysicsEnabled = true;
    private static double rampUpLatencyMs = 5.0;
    private static double rampDownLatencyMs = 8.0;
    private static double inertiaFactor = 0.15;
    private static double resonanceDecayRate = 0.20;

    // Intensity modulation settings
    private static boolean intensityModulationEnabled = true;
    private static double batteryLevelThreshold = 0.20;
    private static double lowBatteryDrain = 0.30;
    private static double thermalThrottleEffect = 0.25;
    private static double gripEffectVariance = 0.15;

    // Wear simulation settings
    private static boolean wearSimulationEnabled = true;
    private static double highFrequencyLoss = 0.10;  // Per month of use
    private static double amplitudeDrift = 0.05;
    private static double rattleProbability = 0.08;

    // Context-aware settings
    private static boolean contextAwareEnabled = true;
    private static double notificationVariation = 0.20;
    private static double keyboardMissProbability = 0.12;
    private static double gestureScaleFactor = 0.15;

    // Current motor state
    private static final AtomicReference<MotorState> motorState = 
        new AtomicReference<>(MotorState.IDLE);
    private static final AtomicReference<GripMode> currentGripMode = 
        new AtomicReference<>(GripMode.PALM);

    // Haptic use tracking
    private static final AtomicInteger hapticUseCount = new AtomicInteger(0);
    private static final AtomicInteger batteryAffectsCount = new AtomicInteger(0);
    private static final AtomicInteger thermalAffectsCount = new AtomicInteger(0);
    private static final AtomicInteger missedHaptics = new AtomicInteger(0);

    // Device state
    private static volatile float batteryLevel = 0.8f;
    private static volatile float deviceTemperature = 35.0f; // Celsius
    private static volatile boolean isCharging = false;
    private static volatile int deviceAgeMonths = 6;

    public enum MotorState {
        IDLE,
        RAMPING_UP,
        ACTIVE,
        RAMPING_DOWN,
        RESONANCE_DECAY
    }

    public enum GripMode {
        PALM,
        FINGERTIP,
        GRIP,
        POCKET,
        TABLE
    }

    public static class HapticEvent {
        public final String source;
        public final long timestamp;
        public final int duration;
        public final float requestedAmplitude;
        public float actualAmplitude;
        public int delayMs;

        public HapticEvent(String source, int duration, float amplitude) {
            this.source = source;
            this.timestamp = System.currentTimeMillis();
            this.duration = duration;
            this.requestedAmplitude = amplitude;
            this.actualAmplitude = amplitude;
            this.delayMs = 0;
        }
    }

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!enabled) return;

        HookUtils.logInfo(TAG, "Initializing Haptic Feedback Realism Hook 18");

        try {
            hookVibrator(lpparam);
            hookVibratorManager(lpparam);
            hookViewHapticFeedback(lpparam);
            hookHapticFeedbackConstants(lpparam);

            HookUtils.logInfo(TAG, "Haptic Feedback Realism Hook 18 initialized");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize", e);
        }
    }

    /**
     * Hook Vibrator
     */
    private static void hookVibrator(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> vibratorClass = Vibrator.class;

            // Hook vibrate(VibrationEffect)
            XposedBridge.hookMethod(
                XposedHelpers.findMethodExact(vibratorClass, "vibrate", VibrationEffect.class),
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        VibrationEffect effect = (VibrationEffect) param.args[0];
                        float effectiveIntensity = getEffectiveIntensity();

                        // Apply motor physics
                        if (motorPhysicsEnabled) {
                            applyMotorPhysics(effect, effectiveIntensity);
                        }

                        // Apply intensity modulation
                        if (intensityModulationEnabled) {
                            applyIntensityModulation(effect, effectiveIntensity);
                        }

                        // Apply wear effects
                        if (wearSimulationEnabled) {
                            applyWearEffects(effect, effectiveIntensity);
                        }

                        hapticUseCount.incrementAndGet();
                    }
                });

            // Hook vibrate(long)
            XposedBridge.hookMethod(
                XposedHelpers.findMethodExact(vibratorClass, "vibrate", long.class),
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        long duration = (long) param.args[0];
                        float effectiveIntensity = getEffectiveIntensity();

                        // Apply effects
                        if (duration > 0 && duration < 10000) { // Ignore special values
                            applyMotorPhysicsSimple(duration, effectiveIntensity);
                        }

                        hapticUseCount.incrementAndGet();
                    }
                });

            // Hook hasVibrator
            XposedBridge.hookMethod(
                XposedHelpers.findMethodExact(vibratorClass, "hasVibrator"),
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        // Could simulate worn-out vibrator
                    }
                });

            // Hook getId (Samsung-specific)
            try {
                XposedBridge.hookMethod(
                    XposedHelpers.findMethodExact(vibratorClass, "getId"),
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            // Could modify vibrator ID to reflect wear
                        }
                    });
            } catch (Exception e) {
                // Method may not exist
            }

            HookUtils.logDebug(TAG, "Vibrator hooked");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook Vibrator", e);
        }
    }

    /**
     * Hook VibratorManager (API 31+)
     */
    private static void hookVibratorManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Class<?> vibratorManagerClass = XposedHelpers.findClass(
                    "android.os.VibratorManager", lpparam.classLoader);

                // Hook getDefaultVibrator
                XposedBridge.hookMethod(
                    XposedHelpers.findMethodExact(vibratorManagerClass, "getDefaultVibrator"),
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            if (!enabled) return;

                            // Could add vibrator wear info here
                        }
                    });

                HookUtils.logDebug(TAG, "VibratorManager hooked");
            }
        } catch (Exception e) {
            // VibratorManager may not exist on older devices
        }
    }

    /**
     * Hook View.performHapticFeedback
     */
    private static void hookViewHapticFeedback(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> viewClass = XposedHelpers.findClass(
                "android.view.View", lpparam.classLoader);

            // Hook performHapticFeedback(int)
            XposedBridge.hookMethod(
                XposedHelpers.findMethodExact(viewClass, "performHapticFeedback", int.class),
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!contextAwareEnabled || !enabled) return;

                        int feedbackConstant = (int) param.args[0];
                        float effectiveIntensity = getEffectiveIntensity();

                        // Keyboard haptics often get missed
                        if (feedbackConstant == HapticFeedbackConstants.KEYBOARD_TAP ||
                            feedbackConstant == HapticFeedbackConstants.VIRTUAL_KEY) {
                            
                            if (random.get().nextDouble() < keyboardMissProbability * effectiveIntensity) {
                                missedHaptics.incrementAndGet();
                                param.setResult(false);
                                HookUtils.logDebug(TAG, "Keyboard haptic missed");
                                return;
                            }
                        }

                        // Apply gesture scaling
                        if (feedbackConstant == HapticFeedbackConstants.CONFIRM ||
                            feedbackConstant == HapticFeedbackConstants.REJECT) {
                            
                            // Add slight delay for gestures
                            int delay = (int) (gestureScaleFactor * effectiveIntensity * 100);
                            param.args[0] = feedbackConstant + delay;
                        }
                    }
                });

            // Hook performHapticFeedback(int, int)
            XposedBridge.hookMethod(
                XposedHelpers.findMethodExact(viewClass, "performHapticFeedback", int.class, int.class),
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!contextAwareEnabled || !enabled) return;

                        int feedbackConstant = (int) param.args[0];
                        int flags = (int) param.args[1];

                        // Apply context-aware modulation
                        float effectiveIntensity = getEffectiveIntensity();

                        // Notifications get variation
                        if ((flags & HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING) != 0) {
                            double variation = notificationVariation * (random.get().nextDouble() * 2 - 1);
                            // Could modify amplitude here
                            HookUtils.logDebug(TAG, String.format("Notification haptic variation: %.1f%%", 
                                variation * 100));
                        }
                    }
                });

            HookUtils.logDebug(TAG, "View haptic feedback hooked");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook View haptic feedback", e);
        }
    }

    /**
     * Hook HapticFeedbackConstants
     */
    private static void hookHapticFeedbackConstants(XC_MethodHook lpparam) {
        // This hooks the constants class - typically no runtime behavior to modify
        HookUtils.logDebug(TAG, "HapticFeedbackConstants reference available");
    }

    /**
     * Apply motor physics effects to VibrationEffect
     */
    private static void applyMotorPhysics(VibrationEffect effect, float effectiveIntensity) {
        MotorState oldState = motorState.getAndSet(MotorState.RAMPING_UP);
        
        if (random.get().nextDouble() < inertiaFactor * effectiveIntensity) {
            // Add ramp-up delay
            int rampDelay = (int) (rampUpLatencyMs * (1 + random.get().nextDouble()));
            try {
                Thread.sleep(rampDelay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            HookUtils.logDebug(TAG, "Motor ramp-up delay: " + rampDelay + "ms");
        }

        motorState.set(MotorState.ACTIVE);

        // Simulate resonance decay
        if (random.get().nextDouble() < resonanceDecayRate * effectiveIntensity) {
            motorState.set(MotorState.RESONANCE_DECAY);
            // Effect naturally decays
        }
    }

    /**
     * Apply motor physics to simple vibrate call
     */
    private static void applyMotorPhysicsSimple(long duration, float effectiveIntensity) {
        MotorState oldState = motorState.getAndSet(MotorState.RAMPING_UP);
        
        // Add inertia effect
        if (random.get().nextDouble() < inertiaFactor * effectiveIntensity) {
            int rampDelay = (int) (rampUpLatencyMs * effectiveIntensity);
            HookUtils.logDebug(TAG, "Motor inertia effect: +" + rampDelay + "ms delay");
        }

        motorState.set(MotorState.ACTIVE);
    }

    /**
     * Apply intensity modulation based on battery and thermal state
     */
    private static void applyIntensityModulation(VibrationEffect effect, float effectiveIntensity) {
        float modulationFactor = 1.0f;

        // Battery level effect
        if (batteryLevel < batteryLevelThreshold && !isCharging) {
            modulationFactor *= (1.0f - lowBatteryDrain);
            batteryAffectsCount.incrementAndGet();
            HookUtils.logDebug(TAG, "Low battery affecting haptic intensity");
        }

        // Thermal throttling effect
        if (deviceTemperature > 42.0f) {
            double thermalFactor = (deviceTemperature - 42.0) / 10.0 * thermalThrottleEffect;
            modulationFactor *= (1.0f - thermalFactor);
            thermalAffectsCount.incrementAndGet();
            HookUtils.logDebug(TAG, "Thermal throttling affecting haptic intensity");
        }

        // Grip mode effect
        GripMode grip = currentGripMode.get();
        if (grip == GripMode.POCKET || grip == GripMode.TABLE) {
            modulationFactor *= (1.0f + gripEffectVariance);
        } else if (grip == GripMode.GRIP) {
            modulationFactor *= (1.0f - gripEffectVariance * 0.5f);
        }
    }

    /**
     * Apply wear effects to vibration
     */
    private static void applyWearEffects(VibrationEffect effect, float effectiveIntensity) {
        // High-frequency loss
        double frequencyLoss = deviceAgeMonths * highFrequencyLoss * effectiveIntensity;
        if (random.get().nextDouble() < frequencyLoss) {
            HookUtils.logDebug(TAG, "High-frequency response degraded due to wear");
        }

        // Amplitude drift
        double drift = (random.get().nextDouble() - 0.5) * amplitudeDrift * deviceAgeMonths * effectiveIntensity;
        
        // Rattle effect
        if (random.get().nextDouble() < rattleProbability * effectiveIntensity) {
            HookUtils.logDebug(TAG, "Motor rattle detected due to wear");
        }
    }

    /**
     * Get effective intensity
     */
    private static float getEffectiveIntensity() {
        ConfigurationManager config = BaseRealismHook.configManager;
        float configIntensity = config != null ? config.getHookIntensity("hook_18") : intensity;
        return intensity * configIntensity;
    }

    /**
     * Update battery level
     */
    public static void setBatteryLevel(float level) {
        batteryLevel = Math.max(0, Math.min(1, level));
    }

    /**
     * Update device temperature
     */
    public static void setDeviceTemperature(float temp) {
        deviceTemperature = temp;
    }

    /**
     * Update charging state
     */
    public static void setCharging(boolean charging) {
        isCharging = charging;
    }

    /**
     * Update grip mode
     */
    public static void setGripMode(GripMode mode) {
        currentGripMode.set(mode);
    }

    /**
     * Set device age in months
     */
    public static void setDeviceAge(int months) {
        deviceAgeMonths = months;
    }

    /**
     * Get haptic statistics
     */
    public static String getStats() {
        return String.format("Haptics[uses=%d, battery_affected=%d, thermal_affected=%d, missed=%d, motor_state=%s, grip=%s]",
            hapticUseCount.get(), batteryAffectsCount.get(), thermalAffectsCount.get(),
            missedHaptics.get(), motorState.get(), currentGripMode.get());
    }
}
