package com.samsungcloak.xposed;

import android.content.Intent;
import android.os.Bundle;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Stack;

public class InterAppNavigationHook {

    private static final String TAG = "[HumanInteraction][InterAppNav]";
    private static final boolean DEBUG = true;
    private static final ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new Random());

    private static boolean enabled = true;
    private static double referralFlowProbability = 0.18;
    private static double deepLinkProbability = 0.12;
    private static double backStackModificationProbability = 0.15;

    private static final Stack<IntentEntry> intentBackStack = new Stack<>();
    private static final Map<String, Double> referralSources = new HashMap<>();

    private static String currentPackage = null;
    private static long lastActivitySwitchTime = 0;

    static {
        referralSources.put("com.android.browser", 0.35);
        referralSources.put("com.chrome.browser", 0.30);
        referralSources.put("com.instagram.android", 0.15);
        referralSources.put("com.facebook.katana", 0.12);
        referralSources.put("com.twitter.android", 0.08);
    }

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        HookUtils.logInfo(TAG, "Initializing Inter-App Navigation Hook");

        try {
            hookActivityStart(lpparam);
            hookActivityResume(lpparam);
            hookTaskStackBuilder(lpparam);
            HookUtils.logInfo(TAG, "Inter-App Navigation Hook initialized successfully");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize hook", e);
        }
    }

    private static void hookActivityStart(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedBridge.hookAllMethods(XposedHelpers.findClass("android.app.Instrumentation", lpparam.classLoader),
                "execStartActivity", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        try {
                            if (param.args.length < 2) return;

                            Intent intent = null;
                            for (Object arg : param.args) {
                                if (arg instanceof Intent) {
                                    intent = (Intent) arg;
                                    break;
                                }
                            }

                            if (intent == null) return;

                            processIntent(intent, param);

                        } catch (Exception e) {
                            if (DEBUG) HookUtils.logDebug(TAG, "Error in execStartActivity hook: " + e.getMessage());
                        }
                    }
                });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked Instrumentation.execStartActivity");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook Activity start", e);
        }
    }

    private static void hookActivityResume(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedBridge.hookAllMethods(XposedHelpers.findClass("android.app.Activity", lpparam.classLoader),
                "onResume", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        try {
                            String packageName = (String) XposedHelpers.getObjectField(param.thisObject, "mPackageName");
                            currentPackage = packageName;
                            lastActivitySwitchTime = System.currentTimeMillis();

                            if (DEBUG && random.get().nextDouble() < 0.01) {
                                HookUtils.logDebug(TAG, "Activity resumed: " + packageName);
                            }
                        } catch (Exception e) {
                            if (DEBUG) HookUtils.logDebug(TAG, "Error in onResume hook: " + e.getMessage());
                        }
                    }
                });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked Activity.onResume");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook Activity resume", e);
        }
    }

    private static void hookTaskStackBuilder(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> taskStackBuilderClass = XposedHelpers.findClass(
                "android.app.TaskStackBuilder",
                lpparam.classLoader
            );

            XposedBridge.hookAllMethods(taskStackBuilderClass, "startActivities",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        try {
                            if (random.get().nextDouble() < backStackModificationProbability) {
                                modifyTaskStack(param);
                            }
                        } catch (Exception e) {
                            if (DEBUG) HookUtils.logDebug(TAG, "Error in startActivities hook: " + e.getMessage());
                        }
                    }
                });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked TaskStackBuilder.startActivities");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook TaskStackBuilder", e);
        }
    }

    private static void processIntent(Intent intent, XC_MethodHook.MethodHookParam param) {
        String action = intent.getAction();

        if (Intent.ACTION_VIEW.equals(action)) {
            if (random.get().nextDouble() < deepLinkProbability) {
                injectReferralContext(intent, param);
            }
        } else {
            if (random.get().nextDouble() < referralFlowProbability) {
                simulateReferralFlow(intent, param);
            }
        }

        pushToBackStack(intent);
    }

    private static void injectReferralContext(Intent intent, XC_MethodHook.MethodHookParam param) {
        try {
            String referrerPackage = selectReferrerPackage();

            Bundle extras = intent.getExtras() != null ? intent.getExtras() : new Bundle();

            extras.putString("android.intent.extra.REFERRER", "android-app://" + referrerPackage);
            extras.putString("android.intent.extra.REFERRER_NAME", referrerPackage);
            extras.putLong("android.intent.extra.REFERRER_TIME", System.currentTimeMillis());

            String utmSource = generateUTMParameter(referrerPackage);
            String utmMedium = random.get().nextDouble() < 0.6 ? "social" : "referral";

            extras.putString("utm_source", utmSource);
            extras.putString("utm_medium", utmMedium);

            if (DEBUG) {
                HookUtils.logDebug(TAG, String.format(
                    "Injected referral context: referrer=%s, utm_source=%s",
                    referrerPackage, utmSource
                ));
            }
        } catch (Exception e) {
            if (DEBUG) HookUtils.logDebug(TAG, "Error injecting referral context: " + e.getMessage());
        }
    }

    private static void simulateReferralFlow(Intent intent, XC_MethodHook.MethodHookParam param) {
        try {
            String referrerPackage = selectReferrerPackage();

            Bundle extras = intent.getExtras() != null ? intent.getExtras() : new Bundle();

            extras.putString("referrer_package", referrerPackage);
            extras.putLong("referral_timestamp", System.currentTimeMillis());

            long timeSinceLastSwitch = System.currentTimeMillis() - lastActivitySwitchTime;
            if (timeSinceLastSwitch > 0 && timeSinceLastSwitch < 30000) {
                extras.putLong("time_since_referral_ms", timeSinceLastSwitch);
            }

            if (DEBUG) {
                HookUtils.logDebug(TAG, String.format(
                    "Simulated referral flow: referrer=%s, intent=%s",
                    referrerPackage, intent.getAction()
                ));
            }
        } catch (Exception e) {
            if (DEBUG) HookUtils.logDebug(TAG, "Error simulating referral flow: " + e.getMessage());
        }
    }

    private static void modifyTaskStack(XC_MethodHook.MethodHookParam param) {
        try {
            if (!intentBackStack.isEmpty()) {
                IntentEntry previousIntent = intentBackStack.peek();

                if (System.currentTimeMillis() - previousIntent.timestamp < 60000) {
                    Bundle extras = new Bundle();
                    extras.putString("stack_context", "simulated_back_navigation");
                    extras.putString("previous_package", previousIntent.packageName);
                    extras.putLong("previous_intent_time", previousIntent.timestamp);

                    if (param.args.length > 0 && param.args[0] instanceof Intent[]) {
                        Intent[] intents = (Intent[]) param.args[0];
                        if (intents.length > 0 && intents[0] != null) {
                            intents[0].putExtras(extras);

                            if (DEBUG) {
                                HookUtils.logDebug(TAG, String.format(
                                    "Modified task stack: previous=%s, current=%s",
                                    previousIntent.packageName, intents[0].getComponent()
                                ));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (DEBUG) HookUtils.logDebug(TAG, "Error modifying task stack: " + e.getMessage());
        }
    }

    private static void pushToBackStack(Intent intent) {
        try {
            String packageName = intent.getPackage();
            if (packageName == null && intent.getComponent() != null) {
                packageName = intent.getComponent().getPackageName();
            }

            if (packageName != null) {
                IntentEntry entry = new IntentEntry(
                    packageName,
                    intent.getAction(),
                    System.currentTimeMillis()
                );

                intentBackStack.push(entry);

                if (intentBackStack.size() > 20) {
                    intentBackStack.remove(0);
                }
            }
        } catch (Exception e) {
            if (DEBUG) HookUtils.logDebug(TAG, "Error pushing to back stack: " + e.getMessage());
        }
    }

    private static String selectReferrerPackage() {
        double rand = random.get().nextDouble();
        double cumulative = 0.0;

        for (Map.Entry<String, Double> entry : referralSources.entrySet()) {
            cumulative += entry.getValue();
            if (rand <= cumulative) {
                return entry.getKey();
            }
        }

        return "com.android.browser";
    }

    private static String generateUTMParameter(String packageName) {
        switch (packageName) {
            case "com.android.browser":
                return "android_browser";
            case "com.chrome.browser":
                return "chrome";
            case "com.instagram.android":
                return "instagram";
            case "com.facebook.katana":
                return "facebook";
            case "com.twitter.android":
                return "twitter";
            default:
                return "unknown_referrer";
        }
    }

    private static class IntentEntry {
        final String packageName;
        final String action;
        final long timestamp;

        IntentEntry(String packageName, String action, long timestamp) {
            this.packageName = packageName;
            this.action = action;
            this.timestamp = timestamp;
        }
    }

    public static void setEnabled(boolean enabled) {
        InterAppNavigationHook.enabled = enabled;
        HookUtils.logInfo(TAG, "Hook " + (enabled ? "enabled" : "disabled"));
    }

    public static void setReferralFlowProbability(double prob) {
        InterAppNavigationHook.referralFlowProbability = HookUtils.clamp(prob, 0.0, 1.0);
        HookUtils.logInfo(TAG, "Referral flow probability set to: " + InterAppNavigationHook.referralFlowProbability);
    }

    public static void setDeepLinkProbability(double prob) {
        InterAppNavigationHook.deepLinkProbability = HookUtils.clamp(prob, 0.0, 1.0);
        HookUtils.logInfo(TAG, "Deep link probability set to: " + InterAppNavigationHook.deepLinkProbability);
    }

    public static void setBackStackModificationProbability(double prob) {
        InterAppNavigationHook.backStackModificationProbability = HookUtils.clamp(prob, 0.0, 1.0);
        HookUtils.logInfo(TAG, "Back stack modification probability set to: " + InterAppNavigationHook.backStackModificationProbability);
    }

    public static void addReferralSource(String packageName, double probability) {
        if (probability > 0.0 && probability < 1.0) {
            referralSources.put(packageName, probability);
            HookUtils.logInfo(TAG, "Added referral source: " + packageName + " (" + probability + ")");
        }
    }

    public static void clearBackStack() {
        intentBackStack.clear();
        HookUtils.logInfo(TAG, "Intent back stack cleared");
    }
}
