
# Native Bridge & JNI preservation rules
-keep class com.example.messengerwrapper.NativeBridge {
    native <methods>;
    *;
}
-keepattributes Signature, *Annotation*, EnclosingMethod
