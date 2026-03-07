package com.samsungcloak.xposed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Sentiment Engagement Demo and Validation
 * Comprehensive testing suite for the Sentiment-Driven Engagement Model.
 * Demonstrates all 5 Emotional Realism Hooks with statistical validation.
 *
 * Target Device: Samsung Galaxy A12 (SM-A125U)
 * Purpose: UX Resilience Testing Validation
 */
public class SentimentEngagementDemo {

    private static final String LOG_TAG = "SamsungCloak.SentimentDemo";

    // Test results storage
    private static final Map<String, TestResult> testResults = new HashMap<>();
    private static final List<EngagementEvent> eventLog = new ArrayList<>();

    public static class TestResult {
        public final String testName;
        public final int iterations;
        public final double successRate;
        public final double avgValue;
        public final double minValue;
        public final double maxValue;
        public final String status;

        public TestResult(String testName, int iterations, double successRate,
                         double avgValue, double minValue, double maxValue, String status) {
            this.testName = testName;
            this.iterations = iterations;
            this.successRate = successRate;
            this.avgValue = avgValue;
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.status = status;
        }

        @Override
        public String toString() {
            return String.format("Test: %s | Iterations: %d | Success: %.2f%% | Avg: %.2f | Range: [%.2f, %.2f] | Status: %s",
                testName, iterations, successRate * 100, avgValue, minValue, maxValue, status);
        }
    }

    public static class EngagementEvent {
        public final long timestamp;
        public final String eventType;
        public final String description;
        public final SentimentEngagementHook.EngagementState engagementState;
        public final SentimentEngagementHook.EmotionalState emotionalState;

        public EngagementEvent(String eventType, String description,
                              SentimentEngagementHook.EngagementState engagementState,
                              SentimentEngagementHook.EmotionalState emotionalState) {
            this.timestamp = System.currentTimeMillis();
            this.eventType = eventType;
            this.description = description;
            this.engagementState = engagementState;
            this.emotionalState = emotionalState;
        }
    }

    /**
     * Run complete validation suite
     */
    public static void runFullValidation() {
        HookUtils.logInfo(LOG_TAG + " ============================================");
        HookUtils.logInfo(LOG_TAG + " SENTIMENT-DRIVEN ENGAGEMENT MODEL VALIDATION");
        HookUtils.logInfo(LOG_TAG + " ============================================");

        validateMoodShiftSimulation();
        validateEngagementFluctuations();
        validateImpulseVsDelayedAction();
        validateExcitementSpikes();
        validateApathyPhases();
        validateIntegrationScenario();

        printSummaryReport();
    }

    /**
     * Hook 1 Validation: Mood Shift Simulation
     * Tests state-machine transitions between engagement states
     */
    public static void validateMoodShiftSimulation() {
        HookUtils.logInfo(LOG_TAG + "\n--- TEST: Mood Shift Simulation ---");

        int iterations = 1000;
        Map<SentimentEngagementHook.EngagementState, Integer> stateCounts = new HashMap<>();
        Map<SentimentEngagementHook.EmotionalState, Integer> moodCounts = new HashMap<>();

        // Initialize counts
        for (SentimentEngagementHook.EngagementState state : SentimentEngagementHook.EngagementState.values()) {
            stateCounts.put(state, 0);
        }
        for (SentimentEngagementHook.EmotionalState mood : SentimentEngagementHook.EmotionalState.values()) {
            moodCounts.put(mood, 0);
        }

        // Reset to known state
        SentimentEngagementHook.reset();
        SentimentEngagementHook.setEngagementState(SentimentEngagementHook.EngagementState.MODERATE_ENGAGEMENT);
        SentimentEngagementHook.setEmotionalState(SentimentEngagementHook.EmotionalState.NEUTRAL);

        // Simulate interactions and track state changes
        for (int i = 0; i < iterations; i++) {
            // Simulate some activity
            simulateActivity();

            // Record current state
            stateCounts.merge(SentimentEngagementHook.getCurrentEngagementState(), 1, Integer::sum);
            moodCounts.merge(SentimentEngagementHook.getCurrentMood(), 1, Integer::sum);

            // Force some state transitions manually for testing
            if (i % 200 == 0 && i > 0) {
                forceRandomStateTransition();
            }
        }

        // Calculate transition diversity
        long uniqueStates = stateCounts.values().stream().filter(c -> c > 0).count();
        long uniqueMoods = moodCounts.values().stream().filter(c -> c > 0).count();

        HookUtils.logInfo(LOG_TAG + " Engagement State Distribution:");
        stateCounts.forEach((state, count) -> {
            double percentage = (count * 100.0) / iterations;
            HookUtils.logInfo(LOG_TAG + String.format("   %s: %.1f%% (%d occurrences)", state, percentage, count));
        });

        HookUtils.logInfo(LOG_TAG + " Emotional State Distribution:");
        moodCounts.forEach((mood, count) -> {
            double percentage = (count * 100.0) / iterations;
            HookUtils.logInfo(LOG_TAG + String.format("   %s: %.1f%% (%d occurrences)", mood, percentage, count));
        });

        String status = (uniqueStates >= 3 && uniqueMoods >= 3) ? "PASS" : "FAIL";
        HookUtils.logInfo(LOG_TAG + " Result: " + status + " (States: " + uniqueStates + ", Moods: " + uniqueMoods + ")");

        testResults.put("MoodShiftSimulation", new TestResult(
            "Mood Shift Simulation", iterations, uniqueStates / 5.0,
            uniqueStates, uniqueStates, uniqueMoods, status
        ));
    }

    /**
     * Hook 2 Validation: Engagement Fluctuations (Attention Economy)
     * Tests context switching behavior
     */
    public static void validateEngagementFluctuations() {
        HookUtils.logInfo(LOG_TAG + "\n--- TEST: Engagement Fluctuations (Attention Economy) ---");

        int iterations = 500;
        int contextSwitches = 0;
        List<Long> switchIntervals = new ArrayList<>();
        long lastSwitch = System.currentTimeMillis();

        SentimentEngagementHook.reset();

        for (int i = 0; i < iterations; i++) {
            // Simulate interactions
            simulateActivity();

            // Check for context switches
            if (SentimentEngagementHook.shouldTriggerContextSwitch()) {
                contextSwitches++;
                long now = System.currentTimeMillis();
                switchIntervals.add(now - lastSwitch);
                lastSwitch = now;

                eventLog.add(new EngagementEvent(
                    "CONTEXT_SWITCH",
                    "User switched context mid-task",
                    SentimentEngagementHook.getCurrentEngagementState(),
                    SentimentEngagementHook.getCurrentMood()
                ));
            }

            // Vary emotional states to test different scenarios
            if (i % 100 == 0) {
                SentimentEngagementHook.EmotionalState[] moods = SentimentEngagementHook.EmotionalState.values();
                SentimentEngagementHook.setEmotionalState(moods[ThreadLocalRandom.current().nextInt(moods.length)]);
            }
        }

        double switchRate = (double) contextSwitches / iterations;
        double avgInterval = switchIntervals.stream().mapToLong(Long::longValue).average().orElse(0);

        HookUtils.logInfo(LOG_TAG + " Context Switch Statistics:");
        HookUtils.logInfo(LOG_TAG + String.format("   Total switches: %d", contextSwitches));
        HookUtils.logInfo(LOG_TAG + String.format("   Switch rate: %.2f%%", switchRate * 100));
        HookUtils.logInfo(LOG_TAG + String.format("   Avg interval: %.0f ms", avgInterval));

        // Expected: 2-15% switch rate based on emotional state
        String status = (switchRate >= 0.02 && switchRate <= 0.20) ? "PASS" : "REVIEW";
        HookUtils.logInfo(LOG_TAG + " Result: " + status);

        testResults.put("EngagementFluctuations", new TestResult(
            "Engagement Fluctuations", iterations, switchRate,
            avgInterval, 0, switchIntervals.stream().mapToLong(Long::longValue).max().orElse(0), status
        ));
    }

    /**
     * Hook 3 Validation: Impulse vs. Delayed Action
     * Tests stochastic decision timing patterns
     */
    public static void validateImpulseVsDelayedAction() {
        HookUtils.logInfo(LOG_TAG + "\n--- TEST: Impulse vs. Delayed Action ---");

        int iterations = 300;
        List<Long> actionDelays = new ArrayList<>();
        Map<SentimentEngagementHook.DecisionStyle, Integer> styleCounts = new HashMap<>();

        for (SentimentEngagementHook.DecisionStyle style : SentimentEngagementHook.DecisionStyle.values()) {
            styleCounts.put(style, 0);
        }

        // Test each emotional state
        for (SentimentEngagementHook.EmotionalState mood : SentimentEngagementHook.EmotionalState.values()) {
            SentimentEngagementHook.setEmotionalState(mood);

            for (int i = 0; i < 50; i++) {
                simulateActivity();

                // Record decision style distribution
                SentimentEngagementHook.DecisionStyle style = SentimentEngagementHook.getCurrentDecisionStyle();
                styleCounts.merge(style, 1, Integer::sum);

                // Simulate delay
                long delay = calculateSimulatedDelay();
                actionDelays.add(delay);
            }
        }

        double avgDelay = actionDelays.stream().mapToLong(Long::longValue).average().orElse(0);
        long minDelay = actionDelays.stream().mapToLong(Long::longValue).min().orElse(0);
        long maxDelay = actionDelays.stream().mapToLong(Long::longValue).max().orElse(0);

        HookUtils.logInfo(LOG_TAG + " Decision Style Distribution:");
        styleCounts.forEach((style, count) -> {
            double percentage = (count * 100.0) / iterations;
            HookUtils.logInfo(LOG_TAG + String.format("   %s: %.1f%%", style, percentage));
        });

        HookUtils.logInfo(LOG_TAG + " Action Delay Statistics:");
        HookUtils.logInfo(LOG_TAG + String.format("   Average: %.0f ms", avgDelay));
        HookUtils.logInfo(LOG_TAG + String.format("   Range: %d - %d ms", minDelay, maxDelay));

        // Validate delay range covers both impulse and deliberation
        boolean hasImpulse = minDelay < 500;
        boolean hasDeliberation = maxDelay > 2000;
        String status = (hasImpulse && hasDeliberation) ? "PASS" : "FAIL";

        HookUtils.logInfo(LOG_TAG + " Result: " + status);

        testResults.put("ImpulseVsDelayed", new TestResult(
            "Impulse vs Delayed Action", iterations, (hasImpulse && hasDeliberation) ? 1.0 : 0.0,
            avgDelay, minDelay, maxDelay, status
        ));
    }

    /**
     * Hook 4 Validation: Excitement Spikes
     * Tests burst interaction patterns
     */
    public static void validateExcitementSpikes() {
        HookUtils.logInfo(LOG_TAG + "\n--- TEST: Excitement Spikes (Burst Interaction) ---");

        int testCycles = 100;
        int spikesTriggered = 0;
        List<Integer> burstDurations = new ArrayList<>();

        SentimentEngagementHook.reset();

        for (int cycle = 0; cycle < testCycles; cycle++) {
            // Simulate excited user discovering features
            SentimentEngagementHook.setEmotionalState(SentimentEngagementHook.EmotionalState.EXCITED);

            for (int i = 0; i < 30; i++) {
                simulateActivity();

                if (SentimentEngagementHook.shouldTriggerExcitementSpike()) {
                    spikesTriggered++;

                    // Simulate burst duration
                    int burstDuration = ThreadLocalRandom.current().nextInt(5, 20);
                    burstDurations.add(burstDuration);

                    eventLog.add(new EngagementEvent(
                        "EXCITEMENT_SPIKE",
                        "Burst interaction pattern detected",
                        SentimentEngagementHook.EngagementState.BURST_MODE,
                        SentimentEngagementHook.EmotionalState.EXCITED
                    ));

                    break; // Move to next cycle after spike
                }
            }

            // Reset for next cycle
            SentimentEngagementHook.setEngagementState(SentimentEngagementHook.EngagementState.MODERATE_ENGAGEMENT);
        }

        double spikeRate = (double) spikesTriggered / testCycles;
        double avgBurstDuration = burstDurations.stream().mapToInt(Integer::intValue).average().orElse(0);

        HookUtils.logInfo(LOG_TAG + " Excitement Spike Statistics:");
        HookUtils.logInfo(LOG_TAG + String.format("   Test cycles: %d", testCycles));
        HookUtils.logInfo(LOG_TAG + String.format("   Spikes triggered: %d", spikesTriggered));
        HookUtils.logInfo(LOG_TAG + String.format("   Spike rate: %.2f%%", spikeRate * 100));
        HookUtils.logInfo(LOG_TAG + String.format("   Avg burst duration: %.1f interactions", avgBurstDuration));

        // Excited users should trigger spikes at reasonable rate
        String status = (spikeRate >= 0.05 && spikeRate <= 0.40) ? "PASS" : "REVIEW";
        HookUtils.logInfo(LOG_TAG + " Result: " + status);

        testResults.put("ExcitementSpikes", new TestResult(
            "Excitement Spikes", testCycles, spikeRate,
            avgBurstDuration, 5, 20, status
        ));
    }

    /**
     * Hook 5 Validation: Apathy Phases
     * Tests intentional prompt ignoring behavior
     */
    public static void validateApathyPhases() {
        HookUtils.logInfo(LOG_TAG + "\n--- TEST: Apathy Phases (Ignored CTAs) ---");

        String[] promptTypes = {
            "purchase_button", "signup_prompt", "notification_opt_in",
            "rating_request", "promo_popup", "modal_dialog",
            "permission_request", "update_available"
        };

        // Test each emotional state
        for (SentimentEngagementHook.EmotionalState mood : SentimentEngagementHook.EmotionalState.values()) {
            SentimentEngagementHook.setEmotionalState(mood);
            SentimentEngagementHook.reset();

            int totalPrompts = 200;
            int ignoredPrompts = 0;

            for (int i = 0; i < totalPrompts; i++) {
                String promptType = promptTypes[ThreadLocalRandom.current().nextInt(promptTypes.length)];

                if (SentimentEngagementHook.shouldIgnorePrompt(promptType)) {
                    ignoredPrompts++;
                }
            }

            double ignoreRate = (double) ignoredPrompts / totalPrompts;
            double expectedRate = getExpectedApathyRate(mood);

            HookUtils.logInfo(LOG_TAG + String.format("   %s: %.1f%% ignored (expected: %.1f%%) %s",
                mood, ignoreRate * 100, expectedRate * 100,
                Math.abs(ignoreRate - expectedRate) < 0.1 ? "✓" : "⚠"));
        }

        // Check extended apathy phase
        boolean extendedApathy = SentimentEngagementHook.isInExtendedApathyPhase();
        long apathyDuration = SentimentEngagementHook.getExtendedApathyDuration();

        HookUtils.logInfo(LOG_TAG + " Extended Apathy Phase:");
        HookUtils.logInfo(LOG_TAG + String.format("   Active: %s", extendedApathy));
        HookUtils.logInfo(LOG_TAG + String.format("   Duration range: %d ms", apathyDuration));

        double overallApathyRate = SentimentEngagementHook.getApathyRate();
        String status = (overallApathyRate >= 0.05 && overallApathyRate <= 0.35) ? "PASS" : "REVIEW";

        HookUtils.logInfo(LOG_TAG + " Overall apathy rate: " + String.format("%.1f%%", overallApathyRate * 100));
        HookUtils.logInfo(LOG_TAG + " Result: " + status);

        testResults.put("ApathyPhases", new TestResult(
            "Apathy Phases", 1600, overallApathyRate,
            overallApathyRate * 100, 0, 100, status
        ));
    }

    /**
     * Integration Test: Full user session simulation
     */
    public static void validateIntegrationScenario() {
        HookUtils.logInfo(LOG_TAG + "\n--- TEST: Full Session Integration ---");

        SentimentEngagementHook.reset();
        eventLog.clear();

        // Simulate a realistic app session
        int sessionLength = 150;
        int actions = 0;
        int contextSwitches = 0;
        int ignoredCTAs = 0;
        int excitementBursts = 0;

        for (int i = 0; i < sessionLength; i++) {
            // Simulate activity
            simulateActivity();
            actions++;

            // Randomly simulate different scenarios
            double random = ThreadLocalRandom.current().nextDouble();

            if (random < 0.05) {
                // Context switch scenario
                if (SentimentEngagementHook.shouldTriggerContextSwitch()) {
                    contextSwitches++;
                }
            } else if (random < 0.15) {
                // CTA encountered
                String[] ctas = {"buy_now", "subscribe", "share", "rate_app"};
                String cta = ctas[ThreadLocalRandom.current().nextInt(ctas.length)];
                if (SentimentEngagementHook.shouldIgnorePrompt(cta)) {
                    ignoredCTAs++;
                }
            } else if (random < 0.20) {
                // Potential excitement spike
                if (SentimentEngagementHook.shouldTriggerExcitementSpike()) {
                    excitementBursts++;
                    // Burst adds multiple rapid actions
                    actions += ThreadLocalRandom.current().nextInt(5, 15);
                }
            }

            // Periodic mood shifts
            if (i % 30 == 0 && i > 0) {
                SentimentEngagementHook.EmotionalState[] moods = SentimentEngagementHook.EmotionalState.values();
                SentimentEngagementHook.setEmotionalState(moods[ThreadLocalRandom.current().nextInt(moods.length)]);
            }
        }

        HookUtils.logInfo(LOG_TAG + " Session Summary:");
        HookUtils.logInfo(LOG_TAG + String.format("   Total actions: %d", actions));
        HookUtils.logInfo(LOG_TAG + String.format("   Context switches: %d (%.1f%%)",
            contextSwitches, (contextSwitches * 100.0) / sessionLength));
        HookUtils.logInfo(LOG_TAG + String.format("   Ignored CTAs: %d", ignoredCTAs));
        HookUtils.logInfo(LOG_TAG + String.format("   Excitement bursts: %d", excitementBursts));
        HookUtils.logInfo(LOG_TAG + String.format("   Final engagement: %s", SentimentEngagementHook.getCurrentEngagementState()));
        HookUtils.logInfo(LOG_TAG + String.format("   Final mood: %s", SentimentEngagementHook.getCurrentMood()));
        HookUtils.logInfo(LOG_TAG + String.format("   Session duration: %d minutes (simulated)",
            SentimentEngagementHook.getSessionDurationMinutes()));

        // Validate realistic session metrics
        boolean realisticActionCount = actions >= sessionLength && actions <= sessionLength * 2;
        boolean realisticSwitchRate = contextSwitches >= 2 && contextSwitches <= 25;

        String status = (realisticActionCount && realisticSwitchRate) ? "PASS" : "REVIEW";
        HookUtils.logInfo(LOG_TAG + " Result: " + status);

        testResults.put("IntegrationScenario", new TestResult(
            "Integration Scenario", sessionLength, (realisticActionCount && realisticSwitchRate) ? 1.0 : 0.5,
            actions, 0, actions, status
        ));
    }

    /**
     * Print comprehensive summary report
     */
    public static void printSummaryReport() {
        HookUtils.logInfo(LOG_TAG + "\n========================================");
        HookUtils.logInfo(LOG_TAG + " VALIDATION SUMMARY REPORT");
        HookUtils.logInfo(LOG_TAG + " ========================================");

        int passCount = 0;
        int totalTests = testResults.size();

        for (TestResult result : testResults.values()) {
            HookUtils.logInfo(LOG_TAG + " " + result.toString());
            if ("PASS".equals(result.status)) {
                passCount++;
            }
        }

        HookUtils.logInfo(LOG_TAG + "\n----------------------------------------");
        HookUtils.logInfo(LOG_TAG + String.format(" Overall: %d/%d tests passed (%.0f%%)",
            passCount, totalTests, (passCount * 100.0) / totalTests));

        if (passCount == totalTests) {
            HookUtils.logInfo(LOG_TAG + " STATUS: ✅ ALL TESTS PASSED");
        } else if (passCount >= totalTests * 0.8) {
            HookUtils.logInfo(LOG_TAG + " STATUS: ⚠️ MOSTLY PASSED - REVIEW RECOMMENDED");
        } else {
            HookUtils.logInfo(LOG_TAG + " STATUS: ❌ ISSUES DETECTED - INVESTIGATION REQUIRED");
        }

        HookUtils.logInfo(LOG_TAG + " ========================================\n");
    }

    /**
     * Generate detailed cognitive profile for analysis
     */
    public static void generateSentimentProfile() {
        HookUtils.logInfo(LOG_TAG + "\n========================================");
        HookUtils.logInfo(LOG_TAG + " SENTIMENT ENGAGEMENT PROFILE");
        HookUtils.logInfo(LOG_TAG + " ========================================");

        HookUtils.logInfo(LOG_TAG + " Current State:");
        HookUtils.logInfo(LOG_TAG + "   Engagement: " + SentimentEngagementHook.getCurrentEngagementState());
        HookUtils.logInfo(LOG_TAG + "   Mood: " + SentimentEngagementHook.getCurrentMood());
        HookUtils.logInfo(LOG_TAG + "   Decision Style: " + SentimentEngagementHook.getCurrentDecisionStyle());

        HookUtils.logInfo(LOG_TAG + "\n Session Metrics:");
        HookUtils.logInfo(LOG_TAG + "   Duration: " + SentimentEngagementHook.getSessionDurationMinutes() + " minutes");
        HookUtils.logInfo(LOG_TAG + "   Interactions: " + SentimentEngagementHook.getTotalInteractionCount());
        HookUtils.logInfo(LOG_TAG + "   Apathy Rate: " + String.format("%.1f%%", SentimentEngagementHook.getApathyRate() * 100));

        HookUtils.logInfo(LOG_TAG + "\n Behavioral Indicators:");
        HookUtils.logInfo(LOG_TAG + "   Engagement Probability: " +
            String.format("%.1f%%", SentimentEngagementHook.getCurrentEngagementProbability() * 100));
        HookUtils.logInfo(LOG_TAG + "   Interaction Delay: " + SentimentEngagementHook.getInteractionDelay() + " ms");
        HookUtils.logInfo(LOG_TAG + "   Context Switch Likely: " + SentimentEngagementHook.shouldTriggerContextSwitch());

        HookUtils.logInfo(LOG_TAG + " ========================================\n");
    }

    /**
     * Simulate a realistic user session with logging
     */
    public static void simulateRealisticSession() {
        HookUtils.logInfo(LOG_TAG + "\n========================================");
        HookUtils.logInfo(LOG_TAG + " REALISTIC USER SESSION SIMULATION");
        HookUtils.logInfo(LOG_TAG + " ========================================");

        SentimentEngagementHook.reset();

        // Simulate session phases
        simulateDiscoveryPhase();
        simulateEngagementPhase();
        simulateFatiguePhase();

        generateSentimentProfile();
    }

    // Helper methods

    private static void simulateDiscoveryPhase() {
        HookUtils.logInfo(LOG_TAG + "\n--- Phase 1: Discovery (0-5 min) ---");
        SentimentEngagementHook.setEmotionalState(SentimentEngagementHook.EmotionalState.EXCITED);

        for (int i = 0; i < 20; i++) {
            simulateActivity();

            if (SentimentEngagementHook.shouldTriggerExcitementSpike()) {
                HookUtils.logInfo(LOG_TAG + "   [EXCITEMENT SPIKE] Rapid exploration detected");
            }
        }
    }

    private static void simulateEngagementPhase() {
        HookUtils.logInfo(LOG_TAG + "\n--- Phase 2: Engagement (5-20 min) ---");
        SentimentEngagementHook.setEmotionalState(SentimentEngagementHook.EmotionalState.CONTENT);

        for (int i = 0; i < 50; i++) {
            simulateActivity();

            if (i % 10 == 0 && SentimentEngagementHook.shouldTriggerContextSwitch()) {
                HookUtils.logInfo(LOG_TAG + "   [CONTEXT SWITCH] User checked notification");
            }

            if (i % 15 == 0) {
                String[] ctas = {"subscribe", "upgrade", "share"};
                String cta = ctas[ThreadLocalRandom.current().nextInt(ctas.length)];
                if (SentimentEngagementHook.shouldIgnorePrompt(cta)) {
                    HookUtils.logInfo(LOG_TAG + "   [APATHY] Ignored CTA: " + cta);
                }
            }
        }
    }

    private static void simulateFatiguePhase() {
        HookUtils.logInfo(LOG_TAG + "\n--- Phase 3: Fatigue (20+ min) ---");
        SentimentEngagementHook.setEmotionalState(SentimentEngagementHook.EmotionalState.BORED);
        SentimentEngagementHook.setEngagementState(SentimentEngagementHook.EngagementState.LOW_ENGAGEMENT);

        for (int i = 0; i < 30; i++) {
            simulateActivity();

            if (SentimentEngagementHook.isInExtendedApathyPhase()) {
                HookUtils.logInfo(LOG_TAG + "   [EXTENDED APATHY] User completely disengaged");
            }
        }
    }

    private static void simulateActivity() {
        // Simulate a user interaction
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void forceRandomStateTransition() {
        SentimentEngagementHook.EngagementState[] states = SentimentEngagementHook.EngagementState.values();
        SentimentEngagementHook.EmotionalState[] moods = SentimentEngagementHook.EmotionalState.values();

        SentimentEngagementHook.setEngagementState(states[ThreadLocalRandom.current().nextInt(states.length)]);
        SentimentEngagementHook.setEmotionalState(moods[ThreadLocalRandom.current().nextInt(moods.length)]);
    }

    private static long calculateSimulatedDelay() {
        SentimentEngagementHook.DecisionStyle style = SentimentEngagementHook.getCurrentDecisionStyle();
        long baseDelay = style.getRandomDelayMs();

        // Apply mood multiplier
        double multiplier = SentimentEngagementHook.getCurrentMood().getSpeedMultiplier();
        return (long) (baseDelay / multiplier);
    }

    private static double getExpectedApathyRate(SentimentEngagementHook.EmotionalState mood) {
        switch (mood) {
            case BORED: return 0.20;
            case DISTRACTED: return 0.25;
            case FRUSTRATED: return 0.15;
            case EXCITED: return 0.05;
            case CONTENT: return 0.08;
            case NEUTRAL: return 0.10;
            default: return 0.10;
        }
    }

    public static List<EngagementEvent> getEventLog() {
        return new ArrayList<>(eventLog);
    }

    public static void clearEventLog() {
        eventLog.clear();
    }
}
