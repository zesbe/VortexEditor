package com.vortexeditor.app.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.vortexeditor.app.R

class ExportDialogFragment : BottomSheetDialogFragment() {

    var onExportRequested: ((resolution: String, quality: String, fps: Int) -> Unit)? = null

    private var selectedResolution = "1080p"
    private var selectedQuality = "high"
    private var selectedFps = 30

    companion object {
        fun newInstance(): ExportDialogFragment {
            return ExportDialogFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.dialog_export, container, false)

        // Resolution
        view.findViewById<RadioGroup>(R.id.rgResolution).setOnCheckedChangeListener { _, checkedId ->
            selectedResolution = when (checkedId) {
                R.id.rb720p -> "720p"
                R.id.rb1080p -> "1080p"
                R.id.rb4k -> "4K"
                else -> "1080p"
            }
        }

        // Quality
        view.findViewById<RadioGroup>(R.id.rgQuality).setOnCheckedChangeListener { _, checkedId ->
            selectedQuality = when (checkedId) {
                R.id.rbLow -> "low"
                R.id.rbMedium -> "medium"
                R.id.rbHigh -> "high"
                else -> "high"
            }
        }

        // FPS
        view.findViewById<RadioGroup>(R.id.rgFps).setOnCheckedChangeListener { _, checkedId ->
            selectedFps = when (checkedId) {
                R.id.rb24fps -> 24
                R.id.rb30fps -> 30
                R.id.rb60fps -> 60
                else -> 30
            }
        }

        // Export button
        view.findViewById<View>(R.id.btnExport).setOnClickListener {
            onExportRequested?.invoke(selectedResolution, selectedQuality, selectedFps)
            dismiss()
        }

        return view
    }
}
