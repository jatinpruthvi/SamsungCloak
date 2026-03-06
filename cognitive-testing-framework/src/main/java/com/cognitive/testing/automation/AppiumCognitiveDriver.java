package com.cognitive.testing.automation;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

import java.time.Duration;
import java.util.List;
import java.util.Random;

/**
 * Appium driver with cognitive realism enhancements.
 * Integrates with CognitiveTestFramework to provide human-like interactions.
 * 
 * Features:
 * - Realistic touch offsets (mis-clicks)
 * - Variable timing (hesitation, think time)
 * - Gesture vs button preference
 * - Decision fatigue effects
 * - Emotional bias bursts
 * 
 * Usage:
 * <pre>
 * AndroidDriver<AndroidElement> driver = new AndroidDriver<>(url, capabilities);
 * AppiumCognitiveDriver cognitiveDriver = new AppiumCognitiveDriver(driver, framework);
 * 
 * // Human-like click with possible mis-click
 * cognitiveDriver.click(button);
 * 
 * // Human-like swipe with natural variation
 * cognitiveDriver.swipe(startX, startY, endX, endY);
 * 
 * // Select option using satisficing
 * AndroidElement selected = cognitiveDriver.selectOption(options, evaluator);
 * </pre>
 */
public class AppiumCognitiveDriver {
    
    private final AndroidDriver<AndroidElement> driver;
    private final CognitiveTestFramework framework;
    private final Random random;
    
    /**
     * Create cognitive driver
     * 
     * @param driver Appium Android driver
     * @param framework Cognitive test framework
     */
    public AppiumCognitiveDriver(AndroidDriver<AndroidElement> driver, 
                                  CognitiveTestFramework framework) {
        this.driver = driver;
        this.framework = framework;
        this.random = framework.getCognitiveState().getRandom();
    }
    
    /**
     * Human-like click with cognitive realism
     * - Mis-clicks based on decision fatigue
     * - Variable timing based on think time
     * - Possible interaction bursts
     * 
     * @param element Element to click
     * @return true if click was successful (not abandoned)
     */
    public boolean click(AndroidElement element) {
        return performCognitiveAction(() -> {
            // Apply touch offset based on decision fatigue
            Point location = applyTouchOffset(element.getLocation());
            Dimension size = element.getSize();
            
            // Calculate click point with offset
            int clickX = location.getX() + size.getWidth() / 2;
            int clickY = location.getY() + size.getHeight() / 2;
            
            // Perform click
            performTap(clickX, clickY);
        });
    }
    
    /**
     * Click with explicit coordinates and cognitive realism
     */
    public boolean click(int x, int y) {
        return performCognitiveAction(() -> {
            Point offset = applyTouchOffset(new Point(x, y));
            performTap(offset.getX(), offset.getY());
        });
    }
    
    /**
     * Human-like tap at coordinates
     */
    private void performTap(int x, int y) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence tap = new Sequence(finger, 1)
            .addAction(finger.createPointerMove(Duration.ofMillis(0), PointerInput.Origin.viewport(), x, y))
            .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
            .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        
        driver.perform(List.of(tap));
    }
    
    /**
     * Apply touch offset based on cognitive state and interaction style
     * Simulates human inaccuracy
     */
    private Point applyTouchOffset(Point original) {
        CognitiveState.InteractionStyle style = framework.getCognitiveState().getInteractionStyle();
        int offsetRange = style.getTouchOffset();
        
        // Decision fatigue increases offset
        int fatigueOffset = (int) (framework.getDecisionFatigueHook()
            .calculateCurrentErrorRate() * 20);
        offsetRange += fatigueOffset;
        
        if (offsetRange == 0) {
            return original;
        }
        
        // Calculate random offset
        int xOffset = random.nextInt(offsetRange * 2 + 1) - offsetRange;
        int yOffset = random.nextInt(offsetRange * 2 + 1) - offsetRange;
        
        return new Point(original.getX() + xOffset, original.getY() + yOffset);
    }
    
    /**
     * Human-like swipe with natural variation
     * - Variable speed and duration
     * - Slight path deviations
     * - Preference for gestures vs back buttons
     */
    public boolean swipe(int startX, int startY, int endX, int endY) {
        return performCognitiveAction(() -> {
            // Check gesture preference
            if (!framework.shouldUseGesture()) {
                // Prefer button, skip swipe
                return;
            }
            
            // Apply natural variation to swipe path
            CognitiveState.InteractionStyle style = framework.getCognitiveState().getInteractionStyle();
            float consistency = style.getSwipeConsistency();
            
            // Calculate intermediate points for natural curve
            int steps = 10;
            for (int i = 0; i < steps; i++) {
                float progress = (float) i / steps;
                int currentX = startX + (int) ((endX - startX) * progress);
                int currentY = startY + (int) ((endY - startY) * progress);
                
                // Add slight deviation based on consistency
                if (random.nextFloat() > consistency) {
                    currentX += random.nextInt(20) - 10;
                    currentY += random.nextInt(20) - 10;
                }
                
                // Perform partial swipe step
                if (i == 0) {
                    performTap(currentX, currentY);
                }
            }
            
            // Final tap at end point
            performTap(endX, endY);
        });
    }
    
    /**
     * Swipe up gesture
     */
    public boolean swipeUp() {
        Dimension size = driver.manage().window().getSize();
        int startX = size.getWidth() / 2;
        int startY = (int) (size.getHeight() * 0.8);
        int endY = (int) (size.getHeight() * 0.2);
        
        return swipe(startX, startY, startX, endY);
    }
    
    /**
     * Swipe down gesture
     */
    public boolean swipeDown() {
        Dimension size = driver.manage().window().getSize();
        int startX = size.getWidth() / 2;
        int startY = (int) (size.getHeight() * 0.2);
        int endY = (int) (size.getHeight() * 0.8);
        
        return swipe(startX, startY, startX, endY);
    }
    
    /**
     * Swipe left gesture
     */
    public boolean swipeLeft() {
        Dimension size = driver.manage().window().getSize();
        int startY = size.getHeight() / 2;
        int startX = (int) (size.getWidth() * 0.8);
        int endX = (int) (size.getWidth() * 0.2);
        
        return swipe(startX, startY, endX, startY);
    }
    
    /**
     * Swipe right gesture
     */
    public boolean swipeRight() {
        Dimension size = driver.manage().window().getSize();
        int startY = size.getHeight() / 2;
        int startX = (int) (size.getWidth() * 0.2);
        int endX = (int) (size.getWidth() * 0.8);
        
        return swipe(startX, startY, endX, startY);
    }
    
    /**
     * Select option using bounded rationality (satisficing)
     * Returns first acceptable option rather than optimal
     */
    public <T> T selectOption(List<T> options, 
                              com.cognitive.testing.hooks.BoundedRationalityHook.OptionEvaluator<T> evaluator) {
        return framework.selectOption(options, evaluator, 0.5f);
    }
    
    /**
     * Select element from list using satisficing
     */
    public AndroidElement selectElement(List<AndroidElement> elements) {
        return framework.selectOption(elements, (element) -> {
            // Evaluate element quality based on visibility and size
            if (!element.isDisplayed()) return 0.0f;
            
            Dimension size = element.getSize();
            int area = size.getWidth() * size.getHeight();
            
            // Normalize area (larger = better)
            return Math.min(1.0f, area / 100000.0f);
        });
    }
    
    /**
     * Type text with realistic timing
     * - Variable typing speed
     * - Possible typos
     * - Backspace corrections
     */
    public boolean typeText(AndroidElement element, String text) {
        return performCognitiveAction(() -> {
            element.click();
            
            // Typing speed varies by cognitive state
            int baseDelay = 100;
            float fatigueMultiplier = 1.0f + framework.getCognitiveState().getFatigueLevel() * 0.5f;
            int charDelay = (int) (baseDelay * fatigueMultiplier);
            
            // Type each character
            StringBuilder typedText = new StringBuilder();
            for (char c : text.toCharArray()) {
                // Check for typo
                float errorRate = framework.getDecisionFatigueHook().calculateCurrentErrorRate();
                boolean hasTypo = random.nextFloat() < errorRate;
                
                if (hasTypo) {
                    // Type wrong character then correct
                    char wrongChar = (char) ('a' + random.nextInt(26));
                    element.sendKeys(String.valueOf(wrongChar));
                    try {
                        Thread.sleep(charDelay * 2);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    
                    // Backspace
                    element.sendKeys("\b");
                    try {
                        Thread.sleep(charDelay);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                
                // Type correct character
                element.sendKeys(String.valueOf(c));
                typedText.append(c);
                
                try {
                    Thread.sleep(charDelay + random.nextInt(50));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }
    
    /**
     * Navigate back with cognitive realism
     * May use gesture or button based on preference
     */
    public boolean navigateBack() {
        return performCognitiveAction(() -> {
            if (framework.shouldUseGesture()) {
                // Use swipe gesture
                swipeRight();
            } else {
                // Use back button
                driver.navigate().back();
            }
        });
    }
    
    /**
     * Wait for element with cognitive realism
     * Variable patience based on cognitive state
     */
    public boolean waitForElement(By locator, int maxWaitSeconds) {
        return performCognitiveAction(() -> {
            int adjustedWait = maxWaitSeconds;
            
            // Lower patience with high stress or fatigue
            float impatience = framework.getCognitiveState().getStressLevel() + 
                              framework.getCognitiveState().getFatigueLevel() / 3.0f;
            adjustedWait = (int) (maxWaitSeconds * (1.0f - impatience * 0.5f));
            adjustedWait = Math.max(3, adjustedWait);
            
            try {
                driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(adjustedWait));
                WebElement element = driver.findElement(locator);
                return element != null;
            } catch (Exception e) {
                return false;
            } finally {
                driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(maxWaitSeconds));
            }
        });
    }
    
    /**
     * Perform action with all cognitive hooks applied
     */
    private boolean performCognitiveAction(Runnable action) {
        // Check for context switching
        if (framework.shouldContextSwitch()) {
            framework.simulateAbandonment();
            return false;
        }
        
        // Check for re-verification (go back to check something)
        int reverifyDepth = framework.shouldReverify();
        if (reverifyDepth > 0) {
            for (int i = 0; i < reverifyDepth; i++) {
                navigateBack();
            }
        }
        
        // Perform the action with cognitive enhancements
        return framework.performAction(action);
    }
    
    /**
     * Get underlying Appium driver
     */
    public AndroidDriver<AndroidElement> getDriver() {
        return driver;
    }
    
    /**
     * Get cognitive framework
     */
    public CognitiveTestFramework getFramework() {
        return framework;
    }
}
