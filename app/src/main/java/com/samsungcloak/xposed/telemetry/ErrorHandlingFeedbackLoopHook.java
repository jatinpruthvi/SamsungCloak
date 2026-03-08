package com.samsungcloak.xposed.telemetry;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ErrorHandlingFeedbackLoopHook {
    private static final String LOG_TAG = "SamsungCloak.ErrorFeedback";
    private static boolean initialized = false;

    private static Random random = new Random();
    private static long sessionStartTime = System.currentTimeMillis();

    private static final Map<Integer, RateLimitState> rateLimitStates = new ConcurrentHashMap<>();
    private static final Map<String, RetryContext> retryContexts = new ConcurrentHashMap<>();
    
    private static final int HTTP_429_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_503_SERVICE_UNAVAILABLE = 503;
    private static final int HTTP_504_GATEWAY_TIMEOUT = 504;
    private static final int HTTP_502_BAD_GATEWAY = 502;

    private static class RateLimitState {
        int statusCode;
        long resetTimeMs;
        int retryAfterSeconds;
        int attemptCount;
        long lastAttemptTime;
        boolean isThrottled;

        RateLimitState(int code) {
            this.statusCode = code;
            this.resetTimeMs = System.currentTimeMillis() + 60000;
            this.retryAfterSeconds = 60;
            this.attemptCount = 0;
            this.lastAttemptTime = System.currentTimeMillis();
            this.isThrottled = true;
        }
    }

    private static class RetryContext {
        String endpoint;
        int consecutiveFailures;
        long lastFailureTime;
        long nextRetryTimeMs;
        int currentBackoffLevel;
        long totalBackoffTimeMs;
        
        static final long MIN_BACKOFF_MS = 500;
        static final long MAX_BACKOFF_MS = 32000;
        static final double BACKOFF_MULTIPLIER = 2.0;
        static final double JITTER_FACTOR = 0.3;

        RetryContext(String endpoint) {
            this.endpoint = endpoint;
            this.consecutiveFailures = 0;
            this.lastFailureTime = 0;
            this.nextRetryTimeMs = MIN_BACKOFF_MS;
            this.currentBackoffLevel = 0;
            this.totalBackoffTimeMs = 0;
        }

        long calculateHumanScaleBackoff() {
            double baseDelay = MIN_BACKOFF_MS * Math.pow(BACKOFF_MULTIPLIER, currentBackoffLevel);
            baseDelay = Math.min(baseDelay, MAX_BACKOFF_MS);
            
            double jitter = (random.nextDouble() * 2.0 - 1.0) * JITTER_FACTOR * baseDelay;
            
            long humanScaleDelay = (long) (baseDelay + jitter);
            
            humanScaleDelay = applyCognitiveLoadFactor(humanScaleDelay);
            
            return Math.max(MIN_BACKOFF_MS, Math.min(MAX_BACKOFF_MS, humanScaleDelay));
        }

        private long applyCognitiveLoadFactor(long delay) {
            double fatigueMultiplier = 1.0 + (consecutiveFailures * 0.15);
            
            double attentionVariance = 0.8 + (random.nextDouble() * 0.4);
            
            long adjustedDelay = (long) (delay * fatigueMultiplier * attentionVariance);
            
            return adjustedDelay;
        }

        void onFailure() {
            consecutiveFailures++;
            lastFailureTime = System.currentTimeMillis();
            currentBackoffLevel = Math.min(currentBackoffLevel + 1, 6);
            nextRetryTimeMs = calculateHumanScaleBackoff();
            totalBackoffTimeMs += nextRetryTimeMs;
        }

        void onSuccess() {
            consecutiveFailures = 0;
            currentBackoffLevel = 0;
            nextRetryTimeMs = MIN_BACKOFF_MS;
        }

        boolean shouldRetry() {
            if (consecutiveFailures >= 5) {
                return false;
            }
            
            long timeSinceLastAttempt = System.currentTimeMillis() - lastFailureTime;
            if (timeSinceLastAttempt < nextRetryTimeMs) {
                return false;
            }
            
            return true;
        }
    }

    private static final Map<String, ThrottlingPattern> throttlingPatterns = new HashMap<>();

    private static class ThrottlingPattern {
        String endpoint;
        int hitCount;
        long windowStartMs;
        int throttleThreshold;
        long throttleDurationMs;
        boolean isActive;

        ThrottlingPattern(String endpoint, int threshold) {
            this.endpoint = endpoint;
            this.hitCount = 0;
            this.windowStartMs = System.currentTimeMillis();
            this.throttleThreshold = threshold;
            this.throttleDurationMs = 60000;
            this.isActive = false;
        }

        boolean recordHit() {
            long now = System.currentTimeMillis();
            if (now - windowStartMs > 60000) {
                windowStartMs = now;
                hitCount = 0;
                isActive = false;
            }

            hitCount++;

            if (hitCount > throttleThreshold && !isActive) {
                isActive = true;
                throttleDurationMs = calculateThrottleDuration();
                return true;
            }

            return false;
        }

        private long calculateThrottleDuration() {
            long excessHits = hitCount - throttleThreshold;
            double severityMultiplier = 1.0 + (excessHits * 0.1);
            
            long baseDuration = 60000;
            long calculatedDuration = (long) (baseDuration * severityMultiplier);
            
            return Math.min(calculatedDuration, 300000);
        }
    }

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (initialized) {
            XposedBridge.log(LOG_TAG + " already initialized");
            return;
        }

        try {
            hookHttpClient(lpparam);
            hookOkHttpClient(lpparam);
            hookUrlConnection(lpparam);
            initialized = true;
            XposedBridge.log(LOG_TAG + " initialized successfully");
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " Failed to initialize: " + e.getMessage());
        }
    }

    private static void hookHttpClient(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> httpClientClass = XposedHelpers.findClass(
                "org.apache.http.impl.client.DefaultHttpClient", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(httpClientClass, "execute", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        handlePreRequest(param);
                    } catch (Exception e) {
                    }
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        handlePostRequest(param);
                    } catch (Exception e) {
                    }
                }
            });

            XposedBridge.log(LOG_TAG + " Hooked Apache HttpClient");
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " Failed to hook HttpClient: " + e.getMessage());
        }
    }

    private static void hookOkHttpClient(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> realCallClass = XposedHelpers.findClass(
                "okhttp3.RealCall", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(realCallClass, "execute", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        handleHttpResponse(param);
                    } catch (Exception e) {
                    }
                }
            });

            XposedBridge.hookAllMethods(realCallClass, "enqueue", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        handleAsyncResponse(param);
                    } catch (Exception e) {
                    }
                }
            });

            XposedBridge.log(LOG_TAG + " Hooked OkHttp RealCall");
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " Failed to hook OkHttp: " + e.getMessage());
        }
    }

    private static void hookUrlConnection(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> httpUrlConnectionClass = XposedHelpers.findClass(
                "com.android.okhttp.internal.huc.HttpURLConnectionImpl", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(httpUrlConnectionClass, "getResponseCode", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        handleResponseCode(param);
                    } catch (Exception e) {
                    }
                }
            });

            XposedBridge.hookAllMethods(httpUrlConnectionClass, "getResponseMessage", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        applyHumanScaleBackoff(param);
                    } catch (Exception e) {
                    }
                }
            });

            XposedBridge.log(LOG_TAG + " Hooked HttpURLConnection");
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " Failed to hook HttpURLConnection: " + e.getMessage());
        }
    }

    private static void handlePreRequest(XC_MethodHook.MethodHookParam param) {
        String endpoint = extractEndpoint(param.args);
        
        if (endpoint != null) {
            RetryContext context = retryContexts.computeIfAbsent(endpoint, RetryContext::new);
            
            if (context.consecutiveFailures > 0 && !context.shouldRetry()) {
                XposedBridge.log(LOG_TAG + " Request blocked - max retries exceeded for: " + endpoint);
                throw new RuntimeException("Maximum retry attempts exceeded");
            }

            if (context.consecutiveFailures > 0) {
                long waitTime = context.nextRetryTimeMs;
                XposedBridge.log(LOG_TAG + " Applying backoff before request: " + waitTime + "ms");
                
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            updateThrottlingPattern(endpoint);
        }
    }

    private static void handlePostRequest(XC_MethodHook.MethodHookParam param) {
        try {
            Object response = param.getResult();
            if (response != null) {
                handleHttpResponse(param);
            }
        } catch (Exception e) {
            handleRequestError(param, e);
        }
    }

    private static void handleHttpResponse(XC_MethodHook.MethodHookParam param) {
        int responseCode = extractResponseCode(param);
        String endpoint = extractEndpoint(param.args);

        if (endpoint != null) {
            RetryContext context = retryContexts.computeIfAbsent(endpoint, RetryContext::new);

            if (isThrottlingResponse(responseCode)) {
                context.onFailure();
                
                RateLimitState state = new RateLimitState(responseCode);
                rateLimitStates.put(responseCode, state);
                
                XposedBridge.log(LOG_TAG + " Throttling detected: HTTP " + responseCode + 
                    " for " + endpoint + ", backoff: " + context.nextRetryTimeMs + "ms");

                applyThrottleDelay(responseCode);
            } else if (responseCode >= 200 && responseCode < 300) {
                context.onSuccess();
                XposedBridge.log(LOG_TAG + " Request successful: " + endpoint);
            }
        }
    }

    private static void handleAsyncResponse(XC_MethodHook.MethodHookParam param) {
        try {
            Object callback = param.args[0];
            if (callback != null) {
                Class<?> callbackClass = callback.getClass();
                XposedBridge.hookAllMethods(callbackClass, "onResponse", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam innerParam) throws Throwable {
                        handleHttpResponse(innerParam);
                    }
                });
            }
        } catch (Exception e) {
        }
    }

    private static void handleResponseCode(XC_MethodHook.MethodHookParam param) {
        int responseCode = -1;
        
        try {
            responseCode = (int) param.getResult();
        } catch (Exception e) {
            return;
        }

        if (isThrottlingResponse(responseCode)) {
            XposedBridge.log(LOG_TAG + " Rate limit response detected: HTTP " + responseCode);
            
            RateLimitState state = new RateLimitState(responseCode);
            rateLimitStates.put(responseCode, state);
            
            applyThrottleDelay(responseCode);
        }
    }

    private static void applyHumanScaleBackoff(XC_MethodHook.MethodHookParam param) {
        int statusCode = -1;
        
        try {
            Object connection = param.thisObject;
            Class<?> clazz = connection.getClass();
            java.lang.reflect.Method getResponseCodeMethod = clazz.getMethod("getResponseCode");
            statusCode = (int) getResponseCodeMethod.invoke(connection);
        } catch (Exception e) {
        }

        if (isThrottlingResponse(statusCode)) {
            String endpoint = "unknown";
            
            RetryContext context = retryContexts.computeIfAbsent(endpoint, RetryContext::new);
            long backoffTime = context.calculateHumanScaleBackoff();
            
            XposedBridge.log(LOG_TAG + " Human-scale backoff: " + backoffTime + "ms");
            
            try {
                Thread.sleep(backoffTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void handleRequestError(XC_MethodHook.MethodHookParam param, Exception error) {
        String endpoint = extractEndpoint(param.args);
        
        if (endpoint != null) {
            RetryContext context = retryContexts.computeIfAbsent(endpoint, RetryContext::new);
            context.onFailure();
            
            XposedBridge.log(LOG_TAG + " Request error for " + endpoint + 
                ": " + error.getMessage() + ", next retry in: " + context.nextRetryTimeMs + "ms");
        }
    }

    private static boolean isThrottlingResponse(int statusCode) {
        return statusCode == HTTP_429_TOO_MANY_REQUESTS ||
               statusCode == HTTP_503_SERVICE_UNAVAILABLE ||
               statusCode == HTTP_504_GATEWAY_TIMEOUT ||
               statusCode == HTTP_502_BAD_GATEWAY;
    }

    private static void applyThrottleDelay(int statusCode) {
        RateLimitState state = rateLimitStates.get(statusCode);
        
        if (state != null) {
            long delay = calculateThrottleDelay(statusCode, state);
            
            XposedBridge.log(LOG_TAG + " Applying throttle delay: " + delay + "ms for HTTP " + statusCode);
            
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static long calculateThrottleDelay(int statusCode, RateLimitState state) {
        long baseDelay;
        
        switch (statusCode) {
            case HTTP_429_TOO_MANY_REQUESTS:
                baseDelay = state.retryAfterSeconds * 1000L;
                break;
            case HTTP_503_SERVICE_UNAVAILABLE:
                baseDelay = 5000 + random.nextInt(5000);
                break;
            case HTTP_504_GATEWAY_TIMEOUT:
                baseDelay = 3000 + random.nextInt(3000);
                break;
            default:
                baseDelay = 1000;
        }

        double humanVariance = 0.7 + (random.nextDouble() * 0.6);
        long adjustedDelay = (long) (baseDelay * humanVariance);
        
        return Math.max(500, Math.min(60000, adjustedDelay));
    }

    private static void updateThrottlingPattern(String endpoint) {
        ThrottlingPattern pattern = throttlingPatterns.computeIfAbsent(
            endpoint, k -> new ThrottlingPattern(endpoint, 20)
        );
        
        if (pattern.recordHit() && pattern.isActive) {
            XposedBridge.log(LOG_TAG + " Throttling pattern activated for: " + endpoint + 
                " (hits: " + pattern.hitCount + ", duration: " + pattern.throttleDurationMs + "ms)");
            
            try {
                Thread.sleep(pattern.throttleDurationMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static int extractResponseCode(XC_MethodHook.MethodHookParam param) {
        try {
            Object result = param.getResult();
            if (result instanceof Integer) {
                return (int) result;
            }
            
            if (result != null) {
                java.lang.reflect.Method getStatusCodeMethod = result.getClass().getMethod("getStatusLine");
                Object statusLine = getStatusCodeMethod.invoke(result);
                if (statusLine != null) {
                    java.lang.reflect.Method getProtocolVersion = statusLine.getClass().getMethod("getProtocolVersion");
                    java.lang.reflect.Method getStatusCode = statusLine.getClass().getMethod("getStatusCode");
                    return (int) getStatusCode.invoke(statusLine);
                }
            }
        } catch (Exception e) {
        }
        return -1;
    }

    private static String extractEndpoint(Object[] args) {
        if (args == null || args.length == 0) return "unknown";
        
        for (Object arg : args) {
            if (arg instanceof String) {
                String str = (String) arg;
                if (str.startsWith("http")) {
                    try {
                        java.net.URL url = new java.net.URL(str);
                        return url.getPath();
                    } catch (Exception e) {
                        return str;
                    }
                }
            }
            
            if (arg != null) {
                try {
                    java.lang.reflect.Method toString = arg.getClass().getMethod("toString");
                    String str = (String) toString.invoke(arg);
                    if (str != null && str.startsWith("http")) {
                        return str;
                    }
                } catch (Exception e) {
                }
            }
        }
        
        return "unknown";
    }

    public static void setConnectionProfile(ConnectionProfile profile) {
    }

    public static Map<Integer, RateLimitState> getRateLimitStates() {
        return new HashMap<>(rateLimitStates);
    }

    public static Map<String, RetryContext> getRetryContexts() {
        return new HashMap<>(retryContexts);
    }

    public static void reset() {
        rateLimitStates.clear();
        retryContexts.clear();
        throttlingPatterns.clear();
        XposedBridge.log(LOG_TAG + " State reset");
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
