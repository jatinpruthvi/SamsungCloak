package com.evolution.demo;

import com.evolution.SoakTestOrchestrator;
import com.evolution.lifecycle.LifecycleManager;
import com.evolution.lifecycle.SimulationSession;
import com.evolution.model.AppModule;
import com.evolution.model.LifecycleState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

public class UserEvolutionDemo {
    private static final Logger logger = LoggerFactory.getLogger(UserEvolutionDemo.class);

    public static void main(String[] args) {
        logger.info("╔════════════════════════════════════════════════════════════════╗");
        logger.info("║   USER EVOLUTION MODEL - DEMONSTRATION                        ║");
        logger.info("║   Device: Samsung Galaxy A12 (SM-A125U)                        ║");
        logger.info("║   Purpose: Longitudinal Retention Audit                        ║");
        logger.info("╚════════════════════════════════════════════════════════════════╝");

        demonstrateSkillImprovement();
        demonstrateInterestDrift();
        demonstrateChurnModel();
        demonstrateLifecycleTransitions();
        runFullSimulation();

        logger.info("╔════════════════════════════════════════════════════════════════╗");
        logger.info("║   DEMONSTRATION COMPLETE                                         ║");
        logger.info("╚════════════════════════════════════════════════════════════════╝");
    }

    private static void demonstrateSkillImprovement() {
        logger.info("\n╔════════════════════════════════════════════════════════════════╗");
        logger.info("║   DEMO 1: SKILL IMPROVEMENT (POWER USER CURVE)                ║");
        logger.info("╚════════════════════════════════════════════════════════════════╝");

        LifecycleManager manager = new LifecycleManager(90);

        logger.info("\nDay  | State       | Latency (ms) | Error Rate | Proficiency");
        logger.info("-----|-------------|--------------|------------|-------------");

        try (PrintWriter writer = new PrintWriter(new FileWriter("demo_skill_curve.csv"))) {
            writer.println("day,state,latency_ms,error_rate,proficiency_score");
            
            for (int day = 1; day <= 90; day++) {
                manager.advanceDay();
                
                var metrics = manager.getSkillCurve().getCurrentMetrics();
                double proficiency = manager.getSkillCurve().getSkillProficiencyScore();
                
                writer.printf("%d,%s,%.2f,%.4f,%.4f%n",
                        day, manager.getCurrentState().name(),
                        metrics.getNavigationLatencyMs(),
                        metrics.getErrorRate(),
                        proficiency);
                
                if (day % 15 == 0 || day == 1 || day == 90) {
                    logger.info("%4d | %-11s | %12.2f | %10.4f | %11.2f%%",
                            day,
                            manager.getCurrentState().name(),
                            metrics.getNavigationLatencyMs(),
                            metrics.getErrorRate(),
                            proficiency * 100);
                }
            }
        } catch (IOException e) {
            logger.error("Failed to write skill curve data: {}", e.getMessage());
        }

        logger.info("\n→ Skill curve data exported to demo_skill_curve.csv");
    }

    private static void demonstrateInterestDrift() {
        logger.info("\n╔════════════════════════════════════════════════════════════════╗");
        logger.info("║   DEMO 2: INTEREST DRIFT & HABIT FORMATION                   ║");
        logger.info("╚════════════════════════════════════════════════════════════════╝");

        LifecycleManager manager = new LifecycleManager(60);

        logger.info("\nSimulating module visitation probability evolution...");
        logger.info("Day 7:  Explorer phase - discovering features");
        logger.info("Day 30: Utility phase - habits forming");
        logger.info("Day 60: Power user - efficient patterns");

        try (PrintWriter writer = new PrintWriter(new FileWriter("demo_interest_drift.csv"))) {
            writer.println("day,module,probability,habit_strength,boredom_score");
            
            for (int day = 1; day <= 60; day++) {
                manager.advanceDay();
                
                for (int i = 0; i < 3; i++) {
                    SimulationSession session = manager.generateSession();
                    if (session != null) {
                        for (AppModule module : session.getVisitedModules()) {
                            writer.printf("%d,%s,%.4f,%.4f,%.4f%n",
                                    day, module.getModuleId(),
                                    manager.getInterestModel().getVisitProbabilities().get(module),
                                    manager.getInterestModel().getHabitStrengths().get(module),
                                    manager.getInterestModel().getBoredomScores().get(module));
                        }
                    }
                }

                if (day == 7 || day == 30 || day == 60) {
                    Map<AppModule, Double> probs = manager.getInterestModel().getVisitProbabilities();
                    logger.info("\nDay {} Top Modules:", day);
                    probs.entrySet().stream()
                            .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                            .limit(3)
                            .forEach(e -> logger.info("  {}: {:.2f}%",
                                    e.getKey().getModuleId(), e.getValue() * 100));
                }
            }
        } catch (IOException e) {
            logger.error("Failed to write interest drift data: {}", e.getMessage());
        }

        logger.info("\n→ Interest drift data exported to demo_interest_drift.csv");
    }

    private static void demonstrateChurnModel() {
        logger.info("\n╔════════════════════════════════════════════════════════════════╗");
        logger.info("║   DEMO 3: CHURN MODEL & DROP-OFF PATTERNS                    ║");
        logger.info("╚════════════════════════════════════════════════════════════════╝");

        LifecycleManager manager = new LifecycleManager(90);

        logger.info("\nDay  | State       | Churn Prob. | Active Triggers | Retention Factors");
        logger.info("-----|-------------|-------------|-----------------|-------------------");

        try (PrintWriter writer = new PrintWriter(new FileWriter("demo_churn_model.csv"))) {
            writer.println("day,state,churn_probability,triggers_count,factors_count");
            
            for (int day = 1; day <= 90; day++) {
                manager.advanceDay();
                
                double churnProb = manager.getChurnModel().getChurnProbability();
                int triggers = manager.getChurnModel().getActiveTriggers().size();
                int factors = manager.getChurnModel().getActiveFactors().size();
                
                writer.printf("%d,%s,%.6f,%d,%d%n",
                        day, manager.getCurrentState().name(),
                        churnProb, triggers, factors);
                
                if (day % 15 == 0 || day == 1 || day == 90) {
                    String triggerInfo = triggers > 0 ? 
                            manager.getChurnModel().getActiveTriggers().get(0).getType() : "None";
                    
                    logger.info("%4d | %-11s | %11.6f | %-15s | %d factors",
                            day,
                            manager.getCurrentState().name(),
                            churnProb,
                            triggerInfo,
                            factors);
                }

                if (manager.getChurnModel().hasChurned()) {
                    logger.warn("\n⚠ USER CHURNED ON DAY {}", day);
                    break;
                }
            }
        } catch (IOException e) {
            logger.error("Failed to write churn model data: {}", e.getMessage());
        }

        logger.info("\n→ Churn model data exported to demo_churn_model.csv");
    }

    private static void demonstrateLifecycleTransitions() {
        logger.info("\n╔════════════════════════════════════════════════════════════════╗");
        logger.info("║   DEMO 4: LIFECYCLE TRANSITIONS                              ║");
        logger.info("╚════════════════════════════════════════════════════════════════╝");

        logger.info("\nLifecycle Phase Transitions:");
        logger.info("┌─────────────┬──────┬──────────────────────────────────────────┐");
        logger.info("│ Phase       │ Days │ Description                             │");
        logger.info("├─────────────┼──────┼──────────────────────────────────────────┤");
        logger.info("│ ONBOARDING  │ 0-7  │ Learning basics, high error rates        │");
        logger.info("│ EXPLORER    │ 7-30 │ Discovering features, forming habits    │");
        logger.info("│ UTILITY     │ 30-60│ Established routines, efficient use      │");
        logger.info("│ POWER_USER  │ 60-90│ Mastered app, minimal errors             │");
        logger.info("└─────────────┴──────┴──────────────────────────────────────────┘");

        logger.info("\nTransition Characteristics:");
        
        for (LifecycleState state : LifecycleState.values()) {
            if (state == LifecycleState.CHURNED) continue;
            
            switch (state) {
                case ONBOARDING:
                    logger.info("• {} - Users are tentative, explore broadly", state.name());
                    break;
                case EXPLORER:
                    logger.info("• {} - Start developing preferences, novelty-seeking", state.name());
                    break;
                case UTILITY:
                    logger.info("• {} - Habits solidify, usage becomes purposeful", state.name());
                    break;
                case POWER_USER:
                    logger.info("• {} - Efficient patterns, high retention potential", state.name());
                    break;
            }
        }

        logger.info("\nCritical Milestones:");
        logger.info("  → Day 7:  Onboarding complete - retention risk drops");
        logger.info("  → Day 30: Explorer to Utility - churn risk increases temporarily");
        logger.info("  → Day 60: Power user achieved - maximum loyalty");
    }

    private static void runFullSimulation() {
        logger.info("\n╔════════════════════════════════════════════════════════════════╗");
        logger.info("║   DEMO 5: FULL 30-DAY SIMULATION                              ║");
        logger.info("╚════════════════════════════════════════════════════════════════╝");

        logger.info("\nRunning comprehensive 30-day simulation...");
        logger.info("(This demonstrates all components working together)");
        
        SoakTestOrchestrator orchestrator = new SoakTestOrchestrator(30, "SM-A125U-DEMO");
        orchestrator.runSimulation();

        logger.info("\n→ Full simulation complete");
        logger.info("→ Check generated CSV files for detailed metrics");
    }
}
