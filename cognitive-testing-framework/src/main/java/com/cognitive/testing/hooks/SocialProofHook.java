package com.cognitive.testing.hooks;

import com.cognitive.testing.model.SocialState;
import com.cognitive.testing.model.SocialConfig;

/**
 * Hook: Social Proof Influence
 * Simulates "Validation Checks" where agents prioritize interactions with UI elements
 * that show high counts (e.g., "Trending Now", "10k+ Views").
 * 
 * Real-world behavior:
 * - Users trust content with high engagement numbers
 * - "Trending" or "Popular" labels drive click-through rates
 * - High view counts create implicit social validation
 * - This affects which UI elements get prioritized during testing
 */
public class SocialProofHook {
    
    private final SocialState state;
    private int validationCheckCount;
    private int highCountSelections;
    private int lowCountSelections;
    private long totalValidationTime;
    
    // Types of social proof elements
    public enum ProofType {
        VIEW_COUNT("views"),
        LIKE_COUNT("likes"),
        COMMENT_COUNT("comments"),
        SHARE_COUNT("shares"),
        TRENDING_LABEL("trending"),
        POPULAR_LABEL("popular"),
        RATING("rating"),
        RANKING("ranking");
        
        private final String value;
        
        ProofType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    public SocialProofHook(SocialState state) {
        this.state = state;
        this.validationCheckCount = 0;
        this.highCountSelections = 0;
        this.lowCountSelections = 0;
        this.totalValidationTime = 0;
    }
    
    /**
     * Check if social proof should influence action selection
     * 
     * @return true if social proof validation should occur
     */
    public boolean shouldCheckSocialProof() {
        state.updateSocialState();
        
        // Probability based on susceptibility level
        float susceptibility = state.getSocialProofSusceptibility();
        
        // Higher susceptibility = more frequent checks
        return state.getRandom().nextFloat() < susceptibility;
    }
    
    /**
     * Simulate validation check - users pause to verify social proof before acting
     * 
     * @return Time spent on validation check in milliseconds
     */
    public long simulateValidationCheck() {
        validationCheckCount++;
        
        Random random = state.getRandom();
        
        // Validation time based on susceptibility
        // More susceptible users take longer to verify
        float susceptibility = state.getSocialProofSusceptibility();
        int baseTime = (int) (500 + susceptibility * 2000); // 500-2500ms
        
        // Add randomness
        int validationTime = baseTime + random.nextInt(1000);
        
        totalValidationTime += validationTime;
        
        try {
            Thread.sleep(validationTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return validationTime;
    }
    
    /**
     * Calculate the preference score for an element based on its social proof
     * 
     * @param count The numeric count (views, likes, etc.)
     * @param proofType The type of social proof
     * @return Preference score (higher = more likely to be selected)
     */
    public float calculateProofPreference(long count, ProofType proofType) {
        SocialConfig config = state.getConfig();
        
        // Check if count exceeds threshold for "high" status
        float thresholdMultiplier = getThresholdMultiplier(proofType);
        float threshold = config.getSocialProofThreshold() * thresholdMultiplier;
        
        // Calculate base preference
        float preference;
        
        if (count >= threshold) {
            // High count - strong positive bias
            float excess = (float) Math.log(count / threshold + 1);
            preference = config.getHighCountBias() * (1.0f + excess * 0.5f);
            
            // Track high count selection
            highCountSelections++;
        } else {
            // Low count - base preference with slight negative bias
            preference = 0.3f * ((float) count / threshold);
            
            // Track low count selection
            lowCountSelections++;
        }
        
        // Apply susceptibility modulation
        preference = preference * (0.5f + state.getSocialProofSusceptibility() * 0.5f);
        
        return Math.min(1.0f, Math.max(0.0f, preference));
    }
    
    /**
     * Get threshold multiplier for different proof types
     * Some metrics naturally have higher/lower counts
     */
    private float getThresholdMultiplier(ProofType proofType) {
        switch (proofType) {
            case VIEW_COUNT:
                return 10.0f; // Views are typically higher
            case LIKE_COUNT:
                return 1.0f;
            case COMMENT_COUNT:
                return 0.1f; // Comments are typically lower
            case SHARE_COUNT:
                return 0.05f; // Shares are rare
            case TRENDING_LABEL:
            case POPULAR_LABEL:
                return 0.5f;
            case RATING:
                return 0.01f; // Ratings are 1-5
            case RANKING:
                return 0.1f;
            default:
                return 1.0f;
        }
    }
    
    /**
     * Apply social proof influence to select from a list of UI elements
     * 
     * @param elements List of elements with social proof counts
     * @param getCount Function to extract count from element
     * @param <T> Element type
     * @return Selected element based on social proof influence
     */
    public <T> T selectWithSocialProof(java.util.List<T> elements,
                                        java.util.function.Function<T, Long> getCount) {
        if (elements == null || elements.isEmpty()) {
            return null;
        }
        
        // Skip social proof check with some probability
        if (!shouldCheckSocialProof()) {
            return elements.get(state.getRandom().nextInt(elements.size()));
        }
        
        // Simulate validation check time
        simulateValidationCheck();
        
        // Calculate preference scores
        java.util.Map<T, Float> preferences = new java.util.HashMap<>();
        float totalPreference = 0;
        
        for (T element : elements) {
            long count = getCount.apply(element);
            float preference = calculateProofPreference(count, ProofType.VIEW_COUNT);
            preferences.put(element, preference);
            totalPreference += preference;
        }
        
        // Weighted random selection
        float randomValue = state.getRandom().nextFloat() * totalPreference;
        float cumulative = 0;
        
        for (T element : elements) {
            cumulative += preferences.get(element);
            if (randomValue <= cumulative) {
                return element;
            }
        }
        
        return elements.get(elements.size() - 1);
    }
    
    /**
     * Apply social proof influence to select from a list with labeled elements
     * 
     * @param labeledElements Map of element to its social proof label
     * @param <T> Element type
     * @return Selected element based on social proof influence
     */
    public <T> T selectWithProofLabel(java.util.Map<T, String> labeledElements) {
        if (labeledElements == null || labeledElements.isEmpty()) {
            return null;
        }
        
        // Skip social proof check with some probability
        if (!shouldCheckSocialProof()) {
            int index = state.getRandom().nextInt(labeledElements.size());
            return (T) labeledElements.keySet().toArray()[index];
        }
        
        // Simulate validation check
        simulateValidationCheck();
        
        // Calculate preference based on label
        java.util.Map<T, Float> preferences = new java.util.HashMap<>();
        float totalPreference = 0;
        
        for (java.util.Map.Entry<T, String> entry : labeledElements.entrySet()) {
            float preference = calculateLabelPreference(entry.getValue());
            preferences.put(entry.getKey(), preference);
            totalPreference += preference;
        }
        
        // Weighted random selection
        float randomValue = state.getRandom().nextFloat() * totalPreference;
        float cumulative = 0;
        
        for (java.util.Map.Entry<T, String> entry : labeledElements.entrySet()) {
            cumulative += preferences.get(entry.getKey());
            if (randomValue <= cumulative) {
                return entry.getKey();
            }
        }
        
        return null;
    }
    
    /**
     * Calculate preference based on social proof label
     */
    private float calculateLabelPreference(String label) {
        if (label == null) {
            return 0.1f;
        }
        
        String lowerLabel = label.toLowerCase();
        float basePreference = 0.5f;
        
        // Strong positive labels
        if (lowerLabel.contains("trending") || lowerLabel.contains("hot")) {
            basePreference = 0.95f;
        } else if (lowerLabel.contains("popular") || lowerLabel.contains("viral")) {
            basePreference = 0.85f;
        } else if (lowerLabel.contains("top") || lowerLabel.contains("best")) {
            basePreference = 0.80f;
        } else if (lowerLabel.contains("recommended") || lowerLabel.contains("featured")) {
            basePreference = 0.70f;
        } else if (lowerLabel.contains("new")) {
            basePreference = 0.60f;
        } else if (lowerLabel.contains("sponsored") || lowerLabel.contains("ad")) {
            basePreference = 0.30f; // Skeptical of ads
        }
        
        // Apply susceptibility modulation
        return basePreference * (0.5f + state.getSocialProofSusceptibility() * 0.5f);
    }
    
    /**
     * Determine if element should be clicked based on social proof
     * 
     * @param viewCount Number of views
     * @param likeCount Number of likes
     * @param commentCount Number of comments
     * @return true if social proof supports clicking this element
     */
    public boolean shouldClickBasedOnProof(long viewCount, long likeCount, long commentCount) {
        if (!shouldCheckSocialProof()) {
            return state.getRandom().nextBoolean();
        }
        
        // Calculate combined proof score
        float viewScore = calculateProofPreference(viewCount, ProofType.VIEW_COUNT);
        float likeScore = calculateProofPreference(likeCount, ProofType.LIKE_COUNT);
        float commentScore = calculateProofPreference(commentCount, ProofType.COMMENT_COUNT);
        
        // Weighted average
        float combinedScore = viewScore * 0.3f + likeScore * 0.4f + commentScore * 0.3f;
        
        return state.getRandom().nextFloat() < combinedScore;
    }
    
    /**
     * Get the current social proof susceptibility level
     */
    public float getCurrentSusceptibility() {
        return state.getSocialProofSusceptibility();
    }
    
    /**
     * Get the high count threshold from config
     */
    public long getProofThreshold() {
        return (long) state.getConfig().getSocialProofThreshold();
    }
    
    /**
     * Reset statistics for a new session
     */
    public void resetSession() {
        validationCheckCount = 0;
        highCountSelections = 0;
        lowCountSelections = 0;
        totalValidationTime = 0;
    }
    
    /**
     * Get statistics for this session
     */
    public SocialProofStatistics getStatistics() {
        int totalSelections = highCountSelections + lowCountSelections;
        float highSelectionRate = totalSelections > 0 ? 
            (float) highCountSelections / totalSelections : 0;
        
        return new SocialProofStatistics(
            validationCheckCount,
            highCountSelections,
            lowCountSelections,
            highSelectionRate,
            totalValidationTime,
            state.getSocialProofSusceptibility()
        );
    }
    
    /**
     * Statistics container for social proof behavior
     */
    public static class SocialProofStatistics {
        public final int validationCheckCount;
        public final int highCountSelections;
        public final int lowCountSelections;
        public final float highCountSelectionRate;
        public final long totalValidationTimeMs;
        public final float susceptibilityLevel;
        
        public SocialProofStatistics(int validationCheckCount, int highCountSelections,
                                     int lowCountSelections, float highCountSelectionRate,
                                     long totalValidationTimeMs, float susceptibilityLevel) {
            this.validationCheckCount = validationCheckCount;
            this.highCountSelections = highCountSelections;
            this.lowCountSelections = lowCountSelections;
            this.highCountSelectionRate = highCountSelectionRate;
            this.totalValidationTimeMs = totalValidationTimeMs;
            this.susceptibilityLevel = susceptibilityLevel;
        }
        
        @Override
        public String toString() {
            return String.format("SocialProofStats{checks=%d, highSelections=%d, lowSelections=%d, " +
                "highRate=%.1f%%, validationTime=%dms, susceptibility=%.1f%%}",
                validationCheckCount, highCountSelections, lowCountSelections,
                highCountSelectionRate * 100, totalValidationTimeMs, susceptibilityLevel * 100);
        }
    }
}
