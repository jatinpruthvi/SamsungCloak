package com.cognitive.testing.environmental;

/**
 * Quick validation test for Environmental Stress Model components.
 * Verifies basic functionality of all 5 hooks.
 */
public class EnvironmentalStressTest {
    
    public static void main(String[] args) {
        System.out.println("=== Environmental Stress Model Validation ===\n");
        
        boolean allTestsPassed = true;
        
        // Test 1: Configuration
        System.out.println("Test 1: Configuration");
        allTestsPassed &= testConfiguration();
        System.out.println();
        
        // Test 2: Network Instability Hook
        System.out.println("Test 2: Network Instability Hook");
        allTestsPassed &= testNetworkInstabilityHook();
        System.out.println();
        
        // Test 3: Device Interruption Hook
        System.out.println("Test 3: Device Interruption Hook");
        allTestsPassed &= testDeviceInterruptionHook();
        System.out.println();
        
        // Test 4: Battery Constraint Hook
        System.out.println("Test 4: Battery Constraint Hook");
        allTestsPassed &= testBatteryConstraintHook();
        System.out.println();
        
        // Test 5: Notification Distraction Hook
        System.out.println("Test 5: Notification Distraction Hook");
        allTestsPassed &= testNotificationDistractionHook();
        System.out.println();
        
        // Test 6: Context Switching Hook
        System.out.println("Test 6: Context Switching Hook");
        allTestsPassed &= testContextSwitchingHook();
        System.out.println();
        
        // Test 7: Environmental Stress Model
        System.out.println("Test 7: Environmental Stress Model");
        allTestsPassed &= testEnvironmentalStressModel();
        System.out.println();
        
        System.out.println("=== Test Summary ===");
        if (allTestsPassed) {
            System.out.println("✓ All tests PASSED");
            System.exit(0);
        } else {
            System.out.println("✗ Some tests FAILED");
            System.exit(1);
        }
    }
    
    private static boolean testConfiguration() {
        try {
            EnvironmentalConfig defaults = EnvironmentalConfig.defaults();
            EnvironmentalConfig lowChaos = EnvironmentalConfig.lowChaos();
            EnvironmentalConfig highChaos = EnvironmentalConfig.highChaos();
            EnvironmentalConfig galaxyA12 = EnvironmentalConfig.galaxyA12Stress();
            
            assert defaults != null : "Default config is null";
            assert lowChaos != null : "Low chaos config is null";
            assert highChaos != null : "High chaos config is null";
            assert galaxyA12 != null : "Galaxy A12 config is null";
            
            assert highChaos.getNetworkChangeProbability() > lowChaos.getNetworkChangeProbability() 
                : "High chaos should have higher network probability";
            assert highChaos.getInterruptionProbability() > lowChaos.getInterruptionProbability()
                : "High chaos should have higher interruption probability";
            
            EnvironmentalConfig custom = new EnvironmentalConfig.Builder()
                .networkChangeProbability(0.50f)
                .build();
            
            assert custom.getNetworkChangeProbability() == 0.50f : "Custom config builder failed";
            
            System.out.println("✓ Configuration test PASSED");
            return true;
        } catch (Exception e) {
            System.out.println("✗ Configuration test FAILED: " + e.getMessage());
            return false;
        }
    }
    
    private static boolean testNetworkInstabilityHook() {
        try {
            EnvironmentalConfig config = EnvironmentalConfig.defaults();
            NetworkInstabilityHook hook = new NetworkInstabilityHook(config);
            
            assert hook.getCurrentState() == NetworkInstabilityHook.NetworkState.FOUR_G
                : "Initial state should be 4G";
            
            hook.forceNetworkState(NetworkInstabilityHook.NetworkState.THREE_G);
            assert hook.getCurrentState() == NetworkInstabilityHook.NetworkState.THREE_G
                : "Forced state should be 3G";
            
            hook.changeNetworkState();
            NetworkInstabilityHook.NetworkStatistics stats = hook.getStatistics();
            
            assert stats.getTotalNetworkChanges() > 0 : "Network changes should be > 0";
            assert stats.getCurrentState() != null : "Current state should not be null";
            
            System.out.println("✓ Network Instability Hook test PASSED");
            return true;
        } catch (Exception e) {
            System.out.println("✗ Network Instability Hook test FAILED: " + e.getMessage());
            return false;
        }
    }
    
    private static boolean testDeviceInterruptionHook() {
        try {
            EnvironmentalConfig config = EnvironmentalConfig.defaults();
            DeviceInterruptionHook hook = new DeviceInterruptionHook(config);
            
            hook.triggerInterruption(DeviceInterruptionHook.InterruptionType.INCOMING_CALL);
            
            DeviceInterruptionHook.InterruptionStatistics stats = hook.getStatistics();
            
            assert stats.getTotalInterruptions() > 0 : "Interruptions should be > 0";
            assert stats.getLastType() != DeviceInterruptionHook.InterruptionType.NONE
                : "Last type should not be NONE";
            
            System.out.println("✓ Device Interruption Hook test PASSED");
            return true;
        } catch (Exception e) {
            System.out.println("✗ Device Interruption Hook test FAILED: " + e.getMessage());
            return false;
        }
    }
    
    private static boolean testBatteryConstraintHook() {
        try {
            EnvironmentalConfig config = EnvironmentalConfig.defaults();
            BatteryConstraintHook hook = new BatteryConstraintHook(config);
            
            int initialLevel = hook.getCurrentBatteryPercentage();
            assert initialLevel == config.getInitialBatteryPercentage()
                : "Initial battery level should match config";
            
            hook.setBatteryPercentage(10);
            assert hook.getCurrentBatteryPercentage() == 10 : "Battery level should be 10%";
            assert hook.isInPowerSaveMode() : "Should be in power save mode at 10%";
            
            hook.setBatteryPercentage(80);
            assert !hook.isInPowerSaveMode() : "Should not be in power save mode at 80%";
            
            BatteryConstraintHook.BatteryStatistics stats = hook.getStatistics();
            assert stats.getCurrentBatteryPercentage() == 80 : "Stats battery should be 80%";
            
            System.out.println("✓ Battery Constraint Hook test PASSED");
            return true;
        } catch (Exception e) {
            System.out.println("✗ Battery Constraint Hook test FAILED: " + e.getMessage());
            return false;
        }
    }
    
    private static boolean testNotificationDistractionHook() {
        try {
            EnvironmentalConfig config = EnvironmentalConfig.defaults();
            NotificationDistractionHook hook = new NotificationDistractionHook(config);
            
            hook.triggerDistraction(NotificationDistractionHook.NotificationType.SOCIAL_MEDIA, "com.instagram.android");
            
            NotificationDistractionHook.NotificationStatistics stats = hook.getStatistics();
            
            assert stats.getTotalNotifications() > 0 : "Notifications should be > 0";
            assert stats.getFocusLossCount() > 0 : "Focus loss count should be > 0";
            
            float recoveryRate = hook.getFocusRecoveryRate();
            assert recoveryRate >= 0.0f && recoveryRate <= 1.0f 
                : "Recovery rate should be between 0 and 1";
            
            System.out.println("✓ Notification Distraction Hook test PASSED");
            return true;
        } catch (Exception e) {
            System.out.println("✗ Notification Distraction Hook test FAILED: " + e.getMessage());
            return false;
        }
    }
    
    private static boolean testContextSwitchingHook() {
        try {
            String targetApp = "com.example.targetapp";
            EnvironmentalConfig config = EnvironmentalConfig.defaults();
            ContextSwitchingHook hook = new ContextSwitchingHook(config, targetApp);
            
            assert hook.isInTargetApp() : "Should initially be in target app";
            
            hook.switchToApp("com.android.chrome");
            assert !hook.isInTargetApp() : "Should not be in target app after switch";
            assert hook.getCurrentApp().equals("com.android.chrome") : "Current app should be chrome";
            
            hook.switchToApp(targetApp);
            assert hook.isInTargetApp() : "Should be back in target app";
            
            ContextSwitchingHook.ContextSwitchStatistics stats = hook.getStatistics();
            assert stats.getTotalSwitches() > 0 : "Switches should be > 0";
            
            float entropy = hook.calculateSwitchEntropy();
            assert entropy >= 0.0f && entropy <= 1.0f : "Entropy should be between 0 and 1";
            
            System.out.println("✓ Context Switching Hook test PASSED");
            return true;
        } catch (Exception e) {
            System.out.println("✗ Context Switching Hook test FAILED: " + e.getMessage());
            return false;
        }
    }
    
    private static boolean testEnvironmentalStressModel() {
        try {
            String targetApp = "com.example.targetapp";
            EnvironmentalConfig config = EnvironmentalConfig.defaults();
            EnvironmentalStressModel model = new EnvironmentalStressModel(config, targetApp);
            
            assert model.isActive() == false : "Model should not be active initially";
            
            model.start();
            assert model.isActive() == true : "Model should be active after start()";
            
            model.processInteraction();
            EnvironmentalStressModel.EnvironmentalStatistics stats = model.getStatistics();
            
            assert stats != null : "Statistics should not be null";
            assert stats.getSessionDurationMs() >= 0 : "Session duration should be >= 0";
            
            assert model.getNetworkHook() != null : "Network hook should not be null";
            assert model.getInterruptionHook() != null : "Interruption hook should not be null";
            assert model.getBatteryHook() != null : "Battery hook should not be null";
            assert model.getNotificationHook() != null : "Notification hook should not be null";
            assert model.getContextHook() != null : "Context hook should not be null";
            
            model.stop();
            assert model.isActive() == false : "Model should not be active after stop()";
            
            String report = model.generateReport();
            assert report != null && !report.isEmpty() : "Report should not be empty";
            assert report.contains("ENVIRONMENTAL STRESS MODEL REPORT") 
                : "Report should contain header";
            
            System.out.println("✓ Environmental Stress Model test PASSED");
            return true;
        } catch (Exception e) {
            System.out.println("✗ Environmental Stress Model test FAILED: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
