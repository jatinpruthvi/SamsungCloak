package com.cognitive.testing.hooks;

import com.cognitive.testing.model.CognitiveState;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Hook: Limited Attention Span
 * Simulates context switching and mid-flow abandonment to test session persistence.
 * 
 * Real-world behavior:
 * - Users get distracted and leave tasks mid-flow
 * - Attention shifts to notifications or other apps
 * - Sessions may be abandoned and later resumed
 * - Context switches occur based on fatigue and time of day
 */
public class LimitedAttentionHook {
    
    private final CognitiveState state;
    private final Deque<String> navigationStack;
    private int contextSwitchCount;
    private long totalAbandonmentTime;
    
    public LimitedAttentionHook(CognitiveState state) {
        this.state = state;
        this.navigationStack = new ArrayDeque<>();
        this.contextSwitchCount = 0;
        this.totalAbandonmentTime = 0;
    }
    
    /**
     * Call before each interaction to check for context switching
     * @return true if context switch should occur (abandon current flow)
     */
    public boolean shouldTriggerContextSwitch() {
        state.recordInteraction();
        
        // Check if cognitive state indicates low attention
        if (state.shouldContextSwitch()) {
            contextSwitchCount++;
            return true;
        }
        
        return false;
    }
    
    /**
     * Simulate mid-flow abandonment
     * Pauses execution for a random duration to simulate user distraction
     * 
     * @param minPauseMs Minimum pause duration (default: 5000ms = 5 seconds)
     * @param maxPauseMs Maximum pause duration (default: 60000ms = 1 minute)
     * @return Actual pause duration in milliseconds
     */
    public long simulateAbandonment(int minPauseMs, int maxPauseMs) {
        if (!state.shouldContextSwitch()) {
            return 0;
        }
        
        // Calculate pause duration based on attention level
        // Lower attention = longer abandonment
        float attentionFactor = 1.0f - state.getAttentionLevel(); // 0.0 (high attention) to 0.7 (low)
        int basePause = minPauseMs + (int) ((maxPauseMs - minPauseMs) * state.getRandom().nextFloat());
        long actualPause = (long) (basePause * (1.0f + attentionFactor * 2.0f));
        
        try {
            Thread.sleep(actualPause);
            totalAbandonmentTime += actualPause;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return actualPause;
    }
    
    /**
     * Simulate mid-flow abandonment with default duration (5-30 seconds)
     * @return Actual pause duration in milliseconds
     */
    public long simulateAbandonment() {
        return simulateAbandonment(5000, 30000);
    }
    
    /**
     * Track current navigation context for potential abandonment
     * @param screenId Identifier for current screen/flow
     */
    public void enterNavigationContext(String screenId) {
        navigationStack.push(screenId);
    }
    
    /**
     * Leave current navigation context
     */
    public void leaveNavigationContext() {
        if (!navigationStack.isEmpty()) {
            navigationStack.pop();
        }
    }
    
    /**
     * Get current navigation depth (number of screens deep)
     * Used to determine abandonment likelihood (deeper = higher chance)
     */
    public int getNavigationDepth() {
        return navigationStack.size();
    }
    
    /**
     * Calculate abandonment probability based on current state
     * Factors: navigation depth, fatigue, attention, stress
     * 
     * @return Probability of abandonment (0.0 - 1.0)
     */
    public float calculateAbandonmentProbability() {
        float baseProbability = 0.02f; // 2% base chance
        
        // Navigation depth increases probability
        float depthFactor = Math.min(0.15f, getNavigationDepth() * 0.02f);
        
        // Fatigue increases probability
        float fatigueFactor = Math.min(0.20f, state.getFatigueLevel() * 0.08f);
        
        // Low attention increases probability
        float attentionFactor = (1.0f - state.getAttentionLevel()) * 0.15f;
        
        // High stress increases probability
        float stressFactor = state.getStressLevel() * 0.10f;
        
        return Math.min(0.50f, baseProbability + depthFactor + fatigueFactor + attentionFactor + stressFactor);
    }
    
    /**
     * Simulate returning to abandoned session
     * Adjusts cognitive state to reflect time elapsed
     * 
     * @param timeElapsedMs Time since abandonment began
     */
    public void simulateReturnToSession(long timeElapsedMs) {
        // If abandonment was short (< 1 minute), attention recovers slightly
        if (timeElapsedMs < 60000) {
            // Partial attention recovery
        } else {
            // Long abandonment may reset some fatigue but also adds confusion
            // User may need to reorient
        }
    }
    
    /**
     * Check if user should check notifications during flow
     * Simulates real-world distraction behavior
     */
    public boolean shouldCheckNotifications() {
        // 5% base chance, modified by attention level
        float baseChance = 0.05f;
        float attentionModifier = (1.0f - state.getAttentionLevel()) * 0.10f;
        return state.getRandom().nextFloat() < (baseChance + attentionModifier);
    }
    
    /**
     * Simulate notification check (brief pause)
     * @return Duration of notification check in milliseconds
     */
    public long simulateNotificationCheck() {
        int duration = 2000 + state.getRandom().nextInt(8000); // 2-10 seconds
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return duration;
    }
    
    /**
     * Get statistics for this session
     */
    public AttentionStatistics getStatistics() {
        return new AttentionStatistics(
            contextSwitchCount,
            totalAbandonmentTime,
            navigationStack.size(),
            calculateAbandonmentProbability()
        );
    }
    
    /**
     * Statistics container for attention behavior
     */
    public static class AttentionStatistics {
        public final int contextSwitchCount;
        public final long totalAbandonmentTimeMs;
        public final int currentNavigationDepth;
        public final float currentAbandonmentProbability;
        
        public AttentionStatistics(int contextSwitchCount, long totalAbandonmentTimeMs,
                                   int currentNavigationDepth, float currentAbandonmentProbability) {
            this.contextSwitchCount = contextSwitchCount;
            this.totalAbandonmentTimeMs = totalAbandonmentTimeMs;
            this.currentNavigationDepth = currentNavigationDepth;
            this.currentAbandonmentProbability = currentAbandonmentProbability;
        }
        
        @Override
        public String toString() {
            return String.format("AttentionStatistics{switches=%d, abandonmentTime=%ds, depth=%d, prob=%.2f%%}",
                contextSwitchCount,
                totalAbandonmentTimeMs / 1000,
                currentNavigationDepth,
                currentAbandonmentProbability * 100);
        }
    }
}
