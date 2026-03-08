package com.samsungcloak.xposed.telemetry;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class DeviceStateEntropyHook {
    private static final String LOG_TAG = "SamsungCloak.DeviceStateEntropy";
    private static boolean initialized = false;

    private static final long sessionStartTime = System.currentTimeMillis();
    
    private static final float SM_A125U_BATTERY_CAPACITY_MAH = 5000.0f;
    private static final float TYPICAL_DISCHARGE_RATE_MA = 450.0f;
    private static final float SCREEN_ON_ADDITIONAL_MA = 250.0f;
    
    private static boolean screenOn = true;
    private static float batteryLevel = 1.0f;
    private static float brightnessLevel = 0.7f;
    private static long lastStateChangeTime = System.currentTimeMillis();
    
    private static final int BRIGHTNESS_TRANSITION_DURATION_MS = 300;
    private static float targetBrightness = 0.7f;
    private static float currentBrightness = 0.7f;

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (initialized) {
            XposedBridge.log(LOG_TAG + " already initialized");
            return;
        }

        try {
            hookBatteryManager(lpparam);
            hookDisplayManager(lpparam);
            hookSystemProperties(lpparam);
            initialized = true;
            XposedBridge.log(LOG_TAG + " initialized successfully");
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " Failed to initialize: " + e.getMessage());
        }
    }

    private static void hookBatteryManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> batteryManagerClass = XposedHelpers.findClass(
                "android.os.BatteryManager", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(batteryManagerClass, "getIntProperty", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        int property = (int) param.args[0];
                        if (property == 1) {
                            int batteryPct = calculateBatteryPercentage();
                            param.setResult(batteryPct);
                            XposedBridge.log(LOG_TAG + " Battery level: " + batteryPct + "%");
                        }
                    } catch (Exception e) {
                    }
                }
            });

            XposedBridge.hookAllMethods(batteryManagerClass, "getScale", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(100);
                }
            });

            XposedBridge.hookAllMethods(batteryManagerClass, "isCharging", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(false);
                }
            });

            XposedBridge.log(LOG_TAG + " Hooked BatteryManager methods");
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " Failed to hook BatteryManager: " + e.getMessage());
        }
    }

    private static void hookDisplayManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> displayManagerClass = XposedHelpers.findClass(
                "android.hardware.display.DisplayManager", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(displayManagerClass, "getBrightness", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    updateBrightnessTransition();
                    float brightnessWithEntropy = applyBrightnessEntropy();
                    param.setResult((int) (brightnessWithEntropy * 255));
                    XposedBridge.log(LOG_TAG + " Brightness: " + (brightnessWithEntropy * 100) + "%");
                }
            });

            XposedBridge.log(LOG_TAG + " Hooked DisplayManager methods");
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " Failed to hook DisplayManager: " + e.getMessage());
        }
    }

    private static void hookSystemProperties(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> systemPropertiesClass = XposedHelpers.findClass(
                "android.os.SystemProperties", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(systemPropertiesClass, "get", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    String key = (String) param.args[0];
                    if ("ro.uptime".equals(key) || "persist.sys.uptime".equals(key)) {
                        long uptimeWithEntropy = calculateUptimeWithEntropy();
                        param.setResult(String.valueOf(uptimeWithEntropy));
                        XposedBridge.log(LOG_TAG + " Uptime: " + uptimeWithEntropy + "s");
                    } else if ("ro.runtime.firstboot".equals(key)) {
                        long firstBoot = System.currentTimeMillis() - (calculateUptimeWithEntropy() * 1000);
                        param.setResult(String.valueOf(firstBoot));
                    }
                }
            });

            XposedBridge.log(LOG_TAG + " Hooked SystemProperties");
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " Failed to hook SystemProperties: " + e.getMessage());
        }
    }

    private static int calculateBatteryPercentage() {
        long elapsedMs = System.currentTimeMillis() - sessionStartTime;
        double elapsedHours = elapsedMs / 3600000.0;
        
        float currentDischargeRate = TYPICAL_DISCHARGE_RATE_MA;
        if (screenOn) {
            currentDischargeRate += SCREEN_ON_ADDITIONAL_MA * currentBrightness;
        }
        
        float dischargeAmount = (currentDischargeRate * (float) elapsedHours) / SM_A125U_BATTERY_CAPACITY_MAH;
        
        float noiseFactor = (float) (Math.random() * 0.02 - 0.01);
        
        batteryLevel = Math.max(0.05f, 1.0f - dischargeAmount + noiseFactor);
        
        return (int) (batteryLevel * 100);
    }

    private static float applyBrightnessEntropy() {
        float noise = (float) ((Math.random() * 0.04) - 0.02);
        float userFluctuation = (float) ((Math.random() * 0.03) - 0.015);
        
        return Math.max(0.05f, Math.min(1.0f, currentBrightness + noise + userFluctuation));
    }

    private static void updateBrightnessTransition() {
        long currentTime = System.currentTimeMillis();
        long transitionElapsed = currentTime - lastStateChangeTime;
        
        if (transitionElapsed < BRIGHTNESS_TRANSITION_DURATION_MS) {
            float progress = (float) transitionElapsed / BRIGHTNESS_TRANSITION_DURATION_MS;
            float easedProgress = easeInOutCubic(progress);
            currentBrightness = currentBrightness + (targetBrightness - currentBrightness) * easedProgress;
        } else {
            currentBrightness = targetBrightness;
        }
    }

    private static float easeInOutCubic(float t) {
        return t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
    }

    private static long calculateUptimeWithEntropy() {
        long baseUptime = (System.currentTimeMillis() - sessionStartTime) / 1000;
        
        double jitter = (Math.random() * 2.0) - 1.0;
        long entropyUptime = (long) (baseUptime + jitter);
        
        return Math.max(0, entropyUptime);
    }

    public static void setScreenState(boolean on) {
        screenOn = on;
        lastStateChangeTime = System.currentTimeMillis();
        targetBrightness = on ? 0.7f : 0.0f;
    }

    public static void setTargetBrightness(float brightness) {
        targetBrightness = Math.max(0.0f, Math.min(1.0f, brightness));
        lastStateChangeTime = System.currentTimeMillis();
    }

    public static float getCurrentBatteryLevel() {
        return batteryLevel;
    }

    public static float getCurrentBrightness() {
        return currentBrightness;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
