# --- FILE: app/proguard-rules.pro ---

# R8 configuration for the Cue d'Etat application.
# This file is automatically applied by the Android Gradle plugin.
# See https://developer.android.com/studio/build/shrink-code for details.

#-------------------------------------------------------------------------------
# Default Proguard Rules
#-------------------------------------------------------------------------------
# These are the default rules that come with a new Android project. They provide
# a good baseline for most applications.

-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification

-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.content.Service
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}
-keepclassmembers class **.R$* {
    public static <fields>;
}

#-------------------------------------------------------------------------------
# Jetpack Compose
#-------------------------------------------------------------------------------
# These rules are necessary for Jetpack Compose to function correctly after
# code shrinking and obfuscation.
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
-keepclassmembers class * {
    @androidx.compose.ui.tooling.preview.Preview <methods>;
}
-keep class androidx.compose.runtime.internal.ComposableLambda { *; }
-keepclassmembers class * implements androidx.compose.runtime.Composer {
  <methods>;
}
-keepclassmembers class * implements androidx.compose.runtime.Composition {
  <methods>;
}
-keepclassmembers class * implements androidx.compose.runtime.Recomposer {
  <methods>;
}

#-------------------------------------------------------------------------------
# Kotlinx Coroutines
#-------------------------------------------------------------------------------
# Keeps critical classes for Kotlin Coroutines.

-keepnames class kotlinx.coroutines.internal.** { *; }
-keep class kotlinx.coroutines.android.** { *;
}
-keepclassmembers class ** {
    kotlinx.coroutines.flow.Flow flow(...);
}

#-------------------------------------------------------------------------------
# Networking: Retrofit, OkHttp, Okio, Gson
#-------------------------------------------------------------------------------
# You wouldn't want the network to forget how to speak.
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-keep class com.google.gson.** { *; }
-keep class okhttp3.** { *;
}
-keep interface okhttp3.** { *; }
-keep class okio.** { *; }
-keep interface okio.** { *;
}

# Keep the Github API and its data classes.
-keep interface com.hereliesaz.cuedetat.network.GithubApi { *; }
-keep class com.hereliesaz.cuedetat.network.GithubRelease { *;
}
-keepclassmembers class com.hereliesaz.cuedetat.network.GithubRelease {
    <fields>;
    <init>(...);
}

#-------------------------------------------------------------------------------
# Dependency Injection: Hilt
#-------------------------------------------------------------------------------
# Hilt requires its generated classes to be preserved.
-keep class * extends androidx.lifecycle.ViewModel
-keep class **_HiltModules* { *; }
-keep class dagger.hilt.internal.aggregatedroot.AggregatedRoot { *; }
-keep class **_Factory { *;
}
-keep class **_MembersInjector { *; }

#-------------------------------------------------------------------------------
# OpenCV
#-------------------------------------------------------------------------------
# Keep all OpenCV classes. The native libraries are large enough without R8
# getting creative and breaking the JNI bindings.

-keep class org.opencv.** { *;
}

#-------------------------------------------------------------------------------
# Google ML Kit & TensorFlow Lite
#-------------------------------------------------------------------------------
# These rules prevent R8 from stripping away essential components of the ML
# models and their interpreters. A model with missing operations is a silent oracle.

-keep class com.google.mlkit.** { *; }
-keep class org.tensorflow.** { *;
}

# Ensure all native JNI methods for TensorFlow Lite and ML Kit are kept.
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# Keep specific model and task classes used by ML Kit.
-keep class com.google.android.gms.internal.mlkit_vision_common.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_object_detection.** { *; }
-keep class com.google.mlkit.vision.objects.** { *; }
-keep class com.google.mlkit.vision.common.** { *;
}

# Keep TensorFlow Lite delegates and support classes.
-keep class org.tensorflow.lite.gpu.GpuDelegate
-keep class org.tensorflow.lite.nnapi.NnApiDelegate
-keep class org.tensorflow.lite.support.** { *;
}

# Prevent obfuscation of model asset file paths if they are dynamically located.
# Since we load from assets, this is a prudent safeguard.
-keepclassmembers class ** {
    java.lang.String getModelPath();
    java.lang.String getLabelPath();
}

#-------------------------------------------------------------------------------
# Application-Specific Rules for com.hereliesaz.cuedetat
#-------------------------------------------------------------------------------
# Final safeguards for the application's core components.
# Keep all data, model, and state classes.
-keep class com.hereliesaz.cuedetat.data.** { *; }
-keep class com.hereliesaz.cuedetat.view.model.** { *;
}
-keep class com.hereliesaz.cuedetat.view.state.** { *; }
-keep class com.hereliesaz.cuedetat.view.config.** { *; }


# Keep your main activity and application class.
-keep public class com.hereliesaz.cuedetat.MainActivity { *; }
-keep public class com.hereliesaz.cuedetat.MyApplication { *;
}

# Added based on R8 build error
-dontwarn com.google.devtools.build.android.desugar.runtime.ThrowableExtension