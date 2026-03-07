# User Evolution Model - Longitudinal Performance Testing

A Java-based simulation framework for conducting 90-day soak tests that audit app retention and UI adaptation on mobile devices, specifically designed for Samsung Galaxy A12 (SM-A125U).

## Overview

This project implements "Longitudinal Realism Hooks" to simulate how user interaction matures over time, providing high-fidelity data for long-term retention audits. The model reflects the reality that real users change their behavior, skills, and engagement patterns throughout their lifecycle.

## Core Components

### 1. Skill Improvement (Power User Curve)

**Location:** `com.evolution.skill.SkillImprovementCurve`

Models how users become more proficient over time:
- **Navigation Latency**: Decreases from 2500ms (new user) to 400ms (power user)
- **Error Rate**: Drops from 15% to 0.5% as users learn the UI
- **Learning Rate**: Varies by lifecycle phase (fastest during onboarding, slower for power users)

```java
SkillImprovementCurve skillCurve = new SkillImprovementCurve(90);
skillCurve.updateSkillForDay(day, lifecycleState, sessionsToday);
double proficiencyScore = skillCurve.getSkillProficiencyScore();
```

### 2. Interest Drift & Habit Formation

**Locations:** 
- `com.evolution.interest.InterestDriftModel`
- `com.evolution.interest.MarkovTransitionMatrix`

Uses a Markov Chain model to simulate behavioral evolution:
- **Habit Strength**: Increases with repeated module visits
- **Boredom Scores**: Increase with overuse of specific features
- **Probabilistic Transitions**: Module visitation probabilities shift over time
- **Novelty Seeking**: Users explore new features when boredom is high

```java
InterestDriftModel interestModel = new InterestDriftModel();
interestModel.updateInterestForDay(day, moduleVisits);
AppModule nextModule = interestModel.selectNextModule();

MarkovTransitionMatrix markovMatrix = new MarkovTransitionMatrix();
markovMatrix.adaptToHabits(habitStrengths, boredomScores);
AppModule nextState = markovMatrix.getNextState(currentModule);
```

### 3. Realistic Drop-off Patterns

**Location:** `com.evolution.churn.ChurnModel`

Implements dynamic churn probability based on multiple factors:
- **Latency Impact**: High navigation latency increases churn risk
- **Error Rate Impact**: Excessive errors trigger churn triggers
- **Low Engagement**: Consecutive days of low engagement increase churn probability
- **Lifecycle Milestones**: Critical churn points at day 7, 30, 60
- **Retention Factors**: Positive experiences reduce churn probability

```java
ChurnModel churnModel = new ChurnModel();
churnModel.updateChurnProbability(day, state, latencyMs, errorRate, sessions, duration);
boolean hasChurned = churnModel.evaluateChurn();
```

### 4. Lifecycle Transition Logic

**Location:** `com.evolution.lifecycle.LifecycleManager`

Orchestrates transitions between user lifecycle phases:

| Phase | Days | Behavior Characteristics |
|-------|------|-------------------------|
| **ONBOARDING** | 0-7 | Learning basics, high exploration, 2-4 sessions/day |
| **EXPLORER** | 7-30 | Discovering features, moderate habits, 3-5 sessions/day |
| **UTILITY** | 30-60 | Established routines, strong habits, 4-6 sessions/day |
| **POWER_USER** | 60-90 | Mastered app, efficient interactions, 5-7 sessions/day |

```java
LifecycleManager manager = new LifecycleManager(90);
while (manager.isSimulationActive()) {
    manager.advanceDay();
    SimulationSession session = manager.generateSession();
}
```

## Running the Simulation

### Command Line

```bash
# Run default 90-day simulation
java -cp target/classes com.evolution.SoakTestOrchestrator

# Specify custom duration and device ID
java -cp target/classes com.evolution.SoakTestOrchestrator 60 SM-A125U-TEST-01
```

### Programmatic Usage

```java
SoakTestOrchestrator orchestrator = new SoakTestOrchestrator(90, "SM-A125U");
orchestrator.runSimulation();
```

## Output Files

### Daily Metrics (`metrics_day_XXX.csv`)
Generated every 10 days:
- `day`: Current simulation day
- `lifecycle_state`: Current lifecycle phase
- `skill_proficiency`: User skill score (0.0-1.0)
- `latency_ms`: Average navigation latency
- `error_rate`: Current error rate
- `churn_probability`: Current churn risk
- `sessions_today`: Number of sessions on this day

### Final Report (`soak_test_report_DEVICE_YYYYMMDD_HHMMSS.csv`)
Complete session-by-session data:
- `session_id`: Sequential session identifier
- `day`: Simulation day
- `lifecycle_state`: Lifecycle phase during session
- `duration_seconds`: Session length
- `total_interactions`: Number of module visits
- `latency_ms`: Navigation latency during session
- `error_rate`: Error rate during session

## Architecture

```
com.evolution
├── model/
│   ├── AppModule.java           # App feature modules
│   ├── InteractionMetric.java   # Performance metrics
│   └── LifecycleState.java      # User lifecycle phases
├── skill/
│   └── SkillImprovementCurve.java  # Power user simulation
├── interest/
│   ├── InterestDriftModel.java  # Habit & boredom dynamics
│   └── MarkovTransitionMatrix.java # Module transition patterns
├── churn/
│   └── ChurnModel.java          # Churn probability engine
├── lifecycle/
│   ├── LifecycleManager.java    # Main orchestrator
│   └── SimulationSession.java   # Session data container
└── SoakTestOrchestrator.java    # Test runner
```

## Key Algorithms

### Learning Rate Calculation

```java
private double calculateLearningRate(LifecycleState state, int day) {
    return switch (state) {
        case ONBOARDING -> 0.25;  // Fast learning
        case EXPLORER -> 0.15;
        case UTILITY -> 0.08;
        case POWER_USER -> 0.03; // Diminishing returns
    };
}
```

### Experience Factor

Uses exponential decay to simulate cumulative learning:
```java
experienceFactor = 1.0 - exp(-3.0 * (day / totalDays))
```

### Churn Probability

Combines base probability with triggers and retention factors:
```
finalProb = baseProb + Σ(churnTriggers) + Σ(retentionFactors)
```

## Configuration

Modify constants in respective classes to adjust simulation parameters:

### Skill Improvement
- `INITIAL_NAVIGATION_LATENCY_MS`: Starting latency (default: 2500ms)
- `MIN_NAVIGATION_LATENCY_MS`: Achievable minimum (default: 400ms)
- `INITIAL_ERROR_RATE`: Starting error rate (default: 0.15)
- `MIN_ERROR_RATE`: Achievable minimum (default: 0.005)

### Churn Model
- `BASE_CHURN_PROBABILITY`: Daily churn risk (default: 0.001)
- `HIGH_LATENCY_THRESHOLD_MS`: Latency trigger point (default: 5000ms)
- `ERROR_RATE_THRESHOLD`: Error rate trigger (default: 0.10)
- `MAX_CONSECUTIVE_LOW_ENGAGEMENT`: Days before churn (default: 5)

## Extending the Model

### Adding New App Modules

Edit `AppModule.java`:
```java
NEW_FEATURE("new_feature", 0.05, 0.7)
```

### Custom Lifecycle Phases

Add to `LifecycleState.java` enum and update transition logic in `LifecycleManager.java`.

### Custom Churn Triggers

Extend `ChurnModel.java` with new trigger types in `applyXXXImpact` methods.

## Testing Scenarios

### Scenario 1: Normal Evolution
```java
LifecycleManager manager = new LifecycleManager(90);
manager.runSimulation(); // Should see gradual skill improvement
```

### Scenario 2: Latency Stress Test
```java
manager.simulateLatencyIssue(3.0, 5); // 3x latency for 5 days
// Monitor churn probability spike
```

### Scenario 3: Retention Analysis
Compare churn patterns across multiple runs to identify retention bottlenecks.

## Device-Specific Considerations

### Samsung Galaxy A12 (SM-A125U)
- **Display**: 720x1600 pixels, 6.5" PLS TFT
- **Performance**: Consider adjusting latency baselines for device class
- **Battery**: May affect session duration modeling
- **Storage**: Could influence feature adoption rates

Adapt `INITIAL_NAVIGATION_LATENCY_MS` and session duration calculations based on device-specific performance characteristics.

## Logging

The simulation uses SLF4J for comprehensive logging:

```xml
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>2.0.9</version>
</dependency>
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.4.11</version>
</dependency>
```

Configure logging levels in `src/main/resources/logback.xml` for detailed output.

## Performance Considerations

- Memory usage scales with simulation duration (~1MB per 10 days)
- Markov matrix complexity is O(n²) where n = number of modules
- Churn probability calculation is O(1) per day

## Future Enhancements

- Multi-user simulation for cohort analysis
- A/B testing framework for UI variant comparison
- Network condition simulation
- Battery drain modeling
- Social influence and referral dynamics

## License

Internal use - Samsung Device Testing Framework
