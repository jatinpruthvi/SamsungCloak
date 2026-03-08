# Structural Fidelity Layer

A Java-based interaction framework for validating app performance and infrastructure resilience across millions of simulated, unique users. Optimized for Samsung Galaxy A12 (SM-A125U) hardware profiles.

## Overview

The Structural Fidelity Layer implements six core "Population Dimensions" to ensure high-fidelity benchmarking of synthetic populations:

1. **Heterogeneous Interdependence** - Shared context modeling with realistic network/social overlaps
2. **Long-Term Behavioral Drift** - 90-day evolution from "New User" to "Power User"
3. **Population Profiling** - Power Law and Gaussian distributional alignment
4. **Structural Network Topology** - Small-world network interaction graphs
5. **Telemetry Sanity Checking** - Human motor constraint validation
6. **Multi-Session Persistent State** - Longitudinal memory and task persistence

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│               StructuralFidelityOrchestrator                     │
│                    (Master Integration Layer)                    │
└──────┬──────────────┬──────────────┬──────────────┬─────────────┘
       │              │              │              │
       ▼              ▼              ▼              ▼
┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐
│   Shared   │ │  Evolution │ │   Density  │ │    Small   │
│  Context   │ │   Engine   │ │  Profiler  │ │   World    │
│   Model    │ │            │ │            │ │  Network   │
└────────────┘ └────────────┘ └────────────┘ └────────────┘
       │                                      │
       ▼                                      ▼
┌────────────┐                       ┌────────────┐
│  Threshold │                       │Longitudinal│
│  Monitor   │                       │   Logic    │
└────────────┘                       └────────────┘
```

## Quick Start

### Build the Project

```bash
cd structural-fidelity
mvn clean package
```

### Run the Demo

```bash
java -jar target/structural-fidelity-layer.jar
```

Or with Maven:

```bash
mvn exec:java -Dexec.mainClass="com.simulation.structural.demo.StructuralFidelityDemo"
```

## Usage Example

```java
// Configure simulation
SimulationConfig config = new SimulationConfig()
    .withPopulationSize(10000)
    .withSimulationDays(90);

// Initialize orchestrator
StructuralFidelityOrchestrator orchestrator =
    new StructuralFidelityOrchestrator(config);

// Run full simulation pipeline
orchestrator.generatePopulation();
orchestrator.assignSharedContexts();
orchestrator.buildNetworkTopology();
SimulationResult result = orchestrator.runSimulation();

// Access results
System.out.println("Evolution summary: " + result.evolutionSummary);
System.out.println("Violations: " + result.violationStats);
```

## Population Dimensions

### 1. Heterogeneous Interdependence (SharedContextModel)

Models realistic network/social overlaps where device clusters share contextual similarities without creating identical telemetry signatures.

**Key Features:**
- Context clustering (household, office, public WiFi)
- Entanglement coefficient modeling
- Temporal synchronization with individual variance
- Network profile inheritance with divergence

```java
SharedContextModel household = SharedContextModel.createHouseholdCluster(
    4, "HOUSEHOLD_001"
);
NetworkProfile profile = sharedContext.getContextualNetworkProfile(deviceId);
```

### 2. Long-Term Behavioral Drift (EvolutionEngine)

Adjusts interaction entropy over a 90-day window simulating natural user progression.

**Mathematical Models:**
- Entropy: H(t) = H∞ + (H₀ - H∞) × e^(-λt)
- Skill: S(t) = S_max × (1 - e^(-kt))
- Phases: New User → Learning → Competent → Power User

```java
EvolutionEngine engine = new EvolutionEngine();
engine.registerUser("USER_001");

// Advance one day
engine.advanceSimulationDay();

// Get predictions
long latency = engine.predictNavigationLatency("USER_001", 200, 44);
```

### 3. Population Profiling (DensityProfiler)

Uses Power Law and Gaussian distributions for human-aligned population benchmarks.

**Distributions:**
- **Pareto (Power Law)**: Session frequency, feature adoption
- **Log-Normal**: Session duration
- **Gaussian**: Reaction latencies
- **Beta**: Daily active probability

```java
DensityProfiler profiler = new DensityProfiler();
int sessionsPerDay = profiler.generateSessionFrequency();
int sessionDuration = profiler.generateSessionDuration();
long reactionLatency = profiler.generateReactionLatency();
```

### 4. Structural Network Topology (SmallWorldNetwork)

Constructs small-world networks with Watts-Strogatz model for realistic community structure.

**Metrics:**
- Clustering coefficient: C ≈ 0.3-0.6
- Average path length: L ~ log(N)
- Small-world coefficient: σ > 1

```java
SmallWorldNetwork network = new SmallWorldNetwork();
network.buildWattsStrogatzNetwork(1000, 6, 0.15);
network.applyTriadicClosure(0.3);

NetworkMetrics metrics = network.getMetrics();
```

### 5. Telemetry Sanity Checking (ThresholdMonitor)

Prevents mechanical anomalies by validating against human motor constraints.

**Physiological Bounds:**
- Simple reaction time: 150-1000ms
- Movement time (Fitts' Law): 80-2000ms
- Inter-key interval: 70ms minimum
- Scroll velocity: 0-5000 px/s

```java
ThresholdMonitor monitor = new ThresholdMonitor();
long validLatency = monitor.validateReactionTime(measuredLatency, "context");
double validPressure = monitor.validateTouchPressure(pressure, "context");
```

### 6. Multi-Session Persistent State (LongitudinalLogic)

Maintains memory across sessions: episodic, semantic, procedural, and working memory.

**Memory Systems:**
- Episodic: Event-specific memories with decay
- Semantic: Feature preferences and categories
- Procedural: Skill-based learning
- Working: Current session context (7±2 chunks)

```java
LongitudinalLogic logic = new LongitudinalLogic();
SessionPreparation prep = logic.prepareSession("USER_001");

// Record session
logic.recordSession("USER_001", sessionRecord);
```

## Hardware Profile: Samsung Galaxy A12 (SM-A125U)

### Specifications
- Display: 720×1600 pixels, 6.5" PLS TFT
- Touch latency baseline: ~28ms
- Network: WiFi/4G hybrid

### Calibration Parameters
```java
PopulationParameters params = new PopulationParameters()
    .withLatencyParams(250, 45)  // mean, stdev ms
    .withEngagementSkew(0.8);
```

## Testing

```bash
mvn test
```

## Integration with Existing Framework

The Structural Fidelity Layer integrates with:
- **User Evolution Model**: Skill curves and lifecycle phases
- **Distributional Fidelity Model**: Statistical distributions
- **Cognitive Testing Framework**: Behavioral realism hooks
- **Network Topology**: Social graph models

## Performance

- 10,000 users × 90 days: ~30 seconds
- Memory usage: ~150MB per 10,000 users
- Thread-safe for concurrent simulation

## License

This project is part of the SamsungCloak simulation framework.
