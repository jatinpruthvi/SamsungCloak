package com.cognitive.testing.hooks;

import com.cognitive.testing.model.SocialState;
import com.cognitive.testing.model.SocialConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Hook: Trend Participation
 * Simulates "Viral Surge" where agents focus 80% of their activity on a specific
 * feature or content category for a defined 24-hour period.
 * 
 * Real-world behavior:
 * - Viral content captures disproportionate attention
 * - Users cluster around trending topics
 * - Trending cycles create traffic spikes
 * - Critical for testing sudden load scenarios
 */
public class TrendParticipationHook {
    
    private final SocialState state;
    private int trendParticipations;
    private int nonTrendActions;
    private long totalTrendFocusTime;
    private int surgeCyclesObserved;
    
    public TrendParticipationHook(SocialState state) {
        this.state = state;
        this.trendParticipations = 0;
        this.nonTrendActions = 0;
        this.totalTrendFocusTime = 0;
        this.surgeCyclesObserved = 0;
    }
    
    /**
     * Check if a viral surge is currently active
     * 
     * @return true if viral surge is active
     */
    public boolean isViralSurgeActive() {
        return state.isViralSurgeActive();
    }
    
    /**
     * Get the current trending category
     * 
     * @return Current trending category name, or null if no trend
     */
    public String getCurrentTrend() {
        return state.getCurrentTrendingCategory();
    }
    
    /**
     * Get the viral surge intensity (0.0 - 1.0)
     * 
     * @return Current surge intensity
     */
    public float getSurgeIntensity() {
        return state.getViralSurgeIntensity();
    }
    
    /**
     * Determine if the next action should focus on the current trend
     * Uses stochastic decision based on trend focus ratio
     * 
     * @return true if next action should be trend-focused
     */
    public boolean shouldFocusOnTrend() {
        state.updateSocialState();
        
        if (!state.isViralSurgeActive()) {
            // No active trend - use base probability
            return state.getRandom().nextFloat() < state.getConfig().getViralSurgeProbability();
        }
        
        // Active viral surge - high probability of trend focus
        SocialConfig config = state.getConfig();
        float focusProbability = config.getTrendFocusRatio() * state.getViralSurgeIntensity();
        
        // Increase participation count if focusing on trend
        if (state.getRandom().nextFloat() < focusProbability) {
            trendParticipations++;
            return true;
        }
        
        nonTrendActions++;
        return false;
    }
    
    /**
     * Simulate the focus shift during a viral surge
     * Records the time spent focusing on trending content
     * 
     * @param actionDuration Duration of the trend-focused action
     */
    public void recordTrendFocus(long actionDuration) {
        if (state.isViralSurgeActive()) {
            totalTrendFocusTime += actionDuration;
        }
    }
    
    /**
     * Get a list of trending content categories to prioritize
     * Returns the current trending category with related categories
     * 
     * @param maxCategories Maximum number of categories to return
     * @return List of trending categories
     */
    public List<String> getTrendingCategories(int maxCategories) {
        List<String> categories = new ArrayList<>();
        
        if (state.isViralSurgeActive() && state.getCurrentTrendingCategory() != null) {
            categories.add(state.getCurrentTrendingCategory());
        }
        
        // Add random categories from config
        String[] allCategories = state.getConfig().getTrendCategories();
        Random random = state.getRandom();
        
        List<String> availableCategories = new ArrayList<>();
        Collections.addAll(availableCategories, allCategories);
        Collections.shuffle(availableCategories, random);
        
        for (String category : availableCategories) {
            if (categories.size() >= maxCategories) break;
            if (!categories.contains(category)) {
                categories.add(category);
            }
        }
        
        return categories;
    }
    
    /**
     * Calculate the engagement boost based on trend alignment
     * 
     * @param baseEngagement Base engagement count
     * @return Modified engagement with trend boost
     */
    public long calculateTrendBoost(long baseEngagement) {
        if (!state.isViralSurgeActive()) {
            return baseEngagement;
        }
        
        // Viral surge multiplies engagement
        float boostMultiplier = 1.0f + state.getViralSurgeIntensity() * 5.0f;
        
        return (long) (baseEngagement * boostMultiplier);
    }
    
    /**
     * Apply trend participation to action selection
     * Biases action selection toward trending content
     * 
     * @param contentList List of available content
     * @param getCategory Function to extract category from content
     * @param <T> Content type
     * @return Selected content considering trend participation
     */
    public <T> T selectWithTrendBias(List<T> contentList, 
                                      java.util.function.Function<T, String> getCategory) {
        if (contentList == null || contentList.isEmpty()) {
            return null;
        }
        
        // If no active trend, return random selection
        if (!shouldFocusOnTrend()) {
            return contentList.get(state.getRandom().nextInt(contentList.size()));
        }
        
        // Calculate weights based on trend alignment
        String currentTrend = state.getCurrentTrendingCategory();
        float intensity = state.getViralSurgeIntensity();
        
        float totalWeight = 0;
        java.util.Map<T, Float> weights = new java.util.HashMap<>();
        
        for (T content : contentList) {
            String category = getCategory.apply(content);
            float weight = calculateTrendWeight(category, currentTrend, intensity);
            weights.put(content, weight);
            totalWeight += weight;
        }
        
        // Weighted random selection
        float randomValue = state.getRandom().nextFloat() * totalWeight;
        float cumulative = 0;
        
        for (T content : contentList) {
            cumulative += weights.get(content);
            if (randomValue <= cumulative) {
                return content;
            }
        }
        
        return contentList.get(contentList.size() - 1);
    }
    
    /**
     * Calculate weight for content based on trend alignment
     */
    private float calculateTrendWeight(String contentCategory, String currentTrend, float intensity) {
        float baseWeight = 1.0f;
        
        if (contentCategory == null || currentTrend == null) {
            return baseWeight;
        }
        
        // Direct match with trending
        if (contentCategory.equalsIgnoreCase(currentTrend)) {
            baseWeight = 1.0f + intensity * 9.0f; // Up to 10x weight
        }
        
        // Partial match (contains)
        if (contentCategory.toLowerCase().contains(currentTrend.toLowerCase()) ||
            currentTrend.toLowerCase().contains(contentCategory.toLowerCase())) {
            baseWeight = 1.0f + intensity * 4.0f; // Up to 5x weight
        }
        
        return baseWeight;
    }
    
    /**
     * Get the trend alignment factor (0.0 - 1.0)
     * 
     * @return Current trend alignment
     */
    public float getTrendAlignment() {
        return state.getTrendAlignment();
    }
    
    /**
     * Check if current session should trigger a new viral surge
     * Uses probability-based stochastic decision
     * 
     * @return true if a new viral surge should start
     */
    public boolean shouldTriggerNewSurge() {
        float probability = state.getConfig().getViralSurgeProbability();
        return state.getRandom().nextFloat() < probability;
    }
    
    /**
     * Force start a viral surge for testing
     * 
     * @param category The category to trend
     * @param intensity The surge intensity (0.0 - 1.0)
     */
    public void forceStartSurge(String category, float intensity) {
        // Note: This requires access to internal state methods
        // For now, we just track that we observed a surge cycle
        surgeCyclesObserved++;
        
        // Log for debugging
        System.out.println("Forced viral surge: category=" + category + ", intensity=" + intensity);
    }
    
    /**
     * Calculate the duration of the current trend period
     * 
     * @return Remaining trend duration in milliseconds
     */
    public long getRemainingTrendDuration() {
        if (!state.isViralSurgeActive()) {
            return 0;
        }
        
        long trendDuration = state.getConfig().getTrendDurationMs();
        long elapsed = state.getSessionDuration();
        
        return Math.max(0, trendDuration - elapsed);
    }
    
    /**
     * Get the trend focus ratio from config
     * 
     * @return Configured trend focus ratio
     */
    public float getConfiguredFocusRatio() {
        return state.getConfig().getTrendFocusRatio();
    }
    
    /**
     * Reset trend participation for a new session
     */
    public void resetSession() {
        trendParticipations = 0;
        nonTrendActions = 0;
        totalTrendFocusTime = 0;
        surgeCyclesObserved = 0;
    }
    
    /**
     * Get statistics for this session
     */
    public TrendStatistics getStatistics() {
        int totalActions = trendParticipations + nonTrendActions;
        float trendFocusRate = totalActions > 0 ? 
            (float) trendParticipations / totalActions : 0;
        
        return new TrendStatistics(
            trendParticipations,
            nonTrendActions,
            trendFocusRate,
            totalTrendFocusTime,
            state.isViralSurgeActive(),
            state.getCurrentTrendingCategory(),
            state.getViralSurgeIntensity(),
            surgeCyclesObserved
        );
    }
    
    /**
     * Statistics container for trend participation
     */
    public static class TrendStatistics {
        public final int trendParticipations;
        public final int nonTrendActions;
        public final float trendFocusRate;
        public final long totalTrendFocusTimeMs;
        public final boolean surgeActive;
        public final String currentTrend;
        public final float surgeIntensity;
        public final int surgeCyclesObserved;
        
        public TrendStatistics(int trendParticipations, int nonTrendActions,
                               float trendFocusRate, long totalTrendFocusTimeMs,
                               boolean surgeActive, String currentTrend,
                               float surgeIntensity, int surgeCyclesObserved) {
            this.trendParticipations = trendParticipations;
            this.nonTrendActions = nonTrendActions;
            this.trendFocusRate = trendFocusRate;
            this.totalTrendFocusTimeMs = totalTrendFocusTimeMs;
            this.surgeActive = surgeActive;
            this.currentTrend = currentTrend;
            this.surgeIntensity = surgeIntensity;
            this.surgeCyclesObserved = surgeCyclesObserved;
        }
        
        @Override
        public String toString() {
            return String.format("TrendStatistics{trendActions=%d, nonTrendActions=%d, " +
                "focusRate=%.1f%%, focusTime=%ds, active=%s, trend=%s, intensity=%.2f, cycles=%d}",
                trendParticipations, nonTrendActions, trendFocusRate * 100,
                totalTrendFocusTimeMs / 1000, surgeActive, currentTrend, 
                surgeIntensity, surgeCyclesObserved);
        }
    }
}
