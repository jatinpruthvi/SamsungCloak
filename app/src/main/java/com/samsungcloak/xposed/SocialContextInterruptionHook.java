package com.samsungcloak.xposed;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SocialContextInterruptionHook - Realistic Social Interruption Simulation
 *
 * Simulates incoming calls, messages, and social interruptions during app usage,
 * including realistic user response behaviors and multi-tasking patterns.
 *
 * Novel Dimensions:
 * 1. Incoming Call UI Behavior - Realistic call handling during app usage
 * 2. Message Notification Patterns - SMS/messaging app interruptions
 * 3. Social Etiquette Delays - Waiting to respond appropriately
 * 4. Multi-tasking Behaviors - App switching during interruptions
 * 5. Context Recovery - Return to original task after interruption
 *
 * Real-World Grounding (HCI Studies):
 * - Average call duration: 3-7 minutes
 * - Message response delay: 30 seconds - 15 minutes (context-dependent)
 * - Social media notification frequency: 15-45 per day
 * - Interruption recovery time: 23 minutes average to refocus
 *
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class SocialContextInterruptionHook {

    private static final String TAG = "[SocialContext][Interruption]";
    private static final boolean DEBUG = true;
    private static final ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new Random());

    private static boolean enabled = true;

    // Interruption frequency configuration
    private static boolean callInterruptionsEnabled = true;
    private static double callInterruptionProbability = 0.08; // Per 30-min session
    private static double callAnswerProbability = 0.65; // 65% answer calls
    private static long averageCallDurationMs = 240000; // 4 minutes

    // Message notification configuration
    private static boolean messageNotificationsEnabled = true;
    private static double messageNotificationProbability = 0.25; // Per session
    private static double immediateReplyProbability = 0.35; // Reply within 2 min
    private static double delayedReplyProbability = 0.45; // Reply within 15 min

    // Social context state
    private static SocialContext currentContext = SocialContext.CASUAL_BROWSING;
    private static boolean isInCall = false;
    private static long callStartTime = 0;
    private static final ConcurrentMap<String, Long> pendingReplies = new ConcurrentHashMap<>();
    private static final AtomicInteger interruptionCount = new AtomicInteger(0);

    // Recovery tracking
    private static long lastInterruptionEndTime = 0;
    private static long contextSwitchRecoveryMs = 1380000; // 23 minutes typical recovery

    public enum SocialContext {
        FOCUSED_WORK,      // Minimal interruptions expected
        CASUAL_BROWSING,   // Normal interruption rate
        SOCIAL_ENGAGED,    // High message/interaction rate
        WAITING_FOR_CALL,  // Expecting specific communication
        DO_NOT_DISTURB     // Minimal interruptions
    }

    public enum InterruptionType {
        INCOMING_CALL,
        MISSED_CALL,
        TEXT_MESSAGE,
        SOCIAL_MEDIA_NOTIFICATION,
        EMAIL_NOTIFICATION,
        APP_UPDATE_AVAILABLE
    }

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        HookUtils.logInfo(TAG, "Initializing Social Context Interruption Hook");

        try {
            determineInitialSocialContext();

            hookTelephonyManager(lpparam);
            hookNotificationManager(lpparam);
            hookBroadcastReceiver(lpparam);

            startInterruptionSimulationThread();

            HookUtils.logInfo(TAG, "Social Context Interruption Hook initialized");
            HookUtils.logInfo(TAG, String.format("Context: %s, Call prob: %.0f%%, Msg prob: %.0f%%",
                currentContext.name(), callInterruptionProbability * 100, messageNotificationProbability * 100));
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize hook", e);
        }
    }

    private static void determineInitialSocialContext() {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        int dayOfWeek = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK);
        boolean isWeekend = (dayOfWeek == java.util.Calendar.SATURDAY || dayOfWeek == java.util.Calendar.SUNDAY);

        if (hour >= 9 && hour <= 17 && !isWeekend) {
            // Work hours
            currentContext = SocialContext.FOCUSED_WORK;
            callInterruptionProbability = 0.04;
            messageNotificationProbability = 0.15;
        } else if (hour >= 19 && hour <= 23) {
            // Evening social time
            currentContext = SocialContext.SOCIAL_ENGAGED;
            callInterruptionProbability = 0.12;
            messageNotificationProbability = 0.45;
        } else {
            currentContext = SocialContext.CASUAL_BROWSING;
        }
    }

    private static void hookTelephonyManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> telephonyManagerClass = XposedHelpers.findClass(
                "android.telephony.TelephonyManager",
                lpparam.classLoader
            );

            // Hook getCallState to simulate incoming calls
            XposedBridge.hookAllMethods(telephonyManagerClass, "getCallState",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !callInterruptionsEnabled) return;

                        try {
                            // Simulate call state based on internal state
                            if (isInCall) {
                                param.setResult(TelephonyManager.CALL_STATE_OFFHOOK);
                            } else if (shouldSimulateRinging()) {
                                param.setResult(TelephonyManager.CALL_STATE_RINGING);
                            }
                        } catch (Exception e) {
                            if (DEBUG) HookUtils.logDebug(TAG, "Error in getCallState: " + e.getMessage());
                        }
                    }
                });

            // Hook listen for PhoneStateListener callbacks
            XposedBridge.hookAllMethods(telephonyManagerClass, "listen",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        try {
                            Object listener = param.args[0];
                            int events = (int) param.args[1];

                            if ((events & PhoneStateListener.LISTEN_CALL_STATE) != 0) {
                                // Inject simulated call state changes
                                injectSimulatedCallState(listener);
                            }
                        } catch (Exception e) {
                            if (DEBUG) HookUtils.logDebug(TAG, "Error in listen hook: " + e.getMessage());
                        }
                    }
                });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked TelephonyManager");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook TelephonyManager", e);
        }
    }

    private static void hookNotificationManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> notificationManagerClass = XposedHelpers.findClass(
                "android.app.NotificationManager",
                lpparam.classLoader
            );

            // Hook notify to track notification patterns
            XposedBridge.hookAllMethods(notificationManagerClass, "notify",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !messageNotificationsEnabled) return;

                        try {
                            // Track notification frequency
                            trackNotificationPattern(param);

                            // Apply social context delays
                            applyNotificationDelay(param);
                        } catch (Exception e) {
                            if (DEBUG) HookUtils.logDebug(TAG, "Error in notify hook: " + e.getMessage());
                        }
                    }
                });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked NotificationManager");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook NotificationManager", e);
        }
    }

    private static void hookBroadcastReceiver(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> broadcastReceiverClass = XposedHelpers.findClass(
                "android.content.BroadcastReceiver",
                lpparam.classLoader
            );

            XposedBridge.hookAllMethods(broadcastReceiverClass, "onReceive",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        try {
                            Context context = (Context) param.args[0];
                            Intent intent = (Intent) param.args[1];
                            String action = intent.getAction();

                            // Track interruption types
                            if (Intent.ACTION_NEW_OUTGOING_CALL.equals(action) ||
                                TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(action)) {
                                handleCallStateChange(intent);
                            }
                        } catch (Exception e) {
                            if (DEBUG) HookUtils.logDebug(TAG, "Error in onReceive: " + e.getMessage());
                        }
                    }
                });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked BroadcastReceiver");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook BroadcastReceiver", e);
        }
    }

    private static void startInterruptionSimulationThread() {
        Thread simulationThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60000); // Check every minute

                    if (!enabled) continue;

                    // Simulate potential interruptions
                    simulateInterruptions();

                    // Update social context based on time
                    updateSocialContext();

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    if (DEBUG) HookUtils.logDebug(TAG, "Simulation error: " + e.getMessage());
                }
            }
        });
        simulationThread.setName("SocialInterruptionSimulator");
        simulationThread.setDaemon(true);
        simulationThread.start();
    }

    private static void simulateInterruptions() {
        // Simulate incoming call
        if (random.get().nextDouble() < callInterruptionProbability / 30.0) {
            simulateIncomingCall();
        }

        // Simulate message notification
        if (random.get().nextDouble() < messageNotificationProbability / 30.0) {
            simulateMessageNotification();
        }
    }

    private static void simulateIncomingCall() {
        interruptionCount.incrementAndGet();

        boolean willAnswer = random.get().nextDouble() < callAnswerProbability;
        long callDuration = 0;

        if (willAnswer) {
            isInCall = true;
            callStartTime = System.currentTimeMillis();

            // Variable call duration: 30s - 10min
            callDuration = (long) (30000 + random.get().nextGaussian() * 180000);
            callDuration = Math.max(30000, Math.min(600000, callDuration));

            if (DEBUG) {
                HookUtils.logDebug(TAG, String.format(
                    "Incoming call simulated: answered=true, duration=%.1f min",
                    callDuration / 60000.0
                ));
            }

            // End call after duration
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                isInCall = false;
                lastInterruptionEndTime = System.currentTimeMillis();
            }, callDuration);
        } else {
            if (DEBUG) HookUtils.logDebug(TAG, "Incoming call simulated: missed");
        }
    }

    private static void simulateMessageNotification() {
        String senderId = "contact_" + random.get().nextInt(20);
        long currentTime = System.currentTimeMillis();

        // Determine reply behavior
        double replyRand = random.get().nextDouble();
        long replyDelay = 0;

        if (replyRand < immediateReplyProbability) {
            replyDelay = (long) (15000 + random.get().nextGaussian() * 30000); // 15s - 2min
        } else if (replyRand < immediateReplyProbability + delayedReplyProbability) {
            replyDelay = (long) (300000 + random.get().nextGaussian() * 600000); // 5-15 min
        }

        if (replyDelay > 0) {
            pendingReplies.put(senderId, currentTime + replyDelay);
        }

        if (DEBUG) {
            HookUtils.logDebug(TAG, String.format(
                "Message notification: sender=%s, replyDelay=%.1f min",
                senderId, replyDelay / 60000.0
            ));
        }
    }

    private static boolean shouldSimulateRinging() {
        // Only simulate ringing if not currently in a call
        return !isInCall && random.get().nextDouble() < 0.02;
    }

    private static void injectSimulatedCallState(Object listener) {
        // Use reflection to trigger PhoneStateListener callbacks
        try {
            Class<?> listenerClass = listener.getClass();
            java.lang.reflect.Method onCallStateChanged = listenerClass.getMethod(
                "onCallStateChanged", int.class, String.class
            );

            if (isInCall) {
                onCallStateChanged.invoke(listener, TelephonyManager.CALL_STATE_OFFHOOK, "");
            }
        } catch (Exception e) {
            if (DEBUG) HookUtils.logDebug(TAG, "Could not inject call state: " + e.getMessage());
        }
    }

    private static void handleCallStateChange(Intent intent) {
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
            interruptionCount.incrementAndGet();
        } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
            lastInterruptionEndTime = System.currentTimeMillis();
        }
    }

    private static void trackNotificationPattern(XC_MethodHook.MethodHookParam param) {
        // Track notification patterns for behavioral analysis
        try {
            Object notification = param.args[1];
            // Could extract notification type and track frequency
        } catch (Exception e) {
            // Silent fail
        }
    }

    private static void applyNotificationDelay(XC_MethodHook.MethodHookParam param) {
        // Apply social context-based delays
        if (currentContext == SocialContext.FOCUSED_WORK) {
            // Delay notifications during focus time
        }
    }

    private static void updateSocialContext() {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);

        // Gradually shift context based on time
        if (hour >= 9 && hour <= 17 && currentContext != SocialContext.FOCUSED_WORK) {
            if (random.get().nextDouble() < 0.1) {
                currentContext = SocialContext.FOCUSED_WORK;
            }
        }
    }

    // Configuration methods

    public static void setEnabled(boolean enabled) {
        SocialContextInterruptionHook.enabled = enabled;
        HookUtils.logInfo(TAG, "Hook " + (enabled ? "enabled" : "disabled"));
    }

    public static void setSocialContext(SocialContext context) {
        currentContext = context;
        HookUtils.logInfo(TAG, "Social context set to: " + context.name());
    }

    public static void setCallInterruptionProbability(double probability) {
        callInterruptionProbability = HookUtils.clamp(probability, 0.0, 1.0);
        HookUtils.logInfo(TAG, "Call interruption probability: " + callInterruptionProbability);
    }

    public static void setMessageNotificationProbability(double probability) {
        messageNotificationProbability = HookUtils.clamp(probability, 0.0, 1.0);
        HookUtils.logInfo(TAG, "Message notification probability: " + messageNotificationProbability);
    }

    public static int getInterruptionCount() {
        return interruptionCount.get();
    }

    public static boolean isInCall() {
        return isInCall;
    }

    public static long getTimeSinceLastInterruption() {
        return System.currentTimeMillis() - lastInterruptionEndTime;
    }

    public static boolean isContextSwitched() {
        return getTimeSinceLastInterruption() < contextSwitchRecoveryMs;
    }

    public static SocialContext getCurrentContext() {
        return currentContext;
    }
}
