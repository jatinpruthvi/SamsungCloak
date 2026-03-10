package com.samsungcloak.realism;

import android.content.SharedPreferences;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_LoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

/**
 * SamsungCloakRealismModule - Main entry point for realism injection hooks
 *
 * Targets: Samsung Galaxy A12 (SM-A125U) Android 10/11
 *
 * This module extends the core device spoofing framework with novel realism hooks
 * that address human-behavioral and environmental fidelity dimensions not covered
 * by the existing 12 hooks.
 *
 * New Hooks Implemented:
 * 1. HapticFeedbackRealismHook - Motor inertia, intensity variation, thermal degradation
 * 2. NotificationInterruptionHook - Social interruptions, time-of-day patterns
 * 3. BiometricRealismHook - Fingerprint/face/iris failure simulation
 * 4. DeviceGripAndOrientationHook - One/two-handed use, tilt during walking
 * 5. PowerStateManagementHook - Doze mode, standby buckets, charging behavior
 * 6. AudioEnvironmentRealismHook - Acoustic environment, noise simulation, adaptive volume
 * 7. GPSLocationTrajectoryHook - GPS physics, trajectory generation, TTFF simulation
 *
 * Integration:
 * All hooks maintain cross-hook coherence through a shared state manager
 */
public class SamsungCloakRealismModule implements IXposedHookLoadPackage {

    private static final String TAG = "SamsungCloakRealism";
    private static final String MODULE_PACKAGE = "com.samsungcloak.realism";

    private static SharedPreferences sPrefs;
    private static RealismStateManager sStateManager;

    @Override
    public void init(SharedPreferences prefs) {
        sPrefs = prefs;

        // Initialize state manager
        sStateManager = RealismStateManager.getInstance();
        sStateManager.initialize(prefs);

        // Initialize all hooks
        HapticFeedbackRealismHook.init(prefs);
        NotificationInterruptionHook.init(prefs);
        BiometricRealismHook.init(prefs);
        DeviceGripAndOrientationHook.init(prefs);
        PowerStateManagementHook.init(prefs);
        AudioEnvironmentRealismHook.init(prefs);
        GPSLocationTrajectoryHook.init(prefs);

        XposedBridge.log(TAG + ": Module initialized with 7 realism hooks");
    }

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals(MODULE_PACKAGE)) {
            // Skip hooking ourselves
            return;
        }

        String processName = lpparam.processName;

        // Target specific packages (TikTok variants and system services)
        if (!isTargetPackage(lpparam.packageName)) {
            return;
        }

        XposedBridge.log(TAG + ": Loading hooks for: " + lpparam.packageName);

        // Haptic Feedback Hook
        HapticFeedbackRealismHook.hookVibrator(lpparam);
        HapticFeedbackRealismHook.hookVibratorManager(lpparam);
        HapticFeedbackRealismHook.hookVibrationEffect(lpparam);
        HapticFeedbackRealismHook.hookViewHapticFeedback(lpparam);

        // Notification Hook
        NotificationInterruptionHook.hookNotificationManager(lpparam);
        NotificationInterruptionHook.hookNotificationListener(lpparam);
        NotificationInterruptionHook.hookActivityManager(lpparam);
        NotificationInterruptionHook.hookNotificationChannel(lpparam);
        NotificationInterruptionHook.hookPackageManager(lpparam);

        // Biometric Hook
        BiometricRealismHook.hookFingerprintManager(lpparam);
        BiometricRealismHook.hookBiometricManager(lpparam);
        BiometricRealismHook.hookBiometricPrompt(lpparam);
        BiometricRealismHook.hookFaceManager(lpparam);
        BiometricRealismHook.hookIrisManager(lpparam);
        BiometricRealismHook.hookSamsungBiometricService(lpparam);

        // Device Grip Hook
        DeviceGripAndOrientationHook.hookWindowManager(lpparam);
        DeviceGripAndOrientationHook.hookOrientationController(lpparam);
        DeviceGripAndOrientationHook.hookSensorManager(lpparam);
        DeviceGripAndOrientationHook.hookMotionEvent(lpparam);
        DeviceGripAndOrientationHook.hookInputDispatcher(lpparam);
        DeviceGripAndOrientationHook.hookView(lpparam);

        // Power Management Hook
        PowerStateManagementHook.hookPowerManager(lpparam);
        PowerStateManagementHook.hookBatteryManager(lpparam);
        PowerStateManagementHook.hookUsageStatsManager(lpparam);
        PowerStateManagementHook.hookPowerManagerInternal(lpparam);
        PowerStateManagementHook.hookActivityManager(lpparam);
        PowerStateManagementHook.hookBatteryStatsService(lpparam);
        PowerStateManagementHook.hookDeviceIdleController(lpparam);

        // Audio Environment Hook (NEW)
        AudioEnvironmentRealismHook.hookAudioManager(lpparam);
        AudioEnvironmentRealismHook.hookAudioRecord(lpparam);
        AudioEnvironmentRealismHook.hookMediaRecorder(lpparam);

        // GPS Location Trajectory Hook (NEW)
        GPSLocationTrajectoryHook.hookLocationManager(lpparam);
        GPSLocationTrajectoryHook.hookLocation(lpparam);

        // Register with state manager
        sStateManager.registerHookContext(lpparam.packageName);
    }

    /**
     * Check if package is a target for hooking
     */
    private boolean isTargetPackage(String packageName) {
        // TikTok variants
        if (packageName.equals("com.zhiliaoapp.musically") ||    // TikTok
            packageName.equals("com.ss.android.ugc.trill") ||   // TikTok regional
            packageName.equals("com.ss.android.ugc.aweme")) {   // Douyin
            return true;
        }

        // System packages for hardware simulation
        if (packageName.equals("android") ||
            packageName.startsWith("com.android.systemui") ||
            packageName.startsWith("com.android.server")) {
            return true;
        }

        // Social apps for notification simulation
        if (packageName.equals("com.instagram.android") ||
            packageName.equals("com.facebook.katana") ||
            packageName.equals("com.twitter.android") ||
            packageName.equals("com.snapchat.android") ||
            packageName.equals("com.google.android.apps.messaging") ||
            packageName.equals("com.samsung.android.messaging")) {
            return true;
        }

        // Audio/video apps for audio environment simulation
        if (packageName.equals("com.google.android.youtube") ||
            packageName.equals("com.spotify.music") ||
            packageName.equals("com.google.android.apps.maps")) {
            return true;
        }

        return false;
    }

    /**
     * Reload all hook settings
     */
    public static void reloadSettings() {
        if (sPrefs != null) {
            HapticFeedbackRealismHook.reloadSettings();
            NotificationInterruptionHook.reloadSettings();
            BiometricRealismHook.reloadSettings();
            DeviceGripAndOrientationHook.reloadSettings();
            PowerStateManagementHook.reloadSettings();
            AudioEnvironmentRealismHook.reloadSettings();
            GPSLocationTrajectoryHook.reloadSettings();
        }
    }

    /**
     * Get comprehensive state for all hooks
     */
    public static RealismStateManager.RealismSystemState getSystemState() {
        return sStateManager.getSystemState();
    }
}
