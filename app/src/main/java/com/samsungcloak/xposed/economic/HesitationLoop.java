package com.samsungcloak.xposed.economic;

import java.util.Random;

/**
 * Hesitation Loops - Risk Aversion & Decision Friction
 * 
 * Simulates cognitive friction before high-value transactions.
 * Models the hesitation humans experience when spending significant amounts.
 * 
 * Real-world behavior:
 * - Users pause before large purchases
 * - Hesitation increases with transaction value
 * - Repeated consideration attempts
 * - May abandon cart during hesitation
 * - Price anchoring affects decision time
 * 
 * Tests: Checkout friction, cart abandonment rates, payment retry logic,
 *        session timeout handling, user patience simulation
 */
public class HesitationLoop {
    
    private final EconomicConfig config;
    private final Random random;
    
    // State tracking per user
    private int currentHesitationAttempts;
    private double currentHesitationDelay;
    private boolean inHesitation;
    private long hesitationStartTime;
    private double lastConsideredAmount;
    
    public HesitationLoop(EconomicConfig config) {
        this.config = config;
        this.random = config.getRandom();
        reset();
    }
    
    /**
     * Check if hesitation should be triggered for a transaction
     * 
     * @param amount Transaction amount
     * @return true if user will experience hesitation
     */
    public boolean shouldHesitate(double amount) {
        lastConsideredAmount = amount;
        
        // Amount below threshold - no hesitation
        if (amount < config.getHesitationThreshold()) {
            return false;
        }
        
        // Calculate hesitation probability based on amount relative to threshold
        double amountRatio = amount / config.getHesitationThreshold();
        double hesitationProbability = Math.min(0.95, 0.3 + (amountRatio * 0.1));
        
        // Increase probability with repeated consideration of same amount
        hesitationProbability *= (1.0 + (currentHesitationAttempts * 0.1));
        
        boolean shouldHesitate = random.nextDouble() < hesitationProbability;
        
        if (shouldHesitate) {
            inHesitation = true;
            hesitationStartTime = System.currentTimeMillis();
        }
        
        return shouldHesitate;
    }
    
    /**
     * Calculate the delay to apply before transaction can proceed
     * Models the cognitive processing time for decision making
     * 
     * @param amount Transaction amount
     * @return Delay in milliseconds
     */
    public int calculateHesitationDelay(double amount) {
        if (!inHesitation) {
            return 0;
        }
        
        // Base delay scales with amount
        double amountRatio = amount / config.getHesitationThreshold();
        int baseDelay = config.getMinHesitationMs() + 
            (int)((config.getMaxHesitationMs() - config.getMinHesitationMs()) * 
                Math.min(1.0, amountRatio * 0.5));
        
        // Add random variation
        int randomVariation = (int)((config.getMaxHesitationMs() - baseDelay) * random.nextDouble());
        
        // Increase with previous attempts
        int attemptMultiplier = 1 + currentHesitationAttempts;
        
        currentHesitationDelay = (baseDelay + randomVariation) * attemptMultiplier;
        
        return (int)currentHesitationDelay;
    }
    
    /**
     * Simulate completing the hesitation process
     * May result in proceeding or abandoning
     * 
     * @return HesitationResult indicating outcome
     */
    public HesitationResult completeHesitation() {
        if (!inHesitation) {
            return new HesitationResult(HesitationOutcome.PROCEED, 0, 
                "No hesitation in progress");
        }
        
        long hesitationDuration = System.currentTimeMillis() - hesitationStartTime;
        
        // Calculate probability of proceeding based on duration and amount
        double proceedProbability = calculateProceedProbability(hesitationDuration);
        
        boolean proceeds = random.nextDouble() < proceedProbability;
        
        HesitationOutcome outcome;
        int retryDelay = 0;
        
        if (proceeds) {
            outcome = HesitationOutcome.PROCEED;
            reset();
        } else if (hesitationDuration < 5000) {
            // Still considering - allow retry
            outcome = HesitationOutcome.RETRY;
            currentHesitationAttempts++;
            retryDelay = (int)(calculateHesitationDelay(lastConsideredAmount) * 0.5);
        } else {
            // Give up - cart abandonment
            outcome = HesitationOutcome.ABANDON_CART;
            reset();
        }
        
        return new HesitationResult(outcome, retryDelay,
            String.format("Hesitation duration: %dms, proceed probability: %.2f", 
                hesitationDuration, proceedProbability));
    }
    
    /**
     * Abort hesitation without completing
     */
    public void abortHesitation() {
        inHesitation = false;
        currentHesitationAttempts++;
    }
    
    /**
     * Force proceed after max hesitation attempts
     */
    public boolean forceProceedIfMaxAttempts() {
        if (currentHesitationAttempts >= 3) {
            reset();
            return true;
        }
        return false;
    }
    
    /**
     * Check if currently in hesitation state
     */
    public boolean isInHesitation() {
        return inHesitation;
    }
    
    /**
     * Get current hesitation attempt count
     */
    public int getHesitationAttempts() {
        return currentHesitationAttempts;
    }
    
    /**
     * Reset hesitation state
     */
    public void reset() {
        currentHesitationAttempts = 0;
        currentHesitationDelay = 0;
        inHesitation = false;
        hesitationStartTime = 0;
    }
    
    /**
     * Calculate probability of proceeding based on hesitation duration
     */
    private double calculateProceedProbability(long durationMs) {
        // Base probability
        double probability = 0.7;
        
        // Increase with duration (diminishing returns after 3 seconds)
        if (durationMs > 1000) {
            probability += Math.min(0.2, (durationMs - 1000) / 20000.0);
        }
        
        // Decrease with more attempts
        probability -= (currentHesitationAttempts * 0.15);
        
        // Factor in amount (larger amounts = lower proceed probability)
        if (lastConsideredAmount > 0) {
            double amountPenalty = Math.min(0.2, (lastConsideredAmount / 200.0) * 0.1);
            probability -= amountPenalty;
        }
        
        return Math.max(0.1, Math.min(0.95, probability));
    }
    
    /**
     * Simulate price comparison behavior during hesitation
     * Users often check competitor prices
     * 
     * @return true if user performs price comparison
     */
    public boolean performsPriceComparison() {
        double comparisonProbability = 0.4;
        
        // Higher for larger amounts
        if (lastConsideredAmount > config.getHesitationThreshold() * 2) {
            comparisonProbability = 0.7;
        }
        
        return random.nextDouble() < comparisonProbability;
    }
    
    /**
     * Get the maximum amount user will tolerate for instant checkout
     * Above this, hesitation is very likely
     * 
     * @return Threshold amount
     */
    public double getImpulsePurchaseThreshold() {
        // Amount below which user typically doesn't hesitate
        return config.getHesitationThreshold() * 0.5;
    }
    
    public enum HesitationOutcome {
        PROCEED,
        RETRY,
        ABANDON_CART
    }
    
    public static class HesitationResult {
        private final HesitationOutcome outcome;
        private final int retryDelay;
        private final String description;
        
        public HesitationResult(HesitationOutcome outcome, int retryDelay, String description) {
            this.outcome = outcome;
            this.retryDelay = retryDelay;
            this.description = description;
        }
        
        public HesitationOutcome getOutcome() { return outcome; }
        public int getRetryDelay() { return retryDelay; }
        public String getDescription() { return description; }
        
        public boolean proceeds() {
            return outcome == HesitationOutcome.PROCEED;
        }
        
        public boolean shouldRetry() {
            return outcome == HesitationOutcome.RETRY;
        }
        
        public boolean isAbandoned() {
            return outcome == HesitationOutcome.ABANDON_CART;
        }
    }
}
