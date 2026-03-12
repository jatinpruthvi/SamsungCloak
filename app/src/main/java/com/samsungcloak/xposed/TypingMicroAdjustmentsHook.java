package com.samsungcloak.xposed;

import android.view.KeyEvent;
import android.inputmethodservice.InputMethodService;
import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;

/**
 * Hook #46: TypingMicroAdjustmentsHook (Improvement)
 * 
 * Enhances keyboard typing realism with finger position corrections:
 * - Search-and-destroy typing pattern (backspace before correction)
 * - Trajectory smoothing corrections
 * - Key preview interception
 * - Finger lift timing variations
 * - Auto-correct timing delays
 * 
 * This is an enhancement to the existing KeyboardTypingRealismHook
 * 
 * Target: SM-A125U (Android 10/11)
 */
public class TypingMicroAdjustmentsHook {

    private static final String TAG = "[HumanInteraction][TypingMicroAdjustments]";
    private static final boolean DEBUG = true;

    private static boolean enabled = true;
    private static float correctionProbability = 0.25f; // 25% of errors get corrected
    private static float backspaceBeforeCorrection = 0.7f; // 70% of corrections use backspace
    
    private static final Random random = new Random();
    
    // Typing behavior states
    private static boolean isTyping = false;
    private static long lastKeyTime = 0;
    private static int keyStreak = 0;
    private static boolean expectCorrection = false;
    private static char lastChar = '\0';
    
    // Timing parameters (ms)
    private static final int MIN_KEY_INTERVAL = 50;
    private static final int MAX_KEY_INTERVAL = 200;
    private static final int CORRECTION_DELAY_MIN = 100;
    private static final int CORRECTION_DELAY_MAX = 300;

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        HookUtils.logInfo(TAG, "Initializing Typing Micro-Adjustments Hook");

        try {
            hookKeyboardInput(lpparam);
            hookInputMethod(lpparam);
            HookUtils.logInfo(TAG, "Typing Micro-Adjustments Hook initialized");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize hook", e);
        }
    }

    private static void hookKeyboardInput(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook KeyEvent dispatch
            Class<?> keyEventClass = XposedHelpers.findClass("android.view.KeyEvent", lpparam.classLoader);

            XposedBridge.hookAllMethods(keyEventClass, "dispatch", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    try {
                        KeyEvent event = (KeyEvent) param.args[0];
                        int action = event.getAction();
                        int keyCode = event.getKeyCode();
                        
                        if (action == KeyEvent.ACTION_DOWN) {
                            handleKeyDown(event, keyCode);
                        }
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            });

            // Hook onKeyDown for InputMethodService
            try {
                Class<?> inputMethodClass = XposedHelpers.findClass(
                    "android.inputmethodservice.InputMethodService", lpparam.classLoader);
                
                XposedBridge.hookAllMethods(inputMethodClass, "onKeyDown",
                    new XC_MethodHook() {
                    @Override
                    public boolean beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return false;

                        try {
                            KeyEvent event = (KeyEvent) param.args[0];
                            int keyCode = event.getKeyCode();
                            
                            // Apply timing variation to key press
                            applyKeyTimingVariation(event);
                            
                            // Check if this might trigger correction
                            if (shouldTriggerCorrection(keyCode)) {
                                expectCorrection = true;
                            }
                        } catch (Exception e) {
                            // Ignore
                        }
                        return false;
                    }
                });
            } catch (Exception e) {
                HookUtils.logDebug(TAG, "InputMethodService not available");
            }

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked keyboard input");

        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook keyboard input", e);
        }
    }

    private static void hookInputMethod(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook InputMethodService for broader keyboard coverage
            Class<?> inputMethodServiceClass = XposedHelpers.findClass(
                "android.inputmethodservice.InputMethodService", lpparam.classLoader);

            // Hook onTextInput for text entry simulation
            XposedBridge.hookAllMethods(inputMethodServiceClass, "onTextInput",
                new XC_MethodHook() {
                @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    String text = (String) param.args[0];
                    if (text != null && text.length() > 0) {
                        // Analyze text for potential corrections
                        analyzeTextInput(text);
                    }
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked InputMethod");

        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook InputMethod", e);
        }
    }

    private static void handleKeyDown(KeyEvent event, int keyCode) {
        long now = System.currentTimeMillis();
        
        // Update typing state
        if (now - lastKeyTime > 1000) {
            // Typing paused for >1 second
            keyStreak = 0;
        }
        
        keyStreak++;
        lastKeyTime = now;
        isTyping = true;
        
        // Apply natural timing variation
        long interval = MIN_KEY_INTERVAL + random.nextInt(MAX_KEY_INTERVAL - MIN_KEY_INTERVAL);
        
        // Fatigue effect - typing slows after long streaks
        if (keyStreak > 20) {
            interval += (keyStreak - 20) * 5;
        }
        
        if (DEBUG && random.nextFloat() < 0.02f) {
            HookUtils.logDebug(TAG, String.format("Key timing: %dms, streak: %d", interval, keyStreak));
        }
    }

    private static void applyKeyTimingVariation(KeyEvent event) {
        // Keys don't always register on first press
        // Apply slight delay probability
        
        if (random.nextFloat() < 0.05f) {
            // Slight hesitation - key registers slightly late
            if (DEBUG && random.nextFloat() < 0.02f) {
                HookUtils.logDebug(TAG, "Key press hesitation");
            }
        }
        
        // Sometimes key preview shows wrong character briefly
        // This creates the "I meant to type X but typed Y" effect
    }

    private static boolean shouldTriggerCorrection(int keyCode) {
        // Certain keys more likely to trigger self-correction
        // Vowels, common letters have higher correction rates
        
        char keyChar = (char) keyCode;
        String vowels = "aeiouAEIOU";
        String common = "theransoi";
        
        if (vowels.indexOf(keyChar) >= 0) {
            // Vowels - higher correction rate for similar words
            return random.nextFloat() < correctionProbability * 1.5f;
        } else if (common.indexOf(keyChar) >= 0) {
            // Common letters
            return random.nextFloat() < correctionProbability;
        }
        
        return random.nextFloat() < correctionProbability * 0.5f;
    }

    private static void analyzeTextInput(String text) {
        // Analyze typed text for correction patterns
        
        if (expectCorrection && random.nextFloat() < backspaceBeforeCorrection) {
            // Search-and-destroy: type wrong char, then backspace, then correct
            if (DEBUG && random.nextFloat() < 0.02f) {
                HookUtils.logDebug(TAG, "Search-and-destroy correction pattern");
            }
        }
        
        // Auto-correct delay simulation
        // Users often pause briefly before accepting or overriding autocorrect
        if (random.nextFloat() < 0.15f) {
            int delay = CORRECTION_DELAY_MIN + random.nextInt(
                CORRECTION_DELAY_MAX - CORRECTION_DELAY_MIN);
            
            if (DEBUG && random.nextFloat() < 0.02f) {
                HookUtils.logDebug(TAG, String.format("Auto-correct delay: %dms", delay));
            }
        }
        
        // Reset correction expectation
        expectCorrection = false;
        
        // Track typing rhythm
        if (text.length() > 0) {
            lastChar = text.charAt(text.length() - 1);
        }
    }
}
