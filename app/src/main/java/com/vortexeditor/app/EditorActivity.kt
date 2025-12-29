package com.vortexeditor.app

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.vortexeditor.app.databinding.ActivityEditorBinding

class EditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditorBinding
    private var videoUri: Uri? = null
    private var videoName: String = ""
    private var isPlaying = false
    private var currentPosition = 0L
    private var videoDuration = 0L

    // Native engine handle
    private var engineHandle: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get video data from intent
        intent?.let {
            videoUri = it.getStringExtra("video_uri")?.toUri()
            videoName = it.getStringExtra("video_name") ?: "Untitled"
        }

        if (videoUri == null) {
            Toast.makeText(this, "No video selected", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        setupVideoPlayer()
        setupToolbar()
        setupBottomTools()
    }

    private fun setupUI() {
        binding.tvVideoName.text = videoName
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            onBackPressed()
        }

        binding.btnUndo.setOnClickListener {
            // TODO: Implement undo
            Toast.makeText(this, "Undo", Toast.LENGTH_SHORT).show()
        }

        binding.btnRedo.setOnClickListener {
            // TODO: Implement redo
            Toast.makeText(this, "Redo", Toast.LENGTH_SHORT).show()
        }

        binding.btnExport.setOnClickListener {
            showExportDialog()
        }
    }

    private fun setupVideoPlayer() {
        videoUri?.let { uri ->
            try {
                binding.videoView.setVideoURI(uri)
                binding.videoView.setOnPreparedListener { mp ->
                    videoDuration = mp.duration.toLong()
                    binding.tvDuration.text = formatTime(videoDuration)
                    binding.seekBar.max = videoDuration.toInt()
                    
                    // Show first frame
                    binding.videoView.seekTo(1)
                }

                binding.videoView.setOnCompletionListener {
                    isPlaying = false
                    updatePlayButton()
                }

                binding.videoView.setOnErrorListener { _, what, extra ->
                    Toast.makeText(this, "Error playing video: $what", Toast.LENGTH_SHORT).show()
                    true
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error loading video: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        // Play/Pause button
        binding.btnPlayPause.setOnClickListener {
            togglePlayPause()
        }

        // SeekBar
        binding.seekBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.videoView.seekTo(progress)
                    binding.tvCurrentTime.text = formatTime(progress.toLong())
                }
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        // Update progress periodically
        binding.videoView.setOnPreparedListener {
            updateProgress()
        }
    }

    private fun togglePlayPause() {
        if (isPlaying) {
            binding.videoView.pause()
        } else {
            binding.videoView.start()
        }
        isPlaying = !isPlaying
        updatePlayButton()
    }

    private fun updatePlayButton() {
        binding.btnPlayPause.setImageResource(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
    }

    private fun updateProgress() {
        binding.root.postDelayed({
            if (binding.videoView.isPlaying) {
                val position = binding.videoView.currentPosition
                binding.seekBar.progress = position
                binding.tvCurrentTime.text = formatTime(position.toLong())
            }
            if (!isFinishing) {
                updateProgress()
            }
        }, 100)
    }

    private fun setupBottomTools() {
        // Trim
        binding.btnTrim.setOnClickListener {
            showToolPanel("trim")
        }

        // Split
        binding.btnSplit.setOnClickListener {
            splitAtCurrentPosition()
        }

        // Speed
        binding.btnSpeed.setOnClickListener {
            showSpeedDialog()
        }

        // Filters
        binding.btnFilters.setOnClickListener {
            showToolPanel("filters")
        }

        // Text
        binding.btnText.setOnClickListener {
            showToolPanel("text")
        }

        // Stickers
        binding.btnStickers.setOnClickListener {
            showToolPanel("stickers")
        }

        // Audio
        binding.btnAudio.setOnClickListener {
            showToolPanel("audio")
        }

        // Background
        binding.btnBackground.setOnClickListener {
            showToolPanel("background")
        }
    }

    private fun showToolPanel(tool: String) {
        Toast.makeText(this, "Opening $tool panel...", Toast.LENGTH_SHORT).show()
        // TODO: Implement tool panels
    }

    private fun splitAtCurrentPosition() {
        val position = binding.videoView.currentPosition
        Toast.makeText(this, "Split at ${formatTime(position.toLong())}", Toast.LENGTH_SHORT).show()
        // TODO: Implement split
    }

    private fun showSpeedDialog() {
        com.vortexeditor.app.ui.dialogs.SpeedDialogFragment.newInstance(1.0f).apply {
            onSpeedChanged = { speed ->
                Toast.makeText(this@EditorActivity, "Speed: ${speed}x", Toast.LENGTH_SHORT).show()
                // TODO: Apply speed change
            }
        }.show(supportFragmentManager, "speed")
    }

    private fun showExportDialog() {
        Toast.makeText(this, "Export dialog...", Toast.LENGTH_SHORT).show()
        // TODO: Show export dialog
    }

    private fun formatTime(ms: Long): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / 60000) % 60
        val hours = ms / 3600000
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }

    override fun onPause() {
        super.onPause()
        if (isPlaying) {
            binding.videoView.pause()
            isPlaying = false
            updatePlayButton()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.videoView.stopPlayback()
    }
}
