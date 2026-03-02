#!/bin/bash

# Samsung Cloak - Project Verification Script
# Verifies all required files are present and valid

echo "=========================================="
echo "Samsung Cloak - Project Verification"
echo "=========================================="
echo ""

ERRORS=0
WARNINGS=0

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

check_file() {
    if [ -f "$1" ]; then
        echo -e "${GREEN}✓${NC} Found: $1"
        return 0
    else
        echo -e "${RED}✗${NC} Missing: $1"
        ((ERRORS++))
        return 1
    fi
}

check_java_syntax() {
    if grep -q "public class\|public interface" "$1" 2>/dev/null; then
        return 0
    else
        echo -e "${YELLOW}⚠${NC} Warning: $1 may have syntax issues"
        ((WARNINGS++))
        return 1
    fi
}

echo "Checking Java Source Files..."
echo "------------------------------"
check_file "app/src/main/java/com/samsungcloak/xposed/MainHook.java"
check_file "app/src/main/java/com/samsungcloak/xposed/DeviceConstants.java"
check_file "app/src/main/java/com/samsungcloak/xposed/HookUtils.java"
check_file "app/src/main/java/com/samsungcloak/xposed/PropertyHook.java"
check_file "app/src/main/java/com/samsungcloak/xposed/SensorHook.java"
check_file "app/src/main/java/com/samsungcloak/xposed/EnvironmentHook.java"
check_file "app/src/main/java/com/samsungcloak/xposed/AntiDetectionHook.java"
echo ""

echo "Checking Build Configuration..."
echo "--------------------------------"
check_file "build.gradle"
check_file "settings.gradle"
check_file "gradle.properties"
check_file "app/build.gradle"
check_file "app/proguard-rules.pro"
echo ""

echo "Checking Android Resources..."
echo "------------------------------"
check_file "app/src/main/AndroidManifest.xml"
check_file "app/src/main/res/values/arrays.xml"
check_file "app/src/main/assets/xposed_init"
echo ""

echo "Checking Documentation..."
echo "--------------------------"
check_file "README.md"
check_file "BUILD_GUIDE.md"
check_file "TECHNICAL.md"
check_file "CHANGELOG.md"
check_file "LICENSE"
check_file "CONTRIBUTING.md"
check_file "PROJECT_SUMMARY.md"
check_file "VALIDATION_CHECKLIST.md"
check_file "DELIVERABLES.md"
check_file "QUICKSTART.md"
echo ""

echo "Checking Project Files..."
echo "--------------------------"
check_file ".gitignore"
echo ""

echo "Verifying xposed_init content..."
echo "---------------------------------"
if [ -f "app/src/main/assets/xposed_init" ]; then
    INIT_CONTENT=$(cat app/src/main/assets/xposed_init)
    if [ "$INIT_CONTENT" = "com.samsungcloak.xposed.MainHook" ]; then
        echo -e "${GREEN}✓${NC} xposed_init content is correct"
    else
        echo -e "${RED}✗${NC} xposed_init content is incorrect"
        echo "  Expected: com.samsungcloak.xposed.MainHook"
        echo "  Found: $INIT_CONTENT"
        ((ERRORS++))
    fi
fi
echo ""

echo "Checking Java file syntax..."
echo "----------------------------"
for file in app/src/main/java/com/samsungcloak/xposed/*.java; do
    if [ -f "$file" ]; then
        check_java_syntax "$file"
    fi
done
echo ""

echo "Counting lines of code..."
echo "--------------------------"
if command -v wc &> /dev/null; then
    JAVA_LINES=$(find app/src/main/java -name "*.java" -exec wc -l {} + 2>/dev/null | tail -1 | awk '{print $1}')
    echo "Total Java LOC: $JAVA_LINES"
    
    if [ "$JAVA_LINES" -lt 1500 ]; then
        echo -e "${YELLOW}⚠${NC} Warning: Expected ~1769 lines, found $JAVA_LINES"
        ((WARNINGS++))
    fi
fi
echo ""

echo "Checking for required keywords..."
echo "----------------------------------"
if grep -r "IXposedHookLoadPackage" app/src/main/java/ > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC} Found IXposedHookLoadPackage interface"
else
    echo -e "${RED}✗${NC} Missing IXposedHookLoadPackage interface"
    ((ERRORS++))
fi

if grep -r "SM-A125U" app/src/main/java/ > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC} Found Samsung Galaxy A12 model identifier"
else
    echo -e "${RED}✗${NC} Missing device model identifier"
    ((ERRORS++))
fi

if grep -r "com.zhiliaoapp.musically" app/src/main/java/ > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC} Found TikTok package name"
else
    echo -e "${RED}✗${NC} Missing TikTok package filter"
    ((ERRORS++))
fi

if grep -r "ThreadLocal" app/src/main/java/ > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC} Found thread-safe Random implementation"
else
    echo -e "${YELLOW}⚠${NC} Warning: ThreadLocal not found"
    ((WARNINGS++))
fi
echo ""

echo "Verification Summary"
echo "===================="
echo -e "Errors: ${RED}$ERRORS${NC}"
echo -e "Warnings: ${YELLOW}$WARNINGS${NC}"
echo ""

if [ $ERRORS -eq 0 ]; then
    echo -e "${GREEN}✓ Project verification PASSED${NC}"
    echo "All required files are present and appear valid."
    echo ""
    echo "Next steps:"
    echo "1. ./gradlew assembleRelease"
    echo "2. adb install app/build/outputs/apk/release/app-release.apk"
    echo "3. Configure LSPosed scope"
    echo "4. Reboot device"
    exit 0
else
    echo -e "${RED}✗ Project verification FAILED${NC}"
    echo "Please fix the errors above before building."
    exit 1
fi
