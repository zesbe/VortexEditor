#ifndef VIDEO_EDITOR_BLUR_FILTER_H
#define VIDEO_EDITOR_BLUR_FILTER_H

#include "common.h"

namespace videoeditor {

class BlurFilter {
public:
    BlurFilter();
    ~BlurFilter();

    // Apply blur
    void apply(VideoFrame& frame, int radius);

    // Specific blur types
    void boxBlur(VideoFrame& frame, int radius);
    void gaussianBlur(VideoFrame& frame, int radius);
    void motionBlur(VideoFrame& frame, int angle, int distance);

private:
    void horizontalBlur(uint8_t* src, uint8_t* dst, int width, int height, int radius);
    void verticalBlur(uint8_t* src, uint8_t* dst, int width, int height, int radius);
    std::vector<float> createGaussianKernel(int radius);
};

}  // namespace videoeditor

#endif  // VIDEO_EDITOR_BLUR_FILTER_H
