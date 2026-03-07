package com.simulation.distributional;

/**
 * Quick Demonstration of Distributional Fidelity Model
 * 
 * This class demonstrates all five statistical realism hooks with simple examples.
 * Run this to see the difference between uniform and distributional approaches.
 */
public class QuickDemo {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("Distributional Fidelity Model - Quick Demo");
        System.out.println("========================================\n");
        
        // 1. Conversion Rate Variance (Binomial Distribution)
        System.out.println("1. Conversion Rate Variance (Binomial Distribution)");
        System.out.println("   Testing: Simulate 100 checkout attempts with 15% success rate");
        BinomialDistribution conversion = new BinomialDistribution(0.15);
        int successes = conversion.simulateConversions(100);
        System.out.println("   Result: " + successes + " successful conversions (expected: ~15)");
        System.out.println("   Variance: " + conversion.getVariance(100));
        System.out.println("   Std Dev: " + String.format("%.2f", conversion.getStandardDeviation(100)));
        System.out.println();
        
        // 2. Engagement Distribution (Pareto Distribution)
        System.out.println("2. Engagement Distribution (Pareto/Power Law)");
        System.out.println("   Testing: Generate engagement scores for 100 users");
        ParetoDistribution engagement = new ParetoDistribution(1.8, 5.0);
        double[] scores = engagement.samplePopulation(100);
        double powerUserCount = 0;
        double totalEngagement = 0;
        for (double score : scores) {
            if (score > 50) powerUserCount++;
            totalEngagement += score;
        }
        System.out.println("   Power users (score > 50): " + (int)powerUserCount + " (" + 
                         String.format("%.1f%%", powerUserCount) + ")");
        System.out.println("   Average engagement: " + String.format("%.2f", totalEngagement / 100));
        System.out.println("   Gini coefficient: " + String.format("%.4f", engagement.getGiniCoefficient()));
        System.out.println();
        
        // 3. Action Frequency Distribution (Poisson Process)
        System.out.println("3. Action Frequency Distribution (Poisson Process)");
        System.out.println("   Testing: Simulate events per minute for 60 minutes (λ=2.5)");
        PoissonProcess poisson = new PoissonProcess(2.5);
        int[] eventsPerMinute = poisson.sampleIntervals(60);
        int totalEvents = 0;
        int maxEvents = 0;
        for (int events : eventsPerMinute) {
            totalEvents += events;
            maxEvents = Math.max(maxEvents, events);
        }
        System.out.println("   Total events: " + totalEvents + " (expected: ~150)");
        System.out.println("   Events per minute (avg): " + String.format("%.2f", totalEvents / 60.0));
        System.out.println("   Peak minute: " + maxEvents + " events");
        System.out.println();
        
        // 4. Long-Tail Behavior Modeling (Outlier Sessions)
        System.out.println("4. Long-Tail Behavior Modeling (Outlier Sessions)");
        System.out.println("   Testing: Generate 20 sessions with 5% outlier probability");
        OutlierSessionGenerator outlierGen = new OutlierSessionGenerator(0.05, 3.0);
        int outliers = 0;
        for (int i = 0; i < 20; i++) {
            OutlierSessionGenerator.Session session = outlierGen.generateSession();
            if (session.isOutlier) outliers++;
        }
        System.out.println("   Outlier sessions: " + outliers + " (expected: ~1)");
        System.out.println("   CPU-heavy outlier example:");
        OutlierSessionGenerator.Session cpuOutlier = outlierGen.generateStressTestScenario("cpu_spike")[0];
        System.out.println("   " + cpuOutlier);
        System.out.println();
        
        // 5. Non-Uniform Sampling (Normal Distribution)
        System.out.println("5. Non-Uniform Sampling (Normal Distribution)");
        System.out.println("   Testing: Sample 100 values from N(μ=100, σ=30)");
        NormalDistribution normal = new NormalDistribution(100.0, 30.0);
        double[] samples = normal.sample(100);
        double sum = 0;
        double within1Std = 0;
        double within2Std = 0;
        for (double sample : samples) {
            sum += sample;
            if (Math.abs(normal.zScore(sample)) <= 1) within1Std++;
            if (Math.abs(normal.zScore(sample)) <= 2) within2Std++;
        }
        double mean = sum / 100;
        System.out.println("   Sample mean: " + String.format("%.2f", mean) + " (expected: 100)");
        System.out.println("   Within 1σ (68-70%): " + within1Std + "%");
        System.out.println("   Within 2σ (95%): " + within2Std + "%");
        System.out.println();
        
        // 6. Integration Test
        System.out.println("6. Full Integration Test (100 users)");
        System.out.println("   Running complete population simulation...");
        DistributionalFidelityModel model = new DistributionalFidelityModel();
        DistributionalFidelityModel.SimulationResults results = model.simulatePopulation(100);
        System.out.println("   Total users: " + results.users.length);
        System.out.println("   Conversions: " + results.totalConversions + " (" + 
                         String.format("%.1f%%", results.conversionRate * 100) + ")");
        System.out.println("   Power users: " + results.powerUserCount + " (" + 
                         String.format("%.1f%%", results.powerUserCount * 1.0) + ")");
        System.out.println("   Outlier sessions: " + results.outlierSessionCount);
        System.out.println("   Avg engagement: " + String.format("%.2f", results.averageEngagementScore));
        System.out.println("   Avg action freq: " + String.format("%.2f", results.averageActionFrequency));
        System.out.println("   Avg CPU usage: " + String.format("%.2f%%", results.totalCpuUsage / 100));
        System.out.println("   Avg memory usage: " + String.format("%.2f MB", results.totalMemoryUsage / 100));
        System.out.println();
        
        System.out.println("========================================");
        System.out.println("Demo Complete!");
        System.out.println("========================================");
        System.out.println();
        System.out.println("Key Insight:");
        System.out.println("Real populations follow probabilistic distributions,");
        System.out.println("not uniform patterns. This model captures that reality.");
        System.out.println();
        System.out.println("For full simulation, run DistributionalFidelityModel.main()");
    }
}
