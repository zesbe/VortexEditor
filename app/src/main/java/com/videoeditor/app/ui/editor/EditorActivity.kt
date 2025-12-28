package com.videoeditor.app.ui.editor

import android.net.Uri
import android.os.Bundle
import android.view.SurfaceHolder
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.videoeditor.app.core.VideoEditorManager
import com.videoeditor.app.databinding.ActivityEditorBinding
import com.videoeditor.app.ui.export.ExportActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class EditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditorBinding
    private lateinit var editorManager: VideoEditorManager

    companion object {
        const val EXTRA_VIDEO_URI = "extra_video_uri"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        editorManager = VideoEditorManager(this)

        setupPreview()
        setupControls()
        setupTimeline()
        loadVideo()
        observeState()
    }

    private fun setupPreview() {
        binding.surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                editorManager.setPreviewSurface(holder.surface)
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                editorManager.setPreviewSurface(null)
            }
        })
    }

    private fun setupControls() {
        binding.btnPlay.setOnClickListener {
            if (editorManager.state.value.isPlaying) {
                editorManager.pause()
            } else {
                editorManager.play()
            }
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
            startExport()
        }

        binding.seekBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val duration = editorManager.state.value.duration
                    val position = (progress.toLong() * duration) / 100
                    editorManager.seekTo(position)
                }
            }

            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {
                editorManager.pause()
            }

            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })
    }

    private fun setupTimeline() {
        // Timeline will be set up with RecyclerView
        binding.btnTrim.setOnClickListener {
            // TODO: Implement trim mode
            Toast.makeText(this, "Trim mode", Toast.LENGTH_SHORT).show()
        }

        binding.btnSplit.setOnClickListener {
            // Split at current position
            val clips = editorManager.state.value.clips
            val currentPos = editorManager.state.value.currentPosition
            clips.firstOrNull()?.let { clip ->
                editorManager.splitClip(clip.id, currentPos)
            }
        }

        binding.btnSpeed.setOnClickListener {
            // TODO: Show speed dialog
            Toast.makeText(this, "Speed adjustment", Toast.LENGTH_SHORT).show()
        }

        binding.btnFilters.setOnClickListener {
            // TODO: Show filters panel
            Toast.makeText(this, "Filters", Toast.LENGTH_SHORT).show()
        }

        binding.btnText.setOnClickListener {
            // TODO: Add text overlay
            Toast.makeText(this, "Add text", Toast.LENGTH_SHORT).show()
        }

        binding.btnAudio.setOnClickListener {
            // TODO: Show audio panel
            Toast.makeText(this, "Audio", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadVideo() {
        val videoUri = intent.getStringExtra(EXTRA_VIDEO_URI)?.let { Uri.parse(it) }

        if (videoUri == null) {
            Toast.makeText(this, "No video selected", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch {
            if (!editorManager.initialize()) {
                Toast.makeText(this@EditorActivity, "Failed to initialize editor", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            if (!editorManager.createProject()) {
                Toast.makeText(this@EditorActivity, "Failed to create project", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            if (!editorManager.addClip(videoUri)) {
                Toast.makeText(this@EditorActivity, "Failed to add video", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            Toast.makeText(this@EditorActivity, "Video loaded", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            editorManager.state.collectLatest { state ->
                updateUI(state)
            }
        }
    }

    private fun updateUI(state: VideoEditorManager.EditorState) {
        // Update play button
        binding.btnPlay.text = if (state.isPlaying) "Pause" else "Play"

        // Update seek bar
        if (state.duration > 0) {
            val progress = ((state.currentPosition * 100) / state.duration).toInt()
            binding.seekBar.progress = progress
        }

        // Update time display
        binding.tvCurrentTime.text = formatTime(state.currentPosition)
        binding.tvDuration.text = formatTime(state.duration)
    }

    private fun formatTime(microseconds: Long): String {
        val totalSeconds = microseconds / 1_000_000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun startExport() {
        val intent = android.content.Intent(this, ExportActivity::class.java)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        editorManager.release()
    }
}
