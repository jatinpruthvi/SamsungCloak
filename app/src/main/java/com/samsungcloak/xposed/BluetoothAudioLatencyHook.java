package com.samsungcloak.xposed;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.media.AudioManager;
import android.media.AudioTrack;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * BluetoothAudioLatencyHook - Bluetooth Audio Delay & Reconnection
 * 
 * Simulates realistic Bluetooth audio behavior:
 * - A2DP latency (80-200ms)
 * - Audio-video sync issues
 * - SBC codec degradation
 * - Unexpected disconnections
 * - Crackling during low battery
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class BluetoothAudioLatencyHook {

    private static final String TAG = "[BT][Audio]";
    private static final boolean DEBUG = true;
    
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    
    // Latency settings (ms)
    private static int minLatency = 80;
    private static int maxLatency = 200;
    private static int currentLatency = 120;
    
    // Failure rates
    private static float disconnectionRate = 0.02f;      // 2% per minute
    private static float cracklingRate = 0.05f;          // 5% when low battery
    private static float syncIssueRate = 0.08f;          // 8% AV desync
    
    // State
    private static boolean isA2dpConnected = false;
    private static boolean isPlaying = false;
    private static String connectedDeviceAddress = null;
    private static int batteryLevel = 100;
    private static boolean lowBatteryMode = false;
    
    private static final Random random = new Random();
    private static final List<AudioEvent> audioEvents = new CopyOnWriteArrayList<>();
    private static long lastDisconnectTime = 0;
    private static long lastLatencyChangeTime = 0;
    
    public static class AudioEvent {
        public String type;
        public long timestamp;
        public String device;
        public int latency;
        
        public AudioEvent(String type, String device, int latency) {
            this.type = type;
            this.timestamp = System.currentTimeMillis();
            this.device = device;
            this.latency = latency;
        }
    }
    
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (hookInitialized.getAndSet(true)) {
            HookUtils.logDebug(TAG, "Already initialized");
            return;
        }
        
        HookUtils.logInfo(TAG, "Initializing Bluetooth Audio Latency Hook");
        
        try {
            hookA2dp(lpparam);
            hookAudioManager(lpparam);
            hookBattery(lpparam);
            
            HookUtils.logInfo(TAG, "Bluetooth audio hook initialized");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Init failed: " + t.getMessage());
            hookInitialized.set(false);
        }
    }
    
    private static void hookA2dp(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> a2dpClass = XposedHelpers.findClass(
                "android.bluetooth.BluetoothA2dp", lpparam.classLoader
            );
            
            // Hook connect() to track connection
            XposedBridge.hookAllMethods(a2dpClass, "connect",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    // Track connected device
                    if (param.args.length > 0 && param.args[0] instanceof BluetoothDevice) {
                        BluetoothDevice device = (BluetoothDevice) param.args[0];
                        connectedDeviceAddress = device.getAddress();
                        isA2dpConnected = true;
                        
                        audioEvents.add(new AudioEvent("CONNECT", 
                            connectedDeviceAddress, currentLatency));
                        
                        HookUtils.logInfo(TAG, "A2DP connected: " + device.getName());
                        
                        // Apply initial latency
                        applyLatency();
                    }
                }
            });
            
            // Hook disconnect()
            XposedBridge.hookAllMethods(a2dpClass, "disconnect",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    isA2dpConnected = false;
                    isPlaying = false;
                    
                    audioEvents.add(new AudioEvent("DISCONNECT", 
                        connectedDeviceAddress, 0));
                    
                    HookUtils.logDebug(TAG, "A2DP disconnected");
                }
            });
            
            // Hook isA2dpPlaying()
            XposedBridge.hookAllMethods(a2dpClass, "isA2dpPlaying",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    boolean originalResult = (boolean) param.getResult();
                    
                    // Check for random disconnection
                    if (originalResult && random.nextFloat() < disconnectionRate) {
                        long now = System.currentTimeMillis();
                        if (now - lastDisconnectTime > 5000) { // Debounce
                            param.setResult(false);
                            lastDisconnectTime = now;
                            audioEvents.add(new AudioEvent("DROPOUT", 
                                connectedDeviceAddress, currentLatency));
                            HookUtils.logDebug(TAG, "Simulated audio dropout");
                        }
                    } else {
                        param.setResult(originalResult || isPlaying);
                    }
                }
            });
            
            HookUtils.logInfo(TAG, "BluetoothA2dp hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "A2dp hook failed: " + t.getMessage());
        }
    }
    
    private static void hookAudioManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> audioManagerClass = XposedHelpers.findClass(
                "android.media.AudioManager", lpparam.classLoader
            );
            
            // Hook startBluetoothSco() for call audio
            XposedBridge.hookAllMethods(audioManagerClass, "startBluetoothSco",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    // Add extra delay for SCO connection
                    int extraDelay = random.nextInt(100) + 50;
                    HookUtils.logDebug(TAG, "Bluetooth SCO delay: " + extraDelay + "ms");
                }
            });
            
            HookUtils.logInfo(TAG, "AudioManager hooked");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "AudioManager hook skipped: " + t.getMessage());
        }
    }
    
    private static void hookBattery(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> batteryManagerClass = XposedHelpers.findClass(
                "android.os.BatteryManager", lpparam.classLoader
            );
            
            // Hook getIntProperty() to track battery level
            XposedBridge.hookAllMethods(batteryManagerClass, "getIntProperty",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    if (param.args.length > 0 && param.args[0] instanceof Integer) {
                        int property = (int) param.args[0];
                        
                        // Track battery level (property 1 = BATTERY_PROPERTY_CAPACITY)
                        if (property == 1) {
                            batteryLevel = (int) param.getResult();
                            lowBatteryMode = batteryLevel < 20;
                            
                            // Increase crackling when low battery
                            if (lowBatteryMode && random.nextFloat() < cracklingRate) {
                                audioEvents.add(new AudioEvent("CRACKLE_LOW_BATTERY", 
                                    connectedDeviceAddress, currentLatency));
                            }
                        }
                    }
                }
            });
            
            HookUtils.logInfo(TAG, "BatteryManager hooked");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "Battery hook skipped: " + t.getMessage());
        }
    }
    
    /**
     * Apply latency by adjusting currentLatency
     */
    private static void applyLatency() {
        // Vary latency periodically
        long now = System.currentTimeMillis();
        if (now - lastLatencyChangeTime > 10000) { // Every 10 seconds
            currentLatency = minLatency + random.nextInt(maxLatency - minLatency);
            
            // Increase latency if low battery
            if (lowBatteryMode) {
                currentLatency += random.nextInt(50);
            }
            
            lastLatencyChangeTime = now;
            
            HookUtils.logDebug(TAG, "Current BT latency: " + currentLatency + "ms");
        }
    }
    
    /**
     * Simulate audio-video sync issue
     */
    public static boolean shouldInjectSyncIssue() {
        return isPlaying && random.nextFloat() < syncIssueRate;
    }
    
    /**
     * Get current latency in milliseconds
     */
    public static int getCurrentLatency() {
        applyLatency();
        return currentLatency;
    }
    
    // ========== Configuration ==========
    
    public static void setEnabled(boolean enabled) {
        BluetoothAudioLatencyHook.enabled = enabled;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void setMinLatency(int ms) {
        minLatency = Math.max(0, ms);
    }
    
    public static void setMaxLatency(int ms) {
        maxLatency = Math.max(0, ms);
    }
    
    public static void setDisconnectionRate(float rate) {
        disconnectionRate = Math.max(0, Math.min(1, rate));
    }
    
    public static void setPlaying(boolean playing) {
        isPlaying = playing;
    }
    
    public static boolean isConnected() {
        return isA2dpConnected;
    }
    
    public static List<AudioEvent> getAudioEvents() {
        return new ArrayList<>(audioEvents);
    }
}
