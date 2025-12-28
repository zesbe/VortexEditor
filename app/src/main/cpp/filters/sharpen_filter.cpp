#include "sharpen_filter.h"
#include <cmath>

namespace videoeditor {

void SharpenFilter::apply(VideoFrame& frame, float intensity) {
    // 3x3 sharpening kernel
    float kernel[9] = {
         0, -intensity,  0,
        -intensity, 1 + 4 * intensity, -intensity,
         0, -intensity,  0
    };
    
    std::vector<uint8_t> result(frame.data.size());
    
    for (int y = 1; y < frame.height - 1; y++) {
        for (int x = 1; x < frame.width - 1; x++) {
            for (int c = 0; c < 3; c++) {  // RGB only
                float sum = 0;
                
                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        int idx = ((y + ky) * frame.width + (x + kx)) * 4 + c;
                        sum += frame.data[idx] * kernel[(ky + 1) * 3 + (kx + 1)];
                    }
                }
                
                int dstIdx = (y * frame.width + x) * 4 + c;
                result[dstIdx] = static_cast<uint8_t>(std::max(0.0f, std::min(255.0f, sum)));
            }
            
            // Alpha
            int alphaIdx = (y * frame.width + x) * 4 + 3;
            result[alphaIdx] = frame.data[alphaIdx];
        }
    }
    
    // Copy edges
    for (int y = 0; y < frame.height; y++) {
        for (int x = 0; x < frame.width; x++) {
            if (y == 0 || y == frame.height - 1 || x == 0 || x == frame.width - 1) {
                int idx = (y * frame.width + x) * 4;
                result[idx + 0] = frame.data[idx + 0];
                result[idx + 1] = frame.data[idx + 1];
                result[idx + 2] = frame.data[idx + 2];
                result[idx + 3] = frame.data[idx + 3];
            }
        }
    }
    
    frame.data = std::move(result);
}

void SharpenFilter::unsharpMask(VideoFrame& frame, float amount, float radius, float threshold) {
    // Create blurred version
    VideoFrame blurred = frame;
    
    // Simple box blur for the mask
    int blurRadius = static_cast<int>(radius);
    std::vector<uint8_t> temp(blurred.data.size());
    
    // Horizontal blur
    for (int y = 0; y < frame.height; y++) {
        for (int x = 0; x < frame.width; x++) {
            int sumR = 0, sumG = 0, sumB = 0;
            int count = 0;
            
            for (int kx = -blurRadius; kx <= blurRadius; kx++) {
                int sx = x + kx;
                if (sx >= 0 && sx < frame.width) {
                    int idx = (y * frame.width + sx) * 4;
                    sumR += blurred.data[idx + 0];
                    sumG += blurred.data[idx + 1];
                    sumB += blurred.data[idx + 2];
                    count++;
                }
            }
            
            int dstIdx = (y * frame.width + x) * 4;
            temp[dstIdx + 0] = sumR / count;
            temp[dstIdx + 1] = sumG / count;
            temp[dstIdx + 2] = sumB / count;
            temp[dstIdx + 3] = blurred.data[dstIdx + 3];
        }
    }
    
    // Vertical blur
    for (int y = 0; y < frame.height; y++) {
        for (int x = 0; x < frame.width; x++) {
            int sumR = 0, sumG = 0, sumB = 0;
            int count = 0;
            
            for (int ky = -blurRadius; ky <= blurRadius; ky++) {
                int sy = y + ky;
                if (sy >= 0 && sy < frame.height) {
                    int idx = (sy * frame.width + x) * 4;
                    sumR += temp[idx + 0];
                    sumG += temp[idx + 1];
                    sumB += temp[idx + 2];
                    count++;
                }
            }
            
            int dstIdx = (y * frame.width + x) * 4;
            blurred.data[dstIdx + 0] = sumR / count;
            blurred.data[dstIdx + 1] = sumG / count;
            blurred.data[dstIdx + 2] = sumB / count;
        }
    }
    
    // Apply unsharp mask
    int thresholdInt = static_cast<int>(threshold);
    
    for (size_t i = 0; i < frame.data.size(); i += 4) {
        for (int c = 0; c < 3; c++) {
            int diff = frame.data[i + c] - blurred.data[i + c];
            
            if (std::abs(diff) > thresholdInt) {
                int newVal = frame.data[i + c] + static_cast<int>(diff * amount);
                frame.data[i + c] = static_cast<uint8_t>(std::max(0, std::min(255, newVal)));
            }
        }
    }
}

}  // namespace videoeditor
