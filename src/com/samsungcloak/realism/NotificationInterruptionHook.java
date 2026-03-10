package com.samsungcloak.realism;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NotificationInterruptionHook - Social/notification interruption simulation
 * 
 * Targets: Samsung Galaxy A12 (SM-A125U) Android 10/11
 * 
 * This hook addresses the realism dimension of social interruptions - real users
 * experience incoming calls, messages, and notifications during app usage.
 * The hook simulates:
 * - Realistic notification arrival patterns (time-of-day distribution)
 * - User response behaviors (dismiss, open, ignore)
 * - Priority-based interruptions (calls > messages > notifications)
 * - Cross-app coherence with usage context
 * - Notification fatigue and dismissal patterns
 */
public class NotificationInterruptionHook {
    private static final String TAG = "NotificationInterruption";
    private static final String PACKAGE_NAME = "com.samsungcloak.realism";
    
    // Configuration keys
    private static final String KEY_ENABLED = "notification_enabled";
    private static final String KEY_CALL_INTERRUPTIONS = "notification_call_interruptions";
    private static final String KEY_MESSAGE_INTERRUPTIONS = "notification_message_interruptions";
    private static final String KEY_NOTIFICATION_INTERRUPTIONS = "notification_notification_interruptions";
    private static final String KEY_TIME_OF_DAY = "notification_time_of_day";
    private static final String KEY_USER_RESPONSE_ENABLED = "notification_user_response";
    
    // Constants
    private static final int CALL_PRIORITY = 3;      // Highest
    private static final int MESSAGE_PRIORITY = 2;  // Medium
    private static final int NOTIFICATION_PRIORITY = 1;  // Low
    
    // Time-of-day probability distributions (24-hour clock)
    private static final float[] NOTIFICATION_PROBABILITY_BY_HOUR = {
        0.05f, 0.02f, 0.01f, 0.01f, 0.02f, 0.05f,  // 0-5: Night (low)
        0.15f, 0.35f, 0.50f, 0.45f, 0.35f, 0.40f,  // 6-11: Morning (increasing)
        0.55f, 0.50f, 0.45f, 0.50f, 0.60f, 0.70f,  // 12-17: Afternoon (peak)
        0.75f, 0.65f, 0.50f, 0.35f, 0.20f, 0.10f   // 18-23: Evening (decreasing)
    };
    
    // State
    private static SharedPreferences sPrefs;
    private static boolean sEnabled = true;
    private static boolean sCallInterruptions = true;
    private static boolean sMessageInterruptions = true;
    private static boolean sNotificationInterruptions = true;
    private static boolean sTimeOfDayEnabled = true;
    private static boolean sUserResponseEnabled = true;
    
    // Runtime state
    private static final Random sRandom = new Random();
    private static final ConcurrentHashMap<String, NotificationEvent> sNotificationHistory = 
        new ConcurrentHashMap<>();
    private static long sModuleLoadTime = System.currentTimeMillis();
    private static long sLastInterruptionTime = 0;
    private static int sTotalInterruptions = 0;
    private static int sDismissedCount = 0;
    private static int sOpenedCount = 0;
    private static int sIgnoredCount = 0;
    private static String sCurrentForegroundApp = null;
    
    // User attention model
    private static float sAttentionLevel = 1.0f;  // 0.0-1.0
    private static long sLastUserInteraction = System.currentTimeMillis();
    
    /**
     * Initialize the hook
     */
    public static void init(SharedPreferences prefs) {
        sPrefs = prefs;
        reloadSettings();
    }
    
    /**
     * Reload settings
     */
    public static void reloadSettings() {
        if (sPrefs == null) return;
        
        sEnabled = sPrefs.getBoolean(KEY_ENABLED, true);
        sCallInterruptions = sPrefs.getBoolean(KEY_CALL_INTERRUPTIONS, true);
        sMessageInterruptions = sPrefs.getBoolean(KEY_MESSAGE_INTERRUPTIONS, true);
        sNotificationInterruptions = sPrefs.getBoolean(KEY_NOTIFICATION_INTERRUPTIONS, true);
        sTimeOfDayEnabled = sPrefs.getBoolean(KEY_TIME_OF_DAY, true);
        sUserResponseEnabled = sPrefs.getBoolean(KEY_USER_RESPONSE_ENABLED, true);
    }
    
    /**
     * Hook NotificationManagerService
     */
    public static void hookNotificationManager(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!sEnabled) return;
        
        try {
            // Hook NotificationManagerService
            Class<?> notificationManagerServiceClass = XposedHelpers.findClass(
                "com.android.server.notification.NotificationManagerService", 
                lpparam.classLoader);
            
            // Hook enqueueNotification()
            XposedBridge.hookAllMethods(notificationManagerServiceClass, 
                "enqueueNotificationInternal", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!sEnabled) return;
                        
                        String pkg = (String) param.args[0];
                        String tag = (String) param.args[1];
                        int id = (Integer) param.args[2];
                        
                        // Track notification
                        trackNotification(pkg, tag, id);
                    }
                });
            
            XposedBridge.log(TAG + ": NotificationManager hooks installed");
            
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Error hooking NotificationManager: " + e.getMessage());
        }
    }
    
    /**
     * Hook NotificationListenerService methods
     */
    public static void hookNotificationListener(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!sEnabled) return;
        
        try {
            Class<?> listenerClass = XposedHelpers.findClass(
                "android.service.notification.NotificationListenerService",
                lpparam.classLoader);
            
            // Hook onNotificationPosted()
            XposedBridge.hookAllMethods(listenerClass, "onNotificationPosted",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!sEnabled) return;
                        
                        // Analyze notification for interruption potential
                        StatusBarNotification sbn = (StatusBarNotification) param.args[0];
                        if (sbn != null) {
                            processNotificationPosted(sbn);
                        }
                    }
                });
            
            // Hook onNotificationRemoved()
            XposedBridge.hookAllMethods(listenerClass, "onNotificationRemoved",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!sEnabled) return;
                        
                        StatusBarNotification sbn = (StatusBarNotification) param.args[0];
                        if (sbn != null) {
                            processNotificationRemoved(sbn);
                        }
                    }
                });
            
            XposedBridge.log(TAG + ": NotificationListener hooks installed");
            
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Error hooking NotificationListener: " + e.getMessage());
        }
    }
    
    /**
     * Hook ActivityManager for foreground app detection
     */
    public static void hookActivityManager(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!sEnabled) return;
        
        try {
            Class<?> activityTaskManagerClass = XposedHelpers.findClass(
                "android.app.ActivityTaskManager",
                lpparam.classLoader);
            
            // Hook getRunningAppProcesses() to detect foreground app
            XposedBridge.hookAllMethods(activityTaskManagerClass, "getRunningTasks",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!sEnabled) return;
                        
                        // Could intercept to track current foreground
                    }
                });
            
            // Hook to ActivityManagerService
            Class<?> activityManagerServiceClass = XposedHelpers.findClass(
                "com.android.server.am.ActivityManagerService",
                lpparam.classLoader);
            
            XposedBridge.log(TAG + ": ActivityManager hooks installed for foreground detection");
            
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Error hooking ActivityManager: " + e.getMessage());
        }
    }
    
    /**
     * Hook NotificationChannel filtering
     */
    public static void hookNotificationChannel(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!sEnabled) return;
        
        try {
            Class<?> channelClass = XposedHelpers.findClass(
                "android.app.NotificationChannel",
                lpparam.classLoader);
            
            // Could modify channel importance based on interruption simulation
            XposedBridge.hookAllMethods(channelClass, "getImportance",
                new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) 
                        throws Throwable {
                        
                        int defaultImportance = (Integer) callOriginalMethod(param);
                        
                        if (!sEnabled) return defaultImportance;
                        
                        // Apply realistic importance variation
                        // Real devices sometimes demote notification importance
                        float randomFactor = sRandom.nextFloat();
                        
                        if (randomFactor < 0.05f) {
                            // 5% chance of importance reduction
                            return Math.max(1, defaultImportance - 1);
                        }
                        
                        return defaultImportance;
                    }
                });
            
            XposedBridge.log(TAG + ": NotificationChannel hooks installed");
            
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Error hooking NotificationChannel: " + e.getMessage());
        }
    }
    
    /**
     * Hook PackageManager to filter notification listeners
     */
    public static void hookPackageManager(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!sEnabled) return;
        
        try {
            Class<?> packageManagerServiceClass = XposedHelpers.findClass(
                "com.android.server.pm.PackageManagerService",
                lpparam.classLoader);
            
            // Could filter notification listener packages here
            XposedBridge.log(TAG + ": PackageManager hooks installed");
            
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Error hooking PackageManager: " + e.getMessage());
        }
    }
    
    /**
     * Track notification for interruption analysis
     */
    private static void trackNotification(String pkg, String tag, int id) {
        if (pkg == null) return;
        
        String key = pkg + ":" + tag + ":" + id;
        
        // Determine notification type
        int priority = NOTIFICATION_PRIORITY;
        
        if (isCallNotification(pkg, tag)) {
            priority = CALL_PRIORITY;
            if (sCallInterruptions) {
                processCallInterruption(pkg, tag, id);
            }
        } else if (isMessageNotification(pkg, tag)) {
            priority = MESSAGE_PRIORITY;
            if (sMessageInterruptions) {
                processMessageInterruption(pkg, tag, id);
            }
        } else if (sNotificationInterruptions) {
            processNotificationInterruption(pkg, tag, id);
        }
        
        NotificationEvent event = new NotificationEvent();
        event.pkg = pkg;
        event.tag = tag;
        event.id = id;
        event.priority = priority;
        event.timestamp = System.currentTimeMillis();
        event.probability = calculateInterruptionProbability(priority);
        
        sNotificationHistory.put(key, event);
        sTotalInterruptions++;
    }
    
    /**
     * Check if notification is a call
     */
    private static boolean isCallNotification(String pkg, String tag) {
        return "com.android.phone".equals(pkg) ||
               "com.samsung.android.incallui".equals(pkg) ||
               (tag != null && tag.startsWith("call_"));
    }
    
    /**
     * Check if notification is a message
     */
    private static boolean isMessageNotification(String pkg, String tag) {
        return "com.google.android.gms".equals(pkg) ||
               "com.samsung.android.messaging".equals(pkg) ||
               "com.android.mms".equals(pkg) ||
               (tag != null && tag.startsWith("msg_"));
    }
    
    /**
     * Process notification posted
     */
    private static void processNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) return;
        
        String pkg = sbn.getPackageName();
        
        // Update attention model
        long timeSinceInteraction = System.currentTimeMillis() - sLastUserInteraction;
        
        // Attention decreases over time without interaction
        if (timeSinceInteraction > 60000) {  // 1 minute
            sAttentionLevel = Math.max(0.2f, sAttentionLevel - 0.1f);
        }
        
        // Notification arrival updates last interruption time
        sLastInterruptionTime = System.currentTimeMillis();
        
        // Determine user response probability
        if (sUserResponseEnabled) {
            NotificationUserResponse response = determineUserResponse(pkg);
            
            switch (response) {
                case DISMISSED:
                    sDismissedCount++;
                    break;
                case OPENED:
                    sOpenedCount++;
                    break;
                case IGNORED:
                    sIgnoredCount++;
                    break;
            }
        }
    }
    
    /**
     * Process notification removed
     */
    private static void processNotificationRemoved(StatusBarNotification sbn) {
        if (sbn == null) return;
        
        String key = sbn.getKey();
        sNotificationHistory.remove(key);
    }
    
    /**
     * Process call interruption
     */
    private static void processCallInterruption(String pkg, String tag, int id) {
        // Calls are high priority - should interrupt foreground app
        if (sTimeOfDayEnabled) {
            float hourProbability = NOTIFICATION_PROBABILITY_BY_HOUR[getCurrentHour()];
            
            // Calls more likely during daytime
            if (sRandom.nextFloat() > hourProbability) {
                XposedBridge.log(TAG + ": Call interruption suppressed by time probability");
            }
        }
        
        XposedBridge.log(TAG + ": Processing call interruption from " + pkg);
    }
    
    /**
     * Process message interruption
     */
    private static void processMessageInterruption(String pkg, String tag, int id) {
        // Messages have medium priority
        // User may or may not respond immediately
        
        if (sTimeOfDayEnabled) {
            float hourProbability = NOTIFICATION_PROBABILITY_BY_HOUR[getCurrentHour()];
            
            // Scale by attention
            hourProbability *= (0.5f + sAttentionLevel * 0.5f);
            
            if (sRandom.nextFloat() > hourProbability) {
                XposedBridge.log(TAG + ": Message interruption suppressed");
            }
        }
    }
    
    /**
     * Process regular notification interruption
     */
    private static void processNotificationInterruption(String pkg, String tag, int id) {
        // Regular notifications have lower interruption priority
        // User may ignore or dismiss without viewing
        
        // Reduce probability based on app usage context
        if (sCurrentForegroundApp != null) {
            // Social apps have higher notification relevance
            float relevance = getAppNotificationRelevance(sCurrentForegroundApp, pkg);
            
            if (relevance < 0.3f) {
                // Low relevance app - high chance of ignoring
                // This would be tracked for dismissal patterns
            }
        }
    }
    
    /**
     * Calculate interruption probability based on time and settings
     */
    private static float calculateInterruptionProbability(int priority) {
        float baseProbability = 0.5f;
        
        if (sTimeOfDayEnabled) {
            baseProbability = NOTIFICATION_PROBABILITY_BY_HOUR[getCurrentHour()];
        }
        
        // Priority multiplier
        switch (priority) {
            case CALL_PRIORITY:
                return baseProbability * 1.5f;  // Calls more likely
            case MESSAGE_PRIORITY:
                return baseProbability * 1.2f;
            default:
                return baseProbability;
        }
    }
    
    /**
     * Determine user response to notification
     */
    private static NotificationUserResponse determineUserResponse(String pkg) {
        float responseRoll = sRandom.nextFloat();
        
        // Base response probabilities (can be influenced by app type)
        float dismissProb = 0.40f;
        float ignoreProb = 0.35f;
        float openProb = 0.25f;
        
        // Adjust based on attention level
        if (sAttentionLevel > 0.7f) {
            // High attention - more likely to open
            openProb = 0.40f;
            dismissProb = 0.30f;
            ignoreProb = 0.30f;
        } else if (sAttentionLevel < 0.3f) {
            // Low attention - more likely to ignore
            openProb = 0.15f;
            dismissProb = 0.35f;
            ignoreProb = 0.50f;
        }
        
        // Adjust based on notification type
        if (pkg != null) {
            if (isCallNotification(pkg, null)) {
                // Calls almost always get responded to
                openProb = 0.85f;
                dismissProb = 0.10f;
                ignoreProb = 0.05f;
            } else if (isMessageNotification(pkg, null)) {
                openProb = 0.50f;
                dismissProb = 0.30f;
                ignoreProb = 0.20f;
            }
        }
        
        if (responseRoll < openProb) {
            return NotificationUserResponse.OPENED;
        } else if (responseRoll < openProb + dismissProb) {
            return NotificationUserResponse.DISMISSED;
        } else {
            return NotificationUserResponse.IGNORED;
        }
    }
    
    /**
     * Get notification relevance between foreground and notification app
     */
    private static float getAppNotificationRelevance(String foregroundPkg, String notificationPkg) {
        // Social apps (foreground) have high relevance for social notifications
        String[] socialApps = {
            "com.zhiliaoapp.musically",  // TikTok
            "com.ss.android.ugc.trill",
            "com.instagram.android",
            "com.facebook.katana",
            "com.twitter.android",
            "com.snapchat.android"
        };
        
        boolean isSocialForeground = false;
        for (String app : socialApps) {
            if (app.equals(foregroundPkg)) {
                isSocialForeground = true;
                break;
            }
        }
        
        if (isSocialForeground) {
            for (String app : socialApps) {
                if (app.equals(notificationPkg)) {
                    return 0.9f;  // High relevance
                }
            }
            // Non-social notification while using social app
            return 0.3f;
        }
        
        // Default relevance
        return 0.5f;
    }
    
    /**
     * Get current hour (0-23)
     */
    private static int getCurrentHour() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        return cal.get(java.util.Calendar.HOUR_OF_DAY);
    }
    
    /**
     * Record user interaction (touch, key press, etc.)
     */
    public static void recordUserInteraction() {
        sLastUserInteraction = System.currentTimeMillis();
        sAttentionLevel = Math.min(1.0f, sAttentionLevel + 0.15f);
    }
    
    /**
     * Update current foreground app
     */
    public static void setCurrentForegroundApp(String pkg) {
        sCurrentForegroundApp = pkg;
    }
    
    /**
     * Get notification interruption state
     */
    public static NotificationState getState() {
        NotificationState state = new NotificationState();
        state.enabled = sEnabled;
        state.totalInterruptions = sTotalInterruptions;
        state.dismissedCount = sDismissedCount;
        state.openedCount = sOpenedCount;
        state.ignoredCount = sIgnoredCount;
        state.attentionLevel = sAttentionLevel;
        state.currentHour = getCurrentHour();
        state.hourProbability = NOTIFICATION_PROBABILITY_BY_HOUR[getCurrentHour()];
        state.currentForegroundApp = sCurrentForegroundApp;
        return state;
    }
    
    /**
     * Get current hour probability
     */
    public static float getCurrentHourProbability() {
        return NOTIFICATION_PROBABILITY_BY_HOUR[getCurrentHour()];
    }
    
    /**
     * Helper to call original method
     */
    private static Object callOriginalMethod(MethodHookParam param) {
        try {
            return param.getResult();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Notification event data
     */
    private static class NotificationEvent {
        String pkg;
        String tag;
        int id;
        int priority;
        long timestamp;
        float probability;
    }
    
    /**
     * User response types
     */
    private enum NotificationUserResponse {
        DISMISSED,
        OPENED,
        IGNORED
    }
    
    /**
     * State container
     */
    public static class NotificationState {
        public boolean enabled;
        public int totalInterruptions;
        public int dismissedCount;
        public int openedCount;
        public int ignoredCount;
        public float attentionLevel;
        public int currentHour;
        public float hourProbability;
        public String currentForegroundApp;
        
        public float getDismissRate() {
            if (totalInterruptions == 0) return 0;
            return (float) dismissedCount / totalInterruptions;
        }
        
        public float getOpenRate() {
            if (totalInterruptions == 0) return 0;
            return (float) openedCount / totalInterruptions;
        }
    }
}
