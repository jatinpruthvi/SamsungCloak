package com.cognitive.testing.environmental;

/**
 * Comprehensive example demonstrating Environmental Stress Model usage
 * for high-fidelity chaos testing on Samsung Galaxy A12 (SM-A125U).
 * 
 * This example shows how to integrate all 5 Environmental Realism Hooks:
 * 1. Network Instability Simulation
 * 2. Device Interruption Logic
 * 3. Battery Constraint Modeling
 * 4. Notification Distractions
 * 5. Context Switching Entropy
 */
public class EnvironmentalStressExample {
    
    /**
     * Example 1: Basic Environmental Stress Setup
     */
    public static void example1_BasicSetup() {
        System.out.println("=== Example 1: Basic Environmental Stress Setup ===\n");
        
        EnvironmentalConfig config = EnvironmentalConfig.galaxyA12Stress();
        String targetApp = "com.your.targetapp";
        
        EnvironmentalStressModel stressModel = new EnvironmentalStressModel(config, targetApp);
        
        stressModel.start();
        
        for (int i = 0; i < 50; i++) {
            stressModel.processInteraction();
            
            System.out.println("Interaction " + (i + 1));
            System.out.println("  Battery: " + stressModel.getBatteryHook().getCurrentBatteryPercentage() + "%");
            System.out.println("  Network: " + stressModel.getNetworkHook().getCurrentState().getDisplayName());
            System.out.println("  State: " + stressModel.getCurrentState().name());
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        System.out.println("\n" + stressModel.generateReport());
        
        stressModel.stop();
    }
    
    /**
     * Example 2: Network-Resilient Data Sync Testing
     */
    public static void example2_NetworkResilientSync() {
        System.out.println("=== Example 2: Network-Resilient Data Sync Testing ===\n");
        
        EnvironmentalConfig config = new EnvironmentalConfig.Builder()
                .enableNetworkInstability(true)
                .networkChangeProbability(0.20f)
                .minNetworkLatencyMs(200)
                .maxNetworkLatencyMs(5000)
                .networkFailureProbability(0.10f)
                .networkRecoveryTimeMs(10000)
                .enableBatteryConstraints(true)
                .initialBatteryPercentage(40)
                .batteryDrainRatePerMinute(2.0f)
                .build();
        
        String targetApp = "com.example.syncapp";
        EnvironmentalStressModel stressModel = new EnvironmentalStressModel(config, targetApp);
        
        stressModel.setEventCallback(event -> {
            System.out.println("[ENV EVENT] " + event.getType() + ": " + event.getDescription());
        });
        
        stressModel.start();
        
        for (int syncAttempt = 0; syncAttempt < 20; syncAttempt++) {
            stressModel.processInteraction();
            
            System.out.println("\nSync Attempt #" + (syncAttempt + 1));
            
            stressModel.beforeNetworkOperation();
            
            if (stressModel.shouldNetworkOperationFail()) {
                System.out.println("  [FAIL] Network operation failed");
                System.out.println("  [INFO] Retrying in 5 seconds...");
                
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                stressModel.beforeNetworkOperation();
            }
            
            if (!stressModel.getNetworkHook().isNetworkDown()) {
                System.out.println("  [SUCCESS] Data synced successfully");
                System.out.println("  [INFO] Latency: " + stressModel.getNetworkHook().getEffectiveLatency() + "ms");
            } else {
                System.out.println("  [FAIL] Network still down");
            }
            
            long modifiedDelay = stressModel.applyPowerSaveModifier(1000);
            try {
                Thread.sleep(modifiedDelay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        stressModel.stop();
        System.out.println("\n" + stressModel.generateReport());
    }
    
    /**
     * Example 3: Background Task Resilience Testing
     */
    public static void example3_BackgroundTaskResilience() {
        System.out.println("=== Example 3: Background Task Resilience Testing ===\n");
        
        EnvironmentalConfig config = EnvironmentalConfig.highChaos();
        String targetApp = "com.example.backgroundapp";
        
        EnvironmentalStressModel stressModel = new EnvironmentalStressModel(config, targetApp);
        stressModel.start();
        
        System.out.println("Testing critical task under interruptions...\n");
        
        for (int taskIteration = 0; taskIteration < 10; taskIteration++) {
            System.out.println("Task Iteration #" + (taskIteration + 1));
            stressModel.processInteraction();
            
            boolean taskInterrupted = false;
            boolean taskCompleted = false;
            String currentState = stressModel.getCurrentState().name();
            
            if (currentState.equals("INTERRUPTED") || currentState.equals("CONTEXT_SWITCHED")) {
                System.out.println("  [WARN] Task interrupted: " + currentState);
                taskInterrupted = true;
                
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                stressModel.processInteraction();
                currentState = stressModel.getCurrentState().name();
            }
            
            if (!currentState.equals("INTERRUPTED") && !currentState.equals("CONTEXT_SWITCHED")) {
                System.out.println("  [SUCCESS] Task completed");
                taskCompleted = true;
            } else {
                System.out.println("  [FAIL] Task abandoned due to interruption");
            }
            
            System.out.println("  Battery: " + stressModel.getBatteryHook().getCurrentBatteryPercentage() + "%");
            System.out.println("  Power Save: " + stressModel.getBatteryHook().isInPowerSaveMode());
            
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        stressModel.stop();
        System.out.println("\n" + stressModel.generateReport());
    }
    
    /**
     * Example 4: Multi-Session App Hopping Simulation
     */
    public static void example4_AppHoppingSimulation() {
        System.out.println("=== Example 4: Multi-Session App Hopping Simulation ===\n");
        
        EnvironmentalConfig config = new EnvironmentalConfig.Builder()
                .enableContextSwitching(true)
                .contextSwitchProbability(0.25f)
                .minContextSwitchIntervalMs(10000)
                .maxContextSwitchIntervalMs(30000)
                .minContextSwitchDurationMs(5000)
                .maxContextSwitchDurationMs(15000)
                .appHoppingEntropy(0.8f)
                .enableNotificationDistractions(true)
                .notificationProbability(0.12f)
                .build();
        
        String targetApp = "com.example.targetapp";
        EnvironmentalStressModel stressModel = new EnvironmentalStressModel(config, targetApp);
        
        stressModel.setEventCallback(event -> {
            switch (event.getType()) {
                case CONTEXT_SWITCH:
                    System.out.println("  [SWITCH] " + event.getDescription());
                    break;
                case NOTIFICATION_DISTRACTION:
                    System.out.println("  [NOTIF] " + event.getDescription());
                    break;
                case TASK_ABANDONMENT:
                    System.out.println("  [ABANDON] " + event.getDescription());
                    break;
                default:
                    break;
            }
        });
        
        stressModel.start();
        
        System.out.println("Simulating messy real-world multitasking...\n");
        
        int sessionsCompleted = 0;
        int sessionsAbandoned = 0;
        
        for (int session = 0; session < 15; session++) {
            System.out.println("\n--- Session #" + (session + 1) + " ---");
            
            stressModel.processInteraction();
            
            if (!stressModel.getContextHook().isInTargetApp()) {
                System.out.println("  [INFO] Current app: " + stressModel.getContextHook().getCurrentApp());
                
                if (stressModel.getContextHook().shouldAbandonTask()) {
                    System.out.println("  [ABANDON] Task abandoned - user didn't return");
                    sessionsAbandoned++;
                    continue;
                }
                
                System.out.println("  [WAIT] Waiting for user to return to target app...");
                
                for (int i = 0; i < 10; i++) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    
                    stressModel.processInteraction();
                    
                    if (stressModel.getContextHook().isInTargetApp()) {
                        System.out.println("  [RETURN] User returned to target app");
                        sessionsCompleted++;
                        break;
                    }
                }
            } else {
                System.out.println("  [INFO] User focused on target app");
                sessionsCompleted++;
            }
            
            System.out.println("  Switch entropy: " + 
                String.format("%.2f", stressModel.getContextHook().calculateSwitchEntropy()));
        }
        
        stressModel.stop();
        
        System.out.println("\n=== Session Summary ===");
        System.out.println("Completed: " + sessionsCompleted);
        System.out.println("Abandoned: " + sessionsAbandoned);
        System.out.println("Completion Rate: " + 
            String.format("%.1f%%", (sessionsCompleted * 100.0) / (sessionsCompleted + sessionsAbandoned)));
        
        System.out.println("\n" + stressModel.generateReport());
    }
    
    /**
     * Example 5: Power Save Mode Performance Testing
     */
    public static void example5_PowerSaveModeTesting() {
        System.out.println("=== Example 5: Power Save Mode Performance Testing ===\n");
        
        EnvironmentalConfig config = new EnvironmentalConfig.Builder()
                .enableBatteryConstraints(true)
                .initialBatteryPercentage(30)
                .batteryDrainRatePerMinute(3.0f)
                .powerSaveModeThreshold(15)
                .powerSaveModeInteractionModifier(0.6f)
                .build();
        
        String targetApp = "com.example.performanceapp";
        EnvironmentalStressModel stressModel = new EnvironmentalStressModel(config, targetApp);
        
        stressModel.setEventCallback(event -> {
            if (event.getType() == EnvironmentalStressModel.EnvironmentalEventType.POWER_SAVE_ENTERED) {
                System.out.println("  [BATTERY] " + event.getDescription());
            }
        });
        
        stressModel.start();
        
        System.out.println("Testing performance degradation with low battery...\n");
        
        int normalOperations = 0;
        int throttledOperations = 0;
        long totalNormalTime = 0;
        long totalThrottledTime = 0;
        
        for (int operation = 0; operation < 100; operation++) {
            stressModel.processInteraction();
            stressModel.getBatteryHook().updateBattery();
            
            long baseDelay = 500;
            long startTime = System.currentTimeMillis();
            
            stressModel.getBatteryHook().applyBatteryDelay();
            long modifiedDelay = stressModel.applyPowerSaveModifier(baseDelay);
            
            try {
                Thread.sleep(modifiedDelay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            long actualTime = System.currentTimeMillis() - startTime;
            
            if (stressModel.getBatteryHook().isInPowerSaveMode()) {
                throttledOperations++;
                totalThrottledTime += actualTime;
                System.out.println("Op " + (operation + 1) + " [THROTTLED] " + 
                    actualTime + "ms (battery: " + 
                    stressModel.getBatteryHook().getCurrentBatteryPercentage() + "%)");
            } else {
                normalOperations++;
                totalNormalTime += actualTime;
            }
            
            if (operation % 20 == 0 && operation > 0) {
                System.out.println("\n--- Performance Check ---");
                System.out.println("Normal ops: " + normalOperations + 
                    ", avg time: " + (totalNormalTime / normalOperations) + "ms");
                System.out.println("Throttled ops: " + throttledOperations + 
                    ", avg time: " + (totalThrottledTime / throttledOperations) + "ms");
                System.out.println("Degradation: " + 
                    String.format("%.1f%%", 
                        ((totalThrottledTime / (double)throttledOperations) / 
                         (totalNormalTime / (double)normalOperations) - 1) * 100));
                System.out.println();
            }
        }
        
        stressModel.stop();
        System.out.println("\n" + stressModel.generateReport());
    }
    
    /**
     * Example 6: Complete Chaos Scenario (All Hooks Active)
     */
    public static void example6_CompleteChaosScenario() {
        System.out.println("=== Example 6: Complete Chaos Scenario ===\n");
        System.out.println("Activating ALL environmental stressors simultaneously...\n");
        
        EnvironmentalConfig config = EnvironmentalConfig.highChaos();
        String targetApp = "com.example.chaostest";
        
        EnvironmentalStressModel stressModel = new EnvironmentalStressModel(config, targetApp);
        
        stressModel.setEventCallback(event -> {
            System.out.println(String.format("[%s] %s", 
                event.getType().name().substring(0, 3), event.getDescription()));
        });
        
        stressModel.start();
        
        System.out.println("Starting chaos test sequence...\n");
        
        int successfulOperations = 0;
        int failedOperations = 0;
        int abandonedTasks = 0;
        
        for (int iteration = 0; iteration < 30; iteration++) {
            System.out.println("\n--- Chaos Iteration #" + (iteration + 1) + " ---");
            System.out.println("Time: " + (stressModel.getSessionDurationMs() / 1000) + "s");
            
            stressModel.processInteraction();
            
            EnvironmentalStressModel.EnvironmentalState state = stressModel.getCurrentState();
            System.out.println("State: " + state.name());
            
            if (state == EnvironmentalStressModel.EnvironmentalState.NORMAL) {
                successfulOperations++;
                System.out.println("  ✓ Operation completed normally");
            } else if (state == EnvironmentalStressModel.EnvironmentalState.CONTEXT_SWITCHED ||
                       state == EnvironmentalStressModel.EnvironmentalState.INTERRUPTED) {
                if (stressModel.getContextHook().shouldAbandonTask()) {
                    abandonedTasks++;
                    System.out.println("  ✗ Task abandoned");
                } else {
                    System.out.println("  ⚠ Task interrupted, waiting for recovery...");
                    
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    
                    stressModel.processInteraction();
                    if (stressModel.getCurrentState() == EnvironmentalStressModel.EnvironmentalState.NORMAL) {
                        successfulOperations++;
                        System.out.println("  ✓ Task recovered");
                    } else {
                        failedOperations++;
                        System.out.println("  ✗ Task failed after interruption");
                    }
                }
            } else if (state == EnvironmentalStressModel.EnvironmentalState.NETWORK_DOWN) {
                stressModel.beforeNetworkOperation();
                if (stressModel.getNetworkHook().isNetworkDown()) {
                    failedOperations++;
                    System.out.println("  ✗ Network operation failed");
                } else {
                    successfulOperations++;
                    System.out.println("  ✓ Network recovered and operation succeeded");
                }
            } else {
                System.out.println("  ⚠ Operation completed with constraints");
                successfulOperations++;
            }
            
            System.out.println("  Battery: " + stressModel.getBatteryHook().getCurrentBatteryPercentage() + "%");
            System.out.println("  Network: " + stressModel.getNetworkHook().getCurrentState().getDisplayName());
            
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        stressModel.stop();
        
        System.out.println("\n=== Chaos Test Results ===");
        System.out.println("Successful Operations: " + successfulOperations);
        System.out.println("Failed Operations: " + failedOperations);
        System.out.println("Abandoned Tasks: " + abandonedTasks);
        System.out.println("Success Rate: " + 
            String.format("%.1f%%", (successfulOperations * 100.0) / (successfulOperations + failedOperations)));
        
        System.out.println("\n" + stressModel.generateReport());
    }
    
    public static void main(String[] args) {
        System.out.println("Environmental Stress Model Examples");
        System.out.println("====================================\n");
        
        System.out.println("Choose an example to run:");
        System.out.println("1. Basic Setup");
        System.out.println("2. Network-Resilient Sync");
        System.out.println("3. Background Task Resilience");
        System.out.println("4. App Hopping Simulation");
        System.out.println("5. Power Save Mode Testing");
        System.out.println("6. Complete Chaos Scenario");
        System.out.println();
        
        if (args.length == 0) {
            System.out.println("Running Example 1 (Basic Setup) as default...\n");
            example1_BasicSetup();
        } else {
            int example = Integer.parseInt(args[0]);
            switch (example) {
                case 1:
                    example1_BasicSetup();
                    break;
                case 2:
                    example2_NetworkResilientSync();
                    break;
                case 3:
                    example3_BackgroundTaskResilience();
                    break;
                case 4:
                    example4_AppHoppingSimulation();
                    break;
                case 5:
                    example5_PowerSaveModeTesting();
                    break;
                case 6:
                    example6_CompleteChaosScenario();
                    break;
                default:
                    System.out.println("Invalid example number");
            }
        }
    }
}
