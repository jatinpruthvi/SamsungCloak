package com.cognitive.testing.environmental;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Context Switching Entropy Hook
 * Simulates "App Hopping" behavior where the agent periodically switches between
 * the target app and browser/social apps to simulate messy real-world multitasking.
 * 
 * Real-world usage patterns this simulates:
 * - Checking social media notifications while using an app
 * - Switching to browser to look up information
 * - Multitasking between multiple apps
 * - Task switching behavior based on cognitive load
 * - Entropic app selection patterns
 */
public class ContextSwitchingHook {
    
    private final EnvironmentalConfig config;
    private final Random random;
    
    private volatile long lastSwitchTime;
    private volatile String currentApp;
    private volatile String previousApp;
    private volatile boolean isSwitching;
    private volatile List<String> switchHistory;
    
    private final AtomicInteger totalSwitches;
    private final AtomicInteger switchesByApp[];
    private final AtomicInteger returnToTargetCount;
    private final AtomicInteger abandonmentCount;
    private final AtomicLong totalSwitchTimeMs;
    private final AtomicLong timeInTargetAppMs;
    private final AtomicLong timeInOtherAppsMs;
    
    private final AtomicReference<ContextSwitchEvent> currentSwitch;
    private final List<String> availableApps;
    
    private final String targetApp;
    
    public ContextSwitchingHook(EnvironmentalConfig config, String targetApp) {
        this(config, targetApp, new Random());
    }
    
    public ContextSwitchingHook(EnvironmentalConfig config, String targetApp, Random random) {
        this.config = config;
        this.targetApp = targetApp;
        this.random = random;
        this.currentApp = targetApp;
        this.previousApp = null;
        this.isSwitching = false;
        this.switchHistory = new ArrayList<>();
        this.lastSwitchTime = System.currentTimeMillis();
        this.totalSwitches = new AtomicInteger(0);
        
        String[] appNames = {
            "target", "chrome", "firefox", "instagram", "facebook", 
            "twitter", "whatsapp", "telegram", "tiktok", "youtube"
        };
        this.switchesByApp = new AtomicInteger[appNames.length];
        for (int i = 0; i < switchesByApp.length; i++) {
            switchesByApp[i] = new AtomicInteger(0);
        }
        
        this.returnToTargetCount = new AtomicInteger(0);
        this.abandonmentCount = new AtomicInteger(0);
        this.totalSwitchTimeMs = new AtomicLong(0);
        this.timeInTargetAppMs = new AtomicLong(0);
        this.timeInOtherAppsMs = new AtomicLong(0);
        this.currentSwitch = new AtomicReference<>();
        
        this.availableApps = new ArrayList<>();
        this.availableApps.add(targetApp);
        this.availableApps.add("com.android.chrome");
        this.availableApps.add("org.mozilla.firefox");
        this.availableApps.add("com.instagram.android");
        this.availableApps.add("com.facebook.katana");
        this.availableApps.add("com.twitter.android");
        this.availableApps.add("com.whatsapp");
        this.availableApps.add("org.telegram.messenger");
        this.availableApps.add("com.zhiliaoapp.musically");
        this.availableApps.add("com.google.android.youtube");
        
        switchHistory.add(targetApp);
    }
    
    /**
     * Check if context should switch
     */
    public boolean shouldSwitchContext() {
        if (!config.isEnableContextSwitching()) {
            return false;
        }
        
        if (isSwitching) {
            return false;
        }
        
        long timeSinceLastSwitch = System.currentTimeMillis() - lastSwitchTime;
        long minInterval = config.getMinContextSwitchIntervalMs();
        
        if (timeSinceLastSwitch < minInterval) {
            return false;
        }
        
        return random.nextFloat() < config.getContextSwitchProbability();
    }
    
    /**
     * Execute context switch
     */
    public void executeContextSwitch() {
        String nextApp = selectNextApp();
        int switchDuration = calculateSwitchDuration();
        SwitchReason reason = determineSwitchReason(nextApp);
        
        ContextSwitchEvent event = new ContextSwitchEvent(
            currentApp,
            nextApp,
            reason,
            switchDuration,
            System.currentTimeMillis()
        );
        
        isSwitching = true;
        previousApp = currentApp;
        lastSwitchTime = System.currentTimeMillis();
        currentSwitch.set(event);
        totalSwitches.incrementAndGet();
        
        int appIndex = availableApps.indexOf(nextApp);
        if (appIndex >= 0 && appIndex < switchesByApp.length) {
            switchesByApp[appIndex].incrementAndGet();
        }
        
        switchHistory.add(nextApp);
        if (switchHistory.size() > 50) {
            switchHistory.remove(0);
        }
        
        performContextSwitch(event);
        
        long timeInApp = System.currentTimeMillis() - lastSwitchTime;
        if ("target".equals(nextApp) || targetApp.equals(nextApp)) {
            timeInTargetAppMs.addAndGet(timeInApp);
        } else {
            timeInOtherAppsMs.addAndGet(timeInApp);
        }
        
        isSwitching = false;
        currentApp = nextApp;
        currentSwitch.set(null);
    }
    
    private String selectNextApp() {
        float entropy = config.getAppHoppingEntropy();
        
        if (random.nextFloat() < entropy) {
            int randomIndex = random.nextInt(availableApps.size());
            return availableApps.get(randomIndex);
        }
        
        if (switchHistory.size() >= 2) {
            String lastApp = switchHistory.get(switchHistory.size() - 1);
            String secondLastApp = switchHistory.size() >= 3 ? 
                switchHistory.get(switchHistory.size() - 2) : null;
            
            if (random.nextFloat() < 0.4f && secondLastApp != null && !secondLastApp.equals(lastApp)) {
                return secondLastApp;
            }
            
            if (random.nextFloat() < 0.3f && !targetApp.equals(lastApp)) {
                return targetApp;
            }
        }
        
        List<String> weightedApps = new ArrayList<>(availableApps);
        if ("target".equals(currentApp) || targetApp.equals(currentApp)) {
            weightedApps.add("com.android.chrome");
            weightedApps.add("com.instagram.android");
            weightedApps.add("com.facebook.katana");
        }
        
        int index = random.nextInt(weightedApps.size());
        return weightedApps.get(index);
    }
    
    private int calculateSwitchDuration() {
        int minDuration = config.getMinContextSwitchDurationMs();
        int maxDuration = config.getMaxContextSwitchDurationMs();
        
        int duration = minDuration + random.nextInt(maxDuration - minDuration);
        
        float entropy = config.getAppHoppingEntropy();
        if (random.nextFloat() < entropy) {
            duration = (int) (duration * (0.8 + random.nextFloat() * 0.4));
        }
        
        if (random.nextFloat() < 0.1f) {
            duration += random.nextInt(10000);
        }
        
        return duration;
    }
    
    private SwitchReason determineSwitchReason(String nextApp) {
        if (targetApp.equals(nextApp)) {
            return SwitchReason.RETURN_TO_TARGET;
        }
        
        if (nextApp.contains("instagram") || nextApp.contains("facebook") || 
            nextApp.contains("twitter")) {
            return SwitchReason.SOCIAL_MEDIA_CHECK;
        }
        
        if (nextApp.contains("chrome") || nextApp.contains("firefox")) {
            return SwitchReason.BROWSER_LOOKUP;
        }
        
        if (nextApp.contains("whatsapp") || nextApp.contains("telegram")) {
            return SwitchReason.MESSAGING;
        }
        
        if (nextApp.contains("youtube") || nextApp.contains("tiktok")) {
            return SwitchReason.ENTERTAINMENT;
        }
        
        return SwitchReason.DISTRACTION;
    }
    
    private void performContextSwitch(ContextSwitchEvent event) {
        long startTime = System.currentTimeMillis();
        
        try {
            Thread.sleep(50);
            
            int duration = event.getDurationMs();
            
            for (int i = 0; i < duration; i += 100) {
                if (random.nextFloat() < 0.1f) {
                    Thread.sleep(50 + random.nextInt(150));
                }
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            long actualDuration = System.currentTimeMillis() - startTime;
            totalSwitchTimeMs.addAndGet(actualDuration);
            
            if (event.getToApp().equals(targetApp)) {
                returnToTargetCount.incrementAndGet();
            }
        }
    }
    
    /**
     * Check if should abandon current task
     */
    public boolean shouldAbandonTask() {
        if (!config.isEnableContextSwitching()) {
            return false;
        }
        
        float entropy = config.getAppHoppingEntropy();
        
        if (random.nextFloat() < entropy * 0.15f) {
            abandonmentCount.incrementAndGet();
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if currently in target app
     */
    public boolean isInTargetApp() {
        return targetApp.equals(currentApp) || "target".equals(currentApp);
    }
    
    /**
     * Check if currently switching
     */
    public boolean isSwitching() {
        return isSwitching;
    }
    
    /**
     * Get current app
     */
    public String getCurrentApp() {
        return currentApp;
    }
    
    /**
     * Get previous app
     */
    public String getPreviousApp() {
        return previousApp;
    }
    
    /**
     * Get current switch event
     */
    public ContextSwitchEvent getCurrentSwitch() {
        return currentSwitch.get();
    }
    
    /**
     * Manually switch to specific app
     */
    public void switchToApp(String app) {
        String normalizedApp = app;
        if ("target".equals(app)) {
            normalizedApp = targetApp;
        }
        
        ContextSwitchEvent event = new ContextSwitchEvent(
            currentApp,
            normalizedApp,
            SwitchReason.MANUAL,
            calculateSwitchDuration(),
            System.currentTimeMillis()
        );
        
        isSwitching = true;
        previousApp = currentApp;
        lastSwitchTime = System.currentTimeMillis();
        currentSwitch.set(event);
        totalSwitches.incrementAndGet();
        
        performContextSwitch(event);
        
        isSwitching = false;
        currentApp = normalizedApp;
        currentSwitch.set(null);
    }
    
    /**
     * Get time since last switch
     */
    public long getTimeSinceLastSwitchMs() {
        return System.currentTimeMillis() - lastSwitchTime;
    }
    
    /**
     * Get switch history
     */
    public List<String> getSwitchHistory() {
        return new ArrayList<>(switchHistory);
    }
    
    /**
     * Get switch count by app
     */
    public int getSwitchCountForApp(String app) {
        int index = availableApps.indexOf(app);
        if (index >= 0 && index < switchesByApp.length) {
            return switchesByApp[index].get();
        }
        return 0;
    }
    
    /**
     * Get switch entropy score (0.0 - 1.0, higher = more chaotic)
     */
    public float calculateSwitchEntropy() {
        if (totalSwitches.get() == 0) {
            return 0.0f;
        }
        
        int uniqueApps = 0;
        for (AtomicInteger count : switchesByApp) {
            if (count.get() > 0) {
                uniqueApps++;
            }
        }
        
        float entropy = (float) uniqueApps / availableApps.size();
        return Math.min(1.0f, entropy);
    }
    
    /**
     * Get statistics
     */
    public ContextSwitchStatistics getStatistics() {
        return new ContextSwitchStatistics(
            totalSwitches.get(),
            getTimeSinceLastSwitchMs(),
            isInTargetApp(),
            returnToTargetCount.get(),
            abandonmentCount.get(),
            totalSwitchTimeMs.get(),
            timeInTargetAppMs.get(),
            timeInOtherAppsMs.get(),
            calculateSwitchEntropy(),
            new ArrayList<>(switchHistory)
        );
    }
    
    /**
     * Reset statistics
     */
    public void resetStatistics() {
        totalSwitches.set(0);
        lastSwitchTime = System.currentTimeMillis();
        currentApp = targetApp;
        previousApp = null;
        switchHistory.clear();
        switchHistory.add(targetApp);
        for (int i = 0; i < switchesByApp.length; i++) {
            switchesByApp[i].set(0);
        }
        returnToTargetCount.set(0);
        abandonmentCount.set(0);
        totalSwitchTimeMs.set(0);
        timeInTargetAppMs.set(0);
        timeInOtherAppsMs.set(0);
    }
    
    public enum SwitchReason {
        RETURN_TO_TARGET("Return to Target"),
        SOCIAL_MEDIA_CHECK("Social Media Check"),
        BROWSER_LOOKUP("Browser Lookup"),
        MESSAGING("Messaging"),
        ENTERTAINMENT("Entertainment"),
        DISTRACTION("Distraction"),
        MANUAL("Manual Switch");
        
        private final String displayName;
        
        SwitchReason(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public static class ContextSwitchEvent {
        private final String fromApp;
        private final String toApp;
        private final SwitchReason reason;
        private final int durationMs;
        private final long timestamp;
        
        public ContextSwitchEvent(String fromApp, String toApp, SwitchReason reason,
                                 int durationMs, long timestamp) {
            this.fromApp = fromApp;
            this.toApp = toApp;
            this.reason = reason;
            this.durationMs = durationMs;
            this.timestamp = timestamp;
        }
        
        public String getFromApp() {
            return fromApp;
        }
        
        public String getToApp() {
            return toApp;
        }
        
        public SwitchReason getReason() {
            return reason;
        }
        
        public int getDurationMs() {
            return durationMs;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        @Override
        public String toString() {
            return String.format("ContextSwitchEvent{from=%s, to=%s, reason=%s, duration=%dms}",
                fromApp, toApp, reason.getDisplayName(), durationMs);
        }
    }
    
    public static class ContextSwitchStatistics {
        private final int totalSwitches;
        private final long timeSinceLastSwitchMs;
        private final boolean isInTargetApp;
        private final int returnToTargetCount;
        private final int abandonmentCount;
        private final long totalSwitchTimeMs;
        private final long timeInTargetAppMs;
        private final long timeInOtherAppsMs;
        private final float switchEntropy;
        private final List<String> switchHistory;
        
        public ContextSwitchStatistics(int totalSwitches, long timeSinceLastSwitchMs,
                                      boolean isInTargetApp, int returnToTargetCount,
                                      int abandonmentCount, long totalSwitchTimeMs,
                                      long timeInTargetAppMs, long timeInOtherAppsMs,
                                      float switchEntropy, List<String> switchHistory) {
            this.totalSwitches = totalSwitches;
            this.timeSinceLastSwitchMs = timeSinceLastSwitchMs;
            this.isInTargetApp = isInTargetApp;
            this.returnToTargetCount = returnToTargetCount;
            this.abandonmentCount = abandonmentCount;
            this.totalSwitchTimeMs = totalSwitchTimeMs;
            this.timeInTargetAppMs = timeInTargetAppMs;
            this.timeInOtherAppsMs = timeInOtherAppsMs;
            this.switchEntropy = switchEntropy;
            this.switchHistory = switchHistory;
        }
        
        public int getTotalSwitches() {
            return totalSwitches;
        }
        
        public long getTimeSinceLastSwitchMs() {
            return timeSinceLastSwitchMs;
        }
        
        public boolean isInTargetApp() {
            return isInTargetApp;
        }
        
        public int getReturnToTargetCount() {
            return returnToTargetCount;
        }
        
        public int getAbandonmentCount() {
            return abandonmentCount;
        }
        
        public long getTotalSwitchTimeMs() {
            return totalSwitchTimeMs;
        }
        
        public long getTimeInTargetAppMs() {
            return timeInTargetAppMs;
        }
        
        public long getTimeInOtherAppsMs() {
            return timeInOtherAppsMs;
        }
        
        public float getSwitchEntropy() {
            return switchEntropy;
        }
        
        public List<String> getSwitchHistory() {
            return switchHistory;
        }
        
        @Override
        public String toString() {
            return String.format(
                "ContextSwitchStatistics{switches=%d, inTarget=%b, returns=%d, abandons=%d, entropy=%.2f, timeTarget=%dms, timeOther=%dms}",
                totalSwitches, isInTargetApp, returnToTargetCount, abandonmentCount,
                switchEntropy, timeInTargetAppMs, timeInOtherAppsMs
            );
        }
    }
}
