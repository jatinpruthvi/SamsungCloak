package com.samsungcloak.xposed;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

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

/**
 * AppTaskSequencePatternHook - App Usage Pattern Simulation
 * 
 * Simulates realistic app usage sequences:
 * - Typical workflow patterns (e.g., WhatsApp → Camera → Gallery)
 * - Session length variation
 * - Task switching frequency
 * - Time-of-day based usage
 * - Habitual sequences
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class AppTaskSequencePatternHook {

    private static final String TAG = "[App][Sequence]";
    private static final boolean DEBUG = true;
    
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    
    // Sequence patterns
    private static final Map<String, String[]> MORNING_SEQUENCES = new HashMap<>();
    private static final Map<String, String[]> EVENING_SEQUENCES = new HashMap<>();
    private static final Map<String, String[]> WORK_SEQUENCES = new HashMap<>();
    
    // Session parameters
    private static int minSessionDuration = 30000;   // 30 seconds
    private static int maxSessionDuration = 300000;  // 5 minutes
    private static float switchProbability = 0.3f;  // 30% chance to switch
    
    // State
    private static String currentApp = null;
    private static long sessionStartTime = 0;
    private static String lastApp = null;
    private static int currentHour = 12;
    private static boolean isWorkMode = false;
    
    private static final Random random = new Random();
    private static final List<TaskEvent> taskHistory = new CopyOnWriteArrayList<>();
    private static final Map<String, Integer> appUsageCount = new ConcurrentHashMap<>();
    
    static {
        // Morning sequences (6-11 AM)
        MORNING_SEQUENCES.put("com.whatsapp", new String[]{
            "com.whatsapp", "com.google.android.apps.photos", "com.samsung.android.camera"});
        MORNING_SEQUENCES.put("com.google.android.apps.maps", new String[]{
            "com.google.android.apps.maps", "com.google.android.apps.photos"});
            
        // Evening sequences (6-11 PM)
        EVENING_SEQUENCES.put("com.google.android.youtube", new String[]{
            "com.google.android.youtube", "com.google.android.apps.photos", "com.instagram.android"});
        EVENING_SEQUENCES.put("com.instagram.android", new String[]{
            "com.instagram.android", "com.facebook.katana", "com.snapchat.android"});
            
        // Work sequences
        WORK_SEQUENCES.put("com.google.android.gm", new String[]{
            "com.google.android.gm", "com.google.android.apps.tachyon", "com.google.android.calendar"});
        WORK_SEQUENCES.put("com.slack", new String[]{
            "com.slack", "com.google.android.gm", "com.microsoft.teams"});
    }
    
    public static class TaskEvent {
        public long timestamp;
        public String app;
        public String previousApp;
        public long sessionDuration;
        public String trigger;  // HABIT, MANUAL, NOTIFICATION, DEEP_LINK
        
        public TaskEvent(String app, String previousApp, long sessionDuration, String trigger) {
            this.timestamp = System.currentTimeMillis();
            this.app = app;
            this.previousApp = previousApp;
            this.sessionDuration = sessionDuration;
            this.trigger = trigger;
        }
    }
    
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (hookInitialized.getAndSet(true)) {
            HookUtils.logDebug(TAG, "Already initialized");
            return;
        }
        
        HookUtils.logInfo(TAG, "Initializing App Task Sequence Pattern Hook");
        
        try {
            hookActivityManager(lpparam);
            hookActivityLifecycle(lpparam);
            
            // Initialize current hour
            currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
            
            HookUtils.logInfo(TAG, "Task sequence hook initialized");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Init failed: " + t.getMessage());
            hookInitialized.set(false);
        }
    }
    
    private static void hookActivityManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> activityManagerClass = XposedHelpers.findClass(
                "android.app.ActivityManager", lpparam.classLoader
            );
            
            // Hook getRecentTasks
            XposedBridge.hookAllMethods(activityManagerClass, "getRecentTasks",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    // Track recent tasks for pattern analysis
                }
            });
            
            // Hook getRunningTasks
            XposedBridge.hookAllMethods(activityManagerClass, "getRunningTasks",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    // Could influence task ordering
                }
            });
            
            HookUtils.logInfo(TAG, "ActivityManager hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "ActivityManager hook failed: " + t.getMessage());
        }
    }
    
    private static void hookActivityLifecycle(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> activityClass = XposedHelpers.findClass(
                "android.app.Activity", lpparam.classLoader
            );
            
            // Hook onResume
            XposedBridge.hookAllMethods(activityClass, "onResume",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    Object activity = param.thisObject;
                    String packageName = activity.getClass().getPackage().getName();
                    
                    onAppSwitched(packageName);
                }
            });
            
            // Hook onPause - track session end
            XposedBridge.hookAllMethods(activityClass, "onPause",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    if (currentApp != null) {
                        long sessionDuration = System.currentTimeMillis() - sessionStartTime;
                        
                        taskHistory.add(new TaskEvent(currentApp, lastApp, 
                            sessionDuration, "MANUAL"));
                        
                        appUsageCount.merge(currentApp, 1, Integer::sum);
                        
                        lastApp = currentApp;
                        currentApp = null;
                    }
                }
            });
            
            HookUtils.logInfo(TAG, "Activity lifecycle hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Activity lifecycle hook failed: " + t.getMessage());
        }
    }
    
    private static void onAppSwitched(String packageName) {
        if (currentApp != null && !currentApp.equals(packageName)) {
            // Previous session ended
            long sessionDuration = System.currentTimeMillis() - sessionStartTime;
            
            taskHistory.add(new TaskEvent(currentApp, packageName, 
                sessionDuration, "SWITCH"));
            
            lastApp = currentApp;
        }
        
        currentApp = packageName;
        sessionStartTime = System.currentTimeMillis();
        
        // Check for habitual sequence
        String predictedApp = predictNextApp(packageName);
        if (predictedApp != null) {
            HookUtils.logDebug(TAG, "From " + packageName + " predicted: " + predictedApp);
        }
    }
    
    /**
     * Predict next app based on time and current app
     */
    public static String predictNextApp(String currentApp) {
        // Get sequence map based on time
        Map<String, String[]> sequences;
        
        if (currentHour >= 6 && currentHour <= 11) {
            sequences = MORNING_SEQUENCES;
        } else if (currentHour >= 18 && currentHour <= 23) {
            sequences = EVENING_SEQUENCES;
        } else if (isWorkMode) {
            sequences = WORK_SEQUENCES;
        } else {
            return null;
        }
        
        String[] sequence = sequences.get(currentApp);
        if (sequence == null || sequence.length == 0) {
            return null;
        }
        
        // Return next app in sequence with some probability
        if (random.nextFloat() < switchProbability) {
            int index = random.nextInt(sequence.length);
            return sequence[index];
        }
        
        return null;
    }
    
    /**
     * Get typical session duration for current time
     */
    public static long getTypicalSessionDuration() {
        // Morning: shorter sessions (checking news, weather)
        if (currentHour >= 6 && currentHour <= 11) {
            return minSessionDuration + random.nextInt(60000);
        }
        
        // Evening: longer sessions (video, social media)
        if (currentHour >= 18 && currentHour <= 23) {
            return maxSessionDuration - random.nextInt(60000);
        }
        
        // Work hours
        if (isWorkMode) {
            return minSessionDuration + random.nextInt(120000);
        }
        
        return minSessionDuration + random.nextInt(maxSessionDuration - minSessionDuration);
    }
    
    // ========== Configuration ==========
    
    public static void setEnabled(boolean enabled) {
        AppTaskSequencePatternHook.enabled = enabled;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void setWorkMode(boolean enabled) {
        isWorkMode = enabled;
    }
    
    public static void setCurrentHour(int hour) {
        currentHour = Math.max(0, Math.min(23, hour));
    }
    
    public static void setSessionDurationRange(int minMs, int maxMs) {
        minSessionDuration = Math.max(10000, minMs);
        maxSessionDuration = Math.max(minSessionDuration, maxMs);
    }
    
    public static void setSwitchProbability(float probability) {
        switchProbability = Math.max(0, Math.min(1, probability));
    }
    
    public static String getCurrentApp() {
        return currentApp;
    }
    
    public static long getSessionDuration() {
        if (sessionStartTime == 0) return 0;
        return System.currentTimeMillis() - sessionStartTime;
    }
    
    public static Map<String, Integer> getAppUsageCount() {
        return new HashMap<>(appUsageCount);
    }
    
    public static List<TaskEvent> getTaskHistory() {
        return new ArrayList<>(taskHistory);
    }
}
