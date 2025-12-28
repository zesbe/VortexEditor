#ifndef VIDEO_EDITOR_COMMON_H
#define VIDEO_EDITOR_COMMON_H

#include <jni.h>
#include <android/log.h>
#include <android/bitmap.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>

#include <string>
#include <vector>
#include <memory>
#include <functional>
#include <mutex>
#include <atomic>
#include <thread>
#include <queue>
#include <condition_variable>

// Logging macros
#define LOG_TAG "VideoEditor"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace videoeditor {

// Video frame format
enum class PixelFormat {
    RGBA,
    RGB,
    NV21,
    YUV420P,
    UNKNOWN
};

// Video resolution presets
struct Resolution {
    int width;
    int height;
    
    static Resolution HD() { return {1280, 720}; }
    static Resolution FHD() { return {1920, 1080}; }
    static Resolution QHD() { return {2560, 1440}; }
    static Resolution UHD() { return {3840, 2160}; }
};

// Video frame data
struct VideoFrame {
    std::vector<uint8_t> data;
    int width;
    int height;
    PixelFormat format;
    int64_t timestamp_us;  // microseconds
    
    size_t dataSize() const {
        switch (format) {
            case PixelFormat::RGBA: return width * height * 4;
            case PixelFormat::RGB: return width * height * 3;
            case PixelFormat::NV21: return width * height * 3 / 2;
            case PixelFormat::YUV420P: return width * height * 3 / 2;
            default: return 0;
        }
    }
};

// Audio sample data
struct AudioSample {
    std::vector<int16_t> data;
    int sampleRate;
    int channels;
    int64_t timestamp_us;
};

// Clip info on timeline
struct ClipInfo {
    std::string filePath;
    int64_t startTime_us;
    int64_t endTime_us;
    int64_t trimStart_us;
    int64_t trimEnd_us;
    float speed;
    float volume;
    int trackIndex;
};

// Effect parameters
struct EffectParams {
    std::string effectType;
    float intensity;
    std::vector<float> params;
};

// Export settings
struct ExportSettings {
    std::string outputPath;
    int width;
    int height;
    int fps;
    int bitrate;
    std::string codec;
    std::string audioCodec;
    int audioBitrate;
    int audioSampleRate;
};

// Progress callback
using ProgressCallback = std::function<void(float progress, const std::string& status)>;

// Error callback
using ErrorCallback = std::function<void(int code, const std::string& message)>;

}  // namespace videoeditor

#endif  // VIDEO_EDITOR_COMMON_H
