#include <jni.h>
#include "../engine/video_engine.h"

using namespace videoeditor;

static VideoEngine* g_engine = nullptr;

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_videoeditor_app_core_NativeEngine_nativeCreate(JNIEnv* env, jobject thiz) {
    auto* engine = new VideoEngine();
    return reinterpret_cast<jlong>(engine);
}

JNIEXPORT void JNICALL
Java_com_videoeditor_app_core_NativeEngine_nativeDestroy(JNIEnv* env, jobject thiz, jlong handle) {
    auto* engine = reinterpret_cast<VideoEngine*>(handle);
    delete engine;
}

JNIEXPORT jboolean JNICALL
Java_com_videoeditor_app_core_NativeEngine_nativeInitialize(JNIEnv* env, jobject thiz, jlong handle) {
    auto* engine = reinterpret_cast<VideoEngine*>(handle);
    return engine->initialize() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_videoeditor_app_core_NativeEngine_nativeRelease(JNIEnv* env, jobject thiz, jlong handle) {
    auto* engine = reinterpret_cast<VideoEngine*>(handle);
    engine->release();
}

JNIEXPORT jboolean JNICALL
Java_com_videoeditor_app_core_NativeEngine_nativeCreateProject(JNIEnv* env, jobject thiz, 
        jlong handle, jint width, jint height, jint fps) {
    auto* engine = reinterpret_cast<VideoEngine*>(handle);
    return engine->createProject(width, height, fps) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_videoeditor_app_core_NativeEngine_nativeAddClip(JNIEnv* env, jobject thiz,
        jlong handle, jstring filePath, jint trackIndex, jlong position) {
    auto* engine = reinterpret_cast<VideoEngine*>(handle);
    const char* path = env->GetStringUTFChars(filePath, nullptr);
    bool result = engine->addClip(path, trackIndex, position);
    env->ReleaseStringUTFChars(filePath, path);
    return result ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_videoeditor_app_core_NativeEngine_nativeRemoveClip(JNIEnv* env, jobject thiz,
        jlong handle, jint clipId) {
    auto* engine = reinterpret_cast<VideoEngine*>(handle);
    return engine->removeClip(clipId) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_videoeditor_app_core_NativeEngine_nativeTrimClip(JNIEnv* env, jobject thiz,
        jlong handle, jint clipId, jlong trimStart, jlong trimEnd) {
    auto* engine = reinterpret_cast<VideoEngine*>(handle);
    return engine->trimClip(clipId, trimStart, trimEnd) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_videoeditor_app_core_NativeEngine_nativeSplitClip(JNIEnv* env, jobject thiz,
        jlong handle, jint clipId, jlong position) {
    auto* engine = reinterpret_cast<VideoEngine*>(handle);
    return engine->splitClip(clipId, position) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_videoeditor_app_core_NativeEngine_nativeSetClipSpeed(JNIEnv* env, jobject thiz,
        jlong handle, jint clipId, jfloat speed) {
    auto* engine = reinterpret_cast<VideoEngine*>(handle);
    return engine->setClipSpeed(clipId, speed) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_videoeditor_app_core_NativeEngine_nativeSetClipVolume(JNIEnv* env, jobject thiz,
        jlong handle, jint clipId, jfloat volume) {
    auto* engine = reinterpret_cast<VideoEngine*>(handle);
    return engine->setClipVolume(clipId, volume) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_videoeditor_app_core_NativeEngine_nativePlay(JNIEnv* env, jobject thiz, jlong handle) {
    auto* engine = reinterpret_cast<VideoEngine*>(handle);
    engine->play();
}

JNIEXPORT void JNICALL
Java_com_videoeditor_app_core_NativeEngine_nativePause(JNIEnv* env, jobject thiz, jlong handle) {
    auto* engine = reinterpret_cast<VideoEngine*>(handle);
    engine->pause();
}

JNIEXPORT void JNICALL
Java_com_videoeditor_app_core_NativeEngine_nativeStop(JNIEnv* env, jobject thiz, jlong handle) {
    auto* engine = reinterpret_cast<VideoEngine*>(handle);
    engine->stop();
}

JNIEXPORT void JNICALL
Java_com_videoeditor_app_core_NativeEngine_nativeSeekTo(JNIEnv* env, jobject thiz,
        jlong handle, jlong position) {
    auto* engine = reinterpret_cast<VideoEngine*>(handle);
    engine->seekTo(position);
}

JNIEXPORT jlong JNICALL
Java_com_videoeditor_app_core_NativeEngine_nativeGetCurrentPosition(JNIEnv* env, jobject thiz,
        jlong handle) {
    auto* engine = reinterpret_cast<VideoEngine*>(handle);
    return engine->getCurrentPosition();
}

JNIEXPORT jlong JNICALL
Java_com_videoeditor_app_core_NativeEngine_nativeGetDuration(JNIEnv* env, jobject thiz,
        jlong handle) {
    auto* engine = reinterpret_cast<VideoEngine*>(handle);
    return engine->getDuration();
}

JNIEXPORT jboolean JNICALL
Java_com_videoeditor_app_core_NativeEngine_nativeIsPlaying(JNIEnv* env, jobject thiz,
        jlong handle) {
    auto* engine = reinterpret_cast<VideoEngine*>(handle);
    return engine->isPlaying() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_videoeditor_app_core_NativeEngine_nativeSetPreviewSurface(JNIEnv* env, jobject thiz,
        jlong handle, jobject surface) {
    auto* engine = reinterpret_cast<VideoEngine*>(handle);
    
    ANativeWindow* window = nullptr;
    if (surface != nullptr) {
        window = ANativeWindow_fromSurface(env, surface);
    }
    
    engine->setPreviewSurface(window);
}

JNIEXPORT jboolean JNICALL
Java_com_videoeditor_app_core_NativeEngine_nativeAddFilter(JNIEnv* env, jobject thiz,
        jlong handle, jint clipId, jstring filterType, jfloat intensity) {
    auto* engine = reinterpret_cast<VideoEngine*>(handle);
    const char* type = env->GetStringUTFChars(filterType, nullptr);
    
    EffectParams params;
    params.effectType = type;
    params.intensity = intensity;
    
    bool result = engine->addFilter(clipId, type, params);
    env->ReleaseStringUTFChars(filterType, type);
    return result ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_videoeditor_app_core_NativeEngine_nativeRemoveFilter(JNIEnv* env, jobject thiz,
        jlong handle, jint clipId, jint filterId) {
    auto* engine = reinterpret_cast<VideoEngine*>(handle);
    return engine->removeFilter(clipId, filterId) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_videoeditor_app_core_NativeEngine_nativeAddAudioTrack(JNIEnv* env, jobject thiz,
        jlong handle, jstring filePath, jlong position) {
    auto* engine = reinterpret_cast<VideoEngine*>(handle);
    const char* path = env->GetStringUTFChars(filePath, nullptr);
    bool result = engine->addAudioTrack(path, position);
    env->ReleaseStringUTFChars(filePath, path);
    return result ? JNI_TRUE : JNI_FALSE;
}

static jobject g_progressCallback = nullptr;
static JavaVM* g_jvm = nullptr;

JNIEXPORT jboolean JNICALL
Java_com_videoeditor_app_core_NativeEngine_nativeExport(JNIEnv* env, jobject thiz,
        jlong handle, jstring outputPath, jint width, jint height, jint fps, jint bitrate,
        jobject progressCallback) {
    auto* engine = reinterpret_cast<VideoEngine*>(handle);
    const char* path = env->GetStringUTFChars(outputPath, nullptr);
    
    // Store JVM and callback for progress updates
    env->GetJavaVM(&g_jvm);
    if (g_progressCallback != nullptr) {
        env->DeleteGlobalRef(g_progressCallback);
    }
    g_progressCallback = env->NewGlobalRef(progressCallback);
    
    ExportSettings settings;
    settings.outputPath = path;
    settings.width = width;
    settings.height = height;
    settings.fps = fps;
    settings.bitrate = bitrate;
    settings.codec = "video/avc";
    settings.audioCodec = "audio/mp4a-latm";
    settings.audioBitrate = 128000;
    settings.audioSampleRate = 44100;
    
    bool result = engine->exportVideo(settings, [](float progress, const std::string& status) {
        if (g_jvm == nullptr || g_progressCallback == nullptr) return;
        
        JNIEnv* callbackEnv;
        bool attached = false;
        
        if (g_jvm->GetEnv(reinterpret_cast<void**>(&callbackEnv), JNI_VERSION_1_6) != JNI_OK) {
            g_jvm->AttachCurrentThread(&callbackEnv, nullptr);
            attached = true;
        }
        
        jclass callbackClass = callbackEnv->GetObjectClass(g_progressCallback);
        jmethodID method = callbackEnv->GetMethodID(callbackClass, "onProgress", "(FLjava/lang/String;)V");
        
        jstring statusStr = callbackEnv->NewStringUTF(status.c_str());
        callbackEnv->CallVoidMethod(g_progressCallback, method, progress, statusStr);
        callbackEnv->DeleteLocalRef(statusStr);
        
        if (attached) {
            g_jvm->DetachCurrentThread();
        }
    });
    
    env->ReleaseStringUTFChars(outputPath, path);
    return result ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_videoeditor_app_core_NativeEngine_nativeCancelExport(JNIEnv* env, jobject thiz,
        jlong handle) {
    auto* engine = reinterpret_cast<VideoEngine*>(handle);
    engine->cancelExport();
}

}  // extern "C"
