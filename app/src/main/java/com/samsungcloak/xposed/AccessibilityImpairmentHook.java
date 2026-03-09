package com.samsungcloak.xposed;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * AccessibilityImpairmentHook - Motor and Accessibility Simulation for UX Testing
 *
 * Simulates realistic human motor impairments and accessibility variations including
 * tremor, reduced dexterity, one-handed use bias, and motor control variations.
 * These factors significantly impact touch accuracy and interaction patterns.
 *
 * Novel Dimensions:
 * 1. Essential Tremor Simulation - Age-related hand tremors affecting precision
 * 2. One-Handed Usage Bias - Device grip affects touch reach and accuracy
 * 3. Reduced Dexterity - Slower and less precise finger movements
 * 4. Touch Target Size Adaptation - Users with motor impairments need larger targets
 * 5. Motor Fatigue - Progressive degradation during extended use
 *
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class AccessibilityImpairmentHook {

    private static final String TAG = "[HumanInteraction][Accessibility]";
    private static final boolean DEBUG = true;
    private static final ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new Random());

    private static boolean enabled = true;
    
    // Tremor configuration
    private static boolean tremorEnabled = true;
    private static double tremorIntensity = 0.3; // 0.0 - 1.0 scale
    private static double tremorFrequency = 8.0; // Hz
    
    // One-handed use configuration
    private static boolean oneHandedBiasEnabled = true;
    private static double dominantHandBias = 0.65; // 65% right-handed
    private static double oneHandedProbability = 0.45;
    
    // Dexterity configuration
    private static boolean reducedDexterityEnabled = true;
    private static double dexterityLevel = 0.7; // 1.0 = full dexterity, 0.0 = severe impairment
    
    // Motor fatigue
    private static boolean motorFatigueEnabled = true;
    private static long sessionStartTime = 0;
    private static double currentFatigueLevel = 0.0;
    
    // State tracking
    private static boolean isOneHandedMode = false;
    private static Handedness currentHandedness = Handedness.RIGHT;
    private static long lastInteractionTime = 0;
    private static List<Float> recentTouchOffsets = new CopyOnWriteArrayList<>();

    public enum Handedness {
        LEFT, RIGHT, AMBIDEXTROUS
    }

    public enum ImpairmentLevel {
        NONE(1.0),
        MILD(0.8),
        MODERATE(0.6),
        SEVERE(0.3);

        public final float dexterityMultiplier;

        ImpairmentLevel(double multiplier) {
            this.dexterityMultiplier = (float) multiplier;
        }
    }

    private static ImpairmentLevel currentImpairmentLevel = ImpairmentLevel.NONE;

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        HookUtils.logInfo(TAG, "Initializing Accessibility Impairment Hook");

        try {
            sessionStartTime = System.currentTimeMillis();
            determineInitialHandedness();
            
            hookViewOnTouchEvent(lpparam);
            hookMotionEvent(lpparam);
            hookActivityLifecycle(lpparam);
            
            HookUtils.logInfo(TAG, "Accessibility Impairment Hook initialized successfully");
            HookUtils.logInfo(TAG, String.format("Initial state: handedness=%s, one-handed=%.0f%%, tremor=%.2f", 
                currentHandedness, oneHandedProbability * 100, tremorIntensity));
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize hook", e);
        }
    }

    private static void determineInitialHandedness() {
        double rand = random.get().nextDouble();
        if (rand < dominantHandBias) {
            currentHandedness = Handedness.RIGHT;
        } else if (rand < dominantHandBias + 0.05) {
            currentHandedness = Handedness.AMBIDEXTROUS;
        } else {
            currentHandedness = Handedness.LEFT;
        }

        isOneHandedMode = random.get().nextDouble() < oneHandedProbability;
    }

    private static void hookViewOnTouchEvent(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> viewClass = XposedHelpers.findClass("android.view.View", lpparam.classLoader);
            
            XposedBridge.hookAllMethods(viewClass, "onTouchEvent", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    try {
                        MotionEvent event = (MotionEvent) param.args[0];
                        int action = event.getActionMasked();

                        if (action == MotionEvent.ACTION_DOWN) {
                            updateMotorState();
                        }

                        if (action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_DOWN) {
                            applyTremorCorrection(event);
                        }

                    } catch (Exception e) {
                        if (DEBUG) HookUtils.logDebug(TAG, "Error in touch event hook: " + e.getMessage());
                    }
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked View.onTouchEvent for accessibility");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook View.onTouchEvent", e);
        }
    }

    private static void hookMotionEvent(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> motionEventClass = XposedHelpers.findClass("android.view.MotionEvent", lpparam.classLoader);

            XposedBridge.hookAllMethods(motionEventClass, "getX", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || !reducedDexterityEnabled) return;

                    try {
                        float originalX = (float) param.getResult();
                        float correctedX = applyDexterityCorrection(originalX, true);
                        param.setResult(correctedX);
                    } catch (Exception e) {
                        // Silent fail
                    }
                }
            });

            XposedBridge.hookAllMethods(motionEventClass, "getY", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || !reducedDexterityEnabled) return;

                    try {
                        float originalY = (float) param.getResult();
                        float correctedY = applyDexterityCorrection(originalY, false);
                        param.setResult(correctedY);
                    } catch (Exception e) {
                        // Silent fail
                    }
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked MotionEvent for dexterity correction");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook MotionEvent", e);
        }
    }

    private static void hookActivityLifecycle(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> activityClass = XposedHelpers.findClass("android.app.Activity", lpparam.classLoader);

            XposedBridge.hookAllMethods(activityClass, "onResume", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    try {
                        // Reset motor fatigue on activity resume after significant break
                        long breakDuration = System.currentTimeMillis() - lastInteractionTime;
                        if (breakDuration > 300000) { // 5 minutes
                            currentFatigueLevel = Math.max(0.0, currentFatigueLevel - 0.3);
                        }

                        // Occasionally switch handedness
                        if (random.get().nextDouble() < 0.1) {
                            determineInitialHandedness();
                        }

                    } catch (Exception e) {
                        if (DEBUG) HookUtils.logDebug(TAG, "Error in activity resume: " + e.getMessage());
                    }
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked Activity lifecycle");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook Activity lifecycle", e);
        }
    }

    /**
     * 1. ESSENTIAL TREMOR SIMULATION
     *
     * Simulates age-related hand tremors using sinusoidal motion with
     * random phase offsets. Tremor intensity increases with fatigue.
     */
    private static void applyTremorCorrection(MotionEvent event) {
        if (!tremorEnabled || currentImpairmentLevel == ImpairmentLevel.NONE) {
            return;
        }

        try {
            long eventTime = event.getEventTime();
            double timeInSeconds = eventTime / 1000.0;

            // Multi-frequency tremor model
            double tremorX = 0;
            double tremorY = 0;

            // Primary frequency
            double phase1 = timeInSeconds * tremorFrequency * 2 * Math.PI;
            tremorX += Math.sin(phase1) * tremorIntensity * 2.0;
            tremorY += Math.cos(phase1) * tremorIntensity * 2.0;

            // Secondary harmonic
            double phase2 = timeInSeconds * tremorFrequency * 1.5 * 2 * Math.PI + 0.5;
            tremorX += Math.sin(phase2) * tremorIntensity * 1.2;
            tremorY += Math.cos(phase2) * tremorIntensity * 1.2;

            // Add randomness
            tremorX += random.get().nextGaussian() * tremorIntensity * 0.8;
            tremorY += random.get().nextGaussian() * tremorIntensity * 0.8;

            // Apply fatigue multiplier
            double fatigueMultiplier = 1.0 + currentFatigueLevel * 0.5;
            tremorX *= fatigueMultiplier;
            tremorY *= fatigueMultiplier;

            event.offsetLocation((float) tremorX, (float) tremorY);

            if (DEBUG && random.get().nextDouble() < 0.005) {
                HookUtils.logDebug(TAG, String.format(
                    "Tremor applied: offset=(%.2f, %.2f), fatigue=%.2f",
                    tremorX, tremorY, currentFatigueLevel
                ));
            }

        } catch (Exception e) {
            if (DEBUG) HookUtils.logDebug(TAG, "Error applying tremor: " + e.getMessage());
        }
    }

    /**
     * 2. ONE-HANDED USAGE BIAS
     *
     * Adjusts touch positions based on which hand is holding the device.
     * Users typically reach less comfortably with their non-dominant hand.
     */
    private static float applyDexterityCorrection(float coordinate, boolean isX) {
        float correction = 0;

        // Apply one-handed reach limitations
        if (isOneHandedMode && oneHandedBiasEnabled) {
            double reachDifficulty = 1.0 - dexterityLevel;
            
            // Non-dominant hand has reduced reach in certain areas
            boolean isNonDominantHand = (currentHandedness == Handedness.LEFT);
            
            if (isNonDominantHand) {
                // Left-handed users (or left-hand mode) have difficulty reaching right side
                if (isX && coordinate > 300) { // Right side of screen
                    correction = (float) ((random.get().nextDouble() - 0.5) * reachDifficulty * 30);
                }
            } else {
                // Right-handed users have difficulty reaching left side
                if (isX && coordinate < 180) { // Left side of screen
                    correction = (float) ((random.get().nextDouble() - 0.5) * reachDifficulty * 25);
                }
            }

            // Upper portion of screen harder to reach in one-handed mode
            if (!isX && coordinate < 400) {
                correction += (float) ((random.get().nextDouble() - 0.5) * reachDifficulty * 20);
            }
        }

        // Apply general dexterity impairment
        correction += (float) ((random.get().nextGaussian()) * (1.0 - dexterityLevel) * 15);
        
        // Record for motor fatigue tracking
        recentTouchOffsets.add(Math.abs(correction));
        if (recentTouchOffsets.size() > 100) {
            recentTouchOffsets.remove(0);
        }

        return coordinate + correction;
    }

    /**
     * 4. MOTOR FATIGUE SIMULATION
     *
     * Tracks motor fatigue over extended use sessions.
     * Fatigue increases error rate and reduces precision.
     */
    private static void updateMotorState() {
        lastInteractionTime = System.currentTimeMillis();

        if (!motorFatigueEnabled) return;

        long sessionDuration = (System.currentTimeMillis() - sessionStartTime) / 60000; // minutes

        // Fatigue increases with session duration
        if (sessionDuration < 5) {
            currentFatigueLevel = sessionDuration * 0.02;
        } else if (sessionDuration < 30) {
            currentFatigueLevel = 0.1 + (sessionDuration - 5) * 0.015;
        } else {
            currentFatigueLevel = 0.475 + (sessionDuration - 30) * 0.008;
        }

        currentFatigueLevel = Math.min(currentFatigueLevel, 1.0);

        if (DEBUG && random.get().nextDouble() < 0.01) {
            HookUtils.logDebug(TAG, String.format(
                "Motor state updated: fatigue=%.2f, session=%d min",
                currentFatigueLevel, sessionDuration
            ));
        }
    }

    /**
     * Determines if a touch target should be enlarged for accessibility
     */
    public static float getAccessibleTouchTargetSize(float originalSize) {
        if (currentImpairmentLevel == ImpairmentLevel.NONE || !enabled) {
            return originalSize;
        }

        float enlargementFactor = 1.0f + (1.0f - currentImpairmentLevel.dexterityMultiplier) * 0.5f;
        return originalSize * enlargementFactor;
    }

    /**
     * Returns current motor accuracy level based on fatigue and impairment
     */
    public static double getCurrentAccuracyLevel() {
        double baseAccuracy = dexterityLevel;
        double fatiguePenalty = currentFatigueLevel * 0.3;
        double impairmentPenalty = 1.0 - currentImpairmentLevel.dexterityMultiplier;
        
        return Math.max(0.1, baseAccuracy - fatiguePenalty - impairmentPenalty);
    }

    // Configuration methods

    public static void setEnabled(boolean enabled) {
        AccessibilityImpairmentHook.enabled = enabled;
        HookUtils.logInfo(TAG, "Hook " + (enabled ? "enabled" : "disabled"));
    }

    public static void setTremorEnabled(boolean enabled) {
        AccessibilityImpairmentHook.tremorEnabled = enabled;
        HookUtils.logInfo(TAG, "Tremor simulation " + (enabled ? "enabled" : "disabled"));
    }

    public static void setTremorIntensity(double intensity) {
        AccessibilityImpairmentHook.tremorIntensity = HookUtils.clamp(intensity, 0.0, 1.0);
        HookUtils.logInfo(TAG, "Tremor intensity set to: " + AccessibilityImpairmentHook.tremorIntensity);
    }

    public static void setDexterityLevel(double level) {
        AccessibilityImpairmentHook.dexterityLevel = HookUtils.clamp(level, 0.0, 1.0);
        
        // Update impairment level based on dexterity
        if (dexterityLevel >= 0.95) {
            currentImpairmentLevel = ImpairmentLevel.NONE;
        } else if (dexterityLevel >= 0.75) {
            currentImpairmentLevel = ImpairmentLevel.MILD;
        } else if (dexterityLevel >= 0.5) {
            currentImpairmentLevel = ImpairmentLevel.MODERATE;
        } else {
            currentImpairmentLevel = ImpairmentLevel.SEVERE;
        }
        
        HookUtils.logInfo(TAG, "Dexterity level set to: " + AccessibilityImpairmentHook.dexterityLevel + 
            " (" + currentImpairmentLevel.name() + ")");
    }

    public static void setOneHandedProbability(double probability) {
        AccessibilityImpairmentHook.oneHandedProbability = HookUtils.clamp(probability, 0.0, 1.0);
        HookUtils.logInfo(TAG, "One-handed probability set to: " + AccessibilityImpairmentHook.oneHandedProbability);
    }

    public static void setDominantHandBias(double bias) {
        AccessibilityImpairmentHook.dominantHandBias = HookUtils.clamp(bias, 0.0, 1.0);
        HookUtils.logInfo(TAG, "Dominant hand bias set to: " + AccessibilityImpairmentHook.dominantHandBias);
    }

    public static Handedness getCurrentHandedness() {
        return currentHandedness;
    }

    public static boolean isOneHandedMode() {
        return isOneHandedMode;
    }

    public static double getCurrentFatigueLevel() {
        return currentFatigueLevel;
    }

    public static void resetMotorFatigue() {
        currentFatigueLevel = 0.0;
        sessionStartTime = System.currentTimeMillis();
        HookUtils.logInfo(TAG, "Motor fatigue reset");
    }
}
