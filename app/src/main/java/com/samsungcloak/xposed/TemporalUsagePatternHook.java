package com.samsungcloak.xposed;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * TemporalUsagePatternHook - Temporal Usage Pattern Simulation
 *
 * Simulates realistic temporal usage patterns based on human circadian rhythms,
 * behavioral cycles, and time-of-day dependent interaction styles. These patterns
 * are critical for creating believable usage profiles.
 *
 * Novel Dimensions:
 * 1. Circadian Rhythm Effects - Energy and attention vary by time of day
 * 2. Usage Bursts - Rapid multi-app switching and interaction spikes
 * 3. Idle Periods - Natural breaks in usage (meals, meetings, sleep)
 * 4. Day-of-Week Patterns - Weekday vs weekend behavior differences
 * 5. Session Clustering - Usage concentrated in specific time windows
 * 6. Attention Decay - Gradual focus loss during extended sessions
 *
 * Real-World Grounding:
 * - Peak usage: 7-9am, 12-1pm, 7-10pm
 * - Low usage: 1-5am, 9am-12pm (work hours)
 * - Average session length: 5-15 minutes
 * - Session gaps: 2-15 minutes typical
 *
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class TemporalUsagePatternHook {

    private static final String TAG = "[Behavior][Temporal]";
    private static final boolean DEBUG = true;
    private static final ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new Random());

    private static boolean enabled = true;

    // Circadian configuration
    private static boolean circadianEnabled = true;
    private static double morningPeakStart = 7.0; // 7 AM
    private static double eveningPeakStart = 19.0; // 7 PM
    private static double energyDecayRate = 0.15; // Per hour

    // Usage burst configuration
    private static boolean usageBurstEnabled = true;
    private static double burstProbability = 0.08; // 8% chance per minute
    private static int burstMinActions = 3;
    private static int burstMaxActions = 12;
    private static double burstWithinSessionProbability = 0.25;

    // Idle configuration
    private static boolean idlePeriodsEnabled = true;
    private static double idleProbability = 0.15;
    private static long idleMinDurationMs = 60000; // 1 minute
    private static long idleMaxDurationMs = 900000; // 15 minutes

    // Day-of-week configuration
    private static boolean dayOfWeekEnabled = true;
    private static double weekendUsageMultiplier = 1.4;
    private static double weekdayWorkHoursMultiplier = 0.6;

    // Attention decay
    private static boolean attentionDecayEnabled = true;
    private static double attentionDecayRate = 0.02; // Per minute
    private static double attentionRecoveryRate = 0.15; // Per minute of idle
    private static double maxAttentionDecay = 0.7; // Max 70% decay

    // Session clustering
    private static boolean sessionClusteringEnabled = true;
    private static final List<UsageCluster> usageClusters = new CopyOnWriteArrayList<>();

    // State tracking
    private static double currentEnergyLevel = 1.0;
    private static double currentAttentionLevel = 1.0;
    private static long sessionStartTime = 0;
    private static long lastInteractionTime = 0;
    private static boolean isInBurst = false;
    private static int currentBurstActions = 0;
    private static UsagePhase currentPhase = UsagePhase.ACTIVE;
    private static int consecutiveActions = 0;

    // Historical data
    private static final List<SessionData> sessionHistory = new CopyOnWriteArrayList<>();
    private static final ConcurrentMap<Integer, DailyPattern> dayPatterns = new ConcurrentHashMap<>();

    public enum UsagePhase {
        ACTIVE,
        BURST,
        IDLE,
        RECOVERY,
        DEEP_IDLE
    }

    public static class SessionData {
        public final long startTime;
        public final long duration;
        public final int actionCount;
        public final String phase;
        public final double avgEnergy;

        public SessionData(long start, long dur, int actions, String ph, double energy) {
            this.startTime = start;
            this.duration = dur;
            this.actionCount = actions;
            this.phase = ph;
            this.avgEnergy = energy;
        }
    }

    public static class UsageCluster {
        public final double startHour;
        public final double endHour;
        public final double intensity;
        public final boolean isWeekend;

        public UsageCluster(double start, double end, double intens, boolean wknd) {
            this.startHour = start;
            this.endHour = end;
            this.intensity = intens;
            this.isWeekend = wknd;
        }
    }

    public static class DailyPattern {
        public final int dayOfWeek;
        public final List<Double> hourlyActivity;
        public final int totalSessions;
        public final long totalUsageTime;

        public DailyPattern(int dow, List<Double> hourly, int sessions, long time) {
            this.dayOfWeek = dow;
            this.hourlyActivity = hourly;
            this.totalSessions = sessions;
            this.totalUsageTime = time;
        }
    }

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        HookUtils.logInfo(TAG, "Initializing Temporal Usage Pattern Hook");

        try {
            initializeUsageClusters();
            
            hookActivityLifecycle(lpparam);
            hookApplication(lpparam);
            hookHandler(lpparam);
            
            startTemporalSimulationThread();

            HookUtils.logInfo(TAG, "Temporal Usage Pattern Hook initialized successfully");
            HookUtils.logInfo(TAG, String.format("Current phase: %s, Energy: %.2f, Attention: %.2f",
                currentPhase.name(), currentEnergyLevel, currentAttentionLevel));
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize hook", e);
        }
    }

    private static void initializeUsageClusters() {
        // Morning cluster (7-9 AM)
        usageClusters.add(new UsageCluster(7.0, 9.5, 0.8, false));
        
        // Lunch cluster (12-1 PM)
        usageClusters.add(new UsageCluster(12.0, 13.5, 0.9, false));
        
        // Evening cluster (7-10 PM) - strongest
        usageClusters.add(new UsageCluster(19.0, 22.0, 1.0, false));
        
        // Weekend clusters
        usageClusters.add(new UsageCluster(9.0, 12.0, 0.7, true));
        usageClusters.add(new UsageCluster(14.0, 17.0, 0.8, true));
        usageClusters.add(new UsageCluster(20.0, 23.0, 1.0, true));
    }

    private static void hookActivityLifecycle(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> activityClass = XposedHelpers.findClass(
                "android.app.Activity",
                lpparam.classLoader
            );

            // Track activity interactions
            XposedBridge.hookAllMethods(activityClass, "onResume", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    try {
                        recordInteraction();
                        checkForPhaseTransition();
                    } catch (Exception e) {
                        if (DEBUG) HookUtils.logDebug(TAG, "Error in onResume: " + e.getMessage());
                    }
                }
            });

            XposedBridge.hookAllMethods(activityClass, "onPause", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    try {
                        long idleTime = System.currentTimeMillis() - lastInteractionTime;
                        
                        if (idleTime > 300000) { // 5+ minutes
                            transitionToPhase(UsagePhase.IDLE);
                            recoverAttention();
                        }
                    } catch (Exception e) {
                        if (DEBUG) HookUtils.logDebug(TAG, "Error in onPause: " + e.getMessage());
                    }
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked Activity lifecycle");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook Activity lifecycle", e);
        }
    }

    private static void hookApplication(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> applicationClass = XposedHelpers.findClass(
                "android.app.Application",
                lpparam.classLoader
            );

            // Track application lifecycle
            XposedBridge.hookAllMethods(applicationClass, "onCreate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    try {
                        // Track app launches as interactions
                        recordInteraction();
                    } catch (Exception e) {
                        // Silent fail
                    }
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked Application lifecycle");
        } catch (Exception e) {
            if (DEBUG) HookUtils.logDebug(TAG, "Application hook not available: " + e.getMessage());
        }
    }

    private static void hookHandler(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> handlerClass = XposedHelpers.findClass(
                "android.os.Handler",
                lpparam.classLoader
            );

            // Hook postDelayed to simulate delayed actions based on attention
            XposedBridge.hookAllMethods(handlerClass, "postDelayed", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || !attentionDecayEnabled) return;

                    try {
                        // Adjust delay based on attention level
                        // Lower attention = longer processing delays
                        if (currentAttentionLevel < 0.5 && random.get().nextDouble() < 0.3) {
                            long delay = (long) param.args[1];
                            long adjustedDelay = (long) (delay * (1.5 - currentAttentionLevel));
                            param.args[1] = adjustedDelay;
                        }
                    } catch (Exception e) {
                        // Silent fail
                    }
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked Handler");
        } catch (Exception e) {
            if (DEBUG) HookUtils.logDebug(TAG, "Handler hook not available: " + e.getMessage());
        }
    }

    private static void recordInteraction() {
        lastInteractionTime = System.currentTimeMillis();
        consecutiveActions++;
        
        // Update energy based on time of day
        if (circadianEnabled) {
            updateCircadianRhythm();
        }
        
        // Update attention
        if (attentionDecayEnabled) {
            decayAttention();
        }

        // Check for burst mode
        if (usageBurstEnabled && !isInBurst) {
            checkForBurstTrigger();
        }

        // Reset session start if new session
        if (sessionStartTime == 0) {
            sessionStartTime = lastInteractionTime;
        }
    }

    private static void updateCircadianRhythm() {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        double currentHour = hour + (System.currentTimeMillis() % 3600000) / 3600000.0;
        
        // Calculate energy based on time of day
        if (currentHour >= morningPeakStart && currentHour < morningPeakStart + 2) {
            // Morning peak
            currentEnergyLevel = Math.min(1.0, 0.7 + random.get().nextDouble() * 0.3);
        } else if (currentHour >= 12 && currentHour < 14) {
            // Lunch boost
            currentEnergyLevel = Math.min(1.0, 0.8 + random.get().nextDouble() * 0.2);
        } else if (currentHour >= eveningPeakStart && currentHour < eveningPeakStart + 3) {
            // Evening peak (highest)
            currentEnergyLevel = Math.min(1.0, 0.85 + random.get().nextDouble() * 0.15);
        } else if (currentHour >= 14 && currentHour < 17) {
            // Afternoon lull
            currentEnergyLevel = Math.max(0.4, 0.6 + random.get().nextDouble() * 0.2);
        } else if (currentHour >= 1 && currentHour < 6) {
            // Sleep hours - very low
            currentEnergyLevel = Math.max(0.2, 0.3 + random.get().nextDouble() * 0.1);
        } else {
            // Normal daylight hours
            currentEnergyLevel = 0.5 + random.get().nextDouble() * 0.4;
        }

        // Day of week adjustment
        int dayOfWeek = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK);
        boolean isWeekend = (dayOfWeek == java.util.Calendar.SATURDAY || 
                            dayOfWeek == java.util.Calendar.SUNDAY);
        
        if (isWeekend) {
            // Higher energy on weekends during certain hours
            if (currentHour >= 10 && currentHour <= 23) {
                currentEnergyLevel = Math.min(1.0, currentEnergyLevel * weekendUsageMultiplier);
            }
        } else {
            // Lower during work hours on weekdays
            if (currentHour >= 9 && currentHour <= 17) {
                currentEnergyLevel *= weekdayWorkHoursMultiplier;
            }
        }
    }

    private static void decayAttention() {
        // Attention decays with consecutive actions
        double decay = attentionDecayRate * consecutiveActions;
        currentAttentionLevel = Math.max(1.0 - maxAttentionDecay, currentEnergyLevel - decay);
        
        // Add some randomness
        currentAttentionLevel += (random.get().nextDouble() - 0.5) * 0.1;
        currentAttentionLevel = Math.max(0.1, Math.min(1.0, currentAttentionLevel));
    }

    private static void recoverAttention() {
        // Attention recovers during idle periods
        long idleTime = System.currentTimeMillis() - lastInteractionTime;
        double recovery = attentionRecoveryRate * (idleTime / 60000.0); // Per minute
        currentAttentionLevel = Math.min(1.0, currentAttentionLevel + recovery);
        consecutiveActions = 0;
    }

    private static void checkForBurstTrigger() {
        double burstChance = burstProbability;
        
        // Higher chance if energy is high
        if (currentEnergyLevel > 0.8) {
            burstChance *= 1.5;
        }
        
        // Higher chance during peak hours
        if (isInPeakHours()) {
            burstChance *= 1.3;
        }
        
        // Within session bursts
        if (consecutiveActions > 3) {
            burstChance *= burstWithinSessionProbability;
        }
        
        if (random.get().nextDouble() < Math.min(0.5, burstChance)) {
            startBurst();
        }
    }

    private static boolean isInPeakHours() {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        return (hour >= 7 && hour <= 9) || 
               (hour >= 12 && hour <= 13) || 
               (hour >= 19 && hour <= 22);
    }

    private static void startBurst() {
        isInBurst = true;
        currentBurstActions = burstMinActions + random.get().nextInt(burstMaxActions - burstMinActions);
        currentPhase = UsagePhase.BURST;
        
        // Increase energy during burst
        currentEnergyLevel = Math.min(1.0, currentEnergyLevel + 0.1);
        
        if (DEBUG) {
            HookUtils.logDebug(TAG, String.format("Burst started: actions=%d, energy=%.2f",
                currentBurstActions, currentEnergyLevel));
        }
    }

    private static void checkForPhaseTransition() {
        if (isInBurst) {
            currentBurstActions--;
            
            if (currentBurstActions <= 0) {
                isInBurst = false;
                transitionToPhase(UsagePhase.RECOVERY);
            }
        }

        // Check for idle transition
        if (idlePeriodsEnabled && currentPhase == UsagePhase.ACTIVE) {
            if (random.get().nextDouble() < idleProbability) {
                transitionToPhase(UsagePhase.IDLE);
            }
        }
    }

    private static void transitionToPhase(UsagePhase newPhase) {
        if (currentPhase != newPhase) {
            // Record previous session if ending
            if (sessionStartTime > 0 && currentPhase == UsagePhase.ACTIVE || 
                currentPhase == UsagePhase.BURST) {
                long duration = System.currentTimeMillis() - sessionStartTime;
                sessionHistory.add(new SessionData(
                    sessionStartTime,
                    duration,
                    consecutiveActions,
                    currentPhase.name(),
                    currentEnergyLevel
                ));
            }

            currentPhase = newPhase;
            
            if (newPhase == UsagePhase.ACTIVE) {
                sessionStartTime = System.currentTimeMillis();
            }

            if (DEBUG) {
                HookUtils.logDebug(TAG, "Phase transition: " + currentPhase.name() + " -> " + newPhase.name());
            }
        }
    }

    private static void startTemporalSimulationThread() {
        Thread simulationThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(30000); // Check every 30 seconds
                    
                    if (!enabled) continue;
                    
                    // Update circadian rhythm
                    if (circadianEnabled) {
                        updateCircadianRhythm();
                    }
                    
                    // Handle idle phase transitions
                    if (currentPhase == UsagePhase.IDLE || currentPhase == UsagePhase.DEEP_IDLE) {
                        long idleTime = System.currentTimeMillis() - lastInteractionTime;
                        
                        if (idleTime > 1800000) { // 30 minutes
                            transitionToPhase(UsagePhase.DEEP_IDLE);
                        } else if (idleTime > 300000 && random.get().nextDouble() < 0.3) {
                            // 20% chance to end idle early
                            transitionToPhase(UsagePhase.RECOVERY);
                        }
                    }
                    
                    // Handle recovery phase
                    if (currentPhase == UsagePhase.RECOVERY) {
                        recoverAttention();
                        
                        if (currentAttentionLevel > 0.7) {
                            transitionToPhase(UsagePhase.ACTIVE);
                        }
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    if (DEBUG) HookUtils.logDebug(TAG, "Temporal simulation error: " + e.getMessage());
                }
            }
        });
        simulationThread.setName("TemporalUsageSimulator");
        simulationThread.setDaemon(true);
        simulationThread.start();
    }

    /**
     * Returns current energy level (0.0 - 1.0)
     */
    public static double getCurrentEnergyLevel() {
        return currentEnergyLevel;
    }

    /**
     * Returns current attention level (0.0 - 1.0)
     */
    public static double getCurrentAttentionLevel() {
        return currentAttentionLevel;
    }

    /**
     * Returns current usage phase
     */
    public static UsagePhase getCurrentPhase() {
        return currentPhase;
    }

    /**
     * Returns whether currently in a usage burst
     */
    public static boolean isInBurst() {
        return isInBurst;
    }

    /**
     * Returns number of consecutive actions
     */
    public static int getConsecutiveActions() {
        return consecutiveActions;
    }

    /**
     * Returns time since last interaction (ms)
     */
    public static long getTimeSinceLastInteraction() {
        return System.currentTimeMillis() - lastInteractionTime;
    }

    // Configuration methods

    public static void setEnabled(boolean enabled) {
        TemporalUsagePatternHook.enabled = enabled;
        HookUtils.logInfo(TAG, "Hook " + (enabled ? "enabled" : "disabled"));
    }

    public static void setCircadianEnabled(boolean enabled) {
        circadianEnabled = enabled;
        HookUtils.logInfo(TAG, "Circadian rhythm " + (enabled ? "enabled" : "disabled"));
    }

    public static void setUsageBurstEnabled(boolean enabled) {
        usageBurstEnabled = enabled;
        HookUtils.logInfo(TAG, "Usage bursts " + (enabled ? "enabled" : "disabled"));
    }

    public static void setIdlePeriodsEnabled(boolean enabled) {
        idlePeriodsEnabled = enabled;
        HookUtils.logInfo(TAG, "Idle periods " + (enabled ? "enabled" : "disabled"));
    }

    public static void setDayOfWeekEnabled(boolean enabled) {
        dayOfWeekEnabled = enabled;
        HookUtils.logInfo(TAG, "Day-of-week patterns " + (enabled ? "enabled" : "disabled"));
    }

    public static void setAttentionDecayEnabled(boolean enabled) {
        attentionDecayEnabled = enabled;
        HookUtils.logInfo(TAG, "Attention decay " + (enabled ? "enabled" : "disabled"));
    }

    public static void setBurstProbability(double probability) {
        burstProbability = HookUtils.clamp(probability, 0.01, 0.5);
        HookUtils.logInfo(TAG, "Burst probability set to: " + burstProbability);
    }

    public static void setIdleProbability(double probability) {
        idleProbability = HookUtils.clamp(probability, 0.01, 0.5);
        HookUtils.logInfo(TAG, "Idle probability set to: " + idleProbability);
    }

    public static void setAttentionDecayRate(double rate) {
        attentionDecayRate = HookUtils.clamp(rate, 0.001, 0.1);
        HookUtils.logInfo(TAG, "Attention decay rate set to: " + attentionDecayRate);
    }
}
