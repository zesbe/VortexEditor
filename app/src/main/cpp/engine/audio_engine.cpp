#include "audio_engine.h"

namespace videoeditor {

AudioEngine::AudioEngine()
    : m_engineObject(nullptr)
    , m_engineEngine(nullptr)
    , m_outputMixObject(nullptr)
    , m_playerObject(nullptr)
    , m_playerPlay(nullptr)
    , m_playerBufferQueue(nullptr)
    , m_playerVolume(nullptr)
    , m_nextTrackId(1)
    , m_masterVolume(1.0f)
    , m_currentPosition(0)
    , m_initialized(false)
    , m_playing(false) {
    LOGI("AudioEngine created");
}

AudioEngine::~AudioEngine() {
    release();
    LOGI("AudioEngine destroyed");
}

bool AudioEngine::initialize() {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    if (m_initialized) {
        return true;
    }
    
    if (!initOpenSL()) {
        LOGE("Failed to initialize OpenSL ES");
        return false;
    }
    
    m_outputBuffer.resize(OUTPUT_BUFFER_SIZE * CHANNELS);
    m_initialized = true;
    
    LOGI("AudioEngine initialized");
    return true;
}

bool AudioEngine::initOpenSL() {
    SLresult result;
    
    // Create engine
    result = slCreateEngine(&m_engineObject, 0, nullptr, 0, nullptr, nullptr);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Failed to create OpenSL engine");
        return false;
    }
    
    result = (*m_engineObject)->Realize(m_engineObject, SL_BOOLEAN_FALSE);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Failed to realize OpenSL engine");
        return false;
    }
    
    result = (*m_engineObject)->GetInterface(m_engineObject, SL_IID_ENGINE, &m_engineEngine);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Failed to get OpenSL engine interface");
        return false;
    }
    
    // Create output mix
    result = (*m_engineEngine)->CreateOutputMix(m_engineEngine, &m_outputMixObject, 0, nullptr, nullptr);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Failed to create output mix");
        return false;
    }
    
    result = (*m_outputMixObject)->Realize(m_outputMixObject, SL_BOOLEAN_FALSE);
    if (result != SL_RESULT_SUCCESS) {
        LOGE("Failed to realize output mix");
        return false;
    }
    
    LOGI("OpenSL ES initialized successfully");
    return true;
}

void AudioEngine::release() {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    releaseOpenSL();
    m_tracks.clear();
    m_initialized = false;
    
    LOGI("AudioEngine released");
}

void AudioEngine::releaseOpenSL() {
    if (m_playerObject) {
        (*m_playerObject)->Destroy(m_playerObject);
        m_playerObject = nullptr;
    }
    
    if (m_outputMixObject) {
        (*m_outputMixObject)->Destroy(m_outputMixObject);
        m_outputMixObject = nullptr;
    }
    
    if (m_engineObject) {
        (*m_engineObject)->Destroy(m_engineObject);
        m_engineObject = nullptr;
    }
}

void AudioEngine::play() {
    std::lock_guard<std::mutex> lock(m_mutex);
    m_playing = true;
    LOGI("Audio playback started");
}

void AudioEngine::pause() {
    std::lock_guard<std::mutex> lock(m_mutex);
    m_playing = false;
    LOGI("Audio playback paused");
}

void AudioEngine::stop() {
    std::lock_guard<std::mutex> lock(m_mutex);
    m_playing = false;
    m_currentPosition = 0;
    LOGI("Audio playback stopped");
}

void AudioEngine::seekTo(int64_t position) {
    std::lock_guard<std::mutex> lock(m_mutex);
    m_currentPosition = position;
    LOGI("Audio seek to: %lld", (long long)position);
}

bool AudioEngine::addTrack(const std::string& filePath, int64_t position) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    AudioTrack track;
    track.id = m_nextTrackId++;
    track.filePath = filePath;
    track.position = position;
    track.volume = 1.0f;
    track.isMuted = false;
    
    if (!decodeAudioFile(filePath, track)) {
        LOGE("Failed to decode audio file: %s", filePath.c_str());
        return false;
    }
    
    m_tracks[track.id] = std::move(track);
    LOGI("Added audio track %d: %s", track.id, filePath.c_str());
    return true;
}

bool AudioEngine::removeTrack(int trackId) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    auto it = m_tracks.find(trackId);
    if (it == m_tracks.end()) {
        return false;
    }
    
    m_tracks.erase(it);
    LOGI("Removed audio track %d", trackId);
    return true;
}

bool AudioEngine::setVolume(int trackId, float volume) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    auto it = m_tracks.find(trackId);
    if (it == m_tracks.end()) {
        return false;
    }
    
    it->second.volume = std::max(0.0f, std::min(1.0f, volume));
    return true;
}

bool AudioEngine::setMute(int trackId, bool muted) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    auto it = m_tracks.find(trackId);
    if (it == m_tracks.end()) {
        return false;
    }
    
    it->second.isMuted = muted;
    return true;
}

void AudioEngine::setMasterVolume(float volume) {
    std::lock_guard<std::mutex> lock(m_mutex);
    m_masterVolume = std::max(0.0f, std::min(1.0f, volume));
}

bool AudioEngine::decodeAudioFile(const std::string& filePath, AudioTrack& track) {
    AMediaExtractor* extractor = AMediaExtractor_new();
    if (!extractor) {
        return false;
    }
    
    if (AMediaExtractor_setDataSource(extractor, filePath.c_str()) != AMEDIA_OK) {
        AMediaExtractor_delete(extractor);
        return false;
    }
    
    // Find audio track
    int numTracks = AMediaExtractor_getTrackCount(extractor);
    int audioTrackIndex = -1;
    AMediaFormat* format = nullptr;
    
    for (int i = 0; i < numTracks; i++) {
        format = AMediaExtractor_getTrackFormat(extractor, i);
        const char* mime;
        AMediaFormat_getString(format, AMEDIAFORMAT_KEY_MIME, &mime);
        
        if (strncmp(mime, "audio/", 6) == 0) {
            audioTrackIndex = i;
            AMediaFormat_getInt32(format, AMEDIAFORMAT_KEY_SAMPLE_RATE, &track.sampleRate);
            AMediaFormat_getInt32(format, AMEDIAFORMAT_KEY_CHANNEL_COUNT, &track.channels);
            AMediaFormat_getInt64(format, AMEDIAFORMAT_KEY_DURATION, &track.duration);
            break;
        }
        
        AMediaFormat_delete(format);
        format = nullptr;
    }
    
    if (audioTrackIndex < 0) {
        AMediaExtractor_delete(extractor);
        return false;
    }
    
    AMediaExtractor_selectTrack(extractor, audioTrackIndex);
    
    // Create decoder
    const char* mime;
    AMediaFormat_getString(format, AMEDIAFORMAT_KEY_MIME, &mime);
    AMediaCodec* codec = AMediaCodec_createDecoderByType(mime);
    
    if (!codec) {
        AMediaFormat_delete(format);
        AMediaExtractor_delete(extractor);
        return false;
    }
    
    AMediaCodec_configure(codec, format, nullptr, nullptr, 0);
    AMediaCodec_start(codec);
    
    // Decode all samples
    bool sawInputEOS = false;
    bool sawOutputEOS = false;
    
    while (!sawOutputEOS) {
        if (!sawInputEOS) {
            ssize_t inputBufferIdx = AMediaCodec_dequeueInputBuffer(codec, 10000);
            if (inputBufferIdx >= 0) {
                size_t inputBufferSize;
                uint8_t* inputBuffer = AMediaCodec_getInputBuffer(codec, inputBufferIdx, &inputBufferSize);
                
                ssize_t sampleSize = AMediaExtractor_readSampleData(extractor, inputBuffer, inputBufferSize);
                int64_t presentationTime = AMediaExtractor_getSampleTime(extractor);
                
                if (sampleSize < 0) {
                    sawInputEOS = true;
                    AMediaCodec_queueInputBuffer(codec, inputBufferIdx, 0, 0, 0, 
                        AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM);
                } else {
                    AMediaCodec_queueInputBuffer(codec, inputBufferIdx, 0, sampleSize, presentationTime, 0);
                    AMediaExtractor_advance(extractor);
                }
            }
        }
        
        AMediaCodecBufferInfo info;
        ssize_t outputBufferIdx = AMediaCodec_dequeueOutputBuffer(codec, &info, 10000);
        
        if (outputBufferIdx >= 0) {
            if (info.flags & AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM) {
                sawOutputEOS = true;
            }
            
            if (info.size > 0) {
                size_t outputBufferSize;
                uint8_t* outputBuffer = AMediaCodec_getOutputBuffer(codec, outputBufferIdx, &outputBufferSize);
                
                // Copy decoded samples
                int16_t* samples = reinterpret_cast<int16_t*>(outputBuffer);
                size_t numSamples = info.size / sizeof(int16_t);
                
                size_t oldSize = track.samples.size();
                track.samples.resize(oldSize + numSamples);
                memcpy(track.samples.data() + oldSize, samples, info.size);
            }
            
            AMediaCodec_releaseOutputBuffer(codec, outputBufferIdx, false);
        }
    }
    
    AMediaCodec_stop(codec);
    AMediaCodec_delete(codec);
    AMediaFormat_delete(format);
    AMediaExtractor_delete(extractor);
    
    LOGI("Decoded audio: %zu samples, %d Hz, %d channels", 
        track.samples.size(), track.sampleRate, track.channels);
    return true;
}

AudioSample AudioEngine::getMixedAudio(int64_t position, int64_t duration) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    AudioSample output;
    output.sampleRate = SAMPLE_RATE;
    output.channels = CHANNELS;
    output.timestamp_us = position;
    
    // Calculate number of samples needed
    size_t numSamples = (duration * SAMPLE_RATE / 1000000) * CHANNELS;
    output.data.resize(numSamples, 0);
    
    // Mix all tracks
    for (const auto& pair : m_tracks) {
        const AudioTrack& track = pair.second;
        
        if (track.isMuted || track.volume <= 0.0f) {
            continue;
        }
        
        // Calculate sample offset in track
        int64_t trackOffset = position - track.position;
        if (trackOffset < 0 || trackOffset >= track.duration) {
            continue;
        }
        
        // Convert time offset to sample offset
        size_t sampleOffset = (trackOffset * track.sampleRate / 1000000) * track.channels;
        
        // Mix samples
        float volume = track.volume * m_masterVolume;
        for (size_t i = 0; i < numSamples && sampleOffset + i < track.samples.size(); i++) {
            int32_t mixed = output.data[i] + static_cast<int32_t>(track.samples[sampleOffset + i] * volume);
            // Clamp to prevent clipping
            output.data[i] = static_cast<int16_t>(std::max(-32768, std::min(32767, mixed)));
        }
    }
    
    return output;
}

bool AudioEngine::extractAudio(const std::string& videoPath, std::vector<int16_t>& outSamples,
                               int& outSampleRate, int& outChannels) {
    AudioTrack tempTrack;
    if (!decodeAudioFile(videoPath, tempTrack)) {
        return false;
    }
    
    outSamples = std::move(tempTrack.samples);
    outSampleRate = tempTrack.sampleRate;
    outChannels = tempTrack.channels;
    return true;
}

}  // namespace videoeditor
