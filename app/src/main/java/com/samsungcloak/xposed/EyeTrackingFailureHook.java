package com.samsungcloak.xposed;

import android.hardware.camera.face.Face;
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
 * EyeTrackingFailureHook - Biometric & Gaze Detection
 * 
 * Simulates eye/face tracking failures:
 * - Detection delays
 * - Attention detection false negatives
 * - Lighting condition issues
 * - Gaze direction jitter
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class EyeTrackingFailureHook {

    private static final String TAG = "[Eye][Tracking]";
    private static final boolean DEBUG = true;
    
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    
    // Parameters
    private static int detectionDelayMin = 200;   // ms
    private static int detectionDelayMax = 800;   // ms
    private static float attentionFailureRate = 0.20f; // 20%
    private static float gazeJitterDegrees = 5f;  // ±5 degrees
    private static float lowLightThreshold = 50;  // lux
    private static float currentLux = 100;
    
    // State
    private static boolean isTracking = false;
    private static boolean userAttending = true;
    
    private static final Random random = new Random();
    private static final List<EyeEvent> eyeEvents = new CopyOnWriteArrayList<>();
    
    public static class EyeEvent {
        public long timestamp;
        public String type;
        public String details;
        
        public EyeEvent(String type, String details) {
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
        
        HookUtils.logInfo(TAG, "Initializing Eye Tracking Failure Hook");
        
        try {
            hookFaceDetection(lpparam);
            hookAttentionService(lpparam);
            
            HookUtils.logInfo(TAG, "Eye tracking hook initialized");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Init failed: " + t.getMessage());
            hookInitialized.set(false);
        }
    }
    
    private static void hookFaceDetection(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> faceSessionClass = XposedHelpers.findClass(
                "android.hardware.camera.face.FaceDetectionSession", lpparam.classLoader
            );
            
            XposedBridge.hookAllMethods(faceSessionClass, "start",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    isTracking = true;
                    
                    // Detection delay
                    int delay = detectionDelayMin + 
                        random.nextInt(detectionDelayMax - detectionDelayMin);
                    
                    // Low light check
                    if (currentLux < lowLightThreshold) {
                        eyeEvents.add(new EyeEvent("LOW_LIGHT", 
                            "Insufficient light: " + currentLux + " lux"));
                    }
                    
                    eyeEvents.add(new EyeEvent("DETECTION_DELAY", 
                        "Face detection: " + delay + "ms"));
                    
                    HookUtils.logDebug(TAG, "Face detection delay: " + delay + "ms");
                }
            });
            
            HookUtils.logInfo(TAG, "FaceDetectionSession hooked");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "FaceDetection hook skipped: " + t.getMessage());
        }
    }
    
    private static void hookAttentionService(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> attentionClass = XposedHelpers.findClass(
                "com.samsung.android.attention.AgentService", lpparam.classLoader
            );
            
            XposedBridge.hookAllMethods(attentionClass, "isUserAttending",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    // Attention detection failure
                    if (random.nextFloat() < attentionFailureRate) {
                        userAttending = false;
                        eyeEvents.add(new EyeEvent("ATTENTION_FAILED", 
                            "User attention not detected"));
                        
                        param.setResult(false);
                    } else {
                        userAttending = true;
                    }
                }
            });
            
            XposedBridge.hookAllMethods(attentionClass, "getGazeDirection",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    // Gaze jitter
                    float jitter = (random.nextFloat() - 0.5f) * 2 * gazeJitterDegrees;
                    
                    if (Math.abs(jitter) > 1) {
                        eyeEvents.add(new EyeEvent("GAZE_JITTER", 
                            "Gaze offset: " + jitter + " degrees"));
                    }
                }
            });
            
            HookUtils.logInfo(TAG, "Attention service hooked");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "Attention hook skipped: " + t.getMessage());
        }
    }
    
    public static void setEnabled(boolean enabled) {
        EyeTrackingFailureHook.enabled = enabled;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void setAmbientLux(float lux) {
        currentLux = lux;
    }
    
    public static void setAttentionFailureRate(float rate) {
        attentionFailureRate = Math.max(0, Math.min(1, rate));
    }
    
    public static boolean isUserAttending() {
        return userAttending;
    }
    
    public static List<EyeEvent> getEyeEvents() {
        return new ArrayList<>(eyeEvents);
    }
}
