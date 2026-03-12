package com.samsungcloak.xposed;

import android.view.Display;
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
 * AlwaysOnDisplayHook - Always-On Display Simulation
 * 
 * Simulates Always-On Display behavior:
 * - Delayed wake
 * - Brightness inconsistency
 * - Notification icon delays
 * - Missing icons
 * - Display flicker
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class AlwaysOnDisplayHook {

    private static final String TAG = "[AOD][Display]";
    private static final boolean DEBUG = true;
    
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    
    // Failure rates
    private static float delayedWakeRate = 0.15f;        // 15%
    private static float brightnessFlickerRate = 0.08f; // 8%
    private static float iconMissingRate = 0.05f;      // 5%
    private static float iconDelayRate = 0.12f;       // 12%
    
    // Timing
    private static int minWakeDelay = 200;   // ms
    private static int maxWakeDelay = 500;   // ms
    
    // State
    private static boolean isAODActive = false;
    private static boolean isDisplayOn = false;
    private static int currentBrightness = 50;
    private static long lastAODUpdate = 0;
    private static int notificationCount = 0;
    
    private static final Random random = new Random();
    private static final List<AODEvent> aodHistory = new CopyOnWriteArrayList<>();
    
    public static class AODEvent {
        public long timestamp;
        public String type;
        public String details;
        
        public AODEvent(String type, String details) {
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
        
        HookUtils.logInfo(TAG, "Initializing Always-On Display Hook");
        
        try {
            hookAODService(lpparam);
            hookDisplay(lpparam);
            
            HookUtils.logInfo(TAG, "AOD hook initialized");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Init failed: " + t.getMessage());
            hookInitialized.set(false);
        }
    }
    
    private static void hookAODService(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> aodClass = XposedHelpers.findClass(
                "com.samsung.android.aod.AODService", lpparam.classLoader
            );
            
            // Hook showAOD
            XposedBridge.hookAllMethods(aodClass, "showAOD",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    // Check for delayed wake
                    if (random.nextFloat() < delayedWakeRate) {
                        int delay = minWakeDelay + 
                            random.nextInt(maxWakeDelay - minWakeDelay);
                        
                        aodHistory.add(new AODEvent("DELAYED_WAKE", 
                            "AOD delayed by " + delay + "ms"));
                        
                        HookUtils.logDebug(TAG, "AOD delayed wake: " + delay + "ms");
                        
                        return;
                    }
                    
                    isAODActive = true;
                    isDisplayOn = true;
                    
                    aodHistory.add(new AODEvent("AOD_SHOWN", "AOD displayed"));
                }
            });
            
            // Hook updateAODContent
            XposedBridge.hookAllMethods(aodClass, "updateAODContent",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    notificationCount++;
                    
                    // Check for icon delay
                    if (random.nextFloat() < iconDelayRate) {
                        int delay = random.nextInt(300) + 100;
                        
                        aodHistory.add(new AODEvent("ICON_DELAY", 
                            "Notification icon delayed by " + delay + "ms"));
                        
                        HookUtils.logDebug(TAG, "Notification icon delayed");
                    }
                    
                    // Check for missing icon
                    if (random.nextFloat() < iconMissingRate) {
                        aodHistory.add(new AODEvent("ICON_MISSING", 
                            "Notification icon not displayed"));
                        
                        HookUtils.logDebug(TAG, "Notification icon missing");
                    }
                    
                    lastAODUpdate = System.currentTimeMillis();
                }
            });
            
            // Hook hideAOD
            XposedBridge.hookAllMethods(aodClass, "hideAOD",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    isAODActive = false;
                    isDisplayOn = false;
                    
                    aodHistory.add(new AODEvent("AOD_HIDDEN", "AOD hidden"));
                }
            });
            
            HookUtils.logInfo(TAG, "AOD service hooked");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "AOD hook skipped: " + t.getMessage());
        }
    }
    
    private static void hookDisplay(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> displayClass = XposedHelpers.findClass(
                "android.view.Display", lpparam.classLoader
            );
            
            // Hook getAlwaysOn
            XposedBridge.hookAllMethods(displayClass, "getAlwaysOn",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    // Return AOD state
                    param.setResult(isAODActive);
                }
            });
            
            // Hook getBrightness
            XposedBridge.hookAllMethods(displayClass, "getBrightness",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || !isAODActive) return;
                    
                    // Check for brightness flicker
                    if (random.nextFloat() < brightnessFlickerRate) {
                        int flicker = currentBrightness + random.nextInt(20) - 10;
                        flicker = Math.max(10, Math.min(100, flicker));
                        
                        aodHistory.add(new AODEvent("BRIGHTNESS_FLICKER", 
                            "Brightness flickered to " + flicker + "%"));
                        
                        currentBrightness = flicker;
                        
                        HookUtils.logDebug(TAG, "AOD brightness flicker");
                    }
                    
                    param.setResult(currentBrightness / 100.0f);
                }
            });
            
            HookUtils.logInfo(TAG, "Display hooked for AOD");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "Display hook skipped: " + t.getMessage());
        }
    }
    
    /**
     * Update AOD brightness
     */
    public static void updateBrightness(int brightness) {
        currentBrightness = Math.max(5, Math.min(100, brightness));
    }
    
    // ========== Configuration ==========
    
    public static void setEnabled(boolean enabled) {
        AlwaysOnDisplayHook.enabled = enabled;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void setDelayedWakeRate(float rate) {
        delayedWakeRate = Math.max(0, Math.min(1, rate));
    }
    
    public static void setWakeDelayRange(int minMs, int maxMs) {
        minWakeDelay = Math.max(50, minMs);
        maxWakeDelay = Math.max(minWakeDelay, maxMs);
    }
    
    public static boolean isAODActive() {
        return isAODActive;
    }
    
    public static boolean isDisplayOn() {
        return isDisplayOn;
    }
    
    public static int getCurrentBrightness() {
        return currentBrightness;
    }
    
    public static int getNotificationCount() {
        return notificationCount;
    }
    
    public static List<AODEvent> getAODHistory() {
        return new ArrayList<>(aodHistory);
    }
}
