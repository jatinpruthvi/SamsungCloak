package com.samsungcloak.xposed;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RealityCoordinator - Cross-hook coordination for temporal alignment
 * 
 * Provides shared context state management across all hooks:
 * - Shared context state (walking, driving, etc.)
 * - Temporal alignment of hook behaviors
 * - Coherent behavior across all hooks
 * - Event coordination between hooks
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class RealityCoordinator {

    private static final String TAG = "[Reality][Coordinator]";
    private static final long STATE_CHANGE_COOLDOWN_MS = 5000; // 5 second cooldown

    // Context states representing device/user context
    public enum ContextState {
        IDLE,                   // Device is idle
        ACTIVE,                 // Device is actively being used
        WALKING,                // User is walking
        RUNNING,                // User is running  
        DRIVING,                // User is driving
        RIDING,                 // User is on public transit
        EXERCISE,               // User is exercising
        SLEEPING,               // User is sleeping
        WORKING,                // User is working
        MEAL,                   // User is eating
        MEETING,                // User is in a meeting
        OUTDOORS,               // User is outdoors
        INDOORS,                // User is indoors
        QUIET_ENV,              // Quiet environment
        NOISY_ENV,              // Noisy environment
        CHARGING,               // Device is charging
        LOW_BATTERY,            // Battery is low
        DOZING,                 // Device is in doze mode
        STANDBY,                // Device is in standby
        NIGHT_MODE,             // Night time (10pm-6am)
        MORNING_MODE,          // Morning (6am-9am)
        WORK_HOURS,             // Work hours (9am-5pm)
        EVENING_MODE           // Evening (6pm-10pm)
    }

    // Time periods
    public enum TimePeriod {
        NIGHT(22, 6),      // 10pm - 6am
        MORNING(6, 9),     // 6am - 9am
        WORK(9, 17),       // 9am - 5pm
        EVENING(17, 22);   // 5pm - 10pm

        public final int startHour;
        public final int endHour;

        TimePeriod(int start, int end) {
            this.startHour = start;
            this.endHour = end;
        }

        public boolean isInPeriod(int hour) {
            if (startHour > endHour) {
                // Overnight period (e.g., 22-6)
                return hour >= startHour || hour < endHour;
            }
            return hour >= startHour && hour < endHour;
        }
    }

    // Singleton instance
    private static final AtomicReference<RealityCoordinator> instance = 
        new AtomicReference<>();

    // Current context state
    private final AtomicReference<ContextState> currentState = 
        new AtomicReference<>(ContextState.IDLE);
    
    // Current time period
    private final AtomicReference<TimePeriod> currentTimePeriod = 
        new AtomicReference<>(TimePeriod.EVENING);

    // Last state change timestamp
    private final AtomicLong lastStateChangeTime = 
        new AtomicLong(0);

    // State change listeners
    private final CopyOnWriteArrayList<StateChangeListener> listeners = 
        new CopyOnWriteArrayList<>();

    // Shared data between hooks
    private final java.util.concurrent.ConcurrentHashMap<String, Object> sharedData = 
        new java.util.concurrent.ConcurrentHashMap<>();

    // Engagement level (0.0 - 1.0)
    private volatile float engagementLevel = 0.5f;

    // Battery level (0.0 - 1.0)
    private volatile float batteryLevel = 0.8f;

    // Screen on state
    private volatile boolean screenOn = true;

    // Thermal state
    private volatile ThermalState thermalState = ThermalState.NORMAL;

    // Current activity/app info
    private volatile String currentPackage = "";
    private volatile long sessionStartTime = 0;

    public enum ThermalState {
        NORMAL,
        WARM,
        THERMAL_THROTTLING,
        CRITICAL
    }

    /**
     * Get singleton instance
     */
    public static RealityCoordinator getInstance() {
        RealityCoordinator existing = instance.get();
        if (existing == null) {
            existing = new RealityCoordinator();
            instance.compareAndSet(null, existing);
            return instance.get();
        }
        return existing;
    }

    /**
     * Initialize coordinator
     */
    public static void init() {
        getInstance().updateTimePeriod();
        HookUtils.logInfo(TAG, "RealityCoordinator initialized");
    }

    /**
     * Get current context state
     */
    public ContextState getContextState() {
        return currentState.get();
    }

    /**
     * Update context state with cooldown protection
     */
    public void updateContextState(ContextState newState) {
        long now = System.currentTimeMillis();
        long lastChange = lastStateChangeTime.get();

        if (now - lastChange < STATE_CHANGE_COOLDOWN_MS) {
            return; // Cooldown active
        }

        ContextState oldState = currentState.get();
        if (oldState != newState) {
            currentState.set(newState);
            lastStateChangeTime.set(now);
            notifyStateChange(oldState, newState);
            HookUtils.logInfo(TAG, "Context state changed: " + oldState + " -> " + newState);
        }
    }

    /**
     * Get current time period
     */
    public TimePeriod getCurrentTimePeriod() {
        return currentTimePeriod.get();
    }

    /**
     * Update time period based on current hour
     */
    public void updateTimePeriod() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);

        for (TimePeriod period : TimePeriod.values()) {
            if (period.isInPeriod(hour)) {
                currentTimePeriod.set(period);
                break;
            }
        }
    }

    /**
     * Get engagement level (0.0 - 1.0)
     */
    public float getEngagementLevel() {
        return engagementLevel;
    }

    /**
     * Set engagement level
     */
    public void setEngagementLevel(float level) {
        this.engagementLevel = Math.max(0.0f, Math.min(1.0f, level));
    }

    /**
     * Get battery level (0.0 - 1.0)
     */
    public float getBatteryLevel() {
        return batteryLevel;
    }

    /**
     * Set battery level
     */
    public void setBatteryLevel(float level) {
        this.batteryLevel = Math.max(0.0f, Math.min(1.0f, level));
    }

    /**
     * Is screen on
     */
    public boolean isScreenOn() {
        return screenOn;
    }

    /**
     * Set screen state
     */
    public void setScreenOn(boolean on) {
        this.screenOn = on;
    }

    /**
     * Get thermal state
     */
    public ThermalState getThermalState() {
        return thermalState;
    }

    /**
     * Set thermal state
     */
    public void setThermalState(ThermalState state) {
        this.thermalState = state;
    }

    /**
     * Get current package
     */
    public String getCurrentPackage() {
        return currentPackage;
    }

    /**
     * Set current package
     */
    public void setCurrentPackage(String packageName) {
        this.currentPackage = packageName;
    }

    /**
     * Get session duration in milliseconds
     */
    public long getSessionDuration() {
        if (sessionStartTime == 0) return 0;
        return System.currentTimeMillis() - sessionStartTime;
    }

    /**
     * Start new session
     */
    public void startSession() {
        this.sessionStartTime = System.currentTimeMillis();
    }

    /**
     * Store shared data
     */
    public void putSharedData(String key, Object value) {
        sharedData.put(key, value);
    }

    /**
     * Get shared data
     */
    @SuppressWarnings("unchecked")
    public <T> T getSharedData(String key, T defaultValue) {
        Object value = sharedData.get(key);
        if (value != null && defaultValue != null && defaultValue.getClass().isInstance(value)) {
            return (T) value;
        }
        return defaultValue;
    }

    /**
     * Add state change listener
     */
    public void addStateChangeListener(StateChangeListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove state change listener
     */
    public void removeStateChangeListener(StateChangeListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notify all listeners of state change
     */
    private void notifyStateChange(ContextState oldState, ContextState newState) {
        for (StateChangeListener listener : listeners) {
            try {
                listener.onStateChange(oldState, newState);
            } catch (Exception e) {
                HookUtils.logError(TAG, "Listener notification failed", e);
            }
        }
    }

    /**
     * Get current hour (0-23)
     */
    public int getCurrentHour() {
        return java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
    }

    /**
     * Get current minute (0-59)
     */
    public int getCurrentMinute() {
        return java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE);
    }

    /**
     * Check if device is in night mode
     */
    public boolean isNightTime() {
        return currentTimePeriod.get() == TimePeriod.NIGHT;
    }

    /**
     * Check if device is in morning
     */
    public boolean isMorningTime() {
        return currentTimePeriod.get() == TimePeriod.MORNING;
    }

    /**
     * Check if device is in work hours
     */
    public boolean isWorkHours() {
        return currentTimePeriod.get() == TimePeriod.WORK;
    }

    /**
     * Check if device is in evening
     */
    public boolean isEveningTime() {
        return currentTimePeriod.get() == TimePeriod.EVENING;
    }

    /**
     * Interface for state change listeners
     */
    public interface StateChangeListener {
        void onStateChange(ContextState oldState, ContextState newState);
    }

    /**
     * Get summary string for debugging
     */
    public String getSummary() {
        return String.format("RealityCoordinator[state=%s, period=%s, engagement=%.2f, battery=%.2f, screenOn=%s, thermal=%s]",
            currentState.get(), currentTimePeriod.get(), engagementLevel, batteryLevel, screenOn, thermalState);
    }
}
