package com.samsungcloak.coherence;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

/**
 * Asymmetric Latency Hook for Samsung Galaxy A12 (SM-A125U)
 *
 * Simulates "Processing Hesitation" where the agent pauses specifically after
 * UI-Load events to simulate the human "Perceptual Gap" - the time it takes
 * for a brain to process new information on screen.
 *
 * Key Features:
 * 1. Perceptual Gap simulation after UI transitions
 * 2. Asymmetric latency between network and UI operations
 * 3. Context-dependent processing delays
 * 4. Information density-based hesitation
 * 5. Adaptive latency based on task complexity
 */
public class AsymmetricLatencyHook {

    private static final String LOG_TAG = "SamsungCloak.AsymmetricLatency";
    private static boolean initialized = false;

    // Perceptual Gap constants (human cognitive processing time)
    private static final double BASE_PERCEPTUAL_GAP_MS = 180.0;
    private static final double PERCEPTUAL_GAP_VARiability = 120.0;
    private static final double MAX_PERCEPTUAL_GAP_MS = 800.0;

    // UI load event types
    public enum UILoadEventType {
        SCREEN_TRANSITION("Screen Transition", 1.0),
        CONTENT_LOAD("Content Load", 1.2),
        IMAGE_LOAD("Image Load", 0.8),
        DIALOG_OPEN("Dialog Open", 0.9),
        MENU_OPEN("Menu Open", 0.7),
        TAB_SWITCH("Tab Switch", 0.85),
        FORM_SUBMIT("Form Submit", 1.4),
        VIDEO_START("Video Start", 0.6),
        LIST_RENDER("List Render", 1.1),
        UNKNOWN("Unknown", 1.0);

        private final String displayName;
        private final double perceptualGapMultiplier;

        UILoadEventType(String displayName, double perceptualGapMultiplier) {
            this.displayName = displayName;
            this.perceptualGapMultiplier = perceptualGapMultiplier;
        }
    }

    // Information density levels
    public enum InformationDensity {
        VERY_LOW("Very Low", 0.6, 50),
        LOW("Low", 0.8, 100),
        MEDIUM("Medium", 1.0, 200),
        HIGH("High", 1.3, 400),
        VERY_HIGH("Very High", 1.7, 800);

        private final String displayName;
        private final double hesitationMultiplier;
        private final double elementCountThreshold;

        InformationDensity(String displayName, double hesitationMultiplier, double elementCountThreshold) {
            this.displayName = displayName;
            this.hesitationMultiplier = hesitationMultiplier;
            this.elementCountThreshold = elementCountThreshold;
        }
    }

    // Network vs UI latency asymmetry
    private static final double NETWORK_LATENCY_BASE_MS = 50.0;
    private static final double NETWORK_LATENCY_VARiability = 100.0;
    private static final double UI_LATENCY_BASE_MS = 16.0;
    private static final double UI_LATENCY_VARiability = 30.0;

    // Task complexity factors
    private static final double TASK_COMPLEXITY_SIMPLE_MULTIPLIER = 0.7;
    private static final double TASK_COMPLEXITY_MODERATE_MULTIPLIER = 1.0;
    private static final double TASK_COMPLEXITY_COMPLEX_MULTIPLIER = 1.5;

    // User state factors
    private static final double FRESH_USER_MULTIPLIER = 0.85;
    private static final double FOCUSED_USER_MULTIPLIER = 1.0;
    private static final double DISTRACTED_USER_MULTIPLIER = 1.4;
    private static final double FATIGUED_USER_MULTIPLIER = 1.6;

    // Adaptation factors
    private static final double ADAPTATION_RATE = 0.05; // Rate at which user adapts to patterns
    private static final double MIN_ADAPTATION_MULTIPLIER = 0.7;

    private final Random random;
    private final Deque<LatencyEvent> latencyHistory;

    // Current state
    private double adaptationFactor;
    private int interactionCount;
    private long lastLoadEventTimeMs;
    private UserState currentUserState;
    private InformationDensity currentInfoDensity;

    public enum UserState {
        FRESH,
        FOCUSED,
        DISTRACTED,
        FATIGUED
    }

    public AsymmetricLatencyHook() {
        this.random = new Random();
        this.latencyHistory = new ArrayDeque<>(50);

        this.adaptationFactor = 1.0;
        this.interactionCount = 0;
        this.lastLoadEventTimeMs = System.currentTimeMillis();
        this.currentUserState = UserState.FOCUSED;
        this.currentInfoDensity = InformationDensity.MEDIUM;
    }

    /**
     * Initialize the asymmetric latency hook.
     */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
    }

    /**
     * Simulate processing hesitation after a UI load event.
     *
     * @param eventType type of UI load event
     * @param informationDensity information density of loaded content
     * @return PerceptualGapResult with hesitation duration and related data
     */
    public PerceptualGapResult simulatePerceptualGap(UILoadEventType eventType,
                                                    InformationDensity informationDensity) {
        long currentTimeMs = System.currentTimeMillis();
        this.currentInfoDensity = informationDensity;

        // Calculate base perceptual gap
        double baseGap = BASE_PERCEPTUAL_GAP_MS * eventType.perceptualGapMultiplier;

        // Apply information density factor
        baseGap *= informationDensity.hesitationMultiplier;

        // Apply user state factor
        baseGap *= getUserStateMultiplier();

        // Add variability
        double gapVariability = random.nextDouble() * PERCEPTUAL_GAP_VARiability;
        double perceptualGapMs = baseGap + gapVariability;

        // Apply adaptation (user gets faster with repeated patterns)
        perceptualGapMs *= adaptationFactor;

        // Clamp to valid range
        perceptualGapMs = Math.max(100.0, Math.min(MAX_PERCEPTUAL_GAP_MS, perceptualGapMs));

        // Calculate network vs UI latency asymmetry
        LatencyAsymmetry asymmetry = calculateLatencyAsymmetry(eventType);

        // Update adaptation factor
        updateAdaptationFactor(eventType);

        // Record latency event
        LatencyEvent event = new LatencyEvent(
            eventType,
            informationDensity,
            perceptualGapMs,
            asymmetry.networkLatency,
            asymmetry.uiLatency,
            currentTimeMs
        );
        latencyHistory.addLast(event);
        if (latencyHistory.size() > 50) {
            latencyHistory.removeFirst();
        }

        // Update state
        lastLoadEventTimeMs = currentTimeMs;
        interactionCount++;

        return new PerceptualGapResult(
            eventType,
            informationDensity,
            perceptualGapMs,
            asymmetry,
            currentUserState,
            adaptationFactor,
            currentTimeMs
        );
    }

    /**
     * Simulate network latency with realistic jitter and packet loss simulation.
     *
     * @param requestDataSize size of request data in bytes
     * @param isWiFiConnection whether connection is WiFi (vs. cellular)
     * @return NetworkLatencyResult with latency details
     */
    public NetworkLatencyResult simulateNetworkLatency(long requestDataSize, boolean isWiFiConnection) {
        long currentTimeMs = System.currentTimeMillis();

        // Calculate base latency based on connection type
        double baseLatency = isWiFiConnection ? NETWORK_LATENCY_BASE_MS * 0.6 : NETWORK_LATENCY_BASE_MS;

        // Adjust for data size (larger requests take longer)
        double dataSizeFactor = 1.0 + (requestDataSize / 10000.0) * 0.1;

        // Add jitter
        double jitter = generateNetworkJitter(isWiFiConnection);

        // Add congestion simulation
        double congestionDelay = simulateCongestionDelay(isWiFiConnection);

        // Calculate total network latency
        double networkLatencyMs = (baseLatency * dataSizeFactor) + jitter + congestionDelay;

        // Simulate packet loss (retransmission adds latency)
        boolean packetLossOccurred = simulatePacketLoss(isWiFiConnection);
        if (packetLossOccurred) {
            networkLatencyMs += 200.0 + random.nextDouble() * 300.0;
        }

        return new NetworkLatencyResult(
            networkLatencyMs,
            jitter,
            packetLossOccurred,
            congestionDelay,
            isWiFiConnection,
            currentTimeMs
        );
    }

    /**
     * Simulate UI latency (frame rendering and touch processing).
     *
     * @param hasAnimations whether the UI has active animations
     * @param isHeavyRender whether the render operation is computationally heavy
     * @return UILatencyResult with latency details
     */
    public UILatencyResult simulateUILatency(boolean hasAnimations, boolean isHeavyRender) {
        long currentTimeMs = System.currentTimeMillis();

        // Calculate base UI latency (16ms = 60fps)
        double baseLatency = UI_LATENCY_BASE_MS;

        // Add animation overhead
        if (hasAnimations) {
            baseLatency += 8.0 + random.nextDouble() * 12.0;
        }

        // Add heavy render overhead
        if (isHeavyRender) {
            baseLatency += 15.0 + random.nextDouble() * 25.0;
        }

        // Add variability
        double variability = random.nextDouble() * UI_LATENCY_VARiability;
        double uiLatencyMs = baseLatency + variability;

        // Calculate frame rate
        double frameRate = 1000.0 / Math.max(16.0, uiLatencyMs);

        return new UILatencyResult(
            uiLatencyMs,
            frameRate,
            hasAnimations,
            isHeavyRender,
            currentTimeMs
        );
    }

    /**
     * Calculate latency asymmetry between network and UI operations.
     */
    private LatencyAsymmetry calculateLatencyAsymmetry(UILoadEventType eventType) {
        // Network latency (varies more, affected by external factors)
        double networkLatencyMs = NETWORK_LATENCY_BASE_MS +
                                random.nextDouble() * NETWORK_LATENCY_VARiability;

        // Adjust network latency based on event type
        switch (eventType) {
            case CONTENT_LOAD:
            case IMAGE_LOAD:
            case VIDEO_START:
                networkLatencyMs *= 1.5;
                break;
            case SCREEN_TRANSITION:
            case TAB_SWITCH:
                networkLatencyMs *= 0.8;
                break;
            default:
                break;
        }

        // UI latency (more consistent, mainly affected by rendering)
        double uiLatencyMs = UI_LATENCY_BASE_MS +
                             random.nextDouble() * UI_LATENCY_VARiability;

        // Adjust UI latency based on event type
        switch (eventType) {
            case LIST_RENDER:
            case CONTENT_LOAD:
                uiLatencyMs *= 1.3;
                break;
            case SCREEN_TRANSITION:
                uiLatencyMs *= 1.5;
                break;
            default:
                break;
        }

        // Calculate asymmetry ratio
        double asymmetryRatio = networkLatencyMs / Math.max(1.0, uiLatencyMs);

        return new LatencyAsymmetry(networkLatencyMs, uiLatencyMs, asymmetryRatio);
    }

    /**
     * Generate network jitter.
     */
    private double generateNetworkJitter(boolean isWiFiConnection) {
        if (isWiFiConnection) {
            // WiFi has less jitter
            return (random.nextGaussian() * 15.0);
        } else {
            // Cellular has more jitter
            return Math.abs(random.nextGaussian() * 40.0);
        }
    }

    /**
     * Simulate network congestion delay.
     */
    private double simulateCongestionDelay(boolean isWiFiConnection) {
        double congestionProbability = isWiFiConnection ? 0.08 : 0.15;

        if (random.nextDouble() < congestionProbability) {
            return 50.0 + random.nextDouble() * 200.0;
        }

        return 0.0;
    }

    /**
     * Simulate packet loss.
     */
    private boolean simulatePacketLoss(boolean isWiFiConnection) {
        double packetLossProbability = isWiFiConnection ? 0.005 : 0.02;
        return random.nextDouble() < packetLossProbability;
    }

    /**
     * Update adaptation factor based on repeated patterns.
     */
    private void updateAdaptationFactor(UILoadEventType eventType) {
        // Check if we've seen this event type recently
        long recentCount = latencyHistory.stream()
            .filter(e -> e.eventType == eventType)
            .filter(e -> System.currentTimeMillis() - e.timestamp < 30000) // Within 30 seconds
            .count();

        if (recentCount > 3) {
            // User is adapting to this pattern
            adaptationFactor -= ADAPTATION_RATE;
        }

        // Clamp adaptation factor
        adaptationFactor = Math.max(MIN_ADAPTATION_MULTIPLIER, Math.min(1.0, adaptationFactor));
    }

    /**
     * Get user state multiplier.
     */
    private double getUserStateMultiplier() {
        switch (currentUserState) {
            case FRESH:
                return FRESH_USER_MULTIPLIER;
            case FOCUSED:
                return FOCUSED_USER_MULTIPLIER;
            case DISTRACTED:
                return DISTRACTED_USER_MULTIPLIER;
            case FATIGUED:
                return FATIGUED_USER_MULTIPLIER;
            default:
                return 1.0;
        }
    }

    /**
     * Update user state based on session duration and interaction patterns.
     */
    public void updateUserState(long sessionDurationMs, int consecutiveErrors) {
        // Update user state based on session duration
        if (sessionDurationMs < 5 * 60 * 1000) { // Less than 5 minutes
            currentUserState = UserState.FRESH;
        } else if (sessionDurationMs < 30 * 60 * 1000) { // Less than 30 minutes
            currentUserState = UserState.FOCUSED;
        } else if (sessionDurationMs < 60 * 60 * 1000) { // Less than 1 hour
            currentUserState = UserState.DISTRACTED;
        } else {
            currentUserState = UserState.FATIGUED;
        }

        // Adjust state based on consecutive errors
        if (consecutiveErrors > 3) {
            currentUserState = UserState.DISTRACTED;
        }
    }

    /**
     * Determine information density based on UI element count.
     */
    public InformationDensity determineInformationDensity(int elementCount) {
        if (elementCount < 50) {
            return InformationDensity.VERY_LOW;
        } else if (elementCount < 100) {
            return InformationDensity.LOW;
        } else if (elementCount < 200) {
            return InformationDensity.MEDIUM;
        } else if (elementCount < 400) {
            return InformationDensity.HIGH;
        } else {
            return InformationDensity.VERY_HIGH;
        }
    }

    /**
     * Calculate task complexity based on interaction depth and requirements.
     */
    public double calculateTaskComplexity(int interactionDepth, boolean requiresAuthentication,
                                         boolean hasMultiStepFlow) {
        double complexity = 1.0;

        // Adjust for interaction depth
        complexity *= (1.0 + interactionDepth * 0.2);

        // Adjust for authentication requirement
        if (requiresAuthentication) {
            complexity *= 1.3;
        }

        // Adjust for multi-step flow
        if (hasMultiStepFlow) {
            complexity *= 1.4;
        }

        // Apply complexity multiplier
        if (complexity < 1.5) {
            return TASK_COMPLEXITY_SIMPLE_MULTIPLIER;
        } else if (complexity < 2.5) {
            return TASK_COMPLEXITY_MODERATE_MULTIPLIER;
        } else {
            return TASK_COMPLEXITY_COMPLEX_MULTIPLIER;
        }
    }

    /**
     * Get latency statistics from history.
     */
    public LatencyStatistics getLatencyStatistics() {
        if (latencyHistory.isEmpty()) {
            return new LatencyStatistics(0, 0, 0, 0, 0, 0);
        }

        double avgPerceptualGap = latencyHistory.stream()
            .mapToDouble(e -> e.perceptualGapMs)
            .average()
            .orElse(0.0);

        double avgNetworkLatency = latencyHistory.stream()
            .mapToDouble(e -> e.networkLatency)
            .average()
            .orElse(0.0);

        double avgUILatency = latencyHistory.stream()
            .mapToDouble(e -> e.uiLatency)
            .average()
            .orElse(0.0);

        double maxPerceptualGap = latencyHistory.stream()
            .mapToDouble(e -> e.perceptualGapMs)
            .max()
            .orElse(0.0);

        double minPerceptualGap = latencyHistory.stream()
            .mapToDouble(e -> e.perceptualGapMs)
            .min()
            .orElse(0.0);

        double avgAsymmetryRatio = latencyHistory.stream()
            .mapToDouble(e -> e.networkLatency / Math.max(1.0, e.uiLatency))
            .average()
            .orElse(0.0);

        return new LatencyStatistics(
            avgPerceptualGap,
            maxPerceptualGap,
            minPerceptualGap,
            avgNetworkLatency,
            avgUILatency,
            avgAsymmetryRatio
        );
    }

    /**
     * Reset adaptation factor.
     */
    public void resetAdaptation() {
        adaptationFactor = 1.0;
        interactionCount = 0;
    }

    /**
     * Data class for latency asymmetry.
     */
    public static class LatencyAsymmetry {
        public final double networkLatency;
        public final double uiLatency;
        public final double asymmetryRatio;

        public LatencyAsymmetry(double networkLatency, double uiLatency, double asymmetryRatio) {
            this.networkLatency = networkLatency;
            this.uiLatency = uiLatency;
            this.asymmetryRatio = asymmetryRatio;
        }
    }

    /**
     * Data class for latency event.
     */
    private static class LatencyEvent {
        public final UILoadEventType eventType;
        public final InformationDensity informationDensity;
        public final double perceptualGapMs;
        public final double networkLatency;
        public final double uiLatency;
        public final long timestamp;

        public LatencyEvent(UILoadEventType eventType, InformationDensity informationDensity,
                          double perceptualGapMs, double networkLatency, double uiLatency,
                          long timestamp) {
            this.eventType = eventType;
            this.informationDensity = informationDensity;
            this.perceptualGapMs = perceptualGapMs;
            this.networkLatency = networkLatency;
            this.uiLatency = uiLatency;
            this.timestamp = timestamp;
        }
    }

    /**
     * Data class for perceptual gap result.
     */
    public static class PerceptualGapResult {
        public final UILoadEventType eventType;
        public final InformationDensity informationDensity;
        public final double perceptualGapMs;
        public final LatencyAsymmetry asymmetry;
        public final UserState userState;
        public final double adaptationFactor;
        public final long timestamp;

        public PerceptualGapResult(UILoadEventType eventType, InformationDensity informationDensity,
                                 double perceptualGapMs, LatencyAsymmetry asymmetry,
                                 UserState userState, double adaptationFactor, long timestamp) {
            this.eventType = eventType;
            this.informationDensity = informationDensity;
            this.perceptualGapMs = perceptualGapMs;
            this.asymmetry = asymmetry;
            this.userState = userState;
            this.adaptationFactor = adaptationFactor;
            this.timestamp = timestamp;
        }
    }

    /**
     * Data class for network latency result.
     */
    public static class NetworkLatencyResult {
        public final double networkLatencyMs;
        public final double jitter;
        public final boolean packetLossOccurred;
        public final double congestionDelay;
        public final boolean isWiFiConnection;
        public final long timestamp;

        public NetworkLatencyResult(double networkLatencyMs, double jitter, boolean packetLossOccurred,
                                   double congestionDelay, boolean isWiFiConnection, long timestamp) {
            this.networkLatencyMs = networkLatencyMs;
            this.jitter = jitter;
            this.packetLossOccurred = packetLossOccurred;
            this.congestionDelay = congestionDelay;
            this.isWiFiConnection = isWiFiConnection;
            this.timestamp = timestamp;
        }
    }

    /**
     * Data class for UI latency result.
     */
    public static class UILatencyResult {
        public final double uiLatencyMs;
        public final double frameRate;
        public final boolean hasAnimations;
        public final boolean isHeavyRender;
        public final long timestamp;

        public UILatencyResult(double uiLatencyMs, double frameRate, boolean hasAnimations,
                             boolean isHeavyRender, long timestamp) {
            this.uiLatencyMs = uiLatencyMs;
            this.frameRate = frameRate;
            this.hasAnimations = hasAnimations;
            this.isHeavyRender = isHeavyRender;
            this.timestamp = timestamp;
        }
    }

    /**
     * Data class for latency statistics.
     */
    public static class LatencyStatistics {
        public final double avgPerceptualGapMs;
        public final double maxPerceptualGapMs;
        public final double minPerceptualGapMs;
        public final double avgNetworkLatencyMs;
        public final double avgUILatencyMs;
        public final double avgAsymmetryRatio;

        public LatencyStatistics(double avgPerceptualGapMs, double maxPerceptualGapMs,
                                double minPerceptualGapMs, double avgNetworkLatencyMs,
                                double avgUILatencyMs, double avgAsymmetryRatio) {
            this.avgPerceptualGapMs = avgPerceptualGapMs;
            this.maxPerceptualGapMs = maxPerceptualGapMs;
            this.minPerceptualGapMs = minPerceptualGapMs;
            this.avgNetworkLatencyMs = avgNetworkLatencyMs;
            this.avgUILatencyMs = avgUILatencyMs;
            this.avgAsymmetryRatio = avgAsymmetryRatio;
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
