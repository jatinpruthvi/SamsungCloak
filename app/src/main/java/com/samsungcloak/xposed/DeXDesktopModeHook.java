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
 * DeXDesktopModeHook - Desktop Experience Simulation
 * 
 * Simulates Samsung DeX desktop mode limitations:
 * - Connection delays/failures
 * - Window management constraints
 * - App compatibility issues
 * - Display output problems
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 * Note: DeX not available on A12, hook simulates for completeness
 */
public class DeXDesktopModeHook {

    private static final String TAG = "[DeX][DesktopMode]";
    private static final boolean DEBUG = true;
    
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    
    // Parameters
    private static int connectionDelayMin = 1000;   // ms
    private static int connectionDelayMax = 5000;   // ms
    private static float connectionFailureRate = 0.10f; // 10%
    private static int maxWindows = 5;
    private static float appNotOptimizedRate = 0.15f; // 15%
    
    // State
    private static boolean isDexConnected = false;
    private static int currentWindowCount = 0;
    private static String connectionType = null;
    
    private static final Random random = new Random();
    private static final List<DeXEvent> dexEvents = new CopyOnWriteArrayList<>();
    
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
        
        HookUtils.logInfo(TAG, "Initializing DeX Desktop Mode Hook");
        
        try {
            hookDexPluginManager(lpparam);
            hookWindowManager(lpparam);
            
            // DeX available for simulation even if not on A12 hardware
            HookUtils.logInfo(TAG, "DeX hook initialized");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Init failed: " + t.getMessage());
            hookInitialized.set(false);
        }
    }
    
    private static void hookDexPluginManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> dexClass = XposedHelpers.findClass(
                "com.samsung.android.dex.DexPluginManager", lpparam.classLoader
            );
            
            XposedBridge.hookAllMethods(dexClass, "connect",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    // Connection delay
                    int delay = connectionDelayMin + 
                        random.nextInt(connectionDelayMax - connectionDelayMin);
                    
                    // Connection failure
                    if (random.nextFloat() < connectionFailureRate) {
                        dexEvents.add(new DeXEvent("CONNECTION_FAILED", 
                            "DeX connection failed"));
                        return;
                    }
                    
                    isDexConnected = true;
                    dexEvents.add(new DeXEvent("CONNECTING", 
                        "Connection delay: " + delay + "ms"));
                    
                    HookUtils.logDebug(TAG, "DeX connecting: " + delay + "ms");
                }
            });
            
            XposedBridge.hookAllMethods(dexClass, "disconnect",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    isDexConnected = false;
                    currentWindowCount = 0;
                    
                    dexEvents.add(new DeXEvent("DISCONNECTED", "DeX disconnected"));
                }
            });
            
            HookUtils.logInfo(TAG, "DexPluginManager hooked");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "DexPluginManager hook skipped: " + t.getMessage());
        }
    }
    
    private static void hookWindowManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> wmClass = XposedHelpers.findClass(
                "android.view.WindowManager", lpparam.classLoader
            );
            
            XposedBridge.hookAllMethods(wmClass, "addWindow",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || !isDexConnected) return;
                    
                    // Window limit
                    if (currentWindowCount >= maxWindows) {
                        dexEvents.add(new DeXEvent("WINDOW_LIMIT_REACHED", 
                            "Max windows: " + maxWindows));
                        return;
                    }
                    
                    currentWindowCount++;
                }
            });
            
            XposedBridge.hookAllMethods(wmClass, "removeWindow",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || !isDexConnected) return;
                    
                    if (currentWindowCount > 0) {
                        currentWindowCount--;
                    }
                }
            });
            
            HookUtils.logInfo(TAG, "WindowManager hooked");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "WindowManager hook skipped: " + t.getMessage());
        }
    }
    
    public static void setEnabled(boolean enabled) {
        DeXDesktopModeHook.enabled = enabled;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static boolean isDexConnected() {
        return isDexConnected;
    }
    
    public static void setMaxWindows(int max) {
        maxWindows = max;
    }
    
    public static List<DeXEvent> getDexEvents() {
        return new ArrayList<>(dexEvents);
    }
}
