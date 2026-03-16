package com.samsungcloak.xposed;

import android.app.Activity;
import android.app.PictureInPictureParams;
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

/** PictureInPictureFailureHook - Media & Productivity */
public class PictureInPictureFailureHook {
    private static final String TAG = "[PiP]";
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    private static float enterFailureRate = 0.12f;
    private static boolean isInPipMode = false;
    private static int pipCount = 0;
    private static final Random random = new Random();
    private static final List<PipEvent> pipEvents = new CopyOnWriteArrayList<>();

    public static class PipEvent {
        public long timestamp; public String type; public String details;
        public PipEvent(String type, String details) { this.timestamp = System.currentTimeMillis(); this.type = type; this.details = details; }
    }

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (hookInitialized.getAndSet(true)) return;
        HookUtils.logInfo(TAG, "Initializing PiP Failure Hook");
        try {
            Class<?> activityClass = XposedHelpers.findClass("android.app.Activity", lpparam.classLoader);
            XposedBridge.hookAllMethods(activityClass, "enterPictureInPictureMode", new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    pipCount++;
                    if (random.nextFloat() < enterFailureRate) {
                        pipEvents.add(new PipEvent("ENTER_FAILED", "Failed to enter PiP"));
                    } else {
                        isInPipMode = true;
                    }
                }
            });
            HookUtils.logInfo(TAG, "PiP hook initialized");
        } catch (Throwable t) { HookUtils.logDebug(TAG, "PiP hook skipped: " + t.getMessage()); hookInitialized.set(true); }
    }
    public static void setEnabled(boolean e) { enabled = e; }
    public static boolean isEnabled() { return enabled; }
    public static List<PipEvent> getPipEvents() { return new ArrayList<>(pipEvents); }
}
