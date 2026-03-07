package com.simulation.distributional;

import java.util.Random;

/**
 * Conversion Rate Variance Hook - Binomial Distribution
 * 
 * Models goal completion (e.g., successful checkouts) using a Binomial Distribution
 * to simulate realistic success/failure fluctuations across a population.
 * 
 * The binomial distribution models the number of successes in a fixed number of
 * independent Bernoulli trials, each with the same probability of success.
 * 
 * Mathematical Formula:
 * P(X = k) = C(n, k) * p^k * (1-p)^(n-k)
 * 
 * Where:
 * - n = number of trials
 * - k = number of successes
 * - p = probability of success on each trial
 * - C(n, k) = binomial coefficient
 */
public class BinomialDistribution {
    
    private final Random random;
    private final double probabilityOfSuccess;
    
    /**
     * Initialize with a given probability of success (conversion rate).
     * 
     * @param probabilityOfSuccess The probability of success on each trial (0.0 to 1.0)
     */
    public BinomialDistribution(double probabilityOfSuccess) {
        this.probabilityOfSuccess = probabilityOfSuccess;
        this.random = new Random();
    }
    
    /**
     * Initialize with a given probability of success and a specific random seed.
     * Useful for reproducible simulations.
     * 
     * @param probabilityOfSuccess The probability of success on each trial (0.0 to 1.0)
     * @param seed The random seed for reproducibility
     */
    public BinomialDistribution(double probabilityOfSuccess, long seed) {
        this.probabilityOfSuccess = probabilityOfSuccess;
        this.random = new Random(seed);
    }
    
    /**
     * Simulate a single conversion event.
     * 
     * @return true if conversion succeeds, false otherwise
     */
    public boolean simulateSingleConversion() {
        return random.nextDouble() < probabilityOfSuccess;
    }
    
    /**
     * Simulate multiple conversion events and return the number of successes.
     * This follows the binomial distribution B(n, p).
     * 
     * @param numberOfTrials The number of conversion attempts
     * @return The number of successful conversions
     */
    public int simulateConversions(int numberOfTrials) {
        if (numberOfTrials < 0) {
            throw new IllegalArgumentException("Number of trials must be non-negative");
        }
        
        int successes = 0;
        for (int i = 0; i < numberOfTrials; i++) {
            if (simulateSingleConversion()) {
                successes++;
            }
        }
        return successes;
    }
    
    /**
     * Simulate conversions for a batch of users with varying conversion rates.
     * This creates realistic variance across a population.
     * 
     * @param numberOfUsers The number of users to simulate
     * @param conversionRateMean The mean conversion rate across the population
     * @param conversionRateStdDev The standard deviation of conversion rates
     * @return Array of conversion outcomes for each user (number of successes)
     */
    public int[] simulatePopulationConversions(
            int numberOfUsers,
            double conversionRateMean,
            double conversionRateStdDev) {
        
        int[] outcomes = new int[numberOfUsers];
        NormalDistribution rateDistribution = new NormalDistribution(conversionRateMean, conversionRateStdDev);
        
        for (int i = 0; i < numberOfUsers; i++) {
            double userConversionRate = rateDistribution.sample();
            userConversionRate = Math.max(0.0, Math.min(1.0, userConversionRate));
            
            BinomialDistribution userDistribution = new BinomialDistribution(userConversionRate, random.nextLong());
            outcomes[i] = userDistribution.simulateConversions(1);
        }
        
        return outcomes;
    }
    
    /**
     * Calculate the expected number of successes.
     * 
     * @param numberOfTrials The number of trials
     * @return Expected number of successes (n * p)
     */
    public double getExpectedValue(int numberOfTrials) {
        return numberOfTrials * probabilityOfSuccess;
    }
    
    /**
     * Calculate the variance of the distribution.
     * 
     * @param numberOfTrials The number of trials
     * @return Variance (n * p * (1-p))
     */
    public double getVariance(int numberOfTrials) {
        return numberOfTrials * probabilityOfSuccess * (1.0 - probabilityOfSuccess);
    }
    
    /**
     * Calculate the standard deviation of the distribution.
     * 
     * @param numberOfTrials The number of trials
     * @return Standard deviation (sqrt(n * p * (1-p)))
     */
    public double getStandardDeviation(int numberOfTrials) {
        return Math.sqrt(getVariance(numberOfTrials));
    }
    
    /**
     * Get the probability of success parameter.
     * 
     * @return The probability of success on each trial
     */
    public double getProbabilityOfSuccess() {
        return probabilityOfSuccess;
    }
    
    /**
     * Simulate conversion events with temporal variance.
     * Conversion rates can vary over time (e.g., peak hours vs. off-hours).
     * 
     * @param numberOfTrials Number of trials
     * @param timeFactor Time-based variance factor (-1.0 to 1.0)
     * @return Number of successful conversions
     */
    public int simulateWithTemporalVariance(int numberOfTrials, double timeFactor) {
        double adjustedProbability = probabilityOfSuccess * (1.0 + 0.3 * timeFactor);
        adjustedProbability = Math.max(0.0, Math.min(1.0, adjustedProbability));
        
        int successes = 0;
        for (int i = 0; i < numberOfTrials; i++) {
            if (random.nextDouble() < adjustedProbability) {
                successes++;
            }
        }
        return successes;
    }
}
