package com.vortexeditor.app.editor

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.vortexeditor.app.R

class BackgroundFragment : BottomSheetDialogFragment() {

    private var selectedMode = "blur"
    private var blurIntensity = 50
    private var selectedColor = Color.GREEN

    var onBackgroundApplied: ((mode: String, intensity: Int, color: Int) -> Unit)? = null

    data class ModeItem(val id: String, val name: String, val icon: String)

    private val modes = listOf(
        ModeItem("none", "Original", "📷"),
        ModeItem("blur", "Blur BG", "🌫️"),
        ModeItem("remove", "Remove", "✂️"),
        ModeItem("color", "Color", "🎨"),
        ModeItem("image", "Image", "🖼️")
    )

    private val colors = listOf(
        Color.parseColor("#00FF00"),
        Color.WHITE, Color.BLACK,
        Color.RED, Color.BLUE,
        Color.YELLOW, Color.MAGENTA, Color.CYAN
    )

    companion object {
        fun newInstance() = BackgroundFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_background, container, false)
        
        view.findViewById<RecyclerView>(R.id.rvModes).apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = ModeAdapter()
        }

        val seekBar = view.findViewById<SeekBar>(R.id.seekBarIntensity)
        val tvIntensity = view.findViewById<TextView>(R.id.tvIntensity)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, u: Boolean) {
                blurIntensity = p
                tvIntensity.text = "$p%"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        view.findViewById<RecyclerView>(R.id.rvColors).apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = ColorAdapter()
        }

        view.findViewById<View>(R.id.btnApply).setOnClickListener {
            onBackgroundApplied?.invoke(selectedMode, blurIntensity, selectedColor)
            dismiss()
        }
        view.findViewById<View>(R.id.btnCancel).setOnClickListener { dismiss() }

        return view
    }

    inner class ModeAdapter : RecyclerView.Adapter<ModeAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvIcon: TextView = v.findViewById(R.id.tvIcon)
            val tvName: TextView = v.findViewById(R.id.tvName)
            val container: View = v.findViewById(R.id.container)
        }

        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(
            LayoutInflater.from(p.context).inflate(R.layout.item_mode, p, false)
        )

        override fun onBindViewHolder(h: VH, pos: Int) {
            val m = modes[pos]
            h.tvIcon.text = m.icon
            h.tvName.text = m.name
            h.container.setBackgroundResource(if (m.id == selectedMode) R.drawable.bg_selected else R.drawable.bg_filter_item)
            h.itemView.setOnClickListener {
                selectedMode = m.id
                notifyDataSetChanged()
            }
        }

        override fun getItemCount() = modes.size
    }

    inner class ColorAdapter : RecyclerView.Adapter<ColorAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val colorView: View = v.findViewById(R.id.colorView)
        }

        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(
            LayoutInflater.from(p.context).inflate(R.layout.item_color, p, false)
        )

        override fun onBindViewHolder(h: VH, pos: Int) {
            val c = colors[pos]
            h.colorView.setBackgroundColor(c)
            h.colorView.alpha = if (c == selectedColor) 1f else 0.5f
            h.itemView.setOnClickListener {
                selectedColor = c
                notifyDataSetChanged()
            }
        }

        override fun getItemCount() = colors.size
    }
}
