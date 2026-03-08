package com.samsungcloak.xposed.telemetry;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import android.view.MotionEvent;
import android.view.View;
import java.util.Random;

public class NaturalisticInputJitterHook {
    private static final String LOG_TAG = "SamsungCloak.InputJitter";
    private static boolean initialized = false;

    private static final double TOUCH_COORDINATE_STDDEV_DP = 1.5;
    private static final double TIMING_JITTER_STDDEV_MS = 12.0;
    private static final double PRESSURE_VARIANCE = 0.08;
    private static final double TOUCH_SIZE_VARIANCE = 0.06;
    
    private static final double FRICTION_COEFFICIENT = 0.15;
    private static final double INERTIA_DECAY = 0.92;
    
    private static Random random = new Random();
    private static long lastTouchTime = 0;
    private static float lastTouchX = 0;
    private static float lastTouchY = 0;
    private static float velocityX = 0;
    private static float velocityY = 0;

    private static class TouchState {
        float x;
        float y;
        long timestamp;
        boolean isMoving;
        
        TouchState(float x, float y, long timestamp) {
            this.x = x;
            this.y = y;
            this.timestamp = timestamp;
            this.isMoving = false;
        }
    }
    
    private static TouchState currentTouch = null;

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (initialized) {
            XposedBridge.log(LOG_TAG + " already initialized");
            return;
        }

        try {
            hookMotionEvent(lpparam);
            hookInputDispatcher(lpparam);
            hookView(lpparam);
            initialized = true;
            XposedBridge.log(LOG_TAG + " initialized successfully");
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " Failed to initialize: " + e.getMessage());
        }
    }

    private static void hookMotionEvent(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> motionEventClass = XposedHelpers.findClass(
                "android.view.MotionEvent", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(motionEventClass, "getX", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        float originalX = (float) param.getResult();
                        float jitteredX = applyCoordinateJitter(originalX, true);
                        param.setResult(jitteredX);
                    } catch (Exception e) {
                    }
                }
            });

            XposedBridge.hookAllMethods(motionEventClass, "getY", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        float originalY = (float) param.getResult();
                        float jitteredY = applyCoordinateJitter(originalY, false);
                        param.setResult(jitteredY);
                    } catch (Exception e) {
                    }
                }
            });

            XposedBridge.hookAllMethods(motionEventClass, "getEventTime", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        long originalTime = (long) param.getResult();
                        long jitteredTime = applyTimingJitter(originalTime);
                        param.setResult(jitteredTime);
                    } catch (Exception e) {
                    }
                }
            });

            XposedBridge.hookAllMethods(motionEventClass, "getDownTime", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        long originalDownTime = (long) param.getResult();
                        long jitteredDownTime = applyTimingJitter(originalDownTime);
                        param.setResult(jitteredDownTime);
                    } catch (Exception e) {
                    }
                }
            });

            XposedBridge.hookAllMethods(motionEventClass, "getPressure", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        float originalPressure = (float) param.getResult();
                        float jitteredPressure = applyPressureJitter(originalPressure);
                        param.setResult(jitteredPressure);
                    } catch (Exception e) {
                    }
                }
            });

            XposedBridge.hookAllMethods(motionEventClass, "getSize", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        float originalSize = (float) param.getResult();
                        float jitteredSize = applySizeJitter(originalSize);
                        param.setResult(jitteredSize);
                    } catch (Exception e) {
                    }
                }
            });

            XposedBridge.log(LOG_TAG + " Hooked MotionEvent methods");
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " Failed to hook MotionEvent: " + e.getMessage());
        }
    }

    private static void hookInputDispatcher(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> inputDispatcherClass = XposedHelpers.findClass(
                "android.inputdispatcher.InputDispatcher", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(inputDispatcherClass, "dispatchMotion", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        Object inputEvent = param.args[0];
                        if (inputEvent != null) {
                            applyInertiaToEvent(inputEvent);
                        }
                    } catch (Exception e) {
                    }
                }
            });

            XposedBridge.log(LOG_TAG + " Hooked InputDispatcher methods");
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " Failed to hook InputDispatcher: " + e.getMessage());
        }
    }

    private static void hookView(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> viewClass = XposedHelpers.findClass(
                "android.view.View", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(viewClass, "onTouchEvent", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        MotionEvent event = (MotionEvent) param.args[0];
                        if (event != null) {
                            updateTouchState(event);
                        }
                    } catch (Exception e) {
                    }
                }
            });

            XposedBridge.log(LOG_TAG + " Hooked View methods");
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " Failed to hook View: " + e.getMessage());
        }
    }

    private static float applyCoordinateJitter(float coordinate, boolean isX) {
        double jitter = gaussianRandom() * TOUCH_COORDINATE_STDDEV_DP;
        
        float frictionEffect = (float) (Math.random() * FRICTION_COEFFICIENT);
        
        if (currentTouch != null && currentTouch.isMoving) {
            jitter += velocityX * frictionEffect * (isX ? 1 : 0);
            jitter += velocityY * frictionEffect * (isX ? 0 : 1);
        }
        
        return coordinate + (float) jitter;
    }

    private static long applyTimingJitter(long originalTime) {
        double jitterMs = gaussianRandom() * TIMING_JITTER_STDDEV_MS;
        long adjustedTime = originalTime + (long) jitterMs;
        
        return Math.max(0, adjustedTime);
    }

    private static float applyPressureJitter(float originalPressure) {
        double variance = gaussianRandom() * PRESSURE_VARIANCE;
        float jitteredPressure = originalPressure + (float) variance;
        
        return Math.max(0.1f, Math.min(1.0f, jitteredPressure));
    }

    private static float applySizeJitter(float originalSize) {
        double variance = gaussianRandom() * TOUCH_SIZE_VARIANCE;
        float jitteredSize = originalSize + (float) variance;
        
        return Math.max(0.2f, Math.min(1.0f, jitteredSize));
    }

    private static void updateTouchState(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        long time = event.getEventTime();
        int action = event.getAction();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                currentTouch = new TouchState(x, y, time);
                lastTouchX = x;
                lastTouchY = y;
                velocityX = 0;
                velocityY = 0;
                break;
                
            case MotionEvent.ACTION_MOVE:
                if (currentTouch != null) {
                    long deltaTime = time - currentTouch.timestamp;
                    if (deltaTime > 0) {
                        velocityX = (x - lastTouchX) * INERTIA_DECAY;
                        velocityY = (y - lastTouchY) * INERTIA_DECAY;
                    }
                    currentTouch.isMoving = true;
                }
                lastTouchX = x;
                lastTouchY = y;
                break;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                currentTouch = null;
                velocityX = 0;
                velocityY = 0;
                break;
        }
    }

    private static void applyInertiaToEvent(Object inputEvent) {
        if (currentTouch != null && currentTouch.isMoving) {
            double inertiaX = velocityX * (1 - INERTIA_DECAY);
            double inertiaY = velocityY * (1 - INERTIA_DECAY);
            
            velocityX *= INERTIA_DECAY;
            velocityY *= INERTIA_DECAY;
        }
    }

    private static double gaussianRandom() {
        return random.nextGaussian();
    }

    public static void setCoordinateJitter(double stddevDp) {
    }

    public static void setTimingJitter(double stddevMs) {
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
