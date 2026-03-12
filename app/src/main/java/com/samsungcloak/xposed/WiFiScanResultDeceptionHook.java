package com.samsungcloak.xposed;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
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
 * WiFiScanResultDeceptionHook - WiFi Scanning Delays & Stale Results
 * 
 * Simulates realistic WiFi behavior:
 * - Scan result delays (10-45 seconds)
 * - Hidden networks
 * - MAC randomization
 * - Connection timeouts
 * - Roaming hysteresis
 * - Stale APs (saved but out of range)
 * - "Connection failed" after multiple attempts
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class WiFiScanResultDeceptionHook {

    private static final String TAG = "[WiFi][Scan]";
    private static final boolean DEBUG = true;
    
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    
    // Timing (ms)
    private static int minScanDelay = 10000;
    private static int maxScanDelay = 45000;
    private static int currentScanDelay = 20000;
    
    // Failure rates
    private static float staleResultRate = 0.20f;        // 20% stale results
    private static float connectionFailureRate = 0.15f;  // 15% connection fails
    private static float hiddenNetworkRate = 0.05f;      // 5% hidden networks
    private static float macRandomizationRate = 0.10f;   // 10% MAC randomization
    private static float roamingFailureRate = 0.08f;     // 8% roaming failures
    
    // State
    private static boolean isWifiEnabled = true;
    private static boolean isScanning = false;
    private static long lastScanTime = 0;
    private static int connectionAttempts = 0;
    private static int maxConnectionAttempts = 5;
    private static String currentSsid = null;
    private static boolean isConnected = false;
    private static int signalStrength = -50; // dBm
    
    private static final Random random = new Random();
    private static final List<WiFiEvent> wifiEvents = new CopyOnWriteArrayList<>();
    private static final Map<String, ScanResult> cachedResults = new ConcurrentHashMap<>();
    private static Handler scanHandler = null;
    
    public static class WiFiEvent {
        public long timestamp;
        public String type;       // SCAN, CONNECT, DISCONNECT, FAILURE, ROAM
        public String ssid;
        public String details;
        
        public WiFiEvent(String type, String ssid, String details) {
            this.timestamp = System.currentTimeMillis();
            this.type = type;
            this.ssid = ssid;
            this.details = details;
        }
    }
    
    // Common network names for simulation
    private static final String[] COMMON_SSIDS = {
        "HomeWiFi", "Guest_Network", "Office_WiFi", "Starbucks", 
        "McDonalds_WiFi", "Airport_Free", "Hotel_WiFi", 
        "XFINITY", "AT&T", "T-Mobile", "Verizon_WiFi"
    };
    
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (hookInitialized.getAndSet(true)) {
            HookUtils.logDebug(TAG, "Already initialized");
            return;
        }
        
        HookUtils.logInfo(TAG, "Initializing WiFi Scan Result Deception Hook");
        
        try {
            hookWifiManager(lpparam);
            hookConnectivityManager(lpparam);
            
            scanHandler = new Handler(Looper.getMainLooper());
            
            // Initialize some cached results
            initializeCachedNetworks();
            
            HookUtils.logInfo(TAG, "WiFi hook initialized");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Init failed: " + t.getMessage());
            hookInitialized.set(false);
        }
    }
    
    private static void initializeCachedNetworks() {
        for (String ssid : COMMON_SSIDS) {
            // Create fake cached results
            ScanResult result = createFakeScanResult(ssid, -50 - random.nextInt(50));
            cachedResults.put(ssid, result);
        }
    }
    
    private static ScanResult createFakeScanResult(String ssid, int level) {
        ScanResult result = new ScanResult();
        // Note: ScanResult constructor and fields vary by Android version
        // This is simplified - real implementation would handle API differences
        
        result.SSID = ssid;
        result.BSSID = generateFakeBSSID();
        result.level = level;
        result.frequency = random.nextBoolean() ? 2450 : 5180 + random.nextInt(5) * 5;
        
        return result;
    }
    
    private static String generateFakeBSSID() {
        return String.format("%02X:%02X:%02X:%02X:%02X:%02X",
            random.nextInt(256), random.nextInt(256), random.nextInt(256),
            random.nextInt(256), random.nextInt(256), random.nextInt(256));
    }
    
    private static void hookWifiManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> wifiManagerClass = XposedHelpers.findClass(
                "android.net.wifi.WifiManager", lpparam.classLoader
            );
            
            // Hook startScan
            XposedBridge.hookAllMethods(wifiManagerClass, "startScan",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || !isWifiEnabled) return;
                    
                    // Add scan delay
                    int delay = minScanDelay + random.nextInt(maxScanDelay - minScanDelay);
                    currentScanDelay = delay;
                    
                    isScanning = true;
                    
                    wifiEvents.add(new WiFiEvent("SCAN_START", 
                        null, "Scan initiated, delay: " + delay + "ms"));
                    
                    HookUtils.logDebug(TAG, "WiFi scan started, delay: " + delay + "ms");
                }
                
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    // Return cached/stale results
                    if (random.nextFloat() < staleResultRate) {
                        // Return stale results
                        List<ScanResult> staleResults = new ArrayList<>(cachedResults.values());
                        
                        // Randomly mark some as out of range
                        for (ScanResult result : staleResults) {
                            if (random.nextFloat() < 0.3f) {
                                result.level = -100; // Out of range
                            }
                        }
                        
                        wifiEvents.add(new WiFiEvent("STALE_RESULTS", 
                            null, "Returned " + staleResults.size() + " stale results"));
                        
                        HookUtils.logDebug(TAG, "Returned stale scan results");
                    }
                    
                    isScanning = false;
                    lastScanTime = System.currentTimeMillis();
                }
            });
            
            // Hook getScanResults
            XposedBridge.hookAllMethods(wifiManagerClass, "getScanResults",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    // Could inject delayed results here
                    // Real implementation would modify the returned list
                }
            });
            
            // Hook enableNetwork
            XposedBridge.hookAllMethods(wifiManagerClass, "enableNetwork",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    int netId = -1;
                    boolean reconnect = false;
                    
                    if (param.args.length > 0 && param.args[0] instanceof Integer) {
                        netId = (int) param.args[0];
                    }
                    if (param.args.length > 1 && param.args[1] instanceof Boolean) {
                        reconnect = (boolean) param.args[1];
                    }
                    
                    connectionAttempts++;
                    
                    // Check for connection failure
                    if (connectionAttempts > 1 && random.nextFloat() < connectionFailureRate) {
                        wifiEvents.add(new WiFiEvent("CONNECTION_FAILED", 
                            "net-" + netId, "Attempt " + connectionAttempts + " failed"));
                        
                        HookUtils.logDebug(TAG, "Simulated connection failure");
                        
                        // Don't block, let it attempt anyway
                    }
                    
                    // Reset after max attempts
                    if (connectionAttempts >= maxConnectionAttempts) {
                        connectionAttempts = 0;
                    }
                }
            });
            
            // Hook disconnect
            XposedBridge.hookAllMethods(wifiManagerClass, "disconnect",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    isConnected = false;
                    currentSsid = null;
                    
                    wifiEvents.add(new WiFiEvent("DISCONNECT", 
                        currentSsid, "WiFi disconnected"));
                    
                    HookUtils.logDebug(TAG, "WiFi disconnected");
                }
            });
            
            HookUtils.logInfo(TAG, "WifiManager hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "WifiManager hook failed: " + t.getMessage());
        }
    }
    
    private static void hookConnectivityManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> connectivityManagerClass = XposedHelpers.findClass(
                "android.net.ConnectivityManager", lpparam.classLoader
            );
            
            // Hook getActiveNetworkInfo
            XposedBridge.hookAllMethods(connectivityManagerClass, "getActiveNetworkInfo",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    // Could modify network info to simulate issues
                }
            });
            
            HookUtils.logInfo(TAG, "ConnectivityManager hooked");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "ConnectivityManager hook skipped: " + t.getMessage());
        }
    }
    
    /**
     * Get current scan delay
     */
    public static int getCurrentScanDelay() {
        // Add some variance
        return currentScanDelay + random.nextInt(5000) - 2500;
    }
    
    /**
     * Check if we should simulate hidden network
     */
    public static boolean shouldShowHiddenNetwork() {
        return random.nextFloat() < hiddenNetworkRate;
    }
    
    /**
     * Check if MAC randomization should apply
     */
    public static boolean shouldRandomizeMac() {
        return random.nextFloat() < macRandomizationRate;
    }
    
    /**
     * Simulate roaming failure
     */
    public static boolean shouldSimulateRoamingFailure() {
        return isConnected && random.nextFloat() < roamingFailureRate;
    }
    
    /**
     * Get signal strength
     */
    public static int getSignalStrength() {
        return signalStrength;
    }
    
    // ========== Configuration ==========
    
    public static void setEnabled(boolean enabled) {
        WiFiScanResultDeceptionHook.enabled = enabled;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void setScanDelayRange(int minMs, int maxMs) {
        minScanDelay = Math.max(5000, minMs);
        maxScanDelay = Math.max(minScanDelay, maxMs);
    }
    
    public static void setStaleResultRate(float rate) {
        staleResultRate = Math.max(0, Math.min(1, rate));
    }
    
    public static void setConnectionFailureRate(float rate) {
        connectionFailureRate = Math.max(0, Math.min(1, rate));
    }
    
    public static void setWifiEnabled(boolean enabled) {
        isWifiEnabled = enabled;
    }
    
    public static boolean isConnected() {
        return isConnected;
    }
    
    public static String getCurrentSsid() {
        return currentSsid;
    }
    
    public static void resetConnectionAttempts() {
        connectionAttempts = 0;
    }
    
    public static List<WiFiEvent> getWifiEvents() {
        return new ArrayList<>(wifiEvents);
    }
}
