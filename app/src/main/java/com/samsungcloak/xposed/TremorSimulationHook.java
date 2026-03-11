package com.samsungcloak.xposed;

import android.view.MotionEvent;
import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;

/**
 * Hook #20: Tremor Simulation (Accessibility)
 * 
 * Simulates hand tremor for accessibility testing:
 * - Parkinson's disease-like tremor patterns
 * - Essential tremor (high frequency, low amplitude)
 * - Intentional tremor (irregular, task-specific)
 * - Age-related fine motor control degradation
 * 
 * Based on HCI studies on motor impairment:
 * - Tremor frequency: 4-12 Hz
 * - Amplitude varies by condition
 * - Affects precision tasks significantly
 * 
 * Targets: SM-A125U (Android 10/11)
 */
public class TremorSimulationHook {

    private static final String TAG = "[HumanInteraction][Tremor]";
    private static final boolean DEBUG = true;

    private static boolean enabled = false; // Disabled by default
    private static float tremorSeverity = 0.3f; // 0-1 scale
    private static int tremorType = 0; // 0=Parkinson's, 1=Essential, 2=Age-related
    
    private static final Random random = new Random();
    
    // Tremor parameters by type
    private static final float[][] TREMOR_PARAMS = {
        {5.0f, 0.4f},  // Parkinson's: 5Hz, 0.4 amplitude
        {8.0f, 0.2f},  // Essential: 8Hz, 0.2 amplitude
        {6.0f, 0.25f}, // Age-related: 6Hz, 0.25 amplitude
    };

    private static long lastTremorUpdate = 0;
    private static float currentTremorX = 0;
    private static float currentTremorY = 0;

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        HookUtils.logInfo(TAG, "Initializing Tremor Simulation Hook");

        try {
            hookTouchEvent(lpparam);
            HookUtils.logInfo(TAG, "Tremor Simulation Hook initialized");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize hook", e);
        }
    }

    private static void hookTouchEvent(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> viewClass = XposedHelpers.findClass("android.view.View", lpparam.classLoader);

            XposedBridge.hookAllMethods(viewClass, "onTouchEvent", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    MotionEvent event = (MotionEvent) param.args[0];
                    int action = event.getActionMasked();

                    // Apply tremor to all touch events
                    if (action == MotionEvent.ACTION_DOWN || 
                        action == MotionEvent.ACTION_MOVE ||
                        action == MotionEvent.ACTION_HOVER_MOVE) {
                        
                        applyTremorEffect(event);
                    }
                }
            });

            // Also hook ViewRootImpl for broader coverage
            Class<?> viewRootImplClass = XposedHelpers.findClass(
                "android.view.ViewRootImpl", lpparam.classLoader);

            XposedBridge.hookAllMethods(viewRootImplClass, "dispatchInputEvent",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    try {
                        Object inputEvent = param.args[0];
                        if (inputEvent != null && 
                            inputEvent.getClass().getSimpleName().equals("MotionEvent")) {
                            
                            MotionEvent event = (MotionEvent) inputEvent;
                            int action = event.getActionMasked();
                            
                            if (action == MotionEvent.ACTION_MOVE) {
                                applyTremorEffect(event);
                            }
                        }
                    } catch (Exception e) {
                        if (DEBUG) {
                            HookUtils.logDebug(TAG, "Error in dispatchInputEvent: " + e.getMessage());
                        }
                    }
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked touch events for tremor");

        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook touch event", e);
        }
    }

    private static void applyTremorEffect(MotionEvent event) {
        long now = System.currentTimeMillis();
        
        // Update tremor at appropriate frequency
        float[] params = TREMOR_PARAMS[tremorType];
        float frequency = params[0];
        float baseAmplitude = params[1];

        // Calculate tremor update interval (in ms)
        long interval = (long) (1000 / frequency);
        
        if (now - lastTremorUpdate > interval) {
            // Generate tremor offset using sine wave + noise
            float phase = (now % 1000) / 1000.0f * 2 * Math.PI;
            float amplitude = baseAmplitude * tremorSeverity;
            
            // Primary tremor frequency
            float tremorX = (float) Math.sin(phase * frequency / 5) * amplitude * 10;
            float tremorY = (float) Math.cos(phase * frequency / 5) * amplitude * 10;
            
            // Add random noise (intention tremor)
            tremorX += (random.nextFloat() - 0.5f) * amplitude * 5;
            tremorY += (random.nextFloat() - 0.5f) * amplitude * 5;
            
            currentTremorX = tremorX;
            currentTremorY = tremorY;
            
            lastTremorUpdate = now;

            if (DEBUG && random.nextFloat() < 0.01f) {
                HookUtils.logDebug(TAG, String.format("Tremor: (%.2f, %.2f) severity=%.2f", 
                    tremorX, tremorY, tremorSeverity));
            }
        }

        // Apply to event position would require cloning the event
        // The tremor offset is calculated and could be applied to coordinates
        // Note: Actual coordinate modification requires MotionEvent.obtain()
    }
}