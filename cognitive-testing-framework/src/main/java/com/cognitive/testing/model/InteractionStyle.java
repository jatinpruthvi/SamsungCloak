package com.cognitive.testing.model;

/**
 * Interaction style enum for cognitive realism.
 * Simulates different levels of precision in user interactions.
 */
public enum InteractionStyle {
    PRECISE,
    CASUAL,
    ERRATIC;
    
    /**
     * Get alternative style for changing preferences
     */
    public InteractionStyle alternative() {
        switch (this) {
            case PRECISE:
                return CASUAL;
            case CASUAL:
                // Randomly switch between precise and erratic
                return Math.random() < 0.5 ? PRECISE : ERRATIC;
            case ERRATIC:
                return CASUAL;
            default:
                return PRECISE;
        }
    }
    
    /**
     * Get touch precision offset (pixels) for this style
     * Simulates how far from center clicks may be
     */
    public int getTouchOffset() {
        switch (this) {
            case PRECISE: return 0;
            case CASUAL: return 5;
            case ERRATIC: return 15;
            default: return 5;
        }
    }
    
    /**
     * Get swipe consistency (0.0 - 1.0)
     * How consistent swipe gestures are
     */
    public float getSwipeConsistency() {
        switch (this) {
            case PRECISE: return 0.95f;
            case CASUAL: return 0.80f;
            case ERRATIC: return 0.60f;
            default: return 0.80f;
        }
    }
    
    /**
     * Get multi-tap probability
     * Probability of accidental double-taps
     */
    public float getMultiTapProbability() {
        switch (this) {
            case PRECISE: return 0.01f;
            case CASUAL: return 0.05f;
            case ERRATIC: return 0.12f;
            default: return 0.05f;
        }
    }
}
