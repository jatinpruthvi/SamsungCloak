package com.simulation.maturity.level4;

import java.util.Random;

/**
 * Contextual Awareness - Environmental State Machine
 * 
 * Models how user behavior shifts based on device/environmental context:
 * - Battery level affects urgency and interaction speed
 * - Network quality affects tolerance for delays
 * - Time of day affects engagement patterns
 * - Device temperature affects willingness to use
 * 
 * Implements a Finite State Machine (FSM) for context states:
 * - NORMAL: Default state
 * - LOW_BATTERY: Battery < 15%, panic mode
 * - POWER_SAVER: Power saver mode active
 * - NO_NETWORK: Offline mode
 * - THROTTLING: Device thermal throttling
 * - URGENT: Critical state (battery < 5%)
 * 
 * Device Target: Samsung Galaxy A12 (SM-A125U)
 * - Battery: 5000mAh
 * - SoC: MediaTek Helio P35
 */
public class ContextualAwareness {
    
    // Context state enumeration
    public enum DeviceEnvironment {
        NORMAL("Normal usage"),
        LOW_BATTERY("Battery < 15%, power conscious"),
        CRITICAL_BATTERY("Battery < 5%, urgent mode"),
        POWER_SAVER("Power saver mode active"),
        NO_NETWORK("Offline mode"),
        POOR_NETWORK("Slow connection"),
        THERMAL_THROTTLING("Device overheating"),
        DO_NOT_DISTURB("Focus mode");
        
        private final String description;
        
        DeviceEnvironment(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // Behavioral modifiers per state
    private static final double NORMAL_SPEED = 1.0;
    private static final double LOW_BATTERY_SPEED = 1.5;      // Faster to complete tasks
    private static final double CRITICAL_SPEED = 2.0;         // Very fast, urgent
    private static final double POWER_SAVER_SPEED = 0.8;      // Slower, conserving
    private static final double NO_NETWORK_SPEED = 0.5;       // Cached operations only
    private static final double THROTTLE_SPEED = 0.6;         // Limited capability
    
    // Current state
    private DeviceEnvironment currentEnvironment;
    private int batteryLevel;           // 0-100
    private int networkQuality;         // 0-100 (ms latency score)
    private float deviceTemperature;    // Celsius
    private int hourOfDay;              // 0-23
    private boolean powerSaverActive;
    private boolean focusModeActive;
    
    private Random random;
    
    public ContextualAwareness() {
        this.currentEnvironment = DeviceEnvironment.NORMAL;
        this.batteryLevel = 80;
        this.networkQuality = 100;
        this.deviceTemperature = 35.0f;
        this.hourOfDay = 12;
        this.powerSaverActive = false;
        this.focusModeActive = false;
        this.random = new Random();
    }
    
    /**
     * Update device context and determine environment state
     * Call this whenever device state changes
     */
    public void updateContext(int batteryLevel, int networkLatencyMs, 
                               float temperatureCelsius, int hour) {
        this.batteryLevel = Math.max(0, Math.min(100, batteryLevel));
        this.deviceTemperature = temperatureCelsius;
        this.hourOfDay = hour;
        
        // Calculate network quality score (lower latency = higher quality)
        // Latency: <50ms = 100, 50-100ms = 80, 100-200ms = 60, >200ms = 40
        if (networkLatencyMs < 50) {
            this.networkQuality = 100;
        } else if (networkLatencyMs < 100) {
            this.networkQuality = 80;
        } else if (networkLatencyMs < 200) {
            this.networkQuality = 60;
        } else if (networkLatencyMs < 500) {
            this.networkQuality = 40;
        } else {
            this.networkQuality = 20;
        }
        
        determineEnvironmentState();
    }
    
    /**
     * Determine current environment state based on device metrics
     */
    private void determineEnvironmentState() {
        // Priority order for state determination
        
        // 1. Critical battery override
        if (batteryLevel < 5) {
            currentEnvironment = DeviceEnvironment.CRITICAL_BATTERY;
            return;
        }
        
        // 2. Thermal throttling
        if (deviceTemperature > 45.0f) {
            currentEnvironment = DeviceEnvironment.THERMAL_THROTTLING;
            return;
        }
        
        // 3. Power saver mode
        if (powerSaverActive) {
            currentEnvironment = DeviceEnvironment.POWER_SAVER;
            return;
        }
        
        // 4. No network
        if (networkQuality < 20) {
            currentEnvironment = DeviceEnvironment.NO_NETWORK;
            return;
        }
        
        // 5. Low battery
        if (batteryLevel < 15) {
            currentEnvironment = DeviceEnvironment.LOW_BATTERY;
            return;
        }
        
        // 6. Poor network
        if (networkQuality < 60) {
            currentEnvironment = DeviceEnvironment.POOR_NETWORK;
            return;
        }
        
        // 7. Focus mode
        if (focusModeActive) {
            currentEnvironment = DeviceEnvironment.DO_NOT_DISTURB;
            return;
        }
        
        // Default: Normal
        currentEnvironment = DeviceEnvironment.NORMAL;
    }
    
    /**
     * Get interaction speed multiplier based on current context
     * >1.0 = faster interactions, <1.0 = slower
     */
    public double getSpeedMultiplier() {
        switch (currentEnvironment) {
            case CRITICAL_BATTERY:
                return CRITICAL_SPEED;
            case LOW_BATTERY:
                return LOW_BATTERY_SPEED;
            case POWER_SAVER:
                return POWER_SAVER_SPEED;
            case NO_NETWORK:
                return NO_NETWORK_SPEED;
            case THERMAL_THROTTLING:
                return THROTTLE_SPEED;
            default:
                return NORMAL_SPEED;
        }
    }
    
    /**
     * Get adjusted response time for current context
     */
    public int getAdjustedResponseTime(int baseTimeMs) {
        double multiplier = getSpeedMultiplier();
        
        // Add network latency penalty if relevant
        if (currentEnvironment == DeviceEnvironment.POOR_NETWORK) {
            // Add 50% latency penalty
            multiplier *= 1.0 + (100 - networkQuality) / 100.0;
        }
        
        return (int) (baseTimeMs * multiplier);
    }
    
    /**
     * Check if certain operations should be deferred
     * Returns true if user would likely avoid certain actions
     */
    public boolean shouldDeferOperation(String operationType) {
        switch (currentEnvironment) {
            case CRITICAL_BATTERY:
            case LOW_BATTERY:
                // Defer heavy operations
                return operationType.equals("download") || 
                       operationType.equals("video") ||
                       operationType.equals("gaming");
                       
            case NO_NETWORK:
                // Defer network-dependent operations
                return operationType.equals("stream") ||
                       operationType.equals("upload") ||
                       operationType.equals("sync");
                       
            case THERMAL_THROTTLING:
                // Defer intensive operations
                return operationType.equals("gaming") ||
                       operationType.equals("camera");
                       
            default:
                return false;
        }
    }
    
    /**
     * Get tolerance for delays based on context
     * Returns max wait time before frustration
     */
    public int getDelayToleranceMs() {
        switch (currentEnvironment) {
            case CRITICAL_BATTERY:
                return 2000;   // Very impatient, needs quick results
            case LOW_BATTERY:
                return 3000;   // Somewhat impatient
            case POWER_SAVER:
                return 5000;   // More patient
            case NO_NETWORK:
                return 0;      // Expects offline/cached
            case THERMAL_THROTTLING:
                return 8000;   // Understanding of limitations
            case POOR_NETWORK:
                return 10000;  // Expects slow
            default:
                return 5000;   // Normal tolerance
        }
    }
    
    /**
     * Get interaction complexity preference based on context
     * Returns max complexity level (1-5)
     */
    public int getComplexityPreference() {
        switch (currentEnvironment) {
            case CRITICAL_BATTERY:
                return 1; // Simple, quick actions only
            case LOW_BATTERY:
                return 2; // Prefer simple
            case POWER_SAVER:
                return 3; // Normal
            case NO_NETWORK:
                return 2; // Cached/simple only
            case THERMAL_THROTTLING:
                return 2; // Light usage
            default:
                return 5; // Full complexity
        }
    }
    
    /**
     * Check if user is in "panic/urgent" mode
     * Triggered by critical battery or similar urgent states
     */
    public boolean isPanicMode() {
        return currentEnvironment == DeviceEnvironment.CRITICAL_BATTERY ||
               currentEnvironment == DeviceEnvironment.LOW_BATTERY;
    }
    
    /**
     * Check if user would exhibit rushed behavior
     */
    public boolean isRushedBehavior() {
        return getSpeedMultiplier() > 1.0;
    }
    
    /**
     * Get current environment state
     */
    public DeviceEnvironment getCurrentEnvironment() {
        return currentEnvironment;
    }
    
    /**
     * Get current battery level
     */
    public int getBatteryLevel() {
        return batteryLevel;
    }
    
    /**
     * Get current network quality (0-100)
     */
    public int getNetworkQuality() {
        return networkQuality;
    }
    
    /**
     * Set power saver mode
     */
    public void setPowerSaverMode(boolean active) {
        this.powerSaverActive = active;
        determineEnvironmentState();
    }
    
    /**
     * Set focus mode
     */
    public void setFocusMode(boolean active) {
        this.focusModeActive = active;
        determineEnvironmentState();
    }
    
    /**
     * Get context summary
     */
    public ContextSummary getSummary() {
        return new ContextSummary(
            currentEnvironment,
            batteryLevel,
            networkQuality,
            deviceTemperature,
            hourOfDay,
            getSpeedMultiplier(),
            isPanicMode()
        );
    }
    
    /**
     * Context summary
     */
    public static class ContextSummary {
        public final DeviceEnvironment environment;
        public final int batteryLevel;
        public final int networkQuality;
        public final float temperatureCelsius;
        public final int hourOfDay;
        public final double speedMultiplier;
        public final boolean isPanic;
        
        public ContextSummary(DeviceEnvironment environment, int batteryLevel,
                              int networkQuality, float temperatureCelsius,
                              int hourOfDay, double speedMultiplier, boolean isPanic) {
            this.environment = environment;
            this.batteryLevel = batteryLevel;
            this.networkQuality = networkQuality;
            this.temperatureCelsius = temperatureCelsius;
            this.hourOfDay = hourOfDay;
            this.speedMultiplier = speedMultiplier;
            this.isPanic = isPanic;
        }
        
        @Override
        public String toString() {
            return String.format(
                "Context{battery=%d%%, network=%d, temp=%.1f°C, hour=%02d, " +
                "state=%s, speed=%.1fx, panic=%s}",
                batteryLevel, networkQuality, temperatureCelsius, hourOfDay,
                environment, speedMultiplier, isPanic
            );
        }
    }
}
