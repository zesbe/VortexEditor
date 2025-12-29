package com.vortexeditor.app.editor

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.vortexeditor.app.databinding.FragmentTrimBinding

class TrimFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentTrimBinding? = null
    private val binding get() = _binding!!

    private var videoDuration: Long = 0
    private var startTime: Long = 0
    private var endTime: Long = 0

    var onTrimApplied: ((startMs: Long, endMs: Long) -> Unit)? = null

    companion object {
        fun newInstance(duration: Long): TrimFragment {
            return TrimFragment().apply {
                arguments = Bundle().apply {
                    putLong("duration", duration)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        videoDuration = arguments?.getLong("duration", 0) ?: 0
        endTime = videoDuration
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrimBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rangeSlider.valueFrom = 0f
        binding.rangeSlider.valueTo = videoDuration.toFloat()
        binding.rangeSlider.values = listOf(0f, videoDuration.toFloat())

        updateTimeLabels()

        binding.rangeSlider.addOnChangeListener { slider, _, _ ->
            val values = slider.values
            startTime = values[0].toLong()
            endTime = values[1].toLong()
            updateTimeLabels()
        }

        binding.btnApply.setOnClickListener {
            onTrimApplied?.invoke(startTime, endTime)
            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun updateTimeLabels() {
        binding.tvStartTime.text = formatTime(startTime)
        binding.tvEndTime.text = formatTime(endTime)
        binding.tvDuration.text = "Duration: ${formatTime(endTime - startTime)}"
    }

    private fun formatTime(ms: Long): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / 60000) % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
