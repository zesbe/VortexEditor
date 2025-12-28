package com.videoeditor.app.core

import android.view.Surface

class NativeEngine {
    private var nativeHandle: Long = 0

    init {
        System.loadLibrary("videoeditor")
        nativeHandle = nativeCreate()
    }

    fun initialize(): Boolean = nativeInitialize(nativeHandle)
    fun release() = nativeRelease(nativeHandle)

    fun createProject(width: Int, height: Int, fps: Int): Boolean =
        nativeCreateProject(nativeHandle, width, height, fps)

    // Clip operations
    fun addClip(filePath: String, trackIndex: Int, position: Long): Boolean =
        nativeAddClip(nativeHandle, filePath, trackIndex, position)

    fun removeClip(clipId: Int): Boolean = nativeRemoveClip(nativeHandle, clipId)
    fun trimClip(clipId: Int, trimStart: Long, trimEnd: Long): Boolean =
        nativeTrimClip(nativeHandle, clipId, trimStart, trimEnd)

    fun splitClip(clipId: Int, position: Long): Boolean =
        nativeSplitClip(nativeHandle, clipId, position)

    fun setClipSpeed(clipId: Int, speed: Float): Boolean =
        nativeSetClipSpeed(nativeHandle, clipId, speed)

    fun setClipVolume(clipId: Int, volume: Float): Boolean =
        nativeSetClipVolume(nativeHandle, clipId, volume)

    // Playback
    fun play() = nativePlay(nativeHandle)
    fun pause() = nativePause(nativeHandle)
    fun stop() = nativeStop(nativeHandle)
    fun seekTo(position: Long) = nativeSeekTo(nativeHandle, position)
    fun getCurrentPosition(): Long = nativeGetCurrentPosition(nativeHandle)
    fun getDuration(): Long = nativeGetDuration(nativeHandle)
    fun isPlaying(): Boolean = nativeIsPlaying(nativeHandle)

    // Preview
    fun setPreviewSurface(surface: Surface?) = nativeSetPreviewSurface(nativeHandle, surface)

    // Filters
    fun addFilter(clipId: Int, filterType: String, intensity: Float): Boolean =
        nativeAddFilter(nativeHandle, clipId, filterType, intensity)

    fun removeFilter(clipId: Int, filterId: Int): Boolean =
        nativeRemoveFilter(nativeHandle, clipId, filterId)

    // Audio
    fun addAudioTrack(filePath: String, position: Long): Boolean =
        nativeAddAudioTrack(nativeHandle, filePath, position)

    // Export
    fun export(
        outputPath: String,
        width: Int,
        height: Int,
        fps: Int,
        bitrate: Int,
        progressCallback: ExportProgressCallback
    ): Boolean = nativeExport(nativeHandle, outputPath, width, height, fps, bitrate, progressCallback)

    fun cancelExport() = nativeCancelExport(nativeHandle)

    fun destroy() {
        nativeDestroy(nativeHandle)
        nativeHandle = 0
    }

    interface ExportProgressCallback {
        fun onProgress(progress: Float, status: String)
    }

    // Native methods
    private external fun nativeCreate(): Long
    private external fun nativeDestroy(handle: Long)
    private external fun nativeInitialize(handle: Long): Boolean
    private external fun nativeRelease(handle: Long)
    private external fun nativeCreateProject(handle: Long, width: Int, height: Int, fps: Int): Boolean

    private external fun nativeAddClip(handle: Long, filePath: String, trackIndex: Int, position: Long): Boolean
    private external fun nativeRemoveClip(handle: Long, clipId: Int): Boolean
    private external fun nativeTrimClip(handle: Long, clipId: Int, trimStart: Long, trimEnd: Long): Boolean
    private external fun nativeSplitClip(handle: Long, clipId: Int, position: Long): Boolean
    private external fun nativeSetClipSpeed(handle: Long, clipId: Int, speed: Float): Boolean
    private external fun nativeSetClipVolume(handle: Long, clipId: Int, volume: Float): Boolean

    private external fun nativePlay(handle: Long)
    private external fun nativePause(handle: Long)
    private external fun nativeStop(handle: Long)
    private external fun nativeSeekTo(handle: Long, position: Long)
    private external fun nativeGetCurrentPosition(handle: Long): Long
    private external fun nativeGetDuration(handle: Long): Long
    private external fun nativeIsPlaying(handle: Long): Boolean

    private external fun nativeSetPreviewSurface(handle: Long, surface: Surface?)

    private external fun nativeAddFilter(handle: Long, clipId: Int, filterType: String, intensity: Float): Boolean
    private external fun nativeRemoveFilter(handle: Long, clipId: Int, filterId: Int): Boolean

    private external fun nativeAddAudioTrack(handle: Long, filePath: String, position: Long): Boolean

    private external fun nativeExport(
        handle: Long, outputPath: String, width: Int, height: Int, fps: Int, bitrate: Int,
        progressCallback: ExportProgressCallback
    ): Boolean
    private external fun nativeCancelExport(handle: Long)
}
