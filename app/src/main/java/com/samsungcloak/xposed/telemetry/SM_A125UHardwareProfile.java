package com.samsungcloak.xposed.telemetry;

import java.util.Random;

/**
 * Hardware profile constants and utilities for Samsung Galaxy A12 (SM-A125U)
 * Used by the High-Fidelity Telemetry Layer for HIL testing
 */
public class SM_A125UHardwareProfile {
    
    public static final String MODEL = "SM-A125U";
    public static final String MANUFACTURER = "samsung";
    public static final String BRAND = "samsung";
    public static final String DEVICE = "a12q";
    public static final String PRODUCT = "a12q";
    public static final String HARDWARE = "exynos850";
    public static final String BOARD = "exynos850";
    public static final String ANDROID_VERSION = "11";
    public static final String SDK_VERSION = "30";

    public static class Battery {
        public static final float CAPACITY_MAH = 5000.0f;
        public static final int VOLTAGE_MV = 3700;
        public static final float TYPICAL_DISCHARGE_RATE_MA = 450.0f;
        public static final float SCREEN_ON_ADDITIONAL_MA = 250.0f;
        public static final float LOW_BATTERY_THRESHOLD = 0.15f;
        public static final float CRITICAL_BATTERY_THRESHOLD = 0.05f;
        
        public static final int BATTERY_TEMPERATURE_MIN_C = 20;
        public static final int BATTERY_TEMPERATURE_MAX_C = 45;
        public static final int BATTERY_TEMPERATURE_IDLE_C = 25;
    }

    public static class Display {
        public static final int WIDTH_PIXELS = 720;
        public static final int HEIGHT_PIXELS = 1560;
        public static final float SIZE_INCHES = 6.5f;
        public static final int DENSITY_DPI = 280;
        public static final float REFRESH_RATE_HZ = 60.0f;
        
        public static final float BRIGHTNESS_MIN = 0.02f;
        public static final float BRIGHTNESS_MAX = 1.0f;
        public static final float BRIGHTNESS_DEFAULT = 0.5f;
        
        public static final int BRIGHTNESS_TRANSITION_MS = 300;
    }

    public static class Sensors {
        public static class Accelerometer {
            public static final String NAME = "LSM6DSO Accelerometer";
            public static final int TYPE = 1;
            public static final float MAX_RANGE = 156.96f;
            public static final float RESOLUTION = 0.0048f;
            public static final float POWER_MA = 0.26f;
            public static final int MIN_DELAY_US = 10000;
        }

        public static class Gyroscope {
            public static final String NAME = "LSM6DSO Gyroscope";
            public static final int TYPE = 4;
            public static final float MAX_RANGE = 34.91f;
            public static final float RESOLUTION = 0.0011f;
            public static final float POWER_MA = 0.26f;
            public static final int MIN_DELAY_US = 10000;
        }

        public static class Proximity {
            public static final String NAME = "Proximity Sensor";
            public static final int TYPE = 8;
            public static final float MAX_RANGE = 5.0f;
        }

        public static class Light {
            public static final String NAME = "Light Sensor";
            public static final int TYPE = 5;
            public static final float MAX_RANGE = 43000.0f;
        }
    }

    public static class Network {
        public static final boolean SUPPORTS_5G = true;
        public static final boolean SUPPORTS_4G = true;
        public static final boolean SUPPORTS_3G = true;
        public static final boolean SUPPORTS_WIFI = true;
        public static final boolean SUPPORTS_BLUETOOTH = true;
        public static final boolean SUPPORTS_NFC = false;

        public static final int WIFI_MAX_SPEED_MBPS = 433;
        public static final int LTE_MAX_SPEED_MBPS = 600;
        public static final int NR_MAX_SPEED_MBPS = 2000;

        public static final int TYPICAL_LATENCY_WIFI_MS = 15;
        public static final int TYPICAL_LATENCY_4G_MS = 40;
        public static final int TYPICAL_LATENCY_5G_MS = 15;
    }

    public static class Performance {
        public static final int CPU_CORES = 8;
        public static final int CPU_MAX_FREQ_MHZ = 2000;
        public static final int CPU_MIN_FREQ_MHZ = 300;
        public static final int RAM_MB = 4096;
        
        public static final int THERMAL_THROTTLE_THRESHOLD_C = 45;
        public static final int THERMAL_CRITICAL_C = 55;
        
        public static final long UPTIME_SECONDS = 3600;
    }

    public static class Touch {
        public static final int TOUCH_MAX_POINTERS = 10;
        public static final float TOUCH_SIZE_MIN = 0.2f;
        public static final float TOUCH_SIZE_MAX = 1.0f;
        
        public static final double COORDINATE_JITTER_STDDEV_DP = 1.5;
        public static final double TIMING_JITTER_STDDEV_MS = 12.0;
        
        public static final double PRESSURE_VARIANCE = 0.08;
        public static final double SIZE_VARIANCE = 0.06;
    }

    public static class MicroVibration {
        public static final double TREMOR_FREQUENCY_HZ = 8.0;
        public static final double TREMOR_AMPLITUDE_MG = 15.0;
        public static final double NOISE_FLOOR_MG = 0.8;
        
        public static final double BREATHING_FREQUENCY_HZ = 0.25;
        public static final double BREATHING_AMPLITUDE_MG = 30.0;
        
        public static final double HEARTBEAT_FREQUENCY_HZ = 1.2;
        public static final double HEARTBEAT_AMPLITUDE_MG = 5.0;
    }
}
