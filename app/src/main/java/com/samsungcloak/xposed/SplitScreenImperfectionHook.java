package com.samsungcloak.xposed;

import android.app.ActivityManager;
import android.view.WindowManager;
import android.os.Handler;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/** SplitScreenImperfectionHook - Multi-Window */
public class SplitScreenImperfectionHook {
    private static final String TAG = "[SplitScreen]";
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    private static float enterFailureRate = 0.10f;
    private static boolean isInSplitMode = false;
    private static int splitCount = 0;
    private static final Random random = new Random();
    private static final List<SplitEvent> splitEvents = new CopyOnWriteArrayList<>();

    public static class SplitEvent {
        public long timestamp; public String type; public String details;
        public SplitEvent(String type, String details) { this.timestamp = System.currentTimeMillis(); this.type = type; this.details = details; }
    }

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (hookInitialized.getAndSet(true)) return;
        HookUtils.logInfo(TAG, "Initializing Split Screen Imperfection Hook");
        try {
            Class<?> amClass = XposedHelpers.findClass("android.app.ActivityManager", lpparam.classLoader);
            XposedBridge.hookAllMethods(amClass, "startMultiWindowMode", new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    splitCount++;
                    if (random.nextFloat() < enterFailureRate) {
                        splitEvents.add(new SplitEvent("ENTER_FAILED", "Cannot enter split screen"));
                    } else {
                        isInSplitMode = true;
                    }
                }
            });
            HookUtils.logInfo(TAG, "Split screen hook initialized");
        } catch (Throwable t) { HookUtils.logDebug(TAG, "Split hook skipped: " + t.getMessage()); hookInitialized.set(true); }
    }
    public static void setEnabled(boolean e) { enabled = e; }
    public static boolean isEnabled() { return enabled; }
    public static List<SplitEvent> getSplitEvents() { return new ArrayList<>(splitEvents); }
}
