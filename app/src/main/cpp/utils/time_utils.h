#ifndef VIDEO_EDITOR_TIME_UTILS_H
#define VIDEO_EDITOR_TIME_UTILS_H

#include <cstdint>
#include <string>
#include <chrono>

namespace videoeditor {

class TimeUtils {
public:
    // Convert microseconds to formatted time string (HH:MM:SS.mmm)
    static std::string formatTime(int64_t microseconds);
    
    // Parse time string to microseconds
    static int64_t parseTime(const std::string& timeStr);
    
    // Get current time in microseconds
    static int64_t currentTimeMicros();
    
    // Convert between units
    static int64_t secondsToMicros(double seconds);
    static double microsToSeconds(int64_t micros);
    static int64_t framesToMicros(int frames, int fps);
    static int microsToFrames(int64_t micros, int fps);
};

inline std::string TimeUtils::formatTime(int64_t microseconds) {
    int64_t totalSeconds = microseconds / 1000000;
    int64_t millis = (microseconds % 1000000) / 1000;
    
    int hours = totalSeconds / 3600;
    int minutes = (totalSeconds % 3600) / 60;
    int seconds = totalSeconds % 60;
    
    char buffer[32];
    snprintf(buffer, sizeof(buffer), "%02d:%02d:%02d.%03d", 
             hours, minutes, seconds, static_cast<int>(millis));
    return buffer;
}

inline int64_t TimeUtils::parseTime(const std::string& timeStr) {
    int hours = 0, minutes = 0, seconds = 0, millis = 0;
    sscanf(timeStr.c_str(), "%d:%d:%d.%d", &hours, &minutes, &seconds, &millis);
    
    return (hours * 3600LL + minutes * 60LL + seconds) * 1000000LL + millis * 1000LL;
}

inline int64_t TimeUtils::currentTimeMicros() {
    return std::chrono::duration_cast<std::chrono::microseconds>(
        std::chrono::high_resolution_clock::now().time_since_epoch()
    ).count();
}

inline int64_t TimeUtils::secondsToMicros(double seconds) {
    return static_cast<int64_t>(seconds * 1000000.0);
}

inline double TimeUtils::microsToSeconds(int64_t micros) {
    return micros / 1000000.0;
}

inline int64_t TimeUtils::framesToMicros(int frames, int fps) {
    return static_cast<int64_t>(frames) * 1000000LL / fps;
}

inline int TimeUtils::microsToFrames(int64_t micros, int fps) {
    return static_cast<int>(micros * fps / 1000000LL);
}

}  // namespace videoeditor

#endif  // VIDEO_EDITOR_TIME_UTILS_H
