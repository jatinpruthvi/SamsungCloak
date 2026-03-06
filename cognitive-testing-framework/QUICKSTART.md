# Cognitive Realism Framework - Quick Start Guide

Get up and running with the Cognitive Realism Testing Framework in 5 minutes.

## Prerequisites

- Java 8+ installed
- Appium Server running
- Samsung Galaxy A12 (SM-A125U) connected via ADB
- Android Studio or IDE for Java development

## Installation

### Step 1: Add to Your Project

```gradle
// In your app's build.gradle
dependencies {
    implementation 'io.appium:java-client:8.3.0'
    implementation 'org.seleniumhq.selenium:selenium-java:4.8.0'
    
    // Copy the cognitive-testing-framework JAR or source files to your project
}
```

### Step 2: Copy Framework Files

Place the framework in your project:
```
your-project/
├── app/
│   └── src/
│       └── main/
│           └── java/
│               └── com/
│                   └── cognitive/
│                       └── testing/
│                           ├── (all framework files)
```

## Your First Cognitive Test

### Basic Example (5 minutes)

```java
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import org.openqa.selenium.remote.DesiredCapabilities;
import com.cognitive.testing.automation.CognitiveTestFramework;
import com.cognitive.testing.automation.AppiumCognitiveDriver;
import com.cognitive.testing.model.CognitiveConfig;
import java.net.URL;

public class MyFirstCognitiveTest {
    
    public static void main(String[] args) throws Exception {
        // 1. Setup Appium with A12 capabilities
        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability("deviceName", "Samsung Galaxy A12");
        capabilities.setCapability("platformName", "Android");
        capabilities.setCapability("automationName", "UiAutomator2");
        capabilities.setCapability("appPackage", "com.example.app");
        capabilities.setCapability("appActivity", ".MainActivity");
        
        URL url = new URL("http://localhost:4723/wd/hub");
        AndroidDriver<AndroidElement> driver = new AndroidDriver<>(url, capabilities);
        
        // 2. Create cognitive framework
        CognitiveTestFramework framework = new CognitiveTestFramework(
            CognitiveConfig.highFidelity()
        );
        
        // 3. Wrap driver with cognitive enhancements
        AppiumCognitiveDriver cognitiveDriver = new AppiumCognitiveDriver(
            driver, framework
        );
        
        try {
            // 4. Start cognitive session
            framework.startSession();
            
            // 5. Perform actions with human-like behavior
            cognitiveDriver.click(driver.findElementById("com.example.app:id/button"));
            cognitiveDriver.swipeUp();
            cognitiveDriver.typeText(
                driver.findElementById("com.example.app:id/input"),
                "Hello World"
            );
            
            // 6. Navigate with cognitive tracking
            framework.navigateTo("HomeScreen");
            framework.navigateTo("ProfileScreen");
            
            // 7. End session and see report
            framework.endSession();
            System.out.println(framework.getSessionReport());
            
        } finally {
            driver.quit();
        }
    }
}
```

## Common Patterns

### Pattern 1: Testing Flow with Context Switching

```java
framework.startSession();

for (int i = 0; i < 20; i++) {
    framework.performAction(() -> {
        // Your test action
        clickButton();
    });
    
    // Check if user got distracted
    if (framework.shouldContextSwitch()) {
        System.out.println("User got distracted!");
        long delay = framework.simulateAbandonment();
        System.out.println("Abandoned for " + delay + "ms");
    }
}

framework.endSession();
```

### Pattern 2: Testing Option Selection

```java
framework.startSession();

List<AndroidElement> options = driver.findElements(By.id("option"));

// Select using satisficing (first acceptable, not optimal)
AndroidElement selected = framework.selectOption(options, element -> {
    if (!element.isDisplayed()) return 0.0f;
    return Math.min(1.0f, element.getSize().height / 100.0f);
}, 0.5f);

selected.click();

framework.endSession();
```

### Pattern 3: Testing Long Session Fatigue

```java
framework.startSession();

// Simulate many decisions to build fatigue
for (int i = 0; i < 100; i++) {
    // Check for errors (increases with fatigue)
    if (framework.getDecisionFatigueHook().shouldCommitError()) {
        System.out.println("Error #" + i + ": " + 
            framework.getDecisionFatigueHook().getNextErrorType());
    }
    
    // Calculate think time (slows with fatigue)
    int thinkTime = framework.getDecisionFatigueHook().calculateThinkTime();
    System.out.println("Think time: " + thinkTime + "ms");
    
    framework.performAction(() -> clickButton());
    
    // Report every 20 interactions
    if (i % 20 == 19) {
        System.out.println("After " + (i+1) + " interactions:");
        System.out.println("  Error rate: " + 
            (framework.getCognitiveState().getDecisionErrorRate() * 100) + "%");
        System.out.println("  Fatigue: " + 
            framework.getCognitiveState().getFatigueLevel());
    }
}

framework.endSession();
```

### Pattern 4: Testing Memory and Re-verification

```java
framework.startSession();

// Navigate through multiple screens
framework.navigateTo("Step1Screen");
framework.navigateTo("Step2Screen");
framework.navigateTo("Step3Screen");
framework.navigateTo("Step4Screen");
framework.navigateTo("Step5Screen");

// Check if user forgot something and needs to go back
int reverifyDepth = framework.shouldReverify();
if (reverifyDepth > 0) {
    System.out.println("User forgot something, going back " + 
        reverifyDepth + " screens");
    for (int i = 0; i < reverifyDepth; i++) {
        cognitiveDriver.navigateBack();
    }
}

framework.endSession();
```

### Pattern 5: Testing Emotional Responses

```java
framework.startSession();

// Simulate frustrating interactions
for (int i = 0; i < 30; i++) {
    // Check for interaction burst (rapid clicking due to frustration)
    if (framework.getEmotionalBiasHook().shouldTriggerInteractionBurst()) {
        int burst = framework.getEmotionalBiasHook().simulateInteractionBurst();
        System.out.println("Frustrated: " + burst + " rapid clicks");
    }
    
    // Check for hesitation (uncertainty)
    if (framework.getEmotionalBiasHook().shouldTriggerHesitation()) {
        long hesitation = framework.getEmotionalBiasHook().simulateHesitation();
        System.out.println("Hesitated for " + hesitation + "ms");
    }
    
    framework.performAction(() -> clickButton());
}

// Check frustration level
float frustration = framework.getEmotionalBiasHook().calculateFrustrationLevel();
System.out.println("Frustration level: " + (frustration * 100) + "%");

framework.endSession();
```

## Configuration Presets

### High-Fidelity (Most Realistic)
```java
CognitiveConfig config = CognitiveConfig.highFidelity();
```
- 5% base error rate
- 25% max error rate
- 15% context switch probability
- 20% re-verification probability
- Best for finding edge cases

### Low-Fidelity (Faster, Fewer Errors)
```java
CognitiveConfig config = CognitiveConfig.lowFidelity();
```
- 2% base error rate
- 10% max error rate
- 5% context switch probability
- 8% re-verification probability
- Best for quick regression tests

### Default (Balanced)
```java
CognitiveConfig config = CognitiveConfig.defaults();
```
- 3% base error rate
- 15% max error rate
- 10% context switch probability
- 15% re-verification probability
- Good starting point

### Custom Configuration
```java
CognitiveConfig config = new CognitiveConfig.Builder()
    .baseErrorRate(0.04f)
    .maxErrorRate(0.20f)
    .fatigueErrorMultiplier(0.04f)
    .contextSwitchProbability(0.12f)
    .satisficingThreshold(0.65f)
    .minThinkTime(300)
    .maxThinkTime(2000)
    .build();
```

## Running the Example

The framework includes a comprehensive example:

```bash
cd cognitive-testing-framework
./gradlew build
./gradlew run
```

Or compile and run manually:

```bash
javac -cp ".:lib/*" src/main/java/com/cognitive/testing/examples/CognitiveTestExample.java
java -cp ".:lib/*" com.cognitive.testing.examples.CognitiveTestExample
```

## Understanding Session Reports

After each session, you get a detailed report:

```
=== COGNITIVE REALITY REPORT ===

Session: SESSION-1234567890
Duration: 45 minutes
Interactions: 127

--- Cognitive State ---
Attention: 75.0%      # Current attention level
Fatigue: 1.23         # 0=Fresh, 3=Very tired
Stress: 45.0%        # Current stress level
Error Rate: 8.5%      # Current probability of errors

--- Limited Attention ---
AttentionStatistics{
  switches=3,         # Number of context switches
  abandonmentTime=180s,  # Total time abandoned
  depth=4,            # Current navigation depth
  prob=12.50%         # Current abandonment probability
}

--- Decision Fatigue ---
FatigueStats{
  interactions=127,   # Total interactions
  errors=11,          # Total errors committed
  errorRate=8.66%,    # Actual error rate
  avgResponse=850ms,  # Average think time
  slowRate=15.75%,    # Slow responses (>2x normal)
  quality=82.00%      # Decision quality
}

... (more sections)
```

## Troubleshooting

### Issue: Tests are too slow
**Solution**: Use `CognitiveConfig.lowFidelity()` or reduce think times:
```java
CognitiveConfig config = new CognitiveConfig.Builder()
    .minThinkTime(100)
    .maxThinkTime(500)
    .build();
```

### Issue: Too many errors
**Solution**: Reduce error rates:
```java
CognitiveConfig config = new CognitiveConfig.Builder()
    .baseErrorRate(0.01f)
    .maxErrorRate(0.05f)
    .fatigueErrorMultiplier(0.01f)
    .build();
```

### Issue: Not enough context switching
**Solution**: Increase probability:
```java
CognitiveConfig config = new CognitiveConfig.Builder()
    .contextSwitchProbability(0.20f)
    .contextSwitchThreshold(0.3f)
    .build();
```

### Issue: Appium not connecting
**Solution**: Start Appium server:
```bash
appium
```

### Issue: Device not found
**Solution**: Check ADB connection:
```bash
adb devices
```

## Next Steps

1. ✅ Run the basic example
2. ✅ Try different configuration presets
3. ✅ Integrate into your existing tests
4. ✅ Run multiple sessions to see variation
5. ✅ Analyze session reports for patterns
6. ✅ Tune configuration for your app

## Need More Help?

- Full Documentation: See `README.md`
- Implementation Details: See `COGNITIVE_REALISM_IMPLEMENTATION.md`
- Examples: See `CognitiveTestExample.java`

**Remember**: The goal is not to make tests faster, but to make them **more realistic**. A 10-30% slowdown is normal and intentional - it represents real human behavior.
