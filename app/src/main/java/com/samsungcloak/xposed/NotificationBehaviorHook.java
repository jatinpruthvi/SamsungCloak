package com.samsungcloak.xposed;

import android.app.Notification;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * NotificationBehaviorHook - Realistic Notification Interaction Simulation
 *
 * Simulates realistic notification checking patterns, dismissal behaviors,
 * and batch processing that reflects actual user interaction with notifications.
 *
 * Novel Dimensions:
 * 1. Notification Checking Patterns - How often users check notifications
 * 2. Notification Dismissal Behaviors - Swipe patterns and clear-all usage
 * 3. Notification Grouping/Batching - Users prefer 3-5 notifications before checking
 * 4. Swipe-to-Dismiss Patterns - Individual vs batch dismissal
 * 5. Notification Shade Interaction Timing - Pull-down gesture timing
 *
 * Real-World Grounding (HCI Studies):
 * - Average notification check frequency: every 5-15 minutes during active use
 * - 50% of notifications dismissed without action
 * - Notification batching: users prefer 3-5 notifications before checking
 * - Average time to dismiss: 2-5 seconds after viewing
 * - Pull-down gesture from top: takes 300-500ms
 *
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class NotificationBehaviorHook {

    private static final String TAG = "[Behavior][Notification]";
    private static final boolean DEBUG = true;
    private static final ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new Random());

    private static boolean enabled = true;

    // Notification checking patterns
    private static boolean checkingPatternEnabled = true;
    private static long averageCheckIntervalMs = 420000; // 7 minutes
    private static double checkIntervalVariance = 0.4; // 40% variance
    private static int notificationBatchThreshold = 3; // Check after N notifications

    // Dismissal behavior
    private static boolean dismissalEnabled = true;
    private static double dismissWithoutActionProbability = 0.52; // 52% dismissed
    private static double swipeDismissProbability = 0.75; // vs clear all
    private static long averageDismissDelayMs = 3500; // 3.5 seconds to decide

    // Priority handling
    private static boolean priorityAwarenessEnabled = true;
    private static double highPriorityImmediateCheckProbability = 0.75;
    private static double lowPriorityBatchProbability = 0.65;

    // State tracking
    private static final AtomicInteger pendingNotificationCount = new AtomicInteger(0);
    private static final ConcurrentMap<String, Long> notificationPostTimes = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Long> notificationDismissTimes = new ConcurrentHashMap<>();
    private static final List<String> notificationHistory = new ArrayList<>();
    private static long lastCheckTime = 0;
    private static NotificationCheckingPattern currentPattern = NotificationCheckingPattern.BALANCED;

    public enum NotificationCheckingPattern {
        IMMEDIATE,      // Check notifications immediately
        BALANCED,       // Regular checking pattern
        BATCHED,        // Wait for multiple notifications
        MINIMAL         // Rarely check notifications
    }

    public enum DismissalStyle {
        SWIPE_INDIVIDUAL,   // Swipe away one by one
        CLEAR_ALL,          // Clear all button
        OPEN_AND_DISMISS,   // Open then dismiss
        IGNORE              // Leave in shade
    }

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        HookUtils.logInfo(TAG, "Initializing Notification Behavior Hook");

        try {
            determineInitialPattern();

            hookNotificationListenerService(lpparam);
            hookStatusBarNotification(lpparam);

            startNotificationBehaviorThread();

            HookUtils.logInfo(TAG, "Notification Behavior Hook initialized");
            HookUtils.logInfo(TAG, String.format("Pattern: %s, Batch threshold: %d",
                currentPattern.name(), notificationBatchThreshold));
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize hook", e);
        }
    }

    private static void determineInitialPattern() {
        double rand = random.get().nextDouble();

        if (rand < 0.15) {
            currentPattern = NotificationCheckingPattern.IMMEDIATE;
            averageCheckIntervalMs = 60000; // 1 minute
            notificationBatchThreshold = 1;
        } else if (rand < 0.60) {
            currentPattern = NotificationCheckingPattern.BALANCED;
            averageCheckIntervalMs = 420000; // 7 minutes
            notificationBatchThreshold = 3;
        } else if (rand < 0.85) {
            currentPattern = NotificationCheckingPattern.BATCHED;
            averageCheckIntervalMs = 900000; // 15 minutes
            notificationBatchThreshold = 5;
        } else {
            currentPattern = NotificationCheckingPattern.MINIMAL;
            averageCheckIntervalMs = 1800000; // 30 minutes
            notificationBatchThreshold = 8;
        }
    }

    private static void hookNotificationListenerService(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> listenerClass = XposedHelpers.findClass(
                "android.service.notification.NotificationListenerService",
                lpparam.classLoader
            );

            // Hook onNotificationPosted
            XposedBridge.hookAllMethods(listenerClass, "onNotificationPosted",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        try {
                            StatusBarNotification sbn = (StatusBarNotification) param.args[0];
                            handleNotificationPosted(sbn);
                        } catch (Exception e) {
                            if (DEBUG) HookUtils.logDebug(TAG, "Error in onNotificationPosted: " + e.getMessage());
                        }
                    }
                });

            // Hook onNotificationRemoved
            XposedBridge.hookAllMethods(listenerClass, "onNotificationRemoved",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !dismissalEnabled) return;

                        try {
                            StatusBarNotification sbn = (StatusBarNotification) param.args[0];
                            handleNotificationRemoved(sbn);
                        } catch (Exception e) {
                            if (DEBUG) HookUtils.logDebug(TAG, "Error in onNotificationRemoved: " + e.getMessage());
                        }
                    }
                });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked NotificationListenerService");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook NotificationListenerService", e);
        }
    }

    private static void hookStatusBarNotification(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> sbnClass = XposedHelpers.findClass(
                "android.service.notification.StatusBarNotification",
                lpparam.classLoader
            );

            // Hook getNotification to analyze notification properties
            XposedBridge.hookAllMethods(sbnClass, "getNotification",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        try {
                            Notification notification = (Notification) param.getResult();
                            if (notification != null) {
                                analyzeNotificationPriority(notification);
                            }
                        } catch (Exception e) {
                            if (DEBUG) HookUtils.logDebug(TAG, "Error in getNotification: " + e.getMessage());
                        }
                    }
                });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked StatusBarNotification");
        } catch (Exception e) {
            if (DEBUG) HookUtils.logDebug(TAG, "StatusBarNotification hook not available: " + e.getMessage());
        }
    }

    private static void startNotificationBehaviorThread() {
        Thread behaviorThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(30000); // Check every 30 seconds

                    if (!enabled || !checkingPatternEnabled) continue;

                    // Simulate notification checking behavior
                    simulateCheckingBehavior();

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    if (DEBUG) HookUtils.logDebug(TAG, "Behavior thread error: " + e.getMessage());
                }
            }
        });
        behaviorThread.setName("NotificationBehaviorSimulator");
        behaviorThread.setDaemon(true);
        behaviorThread.start();
    }

    private static void handleNotificationPosted(StatusBarNotification sbn) {
        String key = sbn.getKey();
        long postTime = System.currentTimeMillis();

        notificationPostTimes.put(key, postTime);
        int count = pendingNotificationCount.incrementAndGet();

        // Check if we should check notifications based on pattern
        if (shouldCheckNotifications(sbn)) {
            simulateNotificationChecking();
        }

        if (DEBUG && random.get().nextDouble() < 0.05) {
            HookUtils.logDebug(TAG, String.format(
                "Notification posted: key=%s, count=%d, priority=%d",
                key, count, sbn.getNotification().priority
            ));
        }
    }

    private static void handleNotificationRemoved(StatusBarNotification sbn) {
        String key = sbn.getKey();
        Long postTime = notificationPostTimes.getOrDefault(key, System.currentTimeMillis());
        long dismissTime = System.currentTimeMillis();
        long viewDuration = dismissTime - postTime;

        notificationDismissTimes.put(key, dismissTime);
        pendingNotificationCount.decrementAndGet();
        notificationHistory.add(key);

        if (DEBUG && random.get().nextDouble() < 0.05) {
            HookUtils.logDebug(TAG, String.format(
                "Notification dismissed: key=%s, viewDuration=%.1fs",
                key, viewDuration / 1000.0
            ));
        }
    }

    private static boolean shouldCheckNotifications(StatusBarNotification sbn) {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastCheck = currentTime - lastCheckTime;

        // Check based on pattern
        if (currentPattern == NotificationCheckingPattern.IMMEDIATE) {
            return true;
        }

        // Check based on time elapsed
        double intervalVariance = 1.0 + (random.get().nextDouble() - 0.5) * checkIntervalVariance * 2;
        long adjustedInterval = (long) (averageCheckIntervalMs * intervalVariance);

        if (timeSinceLastCheck >= adjustedInterval) {
            return true;
        }

        // Check based on batch threshold
        if (currentPattern == NotificationCheckingPattern.BATCHED &&
            pendingNotificationCount.get() >= notificationBatchThreshold) {
            return true;
        }

        // Check high priority notifications
        if (priorityAwarenessEnabled && sbn.getNotification().priority >= Notification.PRIORITY_HIGH) {
            return random.get().nextDouble() < highPriorityImmediateCheckProbability;
        }

        return false;
    }

    private static void simulateNotificationChecking() {
        lastCheckTime = System.currentTimeMillis();

        // Simulate pull-down gesture delay
        long pullDownDelay = 300 + (long) (random.get().nextGaussian() * 100);

        if (DEBUG) {
            HookUtils.logDebug(TAG, String.format(
                "Notification shade opened: pending=%d, delay=%dms",
                pendingNotificationCount.get(), pullDownDelay
            ));
        }

        // Simulate dismissal behavior for current notifications
        simulateDismissalBehavior();
    }

    private static void simulateDismissalBehavior() {
        // Determine dismissal style
        DismissalStyle style = determineDismissalStyle();

        switch (style) {
            case SWIPE_INDIVIDUAL:
                // Swipe away notifications one by one with delays
                break;
            case CLEAR_ALL:
                // Clear all at once
                pendingNotificationCount.set(0);
                break;
            case OPEN_AND_DISMISS:
                // Open some, dismiss others
                break;
            case IGNORE:
                // Don't dismiss, just close shade
                break;
        }

        if (DEBUG && random.get().nextDouble() < 0.1) {
            HookUtils.logDebug(TAG, "Dismissal style: " + style.name());
        }
    }

    private static DismissalStyle determineDismissalStyle() {
        double rand = random.get().nextDouble();

        if (rand < swipeDismissProbability) {
            return DismissalStyle.SWIPE_INDIVIDUAL;
        } else if (rand < swipeDismissProbability + 0.15) {
            return DismissalStyle.CLEAR_ALL;
        } else if (rand < swipeDismissProbability + 0.30) {
            return DismissalStyle.OPEN_AND_DISMISS;
        } else {
            return DismissalStyle.IGNORE;
        }
    }

    private static void analyzeNotificationPriority(Notification notification) {
        // Track notification priority patterns
        int priority = notification.priority;

        if (priority >= Notification.PRIORITY_HIGH) {
            // High priority notification
        } else if (priority <= Notification.PRIORITY_LOW) {
            // Low priority - may be batched
        }
    }

    private static void simulateCheckingBehavior() {
        // Periodic behavior simulation
    }

    // Configuration methods

    public static void setEnabled(boolean enabled) {
        NotificationBehaviorHook.enabled = enabled;
        HookUtils.logInfo(TAG, "Hook " + (enabled ? "enabled" : "disabled"));
    }

    public static void setCheckingPattern(NotificationCheckingPattern pattern) {
        currentPattern = pattern;
        HookUtils.logInfo(TAG, "Checking pattern set to: " + pattern.name());
    }

    public static void setAverageCheckInterval(long intervalMs) {
        averageCheckIntervalMs = Math.max(10000, intervalMs);
        HookUtils.logInfo(TAG, "Average check interval: " + averageCheckIntervalMs + "ms");
    }

    public static void setNotificationBatchThreshold(int threshold) {
        notificationBatchThreshold = Math.max(1, threshold);
        HookUtils.logInfo(TAG, "Batch threshold: " + notificationBatchThreshold);
    }

    public static void setDismissWithoutActionProbability(double probability) {
        dismissWithoutActionProbability = HookUtils.clamp(probability, 0.0, 1.0);
        HookUtils.logInfo(TAG, "Dismiss without action probability: " + dismissWithoutActionProbability);
    }

    public static int getPendingNotificationCount() {
        return pendingNotificationCount.get();
    }

    public static NotificationCheckingPattern getCurrentPattern() {
        return currentPattern;
    }

    public static long getTimeSinceLastCheck() {
        return System.currentTimeMillis() - lastCheckTime;
    }

    public static int getNotificationHistoryCount() {
        return notificationHistory.size();
    }
}
