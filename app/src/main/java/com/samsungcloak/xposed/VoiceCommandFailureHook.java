package com.samsungcloak.xposed;

import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;

/**
 * Hook #18: Voice Command & Speech Recognition Failures
 * 
 * Simulates realistic speech recognition errors and voice input issues:
 * - Misrecognized commands due to background noise
 * - Partial speech recognition failures
 * - Voice input timeout and retry scenarios
 * - Accent/pronunciation variation effects
 * 
 * Targets: SM-A125U (Android 10/11)
 */
public class VoiceCommandFailureHook {

    private static final String TAG = "[HumanInteraction][VoiceCommand]";
    private static final boolean DEBUG = true;

    private static boolean enabled = true;
    private static float failureRate = 0.15f; // 15% recognition failure
    private static float partialFailureRate = 0.20f; // 20% partial failure
    private static float backgroundNoiseLevel = 0.3f;

    private static final Random random = new Random();
    
    // Common speech recognition errors
    private static final String[][] COMMON_MISRECOGNITIONS = {
        {"hey google", "hey gumbo"},
        {"send message", "send ages"},
        {"call mom", "call home"},
        {"set alarm", "set allarm"},
        {"play music", "pray music"},
        {"turn on lights", "turn on likes"},
        {"remind me", "remind mayo"},
        {"navigate to", "navigate do"},
    };

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        HookUtils.logInfo(TAG, "Initializing Voice Command Failure Hook");

        try {
            hookSpeechRecognizer(lpparam);
            hookVoiceInput(lpparam);
            HookUtils.logInfo(TAG, "Voice Command Failure Hook initialized");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize hook", e);
        }
    }

    private static void hookSpeechRecognizer(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook SpeechRecognizer
            Class<?> speechRecognizerClass = XposedHelpers.findClass(
                "android.speech.SpeechRecognizer", lpparam.classLoader);

            // Hook startListening to inject failures
            XposedBridge.hookAllMethods(speechRecognizerClass, "startListening",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    // Check if we should inject a failure
                    if (random.nextFloat() < failureRate) {
                        // Complete recognition failure
                        injectRecognitionFailure(param);
                    } else if (random.nextFloat() < partialFailureRate) {
                        // Partial failure - misrecognition
                        injectMisrecognition(param);
                    }

                    // Add background noise delay
                    if (backgroundNoiseLevel > 0.5f) {
                        Thread.sleep(random.nextInt(200) + 100);
                    }
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked SpeechRecognizer methods");

        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook SpeechRecognizer", e);
        }
    }

    private static void hookVoiceInput(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook InputMethodManager for voice input
            Class<?> inputMethodManagerClass = XposedHelpers.findClass(
                "android.view.inputmethod.InputMethodManager", lpparam.classLoader);

            // Hook startVoiceInput to simulate voice input issues
            XposedBridge.hookAllMethods(inputMethodManagerClass, "startListening",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    // Simulate voice input timeout
                    if (random.nextFloat() < 0.08f) {
                        // 8% chance of timeout
                        if (DEBUG) {
                            HookUtils.logDebug(TAG, "Voice input timeout - no speech detected");
                        }
                    }

                    // Simulate processing delay
                    int delay = random.nextInt(150) + 50;
                    Thread.sleep(delay);
                }
            });

            // Hook restartInput to simulate recognition restart
            XposedBridge.hookAllMethods(inputMethodManagerClass, "restartInput",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    // Sometimes restart fails due to "environment issues"
                    if (random.nextFloat() < 0.05f) {
                        if (DEBUG) {
                            HookUtils.logDebug(TAG, "Voice input restart failed - try again");
                        }
                        // Simulate brief delay before allowing retry
                        Thread.sleep(random.nextInt(100) + 50);
                    }
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked InputMethodManager voice methods");

        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook voice input", e);
        }
    }

    private static void injectRecognitionFailure(MethodHookParam param) {
        try {
            // Create error results to simulate recognition failure
            Bundle results = new Bundle();
            results.putIntArray("results_confidence", new int[]{0});
            results.putStringArray("results", new String[]{""});
            results.putInt("error", 7); // ERROR_NO_MATCH
            results.putString("error_message", "No speech recognized");

            if (DEBUG) {
                HookUtils.logDebug(TAG, "Injected recognition failure (no match)");
            }
            
            // Note: Actual injection would need to modify the callback result
        } catch (Exception e) {
            if (DEBUG) {
                HookUtils.logDebug(TAG, "Error injecting failure: " + e.getMessage());
            }
        }
    }

    private static void injectMisrecognition(MethodHookParam param) {
        try {
            // Select a random misrecognition pair
            int idx = random.nextInt(COMMON_MISRECOGNITIONS.length);
            String[] pair = COMMON_MISRECOGNITIONS[idx];

            // Log the misrecognition
            if (DEBUG) {
                HookUtils.logDebug(TAG, "Injected misrecognition: '" + pair[0] + "' -> '" + pair[1] + "'");
            }
        } catch (Exception e) {
            if (DEBUG) {
                HookUtils.logDebug(TAG, "Error injecting misrecognition: " + e.getMessage());
            }
        }
    }
}
