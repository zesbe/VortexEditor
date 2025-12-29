package com.vortexeditor.app.effects

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

/**
 * Video effects library - all on-device processing
 */
object VideoEffects {

    // ============== COLOR FILTERS ==============
    
    fun applyBrightness(bitmap: Bitmap, value: Float): Bitmap {
        // value: -100 to 100
        val cm = ColorMatrix().apply {
            set(floatArrayOf(
                1f, 0f, 0f, 0f, value,
                0f, 1f, 0f, 0f, value,
                0f, 0f, 1f, 0f, value,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        return applyColorMatrix(bitmap, cm)
    }

    fun applyContrast(bitmap: Bitmap, value: Float): Bitmap {
        // value: 0 to 2 (1 = normal)
        val translate = (-.5f * value + .5f) * 255f
        val cm = ColorMatrix().apply {
            set(floatArrayOf(
                value, 0f, 0f, 0f, translate,
                0f, value, 0f, 0f, translate,
                0f, 0f, value, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        return applyColorMatrix(bitmap, cm)
    }

    fun applySaturation(bitmap: Bitmap, value: Float): Bitmap {
        // value: 0 to 2 (1 = normal, 0 = grayscale)
        val cm = ColorMatrix().apply {
            setSaturation(value)
        }
        return applyColorMatrix(bitmap, cm)
    }

    fun applySepia(bitmap: Bitmap, intensity: Float = 1f): Bitmap {
        val cm = ColorMatrix().apply {
            set(floatArrayOf(
                0.393f * intensity + (1 - intensity), 0.769f * intensity, 0.189f * intensity, 0f, 0f,
                0.349f * intensity, 0.686f * intensity + (1 - intensity), 0.168f * intensity, 0f, 0f,
                0.272f * intensity, 0.534f * intensity, 0.131f * intensity + (1 - intensity), 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        return applyColorMatrix(bitmap, cm)
    }

    fun applyGrayscale(bitmap: Bitmap): Bitmap {
        val cm = ColorMatrix().apply {
            setSaturation(0f)
        }
        return applyColorMatrix(bitmap, cm)
    }

    fun applyInvert(bitmap: Bitmap): Bitmap {
        val cm = ColorMatrix().apply {
            set(floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        return applyColorMatrix(bitmap, cm)
    }

    fun applyWarmth(bitmap: Bitmap, value: Float): Bitmap {
        // value: -100 to 100
        val cm = ColorMatrix().apply {
            set(floatArrayOf(
                1f + value / 100f, 0f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f - value / 100f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        return applyColorMatrix(bitmap, cm)
    }

    fun applyTint(bitmap: Bitmap, value: Float): Bitmap {
        // value: -100 to 100 (green to magenta)
        val cm = ColorMatrix().apply {
            set(floatArrayOf(
                1f, 0f, 0f, 0f, value / 2,
                0f, 1f, 0f, 0f, -value / 2,
                0f, 0f, 1f, 0f, value / 2,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        return applyColorMatrix(bitmap, cm)
    }

    // ============== PRESET FILTERS ==============

    fun applyVintage(bitmap: Bitmap): Bitmap {
        var result = applySepia(bitmap, 0.4f)
        result = applyContrast(result, 1.1f)
        result = applyBrightness(result, -10f)
        return applyVignette(result, 0.3f)
    }

    fun applyCool(bitmap: Bitmap): Bitmap {
        val cm = ColorMatrix().apply {
            set(floatArrayOf(
                0.9f, 0f, 0f, 0f, 0f,
                0f, 0.95f, 0f, 0f, 0f,
                0f, 0f, 1.1f, 0f, 10f,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        return applyColorMatrix(bitmap, cm)
    }

    fun applyWarm(bitmap: Bitmap): Bitmap {
        val cm = ColorMatrix().apply {
            set(floatArrayOf(
                1.1f, 0f, 0f, 0f, 10f,
                0f, 1.0f, 0f, 0f, 5f,
                0f, 0f, 0.9f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        return applyColorMatrix(bitmap, cm)
    }

    fun applyDramatic(bitmap: Bitmap): Bitmap {
        var result = applyContrast(bitmap, 1.3f)
        result = applySaturation(result, 1.2f)
        return applyVignette(result, 0.4f)
    }

    fun applyFade(bitmap: Bitmap): Bitmap {
        var result = applyContrast(bitmap, 0.85f)
        result = applySaturation(result, 0.8f)
        return applyBrightness(result, 15f)
    }

    fun applyNoir(bitmap: Bitmap): Bitmap {
        var result = applyGrayscale(bitmap)
        result = applyContrast(result, 1.4f)
        return applyVignette(result, 0.5f)
    }

    // ============== SPECIAL EFFECTS ==============

    fun applyVignette(bitmap: Bitmap, intensity: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        
        val centerX = width / 2f
        val centerY = height / 2f
        val maxDist = Math.sqrt((centerX * centerX + centerY * centerY).toDouble()).toFloat()
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val dx = x - centerX
                val dy = y - centerY
                val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                val factor = 1f - intensity * (dist / maxDist) * (dist / maxDist)
                
                val pixel = result.getPixel(x, y)
                val r = ((pixel shr 16 and 0xFF) * factor).toInt().coerceIn(0, 255)
                val g = ((pixel shr 8 and 0xFF) * factor).toInt().coerceIn(0, 255)
                val b = ((pixel and 0xFF) * factor).toInt().coerceIn(0, 255)
                val a = pixel shr 24 and 0xFF
                
                result.setPixel(x, y, (a shl 24) or (r shl 16) or (g shl 8) or b)
            }
        }
        
        return result
    }

    fun applyGrain(bitmap: Bitmap, intensity: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val random = java.util.Random()
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = result.getPixel(x, y)
                val noise = ((random.nextFloat() - 0.5f) * intensity * 50).toInt()
                
                val r = ((pixel shr 16 and 0xFF) + noise).coerceIn(0, 255)
                val g = ((pixel shr 8 and 0xFF) + noise).coerceIn(0, 255)
                val b = ((pixel and 0xFF) + noise).coerceIn(0, 255)
                val a = pixel shr 24 and 0xFF
                
                result.setPixel(x, y, (a shl 24) or (r shl 16) or (g shl 8) or b)
            }
        }
        
        return result
    }

    // ============== HELPER ==============

    private fun applyColorMatrix(bitmap: Bitmap, cm: ColorMatrix): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }
}
