package com.samsungcloak.coherence;

/**
 * Demonstration of the Physical & Ecosystem Coherence Layer
 * for Samsung Galaxy A12 (SM-A125U) Digital Twin Framework
 *
 * This demo showcases all five coherence hooks working together
 * to provide high-fidelity human-device interaction simulation.
 */
public class CoherenceLayerDemo {

    private static final String LOG_TAG = "SamsungCloak.CoherenceDemo";

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("Physical & Ecosystem Coherence Layer Demo");
        System.out.println("Samsung Galaxy A12 (SM-A125U) Digital Twin");
        System.out.println("========================================\n");

        // Initialize the Coherence Engine
        CoherenceEngine.init();
        CoherenceEngine engine = CoherenceEngine.getInstance();
        engine.start();

        System.out.println("Coherence Engine initialized and started.\n");

        // Demonstrate Mechanical Micro-Error Hook
        demonstrateMechanicalMicroError(engine);

        // Demonstrate Sensor-Fusion Coherence Hook
        demonstrateSensorFusionCoherence(engine);

        // Demonstrate Inter-App Navigation Context Hook
        demonstrateInterAppNavigation(engine);

        // Demonstrate Input Pressure Dynamics Hook
        demonstrateInputPressureDynamics(engine);

        // Demonstrate Asymmetric Latency Hook
        demonstrateAsymmetricLatency(engine);

        // Get comprehensive engine state
        demonstrateEngineState(engine);

        System.out.println("\n========================================");
        System.out.println("Demo completed successfully!");
        System.out.println("========================================");

        engine.stop();
    }

    /**
     * Demonstrates Mechanical Micro-Error Hook capabilities.
     */
    private static void demonstrateMechanicalMicroError(CoherenceEngine engine) {
        System.out.println("--- Mechanical Micro-Error Hook Demo ---\n");

        MechanicalMicroErrorHook hook = engine.getMechanicalMicroErrorHook();

        // Simulate 5 taps to show fat-finger behavior
        System.out.println("Simulating 5 taps on a target at (320, 480):");
        for (int i = 0; i < 5; i++) {
            MechanicalMicroErrorHook.TapResult result = hook.simulateTap(320, 480);
            System.out.printf("  Tap %d: Actual (%.1f, %.1f), Offset (%.1f, %.1f), " +
                            "Near Miss: %b, Partial Press: %b%n",
                    i + 1, result.actualX, result.actualY,
                    result.offsetX, result.offsetY,
                    result.isNearMiss, result.isPartialPress);
        }

        // Simulate a scroll with potential overshoot
        System.out.println("\nSimulating scroll from (0, 800) to (0, 400):");
        MechanicalMicroErrorHook.ScrollResult scroll =
                hook.simulateScroll(0, 800, 0, 400, 500);
        System.out.printf("  Distance: %.1fpx, Has Overshoot: %b%n",
                scroll.scrollDistance, scroll.hasOvershoot);
        if (scroll.hasOvershoot) {
            System.out.printf("  Overshoot: %.1fpx, Correction: %.1fpx, " +
                            "Correction Delay: %.1fms%n",
                    scroll.overshootDistance, scroll.correctionDistance,
                    scroll.correctionDelayMs);
        }

        System.out.println();
    }

    /**
     * Demonstrates Sensor-Fusion Coherence Hook capabilities.
     */
    private static void demonstrateSensorFusionCoherence(CoherenceEngine engine) {
        System.out.println("--- Sensor-Fusion Coherence Hook Demo ---\n");

        SensorFusionCoherenceHook hook = engine.getSensorFusionCoherenceHook();

        // Simulate walking at 1.2 m/s
        System.out.println("Simulating pedestrian movement (walking at 1.2 m/s):");
        SensorFusionCoherenceHook.SensorFusionState state =
                hook.updateMovement(1.2, 45.0,
                        SensorFusionCoherenceHook.MovementState.WALKING,
                        System.currentTimeMillis());

        System.out.printf("  Movement State: %s%n", state.movementState);
        System.out.printf("  Velocity: %.2f m/s%n", state.velocity);
        System.out.printf("  Heading: %.1f degrees%n", state.heading);
        System.out.printf("  Step Frequency: %.2f Hz%n", state.stepFrequency);
        System.out.printf("  Step Count: %d%n", state.stepCount);

        // Show sensor readings
        System.out.printf("  Accelerometer: [%.2f, %.2f, %.2f] m/s^2%n",
                state.accelerometer[0], state.accelerometer[1], state.accelerometer[2]);
        System.out.printf("  Gyroscope: [%.2f, %.2f, %.2f] rad/s%n",
                state.gyroscope[0], state.gyroscope[1], state.gyroscope[2]);
        System.out.printf("  GPS Velocity: %.2f m/s (accuracy: %.2f m)%n",
                state.gpsVelocity.velocity, state.gpsVelocity.accuracy);

        // Show coherence metrics
        SensorFusionCoherenceHook.CoherenceMetrics metrics = hook.getCoherenceMetrics();
        System.out.printf("  GPS-Accel Coherence: %.2f%n", metrics.gpsAccelCoherenceScore);
        System.out.printf("  Step-Cycle Consistency: %.2f%n", metrics.stepCycleConsistencyScore);

        System.out.println();
    }

    /**
     * Demonstrates Inter-App Navigation Context Hook capabilities.
     */
    private static void demonstrateInterAppNavigation(CoherenceEngine engine) {
        System.out.println("--- Inter-App Navigation Context Hook Demo ---\n");

        InterAppNavigationContextHook hook = engine.getInterAppNavigationContextHook();

        // Simulate referral from Chrome to target app
        System.out.println("Simulating referral flow (Chrome -> Target App):");
        InterAppNavigationContextHook.ReferralFlowResult referral =
                hook.simulateReferralFlow(
                        "com.example.targetapp",
                        "https://example.com/product/12345?utm_source=chrome",
                        InterAppNavigationContextHook.ReferralSource.BROWSER_CHROME
                );

        System.out.printf("  Source: %s (%s)%n",
                referral.source.displayName, referral.source.packageName);
        System.out.printf("  Target: %s%n", referral.targetApp);
        System.out.printf("  Deep Link: %s%n", referral.deepLinkUrl);
        System.out.printf("  Navigation Intent: %s%n", referral.intent);
        System.out.printf("  Transition Latency: %.1f ms%n", referral.transitionLatencyMs);
        System.out.printf("  Browse Duration: %.1f ms%n", referral.browseBehavior.browseDurationMs);
        System.out.printf("  Context Data Size: %d bytes%n", referral.contextTransfer.contextDataSize);

        // Show navigation state
        InterAppNavigationContextHook.NavigationState navState = hook.getNavigationState();
        System.out.printf("  Current App: %s%n", navState.currentApp);
        System.out.printf("  Active Sessions: %d%n", navState.activeSessionCount);

        System.out.println();
    }

    /**
     * Demonstrates Input Pressure Dynamics Hook capabilities.
     */
    private static void demonstrateInputPressureDynamics(CoherenceEngine engine) {
        System.out.println("--- Input Pressure Dynamics Hook Demo ---\n");

        InputPressureDynamicsHook hook = engine.getInputPressureDynamicsHook();

        // Simulate different interaction types
        System.out.println("Simulating different interaction types:");

        // Button tap
        System.out.println("  Button Tap:");
        InputPressureDynamicsHook.TouchDownResult buttonTap =
                hook.simulateTouchDown(InputPressureDynamicsHook.InteractionType.BUTTON_TAP);
        System.out.printf("    Pressure: %.2f, Major: %.1fpx, Minor: %.1fpx%n",
                buttonTap.pressure, buttonTap.contactArea.majorAxis,
                buttonTap.contactArea.minorAxis);

        // Scroll
        System.out.println("  Scroll (300px movement):");
        InputPressureDynamicsHook.TouchDownResult scrollDown =
                hook.simulateTouchDown(InputPressureDynamicsHook.InteractionType.SCROLL);
        InputPressureDynamicsHook.TouchMoveResult scrollMove =
                hook.simulateTouchMove(0, -300, InputPressureDynamicsHook.InteractionType.SCROLL);
        System.out.printf("    Initial Pressure: %.2f%n", scrollDown.pressure);
        System.out.printf("    During Scroll Pressure: %.2f, Expansion: %.2fx%n",
                scrollMove.pressure, scrollMove.expansionRatio);

        // Long press
        System.out.println("  Long Press:");
        InputPressureDynamicsHook.TouchDownResult longPressDown =
                hook.simulateTouchDown(InputPressureDynamicsHook.InteractionType.LONG_PRESS);
        System.out.printf("    Initial Pressure: %.2f%n", longPressDown.pressure);

        // Multi-touch
        System.out.println("  Pinch Gesture (2 fingers):");
        InputPressureDynamicsHook.MultiTouchResult pinch =
                hook.simulateMultiTouch(2, InputPressureDynamicsHook.InteractionType.PINCH);
        System.out.printf("    Pointer 1 Pressure: %.2f%n", pinch.pressures[0]);
        System.out.printf("    Pointer 2 Pressure: %.2f%n", pinch.pressures[1]);

        // Touch up
        InputPressureDynamicsHook.TouchUpResult touchUp = hook.simulateTouchUp();
        System.out.printf("  Touch Up - Final Pressure: %.2f, Duration: %.1f ms%n",
                touchUp.pressure, touchUp.contactDurationMs);

        System.out.println();
    }

    /**
     * Demonstrates Asymmetric Latency Hook capabilities.
     */
    private static void demonstrateAsymmetricLatency(CoherenceEngine engine) {
        System.out.println("--- Asymmetric Latency Hook Demo ---\n");

        AsymmetricLatencyHook hook = engine.getAsymmetricLatencyHook();

        // Update user state
        hook.updateUserState(10 * 60 * 1000, 0); // 10 minutes, no errors

        // Simulate perceptual gap for different UI events
        System.out.println("Simulating perceptual gaps after UI events:");

        AsymmetricLatencyHook.UILoadEventType[] events = {
                AsymmetricLatencyHook.UILoadEventType.SCREEN_TRANSITION,
                AsymmetricLatencyHook.UILoadEventType.CONTENT_LOAD,
                AsymmetricLatencyHook.UILoadEventType.FORM_SUBMIT
        };

        AsymmetricLatencyHook.InformationDensity density =
                AsymmetricLatencyHook.InformationDensity.MEDIUM;

        for (AsymmetricLatencyHook.UILoadEventType event : events) {
            AsymmetricLatencyHook.PerceptualGapResult gap =
                    hook.simulatePerceptualGap(event, density);
            System.out.printf("  %s:%n", event.displayName);
            System.out.printf("    Perceptual Gap: %.1f ms%n", gap.perceptualGapMs);
            System.out.printf("    Network Latency: %.1f ms%n", gap.asymmetry.networkLatency);
            System.out.printf("    UI Latency: %.1f ms%n", gap.asymmetry.uiLatency);
            System.out.printf("    Asymmetry Ratio: %.2f%n", gap.asymmetry.asymmetryRatio);
            System.out.printf("    User State: %s%n", gap.userState);
            System.out.printf("    Adaptation Factor: %.2f%n", gap.adaptationFactor);
        }

        // Simulate network latency
        System.out.println("\nSimulating network latency:");
        AsymmetricLatencyHook.NetworkLatencyResult wifiLatency =
                hook.simulateNetworkLatency(5000, true);
        System.out.printf("  WiFi (5KB request): %.1f ms (jitter: %.1f ms, packet loss: %b)%n",
                wifiLatency.networkLatencyMs, wifiLatency.jitter,
                wifiLatency.packetLossOccurred);

        AsymmetricLatencyHook.NetworkLatencyResult cellularLatency =
                hook.simulateNetworkLatency(5000, false);
        System.out.printf("  Cellular (5KB request): %.1f ms (jitter: %.1f ms, packet loss: %b)%n",
                cellularLatency.networkLatencyMs, cellularLatency.jitter,
                cellularLatency.packetLossOccurred);

        // Show latency statistics
        AsymmetricLatencyHook.LatencyStatistics stats = hook.getLatencyStatistics();
        System.out.printf("\n  Average Perceptual Gap: %.1f ms%n", stats.avgPerceptualGapMs);
        System.out.printf("  Average Network Latency: %.1f ms%n", stats.avgNetworkLatencyMs);
        System.out.printf("  Average UI Latency: %.1f ms%n", stats.avgUILatencyMs);

        System.out.println();
    }

    /**
     * Demonstrates comprehensive engine state retrieval.
     */
    private static void demonstrateEngineState(CoherenceEngine engine) {
        System.out.println("--- Comprehensive Engine State ---\n");

        CoherenceEngine.CoherenceEngineState state = engine.getEngineState();

        System.out.println("Engine Status:");
        System.out.printf("  Running: %b%n", state.isRunning);
        System.out.printf("  Engine Start Time: %d%n", state.engineStartTime);
        System.out.printf("  Last Update: %d%n", state.lastUpdateTime);
        System.out.printf("  In Correction Swipe: %b%n", state.isInCorrectionSwipe);

        System.out.println("\nSensor Fusion Metrics:");
        System.out.printf("  GPS-Accel Coherence: %.2f%n",
                state.sensorFusionMetrics.gpsAccelCoherenceScore);
        System.out.printf("  Step-Cycle Consistency: %.2f%n",
                state.sensorFusionMetrics.stepCycleConsistencyScore);
        System.out.printf("  Movement State: %s%n",
                state.sensorFusionMetrics.movementState);

        System.out.println("\nNavigation State:");
        System.out.printf("  Current App: %s%n", state.navigationState.currentApp);
        System.out.printf("  Current Intent: %s%n", state.navigationState.currentIntent);
        System.out.printf("  Active Sessions: %d%n", state.navigationState.activeSessionCount);

        System.out.println("\nTouch State:");
        System.out.printf("  Current Pressure: %.2f%n", state.touchState.pressure);
        System.out.printf("  Touch Width: %.1f px%n", state.touchState.touchWidth);
        System.out.printf("  Touch Height: %.1f px%n", state.touchState.touchHeight);
        System.out.printf("  Contact Duration: %.1f ms%n", state.touchState.contactDurationMs);
        System.out.printf("  In Contact: %b%n", state.touchState.isInContact);

        System.out.println("\nLatency Statistics:");
        System.out.printf("  Avg Perceptual Gap: %.1f ms%n",
                state.latencyStatistics.avgPerceptualGapMs);
        System.out.printf("  Avg Network Latency: %.1f ms%n",
                state.latencyStatistics.avgNetworkLatencyMs);
        System.out.printf("  Avg UI Latency: %.1f ms%n",
                state.latencyStatistics.avgUILatencyMs);
        System.out.printf("  Avg Asymmetry Ratio: %.2f%n",
                state.latencyStatistics.avgAsymmetryRatio);

        System.out.println();
    }
}
