package com.samsungcloak.xposed;

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
 * FoldableStateTransitionHook - Foldable State Change Issues
 * 
 * Simulates foldable device issues:
 * - Flex mode detection delays
 * - Hinge angle sensor errors
 * - Screen state transition lag
 * - Unfold/fold animation glitches
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class FoldableStateTransitionHook {

    private static final String TAG = "[Foldable][Transition]";
    private static final boolean DEBUG = true;
    
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    
    // Parameters
    private static float flexModeDelay = 150;         // ms
    private static float hingeAngleError = 3;         // degrees
    private static float transitionLagRate = 0.08f;   // 8%
    private static float animationGlitchRate = 0.05f; // 5%
    
    // State
    private static boolean isFolded = false;
    private static boolean isInFlexMode = false;
    private static float currentHingeAngle = 0;
    
    private static final Random random = new Random();
    private static final List<FoldableEvent> foldableEvents = new CopyOnWriteArrayList<>();
    
    public static enum FoldState {
        FOLDED,
        HALF_OPEN,
        UNFOLDED
    }
    
    public static class FoldableEvent {
        public long timestamp;
        public String type;
        public String details;
        
        public FoldableEvent(String type, String details) {
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
        
        HookUtils.logInfo(TAG, "Initializing Foldable State Transition Hook");
        
        try {
            hookFoldStateService(lpparam);
            
            HookUtils.logInfo(TAG, "Foldable hook initialized");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Init failed: " + t.getMessage());
            hookInitialized.set(false);
        }
    }
    
    private static void hookFoldStateService(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> foldStateClass = XposedHelpers.findClass(
                "com.samsung.android.app.fold.FoldStateService", lpparam.classLoader
            );
            
            XposedBridge.hookAllMethods(foldStateClass, "setFoldState",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    FoldState newState = (FoldState) param.args[0];
                    
                    if (random.nextFloat() < transitionLagRate) {
                        foldableEvents.add(new FoldableEvent("TRANSITION_LAG", 
                            "State transition delayed"));
                    }
                    
                    if (random.nextFloat() < animationGlitchRate) {
                        foldableEvents.add(new FoldableEvent("ANIMATION_GLITCH", 
                            "Animation frame dropped"));
                    }
                    
                    isFolded = (newState == FoldState.FOLDED);
                    
                    HookUtils.logDebug(TAG, "Fold state: " + newState);
                }
            });
            
            XposedBridge.hookAllMethods(foldStateClass, "getHingeAngle",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    float angle = (float) param.getResult();
                    currentHingeAngle = angle;
                    
                    // Hinge angle sensor error
                    if (random.nextFloat() < 0.05f) {
                        float error = (random.nextFloat() - 0.5f) * 2 * hingeAngleError;
                        currentHingeAngle += error;
                        
                        foldableEvents.add(new FoldableEvent("HINGE_ERROR", 
                            "Sensor error: " + error + " degrees");
                    }
                }
            });
            
            HookUtils.logInfo(TAG, "FoldStateService hooked");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "Foldable hook skipped: " + t.getMessage());
        }
    }
    
    public static void setEnabled(boolean enabled) {
        FoldableStateTransitionHook.enabled = enabled;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static boolean isFolded() {
        return isFolded;
    }
    
    public static void setFlexMode(boolean flexMode) {
        isInFlexMode = flexMode;
    }
    
    public static List<FoldableEvent> getFoldableEvents() {
        return new ArrayList<>(foldableEvents);
    }
}
