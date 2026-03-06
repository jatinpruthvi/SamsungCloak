package com.samsungcloak.xposed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * CognitiveFidelityHook - Human Cognitive Constraints Simulation for UX Testing
 *
 * Simulates realistic human cognitive limitations and behaviors for automated
 * UX testing on Samsung Galaxy A12 (SM-A125U). This class implements cognitive
 * realism hooks to ensure test scenarios account for non-linear human behavior,
 * attention limitations, decision fatigue, and imperfect recall.
 *
 * Features:
 * 1. Limited Attention Span - Context switching and mid-task abandonment simulation
 * 2. Bounded Rationality - Satisficing behavior (suboptimal option selection)
 * 3. Decision Fatigue - Progressive slowdown and error rate scaling
 * 4. Imperfect Memory - Re-validation and information recheck behaviors
 * 5. Changing Preferences - Stochastic navigation path and speed variations
 */
public class CognitiveFidelityHook {
    private static final String LOG_TAG = "SamsungCloak.CognitiveFidelity";
    private static boolean initialized = false;

    private static final Random random = new Random();
    private static long sessionStartTime;
    private static int interactionCount = 0;

    private static double currentCognitiveLoad = 0.0;
    private static double attentionLevel = 1.0;
    private static double decisionQuality = 1.0;
    private static double memoryConfidence = 1.0;

    private static Queue<String> recentScreens = new LinkedList<>();
    private static final int MAX_MEMORY_QUEUE_SIZE = 5;

    private static Map<String, Object> userPreferences = new HashMap<>();
    private static NavigationStyle currentNavigationStyle;
    private static InteractionSpeed currentInteractionSpeed;

    private static final double BASE_ABANDONMENT_PROBABILITY = 0.05;
    private static final double BASE_SATISFICING_PROBABILITY = 0.15;
    private static final double BASE_REVALIDATION_PROBABILITY = 0.10;

    private static List<String> navigationHistory = new ArrayList<>();
    private static Map<String, Integer> screenVisitCounts = new HashMap<>();

    public enum NavigationStyle {
        EXPLORATORY,      // Likes to browse and discover
        GOAL_DIRECTED,    // Straight to the point
        HABITUAL,         // Follows familiar patterns
        IMPULSIVE         // Random, easily distracted
    }

    public enum InteractionSpeed {
        VERY_SLOW(2.5),
        SLOW(1.8),
        NORMAL(1.0),
        FAST(0.6),
        VERY_FAST(0.4);

        public final double multiplier;

        InteractionSpeed(double multiplier) {
            this.multiplier = multiplier;
        }
    }

    public enum CognitiveState {
        FRESH,            // Start of session, high performance
        ENGAGED,          // Actively focused
        DISTRACTED,       // Attention wandering
        FATIGUED,         // Mental fatigue setting in
        OVERWHELMED       // High error rate, poor decisions
    }

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (initialized) {
            HookUtils.logDebug("CognitiveFidelityHook already initialized");
            return;
        }

        try {
            sessionStartTime = System.currentTimeMillis();
            interactionCount = 0;

            initializeUserProfile();
            hookInteractionMethods(lpparam);

            initialized = true;
            HookUtils.logInfo("CognitiveFidelityHook initialized - Human cognitive constraints simulation active");
            logCognitiveContext();
        } catch (Exception e) {
            HookUtils.logError("Failed to initialize CognitiveFidelityHook: " + e.getMessage());
        }
    }

    private static void initializeUserProfile() {
        NavigationStyle[] styles = NavigationStyle.values();
        currentNavigationStyle = styles[random.nextInt(styles.length)];

        InteractionSpeed[] speeds = InteractionSpeed.values();
        currentInteractionSpeed = speeds[random.nextInt(speeds.length)];

        userPreferences.put("prefersScrolling", random.nextBoolean());
        userPreferences.put("prefersGestures", random.nextBoolean());
        userPreferences.put("detailOriented", random.nextDouble() > 0.6);
        userPreferences.put("riskAverse", random.nextDouble() > 0.5);

        HookUtils.logInfo("User profile initialized - Style: " + currentNavigationStyle +
                         ", Speed: " + currentInteractionSpeed);
    }

    private static void hookInteractionMethods(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> viewClass = XposedHelpers.findClass(
                "android.view.View", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(viewClass, "performClick", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        applyDecisionFatigueDelay();

                        if (shouldAbandonTask()) {
                            HookUtils.logDebug("Task abandonment triggered - simulating distraction");
                            param.setResult(false);
                            return;
                        }

                        if (shouldSatisfice()) {
                            HookUtils.logDebug("Satisficing behavior - selecting first available option");
                        }

                        recordInteraction("click");
                    } catch (Exception e) {
                        HookUtils.logError("Error in performClick hook: " + e.getMessage());
                    }
                }
            });

            Class<?> activityClass = XposedHelpers.findClass(
                "android.app.Activity", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(activityClass, "onResume", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        String activityName = param.thisObject.getClass().getSimpleName();
                        recordScreenVisit(activityName);

                        if (shouldRevalidate(activityName)) {
                            HookUtils.logDebug("Re-validation behavior triggered for: " + activityName);
                            performRevalidationBehavior(activityName);
                        }

                        updateCognitiveLoad();
                    } catch (Exception e) {
                        HookUtils.logError("Error in onResume hook: " + e.getMessage());
                    }
                }
            });

            HookUtils.logInfo("Hooked interaction methods for cognitive fidelity");
        } catch (Exception e) {
            HookUtils.logError("Failed to hook interaction methods: " + e.getMessage());
        }
    }

    /**
     * 1. LIMITED ATTENTION SPAN
     *
     * Simulates context switching and mid-task abandonment due to
     * limited attention span. Probability increases with session
     * duration and cognitive load.
     *
     * @return true if task should be abandoned
     */
    public static boolean shouldAbandonTask() {
        double baseProbability = BASE_ABANDONMENT_PROBABILITY;

        long sessionDuration = getSessionDurationMinutes();
        double fatigueFactor = Math.min(sessionDuration / 30.0, 1.0) * 0.3;

        double attentionFactor = (1.0 - attentionLevel) * 0.4;

        double distractionEvent = random.nextDouble() < 0.1 ? 0.2 : 0.0;

        double totalProbability = baseProbability + fatigueFactor + attentionFactor + distractionEvent;

        if (currentNavigationStyle == NavigationStyle.IMPULSIVE) {
            totalProbability *= 1.5;
        }

        totalProbability = Math.min(totalProbability, 0.5);

        boolean shouldAbandon = random.nextDouble() < totalProbability;

        if (shouldAbandon) {
            attentionLevel = Math.max(0.3, attentionLevel - 0.1);
            HookUtils.logDebug("Task abandonment - Prob: " + String.format("%.2f", totalProbability) +
                              ", Attention dropped to: " + String.format("%.2f", attentionLevel));
        }

        return shouldAbandon;
    }

    /**
     * Simulates a context switch to a different task
     */
    public static void simulateContextSwitch() {
        String[] distractions = {
            "notification_check",
            "message_reply",
            "app_switch",
            "home_button",
            "quick_settings"
        };

        String distraction = distractions[random.nextInt(distractions.length)];
        long switchDuration = (long) (5000 + random.nextGaussian() * 2000);

        HookUtils.logInfo("Context switch simulated: " + distraction +
                         " for " + switchDuration + "ms");

        attentionLevel = Math.max(0.4, attentionLevel - 0.15);
        currentCognitiveLoad += 0.1;
    }

    /**
     * Returns current attention level (0.0 - 1.0)
     */
    public static double getAttentionLevel() {
        return attentionLevel;
    }

    /**
     * 2. BOUNDED RATIONALITY
     *
     * Simulates "satisficing" behavior where users choose the first
     * visible acceptable option rather than searching for the optimal one.
     * Tests UI discoverability and tolerance for suboptimal paths.
     *
     * @return true if user should satisfice (choose first option)
     */
    public static boolean shouldSatisfice() {
        double baseProbability = BASE_SATISFICING_PROBABILITY;

        double fatigueImpact = (1.0 - decisionQuality) * 0.3;

        double timePressure = random.nextDouble() < 0.2 ? 0.15 : 0.0;

        double cognitiveLoadImpact = currentCognitiveLoad * 0.2;

        double totalProbability = baseProbability + fatigueImpact + timePressure + cognitiveLoadImpact;

        switch (currentNavigationStyle) {
            case IMPULSIVE:
                totalProbability *= 1.4;
                break;
            case GOAL_DIRECTED:
                totalProbability *= 0.6;
                break;
            case EXPLORATORY:
                totalProbability *= 0.8;
                break;
            default:
                break;
        }

        totalProbability = Math.min(totalProbability, 0.8);

        boolean shouldSatisfice = random.nextDouble() < totalProbability;

        if (shouldSatisfice) {
            HookUtils.logDebug("Satisficing behavior - choosing first visible option");
        }

        return shouldSatisfice;
    }

    /**
     * Selects an option index using satisficing logic
     *
     * @param totalOptions Number of available options
     * @return Selected option index (biased toward earlier options)
     */
    public static int selectOptionWithSatisficing(int totalOptions) {
        if (totalOptions <= 1) {
            return 0;
        }

        if (shouldSatisfice()) {
            double bias = random.nextDouble();
            if (bias < 0.6) {
                return 0;
            } else if (bias < 0.85) {
                return Math.min(1, totalOptions - 1);
            } else {
                return random.nextInt(Math.min(3, totalOptions));
            }
        }

        return random.nextInt(totalOptions);
    }

    /**
     * Returns current decision quality level (0.0 - 1.0)
     */
    public static double getDecisionQuality() {
        return decisionQuality;
    }

    /**
     * 3. DECISION FATIGUE
     *
     * Implements progressive slowdown and error rate increase as the
     * test session duration increases. Simulates mental fatigue affecting
     * both speed and accuracy of decisions.
     */
    public static void applyDecisionFatigueDelay() {
        long sessionDuration = getSessionDurationMinutes();

        double fatigueLevel = Math.min(sessionDuration / 45.0, 1.0);

        double baseDelay = 200;
        double fatigueDelay = fatigueLevel * 800;

        double speedMultiplier = currentInteractionSpeed.multiplier;
        double adjustedDelay = (baseDelay + fatigueDelay) * speedMultiplier;

        double noise = random.nextGaussian() * 100;
        long finalDelay = (long) Math.max(0, adjustedDelay + noise);

        decisionQuality = Math.max(0.3, 1.0 - (fatigueLevel * 0.5));

        try {
            Thread.sleep(finalDelay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (sessionDuration > 0 && sessionDuration % 10 == 0) {
            HookUtils.logDebug("Decision fatigue - Session: " + sessionDuration +
                              "min, Delay: " + finalDelay + "ms, Quality: " + String.format("%.2f", decisionQuality));
        }
    }

    /**
     * Calculates current error probability based on decision fatigue
     *
     * @return Error probability (0.0 - 1.0)
     */
    public static double calculateErrorProbability() {
        long sessionDuration = getSessionDurationMinutes();

        double baseErrorRate = 0.02;
        double fatigueError = Math.min(sessionDuration / 60.0, 0.3);
        double cognitiveError = currentCognitiveLoad * 0.1;
        double attentionError = (1.0 - attentionLevel) * 0.15;

        double totalError = baseErrorRate + fatigueError + cognitiveError + attentionError;

        return Math.min(totalError, 0.5);
    }

    /**
     * Determines if current action should result in an error
     */
    public static boolean shouldMakeError() {
        double errorProb = calculateErrorProbability();
        boolean shouldError = random.nextDouble() < errorProb;

        if (shouldError) {
            HookUtils.logDebug("Simulated error due to fatigue - Prob: " + String.format("%.2f", errorProb));
        }

        return shouldError;
    }

    /**
     * Returns current cognitive state
     */
    public static CognitiveState getCurrentCognitiveState() {
        long sessionDuration = getSessionDurationMinutes();
        double fatigueLevel = Math.min(sessionDuration / 45.0, 1.0);

        if (sessionDuration < 5) {
            return CognitiveState.FRESH;
        } else if (fatigueLevel < 0.3 && attentionLevel > 0.7) {
            return CognitiveState.ENGAGED;
        } else if (attentionLevel < 0.5) {
            return CognitiveState.DISTRACTED;
        } else if (fatigueLevel > 0.6) {
            return CognitiveState.OVERWHELMED;
        } else {
            return CognitiveState.FATIGUED;
        }
    }

    /**
     * 4. IMPERFECT MEMORY
     *
     * Simulates re-validation behaviors where the user periodically
     * returns to previous screens to verify information already seen.
     * Models human working memory limitations and confidence decay.
     *
     * @param screenName Current screen identifier
     * @return true if re-validation should occur
     */
    public static boolean shouldRevalidate(String screenName) {
        if (!recentScreens.contains(screenName)) {
            return false;
        }

        double baseProbability = BASE_REVALIDATION_PROBABILITY;

        double memoryFactor = (1.0 - memoryConfidence) * 0.3;

        int visitCount = screenVisitCounts.getOrDefault(screenName, 0);
        double familiarityFactor = Math.min(visitCount * 0.05, 0.2);

        double complexityFactor = currentCognitiveLoad * 0.15;

        double totalProbability = baseProbability + memoryFactor - familiarityFactor + complexityFactor;

        if ((boolean) userPreferences.getOrDefault("detailOriented", false)) {
            totalProbability *= 1.3;
        }

        totalProbability = Math.max(0.02, Math.min(totalProbability, 0.5));

        boolean shouldRevalidate = random.nextDouble() < totalProbability;

        if (shouldRevalidate) {
            HookUtils.logDebug("Re-validation triggered for: " + screenName);
            memoryConfidence = Math.min(1.0, memoryConfidence + 0.1);
        } else {
            memoryConfidence = Math.max(0.3, memoryConfidence - 0.05);
        }

        return shouldRevalidate;
    }

    /**
     * Performs re-validation behavior
     */
    private static void performRevalidationBehavior(String screenName) {
        long checkDuration = (long) (1500 + random.nextGaussian() * 500);

        HookUtils.logInfo("Re-validating information on: " + screenName +
                         " for " + checkDuration + "ms");

        try {
            Thread.sleep(checkDuration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Returns current memory confidence level (0.0 - 1.0)
     */
    public static double getMemoryConfidence() {
        return memoryConfidence;
    }

    /**
     * Records a screen visit for memory tracking
     */
    public static void recordScreenVisit(String screenName) {
        recentScreens.offer(screenName);
        if (recentScreens.size() > MAX_MEMORY_QUEUE_SIZE) {
            recentScreens.poll();
        }

        screenVisitCounts.merge(screenName, 1, Integer::sum);
        navigationHistory.add(screenName);

        memoryConfidence = Math.max(0.3, memoryConfidence - 0.02);
    }

    /**
     * 5. CHANGING PREFERENCES
     *
     * Stochastic model that varies the user's preferred navigation paths
     * and interaction speeds between sessions. Simulates natural human
     * adaptation and preference shifts over time.
     */
    public static void evolvePreferences() {
        if (random.nextDouble() > 0.7) {
            NavigationStyle[] styles = NavigationStyle.values();
            NavigationStyle oldStyle = currentNavigationStyle;
            currentNavigationStyle = styles[random.nextInt(styles.length)];

            if (oldStyle != currentNavigationStyle) {
                HookUtils.logInfo("Navigation style evolved: " + oldStyle + " -> " + currentNavigationStyle);
            }
        }

        if (random.nextDouble() > 0.6) {
            InteractionSpeed[] speeds = InteractionSpeed.values();
            InteractionSpeed oldSpeed = currentInteractionSpeed;
            currentInteractionSpeed = speeds[random.nextInt(speeds.length)];

            if (oldSpeed != currentInteractionSpeed) {
                HookUtils.logInfo("Interaction speed evolved: " + oldSpeed + " -> " + currentInteractionSpeed);
            }
        }

        userPreferences.put("prefersScrolling", random.nextDouble() > 0.4);
        userPreferences.put("prefersGestures", random.nextDouble() > 0.5);

        double preferenceDrift = random.nextGaussian() * 0.1;
        currentCognitiveLoad = Math.max(0.0, Math.min(1.0, currentCognitiveLoad + preferenceDrift));

        HookUtils.logDebug("Preferences evolved - Cognitive Load: " + String.format("%.2f", currentCognitiveLoad));
    }

    /**
     * Returns interaction speed multiplier for current session
     */
    public static double getInteractionSpeedMultiplier() {
        return currentInteractionSpeed.multiplier;
    }

    /**
     * Returns current navigation style
     */
    public static NavigationStyle getCurrentNavigationStyle() {
        return currentNavigationStyle;
    }

    /**
     * Generates a navigation path based on current preferences
     */
    public static List<String> generateNavigationPath(List<String> availablePaths) {
        List<String> selectedPath = new ArrayList<>();

        if (availablePaths.isEmpty()) {
            return selectedPath;
        }

        switch (currentNavigationStyle) {
            case GOAL_DIRECTED:
                selectedPath.add(availablePaths.get(0));
                break;

            case EXPLORATORY:
                int exploreCount = Math.min(availablePaths.size(), 2 + random.nextInt(3));
                for (int i = 0; i < exploreCount; i++) {
                    selectedPath.add(availablePaths.get(i));
                }
                break;

            case HABITUAL:
                if (!navigationHistory.isEmpty()) {
                    String lastPath = navigationHistory.get(navigationHistory.size() - 1);
                    if (availablePaths.contains(lastPath)) {
                        selectedPath.add(lastPath);
                        break;
                    }
                }
                selectedPath.add(availablePaths.get(0));
                break;

            case IMPULSIVE:
                selectedPath.add(availablePaths.get(random.nextInt(availablePaths.size())));
                if (random.nextBoolean() && availablePaths.size() > 1) {
                    selectedPath.add(availablePaths.get(random.nextInt(availablePaths.size())));
                }
                break;
        }

        return selectedPath;
    }

    /**
     * MASTER COGNITIVE FIDELITY METHOD
     *
     * Applies all cognitive constraints to an interaction
     */
    public static boolean shouldProceedWithInteraction() {
        if (shouldAbandonTask()) {
            return false;
        }

        applyDecisionFatigueDelay();

        if (shouldMakeError()) {
            HookUtils.logDebug("Cognitive error simulated - interaction may fail");
        }

        interactionCount++;

        if (interactionCount % 20 == 0) {
            evolvePreferences();
        }

        return true;
    }

    /**
     * Records an interaction for tracking
     */
    private static void recordInteraction(String interactionType) {
        interactionCount++;

        if (interactionCount % 10 == 0) {
            updateCognitiveLoad();
        }
    }

    /**
     * Updates cognitive load based on session state
     */
    private static void updateCognitiveLoad() {
        long sessionDuration = getSessionDurationMinutes();

        double timeLoad = Math.min(sessionDuration / 60.0, 0.4);
        double interactionLoad = Math.min(interactionCount / 100.0, 0.3);
        double randomFluctuation = (random.nextDouble() - 0.5) * 0.2;

        currentCognitiveLoad = Math.max(0.0, Math.min(1.0,
            timeLoad + interactionLoad + randomFluctuation));

        HookUtils.logDebug("Cognitive load updated: " + String.format("%.2f", currentCognitiveLoad));
    }

    /**
     * Returns session duration in minutes
     */
    private static long getSessionDurationMinutes() {
        return TimeUnit.MILLISECONDS.toMinutes(
            System.currentTimeMillis() - sessionStartTime
        );
    }

    /**
     * Logs current cognitive context for debugging
     */
    private static void logCognitiveContext() {
        HookUtils.logInfo("=== COGNITIVE FIDELITY CONTEXT ===");
        HookUtils.logInfo("Navigation Style: " + currentNavigationStyle);
        HookUtils.logInfo("Interaction Speed: " + currentInteractionSpeed);
        HookUtils.logInfo("Attention Level: " + String.format("%.2f", attentionLevel));
        HookUtils.logInfo("Decision Quality: " + String.format("%.2f", decisionQuality));
        HookUtils.logInfo("Memory Confidence: " + String.format("%.2f", memoryConfidence));
        HookUtils.logInfo("Cognitive Load: " + String.format("%.2f", currentCognitiveLoad));
        HookUtils.logInfo("Cognitive State: " + getCurrentCognitiveState());
        HookUtils.logInfo("===================================");
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static int getInteractionCount() {
        return interactionCount;
    }

    public static long getSessionDuration() {
        return System.currentTimeMillis() - sessionStartTime;
    }

    public static double getCognitiveLoad() {
        return currentCognitiveLoad;
    }

    public static Map<String, Object> getUserPreferences() {
        return new HashMap<>(userPreferences);
    }

    public static List<String> getNavigationHistory() {
        return new ArrayList<>(navigationHistory);
    }
}
