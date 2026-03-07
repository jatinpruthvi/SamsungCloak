package com.simulation.distributional;

import java.util.Random;

/**
 * Non-Uniform Sampling Hook - Gaussian (Normal) Distribution
 * 
 * Provides stochastic sampling from a Gaussian (Normal) distribution rather
 * than a flat random range, ensuring the aggregate behavior matches real-world
 * human data.
 * 
 * The Normal distribution is ubiquitous in nature and human behavior,
 * modeling phenomena where values cluster around a mean with symmetric
 * deviations.
 * 
 * Probability Density Function:
 * f(x) = (1 / (σ * √(2π))) * e^(-((x-μ)²) / (2σ²))
 * 
 * Where:
 * - μ (mu) = mean (center of distribution)
 * - σ (sigma) = standard deviation (spread)
 * - e = Euler's number (~2.71828)
 * - π = pi (~3.14159)
 * 
 * Key Properties:
 * - 68.27% of values fall within 1σ of μ
 * - 95.45% of values fall within 2σ of μ
 * - 99.73% of values fall within 3σ of μ
 */
public class NormalDistribution {
    
    private final Random random;
    private final double mean;
    private final double standardDeviation;
    private boolean hasSpareValue = false;
    private double spareValue;
    
    /**
     * Initialize with mean and standard deviation.
     * 
     * @param mean The center of the distribution (μ)
     * @param standardDeviation The spread of the distribution (σ, must be > 0)
     */
    public NormalDistribution(double mean, double standardDeviation) {
        if (standardDeviation <= 0) {
            throw new IllegalArgumentException("Standard deviation must be positive");
        }
        this.mean = mean;
        this.standardDeviation = standardDeviation;
        this.random = new Random();
    }
    
    /**
     * Initialize with mean, standard deviation, and specific random seed.
     * 
     * @param mean The center of the distribution (μ)
     * @param standardDeviation The spread of the distribution (σ, must be > 0)
     * @param seed The random seed for reproducibility
     */
    public NormalDistribution(double mean, double standardDeviation, long seed) {
        if (standardDeviation <= 0) {
            throw new IllegalArgumentException("Standard deviation must be positive");
        }
        this.mean = mean;
        this.standardDeviation = standardDeviation;
        this.random = new Random(seed);
    }
    
    /**
     * Generate a sample using the Box-Muller transform.
     * This is an efficient method for generating normally-distributed values.
     * 
     * The transform generates two independent standard normal variables:
     * Z1 = √(-2*ln(U1)) * cos(2π*U2)
     * Z2 = √(-2*ln(U1)) * sin(2π*U2)
     * 
     * where U1, U2 ~ Uniform(0,1)
     * 
     * We use the spare value trick to generate two values at once.
     * 
     * @return A random sample from the normal distribution
     */
    public double sample() {
        if (hasSpareValue) {
            hasSpareValue = false;
            return spareValue * standardDeviation + mean;
        }
        
        double u1, u2, s;
        do {
            u1 = random.nextDouble() * 2.0 - 1.0;
            u2 = random.nextDouble() * 2.0 - 1.0;
            s = u1 * u1 + u2 * u2;
        } while (s >= 1.0 || s == 0.0);
        
        double multiplier = Math.sqrt(-2.0 * Math.log(s) / s);
        spareValue = u2 * multiplier;
        hasSpareValue = true;
        
        return u1 * multiplier * standardDeviation + mean;
    }
    
    /**
     * Generate multiple samples.
     * 
     * @param sampleSize Number of samples to generate
     * @return Array of samples
     */
    public double[] sample(int sampleSize) {
        double[] samples = new double[sampleSize];
        for (int i = 0; i < sampleSize; i++) {
            samples[i] = sample();
        }
        return samples;
    }
    
    /**
     * Generate samples constrained to a specific range.
     * Uses rejection sampling to ensure values fall within bounds.
     * 
     * @param sampleSize Number of samples to generate
     * @param min Minimum value (inclusive)
     * @param max Maximum value (inclusive)
     * @return Array of samples within the specified range
     */
    public double[] sampleConstrained(int sampleSize, double min, double max) {
        if (min > max) {
            throw new IllegalArgumentException("Min must be less than or equal to max");
        }
        
        double[] samples = new double[sampleSize];
        int generated = 0;
        
        while (generated < sampleSize) {
            double value = sample();
            if (value >= min && value <= max) {
                samples[generated] = value;
                generated++;
            }
        }
        
        return samples;
    }
    
    /**
     * Generate integer samples from a discretized normal distribution.
     * Useful for modeling counts or discrete metrics.
     * 
     * @param sampleSize Number of samples to generate
     * @param min Minimum integer value (inclusive)
     * @param max Maximum integer value (inclusive)
     * @return Array of integer samples
     */
    public int[] sampleIntegers(int sampleSize, int min, int max) {
        double[] continuousSamples = sampleConstrained(sampleSize, min - 0.5, max + 0.5);
        int[] integerSamples = new int[sampleSize];
        
        for (int i = 0; i < sampleSize; i++) {
            integerSamples[i] = (int) Math.round(continuousSamples[i]);
            integerSamples[i] = Math.max(min, Math.min(max, integerSamples[i]));
        }
        
        return integerSamples;
    }
    
    /**
     * Generate samples for a population with multiple demographic segments.
     * Each segment has its own mean and standard deviation.
     * 
     * @param populationSize Total population size
     * @param segments Array of segment information (each element is [mean, stdDev, proportion])
     * @return Array of samples representing the mixed population
     */
    public double[] sampleMixedPopulation(int populationSize, double[][] segments) {
        double[] samples = new double[populationSize];
        int currentIndex = 0;
        
        for (double[] segment : segments) {
            double segmentMean = segment[0];
            double segmentStdDev = segment[1];
            double proportion = segment[2];
            
            int segmentSize = (int) (populationSize * proportion);
            NormalDistribution segmentDist = new NormalDistribution(segmentMean, segmentStdDev, random.nextLong());
            
            double[] segmentSamples = segmentDist.sample(segmentSize);
            System.arraycopy(segmentSamples, 0, samples, currentIndex, segmentSamples.length);
            currentIndex += segmentSamples.length;
        }
        
        // Fill any remaining slots with the last segment
        while (currentIndex < populationSize) {
            samples[currentIndex] = sample();
            currentIndex++;
        }
        
        // Shuffle to mix segments
        shuffleArray(samples, random);
        
        return samples;
    }
    
    /**
     * Calculate the probability density at a given value.
     * 
     * @param x The value to evaluate
     * @return Probability density f(x)
     */
    public double probabilityDensity(double x) {
        double exponent = -Math.pow(x - mean, 2) / (2 * Math.pow(standardDeviation, 2));
        double coefficient = 1.0 / (standardDeviation * Math.sqrt(2 * Math.PI));
        return coefficient * Math.exp(exponent);
    }
    
    /**
     * Calculate the cumulative distribution function at a given value.
     * Uses the error function (erf) approximation.
     * 
     * @param x The value to evaluate
     * @return Cumulative probability F(x)
     */
    public double cumulativeDistribution(double x) {
        return 0.5 * (1.0 + erf((x - mean) / (standardDeviation * Math.sqrt(2.0))));
    }
    
    /**
     * Approximation of the error function using a numerical formula.
     * erf(x) ≈ 1 - (a1*t + a2*t^2 + a3*t^3 + a4*t^4 + a5*t^5) * e^(-x^2)
     * where t = 1/(1 + p*x)
     * 
     * @param x The value to evaluate
     * @return Approximation of erf(x)
     */
    private double erf(double x) {
        double p = 0.3275911;
        double a1 = 0.254829592;
        double a2 = -0.284496736;
        double a3 = 1.421413741;
        double a4 = -1.453152027;
        double a5 = 1.061405429;
        
        int sign = x < 0 ? -1 : 1;
        x = Math.abs(x);
        
        double t = 1.0 / (1.0 + p * x);
        double y = 1.0 - (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t * Math.exp(-x * x);
        
        return sign * y;
    }
    
    /**
     * Calculate the inverse CDF (quantile function).
     * Returns the value x such that P(X ≤ x) = p.
     * 
     * Uses the Beasley-Springer-Moro approximation.
     * 
     * @param p Probability (0.0 to 1.0)
     * @return The quantile value
     */
    public double inverseCumulativeDistribution(double p) {
        if (p <= 0.0) {
            return Double.NEGATIVE_INFINITY;
        }
        if (p >= 1.0) {
            return Double.POSITIVE_INFINITY;
        }
        
        // Approximate standard normal quantile
        double a[] = {-3.969683028665376e+01, 2.209460984245205e+02,
                      -2.759285104469687e+02, 1.383577518672690e+02,
                      -3.066479806614716e+01, 2.506628277459239e+00};
        double b[] = {-5.447609879822406e+01, 1.615858368580409e+02,
                      -1.556989798598866e+02, 6.680131188771972e+01,
                      -1.328068155288572e+01};
        double c[] = {-7.784894002430293e-03, -3.223964580411365e-01,
                      -2.400758277161838e+00, -2.549732539343734e+00,
                       4.374664141464968e+00, 2.938163982698783e+00};
        double d[] = {7.784695709041462e-03, 3.224671290700398e-01,
                       2.445134137142996e+00, 3.754408661907416e+00};
        
        double q = Math.min(p, 1 - p);
        double t, u;
        
        if (q > 0.02425) {
            u = q - 0.5;
            t = u * u;
            u = u * (((((a[0] * t + a[1]) * t + a[2]) * t + a[3]) * t + a[4]) * t + a[5]) /
                (((((b[0] * t + b[1]) * t + b[2]) * t + b[3]) * t + b[4]) * t + 1);
        } else {
            t = Math.sqrt(-2.0 * Math.log(q));
            u = (((((c[0] * t + c[1]) * t + c[2]) * t + c[3]) * t + c[4]) * t + c[5]) /
                ((((d[0] * t + d[1]) * t + d[2]) * t + d[3]) * t + 1);
        }
        
        if (p < 0.5) {
            return mean + standardDeviation * (-u);
        } else {
            return mean + standardDeviation * u;
        }
    }
    
    /**
     * Get the probability that a value falls within a given range.
     * 
     * @param lower Lower bound
     * @param upper Upper bound
     * @return Probability P(lower ≤ X ≤ upper)
     */
    public double probabilityInRange(double lower, double upper) {
        return cumulativeDistribution(upper) - cumulativeDistribution(lower);
    }
    
    /**
     * Get the probability that a value is within k standard deviations of the mean.
     * 
     * @param k Number of standard deviations
     * @return Probability P(μ - kσ ≤ X ≤ μ + kσ)
     */
    public double probabilityWithinStandardDeviations(double k) {
        return cumulativeDistribution(mean + k * standardDeviation) - 
               cumulativeDistribution(mean - k * standardDeviation);
    }
    
    /**
     * Calculate the z-score for a given value.
     * z = (x - μ) / σ
     * 
     * @param x The value to convert
     * @return The z-score
     */
    public double zScore(double x) {
        return (x - mean) / standardDeviation;
    }
    
    /**
     * Calculate the mean (μ) of the distribution.
     * 
     * @return Mean
     */
    public double getMean() {
        return mean;
    }
    
    /**
     * Calculate the variance (σ²) of the distribution.
     * 
     * @return Variance
     */
    public double getVariance() {
        return standardDeviation * standardDeviation;
    }
    
    /**
     * Calculate the standard deviation (σ) of the distribution.
     * 
     * @return Standard deviation
     */
    public double getStandardDeviation() {
        return standardDeviation;
    }
    
    /**
     * Generate correlated samples from two normal distributions.
     * Uses the Cholesky decomposition method.
     * 
     * @param sampleSize Number of samples to generate
     * @param correlation Correlation coefficient (-1 to 1)
     * @param otherDistribution The other normal distribution
     * @return Array of [sample1, sample2] pairs
     */
    public double[][] generateCorrelatedSamples(int sampleSize, double correlation, NormalDistribution otherDistribution) {
        if (correlation < -1.0 || correlation > 1.0) {
            throw new IllegalArgumentException("Correlation must be between -1 and 1");
        }
        
        double[][] correlatedSamples = new double[sampleSize][2];
        
        for (int i = 0; i < sampleSize; i++) {
            double z1 = sample();
            double z2 = otherDistribution.sample();
            
            // Apply correlation
            double correlatedZ2 = correlation * z1 + Math.sqrt(1 - correlation * correlation) * z2;
            
            correlatedSamples[i][0] = z1;
            correlatedSamples[i][1] = correlatedZ2;
        }
        
        return correlatedSamples;
    }
    
    /**
     * Shuffle an array using Fisher-Yates algorithm.
     * 
     * @param array Array to shuffle
     * @param rand Random instance
     */
    private void shuffleArray(double[] array, Random rand) {
        for (int i = array.length - 1; i > 0; i--) {
            int j = rand.nextInt(i + 1);
            double temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }
    }
    
    /**
     * Generate samples with temporal autocorrelation.
     * Values tend to be similar to previous values (momentum).
     * 
     * @param sampleSize Number of samples to generate
     * @param autocorrelation Autocorrelation coefficient (0 to 1)
     * @return Array of correlated samples
     */
    public double[] sampleWithAutocorrelation(int sampleSize, double autocorrelation) {
        if (autocorrelation < 0 || autocorrelation >= 1) {
            throw new IllegalArgumentException("Autocorrelation must be between 0 and 1");
        }
        
        double[] samples = new double[sampleSize];
        samples[0] = sample();
        
        for (int i = 1; i < sampleSize; i++) {
            double innovation = sample();
            samples[i] = autocorrelation * samples[i - 1] + 
                         Math.sqrt(1 - autocorrelation * autocorrelation) * innovation;
        }
        
        return samples;
    }
}
