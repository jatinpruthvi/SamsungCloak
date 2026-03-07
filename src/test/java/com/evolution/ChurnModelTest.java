package com.evolution;

import com.evolution.churn.ChurnModel;
import com.evolution.model.LifecycleState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChurnModelTest {

    private ChurnModel churnModel;

    @BeforeEach
    void setUp() {
        churnModel = new ChurnModel();
    }

    @Test
    void testInitialChurnProbability() {
        churnModel.updateChurnProbability(1, LifecycleState.ONBOARDING, 2500.0, 0.15, 3, 120.0);
        
        assertTrue(churnModel.getChurnProbability() > 0.0, 
                "Churn probability should be positive");
        assertTrue(churnModel.getChurnProbability() < 0.1, 
                "Initial churn probability should be low");
        assertFalse(churnModel.hasChurned(), 
                "User should not have churned initially");
    }

    @Test
    void testHighLatencyIncreasesChurnRisk() {
        churnModel.updateChurnProbability(30, LifecycleState.UTILITY, 6000.0, 0.02, 5, 180.0);
        
        assertTrue(churnModel.getChurnProbability() > 0.02, 
                "High latency should increase churn probability");
        
        var triggers = churnModel.getActiveTriggers();
        assertTrue(triggers.stream().anyMatch(t -> t.getType().equals("HIGH_LATENCY")), 
                "High latency trigger should be active");
    }

    @Test
    void testHighErrorRateIncreasesChurnRisk() {
        churnModel.updateChurnProbability(45, LifecycleState.UTILITY, 800.0, 0.15, 5, 180.0);
        
        assertTrue(churnModel.getChurnProbability() > 0.05, 
                "High error rate should increase churn probability");
        
        var triggers = churnModel.getActiveTriggers();
        assertTrue(triggers.stream().anyMatch(t -> t.getType().equals("HIGH_ERROR_RATE")), 
                "High error rate trigger should be active");
    }

    @Test
    void testLowEngagementIncreasesChurnRisk() {
        for (int day = 1; day <= 6; day++) {
            churnModel.updateChurnProbability(day, LifecycleState.UTILITY, 800.0, 0.01, 0, 10.0);
        }
        
        assertTrue(churnModel.getChurnProbability() > 0.05, 
                "Consecutive low engagement days should increase churn probability");
        
        var triggers = churnModel.getActiveTriggers();
        assertTrue(triggers.stream().anyMatch(t -> t.getType().equals("LOW_ENGAGEMENT")), 
                "Low engagement trigger should be active");
    }

    @Test
    void testHighEngagementReducesChurnRisk() {
        churnModel.updateChurnProbability(30, LifecycleState.UTILITY, 800.0, 0.02, 7, 300.0);
        
        var factors = churnModel.getActiveFactors();
        assertTrue(factors.stream().anyMatch(f -> f.getType().equals("HIGH_ENGAGEMENT")), 
                "High engagement should create retention factor");
    }

    @Test
    void testLifecycleMilestoneEffects() {
        churnModel.updateChurnProbability(7, LifecycleState.EXPLORER, 2000.0, 0.10, 4, 150.0);
        
        var factors = churnModel.getActiveFactors();
        assertTrue(factors.stream().anyMatch(f -> f.getType().equals("ONBOARDING_COMPLETE")), 
                "Onboarding completion should reduce churn risk");
    }

    @Test
    void testLatencySpikeEffect() {
        churnModel.updateChurnProbability(30, LifecycleState.UTILITY, 800.0, 0.02, 5, 180.0);
        double baseProbability = churnModel.getChurnProbability();
        
        churnModel.recordLatencySpike(3.0);
        churnModel.updateChurnProbability(31, LifecycleState.UTILITY, 2400.0, 0.02, 5, 180.0);
        
        assertTrue(churnModel.getChurnProbability() > baseProbability, 
                "Latency spike should increase churn probability");
    }

    @Test
    void testPositiveExperienceEffect() {
        churnModel.updateChurnProbability(30, LifecycleState.UTILITY, 800.0, 0.02, 5, 180.0);
        double baseProbability = churnModel.getChurnProbability();
        
        churnModel.recordPositiveExperience();
        
        assertTrue(churnModel.getChurnProbability() < baseProbability, 
                "Positive experience should reduce churn probability");
    }

    @Test
    void testChurnProbabilityBounds() {
        churnModel.updateChurnProbability(1, LifecycleState.ONBOARDING, 10000.0, 1.0, 0, 0.0);
        
        assertTrue(churnModel.getChurnProbability() >= 0.0, 
                "Churn probability should not be negative");
        assertTrue(churnModel.getChurnProbability() <= 1.0, 
                "Churn probability should not exceed 1.0");
    }

    @Test
    void testChurnEvent() {
        churnModel.updateChurnProbability(1, LifecycleState.ONBOARDING, 10000.0, 1.0, 0, 0.0);
        
        churnModel.evaluateChurn();
        
        assertTrue(churnModel.hasChurned(), 
                "High churn probability should trigger churn event");
    }

    @Test
    void testResetChurnState() {
        churnModel.updateChurnProbability(1, LifecycleState.ONBOARDING, 10000.0, 1.0, 0, 0.0);
        churnModel.evaluateChurn();
        
        assertTrue(churnModel.hasChurned());
        
        churnModel.resetChurnState();
        
        assertFalse(churnModel.hasChurned(), 
                "Reset should clear churned state");
        assertTrue(churnModel.getChurnProbability() < 0.01, 
                "Reset should return to base probability");
    }
}
