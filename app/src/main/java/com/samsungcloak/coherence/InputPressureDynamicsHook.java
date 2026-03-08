package com.samsungcloak.coherence;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Input Pressure & Surface Area Dynamics Hook for Samsung Galaxy A12 (SM-A125U)
 *
 * Simulates realistic touch pressure and contact area dynamics based on interaction
 * type and human motor behavior. This provides high-fidelity touch telemetry that
 * accurately reflects real-world finger contact patterns.
 *
 * Key Features:
 * 1. Touch Major/Minor axis modeling based on interaction type
 * 2. Pressure variation for different touch types (firm taps vs. light scrolls)
 * 3. Finger deformation during sustained contact
 * 4. Multi-touch pressure distribution
 * 5. Contact area evolution during touch events
 */
public class InputPressureDynamicsHook {

    private static final String LOG_TAG = "SamsungCloak.InputPressureDynamics";
    private static boolean initialized = false;

    // Pressure constants (normalized 0-1 range)
    private static final double PRESSURE_MIN = 0.15;
    private static final double PRESSURE_MAX = 0.95;
    private static final double PRESSURE_THRESHOLD_FOR_DETECTION = 0.05;

    // Touch area constants (pixels)
    private static final double FINGER_TOUCH_WIDTH_MIN_PX = 8.0;
    private static final double FINGER_TOUCH_WIDTH_MAX_PX = 18.0;
    private static final double FINGER_TOUCH_HEIGHT_MIN_PX = 12.0;
    private static final double FINGER_TOUCH_HEIGHT_MAX_PX = 25.0;

    // Interaction-specific pressure profiles
    private static final double BUTTON_TAP_PRESSURE_BASE = 0.75;
    private static final double BUTTON_TAP_PRESSURE_VARiability = 0.15;
    private static final double LINK_TAP_PRESSURE_BASE = 0.55;
    private static final double LINK_TAP_PRESSURE_VARiability = 0.20;
    private static final double SCROLL_PRESSURE_BASE = 0.30;
    private static final double SCROLL_PRESSURE_VARiability = 0.25;
    private static final double LONG_PRESS_PRESSURE_BASE = 0.65;
    private static final double LONG_PRESS_PRESSURE_VARiability = 0.20;
    private static final double SWIPE_PRESSURE_BASE = 0.40;
    private static final double SWIPE_PRESSURE_VARiability = 0.30;

    // Finger deformation constants
    private static final double CONTACT_AREA_EXPANSION_RATE = 0.08; // Rate of area expansion during contact
    private static final double CONTACT_AREA_EXPANSION_MAX_RATIO = 1.35; // Maximum expansion ratio

    // Multi-touch pressure distribution constants
    private static final double MULTI_TOUCH_PRESSURE_CORRELATION = 0.85;
    private static final double PINCH_PRESSURE_VARIANCE = 0.10;

    // Pressure noise constants
    private static final double PRESSURE_NOISE_STD_DEV = 0.03;

    // Touch history for pressure smoothing
    private static final int PRESSURE_SMOOTHING_WINDOW = 3;

    private final java.util.Random random;
    private final Deque<PressureSample> pressureHistory;

    // Current state
    private double currentPressure;
    private double currentTouchWidth;
    private double currentTouchHeight;
    private long contactStartTimeMs;
    private double contactDurationMs;
    private boolean isInContact;

    public InputPressureDynamicsHook() {
        this.random = new java.util.Random();
        this.pressureHistory = new ArrayDeque<>(PRESSURE_SMOOTHING_WINDOW);

        this.currentPressure = 0.0;
        this.currentTouchWidth = 0.0;
        this.currentTouchHeight = 0.0;
        this.contactStartTimeMs = 0;
        this.contactDurationMs = 0;
        this.isInContact = false;
    }

    /**
     * Initialize the input pressure dynamics hook.
     */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
    }

    /**
     * Simulate initial touch down event with pressure and contact area.
     *
     * @param interactionType type of interaction
     * @return TouchDownResult with initial pressure and contact area
     */
    public TouchDownResult simulateTouchDown(InteractionType interactionType) {
        long currentTimeMs = System.currentTimeMillis();

        // Generate initial pressure based on interaction type
        double initialPressure = generateInitialPressure(interactionType);

        // Generate initial contact area based on interaction type
        TouchArea initialContactArea = generateInitialContactArea(interactionType);

        // Apply pressure noise
        initialPressure += generatePressureNoise();

        // Clamp pressure to valid range
        initialPressure = Math.max(PRESSURE_MIN, Math.min(PRESSURE_MAX, initialPressure));

        // Update state
        currentPressure = initialPressure;
        currentTouchWidth = initialContactArea.majorAxis;
        currentTouchHeight = initialContactArea.minorAxis;
        contactStartTimeMs = currentTimeMs;
        contactDurationMs = 0;
        isInContact = true;

        // Add to pressure history
        addPressureSample(initialPressure, currentTimeMs);

        return new TouchDownResult(
            initialPressure,
            initialContactArea,
            currentTimeMs
        );
    }

    /**
     * Simulate touch move event with pressure and contact area evolution.
     *
     * @param deltaX X movement delta
     * @param deltaY Y movement delta
     * @param interactionType type of interaction
     * @return TouchMoveResult with updated pressure and contact area
     */
    public TouchMoveResult simulateTouchMove(double deltaX, double deltaY, InteractionType interactionType) {
        long currentTimeMs = System.currentTimeMillis();

        // Update contact duration
        if (isInContact) {
            contactDurationMs = currentTimeMs - contactStartTimeMs;
        }

        // Calculate finger deformation (contact area expands over time)
        double expansionRatio = calculateContactExpansionRatio(contactDurationMs);

        // Generate pressure variation based on movement
        double pressureVariation = calculateMovementPressureVariation(deltaX, deltaY, interactionType);

        // Update pressure with smoothing
        double smoothedPressure = calculateSmoothedPressure(currentPressure + pressureVariation);

        // Update contact area with deformation
        double expandedWidth = currentTouchWidth * expansionRatio;
        double expandedHeight = currentTouchHeight * expansionRatio;

        // Add contact area noise
        expandedWidth += generateContactAreaNoise();
        expandedHeight += generateContactAreaNoise();

        // Clamp to valid ranges
        expandedWidth = Math.max(FINGER_TOUCH_WIDTH_MIN_PX,
                               Math.min(FINGER_TOUCH_WIDTH_MAX_PX, expandedWidth));
        expandedHeight = Math.max(FINGER_TOUCH_HEIGHT_MIN_PX,
                                Math.min(FINGER_TOUCH_HEIGHT_MAX_PX, expandedHeight));

        // Update state
        currentPressure = Math.max(PRESSURE_MIN, Math.min(PRESSURE_MAX, smoothedPressure));
        currentTouchWidth = expandedWidth;
        currentTouchHeight = expandedHeight;

        return new TouchMoveResult(
            currentPressure,
            new TouchArea(expandedWidth, expandedHeight),
            expansionRatio,
            contactDurationMs,
            currentTimeMs
        );
    }

    /**
     * Simulate touch up event with final pressure and contact area.
     *
     * @return TouchUpResult with final touch data
     */
    public TouchUpResult simulateTouchUp() {
        long currentTimeMs = System.currentTimeMillis();

        // Calculate final contact duration
        double finalDurationMs = isInContact ? contactDurationMs : 0.0;

        // Generate final pressure (typically lower than initial for quick taps)
        double finalPressure = calculateFinalPressure(currentPressure, finalDurationMs);

        // Generate final contact area
        TouchArea finalContactArea = new TouchArea(currentTouchWidth, currentTouchHeight);

        // Clear state
        isInContact = false;
        contactStartTimeMs = 0;
        contactDurationMs = 0;

        // Clear pressure history
        pressureHistory.clear();

        return new TouchUpResult(
            finalPressure,
            finalContactArea,
            finalDurationMs,
            currentTimeMs
        );
    }

    /**
     * Simulate multi-touch event with pressure distribution.
     *
     * @param pointerCount number of touch pointers
     * @param interactionType type of interaction
     * @return MultiTouchResult with pressure for each pointer
     */
    public MultiTouchResult simulateMultiTouch(int pointerCount, InteractionType interactionType) {
        long currentTimeMs = System.currentTimeMillis();

        if (pointerCount < 1 || pointerCount > 5) {
            pointerCount = Math.max(1, Math.min(5, pointerCount));
        }

        double[] pressures = new double[pointerCount];
        TouchArea[] contactAreas = new TouchArea[pointerCount];

        // Generate pressure for primary pointer
        pressures[0] = generateInitialPressure(interactionType);
        pressures[0] += generatePressureNoise();
        pressures[0] = Math.max(PRESSURE_MIN, Math.min(PRESSURE_MAX, pressures[0]));

        contactAreas[0] = generateInitialContactArea(interactionType);

        // Generate pressures for secondary pointers with correlation
        for (int i = 1; i < pointerCount; i++) {
            double basePressure = pressures[0] * MULTI_TOUCH_PRESSURE_CORRELATION;

            if (interactionType == InteractionType.PINCH) {
                // Pinch gesture has slightly more pressure variance
                basePressure += (random.nextDouble() - 0.5) * PINCH_PRESSURE_VARIANCE;
            } else {
                basePressure += (random.nextDouble() - 0.5) * 0.05;
            }

            pressures[i] = Math.max(PRESSURE_MIN, Math.min(PRESSURE_MAX, basePressure));
            contactAreas[i] = generateInitialContactArea(interactionType);
        }

        return new MultiTouchResult(
            pressures,
            contactAreas,
            currentTimeMs
        );
    }

    /**
     * Simulate pressure changes during specific interaction types.
     *
     * @param interactionType type of interaction
     * @param progress interaction progress (0-1)
     * @return PressureChangeResult with pressure at given progress
     */
    public PressureChangeResult simulatePressureChange(InteractionType interactionType, double progress) {
        if (progress < 0.0) progress = 0.0;
        if (progress > 1.0) progress = 1.0;

        double pressure;

        switch (interactionType) {
            case LONG_PRESS:
                // Long press: pressure increases and stabilizes
                pressure = LONG_PRESS_PRESSURE_BASE +
                          (progress * 0.15) +
                          (random.nextDouble() - 0.5) * LONG_PRESS_PRESSURE_VARiability;
                break;

            case SCROLL:
                // Scroll: pressure varies cyclically during scroll
                pressure = SCROLL_PRESSURE_BASE +
                          (Math.sin(progress * Math.PI * 2) * 0.1) +
                          (random.nextDouble() - 0.5) * SCROLL_PRESSURE_VARiability;
                break;

            case SWIPE:
                // Swipe: pressure peaks in middle of swipe
                pressure = SWIPE_PRESSURE_BASE +
                          (Math.sin(progress * Math.PI) * 0.2) +
                          (random.nextDouble() - 0.5) * SWIPE_PRESSURE_VARiability;
                break;

            case BUTTON_TAP:
            case LINK_TAP:
                // Tap: pressure is highest initially, then decreases
                pressure = (BUTTON_TAP_PRESSURE_BASE + LINK_TAP_PRESSURE_BASE) / 2.0 -
                          (progress * 0.3) +
                          (random.nextDouble() - 0.5) * 0.1;
                break;

            case PINCH:
                // Pinch: pressure increases as pinch tightens
                pressure = 0.6 + (progress * 0.25) + (random.nextDouble() - 0.5) * 0.15;
                break;

            default:
                pressure = 0.5 + (random.nextDouble() - 0.5) * 0.3;
        }

        // Add noise
        pressure += generatePressureNoise();

        // Clamp to valid range
        pressure = Math.max(PRESSURE_MIN, Math.min(PRESSURE_MAX, pressure));

        return new PressureChangeResult(
            interactionType,
            progress,
            pressure,
            System.currentTimeMillis()
        );
    }

    /**
     * Generate initial pressure based on interaction type.
     */
    private double generateInitialPressure(InteractionType interactionType) {
        double basePressure;
        double variability;

        switch (interactionType) {
            case BUTTON_TAP:
                basePressure = BUTTON_TAP_PRESSURE_BASE;
                variability = BUTTON_TAP_PRESSURE_VARiability;
                break;
            case LINK_TAP:
                basePressure = LINK_TAP_PRESSURE_BASE;
                variability = LINK_TAP_PRESSURE_VARiability;
                break;
            case SCROLL:
                basePressure = SCROLL_PRESSURE_BASE;
                variability = SCROLL_PRESSURE_VARiability;
                break;
            case LONG_PRESS:
                basePressure = LONG_PRESS_PRESSURE_BASE;
                variability = LONG_PRESS_PRESSURE_VARiability;
                break;
            case SWIPE:
                basePressure = SWIPE_PRESSURE_BASE;
                variability = SWIPE_PRESSURE_VARiability;
                break;
            case PINCH:
                basePressure = 0.65;
                variability = 0.15;
                break;
            default:
                basePressure = 0.5;
                variability = 0.2;
        }

        return basePressure + (random.nextDouble() - 0.5) * variability;
    }

    /**
     * Generate initial contact area based on interaction type.
     */
    private TouchArea generateInitialContactArea(InteractionType interactionType) {
        double width, height;

        switch (interactionType) {
            case BUTTON_TAP:
                // Compact contact for buttons
                width = FINGER_TOUCH_WIDTH_MIN_PX + random.nextDouble() * 3.0;
                height = FINGER_TOUCH_HEIGHT_MIN_PX + random.nextDouble() * 4.0;
                break;

            case LINK_TAP:
                // Slightly larger contact for links
                width = FINGER_TOUCH_WIDTH_MIN_PX + random.nextDouble() * 4.0;
                height = FINGER_TOUCH_HEIGHT_MIN_PX + random.nextDouble() * 5.0;
                break;

            case SCROLL:
                // Elongated contact for scrolling
                width = FINGER_TOUCH_WIDTH_MIN_PX + random.nextDouble() * 5.0;
                height = FINGER_TOUCH_HEIGHT_MAX_PX - random.nextDouble() * 3.0;
                break;

            case LONG_PRESS:
                // Expanding contact for long press (initially compact)
                width = FINGER_TOUCH_WIDTH_MIN_PX + random.nextDouble() * 3.0;
                height = FINGER_TOUCH_HEIGHT_MIN_PX + random.nextDouble() * 4.0;
                break;

            case SWIPE:
                // Dynamic contact for swipe
                width = FINGER_TOUCH_WIDTH_MIN_PX + random.nextDouble() * 5.0;
                height = FINGER_TOUCH_HEIGHT_MIN_PX + random.nextDouble() * 5.0;
                break;

            case PINCH:
                // Larger contact for pinch (two fingers)
                width = FINGER_TOUCH_WIDTH_MAX_PX - random.nextDouble() * 3.0;
                height = FINGER_TOUCH_HEIGHT_MAX_PX - random.nextDouble() * 4.0;
                break;

            default:
                width = FINGER_TOUCH_WIDTH_MIN_PX + random.nextDouble() * 4.0;
                height = FINGER_TOUCH_HEIGHT_MIN_PX + random.nextDouble() * 5.0;
        }

        return new TouchArea(width, height);
    }

    /**
     * Calculate contact area expansion ratio based on contact duration.
     */
    private double calculateContactExpansionRatio(double durationMs) {
        if (durationMs < 100) {
            return 1.0;
        }

        // Exponential approach to maximum expansion
        double normalizedTime = Math.min(durationMs / 1000.0, 1.0);
        double expansion = 1.0 + (CONTACT_AREA_EXPANSION_MAX_RATIO - 1.0) *
                         (1.0 - Math.exp(-CONTACT_AREA_EXPANSION_RATE * normalizedTime * 10));

        return Math.min(CONTACT_AREA_EXPANSION_MAX_RATIO, expansion);
    }

    /**
     * Calculate pressure variation based on movement.
     */
    private double movementPressureVariation = 0.0;

    private double calculateMovementPressureVariation(double deltaX, double deltaY, InteractionType interactionType) {
        double movementMagnitude = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        double variation = 0.0;

        switch (interactionType) {
            case SCROLL:
                // Pressure varies with scroll speed
                variation = (movementMagnitude / 100.0) * 0.05;
                break;

            case SWIPE:
                // Pressure increases slightly with swipe speed
                variation = (movementMagnitude / 200.0) * 0.08;
                break;

            case BUTTON_TAP:
            case LINK_TAP:
                // Minimal variation for taps
                variation = (random.nextDouble() - 0.5) * 0.02;
                break;

            default:
                variation = 0.0;
        }

        // Add some randomness
        variation += (random.nextDouble() - 0.5) * 0.03;

        return variation;
    }

    /**
     * Calculate smoothed pressure using exponential moving average.
     */
    private double calculateSmoothedPressure(double rawPressure) {
        if (pressureHistory.isEmpty()) {
            return rawPressure;
        }

        // Exponential moving average
        double alpha = 0.3; // Smoothing factor
        return alpha * rawPressure + (1.0 - alpha) * pressureHistory.peekLast().pressure;
    }

    /**
     * Calculate final pressure for touch up.
     */
    private double calculateFinalPressure(double currentPressure, double durationMs) {
        // For quick taps, pressure decreases rapidly
        if (durationMs < 200) {
            return currentPressure * 0.7 + generatePressureNoise();
        }

        // For longer contacts, pressure decreases more gradually
        return currentPressure * 0.9 + generatePressureNoise();
    }

    /**
     * Generate pressure noise.
     */
    private double generatePressureNoise() {
        return (random.nextGaussian() * PRESSURE_NOISE_STD_DEV);
    }

    /**
     * Generate contact area noise.
     */
    private double generateContactAreaNoise() {
        return (random.nextGaussian() * 0.5);
    }

    /**
     * Add pressure sample to history.
     */
    private void addPressureSample(double pressure, long timestamp) {
        pressureHistory.addLast(new PressureSample(pressure, timestamp));
        if (pressureHistory.size() > PRESSURE_SMOOTHING_WINDOW) {
            pressureHistory.removeFirst();
        }
    }

    /**
     * Get current touch state.
     */
    public TouchState getTouchState() {
        return new TouchState(
            currentPressure,
            currentTouchWidth,
            currentTouchHeight,
            contactDurationMs,
            isInContact
        );
    }

    public enum InteractionType {
        BUTTON_TAP,
        LINK_TAP,
        SCROLL,
        LONG_PRESS,
        SWIPE,
        PINCH,
        ROTATE,
        UNKNOWN
    }

    /**
     * Data class for touch area.
     */
    public static class TouchArea {
        public final double majorAxis;
        public final double minorAxis;

        public TouchArea(double majorAxis, double minorAxis) {
            this.majorAxis = majorAxis;
            this.minorAxis = minorAxis;
        }
    }

    /**
     * Data class for pressure sample.
     */
    private static class PressureSample {
        public final double pressure;
        public final long timestamp;

        public PressureSample(double pressure, long timestamp) {
            this.pressure = pressure;
            this.timestamp = timestamp;
        }
    }

    /**
     * Data class for touch down result.
     */
    public static class TouchDownResult {
        public final double pressure;
        public final TouchArea contactArea;
        public final long timestamp;

        public TouchDownResult(double pressure, TouchArea contactArea, long timestamp) {
            this.pressure = pressure;
            this.contactArea = contactArea;
            this.timestamp = timestamp;
        }
    }

    /**
     * Data class for touch move result.
     */
    public static class TouchMoveResult {
        public final double pressure;
        public final TouchArea contactArea;
        public final double expansionRatio;
        public final double contactDurationMs;
        public final long timestamp;

        public TouchMoveResult(double pressure, TouchArea contactArea, double expansionRatio,
                             double contactDurationMs, long timestamp) {
            this.pressure = pressure;
            this.contactArea = contactArea;
            this.expansionRatio = expansionRatio;
            this.contactDurationMs = contactDurationMs;
            this.timestamp = timestamp;
        }
    }

    /**
     * Data class for touch up result.
     */
    public static class TouchUpResult {
        public final double pressure;
        public final TouchArea contactArea;
        public final double contactDurationMs;
        public final long timestamp;

        public TouchUpResult(double pressure, TouchArea contactArea, double contactDurationMs,
                           long timestamp) {
            this.pressure = pressure;
            this.contactArea = contactArea;
            this.contactDurationMs = contactDurationMs;
            this.timestamp = timestamp;
        }
    }

    /**
     * Data class for multi-touch result.
     */
    public static class MultiTouchResult {
        public final double[] pressures;
        public final TouchArea[] contactAreas;
        public final long timestamp;

        public MultiTouchResult(double[] pressures, TouchArea[] contactAreas, long timestamp) {
            this.pressures = pressures;
            this.contactAreas = contactAreas;
            this.timestamp = timestamp;
        }
    }

    /**
     * Data class for pressure change result.
     */
    public static class PressureChangeResult {
        public final InteractionType interactionType;
        public final double progress;
        public final double pressure;
        public final long timestamp;

        public PressureChangeResult(InteractionType interactionType, double progress,
                                   double pressure, long timestamp) {
            this.interactionType = interactionType;
            this.progress = progress;
            this.pressure = pressure;
            this.timestamp = timestamp;
        }
    }

    /**
     * Data class for touch state.
     */
    public static class TouchState {
        public final double pressure;
        public final double touchWidth;
        public final double touchHeight;
        public final double contactDurationMs;
        public final boolean isInContact;

        public TouchState(double pressure, double touchWidth, double touchHeight,
                         double contactDurationMs, boolean isInContact) {
            this.pressure = pressure;
            this.touchWidth = touchWidth;
            this.touchHeight = touchHeight;
            this.contactDurationMs = contactDurationMs;
            this.isInContact = isInContact;
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
