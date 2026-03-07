package com.evolution.churn;

import com.evolution.model.LifecycleState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ChurnModel {
    private static final Logger logger = LoggerFactory.getLogger(ChurnModel.class);

    private double churnProbability;
    private final List<ChurnTrigger> churnTriggers;
    private final List<RetentionFactor> retentionFactors;
    private int consecutiveLowEngagementDays;
    private int consecutiveHighLatencyDays;
    private boolean hasChurned;

    private static final double BASE_CHURN_PROBABILITY = 0.001;
    private static final double HIGH_LATENCY_THRESHOLD_MS = 5000.0;
    private static final double ERROR_RATE_THRESHOLD = 0.10;
    private static final int MAX_CONSECUTIVE_LOW_ENGAGEMENT = 5;
    private static final int MAX_CONSECUTIVE_HIGH_LATENCY = 3;

    public ChurnModel() {
        this.churnProbability = BASE_CHURN_PROBABILITY;
        this.churnTriggers = new ArrayList<>();
        this.retentionFactors = new ArrayList<>();
        this.consecutiveLowEngagementDays = 0;
        this.consecutiveHighLatencyDays = 0;
        this.hasChurned = false;
    }

    public void updateChurnProbability(int day, LifecycleState state, double navigationLatencyMs,
                                       double errorRate, int sessionsToday, double avgSessionDuration) {
        if (hasChurned) {
            return;
        }

        resetTriggers();
        applyBaseProbability(day, state);
        applyLatencyImpact(navigationLatencyMs);
        applyErrorRateImpact(errorRate);
        applyEngagementImpact(sessionsToday, avgSessionDuration);
        applyLifecycleMilestoneImpact(day, state);

        churnProbability = calculateFinalProbability();
        churnProbability = Math.min(Math.max(churnProbability, 0.0), 1.0);

        logger.debug("Day {}: Churn probability updated to {:.4f} (triggers: {}, factors: {})",
                day, churnProbability, churnTriggers.size(), retentionFactors.size());
    }

    private void resetTriggers() {
        churnTriggers.clear();
        retentionFactors.clear();
    }

    private void applyBaseProbability(int day, LifecycleState state) {
        double lifecycleModifier = switch (state) {
            case ONBOARDING -> 0.5;
            case EXPLORER -> 1.0;
            case UTILITY -> 0.8;
            case POWER_USER -> 0.3;
            default -> 1.0;
        };

        churnProbability = BASE_CHURN_PROBABILITY * lifecycleModifier;
    }

    private void applyLatencyImpact(double navigationLatencyMs) {
        if (navigationLatencyMs > HIGH_LATENCY_THRESHOLD_MS) {
            consecutiveHighLatencyDays++;
            double latencySeverity = Math.min((navigationLatencyMs - HIGH_LATENCY_THRESHOLD_MS) / 5000.0, 1.0);
            double churnIncrease = 0.05 * latencySeverity * (1 + consecutiveHighLatencyDays * 0.5);
            
            churnTriggers.add(new ChurnTrigger("HIGH_LATENCY", churnIncrease, 
                    String.format("Latency %.0fms > threshold %.0fms", 
                            navigationLatencyMs, HIGH_LATENCY_THRESHOLD_MS)));
        } else {
            consecutiveHighLatencyDays = Math.max(0, consecutiveHighLatencyDays - 1);
        }
    }

    private void applyErrorRateImpact(double errorRate) {
        if (errorRate > ERROR_RATE_THRESHOLD) {
            double errorSeverity = Math.min((errorRate - ERROR_RATE_THRESHOLD) / ERROR_RATE_THRESHOLD, 1.0);
            double churnIncrease = 0.08 * errorSeverity;
            
            churnTriggers.add(new ChurnTrigger("HIGH_ERROR_RATE", churnIncrease,
                    String.format("Error rate %.4f > threshold %.4f", errorRate, ERROR_RATE_THRESHOLD)));
        }
    }

    private void applyEngagementImpact(int sessionsToday, double avgSessionDuration) {
        if (sessionsToday == 0 || avgSessionDuration < 30.0) {
            consecutiveLowEngagementDays++;
            
            if (consecutiveLowEngagementDays >= MAX_CONSECUTIVE_LOW_ENGAGEMENT) {
                double disengagementSeverity = (double) consecutiveLowEngagementDays / MAX_CONSECUTIVE_LOW_ENGAGEMENT;
                double churnIncrease = 0.10 * disengagementSeverity;
                
                churnTriggers.add(new ChurnTrigger("LOW_ENGAGEMENT", churnIncrease,
                        String.format("%d consecutive low engagement days", consecutiveLowEngagementDays)));
            }
        } else if (sessionsToday >= 5 && avgSessionDuration > 180.0) {
            consecutiveLowEngagementDays = 0;
            retentionFactors.add(new RetentionFactor("HIGH_ENGAGEMENT", -0.02,
                    "Strong engagement observed"));
        } else {
            consecutiveLowEngagementDays = Math.max(0, consecutiveLowEngagementDays - 1);
        }
    }

    private void applyLifecycleMilestoneImpact(int day, LifecycleState state) {
        if (state.isTransitionDay(day)) {
            LifecycleState previousState = LifecycleState.getStateForDay(day - 1);
            
            switch (previousState) {
                case ONBOARDING:
                    retentionFactors.add(new RetentionFactor("ONBOARDING_COMPLETE", -0.03,
                            "User completed onboarding"));
                    break;
                case EXPLORER:
                    retentionFactors.add(new RetentionFactor("EXPLORATION_COMPLETE", -0.05,
                            "User transitioned to utility phase"));
                    break;
                case UTILITY:
                    retentionFactors.add(new RetentionFactor("UTILITY_MASTERY", -0.07,
                            "User achieved power user status"));
                    break;
                default:
                    break;
            }
        }

        if (day >= 30 && day < 35) {
            churnTriggers.add(new ChurnTrigger("30_DAY_CHURN_POINT", 0.02,
                    "Critical 30-day churn window"));
        }
        
        if (day >= 60 && day < 65) {
            retentionFactors.add(new RetentionFactor("60_DAY_RETENTION_BONUS", -0.04,
                    "60-day retention milestone achieved"));
        }
    }

    private double calculateFinalProbability() {
        double finalProbability = churnProbability;

        for (ChurnTrigger trigger : churnTriggers) {
            finalProbability += trigger.getImpact();
        }

        for (RetentionFactor factor : retentionFactors) {
            finalProbability += factor.getImpact();
        }

        return finalProbability;
    }

    public boolean evaluateChurn() {
        if (hasChurned) {
            return true;
        }

        double randomValue = Math.random();
        boolean shouldChurn = randomValue < churnProbability;

        if (shouldChurn) {
            hasChurned = true;
            logger.warn("CHURN EVENT: Probability {:.4f} triggered churn", churnProbability);
            logChurnContext();
        }

        return shouldChurn;
    }

    public boolean hasChurned() {
        return hasChurned;
    }

    public double getChurnProbability() {
        return churnProbability;
    }

    public List<ChurnTrigger> getActiveTriggers() {
        return new ArrayList<>(churnTriggers);
    }

    public List<RetentionFactor> getActiveFactors() {
        return new ArrayList<>(retentionFactors);
    }

    private void logChurnContext() {
        logger.warn("=== CHURN CONTEXT ===");
        logger.warn("Triggers:");
        for (ChurnTrigger trigger : churnTriggers) {
            logger.warn("  - {}: {:.4f} ({})", trigger.getType(), trigger.getImpact(), trigger.getDescription());
        }
        logger.warn("Retention Factors:");
        for (RetentionFactor factor : retentionFactors) {
            logger.warn("  - {}: {:.4f} ({})", factor.getType(), factor.getImpact(), factor.getDescription());
        }
        logger.warn("Consecutive Low Engagement Days: {}", consecutiveLowEngagementDays);
        logger.warn("Consecutive High Latency Days: {}", consecutiveHighLatencyDays);
        logger.warn("=====================");
    }

    public void recordLatencySpike(double spikeMultiplier) {
        double spikeImpact = 0.05 * (spikeMultiplier - 1.0);
        churnTriggers.add(new ChurnTrigger("LATENCY_SPIKE", spikeImpact,
                String.format("Latency spike %.2fx", spikeMultiplier)));
    }

    public void recordPositiveExperience() {
        retentionFactors.add(new RetentionFactor("POSITIVE_EXPERIENCE", -0.01,
                "Recent positive user experience"));
    }

    public void resetChurnState() {
        this.churnProbability = BASE_CHURN_PROBABILITY;
        this.hasChurned = false;
        this.consecutiveLowEngagementDays = 0;
        this.consecutiveHighLatencyDays = 0;
        logger.info("Churn state reset");
    }

    public static class ChurnTrigger {
        private final String type;
        private final double impact;
        private final String description;

        public ChurnTrigger(String type, double impact, String description) {
            this.type = type;
            this.impact = impact;
            this.description = description;
        }

        public String getType() {
            return type;
        }

        public double getImpact() {
            return impact;
        }

        public String getDescription() {
            return description;
        }
    }

    public static class RetentionFactor {
        private final String type;
        private final double impact;
        private final String description;

        public RetentionFactor(String type, double impact, String description) {
            this.type = type;
            this.impact = impact;
            this.description = description;
        }

        public String getType() {
            return type;
        }

        public double getImpact() {
            return impact;
        }

        public String getDescription() {
            return description;
        }
    }
}
