#ifndef VIDEO_EDITOR_FILTER_MANAGER_H
#define VIDEO_EDITOR_FILTER_MANAGER_H

#include "common.h"
#include "color_filter.h"
#include "blur_filter.h"
#include <map>
#include <string>
#include <vector>

namespace videoeditor {

class FilterManager {
public:
    FilterManager();
    ~FilterManager();

    bool initialize();
    void release();

    // Filter operations
    bool addFilter(int clipId, const std::string& filterType, const EffectParams& params);
    bool removeFilter(int clipId, int filterId);
    bool updateFilter(int clipId, int filterId, const EffectParams& params);

    // Apply all filters to frame
    void applyFilters(VideoFrame& frame, const std::string& clipPath);

    // Available filter types
    std::vector<std::string> getAvailableFilters() const;

private:
    struct FilterInstance {
        int id;
        std::string type;
        EffectParams params;
    };

    std::map<int, std::vector<FilterInstance>> m_clipFilters;  // clipId -> filters
    int m_nextFilterId;
    
    std::unique_ptr<ColorFilter> m_colorFilter;
    std::unique_ptr<BlurFilter> m_blurFilter;
    
    std::mutex m_mutex;
    bool m_initialized;
};

}  // namespace videoeditor

#endif  // VIDEO_EDITOR_FILTER_MANAGER_H
