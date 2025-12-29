package com.vortexeditor.app.export

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import kotlin.coroutines.coroutineContext

/**
 * Export video with all effects applied
 */
class VideoExporter(private val context: Context) {

    enum class Resolution(val width: Int, val height: Int) {
        HD_720P(1280, 720),
        FHD_1080P(1920, 1080),
        UHD_4K(3840, 2160)
    }

    enum class Quality(val bitrateFactor: Float) {
        LOW(0.5f),
        MEDIUM(1.0f),
        HIGH(2.0f)
    }

    data class ExportConfig(
        val resolution: Resolution = Resolution.FHD_1080P,
        val quality: Quality = Quality.HIGH,
        val frameRate: Int = 30,
        val audioBitrate: Int = 192000,
        val outputFileName: String = "VortexEditor_${System.currentTimeMillis()}.mp4"
    )

    sealed class ExportState {
        object Preparing : ExportState()
        data class Progress(val percent: Float, val stage: String) : ExportState()
        data class Completed(val outputPath: String, val fileSize: Long) : ExportState()
        data class Error(val message: String) : ExportState()
    }

    /**
     * Export video with given configuration
     */
    fun export(
        inputPath: String,
        config: ExportConfig = ExportConfig()
    ): Flow<ExportState> = flow {
        emit(ExportState.Preparing)

        try {
            val outputPath = getOutputPath(config.outputFileName)
            
            emit(ExportState.Progress(5f, "Initializing encoder..."))
            
            val extractor = MediaExtractor()
            extractor.setDataSource(inputPath)

            // Find video and audio tracks
            var videoTrackIndex = -1
            var audioTrackIndex = -1
            
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                
                if (mime.startsWith("video/") && videoTrackIndex < 0) {
                    videoTrackIndex = i
                } else if (mime.startsWith("audio/") && audioTrackIndex < 0) {
                    audioTrackIndex = i
                }
            }

            if (videoTrackIndex < 0) {
                emit(ExportState.Error("No video track found"))
                return@flow
            }

            // Get source video properties
            extractor.selectTrack(videoTrackIndex)
            val sourceFormat = extractor.getTrackFormat(videoTrackIndex)
            val duration = sourceFormat.getLong(MediaFormat.KEY_DURATION)

            emit(ExportState.Progress(10f, "Processing video..."))

            // Create muxer
            val muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            // Create encoder format
            val videoBitrate = calculateBitrate(config.resolution, config.quality, config.frameRate)
            val outputFormat = createOutputFormat(config, videoBitrate)

            // Setup decoder and encoder
            val decoderMime = sourceFormat.getString(MediaFormat.KEY_MIME)!!
            val decoder = MediaCodec.createDecoderByType(decoderMime)
            val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)

            decoder.configure(sourceFormat, null, null, 0)
            encoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

            decoder.start()
            encoder.start()

            var muxerVideoTrack = -1
            var muxerAudioTrack = -1
            var muxerStarted = false

            val bufferInfo = MediaCodec.BufferInfo()
            var sawInputEOS = false
            var sawOutputEOS = false

            // Process video
            while (!sawOutputEOS && coroutineContext.isActive) {
                // Feed decoder
                if (!sawInputEOS) {
                    val inputBufferIndex = decoder.dequeueInputBuffer(10000)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = decoder.getInputBuffer(inputBufferIndex)!!
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)

                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            sawInputEOS = true
                        } else {
                            decoder.queueInputBuffer(inputBufferIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                // Get decoded frame and encode
                val outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 10000)
                if (outputBufferIndex >= 0) {
                    val decodedBuffer = decoder.getOutputBuffer(outputBufferIndex)

                    // TODO: Apply effects to frame here
                    
                    // Queue to encoder
                    val encoderInputIndex = encoder.dequeueInputBuffer(10000)
                    if (encoderInputIndex >= 0) {
                        val encoderInputBuffer = encoder.getInputBuffer(encoderInputIndex)!!
                        encoderInputBuffer.put(decodedBuffer)
                        encoder.queueInputBuffer(
                            encoderInputIndex, 0, bufferInfo.size,
                            bufferInfo.presentationTimeUs, bufferInfo.flags
                        )
                    }

                    decoder.releaseOutputBuffer(outputBufferIndex, false)

                    // Get encoded data
                    val encoderOutputIndex = encoder.dequeueOutputBuffer(bufferInfo, 10000)
                    if (encoderOutputIndex >= 0) {
                        val encodedBuffer = encoder.getOutputBuffer(encoderOutputIndex)!!

                        if (!muxerStarted) {
                            muxerVideoTrack = muxer.addTrack(encoder.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }

                        if (bufferInfo.size > 0) {
                            muxer.writeSampleData(muxerVideoTrack, encodedBuffer, bufferInfo)
                        }

                        encoder.releaseOutputBuffer(encoderOutputIndex, false)

                        // Update progress
                        val progress = 10f + (bufferInfo.presentationTimeUs.toFloat() / duration) * 80f
                        emit(ExportState.Progress(progress, "Encoding video..."))

                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            sawOutputEOS = true
                        }
                    }
                }
            }

            emit(ExportState.Progress(90f, "Processing audio..."))

            // Copy audio track (if exists)
            if (audioTrackIndex >= 0) {
                extractor.unselectTrack(videoTrackIndex)
                extractor.selectTrack(audioTrackIndex)
                extractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

                val audioFormat = extractor.getTrackFormat(audioTrackIndex)
                muxerAudioTrack = muxer.addTrack(audioFormat)

                val audioBuffer = ByteBuffer.allocate(1024 * 1024)
                val audioInfo = MediaCodec.BufferInfo()

                while (true) {
                    audioInfo.size = extractor.readSampleData(audioBuffer, 0)
                    if (audioInfo.size < 0) break

                    audioInfo.presentationTimeUs = extractor.sampleTime
                    audioInfo.flags = extractor.sampleFlags
                    muxer.writeSampleData(muxerAudioTrack, audioBuffer, audioInfo)
                    extractor.advance()
                }
            }

            emit(ExportState.Progress(95f, "Finalizing..."))

            // Cleanup
            decoder.stop()
            decoder.release()
            encoder.stop()
            encoder.release()
            muxer.stop()
            muxer.release()
            extractor.release()

            // Save to gallery
            val fileSize = File(outputPath).length()
            saveToGallery(outputPath, config.outputFileName)

            emit(ExportState.Progress(100f, "Done!"))
            emit(ExportState.Completed(outputPath, fileSize))

        } catch (e: Exception) {
            emit(ExportState.Error(e.message ?: "Export failed"))
        }
    }

    private fun getOutputPath(fileName: String): String {
        val moviesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        return File(moviesDir, fileName).absolutePath
    }

    private fun calculateBitrate(resolution: Resolution, quality: Quality, fps: Int): Int {
        val baseBitrate = when (resolution) {
            Resolution.HD_720P -> 5_000_000
            Resolution.FHD_1080P -> 10_000_000
            Resolution.UHD_4K -> 35_000_000
        }
        return (baseBitrate * quality.bitrateFactor * (fps / 30f)).toInt()
    }

    private fun createOutputFormat(config: ExportConfig, bitrate: Int): MediaFormat {
        return MediaFormat.createVideoFormat(
            MediaFormat.MIMETYPE_VIDEO_AVC,
            config.resolution.width,
            config.resolution.height
        ).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, config.frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
    }

    private fun saveToGallery(filePath: String, fileName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/VortexEditor")
                put(MediaStore.Video.Media.IS_PENDING, 0)
            }
            
            context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)?.let { uri ->
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    File(filePath).inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }
}
