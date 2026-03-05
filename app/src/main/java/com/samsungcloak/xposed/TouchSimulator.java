package com.samsungcloak.xposed;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class TouchSimulator {
    private static final String LOG_TAG = "SamsungCloak.TouchSimulator";
    private static boolean initialized = false;

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (initialized) {
            HookUtils.logDebug("TouchSimulator already initialized");
            return;
        }

        try {
            hookMotionEvent(lpparam);
            initialized = true;
            HookUtils.logInfo("TouchSimulator initialized successfully");
        } catch (Exception e) {
            HookUtils.logError("Failed to initialize TouchSimulator: " + e.getMessage());
        }
    }

    private static void hookMotionEvent(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> motionEventClass = XposedHelpers.findClass(
                "android.view.MotionEvent", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(motionEventClass, "getPressure", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        float originalPressure = (float) param.getResult();
                        float humanPressure = 0.5f + (HookUtils.generateGaussianNoise(0.1f));
                        param.setResult(HookUtils.clamp(humanPressure, 0.0f, 1.0f));
                        HookUtils.logDebug("MotionEvent.getPressure() -> " + humanPressure + " (was: " + originalPressure + ")");
                    } catch (Exception e) {
                        HookUtils.logError("Error in getPressure hook: " + e.getMessage());
                    }
                }
            });

            XposedBridge.hookAllMethods(motionEventClass, "getSize", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        float originalSize = (float) param.getResult();
                        float humanSize = 0.8f + (HookUtils.generateGaussianNoise(0.05f));
                        param.setResult(HookUtils.clamp(humanSize, 0.3f, 1.5f));
                        HookUtils.logDebug("MotionEvent.getSize() -> " + humanSize + " (was: " + originalSize + ")");
                    } catch (Exception e) {
                        HookUtils.logError("Error in getSize hook: " + e.getMessage());
                    }
                }
            });

            XposedBridge.hookAllMethods(motionEventClass, "getTouchMajor", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        float originalMajor = (float) param.getResult();
                        float humanMajor = 0.6f + (HookUtils.generateGaussianNoise(0.04f));
                        param.setResult(HookUtils.clamp(humanMajor, 0.2f, 1.3f));
                        HookUtils.logDebug("MotionEvent.getTouchMajor() -> " + humanMajor);
                    } catch (Exception e) {
                        HookUtils.logError("Error in getTouchMajor hook: " + e.getMessage());
                    }
                }
            });

            XposedBridge.hookAllMethods(motionEventClass, "getTouchMinor", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        float originalMinor = (float) param.getResult();
                        float humanMinor = 0.5f + (HookUtils.generateGaussianNoise(0.03f));
                        param.setResult(HookUtils.clamp(humanMinor, 0.2f, 1.0f));
                        HookUtils.logDebug("MotionEvent.getTouchMinor() -> " + humanMinor);
                    } catch (Exception e) {
                        HookUtils.logError("Error in getTouchMinor hook: " + e.getMessage());
                    }
                }
            });

            HookUtils.logInfo("Hooked MotionEvent methods");
        } catch (Exception e) {
            HookUtils.logError("Failed to hook MotionEvent: " + e.getMessage());
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
