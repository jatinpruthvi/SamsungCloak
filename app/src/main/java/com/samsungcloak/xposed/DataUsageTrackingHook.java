package com.samsungcloak.xposed;

import android.net.TrafficStats;
import android.app.usage.NetworkStats;
import android.os.Handler;
import android.os.Looper;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * DataUsageTrackingHook - Network Data Simulation
 * 
 * Simulates realistic mobile data consumption patterns:
 * - Background sync throttling
 * - Data saver interactions
 * - Consumption tracking variation
 * - Warning/limit triggers
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class DataUsageTrackingHook {

    private static final String TAG = "[Data][UsageTracking]";
    private static final boolean DEBUG = true;
    
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    
    // Parameters
    private static float backgroundThrottleRate = 0.5f;    // 50% throttle
    private static long dataWarningBytes = 500 * 1024 * 1024;  // 500MB
    private static long dataLimitBytes = 1024 * 1024 * 1024;   // 1GB
    private static boolean dataSaverEnabled = false;
    
    // State
    private static long totalMobileRx = 0;
    private static long totalMobileTx = 0;
    private static boolean warningTriggered = false;
    private static boolean limitTriggered = false;
    
    private static final Random random = new Random();
    private static final List<DataUsageEvent> dataEvents = new CopyOnWriteArrayList<>();
    private static final AtomicLong simulatedRx = new AtomicLong(0);
    private static final AtomicLong simulatedTx = new AtomicLong(0);
    
    public static class DataUsageEvent {
        public long timestamp;
        public String type;
        public String details;
        
        public DataUsageEvent(String type, String details) {
            this.timestamp = System.currentTimeMillis();
            this.type = type;
            this.details = details;
        }
    }
    
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (hookInitialized.getAndSet(true)) {
            HookUtils.logDebug(TAG, "Already initialized");
            return;
        }
        
        HookUtils.logInfo(TAG, "Initializing Data Usage Tracking Hook");
        
        try {
            hookTrafficStats(lpparam);
            hookNetworkPolicy(lpparam);
            
            HookUtils.logInfo(TAG, "Data usage hook initialized");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Init failed: " + t.getMessage());
            hookInitialized.set(false);
        }
    }
    
    private static void hookTrafficStats(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> trafficStatsClass = XposedHelpers.findClass(
                "android.net.TrafficStats", lpparam.classLoader
            );
            
            XposedBridge.hookAllMethods(trafficStatsClass, "getMobileRxBytes",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    long actual = (long) param.getResult();
                    long simulated = actual + simulatedRx.get();
                    param.setResult(simulated);
                }
            });
            
            XposedBridge.hookAllMethods(trafficStatsClass, "getMobileTxBytes",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    long actual = (long) param.getResult();
                    long simulated = actual + simulatedTx.get();
                    param.setResult(simulated);
                }
            });
            
            HookUtils.logInfo(TAG, "TrafficStats hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "TrafficStats hook failed: " + t.getMessage());
        }
    }
    
    private static void hookNetworkPolicy(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> policyManagerClass = XposedHelpers.findClass(
                "android.net.NetworkPolicyManager", lpparam.classLoader
            );
            
            XposedBridge.hookAllMethods(policyManagerClass, "applyTemplate",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    // Background throttling
                    if (dataSaverEnabled) {
                        dataEvents.add(new DataUsageEvent("BACKGROUND_THROTTLED", 
                            "Throttle rate: " + (backgroundThrottleRate * 100) + "%"));
                    }
                }
            });
            
            HookUtils.logInfo(TAG, "NetworkPolicyManager hooked");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "NetworkPolicy hook skipped: " + t.getMessage());
        }
    }
    
    public static void addSimulatedUsage(long rxBytes, long txBytes) {
        simulatedRx.addAndGet(rxBytes);
        simulatedTx.addAndGet(txBytes);
        
        long total = simulatedRx.get() + simulatedTx.get();
        
        // Check warnings
        if (!warningTriggered && total > dataWarningBytes) {
            warningTriggered = true;
            dataEvents.add(new DataUsageEvent("DATA_WARNING", 
                "Approaching limit: " + (total / (1024*1024)) + "MB"));
        }
        
        if (!limitTriggered && total > dataLimitBytes) {
            limitTriggered = true;
            dataEvents.add(new DataUsageEvent("DATA_LIMIT_EXCEEDED", 
                "Limit reached: " + (total / (1024*1024)) + "MB"));
        }
    }
    
    public static void setEnabled(boolean enabled) {
        DataUsageTrackingHook.enabled = enabled;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void setDataSaverEnabled(boolean enabled) {
        dataSaverEnabled = enabled;
    }
    
    public static void setDataLimit(long bytes) {
        dataLimitBytes = bytes;
    }
    
    public static List<DataUsageEvent> getDataUsageEvents() {
        return new ArrayList<>(dataEvents);
    }
}
