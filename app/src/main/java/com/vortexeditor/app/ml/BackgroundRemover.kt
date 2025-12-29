package com.vortexeditor.app.ml

import android.graphics.Bitmap
import android.graphics.Color
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.SegmentationMask
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class BackgroundRemover {

    private val options = SelfieSegmenterOptions.Builder()
        .setDetectorMode(SelfieSegmenterOptions.STREAM_MODE)
        .enableRawSizeMask()
        .build()

    private val segmenter = Segmentation.getClient(options)

    /**
     * Remove background from bitmap, returns bitmap with transparent background
     */
    suspend fun removeBackground(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val mask = getSegmentationMask(bitmap)
        applyMask(bitmap, mask)
    }

    /**
     * Replace background with solid color
     */
    suspend fun replaceBackground(bitmap: Bitmap, backgroundColor: Int): Bitmap = withContext(Dispatchers.Default) {
        val mask = getSegmentationMask(bitmap)
        applyMaskWithBackground(bitmap, mask, backgroundColor)
    }

    /**
     * Replace background with another image
     */
    suspend fun replaceBackground(foreground: Bitmap, background: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val mask = getSegmentationMask(foreground)
        compositeWithBackground(foreground, mask, background)
    }

    /**
     * Get blur background effect
     */
    suspend fun blurBackground(bitmap: Bitmap, blurRadius: Int = 25): Bitmap = withContext(Dispatchers.Default) {
        val mask = getSegmentationMask(bitmap)
        val blurredBg = blurBitmap(bitmap, blurRadius)
        compositeWithBackground(bitmap, mask, blurredBg)
    }

    private suspend fun getSegmentationMask(bitmap: Bitmap): SegmentationMask {
        return suspendCancellableCoroutine { continuation ->
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            
            segmenter.process(inputImage)
                .addOnSuccessListener { mask ->
                    continuation.resume(mask)
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        }
    }

    private fun applyMask(original: Bitmap, mask: SegmentationMask): Bitmap {
        val width = original.width
        val height = original.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        val maskBuffer = mask.buffer
        val maskWidth = mask.width
        val maskHeight = mask.height
        
        maskBuffer.rewind()
        
        val scaleX = maskWidth.toFloat() / width
        val scaleY = maskHeight.toFloat() / height
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val maskX = (x * scaleX).toInt().coerceIn(0, maskWidth - 1)
                val maskY = (y * scaleY).toInt().coerceIn(0, maskHeight - 1)
                
                val maskIndex = maskY * maskWidth + maskX
                maskBuffer.position(maskIndex * 4) // Float = 4 bytes
                val confidence = maskBuffer.float
                
                val originalPixel = original.getPixel(x, y)
                
                if (confidence > 0.5f) {
                    // Foreground - keep original
                    result.setPixel(x, y, originalPixel)
                } else {
                    // Background - make transparent
                    result.setPixel(x, y, Color.TRANSPARENT)
                }
            }
        }
        
        return result
    }

    private fun applyMaskWithBackground(original: Bitmap, mask: SegmentationMask, bgColor: Int): Bitmap {
        val width = original.width
        val height = original.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        val maskBuffer = mask.buffer
        val maskWidth = mask.width
        val maskHeight = mask.height
        
        maskBuffer.rewind()
        
        val scaleX = maskWidth.toFloat() / width
        val scaleY = maskHeight.toFloat() / height
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val maskX = (x * scaleX).toInt().coerceIn(0, maskWidth - 1)
                val maskY = (y * scaleY).toInt().coerceIn(0, maskHeight - 1)
                
                val maskIndex = maskY * maskWidth + maskX
                maskBuffer.position(maskIndex * 4)
                val confidence = maskBuffer.float
                
                val originalPixel = original.getPixel(x, y)
                
                if (confidence > 0.5f) {
                    result.setPixel(x, y, originalPixel)
                } else {
                    result.setPixel(x, y, bgColor)
                }
            }
        }
        
        return result
    }

    private fun compositeWithBackground(foreground: Bitmap, mask: SegmentationMask, background: Bitmap): Bitmap {
        val width = foreground.width
        val height = foreground.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        // Scale background to match foreground size
        val scaledBg = Bitmap.createScaledBitmap(background, width, height, true)
        
        val maskBuffer = mask.buffer
        val maskWidth = mask.width
        val maskHeight = mask.height
        
        maskBuffer.rewind()
        
        val scaleX = maskWidth.toFloat() / width
        val scaleY = maskHeight.toFloat() / height
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val maskX = (x * scaleX).toInt().coerceIn(0, maskWidth - 1)
                val maskY = (y * scaleY).toInt().coerceIn(0, maskHeight - 1)
                
                val maskIndex = maskY * maskWidth + maskX
                maskBuffer.position(maskIndex * 4)
                val confidence = maskBuffer.float
                
                val fgPixel = foreground.getPixel(x, y)
                val bgPixel = scaledBg.getPixel(x, y)
                
                // Blend based on confidence
                val blendedPixel = blendPixels(fgPixel, bgPixel, confidence)
                result.setPixel(x, y, blendedPixel)
            }
        }
        
        scaledBg.recycle()
        return result
    }

    private fun blendPixels(fg: Int, bg: Int, fgAlpha: Float): Int {
        val alpha = fgAlpha.coerceIn(0f, 1f)
        
        val fgR = Color.red(fg)
        val fgG = Color.green(fg)
        val fgB = Color.blue(fg)
        
        val bgR = Color.red(bg)
        val bgG = Color.green(bg)
        val bgB = Color.blue(bg)
        
        val r = (fgR * alpha + bgR * (1 - alpha)).toInt()
        val g = (fgG * alpha + bgG * (1 - alpha)).toInt()
        val b = (fgB * alpha + bgB * (1 - alpha)).toInt()
        
        return Color.rgb(r, g, b)
    }

    private fun blurBitmap(bitmap: Bitmap, radius: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        // Simple box blur
        for (y in 0 until height) {
            for (x in 0 until width) {
                var r = 0
                var g = 0
                var b = 0
                var count = 0
                
                for (dy in -radius..radius) {
                    for (dx in -radius..radius) {
                        val nx = (x + dx).coerceIn(0, width - 1)
                        val ny = (y + dy).coerceIn(0, height - 1)
                        val pixel = bitmap.getPixel(nx, ny)
                        r += Color.red(pixel)
                        g += Color.green(pixel)
                        b += Color.blue(pixel)
                        count++
                    }
                }
                
                result.setPixel(x, y, Color.rgb(r / count, g / count, b / count))
            }
        }
        
        return result
    }

    fun close() {
        segmenter.close()
    }
}
