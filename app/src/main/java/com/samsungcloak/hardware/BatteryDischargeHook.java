package com.samsungcloak.hardware;

import java.util.Random;

/**
 * Battery Discharge Nonlinearity Hook for Samsung Galaxy A12 (SM-A125U)
 * 
 * Implements the Peukert Effect to simulate realistic battery drain where
 * higher CPU/Screen loads cause non-linear drops in percentage, affecting
 * background task scheduling.
 * 
 * SM-A125U Battery Specs: 5000 mAh Li-Po
 */
public class BatteryDischargeHook {
    
    // SM-A125U Battery Constants
    private static final double BATTERY_CAPACITY_MAH = 5000.0;
    private static final double NOMINAL_VOLTAGE = 3.85; // Li-Po nominal
    private static final double PEUKERT_EXPONENT = 1.15; // Realistic for Li-Po under load
    
    // Battery state ranges
    private static final double MIN_VOLTAGE = 3.3; // Cut-off voltage
    private static final double MAX_VOLTAGE = 4.4; // Fully charged
    
    // Power consumption profiles (mW)
    private static final double IDLE_POWER_MW = 15.0;
    private static final double SCREEN_ON_BASE_MW = 80.0;
    private static final double CPU_BASE_POWER_MW = 250.0;
    private static final double MAX_CPU_POWER_MW = 1800.0; // Peak under load
    private static final double MODEM_POWER_4G_MW = 400.0;
    private static final double MODEM_POWER_WIFI_MW = 120.0;
    
    // Battery degradation simulation
    private static final double CYCLE_DEGRADATION_RATE = 0.0002; // Per cycle
    private static final double TIME_DEGRADATION_RATE = 0.000001; // Per second
    
    private final Random random;
    private double currentBatteryPercentage;
    private double currentVoltage;
    private int chargeCycleCount;
    private double cumulativeDischargeMAh;
    private long lastUpdateTimeMs;
    
    public BatteryDischargeHook(double initialPercentage) {
        this.random = new Random();
        this.currentBatteryPercentage = Math.min(100.0, Math.max(0.0, initialPercentage));
        this.currentVoltage = calculateVoltageFromPercentage(currentBatteryPercentage);
        this.chargeCycleCount = 0;
        this.cumulativeDischargeMAh = 0.0;
        this.lastUpdateTimeMs = System.currentTimeMillis();
    }
    
    /**
     * Update battery state based on current power consumption.
     * 
     * @param cpuLoad 0.0 to 1.0 (0% to 100% CPU utilization)
     * @param screenBrightness 0.0 to 1.0 (off to max brightness)
     * @param networkActivity 0.0 (idle) to 1.0 (heavy data transfer)
     * @param isWiFiConnected true if using WiFi, false if using cellular
     * @return Updated battery percentage
     */
    public double updateBatteryState(double cpuLoad, double screenBrightness, 
                                    double networkActivity, boolean isWiFiConnected) {
        long currentTimeMs = System.currentTimeMillis();
        long timeDeltaMs = currentTimeMs - lastUpdateTimeMs;
        double timeDeltaHours = timeDeltaMs / 3600000.0;
        
        if (timeDeltaHours < 0.000001) {
            return currentBatteryPercentage;
        }
        
        // Calculate instantaneous power consumption
        double powerMW = calculatePowerConsumption(cpuLoad, screenBrightness, 
                                                   networkActivity, isWiFiConnected);
        
        // Apply Peukert Effect for non-linear discharge at high loads
        double adjustedCurrentMA = applyPeukertEffect(powerMW / NOMINAL_VOLTAGE * 1000.0);
        
        // Calculate discharge amount in mAh
        double dischargeMAh = adjustedCurrentMA * timeDeltaHours;
        
        // Apply battery degradation
        double degradationFactor = 1.0 - (chargeCycleCount * CYCLE_DEGRADATION_RATE);
        degradationFactor = Math.max(0.85, degradationFactor); // Cap at 15% degradation
        
        dischargeMAh /= degradationFactor;
        
        // Update cumulative discharge
        cumulativeDischargeMAh += dischargeMAh;
        
        // Calculate percentage drop
        double percentageDrop = (dischargeMAh / BATTERY_CAPACITY_MAH) * 100.0;
        
        // Add voltage sag under high load (non-linear percentage drop)
        double voltageSagFactor = calculateVoltageSagFactor(cpuLoad);
        percentageDrop *= voltageSagFactor;
        
        // Update battery percentage
        currentBatteryPercentage -= percentageDrop;
        
        // Clamp to valid range
        currentBatteryPercentage = Math.max(0.0, Math.min(100.0, currentBatteryPercentage));
        
        // Update voltage
        currentVoltage = calculateVoltageFromPercentage(currentBatteryPercentage);
        
        lastUpdateTimeMs = currentTimeMs;
        
        return currentBatteryPercentage;
    }
    
    /**
     * Calculate instantaneous power consumption based on system state.
     */
    private double calculatePowerConsumption(double cpuLoad, double screenBrightness,
                                            double networkActivity, boolean isWiFiConnected) {
        double totalPowerMW = IDLE_POWER_MW;
        
        // Screen power (brightness affects linearly)
        if (screenBrightness > 0.0) {
            totalPowerMW += SCREEN_ON_BASE_MW + (screenBrightness * 150.0);
        }
        
        // CPU power (non-linear with load due to frequency scaling)
        double cpuPower = CPU_BASE_POWER_MW + 
                         (Math.pow(cpuLoad, 1.2) * (MAX_CPU_POWER_MW - CPU_BASE_POWER_MW));
        totalPowerMW += cpuPower;
        
        // Network power
        double modemPower = isWiFiConnected ? MODEM_POWER_WIFI_MW : MODEM_POWER_4G_MW;
        totalPowerMW += modemPower * networkActivity;
        
        // Add random fluctuations (real-world variability)
        double fluctuation = (random.nextDouble() - 0.5) * (totalPowerMW * 0.05);
        totalPowerMW += fluctuation;
        
        return totalPowerMW;
    }
    
    /**
     * Apply Peukert Effect: higher discharge rates are less efficient.
     * 
     * Formula: I_actual = I_nominal × (I_nominal / I_rate)^k
     * where k is the Peukert exponent
     */
    private double applyPeukertEffect(double nominalCurrentMA) {
        // Rate at which capacity is specified (typically 0.2C for Li-Po)
        double rateCurrentMA = BATTERY_CAPACITY_MAH / 5.0;
        
        // Apply Peukert formula
        double adjustedCurrentMA = nominalCurrentMA * 
                                   Math.pow(nominalCurrentMA / rateCurrentMA, PEUKERT_EXPONENT - 1.0);
        
        // Prevent unrealistic values at very low currents
        return Math.max(nominalCurrentMA * 0.9, adjustedCurrentMA);
    }
    
    /**
     * Calculate voltage sag factor under high CPU load.
     * Battery voltage temporarily drops under load, causing faster percentage drain.
     */
    private double calculateVoltageSagFactor(double cpuLoad) {
        if (cpuLoad < 0.5) {
            return 1.0;
        }
        
        // Voltage sag increases with load and decreases with battery level
        double sagBase = 1.0 + (cpuLoad - 0.5) * 0.3;
        double lowBatteryMultiplier = 1.0 + ((100.0 - currentBatteryPercentage) / 100.0) * 0.5;
        
        return sagBase * lowBatteryMultiplier;
    }
    
    /**
     * Calculate battery voltage based on percentage (simplified discharge curve).
     */
    private double calculateVoltageFromPercentage(double percentage) {
        if (percentage <= 0.0) return MIN_VOLTAGE;
        if (percentage >= 100.0) return MAX_VOLTAGE;
        
        // Realistic Li-Po discharge curve approximation
        double normalized = percentage / 100.0;
        double voltage;
        
        if (normalized > 0.8) {
            // Flat region (80-100%)
            voltage = 4.2 - (1.0 - normalized) * 1.0;
        } else if (normalized > 0.2) {
            // Linear region (20-80%)
            voltage = 3.9 - (0.8 - normalized) * 2.0;
        } else {
            // Steep drop region (0-20%)
            voltage = 3.5 - (0.2 - normalized) * 1.0;
        }
        
        voltage = Math.max(MIN_VOLTAGE, Math.min(MAX_VOLTAGE, voltage));
        return voltage;
    }
    
    /**
     * Determine if background tasks should be scheduled based on battery state.
     * Higher CPU loads and lower battery levels reduce background task priority.
     * 
     * @param taskPriority 0.0 to 1.0 (low to high priority)
     * @return true if task should be scheduled
     */
    public boolean shouldScheduleBackgroundTask(double taskPriority) {
        // Low battery conservation mode
        if (currentBatteryPercentage < 15.0) {
            return taskPriority > 0.85;
        }
        
        // Medium battery conservation mode
        if (currentBatteryPercentage < 30.0) {
            return taskPriority > 0.6;
        }
        
        // Normal operation
        return taskPriority > 0.2;
    }
    
    /**
     * Get recommended task execution priority based on battery state.
     * 
     * @return Priority value (0.0 = lowest, 1.0 = highest)
     */
    public double getTaskPriorityMultiplier() {
        if (currentBatteryPercentage > 50.0) {
            return 1.0;
        } else if (currentBatteryPercentage > 20.0) {
            return 0.7;
        } else {
            return 0.4;
        }
    }
    
    /**
     * Simulate battery charging.
     */
    public void chargeBattery(double chargePercentage) {
        currentBatteryPercentage = Math.min(100.0, currentBatteryPercentage + chargePercentage);
        currentVoltage = calculateVoltageFromPercentage(currentBatteryPercentage);
        chargeCycleCount++;
    }
    
    /**
     * Get battery state for telemetry.
     */
    public BatteryState getBatteryState() {
        return new BatteryState(
            currentBatteryPercentage,
            currentVoltage,
            chargeCycleCount,
            cumulativeDischargeMAh,
            calculateEstimatedCapacityMAh()
        );
    }
    
    /**
     * Calculate current effective battery capacity considering degradation.
     */
    private double calculateEstimatedCapacityMAh() {
        double degradationFactor = 1.0 - (chargeCycleCount * CYCLE_DEGRADATION_RATE);
        degradationFactor = Math.max(0.85, degradationFactor);
        return BATTERY_CAPACITY_MAH * degradationFactor;
    }
    
    /**
     * Data class for battery state telemetry.
     */
    public static class BatteryState {
        public final double percentage;
        public final double voltage;
        public final int chargeCycleCount;
        public final double cumulativeDischargeMAh;
        public final double estimatedCapacityMAh;
        
        public BatteryState(double percentage, double voltage, int chargeCycleCount,
                          double cumulativeDischargeMAh, double estimatedCapacityMAh) {
            this.percentage = percentage;
            this.voltage = voltage;
            this.chargeCycleCount = chargeCycleCount;
            this.cumulativeDischargeMAh = cumulativeDischargeMAh;
            this.estimatedCapacityMAh = estimatedCapacityMAh;
        }
    }
}
