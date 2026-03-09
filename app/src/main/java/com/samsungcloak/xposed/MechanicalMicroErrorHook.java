package com.samsungcloak.xposed;

import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;

public class MechanicalMicroErrorHook {

    private static final String TAG = "[HumanInteraction][MechanicalError]";
    private static final boolean DEBUG = true;
    private static final ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new Random());

    private static boolean enabled = true;
    private static double errorRate = 0.08;
    private static double nearMissProbability = 0.15;
    private static double correctionSwipeProbability = 0.12;

    private static long lastActionDownTime = 0;
    private static int lastPointerId = -1;

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        HookUtils.logInfo(TAG, "Initializing Mechanical Micro-Error Hook");

        try {
            hookViewOnTouchEvent(lpparam);
            hookDispatchTouchEvent(lpparam);
            HookUtils.logInfo(TAG, "Mechanical Micro-Error Hook initialized successfully");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize hook", e);
        }
    }

    private static void hookViewOnTouchEvent(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedBridge.hookAllMethods(View.class, "onTouchEvent", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    MotionEvent event = (MotionEvent) param.args[0];
                    int actionMasked = event.getActionMasked();

                    if (actionMasked == MotionEvent.ACTION_DOWN) {
                        lastActionDownTime = event.getEventTime();
                        lastPointerId = event.getPointerId(0);
                    } else if (actionMasked == MotionEvent.ACTION_UP) {
                        View view = (View) param.thisObject;
                        applyFatFingerError(view, event, param);
                    } else if (actionMasked == MotionEvent.ACTION_MOVE) {
                        applyCorrectionSwipe(event, param);
                    }
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked View.onTouchEvent");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook View.onTouchEvent", e);
        }
    }

    private static void hookDispatchTouchEvent(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedBridge.hookAllMethods(XposedHelpers.findClass("android.view.ViewRootImpl", lpparam.classLoader),
                "dispatchInputEvent", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        try {
                            Object inputEvent = param.args[0];
                            if (inputEvent != null && inputEvent.getClass().getName().equals("android.view.MotionEvent")) {
                                MotionEvent event = (MotionEvent) inputEvent;
                                applyNearMissOffset(event);
                            }
                        } catch (Exception e) {
                            if (DEBUG) HookUtils.logDebug(TAG, "Error in dispatchInputEvent hook: " + e.getMessage());
                        }
                    }
                });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked ViewRootImpl.dispatchInputEvent");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook dispatchInputEvent", e);
        }
    }

    private static void applyFatFingerError(View view, MotionEvent event, XC_MethodHook.MethodHookParam param) {
        if (random.get().nextDouble() > errorRate) return;

        try {
            Rect viewRect = new Rect();
            view.getGlobalVisibleRect(viewRect);

            int viewWidth = viewRect.width();
            int viewHeight = viewRect.height();

            float centerX = viewRect.centerX();
            float centerY = viewRect.centerY();

            float touchX = event.getX();
            float touchY = event.getY();

            float distanceFromCenter = (float) Math.sqrt(
                Math.pow(touchX - viewRect.left - centerX, 2) +
                Math.pow(touchY - viewRect.top - centerY, 2)
            );

            boolean isNearEdge = distanceFromCenter > Math.min(viewWidth, viewHeight) * 0.35;

            if (isNearEdge && random.get().nextDouble() < nearMissProbability) {
                float offsetX = generateNearMissOffset(viewWidth);
                float offsetY = generateNearMissOffset(viewHeight);

                MotionEvent modifiedEvent = MotionEvent.obtain(event);
                modifiedEvent.offsetLocation(offsetX, offsetY);

                param.args[0] = modifiedEvent;

                if (DEBUG) {
                    HookUtils.logDebug(TAG, String.format(
                        "Fat-finger error applied: offset=(%.2f,%.2f), viewSize=(%d,%d)",
                        offsetX, offsetY, viewWidth, viewHeight
                    ));
                }
            }
        } catch (Exception e) {
            if (DEBUG) HookUtils.logDebug(TAG, "Error applying fat-finger error: " + e.getMessage());
        }
    }

    private static void applyNearMissOffset(MotionEvent event) {
        if (random.get().nextDouble() > 0.03) return;

        try {
            int displayWidth = 720;
            int displayHeight = 1600;

            float offsetX = (random.get().nextFloat() - 0.5f) * 15.0f;
            float offsetY = (random.get().nextFloat() - 0.5f) * 15.0f;

            event.offsetLocation(offsetX, offsetY);

            if (DEBUG) {
                HookUtils.logDebug(TAG, String.format(
                    "Near-miss offset applied: (%.2f, %.2f)",
                    offsetX, offsetY
                ));
            }
        } catch (Exception e) {
            if (DEBUG) HookUtils.logDebug(TAG, "Error applying near-miss offset: " + e.getMessage());
        }
    }

    private static void applyCorrectionSwipe(MotionEvent event, XC_MethodHook.MethodHookParam param) {
        if (event.getHistorySize() < 2) return;

        try {
            float dx = event.getX() - event.getHistoricalX(0);
            float dy = event.getY() - event.getHistoricalY(0);

            float velocity = (float) Math.sqrt(dx * dx + dy * dy);

            if (velocity > 50.0f && random.get().nextDouble() < correctionSwipeProbability) {
                float jitterX = (random.get().nextFloat() - 0.5f) * 8.0f;
                float jitterY = (random.get().nextFloat() - 0.5f) * 8.0f;

                MotionEvent modifiedEvent = MotionEvent.obtain(event);
                modifiedEvent.offsetLocation(jitterX, jitterY);

                param.args[0] = modifiedEvent;

                if (DEBUG) {
                    HookUtils.logDebug(TAG, String.format(
                        "Correction swipe applied: jitter=(%.2f,%.2f), velocity=%.2f",
                        jitterX, jitterY, velocity
                    ));
                }
            }
        } catch (Exception e) {
            if (DEBUG) HookUtils.logDebug(TAG, "Error applying correction swipe: " + e.getMessage());
        }
    }

    private static float generateNearMissOffset(int dimension) {
        float maxOffset = dimension * 0.15f;
        float offset = (random.get().nextFloat() - 0.5f) * 2.0f * maxOffset;

        if (Math.abs(offset) < 2.0f) {
            offset += offset > 0 ? 3.0f : -3.0f;
        }

        return offset;
    }

    public static void setEnabled(boolean enabled) {
        MechanicalMicroErrorHook.enabled = enabled;
        HookUtils.logInfo(TAG, "Hook " + (enabled ? "enabled" : "disabled"));
    }

    public static void setErrorRate(double rate) {
        MechanicalMicroErrorHook.errorRate = HookUtils.clamp(rate, 0.0, 1.0);
        HookUtils.logInfo(TAG, "Error rate set to: " + MechanicalMicroErrorHook.errorRate);
    }

    public static void setNearMissProbability(double prob) {
        MechanicalMicroErrorHook.nearMissProbability = HookUtils.clamp(prob, 0.0, 1.0);
        HookUtils.logInfo(TAG, "Near-miss probability set to: " + MechanicalMicroErrorHook.nearMissProbability);
    }

    public static void setCorrectionSwipeProbability(double prob) {
        MechanicalMicroErrorHook.correctionSwipeProbability = HookUtils.clamp(prob, 0.0, 1.0);
        HookUtils.logInfo(TAG, "Correction swipe probability set to: " + MechanicalMicroErrorHook.correctionSwipeProbability);
    }
}
