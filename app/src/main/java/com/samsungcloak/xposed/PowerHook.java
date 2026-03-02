package com.samsungcloak.xposed;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class PowerHook {
    private static final String LOG_TAG = "SamsungCloak.PowerHook";
    private static boolean initialized = false;

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (initialized) {
            HookUtils.logDebug("PowerHook already initialized");
            return;
        }

        try {
            hookPowerManager(lpparam);
            initialized = true;
            HookUtils.logInfo("PowerHook initialized successfully");
        } catch (Exception e) {
            HookUtils.logError("Failed to initialize PowerHook: " + e.getMessage());
        }
    }

    private static void hookPowerManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> powerManagerClass = XposedHelpers.findClass(
                "android.os.PowerManager", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(powerManagerClass, "isInteractive", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        boolean isInteractive = System.currentTimeMillis() % 10000 < 5000;
                        param.setResult(isInteractive);
                        HookUtils.logDebug("PowerManager.isInteractive() -> " + isInteractive);
                    } catch (Exception e) {
                        HookUtils.logError("Error in isInteractive hook: " + e.getMessage());
                    }
                }
            });

            XposedBridge.hookAllMethods(powerManagerClass, "isPowerSaveMode", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        param.setResult(false);
                        HookUtils.logDebug("PowerManager.isPowerSaveMode() -> false");
                    } catch (Exception e) {
                        HookUtils.logError("Error in isPowerSaveMode hook: " + e.getMessage());
                    }
                }
            });

            XposedBridge.hookAllMethods(powerManagerClass, "isDeviceIdleMode", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        param.setResult(false);
                        HookUtils.logDebug("PowerManager.isDeviceIdleMode() -> false");
                    } catch (Exception e) {
                        HookUtils.logError("Error in isDeviceIdleMode hook: " + e.getMessage());
                    }
                }
            });

            HookUtils.logInfo("Hooked PowerManager methods");
        } catch (Exception e) {
            HookUtils.logError("Failed to hook power manager: " + e.getMessage());
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
