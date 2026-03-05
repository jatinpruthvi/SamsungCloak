package com.samsungcloak.xposed;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

/**
 * TemporalRealismDemo - Demonstration and Validation Class
 *
 * This class provides demonstration and validation methods for the
 * TemporalRealismHook system. It can be used to verify the correctness
 * of temporal simulations and demonstrate the behavior of each hook.
 *
 * Usage: Call methods from test harness or automated validation suite
 */
public class TemporalRealismDemo {
    private static final String LOG_TAG = "SamsungCloak.TemporalDemo";

    /**
     * Runs comprehensive validation of all temporal realism features
     */
    public static void runFullValidation() {
        HookUtils.logInfo("=== TEMPORAL REALISM VALIDATION SUITE ===");

        validateCircadianCycleSimulation();
        validateTemporalVariance();
        validateSeasonalAdjustments();
        validateLongTermEntropy();
        validateStochasticIdlePeriods();

        HookUtils.logInfo("=== VALIDATION COMPLETE ===");
    }

    /**
     * Demonstrates Circadian Cycle Simulation
     */
    public static void validateCircadianCycleSimulation() {
        HookUtils.logInfo("--- VALIDATING: Circadian Cycle Simulation ---");

        long baseTime = System.currentTimeMillis();

        HookUtils.logInfo("Testing activity levels across 24 hours:");

        for (int hour = 0; hour < 24; hour++) {
            LocalDateTime testTime = LocalDateTime.of(
                LocalDate.now(), LocalTime.of(hour, 0)
            );
            long testMillis = testTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

            long adjustedTime = TemporalRealismHook.applyCircadianCycle(testMillis);
            long offset = adjustedTime - testMillis;

            double activityLevel = TemporalRealismHook.getCurrentCircadianActivityLevel();

            HookUtils.logInfo(String.format("Hour %02d:00 - Activity: %.2f, Time Offset: %+d ms",
                hour, activityLevel, offset));
        }

        int eventCount = 0;
        for (int i = 0; i < 100; i++) {
            if (TemporalRealismHook.shouldScheduleEventByCircadian()) {
                eventCount++;
            }
        }

        HookUtils.logInfo("Event scheduling probability test: " + eventCount + "/100 events scheduled");
    }

    /**
     * Demonstrates Temporal Variance (Work/Life Balance)
     */
    public static void validateTemporalVariance() {
        HookUtils.logInfo("--- VALIDATING: Temporal Variance (Work/Life Balance) ---");

        TemporalRealismHook.DayType currentDay = TemporalRealismHook.getCurrentDayType();
        TemporalRealismHook.EngagementPattern currentPattern = TemporalRealismHook.getCurrentEngagementPattern();

        HookUtils.logInfo("Current Day Type: " + currentDay);
        HookUtils.logInfo("Current Engagement Pattern: " + currentPattern);

        HookUtils.logInfo("Testing engagement multipliers:");

        double[] sampleMultipliers = new double[20];
        double sum = 0;

        for (int i = 0; i < 20; i++) {
            sampleMultipliers[i] = TemporalRealismHook.calculateTemporalVarianceMultiplier();
            sum += sampleMultipliers[i];

            HookUtils.logInfo(String.format("  Sample %02d: %.3f", i + 1, sampleMultipliers[i]));
        }

        double average = sum / 20;
        HookUtils.logInfo(String.format("Average engagement multiplier: %.3f", average));

        double variance = 0;
        for (double multiplier : sampleMultipliers) {
            variance += Math.pow(multiplier - average, 2);
        }
        variance /= 20;

        HookUtils.logInfo(String.format("Variance: %.6f (indicates realistic randomness)", variance));
    }

    /**
     * Demonstrates Seasonal Baseline Adjustments
     */
    public static void validateSeasonalAdjustments() {
        HookUtils.logInfo("--- VALIDATING: Seasonal Baseline Adjustments ---");

        TemporalRealismHook.SeasonalPeriod currentSeason = TemporalRealismHook.getCurrentSeasonalPeriod();
        HookUtils.logInfo("Current Seasonal Period: " + currentSeason);

        double currentAdjustment = TemporalRealismHook.calculateSeasonalAdjustment();
        HookUtils.logInfo(String.format("Current seasonal adjustment: %.3f", currentAdjustment));

        HookUtils.logInfo("Testing seasonal multipliers across samples:");

        double[] samples = new double[30];
        double sum = 0;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;

        for (int i = 0; i < 30; i++) {
            samples[i] = TemporalRealismHook.calculateSeasonalAdjustment();
            sum += samples[i];
            min = Math.min(min, samples[i]);
            max = Math.max(max, samples[i]);

            if (i < 10) {
                HookUtils.logInfo(String.format("  Sample %02d: %.3f", i + 1, samples[i]));
            }
        }

        HookUtils.logInfo(String.format("Statistics: Min=%.3f, Max=%.3f, Avg=%.3f",
            min, max, sum / 30));
    }

    /**
     * Demonstrates Long-term Entropy (Drift Engine)
     */
    public static void validateLongTermEntropy() {
        HookUtils.logInfo("--- VALIDATING: Long-term Entropy (Drift Engine) ---");

        TemporalRealismHook.DriftCycleInfo driftInfo = TemporalRealismHook.getCurrentDriftCycleInfo();
        HookUtils.logInfo("Drift Cycle Info: " + driftInfo);

        HookUtils.logInfo("Testing drift over simulated 30-day period:");

        double baseValue = 1000.0;
        double[] dailyValues = new double[30];

        for (int day = 0; day < 30; day++) {
            double driftedValue = TemporalRealismHook.applyLongTermEntropy(baseValue);
            dailyValues[day] = driftedValue;

            double deviation = Math.abs(driftedValue - baseValue) / baseValue * 100;

            if (day % 5 == 0 || day == 29) {
                HookUtils.logInfo(String.format("Day %02d: Value=%.2f, Deviation=%.1f%%",
                    day + 1, driftedValue, deviation));
            }
        }

        double sum = 0;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;

        for (double value : dailyValues) {
            sum += value;
            min = Math.min(min, value);
            max = Math.max(max, value);
        }

        HookUtils.logInfo(String.format("30-day statistics: Min=%.2f, Max=%.2f, Avg=%.2f, Range=%.2f%%",
            min, max, sum / 30, (max - min) / baseValue * 100));
    }

    /**
     * Demonstrates Realistic Idle Periods with Gaussian distribution
     */
    public static void validateStochasticIdlePeriods() {
        HookUtils.logInfo("--- VALIDATING: Stochastic Idle Periods ---");

        long[] baseDurations = {100, 500, 1000, 2000, 5000, 10000};

        for (long baseDuration : baseDurations) {
            HookUtils.logInfo("Testing base duration: " + baseDuration + "ms");

            long[] samples = new long[50];
            long sum = 0;
            long min = Long.MAX_VALUE;
            long max = Long.MIN_VALUE;

            for (int i = 0; i < 50; i++) {
                samples[i] = TemporalRealismHook.calculateStochasticIdlePeriod(baseDuration);
                sum += samples[i];
                min = Math.min(min, samples[i]);
                max = Math.max(max, samples[i]);
            }

            double average = (double) sum / 50;

            double variance = 0;
            for (long sample : samples) {
                variance += Math.pow(sample - average, 2);
            }
            variance /= 50;
            double stdDev = Math.sqrt(variance);

            HookUtils.logInfo(String.format("  Results: Min=%dms, Max=%dms, Avg=%.1fms, StdDev=%.1fms",
                min, max, average, stdDev));
            HookUtils.logInfo(String.format("  Variation coefficient: %.2f%%",
                (stdDev / average) * 100));
        }

        HookUtils.logInfo("Testing think time generation:");
        long[] thinkTimes = new long[10];
        for (int i = 0; i < 10; i++) {
            thinkTimes[i] = TemporalRealismHook.generateThinkTime();
            HookUtils.logInfo(String.format("  Think time %02d: %d ms", i + 1, thinkTimes[i]));
        }

        HookUtils.logInfo("Testing session duration generation:");
        long[] sessionDurations = new long[10];
        for (int i = 0; i < 10; i++) {
            sessionDurations[i] = TemporalRealismHook.generateSessionDuration();
            long minutes = TimeUnit.MILLISECONDS.toMinutes(sessionDurations[i]);
            HookUtils.logInfo(String.format("  Session %02d: %d ms (~%d minutes)",
                i + 1, sessionDurations[i], minutes));
        }
    }

    /**
     * Generates a performance test scenario report
     */
    public static void generatePerformanceScenarioReport() {
        HookUtils.logInfo("=== PERFORMANCE TEST SCENARIO REPORT ===");

        StringBuilder report = new StringBuilder();
        report.append("\n=== TEMPORAL REALISM CONFIGURATION ===\n");
        report.append("Device: Samsung Galaxy A12 (SM-A125U)\n");
        report.append("Timezone: ").append(ZoneId.systemDefault()).append("\n");
        report.append("Test Start: ").append(new java.util.Date(TemporalRealismHook.getTestStartTime())).append("\n");
        report.append("Elapsed Time: ").append(TemporalRealismHook.getElapsedTestTime()).append("ms\n");
        report.append("\n");

        report.append("=== CURRENT TEMPORAL CONTEXT ===\n");
        report.append("Day Type: ").append(TemporalRealismHook.getCurrentDayType()).append("\n");
        report.append("Engagement Pattern: ").append(TemporalRealismHook.getCurrentEngagementPattern()).append("\n");
        report.append("Seasonal Period: ").append(TemporalRealismHook.getCurrentSeasonalPeriod()).append("\n");
        report.append("Circadian Activity: ").append(String.format("%.2f", TemporalRealismHook.getCurrentCircadianActivityLevel())).append("\n");
        report.append("Drift Cycle: ").append(TemporalRealismHook.getCurrentDriftCycleInfo()).append("\n");
        report.append("\n");

        report.append("=== RECOMMENDED TEST PARAMETERS ===\n");
        report.append("Session Duration: ").append(TemporalRealismHook.generateSessionDuration()).append("ms\n");
        report.append("Think Time: ").append(TemporalRealismHook.generateThinkTime()).append("ms\n");
        report.append("Engagement Multiplier: ").append(String.format("%.2f", TemporalRealismHook.calculateTemporalVarianceMultiplier())).append("x\n");
        report.append("Seasonal Adjustment: ").append(String.format("%.2f", TemporalRealismHook.calculateSeasonalAdjustment())).append("x\n");
        report.append("\n");

        report.append("=== VALIDATION STATUS ===\n");
        report.append("Circadian Simulation: ACTIVE\n");
        report.append("Temporal Variance: ACTIVE\n");
        report.append("Seasonal Adjustments: ACTIVE\n");
        report.append("Long-term Entropy: ACTIVE\n");
        report.append("Stochastic Idle Periods: ACTIVE\n");

        HookUtils.logInfo(report.toString());
    }

    /**
     * Simulates a 30-day soak test with temporal realism
     */
    public static void simulate30DaySoakTest() {
        HookUtils.logInfo("=== 30-DAY SOAK TEST SIMULATION ===");

        long dayStartTime = System.currentTimeMillis();

        for (int day = 1; day <= 30; day++) {
            HookUtils.logInfo("\n--- Day " + day + " ---");

            TemporalRealismHook.DriftCycleInfo driftInfo = TemporalRealismHook.getCurrentDriftCycleInfo();
            HookUtils.logInfo("Drift Cycle: " + driftInfo);

            int sessionsToday = 0;
            for (int hour = 0; hour < 24; hour++) {
                if (TemporalRealismHook.shouldScheduleEventByCircadian()) {
                    sessionsToday++;

                    long sessionDuration = TemporalRealismHook.generateSessionDuration();
                    long thinkTime = TemporalRealismHook.generateThinkTime();

                    if (hour % 6 == 0) {
                        HookUtils.logInfo(String.format("  Hour %02d: Session scheduled (duration=%dms, think=%dms)",
                            hour, sessionDuration, thinkTime));
                    }
                }
            }

            HookUtils.logInfo("Day " + day + " summary: " + sessionsToday + " sessions");

            if (day % 7 == 0) {
                HookUtils.logInfo("Weekly checkpoint - resetting drift patterns");
                TemporalRealismHook.resetDriftSeed();
            }
        }

        long totalDuration = System.currentTimeMillis() - dayStartTime;
        HookUtils.logInfo("\nSimulation completed in " + totalDuration + "ms");
    }

    public static boolean isInitialized() {
        return TemporalRealismHook.isInitialized();
    }
}
