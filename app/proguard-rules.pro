# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# ─── Room ────────────────────────────────────────────────────────────────────

# Keep all Room Entity classes and their fields
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-keepclassmembers @androidx.room.Entity class * { *; }
-keepclassmembers @androidx.room.Dao interface * { *; }

# Keep Room TypeConverters
-keep @androidx.room.TypeConverters class *
-keepclassmembers @androidx.room.TypeConverters class * { *; }

# Prevent Room from having issues with Kotlin data classes used as entities
-keepclassmembers class * {
    @androidx.room.ColumnInfo <fields>;
    @androidx.room.PrimaryKey <fields>;
    @androidx.room.ForeignKey <fields>;
    @androidx.room.Ignore <fields>;
}

# ─── kotlinx.serialization ───────────────────────────────────────────────────

# Keep @Serializable classes and their companions
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep Kotlin serialization plugin-generated classes
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep all @Serializable annotated classes
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers @kotlinx.serialization.Serializable class * {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep serialization descriptor fields
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
    @kotlinx.serialization.Transient <fields>;
}

# ─── Kotlin ──────────────────────────────────────────────────────────────────

# Keep Kotlin metadata for reflection used by serialization
-keep class kotlin.Metadata { *; }
-keepattributes RuntimeVisibleAnnotations

# ─── Hilt / Dagger ───────────────────────────────────────────────────────────

# Hilt-generated components and modules are handled by Hilt's own rules,
# but keep our injection targets explicitly.
-keep @dagger.hilt.android.HiltAndroidApp class *
-keep @dagger.hilt.android.AndroidEntryPoint class *
-keepclassmembers @dagger.hilt.android.AndroidEntryPoint class * { *; }

# ─── Coroutines ──────────────────────────────────────────────────────────────

-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
