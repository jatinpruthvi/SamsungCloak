package com.samsungcloak.xposed;

import java.util.HashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class PropertyHook {
    private static final String LOG_TAG = "SamsungCloak.PropertyHook";
    private static boolean initialized = false;

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (initialized) {
            HookUtils.logDebug("PropertyHook already initialized");
            return;
        }

        try {
            Class<?> systemPropertiesClass = XposedHelpers.findClass("android.os.SystemProperties", lpparam.classLoader);

            hookSystemPropertiesGet(systemPropertiesClass);
            hookSystemPropertiesGetInt(systemPropertiesClass);
            hookSystemPropertiesGetLong(systemPropertiesClass);
            hookSystemPropertiesGetBoolean(systemPropertiesClass);

            initialized = true;
            HookUtils.logInfo("PropertyHook initialized successfully");
        } catch (Exception e) {
            HookUtils.logError("Failed to initialize PropertyHook: " + e.getMessage());
        }
    }

    private static void hookSystemPropertiesGet(Class<?> systemPropertiesClass) {
        try {
            XposedBridge.hookAllMethods(systemPropertiesClass, "get", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        if (param.args.length == 0) return;

                        String key = (String) param.args[0];
                        HashMap<String, String> properties = DeviceConstants.getSystemProperties();

                        if (properties.containsKey(key)) {
                            String spoofedValue = properties.get(key);
                            param.setResult(spoofedValue);
                            HookUtils.logDebug("Property get(" + key + ") -> " + spoofedValue);
                        }
                    } catch (Exception e) {
                        HookUtils.logError("Error in get() hook: " + e.getMessage());
                    }
                }
            });

            HookUtils.logInfo("Hooked SystemProperties.get() methods");
        } catch (Exception e) {
            HookUtils.logError("Failed to hook SystemProperties.get(): " + e.getMessage());
        }
    }

    private static void hookSystemPropertiesGetInt(Class<?> systemPropertiesClass) {
        try {
            XposedBridge.hookAllMethods(systemPropertiesClass, "getInt", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        if (param.args.length == 0) return;

                        String key = (String) param.args[0];
                        HashMap<String, String> properties = DeviceConstants.getSystemProperties();

                        if (properties.containsKey(key)) {
                            String value = properties.get(key);
                            try {
                                int intValue = Integer.parseInt(value);
                                param.setResult(intValue);
                                HookUtils.logDebug("Property getInt(" + key + ") -> " + intValue);
                            } catch (NumberFormatException e) {
                            }
                        }
                    } catch (Exception e) {
                        HookUtils.logError("Error in getInt() hook: " + e.getMessage());
                    }
                }
            });

            HookUtils.logInfo("Hooked SystemProperties.getInt() methods");
        } catch (Exception e) {
            HookUtils.logError("Failed to hook SystemProperties.getInt(): " + e.getMessage());
        }
    }

    private static void hookSystemPropertiesGetLong(Class<?> systemPropertiesClass) {
        try {
            XposedBridge.hookAllMethods(systemPropertiesClass, "getLong", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        if (param.args.length == 0) return;

                        String key = (String) param.args[0];
                        HashMap<String, String> properties = DeviceConstants.getSystemProperties();

                        if (properties.containsKey(key)) {
                            String value = properties.get(key);
                            try {
                                long longValue = Long.parseLong(value);
                                param.setResult(longValue);
                                HookUtils.logDebug("Property getLong(" + key + ") -> " + longValue);
                            } catch (NumberFormatException e) {
                            }
                        }
                    } catch (Exception e) {
                        HookUtils.logError("Error in getLong() hook: " + e.getMessage());
                    }
                }
            });

            HookUtils.logInfo("Hooked SystemProperties.getLong() methods");
        } catch (Exception e) {
            HookUtils.logError("Failed to hook SystemProperties.getLong(): " + e.getMessage());
        }
    }

    private static void hookSystemPropertiesGetBoolean(Class<?> systemPropertiesClass) {
        try {
            XposedBridge.hookAllMethods(systemPropertiesClass, "getBoolean", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        if (param.args.length == 0) return;

                        String key = (String) param.args[0];
                        HashMap<String, String> properties = DeviceConstants.getSystemProperties();

                        if (properties.containsKey(key)) {
                            String value = properties.get(key);
                            boolean boolValue = "1".equals(value) || "true".equalsIgnoreCase(value);
                            param.setResult(boolValue);
                            HookUtils.logDebug("Property getBoolean(" + key + ") -> " + boolValue);
                        }
                    } catch (Exception e) {
                        HookUtils.logError("Error in getBoolean() hook: " + e.getMessage());
                    }
                }
            });

            HookUtils.logInfo("Hooked SystemProperties.getBoolean() methods");
        } catch (Exception e) {
            HookUtils.logError("Failed to hook SystemProperties.getBoolean(): " + e.getMessage());
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
