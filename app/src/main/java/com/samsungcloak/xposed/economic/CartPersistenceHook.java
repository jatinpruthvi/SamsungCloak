package com.samsungcloak.xposed.economic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cart Persistence Hook - Delayed Purchase Decisions
 * 
 * Models user behavior of adding items to cart but not completing purchase immediately.
 * Tests session recovery, cart persistence, and re-engagement logic.
 * 
 * Real-world behavior:
 * - Users add items and leave
 * - Return hours or days later to complete
 * - Some items never purchased (cart abandonment)
 * - Price changes can trigger completion or abandonment
 * - Notification reminders influence completion rate
 * 
 * Tests: Session recovery, cart persistence storage, re-engagement campaigns,
 *        cart analytics, timeout handling, price change detection
 */
public class CartPersistenceHook {
    
    private final EconomicConfig config;
    private final Random random;
    
    // Cart storage - keyed by userId
    private final Map<String, UserCart> userCarts;
    
    public CartPersistenceHook(EconomicConfig config) {
        this.config = config;
        this.random = config.getRandom();
        this.userCarts = new ConcurrentHashMap<>();
    }
    
    /**
     * Add item to cart with delayed purchase intent
     * 
     * @param userId User identifier
     * @param itemId Item identifier
     * @param itemPrice Item price
     * @param itemName Item name for reference
     * @return CartEntry representing the added item
     */
    public CartEntry addToCart(String userId, String itemId, double itemPrice, String itemName) {
        UserCart cart = userCarts.computeIfAbsent(userId, k -> new UserCart());
        
        CartEntry entry = new CartEntry(itemId, itemPrice, itemName, System.currentTimeMillis());
        cart.addItem(entry);
        
        return entry;
    }
    
    /**
     * Calculate when the cart will likely be completed or abandoned
     * 
     * @param userId User identifier
     * @return DelayResult with completion timing
     */
    public DelayResult calculateCompletionDelay(String userId) {
        UserCart cart = userCarts.get(userId);
        if (cart == null || cart.isEmpty()) {
            return new DelayResult(0, DelayType.EMPTY, "Cart is empty");
        }
        
        // Calculate base delay from config
        int baseDelayHours = config.getMinDelayHours() + 
            random.nextInt(config.getMaxDelayHours() - config.getMinDelayHours() + 1);
        
        // Adjust based on cart value
        double cartValue = cart.getTotalValue();
        if (cartValue > 50) {
            // Higher value = longer consideration
            baseDelayHours += random.nextInt(12);
        } else if (cartValue < 10) {
            // Lower value = quicker decision
            baseDelayHours -= random.nextInt(6);
        }
        
        // Calculate probability of completion vs abandonment
        double completionProbability = calculateCompletionProbability(cart);
        
        // Determine outcome type
        DelayType type;
        if (random.nextDouble() < completionProbability) {
            type = DelayType.WILL_COMPLETE;
        } else {
            type = DelayType.WILL_ABANDON;
        }
        
        // Add some randomness to timing
        long delayMs = (baseDelayHours * 3600000L) + (random.nextInt(3600000));
        
        return new DelayResult(delayMs, type, 
            String.format("Cart value: $%.2f, completion probability: %.2f", 
                cartValue, completionProbability));
    }
    
    /**
     * Check if a cart should be auto-abandoned (session timeout simulation)
     * 
     * @param userId User identifier
     * @param currentTime Current timestamp
     * @return AbandonmentResult
     */
    public AbandonmentResult checkAbandonment(String userId, long currentTime) {
        UserCart cart = userCarts.get(userId);
        if (cart == null || cart.isEmpty()) {
            return new AbandonmentResult(false, "Cart empty");
        }
        
        // Check if any item has exceeded max delay
        long maxDelayMs = config.getMaxDelayHours() * 3600000L;
        
        for (CartEntry entry : cart.getItems()) {
            long age = currentTime - entry.getAddedTime();
            
            if (age > maxDelayMs) {
                // Apply abandonment probability
                if (random.nextDouble() < config.getAbandonmentProbability()) {
                    cart.markAbandoned();
                    return new AbandonmentResult(true, 
                        String.format("Item %s exceeded max delay of %d hours", 
                            entry.getItemId(), config.getMaxDelayHours()));
                }
            }
        }
        
        // Random abandonment check for older carts
        for (CartEntry entry : cart.getItems()) {
            long age = currentTime - entry.getAddedTime();
            long hoursOld = age / 3600000;
            
            // 10% chance of abandonment per hour after 24 hours
            if (hoursOld > 24 && random.nextDouble() < (0.10 * (hoursOld - 24) / 24)) {
                cart.markAbandoned();
                return new AbandonmentResult(true, 
                    String.format("Random abandonment at %d hours", hoursOld));
            }
        }
        
        return new AbandonmentResult(false, "No abandonment detected");
    }
    
    /**
     * Simulate user returning to complete purchase
     * 
     * @param userId User identifier
     * @return CompletionResult
     */
    public CompletionResult simulateCompletion(String userId) {
        UserCart cart = userCarts.get(userId);
        if (cart == null || cart.isEmpty()) {
            return new CompletionResult(false, 0, "Cart empty or not found");
        }
        
        // Check if cart is still valid
        if (cart.isAbandoned()) {
            return new CompletionResult(false, 0, "Cart was abandoned");
        }
        
        double cartValue = cart.getTotalValue();
        
        // Apply completion logic with some probability of change
        double completionProbability = calculateCompletionProbability(cart);
        
        // Increase slightly on return (commitment bias)
        completionProbability += 0.15;
        
        boolean completed = random.nextDouble() < completionProbability;
        
        if (completed) {
            cart.markCompleted();
            return new CompletionResult(true, cartValue, 
                String.format("Purchase completed for $%.2f", cartValue));
        } else {
            // User browsed but didn't buy
            cart.incrementViewCount();
            return new CompletionResult(false, cartValue, "Browsed but did not complete");
        }
    }
    
    /**
     * Remove item from cart
     * 
     * @param userId User identifier
     * @param itemId Item to remove
     * @return true if removed
     */
    public boolean removeFromCart(String userId, String itemId) {
        UserCart cart = userCarts.get(userId);
        if (cart == null) return false;
        return cart.removeItem(itemId);
    }
    
    /**
     * Get cart status for analytics
     * 
     * @param userId User identifier
     * @return CartStatus
     */
    public CartStatus getCartStatus(String userId) {
        UserCart cart = userCarts.get(userId);
        if (cart == null) {
            return new CartStatus(0, 0, 0, true, false);
        }
        
        return new CartStatus(
            cart.getItemCount(),
            cart.getTotalValue(),
            cart.getViewCount(),
            cart.isEmpty(),
            cart.isAbandoned()
        );
    }
    
    /**
     * Apply price change to cart items and check impact
     * 
     * @param userId User identifier
     * @param itemId Item with price change
     * @param newPrice New price
     * @return PriceChangeImpact
     */
    public PriceChangeImpact applyPriceChange(String userId, String itemId, double newPrice) {
        UserCart cart = userCarts.get(userId);
        if (cart == null) {
            return new PriceChangeImpact(ImpactType.NO_CHANGE, "Cart not found");
        }
        
        CartEntry entry = cart.getItem(itemId);
        if (entry == null) {
            return new PriceChangeImpact(ImpactType.NO_CHANGE, "Item not in cart");
        }
        
        double oldPrice = entry.getPrice();
        double changePercent = ((newPrice - oldPrice) / oldPrice) * 100;
        
        ImpactType impact;
        String reason;
        
        if (changePercent < -10) {
            // Significant discount - increase completion likelihood
            impact = random.nextDouble() < 0.8 ? ImpactType.COMPLETION_LIKELY : ImpactType.NO_CHANGE;
            reason = String.format("Discount of %.1f%%", Math.abs(changePercent));
        } else if (changePercent > 10) {
            // Price increase - may trigger abandonment
            impact = random.nextDouble() < 0.6 ? ImpactType.ABANDONMENT_LIKELY : ImpactType.NO_CHANGE;
            reason = String.format("Price increase of %.1f%%", changePercent);
        } else {
            impact = ImpactType.NO_CHANGE;
            reason = "Minimal price change";
        }
        
        if (impact != ImpactType.NO_CHANGE) {
            entry.updatePrice(newPrice);
        }
        
        return new PriceChangeImpact(impact, reason);
    }
    
    /**
     * Clear cart after successful purchase
     */
    public void clearCart(String userId) {
        UserCart cart = userCarts.get(userId);
        if (cart != null) {
            cart.clear();
        }
    }
    
    private double calculateCompletionProbability(UserCart cart) {
        double probability = 1.0 - config.getAbandonmentProbability();
        
        // Adjust based on cart size
        int itemCount = cart.getItemCount();
        if (itemCount > 5) {
            probability -= 0.15; // More items = lower completion
        } else if (itemCount == 1) {
            probability += 0.1; // Single item = higher completion
        }
        
        // Adjust based on total value
        double value = cart.getTotalValue();
        if (value > 100) {
            probability -= 0.2; // High value = lower completion
        } else if (value < 20) {
            probability += 0.1; // Low value = higher completion
        }
        
        return Math.max(0.1, Math.min(0.95, probability));
    }
    
    // Inner classes
    
    public static class CartEntry {
        private final String itemId;
        private double price;
        private final String itemName;
        private final long addedTime;
        private int viewCount;
        
        public CartEntry(String itemId, double price, String itemName, long addedTime) {
            this.itemId = itemId;
            this.price = price;
            this.itemName = itemName;
            this.addedTime = addedTime;
            this.viewCount = 0;
        }
        
        public String getItemId() { return itemId; }
        public double getPrice() { return price; }
        public String getItemName() { return itemName; }
        public long getAddedTime() { return addedTime; }
        public int getViewCount() { return viewCount; }
        
        public void updatePrice(double newPrice) {
            this.price = newPrice;
        }
        
        public void incrementViewCount() {
            viewCount++;
        }
    }
    
    private static class UserCart {
        private final Map<String, CartEntry> items;
        private boolean abandoned;
        private boolean completed;
        private int viewCount;
        
        public UserCart() {
            this.items = new HashMap<>();
            this.abandoned = false;
            this.completed = false;
            this.viewCount = 0;
        }
        
        public void addItem(CartEntry entry) {
            items.put(entry.getItemId(), entry);
        }
        
        public boolean removeItem(String itemId) {
            return items.remove(itemId) != null;
        }
        
        public CartEntry getItem(String itemId) {
            return items.get(itemId);
        }
        
        public List<CartEntry> getItems() {
            return new ArrayList<>(items.values());
        }
        
        public int getItemCount() {
            return items.size();
        }
        
        public double getTotalValue() {
            return items.values().stream()
                .mapToDouble(CartEntry::getPrice)
                .sum();
        }
        
        public boolean isEmpty() {
            return items.isEmpty();
        }
        
        public boolean isAbandoned() { return abandoned; }
        public boolean isCompleted() { return completed; }
        public int getViewCount() { return viewCount; }
        
        public void markAbandoned() { this.abandoned = true; }
        public void markCompleted() { this.completed = true; }
        public void incrementViewCount() { this.viewCount++; }
        public void clear() { items.clear(); }
    }
    
    public enum DelayType {
        WILL_COMPLETE,
        WILL_ABANDON,
        EMPTY
    }
    
    public static class DelayResult {
        private final long delayMs;
        private final DelayType type;
        private final String description;
        
        public DelayResult(long delayMs, DelayType type, String description) {
            this.delayMs = delayMs;
            this.type = type;
            this.description = description;
        }
        
        public long getDelayMs() { return delayMs; }
        public DelayType getType() { return type; }
        public String getDescription() { return description; }
    }
    
    public static class AbandonmentResult {
        private final boolean abandoned;
        private final String reason;
        
        public AbandonmentResult(boolean abandoned, String reason) {
            this.abandoned = abandoned;
            this.reason = reason;
        }
        
        public boolean isAbandoned() { return abandoned; }
        public String getReason() { return reason; }
    }
    
    public static class CompletionResult {
        private final boolean completed;
        private final double cartValue;
        private final String description;
        
        public CompletionResult(boolean completed, double cartValue, String description) {
            this.completed = completed;
            this.cartValue = cartValue;
            this.description = description;
        }
        
        public boolean isCompleted() { return completed; }
        public double getCartValue() { return cartValue; }
        public String getDescription() { return description; }
    }
    
    public static class CartStatus {
        private final int itemCount;
        private final double totalValue;
        private final int viewCount;
        private final boolean empty;
        private final boolean abandoned;
        
        public CartStatus(int itemCount, double totalValue, int viewCount, 
                         boolean empty, boolean abandoned) {
            this.itemCount = itemCount;
            this.totalValue = totalValue;
            this.viewCount = viewCount;
            this.empty = empty;
            this.abandoned = abandoned;
        }
        
        public int getItemCount() { return itemCount; }
        public double getTotalValue() { return totalValue; }
        public int getViewCount() { return viewCount; }
        public boolean isEmpty() { return empty; }
        public boolean isAbandoned() { return abandoned; }
    }
    
    public enum ImpactType {
        COMPLETION_LIKELY,
        ABANDONMENT_LIKELY,
        NO_CHANGE
    }
    
    public static class PriceChangeImpact {
        private final ImpactType type;
        private final String reason;
        
        public PriceChangeImpact(ImpactType type, String reason) {
            this.type = type;
            this.reason = reason;
        }
        
        public ImpactType getType() { return type; }
        public String getReason() { return reason; }
    }
}
