package com.vortexeditor.app.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.vortexeditor.app.R

/**
 * Speed adjustment dialog
 */
class SpeedDialogFragment : BottomSheetDialogFragment() {

    private var currentSpeed = 1.0f
    var onSpeedChanged: ((Float) -> Unit)? = null

    companion object {
        fun newInstance(currentSpeed: Float): SpeedDialogFragment {
            return SpeedDialogFragment().apply {
                arguments = Bundle().apply {
                    putFloat("speed", currentSpeed)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentSpeed = arguments?.getFloat("speed", 1.0f) ?: 1.0f
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.dialog_speed, container, false)
        
        val tvSpeed = view.findViewById<TextView>(R.id.tvSpeed)
        val seekBar = view.findViewById<SeekBar>(R.id.seekBarSpeed)
        
        // Speed range: 0.25x to 4x
        // SeekBar: 0 to 100
        // Convert: speed = 0.25 + (seekBar / 100) * 3.75
        
        val speedToProgress = { speed: Float -> ((speed - 0.25f) / 3.75f * 100).toInt() }
        val progressToSpeed = { progress: Int -> 0.25f + progress / 100f * 3.75f }
        
        seekBar.progress = speedToProgress(currentSpeed)
        tvSpeed.text = String.format("%.2fx", currentSpeed)
        
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentSpeed = progressToSpeed(progress)
                tvSpeed.text = String.format("%.2fx", currentSpeed)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                onSpeedChanged?.invoke(currentSpeed)
            }
        })

        // Preset buttons
        view.findViewById<View>(R.id.btn025x)?.setOnClickListener { setSpeed(0.25f, seekBar, tvSpeed, speedToProgress) }
        view.findViewById<View>(R.id.btn05x)?.setOnClickListener { setSpeed(0.5f, seekBar, tvSpeed, speedToProgress) }
        view.findViewById<View>(R.id.btn1x)?.setOnClickListener { setSpeed(1.0f, seekBar, tvSpeed, speedToProgress) }
        view.findViewById<View>(R.id.btn15x)?.setOnClickListener { setSpeed(1.5f, seekBar, tvSpeed, speedToProgress) }
        view.findViewById<View>(R.id.btn2x)?.setOnClickListener { setSpeed(2.0f, seekBar, tvSpeed, speedToProgress) }
        view.findViewById<View>(R.id.btn4x)?.setOnClickListener { setSpeed(4.0f, seekBar, tvSpeed, speedToProgress) }

        return view
    }

    private fun setSpeed(speed: Float, seekBar: SeekBar, tvSpeed: TextView, speedToProgress: (Float) -> Int) {
        currentSpeed = speed
        seekBar.progress = speedToProgress(speed)
        tvSpeed.text = String.format("%.2fx", speed)
        onSpeedChanged?.invoke(speed)
    }
}
