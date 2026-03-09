package com.samsungcloak.xposed;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.location.Location;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SensorFusionCoherenceHook {

    private static final String TAG = "[HumanInteraction][SensorFusion]";
    private static final boolean DEBUG = true;
    private static final ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new Random());

    private static boolean enabled = true;
    private static double walkingIndicationProbability = 0.25;

    private static final double STEP_FREQUENCY_HZ = 1.8;
    private static final double STEP_AMPLITUDE_ACCEL = 2.5;
    private static final double STEP_AMPLITUDE_GYRO = 0.3;

    private static boolean isWalking = false;
    private static double currentWalkingSpeed = 0.0;
    private static long sessionStartTime = 0;
    private static long stepPhaseStartTime = 0;

    private static final Map<Integer, float[]> sensorBaselineValues = new HashMap<>();

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        HookUtils.logInfo(TAG, "Initializing Sensor-Fusion Coherence Hook");

        try {
            hookSensorEventDispatch(lpparam);
            hookLocationUpdates(lpparam);
            HookUtils.logInfo(TAG, "Sensor-Fusion Coherence Hook initialized successfully");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize hook", e);
        }
    }

    private static void hookSensorEventDispatch(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> sensorEventQueueClass = XposedHelpers.findClass(
                "android.hardware.SystemSensorManager$SensorEventQueue",
                lpparam.classLoader
            );

            XposedBridge.hookAllMethods(sensorEventQueueClass, "dispatchSensorEvent",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        try {
                            if (param.args.length < 2) return;

                            int sensorHandle = (int) param.args[0];
                            float[] values = (float[]) param.args[1];

                            if (values == null || values.length == 0) return;

                            int sensorType = mapHandleToSensorType(sensorHandle);

                            if (sensorType == -1) return;

                            applyWalkingDynamics(sensorType, values);

                        } catch (Exception e) {
                            if (DEBUG) HookUtils.logDebug(TAG, "Error in sensor dispatch: " + e.getMessage());
                        }
                    }
                });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked SystemSensorManager.SensorEventQueue.dispatchSensorEvent");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook sensor event dispatch", e);
        }
    }

    private static void hookLocationUpdates(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedBridge.hookAllMethods(Location.class, "getSpeed", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    try {
                        float speed = (float) param.getResult();

                        if (speed > 0.5f && speed < 3.0f) {
                            currentWalkingSpeed = speed;
                            isWalking = true;
                            if (sessionStartTime == 0) {
                                sessionStartTime = System.currentTimeMillis();
                                stepPhaseStartTime = sessionStartTime;
                            }

                            if (DEBUG) {
                                HookUtils.logDebug(TAG, String.format(
                                    "Walking detected from GPS: speed=%.2f m/s",
                                    speed
                                ));
                            }
                        } else if (speed < 0.1f) {
                            isWalking = false;
                            currentWalkingSpeed = 0.0;
                        }
                    } catch (Exception e) {
                        if (DEBUG) HookUtils.logDebug(TAG, "Error processing location speed: " + e.getMessage());
                    }
                }
            });

            XposedBridge.hookAllMethods(Location.class, "setSpeed", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    try {
                        float speed = (float) param.args[0];

                        if (speed > 0.5f && speed < 3.0f) {
                            currentWalkingSpeed = speed;
                            isWalking = true;
                            if (sessionStartTime == 0) {
                                sessionStartTime = System.currentTimeMillis();
                                stepPhaseStartTime = sessionStartTime;
                            }
                        }
                    } catch (Exception e) {
                        if (DEBUG) HookUtils.logDebug(TAG, "Error setting location speed: " + e.getMessage());
                    }
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked Location speed tracking");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook location updates", e);
        }
    }

    private static void applyWalkingDynamics(int sensorType, float[] values) {
        if (!isWalking || currentWalkingSpeed == 0.0) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        double elapsedTime = (currentTime - sessionStartTime) / 1000.0;
        double phase = 2.0 * Math.PI * STEP_FREQUENCY_HZ * elapsedTime;

        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                applyWalkingOscillationAccelerometer(values, phase);
                break;

            case Sensor.TYPE_GYROSCOPE:
                applyWalkingOscillationGyroscope(values, phase);
                break;

            case Sensor.TYPE_LINEAR_ACCELERATION:
                applyWalkingOscillationLinearAccel(values, phase);
                break;

            case Sensor.TYPE_GRAVITY:
                applyGravityFluctuations(values, phase);
                break;
        }
    }

    private static void applyWalkingOscillationAccelerometer(float[] values, double phase) {
        double speedFactor = currentWalkingSpeed / 1.5;

        double accelOscillationX = STEP_AMPLITUDE_ACCEL * speedFactor * Math.sin(phase);
        double accelOscillationY = STEP_AMPLITUDE_ACCEL * speedFactor * Math.sin(phase + Math.PI / 4);
        double accelOscillationZ = STEP_AMPLITUDE_ACCEL * speedFactor * Math.cos(phase * 2);

        values[0] += accelOscillationX + HookUtils.generateGaussianNoise(0.15);
        values[1] += accelOscillationY + HookUtils.generateGaussianNoise(0.15);
        values[2] += accelOscillationZ + HookUtils.generateGaussianNoise(0.2);

        values[0] = HookUtils.clamp(values[0], -20.0f, 20.0f);
        values[1] = HookUtils.clamp(values[1], -20.0f, 20.0f);
        values[2] = HookUtils.clamp(values[2], -20.0f, 20.0f);

        if (DEBUG && random.get().nextDouble() < 0.02) {
            HookUtils.logDebug(TAG, String.format(
                "Walking accel applied: speed=%.2f, phase=%.2f, accel=(%.2f,%.2f,%.2f)",
                currentWalkingSpeed, phase, values[0], values[1], values[2]
            ));
        }
    }

    private static void applyWalkingOscillationGyroscope(float[] values, double phase) {
        double speedFactor = currentWalkingSpeed / 1.5;

        double gyroOscillationX = STEP_AMPLITUDE_GYRO * speedFactor * Math.sin(phase + Math.PI / 6);
        double gyroOscillationY = STEP_AMPLITUDE_GYRO * speedFactor * Math.cos(phase + Math.PI / 3);
        double gyroOscillationZ = STEP_AMPLITUDE_GYRO * speedFactor * Math.sin(phase * 1.5);

        values[0] += gyroOscillationX + HookUtils.generateGaussianNoise(0.03);
        values[1] += gyroOscillationY + HookUtils.generateGaussianNoise(0.03);
        values[2] += gyroOscillationZ + HookUtils.generateGaussianNoise(0.04);

        values[0] = HookUtils.clamp(values[0], -5.0f, 5.0f);
        values[1] = HookUtils.clamp(values[1], -5.0f, 5.0f);
        values[2] = HookUtils.clamp(values[2], -5.0f, 5.0f);

        if (DEBUG && random.get().nextDouble() < 0.02) {
            HookUtils.logDebug(TAG, String.format(
                "Walking gyro applied: speed=%.2f, phase=%.2f, gyro=(%.4f,%.4f,%.4f)",
                currentWalkingSpeed, phase, values[0], values[1], values[2]
            ));
        }
    }

    private static void applyWalkingOscillationLinearAccel(float[] values, double phase) {
        double speedFactor = currentWalkingSpeed / 1.5;

        double linearOscillationX = STEP_AMPLITUDE_ACCEL * 0.7 * speedFactor * Math.sin(phase);
        double linearOscillationY = STEP_AMPLITUDE_ACCEL * 0.7 * speedFactor * Math.sin(phase + Math.PI / 4);

        values[0] += linearOscillationX + HookUtils.generateGaussianNoise(0.1);
        values[1] += linearOscillationY + HookUtils.generateGaussianNoise(0.1);

        values[0] = HookUtils.clamp(values[0], -10.0f, 10.0f);
        values[1] = HookUtils.clamp(values[1], -10.0f, 10.0f);

        if (DEBUG && random.get().nextDouble() < 0.01) {
            HookUtils.logDebug(TAG, String.format(
                "Walking linear accel applied: speed=%.2f, linear=(%.2f,%.2f,%.2f)",
                currentWalkingSpeed, values[0], values[1], values[2]
            ));
        }
    }

    private static void applyGravityFluctuations(float[] values, double phase) {
        double gravityFluctuation = 0.15 * Math.sin(phase * 0.5);

        values[0] += gravityFluctuation * 0.1 + HookUtils.generateGaussianNoise(0.05);
        values[1] += gravityFluctuation * 0.1 + HookUtils.generateGaussianNoise(0.05);
        values[2] += gravityFluctuation + HookUtils.generateGaussianNoise(0.08);

        values[0] = HookUtils.clamp(values[0], -3.0f, 3.0f);
        values[1] = HookUtils.clamp(values[1], -3.0f, 3.0f);
        values[2] = HookUtils.clamp(values[2], 8.0f, 11.0f);
    }

    private static int mapHandleToSensorType(int handle) {
        switch (handle) {
            case 0: return Sensor.TYPE_ACCELEROMETER;
            case 1: return Sensor.TYPE_GYROSCOPE;
            case 3: return Sensor.TYPE_LINEAR_ACCELERATION;
            case 4: return Sensor.TYPE_GRAVITY;
            default: return -1;
        }
    }

    public static void setEnabled(boolean enabled) {
        SensorFusionCoherenceHook.enabled = enabled;
        HookUtils.logInfo(TAG, "Hook " + (enabled ? "enabled" : "disabled"));
    }

    public static void setWalkingIndicationProbability(double prob) {
        SensorFusionCoherenceHook.walkingIndicationProbability = HookUtils.clamp(prob, 0.0, 1.0);
        HookUtils.logInfo(TAG, "Walking indication probability set to: " + SensorFusionCoherenceHook.walkingIndicationProbability);
    }

    public static void setStepFrequency(double frequencyHz) {
        if (frequencyHz > 0.5 && frequencyHz < 4.0) {
            HookUtils.logInfo(TAG, "Step frequency set to: " + frequencyHz + " Hz");
        } else {
            HookUtils.logError(TAG, "Invalid step frequency: " + frequencyHz);
        }
    }

    public static void simulateWalkingState(boolean isWalking, double speed) {
        SensorFusionCoherenceHook.isWalking = isWalking;
        SensorFusionCoherenceHook.currentWalkingSpeed = speed;

        if (isWalking && sessionStartTime == 0) {
            sessionStartTime = System.currentTimeMillis();
            stepPhaseStartTime = sessionStartTime;
        }

        HookUtils.logInfo(TAG, String.format(
            "Walking state set: %s, speed=%.2f m/s",
            isWalking ? "walking" : "stationary", speed
        ));
    }
}
