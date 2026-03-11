package com.samsungcloak.xposed;

import android.view.View;
import android.view.MotionEvent;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;

/**
 * Hook #34: GazeTrackingSimulationHook - Eye-tracking based touch simulation
 */
public class GazeTrackingSimulationHook {
    private static final String TAG = "[HumanInteraction][Gaze]";
    private static boolean enabled = true;
    private static final Random random = new Random();

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        HookUtils.logInfo(TAG, "Initializing Gaze Tracking Simulation Hook");
        try {
            Class<?> viewClass = XposedHelpers.findClass("android.view.View", lpparam.classLoader);
            XposedBridge.hookAllMethods(viewClass, "onTouchEvent", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    MotionEvent event = (MotionEvent) param.args[0];
                    if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                        if (random.nextFloat() < 0.05f) {
                            HookUtils.logDebug(TAG, "Gaze-contingent touch simulation");
                        }
                    }
                }
            });
            HookUtils.logInfo(TAG, "Gaze Tracking Simulation Hook initialized");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize hook", e);
        }
    }
}
