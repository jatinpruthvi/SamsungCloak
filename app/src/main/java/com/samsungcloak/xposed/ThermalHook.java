package com.samsungcloak.xposed;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class ThermalHook {
    private static final String LOG_TAG = "SamsungCloak.ThermalHook";
    private static boolean initialized = false;

    private static final long sessionStart = System.currentTimeMillis();
    private static float baseTemperature = 35.0f;

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (initialized) {
            HookUtils.logDebug("ThermalHook already initialized");
            return;
        }

        try {
            hookThermalZone(lpparam);
            hookPowerManager(lpparam);
            initialized = true;
            HookUtils.logInfo("ThermalHook initialized successfully");
        } catch (Exception e) {
            HookUtils.logError("Failed to initialize ThermalHook: " + e.getMessage());
        }
    }

    private static void hookThermalZone(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> thermalClass = XposedHelpers.findClass(
                "android.os.HwBinder", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(thermalClass, "getService", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        if (param.args.length > 0) {
                            String serviceName = (String) param.args[0];
                            if ("thermal".equals(serviceName)) {
                                Object serviceProxy = param.getResult();
                                if (serviceProxy != null) {
                                    try {
                                        Object thermalService = XposedHelpers.callMethod(serviceProxy, "getService");
                                        if (thermalService != null) {
                                            float temperature = getThermalTemperature();
                                            XposedHelpers.callMethod(thermalService, "getTemperatures",
                                                new float[]{temperature});
                                        }
                                    } catch (Exception e) {
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        HookUtils.logError("Error in thermal zone hook: " + e.getMessage());
                    }
                }
            });

            HookUtils.logInfo("Hooked thermal zone methods");
        } catch (Exception e) {
            HookUtils.logError("Failed to hook thermal zone: " + e.getMessage());
        }
    }

    private static float getThermalTemperature() {
        long elapsed = System.currentTimeMillis() - sessionStart;
        float drift = (float) (0.02 * Math.sin(elapsed / 10000.0));
        float noise = HookUtils.generateGaussianNoise(1.5f);
        float temperature = baseTemperature + drift + noise;
        return HookUtils.clamp(temperature, 30.0f, 58.0f);
    }

    private static void hookPowerManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> powerManagerClass = XposedHelpers.findClass(
                "android.os.PowerManager", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(powerManagerClass, "getCurrentThermalStatus", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        String status = getThermalStatus();
                        param.setResult(status);
                        HookUtils.logDebug("PowerManager.getCurrentThermalStatus() -> " + status);
                    } catch (Exception e) {
                        HookUtils.logError("Error in getCurrentThermalStatus hook: " + e.getMessage());
                    }
                }
            });

            HookUtils.logInfo("Hooked PowerManager thermal methods");
        } catch (Exception e) {
            HookUtils.logError("Failed to hook power manager: " + e.getMessage());
        }
    }

    private static String getThermalStatus() {
        long elapsed = System.currentTimeMillis() - sessionStart;
        float temperature = getThermalTemperature();

        if (temperature > 50.0f) {
            return "critical";
        } else if (temperature > 45.0f) {
            return "moderate";
        } else if (temperature > 40.0f) {
            return "normal";
        } else {
            return "cool";
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
