package com.simulation.structural;

import com.simulation.structural.drift.EvolutionEngine;
import com.simulation.structural.longitudinal.LongitudinalLogic;
import com.simulation.structural.network.SmallWorldNetwork;
import com.simulation.structural.profiler.DensityProfiler;
import com.simulation.structural.sanity.ThresholdMonitor;
import com.simulation.structural.shared.SharedContextModel;

import java.time.LocalDateTime;
import java.util.*;

/**
 * StructuralFidelityOrchestrator - Master Controller for Synthetic Population Simulation
 *
 * Integrates all six Structural Fidelity Layers into a cohesive simulation framework
 * optimized for the Samsung Galaxy A12 (SM-A125U) hardware profile.
 *
 * Integration Architecture:
 *
 * ┌─────────────────────────────────────────────────────────────────┐
 * │               StructuralFidelityOrchestrator                     │
 * │                    (Master Integration Layer)                    │
 * └──────┬──────────────┬──────────────┬──────────────┬─────────────┘
 *        │              │              │              │
 *        ▼              ▼              ▼              ▼
 * ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐
 * │  Shared    │ │ Evolution  │ │  Density   │ │   Small    │
 * │  Context   │ │   Engine   │ │  Profiler  │ │   World    │
 * │  Model     │ │            │ │            │ │  Network   │
 * └────────────┘ └────────────┘ └────────────┘ └────────────┘
 *        │                                      │
 *        ▼                                      ▼
 * ┌────────────┐                       ┌────────────┐
 * │ Threshold  │                       │Longitudinal│
 * │  Monitor   │                       │   Logic    │
 * └────────────┘                       └────────────┘
 *
 * Data Flow:
 * 1. Density Profiler generates population with realistic distributions
 * 2. Shared Context clusters devices by environmental similarity
 * 3. Small World Network establishes interaction topology
 * 4. Evolution Engine progresses users through behavioral phases
 * 5. Longitudinal Logic maintains session-to-session persistence
 * 6. Threshold Monitor validates all telemetry against human constraints
 *
 * SM-A125U Hardware Profile:
 * - Display: 720x1600, 6.5" PLS TFT
 * - Touch latency baseline: 28ms
 * - Network: WiFi/4G hybrid with realistic constraints
 */
public class StructuralFidelityOrchestrator {

    private static final String LOG_TAG = "StructuralFidelity.Orchestrator";

    private final SharedContextModel sharedContext;
    private final EvolutionEngine evolutionEngine;
    private final DensityProfiler densityProfiler;
    private final SmallWorldNetwork networkTopology;
    private final ThresholdMonitor thresholdMonitor;
    private final LongitudinalLogic longitudinalLogic;

    private final SimulationConfig config;
    private final Map<String, SyntheticUser> syntheticPopulation;
    private SimulationState currentState;

    public StructuralFidelityOrchestrator() {
        this(new SimulationConfig());
    }

    public StructuralFidelityOrchestrator(SimulationConfig config) {
        this.config = config;
        this.sharedContext = new SharedContextModel();
        this.evolutionEngine = new EvolutionEngine();
        this.densityProfiler = new DensityProfiler(config.populationParams);
        this.networkTopology = new SmallWorldNetwork();
        this.thresholdMonitor = new ThresholdMonitor(config.violationHandler);
        this.longitudinalLogic = new LongitudinalLogic();
        this.syntheticPopulation = new HashMap<>();
        this.currentState = SimulationState.INITIALIZED;
    }

    /**
     * Simulation configuration
     */
    public static class SimulationConfig {
        public int populationSize = 10000;
        public int simulationDays = 90;
        public int sessionsPerDayTarget = 4;
        public int clusterCount = 500;
        public DensityProfiler.PopulationParameters populationParams =
            new DensityProfiler.PopulationParameters();
        public ThresholdMonitor.ViolationHandler violationHandler =
            ThresholdMonitor.ViolationHandler.LOG_AND_CORRECT;
        public boolean enableNetworkTopology = true;
        public boolean enableLongitudinalPersistence = true;

        public SimulationConfig withPopulationSize(int size) {
            this.populationSize = size;
            return this;
        }

        public SimulationConfig withSimulationDays(int days) {
            this.simulationDays = days;
            return this;
        }
    }

    /**
     * Synthetic user entity combining all dimension aspects
     */
    public static class SyntheticUser {
        public final String userId;
        public final String deviceId;
        public final String clusterId;
        public final DensityProfiler.UserProfile profile;

        public EvolutionEngine.UserEvolutionProfile evolution;
        public LongitudinalLogic.UserLongitudinalState longitudinal;
        public SmallWorldNetwork.NetworkNode networkNode;

        public int totalSessionsGenerated;
        public long totalInteractionsGenerated;
        public long lastSessionTimestamp;

        public SyntheticUser(String userId, String deviceId, String clusterId,
                             DensityProfiler.UserProfile profile) {
            this.userId = userId;
            this.deviceId = deviceId;
            this.clusterId = clusterId;
            this.profile = profile;
        }
    }

    public enum SimulationState {
        INITIALIZED,
        POPULATION_GENERATED,
        CONTEXT_ASSIGNED,
        TOPOLOGY_BUILT,
        EVOLUTION_RUNNING,
        COMPLETED
    }

    /**
     * Phase 1: Generate population with distributional alignment
     */
    public PopulationGenerationResult generatePopulation() {
        System.out.println("Phase 1: Generating synthetic population with distributional alignment...");

        List<DensityProfiler.UserProfile> profiles =
            densityProfiler.generatePopulationProfiles(config.populationSize);

        DensityProfiler.PopulationStatistics stats =
            densityProfiler.calculatePopulationStatistics(profiles);

        int devicesPerCluster = Math.max(1, config.populationSize / config.clusterCount);

        for (int i = 0; i < profiles.size(); i++) {
            String userId = "USER_" + i;
            String deviceId = "SM-A125U_" + UUID.randomUUID().toString().substring(0, 8);
            String clusterId = "CLUSTER_" + (i / devicesPerCluster);

            SyntheticUser user = new SyntheticUser(userId, deviceId, clusterId, profiles.get(i));
            syntheticPopulation.put(userId, user);

            evolutionEngine.registerUser(userId);
            user.evolution = evolutionEngine.getUserProfile(userId);
            user.longitudinal = longitudinalLogic.getOrCreateUserState(userId);
        }

        currentState = SimulationState.POPULATION_GENERATED;
        System.out.println("Generated " + syntheticPopulation.size() + " synthetic users");
        System.out.println("Population stats: " + stats);

        return new PopulationGenerationResult(syntheticPopulation.size(), stats);
    }

    /**
     * Phase 2: Assign shared contexts (household/office clusters)
     */
    public ContextAssignmentResult assignSharedContexts() {
        System.out.println("\nPhase 2: Assigning shared context clusters...");

        int householdCount = config.clusterCount / 3;
        int officeCount = config.clusterCount / 6;

        for (int i = 0; i < householdCount; i++) {
            int devicesInHousehold = 2 + (int) (Math.random() * 4);
            SharedContextModel householdModel = SharedContextModel.createHouseholdCluster(
                devicesInHousehold, "HH_" + i
            );

            for (String clusterId : householdModel.getClusters().keySet()) {
                SharedContextModel.ContextCluster cluster =
                    householdModel.getClusters().get(clusterId);
                for (String deviceId : cluster.getDeviceIds()) {
                    sharedContext.createCluster(
                        clusterId, cluster.getType(),
                        cluster.getNetworkProfile(),
                        cluster.getTemporalArchetype()
                    );
                }
            }
        }

        for (int i = 0; i < officeCount; i++) {
            int devicesInOffice = 5 + (int) (Math.random() * 20);
            SharedContextModel.createOfficeCluster(devicesInOffice, "OFFICE_" + i);
        }

        int assignedCount = 0;
        for (SyntheticUser user : syntheticPopulation.values()) {
            SharedContextModel.NetworkProfile networkProfile =
                sharedContext.getContextualNetworkProfile(user.deviceId);
            if (networkProfile != null) {
                assignedCount++;
            }
        }

        currentState = SimulationState.CONTEXT_ASSIGNED;
        System.out.println("Assigned shared contexts to " + assignedCount + " devices");

        return new ContextAssignmentResult(householdCount, officeCount, assignedCount);
    }

    /**
     * Phase 3: Build small-world network topology
     */
    public TopologyResult buildNetworkTopology() {
        System.out.println("\nPhase 3: Building small-world network topology...");

        if (!config.enableNetworkTopology) {
            System.out.println("Network topology disabled in config");
            return new TopologyResult(0, 0, 0, 0, 0, false);
        }

        int k = Math.min(8, config.populationSize / 20);
        double rewireProb = 0.12;

        networkTopology.buildWattsStrogatzNetwork(config.populationSize, k, rewireProb);
        networkTopology.applyTriadicClosure(0.25);

        Map<String, String> communities = networkTopology.detectCommunities();

        List<SyntheticUser> userList = new ArrayList<>(syntheticPopulation.values());
        for (int i = 0; i < userList.size(); i++) {
            SyntheticUser user = userList.get(i);
            String nodeId = "node_" + i;
            SmallWorldNetwork.NetworkNode node = networkTopology.getNodes().get(nodeId);
            if (node != null) {
                user.networkNode = node;
                String communityId = communities.get(nodeId);
                if (communityId != null) {
                    node.setCommunity(communityId);
                }
            }
        }

        SmallWorldNetwork.NetworkMetrics metrics = networkTopology.getMetrics();
        currentState = SimulationState.TOPOLOGY_BUILT;
        System.out.println("Network topology: " + metrics);

        return new TopologyResult(
            metrics.nodeCount, metrics.edgeCount,
            metrics.clusteringCoefficient, metrics.averagePathLength,
            metrics.modularity, metrics.isSmallWorld()
        );
    }

    /**
     * Phase 4: Run full evolution simulation
     */
    public SimulationResult runSimulation() {
        System.out.println("\nPhase 4: Running " + config.simulationDays + "-day evolution simulation...");

        currentState = SimulationState.EVOLUTION_RUNNING;

        List<DailyResult> dailyResults = new ArrayList<>();

        for (int day = 1; day <= config.simulationDays; day++) {
            evolutionEngine.setCurrentSimulationDay(day);

            DailyMetrics dailyMetrics = simulateDay(day);
            dailyResults.add(new DailyResult(day, dailyMetrics));

            if (day % 10 == 0) {
                System.out.println("Day " + day + ": " + dailyMetrics);
            }
        }

        currentState = SimulationState.COMPLETED;

        EvolutionEngine.EvolutionSummary evolutionSummary = evolutionEngine.getSummary();
        ThresholdMonitor.ViolationStatistics violationStats = thresholdMonitor.getViolationStatistics();

        System.out.println("\n=== SIMULATION COMPLETE ===");
        System.out.println("Evolution summary: " + evolutionSummary);
        System.out.println("Sanity violations: " + violationStats);

        return new SimulationResult(config.simulationDays, dailyResults, evolutionSummary, violationStats);
    }

    private DailyMetrics simulateDay(int day) {
        int sessionsToday = 0;
        int interactionsToday = 0;
        long totalLatencyToday = 0;
        int violationsToday = 0;

        for (SyntheticUser user : syntheticPopulation.values()) {
            double dauProbability = user.profile.dailyActiveProbability;

            if (day <= 7 || Math.random() < dauProbability) {
                int sessionsForUser = (int) (user.profile.sessionsPerDay *
                    (0.8 + Math.random() * 0.4));

                for (int s = 0; s < sessionsForUser; s++) {
                    SessionMetrics metrics = simulateSession(user, day);
                    sessionsToday++;
                    interactionsToday += metrics.interactionCount;
                    totalLatencyToday += metrics.totalLatency;
                    violationsToday += metrics.violations;
                }
            }
        }

        evolutionEngine.advanceSimulationDay();

        long avgLatency = sessionsToday > 0 ? totalLatencyToday / sessionsToday : 0;
        return new DailyMetrics(sessionsToday, interactionsToday, avgLatency, violationsToday);
    }

    private SessionMetrics simulateSession(SyntheticUser user, int day) {
        LongitudinalLogic.SessionPreparation prep =
            longitudinalLogic.prepareSession(user.userId);

        int interactionCount = (int) (user.profile.contentItemsPerSession *
            (0.7 + Math.random() * 0.6));

        long totalLatency = 0;
        int violations = 0;

        for (int i = 0; i < interactionCount; i++) {
            long baseLatency = evolutionEngine.predictNavigationLatency(user.userId, 200, 44);

            long validatedLatency = thresholdMonitor.validateReactionTime(baseLatency,
                "day" + day + "/session");

            if (validatedLatency != baseLatency) {
                violations++;
            }

            totalLatency += validatedLatency;
        }

        user.totalSessionsGenerated++;
        user.totalInteractionsGenerated += interactionCount;
        user.lastSessionTimestamp = System.currentTimeMillis();

        LongitudinalLogic.SessionRecord record = new LongitudinalLogic.SessionRecord(
            "session_" + user.userId + "_" + day + "_" + user.totalSessionsGenerated,
            LocalDateTime.now().minusMinutes(interactionCount * 2),
            LocalDateTime.now(),
            interactionCount,
            user.profile.typicalSessionDuration > 300 ?
                List.of("feature_a", "feature_b") : List.of("feature_a"),
            List.of(),
            List.of(),
            Map.of("feature_a", (double) interactionCount),
            0.7 + Math.random() * 0.3
        );
        longitudinalLogic.recordSession(user.userId, record);

        return new SessionMetrics(interactionCount, totalLatency, violations);
    }

    /**
     * Get comprehensive simulation statistics
     */
    public SimulationStatistics getStatistics() {
        int totalSessions = syntheticPopulation.values().stream()
            .mapToInt(u -> u.totalSessionsGenerated).sum();
        long totalInteractions = syntheticPopulation.values().stream()
            .mapToLong(u -> u.totalInteractionsGenerated).sum();

        Map<EvolutionEngine.EvolutionPhase, Integer> phaseDistribution = new HashMap<>();
        for (SyntheticUser user : syntheticPopulation.values()) {
            if (user.evolution != null) {
                phaseDistribution.merge(user.evolution.getCurrentPhase(), 1, Integer::sum);
            }
        }

        return new SimulationStatistics(
            syntheticPopulation.size(),
            totalSessions,
            totalInteractions,
            phaseDistribution,
            currentState
        );
    }

    // Result classes

    public static class PopulationGenerationResult {
        public final int populationSize;
        public final DensityProfiler.PopulationStatistics statistics;

        public PopulationGenerationResult(int populationSize,
                                          DensityProfiler.PopulationStatistics statistics) {
            this.populationSize = populationSize;
            this.statistics = statistics;
        }
    }

    public static class ContextAssignmentResult {
        public final int householdClusters;
        public final int officeClusters;
        public final int assignedDevices;

        public ContextAssignmentResult(int householdClusters, int officeClusters,
                                       int assignedDevices) {
            this.householdClusters = householdClusters;
            this.officeClusters = officeClusters;
            this.assignedDevices = assignedDevices;
        }
    }

    public static class TopologyResult {
        public final int nodeCount;
        public final int edgeCount;
        public final double clusteringCoefficient;
        public final double averagePathLength;
        public final double modularity;
        public final boolean isSmallWorld;

        public TopologyResult(int nodeCount, int edgeCount, double clusteringCoefficient,
                              double averagePathLength, double modularity,
                              boolean isSmallWorld) {
            this.nodeCount = nodeCount;
            this.edgeCount = edgeCount;
            this.clusteringCoefficient = clusteringCoefficient;
            this.averagePathLength = averagePathLength;
            this.modularity = modularity;
            this.isSmallWorld = isSmallWorld;
        }
    }

    public static class DailyMetrics {
        public final int sessionCount;
        public final int interactionCount;
        public final long averageLatency;
        public final int violationCount;

        public DailyMetrics(int sessionCount, int interactionCount,
                            long averageLatency, int violationCount) {
            this.sessionCount = sessionCount;
            this.interactionCount = interactionCount;
            this.averageLatency = averageLatency;
            this.violationCount = violationCount;
        }

        @Override
        public String toString() {
            return String.format("sessions=%d, interactions=%d, avgLatency=%dms, violations=%d",
                sessionCount, interactionCount, averageLatency, violationCount);
        }
    }

    public static class DailyResult {
        public final int day;
        public final DailyMetrics metrics;

        public DailyResult(int day, DailyMetrics metrics) {
            this.day = day;
            this.metrics = metrics;
        }
    }

    public static class SessionMetrics {
        public final int interactionCount;
        public final long totalLatency;
        public final int violations;

        public SessionMetrics(int interactionCount, long totalLatency, int violations) {
            this.interactionCount = interactionCount;
            this.totalLatency = totalLatency;
            this.violations = violations;
        }
    }

    public static class SimulationResult {
        public final int daysSimulated;
        public final List<DailyResult> dailyResults;
        public final EvolutionEngine.EvolutionSummary evolutionSummary;
        public final ThresholdMonitor.ViolationStatistics violationStats;

        public SimulationResult(int daysSimulated, List<DailyResult> dailyResults,
                                EvolutionEngine.EvolutionSummary evolutionSummary,
                                ThresholdMonitor.ViolationStatistics violationStats) {
            this.daysSimulated = daysSimulated;
            this.dailyResults = dailyResults;
            this.evolutionSummary = evolutionSummary;
            this.violationStats = violationStats;
        }
    }

    public static class SimulationStatistics {
        public final int populationSize;
        public final int totalSessions;
        public final long totalInteractions;
        public final Map<EvolutionEngine.EvolutionPhase, Integer> phaseDistribution;
        public final SimulationState state;

        public SimulationStatistics(int populationSize, int totalSessions,
                                    long totalInteractions,
                                    Map<EvolutionEngine.EvolutionPhase, Integer> phaseDistribution,
                                    SimulationState state) {
            this.populationSize = populationSize;
            this.totalSessions = totalSessions;
            this.totalInteractions = totalInteractions;
            this.phaseDistribution = phaseDistribution;
            this.state = state;
        }
    }

    // Component accessors
    public SharedContextModel getSharedContext() { return sharedContext; }
    public EvolutionEngine getEvolutionEngine() { return evolutionEngine; }
    public DensityProfiler getDensityProfiler() { return densityProfiler; }
    public SmallWorldNetwork getNetworkTopology() { return networkTopology; }
    public ThresholdMonitor getThresholdMonitor() { return thresholdMonitor; }
    public LongitudinalLogic getLongitudinalLogic() { return longitudinalLogic; }
    public Map<String, SyntheticUser> getSyntheticPopulation() { return new HashMap<>(syntheticPopulation); }
    public SimulationState getCurrentState() { return currentState; }
}
