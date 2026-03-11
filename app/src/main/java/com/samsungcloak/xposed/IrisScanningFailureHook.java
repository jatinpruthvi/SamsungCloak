package com.samsungcloak.xposed;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;

/**
 * Hook #35: IrisScanningFailureHook - Biometric iris recognition failures
 */
public class IrisScanningFailureHook {
    private static final String TAG = "[HumanInteraction][IrisScanning]";
    private static boolean enabled = true;
    private static float failureRate = 0.12f;
    private static final Random random = new Random();

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        HookUtils.logInfo(TAG, "Initializing Iris Scanning Failure Hook");
        try {
            Class<?> biometricPromptClass = XposedHelpers.findClass(
                "android.hardware.biometrics.BiometricPrompt", lpparam.classLoader);
            XposedBridge.hookAllMethods(biometricPromptClass, "authenticate", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    if (random.nextFloat() < failureRate) {
                        HookUtils.logDebug(TAG, "Iris scan failure injected");
                    }
                }
            });
            HookUtils.logInfo(TAG, "Iris Scanning Failure Hook initialized");
        } catch (Exception e) {
            HookUtils.logDebug(TAG, "BiometricPrompt not available");
        }
    }
}
