# GeckoView preservation rules
-keep class org.mozilla.geckoview.** { *; }
-dontwarn org.mozilla.geckoview.**

# Native Bridge & JNI preservation rules
-keep class com.example.messengerwrapper.NativeBridge {
    native <methods>;
    *;
}
-keepattributes Signature, *Annotation*, EnclosingMethod
