package com.simulation.maturity.level4;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Habit Formation Logic - Reinforcement Learning Loop
 * 
 * Models how user habits form through positive reinforcement:
 * - Successful app interactions increase visit probability
 * - Reward feedback loops strengthen habit strength
 * - Habit decay when positive reinforcement stops
 * 
 * Mathematical Model (Q-Learning inspired):
 * Q(s) = Q(s) + α * (R - Q(s))
 * 
 * Where:
 * - Q(s) = Action value/visit probability for state s
 * - α = Learning rate (0.0 - 1.0)
 * - R = Reward received (0.0 - 1.0)
 * - Visit probability = softmax(Q(s)) normalized
 * 
 * Device Target: Samsung Galaxy A12 (SM-A125U)
 */
public class HabitFormationLogic {
    
    // Q-values for each action/state
    private Map<String, Double> qValues;
    
    // Visit counts (for frequency-based reinforcement)
    private Map<String, Integer> visitCounts;
    
    // Habit strength (0.0 - 1.0) per action
    private Map<String, Double> habitStrengths;
    
    // Learning parameters
    private double learningRate;       // Alpha - how fast habits form
    private double discountFactor;    // Gamma - importance of future rewards
    private double decayRate;         // How fast habits fade without reinforcement
    
    private Random random;
    private long lastUpdateTime;
    
    public HabitFormationLogic() {
        this.qValues = new HashMap<>();
        this.visitCounts = new HashMap<>();
        this.habitStrengths = new HashMap<>();
        
        this.learningRate = 0.1;      // 10% learning per reinforcement
        this.discountFactor = 0.9;    // High value on future rewards
        this.decayRate = 0.05;        // 5% decay per day without interaction
        
        this.random = new Random();
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    /**
     * Register a positive reinforcement event
     * Called when user successfully completes an action or receives reward
     * 
     * @param actionId The action/app that was reinforced
     * @param rewardValue Reward value (0.0 - 1.0)
     */
    public void applyPositiveReinforcement(String actionId, double rewardValue) {
        rewardValue = Math.max(0.0, Math.min(1.0, rewardValue));
        
        // Get current Q-value
        double currentQ = qValues.getOrDefault(actionId, 0.5);
        
        // Q-learning update: Q(s) = Q(s) + α * (R - Q(s))
        double newQ = currentQ + learningRate * (rewardValue - currentQ);
        qValues.put(actionId, newQ);
        
        // Update visit count
        int visits = visitCounts.getOrDefault(actionId, 0);
        visitCounts.put(actionId, visits + 1);
        
        // Update habit strength based on Q-value and visit frequency
        double habitStrength = calculateHabitStrength(actionId);
        habitStrengths.put(actionId, habitStrength);
        
        lastUpdateTime = System.currentTimeMillis();
    }
    
    /**
     * Apply negative reinforcement (punishment)
     * Called when user encounters issues
     * 
     * @param actionId The action/app that had negative outcome
     * @param penalty Penalty value (0.0 - 1.0)
     */
    public void applyNegativeReinforcement(String actionId, double penalty) {
        penalty = Math.max(0.0, Math.min(1.0, penalty));
        
        double currentQ = qValues.getOrDefault(actionId, 0.5);
        
        // Negative update: decrease Q-value
        double newQ = currentQ - learningRate * penalty;
        qValues.put(actionId, Math.max(0.0, newQ));
        
        // Update habit strength
        double habitStrength = calculateHabitStrength(actionId);
        habitStrengths.put(actionId, habitStrength);
    }
    
    /**
     * Calculate visit probability for an action
     * Uses softmax normalization across all actions
     * 
     * @param actionId The action to check
     * @return Probability of visiting (0.0 - 1.0)
     */
    public double getVisitProbability(String actionId) {
        // Temperature controls exploration vs exploitation
        // Higher temp = more random, lower = more deterministic
        double temperature = 0.5;
        
        double q = qValues.getOrDefault(actionId, 0.5);
        
        // Softmax calculation
        double expSum = 0;
        for (Double value : qValues.values()) {
            expSum += Math.exp(value / temperature);
        }
        
        double expQ = Math.exp(q / temperature);
        return expQ / expSum;
    }
    
    /**
     * Select next action based on current probabilities
     * Epsilon-greedy selection
     * 
     * @param availableActions List of available actions
     * @param epsilon Exploration rate (0.0 = always exploit, 1.0 = always explore)
     * @return Selected action ID
     */
    public String selectNextAction(String[] availableActions, double epsilon) {
        // Exploration: random choice
        if (random.nextDouble() < epsilon) {
            return availableActions[random.nextInt(availableActions.length)];
        }
        
        // Exploitation: weighted by visit probability
        double[] probabilities = new double[availableActions.length];
        double totalProb = 0;
        
        for (int i = 0; i < availableActions.length; i++) {
            probabilities[i] = getVisitProbability(availableActions[i]);
            totalProb += probabilities[i];
        }
        
        // Normalize
        for (int i = 0; i < probabilities.length; i++) {
            probabilities[i] /= totalProb;
        }
        
        // Weighted random selection
        double roll = random.nextDouble();
        double cumulative = 0;
        
        for (int i = 0; i < availableActions.length; i++) {
            cumulative += probabilities[i];
            if (roll <= cumulative) {
                return availableActions[i];
            }
        }
        
        return availableActions[availableActions.length - 1];
    }
    
    /**
     * Apply time-based decay (habits fade without practice)
     * Call this periodically (e.g., daily)
     * 
     * @param daysSinceLastVisit Days since last interaction
     */
    public void applyTemporalDecay(int daysSinceLastVisit) {
        for (String actionId : qValues.keySet()) {
            double currentQ = qValues.get(actionId);
            double decay = Math.pow(1.0 - decayRate, daysSinceLastVisit);
            
            qValues.put(actionId, currentQ * decay);
            
            // Also decay habit strength
            double currentHabit = habitStrengths.getOrDefault(actionId, 0.0);
            habitStrengths.put(actionId, currentHabit * decay);
        }
    }
    
    /**
     * Calculate habit strength for an action
     * Based on Q-value and visit frequency
     */
    private double calculateHabitStrength(String actionId) {
        double q = qValues.getOrDefault(actionId, 0.5);
        int visits = visitCounts.getOrDefault(actionId, 0);
        
        // Habit strength increases with both Q-value and visit count
        // Capped at 1.0
        double visitFactor = Math.min(1.0, visits / 20.0); // Max at 20 visits
        
        return (q * 0.7) + (visitFactor * 0.3);
    }
    
    /**
     * Get current habit strength for an action
     */
    public double getHabitStrength(String actionId) {
        return habitStrengths.getOrDefault(actionId, 0.0);
    }
    
    /**
     * Get Q-value for an action
     */
    public double getQValue(String actionId) {
        return qValues.getOrDefault(actionId, 0.5);
    }
    
    /**
     * Check if action is now a habit (>0.7 strength)
     */
    public boolean isHabitFormed(String actionId) {
        return getHabitStrength(actionId) > 0.7;
    }
    
    /**
     * Get all actions sorted by visit probability
     */
    public String[] getActionsSortedByProbability() {
        return qValues.keySet().stream()
            .sorted((a, b) -> Double.compare(
                getVisitProbability(b), 
                getVisitProbability(a)
            ))
            .toArray(String[]::new);
    }
    
    /**
     * Set learning rate
     */
    public void setLearningRate(double alpha) {
        this.learningRate = Math.max(0.0, Math.min(1.0, alpha));
    }
    
    /**
     * Get summary statistics
     */
    public Map<String, Double> getAllHabitStrengths() {
        return new HashMap<>(habitStrengths);
    }
    
    /**
     * Get reinforcement statistics
     */
    public HabitStatistics getStatistics(String actionId) {
        return new HabitStatistics(
            actionId,
            qValues.getOrDefault(actionId, 0.5),
            visitCounts.getOrDefault(actionId, 0),
            habitStrengths.getOrDefault(actionId, 0.0),
            getVisitProbability(actionId),
            isHabitFormed(actionId)
        );
    }
    
    /**
     * Statistics container
     */
    public static class HabitStatistics {
        public final String actionId;
        public final double qValue;
        public final int visitCount;
        public final double habitStrength;
        public final double visitProbability;
        public final boolean isHabitFormed;
        
        public HabitStatistics(String actionId, double qValue, int visitCount,
                               double habitStrength, double visitProbability,
                               boolean isHabitFormed) {
            this.actionId = actionId;
            this.qValue = qValue;
            this.visitCount = visitCount;
            this.habitStrength = habitStrength;
            this.visitProbability = visitProbability;
            this.isHabitFormed = isHabitFormed;
        }
        
        @Override
        public String toString() {
            return String.format(
                "HabitStats{action=%s, q=%.3f, visits=%d, strength=%.1f%%, " +
                "prob=%.2f%%, isHabit=%s}",
                actionId, qValue, visitCount, 
                habitStrength * 100, visitProbability * 100, isHabitFormed
            );
        }
    }
}
