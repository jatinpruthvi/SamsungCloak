package com.samsungcloak.xposed;

import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * EmotionalStateSimulationHook - User Behavior & Emotional Context
 * 
 * Simulates user emotional states affecting interactions:
 * - Hesitation before critical actions
 * - Frustration (rapid repeated taps)
 * - Satisfaction (deliberate slow interactions)
 * - Accidental dismissals when frustrated
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class EmotionalStateSimulationHook {

    private static final String TAG = "[Emotional][State]";
    private static final boolean DEBUG = true;
    
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    
    // Parameters
    private static int hesitationDelayMin = 200;   // ms
    private static int hesitationDelayMax = 1000;  // ms
    private static float frustrationThreshold = 5; // rapid taps
    private static float frustrationWindowMs = 2000; // 2 seconds
    
    // State
    public enum EmotionalState {
        NEUTRAL,
        HESITANT,
        FRUSTRATED,
        SATISFIED,
        CALM
    }
    
    private static EmotionalState currentState = EmotionalState.NEUTRAL;
    private static int rapidTapCount = 0;
    private static long lastTapTime = 0;
    private static long interactionStartTime = 0;
    private static long averageInteractionMs = 0;
    
    private static final Random random = new Random();
    private static final List<EmotionalEvent> emotionalEvents = new CopyOnWriteArrayList<>();
    private static final AtomicLong hesitationDelay = new AtomicLong(0);
    
    public static class EmotionalEvent {
        public long timestamp;
        public String type;
        public String state;
        public String details;
        
        public EmotionalEvent(String type, String state, String details) {
            this.timestamp = System.currentTimeMillis();
            this.type = type;
            this.state = state;
            this.details = details;
        }
    }
    
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (hookInitialized.getAndSet(true)) {
            HookUtils.logDebug(TAG, "Already initialized");
            return;
        }
        
        HookUtils.logInfo(TAG, "Initializing Emotional State Simulation Hook");
        
        try {
            hookViewClick(lpparam);
            hookKeyEvent(lpparam);
            
            HookUtils.logInfo(TAG, "Emotional state hook initialized");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Init failed: " + t.getMessage());
            hookInitialized.set(false);
        }
    }
    
    private static void hookViewClick(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> viewClass = XposedHelpers.findClass(
                "android.view.View", lpparam.classLoader
            );
            
            XposedBridge.hookAllMethods(viewClass, "performClick",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    long now = System.currentTimeMillis();
                    
                    // Track rapid taps for frustration detection
                    if (now - lastTapTime < frustrationWindowMs) {
                        rapidTapCount++;
                    } else {
                        rapidTapCount = 1;
                    }
                    lastTapTime = now;
                    
                    // Determine emotional state
                    if (rapidTapCount >= frustrationThreshold) {
                        currentState = EmotionalState.FRUSTRATED;
                        emotionalEvents.add(new EmotionalEvent("FRUSTRATION_DETECTED", 
                            "FRUSTRATED", "Rapid taps: " + rapidTapCount));
                    } else if (hesitationDelay.get() > 500) {
                        currentState = EmotionalState.HESITANT;
                    } else if (averageInteractionMs > 2000) {
                        currentState = EmotionalState.SATISFIED;
                    } else {
                        currentState = EmotionalState.NEUTRAL;
                    }
                    
                    // Inject hesitation delay for important views
                    if (currentState == EmotionalState.HESITANT) {
                        int delay = hesitationDelayMin + 
                            random.nextInt(hesitationDelayMax - hesitationDelayMin);
                        hesitationDelay.set(delay);
                        
                        emotionalEvents.add(new EmotionalEvent("HESITATION", 
                            "HESITANT", "Pre-action delay: " + delay + "ms"));
                    }
                    
                    // Frustration: accidental dismissals
                    if (currentState == EmotionalState.FRUSTRATED && random.nextFloat() < 0.3f) {
                        emotionalEvents.add(new EmotionalEvent("ACCIDENTAL_DISMISS", 
                            "FRUSTRATED", "User accidentally dismissed content"));
                    }
                    
                    HookUtils.logDebug(TAG, "Click - State: " + currentState + ", Taps: " + rapidTapCount);
                }
            });
            
            HookUtils.logInfo(TAG, "View performClick hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "View click hook failed: " + t.getMessage());
        }
    }
    
    private static void hookKeyEvent(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> keyEventClass = XposedHelpers.findClass(
                "android.view.KeyEvent", lpparam.classLoader
            );
            
            XposedBridge.hookAllMethods(keyEventClass, "dispatch",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    // Track interaction timing
                    long now = System.currentTimeMillis();
                    if (interactionStartTime == 0) {
                        interactionStartTime = now;
                    }
                    
                    long interactionDuration = now - interactionStartTime;
                    averageInteractionMs = (averageInteractionMs + interactionDuration) / 2;
                }
            });
            
            HookUtils.logInfo(TAG, "KeyEvent hooked");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "KeyEvent hook skipped: " + t.getMessage());
        }
    }
    
    public static void setEnabled(boolean enabled) {
        EmotionalStateSimulationHook.enabled = enabled;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static EmotionalState getCurrentState() {
        return currentState;
    }
    
    public static void setFrustrated(boolean frustrated) {
        if (frustrated) {
            currentState = EmotionalState.FRUSTRATED;
            rapidTapCount = (int) frustrationThreshold + 1;
        }
    }
    
    public static void resetState() {
        currentState = EmotionalState.NEUTRAL;
        rapidTapCount = 0;
        interactionStartTime = 0;
    }
    
    public static List<EmotionalEvent> getEmotionalEvents() {
        return new ArrayList<>(emotionalEvents);
    }
}
