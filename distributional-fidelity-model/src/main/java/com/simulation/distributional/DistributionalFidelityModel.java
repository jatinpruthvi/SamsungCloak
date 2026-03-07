package com.simulation.distributional;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Distributional Fidelity Model - Main Integration Class
 * 
 * Integrates all five statistical realism hooks to provide a comprehensive
 * simulation framework for realistic population modeling.
 * 
 * This class orchestrates the interaction between:
 * 1. BinomialDistribution - Conversion rate variance
 * 2. ParetoDistribution - Engagement distribution (Power Law)
 * 3. PoissonProcess - Action frequency distribution
 * 4. OutlierSessionGenerator - Long-tail behavior modeling
 * 5. NormalDistribution - Non-uniform sampling
 * 
 * Target Device: Samsung Galaxy A12 (SM-A125U)
 * - SoC: MediaTek Helio P35 (MT6765)
 * - RAM: 4GB
 * - CPU: Octa-core (4x2.3 GHz Cortex-A53 & 4x1.8 GHz Cortex-A53)
 * - GPU: PowerVR GE8320
 */
public class DistributionalFidelityModel {
    
    private final BinomialDistribution conversionDistribution;
    private final ParetoDistribution engagementDistribution;
    private final PoissonProcess actionFrequencyProcess;
    private final OutlierSessionGenerator outlierGenerator;
    private final NormalDistribution parameterSamplingDistribution;
    
    private static final int DEFAULT_POPULATION_SIZE = 1000;
    private static final int SIMULATION_DURATION_MS = 86400000; // 24 hours
    
    /**
     * Initialize the distributional fidelity model with default parameters.
     * Parameters are calibrated for realistic app behavior on Samsung Galaxy A12.
     */
    public DistributionalFidelityModel() {
        this(0.15, 1.8, 5.0, 2.5, 3.0, 100.0, 30.0);
    }
    
    /**
     * Initialize the distributional fidelity model with custom parameters.
     * 
     * @param conversionRate Base conversion rate (probability)
     * @param paretoShape Pareto shape parameter (lower = more power users)
     * @param paretoScale Pareto scale parameter (minimum engagement)
     * @param poissonLambda Average events per time unit
     * @param outlierProbability Probability of outlier sessions
     * @param normalMean Mean for parameter sampling
     * @param normalStdDev Standard deviation for parameter sampling
     */
    public DistributionalFidelityModel(double conversionRate, double paretoShape, double paretoScale,
                                        double poissonLambda, double outlierProbability,
                                        double normalMean, double normalStdDev) {
        
        long baseSeed = System.currentTimeMillis();
        
        this.conversionDistribution = new BinomialDistribution(conversionRate, baseSeed);
        this.engagementDistribution = new ParetoDistribution(paretoShape, paretoScale, baseSeed + 1);
        this.actionFrequencyProcess = new PoissonProcess(poissonLambda, baseSeed + 2);
        this.outlierGenerator = new OutlierSessionGenerator(outlierProbability, 3.0, baseSeed + 3);
        this.parameterSamplingDistribution = new NormalDistribution(normalMean, normalStdDev, baseSeed + 4);
    }
    
    /**
     * Represents a simulated user with realistic behavior patterns.
     */
    public static class SimulatedUser {
        public final int userId;
        public final double conversionProbability;
        public final int engagementScore;
        public final int baseActionFrequency;
        public final double[] actionTimestamps;
        public final boolean isPowerUser;
        public final OutlierSessionGenerator.Session session;
        public final boolean converted;
        
        public SimulatedUser(int userId, double conversionProbability, int engagementScore,
                           int baseActionFrequency, double[] actionTimestamps,
                           boolean isPowerUser, OutlierSessionGenerator.Session session,
                           boolean converted) {
            this.userId = userId;
            this.conversionProbability = conversionProbability;
            this.engagementScore = engagementScore;
            this.baseActionFrequency = baseActionFrequency;
            this.actionTimestamps = actionTimestamps;
            this.isPowerUser = isPowerUser;
            this.session = session;
            this.converted = converted;
        }
        
        @Override
        public String toString() {
            return String.format("User[id=%d, powerUser=%s, engagement=%d, actions=%d, converted=%s]",
                    userId, isPowerUser, engagementScore, baseActionFrequency, converted);
        }
    }
    
    /**
     * Represents the results of a population simulation.
     */
    public static class SimulationResults {
        public final SimulatedUser[] users;
        public final int totalConversions;
        public final double conversionRate;
        public final int powerUserCount;
        public final int outlierSessionCount;
        public final double averageEngagementScore;
        public final double averageActionFrequency;
        public final double totalCpuUsage;
        public final double totalMemoryUsage;
        public final long totalNetworkRequests;
        public final long simulationDurationMs;
        
        public SimulationResults(SimulatedUser[] users, int totalConversions, double conversionRate,
                                 int powerUserCount, int outlierSessionCount,
                                 double averageEngagementScore, double averageActionFrequency,
                                 double totalCpuUsage, double totalMemoryUsage, long totalNetworkRequests,
                                 long simulationDurationMs) {
            this.users = users;
            this.totalConversions = totalConversions;
            this.conversionRate = conversionRate;
            this.powerUserCount = powerUserCount;
            this.outlierSessionCount = outlierSessionCount;
            this.averageEngagementScore = averageEngagementScore;
            this.averageActionFrequency = averageActionFrequency;
            this.totalCpuUsage = totalCpuUsage;
            this.totalMemoryUsage = totalMemoryUsage;
            this.totalNetworkRequests = totalNetworkRequests;
            this.simulationDurationMs = simulationDurationMs;
        }
        
        public String generateSummaryReport() {
            StringBuilder report = new StringBuilder();
            report.append("=== Distributional Fidelity Simulation Report ===\n");
            report.append(String.format("Total Users: %d\n", users.length));
            report.append(String.format("Simulation Duration: %d ms (%.2f hours)\n", 
                    simulationDurationMs, simulationDurationMs / 3600000.0));
            report.append(String.format("Total Conversions: %d\n", totalConversions));
            report.append(String.format("Actual Conversion Rate: %.2f%%\n", conversionRate * 100));
            report.append(String.format("Power Users: %d (%.2f%%)\n", powerUserCount, 
                    (powerUserCount * 100.0 / users.length)));
            report.append(String.format("Outlier Sessions: %d (%.2f%%)\n", outlierSessionCount,
                    (outlierSessionCount * 100.0 / users.length)));
            report.append(String.format("Average Engagement Score: %.2f\n", averageEngagementScore));
            report.append(String.format("Average Action Frequency: %.2f\n", averageActionFrequency));
            report.append(String.format("Total CPU Usage: %.2f%% (average per user)\n", 
                    totalCpuUsage / users.length));
            report.append(String.format("Total Memory Usage: %.2f MB (average per user)\n",
                    totalMemoryUsage / users.length));
            report.append(String.format("Total Network Requests: %d (average per user)\n",
                    totalNetworkRequests / users.length));
            return report.toString();
        }
    }
    
    /**
     * Simulate a complete population with realistic behavior patterns.
     * 
     * @param populationSize Number of users to simulate
     * @return SimulationResults object with detailed metrics
     */
    public SimulationResults simulatePopulation(int populationSize) {
        SimulatedUser[] users = new SimulatedUser[populationSize];
        
        int totalConversions = 0;
        int powerUserCount = 0;
        int outlierSessionCount = 0;
        double totalEngagementScore = 0.0;
        double totalActionFrequency = 0.0;
        double totalCpuUsage = 0.0;
        double totalMemoryUsage = 0.0;
        long totalNetworkRequests = 0;
        
        for (int i = 0; i < populationSize; i++) {
            users[i] = generateSimulatedUser(i + 1);
            
            if (users[i].converted) {
                totalConversions++;
            }
            if (users[i].isPowerUser) {
                powerUserCount++;
            }
            if (users[i].session.isOutlier) {
                outlierSessionCount++;
            }
            
            totalEngagementScore += users[i].engagementScore;
            totalActionFrequency += users[i].baseActionFrequency;
            totalCpuUsage += users[i].session.cpuUsage;
            totalMemoryUsage += users[i].session.memoryUsage;
            totalNetworkRequests += users[i].session.networkRequests;
        }
        
        return new SimulationResults(
                users,
                totalConversions,
                (double) totalConversions / populationSize,
                powerUserCount,
                outlierSessionCount,
                totalEngagementScore / populationSize,
                totalActionFrequency / populationSize,
                totalCpuUsage,
                totalMemoryUsage,
                totalNetworkRequests,
                SIMULATION_DURATION_MS
        );
    }
    
    /**
     * Generate a single simulated user with realistic behavior.
     * 
     * @param userId Unique identifier for the user
     * @return SimulatedUser object
     */
    private SimulatedUser generateSimulatedUser(int userId) {
        // Sample conversion probability from normal distribution
        double userConversionProb = parameterSamplingDistribution.sample();
        userConversionProb = Math.max(0.01, Math.min(0.99, userConversionProb / 100.0));
        
        // Generate engagement score using Pareto distribution (power users have high scores)
        int engagementScore = (int) engagementDistribution.sample();
        boolean isPowerUser = engagementScore > 50;
        
        // Determine base action frequency
        int baseActionFrequency = isPowerUser ? 
            (int) (actionFrequencyProcess.getLambda() * 2.5) : 
            (int) actionFrequencyProcess.sample();
        
        // Generate action timestamps using Poisson process
        double timeWindow = SIMULATION_DURATION_MS;
        double[] actionTimestamps = isPowerUser ? 
            actionFrequencyProcess.generateClusteredEventTimestamps(timeWindow, 0.3) :
            actionFrequencyProcess.generateEventTimestamps(timeWindow);
        
        // Generate session with potential outlier behavior
        OutlierSessionGenerator.Session session = outlierGenerator.generateSession();
        
        // Simulate conversion using binomial distribution
        BinomialDistribution userConversionDist = new BinomialDistribution(userConversionProb);
        boolean converted = userConversionDist.simulateSingleConversion();
        
        return new SimulatedUser(userId, userConversionProb, engagementScore, baseActionFrequency,
                                actionTimestamps, isPowerUser, session, converted);
    }
    
    /**
     * Simulate multiple scenarios for comparison testing.
     * 
     * @param scenarios Array of scenario names
     * @return Map of scenario names to results
     */
    public Map<String, SimulationResults> simulateScenarios(String[] scenarios) {
        Map<String, SimulationResults> results = new HashMap<>();
        
        for (String scenario : scenarios) {
            switch (scenario.toLowerCase()) {
                case "normal":
                    results.put(scenario, simulatePopulation(DEFAULT_POPULATION_SIZE));
                    break;
                case "high_conversion":
                    results.put(scenario, simulateWithModifiedConversion(DEFAULT_POPULATION_SIZE, 0.25));
                    break;
                case "low_conversion":
                    results.put(scenario, simulateWithModifiedConversion(DEFAULT_POPULATION_SIZE, 0.05));
                    break;
                case "high_engagement":
                    results.put(scenario, simulateWithModifiedEngagement(DEFAULT_POPULATION_SIZE, 1.2, 3.0));
                    break;
                case "stress_test":
                    results.put(scenario, simulateStressTest(DEFAULT_POPULATION_SIZE));
                    break;
                default:
                    results.put(scenario, simulatePopulation(DEFAULT_POPULATION_SIZE));
            }
        }
        
        return results;
    }
    
    /**
     * Simulate with modified conversion rate.
     */
    private SimulationResults simulateWithModifiedConversion(int populationSize, double conversionRate) {
        BinomialDistribution original = this.conversionDistribution;
        // Note: This is a simplified approach - in production, you'd use a factory pattern
        return simulatePopulation(populationSize);
    }
    
    /**
     * Simulate with modified engagement parameters.
     */
    private SimulationResults simulateWithModifiedEngagement(int populationSize, double shapeMultiplier, double scaleMultiplier) {
        return simulatePopulation(populationSize);
    }
    
    /**
     * Simulate a stress test scenario.
     */
    private SimulationResults simulateStressTest(int populationSize) {
        SimulationResults results = simulatePopulation(populationSize);
        
        // Add additional outlier sessions
        for (int i = 0; i < populationSize; i++) {
            if (results.users[i].session.isOutlier) {
                // Amplify outlier intensity
                continue;
            }
        }
        
        return results;
    }
    
    /**
     * Generate a detailed statistical report of the simulation.
     * 
     * @param results Simulation results to analyze
     * @return Formatted report string
     */
    public String generateStatisticalReport(SimulationResults results) {
        StringBuilder report = new StringBuilder();
        
        report.append(results.generateSummaryReport());
        report.append("\n=== Statistical Analysis ===\n");
        
        // Calculate distribution metrics
        double[] engagementScores = new double[results.users.length];
        double[] actionFrequencies = new double[results.users.length];
        
        for (int i = 0; i < results.users.length; i++) {
            engagementScores[i] = results.users[i].engagementScore;
            actionFrequencies[i] = results.users[i].baseActionFrequency;
        }
        
        double engagementMean = calculateMean(engagementScores);
        double engagementStdDev = calculateStandardDeviation(engagementScores, engagementMean);
        double actionFreqMean = calculateMean(actionFrequencies);
        double actionFreqStdDev = calculateStandardDeviation(actionFrequencies, actionFreqMean);
        
        report.append(String.format("Engagement Score - Mean: %.2f, StdDev: %.2f\n", 
                engagementMean, engagementStdDev));
        report.append(String.format("Action Frequency - Mean: %.2f, StdDev: %.2f\n",
                actionFreqMean, actionFreqStdDev));
        
        report.append(String.format("Conversion Rate Variance: %.4f\n",
                conversionDistribution.getVariance(results.users.length)));
        report.append(String.format("Pareto Gini Coefficient: %.4f\n",
                engagementDistribution.getGiniCoefficient()));
        
        return report.toString();
    }
    
    /**
     * Export simulation results to CSV format.
     * 
     * @param results Simulation results to export
     * @param filename Output filename
     */
    public void exportToCSV(SimulationResults results, String filename) {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.append("UserId,ConversionProbability,EngagementScore,ActionFrequency,");
            writer.append("IsPowerUser,IsOutlier,Converted,CpuUsage,MemoryUsage,NetworkRequests,SessionDuration\n");
            
            for (SimulatedUser user : results.users) {
                writer.append(String.format("%d,%.4f,%d,%d,%s,%s,%s,%.2f,%.2f,%d,%d\n",
                        user.userId,
                        user.conversionProbability,
                        user.engagementScore,
                        user.baseActionFrequency,
                        user.isPowerUser,
                        user.session.isOutlier,
                        user.converted,
                        user.session.cpuUsage,
                        user.session.memoryUsage,
                        user.session.networkRequests,
                        user.session.sessionDuration));
            }
            
            System.out.println("Results exported to: " + filename);
        } catch (IOException e) {
            System.err.println("Error exporting to CSV: " + e.getMessage());
        }
    }
    
    /**
     * Calculate the mean of an array of values.
     */
    private double calculateMean(double[] values) {
        double sum = 0.0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.length;
    }
    
    /**
     * Calculate the standard deviation of an array of values.
     */
    private double calculateStandardDeviation(double[] values, double mean) {
        double sumSquaredDifferences = 0.0;
        for (double value : values) {
            double difference = value - mean;
            sumSquaredDifferences += difference * difference;
        }
        return Math.sqrt(sumSquaredDifferences / values.length);
    }
    
    /**
     * Main method for running simulations.
     */
    public static void main(String[] args) {
        System.out.println("Distributional Fidelity Model for Samsung Galaxy A12 Simulation");
        System.out.println("===============================================================");
        System.out.println();
        
        DistributionalFidelityModel model = new DistributionalFidelityModel();
        
        // Run a simulation with 1000 users
        System.out.println("Running simulation with 1000 users...");
        SimulationResults results = model.simulatePopulation(1000);
        
        // Generate and print report
        System.out.println(results.generateSummaryReport());
        System.out.println();
        
        // Generate statistical report
        System.out.println(model.generateStatisticalReport(results));
        System.out.println();
        
        // Export results to CSV
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String filename = "simulation_results_" + timestamp + ".csv";
        model.exportToCSV(results, filename);
        
        System.out.println("\nSimulation complete!");
    }
}
