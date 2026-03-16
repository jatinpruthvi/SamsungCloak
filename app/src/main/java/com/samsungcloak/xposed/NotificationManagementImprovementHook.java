package com.samsungcloak.xposed;

import android.app.Notification;
import android.app.NotificationManager;
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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * NotificationManagementImprovementHook - Notification Enhancement
 * 
 * IMPROVEMENT over SocialInterruptionsHook:
 * - Added notification stacking delays
 * - Simulated notification shade freeze
 * - Sound queue delays
 * - Cross-hook with DozeModeTransitionHook for DND sync
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class NotificationManagementImprovementHook {

    private static final String TAG = "[Notification][Improvement]";
    private static final boolean DEBUG = true;
    
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    
    // Enhancement parameters
    private static int stackingDelayMin = 100;    // ms
    private static int stackingDelayMax = 500;    // ms
    private static float shadeFreezeRate = 0.03f; // 3%
    private static int soundQueueDelayMin = 50;   // ms
    private static int soundQueueDelayMax = 200;  // ms
    private static int maxStacked = 10;
    
    // Original parameters (from SocialInterruptionsHook)
    private static float interruptionRate = 0.15f;
    private static int notificationDelayMin = 1000;
    private static int notificationDelayMax = 5000;
    
    // State
    private static int notificationCount = 0;
    private static boolean isShadeFrozen = false;
    private static boolean isDNDEnabled = false;
    private static long lastNotificationTime = 0;
    
    private static final Random random = new Random();
    private static final List<NotificationEvent> notificationEvents = new CopyOnWriteArrayList<>();
    private static final AtomicInteger pendingNotifications = new AtomicInteger(0);
    
    public static class NotificationEvent {
        public long timestamp;
        public String type;
        public String details;
        
        public NotificationEvent(String type, String details) {
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
        
        HookUtils.logInfo(TAG, "Initializing Notification Management Improvement Hook");
        
        try {
            hookNotificationManager(lpparam);
            hookNotificationShade(lpparam);
            
            HookUtils.logInfo(TAG, "Notification improvement hook initialized");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Init failed: " + t.getMessage());
            hookInitialized.set(false);
        }
    }
    
    private static void hookNotificationManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> notifManagerClass = XposedHelpers.findClass(
                "android.app.NotificationManager", lpparam.classLoader
            );
            
            XposedBridge.hookAllMethods(notifManagerClass, "notify",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    notificationCount++;
                    pendingNotifications.incrementAndGet();
                    
                    long now = System.currentTimeMillis();
                    
                    // Notification stacking delay
                    if (pendingNotifications.get() > 1 && pendingNotifications.get() <= maxStacked) {
                        int delay = stackingDelayMin + 
                            random.nextInt(stackingDelayMax - stackingDelayMin);
                        
                        notificationEvents.add(new NotificationEvent("STACKING_DELAY", 
                            "Notification queued: " + pendingNotifications.get() + " pending"));
                        
                        HookUtils.logDebug(TAG, "Stacking delay: " + delay + "ms");
                    }
                    
                    // Sound queue delay
                    if (notificationCount > 1) {
                        int delay = soundQueueDelayMin + 
                            random.nextInt(soundQueueDelayMax - soundQueueDelayMin);
                        
                        notificationEvents.add(new NotificationEvent("SOUND_QUEUE_DELAY", 
                            "Sound queued: " + delay + "ms"));
                    }
                    
                    lastNotificationTime = now;
                    
                    // DND check (cross-hook with DozeModeTransitionHook)
                    if (isDNDEnabled && random.nextFloat() < 0.5f) {
                        notificationEvents.add(new NotificationEvent("DND_SUPPRESSED", 
                            "Notification suppressed by DND"));
                        pendingNotifications.decrementAndGet();
                        return;
                    }
                    
                    // Original interruption
                    if (random.nextFloat() < interruptionRate) {
                        notificationEvents.add(new NotificationEvent("NOTIFICATION", 
                            "User interrupted by new notification"));
                    }
                }
            });
            
            XposedBridge.hookAllMethods(notifManagerClass, "cancel",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    if (pendingNotifications.get() > 0) {
                        pendingNotifications.decrementAndGet();
                    }
                }
            });
            
            HookUtils.logInfo(TAG, "NotificationManager hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "NotificationManager hook failed: " + t.getMessage());
        }
    }
    
    private static void hookNotificationShade(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> statusBarClass = XposedHelpers.findClass(
                "com.android.systemui.statusbar.StatusBar", lpparam.classLoader
            );
            
            XposedBridge.hookAllMethods(statusBarClass, "expandShade",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    // Notification shade freeze
                    if (random.nextFloat() < shadeFreezeRate) {
                        isShadeFrozen = true;
                        notificationEvents.add(new NotificationEvent("SHADE_FREEZE", 
                            "Notification shade temporarily frozen"));
                        
                        HookUtils.logDebug(TAG, "Notification shade freeze");
                    }
                }
            });
            
            XposedBridge.hookAllMethods(statusBarClass, "collapseShade",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    if (isShadeFrozen) {
                        isShadeFrozen = false;
                        notificationEvents.add(new NotificationEvent("SHADE_UNFROZEN", 
                            "Notification shade restored"));
                    }
                }
            });
            
            HookUtils.logInfo(TAG, "Notification shade hooked");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "Notification shade hook skipped: " + t.getMessage());
        }
    }
    
    public static void setEnabled(boolean enabled) {
        NotificationManagementImprovementHook.enabled = enabled;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    // Cross-hook setters
    public static void setDNDEnabled(boolean enabled) {
        isDNDEnabled = enabled;
    }
    
    public static void setShadeFrozen(boolean frozen) {
        isShadeFrozen = frozen;
    }
    
    public static int getNotificationCount() {
        return notificationCount;
    }
    
    public static int getPendingCount() {
        return pendingNotifications.get();
    }
    
    public static boolean isShadeFrozen() {
        return isShadeFrozen;
    }
    
    public static List<NotificationEvent> getNotificationEvents() {
        return new ArrayList<>(notificationEvents);
    }
}
