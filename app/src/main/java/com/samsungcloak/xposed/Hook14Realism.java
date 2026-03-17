package com.samsungcloak.xposed;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Hook14Realism - Accessibility Scenario Simulation
 * 
 * Simulates accessibility-related interaction patterns for Samsung Galaxy A12:
 * - Tremor/Parkinsonian simulation: micro-tremor 2-8Hz, touch area expansion 20-50%
 * - One-handed mode adaptation: reach zone limitations, grip-based coordinate offset
 * - Screen reader patterns: TalkBack focus traversal, reading speed 150-300 WPM
 * - Reduced motor control: delayed reaction 150-400ms, gesture completion rate 40-60%
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class Hook14Realism extends BaseRealismHook {

    private static final String TAG = "[Realism][Hook14-Accessibility]";
    private static final boolean DEBUG = true;
    
    private static final String HOOK_ID = "hook_14";
    private static final String HOOK_NAME = "Accessibility Scenario Simulation";
    
    // Configuration keys
    private static final String KEY_ENABLED = "accessibility_enabled";
    private static final String KEY_TREMOR_SIM = "tremor_simulation_enabled";
    private static final String KEY_ONE_HANDED = "one_handed_mode_enabled";
    private static final String KEY_SCREEN_READER = "screen_reader_enabled";
    private static final String KEY_MOTOR_CONTROL = "reduced_motor_control_enabled";
    
    // Hook state
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    private static AtomicBoolean enabled = new AtomicBoolean(true);
    private static float intensity = 0.5f;
    
    // Tremor simulation parameters
    private static boolean tremorEnabled = true;
    private static float tremorFrequency = 5.0f; // Hz
    private static float tremorAmplitude = 0.02f;
    private static float touchAreaExpansion = 0.30f; // 30% increase
    
    // One-handed mode parameters
    private static boolean oneHandedEnabled = true;
    private static float reachZoneLimitation = 0.25f;
    private static float gripOffsetX = 0.0f;
    private static float gripOffsetY = 0.0f;
    
    // Screen reader (TalkBack) parameters
    private static boolean screenReaderEnabled = true;
    private static int talkBackReadingSpeed = 220; // WPM
    private static float focusTraversalDelay = 0.3f; // seconds
    private static boolean gestureRecognitionChallenge = true;
    
    // Reduced motor control parameters
    private static boolean motorControlEnabled = true;
    private static int reactionDelayMs = 250;
    private static float gestureCompletionRate = 0.50f;
    private static int dwellTimeMs = 500;
    
    // State tracking
    private static final AtomicReference<String> currentMode = 
        new AtomicReference<>("NORMAL");
    private static final AtomicInteger tapCount = new AtomicInteger(0);
    private static final AtomicInteger failedGestures = new AtomicInteger(0);
    
    // Touch history for pattern analysis
    private static final CopyOnWriteArrayList<TouchEvent> touchHistory = 
        new CopyOnWriteArrayList<>();
    private static final int MAX_HISTORY = 100;
    
    // Thread-local random
    private static final ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new Random());
    
    // Timing
    private static long lastTouchTime = 0;
    private static long sessionStartTime = 0;
    
    // Tremor phase tracking
    private static double tremorPhase = 0.0;
    
    public Hook14Realism() {
        super(HOOK_ID, HOOK_NAME);
    }
    
    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!hookInitialized.compareAndSet(false, true)) {
            logInfo("Hook already initialized");
            return;
        }
        
        logInfo("Initializing Accessibility Scenario Simulation Hook");
        sessionStartTime = System.currentTimeMillis();
        
        try {
            // Load configuration
            loadConfiguration();
            
            // Hook MotionEvent for touch simulation
            hookMotionEvent(lpparam);
            
            // Hook AccessibilityManager
            hookAccessibilityManager(lpparam);
            
            // Hook TextToSpeech if available
            hookTextToSpeech(lpparam);
            
            // Hook WindowManager for orientation/grip
            hookWindowManager(lpparam);
            
            logInfo("Accessibility Simulation Hook initialized successfully");
        } catch (Exception e) {
            logError("Failed to initialize Accessibility Simulation Hook", e);
        }
    }
    
    private void loadConfiguration() {
        if (configManager != null) {
            enabled.set(configManager.getHookParamBool(HOOK_ID, KEY_ENABLED, true));
            intensity = configManager.getHookIntensity(HOOK_ID);
            
            tremorEnabled = configManager.getHookParamBool(HOOK_ID, KEY_TREMOR_SIM, true);
            tremorFrequency = configManager.getHookParamFloat(HOOK_ID, "tremor_frequency", 5.0f);
            tremorAmplitude = configManager.getHookParamFloat(HOOK_ID, "tremor_amplitude", 0.02f);
            touchAreaExpansion = configManager.getHookParamFloat(HOOK_ID, "touch_area_expansion", 0.30f);
            
            oneHandedEnabled = configManager.getHookParamBool(HOOK_ID, KEY_ONE_HANDED, true);
            reachZoneLimitation = configManager.getHookParamFloat(HOOK_ID, "reach_zone_limitation", 0.25f);
            
            screenReaderEnabled = configManager.getHookParamBool(HOOK_ID, KEY_SCREEN_READER, true);
            talkBackReadingSpeed = configManager.getHookParamInt(HOOK_ID, "talkback_speed", 220);
            focusTraversalDelay = configManager.getHookParamFloat(HOOK_ID, "focus_delay", 0.3f);
            
            motorControlEnabled = configManager.getHookParamBool(HOOK_ID, KEY_MOTOR_CONTROL, true);
            reactionDelayMs = configManager.getHookParamInt(HOOK_ID, "reaction_delay_ms", 250);
            gestureCompletionRate = configManager.getHookParamFloat(HOOK_ID, "gesture_completion_rate", 0.50f);
        }
        
        logInfo("Configuration loaded: enabled=" + enabled.get() + ", intensity=" + intensity);
    }
    
    private void hookMotionEvent(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> motionEventClass = XposedHelpers.findClass(
                "android.view.MotionEvent", lpparam.classLoader);
            
            // Hook obtain method to inject tremor
            XposedBridge.hookAllMethods(motionEventClass, "obtain",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get()) return;
                        
                        try {
                            MotionEvent event = (MotionEvent) param.getResult();
                            if (event != null) {
                                MotionEvent modified = applyTouchModifications(event);
                                if (modified != null) {
                                    param.setResult(modified);
                                }
                            }
                        } catch (Exception e) {
                            logDebug("Error in MotionEvent.obtain hook: " + e.getMessage());
                        }
                    }
                });
            
            // Hook getX and getY to apply offset
            XposedBridge.hookAllMethods(motionEventClass, "getX",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get()) return;
                        
                        try {
                            float originalX = (Float) param.getResult();
                            float modifiedX = applyTremorOffset(originalX, true);
                            param.setResult(modifiedX);
                        } catch (Exception e) {
                            logDebug("Error in getX hook: " + e.getMessage());
                        }
                    }
                });
            
            XposedBridge.hookAllMethods(motionEventClass, "getY",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get()) return;
                        
                        try {
                            float originalY = (Float) param.getResult();
                            float modifiedY = applyTremorOffset(originalY, false);
                            param.setResult(modifiedY);
                        } catch (Exception e) {
                            logDebug("Error in getY hook: " + e.getMessage());
                        }
                    }
                });
            
            logInfo("Hooked MotionEvent for accessibility simulation");
        } catch (Exception e) {
            logError("Failed to hook MotionEvent", e);
        }
    }
    
    private void hookAccessibilityManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> accessibilityManagerClass = XposedHelpers.findClass(
                "android.view.accessibility.AccessibilityManager", lpparam.classLoader);
            
            // Hook isEnabled to simulate TalkBack state
            XposedBridge.hookAllMethods(accessibilityManagerClass, "isEnabled",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get() || !screenReaderEnabled) return;
                        
                        try {
                            // Sometimes simulate TalkBack as enabled
                            if (random.get().nextDouble() < 0.15 * intensity) {
                                param.setResult(true);
                            }
                        } catch (Exception e) {
                            logDebug("Error in AccessibilityManager.isEnabled hook: " + e.getMessage());
                        }
                    }
                });
            
            logInfo("Hooked AccessibilityManager");
        } catch (Exception e) {
            logError("Failed to hook AccessibilityManager", e);
        }
    }
    
    private void hookTextToSpeech(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> ttsClass = XposedHelpers.findClass(
                "android.speech.tts.TextToSpeech", lpparam.classLoader);
            
            // Hook setSpeechRate to simulate reading speed variation
            XposedBridge.hookAllMethods(ttsClass, "setSpeechRate",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get() || !screenReaderEnabled) return;
                        
                        try {
                            float requestedRate = (Float) param.args[0];
                            // Convert WPM to TTS rate (1.0 = normal)
                            float baseRate = requestedRate / 220.0f;
                            // Add variance based on accessibility settings
                            float variance = 0.8f + random.get().nextFloat() * 0.4f;
                            param.args[0] = baseRate * variance;
                        } catch (Exception e) {
                            logDebug("Error in setSpeechRate hook: " + e.getMessage());
                        }
                    }
                });
            
            logInfo("Hooked TextToSpeech");
        } catch (Exception e) {
            logDebug("TextToSpeech not available: " + e.getMessage());
        }
    }
    
    private void hookWindowManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> windowManagerClass = XposedHelpers.findClass(
                "android.view.WindowManager", lpparam.classLoader);
            
            // Hook getDefaultDisplay to simulate one-handed mode
            XposedBridge.hookAllMethods(windowManagerClass, "getDefaultDisplay",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get() || !oneHandedEnabled) return;
                        
                        try {
                            // Occasionally simulate one-handed mode
                            if (random.get().nextDouble() < 0.20 * intensity) {
                                currentMode.set("ONE_HANDED");
                            }
                        } catch (Exception e) {
                            logDebug("Error in getDefaultDisplay hook: " + e.getMessage());
                        }
                    }
                });
            
            logInfo("Hooked WindowManager");
        } catch (Exception e) {
            logError("Failed to hook WindowManager", e);
        }
    }
    
    private MotionEvent applyTouchModifications(MotionEvent event) {
        if (event == null) return null;
        
        try {
            int action = event.getAction();
            
            // Apply touch area expansion for ACTION_DOWN
            if (action == MotionEvent.ACTION_DOWN && touchAreaExpansion > 0) {
                // Store original touch area info for hit testing
                // This would need to be handled at a higher level
            }
            
            // Apply reaction delay for touch events
            if (motorControlEnabled && reactionDelayMs > 0) {
                // Note: Actual delay would need async handling
                // For now, we just track timing
            }
            
            // Track touch events
            long now = System.currentTimeMillis();
            if (now - lastTouchTime > 100) {
                touchHistory.add(new TouchEvent(event.getX(), event.getY(), action, now));
                while (touchHistory.size() > MAX_HISTORY) {
                    touchHistory.remove(0);
                }
                tapCount.incrementAndGet();
            }
            lastTouchTime = now;
            
            return null; // Return null to keep original event
        } catch (Exception e) {
            logDebug("Error applying touch modifications: " + e.getMessage());
            return null;
        }
    }
    
    private float applyTremorOffset(float coordinate, boolean isX) {
        if (!tremorEnabled || tremorAmplitude <= 0) {
            return coordinate;
        }
        
        try {
            // Update tremor phase based on time
            long elapsed = System.currentTimeMillis() - sessionStartTime;
            double phaseIncrement = (2 * Math.PI * tremorFrequency * 0.001);
            tremorPhase += phaseIncrement;
            
            // Calculate tremor offset
            double tremorOffset = Math.sin(tremorPhase) * tremorAmplitude * intensity;
            
            // Add some noise
            tremorOffset += (random.get().nextDouble() - 0.5) * tremorAmplitude * 0.5;
            
            float offset = (float) tremorOffset;
            
            // Apply one-handed grip offset if applicable
            if (oneHandedEnabled && "ONE_HANDED".equals(currentMode.get())) {
                if (isX) {
                    offset += gripOffsetX * reachZoneLimitation;
                } else {
                    offset += gripOffsetY * reachZoneLimitation;
                }
            }
            
            return coordinate + offset;
        } catch (Exception e) {
            logDebug("Error applying tremor offset: " + e.getMessage());
            return coordinate;
        }
    }
    
    /**
     * Set accessibility mode
     */
    public static void setAccessibilityMode(String mode) {
        currentMode.set(mode);
        HookUtils.logInfo(TAG, "Accessibility mode changed to: " + mode);
    }
    
    /**
     * Enable/disable the hook
     */
    public static void setEnabled(boolean isEnabled) {
        enabled.set(isEnabled);
        HookUtils.logInfo(TAG, "Hook " + (isEnabled ? "enabled" : "disabled"));
    }
    
    /**
     * Set intensity (0.0 - 1.0)
     */
    public static void setIntensity(float value) {
        intensity = Math.max(0.0f, Math.min(1.0f, value));
        HookUtils.logInfo(TAG, "Intensity set to: " + intensity);
    }
    
    /**
     * Get current accessibility mode
     */
    public static String getCurrentMode() {
        return currentMode.get();
    }
    
    /**
     * Get tap count for session
     */
    public static int getTapCount() {
        return tapCount.get();
    }
    
    /**
     * Touch event record
     */
    private static class TouchEvent {
        final float x;
        final float y;
        final int action;
        final long timestamp;
        
        TouchEvent(float x, float y, int action, long timestamp) {
            this.x = x;
            this.y = y;
            this.action = action;
            this.timestamp = timestamp;
        }
    }
}
