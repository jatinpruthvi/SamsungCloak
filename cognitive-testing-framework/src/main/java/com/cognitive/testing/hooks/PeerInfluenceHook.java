package com.cognitive.testing.hooks;

import com.cognitive.testing.model.SocialState;
import com.cognitive.testing.model.SocialConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Hook: Community-Driven Decisions (Peer Influence)
 * Implements "Influence Weighting" where the agent's next action is determined by
 * a weighted probability of what its simulated "peer group" is currently doing.
 * 
 * Real-world behavior:
 * - Users observe and copy peer behavior
 * - Friends' actions influence personal decisions
 * - Community engagement patterns drive individual actions
 * - Critical for testing cascade effects and viral spread
 */
public class PeerInfluenceHook {
    
    private final SocialState state;
    private int peerInfluencedActions;
    private int independentActions;
    private long totalInfluenceDelay;
    private int influenceObservationCount;
    
    // Simulated peer group size
    private static final int PEER_GROUP_SIZE = 10;
    
    // Action types in peer influence model
    public enum PeerActionType {
        LIKE("like", 0.3f),
        COMMENT("comment", 0.15f),
        SHARE("share", 0.05f),
        VIEW("view", 0.3f),
        FOLLOW("follow", 0.1f),
        REACT("react", 0.05f),
        SAVE("save", 0.03f),
        REPORT("report", 0.01f),
        SKIP("skip", 0.005f),
        SEARCH("search", 0.015f);
        
        private final String name;
        private final float baseWeight;
        
        PeerActionType(String name, float baseWeight) {
            this.name = name;
            this.baseWeight = baseWeight;
        }
        
        public String getName() {
            return name;
        }
        
        public float getBaseWeight() {
            return baseWeight;
        }
    }
    
    public PeerInfluenceHook(SocialState state) {
        this.state = state;
        this.peerInfluencedActions = 0;
        this.independentActions = 0;
        this.totalInfluenceDelay = 0;
        this.influenceObservationCount = 0;
    }
    
    /**
     * Simulate observing peer group actions
     * Updates internal model of what peers are doing
     * Should be called periodically to refresh peer action data
     */
    public void observePeerActions() {
        influenceObservationCount++;
        
        // Get peer action distribution from social state
        float[] distribution = state.getPeerActionDistribution();
        
        // Simulate peer actions with some randomness
        Random random = state.getRandom();
        
        // Randomly "observe" a peer action based on distribution
        float randomValue = random.nextFloat();
        float cumulative = 0;
        
        for (int i = 0; i < distribution.length; i++) {
            cumulative += distribution[i];
            if (randomValue <= cumulative) {
                // Map index to action type
                PeerActionType[] actions = PeerActionType.values();
                PeerActionType observedAction = actions[i % actions.length];
                
                // Update social state with this observation
                state.observePeerAction(observedAction.getName());
                break;
            }
        }
    }
    
    /**
     * Check if next action should be influenced by peer behavior
     * Uses weighted probability based on peer influence configuration
     * 
     * @return true if peer influence should determine next action
     */
    public boolean shouldBePeerInfluenced() {
        state.updateSocialState();
        
        // Get current peer influence weight
        float influenceWeight = state.getPeerInfluenceWeight();
        
        // Add some randomness
        return state.getRandom().nextFloat() < influenceWeight;
    }
    
    /**
     * Determine next action based on peer influence weighting
     * Uses weighted probability of peer actions
     * 
     * @return Selected action type based on peer influence
     */
    public PeerActionType determineActionFromPeers() {
        // Get current peer action distribution
        float[] distribution = state.getPeerActionDistribution();
        PeerActionType[] actions = PeerActionType.values();
        
        Random random = state.getRandom();
        
        // If not peer influenced, use base weights
        if (!shouldBePeerInfluenced()) {
            independentActions++;
            
            // Select based on base weights
            float totalWeight = 0;
            for (PeerActionType action : actions) {
                totalWeight += action.getBaseWeight();
            }
            
            float randomValue = random.nextFloat() * totalWeight;
            float cumulative = 0;
            
            for (PeerActionType action : actions) {
                cumulative += action.getBaseWeight();
                if (randomValue <= cumulative) {
                    return action;
                }
            }
            
            return PeerActionType.VIEW; // Default
        }
        
        // Peer-influenced selection
        peerInfluencedActions++;
        
        // Combine base weights with peer distribution
        float[] combinedWeights = new float[actions.length];
        
        for (int i = 0; i < actions.length; i++) {
            float peerWeight = i < distribution.length ? distribution[i] : 0.1f;
            float baseWeight = actions[i].getBaseWeight();
            
            // Blend peer influence with base tendency
            combinedWeights[i] = baseWeight * (1 - state.getPeerInfluenceWeight()) + 
                peerWeight * state.getPeerInfluenceWeight();
        }
        
        // Normalize weights
        float totalWeight = 0;
        for (float w : combinedWeights) {
            totalWeight += w;
        }
        
        // Weighted random selection
        float randomValue = random.nextFloat() * totalWeight;
        float cumulative = 0;
        
        for (int i = 0; i < actions.length; i++) {
            cumulative += combinedWeights[i];
            if (randomValue <= cumulative) {
                return actions[i];
            }
        }
        
        return PeerActionType.VIEW;
    }
    
    /**
     * Simulate peer influence delay
     * Users often pause to consider what peers are doing
     * 
     * @return Delay in milliseconds
     */
    public long simulateInfluenceDelay() {
        Random random = state.getRandom();
        
        // Delay based on peer influence weight
        // Higher influence = more time observing peers
        float influence = state.getPeerInfluenceWeight();
        int baseDelay = (int) (influence * 1500); // 0-1500ms
        
        int delay = baseDelay + random.nextInt(500);
        
        totalInfluenceDelay += delay;
        
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return delay;
    }
    
    /**
     * Get the probability distribution of peer actions
     * 
     * @return Copy of peer action distribution
     */
    public float[] getPeerActionDistribution() {
        return state.getPeerActionDistribution();
    }
    
    /**
     * Get the most likely next action based on peer behavior
     * 
     * @return Most popular peer action
     */
    public PeerActionType getMostLikelyPeerAction() {
        float[] distribution = state.getPeerActionDistribution();
        PeerActionType[] actions = PeerActionType.values();
        
        int maxIndex = 0;
        float maxValue = distribution[0];
        
        for (int i = 1; i < Math.min(distribution.length, actions.length); i++) {
            if (distribution[i] > maxValue) {
                maxValue = distribution[i];
                maxIndex = i;
            }
        }
        
        return actions[maxIndex];
    }
    
    /**
     * Apply peer influence to select from a list of options
     * 
     * @param options List of options
     * @param getActionType Function to get action type from option
     * @param <T> Option type
     * @return Selected option considering peer influence
     */
    public <T> T selectWithPeerInfluence(List<T> options,
                                          java.util.function.Function<T, PeerActionType> getActionType) {
        if (options == null || options.isEmpty()) {
            return null;
        }
        
        // Periodically observe peer actions
        if (influenceObservationCount % 5 == 0) {
            observePeerActions();
        }
        
        // Simulate influence delay
        simulateInfluenceDelay();
        
        // Determine action type from peers
        PeerActionType targetAction = determineActionFromPeers();
        
        // Calculate weights for each option based on peer alignment
        float totalWeight = 0;
        java.util.Map<T, Float> weights = new java.util.HashMap<>();
        
        for (T option : options) {
            PeerActionType optionAction = getActionType.apply(option);
            float weight = calculatePeerAlignmentWeight(optionAction, targetAction);
            weights.put(option, weight);
            totalWeight += weight;
        }
        
        // Weighted random selection
        float randomValue = state.getRandom().nextFloat() * totalWeight;
        float cumulative = 0;
        
        for (T option : options) {
            cumulative += weights.get(option);
            if (randomValue <= cumulative) {
                return option;
            }
        }
        
        return options.get(options.size() - 1);
    }
    
    /**
     * Calculate how well an option aligns with peer behavior
     */
    private float calculatePeerAlignmentWeight(PeerActionType optionAction, PeerActionType targetAction) {
        if (optionAction == targetAction) {
            // Direct match - high weight
            return 3.0f;
        }
        
        // Check for related actions (same category)
        if (areRelatedActions(optionAction, targetAction)) {
            return 1.5f;
        }
        
        // Base weight
        return 0.5f;
    }
    
    /**
     * Check if two action types are related
     */
    private boolean areRelatedActions(PeerActionType a, PeerActionType b) {
        // Engagement actions are related
        List<PeerActionType> engagementActions = List.of(
            PeerActionType.LIKE, PeerActionType.COMMENT, PeerActionType.REACT
        );
        
        if (engagementActions.contains(a) && engagementActions.contains(b)) {
            return true;
        }
        
        // Discovery actions are related
        List<PeerActionType> discoveryActions = List.of(
            PeerActionType.VIEW, PeerActionType.SEARCH, PeerActionType.FOLLOW
        );
        
        if (discoveryActions.contains(a) && discoveryActions.contains(b)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Simulate updating peer group with a new action
     * 
     * @param actionType The action type to share with peer group
     */
    public void shareActionWithPeers(PeerActionType actionType) {
        // Record action in social state
        state.recordAction(actionType.getName());
        
        // Observe this action in peer distribution
        state.observePeerAction(actionType.getName());
    }
    
    /**
     * Get the current peer influence weight
     * 
     * @return Current influence weight (0.0 - 1.0)
     */
    public float getCurrentInfluenceWeight() {
        return state.getPeerInfluenceWeight();
    }
    
    /**
     * Get the peer action observation weight from config
     * 
     * @return Configuration observation weight
     */
    public float getObservationWeight() {
        return state.getConfig().getPeerActionObservationWeight();
    }
    
    /**
     * Calculate influence strength based on peer group engagement
     * 
     * @return Influence strength (0.0 - 1.0)
     */
    public float calculateInfluenceStrength() {
        float[] distribution = getPeerActionDistribution();
        
        // Calculate entropy of distribution
        // Higher entropy = more uniform (less influence)
        // Lower entropy = more concentrated (stronger influence)
        float entropy = 0;
        
        for (float p : distribution) {
            if (p > 0) {
                entropy -= p * Math.log(p);
            }
        }
        
        // Normalize entropy (max entropy = log(n))
        int n = distribution.length;
        float maxEntropy = (float) Math.log(n);
        float normalizedEntropy = entropy / maxEntropy;
        
        // Influence is inverse of normalized entropy
        return 1.0f - normalizedEntropy;
    }
    
    /**
     * Reset peer influence state for a new session
     */
    public void resetSession() {
        peerInfluencedActions = 0;
        independentActions = 0;
        totalInfluenceDelay = 0;
        influenceObservationCount = 0;
    }
    
    /**
     * Get statistics for this session
     */
    public PeerInfluenceStatistics getStatistics() {
        int totalActions = peerInfluencedActions + independentActions;
        float influenceRate = totalActions > 0 ? 
            (float) peerInfluencedActions / totalActions : 0;
        
        return new PeerInfluenceStatistics(
            peerInfluencedActions,
            independentActions,
            influenceRate,
            totalInfluenceDelay,
            calculateInfluenceStrength(),
            getCurrentInfluenceWeight(),
            influenceObservationCount
        );
    }
    
    /**
     * Statistics container for peer influence behavior
     */
    public static class PeerInfluenceStatistics {
        public final int peerInfluencedActions;
        public final int independentActions;
        public final float influenceRate;
        public final long totalInfluenceDelayMs;
        public final float influenceStrength;
        public final float influenceWeight;
        public final int observationCount;
        
        public PeerInfluenceStatistics(int peerInfluencedActions, int independentActions,
                                       float influenceRate, long totalInfluenceDelayMs,
                                       float influenceStrength, float influenceWeight,
                                       int observationCount) {
            this.peerInfluencedActions = peerInfluencedActions;
            this.independentActions = independentActions;
            this.influenceRate = influenceRate;
            this.totalInfluenceDelayMs = totalInfluenceDelayMs;
            this.influenceStrength = influenceStrength;
            this.influenceWeight = influenceWeight;
            this.observationCount = observationCount;
        }
        
        @Override
        public String toString() {
            return String.format("PeerInfluenceStats{influenced=%d, independent=%d, " +
                "influenceRate=%.1f%%, delayTotal=%dms, strength=%.2f, weight=%.1f%%, observations=%d}",
                peerInfluencedActions, independentActions, influenceRate * 100,
                totalInfluenceDelayMs, influenceStrength, influenceWeight * 100, observationCount);
        }
    }
}
