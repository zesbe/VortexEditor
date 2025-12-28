#ifndef VIDEO_EDITOR_VIDEO_DECODER_H
#define VIDEO_EDITOR_VIDEO_DECODER_H

#include "common.h"
#include <media/NdkMediaCodec.h>
#include <media/NdkMediaExtractor.h>
#include <media/NdkMediaFormat.h>
#include <unordered_map>

namespace videoeditor {

class VideoDecoder {
public:
    VideoDecoder();
    ~VideoDecoder();

    bool initialize();
    void release();

    // Open video file for decoding
    bool openFile(const std::string& filePath);
    void closeFile(const std::string& filePath);

    // Get video info
    int getWidth(const std::string& filePath);
    int getHeight(const std::string& filePath);
    int64_t getDuration(const std::string& filePath);
    int getFps(const std::string& filePath);

    // Decode frame at specific timestamp
    VideoFrame decodeFrame(const std::string& filePath, int64_t timestamp);

    // Seek to timestamp
    bool seekTo(const std::string& filePath, int64_t timestamp);

    // Get thumbnail
    VideoFrame getThumbnail(const std::string& filePath, int64_t timestamp, int maxWidth, int maxHeight);

private:
    struct DecoderContext {
        AMediaExtractor* extractor;
        AMediaCodec* codec;
        AMediaFormat* format;
        int videoTrackIndex;
        int width;
        int height;
        int64_t duration;
        int fps;
        bool isConfigured;
    };

    DecoderContext* getContext(const std::string& filePath);
    bool configureDecoder(DecoderContext* ctx, const std::string& filePath);
    VideoFrame extractFrame(DecoderContext* ctx);

    std::unordered_map<std::string, std::unique_ptr<DecoderContext>> m_contexts;
    std::mutex m_mutex;
    bool m_initialized;
};

}  // namespace videoeditor

#endif  // VIDEO_EDITOR_VIDEO_DECODER_H
