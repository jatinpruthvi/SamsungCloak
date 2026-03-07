package com.cognitive.testing.hooks;

import com.cognitive.testing.model.SocialState;
import com.cognitive.testing.model.SocialConfig;

/**
 * Hook: Herd Behavior Simulation
 * Simulates the "Follower Effect" where a single action increases the probability
 * of subsequent agents performing the same action.
 * 
 * Real-world behavior:
 * - Users are more likely to engage with content that already has engagement
 * - A single like/comment can trigger a cascade of similar actions
 * - Social validation creates momentum in user behavior
 * - This is critical for testing traffic surge scenarios
 */
public class HerdBehaviorHook {
    
    private final SocialState state;
    private int triggerCount;
    private int followerCount;
    private long totalHerdDelay;
    private long sessionStartTime;
    
    // Action types to track for herd effect
    public enum ActionType {
        LIKE("like"),
        COMMENT("comment"),
        SHARE("share"),
        VIEW("view"),
        FOLLOW("follow"),
        REACT("react"),
        SAVE("save"),
        CLICK("click");
        
        private final String value;
        
        ActionType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    public HerdBehaviorHook(SocialState state) {
        this.state = state;
        this.triggerCount = 0;
        this.followerCount = 0;
        this.totalHerdDelay = 0;
        this.sessionStartTime = System.currentTimeMillis();
    }
    
    /**
     * Check if herd effect should be triggered after observing a trigger action
     * 
     * @param triggerAction The initial action that triggered the herd
     * @return true if herd effect should influence next action decision
     */
    public boolean shouldTriggerHerdEffect(ActionType triggerAction) {
        state.updateSocialState();
        
        // Get recent count of this action type
        int recentCount = state.getRecentActionCount(triggerAction.getValue());
        
        // Herd effect probability increases with:
        // 1. Base herd influence level
        // 2. Number of recent similar actions
        // 3. Social vulnerability factor
        float herdProbability = calculateHerdProbability(recentCount);
        
        boolean willTrigger = state.getRandom().nextFloat() < herdProbability;
        
        if (willTrigger) {
            triggerCount++;
            followerCount++;
            
            // Record this as a validated action
            state.recordAction(triggerAction.getValue());
            state.recordAction("validated");
        }
        
        return willTrigger;
    }
    
    /**
     * Calculate the probability of herd effect based on current state
     * 
     * @param recentCount Number of recent similar actions
     * @return Probability of herd effect (0.0 - 1.0)
     */
    private float calculateHerdProbability(int recentCount) {
        SocialConfig config = state.getConfig();
        
        // Base probability from config
        float baseProbability = config.getBaseHerdInfluence();
        
        // Amplification based on recent action count
        float amplification = 0.0f;
        if (recentCount >= config.getHerdThreshold()) {
            // Logarithmic amplification to prevent runaway effects
            amplification = (float) (Math.log(recentCount - config.getHerdThreshold() + 1) * 
                config.getHerdAmplificationFactor());
            amplification = Math.min(0.8f, amplification); // Cap at 80%
        }
        
        // Apply time decay
        float timeDecay = calculateTimeDecay();
        
        // Combine factors
        float totalProbability = baseProbability + amplification * timeDecay;
        
        return Math.min(1.0f, totalProbability);
    }
    
    /**
     * Calculate time-based decay for herd effect
     * Herd effect is strongest immediately after trigger, then decays
     */
    private float calculateTimeDecay() {
        long elapsed = System.currentTimeMillis() - sessionStartTime;
        SocialConfig config = state.getConfig();
        
        // Exponential decay over time
        float decay = (float) Math.exp(-config.getHerdDecayRate() * (elapsed / 60000.0));
        
        return Math.max(0.1f, decay); // Minimum 10% effect
    }
    
    /**
     * Simulate the delay caused by herd behavior
     * Users may pause to observe before following the herd
     * 
     * @return Delay in milliseconds before performing herd action
     */
    public long simulateHerdDelay() {
        Random random = state.getRandom();
        
        // Delay based on herd influence level
        // Higher influence = shorter delay (more compelled to follow)
        float influence = state.getHerdInfluenceLevel();
        int baseDelay = (int) ((1.0f - influence) * 2000); // 0-2000ms
        
        // Add randomness
        int delay = baseDelay + random.nextInt(1000);
        
        totalHerdDelay += delay;
        
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return delay;
    }
    
    /**
     * Apply herd effect to a list of options
     * Options with higher existing engagement get boosted priority
     * 
     * @param options List of options with engagement counts
     * @param <T> Option type
     * @return Selected option considering herd effect
     */
    public <T> T applyHerdEffect(java.util.List<T> options, 
                                  java.util.function.Function<T, Long> getEngagementCount) {
        if (options == null || options.isEmpty()) {
            return null;
        }
        
        // If herd influence is low, randomly select
        if (state.getHerdInfluenceLevel() < 0.2f) {
            return options.get(state.getRandom().nextInt(options.size()));
        }
        
        // Calculate weights based on engagement + herd boost
        float totalWeight = 0;
        java.util.Map<T, Float> weights = new java.util.HashMap<>();
        
        for (T option : options) {
            long engagement = getEngagementCount.apply(option);
            
            // Apply logarithmic boost for engagement
            float engagementWeight = (float) Math.log(engagement + 1);
            
            // Apply herd influence amplification
            float herdBoost = 1.0f + state.getHerdInfluenceLevel() * 
                state.getConfig().getHerdAmplificationFactor();
            
            float weight = engagementWeight * herdBoost;
            weights.put(option, weight);
            totalWeight += weight;
        }
        
        // Weighted random selection
        float randomValue = state.getRandom().nextFloat() * totalWeight;
        float cumulative = 0;
        
        for (T option : options) {
            cumulative += weights.get(option);
            if (randomValue <= cumulative) {
                return option;
            }
        }
        
        // Fallback to last option
        return options.get(options.size() - 1);
    }
    
    /**
     * Get the current herd influence level
     * 
     * @return Current herd influence (0.0 - 1.0)
     */
    public float getCurrentHerdInfluence() {
        return state.getHerdInfluenceLevel();
    }
    
    /**
     * Get the amplification factor for herd behavior
     * 
     * @return Current amplification factor
     */
    public float getAmplificationFactor() {
        return state.getConfig().getHerdAmplificationFactor();
    }
    
    /**
     * Check if a specific action should be performed due to herd effect
     * 
     * @param actionType The type of action to potentially perform
     * @return true if the action should be performed
     */
    public boolean shouldFollowHerd(ActionType actionType) {
        return shouldTriggerHerdEffect(actionType);
    }
    
    /**
     * Reset herd behavior tracking for a new session
     */
    public void resetSession() {
        triggerCount = 0;
        followerCount = 0;
        totalHerdDelay = 0;
        sessionStartTime = System.currentTimeMillis();
    }
    
    /**
     * Get statistics for this session
     */
    public HerdStatistics getStatistics() {
        return new HerdStatistics(
            triggerCount,
            followerCount,
            totalHerdDelay,
            state.getHerdInfluenceLevel(),
            calculateTimeDecay()
        );
    }
    
    /**
     * Statistics container for herd behavior
     */
    public static class HerdStatistics {
        public final int triggerCount;
        public final int followerCount;
        public final long totalHerdDelayMs;
        public final float currentHerdInfluence;
        public final float timeDecayFactor;
        
        public HerdStatistics(int triggerCount, int followerCount, long totalHerdDelayMs,
                             float currentHerdInfluence, float timeDecayFactor) {
            this.triggerCount = triggerCount;
            this.followerCount = followerCount;
            this.totalHerdDelayMs = totalHerdDelayMs;
            this.currentHerdInfluence = currentHerdInfluence;
            this.timeDecayFactor = timeDecayFactor;
        }
        
        @Override
        public String toString() {
            return String.format("HerdStatistics{triggers=%d, followers=%d, delayTotal=%dms, influence=%.2f%%, decay=%.2f}",
                triggerCount, followerCount, totalHerdDelayMs, 
                currentHerdInfluence * 100, timeDecayFactor);
        }
    }
}
