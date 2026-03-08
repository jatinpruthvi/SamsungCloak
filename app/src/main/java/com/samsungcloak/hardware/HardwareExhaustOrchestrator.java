package com.samsungcloak.hardware;

/**
 * Hardware Exhaust & Environmental Fidelity Orchestrator for Samsung Galaxy A12 (SM-A125U)
 * 
 * Central coordinator for all hardware artifact hooks, providing a unified interface
 * for realistic hardware-in-the-loop (HIL) diagnostics and telemetry.
 * 
 * This orchestrator integrates:
 * - Thermal Throttling Simulation
 * - Battery Discharge Nonlinearity (Peukert Effect)
 * - Network Jitter & Packet Loss
 * - Storage I/O Degradation
 * - Sensor Floor Noise
 */
public class HardwareExhaustOrchestrator {
    
    private final ThermalThrottlingHook thermalHook;
    private final BatteryDischargeHook batteryHook;
    private final NetworkJitterHook networkHook;
    private final StorageIODegradationHook storageHook;
    private final SensorFloorNoiseHook sensorHook;
    
    // Simulation state
    private long simulationStartTimeMs;
    private long simulationDurationMs;
    private int currentDay;
    
    public HardwareExhaustOrchestrator(double initialBatteryPercentage) {
        this.thermalHook = new ThermalThrottlingHook();
        this.batteryHook = new BatteryDischargeHook(initialBatteryPercentage);
        this.networkHook = new NetworkJitterHook(NetworkJitterHook.NetworkType.LTE_GOOD);
        this.storageHook = new StorageIODegradationHook();
        this.sensorHook = new SensorFloorNoiseHook();
        
        this.simulationStartTimeMs = System.currentTimeMillis();
        this.simulationDurationMs = 0;
        this.currentDay = 1;
    }
    
    /**
     * Update all hardware states for a simulation tick.
     * 
     * @param cpuLoad 0.0 to 1.0
     * @param screenBrightness 0.0 to 1.0
     * @param networkActivity 0.0 to 1.0
     * @param isWiFiConnected true if using WiFi
     * @param isDeviceMoving true if device is in motion
     * @param movementSpeedMs speed in m/s
     * @param isIndoors true if device is indoors
     * @param environmentType sensor environment type
     * @return UnifiedHardwareState
     */
    public UnifiedHardwareState updateHardwareState(double cpuLoad, double screenBrightness,
                                                    double networkActivity, boolean isWiFiConnected,
                                                    boolean isDeviceMoving, double movementSpeedMs,
                                                    boolean isIndoors,
                                                    SensorFloorNoiseHook.EnvironmentType environmentType) {
        // Update thermal state
        thermalHook.updateThermalState(cpuLoad);
        
        // Update battery state
        batteryHook.updateBatteryState(cpuLoad, screenBrightness, networkActivity, isWiFiConnected);
        
        // Update network state
        networkHook.updateNetworkState(isDeviceMoving, isIndoors, movementSpeedMs);
        
        // Update sensor environment
        sensorHook.updateEnvironment(environmentType);
        
        // Update simulation duration
        simulationDurationMs = System.currentTimeMillis() - simulationStartTimeMs;
        currentDay = (int) (simulationDurationMs / 86400000) + 1;
        
        return getUnifiedHardwareState();
    }
    
    /**
     * Update storage state for a new day.
     */
    public void updateStorageDay(double newStorageGB, double filesDeletedGB, boolean isHeavyWriteDay) {
        storageHook.updateStorageState(newStorageGB, filesDeletedGB, isHeavyWriteDay);
    }
    
    /**
     * Simulate app launch with all hardware effects.
     */
    public AppLaunchResult simulateAppLaunch(String appName, double appSizeMB, boolean isColdLaunch,
                                           double cpuLoadDuringLaunch) {
        // Apply thermal effects
        double thermalFactor = thermalHook.calculateThermalFactor();
        
        // Apply storage I/O degradation
        double appLoadTimeMs = storageHook.simulateAppLoadTime(appName, appSizeMB, isColdLaunch);
        
        // Apply thermal throttling to load time
        appLoadTimeMs *= thermalFactor;
        
        // Simulate network requests during app launch
        NetworkJitterHook.NetworkResult networkResult = 
            networkHook.simulateNetworkRequest(1024); // 1KB typical request
        
        // Calculate CPU temperature during launch
        double launchTemp = thermalHook.getCurrentTemperatureC();
        
        // Check battery state
        boolean lowPowerMode = batteryHook.getBatteryState().percentage < 20.0;
        
        return new AppLaunchResult(
            appName,
            appLoadTimeMs,
            launchTemp,
            networkResult.latencyMs,
            networkResult.success,
            lowPowerMode,
            thermalFactor
        );
    }
    
    /**
     * Simulate task execution with hardware exhaust.
     */
    public TaskExecutionResult simulateTaskExecution(double baseTaskTimeMs, double cpuLoad,
                                                     double networkRequirement) {
        // Apply thermal throttling
        double thermalFactor = thermalHook.calculateThermalFactor();
        double adjustedTaskTimeMs = thermalHook.calculateTaskExecutionTime(baseTaskTimeMs);
        
        // Apply battery-based priority multiplier
        double priorityMultiplier = batteryHook.getTaskPriorityMultiplier();
        adjustedTaskTimeMs /= priorityMultiplier; // Lower priority = longer time
        
        // Simulate network dependency
        NetworkJitterHook.NetworkResult networkResult = null;
        if (networkRequirement > 0.0) {
            networkResult = networkHook.simulateNetworkRequest((int)(networkRequirement * 1024));
            if (!networkResult.success) {
                // Retry on packet loss
                networkResult = networkHook.simulateNetworkRequest((int)(networkRequirement * 1024));
                adjustedTaskTimeMs += networkResult.latencyMs;
            } else {
                adjustedTaskTimeMs += networkResult.latencyMs * 0.5;
            }
        }
        
        // Apply storage I/O if task involves database operations
        double storageOverhead = 0.0;
        if (cpuLoad > 0.5) {
            storageOverhead = storageHook.simulateDatabaseQuery(cpuLoad * 2.0);
            adjustedTaskTimeMs += storageOverhead;
        }
        
        // Check if task should be scheduled based on battery state
        boolean taskScheduled = true;
        if (cpuLoad > 0.7) {
            taskScheduled = batteryHook.shouldScheduleBackgroundTask(0.5);
        }
        
        return new TaskExecutionResult(
            baseTaskTimeMs,
            adjustedTaskTimeMs,
            thermalHook.getCurrentTemperatureC(),
            batteryHook.getBatteryState().percentage,
            taskScheduled,
            networkResult != null ? networkResult.latencyMs : 0.0,
            storageOverhead
        );
    }
    
    /**
     * Get unified hardware state for telemetry.
     */
    public UnifiedHardwareState getUnifiedHardwareState() {
        ThermalThrottlingHook.ThermalState thermalState = thermalHook.getThermalState();
        BatteryDischargeHook.BatteryState batteryState = batteryHook.getBatteryState();
        NetworkJitterHook.NetworkState networkState = networkHook.getNetworkState();
        StorageIODegradationHook.StorageState storageState = storageHook.getStorageState();
        SensorFloorNoiseHook.SensorState sensorState = sensorHook.getSensorState();
        
        return new UnifiedHardwareState(
            thermalState,
            batteryState,
            networkState,
            storageState,
            sensorState,
            currentDay,
            simulationDurationMs
        );
    }
    
    /**
     * Force hardware state changes for testing.
     */
    public void forceCoolDown() {
        thermalHook.forceCoolDown();
    }
    
    public void forceNetworkType(NetworkJitterHook.NetworkType networkType) {
        networkHook.forceNetworkType(networkType);
    }
    
    public void forceMagnetometerCalibration() {
        sensorHook.forceMagnetometerCalibration();
    }
    
    public void chargeBattery(double chargePercentage) {
        batteryHook.chargeBattery(chargePercentage);
    }
    
    /**
     * Reset simulation to initial state.
     */
    public void resetSimulation(double initialBatteryPercentage) {
        thermalHook.forceCoolDown();
        batteryHook.chargeBattery(100.0 - batteryHook.getBatteryState().percentage);
        forceNetworkType(NetworkJitterHook.NetworkType.LTE_GOOD);
        sensorHook.forceMagnetometerCalibration();
        
        simulationStartTimeMs = System.currentTimeMillis();
        simulationDurationMs = 0;
        currentDay = 1;
    }
    
    // Inner classes for results
    
    public static class AppLaunchResult {
        public final String appName;
        public final double loadTimeMs;
        public final double cpuTemperatureC;
        public final double networkLatencyMs;
        public final boolean networkSuccess;
        public final boolean lowPowerMode;
        public final double thermalFactor;
        
        public AppLaunchResult(String appName, double loadTimeMs, double cpuTemperatureC,
                              double networkLatencyMs, boolean networkSuccess,
                              boolean lowPowerMode, double thermalFactor) {
            this.appName = appName;
            this.loadTimeMs = loadTimeMs;
            this.cpuTemperatureC = cpuTemperatureC;
            this.networkLatencyMs = networkLatencyMs;
            this.networkSuccess = networkSuccess;
            this.lowPowerMode = lowPowerMode;
            this.thermalFactor = thermalFactor;
        }
    }
    
    public static class TaskExecutionResult {
        public final double baseExecutionTimeMs;
        public final double adjustedExecutionTimeMs;
        public final double cpuTemperatureC;
        public final double batteryPercentage;
        public final boolean taskScheduled;
        public final double networkLatencyMs;
        public final double storageOverheadMs;
        
        public TaskExecutionResult(double baseExecutionTimeMs, double adjustedExecutionTimeMs,
                                   double cpuTemperatureC, double batteryPercentage,
                                   boolean taskScheduled, double networkLatencyMs,
                                   double storageOverheadMs) {
            this.baseExecutionTimeMs = baseExecutionTimeMs;
            this.adjustedExecutionTimeMs = adjustedExecutionTimeMs;
            this.cpuTemperatureC = cpuTemperatureC;
            this.batteryPercentage = batteryPercentage;
            this.taskScheduled = taskScheduled;
            this.networkLatencyMs = networkLatencyMs;
            this.storageOverheadMs = storageOverheadMs;
        }
    }
    
    public static class UnifiedHardwareState {
        public final ThermalThrottlingHook.ThermalState thermal;
        public final BatteryDischargeHook.BatteryState battery;
        public final NetworkJitterHook.NetworkState network;
        public final StorageIODegradationHook.StorageState storage;
        public final SensorFloorNoiseHook.SensorState sensor;
        public final int day;
        public final long simulationDurationMs;
        
        public UnifiedHardwareState(ThermalThrottlingHook.ThermalState thermal,
                                    BatteryDischargeHook.BatteryState battery,
                                    NetworkJitterHook.NetworkState network,
                                    StorageIODegradationHook.StorageState storage,
                                    SensorFloorNoiseHook.SensorState sensor,
                                    int day, long simulationDurationMs) {
            this.thermal = thermal;
            this.battery = battery;
            this.network = network;
            this.storage = storage;
            this.sensor = sensor;
            this.day = day;
            this.simulationDurationMs = simulationDurationMs;
        }
        
        /**
         * Generate CSV header for telemetry export.
         */
        public static String getCSVHeader() {
            return "day,simulation_time_ms," +
                   "temp_c,is_throttling,throttle_duration_min,ui_latency_ms,thermal_factor," +
                   "battery_pct,voltage,charge_cycles,discharge_mah,est_capacity_mah," +
                   "network_type,rssi_dbm,avg_latency_ms,packet_loss_rate,is_indoors,is_moving," +
                   "storage_gb_used,fill_level,fragmentation,read_speed_mbps,write_speed_mbps,bad_blocks," +
                   "sensor_env,gps_offset_lat_m,gps_offset_lon_m,mag_bias_x,mag_bias_y,mag_bias_z,em_interference";
        }
        
        /**
         * Generate CSV row for telemetry export.
         */
        public String toCSVRow() {
            return String.format("%d,%d,%.2f,%b,%.2f,%.2f,%.4f,%.2f,%.2f,%d,%.2f,%.2f,%s,%.2f,%.2f,%.4f,%b,%b,%.2f,%.4f,%.4f,%.2f,%.2f,%d,%s,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f",
                    day,
                    simulationDurationMs,
                    thermal.temperatureC,
                    thermal.isThrottling,
                    thermal.throttleDurationMinutes,
                    thermal.currentUILatencyMs,
                    thermal.thermalFactor,
                    battery.percentage,
                    battery.voltage,
                    battery.chargeCycleCount,
                    battery.cumulativeDischargeMAh,
                    battery.estimatedCapacityMAh,
                    network.networkType.name(),
                    network.rssidBm,
                    network.averageLatencyMs,
                    network.packetLossRate,
                    network.isIndoors,
                    network.isMoving,
                    storage.usedStorageGB,
                    storage.fillLevel,
                    storage.fragmentationLevel,
                    storage.effectiveReadSpeedMBps,
                    storage.effectiveWriteSpeedMBps,
                    storage.badBlockCount,
                    sensor.environment.name(),
                    sensor.gpsLatitudeOffsetM,
                    sensor.gpsLongitudeOffsetM,
                    sensor.magnetometerBias[0],
                    sensor.magnetometerBias[1],
                    sensor.magnetometerBias[2],
                    sensor.emInterferenceUT
            );
        }
    }
}
