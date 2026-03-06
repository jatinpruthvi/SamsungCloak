package com.cognitive.testing.model;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Core cognitive state model for human-like testing behavior.
 * Tracks attention, fatigue, stress, and decision metrics over time.
 */
public class CognitiveState {
    
    // Session tracking
    private final long sessionStartTime;
    private final String sessionId;
    private final AtomicInteger interactionCount;
    
    // Cognitive metrics (0.0 - 1.0 scale)
    private volatile float attentionLevel;
    private volatile float fatigueLevel;
    private volatile float stressLevel;
    private volatile float decisionErrorRate;
    
    // Behavioral patterns
    private volatile NavigationPreference navPreference;
    private volatile InteractionStyle interactionStyle;
    
    // Random instance with controlled seed for reproducibility
    private final Random random;
    
    // Configuration
    private final CognitiveConfig config;
    
    public CognitiveState(CognitiveConfig config) {
        this.sessionStartTime = System.currentTimeMillis();
        this.sessionId = "SESSION-" + System.currentTimeMillis();
        this.interactionCount = new AtomicInteger(0);
        this.attentionLevel = 1.0f;
        this.fatigueLevel = 0.0f;
        this.stressLevel = 0.2f;
        this.decisionErrorRate = config.getBaseErrorRate();
        this.navPreference = config.getDefaultNavPreference();
        this.interactionStyle = config.getDefaultInteractionStyle();
        this.random = new Random(config.getRandomSeed());
        this.config = config;
    }
    
    /**
     * Update cognitive state after each interaction
     */
    public void recordInteraction() {
        int count = interactionCount.incrementAndGet();
        long sessionDuration = System.currentTimeMillis() - sessionStartTime;
        
        // Update fatigue based on session duration
        updateFatigue(sessionDuration);
        
        // Update attention with random fluctuations
        updateAttention();
        
        // Update stress based on interaction intensity
        updateStress(count);
        
        // Update decision error rate based on current state
        updateDecisionErrorRate();
    }
    
    private void updateFatigue(long sessionDuration) {
        int minutes = (int) (sessionDuration / 60000);
        
        if (minutes < 30) {
            fatigueLevel = 0.0f + (minutes / 30.0f) * 0.3f;
        } else if (minutes < 60) {
            fatigueLevel = 0.3f + ((minutes - 30) / 30.0f) * 0.3f;
        } else if (minutes < 120) {
            fatigueLevel = 0.6f + ((minutes - 60) / 60.0f) * 0.4f;
        } else if (minutes < 240) {
            fatigueLevel = 1.0f + ((minutes - 120) / 120.0f) * 0.5f;
        } else {
            fatigueLevel = 1.5f + ((minutes - 240) / 240.0f) * 1.0f;
        }
        
        // Cap fatigue at 3.0
        fatigueLevel = Math.min(3.0f, fatigueLevel);
    }
    
    private void updateAttention() {
        // Hour-based attention patterns
        int hourOfDay = java.time.LocalTime.now().getHour();
        float baseAttention = 1.0f;
        
        if (hourOfDay >= 0 && hourOfDay < 6) {
            baseAttention = 0.8f; // Late night
        } else if (hourOfDay >= 6 && hourOfDay < 12) {
            baseAttention = 0.95f; // Morning
        } else if (hourOfDay >= 12 && hourOfDay < 18) {
            baseAttention = 0.9f; // Afternoon
        } else if (hourOfDay >= 18 && hourOfDay < 24) {
            baseAttention = 0.85f; // Evening
        }
        
        // Random attention drops (5% probability)
        if (random.nextFloat() < 0.05) {
            attentionLevel = baseAttention * (0.5f + random.nextFloat() * 0.3f);
        } else {
            // Gradual recovery from drops
            attentionLevel = attentionLevel * 0.9f + baseAttention * 0.1f;
        }
        
        attentionLevel = Math.min(1.0f, Math.max(0.3f, attentionLevel));
    }
    
    private void updateStress(int interactionCount) {
        // Stress increases with interaction intensity
        float intensity = Math.min(1.0f, interactionCount / 100.0f);
        
        // Stress accumulates with fatigue
        float fatigueContribution = fatigueLevel * 0.1f;
        
        // Random stress spikes
        if (random.nextFloat() < 0.03) {
            stressLevel = Math.min(1.0f, stressLevel + 0.2f);
        }
        
        // Gradual stress decay
        stressLevel = stressLevel * 0.95f + (0.2f + intensity + fatigueContribution) * 0.05f;
        stressLevel = Math.min(1.0f, Math.max(0.0f, stressLevel));
    }
    
    private void updateDecisionErrorRate() {
        // Base error rate from config
        float errorRate = config.getBaseErrorRate();
        
        // Fatigue increases error rate
        errorRate += fatigueLevel * config.getFatigueErrorMultiplier();
        
        // Low attention increases error rate
        float attentionDeficit = 1.0f - attentionLevel;
        errorRate += attentionDeficit * config.getAttentionErrorMultiplier();
        
        // High stress increases error rate
        errorRate += stressLevel * config.getStressErrorMultiplier();
        
        // Cap error rate at max
        errorRate = Math.min(config.getMaxErrorRate(), errorRate);
        
        this.decisionErrorRate = errorRate;
    }
    
    // Getter methods
    public long getSessionDuration() {
        return System.currentTimeMillis() - sessionStartTime;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public int getInteractionCount() {
        return interactionCount.get();
    }
    
    public float getAttentionLevel() {
        return attentionLevel;
    }
    
    public float getFatigueLevel() {
        return fatigueLevel;
    }
    
    public float getStressLevel() {
        return stressLevel;
    }
    
    public float getDecisionErrorRate() {
        return decisionErrorRate;
    }
    
    public NavigationPreference getNavPreference() {
        return navPreference;
    }
    
    public InteractionStyle getInteractionStyle() {
        return interactionStyle;
    }
    
    public Random getRandom() {
        return random;
    }
    
    public CognitiveConfig getConfig() {
        return config;
    }
    
    /**
     * Simulate changing preferences between sessions
     */
    public void varyPreferences() {
        // 30% chance to switch navigation preference
        if (random.nextFloat() < 0.30) {
            this.navPreference = navPreference.alternative();
        }
        
        // 25% chance to switch interaction style
        if (random.nextFloat() < 0.25) {
            this.interactionStyle = interactionStyle.alternative();
        }
    }
    
    /**
     * Reset session state while preserving preferences
     */
    public void resetSession() {
        interactionCount.set(0);
        attentionLevel = 1.0f;
        fatigueLevel = 0.0f;
        stressLevel = 0.2f;
    }
    
    /**
     * Check if context switching should occur (limited attention span)
     */
    public boolean shouldContextSwitch() {
        return attentionLevel < config.getContextSwitchThreshold() && 
               random.nextFloat() < config.getContextSwitchProbability();
    }
    
    /**
     * Check if re-verification should occur (imperfect memory)
     */
    public boolean shouldReverify() {
        return (1.0f - attentionLevel) > config.getMemoryThreshold() &&
               random.nextFloat() < config.getReverificationProbability();
    }
    
    /**
     * Navigation preference types
     */
    public enum NavigationPreference {
        GESTURE_HEAVY,
        BUTTON_HEAVY,
        BALANCED;
        
        public NavigationPreference alternative() {
            switch (this) {
                case GESTURE_HEAVY: return BUTTON_HEAVY;
                case BUTTON_HEAVY: return GESTURE_HEAVY;
                case BALANCED: return random.nextBoolean() ? GESTURE_HEAVY : BUTTON_HEAVY;
                default: return BALANCED;
            }
        }
    }
    
    /**
     * Interaction style types
     */
    public enum InteractionStyle {
        PRECISE,
        CASUAL,
        ERRATIC;
        
        public InteractionStyle alternative() {
            switch (this) {
                case PRECISE: return CASUAL;
                case CASUAL: return random.nextBoolean() ? PRECISE : ERRATIC;
                case ERRATIC: return CASUAL;
                default: return PRECISE;
            }
        }
    }
}
