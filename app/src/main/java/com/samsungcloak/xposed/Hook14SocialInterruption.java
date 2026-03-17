package com.samsungcloak.xposed;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.telecom.TelecomManager;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hook14SocialInterruption - Social Interruption Simulation
 * 
 * Simulates incoming calls/SMS during app usage based on engagement depth:
 * - Call interruptions at varying engagement levels
 * - SMS notifications with different patterns
 * - Dismissal patterns simulation:
 *   - Swipe-away: 70-95%
 *   - Partial view: 40%
 *   - Multi-step: 15%
 * - Notification burst scenarios during idle periods
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class Hook14SocialInterruption {

    private static final String TAG = "[Social][Hook14]";
    private static final boolean DEBUG = true;
    private static final ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new Random());

    // Configuration
    private static boolean enabled = true;
    private static float intensity = 0.5f;

    // Call interruption settings
    private static boolean callInterruptionEnabled = true;
    private static double highEngagementCallRate = 0.08;  // 8% chance during high engagement
    private static double mediumEngagementCallRate = 0.15;
    private static double lowEngagementCallRate = 0.25;

    // SMS interruption settings  
    private static boolean smsInterruptionEnabled = true;
    private static double highEngagementSmsRate = 0.12;
    private static double mediumEngagementSmsRate = 0.20;
    private static double lowEngagementSmsRate = 0.35;

    // Notification burst settings
    private static boolean burstEnabled = true;
    private static double burstProbability = 0.05;
    private static int burstMinNotifications = 3;
    private static int burstMaxNotifications = 8;
    private static long burstIntervalMs = 2000;

    // Dismissal patterns
    private static boolean dismissalEnabled = true;
    private static double swipeAwayProbability = 0.80;  // 70-95% average
    private static double partialViewProbability = 0.15;
    private static double multiStepProbability = 0.05;

    // Statistics
    private static final AtomicInteger totalCallsReceived = new AtomicInteger(0);
    private static final AtomicInteger totalSmsReceived = new AtomicInteger(0);
    private static final AtomicInteger callsDismissed = new AtomicInteger(0);
    private static final AtomicInteger smsDismissed = new AtomicInteger(0);
    private static final AtomicInteger notificationsViewed = new AtomicInteger(0);
    private static final AtomicLong lastInterruptionTime = new AtomicLong(0);

    // State tracking
    private static final CopyOnWriteArrayList<InterruptionEvent> recentInterruptions = 
        new CopyOnWriteArrayList<>();
    private static long sessionStartTime = 0;
    private static boolean userIsActive = true;
    private static int sessionInteractionCount = 0;

    // Handler for delayed notifications
    private static Handler interruptHandler;
    private static Runnable burstRunnable;

    public enum InterruptionType {
        INCOMING_CALL,
        SMS_RECEIVED,
        NOTIFICATION_BURST,
        MISSED_CALL,
        VOICEMAIL
    }

    public enum DismissalPattern {
        SWIPE_AWAY,
        PARTIAL_VIEW,
        MULTI_STEP,
        IGNORED
    }

    public static class InterruptionEvent {
        public final InterruptionType type;
        public final long timestamp;
        public final float engagementAtTime;
        public final DismissalPattern dismissal;
        public final int viewDurationMs;

        public InterruptionEvent(InterruptionType type, float engagement, DismissalPattern dismissal, int viewMs) {
            this.type = type;
            this.timestamp = System.currentTimeMillis();
            this.engagementAtTime = engagement;
            this.dismissal = dismissal;
            this.viewDurationMs = viewMs;
        }
    }

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!enabled) return;

        HookUtils.logInfo(TAG, "Initializing Social Interruption Hook 14");

        try {
            sessionStartTime = System.currentTimeMillis();
            interruptHandler = new Handler(Looper.getMainLooper());

            hookTelecomManager(lpparam);
            hookNotificationManager(lpparam);
            hookTelephonyManager(lpparam);

            HookUtils.logInfo(TAG, "Social Interruption Hook 14 initialized");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize", e);
        }
    }

    /**
     * Hook TelecomManager for call handling
     */
    private static void hookTelecomManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> telecomManagerClass = XposedHelpers.findClass(
                "android.telecom.TelecomManager", lpparam.classLoader);

            // Hook showInCallScreen
            XposedBridge.hookMethod(
                XposedHelpers.findMethodExact(telecomManagerClass, "showInCallScreen", 
                    boolean.class),
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!callInterruptionEnabled || !enabled) return;

                        float effectiveIntensity = getEffectiveIntensity();
                        float engagement = getCurrentEngagement();

                        // Determine call interruption chance based on engagement
                        double callRate;
                        if (engagement > 0.7) {
                            callRate = highEngagementCallRate;
                        } else if (engagement > 0.4) {
                            callRate = mediumEngagementCallRate;
                        } else {
                            callRate = lowEngagementCallRate;
                        }

                        if (random.get().nextDouble() < callRate * effectiveIntensity) {
                            totalCallsReceived.incrementAndGet();
                            lastInterruptionTime.set(System.currentTimeMillis());
                            
                            // Simulate dismissal
                            DismissalPattern dismissal = simulateDismissal(engagement);
                            callsDismissed.incrementAndGet();
                            
                            InterruptionEvent event = new InterruptionEvent(
                                InterruptionType.INCOMING_CALL, engagement, 
                                dismissal, random.get().nextInt(5000));
                            recentInterruptions.add(event);

                            HookUtils.logDebug(TAG, String.format(
                                "Call interruption: engagement=%.2f, dismissal=%s",
                                engagement, dismissal));
                        }
                    }
                });

            // Hook placeCall
            XposedBridge.hookMethod(
                XposedHelpers.findMethodExact(telecomManagerClass, "placeCall", 
                    android.net.Uri.class, android.os.Bundle.class),
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        // Track outgoing calls
                        sessionInteractionCount++;
                    }
                });

            HookUtils.logDebug(TAG, "TelecomManager hooked");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook TelecomManager", e);
        }
    }

    /**
     * Hook NotificationManager for SMS/notification handling
     */
    private static void hookNotificationManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> notificationManagerClass = NotificationManager.class;

            // Hook notify methods
            XposedBridge.hookMethod(
                XposedHelpers.findMethodExact(notificationManagerClass, "notify", 
                    String.class, int.class, Notification.class),
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!smsInterruptionEnabled || !enabled) return;

                        String tag = (String) param.args[0];
                        int id = (int) param.args[1];
                        Notification notification = (Notification) param.args[2];

                        // Only process SMS and call notifications
                        if (tag == null || !tag.contains("sms") && !tag.contains("call")) {
                            return;
                        }

                        float effectiveIntensity = getEffectiveIntensity();
                        float engagement = getCurrentEngagement();

                        // Determine SMS interruption chance
                        double smsRate;
                        if (engagement > 0.7) {
                            smsRate = highEngagementSmsRate;
                        } else if (engagement > 0.4) {
                            smsRate = mediumEngagementSmsRate;
                        } else {
                            smsRate = lowEngagementSmsRate;
                        }

                        if (random.get().nextDouble() < smsRate * effectiveIntensity) {
                            totalSmsReceived.incrementAndGet();
                            lastInterruptionTime.set(System.currentTimeMillis());
                            
                            // Simulate dismissal
                            DismissalPattern dismissal = simulateDismissal(engagement);
                            
                            if (dismissal == DismissalPattern.SWIPE_AWAY) {
                                smsDismissed.incrementAndGet();
                            } else if (dismissal == DismissalPattern.PARTIAL_VIEW) {
                                notificationsViewed.incrementAndGet();
                            }
                            
                            InterruptionEvent event = new InterruptionEvent(
                                InterruptionType.SMS_RECEIVED, engagement,
                                dismissal, random.get().nextInt(3000));
                            recentInterruptions.add(event);

                            HookUtils.logDebug(TAG, String.format(
                                "SMS notification: engagement=%.2f, dismissal=%s",
                                engagement, dismissal));
                        }
                    }
                });

            HookUtils.logDebug(TAG, "NotificationManager hooked");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook NotificationManager", e);
        }
    }

    /**
     * Hook TelephonyManager for call state
     */
    private static void hookTelephonyManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> telephonyManagerClass = XposedHelpers.findClass(
                "android.telephony.TelephonyManager", lpparam.classLoader);

            // Hook getCallState
            XposedBridge.hookMethod(
                XposedHelpers.findMethodExact(telephonyManagerClass, "getCallState", int.class),
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        int callState = (int) param.getResult();
                        // Track call state changes
                        if (callState == 0) { // CALL_STATE_IDLE
                            userIsActive = false;
                        }
                    }
                });

            HookUtils.logDebug(TAG, "TelephonyManager hooked");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook TelephonyManager", e);
        }
    }

    /**
     * Simulate notification dismissal based on engagement
     */
    private static DismissalPattern simulateDismissal(float engagement) {
        double roll = random.get().nextDouble();
        
        // Higher engagement = more likely to swipe away immediately
        if (roll < swipeAwayProbability * (0.5 + engagement * 0.5)) {
            return DismissalPattern.SWIPE_AWAY;
        }
        // Medium engagement = partial view more likely
        else if (roll < swipeAwayProbability + partialViewProbability) {
            return DismissalPattern.PARTIAL_VIEW;
        }
        // Low engagement = multi-step dismissal more likely
        else {
            return DismissalPattern.MULTI_STEP;
        }
    }

    /**
     * Get current engagement level (0.0 - 1.0)
     */
    private static float getCurrentEngagement() {
        try {
            RealityCoordinator coordinator = RealityCoordinator.getInstance();
            float baseEngagement = coordinator.getEngagementLevel();
            
            // Factor in session duration
            long sessionDuration = System.currentTimeMillis() - sessionStartTime;
            float sessionDecay = Math.min(1.0f, sessionDuration / 600000f); // Decay over 10 minutes
            
            return baseEngagement * (1.0f - sessionDecay * 0.3f);
        } catch (Exception e) {
            return 0.5f;
        }
    }

    /**
     * Get effective intensity
     */
    private static float getEffectiveIntensity() {
        ConfigurationManager config = BaseRealismHook.configManager;
        float configIntensity = config != null ? config.getHookIntensity("hook_14") : intensity;
        return intensity * configIntensity;
    }

    /**
     * Trigger notification burst during idle
     */
    public static void triggerNotificationBurst() {
        if (!burstEnabled || !enabled) return;

        float effectiveIntensity = getEffectiveIntensity();
        if (random.get().nextDouble() > burstProbability * effectiveIntensity) return;

        int notificationCount = random.get().nextInt(
            burstMaxNotifications - burstMinNotifications + 1) + burstMinNotifications;

        HookUtils.logInfo(TAG, "Triggering notification burst: " + notificationCount + " notifications");

        // Bursts are tracked but actual notifications come from system
        for (int i = 0; i < notificationCount; i++) {
            final int index = i;
            interruptHandler.postDelayed(() -> {
                InterruptionEvent event = new InterruptionEvent(
                    InterruptionType.NOTIFICATION_BURST, 0.2f,
                    DismissalPattern.IGNORED, 0);
                recentInterruptions.add(event);
            }, i * burstIntervalMs);
        }
    }

    /**
     * Get interruption statistics
     */
    public static String getStats() {
        return String.format("SocialInterruption[calls=%d, sms=%d, dismissed=%d, viewed=%d, recent=%d]",
            totalCallsReceived.get(), totalSmsReceived.get(), 
            smsDismissed.get(), notificationsViewed.get(), recentInterruptions.size());
    }

    /**
     * Record user interaction
     */
    public static void recordInteraction() {
        sessionInteractionCount++;
        userIsActive = true;
    }
}
