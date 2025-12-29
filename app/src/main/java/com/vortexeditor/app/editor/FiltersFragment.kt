package com.vortexeditor.app.editor

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.vortexeditor.app.R
import com.vortexeditor.app.effects.VideoEffects

class FiltersFragment : BottomSheetDialogFragment() {

    private var previewBitmap: Bitmap? = null
    private var selectedFilter: String = "none"
    
    var onFilterSelected: ((String) -> Unit)? = null

    data class FilterItem(
        val id: String,
        val name: String,
        val icon: String
    )

    private val filters = listOf(
        FilterItem("none", "Original", "📷"),
        FilterItem("brightness", "Bright", "☀️"),
        FilterItem("contrast", "Contrast", "🔲"),
        FilterItem("saturation", "Vivid", "🌈"),
        FilterItem("grayscale", "B&W", "⬛"),
        FilterItem("sepia", "Sepia", "🟤"),
        FilterItem("vintage", "Vintage", "📺"),
        FilterItem("cool", "Cool", "❄️"),
        FilterItem("warm", "Warm", "🔥"),
        FilterItem("dramatic", "Drama", "🎭"),
        FilterItem("fade", "Fade", "🌫️"),
        FilterItem("noir", "Noir", "🎬"),
        FilterItem("invert", "Invert", "🔄"),
        FilterItem("vignette", "Vignette", "⭕")
    )

    companion object {
        fun newInstance(): FiltersFragment {
            return FiltersFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_filters, container, false)
        
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvFilters)
        recyclerView.layoutManager = GridLayoutManager(context, 4)
        recyclerView.adapter = FilterAdapter()

        view.findViewById<View>(R.id.btnApply).setOnClickListener {
            onFilterSelected?.invoke(selectedFilter)
            dismiss()
        }

        view.findViewById<View>(R.id.btnCancel).setOnClickListener {
            dismiss()
        }

        return view
    }

    inner class FilterAdapter : RecyclerView.Adapter<FilterAdapter.ViewHolder>() {
        
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvIcon: TextView = view.findViewById(R.id.tvIcon)
            val tvName: TextView = view.findViewById(R.id.tvName)
            val container: View = view.findViewById(R.id.container)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_filter, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val filter = filters[position]
            holder.tvIcon.text = filter.icon
            holder.tvName.text = filter.name
            
            holder.container.isSelected = filter.id == selectedFilter
            holder.container.setBackgroundResource(
                if (filter.id == selectedFilter) R.drawable.bg_selected 
                else R.drawable.bg_filter_item
            )

            holder.itemView.setOnClickListener {
                selectedFilter = filter.id
                notifyDataSetChanged()
            }
        }

        override fun getItemCount() = filters.size
    }
}
