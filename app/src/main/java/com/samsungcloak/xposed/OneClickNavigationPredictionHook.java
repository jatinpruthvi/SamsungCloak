package com.samsungcloak.xposed;

import android.location.Location;
import android.location.LocationManager;
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
 * OneClickNavigationPredictionHook - Navigation Prediction Simulation
 * 
 * Simulates navigation app prediction issues:
 * - Wrong destination suggestions (15%)
 * - Route recalculation delays (2-5s)
 * - GPS signal loss in tunnels (3-10s)
 * - "Arrived" false positives
 * - Position errors near intersections
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class OneClickNavigationPredictionHook {

    private static final String TAG = "[Navigation][GPS]";
    private static final boolean DEBUG = true;
    
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    
    // Prediction parameters
    private static float wrongPredictionRate = 0.15f;    // 15%
    private static int recalculationDelayMin = 2000;     // ms
    private static int recalculationDelayMax = 5000;     // ms
    private static int tunnelGpsLossMin = 3000;          // ms
    private static int tunnelGpsLossMax = 10000;         // ms
    private static int positionErrorMeters = 50;         // 50m error
    private static float falseArrivalRate = 0.05f;       // 5%
    
    // State
    private static boolean isNavigating = false;
    private static boolean inTunnel = false;
    static boolean isNearIntersection = false;
    private static String currentDestination = null;
    private static long lastRecalculationTime = 0;
    private static int gpsSignalStrength = 100; // 0-100
    
    private static final Random random = new Random();
    private static final List<NavigationEvent> navHistory = new CopyOnWriteArrayList<>();
    
    public static class NavigationEvent {
        public long timestamp;
        public String type;
        public String details;
        
        public NavigationEvent(String type, String details) {
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
        
        HookUtils.logInfo(TAG, "Initializing Navigation Prediction Hook");
        
        try {
            hookFusedLocationProvider(lpparam);
            hookLocationManager(lpparam);
            hookMapsNavigation(lpparam);
            
            HookUtils.logInfo(TAG, "Navigation prediction hook initialized");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Init failed: " + t.getMessage());
            hookInitialized.set(false);
        }
    }
    
    private static void hookFusedLocationProvider(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> fusedLocationClass = XposedHelpers.findClass(
                "com.google.android.gms.location.FusedLocationProviderClient", 
                lpparam.classLoader
            );
            
            // Hook requestLocationUpdates
            XposedBridge.hookAllMethods(fusedLocationClass, "requestLocationUpdates",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    isNavigating = true;
                    
                    navHistory.add(new NavigationEvent("NAVIGATION_START", 
                        "Location updates requested"));
                    
                    HookUtils.logDebug(TAG, "Navigation started");
                }
            });
            
            HookUtils.logInfo(TAG, "FusedLocationProvider hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "FusedLocationProvider hook failed: " + t.getMessage());
        }
    }
    
    private static void hookLocationManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> locationManagerClass = XposedHelpers.findClass(
                "android.location.LocationManager", lpparam.classLoader
            );
            
            // Hook getLastKnownLocation
            XposedBridge.hookAllMethods(locationManagerClass, "getLastKnownLocation",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    // Apply GPS errors
                    Location original = (Location) param.getResult();
                    if (original != null) {
                        Location modified = applyGpsError(original);
                        param.setResult(modified);
                    }
                }
            });
            
            // Hook requestLocationUpdates
            XposedBridge.hookAllMethods(locationManagerClass, "requestLocationUpdates",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    // Check for tunnel GPS loss
                    if (inTunnel && random.nextFloat() < 0.5f) {
                        int lossDuration = tunnelGpsLossMin + 
                            random.nextInt(tunnelGpsLossMax - tunnelGpsLossMin);
                        
                        gpsSignalStrength = 0;
                        
                        navHistory.add(new NavigationEvent("GPS_LOST", 
                            "Tunnel GPS loss: " + lossDuration + "ms"));
                        
                        HookUtils.logDebug(TAG, "Simulated tunnel GPS loss: " + lossDuration + "ms");
                    }
                }
            });
            
            HookUtils.logInfo(TAG, "LocationManager hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "LocationManager hook failed: " + t.getMessage());
        }
    }
    
    private static void hookMapsNavigation(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> navigationClass = XposedHelpers.findClass(
                "com.google.android.apps.navigation.NavigationActivity", 
                lpparam.classLoader
            );
            
            // Hook onNavigationStarted
            XposedBridge.hookAllMethods(navigationClass, "onNavigationStarted",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    isNavigating = true;
                    
                    // Wrong destination prediction
                    if (random.nextFloat() < wrongPredictionRate) {
                        navHistory.add(new NavigationEvent("WRONG_PREDICTION", 
                            "Wrong destination suggested"));
                        
                        HookUtils.logDebug(TAG, "Wrong destination predicted");
                    }
                }
            });
            
            HookUtils.logInfo(TAG, "Navigation activity hooked");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "Navigation hook skipped: " + t.getMessage());
        }
    }
    
    /**
     * Apply GPS error based on context
     */
    private static Location applyGpsError(Location original) {
        if (original == null) return null;
        
        Location modified = new Location(original);
        
        // Apply position error near intersections
        if (isNearIntersection) {
            double errorLat = (random.nextDouble() - 0.5) * positionErrorMeters * 0.00001;
            double errorLng = (random.nextDouble() - 0.5) * positionErrorMeters * 0.00001;
            
            modified.setLatitude(modified.getLatitude() + errorLat);
            modified.setLongitude(modified.getLongitude() + errorLng);
            
            navHistory.add(new NavigationEvent("POSITION_ERROR", 
                "Near intersection: " + positionErrorMeters + "m error"));
        }
        
        // Apply tunnel error
        if (inTunnel && gpsSignalStrength == 0) {
            modified.setAccuracy(100); // Poor accuracy
        }
        
        return modified;
    }
    
    /**
     * Simulate route recalculation
     */
    public static boolean shouldRecalculate() {
        if (!isNavigating) return false;
        
        long now = System.currentTimeMillis();
        if (now - lastRecalculationTime < 30000) { // Max once per 30s
            return false;
        }
        
        int delay = recalculationDelayMin + 
            random.nextInt(recalculationDelayMax - recalculationDelayMin);
        
        lastRecalculationTime = now;
        
        navHistory.add(new NavigationEvent("RECALCULATING", 
            "Delay: " + delay + "ms"));
        
        HookUtils.logDebug(TAG, "Route recalculation: " + delay + "ms");
        
        return true;
    }
    
    /**
     * Check for false arrival
     */
    public static boolean shouldSimulateFalseArrival() {
        if (!isNavigating) return false;
        
        return random.nextFloat() < falseArrivalRate;
    }
    
    // ========== Configuration ==========
    
    public static void setEnabled(boolean enabled) {
        OneClickNavigationPredictionHook.enabled = enabled;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void setNavigating(boolean navigating) {
        isNavigating = navigating;
    }
    
    public static void setInTunnel(boolean inTunnel) {
        OneClickNavigationPredictionHook.inTunnel = inTunnel;
        
        if (inTunnel) {
            navHistory.add(new NavigationEvent("TUNNEL_ENTER", "Entered tunnel"));
        } else {
            navHistory.add(new NavigationEvent("TUNNEL_EXIT", "Exited tunnel"));
        }
    }
    
    public static void setNearIntersection(boolean near) {
        isNearIntersection = near;
    }
    
    public static void setWrongPredictionRate(float rate) {
        wrongPredictionRate = Math.max(0, Math.min(1, rate));
    }
    
    public static void setPositionErrorMeters(int meters) {
        positionErrorMeters = Math.max(10, Math.min(500, meters));
    }
    
    public static int getGpsSignalStrength() {
        return gpsSignalStrength;
    }
    
    public static List<NavigationEvent> getNavHistory() {
        return new ArrayList<>(navHistory);
    }
}
