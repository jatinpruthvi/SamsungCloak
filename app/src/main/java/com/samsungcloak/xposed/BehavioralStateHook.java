package com.samsungcloak.xposed;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class BehavioralStateHook {
    private static final String LOG_TAG = "SamsungCloak.BehavioralStateHook";
    private static boolean initialized = false;

    private static long sessionStartTime = System.currentTimeMillis();
    private static int interactionCount = 0;
    private static float currentFatigueLevel = 0.0f;
    private static float attentionLevel = 1.0f;
    private static float stressLevel = 0.5f;

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (initialized) {
            HookUtils.logDebug("BehavioralStateHook already initialized");
            return;
        }

        try {
            hookSystemClock(lpparam);
            hookSystemElapsed(lpparam);
            initialized = true;
            HookUtils.logInfo("BehavioralStateHook initialized successfully");
        } catch (Exception e) {
            HookUtils.logError("Failed to initialize BehavioralStateHook: " + e.getMessage());
        }
    }

    private static void hookSystemClock(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> systemClass = XposedHelpers.findClass(
                "android.os.System", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(systemClass, "currentTimeMillis", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        long currentTime = System.currentTimeMillis();
                        applyFatigue(currentTime);
                        applyAttention(currentTime);
                        applyStress(currentTime);
                        applyDecisionInconsistency(currentTime);
                    } catch (Exception e) {
                        HookUtils.logError("Error in currentTimeMillis hook: " + e.getMessage());
                    }
                }
            });

            HookUtils.logInfo("Hooked System.currentTimeMillis() with behavioral state");
        } catch (Exception e) {
            HookUtils.logError("Failed to hook system clock: " + e.getMessage());
        }
    }

    private static void hookSystemElapsed(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> systemClass = XposedHelpers.findClass(
                "android.os.System", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(systemClass, "elapsedRealtime", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        long elapsedTime = (long) param.getResult();
                        if (elapsedTime > 0) {
                            long adjustedTime = applySessionPatterns(elapsedTime);
                            param.setResult(adjustedTime);
                        }
                    } catch (Exception e) {
                        HookUtils.logError("Error in elapsedRealtime hook: " + e.getMessage());
                    }
                }
            });

            XposedBridge.hookAllMethods(systemClass, "uptimeMillis", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        long upTime = (long) param.getResult();
                        if (upTime > 0) {
                            long adjustedTime = applyFatigue(upTime);
                            param.setResult(adjustedTime);
                        }
                    } catch (Exception e) {
                        HookUtils.logError("Error in uptimeMillis hook: " + e.getMessage());
                    }
                }
            });

            HookUtils.logInfo("Hooked System elapsed time methods");
        } catch (Exception e) {
            HookUtils.logError("Failed to hook system elapsed time: " + e.getMessage());
        }
    }

    private static void applyFatigue(long currentTime) {
        long sessionElapsed = currentTime - sessionStartTime;
        float minutesElapsed = sessionElapsed / 60000.0f;

        float fatigue = Math.min(minutesElapsed / 60.0f, 1.0f);

        currentFatigueLevel = fatigue;
        HookUtils.logDebug("Fatigue level: " + String.format("%.2f", fatigue));
    }

    private static void applyAttention(long currentTime) {
        long sessionElapsed = currentTime - sessionStartTime;
        float hoursElapsed = sessionElapsed / 3600000.0f;

        float attentionNoise = HookUtils.generateGaussianNoise(0.1f);
        float attentionDrop = (float) (Math.min(hoursElapsed / 8.0f, 1.0f) * 0.5f);

        float currentHour = (hoursElapsed % 24.0f);
        float attentionLevel = Math.max(0.3f, 1.0f - attentionDrop - (currentHour < 6.0f || currentHour > 22.0f ? 0.3f : 0.0f));
        attentionLevel += attentionNoise;

        if (Math.random() < 0.05) {
            attentionLevel = Math.max(0.4f, attentionLevel * 0.8f);
        }

        HookUtils.logDebug("Attention level: " + String.format("%.2f", attentionLevel));
    }

    private static void applyStress(long currentTime) {
        long sessionElapsed = currentTime - sessionStartTime;
        float minutesElapsed = sessionElapsed / 60000.0f;

        float interactionIntensity = Math.min(interactionCount / 100.0f, 1.0f);
        float stress = 0.3f + (interactionIntensity * 0.7f);

        float stressNoise = HookUtils.generateGaussianNoise(0.1f);
        stress = stress + stressNoise;

        if (minutesElapsed < 5.0f || minutesElapsed > 60.0f) {
            stress = Math.min(stress * 1.2f, 0.8f);
        }

        currentStressLevel = stress;
        HookUtils.logDebug("Stress level: " + String.format("%.2f", stress));
    }

    private static void applyDecisionInconsistency(long currentTime) {
        float errorProb = Math.min(0.05f + (currentFatigueLevel * 0.1f), 0.15f);

        if (HookUtils.getRandom().nextFloat() < errorProb) {
            HookUtils.logDebug("Decision inconsistency injected");
        }
    }

    private static long applySessionPatterns(long elapsedTime) {
        long adjustedTime = elapsedTime;

        long minuteInSession = (elapsedTime / 60000L) % 60L;
        
        if (minuteInSession < 30) {
            float variation = (float) (Math.sin(minuteInSession / 10.0) * 50.0);
            adjustedTime += (long) (variation * 1000.0);
        } else if (minuteInSession < 60) {
            float variation = (float) (Math.sin(minuteInSession / 15.0) * 80.0);
            adjustedTime += (long) (variation * 1500.0);
        } else {
            float variation = (float) (Math.sin(minuteInSession / 20.0) * 120.0);
            adjustedTime += (long) (variation * 2000.0);
        }

        return adjustedTime;
    }

    public static float getFatigueLevel() {
        return currentFatigueLevel;
    }

    public static float getAttentionLevel() {
        return attentionLevel;
    }

    public static float getStressLevel() {
        return currentStressLevel;
    }

    public static float getDecisionErrorProbability() {
        float fatigue = getFatigueLevel();
        float attention = getAttentionLevel();
        return Math.min(0.05f + (fatigue * 0.1f) + (attention * 0.15f), 0.15f);
    }

    public static void incrementInteractionCount() {
        interactionCount++;
    }

    public static int getSessionInteractionCount() {
        return interactionCount;
    }

    public static long getSessionDuration() {
        return System.currentTimeMillis() - sessionStartTime;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
