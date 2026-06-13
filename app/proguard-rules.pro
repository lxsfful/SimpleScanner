# ProGuard / R8 rules for SimpleScanner

# Keep OpenCV (JNI entry points and reflection)
-keep class org.opencv.** { *; }
-keep interface org.opencv.** { *; }
-dontwarn org.opencv.**

# Keep PDFBox-Android (uses reflection and native bridges)
-keep class com.tom_roush.** { *; }
-keep interface com.tom_roush.** { *; }
-dontwarn com.tom_roush.**

# Keep Kotlin metadata for reflection-based libs
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keepattributes InnerClasses

# Compose
-dontwarn androidx.compose.**

# CameraX
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# Suppress warnings for missing optional dependencies referenced by libraries
-dontwarn java.lang.invoke.StringConcatFactory
-dontwarn org.bouncycastle.**
-dontwarn org.apache.fontbox.**
