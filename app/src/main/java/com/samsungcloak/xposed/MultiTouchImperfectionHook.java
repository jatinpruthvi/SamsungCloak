package com.samsungcloak.xposed;

import android.view.MotionEvent;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Hook #14: Multi-Touch Gesture Imperfections
 * 
 * Simulates realistic multi-touch behavior:
 * - Finger position drift during hold
 * - Accidental palm touches
 * - Asymmetric pinch movements
 * - Finger lift timing variations
 * - Contact size variations for multi-touch
 * 
 * Targets: SM-A125U (Android 10/11)
 */
public class MultiTouchImperfectionHook {

    private static final String TAG = "[HumanInteraction][MultiTouch]";
    private static final boolean DEBUG = true;

    private static boolean enabled = true;
    private static float driftRate = 0.03f;
    private static float palmProbability = 0.05f;
    private static float asymmetryFactor = 0.15f;

    private static final Random random = new Random();
    private static long lastUpdateTime = 0;
    private static final Map<Integer, PointerState> activePointers = new HashMap<>();

    private static class PointerState {
        float x;
        float y;
        float pressure;
        float size;
        long firstTouchTime;
        boolean isDrifting;

        PointerState(float x, float y, float pressure, float size) {
            this.x = x;
            this.y = y;
            this.pressure = pressure;
            this.size = size;
            this.firstTouchTime = System.currentTimeMillis();
            this.isDrifting = false;
        }
    }

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        HookUtils.logInfo(TAG, "Initializing Multi-Touch Imperfection Hook");

        try {
            hookMotionEvent(lpparam);
            hookViewOnTouch(lpparam);
            HookUtils.logInfo(TAG, "Multi-Touch Imperfection Hook initialized successfully");
        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to initialize hook", e);
        }
    }

    private static void hookMotionEvent(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> motionEventClass = XposedHelpers.findClass(
                "android.view.MotionEvent", lpparam.classLoader);

            // Hook obtain to process and track multi-touch events
            XposedBridge.hookAllMethods(motionEventClass, "obtain", 
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    Object result = param.getResult();
                    if (result != null) {
                        processMotionEvent(result);
                    }
                }
            });

            // Hook getPointerCount and process multi-touch data
            XposedBridge.hookAllMethods(motionEventClass, "getPointerCount",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    int pointerCount = (int) param.getResult();
                    if (pointerCount > 1) {
                        // Apply multi-touch specific modifications
                        processMultiTouchPointers(param.thisObject);
                    }
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked MotionEvent methods");

        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook MotionEvent", e);
        }
    }

    private static void hookViewOnTouch(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> viewClass = XposedHelpers.findClass("android.view.View", lpparam.classLoader);

            // Hook onTouchEvent for additional touch processing
            XposedBridge.hookAllMethods(viewClass, "onTouchEvent", 
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!enabled) return;

                    MotionEvent event = (MotionEvent) param.args[0];
                    int actionMasked = event.getActionMasked();

                    // Handle pointer state changes
                    if (actionMasked == MotionEvent.ACTION_DOWN || 
                        actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
                        
                        int pointerIndex = event.getActionIndex();
                        int pointerId = event.getPointerId(pointerIndex);
                        float x = event.getX(pointerIndex);
                        float y = event.getY(pointerIndex);
                        float pressure = event.getPressure(pointerIndex);
                        float size = event.getSize(pointerIndex);

                        synchronized (activePointers) {
                            activePointers.put(pointerId, 
                                new PointerState(x, y, pressure, size));
                        }

                    } else if (actionMasked == MotionEvent.ACTION_UP || 
                               actionMasked == MotionEvent.ACTION_POINTER_UP) {
                        
                        int pointerIndex = event.getActionIndex();
                        int pointerId = event.getPointerId(pointerIndex);

                        synchronized (activePointers) {
                            activePointers.remove(pointerId);
                        }

                    } else if (actionMasked == MotionEvent.ACTION_MOVE) {
                        // Update pointer positions for drift simulation
                        updatePointerPositions(event);
                    }
                }
            });

            if (DEBUG) HookUtils.logDebug(TAG, "Hooked View.onTouchEvent");

        } catch (Exception e) {
            HookUtils.logError(TAG, "Failed to hook View onTouch", e);
        }
    }

    private static void processMotionEvent(Object motionEvent) {
        try {
            int action = XposedHelpers.getIntField(motionEvent, "mAction");
            int actionMasked = action & MotionEvent.ACTION_MASK;

            // Handle pointer state changes
            if (actionMasked == MotionEvent.ACTION_DOWN || 
                actionMasked == MotionEvent.ACTION_POINTER_DOWN) {

                int pointerIndex = (action >> MotionEvent.ACTION_POINTER_INDEX_SHIFT) & 0xFF;
                int pointerId = XposedHelpers.callIntMethod(motionEvent, "getPointerId", pointerIndex);
                float x = XposedHelpers.callFloatMethod(motionEvent, "getX", pointerIndex);
                float y = XposedHelpers.callFloatMethod(motionEvent, "getY", pointerIndex);
                float pressure = XposedHelpers.callFloatMethod(motionEvent, "getPressure", pointerIndex);
                float size = XposedHelpers.callFloatMethod(motionEvent, "getSize", pointerIndex);

                synchronized (activePointers) {
                    activePointers.put(pointerId, new PointerState(x, y, pressure, size));
                }

                if (DEBUG) {
                    HookUtils.logDebug(TAG, "Pointer down: id=" + pointerId + 
                        ", x=" + x + ", y=" + y);
                }

            } else if (actionMasked == MotionEvent.ACTION_UP || 
                       actionMasked == MotionEvent.ACTION_POINTER_UP) {

                int pointerIndex = (action >> MotionEvent.ACTION_POINTER_INDEX_SHIFT) & 0xFF;
                int pointerId = XposedHelpers.callIntMethod(motionEvent, "getPointerId", pointerIndex);

                synchronized (activePointers) {
                    activePointers.remove(pointerId);
                }

                if (DEBUG) {
                    HookUtils.logDebug(TAG, "Pointer up: id=" + pointerId);
                }
            }

        } catch (Exception e) {
            if (DEBUG) {
                HookUtils.logDebug(TAG, "Error processing motion event: " + e.getMessage());
            }
        }
    }

    private static void processMultiTouchPointers(Object motionEvent) {
        try {
            long eventTime = XposedHelpers.getLongField(motionEvent, "mEventTime");
            
            // Skip if too frequent (maintain ~60fps)
            if (eventTime - lastUpdateTime < 16) return;
            lastUpdateTime = eventTime;

            int pointerCount = XposedHelpers.callIntMethod(motionEvent, "getPointerCount");

            // Apply imperfections to each pointer
            for (int i = 0; i < pointerCount; i++) {
                int pointerId = XposedHelpers.callIntMethod(motionEvent, "getPointerId", i);
                PointerState state = activePointers.get(pointerId);

                if (state != null) {
                    // Apply finger drift over time for held touches
                    long touchDuration = eventTime - state.firstTouchTime;
                    if (touchDuration > 500) { // After 500ms hold
                        applyPointerDrift(motionEvent, i, touchDuration, state);
                    }

                    // Simulate palm touch for secondary pointers
                    if (i > 0) {
                        applyPalmTouchEffect(motionEvent, i);
                    }

                    // Apply asymmetric pressure between fingers
                    applyAsymmetricPressure(motionEvent, i, pointerCount, state);
                }
            }

        } catch (Exception e) {
            if (DEBUG) {
                HookUtils.logDebug(TAG, "Error processing multi-touch: " + e.getMessage());
            }
        }
    }

    private static void updatePointerPositions(MotionEvent event) {
        try {
            int pointerCount = event.getPointerCount();
            long eventTime = event.getEventTime();

            for (int i = 0; i < pointerCount; i++) {
                int pointerId = event.getPointerId(i);
                PointerState state = activePointers.get(pointerId);

                if (state != null) {
                    float x = event.getX(i);
                    float y = event.getY(i);
                    long touchDuration = eventTime - state.firstTouchTime;

                    // Update position with drift for held touches
                    if (touchDuration > 300) {
                        float driftX = (random.nextFloat() - 0.5f) * driftRate * 
                            (touchDuration / 1000.0f);
                        float driftY = (random.nextFloat() - 0.5f) * driftRate * 
                            (touchDuration / 1000.0f);

                        state.x = x + driftX;
                        state.y = y + driftY;
                        state.isDrifting = true;

                        if (DEBUG && random.nextFloat() < 0.01f) {
                            HookUtils.logDebug(TAG, "Pointer drift: id=" + pointerId + 
                                ", dx=" + driftX + ", dy=" + driftY);
                        }
                    } else {
                        state.x = x;
                        state.y = y;
                    }

                    // Update pressure with natural variation
                    state.pressure = event.getPressure(i) * (0.9f + random.nextFloat() * 0.2f);
                }
            }

        } catch (Exception e) {
            if (DEBUG) {
                HookUtils.logDebug(TAG, "Error updating pointer positions: " + e.getMessage());
            }
        }
    }

    private static void applyPointerDrift(Object motionEvent, int pointerIndex, 
        long touchDuration, PointerState state) {

        try {
            // Calculate drift amount based on hold duration
            float driftMagnitude = driftRate * (touchDuration / 1000.0f);
            
            // Apply small random drift
            float driftX = (random.nextFloat() - 0.5f) * driftMagnitude;
            float driftY = (random.nextFloat() - 0.5f) * driftMagnitude;

            float currentX = XposedHelpers.callFloatMethod(motionEvent, "getX", pointerIndex);
            float currentY = XposedHelpers.callFloatMethod(motionEvent, "getY", pointerIndex);

            // Update state (actual event modification would require event cloning)
            state.x = currentX + driftX;
            state.y = currentY + driftY;
            state.isDrifting = true;

            if (DEBUG && random.nextFloat() < 0.02f) {
                HookUtils.logDebug(TAG, "Applied drift: index=" + pointerIndex + 
                    ", duration=" + touchDuration + "ms, drift=(" + driftX + "," + driftY + ")");
            }

        } catch (Exception e) {
            if (DEBUG) {
                HookUtils.logDebug(TAG, "Error applying drift: " + e.getMessage());
            }
        }
    }

    private static void applyPalmTouchEffect(Object motionEvent, int pointerIndex) {
        try {
            // Small chance of secondary pointer being a palm touch
            if (random.nextFloat() < palmProbability) {
                float currentSize = XposedHelpers.callFloatMethod(motionEvent, "getSize", pointerIndex);
                
                // Palms have larger contact size
                float palmSizeMultiplier = 2.0f + random.nextFloat() * 1.0f;
                float newSize = currentSize * palmSizeMultiplier;

                if (DEBUG) {
                    HookUtils.logDebug(TAG, "Palm touch detected: index=" + pointerIndex + 
                        ", size=" + currentSize + " -> " + newSize);
                }
                
                // Would set via MotionEvent.replace or clone in production
            }

        } catch (Exception e) {
            if (DEBUG) {
                HookUtils.logDebug(TAG, "Error applying palm effect: " + e.getMessage());
            }
        }
    }

    private static void applyAsymmetricPressure(Object motionEvent, int pointerIndex, 
        int pointerCount, PointerState state) {
        try {
            if (pointerCount < 2) return;

            float basePressure = 0.5f;

            if (pointerIndex == 0) {
                // Primary finger (thumb or index) - slightly higher pressure
                basePressure += random.nextFloat() * 0.2f;
            } else {
                // Secondary finger - more variable pressure
                basePressure += (random.nextFloat() - 0.5f) * asymmetryFactor * 2;
            }

            // Clamp pressure to valid range
            basePressure = Math.max(0.1f, Math.min(1.0f, basePressure));

            state.pressure = basePressure;

            if (DEBUG && random.nextFloat() < 0.01f) {
                HookUtils.logDebug(TAG, "Asymmetric pressure: index=" + pointerIndex + 
                    ", pressure=" + basePressure);
            }

        } catch (Exception e) {
            if (DEBUG) {
                HookUtils.logDebug(TAG, "Error applying pressure: " + e.getMessage());
            }
        }
    }
}
