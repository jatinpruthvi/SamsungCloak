package com.samsungcloak.coherence;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Sensor-Fusion Coherence Hook for Samsung Galaxy A12 (SM-A125U)
 *
 * Implements a "Coherence Engine" that synchronizes GPS velocity with
 * Accelerometer/Gyroscope step-cycle noise to simulate realistic pedestrian
 * movement patterns using Pedestrian Dead Reckoning (PDR).
 *
 * This ensures that sensor telemetry remains coherent across all motion
 * sensors during simulated walking scenarios.
 */
public class SensorFusionCoherenceHook {

    private static final String LOG_TAG = "SamsungCloak.SensorFusionCoherence";
    private static boolean initialized = false;

    // Walking step cycle constants
    private static final double DEFAULT_STEP_FREQUENCY_HZ = 1.8; // Average human walking step frequency
    private static final double STEP_FREQUENCY_VARiability = 0.25; // +/- 0.25 Hz variation
    private static final double STEP_ACCELERATION_AMPLITUDE = 2.5; // m/s^2 peak acceleration during step
    private static final double STEP_GYRO_AMPLITUDE = 1.2; // rad/s peak angular velocity during step

    // Sensor coherence constants
    private static final double GPS_VELOCITY_CORRELATION_FACTOR = 0.85; // Correlation between GPS and accel
    private static final double STEP_CYCLE_PHASE_SYNC_ERROR = 0.15; // Phase error between sensors (radians)
    private static final double SENSOR_FUSION_UPDATE_RATE_HZ = 50.0; // 50Hz fusion update rate

    // PDR constants
    private static final double STRIDE_LENGTH_BASE_M = 0.7; // Average stride length in meters
    private static final double STRIDE_LENGTH_VARiability = 0.15; // +/- 15% variation
    private static final double STRIDE_FREQUENCY_CORRELATION = 0.92; // Correlation between stride freq and speed

    // Noise constants
    private static final double ACCELEROMETER_NOISE_STD_DEV = 0.08; // m/s^2
    private static final double GYROSCOPE_NOISE_STD_DEV = 0.02; // rad/s
    private static final double GPS_VELOCITY_NOISE_STD_DEV = 0.15; // m/s

    // Step detection thresholds
    private static final double STEP_DETECTION_THRESHOLD = 1.2; // m/s^2 threshold for step detection
    private static final double STEP_DETECTION_HYSTERESIS = 0.4; // m/s^2 hysteresis

    // Movement state
    public enum MovementState {
        STATIONARY,
        WALKING,
        RUNNING,
        WALKING_UP_STAIRS,
        WALKING_DOWN_STAIRS,
        TRANSITIONING
    }

    // Movement state parameters
    private static final double WALKING_SPEED_MIN_MS = 0.8;
    private static final double WALKING_SPEED_MAX_MS = 2.0;
    private static final double RUNNING_SPEED_MIN_MS = 2.5;
    private static final double RUNNING_SPEED_MAX_MS = 4.5;

    // Stair movement modifiers
    private static final double STAIR_ACCELERATION_MULTIPLIER = 1.5;
    private static final double STAIR_GYRO_MULTIPLIER = 1.3;

    private final long startTimeMs;
    private final java.util.Random random;

    // Current state
    private MovementState currentMovementState;
    private double currentStepFrequency;
    private double currentVelocity;
    private double currentHeading;
    private double stepPhase;

    // Sensor state
    private double[] accelerometerBias; // X, Y, Z bias
    private double[] gyroscopeBias; // X, Y, Z bias
    private double[] orientationQuaternion; // Current orientation (w, x, y, z)

    // PDR state
    private double[] position; // X, Y, Z position in meters (relative to start)
    private double[] lastVelocity; // Last velocity vector
    private int stepCount;
    private Deque<Long> stepTimestamps; // Timestamp of last steps for frequency calculation

    // Coherence monitoring
    private double gpsAccelCoherenceScore;
    private double stepCycleConsistencyScore;

    public SensorFusionCoherenceHook() {
        this.startTimeMs = System.currentTimeMillis();
        this.random = new java.util.Random(startTimeMs);

        // Initialize state
        this.currentMovementState = MovementState.STATIONARY;
        this.currentStepFrequency = 0.0;
        this.currentVelocity = 0.0;
        this.currentHeading = 0.0;
        this.stepPhase = 0.0;

        // Initialize sensor biases
        this.accelerometerBias = new double[]{0.0, 0.0, 0.0};
        this.gyroscopeBias = new double[]{0.0, 0.0, 0.0};
        this.orientationQuaternion = new double[]{1.0, 0.0, 0.0, 0.0}; // Identity quaternion

        // Initialize PDR state
        this.position = new double[]{0.0, 0.0, 0.0};
        this.lastVelocity = new double[]{0.0, 0.0, 0.0};
        this.stepCount = 0;
        this.stepTimestamps = new ArrayDeque<>(10);

        // Initialize coherence scores
        this.gpsAccelCoherenceScore = 1.0;
        this.stepCycleConsistencyScore = 1.0;

        // Apply initial sensor bias (simulates real-world sensor imperfections)
        initializeSensorBias();
    }

    /**
     * Initialize the sensor fusion coherence engine.
     */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
    }

    /**
     * Initialize sensor bias values (realistic sensor imperfections).
     */
    private void initializeSensorBias() {
        for (int i = 0; i < 3; i++) {
            accelerometerBias[i] = (random.nextDouble() - 0.5) * 0.15;
            gyroscopeBias[i] = (random.nextDouble() - 0.5) * 0.03;
        }
    }

    /**
     * Update movement state and sensor coherence.
     *
     * @param targetVelocity target velocity in m/s
     * @param targetHeading target heading in degrees (0-360)
     * @param newMovementState new movement state
     * @param currentTimeMs current timestamp in milliseconds
     * @return SensorFusionState with coherent sensor data
     */
    public SensorFusionState updateMovement(double targetVelocity, double targetHeading,
                                          MovementState newMovementState, long currentTimeMs) {
        // Update movement state with transition handling
        handleMovementStateTransition(newMovementState, currentTimeMs);

        // Calculate coherent step frequency based on velocity
        updateStepFrequency(targetVelocity);

        // Update step phase (time integration)
        updateStepPhase(currentTimeMs);

        // Generate coherent sensor readings
        double[] accelerometer = generateAccelerometerReading(currentTimeMs);
        double[] gyroscope = generateGyroscopeReading(currentTimeMs);
        double[] magnetometer = generateMagnetometerReading(currentTimeMs);

        // Generate GPS velocity reading coherent with accelerometer
        GPSVelocity gpsVelocity = generateGPSVelocity(targetVelocity, currentTimeMs);

        // Update PDR position estimate
        updatePDRPosition(accelerometer, gyroscope, currentTimeMs);

        // Calculate coherence metrics
        updateCoherenceMetrics(accelerometer, gpsVelocity);

        return new SensorFusionState(
            currentTimeMs,
            currentMovementState,
            currentVelocity,
            currentHeading,
            currentStepFrequency,
            stepPhase,
            accelerometer,
            gyroscope,
            magnetometer,
            gpsVelocity,
            position.clone(),
            stepCount,
            gpsAccelCoherenceScore,
            stepCycleConsistencyScore
        );
    }

    /**
     * Handle movement state transitions with realistic ramp-up/down.
     */
    private void handleMovementStateTransition(MovementState newState, long currentTimeMs) {
        if (newState == currentMovementState) {
            return;
        }

        // Check if this is a valid transition
        boolean validTransition = isValidTransition(currentMovementState, newState);

        if (!validTransition) {
            // Force transition through INTERMEDIATE state
            currentMovementState = MovementState.TRANSITIONING;
            return;
        }

        // Apply velocity ramp based on state transition
        double targetVelocity = getTargetVelocityForState(newState);

        // Apply realistic ramp (not instantaneous)
        double rampFactor = 0.3; // 30% of velocity change per update
        currentVelocity = currentVelocity + (targetVelocity - currentVelocity) * rampFactor;

        currentMovementState = newState;
    }

    /**
     * Check if movement state transition is valid.
     */
    private boolean isValidTransition(MovementState from, MovementState to) {
        // Can't go from stationary to running directly
        if (from == MovementState.STATIONARY && to == MovementState.RUNNING) {
            return false;
        }

        // Must go through walking for stair transitions
        if ((from == MovementState.WALKING || from == MovementState.RUNNING) &&
            (to == MovementState.WALKING_UP_STAIRS || to == MovementState.WALKING_DOWN_STAIRS)) {
            return false;
        }

        return true;
    }

    /**
     * Get target velocity for movement state.
     */
    private double getTargetVelocityForState(MovementState state) {
        switch (state) {
            case STATIONARY:
                return 0.0;
            case WALKING:
                return WALKING_SPEED_MIN_MS + random.nextDouble() * (WALKING_SPEED_MAX_MS - WALKING_SPEED_MIN_MS);
            case RUNNING:
                return RUNNING_SPEED_MIN_MS + random.nextDouble() * (RUNNING_SPEED_MAX_MS - RUNNING_SPEED_MIN_MS);
            case WALKING_UP_STAIRS:
                return WALKING_SPEED_MIN_MS * 0.6 + random.nextDouble() * (WALKING_SPEED_MAX_MS - WALKING_SPEED_MIN_MS) * 0.6;
            case WALKING_DOWN_STAIRS:
                return WALKING_SPEED_MIN_MS * 0.8 + random.nextDouble() * (WALKING_SPEED_MAX_MS - WALKING_SPEED_MIN_MS) * 0.8;
            case TRANSITIONING:
                return currentVelocity; // Maintain current velocity during transition
            default:
                return 0.0;
        }
    }

    /**
     * Update step frequency based on velocity with physiological correlation.
     */
    private void updateStepFrequency(double targetVelocity) {
        currentVelocity = targetVelocity;

        if (targetVelocity < 0.1) {
            currentStepFrequency = 0.0;
            return;
        }

        // Calculate step frequency from velocity (Froude number relationship)
        // f = g / (2 * pi) * sqrt(1 / L), where L is stride length
        double strideLength = STRIDE_LENGTH_BASE_M * (1.0 + (random.nextDouble() - 0.5) * STRIDE_LENGTH_VARiability);
        double baseFrequency = targetVelocity / strideLength;

        // Add variability
        double frequencyVariability = (random.nextDouble() - 0.5) * STEP_FREQUENCY_VARiability;

        // Apply frequency correlation (stride frequency doesn't increase linearly with speed)
        double frequencyCorrelation = Math.pow(targetVelocity / WALKING_SPEED_MIN_MS, 0.6);

        currentStepFrequency = baseFrequency * frequencyCorrelation + frequencyVariability;

        // Clamp to realistic range
        currentStepFrequency = Math.max(0.5, Math.min(3.5, currentStepFrequency));
    }

    /**
     * Update step phase for coherent sensor oscillation.
     */
    private void updateStepPhase(long currentTimeMs) {
        if (currentStepFrequency < 0.1) {
            stepPhase = 0.0;
            return;
        }

        // Calculate phase increment
        double phaseIncrement = (2.0 * Math.PI * currentStepFrequency) / SENSOR_FUSION_UPDATE_RATE_HZ;
        stepPhase += phaseIncrement;

        // Normalize to 0-2π
        stepPhase = (stepPhase + 2.0 * Math.PI) % (2.0 * Math.PI);
    }

    /**
     * Generate accelerometer reading with step-cycle coherence.
     */
    private double[] generateAccelerometerReading(long currentTimeMs) {
        double[] accel = new double[3];

        if (currentMovementState == MovementState.STATIONARY) {
            // Gravity only with sensor noise
            accel[0] = accelerometerBias[0] + generateGaussianNoise() * ACCELEROMETER_NOISE_STD_DEV;
            accel[1] = accelerometerBias[1] + generateGaussianNoise() * ACCELEROMETER_NOISE_STD_DEV;
            accel[2] = 9.81 + accelerometerBias[2] + generateGaussianNoise() * ACCELEROMETER_NOISE_STD_DEV;
        } else {
            // Add step-cycle oscillation
            double stepAmplitude = STEP_ACCELERATION_AMPLITUDE;

            // Apply modifiers for different movement states
            switch (currentMovementState) {
                case RUNNING:
                    stepAmplitude *= 2.2;
                    break;
                case WALKING_UP_STAIRS:
                case WALKING_DOWN_STAIRS:
                    stepAmplitude *= STAIR_ACCELERATION_MULTIPLIER;
                    break;
                default:
                    break;
            }

            // Vertical acceleration (dominant step component)
            accel[2] = 9.81 + stepAmplitude * Math.sin(stepPhase) +
                      accelerometerBias[2] + generateGaussianNoise() * ACCELEROMETER_NOISE_STD_DEV;

            // Forward-backward acceleration (smaller component)
            accel[0] = (stepAmplitude * 0.3) * Math.cos(stepPhase) +
                      accelerometerBias[0] + generateGaussianNoise() * ACCELEROMETER_NOISE_STD_DEV;

            // Lateral acceleration (minimal, for sway)
            accel[1] = (stepAmplitude * 0.1) * Math.sin(stepPhase * 0.5) +
                      accelerometerBias[1] + generateGaussianNoise() * ACCELEROMETER_NOISE_STD_DEV;
        }

        return accel;
    }

    /**
     * Generate gyroscope reading with step-cycle coherence.
     */
    private double[] generateGyroscopeReading(long currentTimeMs) {
        double[] gyro = new double[3];

        if (currentMovementState == MovementState.STATIONARY) {
            // Only bias and noise
            gyro[0] = gyroscopeBias[0] + generateGaussianNoise() * GYROSCOPE_NOISE_STD_DEV;
            gyro[1] = gyroscopeBias[1] + generateGaussianNoise() * GYROSCOPE_NOISE_STD_DEV;
            gyro[2] = gyroscopeBias[2] + generateGaussianNoise() * GYROSCOPE_NOISE_STD_DEV;
        } else {
            // Add step-cycle oscillation
            double gyroAmplitude = STEP_GYRO_AMPLITUDE;

            // Apply modifiers for different movement states
            switch (currentMovementState) {
                case RUNNING:
                    gyroAmplitude *= 2.0;
                    break;
                case WALKING_UP_STAIRS:
                case WALKING_DOWN_STAIRS:
                    gyroAmplitude *= STAIR_GYRO_MULTIPLIER;
                    break;
                default:
                    break;
            }

            // Pitch oscillation (forward-backward tilt during step)
            gyro[0] = gyroAmplitude * Math.sin(stepPhase + Math.PI / 4) +
                      gyroscopeBias[0] + generateGaussianNoise() * GYROSCOPE_NOISE_STD_DEV;

            // Roll oscillation (side-to-side sway)
            gyro[1] = (gyroAmplitude * 0.3) * Math.cos(stepPhase) +
                      gyroscopeBias[1] + generateGaussianNoise() * GYROSCOPE_NOISE_STD_DEV;

            // Yaw oscillation (heading changes during walking)
            gyro[2] = (gyroAmplitude * 0.2) * Math.sin(stepPhase * 0.7) +
                      gyroscopeBias[2] + generateGaussianNoise() * GYROSCOPE_NOISE_STD_DEV;
        }

        return gyro;
    }

    /**
     * Generate magnetometer reading with heading information.
     */
    private double[] generateMagnetometerReading(long currentTimeMs) {
        // Earth's magnetic field (approximate values for mid-latitudes)
        double[] earthField = new double[]{20.0, 0.0, 45.0}; // X, Y, Z in µT

        // Rotate by current heading
        double headingRad = Math.toRadians(currentHeading);
        double cosH = Math.cos(headingRad);
        double sinH = Math.sin(headingRad);

        double[] rotatedField = new double[3];
        rotatedField[0] = earthField[0] * cosH - earthField[1] * sinH;
        rotatedField[1] = earthField[0] * sinH + earthField[1] * cosH;
        rotatedField[2] = earthField[2];

        // Add sensor noise
        double[] mag = new double[3];
        for (int i = 0; i < 3; i++) {
            mag[i] = rotatedField[i] + generateGaussianNoise() * 0.3;
        }

        return mag;
    }

    /**
     * Generate GPS velocity reading coherent with accelerometer.
     */
    private GPSVelocity generateGPSVelocity(double trueVelocity, long currentTimeMs) {
        // Calculate coherent velocity from accelerometer integration
        double accelMagnitude = Math.abs(STEP_ACCELERATION_AMPLITUDE * Math.sin(stepPhase));
        double coherentVelocity = currentVelocity + (accelMagnitude * 0.1);

        // Add noise
        coherentVelocity += generateGaussianNoise() * GPS_VELOCITY_NOISE_STD_DEV;

        // Apply correlation factor
        double correlatedVelocity = coherentVelocity * GPS_VELOCITY_CORRELATION_FACTOR +
                                   trueVelocity * (1.0 - GPS_VELOCITY_CORRELATION_FACTOR);

        // Calculate velocity components
        double headingRad = Math.toRadians(currentHeading);
        double velocityX = correlatedVelocity * Math.cos(headingRad);
        double velocityY = correlatedVelocity * Math.sin(headingRad);
        double velocityZ = 0.0; // Assume level ground

        // Calculate accuracy estimate (worse at lower speeds)
        double accuracy = 0.1 + (1.0 / (correlatedVelocity + 0.1)) * 0.3;

        return new GPSVelocity(velocityX, velocityY, velocityZ, correlatedVelocity, accuracy, currentTimeMs);
    }

    /**
     * Update PDR position estimate using sensor fusion.
     */
    private void updatePDRPosition(double[] accel, double[] gyro, long currentTimeMs) {
        // Step detection using accelerometer
        double accelVertical = accel[2] - 9.81;
        boolean isStep = Math.abs(accelVertical) > STEP_DETECTION_THRESHOLD;

        if (isStep && stepPhase < 0.5) {
            // New step detected
            stepCount++;

            // Calculate stride length
            double strideLength = STRIDE_LENGTH_BASE_M * (1.0 + (random.nextDouble() - 0.5) * STRIDE_LENGTH_VARiability);

            // Update position based on heading
            double headingRad = Math.toRadians(currentHeading);
            position[0] += strideLength * Math.cos(headingRad);
            position[1] += strideLength * Math.sin(headingRad);

            // Record step timestamp
            stepTimestamps.addLast(currentTimeMs);
            if (stepTimestamps.size() > 10) {
                stepTimestamps.removeFirst();
            }
        }
    }

    /**
     * Update coherence metrics.
     */
    private void updateCoherenceMetrics(double[] accel, GPSVelocity gpsVelocity) {
        // Calculate GPS-accelerometer coherence
        double accelMagnitude = Math.sqrt(accel[0] * accel[0] + accel[1] * accel[1] + accel[2] * accel[2]);
        double accelVelocityEstimate = (accelMagnitude - 9.81) * 0.5; // Rough velocity estimate

        double coherenceError = Math.abs(accelVelocityEstimate - gpsVelocity.velocity) / (gpsVelocity.velocity + 0.1);
        gpsAccelCoherenceScore = Math.max(0.0, 1.0 - coherenceError);

        // Calculate step cycle consistency
        if (stepTimestamps.size() >= 3) {
            double avgStepInterval = 0.0;
            for (int i = 1; i < stepTimestamps.size(); i++) {
                avgStepInterval += (stepTimestamps.get(i) - stepTimestamps.get(i - 1));
            }
            avgStepInterval /= (stepTimestamps.size() - 1);

            double calculatedFrequency = 1000.0 / avgStepInterval; // Hz
            double frequencyError = Math.abs(calculatedFrequency - currentStepFrequency) / currentStepFrequency;
            stepCycleConsistencyScore = Math.max(0.0, 1.0 - frequencyError);
        }
    }

    /**
     * Generate Gaussian noise.
     */
    private double generateGaussianNoise() {
        double u1 = random.nextDouble();
        double u2 = random.nextDouble();
        return Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2.0 * Math.PI * u2);
    }

    /**
     * Get current coherence metrics.
     */
    public CoherenceMetrics getCoherenceMetrics() {
        return new CoherenceMetrics(
            gpsAccelCoherenceScore,
            stepCycleConsistencyScore,
            currentStepFrequency,
            stepCount,
            currentMovementState
        );
    }

    /**
     * Reset PDR state.
     */
    public void resetPDR() {
        position = new double[]{0.0, 0.0, 0.0};
        stepCount = 0;
        stepTimestamps.clear();
    }

    /**
     * Data class for GPS velocity.
     */
    public static class GPSVelocity {
        public final double velocityX;
        public final double velocityY;
        public final double velocityZ;
        public final double velocity;
        public final double accuracy;
        public final long timestamp;

        public GPSVelocity(double velocityX, double velocityY, double velocityZ,
                          double velocity, double accuracy, long timestamp) {
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.velocityZ = velocityZ;
            this.velocity = velocity;
            this.accuracy = accuracy;
            this.timestamp = timestamp;
        }
    }

    /**
     * Data class for sensor fusion state.
     */
    public static class SensorFusionState {
        public final long timestamp;
        public final MovementState movementState;
        public final double velocity;
        public final double heading;
        public final double stepFrequency;
        public final double stepPhase;
        public final double[] accelerometer;
        public final double[] gyroscope;
        public final double[] magnetometer;
        public final GPSVelocity gpsVelocity;
        public final double[] position;
        public final int stepCount;
        public final double gpsAccelCoherence;
        public final double stepCycleConsistency;

        public SensorFusionState(long timestamp, MovementState movementState, double velocity,
                               double heading, double stepFrequency, double stepPhase,
                               double[] accelerometer, double[] gyroscope, double[] magnetometer,
                               GPSVelocity gpsVelocity, double[] position, int stepCount,
                               double gpsAccelCoherence, double stepCycleConsistency) {
            this.timestamp = timestamp;
            this.movementState = movementState;
            this.velocity = velocity;
            this.heading = heading;
            this.stepFrequency = stepFrequency;
            this.stepPhase = stepPhase;
            this.accelerometer = accelerometer;
            this.gyroscope = gyroscope;
            this.magnetometer = magnetometer;
            this.gpsVelocity = gpsVelocity;
            this.position = position;
            this.stepCount = stepCount;
            this.gpsAccelCoherence = gpsAccelCoherence;
            this.stepCycleConsistency = stepCycleConsistency;
        }
    }

    /**
     * Data class for coherence metrics.
     */
    public static class CoherenceMetrics {
        public final double gpsAccelCoherenceScore;
        public final double stepCycleConsistencyScore;
        public final double currentStepFrequency;
        public final int stepCount;
        public final MovementState movementState;

        public CoherenceMetrics(double gpsAccelCoherenceScore, double stepCycleConsistencyScore,
                               double currentStepFrequency, int stepCount, MovementState movementState) {
            this.gpsAccelCoherenceScore = gpsAccelCoherenceScore;
            this.stepCycleConsistencyScore = stepCycleConsistencyScore;
            this.currentStepFrequency = currentStepFrequency;
            this.stepCount = stepCount;
            this.movementState = movementState;
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
