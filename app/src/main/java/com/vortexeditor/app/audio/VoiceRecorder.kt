package com.vortexeditor.app.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.coroutineContext

/**
 * Voice recorder for voiceover
 */
class VoiceRecorder(private val context: Context) {

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var outputFile: File? = null

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    sealed class RecordingState {
        object Idle : RecordingState()
        object Recording : RecordingState()
        data class AmplitudeUpdate(val amplitude: Int) : RecordingState()
        data class Completed(val filePath: String, val durationMs: Long) : RecordingState()
        data class Error(val message: String) : RecordingState()
    }

    /**
     * Check if has recording permission
     */
    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Start recording and return flow of states
     */
    fun startRecording(outputPath: String): Flow<RecordingState> = flow {
        if (!hasPermission()) {
            emit(RecordingState.Error("Recording permission not granted"))
            return@flow
        }

        try {
            outputFile = File(outputPath)
            val samples = mutableListOf<Short>()
            
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                emit(RecordingState.Error("Failed to initialize AudioRecord"))
                return@flow
            }

            audioRecord?.startRecording()
            isRecording = true
            emit(RecordingState.Recording)

            val buffer = ShortArray(bufferSize / 2)
            val startTime = System.currentTimeMillis()

            while (isRecording && coroutineContext.isActive) {
                val readResult = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                
                if (readResult > 0) {
                    // Store samples
                    for (i in 0 until readResult) {
                        samples.add(buffer[i])
                    }

                    // Calculate amplitude for visualization
                    var maxAmplitude = 0
                    for (i in 0 until readResult) {
                        val abs = kotlin.math.abs(buffer[i].toInt())
                        if (abs > maxAmplitude) maxAmplitude = abs
                    }
                    emit(RecordingState.AmplitudeUpdate(maxAmplitude))
                }
            }

            val durationMs = System.currentTimeMillis() - startTime

            // Stop recording
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null

            // Write WAV file
            withContext(Dispatchers.IO) {
                writeWavFile(outputPath, samples.toShortArray())
            }

            emit(RecordingState.Completed(outputPath, durationMs))

        } catch (e: Exception) {
            emit(RecordingState.Error(e.message ?: "Recording failed"))
            cleanup()
        }
    }

    /**
     * Stop recording
     */
    fun stopRecording() {
        isRecording = false
    }

    /**
     * Cancel recording
     */
    fun cancelRecording() {
        isRecording = false
        cleanup()
        outputFile?.delete()
    }

    private fun cleanup() {
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            // Ignore
        }
        audioRecord = null
    }

    private fun writeWavFile(path: String, samples: ShortArray) {
        val channels = 1
        val bytesPerSample = 2
        val dataSize = samples.size * bytesPerSample
        val fileSize = 44 + dataSize

        FileOutputStream(File(path)).use { fos ->
            // WAV header
            fos.write("RIFF".toByteArray())
            fos.write(intToBytes(fileSize - 8))
            fos.write("WAVE".toByteArray())
            fos.write("fmt ".toByteArray())
            fos.write(intToBytes(16))
            fos.write(shortToBytes(1))
            fos.write(shortToBytes(channels.toShort()))
            fos.write(intToBytes(sampleRate))
            fos.write(intToBytes(sampleRate * channels * bytesPerSample))
            fos.write(shortToBytes((channels * bytesPerSample).toShort()))
            fos.write(shortToBytes((bytesPerSample * 8).toShort()))
            fos.write("data".toByteArray())
            fos.write(intToBytes(dataSize))

            // Audio data
            val buffer = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            for (sample in samples) {
                buffer.putShort(sample)
            }
            fos.write(buffer.array())
        }
    }

    private fun intToBytes(value: Int): ByteArray {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()
    }

    private fun shortToBytes(value: Short): ByteArray {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array()
    }
}
