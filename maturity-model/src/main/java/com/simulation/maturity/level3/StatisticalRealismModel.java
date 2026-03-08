package com.simulation.maturity.level3;

/**
 * Level 3: Statistical Realism Integration
 * 
 * Combines all three statistical components:
 * 1. Log-normal distributions for Inter-Arrival Times
 * 2. Gaussian Blur for UI coordinate interactions
 * 3. Pareto distributions for session frequency
 * 
 * This module provides unified API for generating human-like behavioral patterns
 * that exhibit non-random, structured population dynamics.
 * 
 * Device Target: Samsung Galaxy A12 (SM-A125U)
 */
public class StatisticalRealismModel {
    
    private final LogNormalDistribution interactionTiming;
    private final LogNormalDistribution sessionGapTiming;
    private final GaussianBlur touchBlur;
    private final SessionFrequencyDistribution sessionFrequency;
    
    // Configuration
    private boolean enableFatigueEffects = true;
    private double fatigueLevel = 0.0;
    
    public StatisticalRealismModel() {
        // Default configurations for SM-A125U
        this.interactionTiming = LogNormalDistribution.forUIInteraction();
        this.sessionGapTiming = LogNormalDistribution.forSessionGap();
        this.touchBlur = GaussianBlur.forSmartphone();
        this.sessionFrequency = SessionFrequencyDistribution.forBudgetDevice();
    }
    
    /**
     * Generate realistic inter-arrival time between interactions
     */
    public long getInterArrivalTimeMs() {
        long baseTime = interactionTiming.sampleMs();
        
        // Apply fatigue factor
        if (enableFatigueEffects && fatigueLevel > 0) {
            // Fatigued users have more variable timing
            baseTime = (long) (baseTime * (1.0 + fatigueLevel * 0.5));
        }
        
        return baseTime;
    }
    
    /**
     * Generate realistic session gap time
     */
    public long getSessionGapMs() {
        return sessionGapTiming.sampleMs();
    }
    
    /**
     * Apply motor noise to touch coordinates
     */
    public float[] applyTouchNoise(float targetX, float targetY, float velocity, float targetSize) {
        touchBlur.setFatigueFactor(fatigueLevel);
        return touchBlur.applyNoise(targetX, targetY, velocity, targetSize);
    }
    
    /**
     * Check if touch results in mis-click
     */
    public boolean isMisClick(float targetX, float targetY, 
                               float targetWidth, float targetHeight,
                               float touchX, float touchY) {
        return touchBlur.isMisClick(targetX, targetY, targetWidth, targetHeight, touchX, touchY);
    }
    
    /**
     * Generate session count for population
     */
    public int[] generatePopulationSessions(int populationSize) {
        return sessionFrequency.samplePopulation(populationSize);
    }
    
    /**
     * Segment population into power users and casual users
     */
    public SessionFrequencyDistribution.PopulationSegmentation segmentPopulation(int totalUsers, int threshold) {
        return sessionFrequency.segmentPopulation(totalUsers, threshold);
    }
    
    /**
     * Set fatigue level (0.0 = fresh, 1.0 = fully fatigued)
     */
    public void setFatigueLevel(double level) {
        this.fatigueLevel = Math.max(0.0, Math.min(1.0, level));
    }
    
    /**
     * Enable or disable fatigue effects
     */
    public void setEnableFatigueEffects(boolean enable) {
        this.enableFatigueEffects = enable;
    }
    
    /**
     * Get current Gini coefficient for session distribution
     */
    public double getInequalityIndex() {
        return sessionFrequency.getGiniCoefficient();
    }
    
    /**
     * Generate complete behavioral snapshot for one user
     */
    public UserBehaviorSnapshot generateUserSnapshot(int userId) {
        int sessions = sessionFrequency.sampleInt();
        
        // Generate interaction pattern
        long[] interactionTimes = new long[sessions * 5]; // 5 interactions per session avg
        for (int i = 0; i < interactionTimes.length; i++) {
            interactionTimes[i] = getInterArrivalTimeMs();
        }
        
        return new UserBehaviorSnapshot(
            userId,
            sessions,
            interactionTimes,
            sessionFrequency.getExpectedValue()
        );
    }
    
    /**
     * Snapshot of generated user behavior
     */
    public static class UserBehaviorSnapshot {
        public final int userId;
        public final int sessionCount;
        public final long[] interactionIntervals;
        public final double expectedSessions;
        
        public UserBehaviorSnapshot(int userId, int sessionCount, 
                                    long[] interactionIntervals, double expectedSessions) {
            this.userId = userId;
            this.sessionCount = sessionCount;
            this.interactionIntervals = interactionIntervals;
            this.expectedSessions = expectedSessions;
        }
        
        public boolean isPowerUser(int threshold) {
            return sessionCount >= threshold;
        }
        
        public double getEngagementRatio() {
            return expectedSessions > 0 ? sessionCount / expectedSessions : 0;
        }
    }
    
    /**
     * Run demonstration of statistical realism
     */
    public static void main(String[] args) {
        System.out.println("=== Level 3: Statistical Realism Demo ===\n");
        
        StatisticalRealismModel model = new StatisticalRealismModel();
        
        // Demo 1: Inter-arrival times
        System.out.println("--- Inter-Arrival Times (10 samples) ---");
        for (int i = 0; i < 10; i++) {
            System.out.printf("  Sample %d: %d ms%n", i+1, model.getInterArrivalTimeMs());
        }
        
        // Demo 2: Touch coordinates with Gaussian blur
        System.out.println("\n--- Touch Coordinates with Motor Noise ---");
        float targetX = 360f, targetY = 800f; // Center of SM-A125U screen
        float velocity = 0.5f; // Normal speed
        float targetSize = 48f; // Material Design touch target size
        
        for (int i = 0; i < 5; i++) {
            float[] touched = model.applyTouchNoise(targetX, targetY, velocity, targetSize);
            double accuracy = model.getClass().getDeclaredMethods(); // Placeholder
            System.out.printf("  Touch %d: intended=(%.1f,%.1f) -> actual=(%.1f,%.1f)%n", 
                i+1, targetX, targetY, touched[0], touched[1]);
        }
        
        // Demo 3: Session frequency (80/20 rule)
        System.out.println("\n--- Session Frequency (80/20 Distribution) ---");
        SessionFrequencyDistribution.PopulationSegmentation seg = 
            model.segmentPopulation(1000, 10); // 10+ sessions = power user
        System.out.println("  " + seg);
        System.out.printf("  Power users (10+ sessions): %d (%.1f%%)%n", 
            seg.powerUserCount, seg.getPowerUserPercentage());
        
        // Demo 4: Fatigue effects
        System.out.println("\n--- Fatigue Effects on Timing ---");
        model.setFatigueLevel(0.0);
        System.out.printf("  Fresh user IAT: %d ms%n", model.getInterArrivalTimeMs());
        model.setFatigueLevel(0.5);
        System.out.printf("  Fatigued user IAT: %d ms%n", model.getInterArrivalTimeMs());
        model.setFatigueLevel(0.0); // Reset
        
        // Demo 5: Gini coefficient
        System.out.println("\n--- Inequality Index ---");
        System.out.printf("  Gini coefficient: %.3f (higher = more power-user skewed)%n", 
            model.getInequalityIndex());
        
        System.out.println("\n=== Demo Complete ===");
    }
}
