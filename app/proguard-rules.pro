# ===== 通用 =====
-dontobfuscate
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*,SourceFile,LineNumberTable

# ===== Kotlin =====
-keepclassmembers class kotlin.Metadata { public <methods>; }
-dontwarn kotlin.**
-keep class kotlin.coroutines.Continuation

# ===== AndroidX 常用 =====
-dontwarn androidx.**
-keep class androidx.work.** { *; }
-keep class androidx.room.** { *; }

# ===== Room 实体 / DAO =====
-keep class com.pxy.depositbook.data.entity.** { *; }
-keep class com.pxy.depositbook.data.dao.** { *; }
-keep class com.pxy.depositbook.data.db.** { *; }
-keep class com.pxy.depositbook.data.model.** { *; }

# ===== Gson =====
-keep class com.google.gson.** { *; }
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ===== 保留 Compose 反射所需 =====
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# ===== 移除日志 =====
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
