package com.vortexeditor.app.text

import android.graphics.*

/**
 * Text overlay for video - supports animations and styles
 */
data class TextOverlay(
    val id: Int,
    var text: String,
    var x: Float,  // 0.0 to 1.0 (relative to video width)
    var y: Float,  // 0.0 to 1.0 (relative to video height)
    var fontSize: Float = 48f,
    var fontFamily: String = "sans-serif",
    var color: Int = Color.WHITE,
    var backgroundColor: Int = Color.TRANSPARENT,
    var strokeColor: Int = Color.BLACK,
    var strokeWidth: Float = 2f,
    var rotation: Float = 0f,
    var alpha: Float = 1f,
    var startTimeMs: Long = 0,
    var endTimeMs: Long = Long.MAX_VALUE,
    var animation: TextAnimation = TextAnimation.NONE,
    var animationDurationMs: Long = 500,
    var shadow: Boolean = true,
    var bold: Boolean = false,
    var italic: Boolean = false,
    var alignment: Paint.Align = Paint.Align.CENTER
)

enum class TextAnimation {
    NONE,
    FADE_IN,
    FADE_OUT,
    FADE_IN_OUT,
    SLIDE_LEFT,
    SLIDE_RIGHT,
    SLIDE_UP,
    SLIDE_DOWN,
    SCALE_UP,
    SCALE_DOWN,
    TYPEWRITER,
    BOUNCE
}

class TextRenderer {

    private val textPaint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    private val strokePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        textAlign = Paint.Align.CENTER
    }

    private val bgPaint = Paint().apply {
        isAntiAlias = true
    }

    /**
     * Render text overlay onto a bitmap
     */
    fun render(
        bitmap: Bitmap,
        overlay: TextOverlay,
        currentTimeMs: Long
    ): Bitmap {
        // Check if text should be visible
        if (currentTimeMs < overlay.startTimeMs || currentTimeMs > overlay.endTimeMs) {
            return bitmap
        }

        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        val width = bitmap.width.toFloat()
        val height = bitmap.height.toFloat()

        // Calculate position
        var posX = overlay.x * width
        var posY = overlay.y * height

        // Calculate animation progress
        val animProgress = calculateAnimationProgress(overlay, currentTimeMs)
        var alpha = overlay.alpha
        var scale = 1f
        var textToRender = overlay.text

        // Apply animation
        when (overlay.animation) {
            TextAnimation.FADE_IN -> {
                alpha = animProgress * overlay.alpha
            }
            TextAnimation.FADE_OUT -> {
                val outProgress = calculateOutProgress(overlay, currentTimeMs)
                alpha = (1 - outProgress) * overlay.alpha
            }
            TextAnimation.FADE_IN_OUT -> {
                val inProgress = animProgress
                val outProgress = calculateOutProgress(overlay, currentTimeMs)
                alpha = minOf(inProgress, 1 - outProgress) * overlay.alpha
            }
            TextAnimation.SLIDE_LEFT -> {
                posX = width + (overlay.x * width - width) * animProgress
            }
            TextAnimation.SLIDE_RIGHT -> {
                posX = -width + (overlay.x * width + width) * animProgress
            }
            TextAnimation.SLIDE_UP -> {
                posY = height + (overlay.y * height - height) * animProgress
            }
            TextAnimation.SLIDE_DOWN -> {
                posY = -overlay.fontSize + (overlay.y * height + overlay.fontSize) * animProgress
            }
            TextAnimation.SCALE_UP -> {
                scale = animProgress
            }
            TextAnimation.SCALE_DOWN -> {
                scale = 2f - animProgress
            }
            TextAnimation.TYPEWRITER -> {
                val charCount = (overlay.text.length * animProgress).toInt()
                textToRender = overlay.text.take(charCount)
            }
            TextAnimation.BOUNCE -> {
                val bounceProgress = Math.sin(animProgress * Math.PI * 3).toFloat()
                posY += bounceProgress * 20
            }
            TextAnimation.NONE -> { }
        }

        // Setup paints
        textPaint.apply {
            textSize = overlay.fontSize * scale
            color = overlay.color
            this.alpha = (alpha * 255).toInt()
            textAlign = overlay.alignment
            typeface = Typeface.create(
                overlay.fontFamily,
                when {
                    overlay.bold && overlay.italic -> Typeface.BOLD_ITALIC
                    overlay.bold -> Typeface.BOLD
                    overlay.italic -> Typeface.ITALIC
                    else -> Typeface.NORMAL
                }
            )
            if (overlay.shadow) {
                setShadowLayer(4f, 2f, 2f, Color.argb((alpha * 128).toInt(), 0, 0, 0))
            } else {
                clearShadowLayer()
            }
        }

        strokePaint.apply {
            textSize = overlay.fontSize * scale
            color = overlay.strokeColor
            strokeWidth = overlay.strokeWidth
            this.alpha = (alpha * 255).toInt()
            textAlign = overlay.alignment
            typeface = textPaint.typeface
        }

        // Draw background if set
        if (overlay.backgroundColor != Color.TRANSPARENT) {
            val bounds = Rect()
            textPaint.getTextBounds(textToRender, 0, textToRender.length, bounds)
            val padding = 16f
            
            bgPaint.color = overlay.backgroundColor
            bgPaint.alpha = (alpha * 255).toInt()
            
            val bgRect = RectF(
                posX - bounds.width() / 2 - padding,
                posY - bounds.height() - padding,
                posX + bounds.width() / 2 + padding,
                posY + padding
            )
            canvas.drawRoundRect(bgRect, 8f, 8f, bgPaint)
        }

        // Apply rotation
        canvas.save()
        canvas.rotate(overlay.rotation, posX, posY)

        // Draw stroke first
        if (overlay.strokeWidth > 0) {
            canvas.drawText(textToRender, posX, posY, strokePaint)
        }

        // Draw text
        canvas.drawText(textToRender, posX, posY, textPaint)

        canvas.restore()

        return result
    }

    /**
     * Render multiple text overlays
     */
    fun renderAll(
        bitmap: Bitmap,
        overlays: List<TextOverlay>,
        currentTimeMs: Long
    ): Bitmap {
        var result = bitmap
        for (overlay in overlays.sortedBy { it.id }) {
            result = render(result, overlay, currentTimeMs)
        }
        return result
    }

    private fun calculateAnimationProgress(overlay: TextOverlay, currentTimeMs: Long): Float {
        val elapsed = currentTimeMs - overlay.startTimeMs
        return (elapsed.toFloat() / overlay.animationDurationMs).coerceIn(0f, 1f)
    }

    private fun calculateOutProgress(overlay: TextOverlay, currentTimeMs: Long): Float {
        val remaining = overlay.endTimeMs - currentTimeMs
        return (1 - remaining.toFloat() / overlay.animationDurationMs).coerceIn(0f, 1f)
    }
}

/**
 * Manager for text overlays on timeline
 */
class TextOverlayManager {
    private val overlays = mutableListOf<TextOverlay>()
    private val renderer = TextRenderer()
    private var nextId = 1

    fun addText(
        text: String,
        x: Float = 0.5f,
        y: Float = 0.5f,
        startTimeMs: Long = 0,
        durationMs: Long = 5000
    ): TextOverlay {
        val overlay = TextOverlay(
            id = nextId++,
            text = text,
            x = x,
            y = y,
            startTimeMs = startTimeMs,
            endTimeMs = startTimeMs + durationMs
        )
        overlays.add(overlay)
        return overlay
    }

    fun updateText(id: Int, text: String) {
        overlays.find { it.id == id }?.text = text
    }

    fun removeText(id: Int) {
        overlays.removeAll { it.id == id }
    }

    fun getOverlay(id: Int): TextOverlay? = overlays.find { it.id == id }

    fun getAllOverlays(): List<TextOverlay> = overlays.toList()

    fun renderFrame(bitmap: Bitmap, currentTimeMs: Long): Bitmap {
        return renderer.renderAll(bitmap, overlays, currentTimeMs)
    }

    fun clear() {
        overlays.clear()
    }
}
