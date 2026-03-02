package com.samsungcloak.xposed;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class BiometricSpoofHook {
    private static final String LOG_TAG = "SamsungCloak.BiometricSpoofHook";
    private static boolean initialized = false;

    public static final int BIOMETRIC_SUCCESS = 0;
    public static final int BIOMETRIC_ERROR_NO_HARDWARE = 1;
    public static final int BIOMETRIC_ERROR_HW_UNAVAILABLE = 2;
    public static final int BIOMETRIC_ERROR_LOCKOUT = 3;

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (initialized) {
            HookUtils.logDebug("BiometricSpoofHook already initialized");
            return;
        }

        try {
            hookBiometricManager(lpparam);
            hookFingerprintManager(lpparam);
            initialized = true;
            HookUtils.logInfo("BiometricSpoofHook initialized successfully");
        } catch (Exception e) {
            HookUtils.logError("Failed to initialize BiometricSpoofHook: " + e.getMessage());
        }
    }

    private static void hookBiometricManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> biometricManagerClass = XposedHelpers.findClass(
                "android.hardware.biometrics.BiometricManager", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(biometricManagerClass, "canAuthenticate", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        if (param.args.length >= 2) {
                            Object callback = param.args[1];
                            if (callback != null) {
                                XposedHelpers.callMethod(callback, "onAuthenticationError", 
                                    BIOMETRIC_ERROR_NO_HARDWARE);
                                HookUtils.logDebug("BiometricManager.canAuthenticate() -> ERROR_NO_HARDWARE");
                                param.setResult(false);
                            }
                        }
                    } catch (Exception e) {
                        HookUtils.logError("Error in canAuthenticate hook: " + e.getMessage());
                    }
                }
            });

            XposedBridge.hookAllMethods(biometricManagerClass, "authenticate", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        if (param.args.length >= 2) {
                            Object callback = param.args[1];
                            if (callback != null) {
                                XposedHelpers.callMethod(callback, "onAuthenticationSucceeded",
                                    null, null);
                                HookUtils.logDebug("BiometricManager.authenticate() -> SUCCESS");
                                param.setResult(BIOMETRIC_SUCCESS);
                            }
                        }
                    } catch (Exception e) {
                        HookUtils.logError("Error in authenticate hook: " + e.getMessage());
                    }
                }
            });

            HookUtils.logInfo("Hooked BiometricManager methods");
        } catch (Exception e) {
            HookUtils.logError("Failed to hook biometric manager: " + e.getMessage());
        }
    }

    private static void hookFingerprintManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> fingerprintManagerClass = XposedHelpers.findClass(
                "android.hardware.fingerprint.FingerprintManager", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(fingerprintManagerClass, "hasEnrolledFingerprints", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        param.setResult(true);
                        HookUtils.logDebug("FingerprintManager.hasEnrolledFingerprints() -> true");
                    } catch (Exception e) {
                        HookUtils.logError("Error in hasEnrolledFingerprints hook: " + e.getMessage());
                    }
                }
            });

            HookUtils.logInfo("Hooked FingerprintManager methods");
        } catch (Exception e) {
            HookUtils.logError("Failed to hook fingerprint manager: " + e.getMessage());
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
