package com.evolution.interest;

import com.evolution.model.AppModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MarkovTransitionMatrix {
    private static final Logger logger = LoggerFactory.getLogger(MarkovTransitionMatrix.class);

    private final Map<AppModule, Map<AppModule, Double>> transitionMatrix;
    private final Random random;

    public MarkovTransitionMatrix() {
        this.transitionMatrix = new HashMap<>();
        this.random = new Random();
        initializeMatrix();
    }

    private void initializeMatrix() {
        for (AppModule fromModule : AppModule.values()) {
            Map<AppModule, Double> transitions = new HashMap<>();
            
            switch (fromModule) {
                case HOME_SCREEN:
                    transitions.put(AppModule.SEARCH, 0.20);
                    transitions.put(AppModule.PROFILE, 0.10);
                    transitions.put(AppModule.CONTENT_A, 0.30);
                    transitions.put(AppModule.CONTENT_B, 0.20);
                    transitions.put(AppModule.NOTIFICATIONS, 0.15);
                    transitions.put(AppModule.SETTINGS, 0.05);
                    break;
                case SEARCH:
                    transitions.put(AppModule.HOME_SCREEN, 0.40);
                    transitions.put(AppModule.CONTENT_A, 0.25);
                    transitions.put(AppModule.CONTENT_B, 0.25);
                    transitions.put(AppModule.FEATURE_X, 0.10);
                    break;
                case CONTENT_A:
                    transitions.put(AppModule.HOME_SCREEN, 0.50);
                    transitions.put(AppModule.CONTENT_B, 0.20);
                    transitions.put(AppModule.PROFILE, 0.15);
                    transitions.put(AppModule.NOTIFICATIONS, 0.15);
                    break;
                case CONTENT_B:
                    transitions.put(AppModule.HOME_SCREEN, 0.55);
                    transitions.put(AppModule.CONTENT_A, 0.25);
                    transitions.put(AppModule.PROFILE, 0.20);
                    break;
                case PROFILE:
                    transitions.put(AppModule.HOME_SCREEN, 0.60);
                    transitions.put(AppModule.SETTINGS, 0.25);
                    transitions.put(AppModule.NOTIFICATIONS, 0.15);
                    break;
                case SETTINGS:
                    transitions.put(AppModule.HOME_SCREEN, 0.70);
                    transitions.put(AppModule.PROFILE, 0.30);
                    break;
                case NOTIFICATIONS:
                    transitions.put(AppModule.HOME_SCREEN, 0.45);
                    transitions.put(AppModule.CONTENT_A, 0.25);
                    transitions.put(AppModule.CONTENT_B, 0.20);
                    transitions.put(AppModule.PROFILE, 0.10);
                    break;
                case FEATURE_X:
                    transitions.put(AppModule.HOME_SCREEN, 0.40);
                    transitions.put(AppModule.SEARCH, 0.30);
                    transitions.put(AppModule.FEATURE_Y, 0.30);
                    break;
                case FEATURE_Y:
                    transitions.put(AppModule.HOME_SCREEN, 0.50);
                    transitions.put(AppModule.SEARCH, 0.25);
                    transitions.put(AppModule.FEATURE_X, 0.25);
                    break;
                default:
                    transitions.put(AppModule.HOME_SCREEN, 1.0);
            }
            
            normalizeTransitions(transitions);
            transitionMatrix.put(fromModule, transitions);
        }
    }

    private void normalizeTransitions(Map<AppModule, Double> transitions) {
        double sum = transitions.values().stream().mapToDouble(Double::doubleValue).sum();
        if (sum > 0) {
            for (AppModule module : transitions.keySet()) {
                transitions.put(module, transitions.get(module) / sum);
            }
        }
    }

    public void adaptToHabits(Map<AppModule, Double> habitStrengths, Map<AppModule, Double> boredomScores) {
        logger.debug("Adapting Markov matrix to habits and boredom");

        for (AppModule fromModule : AppModule.values()) {
            Map<AppModule, Double> transitions = transitionMatrix.get(fromModule);
            double fromHabit = habitStrengths.getOrDefault(fromModule, 0.0);

            for (AppModule toModule : transitions.keySet()) {
                double toHabit = habitStrengths.getOrDefault(toModule, 0.0);
                double toBoredom = boredomScores.getOrDefault(toModule, 0.0);

                double baseProbability = transitions.get(toModule);
                double habitBonus = (fromHabit + toHabit) * 0.20;
                double boredomPenalty = toBoredom * 0.25;

                double adaptedProbability = baseProbability + habitBonus - boredomPenalty;
                adaptedProbability = Math.max(adaptedProbability, 0.01);
                
                transitions.put(toModule, adaptedProbability);
            }

            normalizeTransitions(transitions);
        }
    }

    public AppModule getNextState(AppModule currentState) {
        Map<AppModule, Double> transitions = transitionMatrix.get(currentState);
        if (transitions == null || transitions.isEmpty()) {
            return AppModule.HOME_SCREEN;
        }

        double cumulative = 0.0;
        double randomValue = random.nextDouble();

        for (Map.Entry<AppModule, Double> entry : transitions.entrySet()) {
            cumulative += entry.getValue();
            if (randomValue <= cumulative) {
                return entry.getKey();
            }
        }

        return AppModule.HOME_SCREEN;
    }

    public double getTransitionProbability(AppModule from, AppModule to) {
        Map<AppModule, Double> transitions = transitionMatrix.get(from);
        if (transitions != null) {
            return transitions.getOrDefault(to, 0.0);
        }
        return 0.0;
    }

    public void reinforcePath(AppModule from, AppModule to, double reinforcement) {
        Map<AppModule, Double> transitions = transitionMatrix.get(from);
        if (transitions != null) {
            double currentProb = transitions.getOrDefault(to, 0.0);
            double newProb = Math.min(currentProb + reinforcement, 0.90);
            transitions.put(to, newProb);
            normalizeTransitions(transitions);
            
            logger.debug("Reinforced path {} -> {}: {:.3f} -> {:.3f}", 
                    from.getModuleId(), to.getModuleId(), currentProb, newProb);
        }
    }

    public void decayAllTransitions(double decayRate) {
        for (AppModule fromModule : transitionMatrix.keySet()) {
            Map<AppModule, Double> transitions = transitionMatrix.get(fromModule);
            for (AppModule toModule : transitions.keySet()) {
                double currentProb = transitions.get(toModule);
                double decayedProb = currentProb * (1.0 - decayRate);
                transitions.put(toModule, Math.max(decayedProb, 0.01));
            }
            normalizeTransitions(transitions);
        }
    }
}
