package com.evolution;

import com.evolution.lifecycle.LifecycleManager;
import com.evolution.lifecycle.SimulationSession;
import com.evolution.model.LifecycleState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class SoakTestOrchestrator {
    private static final Logger logger = LoggerFactory.getLogger(SoakTestOrchestrator.class);

    private final int totalDays;
    private final LifecycleManager lifecycleManager;
    private final List<SimulationSession> allSessions;
    private final String deviceId;

    public SoakTestOrchestrator(int totalDays, String deviceId) {
        this.totalDays = totalDays;
        this.lifecycleManager = new LifecycleManager(totalDays);
        this.allSessions = new ArrayList<>();
        this.deviceId = deviceId;

        logger.info("SoakTestOrchestrator initialized for device {} with {} day duration", deviceId, totalDays);
    }

    public void runSimulation() {
        logger.info("========================================");
        logger.info("Starting {}-day User Evolution Soak Test", totalDays);
        logger.info("Device ID: {}", deviceId);
        logger.info("Start Time: {}", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        logger.info("========================================");

        while (lifecycleManager.isSimulationActive()) {
            boolean dayAdvanced = lifecycleManager.advanceDay();

            if (!dayAdvanced) {
                break;
            }

            int sessionsForDay = generateSessionCountForDay();

            for (int i = 0; i < sessionsForDay; i++) {
                SimulationSession session = lifecycleManager.generateSession();
                if (session != null) {
                    allSessions.add(session);
                }
            }

            simulateRandomEvents();

            if (lifecycleManager.getCurrentDay() % 10 == 0) {
                lifecycleManager.logCurrentState();
                exportDailyMetrics(lifecycleManager.getCurrentDay());
            }

            if (lifecycleManager.getChurnModel().hasChurned()) {
                logger.warn("=== USER CHURNED ON DAY {} ===", lifecycleManager.getCurrentDay());
                logFinalMetrics();
                break;
            }
        }

        if (!lifecycleManager.getChurnModel().hasChurned()) {
            logger.info("=== SIMULATION COMPLETED SUCCESSFULLY ===");
            logFinalMetrics();
        }

        exportFinalReport();
    }

    private int generateSessionCountForDay() {
        LifecycleState state = lifecycleManager.getCurrentState();
        double randomFactor = Math.random();
        
        return switch (state) {
            case ONBOARDING -> randomFactor < 0.3 ? 2 : (randomFactor < 0.7 ? 3 : 4);
            case EXPLORER -> randomFactor < 0.2 ? 3 : (randomFactor < 0.6 ? 4 : 5);
            case UTILITY -> randomFactor < 0.15 ? 4 : (randomFactor < 0.5 ? 5 : 6);
            case POWER_USER -> randomFactor < 0.1 ? 5 : (randomFactor < 0.4 ? 6 : 7);
            default -> 3;
        };
    }

    private void simulateRandomEvents() {
        double eventChance = Math.random();

        if (eventChance < 0.03) {
            double spikeMultiplier = 2.0 + Math.random() * 2.0;
            int duration = 1 + (int) (Math.random() * 2);
            lifecycleManager.simulateLatencyIssue(spikeMultiplier, duration);
            logger.warn("Random latency event triggered: {:.2f}x for {} days", spikeMultiplier, duration);
        } else if (eventChance > 0.97) {
            lifecycleManager.recoverFromLatencyIssue();
            logger.info("Random recovery event triggered");
        }
    }

    private void logFinalMetrics() {
        logger.info("========================================");
        logger.info("FINAL METRICS SUMMARY");
        logger.info("========================================");
        logger.info("Total Days: {}", lifecycleManager.getCurrentDay());
        logger.info("Final State: {}", lifecycleManager.getCurrentState().name());
        logger.info("Total Sessions: {}", allSessions.size());
        logger.info("Final Skill Proficiency: {:.2f}", lifecycleManager.getSkillCurve().getSkillProficiencyScore());
        logger.info("Final Churn Probability: {:.4f}", lifecycleManager.getChurnModel().getChurnProbability());
        logger.info("User Churned: {}", lifecycleManager.getChurnModel().hasChurned());
        logger.info("========================================");
    }

    private void exportDailyMetrics(int day) {
        String filename = String.format("metrics_day_%03d.csv", day);
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("day,lifecycle_state,skill_proficiency,latency_ms,error_rate,churn_probability,sessions_today");
            
            var metrics = lifecycleManager.getSkillCurve().getCurrentMetrics();
            writer.printf("%d,%s,%.4f,%.2f,%.4f,%.4f,%d%n",
                    day,
                    lifecycleManager.getCurrentState().name(),
                    lifecycleManager.getSkillCurve().getSkillProficiencyScore(),
                    metrics.getNavigationLatencyMs(),
                    metrics.getErrorRate(),
                    lifecycleManager.getChurnModel().getChurnProbability(),
                    (int) allSessions.stream().filter(s -> s.getDay() == day).count());
            
            logger.debug("Exported daily metrics to {}", filename);
        } catch (IOException e) {
            logger.error("Failed to export daily metrics: {}", e.getMessage());
        }
    }

    private void exportFinalReport() {
        String filename = String.format("soak_test_report_%s_%s.csv", 
                deviceId, 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("session_id,day,lifecycle_state,duration_seconds,total_interactions,latency_ms,error_rate");
            
            for (int i = 0; i < allSessions.size(); i++) {
                SimulationSession session = allSessions.get(i);
                var metrics = session.getMetrics();
                writer.printf("%d,%d,%s,%d,%d,%.2f,%.4f%n",
                        i + 1,
                        session.getDay(),
                        session.getLifecycleState().name(),
                        session.getDurationSeconds(),
                        session.getTotalInteractions(),
                        metrics.getNavigationLatencyMs(),
                        metrics.getErrorRate());
            }
            
            logger.info("Exported final report to {}", filename);
        } catch (IOException e) {
            logger.error("Failed to export final report: {}", e.getMessage());
        }
    }

    public static void main(String[] args) {
        int days = 90;
        String deviceId = "SM-A125U";
        
        if (args.length > 0) {
            try {
                days = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                logger.warn("Invalid days argument, using default: {}", days);
            }
        }
        
        if (args.length > 1) {
            deviceId = args[1];
        }

        SoakTestOrchestrator orchestrator = new SoakTestOrchestrator(days, deviceId);
        orchestrator.runSimulation();
    }
}
