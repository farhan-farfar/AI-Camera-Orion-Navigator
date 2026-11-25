# Add project specific ProGuard rules here.

# NCNN native methods
-keep class com.tencent.ncnn.** { *; }
-keepclassmembers class * {
    native <methods>;
}

# Keep NCNN JNI classes
-keep class com.orion.navigator.** {
    native <methods>;
}
