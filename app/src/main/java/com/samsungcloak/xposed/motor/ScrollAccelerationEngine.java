package com.samsungcloak.xposed.motor;

import com.samsungcloak.xposed.HookUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Hook 4: Scroll Acceleration Engine
 * 
 * Implements "Flick-to-Scroll" logic with natural thumb friction simulation.
 * Includes:
 * - Decaying velocity (momentum-based scrolling)
 * - Overscroll with bounce-back effect
 * - Fitts's Law compliant gesture timing
 * 
 * Natural scroll characteristics:
 * - Initial flick determines velocity
 * - Friction gradually slows the scroll
 * - Overscroll at boundaries with elastic bounce
 * - Velocity varies with finger speed
 */
public class ScrollAccelerationEngine {
    private final Random random;
    private final NeuromotorConfig config;
    
    // Scroll state
    private float currentVelocity = 0f;
    private long lastScrollTime = 0;
    private float lastScrollPosition = 0f;
    private boolean isDecelerating = false;

    public ScrollAccelerationEngine(NeuromotorConfig config) {
        this.config = config;
        this.random = new Random();
    }

    public void updateConfig(NeuromotorConfig config) {
        this.config = config;
    }

    /**
     * Calculate scroll velocity at a given elapsed time
     * 
     * Uses exponential decay to simulate friction:
     * v(t) = v0 * e^(-k*t)
     * 
     * @param initialVelocity Initial flick velocity in pixels/ms
     * @param elapsedMs Time elapsed since flick
     * @return Current velocity in pixels/ms
     */
    public float getScrollVelocityAtTime(float initialVelocity, long elapsedMs) {
        if (elapsedMs == 0) {
            return initialVelocity;
        }
        
        // Apply velocity decay using exponential model
        float decayFactor = (float) Math.exp(-config.getVelocityDecayRate() * (elapsedMs / 1000.0));
        
        // Add slight randomness to decay for natural feel
        float noise = 1f + (float) (random.nextGaussian() * 0.02);
        
        float velocity = initialVelocity * decayFactor * noise;
        
        // Apply friction coefficient
        velocity *= (1f - config.getScrollFriction());
        
        // Clamp to minimum threshold
        if (Math.abs(velocity) < config.getMinFlickVelocity() / 1000f) {
            velocity = 0;
        }
        
        return velocity;
    }

    /**
     * Calculate overscroll distance based on momentum
     * 
     * When velocity brings scroll past boundaries, calculate the bounce distance
     * 
     * @param velocity Current velocity in pixels/ms
     * @param friction Friction coefficient (0-1)
     * @return Overscroll distance in pixels (negative if before start)
     */
    public float getOverscrollDistance(float velocity, float friction) {
        if (velocity == 0) return 0;
        
        // Calculate kinetic energy
        float energy = 0.5f * velocity * velocity;
        
        // Convert to distance using spring constant (bounce)
        float overscroll = energy * config.getOverscrollBounce() / (1f - friction);
        
        // Clamp to maximum
        overscroll = clamp(overscroll, -config.getMaxOverscrollPx(), config.getMaxOverscrollPx());
        
        // Add random variation
        overscroll *= (1f + (float) (random.nextGaussian() * 0.1));
        
        HookUtils.logDebug("Overscroll: " + overscroll + "px at velocity " + velocity);
        
        return overscroll;
    }

    /**
     * Generate a complete flick gesture with natural motion
     * 
     * @param startX Start X coordinate
     * @param startY Start Y coordinate  
     * @param endX End X coordinate (direction of flick)
     * @param endY End Y coordinate
     * @param durationMs Duration of the gesture
     * @return List of [x, y, velocity] arrays for each frame
     */
    public List<float[]> generateFlickGesture(float startX, float startY, float endX, float endY, long durationMs) {
        List<float[]> frames = new ArrayList<>();
        
        // Calculate direction vector
        float dx = endX - startX;
        float dy = endY - startY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        
        if (distance == 0) {
            return frames;
        }
        
        // Normalize direction
        float dirX = dx / distance;
        float dirY = dy / distance;
        
        // Calculate initial velocity (pixels per ms)
        float initialVelocity = distance / durationMs;
        
        // Apply natural feel multiplier
        initialVelocity *= config.getFlickVelocityMultiplier();
        
        // Add human variation to velocity
        initialVelocity *= (0.8f + (float) (random.nextDouble() * 0.4));
        
        // Generate frames at ~60fps
        int numFrames = (int) (durationMs / 16.67f);
        
        for (int i = 0; i < numFrames; i++) {
            float progress = (float) i / numFrames;
            long elapsedMs = (long) (progress * durationMs);
            
            // Get current velocity
            float currentVelocity = getScrollVelocityAtTime(initialVelocity, elapsedMs);
            
            // Calculate position based on accumulated velocity
            float distanceTraveled = calculateDistanceTraveled(initialVelocity, elapsedMs);
            
            float x = startX + dirX * distanceTraveled;
            float y = startY + dirY * distanceTraveled;
            
            // Add slight perpendicular drift for natural imperfection
            float drift = (float) (random.nextGaussian() * distance * 0.01);
            x += -dirY * drift;
            y += dirX * drift;
            
            frames.add(new float[]{x, y, currentVelocity});
        }
        
        // Add final frame at full distance
        frames.add(new float[]{endX, endY, 0f});
        
        HookUtils.logDebug("Generated flick: " + frames.size() + " frames, " + 
                          distance + "px distance, " + initialVelocity + " px/ms initial velocity");
        
        return frames;
    }

    /**
     * Calculate total distance traveled given initial velocity and time
     * Using integral of exponential decay: integral of v0 * e^(-kt) dt
     */
    private float calculateDistanceTraveled(float initialVelocity, long elapsedMs) {
        if (elapsedMs == 0 || initialVelocity == 0) return 0;
        
        float decayRate = config.getVelocityDecayRate();
        
        // Integral: v0 * (1 - e^(-kt)) / k
        float decayFactor = (float) (1 - Math.exp(-decayRate * (elapsedMs / 1000.0)));
        
        // Apply friction
        float friction = 1f - config.getScrollFriction();
        
        return initialVelocity * decayFactor * friction / decayRate;
    }

    /**
     * Calculate time to complete stop from initial velocity
     */
    public long getStoppingTime(float initialVelocity) {
        if (initialVelocity <= 0) return 0;
        
        float decayRate = config.getVelocityDecayRate();
        float friction = 1f - config.getScrollFriction();
        
        // Time to reach minimum velocity threshold
        float minVel = config.getMinFlickVelocity() / 1000f;
        
        // Solve: minVel = initialVelocity * e^(-k*t) * friction
        // t = -ln(minVel / (initialVelocity * friction)) / k
        
        double t = -Math.log(minVel / (initialVelocity * friction)) / decayRate;
        
        return (long) (t * 1000);
    }

    /**
     * Calculate deceleration (negative acceleration) at current velocity
     */
    public float getDeceleration(float currentVelocity) {
        float decayRate = config.getVelocityDecayRate();
        float friction = config.getScrollFriction();
        
        // Deceleration = velocity * decayRate + friction_effect
        float deceleration = currentVelocity * decayRate + (friction * currentVelocity);
        
        return -deceleration;
    }

    /**
     * Update scroll state with new position
     */
    public void updateScrollState(float position) {
        long now = System.currentTimeMillis();
        
        if (lastScrollTime > 0) {
            long deltaTime = now - lastScrollTime;
            
            if (deltaTime > 0) {
                // Calculate velocity
                float deltaPosition = position - lastScrollPosition;
                currentVelocity = deltaPosition / deltaTime;
                
                // Check if decelerating
                if (Math.abs(currentVelocity) < config.getMinFlickVelocity() / 1000f) {
                    isDecelerating = false;
                }
            }
        }
        
        lastScrollPosition = position;
        lastScrollTime = now;
    }

    /**
     * Start a new scroll sequence (flick detected)
     */
    public void startScroll(float initialVelocity) {
        this.currentVelocity = initialVelocity;
        this.isDecelerating = true;
        lastScrollTime = System.currentTimeMillis();
        
        HookUtils.logDebug("Scroll started with velocity: " + initialVelocity);
    }

    /**
     * End scroll sequence
     */
    public void endScroll() {
        this.isDecelerating = false;
        
        HookUtils.logDebug("Scroll ended");
    }

    /**
     * Get current scroll state
     */
    public ScrollState getScrollState() {
        return new ScrollState(
            currentVelocity,
            isDecelerating,
            lastScrollPosition,
            System.currentTimeMillis() - lastScrollTime
        );
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Scroll state holder
     */
    public static class ScrollState {
        public final float velocity;
        public final boolean isDecelerating;
        public final float position;
        public final long idleTime;

        public ScrollState(float velocity, boolean isDecelerating, 
                          float position, long idleTime) {
            this.velocity = velocity;
            this.isDecelerating = isDecelerating;
            this.position = position;
            this.idleTime = idleTime;
        }
    }
}
