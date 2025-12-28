#ifndef VIDEO_EDITOR_VIDEO_ENCODER_H
#define VIDEO_EDITOR_VIDEO_ENCODER_H

#include "common.h"
#include <media/NdkMediaCodec.h>
#include <media/NdkMediaMuxer.h>
#include <media/NdkMediaFormat.h>

namespace videoeditor {

class VideoEncoder {
public:
    VideoEncoder();
    ~VideoEncoder();

    bool initialize();
    void release();

    bool configure(const ExportSettings& settings);
    bool encodeFrame(const VideoFrame& frame);
    bool finalize();

    void setProgressCallback(ProgressCallback callback) { m_progressCallback = callback; }

private:
    bool writeEncodedData();

    AMediaCodec* m_codec;
    AMediaMuxer* m_muxer;
    AMediaFormat* m_format;
    
    int m_videoTrackIndex;
    int m_width;
    int m_height;
    int m_fps;
    int m_bitrate;
    int64_t m_frameCount;
    int64_t m_frameDuration;
    
    std::string m_outputPath;
    int m_outputFd;
    
    bool m_initialized;
    bool m_muxerStarted;
    
    ProgressCallback m_progressCallback;
    std::mutex m_mutex;
};

}  // namespace videoeditor

#endif  // VIDEO_EDITOR_VIDEO_ENCODER_H
