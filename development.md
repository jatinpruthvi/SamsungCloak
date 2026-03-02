# Samsung Cloak - Development Documentation

**Project**: Samsung Galaxy A12 (SM-A125U) Device Spoofing Module  
**Version**: 2.0.0  
**Last Updated**: February 2025  
**Total Hooks**: 160  

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Package Structure](#package-structure)
3. [Hook Inventory by Package](#hook-inventory-by-package)
   - [com.samsung.cloak Package (78 hooks)](#comsamsungcloak-package-78-hooks)
   - [com.samsung.device.simulation Package (8 hooks)](#comsamsungdevicesimulation-package-8-hooks)
   - [com.samsungcloak.xposed Package (74 hooks)](#comsamsungcloakxposed-package-74-hooks)
4. [Hook Categories](#hook-categories)
5. [Initialization Phases](#initialization-phases)
6. [Target Applications](#target-applications)

---

## Project Overview

Samsung Cloak is a sophisticated Xposed module designed to spoof device identity to bypass TikTok's device-based restrictions and detection systems. The module creates a complete illusion of running on a genuine Samsung Galaxy A12 device by:

- Replacing all device identity markers (Build class, system properties, hardware profiles)
- Simulating organic sensor behavior that mimics human usage patterns
- Hiding all traces of modification frameworks (Xposed, Magisk, root)
- Providing consistent cross-system validation that survives deep inspection

---

## Package Structure

```
app/src/main/java/
├── com/samsung/cloak/                    # Extended hardening hooks (78 files)
├── com/samsung/device/simulation/        # Core simulation hooks (8 files)
└── com/samsungcloak/xposed/              # Main Xposed hooks (74 files)
```

---

## Hook Inventory by Package

### com.samsung.cloak Package (78 hooks)

| # | Filename | Description |
|---|----------|-------------|
| 1 | `AccessibilityCloak.java` | Accessibility service blinding - returns empty service list to prevent automation detection |
| 2 | `AdvancedEcosystemConsistencyHook.java` | Advanced cross-system ecosystem consistency validation |
| 3 | `AmbientEnvironment.java` | Ambient environment simulation (light, temperature, pressure) |
| 4 | `AppHistorySpoofer.java` | Application usage history spoofing - fakes realistic app usage patterns |
| 5 | `AudioInputSimulator.java` | Audio input hardware simulation - microphone characteristics |
| 6 | `BatteryLifecycle.java` | Battery lifecycle simulation with realistic drain curves |
| 7 | `BloatwareSimulator.java` | Carrier bloatware simulation - T-Mobile Visual Voicemail and other pre-installed apps |
| 8 | `BluetoothProfileHook.java` | Bluetooth device profiles and OUI (Organizationally Unique Identifier) spoofing |
| 9 | `BootIntegrityHook.java` | Boot integrity verification - kernel cmdline and bootloader state |
| 10 | `CameraHardwareHook.java` | Camera hardware simulation - focal length, zoom limits, resolution caps |
| 11 | `ClipboardSimulator.java` | Clipboard behavior simulation with Samsung-specific characteristics |
| 12 | `CpuArchitectureHook.java` | CPU architecture verification - MediaTek MT6765 (Helio P35) specs |
| 13 | `DeepEcosystemHardening.java` | Deep ecosystem hardening - app installer metadata, circadian settings |
| 14 | `DeepForensicIntegrityHardening.java` | Forensic-level integrity hardening against deep inspection |
| 15 | `DeepHardwareAndEcosystemHardening.java` | Combined hardware and ecosystem consistency hardening |
| 16 | `DeepHardwareAndNetworkHardening.java` | Hardware and network consistency cross-validation |
| 17 | `DeepHardwareCoherenceHook.java` | Deep hardware coherence - ensures all hardware components report consistent specs |
| 18 | `DeepHardwareConsistencyHook.java` | Thermal state transitions, network transport metadata, native library paths |
| 19 | `DeepHardwareIdHook.java` | Deep hardware ID management - storage UUID, Widevine ID persistence |
| 20 | `DeepHardwareSecurityHardening.java` | Hardware-level security hardening against advanced detection |
| 21 | `DeepKernelAndTelephonyHardening.java` | Kernel command line sanitization and IMS/telephony enforcement |
| 22 | `DeepNativeAndNetworkHardening.java` | Native library integrity and network hardening |
| 23 | `DeepNativeIntegrityFinalLayer.java` | Final layer of native integrity protection |
| 24 | `DeepNativeIntegrityHardening.java` | Deep native integrity hardening - prevents native detection |
| 25 | `DeepProtocolHardening.java` | Protocol-level hardening - USB metadata, MediaCodec limits |
| 26 | `DeepSystemCoherenceHook.java` | System-wide coherence validation across all subsystems |
| 27 | `DeepSystemConsistencyHardening.java` | System consistency hardening - ensures all system components align |
| 28 | `DeepSystemIntegrityHardening.java` | Deep system integrity hardening - Knox status, warranty bit, NFC simulation |
| 29 | `DeviceAgeSimulator.java` | Device age simulation - realistic wear and tear indicators |
| 30 | `DeviceConstants.java` | **Constants** - Central repository of all Samsung A12 device specifications |
| 31 | `Diagnostics.java` | Diagnostic utilities and logging framework |
| 32 | `EcosystemSynchronizationHardening.java` | Cross-ecosystem synchronization hardening |
| 33 | `FileStatHook.java` | File stat() system call interception - timestamps and inode spoofing |
| 34 | `FinalSystemHardening.java` | Final system-wide hardening layer |
| 35 | `GlesExtensionHook.java` | OpenGL ES extension string spoofing - PowerVR GE8320 |
| 36 | `GmsIntegrityHook.java` | Google Mobile Services integrity - Play Integrity API stub |
| 37 | `GodTierSecurityHardening.java` | God-tier security hardening - highest level of protection |
| 38 | `GoogleServicesHook.java` | Google services simulation - GSF ID stability, account features |
| 39 | `GraphicsHumanizerHook.java` | Graphics humanization - prevents GPU fingerprinting |
| 40 | `HighIntegrityHardwareHardening.java` | High-integrity hardware hardening layer |
| 41 | `HookUtils.java` | **Utilities** - Common hooking utilities and helper methods |
| 42 | `InputHygieneHook.java` | Input hygiene - USB debugging state, input device sanitization |
| 43 | `IntentEcosystemHook.java` | Intent ecosystem hardening - broadcast and intent filtering |
| 44 | `KernelStateHook.java` | Kernel state management - /proc/stat btime sync, load average |
| 45 | `KeyboardIdentityHook.java` | Keyboard IME identity - Samsung Honeyboard ID |
| 46 | `LifecycleSimulator.java` | Device lifecycle simulation - realistic state transitions |
| 47 | `LocationEngine.java` | Location services engine - GPS and network location |
| 48 | `LocationHook.java` | Location API hooking - LocationManager spoofing |
| 49 | `MainHook.java` | **Entry Point** - Main entry point for com.samsung.cloak package |
| 50 | `MediaCodecThrottler.java` | MediaCodec throttling - limits to A12 hardware capabilities |
| 51 | `MediaProviderHook.java` | MediaProvider hardening - media database sanitization |
| 52 | `MotionSimulator.java` | Motion simulation - accelerometer and gyroscope patterns |
| 53 | `NetworkConsistencyHook.java` | Network consistency validation - transport metadata |
| 54 | `NotchGeometryHook.java` | Display notch geometry - Infinity-V notch (80px safe inset) |
| 55 | `PeripheralHook.java` | Peripheral device simulation - USB accessory detection |
| 56 | `PowerConsistencyHook.java` | Power state consistency - thermal throttling coupling |
| 57 | `RegionalAppHider.java` | Regional app hiding - hides Indian apps from app lists |
| 58 | `SafetyWatchdog.java` | Safety watchdog - monitors hook integrity and health |
| 59 | `SamsungFeatureStub.java` | Samsung feature stubs - OneUI-specific feature simulation |
| 60 | `SensorHookInstaller.java` | Sensor hook installer - manages sensor simulation hooks |
| 61 | `SensorInventorySanitizer.java` | Sensor inventory sanitization - removes virtual sensors |
| 62 | `ServiceManagerHook.java` | ServiceManager hooks - system service interception |
| 63 | `SntpLatencyHook.java` | SNTP latency masking - hides network timing patterns |
| 64 | `SocialGraphHook.java` | Social graph simulation - contact and communication patterns |
| 65 | `SubSystemCoherenceHardening.java` | Subsystem coherence - storage, battery, software alignment |
| 66 | `SubscriptionIdentityHook.java` | Subscription identity - SIM and carrier information |
| 67 | `SysFsHook.java` | SysFS hooks - /sys filesystem interception |
| 68 | `SystemEnvironmentHardening.java` | System environment hardening - global environment sanitization |
| 69 | `SystemInteractionHook.java` | System interaction hooks - SystemProperties and runtime |
| 70 | `ThermalPhysicsEngine.java` | Thermal physics engine - realistic temperature simulation |
| 71 | `TouchBehavior.java` | Touch behavior simulation - digitizer characteristics |
| 72 | `UsageHistoryHook.java` | Usage history management - Samsung Notes/Galaxy Store before TikTok |
| 73 | `UsbConfigHook.java` | USB configuration - MTP mode enforcement |
| 74 | `VibrationSimulator.java` | Vibration motor simulation - haptic feedback patterns |
| 75 | `VulkanCapHook.java` | Vulkan capability spoofing - GPU feature flags |
| 76 | `WebRTCDefense.java` | WebRTC defense - prevents IP leakage through WebRTC |
| 77 | `WidevineL1Hook.java` | Widevine L1 DRM simulation - hardware-backed DRM |
| 78 | `WorldState.java` | World state management - global state coordination |

---

### com.samsung.device.simulation Package (8 hooks)

| # | Filename | Description |
|---|----------|-------------|
| 1 | `BatteryLifecycle.java` | Battery lifecycle management - realistic charging/discharging patterns |
| 2 | `BuildHook.java` | Build class reflection and method hooks - core device identity |
| 3 | `DeviceConstants.java` | **Constants** - Device simulation constants |
| 4 | `HookUtils.java` | **Utilities** - Hooking utilities for device simulation |
| 5 | `LifecycleSimulator.java` | Device lifecycle simulator - background state management |
| 6 | `MainHook.java` | **Entry Point** - Main entry for device simulation package |
| 7 | `PropertyHook.java` | System property hooks - ro.* property spoofing |
| 8 | `WorldState.java` | Global world state - device state coordination |

---

### com.samsungcloak.xposed Package (74 hooks)

| # | Filename | Description |
|---|----------|-------------|
| 1 | `AccessibilityServiceHider.java` | Accessibility service hiding - prevents automation detection |
| 2 | `AccountManagerHook.java` | Account manager hooks - Google/Samsung account simulation |
| 3 | `AntiDetectionHook.java` | Anti-detection hardening - Xposed framework concealment |
| 4 | `AudioFingerprintHook.java` | Audio fingerprint spoofing - Web Audio API protection |
| 5 | `AudioHook.java` | Audio system hooks - AudioManager and audio policy |
| 6 | `BatterySimulator.java` | Battery simulation - 5000mAh Li-ion with realistic drain |
| 7 | `BiometricSpoofHook.java` | Biometric spoofing - fingerprint/face unlock simulation |
| 8 | `BluetoothDiscoveryFilter.java` | Bluetooth discovery filtering - hides host BT devices |
| 9 | `BluetoothGattServiceSpoofHook.java` | Bluetooth GATT service spoofing - BLE normalization |
| 10 | `BuildHook.java` | Build class hooks - android.os.Build spoofing |
| 11 | `Camera2MetadataHook.java` | Camera2 API metadata spoofing - camera characteristics |
| 12 | `CanvasFingerprintHook.java` | Canvas fingerprint randomization - prevents GPU fingerprinting |
| 13 | `ClassMethodHider.java` | Class/method hiding - prevents reflective detection |
| 14 | `ClipboardSecurityHook.java` | Clipboard security - prevents clipboard-based detection |
| 15 | `ConsistencyValidator.java` | Cross-system consistency validation |
| 16 | `ContentProviderHook.java` | ContentProvider hooks - Settings, GSF, account providers |
| 17 | `CpuFrequencyHook.java` | CPU frequency simulation - dynamic frequency scaling |
| 18 | `DRMHook.java` | DRM framework hooks - Widevine and other DRM systems |
| 19 | `DeepIntegrityHardening.java` | Deep integrity hardening - Unix sockets, Knox, touch precision |
| 20 | `DeepSleepHook.java` | Deep sleep simulation - Doze mode and app standby |
| 21 | `DeepStackTraceSanitizer.java` | Deep stack trace sanitization - removes Xposed frames |
| 22 | `DeviceConstants.java` | **Constants** - Main device constants repository |
| 23 | `EmulatorDetectionCounter.java` | Emulator detection countermeasures - QEMU/BlueStacks hiding |
| 24 | `EmulatorNetworkCounter.java` | Emulator network countermeasures - IP/DNS pattern hiding |
| 25 | `EnvironmentHook.java` | Environment hooks - external storage, directories |
| 26 | `ExternalIdentityHardening.java` | External identity hardening - biometric, audio, storage |
| 27 | `FeatureHook.java` | Feature hooks - PackageManager.hasSystemFeature() |
| 28 | `FileDescriptorSanitizer.java` | File descriptor sanitization - /proc/self/fd filtering |
| 29 | `FontEnumerationHook.java` | Font enumeration hooks - system font list spoofing |
| 30 | `GPUHook.java` | GPU hooks - OpenGL ES, renderer string spoofing |
| 31 | `GodTierIdentityHardening.java` | God-tier identity hardening - Play Integrity, Bluetooth OUI |
| 32 | `GraphicsAndRadioHook.java` | Graphics and radio hooks - display and radio characteristics |
| 33 | `HardwareAndLogHardening.java` | Hardware and log hardening - display identity, BLE |
| 34 | `HardwareCapabilityHardening.java` | Hardware capability hardening - codec, resolution limits |
| 35 | `HardwareOpticsAndTimingHardening.java` | Hardware optics - rounded corners, dimensions, timing |
| 36 | `HookUtils.java` | **Utilities** - Main hooking utilities and logging |
| 37 | `IdentifierHook.java` | Identifier hooks - IMEI, MEID, serial number spoofing |
| 38 | `IntegrityDefense.java` | Comprehensive integrity defense - stack trace, package filtering |
| 39 | `KeystoreHardwareSpoof.java` | Keystore hardware spoofing - TEE/StrongBox simulation |
| 40 | `LocaleHook.java` | Locale hooks - language, region, timezone spoofing |
| 41 | `LowLevelEcosystemHardening.java` | Low-level ecosystem hardening - kernel, game services |
| 42 | `MainHook.java` | **Main Entry Point** - Primary Xposed module entry point |
| 43 | `MediaCodecHook.java` | MediaCodec hooks - codec capabilities spoofing |
| 44 | `MetaDetectionHook.java` | Meta-detection evasion - proxy and reflection detection |
| 45 | `MiscHook.java` | Miscellaneous hooks - various system services |
| 46 | `NativeAntiHookingHook.java` | Native anti-hooking - prevents native hook detection |
| 47 | `NativeLibrarySanitizer.java` | Native library sanitization - hides Xposed/Magisk libraries |
| 48 | `NetworkSecurityPolicyHook.java` | Network security policy - clearsTextTraffic policies |
| 49 | `NetworkSimulator.java` | Network simulation - T-Mobile US carrier, WiFi, cellular |
| 50 | `OneUISystemIntegrityHook.java` | OneUI system integrity - Samsung-specific hardening |
| 51 | `PowerHook.java` | Power management hooks - BatteryManager, power profiles |
| 52 | `ProcFileInterceptor.java` | /proc file interception - comprehensive proc filesystem spoofing |
| 53 | `ProcFilesystemHook.java` | Proc filesystem hooks - /proc/self/status, maps, etc. |
| 54 | `ProcessHook.java` | Process hooks - ProcessBuilder, process listing |
| 55 | `PropertyHook.java` | System property hooks - SystemProperties.get() interception |
| 56 | `ReflectiveAccessMonitor.java` | Reflective access monitoring - detects reflective detection |
| 57 | `RuntimeVMHook.java` | Runtime/VM hooks - Runtime.exec(), VM characteristics |
| 58 | `SELinuxHook.java` | SELinux hooks - SELinux state and policy spoofing |
| 59 | `SamsungFrameworkHook.java` | Samsung framework hooks - Samsung-specific system services |
| 60 | `SamsungHook.java` | Samsung hooks - general Samsung device simulation |
| 61 | `SensorHook.java` | Sensor hooks - legacy sensor simulation fallback |
| 62 | `SensorSimulator.java` | Sensor simulator - human-like sensor data injection |
| 63 | `SoCAndEcosystemHardening.java` | SoC and ecosystem hardening - SoC paths, SELinux |
| 64 | `StorageHook.java` | Storage hooks - storage volume and capacity spoofing |
| 65 | `SystemIntegrityHardening.java` | System integrity hardening - fonts, users, mounts |
| 66 | `ThermalHook.java` | Thermal hooks - temperature and thermal throttling |
| 67 | `TimingController.java` | Timing controller - human-like timing patterns |
| 68 | `TouchSimulator.java` | Touch simulator - pressure, size, multi-touch patterns |
| 69 | `UltimateEcosystemHardening.java` | Ultimate ecosystem hardening - Samsung stubs, proc sanitization |
| 70 | `UltimateHardwareConsistencyHook.java` | Ultimate hardware consistency - 10-component validation |
| 71 | `UsbDeviceEnumerationHook.java` | USB device enumeration - hides connected USB devices |
| 72 | `VPNDetectionCounter.java` | VPN detection countermeasures - network interface hiding |
| 73 | `WebViewEnhancedHook.java` | Enhanced WebView hooks - user agent, WebSettings |
| 74 | `WebViewHook.java` | WebView hooks - basic WebView protection |

---

## Hook Categories

### By Functionality

| Category | Count | Description |
|----------|-------|-------------|
| **Core Identity** | 15 | Build class, system properties, device identifiers |
| **Behavioral Authenticity** | 12 | Sensor, touch, timing, motion simulation |
| **Environmental Simulation** | 10 | Battery, network, GPU, thermal simulation |
| **Anti-Detection** | 35 | Xposed/root hiding, proc filesystem, stack traces |
| **Hardware Consistency** | 28 | CPU, sensors, camera, Bluetooth, USB |
| **System Integrity** | 25 | SELinux, Knox, keystore, DRM |
| **Network & Connectivity** | 15 | WiFi, cellular, VPN, WebRTC |
| **Advanced Defense** | 20 | Emulator countermeasures, deep hardening |

---

## Initialization Phases

The module initializes hooks in 22 sequential phases:

| Phase | Name | Key Hooks |
|-------|------|-----------|
| 1 | Core Identity Spoofing | BuildHook, PropertyHook |
| 2 | Behavioral Authenticity | SensorSimulator, TouchSimulator, TimingController |
| 3 | Environmental Simulation | BatterySimulator, NetworkSimulator, GPUHook |
| 4 | Anti-Detection Hardening | IntegrityDefense, AntiDetectionHook, SELinuxHook |
| 5 | Validation & Additional | ConsistencyValidator, SensorHook, EnvironmentHook |
| 6 | Advanced Fingerprinting Defense | ProcFileInterceptor, CanvasFingerprintHook |
| 7 | Hardware Geometry & Latency | NotchGeometryHook, SntpLatencyHook |
| 8 | System Integrity Hardening | SystemIntegrityHardening, OneUISystemIntegrityHook |
| 9 | External Identity Hardening | ExternalIdentityHardening |
| 10 | Hardware & Log Hardening | HardwareAndLogHardening |
| 11 | SoC & Ecosystem Hardening | SoCAndEcosystemHardening |
| 12 | Low-Level Ecosystem | LowLevelEcosystemHardening |
| 13 | Ultimate Ecosystem | UltimateEcosystemHardening |
| 14 | Deep Integrity | DeepIntegrityHardening |
| 15 | Hardware Optics & Timing | HardwareOpticsAndTimingHardening |
| 16 | Ultimate Hardware Consistency | UltimateHardwareConsistencyHook |
| 17 | God Tier Identity | GodTierIdentityHardening |
| 18 | Subsystem Coherence | SubSystemCoherenceHardening |
| 19 | Emerging Threat Defense | NativeLibrarySanitizer, DeepStackTraceSanitizer |
| 20 | Emulator Detection Counter | EmulatorDetectionCounter |
| 21 | Emulator Network Counter | EmulatorNetworkCounter |
| 22 | Samsung Cloak Extended | All com.samsung.cloak hooks |

---

## Target Applications

| Package Name | Description |
|--------------|-------------|
| `com.zhiliaoapp.musically` | TikTok International |
| `com.ss.android.ugc.trill` | TikTok Regional |
| `com.ss.android.ugc.aweme` | Douyin (Chinese TikTok) |

---

## Device Profile

| Property | Value |
|----------|-------|
| **Manufacturer** | samsung |
| **Brand** | samsung |
| **Model** | SM-A125U |
| **Device** | a12q |
| **Product** | a12qins |
| **Hardware** | mt6765 |
| **Board** | mt6765 |
| **SoC** | MediaTek Helio P35 (MT6765) |
| **RAM** | 3GB |
| **Battery** | 5000 mAh |
| **Display** | 6.5" Infinity-V (720x1600) |
| **GPU** | PowerVR GE8320 |
| **Carrier** | T-Mobile US |
| **Android** | 11 (API 30) |

---

## File Locations

### Source Files
- **Main Package**: `app/src/main/java/com/samsungcloak/xposed/`
- **Extended Hooks**: `app/src/main/java/com/samsung/cloak/`
- **Device Simulation**: `app/src/main/java/com/samsung/device/simulation/`

### Build Configuration
- **Root build.gradle**: `build.gradle`
- **App build.gradle**: `app/build.gradle`
- **ProGuard Rules**: `app/proguard-rules.pro`
- **Settings**: `settings.gradle`

### Documentation
- **Hooks Documentation**: `HOOKS_DOCUMENTATION.md`
- **Analysis Report**: `COMPREHENSIVE_HOOK_ANALYSIS_REPORT.md`
- **Build Guide**: `BUILD_GUIDE.md`
- **Changelog**: `CHANGELOG.md`

---

## Maintenance Notes

- All hook files follow the naming convention: `[Feature][Type].java`
- Hook initialization methods are typically named `init()` or `installHooks()`
- Each hook file should include a class-level Javadoc describing its purpose
- Target package filtering is applied consistently across all hooks
- Error handling uses try-catch blocks with fallback to original values

---

*This document is auto-generated. Last updated: February 2025*
