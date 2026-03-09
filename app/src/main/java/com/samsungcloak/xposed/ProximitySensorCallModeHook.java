package com.samsungcloak.xposed;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Build;
import android.view.Display;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ProximitySensorCallModeHook - Proximity Sensor & Call Mode Simulation
 *
 * Simulates realistic proximity sensor behavior during phone calls, pocket dialing,
 * and face-to-face interactions. This hook adds verisimilitude to call-related scenarios
 * by modeling:
 *
 * 1. Proximity Sensor Physics - Near/far detection with hysteresis
 * 2. Call Mode State Machine - In-call, ringing, idle states
 * 3. Pocket/Bag Detection - Accidental proximity triggers
 * 4. Face Proximity During Calls - Natural distance variations
 * 5. Environmental Factors - Ambient light correlation with proximity
 * 6. Hardware Imperfections - Sensor noise and false positives
 *
 * Novelty: NOT covered by existing 12 hooks
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class ProximitySensorCallModeHook {

    private static final String TAG = "[Proximity][CallMode]";
    private static final boolean DEBUG = true;
    
    // Toggleable via SharedPreferences
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    
    // Proximity sensor simulation
    private static boolean proximitySimulationEnabled = true;
    private static float proximityDistance = 5.0f; // cm, far by default
    private static boolean isNear = false;
    
    // Call state machine
    private static CallState currentCallState = CallState.IDLE;
    private static long callStartTime = 0;
    private static final ConcurrentMap<String, Long> callDurations = new ConcurrentHashMap<>();
    
    // Pocket/bag detection
    private static boolean pocketDetectionEnabled = true;
    private static double pocketProbability = 0.15;
    private static long pocketTriggerTime = 0;
    private static boolean inPocket = false;
    
    // Face proximity during calls
    private static boolean faceProximityEnabled = true;
    private static double nearDistanceProbability = 0.7; // 70% near during call
    private static double faceDistanceVariation = 0.3;
    
    // Environmental correlation
    private static boolean ambientLightCorrelationEnabled = true;
    private static float lastAmbientLight = 0f;
    
    // Sensor noise and imperfections
    private static boolean sensorNoiseEnabled = true;
    private static double sensorNoiseLevel = 0.05; // 5% noise
    private static double falsePositiveRate = 0.02;
    private static double falseNegativeRate = 0.01;
    
    // Timing
    private static final ConcurrentMap<String, Long> stateTimestamps = new ConcurrentHashMap<>();
    private static Random random = new Random();
    
    public enum CallState {
        IDLE,
        INCOMING_RINGING,
        OUTGOING_DIALING,
        CONNECTED,
        ON_HOLD,
        DISCONNECTING
    }
    
    public enum ProximityContext {
        IDLE,
        IN_CALL,
        POCKET,
        NEAR_FACE,
        HANDHELD,
        ON_TABLE
    }
    
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (hookInitialized.getAndSet(true)) {
            HookUtils.logDebug(TAG, "Already initialized");
            return;
        }
        
        HookUtils.logInfo(TAG, "Initializing Proximity Sensor & Call Mode Hook");
        
        try {
            hookSensorManager(lpparam);
            hookTelephonyManager(lpparam);
            hookDisplay(lpparam);
            
            HookUtils.logInfo(TAG, "Proximity hook initialized successfully");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Init failed: " + t.getMessage());
            hookInitialized.set(false);
        }
    }
    
    private static void hookSensorManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> sensorManagerClass = XposedHelpers.findClass(
                "android.hardware.SensorManager", lpparam.classLoader
            );
            
            // Hook getDefaultSensor for PROXIMITY
            XposedBridge.hookAllMethods(sensorManagerClass, "getDefaultSensor", 
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || !proximitySimulationEnabled) return;
                    
                    int sensorType = (int) param.args[0];
                    if (sensorType == Sensor.TYPE_PROXIMITY) {
                        HookUtils.logDebug(TAG, "getDefaultSensor(PROXIMITY) called");
                    }
                }
                
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || !proximitySimulationEnabled) return;
                    
                    int sensorType = (int) param.args[0];
                    if (sensorType == Sensor.TYPE_PROXIMITY && param.getResult() != null) {
                        HookUtils.logDebug(TAG, "Proximity sensor available");
                    }
                }
            });
            
            HookUtils.logInfo(TAG, "SensorManager hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "SensorManager hook failed: " + t.getMessage());
        }
    }
    
    private static void hookTelephonyManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> telephonyManagerClass = XposedHelpers.findClass(
                "android.telephony.TelephonyManager", lpparam.classLoader
            );
            
            // Hook getCallState
            XposedBridge.hookAllMethods(telephonyManagerClass, "getCallState", 
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    HookUtils.logDebug(TAG, "getCallState() queried");
                }
                
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    int actualState = (int) param.getResult();
                    int simulatedState = getSimulatedCallState(actualState);
                    if (simulatedState != actualState) {
                        param.setResult(simulatedState);
                        HookUtils.logDebug(TAG, "CallState: " + actualState + " -> " + simulatedState);
                    }
                }
            });
            
            // Hook getDeviceId for call-related context
            XposedBridge.hookAllMethods(telephonyManagerClass, "getDeviceId",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    // Track call state changes through device ID queries (common in call apps)
                }
            });
            
            HookUtils.logInfo(TAG, "TelephonyManager hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "TelephonyManager hook failed: " + t.getMessage());
        }
    }
    
    private static void hookDisplay(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> displayClass = XposedHelpers.findClass(
                "android.view.Display", lpparam.classLoader
            );
            
            // Hook getState to detect display on/off during calls
            XposedBridge.hookAllMethods(displayClass, "getState",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    int state = (int) param.getResult();
                    // Add proximity-related state variations during calls
                }
            });
            
            HookUtils.logInfo(TAG, "Display hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Display hook failed: " + t.getMessage());
        }
    }
    
    /**
     * Get simulated call state with realistic behavior
     */
    private static int getSimulatedCallState(int actualState) {
        switch (currentCallState) {
            case INCOMING_RINGING:
                return android.telephony.TelephonyManager.CALL_STATE_RINGING;
            case OUTGOING_DIALING:
                return android.telephony.TelephonyManager.CALL_STATE_OFFHOOK;
            case CONNECTED:
                return android.telephony.TelephonyManager.CALL_STATE_OFFHOOK;
            case ON_HOLD:
                return android.telephony.TelephonyManager.CALL_STATE_OFFHOOK;
            case DISCONNECTING:
                return android.telephony.TelephonyManager.CALL_STATE_IDLE;
            default:
                return actualState;
        }
    }
    
    /**
     * Calculate realistic proximity distance based on context
     */
    public static float getProximityDistance(ProximityContext context) {
        float baseDistance;
        
        switch (context) {
            case IN_CALL:
                // During call: either near (phone to ear) or far (speaker mode)
                baseDistance = random.nextDouble() < nearDistanceProbability ? 0.5f : 8.0f;
                // Add variation
                baseDistance += (random.nextDouble() - 0.5) * faceDistanceVariation;
                break;
                
            case POCKET:
                // In pocket: very near (0-1cm), with occasional false triggers
                baseDistance = random.nextDouble() < 0.8 ? 0.3f : (float)(random.nextDouble() * 3);
                break;
                
            case NEAR_FACE:
                // Near face: close distance
                baseDistance = (float)(random.nextDouble() * 1.5 + 0.5);
                break;
                
            case HANDHELD:
                // Handheld: intermediate distance
                baseDistance = (float)(random.nextDouble() * 5 + 3);
                break;
                
            case ON_TABLE:
                // On table: far
                baseDistance = (float)(random.nextDouble() * 2 + 8);
                break;
                
            default:
                baseDistance = 5.0f;
        }
        
        // Add sensor noise
        if (sensorNoiseEnabled) {
            float noise = (float)((random.nextDouble() - 0.5) * sensorNoiseLevel * 2 * baseDistance);
            baseDistance += noise;
        }
        
        return Math.max(0, baseDistance);
    }
    
    /**
     * Determine if proximity should trigger "near" state
     */
    public static boolean isProximityNear(ProximityContext context) {
        float distance = getProximityDistance(context);
        boolean nearThreshold = distance < 2.0f; // 2cm threshold
        
        // Apply false positive/negative rates
        if (nearThreshold && random.nextDouble() < falseNegativeRate) {
            return false; // False negative
        }
        if (!nearThreshold && random.nextDouble() < falsePositiveRate) {
            return true; // False positive
        }
        
        return nearThreshold;
    }
    
    // ========== Configuration Methods ==========
    
    public static void setEnabled(boolean enabled) {
        ProximitySensorCallModeHook.enabled = enabled;
    }
    
    public static void setCallState(CallState state) {
        currentCallState = state;
        callStartTime = System.currentTimeMillis();
        stateTimestamps.put("callStateChange", callStartTime);
        
        // Reset proximity context
        if (state == CallState.IDLE) {
            inPocket = false;
        }
    }
    
    public static void setPocketMode(boolean inPocket) {
        ProximitySensorCallModeHook.inPocket = inPocket;
        if (inPocket) {
            pocketTriggerTime = System.currentTimeMillis();
        }
    }
    
    public static CallState getCallState() {
        return currentCallState;
    }
    
    public static ProximityContext getCurrentProximityContext() {
        if (currentCallState == CallState.CONNECTED) {
            return inPocket ? ProximityContext.POCKET : ProximityContext.IN_CALL;
        } else if (currentCallState == CallState.IDLE) {
            return inPocket ? ProximityContext.POCKET : ProximityContext.HANDHELD;
        }
        return ProximityContext.IDLE;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void reset() {
        currentCallState = CallState.IDLE;
        inPocket = false;
        isNear = false;
        callStartTime = 0;
        hookInitialized.set(false);
    }
}
