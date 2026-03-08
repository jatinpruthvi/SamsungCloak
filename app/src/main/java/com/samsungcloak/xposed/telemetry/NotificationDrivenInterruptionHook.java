package com.samsungcloak.xposed.telemetry;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Notification-Driven Interruption Hook
 * 
 * Simulates asynchronous interrupts (Push Notifications, System Alerts) that
 * trigger a 3-5 second context-switch where the agent pauses or briefly
 * background-tasks before resuming its primary flow.
 * 
 * Interruption types modeled:
 * - Push notifications (social media, messaging, email)
 * - System alerts (battery, storage, security)
 * - Calendar reminders
 * - App update notifications
 * - Incoming calls (simulated)
 * 
 * Target Device: Samsung Galaxy A12 (SM-A125U)
 */
public class NotificationDrivenInterruptionHook {
    
    private static final String LOG_TAG = "SamsungCloak.NotificationInterrupt";
    private static boolean initialized = false;
    
    private static final Random random = new Random();
    private static final int MIN_CONTEXT_SWITCH_MS = 3000;
    private static final int MAX_CONTEXT_SWITCH_MS = 5000;
    private static final int DEFAULT_CHECK_INTERVAL_MS = 15000;
    
    private static volatile boolean interruptionsEnabled = true;
    private static volatile float interruptionProbability = 0.25f;
    private static volatile int maxConcurrentInterruptions = 3;
    
    // State tracking
    private static final AtomicBoolean isInterrupted = new AtomicBoolean(false);
    private static final AtomicReference<InterruptContext> currentInterrupt = new AtomicReference<>();
    private static final AtomicInteger activeInterruptCount = new AtomicInteger(0);
    private static final AtomicLong totalInterruptions = new AtomicLong(0);
    private static final AtomicLong totalContextSwitchTimeMs = new AtomicLong(0);
    private static final AtomicLong lastInterruptTime = new AtomicLong(0);
    
    // Thread pool for background processing
    private static ExecutorService interruptExecutor;
    private static ScheduledExecutorService scheduler;
    
    // Callback for interruption events
    private static Consumer<InterruptContext> interruptCallback;
    
    // Statistics
    private static int pushNotificationCount = 0;
    private static int systemAlertCount = 0;
    private static int reminderCount = 0;
    private static int callInterruptionCount = 0;
    
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (initialized) {
            XposedBridge.log(LOG_TAG + " already initialized");
            return;
        }
        
        try {
            initializeThreadPool();
            hookNotificationManager(lpparam);
            hookActivityManager(lpparam);
            hookAlarmManager(lpparam);
            
            initialized = true;
            XposedBridge.log(LOG_TAG + " initialized successfully");
            XposedBridge.log(LOG_TAG + " Interruption probability: " + (interruptionProbability * 100) + "%");
            
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " Failed to initialize: " + e.getMessage());
        }
    }
    
    private static void initializeThreadPool() {
        interruptExecutor = Executors.newFixedThreadPool(2, new ThreadFactory() {
            private int count = 0;
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "NotificationInterrupt-" + count++);
                t.setDaemon(true);
                return t;
            }
        });
        
        scheduler = Executors.newScheduledThreadPool(1, new ThreadFactory() {
            private int count = 0;
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "InterruptScheduler-" + count++);
                t.setDaemon(true);
                return t;
            }
        });
    }
    
    private static void hookNotificationManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> notificationManagerClass = XposedHelpers.findClass(
                "android.app.NotificationManager", lpparam.classLoader
            );
            
            XposedBridge.hookAllMethods(notificationManagerClass, "notify", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (interruptionsEnabled && !isInterrupted.get()) {
                        handleNotificationIntercept(param);
                    }
                }
            });
            
            XposedBridge.hookAllMethods(notificationManagerClass, "createNotificationChannel",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        // Allow channel creation
                    }
                });
            
            XposedBridge.log(LOG_TAG + " Hooked NotificationManager");
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " Failed to hook NotificationManager: " + e.getMessage());
        }
    }
    
    private static void hookActivityManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> activityManagerClass = XposedHelpers.findClass(
                "android.app.ActivityManager", lpparam.classLoader
            );
            
            XposedBridge.hookAllMethods(activityManagerClass, "getRunningTasks", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    // Simulate background tasks running
                }
            });
            
            XposedBridge.log(LOG_TAG + " Hooked ActivityManager");
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " Failed to hook ActivityManager: " + e.getMessage());
        }
    }
    
    private static void hookAlarmManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> alarmManagerClass = XposedHelpers.findClass(
                "android.app.AlarmManager", lpparam.classLoader
            );
            
            XposedBridge.hookAllMethods(alarmManagerClass, "set", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    // Allow alarm setting but track for potential interruptions
                }
            });
            
            XposedBridge.log(LOG_TAG + " Hooked AlarmManager");
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " Failed to hook AlarmManager: " + e.getMessage());
        }
    }
    
    /**
     * Handle intercepted notification and potentially trigger interruption
     */
    private static void handleNotificationIntercept(XC_MethodHook.MethodHookParam param) {
        if (random.nextFloat() > interruptionProbability) {
            return;
        }
        
        if (activeInterruptCount.get() >= maxConcurrentInterruptions) {
            return;
        }
        
        // Determine interruption type
        InterruptionType type = selectInterruptionType();
        InterruptContext context = new InterruptContext(
            type,
            generateInterruptionApp(type),
            calculateContextSwitchDuration(type),
            System.currentTimeMillis()
        );
        
        // Trigger the interruption
        triggerInterruption(context);
    }
    
    /**
     * Select a random interruption type with realistic probability distribution
     */
    private static InterruptionType selectInterruptionType() {
        float roll = random.nextFloat();
        
        if (roll < 0.40f) {
            pushNotificationCount++;
            return InterruptionType.PUSH_NOTIFICATION;
        } else if (roll < 0.60f) {
            systemAlertCount++;
            return InterruptionType.SYSTEM_ALERT;
        } else if (roll < 0.75f) {
            reminderCount++;
            return InterruptionType.CALENDAR_REMINDER;
        } else if (roll < 0.85f) {
            callInterruptionCount++;
            return InterruptionType.INCOMING_CALL;
        } else if (roll < 0.93f) {
            return InterruptionType.APP_UPDATE;
        } else {
            return InterruptionType.BATTERY_ALERT;
        }
    }
    
    /**
     * Generate realistic app package for the interruption
     */
    private static String generateInterruptionApp(InterruptionType type) {
        switch (type) {
            case PUSH_NOTIFICATION:
                String[] socialApps = {
                    "com.facebook.katana", "com.instagram.android",
                    "com.zhiliaoapp.musically", "com.twitter.android",
                    "com.whatsapp", "org.telegram.messenger",
                    "com.google.android.apps.messages"
                };
                return socialApps[random.nextInt(socialApps.length)];
                
            case SYSTEM_ALERT:
                String[] systemApps = {
                    "com.android.systemui",
                    "com.samsung.android.lool",
                    "com.samsung.android.sm.devicesecurity"
                };
                return systemApps[random.nextInt(systemApps.length)];
                
            case CALENDAR_REMINDER:
                return "com.google.android.calendar";
                
            case INCOMING_CALL:
                return "com.android.phone";
                
            case APP_UPDATE:
                return "com.android.vending";
                
            case BATTERY_ALERT:
                return "android";
                
            default:
                return "com.example.notification";
        }
    }
    
    /**
     * Calculate context switch duration based on interruption type
     */
    private static int calculateContextSwitchDuration(InterruptionType type) {
        int baseDuration;
        
        switch (type) {
            case PUSH_NOTIFICATION:
                baseDuration = 3000 + random.nextInt(3000); // 3-6 seconds
                break;
            case SYSTEM_ALERT:
                baseDuration = 2000 + random.nextInt(2000); // 2-4 seconds
                break;
            case CALENDAR_REMINDER:
                baseDuration = 2500 + random.nextInt(2500); // 2.5-5 seconds
                break;
            case INCOMING_CALL:
                baseDuration = 4000 + random.nextInt(4000); // 4-8 seconds
                break;
            case APP_UPDATE:
                baseDuration = 1500 + random.nextInt(1500); // 1.5-3 seconds
                break;
            case BATTERY_ALERT:
                baseDuration = 1000 + random.nextInt(2000); // 1-3 seconds
                break;
            default:
                baseDuration = MIN_CONTEXT_SWITCH_MS + random.nextInt(MAX_CONTEXT_SWITCH_MS - MIN_CONTEXT_SWITCH_MS);
        }
        
        return baseDuration;
    }
    
    /**
     * Trigger the interruption and perform context switch
     */
    public static void triggerInterruption(InterruptContext context) {
        if (isInterrupted.getAndSet(true)) {
            return; // Already interrupted
        }
        
        activeInterruptCount.incrementAndGet();
        currentInterrupt.set(context);
        totalInterruptions.incrementAndGet();
        lastInterruptTime.set(System.currentTimeMillis());
        
        XposedBridge.log(LOG_TAG + " Interruption triggered: " + context.getType() + 
            " from " + context.getSourceApp() + " (" + context.getDurationMs() + "ms)");
        
        // Execute callback if registered
        if (interruptCallback != null) {
            try {
                interruptCallback.accept(context);
            } catch (Exception e) {
                XposedBridge.log(LOG_TAG + " Callback error: " + e.getMessage());
            }
        }
        
        // Perform context switch in background
        interruptExecutor.submit(() -> {
            try {
                performContextSwitch(context);
            } finally {
                completeInterruption(context);
            }
        });
    }
    
    /**
     * Simulate the context-switch delay where agent pauses
     */
    private static void performContextSwitch(InterruptContext context) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Simulate processing delay with occasional jitter
            int totalDelay = context.getDurationMs();
            int segmentSize = 500;
            
            for (int elapsed = 0; elapsed < totalDelay; elapsed += segmentSize) {
                // Simulate background processing
                Thread.sleep(segmentSize);
                
                // Occasional additional delay (user interacting with notification)
                if (random.nextFloat() < 0.15f) {
                    Thread.sleep(random.nextInt(500));
                }
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        long actualDuration = System.currentTimeMillis() - startTime;
        totalContextSwitchTimeMs.addAndGet(actualDuration);
    }
    
    /**
     * Complete the interruption and resume normal operation
     */
    private static void completeInterruption(InterruptContext context) {
        isInterrupted.set(false);
        activeInterruptCount.decrementAndGet();
        currentInterrupt.set(null);
        
        XposedBridge.log(LOG_TAG + " Interruption completed: " + context.getType() + 
            " (" + (System.currentTimeMillis() - context.getStartTime()) + "ms total)");
    }
    
    /**
     * Set callback for interruption events
     */
    public static void setInterruptCallback(Consumer<InterruptContext> callback) {
        interruptCallback = callback;
    }
    
    /**
     * Enable or disable interruptions
     */
    public static void setInterruptionsEnabled(boolean enabled) {
        interruptionsEnabled = enabled;
    }
    
    /**
     * Set interruption probability (0.0 - 1.0)
     */
    public static void setInterruptionProbability(float probability) {
        interruptionProbability = Math.max(0.0f, Math.min(1.0f, probability));
    }
    
    /**
     * Check if currently interrupted
     */
    public static boolean isInterrupted() {
        return isInterrupted.get();
    }
    
    /**
     * Get current interrupt context
     */
    public static InterruptContext getCurrentInterrupt() {
        return currentInterrupt.get();
    }
    
    /**
     * Get active interrupt count
     */
    public static int getActiveInterruptCount() {
        return activeInterruptCount.get();
    }
    
    /**
     * Get total interruption count
     */
    public static long getTotalInterruptions() {
        return totalInterruptions.get();
    }
    
    /**
     * Get total context switch time
     */
    public static long getTotalContextSwitchTimeMs() {
        return totalContextSwitchTimeMs.get();
    }
    
    /**
     * Get time since last interruption
     */
    public static long getTimeSinceLastInterruptMs() {
        return System.currentTimeMillis() - lastInterruptTime.get();
    }
    
    /**
     * Get interruption statistics
     */
    public static InterruptionStatistics getStatistics() {
        return new InterruptionStatistics(
            totalInterruptions.get(),
            pushNotificationCount,
            systemAlertCount,
            reminderCount,
            callInterruptionCount,
            totalContextSwitchTimeMs.get(),
            isInterrupted.get(),
            getTimeSinceLastInterruptMs()
        );
    }
    
    /**
     * Reset statistics
     */
    public static void resetStatistics() {
        totalInterruptions.set(0);
        totalContextSwitchTimeMs.set(0);
        lastInterruptTime.set(System.currentTimeMillis());
        pushNotificationCount = 0;
        systemAlertCount = 0;
        reminderCount = 0;
        callInterruptionCount = 0;
    }
    
    public static boolean isInitialized() {
        return initialized;
    }
    
    public enum InterruptionType {
        PUSH_NOTIFICATION("Push Notification"),
        SYSTEM_ALERT("System Alert"),
        CALENDAR_REMINDER("Calendar Reminder"),
        INCOMING_CALL("Incoming Call"),
        APP_UPDATE("App Update"),
        BATTERY_ALERT("Battery Alert");
        
        private final String displayName;
        
        InterruptionType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public static class InterruptContext {
        private final InterruptionType type;
        private final String sourceApp;
        private final int durationMs;
        private final long startTime;
        
        public InterruptContext(InterruptionType type, String sourceApp, 
                               int durationMs, long startTime) {
            this.type = type;
            this.sourceApp = sourceApp;
            this.durationMs = durationMs;
            this.startTime = startTime;
        }
        
        public InterruptionType getType() {
            return type;
        }
        
        public String getSourceApp() {
            return sourceApp;
        }
        
        public int getDurationMs() {
            return durationMs;
        }
        
        public long getStartTime() {
            return startTime;
        }
        
        public long getElapsedMs() {
            return System.currentTimeMillis() - startTime;
        }
        
        @Override
        public String toString() {
            return String.format("InterruptContext{type=%s, app=%s, duration=%dms, elapsed=%dms}",
                type.getDisplayName(), sourceApp, durationMs, getElapsedMs());
        }
    }
    
    public static class InterruptionStatistics {
        private final long totalInterruptions;
        private final int pushNotificationCount;
        private final int systemAlertCount;
        private final int reminderCount;
        private final int callInterruptionCount;
        private final long totalContextSwitchTimeMs;
        private final boolean currentlyInterrupted;
        private final long timeSinceLastInterruptMs;
        
        public InterruptionStatistics(long totalInterruptions, int pushNotificationCount,
                                     int systemAlertCount, int reminderCount,
                                     int callInterruptionCount, long totalContextSwitchTimeMs,
                                     boolean currentlyInterrupted, long timeSinceLastInterruptMs) {
            this.totalInterruptions = totalInterruptions;
            this.pushNotificationCount = pushNotificationCount;
            this.systemAlertCount = systemAlertCount;
            this.reminderCount = reminderCount;
            this.callInterruptionCount = callInterruptionCount;
            this.totalContextSwitchTimeMs = totalContextSwitchTimeMs;
            this.currentlyInterrupted = currentlyInterrupted;
            this.timeSinceLastInterruptMs = timeSinceLastInterruptMs;
        }
        
        public long getTotalInterruptions() { return totalInterruptions; }
        public int getPushNotificationCount() { return pushNotificationCount; }
        public int getSystemAlertCount() { return systemAlertCount; }
        public int getReminderCount() { return reminderCount; }
        public int getCallInterruptionCount() { return callInterruptionCount; }
        public long getTotalContextSwitchTimeMs() { return totalContextSwitchTimeMs; }
        public boolean isCurrentlyInterrupted() { return currentlyInterrupted; }
        public long getTimeSinceLastInterruptMs() { return timeSinceLastInterruptMs; }
        
        public float getAverageContextSwitchTimeMs() {
            return totalInterruptions > 0 ? 
                (float) totalContextSwitchTimeMs / totalInterruptions : 0;
        }
        
        @Override
        public String toString() {
            return String.format("InterruptionStatistics{total=%d, push=%d, alerts=%d, " +
                "reminders=%d, calls=%d, avgSwitchTime=%.1fms, current=%s}",
                totalInterruptions, pushNotificationCount, systemAlertCount,
                reminderCount, callInterruptionCount, getAverageContextSwitchTimeMs(),
                currentlyInterrupted);
        }
    }
}
