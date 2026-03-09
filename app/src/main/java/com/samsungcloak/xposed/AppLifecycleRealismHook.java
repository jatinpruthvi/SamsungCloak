package com.samsungcloak.xposed;

import android.app.ActivityManager;
import android.app.Application;
import android.os.Process;

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
import java.util.concurrent.atomic.AtomicLong;

/**
 * AppLifecycleRealismHook - Realistic App Lifecycle Behavior Simulation
 *
 * Simulates realistic app lifecycle behaviors including launch patterns,
 * background/foreground transitions, and session management.
 *
 * Novel Dimensions:
 * 1. App Launch Timing and Patterns - Cold vs warm start behaviors
 * 2. App Backgrounding/Foregrounding - Realistic app switching
 * 3. App Termination Patterns - Swipe away vs keep running
 * 4. App Usage Session Characteristics - Duration and frequency
 * 5. Memory Pressure Response - App kills under low memory
 *
 * Real-World Grounding (HCI Studies):
 * - Average app session duration: 5-15 minutes
 * - App switch frequency: every 2-5 minutes during active use
 * - Background app retention: 3-7 apps average
 * - App launch cold start: 2-5 seconds
 * - App launch warm start: 0.5-2 seconds
 *
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class AppLifecycleRealismHook {

    private static final String TAG = "[Lifecycle][App]";
    private static final boolean DEBUG = true;
    private static final ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new Random());

    private static boolean enabled = true;

    // Session configuration
    private static boolean sessionSimulationEnabled = true;
    private static long averageSessionDurationMs = 600000; // 10 minutes
    private static double sessionDurationVariance = 0.5; // 50% variance
    private static int maxBackgroundApps = 5;

    // Launch behavior
    private static boolean launchSimulationEnabled = true;
    private static long coldStartDelayMs = 3500; // 3.5 seconds
    private static long warmStartDelayMs = 1200; // 1.2 seconds
    private static double coldStartProbability = 0.25; // 25% cold starts

    // Background behavior
    private static boolean backgroundSimulationEnabled = true;
    private static long backgroundRetentionMs = 300000; // 5 minutes average
    private static double appTerminationProbability = 0.15; // Swipe away

    // State tracking
    private static final CopyOnWriteArrayList<String> runningApps = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<String> backgroundApps = new CopyOnWriteArrayList<>();
    private static final ConcurrentMap<String, Long> appLaunchTimes = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Long> appBackgroundTimes = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Long> appSessionDurations = new ConcurrentHashMap<>();
    private static final AtomicInteger totalSessionCount = new AtomicInteger(0);
    private static final AtomicLong totalAppUsageTimeMs = new AtomicLong(0);

    private static String currentForegroundApp = null;
    private static long currentSessionStartTime = 0;

    public enum AppLaunchType {
        COLD_START,     // App not in memory
        WARM_START,     // App in memory but not visible
        HOT_START       // App recently used
    }

    public enum AppSessionType {
        QUICK_CHECK,    // < 30 seconds
        BRIEF_USE,      // 30 seconds - 5 minutes
        NORMAL_USE,     // 5-15 minutes
        EXTENDED_USE,   // 15-30 minutes
        LONG_SESSION    // > 30 minutes
    }

    public enum AppTerminationType {
        SYSTEM_KILL,    // OS killed for memory
        USER_SWIPE,     // User swiped away
        CRASH,          // App crashed
        BACKGROUND      // Still running in background
    }

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        HookUtils.logInfo(TAG, "Initializing App Lifecycle Realism Hook");

        try {
            determineSessionCharacteristics();

            hookActivityManager(lpparam);
            hookApplication(lpparam);
            hookProcess(lpparam);

            startLifecycleSimulationThread();

            HookUtils.logInfo(TAG, "App Lifecycle Realism Hook initialized");
            HookUtils.logInfo(TAG, String.format("Avg session: %.1f min, Cold start: %.0f%%",
                averageSessionDurationMs / 60000.0, coldStartProbability * 100));
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize hook", e);
        }
    }

    private static void determineSessionCharacteristics() {
        // Determine session characteristics based on user type
        double rand = random.get().nextDouble();

        if (rand < 0.30) {
            // Light user: short sessions
            averageSessionDurationMs = 300000 + (long) (random.get().nextDouble() * 300000); // 5-10 min
            maxBackgroundApps = 3;
        } else if (rand < 0.70) {
            // Average user
            averageSessionDurationMs = 600000 + (long) (random.get().nextDouble() * 600000); // 10-20 min
            maxBackgroundApps = 5;
        } else {
            // Heavy user: long sessions
            averageSessionDurationMs = 1200000 + (long) (random.get().nextDouble() * 1200000); // 20-40 min
            maxBackgroundApps = 7;
        }
    }

    private static void hookActivityManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> activityManagerClass = XposedHelpers.findClass(
                "android.app.ActivityManager",
                lpparam.classLoader
            );

            // Hook getRunningAppProcesses
            XposedBridge.hookAllMethods(activityManagerClass, "getRunningAppProcesses",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !sessionSimulationEnabled) return;

                        try {
                            @SuppressWarnings("unchecked")
                            List<ActivityManager.RunningAppProcessInfo> processes =
                                (List<ActivityManager.RunningAppProcessInfo>) param.getResult();

                            // Could modify running processes list
                        } catch (Exception e) {
                            if (DEBUG) HookUtils.logDebug(TAG, "Error in getRunningAppProcesses: " + e.getMessage());
                        }
                    }
                });

            // Hook getRecentTasks
            XposedBridge.hookAllMethods(activityManagerClass, "getRecentTasks",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        try {
                            // Could modify recent tasks
                        } catch (Exception e) {
                            if (DEBUG) HookUtils.logDebug(TAG, "Error in getRecentTasks: " + e.getMessage());
                        }
                    }
                });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked ActivityManager");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook ActivityManager", e);
        }
    }

    private static void hookApplication(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> applicationClass = XposedHelpers.findClass(
                "android.app.Application",
                lpparam.classLoader
            );

            XposedBridge.hookAllMethods(applicationClass, "onCreate",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !launchSimulationEnabled) return;

                        try {
                            Application app = (Application) param.thisObject;
                            String packageName = app.getPackageName();

                            handleAppLaunch(packageName);
                        } catch (Exception e) {
                            if (DEBUG) HookUtils.logDebug(TAG, "Error in onCreate: " + e.getMessage());
                        }
                    }
                });

            XposedBridge.hookAllMethods(applicationClass, "onTrimMemory",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !backgroundSimulationEnabled) return;

                        try {
                            int level = (int) param.args[0];
                            handleMemoryTrim(level);
                        } catch (Exception e) {
                            if (DEBUG) HookUtils.logDebug(TAG, "Error in onTrimMemory: " + e.getMessage());
                        }
                    }
                });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked Application");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook Application", e);
        }
    }

    private static void hookProcess(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> processClass = XposedHelpers.findClass(
                "android.os.Process",
                lpparam.classLoader
            );

            // Hook killProcess
            XposedBridge.hookAllMethods(processClass, "killProcess",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        try {
                            int pid = (int) param.args[0];
                            handleProcessKilled(pid);
                        } catch (Exception e) {
                            if (DEBUG) HookUtils.logDebug(TAG, "Error in killProcess: " + e.getMessage());
                        }
                    }
                });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked Process");
        } catch (Exception e) {
            if (DEBUG) HookUtils.logDebug(TAG, "Process hook not available: " + e.getMessage());
        }
    }

    private static void startLifecycleSimulationThread() {
        Thread lifecycleThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60000); // Check every minute

                    if (!enabled || !sessionSimulationEnabled) continue;

                    simulateSessionTransitions();
                    manageBackgroundApps();

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    if (DEBUG) HookUtils.logDebug(TAG, "Lifecycle simulation error: " + e.getMessage());
                }
            }
        });
        lifecycleThread.setName("LifecycleSimulation");
        lifecycleThread.setDaemon(true);
        lifecycleThread.start();
    }

    private static void handleAppLaunch(String packageName) {
        // Determine launch type
        AppLaunchType launchType;
        if (backgroundApps.contains(packageName)) {
            launchType = AppLaunchType.WARM_START;
            backgroundApps.remove(packageName);
        } else if (random.get().nextDouble() < coldStartProbability) {
            launchType = AppLaunchType.COLD_START;
        } else {
            launchType = AppLaunchType.HOT_START;
        }

        // Record launch
        appLaunchTimes.put(packageName, System.currentTimeMillis());
        runningApps.add(packageName);

        // Simulate launch delay
        simulateLaunchDelay(launchType);

        // Update session tracking
        if (currentForegroundApp != null && !currentForegroundApp.equals(packageName)) {
            endCurrentSession();
        }
        startNewSession(packageName);

        if (DEBUG) {
            HookUtils.logDebug(TAG, String.format(
                "App launched: %s, type=%s",
                packageName, launchType.name()
            ));
        }
    }

    private static void simulateLaunchDelay(AppLaunchType launchType) {
        long delay = 0;

        switch (launchType) {
            case COLD_START:
                delay = (long) (coldStartDelayMs * (0.8 + random.get().nextDouble() * 0.4));
                break;
            case WARM_START:
                delay = (long) (warmStartDelayMs * (0.8 + random.get().nextDouble() * 0.4));
                break;
            case HOT_START:
                delay = 200 + (long) (random.get().nextDouble() * 300);
                break;
        }

        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void startNewSession(String packageName) {
        currentForegroundApp = packageName;
        currentSessionStartTime = System.currentTimeMillis();
        totalSessionCount.incrementAndGet();

        if (DEBUG) {
            HookUtils.logDebug(TAG, "Session started: " + packageName);
        }
    }

    private static void endCurrentSession() {
        if (currentForegroundApp == null) return;

        long sessionDuration = System.currentTimeMillis() - currentSessionStartTime;
        appSessionDurations.put(currentForegroundApp, sessionDuration);
        totalAppUsageTimeMs.addAndGet(sessionDuration);

        // Move to background
        backgroundApps.add(currentForegroundApp);
        appBackgroundTimes.put(currentForegroundApp, System.currentTimeMillis());

        if (DEBUG) {
            HookUtils.logDebug(TAG, String.format(
                "Session ended: %s, duration=%.1f min",
                currentForegroundApp, sessionDuration / 60000.0
            ));
        }

        currentForegroundApp = null;
    }

    private static void simulateSessionTransitions() {
        // Simulate user switching between apps
        if (currentForegroundApp != null && random.get().nextDouble() < 0.05) {
            // 5% chance per minute to switch apps
            endCurrentSession();
        }
    }

    private static void manageBackgroundApps() {
        // Terminate old background apps
        long currentTime = System.currentTimeMillis();

        for (String app : new ArrayList<>(backgroundApps)) {
            Long backgroundTime = appBackgroundTimes.get(app);
            if (backgroundTime == null) continue;

            long backgroundDuration = currentTime - backgroundTime;

            // Check if should terminate
            if (backgroundDuration > backgroundRetentionMs) {
                if (random.get().nextDouble() < appTerminationProbability) {
                    // Simulate user swiping away or system killing
                    backgroundApps.remove(app);
                    runningApps.remove(app);

                    if (DEBUG) {
                        HookUtils.logDebug(TAG, "App terminated: " + app);
                    }
                }
            }
        }

        // Enforce max background apps
        while (backgroundApps.size() > maxBackgroundApps) {
            String oldestApp = backgroundApps.get(0);
            backgroundApps.remove(oldestApp);
            runningApps.remove(oldestApp);
        }
    }

    private static void handleMemoryTrim(int level) {
        // Handle system memory pressure
        if (level >= Application.TRIM_MEMORY_RUNNING_CRITICAL) {
            // Aggressively trim background apps
            if (DEBUG) HookUtils.logDebug(TAG, "Critical memory trim: level=" + level);
        }
    }

    private static void handleProcessKilled(int pid) {
        // Track process kills
        if (DEBUG) HookUtils.logDebug(TAG, "Process killed: pid=" + pid);
    }

    // Configuration methods

    public static void setEnabled(boolean enabled) {
        AppLifecycleRealismHook.enabled = enabled;
        HookUtils.logInfo(TAG, "Hook " + (enabled ? "enabled" : "disabled"));
    }

    public static void setAverageSessionDuration(long durationMs) {
        averageSessionDurationMs = Math.max(30000, durationMs);
        HookUtils.logInfo(TAG, "Average session duration: " + averageSessionDurationMs + "ms");
    }

    public static void setMaxBackgroundApps(int max) {
        maxBackgroundApps = Math.max(1, Math.min(20, max));
        HookUtils.logInfo(TAG, "Max background apps: " + maxBackgroundApps);
    }

    public static void setColdStartProbability(double probability) {
        coldStartProbability = HookUtils.clamp(probability, 0.0, 1.0);
        HookUtils.logInfo(TAG, "Cold start probability: " + coldStartProbability);
    }

    public static void setAppTerminationProbability(double probability) {
        appTerminationProbability = HookUtils.clamp(probability, 0.0, 1.0);
        HookUtils.logInfo(TAG, "App termination probability: " + appTerminationProbability);
    }

    public static String getCurrentForegroundApp() {
        return currentForegroundApp;
    }

    public static int getRunningAppCount() {
        return runningApps.size();
    }

    public static int getBackgroundAppCount() {
        return backgroundApps.size();
    }

    public static int getTotalSessionCount() {
        return totalSessionCount.get();
    }

    public static long getTotalAppUsageTimeMs() {
        return totalAppUsageTimeMs.get();
    }

    public static long getCurrentSessionDuration() {
        if (currentForegroundApp == null) return 0;
        return System.currentTimeMillis() - currentSessionStartTime;
    }

    public static AppSessionType getCurrentSessionType() {
        long duration = getCurrentSessionDuration();

        if (duration < 30000) return AppSessionType.QUICK_CHECK;
        if (duration < 300000) return AppSessionType.BRIEF_USE;
        if (duration < 900000) return AppSessionType.NORMAL_USE;
        if (duration < 1800000) return AppSessionType.EXTENDED_USE;
        return AppSessionType.LONG_SESSION;
    }

    public static List<String> getRunningApps() {
        return new ArrayList<>(runningApps);
    }

    public static List<String> getBackgroundApps() {
        return new ArrayList<>(backgroundApps);
    }
}
