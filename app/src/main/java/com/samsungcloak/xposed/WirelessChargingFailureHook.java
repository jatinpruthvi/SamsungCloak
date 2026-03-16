package com.samsungcloak.xposed;

import android.os.BatteryManager;
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

/** WirelessChargingFailureHook - Power & Charging */
public class WirelessChargingFailureHook {
    private static final String TAG = "[Wireless][Charging]";
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    private static float foreignObjectRate = 0.08f;
    private static float temperatureThreshold = 35f;
    private static boolean isCharging = false;
    private static float currentTemperature = 25f;
    private static final Random random = new Random();
    private static final List<ChargingEvent> chargingEvents = new CopyOnWriteArrayList<>();

    public static class ChargingEvent {
        public long timestamp; public String type; public String details;
        public ChargingEvent(String type, String details) { this.timestamp = System.currentTimeMillis(); this.type = type; this.details = details; }
    }

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (hookInitialized.getAndSet(true)) return;
        HookUtils.logInfo(TAG, "Initializing Wireless Charging Failure Hook");
        try {
            Class<?> batteryManagerClass = XposedHelpers.findClass("android.os.BatteryManager", lpparam.classLoader);
            XposedBridge.hookAllMethods(batteryManagerClass, "isCharging", new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    if (random.nextFloat() < foreignObjectRate) {
                        chargingEvents.add(new ChargingEvent("FOREIGN_OBJECT", "Foreign object detected"));
                        param.setResult(false);
                    } else if (currentTemperature > temperatureThreshold) {
                        chargingEvents.add(new ChargingEvent("THERMAL_THROTTLED", "Charging throttled"));
                    }
                    isCharging = (boolean) param.getResult();
                }
            });
            HookUtils.logInfo(TAG, "Wireless charging hook initialized");
        } catch (Throwable t) { HookUtils.logDebug(TAG, "Charging hook skipped: " + t.getMessage()); hookInitialized.set(true); }
    }
    public static void setEnabled(boolean e) { enabled = e; }
    public static boolean isEnabled() { return enabled; }
    public static void setTemperature(float temp) { currentTemperature = temp; }
    public static List<ChargingEvent> getChargingEvents() { return new ArrayList<>(chargingEvents); }
}
