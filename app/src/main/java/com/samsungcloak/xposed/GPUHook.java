package com.samsungcloak.xposed;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class GPUHook {
    private static final String LOG_TAG = "SamsungCloak.GPUHook";
    private static boolean initialized = false;

    private static boolean glHooked = false;

    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (initialized) {
            HookUtils.logDebug("GPUHook already initialized");
            return;
        }

        try {
            hookOpenGLGLES20(lpparam);
            hookOpenGLGLES30(lpparam);
            hookEGL14(lpparam);
            initialized = true;
            HookUtils.logInfo("GPUHook initialized successfully");
        } catch (Exception e) {
            HookUtils.logError("Failed to initialize GPUHook: " + e.getMessage());
        }
    }

    private static void hookOpenGLGLES20(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> gles20Class = XposedHelpers.findClass(
                "android.opengl.GLES20", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(gles20Class, "glGetString", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        if (param.args.length > 0) {
                            int name = (int) param.args[0];
                            int glVendor = 0x1F00;
                            int glRenderer = 0x1F01;
                            int glVersion = 0x1F02;
                            int glExtensions = 0x1F03;

                            if (name == glVendor) {
                                param.setResult(DeviceConstants.GPU_VENDOR);
                                HookUtils.logDebug("GLES20.glGetString(GL_VENDOR) -> " + DeviceConstants.GPU_VENDOR);
                            } else if (name == glRenderer) {
                                param.setResult(DeviceConstants.GPU_RENDERER);
                                HookUtils.logDebug("GLES20.glGetString(GL_RENDERER) -> " + DeviceConstants.GPU_RENDERER);
                            } else if (name == glVersion) {
                                param.setResult(DeviceConstants.GPU_VERSION);
                                HookUtils.logDebug("GLES20.glGetString(GL_VERSION) -> " + DeviceConstants.GPU_VERSION);
                            } else if (name == glExtensions) {
                                param.setResult("GL_EXT_texture_rg_bgra GL_EXT_debug_markers GL_EXT_discard_framebuffer GL_OES_depth_texture GL_OES_packed_depth_stencil");
                            }
                        }
                    } catch (Exception e) {
                        HookUtils.logError("Error in glGetString hook: " + e.getMessage());
                    }
                }
            });

            XposedBridge.hookAllMethods(gles20Class, "glGetIntegerv", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        if (param.args.length >= 2) {
                            int pname = (int) param.args[0];
                            int maxTextureSize = 0x0DF1;

                            if (pname == maxTextureSize && param.args.length >= 3) {
                                int[] params = (int[]) param.args[2];
                                if (params != null && params.length > 0) {
                                    params[0] = DeviceConstants.GPU_MAX_TEXTURE_SIZE;
                                    HookUtils.logDebug("GLES20.glGetIntegerv(GL_MAX_TEXTURE_SIZE) -> " + DeviceConstants.GPU_MAX_TEXTURE_SIZE);
                                }
                            }
                        }
                    } catch (Exception e) {
                        HookUtils.logError("Error in glGetIntegerv hook: " + e.getMessage());
                    }
                }
            });

            HookUtils.logInfo("Hooked GLES20 methods");
        } catch (Exception e) {
            HookUtils.logError("Failed to hook GLES20: " + e.getMessage());
        }
    }

    private static void hookOpenGLGLES30(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> gles30Class = XposedHelpers.findClass(
                "android.opengl.GLES30", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(gles30Class, "glGetString", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        if (param.args.length > 0) {
                            int name = (int) param.args[0];
                            if (!glHooked) {
                                glHooked = true;

                                int glVendor = 0x1F00;
                                int glRenderer = 0x1F01;
                                int glVersion = 0x1F02;

                                if (name == glVendor) {
                                    param.setResult(DeviceConstants.GPU_VENDOR);
                                    HookUtils.logDebug("GLES30.glGetString(GL_VENDOR) -> " + DeviceConstants.GPU_VENDOR);
                                } else if (name == glRenderer) {
                                    param.setResult(DeviceConstants.GPU_RENDERER);
                                    HookUtils.logDebug("GLES30.glGetString(GL_RENDERER) -> " + DeviceConstants.GPU_RENDERER);
                                } else if (name == glVersion) {
                                    param.setResult(DeviceConstants.GPU_VERSION);
                                    HookUtils.logDebug("GLES30.glGetString(GL_VERSION) -> " + DeviceConstants.GPU_VERSION);
                                }
                            }
                        }
                    } catch (Exception e) {
                        HookUtils.logError("Error in glGetString hook: " + e.getMessage());
                    }
                }
            });

            HookUtils.logInfo("Hooked GLES30 methods");
        } catch (Exception e) {
            HookUtils.logError("Failed to hook GLES30: " + e.getMessage());
        }
    }

    private static void hookEGL14(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> egl14Class = XposedHelpers.findClass(
                "android.opengl.EGL14", lpparam.classLoader
            );

            XposedBridge.hookAllMethods(egl14Class, "eglQueryString", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        if (param.args.length >= 2) {
                            int name = (int) param.args[0];
                            int vendor = 0x3049;
                            int renderer = 0x3044;
                            int version = 0x3045;

                            if (name == vendor) {
                                param.setResult(DeviceConstants.GPU_VENDOR);
                                HookUtils.logDebug("EGL14.eglQueryString(EGL_VENDOR) -> " + DeviceConstants.GPU_VENDOR);
                            } else if (name == renderer) {
                                param.setResult(DeviceConstants.GPU_RENDERER);
                                HookUtils.logDebug("EGL14.eglQueryString(EGL_RENDERER) -> " + DeviceConstants.GPU_RENDERER);
                            } else if (name == version) {
                                param.setResult(DeviceConstants.GPU_VERSION);
                                HookUtils.logDebug("EGL14.eglQueryString(EGL_VERSION) -> " + DeviceConstants.GPU_VERSION);
                            }
                        }
                    } catch (Exception e) {
                        HookUtils.logError("Error in eglQueryString hook: " + e.getMessage());
                    }
                }
            });

            HookUtils.logInfo("Hooked EGL14 methods");
        } catch (Exception e) {
            HookUtils.logError("Failed to hook EGL14: " + e.getMessage());
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
