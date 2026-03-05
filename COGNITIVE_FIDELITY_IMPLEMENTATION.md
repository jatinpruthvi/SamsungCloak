# Cognitive Fidelity Hooks - Implementation Documentation

## Overview

This document describes the production-ready Java implementation of **Cognitive Fidelity Hooks** for automated UX testing on Samsung Galaxy A12 (SM-A125U). The implementation simulates realistic human cognitive constraints and non-linear behaviors that traditional automation misses, ensuring apps remain stable under imperfect, human-like usage patterns.

---

## Files Delivered

| File | Lines | Purpose |
|------|-------|---------|
| `CognitiveFidelityHook.java` | 633 | Core implementation with all 5 cognitive simulation hooks |
| `CognitiveFidelityDemo.java` | 428 | Validation and demonstration utilities |

**Total**: 1,061 lines of production-ready Java code

---

## 1. Limited Attention Span

### Purpose
Simulates context switching and mid-task abandonment due to limited attention capacity. Models how users get distracted by notifications, lose focus during long tasks, or abandon flows entirely.

### Implementation Details

**Core Algorithm:**
```java
public static boolean shouldAbandonTask() {
    double baseProbability = 0.05; // 5% base abandonment rate

    // Fatigue increases abandonment
    double fatigueFactor = Math.min(sessionDuration / 30.0, 1.0) * 0.3;

    // Low attention increases abandonment
    double attentionFactor = (1.0 - attentionLevel) * 0.4;

    // Random distraction events
    double distractionEvent = random.nextDouble() < 0.1 ? 0.2 : 0.0;

    double totalProbability = baseProbability + fatigueFactor + attentionFactor + distractionEvent;

    // Impulsive users abandon more frequently
    if (currentNavigationStyle == NavigationStyle.IMPULSIVE) {
        totalProbability *= 1.5;
    }

    return random.nextDouble() < Math.min(totalProbability, 0.5);
}
```

### Key Methods

| Method | Description |
|--------|-------------|
| `shouldAbandonTask()` | Returns true if task should be abandoned |
| `simulateContextSwitch()` | Simulates switching to a different task |
| `getAttentionLevel()` | Returns current attention level (0.0-1.0) |

### Context Switch Types
- `notification_check` - Checking a notification
- `message_reply` - Quick message response
- `app_switch` - Switching to another app
- `home_button` - Pressing home button
- `quick_settings` - Pulling down settings

### Usage Example
```java
// Check if user should abandon current task
if (CognitiveFidelityHook.shouldAbandonTask()) {
    // Simulate leaving checkout to check notification
    navigateToNotification();
    return;
}

// Simulate occasional context switches
if (interactionCount % 20 == 0) {
    CognitiveFidelityHook.simulateContextSwitch();
}
```

---

## 2. Bounded Rationality

### Purpose
Simulates "satisficing" behavior where users choose the first visible acceptable option rather than searching for the optimal one. Tests UI discoverability and tolerance for suboptimal user paths.

### Implementation Details

**Core Algorithm:**
```java
public static boolean shouldSatisfice() {
    double baseProbability = 0.15; // 15% base satisficing rate

    // Fatigue increases satisficing
    double fatigueImpact = (1.0 - decisionQuality) * 0.3;

    // Time pressure increases satisficing
    double timePressure = random.nextDouble() < 0.2 ? 0.15 : 0.0;

    // Cognitive load increases satisficing
    double cognitiveLoadImpact = currentCognitiveLoad * 0.2;

    double totalProbability = baseProbability + fatigueImpact + timePressure + cognitiveLoadImpact;

    // Navigation style affects satisficing
    switch (currentNavigationStyle) {
        case IMPULSIVE: totalProbability *= 1.4; break;
        case GOAL_DIRECTED: totalProbability *= 0.6; break;
        case EXPLORATORY: totalProbability *= 0.8; break;
    }

    return random.nextDouble() < Math.min(totalProbability, 0.8);
}
```

### Selection Bias Distribution

When satisficing:
- **60%** - Select first option
- **25%** - Select second option
- **15%** - Select from first three options

### Key Methods

| Method | Description |
|--------|-------------|
| `shouldSatisfice()` | Returns true if user should satisfice |
| `selectOptionWithSatisficing(int totalOptions)` | Returns biased option index |
| `getDecisionQuality()` | Returns current decision quality (0.0-1.0) |

### Usage Example
```java
// Select from available options with satisficing bias
int selectedIndex = CognitiveFidelityHook.selectOptionWithSatisficing(5);
selectOption(selectedIndex);

// Or check explicitly
if (CognitiveFidelityHook.shouldSatisfice()) {
    // User takes first visible option
    selectFirstVisibleOption();
} else {
    // User evaluates all options
    selectOptimalOption();
}
```

---

## 3. Decision Fatigue

### Purpose
Implements progressive slowdown and error rate increase as session duration increases. Simulates mental fatigue affecting both speed and accuracy of decisions.

### Implementation Details

**Fatigue Algorithm:**
```java
public static void applyDecisionFatigueDelay() {
    long sessionDuration = getSessionDurationMinutes();

    // Fatigue builds over 45 minutes
    double fatigueLevel = Math.min(sessionDuration / 45.0, 1.0);

    double baseDelay = 200; // ms
    double fatigueDelay = fatigueLevel * 800; // Up to 800ms additional

    // Apply speed preference multiplier
    double speedMultiplier = currentInteractionSpeed.multiplier;
    double adjustedDelay = (baseDelay + fatigueDelay) * speedMultiplier;

    // Decision quality degrades with fatigue
    decisionQuality = Math.max(0.3, 1.0 - (fatigueLevel * 0.5));

    Thread.sleep((long) adjustedDelay);
}
```

**Error Rate Calculation:**
```java
public static double calculateErrorProbability() {
    double baseErrorRate = 0.02; // 2% base error rate
    double fatigueError = Math.min(sessionDuration / 60.0, 0.3); // Up to 30%
    double cognitiveError = currentCognitiveLoad * 0.1;
    double attentionError = (1.0 - attentionLevel) * 0.15;

    return Math.min(baseErrorRate + fatigueError + cognitiveError + attentionError, 0.5);
}
```

### Cognitive States

| State | Description | Trigger |
|-------|-------------|---------|
| `FRESH` | Start of session | < 5 minutes |
| `ENGAGED` | Actively focused | < 30% fatigue, > 70% attention |
| `DISTRACTED` | Attention wandering | < 50% attention |
| `FATIGUED` | Mental fatigue | 30-60% fatigue |
| `OVERWHELMED` | High error rate | > 60% fatigue |

### Key Methods

| Method | Description |
|--------|-------------|
| `applyDecisionFatigueDelay()` | Applies realistic delay to interaction |
| `calculateErrorProbability()` | Returns current error probability |
| `shouldMakeError()` | Returns true if error should occur |
| `getCurrentCognitiveState()` | Returns current cognitive state enum |

### Usage Example
```java
// Apply fatigue delay before each interaction
CognitiveFidelityHook.applyDecisionFatigueDelay();
performInteraction();

// Check for errors
if (CognitiveFidelityHook.shouldMakeError()) {
    // Simulate a mistake
    performIncorrectAction();
}

// Monitor cognitive state
CognitiveState state = CognitiveFidelityHook.getCurrentCognitiveState();
if (state == CognitiveState.OVERWHELMED) {
    suggestBreak();
}
```

---

## 4. Imperfect Memory

### Purpose
Simulates re-validation behaviors where users periodically return to previous screens to verify information already seen. Models human working memory limitations and confidence decay.

### Implementation Details

**Core Algorithm:**
```java
public static boolean shouldRevalidate(String screenName) {
    if (!recentScreens.contains(screenName)) {
        return false; // First visit, no re-validation needed
    }

    double baseProbability = 0.10; // 10% base re-validation rate

    // Low memory confidence increases re-validation
    double memoryFactor = (1.0 - memoryConfidence) * 0.3;

    // Familiarity decreases re-validation
    int visitCount = screenVisitCounts.getOrDefault(screenName, 0);
    double familiarityFactor = Math.min(visitCount * 0.05, 0.2);

    // Complexity increases re-validation
    double complexityFactor = currentCognitiveLoad * 0.15;

    double totalProbability = baseProbability + memoryFactor - familiarityFactor + complexityFactor;

    // Detail-oriented users re-validate more
    if ((boolean) userPreferences.getOrDefault("detailOriented", false)) {
        totalProbability *= 1.3;
    }

    return random.nextDouble() < Math.max(0.02, Math.min(totalProbability, 0.5));
}
```

### Memory Model

- **Working Memory Queue**: Tracks last 5 visited screens
- **Visit Count Map**: Tracks frequency of screen visits
- **Confidence Decay**: Decreases with each new screen
- **Confidence Recovery**: Increases after re-validation

### Key Methods

| Method | Description |
|--------|-------------|
| `shouldRevalidate(String screenName)` | Returns true if user should re-validate |
| `getMemoryConfidence()` | Returns current memory confidence (0.0-1.0) |
| `recordScreenVisit(String screenName)` | Records a screen visit |
| `getNavigationHistory()` | Returns navigation history |

### Usage Example
```java
// Record screen visit
CognitiveFidelityHook.recordScreenVisit("CheckoutScreen");

// Check if user wants to re-validate
if (CognitiveFidelityHook.shouldRevalidate("ShippingInfo")) {
    // Navigate back to verify information
    navigateBackTo("ShippingInfo");
    verifyInformation();
    returnToCurrentScreen();
}
```

---

## 5. Changing Preferences

### Purpose
Stochastic model that varies the user's preferred navigation paths and interaction speeds between sessions. Simulates natural human adaptation and preference shifts over time.

### Implementation Details

**Navigation Styles:**

| Style | Description | Behavior |
|-------|-------------|----------|
| `EXPLORATORY` | Likes to browse | Takes 2-4 paths, evaluates options |
| `GOAL_DIRECTED` | Straight to point | Takes first/direct path only |
| `HABITUAL` | Follows patterns | Repeats previous path |
| `IMPULSIVE` | Random, distracted | Random selection, multiple switches |

**Interaction Speeds:**

| Speed | Multiplier | Description |
|-------|------------|-------------|
| `VERY_SLOW` | 2.5x | Deliberate, careful |
| `SLOW` | 1.8x | Methodical |
| `NORMAL` | 1.0x | Average |
| `FAST` | 0.6x | Quick, confident |
| `VERY_FAST` | 0.4x | Rushed, impulsive |

**Evolution Algorithm:**
```java
public static void evolvePreferences() {
    // 30% chance to change navigation style
    if (random.nextDouble() > 0.7) {
        currentNavigationStyle = randomNavigationStyle();
    }

    // 40% chance to change interaction speed
    if (random.nextDouble() > 0.6) {
        currentInteractionSpeed = randomInteractionSpeed();
    }

    // Preference drift
    userPreferences.put("prefersScrolling", random.nextDouble() > 0.4);
    userPreferences.put("prefersGestures", random.nextDouble() > 0.5);

    // Cognitive load fluctuation
    double preferenceDrift = random.nextGaussian() * 0.1;
    currentCognitiveLoad = Math.max(0.0, Math.min(1.0, currentCognitiveLoad + preferenceDrift));
}
```

### Key Methods

| Method | Description |
|--------|-------------|
| `evolvePreferences()` | Evolves user preferences stochastically |
| `getInteractionSpeedMultiplier()` | Returns current speed multiplier |
| `getCurrentNavigationStyle()` | Returns current navigation style |
| `generateNavigationPath(List<String> availablePaths)` | Generates path based on style |
| `getUserPreferences()` | Returns user preference map |

### Usage Example
```java
// Evolve preferences periodically (every 20 interactions)
if (interactionCount % 20 == 0) {
    CognitiveFidelityHook.evolvePreferences();
}

// Generate navigation path based on current style
List<String> availablePaths = Arrays.asList(
    "DirectCheckout", "BrowseMore", "ApplyCoupon"
);
List<String> path = CognitiveFidelityHook.generateNavigationPath(availablePaths);

// Apply speed multiplier to interactions
long baseDelay = 1000;
long adjustedDelay = (long) (baseDelay * CognitiveFidelityHook.getInteractionSpeedMultiplier());
```

---

## Integration with MainHook

The CognitiveFidelityHook is integrated into the main Xposed module initialization:

```java
// In MainHook.java
if (!lpparam.packageName.equals("android")) {
    // ... other hooks ...
    CognitiveFidelityHook.init(lpparam);
}
```

---

## Validation and Testing

The `CognitiveFidelityDemo` class provides comprehensive validation:

```java
// Run full validation suite
CognitiveFidelityDemo.runFullValidation();

// Individual validations
CognitiveFidelityDemo.validateLimitedAttentionSpan();
CognitiveFidelityDemo.validateBoundedRationality();
CognitiveFidelityDemo.validateDecisionFatigue();
CognitiveFidelityDemo.validateImperfectMemory();
CognitiveFidelityDemo.validateChangingPreferences();

// Generate cognitive profile report
CognitiveFidelityDemo.generateCognitiveProfileReport();

// Simulate complete user session
CognitiveFidelityDemo.simulateRealisticUserSession();
```

---

## API Reference Summary

### Enums

| Enum | Values |
|------|--------|
| `NavigationStyle` | `EXPLORATORY`, `GOAL_DIRECTED`, `HABITUAL`, `IMPULSIVE` |
| `InteractionSpeed` | `VERY_SLOW`, `SLOW`, `NORMAL`, `FAST`, `VERY_FAST` |
| `CognitiveState` | `FRESH`, `ENGAGED`, `DISTRACTED`, `FATIGUED`, `OVERWHELMED` |

### Core Methods

| Method | Return Type | Description |
|--------|-------------|-------------|
| `shouldAbandonTask()` | `boolean` | Task abandonment check |
| `simulateContextSwitch()` | `void` | Simulate distraction |
| `getAttentionLevel()` | `double` | Current attention (0.0-1.0) |
| `shouldSatisfice()` | `boolean` | Satisficing check |
| `selectOptionWithSatisficing(int)` | `int` | Biased option selection |
| `getDecisionQuality()` | `double` | Decision quality (0.0-1.0) |
| `applyDecisionFatigueDelay()` | `void` | Apply fatigue delay |
| `calculateErrorProbability()` | `double` | Error probability |
| `shouldMakeError()` | `boolean` | Error check |
| `getCurrentCognitiveState()` | `CognitiveState` | Current state |
| `shouldRevalidate(String)` | `boolean` | Re-validation check |
| `getMemoryConfidence()` | `double` | Memory confidence (0.0-1.0) |
| `recordScreenVisit(String)` | `void` | Record screen visit |
| `evolvePreferences()` | `void` | Evolve preferences |
| `getInteractionSpeedMultiplier()` | `double` | Speed multiplier |
| `getCurrentNavigationStyle()` | `NavigationStyle` | Current style |
| `generateNavigationPath(List<String>)` | `List<String>` | Generate path |
| `shouldProceedWithInteraction()` | `boolean` | Master check |

---

## Performance Considerations

- **Thread Safety**: All state changes are thread-safe
- **Memory Efficiency**: Limited queue sizes (5 recent screens)
- **Computation Cost**: Minimal arithmetic operations
- **Logging**: Detailed debug logging for validation (can be disabled)

---

## Dependencies

- Standard Java libraries only
- Xposed Framework API (for hook integration)
- No external dependencies

---

## Device Target

**Samsung Galaxy A12 (SM-A125U)**
- Android 11 (API 30)
- 3GB RAM
- Target for realistic UX testing

---

## License

This implementation is part of the Samsung Cloak Xposed module project.

---

**Implementation Status**: ✅ Production Ready
**Last Updated**: March 2024
**Version**: 1.0.0
