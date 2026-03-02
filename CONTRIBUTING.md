# Contributing to Samsung Cloak

Thank you for your interest in contributing to Samsung Cloak! This document provides guidelines and instructions for contributors.

## 📋 Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Coding Standards](#coding-standards)
- [Submission Guidelines](#submission-guidelines)
- [Testing Requirements](#testing-requirements)
- [Documentation](#documentation)

## 🤝 Code of Conduct

### Our Standards

- **Be respectful**: Treat all contributors with respect and kindness
- **Be constructive**: Provide helpful feedback and suggestions
- **Be collaborative**: Work together to improve the project
- **Be ethical**: Only contribute code for legitimate, legal purposes

### Unacceptable Behavior

- Harassment, discrimination, or abusive language
- Encouraging or assisting in illegal activities
- Sharing exploits for malicious purposes
- Violating intellectual property rights

## 🚀 Getting Started

### Prerequisites

Before contributing, ensure you have:

1. **Development Environment**:
   - Android Studio or IntelliJ IDEA
   - JDK 8 or 11
   - Android SDK (API 30-34)

2. **Testing Environment**:
   - Rooted Android device (Android 11-14)
   - LSPosed or EdXposed installed
   - Target apps for testing

3. **Git Knowledge**:
   - Basic Git commands
   - Understanding of pull requests and branches

### Finding Issues to Work On

- Check **GitHub Issues** for open tasks
- Look for issues labeled `good first issue` or `help wanted`
- Ask in discussions if you're unsure where to start

## 💻 Development Setup

### 1. Fork and Clone

```bash
# Fork the repository on GitHub, then:
git clone https://github.com/YOUR_USERNAME/SamsungCloak.git
cd SamsungCloak
```

### 2. Create a Branch

```bash
git checkout -b feature/your-feature-name
# or
git checkout -b fix/your-bug-fix
```

Branch naming conventions:
- `feature/` - New features
- `fix/` - Bug fixes
- `docs/` - Documentation updates
- `refactor/` - Code refactoring
- `test/` - Test additions or improvements

### 3. Build the Project

```bash
./gradlew assembleDebug
```

### 4. Install and Test

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 📝 Coding Standards

### Java Style Guide

#### 1. Naming Conventions

```java
// Classes: PascalCase
public class DeviceConstants { }

// Methods: camelCase
public void hookMethod() { }

// Constants: UPPER_SNAKE_CASE
public static final String BUILD_MODEL = "SM-A125U";

// Variables: camelCase
private String deviceModel;
```

#### 2. Code Organization

```java
public class ExampleHook {
    // Constants first
    private static final String CATEGORY = "Example";
    
    // Static fields
    private static final ThreadLocal<Random> random = ...;
    
    // Instance fields
    private String instanceVariable;
    
    // Public methods
    public static void init(LoadPackageParam lpparam) { }
    
    // Private methods
    private static void helperMethod() { }
}
```

#### 3. Error Handling

**All hooks MUST be wrapped in try-catch**:

```java
@Override
protected void beforeHookedMethod(MethodHookParam param) {
    try {
        // Hook implementation
    } catch (Throwable t) {
        HookUtils.logError(CATEGORY, "Description: " + t.getMessage());
    }
}
```

#### 4. Logging

```java
// Debug logging (disabled in production)
HookUtils.logDebug(CATEGORY, "Detailed debug info");

// Info logging
HookUtils.logInfo(CATEGORY, "Important milestone");

// Error logging (always enabled)
HookUtils.logError(CATEGORY, "Error description");
```

Format: `[SamsungCloak][Category] Message`

#### 5. Constants Management

**Never hardcode values**:

```java
// ✅ CORRECT
String model = DeviceConstants.MODEL;

// ❌ WRONG
String model = "SM-A125U";
```

**All device-specific values belong in DeviceConstants.java**

#### 6. Thread Safety

Use ThreadLocal for non-thread-safe objects:

```java
// ✅ CORRECT
private static final ThreadLocal<Random> random = 
    ThreadLocal.withInitial(() -> new Random());

public static Random getRandom() {
    return random.get();
}

// ❌ WRONG
private static Random random = new Random(); // Race conditions!
```

#### 7. Performance

- Avoid allocations in hot paths (sensor hooks)
- Use HashMap for O(1) lookups
- No logging in production builds
- In-place array modifications

### Code Review Checklist

Before submitting, verify:

- [ ] All hooks wrapped in try-catch
- [ ] No hardcoded values (use DeviceConstants)
- [ ] Thread-safe implementation
- [ ] Proper logging with categories
- [ ] No memory leaks
- [ ] Tested on real device
- [ ] Comments for complex logic
- [ ] Follows existing code style

## 📬 Submission Guidelines

### Pull Request Process

1. **Update your fork**:
   ```bash
   git remote add upstream https://github.com/ORIGINAL_OWNER/SamsungCloak.git
   git fetch upstream
   git rebase upstream/main
   ```

2. **Commit your changes**:
   ```bash
   git add .
   git commit -m "feat: Add new sensor hook for proximity sensor"
   ```

   **Commit message format**:
   - `feat:` - New feature
   - `fix:` - Bug fix
   - `docs:` - Documentation changes
   - `refactor:` - Code refactoring
   - `test:` - Test additions
   - `chore:` - Maintenance tasks

3. **Push to your fork**:
   ```bash
   git push origin feature/your-feature-name
   ```

4. **Create Pull Request**:
   - Go to GitHub and create a PR from your branch
   - Fill out the PR template completely
   - Reference related issues (`Fixes #123`)
   - Provide clear description of changes

### Pull Request Template

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Tested on Android 11
- [ ] Tested on Android 14
- [ ] Tested with TikTok
- [ ] No regressions observed

## Checklist
- [ ] Code follows project style guidelines
- [ ] Comments added for complex logic
- [ ] Documentation updated
- [ ] No hardcoded values
- [ ] All hooks wrapped in try-catch
- [ ] Thread-safe implementation

## Screenshots (if applicable)
Attach screenshots or logs

## Related Issues
Fixes #123
```

## 🧪 Testing Requirements

### Manual Testing

Before submitting, test on:

1. **Multiple Android versions** (at least 2):
   - Android 11 (API 30)
   - Android 13 or 14 (API 33-34)

2. **Target applications**:
   - TikTok International
   - TikTok Lite (if available)

3. **Scenarios**:
   - Fresh install
   - Upgrade from previous version
   - Cold start (after reboot)
   - Hot start (app already running)

### Test Cases

```java
// Example test validation
1. Install module
2. Enable for TikTok
3. Force stop TikTok
4. Reboot device
5. Launch TikTok
6. Verify LSPosed logs show successful initialization
7. Check device info in TikTok settings
8. Monitor sensor data (accelerometer, etc.)
9. Verify battery level changes over time
10. Check for crashes or errors
```

### Regression Testing

Ensure your changes don't break existing functionality:
- Property spoofing still works
- Sensor simulation still organic
- Battery drain still gradual
- Anti-detection still functional

## 📚 Documentation

### When to Update Documentation

Update docs when you:
- Add new features
- Change existing behavior
- Modify APIs or constants
- Add new device profiles
- Change configuration requirements

### Documentation Files

- **README.md**: User-facing documentation, installation, usage
- **TECHNICAL.md**: Architecture, implementation details
- **BUILD_GUIDE.md**: Build instructions, troubleshooting
- **CHANGELOG.md**: Version history, changes
- **Code comments**: Inline documentation for complex logic

### Documentation Style

- Use clear, concise language
- Include code examples
- Provide step-by-step instructions
- Add screenshots where helpful
- Keep formatting consistent

## 🎯 Contribution Ideas

### High Priority

- [ ] WebView fingerprinting hooks
- [ ] Additional device profiles (other Samsung models)
- [ ] GUI configuration activity
- [ ] Automated testing framework

### Medium Priority

- [ ] Network stack fingerprinting
- [ ] Input event pressure variation
- [ ] Thermal state simulation
- [ ] Camera/codec profile hooks

### Low Priority

- [ ] Import/export device profiles
- [ ] Multi-language support
- [ ] Advanced logging dashboard
- [ ] Profile auto-update mechanism

## 🐛 Bug Reports

### Bug Report Template

```markdown
**Describe the bug**
Clear description of the issue

**To Reproduce**
Steps to reproduce:
1. Go to '...'
2. Click on '...'
3. Observe error

**Expected behavior**
What should happen

**Environment:**
- Device: [e.g. Pixel 5]
- Android version: [e.g. 13]
- LSPosed version: [e.g. 1.8.6]
- Module version: [e.g. 1.0.0]
- Target app: [e.g. TikTok 28.5.4]

**Logs**
Attach LSPosed logs or logcat output

**Screenshots**
If applicable
```

## 💡 Feature Requests

### Feature Request Template

```markdown
**Is your feature request related to a problem?**
Clear description of the problem

**Describe the solution you'd like**
Detailed description of desired feature

**Describe alternatives you've considered**
Alternative solutions or workarounds

**Additional context**
Any other relevant information
```

## 🔍 Code Review Process

### What Reviewers Look For

1. **Correctness**: Does the code work as intended?
2. **Safety**: Proper error handling and thread safety
3. **Performance**: No unnecessary overhead
4. **Style**: Follows project conventions
5. **Documentation**: Clear comments and docs
6. **Testing**: Adequately tested

### Review Timeline

- Initial review: Within 3-5 days
- Follow-up reviews: Within 1-2 days
- Merge decision: After at least 1 approval

## 📞 Getting Help

### Where to Ask Questions

- **GitHub Discussions**: General questions, ideas
- **GitHub Issues**: Bug reports, feature requests
- **Pull Request comments**: Code-specific questions

### Response Time

- We aim to respond within 48 hours
- Complex questions may take longer
- Be patient and respectful

## 🙏 Recognition

### Contributors

All contributors will be:
- Listed in CONTRIBUTORS.md
- Credited in release notes
- Thanked in the community

### Significant Contributions

Major contributions may result in:
- Collaborator status
- Code ownership of modules
- Mention in README

## 📄 License

By contributing, you agree that your contributions will be licensed under the MIT License.

---

**Thank you for contributing to Samsung Cloak!**

Together, we can build a powerful, reliable device spoofing framework.
