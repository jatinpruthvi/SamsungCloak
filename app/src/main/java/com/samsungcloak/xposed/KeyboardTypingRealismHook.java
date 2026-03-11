package com.samsungcloak.xposed;

import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hook #13: Keyboard Typing Realism Simulation
 * 
 * Simulates human typing errors and patterns:
 * - Character substitutions (fat-finger, adjacent keys)
 * - Missing keystrokes requiring re-entry
 * - Double-tap corrections
 * - Auto-correct interaction patterns
 * - Keyboard switching delays
 * 
 * Targets: SM-A125U (Android 10/11)
 */
public class KeyboardTypingRealismHook {

    private static final String TAG = "[HumanInteraction][KeyboardTyping]";
    private static final boolean DEBUG = true;

    private static boolean enabled = true;
    private static float errorRate = 0.08f;
    private static float correctionRate = 0.15f;
    private static float autocorrectTriggerRate = 0.25f;

    private static final Random random = new Random();
    private static long lastKeystrokeTime = 0;
    private static final ConcurrentHashMap<Integer, Long> keyPressTimes = new ConcurrentHashMap<>();

    // QWERTY keyboard layout for adjacent key calculation
    private static final String[] QWERTY_ROWS = {
        "qwertyuiop",
        "asdfghjkl",
        "zxcvbnm"
    };

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        HookUtils.logInfo(TAG, "Initializing Keyboard Typing Realism Hook");

        try {
            hookInputConnection(lpparam);
            hookKeyEvent(lpparam);
            HookUtils.logInfo(TAG, "Keyboard Typing Realism Hook initialized successfully");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize hook", e);
        }
    }

    private static void hookInputConnection(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> inputConnectionClass = XposedHelpers.findClass(
                "android.view.inputmethod.InputConnection", lpparam.classLoader);

            // Hook commitText to simulate typing behavior
            XposedBridge.hookAllMethods(inputConnectionClass, "commitText", 
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    CharSequence text = (CharSequence) param.args[0];
                    int newCursorPosition = (int) param.args[1];

                    if (text != null && text.length() > 0) {
                        String input = text.toString();
                        
                        // Apply typing errors to input
                        String processedInput = applyTypingErrors(input);
                        
                        if (!processedInput.equals(input)) {
                            param.args[0] = processedInput;
                            if (DEBUG) {
                                HookUtils.logDebug(TAG, "Applied typing error: '" + 
                                    input + "' -> '" + processedInput + "'");
                            }
                        }

                        // Simulate auto-correct interaction
                        if (shouldTriggerAutoCorrect(processedInput)) {
                            // Add subtle delay for auto-correct suggestion
                            Thread.sleep(random.nextInt(80) + 20);
                        }

                        // Simulate word suggestion selection delay
                        if (random.nextFloat() < 0.1f) {
                            Thread.sleep(random.nextInt(100) + 50);
                        }
                    }
                }
            });

            // Hook deleteSurroundingText to simulate correction behavior
            XposedBridge.hookAllMethods(inputConnectionClass, "deleteSurroundingText",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    int beforeLength = (int) param.args[0];
                    int afterLength = (int) param.args[1];

                    // Simulate human correction - slight delay before deleting
                    if (random.nextFloat() < correctionRate) {
                        Thread.sleep(random.nextInt(120) + 30);
                    }

                    // Sometimes simulate accidental double-delete
                    if (random.nextFloat() < 0.05f) {
                        param.args[0] = beforeLength + 1;
                    }
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked InputConnection methods");

        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook InputConnection", e);
        }
    }

    private static void hookKeyEvent(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> viewClass = XposedHelpers.findClass("android.view.View", lpparam.classLoader);

            // Hook dispatchKeyEvent to intercept keyboard input
            XposedBridge.hookAllMethods(viewClass, "dispatchKeyEvent", 
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    KeyEvent event = (KeyEvent) param.args[0];
                    int action = event.getAction();
                    int keyCode = event.getKeyCode();

                    if (action == KeyEvent.ACTION_DOWN) {
                        long now = System.currentTimeMillis();
                        long deltaTime = now - lastKeystrokeTime;

                        // Human typing rhythm: 100-400ms between keys
                        // Inject hesitation for过快 (too fast) typing
                        if (deltaTime < 50) {
                            // Too fast - may be automated, inject hesitation
                            Thread.sleep(random.nextInt(40) + 20);
                        } else if (deltaTime > 800) {
                            // Very slow - simulate thinking/hesitation
                            if (random.nextFloat() < 0.1f) {
                                Thread.sleep(random.nextInt(200) + 50);
                            }
                        }

                        // Track key press timing
                        keyPressTimes.put(keyCode, now);
                        lastKeystrokeTime = now;

                        // Apply keystroke errors
                        char typedChar = keyCodeToChar(keyCode);
                        if (typedChar != 0) {
                            char errorChar = applyKeystrokeError(typedChar, deltaTime);
                            if (errorChar != typedChar && errorChar != 0) {
                                if (DEBUG) {
                                    HookUtils.logDebug(TAG, "Keystroke error: '" + 
                                        typedChar + "' -> '" + errorChar + "'");
                                }
                            }
                        }
                    }
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked View.dispatchKeyEvent");

        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook KeyEvent", e);
        }
    }

    private static String applyTypingErrors(String input) {
        if (input == null || input.isEmpty()) return input;

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (random.nextFloat() < errorRate) {
                // Apply specific error types
                int errorType = random.nextInt(100);

                if (errorType < 40) {
                    // 40% - Adjacent key substitution
                    char adjacent = getAdjacentKey(c);
                    if (adjacent != c) {
                        result.append(adjacent);
                        continue;
                    }
                } else if (errorType < 55) {
                    // 15% - Double character (e.g., "helllo")
                    result.append(c);
                } else if (errorType < 70) {
                    // 15% - Skip character (missing letter)
                    continue;
                } else if (errorType < 85) {
                    // 15% - Transposition ready (handled at word level)
                } else {
                    // 15% - Random wrong key
                    result.append((char) ('a' + random.nextInt(26)));
                    continue;
                }
            }
            result.append(c);
        }

        return result.toString();
    }

    private static char applyKeystrokeError(char original, long deltaTime) {
        // Adjust error rate based on typing speed
        float adjustedErrorRate = errorRate;
        if (deltaTime < 100) {
            adjustedErrorRate *= 1.5f; // Faster = more errors
        } else if (deltaTime > 500) {
            adjustedErrorRate *= 0.6f; // Slower = fewer errors
        }

        if (random.nextFloat() > adjustedErrorRate) {
            return original; // No error
        }

        // Apply specific error types
        int errorType = random.nextInt(100);

        if (errorType < 50) {
            // 50% - Adjacent key
            return getAdjacentKey(original);
        } else if (errorType < 70) {
            // 20% - Skip (signal to drop)
            return 0;
        } else if (errorType < 85) {
            // 15% - Double press
            return original;
        } else {
            // 15% - Random wrong key
            return (char) ('a' + random.nextInt(26));
        }
    }

    private static char getAdjacentKey(char key) {
        key = Character.toLowerCase(key);
        if (key < 'a' || key > 'z') return key;

        // Find adjacent keys on QWERTY
        int[] adjacent = getAdjacentIndices(key);
        if (adjacent.length > 0) {
            int idx = adjacent[random.nextInt(adjacent.length)];
            return (char) ('a' + idx);
        }
        return key;
    }

    private static int[] getAdjacentIndices(char key) {
        // Physical adjacency on QWERTY keyboard
        switch (key) {
            case 'q': return new int[]{1, 4, 7};
            case 'w': return new int[]{0, 2, 4, 5, 7, 8};
            case 'e': return new int[]{1, 3, 5, 8, 9};
            case 'r': return new int[]{2, 4, 6, 9, 10};
            case 't': return new int[]{3, 5, 7, 10, 11};
            case 'y': return new int[]{4, 6, 8, 11, 12};
            case 'u': return new int[]{5, 7, 9, 12, 13};
            case 'i': return new int[]{6, 8, 10, 13, 14};
            case 'o': return new int[]{7, 9, 11, 14, 15};
            case 'p': return new int[]{8, 10, 15};
            case 'a': return new int[]{0, 4, 7, 18, 19};
            case 's': return new int[]{1, 4, 5, 7, 8, 19, 20};
            case 'd': return new int[]{2, 5, 6, 8, 9, 20, 21};
            case 'f': return new int[]{3, 6, 7, 9, 10, 21, 22};
            case 'g': return new int[]{4, 7, 8, 10, 11, 22, 23};
            case 'h': return new int[]{5, 8, 9, 11, 12, 23, 24};
            case 'j': return new int[]{6, 9, 10, 12, 13, 24, 25};
            case 'k': return new int[]{7, 10, 11, 13, 14, 25};
            case 'l': return new int[]{8, 11, 12, 14, 15, 25};
            case 'z': return new int[]{18, 19, 22};
            case 'x': return new int[]{17, 19, 20, 22, 23};
            case 'c': return new int[]{18, 20, 21, 23, 24};
            case 'v': return new int[]{19, 21, 22, 24, 25};
            case 'b': return new int[]{20, 22, 23, 25};
            case 'n': return new int[]{21, 23, 24};
            case 'm': return new int[]{22, 24, 25};
            default: return new int[]{};
        }
    }

    private static char keyCodeToChar(int keyCode) {
        // Convert KeyEvent keyCode to character
        if (keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z) {
            return (char) ('a' + (keyCode - KeyEvent.KEYCODE_A));
        }
        if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
            return (char) ('0' + (keyCode - KeyEvent.KEYCODE_0));
        }
        return 0;
    }

    private static boolean shouldTriggerAutoCorrect(String input) {
        // Common patterns that trigger auto-correct
        return input.length() >= 3 && 
               random.nextFloat() < autocorrectTriggerRate;
    }
}
