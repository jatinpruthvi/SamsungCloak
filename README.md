# SamsungCloak

Xposed Module to spoof device identity to Samsung Galaxy A12 (SM-A125U) for bypassing TikTok device-based restrictions.

## Prerequisites

- **Magisk**: Latest stable version installed with Zygisk enabled
- **LSPosed**: Zygisk version of LSPosed framework
- **Android**: Version 11 (API 30) or higher

## Installation

1. **Install the APK**
   - Download and install `SamsungCloak.apk` on your device

2. **Enable in LSPosed**
   - Open LSPosed Manager
   - Navigate to the Modules section
   - Find and enable **SamsungCloak**

3. **Set Scope (CRITICAL)**
   - Tap on SamsungCloak in LSPosed Manager
   - **System Framework** (android): **MUST be ticked** - Required for system-level hooks
   - **Target Apps**: Tick the apps you want to cloak (e.g., TikTok, com.zhiliaoapp.musically)
   - Save the configuration

4. **Reboot**
   - Reboot your device for changes to take effect

## Usage

- **No UI**: SamsungCloak operates silently in the background with no user interface
- **Automated**: All spoofing is performed automatically when target apps are launched
- **Logging**: Check LSPosed Manager logs for "SamsungCloak" entries to verify operation

## Verification

### Device Identity
1. Install **"Device Info HW"** app from Play Store
2. Check device model - should display **SM-A125U**
3. Verify manufacturer shows **Samsung**
4. Check build fingerprint matches Galaxy A12

### Sensor Simulation
1. Install **"Sensor Test"** or **"AndroSensor"** app
2. Check accelerometer readings - data should appear "noisy" with subtle variations
3. Gyroscope should show similar randomized variations
4. This simulates real device sensor behavior vs emulator patterns

## Troubleshooting

### Module not working
- Verify **System Framework** is enabled in scope
- Check LSPosed logs for error messages
- Ensure you rebooted after enabling the module

### Apps still detect root/emulator
- Make sure target app is ticked in the scope
- Some apps may use additional detection methods
- Check logs for any hook failures

## Technical Details

- **Min SDK**: 30 (Android 11)
- **Target SDK**: 34 (Android 14)
- **Xposed API**: 82+
- **LSPosed Min Version**: 93
- **Total Files**: 83 (75 Java classes + 8 config/resource files)

## Project Structure

Run `./setup_project.sh` to view the complete file mapping and project organization documentation.

## Documentation

- **[HOOKS_DOCUMENTATION.md](./HOOKS_DOCUMENTATION.md)** - Complete documentation of all 104 implemented hooks (Single Source of Truth)
- **[DOCUMENTATION_AUDIT_REPORT.md](./DOCUMENTATION_AUDIT_REPORT.md)** - Documentation consistency audit report
- **[OPTIONAL_HOOK_IMPLEMENTATIONS.md](./OPTIONAL_HOOK_IMPLEMENTATIONS.md)** - Optional hook implementations for edge cases

## Cognitive Testing Framework

This repository also includes a comprehensive **Cognitive Realism Testing Framework** for simulating human-like behavior in Android mobile testing.

### Environmental Stress Model (NEW!)

A high-fidelity chaos testing suite for Samsung Galaxy A12 (SM-A125U) that simulates volatile real-world conditions often missed by synthetic tests.

**Key Features**:
- **Network Instability Simulation**: Dynamic 4G/3G/2G transitions with latency injection
- **Device Interruption Logic**: System events forcing app to background
- **Battery Constraint Modeling**: Power save mode behavior with performance throttling
- **Notification Distractions**: Push notification hijacking and focus loss simulation
- **Context Switching Entropy**: App hopping multitasking with task abandonment

**Documentation**:
- **[ENVIRONMENTAL_STRESS_MODEL_README.md](./ENVIRONMENTAL_STRESS_MODEL_README.md)** - Complete implementation summary
- **[cognitive-testing-framework/ENVIRONMENTAL_STRESS_MODEL.md](./cognitive-testing-framework/ENVIRONMENTAL_STRESS_MODEL.md)** - Comprehensive documentation
- **[cognitive-testing-framework/QUICKSTART_ENVIRONMENTAL.md](./cognitive-testing-framework/QUICKSTART_ENVIRONMENTAL.md)** - Quick start guide

**Quick Start**:
```java
EnvironmentalConfig config = EnvironmentalConfig.galaxyA12Stress();
EnvironmentalStressModel stressModel = new EnvironmentalStressModel(config, targetApp);
stressModel.start();

for (int i = 0; i < 100; i++) {
    stressModel.processInteraction();
    performTestAction();
    stressModel.beforeNetworkOperation();
    if (!stressModel.shouldNetworkOperationFail()) {
        performNetworkCall();
    }
}

stressModel.stop();
System.out.println(stressModel.generateReport());
```

**Cognitive Hooks**:
- Limited Attention Span
- Bounded Rationality (Satisficing)
- Emotional Bias Simulation
- Decision Fatigue
- Imperfect Memory
- Changing Preferences

See **[cognitive-testing-framework/README.md](./cognitive-testing-framework/README.md)** for complete framework documentation.

## License

See LICENSE file for details.
