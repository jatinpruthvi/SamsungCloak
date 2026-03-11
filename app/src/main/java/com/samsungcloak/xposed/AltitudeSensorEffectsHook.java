package com.samsungcloak.xposed;

import android.hardware.Sensor;
import android.hardware.SensorEvent;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;

/**
 * Hook #28: Altitude Sensor Effects
 * 
 * Simulates altitude-related sensor variations
 */
public class AltitudeSensorEffectsHook {

    private static final String TAG = "[HumanInteraction][Altitude]";
    private static final boolean DEBUG = true;

    private static boolean enabled = true;
    private static float altitudeMeters = 0;
    private static float pressureVariation = 0.05f;
    private static final Random random = new Random();
    private static final double SEA_LEVEL_PRESSURE = 1013.25;

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        HookUtils.logInfo(TAG, "Initializing Altitude Sensor Effects Hook");
        try {
            hookPressureSensor(lpparam);
            HookUtils.logInfo(TAG, "Altitude Sensor Effects Hook initialized");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize hook", e);
        }
    }

    private static void hookPressureSensor(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> sensorEventClass = XposedHelpers.findClass("android.hardware.SensorEvent", lpparam.classLoader);
            XposedBridge.hookAllMethods(sensorEventClass, "onSensorChanged", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    try {
                        SensorEvent event = (SensorEvent) param.args[0];
                        if (event.sensor.getType() == Sensor.TYPE_PRESSURE) {
                            double weatherVariation = (random.nextFloat() - 0.5f) * 2 * pressureVariation * SEA_LEVEL_PRESSURE;
                            if (event.values != null && event.values.length > 0) {
                                event.values[0] = (float) (SEA_LEVEL_PRESSURE + weatherVariation);
                            }
                            if (DEBUG && random.nextFloat() < 0.01f) {
                                HookUtils.logDebug(TAG, "Pressure modified: " + event.values[0] + " hPa");
                            }
                        }
                    } catch (Exception e) { }
                }
            });
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook pressure sensor", e);
        }
    }
}
