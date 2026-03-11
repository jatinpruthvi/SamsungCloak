package com.samsungcloak.xposed;

import android.app.usage.UsageStatsManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;

/**
 * Hook #39: AppStandbyBucketHook - Android standby bucket simulation
 */
public class AppStandbyBucketHook {
    private static final String TAG = "[Power][AppStandby]";
    private static boolean enabled = true;
    private static float bucketTransitionSpeed = 0.5f;
    private static final Random random = new Random();

    public static final int BUCKET_ACTIVE = 10;
    public static final int BUCKET_WORKING_SET = 20;
    public static final int BUCKET_FREQUENT = 30;
    public static final int BUCKET_RARE = 40;
    public static final int BUCKET_NEVER = 50;

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        HookUtils.logInfo(TAG, "Initializing App Standby Bucket Hook");
        try {
            Class<?> usageStatsManagerClass = XposedHelpers.findClass(
                "android.app.usage.UsageStatsManager", lpparam.classLoader);
            XposedBridge.hookAllMethods(usageStatsManagerClass, "getAppStandbyBucket", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    String packageName = (String) param.args[0];
                    int bucket = calculateStandbyBucket(packageName);
                    param.setResult(bucket);
                }
            });
            HookUtils.logInfo(TAG, "App Standby Bucket Hook initialized");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize hook", e);
        }
    }

    private static int calculateStandbyBucket(String packageName) {
        float roll = random.nextFloat() / bucketTransitionSpeed;
        if (roll < 0.1f) return BUCKET_ACTIVE;
        if (roll < 0.3f) return BUCKET_WORKING_SET;
        if (roll < 0.6f) return BUCKET_FREQUENT;
        if (roll < 0.9f) return BUCKET_RARE;
        return BUCKET_NEVER;
    }
}
