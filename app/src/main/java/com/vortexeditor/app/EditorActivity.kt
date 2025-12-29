package com.vortexeditor.app

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.vortexeditor.app.databinding.ActivityEditorBinding

class EditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditorBinding
    private var videoUri: Uri? = null
    private var videoName: String = ""
    private var isPlaying = false
    private var videoDuration = 0L

    private val pickMusicLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { toast("Music added!") }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        videoUri = intent?.getStringExtra("video_uri")?.toUri()
        videoName = intent?.getStringExtra("video_name") ?: "Untitled"

        if (videoUri == null) {
            toast("No video selected")
            finish()
            return
        }

        setupUI()
        setupVideoPlayer()
        setupToolButtons()
    }

    private fun setupUI() {
        binding.tvVideoName.text = videoName
        binding.btnBack.setOnClickListener { finish() }
        binding.btnUndo.setOnClickListener { toast("Undo") }
        binding.btnRedo.setOnClickListener { toast("Redo") }
        binding.btnExport.setOnClickListener { showExportOptions() }
    }

    private fun setupVideoPlayer() {
        try {
            binding.videoView.setVideoURI(videoUri)
            binding.videoView.setOnPreparedListener { mp ->
                videoDuration = mp.duration.toLong()
                binding.tvDuration.text = formatTime(videoDuration)
                binding.seekBar.max = videoDuration.toInt()
                binding.videoView.seekTo(1)
                startProgressUpdate()
            }
            binding.videoView.setOnCompletionListener {
                isPlaying = false
                updatePlayButton()
            }
            binding.videoView.setOnErrorListener { _, _, _ ->
                toast("Error playing video")
                true
            }
        } catch (e: Exception) {
            toast("Error: ${e.message}")
        }

        binding.btnPlayPause.setOnClickListener { togglePlay() }
        
        binding.seekBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: android.widget.SeekBar?, p: Int, user: Boolean) {
                if (user) {
                    binding.videoView.seekTo(p)
                    binding.tvCurrentTime.text = formatTime(p.toLong())
                }
            }
            override fun onStartTrackingTouch(sb: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(sb: android.widget.SeekBar?) {}
        })
    }

    private fun togglePlay() {
        if (isPlaying) binding.videoView.pause() else binding.videoView.start()
        isPlaying = !isPlaying
        updatePlayButton()
    }

    private fun updatePlayButton() {
        binding.btnPlayPause.setImageResource(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
    }

    private fun startProgressUpdate() {
        binding.root.postDelayed({
            if (!isFinishing && binding.videoView.isPlaying) {
                val pos = binding.videoView.currentPosition
                binding.seekBar.progress = pos
                binding.tvCurrentTime.text = formatTime(pos.toLong())
            }
            if (!isFinishing) startProgressUpdate()
        }, 200)
    }

    private fun setupToolButtons() {
        binding.btnTrim.setOnClickListener { showTrimOptions() }
        binding.btnSplit.setOnClickListener { splitVideo() }
        binding.btnSpeed.setOnClickListener { showSpeedOptions() }
        binding.btnFilters.setOnClickListener { showFilters() }
        binding.btnText.setOnClickListener { showTextEditor() }
        binding.btnStickers.setOnClickListener { showStickers() }
        binding.btnAudio.setOnClickListener { showAudioOptions() }
        binding.btnBackground.setOnClickListener { showBackgroundOptions() }
        binding.btnTransition.setOnClickListener { showTransitions() }
    }

    private fun showTrimOptions() {
        val items = arrayOf("Trim Start", "Trim End", "Cut Selection")
        android.app.AlertDialog.Builder(this)
            .setTitle("✂️ Trim")
            .setItems(items) { _, i -> toast("${items[i]} applied") }
            .show()
    }

    private fun splitVideo() {
        val pos = binding.videoView.currentPosition
        toast("Split at ${formatTime(pos.toLong())}")
    }

    private fun showSpeedOptions() {
        val speeds = arrayOf("0.25x", "0.5x", "1x (Normal)", "1.5x", "2x", "4x")
        android.app.AlertDialog.Builder(this)
            .setTitle("⚡ Speed")
            .setItems(speeds) { _, i -> toast("Speed: ${speeds[i]}") }
            .show()
    }

    private fun showFilters() {
        val filters = arrayOf("None", "Bright", "Contrast", "B&W", "Sepia", "Vintage", "Cool", "Warm", "Dramatic", "Noir")
        android.app.AlertDialog.Builder(this)
            .setTitle("🎨 Filters")
            .setItems(filters) { _, i -> toast("Filter: ${filters[i]}") }
            .show()
    }

    private fun showTextEditor() {
        val input = android.widget.EditText(this)
        input.hint = "Enter text..."
        android.app.AlertDialog.Builder(this)
            .setTitle("📝 Add Text")
            .setView(input)
            .setPositiveButton("Add") { _, _ -> 
                val text = input.text.toString()
                if (text.isNotEmpty()) toast("Text added: $text")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showStickers() {
        val stickers = arrayOf("😀", "😎", "🔥", "❤️", "⭐", "👍", "🎉", "💯", "✨", "🎬")
        android.app.AlertDialog.Builder(this)
            .setTitle("😀 Stickers")
            .setItems(stickers) { _, i -> toast("Sticker added: ${stickers[i]}") }
            .show()
    }

    private fun showAudioOptions() {
        val options = arrayOf("Add Music", "Record Voice", "Mute Video", "Adjust Volume")
        android.app.AlertDialog.Builder(this)
            .setTitle("🎵 Audio")
            .setItems(options) { _, i -> 
                when (i) {
                    0 -> pickMusicLauncher.launch("audio/*")
                    1 -> toast("Recording...")
                    else -> toast("${options[i]}")
                }
            }
            .show()
    }

    private fun showBackgroundOptions() {
        val options = arrayOf("Original", "Blur Background", "Remove Background", "Solid Color", "Custom Image")
        android.app.AlertDialog.Builder(this)
            .setTitle("🎭 Background")
            .setItems(options) { _, i -> toast("Background: ${options[i]}") }
            .show()
    }

    private fun showTransitions() {
        val transitions = arrayOf("None", "Fade", "Dissolve", "Wipe Left", "Wipe Right", "Zoom In", "Zoom Out", "Circle", "Blur")
        android.app.AlertDialog.Builder(this)
            .setTitle("🔄 Transitions")
            .setItems(transitions) { _, i -> toast("Transition: ${transitions[i]}") }
            .show()
    }

    private fun showExportOptions() {
        val options = arrayOf("720p (HD)", "1080p (Full HD)", "4K (Ultra HD)")
        android.app.AlertDialog.Builder(this)
            .setTitle("📤 Export")
            .setItems(options) { _, i -> toast("Exporting ${options[i]}...") }
            .show()
    }

    private fun formatTime(ms: Long): String {
        val s = (ms / 1000) % 60
        val m = (ms / 60000) % 60
        return String.format("%d:%02d", m, s)
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

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
        try { binding.videoView.stopPlayback() } catch (_: Exception) {}
    }
}
