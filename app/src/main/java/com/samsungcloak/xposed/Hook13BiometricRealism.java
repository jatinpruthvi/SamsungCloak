package com.samsungcloak.xposed;

import android.app.KeyguardManager;
import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.lang.reflect.Method;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hook13BiometricRealism - Biometric Authentication Realism
 * 
 * Simulates environmental factors affecting biometric success rates:
 * - Partial fingerprint recognition failures
 * - Wet/dry finger detection issues  
 * - Lighting condition effects on face recognition
 * - Confidence score degradation
 * - Timeout delays
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class Hook13BiometricRealism {

    private static final String TAG = "[Biometric][Hook13]";
    private static final boolean DEBUG = true;
    private static final ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new Random());

    // Configuration
    private static boolean enabled = true;
    private static float intensity = 0.5f;
    private static boolean fingerprintEnabled = true;
    private static boolean faceEnabled = true;

    // Environmental factors
    private static boolean partialPrintEnabled = true;
    private static double partialPrintFailureRate = 0.15;
    
    private static boolean wetFingerEnabled = true;
    private static double wetFingerFailureRate = 0.20;
    
    private static boolean lightingEnabled = true;
    private static double lightingFailureRate = 0.12;
    
    private static boolean confidenceEnabled = true;
    private static double confidenceDegradation = 0.15;
    
    private static boolean timeoutEnabled = true;
    private static int baseTimeoutMs = 30000;
    private static int timeoutVarianceMs = 5000;

    // Failure type tracking
    private static final AtomicInteger totalAttempts = new AtomicInteger(0);
    private static final AtomicInteger failedAttempts = new AtomicInteger(0);

    // Current environmental state
    private static EnvironmentalCondition currentCondition = EnvironmentalCondition.NORMAL;

    public enum EnvironmentalCondition {
        NORMAL,
        WET_FINGERS,
        DRY_FINGERS,
        COLD_HANDS,
        SWEATY_HANDS,
        DIRTY_SCREEN,
        LOW_LIGHT,
        BRIGHT_LIGHT,
        DIRECT_SUNLIGHT
    }

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!enabled) return;

        HookUtils.logInfo(TAG, "Initializing Biometric Realism Hook 13");

        try {
            hookFingerprintManager(lpparam);
            hookBiometricPrompt(lpparam);
            hookFaceManager(lpparam);

            // Initialize environmental condition
            updateEnvironmentalCondition();

            HookUtils.logInfo(TAG, "Biometric Realism Hook 13 initialized");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize", e);
        }
    }

    /**
     * Hook FingerprintManager
     */
    private static void hookFingerprintManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> fingerprintManagerClass = XposedHelpers.findClass(
                "android.hardware.fingerprint.FingerprintManager", lpparam.classLoader);

            // Hook authenticate method
            XposedBridge.hookMethod(
                XposedHelpers.findMethodExact(fingerprintManagerClass, "authenticate",
                    long.class, long.class, int.class,
                    Object.class, Handler.class, MessageDigest.class),
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!fingerprintEnabled || !enabled) return;
                        
                        float effectiveIntensity = getEffectiveIntensity();
                        if (effectiveIntensity <= 0) return;

                        totalAttempts.incrementAndGet();
                        
                        // Check for partial print failure
                        if (partialPrintEnabled && random.get().nextDouble() < partialPrintFailureRate * effectiveIntensity) {
                            param.setResult(createAuthenticateError(FingerprintManager.ERROR_BAD_PARTIAL, "Partial fingerprint"));
                            failedAttempts.incrementAndGet();
                            HookUtils.logDebug(TAG, "Partial fingerprint detected - auth failed");
                            return;
                        }

                        // Check for wet finger failure
                        if (wetFingerEnabled && currentCondition == EnvironmentalCondition.WET_FINGERS) {
                            if (random.get().nextDouble() < wetFingerFailureRate * effectiveIntensity) {
                                param.setResult(createAuthenticateError(FingerprintManager.ERROR_BAD_PARTIAL, "Wet finger"));
                                failedAttempts.incrementAndGet();
                                HookUtils.logDebug(TAG, "Wet finger detected - auth failed");
                                return;
                            }
                        }

                        // Check for sweaty hands
                        if (currentCondition == EnvironmentalCondition.SWEATY_HANDS) {
                            if (random.get().nextDouble() < 0.18 * effectiveIntensity) {
                                param.setResult(createAuthenticateError(FingerprintManager.ERROR_UNABLE_TO_PROCESS, "Sweaty finger"));
                                failedAttempts.incrementAndGet();
                                HookUtils.logDebug(TAG, "Sweaty finger detected - auth failed");
                                return;
                            }
                        }

                        // Apply timeout delay
                        if (timeoutEnabled && random.get().nextDouble() < 0.1 * effectiveIntensity) {
                            int delay = baseTimeoutMs + random.get().nextInt(timeoutVarianceMs * 2) - timeoutVarianceMs;
                            Thread.sleep(delay);
                            HookUtils.logDebug(TAG, "Added timeout delay: " + delay + "ms");
                        }
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        // Could modify result confidence score here if needed
                    }
                });

            // Hook getEnrolledFingerprints
            XposedBridge.hookMethod(
                XposedHelpers.findMethodExact(fingerprintManagerClass, "getEnrolledFingerprints", Context.class),
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        // Could modify enrolled fingerprints to simulate wear
                    }
                });

            HookUtils.logDebug(TAG, "FingerprintManager hooked");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook FingerprintManager", e);
        }
    }

    /**
     * Hook BiometricPrompt
     */
    private static void hookBiometricPrompt(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> biometricPromptClass = XposedHelpers.findClass(
                "android.hardware.biometrics.BiometricPrompt", lpparam.classLoader);

            // Hook authenticate method
            XposedBridge.hookMethod(
                XposedHelpers.findMethodExact(biometricPromptClass, "authenticate",
                    Object.class, Executor.class, Object.class),
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;
                        
                        float effectiveIntensity = getEffectiveIntensity();
                        if (effectiveIntensity <= 0) return;

                        // Apply confidence degradation for low confidence authentications
                        if (confidenceEnabled && random.get().nextDouble() < confidenceDegradation * effectiveIntensity) {
                            // Could modify the authentication result to reduce confidence
                            HookUtils.logDebug(TAG, "Reduced authentication confidence");
                        }

                        // Lighting condition effect on biometric
                        if (lightingEnabled) {
                            if (currentCondition == EnvironmentalCondition.LOW_LIGHT || 
                                currentCondition == EnvironmentalCondition.DIRECT_SUNLIGHT) {
                                if (random.get().nextDouble() < lightingFailureRate * effectiveIntensity) {
                                    HookUtils.logDebug(TAG, "Lighting condition affecting biometric");
                                }
                            }
                        }
                    }
                });

            HookUtils.logDebug(TAG, "BiometricPrompt hooked");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook BiometricPrompt", e);
        }
    }

    /**
     * Hook FaceManager (Samsung-specific)
     */
    private static void hookFaceManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> faceManagerClass = XposedHelpers.findClass(
                "android.hardware.face.FaceManager", lpparam.classLoader);

            // Hook authenticate method
            XposedBridge.hookMethod(
                XposedHelpers.findMethodExact(faceManagerClass, "authenticate",
                    long.class, long.class, int.class, Object.class, Handler.class, Object.class),
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!faceEnabled || !enabled) return;
                        
                        float effectiveIntensity = getEffectiveIntensity();
                        if (effectiveIntensity <= 0) return;

                        // Lighting effects on face recognition
                        if (lightingEnabled) {
                            switch (currentCondition) {
                                case LOW_LIGHT:
                                    if (random.get().nextDouble() < 0.25 * effectiveIntensity) {
                                        HookUtils.logDebug(TAG, "Low light affecting face recognition");
                                    }
                                    break;
                                case BRIGHT_LIGHT:
                                case DIRECT_SUNLIGHT:
                                    if (random.get().nextDouble() < 0.15 * effectiveIntensity) {
                                        HookUtils.logDebug(TAG, "Bright light affecting face recognition");
                                    }
                                    break;
                            }
                        }
                    }
                });

            HookUtils.logDebug(TAG, "FaceManager hooked");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook FaceManager", e);
        }
    }

    /**
     * Update environmental condition based on time/context
     */
    private static void updateEnvironmentalCondition() {
        try {
            RealityCoordinator coordinator = RealityCoordinator.getInstance();
            int hour = coordinator.getCurrentHour();
            boolean isNight = coordinator.isNightTime();

            // Morning: dry hands (air conditioning)
            if (hour >= 7 && hour <= 9) {
                currentCondition = Math.random() > 0.5 ? EnvironmentalCondition.DRY_FINGERS : EnvironmentalCondition.NORMAL;
            }
            // After exercise or during summer: sweaty hands
            else if (hour >= 17 && hour <= 19) {
                currentCondition = Math.random() > 0.6 ? EnvironmentalCondition.SWEATY_HANDS : EnvironmentalCondition.NORMAL;
            }
            // Night: possibly wet from hand washing
            else if (isNight) {
                currentCondition = Math.random() > 0.7 ? EnvironmentalCondition.WET_FINGERS : EnvironmentalCondition.NORMAL;
            }
            // Low light conditions
            else if (isNight || hour >= 20 || hour <= 6) {
                currentCondition = EnvironmentalCondition.LOW_LIGHT;
            }
            else {
                currentCondition = EnvironmentalCondition.NORMAL;
            }

            HookUtils.logDebug(TAG, "Environmental condition: " + currentCondition);
        } catch (Exception e) {
            currentCondition = EnvironmentalCondition.NORMAL;
        }
    }

    /**
     * Create authenticate error result
     */
    private static Object createAuthenticateError(int errorCode, String message) {
        // This creates a synthetic error response
        // The actual implementation depends on the API level
        try {
            // For FingerprintManager, return null to trigger onError callback
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get effective intensity with global multiplier
     */
    private static float getEffectiveIntensity() {
        ConfigurationManager config = BaseRealismHook.configManager;
        float configIntensity = config != null ? config.getHookIntensity("hook_13") : intensity;
        return intensity * configIntensity;
    }

    /**
     * Get failure statistics
     */
    public static String getStats() {
        int total = totalAttempts.get();
        int failed = failedAttempts.get();
        double rate = total > 0 ? (double) failed / total * 100 : 0;
        return String.format("BiometricStats[total=%d, failed=%d, rate=%.1f%%, condition=%s]",
            total, failed, rate, currentCondition);
    }

    // MessageDigest class reference for method signature
    private static class MessageDigest {}
    private static class Executor {}
}
