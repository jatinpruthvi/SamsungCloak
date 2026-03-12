package com.samsungcloak.xposed;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.telephony.TelephonyManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;

/**
 * Hook #43: SocialInterruptionsHook
 * 
 * Simulates incoming calls, messages, and notifications interrupting app usage:
 * - Incoming call interruptions
 * - SMS/MMS message notifications
 * - App notification interruptions
 * - Do Not Disturb patterns
 * 
 * Target: SM-A125U (Android 10/11)
 */
public class SocialInterruptionsHook {

    private static final String TAG = "[HumanInteraction][SocialInterruption]";
    private static final boolean DEBUG = true;

    private static boolean enabled = true;
    private static float interruptionRate = 0.15f; // 15% chance of interruption
    private static boolean doNotDisturb = false;
    
    private static final Random random = new Random();
    
    // Interruption types
    private static final int TYPE_CALL = 0;
    private static final int TYPE_SMS = 1;
    private static final int TYPE_NOTIFICATION = 2;
    private static final int TYPE_ALARM = 3;
    
    private static long lastInterruptionTime = 0;
    private static int interruptionCount = 0;
    private static boolean isInCall = false;

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        HookUtils.logInfo(TAG, "Initializing Social Interruptions Hook");

        try {
            hookTelephonyManager(lpparam);
            hookNotificationManager(lpparam);
            HookUtils.logInfo(TAG, "Social Interruptions Hook initialized");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize hook", e);
        }
    }

    private static void hookTelephonyManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> telephonyManagerClass = XposedHelpers.findClass(
                "android.telephony.TelephonyManager", lpparam.classLoader);

            // Hook getCallState to simulate incoming calls
            XposedBridge.hookAllMethods(telephonyManagerClass, "getCallState",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || doNotDisturb) return;

                    int callState = (int) param.getResult();
                    
                    // Randomly inject incoming call state
                    if (callState == TelephonyManager.CALL_STATE_IDLE && 
                        shouldInterrupt() && 
                        random.nextFloat() < interruptionRate * 0.3f) {
                        
                        // Simulate incoming call
                        isInCall = true;
                        interruptionCount++;
                        lastInterruptionTime = System.currentTimeMillis();
                        
                        // Return ringing state
                        param.setResult(TelephonyManager.CALL_STATE_RINGING);
                        
                        if (DEBUG) {
                            HookUtils.logDebug(TAG, "Simulated incoming call");
                        }
                    } else if (callState == TelephonyManager.CALL_STATE_RINGING && !isInCall) {
                        // Reset after some time
                        new Thread(() -> {
                            try {
                                Thread.sleep(random.nextInt(5000) + 2000);
                                isInCall = false;
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }).start();
                    }
                }
            });

            // Hook listen to monitor phone state
            XposedBridge.hookAllMethods(telephonyManagerClass, "listen",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    // Could track phone state listener registration
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked TelephonyManager");

        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook TelephonyManager", e);
        }
    }

    private static void hookNotificationManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> notificationManagerClass = XposedHelpers.findClass(
                "android.app.NotificationManager", lpparam.classLoader);

            // Hook notify to simulate notification interruptions
            XposedBridge.hookAllMethods(notificationManagerClass, "notify",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || doNotDisturb) return;

                    // Check if this is a new notification that should interrupt
                    if (shouldInterrupt() && random.nextFloat() < interruptionRate) {
                        int notificationType = getRandomNotificationType();
                        
                        // Higher priority notifications more likely to interrupt
                        float interruptChance = interruptionRate;
                        if (notificationType == TYPE_CALL) {
                            interruptChance *= 1.0f; // Always high for calls
                        } else if (notificationType == TYPE_SMS) {
                            interruptChance *= 0.7f;
                        } else {
                            interruptChance *= 0.4f;
                        }
                        
                        if (random.nextFloat() < interruptChance) {
                            interruptionCount++;
                            lastInterruptionTime = System.currentTimeMillis();
                            
                            if (DEBUG && random.nextFloat() < 0.02f) {
                                HookUtils.logDebug(TAG, "Notification interruption: type=" + notificationType);
                            }
                        }
                    }
                }
            });

            // Hook areNotificationsEnabled to simulate DND
            XposedBridge.hookAllMethods(notificationManagerClass, "areNotificationsEnabled",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;
                    
                    // Return DND state
                    param.setResult(!doNotDisturb);
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked NotificationManager");

        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook NotificationManager", e);
        }
    }

    private static boolean shouldInterrupt() {
        long now = System.currentTimeMillis();
        
        // Don't interrupt too frequently
        if (lastInterruptionTime > 0 && (now - lastInterruptionTime) < 60000) {
            return false;
        }
        
        // Check time of day - more interruptions during day
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (hour >= 9 && hour <= 21) {
            return true;
        }
        
        return random.nextFloat() < 0.5f;
    }

    private static int getRandomNotificationType() {
        float roll = random.nextFloat();
        
        if (roll < 0.1f) return TYPE_CALL;
        if (roll < 0.3f) return TYPE_SMS;
        if (roll < 0.4f) return TYPE_ALARM;
        return TYPE_NOTIFICATION;
    }
}
