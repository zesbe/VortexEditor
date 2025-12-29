package com.vortexeditor.app.ui.components

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.max
import kotlin.math.min

/**
 * Custom timeline view for video editing
 */
class TimelineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class Clip(
        val id: Int,
        var startTimeMs: Long,
        var durationMs: Long,
        var trackIndex: Int,
        var color: Int = Color.parseColor("#6200EE"),
        var thumbnails: List<Bitmap>? = null,
        var title: String = ""
    )

    private val clips = mutableListOf<Clip>()
    private var totalDurationMs: Long = 60000 // 1 minute default
    private var currentPositionMs: Long = 0
    private var pixelsPerMs: Float = 0.1f
    private var scrollOffsetX: Float = 0f
    
    // Track settings
    private val trackHeight = 80f
    private val trackSpacing = 8f
    private val trackCount = 3
    
    // Colors
    private val backgroundColor = Color.parseColor("#1E1E1E")
    private val trackBackgroundColor = Color.parseColor("#2D2D2D")
    private val playheadColor = Color.RED
    private val timeRulerColor = Color.parseColor("#666666")
    private val selectedColor = Color.parseColor("#FFD700")
    
    // Paints
    private val clipPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    private val borderPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    
    private val textPaint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
        textSize = 24f
    }
    
    private val playheadPaint = Paint().apply {
        isAntiAlias = true
        color = playheadColor
        strokeWidth = 3f
    }
    
    private val rulerPaint = Paint().apply {
        isAntiAlias = true
        color = timeRulerColor
        textSize = 20f
    }

    // Interaction
    private var selectedClipId: Int? = null
    private var isDragging = false
    private var dragClipId: Int? = null
    private var dragStartX = 0f
    private var dragStartTime = 0L
    
    // Listeners
    var onClipSelected: ((Int) -> Unit)? = null
    var onClipMoved: ((Int, Long) -> Unit)? = null
    var onPositionChanged: ((Long) -> Unit)? = null
    var onClipDoubleClick: ((Int) -> Unit)? = null

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            handleTap(e.x, e.y)
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            handleDoubleTap(e.x, e.y)
            return true
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            if (!isDragging) {
                scrollOffsetX += distanceX
                scrollOffsetX = max(0f, min(scrollOffsetX, totalDurationMs * pixelsPerMs - width))
                invalidate()
            }
            return true
        }
    })

    private val scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            pixelsPerMs *= detector.scaleFactor
            pixelsPerMs = max(0.01f, min(1f, pixelsPerMs))
            invalidate()
            return true
        }
    })

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Background
        canvas.drawColor(backgroundColor)
        
        // Time ruler
        drawTimeRuler(canvas)
        
        // Track backgrounds
        for (i in 0 until trackCount) {
            val y = getTrackY(i)
            canvas.drawRect(
                0f, y, width.toFloat(), y + trackHeight,
                Paint().apply { color = trackBackgroundColor }
            )
        }
        
        // Clips
        for (clip in clips) {
            drawClip(canvas, clip)
        }
        
        // Playhead
        drawPlayhead(canvas)
    }

    private fun drawTimeRuler(canvas: Canvas) {
        val rulerHeight = 30f
        val startMs = (scrollOffsetX / pixelsPerMs).toLong()
        val endMs = ((scrollOffsetX + width) / pixelsPerMs).toLong()
        
        // Draw every second
        var timeMs = (startMs / 1000) * 1000
        while (timeMs <= endMs) {
            val x = (timeMs * pixelsPerMs) - scrollOffsetX
            
            // Long tick every 5 seconds
            val tickHeight = if (timeMs % 5000 == 0L) 15f else 8f
            canvas.drawLine(x, rulerHeight - tickHeight, x, rulerHeight, rulerPaint)
            
            // Time label every 5 seconds
            if (timeMs % 5000 == 0L) {
                val label = formatTime(timeMs)
                canvas.drawText(label, x + 4, rulerHeight - 18, rulerPaint)
            }
            
            timeMs += 1000
        }
        
        canvas.drawLine(0f, rulerHeight, width.toFloat(), rulerHeight, rulerPaint)
    }

    private fun drawClip(canvas: Canvas, clip: Clip) {
        val x = (clip.startTimeMs * pixelsPerMs) - scrollOffsetX
        val width = clip.durationMs * pixelsPerMs
        val y = getTrackY(clip.trackIndex)
        
        // Skip if not visible
        if (x + width < 0 || x > this.width) return
        
        val rect = RectF(x, y, x + width, y + trackHeight)
        
        // Clip background
        clipPaint.color = clip.color
        canvas.drawRoundRect(rect, 8f, 8f, clipPaint)
        
        // Thumbnails
        clip.thumbnails?.let { thumbs ->
            val thumbWidth = width / thumbs.size
            for ((i, thumb) in thumbs.withIndex()) {
                val thumbX = x + i * thumbWidth
                val thumbRect = RectF(thumbX, y + 2, thumbX + thumbWidth - 2, y + trackHeight - 2)
                canvas.drawBitmap(thumb, null, thumbRect, null)
            }
        }
        
        // Selection border
        if (clip.id == selectedClipId) {
            borderPaint.color = selectedColor
            canvas.drawRoundRect(rect, 8f, 8f, borderPaint)
        }
        
        // Title
        if (clip.title.isNotEmpty()) {
            canvas.drawText(clip.title, x + 8, y + 24, textPaint)
        }
    }

    private fun drawPlayhead(canvas: Canvas) {
        val x = (currentPositionMs * pixelsPerMs) - scrollOffsetX
        
        if (x >= 0 && x <= width) {
            // Line
            canvas.drawLine(x, 0f, x, height.toFloat(), playheadPaint)
            
            // Triangle at top
            val path = Path().apply {
                moveTo(x, 0f)
                lineTo(x - 10, 20f)
                lineTo(x + 10, 20f)
                close()
            }
            canvas.drawPath(path, playheadPaint.apply { style = Paint.Style.FILL })
        }
    }

    private fun getTrackY(trackIndex: Int): Float {
        return 40f + trackIndex * (trackHeight + trackSpacing)
    }

    private fun handleTap(x: Float, y: Float) {
        // Check if tapped on a clip
        val clip = findClipAt(x, y)
        if (clip != null) {
            selectedClipId = clip.id
            onClipSelected?.invoke(clip.id)
        } else {
            // Tapped on empty space - move playhead
            selectedClipId = null
            val timeMs = ((x + scrollOffsetX) / pixelsPerMs).toLong()
            setCurrentPosition(timeMs)
        }
        invalidate()
    }

    private fun handleDoubleTap(x: Float, y: Float) {
        val clip = findClipAt(x, y)
        if (clip != null) {
            onClipDoubleClick?.invoke(clip.id)
        }
    }

    private fun findClipAt(x: Float, y: Float): Clip? {
        val timeMs = ((x + scrollOffsetX) / pixelsPerMs).toLong()
        
        for (clip in clips.reversed()) {
            val trackY = getTrackY(clip.trackIndex)
            if (y >= trackY && y <= trackY + trackHeight) {
                if (timeMs >= clip.startTimeMs && timeMs <= clip.startTimeMs + clip.durationMs) {
                    return clip
                }
            }
        }
        return null
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val clip = findClipAt(event.x, event.y)
                if (clip != null && clip.id == selectedClipId) {
                    isDragging = true
                    dragClipId = clip.id
                    dragStartX = event.x
                    dragStartTime = clip.startTimeMs
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging && dragClipId != null) {
                    val deltaX = event.x - dragStartX
                    val deltaTime = (deltaX / pixelsPerMs).toLong()
                    val newTime = max(0L, dragStartTime + deltaTime)
                    
                    clips.find { it.id == dragClipId }?.startTimeMs = newTime
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging && dragClipId != null) {
                    clips.find { it.id == dragClipId }?.let { clip ->
                        onClipMoved?.invoke(clip.id, clip.startTimeMs)
                    }
                }
                isDragging = false
                dragClipId = null
            }
        }
        
        return true
    }

    // Public API
    fun addClip(clip: Clip) {
        clips.add(clip)
        updateTotalDuration()
        invalidate()
    }

    fun removeClip(clipId: Int) {
        clips.removeAll { it.id == clipId }
        if (selectedClipId == clipId) selectedClipId = null
        updateTotalDuration()
        invalidate()
    }

    fun setCurrentPosition(positionMs: Long) {
        currentPositionMs = positionMs.coerceIn(0, totalDurationMs)
        onPositionChanged?.invoke(currentPositionMs)
        invalidate()
    }

    fun setTotalDuration(durationMs: Long) {
        totalDurationMs = durationMs
        invalidate()
    }

    fun getSelectedClipId(): Int? = selectedClipId

    private fun updateTotalDuration() {
        var maxEnd = 60000L
        for (clip in clips) {
            val end = clip.startTimeMs + clip.durationMs
            if (end > maxEnd) maxEnd = end
        }
        totalDurationMs = maxEnd + 10000 // Add 10s padding
    }

    private fun formatTime(ms: Long): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / 60000)
        return String.format("%d:%02d", minutes, seconds)
    }
}
