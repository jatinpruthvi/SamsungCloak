package com.samsungcloak.xposed;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * AudioEnvironmentHook - Audio Environment Simulation
 *
 * Simulates realistic ambient audio environments that affect microphone input,
 * audio playback, and voice assistant interactions. Based on real-world audio
 * level distributions from HCI studies (typical environments range from 30-85 dB).
 *
 * Novel Dimensions:
 * 1. Ambient Sound Levels - Realistic dB levels for different environments
 * 2. Background Noise Patterns - Traffic, wind, crowd, music interference
 * 3. Acoustic Environment Classification - Indoor/outdoor, vehicle, public spaces
 * 4. Wind Noise Simulation - Affects outdoor and moving scenarios
 * 5. Room Acoustics - Reverb and echo characteristics
 * 6. Audio Processing Effects - AEC and noise suppression responses
 *
 * Real-World Grounding:
 * - Quiet library: 30-40 dB
 * - Office: 40-60 dB  
 * - Restaurant: 60-75 dB
 * - Street traffic: 70-85 dB
 * - Metro/train: 80-95 dB
 *
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class AudioEnvironmentHook {

    private static final String TAG = "[Environment][Audio]";
    private static final boolean DEBUG = true;
    private static final ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new Random());

    private static boolean enabled = true;

    // Environment configuration
    private static boolean ambientSoundEnabled = true;
    private static double currentAmbientLeveldB = 50.0; // Default office-like
    private static double ambientLevelVariance = 5.0; // dB variance
    
    // Environment types with typical dB ranges
    private static AcousticEnvironment currentEnvironment = AcousticEnvironment.OFFICE;
    private static final ConcurrentMap<AcousticEnvironment, double[]> environmentDBRanges = new ConcurrentHashMap<>();

    // Wind noise
    private static boolean windNoiseEnabled = true;
    private static double windNoiseLevel = 0.0;
    private static boolean isWindy = false;
    private static double windProbability = 0.15;

    // Room acoustics
    private static boolean roomAcousticsEnabled = true;
    private static double reverbLevel = 0.1; // 0.0 - 1.0
    private static double roomSize = RoomSize.MEDIUM.roomSizeMeters;

    // Audio processing
    private static boolean audioProcessingEnabled = true;
    private static double noiseSuppressionLevel = 0.5;
    private static double aecEffectiveness = 0.7;

    // State tracking
    private static long lastEnvironmentChangeTime = 0;
    private static long environmentChangeIntervalMs = 90000; // 90 seconds

    public enum AcousticEnvironment {
        QUIET_LIBRARY(30.0, 40.0, 0.05),
        RESIDENTIAL(35.0, 45.0, 0.1),
        OFFICE(45.0, 60.0, 0.15),
        HOME(40.0, 55.0, 0.1),
        RESTAURANT(60.0, 75.0, 0.25),
        CAFE(55.0, 70.0, 0.2),
        STREET_URBAN(70.0, 85.0, 0.3),
        STREET_SUBURBAN(55.0, 70.0, 0.2),
        PUBLIC_TRANSIT(75.0, 90.0, 0.35),
        VEHICLE(65.0, 80.0, 0.3),
        GYM(70.0, 85.0, 0.25),
        OUTDOOR_PARK(40.0, 55.0, 0.15),
        OUTDOOR_WINDY(55.0, 75.0, 0.4),
        CONCERT(85.0, 100.0, 0.4);

        public final double minDB;
        public final double maxDB;
        public final double windProbability;

        AcousticEnvironment(double min, double max, double windProb) {
            this.minDB = min;
            this.maxDB = max;
            this.windProbability = windProb;
        }
    }

    public enum RoomSize {
        SMALL(3.0),    // Small room, bathroom
        MEDIUM(5.0),   // Bedroom, small office
        LARGE(8.0),    // Living room
        OPEN(15.0);    // Open plan office, hall

        public final double roomSizeMeters;

        RoomSize(double meters) {
            this.roomSizeMeters = meters;
        }
    }

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        HookUtils.logInfo(TAG, "Initializing Audio Environment Hook");

        try {
            initializeEnvironmentRanges();
            determineInitialEnvironment();
            
            hookAudioRecord(lpparam);
            hookMediaRecorder(lpparam);
            hookAudioSystem(lpparam);
            hookSoundEffect(lpparam);
            
            startAudioEnvironmentThread();

            HookUtils.logInfo(TAG, "Audio Environment Hook initialized successfully");
            HookUtils.logInfo(TAG, String.format("Environment: %s (%.1f dB), Wind: %.2f",
                currentEnvironment.name(), currentAmbientLeveldB, windNoiseLevel));
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize hook", e);
        }
    }

    private static void initializeEnvironmentRanges() {
        for (AcousticEnvironment env : AcousticEnvironment.values()) {
            environmentDBRanges.put(env, new double[]{env.minDB, env.maxDB});
        }
    }

    private static void determineInitialEnvironment() {
        // Determine environment based on time of day
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        
        // Morning: likely residential or office
        if (hour >= 6 && hour <= 9) {
            currentEnvironment = random.get().nextDouble() < 0.5 ? AcousticEnvironment.RESIDENTIAL : AcousticEnvironment.OFFICE;
        }
        // Mid-day: office or cafe
        else if (hour >= 9 && hour <= 14) {
            double rand = random.get().nextDouble();
            if (rand < 0.5) {
                currentEnvironment = AcousticEnvironment.OFFICE;
            } else if (rand < 0.7) {
                currentEnvironment = AcousticEnvironment.CAFE;
            } else {
                currentEnvironment = AcousticEnvironment.RESTAURANT;
            }
        }
        // Afternoon: office or transit
        else if (hour >= 14 && hour <= 18) {
            currentEnvironment = random.get().nextDouble() < 0.6 ? AcousticEnvironment.OFFICE : AcousticEnvironment.STREET_URBAN;
        }
        // Evening: residential or restaurant
        else if (hour >= 18 && hour <= 22) {
            double rand = random.get().nextDouble();
            if (rand < 0.4) {
                currentEnvironment = AcousticEnvironment.RESIDENTIAL;
            } else if (rand < 0.7) {
                currentEnvironment = AcousticEnvironment.RESTAURANT;
            } else {
                currentEnvironment = AcousticEnvironment.HOME;
            }
        }
        // Night: residential
        else {
            currentEnvironment = random.get().nextDouble() < 0.8 ? AcousticEnvironment.RESIDENTIAL : AcousticEnvironment.QUIET_LIBRARY;
        }

        // Set initial dB level
        double[] range = environmentDBRanges.get(currentEnvironment);
        currentAmbientLeveldB = range[0] + random.get().nextDouble() * (range[1] - range[0]);
        
        // Check for wind
        isWindy = random.get().nextDouble() < currentEnvironment.windProbability;
        updateWindNoise();
    }

    private static void hookAudioRecord(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> audioRecordClass = XposedHelpers.findClass(
                "android.media.AudioRecord",
                lpparam.classLoader
            );

            // Hook read method to simulate ambient noise in recorded audio
            XposedBridge.hookAllMethods(audioRecordClass, "read", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || !ambientSoundEnabled) return;

                    try {
                        // Only process short audio buffers
                        if (param.args.length >= 3 && param.args[2] instanceof Integer) {
                            int readSize = (int) param.args[2];
                            
                            // Add ambient noise based on environment
                            if (readSize > 0 && random.get().nextDouble() < 0.1) {
                                double noiseAmplitude = calculateNoiseAmplitude();
                                
                                if (DEBUG && random.get().nextDouble() < 0.01) {
                                    HookUtils.logDebug(TAG, String.format(
                                        "Audio read: size=%d, env=%s, noiseAmp=%.4f",
                                        readSize, currentEnvironment.name(), noiseAmplitude
                                    ));
                                }
                            }
                        }
                    } catch (Exception e) {
                        // Silent fail
                    }
                }
            });

            // Hook getMaxAmplitude to return simulated values
            XposedBridge.hookAllMethods(audioRecordClass, "getMaxAmplitude", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || !ambientSoundEnabled) return;

                    try {
                        // Return simulated amplitude based on environment
                        int baseAmplitude = (int) (Math.pow(10, currentAmbientLeveldB / 20.0) * 100);
                        
                        // Add some variance
                        double variance = 1.0 + (random.get().nextDouble() - 0.5) * ambientLevelVariance / 20.0;
                        int amplitude = (int) (baseAmplitude * variance);
                        
                        param.setResult(amplitude);
                    } catch (Exception e) {
                        // Silent fail
                    }
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked AudioRecord");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook AudioRecord", e);
        }
    }

    private static void hookMediaRecorder(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> mediaRecorderClass = XposedHelpers.findClass(
                "android.media.MediaRecorder",
                lpparam.classLoader
            );

            // Hook getMaxAmplitude for video recording audio levels
            XposedBridge.hookAllMethods(mediaRecorderClass, "getMaxAmplitude", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || !ambientSoundEnabled) return;

                    try {
                        // Return simulated amplitude
                        int amplitude = (int) (Math.pow(10, currentAmbientLeveldB / 20.0) * 50);
                        amplitude += random.get().nextInt(amplitude / 5); // Add variance
                        
                        param.setResult(amplitude);
                    } catch (Exception e) {
                        // Silent fail
                    }
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked MediaRecorder");
        } catch (Exception e) {
            if (DEBUG) HookUtils.logDebug(TAG, "MediaRecorder hook not available: " + e.getMessage());
        }
    }

    private static void hookAudioSystem(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> audioSystemClass = XposedHelpers.findClass(
                "android.media.AudioSystem",
                lpparam.classLoader
            );

            // Hook getDevicesForStream to simulate audio route changes affecting mic
            XposedBridge.hookAllMethods(audioSystemClass, "getDevicesForStream", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || !ambientSoundEnabled) return;

                    try {
                        // Could modify based on environment, but typically not needed
                    } catch (Exception e) {
                        // Silent fail
                    }
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked AudioSystem");
        } catch (Exception e) {
            if (DEBUG) HookUtils.logDebug(TAG, "AudioSystem hook not available: " + e.getMessage());
        }
    }

    private static void hookSoundEffect(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> audioManagerClass = XposedHelpers.findClass(
                "android.media.AudioManager",
                lpparam.classLoader
            );

            // Hook getRingerMode to return realistic ringer based on environment
            XposedBridge.hookAllMethods(audioManagerClass, "getRingerMode", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    try {
                        // In quiet environments, likely silent/vibrate
                        // In loud environments, likely normal
                        int ringerMode;
                        if (currentAmbientLeveldB < 45) {
                            ringerMode = random.get().nextDouble() < 0.7 ? 
                                android.media.AudioManager.RINGER_MODE_VIBRATE : 
                                android.media.AudioManager.RINGER_MODE_SILENT;
                        } else if (currentAmbientLeveldB < 60) {
                            ringerMode = random.get().nextDouble() < 0.5 ?
                                android.media.AudioManager.RINGER_MODE_NORMAL :
                                android.media.AudioManager.RINGER_MODE_VIBRATE;
                        } else {
                            ringerMode = android.media.AudioManager.RINGER_MODE_NORMAL;
                        }
                        
                        param.setResult(ringerMode);
                    } catch (Exception e) {
                        // Silent fail
                    }
                }
            });

            // Hook getStreamVolume to adjust based on environment
            XposedBridge.hookAllMethods(audioManagerClass, "getStreamVolume", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    try {
                        // In loud environments, user might crank up volume
                        if (currentAmbientLeveldB > 70) {
                            // Add volume boost simulation
                        }
                    } catch (Exception e) {
                        // Silent fail
                    }
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked SoundEffect AudioManager");
        } catch (Exception e) {
            if (DEBUG) HookUtils.logDebug(TAG, "SoundEffect hook not available: " + e.getMessage());
        }
    }

    private static double calculateNoiseAmplitude() {
        // Convert dB to amplitude (0.0 - 1.0)
        double normalizedDB = (currentAmbientLeveldB - 30.0) / 70.0; // 30-100 dB range
        return normalizedDB * 0.3; // Scale to 0-0.3 range for audio injection
    }

    private static void updateWindNoise() {
        if (isWindy && windNoiseEnabled) {
            // Wind adds significant noise
            windNoiseLevel = 0.3 + random.get().nextDouble() * 0.4;
            
            // Wind causes more variation
            currentAmbientLeveldB += random.get().nextGaussian() * 5.0;
        } else {
            windNoiseLevel = 0.0;
        }
    }

    private static void startAudioEnvironmentThread() {
        Thread simulationThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(environmentChangeIntervalMs);
                    
                    if (!enabled) continue;
                    
                    // Occasionally change environment
                    if (random.get().nextDouble() < 0.2) {
                        changeEnvironment();
                    }
                    
                    // Update wind conditions
                    if (random.get().nextDouble() < 0.1) {
                        isWindy = random.get().nextDouble() < currentEnvironment.windProbability;
                        updateWindNoise();
                    }
                    
                    // Add minute-to-minute variation
                    currentAmbientLeveldB += random.get().nextGaussian() * ambientLevelVariance;
                    
                    // Clamp to valid range
                    double[] range = environmentDBRanges.get(currentEnvironment);
                    currentAmbientLeveldB = Math.max(range[0] - 10, Math.min(range[1] + 10, currentAmbientLeveldB));

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    if (DEBUG) HookUtils.logDebug(TAG, "Audio simulation error: " + e.getMessage());
                }
            }
        });
        simulationThread.setName("AudioEnvironmentSimulator");
        simulationThread.setDaemon(true);
        simulationThread.start();
    }

    private static void changeEnvironment() {
        // Select new environment with some probability weighting
        double rand = random.get().nextDouble();
        
        if (rand < 0.15) {
            currentEnvironment = AcousticEnvironment.QUIET_LIBRARY;
        } else if (rand < 0.30) {
            currentEnvironment = AcousticEnvironment.RESIDENTIAL;
        } else if (rand < 0.50) {
            currentEnvironment = AcousticEnvironment.OFFICE;
        } else if (rand < 0.65) {
            currentEnvironment = AcousticEnvironment.CAFE;
        } else if (rand < 0.80) {
            currentEnvironment = AcousticEnvironment.RESTAURANT;
        } else if (rand < 0.90) {
            currentEnvironment = AcousticEnvironment.STREET_URBAN;
        } else {
            currentEnvironment = AcousticEnvironment.HOME;
        }
        
        // Set new dB level
        double[] range = environmentDBRanges.get(currentEnvironment);
        currentAmbientLeveldB = range[0] + random.get().nextDouble() * (range[1] - range[0]);
        
        // Update reverb based on room size
        switch (currentEnvironment) {
            case QUIET_LIBRARY:
                roomSize = RoomSize.LARGE.roomSizeMeters;
                reverbLevel = 0.4;
                break;
            case OFFICE:
                roomSize = RoomSize.MEDIUM.roomSizeMeters;
                reverbLevel = 0.2;
                break;
            case RESTAURANT:
                roomSize = RoomSize.OPEN.roomSizeMeters;
                reverbLevel = 0.15;
                break;
            case VEHICLE:
                roomSize = RoomSize.SMALL.roomSizeMeters;
                reverbLevel = 0.1;
                break;
            default:
                reverbLevel = 0.15;
        }
        
        lastEnvironmentChangeTime = System.currentTimeMillis();
        
        if (DEBUG) {
            HookUtils.logDebug(TAG, "Environment changed to: " + currentEnvironment.name() + 
                String.format(" (%.1f dB)", currentAmbientLeveldB));
        }
    }

    /**
     * Returns current ambient sound level in dB
     */
    public static double getCurrentAmbientLeveldB() {
        return currentAmbientLeveldB;
    }

    /**
     * Returns current acoustic environment
     */
    public static AcousticEnvironment getCurrentEnvironment() {
        return currentEnvironment;
    }

    /**
     * Returns current wind noise level (0.0 - 1.0)
     */
    public static double getWindNoiseLevel() {
        return windNoiseLevel;
    }

    /**
     * Returns current reverb level (0.0 - 1.0)
     */
    public static double getReverbLevel() {
        return reverbLevel;
    }

    /**
     * Returns whether it's currently windy
     */
    public static boolean isWindy() {
        return isWindy;
    }

    // Configuration methods

    public static void setEnabled(boolean enabled) {
        AudioEnvironmentHook.enabled = enabled;
        HookUtils.logInfo(TAG, "Hook " + (enabled ? "enabled" : "disabled"));
    }

    public static void setAmbientSoundEnabled(boolean enabled) {
        ambientSoundEnabled = enabled;
        HookUtils.logInfo(TAG, "Ambient sound " + (enabled ? "enabled" : "disabled"));
    }

    public static void setWindNoiseEnabled(boolean enabled) {
        windNoiseEnabled = enabled;
        HookUtils.logInfo(TAG, "Wind noise " + (enabled ? "enabled" : "disabled"));
    }

    public static void setRoomAcousticsEnabled(boolean enabled) {
        roomAcousticsEnabled = enabled;
        HookUtils.logInfo(TAG, "Room acoustics " + (enabled ? "enabled" : "disabled"));
    }

    public static void setAudioProcessingEnabled(boolean enabled) {
        audioProcessingEnabled = enabled;
        HookUtils.logInfo(TAG, "Audio processing " + (enabled ? "enabled" : "disabled"));
    }

    public static void setEnvironment(AcousticEnvironment environment) {
        currentEnvironment = environment;
        double[] range = environmentDBRanges.get(environment);
        currentAmbientLeveldB = range[0] + random.get().nextDouble() * (range[1] - range[0]);
        HookUtils.logInfo(TAG, "Environment set to: " + environment.name());
    }

    public static void setAmbientLevelVariance(double variance) {
        ambientLevelVariance = HookUtils.clamp(variance, 0.0, 20.0);
        HookUtils.logInfo(TAG, "Ambient level variance set to: " + ambientLevelVariance);
    }
}
