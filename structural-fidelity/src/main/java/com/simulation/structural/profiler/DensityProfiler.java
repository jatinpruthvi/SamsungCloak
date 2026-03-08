package com.simulation.structural.profiler;

import java.util.*;

/**
 * DensityProfiler - Population Profiling with Distributional Alignment
 *
 * Implements Power Law (Pareto) and Gaussian distributions to ensure aggregate
 * metrics align with standard human population benchmarks for high-fidelity
 * synthetic population simulation.
 *
 * Mathematical Foundation:
 *
 * 1. Power Law Distribution (Pareto Principle):
 *    P(X > x) = (x_min / x)^α
 *    - Session frequency: 20% of users generate 80% of sessions
 *    - Feature usage: Core features used by all, power features by few
 *    - Engagement depth: Long-tail distribution of session durations
 *
 * 2. Gaussian (Normal) Distribution:
 *    f(x) = (1/σ√(2π)) × e^(-(x-μ)²/(2σ²))
 *    - Biometric latencies: Human motor response times
 *    - Session start times: Clustered around behavioral peaks
 *    - Content consumption: Normal distribution around mean interests
 *
 * 3. Log-Normal Distribution:
 *    - Income/Spending patterns: Right-skewed economic behavior
 *    - Session intervals: Bursty usage patterns
 *
 * Benchmark Alignment:
 * - Session frequency: Power law α ≈ 2.5
 * - Session duration: Lognormal μ=5.2, σ=0.8 (minutes)
 * - Daily active ratio: Beta distribution α=2, β=5
 * - Error rates: Gamma distribution k=2, θ=0.05
 */
public class DensityProfiler {

    private static final String LOG_TAG = "StructuralFidelity.DensityProfiler";

    private final Random random;
    private final PopulationParameters parameters;

    // Statistical distribution constants
    private static final double PARETO_ALPHA_SESSIONS = 2.5;
    private static final double PARETO_XMIN_SESSIONS = 1.0;

    private static final double LOGNORMAL_MU_DURATION = 5.2;
    private static final double LOGNORMAL_SIGMA_DURATION = 0.8;

    private static final double GAUSSIAN_MEAN_LATENCY = 250.0;
    private static final double GAUSSIAN_STDEV_LATENCY = 45.0;

    private static final double BETA_ALPHA_DAU = 2.0;
    private static final double BETA_BETA_DAU = 5.0;

    public DensityProfiler() {
        this(new PopulationParameters());
    }

    public DensityProfiler(PopulationParameters parameters) {
        this.random = new Random();
        this.parameters = parameters;
    }

    /**
     * Configurable population parameters
     */
    public static class PopulationParameters {
        // Power law parameters for session frequency
        public double paretoAlphaSessions = PARETO_ALPHA_SESSIONS;
        public double paretoXminSessions = PARETO_XMIN_SESSIONS;

        // Log-normal parameters for session duration (seconds)
        public double lognormalMuDuration = LOGNORMAL_MU_DURATION;
        public double lognormalSigmaDuration = LOGNORMAL_SIGMA_DURATION;

        // Gaussian parameters for latency (ms)
        public double gaussianMeanLatency = GAUSSIAN_MEAN_LATENCY;
        public double gaussianStdevLatency = GAUSSIAN_STDEV_LATENCY;

        // Beta parameters for daily active ratio
        public double betaAlphaDau = BETA_ALPHA_DAU;
        public double betaBetaDau = BETA_BETA_DAU;

        // Population skew factors
        public double engagementSkew = 0.8;
        public double behavioralVariance = 0.15;

        public PopulationParameters() {}

        public PopulationParameters withEngagementSkew(double skew) {
            this.engagementSkew = skew;
            return this;
        }

        public PopulationParameters withLatencyParams(double mean, double stdev) {
            this.gaussianMeanLatency = mean;
            this.gaussianStdevLatency = stdev;
            return this;
        }
    }

    /**
     * Generate session frequency using Pareto distribution
     * Aligns with 80/20 rule: few heavy users, many light users
     *
     * @return Sessions per day for a synthetic user
     */
    public int generateSessionFrequency() {
        double u = random.nextDouble();
        while (u < 0.001) u = random.nextDouble();

        double paretoSample = parameters.paretoXminSessions *
            Math.pow(u, -1.0 / parameters.paretoAlphaSessions);

        // Apply population skew adjustment
        double adjusted = paretoSample * parameters.engagementSkew;

        // Add behavioral variance
        double variance = 1.0 + (random.nextGaussian() * parameters.behavioralVariance);
        adjusted *= Math.max(0.3, variance);

        // Clamp to realistic bounds (1-50 sessions/day)
        return (int) Math.max(1, Math.min(50, Math.round(adjusted)));
    }

    /**
     * Generate session duration using Log-Normal distribution
     * Models right-skewed duration patterns (most short, some very long)
     *
     * @return Session duration in seconds
     */
    public int generateSessionDuration() {
        double normalSample = random.nextGaussian();
        double lognormalSample = Math.exp(
            parameters.lognormalMuDuration +
            (parameters.lognormalSigmaDuration * normalSample)
        );

        // Convert from minutes to seconds with variance
        double seconds = lognormalSample * 60;

        // Add tail-heavy variance
        if (random.nextDouble() < 0.15) {
            seconds *= (1.5 + random.nextDouble());
        }

        // Clamp to realistic bounds (30s - 4 hours)
        return (int) Math.max(30, Math.min(14400, Math.round(seconds)));
    }

    /**
     * Generate reaction latency using Gaussian distribution
     * Models human motor response times with realistic variance
     *
     * @return Latency in milliseconds
     */
    public long generateReactionLatency() {
        double gaussianSample = parameters.gaussianMeanLatency +
            (random.nextGaussian() * parameters.gaussianStdevLatency);

        // Add occasional slow responses (inattention, distraction)
        if (random.nextDouble() < 0.08) {
            gaussianSample *= (1.5 + random.nextDouble());
        }

        // Clamp to physiological bounds (150ms - 1000ms)
        return Math.round(Math.max(150, Math.min(1000, gaussianSample)));
    }

    /**
     * Generate daily active probability using Beta distribution
     * Models varying engagement levels across population
     *
     * @return Probability [0,1] of user being active on given day
     */
    public double generateDailyActiveProbability() {
        double alpha = parameters.betaAlphaDau;
        double beta = parameters.betaBetaDau;

        double betaSample = sampleBeta(alpha, beta);

        // Add temporal variance (weekday vs weekend patterns)
        double dayFactor = 0.9 + (random.nextDouble() * 0.2);

        return Math.min(1.0, betaSample * dayFactor);
    }

    /**
     * Generate feature adoption depth using Power Law
     * Models how deeply users engage with app features
     *
     * @return Feature utilization ratio [0,1]
     */
    public double generateFeatureAdoptionDepth() {
        double u = random.nextDouble();
        while (u < 0.001) u = random.nextDouble();

        double paretoSample = Math.pow(u, -1.0 / 1.8);
        double normalized = 1.0 / (1.0 + paretoSample * 0.3);

        return Math.min(1.0, Math.max(0.1, normalized));
    }

    /**
     * Generate content consumption rate using Gamma distribution
     * Models bursty consumption patterns
     *
     * @return Content items per session
     */
    public int generateContentConsumptionRate() {
        double k = 2.0;
        double theta = 5.0;

        double gammaSample = sampleGamma(k, theta);

        // Power law adjustment for heavy consumers
        if (random.nextDouble() < 0.2) {
            gammaSample *= (2 + random.nextDouble() * 3);
        }

        return (int) Math.max(1, Math.round(gammaSample));
    }

    /**
     * Generate error rate using Gamma distribution
     * Models decreasing error rates with skill
     *
     * @param skillLevel User skill level [0,1]
     * @return Error probability [0,1]
     */
    public double generateErrorRate(double skillLevel) {
        double k = 2.0;
        double theta = 0.05 * (1 - skillLevel * 0.8);

        double gammaSample = sampleGamma(k, theta);
        double errorRate = Math.min(0.25, gammaSample);

        return Math.max(0.005, errorRate);
    }

    /**
     * Generate inter-session interval using Exponential distribution
     * Models memoryless property of session spacing
     *
     * @param averageSessionFreq Average sessions per day
     * @return Interval in minutes
     */
    public int generateInterSessionInterval(double averageSessionFreq) {
        if (averageSessionFreq <= 0) return 720;

        double lambda = averageSessionFreq / 1440.0;
        double exponentialSample = -Math.log(1 - random.nextDouble()) / lambda;

        // Add circadian rhythm bias (longer intervals during sleep)
        double circadianFactor = 0.7 + (random.nextDouble() * 0.6);

        return (int) Math.max(15, Math.round(exponentialSample * circadianFactor));
    }

    /**
     * Generate complete population profile for batch of users
     */
    public List<UserProfile> generatePopulationProfiles(int populationSize) {
        List<UserProfile> profiles = new ArrayList<>(populationSize);

        for (int i = 0; i < populationSize; i++) {
            UserProfile profile = generateUserProfile();
            profiles.add(profile);
        }

        return profiles;
    }

    /**
     * Generate complete profile for single synthetic user
     */
    public UserProfile generateUserProfile() {
        int sessionFreq = generateSessionFrequency();
        double dailyActiveProb = generateDailyActiveProbability();
        double featureAdoption = generateFeatureAdoptionDepth();
        int contentRate = generateContentConsumptionRate();

        return new UserProfile(
            sessionFreq,
            dailyActiveProb,
            featureAdoption,
            contentRate,
            generateSessionDuration(),
            generateReactionLatency()
        );
    }

    /**
     * User profile containing distributional-aligned metrics
     */
    public static class UserProfile {
        public final int sessionsPerDay;
        public final double dailyActiveProbability;
        public final double featureAdoptionDepth;
        public final int contentItemsPerSession;
        public final int typicalSessionDuration;
        public final long typicalReactionLatency;

        public UserProfile(int sessionsPerDay, double dailyActiveProbability,
                           double featureAdoptionDepth, int contentItemsPerSession,
                           int typicalSessionDuration, long typicalReactionLatency) {
            this.sessionsPerDay = sessionsPerDay;
            this.dailyActiveProbability = dailyActiveProbability;
            this.featureAdoptionDepth = featureAdoptionDepth;
            this.contentItemsPerSession = contentItemsPerSession;
            this.typicalSessionDuration = typicalSessionDuration;
            this.typicalReactionLatency = typicalReactionLatency;
        }

        public double calculateEngagementScore() {
            return (sessionsPerDay / 10.0) * 0.3 +
                   dailyActiveProbability * 0.25 +
                   featureAdoptionDepth * 0.25 +
                   (contentItemsPerSession / 20.0) * 0.2;
        }

        @Override
        public String toString() {
            return String.format(
                "UserProfile{sessions=%d/day, dauProb=%.2f, adoption=%.2f, " +
                "content=%d, duration=%ds, latency=%dms}",
                sessionsPerDay, dailyActiveProbability, featureAdoptionDepth,
                contentItemsPerSession, typicalSessionDuration, typicalReactionLatency
            );
        }
    }

    /**
     * Population statistics summary
     */
    public PopulationStatistics calculatePopulationStatistics(List<UserProfile> profiles) {
        if (profiles.isEmpty()) {
            return new PopulationStatistics(0, 0, 0, 0, 0, 0, 0, 0);
        }

        int n = profiles.size();

        double totalSessions = 0;
        double totalDauProb = 0;
        double totalAdoption = 0;
        double totalContent = 0;
        double totalDuration = 0;
        double totalLatency = 0;

        for (UserProfile p : profiles) {
            totalSessions += p.sessionsPerDay;
            totalDauProb += p.dailyActiveProbability;
            totalAdoption += p.featureAdoptionDepth;
            totalContent += p.contentItemsPerSession;
            totalDuration += p.typicalSessionDuration;
            totalLatency += p.typicalReactionLatency;
        }

        double meanSessions = totalSessions / n;
        double meanDau = totalDauProb / n;
        double meanAdoption = totalAdoption / n;
        double meanContent = totalContent / n;
        double meanDuration = totalDuration / n;
        double meanLatency = totalLatency / n;

        double engagementGini = calculateGiniCoefficient(profiles);

        return new PopulationStatistics(
            meanSessions, meanDau, meanAdoption, meanContent,
            meanDuration, meanLatency, engagementGini, n
        );
    }

    /**
     * Calculate Gini coefficient for engagement inequality
     * 0 = perfect equality, 1 = maximum inequality
     */
    private double calculateGiniCoefficient(List<UserProfile> profiles) {
        double[] engagementScores = profiles.stream()
            .mapToDouble(UserProfile::calculateEngagementScore)
            .sorted()
            .toArray();

        int n = engagementScores.length;
        if (n < 2) return 0;

        double sum = 0;
        for (int i = 0; i < n; i++) {
            sum += engagementScores[i] * (2 * (i + 1) - n - 1);
        }

        double mean = Arrays.stream(engagementScores).average().orElse(0);
        return sum / (n * n * mean);
    }

    public static class PopulationStatistics {
        public final double meanSessionsPerDay;
        public final double meanDailyActiveProbability;
        public final double meanFeatureAdoption;
        public final double meanContentItems;
        public final double meanSessionDuration;
        public final double meanReactionLatency;
        public final double engagementGini;
        public final int populationSize;

        public PopulationStatistics(double meanSessionsPerDay, double meanDailyActiveProbability,
                                    double meanFeatureAdoption, double meanContentItems,
                                    double meanSessionDuration, double meanReactionLatency,
                                    double engagementGini, int populationSize) {
            this.meanSessionsPerDay = meanSessionsPerDay;
            this.meanDailyActiveProbability = meanDailyActiveProbability;
            this.meanFeatureAdoption = meanFeatureAdoption;
            this.meanContentItems = meanContentItems;
            this.meanSessionDuration = meanSessionDuration;
            this.meanReactionLatency = meanReactionLatency;
            this.engagementGini = engagementGini;
            this.populationSize = populationSize;
        }

        @Override
        public String toString() {
            return String.format(
                "PopulationStatistics{n=%d, sessions=%.2f/day, dau=%.2f, " +
                "adoption=%.2f, content=%.1f, duration=%.0fs, latency=%.0fms, gini=%.3f}",
                populationSize, meanSessionsPerDay, meanDailyActiveProbability,
                meanFeatureAdoption, meanContentItems, meanSessionDuration,
                meanReactionLatency, engagementGini
            );
        }
    }

    // Statistical sampling methods

    private double sampleBeta(double alpha, double beta) {
        double x = sampleGamma(alpha, 1);
        double y = sampleGamma(beta, 1);
        return x / (x + y);
    }

    private double sampleGamma(double shape, double scale) {
        if (shape < 1) {
            return sampleGamma(shape + 1, scale) * Math.pow(random.nextDouble(), 1 / shape);
        }

        double d = shape - 0.3333333333333333;
        double c = 0.3333333333333333 / Math.sqrt(d);

        while (true) {
            double x = random.nextGaussian();
            double v = 1 + c * x;
            if (v <= 0) continue;

            v = v * v * v;
            double u = random.nextDouble();

            if (u < 1 - 0.0331 * x * x * x * x) {
                return scale * d * v;
            }

            if (Math.log(u) < 0.5 * x * x + d * (1 - v + Math.log(v))) {
                return scale * d * v;
            }
        }
    }

    /**
     * Validate population aligns with benchmark distributions
     */
    public ValidationResult validateAgainstBenchmarks(List<UserProfile> profiles) {
        PopulationStatistics stats = calculatePopulationStatistics(profiles);

        List<String> warnings = new ArrayList<>();
        boolean valid = true;

        // Check 80/20 rule approximation
        double expectedTop20Engagement = 0.70;
        double actualTop20Engagement = calculateTop20EngagementShare(profiles);
        if (Math.abs(actualTop20Engagement - expectedTop20Engagement) > 0.15) {
            warnings.add(String.format(
                "Top 20%% engagement share %.2f differs from expected %.2f",
                actualTop20Engagement, expectedTop20Engagement
            ));
        }

        // Check latency bounds
        if (stats.meanReactionLatency < 200 || stats.meanReactionLatency > 400) {
            warnings.add("Mean latency outside physiological norms: " + stats.meanReactionLatency);
            valid = false;
        }

        // Check session frequency sanity
        if (stats.meanSessionsPerDay > 20) {
            warnings.add("Mean sessions per day seems high: " + stats.meanSessionsPerDay);
        }

        return new ValidationResult(valid, warnings, stats);
    }

    private double calculateTop20EngagementShare(List<UserProfile> profiles) {
        List<Double> scores = profiles.stream()
            .map(UserProfile::calculateEngagementScore)
            .sorted(Comparator.reverseOrder())
            .toList();

        int top20Count = Math.max(1, scores.size() / 5);
        double top20Sum = scores.stream().limit(top20Count).mapToDouble(Double::doubleValue).sum();
        double totalSum = scores.stream().mapToDouble(Double::doubleValue).sum();

        return totalSum > 0 ? top20Sum / totalSum : 0;
    }

    public static class ValidationResult {
        public final boolean valid;
        public final List<String> warnings;
        public final PopulationStatistics statistics;

        public ValidationResult(boolean valid, List<String> warnings, PopulationStatistics statistics) {
            this.valid = valid;
            this.warnings = warnings;
            this.statistics = statistics;
        }
    }
}
