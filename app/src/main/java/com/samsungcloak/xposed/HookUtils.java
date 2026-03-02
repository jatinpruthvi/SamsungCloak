package com.samsungcloak.xposed;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Random;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookUtils {
    private static final String LOG_TAG = "SamsungCloak";

    private static final ThreadLocal<Random> threadLocalRandom = new ThreadLocal<Random>() {
        @Override
        protected Random initialValue() {
            return new Random();
        }
    };

    public static Random getRandom() {
        return threadLocalRandom.get();
    }

    public static void setStaticField(Class<?> clazz, String fieldName, Object value) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);

            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

            field.set(null, value);
            logDebug("Set " + clazz.getSimpleName() + "." + fieldName + " = " + value);
        } catch (Exception e) {
            logError("Failed to set static field " + clazz.getSimpleName() + "." + fieldName + ": " + e.getMessage());
        }
    }

    public static float generateGaussianNoise(float stddev) {
        return (float) (getRandom().nextGaussian() * stddev);
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static void logDebug(String message) {
        XposedBridge.log(LOG_TAG + " [DEBUG]: " + message);
    }

    public static void logInfo(String message) {
        XposedBridge.log(LOG_TAG + " [INFO]: " + message);
    }

    public static void logError(String message) {
        XposedBridge.log(LOG_TAG + " [ERROR]: " + message);
    }

    public static void logWarn(String message) {
        XposedBridge.log(LOG_TAG + " [WARN]: " + message);
    }

    public static XC_MethodHook createSafeHook(final SafeHookCallback callback) {
        return new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    callback.beforeHooked(param);
                } catch (Exception e) {
                    logError("Hook error in beforeHooked: " + e.getMessage());
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    callback.afterHooked(param);
                } catch (Exception e) {
                    logError("Hook error in afterHooked: " + e.getMessage());
                }
            }
        };
    }

    public interface SafeHookCallback {
        void beforeHooked(MethodHookParam param) throws Throwable;
        void afterHooked(MethodHookParam param) throws Throwable;
    }

    public static XC_MethodHook createSafeBeforeHook(final SafeBeforeHookCallback callback) {
        return new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    callback.beforeHooked(param);
                } catch (Exception e) {
                    logError("Hook error in beforeHooked: " + e.getMessage());
                }
            }
        };
    }

    public interface SafeBeforeHookCallback {
        void beforeHooked(MethodHookParam param) throws Throwable;
    }

    public static XC_MethodHook createSafeAfterHook(final SafeAfterHookCallback callback) {
        return new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    callback.afterHooked(param);
                } catch (Exception e) {
                    logError("Hook error in afterHooked: " + e.getMessage());
                }
            }
        };
    }

    public interface SafeAfterHookCallback {
        void afterHooked(MethodHookParam param) throws Throwable;
    }

    public static void hookAllConstructors(Class<?> clazz, XC_MethodHook hook) {
        try {
            XposedBridge.hookAllConstructors(clazz, hook);
        } catch (Exception e) {
            logError("Failed to hook constructors of " + clazz.getName() + ": " + e.getMessage());
        }
    }

    public static void hookAllMethods(Class<?> clazz, String methodName, XC_MethodHook hook) {
        try {
            XposedBridge.hookAllMethods(clazz, methodName, hook);
        } catch (Exception e) {
            logError("Failed to hook method " + clazz.getName() + "." + methodName + ": " + e.getMessage());
        }
    }

    public static void hookMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        try {
            XposedBridge.findAndHookMethod(clazz, methodName, parameterTypes);
        } catch (Exception e) {
            logError("Failed to find method " + clazz.getName() + "." + methodName + ": " + e.getMessage());
        }
    }

    public static boolean isTargetPackage(XC_LoadPackage.LoadPackageParam lpparam, String packageName) {
        return lpparam.packageName.equals(packageName);
    }

    public static boolean isSystemFramework(XC_LoadPackage.LoadPackageParam lpparam) {
        return lpparam.packageName.equals("android");
    }

    public static String getCallingClass() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if (stackTrace.length >= 4) {
            return stackTrace[3].getClassName();
        }
        return "unknown";
    }

    public static long getSessionTime() {
        return System.currentTimeMillis();
    }
}
