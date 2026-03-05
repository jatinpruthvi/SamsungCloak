package com.samsungcloak.xposed;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class SensorHook {
    private static final String LOG_TAG = "SamsungCloak.SensorHook";
    private static boolean initialized = false;

    private static final int SENSOR_TYPE_ACCELEROMETER = 1;
    private static final int SENSOR_TYPE_MAGNETIC_FIELD = 2;
    private static final int SENSOR_TYPE_GYROSCOPE = 4;
    private static final int SENSOR_TYPE_LIGHT = 5;
    private static final int SENSOR_TYPE_PRESSURE = 6;
    private static final int SENSOR_TYPE_GRAVITY = 9;
    private static final int SENSOR_TYPE_LINEAR_ACCELERATION = 10;
    private static final int SENSOR_TYPE_ROTATION_VECTOR = 11;

    private static final long sessionStartTime = System.currentTimeMillis();

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (initialized) {
            HookUtils.logDebug("SensorHook already initialized");
            return;
        }

        try {
            hookSensorEventQueue(lpparam);
            initialized = true;
            HookUtils.logInfo("SensorHook initialized successfully");
        } catch (Exception e) {
            HookUtils.logError("Failed to initialize SensorHook: " + e.getMessage());
        }
    }

    private static void hookSensorEventQueue(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> sensorEventQueueClass = XposedHelpers.findClass(
                "android.hardware.SystemSensorManager$SensorEventQueue",
                lpparam.classLoader
            );

            XposedBridge.hookAllMethods(sensorEventQueueClass, "dispatchSensorEvent", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        if (param.args.length < 4) return;

                        int handle = (int) param.args[0];
                        float[] values = (float[]) param.args[1];

                        int sensorType = resolveSensorType(handle, param);

                        if (sensorType != -1 && values != null) {
                            modifySensorValues(sensorType, values, param.thisObject);
                        }
                    } catch (Exception e) {
                        HookUtils.logError("Error in dispatchSensorEvent hook: " + e.getMessage());
                    }
                }
            });

            HookUtils.logInfo("Hooked SensorEventQueue.dispatchSensorEvent()");
        } catch (Exception e) {
            HookUtils.logError("Failed to hook SensorEventQueue: " + e.getMessage());
        }
    }

    private static int resolveSensorType(int handle, XC_MethodHook.MethodHookParam param) {
        try {
            Object sensorManager = XposedHelpers.getObjectField(param.thisObject, "mManager");
            Object[] sensors = (Object[]) XposedHelpers.getObjectField(sensorManager, "mHandleToSensor");

            if (handle >= 0 && handle < sensors.length && sensors[handle] != null) {
                return (int) XposedHelpers.getIntField(sensors[handle], "mType");
            }
        } catch (Exception e) {
        }
        return -1;
    }

    private static void modifySensorValues(int sensorType, float[] values, Object eventQueue) {
        long currentTime = System.currentTimeMillis() - sessionStartTime;

        switch (sensorType) {
            case SENSOR_TYPE_ACCELEROMETER:
                modifyAccelerometer(values, currentTime);
                break;
            case SENSOR_TYPE_GYROSCOPE:
                modifyGyroscope(values, currentTime);
                break;
            case SENSOR_TYPE_LIGHT:
                modifyLight(values, currentTime);
                break;
            case SENSOR_TYPE_MAGNETIC_FIELD:
                modifyMagneticField(values, currentTime);
                break;
            case SENSOR_TYPE_PRESSURE:
                modifyPressure(values, currentTime);
                break;
            default:
                break;
        }
    }

    private static void modifyAccelerometer(float[] values, long time) {
        if (values.length >= 3) {
            double driftX = 0.05 * Math.sin(time / 2000.0);
            double driftY = 0.05 * Math.cos(time / 2500.0);
            double driftZ = 0.03 * Math.sin(time / 3000.0);

            values[0] += HookUtils.generateGaussianNoise(0.05f) + driftX;
            values[1] += HookUtils.generateGaussianNoise(0.05f) + driftY;
            values[2] += HookUtils.generateGaussianNoise(0.04f) + driftZ;

            values[0] = HookUtils.clamp(values[0], -12.0f, 12.0f);
            values[1] = HookUtils.clamp(values[1], -12.0f, 12.0f);
            values[2] = HookUtils.clamp(values[2], -12.0f, 12.0f);
        }
    }

    private static void modifyGyroscope(float[] values, long time) {
        if (values.length >= 3) {
            double driftX = 0.003 * Math.sin(time / 1500.0);
            double driftY = 0.003 * Math.cos(time / 1800.0);
            double driftZ = 0.002 * Math.sin(time / 2100.0);

            values[0] += HookUtils.generateGaussianNoise(0.002f) + driftX;
            values[1] += HookUtils.generateGaussianNoise(0.002f) + driftY;
            values[2] += HookUtils.generateGaussianNoise(0.002f) + driftZ;

            values[0] = HookUtils.clamp(values[0], -0.5f, 0.5f);
            values[1] = HookUtils.clamp(values[1], -0.5f, 0.5f);
            values[2] = HookUtils.clamp(values[2], -0.5f, 0.5f);
        }
    }

    private static void modifyLight(float[] values, long time) {
        if (values.length >= 1) {
            double flicker = 2.0 * Math.sin(time / 200.0) + 1.0 * Math.sin(time / 150.0);

            values[0] += HookUtils.generateGaussianNoise(0.5f) + flicker;
            values[0] = HookUtils.clamp(values[0], 0.0f, 10000.0f);
        }
    }

    private static void modifyMagneticField(float[] values, long time) {
        if (values.length >= 3) {
            double driftX = 0.2 * Math.sin(time / 5000.0);
            double driftY = 0.2 * Math.cos(time / 6000.0);
            double driftZ = 0.15 * Math.sin(time / 7000.0);

            values[0] += HookUtils.generateGaussianNoise(0.15f) + driftX;
            values[1] += HookUtils.generateGaussianNoise(0.15f) + driftY;
            values[2] += HookUtils.generateGaussianNoise(0.12f) + driftZ;

            values[0] = HookUtils.clamp(values[0], -100.0f, 100.0f);
            values[1] = HookUtils.clamp(values[1], -100.0f, 100.0f);
            values[2] = HookUtils.clamp(values[2], -100.0f, 100.0f);
        }
    }

    private static void modifyPressure(float[] values, long time) {
        if (values.length >= 1) {
            double drift = 0.05 * Math.sin(time / 10000.0);

            values[0] += HookUtils.generateGaussianNoise(0.02f) + drift;
            values[0] = HookUtils.clamp(values[0], 900.0f, 1100.0f);
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
