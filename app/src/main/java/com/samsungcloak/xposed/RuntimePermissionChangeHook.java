package com.samsungcloak.xposed;

import android.Manifest;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.Process;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RuntimePermissionChangeHook - Runtime Permission Change Simulation
 *
 * Simulates realistic runtime permission granting and revocation scenarios during
 * app foreground/background transitions. This hook adds verisimilitude to permission
 * testing by modeling:
 *
 * 1. Permission Granting During Runtime - User grants permission while app is active
 * 2. Permission Revocation During Runtime - User revokes permission in settings
 * 3. Permission Prompt Response - First-time permission dialogs with allow/deny
 * 4. Background Permission Changes - Permissions changed while app in background
 * 5. App Ops Mode Changes - AppOpsManager mode changes
 * 6. Permission Rationale - Showing rationale dialog before permission grant
 * 7. Permanent Denial - User selects "Don't ask again" and denies
 *
 * Novelty: NEW - Not fully covered by existing hooks
 * Target: Samsung Galaxy A12 (SM-A125U) - Android 10/11
 */
public class RuntimePermissionChangeHook {

    private static final String TAG = "[Permission][Runtime]";
    private static final boolean DEBUG = true;
    private static final ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new Random());

    // Toggleable via SharedPreferences
    private static boolean enabled = true;
    private static AtomicBoolean hookInitialized = new AtomicBoolean(false);

    // Permission simulation configuration
    private static boolean permissionGrantSimulationEnabled = true;
    private static boolean permissionRevokeSimulationEnabled = true;
    private static double grantProbability = 0.70; // 70% of permission requests granted
    private static double revokeProbability = 0.03; // 3% chance of random revoke
    private static double permanentDenialProbability = 0.15; // 15% of denials are permanent

    // App Ops simulation
    private static boolean appOpsSimulationEnabled = true;
    private static final ConcurrentMap<String, Integer> appOpsModes = new ConcurrentHashMap<>();

    // Permission state tracking
    private static final ConcurrentMap<String, PermissionState> permissionStates = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Integer> permissionRequestCount = new ConcurrentHashMap<>();

    // Permission statistics
    private static final AtomicInteger totalPermissionRequests = new AtomicInteger(0);
    private static final AtomicInteger grantedPermissions = new AtomicInteger(0);
    private static final AtomicInteger deniedPermissions = new AtomicInteger(0);
    private static final AtomicInteger revokedPermissions = new AtomicInteger(0);

    // Runtime permission mapping
    private static final Map<String, Integer> DANGEROUS_PERMISSIONS = new HashMap<>();

    // SharedPreferences key for toggle
    private static final String PREFS_NAME = "SamsungCloak_Permission";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_GRANT_SIMULATION = "grant_simulation_enabled";
    private static final String KEY_REVOKE_SIMULATION = "revoke_simulation_enabled";

    static {
        // Initialize dangerous permissions (Android 10/11)
        DANGEROUS_PERMISSIONS.put(Manifest.permission.READ_CALENDAR, PackageManager.PERMISSION_GRANTED);
        DANGEROUS_PERMISSIONS.put(Manifest.permission.WRITE_CALENDAR, PackageManager.PERMISSION_GRANTED);
        DANGEROUS_PERMISSIONS.put(Manifest.permission.CAMERA, PackageManager.PERMISSION_GRANTED);
        DANGEROUS_PERMISSIONS.put(Manifest.permission.READ_CONTACTS, PackageManager.PERMISSION_GRANTED);
        DANGEROUS_PERMISSIONS.put(Manifest.permission.WRITE_CONTACTS, PackageManager.PERMISSION_GRANTED);
        DANGEROUS_PERMISSIONS.put(Manifest.permission.GET_ACCOUNTS, PackageManager.PERMISSION_GRANTED);
        DANGEROUS_PERMISSIONS.put(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
        DANGEROUS_PERMISSIONS.put(Manifest.permission.ACCESS_COARSE_LOCATION, PackageManager.PERMISSION_GRANTED);
        DANGEROUS_PERMISSIONS.put(Manifest.permission.RECORD_AUDIO, PackageManager.PERMISSION_GRANTED);
        DANGEROUS_PERMISSIONS.put(Manifest.permission.READ_PHONE_STATE, PackageManager.PERMISSION_GRANTED);
        DANGEROUS_PERMISSIONS.put(Manifest.permission.CALL_PHONE, PackageManager.PERMISSION_GRANTED);
        DANGEROUS_PERMISSIONS.put(Manifest.permission.READ_CALL_LOG, PackageManager.PERMISSION_GRANTED);
        DANGEROUS_PERMISSIONS.put(Manifest.permission.WRITE_CALL_LOG, PackageManager.PERMISSION_GRANTED);
        DANGEROUS_PERMISSIONS.put(Manifest.permission.ADD_VOICEMAIL, PackageManager.PERMISSION_GRANTED);
        DANGEROUS_PERMISSIONS.put(Manifest.permission.USE_SIP, PackageManager.PERMISSION_GRANTED);
        DANGEROUS_PERMISSIONS.put(Manifest.permission.BODY_SENSORS, PackageManager.PERMISSION_GRANTED);
        DANGEROUS_PERMISSIONS.put(Manifest.permission.SEND_SMS, PackageManager.PERMISSION_GRANTED);
        DANGEROUS_PERMISSIONS.put(Manifest.permission.RECEIVE_SMS, PackageManager.PERMISSION_GRANTED);
        DANGEROUS_PERMISSIONS.put(Manifest.permission.READ_SMS, PackageManager.PERMISSION_GRANTED);
        DANGEROUS_PERMISSIONS.put(Manifest.permission.RECEIVE_WAP_PUSH, PackageManager.PERMISSION_GRANTED);
        DANGEROUS_PERMISSIONS.put(Manifest.permission.RECEIVE_MMS, PackageManager.PERMISSION_GRANTED);
        DANGEROUS_PERMISSIONS.put(Manifest.permission.READ_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
        DANGEROUS_PERMISSIONS.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            DANGEROUS_PERMISSIONS.put(Manifest.permission.ACCESS_BACKGROUND_LOCATION, PackageManager.PERMISSION_GRANTED);
            DANGEROUS_PERMISSIONS.put(Manifest.permission.ACTIVITY_RECOGNITION, PackageManager.PERMISSION_GRANTED);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            DANGEROUS_PERMISSIONS.put(Manifest.permission.READ_MEDIA_IMAGES, PackageManager.PERMISSION_GRANTED);
            DANGEROUS_PERMISSIONS.put(Manifest.permission.READ_MEDIA_VIDEO, PackageManager.PERMISSION_GRANTED);
            DANGEROUS_PERMISSIONS.put(Manifest.permission.READ_MEDIA_AUDIO, PackageManager.PERMISSION_GRANTED);
        }
    }

    public enum PermissionState {
        GRANTED,
        DENIED,
        DENIED_FOREVER, // Permanent denial
        REVOKED,
        PROMPT
    }

    public enum PermissionChangeReason {
        USER_GRANT,
        USER_DENY,
        BACKGROUND_REVOKE,
        SETTINGS_CHANGE,
        AUTO_REVOKE,
        SYSTEM_RESET
    }

    /**
     * Permission state tracking per package
     */
    public static class PermissionState {
        public final String permission;
        public final String packageName;
        public PermissionState currentState;
        public boolean isGranted;
        public boolean isPermanentlyDenied;
        public long lastChangeTime;
        public PermissionChangeReason lastChangeReason;

        public PermissionState(String permission, String packageName) {
            this.permission = permission;
            this.packageName = packageName;
            this.currentState = PermissionState.PROMPT;
            this.isGranted = false;
            this.isPermanentlyDenied = false;
            this.lastChangeTime = 0;
            this.lastChangeReason = null;
        }
    }

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (hookInitialized.getAndSet(true)) {
            HookUtils.logDebug(TAG, "Already initialized");
            return;
        }

        HookUtils.logInfo(TAG, "Initializing Runtime Permission Change Hook");

        try {
            hookPackageManager(lpparam);
            hookAppOpsManager(lpparam);
            hookPermissionManager(lpparam);

            HookUtils.logInfo(TAG, "Runtime Permission Change Hook initialized successfully");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "Init failed: " + t.getMessage());
            hookInitialized.set(false);
        }
    }

    private static void hookPackageManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> packageManagerClass = XposedHelpers.findClass(
                "android.content.pm.PackageManager", lpparam.classLoader
            );

            // Hook checkPermission
            XposedBridge.hookAllMethods(packageManagerClass, "checkPermission",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !permissionGrantSimulationEnabled) return;

                        String permName = (String) param.args[0];
                        String pkgName = (String) param.args[1];

                        if (permName != null && pkgName != null) {
                            String key = pkgName + ":" + permName;
                            PermissionState state = permissionStates.get(key);

                            if (state != null) {
                                totalPermissionRequests.incrementAndGet();

                                int result;
                                if (state.isGranted) {
                                    result = PackageManager.PERMISSION_GRANTED;
                                    grantedPermissions.incrementAndGet();
                                } else {
                                    result = PackageManager.PERMISSION_DENIED;
                                    deniedPermissions.incrementAndGet();
                                }

                                param.setResult(result);
                                HookUtils.logDebug(TAG, "checkPermission " + permName + " for " + pkgName + ": " + result);
                            }
                        }
                    }
                });

            // Hook getPermissionFlags
            XposedBridge.hookAllMethods(packageManagerClass, "getPermissionFlags",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled) return;

                        String permName = (String) param.args[0];
                        String pkgName = (String) param.args[1];

                        if (permName != null && pkgName != null) {
                            String key = pkgName + ":" + permName;
                            PermissionState state = permissionStates.get(key);

                            if (state != null && state.isPermanentlyDenied) {
                                // Set PERMISSION_FLAG_PERMANENTLY_DENIED flag
                                int flags = (int) param.getResult();
                                flags |= PackageManager.PERMISSION_PERMANENTLY_DENIED;
                                param.setResult(flags);
                            }
                        }
                    }
                });

            HookUtils.logInfo(TAG, "PackageManager hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "PackageManager hook failed: " + t.getMessage());
        }
    }

    private static void hookAppOpsManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> appOpsManagerClass = XposedHelpers.findClass(
                "android.app.AppOpsManager", lpparam.classLoader
            );

            // Hook noteOp - checks if operation is allowed
            XposedBridge.hookAllMethods(appOpsManagerClass, "noteOp",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !appOpsSimulationEnabled) return;

                        int op = (int) param.args[0];
                        int uid = (int) param.args[1];
                        String packageName = (String) param.args[2];

                        String key = packageName + ":" + op;
                        Integer savedMode = appOpsModes.get(key);

                        if (savedMode != null) {
                            param.setResult(savedMode);
                            HookUtils.logDebug(TAG, "noteOp returning mode: " + savedMode);
                        }
                    }
                });

            // Hook checkOp
            XposedBridge.hookAllMethods(appOpsManagerClass, "checkOp",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !appOpsSimulationEnabled) return;

                        String opName = (String) param.args[0];
                        int uid = (int) param.args[1];
                        String packageName = (String) param.args[2];

                        String key = packageName + ":" + opName;
                        Integer savedMode = appOpsModes.get(key);

                        if (savedMode != null) {
                            param.setResult(savedMode);
                        }
                    }
                });

            // Hook setMode
            XposedBridge.hookAllMethods(appOpsManagerClass, "setMode",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !appOpsSimulationEnabled) return;

                        String opName = (String) param.args[0];
                        int uid = (int) param.args[1];
                        String packageName = (String) param.args[2];
                        int mode = (int) param.args[3];

                        String key = packageName + ":" + opName;
                        appOpsModes.put(key, mode);

                        HookUtils.logDebug(TAG, "setMode " + opName + " for " + packageName + " to mode " + mode);
                    }
                });

            HookUtils.logInfo(TAG, "AppOpsManager hooked");
        } catch (Throwable t) {
            HookUtils.logError(TAG, "AppOpsManager hook failed: " + t.getMessage());
        }
    }

    private static void hookPermissionManager(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook PermissionManager service
            Class<?> permissionManagerClass = XposedHelpers.findClass(
                "android.content.pm.PermissionManager", lpparam.classLoader
            );

            // Hook checkSelfPermission
            XposedBridge.hookAllMethods(permissionManagerClass, "checkSelfPermission",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enabled || !permissionGrantSimulationEnabled) return;

                        String permName = (String) param.args[0];

                        // Get calling package
                        int callingUid = Binder.getCallingUid();
                        String packageName = getPackageForUid(callingUid);

                        if (permName != null && packageName != null) {
                            String key = packageName + ":" + permName;
                            PermissionState state = permissionStates.get(key);

                            if (state != null) {
                                param.setResult(state.isGranted 
                                    ? PackageManager.PERMISSION_GRANTED 
                                    : PackageManager.PERMISSION_DENIED);
                            }
                        }
                    }
                });

            HookUtils.logInfo(TAG, "PermissionManager hooked");
        } catch (Throwable t) {
            HookUtils.logDebug(TAG, "PermissionManager hook not available: " + t.getMessage());
        }
    }

    /**
     * Simulate permission grant
     */
    public static void simulatePermissionGrant(String packageName, String permission, boolean permanent) {
        if (!enabled || !permissionGrantSimulationEnabled) return;

        String key = packageName + ":" + permission;
        PermissionState state = permissionStates.get(key);

        if (state == null) {
            state = new PermissionState(permission, packageName);
            permissionStates.put(key, state);
        }

        state.isGranted = true;
        state.isPermanentlyDenied = permanent;
        state.currentState = permanent ? PermissionState.DENIED_FOREVER : PermissionState.GRANTED;
        state.lastChangeTime = System.currentTimeMillis();
        state.lastChangeReason = PermissionChangeReason.USER_GRANT;

        // Also update AppOps
        int appOpCode = getAppOpCode(permission);
        if (appOpCode >= 0) {
            String opKey = packageName + ":" + appOpCode;
            appOpsModes.put(opKey, AppOpsManager.MODE_ALLOWED);
        }

        grantedPermissions.incrementAndGet();
        HookUtils.logInfo(TAG, "Permission granted: " + permission + " for " + packageName +
            (permanent ? " (permanent)" : ""));
    }

    /**
     * Simulate permission denial
     */
    public static void simulatePermissionDenial(String packageName, String permission, boolean permanent) {
        if (!enabled || !permissionGrantSimulationEnabled) return;

        String key = packageName + ":" + permission;
        PermissionState state = permissionStates.get(key);

        if (state == null) {
            state = new PermissionState(permission, packageName);
            permissionStates.put(key, state);
        }

        state.isGranted = false;
        state.isPermanentlyDenied = permanent;
        state.currentState = permanent ? PermissionState.DENIED_FOREVER : PermissionState.DENIED;
        state.lastChangeTime = System.currentTimeMillis();
        state.lastChangeReason = PermissionChangeReason.USER_DENY;

        // Update AppOps
        int appOpCode = getAppOpCode(permission);
        if (appOpCode >= 0) {
            String opKey = packageName + ":" + appOpCode;
            appOpsModes.put(opKey, permanent ? AppOpsManager.MODE_IGNORED : AppOpsManager.MODE_ERRORED);
        }

        deniedPermissions.incrementAndGet();
        HookUtils.logInfo(TAG, "Permission denied: " + permission + " for " + packageName +
            (permanent ? " (permanent)" : ""));
    }

    /**
     * Simulate permission revocation
     */
    public static void simulatePermissionRevoke(String packageName, String permission) {
        if (!enabled || !permissionRevokeSimulationEnabled) return;

        String key = packageName + ":" + permission;
        PermissionState state = permissionStates.get(key);

        if (state != null) {
            state.isGranted = false;
            state.currentState = PermissionState.REVOKED;
            state.lastChangeTime = System.currentTimeMillis();
            state.lastChangeReason = PermissionChangeReason.BACKGROUND_REVOKE;

            // Update AppOps
            int appOpCode = getAppOpCode(permission);
            if (appOpCode >= 0) {
                String opKey = packageName + ":" + appOpCode;
                appOpsModes.put(opKey, AppOpsManager.MODE_IGNORED);
            }

            revokedPermissions.incrementAndGet();
            HookUtils.logInfo(TAG, "Permission revoked: " + permission + " for " + packageName);
        }
    }

    /**
     * Simulate random permission changes (for background simulation)
     */
    public static void checkForRandomPermissionChanges(String packageName) {
        if (!enabled) return;

        // Random permission grant
        if (permissionGrantSimulationEnabled && random.get().nextDouble() < revokeProbability) {
            String[] perms = DANGEROUS_PERMISSIONS.keySet().toArray(new String[0]);
            String randomPerm = perms[random.get().nextInt(perms.length)];
            simulatePermissionGrant(packageName, randomPerm, false);
        }

        // Random permission revoke
        if (permissionRevokeSimulationEnabled && random.get().nextDouble() < revokeProbability) {
            for (Map.Entry<String, PermissionState> entry : permissionStates.entrySet()) {
                if (entry.getKey().startsWith(packageName + ":")) {
                    PermissionState state = entry.getValue();
                    if (state.isGranted && random.get().nextDouble() < 0.5) {
                        simulatePermissionRevoke(packageName, state.permission);
                        break;
                    }
                }
            }
        }
    }

    private static String getPackageForUid(int uid) {
        // In a full implementation, would query ActivityManager for package name
        return "unknown";
    }

    private static int getAppOpCode(String permission) {
        // Map permission to AppOp code
        // This is a simplified mapping
        if (permission == null) return -1;

        // Common dangerous permissions
        if (permission.contains("CAMERA")) return AppOpsManager.OPSTR_CAMERA;
        if (permission.contains("LOCATION")) return AppOpsManager.OPSTR_FINE_LOCATION;
        if (permission.contains("RECORD_AUDIO")) return AppOpsManager.OPSTR_RECORD_AUDIO;
        if (permission.contains("READ_CONTACTS") || permission.contains("WRITE_CONTACTS")) {
            return AppOpsManager.OPSTR_READ_CONTACTS;
        }
        if (permission.contains("READ_CALENDAR") || permission.contains("WRITE_CALENDAR")) {
            return AppOpsManager.OPSTR_READ_CALENDAR;
        }
        if (permission.contains("READ_SMS") || permission.contains("SEND_SMS")) {
            return AppOpsManager.OPSTR_READ_SMS;
        }

        return -1;
    }

    /**
     * Get permission statistics
     */
    public static Map<String, Object> getPermissionStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRequests", totalPermissionRequests.get());
        stats.put("granted", grantedPermissions.get());
        stats.put("denied", deniedPermissions.get());
        stats.put("revoked", revokedPermissions.get());

        int total = totalPermissionRequests.get();
        if (total > 0) {
            stats.put("grantRate", (double) grantedPermissions.get() / total);
            stats.put("denialRate", (double) deniedPermissions.get() / total);
        }

        return stats;
    }

    // Configuration methods

    public static void setEnabled(boolean enabled) {
        RuntimePermissionChangeHook.enabled = enabled;
        HookUtils.logInfo(TAG, "Hook " + (enabled ? "enabled" : "disabled"));
    }

    public static void setPermissionGrantSimulationEnabled(boolean enabled) {
        permissionGrantSimulationEnabled = enabled;
        HookUtils.logInfo(TAG, "Permission grant simulation " + (enabled ? "enabled" : "disabled"));
    }

    public static void setPermissionRevokeSimulationEnabled(boolean enabled) {
        permissionRevokeSimulationEnabled = enabled;
        HookUtils.logInfo(TAG, "Permission revoke simulation " + (enabled ? "enabled" : "disabled"));
    }

    public static void setGrantProbability(double probability) {
        grantProbability = HookUtils.clamp(probability, 0.0, 1.0);
        HookUtils.logInfo(TAG, "Grant probability: " + grantProbability);
    }

    public static void setRevokeProbability(double probability) {
        revokeProbability = HookUtils.clamp(probability, 0.0, 1.0);
        HookUtils.logInfo(TAG, "Revoke probability: " + revokeProbability);
    }

    public static void setPermanentDenialProbability(double probability) {
        permanentDenialProbability = HookUtils.clamp(probability, 0.0, 1.0);
        HookUtils.logInfo(TAG, "Permanent denial probability: " + permanentDenialProbability);
    }

    public static int getTotalPermissionRequests() {
        return totalPermissionRequests.get();
    }

    public static int getGrantedPermissions() {
        return grantedPermissions.get();
    }

    public static int getDeniedPermissions() {
        return deniedPermissions.get();
    }

    public static int getRevokedPermissions() {
        return revokedPermissions.get();
    }
}
