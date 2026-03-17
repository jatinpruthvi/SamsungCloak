package com.samsungcloak.xposed;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.StatusBarManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Hook16Realism - Notification Dismissal & Attention Patterns
 * 
 * Simulates realistic notification handling behavior for Samsung Galaxy A12:
 * - Arrival patterns: morning burst 7-9am (3-8), work hours lull, evening peak 6-9pm
 * - User attention probability: screen-on 25-40% dismissed, screen-off 80-95% dismissed
 * - Dismissal behaviors: swipe-away 65-85%, peek-expand-swipe 25-40%, tap-to-open 5-15%
 * - Attention fragmentation: notification bursts, interrupt handling patterns
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class Hook16Realism extends BaseRealismHook {

    private static final String TAG = "[Realism][Hook16-Notification]";
    private static final boolean DEBUG = true;
    
    private static final String HOOK_ID = "hook_16";
    private static final String HOOK_NAME = "Notification Dismissal & Attention Patterns";
    
    // Configuration keys
    private static final String KEY_ENABLED = "notification_patterns_enabled";
    private static final String KEY_ARRIVAL_PATTERNS = "arrival_patterns_enabled";
    private static final String KEY_ATTENTION_PROB = "attention_probability_enabled";
    private static final String KEY_DISMISSAL_BEHAVIOR = "dismissal_behavior_enabled";
    
    // Hook state
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    private static AtomicBoolean enabled = new AtomicBoolean(true);
    private static float intensity = 0.5f;
    
    // Time periods
    public enum TimePeriod {
        MORNING,   // 6am-9am
        WORK,      // 9am-5pm
        EVENING,   // 5pm-10pm
        NIGHT      // 10pm-6am
    }
    
    // Notification tracking
    private static final AtomicReference<TimePeriod> currentPeriod = 
        new AtomicReference<>(TimePeriod.EVENING);
    
    // Notification statistics
    private static final AtomicInteger notificationsReceived = new AtomicInteger(0);
    private static final AtomicInteger notificationsDismissed = new AtomicInteger(0);
    private static final AtomicInteger notificationsViewed = new AtomicInteger(0);
    private static final AtomicInteger notificationsInteracted = new AtomicInteger(0);
    
    // Dismissal behavior probabilities
    private static float swipeAwayProbability = 0.75f;
    private static float peekExpandSwipeProbability = 0.32f;
    private static float tapToOpenProbability = 0.10f;
    
    // Attention probabilities
    private static float screenOnDismissRate = 0.32f;
    private static float screenOffDismissRate = 0.88f;
    
    // Burst simulation
    private static boolean arrivalPatternsEnabled = true;
    private static int morningBurstMin = 3;
    private static int morningBurstMax = 8;
    private static int eveningBurstMin = 5;
    private static int eveningBurstMax = 12;
    
    // Notification queue
    private static final List<PendingNotification> pendingNotifications = 
        new CopyOnWriteArrayList<>();
    private static final Map<String, NotificationStats> notificationStats = 
        new ConcurrentHashMap<>();
    
    // Screen state tracking
    private static boolean screenOn = true;
    private static long lastScreenOnTime = 0;
    private static long lastInteractionTime = 0;
    
    // Thread-local random
    private static final ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new Random());
    
    // Session tracking
    private static long sessionStartTime = 0;
    
    public Hook16Realism() {
        super(HOOK_ID, HOOK_NAME);
    }
    
    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!hookInitialized.compareAndSet(false, true)) {
            logInfo("Hook already initialized");
            return;
        }
        
        logInfo("Initializing Notification Dismissal & Attention Patterns Hook");
        sessionStartTime = System.currentTimeMillis();
        
        try {
            // Load configuration
            loadConfiguration();
            
            // Update time period
            updateTimePeriod();
            
            // Hook NotificationManager
            hookNotificationManager(lpparam);
            
            // Hook StatusBarManager
            hookStatusBarManager(lpparam);
            
            // Hook PowerManager for screen state
            hookPowerManager(lpparam);
            
            logInfo("Notification Dismissal Hook initialized successfully");
        } catch (Exception e) {
            logError("Failed to initialize Notification Dismissal Hook", e);
        }
    }
    
    private void loadConfiguration() {
        if (configManager != null) {
            enabled.set(configManager.getHookParamBool(HOOK_ID, KEY_ENABLED, true));
            intensity = configManager.getHookIntensity(HOOK_ID);
            
            arrivalPatternsEnabled = configManager.getHookParamBool(HOOK_ID, KEY_ARRIVAL_PATTERNS, true);
            morningBurstMin = configManager.getHookParamInt(HOOK_ID, "morning_burst_min", 3);
            morningBurstMax = configManager.getHookParamInt(HOOK_ID, "morning_burst_max", 8);
            eveningBurstMin = configManager.getHookParamInt(HOOK_ID, "evening_burst_min", 5);
            eveningBurstMax = configManager.getHookParamInt(HOOK_ID, "evening_burst_max", 12);
            
            swipeAwayProbability = configManager.getHookParamFloat(HOOK_ID, "swipe_away_prob", 0.75f);
            peekExpandSwipeProbability = configManager.getHookParamFloat(HOOK_ID, "peek_expand_prob", 0.32f);
            tapToOpenProbability = configManager.getHookParamFloat(HOOK_ID, "tap_to_open_prob", 0.10f);
            
            screenOnDismissRate = configManager.getHookParamFloat(HOOK_ID, "screen_on_dismiss_rate", 0.32f);
            screenOffDismissRate = configManager.getHookParamFloat(HOOK_ID, "screen_off_dismiss_rate", 0.88f);
        }
        
        logInfo("Configuration loaded: enabled=" + enabled.get() + ", intensity=" + intensity);
    }
    
    private void hookNotificationManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> notificationManagerClass = XposedHelpers.findClass(
                "android.app.NotificationManager", lpparam.classLoader);
            
            // Hook notify to track incoming notifications
            XposedBridge.hookAllMethods(notificationManagerClass, "notify",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get() || !arrivalPatternsEnabled) return;
                        
                        try {
                            int id = (Integer) param.args[0];
                            Notification notification = (Notification) param.args[1];
                            
                            // Track notification
                            trackIncomingNotification(id, notification);
                            
                            // Check for burst conditions
                            checkAndSimulateBurst(notification);
                        } catch (Exception e) {
                            logDebug("Error in notify hook: " + e.getMessage());
                        }
                    }
                });
            
            // Hook cancel to track dismissal
            XposedBridge.hookAllMethods(notificationManagerClass, "cancel",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get()) return;
                        
                        try {
                            int id = (Integer) param.args[0];
                            trackNotificationDismissal(id);
                        } catch (Exception e) {
                            logDebug("Error in cancel hook: " + e.getMessage());
                        }
                    }
                });
            
            // Hook cancelAll
            XposedBridge.hookAllMethods(notificationManagerClass, "cancelAll",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get()) return;
                        
                        try {
                            // Track mass dismissal
                            int count = pendingNotifications.size();
                            notificationsDismissed.addAndGet(count);
                            pendingNotifications.clear();
                        } catch (Exception e) {
                            logDebug("Error in cancelAll hook: " + e.getMessage());
                        }
                    }
                });
            
            logInfo("Hooked NotificationManager");
        } catch (Exception e) {
            logError("Failed to hook NotificationManager", e);
        }
    }
    
    private void hookStatusBarManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> statusBarManagerClass = XposedHelpers.findClass(
                "android.app.StatusBarManager", lpparam.classLoader);
            
            // Hook expandNotificationsPanel
            XposedBridge.hookAllMethods(statusBarManagerClass, "expandNotificationsPanel",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get()) return;
                        
                        try {
                            // User opened notification panel
                            lastInteractionTime = System.currentTimeMillis();
                            
                            // Calculate attention probability based on screen state
                            float attentionProb = screenOn ? 
                                screenOnDismissRate : screenOffDismissRate;
                            
                            // Determine which notifications get dismissed
                            simulatePanelInteraction(attentionProb);
                        } catch (Exception e) {
                            logDebug("Error in expandNotificationsPanel hook: " + e.getMessage());
                        }
                    }
                });
            
            // Hook collapsePanels
            XposedBridge.hookAllMethods(statusBarManagerClass, "collapsePanels",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get()) return;
                        
                        try {
                            // Panel collapsed - potentially dismiss notifications
                            float dismissProb = screenOn ? 0.25f : 0.90f;
                            simulatePanelInteraction(dismissProb);
                        } catch (Exception e) {
                            logDebug("Error in collapsePanels hook: " + e.getMessage());
                        }
                    }
                });
            
            logInfo("Hooked StatusBarManager");
        } catch (Exception e) {
            logError("Failed to hook StatusBarManager", e);
        }
    }
    
    private void hookPowerManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> powerManagerClass = XposedHelpers.findClass(
                "android.os.PowerManager", lpparam.classLoader);
            
            // Hook isScreenOn to track screen state
            XposedBridge.hookAllMethods(powerManagerClass, "isScreenOn",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get()) return;
                        
                        try {
                            boolean wasScreenOn = screenOn;
                            screenOn = (Boolean) param.getResult();
                            
                            // Update time period when screen state changes
                            if (wasScreenOn != screenOn) {
                                lastScreenOnTime = System.currentTimeMillis();
                            }
                        } catch (Exception e) {
                            logDebug("Error in isScreenOn hook: " + e.getMessage());
                        }
                    }
                });
            
            logInfo("Hooked PowerManager");
        } catch (Exception e) {
            logError("Failed to hook PowerManager", e);
        }
    }
    
    private void trackIncomingNotification(int id, Notification notification) {
        notificationsReceived.incrementAndGet();
        
        String tag = "";
        if (notification != null && notification.extras != null) {
            tag = notification.extras.getString(Notification.EXTRA_TITLE, "");
        }
        
        PendingNotification pending = new PendingNotification(
            id, tag, System.currentTimeMillis(), notification
        );
        pendingNotifications.add(pending);
        
        // Update stats
        String key = tag.isEmpty() ? "unknown" : tag;
        NotificationStats stats = notificationStats.get(key);
        if (stats == null) {
            stats = new NotificationStats();
            notificationStats.put(key, stats);
        }
        stats.received++;
        
        if (DEBUG && random.get().nextDouble() < 0.01) {
            logDebug("Notification tracked: id=" + id + ", tag=" + tag);
        }
    }
    
    private void trackNotificationDismissal(int id) {
        notificationsDismissed.incrementAndGet();
        
        // Remove from pending
        for (PendingNotification pending : pendingNotifications) {
            if (pending.id == id) {
                pendingNotifications.remove(pending);
                break;
            }
        }
        
        // Update stats
        for (NotificationStats stats : notificationStats.values()) {
            if (stats.received > stats.dismissed) {
                stats.dismissed++;
                break;
            }
        }
    }
    
    private void checkAndSimulateBurst(Notification notification) {
        TimePeriod period = getCurrentTimePeriod();
        
        if (period == TimePeriod.MORNING) {
            // Morning burst simulation
            if (random.get().nextDouble() < 0.25 * intensity) {
                int burstCount = morningBurstMin + 
                    random.get().nextInt(morningBurstMax - morningBurstMin + 1);
                simulateNotificationBurst(burstCount, notification);
            }
        } else if (period == TimePeriod.EVENING) {
            // Evening burst simulation
            if (random.get().nextDouble() < 0.30 * intensity) {
                int burstCount = eveningBurstMin + 
                    random.get().nextInt(eveningBurstMax - eveningBurstMin + 1);
                simulateNotificationBurst(burstCount, notification);
            }
        }
    }
    
    private void simulateNotificationBurst(int count, Notification sourceNotification) {
        // In a full implementation, this would queue additional notifications
        // For tracking, we just log the burst
        if (DEBUG) {
            logDebug("Notification burst simulated: " + count + " notifications");
        }
    }
    
    private void simulatePanelInteraction(float attentionProb) {
        // Calculate adjusted probability based on intensity
        float adjustedProb = attentionProb * intensity;
        
        // Track which notifications get attention
        List<PendingNotification> toRemove = new ArrayList<>();
        
        for (PendingNotification pending : pendingNotifications) {
            // Skip very recent notifications
            if (System.currentTimeMillis() - pending.timestamp < 500) {
                continue;
            }
            
            // Determine dismissal method
            double roll = random.get().nextDouble();
            
            if (roll < swipeAwayProbability * adjustedProb) {
                // Swipe away
                toRemove.add(pending);
                notificationsDismissed.incrementAndGet();
                
                NotificationStats stats = notificationStats.get(pending.tag);
                if (stats != null) {
                    stats.dismissed++;
                }
            } else if (roll < (swipeAwayProbability + peekExpandSwipeProbability) * adjustedProb) {
                // Peek, expand, then swipe
                notificationsViewed.incrementAndGet();
                
                // After viewing, possibly dismiss
                if (random.get().nextDouble() < 0.5) {
                    toRemove.add(pending);
                    notificationsDismissed.incrementAndGet();
                }
            } else if (roll < (swipeAwayProbability + peekExpandSwipeProbability + tapToOpenProbability) * adjustedProb) {
                // Tap to open
                toRemove.add(pending);
                notificationsInteracted.incrementAndGet();
            }
        }
        
        pendingNotifications.removeAll(toRemove);
    }
    
    private void updateTimePeriod() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
        
        TimePeriod period;
        if (hour >= 6 && hour < 9) {
            period = TimePeriod.MORNING;
        } else if (hour >= 9 && hour < 17) {
            period = TimePeriod.WORK;
        } else if (hour >= 17 && hour < 22) {
            period = TimePeriod.EVENING;
        } else {
            period = TimePeriod.NIGHT;
        }
        
        currentPeriod.set(period);
    }
    
    private TimePeriod getCurrentTimePeriod() {
        // Update periodically
        long now = System.currentTimeMillis();
        if (now - lastUpdateTime > 60000) { // Update every minute
            updateTimePeriod();
            lastUpdateTime = now;
        }
        return currentPeriod.get();
    }
    
    private static long lastUpdateTime = 0;
    
    /**
     * Enable/disable the hook
     */
    public static void setEnabled(boolean isEnabled) {
        enabled.set(isEnabled);
        HookUtils.logInfo(TAG, "Hook " + (isEnabled ? "enabled" : "disabled"));
    }
    
    /**
     * Set intensity (0.0 - 1.0)
     */
    public static void setIntensity(float value) {
        intensity = Math.max(0.0f, Math.min(1.0f, value));
        HookUtils.logInfo(TAG, "Intensity set to: " + intensity);
    }
    
    /**
     * Get notification statistics
     */
    public static Map<String, Integer> getStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("received", notificationsReceived.get());
        stats.put("dismissed", notificationsDismissed.get());
        stats.put("viewed", notificationsViewed.get());
        stats.put("interacted", notificationsInteracted.get());
        stats.put("pending", pendingNotifications.size());
        return stats;
    }
    
    /**
     * Pending notification record
     */
    private static class PendingNotification {
        final int id;
        final String tag;
        final long timestamp;
        final Notification notification;
        
        PendingNotification(int id, String tag, long timestamp, Notification notification) {
            this.id = id;
            this.tag = tag;
            this.timestamp = timestamp;
            this.notification = notification;
        }
    }
    
    /**
     * Notification statistics
     */
    private static class NotificationStats {
        int received = 0;
        int dismissed = 0;
        int viewed = 0;
        int interacted = 0;
    }
}
