package com.samsungcloak.xposed;

import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * EmotionalStateBehavioralHook - User Emotional State & Behavioral Pattern Simulation
 *
 * Simulates realistic emotional states and their effects on device interaction:
 *
 * 1. Frustration Detection - Rapid repeated taps, aggressive gestures
 * 2. Satisfaction Signals - Slow deliberate interactions, completion actions
 * 3. Hesitation Behavior - Pause before decisions, indecision patterns
 * 4. Engagement Levels - High focus vs distracted interaction
 * 5. Session Arousal - Energy levels affecting interaction speed
 * 6. Cognitive Load - Mental workload affecting response times
 * 7. Impulse vs Deliberate Actions - Quick taps vs considered swipes
 *
 * Novelty: Extends BehavioralStateHook with emotion-specific modeling
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class EmotionalStateBehavioralHook {

    private static final String TAG = "[Emotional][State]";
    private static final boolean DEBUG = true;
    
    // Toggleable via SharedPreferences
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    
    // Emotional states
    public enum EmotionalState {
        NEUTRAL,
        FRUSTRATED,
        SATISFIED,
        HESITANT,
        ENGAGED,
        DISTRACTED,
        IMPATIENT,
        FOCUSED,
        RELAXED,
        ANXIOUS
    }
    
    // Interaction styles
    public enum InteractionStyle {
        DELIBERATE,    // Slow, careful
        IMPULSIVE,     // Quick, rash
        FRUSTRATED,    // Rapid, aggressive
        CAREFUL,       // Precise, accurate
        DISTRACTED     // Inconsistent, variable
    }
    
    // Current emotional state
    private static EmotionalState currentState = EmotionalState.NEUTRAL;
    private static InteractionStyle currentStyle = InteractionStyle.DELIBERATE;
    private static float frustrationLevel = 0.0f; // 0-1
    private static float satisfactionLevel = 0.5f; // 0-1
    private static float engagementLevel = 0.7f; // 0-1
    private static float cognitiveLoad = 0.3f; // 0-1
    private static float energyLevel = 0.8f; // 0-1
    
    // Interaction tracking
    private static final Deque<TouchEvent> recentTouches = new LinkedList<>();
    private static final int maxTouchHistory = 50;
    private static long lastInteractionTime = 0;
    private static long sessionStartTime = System.currentTimeMillis();
    
    // Tap timing analysis
    private static long averageTapInterval = 300; // ms
    private static int rapidTapCount = 0;
    private static long rapidTapStartTime = 0;
    private static final int RAPID_TAP_THRESHOLD = 3;
    
    // Behavioral modifications
    private static boolean frustrationAffectsTouchRate = true;
    private static boolean satisfactionAffectsPrecision = true;
    private static boolean cognitiveLoadAffectsLatency = true;
    private static boolean energyAffectsSpeed = true;
    
    // Event listeners
    private static final List<EmotionalStateListener> listeners = new CopyOnWriteArrayList<>();
    
    // Touch event record
    public static class TouchEvent {
        public float x;
        public float y;
        public long timestamp;
        public int action;
        public float pressure;
        public float size;
        
        public TouchEvent(float x, float y, int action, float pressure, float size) {
            this.x = x;
            this.y = y;
            this.timestamp = System.currentTimeMillis();
            this.action = action;
            this.pressure = pressure;
            this.size = size;
        }
    }
    
    // Listener interface
    public interface EmotionalStateListener {
        void onStateChange(EmotionalState oldState, EmotionalState newState);
        void onFrustrationDetected(float level);
        void onSatisfactionDetected(float level);
    }
    
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (hookInitialized.getAndSet(true)) {
            HookUtils.logDebug(TAG, "Already initialized");
            return;
        }
        
        HookUtils.logInfo(TAG, "Initializing Emotional State Behavioral Hook");
        
        try {
            hookMotionEvent(lpparam);
            hookView(lpparam);
            
            HookUtils.logInfo(TAG, "Emotional hook initialized");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Init failed: " + t.getMessage());
            hookInitialized.set(false);
        }
    }
    
    private static void hookMotionEvent(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> motionEventClass = XposedHelpers.findClass(
                "android.view.MotionEvent", lpparam.classLoader
            );
            
            // Hook action handling to detect tap patterns
            XposedBridge.hookAllMethods(motionEventClass, "getAction",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    int action = (int) param.getResult();
                    
                    // Track ACTION_UP for tap analysis
                    if (action == MotionEvent.ACTION_UP) {
                        analyzeTapPattern();
                    }
                }
            });
            
            // Hook getEventTime for timing analysis
            XposedBridge.hookAllMethods(motionEventClass, "getEventTime",
                new XC_MethodHook() {
                @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    long eventTime = (long) param.getResult();
                    
                    // Apply cognitive load latency
                    if (cognitiveLoadAffectsLatency && cognitiveLoad > 0.5f) {
                        long latency = (long)(cognitiveLoad * 50); // Up to 50ms
                        param.setResult(eventTime - latency);
                    }
                }
            });
            
            HookUtils.logInfo(TAG, "MotionEvent hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "MotionEvent hook failed: " + t.getMessage());
        }
    }
    
    private static void hookView(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> viewClass = XposedHelpers.findClass(
                "android.view.View", lpparam.classLoader
            );
            
            // Hook performClick to detect completion actions
            XposedBridge.hookAllMethods(viewClass, "performClick",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    boolean result = (boolean) param.getResult();
                    if (result) {
                        onInteractionComplete();
                    }
                }
            });
            
            // Hook performLongClick for hesitation detection
            XposedBridge.hookAllMethods(viewClass, "performLongClick",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    boolean result = (boolean) param.getResult();
                    if (result) {
                        // Long click may indicate hesitation or careful consideration
                        onHesitationDetected();
                    }
                }
            });
            
            HookUtils.logInfo(TAG, "View hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "View hook failed: " + t.getMessage());
        }
    }
    
    // ========== Behavioral Analysis ==========
    
    /**
     * Analyze tap patterns to detect emotional state
     */
    private static void analyzeTapPattern() {
        long currentTime = System.currentTimeMillis();
        
        // Add current tap to history
        TouchEvent event = new TouchEvent(0, 0, MotionEvent.ACTION_UP, 0, 0);
        recentTouches.addFirst(event);
        
        // Trim history
        while (recentTouches.size() > maxTouchHistory) {
            recentTouches.removeLast();
        }
        
        // Calculate tap rate
        if (recentTouches.size() >= 2) {
            long interval = currentTime - recentTouches.get(1).timestamp;
            
            // Detect rapid tapping (frustration indicator)
            if (interval < 200) { // Less than 200ms between taps
                if (rapidTapStartTime == 0) {
                    rapidTapStartTime = currentTime;
                    rapidTapCount = 1;
                } else {
                    rapidTapCount++;
                }
                
                if (rapidTapCount >= RAPID_TAP_THRESHOLD) {
                    increaseFrustration(0.1f);
                    rapidTapCount = 0;
                    rapidTapStartTime = 0;
                }
            } else {
                rapidTapCount = 0;
                rapidTapStartTime = 0;
            }
            
            // Update average
            averageTapInterval = (averageTapInterval * 0.7f) + (interval * 0.3f);
        }
        
        // Check for idle time (potential hesitation)
        if (lastInteractionTime > 0) {
            long idleTime = currentTime - lastInteractionTime;
            
            // Long idle before tap might indicate hesitation
            if (idleTime > 2000 && idleTime < 10000) {
                onHesitationDetected();
            }
        }
        
        lastInteractionTime = currentTime;
        
        // Update engagement based on recent activity
        updateEngagementLevel();
    }
    
    /**
     * Called when an interaction completes successfully
     */
    private static void onInteractionComplete() {
        // Successful completion increases satisfaction
        if (satisfactionLevel < 0.9f) {
            satisfactionLevel += 0.05f;
            satisfactionLevel = Math.min(1.0f, satisfactionLevel);
        }
        
        // Decrease frustration on success
        if (frustrationLevel > 0.1f) {
            frustrationLevel -= 0.05f;
            frustrationLevel = Math.max(0.0f, frustrationLevel);
        }
        
        // Notify listeners
        for (EmotionalStateListener listener : listeners) {
            listener.onSatisfactionDetected(satisfactionLevel);
        }
        
        // Check if frustration resolved
        if (frustrationLevel < 0.3f && currentState == EmotionalState.FRUSTRATED) {
            setEmotionalState(EmotionalState.SATISFIED);
        }
    }
    
    /**
     * Called when hesitation is detected
     */
    private static void onHesitationDetected() {
        if (currentState != EmotionalState.HESITANT) {
            setEmotionalState(EmotionalState.HESITANT);
        }
        
        // Increase cognitive load
        if (cognitiveLoad < 0.8f) {
            cognitiveLoad += 0.05f;
        }
    }
    
    /**
     * Increase frustration level
     */
    public static void increaseFrustration(float amount) {
        frustrationLevel += amount;
        frustrationLevel = Math.min(1.0f, frustrationLevel);
        
        // Update state if threshold crossed
        if (frustrationLevel > 0.6f && currentState != EmotionalState.FRUSTRATED) {
            setEmotionalState(EmotionalState.FRUSTRATED);
        }
        
        // Notify listeners
        for (EmotionalStateListener listener : listeners) {
            listener.onFrustrationDetected(frustrationLevel);
        }
    }
    
    /**
     * Update engagement level based on recent activity
     */
    private static void updateEngagementLevel() {
        long currentTime = System.currentTimeMillis();
        long sessionDuration = (currentTime - sessionStartTime) / 60000; // minutes
        
        // Engagement decreases over long sessions
        float sessionDecay = sessionDuration * 0.01f;
        
        // But increases with recent activity
        float recentActivityBoost = 0;
        if (lastInteractionTime > 0) {
            long idleTime = currentTime - lastInteractionTime;
            if (idleTime < 60000) { // Active in last minute
                recentActivityBoost = 0.1f;
            } else if (idleTime < 300000) { // Active in last 5 minutes
                recentActivityBoost = 0.05f;
            }
        }
        
        engagementLevel = engagementLevel * 0.95f + (0.5f + recentActivityBoost - sessionDecay) * 0.05f;
        engagementLevel = Math.max(0.0f, Math.min(1.0f, engagementLevel));
    }
    
    // ========== Behavioral Modifiers ==========
    
    /**
     * Get modified tap interval based on emotional state
     */
    public static long getModifiedTapInterval() {
        long baseInterval = averageTapInterval;
        
        if (currentState == EmotionalState.FRUSTRATED || currentState == EmotionalState.IMPATIENT) {
            // Frustrated users tap faster
            return (long)(baseInterval * 0.6);
        } else if (currentState == EmotionalState.HESITANT || currentState == EmotionalState.FOCUSED) {
            // Hesitant/focused users take longer
            return (long)(baseInterval * 1.5);
        } else if (currentState == EmotionalState.RELAXED) {
            // Relaxed users take their time
            return (long)(baseInterval * 1.2);
        }
        
        return baseInterval;
    }
    
    /**
     * Get modified touch pressure based on emotional state
     */
    public static float getModifiedPressure(float basePressure) {
        if (currentState == EmotionalState.FRUSTRATED) {
            // Frustrated users press harder
            return Math.min(1.0f, basePressure * 1.3f);
        } else if (currentState == EmotionalState.RELAXED || currentState == EmotionalState.SATISFIED) {
            // Relaxed users apply gentle pressure
            return basePressure * 0.8f;
        }
        
        return basePressure;
    }
    
    /**
     * Get modified interaction latency
     */
    public static long getInteractionLatency() {
        long baseLatency = 0;
        
        // Cognitive load adds latency
        if (cognitiveLoadAffectsLatency) {
            baseLatency += cognitiveLoad * 100;
        }
        
        // Energy affects speed
        if (energyAffectsSpeed) {
            if (energyLevel < 0.3f) {
                baseLatency += 50; // Low energy = slower
            }
        }
        
        // State-specific modifications
        switch (currentState) {
            case DISTRACTED:
                baseLatency += 75;
                break;
            case FOCUSED:
                baseLatency -= 25;
                break;
            case IMPATIENT:
                baseLatency -= 50;
                break;
        }
        
        return Math.max(0, baseLatency);
    }
    
    /**
     * Get interaction style based on current state
     */
    public static InteractionStyle getInteractionStyle() {
        if (frustrationLevel > 0.6f) {
            return InteractionStyle.FRUSTRATED;
        } else if (cognitiveLoad > 0.7f) {
            return InteractionStyle.DISTRACTED;
        } else if (engagementLevel > 0.7f && satisfactionLevel > 0.5f) {
            return InteractionStyle.DELIBERATE;
        } else if (energyLevel < 0.4f) {
            return InteractionStyle.DISTRACTED;
        }
        
        return InteractionStyle.CAREFUL;
    }
    
    // ========== State Management ==========
    
    /**
     * Set emotional state and notify listeners
     */
    public static void setEmotionalState(EmotionalState state) {
        if (currentState == state) return;
        
        EmotionalState oldState = currentState;
        currentState = state;
        
        // Update interaction style
        currentStyle = getInteractionStyle();
        
        // Notify listeners
        for (EmotionalStateListener listener : listeners) {
            listener.onStateChange(oldState, state);
        }
        
        HookUtils.logInfo(TAG, "Emotional state: " + oldState + " -> " + state);
    }
    
    /**
     * Reset emotional state to neutral
     */
    public static void resetState() {
        currentState = EmotionalState.NEUTRAL;
        frustrationLevel = 0.0f;
        satisfactionLevel = 0.5f;
        engagementLevel = 0.7f;
        cognitiveLoad = 0.3f;
        rapidTapCount = 0;
        recentTouches.clear();
    }
    
    public static void addListener(EmotionalStateListener listener) {
        listeners.add(listener);
    }
    
    public static void removeListener(EmotionalStateListener listener) {
        listeners.remove(listener);
    }
    
    // ========== Getters ==========
    
    public static EmotionalState getCurrentState() {
        return currentState;
    }
    
    public static InteractionStyle getCurrentStyle() {
        return currentStyle;
    }
    
    public static float getFrustrationLevel() {
        return frustrationLevel;
    }
    
    public static float getSatisfactionLevel() {
        return satisfactionLevel;
    }
    
    public static float getEngagementLevel() {
        return engagementLevel;
    }
    
    public static float getCognitiveLoad() {
        return cognitiveLoad;
    }
    
    public static float getEnergyLevel() {
        return energyLevel;
    }
    
    public static long getAverageTapInterval() {
        return averageTapInterval;
    }
    
    public static void setEnabled(boolean enabled) {
        EmotionalStateBehavioralHook.enabled = enabled;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
}
