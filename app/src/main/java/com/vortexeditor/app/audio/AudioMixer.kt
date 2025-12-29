package com.vortexeditor.app.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Audio mixer - mix multiple audio tracks
 */
class AudioMixer {

    data class AudioTrack(
        val id: Int,
        val filePath: String,
        var startTimeMs: Long = 0,
        var volume: Float = 1.0f,
        var fadeInMs: Long = 0,
        var fadeOutMs: Long = 0,
        var isMuted: Boolean = false,
        var pan: Float = 0f  // -1 (left) to 1 (right)
    )

    private val tracks = mutableListOf<AudioTrack>()
    private var nextTrackId = 1

    fun addTrack(filePath: String, startTimeMs: Long = 0): AudioTrack {
        val track = AudioTrack(
            id = nextTrackId++,
            filePath = filePath,
            startTimeMs = startTimeMs
        )
        tracks.add(track)
        return track
    }

    fun removeTrack(trackId: Int) {
        tracks.removeAll { it.id == trackId }
    }

    fun setVolume(trackId: Int, volume: Float) {
        tracks.find { it.id == trackId }?.volume = volume.coerceIn(0f, 2f)
    }

    fun setPan(trackId: Int, pan: Float) {
        tracks.find { it.id == trackId }?.pan = pan.coerceIn(-1f, 1f)
    }

    fun setFadeIn(trackId: Int, durationMs: Long) {
        tracks.find { it.id == trackId }?.fadeInMs = durationMs
    }

    fun setFadeOut(trackId: Int, durationMs: Long) {
        tracks.find { it.id == trackId }?.fadeOutMs = durationMs
    }

    fun setMuted(trackId: Int, muted: Boolean) {
        tracks.find { it.id == trackId }?.isMuted = muted
    }

    fun getTrack(trackId: Int): AudioTrack? = tracks.find { it.id == trackId }

    fun getAllTracks(): List<AudioTrack> = tracks.toList()

    /**
     * Mix all tracks into single audio file
     */
    suspend fun mixToFile(outputPath: String, durationMs: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val sampleRate = 44100
            val channels = 2
            val bytesPerSample = 2
            val totalSamples = (durationMs * sampleRate / 1000).toInt()
            
            // Create output buffer
            val outputBuffer = ShortArray(totalSamples * channels)
            
            // Mix each track
            for (track in tracks) {
                if (track.isMuted) continue
                
                val trackSamples = decodeAudioFile(track.filePath, sampleRate, channels)
                if (trackSamples != null) {
                    mixTrackIntoBuffer(outputBuffer, trackSamples, track, sampleRate)
                }
            }
            
            // Normalize to prevent clipping
            normalizeBuffer(outputBuffer)
            
            // Write to WAV file
            writeWavFile(outputPath, outputBuffer, sampleRate, channels)
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun mixTrackIntoBuffer(
        output: ShortArray,
        trackSamples: ShortArray,
        track: AudioTrack,
        sampleRate: Int
    ) {
        val startSample = (track.startTimeMs * sampleRate / 1000).toInt() * 2
        val trackDurationMs = trackSamples.size / 2 * 1000L / sampleRate
        
        for (i in trackSamples.indices step 2) {
            val outputIndex = startSample + i
            if (outputIndex >= output.size - 1) break
            
            val timeMs = (i / 2) * 1000L / sampleRate
            
            // Calculate volume with fades
            var volume = track.volume
            
            // Fade in
            if (track.fadeInMs > 0 && timeMs < track.fadeInMs) {
                volume *= timeMs.toFloat() / track.fadeInMs
            }
            
            // Fade out
            val timeFromEnd = trackDurationMs - timeMs
            if (track.fadeOutMs > 0 && timeFromEnd < track.fadeOutMs) {
                volume *= timeFromEnd.toFloat() / track.fadeOutMs
            }
            
            // Apply pan
            val leftVolume = volume * (1 - track.pan.coerceAtLeast(0f))
            val rightVolume = volume * (1 + track.pan.coerceAtMost(0f))
            
            // Mix left channel
            val leftSample = (trackSamples[i] * leftVolume).toInt()
            output[outputIndex] = (output[outputIndex] + leftSample).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            
            // Mix right channel
            val rightSample = (trackSamples[i + 1] * rightVolume).toInt()
            output[outputIndex + 1] = (output[outputIndex + 1] + rightSample).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    private fun normalizeBuffer(buffer: ShortArray) {
        var maxSample = 1
        for (sample in buffer) {
            val abs = kotlin.math.abs(sample.toInt())
            if (abs > maxSample) maxSample = abs
        }
        
        if (maxSample > Short.MAX_VALUE) {
            val scale = Short.MAX_VALUE.toFloat() / maxSample
            for (i in buffer.indices) {
                buffer[i] = (buffer[i] * scale).toInt().toShort()
            }
        }
    }

    private fun decodeAudioFile(filePath: String, targetSampleRate: Int, targetChannels: Int): ShortArray? {
        return try {
            val extractor = MediaExtractor()
            extractor.setDataSource(filePath)
            
            var audioTrackIndex = -1
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime?.startsWith("audio/") == true) {
                    audioTrackIndex = i
                    break
                }
            }
            
            if (audioTrackIndex < 0) return null
            
            extractor.selectTrack(audioTrackIndex)
            val format = extractor.getTrackFormat(audioTrackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
            
            val decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(format, null, null, 0)
            decoder.start()
            
            val samples = mutableListOf<Short>()
            val bufferInfo = MediaCodec.BufferInfo()
            var sawInputEOS = false
            var sawOutputEOS = false
            
            while (!sawOutputEOS) {
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
                
                val outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 10000)
                if (outputBufferIndex >= 0) {
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        sawOutputEOS = true
                    }
                    
                    val outputBuffer = decoder.getOutputBuffer(outputBufferIndex)!!
                    val shortBuffer = outputBuffer.order(ByteOrder.nativeOrder()).asShortBuffer()
                    while (shortBuffer.hasRemaining()) {
                        samples.add(shortBuffer.get())
                    }
                    
                    decoder.releaseOutputBuffer(outputBufferIndex, false)
                }
            }
            
            decoder.stop()
            decoder.release()
            extractor.release()
            
            samples.toShortArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun writeWavFile(path: String, samples: ShortArray, sampleRate: Int, channels: Int) {
        val bytesPerSample = 2
        val dataSize = samples.size * bytesPerSample
        val fileSize = 44 + dataSize
        
        FileOutputStream(File(path)).use { fos ->
            // WAV header
            fos.write("RIFF".toByteArray())
            fos.write(intToBytes(fileSize - 8))
            fos.write("WAVE".toByteArray())
            fos.write("fmt ".toByteArray())
            fos.write(intToBytes(16)) // Subchunk1Size
            fos.write(shortToBytes(1)) // AudioFormat (PCM)
            fos.write(shortToBytes(channels.toShort()))
            fos.write(intToBytes(sampleRate))
            fos.write(intToBytes(sampleRate * channels * bytesPerSample)) // ByteRate
            fos.write(shortToBytes((channels * bytesPerSample).toShort())) // BlockAlign
            fos.write(shortToBytes((bytesPerSample * 8).toShort())) // BitsPerSample
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
