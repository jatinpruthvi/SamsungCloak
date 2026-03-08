package com.samsungcloak.hardware;

import java.util.Random;

/**
 * Sensor Floor Noise Hook for Samsung Galaxy A12 (SM-A125U)
 * 
 * Implements realistic drift and bias in GPS and magnetometer data
 * to reflect low-level electromagnetic interference found in real-world
 * urban environments.
 * 
 * Sensor Specs:
 * - GPS: GNSS (GPS, GLONASS, BDS, GALILEO)
 * - Magnetometer: 3-axis magnetic field sensor
 */
public class SensorFloorNoiseHook {
    
    // GPS Constants
    private static final double BASE_GPS_ACCURACY_M = 5.0; // Typical GPS accuracy
    private static final double URBAN_CANYON_ACCURACY_PENALTY_M = 15.0;
    private static final double INDOOR_ACCURACY_PENALTY_M = 30.0;
    private static final double GPS_DRIFT_RATE_MS_PER_SEC = 0.05; // meters per second
    
    // GPS noise characteristics
    private static final double GPS_NOISE_STD_DEV_M = 2.0;
    private static final double GPS_MULTI_PATH_PROBABILITY = 0.15; // 15% chance of multipath
    
    // Magnetometer Constants
    private static final double BASE_MAGNETIC_FIELD_UT = 45.0; // Typical Earth field (µT)
    private static final double MAGNETOMETER_RESOLUTION_UT = 0.1; // Sensor resolution
    private static final double MAGNETOMETER_NOISE_STD_DEV_UT = 0.5; // Base noise
    
    // Electromagnetic interference sources
    private static final double URBAN_EM_INTERFERENCE_UT = 2.0; // Typical urban interference
    private static final double VEHICLE_INTERFERENCE_UT = 5.0; // Near vehicles/electronics
    private static final double BUILDING_INTERFERENCE_UT = 3.0; // Near steel structures
    
    // Drift and bias parameters
    private static final double MAGNETIC_DRIFT_RATE_UT_PER_SEC = 0.01;
    private static final double BIAS_STABILITY_UT = 0.05;
    
    // Environment types
    public enum EnvironmentType {
        OPEN_OUTDOOR("Open Outdoor", 1.0, 0.5),
        URBAN_CANYON("Urban Canyon", 0.6, 2.0),
        INDOOR("Indoor", 0.3, 3.0),
        UNDERGROUND("Underground", 0.1, 5.0),
        NEAR_VEHICLE("Near Vehicle", 0.8, 4.0),
        NEAR_BUILDING("Near Building", 0.7, 2.5);
        
        private final String name;
        private final double gpsQualityFactor;
        private final double emInterferenceMultiplier;
        
        EnvironmentType(String name, double gpsQualityFactor, double emInterferenceMultiplier) {
            this.name = name;
            this.gpsQualityFactor = gpsQualityFactor;
            this.emInterferenceMultiplier = emInterferenceMultiplier;
        }
    }
    
    private final Random random;
    private EnvironmentType currentEnvironment;
    
    // GPS state
    private double latitudeOffsetM;
    private double longitudeOffsetM;
    private double altitudeOffsetM;
    private long lastGPSTimeMs;
    
    // Magnetometer state
    private double[] magnetometerBias; // X, Y, Z bias in µT
    private double[] magnetometerDrift; // X, Y, Z drift rate in µT/s
    private long lastMagnetometerTimeMs;
    
    // Interference state
    private double currentEMInterferenceUT;
    private boolean isInMagneticAnomaly;
    
    public SensorFloorNoiseHook() {
        this.random = new Random();
        this.currentEnvironment = EnvironmentType.OPEN_OUTDOOR;
        
        // Initialize GPS state
        this.latitudeOffsetM = 0.0;
        this.longitudeOffsetM = 0.0;
        this.altitudeOffsetM = 0.0;
        this.lastGPSTimeMs = System.currentTimeMillis();
        
        // Initialize magnetometer state
        this.magnetometerBias = new double[]{0.0, 0.0, 0.0};
        this.magnetometerDrift = new double[]{0.0, 0.0, 0.0};
        this.lastMagnetometerTimeMs = System.currentTimeMillis();
        
        // Initialize interference state
        this.currentEMInterferenceUT = 0.0;
        this.isInMagneticAnomaly = false;
    }
    
    /**
     * Update environmental conditions.
     * 
     * @param environment current environment type
     */
    public void updateEnvironment(EnvironmentType environment) {
        this.currentEnvironment = environment;
        
        // Update EM interference based on environment
        double baseInterference = URBAN_EM_INTERFERENCE_UT * 
                                 environment.emInterferenceMultiplier;
        currentEMInterferenceUT = baseInterference + 
                                random.nextDouble() * baseInterference * 0.5;
        
        // Check for magnetic anomaly
        isInMagneticAnomaly = (random.nextDouble() < 0.05) && 
                             (environment != EnvironmentType.OPEN_OUTDOOR);
        
        if (isInMagneticAnomaly) {
            currentEMInterferenceUT *= 3.0;
        }
    }
    
    /**
     * Simulate GPS reading with realistic noise and drift.
     * 
     * @param trueLatitude true latitude in degrees
     * @param trueLongitude true longitude in degrees
     * @param trueAltitude true altitude in meters
     * @return GPSReading with noisy coordinates
     */
    public GPSReading simulateGPSReading(double trueLatitude, double trueLongitude, 
                                        double trueAltitude) {
        long currentTimeMs = System.currentTimeMillis();
        long timeDeltaMs = currentTimeMs - lastGPSTimeMs;
        double timeDeltaSec = timeDeltaMs / 1000.0;
        
        // Update GPS drift
        updateGPSDrift(timeDeltaSec);
        
        // Calculate base accuracy for current environment
        double baseAccuracy = BASE_GPS_ACCURACY_M / currentEnvironment.gpsQualityFactor;
        
        // Add random noise
        double latitudeNoise = generateGaussianNoise() * GPS_NOISE_STD_DEV_M;
        double longitudeNoise = generateGaussianNoise() * GPS_NOISE_STD_DEV_M;
        double altitudeNoise = generateGaussianNoise() * (GPS_NOISE_STD_DEV_M * 2.0);
        
        // Check for multipath interference
        if (random.nextDouble() < GPS_MULTI_PATH_PROBABILITY) {
            // Multipath causes larger, biased errors
            double multipathBias = random.nextDouble() * 20.0 - 10.0;
            latitudeNoise += multipathBias;
            longitudeNoise += multipathBias * 0.8;
        }
        
        // Calculate noisy coordinates (convert meter offsets to degrees)
        double latitudeDegreesPerMeter = 1.0 / 111320.0;
        double longitudeDegreesPerMeter = 1.0 / (111320.0 * Math.cos(Math.toRadians(trueLatitude)));
        
        double noisyLatitude = trueLatitude + 
                             (latitudeOffsetM + latitudeNoise) * latitudeDegreesPerMeter;
        double noisyLongitude = trueLongitude + 
                              (longitudeOffsetM + longitudeNoise) * longitudeDegreesPerMeter;
        double noisyAltitude = trueAltitude + altitudeOffsetM + altitudeNoise;
        
        // Calculate accuracy estimate
        double accuracyEstimate = baseAccuracy + 
                                Math.abs(latitudeOffsetM) + 
                                Math.abs(longitudeOffsetM) +
                                Math.abs(altitudeOffsetM) * 0.5;
        
        lastGPSTimeMs = currentTimeMs;
        
        return new GPSReading(noisyLatitude, noisyLongitude, noisyAltitude, 
                             accuracyEstimate, currentTimeMs);
    }
    
    /**
     * Update GPS drift over time.
     */
    private void updateGPSDrift(double timeDeltaSec) {
        // Random walk drift
        latitudeOffsetM += generateGaussianNoise() * GPS_DRIFT_RATE_MS_PER_SEC * timeDeltaSec;
        longitudeOffsetM += generateGaussianNoise() * GPS_DRIFT_RATE_MS_PER_SEC * timeDeltaSec;
        altitudeOffsetM += generateGaussianNoise() * (GPS_DRIFT_RATE_MS_PER_SEC * 0.5) * timeDeltaSec;
        
        // Bias stabilization (slowly drift back toward zero)
        latitudeOffsetM *= 0.999;
        longitudeOffsetM *= 0.999;
        altitudeOffsetM *= 0.999;
    }
    
    /**
     * Simulate magnetometer reading with realistic noise, drift, and bias.
     * 
     * @param trueMagneticFieldX true magnetic field X component in µT
     * @param trueMagneticFieldY true magnetic field Y component in µT
     * @param trueMagneticFieldZ true magnetic field Z component in µT
     * @return MagnetometerReading with noisy components
     */
    public MagnetometerReading simulateMagnetometerReading(double trueMagneticFieldX, 
                                                           double trueMagneticFieldY,
                                                           double trueMagneticFieldZ) {
        long currentTimeMs = System.currentTimeMillis();
        long timeDeltaMs = currentTimeMs - lastMagnetometerTimeMs;
        double timeDeltaSec = timeDeltaMs / 1000.0;
        
        // Update magnetometer drift and bias
        updateMagnetometerState(timeDeltaSec);
        
        // Add sensor noise
        double noiseX = generateGaussianNoise() * MAGNETOMETER_NOISE_STD_DEV_UT;
        double noiseY = generateGaussianNoise() * MAGNETOMETER_NOISE_STD_DEV_UT;
        double noiseZ = generateGaussianNoise() * MAGNETOMETER_NOISE_STD_DEV_UT;
        
        // Add EM interference (affects all axes differently)
        double interferenceX = currentEMInterferenceUT * (random.nextDouble() - 0.5);
        double interferenceY = currentEMInterferenceUT * (random.nextDouble() - 0.5);
        double interferenceZ = currentEMInterferenceUT * (random.nextDouble() - 0.5);
        
        // Calculate noisy readings
        double noisyX = trueMagneticFieldX + magnetometerBias[0] + noiseX + interferenceX;
        double noisyY = trueMagneticFieldY + magnetometerBias[1] + noiseY + interferenceY;
        double noisyZ = trueMagneticFieldZ + magnetometerBias[2] + noiseZ + interferenceZ;
        
        // Apply resolution quantization
        noisyX = Math.round(noisyX / MAGNETOMETER_RESOLUTION_UT) * MAGNETOMETER_RESOLUTION_UT;
        noisyY = Math.round(noisyY / MAGNETOMETER_RESOLUTION_UT) * MAGNETOMETER_RESOLUTION_UT;
        noisyZ = Math.round(noisyZ / MAGNETOMETER_RESOLUTION_UT) * MAGNETOMETER_RESOLUTION_UT;
        
        // Calculate magnetic field magnitude
        double magnitude = Math.sqrt(noisyX * noisyX + noisyY * noisyY + noisyZ * noisyZ);
        
        lastMagnetometerTimeMs = currentTimeMs;
        
        return new MagnetometerReading(noisyX, noisyY, noisyZ, magnitude, 
                                      magnetometerBias.clone(), currentTimeMs);
    }
    
    /**
     * Update magnetometer drift and bias over time.
     */
    private void updateMagnetometerState(double timeDeltaSec) {
        // Update bias (slow random walk)
        for (int i = 0; i < 3; i++) {
            magnetometerBias[i] += generateGaussianNoise() * BIAS_STABILITY_UT * timeDeltaSec;
            
            // Bias has tendency to stabilize
            magnetometerBias[i] *= 0.999;
            
            // Limit bias magnitude
            magnetometerBias[i] = Math.max(-10.0, Math.min(10.0, magnetometerBias[i]));
        }
        
        // Update drift rates
        for (int i = 0; i < 3; i++) {
            magnetometerDrift[i] += (random.nextDouble() - 0.5) * MAGNETIC_DRIFT_RATE_UT_PER_SEC;
            magnetometerDrift[i] *= 0.95; // Drift rates decay over time
        }
    }
    
    /**
     * Simulate compass heading calculation from magnetometer data.
     * 
     * @param magnetometerReading magnetometer reading
     * @return Compass heading in degrees (0-360)
     */
    public double calculateCompassHeading(MagnetometerReading magnetometerReading) {
        // Calculate heading from X and Y components
        double headingRad = Math.atan2(magnetometerReading.y, magnetometerReading.x);
        double headingDeg = Math.toDegrees(headingRad);
        
        // Convert to 0-360 range
        headingDeg = (headingDeg + 360.0) % 360.0;
        
        // Add heading error due to bias and interference
        double headingError = (magnetometerReading.bias[0] - magnetometerReading.bias[1]) / 
                            BASE_MAGNETIC_FIELD_UT * 10.0;
        headingDeg += headingError;
        
        // Normalize to 0-360
        headingDeg = (headingDeg + 360.0) % 360.0;
        
        return headingDeg;
    }
    
    /**
     * Generate Gaussian noise using Box-Muller transform.
     */
    private double generateGaussianNoise() {
        double u1 = random.nextDouble();
        double u2 = random.nextDouble();
        return Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2.0 * Math.PI * u2);
    }
    
    /**
     * Get current sensor state for telemetry.
     */
    public SensorState getSensorState() {
        return new SensorState(
            currentEnvironment,
            latitudeOffsetM,
            longitudeOffsetM,
            altitudeOffsetM,
            magnetometerBias.clone(),
            currentEMInterferenceUT,
            isInMagneticAnomaly
        );
    }
    
    /**
     * Force magnetometer calibration (simulates compass calibration).
     */
    public void forceMagnetometerCalibration() {
        // Reset bias
        magnetometerBias[0] = 0.0;
        magnetometerBias[1] = 0.0;
        magnetometerBias[2] = 0.0;
        
        // Reset drift
        magnetometerDrift[0] = 0.0;
        magnetometerDrift[1] = 0.0;
        magnetometerDrift[2] = 0.0;
        
        lastMagnetometerTimeMs = System.currentTimeMillis();
    }
    
    /**
     * Data class for GPS readings.
     */
    public static class GPSReading {
        public final double latitude;
        public final double longitude;
        public final double altitude;
        public final double accuracy;
        public final long timestamp;
        
        public GPSReading(double latitude, double longitude, double altitude,
                         double accuracy, long timestamp) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.altitude = altitude;
            this.accuracy = accuracy;
            this.timestamp = timestamp;
        }
    }
    
    /**
     * Data class for magnetometer readings.
     */
    public static class MagnetometerReading {
        public final double x;
        public final double y;
        public final double z;
        public final double magnitude;
        public final double[] bias;
        public final long timestamp;
        
        public MagnetometerReading(double x, double y, double z, double magnitude,
                                  double[] bias, long timestamp) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.magnitude = magnitude;
            this.bias = bias;
            this.timestamp = timestamp;
        }
    }
    
    /**
     * Data class for sensor state telemetry.
     */
    public static class SensorState {
        public final EnvironmentType environment;
        public final double gpsLatitudeOffsetM;
        public final double gpsLongitudeOffsetM;
        public final double gpsAltitudeOffsetM;
        public final double[] magnetometerBias;
        public final double emInterferenceUT;
        public final boolean isInMagneticAnomaly;
        
        public SensorState(EnvironmentType environment, double gpsLatitudeOffsetM,
                          double gpsLongitudeOffsetM, double gpsAltitudeOffsetM,
                          double[] magnetometerBias, double emInterferenceUT,
                          boolean isInMagneticAnomaly) {
            this.environment = environment;
            this.gpsLatitudeOffsetM = gpsLatitudeOffsetM;
            this.gpsLongitudeOffsetM = gpsLongitudeOffsetM;
            this.gpsAltitudeOffsetM = gpsAltitudeOffsetM;
            this.magnetometerBias = magnetometerBias;
            this.emInterferenceUT = emInterferenceUT;
            this.isInMagneticAnomaly = isInMagneticAnomaly;
        }
    }
}
