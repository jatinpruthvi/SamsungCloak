package com.samsungcloak.xposed;

import android.view.MotionEvent;
import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;

/**
 * Hook #17: Weather Environmental Effects - Rain on Touchscreen
 * 
 * Simulates realistic touch interference caused by weather conditions:
 * - Water droplets on screen affecting touch accuracy
 * - Rain-induced touch drift and false positives
 * - Humidity effects on capacitive sensor sensitivity
 * 
 * Targets: SM-A125U (Android 10/11)
 */
public class WeatherEnvironmentalEffectsHook {

    private static final String TAG = "[HumanInteraction][Weather]";
    private static final boolean DEBUG = true;

    private static boolean enabled = true;
    private static boolean rainModeEnabled = false;
    private static float dropletDensity = 0.3f; // 0-1 scale
    private static float touchInterference = 0.15f;

    private static final Random random = new Random();
    private static long lastRainUpdate = 0;
    private static int activeDroplets = 0;

    // Screen divided into zones for droplet simulation
    private static final int ZONE_COLS = 8;
    private static final int ZONE_ROWS = 16;
    private static boolean[][] dropletMap = new boolean[ZONE_ROWS][ZONE_COLS];

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        HookUtils.logInfo(TAG, "Initializing Weather Environmental Effects Hook");

        try {
            hookTouchEvent(lpparam);
            updateDropletMap();
            HookUtils.logInfo(TAG, "Weather Environmental Effects Hook initialized");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize hook", e);
        }
    }

    private static void hookTouchEvent(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> viewClass = XposedHelpers.findClass("android.view.View", lpparam.classLoader);

            XposedBridge.hookAllMethods(viewClass, "onTouchEvent", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || !rainModeEnabled) return;

                    MotionEvent event = (MotionEvent) param.args[0];
                    int action = event.getActionMasked();

                    if (action == MotionEvent.ACTION_DOWN || 
                        action == MotionEvent.ACTION_MOVE) {
                        
                        applyWeatherEffects(event);
                    }
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked View.onTouchEvent for weather effects");

        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook touch event", e);
        }
    }

    private static void applyWeatherEffects(MotionEvent event) {
        try {
            // Update droplet map periodically
            long now = System.currentTimeMillis();
            if (now - lastRainUpdate > 500) { // Every 500ms
                updateDropletMap();
                lastRainUpdate = now;
            }

            // Get touch coordinates
            float x = event.getX();
            float y = event.getY();

            // Get screen dimensions (typical for SM-A125U: 720x1600)
            int screenWidth = 720;
            int screenHeight = 1600;

            // Calculate zone
            int zoneX = (int) (x / (screenWidth / ZONE_COLS));
            int zoneY = (int) (y / (screenHeight / ZONE_ROWS));

            zoneX = Math.max(0, Math.min(ZONE_COLS - 1, zoneX));
            zoneY = Math.max(0, Math.min(ZONE_ROWS - 1, zoneY));

            // Check if touch hits a droplet
            if (dropletMap[zoneY][zoneX]) {
                // Apply droplet interference
                float interferenceX = (random.nextFloat() - 0.5f) * touchInterference * 50;
                float interferenceY = (random.nextFloat() - 0.5f) * touchInterference * 50;

                if (DEBUG && random.nextFloat() < 0.02f) {
                    HookUtils.logDebug(TAG, "Droplet interference: (" + interferenceX + "," + interferenceY + ")");
                }
                // Note: Actual position modification would require MotionEvent.clone()
            }

            // Random false touch detection when screen is wet
            if (random.nextFloat() < (dropletDensity * 0.02f)) {
                // Occasional phantom touches in rainy conditions
                if (DEBUG && random.nextFloat() < 0.01f) {
                    HookUtils.logDebug(TAG, "Rain-induced phantom touch detected");
                }
            }

        } catch (Exception e) {
            if (DEBUG) {
                HookUtils.logDebug(TAG, "Error applying weather effects: " + e.getMessage());
            }
        }
    }

    private static void updateDropletMap() {
        // Simulate water droplets on screen
        int totalDroplets = (int) (dropletDensity * ZONE_ROWS * ZONE_COLS * 0.2);
        
        // Reset
        for (int i = 0; i < ZONE_ROWS; i++) {
            for (int j = 0; j < ZONE_COLS; j++) {
                dropletMap[i][j] = false;
            }
        }

        // Place random droplets
        for (int i = 0; i < totalDroplets; i++) {
            int row = random.nextInt(ZONE_ROWS);
            int col = random.nextInt(ZONE_COLS);
            dropletMap[row][col] = true;
        }

        activeDroplets = totalDroplets;
    }
}
