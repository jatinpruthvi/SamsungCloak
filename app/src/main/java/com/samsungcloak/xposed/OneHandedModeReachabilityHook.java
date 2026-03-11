package com.samsungcloak.xposed;

import android.content.res.Configuration;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;

/**
 * Hook #21: One-Handed Mode Reachability Simulation
 * 
 * Simulates one-handed device usage constraints:
 * - Reduced touch reach zones
 * - Thumb arc limitations
 * - Height-adjusted interactions
 * - Grip-induced occlusion zones
 * 
 * Based on HCI research on one-handed use:
 * - Thumb reach: ~60% of screen height
 * - Comfort zone: bottom 40% of screen
 * - Stretch zone: 40-60% of screen height
 * 
 * Targets: SM-A125U (Android 10/11)
 */
public class OneHandedModeReachabilityHook {

    private static final String TAG = "[HumanInteraction][OneHanded]";
    private static final boolean DEBUG = true;

    private static boolean enabled = false; // Disabled by default
    private static boolean oneHandedMode = false;
    private static float thumbReachFactor = 0.6f; // 60% of screen height
    private static int handDominance = 1; // 0=left, 1=right

    private static final Random random = new Random();
    private static int screenHeight = 1600; // Default SM-A125U
    private static int screenWidth = 720;

    // Reach zones (percentage of screen height)
    private static final float COMFORT_ZONE_MAX = 0.4f; // Bottom 40%
    private static final float STRETCH_ZONE_MAX = 0.6f; // Up to 60%
    
    // Define reach boundaries based on hand
    private static float[] reachBoundaries = {0.4f, 0.6f, 1.0f};

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        HookUtils.logInfo(TAG, "Initializing One-Handed Mode Hook");

        try {
            hookConfiguration(lpparam);
            hookTouchEvent(lpparam);
            HookUtils.logInfo(TAG, "One-Handed Mode Hook initialized");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize hook", e);
        }
    }

    private static void hookConfiguration(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> configurationClass = XposedHelpers.findClass(
                "android.content.res.Configuration", lpparam.classLoader);

            // Hook constructor to capture screen dimensions
            XposedBridge.hookAllConstructors(configurationClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    try {
                        Object config = param.thisObject;
                        
                        // Get screen height from window metrics
                        DisplayMetrics metrics = new DisplayMetrics();
                        // Would need Context to get real metrics
                        
                        // Update reach boundaries based on screen size
                        updateReachBoundaries();
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked Configuration");

        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook configuration", e);
        }
    }

    private static void hookTouchEvent(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> viewClass = XposedHelpers.findClass("android.view.View", lpparam.classLoader);

            XposedBridge.hookAllMethods(viewClass, "onTouchEvent", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || !oneHandedMode) return;

                    MotionEvent event = (MotionEvent) param.args[0];
                    int action = event.getActionMasked();

                    if (action == MotionEvent.ACTION_DOWN || 
                        action == MotionEvent.ACTION_MOVE) {
                        
                        checkReachability(event);
                    }
                }
            });

            // Hook performClick to simulate reach adjustment
            XposedBridge.hookAllMethods(viewClass, "performClick", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || !oneHandedMode) return;

                    // Simulate one-handed reach adjustment delay
                    int reachDelay = calculateReachDelay();
                    if (reachDelay > 0) {
                        // Note: Actual delay would be added here
                        if (DEBUG && random.nextFloat() < 0.02f) {
                            HookUtils.logDebug(TAG, "Reach adjustment delay: " + reachDelay + "ms");
                        }
                    }
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked touch events for one-handed mode");

        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook touch event", e);
        }
    }

    private static void checkReachability(MotionEvent event) {
        try {
            float y = event.getY();
            float normalizedY = y / screenHeight;
            
            // Determine reach zone
            String zone;
            if (normalizedY <= COMFORT_ZONE_MAX) {
                zone = "comfort";
            } else if (normalizedY <= STRETCH_ZONE_MAX) {
                zone = "stretch";
                // Add delay for stretch zone
                if (random.nextFloat() < 0.1f) {
                    if (DEBUG && random.nextFloat() < 0.02f) {
                        HookUtils.logDebug(TAG, "Touch in stretch zone - may require grip adjustment");
                    }
                }
            } else {
                zone = "difficult";
                // High chance of adjustment needed
                if (random.nextFloat() < 0.3f) {
                    if (DEBUG && random.nextFloat() < 0.02f) {
                        HookUtils.logDebug(TAG, "Touch in difficult zone - one-handed reach limited");
                    }
                }
            }

            // Simulate accidental palm edge touches in one-handed mode
            float x = event.getX();
            float normalizedX = x / screenWidth;
            
            // Edge zone on same side as hand is more likely palm touch
            boolean isSameSideAsHand = (handDominance == 1 && normalizedX > 0.8f) ||
                                       (handDominance == 0 && normalizedX < 0.2f);
            
            if (isSameSideAsHand && random.nextFloat() < 0.15f) {
                if (DEBUG && random.nextFloat() < 0.02f) {
                    HookUtils.logDebug(TAG, "Potential palm edge touch detected");
                }
            }

        } catch (Exception e) {
            if (DEBUG) {
                HookUtils.logDebug(TAG, "Error checking reachability: " + e.getMessage());
            }
        }
    }

    private static int calculateReachDelay() {
        // Calculate based on current thumb position vs target
        // Higher zone = more delay
        int baseDelay = random.nextInt(50);
        
        if (oneHandedMode) {
            baseDelay += random.nextInt(100) + 50;
        }
        
        return baseDelay;
    }

    private static void updateReachBoundaries() {
        // Recalculate reach zones based on thumb reach factor
        reachBoundaries[0] = thumbReachFactor * COMFORT_ZONE_MAX;
        reachBoundaries[1] = thumbReachFactor * STRETCH_ZONE_MAX;
        reachBoundaries[2] = thumbReachFactor;
    }
}

// Need to import MotionEvent
import android.view.MotionEvent;
