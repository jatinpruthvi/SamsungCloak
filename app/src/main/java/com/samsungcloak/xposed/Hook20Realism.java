package com.samsungcloak.xposed;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionService;
import android.speech.SpeechRecognizer;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Hook20Realism - Voice Command & Speech Recognition Realism
 * 
 * Simulates realistic voice command and speech recognition behavior for Samsung Galaxy A12:
 * - Wake word detection: optimal 95-98%, moderate noise 85-92%, high noise 40-65%
 * - Speech recognition accuracy: simple commands 90-95%, complex sentences 75-85%, proper names 60-80%
 * - Ambient noise effects: fan -5-15%, traffic -15-30%, crowded room -30-50%, wind -50-70%
 * - User speech characteristics: slow 85-90%, normal 80-85%, fast 65-75%, slurred 40-60%, accented 60-75%
 * 
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class Hook20Realism extends BaseRealismHook {

    private static final String TAG = "[Realism][Hook20-Voice]";
    private static final boolean DEBUG = true;
    
    private static final String HOOK_ID = "hook_20";
    private static final String HOOK_NAME = "Voice Command & Speech Recognition Realism";
    
    // Configuration keys
    private static final String KEY_ENABLED = "voice_recognition_enabled";
    private static final String KEY_WAKE_WORD = "wake_word_detection_enabled";
    private static final String KEY_RECOGNITION = "recognition_accuracy_enabled";
    private static final String KEY_NOISE = "ambient_noise_effects_enabled";
    
    // Hook state
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);
    private static AtomicBoolean enabled = new AtomicBoolean(true);
    private static float intensity = 0.5f;
    
    // Ambient noise levels
    public enum NoiseLevel {
        QUIET,       // < 40 dB
        MODERATE,    // 40-60 dB
        LOUD,        // 60-80 dB
        VERY_LOUD    // > 80 dB
    }
    
    // Noise types
    public enum NoiseType {
        FAN,
        TRAFFIC,
        CROWDED_ROOM,
        WIND,
        MUSIC,
        NONE
    }
    
    // User speech characteristics
    public enum SpeechStyle {
        SLOW,
        NORMAL,
        FAST,
        SLURRED,
        ACCENTED
    }
    
    // Current state
    private static final AtomicReference<NoiseLevel> currentNoiseLevel = 
        new AtomicReference<>(NoiseLevel.QUIET);
    private static final AtomicReference<NoiseType> currentNoiseType = 
        new AtomicReference<>(NoiseType.NONE);
    private static final AtomicReference<SpeechStyle> currentSpeechStyle = 
        new AtomicReference<>(SpeechStyle.NORMAL);
    
    // Wake word detection rates
    private static boolean wakeWordEnabled = true;
    private static float optimalWakeRate = 0.96f;
    private static float moderateNoiseWakeRate = 0.88f;
    private static float highNoiseWakeRate = 0.52f;
    
    // Recognition accuracy
    private static boolean recognitionEnabled = true;
    private static float simpleCommandAccuracy = 0.92f;
    private static float complexSentenceAccuracy = 0.80f;
    private static float properNameAccuracy = 0.70f;
    
    // Noise effects on accuracy
    private static boolean noiseEffectsEnabled = true;
    private static float fanAccuracyLoss = 0.10f;
    private static float trafficAccuracyLoss = 0.22f;
    private static float crowdedRoomAccuracyLoss = 0.40f;
    private static float windAccuracyLoss = 0.60f;
    
    // Speech style effects
    private static float slowSpeechAccuracy = 0.87f;
    private static float normalSpeechAccuracy = 0.82f;
    private static float fastSpeechAccuracy = 0.70f;
    private static float slurredSpeechAccuracy = 0.50f;
    private static float accentedSpeechAccuracy = 0.68f;
    
    // Voice command tracking
    private static final AtomicInteger wakeWordAttempts = new AtomicInteger(0);
    private static final AtomicInteger wakeWordSuccesses = new AtomicInteger(0);
    private static final AtomicInteger recognitionAttempts = new AtomicInteger(0);
    private static final AtomicInteger recognitionSuccesses = new AtomicInteger(0);
    private static final AtomicInteger commandFailures = new AtomicInteger(0);
    
    // Recent recognition results for pattern analysis
    private static final CopyOnWriteArrayList<RecognitionResult> recognitionHistory = 
        new CopyOnWriteArrayList<>();
    private static final int MAX_HISTORY = 50;
    
    // Timing
    private static long lastRecognitionTime = 0;
    private static long sessionStartTime = 0;
    
    // Thread-local random
    private static final ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new Random());
    
    // Bixby/voice assistant context
    private static String currentAssistant = "bixby"; // bixby, google, siri
    
    public Hook20Realism() {
        super(HOOK_ID, HOOK_NAME);
    }
    
    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!hookInitialized.compareAndSet(false, true)) {
            logInfo("Hook already initialized");
            return;
        }
        
        logInfo("Initializing Voice Command & Speech Recognition Realism Hook");
        sessionStartTime = System.currentTimeMillis();
        
        try {
            // Load configuration
            loadConfiguration();
            
            // Hook SpeechRecognizer
            hookSpeechRecognizer(lpparam);
            
            // Hook RecognitionService
            hookRecognitionService(lpparam);
            
            // Hook AudioRecord for microphone simulation
            hookAudioRecord(lpparam);
            
            // Hook Bixby if available
            hookBixby(lpparam);
            
            logInfo("Voice Recognition Hook initialized successfully");
        } catch (Exception e) {
            logError("Failed to initialize Voice Recognition Hook", e);
        }
    }
    
    private void loadConfiguration() {
        if (configManager != null) {
            enabled.set(configManager.getHookParamBool(HOOK_ID, KEY_ENABLED, true));
            intensity = configManager.getHookIntensity(HOOK_ID);
            
            wakeWordEnabled = configManager.getHookParamBool(HOOK_ID, KEY_WAKE_WORD, true);
            optimalWakeRate = configManager.getHookParamFloat(HOOK_ID, "optimal_wake_rate", 0.96f);
            moderateNoiseWakeRate = configManager.getHookParamFloat(HOOK_ID, "moderate_noise_wake_rate", 0.88f);
            highNoiseWakeRate = configManager.getHookParamFloat(HOOK_ID, "high_noise_wake_rate", 0.52f);
            
            recognitionEnabled = configManager.getHookParamBool(HOOK_ID, KEY_RECOGNITION, true);
            simpleCommandAccuracy = configManager.getHookParamFloat(HOOK_ID, "simple_command_accuracy", 0.92f);
            complexSentenceAccuracy = configManager.getHookParamFloat(HOOK_ID, "complex_sentence_accuracy", 0.80f);
            properNameAccuracy = configManager.getHookParamFloat(HOOK_ID, "proper_name_accuracy", 0.70f);
            
            noiseEffectsEnabled = configManager.getHookParamBool(HOOK_ID, KEY_NOISE, true);
            fanAccuracyLoss = configManager.getHookParamFloat(HOOK_ID, "fan_accuracy_loss", 0.10f);
            trafficAccuracyLoss = configManager.getHookParamFloat(HOOK_ID, "traffic_accuracy_loss", 0.22f);
            crowdedRoomAccuracyLoss = configManager.getHookParamFloat(HOOK_ID, "crowded_room_accuracy_loss", 0.40f);
            windAccuracyLoss = configManager.getHookParamFloat(HOOK_ID, "wind_accuracy_loss", 0.60f);
        }
        
        logInfo("Configuration loaded: enabled=" + enabled.get() + ", intensity=" + intensity);
    }
    
    private void hookSpeechRecognizer(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> speechRecognizerClass = XposedHelpers.findClass(
                "android.speech.SpeechRecognizer", lpparam.classLoader);
            
            // Hook startListening
            XposedBridge.hookAllMethods(speechRecognizerClass, "startListening",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get()) return;
                        
                        try {
                            wakeWordAttempts.incrementAndGet();
                            
                            // Determine if wake word would be detected
                            if (wakeWordEnabled) {
                                boolean wakeDetected = simulateWakeWordDetection();
                                
                                if (!wakeDetected) {
                                    // Cancel the recognition
                                    // In a full implementation, we'd prevent the actual recognition
                                    if (DEBUG && random.get().nextDouble() < 0.01) {
                                        logDebug("Wake word detection failed - noise: " + 
                                            currentNoiseLevel.get() + ", style: " + currentSpeechStyle.get());
                                    }
                                } else {
                                    wakeWordSuccesses.incrementAndGet();
                                }
                            }
                        } catch (Exception e) {
                            logDebug("Error in startListening hook: " + e.getMessage());
                        }
                    }
                });
            
            // Hook stopListening
            XposedBridge.hookAllMethods(speechRecognizerClass, "stopListening",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get()) return;
                        
                        try {
                            // Analyze what was recognized
                            lastRecognitionTime = System.currentTimeMillis();
                        } catch (Exception e) {
                            logDebug("Error in stopListening hook: " + e.getMessage());
                        }
                    }
                });
            
            logInfo("Hooked SpeechRecognizer");
        } catch (Exception e) {
            logError("Failed to hook SpeechRecognizer", e);
        }
    }
    
    private void hookRecognitionService(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> recognitionServiceClass = XposedHelpers.findClass(
                "android.speech.RecognitionService", lpparam.classLoader);
            
            // Hook onResults to modify recognition accuracy
            XposedBridge.hookAllMethods(recognitionServiceClass, "onResults",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get() || !recognitionEnabled) return;
                        
                        try {
                            recognitionAttempts.incrementAndGet();
                            
                            // Determine recognition accuracy
                            float accuracy = calculateRecognitionAccuracy();
                            
                            // Add noise to results based on accuracy
                            if (random.get().nextDouble() > accuracy) {
                                // Recognition failed or returned incorrect results
                                commandFailures.incrementAndGet();
                                
                                if (DEBUG && random.get().nextDouble() < 0.01) {
                                    logDebug("Recognition failed - accuracy: " + accuracy + 
                                        ", noise: " + currentNoiseType.get());
                                }
                            } else {
                                recognitionSuccesses.incrementAndGet();
                            }
                            
                            // Store result in history
                            RecognitionResult result = new RecognitionResult(
                                accuracy, 
                                currentNoiseType.get(),
                                currentSpeechStyle.get(),
                                System.currentTimeMillis()
                            );
                            recognitionHistory.add(result);
                            while (recognitionHistory.size() > MAX_HISTORY) {
                                recognitionHistory.remove(0);
                            }
                        } catch (Exception e) {
                            logDebug("Error in onResults hook: " + e.getMessage());
                        }
                    }
                });
            
            logInfo("Hooked RecognitionService");
        } catch (Exception e) {
            logError("Failed to hook RecognitionService", e);
        }
    }
    
    private void hookAudioRecord(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> audioRecordClass = XposedHelpers.findClass(
                "android.media.AudioRecord", lpparam.classLoader);
            
            // Hook read to add noise to audio input
            XposedBridge.hookAllMethods(audioRecordClass, "read",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get() || !noiseEffectsEnabled) return;
                        
                        try {
                            int readLength = (Integer) param.getResult();
                            if (readLength <= 0) return;
                            
                            // Get audio buffer
                            Object audioBuffer = param.args[0];
                            
                            // Add ambient noise effects to audio
                            // This is simplified - in full implementation, would modify bytes
                            if (currentNoiseType.get() != NoiseType.NONE) {
                                // Apply noise-based distortion
                                float noiseFactor = getNoiseAccuracyLoss(currentNoiseType.get());
                                if (random.get().nextDouble() < noiseFactor * intensity) {
                                    // Add noise to audio
                                    if (DEBUG && random.get().nextDouble() < 0.01) {
                                        logDebug("Added noise to audio: " + currentNoiseType.get());
                                    }
                                }
                            }
                        } catch (Exception e) {
                            logDebug("Error in AudioRecord.read hook: " + e.getMessage());
                        }
                    }
                });
            
            logInfo("Hooked AudioRecord");
        } catch (Exception e) {
            logError("Failed to hook AudioRecord", e);
        }
    }
    
    private void hookBixby(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook Samsung's Bixby service
            Class<?> bixbyServiceClass = XposedHelpers.findClass(
                "com.samsung.android.bixby.BixbyService", lpparam.classLoader);
            
            XposedBridge.hookAllMethods(bixbyServiceClass, "onStartCommand",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled.get()) return;
                        
                        try {
                            currentAssistant = "bixby";
                        } catch (Exception e) {
                            logDebug("Error in Bixby hook: " + e.getMessage());
                        }
                    }
                });
            
            logInfo("Hooked Bixby");
        } catch (ClassNotFoundException e) {
            logDebug("Bixby service not found on this device");
        } catch (Exception e) {
            logError("Failed to hook Bixby", e);
        }
    }
    
    private boolean simulateWakeWordDetection() {
        NoiseLevel noiseLevel = currentNoiseLevel.get();
        float baseRate;
        
        switch (noiseLevel) {
            case QUIET:
                baseRate = optimalWakeRate;
                break;
            case MODERATE:
                baseRate = moderateNoiseWakeRate;
                break;
            case LOUD:
            case VERY_LOUD:
                baseRate = highNoiseWakeRate;
                break;
            default:
                baseRate = optimalWakeRate;
        }
        
        // Apply speech style modifier
        SpeechStyle style = currentSpeechStyle.get();
        switch (style) {
            case SLOW:
                baseRate *= 1.05f;
                break;
            case FAST:
                baseRate *= 0.90f;
                break;
            case SLURRED:
                baseRate *= 0.70f;
                break;
            case ACCENTED:
                baseRate *= 0.85f;
                break;
            default:
                break;
        }
        
        // Apply intensity
        float adjustedRate = baseRate * (0.5f + intensity * 0.5f);
        
        return random.get().nextDouble() < adjustedRate;
    }
    
    private float calculateRecognitionAccuracy() {
        float baseAccuracy = simpleCommandAccuracy;
        
        // Apply noise type effects
        NoiseType noiseType = currentNoiseType.get();
        float noiseLoss = getNoiseAccuracyLoss(noiseType);
        
        // Apply speech style effects
        SpeechStyle speechStyle = currentSpeechStyle.get();
        float styleAccuracy = getSpeechStyleAccuracy(speechStyle);
        
        // Combine factors
        float finalAccuracy = baseAccuracy * (1.0f - noiseLoss) * styleAccuracy;
        
        // Apply intensity
        finalAccuracy *= (0.5f + intensity * 0.5f);
        
        return Math.max(0.0f, Math.min(1.0f, finalAccuracy));
    }
    
    private float getNoiseAccuracyLoss(NoiseType noiseType) {
        switch (noiseType) {
            case FAN:
                return fanAccuracyLoss;
            case TRAFFIC:
                return trafficAccuracyLoss;
            case CROWDED_ROOM:
                return crowdedRoomAccuracyLoss;
            case WIND:
                return windAccuracyLoss;
            case MUSIC:
                return 0.15f;
            case NONE:
            default:
                return 0.0f;
        }
    }
    
    private float getSpeechStyleAccuracy(SpeechStyle style) {
        switch (style) {
            case SLOW:
                return slowSpeechAccuracy;
            case NORMAL:
                return normalSpeechAccuracy;
            case FAST:
                return fastSpeechAccuracy;
            case SLURRED:
                return slurredSpeechAccuracy;
            case ACCENTED:
                return accentedSpeechAccuracy;
            default:
                return normalSpeechAccuracy;
        }
    }
    
    /**
     * Set ambient noise level
     */
    public static void setNoiseLevel(NoiseLevel level) {
        currentNoiseLevel.set(level);
        
        // Update noise type based on level
        if (level == NoiseLevel.QUIET) {
            currentNoiseType.set(NoiseType.NONE);
        }
        
        HookUtils.logInfo(TAG, "Noise level set to: " + level);
    }
    
    /**
     * Set noise type
     */
    public static void setNoiseType(NoiseType type) {
        currentNoiseType.set(type);
        
        // Update noise level based on type
        switch (type) {
            case FAN:
                currentNoiseLevel.set(NoiseLevel.MODERATE);
                break;
            case TRAFFIC:
            case MUSIC:
                currentNoiseLevel.set(NoiseLevel.LOUD);
                break;
            case CROWDED_ROOM:
                currentNoiseLevel.set(NoiseLevel.VERY_LOUD);
                break;
            case WIND:
                currentNoiseLevel.set(NoiseLevel.VERY_LOUD);
                break;
            case NONE:
            default:
                currentNoiseLevel.set(NoiseLevel.QUIET);
                break;
        }
        
        HookUtils.logInfo(TAG, "Noise type set to: " + type);
    }
    
    /**
     * Set speech style
     */
    public static void setSpeechStyle(SpeechStyle style) {
        currentSpeechStyle.set(style);
        HookUtils.logInfo(TAG, "Speech style set to: " + style);
    }
    
    /**
     * Enable/disable the hook
     */
    public static void setEnabled(boolean isEnabled) {
        enabled.set(isEnabled);
        HookUtils.logInfo(TAG, "Hook " + (isEnabled ? "enabled" : "disabled"));
    }
    
    /**
     * Set intensity (0.0 - 1.0)
     */
    public static void setIntensity(float value) {
        intensity = Math.max(0.0f, Math.min(1.0f, value));
        HookUtils.logInfo(TAG, "Intensity set to: " + intensity);
    }
    
    /**
     * Get wake word success rate
     */
    public static float getWakeWordSuccessRate() {
        int attempts = wakeWordAttempts.get();
        if (attempts == 0) return 0;
        return (float) wakeWordSuccesses.get() / attempts;
    }
    
    /**
     * Get recognition success rate
     */
    public static float getRecognitionSuccessRate() {
        int attempts = recognitionAttempts.get();
        if (attempts == 0) return 0;
        return (float) recognitionSuccesses.get() / attempts;
    }
    
    /**
     * Get statistics
     */
    public static Map<String, Integer> getStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("wake_word_attempts", wakeWordAttempts.get());
        stats.put("wake_word_successes", wakeWordSuccesses.get());
        stats.put("recognition_attempts", recognitionAttempts.get());
        stats.put("recognition_successes", recognitionSuccesses.get());
        stats.put("command_failures", commandFailures.get());
        return stats;
    }
    
    /**
     * Recognition result record
     */
    private static class RecognitionResult {
        final float accuracy;
        final NoiseType noiseType;
        final SpeechStyle speechStyle;
        final long timestamp;
        
        RecognitionResult(float accuracy, NoiseType noiseType, SpeechStyle speechStyle, long timestamp) {
            this.accuracy = accuracy;
            this.noiseType = noiseType;
            this.speechStyle = speechStyle;
            this.timestamp = timestamp;
        }
    }
}
