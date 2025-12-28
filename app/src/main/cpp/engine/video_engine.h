#ifndef VIDEO_EDITOR_VIDEO_ENGINE_H
#define VIDEO_EDITOR_VIDEO_ENGINE_H

#include "common.h"
#include "video_decoder.h"
#include "video_encoder.h"
#include "audio_engine.h"
#include "frame_buffer.h"
#include "timeline.h"
#include "../filters/filter_manager.h"
#include "../utils/thread_pool.h"

namespace videoeditor {

class VideoEngine {
public:
    VideoEngine();
    ~VideoEngine();

    // Initialize/Cleanup
    bool initialize();
    void release();

    // Project management
    bool createProject(int width, int height, int fps);
    bool loadProject(const std::string& projectPath);
    bool saveProject(const std::string& projectPath);

    // Timeline operations
    bool addClip(const std::string& filePath, int trackIndex, int64_t position);
    bool removeClip(int clipId);
    bool moveClip(int clipId, int trackIndex, int64_t position);
    bool trimClip(int clipId, int64_t startTrim, int64_t endTrim);
    bool splitClip(int clipId, int64_t position);
    bool setClipSpeed(int clipId, float speed);
    bool setClipVolume(int clipId, float volume);

    // Playback
    void play();
    void pause();
    void stop();
    void seekTo(int64_t position);
    int64_t getCurrentPosition() const;
    int64_t getDuration() const;
    bool isPlaying() const;

    // Preview
    VideoFrame getPreviewFrame(int64_t position);
    void setPreviewSurface(ANativeWindow* surface);

    // Effects & Filters
    bool addFilter(int clipId, const std::string& filterType, const EffectParams& params);
    bool removeFilter(int clipId, int filterId);
    bool updateFilter(int clipId, int filterId, const EffectParams& params);

    // Transitions
    bool addTransition(int clipId1, int clipId2, const std::string& transitionType, int64_t duration);
    bool removeTransition(int transitionId);

    // Text overlay
    int addText(const std::string& text, int64_t startTime, int64_t duration,
                float x, float y, float fontSize, uint32_t color);
    bool updateText(int textId, const std::string& text);
    bool removeText(int textId);

    // Audio
    bool addAudioTrack(const std::string& filePath, int64_t position);
    bool removeAudioTrack(int audioId);
    bool setAudioVolume(int audioId, float volume);
    bool addVoiceover(const std::string& filePath, int64_t position);

    // Export
    bool exportVideo(const ExportSettings& settings, ProgressCallback progressCallback);
    void cancelExport();

    // Getters
    int getProjectWidth() const { return m_projectWidth; }
    int getProjectHeight() const { return m_projectHeight; }
    int getProjectFps() const { return m_projectFps; }

    // Callbacks
    void setProgressCallback(ProgressCallback callback) { m_progressCallback = callback; }
    void setErrorCallback(ErrorCallback callback) { m_errorCallback = callback; }

private:
    void renderLoop();
    void processFrame(VideoFrame& frame);
    void updatePreview();

    // Project settings
    int m_projectWidth;
    int m_projectHeight;
    int m_projectFps;

    // Components
    std::unique_ptr<Timeline> m_timeline;
    std::unique_ptr<VideoDecoder> m_decoder;
    std::unique_ptr<VideoEncoder> m_encoder;
    std::unique_ptr<AudioEngine> m_audioEngine;
    std::unique_ptr<FilterManager> m_filterManager;
    std::unique_ptr<FrameBuffer> m_frameBuffer;
    std::unique_ptr<ThreadPool> m_threadPool;

    // Preview surface
    ANativeWindow* m_previewSurface;

    // State
    std::atomic<bool> m_initialized;
    std::atomic<bool> m_playing;
    std::atomic<bool> m_exporting;
    std::atomic<int64_t> m_currentPosition;

    // Threading
    std::thread m_renderThread;
    std::mutex m_mutex;
    std::condition_variable m_condition;

    // Callbacks
    ProgressCallback m_progressCallback;
    ErrorCallback m_errorCallback;
};

}  // namespace videoeditor

#endif  // VIDEO_EDITOR_VIDEO_ENGINE_H
