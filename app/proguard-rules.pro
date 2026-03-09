-keep class com.samsungcloak.xposed.** { *; }
-keep class de.robv.android.xposed.** { *; }
-keep interface de.robv.android.xposed.** { *; }
-keepattributes *Annotation*
-keepclasseswithmembernames class * {
    native <methods>;
}
