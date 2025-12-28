#include "image_utils.h"
#include <algorithm>

namespace videoeditor {

VideoFrame ImageUtils::resize(const VideoFrame& src, int newWidth, int newHeight) {
    VideoFrame dst;
    dst.width = newWidth;
    dst.height = newHeight;
    dst.format = src.format;
    dst.timestamp_us = src.timestamp_us;
    dst.data.resize(newWidth * newHeight * 4);
    
    float scaleX = static_cast<float>(src.width) / newWidth;
    float scaleY = static_cast<float>(src.height) / newHeight;
    
    for (int y = 0; y < newHeight; y++) {
        for (int x = 0; x < newWidth; x++) {
            int srcX = static_cast<int>(x * scaleX);
            int srcY = static_cast<int>(y * scaleY);
            srcX = std::min(srcX, src.width - 1);
            srcY = std::min(srcY, src.height - 1);
            
            int srcIdx = (srcY * src.width + srcX) * 4;
            int dstIdx = (y * newWidth + x) * 4;
            
            dst.data[dstIdx + 0] = src.data[srcIdx + 0];
            dst.data[dstIdx + 1] = src.data[srcIdx + 1];
            dst.data[dstIdx + 2] = src.data[srcIdx + 2];
            dst.data[dstIdx + 3] = src.data[srcIdx + 3];
        }
    }
    
    return dst;
}

VideoFrame ImageUtils::crop(const VideoFrame& src, int x, int y, int width, int height) {
    VideoFrame dst;
    dst.width = width;
    dst.height = height;
    dst.format = src.format;
    dst.timestamp_us = src.timestamp_us;
    dst.data.resize(width * height * 4);
    
    for (int dy = 0; dy < height; dy++) {
        int srcY = y + dy;
        if (srcY < 0 || srcY >= src.height) continue;
        
        for (int dx = 0; dx < width; dx++) {
            int srcX = x + dx;
            if (srcX < 0 || srcX >= src.width) continue;
            
            int srcIdx = (srcY * src.width + srcX) * 4;
            int dstIdx = (dy * width + dx) * 4;
            
            dst.data[dstIdx + 0] = src.data[srcIdx + 0];
            dst.data[dstIdx + 1] = src.data[srcIdx + 1];
            dst.data[dstIdx + 2] = src.data[srcIdx + 2];
            dst.data[dstIdx + 3] = src.data[srcIdx + 3];
        }
    }
    
    return dst;
}

VideoFrame ImageUtils::rotate90(const VideoFrame& src) {
    VideoFrame dst;
    dst.width = src.height;
    dst.height = src.width;
    dst.format = src.format;
    dst.timestamp_us = src.timestamp_us;
    dst.data.resize(dst.width * dst.height * 4);
    
    for (int y = 0; y < src.height; y++) {
        for (int x = 0; x < src.width; x++) {
            int srcIdx = (y * src.width + x) * 4;
            int dstX = src.height - 1 - y;
            int dstY = x;
            int dstIdx = (dstY * dst.width + dstX) * 4;
            
            dst.data[dstIdx + 0] = src.data[srcIdx + 0];
            dst.data[dstIdx + 1] = src.data[srcIdx + 1];
            dst.data[dstIdx + 2] = src.data[srcIdx + 2];
            dst.data[dstIdx + 3] = src.data[srcIdx + 3];
        }
    }
    
    return dst;
}

VideoFrame ImageUtils::rotate180(const VideoFrame& src) {
    VideoFrame dst;
    dst.width = src.width;
    dst.height = src.height;
    dst.format = src.format;
    dst.timestamp_us = src.timestamp_us;
    dst.data.resize(src.data.size());
    
    for (int y = 0; y < src.height; y++) {
        for (int x = 0; x < src.width; x++) {
            int srcIdx = (y * src.width + x) * 4;
            int dstIdx = ((src.height - 1 - y) * src.width + (src.width - 1 - x)) * 4;
            
            dst.data[dstIdx + 0] = src.data[srcIdx + 0];
            dst.data[dstIdx + 1] = src.data[srcIdx + 1];
            dst.data[dstIdx + 2] = src.data[srcIdx + 2];
            dst.data[dstIdx + 3] = src.data[srcIdx + 3];
        }
    }
    
    return dst;
}

VideoFrame ImageUtils::rotate270(const VideoFrame& src) {
    VideoFrame dst;
    dst.width = src.height;
    dst.height = src.width;
    dst.format = src.format;
    dst.timestamp_us = src.timestamp_us;
    dst.data.resize(dst.width * dst.height * 4);
    
    for (int y = 0; y < src.height; y++) {
        for (int x = 0; x < src.width; x++) {
            int srcIdx = (y * src.width + x) * 4;
            int dstX = y;
            int dstY = src.width - 1 - x;
            int dstIdx = (dstY * dst.width + dstX) * 4;
            
            dst.data[dstIdx + 0] = src.data[srcIdx + 0];
            dst.data[dstIdx + 1] = src.data[srcIdx + 1];
            dst.data[dstIdx + 2] = src.data[srcIdx + 2];
            dst.data[dstIdx + 3] = src.data[srcIdx + 3];
        }
    }
    
    return dst;
}

VideoFrame ImageUtils::flipH(const VideoFrame& src) {
    VideoFrame dst = src;
    
    for (int y = 0; y < src.height; y++) {
        for (int x = 0; x < src.width / 2; x++) {
            int idx1 = (y * src.width + x) * 4;
            int idx2 = (y * src.width + (src.width - 1 - x)) * 4;
            
            std::swap(dst.data[idx1 + 0], dst.data[idx2 + 0]);
            std::swap(dst.data[idx1 + 1], dst.data[idx2 + 1]);
            std::swap(dst.data[idx1 + 2], dst.data[idx2 + 2]);
            std::swap(dst.data[idx1 + 3], dst.data[idx2 + 3]);
        }
    }
    
    return dst;
}

VideoFrame ImageUtils::flipV(const VideoFrame& src) {
    VideoFrame dst = src;
    int rowBytes = src.width * 4;
    
    for (int y = 0; y < src.height / 2; y++) {
        int offset1 = y * rowBytes;
        int offset2 = (src.height - 1 - y) * rowBytes;
        
        for (int i = 0; i < rowBytes; i++) {
            std::swap(dst.data[offset1 + i], dst.data[offset2 + i]);
        }
    }
    
    return dst;
}

void ImageUtils::copyRegion(const VideoFrame& src, VideoFrame& dst,
                           int srcX, int srcY, int dstX, int dstY,
                           int width, int height) {
    for (int y = 0; y < height; y++) {
        int sy = srcY + y;
        int dy = dstY + y;
        
        if (sy < 0 || sy >= src.height || dy < 0 || dy >= dst.height) continue;
        
        for (int x = 0; x < width; x++) {
            int sx = srcX + x;
            int dx = dstX + x;
            
            if (sx < 0 || sx >= src.width || dx < 0 || dx >= dst.width) continue;
            
            int srcIdx = (sy * src.width + sx) * 4;
            int dstIdx = (dy * dst.width + dx) * 4;
            
            dst.data[dstIdx + 0] = src.data[srcIdx + 0];
            dst.data[dstIdx + 1] = src.data[srcIdx + 1];
            dst.data[dstIdx + 2] = src.data[srcIdx + 2];
            dst.data[dstIdx + 3] = src.data[srcIdx + 3];
        }
    }
}

void ImageUtils::fill(VideoFrame& frame, uint8_t r, uint8_t g, uint8_t b, uint8_t a) {
    for (size_t i = 0; i < frame.data.size(); i += 4) {
        frame.data[i + 0] = r;
        frame.data[i + 1] = g;
        frame.data[i + 2] = b;
        frame.data[i + 3] = a;
    }
}

void ImageUtils::fillRect(VideoFrame& frame, int x, int y, int width, int height,
                         uint8_t r, uint8_t g, uint8_t b, uint8_t a) {
    for (int dy = 0; dy < height; dy++) {
        int py = y + dy;
        if (py < 0 || py >= frame.height) continue;
        
        for (int dx = 0; dx < width; dx++) {
            int px = x + dx;
            if (px < 0 || px >= frame.width) continue;
            
            int idx = (py * frame.width + px) * 4;
            frame.data[idx + 0] = r;
            frame.data[idx + 1] = g;
            frame.data[idx + 2] = b;
            frame.data[idx + 3] = a;
        }
    }
}

}  // namespace videoeditor
