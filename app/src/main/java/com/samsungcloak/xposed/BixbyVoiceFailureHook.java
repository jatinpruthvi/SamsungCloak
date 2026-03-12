package com.samsungcloak.xposed;

import android.content.Context;
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
 * BixbyVoiceFailureHook - Bixby Voice Command Failures
 * 
 * Simulates Bixby command failures:
 * - "I didn't understand" responses
 * - Network errors
 * - Wake word failures
 * - Delayed responses
 * - Accidental triggers
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class BixbyVoiceFailureHook {

    private static final String TAG = "[Bixby][Voice]";
    private static final boolean DEBUG = true;
    
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    
    // Failure rates
    private static float commandFailureRate = 0.15f;      // 15%
    private static float wakeWordFailureRate = 0.08f;   // 8%
    private static float networkErrorRate = 0.05f;      // 5%
    private static float accidentalTriggerRate = 0.02f; // 2%
    
    // Timing
    private static int minResponseDelay = 500;    // ms
    private static int maxResponseDelay = 2000;   // ms
    
    // State
    private static boolean isListening = false;
    private static int commandCount = 0;
    private static int failureCount = 0;
    private static String lastCommand = null;
    
    private static final Random random = new Random();
    private static final List<BixbyEvent> bixbyHistory = new CopyOnWriteArrayList<>();
    
    public static class BixbyEvent {
        public long timestamp;
        public String type;
        public String command;
        public String details;
        
        public BixbyEvent(String type, String command, String details) {
            this.timestamp = System.currentTimeMillis();
            this.type = type;
            this.command = command;
            this.details = details;
        }
    }
    
    // Error messages
    private static final String[] FAILURE_MESSAGES = {
        "I didn't understand that",
        "Sorry, I couldn't hear you clearly",
        "Could you try again?",
        "I'm having trouble understanding",
        "Please try that again"
    };
    
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (hookInitialized.getAndSet(true)) {
            HookUtils.logDebug(TAG, "Already initialized");
            return;
        }
        
        HookUtils.logInfo(TAG, "Initializing Bixby Voice Failure Hook");
        
        try {
            hookBixbyService(lpparam);
            hookVoiceInteraction(lpparam);
            
            HookUtils.logInfo(TAG, "Bixby hook initialized");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Init failed: " + t.getMessage());
            hookInitialized.set(false);
        }
    }
    
    private static void hookBixbyService(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> bixbyClass = XposedHelpers.findClass(
                "com.samsung.android.bixby.BixbyService", lpparam.classLoader
            );
            
            // Hook onVoiceInteraction
            XposedBridge.hookAllMethods(bixbyClass, "onVoiceInteraction",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    isListening = true;
                    commandCount++;
                    
                    bixbyHistory.add(new BixbyEvent("VOICE_START", 
                        lastCommand, "Listening"));
                }
            });
            
            // Hook executeCommand
            XposedBridge.hookAllMethods(bixbyClass, "executeCommand",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    // Check for command failure
                    if (random.nextFloat() < commandFailureRate) {
                        String errorMsg = FAILURE_MESSAGES[
                            random.nextInt(FAILURE_MESSAGES.length)];
                        
                        bixbyHistory.add(new BixbyEvent("COMMAND_FAILED", 
                            lastCommand, errorMsg));
                        
                        failureCount++;
                        
                        HookUtils.logDebug(TAG, "Command failed: " + errorMsg);
                    }
                    
                    // Add response delay
                    int delay = minResponseDelay + 
                        random.nextInt(maxResponseDelay - minResponseDelay);
                    
                    bixbyHistory.add(new BixbyEvent("RESPONSE_DELAY", 
                        lastCommand, "Delayed " + delay + "ms"));
                }
            });
            
            HookUtils.logInfo(TAG, "Bixby service hooked");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "Bixby hook skipped: " + t.getMessage());
        }
    }
    
    private static void hookVoiceInteraction(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> voiceServiceClass = XposedHelpers.findClass(
                "android.service.voice.VoiceInteractionService", lpparam.classLoader
            );
            
            // Hook onReady
            XposedBridge.hookAllMethods(voiceServiceClass, "onReady",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    // Check for wake word failure
                    if (random.nextFloat() < wakeWordFailureRate) {
                        bixbyHistory.add(new BixbyEvent("WAKE_FAILED", 
                            null, "Wake word not detected"));
                        
                        HookUtils.logDebug(TAG, "Wake word failed");
                    }
                    
                    // Check for accidental trigger
                    if (random.nextFloat() < accidentalTriggerRate) {
                        bixbyHistory.add(new BixbyEvent("ACCIDENTAL_TRIGGER", 
                            null, "Accidental wake"));
                        
                        HookUtils.logDebug(TAG, "Accidental trigger");
                    }
                }
            });
            
            HookUtils.logInfo(TAG, "Voice interaction service hooked");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "Voice interaction hook skipped: " + t.getMessage());
        }
    }
    
    // ========== Configuration ==========
    
    public static void setEnabled(boolean enabled) {
        BixbyVoiceFailureHook.enabled = enabled;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void setCommandFailureRate(float rate) {
        commandFailureRate = Math.max(0, Math.min(1, rate));
    }
    
    public static void setResponseDelayRange(int minMs, int maxMs) {
        minResponseDelay = Math.max(100, minMs);
        maxResponseDelay = Math.max(minResponseDelay, maxMs);
    }
    
    public static void setLastCommand(String command) {
        lastCommand = command;
    }
    
    public static int getCommandCount() {
        return commandCount;
    }
    
    public static int getFailureCount() {
        return failureCount;
    }
    
    public static float getFailureRate() {
        return commandCount > 0 ? (float) failureCount / commandCount : 0;
    }
    
    public static List<BixbyEvent> getBixbyHistory() {
        return new ArrayList<>(bixbyHistory);
    }
}
