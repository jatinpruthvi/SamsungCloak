package com.samsungcloak.xposed;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class DeepSleepHook {
    private static final String LOG_TAG = "SamsungCloak.DeepSleepHook";
    private static boolean initialized = false;

    private static final long sessionStart = System.currentTimeMillis();
    private static final long NIGHT_START_MS = 23 * 60 * 60 * 1000L;
    private static final long NIGHT_END_MS = 6 * 60 * 60 * 1000L;

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (initialized) {
            HookUtils.logDebug("DeepSleepHook already initialized");
            return;
        }

        try {
            hookPowerManager(lpparam);
            hookAlarmManager(lpparam);
            initialized = true;
            HookUtils.logInfo("DeepSleepHook initialized successfully");
        } catch (Exception e) {
            HookUtils.logError("Failed to initialize DeepSleepHook: " + e.getMessage());
        }
    }

    private static void hookPowerManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> powerManagerClass = XposedHelpers.findClass(
                "android.os.PowerManager", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(powerManagerClass, "isDeviceIdleMode", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        boolean inNightSleep = isInNightSleepWindow();
                        boolean isIdle = Math.random() < 0.25;
                        param.setResult(inNightSleep && isIdle);
                        HookUtils.logDebug("PowerManager.isDeviceIdleMode() -> " + (inNightSleep && isIdle));
                    } catch (Exception e) {
                        HookUtils.logError("Error in isDeviceIdleMode hook: " + e.getMessage());
                    }
                }
            });

            HookUtils.logInfo("Hooked PowerManager deep sleep methods");
        } catch (Exception e) {
            HookUtils.logError("Failed to hook power manager: " + e.getMessage());
        }
    }

    private static void hookAlarmManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> alarmManagerClass = XposedHelpers.findClass(
                "android.app.AlarmManager", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(alarmManagerClass, "getNextAlarmClock", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        long currentTime = System.currentTimeMillis();
                        long elapsed = currentTime - sessionStart;
                        boolean inNightSleep = isInNightSleepWindow();

                        if (inNightSleep && Math.random() < 0.5) {
                            long nextAlarm = currentTime + (7 * 60 * 60 * 1000L);
                            param.setResult(nextAlarm);
                            HookUtils.logDebug("AlarmManager.getNextAlarmClock() -> 7h in future");
                        }
                    } catch (Exception e) {
                        HookUtils.logError("Error in getNextAlarmClock hook: " + e.getMessage());
                    }
                }
            });

            HookUtils.logInfo("Hooked AlarmManager methods");
        } catch (Exception e) {
            HookUtils.logError("Failed to hook alarm manager: " + e.getMessage());
        }
    }

    private static boolean isInNightSleepWindow() {
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - sessionStart;
        long dayTime = elapsed % (24 * 60 * 60 * 1000L);

        return dayTime >= NIGHT_START_MS && dayTime <= NIGHT_END_MS;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
