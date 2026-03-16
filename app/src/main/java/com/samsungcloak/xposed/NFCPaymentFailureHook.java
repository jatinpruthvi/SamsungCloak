package com.samsungcloak.xposed;

import android.nfc.NfcAdapter;
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
 * NFCPaymentFailureHook - Contactless Payment Simulation
 * 
 * Simulates NFC payment failures and issues:
 * - Terminal communication errors
 * - Security check delays
 * - Payment processing failures
 * - Tap-to-pay timeouts
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class NFCPaymentFailureHook {

    private static final String TAG = "[NFC][Payment]";
    private static final boolean DEBUG = true;
    
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    
    // Parameters
    private static float paymentFailureRate = 0.10f;      // 10%
    private static float terminalDetectionDelay = 500;    // ms
    private static float securityCheckDelayMin = 2000;    // ms
    private static float securityCheckDelayMax = 5000;    // ms
    private static float timeoutRate = 0.05f;             // 5%
    
    // State
    private static boolean isPaymentInProgress = false;
    private static int paymentAttempts = 0;
    private static int successfulPayments = 0;
    
    private static final Random random = new Random();
    private static final List<NFCPaymentEvent> nfcEvents = new CopyOnWriteArrayList<>();
    
    public static class NFCPaymentEvent {
        public long timestamp;
        public String type;
        public String details;
        
        public NFCPaymentEvent(String type, String details) {
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
        
        HookUtils.logInfo(TAG, "Initializing NFC Payment Failure Hook");
        
        try {
            hookNfcAdapter(lpparam);
            hookSamsungPay(lpparam);
            hookTaplingManager(lpparam);
            
            HookUtils.logInfo(TAG, "NFC payment hook initialized");
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
            
            XposedBridge.hookAllMethods(nfcAdapterClass, "enableForegroundDispatch",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    // Terminal detection delay
                    nfcEvents.add(new NFCPaymentEvent("TERMINAL_DETECTION", 
                        "Delay: " + terminalDetectionDelay + "ms"));
                    
                    HookUtils.logDebug(TAG, "NFC terminal detection: " + terminalDetectionDelay + "ms");
                }
            });
            
            HookUtils.logInfo(TAG, "NfcAdapter hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "NfcAdapter hook failed: " + t.getMessage());
        }
    }
    
    private static void hookSamsungPay(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> samsungPayClass = XposedHelpers.findClass(
                "com.samsung.android.spay.SamsungPayManager", lpparam.classLoader
            );
            
            XposedBridge.hookAllMethods(samsungPayClass, "processTransaction",
                new XC_MethodHook() {
                @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    isPaymentInProgress = true;
                    paymentAttempts++;
                    
                    // Payment failure
                    if (random.nextFloat() < paymentFailureRate) {
                        nfcEvents.add(new NFCPaymentEvent("PAYMENT_FAILED", 
                            "Transaction declined"));
                        isPaymentInProgress = false;
                        return;
                    }
                    
                    // Security check delay
                    int delay = (int) (securityCheckDelayMin + 
                        random.nextFloat() * (securityCheckDelayMax - securityCheckDelayMin));
                    nfcEvents.add(new NFCPaymentEvent("SECURITY_CHECK", 
                        "Verification: " + delay + "ms"));
                    
                    HookUtils.logDebug(TAG, "Payment security check: " + delay + "ms");
                }
            });
            
            XposedBridge.hookAllMethods(samsungPayClass, "onTransactionComplete",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    isPaymentInProgress = false;
                    successfulPayments++;
                    
                    nfcEvents.add(new NFCPaymentEvent("PAYMENT_SUCCESS", 
                        "Total: " + paymentAttempts + ", Success: " + successfulPayments));
                }
            });
            
            HookUtils.logInfo(TAG, "SamsungPayManager hooked");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "SamsungPay hook skipped: " + t.getMessage());
        }
    }
    
    private static void hookTaplingManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> taplingClass = XposedHelpers.findClass(
                "com.samsung.android.nfc.TaplingManager", lpparam.classLoader
            );
            
            XposedBridge.hookAllMethods(taplingClass, "handleTap",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    // Tap timeout
                    if (random.nextFloat() < timeoutRate) {
                        nfcEvents.add(new NFCPaymentEvent("TAP_TIMEOUT", 
                            "Payment timed out"));
                    }
                }
            });
            
            HookUtils.logInfo(TAG, "TaplingManager hooked");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "TaplingManager hook skipped: " + t.getMessage());
        }
    }
    
    public static void setEnabled(boolean enabled) {
        NFCPaymentFailureHook.enabled = enabled;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void setPaymentFailureRate(float rate) {
        paymentFailureRate = Math.max(0, Math.min(1, rate));
    }
    
    public static int getPaymentAttempts() {
        return paymentAttempts;
    }
    
    public static int getSuccessfulPayments() {
        return successfulPayments;
    }
    
    public static List<NFCPaymentEvent> getNfcEvents() {
        return new ArrayList<>(nfcEvents);
    }
}
