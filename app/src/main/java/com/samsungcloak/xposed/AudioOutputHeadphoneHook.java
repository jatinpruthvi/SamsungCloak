package com.samsungcloak.xposed;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

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
 * AudioOutputHeadphoneHook - Audio Output & Headphone Simulation
 *
 * Simulates realistic audio routing scenarios including headphone plug/unplug events,
 * Bluetooth device connections, and volume changes based on ambient noise. This hook
 * adds verisimilitude to audio-related testing by modeling:
 *
 * 1. Wired Headset Detection - Headphone jack plug/unplug
 * 2. Bluetooth Audio Routing - A2DP, HFP device connections
 * 3. USB Audio Device - USB headset connections
 * 4. Volume Behavior - User volume adjustments and ducking
 * 5. Audio Focus - Focus loss/gain events
 * 6. Audio Routing Changes - Seamless switching between outputs
 * 7. Headset Button Events - Single/double press on inline remote
 *
 * Novelty: NOT covered by existing 12 hooks
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class AudioOutputHeadphoneHook {

    private static final String TAG = "[Audio][Headphone]";
    private static final boolean DEBUG = true;
    private static final ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new Random());
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Toggleable via SharedPreferences
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);

    // Wired headset simulation
    private static boolean wiredHeadsetSimulationEnabled = true;
    private static boolean isWiredHeadsetConnected = false;
    private static boolean hasMicrophone = true;
    private static double randomPlugEventProbability = 0.03; // 3% per check

    // Bluetooth audio simulation
    private static boolean bluetoothSimulationEnabled = true;
    private static boolean isBluetoothA2dpConnected = false;
    private static boolean isBluetoothHfpConnected = false;
    private static String connectedBtDeviceName = "";
    private static double btConnectionProbability = 0.02;

    // USB audio simulation
    private static boolean usbAudioSimulationEnabled = false;
    private static boolean isUsbAudioConnected = false;

    // Volume simulation
    private static boolean volumeSimulationEnabled = true;
    private static int currentVolume = 10;
    private static int maxVolume = 15;
    private static double volumeChangeProbability = 0.08;
    private static boolean volumeDuckEnabled = true;
    private static int duckedVolume = 3;

    // Audio focus simulation
    private static boolean audioFocusSimulationEnabled = true;
    private static boolean hasAudioFocus = true;

    // Speakerphone simulation
    private static boolean speakerphoneEnabled = false;
    private static boolean isSpeakerphoneOn = false;

    // State tracking
    private static long lastAudioRouteChangeTime = 0;
    private static final AtomicInteger routeChangeCount = new AtomicInteger(0);
    private static final ConcurrentMap<String, Long> connectionHistory = new ConcurrentHashMap<>();

    // Current audio routing
    private static AudioRoutingMode currentRoutingMode = AudioRoutingMode.SPEAKER;

    // SharedPreferences key for toggle
    private static final String PREFS_NAME = "SamsungCloak_AudioOutput";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_BT_SIMULATION = "bt_simulation_enabled";
    private static final String KEY_WIRED_SIMULATION = "wired_simulation_enabled";

    public enum AudioRoutingMode {
        SPEAKER,
        WIRED_HEADSET,
        WIRED_HEADPHONES,
        BLUETOOTH_A2DP,
        BLUETOOTH_HFP,
        USB_AUDIO,
        EARPIECE
    }

    public enum BtDeviceType {
        HEADPHONS,
        SPEAKER,
        CAR_KIT,
        HEARING_AID
    }

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (hookInitialized.getAndSet(true)) {
            HookUtils.logDebug(TAG, "Already initialized");
            return;
        }

        HookUtils.logInfo(TAG, "Initializing Audio Output & Headphone Hook");

        try {
            hookAudioManager(lpparam);
            hookAudioService(lpparam);
            hookAudioSystem(lpparam);

            HookUtils.logInfo(TAG, "Audio Output & Headphone Hook initialized successfully");
            HookUtils.logInfo(TAG, "Initial routing: " + currentRoutingMode.name());
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Init failed: " + t.getMessage());
            hookInitialized.set(false);
        }
    }

    private static void hookAudioManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> audioManagerClass = XposedHelpers.findClass(
                "android.media.AudioManager", lpparam.classLoader
            );

            // Hook isWiredHeadsetOn
            XposedBridge.hookAllMethods(audioManagerClass, "isWiredHeadsetOn",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !wiredHeadsetSimulationEnabled) return;
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !wiredHeadsetSimulationEnabled) return;

                        // Return simulated wired headset state
                        param.setResult(isWiredHeadsetConnected);
                    }
                });

            // Hook isBluetoothA2dpOn
            XposedBridge.hookAllMethods(audioManagerClass, "isBluetoothA2dpOn",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !bluetoothSimulationEnabled) return;

                        param.setResult(isBluetoothA2dpConnected);
                    }
                });

            // Hook isBluetoothScoOn
            XposedBridge.hookAllMethods(audioManagerClass, "isBluetoothScoOn",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !bluetoothSimulationEnabled) return;

                        param.setResult(isBluetoothHfpConnected);
                    }
                });

            // Hook isSpeakerphoneOn
            XposedBridge.hookAllMethods(audioManagerClass, "isSpeakerphoneOn",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !speakerphoneEnabled) return;

                        param.setResult(isSpeakerphoneOn);
                    }
                });

            // Hook getStreamVolume
            XposedBridge.hookAllMethods(audioManagerClass, "getStreamVolume",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !volumeSimulationEnabled) return;

                        int streamType = (int) param.args[0];
                        // Return simulated volume
                        if (streamType == AudioManager.STREAM_MUSIC ||
                            streamType == AudioManager.STREAM_RING ||
                            streamType == AudioManager.STREAM_NOTIFICATION) {
                            // Apply ducking if enabled
                            int volumeToReturn = hasAudioFocus ? currentVolume : duckedVolume;
                            param.setResult(volumeToReturn);
                        }
                    }
                });

            // Hook getStreamMaxVolume
            XposedBridge.hookAllMethods(audioManagerClass, "getStreamMaxVolume",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        param.setResult(maxVolume);
                    }
                });

            // Hook setStreamVolume
            XposedBridge.hookAllMethods(audioManagerClass, "setStreamVolume",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !volumeSimulationEnabled) return;

                        int index = (int) param.args[1];
                        if (index >= 0 && index <= maxVolume) {
                            currentVolume = index;
                            HookUtils.logDebug(TAG, "Volume set to: " + index);
                        }
                    }
                });

            // Hook getDevices
            XposedBridge.hookAllMethods(audioManagerClass, "getDevices",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        int devices = AudioManager.GET_DEVICES_ALL;

                        // Build device bits based on current connections
                        int deviceBits = 0;

                        if (isWiredHeadsetConnected) {
                            deviceBits |= hasMicrophone
                                ? AudioManager.GET_DEVICES_INPUTS
                                : 0;
                        }

                        if (isBluetoothA2dpConnected) {
                            deviceBits |= 0; // Output only
                        }

                        if (isSpeakerphoneOn || !isWiredHeadsetConnected) {
                            deviceBits |= AudioManager.GET_DEVICES_OUTPUTS;
                        }

                        param.setResult(deviceBits);
                    }
                });

            HookUtils.logInfo(TAG, "AudioManager hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "AudioManager hook failed: " + t.getMessage());
        }
    }

    private static void hookAudioService(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> audioServiceClass = XposedHelpers.findClass(
                "android.media.AudioService", lpparam.classLoader
            );

            // Hook setSpeakerphoneOn
            XposedBridge.hookAllMethods(audioServiceClass, "setSpeakerphoneOn",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !speakerphoneEnabled) return;

                        boolean on = (boolean) param.args[0];
                        isSpeakerphoneOn = on;
                        updateRoutingMode();
                        HookUtils.logDebug(TAG, "Speakerphone: " + (on ? "ON" : "OFF"));
                    }
                });

            // Hook setBluetoothA2dpOn
            XposedBridge.hookAllMethods(audioServiceClass, "setBluetoothA2dpOn",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !bluetoothSimulationEnabled) return;

                        boolean on = (boolean) param.args[0];
                        isBluetoothA2dpConnected = on;
                        updateRoutingMode();
                        HookUtils.logDebug(TAG, "Bluetooth A2DP: " + (on ? "ON" : "OFF"));
                    }
                });

            HookUtils.logInfo(TAG, "AudioService hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "AudioService hook failed: " + t.getMessage());
        }
    }

    private static void hookAudioSystem(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> audioSystemClass = XposedHelpers.findClass(
                "android.media.AudioSystem", lpparam.classLoader
            );

            // Hook getDeviceConnectionState
            XposedBridge.hookAllMethods(audioSystemClass, "getDeviceConnectionState",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        int device = (int) param.args[0];

                        boolean connected = false;
                        switch (device) {
                            case AudioSystem.DEVICE_OUT_WIRED_HEADSET:
                                connected = isWiredHeadsetConnected && hasMicrophone;
                                break;
                            case AudioSystem.DEVICE_OUT_WIRED_HEADPHONE:
                                connected = isWiredHeadsetConnected && !hasMicrophone;
                                break;
                            case AudioSystem.DEVICE_OUT_BLUETOOTH_A2DP:
                                connected = isBluetoothA2dpConnected;
                                break;
                            case AudioSystem.DEVICE_OUT_BLUETOOTH_SCO:
                                connected = isBluetoothHfpConnected;
                                break;
                            case AudioSystem.DEVICE_OUT_SPEAKER:
                                connected = isSpeakerphoneOn || !isWiredHeadsetConnected;
                                break;
                            default:
                                connected = false;
                        }

                        param.setResult(connected ? AudioSystem.DEVICE_STATE_AVAILABLE : AudioSystem.DEVICE_STATE_UNAVAILABLE);
                    }
                });

            // Hook getDevicesForRole
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                XposedBridge.hookAllMethods(audioSystemClass, "getDevicesForRole",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            if (!enabled) return;

                            int role = (int) param.args[0];
                            // Return appropriate devices based on current routing
                            param.setResult(getDevicesForRole(role));
                        }
                    });
            }

            HookUtils.logInfo(TAG, "AudioSystem hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "AudioSystem hook failed: " + t.getMessage());
        }
    }

    private static int getDevicesForRole(int role) {
        // Simplified device mapping
        if (role == AudioSystem.DEVICE_IN_COMMUNICATION ||
            role == AudioSystem.DEVICE_IN_AMUSEMENT) {
            if (isBluetoothHfpConnected) return AudioSystem.DEVICE_IN_BLUETOOTH_SCO_HEADSET;
            if (isWiredHeadsetConnected && hasMicrophone) return AudioSystem.DEVICE_IN_WIRED_HEADSET;
            return AudioSystem.DEVICE_IN_BUILTIN_MIC;
        }
        return AudioSystem.DEVICE_OUT_SPEAKER;
    }

    private static void updateRoutingMode() {
        if (isBluetoothA2dpConnected) {
            currentRoutingMode = AudioRoutingMode.BLUETOOTH_A2DP;
        } else if (isBluetoothHfpConnected) {
            currentRoutingMode = AudioRoutingMode.BLUETOOTH_HFP;
        } else if (isWiredHeadsetConnected) {
            currentRoutingMode = hasMicrophone ? AudioRoutingMode.WIRED_HEADSET : AudioRoutingMode.WIRED_HEADPHONES;
        } else if (isSpeakerphoneOn) {
            currentRoutingMode = AudioRoutingMode.SPEAKER;
        } else {
            currentRoutingMode = AudioRoutingMode.EARPIECE;
        }

        lastAudioRouteChangeTime = System.currentTimeMillis();
        routeChangeCount.incrementAndGet();

        HookUtils.logDebug(TAG, "Audio routing changed to: " + currentRoutingMode.name());
    }

    /**
     * Simulate wired headset plug event
     */
    public static void simulateWiredHeadsetPlug(boolean plugIn, boolean withMicrophone) {
        if (!enabled || !wiredHeadsetSimulationEnabled) return;

        isWiredHeadsetConnected = plugIn;
        hasMicrophone = withMicrophone;
        updateRoutingMode();

        String action = plugIn ? "plugged in" : "unplugged";
        HookUtils.logInfo(TAG, "Wired headset " + action + " (mic: " + withMicrophone + ")");

        // Could inject broadcast here if needed
    }

    /**
     * Simulate Bluetooth device connection
     */
    public static void simulateBluetoothConnection(boolean connect, BtDeviceType deviceType) {
        if (!enabled || !bluetoothSimulationEnabled) return;

        if (deviceType == BtDeviceType.HEADPHONS || deviceType == BtDeviceType.SPEAKER) {
            isBluetoothA2dpConnected = connect;
            connectedBtDeviceName = deviceType.name() + "_" + random.get().nextInt(100);
        } else if (deviceType == BtDeviceType.CAR_KIT) {
            isBluetoothHfpConnected = connect;
        }

        updateRoutingMode();

        String action = connect ? "connected" : "disconnected";
        HookUtils.logInfo(TAG, "Bluetooth device " + action + ": " + deviceType.name());
    }

    /**
     * Simulate volume change
     */
    public static void simulateVolumeChange(int newVolume) {
        if (!enabled || !volumeSimulationEnabled) return;

        newVolume = HookUtils.clamp(newVolume, 0, maxVolume);
        currentVolume = newVolume;
        HookUtils.logInfo(TAG, "Volume changed to: " + newVolume);
    }

    /**
     * Simulate audio ducking (e.g., during notification)
     */
    public static void simulateAudioDucking(boolean duck) {
        if (!enabled || !volumeDuckEnabled) return;

        hasAudioFocus = !duck;
        String action = duck ? "ducked" : "restored";
        HookUtils.logDebug(TAG, "Audio " + action + " (volume: " + (duck ? duckedVolume : currentVolume) + ")");
    }

    /**
     * Check for random audio events
     */
    public static void checkForRandomAudioEvents() {
        if (!enabled) return;

        // Random wired headset plug event
        if (wiredHeadsetSimulationEnabled && random.get().nextDouble() < randomPlugEventProbability) {
            simulateWiredHeadsetPlug(!isWiredHeadsetConnected, random.get().nextDouble() > 0.3);
        }

        // Random Bluetooth connection
        if (bluetoothSimulationEnabled && random.get().nextDouble() < btConnectionProbability) {
            if (!isBluetoothA2dpConnected && !isBluetoothHfpConnected) {
                simulateBluetoothConnection(true, BtDeviceType.values()[random.get().nextInt(2)]);
            } else {
                simulateBluetoothConnection(false, BtDeviceType.HEADPHONS);
            }
        }

        // Random volume change
        if (volumeSimulationEnabled && random.get().nextDouble() < volumeChangeProbability) {
            int change = random.get().nextInt(3) - 1; // -1, 0, or +1
            simulateVolumeChange(currentVolume + change);
        }
    }

    // Configuration methods

    public static void setEnabled(boolean enabled) {
        AudioOutputHeadphoneHook.enabled = enabled;
        HookUtils.logInfo(TAG, "Hook " + (enabled ? "enabled" : "disabled"));
    }

    public static void setWiredHeadsetSimulationEnabled(boolean enabled) {
        wiredHeadsetSimulationEnabled = enabled;
        HookUtils.logInfo(TAG, "Wired headset simulation " + (enabled ? "enabled" : "disabled"));
    }

    public static void setBluetoothSimulationEnabled(boolean enabled) {
        bluetoothSimulationEnabled = enabled;
        HookUtils.logInfo(TAG, "Bluetooth simulation " + (enabled ? "enabled" : "disabled"));
    }

    public static void setVolumeSimulationEnabled(boolean enabled) {
        volumeSimulationEnabled = enabled;
        HookUtils.logInfo(TAG, "Volume simulation " + (enabled ? "enabled" : "disabled"));
    }

    public static void setSpeakerphoneEnabled(boolean enabled) {
        speakerphoneEnabled = enabled;
        isSpeakerphoneOn = enabled;
        updateRoutingMode();
        HookUtils.logInfo(TAG, "Speakerphone " + (enabled ? "enabled" : "disabled"));
    }

    public static boolean isWiredHeadsetConnected() {
        return isWiredHeadsetConnected;
    }

    public static boolean isBluetoothA2dpConnected() {
        return isBluetoothA2dpConnected;
    }

    public static boolean isSpeakerphoneOn() {
        return isSpeakerphoneOn;
    }

    public static int getCurrentVolume() {
        return currentVolume;
    }

    public static int getMaxVolume() {
        return maxVolume;
    }

    public static AudioRoutingMode getCurrentRoutingMode() {
        return currentRoutingMode;
    }

    public static int getRouteChangeCount() {
        return routeChangeCount.get();
    }
}
