package com.evolution.skill;

import com.evolution.model.InteractionMetric;
import com.evolution.model.LifecycleState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkillImprovementCurve {
    private static final Logger logger = LoggerFactory.getLogger(SkillImprovementCurve.class);

    private static final double INITIAL_NAVIGATION_LATENCY_MS = 2500.0;
    private static final double MIN_NAVIGATION_LATENCY_MS = 400.0;
    private static final double INITIAL_ERROR_RATE = 0.15;
    private static final double MIN_ERROR_RATE = 0.005;

    private final int totalDays;
    private double currentLatencyMs;
    private double currentErrorRate;
    private int sessionsCompleted;

    public SkillImprovementCurve(int totalDays) {
        this.totalDays = totalDays;
        this.currentLatencyMs = INITIAL_NAVIGATION_LATENCY_MS;
        this.currentErrorRate = INITIAL_ERROR_RATE;
        this.sessionsCompleted = 0;
    }

    public InteractionMetric getCurrentMetrics() {
        return new InteractionMetric(currentLatencyMs, currentErrorRate);
    }

    public void updateSkillForDay(int day, LifecycleState state, int sessionsToday) {
        sessionsCompleted += sessionsToday;

        double learningRate = calculateLearningRate(state, day);
        double experienceFactor = calculateExperienceFactor(day);

        updateNavigationLatency(learningRate, experienceFactor);
        updateErrorRate(learningRate, experienceFactor);

        logger.debug("Day {}: Latency={:.2f}ms, ErrorRate={:.4f} (learningRate={:.4f}, experienceFactor={:.4f})",
                day, currentLatencyMs, currentErrorRate, learningRate, experienceFactor);
    }

    private double calculateLearningRate(LifecycleState state, int day) {
        switch (state) {
            case ONBOARDING:
                return 0.25;
            case EXPLORER:
                return 0.15;
            case UTILITY:
                return 0.08;
            case POWER_USER:
                return 0.03;
            default:
                return 0.05;
        }
    }

    private double calculateExperienceFactor(int day) {
        double experienceProgress = (double) day / totalDays;
        return 1.0 - Math.exp(-3.0 * experienceProgress);
    }

    private void updateNavigationLatency(double learningRate, double experienceFactor) {
        double targetLatency = MIN_NAVIGATION_LATENCY_MS +
                (INITIAL_NAVIGATION_LATENCY_MS - MIN_NAVIGATION_LATENCY_MS) * (1.0 - experienceFactor);
        
        currentLatencyMs = currentLatencyMs +
                (targetLatency - currentLatencyMs) * learningRate;

        currentLatencyMs = Math.max(currentLatencyMs, MIN_NAVIGATION_LATENCY_MS);
    }

    private void updateErrorRate(double learningRate, double experienceFactor) {
        double targetErrorRate = MIN_ERROR_RATE +
                (INITIAL_ERROR_RATE - MIN_ERROR_RATE) * (1.0 - experienceFactor);

        currentErrorRate = currentErrorRate +
                (targetErrorRate - currentErrorRate) * learningRate;

        currentErrorRate = Math.max(currentErrorRate, MIN_ERROR_RATE);
    }

    public void applyLatencySpike(double spikeMultiplier) {
        double increasedLatency = currentLatencyMs * spikeMultiplier;
        logger.warn("Latency spike applied: {:.2f}ms -> {:.2f}ms (multiplier: {:.2f})",
                currentLatencyMs, increasedLatency, spikeMultiplier);
        currentLatencyMs = increasedLatency;
    }

    public void recoverFromLatencySpike(double recoveryRate) {
        double baselineLatency = MIN_NAVIGATION_LATENCY_MS +
                (INITIAL_NAVIGATION_LATENCY_MS - MIN_NAVIGATION_LATENCY_MS) * 
                (1.0 - calculateExperienceFactor(totalDays));
        
        currentLatencyMs = currentLatencyMs +
                (baselineLatency - currentLatencyMs) * recoveryRate;
        
        logger.debug("Recovering from latency spike: {:.2f}ms", currentLatencyMs);
    }

    public double getSkillProficiencyScore() {
        double latencyScore = 1.0 - ((currentLatencyMs - MIN_NAVIGATION_LATENCY_MS) /
                (INITIAL_NAVIGATION_LATENCY_MS - MIN_NAVIGATION_LATENCY_MS));
        double errorScore = 1.0 - ((currentErrorRate - MIN_ERROR_RATE) /
                (INITIAL_ERROR_RATE - MIN_ERROR_RATE));
        
        return (latencyScore + errorScore) / 2.0;
    }
}
