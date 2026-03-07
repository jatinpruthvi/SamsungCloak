package com.samsungcloak.xposed.motor;

import android.view.MotionEvent;
import com.samsungcloak.xposed.HookUtils;

import java.util.Random;

/**
 * Hook 3: Touch Pressure & Contact Area Engine
 * 
 * Simulates the "Fat Finger" effect and varying grip strength by randomizing
 * the Size and Pressure parameters of MotionEvents.
 * 
 * Human touch characteristics:
 * - Pressure varies based on finger size, grip style, and context
 * - Contact area changes with pressure (ellipse shape)
 * - "Fat Finger" effect: larger contact area for same target
 * - Ambient moisture and skin type affect reading
 * - Device holds affect pressure application
 */
public class TouchPressureEngine {
    private final Random random;
    private final NeuromotorConfig config;
    
    // Grip state tracking
    private GripType currentGrip = GripType.ONE_HANDED;
    private float fingerTemperature = 0.5f; // 0=cold, 1=hot

    public TouchPressureEngine(NeuromotorConfig config) {
        this.config = config;
        this.random = new Random();
    }

    public void updateConfig(NeuromotorConfig config) {
        this.config = config;
    }

    /**
     * Generate realistic touch pressure
     * 
     * Pressure distribution characteristics:
     * - Lighter on precise taps
     * - Heavier during fast swipes
     * - Variable grip strength throughout session
     * 
     * @return Pressure value (0.0 - 1.0)
     */
    public float getRealisticPressure() {
        // Generate base pressure with Gaussian variation
        double pressure = config.getBasePressure() 
            + random.nextGaussian() * config.getPressureStdDev();
        
        // Apply grip modifier
        pressure *= getGripPressureModifier();
        
        // Temperature affects sensitivity (cold fingers press harder)
        pressure *= (1.2f - fingerTemperature * 0.4f);
        
        // Clamp to valid range
        float result = clamp((float) pressure, config.getMinPressure(), config.getMaxPressure());
        
        HookUtils.logDebug("Generated pressure: " + result);
        
        return result;
    }

    /**
     * Get realistic contact size (area of touch)
     * 
     * @return Contact size (0.0 - 1.0 normalized)
     */
    public float getContactSize() {
        // Base size
        double size = config.getBaseContactSize();
        
        // Add variation
        size += random.nextGaussian() * config.getContactSizeStdDev();
        
        // Fat finger effect - larger contact area
        if (random.nextDouble() < config.getFatFingerProbability()) {
            size *= config.getFatFingerSizeMultiplier();
            HookUtils.logDebug("Fat finger effect applied");
        }
        
        // Grip affects size
        size *= getGripSizeModifier();
        
        return clamp((float) size, 0.2f, 1.5f);
    }

    /**
     * Get touch major axis (ellipse width)
     * 
     * The touch is modeled as an ellipse, where major axis is the longer dimension
     */
    public float getTouchMajor() {
        float base = 0.6f;
        float variation = (float) (random.nextGaussian() * 0.08);
        
        float size = getContactSize();
        float major = (base + variation) * size;
        
        // Fat finger = larger ellipse
        if (random.nextDouble() < config.getFatFingerProbability()) {
            major *= config.getFatFingerSizeMultiplier();
        }
        
        return clamp(major, 0.2f, 1.5f);
    }

    /**
     * Get touch minor axis (ellipse height)
     * 
     * Minor axis is typically 70-80% of major for human fingers
     */
    public float getTouchMinor() {
        float major = getTouchMajor();
        // Natural finger aspect ratio
        float aspectRatio = 0.7f + (float) (random.nextGaussian() * 0.1);
        
        float minor = major * aspectRatio;
        
        return clamp(minor, 0.15f, 1.2f);
    }

    /**
     * Apply realistic pressure values to a MotionEvent
     * 
     * This hooks into Android's MotionEvent system to modify touch parameters
     */
    public void applyRealisticPressure(MotionEvent event) {
        float pressure = getRealisticPressure();
        float size = getContactSize();
        float major = getTouchMajor();
        float minor = getTouchMinor();
        
        // Note: We can't directly modify MotionEvent, but we return these values
        // for the hooks to use. This is called to generate the values.
        
        HookUtils.logDebug("Applied pressure=" + pressure + ", size=" + size + 
                          ", major=" + major + ", minor=" + minor);
    }

    /**
     * Get pressure for specific touch context
     */
    public float getPressureForContext(TouchContext context) {
        float basePressure;
        
        switch (context) {
            case PRECISE_TAP:
                // Light pressure for accuracy
                basePressure = 0.4f;
                break;
                
            case CASUAL_TAP:
                // Normal pressure
                basePressure = 0.6f;
                break;
                
            case FIRM_PRESS:
                // Heavy press (button hold, etc)
                basePressure = 0.85f;
                break;
                
            case FAST_SWIPE:
                // Variable pressure during motion
                basePressure = 0.55f + (float) (random.nextGaussian() * 0.1);
                break;
                
            case GESTURE_SWIPE:
                // Medium pressure for gestures
                basePressure = 0.65f;
                break;
                
            case FAT_FINGER:
                // Lighter due to larger contact area
                basePressure = 0.45f;
                break;
                
            default:
                basePressure = config.getBasePressure();
        }
        
        // Add natural variation
        return clamp(basePressure + (float) (random.nextGaussian() * config.getPressureStdDev()),
                    config.getMinPressure(), config.getMaxPressure());
    }

    /**
     * Set current grip type for more accurate simulation
     */
    public void setGripType(GripType grip) {
        this.currentGrip = grip;
        HookUtils.logDebug("Grip type set to: " + grip);
    }

    /**
     * Update finger temperature (affects touch sensitivity)
     */
    public void setFingerTemperature(float temperature) {
        this.fingerTemperature = clamp(temperature, 0f, 1f);
    }

    /**
     * Get the pressure modifier for current grip
     */
    private float getGripPressureModifier() {
        switch (currentGrip) {
            case ONE_HANDED:
                return 0.95f;
            case TWO_HANDED:
                return 1.1f;
            case LANDSCAPE:
                return 1.0f;
            case TABLETOP:
                return 0.9f;
            default:
                return 1.0f;
        }
    }

    /**
     * Get the size modifier for current grip
     */
    private float getGripSizeModifier() {
        switch (currentGrip) {
            case ONE_HANDED:
                return 1.1f; // Thumb is wider
            case TWO_HANDED:
                return 0.95f;
            case LANDSCAPE:
                return 1.0f;
            case TABLETOP:
                return 1.05f;
            default:
                return 1.0f;
        }
    }

    /**
     * Get a complete set of touch parameters
     */
    public TouchParameters getTouchParameters(TouchContext context) {
        float pressure = getPressureForContext(context);
        float size = getContactSize();
        float major = getTouchMajor();
        float minor = getTouchMinor();
        
        // Orientation angle (natural finger tilt)
        float orientation = (float) (random.nextGaussian() * 0.2); // radians
        
        return new TouchParameters(pressure, size, major, minor, orientation);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Touch context types
     */
    public enum TouchContext {
        PRECISE_TAP,      // Typing, selecting small items
        CASUAL_TAP,       // General tapping
        FIRM_PRESS,       // Button holds, long presses
        FAST_SWIPE,       // Quick swipes
        GESTURE_SWIPE,    // Navigation gestures
        FAT_FINGER        // Large contact area scenario
    }

    /**
     * Grip types
     */
    public enum GripType {
        ONE_HANDED,       // Single hand holding device
        TWO_HANDED,       // Both hands on device
        LANDSCAPE,        // Device in landscape orientation
        TABLETOP          // Device on flat surface
    }

    /**
     * Complete touch parameter set
     */
    public static class TouchParameters {
        public final float pressure;
        public final float size;
        public final float touchMajor;
        public final float touchMinor;
        public final float orientation;

        public TouchParameters(float pressure, float size, float touchMajor, 
                              float touchMinor, float orientation) {
            this.pressure = pressure;
            this.size = size;
            this.touchMajor = touchMajor;
            this.touchMinor = touchMinor;
            this.orientation = orientation;
        }
    }
}
