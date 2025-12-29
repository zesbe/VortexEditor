package com.vortexeditor.app.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Audio effects processor
 */
object AudioEffects {

    /**
     * Apply volume adjustment
     */
    fun adjustVolume(samples: ShortArray, volume: Float): ShortArray {
        return ShortArray(samples.size) { i ->
            (samples[i] * volume).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    /**
     * Fade in effect
     */
    fun fadeIn(samples: ShortArray, durationSamples: Int): ShortArray {
        val result = samples.copyOf()
        for (i in 0 until minOf(durationSamples, samples.size)) {
            val factor = i.toFloat() / durationSamples
            result[i] = (samples[i] * factor).toInt().toShort()
        }
        return result
    }

    /**
     * Fade out effect
     */
    fun fadeOut(samples: ShortArray, durationSamples: Int): ShortArray {
        val result = samples.copyOf()
        val startIndex = maxOf(0, samples.size - durationSamples)
        for (i in startIndex until samples.size) {
            val factor = (samples.size - i).toFloat() / durationSamples
            result[i] = (samples[i] * factor).toInt().toShort()
        }
        return result
    }

    /**
     * Reverse audio
     */
    fun reverse(samples: ShortArray): ShortArray {
        return samples.reversedArray()
    }

    /**
     * Speed up/slow down (pitch changes)
     */
    fun changeSpeed(samples: ShortArray, speed: Float): ShortArray {
        val newLength = (samples.size / speed).toInt()
        return ShortArray(newLength) { i ->
            val srcIndex = (i * speed).toInt().coerceIn(0, samples.size - 1)
            samples[srcIndex]
        }
    }

    /**
     * Echo effect
     */
    fun echo(samples: ShortArray, delaySamples: Int, decay: Float): ShortArray {
        val result = samples.copyOf()
        for (i in delaySamples until samples.size) {
            val echo = (samples[i - delaySamples] * decay).toInt()
            result[i] = (result[i] + echo).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return result
    }

    /**
     * Reverb effect (simple)
     */
    fun reverb(samples: ShortArray, roomSize: Float = 0.5f): ShortArray {
        val delays = listOf(1557, 1617, 1491, 1422, 1277, 1356, 1188, 1116)
        val result = samples.copyOf()
        
        for (delay in delays) {
            val actualDelay = (delay * roomSize).toInt()
            val decay = 0.5f * roomSize
            
            for (i in actualDelay until samples.size) {
                val reverbSample = (samples[i - actualDelay] * decay).toInt()
                result[i] = (result[i] + reverbSample / delays.size)
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }
        
        return result
    }

    /**
     * Bass boost
     */
    fun bassBoost(samples: ShortArray, amount: Float): ShortArray {
        // Simple low-pass filter based bass boost
        val result = ShortArray(samples.size)
        var prev = 0f
        val alpha = 0.1f // Low-pass filter coefficient
        
        for (i in samples.indices) {
            val filtered = prev + alpha * (samples[i] - prev)
            prev = filtered
            val boosted = samples[i] + (filtered * amount).toInt()
            result[i] = boosted.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        
        return result
    }

    /**
     * Treble boost
     */
    fun trebleBoost(samples: ShortArray, amount: Float): ShortArray {
        val result = ShortArray(samples.size)
        
        for (i in 1 until samples.size) {
            val diff = samples[i] - samples[i - 1]
            val boosted = samples[i] + (diff * amount).toInt()
            result[i] = boosted.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        result[0] = samples[0]
        
        return result
    }

    /**
     * Normalize audio (maximize volume without clipping)
     */
    fun normalize(samples: ShortArray): ShortArray {
        var maxSample = 1
        for (sample in samples) {
            val abs = kotlin.math.abs(sample.toInt())
            if (abs > maxSample) maxSample = abs
        }
        
        val scale = Short.MAX_VALUE.toFloat() / maxSample
        return ShortArray(samples.size) { i ->
            (samples[i] * scale).toInt().toShort()
        }
    }

    /**
     * Remove silence from beginning and end
     */
    fun trimSilence(samples: ShortArray, threshold: Int = 500): ShortArray {
        var start = 0
        var end = samples.size - 1
        
        // Find start
        while (start < samples.size && kotlin.math.abs(samples[start].toInt()) < threshold) {
            start++
        }
        
        // Find end
        while (end > start && kotlin.math.abs(samples[end].toInt()) < threshold) {
            end--
        }
        
        return samples.copyOfRange(start, end + 1)
    }

    /**
     * Voice changer - pitch shift
     */
    fun pitchShift(samples: ShortArray, semitones: Float): ShortArray {
        val ratio = Math.pow(2.0, semitones / 12.0).toFloat()
        return changeSpeed(samples, ratio)
    }

    /**
     * Robot voice effect
     */
    fun robotVoice(samples: ShortArray, frequency: Float = 50f, sampleRate: Int = 44100): ShortArray {
        val result = ShortArray(samples.size)
        
        for (i in samples.indices) {
            val modulator = sin(2 * PI * frequency * i / sampleRate).toFloat()
            result[i] = (samples[i] * modulator).toInt().toShort()
        }
        
        return result
    }

    /**
     * Chipmunk effect (high pitch)
     */
    fun chipmunk(samples: ShortArray): ShortArray {
        return pitchShift(samples, 6f) // Shift up 6 semitones
    }

    /**
     * Deep voice effect (low pitch)
     */
    fun deepVoice(samples: ShortArray): ShortArray {
        return pitchShift(samples, -6f) // Shift down 6 semitones
    }

    /**
     * Telephone effect (band-pass filter)
     */
    fun telephoneEffect(samples: ShortArray): ShortArray {
        // Simple simulation - reduce bass and treble
        var result = bassBoost(samples, -0.8f)
        result = trebleBoost(result, -0.5f)
        return result
    }

    /**
     * Noise gate - remove low volume noise
     */
    fun noiseGate(samples: ShortArray, threshold: Int = 1000): ShortArray {
        return ShortArray(samples.size) { i ->
            if (kotlin.math.abs(samples[i].toInt()) < threshold) 0 else samples[i]
        }
    }

    /**
     * Compressor - reduce dynamic range
     */
    fun compress(samples: ShortArray, threshold: Float = 0.5f, ratio: Float = 4f): ShortArray {
        val thresholdValue = (Short.MAX_VALUE * threshold).toInt()
        
        return ShortArray(samples.size) { i ->
            val sample = samples[i].toInt()
            val abs = kotlin.math.abs(sample)
            
            if (abs > thresholdValue) {
                val excess = abs - thresholdValue
                val compressed = thresholdValue + (excess / ratio).toInt()
                (if (sample >= 0) compressed else -compressed).toShort()
            } else {
                samples[i]
            }
        }
    }
}
