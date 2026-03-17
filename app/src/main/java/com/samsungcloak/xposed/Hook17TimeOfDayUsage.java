package com.samsungcloak.xposed;

import android.app.ActivityManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;

import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hook17TimeOfDayUsage - Time-of-Day Usage Pattern Simulation
 * 
 * Simulates realistic usage patterns based on time of day:
 * - Usage bursts: morning (7-9am), work (9am-5pm), evening (6-9pm), night (10pm-6am)
 * - Idle simulation: screen-on idle periods, app abandonment, return patterns
 * - Task complexity modulation by time of day
 * - Input variability: morning (fast/precise) -> evening (slow/error-prone) -> night (ghost touches)
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class Hook17TimeOfDayUsage {

    private static final String TAG = "[TimeOfDay][Hook17]";
    private static final boolean DEBUG = true;
    private static final ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new Random());

    // Configuration
    private static boolean enabled = true;
    private static float intensity = 0.5f;

    // Time period settings
    private static boolean morningBurstEnabled = true;
    private static boolean workHoursEnabled = true;
    private static boolean eveningBurstEnabled = true;
    private static boolean nightModeEnabled = true;

    // Morning settings (7-9am)
    private static double morningBurstProbability = 0.25;
    private static double morningInputSpeed = 1.2;  // Faster in morning
    private static double morningAccuracy = 1.1;    // More accurate

    // Work hours (9am-5pm)
    private static double workBurstProbability = 0.15;
    private static double workBurstMinApps = 3;
    private static double workBurstMaxApps = 8;

    // Evening settings (6-9pm)
    private static double eveningBurstProbability = 0.30;
    private static double eveningInputSpeed = 0.85;  // Slower in evening
    private static double eveningAccuracy = 0.90;    // Less accurate

    // Night settings (10pm-6am)
    private static double nightBurstProbability = 0.08;
    private static double nightInputSpeed = 0.7;   // Much slower
    private static double nightAccuracy = 0.75;    // Error-prone
    private static double ghostTouchProbability = 0.15;

    // Idle settings
    private static boolean idleEnabled = true;
    private static double idleProbability = 0.20;
    private static long idleMinDurationMs = 60000;   // 1 minute
    private static long idleMaxDurationMs = 300000;  // 5 minutes

    // Statistics tracking
    private static final AtomicInteger morningBursts = new AtomicInteger(0);
    private static final AtomicInteger workBursts = new AtomicInteger(0);
    private static final AtomicInteger eveningBursts = new AtomicInteger(0);
    private static final AtomicInteger nightBursts = new AtomicInteger(0);
    private static final AtomicInteger idlePeriods = new AtomicInteger(0);
    private static final AtomicInteger inputErrors = new AtomicInteger(0);
    private static final AtomicLong totalActiveTime = new AtomicLong(0);

    // State tracking
    private static final AtomicReference<TimePeriod> currentPeriod = 
        new AtomicReference<>(TimePeriod.EVENING);
    private static final AtomicReference<UserState> currentUserState = 
        new AtomicReference<>(UserState.ACTIVE);
    private static long sessionStartTime = 0;
    private static long lastActivityTime = 0;

    // Input variability metrics
    private static volatile double currentInputSpeedMultiplier = 1.0;
    private static volatile double currentAccuracyMultiplier = 1.0;

    private static Handler periodCheckHandler;
    private static Runnable periodCheckRunnable;

    public enum TimePeriod {
        NIGHT(22, 6),       // 10pm - 6am
        MORNING(6, 9),      // 6am - 9am
        WORK(9, 17),        // 9am - 5pm
        EVENING(17, 22);    // 5pm - 10pm

        public final int startHour;
        public final int endHour;

        TimePeriod(int start, int end) {
            this.startHour = start;
            this.endHour = end;
        }

        public boolean isInPeriod(int hour) {
            if (startHour > endHour) {
                return hour >= startHour || hour < endHour;
            }
            return hour >= startHour && hour < endHour;
        }
    }

    public enum UserState {
        ACTIVE,
        IDLE,
        RETURNING,
        ABANDONED,
        SLEEPING
    }

    public static class UsageBurst {
        public final TimePeriod period;
        public final long timestamp;
        public final int appCount;
        public final double complexity;
        public final int durationMs;

        public UsageBurst(TimePeriod period, int appCount, double complexity, int durationMs) {
            this.period = period;
            this.timestamp = System.currentTimeMillis();
            this.appCount = appCount;
            this.complexity = complexity;
            this.durationMs = durationMs;
        }
    }

    private static final ConcurrentMap<String, UsageBurst> recentBursts = 
        new ConcurrentHashMap<>();

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!enabled) return;

        HookUtils.logInfo(TAG, "Initializing Time-of-Day Usage Hook 17");

        try {
            sessionStartTime = System.currentTimeMillis();
            lastActivityTime = sessionStartTime;

            updateTimePeriod();
            updateInputVariability();

            hookActivityLifecycle(lpparam);
            hookPowerManager(lpparam);
            hookUsageStatsManager(lpparam);
            hookAlarmManager(lpparam);

            // Start periodic updates
            startPeriodUpdates();

            HookUtils.logInfo(TAG, "Time-of-Day Usage Hook 17 initialized");
            HookUtils.logInfo(TAG, "Current period: " + currentPeriod.get());
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize", e);
        }
    }

    /**
     * Update time period based on current hour
     */
    private static void updateTimePeriod() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);

        for (TimePeriod period : TimePeriod.values()) {
            if (period.isInPeriod(hour)) {
                TimePeriod oldPeriod = currentPeriod.getAndSet(period);
                if (oldPeriod != period) {
                    onTimePeriodChanged(oldPeriod, period);
                }
                break;
            }
        }

        HookUtils.logDebug(TAG, "Time period updated: " + currentPeriod.get());
    }

    /**
     * Handle time period change
     */
    private static void onTimePeriodChanged(TimePeriod oldPeriod, TimePeriod newPeriod) {
        HookUtils.logInfo(TAG, "Time period changed: " + oldPeriod + " -> " + newPeriod);
        
        // Update context in coordinator
        try {
            RealityCoordinator coordinator = RealityCoordinator.getInstance();
            switch (newPeriod) {
                case MORNING:
                    coordinator.updateContextState(RealityCoordinator.ContextState.MORNING_MODE);
                    break;
                case WORK:
                    coordinator.updateContextState(RealityCoordinator.ContextState.WORK_HOURS);
                    break;
                case EVENING:
                    coordinator.updateContextState(RealityCoordinator.ContextState.EVENING_MODE);
                    break;
                case NIGHT:
                    coordinator.updateContextState(RealityCoordinator.ContextState.NIGHT_MODE);
                    break;
            }
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to update coordinator", e);
        }

        updateInputVariability();
    }

    /**
     * Update input variability based on time period
     */
    private static void updateInputVariability() {
        float effectiveIntensity = getEffectiveIntensity();
        
        switch (currentPeriod.get()) {
            case MORNING:
                currentInputSpeedMultiplier = 1.0 + (morningInputSpeed - 1.0) * effectiveIntensity;
                currentAccuracyMultiplier = 1.0 + (morningAccuracy - 1.0) * effectiveIntensity;
                break;
            case WORK:
                currentInputSpeedMultiplier = 1.0;
                currentAccuracyMultiplier = 1.0;
                break;
            case EVENING:
                currentInputSpeedMultiplier = 1.0 + (eveningInputSpeed - 1.0) * effectiveIntensity;
                currentAccuracyMultiplier = 1.0 + (eveningAccuracy - 1.0) * effectiveIntensity;
                break;
            case NIGHT:
                currentInputSpeedMultiplier = 1.0 + (nightInputSpeed - 1.0) * effectiveIntensity;
                currentAccuracyMultiplier = 1.0 + (nightAccuracy - 1.0) * effectiveIntensity;
                break;
        }

        HookUtils.logDebug(TAG, String.format("Input variability: speed=%.2f, accuracy=%.2f",
            currentInputSpeedMultiplier, currentAccuracyMultiplier));
    }

    /**
     * Start periodic time period checks
     */
    private static void startPeriodUpdates() {
        periodCheckHandler = new Handler(Looper.getMainLooper());
        periodCheckRunnable = new Runnable() {
            @Override
            public void run() {
                updateTimePeriod();
                checkForUsageBurst();
                checkForIdleState();
                periodCheckHandler.postDelayed(this, 30000); // Check every 30 seconds
            }
        };
        periodCheckHandler.post(periodCheckRunnable);
    }

    /**
     * Check for usage burst based on time period
     */
    private static void checkForUsageBurst() {
        float effectiveIntensity = getEffectiveIntensity();
        TimePeriod period = currentPeriod.get();
        
        double burstProbability;
        int minApps, maxApps;
        AtomicInteger burstCounter;

        switch (period) {
            case MORNING:
                burstProbability = morningBurstProbability;
                minApps = 2;
                maxApps = 5;
                burstCounter = morningBursts;
                break;
            case WORK:
                burstProbability = workBurstProbability;
                minApps = (int) workBurstMinApps;
                maxApps = (int) workBurstMaxApps;
                burstCounter = workBursts;
                break;
            case EVENING:
                burstProbability = eveningBurstProbability;
                minApps = 2;
                maxApps = 6;
                burstCounter = eveningBursts;
                break;
            case NIGHT:
                burstProbability = nightBurstProbability;
                minApps = 1;
                maxApps = 3;
                burstCounter = nightBursts;
                break;
            default:
                return;
        }

        if (random.get().nextDouble() < burstProbability * effectiveIntensity) {
            int appCount = random.get().nextInt(maxApps - minApps + 1) + minApps;
            double complexity = calculateComplexity(period);
            int duration = (int) (appCount * 5000 + random.get().nextInt(10000));

            UsageBurst burst = new UsageBurst(period, appCount, complexity, duration);
            String burstId = System.currentTimeMillis() + "_" + random.get().nextInt(1000);
            recentBursts.put(burstId, burst);
            
            burstCounter.incrementAndGet();
            totalActiveTime.addAndGet(duration);

            HookUtils.logInfo(TAG, String.format("Usage burst: period=%s, apps=%d, complexity=%.2f, duration=%dms",
                period, appCount, complexity, duration));
        }
    }

    /**
     * Calculate task complexity based on time period
     */
    private static double calculateComplexity(TimePeriod period) {
        double baseComplexity;
        
        switch (period) {
            case MORNING:
                baseComplexity = 0.7; // Fresh, simpler tasks
                break;
            case WORK:
                baseComplexity = 1.0; // Normal complexity
                break;
            case EVENING:
                baseComplexity = 0.9; // Some fatigue
                break;
            case NIGHT:
                baseComplexity = 0.6; // Simple tasks only
                break;
            default:
                baseComplexity = 0.8;
        }

        // Add some randomness
        return baseComplexity + (random.get().nextDouble() * 0.2 - 0.1);
    }

    /**
     * Check for idle state
     */
    private static void checkForIdleState() {
        if (!idleEnabled) return;

        float effectiveIntensity = getEffectiveIntensity();
        
        long timeSinceActivity = System.currentTimeMillis() - lastActivityTime;
        
        // Only check idle if there's been some activity
        if (timeSinceActivity < 60000) return;

        if (random.get().nextDouble() < idleProbability * effectiveIntensity) {
            long idleDuration = idleMinDurationMs + 
                random.get().nextInt((int) (idleMaxDurationMs - idleMinDurationMs));
            
            idlePeriods.incrementAndGet();
            currentUserState.set(UserState.IDLE);

            HookUtils.logInfo(TAG, "Idle period: " + idleDuration + "ms");

            // Simulate return from idle
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                currentUserState.set(UserState.RETURNING);
                
                // Determine if user returns or abandons
                if (random.get().nextDouble() < 0.7) {
                    currentUserState.set(UserState.ACTIVE);
                    lastActivityTime = System.currentTimeMillis();
                    HookUtils.logDebug(TAG, "User returned from idle");
                } else {
                    currentUserState.set(UserState.ABANDONED);
                    HookUtils.logDebug(TAG, "User abandoned session");
                }
            }, idleDuration);
        }
    }

    /**
     * Hook Activity lifecycle
     */
    private static void hookActivityLifecycle(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> activityClass = XposedHelpers.findClass(
                "android.app.Activity", lpparam.classLoader);

            // Hook onResume
            XposedBridge.hookMethod(
                XposedHelpers.findMethodExact(activityClass, "onResume"),
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        lastActivityTime = System.currentTimeMillis();
                        
                        if (currentUserState.get() == UserState.IDLE) {
                            currentUserState.set(UserState.RETURNING);
                        } else {
                            currentUserState.set(UserState.ACTIVE);
                        }
                    }
                });

            // Hook onPause
            XposedBridge.hookMethod(
                XposedHelpers.findMethodExact(activityClass, "onPause"),
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        lastActivityTime = System.currentTimeMillis();
                    }
                });

            HookUtils.logDebug(TAG, "Activity lifecycle hooked");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook Activity lifecycle", e);
        }
    }

    /**
     * Hook PowerManager
     */
    private static void hookPowerManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> powerManagerClass = XposedHelpers.findClass(
                "android.os.PowerManager", lpparam.classLoader);

            // Hook isScreenOn
            XposedBridge.hookMethod(
                XposedHelpers.findMethodExact(powerManagerClass, "isScreenOn"),
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        boolean isOn = (boolean) param.getResult();
                        
                        if (isOn) {
                            // Apply night time ghost touches
                            if (currentPeriod.get() == TimePeriod.NIGHT && 
                                random.get().nextDouble() < ghostTouchProbability * getEffectiveIntensity()) {
                                inputErrors.incrementAndGet();
                                HookUtils.logDebug(TAG, "Night time ghost touch simulated");
                            }
                        }
                    }
                });

            HookUtils.logDebug(TAG, "PowerManager hooked");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook PowerManager", e);
        }
    }

    /**
     * Hook UsageStatsManager
     */
    private static void hookUsageStatsManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> usageStatsManagerClass = XposedHelpers.findClass(
                "android.app.usage.UsageStatsManager", lpparam.classLoader);

            // Hook queryUsageStats
            XposedBridge.hookMethod(
                XposedHelpers.findMethodExact(usageStatsManagerClass, "queryUsageStats",
                    int.class, long.class, long.class),
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        // Could modify usage stats to reflect time-of-day patterns
                    }
                });

            HookUtils.logDebug(TAG, "UsageStatsManager hooked");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook UsageStatsManager", e);
        }
    }

    /**
     * Hook AlarmManager
     */
    private static void hookAlarmManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> alarmManagerClass = XposedHelpers.findClass(
                "android.app.AlarmManager", lpparam.classLoader);

            // Hook setExact
            XposedBridge.hookMethod(
                XposedHelpers.findMethodExact(alarmManagerClass, "setExact",
                    int.class, long.class, PendingIntent.class),
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        long triggerTime = (long) param.args[1];
                        Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis(triggerTime);
                        int hour = cal.get(Calendar.HOUR_OF_DAY);

                        // Add slight variability to alarm timing
                        int variance = random.get().nextInt(60000); // +/- 30 seconds
                        param.args[1] = triggerTime + variance;
                        
                        HookUtils.logDebug(TAG, "Alarm set with variance: " + variance + "ms");
                    }
                });

            HookUtils.logDebug(TAG, "AlarmManager hooked");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook AlarmManager", e);
        }
    }

    /**
     * Get effective intensity
     */
    private static float getEffectiveIntensity() {
        ConfigurationManager config = BaseRealismHook.configManager;
        float configIntensity = config != null ? config.getHookIntensity("hook_17") : intensity;
        return intensity * configIntensity;
    }

    /**
     * Get current input speed multiplier
     */
    public static double getInputSpeedMultiplier() {
        return currentInputSpeedMultiplier;
    }

    /**
     * Get current accuracy multiplier
     */
    public static double getAccuracyMultiplier() {
        return currentAccuracyMultiplier;
    }

    /**
     * Get time-of-day statistics
     */
    public static String getStats() {
        return String.format("TimeOfDay[period=%s, state=%s, morning=%d, work=%d, evening=%d, night=%d, idle=%d, errors=%d]",
            currentPeriod.get(), currentUserState.get(),
            morningBursts.get(), workBursts.get(), eveningBursts.get(), 
            nightBursts.get(), idlePeriods.get(), inputErrors.get());
    }

    // Required Android class
    private static class PendingIntent {}
}
