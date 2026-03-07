package com.samsungcloak.xposed.economic;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Virtual Wallet - Budget Limitation Logic
 * 
 * Tracks daily spending and enforces budget constraints.
 * Triggers "Window Shopping" behavior when daily threshold is reached.
 * 
 * Real-world behavior:
 * - Users have finite daily budgets
 * - Will browse but not purchase when budget exhausted
 * - Day-of-pay cycles affect spending patterns
 * - Single large purchases can deplete budget quickly
 * 
 * Tests: Budget enforcement, session continuation after budget depletion,
 *        window shopping analytics, payment gateway resilience
 */
public class VirtualWallet {
    
    private final EconomicConfig config;
    private final Map<String, DailyBudget> dailyBudgets;
    private final Map<String, TransactionHistory> transactionHistories;
    private long lastResetTime;
    
    public VirtualWallet(EconomicConfig config) {
        this.config = config;
        this.dailyBudgets = new ConcurrentHashMap<>();
        this.transactionHistories = new ConcurrentHashMap<>();
        this.lastResetTime = System.currentTimeMillis();
    }
    
    /**
     * Check if a transaction can proceed given current budget constraints
     * 
     * @param amount Transaction amount
     * @param userId User identifier
     * @return TransactionResult indicating whether transaction proceeds or falls back to window shopping
     */
    public TransactionResult canAfford(double amount, String userId) {
        String todayKey = getTodayKey(userId);
        DailyBudget budget = getOrCreateDailyBudget(todayKey);
        
        if (!config.isEnableBudgetEnforcement()) {
            return new TransactionResult(true, TransactionType.STANDARD, 
                budget.getRemainingBudget(), "Budget enforcement disabled");
        }
        
        // Check single transaction limit
        if (amount > config.getMaxSingleTransactionLimit()) {
            return new TransactionResult(false, TransactionType.BLOCKED,
                budget.getRemainingBudget(), 
                String.format("Exceeds single transaction limit of $%.2f", 
                    config.getMaxSingleTransactionLimit()));
        }
        
        // Check daily budget
        double remaining = budget.getRemainingBudget();
        if (amount > remaining) {
            // Trigger window shopping behavior
            budget.incrementWindowShoppingBounces();
            return new TransactionResult(false, TransactionType.WINDOW_SHOPPING,
                remaining,
                String.format("Daily budget exhausted. Remaining: $%.2f. Viewing only.", remaining));
        }
        
        return new TransactionResult(true, TransactionType.STANDARD, remaining, "Proceed");
    }
    
    /**
     * Record a successful transaction
     * 
     * @param amount Transaction amount
     * @param userId User identifier
     * @param transactionId Transaction ID
     * @return true if successfully recorded
     */
    public boolean recordTransaction(double amount, String userId, String transactionId) {
        String todayKey = getTodayKey(userId);
        DailyBudget budget = getOrCreateDailyBudget(todayKey);
        
        budget.addTransaction(amount, transactionId);
        
        // Add to history
        TransactionHistory history = transactionHistories.computeIfAbsent(
            userId, k -> new TransactionHistory());
        history.addTransaction(transactionId, amount, System.currentTimeMillis());
        
        return true;
    }
    
    /**
     * Calculate remaining budget for a user today
     * 
     * @param userId User identifier
     * @return Remaining budget
     */
    public double getRemainingBudget(String userId) {
        String todayKey = getTodayKey(userId);
        DailyBudget budget = dailyBudgets.get(todayKey);
        return budget != null ? budget.getRemainingBudget() : config.getDailySpendingLimit();
    }
    
    /**
     * Get daily spending total
     * 
     * @param userId User identifier
     * @return Today's total spending
     */
    public double getDailySpendingTotal(String userId) {
        String todayKey = getTodayKey(userId);
        DailyBudget budget = dailyBudgets.get(todayKey);
        return budget != null ? budget.getTotalSpent() : 0.0;
    }
    
    /**
     * Get window shopping bounce count (how many times user browsed but couldn't buy)
     * 
     * @param userId User identifier
     * @return Number of window shopping instances today
     */
    public int getWindowShoppingBounces(String userId) {
        String todayKey = getTodayKey(userId);
        DailyBudget budget = dailyBudgets.get(todayKey);
        return budget != null ? budget.getWindowShoppingBounces() : 0;
    }
    
    /**
     * Check if daily budget is depleted
     * 
     * @param userId User identifier
     * @return true if no budget remaining
     */
    public boolean isBudgetDepleted(String userId) {
        return getRemainingBudget(userId) <= 0.01;
    }
    
    /**
     * Get spending velocity (transactions per hour average)
     * 
     * @param userId User identifier
     * @return Spending velocity
     */
    public double getSpendingVelocity(String userId) {
        String todayKey = getTodayKey(userId);
        DailyBudget budget = dailyBudgets.get(todayKey);
        if (budget == null) return 0.0;
        
        long sessionDuration = System.currentTimeMillis() - budget.getSessionStartTime();
        double hours = sessionDuration / 3600000.0;
        return hours > 0 ? budget.getTransactionCount() / hours : 0.0;
    }
    
    /**
     * Apply time-based reset (simulate new day)
     * Called automatically on date change, can be manually triggered
     */
    public void checkDailyReset(String userId) {
        String todayKey = getTodayKey(userId);
        if (!dailyBudgets.containsKey(todayKey)) {
            // New day, previous budget cleared
            lastResetTime = System.currentTimeMillis();
        }
    }
    
    /**
     * Apply a spending limit modification (e.g., pay day boost)
     * 
     * @param userId User identifier
     * @param bonus Additional budget to add
     */
    public void applyBudgetBonus(String userId, double bonus) {
        String todayKey = getTodayKey(userId);
        DailyBudget budget = getOrCreateDailyBudget(todayKey);
        budget.applyBonus(bonus);
    }
    
    private String getTodayKey(String userId) {
        long now = System.currentTimeMillis();
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(now);
        return userId + "_" + cal.get(java.util.Calendar.YEAR) + "_" + 
               cal.get(java.util.Calendar.DAY_OF_YEAR);
    }
    
    private DailyBudget getOrCreateDailyBudget(String key) {
        return dailyBudgets.computeIfAbsent(key, k -> 
            new DailyBudget(config.getDailySpendingLimit() * config.getTypicalBudgetMultiplier()));
    }
    
    public enum TransactionType {
        STANDARD,
        WINDOW_SHOPPING,
        BLOCKED
    }
    
    public static class TransactionResult {
        private final boolean canProceed;
        private final TransactionType type;
        private final double remainingBudget;
        private final String reason;
        
        public TransactionResult(boolean canProceed, TransactionType type, 
                                double remainingBudget, String reason) {
            this.canProceed = canProceed;
            this.type = type;
            this.remainingBudget = remainingBudget;
            this.reason = reason;
        }
        
        public boolean canProceed() { return canProceed; }
        public TransactionType getType() { return type; }
        public double getRemainingBudget() { return remainingBudget; }
        public String getReason() { return reason; }
    }
    
    private static class DailyBudget {
        private final double dailyLimit;
        private double remaining;
        private double totalSpent;
        private int transactionCount;
        private int windowShoppingBounces;
        private final long sessionStartTime;
        private double bonusApplied;
        
        public DailyBudget(double dailyLimit) {
            this.dailyLimit = dailyLimit;
            this.remaining = dailyLimit;
            this.totalSpent = 0;
            this.transactionCount = 0;
            this.windowShoppingBounces = 0;
            this.sessionStartTime = System.currentTimeMillis();
            this.bonusApplied = 0;
        }
        
        public void addTransaction(double amount, String transactionId) {
            remaining -= amount;
            totalSpent += amount;
            transactionCount++;
        }
        
        public double getRemainingBudget() {
            return Math.max(0, remaining);
        }
        
        public double getTotalSpent() { return totalSpent; }
        public int getTransactionCount() { return transactionCount; }
        public int getWindowShoppingBounces() { return windowShoppingBounces; }
        public long getSessionStartTime() { return sessionStartTime; }
        
        public void incrementWindowShoppingBounces() {
            windowShoppingBounces++;
        }
        
        public void applyBonus(double bonus) {
            remaining += bonus;
            bonusApplied += bonus;
        }
    }
    
    private static class TransactionHistory {
        private final Map<String, TransactionRecord> transactions;
        
        public TransactionHistory() {
            this.transactions = new HashMap<>();
        }
        
        public void addTransaction(String id, double amount, long timestamp) {
            transactions.put(id, new TransactionRecord(amount, timestamp));
        }
        
        public TransactionRecord getTransaction(String id) {
            return transactions.get(id);
        }
        
        public Map<String, TransactionRecord> getAll() {
            return new HashMap<>(transactions);
        }
    }
    
    public static class TransactionRecord {
        private final double amount;
        private final long timestamp;
        
        public TransactionRecord(double amount, long timestamp) {
            this.amount = amount;
            this.timestamp = timestamp;
        }
        
        public double getAmount() { return amount; }
        public long getTimestamp() { return timestamp; }
    }
}
