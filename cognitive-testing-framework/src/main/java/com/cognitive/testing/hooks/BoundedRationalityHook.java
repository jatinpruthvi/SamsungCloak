package com.cognitive.testing.hooks;

import com.cognitive.testing.model.CognitiveState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Hook: Bounded Rationality - Satisficing Behavior
 * Simulates choosing the first acceptable option rather than optimal pathing.
 * 
 * Real-world behavior:
 * - Users don't always find the "best" option
 * - First acceptable choice is often selected (satisficing)
 * - Decision time is limited by attention span and fatigue
 * - UI discoverability depends on cognitive state
 * 
 * Tests: UI discoverability, option prominence, user decision trees
 */
public class BoundedRationalityHook {
    
    private final CognitiveState state;
    private final SatisficingStrategy strategy;
    
    public BoundedRationalityHook(CognitiveState state) {
        this.state = state;
        this.strategy = new SatisficingStrategy();
    }
    
    /**
     * Select an option from a list using satisficing logic
     * Returns first "good enough" option rather than optimal one
     * 
     * @param <T> Option type
     * @param options List of available options
     * @param evaluator Function to evaluate option quality (0.0 - 1.0)
     * @param acceptableThreshold Minimum quality to accept (0.0 - 1.0)
     * @return Selected option
     */
    public <T> T selectOption(List<T> options, OptionEvaluator<T> evaluator, float acceptableThreshold) {
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("Options list cannot be empty");
        }
        
        state.recordInteraction();
        
        // Apply satisficing based on cognitive state
        float satisficingThreshold = calculateSatisficingThreshold(acceptableThreshold);
        
        // Shuffle options based on visual prominence (simulated)
        List<T> consideredOptions = shuffleByProminence(new ArrayList<>(options));
        
        // Review options in order of prominence
        int optionsReviewed = 0;
        int maxOptionsToReview = calculateMaxOptionsToReview(consideredOptions.size());
        
        for (T option : consideredOptions) {
            if (optionsReviewed >= maxOptionsToReview) {
                break;
            }
            
            float quality = evaluator.evaluate(option);
            optionsReviewed++;
            
            // Accept first option that meets threshold
            if (quality >= satisficingThreshold) {
                return option;
            }
        }
        
        // If no acceptable option found, pick the best reviewed
        return options.get(state.getRandom().nextInt(options.size()));
    }
    
    /**
     * Select option with default threshold (0.5 = 50% quality)
     */
    public <T> T selectOption(List<T> options, OptionEvaluator<T> evaluator) {
        return selectOption(options, evaluator, 0.5f);
    }
    
    /**
     * Select option by index using satisficing
     * Returns first acceptable index rather than optimal one
     * 
     * @param count Total number of options
     * @param isAcceptable Function to check if option at index is acceptable
     * @return Selected index
     */
    public int selectOptionIndex(int count, java.util.function.IntPredicate isAcceptable) {
        if (count <= 0) {
            throw new IllegalArgumentException("Count must be positive");
        }
        
        state.recordInteraction();
        
        float satisficingThreshold = state.getConfig().getSatisficingThreshold();
        int maxOptionsToReview = calculateMaxOptionsToReview(count);
        
        // Review options in visual order (top to bottom, left to right)
        for (int i = 0; i < Math.min(count, maxOptionsToReview); i++) {
            if (isAcceptable.test(i)) {
                // Accept based on satisficing threshold
                if (state.getRandom().nextFloat() < satisficingThreshold) {
                    return i;
                }
            }
        }
        
        // Fallback to random selection
        return state.getRandom().nextInt(count);
    }
    
    /**
     * Determine if user will search for better option or accept current
     * 
     * @param currentOptionQuality Quality of current option (0.0 - 1.0)
     * @return true if user will search for better option
     */
    public boolean shouldSearchForBetter(float currentOptionQuality) {
        state.recordInteraction();
        
        // High fatigue or low attention = less likely to search
        float searchProbability = 1.0f - currentOptionQuality; // Better option = less search
        searchProbability *= (1.0f - state.getFatigueLevel() / 3.0f); // Fatigue reduces search
        searchProbability *= state.getAttentionLevel(); // Low attention reduces search
        
        return state.getRandom().nextFloat() < searchProbability;
    }
    
    /**
     * Calculate how many options user will review before deciding
     * Factors: fatigue, attention, time pressure
     * 
     * @param totalOptions Total available options
     * @return Maximum number of options to review
     */
    public int calculateMaxOptionsToReview(int totalOptions) {
        if (totalOptions <= 3) {
            return totalOptions; // Always review small sets
        }
        
        // Base: review 50% of options
        float reviewRatio = 0.5f;
        
        // Fatigue reduces options reviewed
        reviewRatio *= (1.0f - state.getFatigueLevel() / 4.0f);
        
        // Low attention reduces options reviewed
        reviewRatio *= state.getAttentionLevel();
        
        // High stress reduces options reviewed
        reviewRatio *= (1.0f - state.getStressLevel() / 2.0f);
        
        int maxToReview = Math.max(2, (int) (totalOptions * reviewRatio));
        return Math.min(totalOptions, maxToReview);
    }
    
    /**
     * Calculate satisficing threshold based on cognitive state
     * Higher fatigue/low attention = lower threshold (accept anything)
     * 
     * @param baseThreshold Base acceptable threshold
     * @return Adjusted threshold
     */
    private float calculateSatisficingThreshold(float baseThreshold) {
        float adjusted = baseThreshold;
        
        // Fatigue lowers threshold (willing to accept worse options)
        adjusted *= (1.0f - state.getFatigueLevel() / 5.0f);
        
        // Low attention lowers threshold
        adjusted *= state.getAttentionLevel();
        
        // Stress lowers threshold
        adjusted *= (1.0f - state.getStressLevel() / 3.0f);
        
        // Ensure minimum threshold of 0.2 (won't accept terrible options)
        return Math.max(0.2f, adjusted);
    }
    
    /**
     * Shuffle options by visual prominence
     * Simulates UI layout effects (top items, left items more prominent)
     */
    private <T> List<T> shuffleByProminence(List<T> options) {
        // In real implementation, would use UI position data
        // For now, return as-is (top-left to bottom-right order)
        return options;
    }
    
    /**
     * Strategy for satisficing decisions
     */
    public static class SatisficingStrategy {
        private int satisficingDecisions;
        private int optimalDecisions;
        
        public void recordSatisficing() {
            satisficingDecisions++;
        }
        
        public void recordOptimal() {
            optimalDecisions++;
        }
        
        public float getSatisficingRatio() {
            int total = satisficingDecisions + optimalDecisions;
            return total == 0 ? 0.0f : (float) satisficingDecisions / total;
        }
        
        public int getTotalDecisions() {
            return satisficingDecisions + optimalDecisions;
        }
    }
    
    /**
     * Functional interface for evaluating option quality
     */
    @FunctionalInterface
    public interface OptionEvaluator<T> {
        /**
         * Evaluate option quality
         * @param option The option to evaluate
         * @return Quality score (0.0 = terrible, 1.0 = perfect)
         */
        float evaluate(T option);
    }
    
    /**
     * Get satisficing statistics
     */
    public SatisficingStatistics getStatistics() {
        return new SatisficingStatistics(
            strategy.getSatisficingDecisions(),
            strategy.getOptimalDecisions(),
            strategy.getSatisficingRatio()
        );
    }
    
    /**
     * Statistics for satisficing behavior
     */
    public static class SatisficingStatistics {
        public final int satisficingCount;
        public final int optimalCount;
        public final float satisficingRatio;
        
        public SatisficingStatistics(int satisficingCount, int optimalCount, float satisficingRatio) {
            this.satisficingCount = satisficingCount;
            this.optimalCount = optimalCount;
            this.satisficingRatio = satisficingRatio;
        }
        
        @Override
        public String toString() {
            return String.format("SatisficingStats{satisficing=%d, optimal=%d, ratio=%.2f%%}",
                satisficingCount, optimalCount, satisficingRatio * 100);
        }
    }
    
    /**
     * Inner class to expose satisficing count
     */
    public static class SatisficingStrategy {
        private int satisficingDecisions = 0;
        private int optimalDecisions = 0;
        
        public void recordSatisficing() {
            satisficingDecisions++;
        }
        
        public void recordOptimal() {
            optimalDecisions++;
        }
        
        public int getSatisficingDecisions() {
            return satisficingDecisions;
        }
        
        public int getOptimalDecisions() {
            return optimalDecisions;
        }
        
        public float getSatisficingRatio() {
            int total = satisficingDecisions + optimalDecisions;
            return total == 0 ? 0.0f : (float) satisficingDecisions / total;
        }
    }
}
