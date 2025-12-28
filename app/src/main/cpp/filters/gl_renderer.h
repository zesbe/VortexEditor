#ifndef VIDEO_EDITOR_GL_RENDERER_H
#define VIDEO_EDITOR_GL_RENDERER_H

#include "common.h"
#include <EGL/egl.h>
#include <GLES3/gl3.h>
#include <map>
#include <string>

namespace videoeditor {

class GLRenderer {
public:
    GLRenderer();
    ~GLRenderer();

    bool initialize(ANativeWindow* window);
    void release();

    void render(const VideoFrame& frame);
    void clear(float r, float g, float b, float a);

    // Shader-based effects
    bool loadShader(const std::string& name, const std::string& vertexSrc, const std::string& fragmentSrc);
    void useShader(const std::string& name);
    void setUniform(const std::string& name, float value);
    void setUniform(const std::string& name, float* values, int count);

private:
    bool initEGL(ANativeWindow* window);
    void releaseEGL();
    GLuint compileShader(GLenum type, const char* source);
    GLuint createProgram(const char* vertexSrc, const char* fragmentSrc);

    EGLDisplay m_display;
    EGLSurface m_surface;
    EGLContext m_context;
    EGLConfig m_config;
    
    int m_width;
    int m_height;
    
    GLuint m_textureId;
    GLuint m_defaultProgram;
    GLuint m_vao;
    GLuint m_vbo;
    
    std::map<std::string, GLuint> m_shaders;
    std::string m_currentShader;
    
    bool m_initialized;
};

}  // namespace videoeditor

#endif  // VIDEO_EDITOR_GL_RENDERER_H
