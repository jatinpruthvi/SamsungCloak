package com.cognitive.testing.environmental;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Battery Constraint Modeling Hook
 * Simulates battery drain and modifies interaction behavior based on
 * simulated battery percentage, entering "Power Save Mode" behavior at low charge.
 * 
 * Real-world usage patterns this simulates:
 * - Gradual battery drain during extended testing sessions
 * - Performance throttling in power save mode
 * - Reduced touch responsiveness at low battery
 * - Screen dimming and timeout changes
 * - Background process limitations
 */
public class BatteryConstraintHook {
    
    private final EnvironmentalConfig config;
    private final Random random;
    
    private volatile int currentBatteryPercentage;
    private volatile boolean isInPowerSaveMode;
    private volatile long sessionStartTime;
    private volatile long lastBatteryUpdate;
    
    private final AtomicInteger interactionsInPowerSave;
    private final AtomicInteger performanceThrottles;
    private final AtomicInteger batteryWarnings;
    private final AtomicLong totalDrainTimeMs;
    
    private final AtomicReference<BatteryState> lastBatteryState;
    
    public BatteryConstraintHook(EnvironmentalConfig config) {
        this(config, new Random());
    }
    
    public BatteryConstraintHook(EnvironmentalConfig config, Random random) {
        this.config = config;
        this.random = random;
        this.currentBatteryPercentage = config.getInitialBatteryPercentage();
        this.isInPowerSaveMode = currentBatteryPercentage <= config.getPowerSaveModeThreshold();
        this.sessionStartTime = System.currentTimeMillis();
        this.lastBatteryUpdate = sessionStartTime;
        this.interactionsInPowerSave = new AtomicInteger(0);
        this.performanceThrottles = new AtomicInteger(0);
        this.batteryWarnings = new AtomicInteger(0);
        this.totalDrainTimeMs = new AtomicLong(0);
        this.lastBatteryState = new AtomicReference<>(calculateBatteryState());
    }
    
    /**
     * Update battery level based on elapsed time and drain rate
     */
    public void updateBattery() {
        if (!config.isEnableBatteryConstraints()) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        long timeSinceLastUpdate = currentTime - lastBatteryUpdate;
        long sessionDuration = currentTime - sessionStartTime;
        
        if (timeSinceLastUpdate < 1000) {
            return;
        }
        
        float drainRate = config.getBatteryDrainRatePerMinute();
        float drainPerMs = drainRate / 60000.0f;
        float drainAmount = timeSinceLastUpdate * drainPerMs;
        
        int oldPercentage = currentBatteryPercentage;
        currentBatteryPercentage = Math.max(0, (int) (currentBatteryPercentage - drainAmount));
        
        boolean wasInPowerSave = isInPowerSaveMode;
        isInPowerSaveMode = currentBatteryPercentage <= config.getPowerSaveModeThreshold();
        
        if (!wasInPowerSave && isInPowerSaveMode) {
            batteryWarnings.incrementAndGet();
        }
        
        lastBatteryUpdate = currentTime;
        totalDrainTimeMs.addAndGet(timeSinceLastUpdate);
        
        BatteryState newState = calculateBatteryState();
        lastBatteryState.set(newState);
    }
    
    private BatteryState calculateBatteryState() {
        if (currentBatteryPercentage == 0) {
            return BatteryState.CRITICAL;
        } else if (currentBatteryPercentage <= config.getPowerSaveModeThreshold()) {
            return BatteryState.LOW;
        } else if (currentBatteryPercentage <= 30) {
            return BatteryState.MEDIUM_LOW;
        } else if (currentBatteryPercentage <= 60) {
            return BatteryState.MEDIUM;
        } else {
            return BatteryState.HIGH;
        }
    }
    
    /**
     * Check if power save mode is active
     */
    public boolean isInPowerSaveMode() {
        return isInPowerSaveMode;
    }
    
    /**
     * Get current battery percentage
     */
    public int getCurrentBatteryPercentage() {
        return currentBatteryPercentage;
    }
    
    /**
     * Get current battery state
     */
    public BatteryState getBatteryState() {
        return lastBatteryState.get();
    }
    
    /**
     * Apply power save mode modifier to interaction delay
     */
    public long applyPowerSaveModifier(long baseDelayMs) {
        if (!isInPowerSaveMode || !config.isEnableBatteryConstraints()) {
            return baseDelayMs;
        }
        
        interactionsInPowerSave.incrementAndGet();
        performanceThrottles.incrementAndGet();
        
        float modifier = config.getPowerSaveModeInteractionModifier();
        float batteryFactor = 1.0f + (modifier * (1.0f - currentBatteryPercentage / 100.0f));
        
        long modifiedDelay = (long) (baseDelayMs * batteryFactor);
        
        if (random.nextFloat() < 0.15f) {
            modifiedDelay += random.nextInt(500);
        }
        
        return modifiedDelay;
    }
    
    /**
     * Check if performance should be throttled
     */
    public boolean shouldThrottlePerformance() {
        if (!config.isEnableBatteryConstraints()) {
            return false;
        }
        
        if (currentBatteryPercentage > config.getPowerSaveModeThreshold()) {
            return random.nextFloat() < 0.02f;
        } else if (currentBatteryPercentage > 10) {
            return random.nextFloat() < 0.08f;
        } else {
            return random.nextFloat() < 0.15f;
        }
    }
    
    /**
     * Get throttled delay multiplier
     */
    public float getThrottleMultiplier() {
        if (!isInPowerSaveMode || !config.isEnableBatteryConstraints()) {
            return 1.0f;
        }
        
        float baseMultiplier = config.getPowerSaveModeInteractionModifier();
        float batteryFactor = baseMultiplier + (1.0f - currentBatteryPercentage / 100.0f) * 0.5f;
        
        return Math.min(3.0f, batteryFactor);
    }
    
    /**
     * Check if screen should dim (simulating power save behavior)
     */
    public boolean shouldDimScreen() {
        if (!isInPowerSaveMode || !config.isEnableBatteryConstraints()) {
            return random.nextFloat() < 0.01f;
        }
        
        return random.nextFloat() < 0.08f;
    }
    
    /**
     * Get screen brightness modifier (0.0 - 1.0)
     */
    public float getScreenBrightnessModifier() {
        if (!isInPowerSaveMode || !config.isEnableBatteryConstraints()) {
            return 1.0f;
        }
        
        float brightness = 1.0f - (1.0f - currentBatteryPercentage / 100.0f) * 0.5f;
        
        if (random.nextFloat() < 0.1f) {
            brightness *= 0.8f;
        }
        
        return Math.max(0.3f, brightness);
    }
    
    /**
     * Check if background processes should be limited
     */
    public boolean shouldLimitBackgroundProcesses() {
        if (!isInPowerSaveMode || !config.isEnableBatteryConstraints()) {
            return false;
        }
        
        if (currentBatteryPercentage <= 10) {
            return random.nextFloat() < 0.25f;
        } else if (currentBatteryPercentage <= config.getPowerSaveModeThreshold()) {
            return random.nextFloat() < 0.12f;
        }
        
        return false;
    }
    
    /**
     * Simulate interaction delay due to low battery
     */
    public void applyBatteryDelay() {
        if (!config.isEnableBatteryConstraints()) {
            return;
        }
        
        int delay = 0;
        
        if (isInPowerSaveMode) {
            if (currentBatteryPercentage <= 10) {
                delay = 100 + random.nextInt(300);
            } else {
                delay = 50 + random.nextInt(150);
            }
        } else if (currentBatteryPercentage <= 30) {
            delay = 20 + random.nextInt(80);
        }
        
        if (delay > 0) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Check if battery warning should be shown
     */
    public boolean shouldShowBatteryWarning() {
        if (!config.isEnableBatteryConstraints()) {
            return false;
        }
        
        if (currentBatteryPercentage == 20 || currentBatteryPercentage == 10 || 
            currentBatteryPercentage == 5) {
            return random.nextFloat() < 0.7f;
        }
        
        return false;
    }
    
    /**
     * Manually set battery level
     */
    public void setBatteryPercentage(int percentage) {
        this.currentBatteryPercentage = Math.max(0, Math.min(100, percentage));
        this.isInPowerSaveMode = currentBatteryPercentage <= config.getPowerSaveModeThreshold();
        this.lastBatteryUpdate = System.currentTimeMillis();
        this.lastBatteryState.set(calculateBatteryState());
    }
    
    /**
     * Manually toggle power save mode
     */
    public void setPowerSaveMode(boolean enabled) {
        this.isInPowerSaveMode = enabled;
    }
    
    /**
     * Get time since session start
     */
    public long getSessionDurationMs() {
        return System.currentTimeMillis() - sessionStartTime;
    }
    
    /**
     * Get statistics
     */
    public BatteryStatistics getStatistics() {
        return new BatteryStatistics(
            currentBatteryPercentage,
            isInPowerSaveMode,
            getBatteryState(),
            interactionsInPowerSave.get(),
            performanceThrottles.get(),
            batteryWarnings.get(),
            totalDrainTimeMs.get(),
            getSessionDurationMs()
        );
    }
    
    /**
     * Reset to initial state
     */
    public void reset() {
        this.currentBatteryPercentage = config.getInitialBatteryPercentage();
        this.isInPowerSaveMode = currentBatteryPercentage <= config.getPowerSaveModeThreshold();
        this.sessionStartTime = System.currentTimeMillis();
        this.lastBatteryUpdate = sessionStartTime;
        this.interactionsInPowerSave.set(0);
        this.performanceThrottles.set(0);
        this.batteryWarnings.set(0);
        this.totalDrainTimeMs.set(0);
        this.lastBatteryState.set(calculateBatteryState());
    }
    
    public enum BatteryState {
        HIGH("High", 61, 100),
        MEDIUM("Medium", 31, 60),
        MEDIUM_LOW("Medium-Low", 16, 30),
        LOW("Low", 1, 15),
        CRITICAL("Critical", 0, 0);
        
        private final String displayName;
        private final int minPercentage;
        private final int maxPercentage;
        
        BatteryState(String displayName, int minPercentage, int maxPercentage) {
            this.displayName = displayName;
            this.minPercentage = minPercentage;
            this.maxPercentage = maxPercentage;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public int getMinPercentage() {
            return minPercentage;
        }
        
        public int getMaxPercentage() {
            return maxPercentage;
        }
        
        public static BatteryState fromPercentage(int percentage) {
            for (BatteryState state : values()) {
                if (percentage >= state.minPercentage && percentage <= state.maxPercentage) {
                    return state;
                }
            }
            return CRITICAL;
        }
    }
    
    public static class BatteryStatistics {
        private final int currentBatteryPercentage;
        private final boolean isInPowerSaveMode;
        private final BatteryState batteryState;
        private final int interactionsInPowerSave;
        private final int performanceThrottles;
        private final int batteryWarnings;
        private final long totalDrainTimeMs;
        private final long sessionDurationMs;
        
        public BatteryStatistics(int currentBatteryPercentage, boolean isInPowerSaveMode,
                                BatteryState batteryState, int interactionsInPowerSave,
                                int performanceThrottles, int batteryWarnings,
                                long totalDrainTimeMs, long sessionDurationMs) {
            this.currentBatteryPercentage = currentBatteryPercentage;
            this.isInPowerSaveMode = isInPowerSaveMode;
            this.batteryState = batteryState;
            this.interactionsInPowerSave = interactionsInPowerSave;
            this.performanceThrottles = performanceThrottles;
            this.batteryWarnings = batteryWarnings;
            this.totalDrainTimeMs = totalDrainTimeMs;
            this.sessionDurationMs = sessionDurationMs;
        }
        
        public int getCurrentBatteryPercentage() {
            return currentBatteryPercentage;
        }
        
        public boolean isInPowerSaveMode() {
            return isInPowerSaveMode;
        }
        
        public BatteryState getBatteryState() {
            return batteryState;
        }
        
        public int getInteractionsInPowerSave() {
            return interactionsInPowerSave;
        }
        
        public int getPerformanceThrottles() {
            return performanceThrottles;
        }
        
        public int getBatteryWarnings() {
            return batteryWarnings;
        }
        
        public long getTotalDrainTimeMs() {
            return totalDrainTimeMs;
        }
        
        public long getSessionDurationMs() {
            return sessionDurationMs;
        }
        
        @Override
        public String toString() {
            return String.format(
                "BatteryStatistics{level=%d%%, state=%s, powerSave=%b, throttles=%d, warnings=%d}",
                currentBatteryPercentage, batteryState.getDisplayName(), 
                isInPowerSaveMode, performanceThrottles, batteryWarnings
            );
        }
    }
}
