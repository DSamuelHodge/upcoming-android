# ---------------------------------------------------------------------------
# Upcoming — project-specific ProGuard/R8 rules.
#
# Library consumer rules (Retrofit, Moshi, Room, Firebase, Stripe, Compose,
# Coil, Glance, AppFunctions) are merged automatically by AGP when minification
# is on. The rules below cover the app's own runtime-resolved classes that have
# no library consumer coverage.
# ---------------------------------------------------------------------------

# Retrofit resolves generic signatures (List<EventTypeDto>, ...) and inner
# classes reflectively; keep the attributes it reads.
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*

# Keep line numbers so Crashlytics (and any local stack trace) can be
# deobfuscated against the uploaded mapping file.
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile

# kotlin.Metadata — Moshi's KotlinJsonAdapterFactory (kotlin-reflect) reads it
# to discover constructor parameter names/order at runtime.
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Moshi reflection: every wire DTO is declared @JsonClass(generateAdapter=false),
# so reflection must see the full class (constructor + properties) post-R8.
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclasseswithmembers class * {
    @com.squareup.moshi.FromJson <methods>;
    @com.squareup.moshi.ToJson <methods>;
}

# Wire DTOs — kept whole so KotlinJsonAdapterFactory finds every constructor
# parameter under obfuscation.
-keep class app.getupcoming.core.network.** { *; }

# Room entities + DAOs: generated impls call entity field getters/setters by
# name, so field/method names must survive R8.
-keep class app.getupcoming.core.database.entity.** { *; }
-keep class app.getupcoming.core.database.dao.** { *; }

# FCM push — the service is manifest-registered; keep the package so Firebase's
# reflective dispatch on onMessageReceived/onNewToken never misses a method.
-keep class app.getupcoming.core.push.** { *; }

# AppFunctions — manifest-registered service; the framework resolves the
# shipped functions reflectively through the KSP-generated binder.
-keep class app.getupcoming.core.appfunctions.** { *; }