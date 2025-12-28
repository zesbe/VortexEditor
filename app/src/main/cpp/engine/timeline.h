#ifndef VIDEO_EDITOR_TIMELINE_H
#define VIDEO_EDITOR_TIMELINE_H

#include "common.h"
#include <map>

namespace videoeditor {

struct TimelineClip {
    int id;
    std::string filePath;
    int trackIndex;
    int64_t startTime;      // Position on timeline
    int64_t duration;       // Visible duration on timeline
    int64_t trimStart;      // Trim from start of source
    int64_t trimEnd;        // Trim from end of source
    int64_t sourceDuration; // Original source duration
    float speed;
    float volume;
    std::vector<EffectParams> effects;
};

class Timeline {
public:
    Timeline();
    ~Timeline();

    void clear();

    // Clip operations
    bool addClip(const std::string& filePath, int trackIndex, int64_t position);
    bool removeClip(int clipId);
    bool moveClip(int clipId, int trackIndex, int64_t position);
    bool trimClip(int clipId, int64_t trimStart, int64_t trimEnd);
    bool splitClip(int clipId, int64_t position);
    bool setClipSpeed(int clipId, float speed);
    bool setClipVolume(int clipId, float volume);

    // Get clips
    TimelineClip* getClip(int clipId);
    std::vector<TimelineClip> getClipsAtPosition(int64_t position);
    std::vector<TimelineClip> getClipsInRange(int64_t start, int64_t end);
    std::vector<TimelineClip> getAllClips() const;

    // Timeline info
    int64_t getDuration() const;
    int getTrackCount() const { return m_trackCount; }

private:
    int getNextClipId() { return m_nextClipId++; }
    void recalculateDuration();

    std::map<int, TimelineClip> m_clips;
    int m_nextClipId;
    int m_trackCount;
    int64_t m_duration;
    std::mutex m_mutex;
};

}  // namespace videoeditor

#endif  // VIDEO_EDITOR_TIMELINE_H
