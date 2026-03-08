package com.simulation.maturity.level4;

import java.util.Random;

/**
 * Attention Decay Function - Decision Fatigue Simulation
 * 
 * Models how interaction quality degrades as session duration increases.
 * Key behaviors:
 * - Decision quality decreases over time (decision fatigue)
 * - Response times increase with cognitive load
 * - Error rates rise after sustained mental effort
 * - Users make more mistakes when tired
 * 
 * Mathematical Model:
 * Quality(t) = Q_0 * e^(-λt) + Q_min
 * 
 * Where:
 * - Q_0 = Initial quality (1.0)
 * - λ = Decay constant (tunable)
 * - t = Session duration in minutes
 * - Q_min = Minimum quality floor (typically 0.4)
 * 
 * Device Target: Samsung Galaxy A12 (SM-A125U)
 */
public class AttentionDecayFunction {
    
    private double initialQuality;     // Starting interaction quality
    private double minQualityFloor;    // Lowest possible quality
    private double decayConstant;      // Lambda - how fast quality decays
    private long sessionStartMs;       // When current session started
    private int totalDecisions;        // Total decisions made this session
    
    // Cumulative quality tracking
    private double cumulativeQuality;
    private int qualitySamples;
    
    public AttentionDecayFunction() {
        this.initialQuality = 1.0;
        this.minQualityFloor = 0.4;
        this.decayConstant = 0.02; // 2% decay per minute
        this.sessionStartMs = System.currentTimeMillis();
        this.totalDecisions = 0;
        this.cumulativeQuality = 0;
        this.qualitySamples = 0;
    }
    
    /**
     * Start a new session - resets decay state
     */
    public void startSession() {
        this.sessionStartMs = System.currentTimeMillis();
        this.totalDecisions = 0;
    }
    
    /**
     * Calculate current interaction quality based on session duration
     * 
     * @return Quality score (0.0 - 1.0)
     */
    public double getCurrentQuality() {
        long sessionDurationMs = System.currentTimeMillis() - sessionStartMs;
        double sessionMinutes = sessionDurationMs / 60000.0;
        
        // Exponential decay model: Q = Q_0 * e^(-λt) + Q_min
        double decayedQuality = initialQuality * Math.exp(-decayConstant * sessionMinutes);
        double quality = Math.max(minQualityFloor, decayedQuality);
        
        // Record for statistics
        cumulativeQuality += quality;
        qualitySamples++;
        
        return quality;
    }
    
    /**
     * Calculate current quality using number of decisions instead of time
     * (Alternative model for decision fatigue)
     * 
     * @return Quality score (0.0 - 1.0)
     */
    public double getQualityByDecisionCount() {
        // Quality degrades faster with more decisions
        // Each decision adds 0.5% decay
        double decay = totalDecisions * 0.005;
        double quality = initialQuality * Math.exp(-decay);
        
        cumulativeQuality += quality;
        qualitySamples++;
        
        return Math.max(minQualityFloor, quality);
    }
    
    /**
     * Get adjusted response time based on current quality
     * Fatigued users think slower
     * 
     * @param baseTimeMs Base response time in milliseconds
     * @return Adjusted response time
     */
    public int getAdjustedResponseTime(int baseTimeMs) {
        double quality = getCurrentQuality();
        
        // Lower quality = slower response (up to 2x slower)
        double slowdownFactor = 1.0 + (1.0 - quality);
        
        return (int) (baseTimeMs * slowdownFactor);
    }
    
    /**
     * Calculate error probability based on current quality
     * 
     * @return Error probability (0.0 - 1.0)
     */
    public double getErrorProbability() {
        double quality = getCurrentQuality();
        
        // Error rate is inverse of quality
        // At quality=1.0, error rate = 2%
        // At quality=0.4, error rate = 15%
        return 0.02 + (1.0 - quality) * 0.13;
    }
    
    /**
     * Simulate making a decision - returns whether error occurs
     * 
     * @param random Random instance for stochastic behavior
     * @return true if error occurs
     */
    public boolean makeDecision(Random random) {
        totalDecisions++;
        
        double errorProb = getErrorProbability();
        boolean error = random.nextDouble() < errorProb;
        
        if (error) {
            // Error might trigger a pause (self-correction)
            // Could implement here if needed
        }
        
        return error;
    }
    
    /**
     * Calculate click accuracy penalty due to fatigue
     * Fatigued users have less precise motor control
     * 
     * @param baseAccuracy Base accuracy (0.0 - 1.0)
     * @return Adjusted accuracy
     */
    public double getAdjustedAccuracy(double baseAccuracy) {
        double quality = getCurrentQuality();
        
        // Fatigue reduces accuracy by up to 30%
        double accuracyPenalty = (1.0 - quality) * 0.3;
        
        return Math.max(0.1, baseAccuracy - accuracyPenalty);
    }
    
    /**
     * Get the number of decisions made in this session
     */
    public int getDecisionCount() {
        return totalDecisions;
    }
    
    /**
     * Get session duration in minutes
     */
    public double getSessionDurationMinutes() {
        long sessionDurationMs = System.currentTimeMillis() - sessionStartMs;
        return sessionDurationMs / 60000.0;
    }
    
    /**
     * Get average quality over session
     */
    public double getAverageQuality() {
        return qualitySamples > 0 ? cumulativeQuality / qualitySamples : initialQuality;
    }
    
    /**
     * Reset session-specific metrics
     */
    public void resetSession() {
        sessionStartMs = System.currentTimeMillis();
        totalDecisions = 0;
        cumulativeQuality = 0;
        qualitySamples = 0;
    }
    
    /**
     * Set decay constant
     * Higher = faster quality degradation
     */
    public void setDecayConstant(double lambda) {
        this.decayConstant = lambda;
    }
    
    /**
     * Set minimum quality floor
     */
    public void setMinQualityFloor(double floor) {
        this.minQualityFloor = Math.max(0.1, Math.min(1.0, floor));
    }
    
    /**
     * Check if user should take a break
     * Returns true when quality drops below threshold
     */
    public boolean shouldTakeBreak() {
        return getCurrentQuality() < 0.5;
    }
    
    /**
     * Get statistics for session
     */
    public DecayStatistics getStatistics() {
        return new DecayStatistics(
            getSessionDurationMinutes(),
            totalDecisions,
            getCurrentQuality(),
            getAverageQuality(),
            getErrorProbability(),
            shouldTakeBreak()
        );
    }
    
    /**
     * Statistics container
     */
    public static class DecayStatistics {
        public final double sessionMinutes;
        public final int decisionCount;
        public final double currentQuality;
        public final double averageQuality;
        public final double errorProbability;
        public final boolean needsBreak;
        
        public DecayStatistics(double sessionMinutes, int decisionCount, 
                               double currentQuality, double averageQuality,
                               double errorProbability, boolean needsBreak) {
            this.sessionMinutes = sessionMinutes;
            this.decisionCount = decisionCount;
            this.currentQuality = currentQuality;
            this.averageQuality = averageQuality;
            this.errorProbability = errorProbability;
            this.needsBreak = needsBreak;
        }
        
        @Override
        public String toString() {
            return String.format(
                "DecayStats{duration=%.1fmin, decisions=%d, quality=%.1f%%, " +
                "avgQuality=%.1f%%, errorProb=%.2f%%, needsBreak=%s}",
                sessionMinutes, decisionCount, 
                currentQuality * 100, averageQuality * 100,
                errorProbability * 100, needsBreak
            );
        }
    }
}
