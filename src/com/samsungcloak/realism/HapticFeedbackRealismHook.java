package com.samsungcloak.realism;

import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.HapticFeedbackConstants;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HapticFeedbackRealismHook - Vibration intensity variation and motor inertia simulation
 * 
 * Targets: Samsung Galaxy A12 (SM-A125U) Android 10/11
 * 
 * This hook addresses the realism dimension of haptic feedback fidelity - humans perceive
 * and produce variable vibration patterns. The hook simulates:
 * - Motor inertia (delayed start/stop)
 * - Intensity variation based on finger pressure
 * - Battery-coupled amplitude reduction
 * - Temperature-dependent performance degradation
 * - Aging hardware simulation (increased latency)
 */
public class HapticFeedbackRealismHook {
    private static final String TAG = "HapticFeedbackRealism";
    private static final String PACKAGE_NAME = "com.samsungcloak.realism";
    
    // Configuration keys
    private static final String KEY_ENABLED = "haptic_enabled";
    private static final String KEY_INERTIA_ENABLED = "haptic_inertia_enabled";
    private static final String KEY_INTENSITY_VARIATION = "haptic_intensity_variation";
    private static final String KEY_BATTERY_COUPLING = "haptic_battery_coupling";
    private static final String KEY_THERMAL_THROTTLING = "haptic_thermal_throttling";
    private static final String KEY_HARDWARE_AGING = "haptic_hardware_aging";
    
    // Device-specific constants (SM-A125U)
    private static final float BASE_AMPLITUDE = 1.0f;
    private static final float MIN_AMPLITUDE = 0.15f;
    private static final long INERTIA_DELAY_MS = 12;  // Motor start delay
    private static final long INERTIA_SETTLE_MS = 8;   // Motor stop delay
    
    // State
    private static SharedPreferences sPrefs;
    private static boolean sEnabled = true;
    private static boolean sInertiaEnabled = true;
    private static float sIntensityVariation = 0.25f;
    private static boolean sBatteryCoupling = true;
    private static boolean sThermalThrottling = true;
    private static boolean sHardwareAging = false;
    
    // Runtime state
    private static final Random sRandom = new Random();
    private static final ConcurrentHashMap<Long, Long> sActiveVibrations = new ConcurrentHashMap<>();
    private static long sModuleLoadTime = System.currentTimeMillis();
    private static float sMotorHealth = 1.0f;
    private static float sCurrentTemperature = 25.0f;  // Celsius
    
    /**
     * Initialize the hook with preferences
     */
    public static void init(SharedPreferences prefs) {
        sPrefs = prefs;
        reloadSettings();
    }
    
    /**
     * Reload settings from preferences
     */
    public static void reloadSettings() {
        if (sPrefs == null) return;
        
        sEnabled = sPrefs.getBoolean(KEY_ENABLED, true);
        sInertiaEnabled = sPrefs.getBoolean(KEY_INERTIA_ENABLED, true);
        sIntensityVariation = sPrefs.getFloat(KEY_INTENSITY_VARIATION, 0.25f);
        sBatteryCoupling = sPrefs.getBoolean(KEY_BATTERY_COUPLING, true);
        sThermalThrottling = sPrefs.getBoolean(KEY_THERMAL_THROTTLING, true);
        sHardwareAging = sPrefs.getBoolean(KEY_HARDWARE_AGING, false);
        
        // Calculate initial motor health based on aging setting
        if (sHardwareAging) {
            long uptimeHours = (System.currentTimeMillis() - sModuleLoadTime) / (1000 * 60 * 60);
            sMotorHealth = Math.max(0.6f, 1.0f - (uptimeHours * 0.005f));  // 0.5% degradation per hour
        } else {
            sMotorHealth = 1.0f;
        }
    }
    
    /**
     * Hook the Vibrator service
     */
    public static void hookVibrator(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!sEnabled) return;
        
        try {
            // Hook Vibrator.vibrate() methods
            Class<?> vibratorClass = XposedHelpers.findClass("android.os.Vibrator", lpparam.classLoader);
            
            // Hook single vibration
            XposedBridge.hookAllMethods(vibratorClass, "vibrate", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!sEnabled) return;
                    
                    long milliseconds = (Long) param.args[0];
                    int audioAttributes = -1;
                    
                    // Check for audioAttributes parameter (Android 12+)
                    if (param.args.length > 1 && param.args[1] instanceof Integer) {
                        audioAttributes = (Integer) param.args[1];
                    }
                    
                    // Apply realistic modification
                    long[] pattern = {0, milliseconds};
                    param.args[0] = modifyVibrationPattern(pattern, audioAttributes)[1];
                }
            });
            
            // Hook vibration pattern
            XposedBridge.hookAllMethods(vibratorClass, "vibrate", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!sEnabled) return;
                    
                    Object patternObj = param.args[0];
                    int repeat = (Integer) param.args[1];
                    int audioAttributes = -1;
                    
                    if (param.args.length > 2 && param.args[2] instanceof Integer) {
                        audioAttributes = (Integer) param.args[2];
                    }
                    
                    if (patternObj instanceof long[]) {
                        long[] pattern = (long[]) patternObj;
                        param.args[0] = modifyVibrationPattern(pattern, audioAttributes);
                    }
                }
            });
            
            // Hook hasVibrator()
            XposedBridge.hookAllMethods(vibratorClass, "hasVibrator", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    return true;  // Always report vibrator present
                }
            });
            
            XposedBridge.log(TAG + ": Vibrator hooks installed successfully");
            
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Error hooking Vibrator: " + e.getMessage());
        }
    }
    
    /**
     * Hook the VibratorManager (Android 11+)
     */
    public static void hookVibratorManager(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!sEnabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return;
        
        try {
            Class<?> vibratorManagerClass = XposedHelpers.findClass(
                "android.os.VibratorManager", lpparam.classLoader);
            
            // Hook getDefaultVibrator()
            XposedBridge.hookAllMethods(vibratorManagerClass, "getDefaultVibrator", 
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        // Could wrap the vibrator here if needed
                    }
                });
            
            XposedBridge.log(TAG + ": VibratorManager hooks installed");
            
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Error hooking VibratorManager: " + e.getMessage());
        }
    }
    
    /**
     * Hook VibrationEffect factory methods
     */
    public static void hookVibrationEffect(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!sEnabled) return;
        
        try {
            Class<?> vibrationEffectClass = XposedHelpers.findClass(
                "android.os.VibrationEffect", lpparam.classLoader);
            
            // Hook createOneShot()
            XposedBridge.hookAllMethods(vibrationEffectClass, "createOneShot", 
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!sEnabled || param.getResult() == null) return;
                        
                        long duration = (Long) param.args[0];
                        float amplitude = (Float) param.args[1];
                        
                        // Modify amplitude based on realism factors
                        float modifiedAmplitude = applyIntensityVariation(amplitude);
                        modifiedAmplitude = applyBatteryCoupling(modifiedAmplitude);
                        modifiedAmplitude = applyThermalThrottling(modifiedAmplitude);
                        modifiedAmplitude = applyMotorHealth(modifiedAmplitude);
                        
                        if (modifiedAmplitude != amplitude) {
                            // Create new effect with modified amplitude
                            param.setResult(VibrationEffect.createOneShot(
                                duration, 
                                (int) Math.round(modifiedAmplitude * 255)
                            ));
                        }
                    }
                });
            
            // Hook createWaveform()
            XposedBridge.hookAllMethods(vibrationEffectClass, "createWaveform",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!sEnabled || param.getResult() == null) return;
                        
                        long[] timings = (long[]) param.args[0];
                        int[] amplitudes = (int[]) param.args[1];
                        
                        // Apply amplitude modifications
                        float[] modifiedAmplitudes = applyWaveformModifications(amplitudes);
                        
                        if (modifiedAmplitudes != null) {
                            int[] intAmplitudes = new int[modifiedAmplitudes.length];
                            for (int i = 0; i < modifiedAmplitudes.length; i++) {
                                intAmplitudes[i] = (int) Math.round(modifiedAmplitudes[i] * 255);
                            }
                            
                            param.setResult(VibrationEffect.createWaveform(
                                timings, intAmplitudes, -1));
                        }
                    }
                });
            
            XposedBridge.log(TAG + ": VibrationEffect hooks installed");
            
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Error hooking VibrationEffect: " + e.getMessage());
        }
    }
    
    /**
     * Hook View.performHapticFeedback()
     */
    public static void hookViewHapticFeedback(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!sEnabled) return;
        
        try {
            Class<?> viewClass = XposedHelpers.findClass("android.view.View", lpparam.classLoader);
            
            XposedBridge.hookAllMethods(viewClass, "performHapticFeedback", 
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!sEnabled) return;
                        
                        int feedbackConstant = (Integer) param.args[0];
                        
                        // Apply intensity variation based on feedback type
                        float intensityMod = getFeedbackTypeIntensity(feedbackConstant);
                        intensityMod *= (1.0f + (sRandom.nextFloat() - 0.5f) * sIntensityVariation);
                        
                        // Modify the feedback flag to use custom amplitude if available
                        if (param.args.length > 1) {
                            int flags = (Integer) param.args[1];
                            // Could inject amplitude here if we had access to custom feedback
                        }
                    }
                });
            
            XposedBridge.log(TAG + ": View.performHapticFeedback hooks installed");
            
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Error hooking View haptic feedback: " + e.getMessage());
        }
    }
    
    /**
     * Modify vibration pattern to include motor inertia effects
     */
    private static long[] modifyVibrationPattern(long[] pattern, int audioAttributes) {
        if (pattern == null || pattern.length < 2) return pattern;
        
        long[] modified = pattern.clone();
        
        // Apply inertia delay at start
        if (sInertiaEnabled && modified.length > 1) {
            long actualVibration = modified[1];
            
            // Motor inertia: delay start, extend end
            float inertiaFactor = getInertiaFactor();
            
            // Insert delay at beginning
            long delay = (long) (INERTIA_DELAY_MS * inertiaFactor);
            modified[0] = delay;
            
            // Add settling time at end
            long settleTime = (long) (INERTIA_SETTLE_MS * inertiaFactor);
            modified[1] = actualVibration + settleTime;
            
            // Reduce amplitude for short vibrations due to inertia
            if (actualVibration < 50) {
                float reduction = 1.0f - (0.3f * (1.0f - inertiaFactor));
                // Note: Can't actually reduce amplitude in timing-only API
            }
        }
        
        // Apply battery coupling
        if (sBatteryCoupling) {
            float batteryFactor = getBatteryCouplingFactor();
            // Apply to pattern by extending duration
            if (batteryFactor < 0.8f) {
                modified[1] = (long) (modified[1] / batteryFactor);
            }
        }
        
        return modified;
    }
    
    /**
     * Apply intensity variation to amplitude
     */
    private static float applyIntensityVariation(float amplitude) {
        if (amplitude <= 0 || amplitude > 1.0f) return amplitude;
        
        // Human-like variation: ±variation%
        float variation = (sRandom.nextFloat() - 0.5f) * 2 * sIntensityVariation;
        float result = amplitude * (1.0f + variation);
        
        return Math.max(MIN_AMPLITUDE, Math.min(BASE_AMPLITUDE, result));
    }
    
    /**
     * Apply battery coupling factor
     */
    private static float applyBatteryCoupling(float amplitude) {
        if (!sBatteryCoupling) return amplitude;
        
        // Simulated battery level (would integrate with BatterySimulator in real impl)
        float batteryLevel = getSimulatedBatteryLevel();
        
        // Battery-coupled amplitude reduction
        float factor = 1.0f;
        if (batteryLevel < 0.2f) {
            factor = 0.3f + (batteryLevel * 2);  // 30-70% at <20%
        } else if (batteryLevel < 0.5f) {
            factor = 0.7f + ((batteryLevel - 0.2f) * 1.0f);  // 70-100% at 20-50%
        }
        
        return amplitude * factor;
    }
    
    /**
     * Apply thermal throttling
     */
    private static float applyThermalThrottling(float amplitude) {
        if (!sThermalThrottling) return amplitude;
        
        // Temperature-dependent performance (Samsung thermal limits)
        float factor = 1.0f;
        
        if (sCurrentTemperature > 45.0f) {
            // Heavy throttling above 45°C
            factor = 0.3f;
        } else if (sCurrentTemperature > 40.0f) {
            // Moderate throttling 40-45°C
            factor = 0.5f + ((45.0f - sCurrentTemperature) / 10.0f) * 0.3f;
        } else if (sCurrentTemperature > 35.0f) {
            // Light throttling 35-40°C
            factor = 0.7f + ((40.0f - sCurrentTemperature) / 10.0f) * 0.2f;
        }
        
        return amplitude * factor;
    }
    
    /**
     * Apply motor health (aging) factor
     */
    private static float applyMotorHealth(float amplitude) {
        return amplitude * sMotorHealth;
    }
    
    /**
     * Apply waveform modifications
     */
    private static float[] applyWaveformModifications(int[] amplitudes) {
        if (amplitudes == null || amplitudes.length == 0) return null;
        
        float[] result = new float[amplitudes.length];
        
        for (int i = 0; i < amplitudes.length; i++) {
            float amp = amplitudes[i] / 255.0f;
            
            // Apply all factors
            amp = applyIntensityVariation(amp);
            amp = applyBatteryCoupling(amp);
            amp = applyThermalThrottling(amp);
            amp = applyMotorHealth(amp);
            
            result[i] = amp;
        }
        
        return result;
    }
    
    /**
     * Get inertia factor based on motor health
     */
    private static float getInertiaFactor() {
        // Worse motor health = more inertia
        return 0.5f + (sMotorHealth * 0.5f);
    }
    
    /**
     * Get simulated battery level
     */
    private static float getSimulatedBatteryLevel() {
        // In real implementation, would integrate with BatterySimulator
        // Using reasonable default for SM-A125U (5000mAh battery)
        long uptimeMinutes = (System.currentTimeMillis() - sModuleLoadTime) / 60000;
        float drainPerMin = 0.08f;  // ~5% per hour typical usage
        float initialLevel = 0.85f;  // Start at 85%
        
        return Math.max(0.1f, initialLevel - (uptimeMinutes * drainPerMin / 100f));
    }
    
    /**
     * Get intensity modifier based on feedback type
     */
    private static float getFeedbackTypeIntensity(int feedbackConstant) {
        switch (feedbackConstant) {
            case HapticFeedbackConstants.CONFIRM:
            case HapticFeedbackConstants.LONG_PRESS:
                return 1.0f;
                
            case HapticFeedbackConstants.CLOCK_TICK:
            case HapticFeedbackConstants.KEYBOARD_TAP:
                return 0.4f;
                
            case HapticFeedbackConstants.TEXT_HANDLE_MOVE:
            case HapticFeedbackConstants.SCROLL:
                return 0.25f;
                
            case HapticFeedbackConstants.VIRTUAL_KEY:
            case HapticFeedbackConstants.REJECT:
            case HapticFeedbackConstants.CLEAR:
                return 0.7f;
                
            default:
                return 0.6f;
        }
    }
    
    /**
     * Update temperature (called from thermal monitoring)
     */
    public static void updateTemperature(float temperature) {
        sCurrentTemperature = temperature;
    }
    
    /**
     * Get current haptic feedback state for cross-hook coherence
     */
    public static HapticFeedbackState getState() {
        HapticFeedbackState state = new HapticFeedbackState();
        state.enabled = sEnabled;
        state.motorHealth = sMotorHealth;
        state.currentTemperature = sCurrentTemperature;
        state.batteryLevel = getSimulatedBatteryLevel();
        state.activeVibrations = sActiveVibrations.size();
        return state;
    }
    
    /**
     * State container for cross-hook coherence
     */
    public static class HapticFeedbackState {
        public boolean enabled;
        public float motorHealth;
        public float currentTemperature;
        public float batteryLevel;
        public int activeVibrations;
    }
}
