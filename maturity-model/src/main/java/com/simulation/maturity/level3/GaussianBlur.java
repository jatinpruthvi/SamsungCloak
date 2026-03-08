package com.simulation.maturity.level3;

import java.util.Random;

/**
 * Gaussian Blur for Coordinate-Based UI Interaction
 * 
 * Models human motor imprecision in touchscreen interactions.
 * When humans touch a target, they don't hit exactly - they exhibit:
 * - Targeting precision: How close they aim to the center
 * - Motor noise: Random deviation due to hand tremors, inattention
 * 
 * Mathematical Model:
 * - Touch offset follows 2D Gaussian distribution
 * - Precision decreases with:
 *   - Faster movements (Fitts's Law)
 *   - Fatigued state
 *   - Small target sizes
 *   - Edge/corner positions
 * 
 * Device Target: Samsung Galaxy A12 (SM-A125U)
 * - Screen: 6.5" PLS IPS LCD (720 x 1600 pixels)
 * - Touch: Capacitive, typical 5-point multi-touch
 */
public class GaussianBlur {
    
    private final Random random;
    private double baseStdDev;      // Base standard deviation in pixels
    private double speedFactor;     // Additional noise from speed
    private double fatigueFactor;   // Additional noise when fatigued
    
    public GaussianBlur(double baseStdDevPixels) {
        this.baseStdDev = baseStdDevPixels;
        this.speedFactor = 0.0;
        this.fatigueFactor = 0.0;
        this.random = new Random();
    }
    
    public GaussianBlur(double baseStdDevPixels, long seed) {
        this.baseStdDev = baseStdDevPixels;
        this.random = new Random(seed);
    }
    
    /**
     * Apply motor noise to intended touch coordinates
     * 
     * @param targetX Intended X coordinate
     * @param targetY Intended Y coordinate
     * @param velocity Movement velocity (px/ms) - higher = more noise
     * @param targetSize Target size in pixels - smaller = harder to hit
     * @return Array of {actualX, actualY}
     */
    public float[] applyNoise(float targetX, float targetY, float velocity, float targetSize) {
        double effectiveStdDev = calculateEffectiveStdDev(velocity, targetSize);
        
        double offsetX = random.nextGaussian() * effectiveStdDev;
        double offsetY = random.nextGaussian() * effectiveStdDev;
        
        float actualX = (float) (targetX + offsetX);
        float actualY = (float) (targetY + offsetY);
        
        return new float[]{actualX, actualY};
    }
    
    /**
     * Calculate effective standard deviation based on context
     */
    private double calculateEffectiveStdDev(float velocity, float targetSize) {
        double stdDev = baseStdDev;
        
        // Speed increases noise (harder to control fast movements)
        stdDev += speedFactor * velocity * 10;
        
        // Fatigue increases noise
        stdDev += fatigueFactor * baseStdDev;
        
        // Small targets are harder to hit precisely
        if (targetSize > 0) {
            double sizeFactor = 30.0 / Math.max(30, targetSize); // Inverse relation
            stdDev *= (1.0 + (sizeFactor - 1.0) * 0.3);
        }
        
        return stdDev;
    }
    
    /**
     * Simulate mis-click: return whether touch is outside target bounds
     * 
     * @param targetX Target center X
     * @param targetY Target center Y
     * @param targetWidth Target width in pixels
     * @param targetHeight Target height in pixels
     * @return true if touch falls outside target
     */
    public boolean isMisClick(float targetX, float targetY, 
                               float targetWidth, float targetHeight,
                               float touchX, float touchY) {
        float halfW = targetWidth / 2f;
        float halfH = targetHeight / 2f;
        
        return !(touchX >= targetX - halfW && touchX <= targetX + halfW &&
                 touchY >= targetY - halfH && touchY <= targetY + halfH);
    }
    
    /**
     * Calculate click accuracy (distance from target center)
     * 
     * @param targetX Target center X
     * @param targetY Target center Y
     * @param touchX Actual touch X
     * @param touchY Actual touch Y
     * @return Distance in pixels
     */
    public double calculateAccuracy(float targetX, float targetY, 
                                    float touchX, float touchY) {
        float dx = touchX - targetX;
        float dy = touchY - targetY;
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Set speed-dependent noise factor
     * 
     * @param factor 0.0 = no speed effect, 1.0 = full effect
     */
    public void setSpeedFactor(double factor) {
        this.speedFactor = factor;
    }
    
    /**
     * Set fatigue-dependent noise factor
     * 
     * @param factor 0.0 = no fatigue effect, 1.0 = max fatigue effect
     */
    public void setFatigueFactor(double factor) {
        this.fatigueFactor = factor;
    }
    
    /**
     * Generate random touch point within target bounds
     * Simulates "hitting near but not exactly" the target
     */
    public float[] generateOffsetWithinTarget(float centerX, float centerY,
                                               float width, float height) {
        double effectiveStdDev = baseStdDev;
        
        double offsetX = random.nextGaussian() * effectiveStdDev;
        double offsetY = random.nextGaussian() * effectiveStdDev;
        
        // Clamp to target bounds with some probability of edge hits
        float halfW = width / 2f;
        float halfH = height / 2f;
        
        double maxOffsetX = Math.min(halfW, effectiveStdDev * 2);
        double maxOffsetY = Math.min(halfH, effectiveStdDev * 2);
        
        offsetX = Math.max(-maxOffsetX, Math.min(maxOffsetX, offsetX));
        offsetY = Math.max(-maxOffsetY, Math.min(maxOffsetY, offsetY));
        
        return new float[]{(float)(centerX + offsetX), (float)(centerY + offsetY)};
    }
    
    /**
     * Preconfigured for typical smartphone use
     */
    public static GaussianBlur forSmartphone() {
        return new GaussianBlur(8.0); // ~8px typical touch variation
    }
    
    /**
     * Preconfigured for precise interactions (e.g., text selection)
     */
    public static GaussianBlur forPreciseInteraction() {
        return new GaussianBlur(4.0); // ~4px for careful taps
    }
    
    /**
     * Preconfigured for edge/corner interactions (harder to hit)
     */
    public static GaussianBlur forEdgeInteraction() {
        return new GaussianBlur(12.0); // ~12px for edge taps
    }
    
    /**
     * Preconfigured for fatigued user
     */
    public static GaussianBlur forFatiguedUser() {
        GaussianBlur blur = new GaussianBlur(12.0);
        blur.setFatigueFactor(0.8);
        return blur;
    }
    
    /**
     * Get current base standard deviation
     */
    public double getBaseStdDev() {
        return baseStdDev;
    }
}
