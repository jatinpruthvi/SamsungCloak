package com.cognitive.testing.hooks;

import com.cognitive.testing.model.CognitiveState;

/**
 * Hook: Emotional Bias Simulation
 * Simulates stochastic interaction bursts and navigation hesitation reflecting user frustration or uncertainty.
 * 
 * Real-world behavior:
 * - Users get frustrated with slow or confusing UIs
 * - Rapid tapping when elements don't respond
 * - Hesitation when uncertain about actions
 * - Emotional state affects interaction speed and accuracy
 * 
 * Tests: UI responsiveness, error handling, feedback clarity, usability under stress
 */
public class EmotionalBiasHook {
    
    private final CognitiveState state;
    private int interactionBurstCount;
    private int hesitationCount;
    private long totalBurstDuration;
    private long totalHesitationDuration;
    
    public EmotionalBiasHook(CognitiveState state) {
        this.state = state;
        this.interactionBurstCount = 0;
        this.hesitationCount = 0;
        this.totalBurstDuration = 0;
        this.totalHesitationDuration = 0;
    }
    
    /**
     * Check if emotional interaction burst should occur
     * Simulates rapid, repeated actions due to frustration
     * 
     * @return true if interaction burst should occur
     */
    public boolean shouldTriggerInteractionBurst() {
        state.recordInteraction();
        
        // Base probability from config
        float burstProbability = state.getConfig().getInteractionBurstProbability();
        
        // Stress increases burst probability
        burstProbability += state.getStressLevel() * 0.15f;
        
        // Low attention increases burst probability (user is impatient)
        burstProbability += (1.0f - state.getAttentionLevel()) * 0.10f;
        
        // High fatigue increases burst probability
        burstProbability += Math.min(0.10f, state.getFatigueLevel() * 0.03f);
        
        return state.getRandom().nextFloat() < burstProbability;
    }
    
    /**
     * Simulate interaction burst
     * Returns number of rapid interactions to perform
     * 
     * @param maxInteractions Maximum interactions in burst (default: 5)
     * @return Number of interactions to perform
     */
    public int simulateInteractionBurst(int maxInteractions) {
        interactionBurstCount++;
        
        // Burst size based on stress level
        int burstSize = 2 + (int) (state.getStressLevel() * (maxInteractions - 2));
        burstSize = Math.min(maxInteractions, Math.max(2, burstSize));
        
        // Add randomness
        burstSize = Math.max(2, burstSize + (state.getRandom().nextInt(3) - 1));
        
        long startTime = System.currentTimeMillis();
        
        // Simulate rapid interactions with minimal delay
        for (int i = 0; i < burstSize; i++) {
            // Very short delay between burst interactions (50-200ms)
            int delay = 50 + state.getRandom().nextInt(150);
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        totalBurstDuration += System.currentTimeMillis() - startTime;
        return burstSize;
    }
    
    /**
     * Simulate interaction burst with default max (5 interactions)
     * @return Number of interactions to perform
     */
    public int simulateInteractionBurst() {
        return simulateInteractionBurst(5);
    }
    
    /**
     * Check if navigation hesitation should occur
     * Simulates pausing before action due to uncertainty
     * 
     * @return true if hesitation should occur
     */
    public boolean shouldTriggerHesitation() {
        state.recordInteraction();
        
        // Base probability from config
        float hesitationProbability = state.getConfig().getNavigationHesitationProbability();
        
        // High stress increases hesitation
        hesitationProbability += state.getStressLevel() * 0.12f;
        
        // Low attention increases hesitation
        hesitationProbability += (1.0f - state.getAttentionLevel()) * 0.10f;
        
        // High fatigue increases hesitation
        hesitationProbability += Math.min(0.08f, state.getFatigueLevel() * 0.02f);
        
        return state.getRandom().nextFloat() < hesitationProbability;
    }
    
    /**
     * Simulate navigation hesitation
     * Pauses for a duration reflecting uncertainty
     * 
     * @param minMs Minimum hesitation duration (default: 500ms)
     * @param maxMs Maximum hesitation duration (default: 3000ms)
     * @return Actual hesitation duration in milliseconds
     */
    public long simulateHesitation(int minMs, int maxMs) {
        hesitationCount++;
        
        // Hesitation duration based on stress and attention
        float stressFactor = state.getStressLevel();
        float attentionFactor = 1.0f - state.getAttentionLevel();
        
        // Calculate base hesitation
        int baseHesitation = minMs + (int) ((maxMs - minMs) * state.getRandom().nextFloat());
        
        // Adjust duration based on emotional state
        long actualHesitation = (long) (baseHesitation * (1.0f + stressFactor + attentionFactor));
        
        try {
            Thread.sleep(actualHesitation);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        totalHesitationDuration += actualHesitation;
        return actualHesitation;
    }
    
    /**
     * Simulate navigation hesitation with default duration (500-3000ms)
     * @return Actual hesitation duration in milliseconds
     */
    public long simulateHesitation() {
        return simulateHesitation(500, 3000);
    }
    
    /**
     * Simulate frustrated rapid clicking on unresponsive element
     * Common real-world behavior when apps are slow
     * 
     * @param clickCount Number of rapid clicks to perform
     * @return Duration of clicking sequence
     */
    public long simulateFrustratedClicking(int clickCount) {
        interactionBurstCount++;
        long startTime = System.currentTimeMillis();
        
        // Rapid clicking with minimal delay (30-100ms per click)
        for (int i = 0; i < clickCount; i++) {
            int delay = 30 + state.getRandom().nextInt(70);
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        totalBurstDuration += System.currentTimeMillis() - startTime;
        return System.currentTimeMillis() - startTime;
    }
    
    /**
     * Simulate uncertainty-induced scrolling behavior
     * User scrolls up and down before deciding
     * 
     * @param scrollActions Number of scroll actions to perform
     * @return Duration of scrolling sequence
     */
    public long simulateUncertainScrolling(int scrollActions) {
        hesitationCount++;
        long startTime = System.currentTimeMillis();
        
        // Scroll with hesitation between actions
        for (int i = 0; i < scrollActions; i++) {
            // Brief pause before each scroll
            int pause = 200 + state.getRandom().nextInt(400);
            try {
                Thread.sleep(pause);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        totalHesitationDuration += System.currentTimeMillis() - startTime;
        return System.currentTimeMillis() - startTime;
    }
    
    /**
     * Calculate current frustration level based on recent interactions
     * Higher frustration = more bursts and hesitation
     * 
     * @return Frustration level (0.0 - 1.0)
     */
    public float calculateFrustrationLevel() {
        float baseFrustration = state.getStressLevel();
        
        // Recent bursts increase frustration
        float burstContribution = Math.min(0.2f, interactionBurstCount * 0.02f);
        
        // Recent hesitation increases frustration
        float hesitationContribution = Math.min(0.15f, hesitationCount * 0.015f);
        
        return Math.min(1.0f, baseFrustration + burstContribution + hesitationContribution);
    }
    
    /**
     * Check if user should abandon current action due to frustration
     * 
     * @return true if action should be abandoned
     */
    public boolean shouldAbandonDueToFrustration() {
        float frustration = calculateFrustrationLevel();
        float abandonProbability = frustration * 0.25f; // Max 25% chance at max frustration
        
        return state.getRandom().nextFloat() < abandonProbability;
    }
    
    /**
     * Simulate emotional recovery after frustration
     * Reduces stress and frustration over time
     * 
     * @param recoveryTimeMs Time for emotional recovery
     */
    public void simulateEmotionalRecovery(long recoveryTimeMs) {
        // Reset burst and hesitation counters periodically
        if (interactionBurstCount > 10) {
            interactionBurstCount = (int) (interactionBurstCount * 0.7);
        }
        
        if (hesitationCount > 10) {
            hesitationCount = (int) (hesitationCount * 0.7);
        }
    }
    
    /**
     * Get statistics for this session
     */
    public EmotionalStatistics getStatistics() {
        return new EmotionalStatistics(
            interactionBurstCount,
            hesitationCount,
            totalBurstDuration,
            totalHesitationDuration,
            calculateFrustrationLevel()
        );
    }
    
    /**
     * Statistics container for emotional bias behavior
     */
    public static class EmotionalStatistics {
        public final int interactionBurstCount;
        public final int hesitationCount;
        public final long totalBurstDurationMs;
        public final long totalHesitationDurationMs;
        public final float currentFrustrationLevel;
        
        public EmotionalStatistics(int interactionBurstCount, int hesitationCount,
                                   long totalBurstDurationMs, long totalHesitationDurationMs,
                                   float currentFrustrationLevel) {
            this.interactionBurstCount = interactionBurstCount;
            this.hesitationCount = hesitationCount;
            this.totalBurstDurationMs = totalBurstDurationMs;
            this.totalHesitationDurationMs = totalHesitationDurationMs;
            this.currentFrustrationLevel = currentFrustrationLevel;
        }
        
        @Override
        public String toString() {
            return String.format("EmotionalStats{bursts=%d, hesitations=%d, burstTime=%ds, hesitateTime=%ds, frustration=%.2f%%}",
                interactionBurstCount,
                hesitationCount,
                totalBurstDurationMs / 1000,
                totalHesitationDurationMs / 1000,
                currentFrustrationLevel * 100);
        }
    }
}
