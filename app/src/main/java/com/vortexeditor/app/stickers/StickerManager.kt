package com.vortexeditor.app.stickers

import android.content.Context
import android.graphics.*
import androidx.core.graphics.drawable.toBitmap
import java.io.File

/**
 * Sticker overlay for video
 */
data class Sticker(
    val id: Int,
    var bitmap: Bitmap,
    var x: Float = 0.5f,  // 0.0 to 1.0
    var y: Float = 0.5f,
    var scale: Float = 1.0f,
    var rotation: Float = 0f,
    var alpha: Float = 1.0f,
    var startTimeMs: Long = 0,
    var endTimeMs: Long = Long.MAX_VALUE,
    var animation: StickerAnimation = StickerAnimation.NONE,
    var animationDurationMs: Long = 500
)

enum class StickerAnimation {
    NONE,
    FADE_IN,
    FADE_OUT,
    POP_IN,
    POP_OUT,
    BOUNCE,
    SPIN,
    SHAKE,
    PULSE
}

class StickerManager(private val context: Context) {
    
    private val stickers = mutableListOf<Sticker>()
    private var nextId = 1

    // Built-in emoji stickers
    private val emojiList = listOf(
        "😀", "😂", "🥰", "😎", "🤩", "😍", "🥳", "😇",
        "🔥", "💯", "⭐", "✨", "💥", "💫", "🎉", "🎊",
        "❤️", "💕", "💖", "💗", "💘", "💝", "💜", "💙",
        "👍", "👎", "👏", "🙌", "🤝", "✌️", "🤞", "👋",
        "🎬", "🎥", "📹", "🎞️", "📽️", "🎦", "📺", "📷",
        "🎵", "🎶", "🎤", "🎧", "🎸", "🎹", "🥁", "🎺"
    )

    /**
     * Add emoji sticker
     */
    fun addEmoji(emoji: String, x: Float = 0.5f, y: Float = 0.5f, size: Int = 100): Sticker {
        val bitmap = createEmojiBitmap(emoji, size)
        return addSticker(bitmap, x, y)
    }

    /**
     * Add image sticker from file
     */
    fun addImageSticker(filePath: String, x: Float = 0.5f, y: Float = 0.5f): Sticker? {
        val bitmap = BitmapFactory.decodeFile(filePath) ?: return null
        return addSticker(bitmap, x, y)
    }

    /**
     * Add sticker from bitmap
     */
    fun addSticker(bitmap: Bitmap, x: Float = 0.5f, y: Float = 0.5f): Sticker {
        val sticker = Sticker(
            id = nextId++,
            bitmap = bitmap,
            x = x,
            y = y
        )
        stickers.add(sticker)
        return sticker
    }

    /**
     * Remove sticker
     */
    fun removeSticker(stickerId: Int) {
        stickers.find { it.id == stickerId }?.bitmap?.recycle()
        stickers.removeAll { it.id == stickerId }
    }

    /**
     * Update sticker position
     */
    fun updatePosition(stickerId: Int, x: Float, y: Float) {
        stickers.find { it.id == stickerId }?.apply {
            this.x = x
            this.y = y
        }
    }

    /**
     * Update sticker scale
     */
    fun updateScale(stickerId: Int, scale: Float) {
        stickers.find { it.id == stickerId }?.scale = scale.coerceIn(0.1f, 5f)
    }

    /**
     * Update sticker rotation
     */
    fun updateRotation(stickerId: Int, rotation: Float) {
        stickers.find { it.id == stickerId }?.rotation = rotation
    }

    /**
     * Set sticker time range
     */
    fun setTimeRange(stickerId: Int, startMs: Long, endMs: Long) {
        stickers.find { it.id == stickerId }?.apply {
            startTimeMs = startMs
            endTimeMs = endMs
        }
    }

    /**
     * Set sticker animation
     */
    fun setAnimation(stickerId: Int, animation: StickerAnimation, durationMs: Long = 500) {
        stickers.find { it.id == stickerId }?.apply {
            this.animation = animation
            this.animationDurationMs = durationMs
        }
    }

    /**
     * Get all available emojis
     */
    fun getAvailableEmojis(): List<String> = emojiList

    /**
     * Get all stickers
     */
    fun getAllStickers(): List<Sticker> = stickers.toList()

    /**
     * Render stickers onto frame
     */
    fun renderFrame(bitmap: Bitmap, currentTimeMs: Long): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val width = bitmap.width.toFloat()
        val height = bitmap.height.toFloat()

        for (sticker in stickers) {
            if (currentTimeMs < sticker.startTimeMs || currentTimeMs > sticker.endTimeMs) {
                continue
            }

            val animProgress = calculateAnimationProgress(sticker, currentTimeMs)
            
            var alpha = sticker.alpha
            var scale = sticker.scale
            var rotation = sticker.rotation
            var offsetX = 0f
            var offsetY = 0f

            // Apply animation
            when (sticker.animation) {
                StickerAnimation.FADE_IN -> {
                    alpha = animProgress * sticker.alpha
                }
                StickerAnimation.FADE_OUT -> {
                    val outProgress = calculateOutProgress(sticker, currentTimeMs)
                    alpha = (1 - outProgress) * sticker.alpha
                }
                StickerAnimation.POP_IN -> {
                    scale = sticker.scale * easeOutBack(animProgress)
                }
                StickerAnimation.POP_OUT -> {
                    val outProgress = calculateOutProgress(sticker, currentTimeMs)
                    scale = sticker.scale * (1 - easeInBack(outProgress))
                }
                StickerAnimation.BOUNCE -> {
                    offsetY = -30f * kotlin.math.abs(kotlin.math.sin(animProgress * Math.PI * 3).toFloat())
                }
                StickerAnimation.SPIN -> {
                    rotation = sticker.rotation + 360f * animProgress
                }
                StickerAnimation.SHAKE -> {
                    offsetX = 10f * kotlin.math.sin(animProgress * Math.PI * 10).toFloat()
                }
                StickerAnimation.PULSE -> {
                    val pulseScale = 1f + 0.1f * kotlin.math.sin(animProgress * Math.PI * 4).toFloat()
                    scale = sticker.scale * pulseScale
                }
                StickerAnimation.NONE -> { }
            }

            // Calculate position
            val posX = sticker.x * width + offsetX
            val posY = sticker.y * height + offsetY

            // Draw sticker
            val paint = Paint().apply {
                isAntiAlias = true
                this.alpha = (alpha * 255).toInt()
            }

            canvas.save()
            canvas.translate(posX, posY)
            canvas.rotate(rotation)
            canvas.scale(scale, scale)

            val stickerWidth = sticker.bitmap.width
            val stickerHeight = sticker.bitmap.height
            canvas.drawBitmap(
                sticker.bitmap,
                -stickerWidth / 2f,
                -stickerHeight / 2f,
                paint
            )

            canvas.restore()
        }

        return result
    }

    private fun createEmojiBitmap(emoji: String, size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val paint = Paint().apply {
            textSize = size * 0.8f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val y = size / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(emoji, size / 2f, y, paint)
        
        return bitmap
    }

    private fun calculateAnimationProgress(sticker: Sticker, currentTimeMs: Long): Float {
        val elapsed = currentTimeMs - sticker.startTimeMs
        return (elapsed.toFloat() / sticker.animationDurationMs).coerceIn(0f, 1f)
    }

    private fun calculateOutProgress(sticker: Sticker, currentTimeMs: Long): Float {
        val remaining = sticker.endTimeMs - currentTimeMs
        return (1 - remaining.toFloat() / sticker.animationDurationMs).coerceIn(0f, 1f)
    }

    private fun easeOutBack(t: Float): Float {
        val c1 = 1.70158f
        val c3 = c1 + 1
        return 1 + c3 * Math.pow((t - 1).toDouble(), 3.0).toFloat() + c1 * Math.pow((t - 1).toDouble(), 2.0).toFloat()
    }

    private fun easeInBack(t: Float): Float {
        val c1 = 1.70158f
        val c3 = c1 + 1
        return c3 * t * t * t - c1 * t * t
    }

    fun clear() {
        stickers.forEach { it.bitmap.recycle() }
        stickers.clear()
    }
}
