package com.simulation.distributional;

import java.util.Random;

/**
 * Long-Tail Behavior Modeling Hook - Outlier Session Generation
 * 
 * Generates rare, high-intensity "Outlier Sessions" to test how the app
 * handles extreme resource consumption at the edge of the bell curve.
 * 
 * This combines multiple statistical techniques to model realistic extreme
 * behaviors that standard distributions fail to capture.
 * 
 * Key Concepts:
 * - Heavy-tailed distributions for rare events
 * - Mixture models for multi-modal behavior
 * - Extreme value theory for outlier detection
 * - Resource-intensive session simulation
 */
public class OutlierSessionGenerator {
    
    private final Random random;
    private final NormalDistribution normalDistribution;
    private final ParetoDistribution paretoDistribution;
    
    private final double outlierProbability; // Probability of an outlier session
    private final double outlierIntensityMultiplier; // How much more intense outliers are
    
    /**
     * Initialize the outlier session generator.
     * 
     * @param outlierProbability Probability of a session being an outlier (0.0 to 1.0)
     * @param outlierIntensityMultiplier Multiplier for outlier resource usage
     */
    public OutlierSessionGenerator(double outlierProbability, double outlierIntensityMultiplier) {
        this.outlierProbability = outlierProbability;
        this.outlierIntensityMultiplier = outlierIntensityMultiplier;
        this.random = new Random();
        this.normalDistribution = new NormalDistribution(100.0, 30.0, random.nextLong());
        this.paretoDistribution = new ParetoDistribution(1.5, 50.0, random.nextLong());
    }
    
    /**
     * Initialize with a specific random seed for reproducibility.
     * 
     * @param outlierProbability Probability of a session being an outlier (0.0 to 1.0)
     * @param outlierIntensityMultiplier Multiplier for outlier resource usage
     * @param seed Random seed
     */
    public OutlierSessionGenerator(double outlierProbability, double outlierIntensityMultiplier, long seed) {
        this.outlierProbability = outlierProbability;
        this.outlierIntensityMultiplier = outlierIntensityMultiplier;
        this.random = new Random(seed);
        this.normalDistribution = new NormalDistribution(100.0, 30.0, random.nextLong());
        this.paretoDistribution = new ParetoDistribution(1.5, 50.0, random.nextLong());
    }
    
    /**
     * Represents a simulated user session with resource usage metrics.
     */
    public static class Session {
        public final boolean isOutlier;
        public final double cpuUsage; // Percentage
        public final double memoryUsage; // MB
        public final int networkRequests;
        public final long sessionDuration; // milliseconds
        public final int actionsPerformed;
        public final double resourceIntensity; // Overall intensity score
        
        public Session(boolean isOutlier, double cpuUsage, double memoryUsage, 
                      int networkRequests, long sessionDuration, 
                      int actionsPerformed, double resourceIntensity) {
            this.isOutlier = isOutlier;
            this.cpuUsage = cpuUsage;
            this.memoryUsage = memoryUsage;
            this.networkRequests = networkRequests;
            this.sessionDuration = sessionDuration;
            this.actionsPerformed = actionsPerformed;
            this.resourceIntensity = resourceIntensity;
        }
        
        @Override
        public String toString() {
            return String.format("Session[outlier=%s, cpu=%.2f%%, mem=%.2fMB, requests=%d, duration=%dms, actions=%d, intensity=%.2f]",
                    isOutlier, cpuUsage, memoryUsage, networkRequests, sessionDuration, actionsPerformed, resourceIntensity);
        }
    }
    
    /**
     * Generate a single session with potential outlier behavior.
     * 
     * @return A Session object with simulated metrics
     */
    public Session generateSession() {
        boolean isOutlier = random.nextDouble() < outlierProbability;
        
        if (isOutlier) {
            return generateOutlierSession();
        } else {
            return generateNormalSession();
        }
    }
    
    /**
     * Generate a normal (non-outlier) session.
     * Uses standard distributions for typical user behavior.
     * 
     * @return A normal Session object
     */
    private Session generateNormalSession() {
        double cpuUsage = normalDistribution.sample();
        cpuUsage = Math.max(5.0, Math.min(80.0, cpuUsage));
        
        double memoryUsage = normalDistribution.sample() * 1.5;
        memoryUsage = Math.max(20.0, Math.min(200.0, memoryUsage));
        
        int networkRequests = (int) Math.round(normalDistribution.sample() / 10.0);
        networkRequests = Math.max(1, Math.min(50, networkRequests));
        
        long sessionDuration = (long) (normalDistribution.sample() * 1000);
        sessionDuration = Math.max(5000, Math.min(300000, sessionDuration));
        
        int actionsPerformed = (int) (Math.random() * 20) + 1;
        
        double resourceIntensity = (cpuUsage + memoryUsage / 10.0 + networkRequests + sessionDuration / 10000.0) / 4.0;
        
        return new Session(false, cpuUsage, memoryUsage, networkRequests, sessionDuration, actionsPerformed, resourceIntensity);
    }
    
    /**
     * Generate an outlier session with extreme resource consumption.
     * Uses heavy-tailed distributions for rare, intense events.
     * 
     * @return An outlier Session object
     */
    private Session generateOutlierSession() {
        double baseCpuUsage = paretoDistribution.sample();
        double cpuUsage = baseCpuUsage * outlierIntensityMultiplier;
        cpuUsage = Math.max(50.0, Math.min(100.0, cpuUsage));
        
        double baseMemoryUsage = paretoDistribution.sample() * 3.0;
        double memoryUsage = baseMemoryUsage * outlierIntensityMultiplier;
        memoryUsage = Math.max(200.0, Math.min(1000.0, memoryUsage));
        
        int baseRequests = (int) (paretoDistribution.sample() / 10.0);
        int networkRequests = (int) (baseRequests * outlierIntensityMultiplier);
        networkRequests = Math.max(50, Math.min(500, networkRequests));
        
        long baseDuration = (long) (paretoDistribution.sample() * 1000);
        long sessionDuration = (long) (baseDuration * outlierIntensityMultiplier);
        sessionDuration = Math.max(300000, Math.min(3600000, sessionDuration));
        
        int actionsPerformed = (int) (paretoDistribution.sample() * 0.5);
        actionsPerformed = Math.max(50, Math.min(200, actionsPerformed));
        
        double resourceIntensity = (cpuUsage + memoryUsage / 10.0 + networkRequests + sessionDuration / 10000.0) / 4.0;
        
        return new Session(true, cpuUsage, memoryUsage, networkRequests, sessionDuration, actionsPerformed, resourceIntensity);
    }
    
    /**
     * Generate multiple sessions with a specified outlier ratio.
     * 
     * @param numberOfSessions Total number of sessions to generate
     * @param desiredOutlierRatio Desired ratio of outlier sessions (0.0 to 1.0)
     * @return Array of Session objects
     */
    public Session[] generateSessionsWithRatio(int numberOfSessions, double desiredOutlierRatio) {
        Session[] sessions = new Session[numberOfSessions];
        int outlierCount = (int) (numberOfSessions * desiredOutlierRatio);
        
        for (int i = 0; i < numberOfSessions; i++) {
            boolean isOutlier = i < outlierCount;
            sessions[i] = isOutlier ? generateOutlierSession() : generateNormalSession();
        }
        
        // Shuffle to distribute outliers randomly
        shuffleArray(sessions, random);
        
        return sessions;
    }
    
    /**
     * Generate a mixed population with different types of outliers.
     * Includes CPU-heavy, memory-heavy, network-heavy, and long-duration outliers.
     * 
     * @param numberOfSessions Total number of sessions
     * @return Array of Session objects with various outlier types
     */
    public Session[] generateMixedOutlierPopulation(int numberOfSessions) {
        Session[] sessions = new Session[numberOfSessions];
        
        for (int i = 0; i < numberOfSessions; i++) {
            if (random.nextDouble() < outlierProbability) {
                int outlierType = random.nextInt(4);
                sessions[i] = generateSpecificOutlier(outlierType);
            } else {
                sessions[i] = generateNormalSession();
            }
        }
        
        return sessions;
    }
    
    /**
     * Generate a specific type of outlier.
     * 
     * @param outlierType 0=CPU, 1=Memory, 2=Network, 3=Duration
     * @return An outlier Session object
     */
    private Session generateSpecificOutlier(int outlierType) {
        Session baseSession = generateOutlierSession();
        
        switch (outlierType) {
            case 0: // CPU-heavy outlier
                return new Session(true, Math.min(100.0, baseSession.cpuUsage * 1.5), 
                                   baseSession.memoryUsage * 0.5, baseSession.networkRequests / 2, 
                                   baseSession.sessionDuration, baseSession.actionsPerformed, 
                                   baseSession.resourceIntensity * 1.3);
            case 1: // Memory-heavy outlier
                return new Session(true, baseSession.cpuUsage * 0.7, 
                                   Math.min(1000.0, baseSession.memoryUsage * 1.5), baseSession.networkRequests / 2, 
                                   baseSession.sessionDuration, baseSession.actionsPerformed, 
                                   baseSession.resourceIntensity * 1.3);
            case 2: // Network-heavy outlier
                return new Session(true, baseSession.cpuUsage, baseSession.memoryUsage, 
                                   Math.min(1000, baseSession.networkRequests * 2), 
                                   baseSession.sessionDuration, baseSession.actionsPerformed, 
                                   baseSession.resourceIntensity * 1.4);
            case 3: // Long-duration outlier
                return new Session(true, baseSession.cpuUsage, baseSession.memoryUsage, 
                                   baseSession.networkRequests, 
                                   Math.min(7200000L, baseSession.sessionDuration * 2), 
                                   baseSession.actionsPerformed, baseSession.resourceIntensity * 1.5);
            default:
                return baseSession;
        }
    }
    
    /**
     * Shuffle an array using Fisher-Yates algorithm.
     * 
     * @param array Array to shuffle
     * @param rand Random instance
     */
    private void shuffleArray(Session[] array, Random rand) {
        for (int i = array.length - 1; i > 0; i--) {
            int j = rand.nextInt(i + 1);
            Session temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }
    }
    
    /**
     * Detect outlier sessions using statistical methods.
     * Uses interquartile range (IQR) method for outlier detection.
     * 
     * @param sessions Array of sessions to analyze
     * @return Array of boolean values indicating outliers
     */
    public boolean[] detectOutliers(Session[] sessions) {
        double[] intensities = new double[sessions.length];
        for (int i = 0; i < sessions.length; i++) {
            intensities[i] = sessions[i].resourceIntensity;
        }
        
        double q1 = calculatePercentile(intensities, 25);
        double q3 = calculatePercentile(intensities, 75);
        double iqr = q3 - q1;
        double upperBound = q3 + 1.5 * iqr;
        
        boolean[] isOutlier = new boolean[sessions.length];
        for (int i = 0; i < sessions.length; i++) {
            isOutlier[i] = sessions[i].resourceIntensity > upperBound;
        }
        
        return isOutlier;
    }
    
    /**
     * Calculate a percentile from an array of values.
     * 
     * @param values Array of values
     * @param percentile Percentile to calculate (0 to 100)
     * @return The percentile value
     */
    private double calculatePercentile(double[] values, int percentile) {
        java.util.Arrays.sort(values.clone());
        int index = (int) Math.ceil(percentile / 100.0 * values.length) - 1;
        return values[index];
    }
    
    /**
     * Generate stress test scenarios with extreme outlier combinations.
     * Useful for testing app behavior under extreme conditions.
     * 
     * @param scenarioName Name of the stress test scenario
     * @return Array of Session objects for the scenario
     */
    public Session[] generateStressTestScenario(String scenarioName) {
        switch (scenarioName.toLowerCase()) {
            case "cpu_spike":
                return generateCpuSpikeScenario();
            case "memory_leak":
                return generateMemoryLeakScenario();
            case "network_storm":
                return generateNetworkStormScenario();
            case "marathon_session":
                return generateMarathonSessionScenario();
            default:
                return generateMixedOutlierPopulation(100);
        }
    }
    
    private Session[] generateCpuSpikeScenario() {
        int normalSessions = 80;
        int cpuSpikeSessions = 20;
        Session[] sessions = new Session[normalSessions + cpuSpikeSessions];
        
        for (int i = 0; i < normalSessions; i++) {
            sessions[i] = generateNormalSession();
        }
        
        for (int i = normalSessions; i < sessions.length; i++) {
            Session base = generateNormalSession();
            sessions[i] = new Session(true, 95.0 + random.nextDouble() * 5.0,
                                      base.memoryUsage, base.networkRequests,
                                      base.sessionDuration, base.actionsPerformed,
                                      100.0);
        }
        
        shuffleArray(sessions, random);
        return sessions;
    }
    
    private Session[] generateMemoryLeakScenario() {
        int normalSessions = 90;
        int leakingSessions = 10;
        Session[] sessions = new Session[normalSessions + leakingSessions];
        
        for (int i = 0; i < normalSessions; i++) {
            sessions[i] = generateNormalSession();
        }
        
        for (int i = normalSessions; i < sessions.length; i++) {
            sessions[i] = new Session(true, 30.0 + random.nextDouble() * 20.0,
                                      800.0 + random.nextDouble() * 200.0,
                                      10, 600000, 50, 200.0);
        }
        
        shuffleArray(sessions, random);
        return sessions;
    }
    
    private Session[] generateNetworkStormScenario() {
        int normalSessions = 70;
        int stormSessions = 30;
        Session[] sessions = new Session[normalSessions + stormSessions];
        
        for (int i = 0; i < normalSessions; i++) {
            sessions[i] = generateNormalSession();
        }
        
        for (int i = normalSessions; i < sessions.length; i++) {
            Session base = generateNormalSession();
            sessions[i] = new Session(true, base.cpuUsage, base.memoryUsage,
                                      500 + random.nextInt(500),
                                      base.sessionDuration, base.actionsPerformed,
                                      150.0);
        }
        
        shuffleArray(sessions, random);
        return sessions;
    }
    
    private Session[] generateMarathonSessionScenario() {
        Session[] sessions = new Session[100];
        
        for (int i = 0; i < 95; i++) {
            sessions[i] = generateNormalSession();
        }
        
        for (int i = 95; i < sessions.length; i++) {
            sessions[i] = new Session(true, 40.0 + random.nextDouble() * 30.0,
                                      300.0 + random.nextDouble() * 200.0,
                                      50, 7200000L + random.nextInt(3600000), 500, 250.0);
        }
        
        shuffleArray(sessions, random);
        return sessions;
    }
    
    /**
     * Get the outlier probability parameter.
     * 
     * @return Probability of an outlier session
     */
    public double getOutlierProbability() {
        return outlierProbability;
    }
    
    /**
     * Get the outlier intensity multiplier.
     * 
     * @return Multiplier for outlier resource usage
     */
    public double getOutlierIntensityMultiplier() {
        return outlierIntensityMultiplier;
    }
}
