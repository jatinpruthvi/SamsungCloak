package com.samsungcloak.coherence;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Inter-App Navigation Context Hook for Samsung Galaxy A12 (SM-A125U)
 *
 * Simulates "Referral Flow" where the agent transitions between apps through
 * deep-links, providing realistic "Incoming Intent" telemetry. This hook ensures
 * that app transitions exhibit human-like navigation patterns with appropriate
 * delays, context preservation, and behavioral cues.
 *
 * Key Features:
 * 1. Referral Flow simulation (Browser/Social Feed → Target App via deep-link)
 * 2. App transition latency modeling
 * 3. Context preservation across app boundaries
 * 4. Realistic navigation intent generation
 * 5. Multi-step referral chain simulation
 */
public class InterAppNavigationContextHook {

    private static final String LOG_TAG = "SamsungCloak.InterAppNavigationContext";
    private static boolean initialized = false;

    // Referral source types
    public enum ReferralSource {
        BROWSER_CHROME("com.android.chrome", "Chrome Browser", 0.35),
        BROWSER_FIREFOX("org.mozilla.firefox", "Firefox Browser", 0.12),
        BROWSER_SAMSUNG("com.sec.android.app.sbrowser", "Samsung Internet", 0.25),
        SOCIAL_FACEBOOK("com.facebook.katana", "Facebook", 0.18),
        SOCIAL_INSTAGRAM("com.instagram.android", "Instagram", 0.22),
        SOCIAL_TWITTER("com.twitter.android", "Twitter", 0.08),
        SOCIAL_LINKEDIN("com.linkedin.android", "LinkedIn", 0.06),
        EMAIL_GMAIL("com.google.android.gm", "Gmail", 0.15),
        EMAIL_OUTLOOK("com.microsoft.office.outlook", "Outlook", 0.07),
        MESSENGER_WHATSAPP("com.whatsapp", "WhatsApp", 0.14),
        MESSENGER_TELEGRAM("org.telegram.messenger", "Telegram", 0.04);

        private final String packageName;
        private final String displayName;
        private final double referralProbability;

        ReferralSource(String packageName, String displayName, double referralProbability) {
            this.packageName = packageName;
            this.displayName = displayName;
            this.referralProbability = referralProbability;
        }
    }

    // Navigation intent types
    public enum NavigationIntent {
        PRODUCT_VIEW,
        PURCHASE_FLOW,
        CONTENT_READING,
        VIDEO_PLAYBACK,
        ACCOUNT_MANAGEMENT,
        SEARCH_QUERY,
        PROFILE_VIEW,
        SOCIAL_SHARE,
        NOTIFICATION_TAP,
        HOME_SCREEN_TAP
    }

    // Transition latency constants (ms)
    private static final double APP_LAUNCH_LATENCY_BASE_MS = 800.0;
    private static final double APP_LAUNCH_LATENCY_VARiability = 400.0;
    private static final double DEEP_LINK_RESOLVE_LATENCY_MS = 150.0;
    private static final double CONTEXT_TRANSFER_LATENCY_MS = 50.0;

    // Human behavior constants
    private static final double BROWSE_BEFORE_TAP_PROBABILITY = 0.65;
    private static final double BROWSE_DURATION_BASE_MS = 2500.0;
    private static final double BROWSE_DURATION_VARiability = 3000.0;
    private static final double SCROLL_BEFORE_TAP_PROBABILITY = 0.78;
    private static final double SCROLL_DISTANCE_BASE_PX = 300.0;

    // Referral chain constants
    private static final double REFERRAL_CHAIN_PROBABILITY = 0.12;
    private static final int MAX_REFERRAL_CHAIN_LENGTH = 4;

    // Back navigation constants
    private static final double BACK_NAVIGATION_PROBABILITY = 0.23;
    private static final double BACK_TO_REFERRER_PROBABILITY = 0.68;

    private final Random random;
    private final Deque<AppTransition> transitionHistory;
    private final Map<String, AppSession> activeSessions;

    // Current state
    private String currentApp;
    private AppSession currentSession;
    private ReferralSource lastReferralSource;
    private NavigationIntent currentIntent;
    private long lastNavigationTimeMs;
    private int referralChainDepth;

    public InterAppNavigationContextHook() {
        this.random = new Random();
        this.transitionHistory = new ArrayDeque<>(20);
        this.activeSessions = new HashMap<>();

        this.currentApp = null;
        this.currentSession = null;
        this.lastReferralSource = null;
        this.currentIntent = NavigationIntent.HOME_SCREEN_TAP;
        this.lastNavigationTimeMs = System.currentTimeMillis();
        this.referralChainDepth = 0;
    }

    /**
     * Initialize the inter-app navigation context hook.
     */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
    }

    /**
     * Simulate a referral flow from a source app to a target app.
     *
     * @param targetApp target app package name
     * @param deepLinkUrl deep-link URL
     * @param source referral source (null for random selection)
     * @return ReferralFlowResult with transition details and telemetry
     */
    public ReferralFlowResult simulateReferralFlow(String targetApp, String deepLinkUrl, ReferralSource source) {
        long currentTimeMs = System.currentTimeMillis();

        // Select referral source if not provided
        if (source == null) {
            source = selectRandomReferralSource();
        }

        // Simulate browsing behavior in source app before tap
        ReferralBrowseBehavior browseBehavior = simulateReferralBrowseBehavior(source);

        // Calculate transition latency
        double transitionLatencyMs = calculateTransitionLatency(source, targetApp);

        // Generate navigation intent
        NavigationIntent intent = generateNavigationIntent(deepLinkUrl, source);

        // Simulate context transfer
        ContextTransfer contextTransfer = simulateContextTransfer(source, deepLinkUrl);

        // Create transition record
        AppTransition transition = new AppTransition(
            source.packageName,
            targetApp,
            deepLinkUrl,
            currentTimeMs,
            transitionLatencyMs,
            intent,
            contextTransfer
        );

        // Update state
        lastReferralSource = source;
        currentIntent = intent;
        currentApp = targetApp;
        lastNavigationTimeMs = currentTimeMs;

        // Record transition
        transitionHistory.addLast(transition);
        if (transitionHistory.size() > 20) {
            transitionHistory.removeFirst();
        }

        // Manage app sessions
        manageAppSessions(source, targetApp, transition);

        return new ReferralFlowResult(
            source,
            targetApp,
            deepLinkUrl,
            transitionLatencyMs,
            intent,
            browseBehavior,
            contextTransfer,
            currentTimeMs
        );
    }

    /**
     * Simulate a direct app launch (no referral source).
     *
     * @param targetApp target app package name
     * @return DirectLaunchResult with launch details
     */
    public DirectLaunchResult simulateDirectLaunch(String targetApp) {
        long currentTimeMs = System.currentTimeMillis();

        // Calculate launch latency (typically shorter than referral)
        double launchLatencyMs = APP_LAUNCH_LATENCY_BASE_MS * 0.7 +
                                random.nextDouble() * APP_LAUNCH_LATENCY_VARiability * 0.5;

        // Generate intent (usually HOME_SCREEN_TAP or NOTIFICATION_TAP)
        NavigationIntent intent = random.nextDouble() < 0.3 ?
            NavigationIntent.NOTIFICATION_TAP : NavigationIntent.HOME_SCREEN_TAP;

        // Update state
        currentApp = targetApp;
        currentIntent = intent;
        lastNavigationTimeMs = currentTimeMs;

        // Create transition record
        AppTransition transition = new AppTransition(
            "system_launcher",
            targetApp,
            null,
            currentTimeMs,
            launchLatencyMs,
            intent,
            new ContextTransfer(0, 0, null, null)
        );

        transitionHistory.addLast(transition);

        return new DirectLaunchResult(
            targetApp,
            launchLatencyMs,
            intent,
            currentTimeMs
        );
    }

    /**
     * Simulate back navigation behavior.
     *
     * @return BackNavigationResult with back navigation details
     */
    public BackNavigationResult simulateBackNavigation() {
        long currentTimeMs = System.currentTimeMillis();

        // Determine if user navigates back to referrer
        boolean backToReferrer = false;
        String destinationApp = null;

        if (lastReferralSource != null && random.nextDouble() < BACK_TO_REFERRER_PROBABILITY) {
            backToReferrer = true;
            destinationApp = lastReferralSource.packageName;
        } else {
            // Navigate to previous app in history or home screen
            if (transitionHistory.size() > 1) {
                destinationApp = transitionHistory.stream()
                    .skip(transitionHistory.size() - 2)
                    .findFirst()
                    .map(t -> t.sourceApp)
                    .orElse("system_launcher");
            } else {
                destinationApp = "system_launcher";
            }
        }

        // Calculate back navigation latency (typically very short)
        double backLatencyMs = 100.0 + random.nextDouble() * 150.0;

        // Update state
        currentApp = destinationApp;
        lastNavigationTimeMs = currentTimeMs;

        return new BackNavigationResult(
            destinationApp,
            backToReferrer,
            backLatencyMs,
            currentTimeMs
        );
    }

    /**
     * Select a random referral source based on probability weights.
     */
    private ReferralSource selectRandomReferralSource() {
        double totalWeight = 0.0;
        for (ReferralSource source : ReferralSource.values()) {
            totalWeight += source.referralProbability;
        }

        double randomWeight = random.nextDouble() * totalWeight;
        double currentWeight = 0.0;

        for (ReferralSource source : ReferralSource.values()) {
            currentWeight += source.referralProbability;
            if (randomWeight <= currentWeight) {
                return source;
            }
        }

        return ReferralSource.BROWSER_CHROME; // Fallback
    }

    /**
     * Simulate browsing behavior in referral source app.
     */
    private ReferralBrowseBehavior simulateReferralBrowseBehavior(ReferralSource source) {
        boolean hasBrowseBehavior = random.nextDouble() < BROWSE_BEFORE_TAP_PROBABILITY;

        if (!hasBrowseBehavior) {
            return new ReferralBrowseBehavior(false, 0, 0, false);
        }

        double browseDurationMs = BROWSE_DURATION_BASE_MS +
                                  random.nextDouble() * BROWSE_DURATION_VARiability;

        boolean hasScrolling = random.nextDouble() < SCROLL_BEFORE_TAP_PROBABILITY;
        double scrollDistancePx = 0.0;

        if (hasScrolling) {
            scrollDistancePx = SCROLL_DISTANCE_BASE_PX +
                              random.nextDouble() * SCROLL_DISTANCE_BASE_PX * 2.0;
        }

        return new ReferralBrowseBehavior(true, browseDurationMs, scrollDistancePx, hasScrolling);
    }

    /**
     * Calculate transition latency based on source and target apps.
     */
    private double calculateTransitionLatency(ReferralSource source, String targetApp) {
        double baseLatency = APP_LAUNCH_LATENCY_BASE_MS;
        double variability = APP_LAUNCH_LATENCY_VARiability;

        // Adjust based on source app
        switch (source) {
            case BROWSER_CHROME:
            case BROWSER_FIREFOX:
                baseLatency *= 0.85; // Browsers are optimized for deep-links
                break;
            case SOCIAL_FACEBOOK:
            case SOCIAL_INSTAGRAM:
                baseLatency *= 1.2; // Social apps may have additional processing
                break;
            default:
                break;
        }

        // Add deep-link resolution latency
        baseLatency += DEEP_LINK_RESOLVE_LATENCY_MS;

        // Add context transfer latency
        baseLatency += CONTEXT_TRANSFER_LATENCY_MS;

        // Add random variability
        return baseLatency + random.nextDouble() * variability;
    }

    /**
     * Generate navigation intent based on deep-link and source app.
     */
    private NavigationIntent generateNavigationIntent(String deepLinkUrl, ReferralSource source) {
        if (deepLinkUrl == null) {
            return NavigationIntent.HOME_SCREEN_TAP;
        }

        // Parse deep-link to infer intent
        String urlLower = deepLinkUrl.toLowerCase();

        if (urlLower.contains("product") || urlLower.contains("item") || urlLower.contains("buy")) {
            return NavigationIntent.PRODUCT_VIEW;
        } else if (urlLower.contains("checkout") || urlLower.contains("cart") || urlLower.contains("payment")) {
            return NavigationIntent.PURCHASE_FLOW;
        } else if (urlLower.contains("video") || urlLower.contains("watch") || urlLower.contains("play")) {
            return NavigationIntent.VIDEO_PLAYBACK;
        } else if (urlLower.contains("profile") || urlLower.contains("user") || urlLower.contains("account")) {
            return NavigationIntent.PROFILE_VIEW;
        } else if (urlLower.contains("search") || urlLower.contains("query")) {
            return NavigationIntent.SEARCH_QUERY;
        } else if (urlLower.contains("settings") || urlLower.contains("config")) {
            return NavigationIntent.ACCOUNT_MANAGEMENT;
        } else {
            // Default based on source
            switch (source) {
                case SOCIAL_FACEBOOK:
                case SOCIAL_INSTAGRAM:
                case SOCIAL_TWITTER:
                    return NavigationIntent.CONTENT_READING;
                case BROWSER_CHROME:
                case BROWSER_FIREFOX:
                case BROWSER_SAMSUNG:
                    return random.nextDouble() < 0.5 ?
                        NavigationIntent.PRODUCT_VIEW : NavigationIntent.CONTENT_READING;
                default:
                    return NavigationIntent.CONTENT_READING;
            }
        }
    }

    /**
     * Simulate context transfer between apps.
     */
    private ContextTransfer simulateContextTransfer(ReferralSource source, String deepLinkUrl) {
        // Determine how much context is preserved
        double contextPreservationScore = 0.7 + random.nextDouble() * 0.25;

        // Extract UTM parameters if present
        Map<String, String> utmParameters = extractUTMParameters(deepLinkUrl);

        // Generate context data
        int contextDataSize = (int) (contextPreservationScore * 100) + random.nextInt(50);
        int cookieTransferCount = (int) (contextPreservationScore * 5) + random.nextInt(3);

        return new ContextTransfer(
            contextDataSize,
            cookieTransferCount,
            utmParameters,
            deepLinkUrl
        );
    }

    /**
     * Extract UTM parameters from deep-link URL.
     */
    private Map<String, String> extractUTMParameters(String url) {
        Map<String, String> utmParams = new HashMap<>();

        if (url == null) {
            return utmParams;
        }

        String[] parts = url.split("[?&]");
        for (String part : parts) {
            if (part.startsWith("utm_")) {
                String[] keyValue = part.split("=");
                if (keyValue.length == 2) {
                    utmParams.put(keyValue[0], keyValue[1]);
                }
            }
        }

        return utmParams;
    }

    /**
     * Manage app sessions for context preservation.
     */
    private void manageAppSessions(ReferralSource source, String targetApp, AppTransition transition) {
        // Create or update source app session
        AppSession sourceSession = activeSessions.computeIfAbsent(
            source.packageName,
            k -> new AppSession(source.packageName, System.currentTimeMillis())
        );
        sourceSession.lastInteractionTime = transition.timestamp;
        sourceSession.interactionCount++;

        // Create or update target app session
        AppSession targetSession = activeSessions.computeIfAbsent(
            targetApp,
            k -> new AppSession(targetApp, System.currentTimeMillis())
        );
        targetSession.lastInteractionTime = transition.timestamp;
        targetSession.interactionCount++;
        targetSession.referralSource = source.displayName;

        // Clean up old sessions
        cleanupOldSessions();
    }

    /**
     * Clean up old app sessions.
     */
    private void cleanupOldSessions() {
        long currentTimeMs = System.currentTimeMillis();
        long sessionTimeoutMs = 5 * 60 * 1000; // 5 minutes

        activeSessions.entrySet().removeIf(entry ->
            currentTimeMs - entry.getValue().lastInteractionTime > sessionTimeoutMs
        );
    }

    /**
     * Get current navigation state.
     */
    public NavigationState getNavigationState() {
        return new NavigationState(
            currentApp,
            lastReferralSource,
            currentIntent,
            lastNavigationTimeMs,
            transitionHistory.size(),
            activeSessions.size()
        );
    }

    /**
     * Data class for referral browse behavior.
     */
    public static class ReferralBrowseBehavior {
        public final boolean hasBrowsing;
        public final double browseDurationMs;
        public final double scrollDistancePx;
        public final boolean hasScrolling;

        public ReferralBrowseBehavior(boolean hasBrowsing, double browseDurationMs,
                                     double scrollDistancePx, boolean hasScrolling) {
            this.hasBrowsing = hasBrowsing;
            this.browseDurationMs = browseDurationMs;
            this.scrollDistancePx = scrollDistancePx;
            this.hasScrolling = hasScrolling;
        }
    }

    /**
     * Data class for context transfer.
     */
    public static class ContextTransfer {
        public final int contextDataSize;
        public final int cookieTransferCount;
        public final Map<String, String> utmParameters;
        public final String deepLinkUrl;

        public ContextTransfer(int contextDataSize, int cookieTransferCount,
                             Map<String, String> utmParameters, String deepLinkUrl) {
            this.contextDataSize = contextDataSize;
            this.cookieTransferCount = cookieTransferCount;
            this.utmParameters = utmParameters;
            this.deepLinkUrl = deepLinkUrl;
        }
    }

    /**
     * Data class for app transition.
     */
    private static class AppTransition {
        public final String sourceApp;
        public final String targetApp;
        public final String deepLinkUrl;
        public final long timestamp;
        public final double transitionLatencyMs;
        public final NavigationIntent intent;
        public final ContextTransfer contextTransfer;

        public AppTransition(String sourceApp, String targetApp, String deepLinkUrl,
                           long timestamp, double transitionLatencyMs, NavigationIntent intent,
                           ContextTransfer contextTransfer) {
            this.sourceApp = sourceApp;
            this.targetApp = targetApp;
            this.deepLinkUrl = deepLinkUrl;
            this.timestamp = timestamp;
            this.transitionLatencyMs = transitionLatencyMs;
            this.intent = intent;
            this.contextTransfer = contextTransfer;
        }
    }

    /**
     * Data class for app session.
     */
    private static class AppSession {
        public final String packageName;
        public long sessionStartTime;
        public long lastInteractionTime;
        public int interactionCount;
        public String referralSource;

        public AppSession(String packageName, long sessionStartTime) {
            this.packageName = packageName;
            this.sessionStartTime = sessionStartTime;
            this.lastInteractionTime = sessionStartTime;
            this.interactionCount = 0;
            this.referralSource = null;
        }
    }

    /**
     * Data class for referral flow result.
     */
    public static class ReferralFlowResult {
        public final ReferralSource source;
        public final String targetApp;
        public final String deepLinkUrl;
        public final double transitionLatencyMs;
        public final NavigationIntent intent;
        public final ReferralBrowseBehavior browseBehavior;
        public final ContextTransfer contextTransfer;
        public final long timestamp;

        public ReferralFlowResult(ReferralSource source, String targetApp, String deepLinkUrl,
                                 double transitionLatencyMs, NavigationIntent intent,
                                 ReferralBrowseBehavior browseBehavior, ContextTransfer contextTransfer,
                                 long timestamp) {
            this.source = source;
            this.targetApp = targetApp;
            this.deepLinkUrl = deepLinkUrl;
            this.transitionLatencyMs = transitionLatencyMs;
            this.intent = intent;
            this.browseBehavior = browseBehavior;
            this.contextTransfer = contextTransfer;
            this.timestamp = timestamp;
        }
    }

    /**
     * Data class for direct launch result.
     */
    public static class DirectLaunchResult {
        public final String targetApp;
        public final double launchLatencyMs;
        public final NavigationIntent intent;
        public final long timestamp;

        public DirectLaunchResult(String targetApp, double launchLatencyMs,
                                 NavigationIntent intent, long timestamp) {
            this.targetApp = targetApp;
            this.launchLatencyMs = launchLatencyMs;
            this.intent = intent;
            this.timestamp = timestamp;
        }
    }

    /**
     * Data class for back navigation result.
     */
    public static class BackNavigationResult {
        public final String destinationApp;
        public final boolean backToReferrer;
        public final double backLatencyMs;
        public final long timestamp;

        public BackNavigationResult(String destinationApp, boolean backToReferrer,
                                  double backLatencyMs, long timestamp) {
            this.destinationApp = destinationApp;
            this.backToReferrer = backToReferrer;
            this.backLatencyMs = backLatencyMs;
            this.timestamp = timestamp;
        }
    }

    /**
     * Data class for navigation state.
     */
    public static class NavigationState {
        public final String currentApp;
        public final ReferralSource lastReferralSource;
        public final NavigationIntent currentIntent;
        public final long lastNavigationTimeMs;
        public final int transitionHistorySize;
        public final int activeSessionCount;

        public NavigationState(String currentApp, ReferralSource lastReferralSource,
                             NavigationIntent currentIntent, long lastNavigationTimeMs,
                             int transitionHistorySize, int activeSessionCount) {
            this.currentApp = currentApp;
            this.lastReferralSource = lastReferralSource;
            this.currentIntent = currentIntent;
            this.lastNavigationTimeMs = lastNavigationTimeMs;
            this.transitionHistorySize = transitionHistorySize;
            this.activeSessionCount = activeSessionCount;
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
