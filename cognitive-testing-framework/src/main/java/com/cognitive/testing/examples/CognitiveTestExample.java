package com.cognitive.testing.examples;

import com.cognitive.testing.automation.CognitiveTestFramework;
import com.cognitive.testing.model.CognitiveConfig;
import com.cognitive.testing.hooks.*;

import java.util.Arrays;
import java.util.List;

/**
 * Example: Cognitive Realism Testing
 * 
 * Demonstrates how to use the Cognitive Realism framework to test
 * mobile applications with human-like behavior.
 * 
 * This example shows all 6 cognitive hooks in action:
 * 1. Limited Attention Span
 * 2. Bounded Rationality (Satisficing)
 * 3. Emotional Bias Simulation
 * 4. Decision Fatigue
 * 5. Imperfect Memory
 * 6. Changing Preferences
 */
public class CognitiveTestExample {
    
    public static void main(String[] args) {
        System.out.println("=== Cognitive Realism Test Example ===\n");
        
        // Create framework with high-fidelity simulation
        CognitiveConfig config = CognitiveConfig.highFidelity();
        CognitiveTestFramework framework = new CognitiveTestFramework(config);
        
        // Start cognitive session
        framework.startSession();
        
        try {
            // Simulate a typical mobile app flow with cognitive realism
            simulateAppFlow(framework);
            
            // Demonstrate each cognitive hook
            demonstrateAttentionHook(framework);
            demonstrateBoundedRationalityHook(framework);
            demonstrateEmotionalBiasHook(framework);
            demonstrateDecisionFatigueHook(framework);
            demonstrateMemoryHook(framework);
            demonstratePreferencesHook(framework);
            
        } finally {
            // End session and generate report
            framework.endSession();
        }
    }
    
    /**
     * Simulate a typical mobile app flow
     */
    private static void simulateAppFlow(CognitiveTestFramework framework) {
        System.out.println("\n--- Simulating App Flow ---\n");
        
        // Navigate through screens
        framework.navigateTo("HomeScreen");
        
        // Perform actions with cognitive realism
        framework.performAction(() -> {
            System.out.println("Action: Browse content");
        });
        
        framework.navigateTo("FeedScreen");
        framework.performAction(() -> {
            System.out.println("Action: Scroll through feed");
        });
        
        // Check for context switching (limited attention)
        if (framework.shouldContextSwitch()) {
            System.out.println("Context switch triggered - user got distracted");
            long abandonmentTime = framework.simulateAbandonment();
            System.out.println("Abandoned for " + (abandonmentTime / 1000) + " seconds");
        }
        
        framework.navigateTo("DetailScreen");
        framework.performAction(() -> {
            System.out.println("Action: View content details");
        });
        
        // Check for re-verification (imperfect memory)
        int reverifyDepth = framework.shouldReverify();
        if (reverifyDepth > 0) {
            System.out.println("Re-verification triggered - user forgot something");
            System.out.println("Going back " + reverifyDepth + " screens");
        }
        
        framework.leaveScreen("DetailScreen");
    }
    
    /**
     * Demonstrate Limited Attention Hook
     */
    private static void demonstrateAttentionHook(CognitiveTestFramework framework) {
        System.out.println("\n--- Limited Attention Hook ---\n");
        
        LimitedAttentionHook hook = framework.getAttentionHook();
        
        // Simulate multiple interactions to trigger attention drops
        for (int i = 0; i < 10; i++) {
            if (hook.shouldTriggerContextSwitch()) {
                System.out.println("Interaction " + i + ": Context switch occurred");
                hook.simulateAbandonment();
            }
            
            if (hook.shouldCheckNotifications()) {
                System.out.println("Interaction " + i + ": Checking notifications");
                hook.simulateNotificationCheck();
            }
        }
        
        System.out.println("Attention Statistics: " + hook.getStatistics());
    }
    
    /**
     * Demonstrate Bounded Rationality Hook
     */
    private static void demonstrateBoundedRationalityHook(CognitiveTestFramework framework) {
        System.out.println("\n--- Bounded Rationality Hook (Satisficing) ---\n");
        
        BoundedRationalityHook hook = framework.getBoundedRationalityHook();
        
        // Create sample options with different quality scores
        List<String> options = Arrays.asList(
            "Option A (Quality: 0.4)",
            "Option B (Quality: 0.7)",
            "Option C (Quality: 0.9)",
            "Option D (Quality: 0.6)",
            "Option E (Quality: 0.8)"
        );
        
        // Define evaluator
        BoundedRationalityHook.OptionEvaluator<String> evaluator = option -> {
            String[] parts = option.split("\\(Quality: ");
            return Float.parseFloat(parts[1].replace(")", ""));
        };
        
        // Select options multiple times
        System.out.println("Selecting options using satisficing behavior:");
        for (int i = 0; i < 5; i++) {
            String selected = hook.selectOption(options, evaluator, 0.6f);
            System.out.println("  Selection " + i + ": " + selected);
            
            // Check if user would search for better option
            float quality = evaluator.evaluate(selected);
            boolean shouldSearch = hook.shouldSearchForBetter(quality);
            System.out.println("    Quality: " + quality + ", Search for better: " + shouldSearch);
        }
        
        // Calculate max options user would review
        int maxToReview = hook.calculateMaxOptionsToReview(options.size());
        System.out.println("Max options to review: " + maxToReview + " out of " + options.size());
        
        System.out.println("Bounded Rationality Statistics: " + hook.getStatistics());
    }
    
    /**
     * Demonstrate Emotional Bias Hook
     */
    private static void demonstrateEmotionalBiasHook(CognitiveTestFramework framework) {
        System.out.println("\n--- Emotional Bias Hook ---\n");
        
        EmotionalBiasHook hook = framework.getEmotionalBiasHook();
        
        // Simulate multiple interactions to trigger emotional responses
        System.out.println("Simulating emotional interactions:");
        for (int i = 0; i < 15; i++) {
            if (hook.shouldTriggerInteractionBurst()) {
                int burstCount = hook.simulateInteractionBurst();
                System.out.println("Interaction " + i + ": Burst of " + burstCount + " rapid actions");
            }
            
            if (hook.shouldTriggerHesitation()) {
                long hesitationTime = hook.simulateHesitation();
                System.out.println("Interaction " + i + ": Hesitated for " + hesitationTime + "ms");
            }
            
            if (hook.shouldAbandonDueToFrustration()) {
                System.out.println("Interaction " + i + ": Abandoned action due to frustration");
            }
        }
        
        // Calculate current frustration level
        float frustration = hook.calculateFrustrationLevel();
        System.out.println("Current frustration level: " + (frustration * 100) + "%");
        
        System.out.println("Emotional Bias Statistics: " + hook.getStatistics());
    }
    
    /**
     * Demonstrate Decision Fatigue Hook
     */
    private static void demonstrateDecisionFatigueHook(CognitiveTestFramework framework) {
        System.out.println("\n--- Decision Fatigue Hook ---\n");
        
        DecisionFatigueHook hook = framework.getDecisionFatigueHook();
        
        // Simulate many decisions to build up fatigue
        System.out.println("Simulating decision fatigue over time:");
        for (int i = 0; i < 50; i++) {
            int thinkTime = hook.calculateThinkTime();
            
            if (hook.shouldCommitError()) {
                DecisionFatigueHook.DecisionErrorType errorType = hook.getNextErrorType();
                System.out.println("Decision " + i + ": Error occurred - " + errorType);
            }
            
            if (i % 10 == 0 && i > 0) {
                System.out.println("  After " + i + " decisions:");
                System.out.println("    Error rate: " + (hook.calculateCurrentErrorRate() * 100) + "%");
                System.out.println("    Decision quality: " + (hook.calculateDecisionQuality() * 100) + "%");
                System.out.println("    Avg think time: " + hook.getStatistics().averageResponseTimeMs + "ms");
            }
        }
        
        // Simulate mis-click behavior
        int misClickOffset = hook.simulateMisClick(0.9f);
        System.out.println("Mis-click offset: " + misClickOffset + " pixels");
        
        // Check for decision delays
        boolean shouldDelay = hook.shouldDelayDecision();
        System.out.println("Should delay decision: " + shouldDelay);
        
        System.out.println("Decision Fatigue Statistics: " + hook.getStatistics());
    }
    
    /**
     * Demonstrate Imperfect Memory Hook
     */
    private static void demonstrateMemoryHook(CognitiveTestFramework framework) {
        System.out.println("\n--- Imperfect Memory Hook ---\n");
        
        ImperfectMemoryHook hook = framework.getMemoryHook();
        
        // Simulate navigating through multiple screens
        String[] screens = {"Screen1", "Screen2", "Screen3", "Screen4", "Screen5"};
        
        System.out.println("Navigating through screens:");
        for (String screen : screens) {
            hook.visitScreen(screen);
            System.out.println("  Visited: " + screen + " (Depth: " + hook.getNavigationDepth() + ")");
            
            // Check if user is confused about context
            if (hook.isConfusedAboutContext()) {
                System.out.println("    User is confused about current context");
            }
        }
        
        // Calculate memory decay
        long timeSinceLearned = 300000; // 5 minutes
        float importance = 0.7f; // Moderately important
        float forgetProbability = hook.calculateMemoryDecay(timeSinceLearned, importance);
        System.out.println("Memory decay after 5 minutes (importance: 0.7): " + (forgetProbability * 100) + "%");
        
        // Check for re-verification
        System.out.println("Checking for re-verification:");
        for (int i = 0; i < 5; i++) {
            if (hook.shouldReverify()) {
                int depth = hook.simulateReverification();
                System.out.println("  Re-verify iteration " + i + ": Going back " + depth + " screens");
            }
        }
        
        // Check information verification
        boolean shouldVerify = hook.shouldReverifyInformation("userData", System.currentTimeMillis() - 60000, 0.8f);
        System.out.println("Should verify user data: " + shouldVerify);
        
        System.out.println("Imperfect Memory Statistics: " + hook.getStatistics());
    }
    
    /**
     * Demonstrate Changing Preferences Hook
     */
    private static void demonstratePreferencesHook(CognitiveTestFramework framework) {
        System.out.println("\n--- Changing Preferences Hook ---\n");
        
        ChangingPreferencesHook hook = framework.getPreferencesHook();
        
        // Simulate multiple sessions
        System.out.println("Simulating multiple sessions:");
        for (int session = 1; session <= 5; session++) {
            System.out.println("\nSession " + session + ":");
            
            // Visit multiple screens
            for (int i = 0; i < 10; i++) {
                hook.visitScreen("Screen" + i);
                
                // Check for preference changes
                if (hook.shouldChangeNavigationPreference()) {
                    System.out.println("  Navigation preference changed to: " + 
                        framework.getCognitiveState().getNavPreference());
                }
                
                if (hook.shouldChangeInteractionStyle()) {
                    System.out.println("  Interaction style changed to: " + 
                        framework.getCognitiveState().getInteractionStyle());
                }
            }
            
            // Transition to next session
            framework.transitionToNewSession();
        }
        
        // Analyze preference patterns
        ChangingPreferencesHook.PreferenceAnalysis analysis = hook.analyzePreferences();
        System.out.println("\nPreference Analysis:");
        System.out.println("  Navigation changes: " + analysis.navigationChanges);
        System.out.println("  Style changes: " + analysis.interactionStyleChanges);
        System.out.println("  Total changes: " + analysis.totalChanges);
        System.out.println("  Stability score: " + (analysis.stabilityScore * 100) + "%");
        
        // Get gesture preference ratio
        float gestureRatio = hook.getGesturePreferenceRatio();
        System.out.println("  Gesture preference: " + (gestureRatio * 100) + "%");
    }
}
