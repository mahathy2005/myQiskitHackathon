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
# Suppress missing SLF4J logger binding warning
-dontwarn org.slf4j.**

# Prevent ProGuard from stripping Vosk offline speech recognition native calls
-keep class com.alphacephei.vosk.** { *; }
-dontwarn com.alphacephei.vosk.**

# Preserve GraphHopper / Mapsforge classes used in offline mapping
-keep class com.graphhopper.** { *; }
-dontwarn com.graphhopper.**
-keep class org.mapsforge.** { *; }
-dontwarn org.mapsforge.**
