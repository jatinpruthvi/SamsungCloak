package com.samsungcloak.xposed;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

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

/**
 * SocialContextInterruptionHook - Social Context and Notification Simulation
 *
 * Simulates realistic social interruptions and notification-driven behavior patterns
 * that affect user interaction with the device. This adds a critical dimension of
 * realism often missing from testing frameworks.
 *
 * Novel Dimensions:
 * 1. Notification Interruptions - Simulates calls, messages, social notifications
 * 2. Social Context Modeling - Active vs passive usage patterns
 * 3. Attention Recovery Time - Time needed to refocus after interruption
 * 4. Reply Probability - Likelihood of immediate response to notifications
 * 5. Notification Stacking - Multiple notifications affecting attention
 *
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class SocialContextInterruptionHook {

    private static final String TAG = "[HumanInteraction][SocialContext]";
    private static final boolean DEBUG = true;
    private static final ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new Random());

    private static boolean enabled = true;

    // Notification probability configuration
    private static double callProbability = 0.02; // Per minute
    private static double messageProbability = 0.15;
    private static double socialNotificationProbability = 0.25;
    private static double workNotificationProbability = 0.08;

    // Attention and response configuration
    private static double attentionRecoveryTimeMs = 2000; // 2 seconds average
    private static double immediateReplyProbability = 0.12;
    private static double dismissalProbability = 0.45;

    // Social context states
    private static SocialContext currentContext = SocialContext.PASSIVE_CONSUMPTION;
    private static boolean isDeviceInActiveUse = false;
    private static long lastInteractionTime = 0;
    private static long contextStartTime = 0;

    // Notification tracking
    private static final List<NotificationEvent> recentNotifications = new CopyOnWriteArrayList<>();
    private static final Map<String, Integer> notificationCounts = new ConcurrentHashMap<>();
    private static int activeNotificationCount = 0;
    private static long lastNotificationTime = 0;

    // State tracking
    private static long sessionStartTime = 0;
    private static int totalInterruptions = 0;
    private static int totalResponses = 0;

    // Callback for notification events
    private static NotificationCallback notificationCallback = null;

    public enum SocialContext {
        PASSIVE_CONSUMPTION,    // Browsing, reading, watching
        ACTIVE_ENGAGEMENT,      // Typing, interacting, creating
        SOCIAL_COMMUNICATION,   // Messaging, calling
        WORK_FOCUS,            // Productive tasks
        IDLE                    // Not using device
    }

    public enum NotificationType {
        INCOMING_CALL,
        SMS_MESSAGE,
        SOCIAL_MEDIA,
        EMAIL,
        WORK_MESSAGE,
        SYSTEM_ALERT,
        REMINDER,
        OTHER
    }

    public interface NotificationCallback {
        void onNotificationReceived(NotificationType type, String packageName, String title);
        void onInterruptionOccurred(NotificationEvent event);
        void onAttentionRecovered();
    }

    public static class NotificationEvent {
        public final NotificationType type;
        public final String packageName;
        public final String title;
        public final long timestamp;
        public final boolean wasInterrupted;
        public final boolean wasResponded;

        public NotificationEvent(NotificationType type, String packageName, String title, 
                                 boolean wasInterrupted, boolean wasResponded) {
            this.type = type;
            this.packageName = packageName;
            this.title = title;
            this.timestamp = System.currentTimeMillis();
            this.wasInterrupted = wasInterrupted;
            this.wasResponded = wasResponded;
        }
    }

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        HookUtils.logInfo(TAG, "Initializing Social Context Interruption Hook");

        try {
            sessionStartTime = System.currentTimeMillis();
            contextStartTime = sessionStartTime;
            lastInteractionTime = sessionStartTime;

            hookNotificationService(lpparam);
            hookNotificationManager(lpparam);
            hookActivityLifecycle(lpparam);
            
            startContextSimulationThread();

            HookUtils.logInfo(TAG, "Social Context Interruption Hook initialized successfully");
            HookUtils.logInfo(TAG, "Initial context: " + currentContext.name());
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize hook", e);
        }
    }

    private static void hookNotificationService(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> notificationListenerClass = XposedHelpers.findClass(
                "android.service.notification.NotificationListenerService",
                lpparam.classLoader
            );

            XposedBridge.hookAllMethods(notificationListenerClass, "onNotificationPosted", 
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        try {
                            Object rankingMap = param.args.length > 0 ? param.args[0] : null;
                            
                            // Process notification
                            processIncomingNotification(rankingMap);
                        } catch (Exception e) {
                            if (DEBUG) HookUtils.logDebug(TAG, "Error in notification hook: " + e.getMessage());
                        }
                    }
                });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked NotificationListenerService");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook NotificationListenerService", e);
        }
    }

    private static void hookNotificationManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> notificationManagerClass = XposedHelpers.findClass(
                "android.app.NotificationManager",
                lpparam.classLoader
            );

            XposedBridge.hookAllMethods(notificationManagerClass, "notify", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    try {
                        int id = (int) param.args[0];
                        Object notification = param.args.length > 1 ? param.args[1] : null;

                        if (notification instanceof Notification) {
                            processNotification(id, (Notification) notification);
                        }
                    } catch (Exception e) {
                        if (DEBUG) HookUtils.logDebug(TAG, "Error in notify hook: " + e.getMessage());
                    }
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked NotificationManager.notify");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook NotificationManager", e);
        }
    }

    private static void hookActivityLifecycle(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> activityClass = XposedHelpers.findClass("android.app.Activity", lpparam.classLoader);

            XposedBridge.hookAllMethods(activityClass, "onResume", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    try {
                        isDeviceInActiveUse = true;
                        lastInteractionTime = System.currentTimeMillis();
                        updateSocialContext();

                        // Check if recovering from notification interruption
                        if (activeNotificationCount > 0 && random.get().nextDouble() < 0.3) {
                            triggerAttentionRecovery();
                        }
                    } catch (Exception e) {
                        if (DEBUG) HookUtils.logDebug(TAG, "Error in onResume: " + e.getMessage());
                    }
                }
            });

            XposedBridge.hookAllMethods(activityClass, "onPause", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    try {
                        isDeviceInActiveUse = false;
                        updateSocialContext();
                    } catch (Exception e) {
                        if (DEBUG) HookUtils.logDebug(TAG, "Error in onPause: " + e.getMessage());
                    }
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked Activity lifecycle");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook Activity lifecycle", e);
        }
    }

    private static void processIncomingNotification(Object rankingMap) {
        activeNotificationCount++;
        lastNotificationTime = System.currentTimeMillis();

        // Determine notification type
        NotificationType type = determineNotificationType(rankingMap);
        
        // Create notification event
        boolean interrupted = isDeviceInActiveUse && random.get().nextDouble() < 0.4;
        boolean responded = interrupted && random.get().nextDouble() < immediateReplyProbability;

        NotificationEvent event = new NotificationEvent(
            type,
            "unknown",
            "Notification",
            interrupted,
            responded
        );

        recentNotifications.add(event);
        
        if (recentNotifications.size() > 50) {
            recentNotifications.remove(0);
        }

        notificationCounts.merge(type.name(), 1, Integer::sum);
        totalInterruptions++;

        if (responded) {
            totalResponses++;
        }

        // Trigger callback
        if (notificationCallback != null && interrupted) {
            notificationCallback.onInterruptionOccurred(event);
        }

        if (DEBUG && random.get().nextDouble() < 0.1) {
            HookUtils.logDebug(TAG, String.format(
                "Notification: type=%s, interrupted=%s, responded=%s, active=%d",
                type, interrupted, responded, activeNotificationCount
            ));
        }
    }

    private static void processNotification(int id, Notification notification) {
        try {
            Bundle extras = notification.extras;
            if (extras != null) {
                String title = extras.getCharSequence("android.title", "").toString();
                
                // Track notification
                activeNotificationCount++;
                lastNotificationTime = System.currentTimeMillis();

                if (DEBUG && random.get().nextDouble() < 0.05) {
                    HookUtils.logDebug(TAG, "Notification posted: " + title);
                }
            }
        } catch (Exception e) {
            if (DEBUG) HookUtils.logDebug(TAG, "Error processing notification: " + e.getMessage());
        }
    }

    private static NotificationType determineNotificationType(Object rankingMap) {
        double rand = random.get().nextDouble();
        
        if (rand < callProbability) {
            return NotificationType.INCOMING_CALL;
        } else if (rand < callProbability + messageProbability) {
            return NotificationType.SMS_MESSAGE;
        } else if (rand < callProbability + messageProbability + socialNotificationProbability) {
            return NotificationType.SOCIAL_MEDIA;
        } else if (rand < callProbability + messageProbability + socialNotificationProbability + workNotificationProbability) {
            return NotificationType.WORK_MESSAGE;
        } else {
            return NotificationType.OTHER;
        }
    }

    private static void updateSocialContext() {
        long idleTime = System.currentTimeMillis() - lastInteractionTime;
        
        if (idleTime > 300000) { // 5 minutes idle
            setContext(SocialContext.IDLE);
        } else if (isDeviceInActiveUse) {
            double rand = random.get().nextDouble();
            if (rand < 0.4) {
                setContext(SocialContext.PASSIVE_CONSUMPTION);
            } else if (rand < 0.7) {
                setContext(SocialContext.ACTIVE_ENGAGEMENT);
            } else if (rand < 0.85) {
                setContext(SocialContext.SOCIAL_COMMUNICATION);
            } else {
                setContext(SocialContext.WORK_FOCUS);
            }
        }
    }

    private static void setContext(SocialContext newContext) {
        if (currentContext != newContext) {
            currentContext = newContext;
            contextStartTime = System.currentTimeMillis();
            
            if (DEBUG) {
                HookUtils.logDebug(TAG, "Context changed to: " + newContext.name());
            }
        }
    }

    private static void triggerAttentionRecovery() {
        long recoveryTime = (long) (attentionRecoveryTimeMs * (0.5 + random.get().nextDouble()));
        
        // Add extra delay for attention recovery
        if (random.get().nextDouble() < 0.3) {
            recoveryTime *=1.5;
        }

        if  (notificationCallback != null) {
            notificationCallback.onAttentionRecovered();
        }

        if (DEBUG) {
            HookUtils.logDebug(TAG, "Attention recovery: " + recoveryTime + "ms");
        }
    }

    private static void startContextSimulationThread() {
        Thread simulationThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60000); // Check every minute
                    
                    if (!enabled) continue;
                    
                    // Update context based on time of day
                    updateContextFromTimeOfDay();
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    if (DEBUG) HookUtils.logDebug(TAG, "Simulation error: " + e.getMessage());
                }
            }
        });
        simulationThread.setName("SocialContextSimulator");
        simulationThread.setDaemon(true);
        simulationThread.start();
    }

    private static void updateContextFromTimeOfDay() {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        
        // Morning: work focus
        if (hour >= 9 && hour <= 11) {
            if (currentContext != SocialContext.WORK_FOCUS && random.get().nextDouble() < 0.3) {
                setContext(SocialContext.WORK_FOCUS);
            }
        }
        // Lunch: social
        else if (hour >= 12 && hour <= 13) {
            if (currentContext != SocialContext.SOCIAL_COMMUNICATION && random.get().nextDouble() < 0.4) {
                setContext(SocialContext.SOCIAL_COMMUNICATION);
            }
        }
        // Evening: passive consumption
        else if (hour >= 19 && hour <= 22) {
            if (currentContext != SocialContext.PASSIVE_CONSUMPTION && random.get().nextDouble() < 0.5) {
                setContext(SocialContext.PASSIVE_CONSUMPTION);
            }
        }
    }

    /**
     * Simulates a notification being dismissed
     */
    public static void simulateNotificationDismissal() {
        if (activeNotificationCount > 0) {
            activeNotificationCount--;
            
            if (random.get().nextDouble() < dismissalProbability) {
                if (DEBUG) HookUtils.logDebug(TAG, "Notification dismissed");
            }
        }
    }

    /**
     * Returns the probability that a notification will cause interruption
     */
    public static double getInterruptionProbability() {
        double baseProbability = 0.4;
        
        // Active engagement more likely to be interrupted
        if (currentContext == SocialContext.ACTIVE_ENGAGEMENT) {
            baseProbability *= 0.8;
        } else if (currentContext == SocialContext.WORK_FOCUS) {
            baseProbability *= 0.5;
        } else if (currentContext == SocialContext.SOCIAL_COMMUNICATION) {
            baseProbability *= 1.3;
        }
        
        // More notifications = higher chance of interruption
        baseProbability *= (1.0 + activeNotificationCount * 0.1);
        
        return Math.min(baseProbability, 0.95);
    }

    /**
     * Returns current attention level (0.0 - 1.0)
     */
    public static double getCurrentAttentionLevel() {
        double attention = 1.0;
        
        // Reduce attention with notifications
        attention -= activeNotificationCount * 0.1;
        
        // Reduce attention based on context
        switch (currentContext) {
            case PASSIVE_CONSUMPTION:
                attention *= 0.9;
                break;
            case IDLE:
                attention *= 0.3;
                break;
            case WORK_FOCUS:
                attention *= 1.1;
                break;
        }
        
        return Math.max(0.1, Math.min(1.0, attention));
    }

    // Configuration methods

    public static void setEnabled(boolean enabled) {
        SocialContextInterruptionHook.enabled = enabled;
        HookUtils.logInfo(TAG, "Hook " + (enabled ? "enabled" : "disabled"));
    }

    public static void setNotificationProbabilities(double call, double message, double social, double work) {
        callProbability = HookUtils.clamp(call, 0.0, 1.0);
        messageProbability = HookUtils.clamp(message, 0.0, 1.0);
        socialNotificationProbability = HookUtils.clamp(social, 0.0, 1.0);
        workNotificationProbability = HookUtils.clamp(work, 0.0, 1.0);
        
        HookUtils.logInfo(TAG, "Notification probabilities updated: call=" + callProbability + 
            ", message=" + messageProbability + ", social=" + socialNotificationProbability);
    }

    public static void setAttentionRecoveryTime(double timeMs) {
        attentionRecoveryTimeMs = HookUtils.clamp(timeMs, 500, 10000);
        HookUtils.logInfo(TAG, "Attention recovery time set to: " + attentionRecoveryTimeMs + "ms");
    }

    public static void setImmediateReplyProbability(double probability) {
        immediateReplyProbability = HookUtils.clamp(probability, 0.0, 1.0);
        HookUtils.logInfo(TAG, "Immediate reply probability set to: " + immediateReplyProbability);
    }

    public static void setCallback(NotificationCallback callback) {
        notificationCallback = callback;
    }

    public static SocialContext getCurrentContext() {
        return currentContext;
    }

    public static int getActiveNotificationCount() {
        return activeNotificationCount;
    }

    public static List<NotificationEvent> getRecentNotifications() {
        return new ArrayList<>(recentNotifications);
    }

    public static Map<String, Integer> getNotificationCounts() {
        return new HashMap<>(notificationCounts);
    }

    public static int getTotalInterruptions() {
        return totalInterruptions;
    }

    public static int getTotalResponses() {
        return totalResponses;
    }

    public static double getResponseRate() {
        return totalInterruptions > 0 ? (double) totalResponses / totalInterruptions : 0.0;
    }
}
