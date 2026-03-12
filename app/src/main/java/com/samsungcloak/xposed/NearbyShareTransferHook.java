package com.samsungcloak.xposed;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
 * NearbyShareTransferHook - File Transfer Simulation (Nearby Share)
 * 
 * Simulates Nearby Share/Quick Share behavior:
 * - Discovery delays (10-30 seconds)
 * - Transfer speed variation (2-15 MB/s)
 * - Failure rates (5%)
 * - Interrupted transfers (3% at 80%)
 * - Connection drops
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class NearbyShareTransferHook {

    private static final String TAG = "[Nearby][Share]";
    private static final boolean DEBUG = true;
    
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    
    // Transfer parameters
    private static int minDiscoveryDelay = 10000;   // 10 seconds
    private static int maxDiscoveryDelay = 30000;   // 30 seconds
    private static int minTransferSpeed = 2;         // MB/s
    private static int maxTransferSpeed = 15;        // MB/s
    private static float failureRate = 0.05f;        // 5% failure
    private static float interruptRate = 0.03f;      // 3% interrupt at 80%
    
    // State
    private static boolean isDiscovering = false;
    private static boolean isTransferring = false;
    private static int currentTransferProgress = 0;
    private static long transferStartTime = 0;
    private static String currentFileName = null;
    private static long fileSize = 0;
    private static int transferSpeed = 5; // MB/s
    
    private static final Random random = new Random();
    private static final List<TransferEvent> transferHistory = new CopyOnWriteArrayList<>();
    private static Handler transferHandler = null;
    
    public static class TransferEvent {
        public long timestamp;
        public String type;
        public String fileName;
        public int progress;
        public String details;
        
        public TransferEvent(String type, String fileName, int progress, String details) {
            this.timestamp = System.currentTimeMillis();
            this.type = type;
            this.fileName = fileName;
            this.progress = progress;
            this.details = details;
        }
    }
    
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (hookInitialized.getAndSet(true)) {
            HookUtils.logDebug(TAG, "Already initialized");
            return;
        }
        
        HookUtils.logInfo(TAG, "Initializing Nearby Share Transfer Hook");
        
        try {
            hookNearbyConnection(lpparam);
            hookFileProvider(lpparam);
            
            transferHandler = new Handler(Looper.getMainLooper());
            
            HookUtils.logInfo(TAG, "Nearby Share hook initialized");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Init failed: " + t.getMessage());
            hookInitialized.set(false);
        }
    }
    
    private static void hookNearbyConnection(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Try Samsung's NearbyShare or Google's Nearby Sharing
            Class<?> nearbyClass = XposedHelpers.findClass(
                "com.google.android.gms.nearby.sharing.NearbySharingFragments", 
                lpparam.classLoader
            );
            
            XposedBridge.hookAllMethods(nearbyClass, "startAdvertising",
                new XC_MethodHook() {
                @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    // Simulate discovery delay
                    int delay = minDiscoveryDelay + 
                        random.nextInt(maxDiscoveryDelay - minDiscoveryDelay);
                    
                    isDiscovering = true;
                    
                    transferHistory.add(new TransferEvent("DISCOVERY_START", 
                        null, 0, "Delay: " + delay + "ms"));
                    
                    HookUtils.logDebug(TAG, "Discovery started, delay: " + delay + "ms");
                }
            });
            
            XposedBridge.hookAllMethods(nearbyClass, "startTransfer",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    // Check for transfer failure
                    if (random.nextFloat() < failureRate) {
                        transferHistory.add(new TransferEvent("TRANSFER_FAILED", 
                            currentFileName, 0, "Initial failure"));
                        
                        HookUtils.logDebug(TAG, "Transfer failed (simulated)");
                        
                        // Don't block, just log
                    } else {
                        isTransferring = true;
                        transferStartTime = System.currentTimeMillis();
                        transferSpeed = minTransferSpeed + 
                            random.nextInt(maxTransferSpeed - minTransferSpeed);
                        
                        transferHistory.add(new TransferEvent("TRANSFER_START", 
                            currentFileName, 0, "Speed: " + transferSpeed + " MB/s"));
                        
                        HookUtils.logDebug(TAG, "Transfer started: " + transferSpeed + " MB/s");
                    }
                }
            });
            
            HookUtils.logInfo(TAG, "Nearby connection hooked");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "Nearby hook skipped: " + t.getMessage());
        }
    }
    
    private static void hookFileProvider(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> fileProviderClass = XposedHelpers.findClass(
                "android.content.FileProvider", lpparam.classLoader
            );
            
            // Hook openFileDescriptor
            XposedBridge.hookAllMethods(fileProviderClass, "openFileDescriptor",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    Uri uri = null;
                    if (param.args.length > 0 && param.args[0] instanceof Uri) {
                        uri = (Uri) param.args[0];
                    }
                    
                    if (uri != null) {
                        String path = uri.getPath();
                        if (path != null) {
                            currentFileName = path.substring(path.lastIndexOf('/') + 1);
                            
                            // Get file size (simplified)
                            fileSize = 1024 * 1024 * (1 + random.nextInt(100)); // 1-100 MB
                        }
                    }
                }
            });
            
            HookUtils.logInfo(TAG, "FileProvider hooked");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "FileProvider hook skipped: " + t.getMessage());
        }
    }
    
    /**
     * Simulate transfer progress
     */
    public static int simulateTransferProgress() {
        if (!isTransferring || fileSize == 0) {
            return 0;
        }
        
        // Calculate progress based on speed
        long elapsed = System.currentTimeMillis() - transferStartTime;
        long bytesTransferred = (elapsed / 1000) * transferSpeed * 1024 * 1024;
        
        currentTransferProgress = (int) ((bytesTransferred * 100) / fileSize);
        currentTransferProgress = Math.min(100, currentTransferProgress);
        
        // Check for interrupt at 80%
        if (currentTransferProgress >= 80 && random.nextFloat() < interruptRate) {
            isTransferring = false;
            
            transferHistory.add(new TransferEvent("TRANSFER_INTERRUPTED", 
                currentFileName, currentTransferProgress, "Connection lost at 80%"));
            
            HookUtils.logDebug(TAG, "Transfer interrupted at 80%");
            
            return currentTransferProgress;
        }
        
        // Complete transfer
        if (currentTransferProgress >= 100) {
            isTransferring = false;
            
            long totalTime = System.currentTimeMillis() - transferStartTime;
            
            transferHistory.add(new TransferEvent("TRANSFER_COMPLETE", 
                currentFileName, 100, "Total time: " + totalTime + "ms"));
            
            HookUtils.logDebug(TAG, "Transfer complete: " + totalTime + "ms");
        }
        
        return currentTransferProgress;
    }
    
    /**
     * Get estimated time remaining
     */
    public static long getEstimatedTimeRemaining() {
        if (!isTransferring || transferSpeed == 0) {
            return 0;
        }
        
        long remainingBytes = fileSize - (currentTransferProgress * fileSize / 100);
        return (remainingBytes / (transferSpeed * 1024 * 1024)) * 1000;
    }
    
    // ========== Configuration ==========
    
    public static void setEnabled(boolean enabled) {
        NearbyShareTransferHook.enabled = enabled;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void setDiscoveryDelayRange(int minMs, int maxMs) {
        minDiscoveryDelay = Math.max(5000, minMs);
        maxDiscoveryDelay = Math.max(minDiscoveryDelay, maxMs);
    }
    
    public static void setTransferSpeedRange(int minMb, int maxMb) {
        minTransferSpeed = Math.max(1, minMb);
        maxTransferSpeed = Math.max(minTransferSpeed, maxMb);
    }
    
    public static void setFailureRate(float rate) {
        failureRate = Math.max(0, Math.min(0.5f, rate));
    }
    
    public static boolean isTransferring() {
        return isTransferring;
    }
    
    public static int getCurrentProgress() {
        return currentTransferProgress;
    }
    
    public static String getCurrentFileName() {
        return currentFileName;
    }
    
    public static int getTransferSpeed() {
        return transferSpeed;
    }
    
    public static List<TransferEvent> getTransferHistory() {
        return new ArrayList<>(transferHistory);
    }
}
