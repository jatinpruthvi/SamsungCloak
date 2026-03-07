package com.samsungcloak.xposed.motor;

import com.samsungcloak.xposed.HookUtils;

import java.util.Random;

/**
 * Hook 1: Typing Cadence Engine
 * 
 * Simulates varying delays between keystrokes using log-normal distribution.
 * Includes "burst" typing (rapid consecutive keys) and "backspace-correction"
 * events to reflect natural typing inconsistencies.
 * 
 * Human typing characteristics:
 * - Non-uniform inter-key timing (not metronomic)
 * - Occasional rapid bursts during flow states
 * - Correction pauses after errors
 * - Fatigue-related slowing over time
 */
public class TypingCadenceEngine {
    private final Random random;
    private final NeuromotorConfig config;
    
    // Burst tracking
    private int consecutiveKeyCount = 0;
    private boolean inBurstMode = false;
    private long lastKeyTime = 0;
    
    // Correction tracking
    private int errorCount = 0;
    private boolean awaitingCorrection = false;

    public TypingCadenceEngine(NeuromotorConfig config) {
        this.config = config;
        this.random = new Random();
    }

    public void updateConfig(NeuromotorConfig config) {
        this.config = config;
    }

    /**
     * Generate inter-key latency using log-normal distribution
     * 
     * Log-normal is appropriate because:
     * - Human reaction times are positively skewed
     * - Cannot be negative (unlike normal distribution)
     * - Captures the "occasional long delay" phenomenon
     * 
     * @param isBurst Whether this is during a burst typing sequence
     * @param isCorrection Whether this is a correction (backspace) key
     * @return Delay in milliseconds
     */
    public long getInterKeyLatency(boolean isBurst, boolean isCorrection) {
        double mean = isBurst ? config.getBurstMeanMs() : config.getTypingMeanMs();
        double stdDev = config.getTypingStdDevMs();
        
        // Convert to log-normal parameters
        // mu = ln(mean^2 / sqrt(mean^2 + stdDev^2))
        // sigma = sqrt(ln(1 + stdDev^2 / mean^2))
        double mu = Math.log((mean * mean) / Math.sqrt(mean * mean + stdDev * stdDev));
        double sigma = Math.sqrt(Math.log(1 + (stdDev * stdDev) / (mean * mean)));
        
        // Generate log-normal sample
        double logNormalSample = Math.exp(mu + sigma * random.nextGaussian());
        
        // Clamp to reasonable bounds
        long latency = (long) Math.max(20, Math.min(500, logNormalSample));
        
        // Add correction delay if needed
        if (isCorrection) {
            latency += config.getCorrectionDelayMs();
        }
        
        // Track consecutive keys
        long now = System.currentTimeMillis();
        if (lastKeyTime > 0 && (now - lastKeyTime) < 200) {
            consecutiveKeyCount++;
        } else {
            consecutiveKeyCount = 0;
        }
        lastKeyTime = now;
        
        // Determine if we're in burst mode
        if (consecutiveKeyCount > 3) {
            inBurstMode = true;
        } else if (consecutiveKeyCount == 0) {
            inBurstMode = false;
        }
        
        HookUtils.logDebug("InterKeyLatency: " + latency + "ms (burst=" + isBurst + ", correction=" + isCorrection + ")");
        
        return latency;
    }

    /**
     * Determine if a burst should be triggered
     * Burst typing occurs when the user is "in the flow" and types rapidly
     */
    public boolean shouldTriggerBurst() {
        // Probability increases with consecutive keys, then resets
        double burstChance = config.getBurstProbability();
        
        // Increase chance if already typing rapidly
        if (consecutiveKeyCount > 2) {
            burstChance *= 1.5;
        }
        
        boolean trigger = random.nextDouble() < burstChance;
        
        if (trigger) {
            HookUtils.logDebug("Burst typing triggered");
        }
        
        return trigger;
    }

    /**
     * Determine if a backspace correction should occur
     * Users make typos and need to correct them
     */
    public boolean shouldTriggerCorrection() {
        // Base probability
        double correctionChance = config.getCorrectionProbability();
        
        // Increase chance during burst typing (more errors)
        if (inBurstMode) {
            correctionChance *= 2.0;
        }
        
        boolean trigger = random.nextDouble() < correctionChance;
        
        if (trigger) {
            awaitingCorrection = true;
            errorCount++;
            HookUtils.logDebug("Backspace correction triggered (error #" + errorCount + ")");
        }
        
        return trigger;
    }

    /**
     * Get typing speed in WPM (words per minute)
     * Useful for adjusting difficulty based on user skill level
     */
    public double getTypingSpeedWPM(int characterCount, long totalTimeMs) {
        if (totalTimeMs <= 0) return 0;
        
        // Assume average word length of 5 characters
        double words = characterCount / 5.0;
        double minutes = totalTimeMs / 60000.0;
        
        return words / minutes;
    }

    /**
     * Get the current typing state for tracking
     */
    public TypingState getCurrentState() {
        return new TypingState(
            consecutiveKeyCount,
            inBurstMode,
            awaitingCorrection,
            errorCount
        );
    }

    /**
     * Reset state for new typing session
     */
    public void reset() {
        consecutiveKeyCount = 0;
        inBurstMode = false;
        lastKeyTime = 0;
        awaitingCorrection = false;
        errorCount = 0;
    }

    /**
     * Mutable typing state holder
     */
    public static class TypingState {
        public final int consecutiveKeyCount;
        public final boolean inBurstMode;
        public final boolean awaitingCorrection;
        public final int errorCount;

        public TypingState(int consecutiveKeyCount, boolean inBurstMode, 
                          boolean awaitingCorrection, int errorCount) {
            this.consecutiveKeyCount = consecutiveKeyCount;
            this.inBurstMode = inBurstMode;
            this.awaitingCorrection = awaitingCorrection;
            this.errorCount = errorCount;
        }
    }
}
