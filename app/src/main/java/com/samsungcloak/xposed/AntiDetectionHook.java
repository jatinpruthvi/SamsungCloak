package com.samsungcloak.xposed;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class AntiDetectionHook {
    private static final String LOG_TAG = "SamsungCloak.AntiDetectionHook";
    private static boolean initialized = false;

    private static final String[] FRAMEWORK_KEYWORDS = {
        "xposed", "lsposed", "magisk", "riru", "edxp", "tai"
    };

    private static final String[] FRAMEWORK_FILES = {
        "/system/app/LSPosed",
        "/system/priv-app/LSPosed",
        "/system/bin/app_process32_xposed",
        "/system/bin/app_process64_xposed",
        "/system/framework/XposedBridge.jar",
        "/data/adb/modules",
        "/data/adb/magisk",
        "/cache/.disable_magisk",
        "/dev/.magisk.unblock",
        "/magisk",
        "/magisk.img",
        "/magisk/.core"
    };

    private static final String[] FRAMEWORK_PACKAGES = {
        "de.robv.android.xposed.installer",
        "org.meowcat.edxposed.manager",
        "com.solohsu.android.edxp.manager",
        "com.tsng.hidemyapplist",
        "com.topjohnwu.magisk",
        "io.github.lsposed.manager"
    };

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (initialized) {
            HookUtils.logDebug("AntiDetectionHook already initialized");
            return;
        }

        try {
            hookStackTrace(lpparam);
            hookFileExists(lpparam);
            hookPackageManager(lpparam);

            initialized = true;
            HookUtils.logInfo("AntiDetectionHook initialized successfully");
        } catch (Exception e) {
            HookUtils.logError("Failed to initialize AntiDetectionHook: " + e.getMessage());
        }
    }

    private static void hookStackTrace(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedBridge.hookAllMethods(Throwable.class, "getStackTrace", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        StackTraceElement[] originalStack = (StackTraceElement[]) param.getResult();
                        if (originalStack == null) return;

                        List<StackTraceElement> filteredStack = new ArrayList<>();
                        for (StackTraceElement element : originalStack) {
                            String className = element.getClassName();
                            boolean shouldHide = false;

                            for (String keyword : FRAMEWORK_KEYWORDS) {
                                if (className.toLowerCase().contains(keyword.toLowerCase())) {
                                    shouldHide = true;
                                    break;
                                }
                            }

                            if (!shouldHide) {
                                filteredStack.add(element);
                            }
                        }

                        if (filteredStack.size() != originalStack.length) {
                            param.setResult(filteredStack.toArray(new StackTraceElement[0]));
                            HookUtils.logDebug("Filtered " + (originalStack.length - filteredStack.size()) + " stack trace elements");
                        }
                    } catch (Exception e) {
                        HookUtils.logError("Error in getStackTrace hook: " + e.getMessage());
                    }
                }
            });

            XposedBridge.hookAllMethods(Thread.class, "getStackTrace", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        StackTraceElement[] originalStack = (StackTraceElement[]) param.getResult();
                        if (originalStack == null) return;

                        List<StackTraceElement> filteredStack = new ArrayList<>();
                        for (StackTraceElement element : originalStack) {
                            String className = element.getClassName();
                            boolean shouldHide = false;

                            for (String keyword : FRAMEWORK_KEYWORDS) {
                                if (className.toLowerCase().contains(keyword.toLowerCase())) {
                                    shouldHide = true;
                                    break;
                                }
                            }

                            if (!shouldHide) {
                                filteredStack.add(element);
                            }
                        }

                        if (filteredStack.size() != originalStack.length) {
                            param.setResult(filteredStack.toArray(new StackTraceElement[0]));
                        }
                    } catch (Exception e) {
                        HookUtils.logError("Error in Thread.getStackTrace hook: " + e.getMessage());
                    }
                }
            });

            HookUtils.logInfo("Hooked stack trace cleaning");
        } catch (Exception e) {
            HookUtils.logError("Failed to hook stack trace: " + e.getMessage());
        }
    }

    private static void hookFileExists(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> fileClass = XposedHelpers.findClass("java.io.File", lpparam.classLoader);

            XposedBridge.hookAllMethods(fileClass, "exists", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        Object fileObj = param.thisObject;
                        String path = (String) XposedHelpers.callMethod(fileObj, "getAbsolutePath");

                        for (String frameworkFile : FRAMEWORK_FILES) {
                            if (path != null && path.startsWith(frameworkFile)) {
                                param.setResult(false);
                                HookUtils.logDebug("File.exists() blocked for: " + path);
                                return;
                            }
                        }
                    } catch (Exception e) {
                        HookUtils.logError("Error in exists() hook: " + e.getMessage());
                    }
                }
            });

            XposedBridge.hookAllMethods(fileClass, "isFile", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        Object fileObj = param.thisObject;
                        String path = (String) XposedHelpers.callMethod(fileObj, "getAbsolutePath");

                        for (String frameworkFile : FRAMEWORK_FILES) {
                            if (path != null && path.startsWith(frameworkFile)) {
                                param.setResult(false);
                                return;
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            });

            XposedBridge.hookAllMethods(fileClass, "isDirectory", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        Object fileObj = param.thisObject;
                        String path = (String) XposedHelpers.callMethod(fileObj, "getAbsolutePath");

                        for (String frameworkFile : FRAMEWORK_FILES) {
                            if (path != null && path.startsWith(frameworkFile)) {
                                param.setResult(false);
                                return;
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            });

            XposedBridge.hookAllMethods(fileClass, "length", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        Object fileObj = param.thisObject;
                        String path = (String) XposedHelpers.callMethod(fileObj, "getAbsolutePath");

                        for (String frameworkFile : FRAMEWORK_FILES) {
                            if (path != null && path.startsWith(frameworkFile)) {
                                param.setResult(0L);
                                return;
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            });

            XposedBridge.hookAllMethods(fileClass, "canRead", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        Object fileObj = param.thisObject;
                        String path = (String) XposedHelpers.callMethod(fileObj, "getAbsolutePath");

                        for (String frameworkFile : FRAMEWORK_FILES) {
                            if (path != null && path.startsWith(frameworkFile)) {
                                param.setResult(false);
                                return;
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            });

            HookUtils.logInfo("Hooked File methods for framework hiding");
        } catch (Exception e) {
            HookUtils.logError("Failed to hook file exists: " + e.getMessage());
        }
    }

    private static void hookPackageManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> pmClass = XposedHelpers.findClass("android.content.pm.PackageManager", lpparam.classLoader);

            XposedBridge.hookAllMethods(pmClass, "getInstalledPackages", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        Object packagesList = param.getResult();
                        if (packagesList == null) return;

                        List<?> packages = (List<?>) packagesList;
                        List<Object> filteredPackages = new ArrayList<>();

                        for (Object pkg : packages) {
                            String packageName = (String) XposedHelpers.callMethod(pkg, "getPackageName");

                            boolean isFrameworkPackage = false;
                            for (String frameworkPkg : FRAMEWORK_PACKAGES) {
                                if (frameworkPkg.equalsIgnoreCase(packageName)) {
                                    isFrameworkPackage = true;
                                    break;
                                }
                            }

                            if (!isFrameworkPackage) {
                                filteredPackages.add(pkg);
                            }
                        }

                        if (filteredPackages.size() != packages.size()) {
                            param.setResult(filteredPackages);
                            HookUtils.logDebug("Filtered " + (packages.size() - filteredPackages.size()) + " packages");
                        }
                    } catch (Exception e) {
                        HookUtils.logError("Error in getInstalledPackages hook: " + e.getMessage());
                    }
                }
            });

            XposedBridge.hookAllMethods(pmClass, "getInstalledApplications", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        Object appsList = param.getResult();
                        if (appsList == null) return;

                        List<?> apps = (List<?>) appsList;
                        List<Object> filteredApps = new ArrayList<>();

                        for (Object app : apps) {
                            String packageName = (String) XposedHelpers.callMethod(
                                XposedHelpers.callMethod(app, "getPackageName"), null
                            );

                            boolean isFrameworkPackage = false;
                            for (String frameworkPkg : FRAMEWORK_PACKAGES) {
                                if (frameworkPkg.equalsIgnoreCase(packageName)) {
                                    isFrameworkPackage = true;
                                    break;
                                }
                            }

                            if (!isFrameworkPackage) {
                                filteredApps.add(app);
                            }
                        }

                        if (filteredApps.size() != apps.size()) {
                            param.setResult(filteredApps);
                        }
                    } catch (Exception e) {
                        HookUtils.logError("Error in getInstalledApplications hook: " + e.getMessage());
                    }
                }
            });

            HookUtils.logInfo("Hooked PackageManager methods");
        } catch (Exception e) {
            HookUtils.logError("Failed to hook package manager: " + e.getMessage());
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
