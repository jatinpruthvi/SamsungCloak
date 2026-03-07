package com.samsungcloak.xposed;

import com.samsungcloak.xposed.motor.*;

import java.util.List;

/**
 * NeuromotorRealismDemo - Demonstration of Motor Realism Hooks
 * 
 * This class demonstrates the five Neuromotor Interaction Profile hooks
 * for high-fidelity hardware-in-the-loop testing.
 * 
 * Run this demo to see realistic human motor control simulation in action.
 * 
 * @see NeuromotorInteractionProfile
 * @see TypingCadenceEngine
 * @see MotionCurveEngine
 * @see TouchPressureEngine
 * @see ScrollAccelerationEngine
 * @see ReactionTimeEngine
 */
public class NeuromotorRealismDemo {
    private static final String LOG_TAG = "Neuromotor.Demo";

    public static void main(String[] args) {
        HookUtils.logInfo("=== Neuromotor Interaction Profile Demo ===");
        
        // Get the singleton profile
        NeuromotorInteractionProfile profile = NeuromotorInteractionProfile.getInstance();
        
        // Optionally customize configuration
        NeuromotorConfig config = new NeuromotorConfig();
        config.setTypingMeanMs(110f); // Slightly faster typist
        config.setFatFingerProbability(0.3f); // More fat finger events
        profile.updateConfig(config);
        
        // Run demonstrations for each hook
        demonstrateTypingCadence(profile);
        demonstrateMotionCurves(profile);
        demonstrateTouchPressure(profile);
        demonstrateScrollAcceleration(profile);
        demonstrateReactionTime(profile);
        
        // Demonstrate complete neuromotor profile application
        demonstrateCompleteProfile(profile);
        
        HookUtils.logInfo("=== Demo Complete ===");
    }

    /**
     * Hook 1: Typing Cadence (Inter-Key Latency)
     */
    private static void demonstrateTypingCadence(NeuromotorInteractionProfile profile) {
        HookUtils.logInfo("\n--- Hook 1: Typing Cadence ---");
        
        // Simulate typing a phrase
        String testPhrase = "Hello World";
        
        HookUtils.logInfo("Typing: \"" + testPhrase + "\"");
        
        for (int i = 0; i < testPhrase.length(); i++) {
            char c = testPhrase.charAt(i);
            
            // Check if we should trigger a burst
            if (profile.shouldTriggerBurst()) {
                HookUtils.logInfo("  [BURST MODE]");
            }
            
            // Check if we should trigger a correction
            if (profile.shouldTriggerCorrection()) {
                HookUtils.logInfo("  [CORRECTION] Backspace");
                // Simulate correction delay
                try { Thread.sleep(50); } catch (InterruptedException e) {}
            }
            
            // Get inter-key latency
            long latency = profile.getInterKeyLatency(
                profile.shouldTriggerBurst(), 
                profile.shouldTriggerCorrection()
            );
            
            HookUtils.logInfo("  Key '" + c + "': " + latency + "ms delay");
            
            try { Thread.sleep(latency); } catch (InterruptedException e) {}
        }
        
        HookUtils.logInfo("Typing complete!");
    }

    /**
     * Hook 2: Non-Linear Motion Curves (Fitts's Law)
     */
    private static void demonstrateMotionCurves(NeuromotorInteractionProfile profile) {
        HookUtils.logInfo("\n--- Hook 2: Non-Linear Motion Curves ---");
        
        // Generate a swipe motion from (100, 500) to (400, 500)
        float startX = 100f, startY = 500f;
        float endX = 400f, endY = 500f;
        
        HookUtils.logInfo("Generating motion path: (" + startX + "," + startY + 
                         ") -> (" + endX + "," + endY + ")");
        
        float[] path = profile.generateMotionPath(startX, startY, endX, endY, 10);
        
        HookUtils.logInfo("Motion path points:");
        for (int i = 0; i < path.length; i += 2) {
            float progress = i / 2f / 9f;
            float velocity = profile.getVelocityAtProgress(progress);
            HookUtils.logInfo("  Point " + (i/2) + ": (" + 
                             String.format("%.1f", path[i]) + ", " + 
                             String.format("%.1f", path[i+1]) + 
                             ") velocity: " + String.format("%.2f", velocity));
        }
        
        // Calculate Fitts's Law values
        float distance = 300f; // 400 - 100
        double id = Math.log(2 * distance / 50) / Math.log(2);
        long mt = (long) (50 + 80 * id);
        
        HookUtils.logInfo("Fitts's Law: ID=" + String.format("%.2f", id) + 
                         ", MT=" + mt + "ms");
    }

    /**
     * Hook 3: Touch Pressure & Contact Area Variability
     */
    private static void demonstrateTouchPressure(NeuromotorInteractionProfile profile) {
        HookUtils.logInfo("\n--- Hook 3: Touch Pressure & Contact Area ---");
        
        HookUtils.logInfo("Generating realistic touch parameters:");
        
        // Simulate different touch contexts
        TouchPressureEngine.TouchContext[] contexts = {
            TouchPressureEngine.TouchContext.PRECISE_TAP,
            TouchPressureEngine.TouchContext.CASUAL_TAP,
            TouchPressureEngine.TouchContext.FAST_SWIPE,
            TouchPressureEngine.TouchContext.FAT_FINGER
        };
        
        for (TouchPressureEngine.TouchContext ctx : contexts) {
            // We need to access the engine directly for this demo
            // In production, these would be called from the hooks
            HookUtils.logInfo("  Context: " + ctx);
        }
        
        HookUtils.logInfo("Typical values:");
        HookUtils.logInfo("  Pressure: 0.4-0.9 (variable)");
        HookUtils.logInfo("  Contact Size: 0.5-1.2 (normalized)");
        HookUtils.logInfo("  Touch Major: 0.3-1.0");
        HookUtils.logInfo("  Touch Minor: 0.2-0.8");
        
        HookUtils.logInfo("Fat Finger probability: " + 
                         (config.getFatFingerProbability() * 100) + "%");
    }

    /**
     * Hook 4: Scroll Acceleration Patterns
     */
    private static void demonstrateScrollAcceleration(NeuromotorInteractionProfile profile) {
        HookUtils.logInfo("\n--- Hook 4: Scroll Acceleration ---");
        
        // Simulate a flick gesture
        float startX = 200f, startY = 400f;
        float endX = 200f, endY = 800f; // Downward flick
        
        HookUtils.logInfo("Generating flick gesture: (" + startX + "," + startY + 
                         ") -> (" + endX + "," + endY + ")");
        
        long duration = 200; // 200ms flick
        
        // Note: Need engine access for full demo
        // In actual hooks, this integrates with MotionEvent system
        
        // Demonstrate velocity decay
        float initialVelocity = 2.5f; // 2.5 px/ms
        
        HookUtils.logInfo("Velocity decay over time:");
        for (int t = 0; t <= 500; t += 100) {
            float vel = profile.getScrollVelocityAtTime(initialVelocity, t);
            HookUtils.logInfo("  t=" + t + "ms: " + String.format("%.2f", vel) + " px/ms");
        }
        
        // Overscroll calculation
        float overscroll = profile.getOverscrollDistance(initialVelocity, 0.15f);
        HookUtils.logInfo("Overscroll distance: " + String.format("%.1f", overscroll) + "px");
    }

    /**
     * Hook 5: Reaction Time Distribution
     */
    private static void demonstrateReactionTime(NeuromotorInteractionProfile profile) {
        HookUtils.logInfo("\n--- Hook 5: Reaction Time Distribution ---");
        
        // Simple tap reaction
        HookUtils.logInfo("Simple tap reaction times (5 samples):");
        for (int i = 0; i < 5; i++) {
            long delay = profile.getReactionDelay(false, false);
            HookUtils.logInfo("  Sample " + (i+1) + ": " + delay + "ms");
        }
        
        // Complex task reaction
        HookUtils.logInfo("Complex task reaction times (5 samples):");
        for (int i = 0; i < 5; i++) {
            long delay = profile.getReactionDelay(true, false);
            HookUtils.logInfo("  Sample " + (i+1) + ": " + delay + "ms");
        }
        
        // Fatigued reaction
        HookUtils.logInfo("Fatigued state reaction times (5 samples):");
        for (int i = 0; i < 5; i++) {
            long delay = profile.getReactionDelay(false, true);
            HookUtils.logInfo("  Sample " + (i+1) + ": " + delay + "ms");
        }
        
        // Perceptual delay
        HookUtils.logInfo("Perceptual delay (UI element appearing): " + 
                         profile.getPerceptualDelay() + "ms");
        
        // Cognitive processing delays
        HookUtils.logInfo("Cognitive processing delays by complexity:");
        for (int level = 0; level <= 4; level++) {
            long delay = profile.getCognitiveProcessingDelay(level);
            String[] names = {"Simple", "Decision", "Reading", "Problem", "Reasoning"};
            HookUtils.logInfo("  Level " + level + " (" + names[level] + "): " + delay + "ms");
        }
    }

    /**
     * Complete neuromotor profile demonstration
     */
    private static void demonstrateCompleteProfile(NeuromotorInteractionProfile profile) {
        HookUtils.logInfo("\n--- Complete Neuromotor Profile ---");
        
        HookUtils.logInfo("Summary of Motor Realism Hooks:");
        HookUtils.logInfo("");
        HookUtils.logInfo("1. Typing Cadence:");
        HookUtils.logInfo("   - Log-normal inter-key delays");
        HookUtils.logInfo("   - Burst typing simulation");
        HookUtils.logInfo("   - Backspace-correction events");
        HookUtils.logInfo("");
        HookUtils.logInfo("2. Non-Linear Motion Curves:");
        HookUtils.logInfo("   - Bell-shaped velocity profile");
        HookUtils.logInfo("   - Fitts's Law compliance");
        HookUtils.logInfo("   - Acceleration/deceleration phases");
        HookUtils.logInfo("");
        HookUtils.logInfo("3. Touch Pressure Variability:");
        HookUtils.logInfo("   - Randomized pressure (0.2-1.0)");
        HookUtils.logInfo("   - Fat Finger effect simulation");
        HookUtils.logInfo("   - Contact area variation");
        HookUtils.logInfo("");
        HookUtils.logInfo("4. Scroll Acceleration:");
        HookUtils.logInfo("   - Exponential velocity decay");
        HookUtils.logInfo("   - Momentum-based overscroll");
        HookUtils.logInfo("   - Friction simulation");
        HookUtils.logInfo("");
        HookUtils.logInfo("5. Reaction Time Distribution:");
        HookUtils.logInfo("   - Log-normal perceptual delay");
        HookUtils.logInfo("   - Complexity-based delays");
        HookUtils.logInfo("   - Fatigue simulation");
        
        HookUtils.logInfo("");
        HookUtils.logInfo("These hooks ensure automation reflects the reality that");
        HookUtils.logInfo("'Humans are imprecise and inconsistent' for high-fidelity");
        HookUtils.logInfo("biometric and accessibility testing on Samsung Galaxy A12.");
    }

    private static NeuromotorConfig config = new NeuromotorConfig();
}
