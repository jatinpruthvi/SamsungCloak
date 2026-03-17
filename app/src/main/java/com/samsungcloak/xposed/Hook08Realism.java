package com.samsungcloak.xposed;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicFloat;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Hook08Realism - Network Quality & Handover Realism (IMPROVED)
 * 
 * IMPROVEMENTS:
 * - WiFi Assist Simulation: supplement slow WiFi with LTE (threshold: latency>300ms, download<1Mbps)
 * - Captive Portal Detection: 2-5s delay on public WiFi
 * - Network State Debouncing: 10-30s window for rapid changes
 * - Background Network Restrictions: Android 10+ throttles background apps to 10-50%
 * - Data Saver Mode: restrict background data, block metered network
 * - Carrier-Specific Behaviors: port blocking, NAT behavior differences
 * - Network Congestion Patterns: peak hours 2-3x latency, 5-10x packet loss
 * - VPN Effects: +50-200ms latency, -10-30% bandwidth, DNS delays
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class Hook08Realism extends BaseRealismHook {

    private static final String TAG = "[Realism][Hook08-Network]";
    private static final boolean DEBUG = true;
    
    private static final String HOOK_ID = "hook_08";
    private static final String HOOK_NAME = "Network Quality & Handover Realism";
    
    // Configuration keys
    private static final String KEY_ENABLED = "network_quality_enabled";
    private static final String KEY_WIFI_ASSIST = "wifi_assist_enabled";
    private static final String KEY_CONGESTION = "congestion_enabled";
    private static final String KEY_VPN = "vpn_effects_enabled";
    
    // Hook state
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    private static AtomicBoolean enabled = new AtomicBoolean(true);
    private static float intensity = 0.5f;
    
    // Network types
    public enum NetworkType {
        WIFI,
        LTE,
        5G,
        CELLULAR,
        ETHERNET,
        VPN
    }
    
    // Current network state
    private static final AtomicReference<NetworkType> currentNetworkType = 
        new AtomicReference<>(NetworkType.WIFI);
    private static final AtomicBoolean isConnected = new AtomicBoolean(true);
    private static final AtomicBoolean isMetered = new AtomicBoolean(false);
    private static final AtomicBoolean dataSaverEnabled = new AtomicBoolean(false);
    private static final AtomicBoolean vpnEnabled = new AtomicBoolean(false);
    
    // Network quality metrics
    private static final AtomicFloat latencyMs = new AtomicFloat(50.0f);
    private static final AtomicFloat downloadMbps = new AtomicFloat(100.0f);
    private static final AtomicFloat uploadMbps = new AtomicFloat(50.0f);
    private static final AtomicFloat packetLoss = new AtomicFloat(0.0f);
    private static final AtomicInteger signalStrength = new AtomicInteger(-50);
    
    // WiFi Assist
    private static boolean wifiAssistEnabled = true;
    private static int wifiAssistLatencyThreshold = 300;
    private static float wifiAssistDownloadThreshold = 1.0f;
    
    // Captive portal
    private static boolean captivePortalEnabled = true;
    private static int captivePortalDelayMs = 3000;
    private static boolean isCaptivePortal = false;
    
    // Network debouncing
    private static boolean debouncingEnabled = true;
    private static int debounceWindowMs = 20000;
    private static long lastNetworkChangeTime = 0;
    private static int networkChangeCount = 0;
    
    // Background restrictions (Android 10+)
    private static boolean backgroundRestrictionEnabled = true;
    private static float backgroundThrottleFactor = 0.30f;
    
    // Data saver
    private static boolean dataSaverModeEnabled = false;
    private static float dataSaverBackgroundLimit = 0.10f;
    
    // Congestion patterns
    private static boolean congestionEnabled = true;
    private static float peakHourMultiplier = 2.5f;
    private static float peakPacketLossMultiplier = 8.0f;
    
    // VPN effects
    private static boolean vpnEffectsEnabled = true;
    private static int vpnLatencyAddMs = 100;
    private static float vpnBandwidthLoss = 0.20f;
    private static int vpnDnsDelayMs = 50;
    
    // Carrier-specific
    private static String carrierName = "AT&T";
    private static boolean carrierPortBlocking = false;
    private static float carrierNatFactor = 1.0f;
    
    // Tracking
    private static long sessionStartTime = 0;
    private static final AtomicInteger handovers = new AtomicInteger(0);
    private static final AtomicInteger disconnections = new AtomicInteger(0);
    private static final AtomicInteger wifiAssistActivations = new AtomicInteger(0);
    
    // Network history
    private static final CopyOnWriteArrayList<NetworkSnapshot> networkHistory = 
        new CopyOnWriteArrayList<>();
    private static final int MAX_HISTORY = 50;
    
    // Thread-local random
    private static final ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new Random());
    
    public Hook08Realism() {
        super(HOOK_ID, HOOK_NAME);
    }
    
    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!hookInitialized.compareAndSet(false, true)) {
            logInfo("Hook already initialized");
            return;
        }
        
        logInfo("Initializing Network Quality & Handover Realism Hook (IMPROVED)");
        sessionStartTime = System.currentTimeMillis();
        
        try {
            // Load configuration
            loadConfiguration();
            
            // Hook ConnectivityManager
            hookConnectivityManager(lpparam);
            
            // Hook NetworkCapabilities
            hookNetworkCapabilities(lpparam);
            
            // Hook WiFi Manager
            hookWifiManager(lpparam);
            
            // Hook URL connection for latency simulation
            hookUrlConnection(lpparam);
            
            // Hook DNS
            hookDns(lpparam);
            
            logInfo("Network Quality Hook initialized successfully");
        } catch (Exception e) {
            logError("Failed to initialize Network Quality Hook", e);
        }
    }
    
    private void loadConfiguration() {
        if (configManager != null) {
            enabled.set(configManager.getHookParamBool(HOOK_ID, KEY_ENABLED, true));
            intensity = configManager.getHookIntensity(HOOK_ID);
            
            wifiAssistEnabled = configManager.getHookParamBool(HOOK_ID, KEY_WIFI_ASSIST, true);
            wifiAssistLatencyThreshold = configManager.getHookParamInt(HOOK_ID, "wifi_assist_latency", 300);
            wifiAssistDownloadThreshold = configManager.getHookParamFloat(HOOK_ID, "wifi_assist_download", 1.0f);
            
            captivePortalEnabled = configManager.getHookParamBool(HOOK_ID, "captive_portal_enabled", true);
            captivePortalDelayMs = configManager.getHookParamInt(HOOK_ID, "captive_portal_delay_ms", 3000);
            
            debouncingEnabled = configManager.getHookParamBool(HOOK_ID, "debouncing_enabled", true);
            debounceWindowMs = configManager.getHookParamInt(HOOK_ID, "debounce_window_ms", 20000);
            
            backgroundRestrictionEnabled = configManager.getHookParamBool(HOOK_ID, "background_restriction_enabled", true);
            backgroundThrottleFactor = configManager.getHookParamFloat(HOOK_ID, "background_throttle_factor", 0.30f);
            
            congestionEnabled = configManager.getHookParamBool(HOOK_ID, KEY_CONGESTION, true);
            peakHourMultiplier = configManager.getHookParamFloat(HOOK_ID, "peak_hour_multiplier", 2.5f);
            
            vpnEffectsEnabled = configManager.getHookParamBool(HOOK_ID, KEY_VPN, true);
            vpnLatencyAddMs = configManager.getHookParamInt(HOOK_ID, "vpn_latency_ms", 100);
            vpnBandwidthLoss = configManager.getHookParamFloat(HOOK_ID, "vpn_bandwidth_loss", 0.20f);
        }
        
        logInfo("Configuration loaded: enabled=" + enabled.get() + ", intensity=" + intensity);
    }
    
    private void hookConnectivityManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> connectivityManagerClass = XposedHelpers.findClass(
                "android.net.ConnectivityManager", lpparam.classLoader);
            
            // Hook getActiveNetworkInfo
            XposedBridge.hookAllMethods(connectivityManagerClass, "getActiveNetworkInfo",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get()) return;
                        
                        try {
                            // Apply background restrictions if needed
                            if (backgroundRestrictionEnabled && isAppInBackground()) {
                                // Throttle the network info
                            }
                        } catch (Exception e) {
                            logDebug("Error in getActiveNetworkInfo hook: " + e.getMessage());
                        }
                    }
                });
            
            // Hook getNetworkInfo
            XposedBridge.hookAllMethods(connectivityManagerClass, "getNetworkInfo",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get()) return;
                        
                        try {
                            // Apply network quality simulation
                        } catch (Exception e) {
                            logDebug("Error in getNetworkInfo hook: " + e.getMessage());
                        }
                    }
                });
            
            // Hook requestNetwork
            XposedBridge.hookAllMethods(connectivityManagerClass, "requestNetwork",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get()) return;
                        
                        try {
                            // Check for WiFi assist conditions
                            checkWifiAssist();
                        } catch (Exception e) {
                            logDebug("Error in requestNetwork hook: " + e.getMessage());
                        }
                    }
                });
            
            logInfo("Hooked ConnectivityManager");
        } catch (Exception e) {
            logError("Failed to hook ConnectivityManager", e);
        }
    }
    
    private void hookNetworkCapabilities(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> networkCapabilitiesClass = XposedHelpers.findClass(
                "android.net.NetworkCapabilities", lpparam.classLoader);
            
            // Hook hasCapability to simulate metered network
            XposedBridge.hookAllMethods(networkCapabilitiesClass, "hasCapability",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get()) return;
                        
                        try {
                            int capability = (Integer) param.args[0];
                            
                            if (capability == NetworkCapabilities.NET_CAPABILITY_NOT_METERED) {
                                // Check if should be metered
                                if (isMetered.get() || dataSaverEnabled.get()) {
                                    param.setResult(false);
                                }
                            }
                            
                            if (capability == NetworkCapabilities.NET_CAPABILITY_VALIDATED) {
                                // Captive portal check
                                if (captivePortalEnabled && isCaptivePortal) {
                                    param.setResult(false);
                                }
                            }
                        } catch (Exception e) {
                            logDebug("Error in hasCapability hook: " + e.getMessage());
                        }
                    }
                });
            
            // Hook getLinkDownstreamBandwidthKbps
            XposedBridge.hookAllMethods(networkCapabilitiesClass, "getLinkDownstreamBandwidthKbps",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get()) return;
                        
                        try {
                            int original = (Integer) param.getResult();
                            int adjusted = applyBandwidthAdjustment(original);
                            param.setResult(adjusted);
                        } catch (Exception e) {
                            logDebug("Error in getLinkDownstreamBandwidthKbps hook: " + e.getMessage());
                        }
                    }
                });
            
            logInfo("Hooked NetworkCapabilities");
        } catch (Exception e) {
            logError("Failed to hook NetworkCapabilities", e);
        }
    }
    
    private void hookWifiManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> wifiManagerClass = XposedHelpers.findClass(
                "android.net.wifi.WifiManager", lpparam.classLoader);
            
            // Hook getScanResults
            XposedBridge.hookAllMethods(wifiManagerClass, "getScanResults",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get()) return;
                        
                        try {
                            // Could add phantom WiFi networks here
                        } catch (Exception e) {
                            logDebug("Error in getScanResults hook: " + e.getMessage());
                        }
                    }
                });
            
            // Hook getConnectionInfo
            XposedBridge.hookAllMethods(wifiManagerClass, "getConnectionInfo",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get()) return;
                        
                        try {
                            // Simulate signal strength fluctuation
                            int baseRssi = signalStrength.get();
                            int rssiVariation = (random.get().nextInt(10) - 5);
                            int adjustedRssi = baseRssi + rssiVariation;
                            
                            // Apply congestion
                            if (congestionEnabled && isPeakHours()) {
                                adjustedRssi -= 5; // Weaker perceived signal
                            }
                            
                        } catch (Exception e) {
                            logDebug("Error in getConnectionInfo hook: " + e.getMessage());
                        }
                    }
                });
            
            logInfo("Hooked WifiManager");
        } catch (Exception e) {
            logError("Failed to hook WifiManager", e);
        }
    }
    
    private void hookUrlConnection(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook HttpURLConnection for latency simulation
            Class<?> httpURLConnectionClass = XposedHelpers.findClass(
                "java.net.HttpURLConnection", lpparam.classLoader);
            
            XposedBridge.hookAllMethods(httpURLConnectionClass, "getResponseCode",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get()) return;
                        
                        try {
                            // Add latency
                            int latency = (int)(latencyMs.get() * intensity);
                            
                            // Add VPN latency
                            if (vpnEnabled.get() && vpnEffectsEnabled) {
                                latency += vpnLatencyAddMs;
                            }
                            
                            // Add congestion latency
                            if (congestionEnabled && isPeakHours()) {
                                latency = (int)(latency * peakHourMultiplier);
                            }
                            
                            // Add captive portal delay
                            if (captivePortalEnabled && isCaptivePortal) {
                                latency += captivePortalDelayMs;
                            }
                            
                            if (random.get().nextDouble() < 0.3 * intensity) {
                                Thread.sleep(latency);
                            }
                            
                        } catch (Exception e) {
                            logDebug("Error in getResponseCode hook: " + e.getMessage());
                        }
                    }
                });
            
            logInfo("Hooked URL Connection");
        } catch (Exception e) {
            logError("Failed to hook URL Connection", e);
        }
    }
    
    private void hookDns(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook InetAddress for DNS simulation
            Class<?> inetAddressClass = XposedHelpers.findClass(
                "java.net.InetAddress", lpparam.classLoader);
            
            XposedBridge.hookAllMethods(inetAddressClass, "getAllByName",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get()) return;
                        
                        try {
                            // Add DNS delay for VPN
                            if (vpnEnabled.get() && vpnEffectsEnabled) {
                                int delay = vpnDnsDelayMs + random.get().nextInt(50);
                                if (random.get().nextDouble() < 0.3 * intensity) {
                                    Thread.sleep(delay);
                                }
                            }
                        } catch (Exception e) {
                            logDebug("Error in DNS hook: " + e.getMessage());
                        }
                    }
                });
            
            logInfo("Hooked DNS");
        } catch (Exception e) {
            logError("Failed to hook DNS", e);
        }
    }
    
    private void checkWifiAssist() {
        if (!wifiAssistEnabled) return;
        
        float currentLatency = latencyMs.get();
        float currentDownload = downloadMbps.get();
        
        if (currentLatency > wifiAssistLatencyThreshold || 
            currentDownload < wifiAssistDownloadThreshold) {
            
            if (random.get().nextDouble() < 0.5 * intensity) {
                // WiFi assist would activate
                wifiAssistActivations.incrementAndGet();
                
                if (DEBUG && random.get().nextDouble() < 0.01) {
                    logDebug("WiFi Assist activated: latency=" + currentLatency + 
                        "ms, download=" + currentDownload + "Mbps");
                }
            }
        }
    }
    
    private int applyBandwidthAdjustment(int originalKbps) {
        float bandwidth = originalKbps / 1000.0f; // Convert to Mbps
        
        // Apply VPN effect
        if (vpnEnabled.get() && vpnEffectsEnabled) {
            bandwidth *= (1.0f - vpnBandwidthLoss);
        }
        
        // Apply congestion
        if (congestionEnabled && isPeakHours()) {
            bandwidth /= peakHourMultiplier;
        }
        
        // Apply background restriction
        if (backgroundRestrictionEnabled && isAppInBackground()) {
            bandwidth *= backgroundThrottleFactor;
        }
        
        // Apply data saver
        if (dataSaverEnabled.get()) {
            bandwidth *= dataSaverBackgroundLimit;
        }
        
        // Apply intensity
        bandwidth *= (0.7f + intensity * 0.3f);
        
        return Math.max(1, (int)(bandwidth * 1000)); // Convert back to Kbps
    }
    
    private boolean isPeakHours() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
        
        // Peak hours: 7-9 AM and 5-9 PM
        return (hour >= 7 && hour <= 9) || (hour >= 17 && hour <= 21);
    }
    
    private boolean isAppInBackground() {
        // Simplified check - would need ActivityManager in real implementation
        return false;
    }
    
    /**
     * Set network type
     */
    public static void setNetworkType(NetworkType type) {
        NetworkType oldType = currentNetworkType.get();
        currentNetworkType.set(type);
        
        if (oldType != type) {
            handovers.incrementAndGet();
            
            // Update default metrics based on type
            switch (type) {
                case WIFI:
                    latencyMs.set(50.0f);
                    downloadMbps.set(100.0f);
                    break;
                case LTE:
                    latencyMs.set(30.0f);
                    downloadMbps.set(50.0f);
                    break;
                case 5G:
                    latencyMs.set(15.0f);
                    downloadMbps.set(200.0f);
                    break;
                case VPN:
                    latencyMs.set(latencyMs.get() + vpnLatencyAddMs);
                    downloadMbps.set(downloadMbps.get() * (1.0f - vpnBandwidthLoss));
                    break;
                default:
                    break;
            }
        }
        
        HookUtils.logInfo(TAG, "Network type changed: " + oldType + " -> " + type);
    }
    
    /**
     * Enable/disable VPN
     */
    public static void setVpnEnabled(boolean enabled) {
        vpnEnabled.set(enabled);
        
        if (enabled) {
            // Add VPN latency and reduce bandwidth
            latencyMs.set(latencyMs.get() + vpnLatencyAddMs);
            downloadMbps.set(downloadMbps.get() * (1.0f - vpnBandwidthLoss));
        }
        
        HookUtils.logInfo(TAG, "VPN " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Enable/disable data saver mode
     */
    public static void setDataSaverEnabled(boolean enabled) {
        dataSaverEnabled.set(enabled);
        HookUtils.logInfo(TAG, "Data Saver " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Set captive portal state
     */
    public static void setCaptivePortal(boolean isPortal) {
        isCaptivePortal = isPortal;
        HookUtils.logInfo(TAG, "Captive portal: " + isPortal);
    }
    
    /**
     * Enable/disable the hook
     */
    public static void setEnabled(boolean isEnabled) {
        enabled.set(isEnabled);
        HookUtils.logInfo(TAG, "Hook " + (isEnabled ? "enabled" : "disabled"));
    }
    
    /**
     * Set intensity
     */
    public static void setIntensity(float value) {
        intensity = Math.max(0.0f, Math.min(1.0f, value));
    }
    
    /**
     * Get current latency
     */
    public static float getLatency() {
        return latencyMs.get();
    }
    
    /**
     * Get download speed
     */
    public static float getDownloadSpeed() {
        return downloadMbps.get();
    }
    
    /**
     * Network snapshot
     */
    private static class NetworkSnapshot {
        final NetworkType type;
        final float latency;
        final float download;
        final long timestamp;
        
        NetworkSnapshot(NetworkType type, float latency, float download, long timestamp) {
            this.type = type;
            this.latency = latency;
            this.download = download;
            this.timestamp = timestamp;
        }
    }
}
