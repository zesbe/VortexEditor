package com.videoeditor.app.ui.export

import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.videoeditor.app.core.VideoEditorManager
import com.videoeditor.app.databinding.ActivityExportBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ExportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExportBinding
    private lateinit var editorManager: VideoEditorManager
    private var isExporting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        editorManager = VideoEditorManager(this)

        setupUI()
        observeProgress()
    }

    private fun setupUI() {
        // Resolution options
        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                binding.radio720p.id -> updateResolution(1280, 720)
                binding.radio1080p.id -> updateResolution(1920, 1080)
                binding.radio4k.id -> updateResolution(3840, 2160)
            }
        }

        binding.btnExport.setOnClickListener {
            if (isExporting) {
                cancelExport()
            } else {
                startExport()
            }
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun updateResolution(width: Int, height: Int) {
        binding.tvResolution.text = "${width}x${height}"
    }

    private fun startExport() {
        isExporting = true
        binding.btnExport.text = "Cancel"
        binding.progressBar.progress = 0

        val outputDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val outputFile = File(outputDir, "VideoEditor_$timestamp.mp4")

        val (width, height) = when (binding.radioGroup.checkedRadioButtonId) {
            binding.radio720p.id -> 1280 to 720
            binding.radio1080p.id -> 1920 to 1080
            binding.radio4k.id -> 3840 to 2160
            else -> 1920 to 1080
        }

        val fps = binding.spinnerFps.selectedItem.toString().replace(" fps", "").toIntOrNull() ?: 30
        val bitrate = when (binding.spinnerQuality.selectedItemPosition) {
            0 -> 5_000_000   // Low
            1 -> 10_000_000  // Medium
            2 -> 20_000_000  // High
            else -> 10_000_000
        }

        lifecycleScope.launch {
            val success = editorManager.export(
                outputPath = outputFile.absolutePath,
                width = width,
                height = height,
                fps = fps,
                bitrate = bitrate
            )

            isExporting = false
            binding.btnExport.text = "Export"

            if (success) {
                Toast.makeText(this@ExportActivity, "Export complete: ${outputFile.absolutePath}", Toast.LENGTH_LONG).show()
                finish()
            } else {
                Toast.makeText(this@ExportActivity, "Export failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cancelExport() {
        editorManager.cancelExport()
        isExporting = false
        binding.btnExport.text = "Export"
        Toast.makeText(this, "Export cancelled", Toast.LENGTH_SHORT).show()
    }

    private fun observeProgress() {
        lifecycleScope.launch {
            editorManager.exportProgress.collectLatest { progress ->
                binding.progressBar.progress = (progress * 100).toInt()
                binding.tvProgress.text = "${(progress * 100).toInt()}%"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isExporting) {
            editorManager.cancelExport()
        }
    }
}
