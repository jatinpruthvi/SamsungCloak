package com.cognitive.testing.environmental;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Device Interruption Logic Hook
 * Simulates system events (incoming calls, system updates, alarms) that force
 * the app into the background during critical tasks.
 * 
 * Real-world usage patterns this simulates:
 * - Incoming VOIP calls (WhatsApp, Duo, Phone calls)
 * - System notifications and alerts
 * - OS updates downloading/installing
 * - Battery warnings and system dialogs
 * - Screen wake and lock events
 */
public class DeviceInterruptionHook {
    
    private final EnvironmentalConfig config;
    private final Random random;
    
    private volatile long lastInterruptionTime;
    private volatile InterruptionType lastInterruptionType;
    private volatile boolean isInterrupting;
    
    private final AtomicInteger totalInterruptions;
    private final AtomicInteger interruptionCountByType;
    private final AtomicReference<InterruptionEvent> currentInterruption;
    
    private Consumer<InterruptionEvent> interruptionCallback;
    
    public DeviceInterruptionHook(EnvironmentalConfig config) {
        this(config, new Random());
    }
    
    public DeviceInterruptionHook(EnvironmentalConfig config, Random random) {
        this.config = config;
        this.random = random;
        this.lastInterruptionTime = System.currentTimeMillis();
        this.lastInterruptionType = InterruptionType.NONE;
        this.isInterrupting = false;
        this.totalInterruptions = new AtomicInteger(0);
        this.interruptionCountByType = new AtomicInteger(0);
        this.currentInterruption = new AtomicReference<>();
    }
    
    /**
     * Set callback to execute when interruption occurs
     */
    public void setInterruptionCallback(Consumer<InterruptionEvent> callback) {
        this.interruptionCallback = callback;
    }
    
    /**
     * Check if an interruption should occur
     */
    public boolean shouldInterrupt() {
        if (!config.isEnableDeviceInterruptions()) {
            return false;
        }
        
        if (isInterrupting) {
            return false;
        }
        
        long timeSinceLastInterruption = System.currentTimeMillis() - lastInterruptionTime;
        long minInterval = config.getMinInterruptionIntervalMs();
        
        if (timeSinceLastInterruption < minInterval) {
            return false;
        }
        
        return random.nextFloat() < config.getInterruptionProbability();
    }
    
    /**
     * Execute interruption sequence
     */
    public void executeInterruption() {
        InterruptionType type = selectInterruptionType();
        InterruptionSeverity severity = determineSeverity(type);
        
        InterruptionEvent event = new InterruptionEvent(
            type,
            severity,
            calculateDuration(type, severity),
            System.currentTimeMillis()
        );
        
        isInterrupting = true;
        lastInterruptionTime = System.currentTimeMillis();
        lastInterruptionType = type;
        currentInterruption.set(event);
        totalInterruptions.incrementAndGet();
        interruptionCountByType.incrementAndGet();
        
        if (interruptionCallback != null) {
            try {
                interruptionCallback.accept(event);
            } catch (Exception e) {
            }
        }
        
        performInterruptionSimulation(event);
        
        isInterrupting = false;
        currentInterruption.set(null);
    }
    
    private InterruptionType selectInterruptionType() {
        float roll = random.nextFloat();
        
        if (roll < 0.35f) {
            return InterruptionType.INCOMING_CALL;
        } else if (roll < 0.55f) {
            return InterruptionType.VOIP_CALL;
        } else if (roll < 0.70f) {
            return InterruptionType.SYSTEM_NOTIFICATION;
        } else if (roll < 0.80f) {
            return InterruptionType.ALARM;
        } else if (roll < 0.88f) {
            return InterruptionType.BATTERY_WARNING;
        } else if (roll < 0.94f) {
            return InterruptionType.OS_UPDATE;
        } else if (roll < 0.97f) {
            return InterruptionType.SCREEN_LOCK;
        } else {
            return InterruptionType.FORCE_CLOSE_DIALOG;
        }
    }
    
    private InterruptionSeverity determineSeverity(InterruptionType type) {
        switch (type) {
            case INCOMING_CALL:
            case VOIP_CALL:
                return random.nextFloat() < 0.7f ? 
                    InterruptionSeverity.HIGH : InterruptionSeverity.MEDIUM;
            case SYSTEM_NOTIFICATION:
            case ALARM:
                return InterruptionSeverity.LOW;
            case BATTERY_WARNING:
            case OS_UPDATE:
                return InterruptionSeverity.MEDIUM;
            case SCREEN_LOCK:
                return InterruptionSeverity.LOW;
            case FORCE_CLOSE_DIALOG:
                return InterruptionSeverity.HIGH;
            default:
                return InterruptionSeverity.MEDIUM;
        }
    }
    
    private int calculateDuration(InterruptionType type, InterruptionSeverity severity) {
        int baseDuration = config.getInterruptionDurationMs();
        
        switch (type) {
            case INCOMING_CALL:
                baseDuration = 5000 + random.nextInt(15000);
                break;
            case VOIP_CALL:
                baseDuration = 8000 + random.nextInt(20000);
                break;
            case SYSTEM_NOTIFICATION:
                baseDuration = 2000 + random.nextInt(5000);
                break;
            case ALARM:
                baseDuration = 3000 + random.nextInt(7000);
                break;
            case BATTERY_WARNING:
                baseDuration = 3000 + random.nextInt(5000);
                break;
            case OS_UPDATE:
                baseDuration = 10000 + random.nextInt(30000);
                break;
            case SCREEN_LOCK:
                baseDuration = 2000 + random.nextInt(8000);
                break;
            case FORCE_CLOSE_DIALOG:
                baseDuration = 1000 + random.nextInt(3000);
                break;
        }
        
        switch (severity) {
            case HIGH:
                baseDuration = (int) (baseDuration * 1.5);
                break;
            case MEDIUM:
                break;
            case LOW:
                baseDuration = (int) (baseDuration * 0.7);
                break;
        }
        
        return baseDuration;
    }
    
    private void performInterruptionSimulation(InterruptionEvent event) {
        try {
            Thread.sleep(100);
            
            int duration = event.getDurationMs();
            
            for (int i = 0; i < duration; i += 100) {
                if (random.nextFloat() < 0.1f) {
                    Thread.sleep(50 + random.nextInt(100));
                }
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Check if currently in interruption state
     */
    public boolean isInterrupting() {
        return isInterrupting;
    }
    
    /**
     * Get current interruption event if active
     */
    public InterruptionEvent getCurrentInterruption() {
        return currentInterruption.get();
    }
    
    /**
     * Get last interruption type
     */
    public InterruptionType getLastInterruptionType() {
        return lastInterruptionType;
    }
    
    /**
     * Manually trigger interruption
     */
    public void triggerInterruption(InterruptionType type) {
        InterruptionEvent event = new InterruptionEvent(
            type,
            InterruptionSeverity.MEDIUM,
            calculateDuration(type, InterruptionSeverity.MEDIUM),
            System.currentTimeMillis()
        );
        
        isInterrupting = true;
        lastInterruptionTime = System.currentTimeMillis();
        lastInterruptionType = type;
        currentInterruption.set(event);
        totalInterruptions.incrementAndGet();
        
        performInterruptionSimulation(event);
        
        isInterrupting = false;
        currentInterruption.set(null);
    }
    
    /**
     * Get time since last interruption
     */
    public long getTimeSinceLastInterruptionMs() {
        return System.currentTimeMillis() - lastInterruptionTime;
    }
    
    /**
     * Get statistics
     */
    public InterruptionStatistics getStatistics() {
        return new InterruptionStatistics(
            totalInterruptions.get(),
            interruptionCountByType.get(),
            lastInterruptionType,
            getTimeSinceLastInterruptionMs()
        );
    }
    
    /**
     * Reset statistics
     */
    public void resetStatistics() {
        totalInterruptions.set(0);
        interruptionCountByType.set(0);
        lastInterruptionTime = System.currentTimeMillis();
        lastInterruptionType = InterruptionType.NONE;
    }
    
    public enum InterruptionType {
        NONE("None"),
        INCOMING_CALL("Incoming Call"),
        VOIP_CALL("VOIP Call"),
        SYSTEM_NOTIFICATION("System Notification"),
        ALARM("Alarm"),
        BATTERY_WARNING("Battery Warning"),
        OS_UPDATE("OS Update"),
        SCREEN_LOCK("Screen Lock"),
        FORCE_CLOSE_DIALOG("Force Close Dialog");
        
        private final String displayName;
        
        InterruptionType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public enum InterruptionSeverity {
        LOW("Low", 1),
        MEDIUM("Medium", 2),
        HIGH("High", 3);
        
        private final String displayName;
        private final int level;
        
        InterruptionSeverity(String displayName, int level) {
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
    
    public static class InterruptionEvent {
        private final InterruptionType type;
        private final InterruptionSeverity severity;
        private final int durationMs;
        private final long timestamp;
        
        public InterruptionEvent(InterruptionType type, InterruptionSeverity severity, 
                                int durationMs, long timestamp) {
            this.type = type;
            this.severity = severity;
            this.durationMs = durationMs;
            this.timestamp = timestamp;
        }
        
        public InterruptionType getType() {
            return type;
        }
        
        public InterruptionSeverity getSeverity() {
            return severity;
        }
        
        public int getDurationMs() {
            return durationMs;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        @Override
        public String toString() {
            return String.format("InterruptionEvent{type=%s, severity=%s, duration=%dms}",
                type.getDisplayName(), severity.getDisplayName(), durationMs);
        }
    }
    
    public static class InterruptionStatistics {
        private final int totalInterruptions;
        private final int typeSpecificInterruptions;
        private final InterruptionType lastType;
        private final long timeSinceLastInterruptionMs;
        
        public InterruptionStatistics(int totalInterruptions, int typeSpecificInterruptions,
                                     InterruptionType lastType, long timeSinceLastInterruptionMs) {
            this.totalInterruptions = totalInterruptions;
            this.typeSpecificInterruptions = typeSpecificInterruptions;
            this.lastType = lastType;
            this.timeSinceLastInterruptionMs = timeSinceLastInterruptionMs;
        }
        
        public int getTotalInterruptions() {
            return totalInterruptions;
        }
        
        public int getTypeSpecificInterruptions() {
            return typeSpecificInterruptions;
        }
        
        public InterruptionType getLastType() {
            return lastType;
        }
        
        public long getTimeSinceLastInterruptionMs() {
            return timeSinceLastInterruptionMs;
        }
        
        @Override
        public String toString() {
            return String.format(
                "InterruptionStatistics{total=%d, lastType=%s, timeSinceLast=%dms}",
                totalInterruptions, lastType.getDisplayName(), timeSinceLastInterruptionMs
            );
        }
    }
}
