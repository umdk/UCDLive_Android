#include <stdlib.h>
#include "remote_data_observer.h"
#include "jni_RemoteDataObserver.h"
#include <stdio.h>
#include <android/log.h>
#define LOG_TAG "remote_data_observer"

typedef struct Context {
    jobject obj;
    jmethodID jmid_on_video_event;
    jmethodID jmid_on_audio_event;
} Context;

static JavaVM* g_current_java_vm_ = NULL;

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    g_current_java_vm_ = vm;
    return JNI_VERSION_1_4;
}

JNIEXPORT void JNI_OnUnload(JavaVM *jvm, void *reserved) {
    g_current_java_vm_ = NULL;
}

static inline RemoteDataObserver* getInstance(jlong ptr)
{
    return (RemoteDataObserver*)(intptr_t) ptr;
}

static void freeContext(void* opaque) {
    Context* ctx = (Context*) opaque;
    if (ctx && ctx->obj) {
        JNIEnv *env = NULL;
        g_current_java_vm_->GetEnv((void**)&env, JNI_VERSION_1_4);
        env->DeleteGlobalRef(ctx->obj);
    }
    free(ctx);
}

static void onRTCVideoFrame(int uid, int index, void* opaque, unsigned char* frame_data, int size,
                            int width, int height, int orientation, double pts)
{
    Context* ctx = (Context*) opaque;
    if (ctx == NULL) {
        return;
    }

    if(frame_data == NULL) {
        return;
    }

    int isAtached = 0;
    JNIEnv *env = NULL;
    if(g_current_java_vm_->GetEnv((void**)&env, JNI_VERSION_1_4) == JNI_EDETACHED)
    {
        g_current_java_vm_->AttachCurrentThread(&env, NULL);
        isAtached = 1;
    }

    if(env == NULL || frame_data == NULL) {
        return;
    }

    jobject outBuffer = NULL;
    if (frame_data != NULL && size > 0) {
        outBuffer = env->NewDirectByteBuffer(frame_data, size);
    }

    if (outBuffer != NULL) {
        env->CallVoidMethod(ctx->obj, ctx->jmid_on_video_event,
                            uid, index, outBuffer, size, width, height, orientation, pts);
    }

    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
    }

    if(isAtached) {
        g_current_java_vm_->DetachCurrentThread();
    }
}

static void onRTCAudioFrame(void* opaque, unsigned char* frame_data, int length,
                            int bytesPerSample, int sampleRate, int channels, double pts,bool remote)
{
    Context* ctx = (Context*) opaque;
    if (ctx == NULL) {
        return;
    }

    if(frame_data == NULL) {
        return;
    }

    int isAtached = 0;
    JNIEnv *env = NULL;
    if(g_current_java_vm_->GetEnv((void**)&env, JNI_VERSION_1_4) == JNI_EDETACHED)
    {
        g_current_java_vm_->AttachCurrentThread(&env, NULL);
        isAtached = 1;
    }

    if(env == NULL || frame_data == NULL) {
        return;
    }

    jobject outBuffer = NULL;
    if (frame_data != NULL && length > 0) {
        outBuffer = env->NewDirectByteBuffer(frame_data, length);
    }

    if (outBuffer != NULL && length > 0) {
        env->CallVoidMethod(ctx->obj, ctx->jmid_on_audio_event,
                                outBuffer, length, bytesPerSample,
                                sampleRate, channels, pts,remote);
        env->DeleteLocalRef(outBuffer);
    }

    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
    }

    if(isAtached) {
        g_current_java_vm_->DetachCurrentThread();
    }
}


jlong Java_com_ucloud_ulive_example_ext_agora_RemoteDataObserver_createObserver
  (JNIEnv *env, jobject thiz)
{
    RemoteDataObserver* remoteDataObserver = new RemoteDataObserver();
    if (thiz) {
        Context* ctx = (Context *) calloc(1, sizeof(Context));
        if (ctx == NULL) {
            return 0;
        }
        jclass clazz = env->GetObjectClass(thiz);
        ctx->obj = env->NewGlobalRef(thiz);
        ctx->jmid_on_video_event = env->GetMethodID(clazz, "onVideoFrame", "(IILjava/nio/ByteBuffer;IIIID)V");
        ctx->jmid_on_audio_event = env->GetMethodID(clazz, "onAudioFrame", "(Ljava/nio/ByteBuffer;"
                "IIIIDZ)V");
        remoteDataObserver->set_callback(onRTCVideoFrame, onRTCAudioFrame, ctx, freeContext);
    }
    return (jlong)(intptr_t) remoteDataObserver;
}

void Java_com_ucloud_ulive_example_ext_agora_RemoteDataObserver_release
  (JNIEnv *env, jobject thiz, jlong ptr)
{
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "[RemoteDataObserver][release]");
    RemoteDataObserver* remoteDataObserver = getInstance(ptr);
    if(remoteDataObserver != NULL) {
        delete remoteDataObserver;
        remoteDataObserver = NULL;
    }
    return;
}

void Java_com_ucloud_ulive_example_ext_agora_RemoteDataObserver_enableObserver
  (JNIEnv *env, jobject thiz, jlong ptr, jboolean enable) {
    RemoteDataObserver* remoteDataObserver = getInstance(ptr);
    if(remoteDataObserver != NULL) {
      remoteDataObserver->enableObserver(enable);
    }
  }

void Java_com_ucloud_ulive_example_ext_agora_RemoteDataObserver_resetRemoteUid(
        JNIEnv *env, jobject instance, jlong wrapperInstance, jint uid) {
    RemoteDataObserver* remoteDataObserver = getInstance(wrapperInstance);
    if(remoteDataObserver != NULL) {
        remoteDataObserver->resetRemoteUid(uid);
    }
}