# Changelog

All notable changes to Samsung Cloak will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2024-02-11

### Added
- Complete device identity spoofing for Samsung Galaxy A12 (SM-A125U)
- Comprehensive system property interception (45+ properties)
- Build class reflection spoofing (both Build and Build.VERSION)
- Organic sensor simulation with time-correlated noise:
  - Accelerometer with Gaussian noise + sinusoidal drift
  - Gyroscope with micro-movement patterns
  - Light sensor with environmental flicker
  - Magnetic field with natural variations
  - Pressure sensor with atmospheric fluctuations
- Environment spoofing:
  - Battery drain simulation (1% per 3 minutes)
  - Memory configuration (3GB RAM)
  - Display metrics (720×1600 @ 320 DPI)
  - Input device identity (Samsung touchscreen)
  - Network/carrier information (T-Mobile)
- Anti-detection measures:
  - Stack trace filtering (removes Xposed frames)
  - File system hiding (framework paths return false)
  - Package manager filtering (hides root/Xposed apps)
- Thread-safe implementation using ThreadLocal<Random>
- Strict package filtering (only targets TikTok variants)
- Comprehensive error handling and logging
- Production-ready code quality

### Technical Details
- Target Android versions: 11-14 (API 30-34)
- Xposed API version: 82+
- Build system: Gradle 8.0+ with AGP 8.1.0
- Language: Java 8
- No external dependencies beyond Xposed API

### Documentation
- Complete README with installation instructions
- BUILD_GUIDE with step-by-step compilation guide
- TECHNICAL document with architecture deep-dive
- Inline code documentation and comments

### Compatibility
- ✅ LSPosed v1.8.3+
- ✅ EdXposed v0.5.2.2+
- ✅ Android 11 (API 30)
- ✅ Android 12 (API 31)
- ✅ Android 13 (API 32)
- ✅ Android 14 (API 33-34)

### Target Applications
- com.zhiliaoapp.musically (TikTok International)
- com.ss.android.ugc.trill (TikTok Regional)
- com.ss.android.ugc.aweme (Douyin)

## [Unreleased]

### Added - Part 5: Operating System Level Human Chaos
- **ClipboardSimulator**: Fake clipboard history with realistic content
  - 50+ realistic clipboard items (URLs, OTP codes, addresses, messages)
  - Auto-rotation every 1-4 hours (human-like usage patterns)
  - Hooks: `getPrimaryClip()`, `getText()`, `hasPrimaryClip()`, `setPrimaryClip()`
  - Updates when app sets clipboard (paste consistency)
- **SystemInteractionHook**: Human-like system interaction patterns
  - **Volume simulation**: Time-based volume levels (morning low, day high, night low)
    - Returns dynamic volume based on current hour (not static constants)
    - Music stream: 20-85% depending on time of day
    - Ring mode: Normal by default, vibrate at night (30% chance)
  - **Keyboard simulation**: Realistic keyboard visibility states
    - Hooks: `isAcceptingText()`, `isActive()`, `getInputMethodWindowVisibleHeight()`
    - Dynamic height with natural variation (270-290 dp)
    - Prevents "typing without keyboard" detection
  - **Notification simulation**: Populated status bar with fake notifications
    - 12+ notification types from common US apps
    - WhatsApp, Gmail, Instagram, Messages, Twitter, Spotify, Slack
    - Realistic timestamps (5 min to 3 hours ago)
    - Different importance levels (high/default/low)
- **AppHistorySpoofer**: Realistic app usage history
  - **Recent Tasks spoofing**: 20+ common US apps in recent tasks list
    - Chrome, WhatsApp, Spotify, Instagram, Gmail, YouTube, Maps
    - Realistic task ordering with shuffle
    - Proper ComponentName matching
  - **Running Processes spoofing**: Background process list
    - System processes + user apps
    - Realistic importance levels
  - **UsageStats spoofing**: App usage time tracking
    - TikTok: ~2 hours daily usage
    - Other apps: 5-60 minutes each
    - Realistic launch counts
    - Proper time range calculations

### Planned Features
- WebView fingerprint spoofing (navigator object, WebGL)
- Input event pressure variation
- Network stack fingerprinting
- Thermal state simulation
- Camera/codec profile matching
- Configurable device profiles (switch between devices)
- GUI configuration activity
- Export/import device profiles

### Under Consideration
- Multi-device profile support
- Region-specific carrier presets
- Advanced behavioral biometrics
- ML-based sensor pattern generation
- Automated profile updates from real devices

## Version History

### Version Numbering Scheme
- **Major** (X.0.0): Breaking changes, major architecture updates
- **Minor** (1.X.0): New features, new hooks, significant enhancements
- **Patch** (1.0.X): Bug fixes, minor improvements, documentation

### Release Schedule
- **Stable releases**: Thoroughly tested, production-ready
- **Beta releases**: Feature-complete, testing in progress
- **Alpha releases**: Experimental, early access

## Migration Guides

### Upgrading from Pre-release to 1.0.0
If you used any pre-release version:
1. Uninstall old module
2. Clear target app data
3. Install new version
4. Reconfigure scope in LSPosed
5. Reboot device

### Breaking Changes
None (initial release)

## Known Issues

### Current Limitations
- WebView fingerprinting not implemented (JS APIs still report real device)
- Network-level fingerprinting not addressed (requires VPN/proxy)
- Server-side correlation detection possible (account history mismatch)
- Some OEM-specific APIs may not be hooked

### Workarounds
- Use fresh accounts to avoid historical fingerprint mismatch
- Combine with VPN for network-level anonymity
- Clear app data before first use with module
- Ensure LSPosed scope is correctly configured

### Reporting Issues
Please report bugs via GitHub Issues with:
- Device model and Android version
- LSPosed/EdXposed version
- Target app version
- Complete LSPosed logs
- Steps to reproduce

## Security Advisories

### CVE-None
No security vulnerabilities reported as of 1.0.0 release.

### Security Policy
- Report vulnerabilities privately via GitHub Security Advisories
- Do not disclose publicly until patch is available
- Responsible disclosure appreciated

## Deprecation Notices

None (initial release)

## Acknowledgments

### Contributors
- Initial development: [Author Name]

### Special Thanks
- LSPosed Framework Team
- Xposed Framework (rovo89)
- Android reverse engineering community
- Beta testers and early adopters

### Third-Party Dependencies
- Xposed API (Apache 2.0 License)
- Android SDK (Apache 2.0 License)

## License

This project is provided for educational and research purposes.

See LICENSE file for full details.

---

**Note**: This changelog follows the format recommended by [Keep a Changelog](https://keepachangelog.com/).

For detailed technical changes, see commit history on GitHub.
