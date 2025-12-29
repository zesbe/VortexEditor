package com.vortexeditor.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.vortexeditor.app.databinding.ActivityEditorBinding
import com.vortexeditor.app.editor.*
import com.vortexeditor.app.ui.dialogs.SpeedDialogFragment

class EditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditorBinding
    private var videoUri: Uri? = null
    private var videoName: String = ""
    private var isPlaying = false
    private var currentPosition = 0L
    private var videoDuration = 0L

    // Current editing state
    private var currentSpeed = 1.0f
    private var currentFilter = "none"
    private var trimStart = 0L
    private var trimEnd = 0L
    private val textOverlays = mutableListOf<TextOverlayData>()
    private val stickerOverlays = mutableListOf<StickerData>()

    data class TextOverlayData(
        val text: String,
        val size: Float,
        val color: Int,
        val font: String,
        var x: Float = 0.5f,
        var y: Float = 0.5f
    )

    data class StickerData(
        val emoji: String,
        var x: Float = 0.5f,
        var y: Float = 0.5f,
        var scale: Float = 1f
    )

    // Music picker
    private val pickMusicLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            Toast.makeText(this, "Music added: $it", Toast.LENGTH_SHORT).show()
            // TODO: Add music to timeline
        }
    }

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
            Toast.makeText(this, "Undo", Toast.LENGTH_SHORT).show()
        }

        binding.btnRedo.setOnClickListener {
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
                    trimEnd = videoDuration
                    binding.tvDuration.text = formatTime(videoDuration)
                    binding.seekBar.max = videoDuration.toInt()
                    binding.videoView.seekTo(1)
                }

                binding.videoView.setOnCompletionListener {
                    isPlaying = false
                    updatePlayButton()
                }

                binding.videoView.setOnErrorListener { _, what, _ ->
                    Toast.makeText(this, "Error playing video: $what", Toast.LENGTH_SHORT).show()
                    true
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error loading video: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        binding.btnPlayPause.setOnClickListener {
            togglePlayPause()
        }

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
            showTrimDialog()
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
            showFiltersDialog()
        }

        // Text
        binding.btnText.setOnClickListener {
            showTextDialog()
        }

        // Stickers
        binding.btnStickers.setOnClickListener {
            showStickersDialog()
        }

        // Audio
        binding.btnAudio.setOnClickListener {
            showAudioDialog()
        }

        // Background
        binding.btnBackground.setOnClickListener {
            showBackgroundDialog()
        }

        // Transitions
        binding.btnTransition.setOnClickListener {
            showTransitionsDialog()
        }
    }

    private fun showTrimDialog() {
        TrimFragment.newInstance(videoDuration).apply {
            onTrimApplied = { start, end ->
                trimStart = start
                trimEnd = end
                Toast.makeText(this@EditorActivity, 
                    "Trimmed: ${formatTime(start)} - ${formatTime(end)}", 
                    Toast.LENGTH_SHORT).show()
            }
        }.show(supportFragmentManager, "trim")
    }

    private fun splitAtCurrentPosition() {
        val position = binding.videoView.currentPosition
        Toast.makeText(this, "Split at ${formatTime(position.toLong())}", Toast.LENGTH_SHORT).show()
    }

    private fun showSpeedDialog() {
        SpeedDialogFragment.newInstance(currentSpeed).apply {
            onSpeedChanged = { speed ->
                currentSpeed = speed
                Toast.makeText(this@EditorActivity, "Speed: ${speed}x", Toast.LENGTH_SHORT).show()
            }
        }.show(supportFragmentManager, "speed")
    }

    private fun showFiltersDialog() {
        FiltersFragment.newInstance().apply {
            onFilterSelected = { filter ->
                currentFilter = filter
                Toast.makeText(this@EditorActivity, "Filter: $filter", Toast.LENGTH_SHORT).show()
                applyFilter(filter)
            }
        }.show(supportFragmentManager, "filters")
    }

    private fun applyFilter(filter: String) {
        // TODO: Apply filter to video preview using GPUImage or native code
    }

    private fun showTextDialog() {
        TextFragment.newInstance().apply {
            onTextAdded = { text, size, color, font ->
                textOverlays.add(TextOverlayData(text, size, color, font))
                Toast.makeText(this@EditorActivity, "Text added: $text", Toast.LENGTH_SHORT).show()
                // TODO: Show text overlay on video
            }
        }.show(supportFragmentManager, "text")
    }

    private fun showStickersDialog() {
        StickersFragment.newInstance().apply {
            onStickerSelected = { emoji ->
                stickerOverlays.add(StickerData(emoji))
                Toast.makeText(this@EditorActivity, "Sticker added: $emoji", Toast.LENGTH_SHORT).show()
                // TODO: Show sticker overlay on video
            }
        }.show(supportFragmentManager, "stickers")
    }

    private fun showAudioDialog() {
        AudioFragment.newInstance().apply {
            onAudioSettingsChanged = { videoVol, musicVol, muted ->
                Toast.makeText(this@EditorActivity, 
                    "Audio: Video=$videoVol%, Music=$musicVol%, Muted=$muted", 
                    Toast.LENGTH_SHORT).show()
            }
            onAddMusic = {
                pickMusicLauncher.launch("audio/*")
            }
            onRecordVoice = {
                Toast.makeText(this@EditorActivity, "Voice recording...", Toast.LENGTH_SHORT).show()
                // TODO: Open voice recorder
            }
        }.show(supportFragmentManager, "audio")
    }

    private fun showBackgroundDialog() {
        BackgroundFragment.newInstance().apply {
            onBackgroundApplied = { mode, intensity, color ->
                Toast.makeText(this@EditorActivity, 
                    "Background: $mode, intensity=$intensity%", 
                    Toast.LENGTH_SHORT).show()
            }
        }.show(supportFragmentManager, "background")
    }

    private fun showTransitionsDialog() {
        TransitionsFragment.newInstance().apply {
            onTransitionSelected = { transition, duration ->
                Toast.makeText(this@EditorActivity, 
                    "Transition: $transition, ${duration}s", 
                    Toast.LENGTH_SHORT).show()
            }
        }.show(supportFragmentManager, "transitions")
    }

    private fun showExportDialog() {
        com.vortexeditor.app.ui.dialogs.ExportDialogFragment.newInstance().apply {
            onExportRequested = { resolution, quality, fps ->
                Toast.makeText(this@EditorActivity, 
                    "Exporting: $resolution, $quality quality, ${fps}fps", 
                    Toast.LENGTH_SHORT).show()
                // TODO: Start export with VideoExporter
            }
        }.show(supportFragmentManager, "export")
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
