package com.cognitive.testing.hooks;

import com.cognitive.testing.model.SocialState;
import com.cognitive.testing.model.SocialConfig;

/**
 * Hook: Group Conformity
 * Implements "Normalization Logic" where the agent adjusts its interaction speed
 * and pathing to match the current "mean" behavior of the simulated community.
 * 
 * Real-world behavior:
 * - Users unconsciously match the speed of those around them
 * - Slow content makes users patient, fast content makes them rush
 * - Community norms influence interaction patterns
 * - Critical for testing load-dependent UI responsiveness
 */
public class GroupConformityHook {
    
    private final SocialState state;
    private int conformityAdjustments;
    private long totalAdjustmentTime;
    private float observedMeanSpeed;
    private float currentSpeed;
    private int observationCount;
    
    public GroupConformityHook(SocialState state) {
        this.state = state;
        this.conformityAdjustments = 0;
        this.totalAdjustmentTime = 0;
        this.observedMeanSpeed = state.getConfig().getDefaultCommunitySpeed();
        this.currentSpeed = state.getConfig().getDefaultCommunitySpeed();
        this.observationCount = 0;
    }
    
    /**
     * Observe community behavior and update mean speed
     * Should be called periodically to sample community state
     * 
     * @param sampleSpeed Speed observed from community
     */
    public void observeCommunitySpeed(float sampleSpeed) {
        observationCount++;
        
        // Running average of observed speeds
        observedMeanSpeed = observedMeanSpeed * 0.9f + sampleSpeed * 0.1f;
        
        // Check if conformity adjustment is needed
        if (shouldAdjustConformity()) {
            applyConformityAdjustment();
        }
    }
    
    /**
     * Check if agent should adjust behavior to conform to community
     * 
     * @return true if conformity adjustment should occur
     */
    public boolean shouldAdjustConformity() {
        float speedDifference = Math.abs(currentSpeed - observedMeanSpeed);
        float threshold = state.getConfig().getConformityThreshold();
        
        return speedDifference > threshold;
    }
    
    /**
     * Apply conformity adjustment to match community behavior
     * Simulates the time it takes for user to adjust their pace
     */
    private void applyConformityAdjustment() {
        conformityAdjustments++;
        
        Random random = state.getRandom();
        
        // Calculate adjustment needed
        float adjustment = (observedMeanSpeed - currentSpeed) * state.getConformityFactor();
        
        // Simulate adjustment time (user is "adapting" to community pace)
        // More extreme adjustments take longer
        int adjustmentTime = (int) (Math.abs(adjustment) * 500);
        adjustmentTime += random.nextInt(500); // Add some randomness
        
        totalAdjustmentTime += adjustmentTime;
        
        // Apply adjustment to current speed
        currentSpeed += adjustment;
        
        // Clamp speed to reasonable bounds
        currentSpeed = Math.max(0.2f, Math.min(3.0f, currentSpeed));
        
        try {
            Thread.sleep(adjustmentTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Get the current interaction speed multiplier
     * 
     * @return Current speed multiplier (1.0 = normal speed)
     */
    public float getCurrentSpeed() {
        return currentSpeed;
    }
    
    /**
     * Get the observed community mean speed
     * 
     * @return Community mean speed
     */
    public float getObservedMeanSpeed() {
        return observedMeanSpeed;
    }
    
    /**
     * Get the current conformity factor
     * 
     * @return Conformity factor (0.0 - 1.0)
     */
    public float getConformityFactor() {
        return state.getConformityFactor();
    }
    
    /**
     * Calculate delay for an action based on current conformity-adjusted speed
     * 
     * @param baseDelayMs Base delay for the action in milliseconds
     * @return Adjusted delay based on community conformity
     */
    public long calculateConformityDelay(long baseDelayMs) {
        // Speed > 1.0 means faster (less delay), Speed < 1.0 means slower (more delay)
        float adjustedDelay = baseDelayMs / currentSpeed;
        
        // Add some randomness to make it less robotic
        float variance = state.getRandom().nextFloat() * 0.2f - 0.1f; // +/- 10%
        adjustedDelay *= (1.0f + variance);
        
        return (long) adjustedDelay;
    }
    
    /**
     * Simulate action timing with conformity adjustment
     * Applies community speed norms to action execution
     * 
     * @param action The action to perform
     * @param baseDelayMs Base delay for the action
     * @param <T> Return type of action
     * @return Result of the action
     */
    public <T> T performWithConformity(Runnable action, long baseDelayMs) {
        long delay = calculateConformityDelay(baseDelayMs);
        
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        action.run();
        
        return null;
    }
    
    /**
     * Calculate pathing adjustment based on community behavior
     * Different community speeds may lead to different navigation paths
     * 
     * @param availablePaths Available navigation paths
     * @param getPathSpeed Function to get estimated speed for a path
     * @param <T> Path type
     * @return Selected path considering conformity
     */
    public <T> T selectPathWithConformity(java.util.List<T> availablePaths,
                                           java.util.function.Function<T, Float> getPathSpeed) {
        if (availablePaths == null || availablePaths.isEmpty()) {
            return null;
        }
        
        // Conformity factor determines how much we prioritize community-matched paths
        float conformity = state.getConformityFactor();
        
        // If low conformity, just pick randomly
        if (conformity < 0.2f) {
            return availablePaths.get(state.getRandom().nextInt(availablePaths.size()));
        }
        
        // Calculate weights based on speed match to community
        float totalWeight = 0;
        java.util.Map<T, Float> weights = new java.util.HashMap<>();
        
        for (T path : availablePaths) {
            float pathSpeed = getPathSpeed.apply(path);
            float speedDiff = Math.abs(pathSpeed - observedMeanSpeed);
            
            // Closer to community mean = higher weight
            float weight = 1.0f / (1.0f + speedDiff * conformity);
            
            weights.put(path, weight);
            totalWeight += weight;
        }
        
        // Weighted random selection
        float randomValue = state.getRandom().nextFloat() * totalWeight;
        float cumulative = 0;
        
        for (T path : availablePaths) {
            cumulative += weights.get(path);
            if (randomValue <= cumulative) {
                return path;
            }
        }
        
        return availablePaths.get(availablePaths.size() - 1);
    }
    
    /**
     * Update the simulated community state
     * Called to simulate changes in community behavior
     * 
     * @param newCommunitySpeed New observed community speed
     */
    public void updateCommunityState(float newCommunitySpeed) {
        observeCommunitySpeed(newCommunitySpeed);
        
        // Update the social state with new mean
        state.adjustConformity(newCommunitySpeed);
    }
    
    /**
     * Simulate observation of community behavior
     * Randomly samples community "mean" behavior
     */
    public void sampleCommunityBehavior() {
        SocialConfig config = state.getConfig();
        
        // Generate a sample around the community mean with variance
        float sample = config.getDefaultCommunitySpeed() + 
            (state.getRandom().nextFloat() - 0.5f) * config.getCommunitySpeedVariance() * 2;
        
        observeCommunitySpeed(sample);
    }
    
    /**
     * Calculate the patience level based on community speed norms
     * Fast community = less patient, Slow community = more patient
     * 
     * @return Patience multiplier (0.5 - 2.0)
     */
    public float calculatePatienceMultiplier() {
        // Inverse relationship: faster community = less patience
        float patience = 1.0f / currentSpeed;
        
        // Clamp to reasonable bounds
        return Math.max(0.5f, Math.min(2.0f, patience));
    }
    
    /**
     * Calculate timeout based on community-adjusted expectations
     * If community is slow, user expects slower responses
     * 
     * @param baseTimeoutMs Base timeout expectation
     * @return Community-adjusted timeout
     */
    public long calculateAdjustedTimeout(long baseTimeoutMs) {
        // Scale timeout based on community speed
        // Fast community = tighter timeouts
        float adjustment = currentSpeed * 0.5f + 0.5f;
        
        return (long) (baseTimeoutMs * adjustment);
    }
    
    /**
     * Get the speed difference from community mean
     * 
     * @return Absolute difference from community mean
     */
    public float getSpeedDifferenceFromMean() {
        return Math.abs(currentSpeed - observedMeanSpeed);
    }
    
    /**
     * Check if current behavior is conforming to community norms
     * 
     * @return true if within acceptable range of community mean
     */
    public boolean isConforming() {
        return !shouldAdjustConformity();
    }
    
    /**
     * Reset conformity state for a new session
     */
    public void resetSession() {
        conformityAdjustments = 0;
        totalAdjustmentTime = 0;
        observedMeanSpeed = state.getConfig().getDefaultCommunitySpeed();
        currentSpeed = state.getConfig().getDefaultCommunitySpeed();
        observationCount = 0;
    }
    
    /**
     * Get statistics for this session
     */
    public ConformityStatistics getStatistics() {
        return new ConformityStatistics(
            conformityAdjustments,
            totalAdjustmentTime,
            currentSpeed,
            observedMeanSpeed,
            getSpeedDifferenceFromMean(),
            state.getConformityFactor(),
            observationCount
        );
    }
    
    /**
     * Statistics container for group conformity behavior
     */
    public static class ConformityStatistics {
        public final int adjustmentCount;
        public final long totalAdjustmentTimeMs;
        public final float currentSpeed;
        public final float observedMeanSpeed;
        public final float speedDifference;
        public final float conformityFactor;
        public final int observationCount;
        
        public ConformityStatistics(int adjustmentCount, long totalAdjustmentTimeMs,
                                   float currentSpeed, float observedMeanSpeed,
                                   float speedDifference, float conformityFactor,
                                   int observationCount) {
            this.adjustmentCount = adjustmentCount;
            this.totalAdjustmentTimeMs = totalAdjustmentTimeMs;
            this.currentSpeed = currentSpeed;
            this.observedMeanSpeed = observedMeanSpeed;
            this.speedDifference = speedDifference;
            this.conformityFactor = conformityFactor;
            this.observationCount = observationCount;
        }
        
        @Override
        public String toString() {
            return String.format("ConformityStatistics{adjustments=%d, adjustTime=%dms, " +
                "currentSpeed=%.2f, communityMean=%.2f, diff=%.2f, conformity=%.1f%%, observations=%d}",
                adjustmentCount, totalAdjustmentTimeMs, currentSpeed, observedMeanSpeed,
                speedDifference, conformityFactor * 100, observationCount);
        }
    }
}
