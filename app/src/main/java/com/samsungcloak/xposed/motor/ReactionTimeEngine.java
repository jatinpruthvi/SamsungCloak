package com.samsungcloak.xposed.motor;

import com.samsungcloak.xposed.HookUtils;

import java.util.Random;

/**
 * Hook 5: Reaction Time Engine
 * 
 * Stochastic simulation of "Perceptual Delay" - the variable gap between
 * a UI element appearing and the subsequent user interaction.
 * 
 * Human reaction time characteristics:
 * - Base reaction time: ~200-250ms for visual stimuli
 * - High variability (sigma ~50ms)
 * - Task complexity increases delay significantly
 * - Fatigue reduces response speed
 * - Attention state affects timing
 * - Practice/learning reduces delay over time
 * 
 * Distribution characteristics:
 * - Log-normal fits empirical data better than normal
 * - Accounts for the long tail of slow responses
 * - Cannot be negative
 */
public class ReactionTimeEngine {
    private final Random random;
    private final NeuromotorConfig config;
    
    // State tracking
    private int interactionCount = 0;
    private long sessionStartTime = 0;
    private double cumulativeFatigue = 0.0;

    public ReactionTimeEngine(NeuromotorConfig config) {
        this.config = config;
        this.random = new Random();
    }

    public void updateConfig(NeuromotorConfig config) {
        this.config = config;
    }

    /**
     * Get reaction delay for UI interaction
     * 
     * Uses log-normal distribution to simulate realistic human response times.
     * The distribution is modified based on task complexity and user fatigue.
     * 
     * @param isComplexTask Whether the task requires cognitive processing
     * @param isTired Whether the user is in a fatigue state
     * @return Delay in milliseconds
     */
    public long getReactionDelay(boolean isComplexTask, boolean isTired) {
        // Start with base reaction time
        double mean = config.getBaseReactionTimeMs();
        double stdDev = config.getReactionTimeStdDevMs();
        
        // Apply modifiers
        if (isComplexTask) {
            mean *= config.getComplexTaskMultiplier();
            stdDev *= 1.2; // More variability in complex tasks
        }
        
        if (isTired) {
            mean *= config.getFatigueMultiplier();
            stdDev *= 1.3; // More variable when tired
        }
        
        // Session fatigue accumulates over time
        if (sessionStartTime > 0) {
            long sessionDuration = System.currentTimeMillis() - sessionStartTime;
            double fatigueFactor = 1.0 + (sessionDuration / 60000.0) * 0.1; // 10% slower per minute
            mean *= fatigueFactor;
            cumulativeFatigue += sessionDuration / 1000.0;
        }
        
        // Generate log-normal distributed delay
        long delay = generateLogNormalDelay(mean, stdDev);
        
        // Clamp to bounds
        delay = clampDelay(delay);
        
        // Track interactions
        interactionCount++;
        
        HookUtils.logDebug("Reaction delay: " + delay + "ms (complex=" + isComplexTask + 
                          ", tired=" + isTired + ", count=" + interactionCount + ")");
        
        return delay;
    }

    /**
     * Get perceptual delay for UI element appearing
     * 
     * This is the delay between an element becoming visible/interactive
     * and the user's first perception leading to potential interaction.
     * 
     * @return Delay in milliseconds
     */
    public long getPerceptualDelay() {
        // Perceptual delay is typically faster than full reaction
        double baseMean = config.getBaseReactionTimeMs() * 0.6; // ~132ms base
        double stdDev = config.getReactionTimeStdDevMs() * 0.7;
        
        // Add visual attention delay
        baseMean += config.getVisualAttentionDelayMs();
        
        long delay = generateLogNormalDelay(baseMean, stdDev);
        
        // Add context-specific variation
        // Moving/animating elements are noticed faster
        if (random.nextDouble() < 0.3) {
            delay *= 0.8; // 20% faster for salient stimuli
        }
        
        return clampDelay(delay);
    }

    /**
     * Get cognitive processing delay based on task complexity
     * 
     * Complexity levels:
     * 0 - Simple tap (< 200ms additional)
     * 1 - Simple decision (200-400ms)
     * 2 - Reading comprehension (400-700ms)
     * 3 - Problem solving (700-1200ms)
     * 4 - Complex reasoning (> 1200ms)
     * 
     * @param complexityLevel 0-4 complexity scale
     * @return Additional delay in milliseconds
     */
    public long getCognitiveProcessingDelay(int complexityLevel) {
        // Base cognitive delays for each level (in ms)
        double[] baseDelays = {50, 250, 550, 950, 1450};
        double[] stdDevs = {30, 60, 100, 150, 200};
        
        if (complexityLevel < 0) complexityLevel = 0;
        if (complexityLevel > 4) complexityLevel = 4;
        
        double mean = baseDelays[complexityLevel];
        double stdDev = stdDevs[complexityLevel];
        
        // Add variation based on user state
        double fatigueModifier = 1.0 + (cumulativeFatigue / 600.0); // Fatigue accumulates
        mean *= fatigueModifier;
        
        long delay = generateLogNormalDelay(mean, stdDev);
        
        // Add occasional "hesitation" for more realism
        if (random.nextDouble() < 0.15) {
            delay += 100 + random.nextInt(200); // Add 100-300ms hesitation
        }
        
        return Math.max(0, delay);
    }

    /**
     * Generate log-normally distributed delay
     * 
     * Log-normal parameters derived from mean and stdDev:
     * mu = ln(mean^2 / sqrt(mean^2 + stdDev^2))
     * sigma = sqrt(ln(1 + stdDev^2 / mean^2))
     */
    private long generateLogNormalDelay(double mean, double stdDev) {
        // Calculate log-normal parameters
        double variance = stdDev * stdDev;
        double mu = Math.log((mean * mean) / Math.sqrt(mean * mean + variance));
        double sigma = Math.sqrt(Math.log(1 + variance / (mean * mean)));
        
        // Generate sample
        double sample = Math.exp(mu + sigma * random.nextGaussian());
        
        return Math.round(sample);
    }

    /**
     * Clamp delay to configured min/max bounds
     */
    private long clampDelay(long delay) {
        return Math.max((long) config.getMinReactionTimeMs(),
                       Math.min((long) config.getMaxReactionTimeMs(), delay));
    }

    /**
     * Start a new interaction session
     */
    public void startSession() {
        sessionStartTime = System.currentTimeMillis();
        interactionCount = 0;
        cumulativeFatigue = 0.0;
        
        HookUtils.logDebug("Reaction time session started");
    }

    /**
     * End current session and calculate stats
     */
    public SessionStats endSession() {
        long duration = sessionStartTime > 0 
            ? System.currentTimeMillis() - sessionStartTime 
            : 0;
        
        double avgDelay = cumulativeFatigue > 0 && interactionCount > 0
            ? cumulativeFatigue * 1000 / interactionCount
            : 0; // This is a simplification
        
        SessionStats stats = new SessionStats(
            interactionCount,
            duration,
            cumulativeFatigue
        );
        
        HookUtils.logDebug("Session ended: " + stats.toString());
        
        sessionStartTime = 0;
        return stats;
    }

    /**
     * Reset all state
     */
    public void reset() {
        interactionCount = 0;
        sessionStartTime = 0;
        cumulativeFatigue = 0.0;
    }

    /**
     * Calculate reaction time distribution for testing purposes
     * 
     * @param sampleCount Number of samples to generate
     * @return Array of reaction times in milliseconds
     */
    public long[] getDistributionSample(int sampleCount, boolean isComplexTask, boolean isTired) {
        long[] samples = new long[sampleCount];
        
        for (int i = 0; i < sampleCount; i++) {
            samples[i] = getReactionDelay(isComplexTask, isTired);
        }
        
        return samples;
    }

    /**
     * Get percentile value from distribution
     * Useful for testing worst-case scenarios
     * 
     * @param percentile 0-100
     * @return Delay at given percentile
     */
    public long getPercentile(int percentile, boolean isComplexTask, boolean isTired) {
        long[] samples = getDistributionSample(1000, isComplexTask, isTired);
        
        // Simple sorting to find percentile
        java.util.Arrays.sort(samples);
        
        int index = (int) Math.ceil(percentile / 100.0 * samples.length) - 1;
        return samples[Math.max(0, index)];
    }

    /**
     * Session statistics
     */
    public static class SessionStats {
        public final int interactionCount;
        public final long durationMs;
        public final double cumulativeFatigue;

        public SessionStats(int interactionCount, long durationMs, double cumulativeFatigue) {
            this.interactionCount = interactionCount;
            this.durationMs = durationMs;
            this.cumulativeFatigue = cumulativeFatigue;
        }

        @Override
        public String toString() {
            return "SessionStats{count=" + interactionCount + 
                   ", duration=" + durationMs + "ms" +
                   ", fatigue=" + cumulativeFatigue + "}";
        }
    }
}
