package com.samsungcloak.xposed;

public enum ContextProfile {
    WORK,
    HOME,
    MOBILE,
    DESKTOP,
    PRIVACY,
    TIME_PRESSURE;

    private static ContextProfile currentProfile = ContextProfile.HOME;
    private static long profileStartTime = System.currentTimeMillis();

    public static ContextProfile getCurrentProfile() {
        return currentProfile;
    }

    public static void setProfile(ContextProfile profile) {
        currentProfile = profile;
        profileStartTime = System.currentTimeMillis();
        HookUtils.logInfo("Context profile switched to: " + profile.name());
    }

    public static long getProfileDuration() {
        return System.currentTimeMillis() - profileStartTime;
    }
}

public class ContextualAdaptationEngine {
    private static final String LOG_TAG = "SamsungCloak.ContextAdaptation";
    private static boolean initialized = false;

    private static long sessionStartTime = System.currentTimeMillis();
    private static int sessionInteractionCount = 0;
    private static ContextProfile activeProfile = ContextProfile.HOME;

    private static long workStartHour = 9;
    private static long workEndHour = 17;

    private static long minThinkTimeMs = 150;
    private static long maxThinkTimeMs = 800;
    private static long currentThinkTimeMs = 400;

    private static double errorProbability = 0.05;

    private static boolean isMobileMode = true;
    private static boolean isPrivacyMode = false;
    private static boolean isTimePressureActive = false;
    private static long timePressureEndTime = 0;

    private static double sessionFrequency = 1.0;
    private static double appSwitchSpeed = 1.0;

    private static long mobileSessionDurationMs = 30000;
    private static long desktopSessionDurationMs = 300000;
    private static double mobileInterruptionRate = 0.3;
    private static double desktopInterruptionRate = 0.05;

    private static long privacyModeTimeoutMs = 60000;
    private static long lastPersistentLoginTime = 0;
    private static boolean avoidDataEntry = false;

    private static double timePressureThinkTimeMultiplier = 0.3;
    private static double timePressureErrorMultiplier = 3.0;

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (initialized) {
            HookUtils.logDebug("ContextualAdaptationEngine already initialized");
            return;
        }

        try {
            initializeDefaultProfiles();
            scheduleAutomaticProfileUpdates();
            initialized = true;
            HookUtils.logInfo("ContextualAdaptationEngine initialized successfully");
        } catch (Exception e) {
            HookUtils.logError("Failed to initialize ContextualAdaptationEngine: " + e.getMessage());
        }
    }

    private static void initializeDefaultProfiles() {
        long currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        
        if (currentHour >= workStartHour && currentHour < workEndHour) {
            setWorkProfile();
        } else {
            setHomeProfile();
        }
        
        activeProfile = ContextProfile.MOBILE;
        HookUtils.logInfo("Default context profile initialized: " + activeProfile.name());
    }

    private static void scheduleAutomaticProfileUpdates() {
        Thread profileUpdateThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60000);
                    updateProfileBasedOnTime();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        profileUpdateThread.setDaemon(true);
        profileUpdateThread.setName("ContextProfileUpdater");
        profileUpdateThread.start();
    }

    private static void updateProfileBasedOnTime() {
        long currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        
        if (currentHour >= workStartHour && currentHour < workEndHour) {
            if (activeProfile != ContextProfile.WORK) {
                setWorkProfile();
            }
        } else {
            if (activeProfile != ContextProfile.HOME) {
                setHomeProfile();
            }
        }

        if (isTimePressureActive && System.currentTimeMillis() > timePressureEndTime) {
            deactivateTimePressure();
        }
    }

    public static void setWorkProfile() {
        activeProfile = ContextProfile.WORK;
        ContextProfile.setProfile(ContextProfile.WORK);
        
        minThinkTimeMs = 300;
        maxThinkTimeMs = 1200;
        currentThinkTimeMs = 700;
        
        errorProbability = 0.03;
        sessionFrequency = 0.6;
        appSwitchSpeed = 0.5;
        
        HookUtils.logInfo("WORK profile activated: focused, low frequency, slower app switching");
    }

    public static void setHomeProfile() {
        activeProfile = ContextProfile.HOME;
        ContextProfile.setProfile(ContextProfile.HOME);
        
        minThinkTimeMs = 100;
        maxThinkTimeMs = 500;
        currentThinkTimeMs = 250;
        
        errorProbability = 0.06;
        sessionFrequency = 1.2;
        appSwitchSpeed = 1.5;
        
        HookUtils.logInfo("HOME profile activated: leisure, high frequency, faster app switching");
    }

    public static void activateMobileMode() {
        activeProfile = ContextProfile.MOBILE;
        ContextProfile.setProfile(ContextProfile.MOBILE);
        
        isMobileMode = true;
        
        mobileSessionDurationMs = 30000 + (long) (HookUtils.getRandom().nextDouble() * 30000);
        currentThinkTimeMs = 200 + HookUtils.getRandom().nextInt(200);
        sessionFrequency = 1.3;
        
        HookUtils.logInfo("MOBILE mode activated: shorter sessions, higher interruption rate");
    }

    public static void activateDesktopMode() {
        activeProfile = ContextProfile.DESKTOP;
        ContextProfile.setProfile(ContextProfile.DESKTOP);
        
        isMobileMode = false;
        
        desktopSessionDurationMs = 180000 + (long) (HookUtils.getRandom().nextDouble() * 180000);
        currentThinkTimeMs = 400 + HookUtils.getRandom().nextInt(400);
        sessionFrequency = 0.8;
        
        HookUtils.logInfo("DESKTOP mode activated: longer sessions, lower interruption rate");
    }

    public static void enablePrivacyMode() {
        activeProfile = ContextProfile.PRIVACY;
        ContextProfile.setProfile(ContextProfile.PRIVACY);
        
        isPrivacyMode = true;
        avoidDataEntry = true;
        lastPersistentLoginTime = System.currentTimeMillis() - privacyModeTimeoutMs;
        
        currentThinkTimeMs = 150;
        errorProbability = 0.08;
        
        HookUtils.logInfo("PRIVACY mode enabled: avoiding persistent logins and data entry");
    }

    public static void disablePrivacyMode() {
        isPrivacyMode = false;
        avoidDataEntry = false;
        
        if (activeProfile == ContextProfile.PRIVACY) {
            setHomeProfile();
        }
        
        HookUtils.logInfo("PRIVACY mode disabled");
    }

    public static void activateTimePressure(long durationMs) {
        activeProfile = ContextProfile.TIME_PRESSURE;
        ContextProfile.setProfile(ContextProfile.TIME_PRESSURE);
        
        isTimePressureActive = true;
        timePressureEndTime = System.currentTimeMillis() + durationMs;
        
        currentThinkTimeMs = (long) (currentThinkTimeMs * timePressureThinkTimeMultiplier);
        errorProbability = errorProbability * timePressureErrorMultiplier;
        
        HookUtils.logInfo("TIME PRESSURE activated for " + (durationMs / 1000) + "s: reduced think time, increased error probability");
    }

    public static void deactivateTimePressure() {
        isTimePressureActive = false;
        
        currentThinkTimeMs = (long) (currentThinkTimeMs / timePressureThinkTimeMultiplier);
        errorProbability = errorProbability / timePressureErrorMultiplier;
        
        if (currentThinkTimeMs < minThinkTimeMs) {
            currentThinkTimeMs = minThinkTimeMs + HookUtils.getRandom().nextInt((int)(maxThinkTimeMs - minThinkTimeMs));
        }
        
        setHomeProfile();
        
        HookUtils.logInfo("TIME PRESSURE deactivated: think time normalized, error probability normalized");
    }

    public static long getThinkTime() {
        long variance = HookUtils.getRandom().nextInt((int)(currentThinkTimeMs / 4));
        return currentThinkTimeMs + variance - (currentThinkTimeMs / 8);
    }

    public static long getAdjustedThinkTime(long baseThinkTime) {
        long adjustedTime = (long)(baseThinkTime * getThinkTimeMultiplier());
        
        if (isTimePressureActive) {
            adjustedTime = (long)(adjustedTime * timePressureThinkTimeMultiplier);
        }
        
        return Math.max(adjustedTime, 50);
    }

    public static double getThinkTimeMultiplier() {
        double multiplier = 1.0;
        
        switch (activeProfile) {
            case WORK:
                multiplier = 1.5;
                break;
            case HOME:
                multiplier = 0.7;
                break;
            case MOBILE:
                multiplier = 0.6;
                break;
            case DESKTOP:
                multiplier = 1.2;
                break;
            case PRIVACY:
                multiplier = 0.5;
                break;
            case TIME_PRESSURE:
                multiplier = 0.3;
                break;
        }
        
        return multiplier;
    }

    public static double getErrorProbability() {
        double baseError = errorProbability;
        
        switch (activeProfile) {
            case WORK:
                baseError = 0.03;
                break;
            case HOME:
                baseError = 0.06;
                break;
            case MOBILE:
                baseError = 0.08;
                break;
            case DESKTOP:
                baseError = 0.04;
                break;
            case PRIVACY:
                baseError = 0.07;
                break;
            case TIME_PRESSURE:
                baseError = baseError * timePressureErrorMultiplier;
                break;
        }
        
        return Math.min(baseError, 0.5);
    }

    public static boolean shouldSimulateError() {
        return HookUtils.getRandom().nextDouble() < getErrorProbability();
    }

    public static long getSessionDuration() {
        if (isMobileMode) {
            return mobileSessionDurationMs;
        } else {
            return desktopSessionDurationMs;
        }
    }

    public static double getInterruptionRate() {
        if (isMobileMode) {
            return mobileInterruptionRate;
        } else {
            return desktopInterruptionRate;
        }
    }

    public static boolean shouldInterruptSession() {
        return HookUtils.getRandom().nextDouble() < getInterruptionRate();
    }

    public static double getInteractionFrequency() {
        return sessionFrequency;
    }

    public static double getAppSwitchSpeed() {
        return appSwitchSpeed;
    }

    public static long getAppSwitchDelay() {
        long baseDelay = (long)(500 / appSwitchSpeed);
        long variance = HookUtils.getRandom().nextInt((int)baseDelay);
        return baseDelay + variance;
    }

    public static boolean canPerformPersistentLogin() {
        if (!isPrivacyMode) {
            return true;
        }
        
        long timeSinceLastLogin = System.currentTimeMillis() - lastPersistentLoginTime;
        return timeSinceLastLogin > privacyModeTimeoutMs;
    }

    public static void recordPersistentLogin() {
        lastPersistentLoginTime = System.currentTimeMillis();
        HookUtils.logDebug("Persistent login recorded at: " + lastPersistentLoginTime);
    }

    public static boolean shouldAvoidDataEntry() {
        return isPrivacyMode && avoidDataEntry;
    }

    public static boolean isPrivacyModeActive() {
        return isPrivacyMode;
    }

    public static boolean isTimePressureActive() {
        return isTimePressureActive;
    }

    public static boolean isMobileInteraction() {
        return isMobileMode;
    }

    public static ContextProfile getActiveProfile() {
        return activeProfile;
    }

    public static void incrementInteractionCount() {
        sessionInteractionCount++;
    }

    public static int getSessionInteractionCount() {
        return sessionInteractionCount;
    }

    public static void resetSession() {
        sessionStartTime = System.currentTimeMillis();
        sessionInteractionCount = 0;
        HookUtils.logInfo("Session reset for contextual adaptation engine");
    }

    public static void setWorkHours(long startHour, long endHour) {
        workStartHour = startHour;
        workEndHour = endHour;
        HookUtils.logInfo("Work hours updated: " + startHour + ":00 - " + endHour + ":00");
    }

    public static void setMobileSettings(long sessionDurationMs, double interruptionRate) {
        mobileSessionDurationMs = sessionDurationMs;
        mobileInterruptionRate = interruptionRate;
        HookUtils.logInfo("Mobile settings updated: session=" + sessionDurationMs + "ms, interruption=" + interruptionRate);
    }

    public static void setDesktopSettings(long sessionDurationMs, double interruptionRate) {
        desktopSessionDurationMs = sessionDurationMs;
        desktopInterruptionRate = interruptionRate;
        HookUtils.logInfo("Desktop settings updated: session=" + sessionDurationMs + "ms, interruption=" + interruptionRate);
    }

    public static void setPrivacyModeSettings(long timeoutMs) {
        privacyModeTimeoutMs = timeoutMs;
        HookUtils.logInfo("Privacy mode timeout updated: " + timeoutMs + "ms");
    }

    public static void setTimePressureSettings(double thinkTimeMultiplier, double errorMultiplier) {
        timePressureThinkTimeMultiplier = thinkTimeMultiplier;
        timePressureErrorMultiplier = errorMultiplier;
        HookUtils.logInfo("Time pressure settings updated: thinkTime=" + thinkTimeMultiplier + ", error=" + errorMultiplier);
    }

    public static String getContextDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("Context: ").append(activeProfile.name());
        sb.append(" | ThinkTime: ").append(currentThinkTimeMs).append("ms");
        sb.append(" | ErrorProb: ").append(String.format("%.2f", getErrorProbability()));
        sb.append(" | Frequency: ").append(String.format("%.2f", sessionFrequency));
        sb.append(" | AppSwitch: ").append(String.format("%.2f", appSwitchSpeed));
        
        if (isMobileMode) {
            sb.append(" | Mobile");
        } else {
            sb.append(" | Desktop");
        }
        
        if (isPrivacyMode) {
            sb.append(" | Privacy");
        }
        
        if (isTimePressureActive) {
            sb.append(" | TimePressure");
        }
        
        return sb.toString();
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
