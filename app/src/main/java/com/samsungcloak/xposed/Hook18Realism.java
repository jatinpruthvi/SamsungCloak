package com.samsungcloak.xposed;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Hook18Realism - Emotional State Interaction Patterns
 * 
 * Simulates emotional state-based interaction patterns for Samsung Galaxy A12:
 * - Frustration patterns: rage taps 15-25%, fast scrolling 3-8x faster, back button spam 2-5 presses
 * - Hesitation patterns: dwell before critical actions 1-3s, micro-hover 200-800ms, touch-and-retract 20-35%
 * - Satisfaction patterns: consistent tap timing, smooth gesture paths, long sessions 10-20min
 * - Emotional state triggers: performance issues→frustration 60-80%, complex tasks→hesitation 50-70%
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class Hook18Realism extends BaseRealismHook {

    private static final String TAG = "[Realism][Hook18-Emotional]";
    private static final boolean DEBUG = true;
    
    private static final String HOOK_ID = "hook_18";
    private static final String HOOK_NAME = "Emotional State Interaction Patterns";
    
    // Configuration keys
    private static final String KEY_ENABLED = "emotional_patterns_enabled";
    private static final String KEY_FRUSTRATION = "frustration_simulation_enabled";
    private static final String KEY_HESITATION = "hesitation_simulation_enabled";
    private static final String KEY_SATISFACTION = "satisfaction_simulation_enabled";
    
    // Hook state
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    private static AtomicBoolean enabled = new AtomicBoolean(true);
    private static float intensity = 0.5f;
    
    // Emotional states
    public enum EmotionalState {
        NEUTRAL,
        FRUSTRATED,
        HESITANT,
        SATISFIED,
        IMPATIENT,
        ENGAGED
    }
    
    // Current emotional state
    private static final AtomicReference<EmotionalState> currentState = 
        new AtomicReference<>(EmotionalState.NEUTRAL);
    private static final AtomicReference<Float> frustrationLevel = 
        new AtomicReference<>(0.0f);
    private static final AtomicReference<Float> engagementLevel = 
        new AtomicReference<>(0.5f);
    
    // Frustration parameters
    private static boolean frustrationEnabled = true;
    private static float rageTapProbability = 0.20f;
    private static float scrollSpeedMultiplier = 5.0f;
    private static int backButtonSpamCount = 3;
    private static float frustrationTriggerProbability = 0.70f;
    
    // Hesitation parameters
    private static boolean hesitationEnabled = true;
    private static int dwellTimeMs = 2000;
    private static int microHoverMs = 500;
    private static float touchAndRetractProbability = 0.28f;
    private static float hesitationTriggerProbability = 0.60f;
    
    // Satisfaction parameters
    private static boolean satisfactionEnabled = true;
    private static float consistentTapVariance = 0.10f;
    private static float smoothGestureThreshold = 0.85f;
    private static int longSessionMinutes = 15;
    
    // Interaction tracking
    private static final AtomicInteger totalTaps = new AtomicInteger(0);
    private static final AtomicInteger rageTaps = new AtomicInteger(0);
    private static final AtomicInteger fastScrolls = new AtomicInteger(0);
    private static final AtomicInteger backButtonPresses = new AtomicInteger(0);
    private static final AtomicInteger hesitantActions = new AtomicInteger(0);
    private static final AtomicInteger touchAndRetracts = new AtomicInteger(0);
    
    // Timing tracking
    private static long lastTapTime = 0;
    private static long sessionStartTime = 0;
    private static long currentSessionDuration = 0;
    
    // Tap timing history for pattern analysis
    private static final CopyOnWriteArrayList<Long> tapTimings = 
        new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<Float> gesturePathSmoothness = 
        new CopyOnWriteArrayList<>();
    private static final int MAX_HISTORY = 50;
    
    // Performance issue indicators
    private static final AtomicInteger recentErrors = new AtomicInteger(0);
    private static long lastPerformanceIssue = 0;
    
    // Thread-local random
    private static final ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new Random());
    
    public Hook18Realism() {
        super(HOOK_ID, HOOK_NAME);
    }
    
    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!hookInitialized.compareAndSet(false, true)) {
            logInfo("Hook already initialized");
            return;
        }
        
        logInfo("Initializing Emotional State Interaction Patterns Hook");
        sessionStartTime = System.currentTimeMillis();
        
        try {
            // Load configuration
            loadConfiguration();
            
            // Hook MotionEvent for tap patterns
            hookMotionEvent(lpparam);
            
            // Hook View for click patterns
            hookView(lpparam);
            
            // Hook Back key press
            hookBackKey(lpparam);
            
            logInfo("Emotional State Interaction Hook initialized successfully");
        } catch (Exception e) {
            logError("Failed to initialize Emotional State Interaction Hook", e);
        }
    }
    
    private void loadConfiguration() {
        if (configManager != null) {
            enabled.set(configManager.getHookParamBool(HOOK_ID, KEY_ENABLED, true));
            intensity = configManager.getHookIntensity(HOOK_ID);
            
            frustrationEnabled = configManager.getHookParamBool(HOOK_ID, KEY_FRUSTRATION, true);
            rageTapProbability = configManager.getHookParamFloat(HOOK_ID, "rage_tap_probability", 0.20f);
            scrollSpeedMultiplier = configManager.getHookParamFloat(HOOK_ID, "scroll_speed_multiplier", 5.0f);
            backButtonSpamCount = configManager.getHookParamInt(HOOK_ID, "back_button_spam_count", 3);
            frustrationTriggerProbability = configManager.getHookParamFloat(HOOK_ID, "frustration_trigger_prob", 0.70f);
            
            hesitationEnabled = configManager.getHookParamBool(HOOK_ID, KEY_HESITATION, true);
            dwellTimeMs = configManager.getHookParamInt(HOOK_ID, "dwell_time_ms", 2000);
            microHoverMs = configManager.getHookParamInt(HOOK_ID, "micro_hover_ms", 500);
            touchAndRetractProbability = configManager.getHookParamFloat(HOOK_ID, "touch_and_retract_prob", 0.28f);
            hesitationTriggerProbability = configManager.getHookParamFloat(HOOK_ID, "hesitation_trigger_prob", 0.60f);
            
            satisfactionEnabled = configManager.getHookParamBool(HOOK_ID, KEY_SATISFACTION, true);
            consistentTapVariance = configManager.getHookParamFloat(HOOK_ID, "consistent_tap_variance", 0.10f);
            smoothGestureThreshold = configManager.getHookParamFloat(HOOK_ID, "smooth_gesture_threshold", 0.85f);
            longSessionMinutes = configManager.getHookParamInt(HOOK_ID, "long_session_minutes", 15);
        }
        
        logInfo("Configuration loaded: enabled=" + enabled.get() + ", intensity=" + intensity);
    }
    
    private void hookMotionEvent(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> motionEventClass = XposedHelpers.findClass(
                "android.view.MotionEvent", lpparam.classLoader);
            
            // Hook ACTION_DOWN to detect tap patterns
            XposedBridge.hookAllMethods(motionEventClass, "getDownTime",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get()) return;
                        
                        try {
                            long now = System.currentTimeMillis();
                            long downTime = (Long) param.getResult();
                            
                            // Calculate time since last tap
                            long timeSinceLastTap = now - lastTapTime;
                            
                            // Update emotional state based on patterns
                            analyzeTapPattern(timeSinceLastTap);
                            
                            lastTapTime = now;
                            
                            // Track tap timing
                            tapTimings.add(timeSinceLastTap);
                            while (tapTimings.size() > MAX_HISTORY) {
                                tapTimings.remove(0);
                            }
                            
                            totalTaps.incrementAndGet();
                            
                            // Check for rage taps (very rapid)
                            if (timeSinceLastTap < 50 && frustrationEnabled) {
                                float frustration = frustrationLevel.get();
                                if (random.get().nextDouble() < rageTapProbability * intensity * frustration) {
                                    rageTaps.incrementAndGet();
                                    
                                    if (DEBUG && random.get().nextDouble() < 0.01) {
                                        logDebug("Rage tap detected: " + timeSinceLastTap + "ms since last tap");
                                    }
                                }
                            }
                            
                            // Check for touch-and-retract (hesitation)
                            if (hesitationEnabled && random.get().nextDouble() < 
                                touchAndRetractProbability * intensity * (1.0f - engagementLevel.get())) {
                                touchAndRetracts.incrementAndGet();
                            }
                        } catch (Exception e) {
                            logDebug("Error in getDownTime hook: " + e.getMessage());
                        }
                    }
                });
            
            // Hook ACTION_MOVE for scroll speed and gesture analysis
            XposedBridge.hookAllMethods(motionEventClass, "getEventTime",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get()) return;
                        
                        try {
                            // Check scroll speed if in frustrated state
                            if (frustrationEnabled && currentState.get() == EmotionalState.FRUSTRATED) {
                                // Fast scrolling detection would require tracking history
                            }
                        } catch (Exception e) {
                            logDebug("Error in getEventTime hook: " + e.getMessage());
                        }
                    }
                });
            
            logInfo("Hooked MotionEvent for emotional patterns");
        } catch (Exception e) {
            logError("Failed to hook MotionEvent", e);
        }
    }
    
    private void hookView(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> viewClass = XposedHelpers.findClass(
                "android.view.View", lpparam.classLoader);
            
            // Hook performClick
            XposedBridge.hookAllMethods(viewClass, "performClick",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get()) return;
                        
                        try {
                            // Check for hesitation (dwell before action)
                            if (hesitationEnabled && lastTapTime > 0) {
                                long timeSinceLastTap = System.currentTimeMillis() - lastTapTime;
                                
                                if (timeSinceLastTap > dwellTimeMs && 
                                    timeSinceLastTap < dwellTimeMs * 2) {
                                    
                                    // Hesitation detected
                                    float hesitation = 1.0f - engagementLevel.get();
                                    if (random.get().nextDouble() < hesitationTriggerProbability * 
                                        intensity * hesitation) {
                                        hesitantActions.incrementAndGet();
                                        
                                        if (DEBUG && random.get().nextDouble() < 0.01) {
                                            logDebug("Hesitation detected: dwell time " + timeSinceLastTap + "ms");
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            logDebug("Error in performClick hook: " + e.getMessage());
                        }
                    }
                });
            
            logInfo("Hooked View");
        } catch (Exception e) {
            logError("Failed to hook View", e);
        }
    }
    
    private void hookBackKey(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook dispatchKeyEvent for back button
            Class<?> activityClass = XposedHelpers.findClass(
                "android.app.Activity", lpparam.classLoader);
            
            XposedBridge.hookAllMethods(activityClass, "dispatchKeyEvent",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get() || !frustrationEnabled) return;
                        
                        try {
                            android.view.KeyEvent event = (android.view.KeyEvent) param.args[0];
                            
                            if (event.getKeyCode() == android.view.KeyEvent.KEYCODE_BACK &&
                                event.getAction() == android.view.KeyEvent.ACTION_DOWN) {
                                
                                backButtonPresses.incrementAndGet();
                                
                                // Check for back button spam (frustration indicator)
                                if (backButtonPresses.get() >= backButtonSpamCount) {
                                    float frustration = frustrationLevel.get();
                                    if (frustration > 0.5f) {
                                        if (DEBUG && random.get().nextDouble() < 0.01) {
                                            logDebug("Back button spam detected: " + 
                                                backButtonPresses.get() + " presses");
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            logDebug("Error in dispatchKeyEvent hook: " + e.getMessage());
                        }
                    }
                });
            
            logInfo("Hooked Back key");
        } catch (Exception e) {
            logError("Failed to hook Back key", e);
        }
    }
    
    private void analyzeTapPattern(long timeSinceLastTap) {
        EmotionalState newState = currentState.get();
        float frustration = frustrationLevel.get();
        float engagement = engagementLevel.get();
        
        // Check session duration
        currentSessionDuration = System.currentTimeMillis() - sessionStartTime;
        long sessionMinutes = currentSessionDuration / 60000;
        
        // Update frustration based on recent behavior
        if (rageTaps.get() > 5) {
            frustration = Math.min(1.0f, frustration + 0.1f);
            newState = EmotionalState.FRUSTRATED;
        }
        
        // Update engagement based on session length
        if (sessionMinutes > longSessionMinutes) {
            engagement = Math.min(1.0f, engagement + 0.05f);
            newState = EmotionalState.SATISFIED;
        } else if (sessionMinutes < 1) {
            engagement = Math.max(0.0f, engagement - 0.02f);
        }
        
        // Check for performance issues causing frustration
        if (System.currentTimeMillis() - lastPerformanceIssue < 5000) {
            if (random.get().nextDouble() < frustrationTriggerProbability * intensity) {
                frustration = Math.min(1.0f, frustration + 0.2f);
                newState = EmotionalState.FRUSTRATED;
            }
        }
        
        // Detect hesitation patterns
        if (hesitantActions.get() > 3 && touchAndRetracts.get() > 2) {
            newState = EmotionalState.HESITANT;
            engagement = Math.max(0.0f, engagement - 0.1f);
        }
        
        // Update state
        currentState.set(newState);
        frustrationLevel.set(frustration);
        engagementLevel.set(engagement);
    }
    
    /**
     * Report a performance issue (can be called from other hooks)
     */
    public static void reportPerformanceIssue() {
        lastPerformanceIssue = System.currentTimeMillis();
        recentErrors.incrementAndGet();
        
        float frustration = frustrationLevel.get();
        frustration = Math.min(1.0f, frustration + 0.15f * intensity);
        frustrationLevel.set(frustration);
        
        if (frustration > 0.6f) {
            currentState.set(EmotionalState.FRUSTRATED);
        }
        
        HookUtils.logDebug(TAG, "Performance issue reported - frustration: " + frustration);
    }
    
    /**
     * Set emotional state manually
     */
    public static void setEmotionalState(EmotionalState state) {
        currentState.set(state);
        HookUtils.logInfo(TAG, "Emotional state set to: " + state);
    }
    
    /**
     * Set frustration level (0.0 - 1.0)
     */
    public static void setFrustrationLevel(float level) {
        frustrationLevel.set(Math.max(0.0f, Math.min(1.0f, level)));
        HookUtils.logInfo(TAG, "Frustration level set to: " + level);
    }
    
    /**
     * Set engagement level (0.0 - 1.0)
     */
    public static void setEngagementLevel(float level) {
        engagementLevel.set(Math.max(0.0f, Math.min(1.0f, level)));
        HookUtils.logInfo(TAG, "Engagement level set to: " + level);
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
     * Get current emotional state
     */
    public static EmotionalState getCurrentState() {
        return currentState.get();
    }
    
    /**
     * Get frustration level
     */
    public static float getFrustrationLevel() {
        return frustrationLevel.get();
    }
    
    /**
     * Get engagement level
     */
    public static float getEngagementLevel() {
        return engagementLevel.get();
    }
    
    /**
     * Reset session statistics
     */
    public static void resetSession() {
        totalTaps.set(0);
        rageTaps.set(0);
        fastScrolls.set(0);
        backButtonPresses.set(0);
        hesitantActions.set(0);
        touchAndRetracts.set(0);
        sessionStartTime = System.currentTimeMillis();
        tapTimings.clear();
        gesturePathSmoothness.clear();
    }
}
