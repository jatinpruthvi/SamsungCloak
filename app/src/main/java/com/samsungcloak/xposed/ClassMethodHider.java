package com.samsungcloak.xposed;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class ClassMethodHider {
    private static final String LOG_TAG = "SamsungCloak.ClassMethodHider";
    private static boolean initialized = false;

    private static final String[] XPOSED_KEYWORDS = {
        "de.robv.android.xposed",
        "de.robv.android.xposed.",
        "com.lsposed.",
        "org.lsposed.",
        "handleHookedMethod",
        "beforeHookedMethod",
        "afterHookedMethod",
        "nativeHook",
        "artHook",
        "XposedBridge",
        "IXposedHook",
        "IXposedHookLoadPackage",
        "IXposedHookZygoteInit"
    };

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (initialized) {
            HookUtils.logDebug("ClassMethodHider already initialized");
            return;
        }

        try {
            hookClass(lpparam);
            initialized = true;
            HookUtils.logInfo("ClassMethodHider initialized successfully");
        } catch (Exception e) {
            HookUtils.logError("Failed to initialize ClassMethodHider: " + e.getMessage());
        }
    }

    private static void hookClass(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> classClass = XposedHelpers.findClass(
                "java.lang.Class", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(classClass, "getDeclaredMethods", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        Object[] methods = (Object[]) param.getResult();
                        if (methods != null) {
                            Object[] filteredMethods = filterMethods(methods);
                            if (filteredMethods.length != methods.length) {
                                param.setResult(filteredMethods);
                                HookUtils.logDebug("Class.getDeclaredMethods() filtered " + 
                                    (methods.length - filteredMethods.length) + " Xposed methods");
                            }
                        }
                    } catch (Exception e) {
                        HookUtils.logError("Error in getDeclaredMethods hook: " + e.getMessage());
                    }
                }
            });

            XposedBridge.hookAllMethods(classClass, "getDeclaredFields", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        Object[] fields = (Object[]) param.getResult();
                        if (fields != null) {
                            Object[] filteredFields = filterFields(fields);
                            if (filteredFields.length != fields.length) {
                                param.setResult(filteredFields);
                                HookUtils.logDebug("Class.getDeclaredFields() filtered " + 
                                    (fields.length - filteredFields.length) + " Xposed fields");
                            }
                        }
                    } catch (Exception e) {
                        HookUtils.logError("Error in getDeclaredFields hook: " + e.getMessage());
                    }
                }
            });

            HookUtils.logInfo("Hooked Class methods");
        } catch (Exception e) {
            HookUtils.logError("Failed to hook Class methods: " + e.getMessage());
        }
    }

    private static Object[] filterMethods(Object[] methods) {
        java.util.List<Object> filtered = new java.util.ArrayList<>();
        for (Object method : methods) {
            try {
                String methodName = (String) XposedHelpers.callMethod(method, "getName");
                boolean shouldHide = false;

                for (String keyword : XPOSED_KEYWORDS) {
                    if (methodName != null && methodName.contains(keyword)) {
                        shouldHide = true;
                        break;
                    }
                }

                if (!shouldHide) {
                    filtered.add(method);
                }
            } catch (Exception e) {
            }
        }
        return filtered.toArray();
    }

    private static Object[] filterFields(Object[] fields) {
        java.util.List<Object> filtered = new java.util.ArrayList<>();
        for (Object field : fields) {
            try {
                String fieldName = (String) XposedHelpers.callMethod(field, "getName");
                boolean shouldHide = false;

                for (String keyword : XPOSED_KEYWORDS) {
                    if (fieldName != null && fieldName.contains(keyword)) {
                        shouldHide = true;
                        break;
                    }
                }

                if (!shouldHide) {
                    filtered.add(field);
                }
            } catch (Exception e) {
            }
        }
        return filtered.toArray();
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
