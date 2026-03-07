package com.evolution;

import com.evolution.model.LifecycleState;
import com.evolution.skill.SkillImprovementCurve;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SkillImprovementCurveTest {

    private SkillImprovementCurve skillCurve;
    private static final int TEST_DAYS = 90;

    @BeforeEach
    void setUp() {
        skillCurve = new SkillImprovementCurve(TEST_DAYS);
    }

    @Test
    void testInitialMetrics() {
        var metrics = skillCurve.getCurrentMetrics();
        
        assertTrue(metrics.getNavigationLatencyMs() > 2000, 
                "Initial latency should be high for new users");
        assertTrue(metrics.getErrorRate() > 0.10, 
                "Initial error rate should be high");
        assertEquals(0, skillCurve.getSkillProficiencyScore(), 0.05,
                "Proficiency should start near 0");
    }

    @Test
    void testSkillImprovementOverTime() {
        for (int day = 1; day <= 90; day++) {
            LifecycleState state = LifecycleState.getStateForDay(day);
            skillCurve.updateSkillForDay(day, state, 3);
        }

        var finalMetrics = skillCurve.getCurrentMetrics();
        double proficiency = skillCurve.getSkillProficiencyScore();

        assertTrue(finalMetrics.getNavigationLatencyMs() < 1000, 
                "Latency should decrease significantly after 90 days");
        assertTrue(finalMetrics.getErrorRate() < 0.05, 
                "Error rate should be low after 90 days");
        assertTrue(proficiency > 0.70, 
                "Proficiency should be high after 90 days");
    }

    @Test
    void testLearningRateVariesByLifecyclePhase() {
        double initialLatency = skillCurve.getCurrentMetrics().getNavigationLatencyMs();
        
        skillCurve.updateSkillForDay(5, LifecycleState.ONBOARDING, 3);
        double onboardingLatency = skillCurve.getCurrentMetrics().getNavigationLatencyMs();
        double onboardingImprovement = initialLatency - onboardingLatency;

        skillCurve = new SkillImprovementCurve(TEST_DAYS);
        skillCurve.updateSkillForDay(40, LifecycleState.UTILITY, 3);
        double utilityLatency = skillCurve.getCurrentMetrics().getNavigationLatencyMs();
        double utilityImprovement = initialLatency - utilityLatency;

        assertTrue(onboardingImprovement > utilityImprovement, 
                "Learning should be faster during onboarding phase");
    }

    @Test
    void testLatencySpike() {
        skillCurve.updateSkillForDay(45, LifecycleState.UTILITY, 5);
        double normalLatency = skillCurve.getCurrentMetrics().getNavigationLatencyMs();

        skillCurve.applyLatencySpike(3.0);
        double spikedLatency = skillCurve.getCurrentMetrics().getNavigationLatencyMs();

        assertTrue(spikedLatency > normalLatency * 2.5, 
                "Latency spike should significantly increase latency");

        skillCurve.recoverFromLatencySpike(0.5);
        double recoveredLatency = skillCurve.getCurrentMetrics().getNavigationLatencyMs();

        assertTrue(recoveredLatency < spikedLatency, 
                "Latency should recover after spike");
    }

    @Test
    void testProficiencyScoreBounds() {
        skillCurve.updateSkillForDay(1, LifecycleState.ONBOARDING, 1);
        double minProficiency = skillCurve.getSkillProficiencyScore();

        for (int day = 1; day <= 90; day++) {
            LifecycleState state = LifecycleState.getStateForDay(day);
            skillCurve.updateSkillForDay(day, state, 5);
        }
        double maxProficiency = skillCurve.getSkillProficiencyScore();

        assertTrue(minProficiency >= 0.0, "Proficiency should not be negative");
        assertTrue(maxProficiency <= 1.0, "Proficiency should not exceed 1.0");
        assertTrue(maxProficiency > minProficiency, "Proficiency should increase over time");
    }

    @Test
    void testMinimumThresholds() {
        for (int day = 1; day <= 200; day++) {
            LifecycleState state = LifecycleState.getStateForDay(Math.min(day, 90));
            skillCurve.updateSkillForDay(Math.min(day, 90), state, 5);
        }

        var metrics = skillCurve.getCurrentMetrics();

        assertTrue(metrics.getNavigationLatencyMs() >= 400, 
                "Latency should not drop below minimum threshold");
        assertTrue(metrics.getErrorRate() >= 0.005, 
                "Error rate should not drop below minimum threshold");
    }
}
