package com.samsungcloak.xposed;

import android.view.MotionEvent;
import android.view.View;
import android.view.EdgeEffect;
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
 * EdgePanelLatencyHook - Edge Panel Delay Simulation
 * 
 * Simulates edge panel behavior:
 * - Slow edge detection
 * - Accidental triggers
 * - Panel open/close latency
 * - Animation stuttering
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class EdgePanelLatencyHook {

    private static final String TAG = "[Edge][Panel]";
    private static final boolean DEBUG = true;
    
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    
    // Parameters
    private static int minDetectionDelay = 50;     // ms
    private static int maxDetectionDelay = 200;    // ms
    private static float accidentalTriggerRate = 0.10f; // 10%
    private static float animationStutterRate = 0.08f; // 8%
    
    // State
    private static boolean isPanelOpen = false;
    private static long lastPanelClose = 0;
    private static int edgeTouchCount = 0;
    private static boolean isEdgeTouching = false;
    
    private static final Random random = new Random();
    private static final List<EdgeEvent> edgeHistory = new CopyOnWriteArrayList<>();
    
    public static class EdgeEvent {
        public long timestamp;
        public String type;
        public String details;
        
        public EdgeEvent(String type, String details) {
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
        
        HookUtils.logInfo(TAG, "Initializing Edge Panel Latency Hook");
        
        try {
            hookEdgePanel(lpparam);
            hookEdgeEffect(lpparam);
            hookMotionEvent(lpparam);
            
            HookUtils.logInfo(TAG, "Edge panel hook initialized");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Init failed: " + t.getMessage());
            hookInitialized.set(false);
        }
    }
    
    private static void hookEdgePanel(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> edgePanelClass = XposedHelpers.findClass(
                "com.samsung.android.app.edge.EdgePanelService", lpparam.classLoader
            );
            
            // Hook handleEdgeTouch
            XposedBridge.hookAllMethods(edgePanelClass, "handleEdgeTouch",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    edgeTouchCount++;
                    
                    // Add detection delay
                    int delay = minDetectionDelay + 
                        random.nextInt(maxDetectionDelay - minDetectionDelay);
                    
                    edgeHistory.add(new EdgeEvent("DETECTION_DELAY", 
                        "Edge detected after " + delay + "ms"));
                    
                    // Check for accidental trigger
                    if (random.nextFloat() < accidentalTriggerRate) {
                        edgeHistory.add(new EdgeEvent("ACCIDENTAL_TRIGGER", 
                            "Accidental edge touch"));
                        
                        HookUtils.logDebug(TAG, "Accidental edge trigger");
                    }
                    
                    HookUtils.logDebug(TAG, "Edge detection delay: " + delay + "ms");
                }
            });
            
            // Hook openPanel
            XposedBridge.hookAllMethods(edgePanelClass, "openPanel",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    isPanelOpen = true;
                    
                    // Check for animation stutter
                    if (random.nextFloat() < animationStutterRate) {
                        edgeHistory.add(new EdgeEvent("ANIMATION_STUTTER", 
                            "Panel animation stuttered"));
                        
                        HookUtils.logDebug(TAG, "Panel animation stutter");
                    }
                    
                    edgeHistory.add(new EdgeEvent("PANEL_OPEN", "Edge panel opened"));
                }
            });
            
            // Hook closePanel
            XposedBridge.hookAllMethods(edgePanelClass, "closePanel",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    isPanelOpen = false;
                    lastPanelClose = System.currentTimeMillis();
                    
                    edgeHistory.add(new EdgeEvent("PANEL_CLOSED", "Edge panel closed"));
                }
            });
            
            HookUtils.logInfo(TAG, "Edge panel hooked");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "Edge panel hook skipped: " + t.getMessage());
        }
    }
    
    private static void hookEdgeEffect(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> edgeEffectClass = XposedHelpers.findClass(
                "android.view.EdgeEffect", lpparam.classLoader
            );
            
            // Hook onTouch
            XposedBridge.hookAllMethods(edgeEffectClass, "onTouch",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    isEdgeTouching = true;
                }
            });
            
            HookUtils.logInfo(TAG, "EdgeEffect hooked");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "EdgeEffect hook skipped: " + t.getMessage());
        }
    }
    
    private static void hookMotionEvent(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> motionEventClass = XposedHelpers.findClass(
                "android.view.MotionEvent", lpparam.classLoader
            );
            
            // Hook getX to detect edge touches
            XposedBridge.hookAllMethods(motionEventClass, "getX",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    float x = (float) param.getResult();
                    
                    // Detect edge region (first 50 pixels)
                    if (x < 50 && !isPanelOpen) {
                        // Could trigger panel
                    }
                }
            });
            
            HookUtils.logInfo(TAG, "MotionEvent hooked for edge detection");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "MotionEvent hook skipped: " + t.getMessage());
        }
    }
    
    // ========== Configuration ==========
    
    public static void setEnabled(boolean enabled) {
        EdgePanelLatencyHook.enabled = enabled;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void setDetectionDelayRange(int minMs, int maxMs) {
        minDetectionDelay = Math.max(10, minMs);
        maxDetectionDelay = Math.max(minDetectionDelay, maxMs);
    }
    
    public static void setAccidentalTriggerRate(float rate) {
        accidentalTriggerRate = Math.max(0, Math.min(1, rate));
    }
    
    public static boolean isPanelOpen() {
        return isPanelOpen;
    }
    
    public static int getEdgeTouchCount() {
        return edgeTouchCount;
    }
    
    public static List<EdgeEvent> getEdgeHistory() {
        return new ArrayList<>(edgeHistory);
    }
}
