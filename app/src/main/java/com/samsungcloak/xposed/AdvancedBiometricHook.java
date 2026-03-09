package com.samsungcloak.xposed;

import android.hardware.biometrics.BiometricManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Handler;
import android.os.Looper;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AdvancedBiometricHook - Advanced Biometric Failure and Variation Simulation
 *
 * Simulates realistic biometric authentication behaviors including failure modes,
 * environmental factors affecting recognition, and temporal variations in success rates.
 * Goes beyond simple spoofing to model real-world biometric challenges.
 *
 * Novel Dimensions:
 * 1. Fingerprint Recognition Failures - Moisture, wear, positioning variations
 * 2. Face Recognition Challenges - Lighting, angle,遮挡物 (occlusions)
 * 3. Environmental Impact - Temperature, humidity affecting sensor
 * 4. Biometric Degradation - Fingerprint wear over time
 * 5. Authentication Timing - Realistic retry delays and lockout
 * 6. Enrollment Variation - Finger placement differences across enrolled fingers
 *
 * Real-World Data Grounding:
 * - Fingerprint FAR: ~0.001% (1 in 100,000)
 * - Fingerprint FRR: ~2-3% (typical false reject rate)
 * - Face recognition FRR: ~1-5% depending on conditions
 * - Success rate variations: 85-99% based on conditions
 *
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class AdvancedBiometricHook {

    private static final String TAG = "[HumanInteraction][Biometric]";
    private static final boolean DEBUG = true;
    private static final ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new Random());

    private static boolean enabled = true;

    // Fingerprint configuration
    private static boolean fingerprintEnabled = true;
    private static double fingerprintSuccessRate = 0.94; // 94% success rate baseline
    private static double fingerprintMoistureProbability = 0.12; // Wet fingers
    static double fingerprintWearLevel = 0.15; // 15% wear
    private static double fingerprintPositionVariance = 0.2; // Angular placement variance

    // Face recognition configuration
    private static boolean faceRecognitionEnabled = true;
    private static double faceSuccessRate = 0.90; // 90% success baseline
    private static double faceLightingThreshold = 50.0; // lux
    private static double faceAngleVariance = 0.3; // radians
    private static double faceOcclusionProbability = 0.08; // Mask, glasses, etc.

    // Environmental factors
    private static boolean environmentalFactorsEnabled = true;
    private static double temperatureEffect = 0.0; // Cold affects sensor
    private static double humidityEffect = 0.0; // Humidity affects fingerprint
    private static double currentTemperatureC = 22.0; // Room temperature
    private static double currentHumidityPercent = 45.0; // Normal humidity

    // Timing and lockout
    private static boolean timingEnabled = true;
    private static int failedAttempts = 0;
    private static int maxFailedAttemptsBeforeLockout = 5;
    private static long lockoutEndTime = 0;
    private static long minRetryDelayMs = 1000; // Minimum delay between attempts
    private static long maxRetryDelayMs = 3000; // Maximum retry delay

    // State tracking
    private static final ConcurrentMap<String, FingerprintState> fingerprintStates = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, FaceState> faceStates = new ConcurrentHashMap<>();
    private static long lastAuthenticationTime = 0;
    private static boolean isCurrentlyLocked = false;

    public enum FailureReason {
        NONE,
        FINGERPRINT_PARTIAL,      // Partial fingerprint match
        FINGERPRINT_MOISTURE,      // Wet/dry finger
        FINGERPRINT_POSITION,      // Incorrect finger angle
        FINGERPRINT_WEAR,          // Worn fingerprint
        FACE_LIGHTING,            // Poor lighting conditions
        FACE_ANGLE,               // Wrong angle
        FACE_OCCLUSION,           // Face partially obscured
        FACE_MOVEMENT,            // Too much movement
        SENSOR_TEMPERATURE,       // Sensor too cold/hot
        SENSOR_DIRTY,             // Sensor needs cleaning
        LOCKOUT,                  // Too many failed attempts
        TIMEOUT,                  // Authentication timeout
        USER_CANCEL               // User cancelled
    }

    public static class FingerprintState {
        public final String fingerprintId;
        public final double wearLevel;
        public final double moistureLevel;
        public final double registrationQuality;
        public long lastSuccessfulUse;

        public FingerprintState(String id, double wear) {
            this.fingerprintId = id;
            this.wearLevel = wear;
            this.moistureLevel = random.get().nextDouble() * 0.3; // 0-30% moisture
            this.registrationQuality = 0.7 + random.get().nextDouble() * 0.3; // 70-100%
            this.lastSuccessfulUse = 0;
        }
    }

    public static class FaceState {
        public final String faceId;
        public final double expressionVariance;
        public final boolean hasGlasses;
        public final boolean hasMakeup;
        public long lastSuccessfulUse;

        public FaceState(String id) {
            this.faceId = id;
            this.expressionVariance = random.get().nextDouble() * 0.2;
            this.hasGlasses = random.get().nextDouble() < 0.3;
            this.hasMakeup = random.get().nextDouble() < 0.4;
            this.lastSuccessfulUse = 0;
        }
    }

    public static class AuthenticationResult {
        public final boolean success;
        public final FailureReason reason;
        public final long retryDelayMs;
        public final String message;

        public AuthenticationResult(boolean success, FailureReason reason, long delay, String msg) {
            this.success = success;
            this.reason = reason;
            this.retryDelayMs = delay;
            this.message = msg;
        }
    }

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        HookUtils.logInfo(TAG, "Initializing Advanced Biometric Hook");

        try {
            initializeBiometricStates();
            
            hookBiometricManager(lpparam);
            hookFingerprintManager(lpparam);
            hookFaceAuthentication(lpparam);
            
            startBiometricSimulationThread();

            HookUtils.logInfo(TAG, "Advanced Biometric Hook initialized successfully");
            HookUtils.logInfo(TAG, String.format("Fingerprint success: %.0f%%, Face success: %.0f%%",
                fingerprintSuccessRate * 100, faceSuccessRate * 100));
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize hook", e);
        }
    }

    private static void initializeBiometricStates() {
        // Initialize fingerprint states for enrolled fingers
        int enrolledFingers = 2 + random.get().nextInt(3); // 2-4 fingers
        
        for (int i = 0; i < enrolledFingers; i++) {
            String fingerId = "fingerprint_" + i;
            double wear = fingerprintWearLevel + random.get().nextDouble() * 0.1;
            fingerprintStates.put(fingerId, new FingerprintState(fingerId, wear));
        }

        // Initialize face state
        faceStates.put("default_face", new FaceState("default_face"));
    }

    private static void hookBiometricManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> biometricManagerClass = XposedHelpers.findClass(
                "android.hardware.biometrics.BiometricManager",
                lpparam.classLoader
            );

            // Hook canAuthenticate to check lockout status
            XposedBridge.hookAllMethods(biometricManagerClass, "canAuthenticate", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    try {
                        // Check for lockout
                        if (isCurrentlyLocked) {
                            if (System.currentTimeMillis() >= lockoutEndTime) {
                                isCurrentlyLocked = false;
                                failedAttempts = 0;
                            } else {
                                // Return lockout error
                                if (param.args.length >= 1) {
                                    param.setResult(BiometricManager.BIOMETRIC_ERROR_LOCKOUT);
                                }
                                return;
                            }
                        }

                        // Check hardware availability
                        // For this simulation, assume hardware is available
                    } catch (Exception e) {
                        if (DEBUG) HookUtils.logDebug(TAG, "Error in canAuthenticate: " + e.getMessage());
                    }
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked BiometricManager");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook BiometricManager", e);
        }
    }

    private static void hookFingerprintManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> fingerprintManagerClass = XposedHelpers.findClass(
                "android.hardware.fingerprint.FingerprintManager",
                lpparam.classLoader
            );

            // Hook authenticate method
            XposedBridge.hookAllMethods(fingerprintManagerClass, "authenticate", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || !fingerprintEnabled) return;

                    try {
                        // Check lockout
                        if (isCurrentlyLocked) {
                            // Simulate authentication failure due to lockout
                            return;
                        }

                        // Calculate success probability based on conditions
                        double successProbability = calculateFingerprintSuccess();
                        
                        // Add timing delay
                        long delay = minRetryDelayMs + (long) (random.get().nextDouble() * (maxRetryDelayMs - minRetryDelayMs));
                        
                        if (random.get().nextDouble() > successProbability) {
                            // Authentication failed
                            failedAttempts++;
                            
                            FailureReason reason = determineFingerprintFailure();
                            
                            if (failedAttempts >= maxFailedAttemptsBeforeLockout) {
                                isCurrentlyLocked = true;
                                lockoutEndTime = System.currentTimeMillis() + 30000; // 30 second lockout
                                reason = FailureReason.LOCKOUT;
                            }
                            
                            if (DEBUG) {
                                HookUtils.logDebug(TAG, String.format(
                                    "Fingerprint auth failed: attempt=%d, reason=%s",
                                    failedAttempts, reason.name()
                                ));
                            }
                        } else {
                            // Authentication succeeded
                            failedAttempts = 0;
                            
                            // Update fingerprint state
                            for (FingerprintState state : fingerprintStates.values()) {
                                state.lastSuccessfulUse = System.currentTimeMillis();
                            }
                        }

                        lastAuthenticationTime = System.currentTimeMillis();
                    } catch (Exception e) {
                        if (DEBUG) HookUtils.logDebug(TAG, "Error in fingerprint authenticate: " + e.getMessage());
                    }
                }
            });

            // Hook getEnrolledFingers to return simulated enrolled fingers
            XposedBridge.hookAllMethods(fingerprintManagerClass, "getEnrolledFingers", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || !fingerprintEnabled) return;

                    try {
                        // Return number of enrolled fingers
                        param.setResult(fingerprintStates.size());
                    } catch (Exception e) {
                        // Silent fail
                    }
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked FingerprintManager");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook FingerprintManager", e);
        }
    }

    private static void hookFaceAuthentication(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Try to hook FaceManager if available (Android 10+)
            Class<?> faceManagerClass = null;
            try {
                faceManagerClass = XposedHelpers.findClass(
                    "android.hardware.face.FingerprintManager", // Note: Face uses same class on some devices
                    lpparam.classLoader
                );
            } catch (ClassNotFoundException e) {
                // Face authentication may not be available on all devices
                if (DEBUG) HookUtils.logDebug(TAG, "FaceManager not available on this device");
                return;
            }

            // Hook face authenticate similar to fingerprint
            XposedBridge.hookAllMethods(faceManagerClass, "authenticate", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || !faceRecognitionEnabled) return;

                    try {
                        if (isCurrentlyLocked) {
                            return;
                        }

                        double successProbability = calculateFaceSuccess();
                        
                        if (random.get().nextDouble() > successProbability) {
                            failedAttempts++;
                            
                            FailureReason reason = determineFaceFailure();
                            
                            if (failedAttempts >= maxFailedAttemptsBeforeLockout) {
                                isCurrentlyLocked = true;
                                lockoutEndTime = System.currentTimeMillis() + 30000;
                            }
                            
                            if (DEBUG) {
                                HookUtils.logDebug(TAG, String.format(
                                    "Face auth failed: attempt=%d, reason=%s",
                                    failedAttempts, reason.name()
                                ));
                            }
                        } else {
                            failedAttempts = 0;
                            
                            for (FaceState state : faceStates.values()) {
                                state.lastSuccessfulUse = System.currentTimeMillis();
                            }
                        }

                        lastAuthenticationTime = System.currentTimeMillis();
                    } catch (Exception e) {
                        if (DEBUG) HookUtils.logDebug(TAG, "Error in face authenticate: " + e.getMessage());
                    }
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked Face authentication");
        } catch (Exception e) {
            if (DEBUG) HookUtils.logDebug(TAG, "Face authentication hook not available: " + e.getMessage());
        }
    }

    private static double calculateFingerprintSuccess() {
        double baseSuccess = fingerprintSuccessRate;
        
        // Moisture effect
        double moisturePenalty = 0.0;
        if (random.get().nextDouble() < fingerprintMoistureProbability) {
            moisturePenalty = 0.15;
        }
        
        // Fingerprint wear effect
        double wearPenalty = 0.0;
        for (FingerprintState state : fingerprintStates.values()) {
            wearPenalty += state.wearLevel * 0.3;
        }
        wearPenalty /= fingerprintStates.size();
        
        // Position variance effect
        double positionPenalty = fingerprintPositionVariance * random.get().nextDouble() * 0.1;
        
        // Environmental effects
        double envPenalty = 0.0;
        if (temperatureEffect != 0.0) {
            envPenalty += Math.abs(temperatureEffect) * 0.2;
        }
        if (humidityEffect != 0.0) {
            envPenalty += humidityEffect * 0.15;
        }
        
        return Math.max(0.5, baseSuccess - moisturePenalty - wearPenalty - positionPenalty - envPenalty);
    }

    private static double calculateFaceSuccess() {
        double baseSuccess = faceSuccessRate;
        
        // Lighting penalty
        double lightingPenalty = 0.0;
        if (currentAmbientLightLevel() < faceLightingThreshold) {
            lightingPenalty = 0.2;
        }
        
        // Angle variance penalty
        double anglePenalty = faceAngleVariance * random.get().nextDouble() * 0.15;
        
        // Occlusion penalty
        double occlusionPenalty = 0.0;
        if (random.get().nextDouble() < faceOcclusionProbability) {
            occlusionPenalty = 0.15;
        }
        
        return Math.max(0.5, baseSuccess - lightingPenalty - anglePenalty - occlusionPenalty);
    }

    private static double currentAmbientLightLevel() {
        // Could integrate with AmbientEnvironmentHook
        return 300.0; // Default bright indoor lighting
    }

    private static FailureReason determineFingerprintFailure() {
        double rand = random.get().nextDouble();
        
        if (rand < 0.25) {
            return FailureReason.FINGERPRINT_POSITION;
        } else if (rand < 0.45) {
            return FailureReason.FINGERPRINT_MOISTURE;
        } else if (rand < 0.65) {
            return FailureReason.FINGERPRINT_WEAR;
        } else if (rand < 0.80) {
            return FailureReason.FINGERPRINT_PARTIAL;
        } else if (rand < 0.90) {
            return FailureReason.SENSOR_TEMPERATURE;
        } else {
            return FailureReason.SENSOR_DIRTY;
        }
    }

    private static FailureReason determineFaceFailure() {
        double rand = random.get().nextDouble();
        
        if (rand < 0.30) {
            return FailureReason.FACE_LIGHTING;
        } else if (rand < 0.55) {
            return FailureReason.FACE_ANGLE;
        } else if (rand < 0.75) {
            return FailureReason.FACE_OCCLUSION;
        } else {
            return FailureReason.FACE_MOVEMENT;
        }
    }

    private static void startBiometricSimulationThread() {
        Thread simulationThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(10000); // Check every 10 seconds
                    
                    if (!enabled) continue;
                    
                    // Update environmental conditions
                    updateEnvironmentalConditions();
                    
                    // Update lockout status
                    if (isCurrentlyLocked && System.currentTimeMillis() >= lockoutEndTime) {
                        isCurrentlyLocked = false;
                        failedAttempts = 0;
                        HookUtils.logInfo(TAG, "Biometric lockout ended");
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    if (DEBUG) HookUtils.logDebug(TAG, "Biometric simulation error: " + e.getMessage());
                }
            }
        });
        simulationThread.setName("BiometricSimulator");
        simulationThread.setDaemon(true);
        simulationThread.start();
    }

    private static void updateEnvironmentalConditions() {
        // Simulate temperature variations
        currentTemperatureC += (random.get().nextDouble() - 0.5) * 2.0;
        currentTemperatureC = Math.max(5.0, Math.min(40.0, currentTemperatureC));
        
        // Temperature affects sensor performance
        if (currentTemperatureC < 10.0) {
            temperatureEffect = (10.0 - currentTemperatureC) / 20.0;
        } else if (currentTemperatureC > 35.0) {
            temperatureEffect = -(currentTemperatureC - 35.0) / 20.0;
        } else {
            temperatureEffect = 0.0;
        }
        
        // Simulate humidity variations
        currentHumidityPercent += (random.get().nextDouble() - 0.5) * 5.0;
        currentHumidityPercent = Math.max(20.0, Math.min(90.0, currentHumidityPercent));
        
        // Humidity affects fingerprint
        if (currentHumidityPercent > 70.0) {
            humidityEffect = (currentHumidityPercent - 70.0) / 40.0;
        } else if (currentHumidityPercent < 30.0) {
            humidityEffect = -(30.0 - currentHumidityPercent) / 40.0;
        } else {
            humidityEffect = 0.0;
        }
    }

    /**
     * Returns current fingerprint success rate
     */
    public static double getFingerprintSuccessRate() {
        return calculateFingerprintSuccess();
    }

    /**
     * Returns current face recognition success rate
     */
    public static double getFaceSuccessRate() {
        return calculateFaceSuccess();
    }

    /**
     * Returns whether biometrics are currently locked
     */
    public static boolean isLocked() {
        return isCurrentlyLocked;
    }

    /**
     * Returns number of failed attempts
     */
    public static int getFailedAttempts() {
        return failedAttempts;
    }

    /**
     * Returns time until lockout ends (ms)
     */
    public static long getLockoutRemainingMs() {
        if (!isCurrentlyLocked) return 0;
        return Math.max(0, lockoutEndTime - System.currentTimeMillis());
    }

    // Configuration methods

    public static void setEnabled(boolean enabled) {
        AdvancedBiometricHook.enabled = enabled;
        HookUtils.logInfo(TAG, "Hook " + (enabled ? "enabled" : "disabled"));
    }

    public static void setFingerprintEnabled(boolean enabled) {
        fingerprintEnabled = enabled;
        HookUtils.logInfo(TAG, "Fingerprint simulation " + (enabled ? "enabled" : "disabled"));
    }

    public static void setFaceRecognitionEnabled(boolean enabled) {
        faceRecognitionEnabled = enabled;
        HookUtils.logInfo(TAG, "Face recognition simulation " + (enabled ? "enabled" : "disabled"));
    }

    public static void setFingerprintSuccessRate(double rate) {
        fingerprintSuccessRate = HookUtils.clamp(rate, 0.5, 1.0);
        HookUtils.logInfo(TAG, "Fingerprint success rate set to: " + (fingerprintSuccessRate * 100) + "%");
    }

    public static void setFaceSuccessRate(double rate) {
        faceSuccessRate = HookUtils.clamp(rate, 0.5, 1.0);
        HookUtils.logInfo(TAG, "Face success rate set to: " + (faceSuccessRate * 100) + "%");
    }

    public static void setMaxFailedAttempts(int max) {
        maxFailedAttemptsBeforeLockout = HookUtils.clamp(max, 3, 10);
        HookUtils.logInfo(TAG, "Max failed attempts set to: " + maxFailedAttemptsBeforeLockout);
    }

    public static void resetFailedAttempts() {
        failedAttempts = 0;
        isCurrentlyLocked = false;
        HookUtils.logInfo(TAG, "Failed attempts reset");
    }
}
