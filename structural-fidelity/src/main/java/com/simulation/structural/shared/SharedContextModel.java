package com.simulation.structural.shared;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SharedContextModel - Heterogeneous Interdependence Layer
 *
 * Models realistic network/social overlaps where clusters of simulated devices
 * share contextual similarities (e.g., household, office profiles) without
 * creating identical telemetry signatures.
 *
 * Core Concept:
 * Devices in shared contexts exhibit correlated but non-identical behavior patterns
 * through "Contextual Entanglement" - shared environmental factors with individual
 * expression variance.
 *
 * Mathematical Foundation:
 * - Context Vector: C = [network_profile, temporal_patterns, geographic_cluster, social_tier]
 * - Entanglement Coefficient: ε ∈ [0,1] measuring shared context strength
 * - Signature Divergence: D = f(individual_entropy, ε) ensuring non-identical telemetry
 *
 * Implementation Strategy:
 * 1. Context Clustering: Group devices by shared environmental factors
 * 2. Entanglement Mapping: Define correlation strengths between cluster members
 * 3. Variance Injection: Apply controlled stochastic divergence per device
 * 4. Temporal Synchronization: Align shared context events with individual jitter
 */
public class SharedContextModel {

    private static final String LOG_TAG = "StructuralFidelity.SharedContext";

    private final Map<String, ContextCluster> clusters;
    private final Map<String, DeviceContextProfile> deviceProfiles;
    private final Random entropySource;

    // Context dimensions for SM-A125U simulation
    private static final double BASE_ENTANGLEMENT_COEFFICIENT = 0.65;
    private static final double MAX_SIGNATURE_DIVERGENCE = 0.35;
    private static final double TEMPORAL_SYNC_VARIANCE_MS = 2500;

    public SharedContextModel() {
        this.clusters = new ConcurrentHashMap<>();
        this.deviceProfiles = new ConcurrentHashMap<>();
        this.entropySource = new Random();
    }

    /**
     * Context Cluster Definition
     * Represents a group of devices sharing environmental context
     */
    public static class ContextCluster {
        private final String clusterId;
        private final ClusterType type;
        private final Set<String> deviceIds;
        private final NetworkProfile networkProfile;
        private final TemporalArchetype temporalArchetype;
        private final double entanglementCoefficient;

        public ContextCluster(String clusterId, ClusterType type,
                             NetworkProfile networkProfile,
                             TemporalArchetype temporalArchetype) {
            this.clusterId = clusterId;
            this.type = type;
            this.deviceIds = ConcurrentHashMap.newKeySet();
            this.networkProfile = networkProfile;
            this.temporalArchetype = temporalArchetype;
            this.entanglementCoefficient = calculateEntanglementCoefficient(type);
        }

        private double calculateEntanglementCoefficient(ClusterType type) {
            return switch (type) {
                case HOUSEHOLD -> 0.85;
                case OFFICE -> 0.75;
                case PUBLIC_WIFI -> 0.45;
                case MOBILE_CARRIER -> 0.60;
                case GEOGRAPHIC_REGION -> 0.55;
            };
        }

        public void addDevice(String deviceId) {
            deviceIds.add(deviceId);
        }

        public String getClusterId() { return clusterId; }
        public ClusterType getType() { return type; }
        public Set<String> getDeviceIds() { return new HashSet<>(deviceIds); }
        public NetworkProfile getNetworkProfile() { return networkProfile; }
        public TemporalArchetype getTemporalArchetype() { return temporalArchetype; }
        public double getEntanglementCoefficient() { return entanglementCoefficient; }
    }

    /**
     * Individual device context profile
     * Maintains entanglement with cluster while preserving unique signature
     */
    public static class DeviceContextProfile {
        private final String deviceId;
        private final String clusterId;
        private final double individualEntropy;
        private final Map<ContextDimension, Double> dimensionOffsets;
        private long lastSyncTimestamp;

        public DeviceContextProfile(String deviceId, String clusterId) {
            this.deviceId = deviceId;
            this.clusterId = clusterId;
            this.individualEntropy = 0.15 + (Math.random() * 0.20);
            this.dimensionOffsets = new EnumMap<>(ContextDimension.class);
            this.lastSyncTimestamp = System.currentTimeMillis();
            initializeDimensionOffsets();
        }

        private void initializeDimensionOffsets() {
            Random r = new Random(deviceId.hashCode());
            for (ContextDimension dim : ContextDimension.values()) {
                dimensionOffsets.put(dim, (r.nextDouble() - 0.5) * 2 * MAX_SIGNATURE_DIVERGENCE);
            }
        }

        public double applyEntanglement(ContextDimension dimension, double clusterValue) {
            double offset = dimensionOffsets.getOrDefault(dimension, 0.0);
            return clusterValue + (offset * individualEntropy);
        }

        public void recordSync() {
            lastSyncTimestamp = System.currentTimeMillis();
        }

        public String getDeviceId() { return deviceId; }
        public String getClusterId() { return clusterId; }
        public double getIndividualEntropy() { return individualEntropy; }
        public long getLastSyncTimestamp() { return lastSyncTimestamp; }
    }

    /**
     * Network profile shared by cluster members
     */
    public static class NetworkProfile {
        private final String ispIdentifier;
        private final double baseLatencyMs;
        private final double jitterMs;
        private final double packetLossRate;
        private final int typicalBandwidthKbps;
        private final String geographicRegion;

        public NetworkProfile(String ispIdentifier, double baseLatencyMs,
                             double jitterMs, double packetLossRate,
                             int typicalBandwidthKbps, String geographicRegion) {
            this.ispIdentifier = ispIdentifier;
            this.baseLatencyMs = baseLatencyMs;
            this.jitterMs = jitterMs;
            this.packetLossRate = packetLossRate;
            this.typicalBandwidthKbps = typicalBandwidthKbps;
            this.geographicRegion = geographicRegion;
        }

        public NetworkProfile withVariation(double varianceFactor) {
            return new NetworkProfile(
                ispIdentifier,
                baseLatencyMs * (1 + (Math.random() - 0.5) * varianceFactor),
                jitterMs * (1 + (Math.random() - 0.5) * varianceFactor * 0.5),
                packetLossRate * (1 + (Math.random() - 0.5) * varianceFactor),
                typicalBandwidthKbps,
                geographicRegion
            );
        }

        public String getIspIdentifier() { return ispIdentifier; }
        public double getBaseLatencyMs() { return baseLatencyMs; }
        public double getJitterMs() { return jitterMs; }
        public double getPacketLossRate() { return packetLossRate; }
        public int getTypicalBandwidthKbps() { return typicalBandwidthKbps; }
        public String getGeographicRegion() { return geographicRegion; }
    }

    /**
     * Temporal behavior archetype for cluster
     */
    public static class TemporalArchetype {
        private final double peakActivityHour;        // 0-24 hour of peak activity
        private final double activitySpreadHours;     // Standard deviation of activity
        private final double weekdayWeekendRatio;     // Activity ratio
        private final double sessionFrequencyLambda;  // Poisson parameter for sessions/day

        public TemporalArchetype(double peakActivityHour, double activitySpreadHours,
                                  double weekdayWeekendRatio, double sessionFrequencyLambda) {
            this.peakActivityHour = peakActivityHour;
            this.activitySpreadHours = activitySpreadHours;
            this.weekdayWeekendRatio = weekdayWeekendRatio;
            this.sessionFrequencyLambda = sessionFrequencyLambda;
        }

        public double calculateActivityProbability(int hourOfDay, boolean isWeekend) {
            double timeDiff = Math.abs(hourOfDay - peakActivityHour);
            if (timeDiff > 12) timeDiff = 24 - timeDiff;

            double baseProbability = Math.exp(-0.5 * Math.pow(timeDiff / activitySpreadHours, 2));
            double weekendModifier = isWeekend ? 1.0 / weekdayWeekendRatio : 1.0;

            return Math.min(1.0, baseProbability * weekendModifier);
        }

        public double getPeakActivityHour() { return peakActivityHour; }
        public double getActivitySpreadHours() { return activitySpreadHours; }
        public double getWeekdayWeekendRatio() { return weekdayWeekendRatio; }
        public double getSessionFrequencyLambda() { return sessionFrequencyLambda; }
    }

    public enum ClusterType {
        HOUSEHOLD,      // High entanglement, shared WiFi, temporal sync
        OFFICE,         // Business hours synchronization
        PUBLIC_WIFI,    // Lower entanglement, transient
        MOBILE_CARRIER, // Carrier-level shared characteristics
        GEOGRAPHIC_REGION // Regional behavioral patterns
    }

    public enum ContextDimension {
        NETWORK_LATENCY,
        PACKET_LOSS,
        TEMPORAL_OFFSET,
        SESSION_FREQUENCY,
        ERROR_RATE_BIAS,
        FEATURE_PREFERENCE
    }

    /**
     * Create a new context cluster
     */
    public ContextCluster createCluster(String clusterId, ClusterType type,
                                         NetworkProfile networkProfile,
                                         TemporalArchetype temporalArchetype) {
        ContextCluster cluster = new ContextCluster(clusterId, type, networkProfile, temporalArchetype);
        clusters.put(clusterId, cluster);
        return cluster;
    }

    /**
     * Register a device to a cluster with unique context profile
     */
    public DeviceContextProfile registerDevice(String deviceId, String clusterId) {
        ContextCluster cluster = clusters.get(clusterId);
        if (cluster == null) {
            throw new IllegalArgumentException("Cluster not found: " + clusterId);
        }

        cluster.addDevice(deviceId);
        DeviceContextProfile profile = new DeviceContextProfile(deviceId, clusterId);
        deviceProfiles.put(deviceId, profile);
        return profile;
    }

    /**
     * Get context-aware network parameters for device
     * Applies entanglement while preserving individual variance
     */
    public NetworkProfile getContextualNetworkProfile(String deviceId) {
        DeviceContextProfile profile = deviceProfiles.get(deviceId);
        if (profile == null) {
            return null;
        }

        ContextCluster cluster = clusters.get(profile.getClusterId());
        NetworkProfile baseProfile = cluster.getNetworkProfile();

        double entanglement = cluster.getEntanglementCoefficient();
        double divergence = profile.getIndividualEntropy() * (1 - entanglement);

        return baseProfile.withVariation(divergence);
    }

    /**
     * Get temporally synchronized but individually varied timestamp
     * Ensures cluster members experience events at correlated but not identical times
     */
    public long getSynchronizedTimestamp(String deviceId, long baseEventTime) {
        DeviceContextProfile profile = deviceProfiles.get(deviceId);
        if (profile == null) {
            return baseEventTime;
        }

        ContextCluster cluster = clusters.get(profile.getClusterId());
        double entanglement = cluster.getEntanglementCoefficient();

        double syncVariance = TEMPORAL_SYNC_VARIANCE_MS * (1 - entanglement);
        double offset = (entropySource.nextGaussian() * syncVariance) +
                       (profile.getIndividualEntropy() * TEMPORAL_SYNC_VARIANCE_MS);

        return baseEventTime + Math.round(offset);
    }

    /**
     * Calculate contextual correlation between two devices
     * Returns 0-1 correlation coefficient
     */
    public double calculateDeviceCorrelation(String deviceId1, String deviceId2) {
        DeviceContextProfile profile1 = deviceProfiles.get(deviceId1);
        DeviceContextProfile profile2 = deviceProfiles.get(deviceId2);

        if (profile1 == null || profile2 == null) {
            return 0.0;
        }

        if (!profile1.getClusterId().equals(profile2.getClusterId())) {
            return 0.05;
        }

        ContextCluster cluster = clusters.get(profile1.getClusterId());
        double baseEntanglement = cluster.getEntanglementCoefficient();

        double entropyDiff = Math.abs(profile1.getIndividualEntropy() - profile2.getIndividualEntropy());
        return baseEntanglement * (1 - entropyDiff);
    }

    /**
     * Get devices with highest contextual correlation for coordinated events
     */
    public List<String> getCorrelatedDevices(String sourceDeviceId, double minCorrelation) {
        DeviceContextProfile sourceProfile = deviceProfiles.get(sourceDeviceId);
        if (sourceProfile == null) {
            return Collections.emptyList();
        }

        List<String> correlated = new ArrayList<>();
        ContextCluster cluster = clusters.get(sourceProfile.getClusterId());

        for (String deviceId : cluster.getDeviceIds()) {
            if (deviceId.equals(sourceDeviceId)) continue;

            double correlation = calculateDeviceCorrelation(sourceDeviceId, deviceId);
            if (correlation >= minCorrelation) {
                correlated.add(deviceId);
            }
        }

        return correlated;
    }

    /**
     * Simulate cluster-wide event propagation with individual variance
     */
    public Map<String, Long> propagateClusterEvent(String clusterId, long eventTime,
                                                    EventType eventType) {
        ContextCluster cluster = clusters.get(clusterId);
        if (cluster == null) {
            return Collections.emptyMap();
        }

        Map<String, Long> propagationMap = new HashMap<>();

        for (String deviceId : cluster.getDeviceIds()) {
            DeviceContextProfile profile = deviceProfiles.get(deviceId);

            long deviceEventTime = getSynchronizedTimestamp(deviceId, eventTime);

            if (eventType == EventType.NETWORK_DISRUPTION) {
                double disruptionProbability = cluster.getEntanglementCoefficient();
                if (entropySource.nextDouble() > disruptionProbability) {
                    continue;
                }
            }

            propagationMap.put(deviceId, deviceEventTime);
            profile.recordSync();
        }

        return propagationMap;
    }

    public enum EventType {
        NETWORK_DISRUPTION,
        CONTENT_UPDATE,
        PEAK_ACTIVITY_PERIOD,
        MAINTENANCE_WINDOW
    }

    /**
     * Generate realistic shared context for household simulation
     */
    public static SharedContextModel createHouseholdCluster(int deviceCount, String householdId) {
        SharedContextModel model = new SharedContextModel();

        NetworkProfile homeNetwork = new NetworkProfile(
            "ISP_" + householdId,
            28.0,      // Base latency ~28ms
            4.5,       // Jitter ~4.5ms
            0.002,     // 0.2% packet loss
            50000,     // 50 Mbps
            "REGION_" + householdId.hashCode() % 100
        );

        TemporalArchetype householdPattern = new TemporalArchetype(
            20.5,      // Peak at 8:30 PM
            3.5,       // Spread over ~3.5 hours
            0.85,      // Weekend activity 85% of weekday
            4.2        // ~4 sessions/day average
        );

        ContextCluster cluster = model.createCluster(
            householdId, ClusterType.HOUSEHOLD, homeNetwork, householdPattern
        );

        for (int i = 0; i < deviceCount; i++) {
            String deviceId = householdId + "_DEVICE_" + i;
            model.registerDevice(deviceId, householdId);
        }

        return model;
    }

    /**
     * Generate office environment shared context
     */
    public static SharedContextModel createOfficeCluster(int deviceCount, String officeId) {
        SharedContextModel model = new SharedContextModel();

        NetworkProfile officeNetwork = new NetworkProfile(
            "CORP_" + officeId,
            15.0,      // Lower latency for enterprise
            2.0,
            0.001,
            100000,
            "CORP_REGION"
        );

        TemporalArchetype officePattern = new TemporalArchetype(
            14.0,      // Peak at 2:00 PM
            4.0,
            0.25,      // Much lower weekend activity
            8.5        // Higher session frequency during work
        );

        ContextCluster cluster = model.createCluster(
            officeId, ClusterType.OFFICE, officeNetwork, officePattern
        );

        for (int i = 0; i < deviceCount; i++) {
            String deviceId = officeId + "_WORKSTATION_" + i;
            model.registerDevice(deviceId, officeId);
        }

        return model;
    }

    public Map<String, ContextCluster> getClusters() {
        return new HashMap<>(clusters);
    }

    public Map<String, DeviceContextProfile> getDeviceProfiles() {
        return new HashMap<>(deviceProfiles);
    }
}
