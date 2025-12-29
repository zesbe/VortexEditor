package com.vortexeditor.app.editor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.vortexeditor.app.R

class AudioFragment : BottomSheetDialogFragment() {

    private var videoVolume = 100
    private var musicVolume = 100
    private var isMuted = false

    var onAudioSettingsChanged: ((videoVol: Int, musicVol: Int, muted: Boolean) -> Unit)? = null
    var onAddMusic: (() -> Unit)? = null
    var onRecordVoice: (() -> Unit)? = null

    companion object {
        fun newInstance(): AudioFragment {
            return AudioFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_audio, container, false)
        
        val seekBarVideo = view.findViewById<SeekBar>(R.id.seekBarVideoVolume)
        val seekBarMusic = view.findViewById<SeekBar>(R.id.seekBarMusicVolume)
        val tvVideoVol = view.findViewById<TextView>(R.id.tvVideoVolume)
        val tvMusicVol = view.findViewById<TextView>(R.id.tvMusicVolume)

        seekBarVideo.progress = videoVolume
        seekBarMusic.progress = musicVolume

        seekBarVideo.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                videoVolume = progress
                tvVideoVol.text = "$progress%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBarMusic.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                musicVolume = progress
                tvMusicVol.text = "$progress%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        view.findViewById<View>(R.id.btnMute).setOnClickListener {
            isMuted = !isMuted
            it.alpha = if (isMuted) 0.5f else 1f
        }

        view.findViewById<View>(R.id.btnAddMusic).setOnClickListener {
            onAddMusic?.invoke()
            dismiss()
        }

        view.findViewById<View>(R.id.btnRecordVoice).setOnClickListener {
            onRecordVoice?.invoke()
            dismiss()
        }

        view.findViewById<View>(R.id.btnApply).setOnClickListener {
            onAudioSettingsChanged?.invoke(videoVolume, musicVolume, isMuted)
            dismiss()
        }

        view.findViewById<View>(R.id.btnCancel).setOnClickListener {
            dismiss()
        }

        return view
    }
}
