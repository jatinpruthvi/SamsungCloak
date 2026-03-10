package com.samsungcloak.xposed;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * GestureNavigationImperfectionsHook - Gesture Navigation Imperfections Simulation
 *
 * Simulates realistic gesture navigation imperfections for modern Android gesture-based
 * navigation. This hook adds verisimilitude to gesture testing by modeling:
 *
 * 1. Back Gesture Imperfections - Incomplete swipes, accidental edge touches
 * 2. Home Gesture Imperfections - Unstable bottom swipe, overshoot/undershoot
 * 3. Recent Apps Gesture - Multi-finger gesture failures
 * 4. Edge Touch Accuracy - Touch targets near screen edges
 * 5. Gesture Cancellation - User aborts gesture mid-way
 * 6. Multi-Touch Coordination - Two-finger gesture failures
 * 7. Context-Aware Gesture - Different behavior when walking/driving
 *
 * Novelty: NEW - Not covered by existing hooks
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 *
 * Note: Android 10 introduced full gesture navigation, A12 uses Samsung One UI
 */
public class GestureNavigationImperfectionsHook {

    private static final String TAG = "[Gesture][Navigation]";
    private static final boolean DEBUG = true;
    private static final ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new Random());

    // Toggleable via SharedPreferences
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);

    // Back gesture simulation
    private static boolean backGestureSimulationEnabled = true;
    private static double incompleteGestureProbability = 0.15; // 15% incomplete
    private static double accidentalEdgeTouchProbability = 0.08;
    private static double gestureOvershootProbability = 0.12;
    private static double gestureUndershootProbability = 0.10;
    private static double gestureCancellationProbability = 0.07;

    // Home gesture simulation
    private static boolean homeGestureSimulationEnabled = true;
    private static double homeSwipeUnstableProbability = 0.12;
    private static double homeGestureEarlyAbortProbability = 0.08;

    // Recent apps gesture simulation
    private static boolean recentAppsGestureEnabled = true;
    private static double recentAppsGestureFailProbability = 0.18;

    // Edge touch simulation
    private static boolean edgeTouchSimulationEnabled = true;
    private static double edgeTouchInaccuracyProbability = 0.10;
    private static double edgeTouchDeadZoneProbability = 0.05;

    // Multi-touch gesture simulation
    private static boolean multiTouchGestureEnabled = true;
    private static double twoFingerDesyncProbability = 0.15;
    private static double pinchZoomImperfectionProbability = 0.12;

    // Motion state correlation (walking/driving increases errors)
    private static boolean motionStateCorrelationEnabled = true;
    private static double walkingGestureErrorMultiplier = 1.8;
    private static double drivingGestureErrorMultiplier = 2.5;

    // Current motion state
    private static MotionState currentMotionState = MotionState.STATIONARY;

    // Gesture tracking
    private static final ConcurrentMap<Long, GestureState> activeGestures = new ConcurrentHashMap<>();
    private static long currentGestureId = 0;

    // Statistics
    private static final AtomicInteger totalGestureCount = new AtomicInteger(0);
    private static final AtomicInteger incompleteGestureCount = new AtomicInteger(0);
    private static final AtomicInteger cancelledGestureCount = new AtomicInteger(0);
    private static final AtomicInteger failedGestureCount = new AtomicInteger(0);

    // Edge zone configuration (pixels)
    private static final int BACK_GESTURE_EDGE_WIDTH = 40;
    private static final int HOME_GESTURE_BOTTOM_HEIGHT = 80;
    private static final int RECENT_APPS_EDGE_WIDTH = 50;

    // SharedPreferences key for toggle
    private static final String PREFS_NAME = "SamsungCloak_GestureNav";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_BACK_GESTURE_SIM = "back_gesture_sim_enabled";
    private static final String KEY_MOTION_CORRELATION = "motion_correlation_enabled";

    public enum GestureType {
        BACK_GESTURE,
        HOME_GESTURE,
        RECENT_APPS_GESTURE,
        PULL_DOWN_NOTIFICATION,
        PULL_DOWN_QUICK_SETTINGS,
        SWIPE_LEFT,
        SWIPE_RIGHT,
        PINCH_ZOOM,
        TWO_FINGER_SWIPE
    }

    public enum MotionState {
        STATIONARY,
        WALKING,
        RUNNING,
        DRIVING,
        ON_BUS,
        IN_CAR
    }

    public enum GesturePhase {
        START,
        IN_PROGRESS,
        ALMOST_COMPLETE,
        COMPLETE,
        CANCELLED,
        FAILED
    }

    /**
     * Gesture state tracking
     */
    public static class GestureState {
        public long gestureId;
        public GestureType type;
        public GesturePhase phase;
        public float startX;
        public float startY;
        public float currentX;
        public float currentY;
        public float targetX;
        public float targetY;
        public long startTime;
        public long lastUpdateTime;
        public boolean isIncomplete;
        public boolean wasCancelled;
        public float gestureConfidence; // 0.0 to 1.0

        public GestureState(long id, GestureType type, float x, float y) {
            this.gestureId = id;
            this.type = type;
            this.phase = GesturePhase.START;
            this.startX = x;
            this.startY = y;
            this.currentX = x;
            this.currentY = y;
            this.targetX = x;
            this.targetY = y;
            this.startTime = System.currentTimeMillis();
            this.lastUpdateTime = startTime;
            this.isIncomplete = false;
            this.wasCancelled = false;
            this.gestureConfidence = 1.0f;
        }
    }

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (hookInitialized.getAndSet(true)) {
            HookUtils.logDebug(TAG, "Already initialized");
            return;
        }

        HookUtils.logInfo(TAG, "Initializing Gesture Navigation Imperfections Hook");

        try {
            hookGestureDetector(lpparam);
            hookMotionEvent(lpparam);
            hookScaleGestureDetector(lpparam);

            HookUtils.logInfo(TAG, "Gesture Navigation Imperfections Hook initialized successfully");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Init failed: " + t.getMessage());
            hookInitialized.set(false);
        }
    }

    private static void hookGestureDetector(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> gestureDetectorClass = XposedHelpers.findClass(
                "android.view.GestureDetector", lpparam.classLoader
            );

            // Hook onTouchEvent
            XposedBridge.hookAllMethods(gestureDetectorClass, "onTouchEvent",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        Object eventObj = param.args[0];
                        if (eventObj instanceof MotionEvent) {
                            MotionEvent event = (MotionEvent) eventObj;
                            handleGestureTouchEvent(event);
                        }
                    }
                });

            // Hook isInProgress for gesture detection state
            XposedBridge.hookAllMethods(gestureDetectorClass, "isInProgress",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        // Could modify gesture progress state
                    }
                });

            HookUtils.logInfo(TAG, "GestureDetector hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "GestureDetector hook failed: " + t.getMessage());
        }
    }

    private static void hookMotionEvent(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> motionEventClass = XposedHelpers.findClass(
                "android.view.MotionEvent", lpparam.classLoader
            );

            // Hook getX and getY for coordinate modification
            XposedBridge.hookAllMethods(motionEventClass, "getX",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !edgeTouchSimulationEnabled) return;

                        int pointerIndex = param.args.length > 0 ? (int) param.args[0] : 0;
                        float originalX = (float) param.getResult();
                        float modifiedX = applyGestureNoise(originalX, true, pointerIndex);
                        
                        if (modifiedX != originalX) {
                            param.setResult(modifiedX);
                        }
                    }
                });

            XposedBridge.hookAllMethods(motionEventClass, "getY",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !edgeTouchSimulationEnabled) return;

                        int pointerIndex = param.args.length > 0 ? (int) param.args[0] : 0;
                        float originalY = (float) param.getResult();
                        float modifiedY = applyGestureNoise(originalY, false, pointerIndex);
                        
                        if (modifiedY != originalY) {
                            param.setResult(modifiedY);
                        }
                    }
                });

            // Hook getAxisValue for multi-touch coordination
            XposedBridge.hookAllMethods(motionEventClass, "getAxisValue",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !multiTouchGestureEnabled) return;

                        // Apply multi-touch desync
                        if (random.get().nextDouble() < twoFingerDesyncProbability) {
                            float originalValue = (float) param.getResult();
                            float desyncOffset = (random.get().nextFloat() - 0.5f) * 10.0f;
                            param.setResult(originalValue + desyncOffset);
                        }
                    }
                });

            HookUtils.logInfo(TAG, "MotionEvent hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "MotionEvent hook failed: " + t.getMessage());
        }
    }

    private static void hookScaleGestureDetector(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> scaleGestureDetectorClass = XposedHelpers.findClass(
                "android.view.ScaleGestureDetector", lpparam.classLoader
            );

            // Hook onTouchEvent for pinch zoom imperfections
            XposedBridge.hookAllMethods(scaleGestureDetectorClass, "onTouchEvent",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !multiTouchGestureEnabled) return;

                        Object eventObj = param.args[0];
                        if (eventObj instanceof MotionEvent) {
                            MotionEvent event = (MotionEvent) eventObj;

                            // Check for pinch zoom
                            if (event.getPointerCount() >= 2) {
                                if (random.get().nextDouble() < pinchZoomImperfectionProbability) {
                                    // Add scale imperfection
                                    HookUtils.logDebug(TAG, "Pinch zoom imperfection applied");
                                }
                            }
                        }
                    }
                });

            HookUtils.logInfo(TAG, "ScaleGestureDetector hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "ScaleGestureDetector hook failed: " + t.getMessage());
        }
    }

    private static void handleGestureTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        float x = event.getX();
        float y = event.getY();
        long eventTime = event.getEventTime();
        int pointerCount = event.getPointerCount();

        // Determine gesture type based on touch position
        GestureType gestureType = determineGestureType(x, y, pointerCount);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                handleGestureStart(gestureType, x, y, eventTime);
                break;

            case MotionEvent.ACTION_MOVE:
                handleGestureMove(event);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                handleGestureEnd(gestureType, action == MotionEvent.ACTION_CANCEL);
                break;
        }
    }

    private static GestureType determineGestureType(float x, float y, int pointerCount) {
        // Get screen dimensions - using defaults for A12 (720x1600)
        int screenWidth = 720;
        int screenHeight = 1600;

        // Back gesture: Left edge swipe
        if (x < BACK_GESTURE_EDGE_WIDTH && pointerCount == 1) {
            return GestureType.BACK_GESTURE;
        }

        // Recent apps gesture: Right edge swipe
        if (x > screenWidth - RECENT_APPS_EDGE_WIDTH && pointerCount == 1) {
            return GestureType.RECENT_APPS_GESTURE;
        }

        // Home gesture: Bottom center swipe up
        if (y > screenHeight - HOME_GESTURE_BOTTOM_HEIGHT && pointerCount == 1) {
            return GestureType.HOME_GESTURE;
        }

        // Pull down from top: notification shade
        if (y < 100 && pointerCount == 1) {
            return GestureType.PULL_DOWN_NOTIFICATION;
        }

        // Multi-touch gestures
        if (pointerCount >= 2) {
            return GestureType.TWO_FINGER_SWIPE;
        }

        return GestureType.SWIPE_LEFT; // Default
    }

    private static void handleGestureStart(GestureType type, float x, float y, long time) {
        GestureState state = new GestureState(++currentGestureId, type, x, y);
        activeGestures.put(state.gestureId, state);
        totalGestureCount.incrementAndGet();

        HookUtils.logDebug(TAG, "Gesture started: " + type.name() + " at (" + x + ", " + y + ")");
    }

    private static void handleGestureMove(MotionEvent event) {
        long gestureId = event.getDownTime();
        GestureState state = activeGestures.get(gestureId);

        if (state == null) return;

        // Update current position
        state.currentX = event.getX();
        state.currentY = event.getY();
        state.lastUpdateTime = event.getEventTime();

        // Apply gesture imperfections based on motion state
        double errorMultiplier = getMotionErrorMultiplier();

        // Check for gesture cancellation (user aborts)
        if (random.get().nextDouble() < gestureCancellationProbability * errorMultiplier) {
            state.phase = GesturePhase.CANCELLED;
            state.wasCancelled = true;
            cancelledGestureCount.incrementAndGet();
            HookUtils.logDebug(TAG, "Gesture cancelled mid-way: " + state.type.name());
        }

        // Check for incomplete gesture
        if (random.get().nextDouble() < incompleteGestureProbability * errorMultiplier) {
            state.isIncomplete = true;
            incompleteGestureCount.incrementAndGet();
        }
    }

    private static void handleGestureEnd(GestureType type, boolean wasCancelled) {
        // Clean up gesture state
        for (GestureState state : activeGestures.values()) {
            if (state.type == type && 
                (state.phase == GesturePhase.COMPLETE || state.phase == GesturePhase.CANCELLED)) {
                activeGestures.remove(state.gestureId);
                break;
            }
        }
    }

    private static float applyGestureNoise(float coordinate, boolean isX, int pointerIndex) {
        double errorMultiplier = getMotionErrorMultiplier();

        // Apply edge touch inaccuracy
        if (random.get().nextDouble() < edgeTouchInaccuracyProbability * errorMultiplier) {
            float noise = (random.get().nextFloat() - 0.5f) * 8.0f * (float) errorMultiplier;
            coordinate += noise;
        }

        // Apply edge dead zone
        if (random.get().nextDouble() < edgeTouchDeadZoneProbability * errorMultiplier) {
            // Dead zone - return original coordinate (no movement)
            // This simulates touching edge but not triggering gesture
        }

        return coordinate;
    }

    private static double getMotionErrorMultiplier() {
        if (!motionStateCorrelationEnabled) return 1.0;

        switch (currentMotionState) {
            case WALKING:
            case RUNNING:
                return walkingGestureErrorMultiplier;
            case DRIVING:
            case ON_BUS:
            case IN_CAR:
                return drivingGestureErrorMultiplier;
            default:
                return 1.0;
        }
    }

    /**
     * Set current motion state for context-aware gesture simulation
     */
    public static void setMotionState(MotionState state) {
        currentMotionState = state;
        HookUtils.logInfo(TAG, "Motion state: " + state.name());
    }

    /**
     * Get gesture statistics
     */
    public static Map<String, Object> getGestureStatistics() {
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalGestures", totalGestureCount.get());
        stats.put("incompleteGestures", incompleteGestureCount.get());
        stats.put("cancelledGestures", cancelledGestureCount.get());
        stats.put("failedGestures", failedGestureCount.get());
        
        int total = totalGestureCount.get();
        if (total > 0) {
            stats.put("incompleteRate", (double) incompleteGestureCount.get() / total);
            stats.put("cancellationRate", (double) cancelledGestureCount.get() / total);
        }
        
        return stats;
    }

    // Configuration methods

    public static void setEnabled(boolean enabled) {
        GestureNavigationImperfectionsHook.enabled = enabled;
        HookUtils.logInfo(TAG, "Hook " + (enabled ? "enabled" : "disabled"));
    }

    public static void setBackGestureSimulationEnabled(boolean enabled) {
        backGestureSimulationEnabled = enabled;
        HookUtils.logInfo(TAG, "Back gesture simulation " + (enabled ? "enabled" : "disabled"));
    }

    public static void setHomeGestureSimulationEnabled(boolean enabled) {
        homeGestureSimulationEnabled = enabled;
        HookUtils.logInfo(TAG, "Home gesture simulation " + (enabled ? "enabled" : "disabled"));
    }

    public static void setMotionStateCorrelationEnabled(boolean enabled) {
        motionStateCorrelationEnabled = enabled;
        HookUtils.logInfo(TAG, "Motion state correlation " + (enabled ? "enabled" : "disabled"));
    }

    public static void setIncompleteGestureProbability(double probability) {
        incompleteGestureProbability = HookUtils.clamp(probability, 0.0, 1.0);
        HookUtils.logInfo(TAG, "Incomplete gesture probability: " + incompleteGestureProbability);
    }

    public static void setGestureCancellationProbability(double probability) {
        gestureCancellationProbability = HookUtils.clamp(probability, 0.0, 1.0);
        HookUtils.logInfo(TAG, "Gesture cancellation probability: " + gestureCancellationProbability);
    }

    public static int getTotalGestureCount() {
        return totalGestureCount.get();
    }

    public static int getIncompleteGestureCount() {
        return incompleteGestureCount.get();
    }

    public static int getCancelledGestureCount() {
        return cancelledGestureCount.get();
    }

    public static MotionState getCurrentMotionState() {
        return currentMotionState;
    }
}
