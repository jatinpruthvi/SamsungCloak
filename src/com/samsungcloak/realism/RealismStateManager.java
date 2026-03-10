package com.samsungcloak.realism;

import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * RealismStateManager - Centralized state management for cross-hook coherence
 * 
 * This manager ensures all realism hooks operate coherently by:
 * - Maintaining shared state across hooks
 * - Coordinating cross-hook interactions
 * - Providing unified state queries
 * - Managing coherence timelines
 */
public class RealismStateManager {
    private static final String TAG = "RealismStateManager";
    private static RealismStateManager sInstance;
    
    // Shared preferences
    private SharedPreferences sPrefs;
    
    // System context
    private SystemContext sSystemContext;
    
    // Activity context
    private ActivityContext sActivityContext;
    
    // Coherence timeline
    private CoherenceTimeline sTimeline;
    
    // Active hooks
    private Set<String> mActiveHooks = new HashSet<>();
    
    // Hook contexts
    private Map<String, HookContext> mHookContexts = new HashMap<>();
    
    private RealismStateManager() {
        sSystemContext = new SystemContext();
        sActivityContext = new ActivityContext();
        sTimeline = new CoherenceTimeline();
    }
    
    public static RealismStateManager getInstance() {
        if (sInstance == null) {
            sInstance = new RealismStateManager();
        }
        return sInstance;
    }
    
    /**
     * Initialize the state manager
     */
    public void initialize(SharedPreferences prefs) {
        sPrefs = prefs;
    }
    
    /**
     * Register a hook context
     */
    public void registerHookContext(String packageName) {
        HookContext context = new HookContext();
        context.packageName = packageName;
        context.initialized = true;
        context.lastUpdate = System.currentTimeMillis();
        
        mHookContexts.put(packageName, context);
        mActiveHooks.add(packageName);
    }
    
    /**
     * Get system state
     */
    public RealismSystemState getSystemState() {
        RealismSystemState state = new RealismSystemState();

        // Gather state from all hooks
        state.haptic = HapticFeedbackRealismHook.getState();
        state.notifications = NotificationInterruptionHook.getState();
        state.biometric = BiometricRealismHook.getState();
        state.deviceGrip = DeviceGripAndOrientationHook.getState();
        state.power = PowerStateManagementHook.getState();
        state.audioEnvironment = AudioEnvironmentRealismHook.getState();
        state.gpsTrajectory = GPSLocationTrajectoryHook.getState();

        // System context
        state.system = sSystemContext;

        // Activity context
        state.activity = sActivityContext;

        // Timeline
        state.timeline = sTimeline.getCurrentState();

        return state;
    }
    
    /**
     * Update system context
     */
    public void updateSystemContext(long timestamp, boolean screenOn, int orientation) {
        sSystemContext.timestamp = timestamp;
        sSystemContext.screenOn = screenOn;
        sSystemContext.orientation = orientation;
        sSystemContext.uptimeMinutes = (System.currentTimeMillis() - sSystemContext.bootTime) / 60000;
    }
    
    /**
     * Update activity context
     */
    public void updateActivityContext(String packageName, String activityName, boolean isForeground) {
        if (isForeground) {
            sActivityContext.currentPackage = packageName;
            sActivityContext.currentActivity = activityName;
            sActivityContext.lastActivityChange = System.currentTimeMillis();
        }
    }
    
    /**
     * Record user interaction
     */
    public void recordUserInteraction(float x, float y, int action) {
        sActivityContext.lastInteractionTime = System.currentTimeMillis();
        sActivityContext.lastTouchX = x;
        sActivityContext.lastTouchY = y;
        sActivityContext.lastTouchAction = action;
        
        // Propagate to hooks that need this info
        NotificationInterruptionHook.recordUserInteraction();
        
        // Update grip context
        DeviceGripAndOrientationHook.setScreenDimensions(
            sSystemContext.screenWidth, 
            sSystemContext.screenHeight
        );
    }
    
    /**
     * Update device state
     */
    public void updateDeviceState(float batteryLevel, float temperature, boolean isCharging) {
        sSystemContext.batteryLevel = batteryLevel;
        sSystemContext.batteryTemperature = temperature;
        sSystemContext.isCharging = isCharging;
        
        // Propagate to dependent hooks
        PowerStateManagementHook.setBatteryLevel(batteryLevel);
        PowerStateManagementHook.setBatteryTemperature(temperature);
        
        if (isCharging != sSystemContext.wasCharging) {
            PowerStateManagementHook.setCharging(isCharging);
            sSystemContext.wasCharging = isCharging;
        }
    }
    
    /**
     * Update motion state
     */
    public void updateMotionState(boolean isWalking, boolean isRunning, float speed) {
        sActivityContext.isWalking = isWalking;
        sActivityContext.isRunning = isRunning;
        sActivityContext.movementSpeed = speed;
        
        // Propagate to device grip hook
        DeviceGripAndOrientationHook.setWalkingState(isWalking);
        
        // Update sensor context for other hooks
        sSystemContext.lastMotionUpdate = System.currentTimeMillis();
    }
    
    /**
     * Record app usage
     */
    public void recordAppUsage(String packageName) {
        PowerStateManagementHook.recordAppUsage(packageName);
        
        sActivityContext.foregroundPackage = packageName;
        NotificationInterruptionHook.setCurrentForegroundApp(packageName);
    }
    
    /**
     * System context container
     */
    public static class SystemContext {
        public long timestamp;
        public long bootTime = System.currentTimeMillis();
        public boolean screenOn = true;
        public int orientation = 0;  // 0=portrait, 1=landscape
        public int screenWidth = 720;
        public int screenHeight = 1600;
        public float batteryLevel = 0.85f;
        public float batteryTemperature = 25.0f;
        public boolean isCharging = false;
        public boolean wasCharging = false;
        public long uptimeMinutes;
        public long lastMotionUpdate;
    }
    
    /**
     * Activity context container
     */
    public static class ActivityContext {
        public String currentPackage;
        public String currentActivity;
        public String foregroundPackage;
        public long lastActivityChange;
        public long lastInteractionTime;
        public float lastTouchX;
        public float lastTouchY;
        public int lastTouchAction;
        public boolean isWalking;
        public boolean isRunning;
        public float movementSpeed;
    }
    
    /**
     * Coherence timeline
     */
    public static class CoherenceTimeline {
        private long mStartTime = System.currentTimeMillis();
        private Map<String, Long> mEventTimes = new HashMap<>();
        
        public void recordEvent(String eventType) {
            mEventTimes.put(eventType, System.currentTimeMillis());
        }
        
        public long getEventTime(String eventType) {
            return mEventTimes.containsKey(eventType) ? mEventTimes.get(eventType) : 0;
        }
        
        public long getElapsedSince(String eventType) {
            return System.currentTimeMillis() - getEventTime(eventType);
        }
        
        public TimelineState getCurrentState() {
            TimelineState state = new TimelineState();
            state.systemUptime = System.currentTimeMillis() - mStartTime;
            state.eventTimes = new HashMap<>(mEventTimes);
            return state;
        }
    }
    
    /**
     * Complete system state
     */
    public static class RealismSystemState {
        public HapticFeedbackRealismHook.HapticFeedbackState haptic;
        public NotificationInterruptionHook.NotificationState notifications;
        public BiometricRealismHook.BiometricState biometric;
        public DeviceGripAndOrientationHook.DeviceGripState deviceGrip;
        public PowerStateManagementHook.PowerState power;
        public AudioEnvironmentRealismHook.AudioEnvironmentState audioEnvironment;
        public GPSLocationTrajectoryHook.TrajectoryState gpsTrajectory;
        public SystemContext system;
        public ActivityContext activity;
        public CoherenceTimeline.TimelineState timeline;
        
        /**
         * Check if system is in coherent state
         */
        public boolean isCoherent() {
            // Check for contradictions
            if (deviceGrip != null && deviceGrip.isWalking) {
                // Should have sensor activity
                // Should have step-cycle in haptic if enabled
            }
            
            if (power != null && power.isCharging) {
                // Battery should be increasing
                // Some apps might be restricted
            }
            
            return true;  // Placeholder for actual coherence checks
        }
    }
    
    /**
     * Timeline state
     */
    public static class TimelineState {
        public long systemUptime;
        public Map<String, Long> eventTimes;
    }
    
    /**
     * Hook context
     */
    private static class HookContext {
        String packageName;
        boolean initialized;
        long lastUpdate;
    }
}
