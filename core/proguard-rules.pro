# Preserve source file names and line numbers for GitHub issue crash reports
-keepattributes SourceFile,LineNumberTable

# Keep enum values()/valueOf() used for DataStore preference deserialization and Intent extras
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Jetpack Glance ActionCallbacks instantiated via Class.forName reflection
-keep class * implements androidx.glance.appwidget.action.ActionCallback {
    public <init>();
}
-keep class dev.hayya.core.widget.TogglePrayerActionCallback { <init>(); }
-keep class dev.hayya.core.widget.RefreshWidgetsActionCallback { <init>(); }

# Astronomical calculation library
-keep class com.batoulapps.adhan.** { *; }
