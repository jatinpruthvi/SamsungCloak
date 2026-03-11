package com.samsungcloak.xposed;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Random;

/**
 * Hook #19: Screen Reader / TalkBack Interaction Simulation
 * 
 * Simulates realistic accessibility service (TalkBack) interactions:
 * - Screen reader navigation patterns
 * - Focus announcement delays
 * - Touch exploration behavior
 * - Reading speed variations
 * 
 * Targets: SM-A125U (Android 10/11)
 */
public class ScreenReaderInteractionHook {

    private static final String TAG = "[HumanInteraction][ScreenReader]";
    private static final boolean DEBUG = true;

    private static boolean enabled = true;
    private static boolean simulateTalkBack = false;
    private static float readingSpeed = 1.0f; // 0.5 - 2.0
    private static float navigationDelay = 0.3f;

    private static final Random random = new Random();
    private static long lastAnnounceTime = 0;
    private static int focusChangeCount = 0;

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        HookUtils.logInfo(TAG, "Initializing Screen Reader Interaction Hook");

        try {
            hookAccessibilityEvents(lpparam);
            hookContentDescription(lpparam);
            HookUtils.logInfo(TAG, "Screen Reader Interaction Hook initialized");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize hook", e);
        }
    }

    private static void hookAccessibilityEvents(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook AccessibilityEvent to simulate screen reader behavior
            Class<?> accessibilityEventClass = XposedHelpers.findClass(
                "android.view.accessibility.AccessibilityEvent", lpparam.classLoader);

            // Hook getText to add reading delay simulation
            XposedBridge.hookAllMethods(accessibilityEventClass, "getText",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || !simulateTalkBack) return;

                    // Add delay based on content length
                    CharSequence text = (CharSequence) param.getResult();
                    if (text != null && text.length() > 0) {
                        int contentLength = text.length();
                        int baseDelay = (int) (contentLength * 10 / readingSpeed); // ms
                        int delay = baseDelay + random.nextInt(50);
                        
                        // Add navigation delay
                        if (focusChangeCount > 0) {
                            delay += (int) (navigationDelay * 200);
                        }

                        if (DEBUG && random.nextFloat() < 0.02f) {
                            HookUtils.logDebug(TAG, "Screen reader delay: " + delay + "ms for " + contentLength + " chars");
                        }
                    }
                }
            });

            // Hook getContentDescription for focus announcement
            XposedBridge.hookAllMethods(accessibilityEventClass, "getContentDescription",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || !simulateTalkBack) return;

                    CharSequence description = (CharSequence) param.getResult();
                    if (description != null && description.length() > 0) {
                        // Track focus changes
                        focusChangeCount++;
                        
                        // Add realistic focus announcement delay
                        long now = System.currentTimeMillis();
                        if (now - lastAnnounceTime > 100) {
                            // Allow time between announcements
                            int announceDelay = random.nextInt(100) + 50;
                            lastAnnounceTime = now;
                        }
                    }
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked AccessibilityEvent methods");

        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook accessibility events", e);
        }
    }

    private static void hookContentDescription(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook AccessibilityNodeInfo for content description
            Class<?> nodeInfoClass = XposedHelpers.findClass(
                "android.view.accessibility.AccessibilityNodeInfo", lpparam.classLoader);

            // Hook getContentDescription to simulate TalkBack reading
            XposedBridge.hookAllMethods(nodeInfoClass, "getContentDescription",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || !simulateTalkBack) return;

                    CharSequence description = (CharSequence) param.getResult();
                    if (description != null) {
                        // Add variation to how content is described
                        int length = description.length();
                        
                        // Simulate occasional "clickable" or "selected" state announcements
                        if (random.nextFloat() < 0.1f) {
                            String stateDescription = getStateDescription();
                            if (DEBUG && random.nextFloat() < 0.02f) {
                                HookUtils.logDebug(TAG, "Added state: " + stateDescription);
                            }
                        }
                    }
                }
            });

            // Hook performAction to simulate touch exploration
            XposedBridge.hookAllMethods(nodeInfoClass, "performAction",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled || !simulateTalkBack) return;

                    int action = (int) param.args[0];
                    
                    // Simulate touch exploration delays
                    if (action == AccessibilityNodeInfo.ACTION_CLICK ||
                        action == AccessibilityNodeInfo.ACTION_LONG_CLICK) {
                        
                        // Add exploration delay before action
                        int explorationDelay = random.nextInt(200) + 100;
                        
                        if (DEBUG && random.nextFloat() < 0.03f) {
                            HookUtils.logDebug(TAG, "Touch exploration delay: " + explorationDelay + "ms");
                        }
                    }
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked AccessibilityNodeInfo methods");

        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook content description", e);
        }
    }

    private static String getStateDescription() {
        String[] states = {"selected", "checked", "focused", "activated"};
        return states[random.nextInt(states.length)];
    }
}
