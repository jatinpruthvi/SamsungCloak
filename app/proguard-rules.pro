-keep class com.samsungcloak.xposed.** { *; }
-keep class de.robv.android.xposed.** { *; }
-keep interface de.robv.android.xposed.** { *; }
-keepclassmembers class * {
    public static void handleLoadPackage(...);
    public static void initZygote(...);
    public static void handleInitPackageResources(...);
}
-dontwarn de.robv.android.xposed.**
