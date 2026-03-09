package com.samsungcloak.xposed;

import android.content.Context;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_LoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class HumanInteractionModule implements IXposedHookLoadPackage {

    private static final String TAG = "[HumanInteraction][Main]";

    private static final String[] TARGET_PACKAGES = {
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill",
        "com.ss.android.ugc.aweme"
    };

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (!isTargetPackage(lpparam.packageName)) {
            return;
        }

        logInfo("Human Interaction Module activated for: " + lpparam.packageName);

        try {
            MechanicalMicroErrorHook.init(lpparam);
            SensorFusionCoherenceHook.init(lpparam);
            InterAppNavigationHook.init(lpparam);
            InputPressureDynamicsHook.init(lpparam);
            AsymmetricLatencyHook.init(lpparam);

            logInfo("All hooks initialized successfully");

            logConfiguration();

        } catch (Exception e) {
            logError("Failed to initialize hooks", e);
        }
    }

    private boolean isTargetPackage(String packageName) {
        for (String target : TARGET_PACKAGES) {
            if (target.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    private void logInfo(String message) {
        XposedBridge.log(TAG + " " + message);
    }

    private void logError(String message, Throwable throwable) {
        XposedBridge.log(TAG + " [ERROR] " + message);
        if (throwable != null) {
            XposedBridge.log(throwable);
        }
    }

    private void logConfiguration() {
        logInfo("=== Human Interaction Module Configuration ===");
        logInfo("Target Device: Samsung Galaxy A12 (SM-A125U)");
        logInfo("Target OS: Android 10/11");
        logInfo("");
        logInfo("Active Hooks:");
        logInfo("  1. Mechanical Micro-Error Hook - Simulates fat-finger errors");
        logInfo("  2. Sensor-Fusion Coherence Hook - Walking dynamics injection");
        logInfo("  3. Inter-App Navigation Hook - Referral flow simulation");
        logInfo("  4. Input Pressure Dynamics Hook - Touch pressure/area variation");
        logInfo("  5. Asymmetric Latency Hook - Processing hesitation simulation");
        logInfo("============================================");
    }

    public static class ConfigurationAPI {

        public static void enableAllHooks() {
            MechanicalMicroErrorHook.setEnabled(true);
            SensorFusionCoherenceHook.setEnabled(true);
            InterAppNavigationHook.setEnabled(true);
            InputPressureDynamicsHook.setEnabled(true);
            AsymmetricLatencyHook.setEnabled(true);

            HookUtils.logInfo("[API]", "All hooks enabled");
        }

        public static void disableAllHooks() {
            MechanicalMicroErrorHook.setEnabled(false);
            SensorFusionCoherenceHook.setEnabled(false);
            InterAppNavigationHook.setEnabled(false);
            InputPressureDynamicsHook.setEnabled(false);
            AsymmetricLatencyHook.setEnabled(false);

            HookUtils.logInfo("[API]", "All hooks disabled");
        }

        public static void setMechanicalErrorRate(double rate) {
            MechanicalMicroErrorHook.setErrorRate(rate);
        }

        public static void setSensorFusionWalkingState(boolean isWalking, double speed) {
            SensorFusionCoherenceHook.simulateWalkingState(isWalking, speed);
        }

        public static void setReferralFlowProbability(double prob) {
            InterAppNavigationHook.setReferralFlowProbability(prob);
        }

        public static void setTouchPressureVariation(double prob) {
            InputPressureDynamicsHook.setPressureVariationProbability(prob);
        }

        public static void setHesitationProbability(double prob) {
            AsymmetricLatencyHook.setHesitationProbability(prob);
        }

        public static void setHighFidelityMode(boolean enabled) {
            if (enabled) {
                MechanicalMicroErrorHook.setErrorRate(0.10);
                SensorFusionCoherenceHook.setWalkingIndicationProbability(0.30);
                InterAppNavigationHook.setReferralFlowProbability(0.20);
                InputPressureDynamicsHook.setPressureVariationProbability(0.30);
                InputPressureDynamicsHook.setSurfaceAreaVariationProbability(0.28);
                AsymmetricLatencyHook.setHesitationProbability(0.35);

                HookUtils.logInfo("[API]", "High-fidelity mode enabled");
            } else {
                MechanicalMicroErrorHook.setErrorRate(0.05);
                SensorFusionCoherenceHook.setWalkingIndicationProbability(0.15);
                InterAppNavigationHook.setReferralFlowProbability(0.10);
                InputPressureDynamicsHook.setPressureVariationProbability(0.15);
                InputPressureDynamicsHook.setSurfaceAreaVariationProbability(0.12);
                AsymmetricLatencyHook.setHesitationProbability(0.20);

                HookUtils.logInfo("[API]", "Standard mode enabled");
            }
        }

        public static void setMinimalInterferenceMode() {
            MechanicalMicroErrorHook.setErrorRate(0.02);
            SensorFusionCoherenceHook.setWalkingIndicationProbability(0.05);
            InterAppNavigationHook.setReferralFlowProbability(0.03);
            InputPressureDynamicsHook.setPressureVariationProbability(0.05);
            InputPressureDynamicsHook.setSurfaceAreaVariationProbability(0.05);
            AsymmetricLatencyHook.setHesitationProbability(0.08);

            HookUtils.logInfo("[API]", "Minimal interference mode enabled");
        }
    }
}
