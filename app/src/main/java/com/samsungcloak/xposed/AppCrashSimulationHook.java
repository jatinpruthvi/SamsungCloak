package com.samsungcloak.xposed;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;

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

/**
 * AppCrashSimulationHook - ANR & Crash Pattern Simulation
 * 
 * Simulates realistic app crashes and ANRs based on real Samsung patterns:
 * - ANR in main thread
 * - Native crashes
 * - Low memory kills
 * - Background process deaths
 * - Crash probability based on app state
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class AppCrashSimulationHook {

    private static final String TAG = "[Crash][ANR]";
    private static final boolean DEBUG = true;
    
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    
    // Crash probabilities (per event)
    private static float baseAnrProbability = 0.001f;       // 0.1% during normal use
    private static float typingAnrProbability = 0.002f;       // 0.2% while typing
    private static float heavyLoadAnrProbability = 0.02f;     // 2% during heavy load
    private static float lowMemoryKillProbability = 0.01f;    // 1% when memory low
    private static float backgroundDeathProbability = 0.005f; // 0.5% for background
    
    // Crash types distribution
    private static float anrPercentage = 0.40f;      // 40% ANR
    private static float nativeCrashPercentage = 0.20f; // 20% native
    private static float javaCrashPercentage = 0.25f;  // 25% Java exception
    private static float lowMemoryPercentage = 0.15f;  // 15% low memory
    
    // State tracking
    private static boolean isUserTyping = false;
    private static boolean isHeavyLoad = false;
    private static int currentMemoryPressure = 0;
    private static String currentForegroundApp = null;
    
    // Timing
    private static long lastAnrTime = 0;
    private static long anrCooldown = 60000; // 1 minute between ANRs
    private static long crashDelayMin = 1000;
    private static long crashDelayMax = 5000;
    
    private static final Random random = new Random();
    private static final List<CrashEvent> crashHistory = new CopyOnWriteArrayList<>();
    private static final Map<String, Integer> appCrashCount = new ConcurrentHashMap<>();
    private static Handler crashHandler = null;
    
    public static class CrashEvent {
        public String type;
        public String app;
        public long timestamp;
        public String stackTrace;
        public String reason;
        
        public CrashEvent(String type, String app, String reason, String stackTrace) {
            this.type = type;
            this.app = app;
            this.timestamp = System.currentTimeMillis();
            this.reason = reason;
            this.stackTrace = stackTrace;
        }
    }
    
    // Common ANR traces
    private static final String[] ANR_TRACES = {
        "main (tid=1) Prio=5 TcCl=0ms | BQ: main | | at com.app.Activity.onCreate(MainActivity.java:42)",
        "main (tid=1) Prio=5 TcCl=0ms | Runnable: android.os.MessageQueue.nativePollOnce | -1440ms",
        "main (tid=1) Prio=5 TcCl=0ms | Blocked: com.android.internal.os.Binder | 2341ms",
        "main (tid=1) Prio=5 TcCl=0ms | at java.lang.Thread.sleep(Native Method) | -5000ms",
        "main (tid=1) Prio=5 TcCl=0ms | at android.database.CursorWindow.getLong(CursorWindow.java:511)"
    };
    
    // Java exception types
    private static final String[] JAVA_EXCEPTIONS = {
        "java.lang.NullPointerException",
        "java.lang.RuntimeException",
        "java.lang.IllegalStateException",
        "java.lang.IndexOutOfBoundsException",
        "java.util.ConcurrentModificationException",
        "android.content.ActivityNotFoundException",
        "android.os.NetworkOnMainThreadException"
    };
    
    // Crash reasons
    private static final String[] CRASH_REASONS = {
        "Input dispatching timed out",
        "Native crash",
        "Force finish",
        "low memory",
        "native_heap_allocator",
        "Activity Manager"
    };
    
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (hookInitialized.getAndSet(true)) {
            HookUtils.logDebug(TAG, "Already initialized");
            return;
        }
        
        HookUtils.logInfo(TAG, "Initializing App Crash Simulation Hook");
        
        try {
            hookActivityThread(lpparam);
            hookProcess(lpparam);
            hookActivityManager(lpparam);
            
            crashHandler = new Handler(Looper.getMainLooper());
            
            HookUtils.logInfo(TAG, "Crash simulation hook initialized");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Init failed: " + t.getMessage());
            hookInitialized.set(false);
        }
    }
    
    private static void hookActivityThread(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> activityThreadClass = XposedHelpers.findClass(
                "android.app.ActivityThread", lpparam.classLoader
            );
            
            // Hook scheduleCrash to inject crashes
            XposedBridge.hookAllMethods(activityThreadClass, "scheduleCrash",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    // Determine if we should inject a crash
                    if (shouldInjectCrash()) {
                        String reason = CRASH_REASONS[random.nextInt(CRASH_REASONS.length)];
                        String trace = ANR_TRACES[random.nextInt(ANR_TRACES.length)];
                        
                        crashHistory.add(new CrashEvent("INJECTED", 
                            currentForegroundApp, reason, trace));
                        
                        HookUtils.logInfo(TAG, "Injecting crash: " + reason);
                        
                        // Don't block the actual crash
                    }
                }
            });
            
            // Hook getSystemContext
            XposedBridge.hookAllMethods(activityThreadClass, "getSystemContext",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    // Track which app is starting
                }
            });
            
            HookUtils.logInfo(TAG, "ActivityThread hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "ActivityThread hook failed: " + t.getMessage());
        }
    }
    
    private static void hookProcess(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> processClass = XposedHelpers.findClass(
                "android.os.Process", lpparam.classLoader
            );
            
            // Hook killProcess
            XposedBridge.hookAllMethods(processClass, "killProcess",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    int pid = 0;
                    if (param.args.length > 0 && param.args[0] instanceof Integer) {
                        pid = (int) param.args[0];
                    }
                    
                    // Check if this is a background process kill
                    if (random.nextFloat() < lowMemoryKillProbability && 
                        currentMemoryPressure > 50) {
                        
                        String reason = "low memory: killing " + pid;
                        crashHistory.add(new CrashEvent("LOW_MEMORY", 
                            "pid=" + pid, reason, "native_heap_allocator"));
                        
                        HookUtils.logDebug(TAG, "Simulating low memory kill: " + pid);
                    }
                }
            });
            
            HookUtils.logInfo(TAG, "Process hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Process hook failed: " + t.getMessage());
        }
    }
    
    private static void hookActivityManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> activityManagerClass = XposedHelpers.findClass(
                "android.app.ActivityManager", lpparam.classLoader
            );
            
            // Hook getProcessesInErrorState
            XposedBridge.hookAllMethods(activityManagerClass, "getProcessesInErrorState",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    // Could inject error conditions here
                }
            });
            
            HookUtils.logInfo(TAG, "ActivityManager hooked");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "ActivityManager hook skipped: " + t.getMessage());
        }
    }
    
    /**
     * Determine if we should inject a crash based on current conditions
     */
    private static boolean shouldInjectCrash() {
        long now = System.currentTimeMillis();
        
        // Cooldown check
        if (now - lastAnrTime < anrCooldown) {
            return false;
        }
        
        // Calculate probability based on state
        float probability = baseAnrProbability;
        
        if (isUserTyping) {
            probability = typingAnrProbability;
        } else if (isHeavyLoad) {
            probability = heavyLoadAnrProbability;
        }
        
        // Memory pressure modifier
        if (currentMemoryPressure > 70) {
            probability *= 2;
        }
        
        // App-specific modifier
        if (currentForegroundApp != null) {
            Integer count = appCrashCount.get(currentForegroundApp);
            int crashCount = count != null ? count : 0;
            
            // More crashes for frequently crashing apps
            probability *= (1 + crashCount * 0.5);
        }
        
        boolean shouldCrash = random.nextFloat() < probability;
        
        if (shouldCrash) {
            lastAnrTime = now;
            
            // Increment crash count
            if (currentForegroundApp != null) {
                appCrashCount.merge(currentForegroundApp, 1, Integer::sum);
            }
        }
        
        return shouldCrash;
    }
    
    /**
     * Get the type of crash to simulate
     */
    public static String getCrashType() {
        float roll = random.nextFloat();
        
        if (roll < anrPercentage) {
            return "ANR";
        } else if (roll < anrPercentage + nativeCrashPercentage) {
            return "NATIVE";
        } else if (roll < anrPercentage + nativeCrashPercentage + javaCrashPercentage) {
            return "JAVA";
        } else {
            return "LOW_MEMORY";
        }
    }
    
    /**
     * Generate a crash trace
     */
    public static String generateCrashTrace(String crashType) {
        switch (crashType) {
            case "ANR":
                return ANR_TRACES[random.nextInt(ANR_TRACES.length)];
            case "NATIVE":
                return "native crash at 0x" + 
                    Long.toHexString(random.nextLong() & 0xFFFFFFFFL);
            case "JAVA":
                return JAVA_EXCEPTIONS[random.nextInt(JAVA_EXCEPTIONS.length)] + 
                    "\n    at " + getRandomJavaTrace();
            default:
                return "low memory: cannot allocate";
        }
    }
    
    private static String getRandomJavaTrace() {
        String[] classes = {"MainActivity", "Service", "Receiver", "Provider"};
        String[] methods = {"onCreate", "onResume", "onClick", "handleMessage"};
        int[] lines = {25, 42, 58, 76, 89, 103};
        
        return "com.example.app." + classes[random.nextInt(classes.length)] +
            "." + methods[random.nextInt(methods.length)] +
            "(" + classes[random.nextInt(classes.length)] + ".java:" + 
            lines[random.nextInt(lines.length)] + ")";
    }
    
    // ========== State Management ==========
    
    public static void setUserTyping(boolean typing) {
        isUserTyping = typing;
    }
    
    public static void setHeavyLoad(boolean heavy) {
        isHeavyLoad = heavy;
    }
    
    public static void setMemoryPressure(int pressure) {
        currentMemoryPressure = Math.max(0, Math.min(100, pressure));
    }
    
    public static void setForegroundApp(String app) {
        currentForegroundApp = app;
    }
    
    // ========== Configuration ==========
    
    public static void setEnabled(boolean enabled) {
        AppCrashSimulationHook.enabled = enabled;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void setBaseAnrProbability(float probability) {
        baseAnrProbability = Math.max(0, Math.min(1, probability));
    }
    
    public static void setAnrCooldown(long millis) {
        anrCooldown = millis;
    }
    
    public static List<CrashEvent> getCrashHistory() {
        return new ArrayList<>(crashHistory);
    }
    
    public static Map<String, Integer> getAppCrashCounts() {
        return new HashMap<>(appCrashCount);
    }
}
