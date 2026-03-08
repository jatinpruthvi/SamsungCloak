package com.simulation.structural.drift;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * EvolutionEngine - Long-Term Behavioral Drift System
 *
 * Adjusts interaction entropy over a 90-day window to simulate natural user
 * progression from "New User" to "Power User" with mathematically grounded
 * behavioral evolution.
 *
 * Mathematical Foundation:
 * - Entropy Function: H(t) = H₀ × e^(-λt) + H∞ × (1 - e^(-λt))
 *   Where H₀ = initial entropy, H∞ = asymptotic entropy, λ = learning rate
 *
 * - Skill Acquisition Curve: S(t) = S_max × (1 - e^(-kt))
 *   Logarithmic improvement following power law of practice
 *
 * - Exploration vs Exploitation: ε(t) = ε₀ × e^(-t/τ)
 *   Epsilon-greedy decay modeling reduced novelty-seeking over time
 *
 * Behavioral Dimensions:
 * 1. Navigation Efficiency: Latency reduction following Fitts' Law
 * 2. Error Rate Decay: Decreasing mistakes with experience
 * 3. Feature Adoption: Expanding feature usage breadth
 * 4. Interaction Velocity: Increasing actions per session
 * 5. Session Depth: Longer, more complex session patterns
 */
public class EvolutionEngine {

    private static final String LOG_TAG = "StructuralFidelity.EvolutionEngine";

    // 90-day simulation window
    public static final int SIMULATION_DAYS = 90;

    // Entropy parameters
    private static final double INITIAL_ENTROPY = 0.85;
    private static final double ASYMPTOTIC_ENTROPY = 0.35;
    private static final double ENTROPY_DECAY_RATE = 0.045;

    // Skill acquisition parameters (Power Law of Practice)
    private static final double SKILL_ACQUISITION_RATE = 0.052;
    private static final double MAX_SKILL_LEVEL = 0.95;

    // Phase thresholds
    private static final int NEW_USER_PHASE_DAYS = 7;
    private static final int LEARNING_PHASE_DAYS = 30;
    private static final int COMPETENT_PHASE_DAYS = 60;

    private final Map<String, UserEvolutionProfile> userProfiles;
    private final Random random;
    private int currentSimulationDay;

    public EvolutionEngine() {
        this.userProfiles = new ConcurrentHashMap<>();
        this.random = new Random();
        this.currentSimulationDay = 0;
    }

    /**
     * User Evolution Profile
     * Tracks individual progression through behavioral phases
     */
    public static class UserEvolutionProfile {
        private final String userId;
        private final LocalDateTime registrationTime;

        // Current state
        private EvolutionPhase currentPhase;
        private int daysSinceRegistration;

        // Entropy tracking
        private double currentEntropy;
        private double entropyVelocity;

        // Skill metrics (0.0 - 1.0)
        private double navigationSkill;
        private double featureProficiency;
        private double efficiencyScore;

        // Behavioral metrics
        private double interactionVelocity;      // Actions per minute
        private double errorRate;
        private double explorationRatio;         // Novel vs familiar actions

        // Historical tracking
        private List<DailySnapshot> history;

        public UserEvolutionProfile(String userId) {
            this.userId = userId;
            this.registrationTime = LocalDateTime.now();
            this.currentPhase = EvolutionPhase.NEW_USER;
            this.daysSinceRegistration = 0;

            this.currentEntropy = INITIAL_ENTROPY;
            this.entropyVelocity = 0.0;

            this.navigationSkill = 0.05;
            this.featureProficiency = 0.10;
            this.efficiencyScore = 0.08;

            this.interactionVelocity = 3.5;      // 3.5 actions/min initially
            this.errorRate = 0.18;                // 18% error rate
            this.explorationRatio = 0.75;         // 75% exploration

            this.history = new ArrayList<>();
        }

        public void advanceDay() {
            daysSinceRegistration++;
            recordSnapshot();
        }

        private void recordSnapshot() {
            history.add(new DailySnapshot(
                daysSinceRegistration,
                currentPhase,
                currentEntropy,
                navigationSkill,
                featureProficiency,
                efficiencyScore,
                interactionVelocity,
                errorRate,
                explorationRatio
            ));
        }

        public double calculateProgressionScore() {
            double skillComponent = (navigationSkill + featureProficiency + efficiencyScore) / 3.0;
            double behavioralComponent = (interactionVelocity / 15.0) * (1 - errorRate);
            return (skillComponent * 0.6) + (behavioralComponent * 0.4);
        }

        public String getUserId() { return userId; }
        public EvolutionPhase getCurrentPhase() { return currentPhase; }
        public void setCurrentPhase(EvolutionPhase phase) { this.currentPhase = phase; }
        public int getDaysSinceRegistration() { return daysSinceRegistration; }
        public double getCurrentEntropy() { return currentEntropy; }
        public void setCurrentEntropy(double entropy) { this.currentEntropy = entropy; }
        public double getNavigationSkill() { return navigationSkill; }
        public void setNavigationSkill(double skill) { this.navigationSkill = skill; }
        public double getFeatureProficiency() { return featureProficiency; }
        public void setFeatureProficiency(double proficiency) { this.featureProficiency = proficiency; }
        public double getEfficiencyScore() { return efficiencyScore; }
        public void setEfficiencyScore(double score) { this.efficiencyScore = score; }
        public double getInteractionVelocity() { return interactionVelocity; }
        public void setInteractionVelocity(double velocity) { this.interactionVelocity = velocity; }
        public double getErrorRate() { return errorRate; }
        public void setErrorRate(double rate) { this.errorRate = rate; }
        public double getExplorationRatio() { return explorationRatio; }
        public void setExplorationRatio(double ratio) { this.explorationRatio = ratio; }
        public List<DailySnapshot> getHistory() { return new ArrayList<>(history); }
    }

    /**
     * Daily snapshot for longitudinal tracking
     */
    public static class DailySnapshot {
        public final int day;
        public final EvolutionPhase phase;
        public final double entropy;
        public final double navigationSkill;
        public final double featureProficiency;
        public final double efficiencyScore;
        public final double interactionVelocity;
        public final double errorRate;
        public final double explorationRatio;

        public DailySnapshot(int day, EvolutionPhase phase, double entropy,
                             double navigationSkill, double featureProficiency,
                             double efficiencyScore, double interactionVelocity,
                             double errorRate, double explorationRatio) {
            this.day = day;
            this.phase = phase;
            this.entropy = entropy;
            this.navigationSkill = navigationSkill;
            this.featureProficiency = featureProficiency;
            this.efficiencyScore = efficiencyScore;
            this.interactionVelocity = interactionVelocity;
            this.errorRate = errorRate;
            this.explorationRatio = explorationRatio;
        }
    }

    public enum EvolutionPhase {
        NEW_USER("New User", 0, 7),
        LEARNING("Learning", 7, 30),
        COMPETENT("Competent User", 30, 60),
        POWER_USER("Power User", 60, 90);

        private final String displayName;
        private final int startDay;
        private final int endDay;

        EvolutionPhase(String displayName, int startDay, int endDay) {
            this.displayName = displayName;
            this.startDay = startDay;
            this.endDay = endDay;
        }

        public static EvolutionPhase forDay(int day) {
            for (EvolutionPhase phase : values()) {
                if (day >= phase.startDay && day < phase.endDay) {
                    return phase;
                }
            }
            return POWER_USER;
        }

        public String getDisplayName() { return displayName; }
        public int getStartDay() { return startDay; }
        public int getEndDay() { return endDay; }
    }

    /**
     * Register a new user for evolution tracking
     */
    public UserEvolutionProfile registerUser(String userId) {
        UserEvolutionProfile profile = new UserEvolutionProfile(userId);
        userProfiles.put(userId, profile);
        return profile;
    }

    /**
     * Advance simulation by one day and update all user profiles
     */
    public void advanceSimulationDay() {
        currentSimulationDay++;

        for (UserEvolutionProfile profile : userProfiles.values()) {
            updateUserEvolution(profile);
            profile.advanceDay();
        }
    }

    /**
     * Core evolution update logic
     * Applies mathematical models for behavioral drift
     */
    private void updateUserEvolution(UserEvolutionProfile profile) {
        int day = profile.getDaysSinceRegistration();

        if (day > SIMULATION_DAYS) {
            return;
        }

        EvolutionPhase newPhase = EvolutionPhase.forDay(day);
        profile.setCurrentPhase(newPhase);

        updateEntropy(profile, day);
        updateNavigationSkill(profile, day);
        updateFeatureProficiency(profile, day);
        updateEfficiencyScore(profile, day);
        updateInteractionVelocity(profile, day);
        updateErrorRate(profile, day);
        updateExplorationRatio(profile, day);
    }

    /**
     * Entropy decay following exponential decay model
     * H(t) = H∞ + (H₀ - H∞) × e^(-λt)
     */
    private void updateEntropy(UserEvolutionProfile profile, int day) {
        double entropyRange = INITIAL_ENTROPY - ASYMPTOTIC_ENTROPY;
        double decayFactor = Math.exp(-ENTROPY_DECAY_RATE * day);
        double newEntropy = ASYMPTOTIC_ENTROPY + (entropyRange * decayFactor);

        // Add micro-variance for realism
        double variance = 0.02 * random.nextGaussian();
        profile.setCurrentEntropy(Math.max(0.1, Math.min(0.9, newEntropy + variance)));
    }

    /**
     * Navigation skill improvement following Power Law of Practice
     * S(t) = S_max × (1 - e^(-kt))
     */
    private void updateNavigationSkill(UserEvolutionProfile profile, int day) {
        double skillProgress = 1.0 - Math.exp(-SKILL_ACQUISITION_RATE * day);
        double newSkill = MAX_SKILL_LEVEL * skillProgress;

        // Phase-based acceleration
        double phaseMultiplier = switch (profile.getCurrentPhase()) {
            case NEW_USER -> 1.3;
            case LEARNING -> 1.1;
            case COMPETENT -> 0.9;
            case POWER_USER -> 0.7;
        };

        double currentSkill = profile.getNavigationSkill();
        double targetSkill = newSkill * phaseMultiplier;
        double blendedSkill = currentSkill + (targetSkill - currentSkill) * 0.15;

        profile.setNavigationSkill(Math.min(MAX_SKILL_LEVEL, blendedSkill));
    }

    /**
     * Feature proficiency growth with discovery phases
     */
    private void updateFeatureProficiency(UserEvolutionProfile profile, int day) {
        double baseGrowth = 1.0 - Math.exp(-0.035 * day);
        double discoveryBoost = calculateDiscoveryBoost(day);

        double targetProficiency = (0.85 * baseGrowth) + discoveryBoost;
        double currentProficiency = profile.getFeatureProficiency();
        double blendedProficiency = currentProficiency + (targetProficiency - currentProficiency) * 0.12;

        profile.setFeatureProficiency(Math.min(0.95, blendedProficiency));
    }

    private double calculateDiscoveryBoost(int day) {
        if (day < 3) return 0.05;
        if (day < 14) return 0.15;
        if (day < 30) return 0.10;
        if (day == 30 || day == 60) return 0.08;
        return 0.02;
    }

    /**
     * Efficiency score combining multiple factors
     */
    private void updateEfficiencyScore(UserEvolutionProfile profile, int day) {
        double efficiency = (profile.getNavigationSkill() * 0.4) +
                           (profile.getFeatureProficiency() * 0.35) +
                           ((1 - profile.getErrorRate()) * 0.25);

        double learningCurve = Math.log1p(day) / Math.log1p(SIMULATION_DAYS);
        double targetEfficiency = efficiency * (0.5 + 0.5 * learningCurve);

        double currentEfficiency = profile.getEfficiencyScore();
        double blendedEfficiency = currentEfficiency + (targetEfficiency - currentEfficiency) * 0.10;

        profile.setEfficiencyScore(Math.min(0.95, blendedEfficiency));
    }

    /**
     * Interaction velocity increase with experience
     * Gradual improvement with plateau
     */
    private void updateInteractionVelocity(UserEvolutionProfile profile, int day) {
        double baseVelocity = 3.5;
        double maxVelocity = 15.0;
        double velocityRange = maxVelocity - baseVelocity;

        double progress = Math.min(1.0, day / 60.0);
        double sigmoidProgress = 1.0 / (1.0 + Math.exp(-6 * (progress - 0.5)));

        double targetVelocity = baseVelocity + (velocityRange * sigmoidProgress);
        double currentVelocity = profile.getInteractionVelocity();
        double blendedVelocity = currentVelocity + (targetVelocity - currentVelocity) * 0.08;

        profile.setInteractionVelocity(blendedVelocity);
    }

    /**
     * Error rate decay following learning curve
     */
    private void updateErrorRate(UserEvolutionProfile profile, int day) {
        double initialErrorRate = 0.18;
        double asymptoticErrorRate = 0.02;
        double errorRange = initialErrorRate - asymptoticErrorRate;

        double decayFactor = Math.exp(-0.055 * day);
        double targetErrorRate = asymptoticErrorRate + (errorRange * decayFactor);

        // Add situational variance
        double variance = 0.015 * random.nextGaussian();
        double newErrorRate = Math.max(0.005, targetErrorRate + variance);

        double currentErrorRate = profile.getErrorRate();
        double blendedErrorRate = currentErrorRate + (newErrorRate - currentErrorRate) * 0.12;

        profile.setErrorRate(blendedErrorRate);
    }

    /**
     * Exploration ratio decay (more exploitation over time)
 */
    private void updateExplorationRatio(UserEvolutionProfile profile, int day) {
        double initialExploration = 0.75;
        double asymptoticExploration = 0.25;
        double explorationRange = initialExploration - asymptoticExploration;

        double decayFactor = Math.exp(-0.04 * day);
        double targetExploration = asymptoticExploration + (explorationRange * decayFactor);

        // Power users occasionally re-explore
        if (profile.getCurrentPhase() == EvolutionPhase.POWER_USER && random.nextDouble() < 0.1) {
            targetExploration += 0.15;
        }

        double currentExploration = profile.getExplorationRatio();
        double blendedExploration = currentExploration + (targetExploration - currentExploration) * 0.06;

        profile.setExplorationRatio(Math.max(0.1, Math.min(0.8, blendedExploration)));
    }

    /**
     * Get navigation latency prediction based on skill level
     * Fitts' Law inspired: MT = a + b × log₂(2D/W)
     */
    public long predictNavigationLatency(String userId, double distance, double targetSize) {
        UserEvolutionProfile profile = userProfiles.get(userId);
        if (profile == null) {
            return 1500;
        }

        double skillLevel = profile.getNavigationSkill();
        double efficiency = profile.getEfficiencyScore();

        double a = 200 - (skillLevel * 100);
        double b = 150 - (efficiency * 80);

        double difficultyIndex = Math.log(2 * distance / targetSize) / Math.log(2);
        double baseLatency = a + b * difficultyIndex;

        double entropyFactor = 1.0 + (profile.getCurrentEntropy() * 0.5);
        double fatigueJitter = 50 * random.nextGaussian();

        return Math.round((baseLatency * entropyFactor) + fatigueJitter);
    }

    /**
     * Get predicted session duration based on evolution phase
     */
    public int predictSessionDuration(String userId) {
        UserEvolutionProfile profile = userProfiles.get(userId);
        if (profile == null) {
            return 180;
        }

        double baseDuration = switch (profile.getCurrentPhase()) {
            case NEW_USER -> 120;
            case LEARNING -> 180;
            case COMPETENT -> 240;
            case POWER_USER -> 300;
        };

        double velocityFactor = profile.getInteractionVelocity() / 10.0;
        double entropyVariance = profile.getCurrentEntropy() * 60;

        return (int) Math.round(baseDuration * velocityFactor + entropyVariance * random.nextGaussian());
    }

    /**
     * Check if user action should be an error
     */
    public boolean shouldGenerateError(String userId) {
        UserEvolutionProfile profile = userProfiles.get(userId);
        if (profile == null) {
            return random.nextDouble() < 0.15;
        }

        return random.nextDouble() < profile.getErrorRate();
    }

    /**
     * Determine if user should explore or exploit
     */
    public boolean shouldExplore(String userId) {
        UserEvolutionProfile profile = userProfiles.get(userId);
        if (profile == null) {
            return random.nextDouble() < 0.5;
        }

        return random.nextDouble() < profile.getExplorationRatio();
    }

    /**
     * Get evolution summary statistics
     */
    public EvolutionSummary getSummary() {
        if (userProfiles.isEmpty()) {
            return new EvolutionSummary(0, 0, 0, 0, 0, 0);
        }

        int newUsers = 0, learning = 0, competent = 0, powerUsers = 0;
        double avgEntropy = 0, avgSkill = 0;

        for (UserEvolutionProfile profile : userProfiles.values()) {
            switch (profile.getCurrentPhase()) {
                case NEW_USER -> newUsers++;
                case LEARNING -> learning++;
                case COMPETENT -> competent++;
                case POWER_USER -> powerUsers++;
            }
            avgEntropy += profile.getCurrentEntropy();
            avgSkill += profile.calculateProgressionScore();
        }

        int total = userProfiles.size();
        return new EvolutionSummary(
            newUsers, learning, competent, powerUsers,
            avgEntropy / total, avgSkill / total
        );
    }

    public static class EvolutionSummary {
        public final int newUsers;
        public final int learningUsers;
        public final int competentUsers;
        public final int powerUsers;
        public final double averageEntropy;
        public final double averageSkill;

        public EvolutionSummary(int newUsers, int learningUsers, int competentUsers,
                                int powerUsers, double averageEntropy, double averageSkill) {
            this.newUsers = newUsers;
            this.learningUsers = learningUsers;
            this.competentUsers = competentUsers;
            this.powerUsers = powerUsers;
            this.averageEntropy = averageEntropy;
            this.averageSkill = averageSkill;
        }

        @Override
        public String toString() {
            return String.format(
                "EvolutionSummary{New=%d, Learning=%d, Competent=%d, Power=%d, " +
                "AvgEntropy=%.3f, AvgSkill=%.3f}",
                newUsers, learningUsers, competentUsers, powerUsers,
                averageEntropy, averageSkill
            );
        }
    }

    public UserEvolutionProfile getUserProfile(String userId) {
        return userProfiles.get(userId);
    }

    public int getCurrentSimulationDay() {
        return currentSimulationDay;
    }

    public void setCurrentSimulationDay(int day) {
        this.currentSimulationDay = day;
    }
}
