package com.samsungcloak.xposed.economic;

import java.util.UUID;

/**
 * Economic Constraint Model - Main Controller
 * 
 * Integrates all economic realism hooks for payment gateway resilience testing.
 * Provides unified API for simulating human financial behavior patterns.
 * 
 * Components:
 * - VirtualWallet: Budget limitation and window shopping
 * - HesitationLoop: Decision friction before purchases
 * - CartPersistenceHook: Delayed purchase decisions
 * - ImpulseBuyingModel: Stochastic impulse purchases
 * - RefundProbabilityModel: Buyer's remorse simulation
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - budget-conscious demographic
 */
public class EconomicConstraintModel {
    
    private final EconomicConfig config;
    private final VirtualWallet wallet;
    private final HesitationLoop hesitationLoop;
    private final CartPersistenceHook cartPersistence;
    private final ImpulseBuyingModel impulseBuying;
    private final RefundProbabilityModel refundModel;
    
    public EconomicConstraintModel() {
        this(EconomicConfig.samsungA12Profile());
    }
    
    public EconomicConstraintModel(EconomicConfig config) {
        this.config = config;
        this.wallet = new VirtualWallet(config);
        this.hesitationLoop = new HesitationLoop(config);
        this.cartPersistence = new CartPersistenceHook(config);
        this.impulseBuying = new ImpulseBuyingModel(config);
        this.refundModel = new RefundProbabilityModel(config);
    }
    
    /**
     * Pre-process a transaction request through all economic filters
     * 
     * @param userId User identifier
     * @param amount Transaction amount
     * @param category Product category
     * @return TransactionInterceptResult with all analysis
     */
    public TransactionInterceptResult preProcessTransaction(String userId, double amount, String category) {
        TransactionInterceptResult result = new TransactionInterceptResult();
        result.userId = userId;
        result.amount = amount;
        result.category = category;
        
        // Check 1: Budget limitation (VirtualWallet)
        VirtualWallet.TransactionResult walletResult = wallet.canAfford(amount, userId);
        result.walletAllowed = walletResult.canProceed();
        result.walletType = walletResult.getType();
        result.remainingBudget = walletResult.getRemainingBudget();
        
        if (!walletResult.canProceed()) {
            result.blocked = true;
            result.blockReason = "budget_exhausted";
            result.blockDescription = walletResult.getReason();
            return result;
        }
        
        // Check 2: Hesitation loop (if amount significant)
        if (hesitationLoop.shouldHesitate(amount)) {
            result.requiresHesitation = true;
            result.hesitationDelay = hesitationLoop.calculateHesitationDelay(amount);
            
            // Process hesitation
            HesitationLoop.HesitationResult hesitationResult = hesitationLoop.completeHesitation();
            result.hesitationOutcome = hesitationResult.getOutcome();
            
            if (hesitationResult.isAbandoned()) {
                result.blocked = true;
                result.blockReason = "hesitation_abandonment";
                result.blockDescription = "User abandoned cart during hesitation";
                return result;
            }
            
            if (hesitationResult.shouldRetry()) {
                result.retryDelay = hesitationResult.getRetryDelay();
            }
        }
        
        // Check 3: Impulse buying opportunity
        ImpulseBuyingModel.LimitedTimeOffer offer = impulseBuying.createDefaultOffer(
            "AUTO-" + UUID.randomUUID().toString().substring(0, 8)
        );
        impulseBuying.evaluateImpulsePurchase(userId, offer, amount);
        
        // Check for impulse from browsing
        ImpulseBuyingModel.BrowsingImpulseResult browsingResult = 
            impulseBuying.evaluateBrowsingImpulse(userId, category, amount);
        
        if (browsingResult.triggersImpulse()) {
            result.impulseTriggered = true;
            result.impulseAmount = browsingResult.getImpulseAmount();
        }
        
        result.processed = true;
        return result;
    }
    
    /**
     * Complete a transaction after all checks pass
     * 
     * @param userId User identifier
     * @param amount Transaction amount
     * @param category Product category
     * @param wasImpulse Whether this was an impulse purchase
     * @return TransactionCompletionResult
     */
    public TransactionCompletionResult completeTransaction(String userId, double amount, 
                                                            String category, boolean wasImpulse) {
        String transactionId = "TXN-" + System.currentTimeMillis() + "-" + 
            UUID.randomUUID().toString().substring(0, 8);
        
        // Record in wallet
        wallet.recordTransaction(amount, userId, transactionId);
        
        // Record for refund assessment
        refundModel.recordPurchase(userId, transactionId, amount, 1, category, wasImpulse);
        
        // Check immediate refund likelihood (simulates buyer's remorse kicked in)
        RefundProbabilityModel.RefundAssessment refundAssessment = 
            refundModel.assessRefundLikelihood(transactionId);
        
        return new TransactionCompletionResult(
            transactionId,
            amount,
            refundAssessment.willRefund(),
            refundAssessment.getDescription()
        );
    }
    
    /**
     * Add item to cart with delayed purchase intent
     * 
     * @param userId User identifier
     * @param itemId Item identifier
     * @param itemPrice Item price
     * @param itemName Item name
     */
    public void addToCart(String userId, String itemId, double itemPrice, String itemName) {
        cartPersistence.addToCart(userId, itemId, itemPrice, itemName);
    }
    
    /**
     * Simulate user returning to complete cart
     * 
     * @param userId User identifier
     * @return CartCompletionResult
     */
    public CartCompletionResult completeCart(String userId) {
        CartPersistenceHook.CompletionResult result = cartPersistence.simulateCompletion(userId);
        
        if (result.isCompleted()) {
            // Get cart total and record transactions
            CartPersistenceHook.CartStatus status = cartPersistence.getCartStatus(userId);
            
            VirtualWallet.TransactionResult walletResult = wallet.canAfford(
                status.getTotalValue(), userId
            );
            
            if (walletResult.canProceed()) {
                String transactionId = "CART-" + System.currentTimeMillis();
                wallet.recordTransaction(status.getTotalValue(), userId, transactionId);
                
                refundModel.recordPurchase(
                    userId, transactionId, status.getTotalValue(),
                    status.getItemCount(), "cart", false
                );
                
                cartPersistence.clearCart(userId);
                
                return new CartCompletionResult(true, status.getTotalValue(), 
                    "Cart completed successfully");
            } else {
                return new CartCompletionResult(false, 0, 
                    "Insufficient budget for cart total");
            }
        }
        
        return new CartCompletionResult(false, 0, result.getDescription());
    }
    
    /**
     * Process pending refunds (call periodically)
     */
    public void processPendingRefunds() {
        refundModel.processPendingRefunds();
    }
    
    /**
     * Get comprehensive user financial behavior profile
     * 
     * @param userId User identifier
     * @return UserFinancialProfile
     */
    public UserFinancialProfile getUserProfile(String userId) {
        return new UserFinancialProfile(
            wallet.getRemainingBudget(userId),
            wallet.getDailySpendingTotal(userId),
            wallet.getWindowShoppingBounces(userId),
            wallet.getSpendingVelocity(userId),
            cartPersistence.getCartStatus(userId),
            impulseBuying.getUserStatistics(userId),
            refundModel.getUserRemorseStats(userId)
        );
    }
    
    /**
     * Get configuration being used
     */
    public EconomicConfig getConfig() {
        return config;
    }
    
    // Result classes
    
    public static class TransactionInterceptResult {
        public String userId;
        public double amount;
        public String category;
        
        // Wallet results
        public boolean walletAllowed;
        public VirtualWallet.TransactionType walletType;
        public double remainingBudget;
        
        // Hesitation results
        public boolean requiresHesitation;
        public int hesitationDelay;
        public HesitationLoop.HesitationOutcome hesitationOutcome;
        public int retryDelay;
        
        // Impulse results
        public boolean impulseTriggered;
        public double impulseAmount;
        
        // Final decision
        public boolean blocked;
        public String blockReason;
        public String blockDescription;
        public boolean processed;
    }
    
    public static class TransactionCompletionResult {
        private final String transactionId;
        private final double amount;
        private final boolean refundLikely;
        private final String description;
        
        public TransactionCompletionResult(String transactionId, double amount,
                                           boolean refundLikely, String description) {
            this.transactionId = transactionId;
            this.amount = amount;
            this.refundLikely = refundLikely;
            this.description = description;
        }
        
        public String getTransactionId() { return transactionId; }
        public double getAmount() { return amount; }
        public boolean isRefundLikely() { return refundLikely; }
        public String getDescription() { return description; }
    }
    
    public static class CartCompletionResult {
        private final boolean success;
        private final double amount;
        private final String description;
        
        public CartCompletionResult(boolean success, double amount, String description) {
            this.success = success;
            this.amount = amount;
            this.description = description;
        }
        
        public boolean isSuccess() { return success; }
        public double getAmount() { return amount; }
        public String getDescription() { return description; }
    }
    
    public static class UserFinancialProfile {
        private final double remainingBudget;
        private final double dailySpent;
        private final int windowShoppingCount;
        private final double spendingVelocity;
        private final CartPersistenceHook.CartStatus cartStatus;
        private final ImpulseBuyingModel.ImpulseStatistics impulseStats;
        private final RefundProbabilityModel.UserRemorseStats remorseStats;
        
        public UserFinancialProfile(double remainingBudget, double dailySpent,
                                    int windowShoppingCount, double spendingVelocity,
                                    CartPersistenceHook.CartStatus cartStatus,
                                    ImpulseBuyingModel.ImpulseStatistics impulseStats,
                                    RefundProbabilityModel.UserRemorseStats remorseStats) {
            this.remainingBudget = remainingBudget;
            this.dailySpent = dailySpent;
            this.windowShoppingCount = windowShoppingCount;
            this.spendingVelocity = spendingVelocity;
            this.cartStatus = cartStatus;
            this.impulseStats = impulseStats;
            this.remorseStats = remorseStats;
        }
        
        public double getRemainingBudget() { return remainingBudget; }
        public double getDailySpent() { return dailySpent; }
        public int getWindowShoppingCount() { return windowShoppingCount; }
        public double getSpendingVelocity() { return spendingVelocity; }
        public CartPersistenceHook.CartStatus getCartStatus() { return cartStatus; }
        public ImpulseBuyingModel.ImpulseStatistics getImpulseStats() { return impulseStats; }
        public RefundProbabilityModel.UserRemorseStats getRemorseStats() { return remorseStats; }
    }
}
