#ifndef VIDEO_EDITOR_IMAGE_UTILS_H
#define VIDEO_EDITOR_IMAGE_UTILS_H

#include "common.h"

namespace videoeditor {

class ImageUtils {
public:
    static VideoFrame resize(const VideoFrame& src, int newWidth, int newHeight);
    static VideoFrame crop(const VideoFrame& src, int x, int y, int width, int height);
    static VideoFrame rotate90(const VideoFrame& src);
    static VideoFrame rotate180(const VideoFrame& src);
    static VideoFrame rotate270(const VideoFrame& src);
    static VideoFrame flipH(const VideoFrame& src);
    static VideoFrame flipV(const VideoFrame& src);
    
    static void copyRegion(const VideoFrame& src, VideoFrame& dst,
                          int srcX, int srcY, int dstX, int dstY,
                          int width, int height);
    
    static void fill(VideoFrame& frame, uint8_t r, uint8_t g, uint8_t b, uint8_t a);
    static void fillRect(VideoFrame& frame, int x, int y, int width, int height,
                        uint8_t r, uint8_t g, uint8_t b, uint8_t a);
};

}  // namespace videoeditor

#endif  // VIDEO_EDITOR_IMAGE_UTILS_H
