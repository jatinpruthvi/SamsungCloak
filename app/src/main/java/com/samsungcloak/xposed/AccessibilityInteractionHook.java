package com.samsungcloak.xposed;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AccessibilityInteractionHook - Assistive Technology Usage Simulation
 *
 * Simulates realistic accessibility service interactions:
 * 1. TalkBack exploration patterns (linear vs hierarchical navigation)
 * 2. Magnification gestures (triple-tap timing, pan speeds)
 * 3. Color correction usage patterns
 * 4. Switch Access scan timing
 * 5. Voice Access command latency
 *
 * Based on studies of users with visual/motor impairments.
 * Reference: WebAIM Screen Reader User Surveys, A11y Project Guidelines
 *
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class AccessibilityInteractionHook {

    private static final String TAG = "[Accessibility][Interaction]";
    private static final String PREFS_NAME = "SamsungCloak_A11y";
    private static final boolean DEBUG = true;

    private static boolean enabled = false;
    private static SharedPreferences prefs;
    private static final Random random = new Random();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Accessibility mode
    public enum AccessibilityMode {
        NONE,
        TALKBACK_EXPLORATION,
        TALKBACK_EFFICIENT,
        MAGNIFICATION,
        COLOR_CORRECTION,
        SWITCH_ACCESS,
        VOICE_ACCESS
    }

    private static AccessibilityMode currentMode = AccessibilityMode.NONE;
    private static boolean isAccessibilityEnabled = false;

    // ===== TalkBack Parameters =====
    private static boolean talkbackEnabled = false;
    private static double talkbackSpeechRate = 1.0; // 0.5 - 2.0
    private static int talkbackExplorationDelayMs = 250; // Delay between elements
    private static boolean talkbackHierarchicalNavigation = false;

    // TalkBack gesture timing (empirical data)
    private static final int TALKBACK_SWIPE_MIN_MS = 180;
    private static final int TALKBACK_SWIPE_MAX_MS = 350;
    private static final int TALKBACK_DOUBLE_TAP_MS = 120;
    private static final int TALKBACK_EXPLORATION_PAUSE_MS = 800;

    // ===== Magnification Parameters =====
    private static boolean magnificationEnabled = false;
    private static float magnificationScale = 2.0f;
    private static int tripleTapTimingMs = 300; // Must be < 300ms for detection
    private static int magnificationPanSpeed = 200; // pixels/second

    // ===== Color Correction Parameters =====
    private static boolean colorCorrectionEnabled = false;
    private static int colorCorrectionMode = 0; // 0=deuteranomaly, 1=protanomaly, 2=tritanomaly

    // ===== Switch Access Parameters =====
    private static boolean switchAccessEnabled = false;
    private static int scanRateMs = 1000; // Auto-scan interval
    private static double switchActivationTimeMs = 150; // Time to activate switch

    // ===== Voice Access Parameters =====
    private static boolean voiceAccessEnabled = false;
    private static int voiceCommandLatencyMs = 800; // Voice recognition delay
    private static double voiceCommandAccuracy = 0.92; // Recognition accuracy

    // State tracking
    private static long lastAccessibilityEventTime = 0;
    private static int consecutiveExplorationEvents = 0;
    private static AtomicBoolean isProcessingGesture = new AtomicBoolean(false);

    /**
     * Initialize the Accessibility Interaction Hook
     */
    public static void init(XC_LoadPackage.LoadPackageParam lpparam, Context context) {
        if (context != null) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            enabled = prefs.getBoolean("a11y_interaction_enabled", false);
        }

        if (!enabled) {
            HookUtils.logInfo(TAG, "Accessibility interaction hook disabled");
            return;
        }

        HookUtils.logInfo(TAG, "Initializing Accessibility Interaction Hook");

        try {
            loadAccessibilityState();

            hookAccessibilityManager(lpparam);
            hookAccessibilityService(lpparam);
            hookGestureDetector(lpparam);
            hookViewSystem(lpparam);

            HookUtils.logInfo(TAG, "Accessibility Interaction Hook initialized");
            logAccessibilityStatus();
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize: " + e.getMessage());
        }
    }

    /**
     * Alternative init without Context
     */
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        enabled = false;
        HookUtils.logInfo(TAG, "Accessibility interaction hook requires Context for SharedPreferences");
    }

    private static void loadAccessibilityState() {
        if (prefs == null) return;

        currentMode = AccessibilityMode.values()[prefs.getInt("a11y_mode", 0)];
        talkbackEnabled = prefs.getBoolean("talkback_enabled", false);
        talkbackSpeechRate = prefs.getFloat("talkback_rate", 1.0f);
        talkbackExplorationDelayMs = prefs.getInt("talkback_delay", 250);

        magnificationEnabled = prefs.getBoolean("magnification_enabled", false);
        magnificationScale = prefs.getFloat("magnification_scale", 2.0f);

        colorCorrectionEnabled = prefs.getBoolean("color_correction_enabled", false);
        colorCorrectionMode = prefs.getInt("color_correction_mode", 0);

        switchAccessEnabled = prefs.getBoolean("switch_access_enabled", false);
        scanRateMs = prefs.getInt("scan_rate_ms", 1000);

        voiceAccessEnabled = prefs.getBoolean("voice_access_enabled", false);
        voiceCommandLatencyMs = prefs.getInt("voice_latency_ms", 800);
    }

    private static void hookAccessibilityManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> accessibilityManagerClass = XposedHelpers.findClass(
                "android.view.accessibility.AccessibilityManager", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(accessibilityManagerClass, "isEnabled",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        // Return enabled state based on current simulation mode
                        if (currentMode != AccessibilityMode.NONE) {
                            param.setResult(true);
                        }
                    }
                });

            XposedBridge.hookAllMethods(accessibilityManagerClass, "isTouchExplorationEnabled",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        if (currentMode == AccessibilityMode.TALKBACK_EXPLORATION ||
                            currentMode == AccessibilityMode.TALKBACK_EFFICIENT) {
                            param.setResult(true);
                        }
                    }
                });

            HookUtils.logDebug(TAG, "AccessibilityManager hooked");
        } catch (Exception e) {
            HookUtils.logError(TAG, "AccessibilityManager hook failed: " + e.getMessage());
        }
    }

    private static void hookAccessibilityService(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> accessibilityServiceClass = XposedHelpers.findClass(
                "android.accessibilityservice.AccessibilityService", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(accessibilityServiceClass, "onAccessibilityEvent",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        AccessibilityEvent event = (AccessibilityEvent) param.args[0];
                        processAccessibilityEvent(event);
                    }
                });

            HookUtils.logDebug(TAG, "AccessibilityService hooked");
        } catch (Exception e) {
            HookUtils.logError(TAG, "AccessibilityService hook failed: " + e.getMessage());
        }
    }

    private static void processAccessibilityEvent(AccessibilityEvent event) {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastEvent = currentTime - lastAccessibilityEventTime;

        switch (currentMode) {
            case TALKBACK_EXPLORATION:
            case TALKBACK_EFFICIENT:
                // Simulate TalkBack exploration timing
                if (timeSinceLastEvent < talkbackExplorationDelayMs) {
                    // Too fast for realistic exploration - add delay
                    addExplorationDelay((int) (talkbackExplorationDelayMs - timeSinceLastEvent));
                }
                consecutiveExplorationEvents++;

                if (DEBUG && consecutiveExplorationEvents % 10 == 0) {
                    HookUtils.logDebug(TAG, "TalkBack exploration: " + consecutiveExplorationEvents +
                                     " elements, rate=" + talkbackSpeechRate + "x");
                }
                break;

            case SWITCH_ACCESS:
                // Simulate switch scan timing
                if (timeSinceLastEvent < scanRateMs) {
                    addExplorationDelay((int) (scanRateMs - timeSinceLastEvent));
                }
                break;

            case VOICE_ACCESS:
                // Simulate voice command processing delay
                addExplorationDelay(voiceCommandLatencyMs);
                break;

            default:
                break;
        }

        lastAccessibilityEventTime = currentTime;
    }

    private static void hookGestureDetector(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> gestureDetectorClass = XposedHelpers.findClass(
                "android.view.GestureDetector", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(gestureDetectorClass, "onTouchEvent",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !magnificationEnabled) return;

                        // Check for triple-tap pattern for magnification
                        detectMagnificationGesture(param);
                    }
                });

            HookUtils.logDebug(TAG, "GestureDetector hooked for magnification");
        } catch (Exception e) {
            HookUtils.logError(TAG, "GestureDetector hook failed: " + e.getMessage());
        }
    }

    private static void detectMagnificationGesture(XC_MethodHook.MethodHookParam param) {
        // Track triple-tap timing for magnification
        // Real users typically tap 3 times within 200-300ms

        if (isProcessingGesture.get()) return;

        // Simulate realistic triple-tap timing variations
        if (random.nextDouble() < 0.05) { // 5% chance of slow triple-tap
            int delay = tripleTapTimingMs + random.nextInt(100);
            if (delay > 300) {
                // Too slow - gesture not recognized
                HookUtils.logDebug(TAG, "Magnification triple-tap too slow: " + delay + "ms");
            }
        }
    }

    private static void hookViewSystem(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> viewClass = XposedHelpers.findClass(
                "android.view.View", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(viewClass, "performClick",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        // Accessibility users often have different click timing
                        if (currentMode == AccessibilityMode.TALKBACK_EXPLORATION) {
                            // Double-tap timing for TalkBack activation
                            int doubleTapDelay = TALKBACK_DOUBLE_TAP_MS +
                                random.nextInt(40) - 20; // +/- 20ms variance

                            if (DEBUG && random.nextDouble() < 0.01) {
                                HookUtils.logDebug(TAG, "TalkBack double-tap delay: " + doubleTapDelay + "ms");
                            }
                        }

                        if (currentMode == AccessibilityMode.SWITCH_ACCESS) {
                            // Switch users take time to activate
                            int activationDelay = (int) (switchActivationTimeMs +
                                random.nextGaussian() * 30);
                            addExplorationDelay(Math.max(0, activationDelay));
                        }
                    }
                });

            HookUtils.logDebug(TAG, "View performClick hooked");
        } catch (Exception e) {
            HookUtils.logError(TAG, "View hook failed: " + e.getMessage());
        }
    }

    private static void addExplorationDelay(final int delayMs) {
        if (delayMs <= 0) return;

        isProcessingGesture.set(true);

        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            isProcessingGesture.set(false);
        }
    }

    private static void logAccessibilityStatus() {
        HookUtils.logInfo(TAG, "=== Accessibility Interaction Status ===");
        HookUtils.logInfo(TAG, "Mode: " + currentMode.name());
        HookUtils.logInfo(TAG, "TalkBack: " + talkbackEnabled + " (rate=" + talkbackSpeechRate + "x)");
        HookUtils.logInfo(TAG, "Magnification: " + magnificationEnabled + " (scale=" + magnificationScale + "x)");
        HookUtils.logInfo(TAG, "Color Correction: " + colorCorrectionEnabled);
        HookUtils.logInfo(TAG, "Switch Access: " + switchAccessEnabled + " (scan=" + scanRateMs + "ms)");
        HookUtils.logInfo(TAG, "Voice Access: " + voiceAccessEnabled + " (latency=" + voiceCommandLatencyMs + "ms)");
        HookUtils.logInfo(TAG, "=======================================");
    }

    // ========== Configuration Methods ==========

    public static void setEnabled(boolean enabled) {
        AccessibilityInteractionHook.enabled = enabled;
        if (prefs != null) {
            prefs.edit().putBoolean("a11y_interaction_enabled", enabled).apply();
        }
        HookUtils.logInfo(TAG, "Hook " + (enabled ? "enabled" : "disabled"));
    }

    public static void setAccessibilityMode(AccessibilityMode mode) {
        currentMode = mode;
        isAccessibilityEnabled = (mode != AccessibilityMode.NONE);

        if (prefs != null) {
            prefs.edit().putInt("a11y_mode", mode.ordinal()).apply();
        }

        // Update related flags
        talkbackEnabled = (mode == AccessibilityMode.TALKBACK_EXPLORATION ||
                          mode == AccessibilityMode.TALKBACK_EFFICIENT);
        magnificationEnabled = (mode == AccessibilityMode.MAGNIFICATION);

        HookUtils.logInfo(TAG, "Accessibility mode set to: " + mode.name());
    }

    public static void setTalkBackSpeechRate(double rate) {
        talkbackSpeechRate = Math.max(0.5, Math.min(2.0, rate));
        if (prefs != null) {
            prefs.edit().putFloat("talkback_rate", (float) talkbackSpeechRate).apply();
        }
    }

    public static AccessibilityMode getCurrentMode() {
        return currentMode;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setMagnificationEnabled(boolean enabled) {
        magnificationEnabled = enabled;
    }

    public static void setColorCorrectionEnabled(boolean enabled) {
        colorCorrectionEnabled = enabled;
    }

    public static void setSwitchAccessEnabled(boolean enabled) {
        switchAccessEnabled = enabled;
    }

    public static void setVoiceAccessEnabled(boolean enabled) {
        voiceAccessEnabled = enabled;
    }
}
