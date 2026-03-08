package com.simulation.maturity;

import com.simulation.maturity.level3.*;
import com.simulation.maturity.level4.*;
import com.simulation.maturity.level5.*;

/**
 * Level 5 Realism Maturity Model - Integration Controller
 * 
 * Implements a comprehensive framework for simulating human-like population dynamics:
 * 
 * Level 3 (Pattern-Based) - Statistical Realism:
 * - Log-normal distributions for Inter-Arrival Times
 * - Gaussian Blur for coordinate-based UI interactions
 * - Pareto distributions for session frequency (80/20 rule)
 * 
 * Level 4 (Behavioral Systems) - Cognitive State Machines:
 * - Attention Decay Function (Decision Fatigue)
 * - Habit Formation Logic (Reinforcement Learning)
 * - Contextual Awareness (Device Environment FSM)
 * 
 * Level 5 (Population Systems) - Macro-Behavioral Dynamics:
 * - Neighborhood Influence (Social Proofing/Herd Behavior)
 * - Interest Decay Model (90-Day Lifecycle)
 * - Virtual Budget (Economic Constraints)
 * 
 * Device Target: Samsung Galaxy A12 (SM-A125U)
 * Target Demographic: Budget-conscious users, MediaTek Helio P35
 */
public class Level5RealismMaturityModel {
    
    // Level 3 components
    private StatisticalRealismModel statisticalModel;
    
    // Level 4 components
    private AttentionDecayFunction attentionDecay;
    private HabitFormationLogic habitFormation;
    private ContextualAwareness contextualAwareness;
    
    // Level 5 components
    private NeighborhoodInfluence neighborhoodInfluence;
    private java.util.Map<String, InterestDecayModel> userLifecycleModels;
    private java.util.Map<String, VirtualBudget> userBudgets;
    
    // Population state
    private String[] populationAgents;
    private int populationSize;
    private java.util.Random random;
    
    public Level5RealismMaturityModel() {
        this.random = new java.util.Random();
        initializeComponents();
    }
    
    private void initializeComponents() {
        // Level 3
        this.statisticalModel = new StatisticalRealismModel();
        
        // Level 4
        this.attentionDecay = new AttentionDecayFunction();
        this.habitFormation = new HabitFormationLogic();
        this.contextualAwareness = new ContextualAwareness();
        
        // Level 5
        this.neighborhoodInfluence = new NeighborhoodInfluence();
        this.userLifecycleModels = new java.util.HashMap<>();
        this.userBudgets = new java.util.HashMap<>();
        
        this.populationSize = 0;
    }
    
    /**
     * Initialize a population of agents for simulation
     */
    public void initializePopulation(int size) {
        this.populationSize = size;
        this.populationAgents = new String[size];
        
        for (int i = 0; i < size; i++) {
            String agentId = "agent_" + i;
            populationAgents[i] = agentId;
            
            // Create lifecycle model for each agent
            userLifecycleModels.put(agentId, new InterestDecayModel(agentId));
            
            // Create budget (randomized $10-50 monthly)
            double budget = 10 + Math.random() * 40;
            userBudgets.put(agentId, new VirtualBudget(agentId, budget));
            
            // Initialize habit formation with some base actions
            habitFormation.applyPositiveReinforcement("app_launch", 0.3);
            habitFormation.applyPositiveReinforcement("browse", 0.2);
        }
        
        // Create social graph (random connections)
        createSocialGraph(size);
    }
    
    /**
     * Create random social graph with realistic properties
     */
    private void createSocialGraph(int size) {
        // Each agent has 3-10 random connections
        for (int i = 0; i < size; i++) {
            String agentId = "agent_" + i;
            int connectionCount = 3 + random.nextInt(8);
            
            java.util.List<String> neighbors = new java.util.ArrayList<>();
            for (int j = 0; j < connectionCount; j++) {
                int neighborIdx = random.nextInt(size);
                if (neighborIdx != i) {
                    neighbors.add("agent_" + neighborIdx);
                }
            }
            
            neighborhoodInfluence.addAgent(agentId, neighbors);
        }
    }
    
    /**
     * Simulate one day for the population
     * 
     * @param dayOfMonth Current day (1-30)
     */
    public void simulateDay(int dayOfMonth) {
        for (int i = 0; i < populationSize; i++) {
            String agentId = populationAgents[i];
            
            // Level 4: Check contextual awareness
            contextualAwareness.updateContext(
                50 + random.nextInt(50),     // Battery 50-100%
                50 + random.nextInt(100),    // Network latency 50-150ms
                35 + random.nextInt(10),     // Temperature 35-45°C
                dayOfMonth * 24 / 30         // Hour of day
            );
            
            // Check if urgent context triggers faster behavior
            if (contextualAwareness.isPanicMode()) {
                // User in panic mode - rush to complete tasks
                habitFormation.applyPositiveReinforcement("app_launch", 0.8);
            }
            
            // Level 4: Update attention decay
            attentionDecay.startSession();
            int decisionCount = random.nextInt(20);
            for (int d = 0; d < decisionCount; d++) {
                attentionDecay.makeDecision(new java.util.Random());
            }
            
            // Check if fatigue affects behavior
            if (attentionDecay.getCurrentQuality() < 0.6) {
                // Fatigued - reduce session
                habitFormation.applyPositiveReinforcement("app_launch", 0.2);
            }
            
            // Level 5: Check social influence
            String nextAction = neighborhoodInfluence.sampleAction(
                agentId, new String[]{"browse", "purchase", "share", "comment"});
            
            if (neighborhoodInfluence.hasHerdInfluence(agentId, nextAction)) {
                // Herd influence triggers action
                habitFormation.applyPositiveReinforcement(nextAction, 0.6);
            }
            
            // Level 5: Simulate session
            InterestDecayModel lifecycle = userLifecycleModels.get(agentId);
            int expectedSessions = lifecycle.getExpectedSessionsTomorrow();
            
            for (int s = 0; s < expectedSessions; s++) {
                long sessionDuration = lifecycle.simulateSession();
                
                // Record activity for social graph
                neighborhoodInfluence.recordAction(agentId, "session");
                
                // Apply habit reinforcement
                if (sessionDuration > 300000) {
                    habitFormation.applyPositiveReinforcement("engagement", 0.4);
                }
            }
            
            // Level 5: Economic constraints
            VirtualBudget budget = userBudgets.get(agentId);
            budget.advanceDay();
            
            // Simulate purchase attempts based on interest
            if (lifecycle.getInterestLevel() > 0.5) {
                double purchaseAmount = 1 + Math.random() * 10;
                VirtualBudget.Category[] categories = VirtualBudget.Category.values();
                VirtualBudget.Category category = categories[random.nextInt(categories.length)];
                
                VirtualBudget.PurchaseResult result = budget.attemptPurchase(
                    purchaseAmount, category, "item_" + i);
                
                if (result.success) {
                    habitFormation.applyPositiveReinforcement("purchase", 0.7);
                    neighborhoodInfluence.recordAction(agentId, "purchase");
                }
            }
            
            // Apply temporal decay to habits (every 7 days)
            if (dayOfMonth % 7 == 0) {
                habitFormation.applyTemporalDecay(7);
            }
        }
        
        // Apply social graph time decay
        if (dayOfMonth % 24 == 0) {
            neighborhoodInfluence.applyTimeDecay();
        }
    }
    
    /**
     * Run complete 90-day simulation
     */
    public SimulationResults run90DaySimulation() {
        if (populationSize == 0) {
            initializePopulation(100); // Default 100 agents
        }
        
        int[] dailyActiveUsers = new int[90];
        int[] dailySessions = new int[90];
        double[] avgInterest = new double[90];
        int churnedCount = 0;
        int reactivatedCount = 0;
        
        for (int day = 0; day < 90; day++) {
            simulateDay(day + 1);
            
            // Collect metrics
            int active = 0;
            int sessions = 0;
            double interest = 0;
            
            for (InterestDecayModel model : userLifecycleModels.values()) {
                if (!model.isChurned()) {
                    active++;
                    sessions += model.getExpectedSessionsTomorrow();
                    interest += model.getInterestLevel();
                } else {
                    churnedCount++;
                }
                
                if (model.getCurrentState() == InterestDecayModel.LifecycleState.REACTIVATED) {
                    reactivatedCount++;
                }
            }
            
            dailyActiveUsers[day] = active;
            dailySessions[day] = sessions;
            avgInterest[day] = interest / populationSize;
        }
        
        return new SimulationResults(
            populationSize, dailyActiveUsers, dailySessions, avgInterest,
            churnedCount, reactivatedCount, calculateRetentionMetrics()
        );
    }
    
    /**
     * Calculate retention metrics
     */
    private RetentionMetrics calculateRetentionMetrics() {
        int day1Retention = 0;
        int day7Retention = 0;
        int day30Retention = 0;
        
        for (InterestDecayModel model : userLifecycleModels.values()) {
            if (model.getDayOfLifecycle() >= 1) day1Retention++;
            if (model.getDayOfLifecycle() >= 7) day7Retention++;
            if (model.getDayOfLifecycle() >= 30) day30Retention++;
        }
        
        return new RetentionMetrics(
            (double) day1Retention / populationSize,
            (double) day7Retention / populationSize,
            (double) day30Retention / populationSize
        );
    }
    
    /**
     * Results container
     */
    public static class SimulationResults {
        public final int populationSize;
        public final int[] dailyActiveUsers;
        public final int[] dailySessions;
        public final double[] avgInterest;
        public final int totalChurned;
        public final int totalReactivated;
        public final RetentionMetrics retention;
        
        public SimulationResults(int pop, int[] active, int[] sessions,
                                 double[] interest, int churned, int reactivated,
                                 RetentionMetrics retention) {
            this.populationSize = pop;
            this.dailyActiveUsers = active;
            this.dailySessions = sessions;
            this.avgInterest = interest;
            this.totalChurned = churned;
            this.totalReactivated = reactivated;
            this.retention = retention;
        }
        
        public void printSummary() {
            System.out.println("=== 90-Day Simulation Results ===");
            System.out.printf("Population: %d agents%n", populationSize);
            System.out.printf("Day 1 Retention: %.1f%%%n", retention.day1 * 100);
            System.out.printf("Day 7 Retention: %.1f%%%n", retention.day7 * 100);
            System.out.printf("Day 30 Retention: %.1f%%%n", retention.day30 * 100);
            System.out.printf("Churned: %d (%.1f%%)%n", totalChurned, 
                totalChurned * 100.0 / populationSize);
            System.out.printf("Reactivated: %d%n", totalReactivated);
            System.out.printf("Average Day 90 Interest: %.1f%%%n", avgInterest[89] * 100);
        }
    }
    
    public static class RetentionMetrics {
        public final double day1;
        public final double day7;
        public final double day30;
        
        public RetentionMetrics(double d1, double d7, double d30) {
            this.day1 = d1;
            this.day7 = d7;
            this.day30 = d30;
        }
    }
    
    /**
     * Demo main method
     */
    public static void main(String[] args) {
        System.out.println("=== Level 5 Realism Maturity Model Demo ===\n");
        
        Level5RealismMaturityModel model = new Level5RealismMaturityModel();
        
        // Initialize with test population
        System.out.println("Initializing 50-agent population...");
        model.initializePopulation(50);
        
        // Run simulation
        System.out.println("Running 90-day simulation...\n");
        SimulationResults results = model.run90DaySimulation();
        
        // Print results
        results.printSummary();
        
        System.out.println("\n=== Demo Complete ===");
    }
}
