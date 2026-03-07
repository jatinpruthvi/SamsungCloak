package com.samsungcloak.xposed.economic;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Impulse Buying Model - Stochastic Purchase Triggers
 * 
 * Simulates unplanned high-velocity purchases triggered by "Limited Time Offer" 
 * and other urgency signals in the UI.
 * 
 * Real-world behavior:
 * - Users make unplanned purchases when presented with urgency
 * - "Limited time" and "low stock" cues trigger impulse behavior
 * - Impulse buying follows spending patterns, not mathematical optimization
 * - Social proof increases impulse probability
 * - Time of day affects impulse susceptibility
 * 
 * Tests: Flash sale handling, high-velocity transaction bursts, 
 *        urgency UI triggers, fraud detection under load
 */
public class ImpulseBuyingModel {
    
    private final EconomicConfig config;
    private final Random random;
    
    // Per-user impulse tracking
    private final Map<String, UserImpulseState> userStates;
    private final Map<String, LimitedTimeOffer> activeOffers;
    
    // Global impulse tracking for anomaly detection
    private static volatile long lastImpulseBurst;
    private static volatile int burstCount;
    
    public ImpulseBuyingModel(EconomicConfig config) {
        this.config = config;
        this.random = config.getRandom();
        this.userStates = new ConcurrentHashMap<>();
        this.activeOffers = new ConcurrentHashMap<>();
    }
    
    /**
     * Check if a limited time offer should trigger impulse purchase
     * 
     * @param userId User identifier
     * @param offer The limited time offer
     * @param basePrice Original price
     * @return ImpulseResult
     */
    public ImpulseResult evaluateImpulsePurchase(String userId, LimitedTimeOffer offer, 
                                                   double basePrice) {
        if (offer == null || !offer.isActive()) {
            return new ImpulseResult(false, ImpulseTrigger.NONE, 0, "No active offer");
        }
        
        UserImpulseState state = userStates.computeIfAbsent(userId, k -> new UserImpulseState());
        
        // Check cooldown period
        if (state.isInCooldown()) {
            return new ImpulseResult(false, ImpulseTrigger.COOLDOWN, 0, 
                "User in impulse cooldown");
        }
        
        // Calculate impulse probability
        double impulseProbability = calculateImpulseProbability(state, offer, basePrice);
        
        boolean triggersImpulse = random.nextDouble() < impulseProbability;
        
        if (triggersImpulse) {
            // Calculate impulse purchase amount (can exceed normal limits)
            double impulseAmount = calculateImpulseAmount(basePrice, offer);
            
            // Record impulse event
            state.recordImpulsePurchase(impulseAmount);
            
            // Update global burst tracking
            updateBurstTracking();
            
            return new ImpulseResult(true, ImpulseTrigger.LIMITED_TIME_OFFER, 
                impulseAmount,
                String.format("Impulse triggered: %.0f%% discount, probability: %.2f", 
                    offer.getDiscountPercent(), impulseProbability));
        }
        
        return new ImpulseResult(false, ImpulseTrigger.NONE, 0, 
            String.format("No impulse: probability %.2f", impulseProbability));
    }
    
    /**
     * Create a new limited time offer
     * 
     * @param offerId Offer identifier
     * @param discountPercent Discount percentage
     * @param timeRemainingMs Time remaining in milliseconds
     * @param stockRemaining Remaining stock (0 = unlimited)
     * @param triggerUrgency Urgency level to display
     * @return Created offer
     */
    public LimitedTimeOffer createOffer(String offerId, double discountPercent, 
                                         long timeRemainingMs, int stockRemaining,
                                         UrgencyLevel triggerUrgency) {
        LimitedTimeOffer offer = new LimitedTimeOffer(
            offerId, discountPercent, timeRemainingMs, stockRemaining, triggerUrgency
        );
        activeOffers.put(offerId, offer);
        return offer;
    }
    
    /**
     * Get or create a default offer for simulation
     */
    public LimitedTimeOffer createDefaultOffer(String offerId) {
        return createOffer(offerId, 
            20 + random.nextDouble() * 30, // 20-50% discount
            3600000 + random.nextInt(7200000), // 1-3 hours
            random.nextInt(10) + 1, // 1-10 items
            UrgencyLevel.HIGH
        );
    }
    
    /**
     * Check if user exhibits high-velocity purchasing behavior
     * 
     * @param userId User identifier
     * @param timeWindowMs Time window to analyze
     * @return HighVelocityResult
     */
    public HighVelocityResult checkHighVelocity(String userId, long timeWindowMs) {
        UserImpulseState state = userStates.get(userId);
        if (state == null) {
            return new HighVelocityResult(false, 0, "No purchase history");
        }
        
        int recentPurchases = state.getRecentPurchaseCount(timeWindowMs);
        double recentTotal = state.getRecentTotalSpent(timeWindowMs);
        
        boolean isHighVelocity = recentTotal > config.getHighVelocityPurchaseThreshold() ||
                                 recentPurchases >= 3;
        
        return new HighVelocityResult(
            isHighVelocity, 
            recentPurchases,
            String.format("%d purchases, $%.2f in last %d hours", 
                recentPurchases, recentTotal, timeWindowMs / 3600000)
        );
    }
    
    /**
     * Simulate browsing behavior that may lead to impulse
     * 
     * @param userId User identifier
     * @param productCategory Product category being viewed
     * @param productPrice Price of product
     * @return BrowsingImpulseResult
     */
    public BrowsingImpulseResult evaluateBrowsingImpulse(String userId, String productCategory,
                                                          double productPrice) {
        UserImpulseState state = userStates.computeIfAbsent(userId, k -> new UserImpulseState());
        
        // Base browsing impulse probability
        double baseProbability = config.getImpulsePurchaseProbability();
        
        // Adjust based on category (some categories have higher impulse rates)
        baseProbability *= getCategoryImpulseMultiplier(productCategory);
        
        // Adjust based on time of day (lunch and evening peaks)
        baseProbability *= getTimeOfDayMultiplier();
        
        // Adjust based on user's impulse history
        baseProbability *= state.getImpulseHistoryMultiplier();
        
        // Check for impulse trigger
        boolean triggers = random.nextDouble() < baseProbability;
        
        double impulseAmount = triggers ? calculateImpulseAmount(productPrice, null) : 0;
        
        return new BrowsingImpulseResult(
            triggers,
            impulseAmount,
            baseProbability,
            String.format("Category: %s, Time multiplier: %.2f", 
                productCategory, getTimeOfDayMultiplier())
        );
    }
    
    /**
     * Get active offer by ID
     */
    public LimitedTimeOffer getOffer(String offerId) {
        return activeOffers.get(offerId);
    }
    
    /**
     * Remove expired offers
     */
    public void cleanupExpiredOffers() {
        long now = System.currentTimeMillis();
        activeOffers.entrySet().removeIf(entry -> !entry.getValue().isActive());
    }
    
    /**
     * Get user's impulse statistics
     */
    public ImpulseStatistics getUserStatistics(String userId) {
        UserImpulseState state = userStates.get(userId);
        if (state == null) {
            return new ImpulseStatistics(0, 0, 0, 0);
        }
        
        return new ImpulseStatistics(
            state.getTotalImpulsePurchases(),
            state.getTotalImpulseAmount(),
            state.getRecentPurchaseCount(3600000), // Last hour
            state.getCooldownRemaining()
        );
    }
    
    /**
     * Force clear cooldown for testing
     */
    public void clearCooldown(String userId) {
        UserImpulseState state = userStates.get(userId);
        if (state != null) {
            state.clearCooldown();
        }
    }
    
    private double calculateImpulseProbability(UserImpulseState state, 
                                                 LimitedTimeOffer offer, 
                                                 double basePrice) {
        double probability = config.getImpulsePurchaseProbability();
        
        // Apply offer multiplier
        probability *= config.getLimitedTimeOfferMultiplier();
        
        // Apply discount level (higher discount = higher impulse)
        double discountImpact = offer.getDiscountPercent() / 100.0;
        probability += discountImpact * 0.3;
        
        // Apply stock scarcity (lower stock = higher impulse)
        if (offer.getStockRemaining() > 0) {
            double scarcityFactor = 1.0 - (offer.getStockRemaining() / 10.0);
            probability += scarcityFactor * 0.2;
        }
        
        // Apply urgency level
        probability *= offer.getUrgencyLevel().getMultiplier();
        
        // Apply user-specific factors
        probability *= state.getImpulseHistoryMultiplier();
        
        // Cap probability
        return Math.min(0.95, probability);
    }
    
    private double calculateImpulseAmount(double basePrice, LimitedTimeOffer offer) {
        double price = basePrice;
        
        // Apply discount if offer exists
        if (offer != null) {
            price = price * (1 - offer.getDiscountPercent() / 100.0);
        }
        
        // Impulse purchases can exceed normal limits up to impulse cap
        double maxImpulse = Math.min(
            config.getImpulseSpendingCap(),
            config.getMaxSingleTransactionLimit() * 3
        );
        
        // Random amount between price and max (often closer to price)
        double variance = random.nextDouble();
        if (variance < 0.6) {
            // 60% chance: pay full discounted price
            return price;
        } else if (variance < 0.85) {
            // 25% chance: add a small additional item
            return price * (1 + random.nextDouble() * 0.3);
        } else {
            // 15% chance: max out impulse spending
            return price + (random.nextDouble() * (maxImpulse - price));
        }
    }
    
    private double getCategoryImpulseMultiplier(String category) {
        switch (category.toLowerCase()) {
            case "electronics": return 1.5;
            case "fashion": return 1.3;
            case "home": return 1.1;
            case "groceries": return 0.8;
            case "books": return 0.6;
            default: return 1.0;
        }
    }
    
    private double getTimeOfDayMultiplier() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
        
        // Peak impulse times: 12-14 (lunch), 19-21 (evening)
        if (hour >= 12 && hour <= 14) return 1.3;
        if (hour >= 19 && hour <= 21) return 1.4;
        if (hour >= 22 || hour <= 5) return 0.7; // Late night = less impulse
        return 1.0;
    }
    
    private synchronized void updateBurstTracking() {
        long now = System.currentTimeMillis();
        if (now - lastImpulseBurst < 60000) { // Within last minute
            burstCount++;
        } else {
            burstCount = 1;
        }
        lastImpulseBurst = now;
    }
    
    public static int getBurstCount() {
        return burstCount;
    }
    
    // Inner classes
    
    public enum ImpulseTrigger {
        NONE,
        LIMITED_TIME_OFFER,
        STOCK_SCARCITY,
        SOCIAL_PROOF,
        COOLDOWN
    }
    
    public enum UrgencyLevel {
        LOW(1.0),
        MEDIUM(1.3),
        HIGH(1.6),
        CRITICAL(2.0);
        
        private final double multiplier;
        UrgencyLevel(double mult) { this.multiplier = mult; }
        public double getMultiplier() { return multiplier; }
    }
    
    public static class LimitedTimeOffer {
        private final String offerId;
        private final double discountPercent;
        private final long durationMs;
        private final long startTime;
        private final int stockRemaining;
        private final UrgencyLevel urgencyLevel;
        
        public LimitedTimeOffer(String offerId, double discountPercent, long durationMs,
                                int stockRemaining, UrgencyLevel urgencyLevel) {
            this.offerId = offerId;
            this.discountPercent = discountPercent;
            this.durationMs = durationMs;
            this.startTime = System.currentTimeMillis();
            this.stockRemaining = stockRemaining;
            this.urgencyLevel = urgencyLevel;
        }
        
        public String getOfferId() { return offerId; }
        public double getDiscountPercent() { return discountPercent; }
        public int getStockRemaining() { return stockRemaining; }
        public UrgencyLevel getUrgencyLevel() { return urgencyLevel; }
        
        public boolean isActive() {
            long elapsed = System.currentTimeMillis() - startTime;
            return elapsed < durationMs && (stockRemaining > 0 || stockRemaining == 0);
        }
        
        public long getTimeRemaining() {
            return Math.max(0, durationMs - (System.currentTimeMillis() - startTime));
        }
    }
    
    private static class UserImpulseState {
        private final Map<Long, Double> purchaseHistory;
        private long lastImpulseTime;
        private int totalImpulsePurchases;
        private double totalImpulseAmount;
        
        public UserImpulseState() {
            this.purchaseHistory = new HashMap<>();
            this.lastImpulseTime = 0;
            this.totalImpulsePurchases = 0;
            this.totalImpulseAmount = 0;
        }
        
        public void recordImpulsePurchase(double amount) {
            long now = System.currentTimeMillis();
            purchaseHistory.put(now, amount);
            lastImpulseTime = now;
            totalImpulsePurchases++;
            totalImpulseAmount += amount;
        }
        
        public boolean isInCooldown() {
            if (lastImpulseTime == 0) return false;
            long cooldownMs = config.getImpulseCooldownHours() * 3600000L;
            return (System.currentTimeMillis() - lastImpulseTime) < cooldownMs;
        }
        
        public long getCooldownRemaining() {
            if (!isInCooldown()) return 0;
            long cooldownMs = config.getImpulseCooldownHours() * 3600000L;
            return cooldownMs - (System.currentTimeMillis() - lastImpulseTime);
        }
        
        public int getRecentPurchaseCount(long timeWindowMs) {
            long cutoff = System.currentTimeMillis() - timeWindowMs;
            return (int) purchaseHistory.keySet().stream()
                .filter(time -> time > cutoff)
                .count();
        }
        
        public double getRecentTotalSpent(long timeWindowMs) {
            long cutoff = System.currentTimeMillis() - timeWindowMs;
            return purchaseHistory.entrySet().stream()
                .filter(e -> e.getKey() > cutoff)
                .mapToDouble(Map.Entry::getValue)
                .sum();
        }
        
        public double getImpulseHistoryMultiplier() {
            // More past impulses = slightly lower current impulse (fatigue)
            if (totalImpulsePurchases < 3) return 1.0;
            return Math.max(0.6, 1.0 - (totalImpulsePurchases - 2) * 0.1);
        }
        
        public int getTotalImpulsePurchases() { return totalImpulsePurchases; }
        public double getTotalImpulseAmount() { return totalImpulseAmount; }
        
        public void clearCooldown() {
            lastImpulseTime = 0;
        }
    }
    
    public static class ImpulseResult {
        private final boolean triggered;
        private final ImpulseTrigger trigger;
        private final double amount;
        private final String description;
        
        public ImpulseResult(boolean triggered, ImpulseTrigger trigger, 
                            double amount, String description) {
            this.triggered = triggered;
            this.trigger = trigger;
            this.amount = amount;
            this.description = description;
        }
        
        public boolean isTriggered() { return triggered; }
        public ImpulseTrigger getTrigger() { return trigger; }
        public double getAmount() { return amount; }
        public String getDescription() { return description; }
    }
    
    public static class HighVelocityResult {
        private final boolean isHighVelocity;
        private final int recentPurchaseCount;
        private final String description;
        
        public HighVelocityResult(boolean isHighVelocity, int recentPurchaseCount, 
                                  String description) {
            this.isHighVelocity = isHighVelocity;
            this.recentPurchaseCount = recentPurchaseCount;
            this.description = description;
        }
        
        public boolean isHighVelocity() { return isHighVelocity; }
        public int getRecentPurchaseCount() { return recentPurchaseCount; }
        public String getDescription() { return description; }
    }
    
    public static class BrowsingImpulseResult {
        private final boolean triggersImpulse;
        private final double impulseAmount;
        private final double probability;
        private final String description;
        
        public BrowsingImpulseResult(boolean triggersImpulse, double impulseAmount,
                                     double probability, String description) {
            this.triggersImpulse = triggersImpulse;
            this.impulseAmount = impulseAmount;
            this.probability = probability;
            this.description = description;
        }
        
        public boolean triggersImpulse() { return triggersImpulse; }
        public double getImpulseAmount() { return impulseAmount; }
        public double getProbability() { return probability; }
        public String getDescription() { return description; }
    }
    
    public static class ImpulseStatistics {
        private final int totalPurchases;
        private final double totalAmount;
        private final int recentPurchases;
        private final long cooldownRemainingMs;
        
        public ImpulseStatistics(int totalPurchases, double totalAmount, 
                                 int recentPurchases, long cooldownRemainingMs) {
            this.totalPurchases = totalPurchases;
            this.totalAmount = totalAmount;
            this.recentPurchases = recentPurchases;
            this.cooldownRemainingMs = cooldownRemainingMs;
        }
        
        public int getTotalPurchases() { return totalPurchases; }
        public double getTotalAmount() { return totalAmount; }
        public int getRecentPurchases() { return recentPurchases; }
        public long getCooldownRemainingMs() { return cooldownRemainingMs; }
    }
}
