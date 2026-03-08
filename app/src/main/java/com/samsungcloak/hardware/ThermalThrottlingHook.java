package com.samsungcloak.hardware;

import java.util.Random;

/**
 * Thermal Throttling Simulation Hook for Samsung Galaxy A12 (SM-A125U)
 * MediaTek Helio P35 (MT6765) chipset thermal characteristics.
 * 
 * Simulates realistic UI latency and task execution time degradation
 * as CPU temperature rises during high-intensity sessions.
 */
public class ThermalThrottlingHook {
    
    // SM-A125U Thermal Constants (based on MediaTek Helio P35 characteristics)
    private static final double BASE_TEMPERATURE_C = 25.0;
    private static final double THERMAL_THROTTLE_THRESHOLD_C = 42.0;
    private static final double CRITICAL_TEMPERATURE_C = 48.0;
    private static final double MAX_TEMPERATURE_C = 55.0;
    
    // Performance degradation factors
    private static final double BASE_UI_LATENCY_MS = 16.0;
    private static final double BASE_TASK_EXECUTION_MS = 1.0;
    private static final double MAX_LATENCY_MULTIPLIER = 4.5;
    private static final double MAX_TASK_MULTIPLIER = 3.2;
    
    // Thermal coefficients
    private static final double PASSIVE_COOLING_RATE = 0.8; // °C per minute
    private static final double ACTIVE_COOLING_RATE = 2.5; // °C per minute (fan or reduced load)
    private static final double HEAT_GENERATION_HIGH = 3.5; // °C per minute (gaming/video)
    private static final double HEAT_GENERATION_MEDIUM = 1.8; // °C per minute (browsing/social)
    private static final double HEAT_GENERATION_LOW = 0.5; // °C per minute (idle/light)
    
    private final Random random;
    private double currentTemperatureC;
    private long lastUpdateTimeMs;
    private boolean isThrottlingActive;
    private int consecutiveThrottleMinutes;
    
    public ThermalThrottlingHook() {
        this.random = new Random();
        this.currentTemperatureC = BASE_TEMPERATURE_C;
        this.lastUpdateTimeMs = System.currentTimeMillis();
        this.isThrottlingActive = false;
        this.consecutiveThrottleMinutes = 0;
    }
    
    /**
     * Simulate thermal state update based on workload intensity.
     * 
     * @param workloadIntensity 0.0 (idle) to 1.0 (maximum load)
     * @return Updated temperature in Celsius
     */
    public double updateThermalState(double workloadIntensity) {
        long currentTimeMs = System.currentTimeMillis();
        long timeDeltaMs = currentTimeMs - lastUpdateTimeMs;
        double timeDeltaMinutes = timeDeltaMs / 60000.0;
        
        if (timeDeltaMinutes < 0.01) {
            return currentTemperatureC;
        }
        
        // Calculate heat generation based on workload
        double heatGeneration;
        if (workloadIntensity > 0.7) {
            heatGeneration = HEAT_GENERATION_HIGH * workloadIntensity;
        } else if (workloadIntensity > 0.3) {
            heatGeneration = HEAT_GENERATION_MEDIUM * workloadIntensity;
        } else {
            heatGeneration = HEAT_GENERATION_LOW * workloadIntensity;
        }
        
        // Add thermal noise (real-world fluctuations)
        double thermalNoise = (random.nextDouble() - 0.5) * 0.3;
        
        // Determine cooling rate
        double coolingRate = isThrottlingActive ? 
            (PASSIVE_COOLING_RATE + random.nextDouble() * 0.5) : 
            (PASSIVE_COOLING_RATE * 0.6);
        
        // Update temperature
        double temperatureChange = (heatGeneration * timeDeltaMinutes) - 
                                  (coolingRate * timeDeltaMinutes) + 
                                  thermalNoise;
        
        currentTemperatureC += temperatureChange;
        
        // Clamp temperature to physical limits
        currentTemperatureC = Math.max(BASE_TEMPERATURE_C, 
                                      Math.min(MAX_TEMPERATURE_C, currentTemperatureC));
        
        // Update throttling state
        updateThrottlingState(timeDeltaMinutes);
        
        lastUpdateTimeMs = currentTimeMs;
        
        return currentTemperatureC;
    }
    
    private void updateThrottlingState(double timeDeltaMinutes) {
        if (currentTemperatureC >= THERMAL_THROTTLE_THRESHOLD_C) {
            isThrottlingActive = true;
            consecutiveThrottleMinutes += timeDeltaMinutes;
        } else if (currentTemperatureC < THERMAL_THROTTLE_THRESHOLD_C - 2.0) {
            isThrottlingActive = false;
            consecutiveThrottleMinutes = 0;
        }
    }
    
    /**
     * Calculate current UI latency including thermal throttling effects.
     * 
     * @return Latency in milliseconds
     */
    public double calculateUILatency() {
        double thermalFactor = calculateThermalFactor();
        
        // Add stochastic jitter typical of thermal conditions
        double jitter = random.nextDouble() * (thermalFactor * 5.0);
        
        return BASE_UI_LATENCY_MS * thermalFactor + jitter;
    }
    
    /**
     * Calculate task execution time with thermal degradation.
     * 
     * @param baseExecutionTimeMs Base execution time without thermal effects
     * @return Adjusted execution time in milliseconds
     */
    public double calculateTaskExecutionTime(double baseExecutionTimeMs) {
        double thermalFactor = calculateThermalFactor();
        
        // CPU frequency scaling is non-linear under thermal stress
        double cpuFrequencyFactor = 1.0 / thermalFactor;
        
        return baseExecutionTimeMs * cpuFrequencyFactor;
    }
    
    /**
     * Calculate thermal throttling factor (1.0 = no throttling, >1.0 = throttling).
     * Uses a sigmoid function to model gradual performance degradation.
     */
    private double calculateThermalFactor() {
        if (currentTemperatureC < THERMAL_THROTTLE_THRESHOLD_C) {
            return 1.0;
        }
        
        // Normalize temperature between threshold and max
        double normalizedTemp = (currentTemperatureC - THERMAL_THROTTLE_THRESHOLD_C) / 
                               (MAX_TEMPERATURE_C - THERMAL_THROTTLE_THRESHOLD_C);
        
        // Apply sigmoid for realistic gradual throttling
        double sigmoid = 1.0 / (1.0 + Math.exp(-8.0 * (normalizedTemp - 0.3)));
        
        // Calculate multiplier based on severity
        double multiplier;
        if (currentTemperatureC < CRITICAL_TEMPERATURE_C) {
            multiplier = 1.0 + (MAX_LATENCY_MULTIPLIER - 1.0) * sigmoid * 0.5;
        } else {
            multiplier = 1.0 + (MAX_LATENCY_MULTIPLIER - 1.0) * sigmoid;
        }
        
        // Add consecutive throttle penalty (thermal inertia)
        if (consecutiveThrottleMinutes > 5.0) {
            double inertiaPenalty = Math.min(0.3, consecutiveThrottleMinutes / 60.0);
            multiplier += inertiaPenalty;
        }
        
        return multiplier;
    }
    
    /**
     * Check if device is in critical thermal state.
     */
    public boolean isCriticalThermalState() {
        return currentTemperatureC >= CRITICAL_TEMPERATURE_C;
    }
    
    /**
     * Get current CPU temperature in Celsius.
     */
    public double getCurrentTemperatureC() {
        return currentTemperatureC;
    }
    
    /**
     * Get thermal throttling status.
     */
    public boolean isThrottlingActive() {
        return isThrottlingActive;
    }
    
    /**
     * Simulate rapid cooling (e.g., user stops intensive task, device idle).
     */
    public void forceCoolDown() {
        currentTemperatureC = BASE_TEMPERATURE_C + 
                             (currentTemperatureC - BASE_TEMPERATURE_C) * 0.3;
        isThrottlingActive = false;
        consecutiveThrottleMinutes = 0;
        lastUpdateTimeMs = System.currentTimeMillis();
    }
    
    /**
     * Get thermal state for telemetry logging.
     */
    public ThermalState getThermalState() {
        return new ThermalState(
            currentTemperatureC,
            isThrottlingActive,
            consecutiveThrottleMinutes,
            calculateUILatency(),
            calculateThermalFactor()
        );
    }
    
    /**
     * Data class for thermal state telemetry.
     */
    public static class ThermalState {
        public final double temperatureC;
        public final boolean isThrottling;
        public final double throttleDurationMinutes;
        public final double currentUILatencyMs;
        public final double thermalFactor;
        
        public ThermalState(double temperatureC, boolean isThrottling, 
                          double throttleDurationMinutes, double currentUILatencyMs,
                          double thermalFactor) {
            this.temperatureC = temperatureC;
            this.isThrottling = isThrottling;
            this.throttleDurationMinutes = throttleDurationMinutes;
            this.currentUILatencyMs = currentUILatencyMs;
            this.thermalFactor = thermalFactor;
        }
    }
}
