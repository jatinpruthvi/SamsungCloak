package com.simulation.distributional;

import java.util.Random;

/**
 * Engagement Distribution Hook - Pareto (Power Law) Distribution
 * 
 * Implements a Power Law (Pareto) distribution to ensure a small percentage of
 * "Power Users" perform the bulk of interactions, while the majority remain in
 * a "Long-Tail" low-activity state.
 * 
 * The Pareto distribution follows the 80/20 rule (Pareto Principle), where
 * roughly 80% of effects come from 20% of causes. In user engagement, this
 * means a small number of users account for most interactions.
 * 
 * Probability Density Function:
 * f(x) = (α * x_m^α) / (x^(α+1)) for x ≥ x_m
 * 
 * Cumulative Distribution Function:
 * F(x) = 1 - (x_m / x)^α for x ≥ x_m
 * 
 * Where:
 * - α (alpha) = shape parameter (also called Pareto index)
 * - x_m = scale parameter (minimum value)
 */
public class ParetoDistribution {
    
    private final Random random;
    private final double shapeParameter; // alpha
    private final double scaleParameter; // x_m (minimum value)
    
    /**
     * Initialize with shape and scale parameters.
     * 
     * Typical values for user engagement:
     * - shapeParameter (α): 1.0 to 3.0 (lower = more extreme power users)
     * - scaleParameter (x_m): minimum engagement level
     * 
     * @param shapeParameter The shape parameter α (must be > 0)
     * @param scaleParameter The scale parameter x_m (minimum value, must be > 0)
     */
    public ParetoDistribution(double shapeParameter, double scaleParameter) {
        if (shapeParameter <= 0 || scaleParameter <= 0) {
            throw new IllegalArgumentException("Shape and scale parameters must be positive");
        }
        this.shapeParameter = shapeParameter;
        this.scaleParameter = scaleParameter;
        this.random = new Random();
    }
    
    /**
     * Initialize with shape, scale parameters and a specific random seed.
     * 
     * @param shapeParameter The shape parameter α (must be > 0)
     * @param scaleParameter The scale parameter x_m (minimum value, must be > 0)
     * @param seed The random seed for reproducibility
     */
    public ParetoDistribution(double shapeParameter, double scaleParameter, long seed) {
        if (shapeParameter <= 0 || scaleParameter <= 0) {
            throw new IllegalArgumentException("Shape and scale parameters must be positive");
        }
        this.shapeParameter = shapeParameter;
        this.scaleParameter = scaleParameter;
        this.random = new Random(seed);
    }
    
    /**
     * Generate a sample from the Pareto distribution using inverse transform sampling.
     * 
     * Formula: X = x_m / U^(1/α)
     * where U ~ Uniform(0,1)
     * 
     * @return A random sample from the Pareto distribution
     */
    public double sample() {
        double u = random.nextDouble();
        return scaleParameter / Math.pow(u, 1.0 / shapeParameter);
    }
    
    /**
     * Generate multiple samples representing user engagement levels.
     * 
     * @param sampleSize Number of users to generate engagement for
     * @return Array of engagement scores
     */
    public double[] samplePopulation(int sampleSize) {
        double[] samples = new double[sampleSize];
        for (int i = 0; i < sampleSize; i++) {
            samples[i] = sample();
        }
        return samples;
    }
    
    /**
     * Generate discrete action counts for users based on Pareto distribution.
     * Useful for modeling number of sessions, clicks, or interactions.
     * 
     * @param sampleSize Number of users
     * @param maxActions Maximum number of actions per user (for capping)
     * @return Array of action counts
     */
    public int[] sampleActionCounts(int sampleSize, int maxActions) {
        int[] actionCounts = new int[sampleSize];
        for (int i = 0; i < sampleSize; i++) {
            double continuousValue = sample();
            actionCounts[i] = (int) Math.min(maxActions, Math.round(continuousValue));
            actionCounts[i] = Math.max(1, actionCounts[i]); // At least 1 action
        }
        return actionCounts;
    }
    
    /**
     * Calculate the probability density at a given value.
     * 
     * @param x The value to evaluate
     * @return Probability density f(x)
     */
    public double probabilityDensity(double x) {
        if (x < scaleParameter) {
            return 0.0;
        }
        return (shapeParameter * Math.pow(scaleParameter, shapeParameter)) / 
               Math.pow(x, shapeParameter + 1);
    }
    
    /**
     * Calculate the cumulative distribution function at a given value.
     * 
     * @param x The value to evaluate
     * @return Cumulative probability F(x)
     */
    public double cumulativeDistribution(double x) {
        if (x < scaleParameter) {
            return 0.0;
        }
        return 1.0 - Math.pow(scaleParameter / x, shapeParameter);
    }
    
    /**
     * Calculate the expected value (mean) of the distribution.
     * 
     * E[X] = (α * x_m) / (α - 1) for α > 1
     * 
     * @return Expected value, or infinity if α ≤ 1
     */
    public double getExpectedValue() {
        if (shapeParameter <= 1.0) {
            return Double.POSITIVE_INFINITY;
        }
        return (shapeParameter * scaleParameter) / (shapeParameter - 1.0);
    }
    
    /**
     * Calculate the variance of the distribution.
     * 
     * Var[X] = (x_m^2 * α) / ((α - 1)^2 * (α - 2)) for α > 2
     * 
     * @return Variance, or infinity if α ≤ 2
     */
    public double getVariance() {
        if (shapeParameter <= 2.0) {
            return Double.POSITIVE_INFINITY;
        }
        return (Math.pow(scaleParameter, 2) * shapeParameter) / 
               (Math.pow(shapeParameter - 1.0, 2) * (shapeParameter - 2.0));
    }
    
    /**
     * Get the proportion of users above a certain engagement threshold.
     * Useful for identifying "Power Users".
     * 
     * @param threshold The engagement threshold
     * @return Proportion of users with engagement >= threshold
     */
    public double getProportionAboveThreshold(double threshold) {
        if (threshold < scaleParameter) {
            return 1.0;
        }
        return Math.pow(scaleParameter / threshold, shapeParameter);
    }
    
    /**
     * Generate a segmented population with predefined power user percentages.
     * 
     * @param totalUsers Total number of users
     * @param powerUserPercentage Percentage of power users (0.0 to 1.0)
     * @param powerUserActions Average actions for power users
     * @param normalUserActions Average actions for normal users
     * @return Array of action counts for all users
     */
    public int[] generateSegmentedPopulation(
            int totalUsers,
            double powerUserPercentage,
            int powerUserActions,
            int normalUserActions) {
        
        int[] actionCounts = new int[totalUsers];
        int powerUserCount = (int) (totalUsers * powerUserPercentage);
        
        ParetoDistribution powerUserDist = new ParetoDistribution(1.5, powerUserActions * 0.5, random.nextLong());
        ParetoDistribution normalUserDist = new ParetoDistribution(3.0, normalUserActions * 0.5, random.nextLong());
        
        for (int i = 0; i < totalUsers; i++) {
            if (i < powerUserCount) {
                double val = powerUserDist.sample();
                actionCounts[i] = (int) Math.max(1, Math.min(powerUserActions * 3, val));
            } else {
                double val = normalUserDist.sample();
                actionCounts[i] = (int) Math.max(1, Math.min(normalUserActions * 3, val));
            }
        }
        
        return actionCounts;
    }
    
    /**
     * Calculate the Gini coefficient for inequality measurement.
     * Higher values (closer to 1) indicate more extreme power user behavior.
     * 
     * G = 1 / (2α - 1) for α > 0.5
     * 
     * @return Gini coefficient
     */
    public double getGiniCoefficient() {
        if (shapeParameter <= 0.5) {
            return 1.0; // Maximum inequality
        }
        return 1.0 / (2.0 * shapeParameter - 1.0);
    }
    
    /**
     * Get the shape parameter (α).
     * 
     * @return Shape parameter
     */
    public double getShapeParameter() {
        return shapeParameter;
    }
    
    /**
     * Get the scale parameter (x_m).
     * 
     * @return Scale parameter
     */
    public double getScaleParameter() {
        return scaleParameter;
    }
}
