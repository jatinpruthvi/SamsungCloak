package com.samsungcloak.coherence;

import java.util.Random;

/**
 * Mechanical Micro-Error Simulation Hook for Samsung Galaxy A12 (SM-A125U)
 *
 * Simulates realistic human motor imperfections in touch interactions including:
 * 1. Fat-Finger events - near-miss tap coordinates with slight spatial offsets
 * 2. Partial button-press cancellations - aborted touch events due to hesitation
 * 3. Correction Swipes - overshoot scroll compensation behaviors
 *
 * These micro-errors provide high-fidelity human-like telemetry for behavioral analysis.
 */
public class MechanicalMicroErrorHook {

    private static final String LOG_TAG = "SamsungCloak.MechanicalMicroError";
    private static boolean initialized = false;

    // Fat-finger simulation constants
    private static final double FAT_FINGER_OFFSET_STD_DEV_PX = 8.5; // Pixel spread for finger contact area
    private static final double FAT_FINGER_PROBABILITY = 0.18; // 18% of taps have spatial offset
    private static final double NEAR_MISS_THRESHOLD_PX = 15.0; // Distance considered a "near miss"

    // Partial press cancellation constants
    private static final double PARTIAL_PRESS_CANCELLATION_PROBABILITY = 0.08; // 8% of touches are aborted
    private static final double MIN_PARTIAL_PRESS_DURATION_MS = 50.0;
    private static final double MAX_PARTIAL_PRESS_DURATION_MS = 150.0;

    // Correction swipe constants
    private static final double OVERSHOOT_SCROLL_PROBABILITY = 0.22; // 22% of scrolls overshoot
    private static final double OVERSHOOT_MAGITUDE_RATIO = 0.35; // Overshoot as ratio of intended scroll
    private static final double CORrection_SWIPE_DURATION_MS = 180.0; // Typical correction duration
    private static final double CORrection_MAGITUDE_RATIO = 0.85; // Correction as ratio of overshoot

    // Touch jitter constants
    private static final double TOUCH_JITTER_STD_DEV_PX = 2.3; // Sub-pixel hand tremor
    private static final double PALM_REJECTION_PROBABILITY = 0.03; // 3% chance of palm contact

    // Human motor constants
    private static final double FINGER_WIDTH_AVG_PX = 12.0;
    private static final double FINGER_HEIGHT_AVG_PX = 18.0;
    private static final double FINGER_PRESSURE_VARiability = 0.4;

    private final Random random;
    private double currentFatFingerOffsetX;
    private double currentFatFingerOffsetY;
    private long lastTouchTimeMs;
    private boolean isInCorrectionSwipe;
    private double correctionVelocityY;

    public enum TouchPhase {
        DOWN,
        MOVE,
        UP,
        CANCEL
    }

    public MechanicalMicroErrorHook() {
        this.random = new Random();
        this.currentFatFingerOffsetX = 0.0;
        this.currentFatFingerOffsetY = 0.0;
        this.lastTouchTimeMs = System.currentTimeMillis();
        this.isInCorrectionSwipe = false;
        this.correctionVelocityY = 0.0;
    }

    /**
     * Initialize the mechanical micro-error simulation.
     */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
    }

    /**
     * Simulate a tap event with fat-finger offset.
     *
     * @param targetX intended X coordinate
     * @param targetY intended Y coordinate
     * @return TapResult with potentially offset coordinates and correction data
     */
    public TapResult simulateTap(double targetX, double targetY) {
        lastTouchTimeMs = System.currentTimeMillis();

        // Determine if this tap has fat-finger offset
        boolean hasFatFingerOffset = random.nextDouble() < FAT_FINGER_PROBABILITY;
        double offsetX = 0.0;
        double offsetY = 0.0;

        if (hasFatFingerOffset) {
            // Generate Gaussian-distributed offset
            offsetX = generateGaussianNoise() * FAT_FINGER_OFFSET_STD_DEV_PX;
            offsetY = generateGaussianNoise() * FAT_FINGER_OFFSET_STD_DEV_PX;

            // Store for subsequent move events
            currentFatFingerOffsetX = offsetX;
            currentFatFingerOffsetY = offsetY;
        } else {
            // Minor touch jitter
            offsetX = generateGaussianNoise() * TOUCH_JITTER_STD_DEV_PX;
            offsetY = generateGaussianNoise() * TOUCH_JITTER_STD_DEV_PX;
        }

        double actualX = targetX + offsetX;
        double actualY = targetY + offsetY;

        // Calculate distance from target (for near-miss detection)
        double distance = Math.sqrt(offsetX * offsetX + offsetY * offsetY);
        boolean isNearMiss = distance > NEAR_MISS_THRESHOLD_PX;

        // Determine if this should be a partial press cancellation
        boolean isPartialPress = random.nextDouble() < PARTIAL_PRESS_CANCELLATION_PROBABILITY;

        return new TapResult(
            actualX,
            actualY,
            offsetX,
            offsetY,
            hasFatFingerOffset,
            isNearMiss,
            isPartialPress,
            isPartialPress ? simulatePartialPressDuration() : 0,
            lastTouchTimeMs
        );
    }

    /**
     * Simulate a touch move event with correction swipe logic.
     *
     * @param currentX current X coordinate
     * @param currentY current Y coordinate
     * @param velocityX X velocity in pixels/ms
     * @param velocityY Y velocity in pixels/ms
     * @return MoveResult with correction swipe data if applicable
     */
    public MoveResult simulateMove(double currentX, double currentY, double velocityX, double velocityY) {
        long currentTimeMs = System.currentTimeMillis();
        long timeDelta = currentTimeMs - lastTouchTimeMs;
        lastTouchTimeMs = currentTimeMs;

        // Apply fat-finger offset to initial move
        double adjustedX = currentX + currentFatFingerOffsetX;
        double adjustedY = currentY + currentFatFingerOffsetY;

        // Check for scroll overshoot detection
        double scrollVelocity = Math.sqrt(velocityX * velocityX + velocityY * velocityY);
        boolean isOvershooting = false;
        double overshootDistance = 0.0;

        if (scrollVelocity > 0.5 && !isInCorrectionSwipe) {
            // Detect potential overshoot based on velocity and scroll duration
            if (random.nextDouble() < OVERSHOOT_SCROLL_PROBABILITY) {
                isOvershooting = true;
                overshootDistance = scrollVelocity * CORrection_SWIPE_DURATION_MS * OVERSHOOT_MAGITUDE_RATIO;

                // Initialize correction swipe
                isInCorrectionSwipe = true;
                correctionVelocityY = -velocityY * CORrection_MAGITUDE_RATIO;
            }
        }

        // Apply correction if in correction swipe phase
        double correctedX = adjustedX;
        double correctedY = adjustedY;

        if (isInCorrectionSwipe) {
            // Apply correction velocity (opposite to overshoot direction)
            double correctionStep = correctionVelocityY * (timeDelta / CORrection_SWIPE_DURATION_MS);
            correctedY += correctionStep;

            // Check if correction is complete
            if (Math.abs(correctionStep) < 0.1) {
                isInCorrectionSwipe = false;
                correctionVelocityY = 0.0;
            }
        }

        // Apply palm rejection check
        boolean isPalmContact = random.nextDouble() < PALM_REJECTION_PROBABILITY;

        return new MoveResult(
            correctedX,
            correctedY,
            isOvershooting,
            overshootDistance,
            isInCorrectionSwipe,
            isPalmContact,
            currentTimeMs
        );
    }

    /**
     * Simulate a scroll event with overshoot and correction behavior.
     *
     * @param startX start X coordinate
     * @param startY start Y coordinate
     * @param endX end X coordinate
     * @param endY end Y coordinate
     * @param durationMs scroll duration in milliseconds
     * @return ScrollResult with overshoot and correction data
     */
    public ScrollResult simulateScroll(double startX, double startY, double endX, double endY, double durationMs) {
        // Calculate scroll distance and direction
        double scrollDistanceX = endX - startX;
        double scrollDistanceY = endY - startY;
        double scrollDistance = Math.sqrt(scrollDistanceX * scrollDistanceX + scrollDistanceY * scrollDistanceY);

        // Determine if this scroll should have overshoot
        boolean hasOvershoot = scrollDistance > 50.0 && random.nextDouble() < OVERSHOOT_SCROLL_PROBABILITY;

        double overshootDistance = 0.0;
        double correctionDistance = 0.0;
        double correctionDelayMs = 0.0;

        if (hasOvershoot) {
            // Calculate overshoot magnitude
            overshootDistance = scrollDistance * OVERSHOOT_MAGITUDE_RATIO;

            // Calculate correction magnitude
            correctionDistance = overshootDistance * CORrection_MAGITUDE_RATIO;

            // Calculate correction delay (human reaction time + motor planning)
            correctionDelayMs = 120.0 + random.nextDouble() * 80.0;
        }

        // Apply subtle finger drag acceleration/deceleration profile
        double velocityProfileFactor = calculateVelocityProfileFactor(durationMs);

        return new ScrollResult(
            startX,
            startY,
            endX,
            endY,
            scrollDistance,
            hasOvershoot,
            overshootDistance,
            correctionDistance,
            correctionDelayMs,
            velocityProfileFactor,
            System.currentTimeMillis()
        );
    }

    /**
     * Simulate touch pressure variation based on interaction type.
     *
     * @param interactionType type of interaction (tap, scroll, long-press)
     * @return Pressure value in range 0-1 (normalized)
     */
    public double simulatePressure(InteractionType interactionType) {
        double basePressure;

        switch (interactionType) {
            case BUTTON_TAP:
                // Firm pressure for button taps
                basePressure = 0.75 + random.nextDouble() * 0.20;
                break;
            case LINK_TAP:
                // Medium pressure for link taps
                basePressure = 0.55 + random.nextDouble() * 0.25;
                break;
            case SCROLL:
                // Light pressure for scrolling
                basePressure = 0.30 + random.nextDouble() * 0.30;
                break;
            case LONG_PRESS:
                // Sustained pressure for long press
                basePressure = 0.65 + random.nextDouble() * 0.25;
                break;
            case SWIPE:
                // Variable pressure for swipes
                basePressure = 0.40 + random.nextDouble() * 0.35;
                break;
            default:
                basePressure = 0.50 + random.nextDouble() * 0.40;
        }

        // Apply pressure variability based on finger contact area
        basePressure += generateGaussianNoise() * FINGER_PRESSURE_VARiability;

        // Clamp to valid range
        return Math.max(0.0, Math.min(1.0, basePressure));
    }

    /**
     * Simulate touch surface area (touch major/minor axes).
     *
     * @param interactionType type of interaction
     * @return TouchArea with major and minor axis values
     */
    public TouchArea simulateTouchArea(InteractionType interactionType) {
        double majorAxis;
        double minorAxis;

        switch (interactionType) {
            case BUTTON_TAP:
                // Compact contact for buttons
                majorAxis = FINGER_WIDTH_AVG_PX + random.nextDouble() * 4.0;
                minorAxis = FINGER_WIDTH_AVG_PX * 0.7 + random.nextDouble() * 3.0;
                break;
            case SCROLL:
                // Elongated contact for scrolling
                majorAxis = FINGER_HEIGHT_AVG_PX + random.nextDouble() * 6.0;
                minorAxis = FINGER_WIDTH_AVG_PX + random.nextDouble() * 4.0;
                break;
            case LONG_PRESS:
                // Expanding contact for long press
                majorAxis = FINGER_WIDTH_AVG_PX * 1.3 + random.nextDouble() * 5.0;
                minorAxis = FINGER_WIDTH_AVG_PX * 1.2 + random.nextDouble() * 4.0;
                break;
            case SWIPE:
                // Dynamic contact for swipe
                majorAxis = FINGER_HEIGHT_AVG_PX * 0.8 + random.nextDouble() * 5.0;
                minorAxis = FINGER_WIDTH_AVG_PX + random.nextDouble() * 3.0;
                break;
            default:
                majorAxis = FINGER_WIDTH_AVG_PX + random.nextDouble() * 5.0;
                minorAxis = FINGER_WIDTH_AVG_PX + random.nextDouble() * 4.0;
        }

        return new TouchArea(majorAxis, minorAxis);
    }

    /**
     * Simulate a partial press cancellation duration.
     */
    private double simulatePartialPressDuration() {
        return MIN_PARTIAL_PRESS_DURATION_MS +
               random.nextDouble() * (MAX_PARTIAL_PRESS_DURATION_MS - MIN_PARTIAL_PRESS_DURATION_MS);
    }

    /**
     * Calculate velocity profile factor for realistic scroll acceleration.
     */
    private double calculateVelocityProfileFactor(double durationMs) {
        // Human motor planning shows acceleration in first 30%, deceleration in last 30%
        double normalizedTime = durationMs / 500.0; // Normalize to 500ms scroll

        if (normalizedTime < 0.3) {
            // Acceleration phase
            return 0.3 + (normalizedTime / 0.3) * 0.4;
        } else if (normalizedTime > 0.7) {
            // Deceleration phase
            return 0.7 - ((normalizedTime - 0.7) / 0.3) * 0.3;
        } else {
            // Constant velocity phase
            return 0.7 + random.nextDouble() * 0.1;
        }
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
     * Reset fat-finger offset state.
     */
    public void resetFatFingerOffset() {
        currentFatFingerOffsetX = 0.0;
        currentFatFingerOffsetY = 0.0;
    }

    /**
     * Check if currently in correction swipe.
     */
    public boolean isInCorrectionSwipe() {
        return isInCorrectionSwipe;
    }

    public enum InteractionType {
        BUTTON_TAP,
        LINK_TAP,
        SCROLL,
        LONG_PRESS,
        SWIPE
    }

    /**
     * Data class for tap simulation results.
     */
    public static class TapResult {
        public final double actualX;
        public final double actualY;
        public final double offsetX;
        public final double offsetY;
        public final boolean hasFatFingerOffset;
        public final boolean isNearMiss;
        public final boolean isPartialPress;
        public final double partialPressDurationMs;
        public final long timestamp;

        public TapResult(double actualX, double actualY, double offsetX, double offsetY,
                        boolean hasFatFingerOffset, boolean isNearMiss, boolean isPartialPress,
                        double partialPressDurationMs, long timestamp) {
            this.actualX = actualX;
            this.actualY = actualY;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.hasFatFingerOffset = hasFatFingerOffset;
            this.isNearMiss = isNearMiss;
            this.isPartialPress = isPartialPress;
            this.partialPressDurationMs = partialPressDurationMs;
            this.timestamp = timestamp;
        }
    }

    /**
     * Data class for move simulation results.
     */
    public static class MoveResult {
        public final double correctedX;
        public final double correctedY;
        public final boolean isOvershooting;
        public final double overshootDistance;
        public final boolean isInCorrection;
        public final boolean isPalmContact;
        public final long timestamp;

        public MoveResult(double correctedX, double correctedY, boolean isOvershooting,
                        double overshootDistance, boolean isInCorrection, boolean isPalmContact,
                        long timestamp) {
            this.correctedX = correctedX;
            this.correctedY = correctedY;
            this.isOvershooting = isOvershooting;
            this.overshootDistance = overshootDistance;
            this.isInCorrection = isInCorrection;
            this.isPalmContact = isPalmContact;
            this.timestamp = timestamp;
        }
    }

    /**
     * Data class for scroll simulation results.
     */
    public static class ScrollResult {
        public final double startX;
        public final double startY;
        public final double endX;
        public final double endY;
        public final double scrollDistance;
        public final boolean hasOvershoot;
        public final double overshootDistance;
        public final double correctionDistance;
        public final double correctionDelayMs;
        public final double velocityProfileFactor;
        public final long timestamp;

        public ScrollResult(double startX, double startY, double endX, double endY,
                          double scrollDistance, boolean hasOvershoot, double overshootDistance,
                          double correctionDistance, double correctionDelayMs,
                          double velocityProfileFactor, long timestamp) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.scrollDistance = scrollDistance;
            this.hasOvershoot = hasOvershoot;
            this.overshootDistance = overshootDistance;
            this.correctionDistance = correctionDistance;
            this.correctionDelayMs = correctionDelayMs;
            this.velocityProfileFactor = velocityProfileFactor;
            this.timestamp = timestamp;
        }
    }

    /**
     * Data class for touch area simulation.
     */
    public static class TouchArea {
        public final double majorAxis;
        public final double minorAxis;

        public TouchArea(double majorAxis, double minorAxis) {
            this.majorAxis = majorAxis;
            this.minorAxis = minorAxis;
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
