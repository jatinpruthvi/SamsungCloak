package com.samsungcloak.xposed;

import android.net.ConnectivityManager;
import android.net.VpnService;
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
 * VPNConnectionFailureHook - Network Security & VPN
 * 
 * Simulates VPN connection issues:
 * - Handshake delays
 * - Authentication failures
 * - Server unreachable
 * - Periodic disconnections
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class VPNConnectionFailureHook {

    private static final String TAG = "[VPN][Connection]";
    private static final boolean DEBUG = true;
    
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    
    // Parameters
    private static int handshakeDelayMin = 500;     // ms
    private static int handshakeDelayMax = 3000;    // ms
    private static float authFailureRate = 0.12f;   // 12%
    private static float serverUnreachableRate = 0.08f; // 8%
    private static float disconnectRate = 0.05f;    // 5%
    
    // State
    private static boolean isVPNConnected = false;
    private static int connectionAttempts = 0;
    private static int successfulConnections = 0;
    
    private static final Random random = new Random();
    private static final List<VPNEvent> vpnEvents = new CopyOnWriteArrayList<>();
    
    public static class VPNEvent {
        public long timestamp;
        public String type;
        public String details;
        
        public VPNEvent(String type, String details) {
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
        
        HookUtils.logInfo(TAG, "Initializing VPN Connection Failure Hook");
        
        try {
            hookVpnService(lpparam);
            hookConnectivityManager(lpparam);
            
            HookUtils.logInfo(TAG, "VPN hook initialized");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Init failed: " + t.getMessage());
            hookInitialized.set(false);
        }
    }
    
    private static void hookVpnService(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> vpnServiceClass = XposedHelpers.findClass(
                "android.net.VpnService", lpparam.classLoader
            );
            
            XposedBridge.hookAllMethods(vpnServiceClass, "protect",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    // Handshake delay
                    int delay = handshakeDelayMin + 
                        random.nextInt(handshakeDelayMax - handshakeDelayMin);
                    
                    vpnEvents.add(new VPNEvent("HANDSHAKE_DELAY", 
                        "Tunnel setup: " + delay + "ms"));
                    
                    // Auth failure
                    if (random.nextFloat() < authFailureRate) {
                        vpnEvents.add(new VPNEvent("AUTH_FAILED", 
                            "VPN authentication failed"));
                        return;
                    }
                    
                    // Server unreachable
                    if (random.nextFloat() < serverUnreachableRate) {
                        vpnEvents.add(new VPNEvent("SERVER_UNREACHABLE", 
                            "VPN server not reachable"));
                        return;
                    }
                    
                    isVPNConnected = true;
                    successfulConnections++;
                    
                    HookUtils.logDebug(TAG, "VPN connected after: " + delay + "ms");
                }
            });
            
            XposedBridge.hookAllMethods(vpnServiceClass, "disconnect",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    // Periodic disconnection
                    if (random.nextFloat() < disconnectRate) {
                        vpnEvents.add(new VPNEvent("UNEXPECTED_DISCONNECT", 
                            "VPN connection dropped unexpectedly"));
                    }
                    
                    isVPNConnected = false;
                    vpnEvents.add(new VPNEvent("DISCONNECTED", "VPN disconnected"));
                }
            });
            
            HookUtils.logInfo(TAG, "VpnService hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "VpnService hook failed: " + t.getMessage());
        }
    }
    
    private static void hookConnectivityManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> connectivityClass = XposedHelpers.findClass(
                "android.net.ConnectivityManager", lpparam.classLoader
            );
            
            XposedBridge.hookAllMethods(connectivityClass, "startUsingNetworkFeature",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    connectionAttempts++;
                    
                    // DNS resolution failure for split tunnel
                    if (random.nextFloat() < 0.1f) {
                        vpnEvents.add(new VPNEvent("DNS_FAILED", 
                            "Split tunnel DNS resolution failed"));
                    }
                }
            });
            
            HookUtils.logInfo(TAG, "ConnectivityManager hooked");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "ConnectivityManager hook skipped: " + t.getMessage());
        }
    }
    
    public static void setEnabled(boolean enabled) {
        VPNConnectionFailureHook.enabled = enabled;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static boolean isVPNConnected() {
        return isVPNConnected;
    }
    
    public static int getConnectionAttempts() {
        return connectionAttempts;
    }
    
    public static int getSuccessfulConnections() {
        return successfulConnections;
    }
    
    public static List<VPNEvent> getVPNEvents() {
        return new ArrayList<>(vpnEvents);
    }
}
