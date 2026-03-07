package com.cognitive.testing.examples;

import com.cognitive.testing.automation.SocialTestFramework;
import com.cognitive.testing.model.SocialConfig;
import com.cognitive.testing.hooks.HerdBehaviorHook;
import com.cognitive.testing.hooks.PeerInfluenceHook;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Demo: Social Realism Testing
 * 
 * This example demonstrates how to use the SocialTestFramework for
 * high-fidelity performance auditing with collective user dynamics.
 * 
 * Target: Samsung Galaxy A12 (SM-A125U)
 * 
 * Run with:
 * java -cp target/classes com.cognitive.testing.examples.SocialRealismDemo
 */
public class SocialRealismDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Social Realism Testing Demo ===\n");
        
        // Create framework with high-fidelity configuration
        SocialConfig config = SocialConfig.highFidelity();
        SocialTestFramework framework = new SocialTestFramework(config);
        
        // Start session
        framework.startSession();
        
        // ============ Demo 1: Herd Behavior ============
        System.out.println("\n--- Demo 1: Herd Behavior ---");
        demonstrateHerdBehavior(framework);
        
        // ============ Demo 2: Social Proof ============
        System.out.println("\n--- Demo 2: Social Proof ---");
        demonstrateSocialProof(framework);
        
        // ============ Demo 3: Trend Participation ============
        System.out.println("\n--- Demo 3: Trend Participation ---");
        demonstrateTrendParticipation(framework);
        
        // ============ Demo 4: Group Conformity ============
        System.out.println("\n--- Demo 4: Group Conformity ---");
        demonstrateGroupConformity(framework);
        
        // ============ Demo 5: Peer Influence ============
        System.out.println("\n--- Demo 5: Peer Influence ---");
        demonstratePeerInfluence(framework);
        
        // ============ Demo 6: Combined Social Dynamics ============
        System.out.println("\n--- Demo 6: Combined Social Dynamics ---");
        demonstrateCombinedDynamics(framework);
        
        // End session and get report
        framework.endSession();
    }
    
    private static void demonstrateHerdBehavior(SocialTestFramework framework) {
        // Create sample content items with different engagement counts
        List<ContentItem> items = new ArrayList<>();
        items.add(new ContentItem("Post A", 15000L));  // High engagement
        items.add(new ContentItem("Post B", 500L));    // Medium engagement
        items.add(new ContentItem("Post C", 25L));    // Low engagement
        
        // Select with herd effect - high engagement items more likely
        ContentItem selected = framework.selectWithHerdEffect(items, ContentItem::getViewCount);
        System.out.println("Selected with herd effect: " + selected.name + 
            " (views: " + selected.viewCount + ")");
        
        // Check if herd effect triggers
        if (framework.shouldTriggerHerdEffect(HerdBehaviorHook.ActionType.LIKE)) {
            System.out.println("Herd effect triggered! Followers engaging...");
        }
        
        // Get current herd influence level
        System.out.println("Current herd influence: " + 
            (framework.getHerdInfluence() * 100) + "%");
    }
    
    private static void demonstrateSocialProof(SocialTestFramework framework) {
        // Create items with social proof labels
        Map<String, Long> labeledItems = new HashMap<>();
        labeledItems.put("Trending Now", 50000L);
        labeledItems.put("Popular", 10000L);
        labeledItems.put("New", 100L);
        labeledItems.put("Sponsored", 50L);
        
        // Select based on social proof labels
        String selected = framework.selectWithProofLabel(
            new HashMap<>(Map.of(
                "Trending Now", "trending",
                "Popular", "popular", 
                "New", "new",
                "Sponsored", "sponsored"
            ))
        );
        
        System.out.println("Selected based on label: " + selected);
        
        // Test click decision with social proof
        boolean shouldClick = framework.shouldClickBasedOnProof(10000L, 500L, 100L);
        System.out.println("Should click (high engagement): " + shouldClick);
        
        shouldClick = framework.shouldClickBasedOnProof(10L, 1L, 0L);
        System.out.println("Should click (low engagement): " + shouldClick);
        
        System.out.println("Social proof susceptibility: " + 
            (framework.getSocialProofSusceptibility() * 100) + "%");
    }
    
    private static void demonstrateTrendParticipation(SocialTestFramework framework) {
        // Check if viral surge is active
        if (framework.isViralSurgeActive()) {
            System.out.println("VIRAL SURGE ACTIVE!");
            System.out.println("Current trend: " + framework.getCurrentTrend());
            System.out.println("Surge intensity: " + 
                (framework.getSurgeIntensity() * 100) + "%");
        } else {
            System.out.println("No viral surge active yet");
        }
        
        // Get trending categories
        List<String> trends = framework.getTrendingCategories(3);
        System.out.println("Trending categories: " + trends);
        
        // Select with trend bias
        List<ContentItem> contentList = new ArrayList<>();
        contentList.add(new ContentItem("Viral Video", 100000L, "viral_video"));
        contentList.add(new ContentItem("Tech News", 5000L, "tech"));
        contentList.add(new ContentItem("Random Post", 100L, "random"));
        
        ContentItem selected = framework.selectWithTrendBias(contentList, ContentItem::getCategory);
        System.out.println("Selected with trend bias: " + selected.name);
        
        // Check if should focus on trend
        if (framework.shouldFocusOnTrend()) {
            System.out.println("Focusing on trend for this action");
        }
        
        System.out.println("Trend alignment: " + 
            (framework.getTrendAlignment() * 100) + "%");
    }
    
    private static void demonstrateGroupConformity(SocialTestFramework framework) {
        // Sample community behavior
        framework.adjustForConformity();
        
        // Get current speed (affected by conformity)
        System.out.println("Current interaction speed: " + framework.getCurrentSpeed());
        System.out.println("Community mean speed: " + framework.getCommunityMeanSpeed());
        
        // Calculate conformity-adjusted delay
        long baseDelay = 1000; // 1 second
        long adjustedDelay = framework.calculateConformityDelay(baseDelay);
        System.out.println("Base delay: " + baseDelay + "ms -> Adjusted: " + adjustedDelay + "ms");
        
        // Check if conforming
        System.out.println("Is conforming: " + framework.isConforming());
        
        // Calculate adjusted timeout
        long timeout = framework.calculateAdjustedTimeout(5000);
        System.out.println("Adjusted timeout: " + timeout + "ms");
    }
    
    private static void demonstratePeerInfluence(SocialTestFramework framework) {
        // Observe peer actions
        framework.observePeerActions();
        
        // Get most likely peer action
        PeerInfluenceHook.PeerActionType likelyAction = framework.getMostLikelyPeerAction();
        System.out.println("Most likely peer action: " + likelyAction.getName());
        
        // Determine next action based on peer influence
        PeerInfluenceHook.PeerActionType nextAction = framework.determinePeerAction();
        System.out.println("Next action (peer-influenced): " + nextAction.getName());
        
        // Get peer action distribution
        float[] distribution = framework.getPeerActionDistribution();
        System.out.print("Peer action distribution: [");
        for (int i = 0; i < Math.min(5, distribution.length); i++) {
            System.out.printf("%.2f ", distribution[i]);
        }
        System.out.println("...]");
        
        System.out.println("Peer influence weight: " + 
            (framework.getPeerInfluenceWeight() * 100) + "%");
    }
    
    private static void demonstrateCombinedDynamics(SocialTestFramework framework) {
        // Create action context with all social dynamics information
        SocialTestFramework.ActionContext context = new SocialTestFramework.ActionContext()
            .triggerAction(HerdBehaviorHook.ActionType.LIKE)
            .actionType(PeerInfluenceHook.PeerActionType.LIKE)
            .viewCount(50000L)
            .likeCount(2500L)
            .commentCount(500L)
            .baseDelayMs(500L)
            .category("viral_video");
        
        // Simulate action with all social dynamics
        System.out.println("Performing action with all social dynamics...");
        
        framework.performActionWithSocialDynamics(
            () -> System.out.println("  -> Action executed!"),
            context
        );
        
        // Perform another action
        context.viewCount(100L)
            .likeCount(5L)
            .commentCount(0L);
        
        framework.performActionWithSocialDynamics(
            () -> System.out.println("  -> Low engagement action executed!"),
            context
        );
        
        System.out.println("\nAll social dynamics applied successfully!");
    }
    
    // Helper class for demo
    static class ContentItem {
        String name;
        String category;
        long viewCount;
        
        ContentItem(String name, long viewCount) {
            this.name = name;
            this.viewCount = viewCount;
            this.category = "general";
        }
        
        ContentItem(String name, long viewCount, String category) {
            this.name = name;
            this.viewCount = viewCount;
            this.category = category;
        }
        
        public long getViewCount() {
            return viewCount;
        }
        
        public String getCategory() {
            return category;
        }
    }
}
