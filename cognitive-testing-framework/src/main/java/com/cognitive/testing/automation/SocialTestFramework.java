package com.cognitive.testing.automation;

import com.cognitive.testing.model.SocialState;
import com.cognitive.testing.model.SocialConfig;
import com.cognitive.testing.hooks.*;

/**
 * Main Social Realism Test Framework
 * Integrates all social realism hooks for collective dynamics simulation.
 * 
 * Designed for use with Appium/UIAUTOMATOR2 for mobile automation.
 * 
 * Usage:
 * <pre>
 * SocialTestFramework framework = new SocialTestFramework(SocialConfig.highFidelity());
 * framework.startSession();
 * 
 * // Perform social actions with herd behavior
 * if (framework.shouldTriggerHerdEffect(HerdBehaviorHook.ActionType.LIKE)) {
 *     framework.performSocialAction(() -> clickLikeButton());
 * }
 * 
 * // Select content with social proof bias
 * Content selected = framework.selectWithSocialProof(contentList, Content::getViewCount);
 * 
 * // Check for viral surge
 * if (framework.isViralSurgeActive()) {
 *     List<String> trends = framework.getTrendingCategories(3);
 * }
 * 
 * // Adjust speed for group conformity
 * framework.adjustForConformity();
 * 
 * // Get next action from peer influence
 * PeerInfluenceHook.PeerActionType nextAction = framework.determinePeerAction();
 * 
 * framework.endSession();
 * System.out.println(framework.getSessionReport());
 * </pre>
 */
public class SocialTestFramework {
    
    private final SocialState state;
    private final HerdBehaviorHook herdBehaviorHook;
    private final SocialProofHook socialProofHook;
    private final TrendParticipationHook trendParticipationHook;
    private final GroupConformityHook groupConformityHook;
    private final PeerInfluenceHook peerInfluenceHook;
    
    private boolean sessionActive;
    private long sessionStartTime;
    
    /**
     * Create framework with default configuration
     */
    public SocialTestFramework() {
        this(SocialConfig.defaults());
    }
    
    /**
     * Create framework with custom configuration
     * 
     * @param config Social configuration
     */
    public SocialTestFramework(SocialConfig config) {
        this.state = new SocialState(config);
        this.herdBehaviorHook = new HerdBehaviorHook(state);
        this.socialProofHook = new SocialProofHook(state);
        this.trendParticipationHook = new TrendParticipationHook(state);
        this.groupConformityHook = new GroupConformityHook(state);
        this.peerInfluenceHook = new PeerInfluenceHook(state);
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
        
        // Reset all hooks
        herdBehaviorHook.resetSession();
        socialProofHook.resetSession();
        trendParticipationHook.resetSession();
        groupConformityHook.resetSession();
        peerInfluenceHook.resetSession();
        
        System.out.println("Social Realism Session Started: " + state.getSessionId());
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
        
        System.out.println("\nSocial Realism Session Ended");
        System.out.println("Duration: " + (sessionDuration / 1000) + " seconds");
        System.out.println(getSessionReport());
    }
    
    /**
     * Perform a social action with herd behavior applied
     * 
     * @param action The action to perform
     * @return true if action completed
     */
    public boolean performSocialAction(Runnable action) {
        if (!sessionActive) {
            throw new IllegalStateException("Session not active. Call startSession() first.");
        }
        
        // Check for herd effect delay
        if (herdBehaviorHook.shouldTriggerHerdEffect(HerdBehaviorHook.ActionType.LIKE)) {
            herdBehaviorHook.simulateHerdDelay();
        }
        
        // Perform the action
        action.run();
        
        // Record action in state
        state.recordInteraction();
        state.updateSocialState();
        
        return true;
    }
    
    // ============ Herd Behavior Methods ============
    
    /**
     * Check if herd effect should be triggered
     * 
     * @param triggerAction The initial action type
     * @return true if herd effect should influence behavior
     */
    public boolean shouldTriggerHerdEffect(HerdBehaviorHook.ActionType triggerAction) {
        return herdBehaviorHook.shouldTriggerHerdEffect(triggerAction);
    }
    
    /**
     * Apply herd effect to select from options
     * 
     * @param options List of options
     * @param getEngagementCount Function to get engagement count
     * @param <T> Option type
     * @return Selected option
     */
    public <T> T selectWithHerdEffect(java.util.List<T> options, 
                                      java.util.function.Function<T, Long> getEngagementCount) {
        return herdBehaviorHook.applyHerdEffect(options, getEngagementCount);
    }
    
    /**
     * Get current herd influence level
     */
    public float getHerdInfluence() {
        return herdBehaviorHook.getCurrentHerdInfluence();
    }
    
    // ============ Social Proof Methods ============
    
    /**
     * Check if social proof should influence selection
     */
    public boolean shouldCheckSocialProof() {
        return socialProofHook.shouldCheckSocialProof();
    }
    
    /**
     * Select with social proof bias
     * 
     * @param elements List of elements
     * @param getCount Function to get count
     * @param <T> Element type
     * @return Selected element
     */
    public <T> T selectWithSocialProof(java.util.List<T> elements,
                                        java.util.function.Function<T, Long> getCount) {
        return socialProofHook.selectWithSocialProof(elements, getCount);
    }
    
    /**
     * Select with proof label bias
     * 
     * @param labeledElements Map of elements to labels
     * @param <T> Element type
     * @return Selected element
     */
    public <T> T selectWithProofLabel(java.util.Map<T, String> labeledElements) {
        return socialProofHook.selectWithProofLabel(labeledElements);
    }
    
    /**
     * Check if should click based on social proof
     */
    public boolean shouldClickBasedOnProof(long viewCount, long likeCount, long commentCount) {
        return socialProofHook.shouldClickBasedOnProof(viewCount, likeCount, commentCount);
    }
    
    /**
     * Get current social proof susceptibility
     */
    public float getSocialProofSusceptibility() {
        return socialProofHook.getCurrentSusceptibility();
    }
    
    // ============ Trend Participation Methods ============
    
    /**
     * Check if viral surge is active
     */
    public boolean isViralSurgeActive() {
        return trendParticipationHook.isViralSurgeActive();
    }
    
    /**
     * Get current trending category
     */
    public String getCurrentTrend() {
        return trendParticipationHook.getCurrentTrend();
    }
    
    /**
     * Get viral surge intensity
     */
    public float getSurgeIntensity() {
        return trendParticipationHook.getSurgeIntensity();
    }
    
    /**
     * Check if should focus on trend
     */
    public boolean shouldFocusOnTrend() {
        return trendParticipationHook.shouldFocusOnTrend();
    }
    
    /**
     * Get trending categories
     */
    public java.util.List<String> getTrendingCategories(int maxCategories) {
        return trendParticipationHook.getTrendingCategories(maxCategories);
    }
    
    /**
     * Select with trend bias
     * 
     * @param contentList List of content
     * @param getCategory Function to get category
     * @param <T> Content type
     * @return Selected content
     */
    public <T> T selectWithTrendBias(java.util.List<T> contentList, 
                                       java.util.function.Function<T, String> getCategory) {
        return trendParticipationHook.selectWithTrendBias(contentList, getCategory);
    }
    
    /**
     * Get trend alignment
     */
    public float getTrendAlignment() {
        return trendParticipationHook.getTrendAlignment();
    }
    
    // ============ Group Conformity Methods ============
    
    /**
     * Adjust behavior for group conformity
     */
    public void adjustForConformity() {
        groupConformityHook.sampleCommunityBehavior();
    }
    
    /**
     * Get current interaction speed
     */
    public float getCurrentSpeed() {
        return groupConformityHook.getCurrentSpeed();
    }
    
    /**
     * Get observed community mean speed
     */
    public float getCommunityMeanSpeed() {
        return groupConformityHook.getObservedMeanSpeed();
    }
    
    /**
     * Calculate conformity-adjusted delay
     */
    public long calculateConformityDelay(long baseDelayMs) {
        return groupConformityHook.calculateConformityDelay(baseDelayMs);
    }
    
    /**
     * Select path with conformity adjustment
     * 
     * @param availablePaths List of paths
     * @param getPathSpeed Function to get path speed
     * @param <T> Path type
     * @return Selected path
     */
    public <T> T selectPathWithConformity(java.util.List<T> availablePaths,
                                            java.util.function.Function<T, Float> getPathSpeed) {
        return groupConformityHook.selectPathWithConformity(availablePaths, getPathSpeed);
    }
    
    /**
     * Check if conforming to community
     */
    public boolean isConforming() {
        return groupConformityHook.isConforming();
    }
    
    /**
     * Calculate adjusted timeout
     */
    public long calculateAdjustedTimeout(long baseTimeoutMs) {
        return groupConformityHook.calculateAdjustedTimeout(baseTimeoutMs);
    }
    
    // ============ Peer Influence Methods ============
    
    /**
     * Determine next action based on peer influence
     */
    public PeerInfluenceHook.PeerActionType determinePeerAction() {
        return peerInfluenceHook.determineActionFromPeers();
    }
    
    /**
     * Observe peer actions
     */
    public void observePeerActions() {
        peerInfluenceHook.observePeerActions();
    }
    
    /**
     * Get peer action distribution
     */
    public float[] getPeerActionDistribution() {
        return peerInfluenceHook.getPeerActionDistribution();
    }
    
    /**
     * Get most likely peer action
     */
    public PeerInfluenceHook.PeerActionType getMostLikelyPeerAction() {
        return peerInfluenceHook.getMostLikelyPeerAction();
    }
    
    /**
     * Select with peer influence
     * 
     * @param options List of options
     * @param getActionType Function to get action type
     * @param <T> Option type
     * @return Selected option
     */
    public <T> T selectWithPeerInfluence(java.util.List<T> options,
                                           java.util.function.Function<T, PeerInfluenceHook.PeerActionType> getActionType) {
        return peerInfluenceHook.selectWithPeerInfluence(options, getActionType);
    }
    
    /**
     * Get current peer influence weight
     */
    public float getPeerInfluenceWeight() {
        return peerInfluenceHook.getCurrentInfluenceWeight();
    }
    
    // ============ Combined Methods ============
    
    /**
     * Perform action with all social dynamics applied
     * This is the main entry point for social realism simulation
     * 
     * @param action The action to perform
     * @param actionContext Context about the action for social dynamics
     */
    public void performActionWithSocialDynamics(Runnable action, ActionContext actionContext) {
        if (!sessionActive) {
            throw new IllegalStateException("Session not active. Call startSession() first.");
        }
        
        // 1. Check trend focus
        if (trendParticipationHook.shouldFocusOnTrend()) {
            // Focus on trending content
            System.out.println("Focusing on trend: " + trendParticipationHook.getCurrentTrend());
        }
        
        // 2. Check peer influence
        if (peerInfluenceHook.shouldBePeerInfluenced()) {
            peerInfluenceHook.simulateInfluenceDelay();
            PeerInfluenceHook.PeerActionType peerAction = peerInfluenceHook.determineActionFromPeers();
            System.out.println("Peer-influenced action: " + peerAction.getName());
        }
        
        // 3. Apply herd behavior if applicable
        if (actionContext != null && actionContext.getTriggerAction() != null) {
            if (herdBehaviorHook.shouldTriggerHerdEffect(actionContext.getTriggerAction())) {
                herdBehaviorHook.simulateHerdDelay();
            }
        }
        
        // 4. Check social proof
        if (actionContext != null && actionContext.getViewCount() > 0) {
            if (!socialProofHook.shouldClickBasedOnProof(
                    actionContext.getViewCount(), 
                    actionContext.getLikeCount(), 
                    actionContext.getCommentCount())) {
                // Social proof doesn't support this action
                return;
            }
        }
        
        // 5. Apply conformity timing
        long delay = 0;
        if (actionContext != null && actionContext.getBaseDelayMs() > 0) {
            delay = groupConformityHook.calculateConformityDelay(actionContext.getBaseDelayMs());
        }
        
        if (delay > 0) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Perform the action
        action.run();
        
        // Update state
        state.recordInteraction();
        state.updateSocialState();
        
        // Share with peer group
        if (actionContext != null && actionContext.getActionType() != null) {
            peerInfluenceHook.shareActionWithPeers(actionContext.getActionType());
        }
    }
    
    /**
     * Simple version without context
     */
    public void performActionWithSocialDynamics(Runnable action) {
        performActionWithSocialDynamics(action, null);
    }
    
    /**
     * Context class for social dynamics
     */
    public static class ActionContext {
        private HerdBehaviorHook.ActionType triggerAction;
        private PeerInfluenceHook.PeerActionType actionType;
        private long viewCount;
        private long likeCount;
        private long commentCount;
        private long baseDelayMs;
        private String category;
        
        public ActionContext() {}
        
        public ActionContext triggerAction(HerdBehaviorHook.ActionType triggerAction) {
            this.triggerAction = triggerAction;
            return this;
        }
        
        public ActionContext actionType(PeerInfluenceHook.PeerActionType actionType) {
            this.actionType = actionType;
            return this;
        }
        
        public ActionContext viewCount(long viewCount) {
            this.viewCount = viewCount;
            return this;
        }
        
        public ActionContext likeCount(long likeCount) {
            this.likeCount = likeCount;
            return this;
        }
        
        public ActionContext commentCount(long commentCount) {
            this.commentCount = commentCount;
            return this;
        }
        
        public ActionContext baseDelayMs(long baseDelayMs) {
            this.baseDelayMs = baseDelayMs;
            return this;
        }
        
        public ActionContext category(String category) {
            this.category = category;
            return this;
        }
        
        // Getters
        public HerdBehaviorHook.ActionType getTriggerAction() { return triggerAction; }
        public PeerInfluenceHook.PeerActionType getActionType() { return actionType; }
        public long getViewCount() { return viewCount; }
        public long getLikeCount() { return likeCount; }
        public long getCommentCount() { return commentCount; }
        public long getBaseDelayMs() { return baseDelayMs; }
        public String getCategory() { return category; }
    }
    
    /**
     * Get comprehensive session report
     */
    public String getSessionReport() {
        StringBuilder report = new StringBuilder();
        report.append("\n=== SOCIAL REALISM REPORT ===\n");
        
        // Session info
        report.append(String.format("Session: %s\n", state.getSessionId()));
        report.append(String.format("Duration: %d minutes\n", state.getSessionDuration() / 60000));
        report.append(String.format("Interactions: %d\n", state.getInteractionCount()));
        
        // Social state
        report.append(String.format("\n--- Social State ---\n"));
        report.append(String.format("Herd Influence: %.1f%%\n", state.getHerdInfluenceLevel() * 100));
        report.append(String.format("Social Proof Susceptibility: %.1f%%\n", 
            state.getSocialProofSusceptibility() * 100));
        report.append(String.format("Trend Alignment: %.1f%%\n", state.getTrendAlignment() * 100));
        report.append(String.format("Conformity Factor: %.1f%%\n", state.getConformityFactor() * 100));
        report.append(String.format("Peer Influence Weight: %.1f%%\n", state.getPeerInfluenceWeight() * 100));
        
        // Viral surge status
        if (state.isViralSurgeActive()) {
            report.append(String.format("\n--- Viral Surge Active ---\n"));
            report.append(String.format("Trend: %s\n", state.getCurrentTrendingCategory()));
            report.append(String.format("Intensity: %.1f%%\n", state.getViralSurgeIntensity() * 100));
        }
        
        // Hook statistics
        report.append(String.format("\n--- Herd Behavior ---\n"));
        report.append(herdBehaviorHook.getStatistics().toString());
        
        report.append(String.format("\n--- Social Proof ---\n"));
        report.append(socialProofHook.getStatistics().toString());
        
        report.append(String.format("\n--- Trend Participation ---\n"));
        report.append(trendParticipationHook.getStatistics().toString());
        
        report.append(String.format("\n--- Group Conformity ---\n"));
        report.append(groupConformityHook.getStatistics().toString());
        
        report.append(String.format("\n--- Peer Influence ---\n"));
        report.append(peerInfluenceHook.getStatistics().toString());
        
        report.append("\n==============================\n");
        
        return report.toString();
    }
    
    /**
     * Get social state for advanced usage
     */
    public SocialState getSocialState() {
        return state;
    }
    
    /**
     * Get herd behavior hook for advanced usage
     */
    public HerdBehaviorHook getHerdBehaviorHook() {
        return herdBehaviorHook;
    }
    
    /**
     * Get social proof hook for advanced usage
     */
    public SocialProofHook getSocialProofHook() {
        return socialProofHook;
    }
    
    /**
     * Get trend participation hook for advanced usage
     */
    public TrendParticipationHook getTrendParticipationHook() {
        return trendParticipationHook;
    }
    
    /**
     * Get group conformity hook for advanced usage
     */
    public GroupConformityHook getGroupConformityHook() {
        return groupConformityHook;
    }
    
    /**
     * Get peer influence hook for advanced usage
     */
    public PeerInfluenceHook getPeerInfluenceHook() {
        return peerInfluenceHook;
    }
}
