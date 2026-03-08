package com.samsungcloak.xposed.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * SocialGraphDensityModel - Implements Dunbar's Number constraints for realistic
 * social network topology.
 *
 * Mathematical Foundation:
 * - Dunbar's Number: Cognitive limit of ~150 stable relationships
 * - Core Cluster: 5-10 closest relationships (Dunbar's "support clique")
 * - Sympathy Group: 15-50 relationships
 * - Outer Network: Remaining relationships
 *
 * This model ensures interaction is concentrated within the Core Cluster,
 * reflecting real-world social network constraints where humans maintain
 * dense interaction patterns with a small subset of their network.
 *
 * Density Formula:
 * - Core Cluster Density: 0.8-1.0 (highly interconnected)
 * - Sympathy Group Density: 0.3-0.5 (moderately connected)
 * - Outer Network Density: 0.05-0.15 (sparsely connected)
 */
public class SocialGraphDensityModel {

    private static final String LOG_TAG = "SamsungCloak.SocialGraphDensity";

    public static final int DUNBAR_NUMBER = 150;
    public static final int MIN_CORE_SIZE = 5;
    public static final int MAX_CORE_SIZE = 10;
    public static final int SYMPATHY_GROUP_MAX = 50;

    private final Random random;
    private final Map<String, Set<String>> agentClusters;
    private final Map<String, Double> clusterDensities;

    public enum ClusterTier {
        CORE(0.85, 1.0),           // 5-10 agents, 85-100% density
        SYMPATHY(0.35, 0.50),      // 15-50 agents, 35-50% density
        OUTER(0.05, 0.15),         // Remaining, 5-15% density
        ACQUAINTANCE(0.01, 0.05);  // Weak ties, 1-5% density

        private final double minDensity;
        private final double maxDensity;

        ClusterTier(double minDensity, double maxDensity) {
            this.minDensity = minDensity;
            this.maxDensity = maxDensity;
        }

        public double getRandomDensity(Random random) {
            return minDensity + random.nextDouble() * (maxDensity - minDensity);
        }

        public boolean isEdgePresent(Random random) {
            return random.nextDouble() < getRandomDensity(random);
        }
    }

    public SocialGraphDensityModel() {
        this.random = new Random();
        this.agentClusters = new HashMap<>();
        this.clusterDensities = new HashMap<>();
    }

    /**
     * Initialize a Dunbar-constrained network topology for a set of agents.
     * Assigns each agent to appropriate cluster tiers within their cognitive limit.
     */
    public Map<String, ClusterAssignment> initializeDunbarTopology(
            List<SocialAgent> allAgents,
            Set<String> contentCategories) {

        Map<String, ClusterAssignment> assignments = new HashMap<>();

        for (SocialAgent agent : allAgents) {
            ClusterAssignment assignment = createClusterAssignment(agent, allAgents);
            assignments.put(agent.getAgentId(), assignment);

            initializeCoreClusterConnections(agent, assignment.getCoreCluster());
            initializeSympathyGroupConnections(agent, assignment.getSympathyGroup());
            initializeOuterNetworkConnections(agent, assignment.getOuterNetwork());

            agent.initializePreferences(contentCategories);
        }

        return assignments;
    }

    /**
     * Create cluster assignment following Dunbar's Number constraints.
     */
    private ClusterAssignment createClusterAssignment(SocialAgent agent,
                                                       List<SocialAgent> allAgents) {
        List<SocialAgent> otherAgents = new ArrayList<>(allAgents);
        otherAgents.remove(agent);
        Collections.shuffle(otherAgents, random);

        int coreSize = MIN_CORE_SIZE + random.nextInt(MAX_CORE_SIZE - MIN_CORE_SIZE + 1);
        int sympathySize = 15 + random.nextInt(36);

        Set<String> coreCluster = new HashSet<>();
        Set<String> sympathyGroup = new HashSet<>();
        Set<String> outerNetwork = new HashSet<>();

        for (int i = 0; i < otherAgents.size() && i < DUNBAR_NUMBER; i++) {
            String otherId = otherAgents.get(i).getAgentId();

            if (i < coreSize) {
                coreCluster.add(otherId);
                agent.addToCoreCluster(otherId);
            } else if (i < coreSize + sympathySize) {
                sympathyGroup.add(otherId);
            } else {
                outerNetwork.add(otherId);
            }
        }

        ClusterAssignment assignment = new ClusterAssignment(
            agent.getAgentId(), coreCluster, sympathyGroup, outerNetwork
        );

        agentClusters.put(agent.getAgentId(), coreCluster);
        clusterDensities.put(agent.getAgentId() + "_core", ClusterTier.CORE.getRandomDensity(random));

        return assignment;
    }

    /**
     * Initialize highly interconnected core cluster (support clique).
     * Ensures 80-100% edge density within core cluster.
     */
    private void initializeCoreClusterConnections(SocialAgent agent, Set<String> coreCluster) {
        for (String otherId : coreCluster) {
            double initialStrength = 0.7 + random.nextDouble() * 0.3;

            agent.getRelationships().computeIfAbsent(otherId,
                k -> agent.new Relationship(otherId));

            SocialAgent.Relationship rel = agent.getRelationships().get(otherId);
            rel.updateStrength(initialStrength);
        }
    }

    /**
     * Initialize moderately connected sympathy group.
     * 35-50% edge density with probabilistic connections.
     */
    private void initializeSympathyGroupConnections(SocialAgent agent,
                                                     Set<String> sympathyGroup) {
        for (String otherId : sympathyGroup) {
            if (ClusterTier.SYMPATHY.isEdgePresent(random)) {
                double initialStrength = 0.3 + random.nextDouble() * 0.4;

                agent.getRelationships().computeIfAbsent(otherId,
                    k -> agent.new Relationship(otherId));

                SocialAgent.Relationship rel = agent.getRelationships().get(otherId);
                rel.updateStrength(initialStrength);
            }
        }
    }

    /**
     * Initialize sparsely connected outer network.
     * 5-15% edge density representing weak ties.
     */
    private void initializeOuterNetworkConnections(SocialAgent agent,
                                                    Set<String> outerNetwork) {
        for (String otherId : outerNetwork) {
            if (ClusterTier.OUTER.isEdgePresent(random)) {
                double initialStrength = 0.05 + random.nextDouble() * 0.15;

                agent.getRelationships().computeIfAbsent(otherId,
                    k -> agent.new Relationship(otherId));

                SocialAgent.Relationship rel = agent.getRelationships().get(otherId);
                rel.updateStrength(initialStrength);
            }
        }
    }

    /**
     * Calculate the probability of interaction based on cluster tier.
     * Core cluster agents have exponentially higher interaction probability.
     *
     * P(interact|tier) = baseRate × activityMultiplier
     */
    public double getInteractionProbability(String agentId, String targetId,
                                             ClusterAssignment assignment) {
        if (assignment.getCoreCluster().contains(targetId)) {
            return 0.6 + random.nextDouble() * 0.35;
        } else if (assignment.getSympathyGroup().contains(targetId)) {
            return 0.15 + random.nextDouble() * 0.20;
        } else if (assignment.getOuterNetwork().contains(targetId)) {
            return 0.02 + random.nextDouble() * 0.08;
        }
        return 0.005;
    }

    /**
     * Select interaction target based on Dunbar-tier weighted probability.
     * Returns agent ID weighted toward core cluster interactions.
     */
    public String selectInteractionTarget(ClusterAssignment assignment,
                                           List<SocialAgent> availableAgents) {
        double coreWeight = 0.70;
        double sympathyWeight = 0.25;
        double outerWeight = 0.05;

        double roll = random.nextDouble();

        if (roll < coreWeight && !assignment.getCoreCluster().isEmpty()) {
            List<String> coreList = new ArrayList<>(assignment.getCoreCluster());
            return coreList.get(random.nextInt(coreList.size()));
        } else if (roll < coreWeight + sympathyWeight && !assignment.getSympathyGroup().isEmpty()) {
            List<String> sympathyList = new ArrayList<>(assignment.getSympathyGroup());
            return sympathyList.get(random.nextInt(sympathyList.size()));
        } else if (!assignment.getOuterNetwork().isEmpty()) {
            List<String> outerList = new ArrayList<>(assignment.getOuterNetwork());
            return outerList.get(random.nextInt(outerList.size()));
        }

        return null;
    }

    /**
     * Calculate network density metrics for validation.
     */
    public DensityMetrics calculateDensityMetrics(List<SocialAgent> agents) {
        double totalDensity = 0.0;
        double coreDensitySum = 0.0;
        int coreCount = 0;

        for (SocialAgent agent : agents) {
            Map<String, SocialAgent.Relationship> rels = agent.getRelationships();
            if (rels.isEmpty()) continue;

            int possibleEdges = agents.size() - 1;
            int actualEdges = rels.size();
            double agentDensity = (double) actualEdges / possibleEdges;
            totalDensity += agentDensity;

            int coreEdges = 0;
            for (String coreMember : agent.getCoreCluster()) {
                if (rels.containsKey(coreMember)) {
                    coreEdges++;
                }
            }

            if (!agent.getCoreCluster().isEmpty()) {
                coreDensitySum += (double) coreEdges / agent.getCoreCluster().size();
                coreCount++;
            }
        }

        double avgDensity = agents.isEmpty() ? 0 : totalDensity / agents.size();
        double avgCoreDensity = coreCount == 0 ? 0 : coreDensitySum / coreCount;

        return new DensityMetrics(avgDensity, avgCoreDensity);
    }

    public Map<String, Set<String>> getAgentClusters() {
        return new HashMap<>(agentClusters);
    }

    public Set<String> getCoreCluster(String agentId) {
        return agentClusters.getOrDefault(agentId, new HashSet<>());
    }

    /**
     * ClusterAssignment - Immutable record of an agent's network position.
     */
    public static class ClusterAssignment {
        private final String agentId;
        private final Set<String> coreCluster;
        private final Set<String> sympathyGroup;
        private final Set<String> outerNetwork;

        public ClusterAssignment(String agentId, Set<String> coreCluster,
                                  Set<String> sympathyGroup, Set<String> outerNetwork) {
            this.agentId = agentId;
            this.coreCluster = Collections.unmodifiableSet(new HashSet<>(coreCluster));
            this.sympathyGroup = Collections.unmodifiableSet(new HashSet<>(sympathyGroup));
            this.outerNetwork = Collections.unmodifiableSet(new HashSet<>(outerNetwork));
        }

        public String getAgentId() {
            return agentId;
        }

        public Set<String> getCoreCluster() {
            return coreCluster;
        }

        public Set<String> getSympathyGroup() {
            return sympathyGroup;
        }

        public Set<String> getOuterNetwork() {
            return outerNetwork;
        }

        public int getTotalConnections() {
            return coreCluster.size() + sympathyGroup.size() + outerNetwork.size();
        }
    }

    /**
     * DensityMetrics - Network topology statistics.
     */
    public static class DensityMetrics {
        private final double averageDensity;
        private final double averageCoreDensity;

        public DensityMetrics(double averageDensity, double averageCoreDensity) {
            this.averageDensity = averageDensity;
            this.averageCoreDensity = averageCoreDensity;
        }

        public double getAverageDensity() {
            return averageDensity;
        }

        public double getAverageCoreDensity() {
            return averageCoreDensity;
        }

        @Override
        public String toString() {
            return String.format("DensityMetrics{avg=%.3f, core=%.3f}",
                averageDensity, averageCoreDensity);
        }
    }
}
