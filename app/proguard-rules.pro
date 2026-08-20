# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# --- Crashlytics ---
# Preserve line numbers so Crashlytics can map stack traces to source.
-keepattributes SourceFile,LineNumberTable
# errorprone annotations are referenced by Google libraries but not shipped at runtime.
-dontwarn com.google.errorprone.annotations.**

# --- kotlinx.serialization ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.kutluoglu.**$$serializer { *; }
-keepclassmembers class com.kutluoglu.** {
    *** Companion;
}
-keepclasseswithmembers class com.kutluoglu.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- osmdroid ---
# osmdroid does not ship consumer rules and relies on reflection for tile providers.
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**