package com.simulation.maturity.level3;

import java.util.Random;

/**
 * Session Frequency Distribution using Pareto (Power Law)
 * 
 * Implements the 80/20 Engagement Rule where:
 * - ~20% of users generate ~80% of sessions (Power Users)
 * - ~80% of users generate ~20% of sessions (Casual Users)
 * 
 * Mathematical Model:
 * P(X > x) = (x_m / x)^α  for x >= x_m
 * 
 * Where:
 * - x_m = scale parameter (minimum sessions)
 * - α = shape parameter (tail index, lower = more extreme)
 * 
 * Configuration for Samsung Galaxy A12:
 * - Budget device = more casual users, lower engagement
 * - Shape: 1.5-2.0 captures realistic power user distribution
 * 
 * Device Target: Samsung Galaxy A12 (SM-A125U)
 */
public class SessionFrequencyDistribution {
    
    private final double shapeParameter;  // α (alpha) - tail index
    private final double scaleParameter;  // x_m - minimum value
    private final Random random;
    
    // Precomputed constants for efficiency
    private final double alphaMinusOne;
    private final double alphaMinusTwo;
    private final double alphaSquared;
    
    /**
     * Create Pareto distribution
     * 
     * @param shape Shape parameter α (must be > 0, typical: 1.5-3.0)
     * @param scale Minimum session count (must be > 0)
     */
    public SessionFrequencyDistribution(double shape, double scale) {
        if (shape <= 0 || scale <= 0) {
            throw new IllegalArgumentException("Shape and scale must be positive");
        }
        this.shapeParameter = shape;
        this.scaleParameter = scale;
        this.random = new Random();
        
        this.alphaMinusOne = shape - 1.0;
        this.alphaMinusTwo = shape - 2.0;
        this.alphaSquared = shape * shape;
    }
    
    public SessionFrequencyDistribution(double shape, double scale, long seed) {
        this(shape, scale);
        this.random = new Random(seed);
    }
    
    /**
     * Generate sample using inverse transform method
     * X = x_m / U^(1/α) where U ~ Uniform(0,1)
     */
    public double sample() {
        double u = random.nextDouble();
        // Handle edge case where u is very close to 0
        if (u < 1e-10) u = 1e-10;
        return scaleParameter / Math.pow(u, 1.0 / shapeParameter);
    }
    
    /**
     * Get session count as integer
     */
    public int sampleInt() {
        return Math.max(1, (int) Math.round(sample()));
    }
    
    /**
     * Generate samples for entire population
     * 
     * @param populationSize Number of users
     * @return Array of session counts
     */
    public int[] samplePopulation(int populationSize) {
        int[] sessions = new int[populationSize];
        for (int i = 0; i < populationSize; i++) {
            sessions[i] = sampleInt();
        }
        return sessions;
    }
    
    /**
     * Calculate expected value (mean)
     * E[X] = α * x_m / (α - 1) for α > 1
     */
    public double getExpectedValue() {
        if (shapeParameter <= 1.0) {
            return Double.POSITIVE_INFINITY;
        }
        return shapeParameter * scaleParameter / alphaMinusOne;
    }
    
    /**
     * Calculate variance
     * Var[X] = α * x_m² / ((α-1)² * (α-2)) for α > 2
     */
    public double getVariance() {
        if (shapeParameter <= 2.0) {
            return Double.POSITIVE_INFINITY;
        }
        double numerator = shapeParameter * scaleParameter * scaleParameter;
        double denominator = (alphaMinusOne * alphaMinusOne) * alphaMinusTwo;
        return numerator / denominator;
    }
    
    /**
     * Get the proportion of users above threshold (power users)
     * P(X > threshold) = (x_m / threshold)^α
     */
    public double getPowerUserProportion(double threshold) {
        if (threshold < scaleParameter) {
            return 1.0;
        }
        return Math.pow(scaleParameter / threshold, shapeParameter);
    }
    
    /**
     * Calculate Gini coefficient (inequality measure)
     * G = 1 / (2α - 1) for α > 0.5
     * Higher = more unequal (more power users)
     */
    public double getGiniCoefficient() {
        if (shapeParameter <= 0.5) {
            return 1.0;
        }
        return 1.0 / (2.0 * shapeParameter - 1.0);
    }
    
    /**
     * Generate segmented population with explicit power user definition
     * 
     * @param totalUsers Total population
     * @param powerUserThreshold Sessions threshold to be "power user"
     * @return Segmentation result
     */
    public PopulationSegmentation segmentPopulation(int totalUsers, int powerUserThreshold) {
        int powerUsers = 0;
        int casualUsers = 0;
        int totalSessions = 0;
        
        for (int i = 0; i < totalUsers; i++) {
            int sessions = sampleInt();
            totalSessions += sessions;
            
            if (sessions >= powerUserThreshold) {
                powerUsers++;
            } else {
                casualUsers++;
            }
        }
        
        return new PopulationSegmentation(
            totalUsers,
            powerUsers,
            casualUsers,
            totalSessions,
            (double) totalSessions / totalUsers,
            powerUserThreshold
        );
    }
    
    /**
     * Preconfigured for typical app engagement (80/20 rule)
     * Shape = 1.5 produces approximately 20% power users
     */
    public static SessionFrequencyDistribution forAppEngagement() {
        return new SessionFrequencyDistribution(1.5, 1);
    }
    
    /**
     * Preconfigured for gaming app (higher engagement)
     * Shape = 2.0 produces more moderate distribution
     */
    public static SessionFrequencyDistribution forGamingApp() {
        return new SessionFrequencyDistribution(2.0, 2);
    }
    
    /**
     * Preconfigured for budget device users (lower overall engagement)
     * Shape = 1.8 with minimum 1 session per month
     */
    public static SessionFrequencyDistribution forBudgetDevice() {
        return new SessionFrequencyDistribution(1.8, 1);
    }
    
    /**
     * Get shape parameter
     */
    public double getShapeParameter() {
        return shapeParameter;
    }
    
    /**
     * Get scale parameter
     */
    public double getScaleParameter() {
        return scaleParameter;
    }
    
    /**
     * Result of population segmentation
     */
    public static class PopulationSegmentation {
        public final int totalUsers;
        public final int powerUserCount;
        public final int casualUserCount;
        public final int totalSessions;
        public final double averageSessions;
        public final int powerUserThreshold;
        
        public PopulationSegmentation(int totalUsers, int powerUserCount, int casualUserCount,
                                      int totalSessions, double averageSessions, int threshold) {
            this.totalUsers = totalUsers;
            this.powerUserCount = powerUserCount;
            this.casualUserCount = casualUserCount;
            this.totalSessions = totalSessions;
            this.averageSessions = averageSessions;
            this.powerUserThreshold = threshold;
        }
        
        public double getPowerUserPercentage() {
            return (powerUserCount * 100.0) / totalUsers;
        }
        
        public double getCasualUserPercentage() {
            return (casualUserCount * 100.0) / totalUsers;
        }
        
        public double getPowerUserSessionPercentage() {
            // Approximate: assume power users have threshold sessions
            double powerUserSessions = powerUserCount * powerUserThreshold;
            return (powerUserSessions * 100.0) / totalSessions;
        }
        
        @Override
        public String toString() {
            return String.format(
                "Segmentation[users=%d, power=%d (%.1f%%), casual=%d (%.1f%%), " +
                "powerUserSessionPct=%.1f%%, avgSessions=%.1f]",
                totalUsers, 
                powerUserCount, getPowerUserPercentage(),
                casualUserCount, getCasualUserPercentage(),
                getPowerUserSessionPercentage(),
                averageSessions
            );
        }
    }
}
