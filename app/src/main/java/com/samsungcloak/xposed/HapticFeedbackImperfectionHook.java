package com.samsungcloak.xposed;

import android.os.Vibrator;
import android.os.VibrationEffect;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;

/**
 * Hook #16: Haptic Feedback Imperfections
 * 
 * Simulates:
 * - Motor inertia (delayed start/stop)
 * - Battery-dependent intensity
 * - Hardware aging effects
 * - Context-aware vibration patterns
 * 
 * Extends existing VibrationHapticsHook with hardware aging simulation.
 * 
 * Targets: SM-A125U (Android 10/11)
 */
public class HapticFeedbackImperfectionHook {

    private static final String TAG = "[HumanInteraction][Haptic]";
    private static final boolean DEBUG = true;

    private static boolean enabled = true;
    private static float motorInertiaDelay = 0.15f;
    private static float hardwareDegradation = 0.10f;

    private static final Random random = new Random();
    
    // Hardware aging simulation
    private static float hardwareAgeFactor = 0.0f;
    private static long moduleInstallTime = 0;
    private static final long AGING_SIMULATION_PERIOD = 1000L * 60 * 60 * 24 * 30; // 30 days
    
    // Battery-dependent effects
    private static int currentBatteryLevel = 100;
    private static boolean isCharging = false;

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        HookUtils.logInfo(TAG, "Initializing Haptic Feedback Imperfection Hook");

        try {
            // Initialize aging simulation
            if (moduleInstallTime == 0) {
                moduleInstallTime = System.currentTimeMillis();
            }
            updateHardwareAging();

            hookVibrator(lpparam);
            hookVibrationEffect(lpparam);
            hookBatteryStatus(lpparam);
            
            HookUtils.logInfo(TAG, "Haptic Feedback Imperfection Hook initialized successfully");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize hook", e);
        }
    }

    private static void hookVibrator(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> vibratorClass = XposedHelpers.findClass(
                "android.os.Vibrator", lpparam.classLoader);

            // Hook vibrate(long) method
            XposedBridge.hookAllMethods(vibratorClass, "vibrate", 
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    long duration = -1;
                    if (param.args[0] instanceof Long) {
                        duration = (Long) param.args[0];
                    } else if (param.args[0] instanceof long[]) {
                        long[] pattern = (long[]) param.args[0];
                        // Calculate total pattern duration
                        duration = 0;
                        for (int i = 0; i < pattern.length; i += 2) {
                            duration += pattern[i];
                        }
                    }

                    if (duration > 0) {
                        // Apply motor inertia delay
                        applyMotorInertiaDelay(duration);

                        // Log vibration with realistic parameters
                        if (DEBUG && random.nextFloat() < 0.1f) {
                            float ageFactor = getEffectiveHardwareAge();
                            float batteryFactor = getBatteryFactor();
                            HookUtils.logDebug(TAG, "Vibration: duration=" + duration + 
                                "ms, ageFactor=" + String.format("%.2f", ageFactor) + 
                                ", batteryFactor=" + String.format("%.2f", batteryFactor));
                        }
                    }
                }
            });

            // Hook vibrate with pattern
            XposedBridge.hookAllMethods(vibratorClass, "vibrate", 
                new XC_MethodHook() {
                @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    // Check for pattern-based vibration
                    if (param.args.length >= 2 && param.args[1] instanceof Integer) {
                        // Pattern with repeat
                        long[] pattern = (long[]) param.args[0];
                        int repeat = (int) param.args[1];
                        
                        if (pattern != null && pattern.length >= 2) {
                            // Apply inertia to each pulse in pattern
                            for (int i = 0; i < pattern.length; i += 2) {
                                if (pattern[i] > 0) {
                                    applyMotorInertiaDelay(pattern[i]);
                                }
                            }
                        }
                    }
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked Vibrator methods");

        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook Vibrator", e);
        }
    }

    private static void hookVibrationEffect(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> vibrationEffectClass = XposedHelpers.findClass(
                "android.os.VibrationEffect", lpparam.classLoader);

            // Hook createOneShot to modify amplitude
            XposedBridge.hookAllMethods(vibrationEffectClass, "createOneShot",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    Object effect = param.getResult();
                    if (effect != null) {
                        applyVibrationImperfections(effect);
                    }
                }
            });

            // Hook createWaveform to modify pattern amplitudes
            XposedBridge.hookAllMethods(vibrationEffectClass, "createWaveform",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    Object effect = param.getResult();
                    if (effect != null) {
                        applyVibrationImperfections(effect);
                    }
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked VibrationEffect methods");

        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook VibrationEffect", e);
        }
    }

    private static void hookBatteryStatus(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook BatteryManager to track battery level
            Class<?> batteryManagerClass = XposedHelpers.findClass(
                "android.os.BatteryManager", lpparam.classLoader);

            XposedBridge.hookAllMethods(batteryManagerClass, "getIntProperty",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    int property = (int) param.args[0];
                    
                    // PROPERTY_CAPACITY = 1, PROPERTY_CHARGE_COUNTER = 2
                    if (property == 1) { // Battery level
                        int batteryLevel = (int) param.getResult();
                        if (batteryLevel > 0 && batteryLevel <= 100) {
                            currentBatteryLevel = batteryLevel;
                        }
                    } else if (property == 3) { // STATUS_CHARGING
                        int status = (int) param.getResult();
                        isCharging = (status == 2 || status == 5); // CHARGING or FULL
                    }
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked BatteryManager");

        } catch (Exception e) {
            // Battery hook is optional, continue without it
            if (DEBUG) {
                HookUtils.logDebug(TAG, "Could not hook BatteryManager: " + e.getMessage());
            }
        }
    }

    private static void applyMotorInertiaDelay(long duration) {
        try {
            // Motor inertia: small delay before vibration starts and after it ends
            // This simulates the physical delay of the vibration motor
            
            float inertiaDelayMs = duration * motorInertiaDelay * random.nextFloat();
            int delayMs = Math.max(1, (int) inertiaDelayMs);
            
            // Only apply delay for significant vibrations
            if (duration > 50 && delayMs > 5) {
                Thread.sleep(delayMs);
            }

            // Simulate motor ramp-up time
            if (duration > 100 && random.nextFloat() < 0.3f) {
                Thread.sleep(random.nextInt(10) + 5);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void applyVibrationImperfections(Object effect) {
        try {
            // Get original amplitude
            int originalAmplitude = XposedHelpers.getIntField(effect, "mAmplitude");
            
            if (originalAmplitude <= 0 || originalAmplitude > 255) {
                return; // No amplitude to modify
            }

            // Apply hardware degradation (aging)
            float ageFactor = getEffectiveHardwareAge();
            float degradedAmplitude = originalAmplitude * ageFactor;

            // Apply battery factor
            float batteryFactor = getBatteryFactor();
            degradedAmplitude *= batteryFactor;

            // Apply random variation (±10%)
            float variation = 0.9f + random.nextFloat() * 0.2f;
            degradedAmplitude *= variation;

            // Clamp to valid range
            int newAmplitude = Math.max(1, Math.min(255, (int) degradedAmplitude));

            // Apply changes
            XposedHelpers.setIntField(effect, "mAmplitude", newAmplitude);

            if (DEBUG && random.nextFloat() < 0.05f) {
                HookUtils.logDebug(TAG, "Amplitude: " + originalAmplitude + 
                    " -> " + newAmplitude + " (age=" + String.format("%.2f", ageFactor) + 
                    ", battery=" + String.format("%.2f", batteryFactor) + ")");
            }

        } catch (Exception e) {
            if (DEBUG) {
                HookUtils.logDebug(TAG, "Error applying vibration imperfections: " + 
                    e.getMessage());
            }
        }
    }

    private static void updateHardwareAging() {
        // Calculate hardware age based on time since module installation
        long age = System.currentTimeMillis() - moduleInstallTime;
        
        // Age increases slowly over time (simulation)
        hardwareAgeFactor = Math.min(age / (float) AGING_SIMULATION_PERIOD, 1.0f);
        
        // Add some randomness to aging
        hardwareAgeFactor *= (0.8f + random.nextFloat() * 0.4f);
    }

    private static float getEffectiveHardwareAge() {
        // Returns factor from 0.0 (new) to 1.0 (fully degraded)
        updateHardwareAging();
        
        // Apply degradation percentage
        float effectiveAge = 1.0f - (hardwareAgeFactor * hardwareDegradation);
        
        // Clamp to valid range
        return Math.max(0.5f, Math.min(1.0f, effectiveAge));
    }

    private static float getBatteryFactor() {
        // Battery affects vibration intensity
        // Low battery = weaker vibrations
        
        if (isCharging) {
            return 1.0f; // Full power when charging
        }

        // Non-linear battery factor (drops faster below 20%)
        float batteryLevel = currentBatteryLevel / 100.0f;
        
        if (batteryLevel > 0.5f) {
            return 0.9f + (batteryLevel - 0.5f) * 0.2f; // 0.9 - 1.0
        } else if (batteryLevel > 0.2f) {
            return 0.7f + (batteryLevel - 0.2f) * 0.67f; // 0.7 - 0.9
        } else {
            return Math.max(0.3f, batteryLevel * 3.5f); // 0.3 - 0.7
        }
    }
}
