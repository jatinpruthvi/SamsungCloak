# Distributional Fidelity Model

## Overview

This is a comprehensive Java-based simulation framework for implementing realistic population modeling in app infrastructure testing. The model implements **Distributional Fidelity** to ensure simulations reflect real-world human behavior rather than artificial uniform patterns.

**Target Device:** Samsung Galaxy A12 (SM-A125U)
- SoC: MediaTek Helio P35 (MT6765)
- RAM: 4GB
- CPU: Octa-core (4x2.3 GHz Cortex-A53 & 4x1.8 GHz Cortex-A53)
- GPU: PowerVR GE8320

## Core Principle

> **"Real populations follow probabilistic distributions, not uniform patterns."**

## Statistical Realism Hooks

### 1. Conversion Rate Variance (Binomial Distribution)

**Purpose:** Model goal completion (e.g., successful checkouts) using a Binomial Distribution to simulate realistic success/failure fluctuations across a population.

**Mathematical Foundation:**
```
P(X = k) = C(n, k) * p^k * (1-p)^(n-k)
```

Where:
- n = number of trials
- k = number of successes
- p = probability of success on each trial
- C(n, k) = binomial coefficient

**Key Features:**
- Single conversion simulation with configurable probability
- Batch conversion simulation for multiple users
- Population-level conversion rate variance
- Temporal variance (conversion rates vary over time)
- Expected value and variance calculations

**Usage Example:**
```java
// Initialize with 15% conversion rate
BinomialDistribution conversionDist = new BinomialDistribution(0.15);

// Simulate a single conversion
boolean converted = conversionDist.simulateSingleConversion();

// Simulate 100 conversion attempts
int successes = conversionDist.simulateConversions(100);

// Expected value: 100 * 0.15 = 15 successes
double expected = conversionDist.getExpectedValue(100);
```

**Why It Matters:** Real user behavior has natural variance. A 15% conversion rate doesn't mean exactly 15 out of 100 users convert every time. The binomial distribution captures this natural fluctuation.

---

### 2. Engagement Distribution (Pareto/Power Law Distribution)

**Purpose:** Implement a Power Law (Pareto) distribution to ensure a small percentage of "Power Users" perform the bulk of interactions, while the majority remain in a "Long-Tail" low-activity state.

**Mathematical Foundation:**
```
f(x) = (α * x_m^α) / (x^(α+1)) for x ≥ x_m
F(x) = 1 - (x_m / x)^α for x ≥ x_m
```

Where:
- α (alpha) = shape parameter (Pareto index)
- x_m = scale parameter (minimum value)

**Key Features:**
- 80/20 rule implementation (Pareto Principle)
- Power user identification
- Engagement score generation
- Gini coefficient calculation for inequality measurement
- Segmented population generation
- Customizable shape and scale parameters

**Usage Example:**
```java
// Initialize with shape=1.8 (power users) and scale=5.0 (minimum engagement)
ParetoDistribution engagementDist = new ParetoDistribution(1.8, 5.0);

// Generate engagement scores for 1000 users
double[] engagementScores = engagementDist.samplePopulation(1000);

// Generate discrete action counts
int[] actionCounts = engagementDist.sampleActionCounts(1000, 100);

// Identify power users (top 20% by engagement)
double powerUserThreshold = 50.0;
double powerUserProportion = engagementDist.getProportionAboveThreshold(powerUserThreshold);
// Result: ~0.20 (20% of users are power users)

// Calculate Gini coefficient (0 = perfect equality, 1 = maximum inequality)
double gini = engagementDist.getGiniCoefficient();
// Result: ~0.5-0.7 for realistic user engagement
```

**Why It Matters:** Most apps follow the 80/20 rule - 20% of users generate 80% of value. Uniform distributions fail to capture this critical pattern, leading to unrealistic load testing.

---

### 3. Action Frequency Distribution (Poisson Process)

**Purpose:** Implement a Poisson process to model the timing of discrete events (like message sends or pings), reflecting how events naturally cluster in time.

**Mathematical Foundation:**
```
P(X = k) = (λ^k * e^(-λ)) / k!
Inter-arrival time: T ~ Exponential(λ)
```

Where:
- k = number of events
- λ = expected number of events in the interval
- e = Euler's number (~2.71828)

**Key Features:**
- Event count simulation per time interval
- Inter-arrival time generation (exponential distribution)
- Absolute timestamp generation for events
- Clustered event simulation (message bursts)
- Non-homogeneous processes with time-varying rates
- Multi-user simultaneous simulation

**Usage Example:**
```java
// Initialize with λ = 2.5 events per minute
PoissonProcess actionProcess = new PoissonProcess(2.5);

// Simulate event counts for 60 one-minute intervals
int[] eventCounts = actionProcess.sampleIntervals(60);

// Generate inter-arrival times
double[] interArrivalTimes = actionProcess.generateInterArrivalTimes(100);

// Generate absolute timestamps within a 24-hour window
double timeWindow = 86400000; // 24 hours in milliseconds
double[] timestamps = actionProcess.generateEventTimestamps(timeWindow);

// Generate clustered events (message bursts)
double[] clusteredTimestamps = actionProcess.generateClusteredEventTimestamps(timeWindow, 0.3);
```

**Why It Matters:** User actions don't follow uniform timing. Real users have bursts of activity (message storms, rapid scrolling) followed by periods of inactivity. The Poisson process captures this natural clustering.

---

### 4. Long-Tail Behavior Modeling (Outlier Session Generation)

**Purpose:** Generate rare, high-intensity "Outlier Sessions" to test how the app handles extreme resource consumption at the edge of the bell curve.

**Mathematical Foundation:**
- Heavy-tailed distributions (Pareto) for rare events
- Mixture models for multi-modal behavior
- Extreme value theory for outlier detection
- Interquartile range (IQR) method for detection

**Key Features:**
- Outlier session generation with extreme resource usage
- Multiple outlier types: CPU-heavy, memory-heavy, network-heavy, duration-heavy
- Stress test scenario generation
- Statistical outlier detection (IQR method)
- Customizable outlier probability and intensity
- Resource usage simulation for Samsung Galaxy A12 constraints

**Usage Example:**
```java
// Initialize with 5% outlier probability and 3x intensity multiplier
OutlierSessionGenerator outlierGen = new OutlierSessionGenerator(0.05, 3.0);

// Generate a single session
OutlierSessionGenerator.Session session = outlierGen.generateSession();
System.out.println(session);
// Output: Session[outlier=true, cpu=95.50%, mem=850.25MB, requests=450, duration=1200000ms, actions=150, intensity=200.50]

// Generate 100 sessions with exact 10% outlier ratio
OutlierSessionGenerator.Session[] sessions = outlierGen.generateSessionsWithRatio(100, 0.10);

// Generate mixed outlier population
OutlierSessionGenerator.Session[] mixedSessions = outlierGen.generateMixedOutlierPopulation(100);

// Run stress test scenarios
OutlierSessionGenerator.Session[] cpuSpike = outlierGen.generateStressTestScenario("cpu_spike");
OutlierSessionGenerator.Session[] memoryLeak = outlierGen.generateStressTestScenario("memory_leak");
OutlierSessionGenerator.Session[] networkStorm = outlierGen.generateStressTestScenario("network_storm");
```

**Why It Matters:** Real apps encounter extreme edge cases: memory leaks, CPU spikes, network storms, marathon sessions. Uniform testing never encounters these, but they happen in production and can cause crashes or degraded performance.

---

### 5. Non-Uniform Sampling (Gaussian/Normal Distribution)

**Purpose:** Provide stochastic sampling from a Gaussian (Normal) distribution rather than a flat random range, ensuring the aggregate behavior matches real-world human data.

**Mathematical Foundation:**
```
f(x) = (1 / (σ * √(2π))) * e^(-((x-μ)²) / (2σ²))
```

Where:
- μ (mu) = mean (center of distribution)
- σ (sigma) = standard deviation (spread)

**Key Features:**
- Normal distribution sampling using Box-Muller transform
- Constrained sampling within bounds
- Integer sample generation
- Mixed population sampling (multiple demographic segments)
- Correlated sample generation
- Temporal autocorrelation (momentum in behavior)
- CDF, inverse CDF, and probability calculations

**Usage Example:**
```java
// Initialize with mean=100, stdDev=30
NormalDistribution paramDist = new NormalDistribution(100.0, 30.0);

// Generate samples
double[] samples = paramDist.sample(100);

// Generate constrained samples (min=50, max=200)
double[] constrained = paramDist.sampleConstrained(100, 50.0, 200.0);

// Generate integer samples (min=1, max=100)
int[] integers = paramDist.sampleIntegers(100, 1, 100);

// Mixed population: 60% normal users, 40% power users
double[][] segments = {
    {80.0, 20.0, 0.60},  // mean=80, stdDev=20, 60% of population
    {150.0, 40.0, 0.40}  // mean=150, stdDev=40, 40% of population
};
double[] mixed = paramDist.sampleMixedPopulation(1000, segments);

// Calculate probability of value being within 1 standard deviation
double prob = paramDist.probabilityWithinStandardDeviations(1.0);
// Result: ~0.6827 (68.27%)
```

**Why It Matters:** Human behavior follows normal distributions in many aspects: response times, session durations, action counts. Random uniform sampling creates artificial behavior that doesn't match real-world data.

---

## Integration: DistributionalFidelityModel

The main integration class orchestrates all five statistical realism hooks:

```java
public class DistributionalFidelityModel {
    private final BinomialDistribution conversionDistribution;
    private final ParetoDistribution engagementDistribution;
    private final PoissonProcess actionFrequencyProcess;
    private final OutlierSessionGenerator outlierGenerator;
    private final NormalDistribution parameterSamplingDistribution;
    
    public SimulationResults simulatePopulation(int populationSize);
    public Map<String, SimulationResults> simulateScenarios(String[] scenarios);
    public String generateStatisticalReport(SimulationResults results);
    public void exportToCSV(SimulationResults results, String filename);
}
```

**Usage Example:**
```java
// Initialize with default parameters
DistributionalFidelityModel model = new DistributionalFidelityModel();

// Simulate 1000 users
SimulationResults results = model.simulatePopulation(1000);

// Generate detailed report
System.out.println(results.generateSummaryReport());
System.out.println(model.generateStatisticalReport(results));

// Export to CSV
model.exportToCSV(results, "simulation_results.csv");

// Run multiple scenarios
String[] scenarios = {"normal", "high_conversion", "stress_test"};
Map<String, SimulationResults> scenarioResults = model.simulateScenarios(scenarios);
```

---

## Calibration for Samsung Galaxy A12

### Hardware Constraints
- **CPU:** 8 cores, max 2.3 GHz
- **RAM:** 4 GB
- **GPU:** PowerVR GE8320
- **Storage:** 32/64/128 GB

### Recommended Default Parameters
```java
new DistributionalFidelityModel(
    conversionRate = 0.15,          // 15% base conversion rate
    paretoShape = 1.8,              // Strong power user behavior
    paretoScale = 5.0,              // Minimum 5 actions per user
    poissonLambda = 2.5,            // 2.5 events per minute
    outlierProbability = 0.05,      // 5% outlier sessions
    normalMean = 100.0,             // Mean engagement score
    normalStdDev = 30.0             // Standard deviation
);
```

### Performance Considerations
- **Concurrent Users:** Test with 100-1000 concurrent users
- **Session Duration:** 5-60 minutes (with outliers up to 2 hours)
- **CPU Load:** Target 30-70% average (outliers: 90-100%)
- **Memory Usage:** Target 100-300 MB per session (outliers: 500-1000 MB)
- **Network Requests:** Target 10-50 per session (outliers: 100-500)

---

## Running the Simulation

### Compile and Run
```bash
# Compile all Java files
javac -d bin src/main/java/com/simulation/distributional/*.java

# Run the main simulation
java -cp bin com.simulation.distributional.DistributionalFidelityModel
```

### Expected Output
```
Distributional Fidelity Model for Samsung Galaxy A12 Simulation
===============================================================

Running simulation with 1000 users...
=== Distributional Fidelity Simulation Report ===
Total Users: 1000
Simulation Duration: 86400000 ms (24.00 hours)
Total Conversions: 152
Actual Conversion Rate: 15.20%
Power Users: 205 (20.50%)
Outlier Sessions: 52 (5.20%)
Average Engagement Score: 24.85
Average Action Frequency: 3.12
Total CPU Usage: 45.32% (average per user)
Total Memory Usage: 287.45 MB (average per user)
Total Network Requests: 47 (average per user)

=== Statistical Analysis ===
Engagement Score - Mean: 24.85, StdDev: 42.13
Action Frequency - Mean: 3.12, StdDev: 5.87
Conversion Rate Variance: 128.8000
Pareto Gini Coefficient: 0.5556

Results exported to: simulation_results_20240307_143022.csv

Simulation complete!
```

---

## Comparison: Uniform vs. Distributional Fidelity

| Metric | Uniform Distribution | Distributional Fidelity | Real-World Data |
|--------|---------------------|-------------------------|-----------------|
| Conversion Rate | Exactly 15.00% | 14.8-15.2% (variance) | 12-18% (variance) |
| Power Users | 0% (none) | 20% (realistic) | 15-25% |
| Outlier Sessions | 0% (none) | 5% (realistic) | 3-10% |
| Action Clustering | None | Natural bursts | Message storms |
| Resource Peaks | Flat | Heavy tails | Real spikes |
| Load Pattern | Artificial | Probabilistic | Natural |

---

## Advantages Over Uniform Testing

1. **Realistic Load Patterns:** Matches real user behavior distributions
2. **Edge Case Discovery:** Outlier sessions reveal hidden bugs
3. **Capacity Planning:** Heavy tails inform infrastructure sizing
4. **Stress Testing:** Extreme scenarios test system resilience
5. **Data-Driven:** Decisions based on statistical analysis
6. **Reproducible:** Random seeds enable consistent testing
7. **Flexible:** Easy to adjust parameters for different scenarios

---

## Extending the Framework

### Adding New Distributions
```java
// Example: Log-Normal distribution for session durations
public class LogNormalDistribution {
    private final NormalDistribution normalDist;
    
    public double sample() {
        double z = normalDist.sample();
        return Math.exp(mean + standardDeviation * z);
    }
}
```

### Custom Outlier Scenarios
```java
public Session[] generateCustomOutlierScenario() {
    // Define custom outlier behavior
    // Mix and match resource usage patterns
}
```

### Device-Specific Calibration
```java
// Calibrate for different devices
public class DeviceProfile {
    String deviceName;
    int cpuCores;
    long ramMB;
    double maxCpuLoad;
    double maxMemoryUsage;
}
```

---

## References and Further Reading

1. **Pareto Principle:** 80/20 rule in user behavior
2. **Poisson Process:** Event timing and clustering
3. **Extreme Value Theory:** Modeling rare events
4. **Heavy-Tailed Distributions:** Power laws in nature
5. **Statistical Quality Control:** Process variation

---

## License and Attribution

This framework is designed for educational and testing purposes. Proper statistical modeling should be used to inform infrastructure decisions.

---

**Remember:** "Real populations follow probabilistic distributions, not uniform patterns."
