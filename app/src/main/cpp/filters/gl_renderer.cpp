#include "gl_renderer.h"

namespace videoeditor {

const char* DEFAULT_VERTEX_SHADER = R"(#version 300 es
layout(location = 0) in vec4 aPosition;
layout(location = 1) in vec2 aTexCoord;
out vec2 vTexCoord;
void main() {
    gl_Position = aPosition;
    vTexCoord = aTexCoord;
}
)";

const char* DEFAULT_FRAGMENT_SHADER = R"(#version 300 es
precision mediump float;
in vec2 vTexCoord;
out vec4 fragColor;
uniform sampler2D uTexture;
void main() {
    fragColor = texture(uTexture, vTexCoord);
}
)";

GLRenderer::GLRenderer()
    : m_display(EGL_NO_DISPLAY)
    , m_surface(EGL_NO_SURFACE)
    , m_context(EGL_NO_CONTEXT)
    , m_width(0)
    , m_height(0)
    , m_textureId(0)
    , m_defaultProgram(0)
    , m_vao(0)
    , m_vbo(0)
    , m_initialized(false) {
    LOGI("GLRenderer created");
}

GLRenderer::~GLRenderer() {
    release();
    LOGI("GLRenderer destroyed");
}

bool GLRenderer::initialize(ANativeWindow* window) {
    if (m_initialized) {
        return true;
    }
    
    if (!initEGL(window)) {
        LOGE("Failed to initialize EGL");
        return false;
    }
    
    // Create default shader
    m_defaultProgram = createProgram(DEFAULT_VERTEX_SHADER, DEFAULT_FRAGMENT_SHADER);
    if (m_defaultProgram == 0) {
        LOGE("Failed to create default shader program");
        return false;
    }
    
    // Create VAO and VBO for fullscreen quad
    float vertices[] = {
        // Position      // TexCoord
        -1.0f, -1.0f,   0.0f, 1.0f,
         1.0f, -1.0f,   1.0f, 1.0f,
        -1.0f,  1.0f,   0.0f, 0.0f,
         1.0f,  1.0f,   1.0f, 0.0f,
    };
    
    glGenVertexArrays(1, &m_vao);
    glGenBuffers(1, &m_vbo);
    
    glBindVertexArray(m_vao);
    glBindBuffer(GL_ARRAY_BUFFER, m_vbo);
    glBufferData(GL_ARRAY_BUFFER, sizeof(vertices), vertices, GL_STATIC_DRAW);
    
    glVertexAttribPointer(0, 2, GL_FLOAT, GL_FALSE, 4 * sizeof(float), (void*)0);
    glEnableVertexAttribArray(0);
    glVertexAttribPointer(1, 2, GL_FLOAT, GL_FALSE, 4 * sizeof(float), (void*)(2 * sizeof(float)));
    glEnableVertexAttribArray(1);
    
    // Create texture
    glGenTextures(1, &m_textureId);
    glBindTexture(GL_TEXTURE_2D, m_textureId);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    
    m_initialized = true;
    LOGI("GLRenderer initialized: %dx%d", m_width, m_height);
    return true;
}

bool GLRenderer::initEGL(ANativeWindow* window) {
    m_display = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if (m_display == EGL_NO_DISPLAY) {
        LOGE("eglGetDisplay failed");
        return false;
    }
    
    if (!eglInitialize(m_display, nullptr, nullptr)) {
        LOGE("eglInitialize failed");
        return false;
    }
    
    const EGLint configAttribs[] = {
        EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
        EGL_RED_SIZE, 8,
        EGL_GREEN_SIZE, 8,
        EGL_BLUE_SIZE, 8,
        EGL_ALPHA_SIZE, 8,
        EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT,
        EGL_NONE
    };
    
    EGLint numConfigs;
    if (!eglChooseConfig(m_display, configAttribs, &m_config, 1, &numConfigs) || numConfigs == 0) {
        LOGE("eglChooseConfig failed");
        return false;
    }
    
    const EGLint contextAttribs[] = {
        EGL_CONTEXT_CLIENT_VERSION, 3,
        EGL_NONE
    };
    
    m_context = eglCreateContext(m_display, m_config, EGL_NO_CONTEXT, contextAttribs);
    if (m_context == EGL_NO_CONTEXT) {
        LOGE("eglCreateContext failed");
        return false;
    }
    
    m_surface = eglCreateWindowSurface(m_display, m_config, window, nullptr);
    if (m_surface == EGL_NO_SURFACE) {
        LOGE("eglCreateWindowSurface failed");
        return false;
    }
    
    if (!eglMakeCurrent(m_display, m_surface, m_surface, m_context)) {
        LOGE("eglMakeCurrent failed");
        return false;
    }
    
    eglQuerySurface(m_display, m_surface, EGL_WIDTH, &m_width);
    eglQuerySurface(m_display, m_surface, EGL_HEIGHT, &m_height);
    
    glViewport(0, 0, m_width, m_height);
    
    return true;
}

void GLRenderer::release() {
    if (!m_initialized) {
        return;
    }
    
    if (m_textureId) {
        glDeleteTextures(1, &m_textureId);
        m_textureId = 0;
    }
    
    if (m_vbo) {
        glDeleteBuffers(1, &m_vbo);
        m_vbo = 0;
    }
    
    if (m_vao) {
        glDeleteVertexArrays(1, &m_vao);
        m_vao = 0;
    }
    
    if (m_defaultProgram) {
        glDeleteProgram(m_defaultProgram);
        m_defaultProgram = 0;
    }
    
    for (auto& pair : m_shaders) {
        glDeleteProgram(pair.second);
    }
    m_shaders.clear();
    
    releaseEGL();
    m_initialized = false;
    
    LOGI("GLRenderer released");
}

void GLRenderer::releaseEGL() {
    if (m_display != EGL_NO_DISPLAY) {
        eglMakeCurrent(m_display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        
        if (m_context != EGL_NO_CONTEXT) {
            eglDestroyContext(m_display, m_context);
            m_context = EGL_NO_CONTEXT;
        }
        
        if (m_surface != EGL_NO_SURFACE) {
            eglDestroySurface(m_display, m_surface);
            m_surface = EGL_NO_SURFACE;
        }
        
        eglTerminate(m_display);
        m_display = EGL_NO_DISPLAY;
    }
}

void GLRenderer::render(const VideoFrame& frame) {
    if (!m_initialized) return;
    
    // Upload texture
    glBindTexture(GL_TEXTURE_2D, m_textureId);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, frame.width, frame.height, 0, 
                 GL_RGBA, GL_UNSIGNED_BYTE, frame.data.data());
    
    // Draw
    GLuint program = m_currentShader.empty() ? m_defaultProgram : m_shaders[m_currentShader];
    glUseProgram(program);
    
    glBindVertexArray(m_vao);
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    
    eglSwapBuffers(m_display, m_surface);
}

void GLRenderer::clear(float r, float g, float b, float a) {
    glClearColor(r, g, b, a);
    glClear(GL_COLOR_BUFFER_BIT);
}

GLuint GLRenderer::compileShader(GLenum type, const char* source) {
    GLuint shader = glCreateShader(type);
    glShaderSource(shader, 1, &source, nullptr);
    glCompileShader(shader);
    
    GLint compiled;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &compiled);
    if (!compiled) {
        GLint infoLen = 0;
        glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &infoLen);
        if (infoLen > 0) {
            std::vector<char> info(infoLen);
            glGetShaderInfoLog(shader, infoLen, nullptr, info.data());
            LOGE("Shader compile error: %s", info.data());
        }
        glDeleteShader(shader);
        return 0;
    }
    
    return shader;
}

GLuint GLRenderer::createProgram(const char* vertexSrc, const char* fragmentSrc) {
    GLuint vertexShader = compileShader(GL_VERTEX_SHADER, vertexSrc);
    if (vertexShader == 0) return 0;
    
    GLuint fragmentShader = compileShader(GL_FRAGMENT_SHADER, fragmentSrc);
    if (fragmentShader == 0) {
        glDeleteShader(vertexShader);
        return 0;
    }
    
    GLuint program = glCreateProgram();
    glAttachShader(program, vertexShader);
    glAttachShader(program, fragmentShader);
    glLinkProgram(program);
    
    glDeleteShader(vertexShader);
    glDeleteShader(fragmentShader);
    
    GLint linked;
    glGetProgramiv(program, GL_LINK_STATUS, &linked);
    if (!linked) {
        GLint infoLen = 0;
        glGetProgramiv(program, GL_INFO_LOG_LENGTH, &infoLen);
        if (infoLen > 0) {
            std::vector<char> info(infoLen);
            glGetProgramInfoLog(program, infoLen, nullptr, info.data());
            LOGE("Program link error: %s", info.data());
        }
        glDeleteProgram(program);
        return 0;
    }
    
    return program;
}

bool GLRenderer::loadShader(const std::string& name, const std::string& vertexSrc, const std::string& fragmentSrc) {
    GLuint program = createProgram(vertexSrc.c_str(), fragmentSrc.c_str());
    if (program == 0) {
        return false;
    }
    
    if (m_shaders.find(name) != m_shaders.end()) {
        glDeleteProgram(m_shaders[name]);
    }
    
    m_shaders[name] = program;
    LOGI("Loaded shader: %s", name.c_str());
    return true;
}

void GLRenderer::useShader(const std::string& name) {
    m_currentShader = name;
}

void GLRenderer::setUniform(const std::string& name, float value) {
    GLuint program = m_currentShader.empty() ? m_defaultProgram : m_shaders[m_currentShader];
    GLint location = glGetUniformLocation(program, name.c_str());
    if (location >= 0) {
        glUniform1f(location, value);
    }
}

void GLRenderer::setUniform(const std::string& name, float* values, int count) {
    GLuint program = m_currentShader.empty() ? m_defaultProgram : m_shaders[m_currentShader];
    GLint location = glGetUniformLocation(program, name.c_str());
    if (location >= 0) {
        glUniform1fv(location, count, values);
    }
}

}  // namespace videoeditor
