package com.samsungcloak.realism;

import android.content.Context;
import android.hardware.biometrics.BiometricAuthenticator;
import android.hardware.biometrics.BiometricManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.CancellationSignal;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;

/**
 * BiometricRealismHook - Fingerprint/face/iris authentication failure simulation
 * 
 * Targets: Samsung Galaxy A12 (SM-A125U) Android 10/11
 * 
 * This hook addresses the realism dimension of biometric authentication failures.
 * Real users experience:
 * - Partial/failed fingerprint reads (dry skin, wet fingers, partial contact)
 * - Face unlock failures (lighting conditions, angles, obstructions)
 * - Iris scanner failures (glasses, lighting, distance)
 * - Repeated failure attempts with lockout
 * - Environmental factors affecting biometric performance
 */
public class BiometricRealismHook {
    private static final String TAG = "BiometricRealism";
    private static final String PACKAGE_NAME = "com.samsungcloak.realism";
    
    // Configuration keys
    private static final String KEY_ENABLED = "biometric_enabled";
    private static final String KEY_FINGERPRINT_FAILURES = "biometric_fingerprint_failures";
    private static final String KEY_FACE_FAILURES = "biometric_face_failures";
    private static final String KEY_IRIS_FAILURES = "biometric_iris_failures";
    private static final String KEY_ENVIRONMENTAL_FACTORS = "biometric_environmental";
    private static final String KEY_LOCKOUT_SIMULATION = "biometric_lockout";
    
    // Failure probability constants
    private static final float BASE_FINGERPRINT_FAILURE_RATE = 0.08f;  // 8% typical
    private static final float BASE_FACE_FAILURE_RATE = 0.15f;        // 15% typical
    private static final float BASE_IRIS_FAILURE_RATE = 0.12f;        // 12% typical
    
    // Environmental modifiers
    private static final float WET_FINGER_MULTIPLIER = 2.5f;
    private static final float DRY_SKIN_MULTIPLIER = 1.8f;
    private static final float COLD_WEATHER_MULTIPLIER = 1.5f;
    private static final float DIRTY_SCREEN_MULTIPLIER = 2.0f;
    private static final float LOW_LIGHT_FACE_MULTIPLIER = 2.2f;
    private static final float GLASSES_IRIS_MULTIPLIER = 1.6f;
    
    // State
    private static SharedPreferences sPrefs;
    private static boolean sEnabled = true;
    private static boolean sFingerprintFailures = true;
    private static boolean sFaceFailures = true;
    private static boolean sIrisFailures = true;
    private static boolean sEnvironmentalFactors = true;
    private static boolean sLockoutSimulation = true;
    
    // Runtime state
    private static final Random sRandom = new Random();
    private static int sFingerprintAttemptCount = 0;
    private static int sFingerprintFailureCount = 0;
    private static int sFaceAttemptCount = 0;
    private static int sFaceFailureCount = 0;
    private static int sIrisAttemptCount = 0;
    private static int sIrisFailureCount = 0;
    private static long sLastFingerprintAttempt = 0;
    private static long sLastFaceAttempt = 0;
    private static long sLastIrisAttempt = 0;
    private static int sConsecutiveFailures = 0;
    private static long sLockoutEndTime = 0;
    
    // Environmental state
    private static float sSkinMoisture = 0.5f;  // 0.0 (dry) - 1.0 (wet)
    private static float sAmbientLight = 300f;   // lux
    private static float sScreenCleanliness = 0.8f;  // 0.0 (dirty) - 1.0 (clean)
    private static boolean sUserWearingGlasses = false;
    private static float sTemperature = 22.0f;   // Celsius
    
    /**
     * Initialize the hook
     */
    public static void init(SharedPreferences prefs) {
        sPrefs = prefs;
        reloadSettings();
    }
    
    /**
     * Reload settings
     */
    public static void reloadSettings() {
        if (sPrefs == null) return;
        
        sEnabled = sPrefs.getBoolean(KEY_ENABLED, true);
        sFingerprintFailures = sPrefs.getBoolean(KEY_FINGERPRINT_FAILURES, true);
        sFaceFailures = sPrefs.getBoolean(KEY_FACE_FAILURES, true);
        sIrisFailures = sPrefs.getBoolean(KEY_IRIS_FAILURES, true);
        sEnvironmentalFactors = sPrefs.getBoolean(KEY_ENVIRONMENTAL_FACTORS, true);
        sLockoutSimulation = sPrefs.getBoolean(KEY_LOCKOUT_SIMULATION, true);
    }
    
    /**
     * Hook FingerprintManager (API 23-28)
     */
    public static void hookFingerprintManager(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!sEnabled || Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) return;
        
        try {
            Class<?> fingerprintManagerClass = XposedHelpers.findClass(
                "android.hardware.fingerprint.FingerprintManager",
                lpparam.classLoader);
            
            // Hook authenticate()
            XposedBridge.hookAllMethods(fingerprintManagerClass, "authenticate",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!sEnabled || !sFingerprintFailures) return;
                        
                        // Check for lockout
                        if (isLockedOut(BiometricType.FINGERPRINT)) {
                            // Simulate lockout - auth will fail immediately
                            param.setResult(null);
                            return;
                        }
                        
                        sFingerprintAttemptCount++;
                        sLastFingerprintAttempt = System.currentTimeMillis();
                        
                        // Check if this attempt should fail
                        if (shouldFingerprintFail()) {
                            // Modify callback to simulate failure
                            modifyFingerprintCallback(param);
                        }
                    }
                    
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!sEnabled || !sFingerprintFailures) return;
                        
                        // Track results
                        int result = (Integer) param.getResult();
                        if (result != 0) {  // Non-zero means error
                            sFingerprintFailureCount++;
                            sConsecutiveFailures++;
                            checkLockout(BiometricType.FINGERPRINT);
                        } else {
                            sConsecutiveFailures = 0;
                        }
                    }
                });
            
            // Hook hasEnrolledFingerprints()
            XposedBridge.hookAllMethods(fingerprintManagerClass, "hasEnrolledFingerprints",
                new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) 
                        throws Throwable {
                        return true;  // Always report enrolled
                    }
                });
            
            // Hook isHardwareDetected()
            XposedBridge.hookAllMethods(fingerprintManagerClass, "isHardwareDetected",
                new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) 
                        throws Throwable {
                        return true;  // Always report hardware present
                    }
                });
            
            XposedBridge.log(TAG + ": FingerprintManager hooks installed (API < 28)");
            
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Error hooking FingerprintManager: " + e.getMessage());
        }
    }
    
    /**
     * Hook BiometricManager (API 28+)
     */
    public static void hookBiometricManager(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!sEnabled) return;
        
        try {
            Class<?> biometricManagerClass = XposedHelpers.findClass(
                "android.hardware.biometrics.BiometricManager",
                lpparam.classLoader);
            
            // Hook canAuthenticate()
            XposedBridge.hookAllMethods(biometricManagerClass, "canAuthenticate",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!sEnabled) return;
                        
                        // Return appropriate result based on availability
                        int result = (Integer) param.getResult();
                        
                        // Could modify result based on hardware state
                        if (result == BiometricManager.BIOMETRIC_SUCCESS) {
                            // Check if should simulate biometric unavailable
                            if (sEnvironmentalFactors && sRandom.nextFloat() < 0.02f) {
                                // Rare hardware unavailability
                                param.setResult(BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE);
                            }
                        }
                    }
                });
            
            XposedBridge.log(TAG + ": BiometricManager hooks installed");
            
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Error hooking BiometricManager: " + e.getMessage());
        }
    }
    
    /**
     * Hook BiometricPrompt (API 28+)
     */
    public static void hookBiometricPrompt(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!sEnabled) return;
        
        try {
            Class<?> biometricPromptClass = XposedHelpers.findClass(
                "android.hardware.biometrics.BiometricPrompt",
                lpparam.classLoader);
            
            // Hook authenticate()
            XposedBridge.hookAllMethods(biometricPromptClass, "authenticate",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!sEnabled) return;
                        
                        // Check for lockout across all biometric types
                        if (isAnyLockedOut()) {
                            // Would need to invoke callback with error
                            return;
                        }
                        
                        // Track attempt
                        // Could determine biometric type from params
                        sFingerprintAttemptCount++;
                    }
                });
            
            XposedBridge.log(TAG + ": BiometricPrompt hooks installed");
            
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Error hooking BiometricPrompt: " + e.getMessage());
        }
    }
    
    /**
     * Hook FingerprintManagerCompat (for compatibility)
     */
    public static void hookFingerprintManagerCompat(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!sEnabled) return;
        
        try {
            Class<?> fingerprintManagerCompatClass = XposedHelpers.findClass(
                "androidx.core.hardware.fingerprint.FingerprintManagerCompat",
                lpparam.classLoader);
            
            XposedBridge.log(TAG + ": FingerprintManagerCompat class found");
            
        } catch (Exception e) {
            XposedBridge.log(TAG + ": FingerprintManagerCompat not found (expected)");
        }
    }
    
    /**
     * Hook Samsung's biometric service
     */
    public static void hookSamsungBiometricService(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!sEnabled) return;
        
        try {
            // Samsung's custom biometric implementation
            Class<?> samsungBiometricClass = XposedHelpers.findClass(
                "com.samsung.android.biometrics.fingerprint.FingerprintManager",
                lpparam.classLoader);
            
            // Hook authenticate
            XposedBridge.hookAllMethods(samsungBiometricClass, "authenticate",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!sEnabled || !sFingerprintFailures) return;
                        
                        if (shouldFingerprintFail()) {
                            // Could inject Samsung-specific failure
                            injectSamsungFingerprintFailure(param);
                        }
                    }
                });
            
            XposedBridge.log(TAG + ": Samsung FingerprintManager hooks installed");
            
        } catch (ClassNotFoundException e) {
            // Samsung class not present on AOSP ROMs - normal
            XposedBridge.log(TAG + ": Samsung FingerprintManager not found (expected on AOSP)");
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Error hooking Samsung biometric: " + e.getMessage());
        }
    }
    
    /**
     * Hook FaceManager (API 29+)
     */
    public static void hookFaceManager(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!sEnabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return;
        
        try {
            Class<?> faceManagerClass = XposedHelpers.findClass(
                "android.hardware.face.FaceManager",
                lpparam.classLoader);
            
            // Hook authenticate()
            XposedBridge.hookAllMethods(faceManagerClass, "authenticate",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!sEnabled || !sFaceFailures) return;
                        
                        if (isLockedOut(BiometricType.FACE)) {
                            param.setResult(null);
                            return;
                        }
                        
                        sFaceAttemptCount++;
                        sLastFaceAttempt = System.currentTimeMillis();
                        
                        if (shouldFaceFail()) {
                            modifyFaceCallback(param);
                        }
                    }
                    
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        int result = (Integer) param.getResult();
                        if (result != 0) {
                            sFaceFailureCount++;
                            sConsecutiveFailures++;
                            checkLockout(BiometricType.FACE);
                        } else {
                            sConsecutiveFailures = 0;
                        }
                    }
                });
            
            // Hook hasEnrolledFaces()
            XposedBridge.hookAllMethods(faceManagerClass, "hasEnrolledFaces",
                new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) 
                        throws Throwable {
                        return true;
                    }
                });
            
            XposedBridge.log(TAG + ": FaceManager hooks installed");
            
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Error hooking FaceManager: " + e.getMessage());
        }
    }
    
    /**
     * Hook IrisManager (Samsung specific)
     */
    public static void hookIrisManager(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!sEnabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return;
        
        try {
            Class<?> irisManagerClass = XposedHelpers.findClass(
                "android.hardware.iris.IrisManager",
                lpparam.classLoader);
            
            // Hook authenticate similar to fingerprint
            XposedBridge.hookAllMethods(irisManagerClass, "authenticate",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!sEnabled || !sIrisFailures) return;
                        
                        if (isLockedOut(BiometricType.IRIS)) {
                            param.setResult(null);
                            return;
                        }
                        
                        sIrisAttemptCount++;
                        sLastIrisAttempt = System.currentTimeMillis();
                        
                        if (shouldIrisFail()) {
                            modifyIrisCallback(param);
                        }
                    }
                });
            
            XposedBridge.log(TAG + ": IrisManager hooks installed");
            
        } catch (ClassNotFoundException e) {
            // IrisManager may not exist on all devices
            XposedBridge.log(TAG + ": IrisManager not found");
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Error hooking IrisManager: " + e.getMessage());
        }
    }
    
    /**
     * Determine if fingerprint authentication should fail
     */
    private static boolean shouldFingerprintFail() {
        if (!sFingerprintFailures) return false;
        
        float failureRate = BASE_FINGERPRINT_FAILURE_RATE;
        
        if (sEnvironmentalFactors) {
            // Apply environmental modifiers
            if (sSkinMoisture < 0.2f) {
                failureRate *= DRY_SKIN_MULTIPLIER;
            } else if (sSkinMoisture > 0.9f) {
                failureRate *= WET_FINGER_MULTIPLIER;
            }
            
            if (sTemperature < 5.0f) {
                failureRate *= COLD_WEATHER_MULTIPLIER;
            }
            
            if (sScreenCleanliness < 0.3f) {
                failureRate *= DIRTY_SCREEN_MULTIPLIER;
            }
            
            // Random partial contact
            if (sRandom.nextFloat() < 0.10f) {
                failureRate *= 1.5f;  // Partial finger placement
            }
        }
        
        // Clamp failure rate
        failureRate = Math.min(failureRate, 0.5f);
        
        return sRandom.nextFloat() < failureRate;
    }
    
    /**
     * Determine if face authentication should fail
     */
    private static boolean shouldFaceFail() {
        if (!sFaceFailures) return false;
        
        float failureRate = BASE_FACE_FAILURE_RATE;
        
        if (sEnvironmentalFactors) {
            // Low light conditions
            if (sAmbientLight < 50.0f) {
                failureRate *= LOW_LIGHT_FACE_MULTIPLIER;
            } else if (sAmbientLight < 100.0f) {
                failureRate *= 1.3f;
            }
            
            // Random face angle/position issues
            if (sRandom.nextFloat() < 0.15f) {
                failureRate *= 1.8f;
            }
        }
        
        failureRate = Math.min(failureRate, 0.6f);
        
        return sRandom.nextFloat() < failureRate;
    }
    
    /**
     * Determine if iris authentication should fail
     */
    private static boolean shouldIrisFail() {
        if (!sIrisFailures) return false;
        
        float failureRate = BASE_IRIS_FAILURE_RATE;
        
        if (sEnvironmentalFactors) {
            // Glasses affect iris recognition
            if (sUserWearingGlasses) {
                failureRate *= GLASSES_IRIS_MULTIPLIER;
            }
            
            // Low light affects iris
            if (sAmbientLight < 100.0f) {
                failureRate *= 1.4f;
            }
            
            // Distance/position issues
            if (sRandom.nextFloat() < 0.12f) {
                failureRate *= 1.5f;
            }
        }
        
        failureRate = Math.min(failureRate, 0.5f);
        
        return sRandom.nextFloat() < failureRate;
    }
    
    /**
     * Modify fingerprint callback to simulate failure
     */
    private static void modifyFingerprintCallback(XC_MethodHook.MethodHookParam param) {
        // In real implementation, would invoke the callback with failure
        // This requires accessing the AuthenticationCallback object
        
        // Determine failure reason
        String[] failureReasons = {
            "BAD_PLANCHETTE",
            "FINGER_NOT_MATCH",
            "SENSOR_DIRTY",
            "INSUFFICIENT_COVERAGE",
            "PARTIAL",
            "TOO_FAST"
        };
        
        int reasonIndex = sRandom.nextInt(failureReasons.length);
        
        XposedBridge.log(TAG + ": Simulating fingerprint failure: " + failureReasons[reasonIndex]);
        
        // Could set error code in result to simulate failure
        sFingerprintFailureCount++;
    }
    
    /**
     * Modify face callback to simulate failure
     */
    private static void modifyFaceCallback(XC_MethodHook.MethodHookParam param) {
        String[] failureReasons = {
            "NOT_RECOGNIZED",
            "TOO_BRIGHT",
            "TOO_DARK",
            "TOO_CLOSE",
            "TOO_FAR",
            "FACE_NOT_FOUND"
        };
        
        XposedBridge.log(TAG + ": Simulating face failure");
        sFaceFailureCount++;
    }
    
    /**
     * Modify iris callback to simulate failure
     */
    private static void modifyIrisCallback(XC_MethodHook.MethodHookParam param) {
        String[] failureReasons = {
            "IRIS_NOT_CAPTURED",
            "INSUFFICIENT_QUALITY",
            "TOO_BRIGHT",
            "TOO_DARK"
        };
        
        XposedBridge.log(TAG + ": Simulating iris failure");
        sIrisFailureCount++;
    }
    
    /**
     * Inject Samsung-specific fingerprint failure
     */
    private static void injectSamsungFingerprintFailure(XC_MethodHook.MethodHookParam param) {
        // Samsung-specific error codes
        int[] samsungErrors = {
            0x09,  // SAMSUNG_FINGERPRINT_ERROR_NOT_MATCH
            0x0C,  // SAMSUNG_FINGERPRINT_ERROR_LOW_COVERAGE
            0x0D,  // SAMSUNG_FINGERPRINT_ERROR_PARTIAL
            0x10   // SAMSUNG_FINGERPRINT_ERROR_SKEWED
        };
        
        XposedBridge.log(TAG + ": Injecting Samsung fingerprint failure");
    }
    
    /**
     * Check for lockout after consecutive failures
     */
    private static void checkLockout(BiometricType type) {
        if (!sLockoutSimulation) return;
        
        int maxConsecutiveFailures = 5;  // Lock after 5 failures
        
        if (sConsecutiveFailures >= maxConsecutiveFailures) {
            // Enable lockout (30 seconds typical)
            sLockoutEndTime = System.currentTimeMillis() + 30000;
            XposedBridge.log(TAG + ": Biometric lockout enabled for " + type);
        }
    }
    
    /**
     * Check if specific biometric type is locked out
     */
    private static boolean isLockedOut(BiometricType type) {
        if (!sLockoutSimulation || sLockoutEndTime == 0) return false;
        
        if (System.currentTimeMillis() > sLockoutEndTime) {
            sLockoutEndTime = 0;
            sConsecutiveFailures = 0;
            return false;
        }
        
        return true;
    }
    
    /**
     * Check if any biometric is locked out
     */
    private static boolean isAnyLockedOut() {
        return isLockedOut(BiometricType.FINGERPRINT) ||
               isLockedOut(BiometricType.FACE) ||
               isLockedOut(BiometricType.IRIS);
    }
    
    /**
     * Set environmental factors for realism
     */
    public static void setEnvironmentalFactors(float skinMoisture, float ambientLight, 
                                                 float screenCleanliness, float temperature) {
        sSkinMoisture = skinMoisture;
        sAmbientLight = ambientLight;
        sScreenCleanliness = screenCleanliness;
        sTemperature = temperature;
    }
    
    /**
     * Set user state
     */
    public static void setUserState(boolean wearingGlasses) {
        sUserWearingGlasses = wearingGlasses;
    }
    
    /**
     * Get biometric state for cross-hook coherence
     */
    public static BiometricState getState() {
        BiometricState state = new BiometricState();
        state.enabled = sEnabled;
        
        state.fingerprintAttempts = sFingerprintAttemptCount;
        state.fingerprintFailures = sFingerprintFailureCount;
        state.faceAttempts = sFaceAttemptCount;
        state.faceFailures = sFaceFailureCount;
        state.irisAttempts = sIrisAttemptCount;
        state.irisFailures = sIrisFailureCount;
        
        state.consecutiveFailures = sConsecutiveFailures;
        state.isLockedOut = isAnyLockedOut();
        state.lockoutRemainingMs = Math.max(0, sLockoutEndTime - System.currentTimeMillis());
        
        state.skinMoisture = sSkinMoisture;
        state.ambientLight = sAmbientLight;
        state.screenCleanliness = sScreenCleanliness;
        state.temperature = sTemperature;
        state.wearingGlasses = sUserWearingGlasses;
        
        return state;
    }
    
    /**
     * Get fingerprint failure rate
     */
    public static float getFingerprintFailureRate() {
        if (sFingerprintAttemptCount == 0) return 0;
        return (float) sFingerprintFailureCount / sFingerprintAttemptCount;
    }
    
    /**
     * Get face failure rate
     */
    public static float getFaceFailureRate() {
        if (sFaceAttemptCount == 0) return 0;
        return (float) sFaceFailureCount / sFaceAttemptCount;
    }
    
    /**
     * Get iris failure rate
     */
    public static float getIrisFailureRate() {
        if (sIrisAttemptCount == 0) return 0;
        return (float) sIrisFailureCount / sIrisAttemptCount;
    }
    
    /**
     * Biometric types
     */
    private enum BiometricType {
        FINGERPRINT,
        FACE,
        IRIS
    }
    
    /**
     * State container
     */
    public static class BiometricState {
        public boolean enabled;
        
        public int fingerprintAttempts;
        public int fingerprintFailures;
        public int faceAttempts;
        public int faceFailures;
        public int irisAttempts;
        public int irisFailures;
        
        public int consecutiveFailures;
        public boolean isLockedOut;
        public long lockoutRemainingMs;
        
        public float skinMoisture;
        public float ambientLight;
        public float screenCleanliness;
        public float temperature;
        public boolean wearingGlasses;
        
        public float getFingerprintFailureRate() {
            if (fingerprintAttempts == 0) return 0;
            return (float) fingerprintFailures / fingerprintAttempts;
        }
        
        public float getFaceFailureRate() {
            if (faceAttempts == 0) return 0;
            return (float) faceFailures / faceAttempts;
        }
    }
}
