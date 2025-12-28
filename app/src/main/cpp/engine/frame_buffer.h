#ifndef VIDEO_EDITOR_FRAME_BUFFER_H
#define VIDEO_EDITOR_FRAME_BUFFER_H

#include "common.h"

namespace videoeditor {

class FrameBuffer {
public:
    FrameBuffer(int width, int height);
    ~FrameBuffer();

    // Clear buffer to black
    void clear();

    // Composite source frame onto this buffer
    void composite(VideoFrame& dest, const VideoFrame& src, const ClipInfo& clip);

    // Apply alpha blending
    void blend(VideoFrame& dest, const VideoFrame& src, float alpha);

    // Scale frame to fit
    VideoFrame scale(const VideoFrame& src, int newWidth, int newHeight);

    // Crop frame
    VideoFrame crop(const VideoFrame& src, int x, int y, int width, int height);

    // Rotate frame
    VideoFrame rotate(const VideoFrame& src, int degrees);

    // Flip frame
    VideoFrame flipHorizontal(const VideoFrame& src);
    VideoFrame flipVertical(const VideoFrame& src);

    // Color conversion
    void rgbaToYuv420(const uint8_t* rgba, uint8_t* yuv, int width, int height);
    void yuv420ToRgba(const uint8_t* yuv, uint8_t* rgba, int width, int height);

    // Getters
    int getWidth() const { return m_width; }
    int getHeight() const { return m_height; }

private:
    int m_width;
    int m_height;
    std::vector<uint8_t> m_buffer;
    std::mutex m_mutex;
};

}  // namespace videoeditor

#endif  // VIDEO_EDITOR_FRAME_BUFFER_H
