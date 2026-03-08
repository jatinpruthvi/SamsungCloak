package com.simulation.structural.sanity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ThresholdMonitor - Telemetry Sanity Checking System
 *
 * Prevents mechanical anomalies (e.g., 0ms reaction times) by ensuring all
 * interactions stay within the physical constraints of human motor capabilities.
 * Validates telemetry against known physiological and cognitive limits.
 *
 * Physical Constraints:
 *
 * 1. Simple Reaction Time:
 *    - Minimum: 150ms (physiological lower bound for visual stimuli)
 *    - Typical: 200-250ms
 *    - Maximum: 1000ms (attention lapse threshold)
 *
 * 2. Choice Reaction Time:
 *    - Minimum: 200ms
 *    - Typical: 300-400ms
 *    - Adds ~100ms per additional choice (Hick's Law)
 *
 * 3. Movement Time (Fitts' Law):
 *    - Minimum: 80ms for simple taps
 *    - Scales with distance and target size
 *    - MT = a + b × log₂(2D/W)
 *
 * 4. Typing Cadence:
 *    - Maximum: 12-15 characters/second (expert)
 *    - Typical: 4-6 characters/second
 *    - Inter-key interval: 70ms minimum
 *
 * 5. Scroll Velocity:
 *    - Maximum: ~3000px/second (sustained)
 *    - Burst peaks: up to 5000px/second
 *    - Deceleration follows power law
 *
 * 6. Touch Pressure:
 *    - Minimum detectable: 0.1 (normalized)
 *    - Typical: 0.3-0.7
 *    - Maximum sustained: 0.95
 *
 * Validation Levels:
 * - CRITICAL: Hard bounds (physiologically impossible)
 * - WARNING: Soft bounds (statistically anomalous)
 * - INFO: Quality markers (best practice deviations)
 */
public class ThresholdMonitor {

    private static final String LOG_TAG = "StructuralFidelity.ThresholdMonitor";

    // Hard physiological bounds (CRITICAL violations)
    public static final long MIN_SIMPLE_REACTION_MS = 150;
    public static final long MIN_CHOICE_REACTION_MS = 200;
    public static final long MAX_REACTION_TIME_MS = 3000;

    public static final long MIN_MOVEMENT_TIME_MS = 80;
    public static final long MAX_MOVEMENT_TIME_MS = 2000;

    public static final long MIN_INTER_KEY_MS = 70;
    public static final long MAX_TYPING_SPEED_CPS = 15;

    public static final double MIN_TOUCH_PRESSURE = 0.05;
    public static final double MAX_TOUCH_PRESSURE = 1.0;

    public static final double MAX_SCROLL_VELOCITY_PX_S = 5000;
    public static final double MAX_ACCEL_CHANGE_PX_S2 = 15000;

    // Soft statistical bounds (WARNING anomalies)
    public static final long TYPICAL_MIN_REACTION_MS = 180;
    public static final long TYPICAL_MAX_REACTION_MS = 800;
    public static final double TYPICAL_MIN_PRESSURE = 0.15;
    public static final double TYPICAL_MAX_PRESSURE = 0.85;

    private final Map<String, InteractionConstraint> constraints;
    private final List<SanityViolation> violations;
    private final ViolationHandler handler;

    public ThresholdMonitor() {
        this(ViolationHandler.LOG_AND_CORRECT);
    }

    public ThresholdMonitor(ViolationHandler handler) {
        this.constraints = new ConcurrentHashMap<>();
        this.violations = Collections.synchronizedList(new ArrayList<>());
        this.handler = handler;
        initializeDefaultConstraints();
    }

    /**
     * Interaction constraint definition
     */
    public static class InteractionConstraint {
        private final InteractionType type;
        private final double minValue;
        private final double maxValue;
        private final double typicalMin;
        private final double typicalMax;
        private final String unit;

        public InteractionConstraint(InteractionType type, double minValue, double maxValue,
                                      double typicalMin, double typicalMax, String unit) {
            this.type = type;
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.typicalMin = typicalMin;
            this.typicalMax = typicalMax;
            this.unit = unit;
        }

        public ValidationResult validate(double value, String context) {
            List<ViolationLevel> violations = new ArrayList<>();

            if (value < minValue || value > maxValue) {
                violations.add(ViolationLevel.CRITICAL);
            } else if (value < typicalMin || value > typicalMax) {
                violations.add(ViolationLevel.WARNING);
            }

            if (violations.isEmpty()) {
                return ValidationResult.valid(type, value);
            }

            double corrected = Math.max(minValue, Math.min(maxValue, value));
            return new ValidationResult(false, violations, type, value, corrected);
        }

        public InteractionType getType() { return type; }
        public double getMinValue() { return minValue; }
        public double getMaxValue() { return maxValue; }
    }

    /**
     * Validation result with correction
     */
    public static class ValidationResult {
        public final boolean valid;
        public final List<ViolationLevel> violations;
        public final InteractionType type;
        public final double originalValue;
        public final double correctedValue;

        public ValidationResult(boolean valid, List<ViolationLevel> violations,
                                InteractionType type, double originalValue,
                                double correctedValue) {
            this.valid = valid;
            this.violations = violations;
            this.type = type;
            this.originalValue = originalValue;
            this.correctedValue = correctedValue;
        }

        public static ValidationResult valid(InteractionType type, double value) {
            return new ValidationResult(true, Collections.emptyList(), type, value, value);
        }

        public boolean hasCriticalViolation() {
            return violations.contains(ViolationLevel.CRITICAL);
        }
    }

    public enum InteractionType {
        REACTION_TIME,
        CHOICE_REACTION_TIME,
        MOVEMENT_TIME,
        INTER_KEY_INTERVAL,
        TYPING_SPEED,
        TOUCH_PRESSURE,
        SCROLL_VELOCITY,
        SCROLL_ACCELERATION,
        TAP_COORDINATE_JITTER,
        GESTURE_DURATION
    }

    public enum ViolationLevel {
        INFO, WARNING, CRITICAL
    }

    public enum ViolationHandler {
        LOG_ONLY,
        LOG_AND_CORRECT,
        REJECT_AND_LOG,
        THROW_EXCEPTION
    }

    /**
     * Record of a sanity violation
     */
    public static class SanityViolation {
        public final long timestamp;
        public final InteractionType type;
        public final ViolationLevel level;
        public final double originalValue;
        public final double correctedValue;
        public final String context;

        public SanityViolation(InteractionType type, ViolationLevel level,
                               double originalValue, double correctedValue,
                               String context) {
            this.timestamp = System.currentTimeMillis();
            this.type = type;
            this.level = level;
            this.originalValue = originalValue;
            this.correctedValue = correctedValue;
            this.context = context;
        }
    }

    private void initializeDefaultConstraints() {
        constraints.put(InteractionType.REACTION_TIME.name(),
            new InteractionConstraint(InteractionType.REACTION_TIME,
                MIN_SIMPLE_REACTION_MS, MAX_REACTION_TIME_MS,
                TYPICAL_MIN_REACTION_MS, TYPICAL_MAX_REACTION_MS, "ms"));

        constraints.put(InteractionType.CHOICE_REACTION_TIME.name(),
            new InteractionConstraint(InteractionType.CHOICE_REACTION_TIME,
                MIN_CHOICE_REACTION_MS, MAX_REACTION_TIME_MS,
                250, 600, "ms"));

        constraints.put(InteractionType.MOVEMENT_TIME.name(),
            new InteractionConstraint(InteractionType.MOVEMENT_TIME,
                MIN_MOVEMENT_TIME_MS, MAX_MOVEMENT_TIME_MS,
                100, 500, "ms"));

        constraints.put(InteractionType.INTER_KEY_INTERVAL.name(),
            new InteractionConstraint(InteractionType.INTER_KEY_INTERVAL,
                MIN_INTER_KEY_MS, 500,
                100, 300, "ms"));

        constraints.put(InteractionType.TYPING_SPEED.name(),
            new InteractionConstraint(InteractionType.TYPING_SPEED,
                0.5, MAX_TYPING_SPEED_CPS,
                2, 8, "cps"));

        constraints.put(InteractionType.TOUCH_PRESSURE.name(),
            new InteractionConstraint(InteractionType.TOUCH_PRESSURE,
                MIN_TOUCH_PRESSURE, MAX_TOUCH_PRESSURE,
                TYPICAL_MIN_PRESSURE, TYPICAL_MAX_PRESSURE, "normalized"));

        constraints.put(InteractionType.SCROLL_VELOCITY.name(),
            new InteractionConstraint(InteractionType.SCROLL_VELOCITY,
                0, MAX_SCROLL_VELOCITY_PX_S,
                100, 2500, "px/s"));

        constraints.put(InteractionType.SCROLL_ACCELERATION.name(),
            new InteractionConstraint(InteractionType.SCROLL_ACCELERATION,
                -MAX_ACCEL_CHANGE_PX_S2, MAX_ACCEL_CHANGE_PX_S2,
                -5000, 5000, "px/s²"));
    }

    /**
     * Validate reaction time
     */
    public long validateReactionTime(long reactionMs, String context) {
        return validate(InteractionType.REACTION_TIME, reactionMs, context);
    }

    /**
     * Validate choice reaction time (with n options)
     */
    public long validateChoiceReactionTime(long reactionMs, int numChoices, String context) {
        double hickPenalty = 100 * Math.log(numChoices) / Math.log(2);
        long adjustedMin = (long) (MIN_CHOICE_REACTION_MS + hickPenalty);

        InteractionConstraint constraint = new InteractionConstraint(
            InteractionType.CHOICE_REACTION_TIME,
            adjustedMin, MAX_REACTION_TIME_MS,
            adjustedMin + 50, 800, "ms"
        );

        ValidationResult result = constraint.validate(reactionMs, context);
        return processResult(result, context);
    }

    /**
     * Validate movement time using Fitts' Law
     */
    public long validateMovementTime(long movementMs, double distancePx, double targetSizePx,
                                      String context) {
        double difficultyIndex = Math.log(2 * distancePx / Math.max(targetSizePx, 10)) / Math.log(2);
        double fittsMin = 80 + 100 * difficultyIndex;

        InteractionConstraint constraint = new InteractionConstraint(
            InteractionType.MOVEMENT_TIME,
            fittsMin, MAX_MOVEMENT_TIME_MS,
            fittsMin * 1.2, fittsMin * 3, "ms"
        );

        ValidationResult result = constraint.validate(movementMs, context);
        return processResult(result, context);
    }

    /**
     * Validate touch pressure
     */
    public double validateTouchPressure(double pressure, String context) {
        return validate(InteractionType.TOUCH_PRESSURE, pressure, context);
    }

    /**
     * Validate scroll velocity
     */
    public double validateScrollVelocity(double velocityPxS, String context) {
        return validate(InteractionType.SCROLL_VELOCITY, velocityPxS, context);
    }

    /**
     * Validate typing speed
     */
    public double validateTypingSpeed(double charsPerSecond, String context) {
        return validate(InteractionType.TYPING_SPEED, charsPerSecond, context);
    }

    /**
     * Validate inter-key interval
     */
    public long validateInterKeyInterval(long intervalMs, String context) {
        return validate(InteractionType.INTER_KEY_INTERVAL, intervalMs, context);
    }

    /**
     * Generic validation method
     */
    @SuppressWarnings("unchecked")
    public <T extends Number> T validate(InteractionType type, T value, String context) {
        InteractionConstraint constraint = constraints.get(type.name());
        if (constraint == null) {
            return value;
        }

        ValidationResult result = constraint.validate(value.doubleValue(), context);
        double corrected = processResult(result, context);

        if (value instanceof Long) {
            return (T) Long.valueOf((long) corrected);
        } else if (value instanceof Integer) {
            return (T) Integer.valueOf((int) corrected);
        } else if (value instanceof Double) {
            return (T) Double.valueOf(corrected);
        } else if (value instanceof Float) {
            return (T) Float.valueOf((float) corrected);
        }

        return value;
    }

    private double processResult(ValidationResult result, String context) {
        if (result.valid) {
            return result.originalValue;
        }

        for (ViolationLevel level : result.violations) {
            SanityViolation violation = new SanityViolation(
                result.type, level, result.originalValue, result.correctedValue, context
            );
            violations.add(violation);

            switch (handler) {
                case LOG_ONLY:
                    logViolation(violation);
                    break;
                case LOG_AND_CORRECT:
                    logViolation(violation);
                    return result.correctedValue;
                case REJECT_AND_LOG:
                    logViolation(violation);
                    return Double.NaN;
                case THROW_EXCEPTION:
                    throw new ThresholdViolationException(violation);
            }
        }

        return result.correctedValue;
    }

    private void logViolation(SanityViolation violation) {
        String message = String.format(
            "[%s] %s violation for %s: value=%.2f, corrected=%.2f, context=%s",
            new Date(violation.timestamp),
            violation.level,
            violation.type,
            violation.originalValue,
            violation.correctedValue,
            violation.context
        );
        System.out.println(message);
    }

    /**
     * Validate complete touch event
     */
    public TouchValidationResult validateTouchEvent(long pressDuration, double pressure,
                                                     double x, double y, String context) {
        long validDuration = validateReactionTime(pressDuration, context + "/duration");
        double validPressure = validateTouchPressure(pressure, context + "/pressure");

        boolean hasJitter = detectCoordinateJitter(x, y, context);

        return new TouchValidationResult(validDuration, validPressure, hasJitter);
    }

    private boolean detectCoordinateJitter(double x, double y, String context) {
        return false;
    }

    public static class TouchValidationResult {
        public final long validDuration;
        public final double validPressure;
        public final boolean hasJitterAnomaly;

        public TouchValidationResult(long validDuration, double validPressure,
                                      boolean hasJitterAnomaly) {
            this.validDuration = validDuration;
            this.validPressure = validPressure;
            this.hasJitterAnomaly = hasJitterAnomaly;
        }
    }

    /**
     * Validate scroll gesture physics
     */
    public ScrollValidationResult validateScrollGesture(double velocity, double acceleration,
                                                         double deceleration, String context) {
        double validVelocity = validateScrollVelocity(velocity, context + "/velocity");
        double validAccel = validate(InteractionType.SCROLL_ACCELERATION, acceleration,
            context + "/acceleration");

        boolean realisticDecel = validateDeceleration(velocity, deceleration);

        return new ScrollValidationResult(validVelocity, validAccel, realisticDecel);
    }

    private boolean validateDeceleration(double velocity, double deceleration) {
        double expectedDecel = velocity * 0.85;
        double tolerance = velocity * 0.3;
        return Math.abs(deceleration - expectedDecel) < tolerance;
    }

    public static class ScrollValidationResult {
        public final double validVelocity;
        public final double validAcceleration;
        public final boolean realisticDeceleration;

        public ScrollValidationResult(double validVelocity, double validAcceleration,
                                       boolean realisticDeceleration) {
            this.validVelocity = validVelocity;
            this.validAcceleration = validAcceleration;
            this.realisticDeceleration = realisticDeceleration;
        }
    }

    /**
     * Get violation statistics
     */
    public ViolationStatistics getViolationStatistics() {
        Map<InteractionType, Integer> counts = new HashMap<>();
        Map<ViolationLevel, Integer> levelCounts = new HashMap<>();

        for (SanityViolation v : violations) {
            counts.merge(v.type, 1, Integer::sum);
            levelCounts.merge(v.level, 1, Integer::sum);
        }

        return new ViolationStatistics(
            violations.size(),
            levelCounts.getOrDefault(ViolationLevel.CRITICAL, 0),
            levelCounts.getOrDefault(ViolationLevel.WARNING, 0),
            counts
        );
    }

    public static class ViolationStatistics {
        public final int totalViolations;
        public final int criticalCount;
        public final int warningCount;
        public final Map<InteractionType, Integer> violationsByType;

        public ViolationStatistics(int totalViolations, int criticalCount, int warningCount,
                                    Map<InteractionType, Integer> violationsByType) {
            this.totalViolations = totalViolations;
            this.criticalCount = criticalCount;
            this.warningCount = warningCount;
            this.violationsByType = violationsByType;
        }

        @Override
        public String toString() {
            return String.format(
                "ViolationStatistics{total=%d, critical=%d, warning=%d}",
                totalViolations, criticalCount, warningCount
            );
        }
    }

    public List<SanityViolation> getViolations() {
        return new ArrayList<>(violations);
    }

    public void clearViolations() {
        violations.clear();
    }

    /**
     * Exception for critical threshold violations
     */
    public static class ThresholdViolationException extends RuntimeException {
        public final SanityViolation violation;

        public ThresholdViolationException(SanityViolation violation) {
            super(String.format("Critical threshold violation: %s = %.2f (context: %s)",
                violation.type, violation.originalValue, violation.context));
            this.violation = violation;
        }
    }
}
