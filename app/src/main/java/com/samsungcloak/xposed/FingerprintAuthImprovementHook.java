package com.samsungcloak.xposed;

import android.hardware.fingerprint.FingerprintManager;
import android.os.Handler;
import android.os.Looper;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * FingerprintAuthImprovementHook - Fingerprint Authentication Enhancement
 * 
 * IMPROVEMENT over BiometricFailureHook:
 * - Added sensor dirty detection simulation
 * - Temperature-based degradation (cold fingers)
 * - Angle variation for off-center placements
 * - Cross-hook integration with ThermalHook for cold-weather scenarios
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class FingerprintAuthImprovementHook {

    private static final String TAG = "[Fingerprint][Improvement]";
    private static final boolean DEBUG = true;
    
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    
    // Enhancement parameters
    private static float sensorDirtyRate = 0.08f;     // 8%
    private static float coldFingerFailureRate = 0.25f; // 25% when cold
    private static float angleVariationRate = 0.15f;   // 15%
    private static float currentTemperature = 25f;     // Celsius
    private static float coldThreshold = 10f;          // Below this = cold finger
    
    // Original parameters (from BiometricFailureHook)
    private static float baseFailureRate = 0.10f;
    private static float partialPrintRate = 0.05f;
    
    // State
    private static boolean isSensorDirty = false;
    private static int authAttempts = 0;
    private static int authFailures = 0;
    
    private static final Random random = new Random();
    private static final List<FingerprintEvent> fpEvents = new CopyOnWriteArrayList<>();
    
    public static class FingerprintEvent {
        public long timestamp;
        public String type;
        public String details;
        
        public FingerprintEvent(String type, String details) {
            this.timestamp = System.currentTimeMillis();
            this.type = type;
            this.details = details;
        }
    }
    
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (hookInitialized.getAndSet(true)) {
            HookUtils.logDebug(TAG, "Already initialized");
            return;
        }
        
        HookUtils.logInfo(TAG, "Initializing Fingerprint Auth Improvement Hook");
        
        try {
            hookFingerprintManager(lpparam);
            hookFingerprintSensor(lpparam);
            
            HookUtils.logInfo(TAG, "Fingerprint improvement hook initialized");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Init failed: " + t.getMessage());
            hookInitialized.set(false);
        }
    }
    
    private static void hookFingerprintManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> fpManagerClass = XposedHelpers.findClass(
                "android.hardware.fingerprint.FingerprintManager", lpparam.classLoader
            );
            
            XposedBridge.hookAllMethods(fpManagerClass, "authenticate",
                new XC_MethodHook() {
                @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    authAttempts++;
                    
                    // Sensor dirty check
                    if (random.nextFloat() < sensorDirtyRate) {
                        isSensorDirty = true;
                        fpEvents.add(new FingerprintEvent("SENSOR_DIRTY", 
                            "Fingerprint sensor needs cleaning"));
                    }
                    
                    // Temperature-based failure (cross-hook with ThermalHook)
                    if (currentTemperature < coldThreshold) {
                        if (random.nextFloat() < coldFingerFailureRate) {
                            fpEvents.add(new FingerprintEvent("COLD_FINGER", 
                                "Finger too cold: " + currentTemperature + "°C"));
                            authFailures++;
                            return;
                        }
                    }
                    
                    // Base failure rate
                    if (random.nextFloat() < baseFailureRate) {
                        fpEvents.add(new FingerprintEvent("AUTH_FAILED", 
                            "Fingerprint not recognized"));
                        authFailures++;
                    }
                    
                    // Angle variation
                    if (random.nextFloat() < angleVariationRate) {
                        fpEvents.add(new FingerprintEvent("ANGLE_VARIATION", 
                            "Off-center placement detected"));
                    }
                    
                    HookUtils.logDebug(TAG, "Fingerprint auth attempt #" + authAttempts);
                }
            });
            
            HookUtils.logInfo(TAG, "FingerprintManager hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "FingerprintManager hook failed: " + t.getMessage());
        }
    }
    
    private static void hookFingerprintSensor(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> fingerprintClass = XposedHelpers.findClass(
                "android.hardware.fingerprint.Fingerprint", lpparam.classLoader
            );
            
            XposedBridge.hookAllMethods(fingerprintClass, "getCharacteristics",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    // Partial print detection
                    if (random.nextFloat() < partialPrintRate) {
                        fpEvents.add(new FingerprintEvent("PARTIAL_PRINT", 
                            "Partial fingerprint detected"));
                    }
                    
                    // Sensor properties check
                    fpEvents.add(new FingerprintEvent("SENSOR_CHECK", 
                        "Sensor status: " + (isSensorDirty ? "dirty" : "clean"));
                }
            });
            
            HookUtils.logInfo(TAG, "Fingerprint sensor hooked");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "Fingerprint sensor hook skipped: " + t.getMessage());
        }
    }
    
    public static void setEnabled(boolean enabled) {
        FingerprintAuthImprovementHook.enabled = enabled;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    // Cross-hook setters
    public static void setTemperature(float temp) {
        currentTemperature = temp;
    }
    
    public static void setSensorDirty(boolean dirty) {
        isSensorDirty = dirty;
    }
    
    public static void setColdFingerFailureRate(float rate) {
        coldFingerFailureRate = Math.max(0, Math.min(1, rate));
    }
    
    public static int getAuthAttempts() {
        return authAttempts;
    }
    
    public static int getAuthFailures() {
        return authFailures;
    }
    
    public static List<FingerprintEvent> getFingerprintEvents() {
        return new ArrayList<>(fpEvents);
    }
}
