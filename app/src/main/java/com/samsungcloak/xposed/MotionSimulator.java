package com.samsungcloak.xposed;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MotionSimulator {
    private static final String LOG_TAG = "SamsungCloak.MotionSimulator";
    private static boolean initialized = false;

    private static long lastMotionTime = 0;
    private static float motionState = 0.0f;

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (initialized) {
            HookUtils.logDebug("MotionSimulator already initialized");
            return;
        }

        try {
            hookSensorManager(lpparam);
            initialized = true;
            HookUtils.logInfo("MotionSimulator initialized successfully");
        } catch (Exception e) {
            HookUtils.logError("Failed to initialize MotionSimulator: " + e.getMessage());
        }
    }

    private static void hookSensorManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> sensorManagerClass = XposedHelpers.findClass(
                "android.hardware.SensorManager", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(sensorManagerClass, "getSensorList", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        Object sensorList = param.getResult();
                        if (sensorList != null && sensorList instanceof java.util.List) {
                            java.util.List<?> list = (java.util.List<?>) sensorList;
                            for (Object sensor : list) {
                                try {
                                    int type = (int) XposedHelpers.getIntField(sensor, "mType");
                                    if (type == 1 || type == 4 || type == 9 || type == 10 || type == 11) {
                                        Object listener = XposedHelpers.getObjectField(sensor, "mListener");
                                        if (listener != null) {
                                            XposedHelpers.setObjectField(sensor, "mListener", createMotionListener());
                                        }
                                    }
                                } catch (Exception e) {
                                }
                            }
                        }
                    } catch (Exception e) {
                        HookUtils.logError("Error in getSensorList hook: " + e.getMessage());
                    }
                }
            });

            HookUtils.logInfo("Hooked SensorManager methods");
        } catch (Exception e) {
            HookUtils.logError("Failed to hook sensor manager: " + e.getMessage());
        }
    }

    private static Object createMotionListener() {
        return new MotionListenerProxy();
    }

    private static class MotionListenerProxy {
        private static final long sessionStart = System.currentTimeMillis();

        public void onSensorChanged(float[] values, int type) {
            if (values != null && values.length > 0) {
                long currentTime = System.currentTimeMillis();
                long timeDelta = currentTime - lastMotionTime;

                if (timeDelta > 50) {
                    float motion = HookUtils.generateGaussianNoise(0.02f);
                    motionState = (motionState * 0.9f) + motion;
                    motionState = HookUtils.clamp(motionState, -1.0f, 1.0f);
                }

                lastMotionTime = currentTime;

                switch (type) {
                    case 1:
                        if (values.length >= 3) {
                            values[0] += motionState * 0.1f;
                            values[1] += motionState * 0.1f;
                            values[2] += motionState * 0.1f;
                        }
                        break;
                    case 4:
                        if (values.length >= 3) {
                            values[0] += motionState * 0.05f;
                            values[1] += motionState * 0.05f;
                            values[2] += motionState * 0.05f;
                        }
                        break;
                    case 9:
                    case 10:
                    case 11:
                        if (values.length >= 3) {
                            values[0] += motionState * 0.08f;
                            values[1] += motionState * 0.08f;
                            values[2] += motionState * 0.08f;
                        }
                        break;
                }
            }
        }

        public void onAccuracyChanged(int accuracy) {
        }

        public void onFlush() {
        }
    }
}
