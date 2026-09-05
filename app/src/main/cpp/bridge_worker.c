#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>

static const char *tracker_blocklist[] = {
    "googlesyndication.com",
    "doubleclick.net",
    "google-analytics.com",
    "facebook.com/tr/",
    "ads.twitter.com",
    "analytics.twitter.com",
    "scorecardresearch.com",
    "telemetry",
    "crashlytics",
    NULL
};

bool is_tracker_domain(const char *request_headers) {
    if (!request_headers) return false;
    
    for (int i = 0; tracker_blocklist[i] != NULL; i++) {
        if (strstr(request_headers, tracker_blocklist[i]) != NULL) {
            return true;
        }
    }
    return false;
}

void bridge_worker(const char *payload, char *response_out, size_t max_len) {
    if (!payload || !response_out) {
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
    
    const char *payload = (*env)->GetStringUTFChars(env, payload_jstr, 0);
    char response[512];
    
    bridge_worker(payload, response, sizeof(response));
    
    (*env)->ReleaseStringUTFChars(env, payload_jstr, payload);
    return (*env)->NewStringUTF(env, response);
}
