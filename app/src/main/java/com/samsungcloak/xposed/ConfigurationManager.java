package com.samsungcloak.xposed;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * ConfigurationManager - Manages SharedPreferences for all hooks
 * 
 * Provides centralized configuration management with:
 * - Individual hook enable/disable flags
 * - Intensity sliders (0.0-1.0) per hook
 * - Global context coordination settings
 * - Default values for all hooks
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class ConfigurationManager {

    private static final String TAG = "[Config]";
    private static final String PREFIX_ENABLED = "hook_enabled_";
    private static final String PREFIX_INTENSITY = "hook_intensity_";
    private static final String PREFIX_PARAM = "hook_param_";

    // Global settings
    public static final String KEY_GLOBAL_COORDINATION = "global_coordination_enabled";
    public static final String KEY_CONTEXT_AWARENESS = "context_awareness_enabled";
    public static final String KEY_TEMPORAL_ALIGNMENT = "temporal_alignment_enabled";

    // Default intensities for all hooks
    private static final java.util.Map<String, Float> DEFAULT_INTENSITIES = new java.util.HashMap<>();
    static {
        // Original hooks 1-12
        DEFAULT_INTENSITIES.put("hook_01", 0.5f);
        DEFAULT_INTENSITIES.put("hook_02", 0.5f);
        DEFAULT_INTENSITIES.put("hook_03", 0.5f);
        DEFAULT_INTENSITIES.put("hook_04", 0.5f);
        DEFAULT_INTENSITIES.put("hook_05", 0.5f);
        DEFAULT_INTENSITIES.put("hook_06", 0.5f);
        DEFAULT_INTENSITIES.put("hook_07", 0.5f);
        DEFAULT_INTENSITIES.put("hook_08", 0.5f);
        DEFAULT_INTENSITIES.put("hook_09", 0.5f);
        DEFAULT_INTENSITIES.put("hook_10", 0.5f);
        DEFAULT_INTENSITIES.put("hook_11", 0.5f);
        DEFAULT_INTENSITIES.put("hook_12", 0.5f);
        
        // New hooks 13-20 (original definitions)
        DEFAULT_INTENSITIES.put("hook_13", 0.5f); // Biometric Authentication Realism
        DEFAULT_INTENSITIES.put("hook_14", 0.5f); // Social Interruption Simulation
        DEFAULT_INTENSITIES.put("hook_15", 0.5f); // Hardware Aging & Wear Simulation
        DEFAULT_INTENSITIES.put("hook_16", 0.5f); // Multi-Device Interaction Realism
        DEFAULT_INTENSITIES.put("hook_17", 0.5f); // Time-of-Day Usage Pattern Simulation
        DEFAULT_INTENSITIES.put("hook_18", 0.5f); // Haptic Feedback Realism
        DEFAULT_INTENSITIES.put("hook_19", 0.5f); // Audio Environment & Microphone Realism
        DEFAULT_INTENSITIES.put("hook_20", 0.5f); // App Standby & Doze Mode Realism
        
        // New hooks 13-20 (extended definitions)
        DEFAULT_INTENSITIES.put("hook_13_realism", 0.5f); // GPS Trajectory & Location Context
        DEFAULT_INTENSITIES.put("hook_14_realism", 0.5f); // Accessibility Scenario Simulation
        DEFAULT_INTENSITIES.put("hook_15_realism", 0.5f); // Weather & Environmental Sensor Effects
        DEFAULT_INTENSITIES.put("hook_16_realism", 0.5f); // Notification Dismissal & Attention Patterns
        DEFAULT_INTENSITIES.put("hook_17_realism", 0.5f); // Device Orientation & Grip Dynamics
        DEFAULT_INTENSITIES.put("hook_18_realism", 0.5f); // Emotional State Interaction Patterns
        DEFAULT_INTENSITIES.put("hook_19_realism", 0.5f); // App Session & Task Switching Patterns
        DEFAULT_INTENSITIES.put("hook_20_realism", 0.5f); // Voice Command & Speech Recognition Realism
        
        // Improved existing hooks
        DEFAULT_INTENSITIES.put("hook_03_realism", 0.5f); // Inter-App Navigation (Improved)
        DEFAULT_INTENSITIES.put("hook_07_realism", 0.5f); // Battery Thermal & Performance (Improved)
        DEFAULT_INTENSITIES.put("hook_08_realism", 0.5f); // Network Quality & Handover (Improved)
    }

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public ConfigurationManager(Context context) {
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.editor = prefs.edit();
        initializeDefaults();
    }

    /**
     * Initialize default values if not set
     */
    private void initializeDefaults() {
        // Set global defaults
        if (!prefs.contains(KEY_GLOBAL_COORDINATION)) {
            editor.putBoolean(KEY_GLOBAL_COORDINATION, true);
        }
        if (!prefs.contains(KEY_CONTEXT_AWARENESS)) {
            editor.putBoolean(KEY_CONTEXT_AWARENESS, true);
        }
        if (!prefs.contains(KEY_TEMPORAL_ALIGNMENT)) {
            editor.putBoolean(KEY_TEMPORAL_ALIGNMENT, true);
        }

        // Set default intensities for all hooks
        for (java.util.Map.Entry<String, Float> entry : DEFAULT_INTENSITIES.entrySet()) {
            String key = PREFIX_INTENSITY + entry.getKey();
            if (!prefs.contains(key)) {
                editor.putFloat(key, entry.getValue());
            }
            // Default enabled state
            String enabledKey = PREFIX_ENABLED + entry.getKey();
            if (!prefs.contains(enabledKey)) {
                editor.putBoolean(enabledKey, true);
            }
        }

        editor.apply();
    }

    /**
     * Check if a hook is enabled
     */
    public boolean isHookEnabled(String hookId) {
        return prefs.getBoolean(PREFIX_ENABLED + hookId, true);
    }

    /**
     * Set hook enabled state
     */
    public void setHookEnabled(String hookId, boolean enabled) {
        editor.putBoolean(PREFIX_ENABLED + hookId, enabled);
        editor.apply();
    }

    /**
     * Get hook intensity (0.0 - 1.0)
     */
    public float getHookIntensity(String hookId) {
        return prefs.getFloat(PREFIX_INTENSITY + hookId, 
            DEFAULT_INTENSITIES.getOrDefault(hookId, 0.5f));
    }

    /**
     * Set hook intensity
     */
    public void setHookIntensity(String hookId, float intensity) {
        float clampedIntensity = Math.max(0.0f, Math.min(1.0f, intensity));
        editor.putFloat(PREFIX_INTENSITY + hookId, clampedIntensity);
        editor.apply();
    }

    /**
     * Get hook parameter
     */
    public String getHookParam(String hookId, String paramName, String defaultValue) {
        return prefs.getString(PREFIX_PARAM + hookId + "_" + paramName, defaultValue);
    }

    /**
     * Set hook parameter
     */
    public void setHookParam(String hookId, String paramName, String value) {
        editor.putString(PREFIX_PARAM + hookId + "_" + paramName, value);
        editor.apply();
    }

    /**
     * Get hook parameter as int
     */
    public int getHookParamInt(String hookId, String paramName, int defaultValue) {
        return prefs.getInt(PREFIX_PARAM + hookId + "_" + paramName, defaultValue);
    }

    /**
     * Set hook parameter as int
     */
    public void setHookParamInt(String hookId, String paramName, int value) {
        editor.putInt(PREFIX_PARAM + hookId + "_" + paramName, value);
        editor.apply();
    }

    /**
     * Get hook parameter as float
     */
    public float getHookParamFloat(String hookId, String paramName, float defaultValue) {
        return prefs.getFloat(PREFIX_PARAM + hookId + "_" + paramName, defaultValue);
    }

    /**
     * Set hook parameter as float
     */
    public void setHookParamFloat(String hookId, String paramName, float value) {
        editor.putFloat(PREFIX_PARAM + hookId + "_" + paramName, value);
        editor.apply();
    }

    /**
     * Get hook parameter as boolean
     */
    public boolean getHookParamBool(String hookId, String paramName, boolean defaultValue) {
        return prefs.getBoolean(PREFIX_PARAM + hookId + "_" + paramName, defaultValue);
    }

    /**
     * Set hook parameter as boolean
     */
    public void setHookParamBool(String hookId, String paramName, boolean value) {
        editor.putBoolean(PREFIX_PARAM + hookId + "_" + paramName, value);
        editor.apply();
    }

    /**
     * Check if global coordination is enabled
     */
    public boolean isGlobalCoordinationEnabled() {
        return prefs.getBoolean(KEY_GLOBAL_COORDINATION, true);
    }

    /**
     * Check if context awareness is enabled
     */
    public boolean isContextAwarenessEnabled() {
        return prefs.getBoolean(KEY_CONTEXT_AWARENESS, true);
    }

    /**
     * Check if temporal alignment is enabled
     */
    public boolean isTemporalAlignmentEnabled() {
        return prefs.getBoolean(KEY_TEMPORAL_ALIGNMENT, true);
    }

    /**
     * Get all enabled hook IDs
     */
    public java.util.List<String> getEnabledHooks() {
        java.util.List<String> enabled = new java.util.ArrayList<>();
        for (String hookId : DEFAULT_INTENSITIES.keySet()) {
            if (isHookEnabled(hookId)) {
                enabled.add(hookId);
            }
        }
        return enabled;
    }

    /**
     * Reset all settings to defaults
     */
    public void resetToDefaults() {
        editor.clear();
        editor.apply();
        initializeDefaults();
    }
}
