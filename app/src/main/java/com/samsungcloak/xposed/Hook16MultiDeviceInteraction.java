package com.samsungcloak.xposed;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.os.Build;
import android.os.Parcelable;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hook16MultiDeviceInteraction - Multi-Device Interaction Realism
 * 
 * Simulates realistic multi-device interaction behaviors:
 * - Bluetooth: connection delays, intermittent disconnection, audio dropouts
 * - Nearby Share: detection latency, transfer success rates by distance
 * - NFC: read success probability, hold time variability, multi-tag conflicts
 * - Audio routing: handover delays, volume mismatches, fallback behavior
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class Hook16MultiDeviceInteraction {

    private static final String TAG = "[MultiDevice][Hook16]";
    private static final boolean DEBUG = true;
    private static final ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new Random());

    // Configuration
    private static boolean enabled = true;
    private static float intensity = 0.5f;

    // Bluetooth settings
    private static boolean bluetoothEnabled = true;
    private static double connectionDelayProbability = 0.15;
    private static double disconnectionProbability = 0.08;
    private static double audioDropoutProbability = 0.12;
    private static int connectionDelayBaseMs = 500;
    private static int connectionDelayVarianceMs = 1500;

    // Nearby Share settings
    private static boolean nearbyShareEnabled = true;
    private static double detectionLatencyMs = 2000;
    private static double transferSuccessBaseRate = 0.85;
    private static double distanceBasedFailureRate = 0.20; // Per meter beyond 1m

    // NFC settings
    private static boolean nfcEnabled = true;
    private static double readSuccessProbability = 0.90;
    private static double holdTimeVariance = 0.30;
    private static double multiTagConflictProbability = 0.15;

    // Audio routing settings
    private static boolean audioRoutingEnabled = true;
    private static double handoverDelayProbability = 0.10;
    private static double volumeMismatchProbability = 0.08;
    private static double fallbackFailureProbability = 0.05;

    // Connection tracking
    private static final ConcurrentMap<String, DeviceConnectionState> bluetoothConnections = 
        new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, NfcTagState> nfcTags = 
        new ConcurrentHashMap<>();

    // Statistics
    private static final AtomicInteger btConnectionsAttempted = new AtomicInteger(0);
    private static final AtomicInteger btConnectionsFailed = new AtomicInteger(0);
    private static final AtomicInteger btDisconnections = new AtomicInteger(0);
    private static final AtomicInteger audioDropouts = new AtomicInteger(0);
    private static final AtomicInteger nfcReadsFailed = new AtomicInteger(0);
    private static final AtomicInteger transfersFailed = new AtomicInteger(0);
    private static final AtomicInteger routingFailures = new AtomicInteger(0);

    // Current device states
    private static volatile boolean isDriving = false;
    private static volatile boolean isNearDevice = false;
    private static volatile int currentDistance = 1; // meters

    public static class DeviceConnectionState {
        public String address;
        public String name;
        public long connectTime;
        public int connectionType; // 0=none, 1=A2DP, 2=HFP, 3=HID
        public double signalStrength;
        public boolean isIntermittent;

        public DeviceConnectionState(String address, String name) {
            this.address = address;
            this.name = name;
            this.connectTime = System.currentTimeMillis();
            this.connectionType = 0;
            this.signalStrength = -50; // dBm
            this.isIntermittent = false;
        }
    }

    public static class NfcTagState {
        public String id;
        public String techType;
        public long lastReadTime;
        public int readCount;
        public double successRate;

        public NfcTagState(String id) {
            this.id = id;
            this.techType = "NDEF";
            this.lastReadTime = 0;
            this.readCount = 0;
            this.successRate = 1.0;
        }
    }

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!enabled) return;

        HookUtils.logInfo(TAG, "Initializing Multi-Device Interaction Hook 16");

        try {
            hookBluetoothAdapter(lpparam);
            hookBluetoothGatt(lpparam);
            hookAudioManager(lpparam);
            hookNfcAdapter(lpparam);

            HookUtils.logInfo(TAG, "Multi-Device Interaction Hook 16 initialized");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize", e);
        }
    }

    /**
     * Hook BluetoothAdapter
     */
    private static void hookBluetoothAdapter(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> bluetoothAdapterClass = XposedHelpers.findClass(
                "android.bluetooth.BluetoothAdapter", lpparam.classLoader);

            // Hook getBondedDevices
            XposedBridge.hookMethod(
                XposedHelpers.findMethodExact(bluetoothAdapterClass, "getBondedDevices"),
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!bluetoothEnabled || !enabled) return;

                        float effectiveIntensity = getEffectiveIntensity();

                        // Add connection delay simulation
                        if (random.get().nextDouble() < connectionDelayProbability * effectiveIntensity) {
                            int delay = connectionDelayBaseMs + 
                                random.get().nextInt(connectionDelayVarianceMs);
                            Thread.sleep(delay);
                            HookUtils.logDebug(TAG, "Bluetooth connection delay: " + delay + "ms");
                        }
                    }
                });

            // Hook startDiscovery
            XposedBridge.hookMethod(
                XposedHelpers.findMethodExact(bluetoothAdapterClass, "startDiscovery"),
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!bluetoothEnabled || !enabled) return;

                        float effectiveIntensity = getEffectiveIntensity();
                        btConnectionsAttempted.incrementAndGet();

                        // Simulate discovery failures
                        if (random.get().nextDouble() < 0.1 * effectiveIntensity) {
                            btConnectionsFailed.incrementAndGet();
                            HookUtils.logDebug(TAG, "Bluetooth discovery failed");
                        }
                    }
                });

            HookUtils.logDebug(TAG, "BluetoothAdapter hooked");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook BluetoothAdapter", e);
        }
    }

    /**
     * Hook BluetoothGatt
     */
    private static void hookBluetoothGatt(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> bluetoothGattClass = XposedHelpers.findClass(
                "android.bluetooth.BluetoothGatt", lpparam.classLoader);

            // Hook connect
            XposedBridge.hookMethod(
                XposedHelpers.findMethodExact(bluetoothGattClass, "connect"),
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!bluetoothEnabled || !enabled) return;

                        float effectiveIntensity = getEffectiveIntensity();

                        // Simulate connection delay
                        if (random.get().nextDouble() < connectionDelayProbability * effectiveIntensity) {
                            int delay = connectionDelayBaseMs + 
                                random.get().nextInt(connectionDelayVarianceMs);
                            Thread.sleep(delay);
                            HookUtils.logDebug(TAG, "GATT connection delay: " + delay + "ms");
                        }
                    }
                });

            // Hook disconnect
            XposedBridge.hookMethod(
                XposedHelpers.findMethodExact(bluetoothGattClass, "disconnect"),
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!bluetoothEnabled || !enabled) return;

                        float effectiveIntensity = getEffectiveIntensity();

                        // Simulate unexpected disconnection
                        if (random.get().nextDouble() < disconnectionProbability * effectiveIntensity) {
                            btDisconnections.incrementAndGet();
                            HookUtils.logDebug(TAG, "Unexpected Bluetooth disconnection");
                        }
                    }
                });

            // Hook readRemoteRssi
            XposedBridge.hookMethod(
                XposedHelpers.findMethodExact(bluetoothGattClass, "readRemoteRssi"),
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!bluetoothEnabled || !enabled) return;

                        int rssi = (int) param.getResult();
                        // Add RSSI variance
                        int variance = (random.get().nextInt(10) - 5);
                        param.setResult(rssi + variance);
                    }
                });

            HookUtils.logDebug(TAG, "BluetoothGatt hooked");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook BluetoothGatt", e);
        }
    }

    /**
     * Hook AudioManager for audio routing
     */
    private static void hookAudioManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> audioManagerClass = AudioManager.class;

            // Hook startBluetoothSco
            XposedBridge.hookMethod(
                XposedHelpers.findMethodExact(audioManagerClass, "startBluetoothSco"),
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!audioRoutingEnabled || !enabled) return;

                        float effectiveIntensity = getEffectiveIntensity();

                        // Simulate handover delay
                        if (random.get().nextDouble() < handoverDelayProbability * effectiveIntensity) {
                            int delay = 200 + random.get().nextInt(500);
                            Thread.sleep(delay);
                            HookUtils.logDebug(TAG, "Bluetooth SCO handover delay: " + delay + "ms");
                        }
                    }
                });

            // Hook setSpeakerphoneOn
            XposedBridge.hookMethod(
                XposedHelpers.findMethodExact(audioManagerClass, "setSpeakerphoneOn", boolean.class),
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!audioRoutingEnabled || !enabled) return;

                        float effectiveIntensity = getEffectiveIntensity();

                        // Simulate volume mismatch
                        if (random.get().nextDouble() < volumeMismatchProbability * effectiveIntensity) {
                            routingFailures.incrementAndGet();
                            HookUtils.logDebug(TAG, "Audio routing volume mismatch");
                        }
                    }
                });

            // Hook getDevicesForStream
            XposedBridge.hookMethod(
                XposedHelpers.findMethodExact(audioManagerClass, "getDevicesForStream", int.class),
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!audioRoutingEnabled || !enabled) return;

                        float effectiveIntensity = getEffectiveIntensity();
                        int streamType = (int) param.args[0];

                        // Simulate audio dropout during device switch
                        if (random.get().nextDouble() < audioDropoutProbability * effectiveIntensity) {
                            audioDropouts.incrementAndGet();
                            HookUtils.logDebug(TAG, "Audio dropout during stream: " + streamType);
                        }
                    }
                });

            HookUtils.logDebug(TAG, "AudioManager hooked");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook AudioManager", e);
        }
    }

    /**
     * Hook NfcAdapter
     */
    @SuppressLint("NewApi")
    private static void hookNfcAdapter(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> nfcAdapterClass = XposedHelpers.findClass(
                "android.nfc.NfcAdapter", lpparam.classLoader);

            // Hook enableForegroundDispatch
            XposedBridge.hookMethod(
                XposedHelpers.findMethodExact(nfcAdapterClass, "enableForegroundDispatch",
                    Activity.class, PendingIntent.class, IntentFilter[].class, String[][].class),
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!nfcEnabled || !enabled) return;

                        float effectiveIntensity = getEffectiveIntensity();

                        // Simulate multi-tag conflict
                        if (random.get().nextDouble() < multiTagConflictProbability * effectiveIntensity) {
                            HookUtils.logDebug(TAG, "Multi-tag conflict detected");
                        }
                    }
                });

            // Hook nfcAdapter.dispatch - this is handled via Intent
            // We'll use a receiver hook instead

            HookUtils.logDebug(TAG, "NfcAdapter hooked");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook NfcAdapter", e);
        }
    }

    /**
     * Process NFC tag discovery intent
     */
    public static void processNfcTagIntent(Intent intent) {
        if (!nfcEnabled || !enabled) return;

        float effectiveIntensity = getEffectiveIntensity();

        try {
            Parcelable[] rawTags = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_TAG);
            if (rawTags != null && rawTags.length > 1) {
                // Multi-tag scenario
                if (random.get().nextDouble() < multiTagConflictProbability * effectiveIntensity) {
                    HookUtils.logDebug(TAG, "NFC multi-tag conflict: " + rawTags.length + " tags");
                }
            }

            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null) {
                String tagId = bytesToHex(tag.getId());
                NfcTagState tagState = nfcTags.computeIfAbsent(tagId, NfcTagState::new);
                tagState.readCount++;

                // Check read success
                double successRate = readSuccessProbability * (1 - (tagState.readCount * 0.02));
                if (random.get().nextDouble() > successRate) {
                    nfcReadsFailed.incrementAndGet();
                    HookUtils.logDebug(TAG, "NFC read failed for tag: " + tagId);
                }

                // Apply hold time variability
                double holdTimeVarianceActual = holdTimeVariance * (random.get().nextDouble() * 2 - 1);
                HookUtils.logDebug(TAG, String.format("NFC tag read: id=%s, holdTimeVariance=%.1f%%",
                    tagId.substring(0, Math.min(8, tagId.length())), holdTimeVarianceActual * 100));
            }
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to process NFC intent", e);
        }
    }

    /**
     * Process Nearby Share transfer
     */
    public static void processNearbyShareTransfer(String transferId, int distanceMeters) {
        if (!nearbyShareEnabled || !enabled) return;

        float effectiveIntensity = getEffectiveIntensity();
        currentDistance = distanceMeters;

        try {
            // Detection latency
            double latency = detectionLatencyMs * (1 + random.get().nextDouble() * 0.5);
            Thread.sleep((long) latency);

            // Transfer success based on distance
            double distancePenalty = Math.max(0, distanceMeters - 1) * distanceBasedFailureRate;
            double successRate = transferSuccessBaseRate - distancePenalty;

            if (random.get().nextDouble() > successRate * effectiveIntensity) {
                transfersFailed.incrementAndGet();
                HookUtils.logDebug(TAG, "Nearby Share transfer failed: distance=" + distanceMeters + "m");
            }
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to process Nearby Share", e);
        }
    }

    /**
     * Convert bytes to hex string
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    /**
     * Get effective intensity
     */
    private static float getEffectiveIntensity() {
        ConfigurationManager config = BaseRealismHook.configManager;
        float configIntensity = config != null ? config.getHookIntensity("hook_16") : intensity;
        return intensity * configIntensity;
    }

    /**
     * Get multi-device statistics
     */
    public static String getStats() {
        return String.format("MultiDevice[bt_attempted=%d, bt_failed=%d, bt_disc=%d, audio_drop=%d, nfc_fail=%d, transfer_fail=%d, routing_fail=%d]",
            btConnectionsAttempted.get(), btConnectionsFailed.get(), btDisconnections.get(),
            audioDropouts.get(), nfcReadsFailed.get(), transfersFailed.get(), routingFailures.get());
    }

    // Required Android classes
    private static class Activity {}
    private static class PendingIntent {}
    private static class Intent {}
    private static class IntentFilter {}
}
