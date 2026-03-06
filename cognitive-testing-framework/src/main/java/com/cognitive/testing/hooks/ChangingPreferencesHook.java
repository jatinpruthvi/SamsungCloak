package com.cognitive.testing.hooks;

import com.cognitive.testing.model.CognitiveState;
import com.cognitive.testing.model.NavigationPreference;
import com.cognitive.testing.model.InteractionStyle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hook: Changing Preferences
 * Simulates varying interaction styles between sessions (gesture-heavy vs button-heavy navigation).
 * 
 * Real-world behavior:
 * - Users change interaction patterns over time
 * - Different contexts favor different navigation styles
 * - Learning curves affect preference evolution
 * - Mood and context influence interaction choices
 * 
 * Tests: UI flexibility, multiple interaction paths, accessibility, UX adaptability
 */
public class ChangingPreferencesHook {
    
    private final CognitiveState state;
    private final List<PreferenceTransition> transitionHistory;
    private final Map<String, Integer> screenVisitCounts;
    private int sessionCount;
    private int preferenceChangeCount;
    
    public ChangingPreferencesHook(CognitiveState state) {
        this.state = state;
        this.transitionHistory = new ArrayList<>();
        this.screenVisitCounts = new HashMap<>();
        this.sessionCount = 1;
        this.preferenceChangeCount = 0;
    }
    
    /**
     * Check if navigation preference should change
     * Simulates switching between gesture-heavy and button-heavy navigation
     * 
     * @return true if preference should change
     */
    public boolean shouldChangeNavigationPreference() {
        state.recordInteraction();
        
        // Base probability from config (embedded in state.varyPreferences)
        // Check internal cognitive factors
        float changeProbability = 0.15f; // 15% base chance per session
        
        // High interaction count may trigger preference change
        if (state.getInteractionCount() > 50) {
            changeProbability += 0.10f;
        }
        
        // Time-based change (every 10-15 minutes)
        long sessionDuration = state.getSessionDuration();
        if (sessionDuration > 600000 && state.getRandom().nextFloat() < 0.3f) {
            changeProbability += 0.20f;
        }
        
        return state.getRandom().nextFloat() < changeProbability;
    }
    
    /**
     * Change navigation preference
     * Returns the new preference
     * 
     * @return New navigation preference
     */
    public NavigationPreference changeNavigationPreference() {
        NavigationPreference oldPref = state.getNavPreference();
        state.varyPreferences();
        NavigationPreference newPref = state.getNavPreference();
        
        if (oldPref != newPref) {
            preferenceChangeCount++;
            transitionHistory.add(new PreferenceTransition(
                sessionCount,
                System.currentTimeMillis(),
                PreferenceType.NAVIGATION,
                oldPref.name(),
                newPref.name(),
                "Navigation preference changed"
            ));
        }
        
        return newPref;
    }
    
    /**
     * Check if interaction style should change
     * Simulates switching between precise, casual, and erratic styles
     * 
     * @return true if style should change
     */
    public boolean shouldChangeInteractionStyle() {
        state.recordInteraction();
        
        float changeProbability = 0.20f; // 20% base chance per session
        
        // Stress increases style changes (user gets frustrated)
        if (state.getStressLevel() > 0.7f) {
            changeProbability += 0.15f;
        }
        
        // Fatigue may lead to more casual/erratic style
        if (state.getFatigueLevel() > 1.5f) {
            changeProbability += 0.10f;
        }
        
        return state.getRandom().nextFloat() < changeProbability;
    }
    
    /**
     * Change interaction style
     * Returns the new style
     * 
     * @return New interaction style
     */
    public InteractionStyle changeInteractionStyle() {
        InteractionStyle oldStyle = state.getInteractionStyle();
        state.varyPreferences();
        InteractionStyle newStyle = state.getInteractionStyle();
        
        if (oldStyle != newStyle) {
            preferenceChangeCount++;
            transitionHistory.add(new PreferenceTransition(
                sessionCount,
                System.currentTimeMillis(),
                PreferenceType.INTERACTION,
                oldStyle.name(),
                newStyle.name(),
                "Interaction style changed"
            ));
        }
        
        return newStyle;
    }
    
    /**
     * Determine if gesture or button should be used for navigation
     * Based on current navigation preference
     * 
     * @return true if gesture should be used, false if button
     */
    public boolean shouldUseGesture() {
        NavigationPreference pref = state.getNavPreference();
        float gestureProbability = pref.getGestureProbability();
        
        // Adjust probability based on cognitive state
        // High fatigue = prefer buttons (less effort)
        gestureProbability *= (1.0f - state.getFatigueLevel() / 4.0f);
        
        // Low attention = prefer buttons (simpler)
        gestureProbability *= state.getAttentionLevel();
        
        // Stress may lead to either preference (unpredictable)
        if (state.getStressLevel() > 0.6f) {
            float stressAdjustment = (state.getRandom().nextFloat() - 0.5f) * 0.2f;
            gestureProbability += stressAdjustment;
        }
        
        return state.getRandom().nextFloat() < Math.max(0.1f, Math.min(0.9f, gestureProbability));
    }
    
    /**
     * Track screen visit for preference evolution
     * Certain screens may favor specific navigation styles
     * 
     * @param screenId Screen identifier
     */
    public void visitScreen(String screenId) {
        screenVisitCounts.put(screenId, screenVisitCounts.getOrDefault(screenId, 0) + 1);
        
        state.recordInteraction();
        
        // After visiting same screen multiple times, user may develop preference
        int visitCount = screenVisitCounts.get(screenId);
        if (visitCount == 3 || visitCount == 7) {
            // Learning point - consider preference change
            if (state.getRandom().nextFloat() < 0.25f) {
                changeNavigationPreference();
            }
        }
    }
    
    /**
     * Simulate session transition
     * Resets some metrics and may change preferences
     */
    public void transitionToNewSession() {
        sessionCount++;
        
        // Reset interaction count
        state.resetSession();
        
        // 30-40% chance of preference change on new session
        if (state.getRandom().nextFloat() < 0.35f) {
            // Randomly change one or both preferences
            boolean changeNav = state.getRandom().nextBoolean();
            boolean changeStyle = state.getRandom().nextBoolean();
            
            if (changeNav) {
                changeNavigationPreference();
            }
            
            if (changeStyle) {
                changeInteractionStyle();
            }
        }
    }
    
    /**
     * Calculate preference stability score
     * Higher score = more consistent preferences
     * 
     * @return Stability score (0.0 - 1.0)
     */
    public float calculatePreferenceStability() {
        if (sessionCount <= 1) {
            return 1.0f;
        }
        
        int maxPossibleChanges = sessionCount * 2; // nav + style per session
        float changeRatio = (float) preferenceChangeCount / maxPossibleChanges;
        
        return Math.max(0.0f, 1.0f - changeRatio);
    }
    
    /**
     * Get current gesture vs button preference ratio
     * 
     * @return Gesture preference (0.0 = always button, 1.0 = always gesture)
     */
    public float getGesturePreferenceRatio() {
        return state.getNavPreference().getGestureProbability();
    }
    
    /**
     * Analyze preference patterns over time
     * 
     * @return Analysis of preference changes
     */
    public PreferenceAnalysis analyzePreferences() {
        if (transitionHistory.isEmpty()) {
            return new PreferenceAnalysis(
                0,
                0,
                0,
                state.getNavPreference().name(),
                state.getInteractionStyle().name(),
                1.0f
            );
        }
        
        int navChanges = 0;
        int styleChanges = 0;
        
        for (PreferenceTransition transition : transitionHistory) {
            if (transition.type == PreferenceType.NAVIGATION) {
                navChanges++;
            } else {
                styleChanges++;
            }
        }
        
        return new PreferenceAnalysis(
            navChanges,
            styleChanges,
            preferenceChangeCount,
            state.getNavPreference().name(),
            state.getInteractionStyle().name(),
            calculatePreferenceStability()
        );
    }
    
    /**
     * Get transition history
     */
    public List<PreferenceTransition> getTransitionHistory() {
        return new ArrayList<>(transitionHistory);
    }
    
    /**
     * Preference transition record
     */
    public static class PreferenceTransition {
        public final int sessionNumber;
        public final long timestamp;
        public final PreferenceType type;
        public final String oldValue;
        public final String newValue;
        public final String reason;
        
        public PreferenceTransition(int sessionNumber, long timestamp, PreferenceType type,
                                   String oldValue, String newValue, String reason) {
            this.sessionNumber = sessionNumber;
            this.timestamp = timestamp;
            this.type = type;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.reason = reason;
        }
        
        @Override
        public String toString() {
            return String.format("Transition{session=%d, type=%s, %s -> %s}",
                sessionNumber, type, oldValue, newValue);
        }
    }
    
    /**
     * Preference type enum
     */
    public enum PreferenceType {
        NAVIGATION,
        INTERACTION
    }
    
    /**
     * Preference analysis result
     */
    public static class PreferenceAnalysis {
        public final int navigationChanges;
        public final int interactionStyleChanges;
        public final int totalChanges;
        public final String currentNavigationPreference;
        public final String currentInteractionStyle;
        public final float stabilityScore;
        
        public PreferenceAnalysis(int navigationChanges, int interactionStyleChanges,
                                 int totalChanges, String currentNavigationPreference,
                                 String currentInteractionStyle, float stabilityScore) {
            this.navigationChanges = navigationChanges;
            this.interactionStyleChanges = interactionStyleChanges;
            this.totalChanges = totalChanges;
            this.currentNavigationPreference = currentNavigationPreference;
            this.currentInteractionStyle = currentInteractionStyle;
            this.stabilityScore = stabilityScore;
        }
        
        @Override
        public String toString() {
            return String.format("PreferenceAnalysis{navChanges=%d, styleChanges=%d, total=%d, " +
                               "nav='%s', style='%s', stability=%.2f%%}",
                navigationChanges, interactionStyleChanges, totalChanges,
                currentNavigationPreference, currentInteractionStyle, stabilityScore * 100);
        }
    }
}
