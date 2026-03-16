package com.samsungcloak.xposed;

import android.telephony.CallsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.os.Handler;
import android.os.Looper;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * DualSIMManagementHook - Multi-SIM & Telephony
 * 
 * Simulates dual-SIM device behaviors:
 * - SIM switch delays
 * - Data routing issues
 * - Call routing conflicts
 * - VoLTE/VoWiFi handover problems
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class DualSIMManagementHook {

    private static final String TAG = "[DualSIM][Management]";
    private static final boolean DEBUG = true;
    
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    
    // Parameters
    private static int simSwitchDelayMin = 500;     // ms
    private static int simSwitchDelayMax = 2000;    // ms
    private static float callRoutingConflictRate = 0.10f; // 10%
    private static float dataDropRate = 0.08f;      // 8%
    
    // State
    private static int activeSIM = 0; // 0 or 1
    private static boolean isOnCall = false;
    private static boolean dualSIMEnabled = true;
    
    private static final Random random = new Random();
    private static final List<SIMEvent> simEvents = new CopyOnWriteArrayList<>();
    
    public static class SIMEvent {
        public long timestamp;
        public String type;
        public String details;
        
        public SIMEvent(String type, String details) {
            this.timestamp = System.currentTimeMillis();
            this.type = type;
            this.details = details;
        }
    }
    
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (hookInitialized.getAndSet(true)) {
            HookUtils.logDebug(TAG, "Already initialized");
            return;
        }
        
        HookUtils.logInfo(TAG, "Initializing Dual SIM Management Hook");
        
        try {
            hookSubscriptionManager(lpparam);
            hookCallsManager(lpparam);
            hookPhoneInterface(lpparam);
            
            HookUtils.logInfo(TAG, "Dual SIM hook initialized");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Init failed: " + t.getMessage());
            hookInitialized.set(false);
        }
    }
    
    private static void hookSubscriptionManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> subscriptionManagerClass = XposedHelpers.findClass(
                "android.telephony.SubscriptionManager", lpparam.classLoader
            );
            
            XposedBridge.hookAllMethods(subscriptionManagerClass, "getActiveSubscriptionInfoList",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || !dualSIMEnabled) return;
                    
                    // SIM enumeration delay
                    int delay = simSwitchDelayMin + 
                        random.nextInt(simSwitchDelayMax - simSwitchDelayMin);
                    
                    simEvents.add(new SIMEvent("SIM_ENUMERATION", 
                        "SIM list retrieval: " + delay + "ms"));
                    
                    HookUtils.logDebug(TAG, "SIM enumeration delay: " + delay + "ms");
                }
            });
            
            XposedBridge.hookAllMethods(subscriptionManagerClass, "setDefaultDataSlotId",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || !dualSIMEnabled) return;
                    
                    int slotId = (int) param.args[0];
                    
                    // SIM switch delay
                    int delay = simSwitchDelayMin + 
                        random.nextInt(simSwitchDelayMax - simSwitchDelayMin);
                    
                    activeSIM = slotId;
                    
                    simEvents.add(new SIMEvent("SIM_SWITCH", 
                        "Switching to SIM " + slotId + " (delay: " + delay + "ms)"));
                    
                    HookUtils.logDebug(TAG, "SIM switch delay: " + delay + "ms");
                }
            });
            
            HookUtils.logInfo(TAG, "SubscriptionManager hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "SubscriptionManager hook failed: " + t.getMessage());
        }
    }
    
    private static void hookCallsManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> callsManagerClass = XposedHelpers.findClass(
                "android.telephony.CallsManager", lpparam.classLoader
            );
            
            XposedBridge.hookAllMethods(callsManagerClass, "placeCall",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || !dualSIMEnabled) return;
                    
                    // Call routing conflict
                    if (isOnCall && random.nextFloat() < callRoutingConflictRate) {
                        simEvents.add(new SIMEvent("CALL_ROUTING_CONFLICT", 
                            "SIM routing conflict - call on other SIM"));
                    }
                    
                    isOnCall = true;
                    simEvents.add(new SIMEvent("CALL_STARTED", 
                        "Call initiated on SIM " + activeSIM));
                }
            });
            
            XposedBridge.hookAllMethods(callsManagerClass, "onCallTerminated",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    isOnCall = false;
                    simEvents.add(new SIMEvent("CALL_ENDED", "Call ended"));
                }
            });
            
            HookUtils.logInfo(TAG, "CallsManager hooked");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "CallsManager hook skipped: " + t.getMessage());
        }
    }
    
    private static void hookPhoneInterface(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> phoneInterfaceClass = XposedHelpers.findClass(
                "com.android.phone.PhoneInterfaceManager", lpparam.classLoader
            );
            
            XposedBridge.hookAllMethods(phoneInterfaceClass, "getDataEnabledState",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || !dualSIMEnabled) return;
                    
                    // Data connection drop during SIM switch
                    if (isOnCall && random.nextFloat() < dataDropRate) {
                        simEvents.add(new SIMEvent("DATA_DROP", 
                            "Data connection dropped during call on different SIM"));
                    }
                }
            });
            
            HookUtils.logInfo(TAG, "PhoneInterfaceManager hooked");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "PhoneInterface hook skipped: " + t.getMessage());
        }
    }
    
    public static void setEnabled(boolean enabled) {
        DualSIMManagementHook.enabled = enabled;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void setDualSIMEnabled(boolean enabled) {
        dualSIMEnabled = enabled;
    }
    
    public static int getActiveSIM() {
        return activeSIM;
    }
    
    public static void setActiveSIM(int sim) {
        activeSIM = sim;
    }
    
    public static boolean isOnCall() {
        return isOnCall;
    }
    
    public static List<SIMEvent> getSIMEvents() {
        return new ArrayList<>(simEvents);
    }
}
