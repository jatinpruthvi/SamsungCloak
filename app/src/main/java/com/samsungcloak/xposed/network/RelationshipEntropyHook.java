package com.samsungcloak.xposed.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * RelationshipEntropyHook - Implements relationship strength decay over time.
 *
 * Mathematical Foundation:
 * Real relationships require maintenance. Without interaction, relationship
 * strength decays exponentially following the entropy model:
 *
 * R(t) = R₀ × e^(-λ×Δt) × (1 - δ×D)
 *
 * Where:
 * - R₀ = initial relationship strength
 * - λ = decay rate (varies by relationship tier)
 * - Δt = time since last interaction
 * - δ = distance penalty factor
 * - D = network distance from core cluster
 *
 * Decay Rates by Tier:
 * - Core relationships: λ = 0.001 (very slow decay)
 * - Sympathy group: λ = 0.005 (moderate decay)
 * - Outer network: λ = 0.02 (fast decay)
 * - Acquaintances: λ = 0.05 (rapid decay)
 *
 * Re-engagement Event:
 * When a dormant relationship is reactivated, strength receives a boost:
 * R_new = min(1.0, R_decayed + β×(1 - R_decayed) + γ×N_shared)
 * where β = re-engagement coefficient, N_shared = shared interactions since
 */
public class RelationshipEntropyHook {

    private static final String LOG_TAG = "SamsungCloak.RelationshipEntropy";

    private final Random random;
    private final double baseDecayRate;
    private final double reengagementBoost;

    private Map<String, Long> lastDecayTimestamp;
    private List<DecayEvent> decayHistory;
    private List<ReengagementEvent> reengagementHistory;

    public enum DecayTier {
        CORE(0.001, 0.85),           // Core cluster: minimal decay
        SYMPATHY(0.005, 0.50),       // Sympathy group: slow decay
        OUTER(0.02, 0.15),           // Outer network: moderate decay
        ACQUAINTANCE(0.05, 0.05);    // Weak ties: rapid decay

        private final double decayRate;
        private final double strengthThreshold;

        DecayTier(double decayRate, double strengthThreshold) {
            this.decayRate = decayRate;
            this.strengthThreshold = strengthThreshold;
        }

        public double getDecayRate() {
            return decayRate;
        }

        public double getStrengthThreshold() {
            return strengthThreshold;
        }
    }

    public RelationshipEntropyHook() {
        this(0.01, 0.25);
    }

    public RelationshipEntropyHook(double baseDecayRate, double reengagementBoost) {
        this.random = new Random();
        this.baseDecayRate = baseDecayRate;
        this.reengagementBoost = reengagementBoost;
        this.lastDecayTimestamp = new HashMap<>();
        this.decayHistory = new ArrayList<>();
        this.reengagementHistory = new ArrayList<>();
    }

    /**
     * Apply entropy decay to all relationships for a given agent.
     * Decay rate varies based on relationship tier and network distance.
     */
    public Map<String, Double> applyEntropyDecay(SocialAgent agent,
                                                  SocialGraphDensityModel.ClusterAssignment assignment,
                                                  long currentTime) {
        Map<String, Double> decayedStrengths = new HashMap<>();
        Map<String, SocialAgent.Relationship> relationships = agent.getRelationships();

        for (Map.Entry<String, SocialAgent.Relationship> entry : relationships.entrySet()) {
            String targetId = entry.getKey();
            SocialAgent.Relationship rel = entry.getValue();

            DecayTier tier = determineDecayTier(targetId, assignment);
            double distanceFactor = calculateDistanceFactor(targetId, assignment);

            double decayedStrength = calculateDecayedStrength(
                rel.getBaseStrength(),
                tier.getDecayRate(),
                distanceFactor,
                rel.getLastInteraction(),
                currentTime
            );

            decayedStrengths.put(targetId, decayedStrength);

            if (Math.abs(decayedStrength - rel.getBaseStrength()) > 0.01) {
                decayHistory.add(new DecayEvent(
                    agent.getAgentId(),
                    targetId,
                    rel.getBaseStrength(),
                    decayedStrength,
                    currentTime
                ));
            }
        }

        lastDecayTimestamp.put(agent.getAgentId(), currentTime);
        return decayedStrengths;
    }

    /**
     * Calculate decayed relationship strength using exponential decay model.
     */
    public double calculateDecayedStrength(double initialStrength,
                                            double decayRate,
                                            double distanceFactor,
                                            long lastInteractionTime,
                                            long currentTime) {
        double timeDelta = (currentTime - lastInteractionTime) / (1000.0 * 60 * 60);

        double timeDecay = Math.exp(-decayRate * timeDelta);

        double distancePenalty = 1.0 - (distanceFactor * 0.3);

        double finalStrength = initialStrength * timeDecay * distancePenalty;

        return Math.max(0.0, Math.min(1.0, finalStrength));
    }

    /**
     * Determine decay tier based on cluster assignment.
     */
    private DecayTier determineDecayTier(String targetId,
                                          SocialGraphDensityModel.ClusterAssignment assignment) {
        if (assignment.getCoreCluster().contains(targetId)) {
            return DecayTier.CORE;
        } else if (assignment.getSympathyGroup().contains(targetId)) {
            return DecayTier.SYMPATHY;
        } else if (assignment.getOuterNetwork().contains(targetId)) {
            return DecayTier.OUTER;
        }
        return DecayTier.ACQUAINTANCE;
    }

    /**
     * Calculate distance penalty factor based on network position.
     */
    private double calculateDistanceFactor(String targetId,
                                            SocialGraphDensityModel.ClusterAssignment assignment) {
        if (assignment.getCoreCluster().contains(targetId)) {
            return 0.0;
        } else if (assignment.getSympathyGroup().contains(targetId)) {
            return 0.3;
        } else if (assignment.getOuterNetwork().contains(targetId)) {
            return 0.6;
        }
        return 1.0;
    }

    /**
     * Process re-engagement event for a dormant relationship.
     * Applies strength boost based on re-engagement coefficients.
     */
    public double processReengagement(SocialAgent agent,
                                       String targetId,
                                       double currentStrength,
                                       int sharedInteractions,
                                       ReengagementType type) {
        double baseBoost = reengagementBoost * (1.0 - currentStrength);
        double sharedBonus = Math.min(0.2, sharedInteractions * 0.05);
        double typeMultiplier = type.getBoostMultiplier();

        double totalBoost = baseBoost * typeMultiplier + sharedBonus;
        double newStrength = Math.min(1.0, currentStrength + totalBoost);

        reengagementHistory.add(new ReengagementEvent(
            agent.getAgentId(),
            targetId,
            currentStrength,
            newStrength,
            type,
            System.currentTimeMillis()
        ));

        return newStrength;
    }

    /**
     * Identify relationships eligible for re-engagement.
     * Relationships below threshold with sufficient history may be reactivated.
     */
    public List<ReengagementCandidate> identifyReengagementCandidates(
            SocialAgent agent,
            SocialGraphDensityModel.ClusterAssignment assignment,
            long currentTime) {

        List<ReengagementCandidate> candidates = new ArrayList<>();
        Map<String, SocialAgent.Relationship> relationships = agent.getRelationships();

        for (Map.Entry<String, SocialAgent.Relationship> entry : relationships.entrySet()) {
            String targetId = entry.getKey();
            SocialAgent.Relationship rel = entry.getValue();

            DecayTier tier = determineDecayTier(targetId, assignment);
            double currentStrength = rel.getBaseStrength();

            long timeInactive = currentTime - rel.getLastInteraction();
            double hoursInactive = timeInactive / (1000.0 * 60 * 60);

            if (currentStrength < tier.getStrengthThreshold() && hoursInactive > 24) {
                double reengagementProbability = calculateReengagementProbability(
                    currentStrength, hoursInactive, tier
                );

                candidates.add(new ReengagementCandidate(
                    targetId, currentStrength, hoursInactive, reengagementProbability
                ));
            }
        }

        candidates.sort((a, b) -> Double.compare(b.probability, a.probability));
        return candidates;
    }

    /**
     * Calculate probability of spontaneous re-engagement.
     */
    private double calculateReengagementProbability(double currentStrength,
                                                     double hoursInactive,
                                                     DecayTier tier) {
        double baseProbability = 0.05;
        double strengthFactor = 1.0 - currentStrength;
        double timeFactor = Math.min(1.0, hoursInactive / 168.0);

        double probability = baseProbability * strengthFactor * timeFactor;

        if (tier == DecayTier.CORE) {
            probability *= 2.0;
        } else if (tier == DecayTier.ACQUAINTANCE) {
            probability *= 0.5;
        }

        return Math.min(0.5, probability);
    }

    /**
     * Simulate time-based decay across entire network.
     * Returns statistics about decay events.
     */
    public EntropySimulationResult simulateNetworkDecay(
            List<SocialAgent> agents,
            Map<String, SocialGraphDensityModel.ClusterAssignment> assignments,
            long timeDeltaHours) {

        long currentTime = System.currentTimeMillis();
        long simulatedTime = currentTime + (timeDeltaHours * 60 * 60 * 1000);

        int totalRelationships = 0;
        int decayedRelationships = 0;
        double totalDecayAmount = 0.0;

        Map<DecayTier, Integer> tierCounts = new HashMap<>();
        Map<DecayTier, Double> tierDecaySums = new HashMap<>();

        for (SocialAgent agent : agents) {
            SocialGraphDensityModel.ClusterAssignment assignment = assignments.get(agent.getAgentId());
            if (assignment == null) continue;

            Map<String, Double> decayedStrengths = applyEntropyDecay(agent, assignment, simulatedTime);

            for (Map.Entry<String, Double> entry : decayedStrengths.entrySet()) {
                String targetId = entry.getKey();
                double decayedStrength = entry.getValue();

                SocialAgent.Relationship rel = agent.getRelationships().get(targetId);
                double originalStrength = rel.getBaseStrength();
                double decayAmount = originalStrength - decayedStrength;

                totalRelationships++;

                if (decayAmount > 0.001) {
                    decayedRelationships++;
                    totalDecayAmount += decayAmount;

                    DecayTier tier = determineDecayTier(targetId, assignment);
                    tierCounts.merge(tier, 1, Integer::sum);
                    tierDecaySums.merge(tier, decayAmount, Double::sum);
                }

                rel.updateStrength(decayedStrength);
            }
        }

        return new EntropySimulationResult(
            totalRelationships,
            decayedRelationships,
            totalDecayAmount,
            tierCounts,
            tierDecaySums
        );
    }

    /**
     * Get decay history for analysis.
     */
    public List<DecayEvent> getDecayHistory() {
        return new ArrayList<>(decayHistory);
    }

    /**
     * Get re-engagement history for analysis.
     */
    public List<ReengagementEvent> getReengagementHistory() {
        return new ArrayList<>(reengagementHistory);
    }

    /**
     * ReengagementType - Categories of re-engagement events with different boost levels.
     */
    public enum ReengagementType {
        DIRECT_MESSAGE(1.0),         // Direct interaction: full boost
        CONTENT_REACTION(0.7),       // Like/comment: moderate boost
        MUTUAL_FRIEND_ACTIVITY(0.5), // Through shared connection: partial boost
        ALGORITHM_SUGGESTION(0.3),   // Platform-recommended: minimal boost
        EXTERNAL_TRIGGER(0.8);       // Notification/external: high boost

        private final double boostMultiplier;

        ReengagementType(double boostMultiplier) {
            this.boostMultiplier = boostMultiplier;
        }

        public double getBoostMultiplier() {
            return boostMultiplier;
        }
    }

    /**
     * DecayEvent - Record of a relationship decay occurrence.
     */
    public static class DecayEvent {
        public final String agentId;
        public final String targetId;
        public final double originalStrength;
        public final double decayedStrength;
        public final long timestamp;

        public DecayEvent(String agentId, String targetId,
                          double originalStrength, double decayedStrength,
                          long timestamp) {
            this.agentId = agentId;
            this.targetId = targetId;
            this.originalStrength = originalStrength;
            this.decayedStrength = decayedStrength;
            this.timestamp = timestamp;
        }
    }

    /**
     * ReengagementEvent - Record of a relationship reactivation.
     */
    public static class ReengagementEvent {
        public final String agentId;
        public final String targetId;
        public final double previousStrength;
        public final double newStrength;
        public final ReengagementType type;
        public final long timestamp;

        public ReengagementEvent(String agentId, String targetId,
                                  double previousStrength, double newStrength,
                                  ReengagementType type, long timestamp) {
            this.agentId = agentId;
            this.targetId = targetId;
            this.previousStrength = previousStrength;
            this.newStrength = newStrength;
            this.type = type;
            this.timestamp = timestamp;
        }
    }

    /**
     * ReengagementCandidate - Potential relationship for reactivation.
     */
    public static class ReengagementCandidate {
        public final String targetId;
        public final double currentStrength;
        public final double hoursInactive;
        public final double probability;

        public ReengagementCandidate(String targetId, double currentStrength,
                                      double hoursInactive, double probability) {
            this.targetId = targetId;
            this.currentStrength = currentStrength;
            this.hoursInactive = hoursInactive;
            this.probability = probability;
        }
    }

    /**
     * EntropySimulationResult - Statistics from network decay simulation.
     */
    public static class EntropySimulationResult {
        private final int totalRelationships;
        private final int decayedRelationships;
        private final double totalDecayAmount;
        private final Map<DecayTier, Integer> tierCounts;
        private final Map<DecayTier, Double> tierDecaySums;

        public EntropySimulationResult(int totalRelationships,
                                        int decayedRelationships,
                                        double totalDecayAmount,
                                        Map<DecayTier, Integer> tierCounts,
                                        Map<DecayTier, Double> tierDecaySums) {
            this.totalRelationships = totalRelationships;
            this.decayedRelationships = decayedRelationships;
            this.totalDecayAmount = totalDecayAmount;
            this.tierCounts = tierCounts;
            this.tierDecaySums = tierDecaySums;
        }

        public double getDecayRate() {
            return totalRelationships == 0 ? 0.0 :
                (double) decayedRelationships / totalRelationships;
        }

        public double getAverageDecayAmount() {
            return decayedRelationships == 0 ? 0.0 :
                totalDecayAmount / decayedRelationships;
        }

        @Override
        public String toString() {
            return String.format(
                "EntropyResult{decayed: %d/%d (%.1f%%), avg decay: %.3f}",
                decayedRelationships, totalRelationships,
                getDecayRate() * 100, getAverageDecayAmount()
            );
        }
    }
}
