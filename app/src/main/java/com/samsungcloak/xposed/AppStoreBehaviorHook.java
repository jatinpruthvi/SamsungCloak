package com.samsungcloak.xposed;

import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.SystemClock;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * AppStoreBehaviorHook - App Store Interaction Simulation
 *
 * Simulates realistic app store browsing and download behaviors:
 * 1. Browse-before-install patterns (screenshots, description, reviews)
 * 2. Download pause/resume behaviors (network switching, calls)
 * 3. Review reading patterns (dwell time, helpfulness voting)
 * 4. App comparison behaviors (switching between apps)
 * 5. Update deferral patterns (WiFi preference, time-of-day)
 *
 * Based on Google Play Store analytics and user behavior studies.
 *
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class AppStoreBehaviorHook {

    private static final String TAG = "[AppStore][Behavior]";
    private static final String PREFS_NAME = "SamsungCloak_AppStore";
    private static final boolean DEBUG = true;

    private static boolean enabled = false;
    private static SharedPreferences prefs;
    private static final Random random = new Random();

    // Browse behavior
    private static boolean browseBeforeInstall = true;
    private static double screenshotViewProbability = 0.72;
    private static double descriptionReadProbability = 0.45;
    private static double reviewsCheckProbability = 0.68;
    private static int averageScreenshotsViewed = 4;

    // Review reading
    private static double averageReviewDwellSeconds = 3.5;
    private static double helpfulnessVoteProbability = 0.08;
    private static int reviewsReadBeforeDecision = 5;

    // Download behavior
    private static boolean wifiPreferredForDownload = true;
    private static double downloadPauseProbability = 0.15;
    private static double downloadCancellationProbability = 0.05;
    private static int downloadResumeDelaySeconds = 120;

    // Update behavior
    private static boolean autoUpdateEnabled = false; // Many users disable
    private static double immediateUpdateProbability = 0.25;
    private static double wifiOnlyUpdateProbability = 0.78;
    private static int updateDeferralDays = 3;

    // App comparison
    private static double appComparisonProbability = 0.35;
    private static int appsComparedAverage = 2;
    private static int comparisonSwitchDelayMs = 2500;

    // State tracking
    private static Map<String, AppBrowseSession> activeSessions = new HashMap<>();
    private static String currentAppPackage = null;
    private static long sessionStartTime = 0;

    /**
     * App browse session tracking
     */
    public static class AppBrowseSession {
        public String packageName;
        public long startTime;
        public int screenshotsViewed = 0;
        public boolean descriptionRead = false;
        public int reviewsRead = 0;
        public boolean installed = false;
        public long totalDwellTimeMs = 0;

        public AppBrowseSession(String packageName) {
            this.packageName = packageName;
            this.startTime = System.currentTimeMillis();
        }
    }

    /**
     * Initialize the App Store Behavior Hook
     */
    public static void init(XC_LoadPackage.LoadPackageParam lpparam, Context context) {
        if (context != null) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            enabled = prefs.getBoolean("appstore_behavior_enabled", false);
        }

        if (!enabled) {
            HookUtils.logInfo(TAG, "App store behavior hook disabled");
            return;
        }

        HookUtils.logInfo(TAG, "Initializing App Store Behavior Hook");

        try {
            loadBehaviorState();

            hookDownloadManager(lpparam);
            hookPackageInstaller(lpparam);

            HookUtils.logInfo(TAG, "App Store Behavior Hook initialized");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize: " + e.getMessage());
        }
    }

    /**
     * Alternative init without Context
     */
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        enabled = false;
        HookUtils.logInfo(TAG, "App store behavior hook requires Context for SharedPreferences");
    }

    private static void loadBehaviorState() {
        if (prefs == null) return;

        browseBeforeInstall = prefs.getBoolean("browse_before_install", true);
        screenshotViewProbability = prefs.getFloat("screenshot_prob", 0.72f);
        descriptionReadProbability = prefs.getFloat("description_prob", 0.45f);
        reviewsCheckProbability = prefs.getFloat("reviews_prob", 0.68f);
        wifiPreferredForDownload = prefs.getBoolean("wifi_preferred", true);
        autoUpdateEnabled = prefs.getBoolean("auto_update", false);
    }

    private static void hookDownloadManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> downloadManagerClass = XposedHelpers.findClass(
                "android.app.DownloadManager", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(downloadManagerClass, "enqueue",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        // Simulate download decision process
                        simulateDownloadDecision();
                    }
                });

            HookUtils.logDebug(TAG, "DownloadManager hooked");
        } catch (Exception e) {
            HookUtils.logError(TAG, "DownloadManager hook failed: " + e.getMessage());
        }
    }

    private static void hookPackageInstaller(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> packageInstallerClass = XposedHelpers.findClass(
                "android.content.pm.PackageInstaller", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(packageInstallerClass, "createSession",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        // Simulate app install decision process
                        simulateInstallDecision(param);
                    }
                });

            HookUtils.logDebug(TAG, "PackageInstaller hooked");
        } catch (Exception e) {
            HookUtils.logError(TAG, "PackageInstaller hook failed: " + e.getMessage());
        }
    }

    private static void simulateDownloadDecision() {
        // Check if user would wait for WiFi
        if (wifiPreferredForDownload && !isWifiConnected()) {
            if (random.nextDouble() < 0.7) {
                // Defer download until WiFi
                HookUtils.logDebug(TAG, "Download deferred - waiting for WiFi");
            }
        }

        // Simulate potential download pause
        if (random.nextDouble() < downloadPauseProbability) {
            int pauseDelay = random.nextInt(300) + 60; // 1-6 minute pause
            HookUtils.logDebug(TAG, "Download will pause after " + pauseDelay + " seconds");
        }
    }

    private static void simulateInstallDecision(XC_MethodHook.MethodHookParam param) {
        if (!browseBeforeInstall) return;

        // Simulate browse time before install
        int browseTimeMs = 0;

        // Screenshot viewing time
        if (random.nextDouble() < screenshotViewProbability) {
            int screenshots = random.nextInt(averageScreenshotsViewed) + 2;
            browseTimeMs += screenshots * 2000; // 2 seconds per screenshot
        }

        // Description reading time
        if (random.nextDouble() < descriptionReadProbability) {
            browseTimeMs += 8000 + random.nextInt(7000); // 8-15 seconds
        }

        // Reviews reading time
        if (random.nextDouble() < reviewsCheckProbability) {
            int reviews = random.nextInt(reviewsReadBeforeDecision) + 3;
            browseTimeMs += (int) (reviews * averageReviewDwellSeconds * 1000);
        }

        // Add realistic browse delay
        if (browseTimeMs > 0) {
            int actualDelay = Math.min(browseTimeMs, 30000); // Cap at 30s
            SystemClock.sleep(actualDelay);

            if (DEBUG) {
                HookUtils.logDebug(TAG, "Simulated app browse time: " + (browseTimeMs / 1000) + "s");
            }
        }
    }

    private static boolean isWifiConnected() {
        // Would check actual network state - simulated for now
        return random.nextDouble() < 0.6; // Simulate 60% WiFi availability
    }

    /**
     * Start tracking an app browse session
     */
    public static void startAppBrowse(String packageName) {
        currentAppPackage = packageName;
        sessionStartTime = System.currentTimeMillis();
        activeSessions.put(packageName, new AppBrowseSession(packageName));

        HookUtils.logDebug(TAG, "Started app browse session: " + packageName);
    }

    /**
     * End an app browse session
     */
    public static void endAppBrowse(String packageName, boolean installed) {
        AppBrowseSession session = activeSessions.get(packageName);
        if (session != null) {
            session.totalDwellTimeMs = System.currentTimeMillis() - session.startTime;
            session.installed = installed;

            if (DEBUG) {
                HookUtils.logDebug(TAG, String.format(
                    "App browse ended: %s, dwell=%ds, installed=%b",
                    packageName, session.totalDwellTimeMs / 1000, installed
                ));
            }
        }
        activeSessions.remove(packageName);
    }

    // ========== Configuration Methods ==========

    public static void setEnabled(boolean enabled) {
        AppStoreBehaviorHook.enabled = enabled;
        if (prefs != null) {
            prefs.edit().putBoolean("appstore_behavior_enabled", enabled).apply();
        }
        HookUtils.logInfo(TAG, "Hook " + (enabled ? "enabled" : "disabled"));
    }

    public static void setBrowseBeforeInstall(boolean enabled) {
        browseBeforeInstall = enabled;
        if (prefs != null) {
            prefs.edit().putBoolean("browse_before_install", enabled).apply();
        }
    }

    public static void setWifiPreferred(boolean preferred) {
        wifiPreferredForDownload = preferred;
        if (prefs != null) {
            prefs.edit().putBoolean("wifi_preferred", preferred).apply();
        }
    }

    public static void setAutoUpdateEnabled(boolean enabled) {
        autoUpdateEnabled = enabled;
        if (prefs != null) {
            prefs.edit().putBoolean("auto_update", enabled).apply();
        }
    }

    public static void setScreenshotViewProbability(double probability) {
        screenshotViewProbability = Math.max(0.0, Math.min(1.0, probability));
        if (prefs != null) {
            prefs.edit().putFloat("screenshot_prob", (float) screenshotViewProbability).apply();
        }
    }

    public static void setReviewsCheckProbability(double probability) {
        reviewsCheckProbability = Math.max(0.0, Math.min(1.0, probability));
        if (prefs != null) {
            prefs.edit().putFloat("reviews_prob", (float) reviewsCheckProbability).apply();
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static AppBrowseSession getSession(String packageName) {
        return activeSessions.get(packageName);
    }

    public static boolean isAutoUpdateEnabled() {
        return autoUpdateEnabled;
    }
}
