# Keep Moshi generated adapters
-keep class **JsonAdapter { *; }

# Retrofit/Moshi annotations
-keepattributes *Annotation*

# OkHttp/Okio
-dontwarn javax.annotation.**

