package com.evolution.lifecycle;

import com.evolution.churn.ChurnModel;
import com.evolution.interest.InterestDriftModel;
import com.evolution.interest.MarkovTransitionMatrix;
import com.evolution.model.AppModule;
import com.evolution.model.InteractionMetric;
import com.evolution.model.LifecycleState;
import com.evolution.skill.SkillImprovementCurve;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class LifecycleManager {
    private static final Logger logger = LoggerFactory.getLogger(LifecycleManager.class);

    private final int totalDays;
    private int currentDay;
    private LifecycleState currentState;
    private final SkillImprovementCurve skillCurve;
    private final InterestDriftModel interestModel;
    private final MarkovTransitionMatrix markovMatrix;
    private final ChurnModel churnModel;
    private final Random random;
    private final Map<AppModule, Integer> dailyModuleVisits;

    public LifecycleManager(int totalDays) {
        this.totalDays = totalDays;
        this.currentDay = 0;
        this.currentState = LifecycleState.ONBOARDING;
        this.skillCurve = new SkillImprovementCurve(totalDays);
        this.interestModel = new InterestDriftModel();
        this.markovMatrix = new MarkovTransitionMatrix();
        this.churnModel = new ChurnModel();
        this.random = new Random();
        this.dailyModuleVisits = new HashMap<>();

        logger.info("LifecycleManager initialized for {} day simulation", totalDays);
    }

    public boolean advanceDay() {
        currentDay++;
        
        if (currentDay > totalDays) {
            logger.info("Simulation complete: {} days elapsed", totalDays);
            return false;
        }

        LifecycleState newState = LifecycleState.getStateForDay(currentDay);
        
        if (newState != currentState) {
            handleLifecycleTransition(currentState, newState);
            currentState = newState;
        }

        skillCurve.updateSkillForDay(currentDay, currentState, getExpectedSessionsForDay());
        interestModel.updateInterestForDay(currentDay, dailyModuleVisits);
        markovMatrix.adaptToHabits(interestModel.getHabitStrengths(), interestModel.getBoredomScores());

        InteractionMetric metrics = skillCurve.getCurrentMetrics();
        churnModel.updateChurnProbability(currentDay, currentState, 
                metrics.getNavigationLatencyMs(), metrics.getErrorRate(),
                dailyModuleVisits.values().stream().mapToInt(Integer::intValue).sum(),
                metrics.getAvgSessionDurationSeconds());

        dailyModuleVisits.clear();

        if (currentState.isTransitionDay(currentDay)) {
            logger.info("=== DAY {}: LIFECYCLE TRANSITION ===", currentDay);
            logger.info("Transitioned from previous state to: {}", currentState.name());
            logger.info("Description: {}", currentState.getDescription());
            logCurrentState();
        }

        return !churnModel.hasChurned();
    }

    private void handleLifecycleTransition(LifecycleState oldState, LifecycleState newState) {
        logger.info("Lifecycle transition: {} -> {}", oldState.name(), newState.name());

        switch (newState) {
            case ONBOARDING:
                handleOnboardingEntry();
                break;
            case EXPLORER:
                handleExplorerEntry();
                break;
            case UTILITY:
                handleUtilityEntry();
                break;
            case POWER_USER:
                handlePowerUserEntry();
                break;
            default:
                break;
        }
    }

    private void handleOnboardingEntry() {
        logger.info("Entering ONBOARDING phase - user is learning basics");
        interestModel.triggerNoveltySeeking();
    }

    private void handleExplorerEntry() {
        logger.info("Entering EXPLORER phase - user discovering features");
        interestModel.triggerNoveltySeeking();
        
        interestModel.reinforceHabit(AppModule.HOME_SCREEN, 0.3);
        interestModel.reinforceHabit(AppModule.SEARCH, 0.2);
    }

    private void handleUtilityEntry() {
        logger.info("Entering UTILITY phase - user establishing habits");
        
        AppModule primaryUtility = identifyPrimaryUtilityModule();
        AppModule secondaryUtility = identifySecondaryUtilityModule();

        interestModel.reinforceHabit(primaryUtility, 0.5);
        interestModel.reinforceHabit(secondaryUtility, 0.3);

        markovMatrix.reinforcePath(AppModule.HOME_SCREEN, primaryUtility, 0.15);
        markovMatrix.reinforcePath(AppModule.SEARCH, primaryUtility, 0.15);

        logger.info("Utility habits formed around: {} (primary), {} (secondary)",
                primaryUtility.getModuleId(), secondaryUtility.getModuleId());
    }

    private void handlePowerUserEntry() {
        logger.info("Entering POWER_USER phase - user has mastered the app");

        interestModel.reinforceHabit(AppModule.HOME_SCREEN, 0.4);
        interestModel.reinforceHabit(AppModule.NOTIFICATIONS, 0.25);

        AppModule primaryUtility = identifyPrimaryUtilityModule();
        markovMatrix.reinforcePath(AppModule.NOTIFICATIONS, primaryUtility, 0.20);
        markovMatrix.reinforcePath(primaryUtility, AppModule.HOME_SCREEN, 0.20);

        double skillScore = skillCurve.getSkillProficiencyScore();
        logger.info("Power user achieved with skill proficiency score: {:.2f}", skillScore);
    }

    private AppModule identifyPrimaryUtilityModule() {
        Map<AppModule, Double> probabilities = interestModel.getVisitProbabilities();
        return probabilities.entrySet().stream()
                .filter(e -> !e.getKey().equals(AppModule.HOME_SCREEN) &&
                            !e.getKey().equals(AppModule.SETTINGS))
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(AppModule.CONTENT_A);
    }

    private AppModule identifySecondaryUtilityModule() {
        Map<AppModule, Double> probabilities = interestModel.getVisitProbabilities();
        AppModule primary = identifyPrimaryUtilityModule();
        
        return probabilities.entrySet().stream()
                .filter(e -> !e.getKey().equals(AppModule.HOME_SCREEN) &&
                            !e.getKey().equals(AppModule.SETTINGS) &&
                            !e.getKey().equals(primary))
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(AppModule.CONTENT_B);
    }

    public SimulationSession generateSession() {
        if (churnModel.hasChurned()) {
            return null;
        }

        int sessionDuration = generateSessionDuration();
        int interactionCount = generateInteractionCount(sessionDuration);

        SimulationSession session = new SimulationSession(currentDay, currentState, sessionDuration);
        AppModule currentModule = AppModule.HOME_SCREEN;

        for (int i = 0; i < interactionCount; i++) {
            boolean useMarkov = random.nextDouble() < getMarkovUsageProbability();
            
            if (useMarkov) {
                currentModule = markovMatrix.getNextState(currentModule);
            } else {
                currentModule = interestModel.selectNextModule();
            }

            session.addModuleVisit(currentModule);
            dailyModuleVisits.merge(currentModule, 1, Integer::sum);

            if (random.nextDouble() < 0.15) {
                markovMatrix.reinforcePath(
                    session.getLastModule(), 
                    currentModule, 
                    0.05 * skillCurve.getSkillProficiencyScore()
                );
            }
        }

        InteractionMetric metrics = skillCurve.getCurrentMetrics();
        metrics.setAvgSessionDurationSeconds(sessionDuration);
        metrics.incrementInteractions(interactionCount);

        session.setMetrics(metrics);

        if (sessionDuration > 120 && random.nextDouble() < 0.3) {
            churnModel.recordPositiveExperience();
        }

        return session;
    }

    private int generateSessionDuration() {
        double baseDuration = switch (currentState) {
            case ONBOARDING -> 90.0;
            case EXPLORER -> 180.0;
            case UTILITY -> 150.0;
            case POWER_USER -> 120.0;
            default -> 120.0;
        };

        double skillModifier = skillCurve.getSkillProficiencyScore();
        double actualDuration = baseDuration * (0.7 + 0.6 * random.nextDouble()) * skillModifier;

        return (int) Math.max(actualDuration, 30);
    }

    private int generateInteractionCount(int sessionDurationSeconds) {
        InteractionMetric metrics = skillCurve.getCurrentMetrics();
        double avgTimePerAction = metrics.getNavigationLatencyMs() / 1000.0 + 2.0;
        
        int estimatedActions = (int) (sessionDurationSeconds / avgTimePerAction);
        int variance = (int) (estimatedActions * 0.3);
        
        return Math.max(estimatedActions + random.nextInt(2 * variance + 1) - variance, 5);
    }

    private double getMarkovUsageProbability() {
        return switch (currentState) {
            case ONBOARDING -> 0.3;
            case EXPLORER -> 0.5;
            case UTILITY -> 0.7;
            case POWER_USER -> 0.85;
            default -> 0.5;
        };
    }

    private int getExpectedSessionsForDay() {
        return switch (currentState) {
            case ONBOARDING -> 3;
            case EXPLORER -> 4;
            case UTILITY -> 5;
            case POWER_USER -> 6;
            default -> 3;
        };
    }

    public void simulateLatencyIssue(double spikeMultiplier, int durationDays) {
        logger.warn("Simulating latency issue: {:.2f}x spike for {} days", spikeMultiplier, durationDays);
        
        skillCurve.applyLatencySpike(spikeMultiplier);
        churnModel.recordLatencySpike(spikeMultiplier);
    }

    public void recoverFromLatencyIssue() {
        logger.info("Recovering from latency issue");
        skillCurve.recoverFromLatencySpike(0.3);
    }

    public void logCurrentState() {
        InteractionMetric metrics = skillCurve.getCurrentMetrics();
        
        logger.info("=== CURRENT STATE (Day {}) ===", currentDay);
        logger.info("Lifecycle Phase: {} ({})", currentState.name(), currentState.getDescription());
        logger.info("Skill Proficiency: {:.2f}", skillCurve.getSkillProficiencyScore());
        logger.info("Navigation Latency: {:.2f}ms", metrics.getNavigationLatencyMs());
        logger.info("Error Rate: {:.4f}", metrics.getErrorRate());
        logger.info("Churn Probability: {:.4f}", churnModel.getChurnProbability());
        
        if (!churnModel.getActiveTriggers().isEmpty()) {
            logger.info("Active Churn Triggers:");
            for (var trigger : churnModel.getActiveTriggers()) {
                logger.info("  - {}: {:.4f} ({})", trigger.getType(), trigger.getImpact(), trigger.getDescription());
            }
        }
        
        if (!churnModel.getActiveFactors().isEmpty()) {
            logger.info("Active Retention Factors:");
            for (var factor : churnModel.getActiveFactors()) {
                logger.info("  - {}: {:.4f} ({})", factor.getType(), factor.getImpact(), factor.getDescription());
            }
        }
        
        logger.info("============================");
    }

    public int getCurrentDay() {
        return currentDay;
    }

    public LifecycleState getCurrentState() {
        return currentState;
    }

    public SkillImprovementCurve getSkillCurve() {
        return skillCurve;
    }

    public InterestDriftModel getInterestModel() {
        return interestModel;
    }

    public MarkovTransitionMatrix getMarkovMatrix() {
        return markovMatrix;
    }

    public ChurnModel getChurnModel() {
        return churnModel;
    }

    public boolean isSimulationActive() {
        return currentDay <= totalDays && !churnModel.hasChurned();
    }
}
