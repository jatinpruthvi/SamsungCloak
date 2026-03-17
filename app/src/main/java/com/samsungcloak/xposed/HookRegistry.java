package com.samsungcloak.xposed;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HookRegistry - Registry for managing all Xposed hooks
 * 
 * Provides:
 * - Centralized hook registration and initialization
 * - Hook lifecycle management
 * - Cross-hook coordination
 * - Configuration persistence
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class HookRegistry {

    private static final String TAG = "[HookRegistry]";

    // Singleton instance
    private static volatile HookRegistry instance;

    // Registered hooks map
    private final Map<String, BaseRealismHook> hooks = new HashMap<>();

    // Hook initialization order
    private final List<String> initializationOrder = new ArrayList<>();

    // Package being hooked
    private String currentPackage = "";

    // Initialization flag
    private boolean initialized = false;

    private HookRegistry() {
        // Private constructor for singleton
    }

    /**
     * Get singleton instance
     */
    public static HookRegistry getInstance() {
        if (instance == null) {
            synchronized (HookRegistry.class) {
                if (instance == null) {
                    instance = new HookRegistry();
                }
            }
        }
        return instance;
    }

    /**
     * Register a hook
     */
    public void registerHook(BaseRealismHook hook) {
        if (hook == null || hook.getHookId() == null) {
            return;
        }
        
        hooks.put(hook.getHookId(), hook);
        initializationOrder.add(hook.getHookId());
        HookUtils.logInfo(TAG, "Registered hook: " + hook.getHookId() + " - " + hook.getHookName());
    }

    /**
     * Get hook by ID
     */
    public BaseRealismHook getHook(String hookId) {
        return hooks.get(hookId);
    }

    /**
     * Get all registered hooks
     */
    public List<BaseRealismHook> getAllHooks() {
        List<BaseRealismHook> result = new ArrayList<>();
        for (String hookId : initializationOrder) {
            BaseRealismHook hook = hooks.get(hookId);
            if (hook != null) {
                result.add(hook);
            }
        }
        return result;
    }

    /**
     * Get enabled hooks
     */
    public List<BaseRealismHook> getEnabledHooks() {
        List<BaseRealismHook> result = new ArrayList<>();
        for (BaseRealismHook hook : getAllHooks()) {
            if (hook.isEnabled()) {
                result.add(hook);
            }
        }
        return result;
    }

    /**
     * Initialize all registered hooks
     */
    public void initializeAll(XC_LoadPackage.LoadPackageParam lpparam) {
        if (initialized) {
            HookUtils.logWarn(TAG, "Hooks already initialized");
            return;
        }

        currentPackage = lpparam.packageName;
        HookUtils.logInfo(TAG, "Initializing all hooks for: " + currentPackage);

        // Initialize static components first
        BaseRealismHook.initStatic(null);
        RealityCoordinator.init();

        // Initialize hooks in order
        int successCount = 0;
        int failCount = 0;

        for (String hookId : initializationOrder) {
            BaseRealismHook hook = hooks.get(hookId);
            if (hook != null) {
                try {
                    HookUtils.logInfo(TAG, "Initializing hook: " + hookId);
                    hook.init(lpparam);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    HookUtils.logError(TAG, "Failed to initialize hook: " + hookId, e);
                }
            }
        }

        initialized = true;
        HookUtils.logInfo(TAG, String.format("Initialization complete: %d successful, %d failed", 
            successCount, failCount));
    }

    /**
     * Re-initialize hooks (for testing or config changes)
     */
    public void reinitializeAll(XC_LoadPackage.LoadPackageParam lpparam) {
        HookUtils.logInfo(TAG, "Re-initializing all hooks");
        initialized = false;
        initializeAll(lpparam);
    }

    /**
     * Get initialization order
     */
    public List<String> getInitializationOrder() {
        return Collections.unmodifiableList(initializationOrder);
    }

    /**
     * Get hook count
     */
    public int getHookCount() {
        return hooks.size();
    }

    /**
     * Check if initialized
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Get current package
     */
    public String getCurrentPackage() {
        return currentPackage;
    }

    /**
     * Get hook by ID safely
     */
    @SuppressWarnings("unchecked")
    public <T extends BaseRealismHook> T getHookById(String hookId, Class<T> clazz) {
        BaseRealismHook hook = hooks.get(hookId);
        if (hook != null && clazz.isInstance(hook)) {
            return (T) hook;
        }
        return null;
    }

    /**
     * Enable hook
     */
    public void enableHook(String hookId) {
        BaseRealismHook hook = hooks.get(hookId);
        if (hook != null) {
            hook.enabled = true;
            HookUtils.logInfo(TAG, "Enabled hook: " + hookId);
        }
    }

    /**
     * Disable hook
     */
    public void disableHook(String hookId) {
        BaseRealismHook hook = hooks.get(hookId);
        if (hook != null) {
            hook.enabled = false;
            HookUtils.logInfo(TAG, "Disabled hook: " + hookId);
        }
    }

    /**
     * Set hook intensity
     */
    public void setHookIntensity(String hookId, float intensity) {
        BaseRealismHook hook = hooks.get(hookId);
        if (hook != null) {
            hook.intensity = intensity;
            HookUtils.logInfo(TAG, "Set intensity for " + hookId + ": " + intensity);
        }
    }

    /**
     * Get status summary
     */
    public String getStatusSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("HookRegistry Status:\n");
        sb.append("  Initialized: ").append(initialized).append("\n");
        sb.append("  Current Package: ").append(currentPackage).append("\n");
        sb.append("  Total Hooks: ").append(hooks.size()).append("\n");
        
        int enabled = 0;
        int disabled = 0;
        for (BaseRealismHook hook : hooks.values()) {
            if (hook.isEnabled()) {
                enabled++;
            } else {
                disabled++;
            }
        }
        sb.append("  Enabled: ").append(enabled).append("\n");
        sb.append("  Disabled: ").append(disabled).append("\n");
        
        return sb.toString();
    }

    /**
     * Clear all hooks (for testing)
     */
    public void clear() {
        hooks.clear();
        initializationOrder.clear();
        initialized = false;
    }
}
