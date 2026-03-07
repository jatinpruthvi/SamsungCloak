package com.simulation.distributional;

import java.util.Random;

/**
 * Action Frequency Distribution Hook - Poisson Process
 * 
 * Implements a Poisson process to model the timing of discrete events (like
 * message sends or pings), reflecting how events naturally cluster in time.
 * 
 * The Poisson distribution models the number of events occurring in a fixed
 * interval of time or space, given these events occur with a known constant
 * mean rate and independently of the time since the last event.
 * 
 * Probability Mass Function:
 * P(X = k) = (λ^k * e^(-λ)) / k!
 * 
 * Where:
 * - k = number of events
 * - λ = expected number of events in the interval
 * - e = Euler's number (~2.71828)
 * 
 * The inter-arrival times follow an exponential distribution with mean 1/λ.
 */
public class PoissonProcess {
    
    private final Random random;
    private final double lambda; // rate parameter (events per unit time)
    
    /**
     * Initialize with a given rate parameter.
     * 
     * @param lambda The average number of events per unit time (must be > 0)
     */
    public PoissonProcess(double lambda) {
        if (lambda <= 0) {
            throw new IllegalArgumentException("Lambda must be positive");
        }
        this.lambda = lambda;
        this.random = new Random();
    }
    
    /**
     * Initialize with a given rate parameter and specific random seed.
     * 
     * @param lambda The average number of events per unit time (must be > 0)
     * @param seed The random seed for reproducibility
     */
    public PoissonProcess(double lambda, long seed) {
        if (lambda <= 0) {
            throw new IllegalArgumentException("Lambda must be positive");
        }
        this.lambda = lambda;
        this.random = new Random(seed);
    }
    
    /**
     * Generate a sample from the Poisson distribution using the
     * Knuth algorithm (inverse transform method).
     * 
     * Time complexity: O(λ) for small λ, but efficient for typical use cases.
     * 
     * @return Number of events in the interval
     */
    public int sample() {
        double L = Math.exp(-lambda);
        double p = 1.0;
        int k = 0;
        
        do {
            k++;
            p *= random.nextDouble();
        } while (p > L);
        
        return k - 1;
    }
    
    /**
     * Generate samples for multiple time intervals.
     * 
     * @param intervals Number of intervals to sample
     * @return Array of event counts for each interval
     */
    public int[] sampleIntervals(int intervals) {
        int[] samples = new int[intervals];
        for (int i = 0; i < intervals; i++) {
            samples[i] = sample();
        }
        return samples;
    }
    
    /**
     * Generate inter-arrival times between events.
     * These follow an exponential distribution with mean 1/λ.
     * 
     * @param numberOfEvents Number of inter-arrival times to generate
     * @return Array of time intervals between events
     */
    public double[] generateInterArrivalTimes(int numberOfEvents) {
        double[] interArrivalTimes = new double[numberOfEvents];
        for (int i = 0; i < numberOfEvents; i++) {
            interArrivalTimes[i] = generateInterArrivalTime();
        }
        return interArrivalTimes;
    }
    
    /**
     * Generate a single inter-arrival time.
     * Uses inverse transform: T = -ln(U) / λ where U ~ Uniform(0,1)
     * 
     * @return Time until next event
     */
    public double generateInterArrivalTime() {
        double u = random.nextDouble();
        return -Math.log(u) / lambda;
    }
    
    /**
     * Generate absolute timestamps for events within a time window.
     * 
     * @param timeWindow Duration of the time window
     * @return Array of event timestamps
     */
    public double[] generateEventTimestamps(double timeWindow) {
        java.util.List<Double> timestamps = new java.util.ArrayList<>();
        double currentTime = 0.0;
        
        while (currentTime < timeWindow) {
            double interArrivalTime = generateInterArrivalTime();
            currentTime += interArrivalTime;
            
            if (currentTime < timeWindow) {
                timestamps.add(currentTime);
            }
        }
        
        double[] result = new double[timestamps.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = timestamps.get(i);
        }
        
        return result;
    }
    
    /**
     * Generate event timestamps with clustering behavior.
     * Real-world events often cluster (e.g., message bursts).
     * 
     * @param timeWindow Duration of the time window
     * @param clusteringFactor How much to cluster events (0.0 = uniform, 1.0 = highly clustered)
     * @return Array of event timestamps
     */
    public double[] generateClusteredEventTimestamps(double timeWindow, double clusteringFactor) {
        java.util.List<Double> timestamps = new java.util.ArrayList<>();
        double currentTime = 0.0;
        
        while (currentTime < timeWindow) {
            double baseInterArrival = generateInterArrivalTime();
            
            if (random.nextDouble() < clusteringFactor) {
                // Burst of events
                int burstSize = 1 + random.nextInt(5);
                double burstInterval = baseInterArrival / (burstSize * 2);
                
                for (int i = 0; i < burstSize; i++) {
                    currentTime += burstInterval;
                    if (currentTime < timeWindow) {
                        timestamps.add(currentTime);
                    }
                }
            } else {
                currentTime += baseInterArrival;
                if (currentTime < timeWindow) {
                    timestamps.add(currentTime);
                }
            }
        }
        
        double[] result = new double[timestamps.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = timestamps.get(i);
        }
        
        return result;
    }
    
    /**
     * Calculate the probability of observing exactly k events.
     * 
     * @param k Number of events
     * @return Probability P(X = k)
     */
    public double probability(int k) {
        if (k < 0) {
            return 0.0;
        }
        
        double logProb = k * Math.log(lambda) - lambda - logFactorial(k);
        return Math.exp(logProb);
    }
    
    /**
     * Calculate the cumulative probability P(X ≤ k).
     * 
     * @param k Number of events
     * @return Cumulative probability
     */
    public double cumulativeProbability(int k) {
        if (k < 0) {
            return 0.0;
        }
        
        double sum = 0.0;
        for (int i = 0; i <= k; i++) {
            sum += probability(i);
        }
        return sum;
    }
    
    /**
     * Calculate the natural logarithm of factorial.
     * More numerically stable than calculating factorial directly.
     * 
     * @param n The number to calculate log factorial for
     * @return log(n!)
     */
    private double logFactorial(int n) {
        if (n <= 1) {
            return 0.0;
        }
        
        double sum = 0.0;
        for (int i = 2; i <= n; i++) {
            sum += Math.log(i);
        }
        return sum;
    }
    
    /**
     * Get the expected value (mean) of the distribution.
     * E[X] = λ
     * 
     * @return Expected number of events
     */
    public double getExpectedValue() {
        return lambda;
    }
    
    /**
     * Get the variance of the distribution.
     * Var[X] = λ
     * 
     * @return Variance
     */
    public double getVariance() {
        return lambda;
    }
    
    /**
     * Get the standard deviation of the distribution.
     * σ = √λ
     * 
     * @return Standard deviation
     */
    public double getStandardDeviation() {
        return Math.sqrt(lambda);
    }
    
    /**
     * Get the rate parameter λ.
     * 
     * @return Rate parameter
     */
    public double getLambda() {
        return lambda;
    }
    
    /**
     * Simulate a non-homogeneous Poisson process with time-varying rate.
     * Useful for modeling varying activity levels throughout the day.
     * 
     * @param timeWindow Duration of the time window
     * @param rateFunction Function that defines λ(t) at time t
     * @return Array of event timestamps
     */
    public double[] simulateNonHomogeneousProcess(double timeWindow, java.util.function.DoubleUnaryOperator rateFunction) {
        java.util.List<Double> timestamps = new java.util.ArrayList<>();
        double currentTime = 0.0;
        
        while (currentTime < timeWindow) {
            double currentLambda = rateFunction.applyAsDouble(currentTime);
            double u = random.nextDouble();
            double interArrivalTime = -Math.log(u) / currentLambda;
            currentTime += interArrivalTime;
            
            if (currentTime < timeWindow) {
                timestamps.add(currentTime);
            }
        }
        
        double[] result = new double[timestamps.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = timestamps.get(i);
        }
        
        return result;
    }
    
    /**
     * Generate event counts for multiple users simultaneously.
     * Each user has a potentially different rate parameter.
     * 
     * @param userLambdas Array of rate parameters for each user
     * @return Array of event counts for each user
     */
    public int[] sampleMultipleUsers(double[] userLambdas) {
        int[] eventCounts = new int[userLambdas.length];
        
        for (int i = 0; i < userLambdas.length; i++) {
            PoissonProcess userProcess = new PoissonProcess(userLambdas[i], random.nextLong());
            eventCounts[i] = userProcess.sample();
        }
        
        return eventCounts;
    }
}
