# GeckoView preservation rules
-keep class org.mozilla.geckoview.** { *; }
-dontwarn org.mozilla.geckoview.**

# Native Bridge & JNI preservation rules
-keep class com.example.messengerwrapper.NativeBridge {
    native <methods>;
    *;
}
-keepattributes Signature, *Annotation*, EnclosingMethod

# snakeyaml, transitive via geckoview -> mozilla-glean.
# Bean-introspection path is unused on Android.
-dontwarn java.beans.BeanInfo
-dontwarn java.beans.FeatureDescriptor
-dontwarn java.beans.IntrospectionException
-dontwarn java.beans.Introspector
-dontwarn java.beans.PropertyDescriptor
-dontwarn org.yaml.snakeyaml.**
