package com.samsungcloak.xposed.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CommunityHomophilyModel - Implements dynamic preference weighting based on
 * majority preferences of assigned network cluster.
 *
 * Mathematical Foundation:
 * Homophily ("birds of a feather flock together") is a fundamental property
 * of real social networks. Agents dynamically adjust their content preferences
 * to align with their community's majority preferences.
 *
 * Preference Update Formula:
 * P_i_new(c) = (1 - α·h_i)·P_i_old(c) + α·h_i·P_cluster(c)
 *
 * Where:
 * - P_i(c) = agent i's preference for content category c
 * - α = learning rate (0.05 - 0.15)
 * - h_i = agent's homophily weight (individual tendency to conform)
 * - P_cluster(c) = majority preference in agent's cluster
 *
 * Cluster Majority Calculation:
 * P_cluster(c) = Σ(w_j · P_j(c)) / Σ(w_j)
 * where w_j = relationship strength between agent and cluster member j
 *
 * Convergence Criteria:
 * The model converges when ||P_i - P_cluster|| < ε for all agents in cluster
 */
public class CommunityHomophilyModel {

    private static final String LOG_TAG = "SamsungCloak.CommunityHomophily";

    private final double learningRate;
    private final double convergenceThreshold;
    private final int maxIterations;

    private List<ConvergenceSnapshot> convergenceHistory;

    public CommunityHomophilyModel() {
        this(0.08, 0.05, 100);
    }

    public CommunityHomophilyModel(double learningRate,
                                    double convergenceThreshold,
                                    int maxIterations) {
        this.learningRate = learningRate;
        this.convergenceThreshold = convergenceThreshold;
        this.maxIterations = maxIterations;
        this.convergenceHistory = new ArrayList<>();
    }

    /**
     * Calculate majority preference vector for a cluster using weighted aggregation.
     * Weights based on relationship strengths to center agent.
     */
    public Map<String, Double> calculateClusterMajority(
            SocialAgent agent,
            List<SocialAgent> clusterMembers,
            Set<String> contentCategories) {

        Map<String, Double> weightedPrefs = new HashMap<>();
        Map<String, Double> weights = new HashMap<>();

        for (String category : contentCategories) {
            weightedPrefs.put(category, 0.0);
        }

        double totalWeight = 0.0;

        for (SocialAgent member : clusterMembers) {
            if (member.getAgentId().equals(agent.getAgentId())) continue;

            double relationshipStrength = agent.getRelationshipStrength(
                member.getAgentId(), 0.01
            );

            double weight = Math.pow(relationshipStrength, 2);
            totalWeight += weight;

            Map<String, Double> memberPrefs = member.getContentPreferences();
            for (String category : contentCategories) {
                double prefValue = memberPrefs.getOrDefault(category, 0.0);
                weightedPrefs.merge(category, prefValue * weight, Double::sum);
            }
        }

        Map<String, Double> majorityPrefs = new HashMap<>();
        if (totalWeight > 0) {
            for (String category : contentCategories) {
                majorityPrefs.put(category, weightedPrefs.get(category) / totalWeight);
            }
        } else {
            double uniform = 1.0 / contentCategories.size();
            for (String category : contentCategories) {
                majorityPrefs.put(category, uniform);
            }
        }

        return majorityPrefs;
    }

    /**
     * Calculate simple majority preference (unweighted) for a cluster.
     */
    public Map<String, Double> calculateSimpleMajority(
            List<SocialAgent> clusterMembers,
            Set<String> contentCategories) {

        Map<String, Double> sumPrefs = new HashMap<>();

        for (String category : contentCategories) {
            sumPrefs.put(category, 0.0);
        }

        int validMembers = 0;
        for (SocialAgent member : clusterMembers) {
            Map<String, Double> prefs = member.getContentPreferences();
            if (prefs.isEmpty()) continue;

            validMembers++;
            for (String category : contentCategories) {
                sumPrefs.merge(category, prefs.getOrDefault(category, 0.0), Double::sum);
            }
        }

        Map<String, Double> majorityPrefs = new HashMap<>();
        if (validMembers > 0) {
            for (String category : contentCategories) {
                majorityPrefs.put(category, sumPrefs.get(category) / validMembers);
            }
        }

        return majorityPrefs;
    }

    /**
     * Apply homophily-based preference adjustment to a single agent.
     */
    public Map<String, Double> adjustAgentPreferences(
            SocialAgent agent,
            Map<String, Double> clusterMajority,
            Set<String> contentCategories) {

        Map<String, Double> newPrefs = new HashMap<>();
        Map<String, Double> currentPrefs = agent.getContentPreferences();
        double homophilyWeight = agent.getHomophilyWeight();

        double effectiveLearningRate = learningRate * homophilyWeight;

        for (String category : contentCategories) {
            double currentPref = currentPrefs.getOrDefault(category, 0.0);
            double clusterPref = clusterMajority.getOrDefault(category, 0.0);

            double newPref = (1 - effectiveLearningRate) * currentPref +
                            effectiveLearningRate * clusterPref;

            newPrefs.put(category, newPref);
        }

        return normalizePreferences(newPrefs);
    }

    /**
     * Simulate preference convergence across entire network.
     * Iteratively updates agent preferences until convergence or max iterations.
     */
    public ConvergenceResult simulatePreferenceConvergence(
            List<SocialAgent> agents,
            Map<String, List<SocialAgent>> clusters,
            Set<String> contentCategories) {

        convergenceHistory.clear();
        double previousDivergence = Double.MAX_VALUE;

        for (int iteration = 0; iteration < maxIterations; iteration++) {
            Map<String, Map<String, Double>> clusterMajorities = new HashMap<>();

            for (Map.Entry<String, List<SocialAgent>> entry : clusters.entrySet()) {
                String clusterId = entry.getKey();
                List<SocialAgent> clusterMembers = entry.getValue();

                Map<String, Double> majority = calculateSimpleMajority(
                    clusterMembers, contentCategories
                );
                clusterMajorities.put(clusterId, majority);
            }

            double totalDivergence = 0.0;
            int adjustmentCount = 0;

            for (SocialAgent agent : agents) {
                String clusterId = agent.getClusterId();
                List<SocialAgent> clusterMembers = clusters.getOrDefault(clusterId,
                    Collections.singletonList(agent));

                Map<String, Double> majority = clusterMajorities.get(clusterId);
                if (majority == null) continue;

                Map<String, Double> newPrefs = adjustAgentPreferences(
                    agent, majority, contentCategories
                );

                double divergence = calculateDivergence(newPrefs, majority);
                totalDivergence += divergence;
                adjustmentCount++;
            }

            double avgDivergence = adjustmentCount == 0 ? 0.0 :
                totalDivergence / adjustmentCount;

            convergenceHistory.add(new ConvergenceSnapshot(
                iteration, avgDivergence, calculateNetworkPolarization(agents)
            ));

            if (avgDivergence < convergenceThreshold) {
                return new ConvergenceResult(true, iteration, avgDivergence,
                    convergenceHistory);
            }

            if (Math.abs(previousDivergence - avgDivergence) < 0.0001) {
                return new ConvergenceResult(true, iteration, avgDivergence,
                    convergenceHistory);
            }

            previousDivergence = avgDivergence;
        }

        return new ConvergenceResult(false, maxIterations, previousDivergence,
            convergenceHistory);
    }

    /**
     * Calculate divergence between agent preferences and cluster majority.
     * Uses Jensen-Shannon divergence for probability distributions.
     */
    public double calculateDivergence(Map<String, Double> agentPrefs,
                                       Map<String, Double> clusterMajority) {
        double divergence = 0.0;

        for (String category : agentPrefs.keySet()) {
            double p = agentPrefs.get(category);
            double q = clusterMajority.getOrDefault(category, 0.0);

            double m = (p + q) / 2.0;

            if (p > 0 && m > 0) {
                divergence += 0.5 * p * Math.log(p / m);
            }
            if (q > 0 && m > 0) {
                divergence += 0.5 * q * Math.log(q / m);
            }
        }

        return divergence;
    }

    /**
     * Calculate network polarization - measure of preference diversity across clusters.
     * Higher values indicate stronger "echo chambers".
     */
    public double calculateNetworkPolarization(List<SocialAgent> agents) {
        Map<String, List<SocialAgent>> clusters = new HashMap<>();

        for (SocialAgent agent : agents) {
            clusters.computeIfAbsent(agent.getClusterId(), k -> new ArrayList<>())
                    .add(agent);
        }

        if (clusters.size() < 2) return 0.0;

        double totalDistance = 0.0;
        int comparisons = 0;

        List<String> clusterIds = new ArrayList<>(clusters.keySet());
        for (int i = 0; i < clusterIds.size(); i++) {
            for (int j = i + 1; j < clusterIds.size(); j++) {
                List<SocialAgent> cluster1 = clusters.get(clusterIds.get(i));
                List<SocialAgent> cluster2 = clusters.get(clusterIds.get(j));

                double distance = calculateClusterDistance(cluster1, cluster2);
                totalDistance += distance;
                comparisons++;
            }
        }

        return comparisons == 0 ? 0.0 : totalDistance / comparisons;
    }

    /**
     * Calculate preference distance between two clusters.
     */
    private double calculateClusterDistance(List<SocialAgent> cluster1,
                                             List<SocialAgent> cluster2) {
        if (cluster1.isEmpty() || cluster2.isEmpty()) return 0.0;

        Map<String, Double> avgPrefs1 = calculateAveragePreferences(cluster1);
        Map<String, Double> avgPrefs2 = calculateAveragePreferences(cluster2);

        double distance = 0.0;
        Set<String> allCategories = avgPrefs1.keySet();

        for (String category : allCategories) {
            double diff = avgPrefs1.getOrDefault(category, 0.0) -
                         avgPrefs2.getOrDefault(category, 0.0);
            distance += diff * diff;
        }

        return Math.sqrt(distance);
    }

    /**
     * Calculate average preferences for a cluster.
     */
    private Map<String, Double> calculateAveragePreferences(List<SocialAgent> cluster) {
        Map<String, Double> sumPrefs = new HashMap<>();
        int count = 0;

        for (SocialAgent agent : cluster) {
            Map<String, Double> prefs = agent.getContentPreferences();
            if (prefs.isEmpty()) continue;

            count++;
            for (Map.Entry<String, Double> entry : prefs.entrySet()) {
                sumPrefs.merge(entry.getKey(), entry.getValue(), Double::sum);
            }
        }

        Map<String, Double> avgPrefs = new HashMap<>();
        if (count > 0) {
            for (Map.Entry<String, Double> entry : sumPrefs.entrySet()) {
                avgPrefs.put(entry.getKey(), entry.getValue() / count);
            }
        }

        return avgPrefs;
    }

    /**
     * Normalize preference vector to sum to 1.0.
     */
    private Map<String, Double> normalizePreferences(Map<String, Double> prefs) {
        double sum = prefs.values().stream().mapToDouble(Double::doubleValue).sum();

        Map<String, Double> normalized = new HashMap<>();
        if (sum > 0) {
            for (Map.Entry<String, Double> entry : prefs.entrySet()) {
                normalized.put(entry.getKey(), entry.getValue() / sum);
            }
        }

        return normalized;
    }

    /**
     * Calculate content affinity score for recommendation algorithms.
     * Higher score = content is more aligned with agent's cluster preferences.
     */
    public double calculateContentAffinity(SocialAgent agent,
                                            Map<String, Double> contentFeatures,
                                            Map<String, Double> clusterMajority) {
        double agentAffinity = calculateCosineSimilarity(
            agent.getContentPreferences(), contentFeatures
        );

        double clusterAffinity = calculateCosineSimilarity(
            clusterMajority, contentFeatures
        );

        double homophilyWeight = agent.getHomophilyWeight();

        return (1 - homophilyWeight) * agentAffinity + homophilyWeight * clusterAffinity;
    }

    /**
     * Calculate cosine similarity between two preference vectors.
     */
    private double calculateCosineSimilarity(Map<String, Double> vec1,
                                              Map<String, Double> vec2) {
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        Set<String> allKeys = vec1.keySet();
        allKeys.addAll(vec2.keySet());

        for (String key : allKeys) {
            double v1 = vec1.getOrDefault(key, 0.0);
            double v2 = vec2.getOrDefault(key, 0.0);

            dotProduct += v1 * v2;
            norm1 += v1 * v1;
            norm2 += v2 * v2;
        }

        if (norm1 == 0 || norm2 == 0) return 0.0;

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    public List<ConvergenceSnapshot> getConvergenceHistory() {
        return new ArrayList<>(convergenceHistory);
    }

    /**
     * ConvergenceSnapshot - Records convergence state at iteration.
     */
    public static class ConvergenceSnapshot {
        public final int iteration;
        public final double avgDivergence;
        public final double polarization;

        public ConvergenceSnapshot(int iteration, double avgDivergence,
                                    double polarization) {
            this.iteration = iteration;
            this.avgDivergence = avgDivergence;
            this.polarization = polarization;
        }
    }

    /**
     * ConvergenceResult - Final result of preference convergence simulation.
     */
    public static class ConvergenceResult {
        private final boolean converged;
        private final int iterations;
        private final double finalDivergence;
        private final List<ConvergenceSnapshot> history;

        public ConvergenceResult(boolean converged, int iterations,
                                  double finalDivergence,
                                  List<ConvergenceSnapshot> history) {
            this.converged = converged;
            this.iterations = iterations;
            this.finalDivergence = finalDivergence;
            this.history = history;
        }

        public boolean isConverged() {
            return converged;
        }

        public int getIterations() {
            return iterations;
        }

        public double getFinalDivergence() {
            return finalDivergence;
        }

        public List<ConvergenceSnapshot> getHistory() {
            return history;
        }

        @Override
        public String toString() {
            return String.format(
                "ConvergenceResult{converged=%s, iterations=%d, divergence=%.4f}",
                converged, iterations, finalDivergence
            );
        }
    }
}
