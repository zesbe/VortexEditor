package com.vortexeditor.app.editor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.vortexeditor.app.R

class TransitionsFragment : BottomSheetDialogFragment() {

    private var selectedTransition = "none"
    var onTransitionSelected: ((String, Float) -> Unit)? = null

    data class TransitionItem(val id: String, val name: String, val icon: String)

    private val transitions = listOf(
        TransitionItem("none", "None", "❌"),
        TransitionItem("fade", "Fade", "🌫️"),
        TransitionItem("dissolve", "Dissolve", "✨"),
        TransitionItem("wipe_left", "Wipe ←", "⬅️"),
        TransitionItem("wipe_right", "Wipe →", "➡️"),
        TransitionItem("slide_left", "Slide ←", "📤"),
        TransitionItem("slide_right", "Slide →", "📥"),
        TransitionItem("zoom_in", "Zoom In", "🔍"),
        TransitionItem("zoom_out", "Zoom Out", "🔎"),
        TransitionItem("circle", "Circle", "⭕"),
        TransitionItem("blur", "Blur", "💨")
    )

    companion object {
        fun newInstance() = TransitionsFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_transitions, container, false)
        
        view.findViewById<RecyclerView>(R.id.rvTransitions).apply {
            layoutManager = GridLayoutManager(context, 4)
            adapter = TransitionAdapter()
        }

        val seekBar = view.findViewById<android.widget.SeekBar>(R.id.seekBarDuration)
        val tvDuration = view.findViewById<TextView>(R.id.tvDuration)
        
        seekBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: android.widget.SeekBar?, p: Int, u: Boolean) {
                tvDuration.text = String.format("%.1fs", (p + 1) * 0.1f)
            }
            override fun onStartTrackingTouch(sb: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(sb: android.widget.SeekBar?) {}
        })

        view.findViewById<View>(R.id.btnApply).setOnClickListener {
            onTransitionSelected?.invoke(selectedTransition, (seekBar.progress + 1) * 0.1f)
            dismiss()
        }
        view.findViewById<View>(R.id.btnCancel).setOnClickListener { dismiss() }

        return view
    }

    inner class TransitionAdapter : RecyclerView.Adapter<TransitionAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvIcon: TextView = v.findViewById(R.id.tvIcon)
            val tvName: TextView = v.findViewById(R.id.tvName)
            val container: View = v.findViewById(R.id.container)
        }

        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(
            LayoutInflater.from(p.context).inflate(R.layout.item_filter, p, false)
        )

        override fun onBindViewHolder(h: VH, pos: Int) {
            val t = transitions[pos]
            h.tvIcon.text = t.icon
            h.tvName.text = t.name
            h.container.setBackgroundResource(if (t.id == selectedTransition) R.drawable.bg_selected else R.drawable.bg_filter_item)
            h.itemView.setOnClickListener {
                selectedTransition = t.id
                notifyDataSetChanged()
            }
        }

        override fun getItemCount() = transitions.size
    }
}
