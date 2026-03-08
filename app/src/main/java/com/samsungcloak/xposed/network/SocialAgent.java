package com.samsungcloak.xposed.network;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * SocialAgent - Represents an autonomous agent in the social network simulation.
 *
 * Each agent maintains its own cognitive state, content preferences, relationship
 * strengths with other agents, and interaction history. This model supports the
 * structural interaction topology required for high-fidelity community-scale testing.
 *
 * Mathematical Model:
 * - Agent State Vector: S = {cognitiveLoad, attentionLevel, activityLevel}
 * - Preference Vector: P = {p1, p2, ..., pn} where sum(pi) = 1.0
 * - Relationship Strength: R(i,j) ∈ [0, 1] decaying over time
 * - Interaction Propensity: I(i,j) = α·R(i,j) + β·H(i,j) + γ·T(i,j)
 *   where H = homophily, T = triadic closure
 */
public class SocialAgent {

    private final String agentId;
    private final String clusterId;
    private final long createdAt;

    private double cognitiveLoad;
    private double attentionLevel;
    private double activityLevel;

    private Map<String, Double> contentPreferences;
    private Map<String, Relationship> relationships;
    private Set<String> coreCluster;

    private long lastInteractionTime;
    private int totalInteractions;
    private double homophilyWeight;

    public enum EngagementState {
        HIGHLY_ACTIVE,    // > 0.8 activity level
        ACTIVE,           // 0.5 - 0.8
        MODERATE,         // 0.3 - 0.5
        PASSIVE,          // 0.1 - 0.3
        DORMANT           // < 0.1
    }

    public SocialAgent(String clusterId) {
        this.agentId = UUID.randomUUID().toString();
        this.clusterId = clusterId;
        this.createdAt = System.currentTimeMillis();
        this.cognitiveLoad = 0.3 + Math.random() * 0.4;
        this.attentionLevel = 0.5 + Math.random() * 0.5;
        this.activityLevel = 0.3 + Math.random() * 0.7;
        this.contentPreferences = new HashMap<>();
        this.relationships = new HashMap<>();
        this.coreCluster = new HashSet<>();
        this.lastInteractionTime = createdAt;
        this.totalInteractions = 0;
        this.homophilyWeight = 0.6 + Math.random() * 0.3;
    }

    /**
     * Initialize content preferences with random distribution.
     * Ensures preferences sum to 1.0 (probability distribution).
     */
    public void initializePreferences(Set<String> contentCategories) {
        double totalWeight = 0.0;
        Map<String, Double> initialPrefs = new HashMap<>();

        for (String category : contentCategories) {
            double weight = Math.random();
            initialPrefs.put(category, weight);
            totalWeight += weight;
        }

        for (Map.Entry<String, Double> entry : initialPrefs.entrySet()) {
            contentPreferences.put(entry.getKey(), entry.getValue() / totalWeight);
        }
    }

    /**
     * Calculate relationship strength using exponential decay model.
     * R(t) = R0 · e^(-λ·Δt) where λ = decay rate
     */
    public double getRelationshipStrength(String otherAgentId, double decayRate) {
        Relationship rel = relationships.get(otherAgentId);
        if (rel == null) {
            return 0.0;
        }

        long timeDelta = System.currentTimeMillis() - rel.getLastInteraction();
        double timeDeltaHours = timeDelta / (1000.0 * 60 * 60);

        return rel.getBaseStrength() * Math.exp(-decayRate * timeDeltaHours);
    }

    /**
     * Update relationship strength following successful interaction.
     * Uses reinforcement learning model: R_new = R_old + α·(1 - R_old)
     */
    public void strengthenRelationship(String otherAgentId, double reinforcementRate) {
        Relationship rel = relationships.computeIfAbsent(otherAgentId,
            k -> new Relationship(otherAgentId));

        double currentStrength = rel.getBaseStrength();
        double newStrength = currentStrength + reinforcementRate * (1.0 - currentStrength);
        rel.updateStrength(newStrength);
        rel.recordInteraction();
    }

    /**
     * Apply homophily-based preference adjustment.
     * Pi_new = (1-α)·Pi_old + α·Pcluster_majority
     */
    public void adjustPreferencesByHomophily(Map<String, Double> clusterMajorityPrefs,
                                              double adjustmentRate) {
        for (Map.Entry<String, Double> entry : clusterMajorityPrefs.entrySet()) {
            String category = entry.getKey();
            double clusterPref = entry.getValue();
            double currentPref = contentPreferences.getOrDefault(category, 0.0);

            double newPref = (1 - adjustmentRate) * currentPref + adjustmentRate * clusterPref;
            contentPreferences.put(category, newPref);
        }

        normalizePreferences();
    }

    /**
     * Normalize preference vector to ensure sum = 1.0
     */
    private void normalizePreferences() {
        double sum = contentPreferences.values().stream().mapToDouble(Double::doubleValue).sum();
        if (sum > 0) {
            for (Map.Entry<String, Double> entry : contentPreferences.entrySet()) {
                contentPreferences.put(entry.getKey(), entry.getValue() / sum);
            }
        }
    }

    /**
     * Calculate interaction propensity toward another agent.
     * I(i,j) = α·R(i,j) + β·H(i,j) + γ·T(i,j)
     */
    public double calculateInteractionPropensity(SocialAgent other, double triadicScore) {
        double relationshipStrength = getRelationshipStrength(other.getAgentId(), 0.01);
        double homophilyScore = calculateHomophilyScore(other);

        double alpha = 0.4;
        double beta = 0.35;
        double gamma = 0.25;

        double propensity = alpha * relationshipStrength +
                           beta * homophilyScore +
                           gamma * triadicScore;

        return Math.min(1.0, propensity * activityLevel);
    }

    /**
     * Calculate homophily score based on preference overlap.
     * H(i,j) = Σ√(Pi · Pj)  (Bhattacharyya coefficient)
     */
    public double calculateHomophilyScore(SocialAgent other) {
        double score = 0.0;
        Set<String> allCategories = new HashSet<>(contentPreferences.keySet());
        allCategories.addAll(other.getContentPreferences().keySet());

        for (String category : allCategories) {
            double p1 = contentPreferences.getOrDefault(category, 0.0);
            double p2 = other.getContentPreferences().getOrDefault(category, 0.0);
            score += Math.sqrt(p1 * p2);
        }

        return score;
    }

    /**
     * Record an interaction and update agent state.
     */
    public void recordInteraction(String targetAgentId) {
        totalInteractions++;
        lastInteractionTime = System.currentTimeMillis();

        activityLevel = Math.min(1.0, activityLevel + 0.01);
        cognitiveLoad = Math.min(1.0, cognitiveLoad + 0.005);

        strengthenRelationship(targetAgentId, 0.1);
    }

    /**
     * Decay relationship strengths over time (entropy hook).
     */
    public void applyEntropyDecay(double decayRate, long currentTime) {
        for (Relationship rel : relationships.values()) {
            long timeSinceInteraction = currentTime - rel.getLastInteraction();
            double hoursInactive = timeSinceInteraction / (1000.0 * 60 * 60);

            if (hoursInactive > 24) {
                double decayFactor = Math.exp(-decayRate * hoursInactive);
                double newStrength = rel.getBaseStrength() * decayFactor;
                rel.updateStrength(newStrength);
            }
        }

        activityLevel = Math.max(0.1, activityLevel * 0.999);
    }

    /**
     * Trigger re-engagement event for dormant relationships.
     */
    public void triggerReengagement(String otherAgentId, double reengagementBoost) {
        Relationship rel = relationships.get(otherAgentId);
        if (rel != null) {
            double newStrength = Math.min(1.0, rel.getBaseStrength() + reengagementBoost);
            rel.updateStrength(newStrength);
            rel.recordInteraction();
        }
    }

    public String getAgentId() {
        return agentId;
    }

    public String getClusterId() {
        return clusterId;
    }

    public Map<String, Double> getContentPreferences() {
        return new HashMap<>(contentPreferences);
    }

    public double getPreferenceForCategory(String category) {
        return contentPreferences.getOrDefault(category, 0.0);
    }

    public Set<String> getCoreCluster() {
        return new HashSet<>(coreCluster);
    }

    public void addToCoreCluster(String agentId) {
        coreCluster.add(agentId);
    }

    public boolean isInCoreCluster(String agentId) {
        return coreCluster.contains(agentId);
    }

    public EngagementState getEngagementState() {
        if (activityLevel > 0.8) return EngagementState.HIGHLY_ACTIVE;
        if (activityLevel > 0.5) return EngagementState.ACTIVE;
        if (activityLevel > 0.3) return EngagementState.MODERATE;
        if (activityLevel > 0.1) return EngagementState.PASSIVE;
        return EngagementState.DORMANT;
    }

    public double getActivityLevel() {
        return activityLevel;
    }

    public int getTotalInteractions() {
        return totalInteractions;
    }

    public Map<String, Relationship> getRelationships() {
        return new HashMap<>(relationships);
    }

    public long getLastInteractionTime() {
        return lastInteractionTime;
    }

    public double getHomophilyWeight() {
        return homophilyWeight;
    }

    /**
     * Inner class representing a relationship between agents.
     */
    public static class Relationship {
        private final String targetAgentId;
        private double baseStrength;
        private long lastInteraction;
        private int interactionCount;

        public Relationship(String targetAgentId) {
            this.targetAgentId = targetAgentId;
            this.baseStrength = 0.1 + Math.random() * 0.2;
            this.lastInteraction = System.currentTimeMillis();
            this.interactionCount = 0;
        }

        public void updateStrength(double newStrength) {
            this.baseStrength = Math.max(0.0, Math.min(1.0, newStrength));
        }

        public void recordInteraction() {
            this.lastInteraction = System.currentTimeMillis();
            this.interactionCount++;
        }

        public String getTargetAgentId() {
            return targetAgentId;
        }

        public double getBaseStrength() {
            return baseStrength;
        }

        public long getLastInteraction() {
            return lastInteraction;
        }

        public int getInteractionCount() {
            return interactionCount;
        }
    }
}
