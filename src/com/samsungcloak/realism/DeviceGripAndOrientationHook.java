package com.samsungcloak.realism;

import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;

/**
 * DeviceGripAndOrientationHook - One-handed/two-handed use, grip changes, tilt during walking
 * 
 * Targets: Samsung Galaxy A12 (SM-A125U) Android 10/11
 * 
 * This hook addresses the realism dimension of device handling and grip dynamics:
 * - One-handed vs two-handed use patterns
 * - Grip transitions during different activities
 * - Phone tilt changes while walking (natural sway)
 * - Accidental palm touches on curved edges
 * - Thumb reachability zones
 * - Hand size variations (affects touch coverage)
 */
public class DeviceGripAndOrientationHook {
    private static final String TAG = "DeviceGripAndOrientation";
    private static final String PACKAGE_NAME = "com.samsungcloak.realism";
    
    // Configuration keys
    private static final String KEY_ENABLED = "grip_enabled";
    private static final String KEY_ONE_HANDED_SIM = "grip_one_handed";
    private static final String KEY_TILT_WHILE_WALKING = "grip_tilt_walking";
    private static final String KEY_PALM_REJECTION = "grip_palm_rejection";
    private static final String KEY_GRIP_TRANSITIONS = "grip_transitions";
    private static final String KEY_HAND_SIZE = "grip_hand_size";
    
    // Grip modes
    public enum GripMode {
        ONE_HANDED_LEFT,      // Single hand, left side
        ONE_HANDED_RIGHT,     // Single hand, right side
        TWO_HANDED_PORTRAIT,  // Both hands, portrait
        TWO_HANDED_LANDSCAPE, // Both hands, landscape
        TRANSITIONING         // Between modes
    }
    
    // Hand sizes
    public enum HandSize {
        SMALL,    // < 7cm palm width
        MEDIUM,   // 7-8.5cm
        LARGE     // > 8.5cm
    }
    
    // Constants
    private static final float ONE_HANDED_REACHABILITY_ZONE = 0.75f;  // 75% of screen height
    
    // Tilt parameters while walking (degrees)
    private static final float WALKING_TILT_MEAN_X = 3.5f;
    private static final float WALKING_TILT_STD_DEV_X = 2.0f;
    private static final float WALKING_TILT_MEAN_Y = 2.0f;
    private static final float WALKING_TILT_STD_DEV_Y = 1.5f;
    
    // Walking sway frequency
    private static final float WALKING_SWAY_FREQUENCY_HZ = 1.8f;  // Step frequency
    
    // State
    private static SharedPreferences sPrefs;
    private static boolean sEnabled = true;
    private static boolean sOneHandedSimulation = true;
    private static boolean sTiltWhileWalking = true;
    private static boolean sPalmRejection = true;
    private static boolean sGripTransitions = true;
    private static HandSize sHandSize = HandSize.MEDIUM;
    
    // Runtime state
    private static final Random sRandom = new Random();
    private static GripMode sCurrentGripMode = GripMode.TWO_HANDED_PORTRAIT;
    private static GripMode sTargetGripMode = GripMode.TWO_HANDED_PORTRAIT;
    private static long sGripModeChangeTime = 0;
    private static long sGripModeTransitionDuration = 500;  // ms
    
    // Sensor state
    private static float sCurrentTiltX = 0f;
    private static float sCurrentTiltY = 0f;
    private static float sWalkingPhase = 0f;
    private static boolean sIsWalking = false;
    private static long sLastStepTime = 0;
    
    // Screen dimensions
    private static int sScreenWidth = 720;   // SM-A125U default
    private static int sScreenHeight = 1600; // SM-A125U default
    
    // Touch interaction state
    private static long sLastTouchTime = 0;
    private static float sLastTouchX = 0;
    private static float sLastTouchY = 0;
    private static boolean sIsThumbReachZone = false;
    
    // Grip change probabilities per activity
    private static final float TYPING_GRIP_CHANGE_PROB = 0.15f;
    private static final float SCROLLING_GRIP_CHANGE_PROB = 0.08f;
    private static final float WATCHING_VIDEO_GRIP_CHANGE_PROB = 0.25f;
    
    /**
     * Initialize the hook
     */
    public static void init(SharedPreferences prefs) {
        sPrefs = prefs;
        reloadSettings();
    }
    
    /**
     * Reload settings
     */
    public static void reloadSettings() {
        if (sPrefs == null) return;
        
        sEnabled = sPrefs.getBoolean(KEY_ENABLED, true);
        sOneHandedSimulation = sPrefs.getBoolean(KEY_ONE_HANDED_SIM, true);
        sTiltWhileWalking = sPrefs.getBoolean(KEY_TILT_WHILE_WALKING, true);
        sPalmRejection = sPrefs.getBoolean(KEY_PALM_REJECTION, true);
        sGripTransitions = sPrefs.getBoolean(KEY_GRIP_TRANSITIONS, true);
        
        String handSizeStr = sPrefs.getString(KEY_HAND_SIZE, "MEDIUM");
        try {
            sHandSize = HandSize.valueOf(handSizeStr);
        } catch (Exception e) {
            sHandSize = HandSize.MEDIUM;
        }
    }
    
    /**
     * Hook WindowManager for orientation changes
     */
    public static void hookWindowManager(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!sEnabled) return;
        
        try {
            Class<?> windowManagerClass = XposedHelpers.findClass(
                "android.view.WindowManager",
                lpparam.classLoader);
            
            // Hook getDefaultDisplay()
            XposedBridge.hookAllMethods(windowManagerClass, "getDefaultDisplay",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!sEnabled || param.getResult() == null) return;
                        
                        // Could modify display metrics based on grip
                    }
                });
            
            XposedBridge.log(TAG + ": WindowManager hooks installed");
            
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Error hooking WindowManager: " + e.getMessage());
        }
    }
    
    /**
     * Hook DisplayContent/WindowOrientationController
     */
    public static void hookOrientationController(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!sEnabled) return;
        
        try {
            Class<?> displayContentClass = XposedHelpers.findClass(
                "com.android.server.wm.DisplayContent",
                lpparam.classLoader);
            
            // Hook getOrientation()
            XposedBridge.hookAllMethods(displayContentClass, "getOrientation",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!sEnabled) return;
                        
                        // Could modify orientation request based on grip
                        // e.g., landscape preference when in two-handed landscape
                    }
                });
            
            XposedBridge.log(TAG + ": OrientationController hooks installed");
            
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Error hooking OrientationController: " + e.getMessage());
        }
    }
    
    /**
     * Hook SensorManager for tilt/rotation sensors
     */
    public static void hookSensorManager(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!sEnabled || !sTiltWhileWalking) return;
        
        try {
            Class<?> sensorManagerClass = XposedHelpers.findClass(
                "android.hardware.SensorManager",
                lpparam.classLoader);
            
            // Hook getDefaultSensor() for rotation vector
            XposedBridge.hookAllMethods(sensorManagerClass, "getDefaultSensor",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!sEnabled || !sTiltWhileWalking) return;
                        
                        int sensorType = (Integer) param.args[0];
                        
                        if (sensorType == Sensor.TYPE_ROTATION_VECTOR ||
                            sensorType == Sensor.TYPE_GAME_ROTATION_VECTOR) {
                            
                            // Inject walking tilt
                            SensorEvent event = createWalkingTiltEvent(sensorType);
                            if (event != null) {
                                // Would need to inject into sensor queue
                            }
                        }
                    }
                });
            
            XposedBridge.log(TAG + ": SensorManager hooks installed");
            
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Error hooking SensorManager: " + e.getMessage());
        }
    }
    
    /**
     * Hook MotionEvent for palm touch detection
     */
    public static void hookMotionEvent(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!sEnabled || !sPalmRejection) return;
        
        try {
            // Hook MotionEvent.obtain()
            Class<?> motionEventClass = XposedHelpers.findClass(
                "android.view.MotionEvent",
                lpparam.classLoader);
            
            XposedBridge.hookAllMethods(motionEventClass, "obtain",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!sEnabled || !sPalmRejection) return;
                        
                        MotionEvent event = (MotionEvent) param.getResult();
                        if (event == null) return;
                        
                        // Check for palm touch probability
                        int action = event.getActionMasked();
                        
                        if (action == MotionEvent.ACTION_DOWN) {
                            handleTouchDown(event);
                        }
                    }
                });
            
            // Hook getToolType() for multi-touch tool type detection
            XposedBridge.hookAllMethods(motionEventClass, "getToolType",
                new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) 
                        throws Throwable {
                        
                        int pointerIndex = param.args.length > 0 ? (Integer) param.args[0] : 0;
                        
                        // Check if this might be a palm
                        if (sPalmRejection && isLikelyPalmTouch(pointerIndex)) {
                            return MotionEvent.TOOL_TYPE_ERASER;  // Palm
                        }
                        
                        return MotionEvent.TOOL_TYPE_FINGER;
                    }
                });
            
            XposedBridge.log(TAG + ": MotionEvent hooks installed");
            
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Error hooking MotionEvent: " + e.getMessage());
        }
    }
    
    /**
     * Hook InputDispatcher for touch coordinates
     */
    public static void hookInputDispatcher(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!sEnabled) return;
        
        try {
            Class<?> inputDispatcherClass = XposedHelpers.findClass(
                "com.android.server.input.InputDispatcher",
                lpparam.classLoader);
            
            // Hook dispatchMotion()
            XposedBridge.hookAllMethods(inputDispatcherClass, "dispatchMotion",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!sEnabled) return;
                        
                        // Could modify touch coordinates based on grip
                        // e.g., shift coordinates for one-handed reach
                    }
                });
            
            XposedBridge.log(TAG + ": InputDispatcher hooks installed");
            
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Error hooking InputDispatcher: " + e.getMessage());
        }
    }
    
    /**
     * Hook View for gesture detection
     */
    public static void hookView(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!sEnabled) return;
        
        try {
            Class<?> viewClass = XposedHelpers.findClass(
                "android.view.View",
                lpparam.classLoader);
            
            // Hook onTouchEvent()
            XposedBridge.hookAllMethods(viewClass, "onTouchEvent",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!sEnabled) return;
                        
                        MotionEvent event = (MotionEvent) param.args[0];
                        if (event != null) {
                            processTouchEvent(event);
                        }
                    }
                });
            
            // Hook onHoverEvent() for hover/hovering palm
            XposedBridge.hookAllMethods(viewClass, "onHoverEvent",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!sEnabled || !sPalmRejection) return;
                        
                        // Could detect hovering palm
                    }
                });
            
            XposedBridge.log(TAG + ": View hooks installed");
            
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Error hooking View: " + e.getMessage());
        }
    }
    
    /**
     * Handle touch down event
     */
    private static void handleTouchDown(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        
        // Check if touch is in reach zone based on current grip
        sIsThumbReachZone = isInReachZone(x, y);
        
        // Update last touch state
        sLastTouchTime = System.currentTimeMillis();
        sLastTouchX = x;
        sLastTouchY = y;
        
        // Potentially change grip based on touch location
        if (sGripTransitions && sRandom.nextFloat() < getGripChangeProbability(y)) {
            initiateGripTransition();
        }
    }
    
    /**
     * Process touch event
     */
    private static void processTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        
        if (action == MotionEvent.ACTION_DOWN) {
            // Check for grip transition opportunity
            if (sGripTransitions && shouldChangeGrip()) {
                initiateGripTransition();
            }
        } else if (action == MotionEvent.ACTION_UP) {
            // Reset some state after touch release
        }
    }
    
    /**
     * Check if coordinates are in reachable zone
     */
    private static boolean isInReachZone(float x, float y) {
        switch (sCurrentGripMode) {
            case ONE_HANDED_LEFT:
                // Left thumb can reach left 75% of screen
                return x < (sScreenWidth * 0.75f);
                
            case ONE_HANDED_RIGHT:
                // Right thumb can reach right 75% of screen
                return x > (sScreenWidth * 0.25f);
                
            case TWO_HANDED_PORTRAIT:
            case TWO_HANDED_LANDSCAPE:
                // Both hands can reach full screen
                return true;
                
            default:
                return true;
        }
    }
    
    /**
     * Get grip change probability based on touch location
     */
    private static float getGripChangeProbability(float y) {
        float normalizedY = y / sScreenHeight;
        
        // More likely to change grip when touching near top (harder to reach)
        if (normalizedY < 0.3f) {
            return 0.20f;  // High probability at top
        } else if (normalizedY < 0.5f) {
            return 0.12f;  // Medium at mid-high
        } else if (normalizedY < 0.7f) {
            return 0.05f;  // Low at mid
        } else {
            return 0.02f;  // Very low at bottom (easy reach)
        }
    }
    
    /**
     * Determine if grip should change
     */
    private static boolean shouldChangeGrip() {
        // Time-based check
        long timeSinceLastChange = System.currentTimeMillis() - sGripModeChangeTime;
        if (timeSinceLastChange < 2000) return false;  // Min 2 seconds between changes
        
        // Activity-based probability
        long timeSinceTouch = System.currentTimeMillis() - sLastTouchTime;
        
        if (timeSinceTouch < 500) {
            // Just touched - moderate change chance
            return sRandom.nextFloat() < 0.10f;
        } else if (timeSinceTouch < 2000) {
            // Recent touch - lower chance
            return sRandom.nextFloat() < 0.05f;
        }
        
        // Idle - minimal chance
        return sRandom.nextFloat() < 0.01f;
    }
    
    /**
     * Initiate grip mode transition
     */
    private static void initiateGripTransition() {
        sCurrentGripMode = GripMode.TRANSITIONING;
        sGripModeChangeTime = System.currentTimeMillis();
        
        // Determine new grip mode
        GripMode[] modes = GripMode.values();
        GripMode newMode = modes[sRandom.nextInt(modes.length - 1)];  // Exclude TRANSITIONING
        
        // Apply transition duration (varies by type)
        switch (newMode) {
            case ONE_HANDED_LEFT:
            case ONE_HANDED_RIGHT:
                sGripModeTransitionDuration = 400 + sRandom.nextInt(300);  // 400-700ms
                break;
            case TWO_HANDED_PORTRAIT:
            case TWO_HANDED_LANDSCAPE:
                sGripModeTransitionDuration = 600 + sRandom.nextInt(400);  // 600-1000ms
                break;
        }
        
        sTargetGripMode = newMode;
        
        XposedBridge.log(TAG + ": Initiating grip transition to " + newMode);
    }
    
    /**
     * Update grip mode (call periodically)
     */
    public static void updateGripMode() {
        if (sCurrentGripMode == GripMode.TRANSITIONING) {
            long elapsed = System.currentTimeMillis() - sGripModeChangeTime;
            
            if (elapsed >= sGripModeTransitionDuration) {
                sCurrentGripMode = sTargetGripMode;
                XposedBridge.log(TAG + ": Grip transition complete: " + sCurrentGripMode);
            }
        }
    }
    
    /**
     * Create sensor event for walking tilt
     */
    private static SensorEvent createWalkingTiltEvent(int sensorType) {
        if (!sIsWalking || !sTiltWhileWalking) return null;
        
        // Would create and return a SensorEvent
        // This is a placeholder for actual implementation
        
        return null;
    }
    
    /**
     * Update walking state (called from sensor fusion)
     */
    public static void setWalkingState(boolean isWalking) {
        sIsWalking = isWalking;
        
        if (isWalking) {
            sWalkingPhase = 0f;
            sLastStepTime = System.currentTimeMillis();
        }
    }
    
    /**
     * Update tilt based on walking motion
     */
    public static void updateWalkingTilt(long timestamp) {
        if (!sIsWalking || !sTiltWhileWalking) {
            // Gradually return to neutral
            sCurrentTiltX *= 0.95f;
            sCurrentTiltY *= 0.95f;
            return;
        }
        
        // Calculate time since last step
        long timeSinceStep = timestamp - sLastStepTime;
        double stepInterval = 1000.0 / WALKING_SWAY_FREQUENCY_HZ;  // ms
        
        // Update phase
        sWalkingPhase = (float) ((timeSinceStep % stepInterval) / stepInterval * 2 * Math.PI);
        
        // Apply sinusoidal tilt matching step cycle
        float phaseOffset = (float) Math.PI / 4;  // 45 degree offset between X and Y
        
        // X tilt: forward/backward lean
        float xTilt = WALKING_TILT_MEAN_X + 
                      (float) (sRandom.nextGaussian() * WALKING_TILT_STD_DEV_X) +
                      (float) Math.sin(sWalkingPhase) * 1.5f;
        
        // Y tilt: side-to-side sway
        float yTilt = WALKING_TILT_MEAN_Y +
                      (float) (sRandom.nextGaussian() * WALKING_TILT_STD_DEV_Y) +
                      (float) Math.sin(sWalkingPhase + phaseOffset) * 1.0f;
        
        // Apply smooth transition
        sCurrentTiltX = sCurrentTiltX * 0.7f + xTilt * 0.3f;
        sCurrentTiltY = sCurrentTiltY * 0.7f + yTilt * 0.3f;
    }
    
    /**
     * Check if touch is likely a palm
     */
    private static boolean isLikelyPalmTouch(int pointerIndex) {
        // Only check for second+ finger in multi-touch
        if (pointerIndex == 0) return false;
        
        // Palm probability based on grip mode
        switch (sCurrentGripMode) {
            case ONE_HANDED_LEFT:
            case ONE_HANDED_RIGHT:
                // Single-handed use - higher palm probability
                return sRandom.nextFloat() < 0.08f;
                
            case TWO_HANDED_LANDSCAPE:
                // Both hands - lower palm (fingers from other hand)
                return sRandom.nextFloat() < 0.02f;
                
            default:
                return sRandom.nextFloat() < 0.05f;
        }
    }
    
    /**
     * Set screen dimensions
     */
    public static void setScreenDimensions(int width, int height) {
        sScreenWidth = width;
        sScreenHeight = height;
    }
    
    /**
     * Get current grip mode
     */
    public static GripMode getCurrentGripMode() {
        return sCurrentGripMode;
    }
    
    /**
     * Get current tilt
     */
    public static float[] getCurrentTilt() {
        return new float[] { sCurrentTiltX, sCurrentTiltY };
    }
    
    /**
     * Get state for cross-hook coherence
     */
    public static DeviceGripState getState() {
        DeviceGripState state = new DeviceGripState();
        state.enabled = sEnabled;
        state.currentGripMode = sCurrentGripMode;
        state.targetGripMode = sTargetGripMode;
        state.isTransitioning = (sCurrentGripMode == GripMode.TRANSITIONING);
        state.transitionProgress = calculateTransitionProgress();
        
        state.tiltX = sCurrentTiltX;
        state.tiltY = sCurrentTiltY;
        state.isWalking = sIsWalking;
        
        state.isThumbReachZone = sIsThumbReachZone;
        state.handSize = sHandSize;
        
        return state;
    }
    
    /**
     * Calculate transition progress (0.0-1.0)
     */
    private static float calculateTransitionProgress() {
        if (sCurrentGripMode != GripMode.TRANSITIONING) return 1.0f;
        
        long elapsed = System.currentTimeMillis() - sGripModeChangeTime;
        return Math.min(1.0f, (float) elapsed / sGripModeTransitionDuration);
    }
    
    /**
     * State container
     */
    public static class DeviceGripState {
        public boolean enabled;
        public GripMode currentGripMode;
        public GripMode targetGripMode;
        public boolean isTransitioning;
        public float transitionProgress;
        
        public float tiltX;
        public float tiltY;
        public boolean isWalking;
        
        public boolean isThumbReachZone;
        public HandSize handSize;
        
        public boolean isOneHanded() {
            return currentGripMode == GripMode.ONE_HANDED_LEFT ||
                   currentGripMode == GripMode.ONE_HANDED_RIGHT;
        }
        
        public boolean isTwoHanded() {
            return currentGripMode == GripMode.TWO_HANDED_PORTRAIT ||
                   currentGripMode == GripMode.TWO_HANDED_LANDSCAPE;
        }
    }
}
