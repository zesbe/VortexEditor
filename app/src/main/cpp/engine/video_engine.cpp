#include "video_engine.h"
#include <chrono>

namespace videoeditor {

VideoEngine::VideoEngine()
    : m_projectWidth(1920)
    , m_projectHeight(1080)
    , m_projectFps(30)
    , m_previewSurface(nullptr)
    , m_initialized(false)
    , m_playing(false)
    , m_exporting(false)
    , m_currentPosition(0)
    , m_progressCallback(nullptr)
    , m_errorCallback(nullptr) {
    LOGI("VideoEngine created");
}

VideoEngine::~VideoEngine() {
    release();
    LOGI("VideoEngine destroyed");
}

bool VideoEngine::initialize() {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    if (m_initialized) {
        LOGW("VideoEngine already initialized");
        return true;
    }

    try {
        // Initialize thread pool (4 threads for parallel processing)
        m_threadPool = std::make_unique<ThreadPool>(4);
        
        // Initialize timeline
        m_timeline = std::make_unique<Timeline>();
        
        // Initialize decoder
        m_decoder = std::make_unique<VideoDecoder>();
        if (!m_decoder->initialize()) {
            LOGE("Failed to initialize video decoder");
            return false;
        }
        
        // Initialize encoder
        m_encoder = std::make_unique<VideoEncoder>();
        if (!m_encoder->initialize()) {
            LOGE("Failed to initialize video encoder");
            return false;
        }
        
        // Initialize audio engine
        m_audioEngine = std::make_unique<AudioEngine>();
        if (!m_audioEngine->initialize()) {
            LOGE("Failed to initialize audio engine");
            return false;
        }
        
        // Initialize filter manager
        m_filterManager = std::make_unique<FilterManager>();
        if (!m_filterManager->initialize()) {
            LOGE("Failed to initialize filter manager");
            return false;
        }
        
        // Initialize frame buffer
        m_frameBuffer = std::make_unique<FrameBuffer>(m_projectWidth, m_projectHeight);
        
        m_initialized = true;
        LOGI("VideoEngine initialized successfully");
        return true;
        
    } catch (const std::exception& e) {
        LOGE("Exception during initialization: %s", e.what());
        if (m_errorCallback) {
            m_errorCallback(-1, std::string("Initialization failed: ") + e.what());
        }
        return false;
    }
}

void VideoEngine::release() {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    if (!m_initialized) {
        return;
    }

    stop();

    m_frameBuffer.reset();
    m_filterManager.reset();
    m_audioEngine.reset();
    m_encoder.reset();
    m_decoder.reset();
    m_timeline.reset();
    m_threadPool.reset();

    if (m_previewSurface) {
        ANativeWindow_release(m_previewSurface);
        m_previewSurface = nullptr;
    }

    m_initialized = false;
    LOGI("VideoEngine released");
}

bool VideoEngine::createProject(int width, int height, int fps) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    m_projectWidth = width;
    m_projectHeight = height;
    m_projectFps = fps;
    
    // Reinitialize frame buffer with new dimensions
    m_frameBuffer = std::make_unique<FrameBuffer>(width, height);
    
    // Reset timeline
    m_timeline->clear();
    m_currentPosition = 0;
    
    LOGI("Project created: %dx%d @ %d fps", width, height, fps);
    return true;
}

bool VideoEngine::loadProject(const std::string& projectPath) {
    // TODO: Implement project loading from JSON/binary format
    LOGI("Loading project from: %s", projectPath.c_str());
    return true;
}

bool VideoEngine::saveProject(const std::string& projectPath) {
    // TODO: Implement project saving to JSON/binary format
    LOGI("Saving project to: %s", projectPath.c_str());
    return true;
}

// Timeline operations
bool VideoEngine::addClip(const std::string& filePath, int trackIndex, int64_t position) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    if (!m_timeline) {
        LOGE("Timeline not initialized");
        return false;
    }
    
    return m_timeline->addClip(filePath, trackIndex, position);
}

bool VideoEngine::removeClip(int clipId) {
    std::lock_guard<std::mutex> lock(m_mutex);
    return m_timeline ? m_timeline->removeClip(clipId) : false;
}

bool VideoEngine::moveClip(int clipId, int trackIndex, int64_t position) {
    std::lock_guard<std::mutex> lock(m_mutex);
    return m_timeline ? m_timeline->moveClip(clipId, trackIndex, position) : false;
}

bool VideoEngine::trimClip(int clipId, int64_t startTrim, int64_t endTrim) {
    std::lock_guard<std::mutex> lock(m_mutex);
    return m_timeline ? m_timeline->trimClip(clipId, startTrim, endTrim) : false;
}

bool VideoEngine::splitClip(int clipId, int64_t position) {
    std::lock_guard<std::mutex> lock(m_mutex);
    return m_timeline ? m_timeline->splitClip(clipId, position) : false;
}

bool VideoEngine::setClipSpeed(int clipId, float speed) {
    std::lock_guard<std::mutex> lock(m_mutex);
    return m_timeline ? m_timeline->setClipSpeed(clipId, speed) : false;
}

bool VideoEngine::setClipVolume(int clipId, float volume) {
    std::lock_guard<std::mutex> lock(m_mutex);
    return m_timeline ? m_timeline->setClipVolume(clipId, volume) : false;
}

// Playback controls
void VideoEngine::play() {
    if (m_playing) return;
    
    m_playing = true;
    m_renderThread = std::thread(&VideoEngine::renderLoop, this);
    
    if (m_audioEngine) {
        m_audioEngine->play();
    }
    
    LOGI("Playback started");
}

void VideoEngine::pause() {
    if (!m_playing) return;
    
    m_playing = false;
    m_condition.notify_all();
    
    if (m_renderThread.joinable()) {
        m_renderThread.join();
    }
    
    if (m_audioEngine) {
        m_audioEngine->pause();
    }
    
    LOGI("Playback paused");
}

void VideoEngine::stop() {
    pause();
    m_currentPosition = 0;
    
    if (m_audioEngine) {
        m_audioEngine->stop();
    }
    
    LOGI("Playback stopped");
}

void VideoEngine::seekTo(int64_t position) {
    m_currentPosition = position;
    
    if (m_audioEngine) {
        m_audioEngine->seekTo(position);
    }
    
    updatePreview();
    LOGI("Seeked to position: %lld", (long long)position);
}

int64_t VideoEngine::getCurrentPosition() const {
    return m_currentPosition;
}

int64_t VideoEngine::getDuration() const {
    return m_timeline ? m_timeline->getDuration() : 0;
}

bool VideoEngine::isPlaying() const {
    return m_playing;
}

// Preview
VideoFrame VideoEngine::getPreviewFrame(int64_t position) {
    VideoFrame frame;
    frame.width = m_projectWidth;
    frame.height = m_projectHeight;
    frame.format = PixelFormat::RGBA;
    frame.timestamp_us = position;
    frame.data.resize(frame.dataSize());
    
    if (m_timeline && m_decoder) {
        // Get clips at this position
        auto clips = m_timeline->getClipsAtPosition(position);
        
        for (const auto& clip : clips) {
            // Decode frame from clip
            VideoFrame clipFrame = m_decoder->decodeFrame(clip.filePath, 
                position - clip.startTime + clip.trimStart);
            
            // Apply filters
            if (m_filterManager) {
                m_filterManager->applyFilters(clipFrame, clip.filePath);
            }
            
            // Composite onto main frame
            m_frameBuffer->composite(frame, clipFrame, clip);
        }
    }
    
    return frame;
}

void VideoEngine::setPreviewSurface(ANativeWindow* surface) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    if (m_previewSurface) {
        ANativeWindow_release(m_previewSurface);
    }
    
    m_previewSurface = surface;
    
    if (surface) {
        ANativeWindow_acquire(surface);
    }
    
    LOGI("Preview surface set");
}

// Effects & Filters
bool VideoEngine::addFilter(int clipId, const std::string& filterType, const EffectParams& params) {
    if (!m_filterManager) return false;
    return m_filterManager->addFilter(clipId, filterType, params);
}

bool VideoEngine::removeFilter(int clipId, int filterId) {
    if (!m_filterManager) return false;
    return m_filterManager->removeFilter(clipId, filterId);
}

bool VideoEngine::updateFilter(int clipId, int filterId, const EffectParams& params) {
    if (!m_filterManager) return false;
    return m_filterManager->updateFilter(clipId, filterId, params);
}

// Export
bool VideoEngine::exportVideo(const ExportSettings& settings, ProgressCallback progressCallback) {
    if (m_exporting) {
        LOGW("Export already in progress");
        return false;
    }
    
    m_exporting = true;
    m_progressCallback = progressCallback;
    
    LOGI("Starting export: %s (%dx%d @ %d fps)", 
        settings.outputPath.c_str(), settings.width, settings.height, settings.fps);
    
    // Configure encoder
    if (!m_encoder->configure(settings)) {
        LOGE("Failed to configure encoder");
        m_exporting = false;
        return false;
    }
    
    // Start encoding in background thread
    m_threadPool->enqueue([this, settings]() {
        int64_t duration = getDuration();
        int64_t frameInterval = 1000000 / settings.fps;  // microseconds per frame
        int64_t totalFrames = duration / frameInterval;
        int64_t frameCount = 0;
        
        for (int64_t pos = 0; pos < duration && m_exporting; pos += frameInterval) {
            // Get frame at position
            VideoFrame frame = getPreviewFrame(pos);
            
            // Encode frame
            m_encoder->encodeFrame(frame);
            
            // Update progress
            frameCount++;
            float progress = static_cast<float>(frameCount) / totalFrames;
            
            if (m_progressCallback) {
                m_progressCallback(progress, "Encoding...");
            }
        }
        
        // Finalize encoding
        m_encoder->finalize();
        
        m_exporting = false;
        
        if (m_progressCallback) {
            m_progressCallback(1.0f, "Export complete");
        }
        
        LOGI("Export completed");
    });
    
    return true;
}

void VideoEngine::cancelExport() {
    m_exporting = false;
    LOGI("Export cancelled");
}

// Private methods
void VideoEngine::renderLoop() {
    auto lastFrameTime = std::chrono::high_resolution_clock::now();
    int64_t frameInterval = 1000000 / m_projectFps;  // microseconds
    
    while (m_playing) {
        auto currentTime = std::chrono::high_resolution_clock::now();
        auto elapsed = std::chrono::duration_cast<std::chrono::microseconds>(
            currentTime - lastFrameTime).count();
        
        if (elapsed >= frameInterval) {
            // Update position
            m_currentPosition += elapsed;
            
            // Check if reached end
            if (m_currentPosition >= getDuration()) {
                m_playing = false;
                m_currentPosition = getDuration();
                break;
            }
            
            // Render frame
            updatePreview();
            
            lastFrameTime = currentTime;
        }
        
        // Sleep to prevent busy waiting
        std::this_thread::sleep_for(std::chrono::microseconds(1000));
    }
}

void VideoEngine::updatePreview() {
    if (!m_previewSurface) return;
    
    VideoFrame frame = getPreviewFrame(m_currentPosition);
    
    ANativeWindow_Buffer buffer;
    if (ANativeWindow_lock(m_previewSurface, &buffer, nullptr) == 0) {
        // Copy frame data to surface buffer
        uint8_t* dst = static_cast<uint8_t*>(buffer.bits);
        const uint8_t* src = frame.data.data();
        
        int srcStride = frame.width * 4;
        int dstStride = buffer.stride * 4;
        
        for (int y = 0; y < frame.height && y < buffer.height; y++) {
            memcpy(dst + y * dstStride, src + y * srcStride, 
                   std::min(srcStride, dstStride));
        }
        
        ANativeWindow_unlockAndPost(m_previewSurface);
    }
}

void VideoEngine::processFrame(VideoFrame& frame) {
    // Apply any global effects/processing here
}

// Text overlay
int VideoEngine::addText(const std::string& text, int64_t startTime, int64_t duration,
                         float x, float y, float fontSize, uint32_t color) {
    // TODO: Implement text overlay
    LOGI("Adding text: %s at (%f, %f)", text.c_str(), x, y);
    return 0;
}

bool VideoEngine::updateText(int textId, const std::string& text) {
    // TODO: Implement
    return true;
}

bool VideoEngine::removeText(int textId) {
    // TODO: Implement
    return true;
}

// Transitions
bool VideoEngine::addTransition(int clipId1, int clipId2, const std::string& transitionType, int64_t duration) {
    // TODO: Implement transitions
    LOGI("Adding transition: %s between clips %d and %d", transitionType.c_str(), clipId1, clipId2);
    return true;
}

bool VideoEngine::removeTransition(int transitionId) {
    // TODO: Implement
    return true;
}

// Audio
bool VideoEngine::addAudioTrack(const std::string& filePath, int64_t position) {
    if (!m_audioEngine) return false;
    return m_audioEngine->addTrack(filePath, position);
}

bool VideoEngine::removeAudioTrack(int audioId) {
    if (!m_audioEngine) return false;
    return m_audioEngine->removeTrack(audioId);
}

bool VideoEngine::setAudioVolume(int audioId, float volume) {
    if (!m_audioEngine) return false;
    return m_audioEngine->setVolume(audioId, volume);
}

bool VideoEngine::addVoiceover(const std::string& filePath, int64_t position) {
    return addAudioTrack(filePath, position);
}

}  // namespace videoeditor
