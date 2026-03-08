package com.simulation.maturity.level3;

import java.util.Random;

/**
 * Log-Normal Distribution for Inter-Arrival Times (IAT)
 * 
 * Models realistic human interaction timing patterns where:
 * - Most interactions have short delays (typical human response)
 * - Long tail of occasionally slower responses (distraction, thought)
 * 
 * Mathematical Model:
 * X ~ LogNormal(μ, σ) where:
 * - μ (mu) = log(median) - σ²/2
 * - σ (sigma) = shape parameter controlling spread
 * - Mean = exp(μ + σ²/2)
 * - Median = exp(μ)
 * 
 * Usage: UI element interaction timing, tap intervals, session gaps
 * Device Target: Samsung Galaxy A12 (SM-A125U)
 */
public class LogNormalDistribution {
    
    private final double mu;      // Location parameter (log-scale mean)
    private final double sigma;   // Shape parameter (log-scale std dev)
    private final Random random;
    
    /**
     * Create log-normal distribution with median and coefficient of variation
     * 
     * @param medianMs Median inter-arrival time in milliseconds
     * @param cv Coefficient of variation (0.5 = moderate variability, 1.0 = high)
     */
    public LogNormalDistribution(double medianMs, double cv) {
        this.sigma = Math.sqrt(Math.log(1.0 + cv * cv));
        this.mu = Math.log(medianMs) - (sigma * sigma) / 2.0;
        this.random = new Random();
    }
    
    /**
     * Create with explicit parameters and seed
     */
    public LogNormalDistribution(double mu, double sigma, long seed) {
        this.mu = mu;
        this.sigma = sigma;
        this.random = new Random(seed);
    }
    
    /**
     * Generate a sample using inverse transform sampling
     * 
     * @return Random sample from log-normal distribution
     */
    public double sample() {
        double z = random.nextGaussian();
        return Math.exp(mu + sigma * z);
    }
    
    /**
     * Generate sample in milliseconds
     */
    public long sampleMs() {
        return (long) Math.max(1, sample());
    }
    
    /**
     * Generate batch of samples
     */
    public double[] sample(int n) {
        double[] samples = new double[n];
        for (int i = 0; i < n; i++) {
            samples[i] = sample();
        }
        return samples;
    }
    
    /**
     * Get the theoretical median
     */
    public double getMedian() {
        return Math.exp(mu);
    }
    
    /**
     * Get the theoretical mean
     */
    public double getMean() {
        return Math.exp(mu + (sigma * sigma) / 2.0);
    }
    
    /**
     * Get the theoretical standard deviation
     */
    public double getStandardDeviation() {
        double variance = (Math.exp(sigma * sigma) - 1.0) * Math.exp(2 * mu + sigma * sigma);
        return Math.sqrt(variance);
    }
    
    /**
     * Calculate probability density at x
     */
    public double pdf(double x) {
        if (x <= 0) return 0;
        double z = (Math.log(x) - mu) / sigma;
        return (1.0 / (x * sigma * Math.sqrt(2 * Math.PI))) * 
               Math.exp(-z * z / 2.0);
    }
    
    /**
     * Calculate cumulative probability P(X <= x)
     */
    public double cdf(double x) {
        if (x <= 0) return 0;
        double z = (Math.log(x) - mu) / sigma;
        return normalCdf(z);
    }
    
    /**
     * Standard normal CDF using approximation
     */
    private double normalCdf(double z) {
        final double a1 = 0.254829592;
        final double a2 = -0.284496736;
        final double a3 = 1.421413741;
        final double a4 = -1.453152027;
        final double a5 = 1.061405429;
        final double p = 0.3275911;
        
        int sign = z < 0 ? -1 : 1;
        z = Math.abs(z) / Math.sqrt(2);
        double t = 1.0 / (1.0 + p * z);
        double y = 1.0 - (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t * Math.exp(-z * z);
        return 0.5 * (1.0 + sign * y);
    }
    
    /**
     * Generate human-like inter-arrival times for UI interactions
     * Typical values: 150-500ms for casual browsing, 50-150ms for rapid tapping
     */
    public static LogNormalDistribution forUIInteraction() {
        return new LogNormalDistribution(150, 0.7); // 150ms median, 70% CV
    }
    
    /**
     * Generate human-like session gaps (between app switches)
     * Typical values: 30s - 5min for casual users
     */
    public static LogNormalDistribution forSessionGap() {
        return new LogNormalDistribution(30000, 1.2); // 30s median, 120% CV
    }
    
    /**
     * Generate thinking/delay times before making a decision
     * Typical values: 200ms - 2s
     */
    public static LogNormalDistribution forDecisionTime() {
        return new LogNormalDistribution(500, 0.8); // 500ms median, 80% CV
    }
}
