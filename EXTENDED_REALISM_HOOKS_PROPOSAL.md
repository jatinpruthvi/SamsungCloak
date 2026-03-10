# Extended Realism Hooks Proposal for Samsung Galaxy A12 (SM-A125U)

## Executive Summary

This document proposes **6 new realism hooks** that address aspects not yet covered by the existing 12 hooks in the framework.

---

## Proposed New Hooks

### Hook 1: ProximitySensorCallModeHook

**Status**: NEW (Not Implemented)

**Category**: Environmental Context / Hardware State Simulation

#### Description
Simulates proximity sensor behavior during call scenarios, including pocket dialing detection, face proximity during video calls, and accidental touch blocking.

#### Novel Dimensions
1. **Pocket/Body Proximity Detection** - Simulates proximity sensor triggers when device is in pocket
2. **Call Mode State Machine** - Models Android call state transitions
3. **Accidental Touch Blocking** - Simulates screen-off when proximity detected
4. **Video Call Face Detection** - Proximity changes during video calls

#### Java Implementation

```java
package com.samsungcloak.xposed;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Build;
import android.telephony.TelephonyManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;

/**
 * ProximitySensorCallModeHook - Proximity Sensor & Call Mode Simulation
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class ProximitySensorCallModeHook {

    private static final String TAG = "[Hardware][Proximity]";
    private static final boolean DEBUG = true;
    private static final ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new Random());
    
    private static boolean enabled = true;
    
    // Proximity simulation configuration
    private static boolean proximitySimulationEnabled = true;
    private static double proximityNearProbability = 0.65;
    private static long proximityNearDelayMs = 200;
    private static long proximityFarDelayMs = 500;
    
    // Call state simulation
    private static boolean callStateSimulationEnabled = true;
    private static int currentCallState = TelephonyManager.CALL_STATE_IDLE;
    private static boolean isInCall = false;
    
    // Pocket detection
    private static boolean pocketDetectionEnabled = true;
    private static boolean isInPocket = false;
    
    // Video call mode
    private static boolean videoCallModeEnabled = true;
    private static boolean isInVideoCall = false;
    
    // Sensor values
    private static final float PROXIMITY_NEAR = 0.0f;
    private static final float PROXIMITY_FAR = 5.0f;
    
    // State tracking
    private static boolean lastProximityState = false;
    
    public enum CallScenario {
        INCOMING_CALL,
        OUTGOING_CALL,
        ACTIVE_CALL,
        VIDEO_CALL,
        VOIP_CALL,
        NONE
    }
    
    private static CallScenario currentScenario = CallScenario.NONE;
    
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        HookUtils.logInfo(TAG, "Initializing Proximity Sensor Call Mode Hook");
        
        try {
            hookTelephonyCallState(lpparam);
            hookProximitySensorRegistration(lpparam);
            hookSensorEventDelivery(lpparam);
            hookScreenStateDetection(lpparam);
            
            HookUtils.logInfo(TAG, "Proximity Sensor Call Mode Hook initialized successfully");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize: " + e.getMessage());
        }
    }
    
    private static void hookTelephonyCallState(XC_LoadPackage.LoadPackageParam lpparam) {
        final Class<?> telephonyManagerClass = XposedHelpers.findClassIfExists(
            "android.telephony.TelephonyManager", lpparam.classLoader);
            
        if (telephonyManagerClass == null) {
            HookUtils.logError(TAG, "TelephonyManager class not found");
            return;
        }
        
        XposedBridge.hookAllMethods(telephonyManagerClass, "getCallState", 
            new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || !callStateSimulationEnabled) {
                        return;
                    }
                    
                    if (isInCall && currentCallState != TelephonyManager.CALL_STATE_IDLE) {
                        param.setResult(currentCallState);
                    }
                }
            });
    }
    
    private static void hookProximitySensorRegistration(XC_LoadPackage.LoadPackageParam lpparam) {
        final Class<?> sensorManagerClass = XposedHelpers.findClassIfExists(
            "android.hardware.SensorManager", lpparam.classLoader);
            
        if (sensorManagerClass == null) return;
        
        XposedBridge.hookAllMethods(sensorManagerClass, "getDefaultSensor",
            new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || !proximitySimulationEnabled) {
                        return;
                    }
                    
                    int sensorType = (int) param.args[0];
                    
                    if (sensorType == Sensor.TYPE_PROXIMITY) {
                        HookUtils.logDebug(TAG, "Proximity sensor requested");
                    }
                }
            });
    }
    
    private static void hookSensorEventDelivery(XC_LoadPackage.LoadPackageParam lpparam) {
        final Class<?> sensorEventClass = XposedHelpers.findClassIfExists(
            "android.hardware.SensorEvent", lpparam.classLoader);
            
        if (sensorEventClass == null) return;
        
        XposedBridge.hookAllConstructors(sensorEventClass,
            new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || !proximitySimulationEnabled) {
                        return;
                    }
                    
                    if (isInCall || isInPocket) {
                        boolean shouldBeNear = shouldSimulateNearState();
                        
                        if (shouldBeNear) {
                            try {
                                XposedHelpers.setFloatField(param.thisObject, "values[0]", PROXIMITY_NEAR);
                            } catch (Exception e) {
                                // Values array may have different structure
                            }
                        }
                    }
                }
            });
    }
    
    private static void hookScreenStateDetection(XC_LoadPackage.LoadPackageParam lpparam) {
        final Class<?> powerManagerClass = XposedHelpers.findClassIfExists(
            "android.os.PowerManager", lpparam.classLoader);
            
        if (powerManagerClass == null) return;
        
        XposedBridge.hookAllMethods(powerManagerClass, "isScreenOn",
            new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    boolean defaultResult = (boolean) param.getResult();
                    
                    if (isInCall && lastProximityState) {
                        param.setResult(false);
                    }
                }
            });
    }
    
    private static boolean shouldSimulateNearState() {
        if (isInCall) {
            return random.get().nextDouble() < proximityNearProbability;
        } else if (isInPocket) {
            return true;
        } else if (isInVideoCall) {
            return random.get().nextDouble() < 0.4;
        }
        return false;
    }
    
    // Public API for call simulation
    public static void simulateIncomingCall() {
        isInCall = true;
        currentCallState = TelephonyManager.CALL_STATE_RINGING;
        currentScenario = CallScenario.INCOMING_CALL;
        HookUtils.logInfo(TAG, "Simulating incoming call");
    }
    
    public static void simulateCallEnd() {
        isInCall = false;
        currentCallState = TelephonyManager.CALL_STATE_IDLE;
        currentScenario = CallScenario.NONE;
        HookUtils.logInfo(TAG, "Call ended");
    }
    
    public static void simulatePocketEntry() {
        isInPocket = true;
        HookUtils.logInfo(TAG, "Device placed in pocket");
    }
    
    public static void simulatePocketRemoval() {
        isInPocket = false;
        HookUtils.logInfo(TAG, "Device removed from pocket");
    }
    
    public static void setEnabled(boolean enable) {
        enabled = enable;
    }
}
```

---

### Hook 2: VoiceInputImperfectionHook

**Status**: NEW (Not Implemented)

**Category**: Accessibility / Voice Input Simulation

#### Description
Simulates voice input imperfections including speech recognition delays, ambient noise interference, recognition failures, and voice command timeout scenarios.

#### Novel Dimensions
1. **Speech Recognition Latency** - Variable delay between speech and text display
2. **Recognition Failure Simulation** - Failed voice commands requiring retry
3. **Ambient Noise Interference** - Background noise affecting voice input quality
4. **Voice Command Timeout** - Commands timing out when user pauses too long
5. **Hotword Detection Variability** - "OK Google" detection failures

#### Java Implementation

```java
package com.samsungcloak.xposed;

import android.content.Context;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;

/**
 * VoiceInputImperfectionHook - Voice Input Realism Simulation
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class VoiceInputImperfectionHook {

    private static final String TAG = "[Accessibility][Voice]";
    private static final boolean DEBUG = true;
    private static final ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new Random());
    
    private static boolean enabled = true;
    
    // Recognition delay configuration
    private static boolean recognitionDelayEnabled = true;
    private static long baseRecognitionDelayMs = 800;
    private static long recognitionDelayVarianceMs = 600;
    
    // Recognition failure configuration  
    private static boolean recognitionFailureEnabled = true;
    private static double failureProbability = 0.15;
    
    // Ambient noise impact
    private static boolean ambientNoiseImpactEnabled = true;
    private static double ambientNoiseLevel = 0.5;
    private static double noiseCausedFailureProbability = 0.35;
    
    // Timeout configuration
    private static boolean timeoutEnabled = true;
    private static long voiceCommandTimeoutMs = 5000;
    private static double timeoutProbability = 0.08;
    
    // Hotword detection
    private static boolean hotwordDetectionEnabled = true;
    private static double hotwordMissProbability = 0.12;
    
    // State tracking
    private static boolean isListening = false;
    private static long speechStartTime = 0;
    
    public enum VoiceInputQuality {
        EXCELLENT,
        GOOD,
        MODERATE,
        POOR,
        FAILED
    }
    
    private static VoiceInputQuality currentQuality = VoiceInputQuality.GOOD;
    
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        HookUtils.logInfo(TAG, "Initializing Voice Input Imperfection Hook");
        
        try {
            hookSpeechRecognizerCreation(lpparam);
            hookRecognitionListener(lpparam);
            hookStartListening(lpparam);
            hookAudioInputProcessing(lpparam);
            
            HookUtils.logInfo(TAG, "Voice Input Imperfection Hook initialized");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize: " + e.getMessage());
        }
    }
    
    private static void hookSpeechRecognizerCreation(XC_LoadPackage.LoadPackageParam lpparam) {
        final Class<?> speechRecognizerClass = XposedHelpers.findClassIfExists(
            "android.speech.SpeechRecognizer", lpparam.classLoader);
            
        if (speechRecognizerClass == null) {
            HookUtils.logError(TAG, "SpeechRecognizer class not found");
            return;
        }
        
        XposedBridge.hookAllMethods(speechRecognizerClass, "createSpeechRecognizer",
            new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    Context context = (Context) param.args[0];
                    HookUtils.logDebug(TAG, "SpeechRecognizer created for context: " + context);
                }
            });
    }
    
    private static void hookRecognitionListener(XC_LoadPackage.LoadPackageParam lpparam) {
        final Class<?> recognitionListenerClass = XposedHelpers.findClassIfExists(
            "android.speech.RecognitionListener", lpparam.classLoader);
            
        if (recognitionListenerClass == null) return;
        
        XposedBridge.hookAllMethods(recognitionListenerClass, "onReadyForSpeech",
            new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || !recognitionDelayEnabled) return;
                    
                    isListening = true;
                    speechStartTime = System.currentTimeMillis();
                    updateVoiceInputQuality();
                }
            });
        
        XposedBridge.hookAllMethods(recognitionListenerClass, "onResults",
            new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    if (shouldSimulateFailure()) {
                        param.setResult(null);
                        triggerRecognitionError(param.thisObject, 
                            SpeechRecognizer.ERROR_AUDIO);
                        return;
                    }
                    
                    if (recognitionDelayEnabled) {
                        long delay = calculateRecognitionDelay();
                        Thread.sleep(delay);
                    }
                }
            });
        
        XposedBridge.hookAllMethods(recognitionListenerClass, "onError",
            new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    int error = (int) param.getResult();
                    
                    if (error == SpeechRecognizer.ERROR_NO_MATCH && 
                        random.get().nextDouble() < 0.3) {
                        HookUtils.logDebug(TAG, "Converting NO_MATCH to partial match");
                    }
                }
            });
    }
    
    private static void hookStartListening(XC_LoadPackage.LoadPackageParam lpparam) {
        final Class<?> speechRecognizerClass = XposedHelpers.findClassIfExists(
            "android.speech.SpeechRecognizer", lpparam.classLoader);
            
        if (speechRecognizerClass == null) return;
        
        XposedBridge.hookAllMethods(speechRecognizerClass, "startListening",
            new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    if (hotwordDetectionEnabled && 
                        random.get().nextDouble() < hotwordMissProbability) {
                        HookUtils.logDebug(TAG, "Simulating hotword miss");
                        param.setResult(null);
                    }
                }
            });
    }
    
    private static void hookAudioInputProcessing(XC_LoadPackage.LoadPackageParam lpparam) {
        final Class<?> audioRecordClass = XposedHelpers.findClassIfExists(
            "android.media.AudioRecord", lpparam.classLoader);
            
        if (audioRecordClass == null) return;
        
        XposedBridge.hookAllMethods(audioRecordClass, "read",
            new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || !ambientNoiseImpactEnabled) return;
                    
                    if (currentQuality == VoiceInputQuality.POOR ||
                        currentQuality == VoiceInputQuality.FAILED) {
                        injectAudioNoise(param);
                    }
                }
            });
    }
    
    private static void updateVoiceInputQuality() {
        double qualityRand = random.get().nextDouble();
        
        if (ambientNoiseLevel < 0.2) {
            currentQuality = VoiceInputQuality.EXCELLENT;
        } else if (ambientNoiseLevel < 0.4) {
            currentQuality = qualityRand > 0.3 ? VoiceInputQuality.GOOD : VoiceInputQuality.MODERATE;
        } else if (ambientNoiseLevel < 0.7) {
            currentQuality = qualityRand > 0.5 ? VoiceInputQuality.MODERATE : VoiceInputQuality.POOR;
        } else {
            currentQuality = VoiceInputQuality.POOR;
        }
        
        HookUtils.logDebug(TAG, "Voice input quality: " + currentQuality);
    }
    
    private static boolean shouldSimulateFailure() {
        double failureChance = failureProbability;
        failureChance += ambientNoiseLevel * noiseCausedFailureProbability;
        
        if (timeoutEnabled && isListening) {
            long listeningDuration = System.currentTimeMillis() - speechStartTime;
            if (listeningDuration > voiceCommandTimeoutMs) {
                failureChance += timeoutProbability;
            }
        }
        
        return random.get().nextDouble() < failureChance;
    }
    
    private static long calculateRecognitionDelay() {
        long baseDelay = baseRecognitionDelayMs;
        long variance = (long) ((random.get().nextDouble() - 0.5) * 2 * recognitionDelayVarianceMs);
        
        if (currentQuality == VoiceInputQuality.POOR) {
            baseDelay += 400;
        }
        
        return Math.max(100, baseDelay + variance);
    }
    
    private static void injectAudioNoise(MethodHookParam param) {
        Object buffer = param.getResult();
        if (buffer instanceof short[]) {
            short[] audioData = (short[]) buffer;
            double noiseLevel = currentQuality == VoiceInputQuality.FAILED ? 0.5 : 0.2;
            
            for (int i = 0; i < audioData.length; i += 10) {
                short noise = (short) (random.get().nextDouble() * noiseLevel * Short.MAX_VALUE);
                audioData[i] = (short) (audioData[i] + noise);
            }
        }
    }
    
    private static void triggerRecognitionError(Object listener, int errorCode) {
        try {
            XposedHelpers.callMethod(listener, "onError", errorCode);
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to trigger error: " + e.getMessage());
        }
    }
    
    public static void setAmbientNoiseLevel(double level) {
        ambientNoiseLevel = Math.max(0.0, Math.min(1.0, level));
    }
    
    public static void setEnabled(boolean enable) {
        enabled = enable;
    }
}
```

---

### Hook 3: ScreenReaderInteractionHook

**Status**: NEW (Not Implemented)

**Category**: Accessibility Simulation

#### Description
Simulates TalkBack/screen reader behavior including navigation patterns, focus changes, announcement timing, and element traversal characteristics.

#### Novel Dimensions
1. **TalkBack Navigation Patterns** - Simulates focus traversal
2. **Announcement Timing** - Variable delay between actions and spoken feedback
3. **Element Traversal Speed** - Fast vs. slow navigation
4. **Focus Search Delays** - Time to find focusable elements
5. **Touch Exploration Mode** - Simulates touch exploration

---

### Hook 4: HapticFeedbackImperfectionHook

**Status**: NEW (Not Implemented)

**Category**: Hardware Simulation

#### Description
Simulates haptic feedback imperfections including motor inertia, battery voltage effects, aging motor characteristics, and surface damping.

#### Novel Dimensions
1. **Motor Inertia** - Delay in vibration start/stop
2. **Battery Voltage Effect** - Weak battery reduces vibration
3. **Aging Motor** - Reduced amplitude over time
4. **Surface Damping** - Different vibration on soft/hard surfaces

---

### Hook 5: GazeDirectionImperfectionHook

**Status**: NEW (Not Implemented)

**Category**: User Behavior

#### Description
Simulates user gaze patterns including attention focus zones, reading scan patterns, visual search behavior, and attention drift.

#### Novel Dimensions
1. **Attention Focus Zones** - Screen areas receiving most attention
2. **Reading Scan Pattern** - F-shaped reading pattern
3. **Attention Drift** - Decreasing attention over time
4. **Touch-Gaze Offset** - Gaze leads touch by ~80px

---

### Hook 6: DeviceGripOrientationHook

**Status**: EXTENSION (Improves existing GripHandDominanceHook)

**Category**: Physical Interaction

#### Validation Plan for Existing Hook
The existing `GripHandDominanceHook` provides basic hand preference. To validate its fidelity:

1. **Test Procedure**: Record 50 users' natural grip patterns
2. **Metrics**: Compare detected hand preference vs. self-reported
3. **Expected Accuracy**: >85% correct detection

#### Suggested Improvements
1. Add orientation-aware grip changes during walking
2. Implement grip shift detection during long typing sessions
3. Add one-handed reach zone limitations

#### Integration with Other Hooks
- **With SensorFusionCoherenceHook**: When walking, device orientation drifts
- **With MechanicalMicroErrorHook**: Tight grip increases touch accuracy

---

## Summary

| Hook | Category | Status | Complexity |
|------|----------|--------|-------------|
| ProximitySensorCallModeHook | Hardware State | NEW | Medium |
| VoiceInputImperfectionHook | Accessibility | NEW | Medium-High |
| ScreenReaderInteractionHook | Accessibility | NEW | Medium |
| HapticFeedbackImperfectionHook | Hardware | NEW | Low-Medium |
| GazeDirectionImperfectionHook | User Behavior | NEW | High |
| DeviceGripOrientationHook | Physical | EXTENSION | Medium |

All hooks are toggleable via SharedPreferences and designed for SM-A125U (Android 10/11).
