package com.samsungcloak.realism;

import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;

/**
 * AudioEnvironmentRealismHook - Acoustic environment and audio behavior simulation
 *
 * Targets realistic audio interactions:
 * - Background noise affecting mic input (SNR variation)
 * - Adaptive volume based on ambient sound
 * - Microphone occlusion (finger covering, case)
 * - Audio focus interruptions
 *
 * Device: Samsung Galaxy A12 (SM-A125U) Android 10/11
 */
public class AudioEnvironmentRealismHook {
    private static final String TAG = "AudioEnvironmentRealism";

    // Configuration keys
    private static final String KEY_ENABLED = "audio_env_enabled";
    private static final String KEY_NOISE_SIMULATION = "audio_env_noise";
    private static final String KEY_ADAPTIVE_VOLUME = "audio_env_adaptive_volume";
    private static final String KEY_OCCLUSION_SIMULATION = "audio_env_occlusion";

    // Environment profiles (dB SPL)
    public enum AcousticEnvironment {
        QUIET_ROOM(30, 5),      // 30dB ±5dB
        OFFICE(55, 8),          // 55dB ±8dB
        CAFE(70, 10),           // 70dB ±10dB
        STREET(75, 12),         // 75dB ±12dB
        CONSTRUCTION(90, 15);   // 90dB ±15dB

        final float noiseFloor;
        final float variance;

        AcousticEnvironment(float floor, float var) {
            this.noiseFloor = floor;
            this.variance = var;
        }
    }

    // State
    private static SharedPreferences sPrefs;
    private static boolean sEnabled = true;
    private static boolean sNoiseSimulation = true;
    private static boolean sAdaptiveVolume = true;
    private static boolean sOcclusionSimulation = true;

    // Runtime state
    private static final Random sRandom = new Random();
    private static AcousticEnvironment sCurrentEnvironment = AcousticEnvironment.QUIET_ROOM;
    private static float sCurrentNoiseLevel = 30f;
    private static boolean sIsMicrophoneOccluded = false;
    private static float sOcclusionProbability = 0.02f;

    // Microphone characteristics (SM-A125U)
    private static final int MIC_SENSITIVITY_DBV = -42;
    private static final int MIC_SELF_NOISE_DBA = 28;
    private static final int SAMPLE_RATE = 48000;

    public static void init(SharedPreferences prefs) {
        sPrefs = prefs;
        reloadSettings();
    }

    public static void reloadSettings() {
        if (sPrefs == null) return;
        sEnabled = sPrefs.getBoolean(KEY_ENABLED, true);
        sNoiseSimulation = sPrefs.getBoolean(KEY_NOISE_SIMULATION, true);
        sAdaptiveVolume = sPrefs.getBoolean(KEY_ADAPTIVE_VOLUME, true);
        sOcclusionSimulation = sPrefs.getBoolean(KEY_OCCLUSION_SIMULATION, true);
    }

    /**
     * Hook AudioManager for adaptive volume
     */
    public static void hookAudioManager(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!sEnabled || !sAdaptiveVolume) return;

        try {
            Class<?> audioManagerClass = XposedHelpers.findClass(
                "android.media.AudioManager", lpparam.classLoader);

            // Hook getStreamVolume() to apply adaptive adjustment
            XposedBridge.hookAllMethods(audioManagerClass, "getStreamVolume",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!sEnabled || !sAdaptiveVolume) return;

                        int streamType = (Integer) param.args[0];
                        int originalVolume = (Integer) param.getResult();

                        float adjustment = calculateVolumeAdjustment(streamType);
                        int adjustedVolume = Math.round(originalVolume * adjustment);

                        int maxVolume = getStreamMaxVolume(streamType);
                        param.setResult(Math.max(0, Math.min(maxVolume, adjustedVolume)));
                    }
                });

            // Hook requestAudioFocus() for interruption simulation
            XposedBridge.hookAllMethods(audioManagerClass, "requestAudioFocus",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!sEnabled) return;

                        if (shouldSimulateFocusLoss()) {
                            simulateAudioFocusLoss(param);
                        }
                    }
                });

            XposedBridge.log(TAG + ": AudioManager hooks installed");

        } catch (Exception e) {
            XposedBridge.log(TAG + ": Error hooking AudioManager: " + e.getMessage());
        }
    }

    /**
     * Hook AudioRecord for microphone noise injection
     */
    public static void hookAudioRecord(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!sEnabled || !sNoiseSimulation) return;

        try {
            Class<?> audioRecordClass = XposedHelpers.findClass(
                "android.media.AudioRecord", lpparam.classLoader);

            // Hook read() methods to inject environmental noise
            XposedBridge.hookAllMethods(audioRecordClass, "read",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!sEnabled || !sNoiseSimulation) return;

                        if (sOcclusionSimulation && shouldOccludeMicrophone()) {
                            simulateMicrophoneOcclusion(param);
                        }

                        applyEnvironmentalNoise(param);
                    }
                });

            // Hook startRecording() to initialize noise simulation
            XposedBridge.hookAllMethods(audioRecordClass, "startRecording",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!sEnabled) return;

                        updateAcousticEnvironment();
                        sIsMicrophoneOccluded = sOcclusionSimulation &&
                            sRandom.nextFloat() < sOcclusionProbability;

                        XposedBridge.log(TAG + ": Recording started - Environment: " +
                            sCurrentEnvironment + ", Occluded: " + sIsMicrophoneOccluded);
                    }
                });

            XposedBridge.log(TAG + ": AudioRecord hooks installed");

        } catch (Exception e) {
            XposedBridge.log(TAG + ": Error hooking AudioRecord: " + e.getMessage());
        }
    }

    /**
     * Hook MediaRecorder for recording simulation
     */
    public static void hookMediaRecorder(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!sEnabled) return;

        try {
            Class<?> mediaRecorderClass = XposedHelpers.findClass(
                "android.media.MediaRecorder", lpparam.classLoader);

            // Hook getMaxAmplitude() for realistic amplitude variation
            XposedBridge.hookAllMethods(mediaRecorderClass, "getMaxAmplitude",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!sEnabled || !sNoiseSimulation) return;

                        int originalAmplitude = (Integer) param.getResult();
                        float snr = calculateSignalToNoiseRatio();
                        float modulatedAmplitude = originalAmplitude * snr;

                        param.setResult((int) modulatedAmplitude);
                    }
                });

            XposedBridge.log(TAG + ": MediaRecorder hooks installed");

        } catch (Exception e) {
            XposedBridge.log(TAG + ": Error hooking MediaRecorder: " + e.getMessage());
        }
    }

    private static float calculateVolumeAdjustment(int streamType) {
        float noiseFactor = (sCurrentNoiseLevel - 30f) / 60f;
        noiseFactor = Math.max(0, Math.min(1, noiseFactor));

        switch (streamType) {
            case AudioManager.STREAM_MUSIC:
                return 1.0f + (noiseFactor * 0.3f);
            case AudioManager.STREAM_VOICE_CALL:
                return 1.0f + (noiseFactor * 0.4f);
            case AudioManager.STREAM_NOTIFICATION:
                return 1.0f + (noiseFactor * 0.5f);
            default:
                return 1.0f + (noiseFactor * 0.2f);
        }
    }

    private static boolean shouldSimulateFocusLoss() {
        return sRandom.nextFloat() < 0.05f;
    }

    private static void simulateAudioFocusLoss(XC_MethodHook.MethodHookParam param) {
        XposedBridge.log(TAG + ": Simulating transient audio focus loss");
    }

    private static boolean shouldOccludeMicrophone() {
        return sIsMicrophoneOccluded;
    }

    private static void simulateMicrophoneOcclusion(XC_MethodHook.MethodHookParam param) {
        byte[] buffer = (byte[]) param.args[0];
        if (buffer != null) {
            float occlusionFactor = 0.1f + (sRandom.nextFloat() * 0.2f);
            for (int i = 0; i < buffer.length; i += 2) {
                if (i + 1 < buffer.length) {
                    short sample = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xFF));
                    sample = (short) (sample * occlusionFactor);
                    buffer[i] = (byte) (sample & 0xFF);
                    buffer[i + 1] = (byte) ((sample >> 8) & 0xFF);
                }
            }
        }
    }

    private static void applyEnvironmentalNoise(XC_MethodHook.MethodHookParam param) {
        byte[] buffer = (byte[]) param.args[0];
        if (buffer == null) return;

        float noiseLevel = sCurrentEnvironment.noiseFloor / 100f;

        for (int i = 0; i < buffer.length; i += 2) {
            if (i + 1 < buffer.length) {
                short sample = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xFF));
                float noise = (float) (sRandom.nextGaussian() * noiseLevel * 50);
                sample = (short) Math.max(Short.MIN_VALUE,
                    Math.min(Short.MAX_VALUE, sample + noise));
                buffer[i] = (byte) (sample & 0xFF);
                buffer[i + 1] = (byte) ((sample >> 8) & 0xFF);
            }
        }
    }

    private static float calculateSignalToNoiseRatio() {
        float signalLevel = 70f;
        float noiseLevel = sCurrentEnvironment.noiseFloor;
        float snrDb = signalLevel - noiseLevel;
        return (float) Math.pow(10, snrDb / 20);
    }

    private static void updateAcousticEnvironment() {
        long currentTime = System.currentTimeMillis();
        int hour = (int) ((currentTime / (1000 * 60 * 60)) % 24);

        if (hour >= 9 && hour <= 18) {
            AcousticEnvironment[] dayEnvs = {
                AcousticEnvironment.OFFICE, AcousticEnvironment.CAFE,
                AcousticEnvironment.STREET
            };
            sCurrentEnvironment = dayEnvs[sRandom.nextInt(dayEnvs.length)];
        } else {
            sCurrentEnvironment = AcousticEnvironment.QUIET_ROOM;
        }

        sCurrentNoiseLevel = sCurrentEnvironment.noiseFloor +
            (float) (sRandom.nextGaussian() * sCurrentEnvironment.variance);
    }

    private static int getStreamMaxVolume(int streamType) {
        switch (streamType) {
            case AudioManager.STREAM_MUSIC: return 15;
            case AudioManager.STREAM_VOICE_CALL: return 7;
            case AudioManager.STREAM_RING: return 7;
            case AudioManager.STREAM_NOTIFICATION: return 7;
            case AudioManager.STREAM_ALARM: return 7;
            default: return 15;
        }
    }

    public static void setAcousticEnvironment(AcousticEnvironment env) {
        sCurrentEnvironment = env;
        sCurrentNoiseLevel = env.noiseFloor;
    }

    public static AudioEnvironmentState getState() {
        AudioEnvironmentState state = new AudioEnvironmentState();
        state.enabled = sEnabled;
        state.currentEnvironment = sCurrentEnvironment;
        state.noiseLevel = sCurrentNoiseLevel;
        state.microphoneOccluded = sIsMicrophoneOccluded;
        return state;
    }

    public static class AudioEnvironmentState {
        public boolean enabled;
        public AcousticEnvironment currentEnvironment;
        public float noiseLevel;
        public boolean microphoneOccluded;
    }
}
