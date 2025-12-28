#include "video_decoder.h"

namespace videoeditor {

VideoDecoder::VideoDecoder()
    : m_initialized(false) {
    LOGI("VideoDecoder created");
}

VideoDecoder::~VideoDecoder() {
    release();
    LOGI("VideoDecoder destroyed");
}

bool VideoDecoder::initialize() {
    std::lock_guard<std::mutex> lock(m_mutex);
    m_initialized = true;
    LOGI("VideoDecoder initialized");
    return true;
}

void VideoDecoder::release() {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    for (auto& pair : m_contexts) {
        auto& ctx = pair.second;
        if (ctx->codec) {
            AMediaCodec_stop(ctx->codec);
            AMediaCodec_delete(ctx->codec);
        }
        if (ctx->extractor) {
            AMediaExtractor_delete(ctx->extractor);
        }
    }
    m_contexts.clear();
    
    m_initialized = false;
    LOGI("VideoDecoder released");
}

bool VideoDecoder::openFile(const std::string& filePath) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    if (m_contexts.find(filePath) != m_contexts.end()) {
        return true;  // Already open
    }
    
    auto ctx = std::make_unique<DecoderContext>();
    ctx->extractor = nullptr;
    ctx->codec = nullptr;
    ctx->format = nullptr;
    ctx->videoTrackIndex = -1;
    ctx->isConfigured = false;
    
    if (!configureDecoder(ctx.get(), filePath)) {
        return false;
    }
    
    m_contexts[filePath] = std::move(ctx);
    LOGI("Opened file: %s", filePath.c_str());
    return true;
}

void VideoDecoder::closeFile(const std::string& filePath) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    auto it = m_contexts.find(filePath);
    if (it != m_contexts.end()) {
        auto& ctx = it->second;
        if (ctx->codec) {
            AMediaCodec_stop(ctx->codec);
            AMediaCodec_delete(ctx->codec);
        }
        if (ctx->extractor) {
            AMediaExtractor_delete(ctx->extractor);
        }
        m_contexts.erase(it);
        LOGI("Closed file: %s", filePath.c_str());
    }
}

bool VideoDecoder::configureDecoder(DecoderContext* ctx, const std::string& filePath) {
    // Create extractor
    ctx->extractor = AMediaExtractor_new();
    if (!ctx->extractor) {
        LOGE("Failed to create media extractor");
        return false;
    }
    
    // Set data source
    media_status_t status = AMediaExtractor_setDataSource(ctx->extractor, filePath.c_str());
    if (status != AMEDIA_OK) {
        LOGE("Failed to set data source: %s", filePath.c_str());
        AMediaExtractor_delete(ctx->extractor);
        ctx->extractor = nullptr;
        return false;
    }
    
    // Find video track
    int numTracks = AMediaExtractor_getTrackCount(ctx->extractor);
    for (int i = 0; i < numTracks; i++) {
        AMediaFormat* format = AMediaExtractor_getTrackFormat(ctx->extractor, i);
        const char* mime;
        AMediaFormat_getString(format, AMEDIAFORMAT_KEY_MIME, &mime);
        
        if (strncmp(mime, "video/", 6) == 0) {
            ctx->videoTrackIndex = i;
            ctx->format = format;
            
            // Get video properties
            AMediaFormat_getInt32(format, AMEDIAFORMAT_KEY_WIDTH, &ctx->width);
            AMediaFormat_getInt32(format, AMEDIAFORMAT_KEY_HEIGHT, &ctx->height);
            AMediaFormat_getInt64(format, AMEDIAFORMAT_KEY_DURATION, &ctx->duration);
            
            int32_t frameRate = 30;
            AMediaFormat_getInt32(format, AMEDIAFORMAT_KEY_FRAME_RATE, &frameRate);
            ctx->fps = frameRate;
            
            // Select track
            AMediaExtractor_selectTrack(ctx->extractor, i);
            
            // Create codec
            ctx->codec = AMediaCodec_createDecoderByType(mime);
            if (!ctx->codec) {
                LOGE("Failed to create decoder for mime: %s", mime);
                return false;
            }
            
            // Configure codec
            status = AMediaCodec_configure(ctx->codec, format, nullptr, nullptr, 0);
            if (status != AMEDIA_OK) {
                LOGE("Failed to configure codec");
                AMediaCodec_delete(ctx->codec);
                ctx->codec = nullptr;
                return false;
            }
            
            // Start codec
            status = AMediaCodec_start(ctx->codec);
            if (status != AMEDIA_OK) {
                LOGE("Failed to start codec");
                AMediaCodec_delete(ctx->codec);
                ctx->codec = nullptr;
                return false;
            }
            
            ctx->isConfigured = true;
            LOGI("Video decoder configured: %dx%d @ %d fps, duration: %lld us", 
                ctx->width, ctx->height, ctx->fps, (long long)ctx->duration);
            return true;
        }
        
        AMediaFormat_delete(format);
    }
    
    LOGE("No video track found in file");
    return false;
}

VideoDecoder::DecoderContext* VideoDecoder::getContext(const std::string& filePath) {
    auto it = m_contexts.find(filePath);
    if (it == m_contexts.end()) {
        openFile(filePath);
        it = m_contexts.find(filePath);
    }
    return it != m_contexts.end() ? it->second.get() : nullptr;
}

VideoFrame VideoDecoder::decodeFrame(const std::string& filePath, int64_t timestamp) {
    VideoFrame frame;
    frame.format = PixelFormat::RGBA;
    
    std::lock_guard<std::mutex> lock(m_mutex);
    
    DecoderContext* ctx = getContext(filePath);
    if (!ctx || !ctx->isConfigured) {
        LOGE("Decoder not configured for file: %s", filePath.c_str());
        return frame;
    }
    
    // Seek to timestamp
    AMediaExtractor_seekTo(ctx->extractor, timestamp, AMEDIAEXTRACTOR_SEEK_CLOSEST_SYNC);
    
    // Decode frames until we reach the target timestamp
    bool gotFrame = false;
    while (!gotFrame) {
        // Get input buffer
        ssize_t inputBufferIdx = AMediaCodec_dequeueInputBuffer(ctx->codec, 10000);
        if (inputBufferIdx >= 0) {
            size_t inputBufferSize;
            uint8_t* inputBuffer = AMediaCodec_getInputBuffer(ctx->codec, inputBufferIdx, &inputBufferSize);
            
            // Read sample from extractor
            ssize_t sampleSize = AMediaExtractor_readSampleData(ctx->extractor, inputBuffer, inputBufferSize);
            int64_t presentationTime = AMediaExtractor_getSampleTime(ctx->extractor);
            
            if (sampleSize >= 0) {
                AMediaCodec_queueInputBuffer(ctx->codec, inputBufferIdx, 0, sampleSize, presentationTime, 0);
                AMediaExtractor_advance(ctx->extractor);
            } else {
                // End of stream
                AMediaCodec_queueInputBuffer(ctx->codec, inputBufferIdx, 0, 0, 0, AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM);
            }
        }
        
        // Get output buffer
        AMediaCodecBufferInfo info;
        ssize_t outputBufferIdx = AMediaCodec_dequeueOutputBuffer(ctx->codec, &info, 10000);
        
        if (outputBufferIdx >= 0) {
            // Check if this is the frame we want
            if (info.presentationTimeUs >= timestamp) {
                size_t outputBufferSize;
                uint8_t* outputBuffer = AMediaCodec_getOutputBuffer(ctx->codec, outputBufferIdx, &outputBufferSize);
                
                // Copy frame data
                frame.width = ctx->width;
                frame.height = ctx->height;
                frame.timestamp_us = info.presentationTimeUs;
                frame.data.resize(ctx->width * ctx->height * 4);
                
                // Convert YUV to RGBA (simplified - actual implementation needs proper conversion)
                // This is a placeholder - real implementation would use libyuv or similar
                memcpy(frame.data.data(), outputBuffer, std::min(frame.data.size(), outputBufferSize));
                
                gotFrame = true;
            }
            
            AMediaCodec_releaseOutputBuffer(ctx->codec, outputBufferIdx, false);
        } else if (outputBufferIdx == AMEDIACODEC_INFO_OUTPUT_FORMAT_CHANGED) {
            AMediaFormat* newFormat = AMediaCodec_getOutputFormat(ctx->codec);
            LOGI("Output format changed");
            AMediaFormat_delete(newFormat);
        }
        
        if (info.flags & AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM) {
            break;
        }
    }
    
    return frame;
}

bool VideoDecoder::seekTo(const std::string& filePath, int64_t timestamp) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    DecoderContext* ctx = getContext(filePath);
    if (!ctx || !ctx->extractor) {
        return false;
    }
    
    AMediaExtractor_seekTo(ctx->extractor, timestamp, AMEDIAEXTRACTOR_SEEK_CLOSEST_SYNC);
    
    // Flush codec
    if (ctx->codec) {
        AMediaCodec_flush(ctx->codec);
    }
    
    return true;
}

int VideoDecoder::getWidth(const std::string& filePath) {
    DecoderContext* ctx = getContext(filePath);
    return ctx ? ctx->width : 0;
}

int VideoDecoder::getHeight(const std::string& filePath) {
    DecoderContext* ctx = getContext(filePath);
    return ctx ? ctx->height : 0;
}

int64_t VideoDecoder::getDuration(const std::string& filePath) {
    DecoderContext* ctx = getContext(filePath);
    return ctx ? ctx->duration : 0;
}

int VideoDecoder::getFps(const std::string& filePath) {
    DecoderContext* ctx = getContext(filePath);
    return ctx ? ctx->fps : 30;
}

VideoFrame VideoDecoder::getThumbnail(const std::string& filePath, int64_t timestamp, int maxWidth, int maxHeight) {
    // Get full frame first
    VideoFrame frame = decodeFrame(filePath, timestamp);
    
    // TODO: Scale down to thumbnail size
    // For now, just return the full frame
    
    return frame;
}

}  // namespace videoeditor
