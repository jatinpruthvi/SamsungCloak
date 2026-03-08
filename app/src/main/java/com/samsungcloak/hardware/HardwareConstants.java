package com.samsungcloak.hardware;

/**
 * Hardware-specific constants for Samsung Galaxy A12 (SM-A125U)
 * 
 * This class contains all device-specific calibration constants used
 * by the Hardware Exhaust & Environmental Fidelity layer.
 */
public final class HardwareConstants {
    
    // Prevent instantiation
    private HardwareConstants() {}
    
    /**
     * Device identification
     */
    public static final String MODEL_NAME = "SM-A125U";
    public static final String BRAND = "Samsung";
    public static final String DEVICE = "a12";
    public static final String MANUFACTURER = "Samsung";
    
    /**
     * Processor Specifications (MediaTek Helio P35 - MT6765)
     */
    public static final String CPU_MODEL = "MediaTek Helio P35";
    public static final String CPU_ARCHITECTURE = "ARM Cortex-A53";
    public static final int CPU_CORE_COUNT = 8;
    public static final double CPU_MAX_FREQUENCY_GHZ = 2.35;
    public static final double CPU_MIN_FREQUENCY_GHZ = 1.8;
    
    // CPU configuration: 4x2.35 GHz + 4x1.8 GHz
    public static final int HIGH_PERFORMANCE_CORES = 4;
    public static final int POWER_EFFICIENT_CORES = 4;
    
    /**
     * GPU Specifications (PowerVR GE8320)
     */
    public static final String GPU_MODEL = "PowerVR GE8320";
    public static final int GPU_CORE_COUNT = 1;
    public static final int GPU_FREQUENCY_MHZ = 680;
    
    /**
     * Memory Specifications
     */
    public static final String RAM_TYPE = "LPDDR4X";
    public static final int RAM_CAPACITY_GB = 4; // Base model
    public static final int RAM_FREQUENCY_MHZ = 1600;
    public static final double RAM_BANDWIDTH_GBPS = 12.8;
    
    /**
     * Storage Specifications (eMMC 5.1)
     */
    public static final String STORAGE_TYPE = "eMMC 5.1";
    public static final int STORAGE_CAPACITY_GB = 32; // Base model
    public static final double STORAGE_SEQUENTIAL_READ_MBPS = 250.0;
    public static final double STORAGE_SEQUENTIAL_WRITE_MBPS = 125.0;
    public static final double STORAGE_RANDOM_READ_IOPS = 4000.0;
    public static final double STORAGE_RANDOM_WRITE_IOPS = 2000.0;
    
    /**
     * Display Specifications
     */
    public static final String DISPLAY_TYPE = "PLS TFT";
    public static final double DISPLAY_SIZE_INCHES = 6.5;
    public static final int DISPLAY_RESOLUTION_WIDTH = 720;
    public static final int DISPLAY_RESOLUTION_HEIGHT = 1600;
    public static final int DISPLAY_PPI = 270;
    public static final double DISPLAY_REFRESH_RATE_HZ = 60.0;
    public static final double DISPLAY_TOUCH_SAMPLE_RATE_HZ = 120.0;
    
    /**
     * Battery Specifications
     */
    public static final String BATTERY_TYPE = "Li-Po";
    public static final int BATTERY_CAPACITY_MAH = 5000;
    public static final double BATTERY_VOLTAGE_NOMINAL = 3.85;
    public static final double BATTERY_VOLTAGE_MAX = 4.4;
    public static final double BATTERY_VOLTAGE_MIN = 3.3;
    public static final int BATTERY_CHARGING_WATTS = 15; // 15W fast charging
    public static final double BATTERY_PEUKERT_EXPONENT = 1.15;
    
    /**
     * Network Specifications
     */
    public static final String NETWORK_TECHNOLOGY = "GSM / HSPA / LTE";
    public static final String LTE_CATEGORY = "LTE Cat. 4";
    public static final double LTE_DOWNLOAD_MBPS = 150.0;
    public static final double LTE_UPLOAD_MBPS = 50.0;
    
    public static final String WIFI_STANDARD = "Wi-Fi 802.11 a/b/g/n/ac";
    public static final String_WIFI_BANDS_2_4GHZ = "2.4 GHz";
    public static final String_WIFI_BANDS_5GHZ = "5 GHz";
    public static final double WIFI_MAX_LINK_SPEED_MBPS = 433.0;
    
    public static final String BLUETOOTH_VERSION = "5.0";
    public static final String GPS_TYPE = "GPS, GLONASS, BDS, GALILEO";
    
    /**
     * Thermal Specifications
     */
    public static final double THERMAL_BASE_TEMPERATURE_C = 25.0;
    public static final double THERMAL_THROTTLE_THRESHOLD_C = 42.0;
    public static final double THERMAL_CRITICAL_TEMPERATURE_C = 48.0;
    public static final double THERMAL_MAX_TEMPERATURE_C = 55.0;
    public static final double THERMAL_PASSIVE_COOLING_RATE = 0.8; // °C per minute
    public static final double THERMAL_ACTIVE_COOLING_RATE = 2.5; // °C per minute
    
    // Heat generation rates (°C per minute)
    public static final double THERMAL_HEAT_GENERATION_HIGH = 3.5;
    public static final double THERMAL_HEAT_GENERATION_MEDIUM = 1.8;
    public static final double THERMAL_HEAT_GENERATION_LOW = 0.5;
    
    /**
     * Performance Characteristics
     */
    public static final double BASE_UI_LATENCY_MS = 16.0;
    public static final double MAX_LATENCY_MULTIPLIER = 4.5;
    public static final double BASE_TASK_EXECUTION_MS = 1.0;
    public static final double MAX_TASK_MULTIPLIER = 3.2;
    
    /**
     * Storage Degradation Constants
     */
    public static final double STORAGE_FRAGMENTATION_RATE_PER_DAY = 0.003;
    public static final double STORAGE_FILL_LEVEL_DEGRADATION_THRESHOLD = 0.75;
    public static final double STORAGE_CRITICAL_FILL_LEVEL = 0.90;
    public static final double STORAGE_WEAR_LEVELING_FACTOR = 0.0001;
    public static final double STORAGE_BAD_BLOCK_RATE = 0.00001;
    
    /**
     * Sensor Specifications
     */
    public static final double GPS_BASE_ACCURACY_M = 5.0;
    public static final double GPS_URBAN_CANYON_PENALTY_M = 15.0;
    public static final double GPS_INDOOR_PENALTY_M = 30.0;
    public static final double GPS_DRIFT_RATE_MS_PER_SEC = 0.05;
    public static final double GPS_NOISE_STD_DEV_M = 2.0;
    public static final double GPS_MULTI_PATH_PROBABILITY = 0.15;
    
    public static final double MAGNETOMETER_BASE_FIELD_UT = 45.0;
    public static final double MAGNETOMETER_RESOLUTION_UT = 0.1;
    public static final double MAGNETOMETER_NOISE_STD_DEV_UT = 0.5;
    public static final double MAGNETIC_DRIFT_RATE_UT_PER_SEC = 0.01;
    public static final double BIAS_STABILITY_UT = 0.05;
    
    // EM Interference levels
    public static final double URBAN_EM_INTERFERENCE_UT = 2.0;
    public static final double VEHICLE_INTERFERENCE_UT = 5.0;
    public static final double BUILDING_INTERFERENCE_UT = 3.0;
    
    /**
     * Power Consumption Profiles (mW)
     */
    public static final double POWER_IDLE_MW = 15.0;
    public static final double POWER_SCREEN_ON_BASE_MW = 80.0;
    public static final double POWER_SCREEN_MAX_ADDITIONAL_MW = 150.0;
    public static final double POWER_CPU_BASE_MW = 250.0;
    public static final double POWER_CPU_MAX_MW = 1800.0;
    public static final double POWER_MODEM_4G_MW = 400.0;
    public static final double POWER_MODEM_WIFI_MW = 120.0;
    
    /**
     * Battery Degradation Constants
     */
    public static final double BATTERY_CYCLE_DEGRADATION_RATE = 0.0002;
    public static final double BATTERY_TIME_DEGRADATION_RATE = 0.000001;
    public static final double BATTERY_MAX_DEGRADATION = 0.15; // 15% max degradation
    
    /**
     * Calibration Values
     */
    public static final double THERMAL_SIGMOID_CENTER = 0.3;
    public static final double THERMAL_SIGMOID_STEEPNESS = 8.0;
    public static final double THERMAL_INERTIA_PENALTY_THRESHOLD = 5.0; // minutes
    public static final double THERMAL_INERTIA_MAX_PENALTY = 0.3;
    
    /**
     * Network Constants
     */
    public static final double RSSI_EXCELLENT_DBM = -50.0;
    public static final double RSSI_GOOD_DBM = -70.0;
    public static final double RSSI_FAIR_DBM = -85.0;
    public static final double RSSI_POOR_DBM = -95.0;
    
    public static final double WALKING_SPEED_MS = 1.4;
    public static final double DRIVING_SPEED_MS = 13.9;
    
    /**
     * Simulation Constants
     */
    public static final double SIMULATION_TICK_MS = 1000.0; // 1 second per tick
    public static final double SIMULATION_DAY_MS = 86400000.0; // 24 hours
    
    /**
     * Validation Ranges
     */
    public static final double MIN_VALID_BATTERY_PERCENTAGE = 0.0;
    public static final double MAX_VALID_BATTERY_PERCENTAGE = 100.0;
    public static final double MIN_VALID_TEMPERATURE_C = 0.0;
    public static final double MAX_VALID_TEMPERATURE_C = 100.0;
    public static final double MIN_VALID_LATENCY_MS = 0.0;
    public static final double MAX_VALID_LATENCY_MS = 10000.0;
    
    /**
     * Helper method to validate hardware state
     */
    public static boolean isValidBatteryPercentage(double percentage) {
        return percentage >= MIN_VALID_BATTERY_PERCENTAGE && 
               percentage <= MAX_VALID_BATTERY_PERCENTAGE;
    }
    
    public static boolean isValidTemperature(double temperatureC) {
        return temperatureC >= MIN_VALID_TEMPERATURE_C && 
               temperatureC <= MAX_VALID_TEMPERATURE_C;
    }
    
    public static boolean isValidLatency(double latencyMs) {
        return latencyMs >= MIN_VALID_LATENCY_MS && 
               latencyMs <= MAX_VALID_LATENCY_MS;
    }
    
    /**
     * Get device information string
     */
    public static String getDeviceInfo() {
        return String.format(
            "%s %s (%s)\n" +
            "CPU: %s (8x Cortex-A53, 2.35GHz/1.8GHz)\n" +
            "GPU: %s (%dMHz)\n" +
            "RAM: %dGB %s (%dMHz)\n" +
            "Storage: %dGB %s\n" +
            "Display: %.1f\" %dx%d @ %.0fHz\n" +
            "Battery: %dmAh %s\n" +
            "Network: LTE Cat.4, WiFi 802.11ac",
            BRAND, MODEL_NAME, DEVICE,
            CPU_MODEL,
            GPU_MODEL, GPU_FREQUENCY_MHZ,
            RAM_CAPACITY_GB, RAM_TYPE, RAM_FREQUENCY_MHZ,
            STORAGE_CAPACITY_GB, STORAGE_TYPE,
            DISPLAY_SIZE_INCHES, DISPLAY_RESOLUTION_WIDTH, DISPLAY_RESOLUTION_HEIGHT,
            DISPLAY_REFRESH_RATE_HZ,
            BATTERY_CAPACITY_MAH, BATTERY_TYPE
        );
    }
}
