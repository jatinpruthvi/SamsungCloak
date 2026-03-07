# User Evolution Model - Usage Examples

This guide provides practical examples for using the User Evolution Model to conduct longitudinal performance testing on the Samsung Galaxy A12 (SM-A125U).

## Quick Start

### Basic 90-Day Simulation

```java
import com.evolution.SoakTestOrchestrator;

public class BasicExample {
    public static void main(String[] args) {
        // Run default 90-day simulation for SM-A125U
        SoakTestOrchestrator orchestrator = 
            new SoakTestOrchestrator(90, "SM-A125U");
        orchestrator.runSimulation();
    }
}
```

### Running the Demo

```bash
# Compile (with Maven)
mvn clean package

# Run comprehensive demo
java -cp target/classes:target/user-evolution-model.jar \
    com.evolution.demo.UserEvolutionDemo

# Run full simulation
java -cp target/classes:target/user-evolution-model.jar \
    com.evolution.SoakTestOrchestrator 90 SM-A125U
```

## Advanced Usage

### Custom Simulation with Latency Injection

```java
import com.evolution.lifecycle.LifecycleManager;
import com.evolution.lifecycle.SimulationSession;

public class LatencyStressTest {
    public static void main(String[] args) {
        LifecycleManager manager = new LifecycleManager(90);
        
        while (manager.isSimulationActive()) {
            manager.advanceDay();
            
            // Inject latency spike on day 30
            if (manager.getCurrentDay() == 30) {
                manager.simulateLatencyIssue(4.0, 3);
                System.out.println("Day 30: 4x latency spike for 3 days");
            }
            
            // Generate sessions
            for (int i = 0; i < 5; i++) {
                SimulationSession session = manager.generateSession();
                if (session != null) {
                    System.out.printf("Session: %s, latency=%.2fms%n",
                        session, session.getMetrics().getNavigationLatencyMs());
                }
            }
            
            // Monitor churn probability after spike
            if (manager.getCurrentDay() >= 30) {
                System.out.printf("Churn risk: %.4f%n",
                    manager.getChurnModel().getChurnProbability());
            }
        }
    }
}
```

### Analyzing Habit Formation

```java
import com.evolution.lifecycle.LifecycleManager;
import com.evolution.model.AppModule;
import java.util.Map;

public class HabitAnalysis {
    public static void main(String[] args) {
        LifecycleManager manager = new LifecycleManager(60);
        
        while (manager.getCurrentDay() <= 60) {
            manager.advanceDay();
            
            // Generate sessions to build habits
            for (int i = 0; i < 4; i++) {
                manager.generateSession();
            }
            
            // Analyze habits at key milestones
            if (manager.getCurrentDay() == 7 || 
                manager.getCurrentDay() == 30 || 
                manager.getCurrentDay() == 60) {
                
                System.out.printf("\n=== Day %d Habit Analysis ===%n", 
                    manager.getCurrentDay());
                
                Map<AppModule, Double> habits = 
                    manager.getInterestModel().getHabitStrengths();
                
                habits.entrySet().stream()
                    .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                    .limit(5)
                    .forEach(e -> System.out.printf(
                        "%s: habit strength=%.2f, prob=%.2f%%%n",
                        e.getKey().getModuleId(),
                        e.getValue(),
                        manager.getInterestModel().getVisitProbabilities().get(e.getKey()) * 100));
            }
        }
    }
}
```

### Skill Progression Tracking

```java
import com.evolution.lifecycle.LifecycleManager;
import com.evolution.model.LifecycleState;

public class SkillTracking {
    public static void main(String[] args) {
        LifecycleManager manager = new LifecycleManager(90);
        
        System.out.println("Day | State       | Latency (ms) | Error Rate | Skill %");
        System.out.println("----|-------------|--------------|------------|--------");
        
        while (manager.isSimulationActive()) {
            manager.advanceDay();
            
            // Generate sessions
            for (int i = 0; i < 4; i++) {
                manager.generateSession();
            }
            
            // Track skill progression every 10 days
            if (manager.getCurrentDay() % 10 == 0) {
                var metrics = manager.getSkillCurve().getCurrentMetrics();
                double skill = manager.getSkillCurve().getSkillProficiencyScore();
                
                System.out.printf("%3d | %-11s | %12.2f | %10.4f | %6.1f%%%n",
                    manager.getCurrentDay(),
                    manager.getCurrentState().name(),
                    metrics.getNavigationLatencyMs(),
                    metrics.getErrorRate(),
                    skill * 100);
            }
        }
    }
}
```

### Churn Risk Monitoring

```java
import com.evolution.churn.ChurnModel;
import com.evolution.lifecycle.LifecycleManager;
import java.util.List;

public class ChurnMonitoring {
    public static void main(String[] args) {
        LifecycleManager manager = new LifecycleManager(90);
        
        while (manager.isSimulationActive()) {
            manager.advanceDay();
            
            // Generate sessions
            for (int i = 0; i < 4; i++) {
                manager.generateSession();
            }
            
            ChurnModel churnModel = manager.getChurnModel();
            
            // Alert on high churn risk
            if (churnModel.getChurnProbability() > 0.10) {
                System.out.printf("\n⚠ HIGH CHURN RISK on day %d: %.2f%%%n",
                    manager.getCurrentDay(),
                    churnModel.getChurnProbability() * 100);
                
                List<ChurnModel.ChurnTrigger> triggers = 
                    churnModel.getActiveTriggers();
                
                System.out.println("Active triggers:");
                triggers.forEach(t -> System.out.printf(
                    "  - %s: impact=%.4f (%s)%n",
                    t.getType(), t.getImpact(), t.getDescription()));
            }
            
            // Log churn if it occurs
            if (churnModel.hasChurned()) {
                System.out.printf("\n❌ USER CHURNED on day %d%n",
                    manager.getCurrentDay());
                break;
            }
        }
    }
}
```

### Comparing Multiple Cohorts

```java
import com.evolution.SoakTestOrchestrator;
import java.util.ArrayList;
import java.util.List;

public class CohortComparison {
    public static void main(String[] args) {
        List<String> devices = List.of(
            "SM-A125U-001", "SM-A125U-002", "SM-A125U-003"
        );
        
        List<Integer> churnDays = new ArrayList<>();
        
        for (String device : devices) {
            System.out.printf("\nTesting device: %s%n", device);
            
            LifecycleManager manager = new LifecycleManager(90);
            manager.setDeviceId(device);
            
            while (manager.isSimulationActive()) {
                manager.advanceDay();
                
                for (int i = 0; i < 4; i++) {
                    manager.generateSession();
                }
                
                if (manager.getChurnModel().hasChurned()) {
                    churnDays.add(manager.getCurrentDay());
                    System.out.printf("Churned on day %d%n", 
                        manager.getCurrentDay());
                    break;
                }
            }
            
            if (!manager.getChurnModel().hasChurned()) {
                System.out.println("Completed 90-day test without churn");
            }
        }
        
        // Summary
        System.out.println("\n=== COHORT SUMMARY ===");
        double avgChurnDay = churnDays.stream()
            .mapToInt(Integer::intValue)
            .average()
            .orElse(90);
        
        System.out.printf("Devices churned: %d/%d%n",
            churnDays.size(), devices.size());
        System.out.printf("Average churn day: %.1f%n", avgChurnDay);
        System.out.printf("90-day retention rate: %.1f%%%n",
            (1.0 - (double)churnDays.size() / devices.size()) * 100);
    }
}
```

### Feature Adoption Analysis

```java
import com.evolution.lifecycle.LifecycleManager;
import com.evolution.model.AppModule;
import java.util.HashMap;
import java.util.Map;

public class FeatureAdoption {
    public static void main(String[] args) {
        LifecycleManager manager = new LifecycleManager(60);
        
        Map<AppModule, Integer> firstSeenDay = new HashMap<>();
        Map<AppModule, Integer> totalVisits = new HashMap<>();
        
        while (manager.getCurrentDay() <= 60) {
            manager.advanceDay();
            
            for (int i = 0; i < 4; i++) {
                var session = manager.generateSession();
                if (session != null) {
                    for (AppModule module : session.getVisitedModules()) {
                        totalVisits.merge(module, 1, Integer::sum);
                        
                        // Track first discovery
                        firstSeenDay.putIfAbsent(module, manager.getCurrentDay());
                    }
                }
            }
        }
        
        System.out.println("\n=== FEATURE ADOPTION ANALYSIS ===");
        System.out.println("Module              | First Seen | Total Visits | Adoption Rate");
        System.out.println("--------------------|------------|---------------|---------------");
        
        for (AppModule module : AppModule.values()) {
            int firstDay = firstSeenDay.getOrDefault(module, 0);
            int visits = totalVisits.getOrDefault(module, 0);
            double rate = firstDay > 0 ? 100.0 : 0.0;
            
            System.out.printf("%-19s | %10d | %13d | %11.1f%%%n",
                module.getModuleId(), firstDay, visits, rate);
        }
    }
}
```

## Output Data Analysis

### Parsing Daily Metrics CSV

```python
import pandas as pd
import matplotlib.pyplot as plt

# Load daily metrics
df = pd.read_csv('metrics_day_030.csv')

# Plot skill progression
plt.figure(figsize=(12, 4))

plt.subplot(1, 3, 1)
plt.plot(df['day'], df['skill_proficiency'] * 100)
plt.title('Skill Proficiency Over Time')
plt.xlabel('Day')
plt.ylabel('Proficiency (%)')

plt.subplot(1, 3, 2)
plt.plot(df['day'], df['latency_ms'])
plt.title('Navigation Latency')
plt.xlabel('Day')
plt.ylabel('Latency (ms)')

plt.subplot(1, 3, 3)
plt.plot(df['day'], df['error_rate'] * 100)
plt.title('Error Rate')
plt.xlabel('Day')
plt.ylabel('Error Rate (%)')

plt.tight_layout()
plt.savefig('skill_progression.png')
```

### Analyzing Session Patterns

```python
import pandas as pd

# Load full report
df = pd.read_csv('soak_test_report_SM-A125U_20240307_120000.csv')

# Session duration by lifecycle state
duration_by_state = df.groupby('lifecycle_state')['duration_seconds'].agg([
    ('mean', 'mean'),
    ('std', 'std'),
    ('count', 'count')
])

print("Session Duration by Lifecycle State:")
print(duration_by_state)

# Interaction efficiency (interactions per second)
df['efficiency'] = df['total_interactions'] / df['duration_seconds']
efficiency_by_state = df.groupby('lifecycle_state')['efficiency'].mean()

print("\nInteraction Efficiency by State:")
print(efficiency_by_state)
```

### Churn Risk Heatmap

```python
import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt

# Create churn risk matrix
churn_data = []
for day in range(1, 91):
    # You would need to track this during simulation
    churn_data.append({
        'day': day,
        'state': 'ONBOARDING' if day < 7 else 
                 'EXPLORER' if day < 30 else 
                 'UTILITY' if day < 60 else 'POWER_USER',
        'churn_prob': 0.01  # Placeholder - use actual data
    })

df = pd.DataFrame(churn_data)

# Create pivot table
pivot = df.pivot_table(index='state', columns='day', values='churn_prob')

plt.figure(figsize=(16, 4))
sns.heatmap(pivot, cmap='YlOrRd', cbar_kws={'label': 'Churn Probability'})
plt.title('Churn Risk Heatmap by Lifecycle State')
plt.tight_layout()
plt.savefig('churn_heatmap.png')
```

## Integration with Device Testing

### Samsung Galaxy A12 Specific Configuration

```java
import com.evolution.lifecycle.LifecycleManager;
import com.evolution.skill.SkillImprovementCurve;

public class DeviceSpecificConfig {
    public static void main(String[] args) {
        // Adjust for SM-A125U device characteristics
        LifecycleManager manager = new LifecycleManager(90);
        
        // Get device-specific skill curve
        SkillImprovementCurve skillCurve = manager.getSkillCurve();
        
        // SM-A125U has lower performance, adjust baselines
        // Note: These would be configurable constants in production
        System.out.println("Configuring for SM-A125U:");
        System.out.println("  - Display: 720x1600, 6.5\" PLS TFT");
        System.out.println("  - Baseline latency adjusted for device class");
        System.out.println("  - Battery considerations in session duration");
        
        // Run simulation with device-specific logging
        while (manager.isSimulationActive()) {
            manager.advanceDay();
            
            for (int i = 0; i < 4; i++) {
                manager.generateSession();
            }
            
            // Device-specific monitoring
            if (manager.getCurrentDay() % 15 == 0) {
                System.out.printf("Day %d - Latency: %.0fms (SM-A125U optimized)%n",
                    manager.getCurrentDay(),
                    manager.getSkillCurve().getCurrentMetrics().getNavigationLatencyMs());
            }
        }
    }
}
```

## Best Practices

1. **Start with Shorter Simulations**: Test with 7-14 day simulations before running full 90-day tests
2. **Monitor Logs**: Check `logs/soak-test.log` for detailed progression
3. **Analyze CSV Outputs**: Use the generated CSV files for in-depth analysis
4. **Compare Cohorts**: Run multiple simulations to identify patterns
5. **Calibrate Constants**: Adjust constants in model classes based on real user data
6. **Track Anomalies**: Look for unusual spikes in churn probability or latency

## Troubleshooting

### High Early Churn
- Check if latency baselines are too high for your app
- Verify onboarding phase transition triggers
- Review skill improvement curve parameters

### Unrealistic Skill Progression
- Adjust learning rates in `SkillImprovementCurve`
- Modify session generation logic in `LifecycleManager`
- Check habit formation rates in `InterestDriftModel`

### No Churn Events
- Increase latency/error thresholds in `ChurnModel`
- Add more frequent latency spikes
- Verify churn probability calculation logic
