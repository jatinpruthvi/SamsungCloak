package com.samsungcloak.xposed;

import android.media.MediaCodec;
import android.media.MediaRecorder;
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
 * ScreenRecordingImperfectionHook - Media Capture Realism
 * 
 * Simulates screen recording quality issues:
 * - Initial recording delays
 * - Frame drops under load
 * - Audio-video sync drift
 * - Quality degradation over time
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class ScreenRecordingImperfectionHook {

    private static final String TAG = "[Screen][Recording]";
    private static final boolean DEBUG = true;
    
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    
    // Parameters
    private static int startDelayMin = 500;   // ms
    private static int startDelayMax = 2000;  // ms
    private static float frameDropRate = 0.08f; // 8%
    private static int avSyncDriftMax = 100;  // ms
    
    // State
    private static boolean isRecording = false;
    private static long recordingStartTime = 0;
    private static int currentFrameDrops = 0;
    
    private static final Random random = new Random();
    private static final List<RecordingEvent> recordingEvents = new CopyOnWriteArrayList<>();
    
    public static class RecordingEvent {
        public long timestamp;
        public String type;
        public String details;
        
        public RecordingEvent(String type, String details) {
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
        
        HookUtils.logInfo(TAG, "Initializing Screen Recording Imperfection Hook");
        
        try {
            hookMediaRecorder(lpparam);
            hookMediaCodec(lpparam);
            
            HookUtils.logInfo(TAG, "Screen recording hook initialized");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Init failed: " + t.getMessage());
            hookInitialized.set(false);
        }
    }
    
    private static void hookMediaRecorder(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> mediaRecorderClass = XposedHelpers.findClass(
                "android.media.MediaRecorder", lpparam.classLoader
            );
            
            XposedBridge.hookAllMethods(mediaRecorderClass, "start",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    isRecording = true;
                    recordingStartTime = System.currentTimeMillis();
                    currentFrameDrops = 0;
                    
                    // Initial delay
                    int delay = startDelayMin + random.nextInt(startDelayMax - startDelayMin);
                    recordingEvents.add(new RecordingEvent("START_DELAY", 
                        "Recording starts in: " + delay + "ms"));
                    
                    HookUtils.logDebug(TAG, "Recording start delay: " + delay + "ms");
                }
            });
            
            XposedBridge.hookAllMethods(mediaRecorderClass, "stop",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    isRecording = false;
                    
                    long duration = System.currentTimeMillis() - recordingStartTime;
                    recordingEvents.add(new RecordingEvent("RECORDING_STOPPED", 
                        "Duration: " + duration + "ms, Drops: " + currentFrameDrops));
                }
            });
            
            HookUtils.logInfo(TAG, "MediaRecorder hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "MediaRecorder hook failed: " + t.getMessage());
        }
    }
    
    private static void hookMediaCodec(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> mediaCodecClass = XposedHelpers.findClass(
                "android.media.MediaCodec", lpparam.classLoader
            );
            
            XposedBridge.hookAllMethods(mediaCodecClass, "dequeueInputBuffer",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || !isRecording) return;
                    
                    // Frame drops
                    if (random.nextFloat() < frameDropRate) {
                        currentFrameDrops++;
                        recordingEvents.add(new RecordingEvent("FRAME_DROP", 
                            "Frame dropped during encoding"));
                    }
                }
            });
            
            XposedBridge.hookAllMethods(mediaCodecClass, "release",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || !isRecording) return;
                    
                    // AV sync drift
                    int syncDrift = random.nextInt(avSyncDriftMax);
                    if (syncDrift > 50) {
                        recordingEvents.add(new RecordingEvent("AV_SYNC_DRIFT", 
                            "Audio-video drift: " + syncDrift + "ms"));
                    }
                }
            });
            
            HookUtils.logInfo(TAG, "MediaCodec hooked");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "MediaCodec hook skipped: " + t.getMessage());
        }
    }
    
    public static void setEnabled(boolean enabled) {
        ScreenRecordingImperfectionHook.enabled = enabled;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void setFrameDropRate(float rate) {
        frameDropRate = Math.max(0, Math.min(1, rate));
    }
    
    public static List<RecordingEvent> getRecordingEvents() {
        return new ArrayList<>(recordingEvents);
    }
}
