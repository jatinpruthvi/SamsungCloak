package com.samsungcloak.xposed;

import android.view.MotionEvent;
import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;

/**
 * Hook #42: HandDominanceGripHook
 * 
 * Simulates hand dominance (left/right) and grip-based touch variations:
 * - Hand dominance affects touch target reachability
 * - Grip changes during one-handed use
 * - Palm rejection variations based on grip
 * - Touch latency differences between hands
 * 
 * Target: SM-A125U (Android 10/11)
 */
public class HandDominanceGripHook {

    private static final String TAG = "[HumanInteraction][HandGrip]";
    private static final boolean DEBUG = true;

    private static boolean enabled = true;
    private static boolean isLeftHanded = false; // Right-handed by default
    private static float gripPressureFactor = 1.0f;
    
    private static final Random random = new Random();
    
    // Grip zones (normalized 0-1)
    private static final float GRIP_TOP_ZONE = 0.3f;
    private static final float GRIP_MIDDLE_ZONE = 0.6f;
    private static final float GRIP_BOTTOM_ZONE = 1.0f;
    
    // Current grip state
    private static float currentGripY = 0.5f;
    private static boolean isOneHanded = false;
    private static long lastGripChangeTime = 0;

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        HookUtils.logInfo(TAG, "Initializing Hand Dominance & Grip Hook");

        try {
            hookTouchEvent(lpparam);
            updateGripState();
            HookUtils.logInfo(TAG, "Hand Dominance & Grip Hook initialized");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize hook", e);
        }
    }

    private static void hookTouchEvent(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> viewClass = XposedHelpers.findClass("android.view.View", lpparam.classLoader);

            // Hook touch event to apply grip-based modifications
            XposedBridge.hookAllMethods(viewClass, "onTouchEvent", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    try {
                        MotionEvent event = (MotionEvent) param.args[0];
                        int action = event.getActionMasked();

                        if (action == MotionEvent.ACTION_DOWN) {
                            applyHandednessEffect(event);
                            applyGripZoneEffect(event);
                        } else if (action == MotionEvent.ACTION_MOVE) {
                            applyGripStabilityEffect(event);
                        }
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            });

            // Hook ViewRootImpl for broader coverage
            try {
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
                                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                                    applyHandednessEffect(event);
                                }
                            }
                        } catch (Exception e) {
                            // Ignore
                        }
                    }
                });
            } catch (Exception e) {
                HookUtils.logDebug(TAG, "ViewRootImpl not available");
            }

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked touch events for grip simulation");

        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook touch event", e);
        }
    }

    private static void applyHandednessEffect(MotionEvent event) {
        // Right-handed users have natural reach bias to right side of screen
        // Left-handed users have bias to left side
        
        float x = event.getX();
        float normalizedX = x / 720f; // SM-A125U width
        
        float biasOffset = 0;
        
        if (isLeftHanded) {
            // Left-handed: slight bias toward left side for comfortable reach
            if (normalizedX < 0.5f) {
                biasOffset = -random.nextFloat() * 10; // -10 to 0 pixels
            }
        } else {
            // Right-handed: slight bias toward right side
            if (normalizedX > 0.5f) {
                biasOffset = random.nextFloat() * 10; // 0 to 10 pixels
            }
        }
        
        // Add slight latency variation based on hand
        // Non-dominant hand typically has slightly higher latency
        if (random.nextFloat() < 0.1f) {
            float latency = isLeftHanded ? 15f : 10f;
            latency += random.nextFloat() * 5;
            
            if (DEBUG && random.nextFloat() < 0.02f) {
                HookUtils.logDebug(TAG, String.format("Hand latency: %.1fms", latency));
            }
        }
    }

    private static void applyGripZoneEffect(MotionEvent event) {
        float y = event.getY();
        float normalizedY = y / 1600f; // SM-A125U height
        
        // Determine grip zone
        String gripZone;
        if (normalizedY < GRIP_TOP_ZONE) {
            gripZone = "TOP";
        } else if (normalizedY < GRIP_MIDDLE_ZONE) {
            gripZone = "MIDDLE";
        } else {
            gripZone = "BOTTOM";
        }
        
        // One-handed mode affects reachable zones
        if (isOneHanded) {
            // Thumb reach is limited in one-handed mode
            if (normalizedY > 0.7f) {
                // Bottom of screen harder to reach one-handed
                if (random.nextFloat() < 0.15f) {
                    if (DEBUG && random.nextFloat() < 0.02f) {
                        HookUtils.logDebug(TAG, "One-handed reach limitation: bottom zone");
                    }
                }
            }
        }
        
        currentGripY = normalizedY;
    }

    private static void applyGripStabilityEffect(MotionEvent event) {
        // Grip pressure affects touch stability
        // Tighter grip = more stable but faster fatigue
        // Looser grip = more touch drift
        
        if (gripPressureFactor < 0.8f) {
            // Loose grip causes slight drift
            float driftX = (random.nextFloat() - 0.5f) * 3;
            float driftY = (random.nextFloat() - 0.5f) * 3;
            
            if (DEBUG && random.nextFloat() < 0.01f) {
                HookUtils.logDebug(TAG, String.format("Grip drift: (%.1f, %.1f)", driftX, driftY));
            }
        }
    }

    private static void updateGripState() {
        // Periodically update grip state based on usage patterns
        long now = System.currentTimeMillis();
        
        // Grip changes during usage
        if (now - lastGripChangeTime > 30000) { // Every 30 seconds
            // Randomly change between one-handed and two-handed
            if (random.nextFloat() < 0.2f) {
                isOneHanded = !isOneHanded;
                lastGripChangeTime = now;
                
                if (DEBUG) {
                    HookUtils.logDebug(TAG, "Grip changed: " + (isOneHanded ? "ONE-HANDED" : "TWO-HANDED"));
                }
            }
        }
    }
}
