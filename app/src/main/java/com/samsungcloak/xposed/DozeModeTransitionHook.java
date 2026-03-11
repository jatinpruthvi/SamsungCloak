package com.samsungcloak.xposed;

import android.os.PowerManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;

/**
 * Hook #30: Doze Mode Transitions
 * 
 * Simulates realistic power state transitions
 */
public class DozeModeTransitionHook {

    private static final String TAG = "[Power][DozeMode]";
    private static final boolean DEBUG = true;

    private static boolean enabled = true;
    private static float dozeReadiness = 0.5f;
    private static final Random random = new Random();
    
    private static boolean isScreenOn = true;
    private static boolean isDozing = false;
    private static long lastActivityTime = 0;

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        HookUtils.logInfo(TAG, "Initializing Doze Mode Transition Hook");
        try {
            hookPowerManager(lpparam);
            HookUtils.logInfo(TAG, "Doze Mode Transition Hook initialized");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize hook", e);
        }
    }

    private static void hookPowerManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> powerManagerClass = XposedHelpers.findClass("android.os.PowerManager", lpparam.classLoader);
            XposedBridge.hookAllMethods(powerManagerClass, "isScreenOn", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    boolean screenOn = (boolean) param.getResult();
                    isScreenOn = screenOn;
                    if (screenOn) {
                        lastActivityTime = System.currentTimeMillis();
                        isDozing = false;
                    } else if (random.nextFloat() < dozeReadiness * 0.3f) {
                        isDozing = true;
                        if (DEBUG && random.nextFloat() < 0.01f) {
                            HookUtils.logDebug(TAG, "Entering doze mode");
                        }
                    }
                }
            });
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook PowerManager", e);
        }
    }
}
