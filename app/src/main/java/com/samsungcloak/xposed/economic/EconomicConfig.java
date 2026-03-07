package com.samsungcloak.xposed.economic;

import java.util.Random;

/**
 * Economic Constraint Model - Core Configuration
 * 
 * Simulates human financial constraints for payment gateway resilience testing.
 * Models real-world spending behaviors: budget limits, hesitation, delayed purchases,
 * impulse buying, and buyer's remorse.
 * 
 * Samsung Galaxy A12 (SM-A125U) target profile:
 * - Limited budget users
 * - Price-sensitive demographic
 * - Variable network conditions
 * - Moderate device performance
 */
public class EconomicConfig {
    
    // Virtual wallet configuration
    private final double dailySpendingLimit;
    private final double maxSingleTransactionLimit;
    private final boolean enableBudgetEnforcement;
    
    // Hesitation loop configuration
    private final int minHesitationMs;
    private final int maxHesitationMs;
    private final double hesitationThreshold;
    private final double hesitationIncreasePerAttempt;
    
    // Delayed purchase (cart persistence) configuration
    private final int minDelayHours;
    private final int maxDelayHours;
    private final double abandonmentProbability;
    
    // Impulse buying configuration
    private final double impulsePurchaseProbability;
    private final double limitedTimeOfferMultiplier;
    private final double highVelocityPurchaseThreshold;
    private final int impulseCooldownHours;
    
    // Refund/Buyer's remorse configuration
    private final double refundProbability;
    private final int refundWindowHours;
    private final double refundProbabilityIncreaseWithRegret;
    
    // Device-specific constraints (Samsung A12 profile)
    private final double typicalBudgetMultiplier;
    private final double impulseSpendingCap;
    
    // Random seed for reproducibility
    private final long randomSeed;
    private final Random random;
    
    private EconomicConfig(Builder builder) {
        this.dailySpendingLimit = builder.dailySpendingLimit;
        this.maxSingleTransactionLimit = builder.maxSingleTransactionLimit;
        this.enableBudgetEnforcement = builder.enableBudgetEnforcement;
        this.minHesitationMs = builder.minHesitationMs;
        this.maxHesitationMs = builder.maxHesitationMs;
        this.hesitationThreshold = builder.hesitationThreshold;
        this.hesitationIncreasePerAttempt = builder.hesitationIncreasePerAttempt;
        this.minDelayHours = builder.minDelayHours;
        this.maxDelayHours = builder.maxDelayHours;
        this.abandonmentProbability = builder.abandonmentProbability;
        this.impulsePurchaseProbability = builder.impulsePurchaseProbability;
        this.limitedTimeOfferMultiplier = builder.limitedTimeOfferMultiplier;
        this.highVelocityPurchaseThreshold = builder.highVelocityPurchaseThreshold;
        this.impulseCooldownHours = builder.impulseCooldownHours;
        this.refundProbability = builder.refundProbability;
        this.refundWindowHours = builder.refundWindowHours;
        this.refundProbabilityIncreaseWithRegret = builder.refundProbabilityIncreaseWithRegret;
        this.typicalBudgetMultiplier = builder.typicalBudgetMultiplier;
        this.impulseSpendingCap = builder.impulseSpendingCap;
        this.randomSeed = builder.randomSeed;
        this.random = builder.randomSeed > 0 ? new Random(builder.randomSeed) : new Random();
    }
    
    public static EconomicConfig defaults() {
        return new Builder().build();
    }
    
    public static EconomicConfig budgetSensitive() {
        return new Builder()
            .dailySpendingLimit(50.00)
            .maxSingleTransactionLimit(25.00)
            .hesitationThreshold(15.00)
            .impulsePurchaseProbability(0.15)
            .refundProbability(0.08)
            .build();
    }
    
    public static EconomicConfig premiumUser() {
        return new Builder()
            .dailySpendingLimit(200.00)
            .maxSingleTransactionLimit(100.00)
            .hesitationThreshold(50.00)
            .minHesitationMs(500)
            .maxHesitationMs(2000)
            .impulsePurchaseProbability(0.25)
            .refundProbability(0.12)
            .build();
    }
    
    public static EconomicConfig samsungA12Profile() {
        return new Builder()
            .dailySpendingLimit(35.00)
            .maxSingleTransactionLimit(20.00)
            .typicalBudgetMultiplier(0.7)
            .impulseSpendingCap(30.00)
            .hesitationThreshold(10.00)
            .hesitationIncreasePerAttempt(1.5)
            .minDelayHours(12)
            .maxDelayHours(48)
            .abandonmentProbability(0.35)
            .impulsePurchaseProbability(0.18)
            .limitedTimeOfferMultiplier(2.5)
            .refundProbability(0.06)
            .refundWindowHours(24)
            .build();
    }
    
    // Getters
    public double getDailySpendingLimit() { return dailySpendingLimit; }
    public double getMaxSingleTransactionLimit() { return maxSingleTransactionLimit; }
    public boolean isEnableBudgetEnforcement() { return enableBudgetEnforcement; }
    public int getMinHesitationMs() { return minHesitationMs; }
    public int getMaxHesitationMs() { return maxHesitationMs; }
    public double getHesitationThreshold() { return hesitationThreshold; }
    public double getHesitationIncreasePerAttempt() { return hesitationIncreasePerAttempt; }
    public int getMinDelayHours() { return minDelayHours; }
    public int getMaxDelayHours() { return maxDelayHours; }
    public double getAbandonmentProbability() { return abandonmentProbability; }
    public double getImpulsePurchaseProbability() { return impulsePurchaseProbability; }
    public double getLimitedTimeOfferMultiplier() { return limitedTimeOfferMultiplier; }
    public double getHighVelocityPurchaseThreshold() { return highVelocityPurchaseThreshold; }
    public int getImpulseCooldownHours() { return impulseCooldownHours; }
    public double getRefundProbability() { return refundProbability; }
    public int getRefundWindowHours() { return refundWindowHours; }
    public double getRefundProbabilityIncreaseWithRegret() { return refundProbabilityIncreaseWithRegret; }
    public double getTypicalBudgetMultiplier() { return typicalBudgetMultiplier; }
    public double getImpulseSpendingCap() { return impulseSpendingCap; }
    public long getRandomSeed() { return randomSeed; }
    public Random getRandom() { return random; }
    
    public static class Builder {
        private double dailySpendingLimit = 50.00;
        private double maxSingleTransactionLimit = 30.00;
        private boolean enableBudgetEnforcement = true;
        
        private int minHesitationMs = 200;
        private int maxHesitationMs = 1500;
        private double hesitationThreshold = 20.00;
        private double hesitationIncreasePerAttempt = 1.2;
        
        private int minDelayHours = 12;
        private int maxDelayHours = 48;
        private double abandonmentProbability = 0.30;
        
        private double impulsePurchaseProbability = 0.20;
        private double limitedTimeOfferMultiplier = 2.0;
        private double highVelocityPurchaseThreshold = 50.00;
        private int impulseCooldownHours = 6;
        
        private double refundProbability = 0.07;
        private int refundWindowHours = 24;
        private double refundProbabilityIncreaseWithRegret = 0.10;
        
        private double typicalBudgetMultiplier = 1.0;
        private double impulseSpendingCap = 50.00;
        
        private long randomSeed = -1;
        
        public Builder dailySpendingLimit(double limit) {
            this.dailySpendingLimit = limit;
            return this;
        }
        
        public Builder maxSingleTransactionLimit(double limit) {
            this.maxSingleTransactionLimit = limit;
            return this;
        }
        
        public Builder enableBudgetEnforcement(boolean enable) {
            this.enableBudgetEnforcement = enable;
            return this;
        }
        
        public Builder minHesitationMs(int ms) {
            this.minHesitationMs = ms;
            return this;
        }
        
        public Builder maxHesitationMs(int ms) {
            this.maxHesitationMs = ms;
            return this;
        }
        
        public Builder hesitationThreshold(double threshold) {
            this.hesitationThreshold = threshold;
            return this;
        }
        
        public Builder hesitationIncreasePerAttempt(double increase) {
            this.hesitationIncreasePerAttempt = increase;
            return this;
        }
        
        public Builder minDelayHours(int hours) {
            this.minDelayHours = hours;
            return this;
        }
        
        public Builder maxDelayHours(int hours) {
            this.maxDelayHours = hours;
            return this;
        }
        
        public Builder abandonmentProbability(double prob) {
            this.abandonmentProbability = prob;
            return this;
        }
        
        public Builder impulsePurchaseProbability(double prob) {
            this.impulsePurchaseProbability = prob;
            return this;
        }
        
        public Builder limitedTimeOfferMultiplier(double mult) {
            this.limitedTimeOfferMultiplier = mult;
            return this;
        }
        
        public Builder highVelocityPurchaseThreshold(double threshold) {
            this.highVelocityPurchaseThreshold = threshold;
            return this;
        }
        
        public Builder impulseCooldownHours(int hours) {
            this.impulseCooldownHours = hours;
            return this;
        }
        
        public Builder refundProbability(double prob) {
            this.refundProbability = prob;
            return this;
        }
        
        public Builder refundWindowHours(int hours) {
            this.refundWindowHours = hours;
            return this;
        }
        
        public Builder refundProbabilityIncreaseWithRegret(double increase) {
            this.refundProbabilityIncreaseWithRegret = increase;
            return this;
        }
        
        public Builder typicalBudgetMultiplier(double mult) {
            this.typicalBudgetMultiplier = mult;
            return this;
        }
        
        public Builder impulseSpendingCap(double cap) {
            this.impulseSpendingCap = cap;
            return this;
        }
        
        public Builder randomSeed(long seed) {
            this.randomSeed = seed;
            return this;
        }
        
        public EconomicConfig build() {
            return new EconomicConfig(this);
        }
    }
}
