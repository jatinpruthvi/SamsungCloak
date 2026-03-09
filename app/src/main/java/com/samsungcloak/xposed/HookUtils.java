package com.samsungcloak.xposed;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class HookUtils {

    private static final String TAG = "[HumanInteraction][Utils]";
    private static final boolean DEBUG = true;

    private HookUtils() {}

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }

    public static float generateGaussianNoise(double standardDeviation) {
        Random random = ThreadLocalRandom.current();
        return (float) (random.nextGaussian() * standardDeviation);
    }

    public static void logInfo(String tag, String message) {
        System.out.println(tag + " " + message);
    }

    public static void logDebug(String tag, String message) {
        if (DEBUG) {
            System.out.println(tag + " [DEBUG] " + message);
        }
    }

    public static void logError(String tag, String message, Throwable throwable) {
        System.err.println(tag + " [ERROR] " + message);
        if (throwable != null) {
            throwable.printStackTrace();
        }
    }

    public static Random getThreadSafeRandom() {
        return ThreadLocalRandom.current();
    }
}
