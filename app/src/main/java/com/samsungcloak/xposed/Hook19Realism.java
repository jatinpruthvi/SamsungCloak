package com.samsungcloak.xposed;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Hook19Realism - App Session & Task Switching Patterns
 * 
 * Simulates realistic app session and task switching behavior for Samsung Galaxy A12:
 * - Session length distribution: micro 5-30s (20-30%), short 30-120s (25-35%), medium 2-5min (25-35%), long 5-15min (10-20%)
 * - Task switching frequency: low 0-3/hour (20-30%), moderate 3-10/hour (40-50%), high 10-30/hour (20-30%)
 * - App category patterns: communication (short sessions, high switching), social media (long sessions), productivity (medium sessions)
 * - Interruption handling: interruption→resumption 60-75%, interruption→abandonment 25-40%
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class Hook19Realism extends BaseRealismHook {

    private static final String TAG = "[Realism][Hook19-Session]";
    private static final boolean DEBUG = true;
    
    private static final String HOOK_ID = "hook_19";
    private static final String HOOK_NAME = "App Session & Task Switching Patterns";
    
    // Configuration keys
    private static final String KEY_ENABLED = "session_patterns_enabled";
    private static final String KEY_SESSION_LENGTH = "session_length_enabled";
    private static final String KEY_TASK_SWITCHING = "task_switching_enabled";
    private static final String KEY_INTERRUPTION = "interruption_handling_enabled";
    
    // Hook state
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    private static AtomicBoolean enabled = new AtomicBoolean(true);
    private static float intensity = 0.5f;
    
    // Session types
    public enum SessionType {
        MICRO,      // 5-30 seconds
        SHORT,      // 30-120 seconds
        MEDIUM,     // 2-5 minutes
        LONG        // 5-15 minutes
    }
    
    // Switching frequency
    public enum SwitchingFrequency {
        LOW,        // 0-3/hour
        MODERATE,   // 3-10/hour
        HIGH        // 10-30/hour
    }
    
    // App categories
    public enum AppCategory {
        COMMUNICATION,
        SOCIAL_MEDIA,
        PRODUCTIVITY,
        ENTERTAINMENT,
        UTILITY,
        SYSTEM,
        OTHER
    }
    
    // Current session info
    private static final AtomicReference<String> currentApp = 
        new AtomicReference<>("");
    private static final AtomicReference<AppCategory> currentCategory = 
        new AtomicReference<>(AppCategory.OTHER);
    private static final AtomicReference<SessionType> currentSessionType = 
        new AtomicReference<>(SessionType.MEDIUM);
    private static final AtomicReference<SwitchingFrequency> switchingFrequency = 
        new AtomicReference<>(SwitchingFrequency.MODERATE);
    
    // Session timing
    private static long sessionStartTime = 0;
    private static long lastSwitchTime = 0;
    private static int switchesThisHour = 0;
    private static long hourStartTime = 0;
    
    // Session statistics
    private static final AtomicInteger microSessions = new AtomicInteger(0);
    private static final AtomicInteger shortSessions = new AtomicInteger(0);
    private static final AtomicInteger mediumSessions = new AtomicInteger(0);
    private static final AtomicInteger longSessions = new AtomicInteger(0);
    private static final AtomicInteger totalSwitches = new AtomicInteger(0);
    private static final AtomicInteger resumptions = new AtomicInteger(0);
    private static final AtomicInteger abandonments = new AtomicInteger(0);
    
    // Session length distribution (percentages)
    private static float microSessionPercent = 0.25f;
    private static float shortSessionPercent = 0.30f;
    private static float mediumSessionPercent = 0.30f;
    private static float longSessionPercent = 0.15f;
    
    // Switching frequency distribution
    private static float lowSwitchingPercent = 0.25f;
    private static float moderateSwitchingPercent = 0.45f;
    private static float highSwitchingPercent = 0.30f;
    
    // Interruption handling
    private static float resumptionProbability = 0.68f;
    private static float abandonmentProbability = 0.32f;
    
    // App session history
    private static final List<AppSession> sessionHistory = 
        new CopyOnWriteArrayList<>();
    private static final Map<String, AppSessionStats> appStats = 
        new ConcurrentHashMap<>();
    private static final int MAX_HISTORY = 100;
    
    // Thread-local random
    private static final ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new Random());
    
    // Category detection
    private static final Map<String, AppCategory> appCategories = new ConcurrentHashMap<>();
    static {
        // Communication
        appCategories.put("com.android.mms", AppCategory.COMMUNICATION);
        appCategories.put("com.google.android.apps.messaging", AppCategory.COMMUNICATION);
        appCategories.put("com.samsung.android.messaging", AppCategory.COMMUNICATION);
        appCategories.put("com.whatsapp", AppCategory.COMMUNICATION);
        appCategories.put("com.telegram", AppCategory.COMMUNICATION);
        appCategories.put("com.signal", AppCategory.COMMUNICATION);
        
        // Social Media
        appCategories.put("com.instagram.android", AppCategory.SOCIAL_MEDIA);
        appCategories.put("com.facebook.katana", AppCategory.SOCIAL_MEDIA);
        appCategories.put("com.twitter.android", AppCategory.SOCIAL_MEDIA);
        appCategories.put("com.snapchat.android", AppCategory.SOCIAL_MEDIA);
        appCategories.put("com.tiktok", AppCategory.SOCIAL_MEDIA);
        
        // Productivity
        appCategories.put("com.google.android.apps.docs", AppCategory.PRODUCTIVITY);
        appCategories.put("com.microsoft.office.word", AppCategory.PRODUCTIVITY);
        appCategories.put("com.google.android.apps.calendar", AppCategory.PRODUCTIVITY);
        appCategories.put("com.google.android.gm", AppCategory.PRODUCTIVITY);
        appCategories.put("com.android.email", AppCategory.PRODUCTIVITY);
        
        // Entertainment
        appCategories.put("com.netflix.mediaclient", AppCategory.ENTERTAINMENT);
        appCategories.put("com.google.android.youtube", AppCategory.ENTERTAINMENT);
        appCategories.put("com.spotify.music", AppCategory.ENTERTAINMENT);
        appCategories.put("com.amazon.music", AppCategory.ENTERTAINMENT);
        
        // Utility
        appCategories.put("com.google.android.apps.photos", AppCategory.UTILITY);
        appCategories.put("com.google.android.apps.translate", AppCategory.UTILITY);
        appCategories.put("com.google.android.apps.maps", AppCategory.UTILITY);
    }
    
    public Hook19Realism() {
        super(HOOK_ID, HOOK_NAME);
    }
    
    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!hookInitialized.compareAndSet(false, true)) {
            logInfo("Hook already initialized");
            return;
        }
        
        logInfo("Initializing App Session & Task Switching Patterns Hook");
        
        try {
            // Load configuration
            loadConfiguration();
            
            // Hook Activity lifecycle
            hookActivityLifecycle(lpparam);
            
            // Hook ActivityManager
            hookActivityManager(lpparam);
            
            // Hook UsageStatsManager if available
            hookUsageStats(lpparam);
            
            logInfo("App Session & Task Switching Hook initialized successfully");
        } catch (Exception e) {
            logError("Failed to initialize App Session & Task Switching Hook", e);
        }
    }
    
    private void loadConfiguration() {
        if (configManager != null) {
            enabled.set(configManager.getHookParamBool(HOOK_ID, KEY_ENABLED, true));
            intensity = configManager.getHookIntensity(HOOK_ID);
            
            microSessionPercent = configManager.getHookParamFloat(HOOK_ID, "micro_session_percent", 0.25f);
            shortSessionPercent = configManager.getHookParamFloat(HOOK_ID, "short_session_percent", 0.30f);
            mediumSessionPercent = configManager.getHookParamFloat(HOOK_ID, "medium_session_percent", 0.30f);
            longSessionPercent = configManager.getHookParamFloat(HOOK_ID, "long_session_percent", 0.15f);
            
            lowSwitchingPercent = configManager.getHookParamFloat(HOOK_ID, "low_switching_percent", 0.25f);
            moderateSwitchingPercent = configManager.getHookParamFloat(HOOK_ID, "moderate_switching_percent", 0.45f);
            highSwitchingPercent = configManager.getHookParamFloat(HOOK_ID, "high_switching_percent", 0.30f);
            
            resumptionProbability = configManager.getHookParamFloat(HOOK_ID, "resumption_probability", 0.68f);
            abandonmentProbability = configManager.getHookParamFloat(HOOK_ID, "abandonment_probability", 0.32f);
        }
        
        logInfo("Configuration loaded: enabled=" + enabled.get() + ", intensity=" + intensity);
    }
    
    private void hookActivityLifecycle(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> activityClass = XposedHelpers.findClass(
                "android.app.Activity", lpparam.classLoader);
            
            // Hook onCreate to track new sessions
            XposedBridge.hookAllMethods(activityClass, "onCreate",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get()) return;
                        
                        try {
                            Activity activity = (Activity) param.thisObject;
                            String packageName = activity.getPackageName();
                            
                            if (packageName != null && !packageName.equals(currentApp.get())) {
                                // New app session starting
                                endCurrentSession();
                                startNewSession(packageName);
                            }
                        } catch (Exception e) {
                            logDebug("Error in onCreate hook: " + e.getMessage());
                        }
                    }
                });
            
            // Hook onResume
            XposedBridge.hookAllMethods(activityClass, "onResume",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get()) return;
                        
                        try {
                            Activity activity = (Activity) param.thisObject;
                            String packageName = activity.getPackageName();
                            
                            // Check for app switch
                            if (!packageName.equals(currentApp.get())) {
                                endCurrentSession();
                                startNewSession(packageName);
                            } else {
                                // App resumed - check for resumption vs new start
                                handleAppResume();
                            }
                        } catch (Exception e) {
                            logDebug("Error in onResume hook: " + e.getMessage());
                        }
                    }
                });
            
            // Hook onPause
            XposedBridge.hookAllMethods(activityClass, "onPause",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get()) return;
                        
                        try {
                            // Track interruption for current session
                            handleInterruption();
                        } catch (Exception e) {
                            logDebug("Error in onPause hook: " + e.getMessage());
                        }
                    }
                });
            
            // Hook onDestroy
            XposedBridge.hookAllMethods(activityClass, "onDestroy",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get()) return;
                        
                        try {
                            // End session when activity destroyed
                            endCurrentSession();
                        } catch (Exception e) {
                            logDebug("Error in onDestroy hook: " + e.getMessage());
                        }
                    }
                });
            
            logInfo("Hooked Activity lifecycle");
        } catch (Exception e) {
            logError("Failed to hook Activity lifecycle", e);
        }
    }
    
    private void hookActivityManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> activityManagerClass = XposedHelpers.findClass(
                "android.app.ActivityManager", lpparam.classLoader);
            
            // Hook getRunningTasks
            XposedBridge.hookAllMethods(activityManagerClass, "getRunningTasks",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get()) return;
                        
                        try {
                            // Track task switches through running tasks
                            // This is a simplified approach
                            updateSwitchingFrequency();
                        } catch (Exception e) {
                            logDebug("Error in getRunningTasks hook: " + e.getMessage());
                        }
                    }
                });
            
            logInfo("Hooked ActivityManager");
        } catch (Exception e) {
            logError("Failed to hook ActivityManager", e);
        }
    }
    
    private void hookUsageStats(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> usageStatsManagerClass = XposedHelpers.findClass(
                "android.app.usage.UsageStatsManager", lpparam.classLoader);
            
            // Hook queryUsageStats
            XposedBridge.hookAllMethods(usageStatsManagerClass, "queryUsageStats",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get()) return;
                        
                        try {
                            // Could add noise to usage stats for realism
                        } catch (Exception e) {
                            logDebug("Error in queryUsageStats hook: " + e.getMessage());
                        }
                    }
                });
            
            logInfo("Hooked UsageStatsManager");
        } catch (Exception e) {
            logDebug("UsageStatsManager not available: " + e.getMessage());
        }
    }
    
    private void startNewSession(String packageName) {
        sessionStartTime = System.currentTimeMillis();
        currentApp.set(packageName);
        lastSwitchTime = sessionStartTime;
        
        // Determine app category
        AppCategory category = appCategories.getOrDefault(packageName, AppCategory.OTHER);
        currentCategory.set(category);
        
        // Determine expected session type based on category
        SessionType sessionType = determineSessionType(category);
        currentSessionType.set(sessionType);
        
        // Increment switch counter
        totalSwitches.incrementAndGet();
        switchesThisHour++;
        
        // Update app stats
        AppSessionStats stats = appStats.get(packageName);
        if (stats == null) {
            stats = new AppSessionStats();
            appStats.put(packageName, stats);
        }
        stats.sessions++;
        
        if (DEBUG && random.get().nextDouble() < 0.01) {
            logDebug("New session: " + packageName + ", category: " + category + 
                ", expected type: " + sessionType);
        }
    }
    
    private void endCurrentSession() {
        if (currentApp.get().isEmpty() || sessionStartTime == 0) {
            return;
        }
        
        long duration = System.currentTimeMillis() - sessionStartTime;
        
        // Categorize session length
        SessionType completedType;
        if (duration < 30000) {
            completedType = SessionType.MICRO;
            microSessions.incrementAndGet();
        } else if (duration < 120000) {
            completedType = SessionType.SHORT;
            shortSessions.incrementAndGet();
        } else if (duration < 300000) {
            completedType = SessionType.MEDIUM;
            mediumSessions.incrementAndGet();
        } else {
            completedType = SessionType.LONG;
            longSessions.incrementAndGet();
        }
        
        // Add to history
        AppSession session = new AppSession(
            currentApp.get(),
            currentCategory.get(),
            sessionStartTime,
            System.currentTimeMillis(),
            completedType
        );
        sessionHistory.add(session);
        while (sessionHistory.size() > MAX_HISTORY) {
            sessionHistory.remove(0);
        }
        
        // Update app stats
        AppSessionStats stats = appStats.get(currentApp.get());
        if (stats != null) {
            stats.totalTime += duration;
            stats.lastUsed = System.currentTimeMillis();
        }
        
        if (DEBUG && random.get().nextDouble() < 0.01) {
            logDebug("Session ended: " + currentApp.get() + 
                ", duration: " + (duration/1000) + "s, type: " + completedType);
        }
    }
    
    private SessionType determineSessionType(AppCategory category) {
        // Different categories have different typical session lengths
        double roll = random.get().nextDouble() * intensity;
        
        switch (category) {
            case COMMUNICATION:
                // Short sessions, high switching
                if (roll < 0.40) return SessionType.MICRO;
                if (roll < 0.70) return SessionType.SHORT;
                if (roll < 0.95) return SessionType.MEDIUM;
                return SessionType.LONG;
                
            case SOCIAL_MEDIA:
                // Longer sessions
                if (roll < 0.15) return SessionType.MICRO;
                if (roll < 0.30) return SessionType.SHORT;
                if (roll < 0.65) return SessionType.MEDIUM;
                return SessionType.LONG;
                
            case PRODUCTIVITY:
                // Medium sessions
                if (roll < 0.20) return SessionType.MICRO;
                if (roll < 0.40) return SessionType.SHORT;
                if (roll < 0.80) return SessionType.MEDIUM;
                return SessionType.LONG;
                
            case ENTERTAINMENT:
                // Long sessions
                if (roll < 0.10) return SessionType.MICRO;
                if (roll < 0.25) return SessionType.SHORT;
                if (roll < 0.55) return SessionType.MEDIUM;
                return SessionType.LONG;
                
            default:
                // General distribution
                if (roll < microSessionPercent) return SessionType.MICRO;
                if (roll < microSessionPercent + shortSessionPercent) return SessionType.SHORT;
                if (roll < microSessionPercent + shortSessionPercent + mediumSessionPercent) 
                    return SessionType.MEDIUM;
                return SessionType.LONG;
        }
    }
    
    private void handleAppResume() {
        // Check if this is a resumption or continuation
        long timeSinceLastInteraction = System.currentTimeMillis() - lastSwitchTime;
        
        // If been away for a while, treat as resumption
        if (timeSinceLastInteraction > 60000) { // 1 minute
            if (random.get().nextDouble() < resumptionProbability * intensity) {
                resumptions.incrementAndGet();
                
                if (DEBUG && random.get().nextDouble() < 0.01) {
                    logDebug("App resumed after " + (timeSinceLastInteraction/1000) + "s");
                }
            } else {
                abandonments.incrementAndGet();
            }
        }
    }
    
    private void handleInterruption() {
        // When app is interrupted (paused), track for potential abandonment
        // This is tracked in onPause - actual abandonment would be determined
        // by whether the user returns or starts a different app
    }
    
    private void updateSwitchingFrequency() {
        // Check if we need to reset hourly counter
        long now = System.currentTimeMillis();
        if (now - hourStartTime > 3600000) { // 1 hour
            hourStartTime = now;
            switchesThisHour = 0;
        }
        
        // Determine switching frequency based on recent activity
        int switches = switchesThisHour;
        if (switches < 3) {
            switchingFrequency.set(SwitchingFrequency.LOW);
        } else if (switches < 10) {
            switchingFrequency.set(SwitchingFrequency.MODERATE);
        } else {
            switchingFrequency.set(SwitchingFrequency.HIGH);
        }
    }
    
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
     * Get session statistics
     */
    public static Map<String, Integer> getSessionStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("micro", microSessions.get());
        stats.put("short", shortSessions.get());
        stats.put("medium", mediumSessions.get());
        stats.put("long", longSessions.get());
        stats.put("total_switches", totalSwitches.get());
        stats.put("resumptions", resumptions.get());
        stats.put("abandonments", abandonments.get());
        return stats;
    }
    
    /**
     * Get current app
     */
    public static String getCurrentApp() {
        return currentApp.get();
    }
    
    /**
     * Get current session type
     */
    public static SessionType getCurrentSessionType() {
        return currentSessionType.get();
    }
    
    /**
     * Get switching frequency
     */
    public static SwitchingFrequency getSwitchingFrequency() {
        return switchingFrequency.get();
    }
    
    /**
     * App session record
     */
    private static class AppSession {
        final String packageName;
        final AppCategory category;
        final long startTime;
        final long endTime;
        final SessionType type;
        
        AppSession(String packageName, AppCategory category, long startTime, 
                  long endTime, SessionType type) {
            this.packageName = packageName;
            this.category = category;
            this.startTime = startTime;
            this.endTime = endTime;
            this.type = type;
        }
    }
    
    /**
     * App session statistics
     */
    private static class AppSessionStats {
        int sessions = 0;
        long totalTime = 0;
        long lastUsed = 0;
    }
}
