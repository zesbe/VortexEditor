#include "frame_buffer.h"
#include <cmath>

namespace videoeditor {

FrameBuffer::FrameBuffer(int width, int height)
    : m_width(width)
    , m_height(height) {
    m_buffer.resize(width * height * 4);  // RGBA
    clear();
    LOGI("FrameBuffer created: %dx%d", width, height);
}

FrameBuffer::~FrameBuffer() {
    LOGI("FrameBuffer destroyed");
}

void FrameBuffer::clear() {
    std::lock_guard<std::mutex> lock(m_mutex);
    std::fill(m_buffer.begin(), m_buffer.end(), 0);
}

void FrameBuffer::composite(VideoFrame& dest, const VideoFrame& src, const ClipInfo& clip) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    if (src.data.empty()) {
        return;
    }
    
    // Simple blit - in real implementation, handle scaling, alpha, etc.
    int srcWidth = src.width;
    int srcHeight = src.height;
    int dstWidth = dest.width;
    int dstHeight = dest.height;
    
    // Calculate scaling factors
    float scaleX = static_cast<float>(dstWidth) / srcWidth;
    float scaleY = static_cast<float>(dstHeight) / srcHeight;
    float scale = std::min(scaleX, scaleY);  // Fit inside
    
    int scaledWidth = static_cast<int>(srcWidth * scale);
    int scaledHeight = static_cast<int>(srcHeight * scale);
    
    // Center position
    int offsetX = (dstWidth - scaledWidth) / 2;
    int offsetY = (dstHeight - scaledHeight) / 2;
    
    // Copy with scaling (nearest neighbor for simplicity)
    for (int y = 0; y < scaledHeight; y++) {
        for (int x = 0; x < scaledWidth; x++) {
            int srcX = static_cast<int>(x / scale);
            int srcY = static_cast<int>(y / scale);
            
            if (srcX >= srcWidth) srcX = srcWidth - 1;
            if (srcY >= srcHeight) srcY = srcHeight - 1;
            
            int dstIdx = ((offsetY + y) * dstWidth + (offsetX + x)) * 4;
            int srcIdx = (srcY * srcWidth + srcX) * 4;
            
            if (dstIdx + 3 < dest.data.size() && srcIdx + 3 < src.data.size()) {
                // Alpha blending
                uint8_t srcAlpha = src.data[srcIdx + 3];
                float alpha = srcAlpha / 255.0f;
                
                dest.data[dstIdx + 0] = static_cast<uint8_t>(
                    src.data[srcIdx + 0] * alpha + dest.data[dstIdx + 0] * (1 - alpha));
                dest.data[dstIdx + 1] = static_cast<uint8_t>(
                    src.data[srcIdx + 1] * alpha + dest.data[dstIdx + 1] * (1 - alpha));
                dest.data[dstIdx + 2] = static_cast<uint8_t>(
                    src.data[srcIdx + 2] * alpha + dest.data[dstIdx + 2] * (1 - alpha));
                dest.data[dstIdx + 3] = 255;
            }
        }
    }
}

void FrameBuffer::blend(VideoFrame& dest, const VideoFrame& src, float alpha) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    size_t pixelCount = std::min(dest.data.size(), src.data.size()) / 4;
    
    for (size_t i = 0; i < pixelCount; i++) {
        size_t idx = i * 4;
        dest.data[idx + 0] = static_cast<uint8_t>(
            src.data[idx + 0] * alpha + dest.data[idx + 0] * (1 - alpha));
        dest.data[idx + 1] = static_cast<uint8_t>(
            src.data[idx + 1] * alpha + dest.data[idx + 1] * (1 - alpha));
        dest.data[idx + 2] = static_cast<uint8_t>(
            src.data[idx + 2] * alpha + dest.data[idx + 2] * (1 - alpha));
    }
}

VideoFrame FrameBuffer::scale(const VideoFrame& src, int newWidth, int newHeight) {
    VideoFrame result;
    result.width = newWidth;
    result.height = newHeight;
    result.format = src.format;
    result.timestamp_us = src.timestamp_us;
    result.data.resize(newWidth * newHeight * 4);
    
    float scaleX = static_cast<float>(src.width) / newWidth;
    float scaleY = static_cast<float>(src.height) / newHeight;
    
    // Bilinear interpolation
    for (int y = 0; y < newHeight; y++) {
        for (int x = 0; x < newWidth; x++) {
            float srcX = x * scaleX;
            float srcY = y * scaleY;
            
            int x0 = static_cast<int>(srcX);
            int y0 = static_cast<int>(srcY);
            int x1 = std::min(x0 + 1, src.width - 1);
            int y1 = std::min(y0 + 1, src.height - 1);
            
            float xFrac = srcX - x0;
            float yFrac = srcY - y0;
            
            for (int c = 0; c < 4; c++) {
                float v00 = src.data[(y0 * src.width + x0) * 4 + c];
                float v10 = src.data[(y0 * src.width + x1) * 4 + c];
                float v01 = src.data[(y1 * src.width + x0) * 4 + c];
                float v11 = src.data[(y1 * src.width + x1) * 4 + c];
                
                float v0 = v00 * (1 - xFrac) + v10 * xFrac;
                float v1 = v01 * (1 - xFrac) + v11 * xFrac;
                float v = v0 * (1 - yFrac) + v1 * yFrac;
                
                result.data[(y * newWidth + x) * 4 + c] = static_cast<uint8_t>(v);
            }
        }
    }
    
    return result;
}

VideoFrame FrameBuffer::crop(const VideoFrame& src, int cropX, int cropY, int cropWidth, int cropHeight) {
    VideoFrame result;
    result.width = cropWidth;
    result.height = cropHeight;
    result.format = src.format;
    result.timestamp_us = src.timestamp_us;
    result.data.resize(cropWidth * cropHeight * 4);
    
    for (int y = 0; y < cropHeight; y++) {
        for (int x = 0; x < cropWidth; x++) {
            int srcX = cropX + x;
            int srcY = cropY + y;
            
            if (srcX >= 0 && srcX < src.width && srcY >= 0 && srcY < src.height) {
                int srcIdx = (srcY * src.width + srcX) * 4;
                int dstIdx = (y * cropWidth + x) * 4;
                
                result.data[dstIdx + 0] = src.data[srcIdx + 0];
                result.data[dstIdx + 1] = src.data[srcIdx + 1];
                result.data[dstIdx + 2] = src.data[srcIdx + 2];
                result.data[dstIdx + 3] = src.data[srcIdx + 3];
            }
        }
    }
    
    return result;
}

VideoFrame FrameBuffer::rotate(const VideoFrame& src, int degrees) {
    VideoFrame result;
    result.format = src.format;
    result.timestamp_us = src.timestamp_us;
    
    if (degrees == 90 || degrees == 270) {
        result.width = src.height;
        result.height = src.width;
    } else {
        result.width = src.width;
        result.height = src.height;
    }
    
    result.data.resize(result.width * result.height * 4);
    
    for (int y = 0; y < src.height; y++) {
        for (int x = 0; x < src.width; x++) {
            int srcIdx = (y * src.width + x) * 4;
            int dstX, dstY;
            
            switch (degrees) {
                case 90:
                    dstX = src.height - 1 - y;
                    dstY = x;
                    break;
                case 180:
                    dstX = src.width - 1 - x;
                    dstY = src.height - 1 - y;
                    break;
                case 270:
                    dstX = y;
                    dstY = src.width - 1 - x;
                    break;
                default:
                    dstX = x;
                    dstY = y;
                    break;
            }
            
            int dstIdx = (dstY * result.width + dstX) * 4;
            
            result.data[dstIdx + 0] = src.data[srcIdx + 0];
            result.data[dstIdx + 1] = src.data[srcIdx + 1];
            result.data[dstIdx + 2] = src.data[srcIdx + 2];
            result.data[dstIdx + 3] = src.data[srcIdx + 3];
        }
    }
    
    return result;
}

VideoFrame FrameBuffer::flipHorizontal(const VideoFrame& src) {
    VideoFrame result = src;
    
    for (int y = 0; y < src.height; y++) {
        for (int x = 0; x < src.width / 2; x++) {
            int leftIdx = (y * src.width + x) * 4;
            int rightIdx = (y * src.width + (src.width - 1 - x)) * 4;
            
            for (int c = 0; c < 4; c++) {
                std::swap(result.data[leftIdx + c], result.data[rightIdx + c]);
            }
        }
    }
    
    return result;
}

VideoFrame FrameBuffer::flipVertical(const VideoFrame& src) {
    VideoFrame result = src;
    
    int rowSize = src.width * 4;
    
    for (int y = 0; y < src.height / 2; y++) {
        int topOffset = y * rowSize;
        int bottomOffset = (src.height - 1 - y) * rowSize;
        
        for (int i = 0; i < rowSize; i++) {
            std::swap(result.data[topOffset + i], result.data[bottomOffset + i]);
        }
    }
    
    return result;
}

void FrameBuffer::rgbaToYuv420(const uint8_t* rgba, uint8_t* yuv, int width, int height) {
    int ySize = width * height;
    int uvSize = ySize / 4;
    
    uint8_t* yPlane = yuv;
    uint8_t* uPlane = yuv + ySize;
    uint8_t* vPlane = yuv + ySize + uvSize;
    
    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            int idx = (y * width + x) * 4;
            uint8_t r = rgba[idx + 0];
            uint8_t g = rgba[idx + 1];
            uint8_t b = rgba[idx + 2];
            
            // Y
            yPlane[y * width + x] = static_cast<uint8_t>(
                ((66 * r + 129 * g + 25 * b + 128) >> 8) + 16);
            
            // U, V (subsampled 2x2)
            if (y % 2 == 0 && x % 2 == 0) {
                int uvIdx = (y / 2) * (width / 2) + (x / 2);
                uPlane[uvIdx] = static_cast<uint8_t>(
                    ((-38 * r - 74 * g + 112 * b + 128) >> 8) + 128);
                vPlane[uvIdx] = static_cast<uint8_t>(
                    ((112 * r - 94 * g - 18 * b + 128) >> 8) + 128);
            }
        }
    }
}

void FrameBuffer::yuv420ToRgba(const uint8_t* yuv, uint8_t* rgba, int width, int height) {
    int ySize = width * height;
    int uvSize = ySize / 4;
    
    const uint8_t* yPlane = yuv;
    const uint8_t* uPlane = yuv + ySize;
    const uint8_t* vPlane = yuv + ySize + uvSize;
    
    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            int yIdx = y * width + x;
            int uvIdx = (y / 2) * (width / 2) + (x / 2);
            
            int Y = yPlane[yIdx] - 16;
            int U = uPlane[uvIdx] - 128;
            int V = vPlane[uvIdx] - 128;
            
            int r = (298 * Y + 409 * V + 128) >> 8;
            int g = (298 * Y - 100 * U - 208 * V + 128) >> 8;
            int b = (298 * Y + 516 * U + 128) >> 8;
            
            int idx = (y * width + x) * 4;
            rgba[idx + 0] = static_cast<uint8_t>(std::max(0, std::min(255, r)));
            rgba[idx + 1] = static_cast<uint8_t>(std::max(0, std::min(255, g)));
            rgba[idx + 2] = static_cast<uint8_t>(std::max(0, std::min(255, b)));
            rgba[idx + 3] = 255;
        }
    }
}

}  // namespace videoeditor
