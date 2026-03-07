package com.cognitive.testing.model;

/**
 * Configuration for social realism simulation.
 * Defines parameters for herd behavior, social proof, trends, and peer influence.
 */
public class SocialConfig {
    
    // Herd Behavior parameters
    private final float baseHerdInfluence;
    private final float herdAmplificationFactor;
    private final int herdThreshold;
    private final float herdDecayRate;
    
    // Social Proof parameters
    private final float baseSocialProofSusceptibility;
    private final float validationBoostFactor;
    private final float socialProofThreshold;
    private final float highCountBias;
    
    // Trend Participation parameters
    private final float viralSurgeProbability;
    private final float trendDurationHours;
    private final float trendFocusRatio;
    private final String[] trendCategories;
    
    // Group Conformity parameters
    private final float baseConformityFactor;
    private final float conformityThreshold;
    private final float conformityAdjustmentRate;
    private final float defaultCommunitySpeed;
    private final float communitySpeedVariance;
    
    // Peer Influence parameters
    private final float basePeerInfluenceWeight;
    private final float peerActionObservationWeight;
    private final float influenceDecayRate;
    
    // Random seed for reproducibility
    private final long randomSeed;
    
    private SocialConfig(Builder builder) {
        this.baseHerdInfluence = builder.baseHerdInfluence;
        this.herdAmplificationFactor = builder.herdAmplificationFactor;
        this.herdThreshold = builder.herdThreshold;
        this.herdDecayRate = builder.herdDecayRate;
        
        this.baseSocialProofSusceptibility = builder.baseSocialProofSusceptibility;
        this.validationBoostFactor = builder.validationBoostFactor;
        this.socialProofThreshold = builder.socialProofThreshold;
        this.highCountBias = builder.highCountBias;
        
        this.viralSurgeProbability = builder.viralSurgeProbability;
        this.trendDurationHours = builder.trendDurationHours;
        this.trendFocusRatio = builder.trendFocusRatio;
        this.trendCategories = builder.trendCategories;
        
        this.baseConformityFactor = builder.baseConformityFactor;
        this.conformityThreshold = builder.conformityThreshold;
        this.conformityAdjustmentRate = builder.conformityAdjustmentRate;
        this.defaultCommunitySpeed = builder.defaultCommunitySpeed;
        this.communitySpeedVariance = builder.communitySpeedVariance;
        
        this.basePeerInfluenceWeight = builder.basePeerInfluenceWeight;
        this.peerActionObservationWeight = builder.peerActionObservationWeight;
        this.influenceDecayRate = builder.influenceDecayRate;
        
        this.randomSeed = builder.randomSeed;
    }
    
    // Herd Behavior getters
    public float getBaseHerdInfluence() { return baseHerdInfluence; }
    public float getHerdAmplificationFactor() { return herdAmplificationFactor; }
    public int getHerdThreshold() { return herdThreshold; }
    public float getHerdDecayRate() { return herdDecayRate; }
    
    // Social Proof getters
    public float getBaseSocialProofSusceptibility() { return baseSocialProofSusceptibility; }
    public float getValidationBoostFactor() { return validationBoostFactor; }
    public float getSocialProofThreshold() { return socialProofThreshold; }
    public float getHighCountBias() { return highCountBias; }
    
    // Trend Participation getters
    public float getViralSurgeProbability() { return viralSurgeProbability; }
    public long getTrendDurationMs() { return (long) (trendDurationHours * 3600 * 1000); }
    public float getTrendDurationHours() { return trendDurationHours; }
    public float getTrendFocusRatio() { return trendFocusRatio; }
    public String[] getTrendCategories() { return trendCategories; }
    
    // Group Conformity getters
    public float getBaseConformityFactor() { return baseConformityFactor; }
    public float getConformityThreshold() { return conformityThreshold; }
    public float getConformityAdjustmentRate() { return conformityAdjustmentRate; }
    public float getDefaultCommunitySpeed() { return defaultCommunitySpeed; }
    public float getCommunitySpeedVariance() { return communitySpeedVariance; }
    
    // Peer Influence getters
    public float getBasePeerInfluenceWeight() { return basePeerInfluenceWeight; }
    public float getPeerActionObservationWeight() { return peerActionObservationWeight; }
    public float getInfluenceDecayRate() { return influenceDecayRate; }
    
    public long getRandomSeed() { return randomSeed; }
    
    /**
     * Default configuration for social realism testing
     */
    public static SocialConfig defaults() {
        return builder().build();
    }
    
    /**
     * High fidelity configuration for detailed social simulation
     */
    public static SocialConfig highFidelity() {
        return builder()
            .baseHerdInfluence(0.6f)
            .herdAmplificationFactor(2.5f)
            .herdThreshold(5)
            .herdDecayRate(0.1f)
            .baseSocialProofSusceptibility(0.7f)
            .validationBoostFactor(0.3f)
            .socialProofThreshold(1000)
            .highCountBias(0.8f)
            .viralSurgeProbability(0.02f)
            .trendDurationHours(24)
            .trendFocusRatio(0.8f)
            .trendCategories(new String[]{"viral_video", "trending_music", "popular_meme", 
                "breaking_news", "fashion_trend", "food_trend", "travel_destination"})
            .baseConformityFactor(0.5f)
            .conformityThreshold(0.3f)
            .conformityAdjustmentRate(0.05f)
            .defaultCommunitySpeed(1.0f)
            .communitySpeedVariance(0.5f)
            .basePeerInfluenceWeight(0.6f)
            .peerActionObservationWeight(0.1f)
            .influenceDecayRate(0.05f)
            .randomSeed(System.currentTimeMillis())
            .build();
    }
    
    /**
     * Aggressive social dynamics for stress testing
     */
    public static SocialConfig stressTest() {
        return builder()
            .baseHerdInfluence(0.9f)
            .herdAmplificationFactor(5.0f)
            .herdThreshold(3)
            .herdDecayRate(0.2f)
            .baseSocialProofSusceptibility(0.95f)
            .validationBoostFactor(0.5f)
            .socialProofThreshold(500)
            .highCountBias(1.0f)
            .viralSurgeProbability(0.1f)
            .trendDurationHours(12)
            .trendFocusRatio(0.9f)
            .trendCategories(new String[]{"breaking_news", "viral_video"})
            .baseConformityFactor(0.9f)
            .conformityThreshold(0.1f)
            .conformityAdjustmentRate(0.1f)
            .defaultCommunitySpeed(1.5f)
            .communitySpeedVariance(1.0f)
            .basePeerInfluenceWeight(0.95f)
            .peerActionObservationWeight(0.2f)
            .influenceDecayRate(0.1f)
            .randomSeed(12345L)
            .build();
    }
    
    /**
     * Builder pattern for SocialConfig
     */
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        // Herd Behavior parameters (defaults)
        private float baseHerdInfluence = 0.5f;
        private float herdAmplificationFactor = 2.0f;
        private int herdThreshold = 5;
        private float herdDecayRate = 0.1f;
        
        // Social Proof parameters (defaults)
        private float baseSocialProofSusceptibility = 0.6f;
        private float validationBoostFactor = 0.2f;
        private float socialProofThreshold = 1000;
        private float highCountBias = 0.7f;
        
        // Trend Participation parameters (defaults)
        private float viralSurgeProbability = 0.01f;
        private float trendDurationHours = 24;
        private float trendFocusRatio = 0.8f;
        private String[] trendCategories = new String[]{
            "trending", "popular", "viral", "hot", "breaking"
        };
        
        // Group Conformity parameters (defaults)
        private float baseConformityFactor = 0.4f;
        private float conformityThreshold = 0.2f;
        private float conformityAdjustmentRate = 0.05f;
        private float defaultCommunitySpeed = 1.0f;
        private float communitySpeedVariance = 0.5f;
        
        // Peer Influence parameters (defaults)
        private float basePeerInfluenceWeight = 0.5f;
        private float peerActionObservationWeight = 0.1f;
        private float influenceDecayRate = 0.05f;
        
        // Random seed (default: time-based)
        private long randomSeed = System.currentTimeMillis();
        
        public Builder baseHerdInfluence(float val) {
            this.baseHerdInfluence = val;
            return this;
        }
        
        public Builder herdAmplificationFactor(float val) {
            this.herdAmplificationFactor = val;
            return this;
        }
        
        public Builder herdThreshold(int val) {
            this.herdThreshold = val;
            return this;
        }
        
        public Builder herdDecayRate(float val) {
            this.herdDecayRate = val;
            return this;
        }
        
        public Builder baseSocialProofSusceptibility(float val) {
            this.baseSocialProofSusceptibility = val;
            return this;
        }
        
        public Builder validationBoostFactor(float val) {
            this.validationBoostFactor = val;
            return this;
        }
        
        public Builder socialProofThreshold(float val) {
            this.socialProofThreshold = val;
            return this;
        }
        
        public Builder highCountBias(float val) {
            this.highCountBias = val;
            return this;
        }
        
        public Builder viralSurgeProbability(float val) {
            this.viralSurgeProbability = val;
            return this;
        }
        
        public Builder trendDurationHours(float val) {
            this.trendDurationHours = val;
            return this;
        }
        
        public Builder trendFocusRatio(float val) {
            this.trendFocusRatio = val;
            return this;
        }
        
        public Builder trendCategories(String[] val) {
            this.trendCategories = val;
            return this;
        }
        
        public Builder baseConformityFactor(float val) {
            this.baseConformityFactor = val;
            return this;
        }
        
        public Builder conformityThreshold(float val) {
            this.conformityThreshold = val;
            return this;
        }
        
        public Builder conformityAdjustmentRate(float val) {
            this.conformityAdjustmentRate = val;
            return this;
        }
        
        public Builder defaultCommunitySpeed(float val) {
            this.defaultCommunitySpeed = val;
            return this;
        }
        
        public Builder communitySpeedVariance(float val) {
            this.communitySpeedVariance = val;
            return this;
        }
        
        public Builder basePeerInfluenceWeight(float val) {
            this.basePeerInfluenceWeight = val;
            return this;
        }
        
        public Builder peerActionObservationWeight(float val) {
            this.peerActionObservationWeight = val;
            return this;
        }
        
        public Builder influenceDecayRate(float val) {
            this.influenceDecayRate = val;
            return this;
        }
        
        public Builder randomSeed(long val) {
            this.randomSeed = val;
            return this;
        }
        
        public SocialConfig build() {
            return new SocialConfig(this);
        }
    }
}
