package com.vortexeditor.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.vortexeditor.app.effects.VideoEffects

/**
 * Filter preview grid view
 */
class FilterPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class FilterItem(
        val id: String,
        val name: String,
        val preview: Bitmap? = null
    )

    private val filters = mutableListOf<FilterItem>()
    private var originalBitmap: Bitmap? = null
    private var selectedFilterId: String? = null
    
    private val columns = 4
    private val itemPadding = 8f
    
    private val textPaint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }
    
    private val selectedPaint = Paint().apply {
        isAntiAlias = true
        color = Color.parseColor("#6200EE")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    var onFilterSelected: ((String) -> Unit)? = null

    init {
        // Initialize with default filters
        initDefaultFilters()
    }

    private fun initDefaultFilters() {
        filters.clear()
        filters.addAll(listOf(
            FilterItem("none", "Original"),
            FilterItem("brightness", "Bright"),
            FilterItem("contrast", "Contrast"),
            FilterItem("saturation", "Vivid"),
            FilterItem("grayscale", "B&W"),
            FilterItem("sepia", "Sepia"),
            FilterItem("vintage", "Vintage"),
            FilterItem("cool", "Cool"),
            FilterItem("warm", "Warm"),
            FilterItem("dramatic", "Drama"),
            FilterItem("fade", "Fade"),
            FilterItem("noir", "Noir"),
            FilterItem("invert", "Invert"),
            FilterItem("vignette", "Vignette")
        ))
    }

    fun setSourceBitmap(bitmap: Bitmap) {
        originalBitmap = bitmap
        generatePreviews()
        invalidate()
    }

    private fun generatePreviews() {
        val source = originalBitmap ?: return
        val previewSize = 100
        val scaled = Bitmap.createScaledBitmap(source, previewSize, previewSize, true)
        
        filters.replaceAll { filter ->
            val preview = when (filter.id) {
                "none" -> scaled.copy(scaled.config, false)
                "brightness" -> VideoEffects.applyBrightness(scaled, 30f)
                "contrast" -> VideoEffects.applyContrast(scaled, 1.3f)
                "saturation" -> VideoEffects.applySaturation(scaled, 1.5f)
                "grayscale" -> VideoEffects.applyGrayscale(scaled)
                "sepia" -> VideoEffects.applySepia(scaled)
                "vintage" -> VideoEffects.applyVintage(scaled)
                "cool" -> VideoEffects.applyCool(scaled)
                "warm" -> VideoEffects.applyWarm(scaled)
                "dramatic" -> VideoEffects.applyDramatic(scaled)
                "fade" -> VideoEffects.applyFade(scaled)
                "noir" -> VideoEffects.applyNoir(scaled)
                "invert" -> VideoEffects.applyInvert(scaled)
                "vignette" -> VideoEffects.applyVignette(scaled, 0.5f)
                else -> scaled.copy(scaled.config, false)
            }
            filter.copy(preview = preview)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        canvas.drawColor(Color.parseColor("#1E1E1E"))
        
        val itemWidth = (width - itemPadding * (columns + 1)) / columns
        val itemHeight = itemWidth + 30f // Extra space for label
        
        for ((index, filter) in filters.withIndex()) {
            val col = index % columns
            val row = index / columns
            
            val x = itemPadding + col * (itemWidth + itemPadding)
            val y = itemPadding + row * (itemHeight + itemPadding)
            
            // Draw preview
            filter.preview?.let { preview ->
                canvas.drawBitmap(
                    preview,
                    null,
                    android.graphics.RectF(x, y, x + itemWidth, y + itemWidth),
                    null
                )
            }
            
            // Draw selection
            if (filter.id == selectedFilterId) {
                canvas.drawRect(x, y, x + itemWidth, y + itemWidth, selectedPaint)
            }
            
            // Draw label
            canvas.drawText(filter.name, x + itemWidth / 2, y + itemWidth + 24f, textPaint)
        }
    }

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        if (event.action == android.view.MotionEvent.ACTION_UP) {
            val itemWidth = (width - itemPadding * (columns + 1)) / columns
            val itemHeight = itemWidth + 30f
            
            val col = ((event.x - itemPadding) / (itemWidth + itemPadding)).toInt()
            val row = ((event.y - itemPadding) / (itemHeight + itemPadding)).toInt()
            
            val index = row * columns + col
            if (index in filters.indices) {
                selectedFilterId = filters[index].id
                onFilterSelected?.invoke(filters[index].id)
                invalidate()
            }
        }
        return true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val itemWidth = (width - itemPadding * (columns + 1)) / columns
        val itemHeight = itemWidth + 30f
        val rows = (filters.size + columns - 1) / columns
        val height = (rows * (itemHeight + itemPadding) + itemPadding).toInt()
        
        setMeasuredDimension(width, height)
    }
}
