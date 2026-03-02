package com.samsungcloak.xposed;

import java.util.HashMap;

public class DeviceConstants {
    private DeviceConstants() {}

    public static final String MANUFACTURER = "Samsung";
    public static final String BRAND = "Samsung";
    public static final String MODEL = "SM-A125U";
    public static final String DEVICE = "a12";
    public static final String PRODUCT = "a12ue";
    public static final String HARDWARE = "exynos850";
    public static final String BOARD = "exynos850";
    public static final String BOOTLOADER = "unknown";
    public static final String RADIO = "unknown";
    public static final String SERIAL = "R58M63T8G5J";
    public static final String ID = "RP1A.200720.012";
    public static final String TAGS = "release-keys";
    public static final String TYPE = "user";
    public static final String USER = "dpi";
    public static final String HOST = "21d5-1922-0000";
    public static final String DISPLAY = "RP1A.200720.012.A125USQU3BTK2";
    public static final String FINGERPRINT = "samsung/a12ue/a12:11/RP1A.200720.012/A125USQU3BTK2:user/release-keys";

    public static final String[] SUPPORTED_ABIS = {"arm64-v8a", "armeabi-v7a", "armeabi"};
    public static final String[] SUPPORTED_32_BIT_ABIS = {"armeabi-v7a", "armeabi"};
    public static final String[] SUPPORTED_64_BIT_ABIS = {"arm64-v8a"};
    public static final String CPU_ABI = "arm64-v8a";
    public static final String CPU_ABI2 = "";

    public static final int SDK_INT = 30;
    public static final String RELEASE = "11";
    public static final String SECURITY_PATCH = "2023-06-01";
    public static final String INCREMENTAL = "A125USQU3BTK2";
    public static final String CODENAME = "REL";

    public static final String SOC_MANUFACTURER = "Samsung";
    public static final String SOC_MODEL = "Exynos 850";

    public static final int WIDTH_PIXELS = 720;
    public static final int HEIGHT_PIXELS = 1600;
    public static final int DENSITY_DPI = 320;
    public static final float XDPI = 320.0f;
    public static final float YDPI = 320.0f;
    public static final float DENSITY = 2.0f;
    public static final int SCALED_DENSITY = 320;

    public static final long TOTAL_MEM = 3221225472L;
    public static final int MAX_MEM = 256;

    public static final String NETWORK_OPERATOR = "310260";
    public static final String NETWORK_OPERATOR_NAME = "T-Mobile";
    public static final String NETWORK_COUNTRY_ISO = "us";
    public static final String SIM_OPERATOR = "310260";
    public static final String SIM_OPERATOR_NAME = "T-Mobile";
    public static final String SIM_COUNTRY_ISO = "us";
    public static final String NETWORK_TYPE = "LTE";

    public static final HashMap<String, String> SYSTEM_PROPERTIES = new HashMap<>();

    static {
        SYSTEM_PROPERTIES.put("ro.product.model", MODEL);
        SYSTEM_PROPERTIES.put("ro.product.device", DEVICE);
        SYSTEM_PROPERTIES.put("ro.product.board", BOARD);
        SYSTEM_PROPERTIES.put("ro.product.brand", BRAND);
        SYSTEM_PROPERTIES.put("ro.product.manufacturer", MANUFACTURER);
        SYSTEM_PROPERTIES.put("ro.product.name", PRODUCT);
        SYSTEM_PROPERTIES.put("ro.build.product", DEVICE);
        SYSTEM_PROPERTIES.put("ro.build.id", ID);
        SYSTEM_PROPERTIES.put("ro.build.display.id", DISPLAY);
        SYSTEM_PROPERTIES.put("ro.build.version.sdk", String.valueOf(SDK_INT));
        SYSTEM_PROPERTIES.put("ro.build.version.release", RELEASE);
        SYSTEM_PROPERTIES.put("ro.build.version.security_patch", SECURITY_PATCH);
        SYSTEM_PROPERTIES.put("ro.build.version.incremental", INCREMENTAL);
        SYSTEM_PROPERTIES.put("ro.build.date.utc", "1688140800");
        SYSTEM_PROPERTIES.put("ro.build.date", "2023-07-01");
        SYSTEM_PROPERTIES.put("ro.build.type", TYPE);
        SYSTEM_PROPERTIES.put("ro.build.tags", TAGS);
        SYSTEM_PROPERTIES.put("ro.build.user", USER);
        SYSTEM_PROPERTIES.put("ro.build.host", HOST);
        SYSTEM_PROPERTIES.put("ro.build.fingerprint", FINGERPRINT);
        SYSTEM_PROPERTIES.put("ro.build.description", "a12ue-user 11 RP1A.200720.012 A125USQU3BTK2 release-keys");
        SYSTEM_PROPERTIES.put("ro.build.version.codename", CODENAME);
        SYSTEM_PROPERTIES.put("ro.hardware", HARDWARE);
        SYSTEM_PROPERTIES.put("ro.hardware.platform", "exynos5");
        SYSTEM_PROPERTIES.put("ro.board.platform", "exynos5");
        SYSTEM_PROPERTIES.put("ro.chipname", SOC_MODEL);
        SYSTEM_PROPERTIES.put("ro.soc.model", SOC_MODEL);
        SYSTEM_PROPERTIES.put("ro.soc.manufacturer", SOC_MANUFACTURER);
        SYSTEM_PROPERTIES.put("ro.soc.info", "Exynos 850 (5G Exynos 880)");
        SYSTEM_PROPERTIES.put("ro.mediatek.platform", "");
        SYSTEM_PROPERTIES.put("ro.qcom.board.platform", "");
        SYSTEM_PROPERTIES.put("ro.build.characteristics", "default");
        SYSTEM_PROPERTIES.put("ro.bootloader", BOOTLOADER);
        SYSTEM_PROPERTIES.put("ro.boot.hardware", "exynos850");
        SYSTEM_PROPERTIES.put("ro.boot.bootloader", BOOTLOADER);
        SYSTEM_PROPERTIES.put("ro.boot.revision", "0");
        SYSTEM_PROPERTIES.put("ro.boot.serialno", SERIAL);
        SYSTEM_PROPERTIES.put("ro.boot.hardware.revision", "v1");
        SYSTEM_PROPERTIES.put("ro.boot.hardware.display.primary", "samsung");
        SYSTEM_PROPERTIES.put("persist.sys.dalvik.vm.lib.2", "libart.so");
        SYSTEM_PROPERTIES.put("persist.sys.locale", "en-US");
        SYSTEM_PROPERTIES.put("ro.config.low_ram", "false");
        SYSTEM_PROPERTIES.put("ro.config.compact_action_1", "4");
        SYSTEM_PROPERTIES.put("ro.config.compact_action_2", "2");
        SYSTEM_PROPERTIES.put("ro.dalvik.vm.native.bridge", "0");
        SYSTEM_PROPERTIES.put("ro.debuggable", "0");
        SYSTEM_PROPERTIES.put("ro.secure", "1");
        SYSTEM_PROPERTIES.put("ro.adb.secure", "1");
        SYSTEM_PROPERTIES.put("ro.build.selinux", "1");
        SYSTEM_PROPERTIES.put("ro.zygote", "zygote64");
        SYSTEM_PROPERTIES.put("ro.vendor.qti.va_aosp.supported", "0");
        SYSTEM_PROPERTIES.put("gsm.version.baseband", "A125USQU3BTK2");
        SYSTEM_PROPERTIES.put("gsm.current.operator", T_MOBILE_OPERATOR);
        SYSTEM_PROPERTIES.put("gsm.sim.operator.numeric", SIM_OPERATOR);
        SYSTEM_PROPERTIES.put("gsm.operator.numeric", NETWORK_OPERATOR);
        SYSTEM_PROPERTIES.put("gsm.operator.iso-country", NETWORK_COUNTRY_ISO);
        SYSTEM_PROPERTIES.put("gsm.operator.alpha", NETWORK_OPERATOR_NAME);
        SYSTEM_PROPERTIES.put("gsm.sim.operator.iso-country", SIM_COUNTRY_ISO);
        SYSTEM_PROPERTIES.put("gsm.sim.operator.alpha", SIM_OPERATOR_NAME);
        SYSTEM_PROPERTIES.put("gsm.network.type", NETWORK_TYPE);
        SYSTEM_PROPERTIES.put("ro.telephony.default_network", "13");
        SYSTEM_PROPERTIES.put("telephony.lteOnCdmaDevice", "0");
        SYSTEM_PROPERTIES.put("ro.sf.lcd_density", String.valueOf(DENSITY_DPI));
        SYSTEM_PROPERTIES.put("qcom.sf.lcd_density", String.valueOf(DENSITY_DPI));
        SYSTEM_PROPERTIES.put("ro.sf.lcd_density_override", String.valueOf(DENSITY_DPI));
        SYSTEM_PROPERTIES.put("ro.build.version.preview_sdk", "0");
        SYSTEM_PROPERTIES.put("ro.build.version.preview_sdk_int", "0");
        SYSTEM_PROPERTIES.put("persist.radio.multisim.config", "ss");
        SYSTEM_PROPERTIES.put("rild.libpath", "/vendor/lib64/libril-qc-qmi-1.so");
        SYSTEM_PROPERTIES.put("ro.vendor.build.fingerprint", FINGERPRINT);
        SYSTEM_PROPERTIES.put("ro.vendor.build.id", ID);
        SYSTEM_PROPERTIES.put("ro.vendor.build.version.release", RELEASE);
        SYSTEM_PROPERTIES.put("ro.vendor.build.version.sdk", String.valueOf(SDK_INT));
        SYSTEM_PROPERTIES.put("ro.system.build.fingerprint", FINGERPRINT);
        SYSTEM_PROPERTIES.put("ro.system.build.id", ID);
        SYSTEM_PROPERTIES.put("ro.system.build.version.release", RELEASE);
        SYSTEM_PROPERTIES.put("ro.system.build.version.sdk", String.valueOf(SDK_INT));
        SYSTEM_PROPERTIES.put("ro.product.system.model", MODEL);
        SYSTEM_PROPERTIES.put("ro.product.system.device", DEVICE);
        SYSTEM_PROPERTIES.put("ro.product.system.brand", BRAND);
        SYSTEM_PROPERTIES.put("ro.product.system.manufacturer", MANUFACTURER);
        SYSTEM_PROPERTIES.put("ro.system_ext.build.fingerprint", FINGERPRINT);
        SYSTEM_PROPERTIES.put("ro.product.vendor.model", MODEL);
        SYSTEM_PROPERTIES.put("ro.product.vendor.device", DEVICE);
        SYSTEM_PROPERTIES.put("ro.product.vendor.brand", BRAND);
        SYSTEM_PROPERTIES.put("ro.product.vendor.manufacturer", MANUFACTURER);
        SYSTEM_PROPERTIES.put("ro.odm.build.fingerprint", FINGERPRINT);
        SYSTEM_PROPERTIES.put("ro.product.odm.model", MODEL);
        SYSTEM_PROPERTIES.put("ro.product.odm.device", DEVICE);
        SYSTEM_PROPERTIES.put("ro.product.odm.brand", BRAND);
        SYSTEM_PROPERTIES.put("ro.product.odm.manufacturer", MANUFACTURER);
        SYSTEM_PROPERTIES.put("ro.bootimage.build.fingerprint", FINGERPRINT);
        SYSTEM_PROPERTIES.put("ro.vendor.build.version.codename", CODENAME);
        SYSTEM_PROPERTIES.put("ro.system.build.version.codename", CODENAME);
        SYSTEM_PROPERTIES.put("ro.product.build.version.codename", CODENAME);
    }

    private static final String T_MOBILE_OPERATOR = "T-Mobile";

    public static HashMap<String, String> getSystemProperties() {
        return SYSTEM_PROPERTIES;
    }
}
