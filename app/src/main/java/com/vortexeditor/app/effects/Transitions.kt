package com.vortexeditor.app.effects

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader

/**
 * Video transitions - blend between two clips
 */
object Transitions {

    enum class TransitionType {
        FADE,
        DISSOLVE,
        WIPE_LEFT,
        WIPE_RIGHT,
        WIPE_UP,
        WIPE_DOWN,
        SLIDE_LEFT,
        SLIDE_RIGHT,
        ZOOM_IN,
        ZOOM_OUT,
        CIRCLE_OPEN,
        CIRCLE_CLOSE,
        BLUR
    }

    /**
     * Apply transition between two frames
     * @param from First frame (outgoing)
     * @param to Second frame (incoming)
     * @param progress Transition progress 0.0 to 1.0
     * @param type Type of transition
     */
    fun applyTransition(
        from: Bitmap,
        to: Bitmap,
        progress: Float,
        type: TransitionType
    ): Bitmap {
        return when (type) {
            TransitionType.FADE -> fade(from, to, progress)
            TransitionType.DISSOLVE -> dissolve(from, to, progress)
            TransitionType.WIPE_LEFT -> wipeHorizontal(from, to, progress, true)
            TransitionType.WIPE_RIGHT -> wipeHorizontal(from, to, progress, false)
            TransitionType.WIPE_UP -> wipeVertical(from, to, progress, true)
            TransitionType.WIPE_DOWN -> wipeVertical(from, to, progress, false)
            TransitionType.SLIDE_LEFT -> slideHorizontal(from, to, progress, true)
            TransitionType.SLIDE_RIGHT -> slideHorizontal(from, to, progress, false)
            TransitionType.ZOOM_IN -> zoom(from, to, progress, true)
            TransitionType.ZOOM_OUT -> zoom(from, to, progress, false)
            TransitionType.CIRCLE_OPEN -> circleTransition(from, to, progress, true)
            TransitionType.CIRCLE_CLOSE -> circleTransition(from, to, progress, false)
            TransitionType.BLUR -> blurTransition(from, to, progress)
        }
    }

    private fun fade(from: Bitmap, to: Bitmap, progress: Float): Bitmap {
        val width = from.width
        val height = from.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        
        val paintFrom = Paint().apply {
            alpha = ((1 - progress) * 255).toInt()
        }
        val paintTo = Paint().apply {
            alpha = (progress * 255).toInt()
        }
        
        canvas.drawBitmap(from, 0f, 0f, paintFrom)
        canvas.drawBitmap(Bitmap.createScaledBitmap(to, width, height, true), 0f, 0f, paintTo)
        
        return result
    }

    private fun dissolve(from: Bitmap, to: Bitmap, progress: Float): Bitmap {
        val width = from.width
        val height = from.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val scaledTo = Bitmap.createScaledBitmap(to, width, height, true)
        
        val random = java.util.Random(42) // Fixed seed for consistency
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val threshold = random.nextFloat()
                val pixel = if (progress > threshold) {
                    scaledTo.getPixel(x, y)
                } else {
                    from.getPixel(x, y)
                }
                result.setPixel(x, y, pixel)
            }
        }
        
        return result
    }

    private fun wipeHorizontal(from: Bitmap, to: Bitmap, progress: Float, leftToRight: Boolean): Bitmap {
        val width = from.width
        val height = from.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val scaledTo = Bitmap.createScaledBitmap(to, width, height, true)
        
        val wipeX = (width * progress).toInt()
        
        if (leftToRight) {
            // Draw 'to' on left, 'from' on right
            canvas.drawBitmap(scaledTo, 0f, 0f, null)
            canvas.save()
            canvas.clipRect(wipeX, 0, width, height)
            canvas.drawBitmap(from, 0f, 0f, null)
            canvas.restore()
        } else {
            // Draw 'to' on right, 'from' on left
            canvas.drawBitmap(from, 0f, 0f, null)
            canvas.save()
            canvas.clipRect(width - wipeX, 0, width, height)
            canvas.drawBitmap(scaledTo, 0f, 0f, null)
            canvas.restore()
        }
        
        return result
    }

    private fun wipeVertical(from: Bitmap, to: Bitmap, progress: Float, topToBottom: Boolean): Bitmap {
        val width = from.width
        val height = from.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val scaledTo = Bitmap.createScaledBitmap(to, width, height, true)
        
        val wipeY = (height * progress).toInt()
        
        if (topToBottom) {
            canvas.drawBitmap(scaledTo, 0f, 0f, null)
            canvas.save()
            canvas.clipRect(0, wipeY, width, height)
            canvas.drawBitmap(from, 0f, 0f, null)
            canvas.restore()
        } else {
            canvas.drawBitmap(from, 0f, 0f, null)
            canvas.save()
            canvas.clipRect(0, height - wipeY, width, height)
            canvas.drawBitmap(scaledTo, 0f, 0f, null)
            canvas.restore()
        }
        
        return result
    }

    private fun slideHorizontal(from: Bitmap, to: Bitmap, progress: Float, leftToRight: Boolean): Bitmap {
        val width = from.width
        val height = from.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val scaledTo = Bitmap.createScaledBitmap(to, width, height, true)
        
        val offset = (width * progress).toInt()
        
        if (leftToRight) {
            canvas.drawBitmap(from, -offset.toFloat(), 0f, null)
            canvas.drawBitmap(scaledTo, (width - offset).toFloat(), 0f, null)
        } else {
            canvas.drawBitmap(from, offset.toFloat(), 0f, null)
            canvas.drawBitmap(scaledTo, (offset - width).toFloat(), 0f, null)
        }
        
        return result
    }

    private fun zoom(from: Bitmap, to: Bitmap, progress: Float, zoomIn: Boolean): Bitmap {
        val width = from.width
        val height = from.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val scaledTo = Bitmap.createScaledBitmap(to, width, height, true)
        
        if (zoomIn) {
            // From zooms in and fades, To appears
            val scale = 1f + progress * 0.3f
            val alpha = ((1 - progress) * 255).toInt()
            
            canvas.drawBitmap(scaledTo, 0f, 0f, null)
            
            canvas.save()
            canvas.scale(scale, scale, width / 2f, height / 2f)
            val paint = Paint().apply { this.alpha = alpha }
            canvas.drawBitmap(from, 0f, 0f, paint)
            canvas.restore()
        } else {
            // To zooms out from large
            val scale = 1.3f - progress * 0.3f
            val alpha = (progress * 255).toInt()
            
            canvas.drawBitmap(from, 0f, 0f, null)
            
            canvas.save()
            canvas.scale(scale, scale, width / 2f, height / 2f)
            val paint = Paint().apply { this.alpha = alpha }
            canvas.drawBitmap(scaledTo, 0f, 0f, paint)
            canvas.restore()
        }
        
        return result
    }

    private fun circleTransition(from: Bitmap, to: Bitmap, progress: Float, open: Boolean): Bitmap {
        val width = from.width
        val height = from.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val scaledTo = Bitmap.createScaledBitmap(to, width, height, true)
        
        val maxRadius = Math.sqrt((width * width + height * height).toDouble()).toFloat() / 2
        val radius = if (open) progress * maxRadius else (1 - progress) * maxRadius
        
        val centerX = width / 2f
        val centerY = height / 2f
        
        // Draw background
        canvas.drawBitmap(if (open) from else scaledTo, 0f, 0f, null)
        
        // Create circular mask
        val maskBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val maskCanvas = Canvas(maskBitmap)
        val maskPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
        }
        maskCanvas.drawCircle(centerX, centerY, radius, maskPaint)
        
        // Apply mask
        val paint = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        
        val foreground = if (open) scaledTo else from
        val tempBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val tempCanvas = Canvas(tempBitmap)
        tempCanvas.drawBitmap(foreground, 0f, 0f, null)
        tempCanvas.drawBitmap(maskBitmap, 0f, 0f, paint)
        
        canvas.drawBitmap(tempBitmap, 0f, 0f, null)
        
        maskBitmap.recycle()
        tempBitmap.recycle()
        
        return result
    }

    private fun blurTransition(from: Bitmap, to: Bitmap, progress: Float): Bitmap {
        // First half: from gets blurrier
        // Second half: to gets sharper
        return if (progress < 0.5f) {
            val blurProgress = progress * 2 // 0 to 1
            val blurRadius = (blurProgress * 20).toInt().coerceAtLeast(1)
            simpleBlur(from, blurRadius)
        } else {
            val sharpProgress = (progress - 0.5f) * 2 // 0 to 1
            val blurRadius = ((1 - sharpProgress) * 20).toInt().coerceAtLeast(1)
            val scaledTo = Bitmap.createScaledBitmap(to, from.width, from.height, true)
            simpleBlur(scaledTo, blurRadius)
        }
    }

    private fun simpleBlur(bitmap: Bitmap, radius: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        // Simple box blur (faster but lower quality)
        val step = (radius / 3).coerceAtLeast(1)
        
        for (y in 0 until height step step) {
            for (x in 0 until width step step) {
                var r = 0
                var g = 0
                var b = 0
                var count = 0
                
                for (dy in -radius..radius step step) {
                    for (dx in -radius..radius step step) {
                        val nx = (x + dx).coerceIn(0, width - 1)
                        val ny = (y + dy).coerceIn(0, height - 1)
                        val pixel = bitmap.getPixel(nx, ny)
                        r += (pixel shr 16) and 0xFF
                        g += (pixel shr 8) and 0xFF
                        b += pixel and 0xFF
                        count++
                    }
                }
                
                val avgColor = Color.rgb(r / count, g / count, b / count)
                
                for (fy in y until (y + step).coerceAtMost(height)) {
                    for (fx in x until (x + step).coerceAtMost(width)) {
                        result.setPixel(fx, fy, avgColor)
                    }
                }
            }
        }
        
        return result
    }
}
