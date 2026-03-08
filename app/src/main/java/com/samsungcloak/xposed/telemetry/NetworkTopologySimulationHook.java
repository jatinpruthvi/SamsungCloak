package com.samsungcloak.xposed.telemetry;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkTopologySimulationHook {
    private static final String LOG_TAG = "SamsungCloak.NetworkTopology";
    private static boolean initialized = false;

    private static Random random = new Random();
    private static ConnectionProfile currentProfile = ConnectionProfile.WIFI_RESIDENTIAL;
    private static final Map<String, List<Long>> dnsCache = new ConcurrentHashMap<>();
    private static final Map<String, Long> dnsTtlCache = new ConcurrentHashMap<>();

    public enum ConnectionProfile {
        WIFI_RESIDENTIAL("Residential WiFi", 15, 30, 0.02, 0.05, 500),
        MOBILE_4G("4G Mobile", 40, 80, 0.05, 0.10, 800),
        MOBILE_5G("5G Mobile", 15, 25, 0.02, 0.05, 300),
        MOBILE_3G("3G Mobile", 100, 200, 0.10, 0.20, 1500),
        ETHERNET("Ethernet", 2, 5, 0.005, 0.01, 100),
        SATELLITE("Satellite", 500, 800, 0.15, 0.25, 3000),
        VPN("VPN Connection", 30, 60, 0.03, 0.08, 600);

        public final String displayName;
        public final int minLatencyMs;
        public final int maxLatencyMs;
        public final double packetLossRate;
        public final double jitterFactor;
        public final int typicalDnsMs;

        private ConnectionProfile(String displayName, int minLatency, int maxLatency,
                                  double packetLoss, double jitter, int dnsMs) {
            this.displayName = displayName;
            this.minLatencyMs = minLatency;
            this.maxLatencyMs = maxLatency;
            this.packetLossRate = packetLoss;
            this.jitterFactor = jitter;
            this.typicalDnsMs = dnsMs;
        }
    }

    private static class LatencySnapshot {
        long baseLatency;
        long jitteredLatency;
        long timestamp;
        boolean packetDropped;

        LatencySnapshot(long base, long jitter, long time, boolean dropped) {
            this.baseLatency = base;
            this.jitteredLatency = jitter;
            this.timestamp = time;
            this.packetDropped = dropped;
        }
    }

    private static final Map<String, List<LatencySnapshot>> latencyHistory = new ConcurrentHashMap<>();
    private static long sessionStartTime = System.currentTimeMillis();

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (initialized) {
            XposedBridge.log(LOG_TAG + " already initialized");
            return;
        }

        try {
            hookNetworkStack(lpparam);
            hookDnsResolution(lpparam);
            hookHttpURLConnection(lpparam);
            hookOkHttp(lpparam);
            initialized = true;
            XposedBridge.log(LOG_TAG + " initialized successfully with profile: " + currentProfile.displayName);
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " Failed to initialize: " + e.getMessage());
        }
    }

    private static void hookNetworkStack(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> networkStackClass = XposedHelpers.findClass(
                "android.net.NetworkStack", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(networkStackClass, "getActiveNetwork", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    applyNetworkCharacteristics();
                }
            });

            XposedBridge.log(LOG_TAG + " Hooked NetworkStack");
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " Failed to hook NetworkStack: " + e.getMessage());
        }
    }

    private static void hookDnsResolution(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> dnsResolverClass = XposedHelpers.findClass(
                "android.net.DnsResolver", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(dnsResolverClass, "resolve", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        String hostname = (String) param.args[0];
                        if (hostname != null) {
                            long dnsLatency = simulateDnsResolution(hostname);
                            XposedBridge.log(LOG_TAG + " DNS resolution for " + hostname + ": " + dnsLatency + "ms");
                        }
                    } catch (Exception e) {
                    }
                }
            });

            XposedBridge.hookAllMethods(dnsResolverClass, "query", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        String hostname = extractHostnameFromQuery(param.args);
                        if (hostname != null) {
                            long dnsLatency = simulateDnsResolution(hostname);
                            XposedBridge.log(LOG_TAG + " DNS query for " + hostname + ": " + dnsLatency + "ms");
                        }
                    } catch (Exception e) {
                    }
                }
            });

            try {
                Class<?> inetAddressClass = XposedHelpers.findClass(
                    "java.net.InetAddress", lpparam.classLoader
                );

                XposedBridge.hookAllMethods(inetAddressClass, "getByName", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            String hostname = (String) param.args[0];
                            if (hostname != null && !isIpAddress(hostname)) {
                                simulateNetworkDelay(currentProfile.typicalDnsMs);
                            }
                        } catch (Exception e) {
                        }
                    }
                });
            } catch (Exception e) {
                XposedBridge.log(LOG_TAG + " Could not hook java.net.InetAddress: " + e.getMessage());
            }

            XposedBridge.log(LOG_TAG + " Hooked DNS resolution");
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " Failed to hook DNS: " + e.getMessage());
        }
    }

    private static void hookHttpURLConnection(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> httpUrlConnectionClass = XposedHelpers.findClass(
                "com.android.okhttp.internal.huc.HttpURLConnectionImpl", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(httpUrlConnectionClass, "getOutputStream", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        applyLatencyJitter();
                    } catch (Exception e) {
                    }
                }
            });

            XposedBridge.hookAllMethods(httpUrlConnectionClass, "getInputStream", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        applyLatencyJitter();
                    } catch (Exception e) {
                    }
                }
            });

            XposedBridge.log(LOG_TAG + " Hooked HttpURLConnection");
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " Failed to hook HttpURLConnection: " + e.getMessage());
        }
    }

    private static void hookOkHttp(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> okHttpClientClass = XposedHelpers.findClass(
                "okhttp3.OkHttpClient", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(okHttpClientClass, "newCall", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        Object call = param.getResult();
                        if (call != null) {
                            hookCallExecution(call);
                        }
                    } catch (Exception e) {
                    }
                }
            });

            XposedBridge.log(LOG_TAG + " Hooked OkHttpClient");
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " Failed to hook OkHttp: " + e.getMessage());
        }
    }

    private static void hookCallExecution(Object call) {
        try {
            Class<?> callClass = call.getClass();
            XposedBridge.hookAllMethods(callClass, "execute", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    applyLatencyJitter();
                }
            });
        } catch (Exception e) {
        }
    }

    private static void applyNetworkCharacteristics() {
        if (shouldDropPacket()) {
            XposedBridge.log(LOG_TAG + " Simulated packet drop");
        }
    }

    private static long simulateDnsResolution(String hostname) {
        long baseLatency = currentProfile.typicalDnsMs;
        
        if (dnsCache.containsKey(hostname)) {
            List<Long> cachedTimes = dnsCache.get(hostname);
            if (!cachedTimes.isEmpty()) {
                long cachedLatency = cachedTimes.get(random.nextInt(cachedTimes.size()));
                return cachedLatency + (long) (random.nextGaussian() * 10);
            }
        }

        double jitterMultiplier = 1.0 + (random.nextDouble() * 0.4 - 0.2);
        long jitteredLatency = (long) (baseLatency * jitterMultiplier);
        
        if (random.nextDouble() < 0.05) {
            jitteredLatency += (long) (random.nextDouble() * 100);
        }

        dnsCache.computeIfAbsent(hostname, k -> new ArrayList<>()).add(jitteredLatency);
        
        if (dnsCache.get(hostname).size() > 20) {
            dnsCache.get(hostname).remove(0);
        }

        return Math.max(10, jitteredLatency);
    }

    private static void applyLatencyJitter() {
        long baseLatency = currentProfile.minLatencyMs + 
            random.nextInt(currentProfile.maxLatencyMs - currentProfile.minLatencyMs);
        
        double jitterMultiplier = 1.0 + (random.nextGaussian() * currentProfile.jitterFactor);
        long finalLatency = (long) (baseLatency * Math.abs(jitterMultiplier));
        
        if (shouldDropPacket()) {
            XposedBridge.log(LOG_TAG + " Packet drop simulated for connection");
            return;
        }

        long timeWindow = System.currentTimeMillis() % 10000;
        if (timeWindow < finalLatency) {
            try {
                Thread.sleep(finalLatency);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        recordLatency(baseLatency, finalLatency);
    }

    private static void simulateNetworkDelay(long delayMs) {
        double jitterMultiplier = 1.0 + (random.nextGaussian() * 0.1);
        long finalDelay = (long) (delayMs * Math.abs(jitterMultiplier));
        
        try {
            Thread.sleep(finalDelay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static boolean shouldDropPacket() {
        return random.nextDouble() < currentProfile.packetLossRate;
    }

    private static void recordLatency(long base, long jittered) {
        String key = "connection_" + (System.currentTimeMillis() / 1000);
        latencyHistory.computeIfAbsent(key, k -> new ArrayList<>())
            .add(new LatencySnapshot(base, jittered, System.currentTimeMillis(), shouldDropPacket()));
        
        if (latencyHistory.get(key).size() > 100) {
            latencyHistory.get(key).remove(0);
        }
    }

    private static String extractHostnameFromQuery(Object[] args) {
        if (args == null || args.length == 0) return null;
        for (Object arg : args) {
            if (arg instanceof String) {
                String str = (String) arg;
                if (str.contains(".") && !isIpAddress(str)) {
                    return str;
                }
            }
        }
        return null;
    }

    private static boolean isIpAddress(String hostname) {
        try {
            InetAddress.getByName(hostname);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void setConnectionProfile(ConnectionProfile profile) {
        currentProfile = profile;
        XposedBridge.log(LOG_TAG + " Profile changed to: " + profile.displayName);
    }

    public static ConnectionProfile getCurrentProfile() {
        return currentProfile;
    }

    public static Map<String, List<Long>> getDnsCache() {
        return new HashMap<>(dnsCache);
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
