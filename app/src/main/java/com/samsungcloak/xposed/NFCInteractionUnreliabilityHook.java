package com.samsungcloak.xposed;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.NfcA;
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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * NFCInteractionUnreliabilityHook - NFC Tap Failures & Retry Behavior
 * 
 * Simulates realistic NFC behavior:
 * - 8-15% initial failure rate
 * - Retry delay (1-3 seconds)
 * - "Security timeout" after multiple failures
 * - Antenna positioning issues
 * - Payment/transit specific failures
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class NFCInteractionUnreliabilityHook {

    private static final String TAG = "[NFC][Payment]";
    private static final boolean DEBUG = true;
    
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    
    // Failure rates
    private static float initialFailureRate = 0.12f;     // 12% first tap fails
    private static float retryFailureRate = 0.08f;       // 8% retry fails
    private static float securityTimeoutRate = 0.03f;    // 3% security timeout
    private static float antennaIssueRate = 0.05f;       // 5% antenna issues
    
    // Timing (ms)
    private static int minRetryDelay = 1000;
    private static int maxRetryDelay = 3000;
    private static int currentRetryDelay = 1500;
    
    // State
    private static boolean isNfcEnabled = true;
    private static boolean isInPaymentMode = false;
    private static int consecutiveFailures = 0;
    private static int maxFailuresBeforeLockout = 3;
    private static long lockoutEndTime = 0;
    private static int batteryLevel = 100;
    private static boolean isLowBattery = false;
    
    private static final Random random = new Random();
    private static final List<NFCEvent> nfcEvents = new CopyOnWriteArrayList<>();
    private static Handler nfcHandler = null;
    
    public static class NFCEvent {
        public long timestamp;
        public String type;       // TAP, RETRY, SUCCESS, FAILURE, LOCKOUT
        public String details;
        public int retryCount;
        
        public NFCEvent(String type, String details, int retryCount) {
            this.timestamp = System.currentTimeMillis();
            this.type = type;
            this.details = details;
            this.retryCount = retryCount;
        }
    }
    
    // Failure reasons
    private static final String[] FAILURE_REASONS = {
        "Tap again",
        "Connection lost",
        "Card not supported",
        "Try again",
        "Security timeout",
        "Antenna error",
        "Position adjustment needed",
        "Read error"
    };
    
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (hookInitialized.getAndSet(true)) {
            HookUtils.logDebug(TAG, "Already initialized");
            return;
        }
        
        HookUtils.logInfo(TAG, "Initializing NFC Interaction Unreliability Hook");
        
        try {
            hookNfcAdapter(lpparam);
            hookNfcManager(lpparam);
            hookBattery(lpparam);
            
            nfcHandler = new Handler(Looper.getMainLooper());
            
            HookUtils.logInfo(TAG, "NFC hook initialized");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Init failed: " + t.getMessage());
            hookInitialized.set(false);
        }
    }
    
    private static void hookNfcAdapter(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> nfcAdapterClass = XposedHelpers.findClass(
                "android.nfc.NfcAdapter", lpparam.classLoader
            );
            
            // Hook enableForegroundDispatch
            XposedBridge.hookAllMethods(nfcAdapterClass, "enableForegroundDispatch",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    boolean success = param.getResult() instanceof Boolean && (Boolean) param.getResult();
                    
                    if (success) {
                        HookUtils.logDebug(TAG, "NFC foreground dispatch enabled");
                    }
                }
            });
            
            // Hook disableForegroundDispatch
            XposedBridge.hookAllMethods(nfcAdapterClass, "disableForegroundDispatch",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    // Reset failure count when leaving NFC
                    consecutiveFailures = 0;
                    
                    HookUtils.logDebug(TAG, "NFC disabled, failures reset");
                }
            });
            
            // Hook isEnabled
            XposedBridge.hookAllMethods(nfcAdapterClass, "isEnabled",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    // Check for lockout
                    if (isLockedOut()) {
                        param.setResult(false);
                        HookUtils.logDebug(TAG, "NFC disabled due to lockout");
                    } else {
                        param.setResult(isNfcEnabled);
                    }
                }
            });
            
            HookUtils.logInfo(TAG, "NfcAdapter hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "NfcAdapter hook failed: " + t.getMessage());
        }
    }
    
    private static void hookNfcManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> nfcManagerClass = XposedHelpers.findClass(
                "android.nfc.NfcManager", lpparam.classLoader
            );
            
            // Hook getDefaultAdapter
            XposedBridge.hookAllMethods(nfcManagerClass, "getDefaultAdapter",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    // Could wrap the adapter
                }
            });
            
            HookUtils.logInfo(TAG, "NfcManager hooked");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "NfcManager hook skipped: " + t.getMessage());
        }
    }
    
    private static void hookBattery(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> batteryManagerClass = XposedHelpers.findClass(
                "android.os.BatteryManager", lpparam.classLoader
            );
            
            XposedBridge.hookAllMethods(batteryManagerClass, "getIntProperty",
                new XC_MethodHook() {
                @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    // Track battery level
                    if (param.args.length > 0 && param.args[0] instanceof Integer) {
                        int property = (int) param.args[0];
                        
                        if (property == 1) { // BATTERY_PROPERTY_CAPACITY
                            batteryLevel = (int) param.getResult();
                            isLowBattery = batteryLevel < 15;
                            
                            // NFC may be disabled at low battery
                            if (isLowBattery && random.nextFloat() < 0.3f) {
                                nfcEvents.add(new NFCEvent("LOW_BATTERY", 
                                    "Battery at " + batteryLevel + "%", 0));
                            }
                        }
                    }
                }
            });
            
            HookUtils.logInfo(TAG, "Battery hook for NFC");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "Battery hook skipped: " + t.getMessage());
        }
    }
    
    /**
     * Process an NFC tag discovery
     * Returns true if the tag should be passed through, false if blocked
     */
    public static boolean processTagDiscovery(Tag tag) {
        if (!enabled || !isNfcEnabled) {
            return true;
        }
        
        // Check for lockout
        if (isLockedOut()) {
            nfcEvents.add(new NFCEvent("LOCKOUT", 
                "Too many failures", consecutiveFailures));
            return false;
        }
        
        // Determine if this tap should fail
        float failureRate = consecutiveFailures > 0 ? retryFailureRate : initialFailureRate;
        
        // Increase failure rate at low battery
        if (isLowBattery) {
            failureRate += 0.1f;
        }
        
        // Add antenna issue chance
        if (random.nextFloat() < antennaIssueRate) {
            failureRate += 0.05f;
            nfcEvents.add(new NFCEvent("ANTENNA_ISSUE", 
                "Position adjustment needed", consecutiveFailures));
        }
        
        if (random.nextFloat() < failureRate) {
            // Tap failed
            consecutiveFailures++;
            
            String reason = FAILURE_REASONS[random.nextInt(FAILURE_REASONS.length)];
            
            nfcEvents.add(new NFCEvent("FAILURE", 
                reason, consecutiveFailures));
            
            // Check for security timeout
            if (consecutiveFailures >= maxFailuresBeforeLockout || 
                random.nextFloat() < securityTimeoutRate) {
                // Enter lockout
                long lockoutDuration = (consecutiveFailures * 5000) + random.nextInt(5000);
                lockoutEndTime = System.currentTimeMillis() + lockoutDuration;
                
                nfcEvents.add(new NFCEvent("LOCKOUT", 
                    "Security timeout: " + lockoutDuration + "ms", consecutiveFailures));
                
                HookUtils.logInfo(TAG, "NFC locked out for " + lockoutDuration + "ms");
            } else {
                // Schedule retry
                scheduleRetry();
            }
            
            return false;
        } else {
            // Tap succeeded
            nfcEvents.add(new NFCEvent("SUCCESS", 
                "Tag read", consecutiveFailures));
            consecutiveFailures = 0;
            
            return true;
        }
    }
    
    /**
     * Schedule a retry after failure
     */
    private static void scheduleRetry() {
        if (nfcHandler == null) return;
        
        int delay = minRetryDelay + random.nextInt(maxRetryDelay - minRetryDelay);
        
        nfcHandler.postDelayed(() -> {
            nfcEvents.add(new NFCEvent("RETRY", 
                "Scheduled retry", consecutiveFailures));
            HookUtils.logDebug(TAG, "NFC retry available");
        }, delay);
    }
    
    /**
     * Check if NFC is currently locked out
     */
    private static boolean isLockedOut() {
        return lockoutEndTime > System.currentTimeMillis();
    }
    
    /**
     * Set NFC payment mode
     */
    public static void setPaymentMode(boolean payment) {
        isInPaymentMode = payment;
        
        if (payment) {
            consecutiveFailures = 0;
        }
    }
    
    // ========== Configuration ==========
    
    public static void setEnabled(boolean enabled) {
        NFCInteractionUnreliabilityHook.enabled = enabled;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void setInitialFailureRate(float rate) {
        initialFailureRate = Math.max(0, Math.min(0.5f, rate));
    }
    
    public static void setRetryDelayRange(int minMs, int maxMs) {
        minRetryDelay = Math.max(500, minMs);
        maxRetryDelay = Math.max(minRetryDelay, maxMs);
    }
    
    public static int getConsecutiveFailures() {
        return consecutiveFailures;
    }
    
    public static void resetFailures() {
        consecutiveFailures = 0;
        lockoutEndTime = 0;
    }
    
    public static List<NFCEvent> getNfcEvents() {
        return new ArrayList<>(nfcEvents);
    }
}
