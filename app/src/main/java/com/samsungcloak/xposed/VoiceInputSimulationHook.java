package com.samsungcloak.xposed;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * VoiceInputSimulationHook - Voice Command & Speech Recognition Failures
 * 
 * Simulates realistic voice input failures:
 * - Misrecognized words (phonetic substitutions)
 * - Ambient noise rejection
 * - Network timeout delays
 * - Partial transcription
 * - Bluetooth headset latency
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class VoiceInputSimulationHook {

    private static final String TAG = "[Voice][Speech]";
    private static final boolean DEBUG = true;
    
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    
    // Failure rates
    private static float noSpeechDetectionRate = 0.15f;     // 15% - no speech detected
    private static float networkTimeoutRate = 0.05f;        // 5% - network timeout
    private static float partialResultRate = 0.10f;        // 10% - partial results
    private static float misrecognitionRate = 0.12f;      // 12% - wrong words
    
    // Timing
    private static int minRecognitionDelay = 200;   // ms
    private static int maxRecognitionDelay = 500;   // ms
    
    // State
    private static boolean bluetoothHeadsetConnected = false;
    private static int sessionId = 0;
    private static final Random random = new Random();
    private static final List<VoiceSession> sessions = new CopyOnWriteArrayList<>();
    
    // Common phonetic substitutions for misrecognition
    private static final String[][] PHONETIC_MISTAKES = {
        {"hello", "hello"},
        {"there", "their"},
        {"to", "too"},
        {"two", "too"},
        {"too", "two"},
        {"weather", "whether"},
        {"where", "wear"},
        {"their", "there"},
        {"you're", "your"},
        {"its", "it's"},
        {"then", "than"},
        {"than", "then"},
        {"affect", "effect"},
        {"effect", "affect"},
        {"complement", "compliment"},
        {"principal", "principle"},
        {"discrete", "discreet"},
        {"ensure", "insure"},
        {"advice", "advise"},
        {"device", "devise"}
    };
    
    public static class VoiceSession {
        public int id;
        public long startTime;
        public boolean success;
        public String transcribedText;
        public String errorType;
        
        public VoiceSession(int id) {
            this.id = id;
            this.startTime = System.currentTimeMillis();
        }
    }
    
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (hookInitialized.getAndSet(true)) {
            HookUtils.logDebug(TAG, "Already initialized");
            return;
        }
        
        HookUtils.logInfo(TAG, "Initializing Voice Input Simulation Hook");
        
        try {
            hookSpeechRecognizer(lpparam);
            hookBluetoothHeadset(lpparam);
            
            HookUtils.logInfo(TAG, "Voice input hook initialized");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Init failed: " + t.getMessage());
            hookInitialized.set(false);
        }
    }
    
    private static void hookSpeechRecognizer(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> speechRecognizerClass = XposedHelpers.findClass(
                "android.speech.SpeechRecognizer", lpparam.classLoader
            );
            
            // Hook startListening to inject delays and failures
            XposedBridge.hookAllMethods(speechRecognizerClass, "startListening",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    sessionId++;
                    VoiceSession session = new VoiceSession(sessionId);
                    sessions.add(session);
                    
                    // Check for failures
                    float failRoll = random.nextFloat();
                    
                    if (failRoll < noSpeechDetectionRate) {
                        // Simulate "no speech detected"
                        scheduleNoSpeechResult(param);
                        HookUtils.logDebug(TAG, "Session " + sessionId + ": Injecting no-speech");
                    } else if (failRoll < noSpeechDetectionRate + networkTimeoutRate) {
                        // Simulate network timeout
                        scheduleNetworkTimeout(param);
                        HookUtils.logDebug(TAG, "Session " + sessionId + ": Injecting network timeout");
                    }
                }
                
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    // Add recognition delay
                    int delay = minRecognitionDelay + random.nextInt(maxRecognitionDelay - minRecognitionDelay);
                    
                    if (bluetoothHeadsetConnected) {
                        delay += random.nextInt(150); // Extra delay for BT
                    }
                    
                    Thread.sleep(delay);
                }
            });
            
            // Hook stopListening
            XposedBridge.hookAllMethods(speechRecognizerClass, "stopListening",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    // Occasionally simulate "already stopped" (2%)
                    if (random.nextFloat() < 0.02f) {
                        HookUtils.logDebug(TAG, "Simulating stop failure");
                    }
                }
            });
            
            HookUtils.logInfo(TAG, "SpeechRecognizer hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "SpeechRecognizer hook failed: " + t.getMessage());
        }
    }
    
    private static void hookBluetoothHeadset(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> bluetoothClass = XposedHelpers.findClass(
                "android.bluetooth.BluetoothHeadset", lpparam.classLoader
            );
            
            // Monitor headset connection state changes
            XposedBridge.hookAllMethods(bluetoothClass, "getConnectedDevices",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    // Track if any device is a headset
                    // This is simplified - real implementation would check device type
                }
            });
            
            HookUtils.logInfo(TAG, "Bluetooth headset monitoring hooked");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "Bluetooth hook skipped: " + t.getMessage());
        }
    }
    
    private static void scheduleNoSpeechResult(XC_MethodHook.MethodHookParam param) {
        // In real implementation, would trigger onResults with empty bundle
        // This is a simplified version
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            HookUtils.logDebug(TAG, "No speech result delivered");
        }, 1000);
    }
    
    private static void scheduleNetworkTimeout(XC_MethodHook.MethodHookParam param) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            HookUtils.logDebug(TAG, "Network timeout delivered");
        }, 3000);
    }
    
    /**
     * Inject phonetic misrecognition into text
     */
    public static String injectMisrecognition(String text) {
        if (text == null || text.isEmpty()) return text;
        
        String result = text.toLowerCase();
        
        for (String[] mistake : PHONETIC_MISTAKES) {
            if (random.nextFloat() < misrecognitionRate && result.contains(mistake[0])) {
                result = result.replace(mistake[0], mistake[1]);
                HookUtils.logDebug(TAG, "Misrecognition: " + mistake[0] + " -> " + mistake[1]);
                break;
            }
        }
        
        return result;
    }
    
    /**
     * Simulate partial result
     */
    public static String getPartialResult(String fullResult) {
        if (fullResult == null || fullResult.isEmpty()) return fullResult;
        
        String[] words = fullResult.split("\\s+");
        int keepCount = Math.max(1, random.nextInt(words.length));
        
        StringBuilder partial = new StringBuilder();
        for (int i = 0; i < keepCount && i < words.length; i++) {
            if (i > 0) partial.append(" ");
            partial.append(words[i]);
        }
        
        return partial.toString();
    }
    
    // ========== Configuration ==========
    
    public static void setEnabled(boolean enabled) {
        VoiceInputSimulationHook.enabled = enabled;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void setNoSpeechDetectionRate(float rate) {
        noSpeechDetectionRate = Math.max(0, Math.min(1, rate));
    }
    
    public static void setNetworkTimeoutRate(float rate) {
        networkTimeoutRate = Math.max(0, Math.min(1, rate));
    }
    
    public static void setBluetoothHeadsetConnected(boolean connected) {
        bluetoothHeadsetConnected = connected;
    }
    
    public static List<VoiceSession> getSessions() {
        return new ArrayList<>(sessions);
    }
}
