#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>

// Expanded blocklist for comprehensive ad, analytics, social pixels, and telemetry trackers
static const char *tracker_blocklist[] = {
    "googlesyndication.com",
    "doubleclick.net",
    "google-analytics.com",
    "googletagmanager.com",
    "googleadservices.com",
    "facebook.com/tr/",
    "connect.facebook.net",
    "ads.twitter.com",
    "analytics.twitter.com",
    "scorecardresearch.com",
    "telemetry",
    "crashlytics",
    "amazon-adsystem.com",
    "adnxs.com",
    "criteo.com",
    "hotjar.com",
    "mixpanel.com",
    "segment.io",
    "quantserve.com",
    "pubmatic.com",
    "rubiconproject.com",
    NULL
};

// Hardware Optimization: Inline lookup with branch prediction hints for low-overhead filtering
static inline bool is_tracker_domain(const char *request_headers) {
    if (__builtin_expect(!request_headers, 0)) return false;
    
    for (int i = 0; tracker_blocklist[i] != NULL; i++) {
        if (strstr(request_headers, tracker_blocklist[i]) != NULL) {
            return true;
        }
    }
    return false;
}

// Memory Optimization: Zero dynamic allocations (no malloc/free) to prevent heap fragmentation on low-RAM TV hardware
void bridge_worker(const char *payload, char *response_out, size_t max_len) {
    if (__builtin_expect(!payload || !response_out, 0)) {
        return;
    }

    if (strncmp(payload, "GET ", 4) == 0 || 
        strncmp(payload, "POST ", 5) == 0 || 
        strncmp(payload, "CONNECT ", 8) == 0) {
        
        if (is_tracker_domain(payload)) {
            snprintf(response_out, max_len, 
                "HTTP/1.1 200 OK\r\n"
                "Content-Type: text/plain\r\n"
                "Content-Length: 0\r\n"
                "Connection: close\r\n\r\n");
            return;
        }
    } else if (strncmp(payload, "PING", 4) == 0) {
        snprintf(response_out, max_len, "PONG: Bridge active");
        return;
    }

    snprintf(response_out, max_len, "ACTION: FORWARD");
}

JNIEXPORT jstring JNICALL
Java_com_example_messengerwrapper_NativeBridge_nativeBridgeWorker(
    JNIEnv *env,
    jobject thiz,
    jstring payload_jstr) {
    
    // Memory Optimization: Use stack buffers or direct string operations where possible
    const char *payload = (*env)->GetStringUTFChars(env, payload_jstr, 0);
    char response[512];
    
    bridge_worker(payload, response, sizeof(response));
    
    (*env)->ReleaseStringUTFChars(env, payload_jstr, payload);
    return (*env)->NewStringUTF(env, response);
}
