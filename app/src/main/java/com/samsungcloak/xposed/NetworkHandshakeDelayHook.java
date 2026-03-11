package com.samsungcloak.xposed;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.Uri;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Random;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hook #15: Network Handshake & Connection Delay Simulation
 * 
 * Adds realistic delays for:
 * - TCP three-way handshake
 * - TLS/SSL negotiation
 * - DNS resolution
 * - Connection retry simulation
 * 
 * Extends existing NetworkJitterHook with connection-level delays.
 * 
 * Targets: SM-A125U (Android 10/11)
 */
public class NetworkHandshakeDelayHook {

    private static final String TAG = "[HumanInteraction][NetworkHandshake]";
    private static final boolean DEBUG = true;

    private static boolean enabled = true;
    private static int tcpDelayMs = 40;
    private static int tlsDelayMs = 120;
    private static int dnsDelayMs = 25;
    private static float retryProbability = 0.05f;

    private static final Random random = new Random();
    private static long lastConnectionTime = 0;
    private static int connectionCount = 0;

    // Network type dependent delays
    private static final int[][] NETWORK_DELAYS = {
        {15, 50, 10},   // WiFi 5GHz: [tcp, tls, dns]
        {25, 80, 15},   // WiFi 2.4GHz: [tcp, tls, dns]
        {40, 120, 25},  // LTE: [tcp, tls, dns]
        {80, 200, 50},  // Edge: [tcp, tls, dns]
        {150, 300, 80}  // Poor connection: [tcp, tls, dns]
    };

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        HookUtils.logInfo(TAG, "Initializing Network Handshake Delay Hook");

        try {
            hookNetworkConnections(lpparam);
            hookDnsResolution(lpparam);
            hookTlsHandshake(lpparam);
            HookUtils.logInfo(TAG, "Network Handshake Delay Hook initialized successfully");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize hook", e);
        }
    }

    private static void hookNetworkConnections(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook URLConnection for HTTP/HTTPS connections
            Class<?> urlConnectionClass = XposedHelpers.findClass(
                "java.net.URLConnection", lpparam.classLoader);

            // Hook connect() method for TCP connection delays
            XposedBridge.hookAllMethods(urlConnectionClass, "connect",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    // Add TCP handshake delay
                    int delay = getTcpDelay();
                    addConnectionDelay(delay);

                    connectionCount++;
                    lastConnectionTime = System.currentTimeMillis();

                    if (DEBUG && random.nextFloat() < 0.05f) {
                        HookUtils.logDebug(TAG, "TCP connection delay: " + delay + "ms");
                    }
                }
            });

            // Hook getInputStream for additional connection simulation
            XposedBridge.hookAllMethods(urlConnectionClass, "getInputStream",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    // Add delay for connection establishment
                    int delay = getTcpDelay() / 2;
                    addConnectionDelay(delay);
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked URLConnection methods");

        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook network connections", e);
        }
    }

    private static void hookDnsResolution(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook InetAddress.getAllByName for DNS resolution delays
            Class<?> inetAddressClass = XposedHelpers.findClass(
                "java.net.InetAddress", lpparam.classLoader);

            XposedBridge.hookAllMethods(inetAddressClass, "getAllByName",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    String hostname = (String) param.args[0];
                    
                    // Skip if it's an IP address
                    if (hostname == null || hostname.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
                        return;
                    }

                    // Add DNS resolution delay
                    int delay = getDnsDelay();
                    addConnectionDelay(delay);

                    if (DEBUG && random.nextFloat() < 0.1f) {
                        HookUtils.logDebug(TAG, "DNS resolution delay for " + 
                            hostname + ": " + delay + "ms");
                    }
                }
            });

            // Hook getByName for single address lookup
            XposedBridge.hookAllMethods(inetAddressClass, "getByName",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    String hostname = (String) param.args[0];
                    
                    // Skip if it's an IP address
                    if (hostname == null || hostname.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
                        return;
                    }

                    // Add DNS resolution delay (typically faster)
                    int delay = getDnsDelay() / 2;
                    addConnectionDelay(delay);
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked DNS resolution methods");

        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook DNS resolution", e);
        }
    }

    private static void hookTlsHandshake(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook SSLSocketFactory for TLS handshake delays
            Class<?> sslSocketFactoryClass = XposedHelpers.findClass(
                "javax.net.ssl.SSLSocketFactory", lpparam.classLoader);

            // Hook createSocket methods
            String[] socketMethods = {"createSocket", "createSocketImpl"};
            for (String methodName : socketMethods) {
                try {
                    XposedBridge.hookAllMethods(sslSocketFactoryClass, methodName,
                        new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) 
                                throws Throwable {
                            if (!enabled) return;

                            // Add TLS handshake delay
                            int delay = getTlsDelay();
                            
                            // Simulate connection retry (5% chance)
                            if (random.nextFloat() < retryProbability) {
                                // Brief "connection drop" simulation
                                Thread.sleep(delay / 3);
                                throw new IOException("Simulated TLS connection drop and retry");
                            }

                            addConnectionDelay(delay);

                            if (DEBUG && random.nextFloat() < 0.05f) {
                                HookUtils.logDebug(TAG, "TLS handshake delay: " + delay + "ms");
                            }
                        }
                    });
                } catch (Exception e) {
                    // Method might not exist, continue to next
                }
            }

            // Hook Socket connect for additional TCP delay
            Class<?> socketClass = XposedHelpers.findClass(
                "java.net.Socket", lpparam.classLoader);

            XposedBridge.hookAllMethods(socketClass, "connect",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    // Add TCP connection delay
                    int delay = getTcpDelay();
                    addConnectionDelay(delay);
                }
            });

            // Hook Socket constructor with InetAddress
            XposedBridge.hookAllConstructors(socketClass,
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    // Add small delay for socket initialization
                    int delay = random.nextInt(10) + 5;
                    addConnectionDelay(delay);
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked TLS/SSL socket methods");

        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook TLS handshake", e);
        }
    }

    private static void addConnectionDelay(int baseDelay) throws InterruptedException {
        // Add variance (±30%)
        float variance = 0.7f + random.nextFloat() * 0.6f;
        int actualDelay = (int) (baseDelay * variance);

        // Add small random jitter
        actualDelay += random.nextInt(10);

        // Apply delay
        Thread.sleep(actualDelay);
    }

    private static int getTcpDelay() {
        // Determine network type and return appropriate delay
        // In production, would query actual network state
        int networkType = getCurrentNetworkType();
        return NETWORK_DELAYS[networkType][0];
    }

    private static int getTlsDelay() {
        int networkType = getCurrentNetworkType();
        return NETWORK_DELAYS[networkType][1];
    }

    private static int getDnsDelay() {
        int networkType = getCurrentNetworkType();
        return NETWORK_DELAYS[networkType][2];
    }

    private static int getCurrentNetworkType() {
        // Simulate network type based on recent activity
        // In production, would query ConnectivityManager
        
        long timeSinceLastConnection = System.currentTimeMillis() - lastConnectionTime;
        
        if (connectionCount < 3) {
            // Initial connections - assume WiFi
            return 0; // WiFi 5GHz
        } else if (timeSinceLastConnection > 300000) {
            // No activity for 5 minutes - assume poor connection
            return 4; // Poor
        } else if (random.nextFloat() < 0.7f) {
            return 0; // WiFi
        } else {
            return 2; // LTE
        }
    }
}
