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


# Keep Retrofit, OkHttp, Okio, and Gson classes
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-keep class com.google.gson.** { *; }
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class okio.** { *; }
-keep interface okio.** { *; }

# Keep your GithubApi interface and its methods
-keep interface com.hereliesaz.cuedetat.network.GithubApi { *; }
# Keep your GithubRelease data class and its members if used directly by Gson
-keep class com.hereliesaz.cuedetat.network.GithubRelease { *; }
-keepclassmembers class com.hereliesaz.cuedetat.network.GithubRelease {
    <fields>;
    <init>(...);
}

# Keep Hilt generated classes
-keep class * extends androidx.lifecycle.ViewModel
-keep class **_HiltModules* { *; }
-keep class dagger.hilt.internal.aggregatedroot.AggregatedRoot { *; }
-keep class **_Factory { *; }
-keep class **_MembersInjector { *; }