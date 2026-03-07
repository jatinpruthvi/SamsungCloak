package com.samsungcloak.xposed.motor;

import com.samsungcloak.xposed.HookUtils;

import java.util.Random;

/**
 * Hook 2: Motion Curve Engine
 * 
 * Implements Fitts's Law compliant non-linear motion curves for realistic
 * touch gestures. Human movement follows a bell-shaped velocity curve
 * (acceleration and deceleration) rather than linear paths.
 * 
 * Fitts's Law:
 * - MT = a + b * log2(2D/W)
 * - MT = Movement Time, D = Distance, W = Target Width
 * - Larger targets and shorter distances = faster movements
 * 
 * Bell-shaped velocity profile:
 * - Slow start (anticipatory phase)
 * - Acceleration toward target
 * - Peak velocity at ~30% of path (where control is lowest)
 * - Deceleration as target approaches
 * - Careful final positioning
 */
public class MotionCurveEngine {
    private final Random random;
    private final NeuromotorConfig config;

    public MotionCurveEngine(NeuromotorConfig config) {
        this.config = config;
        this.random = new Random();
    }

    public void updateConfig(NeuromotorConfig config) {
        this.config = config;
    }

    /**
     * Generate a complete motion path with Fitts's Law compliant velocity curve
     * 
     * @param startX Starting X coordinate
     * @param startY Starting Y coordinate
     * @param endX Ending X coordinate
     * @param endY Ending Y coordinate
     * @param steps Number of intermediate points to generate
     * @return Array of [x, y] coordinates representing the motion path
     */
    public float[] generateMotionPath(float startX, float startY, float endX, float endY, int steps) {
        if (steps <= 0) {
            steps = 20; // Default reasonable number of steps
        }

        float[] path = new float[steps * 2]; // x, y pairs
        
        float totalDistance = calculateDistance(startX, startY, endX, endY);
        
        // Calculate Fitts's Law Index of Difficulty
        double id = calculateIndexOfDifficulty(totalDistance, 50f); // Assume 50px target width
        
        // Adjust curve based on ID (more difficult = more careful = lower peak velocity)
        float difficultyMultiplier = config.isFittsLawEnabled() 
            ? (float) Math.max(0.5, 1.0 - (id * 0.1 * config.getFittsLawIdMultiplier()))
            : 1.0f;

        for (int i = 0; i < steps; i++) {
            float progress = (float) i / (steps - 1);
            
            // Get velocity at this point in the curve
            float velocity = getVelocityAtProgress(progress) * difficultyMultiplier;
            
            // Calculate distance traveled at this point
            float distanceTraveled = totalProgressToDistance(progress, totalDistance);
            
            // Interpolate position
            float t = distanceTraveled / totalDistance;
            t = clamp(t, 0f, 1f);
            
            float x = lerp(startX, endX, t);
            float y = lerp(startY, endY, t);
            
            // Add slight noise for natural imperfection
            float noiseMagnitude = totalDistance * 0.01f * (1f - progress);
            x += (float) (random.nextGaussian() * noiseMagnitude);
            y += (float) (random.nextGaussian() * noiseMagnitude);
            
            path[i * 2] = x;
            path[i * 2 + 1] = y;
        }
        
        // Ensure final point is exact
        path[(steps - 1) * 2] = endX;
        path[(steps - 1) * 2 + 1] = endY;

        HookUtils.logDebug("Generated motion path: " + steps + " points, distance=" + totalDistance);
        
        return path;
    }

    /**
     * Get velocity at a given progress point (0.0 to 1.0)
     * Uses a bell-shaped (Gaussian-like) curve
     */
    public float getVelocityAtProgress(float progress) {
        float peakPosition = config.getMotionPeakPosition(); // ~0.3
        float width = config.getMotionCurveWidth(); // ~0.4
        float minVelocity = config.getMinVelocityRatio(); // ~0.1
        
        // Gaussian bell curve centered at peakPosition
        float exponent = -Math.pow(progress - peakPosition, 2) / (2 * width * width);
        float bellCurve = (float) Math.exp(exponent);
        
        // Scale to range [minVelocity, 1.0]
        float velocity = minVelocity + (1f - minVelocity) * bellCurve;
        
        // Add slight random variation for natural feel
        float variation = 1f + (float) (random.nextGaussian() * 0.05);
        velocity *= variation;
        
        return clamp(velocity, minVelocity, 1.0f);
    }

    /**
     * Calculate Fitts's Law Index of Difficulty
     * ID = log2(2D/W) where D=distance, W=target width
     */
    public double calculateIndexOfDifficulty(float distance, float targetWidth) {
        if (targetWidth <= 0) targetWidth = 1;
        return Math.log((2 * distance) / targetWidth) / Math.log(2);
    }

    /**
     * Calculate movement time using Fitts's Law
     * MT = a + b * ID
     */
    public long calculateMovementTime(float distance, float targetWidth) {
        // Typical values: a=50ms (intercept), b=80ms/bit (slope)
        double id = calculateIndexOfDifficulty(distance, targetWidth);
        double mt = 50 + 80 * id;
        return (long) Math.max(50, mt);
    }

    /**
     * Generate swipe gesture with realistic velocity curve
     * 
     * @param startX Start X
     * @param startY Start Y
     * @param endX End X
     * @param endY End Y
     * @param durationMs Total gesture duration
     * @return List of [x, y, timestamp] triplets
     */
    public float[] generateSwipePath(float startX, float startY, float endX, float endY, long durationMs) {
        // Use 60fps for smooth motion
        int steps = (int) (durationMs / 16.67f);
        if (steps < 3) steps = 3;
        
        float[] path = new float[steps * 3]; // x, y, timestamp
        
        for (int i = 0; i < steps; i++) {
            float progress = (float) i / (steps - 1);
            
            // Use ease-in-out curve for swipes
            float easedProgress = easeInOutCubic(progress);
            
            // Add velocity variation
            float velocityFactor = getVelocityAtProgress(progress);
            float adjustedProgress = easedProgress * velocityFactor;
            
            float x = lerp(startX, endX, adjustedProgress);
            float y = lerp(startY, endY, adjustedProgress);
            
            long timestamp = (long) (progress * durationMs);
            
            path[i * 3] = x;
            path[i * 3 + 1] = y;
            path[i * 3 + 2] = timestamp;
        }
        
        return path;
    }

    /**
     * Ease in-out cubic function for smoother swipes
     */
    private float easeInOutCubic(float t) {
        if (t < 0.5f) {
            return 4 * t * t * t;
        } else {
            return 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
        }
    }

    /**
     * Convert progress (0-1) to distance traveled using velocity curve integration
     */
    private float totalProgressToDistance(float progress, float totalDistance) {
        // Integrate velocity curve using trapezoidal rule
        int samples = 100;
        float sum = 0;
        float dt = progress / samples;
        
        for (int i = 0; i < samples; i++) {
            float t1 = i * dt;
            float t2 = (i + 1) * dt;
            float v1 = getVelocityAtProgress(t1);
            float v2 = getVelocityAtProgress(t2);
            sum += (v1 + v2) * dt / 2;
        }
        
        return sum * totalDistance;
    }

    private float calculateDistance(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
