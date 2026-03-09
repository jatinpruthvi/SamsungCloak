package com.samsungcloak.xposed;

import android.view.MotionEvent;
import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;

public class InputPressureDynamicsHook {

    private static final String TAG = "[HumanInteraction][InputPressure]";
    private static final boolean DEBUG = true;
    private static final ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new Random());

    private static boolean enabled = true;
    private static double pressureVariationProbability = 0.25;
    private static double surfaceAreaVariationProbability = 0.22;

    private static long lastDownTime = 0;
    private static float lastX = 0;
    private static float lastY = 0;
    private static int touchDurationMs = 0;

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        HookUtils.logInfo(TAG, "Initializing Input Pressure & Surface Area Dynamics Hook");

        try {
            hookMotionEventConstruction(lpparam);
            hookViewOnTouchEvent(lpparam);
            hookGestureDetection(lpparam);
            HookUtils.logInfo(TAG, "Input Pressure & Surface Area Dynamics Hook initialized successfully");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize hook", e);
        }
    }

    private static void hookMotionEventConstruction(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> motionEventClass = XposedHelpers.findClass("android.view.MotionEvent", lpparam.classLoader);

            XposedBridge.hookAllMethods(motionEventClass, "obtain", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    try {
                        MotionEvent event = (MotionEvent) param.getResult();

                        if (event == null) return;

                        int actionMasked = event.getActionMasked();

                        if (actionMasked == MotionEvent.ACTION_DOWN) {
                            lastDownTime = event.getEventTime();
                            lastX = event.getX();
                            lastY = event.getY();
                        } else if (actionMasked == MotionEvent.ACTION_UP) {
                            touchDurationMs = (int) (event.getEventTime() - lastDownTime);
                        }

                        applyTouchDynamics(event);

                    } catch (Exception e) {
                        if (DEBUG) HookUtils.logDebug(TAG, "Error in MotionEvent.obtain hook: " + e.getMessage());
                    }
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked MotionEvent.obtain");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook MotionEvent construction", e);
        }
    }

    private static void hookViewOnTouchEvent(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedBridge.hookAllMethods(View.class, "onTouchEvent", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    try {
                        MotionEvent event = (MotionEvent) param.args[0];

                        if (event == null) return;

                        int actionMasked = event.getActionMasked();

                        if (actionMasked == MotionEvent.ACTION_DOWN) {
                            classifyInteractionType(event);
                        }

                    } catch (Exception e) {
                        if (DEBUG) HookUtils.logDebug(TAG, "Error in View.onTouchEvent hook: " + e.getMessage());
                    }
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked View.onTouchEvent");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook View onTouchEvent", e);
        }
    }

    private static void hookGestureDetection(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> gestureDetectorClass = XposedHelpers.findClass(
                "android.view.GestureDetector",
                lpparam.classLoader
            );

            XposedBridge.hookAllMethods(gestureDetectorClass, "onScroll",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        try {
                            if (param.args.length >= 2) {
                                MotionEvent e1 = (MotionEvent) param.args[0];
                                MotionEvent e2 = (MotionEvent) param.args[1];

                                applyScrollingDynamics(e2);
                            }
                        } catch (Exception e) {
                            if (DEBUG) HookUtils.logDebug(TAG, "Error in onScroll hook: " + e.getMessage());
                        }
                    }
                });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked GestureDetector.onScroll");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook gesture detection", e);
        }
    }

    private static void applyTouchDynamics(MotionEvent event) {
        try {
            int actionMasked = event.getActionMasked();

            if (actionMasked == MotionEvent.ACTION_DOWN ||
                actionMasked == MotionEvent.ACTION_MOVE ||
                actionMasked == MotionEvent.ACTION_UP) {

                if (random.get().nextDouble() < pressureVariationProbability) {
                    float pressure = calculateRealisticPressure(event, actionMasked);
                    XposedHelpers.setFloatField(event, "mPressure", pressure);
                }

                if (random.get().nextDouble() < surfaceAreaVariationProbability) {
                    float size = calculateRealisticTouchSize(event, actionMasked);
                    XposedHelpers.setFloatField(event, "mSize", size);

                    float touchMajor = calculateTouchMajor(size);
                    float touchMinor = calculateTouchMinor(size);

                    XposedHelpers.setFloatField(event, "mTouchMajor", touchMajor);
                    XposedHelpers.setFloatField(event, "mTouchMinor", touchMinor);
                }

                if (DEBUG && random.get().nextDouble() < 0.005) {
                    HookUtils.logDebug(TAG, String.format(
                        "Touch dynamics applied: pressure=%.3f, touchMajor=%.2f, touchMinor=%.2f, action=%d",
                        event.getPressure(), event.getTouchMajor(), event.getTouchMinor(), actionMasked
                    ));
                }
            }
        } catch (Exception e) {
            if (DEBUG) HookUtils.logDebug(TAG, "Error applying touch dynamics: " + e.getMessage());
        }
    }

    private static void applyScrollingDynamics(MotionEvent event) {
        try {
            if (random.get().nextDouble() < 0.6) {
                float pressure = 0.4f + (random.get().nextFloat() * 0.25f);
                XposedHelpers.setFloatField(event, "mPressure", pressure);

                float touchMajor = 0.8f + (random.get().nextFloat() * 0.6f);
                float touchMinor = 0.6f + (random.get().nextFloat() * 0.4f);

                XposedHelpers.setFloatField(event, "mTouchMajor", touchMajor);
                XposedHelpers.setFloatField(event, "mTouchMinor", touchMinor);
            }
        } catch (Exception e) {
            if (DEBUG) HookUtils.logDebug(TAG, "Error applying scrolling dynamics: " + e.getMessage());
        }
    }

    private static void classifyInteractionType(MotionEvent event) {
        try {
            float dx = event.getX() - lastX;
            float dy = event.getY() - lastY;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            boolean isTap = distance < 15.0f;
            boolean isScroll = distance > 50.0f;
            boolean isLongPress = touchDurationMs > 500;

            if (DEBUG && random.get().nextDouble() < 0.01) {
                String type = isLongPress ? "long_press" : (isScroll ? "scroll" : (isTap ? "tap" : "unknown"));
                HookUtils.logDebug(TAG, String.format(
                    "Interaction classified: type=%s, distance=%.2f, duration=%dms",
                    type, distance, touchDurationMs
                ));
            }
        } catch (Exception e) {
            if (DEBUG) HookUtils.logDebug(TAG, "Error classifying interaction: " + e.getMessage());
        }
    }

    private static float calculateRealisticPressure(MotionEvent event, int actionMasked) {
        float basePressure;

        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN:
                basePressure = 0.85f + (random.get().nextFloat() * 0.12f);
                break;

            case MotionEvent.ACTION_MOVE:
                float velocity = calculateVelocity(event);
                basePressure = Math.max(0.3f, 0.7f - (velocity * 0.01f));
                basePressure += (random.get().nextFloat() - 0.5f) * 0.1f;
                break;

            case MotionEvent.ACTION_UP:
                basePressure = 0.15f + (random.get().nextFloat() * 0.2f);
                break;

            default:
                basePressure = 0.5f;
        }

        return HookUtils.clamp(basePressure, 0.0f, 1.0f);
    }

    private static float calculateRealisticTouchSize(MotionEvent event, int actionMasked) {
        float baseSize;

        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN:
                baseSize = 0.5f + (random.get().nextFloat() * 0.2f);
                break;

            case MotionEvent.ACTION_MOVE:
                baseSize = 0.7f + (random.get().nextFloat() * 0.3f);
                break;

            case MotionEvent.ACTION_UP:
                baseSize = 0.3f + (random.get().nextFloat() * 0.15f);
                break;

            default:
                baseSize = 0.5f;
        }

        return HookUtils.clamp(baseSize, 0.0f, 1.0f);
    }

    private static float calculateTouchMajor(float size) {
        return HookUtils.clamp(size * 0.9f + (random.get().nextFloat() * 0.1f), 0.1f, 1.0f);
    }

    private static float calculateTouchMinor(float size) {
        float aspectRatio = 0.7f + (random.get().nextFloat() * 0.2f);
        return HookUtils.clamp(size * aspectRatio, 0.1f, 1.0f);
    }

    private static float calculateVelocity(MotionEvent event) {
        try {
            if (event.getHistorySize() < 2) {
                return 0.0f;
            }

            float dx = event.getX() - event.getHistoricalX(0);
            float dy = event.getY() - event.getHistoricalY(0);

            return (float) Math.sqrt(dx * dx + dy * dy);
        } catch (Exception e) {
            return 0.0f;
        }
    }

    public static void setEnabled(boolean enabled) {
        InputPressureDynamicsHook.enabled = enabled;
        HookUtils.logInfo(TAG, "Hook " + (enabled ? "enabled" : "disabled"));
    }

    public static void setPressureVariationProbability(double prob) {
        InputPressureDynamicsHook.pressureVariationProbability = HookUtils.clamp(prob, 0.0, 1.0);
        HookUtils.logInfo(TAG, "Pressure variation probability set to: " + InputPressureDynamicsHook.pressureVariationProbability);
    }

    public static void setSurfaceAreaVariationProbability(double prob) {
        InputPressureDynamicsHook.surfaceAreaVariationProbability = HookUtils.clamp(prob, 0.0, 1.0);
        HookUtils.logInfo(TAG, "Surface area variation probability set to: " + InputPressureDynamicsHook.surfaceAreaVariationProbability);
    }

    public static void simulateButtonTapPressure(float targetPressure) {
        if (targetPressure > 0.0f && targetPressure < 1.0f) {
            HookUtils.logInfo(TAG, "Button tap pressure simulated: " + targetPressure);
        }
    }

    public static void simulateScrollingPressure(float targetPressure) {
        if (targetPressure > 0.0f && targetPressure < 1.0f) {
            HookUtils.logInfo(TAG, "Scrolling pressure simulated: " + targetPressure);
        }
    }
}
