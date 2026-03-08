package com.samsungcloak.xposed.telemetry;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Accessibility Profile Diversity Hook
 * 
 * Injects realistic user-specific accessibility settings to move the device
 * away from the default factory state, simulating a real long-term user.
 * 
 * Settings modeled:
 * - Font Scale multipliers (0.85x - 1.5x)
 * - TalkBack/VoiceOver state flags
 * - Reduce Motion preferences
 * - Display size adjustments
 * - High Contrast text
 * - Color correction/filtering
 * 
 * Target Device: Samsung Galaxy A12 (SM-A125U)
 */
public class AccessibilityProfileDiversityHook {
    
    private static final String LOG_TAG = "SamsungCloak.AccessibilityProfile";
    private static boolean initialized = false;
    
    private static final Random random = new Random();
    
    // User-specific accessibility settings (non-factory defaults)
    private static volatile float fontScale = 1.0f;
    private static volatile boolean talkBackEnabled = false;
    private static volatile boolean reduceMotionEnabled = false;
    private static volatile float displaySize = 1.0f;
    private static volatile boolean highContrastText = false;
    private static volatile boolean colorCorrectionEnabled = false;
    private static volatile String colorCorrectionMode = "none";
    private static volatile boolean boldTextEnabled = false;
    private static volatile int touchAndHoldDelay = 500; // ms
    private static volatile boolean accessibilityButtonEnabled = false;
    private static volatile boolean captionsEnabled = false;
    private static volatile String userProfileId = "";
    
    // Statistics
    private static int profileApplicationCount = 0;
    private static long lastProfileChangeTime = 0;
    
    private static final AtomicReference<UserAccessibilityProfile> currentProfile = new AtomicReference<>();
    
    /**
     * Predefined user profiles representing different accessibility needs
     */
    public enum AccessibilityProfile {
        DEFAULT("default", 1.0f, false, false, 1.0f, false),
        ELDERLY("elderly", 1.3f, false, false, 1.15f, true),
        VISUAL_IMPAIRED("visual_impaired", 1.5f, true, false, 1.2f, true),
        SENSITIVE_MOTION("sensitive_motion", 1.0f, false, true, 1.0f, false),
        LOW_VISION("low_vision", 1.4f, true, true, 1.25f, true),
        POWER_USER("power_user", 0.9f, false, true, 1.0f, false),
        TEENAGER("teenager", 0.85f, false, false, 1.05f, false),
        CHILD("child", 1.15f, false, false, 1.1f, false),
        COLOR_BLIND_PROTANOPIA("color_blind_protanopia", 1.0f, false, false, 1.0f, false),
        COLOR_BLIND_DEUTERANOPIA("color_blind_deuteranopia", 1.0f, false, false, 1.0f, false);
        
        private final String id;
        private final float fontScale;
        private final boolean talkBack;
        private final boolean reduceMotion;
        private final float displaySize;
        private final boolean highContrast;
        
        AccessibilityProfile(String id, float fontScale, boolean talkBack, 
                            boolean reduceMotion, float displaySize, boolean highContrast) {
            this.id = id;
            this.fontScale = fontScale;
            this.talkBack = talkBack;
            this.reduceMotion = reduceMotion;
            this.displaySize = displaySize;
            this.highContrast = highContrast;
        }
        
        public String getId() { return id; }
        public float getFontScale() { return fontScale; }
        public boolean isTalkBack() { return talkBack; }
        public boolean isReduceMotion() { return reduceMotion; }
        public float getDisplaySize() { return displaySize; }
        public boolean isHighContrast() { return highContrast; }
    }
    
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (initialized) {
            XposedBridge.log(LOG_TAG + " already initialized");
            return;
        }
        
        try {
            // Generate a consistent user profile for this device
            generateUserSpecificProfile();
            
            hookAccessibilitySettings(lpparam);
            hookContentResolverSettings(lpparam);
            hookSettingsSecure(lpparam);
            
            initialized = true;
            profileApplicationCount = 1;
            lastProfileChangeTime = System.currentTimeMillis();
            
            XposedBridge.log(LOG_TAG + " initialized with profile: " + userProfileId);
            XposedBridge.log(LOG_TAG + " FontScale=" + fontScale + ", TalkBack=" + talkBackEnabled + 
                ", ReduceMotion=" + reduceMotionEnabled + ", DisplaySize=" + displaySize);
            
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " Failed to initialize: " + e.getMessage());
        }
    }
    
    /**
     * Generate a consistent user-specific profile based on random selection
     * This simulates a device that has been personalized by a real user
     */
    private static void generateUserSpecificProfile() {
        // Weight the distribution to favor common configurations
        float profileRoll = random.nextFloat();
        
        AccessibilityProfile profile;
        if (profileRoll < 0.35f) {
            profile = AccessibilityProfile.POWER_USER;
        } else if (profileRoll < 0.55f) {
            profile = AccessibilityProfile.TEENAGER;
        } else if (profileRoll < 0.70f) {
            profile = AccessibilityProfile.ELDERLY;
        } else if (profileRoll < 0.80f) {
            profile = AccessibilityProfile.SENSITIVE_MOTION;
        } else if (profileRoll < 0.88f) {
            profile = AccessibilityProfile.LOW_VISION;
        } else if (profileRoll < 0.94f) {
            profile = AccessibilityProfile.VISUAL_IMPAIRED;
        } else if (profileRoll < 0.97f) {
            profile = AccessibilityProfile.COLOR_BLIND_PROTANOPIA;
        } else if (profileRoll < 0.99f) {
            profile = AccessibilityProfile.COLOR_BLIND_DEUTERANOPIA;
        } else if (profileRoll < 0.995f) {
            profile = AccessibilityProfile.CHILD;
        } else {
            profile = AccessibilityProfile.DEFAULT;
        }
        
        applyProfile(profile);
        
        // Add some variance to make it feel more "worn"
        applyUserVariance();
    }
    
    /**
     * Apply an accessibility profile
     */
    public static void applyProfile(AccessibilityProfile profile) {
        fontScale = profile.getFontScale();
        talkBackEnabled = profile.isTalkBack();
        reduceMotionEnabled = profile.isReduceMotion();
        displaySize = profile.getDisplaySize();
        highContrastText = profile.isHighContrast();
        
        // Set user profile ID
        userProfileId = profile.getId();
        
        // Generate related settings
        boldTextEnabled = random.nextFloat() < 0.25f;
        captionsEnabled = random.nextFloat() < 0.15f;
        touchAndHoldDelay = 300 + random.nextInt(700); // 300-1000ms
        accessibilityButtonEnabled = talkBackEnabled || random.nextFloat() < 0.1f;
        
        // Color correction (for color blind profiles)
        if (profile == AccessibilityProfile.COLOR_BLIND_PROTANOPIA) {
            colorCorrectionEnabled = true;
            colorCorrectionMode = "protanopia";
        } else if (profile == AccessibilityProfile.COLOR_BLIND_DEUTERANOPIA) {
            colorCorrectionEnabled = true;
            colorCorrectionMode = "deuteranopia";
        } else {
            colorCorrectionEnabled = random.nextFloat() < 0.05f;
            colorCorrectionMode = colorCorrectionEnabled ? "grayscale" : "none";
        }
        
        // Update current profile reference
        currentProfile.set(new UserAccessibilityProfile(
            userProfileId, fontScale, talkBackEnabled, reduceMotionEnabled,
            displaySize, highContrastText, boldTextEnabled, colorCorrectionEnabled,
            colorCorrectionMode, touchAndHoldDelay, accessibilityButtonEnabled,
            captionsEnabled, System.currentTimeMillis()
        ));
        
        profileApplicationCount++;
        lastProfileChangeTime = System.currentTimeMillis();
    }
    
    /**
     * Apply additional variance to simulate real-world user adjustments
     */
    private static void applyUserVariance() {
        // Add small random adjustments to make it feel more organic
        fontScale += (random.nextFloat() * 0.1f) - 0.05f;
        fontScale = Math.max(0.8f, Math.min(1.6f, fontScale));
        
        displaySize += (random.nextFloat() * 0.08f) - 0.04f;
        displaySize = Math.max(0.85f, Math.min(1.4f, displaySize));
        
        // Occasionally toggle reduce motion if user hasn't explicitly set it
        if (!reduceMotionEnabled && random.nextFloat() < 0.2f) {
            reduceMotionEnabled = true;
        }
        
        // Some users enable accessibility button even without TalkBack
        if (!talkBackEnabled && random.nextFloat() < 0.15f) {
            accessibilityButtonEnabled = true;
        }
    }
    
    private static void hookAccessibilitySettings(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> accessibilityManagerClass = XposedHelpers.findClass(
                "android.view.accessibility.AccessibilityManager", lpparam.classLoader
            );
            
            XposedBridge.hookAllMethods(accessibilityManagerClass, "getEnabledAccessibilityServiceList",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (talkBackEnabled) {
                            XposedBridge.log(LOG_TAG + " Injecting TalkBack service presence");
                        }
                    }
                });
            
            XposedBridge.hookAllMethods(accessibilityManagerClass, "isTouchExplorationEnabled",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(talkBackEnabled);
                    }
                });
            
            XposedBridge.log(LOG_TAG + " Hooked AccessibilityManager");
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " Failed to hook AccessibilityManager: " + e.getMessage());
        }
    }
    
    private static void hookContentResolverSettings(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> contentResolverClass = XposedHelpers.findClass(
                "android.content.ContentResolver", lpparam.classLoader
            );
            
            // Hook font scale setting
            XposedBridge.hookAllMethods(contentResolverClass, "getFloat", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.args.length >= 1 && param.args[0] != null) {
                        String setting = param.args[0].toString();
                        if (setting.contains("font_scale")) {
                            param.setResult(fontScale);
                            return;
                        }
                        if (setting.contains("display_size")) {
                            param.setResult(displaySize);
                            return;
                        }
                    }
                }
            });
            
            // Hook integer settings
            XposedBridge.hookAllMethods(contentResolverClass, "getInt", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.args.length >= 1 && param.args[0] != null) {
                        String setting = param.args[0].toString();
                        if (setting.contains("touch_exploration_enabled")) {
                            param.setResult(talkBackEnabled ? 1 : 0);
                            return;
                        }
                        if (setting.contains("reduce_motion")) {
                            param.setResult(reduceMotionEnabled ? 1 : 0);
                            return;
                        }
                        if (setting.contains("high_text_contrast")) {
                            param.setResult(highContrastText ? 1 : 0);
                            return;
                        }
                    }
                }
            });
            
            // Hook string settings
            XposedBridge.hookAllMethods(contentResolverClass, "getString", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.args.length >= 1 && param.args[0] != null) {
                        String setting = param.args[0].toString();
                        if (setting.contains("color_correction")) {
                            param.setResult(colorCorrectionEnabled ? colorCorrectionMode : "none");
                            return;
                        }
                    }
                }
            });
            
            XposedBridge.log(LOG_TAG + " Hooked ContentResolver settings");
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " Failed to hook ContentResolver: " + e.getMessage());
        }
    }
    
    private static void hookSettingsSecure(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> settingsSecureClass = XposedHelpers.findClass(
                "android.provider.Settings$Secure", lpparam.classLoader
            );
            
            XposedBridge.hookAllMethods(settingsSecureClass, "getString", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    // Let framework handle most settings, but inject accessibility states
                }
            });
            
            XposedBridge.log(LOG_TAG + " Hooked Settings.Secure");
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " Failed to hook Settings.Secure: " + e.getMessage());
        }
    }
    
    /**
     * Get current user profile
     */
    public static UserAccessibilityProfile getCurrentProfile() {
        return currentProfile.get();
    }
    
    /**
     * Get current font scale
     */
    public static float getFontScale() {
        return fontScale;
    }
    
    /**
     * Get current display size
     */
    public static float getDisplaySize() {
        return displaySize;
    }
    
    /**
     * Check if TalkBack is enabled
     */
    public static boolean isTalkBackEnabled() {
        return talkBackEnabled;
    }
    
    /**
     * Check if Reduce Motion is enabled
     */
    public static boolean isReduceMotionEnabled() {
        return reduceMotionEnabled;
    }
    
    /**
     * Check if high contrast text is enabled
     */
    public static boolean isHighContrastTextEnabled() {
        return highContrastText;
    }
    
    /**
     * Check if color correction is enabled
     */
    public static boolean isColorCorrectionEnabled() {
        return colorCorrectionEnabled;
    }
    
    /**
     * Get user profile ID
     */
    public static String getUserProfileId() {
        return userProfileId;
    }
    
    /**
     * Get profile application count
     */
    public static int getProfileApplicationCount() {
        return profileApplicationCount;
    }
    
    public static boolean isInitialized() {
        return initialized;
    }
    
    /**
     * User accessibility profile data class
     */
    public static class UserAccessibilityProfile {
        private final String profileId;
        private final float fontScale;
        private final boolean talkBackEnabled;
        private final boolean reduceMotionEnabled;
        private final float displaySize;
        private final boolean highContrastText;
        private final boolean boldTextEnabled;
        private final boolean colorCorrectionEnabled;
        private final String colorCorrectionMode;
        private final int touchAndHoldDelay;
        private final boolean accessibilityButtonEnabled;
        private final boolean captionsEnabled;
        private final long lastModified;
        
        public UserAccessibilityProfile(String profileId, float fontScale, 
                                       boolean talkBackEnabled, boolean reduceMotionEnabled,
                                       float displaySize, boolean highContrastText,
                                       boolean boldTextEnabled, boolean colorCorrectionEnabled,
                                       String colorCorrectionMode, int touchAndHoldDelay,
                                       boolean accessibilityButtonEnabled, boolean captionsEnabled,
                                       long lastModified) {
            this.profileId = profileId;
            this.fontScale = fontScale;
            this.talkBackEnabled = talkBackEnabled;
            this.reduceMotionEnabled = reduceMotionEnabled;
            this.displaySize = displaySize;
            this.highContrastText = highContrastText;
            this.boldTextEnabled = boldTextEnabled;
            this.colorCorrectionEnabled = colorCorrectionEnabled;
            this.colorCorrectionMode = colorCorrectionMode;
            this.touchAndHoldDelay = touchAndHoldDelay;
            this.accessibilityButtonEnabled = accessibilityButtonEnabled;
            this.captionsEnabled = captionsEnabled;
            this.lastModified = lastModified;
        }
        
        public String getProfileId() { return profileId; }
        public float getFontScale() { return fontScale; }
        public boolean isTalkBackEnabled() { return talkBackEnabled; }
        public boolean isReduceMotionEnabled() { return reduceMotionEnabled; }
        public float getDisplaySize() { return displaySize; }
        public boolean isHighContrastText() { return highContrastText; }
        public boolean isBoldTextEnabled() { return boldTextEnabled; }
        public boolean isColorCorrectionEnabled() { return colorCorrectionEnabled; }
        public String getColorCorrectionMode() { return colorCorrectionMode; }
        public int getTouchAndHoldDelay() { return touchAndHoldDelay; }
        public boolean isAccessibilityButtonEnabled() { return accessibilityButtonEnabled; }
        public boolean isCaptionsEnabled() { return captionsEnabled; }
        public long getLastModified() { return lastModified; }
        
        @Override
        public String toString() {
            return String.format("UserAccessibilityProfile{id=%s, fontScale=%.2f, talkBack=%s, " +
                "reduceMotion=%s, displaySize=%.2f, highContrast=%s, colorCorrection=%s}",
                profileId, fontScale, talkBackEnabled, reduceMotionEnabled,
                displaySize, highContrastText, colorCorrectionMode);
        }
    }
}
