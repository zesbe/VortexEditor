package com.vortexeditor.app.editor

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.vortexeditor.app.R

class TextFragment : BottomSheetDialogFragment() {

    private var textContent = ""
    private var fontSize = 48f
    private var textColor = Color.WHITE
    private var selectedFont = "sans-serif"

    var onTextAdded: ((text: String, size: Float, color: Int, font: String) -> Unit)? = null

    private val fonts = listOf(
        "sans-serif" to "Default",
        "sans-serif-medium" to "Medium",
        "sans-serif-light" to "Light",
        "serif" to "Serif",
        "monospace" to "Mono",
        "cursive" to "Cursive"
    )

    private val colors = listOf(
        Color.WHITE,
        Color.BLACK,
        Color.RED,
        Color.parseColor("#FF5722"),
        Color.parseColor("#FFC107"),
        Color.parseColor("#4CAF50"),
        Color.parseColor("#2196F3"),
        Color.parseColor("#9C27B0"),
        Color.parseColor("#E91E63"),
        Color.parseColor("#00BCD4")
    )

    companion object {
        fun newInstance(): TextFragment {
            return TextFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_text, container, false)
        
        val etText = view.findViewById<EditText>(R.id.etText)
        val tvPreview = view.findViewById<TextView>(R.id.tvPreview)
        val seekBarSize = view.findViewById<SeekBar>(R.id.seekBarSize)
        val rvColors = view.findViewById<RecyclerView>(R.id.rvColors)
        val rvFonts = view.findViewById<RecyclerView>(R.id.rvFonts)

        // Text input
        etText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                textContent = s.toString()
                tvPreview.text = textContent.ifEmpty { "Preview" }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // Size slider
        seekBarSize.progress = 48
        seekBarSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                fontSize = progress.coerceAtLeast(12).toFloat()
                tvPreview.textSize = fontSize / 2 // Scale down for preview
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Colors
        rvColors.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rvColors.adapter = ColorAdapter { color ->
            textColor = color
            tvPreview.setTextColor(color)
        }

        // Fonts
        rvFonts.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rvFonts.adapter = FontAdapter { font ->
            selectedFont = font
            tvPreview.typeface = android.graphics.Typeface.create(font, android.graphics.Typeface.NORMAL)
        }

        // Apply button
        view.findViewById<View>(R.id.btnApply).setOnClickListener {
            if (textContent.isNotEmpty()) {
                onTextAdded?.invoke(textContent, fontSize, textColor, selectedFont)
            }
            dismiss()
        }

        view.findViewById<View>(R.id.btnCancel).setOnClickListener {
            dismiss()
        }

        return view
    }

    inner class ColorAdapter(private val onColorClick: (Int) -> Unit) : 
        RecyclerView.Adapter<ColorAdapter.ViewHolder>() {

        private var selectedPosition = 0

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val colorView: View = view.findViewById(R.id.colorView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_color, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val color = colors[position]
            holder.colorView.setBackgroundColor(color)
            holder.colorView.alpha = if (position == selectedPosition) 1f else 0.6f
            
            holder.itemView.setOnClickListener {
                selectedPosition = position
                onColorClick(color)
                notifyDataSetChanged()
            }
        }

        override fun getItemCount() = colors.size
    }

    inner class FontAdapter(private val onFontClick: (String) -> Unit) : 
        RecyclerView.Adapter<FontAdapter.ViewHolder>() {

        private var selectedPosition = 0

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvFont: TextView = view.findViewById(R.id.tvFont)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_font, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val (font, name) = fonts[position]
            holder.tvFont.text = name
            holder.tvFont.typeface = android.graphics.Typeface.create(font, android.graphics.Typeface.NORMAL)
            holder.tvFont.alpha = if (position == selectedPosition) 1f else 0.6f
            
            holder.itemView.setOnClickListener {
                selectedPosition = position
                onFontClick(font)
                notifyDataSetChanged()
            }
        }

        override fun getItemCount() = fonts.size
    }
}
