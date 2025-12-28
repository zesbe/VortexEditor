#include "blur_filter.h"
#include <cmath>

namespace videoeditor {

BlurFilter::BlurFilter() {
    LOGI("BlurFilter created");
}

BlurFilter::~BlurFilter() {
    LOGI("BlurFilter destroyed");
}

void BlurFilter::apply(VideoFrame& frame, int radius) {
    boxBlur(frame, radius);
}

void BlurFilter::boxBlur(VideoFrame& frame, int radius) {
    if (radius <= 0) return;
    
    std::vector<uint8_t> temp(frame.data.size());
    
    // Two-pass box blur (horizontal then vertical)
    horizontalBlur(frame.data.data(), temp.data(), frame.width, frame.height, radius);
    verticalBlur(temp.data(), frame.data.data(), frame.width, frame.height, radius);
}

void BlurFilter::horizontalBlur(uint8_t* src, uint8_t* dst, int width, int height, int radius) {
    int kernelSize = radius * 2 + 1;
    
    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            int sumR = 0, sumG = 0, sumB = 0, sumA = 0;
            int count = 0;
            
            for (int kx = -radius; kx <= radius; kx++) {
                int sx = x + kx;
                if (sx >= 0 && sx < width) {
                    int idx = (y * width + sx) * 4;
                    sumR += src[idx + 0];
                    sumG += src[idx + 1];
                    sumB += src[idx + 2];
                    sumA += src[idx + 3];
                    count++;
                }
            }
            
            int dstIdx = (y * width + x) * 4;
            dst[dstIdx + 0] = sumR / count;
            dst[dstIdx + 1] = sumG / count;
            dst[dstIdx + 2] = sumB / count;
            dst[dstIdx + 3] = sumA / count;
        }
    }
}

void BlurFilter::verticalBlur(uint8_t* src, uint8_t* dst, int width, int height, int radius) {
    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            int sumR = 0, sumG = 0, sumB = 0, sumA = 0;
            int count = 0;
            
            for (int ky = -radius; ky <= radius; ky++) {
                int sy = y + ky;
                if (sy >= 0 && sy < height) {
                    int idx = (sy * width + x) * 4;
                    sumR += src[idx + 0];
                    sumG += src[idx + 1];
                    sumB += src[idx + 2];
                    sumA += src[idx + 3];
                    count++;
                }
            }
            
            int dstIdx = (y * width + x) * 4;
            dst[dstIdx + 0] = sumR / count;
            dst[dstIdx + 1] = sumG / count;
            dst[dstIdx + 2] = sumB / count;
            dst[dstIdx + 3] = sumA / count;
        }
    }
}

void BlurFilter::gaussianBlur(VideoFrame& frame, int radius) {
    if (radius <= 0) return;
    
    std::vector<float> kernel = createGaussianKernel(radius);
    std::vector<uint8_t> temp(frame.data.size());
    
    int kernelSize = radius * 2 + 1;
    
    // Horizontal pass
    for (int y = 0; y < frame.height; y++) {
        for (int x = 0; x < frame.width; x++) {
            float sumR = 0, sumG = 0, sumB = 0, sumA = 0;
            float sumWeight = 0;
            
            for (int kx = -radius; kx <= radius; kx++) {
                int sx = x + kx;
                if (sx >= 0 && sx < frame.width) {
                    int idx = (y * frame.width + sx) * 4;
                    float weight = kernel[kx + radius];
                    sumR += frame.data[idx + 0] * weight;
                    sumG += frame.data[idx + 1] * weight;
                    sumB += frame.data[idx + 2] * weight;
                    sumA += frame.data[idx + 3] * weight;
                    sumWeight += weight;
                }
            }
            
            int dstIdx = (y * frame.width + x) * 4;
            temp[dstIdx + 0] = static_cast<uint8_t>(sumR / sumWeight);
            temp[dstIdx + 1] = static_cast<uint8_t>(sumG / sumWeight);
            temp[dstIdx + 2] = static_cast<uint8_t>(sumB / sumWeight);
            temp[dstIdx + 3] = static_cast<uint8_t>(sumA / sumWeight);
        }
    }
    
    // Vertical pass
    for (int y = 0; y < frame.height; y++) {
        for (int x = 0; x < frame.width; x++) {
            float sumR = 0, sumG = 0, sumB = 0, sumA = 0;
            float sumWeight = 0;
            
            for (int ky = -radius; ky <= radius; ky++) {
                int sy = y + ky;
                if (sy >= 0 && sy < frame.height) {
                    int idx = (sy * frame.width + x) * 4;
                    float weight = kernel[ky + radius];
                    sumR += temp[idx + 0] * weight;
                    sumG += temp[idx + 1] * weight;
                    sumB += temp[idx + 2] * weight;
                    sumA += temp[idx + 3] * weight;
                    sumWeight += weight;
                }
            }
            
            int dstIdx = (y * frame.width + x) * 4;
            frame.data[dstIdx + 0] = static_cast<uint8_t>(sumR / sumWeight);
            frame.data[dstIdx + 1] = static_cast<uint8_t>(sumG / sumWeight);
            frame.data[dstIdx + 2] = static_cast<uint8_t>(sumB / sumWeight);
            frame.data[dstIdx + 3] = static_cast<uint8_t>(sumA / sumWeight);
        }
    }
}

std::vector<float> BlurFilter::createGaussianKernel(int radius) {
    std::vector<float> kernel(radius * 2 + 1);
    float sigma = radius / 3.0f;
    float sum = 0;
    
    for (int i = -radius; i <= radius; i++) {
        float value = std::exp(-(i * i) / (2 * sigma * sigma));
        kernel[i + radius] = value;
        sum += value;
    }
    
    // Normalize
    for (auto& v : kernel) {
        v /= sum;
    }
    
    return kernel;
}

void BlurFilter::motionBlur(VideoFrame& frame, int angle, int distance) {
    if (distance <= 0) return;
    
    float radians = angle * M_PI / 180.0f;
    float dx = std::cos(radians);
    float dy = std::sin(radians);
    
    std::vector<uint8_t> result(frame.data.size());
    
    for (int y = 0; y < frame.height; y++) {
        for (int x = 0; x < frame.width; x++) {
            float sumR = 0, sumG = 0, sumB = 0, sumA = 0;
            int count = 0;
            
            for (int d = -distance/2; d <= distance/2; d++) {
                int sx = static_cast<int>(x + d * dx);
                int sy = static_cast<int>(y + d * dy);
                
                if (sx >= 0 && sx < frame.width && sy >= 0 && sy < frame.height) {
                    int idx = (sy * frame.width + sx) * 4;
                    sumR += frame.data[idx + 0];
                    sumG += frame.data[idx + 1];
                    sumB += frame.data[idx + 2];
                    sumA += frame.data[idx + 3];
                    count++;
                }
            }
            
            int dstIdx = (y * frame.width + x) * 4;
            result[dstIdx + 0] = static_cast<uint8_t>(sumR / count);
            result[dstIdx + 1] = static_cast<uint8_t>(sumG / count);
            result[dstIdx + 2] = static_cast<uint8_t>(sumB / count);
            result[dstIdx + 3] = static_cast<uint8_t>(sumA / count);
        }
    }
    
    frame.data = std::move(result);
}

}  // namespace videoeditor
