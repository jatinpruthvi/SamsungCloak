package com.samsungcloak.hardware;

/**
 * Example usage of the Hardware Exhaust & Environmental Fidelity Layer
 * for Samsung Galaxy A12 (SM-A125U) Digital Twin.
 * 
 * This example demonstrates how to use all five hardware artifact hooks
 * in a realistic diagnostic scenario.
 */
public class HardwareFidelityExample {
    
    public static void main(String[] args) {
        System.out.println("=== Samsung Galaxy A12 (SM-A125U) Hardware Fidelity Demo ===\n");
        
        // Initialize the orchestrator with 100% battery
        HardwareExhaustOrchestrator orchestrator = new HardwareExhaustOrchestrator(100.0);
        
        // Scenario 1: Gaming session (high CPU load, high brightness, cellular)
        System.out.println("Scenario 1: Gaming Session");
        simulateGamingSession(orchestrator);
        
        // Scenario 2: Web browsing (medium CPU, medium brightness, WiFi)
        System.out.println("\nScenario 2: Web Browsing");
        simulateWebBrowsing(orchestrator);
        
        // Scenario 3: Navigation (medium CPU, high brightness, moving, GPS intensive)
        System.out.println("\nScenario 3: GPS Navigation");
        simulateNavigation(orchestrator);
        
        // Scenario 4: Multi-day usage with storage degradation
        System.out.println("\nScenario 4: Multi-Day Usage with Storage Degradation");
        simulateMultiDayUsage(orchestrator);
        
        // Export final telemetry
        System.out.println("\n=== Final Telemetry ===");
        printTelemetry(orchestrator);
    }
    
    private static void simulateGamingSession(HardwareExhaustOrchestrator orchestrator) {
        // Simulate 30 minutes of gaming
        for (int i = 0; i < 30; i++) {
            UnifiedHardwareState state = orchestrator.updateHardwareState(
                0.9,     // 90% CPU load
                1.0,     // Max brightness
                0.6,     // High network activity (multiplayer)
                false,   // Cellular connection
                false,   // Stationary
                0.0,     // Not moving
                true,    // Indoors
                SensorFloorNoiseHook.EnvironmentType.INDOOR
            );
            
            // Launch game app every few minutes
            if (i % 5 == 0) {
                HardwareExhaustOrchestrator.AppLaunchResult launch = 
                    orchestrator.simulateAppLaunch("com.game.racing", 150.0, true, 0.9);
                System.out.printf("  Minute %d: Game launch: %.2fms, CPU: %.1f°C, Battery: %.1f%%\n",
                    i, launch.loadTimeMs, launch.cpuTemperatureC, 
                    orchestrator.getUnifiedHardwareState().battery.percentage);
            }
        }
        
        printHardwareState(orchestrator);
    }
    
    private static void simulateWebBrowsing(HardwareExhaustOrchestrator orchestrator) {
        // Simulate 20 minutes of web browsing
        for (int i = 0; i < 20; i++) {
            UnifiedHardwareState state = orchestrator.updateHardwareState(
                0.4,     // 40% CPU load
                0.7,     // Medium brightness
                0.3,     // Medium network activity
                true,    // WiFi connection
                false,   // Stationary
                0.0,     // Not moving
                true,    // Indoors
                SensorFloorNoiseHook.EnvironmentType.INDOOR
            );
            
            // Simulate loading web pages
            if (i % 2 == 0) {
                HardwareExhaustOrchestrator.TaskExecutionResult task = 
                    orchestrator.simulateTaskExecution(500.0, 0.4, 0.3);
                System.out.printf("  Minute %d: Page load: %.2fms, Network: %.2fms, Packet Loss: %.2f%%\n",
                    i, task.adjustedExecutionTimeMs, task.networkLatencyMs,
                    orchestrator.getUnifiedHardwareState().network.packetLossRate * 100);
            }
        }
        
        printHardwareState(orchestrator);
    }
    
    private static void simulateNavigation(HardwareExhaustOrchestrator orchestrator) {
        // Simulate 15 minutes of GPS navigation while driving
        for (int i = 0; i < 15; i++) {
            UnifiedHardwareState state = orchestrator.updateHardwareState(
                0.5,     // 50% CPU load
                0.8,     // High brightness
                0.2,     // Low network activity (map updates)
                false,   // Cellular connection
                true,    // Moving
                13.9,    // Driving speed (50 km/h)
                false,   // Outdoors
                SensorFloorNoiseHook.EnvironmentType.URBAN_CANYON
            );
            
            // Simulate GPS reading
            SensorFloorNoiseHook sensorHook = new SensorFloorNoiseHook();
            sensorHook.updateEnvironment(SensorFloorNoiseHook.EnvironmentType.URBAN_CANYON);
            SensorFloorNoiseHook.GPSReading gps = sensorHook.simulateGPSReading(
                37.7749, -122.4194, 10.0
            );
            
            System.out.printf("  Minute %d: GPS Accuracy: %.2fm, Speed: 13.9m/s, Signal: %.1fdBm\n",
                i, gps.accuracy, state.network.rssidBm);
            
            // Simulate navigation app operation
            HardwareExhaustOrchestrator.TaskExecutionResult task = 
                orchestrator.simulateTaskExecution(100.0, 0.5, 0.2);
        }
        
        printHardwareState(orchestrator);
    }
    
    private static void simulateMultiDayUsage(HardwareExhaustOrchestrator orchestrator) {
        // Simulate 7 days of usage with storage degradation
        for (int day = 1; day <= 7; day++) {
            System.out.printf("\nDay %d:\n", day);
            
            // Simulate typical daily usage
            for (int hour = 0; hour < 8; hour++) {
                // Mix of activities
                double cpuLoad = 0.3 + Math.random() * 0.4;
                double brightness = 0.5 + Math.random() * 0.5;
                double networkActivity = Math.random() * 0.5;
                
                orchestrator.updateHardwareState(
                    cpuLoad,
                    brightness,
                    networkActivity,
                    Math.random() > 0.3,  // 70% chance of WiFi
                    false,
                    0.0,
                    true,
                    SensorFloorNoiseHook.EnvironmentType.INDOOR
                );
            }
            
            // Update storage (add files, some deletions)
            boolean heavyWriteDay = (day % 3 == 0);  // Every 3rd day is heavy write
            orchestrator.updateStorageDay(
                0.5 + Math.random() * 1.5,  // 500MB-2GB added
                0.1 + Math.random() * 0.3,  // 100MB-400MB deleted
                heavyWriteDay
            );
            
            // Simulate app launches throughout the day
            for (int i = 0; i < 3; i++) {
                String[] apps = {"com.social.facebook", "com.social.instagram", "com.game.racing"};
                double[] sizes = {80.0, 120.0, 150.0};
                int appIndex = (int)(Math.random() * apps.length);
                
                HardwareExhaustOrchestrator.AppLaunchResult launch = 
                    orchestrator.simulateAppLaunch(apps[appIndex], sizes[appIndex], 
                                                  Math.random() > 0.5, 0.5);
            }
            
            // Print daily summary
            UnifiedHardwareState state = orchestrator.getUnifiedHardwareState();
            System.out.printf("  Battery: %.1f%%, Storage: %.1fGB (%.1f%% full), Fragmentation: %.2f%%\n",
                state.battery.percentage,
                state.storage.usedStorageGB,
                state.storage.fillLevel * 100,
                state.storage.fragmentationLevel * 100);
            
            // Charge battery overnight (except last day)
            if (day < 7) {
                orchestrator.chargeBattery(100.0 - state.battery.percentage);
            }
        }
        
        printHardwareState(orchestrator);
    }
    
    private static void printHardwareState(HardwareExhaustOrchestrator orchestrator) {
        UnifiedHardwareState state = orchestrator.getUnifiedHardwareState();
        
        System.out.println("\n  Hardware State:");
        System.out.printf("    Temperature: %.1f°C (Throttling: %s)\n",
            state.thermal.temperatureC, state.thermal.isThrottling);
        System.out.printf("    UI Latency: %.2fms (Factor: %.2fx)\n",
            state.thermal.currentUILatencyMs, state.thermal.thermalFactor);
        System.out.printf("    Battery: %.1f%% (%.2fV, %d cycles)\n",
            state.battery.percentage, state.battery.voltage, state.battery.chargeCycleCount);
        System.out.printf("    Network: %s (%.1fdBm, %.2fms latency, %.2f%% loss)\n",
            state.network.networkType.name(), state.network.rssidBm,
            state.network.averageLatencyMs, state.network.packetLossRate * 100);
        System.out.printf("    Storage: %.1fGB used (%.1f%% full, %.2f%% fragmented)\n",
            state.storage.usedStorageGB, state.storage.fillLevel * 100,
            state.storage.fragmentationLevel * 100);
        System.out.printf("    Read Speed: %.2f MB/s, Write Speed: %.2f MB/s\n",
            state.storage.effectiveReadSpeedMBps, state.storage.effectiveWriteSpeedMBps);
        System.out.printf("    Environment: %s (EM Interference: %.2fµT)\n",
            state.sensor.environment.name(), state.sensor.emInterferenceUT);
    }
    
    private static void printTelemetry(HardwareExhaustOrchestrator orchestrator) {
        UnifiedHardwareState state = orchestrator.getUnifiedHardwareState();
        
        System.out.println("\nCSV Header:");
        System.out.println(HardwareExhaustOrchestrator.UnifiedHardwareState.getCSVHeader());
        
        System.out.println("\nCSV Row:");
        System.out.println(state.toCSVRow());
    }
}
