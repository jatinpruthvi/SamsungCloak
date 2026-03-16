package com.samsungcloak.xposed;

import android.hardware.usb.UsbManager;
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

/** USBOTGDetectionHook - Wired Connectivity */
public class USBOTGDetectionHook {
    private static final String TAG = "[USB][OTG]";
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    private static float powerNegotiationFailureRate = 0.12f;
    private static boolean isDeviceConnected = false;
    private static final Random random = new Random();
    private static final List<USBEvent> usbEvents = new CopyOnWriteArrayList<>();

    public static class USBEvent {
        public long timestamp; public String type; public String details;
        public USBEvent(String type, String details) { this.timestamp = System.currentTimeMillis(); this.type = type; this.details = details; }
    }

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (hookInitialized.getAndSet(true)) return;
        HookUtils.logInfo(TAG, "Initializing USB OTG Detection Hook");
        try {
            Class<?> usbManagerClass = XposedHelpers.findClass("android.hardware.usb.UsbManager", lpparam.classLoader);
            XposedBridge.hookAllMethods(usbManagerClass, "openDevice", new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    if (random.nextFloat() < powerNegotiationFailureRate) {
                        usbEvents.add(new USBEvent("POWER_NEGOTIATION_FAILED", "USB power negotiation failed"));
                    } else {
                        isDeviceConnected = true;
                    }
                }
            });
            HookUtils.logInfo(TAG, "USB OTG hook initialized");
        } catch (Throwable t) { HookUtils.logDebug(TAG, "USB hook skipped: " + t.getMessage()); hookInitialized.set(true); }
    }
    public static void setEnabled(boolean e) { enabled = e; }
    public static boolean isEnabled() { return enabled; }
    public static List<USBEvent> getUSBEvents() { return new ArrayList<>(usbEvents); }
}
