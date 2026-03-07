package com.cognitive.testing.model;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Core social state model for tracking collective dynamics.
 * Tracks herd behavior, trends, peer influence, and community metrics.
 */
public class SocialState {
    
    // Session tracking
    private final long sessionStartTime;
    private final String sessionId;
    private final AtomicInteger interactionCount;
    
    // Social metrics (0.0 - 1.0 scale)
    private volatile float herdInfluenceLevel;
    private volatile float socialProofSusceptibility;
    private volatile float trendAlignment;
    private volatile float conformityFactor;
    private volatile float peerInfluenceWeight;
    
    // Trend tracking
    private volatile String currentTrendingCategory;
    private volatile long trendStartTime;
    private volatile boolean isViralSurgeActive;
    private volatile float viralSurgeIntensity;
    
    // Community metrics (simulated)
    private volatile float communityMeanSpeed;
    private volatile int communityActiveAgents;
    private volatile float[] peerActionDistribution;
    
    // Recent actions for herd effect calculation
    private static final int ACTION_HISTORY_SIZE = 100;
    private final String[] actionHistory;
    private volatile int actionHistoryIndex;
    
    // Random instance with controlled seed for reproducibility
    private final Random random;
    
    // Configuration
    private final SocialConfig config;
    
    public SocialState(SocialConfig config) {
        this.sessionStartTime = System.currentTimeMillis();
        this.sessionId = "SOCIAL-" + System.currentTimeMillis();
        this.interactionCount = new AtomicInteger(0);
        
        // Initialize social metrics
        this.herdInfluenceLevel = config.getBaseHerdInfluence();
        this.socialProofSusceptibility = config.getBaseSocialProofSusceptibility();
        this.trendAlignment = config.getBaseTrendAlignment();
        this.conformityFactor = config.getBaseConformityFactor();
        this.peerInfluenceWeight = config.getBasePeerInfluenceWeight();
        
        // Initialize trend tracking
        this.currentTrendingCategory = null;
        this.trendStartTime = 0;
        this.isViralSurgeActive = false;
        this.viralSurgeIntensity = 0.0f;
        
        // Initialize community metrics
        this.communityMeanSpeed = config.getDefaultCommunitySpeed();
        this.communityActiveAgents = 1;
        this.peerActionDistribution = new float[10]; // 10 action categories
        initializePeerDistribution();
        
        // Initialize action history
        this.actionHistory = new String[ACTION_HISTORY_SIZE];
        this.actionHistoryIndex = 0;
        
        this.random = new Random(config.getRandomSeed());
        this.config = config;
    }
    
    private void initializePeerDistribution() {
        // Initialize with roughly uniform distribution
        for (int i = 0; i < peerActionDistribution.length; i++) {
            peerActionDistribution[i] = 1.0f / peerActionDistribution.length;
        }
    }
    
    /**
     * Record an action in the history for herd effect calculation
     */
    public void recordAction(String actionType) {
        actionHistory[actionHistoryIndex] = actionType;
        actionHistoryIndex = (actionHistoryIndex + 1) % ACTION_HISTORY_SIZE;
        interactionCount.incrementAndGet();
    }
    
    /**
     * Get the count of a specific action type in recent history
     */
    public int getRecentActionCount(String actionType) {
        int count = 0;
        for (String action : actionHistory) {
            if (action != null && action.equals(actionType)) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Update social state after each interaction
     */
    public void updateSocialState() {
        long sessionDuration = System.currentTimeMillis() - sessionStartTime;
        
        // Update herd influence based on activity
        updateHerdInfluence(sessionDuration);
        
        // Update social proof susceptibility
        updateSocialProofSusceptibility();
        
        // Check for viral surge conditions
        checkViralSurge();
        
        // Update community metrics
        updateCommunityMetrics();
    }
    
    private void updateHerdInfluence(long sessionDuration) {
        // Herd influence increases with session duration
        int minutes = (int) (sessionDuration / 60000);
        
        // Short sessions have lower herd influence
        if (minutes < 5) {
            herdInfluenceLevel = config.getBaseHerdInfluence() * 0.5f;
        } else if (minutes < 30) {
            herdInfluenceLevel = config.getBaseHerdInfluence() * (0.5f + (minutes - 5) / 25.0f * 0.3f);
        } else {
            herdInfluenceLevel = Math.min(1.0f, config.getBaseHerdInfluence() * 
                (0.8f + (minutes - 30) / 60.0f * 0.2f));
        }
        
        // Random fluctuations in herd influence
        herdInfluenceLevel += (random.nextFloat() - 0.5f) * 0.1f;
        herdInfluenceLevel = Math.max(0.0f, Math.min(1.0f, herdInfluenceLevel));
    }
    
    private void updateSocialProofSusceptibility() {
        // Susceptibility to social proof varies with recent validation experiences
        float recentValidation = getRecentActionCount("validated") / (float) ACTION_HISTORY_SIZE;
        
        // Positive validations increase susceptibility
        socialProofSusceptibility = config.getBaseSocialProofSusceptibility() + 
            recentValidation * config.getValidationBoostFactor();
        
        // Random fluctuations
        socialProofSusceptibility += (random.nextFloat() - 0.5f) * 0.05f;
        socialProofSusceptibility = Math.max(0.0f, Math.min(1.0f, socialProofSusceptibility));
    }
    
    private void checkViralSurge() {
        long sessionDuration = System.currentTimeMillis() - sessionStartTime;
        
        // Check if current trend has expired (24 hours)
        if (isViralSurgeActive && (sessionDuration - trendStartTime) > config.getTrendDurationMs()) {
            isViralSurgeActive = false;
            currentTrendingCategory = null;
            viralSurgeIntensity = 0.0f;
        }
        
        // Random chance to start a new viral surge
        if (!isViralSurgeActive && random.nextFloat() < config.getViralSurgeProbability()) {
            startNewTrend();
        }
    }
    
    private void startNewTrend() {
        String[] categories = config.getTrendCategories();
        currentTrendingCategory = categories[random.nextInt(categories.length)];
        trendStartTime = System.currentTimeMillis() - sessionStartTime;
        isViralSurgeActive = true;
        viralSurgeIntensity = 0.5f + random.nextFloat() * 0.5f;
        trendAlignment = 0.8f; // High alignment during viral surge
    }
    
    private void updateCommunityMetrics() {
        // Simulate community mean speed changes
        float targetSpeed = config.getDefaultCommunitySpeed() + 
            (random.nextFloat() - 0.5f) * config.getCommunitySpeedVariance();
        communityMeanSpeed = communityMeanSpeed * 0.95f + targetSpeed * 0.05f;
        
        // Update peer action distribution based on recent activity
        updatePeerDistribution();
    }
    
    private void updatePeerDistribution() {
        // Adjust distribution based on trending
        if (isViralSurgeActive && currentTrendingCategory != null) {
            // Find index for trending category (simplified mapping)
            int trendIndex = currentTrendingCategory.hashCode() % peerActionDistribution.length;
            if (trendIndex < 0) trendIndex += peerActionDistribution.length;
            
            // Boost trending category weight
            peerActionDistribution[trendIndex] += viralSurgeIntensity * 0.1f;
        }
        
        // Normalize distribution
        float sum = 0;
        for (float f : peerActionDistribution) {
            sum += f;
        }
        for (int i = 0; i < peerActionDistribution.length; i++) {
            peerActionDistribution[i] /= sum;
        }
    }
    
    /**
     * Update peer action from simulated community
     */
    public void observePeerAction(String actionType) {
        int index = actionType.hashCode() % peerActionDistribution.length;
        if (index < 0) index += peerActionDistribution.length;
        peerActionDistribution[index] += config.getPeerActionObservationWeight();
        
        // Normalize
        float sum = 0;
        for (float f : peerActionDistribution) {
            sum += f;
        }
        for (int i = 0; i < peerActionDistribution.length; i++) {
            peerActionDistribution[i] /= sum;
        }
    }
    
    // Getter methods
    public long getSessionDuration() {
        return System.currentTimeMillis() - sessionStartTime;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public int getInteractionCount() {
        return interactionCount.get();
    }
    
    public float getHerdInfluenceLevel() {
        return herdInfluenceLevel;
    }
    
    public float getSocialProofSusceptibility() {
        return socialProofSusceptibility;
    }
    
    public float getTrendAlignment() {
        return trendAlignment;
    }
    
    public float getConformityFactor() {
        return conformityFactor;
    }
    
    public float getPeerInfluenceWeight() {
        return peerInfluenceWeight;
    }
    
    public String getCurrentTrendingCategory() {
        return currentTrendingCategory;
    }
    
    public boolean isViralSurgeActive() {
        return isViralSurgeActive;
    }
    
    public float getViralSurgeIntensity() {
        return viralSurgeIntensity;
    }
    
    public float getCommunityMeanSpeed() {
        return communityMeanSpeed;
    }
    
    public int getCommunityActiveAgents() {
        return communityActiveAgents;
    }
    
    public float[] getPeerActionDistribution() {
        return peerActionDistribution.clone();
    }
    
    public Random getRandom() {
        return random;
    }
    
    public SocialConfig getConfig() {
        return config;
    }
    
    /**
     * Adjust conformity factor based on observed community behavior
     */
    public void adjustConformity(float observedMeanSpeed) {
        float speedDifference = observedMeanSpeed - communityMeanSpeed;
        
        // Conformity pulls toward the mean
        if (Math.abs(speedDifference) > config.getConformityThreshold()) {
            conformityFactor = Math.min(1.0f, conformityFactor + config.getConformityAdjustmentRate());
            
            // Adjust own speed toward mean
            communityMeanSpeed = communityMeanSpeed * (1 - conformityFactor) + 
                observedMeanSpeed * conformityFactor;
        }
    }
    
    /**
     * Reset session state
     */
    public void resetSession() {
        interactionCount.set(0);
        isViralSurgeActive = false;
        currentTrendingCategory = null;
        viralSurgeIntensity = 0.0f;
        actionHistoryIndex = 0;
        initializePeerDistribution();
    }
    
    /**
     * Get the most likely action based on peer distribution
     */
    public String getMostLikelyPeerAction() {
        int maxIndex = 0;
        float maxValue = peerActionDistribution[0];
        
        for (int i = 1; i < peerActionDistribution.length; i++) {
            if (peerActionDistribution[i] > maxValue) {
                maxValue = peerActionDistribution[i];
                maxIndex = i;
            }
        }
        
        // Map back to action types (simplified)
        String[] actionTypes = {"like", "comment", "share", "view", "follow", 
                                 "react", "save", "report", "search", "click"};
        return actionTypes[maxIndex % actionTypes.length];
    }
}
