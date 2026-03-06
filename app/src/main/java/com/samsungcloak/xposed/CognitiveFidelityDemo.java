package com.samsungcloak.xposed;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * CognitiveFidelityDemo - Demonstration and Validation Class
 *
 * This class provides demonstration and validation methods for the
 * CognitiveFidelityHook system. It can be used to verify the correctness
 * of cognitive simulations and demonstrate the behavior of each hook.
 *
 * Usage: Call methods from test harness or automated validation suite
 */
public class CognitiveFidelityDemo {
    private static final String LOG_TAG = "SamsungCloak.CognitiveDemo";

    /**
     * Runs comprehensive validation of all cognitive fidelity features
     */
    public static void runFullValidation() {
        HookUtils.logInfo("=== COGNITIVE FIDELITY VALIDATION SUITE ===");

        validateLimitedAttentionSpan();
        validateBoundedRationality();
        validateDecisionFatigue();
        validateImperfectMemory();
        validateChangingPreferences();

        HookUtils.logInfo("=== VALIDATION COMPLETE ===");
    }

    /**
     * Demonstrates Limited Attention Span simulation
     */
    public static void validateLimitedAttentionSpan() {
        HookUtils.logInfo("--- VALIDATING: Limited Attention Span ---");

        int abandonmentCount = 0;
        int contextSwitchCount = 0;
        int totalTrials = 100;

        HookUtils.logInfo("Testing task abandonment over " + totalTrials + " simulated interactions:");

        for (int i = 0; i < totalTrials; i++) {
            if (CognitiveFidelityHook.shouldAbandonTask()) {
                abandonmentCount++;
            }

            if (i % 20 == 0 && i > 0) {
                CognitiveFidelityHook.simulateContextSwitch();
                contextSwitchCount++;
            }
        }

        double abandonmentRate = (double) abandonmentCount / totalTrials;
        HookUtils.logInfo("Results: " + abandonmentCount + "/" + totalTrials +
                         " abandonments (" + String.format("%.1f", abandonmentRate * 100) + "%)");
        HookUtils.logInfo("Context switches simulated: " + contextSwitchCount);

        double attentionLevel = CognitiveFidelityHook.getAttentionLevel();
        HookUtils.logInfo("Final attention level: " + String.format("%.2f", attentionLevel));

        if (abandonmentRate > 0.02 && abandonmentRate < 0.25) {
            HookUtils.logInfo("✓ Abandonment rate within realistic range (2-25%)");
        } else {
            HookUtils.logWarn("⚠ Abandonment rate outside expected range");
        }
    }

    /**
     * Demonstrates Bounded Rationality (Satisficing) behavior
     */
    public static void validateBoundedRationality() {
        HookUtils.logInfo("--- VALIDATING: Bounded Rationality (Satisficing) ---");

        int satisficeCount = 0;
        int totalDecisions = 50;

        HookUtils.logInfo("Testing satisficing behavior over " + totalDecisions + " decisions:");

        int[] selectionDistribution = new int[5];

        for (int i = 0; i < totalDecisions; i++) {
            boolean satisficed = CognitiveFidelityHook.shouldSatisfice();
            if (satisficed) {
                satisficeCount++;
            }

            int selectedOption = CognitiveFidelityHook.selectOptionWithSatisficing(5);
            selectionDistribution[selectedOption]++;
        }

        double satisficeRate = (double) satisficeCount / totalDecisions;
        HookUtils.logInfo("Satisficing rate: " + String.format("%.1f", satisficeRate * 100) + "%");

        HookUtils.logInfo("Option selection distribution:");
        for (int i = 0; i < selectionDistribution.length; i++) {
            double percentage = (double) selectionDistribution[i] / totalDecisions * 100;
            HookUtils.logInfo("  Option " + i + ": " + selectionDistribution[i] +
                             " (" + String.format("%.1f", percentage) + "%)");
        }

        double firstOptionBias = (double) selectionDistribution[0] / totalDecisions;
        HookUtils.logInfo("First-option bias: " + String.format("%.1f", firstOptionBias * 100) + "%");

        if (firstOptionBias > 0.3) {
            HookUtils.logInfo("✓ Satisficing bias detected (first option preferred)");
        }

        double decisionQuality = CognitiveFidelityHook.getDecisionQuality();
        HookUtils.logInfo("Current decision quality: " + String.format("%.2f", decisionQuality));
    }

    /**
     * Demonstrates Decision Fatigue effects
     */
    public static void validateDecisionFatigue() {
        HookUtils.logInfo("--- VALIDATING: Decision Fatigue ---");

        HookUtils.logInfo("Simulating 60-minute session with decision fatigue:");

        long startTime = System.currentTimeMillis();
        int[] errorDistribution = new int[6];
        double[] delaySamples = new double[6];
        int[] sampleCounts = new int[6];

        for (int minute = 0; minute < 60; minute++) {
            boolean madeError = CognitiveFidelityHook.shouldMakeError();
            int timeSlot = Math.min(minute / 10, 5);
            if (madeError) {
                errorDistribution[timeSlot]++;
            }

            long delayStart = System.currentTimeMillis();
            CognitiveFidelityHook.applyDecisionFatigueDelay();
            long actualDelay = System.currentTimeMillis() - delayStart;

            delaySamples[timeSlot] += actualDelay;
            sampleCounts[timeSlot]++;

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        HookUtils.logInfo("Error distribution by 10-minute intervals:");
        for (int i = 0; i < 6; i++) {
            HookUtils.logInfo("  Minutes " + (i * 10) + "-" + ((i + 1) * 10) + ": " +
                             errorDistribution[i] + " errors");
        }

        HookUtils.logInfo("Average delay by session phase:");
        for (int i = 0; i < 6; i++) {
            if (sampleCounts[i] > 0) {
                double avgDelay = delaySamples[i] / sampleCounts[i];
                HookUtils.logInfo("  Phase " + (i + 1) + ": " + String.format("%.1f", avgDelay) + "ms");
            }
        }

        CognitiveFidelityHook.CognitiveState finalState = CognitiveFidelityHook.getCurrentCognitiveState();
        HookUtils.logInfo("Final cognitive state: " + finalState);

        double errorProbability = CognitiveFidelityHook.calculateErrorProbability();
        HookUtils.logInfo("Current error probability: " + String.format("%.2f", errorProbability));

        if (finalState == CognitiveFidelityHook.CognitiveState.FATIGUED ||
            finalState == CognitiveFidelityHook.CognitiveState.OVERWHELMED) {
            HookUtils.logInfo("✓ Cognitive state correctly reflects session fatigue");
        }
    }

    /**
     * Demonstrates Imperfect Memory behaviors
     */
    public static void validateImperfectMemory() {
        HookUtils.logInfo("--- VALIDATING: Imperfect Memory ---");

        String[] screens = {
            "HomeScreen",
            "ProductList",
            "ProductDetail",
            "ShoppingCart",
            "CheckoutFlow",
            "PaymentScreen"
        };

        HookUtils.logInfo("Simulating navigation through " + screens.length + " screens:");

        int revalidationCount = 0;
        int totalVisits = 0;

        for (int cycle = 0; cycle < 3; cycle++) {
            HookUtils.logInfo("  Navigation cycle " + (cycle + 1) + ":");

            for (String screen : screens) {
                CognitiveFidelityHook.recordScreenVisit(screen);
                totalVisits++;

                if (CognitiveFidelityHook.shouldRevalidate(screen)) {
                    revalidationCount++;
                    HookUtils.logInfo("    Re-validation on: " + screen);
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        double revalidationRate = (double) revalidationCount / totalVisits;
        HookUtils.logInfo("Total visits: " + totalVisits);
        HookUtils.logInfo("Re-validations: " + revalidationCount);
        HookUtils.logInfo("Re-validation rate: " + String.format("%.1f", revalidationRate * 100) + "%");

        double memoryConfidence = CognitiveFidelityHook.getMemoryConfidence();
        HookUtils.logInfo("Final memory confidence: " + String.format("%.2f", memoryConfidence));

        List<String> history = CognitiveFidelityHook.getNavigationHistory();
        HookUtils.logInfo("Navigation history size: " + history.size() + " entries");

        if (revalidationRate > 0.05 && revalidationRate < 0.30) {
            HookUtils.logInfo("✓ Re-validation rate within realistic range (5-30%)");
        }
    }

    /**
     * Demonstrates Changing Preferences evolution
     */
    public static void validateChangingPreferences() {
        HookUtils.logInfo("--- VALIDATING: Changing Preferences ---");

        HookUtils.logInfo("Tracking preference evolution over 10 evolution cycles:");

        CognitiveFidelityHook.NavigationStyle[] styleHistory = new CognitiveFidelityHook.NavigationStyle[10];
        CognitiveFidelityHook.InteractionSpeed[] speedHistory = new CognitiveFidelityHook.InteractionSpeed[10];

        styleHistory[0] = CognitiveFidelityHook.getCurrentNavigationStyle();
        speedHistory[0] = getCurrentSpeedFromMultiplier();

        HookUtils.logInfo("  Initial - Style: " + styleHistory[0] +
                         ", Speed: " + speedHistory[0]);

        for (int i = 1; i < 10; i++) {
            CognitiveFidelityHook.evolvePreferences();

            styleHistory[i] = CognitiveFidelityHook.getCurrentNavigationStyle();
            speedHistory[i] = getCurrentSpeedFromMultiplier();

            if (styleHistory[i] != styleHistory[i - 1] || speedHistory[i] != speedHistory[i - 1]) {
                HookUtils.logInfo("  Evolution " + i + " - Style: " + styleHistory[i] +
                                 ", Speed: " + speedHistory[i]);
            }
        }

        int styleChanges = 0;
        int speedChanges = 0;

        for (int i = 1; i < 10; i++) {
            if (styleHistory[i] != styleHistory[i - 1]) styleChanges++;
            if (speedHistory[i] != speedHistory[i - 1]) speedChanges++;
        }

        HookUtils.logInfo("Total style changes: " + styleChanges + "/9");
        HookUtils.logInfo("Total speed changes: " + speedChanges + "/9");

        HookUtils.logInfo("Testing navigation path generation:");
        List<String> availablePaths = Arrays.asList(
            "DirectCheckout",
            "BrowseMore",
            "ApplyCoupon",
            "SaveForLater",
            "ShareProduct"
        );

        for (int i = 0; i < 5; i++) {
            List<String> path = CognitiveFidelityHook.generateNavigationPath(availablePaths);
            HookUtils.logInfo("  Generated path " + (i + 1) + ": " + path);
        }

        double speedMultiplier = CognitiveFidelityHook.getInteractionSpeedMultiplier();
        HookUtils.logInfo("Current speed multiplier: " + String.format("%.2f", speedMultiplier));

        Map<String, Object> preferences = CognitiveFidelityHook.getUserPreferences();
        HookUtils.logInfo("Current user preferences:");
        for (Map.Entry<String, Object> entry : preferences.entrySet()) {
            HookUtils.logInfo("  " + entry.getKey() + ": " + entry.getValue());
        }

        if (styleChanges > 0 || speedChanges > 0) {
            HookUtils.logInfo("✓ Preferences evolved over time as expected");
        }
    }

    /**
     * Helper to determine speed enum from multiplier
     */
    private static CognitiveFidelityHook.InteractionSpeed getCurrentSpeedFromMultiplier() {
        double multiplier = CognitiveFidelityHook.getInteractionSpeedMultiplier();

        for (CognitiveFidelityHook.InteractionSpeed speed : CognitiveFidelityHook.InteractionSpeed.values()) {
            if (Math.abs(speed.multiplier - multiplier) < 0.1) {
                return speed;
            }
        }

        return CognitiveFidelityHook.InteractionSpeed.NORMAL;
    }

    /**
     * Generates a cognitive profile report
     */
    public static void generateCognitiveProfileReport() {
        HookUtils.logInfo("=== COGNITIVE FIDELITY PROFILE REPORT ===");

        StringBuilder report = new StringBuilder();
        report.append("\n=== COGNITIVE STATE SNAPSHOT ===\n");
        report.append("Device: Samsung Galaxy A12 (SM-A125U)\n");
        report.append("Session Duration: ").append(CognitiveFidelityHook.getSessionDuration()).append("ms\n");
        report.append("Interaction Count: ").append(CognitiveFidelityHook.getInteractionCount()).append("\n");
        report.append("\n");

        report.append("=== CURRENT COGNITIVE METRICS ===\n");
        report.append("Cognitive State: ").append(CognitiveFidelityHook.getCurrentCognitiveState()).append("\n");
        report.append("Attention Level: ").append(String.format("%.2f", CognitiveFidelityHook.getAttentionLevel())).append("\n");
        report.append("Decision Quality: ").append(String.format("%.2f", CognitiveFidelityHook.getDecisionQuality())).append("\n");
        report.append("Memory Confidence: ").append(String.format("%.2f", CognitiveFidelityHook.getMemoryConfidence())).append("\n");
        report.append("Cognitive Load: ").append(String.format("%.2f", CognitiveFidelityHook.getCognitiveLoad())).append("\n");
        report.append("\n");

        report.append("=== BEHAVIORAL PROFILE ===\n");
        report.append("Navigation Style: ").append(CognitiveFidelityHook.getCurrentNavigationStyle()).append("\n");
        report.append("Interaction Speed: ").append(String.format("%.2f", CognitiveFidelityHook.getInteractionSpeedMultiplier())).append("x\n");
        report.append("Error Probability: ").append(String.format("%.2f", CognitiveFidelityHook.calculateErrorProbability())).append("\n");
        report.append("\n");

        report.append("=== SIMULATION PARAMETERS ===\n");
        report.append("Task Abandonment Likelihood: ").append(CognitiveFidelityHook.shouldAbandonTask() ? "YES" : "NO").append("\n");
        report.append("Satisficing Tendency: ").append(CognitiveFidelityHook.shouldSatisfice() ? "HIGH" : "LOW").append("\n");
        report.append("Memory Reliability: ").append(String.format("%.0f", CognitiveFidelityHook.getMemoryConfidence() * 100)).append("%\n");

        HookUtils.logInfo(report.toString());
    }

    /**
     * Simulates a complete user session with cognitive realism
     */
    public static void simulateRealisticUserSession() {
        HookUtils.logInfo("=== REALISTIC USER SESSION SIMULATION ===");

        String[] taskSequence = {
            "LaunchApp",
            "BrowseCategory",
            "SearchProduct",
            "ViewProductDetail",
            "AddToCart",
            "ContinueShopping",
            "BrowseAnotherCategory",
            "ViewAnotherProduct",
            "AddToCart",
            "ViewCart",
            "ProceedToCheckout",
            "EnterShippingInfo",
            "EnterPaymentInfo",
            "ReviewOrder",
            "PlaceOrder"
        };

        HookUtils.logInfo("Simulating " + taskSequence.length + " task session:");

        int completedTasks = 0;
        int abandonedTasks = 0;
        int errors = 0;

        for (int i = 0; i < taskSequence.length; i++) {
            String task = taskSequence[i];

            HookUtils.logInfo("  Task " + (i + 1) + ": " + task);

            if (!CognitiveFidelityHook.shouldProceedWithInteraction()) {
                HookUtils.logInfo("    → Task abandoned (attention lost)");
                abandonedTasks++;

                CognitiveFidelityHook.simulateContextSwitch();
                continue;
            }

            if (CognitiveFidelityHook.shouldMakeError()) {
                HookUtils.logInfo("    → Error made due to fatigue");
                errors++;
            }

            CognitiveFidelityHook.recordScreenVisit(task);

            if (CognitiveFidelityHook.shouldRevalidate(task)) {
                HookUtils.logInfo("    → Re-validating previous information");
            }

            completedTasks++;

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if (i > 0 && i % 5 == 0) {
                CognitiveFidelityHook.evolvePreferences();
                HookUtils.logInfo("    → Preferences evolved");
            }
        }

        HookUtils.logInfo("\nSession Summary:");
        HookUtils.logInfo("  Completed tasks: " + completedTasks + "/" + taskSequence.length);
        HookUtils.logInfo("  Abandoned tasks: " + abandonedTasks);
        HookUtils.logInfo("  Errors made: " + errors);
        HookUtils.logInfo("  Final cognitive state: " + CognitiveFidelityHook.getCurrentCognitiveState());

        double completionRate = (double) completedTasks / taskSequence.length;
        if (completionRate > 0.6 && completionRate < 1.0) {
            HookUtils.logInfo("✓ Session completed with realistic imperfections");
        }
    }

    public static boolean isInitialized() {
        return CognitiveFidelityHook.isInitialized();
    }
}
