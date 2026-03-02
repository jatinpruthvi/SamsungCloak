# Samsung Cloak - Quick Start Guide

Get up and running with Samsung Cloak in 5 minutes.

## ⚡ Prerequisites (2 minutes)

1. **Rooted Android Device** (Android 11-14)
2. **LSPosed Installed** (v1.8.3+)
3. **TikTok Installed** (any recent version)

## 🚀 Installation (3 minutes)

### Step 1: Build the Module

```bash
# Clone repository
git clone https://github.com/yourusername/SamsungCloak.git
cd SamsungCloak

# Build APK
./gradlew assembleRelease

# Output: app/build/outputs/apk/release/app-release.apk
```

### Step 2: Install on Device

```bash
# Install via ADB
adb install app/build/outputs/apk/release/app-release.apk
```

**Or** copy APK to device and install manually.

### Step 3: Configure LSPosed

1. Open **LSPosed Manager**
2. Go to **Modules** tab
3. Enable **Samsung Cloak**
4. Tap **Samsung Cloak** → Select **TikTok**
5. **Reboot** device

## ✅ Verify It's Working

### Check 1: LSPosed Logs

Open LSPosed Manager → Logs → Launch TikTok

**Look for:**
```
[SamsungCloak][Main] Samsung Cloak activated for: com.zhiliaoapp.musically
[SamsungCloak][Main] Target: Samsung Galaxy A12 (SM-A125U)
[SamsungCloak][Main] All hooks initialized successfully
```

### Check 2: Device Info

In TikTok:
1. Go to **Profile** → **Settings**
2. Tap **Report a Problem** → **System Info** (if available)
3. Should show: **SM-A125U** / **samsung**

**Or** install "Device Info HW" with module scope enabled.

## 🎯 What's Being Spoofed?

- ✅ **Device Model**: Samsung Galaxy A12 (SM-A125U)
- ✅ **Manufacturer**: Samsung
- ✅ **Android Version**: 11 (API 30)
- ✅ **Chipset**: MediaTek Helio P35 (MT6765)
- ✅ **Display**: 720×1600 @ 320 DPI
- ✅ **RAM**: 3 GB
- ✅ **Carrier**: T-Mobile (US)
- ✅ **Sensors**: Organic patterns (not static)
- ✅ **Battery**: Gradual drain (1% per 3 min)

## 🐛 Troubleshooting

### Module Not Working?

```bash
# 1. Force stop TikTok
adb shell am force-stop com.zhiliaoapp.musically

# 2. Clear app data (optional)
adb shell pm clear com.zhiliaoapp.musically

# 3. Reboot device
adb reboot
```

### Still Not Working?

1. **Check LSPosed is active** (green indicator in manager)
2. **Verify TikTok is in module scope** (checkmark next to TikTok)
3. **Check logs for errors** (LSPosed Manager → Logs)
4. **Try clean install** (uninstall module, reinstall)

## 📚 Next Steps

- **Read README.md** - Full feature documentation
- **Read TECHNICAL.md** - Understand how it works
- **Read VALIDATION_CHECKLIST.md** - Verify all features
- **Read CONTRIBUTING.md** - Contribute improvements

## ⚠️ Important Notes

### What This Does
✅ Makes TikTok see your device as Samsung Galaxy A12  
✅ Simulates organic sensor patterns  
✅ Hides Xposed/root from TikTok  

### What This Doesn't Do
❌ Bypass account bans or restrictions  
❌ Hide your IP address or network fingerprint  
❌ Guarantee undetectability (fingerprinting evolves)  
❌ Violate any laws (use responsibly!)  

### Legal Notice
This module is for **educational and research purposes only**. Use responsibly and ethically. The authors are not responsible for any misuse.

## 🎉 Success!

If you see the activation logs, you're all set! TikTok now perceives your device as a Samsung Galaxy A12.

---

**Questions?** Check the full README.md or open a GitHub issue.

**Last Updated**: February 11, 2024  
**Module Version**: 1.0.0
