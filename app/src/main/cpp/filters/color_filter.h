#ifndef VIDEO_EDITOR_COLOR_FILTER_H
#define VIDEO_EDITOR_COLOR_FILTER_H

#include "common.h"

namespace videoeditor {

class ColorFilter {
public:
    ColorFilter();
    ~ColorFilter();

    // Apply color adjustment
    void apply(VideoFrame& frame, const std::string& type, float intensity);

    // Specific adjustments
    void adjustBrightness(VideoFrame& frame, float value);  // -1.0 to 1.0
    void adjustContrast(VideoFrame& frame, float value);    // 0.0 to 2.0
    void adjustSaturation(VideoFrame& frame, float value);  // 0.0 to 2.0
    void adjustHue(VideoFrame& frame, float degrees);       // -180 to 180
    void adjustTemperature(VideoFrame& frame, float value); // -1.0 to 1.0
    void adjustTint(VideoFrame& frame, float value);        // -1.0 to 1.0

    // Preset filters
    void applySepia(VideoFrame& frame, float intensity);
    void applyGrayscale(VideoFrame& frame);
    void applyInvert(VideoFrame& frame);
    void applyVignette(VideoFrame& frame, float intensity);

private:
    void rgbToHsl(uint8_t r, uint8_t g, uint8_t b, float& h, float& s, float& l);
    void hslToRgb(float h, float s, float l, uint8_t& r, uint8_t& g, uint8_t& b);
};

}  // namespace videoeditor

#endif  // VIDEO_EDITOR_COLOR_FILTER_H
