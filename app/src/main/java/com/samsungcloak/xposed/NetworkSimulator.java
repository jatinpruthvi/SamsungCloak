package com.samsungcloak.xposed;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class NetworkSimulator {
    private static final String LOG_TAG = "SamsungCloak.NetworkSimulator";
    private static boolean initialized = false;

    private static final String FAKE_SSID = "MyHomeWiFi";
    private static final String FAKE_BSSID = "aa:bb:cc:dd:ee:ff:00";
    private static final int FAKE_LINK_SPEED = 72;
    private static final int FAKE_FREQUENCY = 2437;
    private static final int FAKE_RSSI = -65;
    private static final int FAKE_NETWORK_TYPE = 13;

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (initialized) {
            HookUtils.logDebug("NetworkSimulator already initialized");
            return;
        }

        try {
            hookWifiInfo(lpparam);
            hookConnectivityManager(lpparam);
            initialized = true;
            HookUtils.logInfo("NetworkSimulator initialized successfully");
        } catch (Exception e) {
            HookUtils.logError("Failed to initialize NetworkSimulator: " + e.getMessage());
        }
    }

    private static void hookWifiInfo(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> wifiInfoClass = XposedHelpers.findClass(
                "android.net.wifi.WifiInfo", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(wifiInfoClass, "getSSID", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        param.setResult(FAKE_SSID);
                        HookUtils.logDebug("WifiInfo.getSSID() -> " + FAKE_SSID);
                    } catch (Exception e) {
                        HookUtils.logError("Error in getSSID hook: " + e.getMessage());
                    }
                }
            });

            XposedBridge.hookAllMethods(wifiInfoClass, "getBSSID", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        param.setResult(FAKE_BSSID);
                        HookUtils.logDebug("WifiInfo.getBSSID() -> " + FAKE_BSSID);
                    } catch (Exception e) {
                        HookUtils.logError("Error in getBSSID hook: " + e.getMessage());
                    }
                }
            });

            XposedBridge.hookAllMethods(wifiInfoClass, "getLinkSpeed", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        param.setResult(FAKE_LINK_SPEED);
                        HookUtils.logDebug("WifiInfo.getLinkSpeed() -> " + FAKE_LINK_SPEED);
                    } catch (Exception e) {
                        HookUtils.logError("Error in getLinkSpeed hook: " + e.getMessage());
                    }
                }
            });

            XposedBridge.hookAllMethods(wifiInfoClass, "getFrequency", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        param.setResult(FAKE_FREQUENCY);
                        HookUtils.logDebug("WifiInfo.getFrequency() -> " + FAKE_FREQUENCY + " MHz");
                    } catch (Exception e) {
                        HookUtils.logError("Error in getFrequency hook: " + e.getMessage());
                    }
                }
            });

            XposedBridge.hookAllMethods(wifiInfoClass, "getRssi", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        param.setResult(FAKE_RSSI);
                        HookUtils.logDebug("WifiInfo.getRssi() -> " + FAKE_RSSI);
                    } catch (Exception e) {
                        HookUtils.logError("Error in getRssi hook: " + e.getMessage());
                    }
                }
            });

            HookUtils.logInfo("Hooked WifiInfo methods");
        } catch (Exception e) {
            HookUtils.logError("Failed to hook WifiInfo: " + e.getMessage());
        }
    }

    private static void hookConnectivityManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> connectivityManagerClass = XposedHelpers.findClass(
                "android.net.ConnectivityManager", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(connectivityManagerManagerClass, "getActiveNetworkInfo", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        Object networkInfo = param.getResult();
                        if (networkInfo != null) {
                            try {
                                XposedHelpers.setIntField(networkInfo, "type", FAKE_NETWORK_TYPE);
                                HookUtils.logDebug("ConnectivityManager.getActiveNetworkInfo().type -> " + FAKE_NETWORK_TYPE);
                            } catch (Exception e) {
                            }
                        }
                    } catch (Exception e) {
                        HookUtils.logError("Error in getActiveNetworkInfo hook: " + e.getMessage());
                    }
                }
            });

            HookUtils.logInfo("Hooked ConnectivityManager methods");
        } catch (Exception e) {
            HookUtils.logError("Failed to hook connectivity manager: " + e.getMessage());
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
