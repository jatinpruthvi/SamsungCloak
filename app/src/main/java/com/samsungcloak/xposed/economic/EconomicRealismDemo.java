package com.samsungcloak.xposed.economic;

/**
 * Demo application for Economic Constraint Model
 * 
 * Demonstrates all five behavioral realism hooks:
 * 1. Budget Limitation (VirtualWallet)
 * 2. Risk Aversion / Hesitation Loops
 * 3. Delayed Purchase Decisions (Cart Persistence)
 * 4. Impulse Buying Variance
 * 5. Refund Probability (Buyer's Remorse)
 * 
 * Tests payment gateway resilience on Samsung A12 profile
 */
public class EconomicRealismDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Economic Constraint Model Demo ===");
        System.out.println("Target: Samsung Galaxy A12 (SM-A125U)\n");
        
        // Initialize with Samsung A12 profile
        EconomicConstraintModel model = new EconomicConstraintModel(
            EconomicConfig.samsungA12Profile()
        );
        
        String userId = "user_samsung_a12_" + System.currentTimeMillis();
        
        // Demo 1: Budget Limitation
        System.out.println("--- Demo 1: Budget Limitation (Virtual Wallet) ---");
        demonstrateBudgetLimitation(model, userId);
        
        // Demo 2: Hesitation Loops
        System.out.println("\n--- Demo 2: Hesitation Loops ---");
        demonstrateHesitation(model, userId);
        
        // Demo 3: Cart Persistence
        System.out.println("\n--- Demo 3: Delayed Purchase (Cart Persistence) ---");
        demonstrateCartPersistence(model, userId);
        
        // Demo 4: Impulse Buying
        System.out.println("\n--- Demo 4: Impulse Buying ---");
        demonstrateImpulseBuying(model, userId);
        
        // Demo 5: Refund Probability
        System.out.println("\n--- Demo 5: Refund Probability (Buyer's Remorse) ---");
        demonstrateRefundProbability(model, userId);
        
        // Full user profile
        System.out.println("\n=== User Financial Profile ===");
        printUserProfile(model, userId);
        
        System.out.println("\n=== Demo Complete ===");
    }
    
    private static void demonstrateBudgetLimitation(EconomicConstraintModel model, String userId) {
        // Try multiple transactions to test budget limits
        double[] amounts = {5.00, 10.00, 15.00, 50.00, 25.00};
        
        for (double amount : amounts) {
            EconomicConstraintModel.TransactionInterceptResult result = 
                model.preProcessTransaction(userId, amount, "electronics");
            
            System.out.printf("Transaction $%.2f: ", amount);
            if (result.blocked) {
                System.out.printf("BLOCKED - %s (%s)%n", 
                    result.blockReason, result.blockDescription);
            } else {
                System.out.printf("Allowed (remaining: $%.2f)%n", result.remainingBudget);
                
                EconomicConstraintModel.TransactionCompletionResult completion = 
                    model.completeTransaction(userId, amount, "electronics", false);
                System.out.printf("  -> Completed: %s%n", completion.getTransactionId());
            }
        }
        
        double remaining = model.getConfig().getDailySpendingLimit() - 
            model.getUserProfile(userId).getDailySpent();
        System.out.printf("End of day remaining: $%.2f%n", remaining);
    }
    
    private static void demonstrateHesitation(EconomicConstraintModel model, String userId) {
        // Test with amounts that trigger hesitation
        double[] amounts = {15.00, 25.00, 35.00};
        
        for (double amount : amounts) {
            System.out.printf("Testing hesitation for $%.2f purchase:%n", amount);
            
            // First check
            EconomicConstraintModel.TransactionInterceptResult result = 
                model.preProcessTransaction(userId, amount, "fashion");
            
            if (result.requiresHesitation) {
                System.out.printf("  -> Hesitation triggered: %dms delay%n", result.hesitationDelay);
                System.out.printf("  -> Outcome: %s%n", result.hesitationOutcome);
                
                if (result.hesitationOutcome == HesitationLoop.HesitationOutcome.PROCEED) {
                    model.completeTransaction(userId, amount, "fashion", false);
                    System.out.println("  -> Transaction proceeded after hesitation");
                } else if (result.hesitationOutcome == HesitationLoop.HesitationOutcome.ABANDON_CART) {
                    System.out.println("  -> Cart abandoned during hesitation!");
                }
            } else {
                System.out.println("  -> No hesitation needed");
            }
        }
    }
    
    private static void demonstrateCartPersistence(EconomicConstraintModel model, String userId) {
        // Add items to cart
        String[] items = {"Wireless Earbuds", "Phone Case", "Screen Protector"};
        double[] prices = {29.99, 12.99, 8.99};
        
        System.out.println("Adding items to cart:");
        for (int i = 0; i < items.length; i++) {
            model.addToCart(userId, "ITEM-" + i, prices[i], items[i]);
            System.out.printf("  -> Added %s: $%.2f%n", items[i], prices[i]);
        }
        
        // Check cart status
        CartPersistenceHook.CartStatus status = 
            model.getUserProfile(userId).getCartStatus();
        System.out.printf("Cart status: %d items, $%.2f total%n", 
            status.getItemCount(), status.getTotalValue());
        
        // Simulate returning to complete cart later
        System.out.println("\nSimulating return to complete cart...");
        CartPersistenceHook.DelayResult delayResult = 
            new CartPersistenceHook(model.getConfig()).calculateCompletionDelay(userId);
        System.out.printf("  -> Predicted delay: %d hours%n", delayResult.getDelayMs() / 3600000);
        System.out.printf("  -> Prediction: %s%n", delayResult.getType());
        
        // Complete the cart
        EconomicConstraintModel.CartCompletionResult result = model.completeCart(userId);
        System.out.printf("  -> Result: %s (%.2f)%n", 
            result.isSuccess() ? "SUCCESS" : "FAILED", result.getAmount());
    }
    
    private static void demonstrateImpulseBuying(EconomicConstraintModel model, String userId) {
        ImpulseBuyingModel impulseModel = new ImpulseBuyingModel(model.getConfig());
        
        // Create a limited time offer
        ImpulseBuyingModel.LimitedTimeOffer offer = impulseModel.createOffer(
            "FLASH-SALE-001",
            30.0, // 30% discount
            1800000, // 30 minutes
            5, // Only 5 left
            ImpulseBuyingModel.UrgencyLevel.HIGH
        );
        
        System.out.println("Limited Time Offer: 30% off, only 5 items left!");
        
        // Try multiple browsing sessions
        String[] categories = {"electronics", "fashion", "home", "electronics"};
        double[] prices = {49.99, 35.00, 25.00, 79.99};
        
        for (int i = 0; i < categories.length; i++) {
            // Simulate browsing
            ImpulseBuyingModel.BrowsingImpulseResult browsingResult = 
                impulseModel.evaluateBrowsingImpulse(userId, categories[i], prices[i]);
            
            System.out.printf("Browsing %s ($%.2f): ", categories[i], prices[i]);
            System.out.printf("impulse prob=%.2f%n", browsingResult.getProbability());
            
            // Evaluate with offer
            ImpulseBuyingModel.ImpulseResult impulseResult = 
                impulseModel.evaluateImpulsePurchase(userId, offer, prices[i]);
            
            if (impulseResult.isTriggered()) {
                System.out.printf("  -> IMPULSE TRIGGERED: $%.2f%n", impulseResult.getAmount());
            }
        }
        
        // Check high velocity behavior
        ImpulseBuyingModel.HighVelocityResult velocityResult = 
            impulseModel.checkHighVelocity(userId, 3600000); // Last hour
        System.out.printf("High velocity check: %s (%s)%n", 
            velocityResult.isHighVelocity() ? "YES" : "NO",
            velocityResult.getDescription());
    }
    
    private static void demonstrateRefundProbability(EconomicConstraintModel model, String userId) {
        RefundProbabilityModel refundModel = new RefundProbabilityModel(model.getConfig());
        
        // Record some purchases
        String[] transactions = {"TXN-001", "TXN-002", "TXN-003"};
        double[] amounts = {15.00, 45.00, 120.00};
        boolean[] impulseFlags = {false, true, true};
        
        System.out.println("Recording purchases and checking refund likelihood:");
        
        for (int i = 0; i < transactions.length; i++) {
            refundModel.recordPurchase(
                userId, transactions[i], amounts[i], 1, 
                "electronics", impulseFlags[i]
            );
            
            // Immediate refund assessment
            RefundProbabilityModel.RefundAssessment assessment = 
                refundModel.assessRefundLikelihood(transactions[i]);
            
            System.out.printf("  %s ($%.2f, impulse=%s): ", 
                transactions[i], amounts[i], impulseFlags[i]);
            System.out.printf("refund=%s (%s)%n", 
                assessment.willRefund() ? "YES" : "NO",
                assessment.getDescription());
        }
        
        // Get user remorse stats
        RefundProbabilityModel.UserRemorseStats stats = 
            refundModel.getUserRemorseStats(userId);
        System.out.printf("User remorse profile: %d refunds, $%.2f total, score=%.2f%n",
            stats.getTotalRefundRequests(), 
            stats.getTotalAmountRefunded(),
            stats.getRemorseScore());
    }
    
    private static void printUserProfile(EconomicConstraintModel model, String userId) {
        EconomicConstraintModel.UserFinancialProfile profile = 
            model.getUserProfile(userId);
        
        System.out.printf("Remaining Budget: $%.2f%n", profile.getRemainingBudget());
        System.out.printf("Daily Spent: $%.2f%n", profile.getDailySpent());
        System.out.printf("Window Shopping Bounces: %d%n", profile.getWindowShoppingCount());
        System.out.printf("Spending Velocity: %.2f transactions/hour%n", profile.getSpendingVelocity());
        
        if (profile.getCartStatus() != null) {
            CartPersistenceHook.CartStatus cart = profile.getCartStatus();
            System.out.printf("Cart: %d items, $%.2f%n", 
                cart.getItemCount(), cart.getTotalValue());
        }
        
        if (profile.getImpulseStats() != null) {
            ImpulseBuyingModel.ImpulseStatistics impulse = profile.getImpulseStats();
            System.out.printf("Impulse Purchases: %d total, $%.2f total, %d recent%n",
                impulse.getTotalPurchases(), 
                impulse.getTotalAmount(),
                impulse.getRecentPurchases());
        }
        
        if (profile.getRemorseStats() != null) {
            RefundProbabilityModel.UserRemorseStats remorse = profile.getRemorseStats();
            System.out.printf("Refund History: %d requests, $%.2f refunded, score=%.2f%n",
                remorse.getTotalRefundRequests(),
                remorse.getTotalAmountRefunded(),
                remorse.getRemorseScore());
        }
    }
}
