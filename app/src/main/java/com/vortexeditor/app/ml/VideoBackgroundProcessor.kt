package com.vortexeditor.app.ml

import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Process video frames with background removal/replacement
 */
class VideoBackgroundProcessor {

    private val backgroundRemover = BackgroundRemover()

    sealed class ProcessingState {
        data class Progress(val percent: Float, val currentFrame: Int, val totalFrames: Int) : ProcessingState()
        data class Completed(val outputPath: String) : ProcessingState()
        data class Error(val message: String) : ProcessingState()
    }

    enum class BackgroundMode {
        TRANSPARENT,
        SOLID_COLOR,
        BLUR,
        IMAGE
    }

    data class ProcessingOptions(
        val mode: BackgroundMode = BackgroundMode.BLUR,
        val backgroundColor: Int = Color.GREEN,
        val backgroundImagePath: String? = null,
        val blurRadius: Int = 25
    )

    /**
     * Process video and remove/replace background
     * Returns Flow of processing states
     */
    fun processVideo(
        inputPath: String,
        outputPath: String,
        options: ProcessingOptions = ProcessingOptions()
    ): Flow<ProcessingState> = flow {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(inputPath)
            
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val frameRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toFloatOrNull() ?: 30f
            
            val totalFrames = ((durationMs / 1000f) * frameRate).toInt()
            val frameIntervalUs = (1_000_000 / frameRate).toLong()
            
            // Load background image if needed
            val backgroundBitmap = if (options.mode == BackgroundMode.IMAGE && options.backgroundImagePath != null) {
                android.graphics.BitmapFactory.decodeFile(options.backgroundImagePath)
            } else null
            
            var processedFrames = 0
            
            // Process each frame
            for (timeUs in 0 until (durationMs * 1000) step frameIntervalUs) {
                val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                
                if (frame != null) {
                    val processedFrame = withContext(Dispatchers.Default) {
                        processFrame(frame, options, backgroundBitmap)
                    }
                    
                    // TODO: Encode processed frame to output video
                    // For now, we're just demonstrating the frame processing
                    
                    processedFrame.recycle()
                    frame.recycle()
                }
                
                processedFrames++
                val progress = processedFrames.toFloat() / totalFrames
                emit(ProcessingState.Progress(progress * 100, processedFrames, totalFrames))
            }
            
            backgroundBitmap?.recycle()
            retriever.release()
            
            emit(ProcessingState.Completed(outputPath))
            
        } catch (e: Exception) {
            emit(ProcessingState.Error(e.message ?: "Unknown error"))
        }
    }

    private suspend fun processFrame(
        frame: Bitmap,
        options: ProcessingOptions,
        backgroundBitmap: Bitmap?
    ): Bitmap {
        return when (options.mode) {
            BackgroundMode.TRANSPARENT -> {
                backgroundRemover.removeBackground(frame)
            }
            BackgroundMode.SOLID_COLOR -> {
                backgroundRemover.replaceBackground(frame, options.backgroundColor)
            }
            BackgroundMode.BLUR -> {
                backgroundRemover.blurBackground(frame, options.blurRadius)
            }
            BackgroundMode.IMAGE -> {
                if (backgroundBitmap != null) {
                    backgroundRemover.replaceBackground(frame, backgroundBitmap)
                } else {
                    frame.copy(frame.config, true)
                }
            }
        }
    }

    /**
     * Quick preview - process single frame
     */
    suspend fun previewFrame(
        bitmap: Bitmap,
        options: ProcessingOptions,
        backgroundBitmap: Bitmap? = null
    ): Bitmap {
        return processFrame(bitmap, options, backgroundBitmap)
    }

    fun release() {
        backgroundRemover.close()
    }
}
