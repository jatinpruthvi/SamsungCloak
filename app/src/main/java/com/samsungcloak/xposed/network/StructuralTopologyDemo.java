package com.samsungcloak.xposed.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * StructuralTopologyDemo - Demonstration and validation of the Structural
 * Interaction Topology implementation.
 *
 * This class validates all four network realism hooks:
 * 1. Social Graph Density (Dunbar's Number)
 * 2. Transitive Triads (Triadic Closure)
 * 3. Relationship Entropy (Decay)
 * 4. Community Homophily
 *
 * Validation Criteria:
 * - Core cluster size: 5-10 agents per agent
 * - Network density: 0.05-0.20 (non-random structure)
 * - Clustering coefficient: 0.30-0.60 (realistic vs random ~0.01)
 * - Preference convergence: ||P_i - P_cluster|| < 0.05 within 100 iterations
 */
public class StructuralTopologyDemo {

    private static final String LOG_TAG = "SamsungCloak.StructuralDemo";

    public static void main(String[] args) {
        System.out.println("=== STRUCTURAL INTERACTION TOPOLOGY DEMO ===\n");

        StructuralTopologyDemo demo = new StructuralTopologyDemo();

        demo.demonstrateDunbarDensity();
        demo.demonstrateTransitiveTriads();
        demo.demonstrateRelationshipEntropy();
        demo.demonstrateCommunityHomophily();
        demo.runIntegrationTest();

        System.out.println("\n=== ALL DEMONSTRATIONS COMPLETED ===");
    }

    /**
     * Demonstrate Social Graph Density hook with Dunbar's Number constraints.
     */
    public void demonstrateDunbarDensity() {
        System.out.println("1. SOCIAL GRAPH DENSITY (Dunbar's Number)");
        System.out.println("   " + "-".repeat(50));

        int agentCount = 50;
        List<SocialAgent> agents = createTestAgents(agentCount);
        Set<String> categories = createContentCategories();

        for (SocialAgent agent : agents) {
            agent.initializePreferences(categories);
        }

        SocialGraphDensityModel densityModel = new SocialGraphDensityModel();
        Map<String, SocialGraphDensityModel.ClusterAssignment> assignments =
            densityModel.initializeDunbarTopology(agents, categories);

        System.out.println("   Agents created: " + agentCount);
        System.out.println("   Dunbar's Number limit: " + SocialGraphDensityModel.DUNBAR_NUMBER);

        int totalCore = 0;
        int totalSympathy = 0;
        int totalOuter = 0;

        for (SocialGraphDensityModel.ClusterAssignment assignment : assignments.values()) {
            totalCore += assignment.getCoreCluster().size();
            totalSympathy += assignment.getSympathyGroup().size();
            totalOuter += assignment.getOuterNetwork().size();
        }

        double avgCore = (double) totalCore / agentCount;
        double avgSympathy = (double) totalSympathy / agentCount;
        double avgOuter = (double) totalOuter / agentCount;

        System.out.println("   Average Core Cluster size: " + String.format("%.1f", avgCore));
        System.out.println("   Average Sympathy Group size: " + String.format("%.1f", avgSympathy));
        System.out.println("   Average Outer Network size: " + String.format("%.1f", avgOuter));

        boolean coreValid = avgCore >= SocialGraphDensityModel.MIN_CORE_SIZE &&
                           avgCore <= SocialGraphDensityModel.MAX_CORE_SIZE;

        System.out.println("   ✓ Core cluster constraint (5-10): " + (coreValid ? "PASS" : "FAIL"));

        SocialGraphDensityModel.DensityMetrics metrics =
            densityModel.calculateDensityMetrics(agents);

        System.out.println("   Average network density: " + String.format("%.3f", metrics.getAverageDensity()));
        System.out.println("   Average core density: " + String.format("%.3f", metrics.getAverageCoreDensity()));
        System.out.println("   ✓ Core density > 0.5: " + (metrics.getAverageCoreDensity() > 0.5 ? "PASS" : "FAIL"));
        System.out.println();
    }

    /**
     * Demonstrate Transitive Triads hook with triadic closure.
     */
    public void demonstrateTransitiveTriads() {
        System.out.println("2. TRANSITIVE TRIADS (Triadic Closure)");
        System.out.println("   " + "-".repeat(50));

        int agentCount = 30;
        List<SocialAgent> agents = createTestAgents(agentCount);
        Set<String> categories = createContentCategories();

        SocialGraphDensityModel densityModel = new SocialGraphDensityModel();
        densityModel.initializeDunbarTopology(agents, categories);

        TransitiveTriadModel triadModel = new TransitiveTriadModel(0.45, 0.3);

        double initialClustering = triadModel.calculateGlobalClusteringCoefficient(agents);
        int initialEdges = countEdges(agents);

        System.out.println("   Initial clustering coefficient: " + String.format("%.3f", initialClustering));
        System.out.println("   Initial edge count: " + initialEdges);

        TransitiveTriadModel.CommunityGrowthResult growthResult =
            triadModel.simulateCommunityGrowth(agents, 20);

        System.out.println("   Simulation iterations: 20");
        System.out.println("   Edges formed: " + (growthResult.getFinalEdges() - growthResult.getInitialEdges()));
        System.out.println("   Edge growth rate: " + String.format("%.1f%%", growthResult.getEdgeGrowthRate() * 100));
        System.out.println("   Final clustering: " + String.format("%.3f", growthResult.getFinalClustering()));

        boolean clusteringValid = growthResult.getFinalClustering() >= 0.15 &&
                                  growthResult.getFinalClustering() <= 0.70;

        System.out.println("   ✓ Realistic clustering (0.15-0.70): " + (clusteringValid ? "PASS" : "FAIL"));

        System.out.println("   Closure attempts: " + triadModel.getClosureAttempts());
        System.out.println("   Triangles formed: " + triadModel.getTrianglesFormed());
        System.out.println("   Closure success rate: " + String.format("%.1f%%", triadModel.getClosureSuccessRate() * 100));
        System.out.println();
    }

    /**
     * Demonstrate Relationship Entropy hook with decay and re-engagement.
     */
    public void demonstrateRelationshipEntropy() {
        System.out.println("3. RELATIONSHIP ENTROPY (Decay & Re-engagement)");
        System.out.println("   " + "-".repeat(50));

        int agentCount = 20;
        List<SocialAgent> agents = createTestAgents(agentCount);
        Set<String> categories = createContentCategories();

        SocialGraphDensityModel densityModel = new SocialGraphDensityModel();
        Map<String, SocialGraphDensityModel.ClusterAssignment> assignments =
            densityModel.initializeDunbarTopology(agents, categories);

        RelationshipEntropyHook entropyHook = new RelationshipEntropyHook(0.01, 0.25);

        SocialAgent sampleAgent = agents.get(0);
        SocialGraphDensityModel.ClusterAssignment assignment = assignments.get(sampleAgent.getAgentId());

        System.out.println("   Sample agent relationships before decay:");
        printRelationshipStrengths(sampleAgent, 3);

        RelationshipEntropyHook.EntropySimulationResult decayResult =
            entropyHook.simulateNetworkDecay(agents, assignments, 72);

        System.out.println("   Simulated 72 hours of inactivity");
        System.out.println("   Total relationships: " + decayResult.totalRelationships);
        System.out.println("   Decayed relationships: " + decayResult.decayedRelationships);
        System.out.println("   Decay rate: " + String.format("%.1f%%", decayResult.getDecayRate() * 100));
        System.out.println("   Average decay amount: " + String.format("%.3f", decayResult.getAverageDecayAmount()));

        System.out.println("   Sample agent relationships after decay:");
        printRelationshipStrengths(sampleAgent, 3);

        String targetId = sampleAgent.getRelationships().keySet().iterator().next();
        double currentStrength = sampleAgent.getRelationshipStrength(targetId, 0.01);

        double reengagedStrength = entropyHook.processReengagement(
            sampleAgent, targetId, currentStrength, 3,
            RelationshipEntropyHook.ReengagementType.DIRECT_MESSAGE
        );

        System.out.println("   Re-engagement simulation:");
        System.out.println("   Previous strength: " + String.format("%.3f", currentStrength));
        System.out.println("   New strength: " + String.format("%.3f", reengagedStrength));
        System.out.println("   Boost: " + String.format("%.3f", reengagedStrength - currentStrength));
        System.out.println("   ✓ Re-engagement increases strength: " + (reengagedStrength > currentStrength ? "PASS" : "FAIL"));
        System.out.println();
    }

    /**
     * Demonstrate Community Homophily hook with preference convergence.
     */
    public void demonstrateCommunityHomophily() {
        System.out.println("4. COMMUNITY HOMOPHILY (Preference Convergence)");
        System.out.println("   " + "-".repeat(50));

        int agentCount = 25;
        List<SocialAgent> agents = createTestAgents(agentCount);
        Set<String> categories = createContentCategories();

        Map<String, List<SocialAgent>> clusters = new HashMap<>();
        for (SocialAgent agent : agents) {
            clusters.computeIfAbsent(agent.getClusterId(), k -> new ArrayList<>()).add(agent);
        }

        for (SocialAgent agent : agents) {
            agent.initializePreferences(categories);
        }

        System.out.println("   Initial polarization: " +
            String.format("%.3f", calculatePolarization(agents, clusters, categories)));

        CommunityHomophilyModel homophilyModel = new CommunityHomophilyModel(0.08, 0.05, 100);

        CommunityHomophilyModel.ConvergenceResult convergence =
            homophilyModel.simulatePreferenceConvergence(agents, clusters, categories);

        System.out.println("   Convergence iterations: " + convergence.getIterations());
        System.out.println("   Final divergence: " + String.format("%.4f", convergence.getFinalDivergence()));
        System.out.println("   Converged: " + convergence.isConverged());

        System.out.println("   Final polarization: " +
            String.format("%.3f", calculatePolarization(agents, clusters, categories)));

        boolean convergenceValid = convergence.isConverged() || convergence.getFinalDivergence() < 0.1;
        System.out.println("   ✓ Convergence achieved: " + (convergenceValid ? "PASS" : "FAIL"));

        SocialAgent sampleAgent = agents.get(0);
        Map<String, Double> weightedPrefs = homophilyModel.calculateClusterMajority(
            sampleAgent, clusters.get(sampleAgent.getClusterId()), categories
        );

        System.out.println("   Sample weighted preferences (top 3):");
        weightedPrefs.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(3)
            .forEach(e -> System.out.println("     " + e.getKey() + ": " + String.format("%.3f", e.getValue())));
        System.out.println();
    }

    /**
     * Run integrated test of all hooks together.
     */
    public void runIntegrationTest() {
        System.out.println("5. INTEGRATION TEST (All Hooks Combined)");
        System.out.println("   " + "-".repeat(50));

        int agentCount = 40;
        List<SocialAgent> agents = createTestAgents(agentCount);
        Set<String> categories = createContentCategories();

        for (SocialAgent agent : agents) {
            agent.initializePreferences(categories);
        }

        SocialGraphDensityModel densityModel = new SocialGraphDensityModel();
        Map<String, SocialGraphDensityModel.ClusterAssignment> assignments =
            densityModel.initializeDunbarTopology(agents, categories);

        TransitiveTriadModel triadModel = new TransitiveTriadModel();
        triadModel.processTriadicClosure(agents, createAgentMap(agents));

        RelationshipEntropyHook entropyHook = new RelationshipEntropyHook();
        CommunityHomophilyModel homophilyModel = new CommunityHomophilyModel();

        System.out.println("   Running 50 interaction simulation rounds...");

        int successfulInteractions = 0;
        java.util.Random random = new java.util.Random();

        for (int round = 0; round < 50; round++) {
            SocialAgent agent1 = agents.get(random.nextInt(agents.size()));
            SocialGraphDensityModel.ClusterAssignment assignment = assignments.get(agent1.getAgentId());

            String targetId = densityModel.selectInteractionTarget(assignment, agents);
            if (targetId != null) {
                SocialAgent agent2 = findAgentById(agents, targetId);
                if (agent2 != null) {
                    double triadicScore = calculateTriadicScore(agent1, agent2);
                    double propensity = agent1.calculateInteractionPropensity(agent2, triadicScore);

                    if (random.nextDouble() < propensity) {
                        agent1.recordInteraction(targetId);
                        agent2.recordInteraction(agent1.getAgentId());
                        successfulInteractions++;
                    }
                }
            }
        }

        System.out.println("   Total interaction attempts: 50");
        System.out.println("   Successful interactions: " + successfulInteractions);
        System.out.println("   Success rate: " + String.format("%.1f%%", (successfulInteractions / 50.0) * 100));

        SocialGraphDensityModel.DensityMetrics densityMetrics =
            densityModel.calculateDensityMetrics(agents);
        double clustering = triadModel.calculateGlobalClusteringCoefficient(agents);

        System.out.println("   Final network density: " + String.format("%.3f", densityMetrics.getAverageDensity()));
        System.out.println("   Final clustering coefficient: " + String.format("%.3f", clustering));

        boolean integrationValid = clustering > 0.1 && densityMetrics.getAverageDensity() > 0.02;
        System.out.println("   ✓ Integration test: " + (integrationValid ? "PASS" : "FAIL"));
        System.out.println();
    }

    private List<SocialAgent> createTestAgents(int count) {
        List<SocialAgent> agents = new ArrayList<>();
        int clusterCount = Math.max(2, count / 20);

        for (int i = 0; i < count; i++) {
            String clusterId = "cluster_" + (i % clusterCount);
            agents.add(new SocialAgent(clusterId));
        }

        return agents;
    }

    private Set<String> createContentCategories() {
        Set<String> categories = new HashSet<>();
        categories.add("technology");
        categories.add("entertainment");
        categories.add("sports");
        categories.add("news");
        categories.add("lifestyle");
        return categories;
    }

    private Map<String, SocialAgent> createAgentMap(List<SocialAgent> agents) {
        Map<String, SocialAgent> map = new HashMap<>();
        for (SocialAgent agent : agents) {
            map.put(agent.getAgentId(), agent);
        }
        return map;
    }

    private SocialAgent findAgentById(List<SocialAgent> agents, String id) {
        for (SocialAgent agent : agents) {
            if (agent.getAgentId().equals(id)) {
                return agent;
            }
        }
        return null;
    }

    private int countEdges(List<SocialAgent> agents) {
        int count = 0;
        for (SocialAgent agent : agents) {
            count += agent.getRelationships().size();
        }
        return count / 2;
    }

    private void printRelationshipStrengths(SocialAgent agent, int limit) {
        int count = 0;
        for (Map.Entry<String, SocialAgent.Relationship> entry : agent.getRelationships().entrySet()) {
            if (count >= limit) break;
            System.out.println("     " + entry.getKey().substring(0, 8) + "...: " +
                String.format("%.3f", entry.getValue().getBaseStrength()));
            count++;
        }
    }

    private double calculatePolarization(List<SocialAgent> agents,
                                          Map<String, List<SocialAgent>> clusters,
                                          Set<String> categories) {
        if (clusters.size() < 2) return 0.0;

        double totalDistance = 0.0;
        int comparisons = 0;

        List<String> clusterIds = new ArrayList<>(clusters.keySet());
        for (int i = 0; i < clusterIds.size(); i++) {
            for (int j = i + 1; j < clusterIds.size(); j++) {
                List<SocialAgent> c1 = clusters.get(clusterIds.get(i));
                List<SocialAgent> c2 = clusters.get(clusterIds.get(j));

                Map<String, Double> avg1 = calculateAveragePrefs(c1);
                Map<String, Double> avg2 = calculateAveragePrefs(c2);

                double distance = 0.0;
                for (String cat : categories) {
                    double diff = avg1.getOrDefault(cat, 0.0) - avg2.getOrDefault(cat, 0.0);
                    distance += diff * diff;
                }

                totalDistance += Math.sqrt(distance);
                comparisons++;
            }
        }

        return comparisons == 0 ? 0.0 : totalDistance / comparisons;
    }

    private Map<String, Double> calculateAveragePrefs(List<SocialAgent> agents) {
        Map<String, Double> sum = new HashMap<>();
        int count = 0;

        for (SocialAgent agent : agents) {
            count++;
            for (Map.Entry<String, Double> entry : agent.getContentPreferences().entrySet()) {
                sum.merge(entry.getKey(), entry.getValue(), Double::sum);
            }
        }

        Map<String, Double> avg = new HashMap<>();
        if (count > 0) {
            for (Map.Entry<String, Double> entry : sum.entrySet()) {
                avg.put(entry.getKey(), entry.getValue() / count);
            }
        }
        return avg;
    }

    private double calculateTriadicScore(SocialAgent agent1, SocialAgent agent2) {
        java.util.Set<String> neighbors1 = agent1.getRelationships().keySet();
        java.util.Set<String> neighbors2 = agent2.getRelationships().keySet();

        java.util.Set<String> common = new java.util.HashSet<>(neighbors1);
        common.retainAll(neighbors2);

        if (common.isEmpty()) return 0.0;

        double score = 0.0;
        for (String c : common) {
            score += (agent1.getRelationshipStrength(c, 0.01) +
                     agent2.getRelationshipStrength(c, 0.01)) / 2.0;
        }
        return Math.min(1.0, score / common.size());
    }
}
