package com.samsungcloak.hardware;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Stochastic Network Hook for Samsung Galaxy A12 (SM-A125U)
 * 
 * Simulates realistic network jitter and packet loss typical of mobile devices
 * moving between cells or transitioning between indoor/outdoor environments.
 * 
 * Network Specs: 4G LTE Cat. 4, WiFi 802.11 a/b/g/n/ac
 */
public class NetworkJitterHook {
    
    // Network state constants
    public enum NetworkType {
        WIFI_5GHZ("WiFi 5GHz", 120.0, 0.001, 2.0, -30),
        WIFI_2_4GHZ("WiFi 2.4GHz", 70.0, 0.002, 5.0, -40),
        LTE_EXCELLENT("4G LTE Excellent", 50.0, 0.005, 10.0, -60),
        LTE_GOOD("4G LTE Good", 20.0, 0.01, 25.0, -75),
        LTE_FAIR("4G LTE Fair", 5.0, 0.03, 50.0, -90),
        EDGE("2G Edge", 0.2, 0.1, 200.0, -100);
        
        private final String name;
        private final double typicalLatencyMs;
        private final double basePacketLossRate;
        private final double typicalJitterMs;
        private final double minRSSIdBm;
        
        NetworkType(String name, double typicalLatencyMs, double basePacketLossRate,
                    double typicalJitterMs, double minRSSIdBm) {
            this.name = name;
            this.typicalLatencyMs = typicalLatencyMs;
            this.basePacketLossRate = basePacketLossRate;
            this.typicalJitterMs = typicalJitterMs;
            this.minRSSIdBm = minRSSIdBm;
        }
    }
    
    // RSSI signal strength ranges (dBm)
    private static final double RSSI_EXCELLENT = -50.0;
    private static final double RSSI_GOOD = -70.0;
    private static final double RSSI_FAIR = -85.0;
    private static final double RSSI_POOR = -95.0;
    
    // Movement and environmental factors
    private static final double WALKING_SPEED_MS = 1.4;
    private static final double DRIVING_SPEED_MS = 13.9;
    private static final double CELL_TRANSITION_INTERVAL_MS = 30000; // 30 seconds
    private static final double INDOOR_OUTDOOR_TRANSITION_MS = 60000; // 1 minute
    
    private final Random random;
    private NetworkType currentNetworkType;
    private double currentRSSIdBm;
    private boolean isIndoors;
    private boolean isMoving;
    private double movementSpeedMs;
    private long lastTransitionTimeMs;
    private long lastUpdateTimeMs;
    
    // Network quality tracking
    private final List<Double> recentLatencies;
    private final int LATENCY_HISTORY_SIZE = 20;
    private int recentPacketLosses;
    private int recentPacketsSent;
    
    public NetworkJitterHook(NetworkType initialNetworkType) {
        this.random = new Random();
        this.currentNetworkType = initialNetworkType;
        this.currentRSSIdBm = RSSI_EXCELLENT + random.nextDouble() * 20.0;
        this.isIndoors = true;
        this.isMoving = false;
        this.movementSpeedMs = 0.0;
        this.lastTransitionTimeMs = System.currentTimeMillis();
        this.lastUpdateTimeMs = System.currentTimeMillis();
        this.recentLatencies = new ArrayList<>();
        this.recentPacketLosses = 0;
        this.recentPacketsSent = 0;
    }
    
    /**
     * Update network state based on environmental conditions.
     * 
     * @param isMoving true if device is in motion
     * @param isIndoors true if device is indoors
     * @param movementSpeedMs speed in m/s (0 if stationary)
     * @return Updated network state
     */
    public NetworkState updateNetworkState(boolean isMoving, boolean isIndoors, 
                                          double movementSpeedMs) {
        long currentTimeMs = System.currentTimeMillis();
        long timeDeltaMs = currentTimeMs - lastUpdateTimeMs;
        
        this.isMoving = isMoving;
        this.isIndoors = isIndoors;
        this.movementSpeedMs = movementSpeedMs;
        
        // Simulate RSSI fluctuations
        updateRSSI(timeDeltaMs);
        
        // Check for network type transitions
        checkNetworkTransition(currentTimeMs);
        
        lastUpdateTimeMs = currentTimeMs;
        
        return getNetworkState();
    }
    
    /**
     * Update RSSI based on movement and environment.
     */
    private void updateRSSI(long timeDeltaMs) {
        // Base RSSI fluctuation (random noise)
        double rssiNoise = (random.nextDouble() - 0.5) * 5.0;
        
        // Movement-induced RSSI variation
        double movementVariation = 0.0;
        if (isMoving && movementSpeedMs > 0.0) {
            double movementIntensity = movementSpeedMs / DRIVING_SPEED_MS;
            movementVariation = (random.nextDouble() - 0.5) * 20.0 * movementIntensity;
        }
        
        // Indoor/outdoor transition effects
        double environmentVariation = 0.0;
        if (!isIndoors) {
            // Better signal outdoors, but more fluctuation
            environmentVariation = -10.0 + random.nextDouble() * 15.0;
        } else {
            // Worse signal indoors, more stable
            environmentVariation = 5.0 + (random.nextDouble() - 0.5) * 5.0;
        }
        
        // Multi-path fading (rapid fluctuations)
        double multiPathFading = (random.nextDouble() - 0.5) * 8.0;
        
        // Apply all variations
        currentRSSIdBm += rssiNoise + movementVariation + environmentVariation + multiPathFading;
        
        // Clamp RSSI to realistic range
        double minRSSI = currentNetworkType.minRSSIdBm - 10.0;
        double maxRSSI = -30.0;
        currentRSSIdBm = Math.max(minRSSI, Math.min(maxRSSI, currentRSSIdBm));
        
        // Gradual decay to typical signal strength for current network type
        double targetRSSI = calculateTargetRSSI();
        currentRSSIdBm = currentRSSIdBm * 0.95 + targetRSSI * 0.05;
    }
    
    /**
     * Calculate target RSSI based on network type and environment.
     */
    private double calculateTargetRSSI() {
        double baseRSSI;
        
        if (currentNetworkType == NetworkType.WIFI_5GHZ) {
            baseRSSI = isIndoors ? -45.0 : -60.0;
        } else if (currentNetworkType == NetworkType.WIFI_2_4GHZ) {
            baseRSSI = isIndoors ? -50.0 : -65.0;
        } else {
            // Cellular networks
            if (isIndoors) {
                baseRSSI = -80.0; // Weaker indoors
            } else {
                baseRSSI = -60.0; // Better outdoors
            }
        }
        
        // Add movement penalty
        if (isMoving) {
            baseRSSI += 10.0;
        }
        
        return baseRSSI + random.nextDouble() * 10.0;
    }
    
    /**
     * Check for network type transitions (handover scenarios).
     */
    private void checkNetworkTransition(long currentTimeMs) {
        // WiFi to LTE transition (leaving WiFi range)
        if (currentNetworkType.name().startsWith("WIFI")) {
            double wifiDropProbability = calculateWifiDropProbability();
            if (random.nextDouble() < wifiDropProbability) {
                transitionToCellular(currentTimeMs);
            }
        }
        
        // LTE to WiFi transition (coming into WiFi range)
        if (currentNetworkType.name().startsWith("LTE") || currentNetworkType == NetworkType.EDGE) {
            double wifiConnectProbability = calculateWifiConnectProbability();
            if (random.nextDouble() < wifiConnectProbability) {
                currentNetworkType = isIndoors ? NetworkType.WIFI_2_4GHZ : NetworkType.WIFI_5GHZ;
                lastTransitionTimeMs = currentTimeMs;
            }
        }
        
        // LTE signal degradation
        if (currentNetworkType.name().startsWith("LTE")) {
            double degradationProbability = calculateLTEDegradationProbability();
            if (random.nextDouble() < degradationProbability) {
                degradeLTEConnection();
            }
        }
    }
    
    private double calculateWifiDropProbability() {
        if (isMoving && movementSpeedMs > WALKING_SPEED_MS) {
            return 0.02; // Higher drop probability when moving
        }
        if (currentRSSIdBm < -75.0) {
            return 0.05; // Drop probability increases with weak signal
        }
        return 0.005; // Base drop probability
    }
    
    private double calculateWifiConnectProbability() {
        if (isIndoors) {
            return 0.03; // Higher probability indoors
        }
        return 0.005; // Lower probability outdoors
    }
    
    private double calculateLTEDegradationProbability() {
        if (isMoving && movementSpeedMs > DRIVING_SPEED_MS) {
            return 0.01; // More degradation when driving
        }
        if (currentRSSIdBm < -90.0) {
            return 0.02; // More degradation with weak signal
        }
        return 0.002; // Base degradation probability
    }
    
    private void transitionToCellular(long currentTimeMs) {
        // Select LTE quality based on RSSI
        if (currentRSSIdBm > -70.0) {
            currentNetworkType = NetworkType.LTE_EXCELLENT;
        } else if (currentRSSIdBm > -85.0) {
            currentNetworkType = NetworkType.LTE_GOOD;
        } else if (currentRSSIdBm > -95.0) {
            currentNetworkType = NetworkType.LTE_FAIR;
        } else {
            currentNetworkType = NetworkType.EDGE;
        }
        lastTransitionTimeMs = currentTimeMs;
    }
    
    private void degradeLTEConnection() {
        switch (currentNetworkType) {
            case LTE_EXCELLENT:
                currentNetworkType = NetworkType.LTE_GOOD;
                break;
            case LTE_GOOD:
                currentNetworkType = NetworkType.LTE_FAIR;
                break;
            case LTE_FAIR:
                currentNetworkType = NetworkType.EDGE;
                break;
            default:
                // Already at EDGE, stay there
                break;
        }
    }
    
    /**
     * Simulate network request with realistic latency and packet loss.
     * 
     * @param requestSizeBytes size of the request
     * @return NetworkResult with latency and success status
     */
    public NetworkResult simulateNetworkRequest(int requestSizeBytes) {
        recentPacketsSent++;
        
        // Calculate base latency
        double baseLatencyMs = currentNetworkType.typicalLatencyMs;
        
        // Add RSSI-based latency penalty
        double rssiLatencyPenalty = calculateRSSILatencyPenalty();
        
        // Add movement-induced latency
        double movementLatencyPenalty = calculateMovementLatencyPenalty();
        
        // Add jitter (random variation)
        double jitter = calculateJitter();
        
        // Calculate total latency
        double totalLatencyMs = baseLatencyMs + rssiLatencyPenalty + 
                               movementLatencyPenalty + jitter;
        
        // Determine packet loss
        boolean packetLost = checkPacketLoss();
        
        if (packetLost) {
            recentPacketLosses++;
        }
        
        // Update latency history
        recentLatencies.add(totalLatencyMs);
        if (recentLatencies.size() > LATENCY_HISTORY_SIZE) {
            recentLatencies.remove(0);
        }
        
        return new NetworkResult(totalLatencyMs, !packetLost, currentRSSIdBm);
    }
    
    private double calculateRSSILatencyPenalty() {
        if (currentRSSIdBm > RSSI_EXCELLENT) {
            return 0.0;
        } else if (currentRSSIdBm > RSSI_GOOD) {
            return (RSSI_GOOD - currentRSSIdBm) * 0.5;
        } else if (currentRSSIdBm > RSSI_FAIR) {
            return (RSSI_GOOD - currentRSSIdBm) * 1.0 + 10.0;
        } else {
            return (RSSI_FAIR - currentRSSIdBm) * 2.0 + 30.0;
        }
    }
    
    private double calculateMovementLatencyPenalty() {
        if (!isMoving || movementSpeedMs == 0.0) {
            return 0.0;
        }
        
        double speedFactor = movementSpeedMs / DRIVING_SPEED_MS;
        return speedFactor * 50.0;
    }
    
    private double calculateJitter() {
        double baseJitter = currentNetworkType.typicalJitterMs;
        
        // Increased jitter during movement
        if (isMoving) {
            baseJitter *= 1.5;
        }
        
        // Increased jitter with weak signal
        if (currentRSSIdBm < -80.0) {
            baseJitter *= 2.0;
        }
        
        // Apply random variation (Gaussian-like distribution)
        return (random.nextDouble() - 0.5) * 2.0 * baseJitter;
    }
    
    private boolean checkPacketLoss() {
        double baseLossRate = currentNetworkType.basePacketLossRate;
        
        // Increased loss with weak signal
        if (currentRSSIdBm < -85.0) {
            baseLossRate *= 3.0;
        }
        
        // Increased loss during movement
        if (isMoving) {
            baseLossRate *= 1.5;
        }
        
        // Burst loss simulation (consecutive packets)
        if (recentPacketLosses > 0 && recentPacketLosses < 5) {
            baseLossRate *= 2.0;
        }
        
        return random.nextDouble() < baseLossRate;
    }
    
    /**
     * Get current network state for telemetry.
     */
    public NetworkState getNetworkState() {
        double averageLatencyMs = calculateAverageLatency();
        double packetLossRate = calculatePacketLossRate();
        
        return new NetworkState(
            currentNetworkType,
            currentRSSIdBm,
            averageLatencyMs,
            packetLossRate,
            isIndoors,
            isMoving
        );
    }
    
    private double calculateAverageLatency() {
        if (recentLatencies.isEmpty()) {
            return currentNetworkType.typicalLatencyMs;
        }
        
        double sum = 0.0;
        for (Double latency : recentLatencies) {
            sum += latency;
        }
        return sum / recentLatencies.size();
    }
    
    private double calculatePacketLossRate() {
        if (recentPacketsSent == 0) {
            return 0.0;
        }
        return (double) recentPacketLosses / recentPacketsSent;
    }
    
    /**
     * Force network type change (for testing or specific scenarios).
     */
    public void forceNetworkType(NetworkType newType) {
        this.currentNetworkType = newType;
        this.currentRSSIdBm = calculateTargetRSSI();
        this.lastTransitionTimeMs = System.currentTimeMillis();
        this.recentLatencies.clear();
        this.recentPacketLosses = 0;
        this.recentPacketsSent = 0;
    }
    
    /**
     * Data class for network request results.
     */
    public static class NetworkResult {
        public final double latencyMs;
        public final boolean success;
        public final double rssidBm;
        
        public NetworkResult(double latencyMs, boolean success, double rssidBm) {
            this.latencyMs = latencyMs;
            this.success = success;
            this.rssidBm = rssidBm;
        }
    }
    
    /**
     * Data class for network state telemetry.
     */
    public static class NetworkState {
        public final NetworkType networkType;
        public final double rssidBm;
        public final double averageLatencyMs;
        public final double packetLossRate;
        public final boolean isIndoors;
        public final boolean isMoving;
        
        public NetworkState(NetworkType networkType, double rssidBm, 
                           double averageLatencyMs, double packetLossRate,
                           boolean isIndoors, boolean isMoving) {
            this.networkType = networkType;
            this.rssidBm = rssidBm;
            this.averageLatencyMs = averageLatencyMs;
            this.packetLossRate = packetLossRate;
            this.isIndoors = isIndoors;
            this.isMoving = isMoving;
        }
    }
}
