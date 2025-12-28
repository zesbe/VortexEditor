#include "timeline.h"

namespace videoeditor {

Timeline::Timeline()
    : m_nextClipId(1)
    , m_trackCount(3)  // Default 3 tracks (video, overlay, audio)
    , m_duration(0) {
    LOGI("Timeline created");
}

Timeline::~Timeline() {
    clear();
    LOGI("Timeline destroyed");
}

void Timeline::clear() {
    std::lock_guard<std::mutex> lock(m_mutex);
    m_clips.clear();
    m_duration = 0;
    m_nextClipId = 1;
    LOGI("Timeline cleared");
}

bool Timeline::addClip(const std::string& filePath, int trackIndex, int64_t position) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    TimelineClip clip;
    clip.id = getNextClipId();
    clip.filePath = filePath;
    clip.trackIndex = trackIndex;
    clip.startTime = position;
    clip.trimStart = 0;
    clip.trimEnd = 0;
    clip.speed = 1.0f;
    clip.volume = 1.0f;
    
    // TODO: Get actual duration from video file
    // For now, use placeholder
    clip.sourceDuration = 10000000;  // 10 seconds in microseconds
    clip.duration = clip.sourceDuration;
    
    m_clips[clip.id] = clip;
    
    recalculateDuration();
    
    LOGI("Added clip %d: %s at track %d, position %lld", 
        clip.id, filePath.c_str(), trackIndex, (long long)position);
    return true;
}

bool Timeline::removeClip(int clipId) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    auto it = m_clips.find(clipId);
    if (it == m_clips.end()) {
        return false;
    }
    
    m_clips.erase(it);
    recalculateDuration();
    
    LOGI("Removed clip %d", clipId);
    return true;
}

bool Timeline::moveClip(int clipId, int trackIndex, int64_t position) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    auto it = m_clips.find(clipId);
    if (it == m_clips.end()) {
        return false;
    }
    
    it->second.trackIndex = trackIndex;
    it->second.startTime = position;
    
    recalculateDuration();
    
    LOGI("Moved clip %d to track %d, position %lld", clipId, trackIndex, (long long)position);
    return true;
}

bool Timeline::trimClip(int clipId, int64_t trimStart, int64_t trimEnd) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    auto it = m_clips.find(clipId);
    if (it == m_clips.end()) {
        return false;
    }
    
    TimelineClip& clip = it->second;
    
    // Validate trim values
    int64_t maxTrim = clip.sourceDuration;
    trimStart = std::max(int64_t(0), std::min(trimStart, maxTrim));
    trimEnd = std::max(int64_t(0), std::min(trimEnd, maxTrim - trimStart));
    
    clip.trimStart = trimStart;
    clip.trimEnd = trimEnd;
    clip.duration = (clip.sourceDuration - trimStart - trimEnd) / clip.speed;
    
    recalculateDuration();
    
    LOGI("Trimmed clip %d: start=%lld, end=%lld, new duration=%lld", 
        clipId, (long long)trimStart, (long long)trimEnd, (long long)clip.duration);
    return true;
}

bool Timeline::splitClip(int clipId, int64_t position) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    auto it = m_clips.find(clipId);
    if (it == m_clips.end()) {
        return false;
    }
    
    TimelineClip& originalClip = it->second;
    
    // Check if position is within clip
    if (position <= originalClip.startTime || 
        position >= originalClip.startTime + originalClip.duration) {
        return false;
    }
    
    // Calculate split point relative to source
    int64_t splitOffset = (position - originalClip.startTime) * originalClip.speed;
    int64_t splitInSource = originalClip.trimStart + splitOffset;
    
    // Create second clip
    TimelineClip newClip = originalClip;
    newClip.id = getNextClipId();
    newClip.startTime = position;
    newClip.trimStart = splitInSource;
    newClip.duration = originalClip.duration - (position - originalClip.startTime);
    
    // Modify original clip
    originalClip.trimEnd = originalClip.sourceDuration - splitInSource;
    originalClip.duration = position - originalClip.startTime;
    
    m_clips[newClip.id] = newClip;
    
    LOGI("Split clip %d at %lld, created new clip %d", clipId, (long long)position, newClip.id);
    return true;
}

bool Timeline::setClipSpeed(int clipId, float speed) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    auto it = m_clips.find(clipId);
    if (it == m_clips.end()) {
        return false;
    }
    
    speed = std::max(0.1f, std::min(10.0f, speed));  // Limit speed range
    
    TimelineClip& clip = it->second;
    int64_t sourceDuration = clip.sourceDuration - clip.trimStart - clip.trimEnd;
    clip.speed = speed;
    clip.duration = static_cast<int64_t>(sourceDuration / speed);
    
    recalculateDuration();
    
    LOGI("Set clip %d speed to %f, new duration: %lld", clipId, speed, (long long)clip.duration);
    return true;
}

bool Timeline::setClipVolume(int clipId, float volume) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    auto it = m_clips.find(clipId);
    if (it == m_clips.end()) {
        return false;
    }
    
    it->second.volume = std::max(0.0f, std::min(2.0f, volume));  // Allow up to 2x volume
    
    LOGI("Set clip %d volume to %f", clipId, volume);
    return true;
}

TimelineClip* Timeline::getClip(int clipId) {
    auto it = m_clips.find(clipId);
    return it != m_clips.end() ? &it->second : nullptr;
}

std::vector<TimelineClip> Timeline::getClipsAtPosition(int64_t position) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    std::vector<TimelineClip> result;
    
    for (const auto& pair : m_clips) {
        const TimelineClip& clip = pair.second;
        if (position >= clip.startTime && position < clip.startTime + clip.duration) {
            result.push_back(clip);
        }
    }
    
    // Sort by track index
    std::sort(result.begin(), result.end(), 
        [](const TimelineClip& a, const TimelineClip& b) {
            return a.trackIndex < b.trackIndex;
        });
    
    return result;
}

std::vector<TimelineClip> Timeline::getClipsInRange(int64_t start, int64_t end) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    std::vector<TimelineClip> result;
    
    for (const auto& pair : m_clips) {
        const TimelineClip& clip = pair.second;
        int64_t clipEnd = clip.startTime + clip.duration;
        
        // Check if clip overlaps with range
        if (clip.startTime < end && clipEnd > start) {
            result.push_back(clip);
        }
    }
    
    return result;
}

std::vector<TimelineClip> Timeline::getAllClips() const {
    std::vector<TimelineClip> result;
    for (const auto& pair : m_clips) {
        result.push_back(pair.second);
    }
    return result;
}

int64_t Timeline::getDuration() const {
    return m_duration;
}

void Timeline::recalculateDuration() {
    m_duration = 0;
    
    for (const auto& pair : m_clips) {
        const TimelineClip& clip = pair.second;
        int64_t clipEnd = clip.startTime + clip.duration;
        if (clipEnd > m_duration) {
            m_duration = clipEnd;
        }
    }
    
    LOGD("Timeline duration recalculated: %lld us", (long long)m_duration);
}

}  // namespace videoeditor
