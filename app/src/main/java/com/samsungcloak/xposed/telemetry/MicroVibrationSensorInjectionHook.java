package com.samsungcloak.xposed.telemetry;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;

public class MicroVibrationSensorInjectionHook {
    private static final String LOG_TAG = "SamsungCloak.MicroVibration";
    private static boolean initialized = false;

    private static Random random = new Random();
    private static long sessionStartTime = System.currentTimeMillis();

    private static final double TREMOR_FREQUENCY_HZ = 8.0;
    private static final double TREMOR_AMPLITUDE_MG = 15.0;
    private static final double NOISE_FLOOR_MG = 0.8;
    
    private static final double BREATHING_FREQUENCY_HZ = 0.25;
    private static final double BREATHING_AMPLITUDE_MG = 30.0;
    
    private static final double HEARTBEAT_FREQUENCY_HZ = 1.2;
    private static final double HEARTBEAT_AMPLITUDE_MG = 5.0;
    
    private static double baselineX = 0.0f;
    private static double baselineY = 0.0f;
    private static double baselineZ = 9.81f;
    
    private static final int SENSOR_TYPE_ACCELEROMETER = 1;
    private static final int SENSOR_TYPE_GYROSCOPE = 4;
    private static final int SENSOR_TYPE_GRAVITY = 9;
    private static final int SENSOR_TYPE_LINEAR_ACCELERATION = 10;

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (initialized) {
            XposedBridge.log(LOG_TAG + " already initialized");
            return;
        }

        try {
            hookSensorEventQueue(lpparam);
            initializeBaselines();
            initialized = true;
            XposedBridge.log(LOG_TAG + " initialized successfully");
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " Failed to initialize: " + e.getMessage());
        }
    }

    private static void initializeBaselines() {
        baselineX = random.nextGaussian() * 0.1;
        baselineY = random.nextGaussian() * 0.1;
        baselineZ = 9.81 + random.nextGaussian() * 0.05;
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
                            injectStochasticNoise(sensorType, values, param.thisObject);
                        }
                    } catch (Exception e) {
                    }
                }
            });

            XposedBridge.log(LOG_TAG + " Hooked SensorEventQueue.dispatchSensorEvent()");
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " Failed to hook SensorEventQueue: " + e.getMessage());
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

    private static void injectStochasticNoise(int sensorType, float[] values, Object eventQueue) {
        long currentTime = System.currentTimeMillis() - sessionStartTime;
        double timeSeconds = currentTime / 1000.0;

        switch (sensorType) {
            case SENSOR_TYPE_ACCELEROMETER:
                injectAccelerometerStochasticNoise(values, timeSeconds);
                break;
            case SENSOR_TYPE_GYROSCOPE:
                injectGyroscopeStochasticNoise(values, timeSeconds);
                break;
            case SENSOR_TYPE_GRAVITY:
                injectGravityStochasticNoise(values, timeSeconds);
                break;
            case SENSOR_TYPE_LINEAR_ACCELERATION:
                injectLinearAccelerationStochasticNoise(values, timeSeconds);
                break;
            default:
                break;
        }
    }

    private static void injectAccelerometerStochasticNoise(float[] values, double timeSeconds) {
        if (values.length < 3) return;

        double physiologicalNoiseX = calculatePhysiologicalNoise(timeSeconds, 0);
        double physiologicalNoiseY = calculatePhysiologicalNoise(timeSeconds, 1);
        double physiologicalNoiseZ = calculatePhysiologicalNoise(timeSeconds, 2);

        double stochasticNoiseX = generateStochasticNoise();
        double stochasticNoiseY = generateStochasticNoise();
        double stochasticNoiseZ = generateStochasticNoise();

        double brownianMotionX = calculateBrownianMotion(timeSeconds, 0);
        double brownianMotionY = calculateBrownianMotion(timeSeconds, 1);
        double brownianMotionZ = calculateBrownianMotion(timeSeconds, 2);

        values[0] = (float) (baselineX + physiologicalNoiseX + stochasticNoiseX + brownianMotionX);
        values[1] = (float) (baselineY + physiologicalNoiseY + stochasticNoiseY + brownianMotionY);
        values[2] = (float) (baselineZ + physiologicalNoiseZ + stochasticNoiseZ + brownianMotionZ);

        values[0] = clamp(values[0], -15.0f, 15.0f);
        values[1] = clamp(values[1], -15.0f, 15.0f);
        values[2] = clamp(values[2], -5.0f, 25.0f);
    }

    private static void injectGyroscopeStochasticNoise(float[] values, double timeSeconds) {
        if (values.length < 3) return;

        double driftX = 0.001 * Math.sin(timeSeconds / 15.0);
        double driftY = 0.001 * Math.cos(timeSeconds / 18.0);
        double driftZ = 0.001 * Math.sin(timeSeconds / 21.0);

        double stochasticX = generateStochasticNoise() * 0.003;
        double stochasticY = generateStochasticNoise() * 0.003;
        double stochasticZ = generateStochasticNoise() * 0.003;

        double angularDriftX = calculateAngularDrift(timeSeconds, 0);
        double angularDriftY = calculateAngularDrift(timeSeconds, 1);
        double angularDriftZ = calculateAngularDrift(timeSeconds, 2);

        values[0] = (float) (values[0] + driftX + stochasticX + angularDriftX);
        values[1] = (float) (values[1] + driftY + stochasticY + angularDriftY);
        values[2] = (float) (values[2] + driftZ + stochasticZ + angularDriftZ);

        values[0] = clamp(values[0], -0.8f, 0.8f);
        values[1] = clamp(values[1], -0.8f, 0.8f);
        values[2] = clamp(values[2], -0.8f, 0.8f);
    }

    private static void injectGravityStochasticNoise(float[] values, double timeSeconds) {
        if (values.length < 3) return;

        double gravityNoiseScale = 0.02;
        double noiseX = (random.nextGaussian() * gravityNoiseScale);
        double noiseY = (random.nextGaussian() * gravityNoiseScale);
        double noiseZ = (random.nextGaussian() * gravityNoiseScale);

        values[0] = (float) (values[0] + noiseX);
        values[1] = (float) (values[1] + noiseY);
        values[2] = (float) (values[2] + noiseZ);

        values[0] = clamp(values[0], -1.1f, 1.1f);
        values[1] = clamp(values[1], -1.1f, 1.1f);
        values[2] = clamp(values[2], 8.5f, 11.0f);
    }

    private static void injectLinearAccelerationStochasticNoise(float[] values, double timeSeconds) {
        if (values.length < 3) return;

        double noiseScale = 0.05;
        double impulseX = calculateImpulseNoise(timeSeconds, 0);
        double impulseY = calculateImpulseNoise(timeSeconds, 1);
        double impulseZ = calculateImpulseNoise(timeSeconds, 2);

        values[0] = (float) (values[0] + (random.nextGaussian() * noiseScale) + impulseX);
        values[1] = (float) (values[1] + (random.nextGaussian() * noiseScale) + impulseY);
        values[2] = (float) (values[2] + (random.nextGaussian() * noiseScale) + impulseZ);

        values[0] = clamp(values[0], -12.0f, 12.0f);
        values[1] = clamp(values[1], -12.0f, 12.0f);
        values[2] = clamp(values[2], -12.0f, 12.0f);
    }

    private static double calculatePhysiologicalNoise(double timeSeconds, int axis) {
        double phaseShift = axis * Math.PI / 3.0;
        
        double tremorComponent = TREMOR_AMPLITUDE_MG * Math.sin(2 * Math.PI * TREMOR_FREQUENCY_HZ * timeSeconds + phaseShift);
        
        double breathingComponent = BREATHING_AMPLITUDE_MG * Math.sin(2 * Math.PI * BREATHING_FREQUENCY_HZ * timeSeconds + phaseShift);
        
        double heartbeatComponent = HEARTBEAT_AMPLITUDE_MG * Math.sin(2 * Math.PI * HEARTBEAT_FREQUENCY_HZ * timeSeconds + phaseShift);
        
        double microTremorPhase = timeSeconds * 12.0 + axis;
        double microTremor = 3.0 * Math.sin(microTremorPhase) + 1.5 * Math.sin(microTremorPhase * 2.3);
        
        return (tremorComponent + breathingComponent + heartbeatComponent + microTremor) / 1000.0;
    }

    private static double generateStochasticNoise() {
        double whiteNoise = random.nextGaussian() * NOISE_FLOOR_MG;
        
        double pinkNoiseComponent = 0.0;
        for (int i = 1; i <= 5; i++) {
            pinkNoiseComponent += (random.nextGaussian() * NOISE_FLOOR_MG) / Math.sqrt(i);
        }
        
        return (whiteNoise + pinkNoiseComponent) / 1000.0;
    }

    private static double calculateBrownianMotion(double timeSeconds, int axis) {
        double walk = 0.0;
        double dt = 0.1;
        int steps = (int) (timeSeconds / dt);
        
        double position = 0.0;
        for (int i = 0; i < steps; i++) {
            position += random.nextGaussian() * 0.01 * Math.sqrt(dt);
        }
        
        return position;
    }

    private static double calculateAngularDrift(double timeSeconds, int axis) {
        double driftRate = 0.0001 * (axis + 1);
        return driftRate * Math.sin(timeSeconds / 10.0) * (random.nextDouble() * 0.5 + 0.5);
    }

    private static double calculateImpulseNoise(double timeSeconds, int axis) {
        double impulseInterval = 5.0 + random.nextDouble() * 10.0;
        double phaseInInterval = (timeSeconds % impulseInterval) / impulseInterval;
        
        if (phaseInInterval < 0.1) {
            double impulseStrength = (0.1 - phaseInInterval) * 10.0;
            return impulseStrength * random.nextGaussian() * 0.05;
        }
        return 0.0;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static void setTremorAmplitude(double amplitudeMg) {
    }

    public static void setNoiseFloor(double noiseMg) {
    }

    public static double getBaselineX() {
        return baselineX;
    }

    public static double getBaselineY() {
        return baselineY;
    }

    public static double getBaselineZ() {
        return baselineZ;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
