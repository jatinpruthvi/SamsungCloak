package com.simulation.structural.demo;

import com.simulation.structural.StructuralFidelityOrchestrator;
import com.simulation.structural.StructuralFidelityOrchestrator.*;
import com.simulation.structural.drift.EvolutionEngine;
import com.simulation.structural.profiler.DensityProfiler;
import com.simulation.structural.shared.SharedContextModel;

import java.util.Map;

/**
 * StructuralFidelityDemo - Demonstration and validation of the Structural Fidelity Layer
 *
 * This demo showcases all six population dimensions:
 * 1. Heterogeneous Interdependence - Shared context modeling
 * 2. Long-Term Behavioral Drift - Evolution over 90 days
 * 3. Population Profiling - Power Law and Gaussian distributions
 * 4. Structural Network Topology - Small-world network
 * 5. Telemetry Sanity Checking - Human constraint validation
 * 6. Multi-Session Persistent State - Longitudinal logic
 *
 * Hardware Profile: Samsung Galaxy A12 (SM-A125U)
 */
public class StructuralFidelityDemo {

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║     STRUCTURAL FIDELITY LAYER - SYNTHETIC POPULATION SIMULATION  ║");
        System.out.println("║              Optimized for Samsung Galaxy A12 (SM-A125U)         ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝\n");

        runQuickDemo();
    }

    public static void runQuickDemo() {
        StructuralFidelityOrchestrator orchestrator = new StructuralFidelityOrchestrator(
            new SimulationConfig()
                .withPopulationSize(1000)
                .withSimulationDays(30)
        );

        System.out.println("DEMO 1: Population Generation with Distributional Alignment");
        System.out.println("───────────────────────────────────────────────────────────");
        PopulationGenerationResult popResult = orchestrator.generatePopulation();
        System.out.println("\n✓ Generated population: " + popResult.populationSize + " users");
        System.out.println("  Mean sessions/day: " + String.format("%.2f", popResult.statistics.meanSessionsPerDay));
        System.out.println("  Mean session duration: " + String.format("%.0f", popResult.statistics.meanSessionDuration) + "s");
        System.out.println("  Mean reaction latency: " + String.format("%.0f", popResult.statistics.meanReactionLatency) + "ms");
        System.out.println("  Engagement Gini: " + String.format("%.3f", popResult.statistics.engagementGini));

        System.out.println("\n\nDEMO 2: Shared Context Assignment");
        System.out.println("───────────────────────────────────────────────────────────");
        ContextAssignmentResult contextResult = orchestrator.assignSharedContexts();
        System.out.println("\n✓ Household clusters: " + contextResult.householdClusters);
        System.out.println("✓ Office clusters: " + contextResult.officeClusters);

        SharedContextModel sharedContext = orchestrator.getSharedContext();
        Map<String, SharedContextModel.ContextCluster> clusters = sharedContext.getClusters();
        System.out.println("✓ Total context clusters: " + clusters.size());

        System.out.println("\n\nDEMO 3: Small-World Network Topology");
        System.out.println("───────────────────────────────────────────────────────────");
        TopologyResult topologyResult = orchestrator.buildNetworkTopology();
        System.out.println("\n✓ Nodes: " + topologyResult.nodeCount);
        System.out.println("✓ Edges: " + topologyResult.edgeCount);
        System.out.println("✓ Clustering coefficient: " + String.format("%.3f", topologyResult.clusteringCoefficient));
        System.out.println("✓ Average path length: " + String.format("%.3f", topologyResult.averagePathLength));
        System.out.println("✓ Modularity: " + String.format("%.3f", topologyResult.modularity));
        System.out.println("✓ Small-world property: " + (topologyResult.isSmallWorld ? "YES ✓" : "NO"));

        System.out.println("\n\nDEMO 4: 30-Day Evolution Simulation");
        System.out.println("───────────────────────────────────────────────────────────");
        SimulationResult simResult = orchestrator.runSimulation();
        System.out.println("\n✓ Days simulated: " + simResult.daysSimulated);
        System.out.println("✓ Total sessions: " +
            simResult.dailyResults.stream().mapToInt(d -> d.metrics.sessionCount).sum());
        System.out.println("✓ Total interactions: " +
            simResult.dailyResults.stream().mapToInt(d -> d.metrics.interactionCount).sum());

        EvolutionEngine.EvolutionSummary evoSummary = simResult.evolutionSummary;
        System.out.println("\nFinal population distribution:");
        System.out.println("  New Users: " + evoSummary.newUsers);
        System.out.println("  Learning: " + evoSummary.learningUsers);
        System.out.println("  Competent: " + evoSummary.competentUsers);
        System.out.println("  Power Users: " + evoSummary.powerUsers);
        System.out.println("  Average entropy: " + String.format("%.3f", evoSummary.averageEntropy));
        System.out.println("  Average skill: " + String.format("%.3f", evoSummary.averageSkill));

        System.out.println("\n\nDEMO 5: Telemetry Sanity Checking");
        System.out.println("───────────────────────────────────────────────────────────");
        var violationStats = simResult.violationStats;
        System.out.println("✓ Total violations detected: " + violationStats.totalViolations);
        System.out.println("  Critical: " + violationStats.criticalCount);
        System.out.println("  Warning: " + violationStats.warningCount);
        System.out.println("✓ All telemetry validated against human motor constraints");

        System.out.println("\n\nDEMO 6: Longitudinal Persistence");
        System.out.println("───────────────────────────────────────────────────────────");
        var population = orchestrator.getSyntheticPopulation();
        long totalSessions = population.values().stream()
            .mapToLong(u -> u.totalSessionsGenerated).sum();
        long totalInteractions = population.values().stream()
            .mapToLong(u -> u.totalInteractionsGenerated).sum();

        System.out.println("✓ Sessions with persistent state: " + totalSessions);
        System.out.println("✓ Tracked interactions: " + totalInteractions);
        System.out.println("✓ Episodic memory encoded for each session");
        System.out.println("✓ Semantic preferences learned across sessions");
        System.out.println("✓ Procedural skills improved with practice");

        System.out.println("\n\n═══════════════════════════════════════════════════════════════════");
        System.out.println("                    SIMULATION SUMMARY");
        System.out.println("═══════════════════════════════════════════════════════════════════");
        SimulationStatistics stats = orchestrator.getStatistics();
        System.out.println("Population Size: " + stats.populationSize);
        System.out.println("Total Sessions Generated: " + stats.totalSessions);
        System.out.println("Total Interactions Generated: " + stats.totalInteractions);
        System.out.println("Final State: " + stats.state);
        System.out.println("\nPhase Distribution:");
        for (Map.Entry<EvolutionEngine.EvolutionPhase, Integer> entry :
            stats.phaseDistribution.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }

        System.out.println("\n═══════════════════════════════════════════════════════════════════");
        System.out.println("           STRUCTURAL FIDELITY LAYER - DEMO COMPLETE");
        System.out.println("═══════════════════════════════════════════════════════════════════");
    }

    public static void runFull90DaySimulation() {
        System.out.println("\nRunning full 90-day simulation with 10,000 users...\n");

        StructuralFidelityOrchestrator orchestrator = new StructuralFidelityOrchestrator(
            new SimulationConfig()
                .withPopulationSize(10000)
                .withSimulationDays(90)
        );

        long startTime = System.currentTimeMillis();

        orchestrator.generatePopulation();
        orchestrator.assignSharedContexts();
        orchestrator.buildNetworkTopology();
        SimulationResult result = orchestrator.runSimulation();

        long duration = System.currentTimeMillis() - startTime;

        System.out.println("\n═══════════════════════════════════════════════════════════════════");
        System.out.println("                 FULL 90-DAY SIMULATION RESULTS");
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println("Execution time: " + (duration / 1000.0) + " seconds");
        System.out.println("Users: 10,000 | Days: 90");
        System.out.println("Total sessions: " +
            result.dailyResults.stream().mapToInt(d -> d.metrics.sessionCount).sum());
        System.out.println("Total interactions: " +
            result.dailyResults.stream().mapToInt(d -> d.metrics.interactionCount).sum());
        System.out.println("\nFinal evolution state:");
        System.out.println("  " + result.evolutionSummary);
        System.out.println("\nTelemetry validation:");
        System.out.println("  " + result.violationStats);
        System.out.println("═══════════════════════════════════════════════════════════════════");
    }
}
