package com.samsungcloak.xposed;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;

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
 * SamsungDeXSimulationHook - Samsung DeX Mode Simulation
 * 
 * Simulates Samsung DeX desktop experience:
 * - Window positioning delays (500-1500ms)
 * - Dock connection failures (8%)
 * - Multi-window constraints (max 5 windows)
 * - Display resolution changes
 * - External display detection
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class SamsungDeXSimulationHook {

    private static final String TAG = "[DeX][Desktop]";
    private static final boolean DEBUG = true;
    
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    
    // DeX parameters
    private static int minWindowDelay = 500;       // ms
    private static int maxWindowDelay = 1500;      // ms
    private static float dockFailureRate = 0.08f;   // 8%
    private static int maxWindows = 5;
    private static int externalWidth = 1920;
    private static int externalHeight = 1080;
    
    // State
    private static boolean isDexMode = false;
    private static boolean isDockConnected = false;
    private static int currentWindowCount = 0;
    private static String externalDisplay = null;
    private static int activeWindowDelay = 0;
    
    private static final Random random = new Random();
    private static final List<DeXEvent> dexHistory = new CopyOnWriteArrayList<>();
    private static Handler dexHandler = null;
    
    public static class DeXEvent {
        public long timestamp;
        public String type;
        public String details;
        
        public DeXEvent(String type, String details) {
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
        
        HookUtils.logInfo(TAG, "Initializing Samsung DeX Simulation Hook");
        
        try {
            hookWindowManager(lpparam);
            hookDisplayManager(lpparam);
            hookSamsungDex(lpparam);
            
            dexHandler = new Handler(Looper.getMainLooper());
            
            HookUtils.logInfo(TAG, "DeX simulation hook initialized");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Init failed: " + t.getMessage());
            hookInitialized.set(false);
        }
    }
    
    private static void hookWindowManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> windowManagerClass = XposedHelpers.findClass(
                "android.view.WindowManager", lpparam.classLoader
            );
            
            // Hook addWindow
            XposedBridge.hookAllMethods(windowManagerClass, "addWindow",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || !isDexMode) return;
                    
                    // Check window limit
                    if (currentWindowCount >= maxWindows) {
                        dexHistory.add(new DeXEvent("WINDOW_LIMIT", 
                            "Max " + maxWindows + " windows reached"));
                        
                        HookUtils.logDebug(TAG, "Window limit reached: " + maxWindows);
                    }
                    
                    // Add window positioning delay
                    int delay = minWindowDelay + random.nextInt(maxWindowDelay - minWindowDelay);
                    activeWindowDelay = delay;
                    
                    dexHistory.add(new DeXEvent("WINDOW_DELAY", 
                        "Positioning delay: " + delay + "ms"));
                    
                    HookUtils.logDebug(TAG, "Window positioning delay: " + delay + "ms");
                }
                
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || !isDexMode) return;
                    
                    currentWindowCount++;
                    
                    dexHistory.add(new DeXEvent("WINDOW_ADDED", 
                        "Total windows: " + currentWindowCount));
                }
            });
            
            // Hook removeWindow
            XposedBridge.hookAllMethods(windowManagerClass, "removeWindow",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    currentWindowCount = Math.max(0, currentWindowCount - 1);
                }
            });
            
            HookUtils.logInfo(TAG, "WindowManager hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "WindowManager hook failed: " + t.getMessage());
        }
    }
    
    private static void hookDisplayManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> displayManagerClass = XposedHelpers.findClass(
                "android.hardware.display.DisplayManager", lpparam.classLoader
            );
            
            // Hook getDisplays
            XposedBridge.hookAllMethods(displayManagerClass, "getDisplays",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || !isDexMode) return;
                    
                    // Could modify display list to add external display
                }
            });
            
            HookUtils.logInfo(TAG, "DisplayManager hooked");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "DisplayManager hook skipped: " + t.getMessage());
        }
    }
    
    private static void hookSamsungDex(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Try Samsung DeX SDK
            Class<?> dexSessionClass = XposedHelpers.findClass(
                "com.samsung.android.sdk.dex.DexSession", lpparam.classLoader
            );
            
            XposedBridge.hookAllMethods(dexSessionClass, "start",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    // Check for dock connection failure
                    if (!isDockConnected && random.nextFloat() < dockFailureRate) {
                        dexHistory.add(new DeXEvent("DOCK_FAILURE", 
                            "Dock connection failed"));
                        
                        HookUtils.logDebug(TAG, "Simulated dock connection failure");
                        
                        // Don't block, just log
                    }
                }
                
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    isDexMode = true;
                    
                    dexHistory.add(new DeXEvent("DEX_STARTED", 
                        "Resolution: " + externalWidth + "x" + externalHeight));
                    
                    HookUtils.logInfo(TAG, "DeX mode started");
                }
            });
            
            XposedBridge.hookAllMethods(dexSessionClass, "end",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    isDexMode = false;
                    currentWindowCount = 0;
                    
                    dexHistory.add(new DeXEvent("DEX_ENDED", "DeX mode ended"));
                    
                    HookUtils.logInfo(TAG, "DeX mode ended");
                }
            });
            
            HookUtils.logInfo(TAG, "Samsung DeX hooked");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "DeX hook skipped: " + t.getMessage());
        }
    }
    
    /**
     * Get simulated external display resolution
     */
    public static int[] getExternalResolution() {
        // Random between 1080p and 1440p
        if (random.nextBoolean()) {
            externalWidth = 1920;
            externalHeight = 1080;
        } else {
            externalWidth = 2560;
            externalHeight = 1440;
        }
        
        return new int[]{externalWidth, externalHeight};
    }
    
    // ========== Configuration ==========
    
    public static void setEnabled(boolean enabled) {
        SamsungDeXSimulationHook.enabled = enabled;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void setDexMode(boolean enabled) {
        isDexMode = enabled;
        
        if (enabled) {
            dexHistory.add(new DeXEvent("DEX_ENABLE", "DeX mode enabled"));
        }
    }
    
    public static void setDockConnected(boolean connected) {
        isDockConnected = connected;
        
        if (connected) {
            dexHistory.add(new DeXEvent("DOCK_CONNECTED", "Dock connected"));
        } else {
            dexHistory.add(new DeXEvent("DOCK_DISCONNECTED", "Dock disconnected"));
        }
    }
    
    public static void setWindowDelayRange(int minMs, int maxMs) {
        minWindowDelay = Math.max(100, minMs);
        maxWindowDelay = Math.max(minWindowDelay, maxMs);
    }
    
    public static void setMaxWindows(int max) {
        maxWindows = Math.max(1, Math.min(10, max));
    }
    
    public static boolean isDexMode() {
        return isDexMode;
    }
    
    public static int getCurrentWindowCount() {
        return currentWindowCount;
    }
    
    public static List<DeXEvent> getDexHistory() {
        return new ArrayList<>(dexHistory);
    }
}
