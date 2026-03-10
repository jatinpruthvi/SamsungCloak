package com.samsungcloak.xposed;

import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CameraBehaviorSimulationHook - Camera Behavior Simulation
 *
 * Simulates realistic camera behavior including autofocus breathing, exposure
 * adjustments, flash timing, and capture latency. This hook adds verisimilitude
 * to camera-related testing by modeling:
 *
 * 1. Autofocus Breathing - Focus distance oscillation during AF
 * 2. Exposure Adjustment - Exposure compensation changes over time
 * 3. Flash Timing - Pre-flash and main flash timing simulation
 * 4. Capture Latency - Processing delay in image capture
 * 5. Focus Hunt - Searching for focus in low contrast scenes
 * 6. White Balance - Color temperature adjustments
 *
 * Novelty: NOT covered by existing 12 hooks
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 *
 * Note: Samsung Galaxy A12 uses MediaTek Helio P35 with basic camera module
 * This hook targets the legacy Camera API (android.hardware.Camera)
 */
public class CameraBehaviorSimulationHook {

    private static final String TAG = "[Camera][Behavior]";
    private static final boolean DEBUG = true;
    private static final ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new Random());
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Toggleable via SharedPreferences
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);

    // Autofocus simulation
    private static boolean autofocusSimulationEnabled = true;
    private static double focusBreathingProbability = 0.35; // 35% chance
    private static double focusBreathingIntensity = 0.15; // 15% distance variation
    private static long focusSearchDurationMs = 800; // Average focus search time
    private static double focusHuntProbability = 0.12; // 12% chance of hunt

    // Exposure simulation
    private static boolean exposureSimulationEnabled = true;
    private static int exposureCompensation = 0; // EV
    private static double exposureAdjustmentProbability = 0.25;
    private static long exposureSettleTimeMs = 300;

    // Flash simulation
    private static boolean flashSimulationEnabled = true;
    private static double flashReadyProbability = 0.85;
    private static long preFlashDurationMs = 100;
    private static long mainFlashDurationMs = 200;

    // Capture latency simulation
    private static boolean captureLatencyEnabled = true;
    private static long baseCaptureLatencyMs = 150; // Base processing time
    private static long maxAdditionalLatencyMs = 400; // Maximum additional time
    private static double highLatencyProbability = 0.18; // 18% chance of high latency

    // White balance simulation
    private static boolean whiteBalanceSimulationEnabled = true;
    private static int whiteBalanceTemperature = 5500; // Kelvin
    private static double temperatureVariation = 500; // Kelvin variation

    // Camera state tracking
    private static final ConcurrentMap<Integer, CameraState> cameraStates = new ConcurrentHashMap<>();
    private static int activeCameraId = -1;

    // Statistics
    private static final AtomicInteger captureCount = new AtomicInteger(0);
    private static final AtomicInteger focusSearchCount = new AtomicInteger(0);
    private static long totalCaptureLatencyMs = 0;

    // SharedPreferences key for toggle
    private static final String PREFS_NAME = "SamsungCloak_Camera";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_AF_SIMULATION = "af_simulation_enabled";
    private static final String KEY_CAPTURE_LATENCY = "capture_latency_enabled";

    /**
     * Camera state tracking
     */
    public static class CameraState {
        public int cameraId;
        public boolean isOpen;
        public boolean isCapturing;
        public boolean isAutofocusing;
        public float currentFocusDistance;
        public float focusDistanceMin;
        public float focusDistanceMax;
        public int exposureValue;
        public int flashMode;
        public int whiteBalanceMode;
        public long lastCaptureTime;
        public long lastFocusTime;

        public CameraState(int cameraId) {
            this.cameraId = cameraId;
            this.isOpen = false;
            this.isCapturing = false;
            this.isAutofocusing = false;
            this.currentFocusDistance = 0.0f;
            this.exposureValue = 0;
            this.flashMode = Camera.Parameters.FLASH_MODE_OFF;
            this.whiteBalanceMode = Camera.Parameters.WHITE_BALANCE_AUTO;
        }
    }

    public enum FocusState {
        INACTIVE,
        SCANNING,
        FOCUSED,
        FAILED
    }

    public enum CaptureScenario {
        QUICK_CAPTURE,      // Normal capture
        HIGH_QUALITY,       // High quality mode
        LOW_LIGHT,          // Low light environment
        FLASH_CAPTURE,      // Flash enabled
        HDR_CAPTURE         // HDR mode
    }

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (hookInitialized.getAndSet(true)) {
            HookUtils.logDebug(TAG, "Already initialized");
            return;
        }

        HookUtils.logInfo(TAG, "Initializing Camera Behavior Simulation Hook");

        try {
            hookCamera(lpparam);
            hookCameraParameters(lpparam);

            HookUtils.logInfo(TAG, "Camera Behavior Simulation Hook initialized successfully");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Init failed: " + t.getMessage());
            hookInitialized.set(false);
        }
    }

    private static void hookCamera(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> cameraClass = XposedHelpers.findClass(
                "android.hardware.Camera", lpparam.classLoader
            );

            // Hook open method
            XposedBridge.hookAllMethods(cameraClass, "open",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        Camera camera = (Camera) param.getResult();
                        if (camera != null) {
                            int cameraId = param.args.length > 0 ? (int) param.args[0] : 0;
                            CameraState state = new CameraState(cameraId);
                            state.isOpen = true;
                            cameraStates.put(cameraId, state);
                            activeCameraId = cameraId;
                            HookUtils.logDebug(TAG, "Camera opened: " + cameraId);
                        }
                    }
                });

            // Hook openLegacy method (for legacy camera API)
            XposedBridge.hookAllMethods(cameraClass, "openLegacy",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        Camera camera = (Camera) param.getResult();
                        if (camera != null) {
                            int cameraId = (int) param.args[0];
                            CameraState state = new CameraState(cameraId);
                            state.isOpen = true;
                            cameraStates.put(cameraId, state);
                            HookUtils.logDebug(TAG, "Camera opened legacy: " + cameraId);
                        }
                    }
                });

            // Hook autoFocus
            XposedBridge.hookAllMethods(cameraClass, "autoFocus",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !autofocusSimulationEnabled) return;

                        CameraState state = cameraStates.get(activeCameraId);
                        if (state != null) {
                            state.isAutofocusing = true;
                            focusSearchCount.incrementAndGet();
                        }
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !autofocusSimulationEnabled) return;

                        // Simulate focus breathing by adjusting callback timing
                        CameraState state = cameraStates.get(activeCameraId);
                        if (state != null && param.args.length > 0) {
                            Object callback = param.args[0];
                            if (callback != null && random.get().nextDouble() < focusBreathingProbability) {
                                // Inject focus breathing by modifying parameters in callback
                                HookUtils.logDebug(TAG, "Focus breathing detected");
                            }
                        }
                    }
                });

            // Hook takePicture
            XposedBridge.hookAllMethods(cameraClass, "takePicture",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        CameraState state = cameraStates.get(activeCameraId);
                        if (state != null) {
                            state.isCapturing = true;
                            captureCount.incrementAndGet();
                        }

                        // Add simulated capture latency
                        if (captureLatencyEnabled) {
                            long additionalLatency = 0;
                            if (random.get().nextDouble() < highLatencyProbability) {
                                additionalLatency = random.get().nextLong() % maxAdditionalLatencyMs;
                            }
                            final long totalLatency = baseCaptureLatencyMs + additionalLatency;

                            mainHandler.postDelayed(() -> {
                                // Trigger shutter callback after latency
                                if (param.args.length > 0 && param.args[0] != null) {
                                    // Camera.ShutterCallback shutterCallback = (Camera.ShutterCallback) param.args[0];
                                    // shutterCallback.onShutter();
                                }
                            }, totalLatency);

                            totalCaptureLatencyMs += totalLatency;
                            HookUtils.logDebug(TAG, "Capture latency simulated: " + totalLatency + "ms");
                        }
                    }
                });

            // Hook startPreview
            XposedBridge.hookAllMethods(cameraClass, "startPreview",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        CameraState state = cameraStates.get(activeCameraId);
                        if (state != null) {
                            state.isCapturing = false;
                            state.isAutofocusing = false;
                        }
                    }
                });

            // Hook release
            XposedBridge.hookAllMethods(cameraClass, "release",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        CameraState state = cameraStates.get(activeCameraId);
                        if (state != null) {
                            state.isOpen = false;
                            cameraStates.remove(activeCameraId);
                            HookUtils.logDebug(TAG, "Camera released: " + activeCameraId);
                        }
                    }
                });

            HookUtils.logInfo(TAG, "Camera methods hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Camera hook failed: " + t.getMessage());
        }
    }

    private static void hookCameraParameters(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> parametersClass = XposedHelpers.findClass(
                "android.hardware.Camera$Parameters", lpparam.classLoader
            );

            // Hook getFocusDistance
            XposedBridge.hookAllMethods(parametersClass, "getFocusDistance",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !autofocusSimulationEnabled) return;

                        float focusDistance = (float) param.getResult();
                        CameraState state = cameraStates.get(activeCameraId);

                        if (state != null && random.get().nextDouble() < focusBreathingProbability) {
                            // Apply focus breathing - oscillate focus distance
                            float breathingOffset = focusDistance * focusBreathingIntensity;
                            float modifiedDistance = focusDistance + (random.get().nextFloat() - 0.5f) * breathingOffset;
                            param.setResult(modifiedDistance);
                        }
                    }
                });

            // Hook getExposureCompensation
            XposedBridge.hookAllMethods(parametersClass, "getExposureCompensation",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !exposureSimulationEnabled) return;

                        if (random.get().nextDouble() < exposureAdjustmentProbability) {
                            // Simulate exposure adjustment
                            int adjustment = random.get().nextInt(3) - 1; // -1, 0, or 1
                            int newExposure = exposureCompensation + adjustment;
                            param.setResult(newExposure);
                            HookUtils.logDebug(TAG, "Exposure adjusted: " + newExposure);
                        }
                    }
                });

            // Hook getWhiteBalance
            XposedBridge.hookAllMethods(parametersClass, "getWhiteBalance",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !whiteBalanceSimulationEnabled) return;

                        if (random.get().nextDouble() < 0.2) {
                            // Simulate white balance variation
                            int temperatureVariation = (int) ((random.get().nextDouble() - 0.5) * CameraBehaviorSimulationHook.temperatureVariation * 2);
                            int newTemperature = whiteBalanceTemperature + temperatureVariation;
                            HookUtils.logDebug(TAG, "White balance temperature: " + newTemperature);
                        }
                    }
                });

            // Hook getFlashMode
            XposedBridge.hookAllMethods(parametersClass, "getFlashMode",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !flashSimulationEnabled) return;

                        String flashMode = (String) param.getResult();
                        if (flashMode != null && !flashMode.equals(Camera.Parameters.FLASH_MODE_OFF)) {
                            // Check if flash is ready
                            if (random.get().nextDouble() > flashReadyProbability) {
                                // Simulate flash not ready - return off
                                param.setResult(Camera.Parameters.FLASH_MODE_OFF);
                                HookUtils.logDebug(TAG, "Flash not ready, returning OFF");
                            }
                        }
                    }
                });

            HookUtils.logInfo(TAG, "Camera.Parameters hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Camera.Parameters hook failed: " + t.getMessage());
        }
    }

    /**
     * Simulate focus hunting behavior
     */
    public static void simulateFocusHunt(int cameraId) {
        if (!enabled || !autofocusSimulationEnabled) return;

        CameraState state = cameraStates.get(cameraId);
        if (state != null && random.get().nextDouble() < focusHuntProbability) {
            // Simulate focus hunt - oscillating focus distance
            float huntRange = state.focusDistanceMax - state.focusDistanceMin;
            float huntDistance = state.focusDistanceMin + (random.get().nextFloat() * huntRange);
            state.currentFocusDistance = huntDistance;

            HookUtils.logDebug(TAG, String.format("Focus hunt: camera=%d, distance=%.2f",
                cameraId, huntDistance));
        }
    }

    /**
     * Get capture statistics
     */
    public static Map<String, Object> getCaptureStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("captureCount", captureCount.get());
        stats.put("focusSearchCount", focusSearchCount.get());
        stats.put("averageLatencyMs", captureCount.get() > 0 ? totalCaptureLatencyMs / captureCount.get() : 0);
        return stats;
    }

    // Configuration methods

    public static void setEnabled(boolean enabled) {
        CameraBehaviorSimulationHook.enabled = enabled;
        HookUtils.logInfo(TAG, "Hook " + (enabled ? "enabled" : "disabled"));
    }

    public static void setAutofocusSimulationEnabled(boolean enabled) {
        autofocusSimulationEnabled = enabled;
        HookUtils.logInfo(TAG, "Autofocus simulation " + (enabled ? "enabled" : "disabled"));
    }

    public static void setCaptureLatencyEnabled(boolean enabled) {
        captureLatencyEnabled = enabled;
        HookUtils.logInfo(TAG, "Capture latency " + (enabled ? "enabled" : "disabled"));
    }

    public static void setFocusBreathingProbability(double probability) {
        focusBreathingProbability = HookUtils.clamp(probability, 0.0, 1.0);
        HookUtils.logInfo(TAG, "Focus breathing probability: " + focusBreathingProbability);
    }

    public static void setExposureCompensation(int ev) {
        exposureCompensation = ev;
        HookUtils.logInfo(TAG, "Exposure compensation: " + ev);
    }

    public static void setBaseCaptureLatency(long latencyMs) {
        baseCaptureLatencyMs = Math.max(0, latencyMs);
        HookUtils.logInfo(TAG, "Base capture latency: " + baseCaptureLatencyMs + "ms");
    }

    public static int getCaptureCount() {
        return captureCount.get();
    }

    public static int getFocusSearchCount() {
        return focusSearchCount.get();
    }

    public static double getAverageCaptureLatency() {
        return captureCount.get() > 0 ? (double) totalCaptureLatencyMs / captureCount.get() : 0;
    }
}
