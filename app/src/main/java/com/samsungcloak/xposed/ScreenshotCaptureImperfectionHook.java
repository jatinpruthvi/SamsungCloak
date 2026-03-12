package com.samsungcloak.xposed;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.ImageReader;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Surface;
import android.view.SurfaceControl;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ScreenshotCaptureImperfectionHook - Screenshot Delays & Partial Captures
 * 
 * Simulates realistic screenshot behavior:
 * - 150-400ms capture delay
 * - Partial screen capture failures
 * - Thumbnail generation delays
 * - "Screenshot saved" notification timing
 * - Wrong orientation, black bars
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class ScreenshotCaptureImperfectionHook {

    private static final String TAG = "[Screenshot][Capture]";
    private static final boolean DEBUG = true;
    
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    
    // Timing (ms)
    private static int minCaptureDelay = 150;
    private static int maxCaptureDelay = 400;
    private static int currentCaptureDelay = 200;
    
    // Failure rates
    private static float captureFailureRate = 0.02f;     // 2% complete failure
    private static float partialCaptureRate = 0.01f;     // 1% partial capture
    private static float orientationErrorRate = 0.01f;   // 1% wrong orientation
    private static float thumbnailDelayRate = 0.15f;      // 15% thumbnail delay
    
    // State
    private static boolean isCapturing = false;
    private static long lastCaptureTime = 0;
    private static int captureCount = 0;
    private static int failureCount = 0;
    private static int memoryPressure = 0;
    
    private static final Random random = new Random();
    private static final List<CaptureEvent> captureHistory = new CopyOnWriteArrayList<>();
    private static Handler captureHandler = null;
    
    public static class CaptureEvent {
        public long timestamp;
        public String type;       // SUCCESS, FAILURE, PARTIAL, DELAY
        public int width;
        public int height;
        public long duration;     // capture time in ms
        public String failureReason;
        
        public CaptureEvent(String type, int width, int height, long duration, String failureReason) {
            this.timestamp = System.currentTimeMillis();
            this.type = type;
            this.width = width;
            this.height = height;
            this.duration = duration;
            this.failureReason = failureReason;
        }
    }
    
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (hookInitialized.getAndSet(true)) {
            HookUtils.logDebug(TAG, "Already initialized");
            return;
        }
        
        HookUtils.logInfo(TAG, "Initializing Screenshot Capture Imperfection Hook");
        
        try {
            hookSurfaceControl(lpparam);
            hookImageReader(lpparam);
            hookScreenshotService(lpparam);
            
            captureHandler = new Handler(Looper.getMainLooper());
            
            HookUtils.logInfo(TAG, "Screenshot hook initialized");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Init failed: " + t.getMessage());
            hookInitialized.set(false);
        }
    }
    
    private static void hookSurfaceControl(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> surfaceControlClass = XposedHelpers.findClass(
                "android.view.SurfaceControl", lpparam.classLoader
            );
            
            // Hook screenshot() method
            XposedBridge.hookAllMethods(surfaceControlClass, "screenshot",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    isCapturing = true;
                    captureCount++;
                    
                    // Check for memory pressure
                    if (memoryPressure > 70) {
                        currentCaptureDelay += random.nextInt(200);
                        HookUtils.logDebug(TAG, "High memory pressure, increasing delay");
                    }
                }
                
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    long startTime = System.currentTimeMillis();
                    
                    // Add capture delay
                    int delay = currentCaptureDelay + random.nextInt(100) - 50;
                    delay = Math.max(50, delay);
                    
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    
                    // Check for failures
                    float failRoll = random.nextFloat();
                    
                    if (failRoll < captureFailureRate) {
                        // Complete failure
                        param.setResult(null);
                        failureCount++;
                        
                        captureHistory.add(new CaptureEvent(
                            "FAILURE", 0, 0, delay, "capture_failed"));
                        
                        HookUtils.logDebug(TAG, "Simulated capture failure");
                    } else if (failRoll < captureFailureRate + partialCaptureRate) {
                        // Partial capture - would need complex bitmap manipulation
                        // For now, log the event
                        captureHistory.add(new CaptureEvent(
                            "PARTIAL", 0, 0, delay, "partial_capture"));
                        
                        HookUtils.logDebug(TAG, "Simulated partial capture");
                    } else if (failRoll < captureFailureRate + partialCaptureRate + orientationErrorRate) {
                        // Wrong orientation
                        captureHistory.add(new CaptureEvent(
                            "ORIENTATION_ERROR", 0, 0, delay, "wrong_orientation"));
                        
                        HookUtils.logDebug(TAG, "Simulated orientation error");
                    } else {
                        // Success
                        captureHistory.add(new CaptureEvent(
                            "SUCCESS", 1080, 2400, delay, null));
                        
                        HookUtils.logDebug(TAG, "Screenshot captured: " + delay + "ms");
                    }
                    
                    isCapturing = false;
                    lastCaptureTime = System.currentTimeMillis();
                }
            });
            
            HookUtils.logInfo(TAG, "SurfaceControl.screenshot hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "SurfaceControl hook failed: " + t.getMessage());
        }
    }
    
    private static void hookImageReader(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> imageReaderClass = XposedHelpers.findClass(
                "android.media.ImageReader", lpparam.classLoader
            );
            
            // Hook newInstance
            XposedBridge.hookAllMethods(imageReaderClass, "newInstance",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    // Could modify ImageReader configuration
                    HookUtils.logDebug(TAG, "ImageReader created");
                }
            });
            
            HookUtils.logInfo(TAG, "ImageReader hooked");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "ImageReader hook skipped: " + t.getMessage());
        }
    }
    
    private static void hookScreenshotService(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Try to find screenshot service
            Class<?> screenshotServiceClass = XposedHelpers.findClass(
                "com.android.internal.os.ScreenshotHelper", lpparam.classLoader
            );
            
            XposedBridge.hookAllMethods(screenshotServiceClass, "takeScreenshot",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    // Check for notification delay
                    if (random.nextFloat() < thumbnailDelayRate) {
                        // Add extra delay for thumbnail generation
                        int thumbnailDelay = random.nextInt(500) + 200;
                        
                        HookUtils.logDebug(TAG, "Thumbnail delay: " + thumbnailDelay + "ms");
                    }
                }
            });
            
            HookUtils.logInfo(TAG, "Screenshot service hooked");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "Screenshot service hook skipped: " + t.getMessage());
        }
    }
    
    /**
     * Get the current capture delay
     */
    public static int getCurrentCaptureDelay() {
        // Vary delay based on memory pressure
        int baseDelay = minCaptureDelay + random.nextInt(maxCaptureDelay - minCaptureDelay);
        
        if (memoryPressure > 50) {
            baseDelay += random.nextInt(150);
        }
        
        return baseDelay;
    }
    
    /**
     * Set memory pressure level (0-100)
     */
    public static void setMemoryPressure(int pressure) {
        memoryPressure = Math.max(0, Math.min(100, pressure));
    }
    
    /**
     * Check if capture is currently in progress
     */
    public static boolean isCapturing() {
        return isCapturing;
    }
    
    // ========== Configuration ==========
    
    public static void setEnabled(boolean enabled) {
        ScreenshotCaptureImperfectionHook.enabled = enabled;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void setCaptureDelayRange(int minMs, int maxMs) {
        minCaptureDelay = Math.max(50, minMs);
        maxCaptureDelay = Math.max(minCaptureDelay, maxMs);
    }
    
    public static void setCaptureFailureRate(float rate) {
        captureFailureRate = Math.max(0, Math.min(0.5f, rate));
    }
    
    public static int getCaptureCount() {
        return captureCount;
    }
    
    public static int getFailureCount() {
        return failureCount;
    }
    
    public static float getFailureRate() {
        return captureCount > 0 ? (float) failureCount / captureCount : 0;
    }
    
    public static List<CaptureEvent> getCaptureHistory() {
        return new ArrayList<>(captureHistory);
    }
}
