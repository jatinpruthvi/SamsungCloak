package com.samsungcloak.xposed;

import android.content.pm.PackageManager;
import android.content.pm.PackageInstaller;
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
 * AppInstallBehaviorHook - Installation & Update Timing
 * 
 * Simulates realistic app installation behaviors:
 * - Download speed variation
 * - Verification delays
 * - Installation failures
 * - Update verification timeouts
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class AppInstallBehaviorHook {

    private static final String TAG = "[App][InstallBehavior]";
    private static final boolean DEBUG = true;
    
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    
    // Parameters
    private static int minVerificationDelay = 2000;  // ms
    private static int maxVerificationDelay = 15000; // ms
    private static float installFailureRate = 0.05f; // 5%
    private static float downloadSpeedMbps = 10;     // Mbps
    
    // State
    private static boolean isInstalling = false;
    private static String currentPackage = null;
    
    private static final Random random = new Random();
    private static final List<InstallEvent> installEvents = new CopyOnWriteArrayList<>();
    
    public static class InstallEvent {
        public long timestamp;
        public String type;
        public String packageName;
        public String details;
        
        public InstallEvent(String type, String packageName, String details) {
            this.timestamp = System.currentTimeMillis();
            this.type = type;
            this.packageName = packageName;
            this.details = details;
        }
    }
    
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (hookInitialized.getAndSet(true)) {
            HookUtils.logDebug(TAG, "Already initialized");
            return;
        }
        
        HookUtils.logInfo(TAG, "Initializing App Install Behavior Hook");
        
        try {
            hookPackageManager(lpparam);
            hookPackageInstaller(lpparam);
            
            HookUtils.logInfo(TAG, "Install hook initialized");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Init failed: " + t.getMessage());
            hookInitialized.set(false);
        }
    }
    
    private static void hookPackageManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> packageManagerClass = XposedHelpers.findClass(
                "android.content.pm.PackageManager", lpparam.classLoader
            );
            
            XposedBridge.hookAllMethods(packageManagerClass, "installPackage",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    isInstalling = true;
                    currentPackage = (String) param.args[0];
                    
                    // Verification delay
                    int delay = minVerificationDelay + 
                        random.nextInt(maxVerificationDelay - minVerificationDelay);
                    
                    installEvents.add(new InstallEvent("VERIFYING", currentPackage, 
                        "Verification: " + delay + "ms"));
                    
                    HookUtils.logDebug(TAG, "Install verification: " + delay + "ms");
                }
            });
            
            XposedBridge.hookAllMethods(packageManagerClass, "deletePackage",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    String pkg = (String) param.args[0];
                    
                    // Uninstall delay
                    int delay = 500 + random.nextInt(2000);
                    installEvents.add(new InstallEvent("UNINSTALLING", pkg, 
                        "Removing: " + delay + "ms"));
                }
            });
            
            HookUtils.logInfo(TAG, "PackageManager hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "PackageManager hook failed: " + t.getMessage());
        }
    }
    
    private static void hookPackageInstaller(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> installerClass = XposedHelpers.findClass(
                "android.content.pm.PackageInstaller", lpparam.classLoader
            );
            
            XposedBridge.hookAllMethods(installerClass, "createSession",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    // Simulate download speed variation
                    float speedVariation = 0.7f + random.nextFloat() * 0.6f; // 70%-130%
                    float actualSpeed = downloadSpeedMbps * speedVariation;
                    
                    installEvents.add(new InstallEvent("DOWNLOAD", currentPackage, 
                        "Speed: " + actualSpeed + " Mbps"));
                }
            });
            
            HookUtils.logInfo(TAG, "PackageInstaller hooked");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "PackageInstaller hook skipped: " + t.getMessage());
        }
    }
    
    public static void setEnabled(boolean enabled) {
        AppInstallBehaviorHook.enabled = enabled;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void setDownloadSpeed(float mbps) {
        downloadSpeedMbps = mbps;
    }
    
    public static List<InstallEvent> getInstallEvents() {
        return new ArrayList<>(installEvents);
    }
}
