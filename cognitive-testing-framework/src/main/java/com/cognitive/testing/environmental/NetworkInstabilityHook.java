package com.cognitive.testing.environmental;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Network Instability Simulation Hook
 * Simulates volatile network conditions on Samsung Galaxy A12 by dynamically toggling
 * between 4G, 3G, and "No Connection" states with latency injection.
 * 
 * Real-world usage patterns this simulates:
 * - Moving between cell towers with varying signal strength
 * - Entering buildings/tunnels with poor reception
 * - Network congestion during peak hours
 * - Carrier network maintenance or outages
 */
public class NetworkInstabilityHook {
    
    private final EnvironmentalConfig config;
    private final Random random;
    
    private volatile NetworkState currentState;
    private volatile int currentLatencyMs;
    private volatile boolean isNetworkDown;
    private volatile long lastNetworkChangeTime;
    
    private final AtomicInteger networkChangeCount;
    private final AtomicInteger networkFailureCount;
    private final AtomicInteger totalLatencyInjectedMs;
    
    private final AtomicReference<NetworkTransition> lastTransition;
    
    public NetworkInstabilityHook(EnvironmentalConfig config) {
        this(config, new Random());
    }
    
    public NetworkInstabilityHook(EnvironmentalConfig config, Random random) {
        this.config = config;
        this.random = random;
        this.currentState = NetworkState.FOUR_G;
        this.currentLatencyMs = config.getMinNetworkLatencyMs();
        this.isNetworkDown = false;
        this.lastNetworkChangeTime = System.currentTimeMillis();
        this.networkChangeCount = new AtomicInteger(0);
        this.networkFailureCount = new AtomicInteger(0);
        this.lastTransition = new AtomicReference<>(new NetworkTransition(NetworkState.FOUR_G, 0));
    }
    
    /**
     * Check if network state should change based on probability and time
     */
    public boolean shouldChangeNetworkState() {
        if (!config.isEnableNetworkInstability()) {
            return false;
        }
        
        long timeSinceLastChange = System.currentTimeMillis() - lastNetworkChangeTime;
        
        if (timeSinceLastChange < 3000) {
            return false;
        }
        
        return random.nextFloat() < config.getNetworkChangeProbability();
    }
    
    /**
     * Perform network state transition
     */
    public void changeNetworkState() {
        NetworkState oldState = currentState;
        NetworkState newState;
        
        if (isNetworkDown) {
            if (random.nextFloat() < 0.7f) {
                newState = determineNextState();
                isNetworkDown = false;
            } else {
                newState = NetworkState.NO_CONNECTION;
            }
        } else {
            if (random.nextFloat() < config.getNetworkFailureProbability()) {
                newState = NetworkState.NO_CONNECTION;
                isNetworkDown = true;
                networkFailureCount.incrementAndGet();
            } else {
                newState = determineNextState();
            }
        }
        
        currentState = newState;
        lastNetworkChangeTime = System.currentTimeMillis();
        networkChangeCount.incrementAndGet();
        
        int transitionLatency = calculateTransitionLatency();
        currentLatencyMs = transitionLatency;
        totalLatencyInjectedMs.addAndGet(transitionLatency);
        
        lastTransition.set(new NetworkTransition(newState, transitionLatency));
        
        simulateNetworkInterruption();
    }
    
    private NetworkState determineNextState() {
        float roll = random.nextFloat();
        
        if (currentState == NetworkState.FOUR_G) {
            if (roll < 0.6f) {
                return NetworkState.FOUR_G;
            } else if (roll < 0.85f) {
                return NetworkState.THREE_G;
            } else {
                return NetworkState.TWO_G;
            }
        } else if (currentState == NetworkState.THREE_G) {
            if (roll < 0.4f) {
                return NetworkState.FOUR_G;
            } else if (roll < 0.7f) {
                return NetworkState.THREE_G;
            } else {
                return NetworkState.TWO_G;
            }
        } else {
            if (roll < 0.5f) {
                return NetworkState.FOUR_G;
            } else if (roll < 0.8f) {
                return NetworkState.THREE_G;
            } else {
                return NetworkState.TWO_G;
            }
        }
    }
    
    private int calculateTransitionLatency() {
        if (isNetworkDown) {
            return Integer.MAX_VALUE;
        }
        
        int baseLatency;
        switch (currentState) {
            case FOUR_G:
                baseLatency = 50 + random.nextInt(150);
                break;
            case THREE_G:
                baseLatency = 200 + random.nextInt(300);
                break;
            case TWO_G:
                baseLatency = 400 + random.nextInt(600);
                break;
            default:
                baseLatency = config.getMinNetworkLatencyMs();
        }
        
        int configuredMin = config.getMinNetworkLatencyMs();
        int configuredMax = config.getMaxNetworkLatencyMs();
        
        baseLatency = Math.max(configuredMin, baseLatency);
        baseLatency = Math.min(configuredMax, baseLatency);
        
        if (random.nextFloat() < 0.15f) {
            baseLatency += random.nextInt(2000);
        }
        
        return baseLatency;
    }
    
    private void simulateNetworkInterruption() {
        if (!isNetworkDown) {
            try {
                int jitterDelay = 50 + random.nextInt(150);
                Thread.sleep(jitterDelay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Inject latency before network operation
     */
    public void injectLatency() {
        if (isNetworkDown || !config.isEnableNetworkInstability()) {
            return;
        }
        
        int latency = getEffectiveLatency();
        if (latency > 0 && latency < Integer.MAX_VALUE - 1000) {
            try {
                Thread.sleep(latency);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Get effective latency considering network state
     */
    public int getEffectiveLatency() {
        if (isNetworkDown) {
            return Integer.MAX_VALUE;
        }
        return currentLatencyMs;
    }
    
    /**
     * Check if network operation should fail
     */
    public boolean shouldNetworkOperationFail() {
        if (!config.isEnableNetworkInstability()) {
            return false;
        }
        
        if (isNetworkDown) {
            return random.nextFloat() < 0.9f;
        }
        
        if (currentState == NetworkState.TWO_G) {
            return random.nextFloat() < 0.15f;
        } else if (currentState == NetworkState.THREE_G) {
            return random.nextFloat() < 0.05f;
        }
        
        return random.nextFloat() < 0.02f;
    }
    
    /**
     * Simulate network recovery attempt
     */
    public boolean attemptNetworkRecovery() {
        if (!isNetworkDown) {
            return true;
        }
        
        long timeSinceFailure = System.currentTimeMillis() - lastNetworkChangeTime;
        long recoveryTime = config.getNetworkRecoveryTimeMs();
        
        if (timeSinceFailure < recoveryTime) {
            return false;
        }
        
        float recoveryChance = Math.min(0.95f, (timeSinceFailure - recoveryTime) / 10000.0f);
        
        if (random.nextFloat() < recoveryChance) {
            isNetworkDown = false;
            currentState = NetworkState.FOUR_G;
            currentLatencyMs = config.getMinNetworkLatencyMs();
            lastNetworkChangeTime = System.currentTimeMillis();
            return true;
        }
        
        return false;
    }
    
    /**
     * Manually force network state change
     */
    public void forceNetworkState(NetworkState state) {
        this.currentState = state;
        this.isNetworkDown = (state == NetworkState.NO_CONNECTION);
        this.lastNetworkChangeTime = System.currentTimeMillis();
        this.currentLatencyMs = calculateTransitionLatency();
        this.lastTransition.set(new NetworkTransition(state, currentLatencyMs));
    }
    
    /**
     * Get current network state
     */
    public NetworkState getCurrentState() {
        return currentState;
    }
    
    /**
     * Check if network is currently down
     */
    public boolean isNetworkDown() {
        return isNetworkDown;
    }
    
    /**
     * Get statistics
     */
    public NetworkStatistics getStatistics() {
        return new NetworkStatistics(
            networkChangeCount.get(),
            networkFailureCount.get(),
            totalLatencyInjectedMs.get(),
            currentState,
            isNetworkDown,
            currentLatencyMs,
            lastTransition.get()
        );
    }
    
    /**
     * Reset statistics
     */
    public void resetStatistics() {
        networkChangeCount.set(0);
        networkFailureCount.set(0);
        totalLatencyInjectedMs.set(0);
        currentState = NetworkState.FOUR_G;
        isNetworkDown = false;
        currentLatencyMs = config.getMinNetworkLatencyMs();
        lastNetworkChangeTime = System.currentTimeMillis();
    }
    
    public enum NetworkState {
        FOUR_G("4G", 50, 200),
        THREE_G("3G", 200, 500),
        TWO_G("2G", 400, 1000),
        NO_CONNECTION("No Connection", Integer.MAX_VALUE, Integer.MAX_VALUE);
        
        private final String displayName;
        private final int minLatencyMs;
        private final int maxLatencyMs;
        
        NetworkState(String displayName, int minLatencyMs, int maxLatencyMs) {
            this.displayName = displayName;
            this.minLatencyMs = minLatencyMs;
            this.maxLatencyMs = maxLatencyMs;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public int getMinLatencyMs() {
            return minLatencyMs;
        }
        
        public int getMaxLatencyMs() {
            return maxLatencyMs;
        }
    }
    
    public static class NetworkTransition {
        private final NetworkState state;
        private final int latencyMs;
        private final long timestamp;
        
        public NetworkTransition(NetworkState state, int latencyMs) {
            this.state = state;
            this.latencyMs = latencyMs;
            this.timestamp = System.currentTimeMillis();
        }
        
        public NetworkState getState() {
            return state;
        }
        
        public int getLatencyMs() {
            return latencyMs;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
    }
    
    public static class NetworkStatistics {
        private final int totalNetworkChanges;
        private final int totalNetworkFailures;
        private final int totalLatencyInjectedMs;
        private final NetworkState currentState;
        private final boolean isNetworkDown;
        private final int currentLatencyMs;
        private final NetworkTransition lastTransition;
        
        public NetworkStatistics(int totalNetworkChanges, int totalNetworkFailures,
                                int totalLatencyInjectedMs, NetworkState currentState,
                                boolean isNetworkDown, int currentLatencyMs,
                                NetworkTransition lastTransition) {
            this.totalNetworkChanges = totalNetworkChanges;
            this.totalNetworkFailures = totalNetworkFailures;
            this.totalLatencyInjectedMs = totalLatencyInjectedMs;
            this.currentState = currentState;
            this.isNetworkDown = isNetworkDown;
            this.currentLatencyMs = currentLatencyMs;
            this.lastTransition = lastTransition;
        }
        
        public int getTotalNetworkChanges() {
            return totalNetworkChanges;
        }
        
        public int getTotalNetworkFailures() {
            return totalNetworkFailures;
        }
        
        public int getTotalLatencyInjectedMs() {
            return totalLatencyInjectedMs;
        }
        
        public NetworkState getCurrentState() {
            return currentState;
        }
        
        public boolean isNetworkDown() {
            return isNetworkDown;
        }
        
        public int getCurrentLatencyMs() {
            return currentLatencyMs;
        }
        
        public NetworkTransition getLastTransition() {
            return lastTransition;
        }
        
        @Override
        public String toString() {
            return String.format(
                "NetworkStatistics{changes=%d, failures=%d, latency=%dms, state=%s, down=%b}",
                totalNetworkChanges, totalNetworkFailures, currentLatencyMs,
                currentState.getDisplayName(), isNetworkDown
            );
        }
    }
}
