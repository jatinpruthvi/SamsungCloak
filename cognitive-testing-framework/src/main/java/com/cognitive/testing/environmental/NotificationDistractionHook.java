package com.cognitive.testing.environmental;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Notification Distraction Hook
 * Simulates "Push Notification Hijacking" where the script randomly diverts focus
 * to a different app to test session state recovery.
 * 
 * Real-world usage patterns this simulates:
 * - Social media notifications (Facebook, Instagram, TikTok)
 * - Messaging notifications (WhatsApp, Telegram)
 * - Email notifications
 * - App update notifications
 * - System alerts and reminders
 */
public class NotificationDistractionHook {
    
    private final EnvironmentalConfig config;
    private final Random random;
    
    private volatile long lastNotificationTime;
    private volatile NotificationType lastNotificationType;
    private volatile boolean isDistracted;
    private volatile String distractionApp;
    
    private final AtomicInteger totalNotifications;
    private final AtomicInteger notificationsByType[];
    private final AtomicInteger successfulFocusRecoveries;
    private final AtomicInteger focusLossCount;
    private final AtomicLong totalDistractionTimeMs;
    
    private final AtomicReference<NotificationEvent> currentNotification;
    
    private Consumer<NotificationEvent> notificationCallback;
    
    public NotificationDistractionHook(EnvironmentalConfig config) {
        this(config, new Random());
    }
    
    public NotificationDistractionHook(EnvironmentalConfig config, Random random) {
        this.config = config;
        this.random = random;
        this.lastNotificationTime = System.currentTimeMillis();
        this.lastNotificationType = NotificationType.NONE;
        this.isDistracted = false;
        this.distractionApp = null;
        this.totalNotifications = new AtomicInteger(0);
        this.notificationsByType = new AtomicInteger[NotificationType.values().length];
        for (int i = 0; i < notificationsByType.length; i++) {
            notificationsByType[i] = new AtomicInteger(0);
        }
        this.successfulFocusRecoveries = new AtomicInteger(0);
        this.focusLossCount = new AtomicInteger(0);
        this.totalDistractionTimeMs = new AtomicLong(0);
        this.currentNotification = new AtomicReference<>();
    }
    
    /**
     * Set callback to execute when notification occurs
     */
    public void setNotificationCallback(Consumer<NotificationEvent> callback) {
        this.notificationCallback = callback;
    }
    
    /**
     * Check if a notification should appear
     */
    public boolean shouldShowNotification() {
        if (!config.isEnableNotificationDistractions()) {
            return false;
        }
        
        if (isDistracted) {
            return false;
        }
        
        long timeSinceLastNotification = System.currentTimeMillis() - lastNotificationTime;
        long minInterval = config.getMinNotificationIntervalMs();
        
        if (timeSinceLastNotification < minInterval) {
            return false;
        }
        
        return random.nextFloat() < config.getNotificationProbability();
    }
    
    /**
     * Execute notification distraction sequence
     */
    public void executeNotificationDistraction() {
        NotificationType type = selectNotificationType();
        NotificationPriority priority = determinePriority(type);
        String app = selectDistractionApp(type);
        
        NotificationEvent event = new NotificationEvent(
            type,
            priority,
            app,
            calculateFocusLossDuration(type, priority),
            System.currentTimeMillis()
        );
        
        isDistracted = true;
        lastNotificationTime = System.currentTimeMillis();
        lastNotificationType = type;
        distractionApp = app;
        currentNotification.set(event);
        totalNotifications.incrementAndGet();
        notificationsByType[type.ordinal()].incrementAndGet();
        focusLossCount.incrementAndGet();
        
        if (notificationCallback != null) {
            try {
                notificationCallback.accept(event);
            } catch (Exception e) {
            }
        }
        
        performDistractionSimulation(event);
        
        boolean recovered = attemptFocusRecovery(event);
        if (recovered) {
            successfulFocusRecoveries.incrementAndGet();
        }
        
        isDistracted = false;
        distractionApp = null;
        currentNotification.set(null);
    }
    
    private NotificationType selectNotificationType() {
        float roll = random.nextFloat();
        
        if (roll < 0.25f) {
            return NotificationType.SOCIAL_MEDIA;
        } else if (roll < 0.45f) {
            return NotificationType.MESSAGING;
        } else if (roll < 0.60f) {
            return NotificationType.EMAIL;
        } else if (roll < 0.72f) {
            return NotificationType.PUSH_ALERT;
        } else if (roll < 0.82f) {
            return NotificationType.APP_UPDATE;
        } else if (roll < 0.90f) {
            return NotificationType.SYSTEM_ALERT;
        } else if (roll < 0.96f) {
            return NotificationType.CALENDAR_REMINDER;
        } else {
            return NotificationType.PROMOTIONAL;
        }
    }
    
    private NotificationPriority determinePriority(NotificationType type) {
        switch (type) {
            case MESSAGING:
                return random.nextFloat() < 0.6f ? 
                    NotificationPriority.HIGH : NotificationPriority.MEDIUM;
            case SOCIAL_MEDIA:
                return random.nextFloat() < 0.4f ? 
                    NotificationPriority.HIGH : NotificationPriority.MEDIUM;
            case EMAIL:
                return NotificationPriority.MEDIUM;
            case PUSH_ALERT:
                return random.nextFloat() < 0.7f ? 
                    NotificationPriority.HIGH : NotificationPriority.MEDIUM;
            case SYSTEM_ALERT:
                return NotificationPriority.HIGH;
            case CALENDAR_REMINDER:
                return NotificationPriority.HIGH;
            case APP_UPDATE:
                return NotificationPriority.LOW;
            case PROMOTIONAL:
                return NotificationPriority.LOW;
            default:
                return NotificationPriority.MEDIUM;
        }
    }
    
    private String selectDistractionApp(NotificationType type) {
        switch (type) {
            case SOCIAL_MEDIA:
                String[] socialApps = {"com.facebook.katana", "com.instagram.android", 
                                       "com.zhiliaoapp.musically", "com.twitter.android"};
                return socialApps[random.nextInt(socialApps.length)];
            case MESSAGING:
                String[] messagingApps = {"com.whatsapp", "org.telegram.messenger", 
                                        "com.discord", "com.slack"};
                return messagingApps[random.nextInt(messagingApps.length)];
            case EMAIL:
                String[] emailApps = {"com.google.android.gm", "com.microsoft.office.outlook"};
                return emailApps[random.nextInt(emailApps.length)];
            case APP_UPDATE:
                return "com.android.vending";
            case SYSTEM_ALERT:
                return "android";
            case CALENDAR_REMINDER:
                return "com.google.android.calendar";
            default:
                return "com.example.distraction";
        }
    }
    
    private int calculateFocusLossDuration(NotificationType type, NotificationPriority priority) {
        int baseDuration = config.getNotificationFocusLossDurationMs();
        
        switch (type) {
            case MESSAGING:
                baseDuration = 5000 + random.nextInt(20000);
                break;
            case SOCIAL_MEDIA:
                baseDuration = 8000 + random.nextInt(30000);
                break;
            case EMAIL:
                baseDuration = 3000 + random.nextInt(10000);
                break;
            case PUSH_ALERT:
                baseDuration = 2000 + random.nextInt(8000);
                break;
            case SYSTEM_ALERT:
                baseDuration = 1000 + random.nextInt(3000);
                break;
            case CALENDAR_REMINDER:
                baseDuration = 2000 + random.nextInt(5000);
                break;
            case APP_UPDATE:
                baseDuration = 1000 + random.nextInt(4000);
                break;
            case PROMOTIONAL:
                baseDuration = 500 + random.nextInt(2000);
                break;
        }
        
        switch (priority) {
            case HIGH:
                baseDuration = (int) (baseDuration * 0.8);
                break;
            case MEDIUM:
                break;
            case LOW:
                baseDuration = (int) (baseDuration * 1.3);
                break;
        }
        
        if (random.nextFloat() < 0.15f) {
            baseDuration += random.nextInt(10000);
        }
        
        return baseDuration;
    }
    
    private void performDistractionSimulation(NotificationEvent event) {
        long startTime = System.currentTimeMillis();
        
        try {
            Thread.sleep(100);
            
            int duration = event.getFocusLossDurationMs();
            
            for (int i = 0; i < duration; i += 100) {
                if (random.nextFloat() < 0.08f) {
                    Thread.sleep(50 + random.nextInt(200));
                }
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            long actualDuration = System.currentTimeMillis() - startTime;
            totalDistractionTimeMs.addAndGet(actualDuration);
        }
    }
    
    private boolean attemptFocusRecovery(NotificationEvent event) {
        float recoveryChance;
        
        switch (event.getPriority()) {
            case HIGH:
                recoveryChance = 0.85f;
                break;
            case MEDIUM:
                recoveryChance = 0.70f;
                break;
            case LOW:
                recoveryChance = 0.55f;
                break;
            default:
                recoveryChance = 0.70f;
        }
        
        if (random.nextFloat() < 0.1f) {
            recoveryChance *= 0.8f;
        }
        
        return random.nextFloat() < recoveryChance;
    }
    
    /**
     * Check if currently distracted
     */
    public boolean isDistracted() {
        return isDistracted;
    }
    
    /**
     * Get current distraction app
     */
    public String getDistractionApp() {
        return distractionApp;
    }
    
    /**
     * Get current notification event
     */
    public NotificationEvent getCurrentNotification() {
        return currentNotification.get();
    }
    
    /**
     * Manually trigger notification distraction
     */
    public void triggerDistraction(NotificationType type, String app) {
        NotificationEvent event = new NotificationEvent(
            type,
            NotificationPriority.MEDIUM,
            app,
            calculateFocusLossDuration(type, NotificationPriority.MEDIUM),
            System.currentTimeMillis()
        );
        
        isDistracted = true;
        lastNotificationTime = System.currentTimeMillis();
        lastNotificationType = type;
        distractionApp = app;
        currentNotification.set(event);
        totalNotifications.incrementAndGet();
        
        performDistractionSimulation(event);
        
        isDistracted = false;
        distractionApp = null;
        currentNotification.set(null);
    }
    
    /**
     * Get time since last notification
     */
    public long getTimeSinceLastNotificationMs() {
        return System.currentTimeMillis() - lastNotificationTime;
    }
    
    /**
     * Get notification count by type
     */
    public int getNotificationCount(NotificationType type) {
        return notificationsByType[type.ordinal()].get();
    }
    
    /**
     * Get focus recovery rate
     */
    public float getFocusRecoveryRate() {
        int total = focusLossCount.get();
        if (total == 0) {
            return 1.0f;
        }
        return (float) successfulFocusRecoveries.get() / total;
    }
    
    /**
     * Get statistics
     */
    public NotificationStatistics getStatistics() {
        return new NotificationStatistics(
            totalNotifications.get(),
            lastNotificationType,
            getTimeSinceLastNotificationMs(),
            focusLossCount.get(),
            successfulFocusRecoveries.get(),
            getFocusRecoveryRate(),
            totalDistractionTimeMs.get()
        );
    }
    
    /**
     * Reset statistics
     */
    public void resetStatistics() {
        totalNotifications.set(0);
        lastNotificationTime = System.currentTimeMillis();
        lastNotificationType = NotificationType.NONE;
        for (int i = 0; i < notificationsByType.length; i++) {
            notificationsByType[i].set(0);
        }
        successfulFocusRecoveries.set(0);
        focusLossCount.set(0);
        totalDistractionTimeMs.set(0);
    }
    
    public enum NotificationType {
        NONE("None"),
        SOCIAL_MEDIA("Social Media"),
        MESSAGING("Messaging"),
        EMAIL("Email"),
        PUSH_ALERT("Push Alert"),
        APP_UPDATE("App Update"),
        SYSTEM_ALERT("System Alert"),
        CALENDAR_REMINDER("Calendar Reminder"),
        PROMOTIONAL("Promotional");
        
        private final String displayName;
        
        NotificationType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public enum NotificationPriority {
        LOW("Low", 1),
        MEDIUM("Medium", 2),
        HIGH("High", 3);
        
        private final String displayName;
        private final int level;
        
        NotificationPriority(String displayName, int level) {
            this.displayName = displayName;
            this.level = level;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public int getLevel() {
            return level;
        }
    }
    
    public static class NotificationEvent {
        private final NotificationType type;
        private final NotificationPriority priority;
        private final String distractionApp;
        private final int focusLossDurationMs;
        private final long timestamp;
        
        public NotificationEvent(NotificationType type, NotificationPriority priority,
                               String distractionApp, int focusLossDurationMs, long timestamp) {
            this.type = type;
            this.priority = priority;
            this.distractionApp = distractionApp;
            this.focusLossDurationMs = focusLossDurationMs;
            this.timestamp = timestamp;
        }
        
        public NotificationType getType() {
            return type;
        }
        
        public NotificationPriority getPriority() {
            return priority;
        }
        
        public String getDistractionApp() {
            return distractionApp;
        }
        
        public int getFocusLossDurationMs() {
            return focusLossDurationMs;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        @Override
        public String toString() {
            return String.format("NotificationEvent{type=%s, priority=%s, app=%s, duration=%dms}",
                type.getDisplayName(), priority.getDisplayName(), distractionApp, focusLossDurationMs);
        }
    }
    
    public static class NotificationStatistics {
        private final int totalNotifications;
        private final NotificationType lastType;
        private final long timeSinceLastNotificationMs;
        private final int focusLossCount;
        private final int successfulFocusRecoveries;
        private final float focusRecoveryRate;
        private final long totalDistractionTimeMs;
        
        public NotificationStatistics(int totalNotifications, NotificationType lastType,
                                    long timeSinceLastNotificationMs, int focusLossCount,
                                    int successfulFocusRecoveries, float focusRecoveryRate,
                                    long totalDistractionTimeMs) {
            this.totalNotifications = totalNotifications;
            this.lastType = lastType;
            this.timeSinceLastNotificationMs = timeSinceLastNotificationMs;
            this.focusLossCount = focusLossCount;
            this.successfulFocusRecoveries = successfulFocusRecoveries;
            this.focusRecoveryRate = focusRecoveryRate;
            this.totalDistractionTimeMs = totalDistractionTimeMs;
        }
        
        public int getTotalNotifications() {
            return totalNotifications;
        }
        
        public NotificationType getLastType() {
            return lastType;
        }
        
        public long getTimeSinceLastNotificationMs() {
            return timeSinceLastNotificationMs;
        }
        
        public int getFocusLossCount() {
            return focusLossCount;
        }
        
        public int getSuccessfulFocusRecoveries() {
            return successfulFocusRecoveries;
        }
        
        public float getFocusRecoveryRate() {
            return focusRecoveryRate;
        }
        
        public long getTotalDistractionTimeMs() {
            return totalDistractionTimeMs;
        }
        
        @Override
        public String toString() {
            return String.format(
                "NotificationStatistics{total=%d, lastType=%s, focusLoss=%d, recoveryRate=%.2f%%, totalTime=%dms}",
                totalNotifications, lastType.getDisplayName(), focusLossCount, 
                focusRecoveryRate * 100, totalDistractionTimeMs
            );
        }
    }
}
