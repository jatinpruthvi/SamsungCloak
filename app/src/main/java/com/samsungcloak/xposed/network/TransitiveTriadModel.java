package com.samsungcloak.xposed.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * TransitiveTriadModel - Implements triadic closure for natural community growth.
 *
 * Mathematical Foundation:
 * Transitive triads follow the principle: if A interacts with B and A interacts with C,
 * then B and C are more likely to interact (closure probability).
 *
 * This models real-world social dynamics where mutual connections facilitate
 * new relationships through social proof and shared context.
 *
 * Triadic Closure Probability:
 * P(B-C | A-B, A-C) = α × (w_AB + w_AC) / 2 × (1 + σ_N)
 * where:
 * - α = base closure rate (typically 0.3-0.6)
 * - w_AB, w_AC = relationship strengths
 * - σ_N = network structural coefficient
 *
 * Clustering Coefficient:
 * C = (3 × number of triangles) / (number of connected triples)
 * Real networks: C ≈ 0.3-0.6 (high clustering)
 * Random networks: C ≈ p (edge probability, typically very low)
 */
public class TransitiveTriadModel {

    private static final String LOG_TAG = "SamsungCloak.TransitiveTriad";

    private final Random random;
    private final double baseClosureRate;
    private final double homophilyFactor;

    private int trianglesFormed;
    private int closureAttempts;
    private Map<String, Set<Set<String>>> triadCache;

    public TransitiveTriadModel() {
        this(0.45, 0.3);
    }

    public TransitiveTriadModel(double baseClosureRate, double homophilyFactor) {
        this.random = new Random();
        this.baseClosureRate = baseClosureRate;
        this.homophilyFactor = homophilyFactor;
        this.trianglesFormed = 0;
        this.closureAttempts = 0;
        this.triadCache = new HashMap<>();
    }

    /**
     * Identify all open triads in the network.
     * An open triad: A-B, A-C exist but B-C does not.
     */
    public List<OpenTriad> identifyOpenTriads(List<SocialAgent> agents) {
        List<OpenTriad> openTriads = new ArrayList<>();

        for (SocialAgent agentA : agents) {
            Set<String> neighbors = new HashSet<>(agentA.getRelationships().keySet());

            for (String agentB : neighbors) {
                for (String agentC : neighbors) {
                    if (agentB.compareTo(agentC) >= 0) continue;

                    SocialAgent b = findAgentById(agents, agentB);
                    SocialAgent c = findAgentById(agents, agentC);

                    if (b == null || c == null) continue;

                    if (!hasRelationship(b, agentC)) {
                        double strengthAB = agentA.getRelationshipStrength(agentB, 0.01);
                        double strengthAC = agentA.getRelationshipStrength(agentC, 0.01);

                        openTriads.add(new OpenTriad(agentA, b, c, strengthAB, strengthAC));
                    }
                }
            }
        }

        return openTriads;
    }

    /**
     * Calculate triadic closure probability for an open triad.
     * Uses weighted combination of relationship strengths and homophily.
     */
    public double calculateClosureProbability(OpenTriad triad,
                                               Map<String, SocialAgent> agentMap) {
        closureAttempts++;

        double avgStrength = (triad.strengthAB + triad.strengthAC) / 2.0;

        double structuralCoefficient = calculateStructuralCoefficient(triad, agentMap);

        double homophilyBonus = calculateHomophilyBonus(triad, agentMap);

        double probability = baseClosureRate * avgStrength * (1 + structuralCoefficient) *
                            (1 + homophilyBonus * homophilyFactor);

        return Math.min(0.95, probability);
    }

    /**
     * Calculate structural coefficient based on shared network position.
     * Agents in same cluster tier have higher closure probability.
     */
    private double calculateStructuralCoefficient(OpenTriad triad,
                                                   Map<String, SocialAgent> agentMap) {
        SocialAgent A = triad.agentA;
        SocialAgent B = triad.agentB;
        SocialAgent C = triad.agentC;

        Set<String> neighborsB = B.getRelationships().keySet();
        Set<String> neighborsC = C.getRelationships().keySet();

        Set<String> commonNeighbors = new HashSet<>(neighborsB);
        commonNeighbors.retainAll(neighborsC);

        double jaccardSimilarity = 0.0;
        if (!neighborsB.isEmpty() || !neighborsC.isEmpty()) {
            Set<String> union = new HashSet<>(neighborsB);
            union.addAll(neighborsC);
            jaccardSimilarity = (double) commonNeighbors.size() / union.size();
        }

        boolean sameCluster = A.getClusterId().equals(B.getClusterId()) &&
                             A.getClusterId().equals(C.getClusterId());

        double clusterBonus = sameCluster ? 0.2 : 0.0;

        return jaccardSimilarity * 0.5 + clusterBonus;
    }

    /**
     * Calculate homophily bonus based on content preference similarity.
     */
    private double calculateHomophilyBonus(OpenTriad triad,
                                            Map<String, SocialAgent> agentMap) {
        double homophilyBC = triad.agentB.calculateHomophilyScore(triad.agentC);
        return homophilyBC * 0.3;
    }

    /**
     * Attempt to close triads probabilistically.
     * Returns number of new edges (triangles) formed.
     */
    public int processTriadicClosure(List<SocialAgent> agents,
                                      Map<String, SocialAgent> agentMap) {
        List<OpenTriad> openTriads = identifyOpenTriads(agents);
        int closures = 0;

        for (OpenTriad triad : openTriads) {
            double probability = calculateClosureProbability(triad, agentMap);

            if (random.nextDouble() < probability) {
                closeTriad(triad);
                closures++;
                trianglesFormed++;
            }
        }

        return closures;
    }

    /**
     * Close an open triad by forming the B-C relationship.
     */
    private void closeTriad(OpenTriad triad) {
        SocialAgent B = triad.agentB;
        SocialAgent C = triad.agentC;

        double newStrength = 0.2 + random.nextDouble() * 0.3;

        B.strengthenRelationship(C.getAgentId(), newStrength * 0.5);
        C.strengthenRelationship(B.getAgentId(), newStrength * 0.5);
    }

    /**
     * Calculate local clustering coefficient for a specific agent.
     * C_i = (2 × e_i) / (k_i × (k_i - 1))
     * where e_i = edges between neighbors, k_i = degree
     */
    public double calculateLocalClusteringCoefficient(SocialAgent agent) {
        Set<String> neighbors = agent.getRelationships().keySet();
        int k = neighbors.size();

        if (k < 2) return 0.0;

        int edgesBetweenNeighbors = 0;
        List<String> neighborList = new ArrayList<>(neighbors);

        for (int i = 0; i < neighborList.size(); i++) {
            for (int j = i + 1; j < neighborList.size(); j++) {
                String ni = neighborList.get(i);
                String nj = neighborList.get(j);

                if (areConnected(agent, ni, nj)) {
                    edgesBetweenNeighbors++;
                }
            }
        }

        return (2.0 * edgesBetweenNeighbors) / (k * (k - 1));
    }

    /**
     * Calculate global clustering coefficient (transitivity) for the network.
     */
    public double calculateGlobalClusteringCoefficient(List<SocialAgent> agents) {
        int closedTriples = 0;
        int openTriples = 0;

        for (SocialAgent agent : agents) {
            Set<String> neighbors = agent.getRelationships().keySet();
            List<String> neighborList = new ArrayList<>(neighbors);

            for (int i = 0; i < neighborList.size(); i++) {
                for (int j = i + 1; j < neighborList.size(); j++) {
                    openTriples++;

                    if (areConnected(agent, neighborList.get(i), neighborList.get(j))) {
                        closedTriples++;
                    }
                }
            }
        }

        return openTriples == 0 ? 0.0 : (double) closedTriples / openTriples;
    }

    /**
     * Simulate natural community growth through iterative triadic closure.
     * Models how real communities form denser connections over time.
     */
    public CommunityGrowthResult simulateCommunityGrowth(List<SocialAgent> agents,
                                                          int iterations) {
        Map<String, SocialAgent> agentMap = new HashMap<>();
        for (SocialAgent agent : agents) {
            agentMap.put(agent.getAgentId(), agent);
        }

        List<Double> clusteringHistory = new ArrayList<>();
        List<Integer> edgeHistory = new ArrayList<>();

        int initialEdges = countTotalEdges(agents);
        double initialClustering = calculateGlobalClusteringCoefficient(agents);

        clusteringHistory.add(initialClustering);
        edgeHistory.add(initialEdges);

        for (int i = 0; i < iterations; i++) {
            int newEdges = processTriadicClosure(agents, agentMap);
            double clustering = calculateGlobalClusteringCoefficient(agents);

            clusteringHistory.add(clustering);
            edgeHistory.add(countTotalEdges(agents));
        }

        return new CommunityGrowthResult(
            initialEdges,
            countTotalEdges(agents),
            initialClustering,
            calculateGlobalClusteringCoefficient(agents),
            clusteringHistory,
            edgeHistory
        );
    }

    private boolean hasRelationship(SocialAgent agent, String otherId) {
        return agent.getRelationships().containsKey(otherId);
    }

    private boolean areConnected(SocialAgent context, String id1, String id2) {
        SocialAgent a1 = findAgentById(List.of(context), id1);
        if (a1 == null) return false;
        return a1.getRelationships().containsKey(id2);
    }

    private SocialAgent findAgentById(List<SocialAgent> agents, String id) {
        for (SocialAgent agent : agents) {
            if (agent.getAgentId().equals(id)) {
                return agent;
            }
        }
        return null;
    }

    private int countTotalEdges(List<SocialAgent> agents) {
        int count = 0;
        for (SocialAgent agent : agents) {
            count += agent.getRelationships().size();
        }
        return count / 2;
    }

    public int getTrianglesFormed() {
        return trianglesFormed;
    }

    public int getClosureAttempts() {
        return closureAttempts;
    }

    public double getClosureSuccessRate() {
        return closureAttempts == 0 ? 0.0 : (double) trianglesFormed / closureAttempts;
    }

    /**
     * OpenTriad - Represents an unclosed triadic structure.
     */
    public static class OpenTriad {
        final SocialAgent agentA;
        final SocialAgent agentB;
        final SocialAgent agentC;
        final double strengthAB;
        final double strengthAC;

        public OpenTriad(SocialAgent agentA, SocialAgent agentB, SocialAgent agentC,
                         double strengthAB, double strengthAC) {
            this.agentA = agentA;
            this.agentB = agentB;
            this.agentC = agentC;
            this.strengthAB = strengthAB;
            this.strengthAC = strengthAC;
        }
    }

    /**
     * CommunityGrowthResult - Statistics from community growth simulation.
     */
    public static class CommunityGrowthResult {
        private final int initialEdges;
        private final int finalEdges;
        private final double initialClustering;
        private final double finalClustering;
        private final List<Double> clusteringHistory;
        private final List<Integer> edgeHistory;

        public CommunityGrowthResult(int initialEdges, int finalEdges,
                                      double initialClustering, double finalClustering,
                                      List<Double> clusteringHistory, List<Integer> edgeHistory) {
            this.initialEdges = initialEdges;
            this.finalEdges = finalEdges;
            this.initialClustering = initialClustering;
            this.finalClustering = finalClustering;
            this.clusteringHistory = clusteringHistory;
            this.edgeHistory = edgeHistory;
        }

        public int getInitialEdges() {
            return initialEdges;
        }

        public int getFinalEdges() {
            return finalEdges;
        }

        public double getInitialClustering() {
            return initialClustering;
        }

        public double getFinalClustering() {
            return finalClustering;
        }

        public List<Double> getClusteringHistory() {
            return clusteringHistory;
        }

        public List<Integer> getEdgeHistory() {
            return edgeHistory;
        }

        public double getEdgeGrowthRate() {
            return initialEdges == 0 ? 0.0 : (double) (finalEdges - initialEdges) / initialEdges;
        }

        @Override
        public String toString() {
            return String.format(
                "CommunityGrowth{edges: %d→%d (+%.1f%%), clustering: %.3f→%.3f}",
                initialEdges, finalEdges, getEdgeGrowthRate() * 100,
                initialClustering, finalClustering
            );
        }
    }
}
