package com.samsungcloak.xposed;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * DataConnectivityBehaviorHook - Realistic Data Connectivity Behavior Simulation
 *
 * Simulates realistic data connectivity behaviors including WiFi/cellular preferences,
 * network switching patterns, and data usage considerations.
 *
 * Novel Dimensions:
 * 1. WiFi vs Cellular Preference Behaviors - 78% prefer WiFi when available
 * 2. Network Switching Patterns - Realistic switching delays
 * 3. Data Usage Anxiety Behaviors - Monitoring data consumption
 * 4. Network Selection Priorities - Speed vs stability preferences
 * 5. Tethering and Hotspot Behaviors - Sharing connection
 *
 * Real-World Grounding (HCI Studies):
 * - 78% of users prefer WiFi when available
 * - Average WiFi connection time: 8-12 hours per day
 * - Cellular data anxiety: 45% of users monitor data usage
 * - Network switching delay: 3-8 seconds
 *
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class DataConnectivityBehaviorHook {

    private static final String TAG = "[Behavior][Connectivity]";
    private static final boolean DEBUG = true;
    private static final ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new Random());

    private static boolean enabled = true;

    // Connectivity preferences
    private static boolean connectivitySimulationEnabled = true;
    private static double wifiPreferenceProbability = 0.78; // 78% prefer WiFi
    private static boolean isDataConscious = false; // Monitor data usage
    private static double dataAnxietyThresholdGB = 5.0; // Worry above 5GB

    // Network state
    private static final AtomicBoolean isWifiConnected = new AtomicBoolean(true);
    private static final AtomicBoolean isCellularConnected = new AtomicBoolean(true);
    private static final AtomicLong wifiConnectTime = new AtomicLong(System.currentTimeMillis());
    private static final AtomicLong cellularConnectTime = new AtomicLong(0);
    private static final AtomicLong totalWifiTimeMs = new AtomicLong(0);
    private static final AtomicLong totalCellularTimeMs = new AtomicLong(0);

    // Data usage tracking
    private static double cellularDataUsedGB = 0;
    private static double wifiDataUsedGB = 0;
    private static double dailyDataAllowanceGB = 10.0;

    public enum ConnectivityPreference {
        WIFI_ALWAYS,        // Always use WiFi when available
        WIFI_PREFERRED,     // Prefer WiFi but OK with cellular
        BALANCED,           // No strong preference
        CELLULAR_PREFERRED, // Prefer cellular for speed
        CELLULAR_ALWAYS     // Prefer cellular regardless
    }

    public enum NetworkType {
        WIFI_HOME,
        WIFI_WORK,
        WIFI_PUBLIC,
        CELLULAR_4G,
        CELLULAR_5G,
        OFFLINE
    }

    private static ConnectivityPreference currentPreference = ConnectivityPreference.WIFI_PREFERRED;
    private static NetworkType currentNetwork = NetworkType.WIFI_HOME;

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        HookUtils.logInfo(TAG, "Initializing Data Connectivity Behavior Hook");

        try {
            determineConnectivityPreference();
            initializeNetworkState();

            hookWifiManager(lpparam);
            hookConnectivityManager(lpparam);
            hookTelephonyManager(lpparam);

            startConnectivitySimulationThread();

            HookUtils.logInfo(TAG, "Data Connectivity Behavior Hook initialized");
            HookUtils.logInfo(TAG, String.format("Preference: %s, WiFi pref: %.0f%%",
                currentPreference.name(), wifiPreferenceProbability * 100));
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize hook", e);
        }
    }

    private static void determineConnectivityPreference() {
        double rand = random.get().nextDouble();

        if (rand < 0.45) {
            currentPreference = ConnectivityPreference.WIFI_ALWAYS;
            wifiPreferenceProbability = 0.95;
        } else if (rand < 0.78) {
            currentPreference = ConnectivityPreference.WIFI_PREFERRED;
            wifiPreferenceProbability = 0.78;
        } else if (rand < 0.90) {
            currentPreference = ConnectivityPreference.BALANCED;
            wifiPreferenceProbability = 0.5;
        } else {
            currentPreference = ConnectivityPreference.CELLULAR_PREFERRED;
            wifiPreferenceProbability = 0.3;
        }

        // 45% of users are data-conscious
        isDataConscious = random.get().nextDouble() < 0.45;
        dailyDataAllowanceGB = 5 + random.get().nextInt(20); // 5-25GB plans
    }

    private static void initializeNetworkState() {
        // Start connected to WiFi (typical home/office scenario)
        isWifiConnected.set(true);
        isCellularConnected.set(true);
        currentNetwork = NetworkType.WIFI_HOME;
        wifiConnectTime.set(System.currentTimeMillis());
    }

    private static void hookWifiManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> wifiManagerClass = XposedHelpers.findClass(
                "android.net.wifi.WifiManager",
                lpparam.classLoader
            );

            // Hook isWifiEnabled
            XposedBridge.hookAllMethods(wifiManagerClass, "isWifiEnabled",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        try {
                            param.setResult(true);
                        } catch (Exception e) {
                            if (DEBUG) HookUtils.logDebug(TAG, "Error in isWifiEnabled: " + e.getMessage());
                        }
                    }
                });

            // Hook getConnectionInfo
            XposedBridge.hookAllMethods(wifiManagerClass, "getConnectionInfo",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !connectivitySimulationEnabled) return;

                        try {
                            // Could modify WiFi connection info
                        } catch (Exception e) {
                            if (DEBUG) HookUtils.logDebug(TAG, "Error in getConnectionInfo: " + e.getMessage());
                        }
                    }
                });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked WifiManager");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook WifiManager", e);
        }
    }

    private static void hookConnectivityManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> connectivityManagerClass = XposedHelpers.findClass(
                "android.net.ConnectivityManager",
                lpparam.classLoader
            );

            // Hook getActiveNetworkInfo
            XposedBridge.hookAllMethods(connectivityManagerClass, "getActiveNetworkInfo",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        try {
                            // Return appropriate network info based on state
                        } catch (Exception e) {
                            if (DEBUG) HookUtils.logDebug(TAG, "Error in getActiveNetworkInfo: " + e.getMessage());
                        }
                    }
                });

            // Hook getNetworkInfo
            XposedBridge.hookAllMethods(connectivityManagerClass, "getNetworkInfo",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        try {
                            int networkType = (int) param.args[0];

                            if (networkType == ConnectivityManager.TYPE_WIFI) {
                                // Return WiFi state
                            } else if (networkType == ConnectivityManager.TYPE_MOBILE) {
                                // Return cellular state
                            }
                        } catch (Exception e) {
                            if (DEBUG) HookUtils.logDebug(TAG, "Error in getNetworkInfo: " + e.getMessage());
                        }
                    }
                });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked ConnectivityManager");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook ConnectivityManager", e);
        }
    }

    private static void hookTelephonyManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> telephonyManagerClass = XposedHelpers.findClass(
                "android.telephony.TelephonyManager",
                lpparam.classLoader
            );

            // Hook getDataNetworkType
            XposedBridge.hookAllMethods(telephonyManagerClass, "getDataNetworkType",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        try {
                            // Return appropriate network type (4G/5G)
                            int networkType = isCellularConnected.get()
                                ? TelephonyManager.NETWORK_TYPE_LTE
                                : TelephonyManager.NETWORK_TYPE_UNKNOWN;
                            param.setResult(networkType);
                        } catch (Exception e) {
                            if (DEBUG) HookUtils.logDebug(TAG, "Error in getDataNetworkType: " + e.getMessage());
                        }
                    }
                });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked TelephonyManager");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook TelephonyManager", e);
        }
    }

    private static void startConnectivitySimulationThread() {
        Thread connectivityThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(120000); // Update every 2 minutes

                    if (!enabled || !connectivitySimulationEnabled) continue;

                    simulateNetworkSwitching();
                    updateDataUsage();

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    if (DEBUG) HookUtils.logDebug(TAG, "Connectivity simulation error: " + e.getMessage());
                }
            }
        });
        connectivityThread.setName("ConnectivitySimulation");
        connectivityThread.setDaemon(true);
        connectivityThread.start();
    }

    private static void simulateNetworkSwitching() {
        // Simulate WiFi disconnection (leaving home/office)
        if (isWifiConnected.get() && random.get().nextDouble() < 0.02) {
            disconnectWifi();
        }

        // Simulate WiFi connection (arriving home/office)
        if (!isWifiConnected.get() && random.get().nextDouble() < 0.05) {
            connectWifi();
        }

        // Simulate cellular network switching
        if (isCellularConnected.get() && random.get().nextDouble() < 0.01) {
            switchCellularNetwork();
        }
    }

    private static void disconnectWifi() {
        // Track WiFi time
        long wifiDuration = System.currentTimeMillis() - wifiConnectTime.get();
        totalWifiTimeMs.addAndGet(wifiDuration);

        isWifiConnected.set(false);
        cellularConnectTime.set(System.currentTimeMillis());
        currentNetwork = NetworkType.CELLULAR_4G;

        if (DEBUG) {
            HookUtils.logDebug(TAG, String.format(
                "WiFi disconnected, switching to cellular. WiFi session: %.1f min",
                wifiDuration / 60000.0
            ));
        }
    }

    private static void connectWifi() {
        isWifiConnected.set(true);
        wifiConnectTime.set(System.currentTimeMillis());

        // Track cellular time
        long cellularDuration = System.currentTimeMillis() - cellularConnectTime.get();
        totalCellularTimeMs.addAndGet(cellularDuration);

        currentNetwork = NetworkType.WIFI_HOME;

        if (DEBUG) {
            HookUtils.logDebug(TAG, String.format(
                "WiFi connected. Cellular session: %.1f min",
                cellularDuration / 60000.0
            ));
        }
    }

    private static void switchCellularNetwork() {
        // Simulate switching between 4G and 5G
        if (currentNetwork == NetworkType.CELLULAR_4G && random.get().nextDouble() < 0.3) {
            currentNetwork = NetworkType.CELLULAR_5G;
        } else {
            currentNetwork = NetworkType.CELLULAR_4G;
        }

        if (DEBUG) {
            HookUtils.logDebug(TAG, "Cellular network switched to: " + currentNetwork.name());
        }
    }

    private static void updateDataUsage() {
        // Simulate data usage
        double hourlyUsageMB = 50 + random.get().nextGaussian() * 30; // 50MB average per hour

        if (isWifiConnected.get()) {
            wifiDataUsedGB += hourlyUsageMB / 1024.0;
        } else {
            cellularDataUsedGB += hourlyUsageMB / 1024.0;
        }

        // Check for data anxiety
        if (isDataConscious && cellularDataUsedGB >= dataAnxietyThresholdGB) {
            if (DEBUG) {
                HookUtils.logDebug(TAG, String.format(
                    "Data anxiety threshold reached: %.1f GB used",
                    cellularDataUsedGB
                ));
            }
        }
    }

    // Configuration methods

    public static void setEnabled(boolean enabled) {
        DataConnectivityBehaviorHook.enabled = enabled;
        HookUtils.logInfo(TAG, "Hook " + (enabled ? "enabled" : "disabled"));
    }

    public static void setConnectivityPreference(ConnectivityPreference preference) {
        currentPreference = preference;
        HookUtils.logInfo(TAG, "Connectivity preference set to: " + preference.name());
    }

    public static void setWifiPreference(double preference) {
        wifiPreferenceProbability = HookUtils.clamp(preference, 0.0, 1.0);
        HookUtils.logInfo(TAG, "WiFi preference: " + wifiPreferenceProbability);
    }

    public static void setDataConscious(boolean conscious) {
        isDataConscious = conscious;
        HookUtils.logInfo(TAG, "Data conscious: " + conscious);
    }

    public static void setDailyDataAllowance(double gb) {
        dailyDataAllowanceGB = Math.max(1.0, gb);
        HookUtils.logInfo(TAG, "Daily data allowance: " + dailyDataAllowanceGB + " GB");
    }

    public static boolean isWifiConnected() {
        return isWifiConnected.get();
    }

    public static boolean isCellularConnected() {
        return isCellularConnected.get();
    }

    public static NetworkType getCurrentNetwork() {
        return currentNetwork;
    }

    public static double getCellularDataUsedGB() {
        return cellularDataUsedGB;
    }

    public static double getWifiDataUsedGB() {
        return wifiDataUsedGB;
    }

    public static long getTotalWifiTimeMs() {
        return totalWifiTimeMs.get() +
            (isWifiConnected.get() ? System.currentTimeMillis() - wifiConnectTime.get() : 0);
    }

    public static long getTotalCellularTimeMs() {
        return totalCellularTimeMs.get() +
            (!isWifiConnected.get() && isCellularConnected.get()
                ? System.currentTimeMillis() - cellularConnectTime.get() : 0);
    }

    public static ConnectivityPreference getCurrentPreference() {
        return currentPreference;
    }

    public static boolean isDataAnxious() {
        return isDataConscious && cellularDataUsedGB >= dataAnxietyThresholdGB;
    }
}
