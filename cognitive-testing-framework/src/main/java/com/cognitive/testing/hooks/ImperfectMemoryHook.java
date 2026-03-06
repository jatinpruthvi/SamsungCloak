package com.cognitive.testing.hooks;

import com.cognitive.testing.model.CognitiveState;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Hook: Imperfect Memory
 * Simulates re-verification behaviors where the script periodically regresses to previous screens.
 * 
 * Real-world behavior:
 * - Users forget what they just saw or entered
 * - Navigate back to verify information
 * - Re-check previous screens for context
 * - Memory decay affects information retention
 * 
 * Tests: Navigation flow, session persistence, data validation, UX clarity
 */
public class ImperfectMemoryHook {
    
    private final CognitiveState state;
    private final Deque<String> screenHistory;
    private final Map<String, Long> lastVisitedTime;
    private int reverificationCount;
    private int totalRegressionDepth;
    private long totalReverificationTime;
    
    public ImperfectMemoryHook(CognitiveState state) {
        this.state = state;
        this.screenHistory = new ArrayDeque<>();
        this.lastVisitedTime = new HashMap<>();
        this.reverificationCount = 0;
        this.totalRegressionDepth = 0;
        this.totalReverificationTime = 0;
    }
    
    /**
     * Record screen visit for memory tracking
     * 
     * @param screenId Unique identifier for the screen
     */
    public void visitScreen(String screenId) {
        state.recordInteraction();
        screenHistory.push(screenId);
        lastVisitedTime.put(screenId, System.currentTimeMillis());
    }
    
    /**
     * Leave current screen
     */
    public void leaveScreen() {
        if (!screenHistory.isEmpty()) {
            screenHistory.pop();
        }
    }
    
    /**
     * Check if re-verification should occur
     * Simulates user forgetting information and checking back
     * 
     * @return true if should re-verify on previous screen
     */
    public boolean shouldReverify() {
        state.recordInteraction();
        
        // Check cognitive state
        if (!state.shouldReverify()) {
            return false;
        }
        
        // Need at least one previous screen to go back to
        if (screenHistory.size() < 2) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Calculate re-verification probability based on factors
     * 
     * @return Probability of re-verification (0.0 - 1.0)
     */
    public float calculateReverificationProbability() {
        // Base probability from config
        float baseProbability = state.getConfig().getReverificationProbability();
        
        // Low attention increases probability
        float attentionFactor = (1.0f - state.getAttentionLevel()) * 0.20f;
        
        // High fatigue increases probability
        float fatigueFactor = Math.min(0.15f, state.getFatigueLevel() * 0.05f);
        
        // Navigation depth increases probability (more to remember)
        int depth = screenHistory.size();
        float depthFactor = Math.min(0.10f, depth * 0.02f);
        
        // Time since last visit to previous screen
        if (screenHistory.size() >= 2) {
            String previousScreen = screenHistory.peek();
            Long lastVisit = lastVisitedTime.get(previousScreen);
            if (lastVisit != null) {
                long timeSinceVisit = System.currentTimeMillis() - lastVisit;
                long minutesSinceVisit = timeSinceVisit / 60000;
                float timeFactor = Math.min(0.15f, minutesSinceVisit / 10.0f); // Max after 10 minutes
                baseProbability += timeFactor;
            }
        }
        
        return Math.min(0.50f, baseProbability + attentionFactor + fatigueFactor + depthFactor);
    }
    
    /**
     * Simulate re-verification by navigating back
     * Returns how many screens to go back
     * 
     * @return Number of screens to navigate back
     */
    public int simulateReverification() {
        if (!shouldReverify()) {
            return 0;
        }
        
        reverificationCount++;
        
        // Calculate regression depth (how many screens back)
        int maxDepth = Math.min(3, screenHistory.size() - 1);
        int depth;
        
        // Depth based on cognitive state
        float attentionDeficit = 1.0f - state.getAttentionLevel();
        float fatigueLevel = state.getFatigueLevel();
        
        // Lower attention + higher fatigue = go further back
        float depthProbability = attentionDeficit + (fatigueLevel / 5.0f);
        
        if (state.getRandom().nextFloat() < depthProbability) {
            // Go back 2-3 screens
            depth = 2 + state.getRandom().nextInt(maxDepth - 1);
        } else {
            // Go back 1 screen
            depth = 1;
        }
        
        totalRegressionDepth += depth;
        
        // Simulate time spent on re-verification
        int verificationTime = 2000 + state.getRandom().nextInt(4000); // 2-6 seconds
        try {
            Thread.sleep(verificationTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        totalReverificationTime += verificationTime;
        
        return depth;
    }
    
    /**
     * Simulate memory decay for specific information
     * Returns probability that information is forgotten
     * 
     * @param timeSinceLearnedMs Time since information was learned
     * @param importance Information importance (0.0 - 1.0)
     * @return Probability of forgetting (0.0 - 1.0)
     */
    public float calculateMemoryDecay(long timeSinceLearnedMs, float importance) {
        // Base decay rate
        float decayRate = 0.001f; // 0.1% per second
        
        // Adjust by importance (more important = remembered longer)
        decayRate *= (1.0f - importance * 0.7f);
        
        // Adjust by attention level
        decayRate *= (2.0f - state.getAttentionLevel());
        
        // Adjust by fatigue
        decayRate *= (1.0f + state.getFatigueLevel() / 2.0f);
        
        long seconds = timeSinceLearnedMs / 1000;
        float decayAmount = 1.0f - (float) Math.exp(-decayRate * seconds);
        
        return Math.min(0.80f, decayAmount); // Max 80% chance of forgetting
    }
    
    /**
     * Check if specific information should be re-verified
     * 
     * @param infoId Identifier for the information
     * @param timeLearnedMs When the information was learned
     * @param importance Information importance (0.0 - 1.0)
     * @return true if should re-verify this information
     */
    public boolean shouldReverifyInformation(String infoId, long timeLearnedMs, float importance) {
        state.recordInteraction();
        
        long timeSinceLearned = System.currentTimeMillis() - timeLearnedMs;
        float forgetProbability = calculateMemoryDecay(timeSinceLearned, importance);
        
        return state.getRandom().nextFloat() < forgetProbability;
    }
    
    /**
     * Simulate checking previous data entry
     * User goes back to verify what they entered
     * 
     * @return Duration of verification in milliseconds
     */
    public long simulateDataVerification() {
        reverificationCount++;
        
        // Verification time based on complexity and cognitive state
        int baseTime = 1500 + state.getRandom().nextInt(2500); // 1.5-4 seconds
        
        // Low attention = longer verification
        float attentionFactor = 2.0f - state.getAttentionLevel();
        
        // High fatigue = longer verification
        float fatigueFactor = 1.0f + state.getFatigueLevel() / 3.0f;
        
        long actualTime = (long) (baseTime * attentionFactor * fatigueFactor);
        
        try {
            Thread.sleep(actualTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        totalReverificationTime += actualTime;
        return actualTime;
    }
    
    /**
     * Simulate context confusion
     * User forgets where they are in the flow
     * 
     * @return true if user is confused about current context
     */
    public boolean isConfusedAboutContext() {
        state.recordInteraction();
        
        // Confusion probability increases with depth and fatigue
        int depth = screenHistory.size();
        float baseProbability = depth * 0.02f; // 2% per screen depth
        
        // Fatigue increases confusion
        baseProbability += state.getFatigueLevel() * 0.08f;
        
        // Low attention increases confusion
        baseProbability += (1.0f - state.getAttentionLevel()) * 0.10f;
        
        return state.getRandom().nextFloat() < Math.min(0.40f, baseProbability);
    }
    
    /**
     * Get current navigation depth
     */
    public int getNavigationDepth() {
        return screenHistory.size();
    }
    
    /**
     * Get time since last visit to specific screen
     */
    public long getTimeSinceLastVisit(String screenId) {
        Long lastVisit = lastVisitedTime.get(screenId);
        if (lastVisit == null) {
            return Long.MAX_VALUE;
        }
        return System.currentTimeMillis() - lastVisit;
    }
    
    /**
     * Clear screen history (e.g., after completing a major flow)
     */
    public void clearHistory() {
        screenHistory.clear();
        lastVisitedTime.clear();
    }
    
    /**
     * Get statistics for this session
     */
    public MemoryStatistics getStatistics() {
        int avgRegressionDepth = reverificationCount > 0 ? 
                                 totalRegressionDepth / reverificationCount : 0;
        float avgVerificationTime = reverificationCount > 0 ?
                                   (float) totalReverificationTime / reverificationCount : 0.0f;
        
        return new MemoryStatistics(
            reverificationCount,
            totalRegressionDepth,
            avgRegressionDepth,
            totalReverificationTime,
            avgVerificationTime,
            screenHistory.size(),
            calculateReverificationProbability()
        );
    }
    
    /**
     * Statistics container for imperfect memory behavior
     */
    public static class MemoryStatistics {
        public final int reverificationCount;
        public final int totalRegressionDepth;
        public final int averageRegressionDepth;
        public final long totalReverificationTimeMs;
        public final float averageVerificationTimeMs;
        public final int currentNavigationDepth;
        public final float currentReverificationProbability;
        
        public MemoryStatistics(int reverificationCount, int totalRegressionDepth,
                               int averageRegressionDepth, long totalReverificationTimeMs,
                               float averageVerificationTimeMs, int currentNavigationDepth,
                               float currentReverificationProbability) {
            this.reverificationCount = reverificationCount;
            this.totalRegressionDepth = totalRegressionDepth;
            this.averageRegressionDepth = averageRegressionDepth;
            this.totalReverificationTimeMs = totalReverificationTimeMs;
            this.averageVerificationTimeMs = averageVerificationTimeMs;
            this.currentNavigationDepth = currentNavigationDepth;
            this.currentReverificationProbability = currentReverificationProbability;
        }
        
        @Override
        public String toString() {
            return String.format("MemoryStats{reverifications=%d, avgDepth=%d, verifyTime=%.1fs, depth=%d, prob=%.2f%%}",
                reverificationCount,
                averageRegressionDepth,
                averageVerificationTimeMs / 1000,
                currentNavigationDepth,
                currentReverificationProbability * 100);
        }
    }
}
