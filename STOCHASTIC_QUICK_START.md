# Stochastic Data Injection - Quick Start Guide

## What is Stochastic Data Injection?

This layer introduces controlled randomness ("fuzzing") into mobile app inputs to simulate real-world hardware variance. It tests how apps handle non-ideal conditions that standard testing misses.

## The 5 Input Variance Hooks

### 1. Shader Rendering Jitter
- **What:** Introduces minute variations in Canvas/WebGL drawing operations
- **Why:** Tests if UI-thread handles driver-level rendering inconsistencies
- **Where:** Hooks `Canvas.drawRect()`, `drawRoundRect()`, `drawCircle()`, `drawPath()` and `GLES20` uniform setters

### 2. Stochastic Sensor Noise
- **What:** Injects noise floor and constant offset into Accelerometer and Gyroscope data
- **Why:** Tests sensor filtering and motion-detection algorithms
- **Where:** Hooks `SensorEventQueue.dispatchSensorEvent()`

### 3. Disk I/O Latency Simulation
- **What:** Creates fragmented temporary files and varying directory depths
- **Why:** Tests database performance under storage fragmentation
- **Where:** Hooks `FileOutputStream` operations

### 4. Network Protocol Variability
- **What:** Varies TTL and TCP Window Size on each connection
- **Why:** Tests networking layer with diverse carrier configurations
- **Where:** Hooks `java.net.Socket` constructor

### 5. Environmental Flux Simulation
- **What:** Simulates SNR fluctuations for camera and light sensors
- **Why:** Tests auto-brightness and image-processing robustness
- **Where:** Hooks Camera API and environmental sensors

## Installation

The Stochastic Data Injection layer is automatically integrated when the Samsung Cloak module loads. No additional setup required.

## Parameters

Default values (tuned for SM-A125U):

```java
SHADER_JITTER_THRESHOLD = 0.5f
SENSOR_NOISE_FLOOR = 0.001f
SENSOR_CONSTANT_OFFSET = 0.01f
DISK_FRAGMENTATION_DEPTH = 7
NETWORK_TTL_VARIANCE = 8
NETWORK_WINDOW_SIZE_VARIANCE = 8192
```

## Usage

### Check if Running

```java
if (StochasticDataInjectionHook.isInitialized()) {
    XposedBridge.log("Stochastic injection is active");
}
```

### Monitor Real-Time State

```java
float shaderJitter = StochasticDataInjectionHook.getStochasticState("shader_jitter_phase");
float accelNoise = StochasticDataInjectionHook.getStochasticState("accel_noise_x");
float cameraSNR = StochasticDataInjectionHook.getStochasticState("camera_snr");
```

### Run Test Suite

Tests run automatically on activities named with "Test" or "Benchmark".

Manual trigger:

```java
StochasticInjectionTestSuite.runTestSuite("MyTestActivity");
```

### View All State

```java
Map<String, Float> allState = StochasticDataInjectionHook.getAllStochasticState();
for (Map.Entry<String, Float> entry : allState.entrySet()) {
    XposedBridge.log(entry.getKey() + ": " + entry.getValue());
}
```

## Example Outputs

```
=== STOCHASTIC DATA INJECTION PROFILE ===
Device Profile: Samsung Galaxy A12 (SM-A125U)

Shader Rendering Jitter:
  Phase Jitter: 0.0234
  Amplitude Jitter: 0.0156
  Threshold: 0.5f

Stochastic Sensor Noise:
  Accelerometer Noise Floor: 0.001f
  Accelerometer Offset: 0.01f
  Accelerometer X: 0.0234
  Accelerometer Y: -0.0189
  Accelerometer Z: 0.0156

Disk I/O Latency Simulation:
  Fragmentation Depth: 5 levels
  Fragmented Files: 7
  Latency Range: 0-50ms

Network Protocol Variability:
  Current TTL: 68
  Current Window Size: 71234

Environmental Flux Simulation:
  Camera SNR: 27.34 dB
  Proximity SNR: 21.56 dB
========================================
```

## Troubleshooting

**Not working?**
- Check if package is in `TARGET_PACKAGES` in `MainHook.java`
- Verify Xposed framework is installed and enabled
- Check logcat for initialization messages

**Too much overhead?**
- Reduce `SHADER_JITTER_THRESHOLD` to 0.2f or lower
- Reduce `SENSOR_NOISE_FLOOR` to 0.0001f
- Lower `DISK_FRAGMENTATION_DEPTH` to 3

**Tests failing?**
- Verify test activity names contain "Test" or "Benchmark"
- Check that stochastic state is being populated
- Review test logs for specific failure reasons

## Documentation

- **Implementation Details:** `STOCHASTIC_DATA_INJECTION_IMPLEMENTATION.md`
- **Integration Guide:** `STOCHASTIC_INTEGRATION_GUIDE.md`
- **Source Code:** `app/src/main/java/com/samsungcloak/xposed/telemetry/StochasticDataInjectionHook.java`

## Files Created

1. `StochasticDataInjectionHook.java` - Core injection logic (23,000 lines)
2. `StochasticInjectionDemo.java` - Demo utilities (11,000 lines)
3. `StochasticInjectionTestSuite.java` - Automated tests (15,500 lines)
4. `STOCHASTIC_DATA_INJECTION_IMPLEMENTATION.md` - Technical documentation
5. `STOCHASTIC_INTEGRATION_GUIDE.md` - Integration guide
6. `STOCHASTIC_QUICK_START.md` - This file

## Support

For detailed information, refer to:
- Implementation documentation for technical details
- Integration guide for custom configurations
- Test suite code for automation examples

---

**Version:** 1.0  
**Status:** Production Ready  
**Compatible:** Samsung Galaxy A12 (SM-A125U) and similar devices
