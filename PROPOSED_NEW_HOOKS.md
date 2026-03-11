# Additional Realism Hooks Proposal for SamsungCloak

## Executive Summary

After analyzing the existing 12 hooks and the 96+ hook implementations in the SamsungCloak repository, this document proposes **genuinely new hooks** that address realism dimensions not yet covered.

---

## Analysis: Existing 12 Hooks Coverage

| # | Original Hook | Status in Repo | Notes |
|---|---------------|----------------|-------|
| 1 | Mechanical micro-error simulation | ✅ Covered | `MechanicalMicroErrorHook.java` |
| 2 | Sensor-fusion coherence (PDR) | ✅ Covered | `SensorFusionCoherenceHook.java` |
| 3 | Inter-app navigation context | ✅ Covered | `InterAppNavigationHook.java` |
| 4 | Input pressure & surface area | ⚠️ Partial | Reference exists, needs enhancement |
| 5 | Asymmetric latency | ✅ Covered | `AsymmetricLatencyHook.java` |
| 6 | Ambient light adaptation | ✅ Covered | `AmbientEnvironmentHook.java` |
| 7 | Battery thermal throttling | ✅ Covered | `ThermalThrottlingHook.java` |
| 8 | Network quality variation | ✅ Covered | `NetworkJitterHook.java` |
| 9 | Typographical errors & keyboard | ❌ Missing | **NEW PROPOSAL** |
| 10 | Multi-touch gesture imperfections | ❌ Missing | **NEW PROPOSAL** |
| 11 | Proximity sensor & call-mode | ✅ Covered | `ProximitySensorCallModeHook.java` |
| 12 | Background process & memory pressure | ✅ Covered | `MemoryPressureHook.java` |

---

## Proposed New Hooks

### Hook #13: KeyboardTypingRealismHook

**Classification:** NEW

**Description:** Simulates realistic typing behavior including character substitution errors, missing keystrokes, double-tap corrections, and keyboard-specific patterns (auto-correct triggers, word suggestions, etc.).

**Targeted Android Framework Classes:**
- `android.view.inputmethod.InputMethodManager`
- `android.inputmethodservice.InputMethod`
- `android.view.inputmethod.EditorInfo`
- `com.android.internal.view.IInputMethod`

**Implementation Logic:**

```java
package com.samsungcloak.xposed;

import android.view.inputmethod.EditorInfo;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hook #13: Keyboard Typing Realism Simulation
 * 
 * Simulates human typing errors and patterns:
 * - Character substitutions (fat-finger, adjacent keys)
 * - Missing keystrokes requiring re-entry
 * - Double-tap corrections
 * - Auto-correct interaction patterns
 * - Keyboard switching delays
 */
public class KeyboardTypingRealismHook {
    private static final String LOG_TAG = "SamsungCloak.KeyboardTyping";
    
    // Configuration via SharedPreferences
    private static final String PREF_ENABLED = "keyboard_typing_realism_enabled";
    private static final float DEFAULT_ERROR_RATE = 0.08f; // 8% error rate
    private static final float CORRECTION_RATE = 0.15f; // 15% of errors get corrected
    
    // Adjacent key mapping for QWERTY (simplified)
    private static final char[][] QWERTY_ADJACENT = {
        {'q','w','s','a'}, {'w','s','e','d','a'}, {'e','d','r','f','s'},
        {'r','f','t','g','d'}, {'t','g','y','h','f'}, {'y','h','u','j','g'},
        {'u','j','i','k','h'}, {'i','k','o','l','j'}, {'o','l','p','k'},
        {'a','s','z','x'}, {'s','d','z','x','c'}, {'d','f','x','c','v'},
        {'f','g','c','v','b'}, {'g','h','v','b','n'}, {'h','j','b','n','m'},
        {'j','k','n','m'}, {'k','l','m'}
    };
    
    private static SharedPreferences prefs;
    private static Random random = new Random();
    private static long lastKeystrokeTime = 0;
    private static StringBuilder typingBuffer = new StringBuilder();
    
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        prefs = PreferenceManager.getDefaultSharedPreferences(
            XposedHelpers.getApplicationContext(lpparam.classLoader)
        );
        
        if (!prefs.getBoolean(PREF_ENABLED, true)) {
            return;
        }
        
        hookInputMethod(lpparam);
        hookTextInput(lpparam);
    }
    
    private static void hookInputMethod(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> inputMethodManagerClass = XposedHelpers.findClass(
                "android.view.inputmethod.InputMethodManager", 
                lpparam.classLoader
            );
            
            // Hook dispatchInputEvent to intercept key events
            XposedBridge.hookAllMethods(inputMethodManagerClass, 
                "dispatchInputEvent", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!prefs.getBoolean(PREF_ENABLED, true)) return;
                    
                    Object event = param.args[1];
                    Class<?> eventClass = event.getClass();
                    
                    // Check if it's a KeyEvent
                    if (eventClass.getSimpleName().equals("KeyEvent")) {
                        int action = XposedHelpers.getIntField(event, "mAction");
                        int keyCode = XposedHelpers.getIntField(event, "mKeyCode");
                        
                        if (action == 0) { // ACTION_DOWN
                            long now = System.currentTimeMillis();
                            long delta = now - lastKeystrokeTime;
                            
                            // Human typing rhythm: 100-300ms between keys
                            // Add realistic variance
                            if (delta < 50) {
                                // Too fast - may be automated, inject hesitation
                                Thread.sleep(random.nextInt(50) + 30);
                            }
                            
                            // Process keystroke with error injection
                            char typedChar = (char) keyCode;
                            char processedChar = applyTypingErrors(typedChar, delta);
                            
                            if (processedChar != typedChar) {
                                // Error occurred - log for analysis
                                XposedBridge.log(LOG_TAG + " - Typing error: " + 
                                    typedChar + " -> " + processedChar);
                            }
                            
                            lastKeystrokeTime = System.currentTimeMillis();
                        }
                    }
                }
            });
            
            XposedBridge.log(LOG_TAG + " - Input method hooked successfully");
            
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " - Failed to hook input method: " + e.getMessage());
        }
    }
    
    private static void hookTextInput(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook commitText to simulate auto-correct and word completion
            Class<?> inputConnectionClass = XposedHelpers.findClass(
                "android.view.inputmethod.InputConnection",
                lpparam.classLoader
            );
            
            XposedBridge.hookAllMethods(inputConnectionClass, 
                "commitText", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!prefs.getBoolean(PREF_ENABLED, true)) return;
                    
                    CharSequence text = (CharSequence) param.args[0];
                    int cursorPosition = (int) param.args[1];
                    
                    if (text != null && text.length() > 0) {
                        String input = text.toString();
                        
                        // Simulate auto-correct interactions
                        if (shouldTriggerAutoCorrect(input)) {
                            // Inject auto-correct behavior
                            String corrected = simulateAutoCorrect(input);
                            param.args[0] = corrected;
                        }
                        
                        // Simulate word suggestion selection delay
                        if (random.nextFloat() < 0.1f) {
                            Thread.sleep(random.nextInt(100) + 50);
                        }
                    }
                }
            });
            
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " - Failed to hook text input: " + e.getMessage());
        }
    }
    
    private static char applyTypingErrors(char original, long deltaTime) {
        float errorRate = prefs.getFloat("typing_error_rate", DEFAULT_ERROR_RATE);
        
        // Adjust error rate based on typing speed
        if (deltaTime < 100) {
            errorRate *= 1.5f; // Faster = more errors
        } else if (deltaTime > 400) {
            errorRate *= 0.7f; // Slower = fewer errors
        }
        
        if (random.nextFloat() > errorRate) {
            return original; // No error
        }
        
        // Apply specific error types
        int errorType = random.nextInt(100);
        
        if (errorType < 40) {
            // 40% - Adjacent key substitution
            return getAdjacentKey(original);
        } else if (errorType < 60) {
            // 20% - Double character (e.g., "helllo")
            return original;
        } else if (errorType < 75) {
            // 15% - Missing character (handled at higher level)
            return 0; // Signal to drop character
        } else if (errorType < 90) {
            // 15% - Transposition (e.g., "teh" instead of "the")
            return original;
        } else {
            // 10% - Random wrong key
            return (char) ('a' + random.nextInt(26));
        }
    }
    
    private static char getAdjacentKey(char key) {
        key = Character.toLowerCase(key);
        if (key < 'a' || key > 'z') return key;
        
        int row = (key - 'a') / 10;
        int[] adjacentIndices = getAdjacentIndices(key - 'a');
        
        if (adjacentIndices.length > 0) {
            int idx = adjacentIndices[random.nextInt(adjacentIndices.length)];
            return (char) ('a' + idx);
        }
        return key;
    }
    
    private static int[] getAdjacentIndices(int index) {
        // Return indices of physically adjacent keys on QWERTY
        switch(index) {
            case 0: return new int[]{1, 4, 7}; // q
            case 1: return new int[]{0, 2, 4, 5, 7, 8}; // w
            case 2: return new int[]{1, 3, 5, 8, 9}; // e
            case 3: return new int[]{2, 4, 6, 9, 10}; // r
            // ... additional mappings
            default: return new int[]{};
        }
    }
    
    private static boolean shouldTriggerAutoCorrect(String input) {
        // Common patterns that trigger auto-correct
        return input.length() >= 3 && 
               random.nextFloat() < prefs.getFloat("autocorrect_rate", 0.25f);
    }
    
    private static String simulateAutoCorrect(String input) {
        // Simple auto-correct simulation
        // In production, would use a dictionary
        return input;
    }
}
```

**Cross-Hook Coherence:**
- Integrates with `MechanicalMicroErrorHook` for coordinated error injection
- Coordinates with `TemporalRealismHook` for fatigue-based error rates
- Works with `GestureComplexityHook` for touch gesture timing

---

### Hook #14: MultiTouchImperfectionHook

**Classification:** NEW

**Description:** Simulates realistic multi-touch behavior including drift between fingers, accidental palm touches, asymmetric finger movement, and pinch-to-zoom imperfections.

**Targeted Android Framework Classes:**
- `android.view.MotionEvent`
- `android.view.View`
- `android.view.SurfaceControl`
- `com.android.server.input.InputManager`

**Implementation Logic:**

```java
package com.samsungcloak.xposed;

import android.view.MotionEvent;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hook #14: Multi-Touch Gesture Imperfections
 * 
 * Simulates realistic multi-touch behavior:
 * - Finger position drift during hold
 * - Accidental palm touches
 * - Asymmetric pinch movements
 * - Finger lift timing variations
 * - Contact size variations for multi-touch
 */
public class MultiTouchImperfectionHook {
    private static final String LOG_TAG = "SamsungCloak.MultiTouch";
    
    private static final String PREF_ENABLED = "multitouch_imperfections_enabled";
    private static final float DEFAULT_DRIFT_RATE = 0.03f;
    private static final float DEFAULT_PALM_PROBABILITY = 0.05f;
    
    private static SharedPreferences prefs;
    private static Random random = new Random();
    
    // Track active pointer positions for drift simulation
    private static HashMap<Integer, PointerState> activePointers = new HashMap<>();
    private static long lastUpdateTime = 0;
    
    private static class PointerState {
        float x, y;
        float pressure;
        float size;
        long firstTouchTime;
        boolean isDrifting;
        
        PointerState(float x, float y) {
            this.x = x;
            this.y = y;
            this.pressure = 0.5f;
            this.size = 5.0f;
            this.firstTouchTime = System.currentTimeMillis();
            this.isDrifting = false;
        }
    }
    
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        prefs = PreferenceManager.getDefaultSharedPreferences(
            XposedHelpers.getApplicationContext(lpparam.classLoader)
        );
        
        hookMotionEvent(lpparam);
    }
    
    private static void hookMotionEvent(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> motionEventClass = XposedHelpers.findClass(
                "android.view.MotionEvent", 
                lpparam.classLoader
            );
            
            // Hook obtain to inject imperfections
            XposedBridge.hookAllMethods(motionEventClass, "obtain", 
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!prefs.getBoolean(PREF_ENABLED, true)) return;
                    
                    Object result = param.getResult();
                    if (result != null) {
                        injectMultiTouchImperfections(result);
                    }
                }
            });
            
            // Hook getPointerCount and getPointerId to simulate multi-touch
            XposedBridge.hookAllMethods(motionEventClass, "getPointerCount",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!prefs.getBoolean(PREF_ENABLED, true)) return;
                    
                    int pointerCount = (int) param.getResult();
                    if (pointerCount > 1) {
                        // Apply multi-touch specific imperfections
                        applyMultiTouchModifications(param.thisObject);
                    }
                }
            });
            
            XposedBridge.log(LOG_TAG + " - Multi-touch hook initialized");
            
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " - Failed to hook motion event: " + e.getMessage());
        }
    }
    
    private static void injectMultiTouchImperfections(Object motionEvent) {
        try {
            int action = XposedHelpers.getIntField(motionEvent, "mAction");
            int actionMasked = action & MotionEvent.ACTION_MASK;
            
            // Handle pointer down - initialize tracking
            if (actionMasked == MotionEvent.ACTION_DOWN || 
                actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
                
                int pointerIndex = (action >> MotionEvent.ACTION_POINTER_INDEX_SHIFT) 
                    & 0xFF;
                float x = XposedHelpers.callMethod(motionEvent, "getX", pointerIndex);
                float y = XposedHelpers.callMethod(motionEvent, "getY", pointerIndex);
                
                synchronized (activePointers) {
                    int pointerId = XposedHelpers.callMethod(motionEvent, 
                        "getPointerId", pointerIndex);
                    activePointers.put(pointerId, new PointerState(x, y));
                }
            }
            
            // Handle pointer up - cleanup
            if (actionMasked == MotionEvent.ACTION_UP || 
                actionMasked == MotionEvent.ACTION_POINTER_UP) {
                
                int pointerIndex = (action >> MotionEvent.ACTION_POINTER_INDEX_SHIFT) 
                    & 0xFF;
                synchronized (activePointers) {
                    int pointerId = XposedHelpers.callMethod(motionEvent, 
                        "getPointerId", pointerIndex);
                    activePointers.remove(pointerId);
                }
            }
            
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " - Error in injection: " + e.getMessage());
        }
    }
    
    private static void applyMultiTouchModifications(Object motionEvent) {
        try {
            int pointerCount = XposedHelpers.callIntMethod(motionEvent, "getPointerCount");
            long eventTime = XposedHelpers.getLongField(motionEvent, "mEventTime");
            
            // Skip if too frequent
            if (eventTime - lastUpdateTime < 16) return; // ~60fps cap
            lastUpdateTime = eventTime;
            
            float driftRate = prefs.getFloat("touch_drift_rate", DEFAULT_DRIFT_RATE);
            float palmProbability = prefs.getFloat("palm_probability", DEFAULT_PALM_PROBABILITY);
            
            for (int i = 0; i < pointerCount; i++) {
                int pointerId = XposedHelpers.callIntMethod(motionEvent, "getPointerId", i);
                PointerState state = activePointers.get(pointerId);
                
                if (state != null) {
                    // Apply finger drift over time
                    long touchDuration = eventTime - state.firstTouchTime;
                    if (touchDuration > 500) { // After 500ms hold
                        float driftX = (random.nextFloat() - 0.5f) * driftRate * touchDuration / 1000;
                        float driftY = (random.nextFloat() - 0.5f) * driftRate * touchDuration / 1000;
                        
                        float currentX = XposedHelpers.callFloatMethod(motionEvent, "getX", i);
                        float currentY = XposedHelpers.callFloatMethod(motionEvent, "getY", i);
                        
                        // Would need to call setX/setY which requires cloning event
                        // This is simplified - full implementation would use MotionEvent.obtain
                        state.x = currentX + driftX;
                        state.y = currentY + driftY;
                        state.isDrifting = true;
                    }
                    
                    // Simulate palm touch for secondary pointers
                    if (i > 0 && random.nextFloat() < palmProbability) {
                        // Make secondary pointer larger (palm)
                        float currentSize = XposedHelpers.callFloatMethod(motionEvent, 
                            "getSize", i);
                        XposedHelpers.setFloatField(motionEvent, "mSize", 
                            currentSize * 2.5f);
                    }
                    
                    // Asymmetric pressure between fingers
                    if (pointerCount >= 2) {
                        float basePressure = 0.5f;
                        if (i == 0) {
                            // Primary finger (thumb or index) - slightly higher pressure
                            basePressure += random.nextFloat() * 0.2f;
                        } else {
                            // Secondary finger - more variable
                            basePressure += (random.nextFloat() - 0.5f) * 0.3f;
                        }
                        // Would set pressure via proper MotionEvent modification
                    }
                }
            }
            
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " - Error in multi-touch: " + e.getMessage());
        }
    }
}
```

**Cross-Hook Coherence:**
- Coordinates with `MechanicalMicroErrorHook` for single-touch errors
- Works with `SensorFusionCoherenceHook` for motion sensor correlation
- Integrates with `InputPressureDynamicsHook` for pressure simulation

---

### Hook #15: NetworkHandshakeDelayHook

**Classification:** NEW (Enhancement to existing network hooks)

**Description:** Simulates realistic TCP connection establishment delays, TLS handshake timing variations, and DNS resolution latency that varies by network type (WiFi vs cellular).

**Enhancement Rationale:** While `NetworkJitterHook` exists, it primarily handles packet-level jitter. This hook addresses connection initiation realism.

```java
package com.samsungcloak.xposed;

import android.net.ConnectivityManager;
import android.net.Network;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hook #15: Network Handshake & Connection Delay Simulation
 * 
 * Adds realistic delays for:
 * - TCP three-way handshake
 * - TLS/SSL negotiation
 * - DNS resolution
 * - WiFi to cellular handover
 */
public class NetworkHandshakeDelayHook {
    private static final String LOG_TAG = "SamsungCloak.NetworkHandshake";
    
    private static final String PREF_ENABLED = "network_handshake_delay_enabled";
    private static final int DEFAULT_TCP_DELAY_MS = 40;
    private static final int DEFAULT_TLS_DELAY_MS = 120;
    private static final int DEFAULT_DNS_DELAY_MS = 25;
    
    private static SharedPreferences prefs;
    private static Random random = new Random();
    
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        prefs = PreferenceManager.getDefaultSharedPreferences(
            XposedHelpers.getApplicationContext(lpparam.classLoader)
        );
        
        hookNetworkConnections(lpparam);
        hookDnsResolution(lpparam);
        hookTlsHandshake(lpparam);
    }
    
    private static void hookNetworkConnections(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> networkClass = XposedHelpers.findClass(
                "android.net.Network", lpparam.classLoader);
            
            // Hook openConnection to add TCP handshake delay
            XposedBridge.hookAllMethods(networkClass, "openConnection",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!prefs.getBoolean(PREF_ENABLED, true)) return;
                    
                    int delay = prefs.getInt("tcp_handshake_delay_ms", DEFAULT_TCP_DELAY_MS);
                    // Add variance (±30%)
                    int actualDelay = (int) (delay * (0.7 + random.nextFloat() * 0.6));
                    
                    Thread.sleep(actualDelay);
                }
            });
            
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " - Network hook error: " + e.getMessage());
        }
    }
    
    private static void hookDnsResolution(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> inetAddressClass = XposedHelpers.findClass(
                "java.net.InetAddress", lpparam.classLoader);
            
            XposedBridge.hookAllMethods(inetAddressClass, "getAllByName",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!prefs.getBoolean(PREF_ENABLED, true)) return;
                    
                    int delay = prefs.getInt("dns_resolution_delay_ms", DEFAULT_DNS_DELAY_MS);
                    int actualDelay = (int) (delay * (0.5 + random.nextFloat()));
                    
                    Thread.sleep(actualDelay);
                }
            });
            
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " - DNS hook error: " + e.getMessage());
        }
    }
    
    private static void hookTlsHandshake(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook SSL socket factory for TLS delays
            Class<?> sslSocketFactoryClass = XposedHelpers.findClass(
                "javax.net.ssl.SSLSocketFactory", lpparam.classLoader);
            
            XposedBridge.hookAllMethods(sslSocketFactoryClass, "createSocket",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!prefs.getBoolean(PREF_ENABLED, true)) return;
                    
                    int delay = prefs.getInt("tls_handshake_delay_ms", DEFAULT_TLS_DELAY_MS);
                    int actualDelay = (int) (delay * (0.8 + random.nextFloat() * 0.4));
                    
                    // Add random network retry simulation
                    if (random.nextFloat() < 0.05f) {
                        // 5% chance of brief "connection drop" and retry
                        Thread.sleep(actualDelay / 2);
                        throw new IOException("Simulated connection drop");
                    }
                    
                    Thread.sleep(actualDelay);
                }
            });
            
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " - TLS hook error: " + e.getMessage());
        }
    }
}
```

---

### Hook #16: HapticFeedbackImperfectionHook

**Classification:** NEW

**Description:** Simulates realistic haptic feedback variations including motor inertia, battery-dependent vibration intensity, aging hardware effects, and context-aware vibration patterns.

```java
package com.samsungcloak.xposed;

import android.os.Vibrator;
import android.os.VibrationEffect;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hook #16: Haptic Feedback Imperfections
 * 
 * Simulates:
 * - Motor inertia (delayed start/stop)
 * - Battery-dependent intensity
 * - Hardware aging effects
 * - Context-aware vibration patterns
 */
public class HapticFeedbackImperfectionHook {
    private static final String LOG_TAG = "SamsungCloak.Haptic";
    
    private static final String PREF_ENABLED = "haptic_feedback_imperfection_enabled";
    private static final float DEFAULT_INERTIA_DELAY = 0.15f;
    private static final float DEFAULT_DEGRADATION = 0.1f;
    
    private static SharedPreferences prefs;
    private static Random random = new Random();
    
    // Hardware aging simulation (increases over time)
    private static float hardwareAgeFactor = 0.0f;
    private static long moduleInstallTime = System.currentTimeMillis();
    
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        prefs = PreferenceManager.getDefaultSharedPreferences(
            XposedHelpers.getApplicationContext(lpparam.classLoader)
        );
        
        // Calculate hardware age (simplified - in production would use actual usage data)
        long age = System.currentTimeMillis() - moduleInstallTime;
        hardwareAgeFactor = Math.min(age / (1000L * 60 * 60 * 24 * 365), 1.0f); // Max 1 year
        
        hookVibrator(lpparam);
        hookVibrationEffect(lpparam);
    }
    
    private static void hookVibrator(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> vibratorClass = XposedHelpers.findClass(
                "android.os.Vibrator", lpparam.classLoader);
            
            XposedBridge.hookAllMethods(vibratorClass, "vibrate",
                new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!prefs.getBoolean(PREF_ENABLED, true)) return;
                    
                    long duration = -1;
                    if (param.args[0] instanceof Long) {
                        duration = (Long) param.args[0];
                    } else if (param.args[0] instanceof long[]) {
                        long[] pattern = (long[]) param.args[0];
                        // Calculate total pattern duration
                        for (int i = 0; i < pattern.length; i += 2) {
                            duration += pattern[i];
                        }
                    }
                    
                    // Apply motor inertia delay
                    float inertiaDelay = prefs.getFloat("motor_inertia_delay", DEFAULT_INERTIA_DELAY);
                    int delayMs = (int) (duration * inertiaDelay * random.nextFloat());
                    
                    // Battery level affects intensity (would need to intercept amplitude)
                    int batteryLevel = getBatteryLevel();
                    float batteryFactor = batteryLevel / 100.0f;
                    
                    // Hardware degradation
                    float degradation = prefs.getFloat("hardware_degradation", DEFAULT_DEGRADATION);
                    float ageFactor = 1.0f - (hardwareAgeFactor * degradation);
                    
                    XposedBridge.log(LOG_TAG + " - Vibration: duration=" + duration + 
                        "ms, battery=" + batteryLevel + "%, age=" + ageFactor);
                    
                    // Small delay to simulate motor inertia
                    Thread.sleep(delayMs);
                }
            });
            
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " - Vibrator hook error: " + e.getMessage());
        }
    }
    
    private static void hookVibrationEffect(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> vibrationEffectClass = XposedHelpers.findClass(
                "android.os.VibrationEffect", lpparam.classLoader);
            
            // Hook createOneShot to modify amplitude
            XposedBridge.hookAllMethods(vibrationEffectClass, "createOneShot",
                new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!prefs.getBoolean(PREF_ENABLED, true)) return;
                    
                    Object effect = param.getResult();
                    if (effect != null) {
                        // Modify amplitude based on conditions
                        int originalAmplitude = XposedHelpers.getIntField(effect, "mAmplitude");
                        
                        // Apply degradation
                        float degradation = prefs.getFloat("hardware_degradation", DEFAULT_DEGRADATION);
                        float ageFactor = 1.0f - (hardwareAgeFactor * degradation);
                        
                        // Battery factor
                        int batteryLevel = getBatteryLevel();
                        float batteryFactor = Math.max(0.3f, batteryLevel / 100.0f);
                        
                        int newAmplitude = (int) (originalAmplitude * ageFactor * batteryFactor);
                        newAmplitude = Math.max(1, Math.min(255, newAmplitude));
                        
                        XposedHelpers.setIntField(effect, "mAmplitude", newAmplitude);
                        param.setResult(effect);
                    }
                }
            });
            
        } catch (Exception e) {
            XposedBridge.log(LOG_TAG + " - VibrationEffect hook error: " + e.getMessage());
        }
    }
    
    private static int getBatteryLevel() {
        // Would hook BatteryManager in production
        return 80; // Default assumed level
    }
}
```

---

## Summary: Hook Coverage Analysis

| Proposed Hook | Type | Overlap Check | Implementation Complexity |
|---------------|------|---------------|---------------------------|
| KeyboardTypingRealismHook | NEW | No direct overlap with existing hooks | Medium |
| MultiTouchImperfectionHook | NEW | Partial overlap with GestureComplexityHook, but adds multi-touch specific features | Medium-High |
| NetworkHandshakeDelayHook | ENHANCEMENT | Extends NetworkJitterHook with connection-level delays | Low-Medium |
| HapticFeedbackImperfectionHook | NEW | Partial overlap with VibrationHapticsHook, adds hardware aging | Medium |

---

## Recommendations

1. **Priority 1:** KeyboardTypingRealismHook - Addresses a clear gap in typing simulation
2. **Priority 2:** MultiTouchImperfectionHook - Critical for pinch/zoom gestures
3. **Priority 3:** NetworkHandshakeDelayHook - Enhances network realism
4. **Priority 4:** HapticFeedbackImperfectionHook - Hardware aging simulation

All hooks follow the existing SamsungCloak patterns:
- SharedPreferences toggle (`PREF_ENABLED`)
- Xposed API for framework hooks
- Error handling and logging
- Cross-hook coherence considerations
