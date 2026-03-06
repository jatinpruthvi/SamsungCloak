package com.samsungcloak.xposed;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Sentiment-Driven Engagement Model
 * Simulates realistic human emotional variability for high-fidelity UX testing.
 * Models mood shifts, engagement fluctuations, impulse vs. delayed actions,
 * excitement spikes, and apathy phases to provide authentic behavioral data.
 *
 * Target Device: Samsung Galaxy A12 (SM-A125U)
 * Purpose: UX Resilience Testing with Emotional Realism
 */
public class SentimentEngagementHook {

    private static final String LOG_TAG = "SamsungCloak.SentimentEngagement";
    private static volatile boolean initialized = false;

    // Session tracking
    private static final AtomicLong sessionStartTime = new AtomicLong(System.currentTimeMillis());
    private static final AtomicInteger totalInteractionCount = new AtomicInteger(0);
    private static final AtomicInteger consecutiveInteractions = new AtomicInteger(0);

    // Current emotional state
    private static final AtomicReference<EngagementState> currentState =
        new AtomicReference<>(EngagementState.MODERATE_ENGAGEMENT);
    private static final AtomicReference<EmotionalState> currentMood =
        new AtomicReference<>(EmotionalState.NEUTRAL);

    // State transition probability matrix
    private static final Map<EngagementState, double[]> transitionMatrix = new EnumMap<>(EngagementState.class);

    // Apathy tracking
    private static final AtomicInteger ignoredPrompts = new AtomicInteger(0);
    private static final AtomicInteger totalPrompts = new AtomicInteger(0);
    private static final AtomicLong lastApathyStart = new AtomicLong(0);

    // Excitement spike tracking
    private static final AtomicLong lastExcitementSpike = new AtomicLong(0);
    private static final AtomicInteger burstInteractionCount = new AtomicInteger(0);

    // Decision style tracking
    private static final AtomicReference<DecisionStyle> currentDecisionStyle =
        new AtomicReference<>(DecisionStyle.BALANCED);

    static {
        initializeTransitionMatrix();
    }

    /**
     * Engagement states representing different levels of user attention
     */
    public enum EngagementState {
        HIGH_ENGAGEMENT(0.8, 50, 200),      // Rapid clicks, 50-200ms between actions
        MODERATE_ENGAGEMENT(0.5, 200, 800), // Normal pace, 200-800ms
        LOW_ENGAGEMENT(0.2, 1000, 5000),    // Long delays, 1-5 seconds
        DISENGAGED(0.05, 5000, 30000),      // Very long pauses, 5-30 seconds
        BURST_MODE(0.9, 30, 100);           // Excitement spike, 30-100ms

        private final double engagementProbability;
        private final int minDelayMs;
        private final int maxDelayMs;

        EngagementState(double engagementProbability, int minDelayMs, int maxDelayMs) {
            this.engagementProbability = engagementProbability;
            this.minDelayMs = minDelayMs;
            this.maxDelayMs = maxDelayMs;
        }

        public double getEngagementProbability() {
            return engagementProbability;
        }

        public int getRandomDelay() {
            return ThreadLocalRandom.current().nextInt(minDelayMs, maxDelayMs + 1);
        }

        public int getMinDelayMs() {
            return minDelayMs;
        }

        public int getMaxDelayMs() {
            return maxDelayMs;
        }
    }

    /**
     * Emotional states affecting decision-making patterns
     */
    public enum EmotionalState {
        EXCITED(1.5, 0.3, 0.7),      // Fast actions, high error rate, high engagement
        CONTENT(1.0, 0.1, 0.6),      // Normal pace, low error rate, moderate engagement
        NEUTRAL(1.0, 0.15, 0.5),     // Baseline behavior
        BORED(0.7, 0.25, 0.3),       // Slow actions, higher error rate, low engagement
        FRUSTRATED(0.6, 0.4, 0.4),   // Erratic timing, high error rate
        DISTRACTED(0.8, 0.3, 0.2);   // Inconsistent, prone to context switches

        private final double speedMultiplier;
        private final double errorProbability;
        private final double engagementBias;

        EmotionalState(double speedMultiplier, double errorProbability, double engagementBias) {
            this.speedMultiplier = speedMultiplier;
            this.errorProbability = errorProbability;
            this.engagementBias = engagementBias;
        }

        public double getSpeedMultiplier() {
            return speedMultiplier;
        }

        public double getErrorProbability() {
            return errorProbability;
        }

        public double getEngagementBias() {
            return engagementBias;
        }
    }

    /**
     * Decision styles for impulse vs. deliberation modeling
     */
    public enum DecisionStyle {
        IMPULSIVE(0.1, 0.3, 0.8),        // 100-300ms, 80% instant taps
        SPONTANEOUS(0.3, 0.6, 0.6),      // 300-600ms, 60% quick decisions
        BALANCED(0.8, 1.5, 0.4),         // 800-1500ms, balanced approach
        DELIBERATE(2.0, 4.0, 0.2),       // 2-4 seconds, careful consideration
        EXTENDED_DELIBERATION(5.0, 15.0, 0.05); // 5-15 seconds, very careful

        private final double minDelaySeconds;
        private final double maxDelaySeconds;
        private final double instantActionProbability;

        DecisionStyle(double minDelaySeconds, double maxDelaySeconds, double instantActionProbability) {
            this.minDelaySeconds = minDelaySeconds;
            this.maxDelaySeconds = maxDelaySeconds;
            this.instantActionProbability = instantActionProbability;
        }

        public long getRandomDelayMs() {
            double seconds = ThreadLocalRandom.current().nextDouble(minDelaySeconds, maxDelaySeconds);
            return (long) (seconds * 1000);
        }

        public boolean shouldActInstantly() {
            return ThreadLocalRandom.current().nextDouble() < instantActionProbability;
        }
    }

    /**
     * Initialize the Markov transition probability matrix
     * Rows represent current state, columns represent next state probabilities
     */
    private static void initializeTransitionMatrix() {
        // HIGH_ENGAGEMENT transitions
        transitionMatrix.put(EngagementState.HIGH_ENGAGEMENT,
            new double[]{0.50, 0.30, 0.15, 0.03, 0.02}); // Mostly stay high, some decay

        // MODERATE_ENGAGEMENT transitions
        transitionMatrix.put(EngagementState.MODERATE_ENGAGEMENT,
            new double[]{0.20, 0.45, 0.25, 0.08, 0.02}); // Stable with some variation

        // LOW_ENGAGEMENT transitions
        transitionMatrix.put(EngagementState.LOW_ENGAGEMENT,
            new double[]{0.10, 0.25, 0.40, 0.20, 0.05}); // Risk of further disengagement

        // DISENGAGED transitions
        transitionMatrix.put(EngagementState.DISENGAGED,
            new double[]{0.05, 0.15, 0.30, 0.45, 0.05}); // Sticky state, some recovery

        // BURST_MODE transitions
        transitionMatrix.put(EngagementState.BURST_MODE,
            new double[]{0.30, 0.40, 0.20, 0.05, 0.05}); // Short-lived, returns to moderate
    }

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (initialized) {
            HookUtils.logDebug(LOG_TAG + " already initialized");
            return;
        }

        try {
            hookTouchEventTiming(lpparam);
            hookActivityLifecycle(lpparam);
            startStateMachineDaemon();
            initialized = true;
            HookUtils.logInfo(LOG_TAG + " initialized successfully");
        } catch (Exception e) {
            HookUtils.logError(LOG_TAG + " initialization failed: " + e.getMessage());
        }
    }

    /**
     * Hook 1: Mood Shift Simulation
     * State-machine logic transitioning between High/Low engagement states
     * based on probability matrix with emotional state influence
     */
    private static void startStateMachineDaemon() {
        Thread daemon = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(ThreadLocalRandom.current().nextInt(30000, 120000)); // 30s-2min intervals
                    evaluateStateTransition();
                    evaluateEmotionalShift();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        daemon.setName("SentimentStateMachine");
        daemon.setDaemon(true);
        daemon.start();
    }

    /**
     * Evaluate and execute state transition based on Markov matrix
     * with emotional bias adjustment
     */
    private static void evaluateStateTransition() {
        EngagementState current = currentState.get();
        double[] probabilities = transitionMatrix.get(current);

        // Apply emotional bias
        EmotionalState mood = currentMood.get();
        double[] adjustedProbs = adjustProbabilitiesByMood(probabilities, mood);

        // Select next state
        double random = ThreadLocalRandom.current().nextDouble();
        double cumulative = 0.0;
        EngagementState[] states = EngagementState.values();

        for (int i = 0; i < adjustedProbs.length; i++) {
            cumulative += adjustedProbs[i];
            if (random <= cumulative) {
                EngagementState next = states[i];
                if (next != current) {
                    currentState.set(next);
                    consecutiveInteractions.set(0);
                    HookUtils.logDebug(LOG_TAG + " State transition: " + current + " -> " + next +
                        " (mood: " + mood + ")");
                }
                break;
            }
        }
    }

    /**
     * Adjust transition probabilities based on current emotional state
     */
    private static double[] adjustProbabilitiesByMood(double[] probs, EmotionalState mood) {
        double[] adjusted = probs.clone();
        double bias = mood.getEngagementBias();

        // Boost higher engagement states when positive, lower when negative
        if (bias > 0.6) {
            // Positive mood - boost HIGH_ENGAGEMENT and BURST_MODE
            adjusted[0] *= 1.2; // HIGH_ENGAGEMENT
            adjusted[4] *= 1.3; // BURST_MODE
            normalizeProbabilities(adjusted);
        } else if (bias < 0.4) {
            // Negative mood - boost LOW_ENGAGEMENT and DISENGAGED
            adjusted[2] *= 1.3; // LOW_ENGAGEMENT
            adjusted[3] *= 1.2; // DISENGAGED
            normalizeProbabilities(adjusted);
        }

        return adjusted;
    }

    private static void normalizeProbabilities(double[] probs) {
        double sum = 0.0;
        for (double p : probs) sum += p;
        for (int i = 0; i < probs.length; i++) probs[i] /= sum;
    }

    /**
     * Evaluate and execute emotional state shift
     */
    private static void evaluateEmotionalShift() {
        EmotionalState current = currentMood.get();
        double shiftProbability = 0.15; // 15% chance of emotional shift

        // Frustration builds with consecutive interactions
        if (consecutiveInteractions.get() > 50) {
            shiftProbability += 0.1;
        }

        // Long sessions lead to boredom
        long sessionMinutes = getSessionDurationMinutes();
        if (sessionMinutes > 30) {
            shiftProbability += 0.05;
        }

        if (ThreadLocalRandom.current().nextDouble() < shiftProbability) {
            EmotionalState next = selectNextEmotionalState(current);
            if (next != current) {
                currentMood.set(next);
                HookUtils.logDebug(LOG_TAG + " Mood shift: " + current + " -> " + next);
            }
        }
    }

    private static EmotionalState selectNextEmotionalState(EmotionalState current) {
        EmotionalState[] states = EmotionalState.values();

        // Weight transitions based on session context
        double[] weights = new double[states.length];
        for (int i = 0; i < states.length; i++) {
            weights[i] = 1.0;
        }

        // Current state sticky
        weights[current.ordinal()] = 3.0;

        // Frustrated users tend toward disengagement
        if (current == EmotionalState.FRUSTRATED) {
            weights[EmotionalState.BORED.ordinal()] = 2.0;
            weights[EmotionalState.DISTRACTED.ordinal()] = 2.0;
        }

        // Excitement fades to content
        if (current == EmotionalState.EXCITED) {
            weights[EmotionalState.CONTENT.ordinal()] = 3.0;
        }

        // Boredom leads to excitement or frustration
        if (current == EmotionalState.BORED) {
            weights[EmotionalState.EXCITED.ordinal()] = 1.5;
            weights[EmotionalState.FRUSTRATED.ordinal()] = 1.5;
        }

        // Weighted random selection
        double totalWeight = 0.0;
        for (double w : weights) totalWeight += w;

        double random = ThreadLocalRandom.current().nextDouble() * totalWeight;
        double cumulative = 0.0;

        for (int i = 0; i < weights.length; i++) {
            cumulative += weights[i];
            if (random <= cumulative) {
                return states[i];
            }
        }

        return states[states.length - 1];
    }

    /**
     * Hook 2: Engagement Fluctuations - "Attention Economy"
     * Simulates sudden app minimization for background notifications
     */
    private static void hookActivityLifecycle(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> activityClass = XposedHelpers.findClass(
                "android.app.Activity", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(activityClass, "onUserInteraction", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        totalInteractionCount.incrementAndGet();
                        consecutiveInteractions.incrementAndGet();

                        // Check for sudden disengagement (notification check)
                        if (shouldTriggerContextSwitch()) {
                            simulateContextSwitch();
                        }
                    } catch (Exception e) {
                        HookUtils.logDebug(LOG_TAG + " Error in interaction hook: " + e.getMessage());
                    }
                }
            });

            HookUtils.logInfo(LOG_TAG + " Hooked Activity lifecycle for engagement tracking");
        } catch (Exception e) {
            HookUtils.logError(LOG_TAG + " Failed to hook Activity lifecycle: " + e.getMessage());
        }
    }

    /**
     * Determine if user should suddenly switch contexts (check notification, etc.)
     */
    public static boolean shouldTriggerContextSwitch() {
        double baseProbability = 0.02; // 2% base chance per interaction

        // Distracted users switch more
        if (currentMood.get() == EmotionalState.DISTRACTED) {
            baseProbability += 0.08;
        }

        // Low engagement leads to wandering
        if (currentState.get().engagementProbability < 0.3) {
            baseProbability += 0.05;
        }

        // Frustrated users abandon tasks
        if (currentMood.get() == EmotionalState.FRUSTRATED) {
            baseProbability += 0.1;
        }

        // Session duration fatigue
        if (getSessionDurationMinutes() > 20) {
            baseProbability += 0.03;
        }

        return ThreadLocalRandom.current().nextDouble() < Math.min(baseProbability, 0.25);
    }

    private static void simulateContextSwitch() {
        HookUtils.logDebug(LOG_TAG + " Context switch triggered - simulating notification check");
        consecutiveInteractions.set(0);

        // Simulate different types of context switches
        String[] switchTypes = {
            "notification_check",
            "quick_message_reply",
            "app_switch",
            "home_button_press",
            "quick_settings_check"
        };

        String switchType = switchTypes[ThreadLocalRandom.current().nextInt(switchTypes.length)];

        // Vary the duration of the context switch
        int switchDuration = ThreadLocalRandom.current().nextInt(2000, 15000);

        HookUtils.logDebug(LOG_TAG + " Context switch type: " + switchType + ", duration: " + switchDuration + "ms");
    }

    /**
     * Hook 3: Impulse vs. Delayed Action
     * Stochastic model fluctuating between instant-tap and extended-pause
     */
    private static void hookTouchEventTiming(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> viewClass = XposedHelpers.findClass(
                "android.view.View", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(viewClass, "performClick", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        // Determine decision style for this action
                        DecisionStyle style = determineDecisionStyle();
                        currentDecisionStyle.set(style);

                        // Apply appropriate delay
                        long delay = calculateActionDelay(style);
                        if (delay > 0) {
                            Thread.sleep(delay);
                        }

                        HookUtils.logDebug(LOG_TAG + " Action delay: " + delay + "ms (style: " + style + ")");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        HookUtils.logDebug(LOG_TAG + " Error in click timing: " + e.getMessage());
                    }
                }
            });

            HookUtils.logInfo(LOG_TAG + " Hooked touch event timing for impulse/delay modeling");
        } catch (Exception e) {
            HookUtils.logError(LOG_TAG + " Failed to hook touch events: " + e.getMessage());
        }
    }

    /**
     * Determine decision style based on context and emotional state
     */
    private static DecisionStyle determineDecisionStyle() {
        EmotionalState mood = currentMood.get();
        EngagementState engagement = currentState.get();

        // Critical actions (checkout, sign-up) use different patterns
        boolean isCriticalAction = isCriticalActionContext();

        if (isCriticalAction) {
            return determineCriticalActionStyle(mood);
        }

        // Regular actions based on mood and engagement
        double random = ThreadLocalRandom.current().nextDouble();
        double impulsiveness = calculateImpulsivenessScore(mood, engagement);

        if (random < impulsiveness * 0.4) {
            return DecisionStyle.IMPULSIVE;
        } else if (random < impulsiveness * 0.7) {
            return DecisionStyle.SPONTANEOUS;
        } else if (random < 0.6 + (impulsiveness * 0.2)) {
            return DecisionStyle.BALANCED;
        } else if (random < 0.85) {
            return DecisionStyle.DELIBERATE;
        } else {
            return DecisionStyle.EXTENDED_DELIBERATION;
        }
    }

    private static DecisionStyle determineCriticalActionStyle(EmotionalState mood) {
        double random = ThreadLocalRandom.current().nextDouble();

        // Even impulsive users pause for critical actions
        switch (mood) {
            case EXCITED:
                // Excited users might rush critical actions
                if (random < 0.3) return DecisionStyle.SPONTANEOUS;
                if (random < 0.7) return DecisionStyle.BALANCED;
                return DecisionStyle.DELIBERATE;

            case FRUSTRATED:
                // Frustrated users might give up or rush
                if (random < 0.2) return DecisionStyle.IMPULSIVE;
                if (random < 0.5) return DecisionStyle.SPONTANEOUS;
                return DecisionStyle.DELIBERATE;

            case DISTRACTED:
                // Distracted users have inconsistent patterns
                if (random < 0.4) return DecisionStyle.IMPULSIVE;
                if (random < 0.6) return DecisionStyle.EXTENDED_DELIBERATION;
                return DecisionStyle.BALANCED;

            case BORED:
                // Bored users rush to finish
                if (random < 0.4) return DecisionStyle.SPONTANEOUS;
                return DecisionStyle.BALANCED;

            default:
                // Neutral/content users are more careful
                if (random < 0.2) return DecisionStyle.SPONTANEOUS;
                if (random < 0.6) return DecisionStyle.BALANCED;
                if (random < 0.9) return DecisionStyle.DELIBERATE;
                return DecisionStyle.EXTENDED_DELIBERATION;
        }
    }

    private static double calculateImpulsivenessScore(EmotionalState mood, EngagementState engagement) {
        double baseScore = 0.5;

        // Mood impact
        baseScore += (mood.getSpeedMultiplier() - 1.0) * 0.5;

        // Engagement impact
        baseScore += (engagement.engagementProbability - 0.5) * 0.3;

        // Session novelty - early sessions more exploratory
        if (getSessionDurationMinutes() < 5) {
            baseScore += 0.1;
        }

        // Fatigue reduces impulsiveness (tired users are slower)
        if (getSessionDurationMinutes() > 30) {
            baseScore -= 0.15;
        }

        return Math.max(0.1, Math.min(0.9, baseScore));
    }

    private static boolean isCriticalActionContext() {
        // Check if we're in a critical action context based on stack trace
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        String[] criticalPatterns = {
            "checkout", "payment", "purchase", "subscribe", "signup",
            "register", "confirm", "submit", "delete", "remove"
        };

        for (StackTraceElement element : stack) {
            String className = element.getClassName().toLowerCase();
            String methodName = element.getMethodName().toLowerCase();

            for (String pattern : criticalPatterns) {
                if (className.contains(pattern) || methodName.contains(pattern)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static long calculateActionDelay(DecisionStyle style) {
        EmotionalState mood = currentMood.get();
        long baseDelay = style.getRandomDelayMs();

        // Apply mood multiplier
        baseDelay = (long) (baseDelay / mood.getSpeedMultiplier());

        // Add emotional variability
        double noise = HookUtils.generateGaussianNoise(0.2f);
        baseDelay = (long) (baseDelay * (1.0 + noise));

        return Math.max(50, baseDelay); // Minimum 50ms
    }

    /**
     * Hook 4: Excitement Spikes - "Burst Interaction"
     * Rapid, non-linear navigation when user discovers new features
     */
    public static boolean shouldTriggerExcitementSpike() {
        double baseProbability = 0.03; // 3% base chance

        // Discovery events increase excitement
        if (totalInteractionCount.get() % 15 == 0) {
            baseProbability += 0.05;
        }

        // Positive mood increases excitement likelihood
        if (currentMood.get() == EmotionalState.EXCITED) {
            baseProbability += 0.1;
        }

        // Content users occasionally get excited
        if (currentMood.get() == EmotionalState.CONTENT) {
            baseProbability += 0.02;
        }

        // New users more prone to excitement
        if (getSessionDurationMinutes() < 10) {
            baseProbability += 0.04;
        }

        // Cooldown period between spikes
        long timeSinceLastSpike = System.currentTimeMillis() - lastExcitementSpike.get();
        if (timeSinceLastSpike < 60000) { // 1 minute cooldown
            baseProbability *= 0.1;
        }

        boolean shouldTrigger = ThreadLocalRandom.current().nextDouble() < Math.min(baseProbability, 0.2);

        if (shouldTrigger) {
            triggerExcitementSpike();
        }

        return shouldTrigger;
    }

    private static void triggerExcitementSpike() {
        lastExcitementSpike.set(System.currentTimeMillis());
        burstInteractionCount.set(0);
        currentState.set(EngagementState.BURST_MODE);

        HookUtils.logDebug(LOG_TAG + " EXCITEMENT SPIKE TRIGGERED - Burst mode activated");

        // Burst mode lasts for a random number of interactions
        Thread burstThread = new Thread(() -> {
            int burstDuration = ThreadLocalRandom.current().nextInt(5, 20);

            for (int i = 0; i < burstDuration; i++) {
                burstInteractionCount.incrementAndGet();

                // Rapid interactions with minimal delay
                try {
                    Thread.sleep(ThreadLocalRandom.current().nextInt(50, 150));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            // Return to moderate engagement after burst
            currentState.set(EngagementState.MODERATE_ENGAGEMENT);
            currentMood.set(EmotionalState.CONTENT); // Satisfied after exploration

            HookUtils.logDebug(LOG_TAG + " Burst mode ended after " + burstDuration + " interactions");
        });

        burstThread.setName("ExcitementBurst");
        burstThread.setDaemon(true);
        burstThread.start();
    }

    /**
     * Hook 5: Apathy Phases - "The Ignored CTA"
     * Intentionally ignores UI prompts for no deterministic reason
     */
    public static boolean shouldIgnorePrompt(String promptType) {
        totalPrompts.incrementAndGet();

        double ignoreProbability = calculateApathyProbability(promptType);

        boolean shouldIgnore = ThreadLocalRandom.current().nextDouble() < ignoreProbability;

        if (shouldIgnore) {
            ignoredPrompts.incrementAndGet();
            lastApathyStart.set(System.currentTimeMillis());

            HookUtils.logDebug(LOG_TAG + " APATHY PHASE: Ignoring " + promptType +
                " (total ignored: " + ignoredPrompts.get() + "/" + totalPrompts.get() + ")");
        }

        return shouldIgnore;
    }

    private static double calculateApathyProbability(String promptType) {
        double baseApathy = 0.05; // 5% base ignore rate

        // Emotional state impact
        EmotionalState mood = currentMood.get();
        switch (mood) {
            case BORED:
                baseApathy += 0.15;
                break;
            case DISTRACTED:
                baseApathy += 0.2;
                break;
            case FRUSTRATED:
                baseApathy += 0.1;
                break;
            case EXCITED:
                baseApathy -= 0.03; // Excited users engage more
                break;
            default:
                break;
        }

        // Engagement state impact
        EngagementState engagement = currentState.get();
        if (engagement == EngagementState.DISENGAGED) {
            baseApathy += 0.25;
        } else if (engagement == EngagementState.LOW_ENGAGEMENT) {
            baseApathy += 0.1;
        }

        // Prompt type impact
        if (isInterruptivePrompt(promptType)) {
            baseApathy += 0.1; // Users ignore interruptive prompts more
        }

        if (isPromotionalPrompt(promptType)) {
            baseApathy += 0.15; // Users ignore ads and promotions
        }

        // Session fatigue
        if (getSessionDurationMinutes() > 25) {
            baseApathy += 0.08;
        }

        // Apathy momentum - once ignoring, more likely to continue
        long timeSinceLastApathy = System.currentTimeMillis() - lastApathyStart.get();
        if (timeSinceLastApathy < 30000) { // Within 30 seconds of last apathy
            baseApathy += 0.1;
        }

        // Streak breaker - occasionally engage after ignoring many
        if (ignoredPrompts.get() > 3) {
            baseApathy *= 0.7;
        }

        return Math.max(0.0, Math.min(0.8, baseApathy));
    }

    private static boolean isInterruptivePrompt(String promptType) {
        String[] interruptive = {
            "popup", "modal", "dialog", "interrupt", "blocker",
            "permission_request", "rating_request", "update_prompt"
        };

        String lower = promptType.toLowerCase();
        for (String pattern : interruptive) {
            if (lower.contains(pattern)) return true;
        }
        return false;
    }

    private static boolean isPromotionalPrompt(String promptType) {
        String[] promotional = {
            "ad", "promo", "offer", "discount", "premium", "upgrade",
            "subscription", "newsletter", "notification_opt_in"
        };

        String lower = promptType.toLowerCase();
        for (String pattern : promotional) {
            if (lower.contains(pattern)) return true;
        }
        return false;
    }

    /**
     * Extended apathy phase - completely disengage for a period
     */
    public static boolean isInExtendedApathyPhase() {
        if (ignoredPrompts.get() < 2) return false;

        double phaseProbability = 0.1;

        // Bored and disengaged users enter extended apathy
        if (currentMood.get() == EmotionalState.BORED &&
            currentState.get() == EngagementState.DISENGAGED) {
            phaseProbability += 0.2;
        }

        return ThreadLocalRandom.current().nextDouble() < phaseProbability;
    }

    public static long getExtendedApathyDuration() {
        return ThreadLocalRandom.current().nextLong(5000, 60000); // 5-60 seconds
    }

    /**
     * Utility methods for external integration
     */
    public static long getInteractionDelay() {
        return currentState.get().getRandomDelay();
    }

    public static double getCurrentEngagementProbability() {
        return currentState.get().getEngagementProbability();
    }

    public static EngagementState getCurrentEngagementState() {
        return currentState.get();
    }

    public static EmotionalState getCurrentMood() {
        return currentMood.get();
    }

    public static DecisionStyle getCurrentDecisionStyle() {
        return currentDecisionStyle.get();
    }

    public static long getSessionDurationMinutes() {
        return (System.currentTimeMillis() - sessionStartTime.get()) / 60000;
    }

    public static int getTotalInteractionCount() {
        return totalInteractionCount.get();
    }

    public static int getIgnoredPromptsCount() {
        return ignoredPrompts.get();
    }

    public static double getApathyRate() {
        int total = totalPrompts.get();
        return total > 0 ? (double) ignoredPrompts.get() / total : 0.0;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Force a specific emotional state (for testing)
     */
    public static void setEmotionalState(EmotionalState state) {
        currentMood.set(state);
        HookUtils.logDebug(LOG_TAG + " Emotional state manually set to: " + state);
    }

    /**
     * Force a specific engagement state (for testing)
     */
    public static void setEngagementState(EngagementState state) {
        currentState.set(state);
        HookUtils.logDebug(LOG_TAG + " Engagement state manually set to: " + state);
    }

    /**
     * Reset all counters and states
     */
    public static void reset() {
        sessionStartTime.set(System.currentTimeMillis());
        totalInteractionCount.set(0);
        consecutiveInteractions.set(0);
        ignoredPrompts.set(0);
        totalPrompts.set(0);
        currentState.set(EngagementState.MODERATE_ENGAGEMENT);
        currentMood.set(EmotionalState.NEUTRAL);
        currentDecisionStyle.set(DecisionStyle.BALANCED);
        HookUtils.logInfo(LOG_TAG + " Reset complete");
    }
}
