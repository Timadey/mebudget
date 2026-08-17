# MeBudget release rules. Minify is enabled for release; keep the classes that
# are accessed reflectively by Gson/Retrofit/the Paystack SDK.

# Retrofit + Gson model classes are (de)serialized reflectively by Gson and by
# Retrofit's Gson converter — never strip their members.
-keepclassmembers class com.mebudget.app.data.sync.models.** {
    <fields>;
}
-keep class com.mebudget.app.data.sync.models.** { *; }

# Paystack SDK relies on reflection for card model validation.
-keep class co.paystack.android.** { *; }
-dontwarn co.paystack.android.**

# OkHttp/Retrofit ship their own rules; avoid noisy warnings for optional deps.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Paystack pulls in some optional AndroidX/annotations classes.
-dontwarn javax.annotation.**
-dontwarn javax.net.ssl.**