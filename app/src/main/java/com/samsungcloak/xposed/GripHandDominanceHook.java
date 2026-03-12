package com.samsungcloak.xposed;

import android.content.Context;
import android.view.Display;
import android.view.MotionEvent;
import android.view.WindowManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * GripHandDominanceHook - Hand Dominance & Phone Grip Simulation
 *
 * Simulates realistic hand dominance and grip patterns:
 *
 * 1. Hand Dominance - Left vs right-handed users
 * 2. Grip Types - One-hand, two-hand, cradle, pocket retrieval
 * 3. Reachability Zones - Thumb reach based on grip
 * 4. Grip Transitions - Natural grip changes during use
 * 5. One-Handed Mode - UI adaptation patterns
 * 6. Tremor Simulation - Age-related or situational shaking
 * 7. Finger Length Calibration - Touch accuracy by grip
 *
 * Novelty: NOT covered by existing hooks
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class GripHandDominanceHook {

    private static final String TAG = "[Grip][Hand]";
    private static final boolean DEBUG = true;
    
    // Toggleable via SharedPreferences
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    
    // Hand dominance
    public enum Handedness {
        RIGHT_HANDED,
        LEFT_HANDED,
        AMBIDEXTROUS
    }
    
    // Grip types
    public enum GripType {
        ONE_HANDED_BASE,      // Pinky brace, thumb operation
        ONE_HANDED_HIGH,      // Upper grip, reach difficult
        ONE_HANDED_LOW,       // Lower grip, stable but limited
        TWO_HANDED_TYPE,      // Both hands, thumbs
        TWO_HANDED_GAME,      // Horizontal grip
        CRADLE_HOLD,          // In palm, finger operation
        POCKET_RETRIEVAL,     // Quick grab, awkward initial position
        TABLE_PICKUP          // Lifted from flat surface
    }
    
    // Current state
    private static Handedness userHandedness = Handedness.RIGHT_HANDED;
    private static GripType currentGrip = GripType.ONE_HANDED_BASE;
    private static boolean isOneHandedMode = false;
    private static float reachabilityZone = 0.7f; // 0-1, percentage of screen reachable
    
    // Tremor settings
    private static boolean tremorEnabled = true;
    private static float tremorIntensity = 0.0f; // 0-1
    private static float tremorFrequency = 8.0f; // Hz
    
    // Touch offset calibration
    private static float xOffset = 0.0f;
    private static float yOffset = 0.0f;
    private static float touchAccuracy = 1.0f;
    
    // Grip transition
    private static boolean autoDetectGrip = true;
    private static long gripChangeCooldown = 2000; // ms
    private static long lastGripChange = 0;
    private static final List<GripTransition> gripHistory = new CopyOnWriteArrayList<>();
    
    // Tremor noise generator
    private static long sessionStartTime = System.currentTimeMillis();
    private static final Random random = new Random();
    private static double tremorPhase = 0;
    
    /**
     * Grip transition event
     */
    public static class GripTransition {
        public GripType from;
        public GripType to;
        public long timestamp;
        
        public GripTransition(GripType from, GripType to) {
            this.from = from;
            this.to = to;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (hookInitialized.getAndSet(true)) {
            HookUtils.logDebug(TAG, "Already initialized");
            return;
        }
        
        HookUtils.logInfo(TAG, "Initializing Grip & Hand Dominance Hook");
        
        try {
            hookMotionEvent(lpparam);
            hookDisplay(lpparam);
            
            HookUtils.logInfo(TAG, "Grip hook initialized");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Init failed: " + t.getMessage());
            hookInitialized.set(false);
        }
    }
    
    private static void hookMotionEvent(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> motionEventClass = XposedHelpers.findClass(
                "android.view.MotionEvent", lpparam.classLoader
            );
            
            // Hook getX to apply grip-based offset
            XposedBridge.hookAllMethods(motionEventClass, "getX",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    int pointerIndex = 0;
                    if (param.args.length > 0 && param.args[0] instanceof Integer) {
                        pointerIndex = (int) param.args[0];
                    }
                    
                    float originalX = (float) param.getResult();
                    float modifiedX = applyGripOffsetX(originalX, pointerIndex);
                    
                    if (modifiedX != originalX) {
                        param.setResult(modifiedX);
                    }
                }
            });
            
            // Hook getY to apply grip-based offset
            XposedBridge.hookAllMethods(motionEventClass, "getY",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    int pointerIndex = 0;
                    if (param.args.length > 0 && param.args[0] instanceof Integer) {
                        pointerIndex = (int) param.args[0];
                    }
                    
                    float originalY = (float) param.getResult();
                    float modifiedY = applyGripOffsetY(originalY, pointerIndex);
                    
                    if (modifiedY != originalY) {
                        param.setResult(modifiedY);
                    }
                }
            });
            
            // Hook getRawX/rawY for more accurate injection
            XposedBridge.hookAllMethods(motionEventClass, "getRawX",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    // Could add tremor here
                }
            });
            
            HookUtils.logInfo(TAG, "MotionEvent hooked for grip simulation");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "MotionEvent hook failed: " + t.getMessage());
        }
    }
    
    private static void hookDisplay(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> displayClass = XposedHelpers.findClass(
                "android.view.Display", lpparam.classLoader
            );
            
            // Could hook for orientation-specific grip changes
            HookUtils.logInfo(TAG, "Display hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Display hook failed: " + t.getMessage());
        }
    }
    
    // ========== Touch Position Modifications ==========
    
    /**
     * Apply X offset based on grip type and handedness
     */
    private static float applyGripOffsetX(float originalX, int pointerIndex) {
        if (pointerIndex > 0) {
            // Multi-touch: add spread for two-handed grip
            if (currentGrip == GripType.TWO_HANDED_TYPE || 
                currentGrip == GripType.TWO_HANDED_GAME) {
                return originalX + (pointerIndex == 1 ? 20 : -20);
            }
            return originalX;
        }
        
        // Apply grip-based offset
        float offset = getGripXOffset();
        
        // Apply handedness shift
        if (userHandedness == Handedness.LEFT_HANDED) {
            offset = -offset;
        }
        
        return originalX + offset;
    }
    
    /**
     * Apply Y offset based on grip type
     */
    private static float applyGripOffsetY(float originalY, int pointerIndex) {
        float offset = getGripYOffset();
        
        // Add tremor if enabled
        if (tremorEnabled && pointerIndex == 0) {
            float tremorOffset = getTremorOffset();
            offset += tremorOffset;
        }
        
        return originalY + offset;
    }
    
    /**
     * Get X offset based on current grip
     */
    private static float getGripXOffset() {
        switch (currentGrip) {
            case ONE_HANDED_BASE:
                return xOffset + 10; // Slight right for right-handed base grip
            case ONE_HANDED_HIGH:
                return xOffset + 15; // More shift when gripping high
            case ONE_HANDED_LOW:
                return xOffset + 5;  // Less shift when gripping low
            case TWO_HANDED_TYPE:
                return 0; // Centered
            case TWO_HANDED_GAME:
                return 0; // Centered
            case CRADLE_HOLD:
                return xOffset + 20; // More offset in cradle
            case POCKET_RETRIEVAL:
                return xOffset + random.nextFloat() * 30 - 15; // Uncertain
            case TABLE_PICKUP:
                return xOffset + random.nextFloat() * 10 - 5; // Slight adjustment
            default:
                return xOffset;
        }
    }
    
    /**
     * Get Y offset based on current grip
     */
    private static float getGripYOffset() {
        switch (currentGrip) {
            case ONE_HANDED_BASE:
                return yOffset + 20; // Slight up
            case ONE_HANDED_HIGH:
                return yOffset - 10; // Grip high reaches more
            case ONE_HANDED_LOW:
                return yOffset + 40; // Grip low, thumb lower
            case TWO_HANDED_TYPE:
                return 0;
            case TWO_HANDED_GAME:
                return 0;
            case CRADLE_HOLD:
                return yOffset + 10;
            case POCKET_RETRIEVAL:
                return yOffset + random.nextFloat() * 50 - 25; // Very uncertain
            case TABLE_PICKUP:
                return yOffset + random.nextFloat() * 20 - 10;
            default:
                return yOffset;
        }
    }
    
    /**
     * Calculate tremor offset
     */
    private static float getTremorOffset() {
        if (tremorIntensity <= 0) return 0;
        
        long elapsed = System.currentTimeMillis() - sessionStartTime;
        double timeSeconds = elapsed / 1000.0;
        
        // Generate tremor using multiple sine waves for realistic effect
        double tremor = 0;
        tremor += Math.sin(timeSeconds * tremorFrequency * 2 * Math.PI) * 0.3;
        tremor += Math.sin(timeSeconds * tremorFrequency * 3.7 * Math.PI) * 0.2;
        tremor += Math.sin(timeSeconds * tremorFrequency * 1.3 * Math.PI) * 0.1;
        
        // Add random component
        tremor += (random.nextFloat() - 0.5) * 0.3;
        
        return (float)(tremor * tremorIntensity * 3.0); // Scale to pixels
    }
    
    // ========== Grip Management ==========
    
    /**
     * Change grip type
     */
    public static void setGrip(GripType grip) {
        if (currentGrip != grip) {
            GripTransition transition = new GripTransition(currentGrip, grip);
            gripHistory.add(transition);
            
            currentGrip = grip;
            updateReachabilityZone();
            
            HookUtils.logInfo(TAG, "Grip changed: " + transition.from + " -> " + transition.to);
        }
    }
    
    /**
     * Update reachability zone based on grip
     */
    private static void updateReachabilityZone() {
        switch (currentGrip) {
            case ONE_HANDED_BASE:
                reachabilityZone = 0.65f;
                break;
            case ONE_HANDED_HIGH:
                reachabilityZone = 0.85f;
                break;
            case ONE_HANDED_LOW:
                reachabilityZone = 0.45f;
                break;
            case TWO_HANDED_TYPE:
                reachabilityZone = 0.9f;
                break;
            case TWO_HANDED_GAME:
                reachabilityZone = 0.95f;
                break;
            case CRADLE_HOLD:
                reachabilityZone = 0.55f;
                break;
            case POCKET_RETRIEVAL:
                reachabilityZone = 0.3f; // Uncertain
                break;
            case TABLE_PICKUP:
                reachabilityZone = 0.5f;
                break;
        }
        
        // Update touch accuracy based on grip
        switch (currentGrip) {
            case ONE_HANDED_BASE:
                touchAccuracy = 0.9f;
                break;
            case POCKET_RETRIEVAL:
                touchAccuracy = 0.5f; // Poor accuracy
                break;
            case TABLE_PICKUP:
                touchAccuracy = 0.7f;
                break;
            default:
                touchAccuracy = 0.95f;
        }
    }
    
    /**
     * Simulate natural grip transition based on activity
     */
    public static void simulateGripTransition() {
        if (!autoDetectGrip) return;
        
        long now = System.currentTimeMillis();
        if (now - lastGripChange < gripChangeCooldown) return;
        
        // Random grip change probability based on usage
        double changeProbability = 0.1; // 10% chance per check
        
        if (random.nextDouble() < changeProbability) {
            GripType newGrip = getNextNaturalGrip();
            setGrip(newGrip);
            lastGripChange = now;
        }
    }
    
    /**
     * Get next natural grip based on context
     */
    private static GripType getNextNaturalGrip() {
        GripType[] possibleGrips;
        
        switch (currentGrip) {
            case ONE_HANDED_BASE:
                possibleGrips = new GripType[]{
                    GripType.ONE_HANDED_HIGH,
                    GripType.ONE_HANDED_LOW,
                    GripType.TWO_HANDED_TYPE
                };
                break;
            case TWO_HANDED_TYPE:
                possibleGrips = new GripType[]{
                    GripType.ONE_HANDED_BASE,
                    GripType.ONE_HANDED_HIGH
                };
                break;
            default:
                possibleGrips = new GripType[]{
                    GripType.ONE_HANDED_BASE,
                    GripType.ONE_HANDED_HIGH,
                    GripType.CRADLE_HOLD
                };
        }
        
        return possibleGrips[random.nextInt(possibleGrips.length)];
    }
    
    // ========== Configuration ==========
    
    public static void setHandedness(Handedness handedness) {
        userHandedness = handedness;
        
        // Update X offset based on handedness
        if (handedness == Handedness.LEFT_HANDED) {
            xOffset = -xOffset;
        }
    }
    
    public static void setTremorIntensity(float intensity) {
        tremorIntensity = Math.max(0, Math.min(1, intensity));
    }
    
    public static void setOneHandedMode(boolean enabled) {
        isOneHandedMode = enabled;
        if (enabled && currentGrip != GripType.TWO_HANDED_TYPE) {
            setGrip(GripType.ONE_HANDED_BASE);
        }
    }
    
    public static Handedness getHandedness() {
        return userHandedness;
    }
    
    public static GripType getCurrentGrip() {
        return currentGrip;
    }
    
    public static float getReachabilityZone() {
        return reachabilityZone;
    }
    
    public static float getTremorIntensity() {
        return tremorIntensity;
    }
    
    public static boolean isOneHandedMode() {
        return isOneHandedMode;
    }
    
    public static List<GripTransition> getGripHistory() {
        return new ArrayList<>(gripHistory);
    }
    
    public static void setEnabled(boolean enabled) {
        GripHandDominanceHook.enabled = enabled;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
}
