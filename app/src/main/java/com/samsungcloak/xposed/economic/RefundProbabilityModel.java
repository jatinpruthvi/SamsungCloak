package com.samsungcloak.xposed.economic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Refund Probability Model - Buyer's Remorse Simulation
 * 
 * Models the phenomenon where completed purchases are sometimes reversed
 * due to regret, comparison shopping, or financial constraints.
 * 
 * Real-world behavior:
 * - Some purchases trigger immediate regret
 * - Returns often happen within 24 hours
 * - Higher value purchases have higher remorse rates
 * - Impulse purchases more likely to be returned
 * - Social proof can reduce remorse (justification)
 * 
 * Tests: Refund flow handling, return analytics, fraud detection on returns,
 *        inventory reconciliation, customer service simulation
 */
public class RefundProbabilityModel {
    
    private final EconomicConfig config;
    private final Random random;
    
    // Track completed purchases pending potential refund
    private final Map<String, CompletedPurchase> completedPurchases;
    // Track refund requests
    private final Map<String, List<RefundRequest>> refundRequests;
    // User remorse profiles
    private final Map<String, UserRemorseProfile> userProfiles;
    
    public RefundProbabilityModel(EconomicConfig config) {
        this.config = config;
        this.random = config.getRandom();
        this.completedPurchases = new ConcurrentHashMap<>();
        this.refundRequests = new ConcurrentHashMap<>();
        this.userProfiles = new ConcurrentHashMap<>();
    }
    
    /**
     * Record a completed purchase that could potentially be refunded
     * 
     * @param userId User identifier
     * @param transactionId Transaction ID
     * @param amount Purchase amount
     * @param itemCount Number of items
     * @param category Purchase category
     * @param wasImpulse Whether purchase was impulse-driven
     * @return CompletedPurchase record
     */
    public CompletedPurchase recordPurchase(String userId, String transactionId, 
                                             double amount, int itemCount, 
                                             String category, boolean wasImpulse) {
        CompletedPurchase purchase = new CompletedPurchase(
            transactionId, userId, amount, itemCount, category,
            wasImpulse, System.currentTimeMillis()
        );
        
        completedPurchases.put(transactionId, purchase);
        
        // Initialize user profile if new
        userProfiles.computeIfAbsent(userId, k -> new UserRemorseProfile());
        
        return purchase;
    }
    
    /**
     * Check if a purchase will trigger a refund request (buyer's remorse)
     * Should be called shortly after purchase to simulate immediate regret
     * 
     * @param transactionId Transaction to check
     * @return RefundAssessment with outcome
     */
    public RefundAssessment assessRefundLikelihood(String transactionId) {
        CompletedPurchase purchase = completedPurchases.get(transactionId);
        if (purchase == null) {
            return new RefundAssessment(false, 0, "Purchase not found");
        }
        
        // Check if within refund window
        long timeSincePurchase = System.currentTimeMillis() - purchase.getPurchaseTime();
        if (timeSincePurchase > config.getRefundWindowHours() * 3600000L) {
            return new RefundAssessment(false, 0, "Outside refund window");
        }
        
        // Check if already refunded
        if (purchase.isRefunded()) {
            return new RefundAssessment(true, purchase.getAmount(), "Already refunded");
        }
        
        // Calculate refund probability
        double refundProbability = calculateRefundProbability(purchase);
        
        boolean willRequestRefund = random.nextDouble() < refundProbability;
        
        if (willRequestRefund) {
            // Create refund request
            RefundRequest request = createRefundRequest(purchase);
            purchase.markAsRefunded();
            
            List<Request> requests = refundRequests.computeIfAbsent(
                purchase.getUserId(), k -> new ArrayList<>());
            requests.add(request);
            
            return new RefundAssessment(true, purchase.getAmount(),
                String.format("Refund requested: probability was %.2f", refundProbability));
        }
        
        return new RefundAssessment(false, refundProbability,
            String.format("No refund: probability %.2f", refundProbability));
    }
    
    /**
     * Simulate delayed remorse check (e.g., next day)
     * 
     * @param transactionId Transaction to check
     * @param hoursAfterPurchase Hours since purchase
     * @return RefundAssessment
     */
    public RefundAssessment assessDelayedRemorse(String transactionId, int hoursAfterPurchase) {
        CompletedPurchase purchase = completedPurchases.get(transactionId);
        if (purchase == null || purchase.isRefunded()) {
            return new RefundAssessment(false, 0, "Purchase not found or already refunded");
        }
        
        // Remorse probability changes over time
        // Most remorse within first few hours, decreases over time
        double timeDecayFactor;
        if (hoursAfterPurchase <= 2) {
            timeDecayFactor = 1.0; // Peak remorse
        } else if (hoursAfterPurchase <= 12) {
            timeDecayFactor = 0.7;
        } else if (hoursAfterPurchase <= 24) {
            timeDecayFactor = 0.4;
        } else {
            timeDecayFactor = 0.2;
        }
        
        double baseProbability = calculateRefundProbability(purchase);
        double adjustedProbability = baseProbability * timeDecayFactor;
        
        boolean willRequestRefund = random.nextDouble() < adjustedProbability;
        
        if (willRequestRefund) {
            RefundRequest request = createRefundRequest(purchase);
            purchase.markAsRefunded();
            
            List<RefundRequest> requests = refundRequests.computeIfAbsent(
                purchase.getUserId(), k -> new ArrayList<>());
            requests.add(request);
            
            return new RefundAssessment(true, purchase.getAmount(),
                String.format("Delayed refund at hour %d", hoursAfterPurchase));
        }
        
        return new RefundAssessment(false, adjustedProbability,
            String.format("No delayed refund: probability %.2f", adjustedProbability));
    }
    
    /**
     * Get user's refund history statistics
     * 
     * @param userId User identifier
     * @return UserRemorseStats
     */
    public UserRemorseStats getUserRemorseStats(String userId) {
        List<RefundRequest> requests = refundRequests.get(userId);
        UserRemorseProfile profile = userProfiles.get(userId);
        
        int totalRequests = requests != null ? requests.size() : 0;
        double totalRefunded = requests != null ? 
            requests.stream().mapToDouble(RefundRequest::getAmount).sum() : 0;
        
        double remorseScore = profile != null ? profile.getRemorseScore() : 0.3;
        
        return new UserRemorseStats(totalRequests, totalRefunded, remorseScore);
    }
    
    /**
     * Check for refund requests that should be processed
     * 
     * @return List of pending refund requests
     */
    public List<RefundRequest> processPendingRefunds() {
        List<RefundRequest> processed = new ArrayList<>();
        
        for (Map.Entry<String, CompletedPurchase> entry : completedPurchases.entrySet()) {
            CompletedPurchase purchase = entry.getValue();
            
            if (!purchase.isRefunded() && !purchase.isRefundProcessed()) {
                long timeSince = System.currentTimeMillis() - purchase.getPurchaseTime();
                
                // Check within refund window
                if (timeSince <= config.getRefundWindowHours() * 3600000L) {
                    // Random check for refund (simulates user deciding to return)
                    double probability = calculateRefundProbability(purchase);
                    
                    // More likely to process refund as time passes
                    double timeFactor = timeSince / (config.getRefundWindowHours() * 1800000.0);
                    probability *= Math.min(1.0, timeFactor);
                    
                    if (random.nextDouble() < probability * 0.3) { // 30% check rate
                        RefundRequest request = createRefundRequest(purchase);
                        purchase.markAsRefunded();
                        purchase.markAsProcessed();
                        processed.add(request);
                    }
                }
            }
        }
        
        return processed;
    }
    
    /**
     * Get all refund requests for a user
     * 
     * @param userId User identifier
     * @return List of refund requests
     */
    public List<RefundRequest> getRefundRequests(String userId) {
        return new ArrayList<>(refundRequests.getOrDefault(userId, new ArrayList<>()));
    }
    
    /**
     * Clear old purchases to manage memory
     * 
     * @param olderThanHours Remove purchases older than this
     */
    public void cleanupOldPurchases(int olderThanHours) {
        long cutoff = System.currentTimeMillis() - (olderThanHours * 3600000L);
        completedPurchases.entrySet().removeIf(
            entry -> entry.getValue().getPurchaseTime() < cutoff);
    }
    
    private double calculateRefundProbability(CompletedPurchase purchase) {
        double probability = config.getRefundProbability();
        
        // Amount factor - higher value = higher remorse
        if (purchase.getAmount() > 50) {
            probability += 0.05;
        }
        if (purchase.getAmount() > 100) {
            probability += 0.08;
        }
        
        // Impulse purchases more likely to be returned
        if (purchase.wasImpulse()) {
            probability += config.getRefundProbabilityIncreaseWithRegret();
        }
        
        // Category factors
        probability += getCategoryRemorseFactor(purchase.getCategory());
        
        // User profile factor
        UserRemorseProfile profile = userProfiles.get(purchase.getUserId());
        if (profile != null) {
            probability *= profile.getRemorseScore();
        }
        
        return Math.min(0.5, probability); // Cap at 50% max
    }
    
    private double getCategoryRemorseFactor(String category) {
        switch (category.toLowerCase()) {
            case "electronics": return 0.03; // Often compatibility concerns
            case "fashion": return 0.05; // Fit/size issues
            case "home": return 0.02;
            case "luxury": return 0.04;
            default: return 0;
        }
    }
    
    private RefundRequest createRefundRequest(CompletedPurchase purchase) {
        RefundReason reason = selectRefundReason(purchase);
        
        return new RefundRequest(
            "REFUND-" + System.currentTimeMillis(),
            purchase.getTransactionId(),
            purchase.getUserId(),
            purchase.getAmount(),
            reason,
            System.currentTimeMillis()
        );
    }
    
    private RefundReason selectRefundReason(CompletedPurchase purchase) {
        // Weight reasons based on purchase characteristics
        double[] weights;
        
        if (purchase.wasImpulse()) {
            weights = new double[]{0.4, 0.2, 0.15, 0.15, 0.1}; // Higher "found better price"
        } else if (purchase.getAmount() > 100) {
            weights = new double[]{0.2, 0.3, 0.2, 0.15, 0.15}; // Higher "too expensive"
        } else {
            weights = new double[]{0.25, 0.25, 0.2, 0.15, 0.15};
        }
        
        double roll = random.nextDouble();
        double cumulative = 0;
        
        for (int i = 0; i < RefundReason.values().length; i++) {
            cumulative += weights[i];
            if (roll < cumulative) {
                return RefundReason.values()[i];
            }
        }
        
        return RefundReason.CHANGED_MIND;
    }
    
    // Inner classes
    
    public enum RefundReason {
        FOUND_BETTER_PRICE,
        TOO_EXPENSIVE,
        CHANGED_MIND,
        QUALITY_CONCERN,
        ACCIDENTAL_PURCHASE
    }
    
    public static class CompletedPurchase {
        private final String transactionId;
        private final String userId;
        private final double amount;
        private final int itemCount;
        private final String category;
        private final boolean wasImpulse;
        private final long purchaseTime;
        private boolean refunded;
        private boolean processed;
        
        public CompletedPurchase(String transactionId, String userId, double amount,
                                 int itemCount, String category, boolean wasImpulse,
                                 long purchaseTime) {
            this.transactionId = transactionId;
            this.userId = userId;
            this.amount = amount;
            this.itemCount = itemCount;
            this.category = category;
            this.wasImpulse = wasImpulse;
            this.purchaseTime = purchaseTime;
            this.refunded = false;
            this.processed = false;
        }
        
        public String getTransactionId() { return transactionId; }
        public String getUserId() { return userId; }
        public double getAmount() { return amount; }
        public int getItemCount() { return itemCount; }
        public String getCategory() { return category; }
        public boolean wasImpulse() { return wasImpulse; }
        public long getPurchaseTime() { return purchaseTime; }
        public boolean isRefunded() { return refunded; }
        public boolean isRefundProcessed() { return processed; }
        
        public void markAsRefunded() { this.refunded = true; }
        public void markAsProcessed() { this.processed = true; }
    }
    
    public static class RefundRequest {
        private final String refundId;
        private final String transactionId;
        private final String userId;
        private final double amount;
        private final RefundReason reason;
        private final long requestTime;
        
        public RefundRequest(String refundId, String transactionId, String userId,
                            double amount, RefundReason reason, long requestTime) {
            this.refundId = refundId;
            this.transactionId = transactionId;
            this.userId = userId;
            this.amount = amount;
            this.reason = reason;
            this.requestTime = requestTime;
        }
        
        public String getRefundId() { return refundId; }
        public String getTransactionId() { return transactionId; }
        public String getUserId() { return userId; }
        public double getAmount() { return amount; }
        public RefundReason getReason() { return reason; }
        public long getRequestTime() { return requestTime; }
    }
    
    public static class RefundAssessment {
        private final boolean willRefund;
        private final double probability;
        private final String description;
        
        public RefundAssessment(boolean willRefund, double probability, String description) {
            this.willRefund = willRefund;
            this.probability = probability;
            this.description = description;
        }
        
        public boolean willRefund() { return willRefund; }
        public double getProbability() { return probability; }
        public String getDescription() { return description; }
    }
    
    public static class UserRemorseStats {
        private final int totalRefundRequests;
        private final double totalAmountRefunded;
        private final double remorseScore;
        
        public UserRemorseStats(int totalRefundRequests, double totalAmountRefunded,
                                double remorseScore) {
            this.totalRefundRequests = totalRefundRequests;
            this.totalAmountRefunded = totalAmountRefunded;
            this.remorseScore = remorseScore;
        }
        
        public int getTotalRefundRequests() { return totalRefundRequests; }
        public double getTotalAmountRefunded() { return totalAmountRefunded; }
        public double getRemorseScore() { return remorseScore; }
    }
    
    private static class UserRemorseProfile {
        private int purchaseCount;
        private int refundCount;
        private double totalSpent;
        private double totalRefunded;
        
        public UserRemorseProfile() {
            this.purchaseCount = 0;
            this.refundCount = 0;
            this.totalSpent = 0;
            this.totalRefunded = 0;
        }
        
        public void recordPurchase(double amount) {
            purchaseCount++;
            totalSpent += amount;
        }
        
        public void recordRefund(double amount) {
            refundCount++;
            totalRefunded += amount;
        }
        
        public double getRemorseScore() {
            if (purchaseCount == 0) return 0.3; // Default
            
            double refundRate = (double) refundCount / purchaseCount;
            double amountRatio = totalSpent > 0 ? totalRefunded / totalSpent : 0;
            
            return Math.min(1.0, (refundRate * 0.7) + (amountRatio * 0.3));
        }
    }
}
