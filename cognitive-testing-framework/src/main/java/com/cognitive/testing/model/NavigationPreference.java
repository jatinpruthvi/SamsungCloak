package com.cognitive.testing.model;

/**
 * Navigation preference enum for cognitive realism.
 * Simulates different user interaction patterns.
 */
public enum NavigationPreference {
    GESTURE_HEAVY,
    BUTTON_HEAVY,
    BALANCED;
    
    /**
     * Get alternative preference for changing preferences
     */
    public NavigationPreference alternative() {
        switch (this) {
            case GESTURE_HEAVY:
                return BUTTON_HEAVY;
            case BUTTON_HEAVY:
                return GESTURE_HEAVY;
            case BALANCED:
                // Randomly switch to one extreme
                return Math.random() < 0.5 ? GESTURE_HEAVY : BUTTON_HEAVY;
            default:
                return BALANCED;
        }
    }
    
    /**
     * Get gesture probability for this preference
     */
    public float getGestureProbability() {
        switch (this) {
            case GESTURE_HEAVY: return 0.80f;
            case BUTTON_HEAVY: return 0.20f;
            case BALANCED: return 0.50f;
            default: return 0.50f;
        }
    }
    
    /**
     * Get button probability for this preference
     */
    public float getButtonProbability() {
        return 1.0f - getGestureProbability();
    }
}
