cmake_minimum_required(VERSION 3.22.1)

project("videoeditor")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Add compile options for optimization
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -ffast-math")
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -O3 -ffast-math -DNDEBUG")

# Find required packages
find_library(log-lib log)
find_library(android-lib android)
find_library(jnigraphics-lib jnigraphics)
find_library(EGL-lib EGL)
find_library(GLESv3-lib GLESv3)
find_library(mediandk-lib mediandk)

# Include directories
include_directories(
    ${CMAKE_SOURCE_DIR}/include
    ${CMAKE_SOURCE_DIR}/engine
    ${CMAKE_SOURCE_DIR}/filters
    ${CMAKE_SOURCE_DIR}/utils
)

# Source files - Core Engine
set(ENGINE_SOURCES
    engine/video_engine.cpp
    engine/video_decoder.cpp
    engine/video_encoder.cpp
    engine/audio_engine.cpp
    engine/frame_buffer.cpp
    engine/timeline.cpp
)

# Source files - Filters & Effects
set(FILTER_SOURCES
    filters/filter_manager.cpp
    filters/color_filter.cpp
    filters/blur_filter.cpp
    filters/sharpen_filter.cpp
    filters/gl_renderer.cpp
)

# Source files - Utils
set(UTIL_SOURCES
    utils/thread_pool.cpp
    utils/memory_pool.cpp
    utils/image_utils.cpp
    utils/time_utils.cpp
)

# JNI Bridge
set(JNI_SOURCES
    jni/video_editor_jni.cpp
)

# Create shared library
add_library(
    videoeditor
    SHARED
    ${ENGINE_SOURCES}
    ${FILTER_SOURCES}
    ${UTIL_SOURCES}
    ${JNI_SOURCES}
)

# Link libraries
target_link_libraries(
    videoeditor
    ${log-lib}
    ${android-lib}
    ${jnigraphics-lib}
    ${EGL-lib}
    ${GLESv3-lib}
    ${mediandk-lib}
)
