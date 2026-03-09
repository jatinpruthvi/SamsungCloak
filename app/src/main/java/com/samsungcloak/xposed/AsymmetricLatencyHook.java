package com.samsungcloak.xposed;

import android.app.Activity;
import android.app.Fragment;
import android.os.Handler;
import android.os.Looper;
import android.view.Choreographer;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class AsymmetricLatencyHook {

    private static final String TAG = "[HumanInteraction][AsymmetricLatency]";
    private static final boolean DEBUG = true;
    private static final ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new Random());

    private static boolean enabled = true;
    private static double hesitationProbability = 0.30;

    private static final double BASE_LATENCY_MS = 250.0;
    private static final double MIN_LATENCY_MS = 80.0;
    private static final double MAX_LATENCY_MS = 1500.0;

    private static final ConcurrentHashMap<String, ViewLoadInfo> viewLoadTimes = new ConcurrentHashMap<>();
    private static final AtomicLong lastInteractionTime = new AtomicLong(System.currentTimeMillis());
    private static final AtomicBoolean inputBlocked = new AtomicBoolean(false);

    private static Handler latencyHandler = new Handler(Looper.getMainLooper());

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        HookUtils.logInfo(TAG, "Initializing Asymmetric Latency Hook");

        try {
            hookActivityLifecycle(lpparam);
            hookFragmentLifecycle(lpparam);
            hookChoreographerCallbacks(lpparam);
            hookInputProcessing(lpparam);
            HookUtils.logInfo(TAG, "Asymmetric Latency Hook initialized successfully");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize hook", e);
        }
    }

    private static void hookActivityLifecycle(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedBridge.hookAllMethods(Activity.class, "onResume", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    try {
                        String activityName = param.thisObject.getClass().getName();

                        ViewLoadInfo loadInfo = new ViewLoadInfo(
                            activityName,
                            "activity",
                            System.currentTimeMillis()
                        );

                        viewLoadTimes.put(activityName, loadInfo);

                        long loadLatency = calculateLoadLatency(loadInfo);
                        applyProcessingHesitation(loadLatency, activityName);

                        if (DEBUG) {
                            HookUtils.logDebug(TAG, String.format(
                                "Activity resumed: %s, latency=%dms",
                                activityName, loadLatency
                            ));
                        }
                    } catch (Exception e) {
                        if (DEBUG) HookUtils.logDebug(TAG, "Error in Activity.onResume hook: " + e.getMessage());
                    }
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked Activity.onResume");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook Activity lifecycle", e);
        }
    }

    private static void hookFragmentLifecycle(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedBridge.hookAllMethods(XposedHelpers.findClass("android.app.Fragment", lpparam.classLoader),
                "onResume", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        try {
                            String fragmentName = param.thisObject.getClass().getName();

                            ViewLoadInfo loadInfo = new ViewLoadInfo(
                                fragmentName,
                                "fragment",
                                System.currentTimeMillis()
                            );

                            viewLoadTimes.put(fragmentName, loadInfo);

                            long loadLatency = calculateLoadLatency(loadInfo);
                            applyProcessingHesitation(loadLatency / 2, fragmentName);

                            if (DEBUG) {
                                HookUtils.logDebug(TAG, String.format(
                                    "Fragment resumed: %s, latency=%dms",
                                    fragmentName, loadLatency
                                ));
                            }
                        } catch (Exception e) {
                            if (DEBUG) HookUtils.logDebug(TAG, "Error in Fragment.onResume hook: " + e.getMessage());
                        }
                    }
                });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked Fragment.onResume");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook Fragment lifecycle", e);
        }
    }

    private static void hookChoreographerCallbacks(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedBridge.hookAllMethods(Choreographer.class, "postCallback",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        try {
                            int callbackType = (int) param.args[0];

                            if (callbackType == Choreographer.CALLBACK_INPUT) {
                                processInputCallback(param);
                            }
                        } catch (Exception e) {
                            if (DEBUG) HookUtils.logDebug(TAG, "Error in Choreographer callback hook: " + e.getMessage());
                        }
                    }
                });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked Choreographer.postCallback");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook Choreographer callbacks", e);
        }
    }

    private static void hookInputProcessing(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedBridge.hookAllMethods(XposedHelpers.findClass("android.view.ViewRootImpl", lpparam.classLoader),
                "dispatchInputEvent", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        try {
                            if (inputBlocked.get()) {
                                param.setResult(null);

                                if (DEBUG) {
                                    HookUtils.logDebug(TAG, "Input blocked during processing hesitation");
                                }
                            }
                        } catch (Exception e) {
                            if (DEBUG) HookUtils.logDebug(TAG, "Error in dispatchInputEvent hook: " + e.getMessage());
                        }
                    }
                });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked ViewRootImpl.dispatchInputEvent");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook input processing", e);
        }
    }

    private static void processInputCallback(XC_MethodHook.MethodHookParam param) {
        long timeSinceLastInteraction = System.currentTimeMillis() - lastInteractionTime.get();

        if (timeSinceLastInteraction < 500) {
            return;
        }

        if (random.get().nextDouble() < hesitationProbability) {
            long hesitationDelay = calculateHesitationDelay();
            inputBlocked.set(true);

            latencyHandler.postDelayed(() -> {
                inputBlocked.set(false);
                lastInteractionTime.set(System.currentTimeMillis());

                if (DEBUG) {
                    HookUtils.logDebug(TAG, "Processing hesitation completed: delay=" + hesitationDelay + "ms");
                }
            }, hesitationDelay);

            if (DEBUG) {
                HookUtils.logDebug(TAG, "Processing hesitation triggered: delay=" + hesitationDelay + "ms");
            }
        }
    }

    private static long calculateLoadLatency(ViewLoadInfo loadInfo) {
        double baseLatency = BASE_LATENCY_MS;

        double complexityFactor = 1.0;

        String viewType = loadInfo.viewType;
        String viewName = loadInfo.viewName;

        if (viewType.equals("activity")) {
            if (viewName.contains("List") || viewName.contains("Grid")) {
                complexityFactor = 1.4;
            } else if (viewName.contains("Detail") || viewName.contains("Content")) {
                complexityFactor = 1.2;
            }
        } else if (viewType.equals("fragment")) {
            complexityFactor = 0.8;
        }

        double randomFactor = 0.8 + (random.get().nextDouble() * 0.4);

        long latency = (long) (baseLatency * complexityFactor * randomFactor);

        return (long) HookUtils.clamp(latency, MIN_LATENCY_MS, MAX_LATENCY_MS);
    }

    private static long calculateHesitationDelay() {
        double baseDelay = 200.0;

        double cognitiveLoadFactor = 1.0;

        long recentLoadTime = getMostRecentViewLoadTime();
        long timeSinceLoad = System.currentTimeMillis() - recentLoadTime;

        if (timeSinceLoad < 1000) {
            cognitiveLoadFactor = 1.6;
        } else if (timeSinceLoad < 3000) {
            cognitiveLoadFactor = 1.3;
        } else if (timeSinceLoad < 10000) {
            cognitiveLoadFactor = 1.1;
        }

        double randomVariation = 0.7 + (random.get().nextDouble() * 0.6);

        long hesitationDelay = (long) (baseDelay * cognitiveLoadFactor * randomVariation);

        return (long) HookUtils.clamp(hesitationDelay, 80.0, 1500.0);
    }

    private static long getMostRecentViewLoadTime() {
        long mostRecentTime = 0;

        for (ViewLoadInfo info : viewLoadTimes.values()) {
            if (info.loadTime > mostRecentTime) {
                mostRecentTime = info.loadTime;
            }
        }

        return mostRecentTime;
    }

    private static void applyProcessingHesitation(long loadLatency, String viewName) {
        if (random.get().nextDouble() < hesitationProbability) {
            double hesitationRatio = 0.2 + (random.get().nextDouble() * 0.3);
            long hesitationDelay = (long) (loadLatency * hesitationRatio);

            inputBlocked.set(true);

            latencyHandler.postDelayed(() -> {
                inputBlocked.set(false);

                if (DEBUG) {
                    HookUtils.logDebug(TAG, String.format(
                        "Post-load hesitation applied for %s: delay=%dms (load=%dms)",
                        viewName, hesitationDelay, loadLatency
                    ));
                }
            }, hesitationDelay);
        }
    }

    private static class ViewLoadInfo {
        final String viewName;
        final String viewType;
        final long loadTime;

        ViewLoadInfo(String viewName, String viewType, long loadTime) {
            this.viewName = viewName;
            this.viewType = viewType;
            this.loadTime = loadTime;
        }
    }

    public static void setEnabled(boolean enabled) {
        AsymmetricLatencyHook.enabled = enabled;
        HookUtils.logInfo(TAG, "Hook " + (enabled ? "enabled" : "disabled"));
    }

    public static void setHesitationProbability(double prob) {
        AsymmetricLatencyHook.hesitationProbability = HookUtils.clamp(prob, 0.0, 1.0);
        HookUtils.logInfo(TAG, "Hesitation probability set to: " + AsymmetricLatencyHook.hesitationProbability);
    }

    public static void setBaseLatency(double latencyMs) {
        if (latencyMs >= 50.0 && latencyMs <= 2000.0) {
            HookUtils.logInfo(TAG, "Base latency set to: " + latencyMs + "ms");
        } else {
            HookUtils.logError(TAG, "Invalid base latency: " + latencyMs);
        }
    }

    public static void simulateCognitiveLoad(boolean highCognitiveLoad) {
        if (highCognitiveLoad) {
            setHesitationProbability(0.45);
            HookUtils.logInfo(TAG, "Simulating high cognitive load mode");
        } else {
            setHesitationProbability(0.30);
            HookUtils.logInfo(TAG, "Simulating normal cognitive load mode");
        }
    }

    public static void clearViewLoadHistory() {
        viewLoadTimes.clear();
        HookUtils.logInfo(TAG, "View load history cleared");
    }
}
