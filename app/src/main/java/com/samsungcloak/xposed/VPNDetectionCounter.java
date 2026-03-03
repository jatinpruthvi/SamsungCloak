package com.samsungcloak.xposed;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.net.NetworkInterface;
import java.util.Enumeration;

public class VPNDetectionCounter {
    private static final String LOG_TAG = "SamsungCloak.VPNDetectionCounter";
    private static boolean initialized = false;

    private static final String[] VPN_INTERFACE_PATTERNS = {
        "tun",
        "tap",
        "ppp",
        "vpn",
        "utun",
        "ipsec",
        "tun0",
        "tun1",
        "tun2",
        "tun3",
        "tun4",
        "tun5",
        "tun6",
        "tun7",
        "tun8",
        "tun9",
        "tun10",
        "tun11",
        "tun12",
        "tun13",
        "tun14",
        "tun15",
        "tun16",
        "tun17",
        "tun18",
        "tun19",
        "ppp0",
        "ppp1",
        "pptp",
        "l2tp",
        "ipsec",
        "gre",
        "sit"
    };

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (initialized) {
            HookUtils.logDebug("VPNDetectionCounter already initialized");
            return;
        }

        try {
            hookNetworkInterface(lpparam);
            hookConnectivityManager(lpparam);
            hookVpnService(lpparam);
            initialized = true;
            HookUtils.logInfo("VPNDetectionCounter initialized successfully");
        } catch (Exception e) {
            HookUtils.logError("Failed to initialize VPNDetectionCounter: " + e.getMessage());
        }
    }

    private static void hookNetworkInterface(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> networkInterfaceClass = XposedHelpers.findClass(
                "java.net.NetworkInterface", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(networkInterfaceClass, "getNetworkInterfaces", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        Enumeration<NetworkInterface> interfaces = (Enumeration<NetworkInterface>) param.getResult();
                        java.util.List<NetworkInterface> filteredInterfaces = new java.util.ArrayList<>();

                        while (interfaces.hasMoreElements()) {
                            NetworkInterface netIf = interfaces.nextElement();
                            if (isVPNInterface(netIf)) {
                                HookUtils.logDebug("Filtering VPN interface: " + netIf.getName());
                            } else {
                                filteredInterfaces.add(netIf);
                            }
                        }

                        if (filteredInterfaces.size() != java.util.Collections.list(interfaces).size()) {
                            param.setResult(java.util.Collections.enumeration(filteredInterfaces));
                            HookUtils.logDebug("Filtered " + 
                                (java.util.Collections.list(interfaces).size() - filteredInterfaces.size()) + " VPN interfaces");
                        }
                    } catch (Exception e) {
                        HookUtils.logError("Error in getNetworkInterfaces hook: " + e.getMessage());
                    }
                }
            });

            HookUtils.logInfo("Hooked NetworkInterface.getNetworkInterfaces()");
        } catch (Exception e) {
            HookUtils.logError("Failed to hook NetworkInterface: " + e.getMessage());
        }
    }

    private static void hookConnectivityManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> connectivityManagerClass = XposedHelpers.findClass(
                "android.net.ConnectivityManager", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(connectivityManagerManagerClass, "getAllNetworks", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        Object networks = param.getResult();
                        if (networks instanceof java.util.List) {
                            java.util.List<?> networkList = (java.util.List<?>) networks;
                            java.util.List<Object> filteredNetworks = new java.util.ArrayList<>();

                            for (Object network : networkList) {
                                try {
                                    Object networkInfo = XposedHelpers.callMethod(network, "getNetworkInfo");
                                    if (networkInfo != null) {
                                        Object typeObj = XposedHelpers.callMethod(networkInfo, "getType");
                                        String typeStr = typeObj != null ? typeObj.toString() : "";

                                        if (!isVPNType(typeStr) && !isVPNInterface(networkInfo)) {
                                            filteredNetworks.add(network);
                                        } else {
                                            HookUtils.logDebug("Filtering VPN network: " + typeStr);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                }
                            }

                            if (filteredNetworks.size() != networkList.size()) {
                                param.setResult(filteredNetworks);
                                HookUtils.logDebug("Filtered " + 
                                    (networkList.size() - filteredNetworks.size()) + " VPN networks");
                            }
                        }
                    } catch (Exception e) {
                        HookUtils.logError("Error in getAllNetworks hook: " + e.getMessage());
                    }
                }
            });

            HookUtils.logInfo("Hooked ConnectivityManager.getAllNetworks()");
        } catch (Exception e) {
            HookUtils.logError("Failed to hook connectivity manager: " + e.getMessage());
        }
    }

    private static void hookVpnService(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> vpnServiceClass = XposedHelpers.findClass(
                "android.net.VpnService", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(vpnServiceClass, "isAlwaysOnVpnPackage", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        param.setResult(false);
                        HookUtils.logDebug("VpnService.isAlwaysOnVpnPackage() -> false");
                    } catch (Exception e) {
                        HookUtils.logError("Error in isAlwaysOnVpnPackage hook: " + e.getMessage());
                    }
                }
            });

            XposedBridge.hookAllMethods(vpnServiceClass, "getVpnConfig", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        param.setResult(null);
                        HookUtils.logDebug("VpnService.getVpnConfig() -> null");
                    } catch (Exception e) {
                        HookUtils.logError("Error in getVpnConfig hook: " + e.getMessage());
                    }
                }
            });

            HookUtils.logInfo("Hooked VpnService methods");
        } catch (Exception e) {
            HookUtils.logError("Failed to hook VPN service: " + e.getMessage());
        }
    }

    private static boolean isVPNInterface(Object networkInfo) {
        try {
            String name = (String) XposedHelpers.callMethod(networkInfo, "getName");
            for (String pattern : VPN_INTERFACE_PATTERNS) {
                if (name != null && name.toLowerCase().contains(pattern)) {
                    return true;
                }
            }
        } catch (Exception e) {
        }
        return false;
    }

    private static boolean isVPNType(String typeStr) {
        for (String pattern : VPN_INTERFACE_PATTERNS) {
            if (typeStr != null && typeStr.toLowerCase().contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
