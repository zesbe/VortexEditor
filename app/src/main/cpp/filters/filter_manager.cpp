#include "filter_manager.h"

namespace videoeditor {

FilterManager::FilterManager()
    : m_nextFilterId(1)
    , m_initialized(false) {
    LOGI("FilterManager created");
}

FilterManager::~FilterManager() {
    release();
    LOGI("FilterManager destroyed");
}

bool FilterManager::initialize() {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    m_colorFilter = std::make_unique<ColorFilter>();
    m_blurFilter = std::make_unique<BlurFilter>();
    
    m_initialized = true;
    LOGI("FilterManager initialized");
    return true;
}

void FilterManager::release() {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    m_clipFilters.clear();
    m_colorFilter.reset();
    m_blurFilter.reset();
    
    m_initialized = false;
    LOGI("FilterManager released");
}

bool FilterManager::addFilter(int clipId, const std::string& filterType, const EffectParams& params) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    FilterInstance filter;
    filter.id = m_nextFilterId++;
    filter.type = filterType;
    filter.params = params;
    
    m_clipFilters[clipId].push_back(filter);
    
    LOGI("Added filter %d (%s) to clip %d", filter.id, filterType.c_str(), clipId);
    return true;
}

bool FilterManager::removeFilter(int clipId, int filterId) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    auto it = m_clipFilters.find(clipId);
    if (it == m_clipFilters.end()) {
        return false;
    }
    
    auto& filters = it->second;
    for (auto filterIt = filters.begin(); filterIt != filters.end(); ++filterIt) {
        if (filterIt->id == filterId) {
            filters.erase(filterIt);
            LOGI("Removed filter %d from clip %d", filterId, clipId);
            return true;
        }
    }
    
    return false;
}

bool FilterManager::updateFilter(int clipId, int filterId, const EffectParams& params) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    auto it = m_clipFilters.find(clipId);
    if (it == m_clipFilters.end()) {
        return false;
    }
    
    for (auto& filter : it->second) {
        if (filter.id == filterId) {
            filter.params = params;
            LOGI("Updated filter %d on clip %d", filterId, clipId);
            return true;
        }
    }
    
    return false;
}

void FilterManager::applyFilters(VideoFrame& frame, const std::string& clipPath) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    // Find clip ID by path (simplified - in real implementation, would have proper mapping)
    // For now, apply global filters
    
    for (const auto& pair : m_clipFilters) {
        for (const auto& filter : pair.second) {
            if (filter.type == "brightness" || filter.type == "contrast" || 
                filter.type == "saturation" || filter.type == "hue") {
                if (m_colorFilter) {
                    m_colorFilter->apply(frame, filter.type, filter.params.intensity);
                }
            } else if (filter.type == "blur" || filter.type == "gaussian") {
                if (m_blurFilter) {
                    m_blurFilter->apply(frame, static_cast<int>(filter.params.intensity));
                }
            }
        }
    }
}

std::vector<std::string> FilterManager::getAvailableFilters() const {
    return {
        "brightness",
        "contrast",
        "saturation",
        "hue",
        "blur",
        "gaussian",
        "sharpen",
        "vignette",
        "sepia",
        "grayscale",
        "invert"
    };
}

}  // namespace videoeditor
