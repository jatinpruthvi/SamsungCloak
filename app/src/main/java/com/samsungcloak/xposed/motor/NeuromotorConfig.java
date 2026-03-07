package com.samsungcloak.xposed.motor;

import com.samsungcloak.xposed.HookUtils;

/**
 * Configuration for Neuromotor Interaction Profile
 * 
 * All parameters are tuned to reflect realistic human motor control characteristics
 * observed in studies on touchscreen interaction, particularly for the
 * Samsung Galaxy A12 (SM-A125U) form factor.
 */
public class NeuromotorConfig {
    // ========== Typing Cadence Configuration ==========
    
    /** Mean inter-key delay in milliseconds (log-normal mu) */
    private float typingMeanMs = 120f;
    
    /** Standard deviation of inter-key delay (log-normal sigma) */
    private float typingStdDevMs = 45f;
    
    /** Mean delay during burst typing (faster) */
    private float burstMeanMs = 65f;
    
    /** Burst probability (0.0 - 1.0) */
    private float burstProbability = 0.15f;
    
    /** Backspace correction probability per keystroke */
    private float correctionProbability = 0.08f;
    
    /** Correction delay (longer pause after error) */
    private float correctionDelayMs = 350f;

    // ========== Motion Curve Configuration ==========
    
    /** Bell curve peak position (0.0 - 1.0, typically ~0.3 for human motion) */
    private float motionPeakPosition = 0.3f;
    
    /** Bell curve width (controls acceleration/deceleration) */
    private float motionCurveWidth = 0.4f;
    
    /** Minimum velocity as fraction of peak */
    private float minVelocityRatio = 0.1f;
    
    /** Enable Fitts's Law compliance */
    private boolean fittsLawEnabled = true;
    
    /** Fitts's Law Index of Difficulty multiplier */
    private float fittsLawIdMultiplier = 1.0f;

    // ========== Touch Pressure Configuration ==========
    
    /** Base touch pressure (0.0 - 1.0) */
    private float basePressure = 0.6f;
    
    /** Pressure variation standard deviation */
    private float pressureStdDev = 0.15f;
    
    /** Minimum pressure (fat finger = lighter) */
    private float minPressure = 0.2f;
    
    /** Maximum pressure (strong grip) */
    private float maxPressure = 1.0f;
    
    /** Base contact size */
    private float baseContactSize = 0.7f;
    
    /** Contact size variation */
    private float contactSizeStdDev = 0.12f;
    
    /** Fat finger effect probability */
    private float fatFingerProbability = 0.25f;
    
    /** Fat finger size multiplier */
    private float fatFingerSizeMultiplier = 1.4f;

    // ========== Scroll Acceleration Configuration ==========
    
    /** Friction coefficient (0.0 - 1.0, lower = more slippery) */
    private float scrollFriction = 0.15f;
    
    /** Initial velocity decay rate */
    private float velocityDecayRate = 0.92f;
    
    /** Overscroll bounce factor */
    private float overscrollBounce = 0.3f;
    
    /** Maximum overscroll distance in pixels */
    private float maxOverscrollPx = 50f;
    
    /** Minimum flick velocity to trigger momentum */
    private float minFlickVelocity = 100f;
    
    /** Flick velocity multiplier for natural feel */
    private float flickVelocityMultiplier = 0.85f;

    // ========== Reaction Time Configuration ==========
    
    /** Base reaction time in milliseconds */
    private float baseReactionTimeMs = 220f;
    
    /** Reaction time standard deviation */
    private float reactionTimeStdDevMs = 45f;
    
    /** Minimum reaction time (reflex response) */
    private float minReactionTimeMs = 150f;
    
    /** Maximum reaction time (distracted) */
    private float maxReactionTimeMs = 500f;
    
    /** Complex task delay multiplier */
    private float complexTaskMultiplier = 1.8f;
    
    /** Fatigue delay multiplier */
    private float fatigueMultiplier = 1.4f;
    
    /** Visual attention delay for UI elements */
    private float visualAttentionDelayMs = 80f;

    // Getters and Setters

    public float getTypingMeanMs() { return typingMeanMs; }
    public void setTypingMeanMs(float v) { this.typingMeanMs = v; }
    
    public float getTypingStdDevMs() { return typingStdDevMs; }
    public void setTypingStdDevMs(float v) { this.typingStdDevMs = v; }
    
    public float getBurstMeanMs() { return burstMeanMs; }
    public void setBurstMeanMs(float v) { this.burstMeanMs = v; }
    
    public float getBurstProbability() { return burstProbability; }
    public void setBurstProbability(float v) { this.burstProbability = v; }
    
    public float getCorrectionProbability() { return correctionProbability; }
    public void setCorrectionProbability(float v) { this.correctionProbability = v; }
    
    public float getCorrectionDelayMs() { return correctionDelayMs; }
    public void setCorrectionDelayMs(float v) { this.correctionDelayMs = v; }

    public float getMotionPeakPosition() { return motionPeakPosition; }
    public void setMotionPeakPosition(float v) { this.motionPeakPosition = v; }
    
    public float getMotionCurveWidth() { return motionCurveWidth; }
    public void setMotionCurveWidth(float v) { this.motionCurveWidth = v; }
    
    public float getMinVelocityRatio() { return minVelocityRatio; }
    public void setMinVelocityRatio(float v) { this.minVelocityRatio = v; }
    
    public boolean isFittsLawEnabled() { return fittsLawEnabled; }
    public void setFittsLawEnabled(boolean v) { this.fittsLawEnabled = v; }
    
    public float getFittsLawIdMultiplier() { return fittsLawIdMultiplier; }
    public void setFittsLawIdMultiplier(float v) { this.fittsLawIdMultiplier = v; }

    public float getBasePressure() { return basePressure; }
    public void setBasePressure(float v) { this.basePressure = v; }
    
    public float getPressureStdDev() { return pressureStdDev; }
    public void setPressureStdDev(float v) { this.pressureStdDev = v; }
    
    public float getMinPressure() { return minPressure; }
    public void setMinPressure(float v) { this.minPressure = v; }
    
    public float getMaxPressure() { return maxPressure; }
    public void setMaxPressure(float v) { this.maxPressure = v; }
    
    public float getBaseContactSize() { return baseContactSize; }
    public void setBaseContactSize(float v) { this.baseContactSize = v; }
    
    public float getContactSizeStdDev() { return contactSizeStdDev; }
    public void setContactSizeStdDev(float v) { this.contactSizeStdDev = v; }
    
    public float getFatFingerProbability() { return fatFingerProbability; }
    public void setFatFingerProbability(float v) { this.fatFingerProbability = v; }
    
    public float getFatFingerSizeMultiplier() { return fatFingerSizeMultiplier; }
    public void setFatFingerSizeMultiplier(float v) { this.fatFingerSizeMultiplier = v; }

    public float getScrollFriction() { return scrollFriction; }
    public void setScrollFriction(float v) { this.scrollFriction = v; }
    
    public float getVelocityDecayRate() { return velocityDecayRate; }
    public void setVelocityDecayRate(float v) { this.velocityDecayRate = v; }
    
    public float getOverscrollBounce() { return overscrollBounce; }
    public void setOverscrollBounce(float v) { this.overscrollBounce = v; }
    
    public float getMaxOverscrollPx() { return maxOverscrollPx; }
    public void setMaxOverscrollPx(float v) { this.maxOverscrollPx = v; }
    
    public float getMinFlickVelocity() { return minFlickVelocity; }
    public void setMinFlickVelocity(float v) { this.minFlickVelocity = v; }
    
    public float getFlickVelocityMultiplier() { return flickVelocityMultiplier; }
    public void setFlickVelocityMultiplier(float v) { this.flickVelocityMultiplier = v; }

    public float getBaseReactionTimeMs() { return baseReactionTimeMs; }
    public void setBaseReactionTimeMs(float v) { this.baseReactionTimeMs = v; }
    
    public float getReactionTimeStdDevMs() { return reactionTimeStdDevMs; }
    public void setReactionTimeStdDevMs(float v) { this.reactionTimeStdDevMs = v; }
    
    public float getMinReactionTimeMs() { return minReactionTimeMs; }
    public void setMinReactionTimeMs(float v) { this.minReactionTimeMs = v; }
    
    public float getMaxReactionTimeMs() { return maxReactionTimeMs; }
    public void setMaxReactionTimeMs(float v) { this.maxReactionTimeMs = v; }
    
    public float getComplexTaskMultiplier() { return complexTaskMultiplier; }
    public void setComplexTaskMultiplier(float v) { this.complexTaskMultiplier = v; }
    
    public float getFatigueMultiplier() { return fatigueMultiplier; }
    public void setFatigueMultiplier(float v) { this.fatigueMultiplier = v; }
    
    public float getVisualAttentionDelayMs() { return visualAttentionDelayMs; }
    public void setVisualAttentionDelayMs(float v) { this.visualAttentionDelayMs = v; }

    /**
     * Validate configuration values
     */
    public void validate() {
        if (typingMeanMs <= 0 || typingStdDevMs <= 0) {
            HookUtils.logWarn("Invalid typing parameters, using defaults");
            typingMeanMs = 120f;
            typingStdDevMs = 45f;
        }
        if (basePressure < 0 || basePressure > 1) {
            basePressure = 0.6f;
        }
        if (scrollFriction < 0 || scrollFriction > 1) {
            scrollFriction = 0.15f;
        }
    }
}
