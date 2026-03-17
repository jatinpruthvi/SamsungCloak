package com.samsungcloak.xposed;

/**
 * BaseRealismHook - Base class for all realism hooks
 * 
 * Provides common functionality for all hooks including:
 * - Configuration management
 * - Cross-hook coordination through RealityCoordinator
 * - Common utility methods
 * - Graceful failure handling
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public abstract class BaseRealismHook {

    protected static final String TAG = "[Realism][Base]";
    protected static final boolean DEBUG = true;

    // Hook identification
    protected final String hookId;
    protected final String hookName;
    protected boolean enabled = true;
    protected float intensity = 0.5f;

    // Cross-hook coordination
    protected static RealityCoordinator coordinator;
    protected static ConfigurationManager configManager;

    public BaseRealismHook(String hookId, String hookName) {
        this.hookId = hookId;
        this.hookName = hookName;
    }

    /**
     * Initialize the hook - to be implemented by subclasses
     */
    public abstract void init(de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam lpparam);

    /**
     * Get the hook ID
     */
    public String getHookId() {
        return hookId;
    }

    /**
     * Get the hook name
     */
    public String getHookName() {
        return hookName;
    }

    /**
     * Check if hook is enabled
     */
    public boolean isEnabled() {
        return enabled && configManager != null && configManager.isHookEnabled(hookId);
    }

    /**
     * Get current intensity (0.0 - 1.0)
     */
    public float getIntensity() {
        return intensity * (configManager != null ? configManager.getHookIntensity(hookId) : 1.0f);
    }

    /**
     * Get shared context state from coordinator
     */
    protected RealityCoordinator.ContextState getContextState() {
        return coordinator != null ? coordinator.getContextState() : RealityCoordinator.ContextState.IDLE;
    }

    /**
     * Notify coordinator of state change
     */
    protected void notifyContextChange(RealityCoordinator.ContextState newState) {
        if (coordinator != null) {
            coordinator.updateContextState(newState);
        }
    }

    /**
     * Log info message
     */
    protected void logInfo(String message) {
        HookUtils.logInfo(TAG + "[" + hookId + "] " + message);
    }

    /**
     * Log debug message
     */
    protected void logDebug(String message) {
        if (DEBUG) {
            HookUtils.logInfo(TAG + "[" + hookId + "] " + message);
        }
    }

    /**
     * Log error message
     */
    protected void logError(String message, Throwable... t) {
        HookUtils.logError(TAG + "[" + hookId + "] " + message, t);
    }

    /**
     * Initialize static components
     */
    public static void initStatic(android.content.Context context) {
        if (configManager == null) {
            configManager = new ConfigurationManager(context);
        }
        if (coordinator == null) {
            coordinator = new RealityCoordinator();
        }
    }
}
