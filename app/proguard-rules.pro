# Project-specific ProGuard / R8 rules for NewbiesTeam V1

# Keep JavaScript Interface methods
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface

# Keep WorkManager Workers
-keepclassmembers class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keepclassmembers class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class androidx.work.** { *; }

# Keep Domain Models (for JSON serialization & reflection)
-keep class team.newbies.core.model.** { *; }

# Keep Material Design components & dynamic theme
-keep class com.google.android.material.** { *; }

# Optimization attributes
-dontwarn okio.**
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
