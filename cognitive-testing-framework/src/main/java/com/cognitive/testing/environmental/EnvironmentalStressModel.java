package com.cognitive.testing.environmental;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Environmental Stress Model Orchestrator
 * Coordinates all 5 Environmental Realism Hooks to simulate high-fidelity
 * real-world conditions on Samsung Galaxy A12 (SM-A125U).
 * 
 * This model ensures "Real-world usage is messy" by simultaneously applying
 * network instability, device interruptions, battery constraints, notification
 * distractions, and context switching entropy during automated testing.
 */
public class EnvironmentalStressModel {
    
    private final EnvironmentalConfig config;
    private final String targetApp;
    
    private final NetworkInstabilityHook networkHook;
    private final DeviceInterruptionHook interruptionHook;
    private final BatteryConstraintHook batteryHook;
    private final NotificationDistractionHook notificationHook;
    private final ContextSwitchingHook contextHook;
    
    private final AtomicBoolean isActive;
    private final AtomicReference<EnvironmentalState> currentState;
    private volatile long sessionStartTime;
    
    private Consumer<EnvironmentalEvent> eventCallback;
    
    public EnvironmentalStressModel(EnvironmentalConfig config, String targetApp) {
        this.config = config;
        this.targetApp = targetApp;
        this.networkHook = new NetworkInstabilityHook(config);
        this.interruptionHook = new DeviceInterruptionHook(config);
        this.batteryHook = new BatteryConstraintHook(config);
        this.notificationHook = new NotificationDistractionHook(config);
        this.contextHook = new ContextSwitchingHook(config, targetApp);
        this.isActive = new AtomicBoolean(false);
        this.currentState = new AtomicReference<>(EnvironmentalState.NORMAL);
        this.sessionStartTime = System.currentTimeMillis();
        
        setupCallbacks();
    }
    
    private void setupCallbacks() {
        interruptionHook.setInterruptionCallback(event -> {
            if (eventCallback != null) {
                eventCallback.accept(new EnvironmentalEvent(
                    EnvironmentalEventType.INTERRUPTION,
                    event.toString(),
                    System.currentTimeMillis()
                ));
            }
        });
        
        notificationHook.setNotificationCallback(event -> {
            if (eventCallback != null) {
                eventCallback.accept(new EnvironmentalEvent(
                    EnvironmentalEventType.NOTIFICATION_DISTRACTION,
                    event.toString(),
                    System.currentTimeMillis()
                ));
            }
        });
    }
    
    /**
     * Set callback for environmental events
     */
    public void setEventCallback(Consumer<EnvironmentalEvent> callback) {
        this.eventCallback = callback;
    }
    
    /**
     * Start the environmental stress model
     */
    public void start() {
        if (isActive.compareAndSet(false, true)) {
            sessionStartTime = System.currentTimeMillis();
            currentState.set(EnvironmentalState.NORMAL);
        }
    }
    
    /**
     * Stop the environmental stress model
     */
    public void stop() {
        isActive.set(false);
        currentState.set(EnvironmentalState.STOPPED);
    }
    
    /**
     * Check if model is active
     */
    public boolean isActive() {
        return isActive.get();
    }
    
    /**
     * Process a single interaction with all environmental hooks
     * Call this before each automated test action
     */
    public void processInteraction() {
        if (!isActive.get()) {
            return;
        }
        
        currentState.set(EnvironmentalState.PROCESSING);
        
        try {
            updateBatteryLevel();
            checkAndApplyNetworkInstability();
            checkAndApplyInterruptions();
            checkAndApplyNotificationDistractions();
            checkAndApplyContextSwitching();
            
            updateOverallState();
        } finally {
            currentState.set(EnvironmentalState.NORMAL);
        }
    }
    
    /**
     * Inject network latency before network operations
     */
    public void beforeNetworkOperation() {
        if (!isActive.get()) {
            return;
        }
        
        updateBatteryLevel();
        checkAndApplyNetworkInstability();
        
        if (networkHook.isNetworkDown()) {
            if (networkHook.attemptNetworkRecovery()) {
                if (eventCallback != null) {
                    eventCallback.accept(new EnvironmentalEvent(
                        EnvironmentalEventType.NETWORK_RECOVERY,
                        "Network recovered",
                        System.currentTimeMillis()
                    ));
                }
            }
        }
        
        networkHook.injectLatency();
    }
    
    /**
     * Check if network operation should fail
     */
    public boolean shouldNetworkOperationFail() {
        if (!isActive.get()) {
            return false;
        }
        
        return networkHook.shouldNetworkOperationFail();
    }
    
    private void updateBatteryLevel() {
        batteryHook.updateBattery();
        batteryHook.applyBatteryDelay();
    }
    
    private void checkAndApplyNetworkInstability() {
        if (networkHook.shouldChangeNetworkState()) {
            networkHook.changeNetworkState();
            
            if (eventCallback != null) {
                eventCallback.accept(new EnvironmentalEvent(
                    EnvironmentalEventType.NETWORK_CHANGE,
                    networkHook.getCurrentState().getDisplayName() + 
                    " (latency: " + networkHook.getEffectiveLatency() + "ms)",
                    System.currentTimeMillis()
                ));
            }
        }
    }
    
    private void checkAndApplyInterruptions() {
        if (interruptionHook.shouldInterrupt()) {
            interruptionHook.executeInterruption();
            
            if (eventCallback != null) {
                eventCallback.accept(new EnvironmentalEvent(
                    EnvironmentalEventType.DEVICE_INTERRUPTED,
                    interruptionHook.getLastInterruptionType().getDisplayName(),
                    System.currentTimeMillis()
                ));
            }
        }
    }
    
    private void checkAndApplyNotificationDistractions() {
        if (notificationHook.shouldShowNotification()) {
            notificationHook.executeNotificationDistraction();
            
            if (eventCallback != null) {
                eventCallback.accept(new EnvironmentalEvent(
                    EnvironmentalEventType.NOTIFICATION_DISTRACTION,
                    notificationHook.getCurrentNotification().toString(),
                    System.currentTimeMillis()
                ));
            }
        }
    }
    
    private void checkAndApplyContextSwitching() {
        if (contextHook.shouldSwitchContext()) {
            contextHook.executeContextSwitch();
            
            if (eventCallback != null) {
                eventCallback.accept(new EnvironmentalEvent(
                    EnvironmentalEventType.CONTEXT_SWITCH,
                    contextHook.getCurrentSwitch().toString(),
                    System.currentTimeMillis()
                ));
            }
        }
        
        if (contextHook.shouldAbandonTask()) {
            if (eventCallback != null) {
                eventCallback.accept(new EnvironmentalEvent(
                    EnvironmentalEventType.TASK_ABANDONMENT,
                    "Task abandoned due to context switch entropy",
                    System.currentTimeMillis()
                ));
            }
        }
    }
    
    private void updateOverallState() {
        if (!contextHook.isInTargetApp()) {
            currentState.set(EnvironmentalState.CONTEXT_SWITCHED);
        } else if (networkHook.isNetworkDown()) {
            currentState.set(EnvironmentalState.NETWORK_DOWN);
        } else if (interruptionHook.isInterrupting()) {
            currentState.set(EnvironmentalState.INTERRUPTED);
        } else if (notificationHook.isDistracted()) {
            currentState.set(EnvironmentalState.DISTRACTED);
        } else if (batteryHook.isInPowerSaveMode()) {
            currentState.set(EnvironmentalState.POWER_SAVE);
        } else {
            currentState.set(EnvironmentalState.NORMAL);
        }
    }
    
    /**
     * Apply power save mode modifier to delays
     */
    public long applyPowerSaveModifier(long baseDelayMs) {
        return batteryHook.applyPowerSaveModifier(baseDelayMs);
    }
    
    /**
     * Get comprehensive statistics
     */
    public EnvironmentalStatistics getStatistics() {
        return new EnvironmentalStatistics(
            getSessionDurationMs(),
            networkHook.getStatistics(),
            interruptionHook.getStatistics(),
            batteryHook.getStatistics(),
            notificationHook.getStatistics(),
            contextHook.getStatistics(),
            currentState.get()
        );
    }
    
    /**
     * Get session duration
     */
    public long getSessionDurationMs() {
        return System.currentTimeMillis() - sessionStartTime;
    }
    
    /**
     * Get current environmental state
     */
    public EnvironmentalState getCurrentState() {
        return currentState.get();
    }
    
    /**
     * Get network hook
     */
    public NetworkInstabilityHook getNetworkHook() {
        return networkHook;
    }
    
    /**
     * Get interruption hook
     */
    public DeviceInterruptionHook getInterruptionHook() {
        return interruptionHook;
    }
    
    /**
     * Get battery hook
     */
    public BatteryConstraintHook getBatteryHook() {
        return batteryHook;
    }
    
    /**
     * Get notification hook
     */
    public NotificationDistractionHook getNotificationHook() {
        return notificationHook;
    }
    
    /**
     * Get context hook
     */
    public ContextSwitchingHook getContextHook() {
        return contextHook;
    }
    
    /**
     * Reset all statistics
     */
    public void resetStatistics() {
        networkHook.resetStatistics();
        interruptionHook.resetStatistics();
        batteryHook.reset();
        notificationHook.resetStatistics();
        contextHook.resetStatistics();
        sessionStartTime = System.currentTimeMillis();
    }
    
    /**
     * Generate comprehensive report
     */
    public String generateReport() {
        EnvironmentalStatistics stats = getStatistics();
        
        StringBuilder report = new StringBuilder();
        report.append("=== ENVIRONMENTAL STRESS MODEL REPORT ===\n\n");
        report.append(String.format("Session Duration: %d minutes (%d ms)\n",
            stats.sessionDurationMs / 60000, stats.sessionDurationMs));
        report.append(String.format("Current State: %s\n\n", stats.currentState.name()));
        
        report.append("--- Network Instability ---\n");
        report.append(stats.networkStats.toString()).append("\n\n");
        
        report.append("--- Device Interruptions ---\n");
        report.append(stats.interruptionStats.toString()).append("\n\n");
        
        report.append("--- Battery Constraints ---\n");
        report.append(stats.batteryStats.toString()).append("\n\n");
        
        report.append("--- Notification Distractions ---\n");
        report.append(stats.notificationStats.toString()).append("\n\n");
        
        report.append("--- Context Switching ---\n");
        report.append(stats.contextStats.toString()).append("\n");
        
        report.append("======================================\n");
        
        return report.toString();
    }
    
    public enum EnvironmentalState {
        NORMAL,
        PROCESSING,
        NETWORK_DOWN,
        INTERRUPTED,
        DISTRACTED,
        CONTEXT_SWITCHED,
        POWER_SAVE,
        STOPPED
    }
    
    public enum EnvironmentalEventType {
        NETWORK_CHANGE,
        NETWORK_FAILURE,
        NETWORK_RECOVERY,
        DEVICE_INTERRUPTED,
        NOTIFICATION_DISTRACTION,
        CONTEXT_SWITCH,
        TASK_ABANDONMENT,
        BATTERY_WARNING,
        POWER_SAVE_ENTERED
    }
    
    public static class EnvironmentalEvent {
        private final EnvironmentalEventType type;
        private final String description;
        private final long timestamp;
        
        public EnvironmentalEvent(EnvironmentalEventType type, String description, long timestamp) {
            this.type = type;
            this.description = description;
            this.timestamp = timestamp;
        }
        
        public EnvironmentalEventType getType() {
            return type;
        }
        
        public String getDescription() {
            return description;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        @Override
        public String toString() {
            return String.format("[%s] %s", type.name(), description);
        }
    }
    
    public static class EnvironmentalStatistics {
        private final long sessionDurationMs;
        private final NetworkInstabilityHook.NetworkStatistics networkStats;
        private final DeviceInterruptionHook.InterruptionStatistics interruptionStats;
        private final BatteryConstraintHook.BatteryStatistics batteryStats;
        private final NotificationDistractionHook.NotificationStatistics notificationStats;
        private final ContextSwitchingHook.ContextSwitchStatistics contextStats;
        private final EnvironmentalState currentState;
        
        public EnvironmentalStatistics(long sessionDurationMs,
                                       NetworkInstabilityHook.NetworkStatistics networkStats,
                                       DeviceInterruptionHook.InterruptionStatistics interruptionStats,
                                       BatteryConstraintHook.BatteryStatistics batteryStats,
                                       NotificationDistractionHook.NotificationStatistics notificationStats,
                                       ContextSwitchingHook.ContextSwitchStatistics contextStats,
                                       EnvironmentalState currentState) {
            this.sessionDurationMs = sessionDurationMs;
            this.networkStats = networkStats;
            this.interruptionStats = interruptionStats;
            this.batteryStats = batteryStats;
            this.notificationStats = notificationStats;
            this.contextStats = contextStats;
            this.currentState = currentState;
        }
        
        public long getSessionDurationMs() {
            return sessionDurationMs;
        }
        
        public NetworkInstabilityHook.NetworkStatistics getNetworkStats() {
            return networkStats;
        }
        
        public DeviceInterruptionHook.InterruptionStatistics getInterruptionStats() {
            return interruptionStats;
        }
        
        public BatteryConstraintHook.BatteryStatistics getBatteryStats() {
            return batteryStats;
        }
        
        public NotificationDistractionHook.NotificationStatistics getNotificationStats() {
            return notificationStats;
        }
        
        public ContextSwitchingHook.ContextSwitchStatistics getContextStats() {
            return contextStats;
        }
        
        public EnvironmentalState getCurrentState() {
            return currentState;
        }
    }
}
