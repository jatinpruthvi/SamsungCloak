package com.samsungcloak.xposed;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class ClipboardSecurityHook {
    private static final String LOG_TAG = "SamsungCloak.ClipboardSecurityHook";
    private static boolean initialized = false;

    private static long lastClipTime = 0;
    private static int clipChangeCount = 0;

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (initialized) {
            HookUtils.logDebug("ClipboardSecurityHook already initialized");
            return;
        }

        try {
            hookClipboardManager(lpparam);
            initialized = true;
            HookUtils.logInfo("ClipboardSecurityHook initialized successfully");
        } catch (Exception e) {
            HookUtils.logError("Failed to initialize ClipboardSecurityHook: " + e.getMessage());
        }
    }

    private static void hookClipboardManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> clipboardManagerClass = XposedHelpers.findClass(
                "android.content.ClipboardManager", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(clipboardManagerClass, "getPrimaryClip", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        Object clip = param.getResult();
                        if (clip != null) {
                            long currentTime = System.currentTimeMillis();
                            long timeDiff = currentTime - lastClipTime;

                            if (timeDiff > 100 && timeDiff < 5000) {
                                clipChangeCount++;
                            }
                        }

                        lastClipTime = currentTime;
                        HookUtils.logDebug("Clipboard get - clip changes: " + clipChangeCount);
                    } catch (Exception e) {
                        HookUtils.logError("Error in getPrimaryClip hook: " + e.getMessage());
                    }
                }
            });

            XposedBridge.hookAllMethods(clipboardManagerClass, "getText", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        Object clipData = param.getResult();
                        if (clipData instanceof String) {
                            String text = (String) clipData;
                            if (text != null && text.contains("com.android.internal")) {
                                param.setResult("");
                                HookUtils.logDebug("Clipboard getText - filtered internal data");
                            }
                        }
                    } catch (Exception e) {
                        HookUtils.logError("Error in getText hook: " + e.getMessage());
                    }
                }
            });

            HookUtils.logInfo("Hooked ClipboardManager methods");
        } catch (Exception e) {
            HookUtils.logError("Failed to hook clipboard manager: " + e.getMessage());
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
