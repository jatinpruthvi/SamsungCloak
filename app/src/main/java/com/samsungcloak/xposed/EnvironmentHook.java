package com.samsungcloak.xposed;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class EnvironmentHook {
    private static final String LOG_TAG = "SamsungCloak.EnvironmentHook";
    private static boolean initialized = false;

    private static final long sessionStartTime = System.currentTimeMillis();

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (initialized) {
            HookUtils.logDebug("EnvironmentHook already initialized");
            return;
        }

        try {
            hookBatteryIntent(lpparam);
            hookMemoryInfo(lpparam);
            hookRuntimeMemory(lpparam);
            hookDisplayMetrics(lpparam);
            hookInputDevice(lpparam);
            hookTelephonyManager(lpparam);

            initialized = true;
            HookUtils.logInfo("EnvironmentHook initialized successfully");
        } catch (Exception e) {
            HookUtils.logError("Failed to initialize EnvironmentHook: " + e.getMessage());
        }
    }

    private static void hookBatteryIntent(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> intentClass = XposedHelpers.findClass("android.content.Intent", lpparam.classLoader);

            XposedBridge.hookAllMethods(intentClass, "getIntExtra", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        if (param.args.length < 2) return;

                        Object intentObj = param.thisObject;
                        String action = (String) XposedHelpers.callMethod(intentObj, "getAction");

                        if ("android.intent.action.BATTERY_CHANGED".equals(action)) {
                            String key = (String) param.args[0];

                            if ("level".equals(key)) {
                                long elapsedTime = System.currentTimeMillis() - sessionStartTime;
                                int baseLevel = 85;
                                int drainMinutes = (int) (elapsedTime / (1000 * 60));
                                int currentLevel = Math.max(5, baseLevel - (drainMinutes / 3));
                                param.setResult(currentLevel);
                                HookUtils.logDebug("Battery level: " + currentLevel);
                            } else if ("scale".equals(key)) {
                                param.setResult(100);
                            } else if ("status".equals(key)) {
                                param.setResult(2);
                            } else if ("health".equals(key)) {
                                param.setResult(2);
                            } else if ("plugged".equals(key)) {
                                param.setResult(0);
                            }
                        }
                    } catch (Exception e) {
                        HookUtils.logError("Error in getIntExtra hook: " + e.getMessage());
                    }
                }
            });

            HookUtils.logInfo("Hooked Intent.getIntExtra() for battery");
        } catch (Exception e) {
            HookUtils.logError("Failed to hook battery intent: " + e.getMessage());
        }
    }

    private static void hookMemoryInfo(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> activityManagerClass = XposedHelpers.findClass("android.app.ActivityManager", lpparam.classLoader);

            XposedBridge.hookAllMethods(activityManagerClass, "getMemoryInfo", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        if (param.args.length == 0) return;

                        Object memoryInfo = param.args[0];
                        if (memoryInfo != null) {
                            XposedHelpers.setLongField(memoryInfo, "totalMem", DeviceConstants.TOTAL_MEM);
                            XposedHelpers.setLongField(memoryInfo, "availMem", DeviceConstants.TOTAL_MEM - (long)(DeviceConstants.TOTAL_MEM * 0.6));
                            XposedHelpers.setLongField(memoryInfo, "threshold", (long)(DeviceConstants.TOTAL_MEM * 0.1));
                            HookUtils.logDebug("Memory info modified");
                        }
                    } catch (Exception e) {
                        HookUtils.logError("Error in getMemoryInfo hook: " + e.getMessage());
                    }
                }
            });

            HookUtils.logInfo("Hooked ActivityManager.getMemoryInfo()");
        } catch (Exception e) {
            HookUtils.logError("Failed to hook memory info: " + e.getMessage());
        }
    }

    private static void hookRuntimeMemory(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedBridge.hookAllMethods(Runtime.class, "maxMemory", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(DeviceConstants.MAX_MEM * 1024L * 1024L);
                    HookUtils.logDebug("Runtime.maxMemory() -> " + DeviceConstants.MAX_MEM + " MB");
                }
            });

            XposedBridge.hookAllMethods(Runtime.class, "totalMemory", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    long totalMem = (long)(DeviceConstants.MAX_MEM * 0.8 * 1024L * 1024L);
                    param.setResult(totalMem);
                    HookUtils.logDebug("Runtime.totalMemory() -> " + (DeviceConstants.MAX_MEM * 0.8) + " MB");
                }
            });

            HookUtils.logInfo("Hooked Runtime memory methods");
        } catch (Exception e) {
            HookUtils.logError("Failed to hook runtime memory: " + e.getMessage());
        }
    }

    private static void hookDisplayMetrics(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> displayClass = XposedHelpers.findClass("android.view.Display", lpparam.classLoader);

            XposedBridge.hookAllMethods(displayClass, "getMetrics", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        if (param.args.length == 0) return;

                        Object metrics = param.args[0];
                        if (metrics != null) {
                            XposedHelpers.setIntField(metrics, "widthPixels", DeviceConstants.WIDTH_PIXELS);
                            XposedHelpers.setIntField(metrics, "heightPixels", DeviceConstants.HEIGHT_PIXELS);
                            XposedHelpers.setIntField(metrics, "densityDpi", DeviceConstants.DENSITY_DPI);
                            XposedHelpers.setFloatField(metrics, "xdpi", DeviceConstants.XDPI);
                            XposedHelpers.setFloatField(metrics, "ydpi", DeviceConstants.YDPI);
                            XposedHelpers.setFloatField(metrics, "density", DeviceConstants.DENSITY);
                            XposedHelpers.setIntField(metrics, "scaledDensity", DeviceConstants.SCALED_DENSITY);
                            HookUtils.logDebug("DisplayMetrics modified");
                        }
                    } catch (Exception e) {
                        HookUtils.logError("Error in getMetrics hook: " + e.getMessage());
                    }
                }
            });

            XposedBridge.hookAllMethods(displayClass, "getRealMetrics", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        if (param.args.length == 0) return;

                        Object metrics = param.args[0];
                        if (metrics != null) {
                            XposedHelpers.setIntField(metrics, "widthPixels", DeviceConstants.WIDTH_PIXELS);
                            XposedHelpers.setIntField(metrics, "heightPixels", DeviceConstants.HEIGHT_PIXELS);
                            XposedHelpers.setIntField(metrics, "densityDpi", DeviceConstants.DENSITY_DPI);
                            XposedHelpers.setFloatField(metrics, "xdpi", DeviceConstants.XDPI);
                            XposedHelpers.setFloatField(metrics, "ydpi", DeviceConstants.YDPI);
                            XposedHelpers.setFloatField(metrics, "density", DeviceConstants.DENSITY);
                            XposedHelpers.setIntField(metrics, "scaledDensity", DeviceConstants.SCALED_DENSITY);
                            HookUtils.logDebug("RealDisplayMetrics modified");
                        }
                    } catch (Exception e) {
                        HookUtils.logError("Error in getRealMetrics hook: " + e.getMessage());
                    }
                }
            });

            HookUtils.logInfo("Hooked Display metrics methods");
        } catch (Exception e) {
            HookUtils.logError("Failed to hook display metrics: " + e.getMessage());
        }
    }

    private static void hookInputDevice(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> inputDeviceClass = XposedHelpers.findClass("android.view.InputDevice", lpparam.classLoader);

            XposedBridge.hookAllMethods(inputDeviceClass, "getName", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult("sec_touchscreen");
                    HookUtils.logDebug("InputDevice.getName() -> sec_touchscreen");
                }
            });

            XposedBridge.hookAllMethods(inputDeviceClass, "getSources", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    int sourceTouchscreen = XposedHelpers.getStaticIntField(inputDeviceClass, "SOURCE_TOUCHSCREEN");
                    int sourceKeyboard = XposedHelpers.getStaticIntField(inputDeviceClass, "SOURCE_KEYBOARD");
                    param.setResult(sourceTouchscreen | sourceKeyboard);
                    HookUtils.logDebug("InputDevice.getSources() modified");
                }
            });

            XposedBridge.hookAllMethods(inputDeviceClass, "getVendorId", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(1449);
                    HookUtils.logDebug("InputDevice.getVendorId() -> 1449");
                }
            });

            XposedBridge.hookAllMethods(inputDeviceClass, "getProductId", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(5747);
                    HookUtils.logDebug("InputDevice.getProductId() -> 5747");
                }
            });

            XposedBridge.hookAllMethods(inputDeviceClass, "getDescriptor", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult("1449:5747");
                    HookUtils.logDebug("InputDevice.getDescriptor() -> 1449:5747");
                }
            });

            HookUtils.logInfo("Hooked InputDevice methods");
        } catch (Exception e) {
            HookUtils.logError("Failed to hook input device: " + e.getMessage());
        }
    }

    private static void hookTelephonyManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> telephonyManagerClass = XposedHelpers.findClass(
                "android.telephony.TelephonyManager", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(telephonyManagerClass, "getNetworkOperator", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(DeviceConstants.NETWORK_OPERATOR);
                    HookUtils.logDebug("TelephonyManager.getNetworkOperator() -> " + DeviceConstants.NETWORK_OPERATOR);
                }
            });

            XposedBridge.hookAllMethods(telephonyManagerClass, "getNetworkOperatorName", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(DeviceConstants.NETWORK_OPERATOR_NAME);
                    HookUtils.logDebug("TelephonyManager.getNetworkOperatorName() -> " + DeviceConstants.NETWORK_OPERATOR_NAME);
                }
            });

            XposedBridge.hookAllMethods(telephonyManagerClass, "getNetworkCountryIso", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(DeviceConstants.NETWORK_COUNTRY_ISO);
                    HookUtils.logDebug("TelephonyManager.getNetworkCountryIso() -> " + DeviceConstants.NETWORK_COUNTRY_ISO);
                }
            });

            XposedBridge.hookAllMethods(telephonyManagerClass, "getSimOperator", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(DeviceConstants.SIM_OPERATOR);
                    HookUtils.logDebug("TelephonyManager.getSimOperator() -> " + DeviceConstants.SIM_OPERATOR);
                }
            });

            XposedBridge.hookAllMethods(telephonyManagerClass, "getSimOperatorName", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(DeviceConstants.SIM_OPERATOR_NAME);
                    HookUtils.logDebug("TelephonyManager.getSimOperatorName() -> " + DeviceConstants.SIM_OPERATOR_NAME);
                }
            });

            XposedBridge.hookAllMethods(telephonyManagerClass, "getSimCountryIso", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(DeviceConstants.SIM_COUNTRY_ISO);
                    HookUtils.logDebug("TelephonyManager.getSimCountryIso() -> " + DeviceConstants.SIM_COUNTRY_ISO);
                }
            });

            HookUtils.logInfo("Hooked TelephonyManager methods");
        } catch (Exception e) {
            HookUtils.logError("Failed to hook telephony manager: " + e.getMessage());
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
