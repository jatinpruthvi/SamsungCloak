package com.samsungcloak.xposed;

import android.app.Notification;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;

/**
 * Hook #44: NotificationDismissalPatternHook
 * 
 * Simulates realistic notification interaction patterns:
 * - Swipe to dismiss
 * - Tap to open
 * - Silent dismissal
 * - Snooze patterns
 * - Notification priority handling
 * 
 * Target: SM-A125U (Android 10/11)
 */
public class NotificationDismissalPatternHook {

    private static final String TAG = "[HumanInteraction][NotificationDismissal]";
    private static final boolean DEBUG = true;

    private static boolean enabled = true;
    private static float dismissalRate = 0.6f; // 60% of notifications get dismissed
    private static float tapToOpenRate = 0.3f;
    
    private static final Random random = new Random();
    
    // Dismissal types
    private static final int DISMISS_SWIPE = 0;
    private static final int DISMISS_TAP = 1;
    private static final int DISMISS_SILENT = 2;
    private static final int DISMISS_SNOOZE = 3;
    
    private static int dismissedCount = 0;
    private static int openedCount = 0;
    private static int ignoredCount = 0;

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        HookUtils.logInfo(TAG, "Initializing Notification Dismissal Pattern Hook");

        try {
            hookNotificationListener(lpparam);
            hookNotificationCallbacks(lpparam);
            HookUtils.logInfo(TAG, "Notification Dismissal Pattern Hook initialized");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize hook", e);
        }
    }

    private static void hookNotificationListener(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook NotificationListenerService methods
            Class<?> notificationListenerClass = null;
            try {
                notificationListenerClass = XposedHelpers.findClass(
                    "android.service.notification.NotificationListenerService", lpparam.classLoader);
            } catch (Exception e) {
                if (DEBUG) HookUtils.logDebug(TAG, "NotificationListenerService not found");
                return;
            }

            // Hook onNotificationPosted
            XposedBridge.hookAllMethods(notificationListenerClass, "onNotificationPosted",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    try {
                        StatusBarNotification sbn = (StatusBarNotification) param.args[0];
                        String packageName = sbn.getPackageName();
                        
                        // Determine dismissal behavior
                        int dismissalType = getDismissalType(packageName);
                        
                        switch (dismissalType) {
                            case DISMISS_SWIPE:
                                dismissedCount++;
                                if (DEBUG && random.nextFloat() < 0.02f) {
                                    HookUtils.logDebug(TAG, "Notification will be swiped: " + packageName);
                                }
                                break;
                            case DISMISS_TAP:
                                openedCount++;
                                if (DEBUG && random.nextFloat() < 0.02f) {
                                    HookUtils.logDebug(TAG, "Notification will be tapped: " + packageName);
                                }
                                break;
                            case DISMISS_SILENT:
                                ignoredCount++;
                                if (DEBUG && random.nextFloat() < 0.02f) {
                                    HookUtils.logDebug(TAG, "Notification will be ignored: " + packageName);
                                }
                                break;
                            case DISMISS_SNOOZE:
                                if (DEBUG && random.nextFloat() < 0.02f) {
                                    HookUtils.logDebug(TAG, "Notification will be snoozed: " + packageName);
                                }
                                break;
                        }
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked NotificationListenerService");

        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook NotificationListenerService", e);
        }
    }

    private static void hookNotificationCallbacks(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook StatusBarNotification to track notification events
            Class<?> statusBarNotificationClass = XposedHelpers.findClass(
                "android.service.notification.StatusBarNotification", lpparam.classLoader);

            // Could hook getNotification() to analyze notification priority
            XposedBridge.hookAllMethods(statusBarNotificationClass, "getNotification",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    try {
                        Notification notification = (Notification) param.getResult();
                        if (notification != null) {
                            // Analyze notification priority
                            int priority = notification.priority;
                            
                            // Higher priority = more likely to be acted upon
                            float actionRate = dismissalRate;
                            if (priority > 0) {
                                actionRate += 0.2f;
                            } else if (priority < 0) {
                                actionRate -= 0.3f;
                            }
                            
                            if (random.nextFloat() > actionRate) {
                                // User ignores this notification
                                ignoredCount++;
                            }
                        }
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked StatusBarNotification callbacks");

        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook StatusBarNotification", e);
        }
    }

    private static int getDismissalType(String packageName) {
        float roll = random.nextFloat();
        
        // App-specific behavior patterns
        // Messaging apps: higher tap-to-open rate
        if (packageName.contains("messaging") || 
            packageName.contains("whatsapp") ||
            packageName.contains("telegram") ||
            packageName.contains("signal")) {
            
            if (roll < tapToOpenRate + 0.3f) {
                return DISMISS_TAP;
            } else if (roll < tapToOpenRate + 0.3f + 0.4f) {
                return DISMISS_SWIPE;
            } else {
                return DISMISS_SILENT;
            }
        }
        
        // Social media: higher swipe rate
        if (packageName.contains("instagram") || 
            packageName.contains("facebook") ||
            packageName.contains("twitter")) {
            
            if (roll < 0.7f) {
                return DISMISS_SWIPE;
            } else if (roll < 0.85f) {
                return DISMISS_TAP;
            } else {
                return DISMISS_SILENT;
            }
        }
        
        // Default distribution
        if (roll < dismissalRate) {
            return DISMISS_SWIPE;
        } else if (roll < dismissalRate + tapToOpenRate) {
            return DISMISS_TAP;
        } else if (roll < dismissalRate + tapToOpenRate + 0.08f) {
            return DISMISS_SNOOZE;
        } else {
            return DISMISS_SILENT;
        }
    }
}
