#include "color_filter.h"
#include <cmath>
#include <algorithm>

namespace videoeditor {

ColorFilter::ColorFilter() {
    LOGI("ColorFilter created");
}

ColorFilter::~ColorFilter() {
    LOGI("ColorFilter destroyed");
}

void ColorFilter::apply(VideoFrame& frame, const std::string& type, float intensity) {
    if (type == "brightness") {
        adjustBrightness(frame, intensity);
    } else if (type == "contrast") {
        adjustContrast(frame, intensity);
    } else if (type == "saturation") {
        adjustSaturation(frame, intensity);
    } else if (type == "hue") {
        adjustHue(frame, intensity);
    } else if (type == "sepia") {
        applySepia(frame, intensity);
    } else if (type == "grayscale") {
        applyGrayscale(frame);
    } else if (type == "invert") {
        applyInvert(frame);
    } else if (type == "vignette") {
        applyVignette(frame, intensity);
    }
}

void ColorFilter::adjustBrightness(VideoFrame& frame, float value) {
    int adjustment = static_cast<int>(value * 255);
    
    for (size_t i = 0; i < frame.data.size(); i += 4) {
        frame.data[i + 0] = std::max(0, std::min(255, frame.data[i + 0] + adjustment));
        frame.data[i + 1] = std::max(0, std::min(255, frame.data[i + 1] + adjustment));
        frame.data[i + 2] = std::max(0, std::min(255, frame.data[i + 2] + adjustment));
    }
}

void ColorFilter::adjustContrast(VideoFrame& frame, float value) {
    float factor = (259.0f * (value * 255 + 255)) / (255.0f * (259 - value * 255));
    
    for (size_t i = 0; i < frame.data.size(); i += 4) {
        frame.data[i + 0] = std::max(0, std::min(255, 
            static_cast<int>(factor * (frame.data[i + 0] - 128) + 128)));
        frame.data[i + 1] = std::max(0, std::min(255, 
            static_cast<int>(factor * (frame.data[i + 1] - 128) + 128)));
        frame.data[i + 2] = std::max(0, std::min(255, 
            static_cast<int>(factor * (frame.data[i + 2] - 128) + 128)));
    }
}

void ColorFilter::adjustSaturation(VideoFrame& frame, float value) {
    for (size_t i = 0; i < frame.data.size(); i += 4) {
        uint8_t r = frame.data[i + 0];
        uint8_t g = frame.data[i + 1];
        uint8_t b = frame.data[i + 2];
        
        float gray = 0.299f * r + 0.587f * g + 0.114f * b;
        
        frame.data[i + 0] = std::max(0, std::min(255, 
            static_cast<int>(gray + value * (r - gray))));
        frame.data[i + 1] = std::max(0, std::min(255, 
            static_cast<int>(gray + value * (g - gray))));
        frame.data[i + 2] = std::max(0, std::min(255, 
            static_cast<int>(gray + value * (b - gray))));
    }
}

void ColorFilter::adjustHue(VideoFrame& frame, float degrees) {
    float hueShift = degrees / 360.0f;
    
    for (size_t i = 0; i < frame.data.size(); i += 4) {
        float h, s, l;
        rgbToHsl(frame.data[i + 0], frame.data[i + 1], frame.data[i + 2], h, s, l);
        
        h += hueShift;
        if (h > 1.0f) h -= 1.0f;
        if (h < 0.0f) h += 1.0f;
        
        hslToRgb(h, s, l, frame.data[i + 0], frame.data[i + 1], frame.data[i + 2]);
    }
}

void ColorFilter::adjustTemperature(VideoFrame& frame, float value) {
    int rAdjust = static_cast<int>(value * 30);
    int bAdjust = static_cast<int>(-value * 30);
    
    for (size_t i = 0; i < frame.data.size(); i += 4) {
        frame.data[i + 0] = std::max(0, std::min(255, frame.data[i + 0] + rAdjust));
        frame.data[i + 2] = std::max(0, std::min(255, frame.data[i + 2] + bAdjust));
    }
}

void ColorFilter::adjustTint(VideoFrame& frame, float value) {
    int gAdjust = static_cast<int>(value * 30);
    int mAdjust = static_cast<int>(-value * 15);
    
    for (size_t i = 0; i < frame.data.size(); i += 4) {
        frame.data[i + 1] = std::max(0, std::min(255, frame.data[i + 1] + gAdjust));
        frame.data[i + 0] = std::max(0, std::min(255, frame.data[i + 0] + mAdjust));
        frame.data[i + 2] = std::max(0, std::min(255, frame.data[i + 2] + mAdjust));
    }
}

void ColorFilter::applySepia(VideoFrame& frame, float intensity) {
    for (size_t i = 0; i < frame.data.size(); i += 4) {
        uint8_t r = frame.data[i + 0];
        uint8_t g = frame.data[i + 1];
        uint8_t b = frame.data[i + 2];
        
        int newR = static_cast<int>(0.393f * r + 0.769f * g + 0.189f * b);
        int newG = static_cast<int>(0.349f * r + 0.686f * g + 0.168f * b);
        int newB = static_cast<int>(0.272f * r + 0.534f * g + 0.131f * b);
        
        frame.data[i + 0] = std::max(0, std::min(255, 
            static_cast<int>(r + intensity * (newR - r))));
        frame.data[i + 1] = std::max(0, std::min(255, 
            static_cast<int>(g + intensity * (newG - g))));
        frame.data[i + 2] = std::max(0, std::min(255, 
            static_cast<int>(b + intensity * (newB - b))));
    }
}

void ColorFilter::applyGrayscale(VideoFrame& frame) {
    for (size_t i = 0; i < frame.data.size(); i += 4) {
        uint8_t gray = static_cast<uint8_t>(
            0.299f * frame.data[i + 0] + 
            0.587f * frame.data[i + 1] + 
            0.114f * frame.data[i + 2]);
        
        frame.data[i + 0] = gray;
        frame.data[i + 1] = gray;
        frame.data[i + 2] = gray;
    }
}

void ColorFilter::applyInvert(VideoFrame& frame) {
    for (size_t i = 0; i < frame.data.size(); i += 4) {
        frame.data[i + 0] = 255 - frame.data[i + 0];
        frame.data[i + 1] = 255 - frame.data[i + 1];
        frame.data[i + 2] = 255 - frame.data[i + 2];
    }
}

void ColorFilter::applyVignette(VideoFrame& frame, float intensity) {
    float centerX = frame.width / 2.0f;
    float centerY = frame.height / 2.0f;
    float maxDist = std::sqrt(centerX * centerX + centerY * centerY);
    
    for (int y = 0; y < frame.height; y++) {
        for (int x = 0; x < frame.width; x++) {
            float dx = x - centerX;
            float dy = y - centerY;
            float dist = std::sqrt(dx * dx + dy * dy);
            float factor = 1.0f - intensity * std::pow(dist / maxDist, 2);
            factor = std::max(0.0f, factor);
            
            size_t idx = (y * frame.width + x) * 4;
            frame.data[idx + 0] = static_cast<uint8_t>(frame.data[idx + 0] * factor);
            frame.data[idx + 1] = static_cast<uint8_t>(frame.data[idx + 1] * factor);
            frame.data[idx + 2] = static_cast<uint8_t>(frame.data[idx + 2] * factor);
        }
    }
}

void ColorFilter::rgbToHsl(uint8_t r, uint8_t g, uint8_t b, float& h, float& s, float& l) {
    float rf = r / 255.0f;
    float gf = g / 255.0f;
    float bf = b / 255.0f;
    
    float maxVal = std::max({rf, gf, bf});
    float minVal = std::min({rf, gf, bf});
    float delta = maxVal - minVal;
    
    l = (maxVal + minVal) / 2.0f;
    
    if (delta == 0) {
        h = 0;
        s = 0;
    } else {
        s = l > 0.5f ? delta / (2.0f - maxVal - minVal) : delta / (maxVal + minVal);
        
        if (maxVal == rf) {
            h = (gf - bf) / delta + (gf < bf ? 6.0f : 0.0f);
        } else if (maxVal == gf) {
            h = (bf - rf) / delta + 2.0f;
        } else {
            h = (rf - gf) / delta + 4.0f;
        }
        
        h /= 6.0f;
    }
}

void ColorFilter::hslToRgb(float h, float s, float l, uint8_t& r, uint8_t& g, uint8_t& b) {
    auto hueToRgb = [](float p, float q, float t) -> float {
        if (t < 0) t += 1;
        if (t > 1) t -= 1;
        if (t < 1.0f/6.0f) return p + (q - p) * 6 * t;
        if (t < 1.0f/2.0f) return q;
        if (t < 2.0f/3.0f) return p + (q - p) * (2.0f/3.0f - t) * 6;
        return p;
    };
    
    float rf, gf, bf;
    
    if (s == 0) {
        rf = gf = bf = l;
    } else {
        float q = l < 0.5f ? l * (1 + s) : l + s - l * s;
        float p = 2 * l - q;
        rf = hueToRgb(p, q, h + 1.0f/3.0f);
        gf = hueToRgb(p, q, h);
        bf = hueToRgb(p, q, h - 1.0f/3.0f);
    }
    
    r = static_cast<uint8_t>(rf * 255);
    g = static_cast<uint8_t>(gf * 255);
    b = static_cast<uint8_t>(bf * 255);
}

}  // namespace videoeditor
