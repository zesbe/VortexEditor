#ifndef VIDEO_EDITOR_AUDIO_ENGINE_H
#define VIDEO_EDITOR_AUDIO_ENGINE_H

#include "common.h"
#include <media/NdkMediaCodec.h>
#include <media/NdkMediaExtractor.h>
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include <unordered_map>

namespace videoeditor {

struct AudioTrack {
    int id;
    std::string filePath;
    int64_t position;
    int64_t duration;
    float volume;
    bool isMuted;
    std::vector<int16_t> samples;
    int sampleRate;
    int channels;
};

class AudioEngine {
public:
    AudioEngine();
    ~AudioEngine();

    bool initialize();
    void release();

    // Playback control
    void play();
    void pause();
    void stop();
    void seekTo(int64_t position);

    // Track management
    bool addTrack(const std::string& filePath, int64_t position);
    bool removeTrack(int trackId);
    bool setVolume(int trackId, float volume);
    bool setMute(int trackId, bool muted);

    // Master volume
    void setMasterVolume(float volume);
    float getMasterVolume() const { return m_masterVolume; }

    // Get mixed audio at position
    AudioSample getMixedAudio(int64_t position, int64_t duration);

    // Audio extraction
    bool extractAudio(const std::string& videoPath, std::vector<int16_t>& outSamples, 
                     int& outSampleRate, int& outChannels);

private:
    bool initOpenSL();
    void releaseOpenSL();
    bool decodeAudioFile(const std::string& filePath, AudioTrack& track);
    void mixTracks(int64_t position, int16_t* outputBuffer, size_t numSamples);

    // OpenSL ES objects
    SLObjectItf m_engineObject;
    SLEngineItf m_engineEngine;
    SLObjectItf m_outputMixObject;
    SLObjectItf m_playerObject;
    SLPlayItf m_playerPlay;
    SLAndroidSimpleBufferQueueItf m_playerBufferQueue;
    SLVolumeItf m_playerVolume;

    // Audio tracks
    std::unordered_map<int, AudioTrack> m_tracks;
    int m_nextTrackId;

    // State
    float m_masterVolume;
    int64_t m_currentPosition;
    bool m_initialized;
    bool m_playing;
    
    // Output buffer
    std::vector<int16_t> m_outputBuffer;
    static const int OUTPUT_BUFFER_SIZE = 4096;
    static const int SAMPLE_RATE = 44100;
    static const int CHANNELS = 2;

    std::mutex m_mutex;
};

}  // namespace videoeditor

#endif  // VIDEO_EDITOR_AUDIO_ENGINE_H
