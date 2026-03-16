package com.samsungcloak.xposed;

import android.content.pm.LauncherApps;
import android.appwidget.AppWidgetManager;
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

/** AppShortcutFailureHook - Home Screen & Launchers */
public class AppShortcutFailureHook {
    private static final String TAG = "[App][Shortcut]";
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    private static float creationFailureRate = 0.10f;
    private static float shortcutNotFoundRate = 0.05f;
    private static int shortcutCount = 0;
    private static final Random random = new Random();
    private static final List<ShortcutEvent> shortcutEvents = new CopyOnWriteArrayList<>();

    public static class ShortcutEvent {
        public long timestamp; public String type; public String details;
        public ShortcutEvent(String type, String details) { this.timestamp = System.currentTimeMillis(); this.type = type; this.details = details; }
    }

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (hookInitialized.getAndSet(true)) return;
        HookUtils.logInfo(TAG, "Initializing App Shortcut Failure Hook");
        try {
            Class<?> launcherAppsClass = XposedHelpers.findClass("android.content.pm.LauncherApps", lpparam.classLoader);
            XposedBridge.hookAllMethods(launcherAppsClass, "pinShortcuts", new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    shortcutCount++;
                    if (random.nextFloat() < creationFailureRate) shortcutEvents.add(new ShortcutEvent("CREATION_FAILED", "Shortcut creation failed"));
                }
            });
            HookUtils.logInfo(TAG, "App shortcut hook initialized");
        } catch (Throwable t) { HookUtils.logDebug(TAG, "Shortcut hook skipped: " + t.getMessage()); hookInitialized.set(true); }
    }
    public static void setEnabled(boolean e) { enabled = e; }
    public static boolean isEnabled() { return enabled; }
    public static List<ShortcutEvent> getShortcutEvents() { return new ArrayList<>(shortcutEvents); }
}
