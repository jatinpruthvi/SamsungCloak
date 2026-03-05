package com.samsungcloak.xposed;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class IdentifierHook {
    private static final String LOG_TAG = "SamsungCloak.IdentifierHook";
    private static boolean initialized = false;

    public static final String FAKE_IMEI = "3548291023456781";
    public static final String FAKE_MEID = "A0000041234567";

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (initialized) {
            HookUtils.logDebug("IdentifierHook already initialized");
            return;
        }

        try {
            hookTelephonyManager(lpparam);
            initialized = true;
            HookUtils.logInfo("IdentifierHook initialized successfully");
        } catch (Exception e) {
            HookUtils.logError("Failed to initialize IdentifierHook: " + e.getMessage());
        }
    }

    private static void hookTelephonyManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> telephonyManagerClass = XposedHelpers.findClass(
                "android.telephony.TelephonyManager", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(telephonyManagerClass, "getDeviceId", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        param.setResult(FAKE_IMEI);
                        HookUtils.logDebug("TelephonyManager.getDeviceId() -> " + FAKE_IMEI);
                    } catch (Exception e) {
                        HookUtils.logError("Error in getDeviceId hook: " + e.getMessage());
                    }
                }
            });

            XposedBridge.hookAllMethods(telephonyManagerClass, "getImei", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        param.setResult(FAKE_IMEI);
                        HookUtils.logDebug("TelephonyManager.getImei() -> " + FAKE_IMEI);
                    } catch (Exception e) {
                        HookUtils.logError("Error in getImei hook: " + e.getMessage());
                    }
                }
            });

            XposedBridge.hookAllMethods(telephonyManagerClass, "getMeid", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        param.setResult(FAKE_MEID);
                        HookUtils.logDebug("TelephonyManager.getMeid() -> " + FAKE_MEID);
                    } catch (Exception e) {
                        HookUtils.logError("Error in getMeid hook: " + e.getMessage());
                    }
                }
            });

            XposedBridge.hookAllMethods(telephonyManagerClass, "getSimSerialNumber", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        param.setResult(DeviceConstants.SERIAL);
                        HookUtils.logDebug("TelephonyManager.getSimSerialNumber() -> " + DeviceConstants.SERIAL);
                    } catch (Exception e) {
                        HookUtils.logError("Error in getSimSerialNumber hook: " + e.getMessage());
                    }
                }
            });

            HookUtils.logInfo("Hooked TelephonyManager identifier methods");
        } catch (Exception e) {
            HookUtils.logError("Failed to hook telephony manager: " + e.getMessage());
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
