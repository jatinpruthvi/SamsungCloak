package com.cognitive.testing.automation;

import com.cognitive.testing.model.CognitiveState;
import com.cognitive.testing.model.CognitiveConfig;
import com.cognitive.testing.hooks.*;

/**
 * Main Cognitive Realism Test Framework
 * Integrates all cognitive hooks for human-like Android testing.
 * 
 * Designed for use with Appium/UIAutomator2 for mobile automation.
 * 
 * Usage:
 * <pre>
 * CognitiveTestFramework framework = new CognitiveTestFramework(CognitiveConfig.highFidelity());
 * framework.startSession();
 * 
 * // Perform actions with cognitive realism
 * framework.performAction(() -> clickButton());
 * framework.selectOption(options, evaluator);
 * framework.navigateTo(screenId);
 * 
 * // Check for cognitive behaviors
 * if (framework.shouldContextSwitch()) {
 *     framework.simulateAbandonment();
 * }
 * 
 * framework.endSession();
 * System.out.println(framework.getSessionReport());
 * </pre>
 */
public class CognitiveTestFramework {
    
    private final CognitiveState state;
    private final LimitedAttentionHook attentionHook;
    private final BoundedRationalityHook boundedRationalityHook;
    private final EmotionalBiasHook emotionalBiasHook;
    private final DecisionFatigueHook decisionFatigueHook;
    private final ImperfectMemoryHook memoryHook;
    private final ChangingPreferencesHook preferencesHook;
    
    private boolean sessionActive;
    private long sessionStartTime;
    
    /**
     * Create framework with default configuration
     */
    public CognitiveTestFramework() {
        this(CognitiveConfig.defaults());
    }
    
    /**
     * Create framework with custom configuration
     * 
     * @param config Cognitive configuration
     */
    public CognitiveTestFramework(CognitiveConfig config) {
        this.state = new CognitiveState(config);
        this.attentionHook = new LimitedAttentionHook(state);
        this.boundedRationalityHook = new BoundedRationalityHook(state);
        this.emotionalBiasHook = new EmotionalBiasHook(state);
        this.decisionFatigueHook = new DecisionFatigueHook(state);
        this.memoryHook = new ImperfectMemoryHook(state);
        this.preferencesHook = new ChangingPreferencesHook(state);
        this.sessionActive = false;
    }
    
    /**
     * Start a new test session
     */
    public void startSession() {
        if (sessionActive) {
            endSession();
        }
        
        sessionActive = true;
        sessionStartTime = System.currentTimeMillis();
        state.resetSession();
        
        System.out.println("Cognitive Session Started: " + state.getSessionId());
    }
    
    /**
     * End current test session and generate report
     */
    public void endSession() {
        if (!sessionActive) {
            return;
        }
        
        sessionActive = false;
        long sessionDuration = System.currentTimeMillis() - sessionStartTime;
        
        System.out.println("\nCognitive Session Ended");
        System.out.println("Duration: " + (sessionDuration / 1000) + " seconds");
        System.out.println(getSessionReport());
    }
    
    /**
     * Perform an action with cognitive realism applied
     * Handles timing, errors, and emotional state
     * 
     * @param action The action to perform
     * @return true if action completed, false if abandoned
     */
    public boolean performAction(Runnable action) {
        if (!sessionActive) {
            throw new IllegalStateException("Session not active. Call startSession() first.");
        }
        
        // Check for decision fatigue - think time
        int thinkTime = decisionFatigueHook.calculateThinkTime();
        if (thinkTime > 0) {
            try {
                Thread.sleep(thinkTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        
        // Check for errors
        if (decisionFatigueHook.shouldCommitError()) {
            DecisionFatigueHook.DecisionErrorType errorType = 
                decisionFatigueHook.getNextErrorType();
            
            System.out.println("Cognitive Error: " + errorType);
            
            // Some errors might be recoverable, others might fail the action
            if (errorType == DecisionFatigueHook.DecisionErrorType.MISCLICK) {
                // Mis-click might still perform action, just incorrectly
                action.run();
                return true;
            } else if (errorType == DecisionFatigueHook.DecisionErrorType.SKIPPED_ELEMENT) {
                // Skip the action entirely
                return false;
            }
            // Other errors - try to perform action anyway
        }
        
        // Check for emotional bias - hesitation before action
        if (emotionalBiasHook.shouldTriggerHesitation()) {
            long hesitationDuration = emotionalBiasHook.simulateHesitation();
            System.out.println("Hesitated for " + hesitationDuration + "ms");
        }
        
        // Check for emotional bias - interaction burst
        if (emotionalBiasHook.shouldTriggerInteractionBurst()) {
            int burstCount = emotionalBiasHook.simulateInteractionBurst();
            System.out.println("Interaction burst: " + burstCount + " rapid actions");
            action.run(); // First action in burst
            return true;
        }
        
        // Perform the action
        action.run();
        
        return true;
    }
    
    /**
     * Select an option using bounded rationality (satisficing)
     * 
     * @param options List of options
     * @param evaluator Option quality evaluator
     * @param acceptableThreshold Minimum acceptable quality
     * @return Selected option
     */
    public <T> T selectOption(java.util.List<T> options, 
                              BoundedRationalityHook.OptionEvaluator<T> evaluator,
                              float acceptableThreshold) {
        return boundedRationalityHook.selectOption(options, evaluator, acceptableThreshold);
    }
    
    /**
     * Navigate to a new screen
     * Tracks navigation for memory and attention hooks
     * 
     * @param screenId Screen identifier
     */
    public void navigateTo(String screenId) {
        attentionHook.enterNavigationContext(screenId);
        memoryHook.visitScreen(screenId);
        preferencesHook.visitScreen(screenId);
        
        System.out.println("Navigated to: " + screenId);
    }
    
    /**
     * Leave current screen
     * 
     * @param screenId Screen identifier
     */
    public void leaveScreen(String screenId) {
        attentionHook.leaveNavigationContext();
        memoryHook.leaveScreen();
        System.out.println("Left screen: " + screenId);
    }
    
    /**
     * Check if context switch should occur (limited attention)
     * 
     * @return true if should context switch
     */
    public boolean shouldContextSwitch() {
        return attentionHook.shouldTriggerContextSwitch();
    }
    
    /**
     * Simulate mid-flow abandonment
     * 
     * @param minPauseMs Minimum pause duration
     * @param maxPauseMs Maximum pause duration
     * @return Actual pause duration
     */
    public long simulateAbandonment(int minPauseMs, int maxPauseMs) {
        return attentionHook.simulateAbandonment(minPauseMs, maxPauseMs);
    }
    
    /**
     * Simulate mid-flow abandonment with default duration
     */
    public long simulateAbandonment() {
        return attentionHook.simulateAbandonment();
    }
    
    /**
     * Check if re-verification should occur (imperfect memory)
     * 
     * @return Number of screens to go back, 0 if no re-verification
     */
    public int shouldReverify() {
        return memoryHook.simulateReverification();
    }
    
    /**
     * Determine if gesture or button should be used
     * 
     * @return true if gesture, false if button
     */
    public boolean shouldUseGesture() {
        return preferencesHook.shouldUseGesture();
    }
    
    /**
     * Transition to a new session
     * Resets metrics and may change preferences
     */
    public void transitionToNewSession() {
        preferencesHook.transitionToNewSession();
        memoryHook.clearHistory();
        decisionFatigueHook.resetSessionMetrics();
        
        System.out.println("Transitioned to new session");
    }
    
    /**
     * Check if user should check notifications (distraction)
     * 
     * @return true if should check notifications
     */
    public boolean shouldCheckNotifications() {
        return attentionHook.shouldCheckNotifications();
    }
    
    /**
     * Simulate notification check
     */
    public void simulateNotificationCheck() {
        long duration = attentionHook.simulateNotificationCheck();
        System.out.println("Checked notifications for " + duration + "ms");
    }
    
    /**
     * Get comprehensive session report
     */
    public String getSessionReport() {
        StringBuilder report = new StringBuilder();
        report.append("\n=== COGNITIVE REALITY REPORT ===\n");
        
        // Session info
        report.append(String.format("Session: %s\n", state.getSessionId()));
        report.append(String.format("Duration: %d minutes\n", 
            state.getSessionDuration() / 60000));
        report.append(String.format("Interactions: %d\n", state.getInteractionCount()));
        
        // Cognitive state
        report.append(String.format("\n--- Cognitive State ---\n"));
        report.append(String.format("Attention: %.1f%%\n", state.getAttentionLevel() * 100));
        report.append(String.format("Fatigue: %.2f\n", state.getFatigueLevel()));
        report.append(String.format("Stress: %.1f%%\n", state.getStressLevel() * 100));
        report.append(String.format("Error Rate: %.2f%%\n", state.getDecisionErrorRate() * 100));
        
        // Hook statistics
        report.append(String.format("\n--- Limited Attention ---\n"));
        report.append(attentionHook.getStatistics().toString());
        
        report.append(String.format("\n--- Decision Fatigue ---\n"));
        report.append(decisionFatigueHook.getStatistics().toString());
        
        report.append(String.format("\n--- Emotional Bias ---\n"));
        report.append(emotionalBiasHook.getStatistics().toString());
        
        report.append(String.format("\n--- Imperfect Memory ---\n"));
        report.append(memoryHook.getStatistics().toString());
        
        report.append(String.format("\n--- Changing Preferences ---\n"));
        report.append(preferencesHook.analyzePreferences().toString());
        
        report.append("\n==============================\n");
        
        return report.toString();
    }
    
    /**
     * Get cognitive state for advanced usage
     */
    public CognitiveState getCognitiveState() {
        return state;
    }
    
    /**
     * Get attention hook for advanced usage
     */
    public LimitedAttentionHook getAttentionHook() {
        return attentionHook;
    }
    
    /**
     * Get bounded rationality hook for advanced usage
     */
    public BoundedRationalityHook getBoundedRationalityHook() {
        return boundedRationalityHook;
    }
    
    /**
     * Get emotional bias hook for advanced usage
     */
    public EmotionalBiasHook getEmotionalBiasHook() {
        return emotionalBiasHook;
    }
    
    /**
     * Get decision fatigue hook for advanced usage
     */
    public DecisionFatigueHook getDecisionFatigueHook() {
        return decisionFatigueHook;
    }
    
    /**
     * Get memory hook for advanced usage
     */
    public ImperfectMemoryHook getMemoryHook() {
        return memoryHook;
    }
    
    /**
     * Get preferences hook for advanced usage
     */
    public ChangingPreferencesHook getPreferencesHook() {
        return preferencesHook;
    }
}
