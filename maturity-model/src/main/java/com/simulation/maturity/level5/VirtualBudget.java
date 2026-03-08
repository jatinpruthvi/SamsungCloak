package com.simulation.maturity.level5;

import java.util.*;

/**
 * Economic Constraints - Virtual Budget System
 * 
 * Simulates financial behavior and constraints:
 * - Virtual budget allocation across categories
 * - Risk aversion modeling
 * - Delayed conversion (hesitation before purchases)
 * - Financial cycle simulation (payday effects)
 * 
 * Implements bounded rationality for purchase decisions:
 * - Users don't always choose optimal option
 * - Price sensitivity varies by context
 * - Impulse vs. planned purchasing
 * 
 * Device Target: Samsung Galaxy A12 (SM-A125U)
 * Budget-conscious demographic profile
 */
public class VirtualBudget {
    
    // Budget categories
    public enum Category {
        ESSENTIAL("Required purchases", 0.6),
        ENTERTAINMENT("Games, apps, media", 0.2),
        LUXURY("Premium features", 0.15),
        SUBSCRIPTION("Recurring payments", 0.05);
        
        private final String description;
        private final double defaultAllocation;
        
        Category(String description, double defaultAllocation) {
            this.description = description;
            this.defaultAllocation = defaultAllocation;
        }
        
        public String getDescription() { return description; }
        public double getDefaultAllocation() { return defaultAllocation; }
    }
    
    // User financial state
    private String userId;
    private double monthlyBudget;        // Total monthly budget for app-related spending
    private double remainingBudget;      // Current remaining budget
    private double spentThisMonth;       // Amount spent this month
    private Map<Category, Double> categoryAllocations;
    private Map<Category, Double> categorySpending;
    
    // Behavioral parameters
    private double riskAversion;          // 0.0 = risk-tolerant, 1.0 = very risk-averse
    private double priceSensitivity;     // 0.0 = insensitive, 1.0 = very sensitive
    private double impulseFactor;        // Probability of impulse purchases
    
    // Financial cycle
    private int dayOfMonth;              // 1-30
    private int paydayDay;               // Day of month when user gets paid
    private double[] monthlyIncomeCycle; // Daily income pattern
    
    // Hesitation tracking
    private Map<String, Long> cartItems;        // itemId -> added timestamp
    private Map<String, Double> hesitationDelays; // itemId -> delay before purchase
    
    private Random random;
    private Calendar calendar;
    
    public VirtualBudget(String userId, double monthlyBudget) {
        this.userId = userId;
        this.monthlyBudget = monthlyBudget;
        this.remainingBudget = monthlyBudget;
        this.spentThisMonth = 0;
        
        this.categoryAllocations = new HashMap<>();
        this.categorySpending = new HashMap<>();
        
        // Default allocations
        for (Category cat : Category.values()) {
            categoryAllocations.put(cat, monthlyBudget * cat.getDefaultAllocation());
            categorySpending.put(cat, 0.0);
        }
        
        // Default behavioral parameters (budget-conscious user)
        this.riskAversion = 0.7;         // High aversion
        this.priceSensitivity = 0.8;     // Very price sensitive
        this.impulseFactor = 0.2;        // Low impulse purchasing
        
        this.dayOfMonth = 1;
        this.paydayDay = 1;
        this.monthlyIncomeCycle = new double[30];
        
        this.cartItems = new HashMap<>();
        this.hesitationDelays = new HashMap<>();
        
        this.random = new Random();
        this.calendar = Calendar.getInstance();
        
        // Initialize income cycle (bi-weekly paydays)
        initializeIncomeCycle();
    }
    
    /**
     * Initialize monthly income pattern (bi-weekly paydays)
     */
    private void initializeIncomeCycle() {
        Arrays.fill(monthlyIncomeCycle, 0);
        
        // Payday 1
        for (int i = paydayDay - 1; i < Math.min(30, paydayDay + 5); i++) {
            monthlyIncomeCycle[i] += 0.4;
        }
        
        // Payday 2 (2 weeks later)
        int payday2 = (paydayDay + 14) % 30;
        for (int i = payday2 - 1; i < Math.min(30, payday2 + 5); i++) {
            monthlyIncomeCycle[i] += 0.4;
        }
    }
    
    /**
     * Set payday day
     */
    public void setPaydayDay(int day) {
        this.paydayDay = Math.max(1, Math.min(30, day));
        initializeIncomeCycle();
    }
    
    /**
     * Check if user can afford a purchase
     * 
     * @param amount Purchase amount
     * @param category Spending category
     * @return Affordability result
     */
    public AffordabilityResult canAfford(double amount, Category category) {
        // Check total budget
        if (amount > remainingBudget) {
            return new AffordabilityResult(false, "Exceeds total budget", 0);
        }
        
        // Check category budget
        double categoryBudget = categoryAllocations.get(category);
        double categorySpent = categorySpending.get(category);
        double categoryRemaining = categoryBudget - categorySpent;
        
        if (amount > categoryRemaining) {
            return new AffordabilityResult(false, "Exceeds category budget", 
                categoryRemaining / amount);
        }
        
        // Risk aversion check
        double riskCheck = random.nextDouble();
        if (riskCheck < riskAversion * 0.3) {
            // Risk averse users may still decline
            return new AffordabilityResult(false, "Risk aversion", 0);
        }
        
        return new AffordabilityResult(true, "Affordable", 1.0);
    }
    
    /**
     * Process a purchase attempt
     * 
     * @param amount Purchase amount
     * @param category Category
     * @param itemId Item identifier
     * @return Purchase result
     */
    public PurchaseResult attemptPurchase(double amount, Category category, String itemId) {
        AffordabilityResult affordability = canAfford(amount, category);
        
        if (!affordability.canAfford) {
            return new PurchaseResult(false, affordability.reason, amount, 0);
        }
        
        // Apply price sensitivity
        double effectivePrice = applyPriceSensitivity(amount);
        
        // Calculate hesitation delay based on amount
        long delay = calculateHesitationDelay(effectivePrice);
        
        // Simulate hesitation before purchase
        if (random.nextDouble() < (delay / 10000.0)) { // Probability proportional to delay
            // User hesitates - add to cart instead of immediate purchase
            cartItems.put(itemId + "_" + System.currentTimeMillis(), System.currentTimeMillis());
            hesitationDelays.put(itemId, (double) delay);
            
            return new PurchaseResult(false, "Hesitation - added to cart", effectivePrice, delay);
        }
        
        // Check impulse factor
        if (random.nextDouble() < impulseFactor && amount < 5.0) {
            // Impulse purchase - lower scrutiny
            return completePurchase(effectivePrice, category, itemId);
        }
        
        // Complete purchase
        return completePurchase(effectivePrice, category, itemId);
    }
    
    /**
     * Complete a purchase
     */
    private PurchaseResult completePurchase(double amount, Category category, String itemId) {
        remainingBudget -= amount;
        spentThisMonth += amount;
        categorySpending.merge(category, amount, Double::sum);
        
        return new PurchaseResult(true, "Purchase completed", amount, 0);
    }
    
    /**
     * Apply price sensitivity adjustment
     */
    private double applyPriceSensitivity(double amount) {
        // Price sensitive users may look for alternatives
        if (random.nextDouble() < priceSensitivity * 0.5) {
            // Apply 5-15% discount expectation
            double discount = amount * (0.05 + random.nextDouble() * 0.10);
            amount -= discount;
        }
        
        return amount;
    }
    
    /**
     * Calculate hesitation delay based on amount
     * Higher amounts = longer hesitation
     */
    private long calculateHesitationDelay(double amount) {
        // Base delay: 1 second per dollar for amounts > $5
        long baseDelay = (long) (Math.max(0, amount - 5) * 1000);
        
        // Risk aversion increases delay
        baseDelay = (long) (baseDelay * (1 + riskAversion));
        
        // Price sensitivity increases delay
        baseDelay = (long) (baseDelay * (1 + priceSensitivity * 0.5));
        
        return Math.min(baseDelay, 60000); // Max 60 second delay
    }
    
    /**
     * Check and complete pending cart items
     * Called when user returns to app
     */
    public CartCompletionResult processCart() {
        int completed = 0;
        double totalCompleted = 0;
        
        List<String> toRemove = new ArrayList<>();
        
        for (Map.Entry<String, Long> entry : cartItems.entrySet()) {
            String itemId = entry.getKey().split("_")[0];
            long timeInCart = System.currentTimeMillis() - entry.getValue();
            
            // Complete if been in cart less than 24 hours
            if (timeInCart < 86400000) {
                // Random completion chance based on time
                double completionChance = Math.min(0.8, timeInCart / 86400000.0);
                
                if (random.nextDouble() < completionChance) {
                    // Would need amount - simplified here
                    completed++;
                    toRemove.add(entry.getKey());
                }
            } else {
                // Old cart items - remove
                toRemove.add(entry.getKey());
            }
        }
        
        for (String key : toRemove) {
            cartItems.remove(key);
            hesitationDelays.remove(key);
        }
        
        return new CartCompletionResult(completed, totalCompleted);
    }
    
    /**
     * Simulate financial cycle effect on purchasing power
     * 
     * @return Multiplier for purchasing (0.2 - 1.5)
     */
    public double getFinancialCycleMultiplier() {
        // Day of month affects available funds
        int daysSincePayday = (dayOfMonth - paydayDay + 30) % 30;
        
        // Funds available: high just after payday, low just before
        double baseMultiplier = 1.0;
        
        if (daysSincePayday < 3) {
            // Just paid - higher purchasing power
            baseMultiplier = 1.3 + random.nextDouble() * 0.2;
        } else if (daysSincePayday > 25) {
            // Near end of cycle - lower purchasing power
            baseMultiplier = 0.3 + random.nextDouble() * 0.3;
        } else {
            // Mid-cycle
            baseMultiplier = 0.7 + random.nextDouble() * 0.4;
        }
        
        return baseMultiplier;
    }
    
    /**
     * Advance to next day
     */
    public void advanceDay() {
        dayOfMonth = (dayOfMonth % 30) + 1;
        
        // Reset monthly at start
        if (dayOfMonth == 1) {
            resetMonth();
        }
    }
    
    /**
     * Reset budget for new month
     */
    private void resetMonth() {
        remainingBudget = monthlyBudget;
        spentThisMonth = 0;
        
        for (Category cat : Category.values()) {
            categorySpending.put(cat, 0.0);
        }
        
        cartItems.clear();
        hesitationDelays.clear();
    }
    
    /**
     * Get remaining budget
     */
    public double getRemainingBudget() {
        return remainingBudget;
    }
    
    /**
     * Get spending this month
     */
    public double getSpentThisMonth() {
        return spentThisMonth;
    }
    
    /**
     * Get budget utilization percentage
     */
    public double getBudgetUtilization() {
        return spentThisMonth / monthlyBudget;
    }
    
    /**
     * Set risk aversion
     */
    public void setRiskAversion(double aversion) {
        this.riskAversion = Math.max(0, Math.min(1, aversion));
    }
    
    /**
     * Get financial summary
     */
    public FinancialSummary getSummary() {
        return new FinancialSummary(
            userId, monthlyBudget, remainingBudget, spentThisMonth,
            getBudgetUtilization(), cartItems.size(), riskAversion
        );
    }
    
    /**
     * Affordability result
     */
    public static class AffordabilityResult {
        public final boolean canAfford;
        public final String reason;
        public final double affordabilityRatio;
        
        public AffordabilityResult(boolean canAfford, String reason, double ratio) {
            this.canAfford = canAfford;
            this.reason = reason;
            this.affordabilityRatio = ratio;
        }
    }
    
    /**
     * Purchase result
     */
    public static class PurchaseResult {
        public final boolean success;
        public final String message;
        public final double amount;
        public final long hesitationDelayMs;
        
        public PurchaseResult(boolean success, String message, double amount, long delayMs) {
            this.success = success;
            this.message = message;
            this.amount = amount;
            this.hesitationDelayMs = delayMs;
        }
    }
    
    /**
     * Cart completion result
     */
    public static class CartCompletionResult {
        public final int itemsCompleted;
        public final double totalCompleted;
        
        public CartCompletionResult(int items, double total) {
            this.itemsCompleted = items;
            this.totalCompleted = total;
        }
    }
    
    /**
     * Financial summary
     */
    public static class FinancialSummary {
        public final String userId;
        public final double budget;
        public final double remaining;
        public final double spent;
        public final double utilization;
        public final int cartItems;
        public final double riskAversion;
        
        public FinancialSummary(String userId, double budget, double remaining, 
                                double spent, double utilization, int cartItems,
                                double riskAversion) {
            this.userId = userId;
            this.budget = budget;
            this.remaining = remaining;
            this.spent = spent;
            this.utilization = utilization;
            this.cartItems = cartItems;
            this.riskAversion = riskAversion;
        }
        
        @Override
        public String toString() {
            return String.format(
                "Financial{user=%s, budget=%.2f, remaining=%.2f, spent=%.2f " +
                "(%.0f%%), cart=%d, riskAversion=%.1f}",
                userId, budget, remaining, spent, utilization * 100,
                cartItems, riskAversion
            );
        }
    }
}
