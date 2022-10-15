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

-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keepattributes JavascriptInterface
-keepattributes *Annotation*

-dontwarn com.razorpay.**
-keep class com.razorpay.** {*;}

-optimizations !method/inlining/*

-keepclasseswithmembers class * {
  public void onPayment*(...);
}

-keep class com.voidapp.magizhiniorganics.magizhiniorganics.data.** { *; }

-keepattributes Signature
-keep class com.google.gson.examples.android.model.** { <fields>; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keep class com.voidapp.magizhiniorganics.magizhiniorganics.utils.Converters

-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Retain generic signatures of TypeToken and its subclasses with R8 version 3.0 and higher.
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

-keep class org.kodein.di.TypeReference { *; }
-keep class org.kodein.di.TypeToken { *; }
-keep class org.kodein.di.TypeTokenKt { *; }
-keep class org.kodein.di.JVMTypeToken { *; }
-keep class org.kodein.di.KodeinAwareJVMKt { *; }
-keep class org.kodein.di.KodeinAware { *; }
-keep class org.kodein.di.KodeinAwareKt { *; }
-keep class org.kodein.di.KodeinAwareJVMKt { *; }
#-keep class org.kodein { *; }


-keep, allowobfuscation, allowoptimization class org.kodein.di.TypeReference
-keep, allowobfuscation, allowoptimization class org.kodein.di.JVMTypeToken
-keep, allowobfuscation, allowoptimization class org.kodein.di.JVMTypeToken

-keep, allowobfuscation, allowoptimization class * extends org.kodein.di.TypeReference
-keep, allowobfuscation, allowoptimization class * extends org.kodein.di.JVMTypeToken

