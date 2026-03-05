package com.samsungcloak.xposed;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class VibrationSimulator {
    private static final String LOG_TAG = "SamsungCloak.VibrationSimulator";
    private static boolean initialized = false;

    private static final int VIBRATION_AMPLITUDE = 255;
    private static final int VIBRATION_FREQUENCY = 1;

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (initialized) {
            HookUtils.logDebug("VibrationSimulator already initialized");
            return;
        }

        try {
            hookVibrator(lpparam);
            initialized = true;
            HookUtils.logInfo("VibrationSimulator initialized successfully");
        } catch (Exception e) {
            HookUtils.logError("Failed to initialize VibrationSimulator: " + e.getMessage());
        }
    }

    private static void hookVibrator(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> vibratorClass = XposedHelpers.findClass(
                "android.os.Vibrator", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(vibratorClass, "hasVibrator", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        param.setResult(true);
                        HookUtils.logDebug("Vibrator.hasVibrator() -> true");
                    } catch (Exception e) {
                        HookUtils.logError("Error in hasVibrator hook: " + e.getMessage());
                    }
                }
            });

            HookUtils.logInfo("Hooked Vibrator methods");
        } catch (Exception e) {
            HookUtils.logError("Failed to hook vibrator: " + e.getMessage());
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
