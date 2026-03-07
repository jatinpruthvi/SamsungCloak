package com.cognitive.testing.environmental;

/**
 * Configuration for Environmental Stress Model chaos testing.
 * Simulates real-world volatile conditions on Samsung Galaxy A12 (SM-A125U).
 */
public class EnvironmentalConfig {
    
    // Network configuration
    private final boolean enableNetworkInstability;
    private final float networkChangeProbability;
    private final int minNetworkLatencyMs;
    private final int maxNetworkLatencyMs;
    private final float networkFailureProbability;
    private final int networkRecoveryTimeMs;
    
    // Device interruption configuration
    private final boolean enableDeviceInterruptions;
    private final float interruptionProbability;
    private final int minInterruptionIntervalMs;
    private final int maxInterruptionIntervalMs;
    private final int interruptionDurationMs;
    
    // Battery constraint configuration
    private final boolean enableBatteryConstraints;
    private final int initialBatteryPercentage;
    private final float batteryDrainRatePerMinute;
    private final int powerSaveModeThreshold;
    private final float powerSaveModeInteractionModifier;
    
    // Notification distraction configuration
    private final boolean enableNotificationDistractions;
    private final float notificationProbability;
    private final int minNotificationIntervalMs;
    private final int maxNotificationIntervalMs;
    private final int notificationFocusLossDurationMs;
    
    // Context switching configuration
    private final boolean enableContextSwitching;
    private final float contextSwitchProbability;
    private final int minContextSwitchIntervalMs;
    private final int maxContextSwitchIntervalMs;
    private final int minContextSwitchDurationMs;
    private final int maxContextSwitchDurationMs;
    private final float appHoppingEntropy;
    
    private EnvironmentalConfig(Builder builder) {
        this.enableNetworkInstability = builder.enableNetworkInstability;
        this.networkChangeProbability = builder.networkChangeProbability;
        this.minNetworkLatencyMs = builder.minNetworkLatencyMs;
        this.maxNetworkLatencyMs = builder.maxNetworkLatencyMs;
        this.networkFailureProbability = builder.networkFailureProbability;
        this.networkRecoveryTimeMs = builder.networkRecoveryTimeMs;
        
        this.enableDeviceInterruptions = builder.enableDeviceInterruptions;
        this.interruptionProbability = builder.interruptionProbability;
        this.minInterruptionIntervalMs = builder.minInterruptionIntervalMs;
        this.maxInterruptionIntervalMs = builder.maxInterruptionIntervalMs;
        this.interruptionDurationMs = builder.interruptionDurationMs;
        
        this.enableBatteryConstraints = builder.enableBatteryConstraints;
        this.initialBatteryPercentage = builder.initialBatteryPercentage;
        this.batteryDrainRatePerMinute = builder.batteryDrainRatePerMinute;
        this.powerSaveModeThreshold = builder.powerSaveModeThreshold;
        this.powerSaveModeInteractionModifier = builder.powerSaveModeInteractionModifier;
        
        this.enableNotificationDistractions = builder.enableNotificationDistractions;
        this.notificationProbability = builder.notificationProbability;
        this.minNotificationIntervalMs = builder.minNotificationIntervalMs;
        this.maxNotificationIntervalMs = builder.maxNotificationIntervalMs;
        this.notificationFocusLossDurationMs = builder.notificationFocusLossDurationMs;
        
        this.enableContextSwitching = builder.enableContextSwitching;
        this.contextSwitchProbability = builder.contextSwitchProbability;
        this.minContextSwitchIntervalMs = builder.minContextSwitchIntervalMs;
        this.maxContextSwitchIntervalMs = builder.maxContextSwitchIntervalMs;
        this.minContextSwitchDurationMs = builder.minContextSwitchDurationMs;
        this.maxContextSwitchDurationMs = builder.maxContextSwitchDurationMs;
        this.appHoppingEntropy = builder.appHoppingEntropy;
    }
    
    public static EnvironmentalConfig defaults() {
        return new Builder().build();
    }
    
    public static EnvironmentalConfig highChaos() {
        return new Builder()
                .enableNetworkInstability(true)
                .networkChangeProbability(0.15f)
                .minNetworkLatencyMs(100)
                .maxNetworkLatencyMs(5000)
                .networkFailureProbability(0.08f)
                .networkRecoveryTimeMs(8000)
                
                .enableDeviceInterruptions(true)
                .interruptionProbability(0.12f)
                .minInterruptionIntervalMs(15000)
                .maxInterruptionIntervalMs(60000)
                .interruptionDurationMs(5000)
                
                .enableBatteryConstraints(true)
                .initialBatteryPercentage(30)
                .batteryDrainRatePerMinute(2)
                .powerSaveModeThreshold(15)
                .powerSaveModeInteractionModifier(0.4f)
                
                .enableNotificationDistractions(true)
                .notificationProbability(0.10f)
                .minNotificationIntervalMs(20000)
                .maxNotificationIntervalMs(90000)
                .notificationFocusLossDurationMs(8000)
                
                .enableContextSwitching(true)
                .contextSwitchProbability(0.18f)
                .minContextSwitchIntervalMs(10000)
                .maxContextSwitchIntervalMs(45000)
                .minContextSwitchDurationMs(5000)
                .maxContextSwitchDurationMs(20000)
                .appHoppingEntropy(0.7f)
                .build();
    }
    
    public static EnvironmentalConfig lowChaos() {
        return new Builder()
                .enableNetworkInstability(true)
                .networkChangeProbability(0.05f)
                .minNetworkLatencyMs(50)
                .maxNetworkLatencyMs(1000)
                .networkFailureProbability(0.02f)
                .networkRecoveryTimeMs(3000)
                
                .enableDeviceInterruptions(true)
                .interruptionProbability(0.04f)
                .minInterruptionIntervalMs(30000)
                .maxInterruptionIntervalMs(120000)
                .interruptionDurationMs(2000)
                
                .enableBatteryConstraints(true)
                .initialBatteryPercentage(70)
                .batteryDrainRatePerMinute(0.5f)
                .powerSaveModeThreshold(15)
                .powerSaveModeInteractionModifier(0.2f)
                
                .enableNotificationDistractions(true)
                .notificationProbability(0.03f)
                .minNotificationIntervalMs(45000)
                .maxNotificationIntervalMs(180000)
                .notificationFocusLossDurationMs(3000)
                
                .enableContextSwitching(true)
                .contextSwitchProbability(0.06f)
                .minContextSwitchIntervalMs(30000)
                .maxContextSwitchIntervalMs(90000)
                .minContextSwitchDurationMs(3000)
                .maxContextSwitchDurationMs(10000)
                .appHoppingEntropy(0.3f)
                .build();
    }
    
    public static EnvironmentalConfig galaxyA12Stress() {
        return new Builder()
                .enableNetworkInstability(true)
                .networkChangeProbability(0.12f)
                .minNetworkLatencyMs(150)
                .maxNetworkLatencyMs(4000)
                .networkFailureProbability(0.06f)
                .networkRecoveryTimeMs(6000)
                
                .enableDeviceInterruptions(true)
                .interruptionProbability(0.10f)
                .minInterruptionIntervalMs(20000)
                .maxInterruptionIntervalMs(75000)
                .interruptionDurationMs(4000)
                
                .enableBatteryConstraints(true)
                .initialBatteryPercentage(25)
                .batteryDrainRatePerMinute(1.5f)
                .powerSaveModeThreshold(15)
                .powerSaveModeInteractionModifier(0.5f)
                
                .enableNotificationDistractions(true)
                .notificationProbability(0.08f)
                .minNotificationIntervalMs(25000)
                .maxNotificationIntervalMs(80000)
                .notificationFocusLossDurationMs(6000)
                
                .enableContextSwitching(true)
                .contextSwitchProbability(0.15f)
                .minContextSwitchIntervalMs(15000)
                .maxContextSwitchIntervalMs(50000)
                .minContextSwitchDurationMs(4000)
                .maxContextSwitchDurationMs(15000)
                .appHoppingEntropy(0.6f)
                .build();
    }
    
    public boolean isEnableNetworkInstability() { return enableNetworkInstability; }
    public float getNetworkChangeProbability() { return networkChangeProbability; }
    public int getMinNetworkLatencyMs() { return minNetworkLatencyMs; }
    public int getMaxNetworkLatencyMs() { return maxNetworkLatencyMs; }
    public float getNetworkFailureProbability() { return networkFailureProbability; }
    public int getNetworkRecoveryTimeMs() { return networkRecoveryTimeMs; }
    
    public boolean isEnableDeviceInterruptions() { return enableDeviceInterruptions; }
    public float getInterruptionProbability() { return interruptionProbability; }
    public int getMinInterruptionIntervalMs() { return minInterruptionIntervalMs; }
    public int getMaxInterruptionIntervalMs() { return maxInterruptionIntervalMs; }
    public int getInterruptionDurationMs() { return interruptionDurationMs; }
    
    public boolean isEnableBatteryConstraints() { return enableBatteryConstraints; }
    public int getInitialBatteryPercentage() { return initialBatteryPercentage; }
    public float getBatteryDrainRatePerMinute() { return batteryDrainRatePerMinute; }
    public int getPowerSaveModeThreshold() { return powerSaveModeThreshold; }
    public float getPowerSaveModeInteractionModifier() { return powerSaveModeInteractionModifier; }
    
    public boolean isEnableNotificationDistractions() { return enableNotificationDistractions; }
    public float getNotificationProbability() { return notificationProbability; }
    public int getMinNotificationIntervalMs() { return minNotificationIntervalMs; }
    public int getMaxNotificationIntervalMs() { return maxNotificationIntervalMs; }
    public int getNotificationFocusLossDurationMs() { return notificationFocusLossDurationMs; }
    
    public boolean isEnableContextSwitching() { return enableContextSwitching; }
    public float getContextSwitchProbability() { return contextSwitchProbability; }
    public int getMinContextSwitchIntervalMs() { return minContextSwitchIntervalMs; }
    public int getMaxContextSwitchIntervalMs() { return maxContextSwitchIntervalMs; }
    public int getMinContextSwitchDurationMs() { return minContextSwitchDurationMs; }
    public int getMaxContextSwitchDurationMs() { return maxContextSwitchDurationMs; }
    public float getAppHoppingEntropy() { return appHoppingEntropy; }
    
    public static class Builder {
        private boolean enableNetworkInstability = true;
        private float networkChangeProbability = 0.10f;
        private int minNetworkLatencyMs = 100;
        private int maxNetworkLatencyMs = 3000;
        private float networkFailureProbability = 0.05f;
        private int networkRecoveryTimeMs = 5000;
        
        private boolean enableDeviceInterruptions = true;
        private float interruptionProbability = 0.08f;
        private int minInterruptionIntervalMs = 20000;
        private int maxInterruptionIntervalMs = 60000;
        private int interruptionDurationMs = 3000;
        
        private boolean enableBatteryConstraints = true;
        private int initialBatteryPercentage = 50;
        private float batteryDrainRatePerMinute = 1.0f;
        private int powerSaveModeThreshold = 15;
        private float powerSaveModeInteractionModifier = 0.3f;
        
        private boolean enableNotificationDistractions = true;
        private float notificationProbability = 0.06f;
        private int minNotificationIntervalMs = 30000;
        private int maxNotificationIntervalMs = 90000;
        private int notificationFocusLossDurationMs = 5000;
        
        private boolean enableContextSwitching = true;
        private float contextSwitchProbability = 0.12f;
        private int minContextSwitchIntervalMs = 15000;
        private int maxContextSwitchIntervalMs = 60000;
        private int minContextSwitchDurationMs = 4000;
        private int maxContextSwitchDurationMs = 12000;
        private float appHoppingEntropy = 0.5f;
        
        public Builder enableNetworkInstability(boolean enabled) {
            this.enableNetworkInstability = enabled;
            return this;
        }
        
        public Builder networkChangeProbability(float prob) {
            this.networkChangeProbability = prob;
            return this;
        }
        
        public Builder minNetworkLatencyMs(int ms) {
            this.minNetworkLatencyMs = ms;
            return this;
        }
        
        public Builder maxNetworkLatencyMs(int ms) {
            this.maxNetworkLatencyMs = ms;
            return this;
        }
        
        public Builder networkFailureProbability(float prob) {
            this.networkFailureProbability = prob;
            return this;
        }
        
        public Builder networkRecoveryTimeMs(int ms) {
            this.networkRecoveryTimeMs = ms;
            return this;
        }
        
        public Builder enableDeviceInterruptions(boolean enabled) {
            this.enableDeviceInterruptions = enabled;
            return this;
        }
        
        public Builder interruptionProbability(float prob) {
            this.interruptionProbability = prob;
            return this;
        }
        
        public Builder minInterruptionIntervalMs(int ms) {
            this.minInterruptionIntervalMs = ms;
            return this;
        }
        
        public Builder maxInterruptionIntervalMs(int ms) {
            this.maxInterruptionIntervalMs = ms;
            return this;
        }
        
        public Builder interruptionDurationMs(int ms) {
            this.interruptionDurationMs = ms;
            return this;
        }
        
        public Builder enableBatteryConstraints(boolean enabled) {
            this.enableBatteryConstraints = enabled;
            return this;
        }
        
        public Builder initialBatteryPercentage(int percent) {
            this.initialBatteryPercentage = percent;
            return this;
        }
        
        public Builder batteryDrainRatePerMinute(float rate) {
            this.batteryDrainRatePerMinute = rate;
            return this;
        }
        
        public Builder powerSaveModeThreshold(int threshold) {
            this.powerSaveModeThreshold = threshold;
            return this;
        }
        
        public Builder powerSaveModeInteractionModifier(float modifier) {
            this.powerSaveModeInteractionModifier = modifier;
            return this;
        }
        
        public Builder enableNotificationDistractions(boolean enabled) {
            this.enableNotificationDistractions = enabled;
            return this;
        }
        
        public Builder notificationProbability(float prob) {
            this.notificationProbability = prob;
            return this;
        }
        
        public Builder minNotificationIntervalMs(int ms) {
            this.minNotificationIntervalMs = ms;
            return this;
        }
        
        public Builder maxNotificationIntervalMs(int ms) {
            this.maxNotificationIntervalMs = ms;
            return this;
        }
        
        public Builder notificationFocusLossDurationMs(int ms) {
            this.notificationFocusLossDurationMs = ms;
            return this;
        }
        
        public Builder enableContextSwitching(boolean enabled) {
            this.enableContextSwitching = enabled;
            return this;
        }
        
        public Builder contextSwitchProbability(float prob) {
            this.contextSwitchProbability = prob;
            return this;
        }
        
        public Builder minContextSwitchIntervalMs(int ms) {
            this.minContextSwitchIntervalMs = ms;
            return this;
        }
        
        public Builder maxContextSwitchIntervalMs(int ms) {
            this.maxContextSwitchIntervalMs = ms;
            return this;
        }
        
        public Builder minContextSwitchDurationMs(int ms) {
            this.minContextSwitchDurationMs = ms;
            return this;
        }
        
        public Builder maxContextSwitchDurationMs(int ms) {
            this.maxContextSwitchDurationMs = ms;
            return this;
        }
        
        public Builder appHoppingEntropy(float entropy) {
            this.appHoppingEntropy = entropy;
            return this;
        }
        
        public EnvironmentalConfig build() {
            return new EnvironmentalConfig(this);
        }
    }
}
