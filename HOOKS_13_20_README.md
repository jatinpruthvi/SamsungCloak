# SamsungCloak Xposed Module - Hook 13-20 Realism Implementation

## Overview

This document describes the new Xposed realism hooks (13-20) implemented for Samsung Galaxy A12 (SM-A125U), along with improvements to existing hooks (03, 07, 08).

## Target Device
- **Model**: Samsung Galaxy A12 (SM-A125U)
- **Android Version**: 10/11

---

## NEW HOOKS IMPLEMENTED

### Hook 13: GPS Trajectory & Location Context Realism
**File**: `Hook13Realism.java`

**Target APIs**:
- `LocationManager`
- `Location`
- `SamsungLocationManager` (SLocationManager)
- `GnssStatus.Callback`

**Features**:
- Signal quality simulation: urban canyon (20-50m), outdoor (5-15m), indoor (50-200m)
- Trajectory smoothing: GPS lag 1-3s, speed overestimation on turns 10-30%
- Multi-path interference: false location jumps 2-5%, satellite count fluctuation
- Commute simulation: regularized movement patterns, location-based context detection

**Configuration Parameters**:
- `gps_trajectory_enabled` - Enable/disable hook
- `gps_trajectory_intensity` - Intensity (0.0-1.0)
- `urban_min_accuracy`, `urban_max_accuracy` - Urban canyon accuracy range
- `outdoor_min_accuracy`, `outdoor_max_accuracy` - Outdoor accuracy range
- `gps_lag_ms` - GPS lag in milliseconds
- `turn_speed_overestimation` - Speed overestimation factor
- `false_jump_probability` - False location jump probability

---

### Hook 14: Accessibility Scenario Simulation
**File**: `Hook14Realism.java`

**Target APIs**:
- `AccessibilityManager`
- `Settings.Secure`
- `MotionEvent`
- `TextToSpeech`

**Features**:
- Tremor/Parkinsonian simulation: micro-tremor 2-8Hz, touch area expansion 20-50%
- One-handed mode adaptation: reach zone limitations, grip-based coordinate offset
- Screen reader patterns: TalkBack focus traversal, reading speed 150-300 WPM
- Reduced motor control: delayed reaction 150-400ms, gesture completion rate 40-60%

**Configuration Parameters**:
- `accessibility_enabled` - Enable/disable hook
- `tremor_frequency` - Tremor frequency in Hz
- `tremor_amplitude` - Tremor amplitude
- `touch_area_expansion` - Touch area expansion factor
- `talkback_speed` - TalkBack reading speed in WPM
- `reaction_delay_ms` - Delayed reaction in milliseconds

---

### Hook 15: Weather & Environmental Sensor Effects
**File**: `Hook15Realism.java`

**Target APIs**:
- `MotionEvent`
- `SensorManager`
- `Location`
- `CameraDevice`
- `WeatherService`

**Features**:
- Rain/water droplet simulation: dead spots, reduced sensitivity 30-50%, ghost touches 3-8%
- Humidity effects: touch screen stickiness, microphone degradation, speaker muffling
- Temperature extremes: cold latency increase 20-40%, hot thermal throttling
- Altitude/pressure changes: barometric sensor variation, GPS accuracy reduction

**Configuration Parameters**:
- `weather_effects_enabled` - Enable/disable hook
- `touch_sensitivity_reduction` - Touch sensitivity reduction
- `ghost_touch_probability` - Ghost touch probability
- `cold_latency_increase` - Cold-induced latency increase
- `hot_throttle_threshold` - Temperature threshold for throttling
- `pressure_variation` - Barometric pressure variation

---

### Hook 16: Notification Dismissal & Attention Patterns
**File**: `Hook16Realism.java`

**Target APIs**:
- `NotificationManager`
- `NotificationListenerService`
- `StatusBarManager`
- `PowerManager`

**Features**:
- Arrival patterns: morning burst 7-9am (3-8), work hours lull, evening peak 6-9pm
- User attention probability: screen-on 25-40% dismissed, screen-off 80-95% dismissed
- Dismissal behaviors: swipe-away 65-85%, peek-expand-swipe 25-40%, tap-to-open 5-15%
- Attention fragmentation: notification bursts, interrupt handling patterns

**Configuration Parameters**:
- `notification_patterns_enabled` - Enable/disable hook
- `morning_burst_min/max` - Morning notification burst range
- `evening_burst_min/max` - Evening notification burst range
- `screen_on_dismiss_rate` - Dismissal rate when screen on
- `screen_off_dismiss_rate` - Dismissal rate when screen off

---

### Hook 17: Device Orientation & Grip Dynamics
**File**: `Hook17Realism.java`

**Target APIs**:
- `WindowManager`
- `SensorManager`
- `MotionEvent`
- `MultiWindowManager`

**Features**:
- Orientation changes: portrait↔landscape transitions 3-8s delay, auto-rotate disable 25-35%
- Grip modes: one-handed (70-80% standing/walking), two-handed (60-70% sitting), on surface (30-40% desk)
- Accidental palm touches: palm rejection failure 5-15%, screen edge ghost touches 2-5%
- Dynamic grip changes: 2-4 changes per 5min session, physical device movement patterns

**Configuration Parameters**:
- `orientation_grip_enabled` - Enable/disable hook
- `transition_delay_ms` - Orientation transition delay
- `auto_rotate_disable_prob` - Auto-rotate disable probability
- `palm_rejection_failure` - Palm rejection failure rate
- `edge_ghost_touch_prob` - Edge ghost touch probability

---

### Hook 18: Emotional State Interaction Patterns
**File**: `Hook18Realism.java`

**Target APIs**:
- `MotionEvent`
- `OnClickListener`
- `GestureDescription`
- `UsageEvents`

**Features**:
- Frustration patterns: rage taps 15-25%, fast scrolling 3-8x faster, back button spam 2-5 presses
- Hesitation patterns: dwell before critical actions 1-3s, micro-hover 200-800ms, touch-and-retract 20-35%
- Satisfaction patterns: consistent tap timing, smooth gesture paths, long sessions 10-20min
- Emotional state triggers: performance issues→frustration 60-80%, complex tasks→hesitation 50-70%

**Configuration Parameters**:
- `emotional_patterns_enabled` - Enable/disable hook
- `rage_tap_probability` - Rage tap probability
- `scroll_speed_multiplier` - Fast scrolling speed multiplier
- `back_button_spam_count` - Back button spam threshold
- `dwell_time_ms` - Hesitation dwell time

---

### Hook 19: App Session & Task Switching Patterns
**File**: `Hook19Realism.java`

**Target APIs**:
- `Activity`
- `ActivityManagerService`
- `UsageStatsManager`
- `Intent`

**Features**:
- Session length distribution: micro 5-30s (20-30%), short 30-120s (25-35%), medium 2-5min (25-35%), long 5-15min (10-20%)
- Task switching frequency: low 0-3/hour (20-30%), moderate 3-10/hour (40-50%), high 10-30/hour (20-30%)
- App category patterns: communication (short sessions, high switching), social media (long sessions), productivity (medium sessions)
- Interruption handling: interruption→resumption 60-75%, interruption→abandonment 25-40%

**Configuration Parameters**:
- `session_patterns_enabled` - Enable/disable hook
- `micro_session_percent` - Micro session percentage
- `short_session_percent` - Short session percentage
- `medium_session_percent` - Medium session percentage
- `long_session_percent` - Long session percentage
- `resumption_probability` - Resumption probability after interruption
- `abandonment_probability` - Abandonment probability after interruption

---

### Hook 20: Voice Command & Speech Recognition Realism
**File**: `Hook20Realism.java`

**Target APIs**:
- `RecognitionService`
- `SpeechRecognizer`
- `AudioRecord`
- `MicrophoneInfo`
- `BixbyService`

**Features**:
- Wake word detection: optimal 95-98%, moderate noise 85-92%, high noise 40-65%
- Speech recognition accuracy: simple commands 90-95%, complex sentences 75-85%, proper names 60-80%
- Ambient noise effects: fan -5-15%, traffic -15-30%, crowded room -30-50%, wind -50-70%
- User speech characteristics: slow 85-90%, normal 80-85%, fast 65-75%, slurred 40-60%, accented 60-75%

**Configuration Parameters**:
- `voice_recognition_enabled` - Enable/disable hook
- `optimal_wake_rate` - Wake word detection in quiet
- `moderate_noise_wake_rate` - Wake word in moderate noise
- `high_noise_wake_rate` - Wake word in high noise
- `simple_command_accuracy` - Simple command accuracy
- `complex_sentence_accuracy` - Complex sentence accuracy
- `proper_name_accuracy` - Proper name recognition accuracy

---

## IMPROVED EXISTING HOOKS

### Hook 03: Inter-App Navigation Realism (ENHANCED)
**File**: `Hook03Realism.java`

**New Features**:
- Android 10/11 Gesture Navigation: back swipe zones (20-30% edge), home bar (bottom 15%), recent apps swipe-hold
- Task Switcher Latency: 300-800ms delay, occasional stutter 2-5%
- App Death/Relaunch: low-memory scenarios cause fresh instance 30-50%
- Samsung-specific gestures: Edge panel, split-screen, one-handed mode
- Recent Apps Reordering: by usage recency, not chronological

---

### Hook 07: Battery Thermal & Performance (ENHANCED)
**File**: `Hook07Realism.java`

**New Features**:
- Samsung Game Booster: FPS limits, temperature-based mode switching, per-game profiles
- Exponential Battery Degradation: non-linear fade, steeper at extremes
- Battery Calibration Drift: ±5% drift at 12 months, ±10% at 24 months
- Charging Behavior: fast phase 0-80% (15-25W), trickle 80-100% (2-5W), adaptive charging
- Battery Health Variability: ±8% manufacturing variance
- Thermal Expansion Effects: 0.1-0.3mm dimension change with temperature

---

### Hook 08: Network Quality & Handover (ENHANCED)
**File**: `Hook08Realism.java`

**New Features**:
- WiFi Assist Simulation: supplement slow WiFi with LTE (threshold: latency>300ms, download<1Mbps)
- Captive Portal Detection: 2-5s delay on public WiFi
- Network State Debouncing: 10-30s window for rapid changes
- Background Network Restrictions: Android 10+ throttles background apps to 10-50%
- Data Saver Mode: restrict background data, block metered network
- Carrier-Specific Behaviors: port blocking, NAT behavior differences
- Network Congestion Patterns: peak hours 2-3x latency, 5-10x packet loss
- VPN Effects: +50-200ms latency, -10-30% bandwidth, DNS delays

---

## CROSS-HOOK COORDINATION

All new hooks are integrated with the `RealityCoordinator` class for temporal alignment:

```java
// Example: Setting location context from GPS hook
Hook13Realism.setLocationContext(LocationContext.URBAN_CAYON);

// Example: Getting emotional state for other hooks
float frustration = Hook18Realism.getFrustrationLevel();

// Example: Notifying performance issues
Hook18Realism.reportPerformanceIssue();
```

---

## CONFIGURATION

All hooks can be configured through:

1. **SharedPreferences** via `ConfigurationManager`
2. **hooks_config.xml** in assets folder

Each hook supports:
- Enable/disable flag
- Intensity slider (0.0-1.0)
- Individual parameter tuning

---

## BUILD INSTRUCTIONS

The module targets Android 10/11 on Samsung Galaxy A12 (SM-A125U).

```bash
# Build the module
cd /home/engine/project
./gradlew assembleDebug

# Install on device
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## COMPATIBILITY

- **Android Version**: 10 (API 29), 11 (API 30)
- **Device**: Samsung Galaxy A12 (SM-A125U)
- **Xposed Version**: 89/90+

All hooks work independently or in combination with graceful failure handling.
