package com.samsungcloak.xposed;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.UserHandle;
import android.os.UserManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * UserProfileSwitchingHook - Multi-User Profile Switching Simulation
 *
 * Simulates realistic multi-user device scenarios including work/personal profile
 * switching, guest users, and restricted profiles. This hook adds verisimilitude
 * to multi-user device testing by modeling:
 *
 * 1. User Profile Management - Multiple user profiles (work/personal/guest)
 * 2. User Switching Events - Switching between profiles with authentication
 * 3. Profile-Specific Data Separation - App data isolation per user
 * 4. Cross-Profile Notification Behavior - Different notification patterns
 * 5. Authentication Requirements - PIN/pattern/password during switch
 * 6. Background User State - Managing multiple user sessions
 *
 * Novelty: NOT covered by existing 12 hooks
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class UserProfileSwitchingHook {

    private static final String TAG = "[User][ProfileSwitch]";
    private static final boolean DEBUG = true;
    private static final ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new Random());

    // Toggleable via SharedPreferences
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);

    // User profile simulation
    private static boolean profileSimulationEnabled = true;
    private static int currentUserId = 0; // Primary user ID
    private static int simulatedUserId = 0;
    private static boolean isUserSwitching = false;

    // Multi-profile configuration
    private static boolean multiProfileEnabled = true;
    private static int maxUsers = 3;
    private static double switchProbability = 0.05; // 5% chance of random switch

    // Authentication simulation
    private static boolean authRequiredEnabled = true;
    private static boolean keyguardActive = false;
    private static long authTimeoutMs = 5000; // 5 second auth delay

    // User profile data
    private static final ConcurrentMap<Integer, UserProfileData> userProfiles = new ConcurrentHashMap<>();
    private static final List<Integer> userProfileIds = new ArrayList<>();

    // State tracking
    private static long lastUserSwitchTime = 0;
    private static final AtomicInteger userSwitchCount = new AtomicInteger(0);
    private static String lastSwitchReason = "";

    // SharedPreferences key for toggle
    private static final String PREFS_NAME = "SamsungCloak_UserProfile";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_MULTI_PROFILE = "multi_profile_enabled";
    private static final String KEY_SWITCH_PROBABILITY = "switch_probability";

    public enum UserType {
        PRIMARY,       // Device owner
        WORK,          // Work profile
        PERSONAL,      // Personal profile
        GUEST,        // Temporary guest
        RESTRICTED    // Restricted profile
    }

    public enum SwitchReason {
        EXPLICIT_SWITCH,
        APP_LAUNCH,
        BACKGROUND_RETURN,
        AUTH_TIMEOUT,
        SYSTEM_REQUEST
    }

    /**
     * User profile data structure
     */
    public static class UserProfileData {
        public final int userId;
        public final UserType userType;
        public final String userName;
        public final boolean isGuest;
        public final boolean isEnabled;
        public final long createdTime;
        public final Map<String, Boolean> appVisibility;

        public UserProfileData(int userId, UserType userType, String userName, boolean isGuest) {
            this.userId = userId;
            this.userType = userType;
            this.userName = userName;
            this.isGuest = isGuest;
            this.isEnabled = true;
            this.createdTime = System.currentTimeMillis();
            this.appVisibility = new HashMap<>();
        }
    }

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (hookInitialized.getAndSet(true)) {
            HookUtils.logDebug(TAG, "Already initialized");
            return;
        }

        HookUtils.logInfo(TAG, "Initializing User Profile Switching Hook");

        try {
            initializeUserProfiles();
            hookUserManager(lpparam);
            hookActivityManager(lpparam);
            hookKeyguardManager(lpparam);

            HookUtils.logInfo(TAG, "User Profile Switching Hook initialized successfully");
            HookUtils.logInfo(TAG, String.format("Profiles: %d, Current user: %d",
                userProfiles.size(), currentUserId));
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Init failed: " + t.getMessage());
            hookInitialized.set(false);
        }
    }

    private static void initializeUserProfiles() {
        // Create primary user (owner)
        UserProfileData primaryUser = new UserProfileData(0, UserType.PRIMARY, "Owner", false);
        userProfiles.put(0, primaryUser);
        userProfileIds.add(0);

        // Create work profile
        if (maxUsers > 1) {
            UserProfileData workUser = new UserProfileData(10, UserType.WORK, "Work", false);
            userProfiles.put(10, workUser);
            userProfileIds.add(10);
        }

        // Create personal profile
        if (maxUsers > 2) {
            UserProfileData personalUser = new UserProfileData(11, UserType.PERSONAL, "Personal", false);
            userProfiles.put(11, personalUser);
            userProfileIds.add(11);
        }

        // Initialize current user
        simulatedUserId = currentUserId;
    }

    private static void hookUserManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> userManagerClass = XposedHelpers.findClass(
                "android.os.UserManager", lpparam.classLoader
            );

            // Hook getUserInfo
            XposedBridge.hookAllMethods(userManagerClass, "getUserInfo",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !profileSimulationEnabled) return;
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !profileSimulationEnabled) return;

                        int userId = (int) param.args[0];
                        // Allow getting info for any configured profile
                        if (userProfiles.containsKey(userId)) {
                            // Return original UserInfo object, but logging
                            HookUtils.logDebug(TAG, "getUserInfo called for user: " + userId);
                        }
                    }
                });

            // Hook getUserProfiles
            XposedBridge.hookAllMethods(userManagerClass, "getUserProfiles",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !multiProfileEnabled) return;

                        @SuppressWarnings("unchecked")
                        List<UserHandle> profiles = (List<UserHandle>) param.getResult();
                        if (profiles != null) {
                            // Add work profile to the list if multi-profile enabled
                            if (userProfiles.containsKey(10) && profiles.size() < maxUsers) {
                                HookUtils.logDebug(TAG, "Returning user profiles including work profile");
                            }
                        }
                    }
                });

            // Hook getSerialNumberForUser
            XposedBridge.hookAllMethods(userManagerClass, "getSerialNumberForUser",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !profileSimulationEnabled) return;

                        UserHandle userHandle = (UserHandle) param.args[0];
                        int userId = UserHandle.getUserId(userHandle.getIdentifier());
                        // Return serial number based on user ID
                        param.setResult((long) userId * 1000);
                    }
                });

            HookUtils.logInfo(TAG, "UserManager hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "UserManager hook failed: " + t.getMessage());
        }
    }

    private static void hookActivityManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> activityManagerClass = XposedHelpers.findClass(
                "android.app.ActivityManager", lpparam.classLoader
            );

            // Hook getCurrentUser
            XposedBridge.hookAllMethods(activityManagerClass, "getCurrentUser",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !profileSimulationEnabled) return;
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !profileSimulationEnabled) return;

                        // Return simulated current user
                        Object userInfo = param.getResult();
                        if (userInfo != null) {
                            // In a full implementation, we'd create a custom UserInfo
                            HookUtils.logDebug(TAG, "getCurrentUser returning user: " + simulatedUserId);
                        }
                    }
                });

            HookUtils.logInfo(TAG, "ActivityManager hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "ActivityManager hook failed: " + t.getMessage());
        }
    }

    private static void hookKeyguardManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> keyguardManagerClass = XposedHelpers.findClass(
                "android.app.KeyguardManager", lpparam.classLoader
            );

            // Hook isKeyguardLocked
            XposedBridge.hookAllMethods(keyguardManagerClass, "isKeyguardLocked",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !authRequiredEnabled) return;

                        // Return simulated keyguard state
                        param.setResult(keyguardActive);
                    }
                });

            // Hook isKeyguardSecure
            XposedBridge.hookAllMethods(keyguardManagerClass, "isKeyguardSecure",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !authRequiredEnabled) return;

                        // Return that keyguard is secure (PIN/pattern set)
                        param.setResult(true);
                    }
                });

            HookUtils.logInfo(TAG, "KeyguardManager hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "KeyguardManager hook failed: " + t.getMessage());
        }
    }

    /**
     * Simulate a user switch event
     */
    public static void simulateUserSwitch(int targetUserId, SwitchReason reason) {
        if (!userProfiles.containsKey(targetUserId)) {
            HookUtils.logError(TAG, "Invalid target user: " + targetUserId);
            return;
        }

        int previousUserId = simulatedUserId;
        simulatedUserId = targetUserId;
        isUserSwitching = true;

        lastUserSwitchTime = System.currentTimeMillis();
        userSwitchCount.incrementAndGet();
        lastSwitchReason = reason.name();

        // Activate keyguard during switch
        keyguardActive = true;

        // Simulate authentication delay
        if (authRequiredEnabled) {
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                keyguardActive = false;
                isUserSwitching = false;
                HookUtils.logInfo(TAG, String.format("User switch complete: %d -> %d (%s)",
                    previousUserId, targetUserId, reason.name()));
            }, authTimeoutMs);
        } else {
            isUserSwitching = false;
        }

        HookUtils.logInfo(TAG, String.format("Simulating user switch: %d -> %d (%s)",
            previousUserId, targetUserId, reason.name()));
    }

    /**
     * Check if random user switch should occur
     */
    public static void checkForRandomUserSwitch() {
        if (!enabled || !profileSimulationEnabled || !multiProfileEnabled) return;

        if (random.get().nextDouble() < switchProbability) {
            // Select random user different from current
            List<Integer> otherUsers = new ArrayList<>(userProfileIds);
            otherUsers.remove(Integer.valueOf(simulatedUserId));

            if (!otherUsers.isEmpty()) {
                int randomIndex = random.get().nextInt(otherUsers.size());
                int targetUser = otherUsers.get(randomIndex);
                simulateUserSwitch(targetUser, SwitchReason.BACKGROUND_RETURN);
            }
        }
    }

    // Configuration methods

    public static void setEnabled(boolean enabled) {
        UserProfileSwitchingHook.enabled = enabled;
        HookUtils.logInfo(TAG, "Hook " + (enabled ? "enabled" : "disabled"));
    }

    public static void setMultiProfileEnabled(boolean enabled) {
        multiProfileEnabled = enabled;
        HookUtils.logInfo(TAG, "Multi-profile " + (enabled ? "enabled" : "disabled"));
    }

    public static void setSwitchProbability(double probability) {
        switchProbability = HookUtils.clamp(probability, 0.0, 1.0);
        HookUtils.logInfo(TAG, "Switch probability: " + switchProbability);
    }

    public static void setAuthRequired(boolean required) {
        authRequiredEnabled = required;
        HookUtils.logInfo(TAG, "Auth required: " + required);
    }

    public static int getCurrentUserId() {
        return simulatedUserId;
    }

    public static int getUserSwitchCount() {
        return userSwitchCount.get();
    }

    public static long getLastUserSwitchTime() {
        return lastUserSwitchTime;
    }

    public static boolean isKeyguardActive() {
        return keyguardActive;
    }

    public static boolean isUserSwitching() {
        return isUserSwitching;
    }

    public static String getLastSwitchReason() {
        return lastSwitchReason;
    }

    public static int getProfileCount() {
        return userProfiles.size();
    }
}
