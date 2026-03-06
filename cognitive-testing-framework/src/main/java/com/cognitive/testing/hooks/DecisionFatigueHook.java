package com.cognitive.testing.hooks;

import com.cognitive.testing.model.CognitiveState;

/**
 * Hook: Decision Fatigue
 * Simulates increasing error rates and slower response times as test session duration increases.
 * 
 * Real-world behavior:
 * - Decision quality degrades over time
 * - Response times increase with fatigue
 * - Error rates rise after sustained mental effort
 * - Users make more mistakes when tired
 * 
 * Tests: Session durability, error handling, long-form UX, cognitive load management
 */
public class DecisionFatigueHook {
    
    private final CognitiveState state;
    private int totalInteractions;
    private int errorCount;
    private long totalResponseTime;
    private int slowResponseCount;
    
    public DecisionFatigueHook(CognitiveState state) {
        this.state = state;
        this.totalInteractions = 0;
        this.errorCount = 0;
        this.totalResponseTime = 0;
        this.slowResponseCount = 0;
    }
    
    /**
     * Record an interaction and update fatigue metrics
     * Should be called before each UI interaction
     */
    public void recordInteraction() {
        state.recordInteraction();
        totalInteractions++;
    }
    
    /**
     * Check if decision error should occur based on current fatigue
     * Simulates mis-clicks, wrong selections, typos
     * 
     * @return true if error should occur
     */
    public boolean shouldCommitError() {
        recordInteraction();
        
        float errorRate = state.getDecisionErrorRate();
        
        // Additional error probability based on recent interaction density
        float interactionDensity = calculateRecentInteractionDensity();
        errorRate += interactionDensity * 0.05f;
        
        // Check if error occurs
        boolean errorOccurs = state.getRandom().nextFloat() < errorRate;
        
        if (errorOccurs) {
            errorCount++;
        }
        
        return errorOccurs;
    }
    
    /**
     * Calculate current error rate based on all factors
     * 
     * @return Current error rate (0.0 - 1.0)
     */
    public float calculateCurrentErrorRate() {
        float baseErrorRate = state.getDecisionErrorRate();
        
        // Session duration factor (longer sessions = higher error rate)
        long sessionDurationMs = state.getSessionDuration();
        int sessionMinutes = (int) (sessionDurationMs / 60000);
        float durationFactor = Math.min(0.10f, sessionMinutes / 300.0f); // Max after 5 hours
        
        // Interaction count factor (more decisions = more fatigue)
        float interactionFactor = Math.min(0.08f, totalInteractions / 500.0f);
        
        return Math.min(state.getConfig().getMaxErrorRate(),
                       baseErrorRate + durationFactor + interactionFactor);
    }
    
    /**
     * Calculate think time before next interaction based on fatigue
     * Simulates slower cognitive processing when tired
     * 
     * @return Think time in milliseconds
     */
    public int calculateThinkTime() {
        recordInteraction();
        
        int baseMinTime = state.getConfig().getMinThinkTime();
        int baseMaxTime = state.getConfig().getMaxThinkTime();
        
        // Calculate base think time
        int baseThinkTime = baseMinTime + (int) ((baseMaxTime - baseMinTime) * state.getRandom().nextFloat());
        
        // Fatigue slows down thinking
        float fatigueFactor = 1.0f + (state.getFatigueLevel() * (state.getConfig().getFatigueSlowdownMultiplier() - 1.0f));
        
        // Low attention slows down thinking
        float attentionFactor = 1.0f + ((1.0f - state.getAttentionLevel()) * 0.5f);
        
        // High stress can speed up or slow down (random)
        float stressFactor = 1.0f;
        if (state.getStressLevel() > 0.7f) {
            stressFactor = 0.8f + (state.getRandom().nextFloat() * 0.4f); // 0.8x to 1.2x
        }
        
        int adjustedThinkTime = (int) (baseThinkTime * fatigueFactor * attentionFactor * stressFactor);
        
        totalResponseTime += adjustedThinkTime;
        
        // Track slow responses (> 2x base)
        if (adjustedThinkTime > baseMaxTime * 2) {
            slowResponseCount++;
        }
        
        return adjustedThinkTime;
    }
    
    /**
     * Simulate mis-click behavior
     * Returns offset from intended click target
     * 
     * @param accuracy Required accuracy (0.0 - 1.0, 1.0 = perfect)
     * @return Offset in pixels (positive = off-target)
     */
    public int simulateMisClick(float accuracy) {
        recordInteraction();
        
        // Error rate affects accuracy
        float errorRate = calculateCurrentErrorRate();
        float effectiveAccuracy = accuracy * (1.0f - errorRate);
        
        if (state.getRandom().nextFloat() < effectiveAccuracy) {
            return 0; // Perfect click
        }
        
        // Calculate offset based on error severity
        int maxOffset = 20 + (int) (state.getFatigueLevel() * 10);
        int offset = state.getRandom().nextInt(maxOffset);
        
        // Random direction (positive or negative)
        if (state.getRandom().nextBoolean()) {
            offset = -offset;
        }
        
        return offset;
    }
    
    /**
     * Simulate decision error type
     * Returns what type of error should occur
     * 
     * @return Error type
     */
    public DecisionErrorType getNextErrorType() {
        if (!shouldCommitError()) {
            return DecisionErrorType.NONE;
        }
        
        // Distribute error types based on cognitive state
        float stressLevel = state.getStressLevel();
        float fatigueLevel = state.getFatigueLevel();
        
        float rand = state.getRandom().nextFloat();
        
        // High stress -> more wrong selections
        if (stressLevel > 0.6f && rand < 0.4f) {
            return DecisionErrorType.WRONG_SELECTION;
        }
        
        // High fatigue -> more mis-clicks
        if (fatigueLevel > 1.0f && rand < 0.7f) {
            return DecisionErrorType.MISCLICK;
        }
        
        // Low attention -> typos and skipped elements
        if (state.getAttentionLevel() < 0.6f && rand < 0.5f) {
            return state.getRandom().nextBoolean() ? 
                   DecisionErrorType.TYPO : DecisionErrorType.SKIPPED_ELEMENT;
        }
        
        // Random error type
        DecisionErrorType[] types = {
            DecisionErrorType.MISCLICK,
            DecisionErrorType.WRONG_SELECTION,
            DecisionErrorType.TYPO,
            DecisionErrorType.SKIPPED_ELEMENT,
            DecisionErrorType.PREMATURE_ACTION
        };
        
        return types[state.getRandom().nextInt(types.length)];
    }
    
    /**
     * Calculate interaction density (interactions per minute)
     * High density can cause rapid fatigue
     */
    private float calculateRecentInteractionDensity() {
        long sessionDurationMs = state.getSessionDuration();
        if (sessionDurationMs < 60000) {
            return 0.0f; // Not enough data
        }
        
        int sessionMinutes = (int) (sessionDurationMs / 60000);
        float interactionsPerMinute = (float) totalInteractions / sessionMinutes;
        
        // Normalize: > 30 interactions/minute is high density
        return Math.min(1.0f, interactionsPerMinute / 30.0f);
    }
    
    /**
     * Check if decision should be delayed due to cognitive load
     * 
     * @return true if decision should be delayed
     */
    public boolean shouldDelayDecision() {
        recordInteraction();
        
        // High fatigue = more delays
        float delayProbability = Math.min(0.15f, state.getFatigueLevel() * 0.05f);
        
        // High interaction density = more delays
        delayProbability += calculateRecentInteractionDensity() * 0.08f;
        
        return state.getRandom().nextFloat() < delayProbability;
    }
    
    /**
     * Calculate decision quality score
     * Decreases as fatigue increases
     * 
     * @return Quality score (0.0 - 1.0)
     */
    public float calculateDecisionQuality() {
        float baseQuality = 1.0f;
        
        // Fatigue reduces quality
        float fatiguePenalty = state.getFatigueLevel() * 0.15f;
        
        // Low attention reduces quality
        float attentionPenalty = (1.0f - state.getAttentionLevel()) * 0.20f;
        
        // Stress reduces quality
        float stressPenalty = state.getStressLevel() * 0.10f;
        
        float quality = baseQuality - fatiguePenalty - attentionPenalty - stressPenalty;
        return Math.max(0.4f, quality); // Minimum 40% quality
    }
    
    /**
     * Reset session-specific metrics while preserving cognitive state
     */
    public void resetSessionMetrics() {
        totalInteractions = 0;
        errorCount = 0;
        totalResponseTime = 0;
        slowResponseCount = 0;
    }
    
    /**
     * Get statistics for this session
     */
    public FatigueStatistics getStatistics() {
        float errorRate = totalInteractions > 0 ? 
                         (float) errorCount / totalInteractions : 0.0f;
        float avgResponseTime = totalInteractions > 0 ?
                               (float) totalResponseTime / totalInteractions : 0.0f;
        float slowResponseRate = totalInteractions > 0 ?
                                 (float) slowResponseCount / totalInteractions : 0.0f;
        
        return new FatigueStatistics(
            totalInteractions,
            errorCount,
            errorRate,
            avgResponseTime,
            slowResponseCount,
            slowResponseRate,
            calculateCurrentErrorRate(),
            calculateDecisionQuality()
        );
    }
    
    /**
     * Decision error types
     */
    public enum DecisionErrorType {
        NONE,
        MISCLICK,
        WRONG_SELECTION,
        TYPO,
        SKIPPED_ELEMENT,
        PREMATURE_ACTION
    }
    
    /**
     * Statistics container for decision fatigue behavior
     */
    public static class FatigueStatistics {
        public final int totalInteractions;
        public final int errorCount;
        public final float actualErrorRate;
        public final float averageResponseTimeMs;
        public final int slowResponseCount;
        public final float slowResponseRate;
        public final float currentErrorRate;
        public final float decisionQuality;
        
        public FatigueStatistics(int totalInteractions, int errorCount, float actualErrorRate,
                                 float averageResponseTimeMs, int slowResponseCount, float slowResponseRate,
                                 float currentErrorRate, float decisionQuality) {
            this.totalInteractions = totalInteractions;
            this.errorCount = errorCount;
            this.actualErrorRate = actualErrorRate;
            this.averageResponseTimeMs = averageResponseTimeMs;
            this.slowResponseCount = slowResponseCount;
            this.slowResponseRate = slowResponseRate;
            this.currentErrorRate = currentErrorRate;
            this.decisionQuality = decisionQuality;
        }
        
        @Override
        public String toString() {
            return String.format("FatigueStats{interactions=%d, errors=%d, errorRate=%.2f%%, avgResponse=%.0fms, slowRate=%.2f%%, quality=%.2f%%}",
                totalInteractions,
                errorCount,
                actualErrorRate * 100,
                averageResponseTimeMs,
                slowResponseRate * 100,
                decisionQuality * 100);
        }
    }
}
