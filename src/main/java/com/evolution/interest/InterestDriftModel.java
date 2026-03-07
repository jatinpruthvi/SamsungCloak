package com.evolution.interest;

import com.evolution.model.AppModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class InterestDriftModel {
    private static final Logger logger = LoggerFactory.getLogger(InterestDriftModel.class);

    private final Map<AppModule, Double> visitProbabilities;
    private final Map<AppModule, Double> habitStrengths;
    private final Map<AppModule, Double> boredomScores;
    private final Random random;
    private int currentDay;

    public InterestDriftModel() {
        this.visitProbabilities = new HashMap<>();
        this.habitStrengths = new HashMap<>();
        this.boredomScores = new HashMap<>();
        this.random = new Random();
        this.currentDay = 0;

        initializeProbabilities();
    }

    private void initializeProbabilities() {
        for (AppModule module : AppModule.values()) {
            visitProbabilities.put(module, module.getInitialVisitProbability());
            habitStrengths.put(module, 0.0);
            boredomScores.put(module, 0.0);
        }
        normalizeProbabilities();
    }

    public void updateInterestForDay(int day, Map<AppModule, Integer> moduleVisits) {
        this.currentDay = day;
        
        updateHabitStrengths(moduleVisits);
        updateBoredomScores(moduleVisits);
        applyDrift();
        normalizeProbabilities();

        logProbabilities(day);
    }

    private void updateHabitStrengths(Map<AppModule, Integer> moduleVisits) {
        double habitDecayRate = 0.05;
        double habitGrowthRate = 0.15;

        for (AppModule module : AppModule.values()) {
            double currentStrength = habitStrengths.get(module);
            int visits = moduleVisits.getOrDefault(module, 0);

            double decayedStrength = currentStrength * (1.0 - habitDecayRate);
            double newStrength = decayedStrength + (visits * habitGrowthRate);
            
            habitStrengths.put(module, Math.min(newStrength, 1.0));
        }
    }

    private void updateBoredomScores(Map<AppModule, Integer> moduleVisits) {
        double boredomIncrement = 0.03;
        double boredomDecay = 0.10;

        for (AppModule module : AppModule.values()) {
            double currentBoredom = boredomScores.get(module);
            int visits = moduleVisits.getOrDefault(module, 0);

            if (visits > 0) {
                double newBoredom = currentBoredom + boredomIncrement;
                boredomScores.put(module, Math.min(newBoredom, 1.0));
            } else {
                double decayedBoredom = currentBoredom * (1.0 - boredomDecay);
                boredomScores.put(module, decayedBoredom);
            }
        }
    }

    private void applyDrift() {
        for (AppModule module : AppModule.values()) {
            double baseProbability = module.getInitialVisitProbability();
            double habitStrength = habitStrengths.get(module);
            double boredomScore = boredomScores.get(module);

            double habitBonus = habitStrength * 0.40;
            double boredomPenalty = boredomScore * 0.35;

            double adjustedProbability = baseProbability + habitBonus - boredomPenalty;
            adjustedProbability = Math.max(adjustedProbability, 0.01);

            visitProbabilities.put(module, adjustedProbability);
        }
    }

    private void normalizeProbabilities() {
        double sum = visitProbabilities.values().stream().mapToDouble(Double::doubleValue).sum();
        
        if (sum > 0) {
            for (AppModule module : AppModule.values()) {
                double normalized = visitProbabilities.get(module) / sum;
                visitProbabilities.put(module, normalized);
            }
        }
    }

    public AppModule selectNextModule() {
        double cumulative = 0.0;
        double randomValue = random.nextDouble();

        for (AppModule module : AppModule.values()) {
            cumulative += visitProbabilities.get(module);
            if (randomValue <= cumulative) {
                return module;
            }
        }

        return AppModule.HOME_SCREEN;
    }

    public Map<AppModule, Double> getVisitProbabilities() {
        return new HashMap<>(visitProbabilities);
    }

    public Map<AppModule, Double> getHabitStrengths() {
        return new HashMap<>(habitStrengths);
    }

    public Map<AppModule, Double> getBoredomScores() {
        return new HashMap<>(boredomScores);
    }

    public void triggerNoveltySeeking() {
        logger.info("Novelty seeking triggered - redistributing interest away from habitual modules");

        for (AppModule module : AppModule.values()) {
            double habitStrength = habitStrengths.get(module);
            if (habitStrength > 0.5) {
                double reduction = habitStrength * 0.30;
                double reducedProbability = visitProbabilities.get(module) - reduction;
                visitProbabilities.put(module, Math.max(reducedProbability, 0.01));
                boredomScores.put(module, Math.max(boredomScores.get(module) - 0.20, 0.0));
            }
        }

        normalizeProbabilities();
    }

    public void reinforceHabit(AppModule module, double reinforcementStrength) {
        double currentHabit = habitStrengths.get(module);
        double newHabit = Math.min(currentHabit + reinforcementStrength, 1.0);
        habitStrengths.put(module, newHabit);
        
        logger.debug("Habit reinforced for {}: {:.2f} -> {:.2f}", 
                module.getModuleId(), currentHabit, newHabit);
    }

    private void logProbabilities(int day) {
        if (day % 10 == 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Day %d Probabilities: ", day));
            for (AppModule module : AppModule.values()) {
                double prob = visitProbabilities.get(module);
                sb.append(String.format("%s=%.3f ", module.getModuleId(), prob));
            }
            logger.info(sb.toString());
        }
    }
}
