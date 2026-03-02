package com.samsungcloak.xposed;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class TimingController {
    private static final String LOG_TAG = "SamsungCloak.TimingController";
    private static boolean initialized = false;

    private static long baseTime = 0;
    private static long baseNanoTime = 0;

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (initialized) {
            HookUtils.logDebug("TimingController already initialized");
            return;
        }

        try {
            baseTime = System.currentTimeMillis();
            baseNanoTime = System.nanoTime();

            hookSystemTiming(lpparam);
            initialized = true;
            HookUtils.logInfo("TimingController initialized successfully");
        } catch (Exception e) {
            HookUtils.logError("Failed to initialize TimingController: " + e.getMessage());
        }
    }

    private static void hookSystemTiming(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> systemClass = XposedHelpers.findClass(
                "java.lang.System", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(systemClass, "currentTimeMillis", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        long currentTime = System.currentTimeMillis();
                        long elapsedTime = currentTime - baseTime;

                        int humanVariance = (int) (elapsedTime % 10);

                        if (humanVariance > 5) {
                            param.setResult(currentTime + humanVariance);
                        } else {
                            param.setResult(currentTime);
                        }
                    } catch (Exception e) {
                        HookUtils.logError("Error in currentTimeMillis hook: " + e.getMessage());
                    }
                }
            });

            XposedBridge.hookAllMethods(systemClass, "nanoTime", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        long currentNano = System.nanoTime();
                        long elapsedNano = currentNano - baseNanoTime;

                        long humanVariance = (long) (elapsedNano % 100000);

                        if (humanVariance > 50000) {
                            param.setResult(currentNano + humanVariance);
                        } else {
                            param.setResult(currentNano);
                        }
                    } catch (Exception e) {
                        HookUtils.logError("Error in nanoTime hook: " + e.getMessage());
                    }
                }
            });

            HookUtils.logInfo("Hooked System timing methods");
        } catch (Exception e) {
            HookUtils.logError("Failed to hook System timing: " + e.getMessage());
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
