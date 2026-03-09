package com.samsungcloak.xposed;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import android.os.Build;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import com.samsungcloak.xposed.telemetry.TelemetryLayerInitializer;
import com.samsungcloak.xposed.telemetry.StochasticDataInjectionHook;

public class MainHook implements IXposedHookLoadPackage {
    private static final String LOG_TAG = "SamsungCloak.MainHook";

    private static final Set<String> TARGET_PACKAGES = new HashSet<>();
    static {
        TARGET_PACKAGES.add("com.zhiliaoapp.musically");
        TARGET_PACKAGES.add("com.ss.android.ugc.trill");
        TARGET_PACKAGES.add("com.ss.android.ugc.aweme");
        TARGET_PACKAGES.add("android");
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!shouldHookPackage(lpparam)) {
            return;
        }

        HookUtils.logInfo("Loading SamsungCloak for package: " + lpparam.packageName);

        try {
            spoofBuildFields();

            if (!lpparam.packageName.equals("android")) {
                PropertyHook.init(lpparam);
                SensorHook.init(lpparam);
                EnvironmentHook.init(lpparam);
                AntiDetectionHook.init(lpparam);
                IdentifierHook.init(lpparam);
                NetworkSimulator.init(lpparam);
                TouchSimulator.init(lpparam);
                AdvancedTouchSimulator.init(lpparam);
                TimingController.init(lpparam);
                MotionSimulator.init(lpparam);
                VibrationSimulator.init(lpparam);
                GPUHook.init(lpparam);
                ThermalHook.init(lpparam);
                PowerHook.init(lpparam);
                DeepSleepHook.init(lpparam);
                ClassMethodHider.init(lpparam);
                BiometricSpoofHook.init(lpparam);
                ClipboardSecurityHook.init(lpparam);
                TemporalRealismHook.init(lpparam);
                CognitiveFidelityHook.init(lpparam);
                SentimentEngagementHook.init(lpparam);
                ContextualAdaptationEngine.init(lpparam);
                
                // NEW: Additional realism hooks
                AccessibilityImpairmentHook.init(lpparam);
                SocialContextInterruptionHook.init(lpparam);
                HardwareDegradationHook.init(lpparam);
                AmbientEnvironmentHook.init(lpparam);
                GestureComplexityHook.init(lpparam);
                
                // NEW: Extended realism hooks (beyond 12)
                MultiDeviceEcosystemHook.init(lpparam);
                AudioEnvironmentHook.init(lpparam);
                AdvancedBiometricHook.init(lpparam);
                TemporalUsagePatternHook.init(lpparam);
                
                // NEW: Memory Pressure Hook (fills gap in memory simulation)
                MemoryPressureHook.init(lpparam);
                
                TelemetryLayerInitializer.init(lpparam);
                StochasticDataInjectionHook.init(lpparam);
            }

            HookUtils.logInfo("SamsungCloak loaded successfully for: " + lpparam.packageName);
        } catch (Exception e) {
            HookUtils.logError("Failed to load SamsungCloak: " + e.getMessage());
        }
    }

    private boolean shouldHookPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        return TARGET_PACKAGES.contains(lpparam.packageName);
    }

    private void spoofBuildFields() {
        try {
            HookUtils.logInfo("Spoofing Build fields...");

            HookUtils.setStaticField(Build.class, "MANUFACTURER", DeviceConstants.MANUFACTURER);
            HookUtils.setStaticField(Build.class, "BRAND", DeviceConstants.BRAND);
            HookUtils.setStaticField(Build.class, "MODEL", DeviceConstants.MODEL);
            HookUtils.setStaticField(Build.class, "DEVICE", DeviceConstants.DEVICE);
            HookUtils.setStaticField(Build.class, "PRODUCT", DeviceConstants.PRODUCT);
            HookUtils.setStaticField(Build.class, "HARDWARE", DeviceConstants.HARDWARE);
            HookUtils.setStaticField(Build.class, "BOARD", DeviceConstants.BOARD);
            HookUtils.setStaticField(Build.class, "BOOTLOADER", DeviceConstants.BOOTLOADER);
            HookUtils.setStaticField(Build.class, "RADIO", DeviceConstants.RADIO);
            HookUtils.setStaticField(Build.class, "SERIAL", DeviceConstants.SERIAL);
            HookUtils.setStaticField(Build.class, "ID", DeviceConstants.ID);
            HookUtils.setStaticField(Build.class, "TAGS", DeviceConstants.TAGS);
            HookUtils.setStaticField(Build.class, "TYPE", DeviceConstants.TYPE);
            HookUtils.setStaticField(Build.class, "USER", DeviceConstants.USER);
            HookUtils.setStaticField(Build.class, "HOST", DeviceConstants.HOST);
            HookUtils.setStaticField(Build.class, "DISPLAY", DeviceConstants.DISPLAY);
            HookUtils.setStaticField(Build.class, "FINGERPRINT", DeviceConstants.FINGERPRINT);

            Class<?> buildVersionClass = Build.VERSION.class;
            Field sdkIntField = buildVersionClass.getDeclaredField("SDK_INT");
            removeFinal(sdkIntField);
            sdkIntField.set(null, DeviceConstants.SDK_INT);

            Field releaseField = buildVersionClass.getDeclaredField("RELEASE");
            removeFinal(releaseField);
            releaseField.set(null, DeviceConstants.RELEASE);

            Field securityPatchField = buildVersionClass.getDeclaredField("SECURITY_PATCH");
            if (securityPatchField != null) {
                removeFinal(securityPatchField);
                securityPatchField.set(null, DeviceConstants.SECURITY_PATCH);
            }

            Field incrementalField = buildVersionClass.getDeclaredField("INCREMENTAL");
            removeFinal(incrementalField);
            incrementalField.set(null, DeviceConstants.INCREMENTAL);

            HookUtils.logInfo("Build.VERSION fields spoofed successfully");

            try {
                Field supportedAbisField = Build.class.getDeclaredField("SUPPORTED_ABIS");
                removeFinal(supportedAbisField);
                supportedAbisField.set(null, DeviceConstants.SUPPORTED_ABIS);
            } catch (NoSuchFieldException e) {
                HookUtils.logDebug("SUPPORTED_ABIS field not found (may not exist on this API level)");
            }

            try {
                Field supported32BitAbisField = Build.class.getDeclaredField("SUPPORTED_32_BIT_ABIS");
                removeFinal(supported32BitAbisField);
                supported32BitAbisField.set(null, DeviceConstants.SUPPORTED_32_BIT_ABIS);
            } catch (NoSuchFieldException e) {
                HookUtils.logDebug("SUPPORTED_32_BIT_ABIS field not found");
            }

            try {
                Field supported64BitAbisField = Build.class.getDeclaredField("SUPPORTED_64_BIT_ABIS");
                removeFinal(supported64BitAbisField);
                supported64BitAbisField.set(null, DeviceConstants.SUPPORTED_64_BIT_ABIS);
            } catch (NoSuchFieldException e) {
                HookUtils.logDebug("SUPPORTED_64_BIT_ABIS field not found");
            }

            try {
                Field cpuAbiField = Build.class.getDeclaredField("CPU_ABI");
                removeFinal(cpuAbiField);
                cpuAbiField.set(null, DeviceConstants.CPU_ABI);
            } catch (NoSuchFieldException e) {
                HookUtils.logDebug("CPU_ABI field not found");
            }

            try {
                Field cpuAbi2Field = Build.class.getDeclaredField("CPU_ABI2");
                removeFinal(cpuAbi2Field);
                cpuAbi2Field.set(null, DeviceConstants.CPU_ABI2);
            } catch (NoSuchFieldException e) {
                HookUtils.logDebug("CPU_ABI2 field not found");
            }

            HookUtils.logInfo("Build fields spoofed successfully");

        } catch (Exception e) {
            HookUtils.logError("Failed to spoof Build fields: " + e.getMessage());
        }
    }

    private void removeFinal(Field field) throws Exception {
        field.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
    }
}
