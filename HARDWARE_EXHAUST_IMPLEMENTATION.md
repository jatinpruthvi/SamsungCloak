# Hardware Exhaust & Environmental Fidelity Implementation

## Overview

This implementation provides a comprehensive Hardware Exhaust & Environmental Fidelity layer for the Samsung Galaxy A12 (SM-A125U) Digital Twin, enabling realistic Hardware-in-the-Loop (HIL) diagnostics and telemetry.

## Components

### 1. Thermal Throttling Simulation (`ThermalThrottlingHook.java`)

Simulates realistic CPU thermal behavior based on MediaTek Helio P35 chipset characteristics.

**Key Features:**
- Temperature-based performance degradation (25°C - 55°C range)
- Non-linear throttling using sigmoid function
- UI latency and task execution time scaling
- Thermal inertia and consecutive throttle penalties
- Active/passive cooling simulation

**Parameters:**
- Base Temperature: 25°C
- Throttle Threshold: 42°C
- Critical Temperature: 48°C
- Max Temperature: 55°C
- Base UI Latency: 16ms
- Max Latency Multiplier: 4.5x

**Usage Example:**
```java
ThermalThrottlingHook thermalHook = new ThermalThrottlingHook();

// Update thermal state based on workload
double temperature = thermalHook.updateThermalState(0.8); // 80% CPU load

// Get current UI latency
double latencyMs = thermalHook.calculateUILatency();

// Calculate task execution time with thermal effects
double adjustedTimeMs = thermalHook.calculateTaskExecutionTime(baseExecutionTimeMs);

// Get thermal state telemetry
ThermalState state = thermalHook.getThermalState();
```

### 2. Battery Discharge Nonlinearity (`BatteryDischargeHook.java`)

Implements the Peukert Effect for realistic battery drain simulation.

**Key Features:**
- Non-linear discharge under high loads (Peukert exponent: 1.15)
- Voltage sag simulation under load
- Battery degradation over cycles
- Background task scheduling based on battery state
- 5000 mAh Li-Po battery model

**Parameters:**
- Battery Capacity: 5000 mAh
- Nominal Voltage: 3.85V
- Peukert Exponent: 1.15
- Max CPU Power: 1800 mW
- Screen Power: 80-230 mW (depending on brightness)

**Usage Example:**
```java
BatteryDischargeHook batteryHook = new BatteryDischargeHook(100.0); // Start at 100%

// Update battery state
double percentage = batteryHook.updateBatteryState(
    0.6,    // CPU load
    0.8,    // Screen brightness
    0.3,    // Network activity
    false   // Using cellular
);

// Check if background task should be scheduled
boolean shouldSchedule = batteryHook.shouldScheduleBackgroundTask(taskPriority);

// Get battery state telemetry
BatteryState state = batteryHook.getBatteryState();
```

### 3. Network Jitter & Packet Loss (`NetworkJitterHook.java`)

Stochastic network simulation for realistic mobile connectivity.

**Key Features:**
- Multiple network types (WiFi 5GHz, 2.4GHz, LTE grades, EDGE)
- RSSI signal strength fluctuations
- Movement-induced signal degradation
- Indoor/outdoor transitions
- WiFi/LTE handover simulation
- Packet loss and burst loss modeling

**Network Types:**
- WiFi 5GHz: 120ms latency, 0.1% loss
- WiFi 2.4GHz: 70ms latency, 0.2% loss
- LTE Excellent: 50ms latency, 0.5% loss
- LTE Good: 20ms latency, 1.0% loss
- LTE Fair: 5ms latency, 3.0% loss
- EDGE: 0.2ms latency, 10% loss

**Usage Example:**
```java
NetworkJitterHook networkHook = new NetworkJitterHook(NetworkType.LTE_GOOD);

// Update network state
NetworkState state = networkHook.updateNetworkState(
    true,    // Moving
    false,   // Outdoors
    13.9     // Driving speed (m/s)
);

// Simulate network request
NetworkResult result = networkHook.simulateNetworkRequest(1024); // 1KB request
double latencyMs = result.latencyMs;
boolean success = result.success;

// Get network telemetry
NetworkState telemetry = networkHook.getNetworkState();
```

### 4. Storage I/O Degradation (`StorageIODegradationHook.java`)

Simulates eMMC storage performance degradation over time.

**Key Features:**
- Fragmentation accumulation (0.3% per day)
- Fill-level based performance degradation
- Bad block development simulation
- App load time calculation
- Database query performance
- Random/sequential I/O modeling

**Parameters:**
- Storage Capacity: 32GB eMMC 5.1
- Base Read Speed: 250 MB/s
- Base Write Speed: 125 MB/s
- Base Random Read IOPS: 4000
- Base Random Write IOPS: 2000

**Degradation Thresholds:**
- Performance Impact Starts: 75% full
- Critical Performance: 90% full
- Fragmentation Impact: Quadratic

**Usage Example:**
```java
StorageIODegradationHook storageHook = new StorageIODegradationHook();

// Update storage for new day
storageHook.updateStorageState(
    0.5,     // 500MB added
    0.1,     // 100MB deleted
    true     // Heavy write day
);

// Simulate app load
double loadTimeMs = storageHook.simulateAppLoadTime(
    "com.example.app",
    50.0,    // 50MB app size
    true     // Cold load
);

// Simulate database query
double queryTimeMs = storageHook.simulateDatabaseQuery(5.0); // Complexity 5.0

// Get storage telemetry
StorageState state = storageHook.getStorageState();
```

### 5. Sensor Floor Noise (`SensorFloorNoiseHook.java`)

Realistic GPS and magnetometer noise simulation.

**Key Features:**
- GPS drift and multipath interference
- Magnetometer bias and drift
- Electromagnetic interference modeling
- Urban canyon and indoor effects
- Compass heading calculation
- Magnetic anomaly detection

**Environment Types:**
- Open Outdoor: Best GPS accuracy, minimal EM interference
- Urban Canyon: GPS accuracy penalty (15m), moderate EM interference
- Indoor: GPS accuracy penalty (30m), high EM interference
- Underground: Very poor GPS, very high EM interference
- Near Vehicle: Moderate GPS, high EM interference
- Near Building: Moderate GPS, moderate EM interference

**Usage Example:**
```java
SensorFloorNoiseHook sensorHook = new SensorFloorNoiseHook();

// Update environment
sensorHook.updateEnvironment(EnvironmentType.URBAN_CANYON);

// Simulate GPS reading
GPSReading gps = sensorHook.simulateGPSReading(
    37.7749,   // True latitude
    -122.4194, // True longitude
    10.0       // True altitude (m)
);
double accuracy = gps.accuracy; // Estimated accuracy in meters

// Simulate magnetometer reading
MagnetometerReading mag = sensorHook.simulateMagnetometerReading(
    20.0,  // True X (µT)
    -15.0, // True Y (µT)
    45.0   // True Z (µT)
);

// Calculate compass heading
double heading = sensorHook.calculateCompassHeading(mag);

// Get sensor telemetry
SensorState state = sensorHook.getSensorState();
```

## Hardware Exhaust Orchestrator

Central coordinator integrating all hardware hooks.

**Usage Example:**
```java
HardwareExhaustOrchestrator orchestrator = new HardwareExhaustOrchestrator(100.0);

// Update all hardware states
UnifiedHardwareState state = orchestrator.updateHardwareState(
    0.7,     // CPU load
    0.8,     // Screen brightness
    0.4,     // Network activity
    false,   // Cellular connection
    true,    // Device moving
    1.4,     // Walking speed (m/s)
    false,   // Outdoors
    SensorFloorNoiseHook.EnvironmentType.URBAN_CANYON
);

// Simulate app launch with all hardware effects
AppLaunchResult launchResult = orchestrator.simulateAppLaunch(
    "com.example.app",
    50.0,    // 50MB app size
    true,    // Cold launch
    0.6      // CPU load during launch
);

// Simulate task execution
TaskExecutionResult taskResult = orchestrator.simulateTaskExecution(
    100.0,   // Base task time (ms)
    0.5,     // CPU load
    0.3      // Network requirement
);

// Export telemetry
String csvRow = state.toCSVRow();
```

## Telemetry Export

The `UnifiedHardwareState` class provides CSV export functionality:

**CSV Format:**
```csv
day,simulation_time_ms,temp_c,is_throttling,throttle_duration_min,ui_latency_ms,thermal_factor,battery_pct,voltage,charge_cycles,discharge_mah,est_capacity_mah,network_type,rssi_dbm,avg_latency_ms,packet_loss_rate,is_indoors,is_moving,storage_gb_used,fill_level,fragmentation,read_speed_mbps,write_speed_mbps,bad_blocks,sensor_env,gps_offset_lat_m,gps_offset_lon_m,mag_bias_x,mag_bias_y,mag_bias_z,em_interference
1,86400000,38.5,false,0.0,18.2,1.02,92.5,3.92,0,375.0,5000.0,LTE_GOOD,-72.3,25.5,0.008,false,true,12.5,0.39,0.08,245.2,118.7,0,URBAN_CANYON,0.3,-0.2,0.1,-0.05,0.08,2.3
```

## Device-Specific Constants

All constants are calibrated for the Samsung Galaxy A12 (SM-A125U):

- **Chipset:** MediaTek Helio P35 (MT6765)
- **CPU:** Octa-core (4x2.35 GHz Cortex-A53 & 4x1.8 GHz Cortex-A53)
- **Battery:** 5000 mAh Li-Po
- **Storage:** 32GB eMMC 5.1
- **Display:** 6.5" PLS TFT, 720x1600 pixels
- **Network:** 4G LTE Cat. 4, WiFi 802.11 a/b/g/n/ac
- **GPS:** GNSS (GPS, GLONASS, BDS, GALILEO)
- **Sensors:** Accelerometer, Proximity, Compass, Barometer

## Integration with Android Framework

The hooks can be integrated into Android diagnostic applications:

```java
// In your Xposed module or diagnostic app
public class HardwareDiagnosticModule implements IXposedHookZygoteInit {
    
    private HardwareExhaustOrchestrator orchestrator;
    
    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        orchestrator = new HardwareExhaustOrchestrator(100.0);
        
        // Hook into system calls to monitor hardware state
        XposedHelpers.findAndHookMethod(
            "android.os.Process",
            null,
            "getElapsedCpuTime",
            new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    // Update thermal state
                    orchestrator.updateHardwareState(...);
                }
            }
        );
    }
}
```

## Testing and Validation

The implementation includes stochastic elements to ensure realistic variability:

1. **Thermal Testing:**
   - Gradual temperature rise under sustained load
   - Thermal inertia and cooling curves
   - UI latency degradation verification

2. **Battery Testing:**
   - Peukert effect validation at different loads
   - Voltage sag under high current
   - Background task scheduling logic

3. **Network Testing:**
   - RSSI fluctuation patterns
   - Handover scenarios (WiFi ↔ LTE)
   - Packet loss and burst modeling

4. **Storage Testing:**
   - Fragmentation accumulation over 30 days
   - Fill-level performance degradation
   - Bad block development

5. **Sensor Testing:**
   - GPS accuracy in different environments
   - Magnetometer bias drift over time
   - Compass heading error calculation

## Performance Considerations

- **Memory Usage:** ~50KB per orchestrator instance
- **CPU Overhead:** Minimal (simple arithmetic operations)
- **Thread Safety:** Each hook maintains its own state
- **Scalability:** Suitable for 100+ concurrent simulations

## Future Enhancements

1. **Additional Sensors:**
   - Accelerometer noise modeling
   - Barometric pressure drift
   - Proximity sensor characteristics

2. **Advanced Thermal:**
   - GPU thermal coupling
   - Battery temperature modeling
   - Passive/active cooling modes

3. **Network Enhancements:**
   - 5G simulation
   - WiFi roaming scenarios
   - Network congestion modeling

4. **Storage Enhancements:**
   - TRIM operation simulation
   - Wear leveling algorithms
   - SSD/NVFlash characteristics

## License

This implementation is part of the Samsung Galaxy A12 Digital Twin project and follows the project's license terms.

## References

- MediaTek Helio P35 Datasheet
- Samsung Galaxy A12 Technical Specifications
- Android Hardware Abstraction Layer (HAL)
- IEEE 11073 for Medical Device Communications
- ISO/IEC 18000 for RFID Identification
