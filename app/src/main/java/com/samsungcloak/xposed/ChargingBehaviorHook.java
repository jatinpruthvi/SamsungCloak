package com.samsungcloak.xposed;

import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.os.BatteryManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ChargingBehaviorHook - Realistic Charging Behavior Simulation
 *
 * Simulates realistic charging behaviors including charging patterns,
 * battery anxiety responses, and user preferences for charging timing.
 *
 * Novel Dimensions:
 * 1. Charging Pattern Behaviors - Overnight vs opportunistic charging
 * 2. Battery Anxiety Effects - When users decide to charge
 * 3. Charging Interruption Behaviors - Brief disconnects while moving device
 * 4. Night Charging Patterns - 68% charge overnight
 * 5. Fast vs Slow Charging Preferences - User preferences and constraints
 *
 * Real-World Grounding (HCI Studies):
 * - 68% of users charge overnight
 * - Average charge starts at 20-30% battery
 * - Fast charging used 75% of the time when available
 * - Charging sessions: 30-120 minutes average
 * - Battery anxiety threshold: 20-30% for most users
 *
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class ChargingBehaviorHook {

    private static final String TAG = "[Behavior][Charging]";
    private static final boolean DEBUG = true;
    private static final ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new Random());

    private static boolean enabled = true;

    // Charging behavior configuration
    private static boolean chargingSimulationEnabled = true;
    private static int batteryAnxietyThreshold = 25; // Start charging below 25%
    private static int targetChargeLevel = 85; // Most users unplug at 80-90%
    private static boolean preferFastCharging = true;
    private static boolean overnightChargingEnabled = true;

    // Charging session tracking
    private static final AtomicBoolean isCharging = new AtomicBoolean(false);
    private static final AtomicBoolean isFastCharging = new AtomicBoolean(false);
    private static final AtomicLong chargingStartTime = new AtomicLong(0);
    private static final AtomicInteger chargeStartLevel = new AtomicInteger(0);
    private static final AtomicInteger chargeSessionCount = new AtomicInteger(0);
    private static final AtomicLong totalChargingTimeMs = new AtomicLong(0);

    // Battery state simulation
    private static int currentBatteryLevel = 65; // Starting level
    private static int batteryDischargeRate = 5; // % per hour average
    private static long lastBatteryUpdateTime = 0;

    public enum ChargingPattern {
        OVERNIGHT_FULL,      // Charge to 100% overnight
        TOP_UP,              // Brief charges throughout day
        DEEP_CYCLE,          // Let drain, then full charge
        ANXIETY_BASED        // Charge whenever below threshold
    }

    public enum ChargerType {
        WALL_FAST,           // Fast wall charger
        WALL_SLOW,           // Standard wall charger
        USB_COMPUTER,        // USB from computer
        USB_CAR,             // Car charger
        WIRELESS             // Wireless charging pad
    }

    private static ChargingPattern currentPattern = ChargingPattern.TOP_UP;
    private static ChargerType currentCharger = ChargerType.WALL_FAST;

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        HookUtils.logInfo(TAG, "Initializing Charging Behavior Hook");

        try {
            determineChargingPattern();
            initializeBatteryState();

            hookBatteryManager(lpparam);
            hookUsbManager(lpparam);
            hookPowerIntents(lpparam);

            startBatterySimulationThread();

            HookUtils.logInfo(TAG, "Charging Behavior Hook initialized");
            HookUtils.logInfo(TAG, String.format("Pattern: %s, Anxiety threshold: %d%%",
                currentPattern.name(), batteryAnxietyThreshold));
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize hook", e);
        }
    }

    private static void determineChargingPattern() {
        double rand = random.get().nextDouble();

        if (rand < 0.68) {
            currentPattern = ChargingPattern.OVERNIGHT_FULL;
            overnightChargingEnabled = true;
            targetChargeLevel = 95 + random.get().nextInt(6); // 95-100%
        } else if (rand < 0.80) {
            currentPattern = ChargingPattern.TOP_UP;
            targetChargeLevel = 80 + random.get().nextInt(11); // 80-90%
        } else if (rand < 0.90) {
            currentPattern = ChargingPattern.DEEP_CYCLE;
            batteryAnxietyThreshold = 15 + random.get().nextInt(10); // 15-25%
            targetChargeLevel = 100;
        } else {
            currentPattern = ChargingPattern.ANXIETY_BASED;
            batteryAnxietyThreshold = 30 + random.get().nextInt(20); // 30-50%
            targetChargeLevel = 70 + random.get().nextInt(20); // 70-90%
        }

        // Fast charging preference
        preferFastCharging = random.get().nextDouble() < 0.75;
    }

    private static void initializeBatteryState() {
        // Start with realistic battery level
        currentBatteryLevel = 30 + random.get().nextInt(50); // 30-80%
        lastBatteryUpdateTime = System.currentTimeMillis();
    }

    private static void hookBatteryManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> batteryManagerClass = XposedHelpers.findClass(
                "android.os.BatteryManager",
                lpparam.classLoader
            );

            // Hook getIntProperty
            XposedBridge.hookAllMethods(batteryManagerClass, "getIntProperty",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        try {
                            int property = (int) param.args[0];

                            if (property == BatteryManager.BATTERY_PROPERTY_CAPACITY) {
                                param.setResult(currentBatteryLevel);
                            } else if (property == BatteryManager.BATTERY_PROPERTY_STATUS) {
                                param.setResult(isCharging.get()
                                    ? BatteryManager.BATTERY_STATUS_CHARGING
                                    : BatteryManager.BATTERY_STATUS_DISCHARGING);
                            }
                        } catch (Exception e) {
                            if (DEBUG) HookUtils.logDebug(TAG, "Error in getIntProperty: " + e.getMessage());
                        }
                    }
                });

            // Hook isCharging
            XposedBridge.hookAllMethods(batteryManagerClass, "isCharging",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        try {
                            param.setResult(isCharging.get());
                        } catch (Exception e) {
                            if (DEBUG) HookUtils.logDebug(TAG, "Error in isCharging: " + e.getMessage());
                        }
                    }
                });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked BatteryManager");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook BatteryManager", e);
        }
    }

    private static void hookUsbManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> usbManagerClass = XposedHelpers.findClass(
                "android.hardware.usb.UsbManager",
                lpparam.classLoader
            );

            // Hook getDeviceList to simulate charger connection
            XposedBridge.hookAllMethods(usbManagerClass, "getDeviceList",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        // Could modify connected USB devices list
                    }
                });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked UsbManager");
        } catch (Exception e) {
            if (DEBUG) HookUtils.logDebug(TAG, "UsbManager hook not available: " + e.getMessage());
        }
    }

    private static void hookPowerIntents(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook broadcast receiver for power connected/disconnected
            Class<?> broadcastReceiverClass = XposedHelpers.findClass(
                "android.content.BroadcastReceiver",
                lpparam.classLoader
            );

            XposedBridge.hookAllMethods(broadcastReceiverClass, "onReceive",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        try {
                            Intent intent = (Intent) param.args[1];
                            String action = intent.getAction();

                            if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
                                handleChargingConnected();
                            } else if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
                                handleChargingDisconnected();
                            }
                        } catch (Exception e) {
                            if (DEBUG) HookUtils.logDebug(TAG, "Error in power intent: " + e.getMessage());
                        }
                    }
                });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked power intents");
        } catch (Exception e) {
            if (DEBUG) HookUtils.logDebug(TAG, "Power intent hook not available: " + e.getMessage());
        }
    }

    private static void startBatterySimulationThread() {
        Thread batteryThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60000); // Update every minute

                    if (!enabled || !chargingSimulationEnabled) continue;

                    updateBatteryState();
                    simulateChargingBehavior();

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    if (DEBUG) HookUtils.logDebug(TAG, "Battery simulation error: " + e.getMessage());
                }
            }
        });
        batteryThread.setName("BatterySimulation");
        batteryThread.setDaemon(true);
        batteryThread.start();
    }

    private static void updateBatteryState() {
        long currentTime = System.currentTimeMillis();
        long elapsedMinutes = (currentTime - lastBatteryUpdateTime) / 60000;

        if (isCharging.get()) {
            // Charging: increase battery level
            int chargeRate = isFastCharging.get() ? 2 : 1; // % per minute
            currentBatteryLevel = Math.min(100, currentBatteryLevel + (int) (chargeRate * elapsedMinutes));

            // Check if fully charged or target reached
            if (currentBatteryLevel >= targetChargeLevel) {
                if (shouldUnplug()) {
                    simulateUnplugging();
                }
            }
        } else {
            // Discharging: decrease battery level
            int dischargeAmount = (int) (batteryDischargeRate * elapsedMinutes / 60.0);
            currentBatteryLevel = Math.max(0, currentBatteryLevel - dischargeAmount);

            // Check if should start charging
            if (currentBatteryLevel <= batteryAnxietyThreshold) {
                if (shouldStartCharging()) {
                    simulateChargingStart();
                }
            }
        }

        lastBatteryUpdateTime = currentTime;
    }

    private static void simulateChargingBehavior() {
        // Additional charging behavior simulation
        if (isCharging.get()) {
            // Occasionally simulate charging interruption
            if (random.get().nextDouble() < 0.01) {
                simulateChargingInterruption();
            }
        }
    }

    private static boolean shouldStartCharging() {
        // Check if it's appropriate time to charge
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);

        // Overnight charging preferred
        if (currentPattern == ChargingPattern.OVERNIGHT_FULL) {
            return hour >= 22 || hour <= 7;
        }

        // Anxiety-based: always charge when below threshold
        if (currentPattern == ChargingPattern.ANXIETY_BASED) {
            return true;
        }

        return random.get().nextDouble() < 0.7;
    }

    private static boolean shouldUnplug() {
        // Determine if user would unplug at current level
        if (currentBatteryLevel >= 100) {
            return true;
        }

        if (currentBatteryLevel >= targetChargeLevel) {
            return random.get().nextDouble() < 0.8;
        }

        // Check if user needs to leave
        return random.get().nextDouble() < 0.1;
    }

    private static void simulateChargingStart() {
        isCharging.set(true);
        isFastCharging.set(preferFastCharging && random.get().nextDouble() < 0.8);
        chargingStartTime.set(System.currentTimeMillis());
        chargeStartLevel.set(currentBatteryLevel);
        chargeSessionCount.incrementAndGet();

        if (DEBUG) {
            HookUtils.logDebug(TAG, String.format(
                "Charging started: level=%d%%, fast=%s",
                currentBatteryLevel, isFastCharging.get()
            ));
        }
    }

    private static void simulateUnplugging() {
        long sessionDuration = System.currentTimeMillis() - chargingStartTime.get();
        totalChargingTimeMs.addAndGet(sessionDuration);

        isCharging.set(false);
        isFastCharging.set(false);

        if (DEBUG) {
            HookUtils.logDebug(TAG, String.format(
                "Charging ended: level=%d%%, duration=%.1f min",
                currentBatteryLevel, sessionDuration / 60000.0
            ));
        }
    }

    private static void simulateChargingInterruption() {
        // Simulate brief unplugging (moving device while charging)
        if (DEBUG) HookUtils.logDebug(TAG, "Charging interrupted briefly");
    }

    private static void handleChargingConnected() {
        simulateChargingStart();
    }

    private static void handleChargingDisconnected() {
        simulateUnplugging();
    }

    // Configuration methods

    public static void setEnabled(boolean enabled) {
        ChargingBehaviorHook.enabled = enabled;
        HookUtils.logInfo(TAG, "Hook " + (enabled ? "enabled" : "disabled"));
    }

    public static void setChargingPattern(ChargingPattern pattern) {
        currentPattern = pattern;
        HookUtils.logInfo(TAG, "Charging pattern set to: " + pattern.name());
    }

    public static void setChargerType(ChargerType type) {
        currentCharger = type;
        HookUtils.logInfo(TAG, "Charger type set to: " + type.name());
    }

    public static void setBatteryAnxietyThreshold(int threshold) {
        batteryAnxietyThreshold = Math.max(5, Math.min(50, threshold));
        HookUtils.logInfo(TAG, "Battery anxiety threshold: " + batteryAnxietyThreshold + "%");
    }

    public static void setTargetChargeLevel(int level) {
        targetChargeLevel = Math.max(50, Math.min(100, level));
        HookUtils.logInfo(TAG, "Target charge level: " + targetChargeLevel + "%");
    }

    public static void setPreferFastCharging(boolean prefer) {
        preferFastCharging = prefer;
        HookUtils.logInfo(TAG, "Fast charging preference: " + prefer);
    }

    public static int getCurrentBatteryLevel() {
        return currentBatteryLevel;
    }

    public static boolean isCharging() {
        return isCharging.get();
    }

    public static boolean isFastCharging() {
        return isFastCharging.get();
    }

    public static int getChargeSessionCount() {
        return chargeSessionCount.get();
    }

    public static long getTotalChargingTimeMs() {
        return totalChargingTimeMs.get();
    }

    public static ChargingPattern getCurrentPattern() {
        return currentPattern;
    }
}
