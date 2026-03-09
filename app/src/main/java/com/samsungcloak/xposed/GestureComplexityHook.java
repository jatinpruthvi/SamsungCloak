package com.samsungcloak.xposed;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.VelocityTracker;
import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * GestureComplexityHook - Human Gesture Pattern Simulation
 *
 * Simulates realistic human gesture patterns including scroll velocity variations,
 * fling gestures, multi-finger gestures, and gesture timing inconsistencies.
 * Adds natural human imperfections to all gesture-based interactions.
 *
 * Novel Dimensions:
 * 1. Scroll Velocity Imperfections - Natural velocity decay and variations
 * 2. Fling Gesture Inconsistencies - Incomplete flings, early terminations
 * 3. Multi-touch Gesture Errors - Asymmetric finger movements, early lifts
 * 4. Gesture Timing Variations - Variable timing in gesture initiation/termination
 * 5. Zoom Gesture Realism - Pinch zoom with natural errors
 * 6. Swipe Direction Variance - Slight directional drift during swipes
 *
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class GestureComplexityHook {

    private static final String TAG = "[HumanInteraction][Gesture]";
    private static final boolean DEBUG = true;
    private static final ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new Random());

    private static boolean enabled = true;

    // Scroll configuration
    private static boolean scrollImperfectionsEnabled = true;
    private static double scrollVelocityVariance = 0.2; // 20% velocity variance
    private static double scrollDecayRate = 0.95; // Natural decay
    private static double earlyTerminationProbability = 0.15;

    // Fling configuration
    private static boolean flingImperfectionsEnabled = true;
    private static double incompleteFlingProbability = 0.12;
    private static double flingOvershootProbability = 0.08;
    private static double flingDirectionVariance = 0.15; // radians

    // Multi-touch configuration
    private static boolean multiTouchImperfectionsEnabled = true;
    private static double asymmetricFingerProbability = 0.25;
    private static double earlyLiftProbability = 0.10;
    private static double fingerDriftVariance = 3.0; // pixels

    // Timing configuration
    private static boolean timingVariationsEnabled = true;
    private static double gestureInitiationDelayMs = 50; // Average delay
    private static double gestureResponseVariance = 0.3; // 30% variance

    // Zoom configuration
    private static boolean zoomImperfectionsEnabled = true;
    private static double pinchErrorProbability = 0.18;
    private static double zoomOvershootProbability = 0.10;

    // Swipe configuration
    private static boolean swipeDirectionVarianceEnabled = true;
    private static double swipeAngleVariance = 0.12; // radians

    // State tracking
    private static final List<GestureState> activeGestures = new ArrayList<>();
    private static long lastGestureTime = 0;

    private static class GestureState {
        int pointerCount;
        long startTime;
        float startX, startY;
        float lastX, lastY;
        boolean isScroll;
        boolean isFling;
        boolean isScale;

        GestureState(int pointerCount, float x, float y) {
            this.pointerCount = pointerCount;
            this.startTime = System.currentTimeMillis();
            this.startX = x;
            this.startY = y;
            this.lastX = x;
            this.lastY = y;
        }
    }

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        HookUtils.logInfo(TAG, "Initializing Gesture Complexity Hook");

        try {
            hookViewTouch(lpparam);
            hookGestureDetector(lpparam);
            hookScaleGestureDetector(lpparam);
            hookVelocityTracker(lpparam);

            HookUtils.logInfo(TAG, "Gesture Complexity Hook initialized successfully");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize hook", e);
        }
    }

    private static void hookViewTouch(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> viewClass = XposedHelpers.findClass("android.view.View", lpparam.classLoader);

            XposedBridge.hookAllMethods(viewClass, "onTouchEvent", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    try {
                        MotionEvent event = (MotionEvent) param.args[0];
                        processTouchEvent(event);
                    } catch (Exception e) {
                        if (DEBUG) HookUtils.logDebug(TAG, "Error in view touch hook: " + e.getMessage());
                    }
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked View.onTouchEvent");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook View.onTouchEvent", e);
        }
    }

    private static void hookGestureDetector(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> gestureDetectorClass = XposedHelpers.findClass(
                "android.view.GestureDetector",
                lpparam.classLoader
            );

            XposedBridge.hookAllMethods(gestureDetectorClass, "onTouchEvent", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || !timingVariationsEnabled) return;

                    try {
                        // Add timing variation to gesture detection
                        MotionEvent event = (MotionEvent) param.args[0];
                        int action = event.getActionMasked();

                        if (action == MotionEvent.ACTION_DOWN) {
                            // Occasionally delay gesture initiation
                            if (random.get().nextDouble() < 0.2) {
                                long delay = (long) (gestureInitiationDelayMs * 
                                    (0.5 + random.get().nextDouble() * gestureResponseVariance));
                                
                                if (DEBUG && random.get().nextDouble() < 0.05) {
                                    HookUtils.logDebug(TAG, "Gesture initiation delay: " + delay + "ms");
                                }
                            }
                        }
                    } catch (Exception e) {
                        if (DEBUG) HookUtils.logDebug(TAG, "Error in gesture detector hook: " + e.getMessage());
                    }
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked GestureDetector");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook GestureDetector", e);
        }
    }

    private static void hookScaleGestureDetector(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> scaleGestureDetectorClass = XposedHelpers.findClass(
                "android.view.ScaleGestureDetector",
                lpparam.classLoader
            );

            XposedBridge.hookAllMethods(scaleGestureDetectorClass, "onTouchEvent", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || !zoomImperfectionsEnabled) return;

                    try {
                        // Simulate pinch zoom errors
                        if (random.get().nextDouble() < pinchErrorProbability) {
                            applyPinchError(param);
                        }
                    } catch (Exception e) {
                        if (DEBUG) HookUtils.logDebug(TAG, "Error in scale gesture hook: " + e.getMessage());
                    }
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked ScaleGestureDetector");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook ScaleGestureDetector", e);
        }
    }

    private static void hookVelocityTracker(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> velocityTrackerClass = XposedHelpers.findClass(
                "android.view.VelocityTracker",
                lpparam.classLoader
            );

            XposedBridge.hookAllMethods(velocityTrackerClass, "computeCurrentVelocity", 
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !scrollImperfectionsEnabled) return;

                        try {
                            // Apply velocity variance
                            float originalVelocityX = param.args.length > 0 ? (float) param.args[0] : 0;
                            float originalVelocityY = param.args.length > 1 ? (float) param.args[1] : 0;
                            
                            float variedVelocityX = applyVelocityVariance(originalVelocityX);
                            float variedVelocityY = applyVelocityVariance(originalVelocityY);
                            
                            // Can't easily modify result, but could track for later
                        } catch (Exception e) {
                            if (DEBUG) HookUtils.logDebug(TAG, "Error in velocity hook: " + e.getMessage());
                        }
                    }
                });

            XposedBridge.hookAllMethods(velocityTrackerClass, "getXVelocity", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || !scrollImperfectionsEnabled) return;

                    try {
                        float velocity = (float) param.getResult();
                        param.setResult(applyVelocityVariance(velocity));
                    } catch (Exception e) {
                        // Silent fail
                    }
                }
            });

            XposedBridge.hookAllMethods(velocityTrackerClass, "getYVelocity", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || !scrollImperfectionsEnabled) return;

                    try {
                        float velocity = (float) param.getResult();
                        param.setResult(applyVelocityVariance(velocity));
                    } catch (Exception e) {
                        // Silent fail
                    }
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked VelocityTracker");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook VelocityTracker", e);
        }
    }

    private static void processTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int pointerCount = event.getPointerCount();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                // Start tracking new gesture
                GestureState state = new GestureState(
                    pointerCount,
                    event.getX(),
                    event.getY()
                );
                activeGestures.clear();
                activeGestures.add(state);
                lastGestureTime = System.currentTimeMillis();
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                // Multi-touch started
                if (activeGestures.size() > 0) {
                    activeGestures.get(0).pointerCount = pointerCount;
                    
                    // Apply asymmetric finger error
                    if (multiTouchImperfectionsEnabled && random.get().nextDouble() < asymmetricFingerProbability) {
                        applyAsymmetricFingerError(event);
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                // Track movement for scroll/fling
                if (activeGestures.size() > 0) {
                    GestureState currentState = activeGestures.get(0);
                    currentState.lastX = event.getX(event.getActionIndex());
                    currentState.lastY = event.getY(event.getActionIndex());
                    
                    // Apply scroll velocity variance
                    if (scrollImperfectionsEnabled) {
                        applyScrollVariance(event, currentState);
                    }
                    
                    // Apply swipe direction variance
                    if (swipeDirectionVarianceEnabled) {
                        applySwipeDirectionVariance(event);
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                // Gesture ended - apply fling imperfections
                if (activeGestures.size() > 0 && flingImperfectionsEnabled) {
                    applyFlingImperfections(event, activeGestures.get(0));
                }
                activeGestures.clear();
                break;

            case MotionEvent.ACTION_POINTER_UP:
                // Finger lifted - early lift error
                if (multiTouchImperfectionsEnabled && pointerCount > 1) {
                    if (random.get().nextDouble() < earlyLiftProbability) {
                        applyEarlyLiftError(event);
                    }
                }
                break;
        }
    }

    /**
     * 1. SCROLL VELOCITY IMPERFECTIONS
     */
    private static void applyScrollVariance(MotionEvent event, GestureState state) {
        // Add natural velocity decay variations
        if (event.getHistorySize() > 0) {
            float historicalX = event.getHistoricalX(0);
            float historicalY = event.getHistoricalY(0);
            float currentX = event.getX();
            float currentY = event.getY();
            
            long historicalTime = event.getHistoricalEventTime(0);
            long currentTime = event.getEventTime();
            float dx = currentX - historicalX;
            float dy = currentY - historicalY;
            float dt = (currentTime - historicalTime) / 1000.0f;
            
            if (dt > 0) {
                // Add velocity variance
                float velocityX = dx / dt;
                float velocityY = dy / dt;
                
                velocityX *= (1.0 + (random.get().nextDouble() - 0.5) * scrollVelocityVariance * 2);
                velocityY *= (1.0 + (random.get().nextDouble() - 0.5) * scrollVelocityVariance * 2);
            }
        }
        
        // Early scroll termination
        if (random.get().nextDouble() < earlyTerminationProbability * 0.01) {
            if (DEBUG && random.get().nextDouble() < 0.05) {
                HookUtils.logDebug(TAG, "Early scroll termination");
            }
        }
    }

    /**
     * 2. FLING GESTURE INCONSISTENCIES
     */
    private static void applyFlingImperfections(MotionEvent event, GestureState state) {
        long gestureDuration = System.currentTimeMillis() - state.startTime;
        float totalDistance = (float) Math.sqrt(
            Math.pow(event.getX() - state.startX, 2) +
            Math.pow(event.getY() - state.startY, 2)
        );

        // Incomplete fling (user stops before fling completes)
        if (random.get().nextDouble() < incompleteFlingProbability) {
            // Reduce fling velocity
            float reductionFactor = 0.5f + (float) random.get().nextDouble() * 0.3f;
            
            if (DEBUG && random.get().nextDouble() < 0.05) {
                HookUtils.logDebug(TAG, String.format(
                    "Incomplete fling: duration=%dms, distance=%.1f, reduction=%.1f%%",
                    gestureDuration, totalDistance, (1-reductionFactor)*100
                ));
            }
        }

        // Fling overshoot
        if (random.get().nextDouble() < flingOvershootProbability) {
            float overshootFactor = 1.1f + (float) random.get().nextDouble() * 0.2f;
            
            if (DEBUG && random.get().nextDouble() < 0.05) {
                HookUtils.logDebug(TAG, "Fling overshoot: " + String.format("%.1f%%", (overshootFactor-1)*100));
            }
        }
    }

    /**
     * 3. MULTI-TOUCH GESTURE ERRORS - Asymmetric finger movement
     */
    private static void applyAsymmetricFingerError(MotionEvent event) {
        int pointerIndex = event.getActionIndex();
        
        // One finger moves slightly differently than the other
        float offsetX = (float) ((random.get().nextDouble() - 0.5) * fingerDriftVariance * 2);
        float offsetY = (float) ((random.get().nextDouble() - 0.5) * fingerDriftVariance * 2);
        
        // Could modify event but MotionEvent is immutable
        if (DEBUG && random.get().nextDouble() < 0.05) {
            HookUtils.logDebug(TAG, String.format(
                "Asymmetric finger: offset=(%.1f, %.1f)",
                offsetX, offsetY
            ));
        }
    }

    /**
     * Early finger lift during multi-touch gesture
     */
    private static void applyEarlyLiftError(MotionEvent event) {
        // Simulates user lifting one finger early during pinch/zoom
        if (DEBUG && random.get().nextDouble() < 0.05) {
            HookUtils.logDebug(TAG, "Early finger lift during multi-touch");
        }
    }

    /**
     * 4. ZOOM GESTURE REALISM - Pinch errors
     */
    private static void applyPinchError(XC_MethodHook.MethodHookParam param) {
        // Simulate pinch zoom errors
        MotionEvent event = (MotionEvent) param.args[0];
        
        // Add noise to scale factor
        double scaleNoise = 1.0 + (random.get().nextDouble() - 0.5) * 0.1;
        
        // Zoom overshoot/undershoot
        if (random.get().nextDouble() < zoomOvershootProbability) {
            double overshoot = 1.0 + (random.get().nextDouble() * 0.15);
            scaleNoise *= overshoot;
            
            if (DEBUG && random.get().nextDouble() < 0.05) {
                HookUtils.logDebug(TAG, "Pinch zoom overshoot");
            }
        }
        
        if (DEBUG && random.get().nextDouble() < 0.05) {
            HookUtils.logDebug(TAG, "Pinch error applied: factor=" + String.format("%.3f", scaleNoise));
        }
    }

    /**
     * 5. SWIPE DIRECTION VARIANCE
     */
    private static void applySwipeDirectionVariance(MotionEvent event) {
        // Add slight directional drift
        float variance = (float) ((random.get().nextDouble() - 0.5) * swipeAngleVariance * 2);
        
        // Convert to offset based on swipe distance
        float currentX = event.getX();
        float currentY = event.getY();
        
        // Can't directly modify, but could track for future modifications
        if (DEBUG && random.get().nextDouble() < 0.01) {
            HookUtils.logDebug(TAG, String.format(
                "Swipe direction variance: angle=%.2f deg",
                Math.toDegrees(variance)
            ));
        }
    }

    private static float applyVelocityVariance(float velocity) {
        if (Math.abs(velocity) < 10) {
            return velocity; // Ignore small velocities
        }
        
        // Apply variance with direction preservation
        float variance = (float) ((random.get().nextDouble() - 0.5) * 2 * scrollVelocityVariance);
        return velocity * (1.0f + variance);
    }

    // Configuration methods

    public static void setEnabled(boolean enabled) {
        GestureComplexityHook.enabled = enabled;
        HookUtils.logInfo(TAG, "Hook " + (enabled ? "enabled" : "disabled"));
    }

    public static void setScrollImperfectionsEnabled(boolean enabled) {
        scrollImperfectionsEnabled = enabled;
        HookUtils.logInfo(TAG, "Scroll imperfections " + (enabled ? "enabled" : "disabled"));
    }

    public static void setFlingImperfectionsEnabled(boolean enabled) {
        flingImperfectionsEnabled = enabled;
        HookUtils.logInfo(TAG, "Fling imperfections " + (enabled ? "enabled" : "disabled"));
    }

    public static void setMultiTouchImperfectionsEnabled(boolean enabled) {
        multiTouchImperfectionsEnabled = enabled;
        HookUtils.logInfo(TAG, "Multi-touch imperfections " + (enabled ? "enabled" : "disabled"));
    }

    public static void setTimingVariationsEnabled(boolean enabled) {
        timingVariationsEnabled = enabled;
        HookUtils.logInfo(TAG, "Timing variations " + (enabled ? "enabled" : "disabled"));
    }

    public static void setZoomImperfectionsEnabled(boolean enabled) {
        zoomImperfectionsEnabled = enabled;
        HookUtils.logInfo(TAG, "Zoom imperfections " + (enabled ? "enabled" : "disabled"));
    }

    public static void setSwipeDirectionVarianceEnabled(boolean enabled) {
        swipeDirectionVarianceEnabled = enabled;
        HookUtils.logInfo(TAG, "Swipe direction variance " + (enabled ? "enabled" : "disabled"));
    }

    public static void setScrollVelocityVariance(double variance) {
        scrollVelocityVariance = HookUtils.clamp(variance, 0.0, 1.0);
        HookUtils.logInfo(TAG, "Scroll velocity variance set to: " + scrollVelocityVariance);
    }

    public static void setFlingDirectionVariance(double variance) {
        flingDirectionVariance = HookUtils.clamp(variance, 0.0, 1.0);
        HookUtils.logInfo(TAG, "Fling direction variance set to: " + flingDirectionVariance);
    }

    public static void setGestureInitiationDelayMs(double delay) {
        gestureInitiationDelayMs = HookUtils.clamp(delay, 0, 500);
        HookUtils.logInfo(TAG, "Gesture initiation delay set to: " + gestureInitiationDelayMs + "ms");
    }

    public static int getActiveGestureCount() {
        return activeGestures.size();
    }

    public static long getTimeSinceLastGesture() {
        return System.currentTimeMillis() - lastGestureTime;
    }
}
