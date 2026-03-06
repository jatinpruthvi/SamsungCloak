package com.cognitive.testing.model;

/**
 * Configuration parameters for cognitive realism testing.
 * All values are tunable for different test scenarios.
 */
public class CognitiveConfig {
    
    // Random seed for reproducibility (-1 for random)
    private final long randomSeed;
    
    // Base error rate (0.0 - 1.0)
    private final float baseErrorRate;
    
    // Max error rate cap (0.0 - 1.0)
    private final float maxErrorRate;
    
    // Multipliers for cognitive factors
    private final float fatigueErrorMultiplier;
    private final float attentionErrorMultiplier;
    private final float stressErrorMultiplier;
    
    // Thresholds and probabilities
    private final float contextSwitchThreshold;      // Attention level below which context switching may occur
    private final float contextSwitchProbability;      // Probability of context switching when threshold met
    private final float memoryThreshold;               // Attention deficit level for memory issues
    private final float reverificationProbability;     // Probability of re-verification when memory issues
    private final float satisficingThreshold;          // Probability of choosing first acceptable option
    private final float interactionBurstProbability;   // Probability of emotional interaction bursts
    private final float navigationHesitationProbability; // Probability of navigation hesitation
    
    // Defaults
    private final NavigationPreference defaultNavPreference;
    private final InteractionStyle defaultInteractionStyle;
    
    // Timing parameters (milliseconds)
    private final int minThinkTime;
    private final int maxThinkTime;
    private final int fatigueSlowdownMultiplier;       // Response time slows by this factor at max fatigue
    
    private CognitiveConfig(Builder builder) {
        this.randomSeed = builder.randomSeed;
        this.baseErrorRate = builder.baseErrorRate;
        this.maxErrorRate = builder.maxErrorRate;
        this.fatigueErrorMultiplier = builder.fatigueErrorMultiplier;
        this.attentionErrorMultiplier = builder.attentionErrorMultiplier;
        this.stressErrorMultiplier = builder.stressErrorMultiplier;
        this.contextSwitchThreshold = builder.contextSwitchThreshold;
        this.contextSwitchProbability = builder.contextSwitchProbability;
        this.memoryThreshold = builder.memoryThreshold;
        this.reverificationProbability = builder.reverificationProbability;
        this.satisficingThreshold = builder.satisficingThreshold;
        this.interactionBurstProbability = builder.interactionBurstProbability;
        this.navigationHesitationProbability = builder.navigationHesitationProbability;
        this.defaultNavPreference = builder.defaultNavPreference;
        this.defaultInteractionStyle = builder.defaultInteractionStyle;
        this.minThinkTime = builder.minThinkTime;
        this.maxThinkTime = builder.maxThinkTime;
        this.fatigueSlowdownMultiplier = builder.fatigueSlowdownMultiplier;
    }
    
    // Static factory for default configuration
    public static CognitiveConfig defaults() {
        return new Builder().build();
    }
    
    // Static factory for high-fidelity simulation (more realistic)
    public static CognitiveConfig highFidelity() {
        return new Builder()
                .baseErrorRate(0.05f)
                .maxErrorRate(0.25f)
                .fatigueErrorMultiplier(0.05f)
                .attentionErrorMultiplier(0.15f)
                .stressErrorMultiplier(0.10f)
                .contextSwitchThreshold(0.5f)
                .contextSwitchProbability(0.15f)
                .memoryThreshold(0.4f)
                .reverificationProbability(0.20f)
                .satisficingThreshold(0.60f)
                .interactionBurstProbability(0.08f)
                .navigationHesitationProbability(0.12f)
                .minThinkTime(300)
                .maxThinkTime(2000)
                .fatigueSlowdownMultiplier(3)
                .build();
    }
    
    // Static factory for low-fidelity simulation (fewer errors)
    public static CognitiveConfig lowFidelity() {
        return new Builder()
                .baseErrorRate(0.02f)
                .maxErrorRate(0.10f)
                .fatigueErrorMultiplier(0.02f)
                .attentionErrorMultiplier(0.05f)
                .stressErrorMultiplier(0.03f)
                .contextSwitchThreshold(0.3f)
                .contextSwitchProbability(0.05f)
                .memoryThreshold(0.6f)
                .reverificationProbability(0.08f)
                .satisficingThreshold(0.80f)
                .interactionBurstProbability(0.03f)
                .navigationHesitationProbability(0.05f)
                .minThinkTime(200)
                .maxThinkTime(1000)
                .fatigueSlowdownMultiplier(2)
                .build();
    }
    
    // Getters
    public long getRandomSeed() { return randomSeed; }
    public float getBaseErrorRate() { return baseErrorRate; }
    public float getMaxErrorRate() { return maxErrorRate; }
    public float getFatigueErrorMultiplier() { return fatigueErrorMultiplier; }
    public float getAttentionErrorMultiplier() { return attentionErrorMultiplier; }
    public float getStressErrorMultiplier() { return stressErrorMultiplier; }
    public float getContextSwitchThreshold() { return contextSwitchThreshold; }
    public float getContextSwitchProbability() { return contextSwitchProbability; }
    public float getMemoryThreshold() { return memoryThreshold; }
    public float getReverificationProbability() { return reverificationProbability; }
    public float getSatisficingThreshold() { return satisficingThreshold; }
    public float getInteractionBurstProbability() { return interactionBurstProbability; }
    public float getNavigationHesitationProbability() { return navigationHesitationProbability; }
    public CognitiveState.NavigationPreference getDefaultNavPreference() { return defaultNavPreference; }
    public CognitiveState.InteractionStyle getDefaultInteractionStyle() { return defaultInteractionStyle; }
    public int getMinThinkTime() { return minThinkTime; }
    public int getMaxThinkTime() { return maxThinkTime; }
    public int getFatigueSlowdownMultiplier() { return fatigueSlowdownMultiplier; }
    
    /**
     * Builder for flexible configuration
     */
    public static class Builder {
        private long randomSeed = -1;
        private float baseErrorRate = 0.03f;
        private float maxErrorRate = 0.15f;
        private float fatigueErrorMultiplier = 0.03f;
        private float attentionErrorMultiplier = 0.10f;
        private float stressErrorMultiplier = 0.05f;
        private float contextSwitchThreshold = 0.4f;
        private float contextSwitchProbability = 0.10f;
        private float memoryThreshold = 0.5f;
        private float reverificationProbability = 0.15f;
        private float satisficingThreshold = 0.70f;
        private float interactionBurstProbability = 0.05f;
        private float navigationHesitationProbability = 0.08f;
        private NavigationPreference defaultNavPreference = NavigationPreference.BALANCED;
        private InteractionStyle defaultInteractionStyle = InteractionStyle.CASUAL;
        private int minThinkTime = 250;
        private int maxThinkTime = 1500;
        private int fatigueSlowdownMultiplier = 2;
        
        public Builder randomSeed(long seed) {
            this.randomSeed = seed;
            return this;
        }
        
        public Builder baseErrorRate(float rate) {
            this.baseErrorRate = rate;
            return this;
        }
        
        public Builder maxErrorRate(float rate) {
            this.maxErrorRate = rate;
            return this;
        }
        
        public Builder fatigueErrorMultiplier(float mult) {
            this.fatigueErrorMultiplier = mult;
            return this;
        }
        
        public Builder attentionErrorMultiplier(float mult) {
            this.attentionErrorMultiplier = mult;
            return this;
        }
        
        public Builder stressErrorMultiplier(float mult) {
            this.stressErrorMultiplier = mult;
            return this;
        }
        
        public Builder contextSwitchThreshold(float threshold) {
            this.contextSwitchThreshold = threshold;
            return this;
        }
        
        public Builder contextSwitchProbability(float prob) {
            this.contextSwitchProbability = prob;
            return this;
        }
        
        public Builder memoryThreshold(float threshold) {
            this.memoryThreshold = threshold;
            return this;
        }
        
        public Builder reverificationProbability(float prob) {
            this.reverificationProbability = prob;
            return this;
        }
        
        public Builder satisficingThreshold(float threshold) {
            this.satisficingThreshold = threshold;
            return this;
        }
        
        public Builder interactionBurstProbability(float prob) {
            this.interactionBurstProbability = prob;
            return this;
        }
        
        public Builder navigationHesitationProbability(float prob) {
            this.navigationHesitationProbability = prob;
            return this;
        }
        
        public Builder defaultNavPreference(NavigationPreference pref) {
            this.defaultNavPreference = pref;
            return this;
        }
        
        public Builder defaultInteractionStyle(InteractionStyle style) {
            this.defaultInteractionStyle = style;
            return this;
        }
        
        public Builder minThinkTime(int ms) {
            this.minThinkTime = ms;
            return this;
        }
        
        public Builder maxThinkTime(int ms) {
            this.maxThinkTime = ms;
            return this;
        }
        
        public Builder fatigueSlowdownMultiplier(int mult) {
            this.fatigueSlowdownMultiplier = mult;
            return this;
        }
        
        public CognitiveConfig build() {
            return new CognitiveConfig(this);
        }
        
        // Enums for inner reference
        private enum NavigationPreference {
            GESTURE_HEAVY, BUTTON_HEAVY, BALANCED
        }
        
        private enum InteractionStyle {
            PRECISE, CASUAL, ERRATIC
        }
    }
}
