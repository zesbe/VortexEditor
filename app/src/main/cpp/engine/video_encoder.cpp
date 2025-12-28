#include "video_encoder.h"
#include <fcntl.h>
#include <unistd.h>

namespace videoeditor {

VideoEncoder::VideoEncoder()
    : m_codec(nullptr)
    , m_muxer(nullptr)
    , m_format(nullptr)
    , m_videoTrackIndex(-1)
    , m_width(1920)
    , m_height(1080)
    , m_fps(30)
    , m_bitrate(10000000)
    , m_frameCount(0)
    , m_frameDuration(0)
    , m_outputFd(-1)
    , m_initialized(false)
    , m_muxerStarted(false)
    , m_progressCallback(nullptr) {
    LOGI("VideoEncoder created");
}

VideoEncoder::~VideoEncoder() {
    release();
    LOGI("VideoEncoder destroyed");
}

bool VideoEncoder::initialize() {
    std::lock_guard<std::mutex> lock(m_mutex);
    m_initialized = true;
    LOGI("VideoEncoder initialized");
    return true;
}

void VideoEncoder::release() {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    if (m_codec) {
        AMediaCodec_stop(m_codec);
        AMediaCodec_delete(m_codec);
        m_codec = nullptr;
    }
    
    if (m_muxer) {
        if (m_muxerStarted) {
            AMediaMuxer_stop(m_muxer);
        }
        AMediaMuxer_delete(m_muxer);
        m_muxer = nullptr;
    }
    
    if (m_format) {
        AMediaFormat_delete(m_format);
        m_format = nullptr;
    }
    
    if (m_outputFd >= 0) {
        close(m_outputFd);
        m_outputFd = -1;
    }
    
    m_muxerStarted = false;
    m_initialized = false;
    LOGI("VideoEncoder released");
}

bool VideoEncoder::configure(const ExportSettings& settings) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    release();
    
    m_width = settings.width;
    m_height = settings.height;
    m_fps = settings.fps;
    m_bitrate = settings.bitrate;
    m_outputPath = settings.outputPath;
    m_frameDuration = 1000000 / m_fps;  // microseconds
    m_frameCount = 0;
    
    // Create format
    m_format = AMediaFormat_new();
    AMediaFormat_setString(m_format, AMEDIAFORMAT_KEY_MIME, "video/avc");
    AMediaFormat_setInt32(m_format, AMEDIAFORMAT_KEY_WIDTH, m_width);
    AMediaFormat_setInt32(m_format, AMEDIAFORMAT_KEY_HEIGHT, m_height);
    AMediaFormat_setInt32(m_format, AMEDIAFORMAT_KEY_BIT_RATE, m_bitrate);
    AMediaFormat_setInt32(m_format, AMEDIAFORMAT_KEY_FRAME_RATE, m_fps);
    AMediaFormat_setInt32(m_format, AMEDIAFORMAT_KEY_I_FRAME_INTERVAL, 1);
    AMediaFormat_setInt32(m_format, AMEDIAFORMAT_KEY_COLOR_FORMAT, 21); // COLOR_FormatYUV420SemiPlanar
    
    // Create encoder
    m_codec = AMediaCodec_createEncoderByType("video/avc");
    if (!m_codec) {
        LOGE("Failed to create encoder");
        return false;
    }
    
    // Configure encoder
    media_status_t status = AMediaCodec_configure(m_codec, m_format, nullptr, nullptr, 
        AMEDIACODEC_CONFIGURE_FLAG_ENCODE);
    if (status != AMEDIA_OK) {
        LOGE("Failed to configure encoder");
        return false;
    }
    
    // Start encoder
    status = AMediaCodec_start(m_codec);
    if (status != AMEDIA_OK) {
        LOGE("Failed to start encoder");
        return false;
    }
    
    // Open output file and create muxer
    m_outputFd = open(m_outputPath.c_str(), O_WRONLY | O_CREAT | O_TRUNC, 0644);
    if (m_outputFd < 0) {
        LOGE("Failed to open output file: %s", m_outputPath.c_str());
        return false;
    }
    
    m_muxer = AMediaMuxer_new(m_outputFd, AMEDIAMUXER_OUTPUT_FORMAT_MPEG_4);
    if (!m_muxer) {
        LOGE("Failed to create muxer");
        return false;
    }
    
    m_initialized = true;
    LOGI("VideoEncoder configured: %dx%d @ %d fps, bitrate: %d", m_width, m_height, m_fps, m_bitrate);
    return true;
}

bool VideoEncoder::encodeFrame(const VideoFrame& frame) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    if (!m_initialized || !m_codec) {
        return false;
    }
    
    // Get input buffer
    ssize_t inputBufferIdx = AMediaCodec_dequeueInputBuffer(m_codec, 10000);
    if (inputBufferIdx < 0) {
        LOGW("Failed to get input buffer");
        return false;
    }
    
    size_t inputBufferSize;
    uint8_t* inputBuffer = AMediaCodec_getInputBuffer(m_codec, inputBufferIdx, &inputBufferSize);
    
    // Convert RGBA to YUV420 and copy to input buffer
    // Simplified conversion - real implementation would use libyuv
    size_t yuvSize = m_width * m_height * 3 / 2;
    
    // Copy Y plane (simplified - just using R channel as Y)
    for (int i = 0; i < m_width * m_height && i * 4 < frame.data.size(); i++) {
        inputBuffer[i] = frame.data[i * 4];  // R as Y
    }
    
    // Copy UV planes (simplified)
    int uvStart = m_width * m_height;
    for (int i = 0; i < m_width * m_height / 4; i++) {
        inputBuffer[uvStart + i * 2] = 128;      // U
        inputBuffer[uvStart + i * 2 + 1] = 128;  // V
    }
    
    int64_t presentationTime = m_frameCount * m_frameDuration;
    
    AMediaCodec_queueInputBuffer(m_codec, inputBufferIdx, 0, yuvSize, presentationTime, 0);
    m_frameCount++;
    
    // Write encoded data
    writeEncodedData();
    
    return true;
}

bool VideoEncoder::writeEncodedData() {
    AMediaCodecBufferInfo info;
    ssize_t outputBufferIdx = AMediaCodec_dequeueOutputBuffer(m_codec, &info, 0);
    
    while (outputBufferIdx >= 0) {
        if (info.flags & AMEDIACODEC_BUFFER_FLAG_CODEC_CONFIG) {
            // This is codec config data (SPS/PPS), add track to muxer
            if (!m_muxerStarted) {
                AMediaFormat* outputFormat = AMediaCodec_getOutputFormat(m_codec);
                m_videoTrackIndex = AMediaMuxer_addTrack(m_muxer, outputFormat);
                AMediaMuxer_start(m_muxer);
                m_muxerStarted = true;
                AMediaFormat_delete(outputFormat);
                LOGI("Muxer started, video track index: %d", m_videoTrackIndex);
            }
        } else if (m_muxerStarted && info.size > 0) {
            size_t outputBufferSize;
            uint8_t* outputBuffer = AMediaCodec_getOutputBuffer(m_codec, outputBufferIdx, &outputBufferSize);
            
            AMediaMuxer_writeSampleData(m_muxer, m_videoTrackIndex, outputBuffer, &info);
        }
        
        AMediaCodec_releaseOutputBuffer(m_codec, outputBufferIdx, false);
        outputBufferIdx = AMediaCodec_dequeueOutputBuffer(m_codec, &info, 0);
    }
    
    if (outputBufferIdx == AMEDIACODEC_INFO_OUTPUT_FORMAT_CHANGED) {
        AMediaFormat* newFormat = AMediaCodec_getOutputFormat(m_codec);
        LOGI("Output format changed");
        if (!m_muxerStarted) {
            m_videoTrackIndex = AMediaMuxer_addTrack(m_muxer, newFormat);
            AMediaMuxer_start(m_muxer);
            m_muxerStarted = true;
        }
        AMediaFormat_delete(newFormat);
    }
    
    return true;
}

bool VideoEncoder::finalize() {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    if (!m_initialized || !m_codec) {
        return false;
    }
    
    // Signal end of stream
    ssize_t inputBufferIdx = AMediaCodec_dequeueInputBuffer(m_codec, 10000);
    if (inputBufferIdx >= 0) {
        AMediaCodec_queueInputBuffer(m_codec, inputBufferIdx, 0, 0, 0, 
            AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM);
    }
    
    // Drain encoder
    AMediaCodecBufferInfo info;
    while (true) {
        ssize_t outputBufferIdx = AMediaCodec_dequeueOutputBuffer(m_codec, &info, 10000);
        
        if (outputBufferIdx >= 0) {
            if (info.flags & AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM) {
                AMediaCodec_releaseOutputBuffer(m_codec, outputBufferIdx, false);
                break;
            }
            
            if (m_muxerStarted && info.size > 0) {
                size_t outputBufferSize;
                uint8_t* outputBuffer = AMediaCodec_getOutputBuffer(m_codec, outputBufferIdx, &outputBufferSize);
                AMediaMuxer_writeSampleData(m_muxer, m_videoTrackIndex, outputBuffer, &info);
            }
            
            AMediaCodec_releaseOutputBuffer(m_codec, outputBufferIdx, false);
        } else if (outputBufferIdx == AMEDIACODEC_INFO_TRY_AGAIN_LATER) {
            break;
        }
    }
    
    // Stop muxer
    if (m_muxerStarted) {
        AMediaMuxer_stop(m_muxer);
        m_muxerStarted = false;
    }
    
    LOGI("Encoding finalized, total frames: %lld", (long long)m_frameCount);
    return true;
}

}  // namespace videoeditor
