# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile


# Keep Hilt generated classes
-keep class * extends androidx.lifecycle.ViewModel
-keep class **_HiltModules* { *; }
-keep class dagger.hilt.internal.aggregatedroot.AggregatedRoot { *; }
-keep class **_Factory { *; }
-keep class **_MembersInjector { *; }
-keep class com.google.android.gms.common.** { *; }
-keep class com.google.android.gms.location.** { *; }
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.tasks.** { *; }