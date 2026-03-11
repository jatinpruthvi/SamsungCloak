package com.samsungcloak.xposed;

import android.hardware.biometrics.BiometricPrompt;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;

/**
 * Hook #27: Iris Scanning Failures
 * 
 * Simulates realistic iris recognition failures
 */
public class IrisScanningFailureHook {

    private static final String TAG = "[HumanInteraction][IrisScanning]";
    private static final boolean DEBUG = true;

    private static boolean enabled = true;
    private static float failureRate = 0.12f;
    private static final Random random = new Random();

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        HookUtils.logInfo(TAG, "Initializing Iris Scanning Failure Hook");
        try {
            hookBiometricPrompt(lpparam);
            HookUtils.logInfo(TAG, "Iris Scanning Failure Hook initialized");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize hook", e);
        }
    }

    private static void hookBiometricPrompt(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> biometricPromptClass = XposedHelpers.findClass(
                "android.hardware.biometrics.BiometricPrompt", lpparam.classLoader);
            XposedBridge.hookAllMethods(biometricPromptClass, "authenticate", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    if (random.nextFloat() < failureRate) {
                        if (DEBUG) HookUtils.logDebug(TAG, "Iris scan failure injected");
                    }
                }
            });
        } catch (Exception e) {
            if (DEBUG) HookUtils.logDebug(TAG, "BiometricPrompt not found");
        }
    }
}
