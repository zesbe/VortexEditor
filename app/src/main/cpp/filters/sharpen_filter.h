#ifndef VIDEO_EDITOR_SHARPEN_FILTER_H
#define VIDEO_EDITOR_SHARPEN_FILTER_H

#include "common.h"

namespace videoeditor {

class SharpenFilter {
public:
    void apply(VideoFrame& frame, float intensity);
    void unsharpMask(VideoFrame& frame, float amount, float radius, float threshold);
};

}  // namespace videoeditor

#endif  // VIDEO_EDITOR_SHARPEN_FILTER_H
