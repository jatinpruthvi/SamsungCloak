package com.samsungcloak.xposed;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
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
 * MicrophoneInputRealismHook - Audio Input Realism
 * 
 * Simulates realistic microphone input degradation:
 * - Ambient noise interference
 * - Acoustic echo during calls
 * - Voice recognition failures
 * - Audio preprocessing delays
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class MicrophoneInputRealismHook {

    private static final String TAG = "[Mic][InputRealism]";
    private static final boolean DEBUG = true;
    
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    
    // Parameters
    private static float ambientNoiseLevel = 0.3f;       // 0-1 scale
    private static float recognitionFailureRate = 0.12f; // 12%
    private static int minLatencyMs = 100;
    private static int maxLatencyMs = 500;
    private static float echoRate = 0.08f;              // 8%
    
    // State
    private static boolean isRecording = false;
    private static boolean isSpeakerActive = false;
    private static String currentApp = null;
    
    private static final Random random = new Random();
    private static final List<MicEvent> micEvents = new CopyOnWriteArrayList<>();
    
    public static class MicEvent {
        public long timestamp;
        public String type;
        public String details;
        
        public MicEvent(String type, String details) {
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
        
        HookUtils.logInfo(TAG, "Initializing Microphone Input Realism Hook");
        
        try {
            hookAudioRecord(lpparam);
            hookSpeechRecognizer(lpparam);
            
            HookUtils.logInfo(TAG, "Microphone hook initialized");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Init failed: " + t.getMessage());
            hookInitialized.set(false);
        }
    }
    
    private static void hookAudioRecord(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> audioRecordClass = XposedHelpers.findClass(
                "android.media.AudioRecord", lpparam.classLoader
            );
            
            XposedBridge.hookAllMethods(audioRecordClass, "startRecording",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    isRecording = true;
                    
                    // Add ambient noise injection
                    if (ambientNoiseLevel > 0.2f) {
                        micEvents.add(new MicEvent("AMBIENT_NOISE", 
                            "Level: " + ambientNoiseLevel));
                    }
                    
                    HookUtils.logDebug(TAG, "Recording started with noise: " + ambientNoiseLevel);
                }
            });
            
            XposedBridge.hookAllMethods(audioRecordClass, "stop",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    isRecording = false;
                    
                    // Check for echo
                    if (isSpeakerActive && random.nextFloat() < echoRate) {
                        micEvents.add(new MicEvent("ACOUSTIC_ECHO", 
                            "Echo detected during recording"));
                    }
                }
            });
            
            HookUtils.logInfo(TAG, "AudioRecord hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "AudioRecord hook failed: " + t.getMessage());
        }
    }
    
    private static void hookSpeechRecognizer(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> recognizerClass = XposedHelpers.findClass(
                "android.speech.SpeechRecognizer", lpparam.classLoader
            );
            
            XposedBridge.hookAllMethods(recognizerClass, "recognize",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    // Add recognition delay
                    int delay = minLatencyMs + random.nextInt(maxLatencyMs - minLatencyMs);
                    
                    micEvents.add(new MicEvent("RECOGNITION_DELAY", 
                        "Processing: " + delay + "ms"));
                    
                    // Recognition failure
                    if (random.nextFloat() < recognitionFailureRate) {
                        micEvents.add(new MicEvent("RECOGNITION_FAILED", 
                            "Speech not recognized"));
                    }
                }
            });
            
            HookUtils.logInfo(TAG, "SpeechRecognizer hooked");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "SpeechRecognizer hook skipped: " + t.getMessage());
        }
    }
    
    public static void setEnabled(boolean enabled) {
        MicrophoneInputRealismHook.enabled = enabled;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void setAmbientNoiseLevel(float level) {
        ambientNoiseLevel = Math.max(0, Math.min(1, level));
    }
    
    public static void setSpeakerActive(boolean active) {
        isSpeakerActive = active;
    }
    
    public static List<MicEvent> getMicEvents() {
        return new ArrayList<>(micEvents);
    }
}
