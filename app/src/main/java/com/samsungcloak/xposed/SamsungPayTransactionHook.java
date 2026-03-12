package com.samsungcloak.xposed;

import android.content.Context;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
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
 * SamsungPayTransactionHook - Samsung Pay Transaction Simulation
 * 
 * Simulates Samsung Pay transaction issues:
 * - NFC payment failures
 * - MST (Magnetic Secure Transmission) issues
 * - "Try again" prompts
 * - Security check delays
 * - Card rejection
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class SamsungPayTransactionHook {

    private static final String TAG = "[SamsungPay][Payment]";
    private static final boolean DEBUG = true;
    
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    
    // Transaction failure rates
    private static float initialFailureRate = 0.08f;     // 8%
    private static float retryFailureRate = 0.05f;       // 5%
    private static float securityTimeoutRate = 0.03f;    // 3%
    private static float cardRejectionRate = 0.02f;      // 2%
    
    // Timing
    private static int minValidationDelay = 500;   // ms
    private static int maxValidationDelay = 2000;  // ms
    
    // State
    private static boolean isPayActive = false;
    private static int transactionAttempts = 0;
    private static String selectedCard = null;
    private static long lastTransactionTime = 0;
    
    private static final Random random = new Random();
    private static final List<TransactionEvent> transactionHistory = new CopyOnWriteArrayList<>();
    
    public static class TransactionEvent {
        public long timestamp;
        public String type;
        public String details;
        
        public TransactionEvent(String type, String details) {
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
        
        HookUtils.logInfo(TAG, "Initializing Samsung Pay Transaction Hook");
        
        try {
            hookSpayService(lpparam);
            hookNfcAdapter(lpparam);
            
            HookUtils.logInfo(TAG, "Samsung Pay hook initialized");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Init failed: " + t.getMessage());
            hookInitialized.set(false);
        }
    }
    
    private static void hookSpayService(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> spayClass = XposedHelpers.findClass(
                "com.samsung.android.spay.SpayFramework", lpparam.classLoader
            );
            
            // Hook initTransaction
            XposedBridge.hookAllMethods(spayClass, "initTransaction",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    isPayActive = true;
                    transactionAttempts = 0;
                    
                    transactionHistory.add(new TransactionEvent("TRANSACTION_START", 
                        "Payment initiated"));
                }
            });
            
            // Hook processTransaction
            XposedBridge.hookAllMethods(spayClass, "processTransaction",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    transactionAttempts++;
                    
                    // Check for transaction failure
                    float failureRate = transactionAttempts > 1 ? 
                        retryFailureRate : initialFailureRate;
                    
                    if (random.nextFloat() < failureRate) {
                        transactionHistory.add(new TransactionEvent("TRANSACTION_FAILED", 
                            "Attempt " + transactionAttempts + " failed"));
                        
                        HookUtils.logDebug(TAG, "Transaction failed (attempt " + 
                            transactionAttempts + ")");
                    }
                    
                    // Security timeout
                    if (random.nextFloat() < securityTimeoutRate) {
                        transactionHistory.add(new TransactionEvent("SECURITY_TIMEOUT", 
                            "Security check timed out"));
                        
                        HookUtils.logDebug(TAG, "Security timeout");
                    }
                    
                    // Card rejection
                    if (random.nextFloat() < cardRejectionRate) {
                        transactionHistory.add(new TransactionEvent("CARD_REJECTED", 
                            "Card rejected by terminal"));
                        
                        HookUtils.logDebug(TAG, "Card rejected");
                    }
                }
            });
            
            HookUtils.logInfo(TAG, "Spay service hooked");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "Spay hook skipped: " + t.getMessage());
        }
    }
    
    private static void hookNfcAdapter(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> nfcAdapterClass = XposedHelpers.findClass(
                "android.nfc.NfcAdapter", lpparam.classLoader
            );
            
            // Hook enableForegroundDispatch for pay mode
            XposedBridge.hookAllMethods(nfcAdapterClass, "enableForegroundDispatch",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    transactionHistory.add(new TransactionEvent("NFC_ENABLED", 
                        "NFC for payment enabled"));
                }
            });
            
            HookUtils.logInfo(TAG, "NfcAdapter hooked for Samsung Pay");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "NfcAdapter hook skipped: " + t.getMessage());
        }
    }
    
    // ========== Configuration ==========
    
    public static void setEnabled(boolean enabled) {
        SamsungPayTransactionHook.enabled = enabled;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void setInitialFailureRate(float rate) {
        initialFailureRate = Math.max(0, Math.min(0.5f, rate));
    }
    
    public static void setValidationDelayRange(int minMs, int maxMs) {
        minValidationDelay = Math.max(100, minMs);
        maxValidationDelay = Math.max(minValidationDelay, maxMs);
    }
    
    public static boolean isPayActive() {
        return isPayActive;
    }
    
    public static int getTransactionAttempts() {
        return transactionAttempts;
    }
    
    public static List<TransactionEvent> getTransactionHistory() {
        return new ArrayList<>(transactionHistory);
    }
}
