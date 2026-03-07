package com.evolution;

import com.evolution.interest.InterestDriftModel;
import com.evolution.model.AppModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InterestDriftModelTest {

    private InterestDriftModel interestModel;

    @BeforeEach
    void setUp() {
        interestModel = new InterestDriftModel();
    }

    @Test
    void testInitialProbabilitiesSumToOne() {
        Map<AppModule, Double> probs = interestModel.getVisitProbabilities();
        double sum = probs.values().stream().mapToDouble(Double::doubleValue).sum();
        
        assertEquals(1.0, sum, 0.001, "Probabilities should sum to 1.0");
    }

    @Test
    void testHabitsFormWithRepeatedVisits() {
        Map<AppModule, Integer> visits = new HashMap<>();
        visits.put(AppModule.CONTENT_A, 10);
        visits.put(AppModule.HOME_SCREEN, 5);
        
        interestModel.updateInterestForDay(10, visits);
        
        Map<AppModule, Double> habits = interestModel.getHabitStrengths();
        assertTrue(habits.get(AppModule.CONTENT_A) > habits.get(AppModule.SETTINGS), 
                "Repeatedly visited module should have higher habit strength");
    }

    @Test
    void testBoredomIncreasesWithOveruse() {
        Map<AppModule, Integer> visits = new HashMap<>();
        visits.put(AppModule.CONTENT_A, 15);
        
        interestModel.updateInterestForDay(10, visits);
        double initialBoredom = interestModel.getBoredomScores().get(AppModule.CONTENT_A);
        
        visits.put(AppModule.CONTENT_A, 15);
        interestModel.updateInterestForDay(11, visits);
        double increasedBoredom = interestModel.getBoredomScores().get(AppModule.CONTENT_A);
        
        assertTrue(increasedBoredom > initialBoredom, 
                "Boredom should increase with repeated visits");
    }

    @Test
    void testBoredomDecreasesWithoutVisits() {
        Map<AppModule, Integer> visits = new HashMap<>();
        visits.put(AppModule.CONTENT_A, 10);
        
        interestModel.updateInterestForDay(10, visits);
        double initialBoredom = interestModel.getBoredomScores().get(AppModule.CONTENT_A);
        
        Map<AppModule, Integer> noVisits = new HashMap<>();
        interestModel.updateInterestForDay(11, noVisits);
        interestModel.updateInterestForDay(12, noVisits);
        double decreasedBoredom = interestModel.getBoredomScores().get(AppModule.CONTENT_A);
        
        assertTrue(decreasedBoredom < initialBoredom, 
                "Boredom should decrease without visits");
    }

    @Test
    void testProbabilitiesShiftWithHabits() {
        Map<AppModule, Double> initialProbs = new HashMap<>(interestModel.getVisitProbabilities());
        
        Map<AppModule, Integer> visits = new HashMap<>();
        visits.put(AppModule.CONTENT_A, 20);
        visits.put(AppModule.CONTENT_B, 10);
        
        for (int day = 1; day <= 10; day++) {
            interestModel.updateInterestForDay(day, visits);
        }
        
        Map<AppModule, Double> shiftedProbs = interestModel.getVisitProbabilities();
        
        assertTrue(shiftedProbs.get(AppModule.CONTENT_A) > initialProbs.get(AppModule.CONTENT_A), 
                "Highly visited module probability should increase");
    }

    @Test
    void testNoveltySeekingRedistributesInterest() {
        Map<AppModule, Integer> visits = new HashMap<>();
        visits.put(AppModule.CONTENT_A, 30);
        
        for (int day = 1; day <= 15; day++) {
            interestModel.updateInterestForDay(day, visits);
        }
        
        Map<AppModule, Double> preNoveltyProbs = interestModel.getVisitProbabilities();
        double contentAProbBefore = preNoveltyProbs.get(AppModule.CONTENT_A);
        
        interestModel.triggerNoveltySeeking();
        
        Map<AppModule, Double> postNoveltyProbs = interestModel.getVisitProbabilities();
        double contentAProbAfter = postNoveltyProbs.get(AppModule.CONTENT_A);
        
        assertTrue(contentAProbAfter < contentAProbBefore, 
                "Novelty seeking should reduce probability of habitual modules");
    }

    @Test
    void testReinforceHabit() {
        double initialHabit = interestModel.getHabitStrengths().get(AppModule.SEARCH);
        
        interestModel.reinforceHabit(AppModule.SEARCH, 0.3);
        
        double reinforcedHabit = interestModel.getHabitStrengths().get(AppModule.SEARCH);
        assertTrue(reinforcedHabit > initialHabit, 
                "Reinforcing should increase habit strength");
        assertTrue(reinforcedHabit <= 1.0, 
                "Habit strength should not exceed 1.0");
    }

    @Test
    void testProbabilitiesStayNormalizedAfterUpdates() {
        Map<AppModule, Integer> visits = new HashMap<>();
        visits.put(AppModule.CONTENT_A, 15);
        visits.put(AppModule.CONTENT_B, 10);
        visits.put(AppModule.PROFILE, 5);
        
        for (int day = 1; day <= 20; day++) {
            interestModel.updateInterestForDay(day, visits);
            
            Map<AppModule, Double> probs = interestModel.getVisitProbabilities();
            double sum = probs.values().stream().mapToDouble(Double::doubleValue).sum();
            assertEquals(1.0, sum, 0.001, 
                    "Probabilities should always sum to 1.0 after update");
        }
    }

    @Test
    void testSelectNextModuleReturnsValidModule() {
        AppModule module = interestModel.selectNextModule();
        
        assertNotNull(module, "Should return a valid module");
        assertTrue(module != AppModule.CHURNED, "Should not return CHURNED state");
    }

    @Test
    void testHabitAndBoredomBounds() {
        Map<AppModule, Integer> visits = new HashMap<>();
        visits.put(AppModule.CONTENT_A, 100);
        
        for (int day = 1; day <= 50; day++) {
            interestModel.updateInterestForDay(day, visits);
        }
        
        Map<AppModule, Double> habits = interestModel.getHabitStrengths();
        Map<AppModule, Double> boredomScores = interestModel.getBoredomScores();
        
        habits.values().forEach(h -> {
            assertTrue(h >= 0.0, "Habit strength should not be negative");
            assertTrue(h <= 1.0, "Habit strength should not exceed 1.0");
        });
        
        boredomScores.values().forEach(b -> {
            assertTrue(b >= 0.0, "Boredom score should not be negative");
            assertTrue(b <= 1.0, "Boredom score should not exceed 1.0");
        });
    }
}
