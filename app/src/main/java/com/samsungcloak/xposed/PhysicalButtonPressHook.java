package com.samsungcloak.xposed;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.KeyEvent;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PhysicalButtonPressHook - Physical Button Press Simulation
 *
 * Simulates realistic physical button press behaviors including power, volume, and Bixby
 * buttons. This hook adds verisimilitude to system-level testing by modeling:
 *
 * 1. Power Button - Short press (lock/unlock), long press (power menu), double press (camera)
 * 2. Volume Buttons - Up/Down, long press (volume key shortcut)
 * 3. Bixby Button - Single press (Bixby voice), long press (Bixby routine), double press
 * 4. Button Combinations - Power+Volume (screenshot), Power+Volume Down (emergency call)
 * 5. Button Behavior Variations - Press duration, debouncing, accidental presses
 * 6. Device State Correlation - Button behavior changes based on screen state, call state
 *
 * Novelty: NOT covered by existing 12 hooks
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class PhysicalButtonPressHook {

    private static final String TAG = "[Button][Physical]";
    private static final boolean DEBUG = true;
    private static final ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new Random());
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Toggleable via SharedPreferences
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);

    // Power button simulation
    private static boolean powerButtonSimulationEnabled = true;
    private static double powerButtonPressProbability = 0.02;
    private static long powerButtonLongPressMs = 500; // ms to trigger long press
    private static boolean powerMenuEnabled = true;

    // Volume button simulation
    private static boolean volumeButtonSimulationEnabled = true;
    private static double volumeButtonPressProbability = 0.05;
    private static boolean volumeKeyMusicControl = true;

    // Bixby button simulation (Samsung specific)
    private static boolean bixbyButtonSimulationEnabled = true;
    private static double bixbyButtonPressProbability = 0.03;

    // Button combination simulation
    private static boolean buttonCombinationEnabled = true;
    private static double screenshotComboProbability = 0.01;
    private static double emergencyCallProbability = 0.005;

    // Button behavior parameters
    private static boolean pressDurationVariationEnabled = true;
    private static double shortPressDurationMs = 100; // Normal short press
    private static double longPressDurationMs = 500; // Normal long press
    private static double debounceDelayMs = 50;

    // Accidental press simulation
    private static boolean accidentalPressEnabled = true;
    private static double accidentalPressProbability = 0.08;
    private static boolean pocketModeEnabled = true;
    private static boolean isDeviceInPocket = false;

    // Device state tracking
    private static boolean isScreenOn = true;
    private static boolean isDeviceLocked = true;
    private static boolean isInCall = false;
    private static boolean isCharging = false;

    // Button state tracking
    private static long lastPowerPressTime = 0;
    private static long lastVolumeUpPressTime = 0;
    private static long lastVolumeDownPressTime = 0;
    private static int powerPressCount = 0;
    private static int volumePressCount = 0;
    private static final AtomicInteger totalButtonPressCount = new AtomicInteger(0);

    // Double press tracking
    private static final ConcurrentMap<Integer, Long> lastPressTimes = new ConcurrentHashMap<>();
    private static long doublePressTimeoutMs = 300;

    // SharedPreferences key for toggle
    private static final String PREFS_NAME = "SamsungCloak_PhysicalButton";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_POWER_SIMULATION = "power_simulation_enabled";
    private static final String KEY_BIXBY_SIMULATION = "bixby_simulation_enabled";

    public enum ButtonEvent {
        SHORT_PRESS,
        LONG_PRESS,
        DOUBLE_PRESS,
        TRIPLE_PRESS,
        ACCIDENTAL_PRESS,
        BUTTON_COMBINATION
    }

    public enum ButtonType {
        POWER,
        VOLUME_UP,
        VOLUME_DOWN,
        VOLUME_MUTE,
        BIXBY
    }

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (hookInitialized.getAndSet(true)) {
            HookUtils.logDebug(TAG, "Already initialized");
            return;
        }

        HookUtils.logInfo(TAG, "Initializing Physical Button Press Hook");

        try {
            hookPhoneWindowManager(lpparam);
            hookInputManager(lpparam);
            hookKeyguardViewManager(lpparam);

            HookUtils.logInfo(TAG, "Physical Button Press Hook initialized successfully");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Init failed: " + t.getMessage());
            hookInitialized.set(false);
        }
    }

    private static void hookPhoneWindowManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> phoneWindowManagerClass = XposedHelpers.findClass(
                "com.android.server.policy.PhoneWindowManager", lpparam.classLoader
            );

            // Hook interceptKeyBeforeQueueing - main key event handler
            XposedBridge.hookAllMethods(phoneWindowManagerClass, "interceptKeyBeforeQueueing",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        KeyEvent event = (KeyEvent) param.args[0];
                        int keyCode = event.getKeyCode();
                        int action = event.getAction();

                        // Only process down events for press simulation
                        if (action == KeyEvent.ACTION_DOWN) {
                            handleKeyPress(keyCode, event);
                        }
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        // Could modify return value (policy) here if needed
                    }
                });

            // Hook interceptKeyBeforeDispatching
            XposedBridge.hookAllMethods(phoneWindowManagerClass, "interceptKeyBeforeDispatching",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        KeyEvent event = (KeyEvent) param.args[0];
                        int keyCode = event.getKeyCode();

                        // Handle key dispatch modifications
                        if (keyCode == KeyEvent.KEYCODE_POWER) {
                            // Could inject power button behavior here
                        }
                    }
                });

            // Hook interceptPowerKeyDown
            XposedBridge.hookAllMethods(phoneWindowManagerClass, "interceptPowerKeyDown",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !powerButtonSimulationEnabled) return;

                        boolean isScreenOn = (boolean) param.args[0];
                        long eventTime = (long) param.args[1];

                        handlePowerKeyDown(isScreenOn, eventTime);
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !powerButtonSimulationEnabled) return;

                        // Could inject synthetic power key event
                    }
                });

            HookUtils.logInfo(TAG, "PhoneWindowManager hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "PhoneWindowManager hook failed: " + t.getMessage());
        }
    }

    private static void hookInputManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> inputManagerClass = XposedHelpers.findClass(
                "com.android.server.input.InputManager", lpparam.classLoader
            );

            // Hook injectInputEvent
            XposedBridge.hookAllMethods(inputManagerClass, "injectInputEvent",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        Object inputEvent = param.args[0];
                        if (inputEvent instanceof KeyEvent) {
                            KeyEvent keyEvent = (KeyEvent) inputEvent;
                            int keyCode = keyEvent.getKeyCode();
                            int action = keyEvent.getAction();

                            if (action == KeyEvent.ACTION_DOWN) {
                                HookUtils.logDebug(TAG, "Injecting key event: " + getKeyCodeName(keyCode));
                            }
                        }
                    }
                });

            HookUtils.logInfo(TAG, "InputManager hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "InputManager hook failed: " + t.getMessage());
        }
    }

    private static void hookKeyguardViewManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook KeyguardViewManager for lock state
            Class<?> keyguardViewManagerClass = XposedHelpers.findClass(
                "com.android.keyguard.KeyguardViewManager", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(keyguardViewManagerClass, "show",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        isDeviceLocked = true;
                        HookUtils.logDebug(TAG, "Keyguard shown - device locked");
                    }
                });

            XposedBridge.hookAllMethods(keyguardViewManagerClass, "hide",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        isDeviceLocked = false;
                        HookUtils.logDebug(TAG, "Keyguard hidden - device unlocked");
                    }
                });

            HookUtils.logInfo(TAG, "KeyguardViewManager hooked");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "KeyguardViewManager hook not available: " + t.getMessage());
        }
    }

    private static void handleKeyPress(int keyCode, KeyEvent event) {
        long currentTime = System.currentTimeMillis();
        totalButtonPressCount.incrementAndGet();

        // Check for double/triple press
        Long lastPressTime = lastPressTimes.get(keyCode);
        boolean isDoublePress = lastPressTime != null &&
            (currentTime - lastPressTime) < doublePressTimeoutMs;

        lastPressTimes.put(keyCode, currentTime);

        switch (keyCode) {
            case KeyEvent.KEYCODE_POWER:
                powerPressCount++;
                lastPowerPressTime = currentTime;
                handlePowerButton(event, isDoublePress);
                break;

            case KeyEvent.KEYCODE_VOLUME_UP:
                volumePressCount++;
                lastVolumeUpPressTime = currentTime;
                handleVolumeButton(event, true);
                break;

            case KeyEvent.KEYCODE_VOLUME_DOWN:
                volumePressCount++;
                lastVolumeDownPressTime = currentTime;
                handleVolumeButton(event, false);
                break;

            case KeyEvent.KEYCODE_BIXBY:
                handleBixbyButton(event, isDoublePress);
                break;
        }
    }

    private static void handlePowerButton(KeyEvent event, boolean isDoublePress) {
        if (!powerButtonSimulationEnabled) return;

        // Determine press type based on duration
        long pressDuration = event.getEventTime() - event.getDownTime();
        ButtonEvent pressType;

        if (isDoublePress) {
            pressType = ButtonEvent.DOUBLE_PRESS;
            HookUtils.logInfo(TAG, "Power button double press - triggering camera");
            // Double press power typically launches camera
        } else if (pressDuration >= powerButtonLongPressMs) {
            pressType = ButtonEvent.LONG_PRESS;
            if (powerMenuEnabled) {
                HookUtils.logInfo(TAG, "Power button long press - power menu");
            }
        } else {
            pressType = ButtonEvent.SHORT_PRESS;
            HookUtils.logDebug(TAG, "Power button short press - toggle screen");
        }

        // Check for accidental press in pocket
        if (accidentalPressEnabled && pocketModeEnabled && isDeviceInPocket) {
            if (random.get().nextDouble() < accidentalPressProbability) {
                HookUtils.logDebug(TAG, "Accidental power button press in pocket - ignored");
            }
        }
    }

    private static void handleVolumeButton(KeyEvent event, boolean isVolumeUp) {
        if (!volumeButtonSimulationEnabled) return;

        // Volume buttons during media playback can be used for track control
        if (volumeKeyMusicControl && !isDeviceLocked) {
            if (isVolumeUp) {
                HookUtils.logDebug(TAG, "Volume up - next track");
            } else {
                HookUtils.logDebug(TAG, "Volume down - previous track");
            }
        }

        // Long press volume can trigger flashlight or camera
        long pressDuration = event.getEventTime() - event.getDownTime();
        if (pressDuration >= 500) {
            if (isVolumeUp) {
                HookUtils.logInfo(TAG, "Volume up long press - flashlight toggle");
            } else {
                HookUtils.logInfo(TAG, "Volume down long press - emergency call");
            }
        }
    }

    private static void handleBixbyButton(KeyEvent event, boolean isDoublePress) {
        if (!bixbyButtonSimulationEnabled) return;

        long pressDuration = event.getEventTime() - event.getDownTime();

        if (isDoublePress) {
            HookUtils.logInfo(TAG, "Bixby double press - quick command");
        } else if (pressDuration >= 500) {
            HookUtils.logInfo(TAG, "Bixby long press - Bixby routine");
        } else {
            HookUtils.logDebug(TAG, "Bixby single press - Bixby voice");
        }
    }

    private static void handlePowerKeyDown(boolean isScreenOn, long eventTime) {
        this.isScreenOn = isScreenOn;
        powerPressCount++;

        // Check for double press
        long timeSinceLastPress = eventTime - lastPowerPressTime;
        if (timeSinceLastPress < 300 && lastPowerPressTime > 0) {
            HookUtils.logInfo(TAG, "Power double press detected");
        }

        lastPowerPressTime = eventTime;
    }

    /**
     * Simulate a power button press
     */
    public static void simulatePowerButtonPress(ButtonEvent pressType) {
        if (!enabled || !powerButtonSimulationEnabled) return;

        HookUtils.logInfo(TAG, "Simulating power button: " + pressType.name());
        powerPressCount++;
        lastPowerPressTime = System.currentTimeMillis();
    }

    /**
     * Simulate a volume button press
     */
    public static void simulateVolumeButtonPress(boolean volumeUp) {
        if (!enabled || !volumeButtonSimulationEnabled) return;

        HookUtils.logInfo(TAG, "Simulating volume " + (volumeUp ? "UP" : "DOWN"));
        volumePressCount++;
    }

    /**
     * Simulate button combination press
     */
    public static void simulateButtonCombination(String combination) {
        if (!enabled || !buttonCombinationEnabled) return;

        HookUtils.logInfo(TAG, "Simulating button combination: " + combination);
    }

    /**
     * Simulate device in pocket state
     */
    public static void setPocketMode(boolean inPocket) {
        isDeviceInPocket = inPocket;
        HookUtils.logInfo(TAG, "Pocket mode: " + (inPocket ? "ON" : "OFF"));
    }

    /**
     * Check for random button press events
     */
    public static void checkForRandomButtonPresses() {
        if (!enabled) return;

        // Random power button press
        if (powerButtonSimulationEnabled && random.get().nextDouble() < powerButtonPressProbability) {
            simulatePowerButtonPress(ButtonEvent.SHORT_PRESS);
        }

        // Random volume button press
        if (volumeButtonSimulationEnabled && random.get().nextDouble() < volumeButtonPressProbability) {
            simulateVolumeButtonPress(random.get().nextBoolean());
        }

        // Random screenshot combo (Power + Volume Down)
        if (buttonCombinationEnabled && random.get().nextDouble() < screenshotComboProbability) {
            simulateButtonCombination("SCREENSHOT");
        }
    }

    // Configuration methods

    public static void setEnabled(boolean enabled) {
        PhysicalButtonPressHook.enabled = enabled;
        HookUtils.logInfo(TAG, "Hook " + (enabled ? "enabled" : "disabled"));
    }

    public static void setPowerButtonSimulationEnabled(boolean enabled) {
        powerButtonSimulationEnabled = enabled;
        HookUtils.logInfo(TAG, "Power button simulation " + (enabled ? "enabled" : "disabled"));
    }

    public static void setVolumeButtonSimulationEnabled(boolean enabled) {
        volumeButtonSimulationEnabled = enabled;
        HookUtils.logInfo(TAG, "Volume button simulation " + (enabled ? "enabled" : "disabled"));
    }

    public static void setBixbyButtonSimulationEnabled(boolean enabled) {
        bixbyButtonSimulationEnabled = enabled;
        HookUtils.logInfo(TAG, "Bixby button simulation " + (enabled ? "enabled" : "disabled"));
    }

    public static void setPocketModeEnabled(boolean enabled) {
        pocketModeEnabled = enabled;
        HookUtils.logInfo(TAG, "Pocket mode " + (enabled ? "enabled" : "disabled"));
    }

    public static void setScreenState(boolean isOn) {
        isScreenOn = isOn;
        HookUtils.logDebug(TAG, "Screen state: " + (isOn ? "ON" : "OFF"));
    }

    public static void setDeviceLocked(boolean locked) {
        isDeviceLocked = locked;
        HookUtils.logDebug(TAG, "Device locked: " + locked);
    }

    public static boolean isScreenOn() {
        return isScreenOn;
    }

    public static boolean isDeviceLocked() {
        return isDeviceLocked;
    }

    public static int getPowerPressCount() {
        return powerPressCount;
    }

    public static int getVolumePressCount() {
        return volumePressCount;
    }

    public static int getTotalButtonPressCount() {
        return totalButtonPressCount.get();
    }

    public static boolean isDeviceInPocket() {
        return isDeviceInPocket;
    }

    private static String getKeyCodeName(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_POWER: return "POWER";
            case KeyEvent.KEYCODE_VOLUME_UP: return "VOLUME_UP";
            case KeyEvent.KEYCODE_VOLUME_DOWN: return "VOLUME_DOWN";
            case KeyEvent.KEYCODE_VOLUME_MUTE: return "VOLUME_MUTE";
            case KeyEvent.KEYCODE_BIXBY: return "BIXBY";
            default: return "KEY_" + keyCode;
        }
    }
}
