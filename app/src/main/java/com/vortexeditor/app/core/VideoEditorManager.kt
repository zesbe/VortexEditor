package com.vortexeditor.app.core

import android.content.Context
import android.net.Uri
import android.view.Surface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VideoEditorManager(private val context: Context) {

    private val nativeEngine = NativeEngine()
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state

    private val _exportProgress = MutableStateFlow(0f)
    val exportProgress: StateFlow<Float> = _exportProgress

    data class EditorState(
        val isInitialized: Boolean = false,
        val isPlaying: Boolean = false,
        val currentPosition: Long = 0,
        val duration: Long = 0,
        val clips: List<ClipInfo> = emptyList(),
        val projectWidth: Int = 1920,
        val projectHeight: Int = 1080,
        val projectFps: Int = 30
    )

    data class ClipInfo(
        val id: Int,
        val filePath: String,
        val trackIndex: Int,
        val startTime: Long,
        val duration: Long,
        val thumbnailUri: Uri? = null
    )

    fun initialize(): Boolean {
        val result = nativeEngine.initialize()
        if (result) {
            _state.value = _state.value.copy(isInitialized = true)
        }
        return result
    }

    fun createProject(width: Int = 1920, height: Int = 1080, fps: Int = 30): Boolean {
        val result = nativeEngine.createProject(width, height, fps)
        if (result) {
            _state.value = _state.value.copy(
                projectWidth = width,
                projectHeight = height,
                projectFps = fps
            )
        }
        return result
    }

    fun setPreviewSurface(surface: Surface?) {
        nativeEngine.setPreviewSurface(surface)
    }

    suspend fun addClip(uri: Uri, trackIndex: Int = 0, position: Long = 0): Boolean {
        return withContext(Dispatchers.IO) {
            val filePath = getPathFromUri(uri) ?: return@withContext false
            val result = nativeEngine.addClip(filePath, trackIndex, position)
            if (result) {
                updateState()
            }
            result
        }
    }

    fun removeClip(clipId: Int): Boolean {
        val result = nativeEngine.removeClip(clipId)
        if (result) {
            updateState()
        }
        return result
    }

    fun trimClip(clipId: Int, trimStart: Long, trimEnd: Long): Boolean {
        return nativeEngine.trimClip(clipId, trimStart, trimEnd)
    }

    fun splitClip(clipId: Int, position: Long): Boolean {
        val result = nativeEngine.splitClip(clipId, position)
        if (result) {
            updateState()
        }
        return result
    }

    fun setClipSpeed(clipId: Int, speed: Float): Boolean {
        return nativeEngine.setClipSpeed(clipId, speed)
    }

    fun setClipVolume(clipId: Int, volume: Float): Boolean {
        return nativeEngine.setClipVolume(clipId, volume)
    }

    // Playback controls
    fun play() {
        nativeEngine.play()
        _state.value = _state.value.copy(isPlaying = true)
        startPositionUpdates()
    }

    fun pause() {
        nativeEngine.pause()
        _state.value = _state.value.copy(isPlaying = false)
    }

    fun stop() {
        nativeEngine.stop()
        _state.value = _state.value.copy(isPlaying = false, currentPosition = 0)
    }

    fun seekTo(position: Long) {
        nativeEngine.seekTo(position)
        _state.value = _state.value.copy(currentPosition = position)
    }

    // Filters
    fun addFilter(clipId: Int, filterType: String, intensity: Float): Boolean {
        return nativeEngine.addFilter(clipId, filterType, intensity)
    }

    fun removeFilter(clipId: Int, filterId: Int): Boolean {
        return nativeEngine.removeFilter(clipId, filterId)
    }

    // Audio
    suspend fun addAudioTrack(uri: Uri, position: Long = 0): Boolean {
        return withContext(Dispatchers.IO) {
            val filePath = getPathFromUri(uri) ?: return@withContext false
            nativeEngine.addAudioTrack(filePath, position)
        }
    }

    // Export
    suspend fun export(
        outputPath: String,
        width: Int = _state.value.projectWidth,
        height: Int = _state.value.projectHeight,
        fps: Int = _state.value.projectFps,
        bitrate: Int = 10_000_000
    ): Boolean {
        return withContext(Dispatchers.IO) {
            nativeEngine.export(outputPath, width, height, fps, bitrate,
                object : NativeEngine.ExportProgressCallback {
                    override fun onProgress(progress: Float, status: String) {
                        scope.launch {
                            _exportProgress.value = progress
                        }
                    }
                })
        }
    }

    fun cancelExport() {
        nativeEngine.cancelExport()
    }

    fun release() {
        nativeEngine.release()
        nativeEngine.destroy()
        _state.value = EditorState()
    }

    private fun updateState() {
        _state.value = _state.value.copy(
            duration = nativeEngine.getDuration(),
            currentPosition = nativeEngine.getCurrentPosition()
        )
    }

    private fun startPositionUpdates() {
        scope.launch {
            while (_state.value.isPlaying) {
                _state.value = _state.value.copy(
                    currentPosition = nativeEngine.getCurrentPosition(),
                    isPlaying = nativeEngine.isPlaying()
                )
                kotlinx.coroutines.delay(100)
            }
        }
    }

    private fun getPathFromUri(uri: Uri): String? {
        // Handle content:// URIs
        if (uri.scheme == "content") {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val tempFile = java.io.File(context.cacheDir, "temp_${System.currentTimeMillis()}")
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
                return tempFile.absolutePath
            }
        }
        return uri.path
    }
}
